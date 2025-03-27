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

        // Check if there are any interest rate changes on or before the accrual date
        // If so, we need to make sure we're using the correct interest rate
        LoanTermVariationsData interestRateChange = this.getLoanTermVariationsDataFor(loanId, accrualDate);
        if (interestRateChange != null && interestRateChange.getDecimalValue() != null) {
            log.info("Loan {} has interest rate change applicable from {} with rate {}. Accrual date: {}", loan.getId(),
                    interestRateChange.getTermVariationApplicableFrom(), interestRateChange.getDecimalValue(), accrualDate);
        }

        // Check if there are any special write-off or Credit Note transactions on the accrual date
        // If so, we need to make sure we're using the correct principal balance
        boolean hasSpecialWriteOffOrCreditNoteOnAccrualDate = loan.getLoanTransactions().stream().anyMatch(
                transaction -> (transaction.isSpecialWriteOff() || transaction.getTypeOf().equals(LoanTransactionType.CREDIT_NOTE))
                        && !transaction.isReversed() && transaction.getTransactionDate().isEqual(accrualDate));

        // If there are special write-off or Credit Note transactions on the accrual date,
        // we need to refresh the loan to ensure we have the most up-to-date principal balance
        if (hasSpecialWriteOffOrCreditNoteOnAccrualDate) {
            log.info("Loan {} has special write-off or Credit Note transactions on accrual date {}. Refreshing loan data.", loan.getId(),
                    accrualDate);
            // Force a refresh of the loan to ensure we have the most up-to-date data
            this.loanRepository.saveAndFlush(loan);
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

    @SuppressWarnings("all")
    private void processInterestAccrualForDate(final LocalDate accrualDate, Loan loan, Long minimumDaysInArrearsToSuspendLoanAccount) {
        final MonetaryCurrency currency = loan.getCurrency();
        ExternalId externalIdentifier = ExternalId.empty();
        if (TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
            externalIdentifier = ExternalId.generate();
        }
        final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments = loan.getRepaymentScheduleInstallments();
        BigDecimal dailyAccrualInterest = null;
        Integer accrualInstallmentNumber = null;

        // Start with the total principal disbursed as the outstanding balance
        Money principalLoanBalanceOutstanding = Money.of(currency, loan.getLoanSummary().getTotalPrincipalDisbursed());

        // Get the applicable interest rate for the accrual date
        // This ensures we're using the correct interest rate after any maximum legal rate changes
        LoanTermVariationsData interestRateChange = this.getLoanTermVariationsDataFor(loan.getId(), accrualDate);
        BigDecimal annualNominalInterestRate = loan.getLoanRepaymentScheduleDetail().getAnnualNominalInterestRate();
        if (interestRateChange != null && interestRateChange.getDecimalValue() != null) {
            annualNominalInterestRate = interestRateChange.getDecimalValue();
            log.debug("Using interest rate {} for loan {} on accrual date {} (rate change applicable from {})", annualNominalInterestRate,
                    loan.getId(), accrualDate, interestRateChange.getTermVariationApplicableFrom());
        }

        for (final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : repaymentScheduleInstallments) {
            if (!accrualDate.isBefore(loanRepaymentScheduleInstallment.getFromDate())
                    && !accrualDate.isAfter(loanRepaymentScheduleInstallment.getDueDate().minusDays(1))) {
                final BigDecimal totalAccruedInterestForInstallment = loan
                        .getAccruedInterestForInstallment(loanRepaymentScheduleInstallment.getInstallmentNumber());
                LocalDate periodStartDate = loanRepaymentScheduleInstallment.getFromDate();
                LocalDate periodEndDate = accrualDate;

                // We've already retrieved the interest rate change above, so we don't need to do it again here
                // Just use the periodStartDate adjustment logic if needed
                if (interestRateChange != null && interestRateChange.getDecimalValue() != null) {
                    final LocalDate termVariationApplicableFromDate = interestRateChange.getTermVariationApplicableFrom();
                    if (DateUtils.isAfter(termVariationApplicableFromDate, periodStartDate)) {
                        periodStartDate = termVariationApplicableFromDate;
                    }
                }

                final ScheduleGeneratorDTO scheduleGeneratorDTO = loanUtilService.buildScheduleGeneratorDTO(loan, null);
                final LoanApplicationTerms loanApplicationTerms = loan.constructLoanApplicationTerms(scheduleGeneratorDTO);
                if (loan.getLoanProduct().isInterestStartsAfterGracePeriod()) {
                    // Remove any grace period since this is already defined on the schedule
                    loanApplicationTerms.getPeriodNumbersApplicableForInterestGrace().clear();
                    loanApplicationTerms.setInterestChargingGrace(0);
                }
                loanApplicationTerms.updateAnnualNominalInterestRate(annualNominalInterestRate);
                final LoanScheduleGenerator loanScheduleGenerator = this.loanScheduleFactory
                        .create(loanApplicationTerms.getLoanScheduleType(), loanApplicationTerms.getInterestMethod());
                final int periodNumber = loanRepaymentScheduleInstallment.getInstallmentNumber();
                if (DateUtils.isEqual(periodStartDate, periodEndDate)) {
                    periodEndDate = periodEndDate.plusDays(1);
                }

                // We no longer need to adjust the principal balance here since we're using the actual outstanding
                // balance
                // from the loan summary
                principalLoanBalanceOutstanding = principalLoanBalanceOutstanding
                        .minus(loanRepaymentScheduleInstallment.getPrincipalAccountedFor(currency));
                final boolean ignoreCurrencyDigitsAfterDecimal = false;
                final boolean truncateInterestAmount = true;
                final PrincipalInterest principalInterest = loanScheduleGenerator.calculatePrincipalInterestComponents(
                        principalLoanBalanceOutstanding, loanApplicationTerms, periodNumber, periodStartDate, periodEndDate,
                        ignoreCurrencyDigitsAfterDecimal, truncateInterestAmount);
                final int daysDifference = Math.toIntExact(ChronoUnit.DAYS.between(periodStartDate, periodEndDate));
                dailyAccrualInterest = principalInterest.interest().getAmount().divide(BigDecimal.valueOf(daysDifference), 2,
                        RoundingMode.HALF_UP);
                // Accumulate the daily interest to the installment's accrued interest
                dailyAccrualInterest = dailyAccrualInterest.setScale(0, RoundingMode.DOWN);
                final BigDecimal accruedInterest = totalAccruedInterestForInstallment.add(dailyAccrualInterest);
                loanRepaymentScheduleInstallment.setInterestAccrued(accruedInterest);
                accrualInstallmentNumber = loanRepaymentScheduleInstallment.getInstallmentNumber();
                break;
            } else {
                principalLoanBalanceOutstanding = principalLoanBalanceOutstanding
                        .minus(loanRepaymentScheduleInstallment.getPrincipal(currency));
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
        // Modified query to order by applicable_date in descending order to get the most recent applicable interest
        // rate
        // This ensures that we get the correct interest rate for the accrual date, even if there are multiple rate
        // changes
        String query = "select decimal_value, applicable_date, is_specific_to_installment from m_loan_term_variations where loan_id = ? "
                + " and applicable_date <= ? order by applicable_date desc";
        List<LoanTermVariationsData> results = this.jdbcTemplate.query(query, (rs, rowNum) -> {
            final BigDecimal decimalValue = rs.getBigDecimal("decimal_value");
            final LocalDate applicableDate = JdbcSupport.getLocalDate(rs, "applicable_date");
            final boolean isSpecificToInstallment = rs.getBoolean("is_specific_to_installment");
            return new LoanTermVariationsData(loanId, null, applicableDate, decimalValue, null, isSpecificToInstallment);
        }, loanId, periodEndDate);
        if (results.isEmpty()) {
            return null;
        }

        // Log the interest rate change for debugging
        LoanTermVariationsData result = results.get(0);
        log.debug("Loan {} has interest rate change applicable from {} with rate {}", loanId, result.getTermVariationApplicableFrom(),
                result.getDecimalValue());

        return result;
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
