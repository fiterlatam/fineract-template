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
package org.apache.fineract.portfolio.charge.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.charge.api.ChargesApiConstants;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.charge.data.ChargeInsuranceDetailData;
import org.apache.fineract.portfolio.charge.enumerator.ChargeCalculationTypeBaseItemsEnum;
import org.apache.fineract.portfolio.charge.exception.ChargeCannotBeCreatedException;
import org.apache.fineract.portfolio.charge.exception.ChargeDueAtDisbursementCannotBePenaltyException;
import org.apache.fineract.portfolio.charge.exception.ChargeMustBePenaltyException;
import org.apache.fineract.portfolio.charge.exception.ChargeParameterUpdateNotSupportedException;
import org.apache.fineract.portfolio.charge.service.ChargeEnumerations;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.interestrates.domain.InterestRate;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentType;
import org.apache.fineract.portfolio.tax.data.TaxGroupData;
import org.apache.fineract.portfolio.tax.domain.TaxGroup;

@Entity
@Table(name = "m_charge", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }, name = "name") })
public class Charge extends AbstractPersistableCustom {

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "charge_applies_to_enum", nullable = false)
    private Integer chargeAppliesTo;

    @Column(name = "charge_time_enum", nullable = false)
    private Integer chargeTimeType;

    @Column(name = "charge_calculation_enum")
    private Integer chargeCalculation;

    @Column(name = "charge_payment_mode_enum", nullable = true)
    private Integer chargePaymentMode;

    @Column(name = "fee_on_day", nullable = true)
    private Integer feeOnDay;

    @Column(name = "fee_interval", nullable = true)
    private Integer feeInterval;

    @Column(name = "fee_on_month", nullable = true)
    private Integer feeOnMonth;

    @Column(name = "is_penalty", nullable = false)
    private boolean penalty;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "min_cap", scale = 6, precision = 19, nullable = true)
    private BigDecimal minCap;

    @Column(name = "max_cap", scale = 6, precision = 19, nullable = true)
    private BigDecimal maxCap;

    @Column(name = "fee_frequency", nullable = true)
    private Integer feeFrequency;

    @Column(name = "is_free_withdrawal", nullable = false)
    private boolean enableFreeWithdrawal;

    @Column(name = "free_withdrawal_charge_frequency", nullable = true)
    private Integer freeWithdrawalFrequency;

    @Column(name = "restart_frequency", nullable = true)
    private Integer restartFrequency;

    @Column(name = "restart_frequency_enum", nullable = true)
    private Integer restartFrequencyEnum;

    @Column(name = "is_payment_type", nullable = false)
    private boolean enablePaymentType;

    @Column(name = "grace_on_charge_period_enum", nullable = false)
    private Short graceOnChargePeriodEnum = 1;

    @Column(name = "grace_on_charge_period_amount", nullable = false)
    private Long graceOnChargePeriodAmount = 0L;

    @Column(name = "parent_charge_id", nullable = false)
    private Long parentChargeId = 0L;

    @ManyToOne
    @JoinColumn(name = "payment_type_id", nullable = false)
    private PaymentType paymentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_or_liability_account_id")
    private GLAccount account;

    @ManyToOne
    @JoinColumn(name = "tax_group_id")
    private TaxGroup taxGroup;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_rate_id")
    private InterestRate interestRate;

    @Column(name = "is_get_percentage_from_table", nullable = false)
    private boolean getPercentageFromTable;

    @Embedded
    private ChargeInsuranceDetail chargeInsuranceDetail;

    public static Charge fromJson(final JsonCommand command, final GLAccount account, final TaxGroup taxGroup,
            final PaymentType paymentType) {

        final String name = command.stringValueOfParameterNamed("name");
        final BigDecimal amount = command.bigDecimalValueOfParameterNamed("amount");
        final String currencyCode = command.stringValueOfParameterNamed("currencyCode");

        final ChargeAppliesTo chargeAppliesTo = ChargeAppliesTo.fromInt(command.integerValueOfParameterNamed("chargeAppliesTo"));
        final ChargeTimeType chargeTimeType = ChargeTimeType.fromInt(command.integerValueOfParameterNamed("chargeTimeType"));
        final ChargeCalculationType chargeCalculationType = ChargeCalculationType
                .fromInt(command.integerValueOfParameterNamed("chargeCalculationType"));
        final Integer chargePaymentMode = command.integerValueOfParameterNamed("chargePaymentMode");

        final ChargePaymentMode paymentMode = chargePaymentMode == null ? null : ChargePaymentMode.fromInt(chargePaymentMode);

        final boolean penalty = command.booleanPrimitiveValueOfParameterNamed("penalty");
        final boolean active = command.booleanPrimitiveValueOfParameterNamed("active");
        final MonthDay feeOnMonthDay = command.extractMonthDayNamed("feeOnMonthDay");
        final Integer feeInterval = command.integerValueOfParameterNamed("feeInterval");
        final BigDecimal minCap = command.bigDecimalValueOfParameterNamed("minCap");
        final BigDecimal maxCap = command.bigDecimalValueOfParameterNamed("maxCap");
        final Integer feeFrequency = command.integerValueOfParameterNamed("feeFrequency");

        boolean enableFreeWithdrawalCharge = false;
        enableFreeWithdrawalCharge = command.booleanPrimitiveValueOfParameterNamed("enableFreeWithdrawalCharge");

        boolean enablePaymentType = false;
        enablePaymentType = command.booleanPrimitiveValueOfParameterNamed("enablePaymentType");

        Integer freeWithdrawalFrequency = null;
        Integer restartCountFrequency = null;
        PeriodFrequencyType countFrequencyType = null;

        if (enableFreeWithdrawalCharge) {
            freeWithdrawalFrequency = command.integerValueOfParameterNamed("freeWithdrawalFrequency");
            restartCountFrequency = command.integerValueOfParameterNamed("restartCountFrequency");

            countFrequencyType = PeriodFrequencyType.fromInt(command.integerValueOfParameterNamed("countFrequencyType"));
        }

        final Long graceOnChargePeriodAmount = command.longValueOfParameterNamed("graceOnChargePeriodAmount");

        Long parentChargeId = null;
        if (command.hasParameter("parentChargeId")) {
            parentChargeId = command.longValueOfParameterNamed("parentChargeId");
        }
        // Voluntary Insurance details
        ChargeInsuranceDetail chargeInsuranceDetail = null;
        if (chargeCalculationType.isInsurance()) {
            if (chargeCalculationType.isVoluntaryInsurance()) {
                validateVoluntaryInsuranceCondition(chargeCalculationType);
            }
            final String insuranceName = command.stringValueOfParameterNamed("insuranceName");
            final ChargeInsuranceType insuranceChargeAs = ChargeInsuranceType
                    .fromInt(command.integerValueOfParameterNamed("insuranceChargedAs"));
            final String insuranceCompany = command.stringValueOfParameterNamed("insuranceCompany");
            final String insurerName = command.stringValueOfParameterNamed("insurerName");
            final Long insuranceCode = command.longValueOfParameterNamed("insuranceCode");
            final String insurancePlan = command.stringValueOfParameterNamed("insurancePlan");
            final BigDecimal baseValue = command.bigDecimalValueOfParameterNamed("baseValue");
            final BigDecimal vatValue = command.bigDecimalValueOfParameterNamed("vatValue");
            final BigDecimal totalValue = command.bigDecimalValueOfParameterNamed("totalValue");
            final Integer daysInArrears = command.integerValueOfParameterNamedDefaultToNullIfZero("daysInArrears");
            Long deadline = null;
            if (insuranceChargeAs.isCompra()) {
                deadline = command.longValueOfParameterNamed("deadline");
            }
            chargeInsuranceDetail = new ChargeInsuranceDetail(insuranceName, insuranceChargeAs, insuranceCompany, insurerName,
                    insuranceCode, insurancePlan, baseValue, vatValue, totalValue, deadline, daysInArrears);
        }
        boolean getPercentageFromTable = command
                .booleanPrimitiveValueOfParameterNamed(ChargesApiConstants.GET_INTEREST_PERCENTAGE_FROM_TABLE);
        return new Charge(name, amount, currencyCode, chargeAppliesTo, chargeTimeType, chargeCalculationType, penalty, active, paymentMode,
                feeOnMonthDay, feeInterval, minCap, maxCap, feeFrequency, enableFreeWithdrawalCharge, freeWithdrawalFrequency,
                restartCountFrequency, countFrequencyType, account, taxGroup, enablePaymentType, paymentType, graceOnChargePeriodAmount,
                parentChargeId, chargeInsuranceDetail, getPercentageFromTable);
    }

    protected Charge() {}

    private Charge(final String name, final BigDecimal amount, final String currencyCode, final ChargeAppliesTo chargeAppliesTo,
            final ChargeTimeType chargeTime, final ChargeCalculationType chargeCalculationType, final boolean penalty, final boolean active,
            final ChargePaymentMode paymentMode, final MonthDay feeOnMonthDay, final Integer feeInterval, final BigDecimal minCap,
            final BigDecimal maxCap, final Integer feeFrequency, final boolean enableFreeWithdrawalCharge,
            final Integer freeWithdrawalFrequency, final Integer restartFrequency, final PeriodFrequencyType restartFrequencyEnum,
            final GLAccount account, final TaxGroup taxGroup, final boolean enablePaymentType, final PaymentType paymentType,
            final Long graceOnChargePeriodAmount, Long parentChargeId, ChargeInsuranceDetail chargeInsuranceDetail,
            boolean getPercentageFromTable) {
        this.name = name;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.chargeAppliesTo = chargeAppliesTo.getValue();
        this.chargeTimeType = chargeTime.getValue();
        this.chargeCalculation = chargeCalculationType.getValue();
        this.penalty = penalty;
        this.active = active;
        this.account = account;
        this.taxGroup = taxGroup;
        this.chargePaymentMode = paymentMode == null ? null : paymentMode.getValue();
        this.chargeInsuranceDetail = chargeInsuranceDetail;

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("charges");

        if (isMonthlyFee() || isAnnualFee()) {
            this.feeOnMonth = feeOnMonthDay.getMonthValue();
            this.feeOnDay = feeOnMonthDay.getDayOfMonth();
        }
        this.feeInterval = feeInterval;
        this.feeFrequency = feeFrequency;

        if (isSavingsCharge()) {
            // TODO vishwas, this validation seems unnecessary as identical
            // validation is performed in the write service
            if (!isAllowedSavingsChargeTime()) {
                baseDataValidator.reset().parameter("chargeTimeType").value(this.chargeTimeType)
                        .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.time.for.savings");
            }
            // TODO vishwas, this validation seems unnecessary as identical
            // validation is performed in the writeservice
            if (!isAllowedSavingsChargeCalculationType()) {
                baseDataValidator.reset().parameter("chargeCalculationType").value(this.chargeCalculation)
                        .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.calculation.type.for.savings");
            }

            if (!(ChargeTimeType.fromInt(getChargeTimeType()).isWithdrawalFee()
                    || ChargeTimeType.fromInt(getChargeTimeType()).isSavingsNoActivityFee())
                    && ChargeCalculationType.fromInt(getChargeCalculation()).isPercentageOfInstallmentPrincipal()) {
                baseDataValidator.reset().parameter("chargeCalculationType").value(this.chargeCalculation)
                        .failWithCodeNoParameterAddedToErrorCode(
                                "savings.charge.calculation.type.percentage.allowed.only.for.withdrawal.or.NoActivity");
            }

            if (enableFreeWithdrawalCharge) {
                this.enableFreeWithdrawal = true;
                this.freeWithdrawalFrequency = freeWithdrawalFrequency;
                this.restartFrequency = restartFrequency;
                this.restartFrequencyEnum = restartFrequencyEnum.getValue();
            }

            if (enablePaymentType) {
                if (paymentType != null) {

                    this.enablePaymentType = true;
                    this.paymentType = paymentType;
                }
            }

        } else if (isLoanCharge()) {

            if (penalty && (chargeTime.isTimeOfDisbursement() || chargeTime.isTrancheDisbursement())) {
                throw new ChargeDueAtDisbursementCannotBePenaltyException(name);
            }
            if (!penalty && chargeTime.isOverdueInstallment()) {
                throw new ChargeMustBePenaltyException(name);
            }
            // TODO vishwas, this validation seems unnecessary as identical
            // validation is performed in the write service
            if (!isAllowedLoanChargeTime()) {
                baseDataValidator.reset().parameter("chargeTimeType").value(this.chargeTimeType)
                        .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.time.for.loan");
            }

            if (graceOnChargePeriodAmount != null) {
                this.graceOnChargePeriodAmount = graceOnChargePeriodAmount;
            }

            if (parentChargeId != null) {
                this.parentChargeId = parentChargeId;
            }
        }

        if (isPercentageOfApprovedAmount()) {
            this.minCap = minCap;
            this.maxCap = maxCap;
        }
        this.getPercentageFromTable = getPercentageFromTable;
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    public String getName() {
        return this.name;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public String getCurrencyCode() {
        return this.currencyCode;
    }

    public Integer getChargeTimeType() {
        return this.chargeTimeType;
    }

    public Integer getChargeCalculation() {
        return this.chargeCalculation;
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isPenalty() {
        return this.penalty;
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public boolean isLoanCharge() {
        return ChargeAppliesTo.fromInt(this.chargeAppliesTo).isLoanCharge();
    }

    public boolean isAllowedLoanChargeTime() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isAllowedLoanChargeTime();
    }

    public boolean isAllowedClientChargeTime() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isAllowedClientChargeTime();
    }

    public boolean isSavingsCharge() {
        return ChargeAppliesTo.fromInt(this.chargeAppliesTo).isSavingsCharge();
    }

    public boolean isClientCharge() {
        return ChargeAppliesTo.fromInt(this.chargeAppliesTo).isClientCharge();
    }

    public boolean isAllowedSavingsChargeTime() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isAllowedSavingsChargeTime();
    }

    public boolean isAllowedSavingsChargeCalculationType() {
        return ChargeCalculationType.fromInt(this.chargeCalculation).isAllowedSavingsChargeCalculationType();
    }

    public boolean isAllowedClientChargeCalculationType() {
        return ChargeCalculationType.fromInt(this.chargeCalculation).isAllowedClientChargeCalculationType();
    }

    public boolean isPercentageOfApprovedAmount() {
        return ChargeCalculationType.fromInt(this.chargeCalculation).isPercentageOfInstallmentPrincipal();
    }

    public boolean isPercentageOfDisbursementAmount() {
        return ChargeCalculationType.fromInt(this.chargeCalculation).isPercentageOfDisbursement();
    }

    public boolean isPercentageOfAnotherCharge() {
        return ChargeCalculationType.fromInt(this.chargeCalculation).isPercentageOfAnotherCharge();
    }

    public boolean isFlatHono() {
        return ChargeCalculationType.fromInt(this.chargeCalculation).isFlatHono();
    }

    public boolean isCustomFlatDistributedCharge() {
        // Charge is distributed among the installments
        return isCustomFlatVoluntaryInsurenceCharge() || ChargeCalculationType.fromInt(this.chargeCalculation).isFlatMandatoryInsurance();
    }

    public boolean isFlatMandatoryInsurance() {
        // Charge is distributed among the installments
        return ChargeCalculationType.fromInt(this.chargeCalculation).isFlatMandatoryInsurance();
    }

    public boolean isCustomPercentageBasedDistributedCharge() {
        // Charge is distributed among the installments
        return ChargeCalculationType.fromInt(this.chargeCalculation).isPercentageBasedMandatoryInsurance();
    }

    public boolean isAvalCharge() {
        // Charge is distributed among the installments
        return ChargeCalculationType.fromInt(this.chargeCalculation).isPercentageOfAval();
    }

    public boolean isMandatoryInsurance() {
        // Charge is distributed among the installments
        return ChargeCalculationType.fromInt(this.chargeCalculation).isMandatoryInsuranceCharge();
    }

    public boolean isVoluntaryInsurance() {
        // Charge is distributed among the installments
        return ChargeCalculationType.fromInt(this.chargeCalculation).isVoluntaryInsurance();
    }

    public boolean isCustomPercentageBasedOfAnotherCharge() {
        // Charge is distributed among the installments
        return ChargeCalculationType.fromInt(this.chargeCalculation).isPercentageOfAnotherCharge();
    }

    public boolean isCustomPercentageOfOutstandingPrincipalCharge() {
        // Charge is distributed among the installments
        return ChargeCalculationType.fromInt(this.chargeCalculation).isCustomPercentageOfOutstandingPrincipalCharge();
    }

    public boolean isCustomFlatVoluntaryInsurenceCharge() {
        return ChargeCalculationType.fromInt(this.chargeCalculation).getCode().equals(ChargeCalculationType.FLAT_SEGOVOLUNTARIO.getCode());
    }

    public boolean isPercentageBasedMandatoryInsurance() {
        return ChargeCalculationType.fromInt(this.chargeCalculation).isPercentageBasedMandatoryInsurance();
    }

    public boolean isInstallmentFee() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isInstalmentFee();
    }

    public BigDecimal getMinCap() {
        return this.minCap;
    }

    public BigDecimal getMaxCap() {
        return this.maxCap;
    }

    public boolean isEnableFreeWithdrawal() {
        return this.enableFreeWithdrawal;
    }

    public boolean isEnablePaymentType() {
        return this.enablePaymentType;
    }

    public Integer getFrequencyFreeWithdrawalCharge() {
        return this.freeWithdrawalFrequency;
    }

    public Integer getRestartFrequency() {
        return this.restartFrequency;
    }

    public Integer getRestartFrequencyEnum() {
        return this.restartFrequencyEnum;
    }

    public PaymentType getPaymentType() {
        return this.paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    private Long getPaymentTypeId() {
        Long paymentTypeId = null;
        if (this.paymentType != null) {
            paymentTypeId = this.paymentType.getId();
        }
        return paymentTypeId;
    }

    private Long getInterestRateId() {
        Long interestRateId = null;
        if (this.interestRate != null) {
            interestRateId = this.interestRate.getId();
        }
        return interestRateId;
    }

    public Long getParentChargeId() {
        return parentChargeId;
    }

    public boolean isGetPercentageFromTable() {
        return getPercentageFromTable;
    }

    public Map<String, Object> update(final JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final Locale locale = command.extractLocale();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("charges");

        final String nameParamName = "name";
        if (command.isChangeInStringParameterNamed(nameParamName, this.name)) {
            final String newValue = command.stringValueOfParameterNamed(nameParamName);
            actualChanges.put(nameParamName, newValue);
            this.name = newValue;
        }

        final String currencyCodeParamName = "currencyCode";
        if (command.isChangeInStringParameterNamed(currencyCodeParamName, this.currencyCode)) {
            final String newValue = command.stringValueOfParameterNamed(currencyCodeParamName);
            actualChanges.put(currencyCodeParamName, newValue);
            this.currencyCode = newValue;
        }

        final String amountParamName = "amount";
        if (command.isChangeInBigDecimalParameterNamed(amountParamName, this.amount)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(amountParamName, locale);
            actualChanges.put(amountParamName, newValue);
            actualChanges.put("locale", locale.getLanguage());
            this.amount = newValue;
        }

        final String chargeTimeParamName = "chargeTimeType";
        if (command.isChangeInIntegerParameterNamed(chargeTimeParamName, this.chargeTimeType)) {
            final Integer newValue = command.integerValueOfParameterNamed(chargeTimeParamName);
            actualChanges.put(chargeTimeParamName, newValue);
            actualChanges.put("locale", locale.getLanguage());
            this.chargeTimeType = ChargeTimeType.fromInt(newValue).getValue();

            if (isSavingsCharge()) {
                if (!isAllowedSavingsChargeTime()) {
                    baseDataValidator.reset().parameter("chargeTimeType").value(this.chargeTimeType)
                            .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.time.for.savings");
                }
                // if charge time is changed to monthly then validate for
                // feeOnMonthDay and feeInterval
                if (isMonthlyFee()) {
                    final MonthDay monthDay = command.extractMonthDayNamed("feeOnMonthDay");
                    baseDataValidator.reset().parameter("feeOnMonthDay").value(monthDay).notNull();

                    final Integer feeInterval = command.integerValueOfParameterNamed("feeInterval");
                    baseDataValidator.reset().parameter("feeInterval").value(feeInterval).notNull().inMinMaxRange(1, 12);
                }
            } else if (isLoanCharge()) {
                if (!isAllowedLoanChargeTime()) {
                    baseDataValidator.reset().parameter("chargeTimeType").value(this.chargeTimeType)
                            .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.time.for.loan");
                }
            } else if (isClientCharge()) {
                if (!isAllowedLoanChargeTime()) {
                    baseDataValidator.reset().parameter("chargeTimeType").value(this.chargeTimeType)
                            .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.time.for.client");
                }
            }
        }

        final String freeWithdrawalFrequencyParamName = "freeWithdrawalFrequency";
        if (command.isChangeInIntegerParameterNamed(freeWithdrawalFrequencyParamName, this.freeWithdrawalFrequency)) {
            final Integer enableFreeWithdrawalChargeNewValue = command.integerValueOfParameterNamed(freeWithdrawalFrequencyParamName);
            actualChanges.put(freeWithdrawalFrequencyParamName, enableFreeWithdrawalChargeNewValue);
            this.freeWithdrawalFrequency = enableFreeWithdrawalChargeNewValue;
        }

        final String restartCountFrequencyParamName = "restartCountFrequency";
        if (command.isChangeInIntegerParameterNamed(restartCountFrequencyParamName, this.restartFrequency)) {
            final Integer restartCountFrequencyNewValue = command.integerValueOfParameterNamed(restartCountFrequencyParamName);
            actualChanges.put(restartCountFrequencyParamName, restartCountFrequencyNewValue);
            this.restartFrequency = restartCountFrequencyNewValue;
        }

        final String countFrequencyTypeParamName = "countFrequencyType";
        if (command.isChangeInIntegerParameterNamed(countFrequencyTypeParamName, this.restartFrequencyEnum)) {
            final Integer countFrequencyTypeNewValue = command.integerValueOfParameterNamed(countFrequencyTypeParamName);
            actualChanges.put(countFrequencyTypeParamName, countFrequencyTypeNewValue);
            this.restartFrequencyEnum = ChargeTimeType.fromInt(countFrequencyTypeNewValue).getValue();
        }

        final String enableFreeWithdrawalChargeParamName = "enableFreeWithdrawalCharge";
        if (command.isChangeInBooleanParameterNamed(enableFreeWithdrawalChargeParamName, this.enableFreeWithdrawal)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(enableFreeWithdrawalChargeParamName);
            actualChanges.put(enableFreeWithdrawalChargeParamName, newValue);
            this.enableFreeWithdrawal = newValue;

        }

        final String enablePaymentTypeParamName = "enablePaymentType";
        if (command.isChangeInBooleanParameterNamed(enablePaymentTypeParamName, this.enablePaymentType)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(enablePaymentTypeParamName);
            actualChanges.put(enablePaymentTypeParamName, newValue);
            this.enablePaymentType = newValue;
        }

        final String paymentTypeParamName = "paymentTypeId";
        if (command.isChangeInLongParameterNamed(paymentTypeParamName, getPaymentTypeId())) {
            final Long newValue = command.longValueOfParameterNamed(paymentTypeParamName);
            actualChanges.put(paymentTypeParamName, newValue);
        }

        final String interestRateParamName = "interestRateId";
        if (command.isChangeInLongParameterNamed(interestRateParamName, getInterestRateId())) {
            final Long newValue = command.longValueOfParameterNamed(interestRateParamName);
            actualChanges.put(interestRateParamName, newValue);
        }

        final String chargeAppliesToParamName = "chargeAppliesTo";
        if (command.isChangeInIntegerParameterNamed(chargeAppliesToParamName, this.chargeAppliesTo)) {
            /*
             * final Integer newValue = command.integerValueOfParameterNamed(chargeAppliesToParamName);
             * actualChanges.put(chargeAppliesToParamName, newValue); actualChanges.put("locale", localeAsInput);
             * this.chargeAppliesTo = ChargeAppliesTo.fromInt(newValue).getValue();
             */

            // AA: Do not allow to change chargeAppliesTo.
            final String errorMessage = "Update of Charge applies to is not supported";
            throw new ChargeParameterUpdateNotSupportedException("charge.applies.to", errorMessage);
        }

        final String chargeCalculationParamName = "chargeCalculationType";
        if (command.isChangeInIntegerParameterNamed(chargeCalculationParamName, this.chargeCalculation)) {
            final Integer newValue = command.integerValueOfParameterNamed(chargeCalculationParamName);
            actualChanges.put(chargeCalculationParamName, newValue);
            actualChanges.put("locale", locale.getLanguage());
            this.chargeCalculation = ChargeCalculationType.fromInt(newValue).getValue();

            if (isSavingsCharge()) {
                if (!isAllowedSavingsChargeCalculationType()) {
                    baseDataValidator.reset().parameter("chargeCalculationType").value(this.chargeCalculation)
                            .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.calculation.type.for.savings");
                }

                if (!(ChargeTimeType.fromInt(getChargeTimeType()).isWithdrawalFee()
                        || ChargeTimeType.fromInt(getChargeTimeType()).isSavingsNoActivityFee())
                        && ChargeCalculationType.fromInt(getChargeCalculation()).isPercentageOfInstallmentPrincipal()) {
                    baseDataValidator.reset().parameter("chargeCalculationType").value(this.chargeCalculation)
                            .failWithCodeNoParameterAddedToErrorCode(
                                    "charge.calculation.type.percentage.allowed.only.for.withdrawal.or.noactivity");
                }
            } else if (isClientCharge()) {
                if (!isAllowedClientChargeCalculationType()) {
                    baseDataValidator.reset().parameter("chargeCalculationType").value(this.chargeCalculation)
                            .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.calculation.type.for.client");
                }
            }
        }

        // validate only for loan charge
        if (isLoanCharge()) {
            final String paymentModeParamName = "chargePaymentMode";
            if (command.isChangeInIntegerParameterNamed(paymentModeParamName, this.chargePaymentMode)) {
                final Integer newValue = command.integerValueOfParameterNamed(paymentModeParamName);
                actualChanges.put(paymentModeParamName, newValue);
                actualChanges.put("locale", locale.getLanguage());
                this.chargePaymentMode = ChargePaymentMode.fromInt(newValue).getValue();
            }

            // Load grace period, if any changes
            if (command.isChangeInIntegerParameterNamed(ChargesApiConstants.graceOnChargePeriodEnumIdParamName,
                    Integer.valueOf(this.graceOnChargePeriodEnum))) {
                final Integer newValue = command.integerValueOfParameterNamed(ChargesApiConstants.graceOnChargePeriodEnumIdParamName);
                actualChanges.put(ChargesApiConstants.graceOnChargePeriodEnumIdParamName, newValue);
                this.graceOnChargePeriodEnum = newValue.shortValue();
            }

            if (command.isChangeInLongParameterNamed(ChargesApiConstants.graceOnChargePeriodAmountParamName,
                    this.graceOnChargePeriodAmount)) {
                final Long newValue = command.longValueOfParameterNamed(ChargesApiConstants.graceOnChargePeriodAmountParamName);
                actualChanges.put(ChargesApiConstants.graceOnChargePeriodAmountParamName, newValue);
                this.graceOnChargePeriodAmount = newValue;
            }

            if (command.isChangeInLongParameterNamed(ChargesApiConstants.parentChargeIdParamName, this.parentChargeId)) {
                final Long newValue = command.longValueOfParameterNamed(ChargesApiConstants.parentChargeIdParamName);
                actualChanges.put(ChargesApiConstants.parentChargeIdParamName, newValue);
                this.parentChargeId = newValue;
            }

            final ChargeCalculationType chargeCalculationType = ChargeCalculationType
                    .fromInt(command.integerValueOfParameterNamed(chargeCalculationParamName));
            if (chargeCalculationType.isInsurance()) {
                if (chargeCalculationType.isVoluntaryInsurance()) {
                    validateVoluntaryInsuranceCondition(chargeCalculationType);
                }
                if (this.chargeInsuranceDetail == null) {
                    this.chargeInsuranceDetail = new ChargeInsuranceDetail();
                }
                this.chargeInsuranceDetail.update(command, actualChanges);
            }
        }

        if (command.hasParameter("feeOnMonthDay")) {
            final MonthDay monthDay = command.extractMonthDayNamed("feeOnMonthDay");
            final String actualValueEntered = command.stringValueOfParameterNamed("feeOnMonthDay");
            final Integer dayOfMonthValue = monthDay.getDayOfMonth();
            if (!this.feeOnDay.equals(dayOfMonthValue)) {
                actualChanges.put("feeOnMonthDay", actualValueEntered);
                actualChanges.put("locale", locale.getLanguage());
                this.feeOnDay = dayOfMonthValue;
            }

            final Integer monthOfYear = monthDay.getMonthValue();
            if (!this.feeOnMonth.equals(monthOfYear)) {
                actualChanges.put("feeOnMonthDay", actualValueEntered);
                actualChanges.put("locale", locale.getLanguage());
                this.feeOnMonth = monthOfYear;
            }
        }

        final String feeInterval = "feeInterval";
        if (command.isChangeInIntegerParameterNamed(feeInterval, this.feeInterval)) {
            final Integer newValue = command.integerValueOfParameterNamed(feeInterval);
            actualChanges.put(feeInterval, newValue);
            actualChanges.put("locale", locale.getLanguage());
            this.feeInterval = newValue;
        }

        final String feeFrequency = "feeFrequency";
        if (command.isChangeInIntegerParameterNamed(feeFrequency, this.feeFrequency)) {
            final Integer newValue = command.integerValueOfParameterNamed(feeFrequency);
            actualChanges.put(feeFrequency, newValue);
            actualChanges.put("locale", locale.getLanguage());
            this.feeFrequency = newValue;
        }

        if (this.feeFrequency != null) {
            baseDataValidator.reset().parameter("feeInterval").value(this.feeInterval).notNull();
        }

        final String penaltyParamName = "penalty";
        if (command.isChangeInBooleanParameterNamed(penaltyParamName, this.penalty)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(penaltyParamName);
            actualChanges.put(penaltyParamName, newValue);
            this.penalty = newValue;
        }

        final String activeParamName = "active";
        if (command.isChangeInBooleanParameterNamed(activeParamName, this.active)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(activeParamName);
            actualChanges.put(activeParamName, newValue);
            this.active = newValue;
        }
        // allow min and max cap to be only added to PERCENT_OF_AMOUNT for now
        if (isPercentageOfApprovedAmount()) {
            final String minCapParamName = "minCap";
            if (command.isChangeInBigDecimalParameterNamed(minCapParamName, this.minCap)) {
                final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(minCapParamName);
                actualChanges.put(minCapParamName, newValue);
                actualChanges.put("locale", locale.getLanguage());
                this.minCap = newValue;
            }
            final String maxCapParamName = "maxCap";
            if (command.isChangeInBigDecimalParameterNamed(maxCapParamName, this.maxCap)) {
                final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maxCapParamName);
                actualChanges.put(maxCapParamName, newValue);
                actualChanges.put("locale", locale.getLanguage());
                this.maxCap = newValue;
            }

        }

        if (this.penalty && ChargeTimeType.fromInt(this.chargeTimeType).isTimeOfDisbursement()) {
            throw new ChargeDueAtDisbursementCannotBePenaltyException(this.name);
        }
        if (!penalty && ChargeTimeType.fromInt(this.chargeTimeType).isOverdueInstallment()) {
            throw new ChargeMustBePenaltyException(name);
        }

        if (command.isChangeInLongParameterNamed(ChargesApiConstants.glAccountIdParamName, getIncomeAccountId())) {
            final Long newValue = command.longValueOfParameterNamed(ChargesApiConstants.glAccountIdParamName);
            actualChanges.put(ChargesApiConstants.glAccountIdParamName, newValue);
        }

        if (command.isChangeInLongParameterNamed(ChargesApiConstants.taxGroupIdParamName, getTaxGroupId())) {
            final Long newValue = command.longValueOfParameterNamed(ChargesApiConstants.taxGroupIdParamName);
            actualChanges.put(ChargesApiConstants.taxGroupIdParamName, newValue);
            if (taxGroup != null) {
                baseDataValidator.reset().parameter(ChargesApiConstants.taxGroupIdParamName).failWithCode("modification.not.supported");
            }
        }

        if (command.isChangeInBooleanParameterNamed(ChargesApiConstants.GET_INTEREST_PERCENTAGE_FROM_TABLE, this.getPercentageFromTable)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(ChargesApiConstants.GET_INTEREST_PERCENTAGE_FROM_TABLE);
            actualChanges.put(ChargesApiConstants.GET_INTEREST_PERCENTAGE_FROM_TABLE, newValue);
            this.getPercentageFromTable = newValue;
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }

        return actualChanges;
    }

    /**
     * Delete is a <i>soft delete</i>. Updates flag on charge so it wont appear in query/report results.
     *
     * Any fields with unique constraints and prepended with id of record.
     */
    public void delete() {
        this.deleted = true;
        this.name = getId() + "_" + this.name;
    }

    public ChargeData toData() {
        final EnumOptionData chargeTimeType = ChargeEnumerations.chargeTimeType(this.chargeTimeType);
        final EnumOptionData chargeAppliesTo = ChargeEnumerations.chargeAppliesTo(this.chargeAppliesTo);
        final EnumOptionData chargeCalculationType = ChargeEnumerations.chargeCalculationType(this.chargeCalculation);
        final EnumOptionData chargePaymentMode = ChargeEnumerations.chargePaymentMode(this.chargePaymentMode);
        EnumOptionData feeFrequencyType = null;
        if (this.feeFrequency != null) {
            feeFrequencyType = ChargeEnumerations.feeFrequencyType(this.feeFrequency);
        }
        GLAccountData accountData = null;
        if (account != null) {
            accountData = new GLAccountData().setId(account.getId()).setName(account.getName()).setGlCode(account.getGlCode());
        }
        TaxGroupData taxGroupData = null;
        if (this.taxGroup != null) {
            taxGroupData = TaxGroupData.lookup(taxGroup.getId(), taxGroup.getName());
        }

        PaymentTypeData paymentTypeData = null;
        if (this.paymentType != null) {
            paymentTypeData = PaymentTypeData.instance(paymentType.getId(), paymentType.getName());
        }

        ChargeInsuranceDetailData chargeInsuranceDetailData = new ChargeInsuranceDetailData(null,
                this.chargeInsuranceDetail.getInsuranceName(), this.chargeInsuranceDetail.getInsuranceChargedAs().getValue().longValue(),
                this.chargeInsuranceDetail.getInsuranceCompany(), this.chargeInsuranceDetail.getInsurerName(),
                this.chargeInsuranceDetail.getInsuranceCode(), this.chargeInsuranceDetail.getInsurancePlan(),
                this.chargeInsuranceDetail.getBaseValue(), this.chargeInsuranceDetail.getVatValue(),
                this.chargeInsuranceDetail.getTotalValue(), this.chargeInsuranceDetail.getDeadline(), null,
                this.chargeInsuranceDetail.getDaysInArrears());
        final CurrencyData currency = new CurrencyData(this.currencyCode, null, 0, 0, null, null);
        return ChargeData.instance(getId(), this.name, this.amount, currency, chargeTimeType, chargeAppliesTo, chargeCalculationType,
                chargePaymentMode, getFeeOnMonthDay(), this.feeInterval, this.penalty, this.active, this.enableFreeWithdrawal,
                this.freeWithdrawalFrequency, this.restartFrequency, this.restartFrequencyEnum, this.enablePaymentType, paymentTypeData,
                this.minCap, this.maxCap, feeFrequencyType, accountData, taxGroupData, chargeInsuranceDetailData,
                this.getPercentageFromTable);
    }

    public Integer getChargePaymentMode() {
        return this.chargePaymentMode;
    }

    public Integer getFeeInterval() {
        return this.feeInterval;
    }

    public boolean isMonthlyFee() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isMonthlyFee();
    }

    public boolean isAnnualFee() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isAnnualFee();
    }

    public boolean isOverdueInstallment() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isOverdueInstallment();
    }

    public MonthDay getFeeOnMonthDay() {
        MonthDay feeOnMonthDay = null;
        if (this.feeOnDay != null && this.feeOnMonth != null) {
            feeOnMonthDay = MonthDay.now(DateUtils.getDateTimeZoneOfTenant()).withMonth(this.feeOnMonth).withDayOfMonth(this.feeOnDay);
        }
        return feeOnMonthDay;
    }

    public Integer feeInterval() {
        return this.feeInterval;
    }

    public Integer feeFrequency() {
        return this.feeFrequency;
    }

    public GLAccount getAccount() {
        return this.account;
    }

    public void setAccount(GLAccount account) {
        this.account = account;
    }

    public Long getIncomeAccountId() {
        Long incomeAccountId = null;
        if (this.account != null) {
            incomeAccountId = this.account.getId();
        }
        return incomeAccountId;
    }

    private Long getTaxGroupId() {
        Long taxGroupId = null;
        if (this.taxGroup != null) {
            taxGroupId = this.taxGroup.getId();
        }
        return taxGroupId;
    }

    public boolean isDisbursementCharge() {
        return ChargeTimeType.fromInt(this.chargeTimeType).equals(ChargeTimeType.DISBURSEMENT)
                || ChargeTimeType.fromInt(this.chargeTimeType).equals(ChargeTimeType.TRANCHE_DISBURSEMENT);
    }

    public TaxGroup getTaxGroup() {
        return this.taxGroup;
    }

    public void setTaxGroup(TaxGroup taxGroup) {
        this.taxGroup = taxGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Charge)) {
            return false;
        }
        Charge other = (Charge) o;
        return Objects.equals(name, other.name) && Objects.equals(amount, other.amount) && Objects.equals(currencyCode, other.currencyCode)
                && Objects.equals(chargeAppliesTo, other.chargeAppliesTo) && Objects.equals(chargeTimeType, other.chargeTimeType)
                && Objects.equals(chargeCalculation, other.chargeCalculation) && Objects.equals(chargePaymentMode, other.chargePaymentMode)
                && Objects.equals(feeOnDay, other.feeOnDay) && Objects.equals(feeInterval, other.feeInterval)
                && Objects.equals(feeOnMonth, other.feeOnMonth) && penalty == other.penalty && active == other.active
                && deleted == other.deleted && Objects.equals(minCap, other.minCap) && Objects.equals(maxCap, other.maxCap)
                && Objects.equals(feeFrequency, other.feeFrequency) && Objects.equals(account, other.account)
                && Objects.equals(taxGroup, other.taxGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, amount, currencyCode, chargeAppliesTo, chargeTimeType, chargeCalculation, chargePaymentMode, feeOnDay,
                feeInterval, feeOnMonth, penalty, active, deleted, minCap, maxCap, feeFrequency, account, taxGroup);
    }

    public boolean hasCustomGracePeriodDefined() {
        return this.graceOnChargePeriodAmount.compareTo(0L) > 0;
    }

    public Short getGraceOnChargePeriodEnum() {
        return graceOnChargePeriodEnum;
    }

    public Long getGraceOnChargePeriodAmount() {
        return graceOnChargePeriodAmount;
    }

    private static void validateVoluntaryInsuranceCondition(ChargeCalculationType chargeCalculationType) {
        if (!chargeCalculationType.isFlat() || chargeCalculationType.isPercentageOfDisbursement()
                || chargeCalculationType.isPercentageOfInstallmentPrincipal() || chargeCalculationType.isPercentageOfInstallmentInterest()
                || chargeCalculationType.isPercentageOfOutstandingPrincipal() || chargeCalculationType.isPercentageOfOutstandingInterest()
                || chargeCalculationType.isPercentageOfInsurance() || chargeCalculationType.isPercentageOfAval()
                || chargeCalculationType.isPercentageOfHonorarios() || chargeCalculationType.isPercentageOfAnotherCharge()) {
            throw new ChargeCannotBeCreatedException("error.msg.charge.calculation.type.invalid.forVoluntaryInsurance",
                    "Only Flat Charge Calculation Type can be selected for Voluntary Insurance",
                    chargeCalculationType.isVoluntaryInsurance());
        }
    }

    public void validateChargeIsSetupCorrectly() {
        if (this.isLoanCharge()) {
            final ChargeCalculationType chargeCalculationType = ChargeCalculationType.fromInt(this.getChargeCalculation());
            String code = chargeCalculationType.getByteRepresentation();
            if (code.length() == 10) {
                code = code + "0"; // Voluntary Insurance code is 11 digit
            }
            if (this.isPenalty()) {
                if (!ChargeTimeType.fromInt(this.getChargeTimeType()).isOverdueInstallment()) {
                    throw new GeneralPlatformDomainRuleException("error.msg.charge.not.setup.correctly", "Charge not setup correctly",
                            this.getName());
                }
                if (this.interestRate == null) {
                    throw new GeneralPlatformDomainRuleException("error.msg.charge.not.setup.correctly", "Charge not setup correctly",
                            this.getName());
                }
                if (this.isCustomPercentageBasedOfAnotherCharge()) {
                    verifyChargeConfiguration(code, ChargeCalculationTypeBaseItemsEnum.PERCENT_OF_ANOTHER_CHARGE.getIndex(), null, null,
                            null, null);
                } else {
                    verifyChargeConfiguration(code, ChargeCalculationTypeBaseItemsEnum.SEGURO_OBRIGATORIO.getIndex(),
                            ChargeCalculationTypeBaseItemsEnum.SEGURO_VOLUNTARIO.getIndex(),
                            ChargeCalculationTypeBaseItemsEnum.AVAL.getIndex(),
                            ChargeCalculationTypeBaseItemsEnum.INTEREST_INSTALLMENT.getIndex(),
                            ChargeCalculationTypeBaseItemsEnum.PRINCIPAL_INSTALLMENT.getIndex());
                }
            } else if (this.isMandatoryInsurance()) {
                if (this.isFlatMandatoryInsurance()) { // Flat and Mandatory Insurance
                    verifyChargeConfiguration(code, ChargeCalculationTypeBaseItemsEnum.SEGURO_OBRIGATORIO.getIndex(),
                            ChargeCalculationTypeBaseItemsEnum.FLAT.getIndex(), null, null, null);
                } else if (this.isPercentageBasedMandatoryInsurance()) { // Disbursement and Mandatory Insurance
                    verifyChargeConfiguration(code, ChargeCalculationTypeBaseItemsEnum.SEGURO_OBRIGATORIO.getIndex(),
                            ChargeCalculationTypeBaseItemsEnum.DISBURSED_AMOUNT.getIndex(), null, null, null);
                } else if (this.isCustomPercentageOfOutstandingPrincipalCharge()) { // Disbursement and Mandatory
                                                                                    // Insurance
                    verifyChargeConfiguration(code, ChargeCalculationTypeBaseItemsEnum.SEGURO_OBRIGATORIO.getIndex(),
                            ChargeCalculationTypeBaseItemsEnum.OUTSTANDING_PRINCIPAL.getIndex(), null, null, null);
                }
            } else if (this.isCustomFlatVoluntaryInsurenceCharge()) {
                verifyChargeConfiguration(code, ChargeCalculationTypeBaseItemsEnum.SEGURO_VOLUNTARIO.getIndex(),
                        ChargeCalculationTypeBaseItemsEnum.FLAT.getIndex(), null, null, null);
            } else if (this.isAvalCharge()) {
                verifyChargeConfiguration(code, ChargeCalculationTypeBaseItemsEnum.AVAL.getIndex(),
                        ChargeCalculationTypeBaseItemsEnum.DISBURSED_AMOUNT.getIndex(), null, null, null);
            } else if (this.isCustomPercentageBasedOfAnotherCharge()) {
                verifyChargeConfiguration(code, ChargeCalculationTypeBaseItemsEnum.PERCENT_OF_ANOTHER_CHARGE.getIndex(), null, null, null,
                        null);
            } else if (this.isFlatHono()) {
                verifyChargeConfiguration(code, ChargeCalculationTypeBaseItemsEnum.FLAT.getIndex(),
                        ChargeCalculationTypeBaseItemsEnum.HOORARIOS.getIndex(), null, null, null);
            } else {
                throw new GeneralPlatformDomainRuleException("error.msg.charge.not.setup.correctly", "Charge not setup correctly",
                        this.getName());
            }
        }
    }

    private void verifyChargeConfiguration(String code, Integer index1, Integer index2, Integer index3, Integer index4, Integer index5) {
        if (this.isAvalCharge() && !this.isPenalty()) {
            if (!this.isGetPercentageFromTable()) {
                throw new GeneralPlatformDomainRuleException("error.msg.charge.not.setup.correctly", "Charge not setup correctly",
                        this.getName());
            }
        }
        if (!this.isPenalty() && !this.isInstallmentFee()) {
            throw new GeneralPlatformDomainRuleException("error.msg.charge.not.setup.correctly", "Charge not setup correctly",
                    this.getName());
        }
        char[] codeArray = code.toCharArray();
        codeArray[index1] = '0';

        if (index2 != null) {
            codeArray[index2] = '0';
        }
        if (index3 != null) {
            codeArray[index3] = '0';
        }
        if (index4 != null) {
            codeArray[index4] = '0';
        }
        if (index5 != null) {
            codeArray[index5] = '0';
        }
        code = String.valueOf(codeArray);
        if (code.indexOf('1') != -1) {
            throw new GeneralPlatformDomainRuleException("error.msg.charge.not.setup.correctly", "Charge not setup correctly",
                    this.getName());
        }
    }

    public ChargeInsuranceDetail getChargeInsuranceDetail() {
        return this.chargeInsuranceDetail;
    }

}
