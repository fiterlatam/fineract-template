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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.springframework.stereotype.Service;

/**
 * Service to handle automatic adjustment of first payment dates when disbursement date is after the configured first
 * repayment date.
 *
 * This service leverages the existing RevolvingLoanUtil for consistent date normalization and follows the same business
 * rules for repayment date calculations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FirstPaymentDateAdjustmentService {

    /**
     * Adjusts the first payment date if needed based on the actual disbursement date. Only makes adjustments if this is
     * the first disbursement.
     *
     * @param loan
     *            The loan to adjust
     * @param disbursementDate
     *            The actual disbursement date
     * @return true if adjustment was made, false otherwise
     */
    public boolean adjustFirstPaymentDateIfNeeded(Loan loan, LocalDate disbursementDate) {
        // Only adjust if this is the first disbursement
        boolean isFirstDisbursement = !loan.isMultiDisburmentLoan()
                || loan.getDisbursementDetails().stream().noneMatch(detail -> detail.actualDisbursementDate() != null);

        if (!isFirstDisbursement) {
            log.debug("Skipping first payment date adjustment for loan {} - not first disbursement", loan.getId());
            return false;
        }

        LocalDate firstRepaymentDate = loan.getExpectedFirstRepaymentOnDate();

        if (firstRepaymentDate == null) {
            log.debug("No first repayment date configured for loan {}", loan.getId());
            return false;
        }

        // Check if disbursement date is after the first repayment date
        if (disbursementDate.isAfter(firstRepaymentDate)) {
            log.info("Disbursement date {} is after first repayment date {} for loan {}. Adjusting first payment date.", disbursementDate,
                    firstRepaymentDate, loan.getId());

            LocalDate adjustedFirstRepaymentDate = calculateAdjustedFirstPaymentDate(loan, disbursementDate);
            loan.setExpectedFirstRepaymentOnDate(adjustedFirstRepaymentDate);

            log.info("Adjusted first repayment date to {} for loan {}", adjustedFirstRepaymentDate, loan.getId());
            return true;
        }

        log.debug("No adjustment needed for loan {} - disbursement date {} is on or before first repayment date {}", loan.getId(),
                disbursementDate, firstRepaymentDate);
        return false;
    }

    /**
     * Calculates the adjusted first payment date based on the loan product frequency and disbursement date.
     *
     * @param loan
     *            the loan
     * @param disbursementDate
     *            the disbursement date
     * @return the adjusted first payment date
     */
    private LocalDate calculateAdjustedFirstPaymentDate(Loan loan, LocalDate disbursementDate) {
        PeriodFrequencyType frequency = loan.repaymentScheduleDetail().getRepaymentPeriodFrequencyType();
        Integer repayEvery = loan.repaymentScheduleDetail().getRepayEvery();

        LocalDate adjustedDate = switch (frequency) {
            case DAYS -> disbursementDate.plusDays(repayEvery);
            case WEEKS -> disbursementDate.plusWeeks(repayEvery);
            case MONTHS -> calculateMonthlyFirstPaymentDate(disbursementDate, repayEvery);
            case YEARS -> disbursementDate.plusYears(repayEvery);
            case INVALID, WHOLE_TERM -> {
                log.warn("Unsupported frequency type {} for loan {}, using default monthly calculation", frequency, loan.getId());
                yield calculateMonthlyFirstPaymentDate(disbursementDate, 1);
            }
        };

        // For revolving loans, normalize to standard repayment days (1, 10, 20)
        if (loan.isRevolvingLoan()) {
            adjustedDate = normalizeRepaymentDateForRevolvingLoan(adjustedDate);
        }

        return adjustedDate;
    }

    /**
     * Calculates the first payment date for monthly frequency loans. Uses the same logic as RevolvingLoanUtil for
     * consistency.
     *
     * @param disbursementDate
     *            the disbursement date
     * @param repayEvery
     *            the repayment frequency (number of months)
     * @return the calculated first payment date
     */
    private LocalDate calculateMonthlyFirstPaymentDate(LocalDate disbursementDate, Integer repayEvery) {
        LocalDate baseDate = disbursementDate.plusMonths(repayEvery);

        // Use the same normalization logic as RevolvingLoanUtil
        int day = baseDate.getDayOfMonth();
        int repaymentDay;

        if (day < 10) {
            repaymentDay = 1;
        } else if (day < 20) {
            repaymentDay = 10;
        } else {
            repaymentDay = 20;
        }

        return baseDate.withDayOfMonth(repaymentDay);
    }

    /**
     * Normalizes a repayment date for revolving loans to use standard repayment days (1, 10, 20). This method uses the
     * same logic as RevolvingLoanUtil for consistency.
     *
     * @param repaymentDate
     *            the original repayment date
     * @return the normalized repayment date
     */
    private LocalDate normalizeRepaymentDateForRevolvingLoan(LocalDate repaymentDate) {
        int day = repaymentDate.getDayOfMonth();
        int repaymentDay;

        if (day < 10) {
            repaymentDay = 1;
        } else if (day < 20) {
            repaymentDay = 10;
        } else {
            repaymentDay = 20;
        }

        return repaymentDate.withDayOfMonth(repaymentDay);
    }

    /**
     * Validates that the adjusted first payment date is valid for the loan.
     *
     * @param loan
     *            the loan
     * @param adjustedFirstPaymentDate
     *            the adjusted first payment date
     * @return true if valid, false otherwise
     */
    public boolean isValidFirstPaymentDate(Loan loan, LocalDate adjustedFirstPaymentDate) {
        LocalDate disbursementDate = loan.getDisbursementDate();

        if (disbursementDate == null) {
            log.warn("No disbursement date set for loan {}, cannot validate first payment date", loan.getId());
            return false;
        }

        // First payment date must be after disbursement date
        if (!DateUtils.isAfter(adjustedFirstPaymentDate, disbursementDate)) {
            log.warn("Adjusted first payment date {} is not after disbursement date {} for loan {}", adjustedFirstPaymentDate,
                    disbursementDate, loan.getId());
            return false;
        }

        // For revolving loans, validate that the day is one of the standard repayment days
        if (loan.isRevolvingLoan()) {
            int dayOfMonth = adjustedFirstPaymentDate.getDayOfMonth();
            if (dayOfMonth != 1 && dayOfMonth != 10 && dayOfMonth != 20) {
                log.warn("Adjusted first payment date {} does not fall on standard repayment day (1, 10, 20) for revolving loan {}",
                        adjustedFirstPaymentDate, loan.getId());
                return false;
            }
        }

        return true;
    }
}
