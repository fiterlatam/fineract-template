package org.apache.fineract.infrastructure.clientblockingreasons.domain;

public enum BlockingReasonSettingEnum {

    CREDIT_CANCELADO("CANCELADO"), CLIENTE_INACTIVIDAD("INACTIVIDAD"), CREDIT_MORA("MORA"), CLIENT_MORA(
            "MORA"), CREDIT_RECLAMADO_A_AVALADORA("RECLAMADO A AVALADORA"), CREDIT_RESTRUCTURE("RESTRUCTURADO");

    final String databaseString;

    BlockingReasonSettingEnum(String databaseString) {
        this.databaseString = databaseString;
    }

    public String getDatabaseString() {
        return this.databaseString;
    }

}
