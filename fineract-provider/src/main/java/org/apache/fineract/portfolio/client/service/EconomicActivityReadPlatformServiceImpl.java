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

import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.data.EconomicActivityData;
import org.apache.fineract.portfolio.client.data.EconomicSectorData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class EconomicActivityReadPlatformServiceImpl implements EconomicActivityReadPlatformService {

    private final PaginationHelper paginationHelper;
    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;
    private final EconomicSectorMapper economicSectorMapper;

    @Autowired
    public EconomicActivityReadPlatformServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate,
                                                   DatabaseSpecificSQLGenerator sqlGenerator, PaginationHelper paginationHelper) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.economicSectorMapper = new EconomicSectorMapper();
        this.paginationHelper = paginationHelper;
    }

    public static final class EconomicSectorMapper implements RowMapper<EconomicSectorData> {

        @Override
        public EconomicSectorData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String name = rs.getString("name");

            return EconomicSectorData.instance(id, name);

        }

        public String schema() {
            return " ms.id,ms.name from m_sector_economico ms ";
        }

    }

    public static final class EconomicActivityMapper implements RowMapper<EconomicActivityData> {

        @Override
        public EconomicActivityData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final Long sectorId = rs.getLong("sector_id");
            final String name = rs.getString("name");

            return EconomicActivityData.instance(id,sectorId, name);

        }

        public String schema() {
            return " ma.id,ma.sector_id ,ma.name from m_actividad_economica ma ";
        }

    }

    @Override
    public List<EconomicSectorData> retrieveSectorData() {
        try {
            this.context.authenticatedUser();

            final EconomicSectorMapper rm = new EconomicSectorMapper();

            final String sql = "select " + rm.schema();

            return this.jdbcTemplate.query(sql, rm);
        } catch (final EmptyResultDataAccessException e) {
            e.printStackTrace();
        return null;
        }
    }

    @Override
    public List<EconomicActivityData> retrieveEconomicActivityData() {
        try {
            this.context.authenticatedUser();

            final EconomicActivityMapper rm = new EconomicActivityMapper();

            final String sql = "select " + rm.schema();
            return this.jdbcTemplate.query(sql, rm);
        } catch (final EmptyResultDataAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
