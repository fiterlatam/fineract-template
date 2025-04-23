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
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientEnumerations;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.poi.util.StringUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanDisbursementUpdateLoanOfficerCodeEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final JdbcTemplate jdbcTemplate;

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
            return generateSuccessResponse(CommandProcessingResult.fromCommandProcessingResult(successResult));
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("squid:S3776")
    public Map<String, Object> generateSuccessResponse(CommandProcessingResult successResult) {
        Map<String, Object> response = new HashMap<>();
        Long loanId = successResult.getLoanId();

        // Just send the message if loan is active and not in mora
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

        if (Boolean.FALSE.equals(loan.getStatus().isActive())) {
            Long overDueInstallments = loan.getRepaymentScheduleInstallments().stream()
                    .filter(dd -> dd.getDueDate().isBefore(DateUtils.getLocalDateOfTenant())).filter(om -> om.isObligationsMet()).count();

            if (overDueInstallments.compareTo(0L) > 0) {
                return response;
            }
        }

        String promoterCode = null;
        String promoterCodeOriginal = null;
        String sql = """
                select ml.id AS loanId, ian.codigo_promotor as promoterCode, ian.codigo_promotor_original as promoterCodeOriginal
                FROM m_loan ml
                LEFT JOIN "Informacion Adicional" ian ON ian.loan_id = ml.id
                where ml.id = ?
                """;
        SqlRowSet rs = this.jdbcTemplate.queryForRowSet(sql, loanId);
        while (rs.next()) {
            promoterCode = rs.getString("promoterCode");
            promoterCodeOriginal = rs.getString("promoterCodeOriginal");
        }

        // Checking promoter code is not EMPTY
        if (StringUtil.isNotBlank(promoterCode)) {

            response.put("promoterCode", promoterCode); // loan data table - Informacion Adicional
            if (promoterCodeOriginal != null) {
                response.put("promoterCodeOriginal", promoterCodeOriginal); // loan data table - Informacion Adicional
            }

            Client client = loan.getClient();
            String name = client.getFirstname();
            String surName = client.getLastname();
            String email = client.getEmailAddress();
            String phoneNumber = client.getMobileNo();
            String clientType = ClientEnumerations.legalForm(client.getLegalForm()).getValue();
            response.put("name", name);
            response.put("surName", surName);
            response.put("clientType", clientType);
            if (email != null) {
                response.put("email", email);
            }
            if (phoneNumber != null) {
                response.put("phone", phoneNumber);
            }

            String document = null;
            String documentType = null;
            String city = null;
            String address = null;
            sql = """
                    select mc.id AS clientId, ccp."Cedula" AS document,
                    cv.code_value as documentType,\s
                    cvc.code_value as city,
                    ccp."Direccion" as address
                    FROM m_client mc
                    LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                    LEFT JOIN m_code_value cv ON cv.id = ccp."Customer Identifier_cd_Tipo identificacion"
                    LEFT JOIN m_code_value cvc ON cvc.id = ccp."Ciudad_cd_Ciudad"
                    where mc.id = ?
                    """;
            rs = this.jdbcTemplate.queryForRowSet(sql, client.getId());
            while (rs.next()) {
                document = rs.getString("document");
                documentType = rs.getString("documentType");
                city = rs.getString("city");
                address = rs.getString("address");
            }
            if (document != null) {
                response.put("documentClient", document); // client data table - campos_cliente_persona
            }
            if (documentType != null) {
                response.put("documentType", documentType); // client data table - campos_cliente_persona
            }
            if (city != null) {
                response.put("city", city); // client data table - campos_cliente_persona
            }
            if (address != null) {
                response.put("address", address); // client data table - campos_cliente_persona
            }
            return response;
        }
        return response;
    }
}
