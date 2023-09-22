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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.repaymentwithpostdatedchecks.domain.PostDatedChecks;

@Entity
@Table(name = "m_loan_repayment_schedule")
public final class LoanRepaymentScheduleInstallment extends AbstractAuditableCustom
        implements Comparable<LoanRepaymentScheduleInstallment> {

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_id", referencedColumnName = "id")
    private Loan loan;

    @Column(name = "installment", nullable = false)
    private Integer installmentNumber;

    @Column(name = "fromdate", nullable = true)
    private LocalDate fromDate;

    @Column(name = "duedate", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_amount", scale = 6, precision = 19, nullable = true)
    private BigDecimal principal;

    @Column(name = "principal_completed_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal principalCompleted;

    @Column(name = "principal_writtenoff_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal principalWrittenOff;

    @Column(name = "interest_amount", scale = 6, precision = 19, nullable = true)
    private BigDecimal interestCharged;

    @Column(name = "interest_completed_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal interestPaid;

    @Column(name = "interest_waived_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal interestWaived;

    @Column(name = "interest_writtenoff_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal interestWrittenOff;

    @Column(name = "accrual_interest_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal interestAccrued;

    @Column(name = "reschedule_interest_portion", scale = 6, precision = 19, nullable = true)
    private BigDecimal rescheduleInterestPortion;

    @Column(name = "fee_charges_amount", scale = 6, precision = 19, nullable = true)
    private BigDecimal feeChargesCharged;

    @Column(name = "fee_charges_completed_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal feeChargesPaid;

    @Column(name = "fee_charges_writtenoff_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal feeChargesWrittenOff;

    @Column(name = "fee_charges_waived_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal feeChargesWaived;

    @Column(name = "accrual_fee_charges_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal feeAccrued;

    @Column(name = "penalty_charges_amount", scale = 6, precision = 19, nullable = true)
    private BigDecimal penaltyCharges;

    @Column(name = "penalty_charges_completed_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal penaltyChargesPaid;

    @Column(name = "penalty_charges_writtenoff_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal penaltyChargesWrittenOff;

    @Column(name = "penalty_charges_waived_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal penaltyChargesWaived;

    @Column(name = "accrual_penalty_charges_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal penaltyAccrued;

    @Column(name = "total_paid_in_advance_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal totalPaidInAdvance;

    @Column(name = "total_paid_late_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal totalPaidLate;

    @Column(name = "completed_derived", nullable = false)
    private boolean obligationsMet;

    @Column(name = "obligations_met_on_date")
    private LocalDate obligationsMetOnDate;

    @Column(name = "recalculated_interest_component", nullable = false)
    private boolean recalculatedInterestComponent;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "loanRepaymentScheduleInstallment")
    private Set<LoanInterestRecalcualtionAdditionalDetails> loanCompoundingDetails = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "loanRepaymentScheduleInstallment")
    private Set<PostDatedChecks> postDatedChecks;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "installment")
    private Set<LoanInstallmentCharge> installmentCharges = new HashSet<>();

    @Column(name = "vat_on_interest_charged_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal vatOnInterestCharged;

    @Column(name = "vat_on_interest_paid_derived", scale = 6, precision = 19)
    private BigDecimal vatOnInterestPaid;

    @Column(name = "vat_on_interest_writtenoff_derived", scale = 6, precision = 19)
    private BigDecimal vatOnInterestWrittenOff;

    @Column(name = "vat_on_interest_waived_derived", scale = 6, precision = 19)
    private BigDecimal vatOnInterestWaived;

    @Column(name = "vat_on_interest_outstanding", scale = 6, precision = 19)
    private BigDecimal vatOnInterestOutstanding;

    @Column(name = "vat_on_interest_overdue_derived", scale = 6, precision = 19)
    private BigDecimal vatOnInterestOverdue;

    @Column(name = "vat_on_charges_expected_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal vatOnChargeExpected;

    @Column(name = "vat_on_charges_paid_derived", scale = 6, precision = 19)
    private BigDecimal vatOnChargePaid;

    @Column(name = "vat_on_charges_writtenoff_derived", scale = 6, precision = 19)
    private BigDecimal vatOnChargeWrittenOff;

    @Column(name = "vat_on_charges_waived_derived", scale = 6, precision = 19)
    private BigDecimal vatOnChargeWaived;

    @Column(name = "vat_on_charges_outstanding_derived", scale = 6, precision = 19)
    private BigDecimal vatOnChargeOutstanding;

    @Column(name = "vat_on_charges_overdue_derived", scale = 6, precision = 19)
    private BigDecimal vatOnChargeOverdue;

    @Getter
    @Column(name = "vat_on_penalty_charges_expected_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal vatOnPenaltyChargeExpected;

    @Getter
    @Column(name = "vat_on_penalty_charges_paid_derived", scale = 6, precision = 19)
    private BigDecimal vatOnPenaltyChargePaid;

    @Getter
    @Column(name = "vat_on_penalty_charges_writtenoff_derived", scale = 6, precision = 19)
    private BigDecimal vatOnPenaltyChargeWrittenOff;

    @Getter
    @Column(name = "vat_on_penalty_charges_waived_derived", scale = 6, precision = 19)
    private BigDecimal vatOnPenaltyChargeWaived;

    @Getter
    @Column(name = "vat_on_penalty_charges_outstanding_derived", scale = 6, precision = 19)
    private BigDecimal vatOnPenaltyChargeOutstanding;

    @Getter
    @Column(name = "vat_on_penalty_charges_overdue_derived", scale = 6, precision = 19)
    private BigDecimal vatOnPenaltyChargeOverdue;

    LoanRepaymentScheduleInstallment() {
        this.installmentNumber = null;
        this.fromDate = null;
        this.dueDate = null;
        this.obligationsMet = false;
    }

    public LoanRepaymentScheduleInstallment(final Loan loan, final Integer installmentNumber, final LocalDate fromDate,
            final LocalDate dueDate, final BigDecimal principal, final BigDecimal interest, final BigDecimal feeCharges,
            final BigDecimal penaltyCharges, final boolean recalculatedInterestComponent,
            final Set<LoanInterestRecalcualtionAdditionalDetails> compoundingDetails, final BigDecimal rescheduleInterestPortion,
            final BigDecimal vatOnInterestCharged, final BigDecimal vatOnChargeExpected, final BigDecimal vatOnPenaltyChargeExpected) {
        this.loan = loan;
        this.installmentNumber = installmentNumber;
        this.fromDate = fromDate;
        this.dueDate = dueDate;
        this.principal = defaultToNullIfZero(principal);
        this.interestCharged = defaultToNullIfZero(interest);
        this.feeChargesCharged = defaultToNullIfZero(feeCharges);
        this.penaltyCharges = defaultToNullIfZero(penaltyCharges);
        this.obligationsMet = false;
        this.recalculatedInterestComponent = recalculatedInterestComponent;
        if (compoundingDetails != null) {
            compoundingDetails.forEach(cd -> cd.setLoanRepaymentScheduleInstallment(this));
        }
        this.loanCompoundingDetails = compoundingDetails;
        this.rescheduleInterestPortion = rescheduleInterestPortion;
        this.vatOnInterestCharged = vatOnInterestCharged;
        this.vatOnChargeExpected = vatOnChargeExpected;
        this.vatOnPenaltyChargeExpected = vatOnPenaltyChargeExpected;
    }

    public LoanRepaymentScheduleInstallment(final Loan loan, final Integer installmentNumber, final LocalDate fromDate,
            final LocalDate dueDate, final BigDecimal principal, final BigDecimal interest, final BigDecimal feeCharges,
            final BigDecimal penaltyCharges, final boolean recalculatedInterestComponent,
            final Set<LoanInterestRecalcualtionAdditionalDetails> compoundingDetails, final BigDecimal vatOnInterestCharged,
            final BigDecimal vatOnChargeExpected, final BigDecimal vatOnPenaltyChargeExpected) {
        this.loan = loan;
        this.installmentNumber = installmentNumber;
        this.fromDate = fromDate;
        this.dueDate = dueDate;
        this.principal = defaultToNullIfZero(principal);
        this.interestCharged = defaultToNullIfZero(interest);
        this.feeChargesCharged = defaultToNullIfZero(feeCharges);
        this.penaltyCharges = defaultToNullIfZero(penaltyCharges);
        this.obligationsMet = false;
        this.recalculatedInterestComponent = recalculatedInterestComponent;
        if (compoundingDetails != null) {
            compoundingDetails.forEach(cd -> cd.setLoanRepaymentScheduleInstallment(this));
        }
        this.loanCompoundingDetails = compoundingDetails;
        this.vatOnInterestCharged = vatOnInterestCharged;
        this.vatOnChargeExpected = vatOnChargeExpected;
        this.vatOnPenaltyChargeExpected = vatOnPenaltyChargeExpected;
    }

    public LoanRepaymentScheduleInstallment(final Loan loan) {
        this.loan = loan;
        this.installmentNumber = null;
        this.fromDate = null;
        this.dueDate = null;
        this.obligationsMet = false;
    }

    private BigDecimal defaultToNullIfZero(final BigDecimal value) {
        BigDecimal result = value;
        if (BigDecimal.ZERO.compareTo(value) == 0) {
            result = null;
        }
        return result;
    }

    public Loan getLoan() {
        return this.loan;
    }

    public Integer getInstallmentNumber() {
        return this.installmentNumber;
    }

    public LocalDate getFromDate() {
        return this.fromDate;
    }

    public void setPostDatedChecksToNull() {
        this.postDatedChecks = null;
    }

    public Set<PostDatedChecks> getPostDatedCheck() {
        return this.postDatedChecks;
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public Money getPrincipal(final MonetaryCurrency currency) {
        return Money.of(currency, this.principal);
    }

    public Money getPrincipalCompleted(final MonetaryCurrency currency) {
        return Money.of(currency, this.principalCompleted);
    }

    public void updateLoanRepaymentSchedule(final BigDecimal amountWaived) {
        this.feeChargesWaived = this.feeChargesWaived.subtract(amountWaived);
    }

    public Money getPrincipalWrittenOff(final MonetaryCurrency currency) {
        return Money.of(currency, this.principalWrittenOff);
    }

    public Money getPrincipalOutstanding(final MonetaryCurrency currency) {
        final Money principalAccountedFor = getPrincipalCompleted(currency).plus(getPrincipalWrittenOff(currency));
        return getPrincipal(currency).minus(principalAccountedFor);
    }

    public Money getInterestCharged(final MonetaryCurrency currency) {
        return Money.of(currency, this.interestCharged);
    }

    public Money getInterestPaid(final MonetaryCurrency currency) {
        return Money.of(currency, this.interestPaid);
    }

    public Money getInterestWaived(final MonetaryCurrency currency) {
        return Money.of(currency, this.interestWaived);
    }

    public Money getInterestWrittenOff(final MonetaryCurrency currency) {
        return Money.of(currency, this.interestWrittenOff);
    }

    public Money getInterestOutstanding(final MonetaryCurrency currency) {
        final Money interestAccountedFor = getInterestPaid(currency).plus(getInterestWaived(currency))
                .plus(getInterestWrittenOff(currency));
        return getInterestCharged(currency).minus(interestAccountedFor);
    }

    public Money getInterestAccrued(final MonetaryCurrency currency) {
        return Money.of(currency, this.interestAccrued);
    }

    public Money getFeeChargesCharged(final MonetaryCurrency currency) {
        return Money.of(currency, this.feeChargesCharged);
    }

    public Money getFeeChargesPaid(final MonetaryCurrency currency) {
        return Money.of(currency, this.feeChargesPaid);
    }

    public Money getFeeChargesWaived(final MonetaryCurrency currency) {
        return Money.of(currency, this.feeChargesWaived);
    }

    public Money getFeeChargesWrittenOff(final MonetaryCurrency currency) {
        return Money.of(currency, this.feeChargesWrittenOff);
    }

    public Money getFeeChargesOutstanding(final MonetaryCurrency currency) {
        final Money feeChargesAccountedFor = getFeeChargesPaid(currency).plus(getFeeChargesWaived(currency))
                .plus(getFeeChargesWrittenOff(currency));
        return getFeeChargesCharged(currency).minus(feeChargesAccountedFor);
    }

    public Money getFeeAccrued(final MonetaryCurrency currency) {
        return Money.of(currency, this.feeAccrued);
    }

    public Money getPenaltyChargesCharged(final MonetaryCurrency currency) {
        return Money.of(currency, this.penaltyCharges);
    }

    public Money getPenaltyChargesPaid(final MonetaryCurrency currency) {
        return Money.of(currency, this.penaltyChargesPaid);
    }

    public Money getPenaltyChargesWaived(final MonetaryCurrency currency) {
        return Money.of(currency, this.penaltyChargesWaived);
    }

    public Money getPenaltyChargesWrittenOff(final MonetaryCurrency currency) {
        return Money.of(currency, this.penaltyChargesWrittenOff);
    }

    public Money getPenaltyChargesOutstanding(final MonetaryCurrency currency) {
        final Money feeChargesAccountedFor = getPenaltyChargesPaid(currency).plus(getPenaltyChargesWaived(currency))
                .plus(getPenaltyChargesWrittenOff(currency));
        return getPenaltyChargesCharged(currency).minus(feeChargesAccountedFor);
    }

    public Money getPenaltyAccrued(final MonetaryCurrency currency) {
        return Money.of(currency, this.penaltyAccrued);
    }

    public boolean isInterestDue(final MonetaryCurrency currency) {
        return getInterestOutstanding(currency).isGreaterThanZero();
    }

    public Money getTotalPrincipalAndInterest(final MonetaryCurrency currency) {
        return getPrincipal(currency).plus(getInterestCharged(currency));
    }

    public Money getTotalOutstanding(final MonetaryCurrency currency) {
        return getPrincipalOutstanding(currency).plus(getInterestOutstanding(currency)).plus(getFeeChargesOutstanding(currency))
                .plus(getPenaltyChargesOutstanding(currency)).plus(getVatOnChargeOutstanding(currency))
                .plus(getVatOnInterestOutstanding(currency));
    }

    public void updateLoan(final Loan loan) {
        this.loan = loan;
    }

    public boolean isPartlyPaid() {
        return !this.obligationsMet && (this.interestPaid != null || this.feeChargesPaid != null || this.principalCompleted != null
                || this.vatOnInterestPaid != null || this.vatOnChargePaid != null);
    }

    public boolean isObligationsMet() {
        return this.obligationsMet;
    }

    public boolean isNotFullyPaidOff() {
        return !this.obligationsMet;
    }

    @Override
    public int compareTo(LoanRepaymentScheduleInstallment o) {
        return this.installmentNumber.compareTo(o.installmentNumber);
    }

    public boolean isPrincipalNotCompleted(final MonetaryCurrency currency) {
        return !isPrincipalCompleted(currency);
    }

    public boolean isPrincipalCompleted(final MonetaryCurrency currency) {
        return getPrincipalOutstanding(currency).isZero();
    }

    public void resetDerivedComponents() {
        this.principalCompleted = null;
        this.principalWrittenOff = null;
        this.interestPaid = null;
        this.interestWaived = null;
        this.interestWrittenOff = null;
        this.feeChargesPaid = null;
        this.feeChargesWaived = null;
        this.feeChargesWrittenOff = null;
        this.penaltyChargesPaid = null;
        this.penaltyChargesWaived = null;
        this.penaltyChargesWrittenOff = null;
        this.totalPaidInAdvance = null;
        this.totalPaidLate = null;

        this.obligationsMet = false;
        this.obligationsMetOnDate = null;

        this.vatOnInterestPaid = null;
        this.vatOnInterestWrittenOff = null;
        this.vatOnInterestWaived = null;
        this.vatOnInterestOutstanding = null;
        this.vatOnInterestOverdue = null;
        this.vatOnChargePaid = null;
        this.vatOnChargeWrittenOff = null;
        this.vatOnChargeWaived = null;
        this.vatOnChargeOutstanding = null;
        this.vatOnChargeOverdue = null;

        this.vatOnPenaltyChargePaid = null;
        this.vatOnPenaltyChargeWrittenOff = null;
        this.vatOnPenaltyChargeWaived = null;
        this.vatOnPenaltyChargeOutstanding = null;
        this.vatOnPenaltyChargeOverdue = null;
    }

    public void resetAccrualComponents() {
        this.interestAccrued = null;
        this.feeAccrued = null;
        this.penaltyAccrued = null;
    }

    public Money payPenaltyChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money penaltyPortionOfTransaction = Money.zero(currency);

        final Money penaltyChargesDue = getPenaltyChargesOutstanding(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(penaltyChargesDue)) {
            this.penaltyChargesPaid = getPenaltyChargesPaid(currency).plus(penaltyChargesDue).getAmount();
            penaltyPortionOfTransaction = penaltyPortionOfTransaction.plus(penaltyChargesDue);
        } else {
            this.penaltyChargesPaid = getPenaltyChargesPaid(currency).plus(transactionAmountRemaining).getAmount();
            penaltyPortionOfTransaction = penaltyPortionOfTransaction.plus(transactionAmountRemaining);
        }

        this.penaltyChargesPaid = defaultToNullIfZero(this.penaltyChargesPaid);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return penaltyPortionOfTransaction;
    }

    public Pair<Money, Money> payPenaltyChargesAndVatComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            BigDecimal vatPercentage) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money penaltyPortionOfTransaction = Money.zero(currency);
        Money vatPortionOfTransaction = Money.zero(currency);
        // we'll calculate the vat compo

        final Money penaltyChargesDue = getPenaltyChargesOutstanding(currency);
        BigDecimal vatConverted = vatPercentage.divide(BigDecimal.valueOf(100));
        final Money vatOnPenaltiesPortion = penaltyChargesDue.multipliedBy(vatConverted);
        final Money penaltyAndVat = vatOnPenaltiesPortion.plus(penaltyChargesDue);

        if (transactionAmountRemaining.isGreaterThanOrEqualTo(penaltyAndVat)) {

            this.penaltyChargesPaid = getPenaltyChargesPaid(currency).plus(penaltyChargesDue).getAmount();
            this.vatOnPenaltyChargePaid = getVatOnPenaltyChargePaid(currency).plus(vatOnPenaltiesPortion).getAmount();
            penaltyPortionOfTransaction = penaltyPortionOfTransaction.plus(penaltyChargesDue);
            vatPortionOfTransaction = vatPortionOfTransaction.plus(vatOnPenaltiesPortion);

        } else {
            BigDecimal percentageVatPlusOne = vatConverted.add(BigDecimal.ONE);
            BigDecimal feeChargesPortion = transactionAmountRemaining.getAmount().divide(percentageVatPlusOne,
                    MoneyHelper.getMathContext());
            BigDecimal vatPortion = transactionAmountRemaining.getAmount().subtract(feeChargesPortion);

            this.penaltyChargesPaid = getPenaltyChargesPaid(currency).plus(feeChargesPortion).getAmount();
            this.vatOnPenaltyChargePaid = getVatOnPenaltyChargePaid(currency).plus(vatPortion).getAmount();
            penaltyPortionOfTransaction = penaltyPortionOfTransaction.plus(feeChargesPortion);
            vatPortionOfTransaction = vatPortionOfTransaction.plus(vatPortion);
        }

        this.penaltyChargesPaid = defaultToNullIfZero(this.penaltyChargesPaid);
        this.vatOnPenaltyChargePaid = defaultToNullIfZero(this.vatOnPenaltyChargePaid);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, penaltyPortionOfTransaction);
        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, vatPortionOfTransaction);

        return Pair.of(penaltyPortionOfTransaction, vatPortionOfTransaction);
    }

    public Money payFeeChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money feePortionOfTransaction = Money.zero(currency);

        final Money feeChargesDue = getFeeChargesOutstanding(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(feeChargesDue)) {
            this.feeChargesPaid = getFeeChargesPaid(currency).plus(feeChargesDue).getAmount();
            feePortionOfTransaction = feePortionOfTransaction.plus(feeChargesDue);
        } else {
            this.feeChargesPaid = getFeeChargesPaid(currency).plus(transactionAmountRemaining).getAmount();
            feePortionOfTransaction = feePortionOfTransaction.plus(transactionAmountRemaining);
        }

        this.feeChargesPaid = defaultToNullIfZero(this.feeChargesPaid);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, feePortionOfTransaction);

        return feePortionOfTransaction;
    }

    public Pair<Money, Money> payFeeChargesAndVatComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final BigDecimal vatPercentage) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money feePortionOfTransaction = Money.zero(currency);
        Money vatPortionOfTransaction = Money.zero(currency);

        Money feeChargesDue = getFeeChargesOutstanding(currency);
        BigDecimal vatConverted = vatPercentage.divide(BigDecimal.valueOf(100));
        final Money vatOnFeesPortion = feeChargesDue.multipliedBy(vatConverted);
        final Money feeAndVat = vatOnFeesPortion.plus(feeChargesDue);

        if (transactionAmountRemaining.isGreaterThanOrEqualTo(feeAndVat)) {
            this.feeChargesPaid = getFeeChargesPaid(currency).plus(feeChargesDue).getAmount();
            this.vatOnChargePaid = getVatOnChargePaid(currency).plus(vatOnFeesPortion).getAmount();
            feePortionOfTransaction = feePortionOfTransaction.plus(feeChargesDue);
            vatPortionOfTransaction = vatPortionOfTransaction.plus(vatOnFeesPortion);
        } else {
            BigDecimal percentageVatPlusOne = vatConverted.add(BigDecimal.ONE);
            BigDecimal feeChargesPortion = transactionAmountRemaining.getAmount().divide(percentageVatPlusOne,
                    MoneyHelper.getMathContext());
            BigDecimal vatPortion = transactionAmountRemaining.getAmount().subtract(feeChargesPortion);

            this.feeChargesPaid = getFeeChargesPaid(currency).plus(feeChargesPortion).getAmount();
            this.vatOnChargePaid = getVatOnChargePaid(currency).plus(vatPortion).getAmount();
            feePortionOfTransaction = feePortionOfTransaction.plus(feeChargesPortion);
            vatPortionOfTransaction = vatPortionOfTransaction.plus(vatPortion);
        }

        this.feeChargesPaid = defaultToNullIfZero(this.feeChargesPaid);
        this.vatOnChargePaid = defaultToNullIfZero(this.vatOnChargePaid);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, feePortionOfTransaction);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, vatPortionOfTransaction);

        return Pair.of(feePortionOfTransaction, vatPortionOfTransaction);
    }

    public Money payInterestComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money interestPortionOfTransaction = Money.zero(currency);

        final Money interestDue = getInterestOutstanding(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(interestDue)) {
            this.interestPaid = getInterestPaid(currency).plus(interestDue).getAmount();
            interestPortionOfTransaction = interestPortionOfTransaction.plus(interestDue);
        } else {
            this.interestPaid = getInterestPaid(currency).plus(transactionAmountRemaining).getAmount();
            interestPortionOfTransaction = interestPortionOfTransaction.plus(transactionAmountRemaining);
        }

        this.interestPaid = defaultToNullIfZero(this.interestPaid);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, interestPortionOfTransaction);

        return interestPortionOfTransaction;
    }

    public Pair<Money, Money> payInterestAndVatComponents(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final BigDecimal vatPercentage) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money interestPortionOfTransaction = Money.zero(currency);
        Money vatOnInterestPortionOfTransaction = Money.zero(currency);

        final Money interestDue = getInterestOutstanding(currency);

        BigDecimal vatConverted = vatPercentage.divide(BigDecimal.valueOf(100));
        final Money vatOnInterestPortion = loan.calculateVatOnAmount(interestDue);
        final Money interestDueAndVat = interestDue.plus(vatOnInterestPortion);

        if (transactionAmountRemaining.isGreaterThanOrEqualTo(interestDueAndVat)) {
            this.interestPaid = getInterestPaid(currency).plus(interestDue).getAmount();
            this.vatOnInterestPaid = getVatOnInterestPaid(currency).plus(vatOnInterestPortion).getAmount();
            interestPortionOfTransaction = interestPortionOfTransaction.plus(interestDue);
            vatOnInterestPortionOfTransaction = vatOnInterestPortionOfTransaction.plus(vatOnInterestPortion);
        } else {
            BigDecimal percentageVatPlusOne = vatConverted.add(BigDecimal.ONE);
            BigDecimal interestPort = transactionAmountRemaining.getAmount().divide(percentageVatPlusOne, MoneyHelper.getMathContext());
            BigDecimal vatPortion = transactionAmountRemaining.getAmount().subtract(interestPort);

            this.interestPaid = getInterestPaid(currency).plus(interestPort).getAmount();
            this.vatOnInterestPaid = getVatOnInterestPaid(currency).plus(vatPortion).getAmount();
            interestPortionOfTransaction = interestPortionOfTransaction.plus(interestPort);
            vatOnInterestPortionOfTransaction = vatOnInterestPortionOfTransaction.plus(vatPortion);
        }

        this.interestPaid = defaultToNullIfZero(this.interestPaid);
        this.vatOnInterestPaid = defaultToNullIfZero(this.vatOnInterestPaid);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, interestPortionOfTransaction);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, vatOnInterestPortionOfTransaction);

        return Pair.of(interestPortionOfTransaction, vatOnInterestPortionOfTransaction);
    }

    public Money updateVatInterestComponents(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final Money interestPortion) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money interestPortionOfTransaction = Money.zero(currency);
        Money vatOnInterestPortionOfTransaction = loan.calculateVatOnAmount(interestPortion);

        interestPortionOfTransaction = interestPortionOfTransaction.plus(vatOnInterestPortionOfTransaction);
        this.vatOnInterestPaid = getVatOnInterestPaid(currency).plus(vatOnInterestPortionOfTransaction).getAmount();

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, interestPortionOfTransaction);

        return vatOnInterestPortionOfTransaction;
    }

    public Money updateVatFeeChargeComponents(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final Money feeChargesPortion) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money interestPortionOfTransaction = Money.zero(currency);
        Money vatOnInterestPortionOfTransaction = loan.calculateVatOnAmount(feeChargesPortion);

        interestPortionOfTransaction = interestPortionOfTransaction.plus(vatOnInterestPortionOfTransaction);
        this.vatOnChargePaid = getVatOnChargePaid(currency).plus(vatOnInterestPortionOfTransaction).getAmount();

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, interestPortionOfTransaction);

        return vatOnInterestPortionOfTransaction;
    }

    public Money updateVatPenaltyComponents(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final Money penaltyChargesPortion) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money interestPortionOfTransaction = Money.zero(currency);
        Money vatOnInterestPortionOfTransaction = loan.calculateVatOnAmount(penaltyChargesPortion);

        interestPortionOfTransaction = interestPortionOfTransaction.plus(vatOnInterestPortionOfTransaction);
        this.vatOnPenaltyChargePaid = getVatOnPenaltyChargePaid(currency).plus(vatOnInterestPortionOfTransaction).getAmount();

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, interestPortionOfTransaction);

        return vatOnInterestPortionOfTransaction;
    }

    public Money payPrincipalComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money principalPortionOfTransaction = Money.zero(currency);

        final Money principalDue = getPrincipalOutstanding(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(principalDue)) {
            this.principalCompleted = getPrincipalCompleted(currency).plus(principalDue).getAmount();
            principalPortionOfTransaction = principalPortionOfTransaction.plus(principalDue);
        } else {
            this.principalCompleted = getPrincipalCompleted(currency).plus(transactionAmountRemaining).getAmount();
            principalPortionOfTransaction = principalPortionOfTransaction.plus(transactionAmountRemaining);
        }

        this.principalCompleted = defaultToNullIfZero(this.principalCompleted);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, principalPortionOfTransaction);

        return principalPortionOfTransaction;
    }

    public Money waiveInterestComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {
        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money waivedInterestPortionOfTransaction = Money.zero(currency);

        final Money interestDue = getInterestOutstanding(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(interestDue)) {
            this.interestWaived = getInterestWaived(currency).plus(interestDue).getAmount();
            waivedInterestPortionOfTransaction = waivedInterestPortionOfTransaction.plus(interestDue);
        } else {
            this.interestWaived = getInterestWaived(currency).plus(transactionAmountRemaining).getAmount();
            waivedInterestPortionOfTransaction = waivedInterestPortionOfTransaction.plus(transactionAmountRemaining);
        }

        this.interestWaived = defaultToNullIfZero(this.interestWaived);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return waivedInterestPortionOfTransaction;
    }

    public Pair<Money, Money> waiveInterestAndVatComponents(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final BigDecimal vatPercentage) {
        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money waivedInterestPortionOfTransaction = Money.zero(currency);
        Money vatOnInterestWaivedPortionOfTransaction = Money.zero(currency);

        final Money interestDue = getInterestOutstanding(currency);

        BigDecimal vatConverted = vatPercentage.divide(BigDecimal.valueOf(100));
        final Money vatOnInterestPortion = interestDue.multipliedBy(vatConverted);

        final Money interestDueAndVat = interestDue.plus(vatOnInterestPortion);

        if (transactionAmountRemaining.isGreaterThanOrEqualTo(interestDueAndVat)) {
            this.interestWaived = getInterestWaived(currency).plus(interestDue).getAmount();
            this.vatOnInterestWaived = getVatOnInterestWaived(currency).plus(vatOnInterestPortion).getAmount();
            waivedInterestPortionOfTransaction = waivedInterestPortionOfTransaction.plus(interestDue);
            vatOnInterestWaivedPortionOfTransaction = vatOnInterestWaivedPortionOfTransaction.plus(vatOnInterestPortion);
        } else {
            BigDecimal percentageVatPlusOne = vatConverted.add(BigDecimal.ONE);
            BigDecimal interestPort = transactionAmountRemaining.getAmount().divide(percentageVatPlusOne);
            BigDecimal vatPortion = transactionAmountRemaining.getAmount().subtract(interestPort);

            this.interestWaived = getInterestWaived(currency).plus(interestPort).getAmount();
            this.vatOnInterestWaived = getVatOnInterestWaived(currency).plus(vatPortion).getAmount();
            waivedInterestPortionOfTransaction = waivedInterestPortionOfTransaction.plus(interestPort);
            vatOnInterestWaivedPortionOfTransaction = vatOnInterestWaivedPortionOfTransaction.plus(vatPortion);
        }

        this.interestWaived = defaultToNullIfZero(this.interestWaived);
        this.vatOnInterestWaived = defaultToNullIfZero(this.vatOnInterestWaived);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return Pair.of(waivedInterestPortionOfTransaction, vatOnInterestWaivedPortionOfTransaction);
    }

    public Money waivePenaltyChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {
        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money waivedPenaltyChargesPortionOfTransaction = Money.zero(currency);

        final Money penanltiesDue = getPenaltyChargesOutstanding(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(penanltiesDue)) {
            this.penaltyChargesWaived = getPenaltyChargesWaived(currency).plus(penanltiesDue).getAmount();
            waivedPenaltyChargesPortionOfTransaction = waivedPenaltyChargesPortionOfTransaction.plus(penanltiesDue);
        } else {
            this.penaltyChargesWaived = getPenaltyChargesWaived(currency).plus(transactionAmountRemaining).getAmount();
            waivedPenaltyChargesPortionOfTransaction = waivedPenaltyChargesPortionOfTransaction.plus(transactionAmountRemaining);
        }

        this.penaltyChargesWaived = defaultToNullIfZero(this.penaltyChargesWaived);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return waivedPenaltyChargesPortionOfTransaction;
    }

    public Pair<Money, Money> waivePenaltyChargesAndVatComponents(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final BigDecimal vatPercentage) {
        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money vatPortionOfTransaction = Money.zero(currency);
        Money waivedPenaltyChargesPortionOfTransaction = Money.zero(currency);
        final Money penanltiesDue = getPenaltyChargesOutstanding(currency);

        // we'll calculate the vat compo
        BigDecimal vatConverted = vatPercentage.divide(BigDecimal.valueOf(100));
        final Money vatOnPenaltiesPortion = penanltiesDue.multipliedBy(vatConverted);
        final Money penaltyAndVat = vatOnPenaltiesPortion.plus(penanltiesDue);

        if (transactionAmountRemaining.isGreaterThanOrEqualTo(penaltyAndVat)) {

            this.penaltyChargesWaived = getPenaltyChargesWaived(currency).plus(penanltiesDue).getAmount();
            this.vatOnPenaltyChargeWaived = getVatOnPenaltyChargeWaived(currency).plus(vatOnPenaltiesPortion).getAmount();
            waivedPenaltyChargesPortionOfTransaction = waivedPenaltyChargesPortionOfTransaction.plus(penanltiesDue);
            vatPortionOfTransaction = vatPortionOfTransaction.plus(vatOnPenaltiesPortion);

        } else {
            BigDecimal percentageVatPlusOne = vatConverted.add(BigDecimal.ONE);
            BigDecimal feeChargesPortion = transactionAmountRemaining.getAmount().divide(percentageVatPlusOne);
            BigDecimal vatPortion = transactionAmountRemaining.getAmount().subtract(feeChargesPortion);

            this.penaltyChargesWaived = getPenaltyChargesWaived(currency).plus(transactionAmountRemaining).getAmount();
            this.vatOnPenaltyChargeWaived = getVatOnPenaltyChargeWaived(currency).plus(vatPortion).getAmount();
            waivedPenaltyChargesPortionOfTransaction = waivedPenaltyChargesPortionOfTransaction.plus(transactionAmountRemaining);
            vatPortionOfTransaction = vatPortionOfTransaction.plus(vatPortion);
        }

        this.penaltyChargesWaived = defaultToNullIfZero(this.penaltyChargesWaived);
        this.vatOnPenaltyChargeWaived = defaultToNullIfZero(this.vatOnPenaltyChargeWaived);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return Pair.of(waivedPenaltyChargesPortionOfTransaction, vatPortionOfTransaction);
    }

    public Money waiveFeeChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {
        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money waivedFeeChargesPortionOfTransaction = Money.zero(currency);

        final Money feesDue = getFeeChargesOutstanding(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(feesDue)) {
            this.feeChargesWaived = getFeeChargesWaived(currency).plus(feesDue).getAmount();
            waivedFeeChargesPortionOfTransaction = waivedFeeChargesPortionOfTransaction.plus(feesDue);
        } else {
            this.feeChargesWaived = getFeeChargesWaived(currency).plus(transactionAmountRemaining).getAmount();
            waivedFeeChargesPortionOfTransaction = waivedFeeChargesPortionOfTransaction.plus(transactionAmountRemaining);
        }

        this.feeChargesWaived = defaultToNullIfZero(this.feeChargesWaived);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return waivedFeeChargesPortionOfTransaction;
    }

    public Pair<Money, Money> waiveFeeChargesAndVatComponents(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final BigDecimal vatPercentage) {
        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money waivedFeeChargesPortionOfTransaction = Money.zero(currency);
        Money waivedFeeChargesAndVatPortionOfTransaction = Money.zero(currency);

        final Money feesDue = getFeeChargesOutstanding(currency);
        // we'll calculate the vat compo
        BigDecimal vatConverted = vatPercentage.divide(BigDecimal.valueOf(100));
        final Money vatOnPenaltiesPortion = feesDue.multipliedBy(vatConverted);
        final Money feesAndVat = vatOnPenaltiesPortion.plus(feesDue);

        if (transactionAmountRemaining.isGreaterThanOrEqualTo(feesAndVat)) {

            this.feeChargesWaived = getFeeChargesWaived(currency).plus(feesDue).getAmount();
            this.vatOnChargeWaived = getVatOnChargeWaived(currency).plus(vatOnPenaltiesPortion).getAmount();
            waivedFeeChargesPortionOfTransaction = waivedFeeChargesPortionOfTransaction.plus(feesDue);
            waivedFeeChargesAndVatPortionOfTransaction = waivedFeeChargesAndVatPortionOfTransaction.plus(vatOnPenaltiesPortion);

        } else {
            BigDecimal percentageVatPlusOne = vatConverted.add(BigDecimal.ONE);
            BigDecimal feeChargesPortion = transactionAmountRemaining.getAmount().divide(percentageVatPlusOne);
            BigDecimal vatPortion = transactionAmountRemaining.getAmount().subtract(feeChargesPortion);

            this.feeChargesWaived = getFeeChargesWaived(currency).plus(transactionAmountRemaining).getAmount();
            this.vatOnChargeWaived = getVatOnChargeWaived(currency).plus(vatPortion).getAmount();
            waivedFeeChargesPortionOfTransaction = waivedFeeChargesPortionOfTransaction.plus(transactionAmountRemaining);
            waivedFeeChargesAndVatPortionOfTransaction = waivedFeeChargesAndVatPortionOfTransaction.plus(vatPortion);
        }

        this.feeChargesWaived = defaultToNullIfZero(this.feeChargesWaived);
        this.vatOnChargeWaived = defaultToNullIfZero(this.vatOnChargeWaived);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return Pair.of(waivedFeeChargesPortionOfTransaction, waivedFeeChargesAndVatPortionOfTransaction);
    }

    public Money writeOffOutstandingPrincipal(final LocalDate transactionDate, final MonetaryCurrency currency) {

        final Money principalDue = getPrincipalOutstanding(currency);
        this.principalWrittenOff = defaultToNullIfZero(principalDue.getAmount());

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return principalDue;
    }

    public Money writeOffOutstandingInterest(final LocalDate transactionDate, final MonetaryCurrency currency) {

        final Money interestDue = getInterestOutstanding(currency);
        this.interestWrittenOff = defaultToNullIfZero(interestDue.getAmount());

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return interestDue;
    }

    public Money writeOffOutstandingFeeCharges(final LocalDate transactionDate, final MonetaryCurrency currency) {
        final Money feeChargesOutstanding = getFeeChargesOutstanding(currency);
        this.feeChargesWrittenOff = defaultToNullIfZero(feeChargesOutstanding.getAmount());

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return feeChargesOutstanding;
    }

    public Money writeOffOutstandingPenaltyCharges(final LocalDate transactionDate, final MonetaryCurrency currency) {
        final Money penaltyChargesOutstanding = getPenaltyChargesOutstanding(currency);
        this.penaltyChargesWrittenOff = defaultToNullIfZero(penaltyChargesOutstanding.getAmount());

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return penaltyChargesOutstanding;
    }

    public boolean isOverdueOn(final LocalDate date) {
        return getDueDate().isBefore(date);
    }

    public void updateChargePortion(final Money feeChargesDue, final Money feeChargesWaived, final Money feeChargesWrittenOff,
            final Money penaltyChargesDue, final Money penaltyChargesWaived, final Money penaltyChargesWrittenOff,
            final Money chargeAmountDueForVatCalculation, final Money vatOnChargeWaived, final Money vatOnChargeWrittenOff,
            final Money vatOnPenaltyChargeExpected, final Money vatOnPenaltyChargeWaived, final Money vatOnPenaltyChargeWrittenOff) {
        this.feeChargesCharged = defaultToNullIfZero(feeChargesDue.getAmount());
        this.feeChargesWaived = defaultToNullIfZero(feeChargesWaived.getAmount());
        this.feeChargesWrittenOff = defaultToNullIfZero(feeChargesWrittenOff.getAmount());
        this.penaltyCharges = defaultToNullIfZero(penaltyChargesDue.getAmount());
        this.penaltyChargesWaived = defaultToNullIfZero(penaltyChargesWaived.getAmount());
        this.penaltyChargesWrittenOff = defaultToNullIfZero(penaltyChargesWrittenOff.getAmount());
        this.vatOnChargeExpected = defaultToNullIfZero(chargeAmountDueForVatCalculation.getAmount());
        this.vatOnChargeWaived = defaultToNullIfZero(vatOnChargeWaived.getAmount());
        this.vatOnChargeWrittenOff = defaultToNullIfZero(vatOnChargeWrittenOff.getAmount());
        this.vatOnPenaltyChargeExpected = defaultToNullIfZero(vatOnPenaltyChargeExpected.getAmount());
        this.vatOnPenaltyChargeWaived = defaultToNullIfZero(vatOnPenaltyChargeWaived.getAmount());
        this.vatOnPenaltyChargeWrittenOff = defaultToNullIfZero(vatOnPenaltyChargeWrittenOff.getAmount());
    }

    public void updateAccrualPortion(final Money interest, final Money feeCharges, final Money penalityCharges) {
        this.interestAccrued = defaultToNullIfZero(interest.getAmount());
        this.feeAccrued = defaultToNullIfZero(feeCharges.getAmount());
        this.penaltyAccrued = defaultToNullIfZero(penalityCharges.getAmount());
    }

    public void updateDerivedFields(final MonetaryCurrency currency, final LocalDate actualDisbursementDate) {
        if (!this.obligationsMet && getTotalOutstanding(currency).isZero()) {
            this.obligationsMet = true;
            this.obligationsMetOnDate = actualDisbursementDate;
        }
    }

    private void trackAdvanceAndLateTotalsForRepaymentPeriod(final LocalDate transactionDate, final MonetaryCurrency currency,
            final Money amountPaidInRepaymentPeriod) {
        if (isInAdvance(transactionDate)) {
            this.totalPaidInAdvance = asMoney(this.totalPaidInAdvance, currency).plus(amountPaidInRepaymentPeriod).getAmount();
        } else if (isLatePayment(transactionDate)) {
            this.totalPaidLate = asMoney(this.totalPaidLate, currency).plus(amountPaidInRepaymentPeriod).getAmount();
        }
    }

    private Money asMoney(final BigDecimal decimal, final MonetaryCurrency currency) {
        return Money.of(currency, decimal);
    }

    private boolean isInAdvance(final LocalDate transactionDate) {
        return transactionDate.isBefore(getDueDate());
    }

    private boolean isLatePayment(final LocalDate transactionDate) {
        return transactionDate.isAfter(getDueDate());
    }

    private void checkIfRepaymentPeriodObligationsAreMet(final LocalDate transactionDate, final MonetaryCurrency currency) {
        this.obligationsMet = getTotalOutstanding(currency).isZero();
        if (this.obligationsMet) {
            this.obligationsMetOnDate = transactionDate;
        } else {
            this.obligationsMetOnDate = null;
        }
    }

    public void updateDueDate(final LocalDate newDueDate) {
        if (newDueDate != null) {
            this.dueDate = newDueDate;
        }
    }

    public void updateFromDate(final LocalDate newFromDate) {
        if (newFromDate != null) {
            this.fromDate = newFromDate;
        }
    }

    public Money getTotalPaidInAdvance(final MonetaryCurrency currency) {
        return Money.of(currency, this.totalPaidInAdvance);
    }

    public Money getTotalPaidLate(final MonetaryCurrency currency) {
        return Money.of(currency, this.totalPaidLate);
    }

    public boolean isRecalculatedInterestComponent() {
        return this.recalculatedInterestComponent;
    }

    public void setRecalculatedInterestComponent(boolean recalculatedInterestComponent) {
        this.recalculatedInterestComponent = recalculatedInterestComponent;
    }

    public void updateInstallmentNumber(final Integer installmentNumber) {
        if (installmentNumber != null) {
            this.installmentNumber = installmentNumber;
        }
    }

    public void updateInterestCharged(final BigDecimal interestCharged) {
        this.interestCharged = interestCharged;
    }

    public void updateObligationMet(final Boolean obligationMet) {
        this.obligationsMet = obligationMet;
    }

    public void updateObligationMetOnDate(final LocalDate obligationsMetOnDate) {
        this.obligationsMetOnDate = obligationsMetOnDate;
    }

    public void updateInterestWrittenOff(final BigDecimal interestWrittenOff) {
        this.interestWrittenOff = interestWrittenOff;
    }

    public void updatePrincipal(final BigDecimal principal) {
        this.principal = principal;
    }

    public void updateVatOnInterestCharged(final BigDecimal interestVatWrittenOff) {
        this.vatOnInterestCharged = interestVatWrittenOff;
    }

    public static Comparator<LoanRepaymentScheduleInstallment> installmentNumberComparator = new Comparator<LoanRepaymentScheduleInstallment>() {

        @Override
        public int compare(LoanRepaymentScheduleInstallment arg0, LoanRepaymentScheduleInstallment arg1) {

            return arg0.getInstallmentNumber().compareTo(arg1.getInstallmentNumber());
        }
    };

    public BigDecimal getTotalPaidInAdvance() {
        return this.totalPaidInAdvance;
    }

    public BigDecimal getTotalPaidLate() {
        return this.totalPaidLate;
    }

    public LocalDate getObligationsMetOnDate() {
        return this.obligationsMetOnDate;
    }

    /********** UNPAY COMPONENTS ****/

    public Money unpayPenaltyChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money penaltyPortionOfTransactionDeducted = Money.zero(currency);

        final Money penaltyChargesCompleted = getPenaltyChargesPaid(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(penaltyChargesCompleted)) {
            this.penaltyChargesPaid = Money.zero(currency).getAmount();
            penaltyPortionOfTransactionDeducted = penaltyChargesCompleted;
        } else {
            this.penaltyChargesPaid = penaltyChargesCompleted.minus(transactionAmountRemaining).getAmount();
            penaltyPortionOfTransactionDeducted = transactionAmountRemaining;
        }

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return penaltyPortionOfTransactionDeducted;
    }

    public Money unpayFeeChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money feePortionOfTransactionDeducted = Money.zero(currency);

        final Money feeChargesCompleted = getFeeChargesPaid(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(feeChargesCompleted)) {
            this.feeChargesPaid = Money.zero(currency).getAmount();
            feePortionOfTransactionDeducted = feeChargesCompleted;
        } else {
            this.feeChargesPaid = feeChargesCompleted.minus(transactionAmountRemaining).getAmount();
            feePortionOfTransactionDeducted = transactionAmountRemaining;
        }

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        reduceAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, feePortionOfTransactionDeducted);

        return feePortionOfTransactionDeducted;
    }

    public Money unpayInterestComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money interestPortionOfTransactionDeducted = Money.zero(currency);

        final Money interestCompleted = getInterestPaid(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(interestCompleted)) {
            this.interestPaid = Money.zero(currency).getAmount();
            interestPortionOfTransactionDeducted = interestCompleted;
        } else {
            this.interestPaid = interestCompleted.minus(transactionAmountRemaining).getAmount();
            interestPortionOfTransactionDeducted = transactionAmountRemaining;
        }

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        reduceAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, interestPortionOfTransactionDeducted);

        return interestPortionOfTransactionDeducted;
    }

    public Money unpayPrincipalComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money principalPortionOfTransactionDeducted = Money.zero(currency);

        final Money principalCompleted = getPrincipalCompleted(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(principalCompleted)) {
            this.principalCompleted = Money.zero(currency).getAmount();
            principalPortionOfTransactionDeducted = principalCompleted;
        } else {
            this.principalCompleted = principalCompleted.minus(transactionAmountRemaining).getAmount();
            principalPortionOfTransactionDeducted = transactionAmountRemaining;
        }

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        reduceAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, principalPortionOfTransactionDeducted);

        return principalPortionOfTransactionDeducted;
    }

    private void reduceAdvanceAndLateTotalsForRepaymentPeriod(final LocalDate transactionDate, final MonetaryCurrency currency,
            final Money amountDeductedInRepaymentPeriod) {

        if (isInAdvance(transactionDate)) {
            Money mTotalPaidInAdvance = Money.of(currency, this.totalPaidInAdvance);

            if (mTotalPaidInAdvance.isLessThan(amountDeductedInRepaymentPeriod)
                    || mTotalPaidInAdvance.isEqualTo(amountDeductedInRepaymentPeriod)) {
                this.totalPaidInAdvance = Money.zero(currency).getAmount();
            } else {
                this.totalPaidInAdvance = mTotalPaidInAdvance.minus(amountDeductedInRepaymentPeriod).getAmount();
            }
        } else if (isLatePayment(transactionDate)) {
            Money mTotalPaidLate = Money.of(currency, this.totalPaidLate);

            if (mTotalPaidLate.isLessThan(amountDeductedInRepaymentPeriod) || mTotalPaidLate.isEqualTo(amountDeductedInRepaymentPeriod)) {
                this.totalPaidLate = Money.zero(currency).getAmount();
            } else {
                this.totalPaidLate = mTotalPaidLate.minus(amountDeductedInRepaymentPeriod).getAmount();
            }
        }
    }

    public Money getDue(MonetaryCurrency currency) {
        return getPrincipal(currency).plus(getInterestCharged(currency)).plus(getFeeChargesCharged(currency))
                .plus(getPenaltyChargesCharged(currency));
    }

    public Set<LoanInterestRecalcualtionAdditionalDetails> getLoanCompoundingDetails() {
        return this.loanCompoundingDetails;
    }

    public Money getAccruedInterestOutstanding(final MonetaryCurrency currency) {
        final Money interestAccountedFor = getInterestPaid(currency).plus(getInterestWaived(currency))
                .plus(getInterestWrittenOff(currency));
        return getInterestAccrued(currency).minus(interestAccountedFor);
    }

    public Money getTotalPaid(final MonetaryCurrency currency) {
        return getPenaltyChargesPaid(currency).plus(getFeeChargesPaid(currency)).plus(getInterestPaid(currency))
                .plus(getPrincipalCompleted(currency));
    }

    public BigDecimal getRescheduleInterestPortion() {
        return rescheduleInterestPortion;
    }

    public void setRescheduleInterestPortion(BigDecimal rescheduleInterestPortion) {
        this.rescheduleInterestPortion = rescheduleInterestPortion;
    }

    public void setFeeChargesWaived(final BigDecimal newFeeChargesCharged) {
        this.feeChargesWaived = newFeeChargesCharged;
    }

    public Set<LoanInstallmentCharge> getInstallmentCharges() {
        return installmentCharges;
    }

    public BigDecimal getVatOnInterestCharged() {
        return vatOnInterestCharged;
    }

    public BigDecimal getVatOnInterestPaid() {
        return vatOnInterestPaid;
    }

    public BigDecimal getVatOnInterestWrittenOff() {
        return vatOnInterestWrittenOff;
    }

    public BigDecimal getVatOnInterestWaived() {
        return vatOnInterestWaived;
    }

    public BigDecimal getVatOnInterestOutstanding() {
        return vatOnInterestOutstanding;
    }

    public BigDecimal getVatOnInterestOverdue() {
        return vatOnInterestOverdue;
    }

    public BigDecimal getVatOnChargeExpected() {
        return vatOnChargeExpected;
    }

    public BigDecimal getVatOnChargePaid() {
        return vatOnChargePaid;
    }

    public BigDecimal getVatOnChargeWrittenOff() {
        return vatOnChargeWrittenOff;
    }

    public BigDecimal getVatOnChargeWaived() {
        return vatOnChargeWaived;
    }

    public BigDecimal getVatOnChargeOutstanding() {
        return vatOnChargeOutstanding;
    }

    public BigDecimal getVatOnChargeOverdue() {
        return vatOnChargeOverdue;
    }

    public Money getVatOnInterestCharged(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnInterestCharged);
    }

    public Money getVatOnInterestPaid(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnInterestPaid);
    }

    public Money getVatOnInterestWrittenOff(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnInterestWrittenOff);
    }

    public Money getVatOnInterestWaived(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnInterestWaived);
    }

    public Money getVatOnInterestOutstanding(final MonetaryCurrency currency) {
        final Money vatOnInterestAccountedFor = getVatOnInterestPaid(currency).plus(getVatOnInterestWaived(currency))
                .plus(getVatOnInterestWrittenOff(currency));
        return getVatOnInterestCharged(currency).minus(vatOnInterestAccountedFor);
    }

    public Money getVatOnInterestOverdue(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnInterestOverdue);
    }

    public Money getVatOnChargeExpected(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnChargeExpected);
    }

    public Money getVatOnChargePaid(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnChargePaid);
    }

    public Money getVatOnChargeWrittenOff(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnChargeWrittenOff);
    }

    public Money getVatOnChargeWaived(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnChargeWaived);
    }

    public Money getVatOnChargeOutstanding(final MonetaryCurrency currency) {
        final Money vatOnChargeAccountedFor = getVatOnChargePaid(currency).plus(getVatOnChargeWaived(currency))
                .plus(getVatOnChargeWrittenOff(currency));
        return getVatOnChargeExpected(currency).minus(vatOnChargeAccountedFor);
    }

    public Money getVatOnPenaltyChargeOverdue(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnPenaltyChargeOverdue);
    }

    public Money getVatOnPenaltyChargeExpected(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnPenaltyChargeExpected);
    }

    public Money getVatOnPenaltyChargePaid(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnPenaltyChargePaid);
    }

    public Money getVatOnPenaltyChargeWrittenOff(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnPenaltyChargeWrittenOff);
    }

    public Money getVatOnPenaltyChargeWaived(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnPenaltyChargeWaived);
    }

    public Money getVatOnPenaltyChargeOutstanding(final MonetaryCurrency currency) {
        final Money vatOnPenaltyChargeAccountedFor = getVatOnPenaltyChargePaid(currency).plus(getVatOnPenaltyChargeWaived(currency))
                .plus(getVatOnPenaltyChargeWrittenOff(currency));
        return getVatOnPenaltyChargeExpected(currency).minus(vatOnPenaltyChargeAccountedFor);
    }

    public Money getVatOnChargeOverdue(final MonetaryCurrency currency) {
        return Money.of(currency, this.vatOnChargeOverdue);
    }

    public void setVatOnInterestCharged(BigDecimal vatOnInterestCharged) {
        this.vatOnInterestCharged = vatOnInterestCharged;
    }

    public void setVatOnInterestPaid(BigDecimal vatOnInterestPaid) {
        this.vatOnInterestPaid = vatOnInterestPaid;
    }

    public void setVatOnInterestWrittenOff(BigDecimal vatOnInterestWrittenOff) {
        this.vatOnInterestWrittenOff = vatOnInterestWrittenOff;
    }

    public void setVatOnInterestWaived(BigDecimal vatOnInterestWaived) {
        this.vatOnInterestWaived = vatOnInterestWaived;
    }

    public void setVatOnInterestOutstanding(BigDecimal vatOnInterestOutstanding) {
        this.vatOnInterestOutstanding = vatOnInterestOutstanding;
    }

    public void setVatOnInterestOverdue(BigDecimal vatOnInterestOverdue) {
        this.vatOnInterestOverdue = vatOnInterestOverdue;
    }

    public void setVatOnChargeExpected(BigDecimal vatOnChargeExpected) {
        this.vatOnChargeExpected = vatOnChargeExpected;
    }

    public void setVatOnChargePaid(BigDecimal vatOnChargePaid) {
        this.vatOnChargePaid = vatOnChargePaid;
    }

    public void setVatOnChargeWrittenOff(BigDecimal vatOnChargeWrittenOff) {
        this.vatOnChargeWrittenOff = vatOnChargeWrittenOff;
    }

    public void setVatOnChargeWaived(BigDecimal vatOnChargeWaived) {
        this.vatOnChargeWaived = vatOnChargeWaived;
    }

    public void setVatOnChargeOutstanding(BigDecimal vatOnChargeOutstanding) {
        this.vatOnChargeOutstanding = vatOnChargeOutstanding;
    }

    public void setVatOnChargeOverdue(BigDecimal vatOnChargeOverdue) {
        this.vatOnChargeOverdue = vatOnChargeOverdue;
    }

    public void setVatOnPenaltyChargeExpected(BigDecimal vatOnPenaltyChargeExpected) {
        this.vatOnPenaltyChargeExpected = vatOnPenaltyChargeExpected;
    }

    public void setVatOnPenaltyChargePaid(BigDecimal vatOnPenaltyChargePaid) {
        this.vatOnPenaltyChargePaid = vatOnPenaltyChargePaid;
    }

    public void setVatOnPenaltyChargeWrittenOff(BigDecimal vatOnPenaltyChargeWrittenOff) {
        this.vatOnPenaltyChargeWrittenOff = vatOnPenaltyChargeWrittenOff;
    }

    public void setVatOnPenaltyChargeWaived(BigDecimal vatOnPenaltyChargeWaived) {
        this.vatOnPenaltyChargeWaived = vatOnPenaltyChargeWaived;
    }

    public void setVatOnPenaltyChargeOutstanding(BigDecimal vatOnPenaltyChargeOutstanding) {
        this.vatOnPenaltyChargeOutstanding = vatOnPenaltyChargeOutstanding;
    }

    public void setVatOnPenaltyChargeOverdue(BigDecimal vatOnPenaltyChargeOverdue) {
        this.vatOnPenaltyChargeOverdue = vatOnPenaltyChargeOverdue;
    }
}
