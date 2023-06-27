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
package org.apache.fineract.organisation.centerGroup.service;

import static org.apache.fineract.organisation.centerGroup.service.CenterGroupEnumerations.groupStatusOptionData;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.centerGroup.data.CenterGroupData;
import org.apache.fineract.organisation.centerGroup.domain.CenterGroupStatus;
import org.apache.fineract.organisation.centerGroup.exception.CenterGroupNotFoundException;
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
public class CenterGroupReadPlatformServiceImpl implements CenterGroupReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;
    private final ColumnValidator columnValidator;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final AppUserReadPlatformService appUserReadPlatformService;

    @Autowired
    public CenterGroupReadPlatformServiceImpl(final JdbcTemplate jdbcTemplate, final DatabaseSpecificSQLGenerator sqlGenerator,
            final PlatformSecurityContext context, final ColumnValidator columnValidator,
            final CodeValueReadPlatformService codeValueReadPlatformService, final OfficeReadPlatformService officeReadPlatformService,
            final AppUserReadPlatformService appUserReadPlatformService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.context = context;
        this.columnValidator = columnValidator;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.officeReadPlatformService = officeReadPlatformService;
        this.appUserReadPlatformService = appUserReadPlatformService;
    }

    @Override
    public CenterGroupData findById(Long centerGroupId) {
        try {
            this.context.authenticatedUser();

            CenterGroupMapper centerGroupMapper = new CenterGroupMapper();
            String schemaSql = "select " + centerGroupMapper.schema();
            schemaSql += "where cg.id = ?";

            return this.jdbcTemplate.queryForObject(schemaSql, centerGroupMapper, new Object[] { centerGroupId });
        } catch (final EmptyResultDataAccessException e) {
            throw new CenterGroupNotFoundException(centerGroupId, e);
        }
    }

    @Override
    public Collection<CenterGroupData> retrieveAllByCenter(Long portfolioCenterId) {
        this.context.authenticatedUser();

        // retrieve only active groups related to the center
        CenterGroupMapper centerGroupMapper = new CenterGroupMapper();
        String schemaSql = "select " + centerGroupMapper.schema();
        schemaSql += " where cg.portfolio_center_id = ? ";
        schemaSql += " and cg.status = ? ";
        schemaSql += " order by meetingStartTime ";

        List<Object> params = new ArrayList<>();
        params.add(portfolioCenterId);
        params.add(CenterGroupStatus.ACTIVE.getValue());

        return this.jdbcTemplate.query(schemaSql, params.toArray(), centerGroupMapper);
    }

    @Override
    public CenterGroupData retrieveNewCenterGroupTemplate() {
        this.context.authenticatedUser();

        final Collection<OfficeData> parentOfficesOptions = officeReadPlatformService
                .retrieveOfficesByHierarchyLevel(Long.valueOf(OfficeHierarchyLevel.CARTERA.getValue()));

        final List<AppUserData> appUsers = new ArrayList<>(this.appUserReadPlatformService.retrieveAllUsers());

        final Collection<EnumOptionData> statusOptions = retrieveGroupStatusOptions();

        return CenterGroupData.template(parentOfficesOptions, appUsers, statusOptions);
    }

    private List<EnumOptionData> retrieveGroupStatusOptions() {
        final List<EnumOptionData> statusOptions = Arrays.asList(groupStatusOptionData(CenterGroupStatus.ACTIVE),
                groupStatusOptionData(CenterGroupStatus.INACTIVE));
        return statusOptions;
    }

    private static final class CenterGroupMapper implements RowMapper<CenterGroupData> {

        private final String schema;

        public CenterGroupMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append("cg.id as id, cg.name as name, cg.portfolio_center_id as portfolioCenterId, ");
            sqlBuilder.append("pc.name as portfolioCenterName, cg.legacy_group_number as legacyGroupNumber, ");
            sqlBuilder.append("cg.latitude as latitude, cg.longitude as longitude, cg.formation_date as formationDate, ");
            sqlBuilder.append("cg.latitude as latitude, cg.longitude as longitude, cg.formation_date as formationDate, ");
            sqlBuilder.append("cg.responsible_user_id as responsibleUserId, ru.firstname as userFirstName, ru.lastname as userLastName, ");
            sqlBuilder.append("cg.status as status, cg.size as size, cg.created_date as createdDate, ");
            sqlBuilder.append("cg.meeting_start_time as meetingStartTime, cg.meeting_end_time as meetingEndTime ");
            sqlBuilder.append("from m_center_group cg ");
            sqlBuilder.append("left join m_portfolio_center AS pc ON pc.id = cg.portfolio_center_id ");
            sqlBuilder.append("left join m_appuser ru on ru.id = cg.responsible_user_id ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public CenterGroupData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            Long legacyGroupNumber = rs.getLong("legacyGroupNumber");
            legacyGroupNumber = rs.wasNull() ? null : legacyGroupNumber;

            final Long portfolioCenterId = rs.getLong("portfolioCenterId");
            final String portfolioCenterName = rs.getString("portfolioCenterName");

            final BigDecimal latitude = rs.getBigDecimal("latitude");
            final BigDecimal longitude = rs.getBigDecimal("longitude");
            final LocalDate formationDate = JdbcSupport.getLocalDate(rs, "formationDate");

            final long responsibleUserId = rs.getLong("responsibleUserId");

            final Integer statusId = rs.getInt("status");
            final Integer size = rs.getInt("size");
            final LocalDate createdDate = JdbcSupport.getLocalDate(rs, "createdDate");

            final LocalTime meetingStartTime = JdbcSupport.getLocalTime(rs, "meetingStartTime");
            final LocalTime meetingEndTime = JdbcSupport.getLocalTime(rs, "meetingEndTime");

            EnumOptionData statusEnum = null;
            if (statusId != null) {
                statusEnum = CenterGroupEnumerations.groupStatusOptionData(statusId);
            }

            return CenterGroupData.instance(id, name, portfolioCenterId, portfolioCenterName, legacyGroupNumber, latitude, longitude,
                    formationDate, statusEnum, size, responsibleUserId, createdDate, meetingStartTime, meetingEndTime);
        }

        public String schema() {
            return this.schema;
        }

    }

}
