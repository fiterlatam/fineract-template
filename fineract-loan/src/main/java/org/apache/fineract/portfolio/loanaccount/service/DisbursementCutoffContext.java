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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ThreadLocal context to store disbursement cutoff calculation results to avoid duplicate calculations during
 * disbursement operations.
 */
public class DisbursementCutoffContext {

    private static final Logger log = LoggerFactory.getLogger(DisbursementCutoffContext.class);

    private static final ThreadLocal<ImmutablePair<Integer, LocalDate>> pairContext = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> isAfterCutoffContext = new ThreadLocal<>();
    private static final ThreadLocal<LocalDate> cutoffDate = new ThreadLocal<>();
    private static final ThreadLocal<Integer> installmentsBeforeCutoff = new ThreadLocal<>();
    private static final ThreadLocal<Integer> numberOfNewInstallments = new ThreadLocal<>();

    /**
     * Set the cutoff calculation result pair
     *
     * @param pair
     *            the cutoff calculation result as ImmutablePair
     */
    public static void setCutoffPair(ImmutablePair<Integer, LocalDate> pair) {
        pairContext.set(pair);
    }

    /**
     * Set the isAfterCutoff flag
     *
     * @param isAfterCutoff
     *            whether the disbursement is after cutoff
     */
    public static void setIsAfterCutoff(boolean isAfterCutoff) {
        isAfterCutoffContext.set(isAfterCutoff);
    }

    /**
     * Get the cutoff calculation result pair
     *
     * @return the cutoff calculation result pair, or null if not set
     */
    public static ImmutablePair<Integer, LocalDate> getCutoffPair() {
        return pairContext.get();
    }

    /**
     * Get the isAfterCutoff flag
     *
     * @return the isAfterCutoff flag, or null if not set
     */
    public static Boolean getIsAfterCutoff() {
        return Boolean.TRUE.equals(isAfterCutoffContext.get());
    }

    /**
     * Set the cutoff date
     *
     * @param date
     *            the cutoff date
     */
    public static void setCutoffDate(LocalDate date) {
        cutoffDate.set(date);
    }

    /**
     * Get the cutoff date
     *
     * @return the cutoff date, or null if not set
     */
    public static LocalDate getCutoffDate() {
        return cutoffDate.get();
    }

    /**
     * Set the number of installments before the cutoff
     *
     * @param installments
     *            the number of installments before the cutoff
     */
    public static void setInstallmentsBeforeCutoff(Integer installments) {
        installmentsBeforeCutoff.set(installments);
    }

    /**
     * Get the number of installments before the cutoff
     *
     * @return the number of installments before the cutoff, or null if not set
     */
    public static Integer getInstallmentsBeforeCutoff() {

        return installmentsBeforeCutoff.get() != null ? installmentsBeforeCutoff.get() : 0;
    }

    /**
     * Clear the context for the current thread
     */
    public static void clear() {
        pairContext.remove();
        isAfterCutoffContext.remove();
        cutoffDate.remove();
        installmentsBeforeCutoff.remove();
    }

    /**
     * Determine if schedule recalculation is needed based on cutoff logic
     *
     * @return true if schedule recalculation is needed, false otherwise
     */
    public static boolean shouldRecalculateSchedule() {
        Boolean isAfterCutoff = getIsAfterCutoff();

        if (isAfterCutoff == null) {
            // Fallback: if context wasn't set, default to no recalculation for safety
            log.warn("DisbursementCutoffContext.isAfterCutoff was not set, defaulting to no recalculation");
            return false;
        }

        // Recalculate if disbursement is BEFORE cutoff (not after cutoff)
        // This means we should modify existing installments rather than create new ones
        return !isAfterCutoff;
    }

    /**
     * Determine if schedule recalculation is needed with fallback calculation
     *
     * @param loan
     *            the loan being processed
     * @param disbursementDate
     *            the actual disbursement date
     * @param calculateInstallmentsToAddFunction
     *            function to calculate installments if context is not set
     * @return true if schedule recalculation is needed, false otherwise
     */
    public static boolean shouldRecalculateSchedule(Loan loan, LocalDate disbursementDate,
            java.util.function.BiFunction<Loan, LocalDate, ImmutablePair<Integer, LocalDate>> calculateInstallmentsToAddFunction) {
        Boolean isAfterCutoff = getIsAfterCutoff();

        if (isAfterCutoff == null) {
            // Fallback: calculate it now if context wasn't set
            log.warn("DisbursementCutoffContext.isAfterCutoff was not set for loan {}, calculating cutoff result on-demand", loan.getId());
            ImmutablePair<Integer, LocalDate> pair = calculateInstallmentsToAddFunction.apply(loan, disbursementDate);

            if (pair != null) {
                // Determine if after cutoff based on the result
                // If installmentsToAdd > 0, it's after cutoff (we need new installments)
                isAfterCutoff = (pair.getLeft() != null && pair.getLeft() > 0);
            } else {
                // No result means not after cutoff (safe default)
                isAfterCutoff = false;
            }
        }

        // Recalculate if disbursement is BEFORE cutoff (not after cutoff)
        // This means we should modify existing installments rather than create new ones
        return !isAfterCutoff;
    }

    /**
     * Check if the context is set for the current thread
     *
     * @return true if context is set, false otherwise
     */
    public static boolean isContextSet() {
        return pairContext.get() != null || isAfterCutoffContext.get() != null || cutoffDate.get() != null
                || installmentsBeforeCutoff.get() != null;
    }

    /**
     * Check if the pair context is set
     *
     * @return true if pair context is set, false otherwise
     */
    public static boolean isPairContextSet() {
        return pairContext.get() != null;
    }

    /**
     * Check if the isAfterCutoff context is set
     *
     * @return true if isAfterCutoff context is set, false otherwise
     */
    public static boolean isAfterCutoffContextSet() {
        return isAfterCutoffContext.get() != null;
    }

    /**
     * Check if the cutoff date is set
     *
     * @return true if cutoff date is set, false otherwise
     */
    public static boolean isCutoffDateSet() {
        return cutoffDate.get() != null;
    }

    /**
     * Check if the installments before cutoff is set
     *
     * @return true if installments before cutoff is set, false otherwise
     */
    public static boolean isInstallmentsBeforeCutoffSet() {
        return installmentsBeforeCutoff.get() != null;
    }


    public static Integer getNumberOfNewInstallments() {
        return numberOfNewInstallments.get() != null ? numberOfNewInstallments.get() : 0;
    }

    public static void setNumberOfNewInstallments(Integer numberOfNewInstallmentsValue) {
        numberOfNewInstallments.set(numberOfNewInstallmentsValue);
    }

}
