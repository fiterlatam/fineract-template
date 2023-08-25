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
package org.apache.fineract.organisation.prequalification.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.organisation.prequalification.data.PrequalificationChecklistData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class PrequalificationChecklistReadPlatformServiceImpl implements PrequalificationChecklistReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PrequalificationChecklistReadPlatformServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<PrequalificationChecklistData> retrievePrequalificationChecklists(String prequalificationNumber) {
        final PrequalificationChecklistMapper rm = new PrequalificationChecklistMapper();
        final String sql = "select " + rm.schema() + " WHERE mpg.prequalification_number = ?";
        return this.jdbcTemplate.query(sql, rm, prequalificationNumber);
    }

    private static final class PrequalificationChecklistMapper implements RowMapper<PrequalificationChecklistData> {

        public String schema() {
            return "cdm.id AS id, cc.name AS name, cc.description AS description, \n"
                    + "LOWER(cdm.color) AS color, mpl.id AS loanProductId, mpl.name AS loanProductName, \n"
                    + "mpg.prequalification_number AS prequalificationNumber, mpg.group_name AS prequalificationName,\n"
                    + "cdm.reference AS reference, TRIM(cdm.`condition`) AS conditionalOperator, cdm.`first_value` firstValue,\n"
                    + "cdm.second_value AS secondValue, cdm.value_list AS valueList\n" + "FROM m_prequalification_group mpg \n"
                    + "LEFT JOIN m_product_loan mpl ON mpl.id = mpg.product_id \n"
                    + "LEFT JOIN checklist_decision_making cdm ON cdm.product_id = mpl.id \n"
                    + "LEFT JOIN checklist_categories cc ON cc.id = cdm.checklist_category_id \n";
        }

        @Override
        public PrequalificationChecklistData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final String name = rs.getString("name");
            final String description = rs.getString("description");
            final String color = rs.getString("color");
            final Long loanProductId = JdbcSupport.getLong(rs, "loanProductId");
            final String loanProductName = rs.getString("loanProductName");
            final String prequalificationNumber = rs.getString("prequalificationNumber");
            final String prequalificationName = rs.getString("prequalificationName");
            final String reference = rs.getString("reference");
            final String conditionalOperator = rs.getString("conditionalOperator");
            final String firstValue = rs.getString("firstValue");
            final String secondValue = rs.getString("secondValue");
            final String valueList = rs.getString("valueList");

            PrequalificationChecklistData prequalificationChecklistData = new PrequalificationChecklistData(id, name, description, color,
                    loanProductId, loanProductName, prequalificationNumber, prequalificationName, reference, conditionalOperator,
                    firstValue, secondValue, valueList);
            return prequalificationChecklistData;
        }
    }
}
