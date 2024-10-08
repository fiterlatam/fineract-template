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
package org.apache.fineract.portfolio.savings.domain;

import java.math.BigDecimal;
import java.util.List;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionData;
import org.springframework.stereotype.Component;

/**
 * A wrapper for dealing with side-effect free functionality related to a {@link SavingsAccount}'s
 * {@link SavingsAccountTransaction}'s.
 */
@Component
public final class SavingsAccountTransactionSummaryWrapper {

    public BigDecimal calculateTotalDeposits(final MonetaryCurrency currency, final List<SavingsAccountTransaction> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransaction transaction : transactions) {
            if ((transaction.isDepositAndNotReversed() || transaction.isDividendPayoutAndNotReversed() || transaction.isAmountRelease())
                    && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalDeposits(final CurrencyData currency, final List<SavingsAccountTransactionData> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransactionData transaction : transactions) {
            if (transaction.isDepositAndNotReversed() || transaction.isDividendPayoutAndNotReversed()) {
                total = total.plus(transaction.getAmount());
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalWithdrawals(final MonetaryCurrency currency, final List<SavingsAccountTransaction> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransaction transaction : transactions) {
            if ((transaction.isWithdrawal() || transaction.isAmountOnHold()) && transaction.isNotReversed()
                    && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalWithdrawals(final CurrencyData currency, final List<SavingsAccountTransactionData> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransactionData transaction : transactions) {
            if (transaction.isWithdrawal() && transaction.isNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount());
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalInterestPosted(final MonetaryCurrency currency, final List<SavingsAccountTransaction> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransaction transaction : transactions) {
            if (transaction.isInterestPostingAndNotReversed() && transaction.isNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalInterestPosted(final CurrencyData currency, final List<SavingsAccountTransactionData> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransactionData transaction : transactions) {
            if (transaction.isInterestPostingAndNotReversed() && transaction.isNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount());
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalInterestPosted(final MonetaryCurrency currency, final BigDecimal currentInterestPosted,
            final List<SavingsAccountTransaction> savingsAccountTransactions) {
        Money total = Money.of(currency, currentInterestPosted);
        for (final SavingsAccountTransaction transaction : savingsAccountTransactions) {
            if (transaction.isInterestPostingAndNotReversed() && transaction.isNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalWithdrawalFees(final MonetaryCurrency currency, final List<SavingsAccountTransaction> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransaction transaction : transactions) {
            if (transaction.isWithdrawalFeeAndNotReversed() && transaction.isNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalWithdrawalFees(final CurrencyData currency, final List<SavingsAccountTransactionData> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransactionData transaction : transactions) {
            if (transaction.isWithdrawalFeeAndNotReversed() && transaction.isNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount());
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalAnnualFees(final MonetaryCurrency currency, final List<SavingsAccountTransaction> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransaction transaction : transactions) {
            if (transaction.isAnnualFeeAndNotReversed() && transaction.isNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalAnnualFees(final CurrencyData currency, final List<SavingsAccountTransactionData> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransactionData transaction : transactions) {
            if (transaction.isAnnualFeeAndNotReversed() && transaction.isNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount());
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalFeesCharge(final MonetaryCurrency currency, final List<SavingsAccountTransaction> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransaction transaction : transactions) {
            if (transaction.isFeeChargeAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalFeesCharge(final CurrencyData currency, final List<SavingsAccountTransactionData> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransactionData transaction : transactions) {
            if (transaction.isFeeChargeAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount());
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalFeesChargeWaived(final MonetaryCurrency currency, final List<SavingsAccountTransaction> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransaction transaction : transactions) {
            if (transaction.isWaiveFeeChargeAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalFeesChargeWaived(final CurrencyData currency, final List<SavingsAccountTransactionData> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransactionData transaction : transactions) {
            if (transaction.isWaiveFeeChargeAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount());
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalPenaltyCharge(final MonetaryCurrency currency, final List<SavingsAccountTransaction> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransaction transaction : transactions) {
            if (transaction.isPenaltyChargeAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalPenaltyCharge(final CurrencyData currency, final List<SavingsAccountTransactionData> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransactionData transaction : transactions) {
            if (transaction.isPenaltyChargeAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount());
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalPenaltyChargeWaived(final MonetaryCurrency currency,
            final List<SavingsAccountTransaction> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransaction transaction : transactions) {
            if (transaction.isWaivePenaltyChargeAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalPenaltyChargeWaived(final CurrencyData currency,
            final List<SavingsAccountTransactionData> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransactionData transaction : transactions) {
            if (transaction.isWaivePenaltyChargeAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount());
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalOverdraftInterest(MonetaryCurrency currency, BigDecimal overdraftPosted,
            List<SavingsAccountTransaction> transactions) {
        Money total = Money.of(currency, overdraftPosted);
        for (final SavingsAccountTransaction transaction : transactions) {
            if (transaction.isOverdraftInterestAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalOverdraftInterest(MonetaryCurrency currency, List<SavingsAccountTransaction> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransaction transaction : transactions) {
            if (transaction.isOverdraftInterestAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalOverdraftInterest(CurrencyData currency, List<SavingsAccountTransactionData> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransactionData transaction : transactions) {
            if (transaction.isOverdraftInterestAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount());
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalWithholdTaxWithdrawal(MonetaryCurrency currency, List<SavingsAccountTransaction> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransaction transaction : transactions) {
            if (transaction.isWithHoldTaxAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount(currency));
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public BigDecimal calculateTotalWithholdTaxWithdrawal(CurrencyData currency, List<SavingsAccountTransactionData> transactions) {
        Money total = Money.zero(currency);
        for (final SavingsAccountTransactionData transaction : transactions) {
            if (transaction.isWithHoldTaxAndNotReversed() && !transaction.isReversalTransaction()) {
                total = total.plus(transaction.getAmount());
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public void updateTotalDeposits(SavingsAccountSummary summary, MonetaryCurrency currency,
            List<SavingsAccountTransaction> transactions) {
        BigDecimal totalDeposits = this.calculateTotalDeposits(currency, transactions);
        if (totalDeposits != null) {
            if (summary.getTotalDeposits() != null) {
                summary.setTotalDeposits(summary.getTotalDeposits().add(totalDeposits));
            } else {
                summary.setTotalDeposits(totalDeposits);
            }
        }
    }

    public void updateTotalWithdrawals(SavingsAccountSummary summary, MonetaryCurrency currency,
            List<SavingsAccountTransaction> transactions) {
        BigDecimal totalWithdrawals = this.calculateTotalWithdrawals(currency, transactions);
        if (totalWithdrawals != null) {
            if (summary.getTotalWithdrawals() != null) {
                summary.setTotalWithdrawals(summary.getTotalWithdrawals().add(totalWithdrawals));
            } else {
                summary.setTotalWithdrawals(totalWithdrawals);
            }
        }
    }

    public void updateTotalInterestPosted(SavingsAccountSummary summary, MonetaryCurrency currency,
            List<SavingsAccountTransaction> transactions) {
        BigDecimal totalInterestPosted = this.calculateTotalInterestPosted(currency, transactions, false);
        if (totalInterestPosted != null) {
            if (summary.getTotalInterestPosted() != null) {
                summary.setTotalInterestPosted(summary.getTotalInterestPosted().add(totalInterestPosted));
            } else {
                summary.setTotalInterestPosted(totalInterestPosted);
            }
        }
    }

    public BigDecimal calculateTotalInterestPosted(final MonetaryCurrency currency,
            final List<? extends SavingsAccountTransaction> transactions, boolean ignoreTaxedInterest) {
        Money total = Money.zero(currency);
        for (int i = 0; i < transactions.size(); i++) {
            SavingsAccountTransaction transaction = transactions.get(i);
            SavingsAccountTransaction nextTransaction = (i + 1) < transactions.size() ? transactions.get(i + 1) : null;
            if (transaction.isInterestPostingAndNotReversed() && transaction.isNotReversed()) {
                total = total.plus(transaction.getAmount(currency));
                if (ignoreTaxedInterest && nextTransaction != null && nextTransaction.isWithHoldTaxAndNotReversed()) {
                    total = total.minus(transaction.getAmount(currency));
                }
            }
        }
        return total.getAmountDefaultedToNullIfZero();
    }

    public void updateTotalWithdrawalFees(SavingsAccountSummary summary, MonetaryCurrency currency,
            List<SavingsAccountTransaction> transactions) {
        BigDecimal totalWithdrawalFees = this.calculateTotalWithdrawalFees(currency, transactions);
        if (totalWithdrawalFees != null) {
            if (summary.getTotalWithdrawalFees() != null) {
                summary.setTotalWithdrawalFees(summary.getTotalWithdrawalFees().add(totalWithdrawalFees));
            } else {
                summary.setTotalWithdrawalFees(totalWithdrawalFees);
            }
        }
    }

    public void updateTotalAnnualFees(SavingsAccountSummary summary, MonetaryCurrency currency,
            List<SavingsAccountTransaction> transactions) {
        BigDecimal totalAnnualFees = this.calculateTotalAnnualFees(currency, transactions);
        if (totalAnnualFees != null) {
            if (summary.getTotalAnnualFees() != null) {
                summary.setTotalAnnualFees(summary.getTotalAnnualFees().add(totalAnnualFees));
            } else {
                summary.setTotalAnnualFees(totalAnnualFees);
            }
        }
    }

    public void updateTotalFeesCharge(SavingsAccountSummary summary, MonetaryCurrency currency,
            List<SavingsAccountTransaction> transactions) {
        BigDecimal totalFeeCharge = this.calculateTotalFeesCharge(currency, transactions);
        if (totalFeeCharge != null) {
            if (summary.getTotalFeeCharge() != null) {
                summary.setTotalFeeCharge(summary.getTotalFeeCharge().add(totalFeeCharge));
            } else {
                summary.setTotalFeeCharge(totalFeeCharge);
            }
        }
    }

    public void updateTotalFeesChargeWaived(SavingsAccountSummary summary, MonetaryCurrency currency,
            List<SavingsAccountTransaction> transactions) {
        BigDecimal totalFeeChargeWaived = this.calculateTotalFeesChargeWaived(currency, transactions);
        if (totalFeeChargeWaived != null) {
            if (summary.getTotalFeeChargesWaived() != null) {
                summary.setTotalFeeChargesWaived(summary.getTotalFeeChargesWaived().add(totalFeeChargeWaived));
            } else {
                summary.setTotalFeeChargesWaived(totalFeeChargeWaived);
            }
        }
    }

    public void updateTotalPenaltyCharge(SavingsAccountSummary summary, MonetaryCurrency currency,
            List<SavingsAccountTransaction> transactions) {
        BigDecimal totalPenaltyCharge = this.calculateTotalPenaltyCharge(currency, transactions);
        if (totalPenaltyCharge != null) {
            if (summary.getTotalPenaltyCharge() != null) {
                summary.setTotalPenaltyCharge(summary.getTotalPenaltyCharge().add(totalPenaltyCharge));
            } else {
                summary.setTotalPenaltyCharge(totalPenaltyCharge);
            }
        }
    }

    public void updateTotalPenaltyChargeWaived(SavingsAccountSummary summary, MonetaryCurrency currency,
            List<SavingsAccountTransaction> transactions) {
        BigDecimal totalPenaltyChargeWaived = this.calculateTotalPenaltyChargeWaived(currency, transactions);
        if (totalPenaltyChargeWaived != null) {
            if (summary.getTotalPenaltyChargesWaived() != null) {
                summary.setTotalPenaltyChargesWaived(summary.getTotalPenaltyChargesWaived().add(totalPenaltyChargeWaived));
            } else {
                summary.setTotalPenaltyChargesWaived(totalPenaltyChargeWaived);
            }
        }
    }

    public void updateTotalOverdraftInterest(SavingsAccountSummary summary, MonetaryCurrency currency,
            List<SavingsAccountTransaction> transactions) {
        BigDecimal totalOverdraftInterest = this.calculateTotalOverdraftInterest(currency, transactions);
        if (totalOverdraftInterest != null) {
            if (summary.getTotalOverdraftInterestDerived() != null) {
                summary.setTotalOverdraftInterestDerived(summary.getTotalOverdraftInterestDerived().add(totalOverdraftInterest));
            } else {
                summary.setTotalOverdraftInterestDerived(totalOverdraftInterest);
            }
        }
    }

    public void updateTotalWithholdTaxWithdrawal(SavingsAccountSummary summary, MonetaryCurrency currency,
            List<SavingsAccountTransaction> transactions) {
        BigDecimal totalWithholdTax = this.calculateTotalWithholdTaxWithdrawal(currency, transactions);
        if (totalWithholdTax != null) {
            if (summary.getTotalWithholdTax() != null) {
                summary.setTotalWithholdTax(summary.getTotalWithholdTax().add(totalWithholdTax));
            } else {
                summary.setTotalWithholdTax(totalWithholdTax);
            }
        }
    }
}
