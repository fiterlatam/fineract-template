package org.apache.fineract.portfolio.charge.domain;

public enum ChargeInsuranceType {
    INVALID(0, "chargeInsuranceType.invalid"), //
    COMPRA(1, "chargeInsuranceType.compra"),
    CARGO(2, "chargeInsuranceType.cargo");

    private final Integer value;
    private final String code;

    ChargeInsuranceType(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public static ChargeInsuranceType fromInt(final Integer type) {
        ChargeInsuranceType chargeInsuranceType = ChargeInsuranceType.INVALID;
        if (type != null) {
            switch (type) {
                case 1:
                    chargeInsuranceType = ChargeInsuranceType.COMPRA;
                    break;
                case 2:
                    chargeInsuranceType = ChargeInsuranceType.CARGO;
                    break;
            }
        }
        return chargeInsuranceType;
    }

    public boolean isCompra() {
        return this.value.equals(ChargeInsuranceType.COMPRA.getValue());
    }

    public boolean isCargo() {
        return this.value.equals(ChargeInsuranceType.CARGO.getValue());
    }
}
