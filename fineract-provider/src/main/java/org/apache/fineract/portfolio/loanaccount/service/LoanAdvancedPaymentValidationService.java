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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.springframework.stereotype.Service;

/**
 * Service to validate advanced payments (payments for future installments). Advanced payments are payments made for
 * installments with due dates in the future.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoanAdvancedPaymentValidationService {

    /**
     * Validates if an advanced payment is allowed for the given loan and transaction amount. An advanced payment occurs
     * when the repayment amount would cover future installments (installments with due dates in the future).
     *
     * @param loan
     *            The loan for which the repayment is being made
     * @param transactionAmount
     *            The amount of the repayment transaction
     * @throws GeneralPlatformDomainRuleException
     *             if advanced payment is not allowed
     */
    public void validateAdvancedPayment(Loan loan, BigDecimal transactionAmount) {
        log.info("Validating advanced payment for loan ID: {}, transaction amount: {}", loan.getId(), transactionAmount);

        // Get the current business date (today)
        LocalDate currentDate = LocalDate.now();

        // Get all installments
        List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();

        // Calculate the total amount due for past and current installments
        BigDecimal totalAmountDueForPastAndCurrent = BigDecimal.ZERO;
        BigDecimal totalAmountDueForFuture = BigDecimal.ZERO;

        for (LoanRepaymentScheduleInstallment installment : installments) {
            BigDecimal installmentOutstanding = installment.getTotalOutstanding(loan.getCurrency()).getAmount();

            if (installment.getDueDate().isAfter(currentDate)) {
                // Future installment
                totalAmountDueForFuture = totalAmountDueForFuture.add(installmentOutstanding);
            } else {
                // Past or current installment
                totalAmountDueForPastAndCurrent = totalAmountDueForPastAndCurrent.add(installmentOutstanding);
            }
        }

        log.info("Loan ID: {} - Current date: {}, Past/Current due: {}, Future due: {}, Transaction amount: {}", loan.getId(), currentDate,
                totalAmountDueForPastAndCurrent, totalAmountDueForFuture, transactionAmount);

        // Check if this payment would cover future installments
        if (transactionAmount.compareTo(totalAmountDueForPastAndCurrent) > 0) {
            BigDecimal advancedAmount = transactionAmount.subtract(totalAmountDueForPastAndCurrent);
            log.info(
                    "Advanced payment detected for loan ID: {}. Transaction amount ({}) exceeds past/current due ({}) by {}. Future installments due: {}",
                    loan.getId(), transactionAmount, totalAmountDueForPastAndCurrent, advancedAmount, totalAmountDueForFuture);
            boolean isAdvancedPaymentAllowed = loan.getLoanProduct().isAdvancedPaymentsAllowed();
            // Check if the loan product allows advanced payments
            if (!isAdvancedPaymentAllowed) {
                String errorMessage = String.format("Advanced payments are not allowed for this loan product. "
                        + "Transaction amount (%s) exceeds past and current due amounts (%s) by %s. "
                        + "This would pay for future installments. Please pay only the amount due for past and current installments.",
                        transactionAmount.setScale(2, RoundingMode.HALF_EVEN).toPlainString(),
                        totalAmountDueForPastAndCurrent.setScale(2, RoundingMode.HALF_EVEN).toPlainString(),
                        advancedAmount.setScale(2, RoundingMode.HALF_EVEN).toPlainString());

                log.warn("Advanced payment rejected for loan ID: {}. Product does not allow advanced payments. Error: {}", loan.getId(),
                        errorMessage);

                throw new GeneralPlatformDomainRuleException("error.msg.loan.advanced.payment.not.allowed", errorMessage, transactionAmount,
                        totalAmountDueForPastAndCurrent, advancedAmount);
            }

            log.info("Advanced payment allowed for loan ID: {}. Product allows advanced payments.", loan.getId());
        } else {
            log.info("No advanced payment detected for loan ID: {}. Transaction amount does not exceed past/current due amounts.",
                    loan.getId());
        }
    }
}
