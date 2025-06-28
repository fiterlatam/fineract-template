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

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;

/**
 * ThreadLocal context to store disbursement cutoff calculation results to avoid duplicate calculations during
 * disbursement operations.
 */
@Slf4j
public class DisbursementCutoffContext {

    private static final ThreadLocal<CutoffCalculationResult> cutoffResult = new ThreadLocal<>();

    /**
     * Record containing all cutoff calculation results in a single, immutable structure.
     * This consolidates the previously scattered ThreadLocal variables into a single, cohesive data structure.
     */
    public record CutoffCalculationResult(
            boolean isAfterCutoff,
            int installmentsBeforeCutoff,
            int numberOfNewInstallments,
            LocalDate cutoffDate,
            LocalDate disbursementDate,
            Money disbursementAmount,
            LoanRepaymentScheduleInstallment lastInstallmentBeforeCutoff,
            ImmutablePair<Integer, LocalDate> calculationPair) {

        /**
         * Factory method to create a cutoff result for after-cutoff scenarios
         */
        public static CutoffCalculationResult afterCutoff(
                int installmentsBeforeCutoff,
                int numberOfNewInstallments,
                LocalDate disbursementDate,
                Money disbursementAmount,
                LoanRepaymentScheduleInstallment lastInstallmentBeforeCutoff) {
            
            LocalDate cutoffDate = calculateCutoffDate(lastInstallmentBeforeCutoff.getDueDate());
            ImmutablePair<Integer, LocalDate> pair = ImmutablePair.of(numberOfNewInstallments, disbursementDate);
            
            return new CutoffCalculationResult(
                    true,
                    installmentsBeforeCutoff,
                    numberOfNewInstallments,
                    cutoffDate,
                    disbursementDate,
                    disbursementAmount,
                    lastInstallmentBeforeCutoff,
                    pair
            );
        }

        /**
         * Factory method to create a cutoff result for after-cutoff scenarios using loan data
         */
        public static CutoffCalculationResult afterCutoff(Loan loan, LocalDate disbursementDate, Money disbursementAmount) {
            LoanRepaymentScheduleInstallment lastInstallment = loan.getLastLoanRepaymentScheduleInstallment();
            int installmentsBeforeCutoff = loan.getRepaymentScheduleInstallments().size();
            int numberOfNewInstallments = loan.getTermFrequency();
            
            return afterCutoff(
                installmentsBeforeCutoff,
                numberOfNewInstallments,
                disbursementDate,
                disbursementAmount,
                lastInstallment
            );
        }

        /**
         * Factory method to create a cutoff result for before-cutoff scenarios
         */
        public static CutoffCalculationResult beforeCutoff(
                LocalDate disbursementDate,
                Money disbursementAmount,
                ImmutablePair<Integer, LocalDate> calculationPair) {
            
            return new CutoffCalculationResult(
                    false,
                    0,
                    0,
                    null,
                    disbursementDate,
                    disbursementAmount,
                    null,
                    calculationPair
            );
        }

        /**
         * Factory method to create a cutoff result for before-cutoff scenarios using loan data
         */
        public static CutoffCalculationResult beforeCutoff(Loan loan, LocalDate disbursementDate, Money disbursementAmount, 
                ImmutablePair<Integer, LocalDate> calculationPair) {
            
            return new CutoffCalculationResult(
                    false,
                    0,
                    0,
                    null,
                    disbursementDate,
                    disbursementAmount,
                    null,
                    calculationPair
            );
        }

        /**
         * Calculate the cutoff date based on the last installment due date
         */
        public static LocalDate calculateCutoffDate(LocalDate repaymentDate) {
            int day = repaymentDate.getDayOfMonth();
            int repaymentDay;
            if (day < 10) {
                repaymentDay = 1;
            } else if (day < 20) {
                repaymentDay = 10;
            } else {
                repaymentDay = 20;
            }
            
            LocalDate normalizedRepaymentDate = repaymentDate.withDayOfMonth(repaymentDay);
            LocalDate previousMonth = normalizedRepaymentDate.minusMonths(1);
            
            int cutoffDay = switch (repaymentDay) {
                case 1 -> 15;
                case 10 -> 25;
                case 20 -> 5;
                default -> throw new IllegalArgumentException("Invalid repayment day: " + repaymentDay);
            };
            
            return previousMonth.withDayOfMonth(cutoffDay);
        }

        /**
         * Get the first installment number after cutoff
         */
        public int getFirstInstallmentAfterCutoff() {
            return installmentsBeforeCutoff + 1;
        }

        /**
         * Check if installment calculation is allowed using disbursement amount for the given period
         */
        public boolean isInstallmentCalculationAllowedOnPeriodNumberUsingDisbursementAmount(int periodNumber) {
            return disbursementAmount != null 
                    && installmentsBeforeCutoff > 0 
                    && periodNumber >= installmentsBeforeCutoff
                    && isAfterCutoff 
                    && numberOfNewInstallments > 0;
        }

        /**
         * Check if next repayment exists for the current installment
         */
        public boolean doesNextRepaymentExist(int currentInstallment, boolean isNextRepaymentAvailable) {
            if (!isAfterCutoff) {
                return isNextRepaymentAvailable;
            }

            int firstInstallmentAfterCutoff = getFirstInstallmentAfterCutoff();
            
            // If we're beyond the last new installment, there's no next repayment
            if (firstInstallmentAfterCutoff > 1 && currentInstallment >= (firstInstallmentAfterCutoff + numberOfNewInstallments - 1)) {
                return false;
            }

            return isNextRepaymentAvailable;
        }
    }

    /**
     * Set the cutoff calculation result
     */
    public static void setCutoffResult(CutoffCalculationResult result) {
        cutoffResult.set(result);
    }

    /**
     * Set after-cutoff context using loan data
     */
    public static void setAfterCutoff(Loan loan, LocalDate disbursementDate, ImmutablePair<Integer, LocalDate> pair) {
        Money disbursementAmount = getDisbursementAmount();
        // Create a custom result with the provided pair instead of the default one
        LoanRepaymentScheduleInstallment lastInstallment = loan.getLastLoanRepaymentScheduleInstallment();
        int installmentsBeforeCutoff = loan.getRepaymentScheduleInstallments().size();
        int numberOfNewInstallments = loan.getTermFrequency();
        LocalDate cutoffDate = CutoffCalculationResult.calculateCutoffDate(lastInstallment.getDueDate());
        
        CutoffCalculationResult result = new CutoffCalculationResult(
            true,
            installmentsBeforeCutoff,
            numberOfNewInstallments,
            cutoffDate,
            disbursementDate,
            disbursementAmount,
            lastInstallment,
            pair
        );
        setCutoffResult(result);
    }

    /**
     * Set before-cutoff context using loan data
     */
    public static void setBeforeCutoff(Loan loan, LocalDate disbursementDate, 
            ImmutablePair<Integer, LocalDate> calculationPair) {
        Money disbursementAmount = getDisbursementAmount();
        CutoffCalculationResult result = CutoffCalculationResult.beforeCutoff(loan, disbursementDate, disbursementAmount, calculationPair);
        setCutoffResult(result);
    }

    /**
     * Set the disbursement amount with internal context creation
     */
    public static void setDisbursementAmount(Money disbursementAmount) {
        CutoffCalculationResult currentResult = getCutoffResult();
        LocalDate disbursementDate = currentResult != null ? currentResult.disbursementDate() : null;
        ImmutablePair<Integer, LocalDate> calculationPair = currentResult != null ? currentResult.calculationPair() : null;
        
        CutoffCalculationResult newResult = CutoffCalculationResult.beforeCutoff(
            disbursementDate,
            disbursementAmount,
            calculationPair
        );
        setCutoffResult(newResult);
    }

    /**
     * Get the cutoff calculation result
     */
    public static CutoffCalculationResult getCutoffResult() {
        return cutoffResult.get();
    }

    /**
     * Check if the context is set
     */
    public static boolean isContextSet() {
        return cutoffResult.get() != null;
    }

    /**
     * Clear the context for the current thread
     */
    public static void clear() {
        cutoffResult.remove();
    }

    /**
     * Check if installment calculation is allowed using disbursement amount for the given period
     */
    public static boolean isInstallmentCalculationAllowedUsingDisbursementAmount(int periodNumber) {
        CutoffCalculationResult result = getCutoffResult();
        return result != null && result.isInstallmentCalculationAllowedOnPeriodNumberUsingDisbursementAmount(periodNumber);
    }

    /**
     * Check if next repayment exists for the current installment
     */
    public static boolean doesNextRepaymentExist(int currentInstallment, boolean isNextRepaymentAvailable) {
        CutoffCalculationResult result = getCutoffResult();
        if (result == null) {
            return isNextRepaymentAvailable;
        }
        return result.doesNextRepaymentExist(currentInstallment, isNextRepaymentAvailable);
    }

    /**
     * Get the number of installments before cutoff
     */
    public static int getInstallmentsBeforeCutoff() {
        CutoffCalculationResult result = getCutoffResult();
        return result != null ? result.installmentsBeforeCutoff() : 0;
    }

    /**
     * Get the number of new installments
     */
    public static int getNumberOfNewInstallments() {
        CutoffCalculationResult result = getCutoffResult();
        return result != null ? result.numberOfNewInstallments() : 0;
    }

    /**
     * Get the disbursement amount
     */
    public static Money getDisbursementAmount() {
        CutoffCalculationResult result = getCutoffResult();
        return result != null ? result.disbursementAmount() : null;
    }

    /**
     * Check if the disbursement is after cutoff
     */
    public static boolean isAfterCutoff() {
        CutoffCalculationResult result = getCutoffResult();
        return result != null && result.isAfterCutoff();
    }

    /**
     * Determine if schedule recalculation is needed based on cutoff logic
     */
    public static boolean shouldRecalculateSchedule() {
        CutoffCalculationResult result = getCutoffResult();
        
        if (result == null) {
            log.warn("DisbursementCutoffContext was not set, defaulting to no recalculation");
            return false;
        }

        // Recalculate if disbursement is BEFORE cutoff (not after cutoff)
        // This means we should modify existing installments rather than create new ones
        return !result.isAfterCutoff();
    }

    /**
     * Determine if schedule recalculation is needed with fallback calculation
     */
    public static boolean shouldRecalculateSchedule(Loan loan, LocalDate disbursementDate,
            java.util.function.BiFunction<Loan, LocalDate, ImmutablePair<Integer, LocalDate>> calculateInstallmentsToAddFunction) {
        
        CutoffCalculationResult result = getCutoffResult();

        if (result == null) {
            // Fallback: calculate it now if context wasn't set
            log.warn("DisbursementCutoffContext was not set for loan {}, calculating cutoff result on-demand", loan.getId());
            ImmutablePair<Integer, LocalDate> pair = calculateInstallmentsToAddFunction.apply(loan, disbursementDate);

            if (pair != null && pair.getLeft() != null && pair.getLeft() > 0) {
                // If installmentsToAdd > 0, it's after cutoff (we need new installments)
                return false; // Don't recalculate for after-cutoff scenarios
            } else {
                // No result means not after cutoff (safe default)
                return true; // Recalculate for before-cutoff scenarios
            }
        }

        // Recalculate if disbursement is BEFORE cutoff (not after cutoff)
        return !result.isAfterCutoff();
    }
}
