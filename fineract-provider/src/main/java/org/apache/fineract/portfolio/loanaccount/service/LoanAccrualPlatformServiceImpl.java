/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.loanaccount.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.configuration.service.TemporaryConfigurationServiceContainer;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.data.LoanScheduleAccrualData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanApplicationTerms;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGeneratorFactory;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.PrincipalInterest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class LoanAccrualPlatformServiceImpl implements LoanAccrualPlatformService {

    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanAccrualWritePlatformService loanAccrualWritePlatformService;
    private final LoanAssembler loanAssembler;
    private final LoanRepository loanRepository;
    private final LoanUtilService loanUtilService;
    private final JdbcTemplate jdbcTemplate;
    private final LoanScheduleGeneratorFactory loanScheduleFactory;
    private final ConfigurationDomainService configurationDomainService;

    @Override
    public void addPeriodicAccruals(final LocalDate tillDate) throws JobExecutionException {
        Collection<LoanScheduleAccrualData> loanScheduleAccrualDataList = this.loanReadPlatformService
                .retrievePeriodicAccrualData(tillDate);
        addPeriodicAccruals(tillDate, loanScheduleAccrualDataList);
    }

    @Override
    public void addPeriodicAccruals(final LocalDate tillDate, Loan loan) throws JobExecutionException {
        Collection<LoanScheduleAccrualData> loanScheduleAccrualDataList = this.loanReadPlatformService.retrievePeriodicAccrualData(tillDate,
                loan);
        addPeriodicAccruals(tillDate, loanScheduleAccrualDataList);
    }

    @Override
    public void addPeriodicAccruals(final LocalDate tillDate, Collection<LoanScheduleAccrualData> loanScheduleAccrualDataList)
            throws JobExecutionException {
        Map<Long, Collection<LoanScheduleAccrualData>> loanDataMap = new HashMap<>();
        for (final LoanScheduleAccrualData accrualData : loanScheduleAccrualDataList) {
            if (loanDataMap.containsKey(accrualData.getLoanId())) {
                loanDataMap.get(accrualData.getLoanId()).add(accrualData);
            } else {
                Collection<LoanScheduleAccrualData> accrualDataList = new ArrayList<>();
                accrualDataList.add(accrualData);
                loanDataMap.put(accrualData.getLoanId(), accrualDataList);
            }
        }

        List<Throwable> errors = new ArrayList<>();
        for (Map.Entry<Long, Collection<LoanScheduleAccrualData>> mapEntry : loanDataMap.entrySet()) {
            try {
                this.loanAccrualWritePlatformService.addPeriodicAccruals(tillDate, mapEntry.getKey(), mapEntry.getValue());
            } catch (Exception e) {
                log.error("Failed to add accrual transaction for loan {}", mapEntry.getKey(), e);
                errors.add(e);
            }
        }
        if (!errors.isEmpty()) {
            throw new JobExecutionException(errors);
        }
    }

    @Override
    @Transactional
    public void persistDailyInterestAccrual(final Long loanId, final LocalDate accrualDate) {
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        final String claimType = loan.claimType();
        if (claimType != null && claimType.equalsIgnoreCase("guarantor")) {
            return;
        }
        final Long minimumDaysInArrearsToSuspendLoanAccount = this.configurationDomainService
                .retriveMinimumDaysInArrearsToSuspendLoanAccount();
        LocalDate lastInterestAccrualDate = loan.getInterestAccruedTill() != null ? loan.getInterestAccruedTill()
                : loan.getDisbursementDate();
        // Loop through the days between the last accrual date and accrual date and process interest accrual for each
        // day
        log.info("Persisting daily accrual for loan: {}", loan.getId());
        while (lastInterestAccrualDate.isBefore(accrualDate)) {
            lastInterestAccrualDate = lastInterestAccrualDate.plusDays(1);
            this.processInterestAccrualForDate(lastInterestAccrualDate, loan, minimumDaysInArrearsToSuspendLoanAccount);
        }
        log.info("Daily accrual persisted for loan: {}", loan.getId());
    }

    private void processInterestAccrualForDate(final LocalDate accrualDate, Loan loan, Long minimumDaysInArrearsToSuspendLoanAccount) {
        final MonetaryCurrency currency = loan.getCurrency();
        ExternalId externalIdentifier = ExternalId.empty();
        if (TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
            externalIdentifier = ExternalId.generate();
        }
        final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments = loan.getRepaymentScheduleInstallments();
        BigDecimal dailyAccrualInterest = null;
        Integer accrualInstallmentNumber = null;
        Money principalLoanBalanceOutstanding = loan.getPrincipal();
        for (final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : repaymentScheduleInstallments) {
            if (!accrualDate.isBefore(loanRepaymentScheduleInstallment.getFromDate())
                    && !accrualDate.isAfter(loanRepaymentScheduleInstallment.getDueDate().minusDays(1))) {
                final BigDecimal totalAccruedInterestForInstallment = loan
                        .getAccruedInterestForInstallment(loanRepaymentScheduleInstallment.getInstallmentNumber());
                long daysInPeriod = Math.toIntExact(ChronoUnit.DAYS.between(loanRepaymentScheduleInstallment.getFromDate(),
                        loanRepaymentScheduleInstallment.getDueDate()));
                Money interestForInstallment = loanRepaymentScheduleInstallment.getInterestOutstanding(currency);
                // Adjust interest on the last day of the period to make up the difference
                if (accrualDate.equals(loanRepaymentScheduleInstallment.getDueDate().minusDays(1))) {
                    // if the amount is whole number when divided across the days in the period, then do same for
                    // last
                    // day
                    if (interestForInstallment.getAmount().remainder(BigDecimal.valueOf(daysInPeriod)).compareTo(BigDecimal.ZERO) == 0) {
                        dailyAccrualInterest = interestForInstallment.getAmount().divide(BigDecimal.valueOf(daysInPeriod), 2,
                                RoundingMode.HALF_UP);
                    } else {
                        // This will ensure no rounding differences remain
                        if (interestForInstallment.getAmount().compareTo(totalAccruedInterestForInstallment) > 0) {
                            dailyAccrualInterest = interestForInstallment.getAmount().subtract(totalAccruedInterestForInstallment);
                        } else {
                            dailyAccrualInterest = BigDecimal.ZERO;
                        }
                    }
                } else {
                    LocalDate periodStartDate = loanRepaymentScheduleInstallment.getFromDate();
                    LocalDate periodEndDate = accrualDate;
                    LoanTermVariationsData loanTermVariationsData = this.getLoanTermVariationsDataFor(loan.getId(), periodEndDate);
                    BigDecimal annualNominalInterestRate = loan.getLoanRepaymentScheduleDetail().getAnnualNominalInterestRate();
                    if (loanTermVariationsData != null && loanTermVariationsData.getDecimalValue() != null
                            && loan.getLoanRepaymentScheduleDetail().getAnnualNominalInterestRate() != null) {
                        annualNominalInterestRate = loanTermVariationsData.getDecimalValue();
                        final LocalDate termVariationApplicableFromDate = loanTermVariationsData.getTermVariationApplicableFrom();
                        if (DateUtils.isAfter(termVariationApplicableFromDate, periodStartDate)) {
                            periodStartDate = termVariationApplicableFromDate;
                        }
                    }
                    final ScheduleGeneratorDTO scheduleGeneratorDTO = loanUtilService.buildScheduleGeneratorDTO(loan, null);
                    final LoanApplicationTerms loanApplicationTerms = loan.constructLoanApplicationTerms(scheduleGeneratorDTO);
                    loanApplicationTerms.updateAnnualNominalInterestRate(annualNominalInterestRate);
                    final LoanScheduleGenerator loanScheduleGenerator = this.loanScheduleFactory
                            .create(loanApplicationTerms.getLoanScheduleType(), loanApplicationTerms.getInterestMethod());
                    final int periodNumber = loanRepaymentScheduleInstallment.getInstallmentNumber();
                    if (DateUtils.isEqual(periodStartDate, periodEndDate)) {
                        periodEndDate = periodEndDate.plusDays(1);
                    }
                    final Money principalAccountedFor = loanRepaymentScheduleInstallment.getPrincipalAccountedFor(currency);
                    principalLoanBalanceOutstanding = principalLoanBalanceOutstanding.minus(principalAccountedFor);
                    final boolean ignoreCurrencyDigitsAfterDecimal = false;
                    final PrincipalInterest principalInterest = loanScheduleGenerator.calculatePrincipalInterestComponents(
                            principalLoanBalanceOutstanding, loanApplicationTerms, periodNumber, periodStartDate, periodEndDate,
                            ignoreCurrencyDigitsAfterDecimal);
                    final int daysDifference = Math.toIntExact(ChronoUnit.DAYS.between(periodStartDate, periodEndDate));
                    dailyAccrualInterest = principalInterest.interest().getAmount().divide(BigDecimal.valueOf(daysDifference), 2,
                            RoundingMode.HALF_UP);
                }
                // Accumulate the daily interest to the installment's accrued interest
                dailyAccrualInterest = dailyAccrualInterest.setScale(0, RoundingMode.DOWN);
                final BigDecimal accruedInterest = totalAccruedInterestForInstallment.add(dailyAccrualInterest);
                loanRepaymentScheduleInstallment.setInterestAccrued(accruedInterest);
                accrualInstallmentNumber = loanRepaymentScheduleInstallment.getInstallmentNumber();
                break;
            } else {
                final Money principalAccountedFor = loanRepaymentScheduleInstallment.getPrincipalAccountedFor(currency);
                principalLoanBalanceOutstanding = principalLoanBalanceOutstanding.minus(principalAccountedFor);
            }
        }

        if (dailyAccrualInterest != null && dailyAccrualInterest.compareTo(BigDecimal.ZERO) > 0) {
            final Money dailyInterestMoney = Money.of(currency, dailyAccrualInterest);
            final LoanTransaction dailyAccrualTransaction = LoanTransaction.accrueDailyInterest(loan.getOffice(), loan, dailyInterestMoney,
                    accrualDate, externalIdentifier, accrualInstallmentNumber);
            final long daysInArrears = this.getDaysInArrears(loan.getId());
            if (minimumDaysInArrearsToSuspendLoanAccount == null) {
                minimumDaysInArrearsToSuspendLoanAccount = 90L;
            }
            if (daysInArrears >= minimumDaysInArrearsToSuspendLoanAccount) {
                dailyAccrualTransaction.markAsOccurredOnSuspendedAccount();
            }
            if (!this.accrualExistsForDate(accrualDate, loan.getId())) {
                loan.addLoanTransaction(dailyAccrualTransaction);
            }
        }
        loan.setInterestAccruedTill(accrualDate);
        this.loanRepository.saveAndFlush(loan);
    }

    private LoanTermVariationsData getLoanTermVariationsDataFor(Long loanId, LocalDate periodEndDate) {
        String query = "select decimal_value, applicable_date, is_specific_to_installment from m_loan_term_variations where loan_id = ? "
                + " and applicable_date <= ? order by last_modified_on_utc desc";
        List<LoanTermVariationsData> results = this.jdbcTemplate.query(query, (rs, rowNum) -> {
            final BigDecimal decimalValue = rs.getBigDecimal("decimal_value");
            final LocalDate applicableDate = JdbcSupport.getLocalDate(rs, "applicable_date");
            final boolean isSpecificToInstallment = rs.getBoolean("is_specific_to_installment");
            return new LoanTermVariationsData(loanId, null, applicableDate, decimalValue, null, isSpecificToInstallment);
        }, loanId, periodEndDate);
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    private long getDaysInArrears(final Long loanId) {
        String query = "SELECT overdue_since_date_derived FROM m_loan_arrears_aging WHERE loan_id = ?";
        List<LocalDate> results = this.jdbcTemplate.queryForList(query, LocalDate.class, loanId);
        if (results.isEmpty()) {
            return 0;
        }
        LocalDate overdueSinceDate = results.get(0);
        return overdueSinceDate != null ? ChronoUnit.DAYS.between(overdueSinceDate, DateUtils.getLocalDateOfTenant()) : 0;
    }

    private boolean accrualExistsForDate(final LocalDate accrualDate, final Long loanId) {
        String sql = "SELECT id FROM m_loan_transaction WHERE transaction_type_enum = ? AND transaction_date = ? AND loan_id = ? AND is_reversed = false And is_daily_accrual = true";
        List<Long> results = this.jdbcTemplate.queryForList(sql, Long.class, LoanTransactionType.ACCRUAL.getValue(), accrualDate, loanId);
        return !results.isEmpty();
    }
}
