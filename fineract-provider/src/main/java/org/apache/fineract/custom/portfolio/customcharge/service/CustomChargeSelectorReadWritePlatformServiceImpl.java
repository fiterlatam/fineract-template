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
package org.apache.fineract.custom.portfolio.customcharge.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.fineract.custom.portfolio.ally.validator.ClientAllyPointOfSalesDataValidator;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeSelectorData;
import org.apache.fineract.custom.portfolio.customcharge.enumerator.CustomChargeTypeEnum;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class CustomChargeSelectorReadWritePlatformServiceImpl implements CustomChargeSelectorReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final ClientAllyPointOfSalesDataValidator validatorClass;
    private final PlatformSecurityContext context;

    @Autowired
    public CustomChargeSelectorReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate,
            final DatabaseSpecificSQLGenerator sqlGenerator, final ClientAllyPointOfSalesDataValidator validatorClass,
            final PlatformSecurityContext context) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.validatorClass = validatorClass;
        this.context = context;
    }

    @Override
    public List<CustomChargeSelectorData> findAllByClientIdAndClientAllyId(Long clientId, Long clientAllyId) {
        this.context.authenticatedUser();
        final CustomChargeSelectorRowMapper rm = new CustomChargeSelectorRowMapper();

        final String sql = "SELECT " + rm.schema()
                + " WHERE client_id = ? OR client_ally_id = ? OR (client_id IS NULL AND client_ally_id IS NULL) "
                + " ORDER BY custom_charge_type_id, term";

        return this.jdbcTemplate.query(sql, rm, new Object[] { clientId, clientAllyId });
    }

    @Override
    public List<CustomChargeSelectorData> getCustomChargeAval(Long clientId, Long clientAllyId) {
        this.context.authenticatedUser();

        List<CustomChargeSelectorData> ret = new ArrayList<>();

        List<CustomChargeSelectorData> customChargeSelectorDataList = this.findAllByClientIdAndClientAllyId(clientId, clientAllyId);

        // Get all the custom charge type VIP
        List<CustomChargeSelectorData> vipCurve = customChargeSelectorDataList.stream()
                .filter(c -> Objects.equals(c.getCustomChargeTypeId(), CustomChargeTypeEnum.AVAL_VIP.getCode())).toList();

        // Get all the custom charge type from Ally
        List<CustomChargeSelectorData> allyCurve = customChargeSelectorDataList.stream()
                .filter(c -> Objects.equals(c.getCustomChargeTypeId(), CustomChargeTypeEnum.AVAL_ALLY.getCode())).toList();

        // Merge curves to bring the minumum percentage per term
        for (CustomChargeSelectorData vip : vipCurve) {
            for (CustomChargeSelectorData ally : allyCurve) {
                if (vip.getTerm().equals(ally.getTerm())) {
                    ret.add(CustomChargeSelectorData.builder() //
                            .customChargeMapId(vip.getCustomChargeMapId()) //
                            .clientId(vip.getClientId()) //
                            .clientAllyId(vip.getClientAllyId()) //
                            .customChargeTypeId(vip.getCustomChargeTypeId()) //
                            .customChargeTypeName(vip.getCustomChargeTypeName()) //
                            .term(vip.getTerm()) //
                            .percentage(vip.getPercentage().min(ally.getPercentage())) //
                            .build());
                }
            }
        }

        // Get all the custom charge type By Product, if nothing found.
        if (ret.isEmpty()) {
            ret = customChargeSelectorDataList.stream()
                    .filter(c -> Objects.equals(c.getCustomChargeTypeId(), CustomChargeTypeEnum.AVAL_PRODUCT.getCode())).toList();
        }

        return ret;
    }

    private static final class CustomChargeSelectorRowMapper implements RowMapper<CustomChargeSelectorData> {

        public String schema() {
            return "" + "    custom_charge_map_id, client_id, client_ally_id, custom_charge_type_id, term, percentage " //
                    + "    from custom.v_custom_charge_type_choices ";
        }

        @Override
        public CustomChargeSelectorData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            return CustomChargeSelectorData.builder() //
                    .customChargeMapId(rs.getLong("custom_charge_map_id")) //
                    .clientId(rs.getLong("client_id")) //
                    .clientAllyId(rs.getLong("client_ally_id")) //
                    .customChargeTypeId(rs.getLong("custom_charge_type_id")) //
                    .term(rs.getLong("term")) //
                    .percentage(rs.getBigDecimal("percentage")) //
                    .build();
        }
    }

}
