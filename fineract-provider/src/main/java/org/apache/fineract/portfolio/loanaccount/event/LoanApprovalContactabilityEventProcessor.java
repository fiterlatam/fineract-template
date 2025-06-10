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

package org.apache.fineract.portfolio.loanaccount.event;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.event.BaseCustomWebhookEventProcessorImpl;
import org.apache.fineract.custom.infrastructure.dataqueries.data.CamposClienteGenericDatatableData;
import org.apache.fineract.custom.infrastructure.dataqueries.data.ValidacionContactaDatatableData;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanApprovalContactabilityEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    public static final String STRING_CA = "CA";
    private final JdbcTemplate jdbcTemplate;
    private final LoanReadPlatformService loanReadPlatformService;
    private final ClientReadPlatformService clientReadPlatformService;
    private final LoanRepositoryWrapper loanRepositoryWrapper;

    @Override
    protected String hookName() {
        return CustomHookEventProcessorEnum.fromClazz(this.getClass().getName()).getHookName();
    }

    @Override
    protected List<Map<String, String>> getSupportedEvents() {
        Map<String, String> loanEvent = Map.of("entityName", "Validacion Contacta", "actionName", "CREATE");
        return Collections.singletonList(loanEvent);
    }

    public Map<String, Object> transform(String entityName, String actionName, JsonCommand command, Object result) {
        if (result instanceof CommandProcessingResult successResult) {
            return generateSuccessResponse(CommandProcessingResult.fromCommandProcessingResult(successResult));
        }
        return Collections.emptyMap();
    }

    public Map<String, Object> generateSuccessResponse(CommandProcessingResult result) {
        Map<String, Object> requestBody = new HashMap<>();
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(result.getLoanId(), true);

        if (Boolean.FALSE.equals(loan.isApproved()) || loan.isDisbursed()) {
            return Collections.emptyMap();
        }

        // Check if client is Persona o Empresa
        ClientData clientData = clientReadPlatformService.retrieveOne(result.getClientId());
        EnumOptionData legalFormEnum = clientData.getLegalForm();

        // Get Campos_Cliente_Empresa and Campos_Cliente_Persona for check
        CamposClienteGenericDatatableData camposClienteEmpresaYPersona = getCamposClienteEmpresaYPersona(result, legalFormEnum);

        // Get "Validacion Contacta" datatable data
        ValidacionContactaDatatableData validacionContactaData = getValidacionContacta(loan);

        // Check the business rules and set the responseBody
        if (validacionContactaData != null && camposClienteEmpresaYPersona != null && Objects.nonNull(validacionContactaData.getLoanId()) //
                && Objects.nonNull(camposClienteEmpresaYPersona.getClientId()) //
                && Objects.nonNull(validacionContactaData.getUsuarioAsignadoCdUsuarioAsignado()) //
                && validacionContactaData.getUsuarioAsignadoCdUsuarioAsignado().compareTo(0L) > 0 //
                && Objects.isNull(validacionContactaData.getFechaInicioContactabilidad()) //
                && Objects.nonNull(validacionContactaData.getCorreoUsuarioAsignadoCdCorreoUsuarioAsignado()) //
                && validacionContactaData.getCorreoUsuarioAsignadoCdCorreoUsuarioAsignado().compareTo(0L) > 0 //
                && (Objects.isNull(validacionContactaData.getValidacionContactabilidadCdValidacionContactabilidad()) //
                        || validacionContactaData.getValidacionContactabilidadCdValidacionContactabilidad() == 0)) { //

            requestBody.put("documentTypeId", camposClienteEmpresaYPersona.getTipoIdentificacionId());
            requestBody.put("documentType", camposClienteEmpresaYPersona.getTipoIdentificacion());
            requestBody.put("documentClient", camposClienteEmpresaYPersona.getNumeroIdentificacion());
            requestBody.put("name", clientData.getFirstname());
            requestBody.put("surNames", clientData.getLastname());
            requestBody.put("city", camposClienteEmpresaYPersona.getCiudad());
            requestBody.put("address", camposClienteEmpresaYPersona.getDireccion());
            requestBody.put("phone", clientData.getMobileNo());
            requestBody.put("email", clientData.getEmailAddress());
            requestBody.put("userAsignedId", validacionContactaData.getUsuarioAsignadoCdUsuarioAsignado());
            requestBody.put("userAsigned", validacionContactaData.getUsuarioAsignado());
            requestBody.put("emailUserAsignedId", validacionContactaData.getCorreoUsuarioAsignadoCdCorreoUsuarioAsignado());
            requestBody.put("emailUserAsigned", validacionContactaData.getCorreoUsuarioAsignado());
            requestBody.put("clientType", STRING_CA);
        }

        return requestBody;
    }

    private ValidacionContactaDatatableData getValidacionContacta(Loan loan) {
        ValidacionContactaDatatableData validacionContactaData = ValidacionContactaDatatableData.builder().build();

        try {
            // Get ValidacionContactaDatatableData data
            String query = """
                    SELECT *
                        , fn_core_codevalue_getdescription("Usuario Asignado_cd_Usuario Asignado")                          as "Usuario Asignado"
                        , fn_core_codevalue_getdescription("Correo Usuario Asignado_cd_Correo Usuario Asignado")            as "Correo Usuario Asignado"
                        , fn_core_codevalue_getdescription("Validacion Contactabilidad_cd_Validacion Contactabilidad")      as "Validacion Contactabilidad"
                        , fn_core_codevalue_getdescription("Causal Rechazo Contactabilidad_cd_Causal Rechazo Contactabilida") as "Causal Rechazo Contactabilida"
                    FROM "Validacion Contacta"
                    WHERE loan_id = ?
                    """;

            validacionContactaData = this.jdbcTemplate.queryForObject(query, new RowMapper<ValidacionContactaDatatableData>() {

                @Override
                public ValidacionContactaDatatableData mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return ValidacionContactaDatatableData.builder().loanId(rs.getLong("loan_id"))
                            .usuarioAsignadoCdUsuarioAsignado(rs.getLong("Usuario Asignado_cd_Usuario Asignado"))
                            .usuarioAsignado(rs.getString("Usuario Asignado"))
                            .fechaInicioContactabilidad(rs.getObject("fecha_inicio_contactabilidad", LocalDate.class))
                            .correoUsuarioAsignadoCdCorreoUsuarioAsignado(rs.getLong("Correo Usuario Asignado_cd_Correo Usuario Asignado"))
                            .correoUsuarioAsignado(rs.getString("Correo Usuario Asignado"))
                            .validacionContactabilidadCdValidacionContactabilidad(
                                    rs.getLong("Validacion Contactabilidad_cd_Validacion Contactabilidad"))
                            .build();
                }
            }, loan.getId());

        } catch (Exception e) {
            return validacionContactaData;
        }

        return validacionContactaData;
    }

    protected CamposClienteGenericDatatableData getCamposClienteEmpresaYPersona(CommandProcessingResult result,
            EnumOptionData legalFormEnum) {
        CamposClienteGenericDatatableData validacionCamposClienteEmpresaYPersona = CamposClienteGenericDatatableData.builder().build();

        try {

            String query;
            // if Persona, pick this values
            if (legalFormEnum.getValue().equalsIgnoreCase("Person")) {

                query = """
                            SElECT *
                                , "Cedula"                                                                              as "Numero identificacion"
                                , "Customer Identifier_cd_Tipo identificacion"                                          as "Tipo identificacion Id"
                                , fn_core_codevalue_getcodevalue("Customer Identifier_cd_Tipo identificacion")          as "Tipo identificacion"
                                , fn_core_codevalue_getcodevalue("Ciudad_cd_Ciudad")                                    as "Ciudad"
                            FROM "campos_cliente_persona"
                            WHERE client_id = ?
                        """;

            } else { // If empresa, pick this ones

                query = """
                            SElECT *
                                , "NIT"                                                                                 as "Numero identificacion"
                                , "Tipo ID_cd_Tipo ID"                                                                  as "Tipo identificacion Id"
                                , fn_core_codevalue_getcodevalue("Tipo ID_cd_Tipo ID")                                  as "Tipo identificacion"
                                , fn_core_codevalue_getcodevalue("Ciudad_cd_Ciudad")                                    as "Ciudad"
                            FROM "campos_cliente_empresas"
                           WHERE client_id = ?
                        """;

            }

            validacionCamposClienteEmpresaYPersona = this.jdbcTemplate.queryForObject(query,
                    new RowMapper<CamposClienteGenericDatatableData>() {

                        @Override
                        public CamposClienteGenericDatatableData mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return CamposClienteGenericDatatableData.builder().clientId(rs.getLong("client_id"))
                                    .tipoIdentificacionId(rs.getLong("Tipo identificacion Id"))
                                    .tipoIdentificacion(rs.getString("Tipo identificacion"))
                                    .numeroIdentificacion(rs.getString("Numero identificacion")).ciudadId(rs.getLong("Ciudad_cd_Ciudad"))
                                    .ciudad(rs.getString("Ciudad")).direccion(rs.getString("Direccion")).telefono(rs.getString("Telefono"))
                                    .build();
                        }
                    }, result.getClientId());

        } catch (Exception e) {
            return validacionCamposClienteEmpresaYPersona;
        }

        return validacionCamposClienteEmpresaYPersona;
    }

}
