package org.apache.fineract.infrastructure.clientblockingreasons.domain;

public enum BlockingReasonSettingEnum {

    CREDIT_CANCELADO("CANCELADO"), CLIENTE_INACTIVIDAD("INACTIVIDAD");

    final String databaseString;

    BlockingReasonSettingEnum(String databaseString) {
        this.databaseString = databaseString;
    }

    public String getDatabaseString() {
        return this.databaseString;
    }

}
