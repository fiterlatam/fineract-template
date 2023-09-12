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
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;

/**
 * A wrapper around loan schedule related data exposing needed behaviour by loan.
 */
public class LoanRepaymentScheduleProcessingWrapper {

    public void reprocess(final MonetaryCurrency currency, final LocalDate disbursementDate,
            final List<LoanRepaymentScheduleInstallment> repaymentPeriods, final Set<LoanCharge> loanCharges, Loan loan) {

        Money totalInterest = Money.zero(currency);
        Money totalPrincipal = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentPeriods) {
            totalInterest = totalInterest.plus(installment.getInterestCharged(currency));
            totalPrincipal = totalPrincipal.plus(installment.getPrincipal(currency));
        }
        LocalDate startDate = disbursementDate;
        for (final LoanRepaymentScheduleInstallment period : repaymentPeriods) {

            if (disbursementDate.isEqual(period.getDueDate()) && loan.getInterestChargedFromDate() != null
                    && loan.getInterestChargedFromDate().isAfter(loan.getDisbursementDate())) {
                continue;
            }

            if (loan.isVatRequired()) {

                final Pair<Money, Money> cumulativeFeeChargesDueWithin = cumulativeFeeChargesDueWithin(startDate, period.getDueDate(),
                        loanCharges, currency, period, totalPrincipal, totalInterest, !period.isRecalculatedInterestComponent());
                final Money feeChargesDueForRepaymentPeriod = cumulativeFeeChargesDueWithin.getLeft();
                final Money feeChargesDueForVatCalculation = cumulativeFeeChargesDueWithin.getRight();

                final Pair<Money, Money> feeChargesWaivedForRepaymentPeriod = cumulativeFeeChargesAndVatWaivedWithin(startDate,
                        period.getDueDate(), loanCharges, currency, !period.isRecalculatedInterestComponent());
                final Money feeChargesWaivedForRepayment = feeChargesWaivedForRepaymentPeriod.getLeft();
                final Money feeChargesWaivedForVatCalculation = feeChargesWaivedForRepaymentPeriod.getRight();

                final Pair<Money, Money> feeChargesWrittenOffForRepaymentPeriod = cumulativeFeeChargesWrittenOffAndVatWithin(startDate,
                        period.getDueDate(), loanCharges, currency, !period.isRecalculatedInterestComponent());
                final Money feeChargesWrittenOffForRepayment = feeChargesWrittenOffForRepaymentPeriod.getLeft();
                final Money feeChargesWrittenForVatCalculation = feeChargesWrittenOffForRepaymentPeriod.getRight();

                final Pair<Money, Money> cumulativePenaltyChargesDueWithin = cumulativePenaltyChargesDueWithin(startDate,
                        period.getDueDate(), loanCharges, currency, period, totalPrincipal, totalInterest,
                        !period.isRecalculatedInterestComponent());
                final Money penaltyChargesDueForRepaymentPeriod = cumulativePenaltyChargesDueWithin.getLeft();
                final Money penaltyChargesDueForVatCalculation = cumulativePenaltyChargesDueWithin.getRight();
                final Money vatOnChargeDue = calculateVatOnAmount(loan.getVatPercentage(),
                        feeChargesDueForVatCalculation);
                final Money vatOnPenaltyChargeDue = calculateVatOnAmount(loan.getVatPercentage(),
                        penaltyChargesDueForVatCalculation);

                final Pair<Money, Money> penaltyChargesWaivedForRepaymentPeriod = cumulativePenaltyChargesAndVatWaivedWithin(startDate,
                        period.getDueDate(), loanCharges, currency, !period.isRecalculatedInterestComponent());
                final Money penaltyChargesWaivedForRepayment = penaltyChargesWaivedForRepaymentPeriod.getLeft();
                final Money penaltyChargesWaivedForVatCalculation = penaltyChargesWaivedForRepaymentPeriod.getRight();
                final Money vatOnChargeWaived = calculateVatOnAmount(loan.getVatPercentage(),
                        feeChargesWaivedForVatCalculation);
                final Money vatOnPenaltyChargeWaived = calculateVatOnAmount(loan.getVatPercentage(),
                        penaltyChargesWaivedForVatCalculation);

                final Pair<Money, Money> penaltyChargesWrittenOffForRepaymentPeriod = cumulativePenaltyChargesAndVatWrittenOffWithin(
                        startDate, period.getDueDate(), loanCharges, currency, !period.isRecalculatedInterestComponent());
                final Money penaltyChargesWrittenOffForRepayment = penaltyChargesWrittenOffForRepaymentPeriod.getLeft();
                final Money penaltyChargesWrittenOffForVatCalculation = penaltyChargesWrittenOffForRepaymentPeriod.getRight();
                final Money vatOnChargeWrittenOff = calculateVatOnAmount(loan.getVatPercentage(),
                        feeChargesWrittenForVatCalculation);
                final Money vatOnPenaltyChargeWrittenOff = calculateVatOnAmount(loan.getVatPercentage(),
                        penaltyChargesWrittenOffForVatCalculation);

                period.updateChargePortion(feeChargesDueForRepaymentPeriod, feeChargesWaivedForRepayment, feeChargesWrittenOffForRepayment,
                        penaltyChargesDueForRepaymentPeriod, penaltyChargesWaivedForRepayment, penaltyChargesWrittenOffForRepayment,
                        vatOnChargeDue, vatOnChargeWaived, vatOnChargeWrittenOff, vatOnPenaltyChargeDue, vatOnPenaltyChargeWaived, vatOnPenaltyChargeWrittenOff);

            } else {
                final Pair<Money, Money> cumulativeFeeChargesDueWithin = cumulativeFeeChargesDueWithin(startDate, period.getDueDate(),
                        loanCharges, currency, period, totalPrincipal, totalInterest, !period.isRecalculatedInterestComponent());
                final Money feeChargesDueForRepaymentPeriod = cumulativeFeeChargesDueWithin.getLeft();

                final Money feeChargesWaivedForRepaymentPeriod = cumulativeFeeChargesWaivedWithin(startDate, period.getDueDate(),
                        loanCharges, currency, !period.isRecalculatedInterestComponent());
                final Money feeChargesWrittenOffForRepaymentPeriod = cumulativeFeeChargesWrittenOffWithin(startDate, period.getDueDate(),
                        loanCharges, currency, !period.isRecalculatedInterestComponent());

                final Pair<Money, Money> cumulativePenaltyChargesDueWithin = cumulativePenaltyChargesDueWithin(startDate,
                        period.getDueDate(), loanCharges, currency, period, totalPrincipal, totalInterest,
                        !period.isRecalculatedInterestComponent());
                final Money penaltyChargesDueForRepaymentPeriod = cumulativePenaltyChargesDueWithin.getLeft();

                final Money penaltyChargesWaivedForRepaymentPeriod = cumulativePenaltyChargesWaivedWithin(startDate, period.getDueDate(),
                        loanCharges, currency, !period.isRecalculatedInterestComponent());

                final Money penaltyChargesWrittenOffForRepaymentPeriod = cumulativePenaltyChargesWrittenOffWithin(startDate,
                        period.getDueDate(), loanCharges, currency, !period.isRecalculatedInterestComponent());

                period.updateChargePortion(feeChargesDueForRepaymentPeriod, feeChargesWaivedForRepaymentPeriod,
                        feeChargesWrittenOffForRepaymentPeriod, penaltyChargesDueForRepaymentPeriod, penaltyChargesWaivedForRepaymentPeriod,
                        penaltyChargesWrittenOffForRepaymentPeriod, Money.zero(currency), Money.zero(currency), Money.zero(currency),
                        Money.zero(currency), Money.zero(currency), Money.zero(currency));
            }

            startDate = period.getDueDate();
        }
    }

    private Pair<Money, Money> cumulativeFeeChargesDueWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency monetaryCurrency, LoanRepaymentScheduleInstallment period,
            final Money totalPrincipal, final Money totalInterest, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(monetaryCurrency);
        Money amountSubjectToVat = Money.zero(monetaryCurrency);
        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isFeeCharge() && !loanCharge.isDueAtDisbursement() && loanCharge.isNotOriginationFee()) {
                if (loanCharge.isInstalmentFee()) {
                    if (loanCharge.getChargeCalculation().isPercentageBased()) {
                        BigDecimal amount = BigDecimal.ZERO;
                        if (loanCharge.getChargeCalculation().isPercentageOfAmountAndInterest()) {
                            amount = amount.add(period.getPrincipal(monetaryCurrency).getAmount())
                                    .add(period.getInterestCharged(monetaryCurrency).getAmount());
                        } else if (loanCharge.getChargeCalculation().isPercentageOfInterest()) {
                            amount = amount.add(period.getInterestCharged(monetaryCurrency).getAmount());
                        } else {
                            amount = amount.add(loanCharge.getAmountPercentageAppliedTo());
                        }
                        Money loanChargeAmt = calculateInstalmentChargeForInstalment(loanCharge, amount);
                        cumulative = cumulative.plus(loanChargeAmt);

                        if (loanCharge.getCharge().isVatRequired()) {
                            amountSubjectToVat = amountSubjectToVat.plus(loanChargeAmt);
                        }

                    } else {
                        cumulative = cumulative.plus(loanCharge.amountOrPercentage());

                        if (loanCharge.getCharge().isVatRequired()) {
                            amountSubjectToVat = amountSubjectToVat.plus(loanCharge.amountOrPercentage());
                        }
                    }
                } else if (loanCharge.isOverdueInstallmentCharge()
                        && loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)
                        && loanCharge.getChargeCalculation().isPercentageBased()) {
                    cumulative = cumulative.plus(loanCharge.chargeAmount());
                    if (loanCharge.getCharge().isVatRequired()) {
                        amountSubjectToVat = amountSubjectToVat.plus(loanCharge.chargeAmount());
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)
                        && loanCharge.getChargeCalculation().isPercentageBased()) {
                    BigDecimal amount = BigDecimal.ZERO;
                    if (loanCharge.getChargeCalculation().isPercentageOfAmountAndInterest()) {
                        amount = amount.add(totalPrincipal.getAmount()).add(totalInterest.getAmount());
                    } else if (loanCharge.getChargeCalculation().isPercentageOfInterest()) {
                        amount = amount.add(totalInterest.getAmount());
                    } else {
                        // If charge type is specified due date and loan is
                        // multi disburment loan.
                        // Then we need to get as of this loan charge due date
                        // how much amount disbursed.
                        if (loanCharge.getLoan() != null && loanCharge.isSpecifiedDueDate()
                                && loanCharge.getLoan().isMultiDisburmentLoan()) {
                            for (final LoanDisbursementDetails loanDisbursementDetails : loanCharge.getLoan().getDisbursementDetails()) {
                                if (!loanDisbursementDetails.expectedDisbursementDate().isAfter(loanCharge.getDueDate())) {
                                    amount = amount.add(loanDisbursementDetails.principal());
                                }
                            }
                        } else {
                            amount = amount.add(totalPrincipal.getAmount());
                        }
                    }
                    BigDecimal loanChargeAmt = amount.multiply(loanCharge.getPercentage()).divide(BigDecimal.valueOf(100));
                    cumulative = cumulative.plus(loanChargeAmt);

                    if (loanCharge.getCharge().isVatRequired()) {
                        amountSubjectToVat = amountSubjectToVat.plus(loanChargeAmt);
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.amount());

                    if (loanCharge.getCharge().isVatRequired()) {
                        amountSubjectToVat = amountSubjectToVat.plus(loanCharge.amount());
                    }

                }
            }
        }

        return Pair.of(cumulative, amountSubjectToVat);
    }

    private Money cumulativeFeeChargesWaivedWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isFeeCharge() && !loanCharge.isDueAtDisbursement()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    LoanInstallmentCharge loanChargePerInstallment = loanCharge.getInstallmentLoanCharge(periodEnd);
                    if (loanChargePerInstallment != null) {
                        cumulative = cumulative.plus(loanChargePerInstallment.getAmountWaived(currency));
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.getAmountWaived(currency));
                }
            }
        }

        return cumulative;
    }

    private Pair<Money, Money> cumulativeFeeChargesAndVatWaivedWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);
        Money amountSubjectToVat = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isFeeCharge() && !loanCharge.isDueAtDisbursement()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    LoanInstallmentCharge loanChargePerInstallment = loanCharge.getInstallmentLoanCharge(periodEnd);
                    if (loanChargePerInstallment != null) {
                        cumulative = cumulative.plus(loanChargePerInstallment.getAmountWaived(currency));
                        if (loanCharge.getCharge().isVatRequired()) {
                            amountSubjectToVat = amountSubjectToVat.plus(loanChargePerInstallment.getAmountWaived(currency));
                        }
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.getAmountWaived(currency));
                    if (loanCharge.getCharge().isVatRequired()) {
                        amountSubjectToVat = amountSubjectToVat.plus(loanCharge.getAmountWaived(currency));
                    }
                }
            }
        }

        return Pair.of(cumulative, amountSubjectToVat);
    }

    private Money cumulativeFeeChargesWrittenOffWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isFeeCharge() && !loanCharge.isDueAtDisbursement()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    LoanInstallmentCharge loanChargePerInstallment = loanCharge.getInstallmentLoanCharge(periodEnd);
                    if (loanChargePerInstallment != null) {
                        cumulative = cumulative.plus(loanChargePerInstallment.getAmountWrittenOff(currency));
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.getAmountWrittenOff(currency));
                }
            }
        }

        return cumulative;
    }

    private Pair<Money, Money> cumulativeFeeChargesWrittenOffAndVatWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);
        Money amountSubjectToVat = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isFeeCharge() && !loanCharge.isDueAtDisbursement()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    LoanInstallmentCharge loanChargePerInstallment = loanCharge.getInstallmentLoanCharge(periodEnd);
                    if (loanChargePerInstallment != null) {
                        cumulative = cumulative.plus(loanChargePerInstallment.getAmountWrittenOff(currency));
                        if (loanCharge.getCharge().isVatRequired()) {
                            amountSubjectToVat = amountSubjectToVat.plus(loanChargePerInstallment.getAmountWrittenOff(currency));
                        }
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.getAmountWrittenOff(currency));
                    if (loanCharge.getCharge().isVatRequired()) {
                        amountSubjectToVat = amountSubjectToVat.plus(loanCharge.getAmountWrittenOff(currency));
                    }
                }

            }
        }

        return Pair.of(cumulative, amountSubjectToVat);
    }

    private Pair<Money, Money> cumulativePenaltyChargesDueWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, LoanRepaymentScheduleInstallment period,
            final Money totalPrincipal, final Money totalInterest, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);
        Money amountSubjectToVat = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isPenaltyCharge()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    if (loanCharge.getChargeCalculation().isPercentageBased()) {
                        BigDecimal amount = BigDecimal.ZERO;
                        if (loanCharge.getChargeCalculation().isPercentageOfAmountAndInterest()) {
                            amount = amount.add(period.getPrincipal(currency).getAmount())
                                    .add(period.getInterestCharged(currency).getAmount());
                        } else if (loanCharge.getChargeCalculation().isPercentageOfInterest()) {
                            amount = amount.add(period.getInterestCharged(currency).getAmount());
                        } else {
                            amount = amount.add(loanCharge.getAmountPercentageAppliedTo());
                        }
                        Money loanChargeAmt = calculateInstalmentChargeForInstalment(loanCharge, amount);
                        cumulative = cumulative.plus(loanChargeAmt);

                        if (loanCharge.getCharge().isVatRequired()) {
                            amountSubjectToVat = amountSubjectToVat.plus(loanChargeAmt);
                        }

                    } else {
                        cumulative = cumulative.plus(loanCharge.amountOrPercentage());

                        if (loanCharge.getCharge().isVatRequired()) {
                            amountSubjectToVat = amountSubjectToVat.plus(loanCharge.amountOrPercentage());
                        }
                    }
                } else if (loanCharge.isOverdueInstallmentCharge()
                        && loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)
                        && loanCharge.getChargeCalculation().isPercentageBased()) {
                    cumulative = cumulative.plus(loanCharge.chargeAmount());

                    if (loanCharge.getCharge().isVatRequired()) {
                        amountSubjectToVat = amountSubjectToVat.plus(loanCharge.chargeAmount());
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)
                        && loanCharge.getChargeCalculation().isPercentageBased()) {
                    BigDecimal amount = BigDecimal.ZERO;
                    if (loanCharge.getChargeCalculation().isPercentageOfAmountAndInterest()) {
                        amount = amount.add(totalPrincipal.getAmount()).add(totalInterest.getAmount());
                    } else if (loanCharge.getChargeCalculation().isPercentageOfInterest()) {
                        amount = amount.add(totalInterest.getAmount());
                    } else {
                        amount = amount.add(loanCharge.getAmountPercentageAppliedTo());
                    }
                    Money loanChargeAmt = calculateInstalmentChargeForInstalment(loanCharge, amount);
                    cumulative = cumulative.plus(loanChargeAmt);

                    if (loanCharge.getCharge().isVatRequired()) {
                        amountSubjectToVat = amountSubjectToVat.plus(loanChargeAmt);
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.amount());

                    if (loanCharge.getCharge().isVatRequired()) {
                        amountSubjectToVat = amountSubjectToVat.plus(loanCharge.amount());
                    }
                }
            }
        }

        return Pair.of(cumulative, amountSubjectToVat);
    }

    private Money cumulativePenaltyChargesWaivedWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isPenaltyCharge()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    LoanInstallmentCharge loanChargePerInstallment = loanCharge.getInstallmentLoanCharge(periodEnd);
                    if (loanChargePerInstallment != null) {
                        cumulative = cumulative.plus(loanChargePerInstallment.getAmountWaived(currency));
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.getAmountWaived(currency));
                }
            }
        }

        return cumulative;
    }

    private Pair<Money, Money> cumulativePenaltyChargesAndVatWaivedWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);
        Money amountSubjectToVat = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isPenaltyCharge()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    LoanInstallmentCharge loanChargePerInstallment = loanCharge.getInstallmentLoanCharge(periodEnd);
                    if (loanChargePerInstallment != null) {
                        cumulative = cumulative.plus(loanChargePerInstallment.getAmountWaived(currency));
                        if (loanCharge.getCharge().isVatRequired()) {
                            amountSubjectToVat = amountSubjectToVat.plus(loanChargePerInstallment.getAmountWaived(currency));
                        }
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.getAmountWaived(currency));
                    if (loanCharge.getCharge().isVatRequired()) {
                        amountSubjectToVat = amountSubjectToVat.plus(loanCharge.getAmountWaived(currency));
                    }
                }

            }
        }

        return Pair.of(cumulative, amountSubjectToVat);
    }

    private Money cumulativePenaltyChargesWrittenOffWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isPenaltyCharge()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    LoanInstallmentCharge loanChargePerInstallment = loanCharge.getInstallmentLoanCharge(periodEnd);
                    if (loanChargePerInstallment != null) {
                        cumulative = cumulative.plus(loanChargePerInstallment.getAmountWrittenOff(currency));
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.getAmountWrittenOff(currency));
                }
            }
        }

        return cumulative;
    }

    private Pair<Money, Money> cumulativePenaltyChargesAndVatWrittenOffWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);
        Money amountSubjectToVat = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isPenaltyCharge()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    LoanInstallmentCharge loanChargePerInstallment = loanCharge.getInstallmentLoanCharge(periodEnd);
                    if (loanChargePerInstallment != null) {
                        cumulative = cumulative.plus(loanChargePerInstallment.getAmountWrittenOff(currency));
                        if (loanCharge.getCharge().isVatRequired()) {
                            amountSubjectToVat = amountSubjectToVat.plus(loanChargePerInstallment.getAmountWrittenOff(currency));
                        }
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.getAmountWrittenOff(currency));
                    if (loanCharge.getCharge().isVatRequired()) {
                        amountSubjectToVat = amountSubjectToVat.plus(loanCharge.getAmountWrittenOff(currency));
                    }
                }
            }
        }

        return Pair.of(cumulative, amountSubjectToVat);
    }

    private Money calculateVatOnAmount(BigDecimal vatPercentage, Money amountSubjectToVat) {
        BigDecimal vatConverted = vatPercentage.divide(BigDecimal.valueOf(100));
        return amountSubjectToVat.multipliedBy(vatConverted);
    }

    @SuppressWarnings("unused")
    private Money calculatePunitiveFeesForSinglePeriod(Money chargeAmount, Loan loan) {
        BigDecimal divisor = BigDecimal.valueOf(loan.getTermFrequency());
        return chargeAmount.dividedBy(divisor, MoneyHelper.getRoundingMode());
    }

    private Money calculateInstalmentChargeForInstalment(LoanCharge loanCharge, BigDecimal amount) {
        Loan loan = loanCharge.getLoan();
        BigDecimal totalCharge = LoanCharge.percentageOf(amount, loanCharge.getPercentage());
        BigDecimal divisor = BigDecimal.valueOf(loan.getTermFrequency());
        BigDecimal unitInstalmentCharge = totalCharge.divide(divisor, MoneyHelper.getRoundingMode());
        return Money.of(loan.getCurrency(), unitInstalmentCharge);
    }

}
