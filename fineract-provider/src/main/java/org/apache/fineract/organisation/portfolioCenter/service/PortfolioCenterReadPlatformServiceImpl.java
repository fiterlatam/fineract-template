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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
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
import org.apache.fineract.organisation.portfolioCenter.data.PortfolioCenterFrecuencyMeetingEnumData;
import org.apache.fineract.organisation.portfolioCenter.data.PortfolioCenterStatusEnumData;
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
        schemaSql += " order by centerCodeName, meetingDay";

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

        final List<CodeValueData> countryOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(PortfolioCenterConstants.PORTFOLIO_CENTER_COUNTRIES));

        final List<CodeValueData> labourDayOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(PortfolioCenterConstants.MEETING_DAYS));

        return PortfolioCenterData.template(parentOfficesOptions, appUsers, cityOptions, stateOptions, countryOptions, labourDayOptions);
    }

    private static final class PortfolioCenterMapper implements RowMapper<PortfolioCenterData> {

        private final String schema;

        public PortfolioCenterMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append("pc.id as id, pc.name as name, substring(pc.name, 1, 4) as centerCodeName, pc.portfolio_id as portfolioId, ");
            sqlBuilder.append("p.name as portfolioName, pc.address as address, pc.legacy_center_number as legacyCenterNumber, ");
            sqlBuilder.append("pc.address2 as address2, pc.city_id as cityId, cvCity.code_value as cityValue, ");
            sqlBuilder.append("pc.state_province_id as stateId, cvState.code_value as stateValue, ");
            sqlBuilder.append("pc.country_id as countryId, cvCountry.code_value as countryValue, ");
            sqlBuilder.append("pc.zone as zone, pc.center_status as status, pc.distance_from_agency as distance, ");
            sqlBuilder.append("pc.location_id as locationId, cvLocation.code_value as locationValue, ");
            sqlBuilder.append("pc.facilitator_effective_date as effectiveDate, pc.first_meeting_date as firstMeetingDate, ");
            sqlBuilder.append("pc.frequency_meeting as frequencyMeeting, pc.meeting_start_date as meetingStart, ");
            sqlBuilder.append("pc.meeting_end_date as meetingEnd, pc.next_meeting_date as nextMeetingDate, pc.meeting_day as meetingDay ");
            sqlBuilder.append("from m_portfolio_center pc ");
            sqlBuilder.append("left join m_portfolio AS p ON p.id = pc.portfolio_id ");
            sqlBuilder.append("left join m_code_value cvCity on pc.city_id = cvCity.id ");
            sqlBuilder.append("left join m_code_value cvState on pc.state_province_id = cvState.id ");
            sqlBuilder.append("left join m_code_value cvCountry on pc.country_id = cvCountry.id ");
            sqlBuilder.append("left join m_code_value cvLocation on pc.location_id = cvLocation.id ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public PortfolioCenterData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final BigDecimal legacyCenterNumber = rs.getBigDecimal("legacyCenterNumber");

            final Long portfolioId = rs.getLong("portfolioId");
            final String portfolioName = rs.getString("portfolioName");

            final String address = rs.getString("address");
            final String address2 = rs.getString("address2");

            final long cityId = rs.getLong("cityId");
            final String cityValue = rs.getString("cityValue");
            final CodeValueData city = CodeValueData.instance(cityId, cityValue);

            final long stateId = rs.getLong("stateId");
            final String stateValue = rs.getString("stateValue");
            final CodeValueData state = CodeValueData.instance(stateId, stateValue);

            final long countryId = rs.getLong("countryId");
            final String countryValue = rs.getString("countryValue");
            final CodeValueData country = CodeValueData.instance(countryId, countryValue);

            final Integer zone = rs.getInt("zone");
            final Integer distance = rs.getInt("distance");
            final Integer statusId = rs.getInt("status");

            PortfolioCenterStatusEnumData statusEnum = null;
            if (statusId != null) {
                statusEnum = PortfolioCenterEnumerations.status(statusId);
            }

            final long locationId = rs.getLong("locationId");
            final String locationValue = rs.getString("locationValue");
            final CodeValueData location = CodeValueData.instance(locationId, locationValue);

            final LocalDate effectiveDate = JdbcSupport.getLocalDate(rs, "effectiveDate");
            final LocalDate firstMeetingDate = JdbcSupport.getLocalDate(rs, "firstMeetingDate");

            final Integer frequencyMeetingId = rs.getInt("frequencyMeeting");
            PortfolioCenterFrecuencyMeetingEnumData frequencyMeeting = null;
            if (frequencyMeetingId != null) {
                frequencyMeeting = PortfolioCenterEnumerations.type(frequencyMeetingId);
            }

            final int meetingStart = rs.getInt("meetingStart");
            final int meetingEnd = rs.getInt("meetingEnd");
            final LocalDate nextMeetingDate = JdbcSupport.getLocalDate(rs, "nextMeetingDate");
            final int meetingDay = rs.getInt("meetingDay");

            return PortfolioCenterData.instance(id, name, portfolioId, portfolioName, legacyCenterNumber, address, address2, city, state,
                    country, zone, location, statusEnum, distance, effectiveDate, firstMeetingDate, frequencyMeeting, meetingStart,
                    meetingEnd, nextMeetingDate, meetingDay);
        }

        public String schema() {
            return this.schema;
        }

    }

}
