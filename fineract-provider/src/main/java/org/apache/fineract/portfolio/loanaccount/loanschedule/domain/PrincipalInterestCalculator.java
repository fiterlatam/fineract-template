package org.apache.fineract.portfolio.loanaccount.loanschedule.domain;

import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class PrincipalInterestCalculator {

    public PrincipalInterest principalInterestComponentsForFlatInterestLoans(final PaymentPeriodsInOneYearCalculator calculator,
               final BigDecimal interestCalculationGraceOnRepaymentPeriodFraction, final Money totalCumulativePrincipal,
               Money totalCumulativeInterest, Money totalInterestDueForLoan, final Money cumulatingInterestPaymentDueToGrace,
               final Money outstandingBalance, final LoanApplicationTerms loanApplicationTerms, final int periodNumber, final MathContext mc,
               @SuppressWarnings("unused") TreeMap<LocalDate, Money> principalVariation,
               @SuppressWarnings("unused") Map<LocalDate, Money> compoundingMap, LocalDate periodStartDate, LocalDate periodEndDate,
               @SuppressWarnings("unused") Collection<LoanTermVariationsData> termVariations) {

        final PrincipalInterest result = loanApplicationTerms.calculateTotalInterestForPeriod(calculator,
                interestCalculationGraceOnRepaymentPeriodFraction, periodNumber, mc, cumulatingInterestPaymentDueToGrace,
                outstandingBalance, periodStartDate, periodEndDate);
        Money interestForThisInstallment = result.interest();

        Money principalForThisInstallment = loanApplicationTerms.calculateTotalPrincipalForPeriod(calculator, outstandingBalance,
                periodNumber, mc, interestForThisInstallment);

        // update cumulative fields for principal & interest
        final Money interestBroughtForwardDueToGrace = result.interestPaymentDueToGrace();
        final Money totalCumulativePrincipalToDate = totalCumulativePrincipal.plus(principalForThisInstallment);
        final Money totalCumulativeInterestToDate = totalCumulativeInterest.plus(interestForThisInstallment);

        // adjust if needed
        principalForThisInstallment = loanApplicationTerms.adjustPrincipalIfLastRepaymentPeriod(principalForThisInstallment,
                totalCumulativePrincipalToDate, periodNumber);

        // totalCumulativeInterest from partial schedule generation for multi
        // rescheduling
        /*
         * if (loanApplicationTerms.getPartialTotalCumulativeInterest() != null &&
         * loanApplicationTerms.getTotalInterestDue() != null) { totalInterestDueForLoan =
         * loanApplicationTerms.getTotalInterestDue(); totalInterestDueForLoan =
         * totalInterestDueForLoan.plus(loanApplicationTerms. getPartialTotalCumulativeInterest()); }
         */
        interestForThisInstallment = loanApplicationTerms.adjustInterestIfLastRepaymentPeriod(interestForThisInstallment,
                totalCumulativeInterestToDate, totalInterestDueForLoan, periodNumber);

        return new PrincipalInterest(principalForThisInstallment, interestForThisInstallment, interestBroughtForwardDueToGrace);
    }

    public PrincipalInterest principalInterestComponentsForDecliningBalanceLoan(final PaymentPeriodsInOneYearCalculator calculator,
               final BigDecimal interestCalculationGraceOnRepaymentPeriodFraction, final Money totalCumulativePrincipal,
               @SuppressWarnings("unused") final Money totalCumulativeInterest,
               @SuppressWarnings("unused") final Money totalInterestDueForLoan, final Money cumulatingInterestPaymentDueToGrace,
               final Money outstandingBalance, final LoanApplicationTerms loanApplicationTerms, final int periodNumber, final MathContext mc,
               final TreeMap<LocalDate, Money> principalVariation, final Map<LocalDate, Money> compoundingMap, final LocalDate periodStartDate,
               final LocalDate periodEndDate, final Collection<LoanTermVariationsData> termVariations) {

        LocalDate interestStartDate = periodStartDate;
        Money interestForThisInstallment = totalCumulativePrincipal.zero();
        Money compoundedInterest = totalCumulativePrincipal.zero();
        Money balanceForInterestCalculation = outstandingBalance;
        Money cumulatingInterestDueToGrace = cumulatingInterestPaymentDueToGrace;
        Map<LocalDate, BigDecimal> interestRates = new HashMap<>(termVariations.size());

        for (LoanTermVariationsData loanTermVariation : termVariations) {
            if (loanTermVariation.getTermVariationType().isInterestRateVariation()
                    && loanTermVariation.isApplicable(periodStartDate, periodEndDate)) {
                LocalDate fromDate = loanTermVariation.getTermVariationApplicableFrom();
                if (fromDate == null) {
                    fromDate = periodStartDate;
                }
                interestRates.put(fromDate, loanTermVariation.getDecimalValue());
                if (!principalVariation.containsKey(fromDate)) {
                    principalVariation.put(fromDate, balanceForInterestCalculation.zero());
                }
            }
        }

        if (principalVariation != null) {

            for (Map.Entry<LocalDate, Money> principal : principalVariation.entrySet()) {

                if (!DateUtils.isAfter(principal.getKey(), periodEndDate)) {
                    int interestForDays = Math.toIntExact(ChronoUnit.DAYS.between(interestStartDate, principal.getKey()));
                    if (interestForDays > 0) {
                        final PrincipalInterest result = loanApplicationTerms.calculateTotalInterestForPeriod(calculator,
                                interestCalculationGraceOnRepaymentPeriodFraction, periodNumber, mc, cumulatingInterestDueToGrace,
                                balanceForInterestCalculation, interestStartDate, principal.getKey());
                        interestForThisInstallment = interestForThisInstallment.plus(result.interest());
                        cumulatingInterestDueToGrace = result.interestPaymentDueToGrace();
                        interestStartDate = principal.getKey();

                    }
                    Money compoundFee = totalCumulativePrincipal.zero();
                    if (compoundingMap.containsKey(principal.getKey())) {
                        Money interestToBeCompounded = totalCumulativePrincipal.zero();
                        // for interest compounding
                        if (loanApplicationTerms.getInterestRecalculationCompoundingMethod().isInterestCompoundingEnabled()) {
                            interestToBeCompounded = interestForThisInstallment.minus(compoundedInterest);
                            balanceForInterestCalculation = balanceForInterestCalculation.plus(interestToBeCompounded);
                            compoundedInterest = interestForThisInstallment;
                        }
                        // fee compounding will be done after calculation
                        compoundFee = compoundingMap.get(principal.getKey());
                        compoundingMap.put(principal.getKey(), interestToBeCompounded.plus(compoundFee));
                    }
                    if (!loanApplicationTerms.isPrincipalCompoundingDisabledForOverdueLoans()) {
                        balanceForInterestCalculation = balanceForInterestCalculation.plus(principal.getValue());
                    }
                    balanceForInterestCalculation = balanceForInterestCalculation.plus(compoundFee);

                    if (interestRates.containsKey(principal.getKey())) {
                        loanApplicationTerms.updateAnnualNominalInterestRate(interestRates.get(principal.getKey()));
                    }
                }
            }
        }

        final PrincipalInterest result = loanApplicationTerms.calculateTotalInterestForPeriod(calculator,
                interestCalculationGraceOnRepaymentPeriodFraction, periodNumber, mc, cumulatingInterestDueToGrace,
                balanceForInterestCalculation, interestStartDate, periodEndDate);

        interestForThisInstallment = interestForThisInstallment.plus(result.interest());
        cumulatingInterestDueToGrace = result.interestPaymentDueToGrace();

        if (loanApplicationTerms.isInterestToBeRecoveredFirstWhenGreaterThanEMIEnabled()
                && loanApplicationTerms.isInterestTobeApproppriated()) {
            interestForThisInstallment = interestForThisInstallment.add(loanApplicationTerms.getInterestTobeApproppriated());
            loanApplicationTerms.setInterestTobeApproppriated(interestForThisInstallment.zero());
        }

        Money interestForPeriod = interestForThisInstallment;
        if (interestForPeriod.isGreaterThanZero()) {
            interestForPeriod = interestForPeriod.minus(cumulatingInterestPaymentDueToGrace);
        } else {
            interestForPeriod = cumulatingInterestDueToGrace.minus(cumulatingInterestPaymentDueToGrace);
        }

        Money principalForThisInstallment = loanApplicationTerms.calculateTotalPrincipalForPeriod(calculator, outstandingBalance,
                periodNumber, mc, interestForPeriod);
        if (loanApplicationTerms.isInterestToBeRecoveredFirstWhenGreaterThanEMIEnabled() && principalForThisInstallment.isLessThanZero()
                && !loanApplicationTerms.isLastRepaymentPeriod(periodNumber)) {
            loanApplicationTerms.setInterestTobeApproppriated(principalForThisInstallment.abs());
            interestForThisInstallment = interestForThisInstallment.minus(loanApplicationTerms.getInterestTobeApproppriated());
            principalForThisInstallment = principalForThisInstallment.zero();
        }

        // update cumulative fields for principal & interest
        final Money interestBroughtFowardDueToGrace = cumulatingInterestDueToGrace;
        final Money totalCumulativePrincipalToDate = totalCumulativePrincipal.plus(principalForThisInstallment);

        // adjust if needed
        principalForThisInstallment = loanApplicationTerms.adjustPrincipalIfLastRepaymentPeriod(principalForThisInstallment,
                totalCumulativePrincipalToDate, periodNumber);

        PrincipalInterest principalInterest = new PrincipalInterest(principalForThisInstallment, interestForThisInstallment,
                interestBroughtFowardDueToGrace);
        principalInterest.setRescheduleInterestPortion(loanApplicationTerms.getInterestTobeApproppriated());
        return principalInterest;
    }
}
