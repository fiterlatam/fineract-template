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

package org.apache.fineract.portfolio.loanaccount.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.clientblockingreasons.data.BlockingReasonsData;
import org.apache.fineract.portfolio.loanaccount.data.LoanBlockingReasonData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanBlockReadPlatformServiceImpl implements LoanBlockReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    private static class LoanBlockingReasonMapper implements RowMapper<LoanBlockingReasonData> {

        public String schema() {
            return """
                    mcbr.id  as id,
                          mcbr."comment" as comment,
                          mcbr.loan_id as loanId,
                          mcbr.is_active  as isActive,
                          mcbr.created_date as createdDate,
                          mbrs.id as blockingReasonId,
                          mbrs.priority as blockingPriority,
                          mbrs.name_of_reason as blockingReasonName,
                          mbrs.is_enabled as blockingReasonEnabled
                          from
                          m_credit_blocking_reason mcbr
                          left join m_blocking_reason_setting mbrs on mbrs.id = mcbr.blocking_reason_id
                          left join m_loan loan on loan.id = mcbr.loan_id
                          left join m_client client on client.id = loan.client_id
                    """;
        }

        @Override
        public LoanBlockingReasonData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String comment = rs.getString("comment");
            final Long loanId = rs.getLong("loanId");
            final boolean isActive = rs.getBoolean("isActive");

            final Long blockingReasonId = rs.getLong("blockingReasonId");
            final Integer blockingPriority = rs.getInt("blockingPriority");
            final String blockingReasonName = rs.getString("blockingReasonName");
            final boolean blockingReasonEnabled = rs.getBoolean("blockingReasonEnabled");

            BlockingReasonsData blockReasonSetting = BlockingReasonsData.builder().id(blockingReasonId).priority(blockingPriority)
                    .nameOfReason(blockingReasonName).isEnabled(blockingReasonEnabled).build();

            return LoanBlockingReasonData.builder().id(id).comment(comment).loanId(loanId).isActive(isActive)
                    .blockReasonSetting(blockReasonSetting).createdDate(rs.getTimestamp("createdDate").toLocalDateTime()).build();

        }
    }

    @Override
    public Collection<LoanBlockingReasonData> retrieveLoanBlockingReason(Long loanId) {
        LoanBlockingReasonMapper mapper = new LoanBlockingReasonMapper();
        final String query = "SELECT " + mapper.schema() + " where mcbr.loan_id = ? and mcbr.is_active = true";
        return jdbcTemplate.query(query, mapper, loanId);
    }

    @Override
    public BlockingReasonsData retrieveLoanBlockingSettings(final String level, final String nameOfReason) {

        final String query = """
                Select mbrs.id as id, mbrs.priority as priority, mbrs.name_of_reason as nameOfReason, mbrs.is_enabled as isEnabled
                from m_blocking_reason_setting mbrs
                where mbrs.level = ? and mbrs.name_of_reason = ?
                """;

        return jdbcTemplate
                .queryForObject(query,
                        (rs, rowNum) -> BlockingReasonsData.builder().id(rs.getLong("id")).priority(rs.getInt("priority"))
                                .nameOfReason(rs.getString("nameOfReason")).isEnabled(rs.getBoolean("isEnabled")).build(),
                        level, nameOfReason);
    }

    @Override
    public Collection<LoanBlockingReasonData> retrieveAllLoanWithBlockingReason(Long blockingReasonId) {
        LoanBlockingReasonMapper mapper = new LoanBlockingReasonMapper();
        final String query = "SELECT " + mapper.schema() + " where mbrs.id = ? and mcbr.is_active = true";
        return jdbcTemplate.query(query, mapper, blockingReasonId);
    }

    @Override
    public Collection<LoanBlockingReasonData> retrieveAllLoanWithClientAndBlockingReason(Long clientId, Long blockingReasonId) {
        LoanBlockingReasonMapper mapper = new LoanBlockingReasonMapper();
        final String query = "SELECT " + mapper.schema() + " where client.id = ? and mbrs.id = ? and mcbr.is_active = true";
        return jdbcTemplate.query(query, mapper, blockingReasonId);
    }
}
