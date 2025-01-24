package org.apache.fineract.custom.infrastructure.dataqueries.data;

import java.sql.Timestamp;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ValidacionContactaDatatableData {

    private Long loanId; // loan_id
    private Long usuarioAsignadoCdUsuarioAsignado; // "Usuario Asignado_cd_Usuario Asignado"
    private String usuarioAsignado;
    private Long correoUsuarioAsignadoCdCorreoUsuarioAsignado; // "Correo Usuario Asignado_cd_Correo Usuario Asignado"
    private String correoUsuarioAsignado; // "Correo Usuario Asignado_cd_Correo Usuario Asignado"
    private LocalDate fechaInicioContactabilidad; // fecha_inicio_contactabilidad
    private Long validacionContactabilidadCdValidacionContactabilidad; // "Validacion Contactabilidad_cd_Validacion
    private Long causalRechazoContactabilidadCdCausalRechazoContactabilidad; // "Causal Rechazo
    private String contactabilidadFallida; // contactabilidad_fallida
    private String observacionContactabilidad; // observacion_contactabilidad
    private String contactabilidadObserv2; // contactabilidad_observ2
    private String contactabilidadObserv3; // contactabilidad_observ3
    private String telefonoDeContacto; // telefono_de_contacto
    private LocalDate fechaFinContactabilidad; // fecha_fin_contactabilidad
    private Timestamp createdAt; // created_at
    private Timestamp updatedAt; // updated_at
}
