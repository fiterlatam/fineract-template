package org.apache.fineract.portfolio.charge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.util.Map;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.portfolio.charge.api.ChargesApiConstants;

@Embeddable
public class ChargeInsuranceDetail {

    @Column(name = "insurance_name")
    private String insuranceName;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "insurance_charged_as")
    private ChargeInsuranceType insuranceChargedAs;

    @Column(name = "insurance_company")
    private String insuranceCompany;

    @Column(name = "insurer_name")
    private String insurerName;

    @Column(name = "insurance_code")
    private Long insuranceCode;

    @Column(name = "insurance_plan")
    private String insurancePlan;

    @Column(name = "base_value", scale = 6, precision = 19)
    private BigDecimal baseValue;

    @Column(name = "vat_value", scale = 6, precision = 19)
    private BigDecimal vatValue;

    @Column(name = "total_value", scale = 6, precision = 19)
    private BigDecimal totalValue;

    @Column(name = "deadline")
    private Long deadline;

    public ChargeInsuranceDetail() {

    }

    public ChargeInsuranceDetail(String insuranceName, ChargeInsuranceType insuranceChargedAs, String insuranceCompany, String insurerName,
            Long insuranceCode, String insurancePlan, BigDecimal baseValue, BigDecimal vatValue, BigDecimal totalValue, Long deadline) {
        this.insuranceName = insuranceName;
        this.insuranceChargedAs = insuranceChargedAs;
        this.insuranceCompany = insuranceCompany;
        this.insurerName = insurerName;
        this.insuranceCode = insuranceCode;
        this.insurancePlan = insurancePlan;
        this.baseValue = baseValue;
        this.vatValue = vatValue;
        this.totalValue = totalValue;
        this.deadline = deadline;
    }

    public String getInsuranceName() {
        return insuranceName;
    }

    public ChargeInsuranceType getInsuranceChargedAs() {
        return insuranceChargedAs;
    }

    public String getInsuranceCompany() {
        return insuranceCompany;
    }

    public String getInsurerName() {
        return insurerName;
    }

    public Long getInsuranceCode() {
        return insuranceCode;
    }

    public String getInsurancePlan() {
        return insurancePlan;
    }

    public BigDecimal getBaseValue() {
        return baseValue;
    }

    public BigDecimal getVatValue() {
        return vatValue;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public Long getDeadline() {
        return deadline;
    }

    public Map<String, Object> update(final JsonCommand command, Map<String, Object> actualChanges) {

        final String insuranceCompanyParamName = ChargesApiConstants.insuranceCompanyParamName;
        if (command.isChangeInStringParameterNamed(insuranceCompanyParamName, this.insuranceCompany)) {
            final String newValue = command.stringValueOfParameterNamed(insuranceCompanyParamName);
            actualChanges.put(insuranceCompanyParamName, newValue);
            this.insuranceCompany = newValue;
        }

        final String insuranceNameParamName = ChargesApiConstants.insuranceNameParamName;
        if (command.isChangeInStringParameterNamed(insuranceNameParamName, this.insuranceName)) {
            final String newValue = command.stringValueOfParameterNamed(insuranceNameParamName);
            actualChanges.put(insuranceNameParamName, newValue);
            this.insuranceName = newValue;
        }

        final String insuranceChargedAsParamName = ChargesApiConstants.insuranceChargedAsParamName;
        if (command.isChangeInIntegerParameterNamed(insuranceChargedAsParamName, this.insuranceChargedAs.getValue())) {
            final Integer newValue = command.integerValueOfParameterNamed(insuranceChargedAsParamName);
            actualChanges.put(insuranceChargedAsParamName, newValue);
            this.insuranceChargedAs = ChargeInsuranceType.fromInt(newValue);
        }

        final String insurerNameParamName = ChargesApiConstants.insurerNameParamName;
        if (command.isChangeInStringParameterNamed(insurerNameParamName, this.insurerName)) {
            final String newValue = command.stringValueOfParameterNamed(insurerNameParamName);
            actualChanges.put(insurerNameParamName, newValue);
            this.insurerName = newValue;
        }

        final String insuranceCodeParamName = ChargesApiConstants.insuranceCodeParamName;
        if (command.isChangeInLongParameterNamed(insuranceCodeParamName, this.insuranceCode)) {
            final Long newValue = command.longValueOfParameterNamed(insuranceCodeParamName);
            actualChanges.put(insuranceCodeParamName, newValue);
            this.insuranceCode = newValue;
        }

        final String insurancePlanParamName = ChargesApiConstants.insurancePlanParamName;
        if (command.isChangeInStringParameterNamed(insurancePlanParamName, this.insurerName)) {
            final String newValue = command.stringValueOfParameterNamed(insurancePlanParamName);
            actualChanges.put(insurancePlanParamName, newValue);
            this.insurancePlan = newValue;
        }

        final String baseValueParamName = ChargesApiConstants.baseValueParamName;
        if (command.isChangeInBigDecimalParameterNamed(baseValueParamName, this.baseValue)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(baseValueParamName);
            actualChanges.put(baseValueParamName, newValue);
            this.baseValue = newValue;
        }

        final String vatValueParamName = ChargesApiConstants.vatValueParamName;
        if (command.isChangeInBigDecimalParameterNamed(vatValueParamName, this.vatValue)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(vatValueParamName);
            actualChanges.put(vatValueParamName, newValue);
            this.vatValue = newValue;
        }

        final String totalValueParamName = ChargesApiConstants.totalValueParamName;
        if (command.isChangeInBigDecimalParameterNamed(totalValueParamName, this.totalValue)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(totalValueParamName);
            actualChanges.put(totalValueParamName, newValue);
            this.totalValue = newValue;
        }

        if (this.insuranceChargedAs.isCargo()) {
            this.deadline = null;
        } else {
            final String deadlineParamName = ChargesApiConstants.deadlineParamName;
            if (command.isChangeInLongParameterNamed(deadlineParamName, this.deadline)) {
                final Long newValue = command.longValueOfParameterNamed(deadlineParamName);
                actualChanges.put(deadlineParamName, newValue);
                this.deadline = newValue;
            }
        }

        return actualChanges;
    }
}
