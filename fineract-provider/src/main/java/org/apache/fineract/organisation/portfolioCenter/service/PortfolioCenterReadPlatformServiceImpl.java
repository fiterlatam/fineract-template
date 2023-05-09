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
package org.apache.fineract.organisation.portfolioCenter.service;

import static org.apache.fineract.organisation.portfolioCenter.service.PortfolioCenterEnumerations.statusOptionData;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.office.domain.OfficeHierarchyLevel;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.organisation.portfolio.exception.PortfolioNotFoundException;
import org.apache.fineract.organisation.portfolioCenter.data.PortfolioCenterData;
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenterStatus;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class PortfolioCenterReadPlatformServiceImpl implements PortfolioCenterReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;
    private final CurrencyReadPlatformService currencyReadPlatformService;
    private final ColumnValidator columnValidator;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final AppUserReadPlatformService appUserReadPlatformService;

    @Autowired
    public PortfolioCenterReadPlatformServiceImpl(final JdbcTemplate jdbcTemplate, final DatabaseSpecificSQLGenerator sqlGenerator,
            final PlatformSecurityContext context, final CurrencyReadPlatformService currencyReadPlatformService,
            final ColumnValidator columnValidator, final CodeValueReadPlatformService codeValueReadPlatformService,
            final OfficeReadPlatformService officeReadPlatformService, final AppUserReadPlatformService appUserReadPlatformService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.context = context;
        this.currencyReadPlatformService = currencyReadPlatformService;
        this.columnValidator = columnValidator;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.officeReadPlatformService = officeReadPlatformService;
        this.appUserReadPlatformService = appUserReadPlatformService;
    }

    @Override
    public PortfolioCenterData findById(Long portfolioCenterId) {
        try {
            this.context.authenticatedUser();

            PortfolioCenterMapper portfolioCenterMapper = new PortfolioCenterMapper();
            String schemaSql = "select " + portfolioCenterMapper.schema();
            schemaSql += "where pc.id = ?";

            return this.jdbcTemplate.queryForObject(schemaSql, portfolioCenterMapper, new Object[] { portfolioCenterId });
        } catch (final EmptyResultDataAccessException e) {
            throw new PortfolioNotFoundException(portfolioCenterId, e);
        }
    }

    @Override
    public Collection<PortfolioCenterData> retrieveAllByPortfolio(Long portfolioId) {
        this.context.authenticatedUser();

        PortfolioCenterMapper portfolioCenterMapper = new PortfolioCenterMapper();
        String schemaSql = "select " + portfolioCenterMapper.schema();
        schemaSql += " where pc.portfolio_id = ? ";
        schemaSql += " order by centerCodeName";

        List<Object> params = new ArrayList<>();
        params.add(portfolioId);

        return this.jdbcTemplate.query(schemaSql, params.toArray(), portfolioCenterMapper);
    }

    @Override
    public PortfolioCenterData retrievePortfolioCenterTemplate() {
        this.context.authenticatedUser();

        final Collection<OfficeData> parentOfficesOptions = officeReadPlatformService
                .retrieveOfficesByHierarchyLevel(Long.valueOf(OfficeHierarchyLevel.SUPERVISION.getValue()));

        final List<AppUserData> appUsers = new ArrayList<>(this.appUserReadPlatformService.retrieveAllUsers());

        final List<CodeValueData> cityOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(PortfolioCenterConstants.PORTFOLIO_CENTER_MUNICIPALITIES));

        final List<CodeValueData> stateOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(PortfolioCenterConstants.PORTFOLIO_CENTER_DEPARTMENTS));

        final List<CodeValueData> typeOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(PortfolioCenterConstants.PORTFOLIO_CENTER_TYPE));

        final Collection<EnumOptionData> statusOptions = retrieveCenterStatusOptions();

        return PortfolioCenterData.template(parentOfficesOptions, appUsers, cityOptions, stateOptions, typeOptions, statusOptions);
    }

    @Override
    public List<EnumOptionData> retrieveCenterStatusOptions() {
        final List<EnumOptionData> statusOptions = Arrays.asList(statusOptionData(PortfolioCenterStatus.ACTIVE),
                statusOptionData(PortfolioCenterStatus.INACTIVE));
        return statusOptions;
    }

    private static final class PortfolioCenterMapper implements RowMapper<PortfolioCenterData> {

        private final String schema;

        public PortfolioCenterMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append("pc.id as id, pc.name as name, substring(pc.name, 1, 4) as centerCodeName, pc.portfolio_id as portfolioId, ");
            sqlBuilder.append("p.name as portfolioName, pc.legacy_center_number as legacyCenterNumber, ");
            sqlBuilder.append("pc.city_id as cityId, cvCity.code_value as cityValue, ");
            sqlBuilder.append("pc.state_province_id as stateId, cvState.code_value as stateValue, ");
            sqlBuilder.append("pc.center_status as status, pc.distance_from_agency as distance, ");
            sqlBuilder.append("pc.type_id as typeId, cvType.code_value as typeValue, pc.created_date as createdDate ");
            sqlBuilder.append("from m_portfolio_center pc ");
            sqlBuilder.append("left join m_portfolio AS p ON p.id = pc.portfolio_id ");
            sqlBuilder.append("left join m_code_value cvCity on pc.city_id = cvCity.id ");
            sqlBuilder.append("left join m_code_value cvState on pc.state_province_id = cvState.id ");
            sqlBuilder.append("left join m_code_value cvType on pc.type_id = cvType.id ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public PortfolioCenterData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final BigDecimal legacyCenterNumber = rs.getBigDecimal("legacyCenterNumber");

            final Long portfolioId = rs.getLong("portfolioId");
            final String portfolioName = rs.getString("portfolioName");

            final long cityId = rs.getLong("cityId");
            final String cityValue = rs.getString("cityValue");
            final CodeValueData city = CodeValueData.instance(cityId, cityValue);

            final long stateId = rs.getLong("stateId");
            final String stateValue = rs.getString("stateValue");
            final CodeValueData state = CodeValueData.instance(stateId, stateValue);

            final Integer distance = rs.getInt("distance");
            final Integer statusId = rs.getInt("status");

            final LocalDate createdDate = JdbcSupport.getLocalDate(rs, "createdDate");

            EnumOptionData statusEnum = null;
            if (statusId != null) {
                statusEnum = PortfolioCenterEnumerations.statusOptionData(statusId);
            }

            final long typeId = rs.getLong("typeId");
            final String typeValue = rs.getString("typeValue");
            final CodeValueData type = CodeValueData.instance(typeId, typeValue);

            return PortfolioCenterData.instance(id, name, portfolioId, portfolioName, legacyCenterNumber, city, state, type, statusEnum,
                    distance, createdDate);
        }

        public String schema() {
            return this.schema;
        }

    }

}
