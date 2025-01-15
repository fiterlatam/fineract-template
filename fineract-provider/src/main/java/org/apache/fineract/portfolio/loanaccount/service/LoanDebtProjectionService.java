/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.fineract.portfolio.loanaccount.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.DisbursementData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.LoanDebtProjectionData;
import org.apache.fineract.portfolio.loanaccount.data.RepaymentScheduleRelatedLoanData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanAccountDomainServiceJpa;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleCalculationPlatformService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanDebtProjectionService {

    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanChargeAssembler loanChargeAssembler;
    private final FromJsonHelper fromApiJsonHelper;
    private final ChargeReadPlatformService chargeReadPlatformService;
    private final ChargeRepositoryWrapper chargeRepository;
    private final LoanAccountDomainServiceJpa loanAccountDomainServiceJpa;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanScheduleCalculationPlatformService calculationPlatformService;

    public LoanDebtProjectionData calculateDebtProjection(Long loanId, String projectionDate, String dateFormat) {
        // Find the loan and validate
        Loan loan = validateLoanForProjection(loanId);
        LocalDate projectedFutureDate = DateUtils.parseLocalDate(projectionDate, dateFormat);
        // projectedFutureDate can not be in the past
        if (projectedFutureDate.isBefore(DateUtils.getLocalDateOfTenant())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.projection.date.in.past", "Projection date cannot be in the past",
                    loan.getId());
        }
        final LoanAccountData loanAccountData = this.loanReadPlatformService.retrieveOne(loanId);
        final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedData = loanAccountData.getTimeline().repaymentScheduleRelatedData(
                loanAccountData.getCurrency(), loanAccountData.getPrincipal(), loanAccountData.getApprovedPrincipal(),
                loanAccountData.getInArrearsTolerance(), loanAccountData.getFeeChargesAtDisbursementCharged());
        Collection<DisbursementData> disbursementData = this.loanReadPlatformService.retrieveLoanDisbursementDetails(loanId);
        final LoanScheduleData loanScheduleData = this.loanReadPlatformService.retrieveRepaymentSchedule(loanId,
                repaymentScheduleRelatedData, disbursementData, loanAccountData.isInterestRecalculationEnabled(),
                LoanScheduleType.fromEnumOptionData(loanAccountData.getLoanScheduleType()));
        this.calculationPlatformService.getFeeChargesDetail(loanScheduleData, loanId);

        // Get overdue and future installments
        final List<LoanSchedulePeriodData> overdueInstallments = getOverdueInstallments(loanScheduleData, projectedFutureDate);
        final List<LoanSchedulePeriodData> futureInstallments = getFutureInstallments(loanScheduleData, projectedFutureDate);

        // Calculate projected overdue days
        final Long projectedOverdueDays = calculateProjectedOverdueDays(overdueInstallments, projectedFutureDate);

        // Calculate discriminated past due balance
        LoanDebtProjectionData.OverdueBalanceDetails overdueBalanceDetails = calculateDiscriminatedPastDueBalance(overdueInstallments,
                loan.getCurrency(), projectedFutureDate);

        // Calculate total balance details
        LoanDebtProjectionData.TotalBalanceDetails totalBalanceDetails = calculateTotalBalanceDetails(overdueBalanceDetails,
                futureInstallments);

        return new LoanDebtProjectionData(projectedOverdueDays, overdueBalanceDetails, totalBalanceDetails);
    }

    private Loan validateLoanForProjection(Long loanId) {
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

        // Validate loan is active
        if (loan.isClosed()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.closed", "Loan is closed and cannot be projected",
                    loan.getId());
        }

        return loan;
    }

    private List<LoanSchedulePeriodData> getOverdueInstallments(final LoanScheduleData loanScheduleData,
            final LocalDate projectedFutureDate) {
        return loanScheduleData.getPeriods().stream().filter(period -> period.getPeriod() != null && !period.getComplete())
                .filter(period -> period.getDueDate().isBefore(projectedFutureDate) || period.getDueDate().equals(projectedFutureDate))
                .toList();
    }

    private List<LoanSchedulePeriodData> getFutureInstallments(final LoanScheduleData loanScheduleData,
            final LocalDate projectedFutureDate) {
        final LocalDate currentDate = DateUtils.getLocalDateOfTenant();
        return loanScheduleData.getPeriods().stream().filter(period -> period.getPeriod() != null && !period.getComplete())
                .filter(period -> period.getDueDate().isAfter(currentDate) && period.getDueDate().isBefore(projectedFutureDate)).toList();
    }

    private LoanDebtProjectionData.OverdueBalanceDetails calculateDiscriminatedPastDueBalance(
            List<LoanSchedulePeriodData> overdueInstallments, MonetaryCurrency currency, LocalDate projectedFutureDate) {
        if (overdueInstallments.isEmpty()) {
            return new LoanDebtProjectionData.OverdueBalanceDetails(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Calculate Past Due Installment Balance
        BigDecimal pastDueInstallmentBalance = overdueInstallments.stream()
                .map(installment -> installment.getPrincipalOutstanding().add(installment.getInterestOutstanding()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        // Calculate Delinquency Interest
        final BigDecimal outstandingPenaltyCharges = overdueInstallments.stream().map(LoanSchedulePeriodData::getPenaltyChargesOutstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        // Calculate Fee
        final BigDecimal outstandingHonorariosFees = overdueInstallments.stream().map(LoanSchedulePeriodData::getHonorariosOutstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        return new LoanDebtProjectionData.OverdueBalanceDetails(pastDueInstallmentBalance, outstandingPenaltyCharges,
                outstandingHonorariosFees);
    }

    private LoanDebtProjectionData.TotalBalanceDetails calculateTotalBalanceDetails(
            LoanDebtProjectionData.OverdueBalanceDetails overdueDetails, List<LoanSchedulePeriodData> futureInstallments) {
        // Calculate Future Balance
        BigDecimal futureBalance = calculateFutureBalance(futureInstallments);

        // Calculate Total Overdue Balance
        BigDecimal totalOverdueBalance = calculateTotalOverdueBalance(overdueDetails);

        // Calculate Total Balance
        BigDecimal totalBalance = totalOverdueBalance.add(futureBalance).setScale(2, RoundingMode.HALF_UP);

        return new LoanDebtProjectionData.TotalBalanceDetails(totalOverdueBalance, futureBalance, totalBalance);
    }

    private BigDecimal calculateFutureBalance(List<LoanSchedulePeriodData> futureInstallments) {
        if (futureInstallments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // we should be able to calculate the outstanding for each installment
        return futureInstallments.stream().map(LoanSchedulePeriodData::getTotalOutstandingForPeriod)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalOverdueBalance(LoanDebtProjectionData.OverdueBalanceDetails overdueDetails) {
        return overdueDetails.getTotal();
    }

    private Long calculateProjectedOverdueDays(final List<LoanSchedulePeriodData> overdueInstallments,
            final LocalDate projectedFutureDate) {
        if (overdueInstallments.isEmpty()) {
            return 0L;
        }
        return overdueInstallments.stream().map(LoanSchedulePeriodData::getDueDate)
                .map(dueDate -> DateUtils.getDifferenceInDays(dueDate, projectedFutureDate)).max(Long::compareTo).orElse(0L);
    }

}
