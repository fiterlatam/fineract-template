package org.apache.fineract.portfolio.insurance.domain;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum InsuranceIncidentType {
    INVALID(0, "insurance.incident.invalid"), //
    DEFINITIVE_CANCELLATION_DEFAULT(1, "labels.inputs.insurance.incident.definitive.default"), //
    DEFINITIVE_VOLUNTARY_CANCELLATION(2, "labels.inputs.insurance.incident.definitive.voluntary.cancellation"), //
    FINAL_ADVANCE_PAYMENT_CANCELLATION(3, "labels.inputs.insurance.incident.final.advance.payment.cancellation"), //
    FINAL_GUARANTEE_CLAIM_CANCELLATION(4, "labels.inputs.insurance.incident.final.guarantee.claim.cancellation"), //
    FINAL_REFINANCED_CANCELLATION(5, "labels.inputs.insurance.incident.final.refinanced.cancellation"), //
    BAD_SALE_CANCELLATION(6, "labels.inputs.insurance.incident.bad.sale.cancellation"), //
    PORTFOLIO_WRITE_OFF_CANCELLATION(7, "labels.inputs.insurance.incident.portfolio.write.off.cancellation"); //

    private final Integer value;
    private final String code;

    InsuranceIncidentType(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static List<EnumOptionData> getValuesAsEnumOptionDataList() {
        List<EnumOptionData> list = new ArrayList<>(
                Arrays.stream(values()).map(v -> new EnumOptionData((long) (v.getValue()), v.name(), v.getCode())).toList());
        // Remove FEE enum from the list as it is split into FEES, AVAL, MANDATORY_INSURANCE and VOLUNTARY_INSURANCE.
        list.removeIf(x -> x.getCode().equals(INVALID.name()));
        return list;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public static InsuranceIncidentType fromInt(final Integer value) {

        InsuranceIncidentType enumeration = InsuranceIncidentType.INVALID;
        switch (value) {
            case 1:
                enumeration = InsuranceIncidentType.DEFINITIVE_CANCELLATION_DEFAULT;
                break;
            case 2:
                enumeration = InsuranceIncidentType.DEFINITIVE_VOLUNTARY_CANCELLATION;
                break;
            case 3:
                enumeration = InsuranceIncidentType.FINAL_ADVANCE_PAYMENT_CANCELLATION;
                break;
            case 4:
                enumeration = InsuranceIncidentType.FINAL_GUARANTEE_CLAIM_CANCELLATION;
                break;
            case 5:
                enumeration = InsuranceIncidentType.FINAL_REFINANCED_CANCELLATION;
                break;
            case 6:
                enumeration = InsuranceIncidentType.BAD_SALE_CANCELLATION;
                break;
            case 7:
                enumeration = InsuranceIncidentType.PORTFOLIO_WRITE_OFF_CANCELLATION;
                break;
        }
        return enumeration;
    }
}
