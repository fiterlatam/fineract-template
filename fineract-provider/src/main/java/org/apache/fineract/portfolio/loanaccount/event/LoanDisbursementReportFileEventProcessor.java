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
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.event.BaseCustomWebhookEventProcessorImpl;
import org.apache.fineract.custom.infrastructure.dataqueries.data.DetalleGarantiaDatatableData;
import org.apache.fineract.custom.infrastructure.dataqueries.data.DetallesDeLaTransacionDatatableData;
import org.apache.fineract.custom.infrastructure.dataqueries.data.InformacionAdicionalDatatableData;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanDisbursementReportFileEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    private final LoanDisbursementReportEventProcessor loanDisbursementReportEventProcessor;
    private final JdbcTemplate jdbcTemplate;
    private final LoanRejectionGuaranteeEventProcessor loanRejectionGuaranteeEventProcessor;
    private final LoanRepositoryWrapper loanRepositoryWrapper;

    @Override
    protected String hookName() {
        return CustomHookEventProcessorEnum.fromClazz(this.getClass().getName()).getHookName();
    }

    @Override
    protected List<Map<String, String>> getSupportedEvents() {
        Map<String, String> loanEvent = Map.of("entityName", "detalles_de_la_transaccion", "actionName", "CREATE");
        return Collections.singletonList(loanEvent);
    }

    @Override
    public Map<String, Object> transform(String entityName, String actionName, JsonCommand command, Object result) {
        if (result instanceof CommandProcessingResult successResult) {
            return generateSuccessResponse(CommandProcessingResult.fromCommandProcessingResult(successResult), true);
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

        DetalleGarantiaDatatableData detalleGarantiaDatatableData = loanRejectionGuaranteeEventProcessor.getDetalleGarantia(loan);
        InformacionAdicionalDatatableData informationTableData = getInformacionAdicional(loan);

        requestBody.put("externalId", client.getExternalId().getValue());
        requestBody.put("city", informationTableData.getCiudadCliente() == null ? "" : informationTableData.getCiudadCliente());
        requestBody.put("region",
                informationTableData.getDepartamentoCliente() == null ? "" : informationTableData.getDepartamentoCliente());
        requestBody.put("promoterCode", informationTableData.getCodigoPromotor() == null ? "" : informationTableData.getCodigoPromotor());
        requestBody.put("loanId", loan.getAccountNumber());
        requestBody.put("productName", loan.getLoanProduct().getName());
        requestBody.put("applyGuarantee", detalleGarantiaDatatableData.isAplicaGarantia() ? "Si" : "No");
        requestBody.put("guaranteeType",
                detalleGarantiaDatatableData.getTipoGarantia() == null ? "" : detalleGarantiaDatatableData.getTipoGarantia());
        requestBody.put("guaranteePercentage",
                detalleGarantiaDatatableData.getPctComission() == null ? "" : detalleGarantiaDatatableData.getPctComission().toString());
        requestBody.put("transactionId", disbursalTransaction.getId());

        List<LoanTransactionData.DisbursementFeeData> disbursementFees = getDisbursementFees(loan.getId());
        BigDecimal netDisbursalAmount = disbursalTransaction.getAmount();
        if (!disbursementFees.isEmpty()) {
            LoanTransactionData.DisbursementFeeData data = disbursementFees.get(0);
            netDisbursalAmount = data.getNetDisbursalAmount();
        }
        requestBody.put("transactionAmount", netDisbursalAmount);

        DetallesDeLaTransacionDatatableData transactionData = getDetallesDeLaTransacion(disbursalTransaction);
        requestBody.put("commercialEstablishmentId", transactionData.getIdentificacionEstablecimientoComercial());

        return requestBody;
    }

    private List<LoanTransactionData.DisbursementFeeData> getDisbursementFees(Long loanId) {
        final String sql = """
                SELECT
                	mc.id AS "chargeId",
                	mlc.amount AS "amount",
                	mc.name AS "chargeName",
                	ml.net_disbursal_amount AS "netDisbursalAmount"
                FROM m_loan_transaction mlt
                INNER JOIN m_loan ml ON ml.id = mlt.loan_id
                INNER JOIN m_loan_charge_paid_by mlcpb ON mlcpb.loan_transaction_id = mlt.id
                INNER JOIN m_loan_charge mlc ON mlc.id = mlcpb.loan_charge_id
                INNER JOIN m_charge mc ON mc.id = mlc.charge_id
                WHERE mlt.loan_id = ? AND mlt.is_reversed = FALSE AND mlt.transaction_type_enum = 5
                """;
        final List<LoanTransactionData.DisbursementFeeData> disbursementFees = jdbcTemplate.query(sql, resultSet -> {
            List<LoanTransactionData.DisbursementFeeData> disbursementFeeDataList = new ArrayList<>();
            while (resultSet.next()) {
                final Long chargeId = resultSet.getLong("chargeId");
                final BigDecimal amount = resultSet.getBigDecimal("amount");
                final String chargeName = resultSet.getString("chargeName");
                final BigDecimal netPrincipalDisbursalAmount = resultSet.getBigDecimal("netDisbursalAmount");
                final LoanTransactionData.DisbursementFeeData disbursementFeeData = LoanTransactionData.DisbursementFeeData.builder()
                        .chargeId(chargeId).amount(amount).chargeName(chargeName).netDisbursalAmount(netPrincipalDisbursalAmount).build();
                disbursementFeeDataList.add(disbursementFeeData);
            }
            return disbursementFeeDataList;
        }, loanId);

        return disbursementFees;
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
