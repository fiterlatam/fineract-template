package org.apache.fineract.infrastructure.clientblockingreasons.domain;

import lombok.Getter;

@Getter
public enum BlockingReasonSettingEnum {

    CREDIT_CANCELADO("CANCELADO"), //
    CLIENTE_INACTIVIDAD("INACTIVIDAD"), //
    CREDIT_MORA("MORA"), //
    CLIENT_MORA("MORA"), //
    CREDIT_RECLAMADO_A_AVALADORA("RECLAMADO A AVALADORA"), //
    CREDIT_RESTRUCTURE("RESTRUCTURADO"), //
    CREDIT_ANULADO("ANULADO"), //
    CLIENT_ANULADO("ANULADO"); //

    final String databaseString;

    BlockingReasonSettingEnum(String databaseString) {
        this.databaseString = databaseString;
    }

}
