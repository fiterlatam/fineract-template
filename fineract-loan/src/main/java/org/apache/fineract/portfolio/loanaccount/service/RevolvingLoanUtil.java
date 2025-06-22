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
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public class RevolvingLoanUtil {

    /**
     * Returns the cutoff date for a given repayment date based on business rules: - If day < 10, set repayment day to 1
     * - If day >= 10 and < 20, set repayment day to 10 - If day >= 20, set repayment day to 20 Then: - If repayment day
     * is 1, cutoff is 15th of previous month - If repayment day is 10, cutoff is 25th of previous month - If repayment
     * day is 20, cutoff is 5th of previous month
     *
     * @param repaymentDate
     *            the original repayment date
     * @return the calculated cutoff date
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
        // Set the normalized repayment date
        LocalDate normalizedRepaymentDate = repaymentDate.withDayOfMonth(repaymentDay);
        // Calculate cutoff date based on normalized repayment day
        LocalDate previousMonth = normalizedRepaymentDate.minusMonths(1);
        int cutoffDay = switch (repaymentDay) {
            case 1 -> 15;
            case 10 -> 25;
            case 20 -> 5;
            default ->
                throw new GeneralPlatformDomainRuleException("error.msg.invalid.repayment.day", "Invalid repayment day: " + repaymentDay);
        };
        // Return cutoff date in previous month
        return previousMonth.withDayOfMonth(cutoffDay);
    }

    /**
     * Returns true if the given date is after the cutoff date for the given repayment (due) date.
     *
     * @param date
     *            the date to check
     * @param repaymentDate
     *            the repayment (due) date
     * @return true if date is after the cutoff date, false otherwise
     */
    public static boolean isAfterCutoff(LocalDate date, LocalDate repaymentDate) {
        LocalDate cutoffDate = calculateCutoffDate(repaymentDate);
        // Only return true if date is after cutoff and on or before repayment date
        return date.isAfter(cutoffDate) && (date.isBefore(repaymentDate) || date.isEqual(repaymentDate));
    }

}
