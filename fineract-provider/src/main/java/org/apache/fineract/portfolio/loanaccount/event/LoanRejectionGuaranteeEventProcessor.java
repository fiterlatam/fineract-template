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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.event.BaseCustomWebhookEventProcessorImpl;
import org.apache.fineract.custom.infrastructure.dataqueries.data.CamposClienteGenericDatatableData;
import org.apache.fineract.custom.infrastructure.dataqueries.data.DetalleGarantiaDatatableData;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanRejectionGuaranteeEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    public static final String LOAN_ID_PARAM = "loanId";
    public static final String EXTERNAL_ID_PARAM = "externalId";
    public static final String GUARANTEE_NUMBER_PARAM = "idGuaranteeFNG";
    public static final String GUARANTEE_TYPE_PARAM = "typeGuarantee";
    public static final String DOCUMENT_ID_PARAM = "documentId";
    private final JdbcTemplate jdbcTemplate;
    private final ClientReadPlatformService clientReadPlatformService;
    private final LoanApprovalContactabilityEventProcessor loanApprovalContactabilityEventProcessor;
    private final LoanRepositoryWrapper loanRepositoryWrapper;

    @Override
    protected String hookName() {
        return CustomHookEventProcessorEnum.fromClazz(this.getClass().getName()).getHookName();
    }

    @Override
    protected List<Map<String, String>> getSupportedEvents() {
        Map<String, String> loanEvent = Map.of("entityName", "LOAN", "actionName", "REJECT");
        return Collections.singletonList(loanEvent);
    }

    @Override
    public Map<String, Object> transform(String entityName, String actionName, JsonCommand command, Object result) {
        if (result instanceof CommandProcessingResult successResult) {
            return generateSuccessResponse(CommandProcessingResult.fromCommandProcessingResult(successResult));
        }
        return Collections.emptyMap();
    }

    public Map<String, Object> generateSuccessResponse(CommandProcessingResult result) {
        Map<String, Object> requestBody = new HashMap<>();
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(result.getLoanId(), true);

        if (Boolean.FALSE.equals(loan.getLoanProduct().getName().contains(LoanProductType.CREDITO_ROTATIVO.getCode()))
                && Boolean.FALSE.equals(loan.getLoanProduct().getName().contains(LoanProductType.NANO_CREDITO.getCode()))) {

            // Check if client is Persona o Empresa
            ClientData clientData = clientReadPlatformService.retrieveOne(result.getClientId());
            EnumOptionData legalFormEnum = clientData.getLegalForm();

            // Get Campos_Cliente_Empresa and Campos_Cliente_Persona for check
            CamposClienteGenericDatatableData camposClienteEmpresaYPersona = loanApprovalContactabilityEventProcessor
                    .getCamposClienteEmpresaYPersona(result, legalFormEnum);

            // Guarantee data
            DetalleGarantiaDatatableData detalleGaranta = getDetalleGarantia(loan);

            // Create response object
            getRequestBody(result, requestBody, loan, camposClienteEmpresaYPersona, detalleGaranta);
        }

        return requestBody;
    }

    private void getRequestBody(CommandProcessingResult result, Map<String, Object> requestBody, Loan loan,
            CamposClienteGenericDatatableData camposClienteEmpresaYPersona, DetalleGarantiaDatatableData detalleGaranta) {
        requestBody.put(LOAN_ID_PARAM, loan.getAccountNumber());
        if (Objects.nonNull(loan.getClient())) {
            requestBody.put(EXTERNAL_ID_PARAM, loan.getClient().getExternalId());
        }

        if (Objects.nonNull(detalleGaranta)) {
            requestBody.put(GUARANTEE_NUMBER_PARAM, detalleGaranta.getNumeroGarantia());
            requestBody.put(GUARANTEE_TYPE_PARAM, detalleGaranta.getTipoGarantia());
        }

        Optional.ofNullable(camposClienteEmpresaYPersona)
                .filter(c -> detalleGaranta != null && detalleGaranta.getNumeroPagare() != null)
                .ifPresent(c -> requestBody.put(DOCUMENT_ID_PARAM, detalleGaranta.getNumeroPagare()));
    }

    private DetalleGarantiaDatatableData getDetalleGarantia(Loan loan) {
        DetalleGarantiaDatatableData validacionContactaData = DetalleGarantiaDatatableData.builder().build();

        try {
            String query = """
                    SELECT *, fn_core_codevalue_getdescription("Tipo Garantía_cd_Tipo Garantía") AS tipo_garantia
                    FROM "Detalle garantia"
                    WHERE loan_id = ?
                    """;

            validacionContactaData = this.jdbcTemplate.queryForObject(query, new RowMapper<DetalleGarantiaDatatableData>() {

                @Override
                public DetalleGarantiaDatatableData mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return DetalleGarantiaDatatableData.builder() //
                            .loanId(rs.getLong("loan_id")) //
                            .numeroGarantia(rs.getString("numero_garantia")) //
                            .tipoGarantia(rs.getString("tipo_garantia")) //
                            .numeroPagare(rs.getString("numero_pagare")) //
                            .build();
                }
            }, loan.getId());

        } catch (Exception e) {
            return validacionContactaData;
        }

        return validacionContactaData;
    }
}
