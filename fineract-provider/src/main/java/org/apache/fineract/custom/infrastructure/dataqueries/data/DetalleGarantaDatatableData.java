package org.apache.fineract.custom.infrastructure.dataqueries.data;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DetalleGarantaDatatableData {

    private boolean aplicaGarantia;
    private Object fechaRegistroGarantia;
    private String numeroGarantia;
    private String numeroPagare;
    private String tipoGarantia;
    private Long tipoGarantiaId;
}
