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
package org.apache.fineract.organisation.agency.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.agency.data.AgencyData;
import org.apache.fineract.organisation.agency.exception.AgencyNotFoundException;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.office.domain.OfficeHierarchyLevel;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class AgencyReadPlatformServiceImpl implements AgencyReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;
    private final CurrencyReadPlatformService currencyReadPlatformService;
    private final ColumnValidator columnValidator;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final AppUserReadPlatformService appUserReadPlatformService;

    @Autowired
    public AgencyReadPlatformServiceImpl(final PlatformSecurityContext context,
            final CurrencyReadPlatformService currencyReadPlatformService, final JdbcTemplate jdbcTemplate,
            final ColumnValidator columnValidator, final DatabaseSpecificSQLGenerator sqlGenerator,
            final CodeValueReadPlatformService codeValueReadPlatformService, final OfficeReadPlatformService officeReadPlatformService,
            final AppUserReadPlatformService appUserReadPlatformService) {
        this.context = context;
        this.currencyReadPlatformService = currencyReadPlatformService;
        this.columnValidator = columnValidator;
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.officeReadPlatformService = officeReadPlatformService;
        this.appUserReadPlatformService = appUserReadPlatformService;
    }

    @Override
    public AgencyData findById(Long agencyId) {
        try {
            this.context.authenticatedUser();

            AgencyMapper agencyMapper = new AgencyMapper();
            String schemaSql = "select " + agencyMapper.schema();
            schemaSql += "where a.id = ?";

            return this.jdbcTemplate.queryForObject(schemaSql, agencyMapper, new Object[] { agencyId });
        } catch (final EmptyResultDataAccessException e) {
            throw new AgencyNotFoundException(agencyId, e);
        }
    }

    @Override
    public AgencyData retrieveNewAgencyTemplate() {
        this.context.authenticatedUser();

        final Collection<OfficeData> parentOfficesOptions = officeReadPlatformService
                .retrieveOfficesByHierarchyLevel(Long.valueOf(OfficeHierarchyLevel.GERENCIA.getValue()));

        Collection<CurrencyData> currencyOptions = this.currencyReadPlatformService.retrieveAllowedCurrencies();

        final List<CodeValueData> cityOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(AgencyConstants.AGENCY_MUNICIPALITIES));

        final List<CodeValueData> stateOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(AgencyConstants.AGENCY_DEPARTMENTS));

        final List<CodeValueData> countryOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(AgencyConstants.AGENCY_COUNTRIES));

        final List<CodeValueData> agencyEntityCodesOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(AgencyConstants.AGENCY_ENTITY_CODE));

        final List<CodeValueData> agencyTypeOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(AgencyConstants.AGENCY_TYPE));

        final List<CodeValueData> labourDayOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(AgencyConstants.LABOUR_DAYS));

        final List<CodeValueData> financialMonthOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(AgencyConstants.FINANCIAL_MONTHS));

        final List<AppUserData> appUsers = new ArrayList<>(
                this.appUserReadPlatformService.retrieveUsersUnderHierarchy(Long.valueOf(OfficeHierarchyLevel.AGENCIA.getValue())));

        return AgencyData.template(parentOfficesOptions, currencyOptions, cityOptions, stateOptions, countryOptions,
                agencyEntityCodesOptions, agencyTypeOptions, labourDayOptions, financialMonthOptions, appUsers);
    }

    @Override
    public Collection<AgencyData> retrieveAllByUser() {
        this.context.authenticatedUser();

        final List<Long> officeIds = new ArrayList<>();
        final Collection<OfficeData> parentOfficesOptions = officeReadPlatformService
                .retrieveOfficesByHierarchyLevel(Long.valueOf(OfficeHierarchyLevel.REGION.getValue()));
        parentOfficesOptions.forEach(parentOffice -> officeIds.add(parentOffice.getId()));

        String inSql = String.join(",", Collections.nCopies(officeIds.size(), "?"));
        AgencyMapper agencyMapper = new AgencyMapper();
        String schemaSql = "select " + agencyMapper.schema();
        schemaSql += "where a.linked_office_id in (%s)";

        return this.jdbcTemplate.query(String.format(schemaSql, inSql), agencyMapper, officeIds.toArray());
    }

    private static final class AgencyMapper implements RowMapper<AgencyData> {

        private final String schema;

        public AgencyMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append("a.id as id, a.name as name,  a.address as address, a.city_id as cityId, cvCity.code_value as cityValue, ");
            sqlBuilder.append("a.state_province_id as stateId, cvState.code_value as stateValue, a.country_id as countryId, ");
            sqlBuilder.append(
                    "cvCountry.code_value as countryValue, a.entity_code as entityCodeId, cvEntityCode.code_value as entityCodeValue, ");
            sqlBuilder.append("currency_code as currencyCode, a.agency_type as agencyTypeId, cvAgencyType.code_value as agencyTypeValue, ");
            sqlBuilder.append("a.phone_number as phone, a.telex_number as telex, ");
            sqlBuilder.append("a.labour_day_from as labourDayFromId, cvLabourDayFrom.code_value as labourDayFromValue, ");
            sqlBuilder.append(
                    "a.labour_day_to as labourDayToId, cvLabourDayTo.code_value as labourDayToValue, a.open_hour_morning as openHourMorning, ");
            sqlBuilder.append(
                    "a.open_hour_afternoon as openHourAfternoon, a.financial_year_from as financialYearFromId, cvFinancialYearFrom.code_value as financialYearFromValue, ");
            sqlBuilder.append("a.financial_year_to as financialYearToId, cvFinancialYearTo.code_value as financialYearToValue, ");
            sqlBuilder.append("a.non_business_day_1 as nonBusinessDay1Id, cvNonBusinessDay1.code_value as nonBusinessDay1Value, ");
            sqlBuilder.append("a.non_business_day_2 as nonBusinessDay2Id, cvNonBusinessDay2.code_value as nonBusinessDay2Value, ");
            sqlBuilder.append("a.half_business_day_1 as halfBusinessDay1Id, cvHalfBusinessDay1.code_value as halfBusinessDay1Value, ");
            sqlBuilder.append("a.half_business_day_2 as halfBusinessDay2Id, cvHalfBusinessDay2.code_value as halfBusinessDay2Value, ");
            sqlBuilder.append("a.linked_office_id as parentRegionId, region.name as parentRegionName, ");
            sqlBuilder.append("a.responsible_user_id as responsibleUserId, ru.firstname as userFirstName, ru.lastname as userLastName ");
            sqlBuilder.append("from m_agency a left join m_office AS region ON region.id = a.linked_office_id ");
            sqlBuilder.append("left join m_code_value cvCity on a.city_id = cvCity.id ");
            sqlBuilder.append("left join m_code_value cvState on a.state_province_id = cvState.id ");
            sqlBuilder.append("left join m_code_value cvCountry on a.country_id = cvCountry.id ");
            sqlBuilder.append("left join m_code_value cvEntityCode on a.entity_code = cvEntityCode.id ");
            sqlBuilder.append("left join m_code_value cvAgencyType on a.agency_type = cvAgencyType.id ");
            sqlBuilder.append("left join m_code_value cvLabourDayFrom on a.labour_day_from = cvLabourDayFrom.id ");
            sqlBuilder.append("left join m_code_value cvLabourDayTo on a.labour_day_to = cvLabourDayTo.id ");
            sqlBuilder.append("left join m_code_value cvFinancialYearFrom on a.financial_year_from = cvFinancialYearFrom.id ");
            sqlBuilder.append("left join m_code_value cvFinancialYearTo on a.financial_year_to = cvFinancialYearTo.id ");
            sqlBuilder.append("left join m_code_value cvNonBusinessDay1 on a.non_business_day_1 = cvNonBusinessDay1.id ");
            sqlBuilder.append("left join m_code_value cvNonBusinessDay2 on a.non_business_day_2 = cvNonBusinessDay2.id ");
            sqlBuilder.append("left join m_code_value cvHalfBusinessDay1 on a.half_business_day_1 = cvHalfBusinessDay1.id ");
            sqlBuilder.append("left join m_code_value cvHalfBusinessDay2 on a.half_business_day_2 = cvHalfBusinessDay2.id ");
            sqlBuilder.append("left join m_appuser ru on ru.id = a.responsible_user_id ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public AgencyData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final String address = rs.getString("address");

            final long cityId = rs.getLong("cityId");
            final String cityValue = rs.getString("cityValue");
            final CodeValueData city = CodeValueData.instance(cityId, cityValue);

            final long stateId = rs.getLong("stateId");
            final String stateValue = rs.getString("stateValue");
            final CodeValueData state = CodeValueData.instance(stateId, stateValue);

            final long countryId = rs.getLong("countryId");
            final String countryValue = rs.getString("countryValue");
            final CodeValueData country = CodeValueData.instance(countryId, countryValue);

            final Long entityCodeId = rs.getLong("entityCodeId");
            final String entityCodeValue = rs.getString("entityCodeValue");
            final CodeValueData entityCode = CodeValueData.instance(entityCodeId, entityCodeValue);

            final Long agencyTypeId = rs.getLong("agencyTypeId");
            final String agencyTypeValue = rs.getString("agencyTypeValue");
            final CodeValueData agencyType = CodeValueData.instance(agencyTypeId, agencyTypeValue);

            final String currencyCode = rs.getString("currencyCode");
            CurrencyData currencyData = new CurrencyData(currencyCode);

            final String phone = rs.getString("phone");
            final String telex = rs.getString("telex");

            final long labourDayFromId = rs.getLong("labourDayFromId");
            final String labourDayFromValue = rs.getString("labourDayFromValue");
            final CodeValueData labourDayFrom = CodeValueData.instance(labourDayFromId, labourDayFromValue);

            final long labourDayToId = rs.getLong("labourDayToId");
            final String labourDayToValue = rs.getString("labourDayToValue");
            final CodeValueData labourDayTo = CodeValueData.instance(labourDayToId, labourDayToValue);

            final LocalTime openHourMorning = JdbcSupport.getLocalTime(rs, "openHourMorning");
            final LocalTime openHourAfternoon = JdbcSupport.getLocalTime(rs, "openHourAfternoon");

            final long financialYearFromId = rs.getLong("financialYearFromId");
            final String financialYearFromValue = rs.getString("financialYearFromValue");
            final CodeValueData financialYearFrom = CodeValueData.instance(financialYearFromId, financialYearFromValue);

            final long financialYearToId = rs.getLong("financialYearToId");
            final String financialYearToValue = rs.getString("financialYearToValue");
            final CodeValueData financialYearTo = CodeValueData.instance(financialYearToId, financialYearToValue);

            final long nonBusinessDay1Id = rs.getLong("nonBusinessDay1Id");
            final String nonBusinessDay1Value = rs.getString("nonBusinessDay1Value");
            final CodeValueData nonBusinessDay1 = CodeValueData.instance(nonBusinessDay1Id, nonBusinessDay1Value);

            final long nonBusinessDay2Id = rs.getLong("nonBusinessDay2Id");
            final String nonBusinessDay2Value = rs.getString("nonBusinessDay2Value");
            final CodeValueData nonBusinessDay2 = CodeValueData.instance(nonBusinessDay2Id, nonBusinessDay2Value);

            final long halfBusinessDay1Id = rs.getLong("halfBusinessDay1Id");
            final String halfBusinessDay1Value = rs.getString("halfBusinessDay1Value");
            final CodeValueData halfBusinessDay1 = CodeValueData.instance(halfBusinessDay1Id, halfBusinessDay1Value);

            final long halfBusinessDay2Id = rs.getLong("halfBusinessDay2Id");
            final String halfBusinessDay2Value = rs.getString("halfBusinessDay2Value");
            final CodeValueData halfBusinessDay2 = CodeValueData.instance(halfBusinessDay2Id, halfBusinessDay2Value);

            final long parentId = rs.getLong("parentRegionId");
            final String parentName = rs.getString("parentRegionName");

            final long responsibleUserId = rs.getLong("responsibleUserId");
            final String responsibleUserName = rs.getString("userFirstName") + " " + rs.getString("userLastName");

            return AgencyData.instance(id, name, parentId, parentName, address, city, state, country, entityCode, currencyData, agencyType,
                    phone, telex, labourDayFrom, labourDayTo, openHourMorning, openHourAfternoon, responsibleUserId, responsibleUserName,
                    financialYearFrom, financialYearTo, nonBusinessDay1, nonBusinessDay2, halfBusinessDay1, halfBusinessDay2);
        }

        public String schema() {
            return this.schema;
        }
    }

    @Override
    public Collection<AgencyData> retrieveByOfficeHierarchy(final String hierarchy) {
        final String sql = """
                        SELECT
                        ma.id AS id,
                        ma.name AS name
                        FROM
                        m_office mo
                        INNER JOIN m_office office_under ON
                        office_under.hierarchy LIKE CONCAT(mo.hierarchy, '%')AND office_under.hierarchy LIKE CONCAT(?, '%')
                        INNER JOIN m_agency ma ON ma.office_id = office_under.id
                        GROUP BY ma.id
                """;
        return this.jdbcTemplate.query(sql, (rs, rowNum) -> AgencyData.instance(rs.getLong("id"), rs.getString("name")),
                new Object[] { hierarchy });
    }
}
