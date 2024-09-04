package org.apache.fineract.portfolio.insurance.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public enum InsuranceIncidentType {

    INVALID(0, "insurance.incident.invalid", "invalid"), //
    DEFINITIVE_CANCELLATION_DEFAULT(1, "labels.inputs.insurance.incident.definitive.default", "Cancelación definitiva por mora"), //
    DEFINITIVE_VOLUNTARY_CANCELLATION(2, "labels.inputs.insurance.incident.definitive.voluntary.cancellation",
            "Cancelación voluntaria definitiva"), //
    DEFINITIVE_FINAL_CANCELLATION(3, "labels.inputs.insurance.incident.final.advance.payment.cancellation",
            "Cancelación definitiva por cancelación del crédito"), //
    FINAL_GUARANTEE_CLAIM_CANCELLATION(4, "labels.inputs.insurance.incident.final.guarantee.claim.cancellation",
            "Cancelación definitiva por reclamación avaladora"), //
    FINAL_REFINANCED_CANCELLATION(5, "labels.inputs.insurance.incident.final.refinanced.cancellation",
            "Cancelación definitiva por rediferido/refinanciado"), //
    BAD_SALE_CANCELLATION(6, "labels.inputs.insurance.incident.bad.sale.cancellation", "Cancelación por mala venta"), //
    PORTFOLIO_WRITE_OFF_CANCELLATION(7, "labels.inputs.insurance.incident.portfolio.write.off.cancellation",
            "Cancelación por castigo de cartera"); //

    private final Integer value;
    private final String code;
    private final String readableName;

    InsuranceIncidentType(final Integer value, final String code, final String readableName) {
        this.value = value;
        this.code = code;
        this.readableName = readableName;
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
                enumeration = InsuranceIncidentType.DEFINITIVE_FINAL_CANCELLATION;
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
