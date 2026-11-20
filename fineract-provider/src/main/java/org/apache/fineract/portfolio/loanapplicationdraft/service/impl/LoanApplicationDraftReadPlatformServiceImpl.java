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
package org.apache.fineract.portfolio.loanapplicationdraft.service.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.portfolio.loanaccount.exception.LoanNotFoundException;
import org.apache.fineract.portfolio.loanapplicationdraft.data.LoanApplicationDraftData;
import org.apache.fineract.portfolio.loanapplicationdraft.domain.LoanApplicationDraftStatus;
import org.apache.fineract.portfolio.loanapplicationdraft.service.LoanApplicationDraftReadPlatformService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanApplicationDraftReadPlatformServiceImpl implements LoanApplicationDraftReadPlatformService {

    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<LoanApplicationDraftData> retrieveAll() {
        final LoanApplicationDraftMapper rm = new LoanApplicationDraftMapper(sqlGenerator);

        final StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select ");
        sqlBuilder.append(rm.loanSchema());

        return this.jdbcTemplate.query(sqlBuilder.toString(), rm);
    }

    @Override
    public List<LoanApplicationDraftData> retrieveAllActive(Long clientId) {
        final LoanApplicationDraftMapper rm = new LoanApplicationDraftMapper(sqlGenerator);

        final StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select ");
        sqlBuilder.append(rm.loanSchema());
        sqlBuilder.append(" where lad.status_enum not in (?) and lad.client_id = ?");

        return this.jdbcTemplate.query(sqlBuilder.toString(), rm, LoanApplicationDraftStatus.DELETED.getValue(), clientId);
    }

    @Override
    public LoanApplicationDraftData retrieveById(Long id) {

        try {

            final LoanApplicationDraftMapper rm = new LoanApplicationDraftMapper(sqlGenerator);

            final StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("select ");
            sqlBuilder.append(rm.loanSchema());
            sqlBuilder.append("where lad.id=?");

            return this.jdbcTemplate.queryForObject(sqlBuilder.toString(), rm, id);

        } catch (final EmptyResultDataAccessException e) {
            throw new LoanNotFoundException(id, e);
        }
    }

    private static final class LoanApplicationDraftMapper implements RowMapper<LoanApplicationDraftData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;

        LoanApplicationDraftMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String loanSchema() {

            return "lad.id as id, lad.client_id as clientId, pl.id as loanProductId, pl.name as loanProductName, "
                    + "lad.status_enum as status, lad.payload_json as payloadJson, lad.current_step as currentStep "
                    + "from m_loan_application_draft lad " + "left join m_product_loan pl ON lad.loan_product_id = pl.id ";
        }

        @Override
        public LoanApplicationDraftData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final Long clientId = rs.getLong("clientId");
            final Long loanProductId = rs.getLong("loanProductId");
            final String loanProductName = rs.getString("loanProductName");
            final Integer status = rs.getInt("status");
            final String payloadJson = rs.getString("payloadJson");
            final String currentStep = rs.getString("currentStep");

            return new LoanApplicationDraftData(id, clientId, loanProductId, loanProductName, status, currentStep, payloadJson);

        }
    }

}
