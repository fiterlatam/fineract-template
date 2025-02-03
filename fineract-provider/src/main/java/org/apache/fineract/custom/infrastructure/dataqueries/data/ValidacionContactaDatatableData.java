/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
