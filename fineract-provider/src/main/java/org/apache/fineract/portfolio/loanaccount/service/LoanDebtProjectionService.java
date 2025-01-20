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

import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.DisbursementData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.LoanDebtProjectionData;
import org.apache.fineract.portfolio.loanaccount.data.RepaymentScheduleRelatedLoanData;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanAccountDomainServiceJpa;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
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
    private final LoanChargeAssembler loanChargeAssembler;
    private final FromJsonHelper fromApiJsonHelper;
    private final ChargeReadPlatformService chargeReadPlatformService;
    private final ChargeRepositoryWrapper chargeRepository;
    private final LoanAccountDomainServiceJpa loanAccountDomainServiceJpa;
    private final LoanUtilService loanUtilService;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanScheduleCalculationPlatformService calculationPlatformService;

    public LoanDebtProjectionData calculateDebtProjection(Long loanId, String projectionDate, String dateFormat) {
        final Loan loan = validateLoanForProjection(loanId);
        final LocalDate projectedFutureDate = DateUtils.parseLocalDate(projectionDate, dateFormat);
        if (projectedFutureDate.isBefore(DateUtils.getLocalDateOfTenant())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.projection.date.in.past", "Projection date cannot be in the past",
                    loan.getId());
        }
        final ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, null);
        final LoanRepaymentScheduleInstallment loanForeclosureDetail = loan.fetchLoanForeclosureDetail(projectedFutureDate, scheduleGeneratorDTO);
        final LoanAccountData loanBasicDetails = this.loanReadPlatformService.retrieveOne(loanId);
        final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedData = loanBasicDetails.getTimeline()
                .repaymentScheduleRelatedData(loanBasicDetails.getCurrency(), loanBasicDetails.getPrincipal(),
                        loanBasicDetails.getApprovedPrincipal(), loanBasicDetails.getInArrearsTolerance(),
                        loanBasicDetails.getFeeChargesAtDisbursementCharged());
        final Collection<DisbursementData> disbursementData = this.loanReadPlatformService.retrieveLoanDisbursementDetails(loanId);
        final LoanScheduleData repaymentSchedule = this.loanReadPlatformService.retrieveRepaymentSchedule(loanId, repaymentScheduleRelatedData,
                disbursementData, loanBasicDetails.isInterestRecalculationEnabled(),
                LoanScheduleType.fromEnumOptionData(loanBasicDetails.getLoanScheduleType()));
        this.calculationPlatformService.getFeeChargesDetail(repaymentSchedule, loanId);
        final List<LoanSchedulePeriodData> projectedRepaymentPeriods = repaymentSchedule.getPeriods().stream()
                .filter(period -> period.getPeriod() != null && !period.getComplete())
                .filter(period -> !DateUtils.isAfter(period.getDueDate(), projectedFutureDate)).toList();
        final BigDecimal interestProjected = loanForeclosureDetail.getInterestOutstanding(loan.getCurrency()).getAmount();
        final BigDecimal principalProjected = projectedRepaymentPeriods.stream().map(LoanSchedulePeriodData::getPrincipalOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal avalProjected = projectedRepaymentPeriods.stream().map(LoanSchedulePeriodData::getAvalOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal mandatoryInsuranceProjected = projectedRepaymentPeriods.stream().map(LoanSchedulePeriodData::getMandatoryInsuranceOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal voluntaryInsuranceProjected = projectedRepaymentPeriods.stream().map(LoanSchedulePeriodData::getVoluntaryInsuranceOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal penaltyProjected = projectedRepaymentPeriods.stream().map(LoanSchedulePeriodData::getPenaltyChargesOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("interestProjected: " + interestProjected);
        System.out.println("principalProjected: " + principalProjected);
        System.out.println("avalProjected: " + avalProjected);
        System.out.println("mandatoryInsuranceProjected: " + mandatoryInsuranceProjected);
        System.out.println("voluntaryInsuranceProjected: " + voluntaryInsuranceProjected);
        System.out.println("penaltyProjected: " + penaltyProjected);
        final BigDecimal totalRepayment = interestProjected.add(principalProjected).add(avalProjected).add(mandatoryInsuranceProjected).add(voluntaryInsuranceProjected).add(penaltyProjected);
        final BigDecimal honorariosProjected =  this.loanReadPlatformService.calculateHonorariosAmount(loanId, totalRepayment, projectedFutureDate);
        System.out.println("honorariosProjected: " + honorariosProjected);

        final Collection<OverdueLoanScheduleData> overdueLoanScheduleDataList = loanReadPlatformService
                .retrieveAllOverdueInstallmentsForLoan(loan, projectedFutureDate);




        // Get overdue and future installments
        List<LoanRepaymentScheduleInstallment> overdueInstallments = getOverdueInstallments(loan, projectedFutureDate);
        List<LoanRepaymentScheduleInstallment> futureInstallments = getFutureInstallments(loan, projectedFutureDate);

        // Calculate projected overdue days
        Long projectedOverdueDays = calculateProjectedOverdueDays(overdueInstallments, projectedFutureDate);

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

    private List<LoanRepaymentScheduleInstallment> getOverdueInstallments(Loan loan, LocalDate projectedFutureDate) {
        return loan.getRepaymentScheduleInstallments().stream()
                .filter(installment -> installment.getDueDate().isBefore(projectedFutureDate)
                        || installment.getDueDate().equals(projectedFutureDate))
                .filter(LoanRepaymentScheduleInstallment::isNotFullyPaidOff).toList();
    }

    private List<LoanRepaymentScheduleInstallment> getFutureInstallments(Loan loan, LocalDate projectedFutureDate) {
        LocalDate currentDate = DateUtils.getLocalDateOfTenant();
        return loan.getRepaymentScheduleInstallments().stream().filter(
                installment -> installment.getDueDate().isAfter(currentDate) && installment.getDueDate().isBefore(projectedFutureDate))
                .toList();
    }

    private LoanDebtProjectionData.OverdueBalanceDetails calculateDiscriminatedPastDueBalance(
            List<LoanRepaymentScheduleInstallment> overdueInstallments, MonetaryCurrency currency, LocalDate projectedFutureDate) {
        if (overdueInstallments.isEmpty()) {
            return new LoanDebtProjectionData.OverdueBalanceDetails(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Calculate Past Due Installment Balance
        BigDecimal pastDueInstallmentBalance = overdueInstallments.stream()
                .map(installment -> installment.getPrincipal(currency).add(installment.getInterestCharged(currency))
                        .add(installment.getFeeChargesCharged(currency)).add(installment.getPenaltyChargesCharged(currency)).getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        // Calculate Delinquency Interest
        BigDecimal delinquencyInterest = calculatePenaltyForOverdueInstallments(overdueInstallments, projectedFutureDate);

        // Calculate Fee

        BigDecimal honorarioFee = overdueInstallments.stream()
                .map(installment -> loanAccountDomainServiceJpa.calculateFeeHonorario(installment, BigDecimal.ZERO).getFeeHono())
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        return new LoanDebtProjectionData.OverdueBalanceDetails(pastDueInstallmentBalance, delinquencyInterest, honorarioFee);
    }

    private LoanDebtProjectionData.TotalBalanceDetails calculateTotalBalanceDetails(
            LoanDebtProjectionData.OverdueBalanceDetails overdueDetails, List<LoanRepaymentScheduleInstallment> futureInstallments) {
        // Calculate Future Balance
        BigDecimal futureBalance = calculateFutureBalance(futureInstallments);

        // Calculate Total Overdue Balance
        BigDecimal totalOverdueBalance = calculateTotalOverdueBalance(overdueDetails);

        // Calculate Total Balance
        BigDecimal totalBalance = totalOverdueBalance.add(futureBalance).setScale(2, RoundingMode.HALF_UP);

        return new LoanDebtProjectionData.TotalBalanceDetails(totalOverdueBalance, futureBalance, totalBalance);
    }

    private BigDecimal calculateFutureBalance(List<LoanRepaymentScheduleInstallment> futureInstallments) {
        if (futureInstallments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        MonetaryCurrency currency = futureInstallments.get(0).getLoan().getCurrency();

        // we should be able to calculate the outstanding for each installment
        return futureInstallments.stream().map(installment -> installment.getTotalOutstanding(currency).getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalOverdueBalance(LoanDebtProjectionData.OverdueBalanceDetails overdueDetails) {
        return overdueDetails.getTotal();
    }

    private Long calculateProjectedOverdueDays(List<LoanRepaymentScheduleInstallment> overdueInstallments, LocalDate projectedFutureDate) {
        if (overdueInstallments.isEmpty()) {
            return 0L;
        }
        return overdueInstallments.stream().map(LoanRepaymentScheduleInstallment::getDueDate)
                .map(dueDate -> DateUtils.getDifferenceInDays(dueDate, projectedFutureDate)).max(Long::compareTo).orElse(0L);
    }

    private BigDecimal calculatePenaltyForOverdueInstallments(List<LoanRepaymentScheduleInstallment> overdueInstallments,
            LocalDate projectedFutureDate) {
        return overdueInstallments.stream().map(installment -> calculatePenaltyCharge(installment, projectedFutureDate))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePenaltyCharge(LoanRepaymentScheduleInstallment installment, LocalDate projectedFutureDate) {
        LocalDate dueDate = installment.getDueDate();
        long daysInArrears = DateUtils.getDifferenceInDays(dueDate, projectedFutureDate);
        Loan loan = installment.getLoan();
        Long loanId = loan.getId();
        final MonetaryCurrency currency = loan.getCurrency();

        Collection<ChargeData> overdueCharges = this.chargeReadPlatformService
                .retrieveLoanProductCharges(loan.getLoanProduct().getId(), ChargeTimeType.OVERDUE_INSTALLMENT).stream()
                .filter(ChargeData::isPenalty).toList();
        Long chargeId = overdueCharges.stream().filter(i -> i.getParentChargeId() == null).findFirst().map(ChargeData::getId).orElse(null);
        if (chargeId == null) {
            chargeId = overdueCharges.stream().findFirst().map(ChargeData::getParentChargeId).orElse(null);
        }
        if (chargeId == null) {
            return BigDecimal.ZERO;
        }
        Charge chargeDefinition = this.chargeRepository.findOneWithNotFoundDetection(chargeId);
        if (Objects.isNull(chargeDefinition)) {
            return BigDecimal.ZERO;
        }
        final String locale = Locale.ENGLISH.toLanguageTag();
        final String dateFormat = "yyyy-MM-dd";
        String json = String.format(
                "{\"chargeId\":%d, \"locale\":\"%s\", \"amount\":%.2f, \"dateFormat\":\"%s\", \"dueDate\":\"%s\", \"principal\":\"%s\", \"interest\":\"%s\"}",
                chargeId, locale, chargeDefinition.getAmount(), dateFormat, installment.getDueDate(),
                installment.getPrincipalOutstanding(currency).getAmount(), installment.getInterestOutstanding(currency));

        final JsonElement parsedCommand = this.fromApiJsonHelper.parse(json);
        final JsonCommand command = JsonCommand.from(json, parsedCommand, this.fromApiJsonHelper, null, null, null, null, null, loanId,
                null, null, null, null, null, null, null);

        final LoanCharge loanCharge = loanChargeAssembler.createNewFromJson(loan, chargeDefinition, command, installment.getDueDate(),
                installment, null);

        if (loanCharge == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal penaltyAmount = BigDecimal.ZERO;
        if (daysInArrears > 0) {
            BigDecimal dailyChargeAmount = loanCharge.amount();
            penaltyAmount = dailyChargeAmount.multiply(BigDecimal.valueOf(daysInArrears));
        }

        return penaltyAmount.setScale(2, RoundingMode.HALF_UP);
    }

}
