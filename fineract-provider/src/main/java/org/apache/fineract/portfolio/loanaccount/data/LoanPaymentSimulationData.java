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
package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import org.apache.fineract.organisation.monetary.data.CurrencyData;

/**
 * Immutable data object representing a loan transaction.
 */
@SuppressWarnings("unused")
public class LoanPaymentSimulationData {

    private final LocalDate paymentDate;
    private final String paymentCode;
    private final BigDecimal principalPortion;
    private final BigDecimal interestPortion;
    private final BigDecimal lateInterestPortion;
    private final BigDecimal totalAmount;
    private final CurrencyData currencyData;
    private final BigDecimal availableGuaranteeAmount;
    Collection<String> loanBankAgreements;

    public LoanPaymentSimulationData(final LocalDate paymentDate, final String paymentCode, final BigDecimal principalPortion,
            final BigDecimal interestPortion, final BigDecimal lateInterestPortion, final BigDecimal totalAmount,
            final BigDecimal availableGuaranteeAmount, final CurrencyData currencyData) {
        this.paymentDate = paymentDate;
        this.paymentCode = paymentCode;
        this.principalPortion = principalPortion;
        this.interestPortion = interestPortion;
        this.lateInterestPortion = lateInterestPortion;
        this.totalAmount = totalAmount;
        this.availableGuaranteeAmount = availableGuaranteeAmount;
        this.currencyData = currencyData;
    }

    public void setLoanBankAgreements(Collection<String> loanBankAgreements) {
        this.loanBankAgreements = loanBankAgreements;
    }
}
