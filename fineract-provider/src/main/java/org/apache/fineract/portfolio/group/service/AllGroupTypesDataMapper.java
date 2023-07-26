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
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.organisation.centerGroup.service.GroupLocationEnumerations;
import org.apache.fineract.portfolio.client.domain.ClientEnumerations;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.group.data.GroupTimelineData;
import org.springframework.jdbc.core.RowMapper;

/**
 *
 */
public final class AllGroupTypesDataMapper implements RowMapper<GroupGeneralData> {

    private final String schemaSql;

    public AllGroupTypesDataMapper() {
        final StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append("g.id as id, g.account_no as accountNumber, g.external_id as externalId, g.display_name as name, ");
        sqlBuilder.append("g.office_id as officeId, o.name as officeName, ");
        sqlBuilder.append("g.parent_id as centerId, pg.display_name as centerName, ");
        sqlBuilder.append("g.staff_id as staffId, s.display_name as staffName, ");
        sqlBuilder.append("g.status_enum as statusEnum, g.activation_date as activationDate, ");
        sqlBuilder.append("g.closedon_date as closedOnDate, ");

        sqlBuilder.append("g.submittedon_date as submittedOnDate, ");
        sqlBuilder.append("sbu.username as submittedByUsername, ");
        sqlBuilder.append("sbu.firstname as submittedByFirstname, ");
        sqlBuilder.append("sbu.lastname as submittedByLastname, ");

        sqlBuilder.append("clu.username as closedByUsername, ");
        sqlBuilder.append("clu.firstname as closedByFirstname, ");
        sqlBuilder.append("clu.lastname as closedByLastname, ");

        sqlBuilder.append("acu.username as activatedByUsername, ");
        sqlBuilder.append("acu.firstname as activatedByFirstname, ");
        sqlBuilder.append("acu.lastname as activatedByLastname, ");

        sqlBuilder.append("g.hierarchy as hierarchy, ");
        sqlBuilder.append("g.level_id as groupLevel, ");

        sqlBuilder.append("g.legacy_number as legacyNumber, ");
        sqlBuilder.append("g.latitude as latitude, g.longitude as longitude, ");
        sqlBuilder.append("g.formation_date as formationDate, g.responsible_user_id as responsibleUserId, ");
        sqlBuilder.append("ru.firstname as userFirstName, ru.lastname as userLastName, ");
        sqlBuilder.append("g.size as size, g.created_date as createdDate, ");
        sqlBuilder.append("g.meeting_start_time as meetingStartTime, g.meeting_end_time as meetingEndTime, ");

        sqlBuilder.append("substring(g.display_name, 1, 5) as centerCodeName, g.portfolio_id as portfolioId, ");
        sqlBuilder.append("g.legacy_number as legacyNumber, g.city_id as cityId, cvCity.code_value as cityValue, ");
        sqlBuilder.append("g.state_province_id as stateId, cvState.code_value as stateValue, g.group_location as group_location, ");
        sqlBuilder.append("g.distance_from_agency as distance, ");
        sqlBuilder.append("g.type_id as typeId, cvType.code_value as typeValue, g.created_date as createdDate, ");
        sqlBuilder.append("g.meeting_start_date as meetingStart, g.meeting_end_date as meetingEnd, ");
        sqlBuilder.append("g.meeting_day as meetingDay, cvMeetingDay.code_value as meetingDayValue, ");
        sqlBuilder.append("cvMeetingDay.order_position as meetingDayOrderPosition, g.meeting_start_time as meetingStartTime, ");
        sqlBuilder.append("g.meeting_end_time as meetingEndTime, g.reference_point as referencePoint ");

        sqlBuilder.append("from m_group g ");
        sqlBuilder.append("join m_office o on o.id = g.office_id ");
        sqlBuilder.append("left join m_staff s on s.id = g.staff_id ");
        sqlBuilder.append("left join m_group pg on pg.id = g.parent_id ");
        sqlBuilder.append("left join m_appuser sbu on sbu.id = g.submittedon_userid ");
        sqlBuilder.append("left join m_appuser acu on acu.id = g.activatedon_userid ");
        sqlBuilder.append("left join m_appuser clu on clu.id = g.closedon_userid ");
        sqlBuilder.append("left join m_appuser ru on ru.id = g.responsible_user_id ");
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
        final String accountNo = rs.getString("accountNumber");
        final String name = rs.getString("name");
        final String externalId = rs.getString("externalId");

        final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
        final EnumOptionData status = ClientEnumerations.status(statusEnum);
        final LocalDate activationDate = JdbcSupport.getLocalDate(rs, "activationDate");

        final Long officeId = JdbcSupport.getLong(rs, "officeId");
        final String officeName = rs.getString("officeName");
        final Long centerId = JdbcSupport.getLong(rs, "centerId");
        final String centerName = rs.getString("centerName");
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

        final Long legacyNumber = rs.getLong("legacyNumber");
        final BigDecimal latitude = rs.getBigDecimal("latitude");
        final BigDecimal longitude = rs.getBigDecimal("longitude");
        final LocalDate formationDate = JdbcSupport.getLocalDate(rs, "formationDate");
        final long responsibleUserId = rs.getLong("responsibleUserId");
        final Integer size = rs.getInt("size");
        final LocalDate createdDate = JdbcSupport.getLocalDate(rs, "createdDate");
        final LocalTime meetingStartTime = JdbcSupport.getLocalTime(rs, "meetingStartTime");
        final LocalTime meetingEndTime = JdbcSupport.getLocalTime(rs, "meetingEndTime");

        final Long portfolioId = rs.getLong("portfolioId");
        final long cityId = rs.getLong("cityId");
        final String cityValue = rs.getString("cityValue");
        final CodeValueData city = CodeValueData.instance(cityId, cityValue);

        final long stateId = rs.getLong("stateId");
        final String stateValue = rs.getString("stateValue");
        final CodeValueData state = CodeValueData.instance(stateId, stateValue);

        Integer distance = rs.getInt("distance");
        distance = rs.wasNull() ? null : distance;

        final Integer groupLocationId = rs.getInt("group_location");

        EnumOptionData groupLocation = null;
        if (groupLocationId != null) {
            groupLocation = GroupLocationEnumerations.groupLocationsOptionData(groupLocationId);
        }

        final long typeId = rs.getLong("typeId");
        final String typeValue = rs.getString("typeValue");
        final CodeValueData type = CodeValueData.instance(typeId, typeValue);

        final int meetingStart = rs.getInt("meetingStart");
        final int meetingEnd = rs.getInt("meetingEnd");
        final int meetingDay = rs.getInt("meetingDay");
        final String meetingDayValue = rs.getString("meetingDayValue");
        final String referencePoint = rs.getString("referencePoint");

        final GroupTimelineData timeline = new GroupTimelineData(submittedOnDate, submittedByUsername, submittedByFirstname,
                submittedByLastname, activationDate, activatedByUsername, activatedByFirstname, activatedByLastname, closedOnDate,
                closedByUsername, closedByFirstname, closedByLastname);

        GroupGeneralData ret = GroupGeneralData.instance(id, accountNo, name, externalId, status, activationDate, officeId, officeName,
                centerId, centerName, staffId, staffName, hierarchy, groupLevel, timeline);

        // set the remaining fields for group general data
        ret.setLegacyNumber(legacyNumber);
        ret.setLatitude(latitude);
        ret.setLongitude(longitude);
        ret.setFormationDate(formationDate);
        ret.setResponsibleUserId(responsibleUserId);
        ret.setSize(size);
        ret.setCreatedDate(createdDate);
        ret.setMeetingStartTime(meetingStartTime);
        ret.setMeetingEndTime(meetingEndTime);
        ret.setPortfolioCenterId(centerId);
        ret.setGroupLocation(groupLocation);

        ret.setPortfolioId(portfolioId);
        ret.setCity(city);
        ret.setState(state);
        ret.setType(type);
        ret.setDistance(distance);
        ret.setMeetingStart(meetingStart);
        ret.setMeetingEnd(meetingEnd);
        ret.setMeetingDay(meetingDay);
        ret.setMeetingDayName(meetingDayValue);
        ret.setReferencePoint(referencePoint);

        return ret;
    }
}
