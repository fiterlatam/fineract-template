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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.infrastructure.channel.data.ChannelData;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.springframework.stereotype.Service;

/**
 * Service responsible for validating overpayments on loans based on product type and business rules. This service
 * handles different validation logic for various loan product types.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoanOverpaymentValidationService {

    private static final String BANCOS_PARAM = "Bancos";

    /**
     * Validates if an overpayment is allowed for the given loan, transaction amount, and channel. This is the main
     * entry point for all overpayment validation logic.
     *
     * @param loan
     *            The loan for which the repayment is being made
     * @param transactionAmount
     *            The amount of the repayment transaction
     * @param foreclosureAmount
     *            The foreclosure amount (total outstanding amount)
     * @param channelData
     *            The channel data for the repayment
     * @throws GeneralPlatformDomainRuleException
     *             if overpayment is not allowed
     */
    public void validateOverpayment(Loan loan, BigDecimal transactionAmount, BigDecimal foreclosureAmount, ChannelData channelData) {
        log.info("Validating overpayment for loan ID: {}, transaction amount: {}, foreclosure amount: {}, channel: {}", loan.getId(),
                transactionAmount, foreclosureAmount, channelData.getName());

        // Check if this is an overpayment (transaction amount exceeds foreclosure amount)
        if (transactionAmount.compareTo(foreclosureAmount) <= 0) {
            log.info("Transaction amount does not exceed foreclosure amount, no overpayment validation needed");
            return;
        }

        // Apply product-specific validation rules
        validateRevolvingCreditOverpayment(loan, transactionAmount, foreclosureAmount);
    }

    /**
     * Validates overpayment based on channel rules. Currently, only the "Bancos" channel allows overpayments for
     * non-revolving loans.
     *
     * @param transactionAmount
     *            The amount of the repayment transaction
     * @param foreclosureAmount
     *            The foreclosure amount (total outstanding amount)
     * @param channelData
     *            The channel data for the repayment
     * @throws GeneralPlatformDomainRuleException
     *             if overpayment is not allowed for this channel
     */
    private void validateChannelBasedOverpayment(BigDecimal transactionAmount, BigDecimal foreclosureAmount, ChannelData channelData) {
        // For non-Bancos channels, overpayments are not allowed (unless overridden by product-specific rules)
        if (!BANCOS_PARAM.equalsIgnoreCase(channelData.getName())) {
            String repaymentStr = transactionAmount.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
            String foreclosureStr = foreclosureAmount.setScale(2, RoundingMode.HALF_EVEN).toPlainString();

            throw new GeneralPlatformDomainRuleException("error.msg.loan.repayment.exceeds.foreclosure.amount",
                    String.format("Repayment amount (%s) exceeds Foreclosure amount (%s)", repaymentStr, foreclosureStr), BANCOS_PARAM);
        }
    }

    /**
     * Validates overpayment for revolving credit products. Revolving credit products do not allow overpayments as they
     * would create balances in favor of the client.
     *
     * @param loan
     *            The loan for which the repayment is being made
     * @param transactionAmount
     *            The amount of the repayment transaction
     * @param foreclosureAmount
     *            The foreclosure amount (total outstanding amount)
     * @throws GeneralPlatformDomainRuleException
     *             if overpayment is not allowed for revolving credit
     */
    private void validateRevolvingCreditOverpayment(Loan loan, BigDecimal transactionAmount, BigDecimal foreclosureAmount) {
        // Check if this is a revolving credit product
        if (isRevolvingCreditProduct(loan)) {
            log.info("Loan {} is a revolving credit product, overpayments are not allowed", loan.getId());

            BigDecimal overpaymentAmount = transactionAmount.subtract(foreclosureAmount);
            String errorMessage = String.format(
                    "Overpayments are not allowed for revolving credit products. "
                            + "Transaction amount (%s) exceeds foreclosure amount (%s) by %s",
                    transactionAmount, foreclosureAmount, overpaymentAmount);

            throw new GeneralPlatformDomainRuleException("error.msg.loan.revolving.credit.overpayment.not.allowed", errorMessage,
                    transactionAmount, foreclosureAmount, overpaymentAmount);
        }
    }

    /**
     * Determines if a loan is a revolving credit product.
     *
     * @param loan
     *            The loan to check
     * @return true if the loan is a revolving credit product, false otherwise
     */
    private boolean isRevolvingCreditProduct(Loan loan) {
        return loan.isRevolvingLoan();
    }

}
