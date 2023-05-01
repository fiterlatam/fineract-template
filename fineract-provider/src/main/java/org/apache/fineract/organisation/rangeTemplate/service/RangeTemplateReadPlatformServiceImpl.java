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
package org.apache.fineract.organisation.rangeTemplate.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.rangeTemplate.data.RangeTemplateData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class RangeTemplateReadPlatformServiceImpl implements RangeTemplateReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;
    private final ColumnValidator columnValidator;

    @Autowired
    public RangeTemplateReadPlatformServiceImpl(final JdbcTemplate jdbcTemplate, final DatabaseSpecificSQLGenerator sqlGenerator,
            final PlatformSecurityContext context, final ColumnValidator columnValidator) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.context = context;
        this.columnValidator = columnValidator;
    }

    @Override
    public Collection<RangeTemplateData> retrieveAll() {
        RangeTemplateMapper rangeTemplateMapper = new RangeTemplateMapper();

        final String sql = "select " + rangeTemplateMapper.schema() + " order by name asc";
        return this.jdbcTemplate.query(sql, rangeTemplateMapper); // NOSON
    }

    private static final class RangeTemplateMapper implements RowMapper<RangeTemplateData> {

        private final String schema;

        public RangeTemplateMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append("rt.id as id, rt.name as name, rt.code as code, rt.month_start_day as startDay, rt.month_end_day as endDay ");
            sqlBuilder.append("from m_range_template rt");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public RangeTemplateData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final String code = rs.getString("code");

            final int startDay = rs.getInt("startDay");
            final int endDay = rs.getInt("endDay");

            return RangeTemplateData.instance(id, name, code, startDay, endDay);
        }

        public String schema() {
            return this.schema;
        }

    }
}
