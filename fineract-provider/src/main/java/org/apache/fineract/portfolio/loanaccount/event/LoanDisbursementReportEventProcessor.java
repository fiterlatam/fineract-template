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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.event.BaseCustomWebhookEventProcessorImpl;
import org.apache.fineract.custom.infrastructure.dataqueries.data.DetalleGarantiaDatatableData;
import org.apache.fineract.custom.infrastructure.dataqueries.data.DetallesDeLaTransacionDatatableData;
import org.apache.fineract.custom.infrastructure.dataqueries.data.InformacionAdicionalDatatableData;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanDisbursementReportEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final ClientReadPlatformService clientReadPlatformService;
    private final LoanApprovalContactabilityEventProcessor loanApprovalContactabilityEventProcessor;
    private final JdbcTemplate jdbcTemplate;
    private final LoanRejectionGuaranteeEventProcessor loanRejectionGuaranteeEventProcessor;

    @Override
    protected String hookName() {
        return CustomHookEventProcessorEnum.fromClazz(this.getClass().getName()).getHookName();
    }

    @Override
    protected List<Map<String, String>> getSupportedEvents() {
        Map<String, String> loanEvent = Map.of("entityName", "LOAN", "actionName", "DISBURSE");
        return Collections.singletonList(loanEvent);
    }

    @Override
    public Map<String, Object> transform(String entityName, String actionName, JsonCommand command, Object result) {
        if (result instanceof CommandProcessingResult successResult) {
            return generateSuccessResponse(CommandProcessingResult.fromCommandProcessingResult(successResult), false);
        }
        return Collections.emptyMap();
    }

    public Map<String, Object> generateSuccessResponse(CommandProcessingResult result, Boolean useTransactionDetailsForEstablishment) {

        Map<String, Object> requestBody = new HashMap<>();
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(result.getLoanId());
        Client client = loan.client();

        // Get last disbursal transaction
        LoanTransaction disbursalTransaction = loan.getLoanTransactions().stream().filter(type -> type.getTypeOf().isDisbursement())
                .max(Comparator.comparing(dt -> dt.getCreatedDateTime())).get();

        InformacionAdicionalDatatableData informacionAdicional = getInformacionAdicional(loan);

        DetalleGarantiaDatatableData detalleGarantiaDatatableData = loanRejectionGuaranteeEventProcessor.getDetalleGarantia(loan);

        requestBody.put("loanId", loan.getAccountNumber());
        requestBody.put("productName", loan.getLoanProduct().getName());

        if (Objects.nonNull(client.getExternalId())) {
            requestBody.put("externalId", client.getExternalId().getValue());
        }

        if (Objects.nonNull(disbursalTransaction)) {
            requestBody.put("transactionId", disbursalTransaction.getId());
        }

        requestBody.put("city", informacionAdicional.getCiudadCliente());
        requestBody.put("region", informacionAdicional.getDepartamentoCliente());
        requestBody.put("promoterCode", informacionAdicional.getCodigoPromotor());

        requestBody.put("applyGuarantee", detalleGarantiaDatatableData.isAplicaGarantia());
        requestBody.put("guaranteeType", detalleGarantiaDatatableData.getTipoGarantia());
        requestBody.put("guaranteePercentage", detalleGarantiaDatatableData.getPctComission());

        if (Objects.nonNull(disbursalTransaction)) {
            requestBody.put("transactionAmount", disbursalTransaction.getAmount());
            requestBody.put("commercialEstablishmentId",
                    getDetallesDeLaTransacion(disbursalTransaction).getIdentificacionEstablecimientoComercial());
        }

        return requestBody;
    }

    private InformacionAdicionalDatatableData getInformacionAdicional(Loan loan) {
        InformacionAdicionalDatatableData result = InformacionAdicionalDatatableData.builder().build();

        try {
            String query = """
                    SELECT *
                    FROM "Informacion Adicional"
                    WHERE loan_id = ?
                    """;

            result = this.jdbcTemplate.queryForObject(query, new RowMapper<InformacionAdicionalDatatableData>() {

                @Override
                public InformacionAdicionalDatatableData mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return InformacionAdicionalDatatableData.builder().loanId(rs.getLong("loan_id"))
                            .ciudadCliente(rs.getString("ciudad_cliente")).departamentoCliente(rs.getString("departamento_cliente"))
                            .codigoPromotor(rs.getString("codigo_promotor"))
                            .numeroIdentificacionAliado(rs.getString("numero_identificacion_aliado")).build();
                }
            }, loan.getId());

        } catch (Exception e) {
            return result;
        }

        return result;
    }

    private DetallesDeLaTransacionDatatableData getDetallesDeLaTransacion(LoanTransaction loanTransaction) {
        DetallesDeLaTransacionDatatableData result = DetallesDeLaTransacionDatatableData.builder().build();

        try {
            String query = """
                    SELECT *
                    FROM detalles_de_la_transaccion
                    WHERE loan_transaction_id = ?
                    """;

            result = this.jdbcTemplate.queryForObject(query, new RowMapper<DetallesDeLaTransacionDatatableData>() {

                @Override
                public DetallesDeLaTransacionDatatableData mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return DetallesDeLaTransacionDatatableData.builder().loanId(loanTransaction.getLoan().getId())
                            .identificacionEstablecimientoComercial(rs.getString("id_establishment")).build();
                }
            }, loanTransaction.getId());

        } catch (Exception e) {
            return result;
        }

        return result;
    }
}
