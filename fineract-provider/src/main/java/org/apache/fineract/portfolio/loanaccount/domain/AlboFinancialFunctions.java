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
package org.apache.fineract.portfolio.loanaccount.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.common.domain.DaysInMonthType;
import org.apache.fineract.portfolio.common.domain.DaysInYearType;

public final class AlboFinancialFunctions {

    private static BigDecimal a100 = BigDecimal.valueOf(Double.parseDouble("100.0"));

    private AlboFinancialFunctions() {}

    public static BigDecimal futureValue(BigDecimal loanAmount, Integer loanDuration, BigDecimal vatRate,
            BigDecimal annualNominalInterest) {
        BigDecimal loanFutureValue;

        // get values to use in the formula
        BigDecimal effectiveAnnualRateWithVat = effectiveRateWithVat(annualNominalInterest, vatRate);

        // calculate elements needed for the formula
        BigDecimal interestRate = effectiveAnnualRateWithVat.divide(a100, MathContext.DECIMAL64);
        BigDecimal base = interestRate.add(BigDecimal.ONE);
        BigDecimal exponent = BigDecimal.valueOf(loanDuration.intValue())
                .divide(BigDecimal.valueOf(DaysInYearType.DAYS_365.getValue().intValue()), MathContext.DECIMAL64);
        Double powResult = Math.pow(base.doubleValue(), exponent.doubleValue());

        loanFutureValue = loanAmount.multiply(BigDecimal.valueOf(powResult), MathContext.DECIMAL64).setScale(9,
                MoneyHelper.getRoundingMode());

        return loanFutureValue;
    }

    public static BigDecimal effectiveRateWithVat(BigDecimal annualInterestRate, BigDecimal vatRate) {
        BigDecimal exponent = BigDecimal.valueOf(DaysInYearType.DAYS_365.getValue().doubleValue())
                .divide(BigDecimal.valueOf(DaysInMonthType.DAYS_30.getValue()), MathContext.DECIMAL64);
        BigDecimal interestRate = annualInterestRate.divide(a100, MathContext.DECIMAL64);
        BigDecimal vatRateForCalculation = vatRate.divide(a100, MathContext.DECIMAL64).add(BigDecimal.ONE);
        BigDecimal base = interestRate.divide(exponent, MathContext.DECIMAL64).multiply(vatRateForCalculation, MathContext.DECIMAL64)
                .add(BigDecimal.ONE);
        Double effectiveRateInterest = Math.pow(base.doubleValue(), exponent.doubleValue()) - 1;
        BigDecimal effectiveRateInterestBigDecimal = BigDecimal.valueOf(effectiveRateInterest).multiply(a100, MathContext.DECIMAL64)
                .setScale(9, MoneyHelper.getRoundingMode());

        return effectiveRateInterestBigDecimal;
    }

    public static double pmt(BigDecimal loanDisbursedAmount, BigDecimal loanFutureValue, int numberOfInstallments, BigDecimal vatPercentage,
            BigDecimal annualNominalInterest, List<LocalDate> installmentDateList, LocalDate loanDueDate, List<LoanCharge> charges,
            ApplicationCurrency currency) {
        BigDecimal totalInstallmentWithVat;

        final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
        final MathContext mc = new MathContext(8, roundingMode);

        BigDecimal effectiveAnnualRateWithVat = effectiveRateWithVat(annualNominalInterest, vatPercentage);

        Map<Integer, BigDecimal> factorInstallmentsMap = calculateFactorPerInstallment(loanDueDate, effectiveAnnualRateWithVat,
                installmentDateList);
        BigDecimal installmentsFactorTotal = factorInstallmentsMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal installmentFee = BigDecimal.ZERO;
        BigDecimal collectionFee = BigDecimal.ZERO;

        BigDecimal loanAmountWithFeeAndCharges = loanDisbursedAmount;

        for (LoanCharge charge : charges) {
            if (charge.isFeeCharge() && charge.isCollectionFee()) {
                collectionFee = collectionFee.add(charge.amountOrPercentage());
            }

            if (charge.isAlboInstalmentFee() && charge.isFeeCharge()) {
                BigDecimal chargeFraction = charge.getPercentage().divide(a100, mc);
                installmentFee = installmentFee.add(chargeFraction);
            }

        }

        vatPercentage = vatPercentage.divide(a100, MathContext.DECIMAL64).add(BigDecimal.ONE);

        // calculate total installments with VAT
        BigDecimal futureValueOverFactors = loanFutureValue.divide(installmentsFactorTotal, MathContext.DECIMAL64);
        BigDecimal loanAmountOverNumberOfInstallments = loanAmountWithFeeAndCharges.multiply(installmentFee, MathContext.DECIMAL64)
                .divide(BigDecimal.valueOf(numberOfInstallments), MathContext.DECIMAL64).add(collectionFee);// check
                                                                                                            // this
                                                                                                            // formula
                                                                                                            // now
        totalInstallmentWithVat = loanAmountOverNumberOfInstallments.multiply(vatPercentage, MathContext.DECIMAL64)
                .add(futureValueOverFactors);

        return totalInstallmentWithVat.doubleValue();
    }

    public static Map<Integer, BigDecimal> calculateFactorPerInstallment(LocalDate dueDateLastInstallment,
            BigDecimal effectiveAnnualRateWithVat, List<LocalDate> installmentDateList) {
        Map<Integer, BigDecimal> factorInstallmentsMapping = new HashMap<>();

        // get values to use in the formula
        BigDecimal interestRate = effectiveAnnualRateWithVat.divide(a100, MathContext.DECIMAL64);
        BigDecimal base = interestRate.add(BigDecimal.ONE);

        // calculate the factor per installment of the loan
        int instalmentCount = 1;
        for (LocalDate instalmentDate : installmentDateList) {
            // Calculate the duration of a loan between the last installment due date and the installment due date
            Integer daysInPeriod = Math.toIntExact(ChronoUnit.DAYS.between(instalmentDate, dueDateLastInstallment));
            BigDecimal exponent = BigDecimal.valueOf(daysInPeriod.intValue())
                    .divide(BigDecimal.valueOf(DaysInYearType.DAYS_365.getValue().intValue()), MathContext.DECIMAL64);

            // Calculate the factor of the current installment
            Double powResult = Math.pow(base.doubleValue(), exponent.doubleValue());
            BigDecimal factorInstallment = BigDecimal.valueOf(powResult).setScale(9, MoneyHelper.getRoundingMode());

            factorInstallmentsMapping.put(instalmentCount, factorInstallment);
            instalmentCount++;
        }

        return factorInstallmentsMapping;
    }

}
