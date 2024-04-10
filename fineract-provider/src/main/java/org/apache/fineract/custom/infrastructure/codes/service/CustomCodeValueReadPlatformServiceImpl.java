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
package org.apache.fineract.custom.infrastructure.codes.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.fineract.custom.infrastructure.codes.data.CustomCodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformServiceImpl;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class CustomCodeValueReadPlatformServiceImpl extends CodeValueReadPlatformServiceImpl implements CustomCodeValueReadPlatformService {

    @Autowired
    public CustomCodeValueReadPlatformServiceImpl(PlatformSecurityContext context, JdbcTemplate jdbcTemplate) {
        super(context, jdbcTemplate);
    }

    @Override
    public List<CustomCodeValueData> retrieveCodeValuesByCodeWithParent(final String code) {
        super.context.authenticatedUser();

        final CodeValueDataMapper rm = new CodeValueDataMapper();
        final String sql = "select " + rm.schema()
                + "where c.code_name like ? and cv.is_active = true order by parentCV.code_value, cv.code_value";

        return this.jdbcTemplate.query(sql, rm, new Object[] { code });
    }

    @Override
    public List<CustomCodeValueData> retrieveCodeValuesByCodeAndParent(final String code, final Long parentId) {
        super.context.authenticatedUser();

        final CodeValueDataMapper rm = new CodeValueDataMapper();
        final String sql = "select " + rm.schema()
                + "where c.code_name like ? and cv.parent_id = ? and cv.is_active = true order by position";

        return this.jdbcTemplate.query(sql, rm, new Object[] { code, parentId });
    }

    private static final class CodeValueDataMapper implements RowMapper<CustomCodeValueData> {

        public String schema() {
            return "  cv.id as id, cv.code_value as value, cv.code_id as codeId, cv.code_description as description, " + //
                    " cv.order_position as position, cv.is_active isActive, cv.is_mandatory as mandatory, " + //
                    " cv.parent_id as parentId, parentCV.code_value as parentValue " + //
                    "from " + //
                    " m_code_value as cv " + //
                    " join m_code c on cv.code_id = c.id " + //
                    " left join m_code_value parentCV on parentCV.id = cv.parent_id ";
        }

        @Override
        public CustomCodeValueData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            return new CustomCodeValueData(rs.getLong("id"), rs.getString("value"), rs.getInt("position"), rs.getString("description"),
                    rs.getBoolean("isActive"), rs.getBoolean("mandatory"), rs.getLong("parentId"), rs.getString("parentValue"));
        }
    }
}
