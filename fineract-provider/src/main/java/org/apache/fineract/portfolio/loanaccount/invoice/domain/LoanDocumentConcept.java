package org.apache.fineract.portfolio.loanaccount.invoice.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
public enum LoanDocumentConcept {

    INT_CORRIENTE("INT", "INTERÉS DE FINANCIACION"), //
    INT_DE_MORA("IPM", "INTERÉS POR MORA"), //
    SEGURO_OBLIGATORIO("SEGU", "SEGURO"), //
    HONORARIOS("HON", "HONORARIOS"), //
    SEGUROS_VOLUNTARIOS("SEGV", "SEGURO VOLUNTARIO");

    @Getter
    private final String sku;
    private final String name;

    public String getName() {
        return StringUtils.stripAccents(this.name);
    }
}
