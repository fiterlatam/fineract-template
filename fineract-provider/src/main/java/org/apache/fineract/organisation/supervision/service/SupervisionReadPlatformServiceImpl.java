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
package org.apache.fineract.organisation.supervision.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.agency.data.AgencyData;
import org.apache.fineract.organisation.agency.service.AgencyReadPlatformServiceImpl;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.office.domain.OfficeHierarchyLevel;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.organisation.supervision.data.SupervisionData;
import org.apache.fineract.organisation.supervision.exception.SupervisionNotFoundException;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class SupervisionReadPlatformServiceImpl implements SupervisionReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;
    private final ColumnValidator columnValidator;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final AppUserReadPlatformService appUserReadPlatformService;
    private final AgencyReadPlatformServiceImpl agencyReadPlatformService;

    @Autowired
    public SupervisionReadPlatformServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate,
            final ColumnValidator columnValidator, final DatabaseSpecificSQLGenerator sqlGenerator,
            final OfficeReadPlatformService officeReadPlatformService, final AppUserReadPlatformService appUserReadPlatformService,
            AgencyReadPlatformServiceImpl agencyReadPlatformService) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
        this.columnValidator = columnValidator;
        this.sqlGenerator = sqlGenerator;
        this.officeReadPlatformService = officeReadPlatformService;
        this.appUserReadPlatformService = appUserReadPlatformService;
        this.agencyReadPlatformService = agencyReadPlatformService;
    }

    @Override
    public SupervisionData findById(Long supervisionId) {
        try {
            this.context.authenticatedUser();

            SupervisionMapper supervisionMapper = new SupervisionMapper();
            String schemaSql = "select " + supervisionMapper.schema();
            schemaSql += "where s.id = ?";

            return this.jdbcTemplate.queryForObject(schemaSql, supervisionMapper, new Object[] { supervisionId });
        } catch (final EmptyResultDataAccessException e) {
            throw new SupervisionNotFoundException(supervisionId, e);
        }
    }

    @Override
    public SupervisionData retrieveNewSupervisionTemplate() {
        this.context.authenticatedUser();

        final Collection<OfficeData> parentOfficesOptions = officeReadPlatformService
                .retrieveOfficesByHierarchyLevel(Long.valueOf(OfficeHierarchyLevel.GERENCIA.getValue()));

        final List<AppUserData> appUsers = new ArrayList<>(
                this.appUserReadPlatformService.retrieveUsersUnderHierarchy(Long.valueOf(OfficeHierarchyLevel.SUPERVISION.getValue())));
        final String hierarchy = this.context.authenticatedUser().getOffice().getHierarchy();
        final Collection<AgencyData> agencyOptions = this.agencyReadPlatformService.retrieveByOfficeHierarchy(hierarchy);
        return SupervisionData.template(parentOfficesOptions, appUsers, agencyOptions);
    }

    @Override
    public Collection<SupervisionData> retrieveAllByUser() {
        this.context.authenticatedUser();

        final List<Object> officeIds = new ArrayList<>();
        final Collection<OfficeData> parentOfficesOptions = officeReadPlatformService
                .retrieveOfficesByHierarchyLevel(Long.valueOf(OfficeHierarchyLevel.AGENCIA.getValue()));
        parentOfficesOptions.forEach(parentOffice -> officeIds.add(parentOffice.getId()));

        String inSql = String.join(",", Collections.nCopies(officeIds.size(), "?"));
        SupervisionMapper supervisionMapper = new SupervisionMapper();
        String schemaSql = "select " + supervisionMapper.schema();
        schemaSql += "where s.linked_office_id in (%s)";

        return this.jdbcTemplate.query(String.format(schemaSql, inSql), supervisionMapper, officeIds.toArray());
    }

    private static final class SupervisionMapper implements RowMapper<SupervisionData> {

        private final String schema;

        public SupervisionMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append("s.id as id, s.name as name, ma.name as agencyName, ma.id as agencyId, ");
            sqlBuilder.append("s.linked_office_id as parentRegionId, region.name as parentRegionName, ");
            sqlBuilder.append("s.responsible_user_id as responsibleUserId, ru.firstname as userFirstName, ru.lastname as userLastName ");
            sqlBuilder.append("from m_supervision s left join m_office AS region ON region.id = s.linked_office_id ");
            sqlBuilder.append("left join m_appuser ru on ru.id = s.responsible_user_id ");
            sqlBuilder.append("left join m_agency ma on ma.id = s.agency_id ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public SupervisionData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final String agencyName = rs.getString("agencyName");
            final Long agencyId = JdbcSupport.getLong(rs, "agencyId");
            final long parentId = rs.getLong("parentRegionId");
            final String parentName = rs.getString("parentRegionName");
            final long responsibleUserId = rs.getLong("responsibleUserId");
            final SupervisionData supervision = SupervisionData.instance(id, name, parentId, parentName, responsibleUserId, agencyName);
            supervision.setAgencyId(agencyId);
            return supervision;

        }

        public String schema() {
            return this.schema;
        }

    }
}
