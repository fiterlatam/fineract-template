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
package org.apache.fineract.infrastructure.clientblockingreasons.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.clientblockingreasons.data.BlockingReasonsData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManageBlockingReasonsReadPlatformServiceImpl implements ManageBlockingReasonsReadPlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(ManageBlockingReasonsReadPlatformServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;

    @Override
    public Collection<BlockingReasonsData> retrieveAllBlockingReasons(final String level) {
        this.context.authenticatedUser();
        final BlockingReasonsMapper rm = new BlockingReasonsMapper();
        String sql = "SELECT " + rm.schema();
        Object[] parmams = new Object[] {};
        if (StringUtils.isNotBlank(level)) {
            sql += " WHERE brs.level = ? ";
            parmams = new Object[] { level };
        }
        sql += " order by brs.id";
        return this.jdbcTemplate.query(sql, rm, parmams);
    }

    @Override
    public BlockingReasonsData getBlockingReasonsById(Long id) {
        this.context.authenticatedUser();

        final BlockingReasonsSettingsMapper rm = new BlockingReasonsSettingsMapper();
        final String sql = "SELECT " + rm.schema();

        return this.jdbcTemplate.queryForObject(sql, rm, id);
    }

    private static final class BlockingReasonsMapper implements RowMapper<BlockingReasonsData> {

        public String schema() {
            return "  brs.id as id, brs.priority as priority,    brs.description as description, "
                    + "       brs.name_of_reason as nameOfReason,brs.level as level, " + "       brs.created_date as createdDate "
                    + "       FROM m_blocking_reason_setting  brs";
        }

        @Override
        public BlockingReasonsData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final Integer priority = rs.getInt("priority");
            final String description = rs.getString("description");
            final String nameOfReason = rs.getString("nameOfReason");
            final String level = rs.getString("level");
            final LocalDate createdDate = JdbcSupport.getLocalDate(rs, "createdDate");

            return new BlockingReasonsData(id, priority, description, nameOfReason, level, createdDate);

        }
    }

    private static final class BlockingReasonsSettingsMapper implements RowMapper<BlockingReasonsData> {

        public String schema() {
            return "  brs.id as id, brs.priority as priority,  brs.description as description, "
                    + "       brs.name_of_reason as nameOfReason,brs.level as level, " + "       brs.created_date as createdDate "
                    + "       FROM m_blocking_reason_setting brs  WHERE brs.id = ?";
        }

        @Override
        public BlockingReasonsData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final Integer priority = rs.getInt("priority");
            final String description = rs.getString("description");
            final String nameOfReason = rs.getString("nameOfReason");
            final String level = rs.getString("level");
            final LocalDate createdDate = JdbcSupport.getLocalDate(rs, "createdDate");

            return new BlockingReasonsData(id, priority, description, nameOfReason, level, createdDate);

        }
    }

}
