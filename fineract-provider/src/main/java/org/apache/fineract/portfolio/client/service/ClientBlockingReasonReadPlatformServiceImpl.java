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
package org.apache.fineract.portfolio.client.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.data.ClientBlockingReasonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ClientBlockingReasonReadPlatformServiceImpl implements ClientBlockingReasonReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;

    @Autowired
    public ClientBlockingReasonReadPlatformServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final class ClientBlockingReasonMapper implements RowMapper<ClientBlockingReasonData> {

        public String schema() {
            return "cbr.id AS id, cbr.block_date AS blockDate, brs.priority AS priority, brs.name_of_reason AS name, brs.description AS description "
                    + "FROM m_client_blocking_reason cbr " + "JOIN m_blocking_reason_setting brs ON brs.id = cbr.blocking_reason_id ";
        }

        @Override
        public ClientBlockingReasonData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final long id = rs.getLong("id");
            final String name = rs.getString("name");
            final String description = rs.getString("description");
            final Integer priority = rs.getInt("priority");
            final LocalDate blockDate = JdbcSupport.getLocalDate(rs, "blockDate");

            return ClientBlockingReasonData.create(id, name, description, blockDate, priority);
        }
    }

    @Override
    public Collection<ClientBlockingReasonData> retrieveClientBlockingReason(Long clientId) {
        this.context.authenticatedUser();

        final ClientBlockingReasonMapper rm = new ClientBlockingReasonMapper();
        final String sql = "select " + rm.schema() + " WHERE cbr.client_id=? AND cbr.unblock_date IS NULL ORDER BY brs.priority ASC";

        return this.jdbcTemplate.query(sql, rm, clientId); // NOSONAR
    }
}
