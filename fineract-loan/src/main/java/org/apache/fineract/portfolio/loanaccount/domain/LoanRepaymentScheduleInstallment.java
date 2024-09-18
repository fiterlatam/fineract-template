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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleProcessingType;
import org.apache.fineract.portfolio.loanproduct.domain.AllocationType;
import org.apache.fineract.portfolio.repaymentwithpostdatedchecks.domain.PostDatedChecks;

@Entity
@Table(name = "m_loan_repayment_schedule")
public class LoanRepaymentScheduleInstallment extends AbstractAuditableWithUTCDateTimeCustom
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

    @Column(name = "is_additional", nullable = false)
    private boolean additional;

    @Column(name = "credits_amount", scale = 6, precision = 19, nullable = true)
    private BigDecimal credits;

    @Column(name = "is_down_payment", nullable = false)
    private boolean isDownPayment;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "loanRepaymentScheduleInstallment")
    private Set<LoanInterestRecalcualtionAdditionalDetails> loanCompoundingDetails = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "loanRepaymentScheduleInstallment")
    private Set<PostDatedChecks> postDatedChecks = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "installment")
    private Set<LoanInstallmentCharge> installmentCharges = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "installment")
    private Set<LoanTransactionToRepaymentScheduleMapping> loanTransactionToRepaymentScheduleMappings = new HashSet<>();

    @Transient
    private List<LoanChargeData> currentOutstandingLoanCharges;

    @Column(name = "advance_principal_amount", nullable = true)
    private BigDecimal advancePrincipalAmount;

    @Column(name = "recalculate_emi")
    private boolean recalculateEMI;

    @Column(name = "interest_recalculatedon_date", nullable = true)
    private LocalDate interestRecalculatedOnDate;

    @Column(name = "original_interest_charged", nullable = true)
    private BigDecimal originalInterestChargedAmount;

    public LoanRepaymentScheduleInstallment() {
        this.installmentNumber = null;
        this.fromDate = null;
        this.dueDate = null;
        this.obligationsMet = false;
    }

    public LoanRepaymentScheduleInstallment(final Loan loan, final Integer installmentNumber, final LocalDate fromDate,
            final LocalDate dueDate, final BigDecimal principal, final BigDecimal interest, final BigDecimal feeCharges,
            final BigDecimal penaltyCharges, final boolean recalculatedInterestComponent,
            final Set<LoanInterestRecalcualtionAdditionalDetails> compoundingDetails, final BigDecimal rescheduleInterestPortion) {
        this(loan, installmentNumber, fromDate, dueDate, principal, interest, feeCharges, penaltyCharges, recalculatedInterestComponent,
                compoundingDetails, rescheduleInterestPortion, false);
    }

    public LoanRepaymentScheduleInstallment(final Loan loan, final Integer installmentNumber, final LocalDate fromDate,
            final LocalDate dueDate, final BigDecimal principal, final BigDecimal interest, final BigDecimal feeCharges,
            final BigDecimal penaltyCharges, final boolean recalculatedInterestComponent,
            final Set<LoanInterestRecalcualtionAdditionalDetails> compoundingDetails, final BigDecimal rescheduleInterestPortion,
            final boolean isDownPayment) {
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
        this.isDownPayment = isDownPayment;
    }

    public void adjustSpecialWriteOff(final LocalDate fromDate, final LocalDate dueDate, final BigDecimal principal,
            final BigDecimal interest, final BigDecimal feeCharges, final BigDecimal penaltyCharges,
            final boolean recalculatedInterestComponent, final Set<LoanInterestRecalcualtionAdditionalDetails> compoundingDetails,
            final BigDecimal rescheduleInterestPortion, final boolean isDownPayment) {
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
        this.isDownPayment = isDownPayment;
    }

    public LoanRepaymentScheduleInstallment(final Loan loan, final Integer installmentNumber, final LocalDate fromDate,
            final LocalDate dueDate, final BigDecimal principal, final BigDecimal interest, final BigDecimal feeCharges,
            final BigDecimal penaltyCharges, final boolean recalculatedInterestComponent,
            final Set<LoanInterestRecalcualtionAdditionalDetails> compoundingDetails) {
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
        if (value != null && value.compareTo(BigDecimal.ZERO) == 0) {
            result = null;
        }
        return result;
    }

    private BigDecimal defaultToZeroIfNull(final BigDecimal value) {
        BigDecimal result = value;
        if (value == null) {
            result = BigDecimal.ZERO;
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

    public Money getCredits(final MonetaryCurrency currency) {
        return Money.of(currency, this.credits);
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

    public Money getInterestOutstanding(final MonetaryCurrency currency, final LocalDate tillDate) {
        Money interestOutstanding = Money.zero(currency);
        if (!DateUtils.isBefore(tillDate, this.dueDate)) {
            final Money interestAccountedFor = getInterestPaid(currency).plus(getInterestWaived(currency))
                    .plus(getInterestWrittenOff(currency));
            interestOutstanding = getInterestCharged(currency).minus(interestAccountedFor);
        } else if (DateUtils.isAfter(tillDate, this.fromDate)) {
            int totalPeriodDays = Math.toIntExact(ChronoUnit.DAYS.between(this.fromDate, this.dueDate));
            int tillDays = Math.toIntExact(ChronoUnit.DAYS.between(this.fromDate, tillDate));
            Money interestForCurrentPeriod = Money.of(currency,
                    BigDecimal.valueOf(calculateInterestForDays(totalPeriodDays, this.getInterestCharged(currency).getAmount(), tillDays)));
            Money interestAccountedForCurrentPeriod = this.getInterestWaived(currency).plus(this.getInterestPaid(currency))
                    .plus(this.getInterestWrittenOff(currency));
            if (interestForCurrentPeriod.isGreaterThan(interestAccountedForCurrentPeriod)) {
                interestOutstanding = interestForCurrentPeriod.minus(interestAccountedForCurrentPeriod);
            }
        }
        return interestOutstanding;
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

    public Money getFeeChargesOutstanding(final MonetaryCurrency currency, final LocalDate tillDate) {
        Money feeChargesOutstanding = Money.zero(currency);
        if (!DateUtils.isBefore(tillDate, this.dueDate)) {
            final Money feeChargesAccountedFor = getFeeChargesPaid(currency).plus(getFeeChargesWaived(currency))
                    .plus(getFeeChargesWrittenOff(currency));
            feeChargesOutstanding = getFeeChargesCharged(currency).minus(feeChargesAccountedFor);
        } else if (DateUtils.isAfter(tillDate, this.fromDate)) {
            final Money feeChargesAccountedFor = getFeeChargesPaid(currency).plus(getFeeChargesWaived(currency))
                    .plus(getFeeChargesWrittenOff(currency));
            feeChargesOutstanding = getFeeChargesCharged(currency).minus(feeChargesAccountedFor);
        }
        return feeChargesOutstanding;
    }

    public Money getFeeChargesOutstandingByType(final MonetaryCurrency currency, String chargeType) {
        Money amount = Money.zero(currency);
        if (chargeType == null) {
            return amount;
        }
        for (LoanInstallmentCharge installmentCharge : getInstallmentCharges()) {
            if (chargeType.equals("Honorarios")) {
                if (installmentCharge.getLoanCharge().getChargeCalculation().isFlatHono()) {
                    amount = amount.plus(getInstallmentChargeOutstandingAmount(currency, installmentCharge));
                }
            } else if (chargeType.equals("Aval")) {
                if (installmentCharge.getLoanCharge().isAvalCharge()) {
                    amount = amount.plus(getInstallmentChargeOutstandingAmount(currency, installmentCharge));
                }
            } else if (chargeType.equals("MandatoryInsurance")) {
                if (installmentCharge.getLoanCharge().getChargeCalculation().isMandatoryInsuranceCharge()) {
                    amount = amount.plus(getInstallmentChargeOutstandingAmount(currency, installmentCharge));
                }
                for (LoanInstallmentCharge vatCharge : getInstallmentCharges()) {
                    if (Objects.equals(installmentCharge.getLoanCharge().getCharge().getId(),
                            vatCharge.getLoanCharge().getCharge().getParentChargeId())) {
                        amount = amount.plus(getInstallmentChargeOutstandingAmount(currency, installmentCharge));
                    }
                }
            } else if (chargeType.equals("VoluntaryInsurance")) {
                if (installmentCharge.getLoanCharge().getChargeCalculation().isVoluntaryInsurance()) {
                    amount = amount.plus(getInstallmentChargeOutstandingAmount(currency, installmentCharge));
                }
                for (LoanInstallmentCharge vatCharge : getInstallmentCharges()) {
                    if (Objects.equals(installmentCharge.getLoanCharge().getCharge().getId(),
                            vatCharge.getLoanCharge().getCharge().getParentChargeId())) {
                        amount = amount.plus(getInstallmentChargeOutstandingAmount(currency, installmentCharge));
                    }
                }
            }
        }
        return amount;
    }

    public Money getInstallmentChargeOutstandingAmount(final MonetaryCurrency currency, LoanInstallmentCharge installmentCharge) {
        final Money feeChargesAccountedFor = installmentCharge.getAmountPaid(currency).plus(installmentCharge.getAmountWaived(currency))
                .plus(installmentCharge.getAmountWrittenOff(currency));

        return installmentCharge.getAmount(currency).minus(feeChargesAccountedFor);
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
                .plus(getPenaltyChargesOutstanding(currency));
    }

    public Money getRediferirAmount(final MonetaryCurrency currency) {
        return getInterestOutstanding(currency).plus(getFeeChargesOutstanding(currency)).plus(getPenaltyChargesOutstanding(currency));
    }

    public void updateLoan(final Loan loan) {
        this.loan = loan;
    }

    public boolean isPartlyPaid() {
        return !this.obligationsMet && (this.interestPaid != null || this.feeChargesPaid != null || this.principalCompleted != null);
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

    public BigDecimal getAdvancePrincipalAmount() {
        return advancePrincipalAmount == null ? BigDecimal.ZERO : advancePrincipalAmount;
    }

    public void setAdvancePrincipalAmount(BigDecimal advancePrincipalAmount) {
        this.advancePrincipalAmount = advancePrincipalAmount;
    }

    public boolean recalculateEMI() {
        return recalculateEMI;
    }

    public void setRecalculateEMI(boolean recalculateEMI) {
        this.recalculateEMI = recalculateEMI;
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
        this.advancePrincipalAmount = null;
        this.recalculateEMI = false;

        this.obligationsMet = false;
        this.obligationsMetOnDate = null;
        if (this.credits != null) {
            this.principal = this.principal.subtract(this.credits);
            this.credits = null;
        }

        if (this.originalInterestChargedAmount != null && this.originalInterestChargedAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.interestCharged = this.originalInterestChargedAmount;
            this.originalInterestChargedAmount = BigDecimal.ZERO;
            this.interestRecalculatedOnDate = null;
        }
    }

    public void resetPrincipalComponents() {
        this.principal = BigDecimal.ZERO;
        this.principalCompleted = BigDecimal.ZERO;
        this.principalWrittenOff = BigDecimal.ZERO;
    }

    public void resetAccrualComponents() {
        this.interestAccrued = null;
        this.feeAccrued = null;
        this.penaltyAccrued = null;
    }

    public void resetChargesCharged() {
        this.feeChargesCharged = null;
        this.penaltyCharges = null;
    }

    public boolean hasOverdueCharges() {
        return getPenaltyChargesOutstanding(getLoan().getCurrency()).isGreaterThanZero();
    }

    public interface PaymentFunction {

        Money accept(LocalDate transactionDate, Money transactionAmountRemaining, boolean isWriteOffTransaction);
    }

    public PaymentFunction getPaymentFunction(AllocationType allocationType, PaymentAction action) {
        return switch (allocationType) {
            case PENALTY -> PaymentAction.PAY.equals(action) ? this::payPenaltyChargesComponent
                    : PaymentAction.UNPAY.equals(action) ? this::unpayPenaltyChargesComponent : null;
            case FEE -> PaymentAction.PAY.equals(action) ? this::payFeeChargesComponent
                    : PaymentAction.UNPAY.equals(action) ? this::unpayFeeChargesComponent : null;
            case FEES -> PaymentAction.PAY.equals(action) ? this::payHonorariosChargesComponent
                    : PaymentAction.UNPAY.equals(action) ? this::unpayFeeChargesComponent : null;
            case AVAL -> PaymentAction.PAY.equals(action) ? this::payAvalChargesComponent
                    : PaymentAction.UNPAY.equals(action) ? this::unpayFeeChargesComponent : null;
            case MANDATORY_INSURANCE -> PaymentAction.PAY.equals(action) ? this::payMandatoryInsuranceChargesComponent
                    : PaymentAction.UNPAY.equals(action) ? this::unpayFeeChargesComponent : null;
            case VOLUNTARY_INSURANCE -> PaymentAction.PAY.equals(action) ? this::payVoluntaryInsuranceChargesComponent
                    : PaymentAction.UNPAY.equals(action) ? this::unpayFeeChargesComponent : null;
            case INTEREST -> PaymentAction.PAY.equals(action) ? this::payInterestComponent
                    : PaymentAction.UNPAY.equals(action) ? this::unpayInterestComponent : null;
            case PRINCIPAL -> PaymentAction.PAY.equals(action) ? this::payPrincipalComponent
                    : PaymentAction.UNPAY.equals(action) ? this::unpayPrincipalComponent : null;
        };
    }

    public Money payPenaltyChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final boolean isWriteOffTransaction) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money penaltyPortionOfTransaction = Money.zero(currency);

        if (transactionAmountRemaining.isZero()) {
            return penaltyPortionOfTransaction;
        }

        final Money penaltyChargesDue = getPenaltyChargesOutstanding(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(penaltyChargesDue)) {
            if (isWriteOffTransaction) {
                this.penaltyChargesWrittenOff = getPenaltyChargesWrittenOff(currency).plus(penaltyChargesDue).getAmount();
            } else {
                this.penaltyChargesPaid = getPenaltyChargesPaid(currency).plus(penaltyChargesDue).getAmount();
            }
            penaltyPortionOfTransaction = penaltyPortionOfTransaction.plus(penaltyChargesDue);
        } else {
            if (isWriteOffTransaction) {
                this.penaltyChargesWrittenOff = getPenaltyChargesWrittenOff(currency).plus(transactionAmountRemaining).getAmount();
            } else {
                this.penaltyChargesPaid = getPenaltyChargesPaid(currency).plus(transactionAmountRemaining).getAmount();
            }
            penaltyPortionOfTransaction = penaltyPortionOfTransaction.plus(transactionAmountRemaining);
        }

        this.penaltyChargesPaid = defaultToNullIfZero(this.penaltyChargesPaid);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, penaltyPortionOfTransaction);

        return penaltyPortionOfTransaction;
    }

    public Money payFeeChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final boolean isWriteOffTransaction) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money feePortionOfTransaction = Money.zero(currency);
        if (transactionAmountRemaining.isZero()) {
            return feePortionOfTransaction;
        }
        final Money feeChargesDue = getFeeChargesOutstanding(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(feeChargesDue)) {
            if (isWriteOffTransaction) {
                this.feeChargesWrittenOff = getFeeChargesWrittenOff(currency).plus(feeChargesDue).getAmount();
            } else {
                this.feeChargesPaid = getFeeChargesPaid(currency).plus(feeChargesDue).getAmount();
            }
            feePortionOfTransaction = feePortionOfTransaction.plus(feeChargesDue);
        } else {
            if (isWriteOffTransaction) {
                this.feeChargesWrittenOff = getFeeChargesWrittenOff(currency).plus(transactionAmountRemaining).getAmount();
            } else {
                this.feeChargesPaid = getFeeChargesPaid(currency).plus(transactionAmountRemaining).getAmount();
            }
            feePortionOfTransaction = feePortionOfTransaction.plus(transactionAmountRemaining);
        }

        this.feeChargesPaid = defaultToNullIfZero(this.feeChargesPaid);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, feePortionOfTransaction);

        return feePortionOfTransaction;
    }

    public Money payHonorariosChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final boolean isWriteOffTransaction) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money feePortionOfTransaction = Money.zero(currency);
        if (transactionAmountRemaining.isZero()) {
            return feePortionOfTransaction;
        }
        for (LoanInstallmentCharge installmentCharge : getInstallmentCharges()) {
            if (installmentCharge.getLoanCharge().getChargeCalculation().isFlatHono()) {
                feePortionOfTransaction = payLoanCharge(installmentCharge, transactionDate, transactionAmountRemaining, currency,
                        feePortionOfTransaction, isWriteOffTransaction);
            }
        }
        return feePortionOfTransaction;

    }

    public Money payAvalChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final boolean isWriteOffTransaction) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money feePortionOfTransaction = Money.zero(currency);
        if (transactionAmountRemaining.isZero()) {
            return feePortionOfTransaction;
        }
        for (LoanInstallmentCharge installmentCharge : getInstallmentCharges()) {
            if (installmentCharge.getLoanCharge().isAvalCharge()) {
                feePortionOfTransaction = payLoanCharge(installmentCharge, transactionDate, transactionAmountRemaining, currency,
                        feePortionOfTransaction, isWriteOffTransaction);
            }
        }
        return feePortionOfTransaction;

    }

    public Money payMandatoryInsuranceChargesComponent(final LocalDate transactionDate, Money transactionAmountRemaining,
            final boolean isWriteOffTransaction) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money feePortionOfTransaction = Money.zero(currency);
        if (transactionAmountRemaining.isZero()) {
            return feePortionOfTransaction;
        }
        for (LoanInstallmentCharge installmentCharge : getInstallmentCharges()) {
            if (installmentCharge.getLoanCharge().getChargeCalculation().isMandatoryInsuranceCharge()) {
                for (LoanInstallmentCharge vatCharge : getInstallmentCharges()) {
                    if (Objects.equals(installmentCharge.getLoanCharge().getCharge().getId(),
                            vatCharge.getLoanCharge().getCharge().getParentChargeId())) {
                        feePortionOfTransaction = payLoanCharge(vatCharge, transactionDate, transactionAmountRemaining, currency,
                                feePortionOfTransaction, isWriteOffTransaction);
                        transactionAmountRemaining = transactionAmountRemaining.minus(feePortionOfTransaction);
                        break;
                    }
                }
                feePortionOfTransaction = payLoanCharge(installmentCharge, transactionDate, transactionAmountRemaining, currency,
                        feePortionOfTransaction, isWriteOffTransaction);
            }
        }
        return feePortionOfTransaction;

    }

    public Money payVoluntaryInsuranceChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final boolean isWriteOffTransaction) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money feePortionOfTransaction = Money.zero(currency);
        if (transactionAmountRemaining.isZero()) {
            return feePortionOfTransaction;
        }
        for (LoanInstallmentCharge installmentCharge : getInstallmentCharges()) {
            if (installmentCharge.getLoanCharge().getChargeCalculation().isVoluntaryInsurance()) {
                for (LoanInstallmentCharge vatCharge : getInstallmentCharges()) {
                    if (Objects.equals(installmentCharge.getLoanCharge().getCharge().getId(),
                            vatCharge.getLoanCharge().getCharge().getParentChargeId())) {
                        feePortionOfTransaction = payLoanCharge(vatCharge, transactionDate, transactionAmountRemaining, currency,
                                feePortionOfTransaction, isWriteOffTransaction);
                    }
                }
                feePortionOfTransaction = payLoanCharge(installmentCharge, transactionDate, transactionAmountRemaining, currency,
                        feePortionOfTransaction, isWriteOffTransaction);
            }
        }
        return feePortionOfTransaction;

    }

    public Money payLoanCharge(LoanInstallmentCharge installmentCharge, final LocalDate transactionDate,
            final Money transactionAmountRemaining, final MonetaryCurrency currency, Money feePortionOfTransaction,
            final boolean isWriteOffTransaction) {
        if (transactionAmountRemaining.isZero()) {
            return feePortionOfTransaction;
        }
        Money feeChargePaid = Money.zero(currency);
        Money feeChargesDue = getInstallmentChargeOutstandingAmount(currency, installmentCharge);
        if (installmentCharge.getLoanCharge().isCustomPercentageBasedOfAnotherCharge()) {
            Money percentageAmountToBePaid = transactionAmountRemaining.percentageOf(installmentCharge.getLoanCharge().amountOrPercentage(),
                    RoundingMode.HALF_UP);
            if (percentageAmountToBePaid.isLessThan(feeChargesDue)) {
                feeChargesDue = Money.of(percentageAmountToBePaid.getCurrency(), percentageAmountToBePaid.getAmount());
            }
        }
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(feeChargesDue)) {
            if (isWriteOffTransaction) {
                this.feeChargesWrittenOff = getFeeChargesWrittenOff(currency).plus(feeChargesDue).getAmount();
            } else {
                this.feeChargesPaid = getFeeChargesPaid(currency).plus(feeChargesDue).getAmount();
            }
            feePortionOfTransaction = feePortionOfTransaction.plus(feeChargesDue);
            feeChargePaid = feeChargePaid.plus(feeChargesDue);
        } else {
            if (isWriteOffTransaction) {
                this.feeChargesWrittenOff = getFeeChargesWrittenOff(currency).plus(transactionAmountRemaining).getAmount();
            } else {
                this.feeChargesPaid = getFeeChargesPaid(currency).plus(transactionAmountRemaining).getAmount();
            }
            feePortionOfTransaction = feePortionOfTransaction.plus(transactionAmountRemaining);
            feeChargePaid = feeChargePaid.plus(transactionAmountRemaining);
        }
        // installmentCharge.updatePaidAmountBy(feePortionOfTransaction, Money.zero(currency));
        installmentCharge.getLoanCharge().updatePaidAmountBy(feeChargePaid, this.installmentNumber, Money.zero(currency),
                isWriteOffTransaction);
        this.feeChargesPaid = defaultToNullIfZero(this.feeChargesPaid);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, feeChargePaid);

        return feePortionOfTransaction;
    }

    public Money payInterestComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final boolean isWriteOffTransaction) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money interestPortionOfTransaction = Money.zero(currency);
        if (transactionAmountRemaining.isZero()) {
            return interestPortionOfTransaction;
        }

        Money interestDue = Money.zero(currency);
        if (this.getLoan() != null && this.getLoan().getLoanProductRelatedDetail().getLoanScheduleProcessingType()
                .equals(LoanScheduleProcessingType.HORIZONTAL)) {
            if (isOnOrBetween(transactionDate) && getInterestOutstanding(currency).isGreaterThanZero()) {
                final RoundingMode roundingMode = RoundingMode.HALF_UP;

                BigDecimal numberOfDaysForInterestCalculation = BigDecimal.ZERO;
                if (this.interestRecalculatedOnDate != null) {
                    if (this.interestRecalculatedOnDate.isAfter(transactionDate)) { // This should only be true if the
                                                                                    // repayment is reversed
                        numberOfDaysForInterestCalculation = BigDecimal.valueOf(ChronoUnit.DAYS.between(this.fromDate, transactionDate));
                    } else {
                        numberOfDaysForInterestCalculation = BigDecimal
                                .valueOf(ChronoUnit.DAYS.between(this.interestRecalculatedOnDate, transactionDate));
                    }
                } else {
                    numberOfDaysForInterestCalculation = BigDecimal.valueOf(ChronoUnit.DAYS.between(this.fromDate, transactionDate));
                }
                BigDecimal numberOfDaysInPeriod = BigDecimal.valueOf(ChronoUnit.DAYS.between(this.fromDate, this.dueDate));
                BigDecimal oneDayOfInterest = this.interestCharged.divide(numberOfDaysInPeriod, RoundingMode.HALF_UP);
                oneDayOfInterest = oneDayOfInterest.setScale(5, roundingMode);
                interestDue = Money.of(currency, oneDayOfInterest.multiply(numberOfDaysForInterestCalculation));
                if (interestDue.isGreaterThan(getInterestOutstanding(currency))) {
                    interestDue = getInterestOutstanding(currency);
                }

                //// Update installment interest charged if principal is fully paid during the accrual period
                // Keep the original interest charged in case the transaction is rollbacked and interest charged needs
                //// to be moved to original amount.
                if (this.getPrincipalOutstanding(currency).isZero() && this.interestRecalculatedOnDate == null) {
                    this.interestRecalculatedOnDate = transactionDate;
                    this.originalInterestChargedAmount = this.interestCharged;
                    this.interestCharged = getInterestPaid(currency).plus(getInterestWaived(currency)).plus(getInterestWrittenOff(currency))
                            .plus(interestDue).getAmount();
                }

            } else {
                interestDue = getInterestOutstanding(currency);
            }
        } else {
            interestDue = getInterestOutstanding(currency);
        }
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(interestDue)) {
            if (isWriteOffTransaction) {
                this.interestWrittenOff = getInterestWrittenOff(currency).plus(interestDue).getAmount();
            } else {
                this.interestPaid = getInterestPaid(currency).plus(interestDue).getAmount();
            }
            interestPortionOfTransaction = interestPortionOfTransaction.plus(interestDue);
        } else {
            if (isWriteOffTransaction) {
                this.interestWrittenOff = getInterestWrittenOff(currency).plus(transactionAmountRemaining).getAmount();
            } else {
                this.interestPaid = getInterestPaid(currency).plus(transactionAmountRemaining).getAmount();
            }
            interestPortionOfTransaction = interestPortionOfTransaction.plus(transactionAmountRemaining);
        }

        this.interestPaid = defaultToNullIfZero(this.interestPaid);

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, interestPortionOfTransaction);

        return interestPortionOfTransaction;
    }

    public Money payPrincipalComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final boolean isWriteOffTransaction) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money principalPortionOfTransaction = Money.zero(currency);
        if (transactionAmountRemaining.isZero()) {
            return principalPortionOfTransaction;
        }
        final Money principalDue = getPrincipalOutstanding(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(principalDue)) {
            if (isWriteOffTransaction) {
                this.principalWrittenOff = getPrincipalWrittenOff(currency).plus(principalDue).getAmount();
            } else {
                this.principalCompleted = getPrincipalCompleted(currency).plus(principalDue).getAmount();
            }
            principalPortionOfTransaction = principalPortionOfTransaction.plus(principalDue);
        } else {
            if (isWriteOffTransaction) {
                this.principalWrittenOff = getPrincipalWrittenOff(currency).plus(transactionAmountRemaining).getAmount();
            } else {
                this.principalCompleted = getPrincipalCompleted(currency).plus(transactionAmountRemaining).getAmount();
            }
            principalPortionOfTransaction = principalPortionOfTransaction.plus(transactionAmountRemaining);
        }

        this.principalCompleted = defaultToNullIfZero(this.principalCompleted);

        //// Update installment interest charged if principal is fully paid during the accrual period and interest has
        //// also been recalculated and paid
        // Keep the original interest charged in case the transaction is rollbacked.
        if (this.getLoan() != null && this.getLoan().getLoanProductRelatedDetail().getLoanScheduleProcessingType()
                .equals(LoanScheduleProcessingType.HORIZONTAL)) {
            if (isOnOrBetween(transactionDate)) {
                if (this.getPrincipalOutstanding(currency).isZero() && this.interestRecalculatedOnDate != null) {
                    this.interestRecalculatedOnDate = transactionDate;
                    this.originalInterestChargedAmount = this.interestCharged;
                    this.interestCharged = getInterestPaid(currency).plus(getInterestWaived(currency)).plus(getInterestWrittenOff(currency))
                            .getAmount();
                }
            }
        }

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        trackAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, principalPortionOfTransaction);

        return principalPortionOfTransaction;
    }

    public Money waiveInterestComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {
        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money waivedInterestPortionOfTransaction = Money.zero(currency);
        if (transactionAmountRemaining.isZero()) {
            return waivedInterestPortionOfTransaction;
        }
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

    public Money waivePenaltyChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {
        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money waivedPenaltyChargesPortionOfTransaction = Money.zero(currency);
        if (transactionAmountRemaining.isZero()) {
            return waivedPenaltyChargesPortionOfTransaction;
        }
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

    public Money waiveFeeChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining) {
        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money waivedFeeChargesPortionOfTransaction = Money.zero(currency);
        if (transactionAmountRemaining.isZero()) {
            return waivedFeeChargesPortionOfTransaction;
        }
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

    public Money writeOffOutstandingPrincipal(final Money principalAmountRemaining, final LocalDate transactionDate,
            final MonetaryCurrency currency) {
        final Money principalDue = getPrincipalOutstanding(currency);
        Money principalPortionWrittenOff = Money.zero(currency);
        if (principalDue.isGreaterThanZero() && principalAmountRemaining.isGreaterThanZero()) {
            if (principalAmountRemaining.isGreaterThan(principalDue)) {
                this.principalWrittenOff = defaultToZeroIfNull(principalDue.getAmount());
                principalPortionWrittenOff = principalPortionWrittenOff.plus(principalDue);
            } else {
                this.principalWrittenOff = defaultToZeroIfNull(principalAmountRemaining.getAmount());
                principalPortionWrittenOff = principalPortionWrittenOff.plus(principalAmountRemaining);
            }
            checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);
        }
        return principalPortionWrittenOff;
    }

    public Money writeOffOutstandingInterest(final Money interestAmountRemaining, final LocalDate transactionDate,
            final MonetaryCurrency currency) {
        final Money interestDue = getInterestOutstanding(currency, transactionDate);
        Money interestPortionWrittenOff = Money.zero(currency);
        if (interestDue.isGreaterThanZero() && interestAmountRemaining.isGreaterThanZero()) {
            if (interestAmountRemaining.isGreaterThan(interestDue)) {
                this.interestWrittenOff = defaultToZeroIfNull(this.interestWrittenOff).add(interestDue.getAmount());
                interestPortionWrittenOff = interestPortionWrittenOff.plus(interestDue);
            } else {
                this.interestWrittenOff = defaultToZeroIfNull(this.interestWrittenOff).add(interestAmountRemaining.getAmount());
                interestPortionWrittenOff = interestPortionWrittenOff.plus(interestAmountRemaining);
            }
            checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);
        }
        return interestPortionWrittenOff;
    }

    public Money writeOffOutstandingFeeCharges(final Money feeChargesAmountRemaining, final LocalDate transactionDate,
            final MonetaryCurrency currency) {
        final Money feeChargesDue = getFeeChargesOutstanding(currency, transactionDate);
        Money feeChargesPortionWrittenOff = Money.zero(currency);
        if (feeChargesAmountRemaining.isGreaterThanZero() && feeChargesDue.isGreaterThanZero()) {
            if (feeChargesAmountRemaining.isGreaterThan(feeChargesDue)) {
                this.feeChargesWrittenOff = defaultToZeroIfNull(this.feeChargesWrittenOff).add(feeChargesDue.getAmount());
                feeChargesPortionWrittenOff = feeChargesPortionWrittenOff.plus(feeChargesDue);
            } else {
                this.feeChargesWrittenOff = defaultToZeroIfNull(this.feeChargesWrittenOff).add(feeChargesAmountRemaining.getAmount());
                feeChargesPortionWrittenOff = feeChargesPortionWrittenOff.plus(feeChargesAmountRemaining);
            }
            checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);
        }
        return feeChargesPortionWrittenOff;
    }

    public Money writeOffOutstandingPenaltyCharges(final Money penaltyChargesAmountRemaining, final LocalDate transactionDate,
            final MonetaryCurrency currency) {
        final Money penaltyChargesDue = getPenaltyChargesOutstanding(currency);
        Money penaltyChargesPortionWrittenOff = Money.zero(currency);
        if (penaltyChargesAmountRemaining.isGreaterThanZero() && penaltyChargesDue.isGreaterThanZero()) {
            if (penaltyChargesAmountRemaining.isGreaterThan(penaltyChargesDue)) {
                this.penaltyChargesWrittenOff = defaultToZeroIfNull(this.penaltyChargesWrittenOff).add(penaltyChargesDue.getAmount());
                penaltyChargesPortionWrittenOff = penaltyChargesPortionWrittenOff.plus(penaltyChargesDue);
            } else {
                this.penaltyChargesWrittenOff = defaultToZeroIfNull(this.penaltyChargesWrittenOff)
                        .add(penaltyChargesAmountRemaining.getAmount());
                penaltyChargesPortionWrittenOff = penaltyChargesPortionWrittenOff.plus(penaltyChargesAmountRemaining);
            }
            checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);
        }
        return penaltyChargesPortionWrittenOff;
    }

    public double calculateInterestForDays(int daysInPeriod, BigDecimal interest, int days) {
        if (interest.doubleValue() == 0) {
            return 0;
        }
        return interest.doubleValue() / daysInPeriod * days;
    }

    public boolean isOverdueOn(final LocalDate date) {
        return DateUtils.isAfter(date, getDueDate());
    }

    public void updateChargePortion(final Money feeChargesDue, final Money feeChargesWaived, final Money feeChargesWrittenOff,
            final Money penaltyChargesDue, final Money penaltyChargesWaived, final Money penaltyChargesWrittenOff) {
        this.feeChargesCharged = defaultToNullIfZero(feeChargesDue.getAmount());
        this.feeChargesWaived = defaultToNullIfZero(feeChargesWaived.getAmount());
        this.feeChargesWrittenOff = defaultToNullIfZero(feeChargesWrittenOff.getAmount());
        this.penaltyCharges = defaultToNullIfZero(penaltyChargesDue.getAmount());
        this.penaltyChargesWaived = defaultToNullIfZero(penaltyChargesWaived.getAmount());
        this.penaltyChargesWrittenOff = defaultToNullIfZero(penaltyChargesWrittenOff.getAmount());
    }

    public void addToChargePortion(final Money feeChargesDue, final Money feeChargesWaived, final Money feeChargesWrittenOff,
            final Money penaltyChargesDue, final Money penaltyChargesWaived, final Money penaltyChargesWrittenOff) {
        this.feeChargesCharged = defaultToNullIfZero(feeChargesDue.plus(this.feeChargesCharged).getAmount());
        this.feeChargesWaived = defaultToNullIfZero(feeChargesWaived.plus(this.feeChargesWaived).getAmount());
        this.feeChargesWrittenOff = defaultToNullIfZero(feeChargesWrittenOff.plus(this.feeChargesWrittenOff).getAmount());
        this.penaltyCharges = defaultToNullIfZero(penaltyChargesDue.plus(this.penaltyCharges).getAmount());
        this.penaltyChargesWaived = defaultToNullIfZero(penaltyChargesWaived.plus(this.penaltyChargesWaived).getAmount());
        this.penaltyChargesWrittenOff = defaultToNullIfZero(penaltyChargesWrittenOff.plus(this.penaltyChargesWrittenOff).getAmount());
        checkIfRepaymentPeriodObligationsAreMet(getObligationsMetOnDate(), feeChargesDue.getCurrency());
    }

    public void adjustFeeChargePortion(final Money feeChargesDue) {
        this.feeChargesCharged = defaultToNullIfZero(this.feeChargesCharged.subtract(feeChargesDue.getAmount()));
        checkIfRepaymentPeriodObligationsAreMet(getObligationsMetOnDate(), feeChargesDue.getCurrency());
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
        return DateUtils.isBefore(transactionDate, getDueDate());
    }

    private boolean isLatePayment(final LocalDate transactionDate) {
        return DateUtils.isAfter(transactionDate, getDueDate());
    }

    public void checkIfRepaymentPeriodObligationsAreMet(final LocalDate transactionDate, final MonetaryCurrency currency) {
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

    public void updatePrincipal(final BigDecimal principal) {
        this.principal = principal;
    }

    public void addToPrincipal(final LocalDate transactionDate, final Money transactionAmount) {
        if (this.principal == null) {
            this.principal = transactionAmount.getAmount();
        } else {
            this.principal = this.principal.add(transactionAmount.getAmount());
        }
        checkIfRepaymentPeriodObligationsAreMet(transactionDate, transactionAmount.getCurrency());
    }

    public void addToCredits(final BigDecimal amount) {
        if (this.credits == null) {
            this.credits = amount;
        } else {
            this.credits = this.credits.add(amount);
        }
    }

    public BigDecimal getTotalPaidLate() {
        return this.totalPaidLate;
    }

    public LocalDate getObligationsMetOnDate() {
        return this.obligationsMetOnDate;
    }

    /********** UNPAY COMPONENTS ****/

    public Money unpayPenaltyChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final boolean isWriteOffTransaction) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money penaltyPortionOfTransactionDeducted;

        final Money penaltyChargesCompleted = getPenaltyChargesPaid(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(penaltyChargesCompleted)) {
            if (isWriteOffTransaction) {
                this.penaltyChargesWrittenOff = Money.zero(currency).getAmount();
            } else {
                this.penaltyChargesPaid = Money.zero(currency).getAmount();
            }
            penaltyPortionOfTransactionDeducted = penaltyChargesCompleted;
        } else {
            if (isWriteOffTransaction) {
                this.penaltyChargesWrittenOff = penaltyChargesCompleted.minus(transactionAmountRemaining).getAmount();
            } else {
                this.penaltyChargesPaid = penaltyChargesCompleted.minus(transactionAmountRemaining).getAmount();
            }
            penaltyPortionOfTransactionDeducted = transactionAmountRemaining;
        }

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        return penaltyPortionOfTransactionDeducted;
    }

    public Money unpayFeeChargesComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final boolean isWriteOffTransaction) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money feePortionOfTransactionDeducted;

        final Money feeChargesCompleted = getFeeChargesPaid(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(feeChargesCompleted)) {
            if (isWriteOffTransaction) {
                this.feeChargesWrittenOff = Money.zero(currency).getAmount();
            } else {
                this.feeChargesPaid = Money.zero(currency).getAmount();
            }
            feePortionOfTransactionDeducted = feeChargesCompleted;
        } else {
            if (isWriteOffTransaction) {
                this.feeChargesWrittenOff = feeChargesCompleted.minus(transactionAmountRemaining).getAmount();
            } else {
                this.feeChargesPaid = feeChargesCompleted.minus(transactionAmountRemaining).getAmount();
            }
            feePortionOfTransactionDeducted = transactionAmountRemaining;
        }

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        reduceAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, feePortionOfTransactionDeducted);

        return feePortionOfTransactionDeducted;
    }

    public Money unpayInterestComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final boolean isWriteOffTransaction) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money interestPortionOfTransactionDeducted;

        final Money interestCompleted = getInterestPaid(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(interestCompleted)) {
            if (isWriteOffTransaction) {
                this.interestWrittenOff = Money.zero(currency).getAmount();
            } else {
                this.interestPaid = Money.zero(currency).getAmount();
            }
            interestPortionOfTransactionDeducted = interestCompleted;
        } else {
            this.interestPaid = interestCompleted.minus(transactionAmountRemaining).getAmount();
            interestPortionOfTransactionDeducted = transactionAmountRemaining;
        }

        checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency);

        reduceAdvanceAndLateTotalsForRepaymentPeriod(transactionDate, currency, interestPortionOfTransactionDeducted);

        return interestPortionOfTransactionDeducted;
    }

    public Money unpayPrincipalComponent(final LocalDate transactionDate, final Money transactionAmountRemaining,
            final boolean isWriteOffTransaction) {

        final MonetaryCurrency currency = transactionAmountRemaining.getCurrency();
        Money principalPortionOfTransactionDeducted;

        final Money principalCompleted = getPrincipalCompleted(currency);
        if (transactionAmountRemaining.isGreaterThanOrEqualTo(principalCompleted)) {
            if (isWriteOffTransaction) {
                this.principalWrittenOff = Money.zero(currency).getAmount();
            } else {
                this.principalCompleted = Money.zero(currency).getAmount();
            }
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

    public void updateCredits(final LocalDate transactionDate, final Money transactionAmount) {
        addToCredits(transactionAmount.getAmount());
        addToPrincipal(transactionDate, transactionAmount);
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

    public void setPenaltyChargesWaived(final BigDecimal newPenaltyChargesCharged) {
        this.penaltyChargesWaived = newPenaltyChargesCharged;
    }

    public Set<LoanInstallmentCharge> getInstallmentCharges() {
        return installmentCharges;
    }

    public boolean isAdditional() {
        return additional;
    }

    public void markAsAdditional() {
        this.additional = true;
    }

    public Set<LoanTransactionToRepaymentScheduleMapping> getLoanTransactionToRepaymentScheduleMappings() {
        return this.loanTransactionToRepaymentScheduleMappings;
    }

    public boolean isDownPayment() {
        return isDownPayment;
    }

    public void resetBalances() {
        resetDerivedComponents();
        resetPrincipalDue();
        resetChargesCharged();
    }

    public void resetPrincipalDue() {
        this.principal = null;
    }

    public enum PaymentAction {
        PAY, UNPAY
    }

    public void setCurrentOutstandingLoanCharges(List<LoanChargeData> currentOutstandingLoanCharges) {
        this.currentOutstandingLoanCharges = currentOutstandingLoanCharges;
    }

    public List<LoanChargeData> getCurrentOutstandingLoanCharges() {
        return currentOutstandingLoanCharges;
    }

    public void addAccruedInterest(Money interestAccrued) {
        this.interestAccrued = defaultToZeroIfNull(this.interestAccrued).add(interestAccrued.getAmount());
    }

    public Money getAccruedInterest(MonetaryCurrency currency) {
        return Money.of(currency, this.interestAccrued);
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }

    public void setInterestCharged(BigDecimal interestCharged) {
        this.interestCharged = interestCharged;
    }

    public void setFeeChargesCharged(BigDecimal feeChargesCharged) {
        this.feeChargesCharged = feeChargesCharged;
    }

    public void setPenaltyCharges(BigDecimal penaltyCharges) {
        this.penaltyCharges = penaltyCharges;
    }

    public void setLoanCompoundingDetails(Set<LoanInterestRecalcualtionAdditionalDetails> loanCompoundingDetails) {
        if (loanCompoundingDetails != null) {
            loanCompoundingDetails.forEach(cd -> cd.setLoanRepaymentScheduleInstallment(this));
        }
        this.loanCompoundingDetails = loanCompoundingDetails;
    }

    public void setDownPayment(boolean downPayment) {
        isDownPayment = downPayment;
    }

    public boolean isOn(final LocalDate date, final LocalDate transactionDate) {
        return DateUtils.isEqual(transactionDate, date);
    }

    public boolean isOnOrBetween(final LocalDate transactionDate) {
        return isOn(fromDate, transactionDate) || isOn(dueDate, transactionDate)
                || (DateUtils.isBefore(transactionDate, dueDate) && DateUtils.isAfter(transactionDate, fromDate));
    }

}
