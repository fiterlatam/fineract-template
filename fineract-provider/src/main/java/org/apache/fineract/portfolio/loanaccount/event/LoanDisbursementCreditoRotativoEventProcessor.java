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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.event.BaseCustomWebhookEventProcessorImpl;
import org.apache.fineract.custom.infrastructure.dataqueries.data.DetalleGarantiaDatatableData;
import org.apache.fineract.custom.infrastructure.dataqueries.data.InformacionAdicionalDatatableData;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanDisbursementCreditoRotativoEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    public static final String LOAN_ID_PARAM = "loanId";
    public static final String EMAIL_PARAM = "email";
    public static final String FIRST_NAME_PARAM = "firstName";
    public static final String FULL_NAME_PARAM = "fullName";
    public static final String MOBILE_PHONE_PARAM = "mobilePhone";
    public static final String INSTALLMENTS_PARAM = "installments";
    public static final String PRODUCT_NAME_PARAM = "productName";
    public static final String USER_ID_PARAM = "userId";
    public static final String LOAN_AMOUNT_PARAM = "loanAmount";
    public static final String PROMISSORY_NOTE_PARAM = "promissoryNote";

    private final JdbcTemplate jdbcTemplate;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final ClientReadPlatformService clientReadPlatformService;

    @Override
    protected String hookName() {
        return CustomHookEventProcessorEnum.fromClazz(this.getClass().getName()).getHookName();
    }

    @Override
    protected List<Map<String, String>> getSupportedEvents() {
        Map<String, String> loanEvent = Map.of("entityName", "Informacion Adicional", "actionName", "CREATE");
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

        if (Boolean.FALSE.equals(loan.isApproved()) || loan.isDisbursed()) {
            return Collections.emptyMap();
        }

        // Get "InformacionAdicional" datatable data
        InformacionAdicionalDatatableData informacionAdicionalData = getInformacionAdicionalDatatableData(loan);

        // Check the business rules and set the responseBody
        if (loan.containsRevolvingLoan() //
                && Objects.nonNull(informacionAdicionalData) //
                && Objects.nonNull(informacionAdicionalData.getValidacionManual()) //
                && Boolean.TRUE.equals(informacionAdicionalData.getValidacionManual()) //
                && (Objects.isNull(informacionAdicionalData.getNotificacionBienvenida()) //
                        || Boolean.FALSE.equals(informacionAdicionalData.getNotificacionBienvenida()))) {

            ClientData clientData = clientReadPlatformService.retrieveOne(result.getClientId());

            if (Objects.nonNull(loan.getClient().getExternalId())) {
                requestBody.put(USER_ID_PARAM, loan.getClient().getExternalId().getValue());
            }
            requestBody.put(LOAN_ID_PARAM, loan.getAccountNumber());
            requestBody.put(EMAIL_PARAM, clientData.getEmailAddress());
            requestBody.put(FIRST_NAME_PARAM, clientData.getFirstname());
            requestBody.put(FULL_NAME_PARAM, clientData.getDisplayName());
            requestBody.put(MOBILE_PHONE_PARAM, clientData.getMobileNo());
            requestBody.put(INSTALLMENTS_PARAM, loan.getLoanProduct().getNumberOfRepayments());
            requestBody.put(PRODUCT_NAME_PARAM, loan.getLoanProduct().getName());

            BigDecimal loanAmount = loan.getLoanTransactions().stream().filter(type -> type.getTypeOf().isDisbursement())
                    .max(Comparator.comparing(dt -> dt.getCreatedDateTime())).map(p -> p.getAmount()).orElse(BigDecimal.ZERO);

            if (loanAmount.compareTo(BigDecimal.ZERO) == 0) {
                loanAmount = loan.getPrincipal().getAmount();
            }

            requestBody.put(LOAN_AMOUNT_PARAM, loanAmount);

            // Get "DetalleGarantia" datatable data
            DetalleGarantiaDatatableData detalleGarantiaData = getDetalleGarantiaDatatableData(loan);
            requestBody.put(PROMISSORY_NOTE_PARAM, detalleGarantiaData.getNumeroPagare());
        }

        return requestBody;
    }

    protected InformacionAdicionalDatatableData getInformacionAdicionalDatatableData(Loan loan) {
        InformacionAdicionalDatatableData validacionContactaData = InformacionAdicionalDatatableData.builder().build();

        try {
            // Get ValidacionContactaDatatableData data
            String query = """
                    SELECT * FROM "Informacion Adicional" WHERE loan_id = ?
                    """;

            validacionContactaData = this.jdbcTemplate.queryForObject(query, new RowMapper<InformacionAdicionalDatatableData>() {

                @Override
                public InformacionAdicionalDatatableData mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return InformacionAdicionalDatatableData.builder() //
                            .loanId(rs.getLong("loan_id")) //
                            .validacionManual(rs.getBoolean("validacion_manual")) //
                            .notificacionBienvenida(rs.getBoolean("notificacion_bienvenida")) //
                            .montoDisponible(rs.getBoolean("monto_disponible")) //
                            .fechaPrimerUso(rs.getString("fecha_primer_uso")).build();
                }
            }, loan.getId());

        } catch (Exception e) {
            return validacionContactaData;
        }

        return validacionContactaData;
    }

    protected DetalleGarantiaDatatableData getDetalleGarantiaDatatableData(Loan loan) {
        DetalleGarantiaDatatableData detalleGarantiaData = DetalleGarantiaDatatableData.builder().build();

        try {
            // Get DetalleGarantiaDatatableData data
            String query = """
                    SELECT * FROM "Detalle garantia" WHERE loan_id = ?
                    """;

            detalleGarantiaData = this.jdbcTemplate.queryForObject(query, new RowMapper<DetalleGarantiaDatatableData>() {

                @Override
                public DetalleGarantiaDatatableData mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return DetalleGarantiaDatatableData.builder() //
                            .loanId(rs.getLong("loan_id")) //
                            .numeroPagare(rs.getString("numero_pagare")).build();
                }
            }, loan.getId());

        } catch (Exception e) {
            return detalleGarantiaData;
        }

        return detalleGarantiaData;
    }

}
