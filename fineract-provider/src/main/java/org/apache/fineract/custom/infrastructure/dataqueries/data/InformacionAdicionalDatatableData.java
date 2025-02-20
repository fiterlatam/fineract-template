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

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class InformacionAdicionalDatatableData {

    private Long loanId; // loan_id
    private String codigoPromotor; // codigo_promotor
    private String nombreAliado; // nombre_aliado
    private String numeroIdentificacionAliado; // numero_identificacion_aliado
    private String ciudadAliado; // ciudad_aliado
    private String verificacionRiesgo; // verificacion_riesgo
    private String tipoIdentificacion; // tipo_identificacion
    private String numeroIdentificacion; // numero_identificacion
    private String diasMoraIniMes; // dias_mora_ini_mes
    private String fechaRegistroDiasMoraIniMes; // fecha_registro_dias_mora_ini_mes
    private Boolean validacionManual; // validacion_manual
    private String fechaPrimerUso; // fecha_primer_uso
    private String codigoPromotorOriginal; // codigo_promotor_original
    private String nombrePromotor; // nombre_promotor
    private String ciudadCliente; // ciudad_cliente
    private Boolean notificacionBienvenida; // notificacion_bienvenida
    private String departamentoCliente; // departamento_cliente
    private Boolean montoDisponible; // monto_disponible
    private String fullnameReferer; // fullname_referer
    private String modeloExterno; // modelo_externo
    private String createdAt; // created_at
    private String updatedAt; // updated_at

}
