package org.apache.fineract.portfolio.loanaccount.invoice.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoanDocumentConcept {

    INT_CORRIENTE("INT", "INTERÉS DE FINANCIACION"), //
    INT_DE_MORA("IPM", "INTERÉS POR MORA"), //
    SEGURO_OBLIGATORIO("SEGU", "SEGURO"), //
    HONORARIOS("HON", "HONORARIOS"), //
    SEGUROS_VOLUNTARIOS("SEGV", "SEGURO VOLUNTARIO");

    private final String sku;
    private final String name;
}
