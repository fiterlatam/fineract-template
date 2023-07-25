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
package org.apache.fineract.portfolio.group.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.data.PaginationParameters;
import org.apache.fineract.infrastructure.core.data.PaginationParametersDataValidator;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.infrastructure.security.utils.SQLBuilder;
import org.apache.fineract.organisation.centerGroup.domain.CenterGroupLocation;
import org.apache.fineract.organisation.centerGroup.service.GroupLocationEnumerations;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.office.domain.OfficeHierarchyLevel;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.organisation.portfolioCenter.data.PortfolioCenterData;
import org.apache.fineract.organisation.portfolioCenter.service.PortfolioCenterConstants;
import org.apache.fineract.organisation.portfolioCenter.service.PortfolioCenterReadPlatformService;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.organisation.staff.service.StaffReadPlatformService;
import org.apache.fineract.portfolio.calendar.data.CalendarData;
import org.apache.fineract.portfolio.calendar.service.CalendarEnumerations;
import org.apache.fineract.portfolio.calendar.service.CalendarReadPlatformService;
import org.apache.fineract.portfolio.calendar.service.CalendarUtils;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.domain.ClientEnumerations;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.group.api.GroupingTypesApiConstants;
import org.apache.fineract.portfolio.group.data.CenterData;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.group.data.GroupTimelineData;
import org.apache.fineract.portfolio.group.data.StaffCenterData;
import org.apache.fineract.portfolio.group.domain.GroupTypes;
import org.apache.fineract.portfolio.group.domain.GroupingTypeEnumerations;
import org.apache.fineract.portfolio.group.exception.CenterNotFoundException;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import static org.apache.fineract.organisation.centerGroup.service.GroupLocationEnumerations.groupLocationsOptionData;

@Service
@RequiredArgsConstructor
public class CenterReadPlatformServiceImpl implements CenterReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;
    private final ClientReadPlatformService clientReadPlatformService;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final StaffReadPlatformService staffReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final ConfigurationDomainService configurationDomainService;
    private final CalendarReadPlatformService calendarReadPlatformService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ColumnValidator columnValidator;
    private final AppUserReadPlatformService appUserReadPlatformService;
    private final PortfolioCenterReadPlatformService portfolioCenterReadPlatformService;

    // data mappers
    private final CenterDataMapper centerMapper = new CenterDataMapper();
    private final GroupDataMapper groupDataMapper = new GroupDataMapper();

    private final PaginationHelper paginationHelper;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PaginationParametersDataValidator paginationParametersDataValidator;
    private static final Set<String> SUPPORTED_ORDER_BY_VALUES = new HashSet<>(Arrays.asList("id", "name", "officeId", "officeName"));

    // 'g.' preffix because of ERROR 1052 (23000): Column 'column_name' in where
    // clause is ambiguous
    // caused by the same name of columns in m_office and m_group tables
    private SQLBuilder getCenterExtraCriteria(String schemaSl, final SearchParameters searchCriteria) {

        SQLBuilder extraCriteria = new SQLBuilder();
        extraCriteria.addCriteria("g.level_id =", GroupTypes.CENTER.getId());
        if (searchCriteria != null) {
            extraCriteria.addNonNullCriteria("g.office_id = ", searchCriteria.getOfficeId());
            extraCriteria.addNonNullCriteria("g.external_id = ", searchCriteria.getExternalId());
            extraCriteria.addNonNullCriteria("g.display_name like ", searchCriteria.getName());
            extraCriteria.addNonNullCriteria(" o.hierarchy like ", searchCriteria.getHierarchy());
            extraCriteria.addNonNullCriteria(" g.staff_id = ", searchCriteria.getStaffId());
        }
        return extraCriteria;
    }

    private static final class CenterDataMapper implements RowMapper<CenterData> {

        private final String schemaSql;

        CenterDataMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(500);

            sqlBuilder.append("g.id as id, g.account_no as accountNo, g.external_id as externalId, g.display_name as name, ");
            sqlBuilder.append("g.office_id as officeId, o.name as officeName, ");
            sqlBuilder.append("g.staff_id as staffId, s.display_name as staffName, ");
            sqlBuilder.append("g.status_enum as statusEnum, g.activation_date as activationDate, ");
            sqlBuilder.append("g.hierarchy as hierarchy, g.level_id as groupLevel, ");
            sqlBuilder.append("g.closedon_date as closedOnDate, g.submittedon_date as submittedOnDate, ");
            sqlBuilder
                    .append("substring(g.display_name, 1, 5) as centerCodeName, g.portfolio_id as portfolioId, p.name as portfolioName, ");
            sqlBuilder.append("g.legacy_number as legacyNumber, g.city_id as cityId, cvCity.code_value as cityValue, ");
            sqlBuilder.append("g.state_province_id as stateId, cvState.code_value as stateValue, 0 as center_location, ");
            sqlBuilder.append("g.distance_from_agency as distance, ");
            sqlBuilder.append("g.type_id as typeId, cvType.code_value as typeValue, g.created_date as createdDate, ");
            sqlBuilder.append("g.meeting_start_date as meetingStart, g.meeting_end_date as meetingEnd, ");
            sqlBuilder.append("g.meeting_day as meetingDay, cvMeetingDay.code_value as meetingDayValue, ");
            sqlBuilder.append("cvMeetingDay.order_position as meetingDayOrderPosition, g.meeting_start_time as meetingStartTime, ");
            sqlBuilder.append("g.meeting_end_time as meetingEndTime, g.reference_point as referencePoint, ");
            sqlBuilder.append(
                    "sbu.username as submittedByUsername, sbu.firstname as submittedByFirstname, sbu.lastname as submittedByLastname, ");
            sqlBuilder.append(
                    "clu.username as closedByUsername, clu.firstname as closedByFirstname, clu.lastname as closedByLastname, acu.username as activatedByUsername, ");
            sqlBuilder.append("acu.firstname as activatedByFirstname, acu.lastname as activatedByLastname from m_group g ");
            sqlBuilder.append("join m_office o on o.id = g.office_id left join m_staff s on s.id = g.staff_id ");
            sqlBuilder.append("left join m_group pg on pg.id = g.parent_id left join m_appuser sbu on sbu.id = g.submittedon_userid ");
            sqlBuilder.append(
                    "left join m_appuser acu on acu.id = g.activatedon_userid left join m_appuser clu on clu.id = g.closedon_userid ");
            sqlBuilder.append("left join m_portfolio AS p ON p.id = g.portfolio_id ");
            sqlBuilder.append("left join m_code_value cvCity on g.city_id = cvCity.id ");
            sqlBuilder.append("left join m_code_value cvState on g.state_province_id = cvState.id ");
            sqlBuilder.append("left join m_code_value cvType on g.type_id = cvType.id ");
            sqlBuilder.append("left join m_code_value cvMeetingDay on g.meeting_day = cvMeetingDay.id ");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public CenterData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String accountNo = rs.getString("accountNo");
            final String name = rs.getString("name");
            final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
            final EnumOptionData status = GroupingTypeEnumerations.status(statusEnum);
            final LocalDate activationDate = JdbcSupport.getLocalDate(rs, "activationDate");
            final String externalId = rs.getString("externalId");
            final Long officeId = rs.getLong("officeId");
            final String officeName = rs.getString("officeName");
            final Long staffId = JdbcSupport.getLong(rs, "staffId");
            final String staffName = rs.getString("staffName");
            final String hierarchy = rs.getString("hierarchy");

            final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedOnDate");
            final String closedByUsername = rs.getString("closedByUsername");
            final String closedByFirstname = rs.getString("closedByFirstname");
            final String closedByLastname = rs.getString("closedByLastname");

            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
            final String submittedByUsername = rs.getString("submittedByUsername");
            final String submittedByFirstname = rs.getString("submittedByFirstname");
            final String submittedByLastname = rs.getString("submittedByLastname");

            final String activatedByUsername = rs.getString("activatedByUsername");
            final String activatedByFirstname = rs.getString("activatedByFirstname");
            final String activatedByLastname = rs.getString("activatedByLastname");

            final Long portfolioId = rs.getLong("portfolioId");
            final String portfolioName = rs.getString("portfolioName");
            final long cityId = rs.getLong("cityId");
            final String cityValue = rs.getString("cityValue");
            final CodeValueData city = CodeValueData.instance(cityId, cityValue);

            final long stateId = rs.getLong("stateId");
            final String stateValue = rs.getString("stateValue");
            final CodeValueData state = CodeValueData.instance(stateId, stateValue);

            Integer distance = rs.getInt("distance");
            distance = rs.wasNull() ? null : distance;

            final Integer centerLocationId = rs.getInt("center_location");

            EnumOptionData centerLocation = null;
            if (centerLocationId != null) {
                centerLocation = GroupLocationEnumerations.groupLocationsOptionData(centerLocationId);
            }

            final long typeId = rs.getLong("typeId");
            final String typeValue = rs.getString("typeValue");
            final CodeValueData type = CodeValueData.instance(typeId, typeValue);

            final int meetingStart = rs.getInt("meetingStart");
            final int meetingEnd = rs.getInt("meetingEnd");
            final int meetingDay = rs.getInt("meetingDay");
            final String meetingDayValue = rs.getString("meetingDayValue");
            final String referencePoint = rs.getString("referencePoint");
            final LocalDate createdDate = JdbcSupport.getLocalDate(rs, "createdDate");
            final LocalTime meetingStartTime = JdbcSupport.getLocalTime(rs, "meetingStartTime");
            final LocalTime meetingEndTime = JdbcSupport.getLocalTime(rs, "meetingEndTime");
            final Long legacyNumber = rs.getLong("legacyNumber");

            final GroupTimelineData timeline = new GroupTimelineData(submittedOnDate, submittedByUsername, submittedByFirstname,
                    submittedByLastname, activationDate, activatedByUsername, activatedByFirstname, activatedByLastname, closedOnDate,
                    closedByUsername, closedByFirstname, closedByLastname);

            CenterData centerData = CenterData.instance(id, accountNo, name, externalId, status, activationDate, officeId, officeName,
                    staffId, staffName, hierarchy, timeline, null, null, null, null, null);

            centerData.setPortfolioId(portfolioId);
            centerData.setPortfolioName(portfolioName);
            centerData.setCity(city);
            centerData.setState(state);
            centerData.setType(type);
            centerData.setDistance(distance);
            centerData.setMeetingStart(meetingStart);
            centerData.setMeetingEnd(meetingEnd);
            centerData.setMeetingDay(meetingDay);
            centerData.setMeetingDayName(meetingDayValue);
            centerData.setReferencePoint(referencePoint);
            centerData.setCreatedDate(createdDate);
            centerData.setMeetingStartTime(meetingStartTime);
            centerData.setMeetingEndTime(meetingEndTime);
            centerData.setLegacyNumber(legacyNumber);

            return centerData;
        }
    }

    private static final class CenterCalendarDataMapper implements RowMapper<CenterData> {

        private final String schemaSql;

        CenterCalendarDataMapper() {

            schemaSql = "select ce.id as id, g.account_no as accountNo,"
                    + "ce.display_name as name, g.office_id as officeId, g.staff_id as staffId, s.display_name as staffName,"
                    + " g.external_id as externalId,  g.status_enum as statusEnum, g.activation_date as activationDate,"
                    + " g.hierarchy as hierarchy,   c.id as calendarId, ci.id as calendarInstanceId, ci.entity_id as entityId,"
                    + " ci.entity_type_enum as entityTypeId, c.title as title,  c.description as description,"
                    + "c.location as location, c.start_date as startDate, c.end_date as endDate, c.recurrence as recurrence,c.meeting_time as meetingTime,"
                    + "sum(CASE WHEN l.loan_status_id=300 and lrs.duedate = ? THEN COALESCE(lrs.principal_amount,0)) + (COALESCE(lrs.interest_amount,0) ELSE 0 END)) as installmentDue,"
                    + "sum(CASE WHEN l.loan_status_id=300 and lrs.duedate = ? THEN COALESCE(lrs.principal_completed_derived,0) + COALESCE(lrs.interest_completed_derived,0) ELSE 0 END) as totalCollected,"
                    + "sum(CASE WHEN l.loan_status_id=300 and lrs.duedate <= ? THEN COALESCE(lrs.principal_amount,0) + COALESCE(lrs.interest_amount,0) ELSE 0 END)"
                    + "- sum(CASE WHEN l.loan_status_id=300 and lrs.duedate <= ? THEN COALESCE(lrs.principal_completed_derived,0) + COALESCE(lrs.interest_completed_derived,0) ELSE 0 END) as totaldue, "
                    + "sum(CASE WHEN l.loan_status_id=300 and lrs.duedate < ? THEN COALESCE(lrs.principal_amount,0) + COALESCE(lrs.interest_amount,0) ELSE 0 END)"
                    + "- sum(CASE WHEN l.loan_status_id=300 and lrs.duedate < ? THEN COALESCE(lrs.principal_completed_derived,0) + COALESCE(lrs.interest_completed_derived,0) ELSE 0 END) as totaloverdue"
                    + " from m_calendar c join m_calendar_instance ci on ci.calendar_id=c.id and ci.entity_type_enum=4"
                    + " join m_group ce on ce.id = ci.entity_id" + " join m_group g   on g.parent_id = ce.id"
                    + " join m_group_client gc on gc.group_id=g.id" + " join m_client cl on cl.id=gc.client_id"
                    + " join m_loan l on l.client_id = cl.id"
                    + " join m_loan_repayment_schedule lrs on lrs.loan_id=l.id join m_staff s on g.staff_id = s.id"
                    + " where g.office_id=?";
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public CenterData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String accountNo = rs.getString("accountNo");
            final String name = rs.getString("name");
            final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
            final EnumOptionData status = GroupingTypeEnumerations.status(statusEnum);
            final LocalDate activationDate = JdbcSupport.getLocalDate(rs, "activationDate");
            final String externalId = rs.getString("externalId");
            final Long officeId = rs.getLong("officeId");
            final Long staffId = JdbcSupport.getLong(rs, "staffId");
            final String staffName = rs.getString("staffName");
            final String hierarchy = rs.getString("hierarchy");

            final Long calendarId = rs.getLong("calendarId");
            final Long calendarInstanceId = rs.getLong("calendarInstanceId");
            final Long entityId = rs.getLong("entityId");
            final Integer entityTypeId = rs.getInt("entityTypeId");
            final EnumOptionData entityType = CalendarEnumerations.calendarEntityType(entityTypeId);
            final String title = rs.getString("title");
            final String description = rs.getString("description");
            final String location = rs.getString("location");
            final LocalDate startDate = JdbcSupport.getLocalDate(rs, "startDate");
            final LocalDate endDate = JdbcSupport.getLocalDate(rs, "endDate");
            final String recurrence = rs.getString("recurrence");
            final LocalTime meetingTime = JdbcSupport.getLocalTime(rs, "meetingTime");
            final BigDecimal totalCollected = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalCollected");
            final BigDecimal totalOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalOverdue");
            final BigDecimal totaldue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totaldue");
            final BigDecimal installmentDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "installmentDue");
            Integer monthOnDay = CalendarUtils.getMonthOnDay(recurrence);

            CalendarData calendarData = CalendarData.instance(calendarId, calendarInstanceId, entityId, entityType, title, description,
                    location, startDate, endDate, null, null, false, recurrence, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, meetingTime, monthOnDay);

            return CenterData.instance(id, accountNo, name, externalId, status, activationDate, officeId, null, staffId, staffName,
                    hierarchy, null, calendarData, totalCollected, totalOverdue, totaldue, installmentDue);
        }
    }

    private static final class GroupDataMapper implements RowMapper<GroupGeneralData> {

        private final String schemaSql;

        GroupDataMapper() {

            final StringBuilder sqlBuilder = new StringBuilder(500);

            sqlBuilder.append("g.id as id, g.account_no as accountNo, g.external_id as externalId, g.display_name as name, ");
            sqlBuilder.append("g.office_id as officeId, o.name as officeName, ");
            sqlBuilder.append("g.staff_id as staffId, s.display_name as staffName, ");
            sqlBuilder.append("g.status_enum as statusEnum, g.activation_date as activationDate, ");
            sqlBuilder.append("g.hierarchy as hierarchy, g.level_id as groupLevel, ");
            sqlBuilder.append("g.closedon_date as closedOnDate, g.submittedon_date as submittedOnDate, ");
            sqlBuilder.append("substring(g.display_name, 1, 5) as centerCodeName, g.portfolio_id as portfolioId, ");
            sqlBuilder.append("g.legacy_number as legacyNumber, g.city_id as cityId, cvCity.code_value as cityValue, ");
            sqlBuilder.append("g.state_province_id as stateId, cvState.code_value as stateValue, 0 as center_location, ");
            sqlBuilder.append("g.distance_from_agency as distance, ");
            sqlBuilder.append("g.type_id as typeId, cvType.code_value as typeValue, g.created_date as createdDate, ");
            sqlBuilder.append("g.meeting_start_date as meetingStart, g.meeting_end_date as meetingEnd, ");
            sqlBuilder.append("g.meeting_day as meetingDay, cvMeetingDay.code_value as meetingDayValue, ");
            sqlBuilder.append("cvMeetingDay.order_position as meetingDayOrderPosition, g.meeting_start_time as meetingStartTime, ");
            sqlBuilder.append("g.meeting_end_time as meetingEndTime, g.reference_point as referencePoint, ");
            sqlBuilder.append(
                    "sbu.username as submittedByUsername, sbu.firstname as submittedByFirstname, sbu.lastname as submittedByLastname, ");
            sqlBuilder.append(
                    "clu.username as closedByUsername, clu.firstname as closedByFirstname, clu.lastname as closedByLastname, acu.username as activatedByUsername, ");
            sqlBuilder.append("acu.firstname as activatedByFirstname, acu.lastname as activatedByLastname from m_group g ");
            sqlBuilder.append("join m_office o on o.id = g.office_id left join m_staff s on s.id = g.staff_id ");
            sqlBuilder.append("left join m_group pg on pg.id = g.parent_id left join m_appuser sbu on sbu.id = g.submittedon_userid ");
            sqlBuilder.append(
                    "left join m_appuser acu on acu.id = g.activatedon_userid left join m_appuser clu on clu.id = g.closedon_userid ");
            sqlBuilder.append("left join m_portfolio AS p ON p.id = g.portfolio_id ");
            sqlBuilder.append("left join m_code_value cvCity on g.city_id = cvCity.id ");
            sqlBuilder.append("left join m_code_value cvState on g.state_province_id = cvState.id ");
            sqlBuilder.append("left join m_code_value cvType on g.type_id = cvType.id ");
            sqlBuilder.append("left join m_code_value cvMeetingDay on g.meeting_day = cvMeetingDay.id ");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public GroupGeneralData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String accountNo = rs.getString("accountNo");
            final String name = rs.getString("name");
            final String externalId = rs.getString("externalId");

            final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
            final EnumOptionData status = ClientEnumerations.status(statusEnum);
            final LocalDate activationDate = JdbcSupport.getLocalDate(rs, "activationDate");

            final Long officeId = rs.getLong("officeId");
            final String officeName = rs.getString("officeName");
            final Long staffId = JdbcSupport.getLong(rs, "staffId");
            final String staffName = rs.getString("staffName");
            final String hierarchy = rs.getString("hierarchy");
            final String groupLevel = rs.getString("groupLevel");

            final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedOnDate");
            final String closedByUsername = rs.getString("closedByUsername");
            final String closedByFirstname = rs.getString("closedByFirstname");
            final String closedByLastname = rs.getString("closedByLastname");

            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
            final String submittedByUsername = rs.getString("submittedByUsername");
            final String submittedByFirstname = rs.getString("submittedByFirstname");
            final String submittedByLastname = rs.getString("submittedByLastname");

            final String activatedByUsername = rs.getString("activatedByUsername");
            final String activatedByFirstname = rs.getString("activatedByFirstname");
            final String activatedByLastname = rs.getString("activatedByLastname");

            final GroupTimelineData timeline = new GroupTimelineData(submittedOnDate, submittedByUsername, submittedByFirstname,
                    submittedByLastname, activationDate, activatedByUsername, activatedByFirstname, activatedByLastname, closedOnDate,
                    closedByUsername, closedByFirstname, closedByLastname);

            return GroupGeneralData.instance(id, accountNo, name, externalId, status, activationDate, officeId, officeName, null, null,
                    staffId, staffName, hierarchy, groupLevel, timeline);
        }
    }

    @Override
    public Page<CenterData> retrievePagedAll(final SearchParameters searchParameters, final PaginationParameters parameters) {

        this.paginationParametersDataValidator.validateParameterValues(parameters, SUPPORTED_ORDER_BY_VALUES, "audits");
        final AppUser currentUser = this.context.authenticatedUser();
        final String hierarchy = currentUser.getOffice().getHierarchy();
        final String hierarchySearchString = hierarchy + "%";

        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select " + sqlGenerator.calcFoundRows() + " ");
        sqlBuilder.append(this.centerMapper.schema());
        final SQLBuilder extraCriteria = getCenterExtraCriteria(this.centerMapper.schema(), searchParameters);
        extraCriteria.addNonNullCriteria("o.hierarchy like ", hierarchySearchString);
        sqlBuilder.append(' ').append(extraCriteria.getSQLTemplate());
        if (searchParameters.isOrderByRequested()) {
            sqlBuilder.append(" order by ").append(searchParameters.getOrderBy()).append(' ').append(searchParameters.getSortOrder());
            this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getOrderBy(),
                    searchParameters.getSortOrder());

        } else {
            // default order - order by centerCodeName, meetingDay
            sqlBuilder.append(" order by centerCodeName, meetingDay ");
        }

        if (searchParameters.isLimited()) {
            sqlBuilder.append(" ");
            if (searchParameters.isOffset()) {
                sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit(), searchParameters.getOffset()));
            } else {
                sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit()));
            }
        }

        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), extraCriteria.getArguments(), this.centerMapper);
    }

    @Override
    public Collection<CenterData> retrieveAll(SearchParameters searchParameters, PaginationParameters parameters) {
        if (parameters != null) {
            this.paginationParametersDataValidator.validateParameterValues(parameters, SUPPORTED_ORDER_BY_VALUES, "audits");
        }
        final AppUser currentUser = this.context.authenticatedUser();
        final String hierarchy = currentUser.getOffice().getHierarchy();
        final String hierarchySearchString = hierarchy + "%";

        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select ");
        sqlBuilder.append(this.centerMapper.schema());
        final SQLBuilder extraCriteria = getCenterExtraCriteria(this.centerMapper.schema(), searchParameters);
        extraCriteria.addNonNullCriteria("o.hierarchy like ", hierarchySearchString);
        sqlBuilder.append(' ').append(extraCriteria.getSQLTemplate());
        if (searchParameters != null) {
            if (searchParameters.isOrderByRequested()) {
                sqlBuilder.append(" order by ").append(searchParameters.getOrderBy()).append(' ').append(searchParameters.getSortOrder());
                this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getOrderBy(),
                        searchParameters.getSortOrder());
            } else {
                // default order - order by centerCodeName, meetingDay
                sqlBuilder.append(" order by centerCodeName, meetingDay ");
            }

            if (searchParameters.isLimited()) {
                sqlBuilder.append(" ");
                if (searchParameters.isOffset()) {
                    sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit(), searchParameters.getOffset()));
                } else {
                    sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit()));
                }
            }
        }

        return this.jdbcTemplate.query(sqlBuilder.toString(), this.centerMapper, extraCriteria.getArguments()); // NOSONAR
    }

    @Override
    public Collection<CenterData> retrieveAllForDropdown(final Long officeId) {

        final AppUser currentUser = this.context.authenticatedUser();
        final String hierarchy = currentUser.getOffice().getHierarchy();
        final String hierarchySearchString = hierarchy + "%";

        final String sql = "select " + this.centerMapper.schema()
                + " where g.office_id = ? and g.parent_id is null and g.level_Id = ? and o.hierarchy like ? order by g.hierarchy";

        return this.jdbcTemplate.query(sql, this.centerMapper, new Object[] { officeId, GroupTypes.CENTER.getId(), hierarchySearchString }); // NOSONAR
    }

    @Override
    public CenterData retrieveTemplate(final Long officeId, final boolean staffInSelectedOfficeOnly) {

        final Long officeIdDefaulted = defaultToUsersOfficeIfNull(officeId);

        final Collection<OfficeData> officeOptions = this.officeReadPlatformService.retrieveAllOfficesForDropdown();

        final boolean loanOfficersOnly = false;
        Collection<StaffData> staffOptions = null;
        if (staffInSelectedOfficeOnly) {
            staffOptions = this.staffReadPlatformService.retrieveAllStaffForDropdown(officeIdDefaulted);
        } else {
            staffOptions = this.staffReadPlatformService.retrieveAllStaffInOfficeAndItsParentOfficeHierarchy(officeIdDefaulted,
                    loanOfficersOnly);
        }

        if (CollectionUtils.isEmpty(staffOptions)) {
            staffOptions = null;
        }
        final Collection<GroupGeneralData> groupMembersOptions = null;
        final String accountNo = null;
        final BigDecimal totalCollected = null;
        final BigDecimal totalOverdue = null;
        final BigDecimal totaldue = null;
        final BigDecimal installmentDue = null;

        // final boolean clientPendingApprovalAllowed =
        // this.configurationDomainService.isClientPendingApprovalAllowedEnabled();

        // FB Centers - add options to template
        // cityOptions, stateOptions, typeOptions, statusOptions, meetingDayOptions
        final List<CodeValueData> cityOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(PortfolioCenterConstants.PORTFOLIO_CENTER_MUNICIPALITIES));

        final List<CodeValueData> stateOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(PortfolioCenterConstants.PORTFOLIO_CENTER_DEPARTMENTS));

        final List<CodeValueData> typeOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(PortfolioCenterConstants.PORTFOLIO_CENTER_TYPE));

        final List<CodeValueData> meetingDayOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(PortfolioCenterConstants.MEETING_DAYS));

        return CenterData.template(officeIdDefaulted, accountNo, LocalDate.now(DateUtils.getDateTimeZoneOfTenant()), officeOptions,
                staffOptions, groupMembersOptions, totalCollected, totalOverdue, totaldue, installmentDue, cityOptions, stateOptions,
                typeOptions, meetingDayOptions);
    }

    private Long defaultToUsersOfficeIfNull(final Long officeId) {
        Long defaultOfficeId = officeId;
        if (defaultOfficeId == null) {
            defaultOfficeId = this.context.authenticatedUser().getOffice().getId();
        }
        return defaultOfficeId;
    }

    @Override
    public CenterData retrieveOne(final Long centerId) {

        try {
            final AppUser currentUser = this.context.authenticatedUser();
            final String hierarchy = currentUser.getOffice().getHierarchy();
            final String hierarchySearchString = hierarchy + "%";

            final String sql = "select " + this.centerMapper.schema() + " where g.id = ? and o.hierarchy like ?";
            return this.jdbcTemplate.queryForObject(sql, this.centerMapper, new Object[] { centerId, hierarchySearchString }); // NOSONAR

        } catch (final EmptyResultDataAccessException e) {
            throw new CenterNotFoundException(centerId, e);
        }
    }

    @Override
    public GroupGeneralData retrieveCenterGroupTemplate(final Long centerId) {

        final CenterData center = retrieveOne(centerId);

        final Long centerOfficeId = center.officeId();
        final OfficeData centerOffice = this.officeReadPlatformService.retrieveOffice(centerOfficeId);

        StaffData staff = null;
        final Long staffId = center.staffId();
        String staffName = null;
        if (staffId != null) {
            staff = this.staffReadPlatformService.retrieveStaff(staffId);
            staffName = staff.getDisplayName();
        }

        final Collection<CenterData> centerOptions = Arrays.asList(center);
        final Collection<OfficeData> officeOptions = Arrays.asList(centerOffice);

        Collection<StaffData> staffOptions = this.staffReadPlatformService.retrieveAllStaffForDropdown(centerOfficeId);
        if (CollectionUtils.isEmpty(staffOptions)) {
            staffOptions = null;
        }

        Collection<ClientData> clientOptions = this.clientReadPlatformService.retrieveAllForLookupByOfficeId(centerOfficeId);
        if (CollectionUtils.isEmpty(clientOptions)) {
            clientOptions = null;
        }

        final Collection<OfficeData> parentOfficesOptions = officeReadPlatformService
                .retrieveOfficesByHierarchyLevel(Long.valueOf(OfficeHierarchyLevel.CARTERA.getValue()));

        final List<AppUserData> appUsers = new ArrayList<>(this.appUserReadPlatformService.retrieveAllUsers());

        Collection<PortfolioCenterData> portfolioCenterOptions = this.portfolioCenterReadPlatformService.retrieveAllByCurrentUser();

        List<EnumOptionData> centerGroupLocations = Arrays.asList(groupLocationsOptionData(CenterGroupLocation.URBAN),
                groupLocationsOptionData(CenterGroupLocation.RURAL));

        return GroupGeneralData.template(centerOfficeId, center.getId(), center.getAccountNo(), center.getName(), staffId, staffName,
                centerOptions, officeOptions, staffOptions, clientOptions, null, parentOfficesOptions, appUsers, portfolioCenterOptions,
                centerGroupLocations);
    }

    @Override
    public Collection<GroupGeneralData> retrieveAssociatedGroups(final Long centerId) {
        final String sql = "select " + this.groupDataMapper.schema() + " where g.parent_id = ? order by g.id";
        return this.jdbcTemplate.query(sql, this.groupDataMapper, new Object[] { centerId }); // NOSONAR
    }

    @Override
    public CenterData retrieveCenterWithClosureReasons() {
        final List<CodeValueData> closureReasons = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(GroupingTypesApiConstants.CENTER_CLOSURE_REASON));
        return CenterData.withClosureReasons(closureReasons);
    }

    @Override
    public Collection<StaffCenterData> retriveAllCentersByMeetingDate(final Long officeId, final LocalDate meetingDate,
            final Long staffId) {
        validateForGenerateCollectionSheet(staffId);
        final CenterCalendarDataMapper centerCalendarMapper = new CenterCalendarDataMapper();
        String sql = centerCalendarMapper.schema();
        Collection<CenterData> centerDataArray;
        if (staffId != null) {
            sql += " and g.staff_id=? ";
            sql += "and lrs.duedate<=? and l.loan_type_enum=3";
            sql += " group by c.id, ci.id, g.account_no, g.external_id, g.status_enum, g.activation_date, g.hierarchy";
            centerDataArray = this.jdbcTemplate.query(sql, centerCalendarMapper, // NOSONAR
                    meetingDate, meetingDate, meetingDate, meetingDate, meetingDate, meetingDate, officeId, staffId, meetingDate);
        } else {
            centerDataArray = this.jdbcTemplate.query(sql, centerCalendarMapper, // NOSONAR
                    meetingDate, meetingDate, meetingDate, meetingDate, meetingDate, meetingDate, officeId);
        }

        Collection<StaffCenterData> staffCenterDataArray = new ArrayList<>();
        Boolean flag = false;
        Integer numberOfDays = 0;
        boolean isSkipRepaymentOnFirstMonthEnabled = this.configurationDomainService.isSkippingMeetingOnFirstDayOfMonthEnabled();
        if (isSkipRepaymentOnFirstMonthEnabled) {
            numberOfDays = this.configurationDomainService.retreivePeroidInNumberOfDaysForSkipMeetingDate().intValue();
        }
        for (CenterData centerData : centerDataArray) {
            if (centerData.getCollectionMeetingCalendar().isValidRecurringDate(meetingDate, isSkipRepaymentOnFirstMonthEnabled,
                    numberOfDays)) {
                if (staffCenterDataArray.size() <= 0) {
                    Collection<CenterData> meetingFallCenter = new ArrayList<>();
                    meetingFallCenter.add(centerData);
                    staffCenterDataArray.add(StaffCenterData.instance(centerData.staffId(), centerData.getStaffName(), meetingFallCenter));
                } else {
                    for (StaffCenterData staffCenterData : staffCenterDataArray) {
                        flag = false;
                        if (staffCenterData.getStaffId().equals(centerData.staffId())) {
                            staffCenterData.getMeetingFallCenters().add(centerData);

                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        Collection<CenterData> meetingFallCenter = new ArrayList<>();
                        meetingFallCenter.add(centerData);
                        staffCenterDataArray
                                .add(StaffCenterData.instance(centerData.staffId(), centerData.getStaffName(), meetingFallCenter));
                    }
                }

            }
        }
        return staffCenterDataArray;
    }

    @Override
    public Collection<CenterData> retrieveAllCentersByCurrentUser() {

        final List<Long> officeIds = new ArrayList<>();

        final Collection<OfficeData> parentOfficesOptions = officeReadPlatformService
                .retrieveOfficesByHierarchyLevel(Long.valueOf(OfficeHierarchyLevel.AGENCIA.getValue()));
        parentOfficesOptions.forEach(parentOffice -> officeIds.add(parentOffice.getId()));

        String inSql = String.join(",", Collections.nCopies(officeIds.size(), "?"));

        String schemaSql = "select " + this.centerMapper.schema();
        schemaSql += "where g.office_id in (%s)";

        return this.jdbcTemplate.query(String.format(schemaSql, inSql), this.centerMapper, officeIds.toArray());
    }

    public void validateForGenerateCollectionSheet(final Long staffId) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("productivecollectionsheet");
        baseDataValidator.reset().parameter("staffId").value(staffId).notNull();

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }

    }
}
