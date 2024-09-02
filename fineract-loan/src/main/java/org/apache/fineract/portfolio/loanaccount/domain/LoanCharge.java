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

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.domain.CustomChargeHonorarioMap;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeCalculationType;
import org.apache.fineract.portfolio.charge.domain.ChargePaymentMode;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.charge.exception.LoanChargeWithoutMandatoryFieldException;
import org.apache.fineract.portfolio.loanaccount.command.LoanChargeCommand;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargePaidDetail;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentChargeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "m_loan_charge", uniqueConstraints = { @UniqueConstraint(columnNames = { "external_id" }, name = "external_id") })
public class LoanCharge extends AbstractAuditableWithUTCDateTimeCustom {

    private static final Logger LOG = LoggerFactory.getLogger(LoanCharge.class);

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_id", referencedColumnName = "id", nullable = false)
    private Loan loan;

    @ManyToOne(optional = false)
    @JoinColumn(name = "charge_id", referencedColumnName = "id", nullable = false)
    private Charge charge;

    @Column(name = "charge_time_enum", nullable = false)
    private Integer chargeTime;

    @Column(name = "submitted_on_date", nullable = true)
    private LocalDate submittedOnDate;

    @Column(name = "due_for_collection_as_of_date")
    private LocalDate dueDate;

    @Column(name = "charge_calculation_enum")
    private Integer chargeCalculation;

    @Column(name = "charge_payment_mode_enum")
    private Integer chargePaymentMode;

    @Column(name = "calculation_percentage", scale = 6, precision = 19, nullable = true)
    private BigDecimal percentage;

    @Column(name = "calculation_on_amount", scale = 6, precision = 19, nullable = true)
    private BigDecimal amountPercentageAppliedTo;

    @Column(name = "charge_amount_or_percentage", scale = 6, precision = 19, nullable = false)
    private BigDecimal amountOrPercentage;

    @Column(name = "amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal amount;

    @Column(name = "amount_paid_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal amountPaid;

    @Column(name = "amount_waived_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal amountWaived;

    @Column(name = "amount_writtenoff_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal amountWrittenOff;

    @Column(name = "amount_outstanding_derived", scale = 6, precision = 19, nullable = false)
    private BigDecimal amountOutstanding;

    @Column(name = "is_penalty", nullable = false)
    private boolean penaltyCharge = false;

    @Column(name = "is_paid_derived", nullable = false)
    private boolean paid = false;

    @Column(name = "waived", nullable = false)
    private boolean waived = false;

    @Column(name = "min_cap", scale = 6, precision = 19, nullable = true)
    private BigDecimal minCap;

    @Column(name = "max_cap", scale = 6, precision = 19, nullable = true)
    private BigDecimal maxCap;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loancharge", orphanRemoval = true, fetch = FetchType.EAGER)
    private TreeSet<LoanInstallmentCharge> loanInstallmentCharge = new TreeSet<>();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "external_id")
    private ExternalId externalId;

    @OneToOne(mappedBy = "loanCharge", cascade = CascadeType.ALL, optional = true, orphanRemoval = true, fetch = FetchType.EAGER)
    private LoanOverdueInstallmentCharge overdueInstallmentCharge;

    @OneToOne(mappedBy = "loancharge", cascade = CascadeType.ALL, optional = true, orphanRemoval = true, fetch = FetchType.EAGER)
    private LoanTrancheDisbursementCharge loanTrancheDisbursementCharge;

    @OneToMany(mappedBy = "loanCharge", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<LoanChargePaidBy> loanChargePaidBySet = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "loan_charge_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Set<CustomChargeHonorarioMap> customChargeHonorarioMaps = new HashSet<>();

    @Column(name = "applicable_from_installment", nullable = true)
    private Integer applicableFromInstallment;

    @Column(name = "is_get_percentage_from_table", nullable = false)
    private boolean getPercentageAmountFromTable;

    @Column(name = "default_from_installment", nullable = true)
    private Integer defaultFromInstallment;

    @Column(name = "amount_paid_in_default_installment", nullable = true)
    private BigDecimal partialAmountPaidInFirstDefaultInstallment;

    // This attribute is used only to hold the current installment charge amount calculated and used
    // when repayment schedule is generated during loan creation. This amount is needed to show individual charge
    // amounts on the loan schedule screen.
    // Try not to use this variable anywhere else in the code
    @Transient
    private BigDecimal installmentChargeAmount = BigDecimal.ZERO;

    protected LoanCharge() {
        //
    }

    public LoanCharge(final Loan loan, final Charge chargeDefinition, final BigDecimal loanPrincipal, final BigDecimal amount,
            final ChargeTimeType chargeTime, final ChargeCalculationType chargeCalculation, final LocalDate dueDate,
            final ChargePaymentMode chargePaymentMode, final Integer numberOfRepayments, final BigDecimal loanCharge,
            final ExternalId externalId, boolean getPercentageAmountFromTable, Long numberOfPenaltyDays,
            Integer applicableFromInstallment) {

        this(loan, chargeDefinition, loanPrincipal, amount, chargeTime, chargeCalculation, dueDate, chargePaymentMode, numberOfRepayments,
                loanCharge, externalId, getPercentageAmountFromTable, numberOfPenaltyDays);

        this.setApplicableFromInstallment(applicableFromInstallment);
    }

    public LoanCharge(final Loan loan, final Charge chargeDefinition, final BigDecimal loanPrincipal, final BigDecimal amount,
            final ChargeTimeType chargeTime, final ChargeCalculationType chargeCalculation, final LocalDate dueDate,
            final ChargePaymentMode chargePaymentMode, final Integer numberOfRepayments, final BigDecimal loanCharge,
            final ExternalId externalId, boolean getPercentageAmountFromTable, Long numberOfPenaltyDays) {
        this.loan = loan;
        this.charge = chargeDefinition;
        this.submittedOnDate = DateUtils.getBusinessLocalDate();
        this.penaltyCharge = chargeDefinition.isPenalty();
        this.minCap = chargeDefinition.getMinCap();
        this.maxCap = chargeDefinition.getMaxCap();
        this.getPercentageAmountFromTable = getPercentageAmountFromTable;

        this.chargeTime = chargeDefinition.getChargeTimeType();
        if (chargeTime != null) {
            this.chargeTime = chargeTime.getValue();
        }

        if (ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.SPECIFIED_DUE_DATE)
                || ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.OVERDUE_INSTALLMENT)) {

            if (dueDate == null) {
                final String defaultUserMessage = "Loan charge is missing due date.";
                throw new LoanChargeWithoutMandatoryFieldException("loanCharge", "dueDate", defaultUserMessage, chargeDefinition.getId(),
                        chargeDefinition.getName());
            }

            this.dueDate = dueDate;
        } else {
            this.dueDate = null;
        }

        this.chargeCalculation = chargeDefinition.getChargeCalculation();
        if (chargeCalculation != null) {
            this.chargeCalculation = chargeCalculation.getValue();
        }

        BigDecimal chargeAmount = chargeDefinition.getAmount();
        if (amount != null) {
            chargeAmount = amount;
        }

        this.chargePaymentMode = chargeDefinition.getChargePaymentMode();
        if (chargePaymentMode != null) {
            this.chargePaymentMode = chargePaymentMode.getValue();
        }

        populateDerivedFields(loanPrincipal, chargeAmount, numberOfRepayments, loanCharge, numberOfPenaltyDays);
        this.paid = determineIfFullyPaid();
        this.externalId = externalId;
    }

    private void populateDerivedFields(final BigDecimal amountPercentageAppliedTo, final BigDecimal chargeAmount,
            Integer numberOfRepayments, BigDecimal loanCharge, Long numberOfPenaltyDays) {

        ChargeCalculationType chargeCalculationType = ChargeCalculationType.fromInt(this.chargeCalculation);

        if (ChargeCalculationType.INVALID.equals(chargeCalculationType)) {
            this.percentage = null;
            this.amount = null;
            this.amountPercentageAppliedTo = null;
            this.amountPaid = null;
            this.amountOutstanding = BigDecimal.ZERO;
            this.amountWaived = null;
            this.amountWrittenOff = null;
        } else {
            if (this.isPenaltyCharge()) {
                final RoundingMode roundingMode = RoundingMode.HALF_UP;
                final MathContext mc = MoneyHelper.getMathContext();
                // Get one day of interest
                this.percentage = chargeAmount.divide(BigDecimal.valueOf(365), mc).setScale(5, roundingMode)
                        .multiply(BigDecimal.valueOf(100L));

                this.amountPercentageAppliedTo = amountPercentageAppliedTo;
                if (loanCharge.compareTo(BigDecimal.ZERO) == 0) {
                    loanCharge = percentageOf(this.amountPercentageAppliedTo);
                    if (numberOfPenaltyDays != null) {
                        loanCharge = loanCharge.multiply(BigDecimal.valueOf(numberOfPenaltyDays));
                    }
                }
                this.amount = minimumAndMaximumCap(loanCharge);
                this.amountPaid = null;
                this.amountOutstanding = calculateOutstanding();
                this.amountWaived = null;
                this.amountWrittenOff = null;
            } else if (chargeCalculationType.isFlat()) {
                this.percentage = null;
                this.amountPercentageAppliedTo = null;
                this.amountPaid = null;
                if (isInstalmentFee()) {
                    if (numberOfRepayments == null) {
                        numberOfRepayments = this.loan.fetchNumberOfInstallmensAfterExceptions();
                    }
                    if (isCustomFlatDistributedCharge()) {
                        updateAmountOrPercentageForDistributedCharge(numberOfRepayments, chargeAmount);
                        this.amount = chargeAmount;
                    } else {
                        this.amount = chargeAmount.multiply(BigDecimal.valueOf(numberOfRepayments));
                    }
                } else {
                    this.amount = chargeAmount;
                }
                this.amountOutstanding = this.amount;
                this.amountWaived = null;
                this.amountWrittenOff = null;
            } else if (chargeCalculationType.isPercentageOfInstallmentPrincipal()
                    || chargeCalculationType.isPercentageOfInstallmentPrincipalAndInterest()
                    || chargeCalculationType.isPercentageOfInstallmentInterest() || chargeCalculationType.isPercentageOfDisbursement()
                    || chargeCalculationType.isPercentageOfInsurance() || chargeCalculationType.isPercentageOfHonorarios()
                    || chargeCalculationType.isPercentageOfAnotherCharge()) {
                this.percentage = chargeAmount;
                this.amountPercentageAppliedTo = amountPercentageAppliedTo;
                if (loanCharge.compareTo(BigDecimal.ZERO) == 0) {
                    loanCharge = percentageOf(this.amountPercentageAppliedTo);
                }
                this.amount = minimumAndMaximumCap(loanCharge);
                this.amountPaid = null;
                this.amountOutstanding = calculateOutstanding();
                this.amountWaived = null;
                this.amountWrittenOff = null;
                if (isCustomPercentageBasedDistributedCharge()) {
                    if (numberOfRepayments == null) {
                        numberOfRepayments = this.loan.fetchNumberOfInstallmensAfterExceptions();
                    }
                    updateAmountOrPercentageForDistributedCharge(numberOfRepayments, this.amount);
                }
            } else {
                LOG.error("TODO Implement for other charge calculation types");
            }
        }

        if (!isCustomFlatDistributedCharge() && !isCustomPercentageBasedDistributedCharge()) {
            this.amountOrPercentage = chargeAmount;
        }
        if (this.loan != null && isInstalmentFee()) {
            updateInstallmentCharges();
        }
    }

    private void updateAmountOrPercentageForDistributedCharge(Integer numberOfRepayments, BigDecimal amount) {
        if (this.loan != null) {
            numberOfRepayments = this.loan.fetchUnpaidNumberOfInstallments(null);
            if (this.applicableFromInstallment == null) {
                for (LoanRepaymentScheduleInstallment installment : loan.getRepaymentScheduleInstallments()) {
                    if (!installment.isObligationsMet()) {
                        this.applicableFromInstallment = installment.getInstallmentNumber();
                        break;
                    }
                }
            }
        }
        this.amountOrPercentage = amount.divide(BigDecimal.valueOf(numberOfRepayments), 2, RoundingMode.CEILING);
    }

    public void markAsFullyPaid() {
        this.amountPaid = this.amount;
        this.amountOutstanding = BigDecimal.ZERO;
        this.paid = true;
    }

    public boolean isFullyPaid() {
        return this.paid;
    }

    public void resetToOriginal(final MonetaryCurrency currency) {
        this.amountPaid = BigDecimal.ZERO;
        this.amountWaived = BigDecimal.ZERO;
        this.amountWrittenOff = BigDecimal.ZERO;
        this.amountOutstanding = calculateAmountOutstanding(currency);
        this.paid = false;
        this.waived = false;
        for (final LoanInstallmentCharge installmentCharge : this.loanInstallmentCharge) {
            installmentCharge.resetToOriginal(currency);
        }
    }

    public void resetPaidAmount(final MonetaryCurrency currency) {
        this.amountPaid = BigDecimal.ZERO;
        this.amountOutstanding = calculateAmountOutstanding(currency);
        this.paid = false;
        for (final LoanInstallmentCharge installmentCharge : this.loanInstallmentCharge) {
            installmentCharge.resetPaidAmount(currency);
        }
    }

    public void setOutstandingAmount(final BigDecimal amountOutstanding) {
        this.amountOutstanding = amountOutstanding;
    }

    public Money waive(final MonetaryCurrency currency, final Integer loanInstallmentNumber) {
        if (isInstalmentFee()) {
            final LoanInstallmentCharge chargePerInstallment = getInstallmentLoanCharge(loanInstallmentNumber);
            final Money amountWaived = chargePerInstallment.waive(currency);
            if (this.amountWaived == null) {
                this.amountWaived = BigDecimal.ZERO;
            }
            this.amountWaived = this.amountWaived.add(amountWaived.getAmount());
            this.amountOutstanding = this.amountOutstanding.subtract(amountWaived.getAmount());
            if (determineIfFullyPaid()) {
                this.paid = false;
                this.waived = true;
            }
            return amountWaived;
        }
        this.amountWaived = this.amountOutstanding;
        this.amountOutstanding = BigDecimal.ZERO;
        this.paid = false;
        this.waived = true;
        return getAmountWaived(currency);

    }

    public BigDecimal getAmountPercentageAppliedTo() {
        return this.amountPercentageAppliedTo;
    }

    private BigDecimal calculateAmountOutstanding(final MonetaryCurrency currency) {
        return getAmount(currency).minus(getAmountWaived(currency)).minus(getAmountPaid(currency)).minus(getAmountWrittenOff(currency))
                .getAmount();
    }

    public void update(final Loan loan) {
        this.loan = loan;
    }

    public void update(final BigDecimal amount, final LocalDate dueDate, final BigDecimal loanPrincipal, Integer numberOfRepayments,
            BigDecimal loanCharge) {
        if (dueDate != null) {
            this.dueDate = dueDate;
        }

        if (amount != null) {
            switch (ChargeCalculationType.fromInt(this.chargeCalculation)) {
                case INVALID:
                break;
                case FLAT_AMOUNT:
                case FLAT_SEGOVOLUNTARIO:
                case FLAT_SEGO:
                    if (isInstalmentFee()) {
                        if (numberOfRepayments == null) {
                            numberOfRepayments = this.loan.fetchNumberOfInstallmensAfterExceptions();
                        }
                        if (isCustomFlatDistributedCharge()) {
                            if (this.loan != null) {
                                numberOfRepayments = this.loan.fetchUnpaidNumberOfInstallments(this.getApplicableFromInstallment());
                            }
                            this.amountOrPercentage = amount.divide(BigDecimal.valueOf(numberOfRepayments), 2, RoundingMode.CEILING);
                            this.amount = amount;
                        } else {
                            this.amount = amount.multiply(BigDecimal.valueOf(numberOfRepayments));
                        }
                    } else {
                        this.amount = amount;
                    }
                break;
                case PERCENT_OF_AMOUNT:
                case PERCENT_OF_AMOUNT_AND_INTEREST:
                case PERCENT_OF_INTEREST:
                case PERCENT_OF_DISBURSEMENT_AMOUNT:
                case DISB_SEGO:
                case DISB_AVAL:
                    this.percentage = amount;
                    this.amountPercentageAppliedTo = loanPrincipal;
                    if (loanCharge.compareTo(BigDecimal.ZERO) == 0) {
                        loanCharge = percentageOf(this.amountPercentageAppliedTo);
                    }
                    this.amount = minimumAndMaximumCap(loanCharge);
                    if (isInstalmentFee() && isCustomPercentageBasedDistributedCharge()) {
                        if (numberOfRepayments == null) {
                            numberOfRepayments = this.loan.fetchNumberOfInstallmensAfterExceptions();
                        }
                        if (this.loan != null) {
                            numberOfRepayments = this.loan.fetchUnpaidNumberOfInstallments(this.getApplicableFromInstallment());
                        }
                        this.amountOrPercentage = this.amount.divide(BigDecimal.valueOf(numberOfRepayments), 2, RoundingMode.CEILING);
                    }
                break;
                case OPRIN_SEGO:
                    this.percentage = amount;
                    this.amountPercentageAppliedTo = loanPrincipal;
                break;
                default:
                    LOG.error("TODO Implement for other charge calculation types");
                break;
            }
            if (isCustomFlatDistributedCharge()) {
                this.amountOrPercentage = amount.divide(BigDecimal.valueOf(numberOfRepayments), 2, RoundingMode.CEILING);
            } else if (isCustomPercentageBasedDistributedCharge()) {
                this.amountOrPercentage = this.amount.divide(BigDecimal.valueOf(numberOfRepayments), 2, RoundingMode.CEILING);
            } else {
                this.amountOrPercentage = amount;
            }
            this.amountOutstanding = calculateOutstanding();
            if (this.loan != null && isInstalmentFee()) {
                updateInstallmentCharges();
            }
        }
    }

    public void update(final BigDecimal amount, final LocalDate dueDate, final Integer numberOfRepayments) {
        BigDecimal amountPercentageAppliedTo = BigDecimal.ZERO;
        if (this.loan != null) {
            switch (ChargeCalculationType.fromInt(this.chargeCalculation)) {
                case PERCENT_OF_AMOUNT:
                    // If charge type is specified due date and loan is multi disburment loan.
                    // Then we need to get as of this loan charge due date how much amount disbursed.
                    if (this.loan.isMultiDisburmentLoan() && this.isSpecifiedDueDate()) {
                        for (final LoanDisbursementDetails loanDisbursementDetails : this.loan.getDisbursementDetails()) {
                            if (!DateUtils.isAfter(loanDisbursementDetails.expectedDisbursementDate(), this.getDueDate())) {
                                amountPercentageAppliedTo = amountPercentageAppliedTo.add(loanDisbursementDetails.principal());
                            }
                        }
                    } else {
                        amountPercentageAppliedTo = this.loan.getPrincipal().getAmount();
                    }
                break;
                case PERCENT_OF_AMOUNT_AND_INTEREST:
                    amountPercentageAppliedTo = this.loan.getPrincipal().getAmount().add(this.loan.getTotalInterest());
                break;
                case PERCENT_OF_INTEREST:
                    amountPercentageAppliedTo = this.loan.getTotalInterest();
                break;
                case PERCENT_OF_DISBURSEMENT_AMOUNT:
                    LoanTrancheDisbursementCharge loanTrancheDisbursementCharge = this.loanTrancheDisbursementCharge;
                    amountPercentageAppliedTo = loanTrancheDisbursementCharge.getloanDisbursementDetails().principal();
                break;
                default:
                break;
            }
        }
        update(amount, dueDate, amountPercentageAppliedTo, numberOfRepayments, BigDecimal.ZERO);
    }

    public Map<String, Object> update(final JsonCommand command, final BigDecimal amount) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();

        final String dueDateParamName = "dueDate";
        if (command.isChangeInLocalDateParameterNamed(dueDateParamName, getDueLocalDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(dueDateParamName);
            actualChanges.put(dueDateParamName, valueAsInput);
            actualChanges.put("dateFormat", dateFormatAsInput);
            actualChanges.put("locale", localeAsInput);

            this.dueDate = command.localDateValueOfParameterNamed(dueDateParamName);
        }

        final String amountParamName = "amount";
        if (command.isChangeInBigDecimalParameterNamed(amountParamName, this.amount)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(amountParamName);
            BigDecimal loanCharge = null;
            actualChanges.put(amountParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            switch (ChargeCalculationType.fromInt(this.chargeCalculation)) {
                case INVALID:
                break;
                case FLAT_AMOUNT:
                    if (isInstalmentFee()) {
                        this.amount = newValue.multiply(BigDecimal.valueOf(this.loan.fetchNumberOfInstallmensAfterExceptions()));
                    } else {
                        this.amount = newValue;
                    }
                    this.amountOutstanding = calculateOutstanding();
                break;
                case PERCENT_OF_AMOUNT:
                case PERCENT_OF_AMOUNT_AND_INTEREST:
                case PERCENT_OF_INTEREST:
                case PERCENT_OF_DISBURSEMENT_AMOUNT:
                    this.percentage = newValue;
                    this.amountPercentageAppliedTo = amount;
                    loanCharge = BigDecimal.ZERO;
                    if (isInstalmentFee()) {
                        loanCharge = this.loan.calculatePerInstallmentChargeAmount(ChargeCalculationType.fromInt(this.chargeCalculation),
                                this.percentage, this.amountOrPercentage, this.getCharge().getParentChargeId());
                    }
                    if (loanCharge.compareTo(BigDecimal.ZERO) == 0) {
                        loanCharge = percentageOf(this.amountPercentageAppliedTo);
                    }
                    this.amount = minimumAndMaximumCap(loanCharge);
                    this.amountOutstanding = calculateOutstanding();
                break;
                default:
                    LOG.error("TODO Implement for other charge calculation types");
                break;
            }
            this.amountOrPercentage = newValue;
            if (isInstalmentFee()) {
                updateInstallmentCharges();
            }
        }
        return actualChanges;
    }

    public void resetAndUpdateInstallmentCharges() {
        updateInstallmentCharges();
    }

    private void updateInstallmentCharges() {
        final Collection<LoanInstallmentCharge> remove = new HashSet<>();
        final List<LoanInstallmentCharge> newChargeInstallments = this.loan.generateInstallmentLoanCharges(this);
        if (this.loanInstallmentCharge.isEmpty()) {
            this.loanInstallmentCharge.addAll(newChargeInstallments);
        } else {
            int index = 0;
            final List<LoanInstallmentCharge> oldChargeInstallments = new ArrayList<>();
            if (this.loanInstallmentCharge != null && !this.loanInstallmentCharge.isEmpty()) {
                oldChargeInstallments.addAll(this.loanInstallmentCharge);
            }
            Collections.sort(oldChargeInstallments);
            final LoanInstallmentCharge[] loanChargePerInstallmentArray = newChargeInstallments
                    .toArray(new LoanInstallmentCharge[newChargeInstallments.size()]);
            for (final LoanInstallmentCharge chargePerInstallment : oldChargeInstallments) {
                if (index == loanChargePerInstallmentArray.length) {
                    remove.add(chargePerInstallment);
                    chargePerInstallment.getInstallment().getInstallmentCharges().remove(chargePerInstallment);
                } else {
                    LoanInstallmentCharge newLoanInstallmentCharge = loanChargePerInstallmentArray[index++];
                    newLoanInstallmentCharge.getInstallment().getInstallmentCharges().remove(newLoanInstallmentCharge);
                    chargePerInstallment.copyFrom(newLoanInstallmentCharge);
                }
            }
            this.loanInstallmentCharge.removeAll(remove);
            while (index < loanChargePerInstallmentArray.length) {
                this.loanInstallmentCharge.add(loanChargePerInstallmentArray[index++]);
            }
        }

        Money amount = Money.zero(this.loan.getCurrency());
        // adjust decimal difference in amount that comes due to division of Charge amount with number of repayments
        if ((isCustomPercentageBasedDistributedCharge() || isCustomFlatDistributedCharge()) && this.charge.isInstallmentFee()) {
            int i = 1;
            for (LoanInstallmentCharge charge : this.loanInstallmentCharge) {
                if (i == this.loanInstallmentCharge.size()) {
                    amount = amount.plus(charge.getAmount());
                    if (amount.getAmount().compareTo(this.amount) != 0) {
                        if (amount.getAmount().compareTo(this.amount) < 0) {
                            BigDecimal difference = this.amount.subtract(amount.getAmount());
                            charge.setAmount(charge.getAmount().add(difference));
                        }
                        if (amount.getAmount().compareTo(this.amount) > 0) {
                            BigDecimal difference = amount.getAmount().subtract(this.amount);
                            charge.setAmount(charge.getAmount().subtract(difference));
                        }
                        charge.setAmountOutstanding(charge.getAmount());
                        amount = Money.of(amount.getCurrency(), this.amount);

                    }
                } else {
                    amount = amount.plus(charge.getAmount());
                }
                i++;
            }
        } else {
            for (LoanInstallmentCharge charge : this.loanInstallmentCharge) {
                amount = amount.plus(charge.getAmount());
            }
        }
        this.amount = amount.getAmount();
        this.amountOutstanding = calculateOutstanding();
    }

    public boolean isDueAtDisbursement() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.DISBURSEMENT)
                || ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.TRANCHE_DISBURSEMENT);
    }

    public boolean isSpecifiedDueDate() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.SPECIFIED_DUE_DATE);
    }

    public boolean isInstalmentFee() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.INSTALMENT_FEE);
    }

    public boolean isOverdueInstallmentCharge() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.OVERDUE_INSTALLMENT);
    }

    private static boolean isGreaterThanZero(final BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    public LoanChargeCommand toCommand() {
        return new LoanChargeCommand(getId(), this.charge.getId(), this.amount, this.chargeTime, this.chargeCalculation, getDueLocalDate());
    }

    public LocalDate getDueLocalDate() {
        return this.dueDate;
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public LocalDate getSubmittedOnDate() {
        return submittedOnDate;
    }

    private boolean determineIfFullyPaid() {
        if (this.amount == null) {
            return true;
        }
        return BigDecimal.ZERO.compareTo(calculateOutstanding()) == 0;
    }

    private BigDecimal calculateOutstanding() {
        if (this.amount == null) {
            return null;
        }
        BigDecimal amountPaidLocal = BigDecimal.ZERO;
        if (this.amountPaid != null) {
            amountPaidLocal = this.amountPaid;
        }

        BigDecimal amountWaivedLocal = BigDecimal.ZERO;
        if (this.amountWaived != null) {
            amountWaivedLocal = this.amountWaived;
        }

        BigDecimal amountWrittenOffLocal = BigDecimal.ZERO;
        if (this.amountWrittenOff != null) {
            amountWrittenOffLocal = this.amountWrittenOff;
        }

        final BigDecimal totalAccountedFor = amountPaidLocal.add(amountWaivedLocal).add(amountWrittenOffLocal);

        return this.amount.subtract(totalAccountedFor);
    }

    public BigDecimal percentageOf(final BigDecimal value) {
        return percentageOf(value, this.percentage);
    }

    public static BigDecimal percentageOf(final BigDecimal value, final BigDecimal percentage) {

        BigDecimal percentageOf = BigDecimal.ZERO;

        if (isGreaterThanZero(value)) {
            final MathContext mc = MoneyHelper.getMathContext();
            final BigDecimal multiplicand = percentage.divide(BigDecimal.valueOf(100L), mc);
            percentageOf = value.multiply(multiplicand, mc);
        }
        return percentageOf;
    }

    /**
     * @param percentageOf
     * @returns a minimum cap or maximum cap set on charges if the criteria fits else it returns the percentageOf if the
     *          amount is within min and max cap
     */
    private BigDecimal minimumAndMaximumCap(final BigDecimal percentageOf) {
        BigDecimal minMaxCap;
        if (this.minCap != null) {
            final int minimumCap = percentageOf.compareTo(this.minCap);
            if (minimumCap == -1) {
                minMaxCap = this.minCap;
                return minMaxCap;
            }
        }
        if (this.maxCap != null) {
            final int maximumCap = percentageOf.compareTo(this.maxCap);
            if (maximumCap == 1) {
                minMaxCap = this.maxCap;
                return minMaxCap;
            }
        }
        minMaxCap = percentageOf;
        // this will round the amount value
        if (this.loan != null && minMaxCap != null) {
            minMaxCap = Money.of(this.loan.getCurrency(), minMaxCap).getAmount();
        }
        return minMaxCap;
    }

    public BigDecimal amount() {
        return this.amount;
    }

    public BigDecimal amountOutstanding() {
        return this.amountOutstanding;
    }

    public Money getAmountOutstanding(final MonetaryCurrency currency) {
        return Money.of(currency, this.amountOutstanding);
    }

    public boolean hasNotLoanIdentifiedBy(final Long loanId) {
        return !hasLoanIdentifiedBy(loanId);
    }

    public boolean hasLoanIdentifiedBy(final Long loanId) {
        return this.loan.hasIdentifyOf(loanId);
    }

    public boolean isDueForCollectionFromAndUpToAndIncluding(final LocalDate fromNotInclusive, final LocalDate upToAndInclusive) {
        final LocalDate dueDate = getDueLocalDate();
        return occursOnDayFromExclusiveAndUpToAndIncluding(fromNotInclusive, upToAndInclusive, dueDate);
    }

    public boolean isDueForCollectionFromIncludingAndUpToAndIncluding(final LocalDate fromAndInclusive, final LocalDate upToAndInclusive) {
        final LocalDate dueDate = getDueLocalDate();
        return occursOnDayFromAndUpToAndIncluding(fromAndInclusive, upToAndInclusive, dueDate);
    }

    private boolean occursOnDayFromExclusiveAndUpToAndIncluding(final LocalDate fromNotInclusive, final LocalDate upToAndInclusive,
            final LocalDate target) {
        return DateUtils.isAfter(target, fromNotInclusive) && !DateUtils.isAfter(target, upToAndInclusive);
    }

    private boolean occursOnDayFromAndUpToAndIncluding(final LocalDate fromAndInclusive, final LocalDate upToAndInclusive,
            final LocalDate target) {
        return target != null && !DateUtils.isBefore(target, fromAndInclusive) && !DateUtils.isAfter(target, upToAndInclusive);
    }

    public boolean isFeeCharge() {
        return !this.penaltyCharge;
    }

    public boolean isPenaltyCharge() {
        return this.penaltyCharge;
    }

    public boolean isNotFullyPaid() {
        return !isPaid();
    }

    public boolean isChargePending() {
        return isNotFullyPaid() && !isWaived();
    }

    public boolean isPaid() {
        return this.paid;
    }

    public boolean isWaived() {
        return this.waived;
    }

    public BigDecimal getMinCap() {
        return this.minCap;
    }

    public BigDecimal getMaxCap() {
        return this.maxCap;
    }

    public Set<CustomChargeHonorarioMap> getCustomChargeHonorarioMaps() {
        return this.customChargeHonorarioMaps;
    }

    public boolean isPaidOrPartiallyPaid(final MonetaryCurrency currency) {

        final Money amountWaivedOrWrittenOff = getAmountWaived(currency).plus(getAmountWrittenOff(currency));
        return Money.of(currency, this.amountPaid).plus(amountWaivedOrWrittenOff).isGreaterThanZero();
    }

    public Money getAmount(final MonetaryCurrency currency) {
        return Money.of(currency, this.amount);
    }

    public Money getAmountPaid(final MonetaryCurrency currency) {
        return Money.of(currency, this.amountPaid);
    }

    public Money getAmountWaived(final MonetaryCurrency currency) {
        return Money.of(currency, this.amountWaived);
    }

    public Money getAmountWrittenOff(final MonetaryCurrency currency) {
        return Money.of(currency, this.amountWrittenOff);
    }

    public BigDecimal getInstallmentChargeAmount() {
        return installmentChargeAmount;
    }

    public void setInstallmentChargeAmount(BigDecimal installmentChargeAmount) {
        this.installmentChargeAmount = installmentChargeAmount;
    }

    public Integer getApplicableFromInstallment() {
        if (applicableFromInstallment == null) {
            return 1;
        }
        return applicableFromInstallment;
    }

    public void setApplicableFromInstallment(Integer applicableFromInstallment) {
        this.applicableFromInstallment = applicableFromInstallment;
    }

    public BigDecimal partialAmountPaidInFirstDefaultInstallment() {
        return partialAmountPaidInFirstDefaultInstallment;
    }

    public void setPartialAmountPaidInFirstDefaultInstallment(BigDecimal partialAmountPaidInFirstDefaultInstallment) {
        this.partialAmountPaidInFirstDefaultInstallment = partialAmountPaidInFirstDefaultInstallment;
    }

    public Integer defaultFromInstallment() {
        return defaultFromInstallment;
    }

    public void setDefaultFromInstallment(Integer defaultFromInstallment) {
        this.defaultFromInstallment = defaultFromInstallment;
    }

    /**
     * @param incrementBy
     *
     * @param installmentNumber
     *
     * @param feeAmount
     *            TODO
     *
     *
     * @return Actual amount paid on this charge
     */
    public Money updatePaidAmountBy(final Money incrementBy, final Integer installmentNumber, final Money feeAmount,
            final boolean isWriteOffTransaction) {
        Money processAmount;
        if (isInstalmentFee()) {
            if (installmentNumber == null) {
                final LoanInstallmentCharge unpaidInstallmentLoanCharge = getUnpaidInstallmentLoanCharge();
                if (unpaidInstallmentLoanCharge != null) {
                    processAmount = unpaidInstallmentLoanCharge.updatePaidAmountBy(incrementBy, feeAmount, isWriteOffTransaction);
                } else {
                    processAmount = incrementBy;
                }
            } else {
                final LoanInstallmentCharge installmentLoanCharge = getInstallmentLoanCharge(installmentNumber);
                if (installmentLoanCharge != null) {
                    processAmount = installmentLoanCharge.updatePaidAmountBy(incrementBy, feeAmount, isWriteOffTransaction);
                } else {
                    processAmount = incrementBy;
                }
            }
        } else {
            processAmount = incrementBy;
        }

        Money amountPaidToDate = Money.of(processAmount.getCurrency(), this.amountPaid);
        if (isWriteOffTransaction) {
            amountPaidToDate = Money.of(processAmount.getCurrency(), this.amountWrittenOff);
        }
        final Money amountOutstanding = Money.of(processAmount.getCurrency(), this.amountOutstanding);

        Money amountPaidOnThisCharge;
        if (processAmount.isGreaterThanOrEqualTo(amountOutstanding)) {
            amountPaidOnThisCharge = amountOutstanding;
            amountPaidToDate = amountPaidToDate.plus(amountOutstanding);
            if (isWriteOffTransaction) {
                this.amountWrittenOff = amountPaidToDate.getAmount();
            } else {
                this.amountPaid = amountPaidToDate.getAmount();
            }
            this.amountOutstanding = BigDecimal.ZERO;
            Money waivedAmount = getAmountWaived(processAmount.getCurrency());
            if (waivedAmount.isGreaterThanZero()) {
                this.waived = true;
            } else {
                this.paid = true;
            }

        } else {
            amountPaidOnThisCharge = processAmount;
            amountPaidToDate = amountPaidToDate.plus(processAmount);
            if (isWriteOffTransaction) {
                this.amountWrittenOff = amountPaidToDate.getAmount();
            } else {
                this.amountPaid = amountPaidToDate.getAmount();
            }
            this.amountOutstanding = calculateAmountOutstanding(incrementBy.getCurrency());
        }
        return amountPaidOnThisCharge;
    }

    public String name() {
        return this.charge.getName();
    }

    public String currencyCode() {
        return this.charge.getCurrencyCode();
    }

    public Charge getCharge() {
        return this.charge;
    }

    /*
     * @Override public boolean equals(final Object obj) { if (obj == null) { return false; } if (obj == this) { return
     * true; } if (obj.getClass() != getClass()) { return false; } final LoanCharge rhs = (LoanCharge) obj; return new
     * EqualsBuilder().appendSuper(super.equals(obj)) // .append(getId(), rhs.getId()) // .append(this.charge.getId(),
     * rhs.charge.getId()) // .append(this.amount, rhs.amount) // .append(getDueLocalDate(), rhs.getDueLocalDate()) //
     * .isEquals(); }
     *
     * @Override public int hashCode() { return 1;
     *
     * return new HashCodeBuilder(3, 5) // .append(getId()) // .append(this.charge.getId()) //
     * .append(this.amount).append(getDueLocalDate()) // .toHashCode();
     *
     * }
     */

    public ChargePaymentMode getChargePaymentMode() {
        return ChargePaymentMode.fromInt(this.chargePaymentMode);
    }

    public ChargeCalculationType getChargeCalculation() {
        return ChargeCalculationType.fromInt(this.chargeCalculation);
    }

    public BigDecimal getPercentage() {
        return this.percentage;
    }

    public void updateAmount(final BigDecimal amount) {
        this.amount = amount;
        calculateOutstanding();
    }

    public LoanInstallmentCharge getUnpaidInstallmentLoanCharge() {
        LoanInstallmentCharge unpaidChargePerInstallment = null;
        for (final LoanInstallmentCharge loanChargePerInstallment : this.loanInstallmentCharge) {
            if (loanChargePerInstallment.isPending() && (unpaidChargePerInstallment == null
                    || DateUtils.isAfter(unpaidChargePerInstallment.getRepaymentInstallment().getDueDate(),
                            loanChargePerInstallment.getRepaymentInstallment().getDueDate()))) {
                unpaidChargePerInstallment = loanChargePerInstallment;
            }
        }
        return unpaidChargePerInstallment;
    }

    public LoanInstallmentCharge getInstallmentLoanCharge(final LocalDate periodDueDate) {
        for (final LoanInstallmentCharge loanChargePerInstallment : this.loanInstallmentCharge) {
            if (DateUtils.isEqual(periodDueDate, loanChargePerInstallment.getRepaymentInstallment().getDueDate())) {
                return loanChargePerInstallment;
            }
        }
        return null;
    }

    public LoanInstallmentCharge getInstallmentLoanCharge(final Integer installmentNumber) {
        for (final LoanInstallmentCharge loanChargePerInstallment : this.loanInstallmentCharge) {
            if (installmentNumber.equals(loanChargePerInstallment.getRepaymentInstallment().getInstallmentNumber())) {
                return loanChargePerInstallment;
            }
        }
        return null;
    }

    public void setInstallmentLoanCharge(final LoanInstallmentCharge loanInstallmentCharge, final Integer installmentNumber) {
        LoanInstallmentCharge loanInstallmentChargeToBeRemoved = null;
        for (final LoanInstallmentCharge loanChargePerInstallment : this.loanInstallmentCharge) {
            if (installmentNumber.equals(loanChargePerInstallment.getRepaymentInstallment().getInstallmentNumber())) {
                loanInstallmentChargeToBeRemoved = loanChargePerInstallment;
                break;
            }
        }
        this.loanInstallmentCharge.remove(loanInstallmentChargeToBeRemoved);
        this.loanInstallmentCharge.add(loanInstallmentCharge);
    }

    public void clearLoanInstallmentCharges() {
        this.loanInstallmentCharge.clear();
    }

    public void addLoanInstallmentCharges(final Collection<LoanInstallmentCharge> installmentCharges) {
        this.loanInstallmentCharge.addAll(installmentCharges);
    }

    public boolean hasNoLoanInstallmentCharges() {
        return this.loanInstallmentCharge.isEmpty();
    }

    public Set<LoanInstallmentCharge> installmentCharges() {
        return this.loanInstallmentCharge;
    }

    public List<LoanChargePaidDetail> fetchRepaymentInstallment(final MonetaryCurrency currency) {
        List<LoanChargePaidDetail> chargePaidDetails = new ArrayList<>();
        for (final LoanInstallmentCharge loanChargePerInstallment : this.loanInstallmentCharge) {
            if (!loanChargePerInstallment.isChargeAmountpaid(currency)
                    && loanChargePerInstallment.getAmountThroughChargePayment(currency).isGreaterThanZero()) {
                LoanChargePaidDetail chargePaidDetail = new LoanChargePaidDetail(
                        loanChargePerInstallment.getAmountThroughChargePayment(currency),
                        loanChargePerInstallment.getRepaymentInstallment(), isFeeCharge());
                chargePaidDetails.add(chargePaidDetail);
            }
        }
        return chargePaidDetails;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            this.overdueInstallmentCharge = null;
            this.loanTrancheDisbursementCharge = null;
            this.clearLoanInstallmentCharges();
        }
    }

    public BigDecimal amountOrPercentage() {
        return this.amountOrPercentage;
    }

    public BigDecimal chargeAmount() {
        BigDecimal totalChargeAmount = this.amountOutstanding;
        if (this.amountPaid != null) {
            totalChargeAmount = totalChargeAmount.add(this.amountPaid);
        }
        if (this.amountWaived != null) {
            totalChargeAmount = totalChargeAmount.add(this.amountWaived);
        }
        if (this.amountWrittenOff != null) {
            totalChargeAmount = totalChargeAmount.add(this.amountWrittenOff);
        }
        return totalChargeAmount;
    }

    public void updateOverdueInstallmentCharge(LoanOverdueInstallmentCharge overdueInstallmentCharge) {
        this.overdueInstallmentCharge = overdueInstallmentCharge;
    }

    public void updateLoanTrancheDisbursementCharge(final LoanTrancheDisbursementCharge loanTrancheDisbursementCharge) {
        this.loanTrancheDisbursementCharge = loanTrancheDisbursementCharge;
    }

    public void updateWaivedAmount(MonetaryCurrency currency) {
        if (isInstalmentFee()) {
            this.amountWaived = BigDecimal.ZERO;
            for (final LoanInstallmentCharge chargePerInstallment : this.loanInstallmentCharge) {
                final Money amountWaived = chargePerInstallment.updateWaivedAndAmountPaidThroughChargePaymentAmount(currency);
                this.amountWaived = this.amountWaived.add(amountWaived.getAmount());
                this.amountOutstanding = this.amountOutstanding.subtract(amountWaived.getAmount());
                if (determineIfFullyPaid() && Money.of(currency, this.amountWaived).isGreaterThanZero()) {
                    this.paid = false;
                    this.waived = true;
                }
            }
            return;
        }

        Money waivedAmount = Money.of(currency, this.amountWaived);
        if (waivedAmount.isGreaterThanZero()) {
            if (waivedAmount.isGreaterThan(this.getAmount(currency))) {
                this.amountWaived = this.getAmount(currency).getAmount();
                this.amountOutstanding = BigDecimal.ZERO;
                this.paid = false;
                this.waived = true;
            } else if (waivedAmount.isLessThan(this.getAmount(currency))) {
                this.paid = false;
                this.waived = false;
            }
        }

    }

    public LoanOverdueInstallmentCharge getOverdueInstallmentCharge() {
        return this.overdueInstallmentCharge;
    }

    public LoanTrancheDisbursementCharge getTrancheDisbursementCharge() {
        return this.loanTrancheDisbursementCharge;
    }

    public Money undoPaidOrPartiallyAmountBy(final Money incrementBy, final Integer installmentNumber, final Money feeAmount) {
        Money processAmount;
        if (isInstalmentFee()) {
            if (installmentNumber == null) {
                processAmount = getLastPaidOrPartiallyPaidInstallmentLoanCharge(incrementBy.getCurrency()).undoPaidAmountBy(incrementBy,
                        feeAmount);
            } else {
                processAmount = getInstallmentLoanCharge(installmentNumber).undoPaidAmountBy(incrementBy, feeAmount);
            }
        } else {
            processAmount = incrementBy;
        }
        Money amountPaidToDate = Money.of(processAmount.getCurrency(), this.amountPaid);

        Money amountDeductedOnThisCharge;
        if (processAmount.isGreaterThanOrEqualTo(amountPaidToDate)) {
            amountDeductedOnThisCharge = amountPaidToDate;
            amountPaidToDate = Money.zero(processAmount.getCurrency());
            this.amountPaid = amountPaidToDate.getAmount();
            this.amountOutstanding = this.amount;
            this.paid = false;

        } else {
            amountDeductedOnThisCharge = processAmount;
            amountPaidToDate = amountPaidToDate.minus(processAmount);
            this.amountPaid = amountPaidToDate.getAmount();
            this.amountOutstanding = calculateAmountOutstanding(incrementBy.getCurrency());
        }
        return amountDeductedOnThisCharge;
    }

    public LoanInstallmentCharge getLastPaidOrPartiallyPaidInstallmentLoanCharge(MonetaryCurrency currency) {
        LoanInstallmentCharge paidChargePerInstallment = null;
        for (final LoanInstallmentCharge loanChargePerInstallment : this.loanInstallmentCharge) {
            Money outstanding = Money.of(currency, loanChargePerInstallment.getAmountOutstanding());
            final boolean partiallyPaid = outstanding.isGreaterThanZero()
                    && outstanding.isLessThan(loanChargePerInstallment.getAmount(currency));
            if ((partiallyPaid || loanChargePerInstallment.isPaid()) && (paidChargePerInstallment == null
                    || DateUtils.isBefore(paidChargePerInstallment.getRepaymentInstallment().getDueDate(),
                            loanChargePerInstallment.getRepaymentInstallment().getDueDate()))) {
                paidChargePerInstallment = loanChargePerInstallment;
            }
        }
        return paidChargePerInstallment;
    }

    public Set<LoanChargePaidBy> getLoanChargePaidBySet() {
        return this.loanChargePaidBySet;
    }

    public Loan getLoan() {
        return this.loan;
    }

    public boolean isDisbursementCharge() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.DISBURSEMENT);
    }

    public boolean isTrancheDisbursementCharge() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.TRANCHE_DISBURSEMENT);
    }

    public boolean isDueDateCharge() {
        return this.dueDate != null;
    }

    public void setAmountWaived(final BigDecimal amountWaived) {
        this.amountWaived = amountWaived;
    }

    public void undoWaived() {
        this.waived = false;
    }

    public ExternalId getExternalId() {
        return externalId;
    }

    public ChargeTimeType getChargeTimeType() {
        return ChargeTimeType.fromInt(this.chargeTime);
    }

    /**
     * Return the effective due date of the loan charge. For installment fee we are using the earliest not fully paid
     * installment due date
     *
     * @return LocalDate
     */
    public LocalDate getEffectiveDueDate() {
        LocalDate dueDate;
        if (Objects.requireNonNull(getChargeTimeType()) == ChargeTimeType.INSTALMENT_FEE) {
            LoanInstallmentCharge firstUnpaidInstallment = getUnpaidInstallmentLoanCharge();
            dueDate = firstUnpaidInstallment != null ? firstUnpaidInstallment.getInstallment().getDueDate() : null;
        } else {
            dueDate = getDueLocalDate();
        }
        return dueDate;
    }

    public LoanChargeData toData() {
        EnumOptionData chargeTimeTypeData = new EnumOptionData((long) getChargeTimeType().ordinal(), getChargeTimeType().getCode(),
                String.valueOf(getChargeTimeType().getValue()));
        EnumOptionData chargeCalculationTypeData = new EnumOptionData((long) getChargeCalculation().ordinal(),
                getChargeCalculation().getCode(), String.valueOf(getChargeCalculation().getValue()));
        EnumOptionData chargePaymentModeData = new EnumOptionData((long) getChargePaymentMode().ordinal(), getChargePaymentMode().getCode(),
                String.valueOf(getChargePaymentMode().getValue()));
        Set<LoanInstallmentChargeData> loanInstallmentChargeDataSet = installmentCharges().stream().map(LoanInstallmentCharge::toData)
                .collect(Collectors.toSet());

        return LoanChargeData.builder().id(getId()).chargeId(getCharge().getId()).name(getCharge().getName())
                .currency(getCharge().toData().getCurrency()).amount(amount).amountPaid(amountPaid).amountWaived(amountWaived)
                .amountWrittenOff(amountWrittenOff).amountOutstanding(amountOutstanding).chargeTimeType(chargeTimeTypeData)
                .submittedOnDate(submittedOnDate).dueDate(dueDate).chargeCalculationType(chargeCalculationTypeData).percentage(percentage)
                .amountPercentageAppliedTo(amountPercentageAppliedTo).amountOrPercentage(amountOrPercentage).penalty(penaltyCharge)
                .chargePaymentMode(chargePaymentModeData).paid(paid).waived(waived).loanId(loan.getId()).minCap(minCap).maxCap(maxCap)
                .installmentChargeData(loanInstallmentChargeDataSet).externalId(externalId).build();
    }

    public boolean isCustomFeeChargeApplicableOnInstallment(Integer installmentNumber) {
        boolean isApplicable = false;
        if (this.getChargeCalculation().isFlatHono() && !this.getCustomChargeHonorarioMaps().isEmpty()) {
            for (CustomChargeHonorarioMap customCharge : this.getCustomChargeHonorarioMaps()) {
                if (customCharge.getLoanInstallmentNr().equals(installmentNumber)) {
                    isApplicable = true;
                    break;
                }
            }
        } else if (this.isCustomFlatDistributedCharge() || this.isCustomPercentageBasedDistributedCharge()
                || this.isCustomPercentageBasedOfAnotherCharge() || isCustomPercentageOfOutstandingPrincipalCharge()) {
            isApplicable = true;
        }
        return isApplicable;
    }

    public boolean isCustomFlatVoluntaryInsurenceCharge() {
        return this.getChargeCalculation().getCode().equals(ChargeCalculationType.FLAT_SEGOVOLUNTARIO.getCode());
    }

    public BigDecimal calculateCustomFeeChargeToInstallment(Integer installmentNumber, Money principalDisbursed,
            Integer numberOfInstallments, Money outstandingBalance) {
        BigDecimal customAmout = BigDecimal.ZERO;
        if (this.getChargeCalculation().isFlatHono() && !this.getCustomChargeHonorarioMaps().isEmpty()) {
            for (CustomChargeHonorarioMap customCharge : this.getCustomChargeHonorarioMaps()) {
                if (customCharge.getLoanInstallmentNr().equals(installmentNumber)) {
                    customAmout = customAmout.add(customCharge.getFeeTotalAmount());
                    break;
                }
            }
        } else if (this.isCustomFlatDistributedCharge()) {
            BigDecimal amountToAdd = this.amountOrPercentage;
            if (!this.installmentCharges().isEmpty()) {
                LoanInstallmentCharge installmentCharge = this.getInstallmentLoanCharge(installmentNumber);
                if (installmentCharge != null) {
                    amountToAdd = installmentCharge.getAmount();
                }
            }
            customAmout = customAmout.add(amountToAdd);
        } else if (this.isCustomPercentageBasedDistributedCharge()) {
            BigDecimal chargeAmount = BigDecimal.ZERO;
            if (this.installmentCharges().isEmpty()) {
                chargeAmount = this.amountOrPercentage;
            } else {
                final LoanInstallmentCharge installmentCharge = this.getInstallmentLoanCharge(installmentNumber);
                if (installmentCharge != null) {
                    chargeAmount = installmentCharge.getAmount();
                }
            }
            customAmout = customAmout.add(chargeAmount);
        } else if (this.isCustomPercentageBasedOfAnotherCharge()) {
            customAmout = customAmout.add(this.installmentCharges().isEmpty() ? this.amountOrPercentage
                    : this.getInstallmentLoanCharge(installmentNumber).getAmount());
        } else if (this.isCustomPercentageOfOutstandingPrincipalCharge()) {
            if (this.installmentCharges().isEmpty()) {
                BigDecimal installmentCount = BigDecimal.valueOf(numberOfInstallments);
                BigDecimal computedAmount = LoanCharge.percentageOf(outstandingBalance.getAmount(), this.percentage);
                BigDecimal finalAmount = computedAmount.divide(installmentCount, 0, RoundingMode.HALF_UP);
                customAmout = customAmout.add(finalAmount);
            } else {
                final LoanInstallmentCharge installmentLoanCharge = this.getInstallmentLoanCharge(installmentNumber);
                if (installmentLoanCharge != null) {
                    customAmout = customAmout.add(installmentLoanCharge.getAmount());
                }
            }
        }
        return customAmout;
    }

    public BigDecimal calculateCustomFeeChargeToInstallment(Integer installmentNumber) {
        return calculateCustomFeeChargeToInstallment(installmentNumber, null, null, null);
    }

    public void updateCustomFeeCharge() {
        updateInstallmentCharges();
    }

    public boolean isFlatHono() {
        return getChargeCalculation().isFlatHono();
    }

    public boolean isCustomFlatDistributedCharge() {
        // Charge is distributed among the installments
        return getChargeCalculation().isFlatMandatoryInsurance();
    }

    public boolean isCustomPercentageBasedDistributedCharge() {
        // Charge is distributed among the installments
        return getChargeCalculation().isCustomPercentageBasedDistributedCharge();
    }

    public boolean isAvalCharge() {
        // Charge is distributed among the installments
        return getChargeCalculation().isPercentageOfAval();
    }

    public boolean isMandatoryInsurance() {
        // Charge is distributed among the installments
        return getChargeCalculation().isMandatoryInsuranceCharge();
    }

    public boolean isVoluntaryInsurance() {
        // Charge is distributed among the installments
        return getChargeCalculation().isVoluntaryInsurance();
    }

    public boolean isCustomPercentageBasedOfAnotherCharge() {
        // Charge is distributed among the installments
        return getChargeCalculation().isPercentageOfAnotherCharge();
    }

    public boolean isCustomPercentageOfOutstandingPrincipalCharge() {
        // Charge is distributed among the installments
        return getChargeCalculation().isCustomPercentageOfOutstandingPrincipalCharge();
    }

    public BigDecimal getLastInstallmentRoundOffAmountForVoluntaryInsurance(Integer lastInstallmentNumber) {
        Integer numberOfRepayments = 1;
        if (lastInstallmentNumber > 1) {
            if (this.applicableFromInstallment == null) {
                numberOfRepayments = lastInstallmentNumber;
            } else {
                numberOfRepayments = lastInstallmentNumber - this.applicableFromInstallment + 1;
            }
        }
        BigDecimal installmentAmount = this.amount.divide(BigDecimal.valueOf(numberOfRepayments), 2, RoundingMode.CEILING);
        BigDecimal amt = BigDecimal.ZERO;
        for (int i = 1; i <= numberOfRepayments; i++) {
            amt = amt.add(installmentAmount);
        }

        if (amt.compareTo(this.amount) > 0) {
            BigDecimal difference = amt.subtract(this.amount);
            installmentAmount = installmentAmount.subtract(difference);
        }
        return installmentAmount;
    }

    public BigDecimal calculateParentChargeAmountForInstallment(Set<LoanCharge> loanCharges, Integer installmentNumber,
            Money principalDisbursed, Integer numberOfInstallments, Money outstandingBalance) {
        // First Identify the parent loan charge and then Calculate the installment charge for the parent Charge. This
        // has already been calculated before when the parent charge was processed
        // But we no longer have that value available when the percentage based charge is processed
        BigDecimal parentChargeAmount = BigDecimal.ZERO;
        for (LoanCharge parentCharge : loanCharges) {
            if (parentCharge.getCharge().getId().equals(this.getCharge().getParentChargeId())) {
                parentChargeAmount = parentCharge.calculateCustomFeeChargeToInstallment(installmentNumber, principalDisbursed,
                        numberOfInstallments, outstandingBalance);
                break;
            }
        }
        return parentChargeAmount;
    }

    public void adjustAmountWrittenOff(final Money amountToBeWrittenOff) {
        final Money amountOutstanding = Money.of(amountToBeWrittenOff.getCurrency(), this.calculateOutstanding());
        if (amountOutstanding.isGreaterThanOrEqualTo(amountToBeWrittenOff)) {
            this.amountWrittenOff = this.getAmountWrittenOff(amountToBeWrittenOff.getCurrency()).plus(amountToBeWrittenOff).getAmount();
            this.amountOutstanding = calculateOutstanding();
            this.paid = this.determineIfFullyPaid();
        }
    }

    public void adjustChargeAmount(final Money adjustedAmount) {
        final Money currentAmount = this.getAmount(adjustedAmount.getCurrency());
        if (currentAmount.isGreaterThanOrEqualTo(adjustedAmount)) {
            this.amount = currentAmount.minus(adjustedAmount).getAmount();
            this.amountOutstanding = calculateOutstanding();
            this.paid = this.determineIfFullyPaid();
        }
    }

    public void updateAmountOutstanding() {
        this.amountOutstanding = calculateOutstanding();
    }
}
