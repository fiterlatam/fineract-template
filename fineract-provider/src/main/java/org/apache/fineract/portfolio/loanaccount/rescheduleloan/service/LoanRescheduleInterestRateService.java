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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTermVariationType;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanRescheduleInterestRateService {

    /**
     * Handles the interest rate change for an installment that contains the reschedule date
     */
    public void handleMidInstallmentInterestRateChange(Loan loan, LoanRescheduleRequest rescheduleRequest) {
        BigDecimal newInterestRate = getNewInterestRateFromRequest(rescheduleRequest);
        if (newInterestRate == null) {
            log.info("No interest rate change found in reschedule request");
            return;
        }

        LocalDate rescheduleFromDate = rescheduleRequest.getRescheduleFromDate();
        if (rescheduleFromDate == null) {
            log.info("Reschedule date is null, skipping interest rate change");
            return;
        }

        log.info("Starting interest rate change process - Reschedule date: {}, New rate: {}", rescheduleFromDate, newInterestRate);

        List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
        boolean changesMade = false;

        // Process all installments
        for (LoanRepaymentScheduleInstallment installment : installments) {
            log.info("Processing installment {}: {} to {}", installment.getInstallmentNumber(), installment.getFromDate(),
                    installment.getDueDate());

            // Skip if installment is already paid
            if (installment.isObligationsMet()) {
                log.info("Skipping paid installment {}", installment.getInstallmentNumber());
                continue;
            }

            Money originalInterest = installment.getInterestCharged(loan.getCurrency());

            // If installment contains reschedule date, split the interest calculation
            if (!DateUtils.isBefore(rescheduleFromDate, installment.getFromDate())
                    && !DateUtils.isAfter(rescheduleFromDate, installment.getDueDate())) {

                log.info("Splitting interest calculation for installment {} at date {}", installment.getInstallmentNumber(),
                        rescheduleFromDate);

                BigDecimal totalInterest = calculateSplitPeriodInterest(installment, rescheduleFromDate, newInterestRate,
                        loan.getCurrency());

                logInstallment(installment, originalInterest, totalInterest);

                updateInstallmentInterest(installment, totalInterest, loan.getCurrency());
                changesMade = true;
            }
            // If installment is after reschedule date, use new rate entirely
            else if (DateUtils.isAfter(installment.getFromDate(), rescheduleFromDate)) {
                log.info("Applying new rate to installment {}", installment.getInstallmentNumber());

                BigDecimal newInterest = calculateInterestForPeriod(installment.getPrincipalOutstanding(loan.getCurrency()).getAmount(),
                        newInterestRate, ChronoUnit.DAYS.between(installment.getFromDate(), installment.getDueDate()));

                logInstallment(installment, originalInterest, newInterest);

                updateInstallmentInterest(installment, newInterest, loan.getCurrency());
                changesMade = true;
            }
        }

        if (changesMade) {
            log.info("Successfully updated interest rates from date {} with new rate {}", rescheduleFromDate, newInterestRate);
        } else {
            log.info("No changes were made to interest rates");
        }

    }

    private static void logInstallment(LoanRepaymentScheduleInstallment installment, Money originalInterest, BigDecimal totalInterest) {
        log.info("Installment {} - Original interest: {}, New interest: {}", installment.getInstallmentNumber(), originalInterest,
                totalInterest);
    }

    /**
     * Extracts the new interest rate from the reschedule request if it exists
     */
    private BigDecimal getNewInterestRateFromRequest(LoanRescheduleRequest rescheduleRequest) {
        if (rescheduleRequest == null) {
            log.info("Reschedule request is null");
            return null;
        }

        if (rescheduleRequest.getLoanRescheduleRequestToTermVariationMappings() == null) {
            log.info("No term variation mappings found in reschedule request");
            return null;
        }

        log.info("Checking term variations for interest rate change");

        return rescheduleRequest.getLoanRescheduleRequestToTermVariationMappings().stream().filter(mapping -> {
            LoanTermVariationType termType = mapping.getLoanTermVariations().getTermType();
            boolean isInterestRate = termType != null
                    && (termType.isInterestRateVariation() || termType == LoanTermVariationType.INTEREST_RATE_FROM_INSTALLMENT);
            log.info("Checking term variation: type={}, isInterestRate={}, termValue={}", termType, isInterestRate,
                    mapping.getLoanTermVariations().getTermValue());
            return isInterestRate;
        }).map(mapping -> {
            BigDecimal rate = mapping.getLoanTermVariations().getTermValue();
            log.info("Found interest rate change: {}", rate);
            return rate;
        }).findFirst().orElseGet(() -> {
            log.info("No interest rate change found in term variations");
            return null;
        });
    }

    /**
     * Calculates interest for an installment that spans two different interest rates
     */
    private BigDecimal calculateSplitPeriodInterest(LoanRepaymentScheduleInstallment installment, LocalDate rescheduleDate,
            BigDecimal newRate, MonetaryCurrency currency) {
        LocalDate fromDate = installment.getFromDate();
        LocalDate dueDate = installment.getDueDate();

        // Calculate days in each period
        long firstPeriodDays = ChronoUnit.DAYS.between(fromDate, rescheduleDate);
        long secondPeriodDays = ChronoUnit.DAYS.between(rescheduleDate, dueDate);
        long totalDays = ChronoUnit.DAYS.between(fromDate, dueDate);

        log.info("Split period calculation - First period: {} days ({} to {}), Second period: {} days ({} to {})", firstPeriodDays,
                fromDate, rescheduleDate, secondPeriodDays, rescheduleDate, dueDate);

        // Get original rate from installment
        Money interestCharged = installment.getInterestCharged(currency);
        Money principalOutstanding = installment.getPrincipalOutstanding(currency);

        log.info("Original values - Principal: {}, Interest: {}", principalOutstanding.getAmount(), interestCharged.getAmount());

        // Calculate original annual rate
        BigDecimal originalRate = interestCharged.getAmount().multiply(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(365))
                .divide(principalOutstanding.getAmount().multiply(BigDecimal.valueOf(totalDays)), 8, RoundingMode.HALF_UP);

        log.info("Rate calculation - Original annual rate: {}%, New annual rate: {}%", originalRate, newRate);

        // Calculate daily rates
        BigDecimal originalDailyRate = originalRate.divide(BigDecimal.valueOf(365), 8, RoundingMode.HALF_UP);
        BigDecimal newDailyRate = newRate.divide(BigDecimal.valueOf(365), 8, RoundingMode.HALF_UP);

        log.info("Daily rates - Original: {}%, New: {}%", originalDailyRate, newDailyRate);

        // Calculate interest for each period
        BigDecimal firstPeriodInterest = principalOutstanding.getAmount().multiply(originalDailyRate)
                .multiply(BigDecimal.valueOf(firstPeriodDays)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        log.info("First period interest calculation - Principal: {} * Daily Rate: {}% * Days: {} = {}", principalOutstanding.getAmount(),
                originalDailyRate, firstPeriodDays, firstPeriodInterest);

        BigDecimal secondPeriodInterest = principalOutstanding.getAmount().multiply(newDailyRate)
                .multiply(BigDecimal.valueOf(secondPeriodDays)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        log.info("Second period interest calculation - Principal: {} * Daily Rate: {}% * Days: {} = {}", principalOutstanding.getAmount(),
                newDailyRate, secondPeriodDays, secondPeriodInterest);

        BigDecimal totalInterest = firstPeriodInterest.add(secondPeriodInterest);

        log.info("Total interest calculation - First period: {} + Second period: {} = Total: {}", firstPeriodInterest, secondPeriodInterest,
                totalInterest);

        return totalInterest;
    }

    private BigDecimal calculateInterestForPeriod(BigDecimal principal, BigDecimal rate, long days) {
        BigDecimal interest = principal.multiply(rate.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);

        log.info("Interest calculation - Principal: {}, Rate: {}, Days: {}, Interest: {}", principal, rate, days, interest);

        return interest;
    }

    private void updateInstallmentInterest(LoanRepaymentScheduleInstallment installment, BigDecimal newTotalInterest,
            MonetaryCurrency currency) {
        // Update the interest amount
        installment.setInterestCharged(newTotalInterest);

        // Recalculate principal if needed
        Money principalOutstanding = installment.getPrincipalOutstanding(currency);
        installment.setPrincipal(principalOutstanding.getAmount());
    }
}
