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
package org.apache.fineract.portfolio.loanaccount.loanschedule.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanproduct.domain.AmortizationMethod;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Declining balance can be amortized (see {@link AmortizationMethod}) in two ways at present:
 * <ol>
 * <li>Equal principal payments</li>
 * <li>Equal installment payments</li>
 * </ol>
 * <p>
 * </p>
 *
 * <p>
 * When amortized using <i>equal principal payments</i>, the <b>principal component</b> of each installment is fixed and
 * <b>interest due</b> is calculated from the <b>outstanding principal balance</b> resulting in a different <b>total
 * payment due</b> for each installment.
 * </p>
 *
 * <p>
 * When amortized using <i>equal installments</i>, the <b>total payment due</b> for each installment is fixed and is
 * calculated using the excel like <code>pmt</code> function. The <b>interest due</b> is calculated from the
 * <b>outstanding principal balance</b> which results in a <b>principal component</b> that is <b>total payment due</b>
 * minus <b>interest due</b>.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class CumulativeDecliningBalanceInterestLoanScheduleGenerator extends AbstractCumulativeLoanScheduleGenerator {

    private final ScheduledDateGenerator scheduledDateGenerator;
    private final PaymentPeriodsInOneYearCalculator paymentPeriodsInOneYearCalculator;

    @Override
    public ScheduledDateGenerator getScheduledDateGenerator() {
        return scheduledDateGenerator;
    }

    @Override
    public PaymentPeriodsInOneYearCalculator getPaymentPeriodsInOneYearCalculator() {
        return paymentPeriodsInOneYearCalculator;
    }

    @Override
    public PrincipalInterest calculatePrincipalInterestComponentsForPeriod(final PaymentPeriodsInOneYearCalculator calculator,
            final BigDecimal interestCalculationGraceOnRepaymentPeriodFraction, final Money totalCumulativePrincipal,
            @SuppressWarnings("unused") final Money totalCumulativeInterest,
            @SuppressWarnings("unused") final Money totalInterestDueForLoan, final Money cumulatingInterestPaymentDueToGrace,
            final Money outstandingBalance, final LoanApplicationTerms loanApplicationTerms, final int periodNumber, final MathContext mc,
            final TreeMap<LocalDate, Money> principalVariation, final Map<LocalDate, Money> compoundingMap, final LocalDate periodStartDate,
            final LocalDate periodEndDate, final Collection<LoanTermVariationsData> termVariations) {

        PrincipalInterestCalculator principalInterestCalculator = new PrincipalInterestCalculator();
        return principalInterestCalculator.principalInterestComponentsForDecliningBalanceLoan(calculator,
                interestCalculationGraceOnRepaymentPeriodFraction, totalCumulativePrincipal, totalCumulativeInterest,
                totalInterestDueForLoan, cumulatingInterestPaymentDueToGrace, outstandingBalance, loanApplicationTerms, periodNumber, mc,
                principalVariation, compoundingMap, periodStartDate, periodEndDate, termVariations);
    }
}
