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

package org.apache.fineract.organisation.committee.mappers;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.organisation.committee.data.CommitteeApprovalsData;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationsEnumerations;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RequiredCommitteeApprovalsMapper implements RowMapper<CommitteeApprovalsData> {

        private final String schema;

        public RequiredCommitteeApprovalsMapper() {
            this.schema = """
                    c.id,c.from_amount, c.to_amount, cv.code_value
                    from committee_approval_limits c 
                    join m_code_value cv on cv.id = c.committee_id
                    """;
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public CommitteeApprovalsData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final Integer statusEnum = JdbcSupport.getInteger(rs, "status");
            final Integer bureauStatus = rs.getInt("buroCheckStatus");
            final Long id = JdbcSupport.getLong(rs, "id");
            final BigDecimal nombre = rs.getBigDecimal("from_amount");
            final BigDecimal tipo = rs.getBigDecimal("to_amount");
            final String committeeValue = rs.getString("code_value");
            final EnumOptionData status = PreQualificationsEnumerations.status(committeeValue);

            CommitteeApprovalsData committeeApprovalsData = CommitteeApprovalsData.builder()
                    .id(id)
                    .fromAmount(nombre)
                    .toAmount(tipo)
                    .approvalData(status)
                    .build();
            return committeeApprovalsData;
        }
    }
