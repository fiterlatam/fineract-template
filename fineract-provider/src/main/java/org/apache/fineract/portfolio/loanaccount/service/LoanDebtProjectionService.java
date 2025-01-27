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
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanaccount.data.DisbursementData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.LoanDebtProjectionData;
import org.apache.fineract.portfolio.loanaccount.data.RepaymentScheduleRelatedLoanData;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleCalculationPlatformService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanDebtProjectionService {

    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanUtilService loanUtilService;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanScheduleCalculationPlatformService calculationPlatformService;
    private final LoanChargeWritePlatformService loanChargeWritePlatformService;

    public LoanDebtProjectionData calculateDebtProjection(Long loanId, String projectionDate, String dateFormat) {
        final Loan loan = validateLoanForProjection(loanId);
        final LocalDate projectedFutureDate = DateUtils.parseLocalDate(projectionDate, dateFormat);
        if (projectedFutureDate.isBefore(DateUtils.getLocalDateOfTenant())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.projection.date.in.past", "Projection date cannot be in the past",
                    loan.getId());
        }
        final ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, null);
        final LoanRepaymentScheduleInstallment loanForeclosureDetail = loan.fetchLoanForeclosureDetail(projectedFutureDate,
                scheduleGeneratorDTO);
        final LoanAccountData loanBasicDetails = this.loanReadPlatformService.retrieveOne(loanId);
        final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedData = loanBasicDetails.getTimeline().repaymentScheduleRelatedData(
                loanBasicDetails.getCurrency(), loanBasicDetails.getPrincipal(), loanBasicDetails.getApprovedPrincipal(),
                loanBasicDetails.getInArrearsTolerance(), loanBasicDetails.getFeeChargesAtDisbursementCharged());
        final Collection<DisbursementData> disbursementData = this.loanReadPlatformService.retrieveLoanDisbursementDetails(loanId);
        final LoanScheduleData repaymentSchedule = this.loanReadPlatformService.retrieveRepaymentSchedule(loanId,
                repaymentScheduleRelatedData, disbursementData, loanBasicDetails.isInterestRecalculationEnabled(),
                LoanScheduleType.fromEnumOptionData(loanBasicDetails.getLoanScheduleType()));
        this.calculationPlatformService.getFeeChargesDetail(repaymentSchedule, loanId);
        final List<LoanSchedulePeriodData> projectedRepaymentPeriods = repaymentSchedule.getPeriods().stream()
                .filter(period -> period.getPeriod() != null && !period.getComplete())
                .filter(period -> DateUtils.isAfter(projectedFutureDate, period.getFromDate())).toList();
        final Long projectedOverdueDays = calculateProjectedOverdueDays(projectedRepaymentPeriods, projectedFutureDate);
        final List<LoanSchedulePeriodData> overdueRepaymentPeriods = repaymentSchedule.getPeriods().stream()
                .filter(period -> period.getPeriod() != null && !period.getComplete())
                .filter(period -> DateUtils.isAfter(projectedFutureDate, period.getDueDate())).toList();
        final BigDecimal principalProjected = overdueRepaymentPeriods.stream().map(LoanSchedulePeriodData::getPrincipalOutstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal interestProjected = overdueRepaymentPeriods.stream().map(LoanSchedulePeriodData::getInterestOutstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal avalProjected = overdueRepaymentPeriods.stream().map(LoanSchedulePeriodData::getAvalOutstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal mandatoryInsuranceProjected = overdueRepaymentPeriods.stream()
                .map(LoanSchedulePeriodData::getMandatoryInsuranceOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal voluntaryInsuranceProjected = overdueRepaymentPeriods.stream()
                .map(LoanSchedulePeriodData::getVoluntaryInsuranceOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        final Collection<OverdueLoanScheduleData> overdueLoanScheduleDataList = loanReadPlatformService
                .retrieveAllOverdueInstallmentsForLoan(loan, projectedFutureDate);
        final BigDecimal penaltyChargesProjected = loanChargeWritePlatformService.projectOverdueChargesForLoan(loanId,
                overdueLoanScheduleDataList, projectedFutureDate);
        final BigDecimal penaltyChargesAccountedFor = overdueRepaymentPeriods.stream().map(
                period -> period.getPenaltyChargesPaid().add(period.getPenaltyChargesWaived()).add(period.getPenaltyChargesWrittenOff()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal penaltyProjected = penaltyChargesProjected.subtract(penaltyChargesAccountedFor);
        final BigDecimal saldoVencidoCuotas = interestProjected.add(principalProjected).add(avalProjected).add(mandatoryInsuranceProjected)
                .add(voluntaryInsuranceProjected).setScale(2, RoundingMode.HALF_UP);
        final BigDecimal totalRepayment = saldoVencidoCuotas.add(penaltyProjected);
        final BigDecimal honorariosProjected = this.loanReadPlatformService.calculateHonorariosAmount(loanId, totalRepayment,
                projectedFutureDate);
        final BigDecimal saldoVencidoIntMora = penaltyProjected.setScale(2, RoundingMode.HALF_UP);
        final BigDecimal saldoVencidoHonorario = honorariosProjected.setScale(2, RoundingMode.HALF_UP);
        final BigDecimal saldoTotalVencido = saldoVencidoCuotas.add(saldoVencidoIntMora).add(saldoVencidoHonorario).setScale(2,
                RoundingMode.HALF_UP);
        final BigDecimal saldoTotal = loanForeclosureDetail.getTotalOutstanding(loan.getCurrency()).getAmount().setScale(2,
                RoundingMode.HALF_UP);
        final BigDecimal saldoTotalFuturo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        final LoanDebtProjectionData.OverdueBalanceDetails overdueBalanceDetails = new LoanDebtProjectionData.OverdueBalanceDetails(
                saldoVencidoCuotas, saldoVencidoIntMora, saldoVencidoHonorario);
        final LoanDebtProjectionData.TotalBalanceDetails totalBalanceDetails = new LoanDebtProjectionData.TotalBalanceDetails(
                saldoTotalVencido, saldoTotalFuturo, saldoTotal);
        return new LoanDebtProjectionData(projectedOverdueDays, overdueBalanceDetails, totalBalanceDetails);
    }

    private Loan validateLoanForProjection(Long loanId) {
        final Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
        if (loan.isClosed()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.closed", "Loan is closed and cannot be projected",
                    loan.getId());
        }
        return loan;
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
