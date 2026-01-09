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
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.event.BaseCustomWebhookEventProcessorImpl;
import org.apache.fineract.custom.infrastructure.dataqueries.data.DetalleGarantiaDatatableData;
import org.apache.fineract.custom.infrastructure.dataqueries.data.InformacionAdicionalDatatableData;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
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

        requestBody.put("applyGuarantee", detalleGarantiaDatatableData.isAplicaGarantia());
        requestBody.put("transactionAmount", disbursalTransaction.getAmount());
        requestBody.put("loanId", loan.getAccountNumber());
        requestBody.put("productName", loan.getLoanProduct().getName());
        requestBody.put("transactionId", disbursalTransaction.getId());

        List<LoanTransactionData.DisbursementFeeData> disbursementFees = getDisbursementFees(loan.getId());
        requestBody.put("disbursementFees", disbursementFees);

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
                        .chargeId(chargeId).amount(amount).chargeName(chargeName).netDisbursalAmount(netPrincipalDisbursalAmount)
                        .build();
                disbursementFeeDataList.add(disbursementFeeData);
            }
            return disbursementFeeDataList;
        }, loanId);

        return disbursementFees;
    }
}
