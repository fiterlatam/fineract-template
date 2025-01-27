package org.apache.fineract.custom.infrastructure.dataqueries.data;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CamposClienteGenericDatatableData {

    private Long clientId;
    private Long tipoIdentificacionId;
    private String tipoIdentificacion;
    private String numeroIdentificacion;
    private String direccion;
    private String telefono;
    private Long ciudadId;
    private String ciudad;
}
