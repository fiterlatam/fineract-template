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
import java.sql.Timestamp;
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
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanDisbursementGuaranteeEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    private final JdbcTemplate jdbcTemplate;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanReadPlatformService loanReadPlatformService;

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

    public Map<String, Object> generateSuccessResponse(CommandProcessingResult result) {
        Map<String, Object> requestBody = new HashMap<>();
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(result.getLoanId());

        String query = """
                SELECT *, fn_core_codevalue_getdescription("Tipo Garantía_cd_Tipo Garantía") AS tipo_garantia
                FROM "Detalle garantia"
                WHERE loan_id = ?
                """;

        try {
            DetalleGarantiaDatatableData detalleGaranta = this.jdbcTemplate.queryForObject(query,
                    new RowMapper<DetalleGarantiaDatatableData>() {

                        @Override
                        public DetalleGarantiaDatatableData mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return DetalleGarantiaDatatableData.builder().aplicaGarantia(rs.getBoolean("aplica_garantia"))
                                    .fechaRegistroGarantia(rs.getObject("fecha_registro_garantia", Timestamp.class))
                                    .numeroGarantia(rs.getString("numero_garantia")).numeroPagare(rs.getString("numero_pagare"))
                                    .tipoGarantia(rs.getString("tipo_garantia"))
                                    .tipoGarantiaId(rs.getLong("Tipo Garantía_cd_Tipo Garantía")).build();
                        }
                    }, result.getLoanId());

            if (Objects.nonNull(detalleGaranta) && detalleGaranta.isAplicaGarantia()
                    && Objects.isNull(detalleGaranta.getFechaRegistroGarantia())) {

                BigDecimal loanAmount = loan.getLoanTransactions().stream().filter(type -> type.getTypeOf().isDisbursement())
                        .max(Comparator.comparing(dt -> dt.getCreatedDateTime())).map(p -> p.getAmount()).orElse(BigDecimal.ZERO);

                requestBody.put("loanId", loan.getAccountNumber());
                requestBody.put("loanAmount", loanAmount);
                requestBody.put("guaranteeNumber", detalleGaranta.getNumeroGarantia());
                requestBody.put("promissoryNote", detalleGaranta.getNumeroPagare());
                requestBody.put("guaranteeType", detalleGaranta.getTipoGarantia());
                requestBody.put("guaranteeTypeId", detalleGaranta.getTipoGarantiaId());

                LoanAccountData loanAccountData = loanReadPlatformService.retrieveOne(result.getLoanId());
                requestBody.put("interestRate", loanAccountData.getAnnualInterestRate());
                requestBody.put("productName", loan.getLoanProduct().getName());
            }
        } catch (EmptyResultDataAccessException e) {
            return requestBody;
        }

        return requestBody;
    }
}
