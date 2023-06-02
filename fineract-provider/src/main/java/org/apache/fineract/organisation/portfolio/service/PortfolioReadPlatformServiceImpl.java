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
package org.apache.fineract.organisation.portfolio.service;

import static org.apache.fineract.useradministration.service.AppUserConstants.FACILITATOR_ROLE_START_WITH;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.office.domain.OfficeHierarchyLevel;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.organisation.portfolio.data.PortfolioData;
import org.apache.fineract.organisation.portfolio.data.PortfolioPlanningData;
import org.apache.fineract.organisation.portfolio.exception.PortfolioNotFoundException;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class PortfolioReadPlatformServiceImpl implements PortfolioReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;
    private final ColumnValidator columnValidator;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final AppUserReadPlatformService appUserReadPlatformService;

    @Autowired
    public PortfolioReadPlatformServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate,
            final ColumnValidator columnValidator, final DatabaseSpecificSQLGenerator sqlGenerator,
            final OfficeReadPlatformService officeReadPlatformService, final AppUserReadPlatformService appUserReadPlatformService) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
        this.columnValidator = columnValidator;
        this.sqlGenerator = sqlGenerator;
        this.officeReadPlatformService = officeReadPlatformService;
        this.appUserReadPlatformService = appUserReadPlatformService;
    }

    @Override
    public PortfolioData findById(Long portfolioId) {
        try {
            this.context.authenticatedUser();

            PortfolioMapper portfolioMapper = new PortfolioMapper();
            String schemaSql = "select " + portfolioMapper.schema();
            schemaSql += "where p.id = ?";

            return this.jdbcTemplate.queryForObject(schemaSql, portfolioMapper, new Object[] { portfolioId });
        } catch (final EmptyResultDataAccessException e) {
            throw new PortfolioNotFoundException(portfolioId, e);
        }
    }

    @Override
    public PortfolioData retrieveNewPortfolioTemplate() {
        this.context.authenticatedUser();

        final Collection<OfficeData> parentOfficesOptions = officeReadPlatformService
                .retrieveOfficesByHierarchyLevel(Long.valueOf(OfficeHierarchyLevel.SUPERVISION.getValue()));

        // retrieve list of users under agency hierarchy level as this is the user role to access these feature
        final List<AppUserData> appUsers = new ArrayList<>(
                this.appUserReadPlatformService.retrieveUsersUnderHierarchy(Long.valueOf(OfficeHierarchyLevel.CARTERA.getValue())));

        return PortfolioData.template(parentOfficesOptions, appUsers);
    }

    @Override
    public Collection<PortfolioData> retrieveAllByUser() {
        AppUser currentUser = this.context.authenticatedUser();

        final List<Object> officeIds = new ArrayList<>();
        final Collection<OfficeData> parentOfficesOptions = officeReadPlatformService
                .retrieveOfficesByHierarchyLevel(Long.valueOf(OfficeHierarchyLevel.SUPERVISION.getValue()));
        parentOfficesOptions.forEach(parentOffice -> officeIds.add(parentOffice.getId()));

        String inSql = String.join(",", Collections.nCopies(officeIds.size(), "?"));
        PortfolioMapper portfolioMapper = new PortfolioMapper();
        String schemaSql = "select " + portfolioMapper.schema();

        // TODO: this must be improved by adding types in Roles
        // check if the user has a role as FACILITATOR
        Role facilitatorRole = currentUser.getRoles().stream().filter(x -> x.getName().startsWith(FACILITATOR_ROLE_START_WITH)) //
                .findFirst().orElse(null);

        if (facilitatorRole != null) {
            schemaSql += "where p.responsible_user_id = ?";
            return this.jdbcTemplate.query(String.format(schemaSql, inSql), portfolioMapper, currentUser.getId());
        } else {
            schemaSql += "where p.linked_office_id in (%s)";
            return this.jdbcTemplate.query(String.format(schemaSql, inSql), portfolioMapper, officeIds.toArray());
        }
    }

    @Override
    public PortfolioPlanningData retrievePlanningByPortfolio(Long portfolioId) {
        AppUser currentUser = this.context.authenticatedUser();

        PortfolioPlanningMapper portfolioPlanningMapper = new PortfolioPlanningMapper();
        String schemaSql = "select " + portfolioPlanningMapper.schema();
        schemaSql += "where p.id = ? and p.responsible_user_id = ?";

        return this.jdbcTemplate.queryForObject(schemaSql, portfolioPlanningMapper, new Object[] { portfolioId, currentUser.getId() });
    }

    private static final class PortfolioMapper implements RowMapper<PortfolioData> {

        private final String schema;

        public PortfolioMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append("p.id as id, p.name as name, ");
            sqlBuilder.append("p.linked_office_id as parentRegionId, region.name as parentRegionName, ");
            sqlBuilder.append("p.responsible_user_id as responsibleUserId, ru.firstname as userFirstName, ru.lastname as userLastName ");
            sqlBuilder.append("from m_portfolio p left join m_office AS region ON region.id = p.linked_office_id ");
            sqlBuilder.append("left join m_appuser ru on ru.id = p.responsible_user_id ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public PortfolioData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String name = rs.getString("name");

            final long parentId = rs.getLong("parentRegionId");
            final String parentName = rs.getString("parentRegionName");

            final long responsibleUserId = rs.getLong("responsibleUserId");

            return PortfolioData.instance(id, name, parentId, parentName, responsibleUserId);
        }

        public String schema() {
            return this.schema;
        }

    }

    private static final class PortfolioPlanningMapper implements RowMapper<PortfolioPlanningData> {

        private final String schema;

        public PortfolioPlanningMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append("p.id as id, p.name as name, ");
            sqlBuilder.append(
                    "p.linked_office_id as parentSupervisionId, supervision.name as parentSupervisionName, agency.id as agencyId, agency.name as agencyName, ");
            sqlBuilder.append("p.responsible_user_id as responsibleUserId, ru.firstname as userFirstName, ru.lastname as userLastName ");
            sqlBuilder.append("from m_portfolio p left join m_office AS supervision ON supervision.id = p.linked_office_id ");
            sqlBuilder.append("left join m_appuser ru on ru.id = p.responsible_user_id ");
            sqlBuilder.append("left join fineract_default.m_office AS agency ON agency.id = supervision.parent_id ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public PortfolioPlanningData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String name = rs.getString("name");

            final long parentId = rs.getLong("parentSupervisionId");
            final String parentName = rs.getString("parentSupervisionName");

            final long agencyId = rs.getLong("agencyId");
            final String agencyName = rs.getString("agencyName");

            final long responsibleUserId = rs.getLong("responsibleUserId");

            return PortfolioPlanningData.instance(id, name, parentId, parentName, responsibleUserId, agencyId, agencyName);
        }

        public String schema() {
            return this.schema;
        }

    }
}
