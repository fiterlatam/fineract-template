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
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanCloseAsRescheduledEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final JdbcTemplate jdbcTemplate;
    private final ReadWriteNonCoreDataService readWriteNonCoreDataService;

    @Override
    protected List<Map<String, String>> getSupportedEvents() {
        Map<String, String> loanCloseEvent = Map.of("entityName", "LOAN", "actionName", "CLOSEASRESCHEDULED");
        return Collections.singletonList(loanCloseEvent);
    }

    @Override
    public Map<String, Object> transform(String entityName, String actionName, JsonCommand command, Object result) {
        if (result instanceof CommandProcessingResult successResult) {
            return generateSuccessResponse(CommandProcessingResult.fromCommandProcessingResult(successResult));
        }
        return Collections.emptyMap();
    }

    public Map<String, Object> generateSuccessResponse(CommandProcessingResult successResult) {
        Map<String, Object> response = new HashMap<>();
        Long loanId = successResult.getLoanId();
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
        Client client = loan.getClient();
        String firstName = client.getFirstname();
        String lastName = client.getLastname();
        String email = client.getEmailAddress();
        String phoneNumber = client.getMobileNo();
        ExternalId externalId = client.getExternalId();
        Long customerId = client.getId();
        String productName = loan.getLoanProduct().getName();

        response.put("loanId", loan.getAccountNumber());
        response.put("customerId", customerId);
        response.put("firstName", firstName);
        response.put("lastName", lastName);
        if (email != null) {
            response.put("email", email);
        }
        if (phoneNumber != null) {
            response.put("phoneNumber", phoneNumber);
        }
        if (externalId.getValue() != null) {
            response.put("externalId", externalId.getValue());
        }
        response.put("productName", productName);

        String document = null;
        String documentType = null;
        String sql = """
                select mc.id AS clientId, ccp."Cedula" AS document, cv.code_value as documentType
                FROM m_client mc
                LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                LEFT JOIN m_code_value cv ON cv.id = ccp."Customer Identifier_cd_Tipo identificacion"
                where mc.id = ?
                """;
        SqlRowSet rs = this.jdbcTemplate.queryForRowSet(sql, client.getId());
        while (rs.next()) {
            document = rs.getString("document");
            documentType = rs.getString("documentType");
        }
        if (document != null) {
            response.put("documentId", document); // client data table - campos_cliente_persona
        }
        if (documentType != null) {
            response.put("documentType", documentType); // client data table - campos_cliente_persona
        }

        String monthlyIncome = null;
        String assets = null;
        sql = """
                select ml.id AS loanId, ifn.ingresos as monthlyIncome, ifn.activos as assets
                FROM m_loan ml
                LEFT JOIN "Informacion Financiera" ifn ON ifn.loan_id = ml.id
                where ml.id = ?
                """;
        rs = this.jdbcTemplate.queryForRowSet(sql, loanId);
        while (rs.next()) {
            monthlyIncome = rs.getString("monthlyIncome");
            assets = rs.getString("assets");
        }
        if (monthlyIncome != null) {
            response.put("monthlyIncome", monthlyIncome); // loan data table - Informacion Financiera
        }
        if (assets != null) {
            response.put("assets", assets); // loan data table - Informacion Financiera
        }

        String promoterCode = null;
        String promoterCodeOriginal = null;
        sql = """
                select ml.id AS loanId, ian.codigo_promotor as promoterCode, ian.codigo_promotor_original as promoterCodeOriginal
                FROM m_loan ml
                LEFT JOIN "Informacion Adicional" ian ON ian.loan_id = ml.id
                where ml.id = ?
                """;
        rs = this.jdbcTemplate.queryForRowSet(sql, loanId);
        while (rs.next()) {
            promoterCode = rs.getString("promoterCode");
            promoterCodeOriginal = rs.getString("promoterCodeOriginal");
        }
        if (promoterCode != null) {
            response.put("promoterCode", promoterCode); // loan data table - Informacion Adicional
        }
        if (promoterCodeOriginal != null) {
            response.put("promoterCodeOriginal", promoterCodeOriginal); // loan data table - Informacion Adicional
        }
        return response;
    }
}
