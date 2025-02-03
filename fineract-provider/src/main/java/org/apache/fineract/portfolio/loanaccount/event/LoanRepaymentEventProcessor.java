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
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.event.BaseCustomWebhookEventProcessorImpl;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component()
@Slf4j
@RequiredArgsConstructor
public class LoanRepaymentEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    protected List<Map<String, String>> getSupportedEvents() {
        Map<String, String> loanEvent = Map.of("entityName", "LOAN", "actionName", "REPAYMENT");
        return Collections.singletonList(loanEvent);
    }

    @Override
    public Map<String, Object> transform(String entityName, String actionName, JsonCommand command, Object result) {

        if (result instanceof CommandProcessingResult successResult) {
            return generateSuccessResponse(command, CommandProcessingResult.fromCommandProcessingResult(successResult));
        }
        return Collections.emptyMap();
    }

    public Map<String, Object> generateSuccessResponse(JsonCommand command, CommandProcessingResult successResult) {
        Map<String, Object> response = new HashMap<>();
        Long loanId = successResult.getLoanId();
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
        Client client = loan.getClient();
        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
        BigDecimal principalAmount = loan.getApprovedPrincipal();
        BigDecimal principalOutstanding = loan.getLoanSummary().getTotalPrincipalOutstanding();
        BigDecimal loanBalance = loan.getLoanSummary().getTotalOutstanding();

        String clientName = client.getDisplayName();
        ExternalId externalId = client.getExternalId();
        if (externalId != null) {
            response.put("externalId", externalId.getValue());
        }
        String mobileNumber = client.mobileNo();
        int daysInArrears = 0;
        String query = """
                SELECT COALESCE(current_date - overdue_since_date_derived, 0) AS aging_days
                FROM m_loan_arrears_aging
                WHERE loan_id = ?
                """;

        try {
            daysInArrears = this.jdbcTemplate.queryForObject(query, Integer.class, loan.getId());
        } catch (EmptyResultDataAccessException e) {
            // Loan is not in arrears, default value is already set
            log.warn("No arrears data found for loan ID: {}", loan.getId());
        }
        Map<String, Object> clientDocument = getClientById(client.getId());
        if (!clientDocument.isEmpty()) {
            response.put("document", clientDocument.get("document"));
            response.put("documentType", clientDocument.get("documentType"));
        }

        response.put("loanId", loanId);
        response.put("amount", transactionAmount);
        response.put("arrearDue", daysInArrears);
        response.put("paymentDate", transactionDate);

        response.put("fullname", clientName);
        response.put("loanAmount", principalAmount);
        response.put("mobilePhone", mobileNumber);
        response.put("principalBalance", principalOutstanding);
        response.put("totalDue", loanBalance);

        return response;
    }

    public Map<String, Object> getClientById(Long clientId) {
        try {
            String sql = """
                    SELECT mc.id AS clientId,
                           ccp."Cedula" AS document,
                           cv.code_value AS documentType
                    FROM m_client mc
                    LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                    LEFT JOIN m_code_value cv ON cv.id = ccp."Customer Identifier_cd_Tipo identificacion"
                    WHERE mc.id = ?
                    """;

            return jdbcTemplate.queryForMap(sql, clientId);
        } catch (EmptyResultDataAccessException e) {
            log.error("Client not found with ID: {}", clientId);
            return Collections.emptyMap();
        }
    }
}
