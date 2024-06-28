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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.office.domain.OfficeHierarchyLevel;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.organisation.portfolio.data.PortfolioData;
import org.apache.fineract.organisation.portfolio.data.PortfolioPlanningData;
import org.apache.fineract.organisation.portfolio.exception.PortfolioNotFoundException;
import org.apache.fineract.organisation.supervision.data.SupervisionData;
import org.apache.fineract.organisation.supervision.service.SupervisionReadPlatformService;
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
    private final PlatformSecurityContext context;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final AppUserReadPlatformService appUserReadPlatformService;
    private final SupervisionReadPlatformService supervisionReadPlatformService;

    @Autowired
    public PortfolioReadPlatformServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate,
            final OfficeReadPlatformService officeReadPlatformService, final AppUserReadPlatformService appUserReadPlatformService,
            SupervisionReadPlatformService supervisionReadPlatformService) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
        this.officeReadPlatformService = officeReadPlatformService;
        this.appUserReadPlatformService = appUserReadPlatformService;
        this.supervisionReadPlatformService = supervisionReadPlatformService;
    }

    @Override
    public PortfolioData findById(Long portfolioId) {
        try {
            final String hierarchy = this.context.authenticatedUser().getOffice().getHierarchy();
            PortfolioMapper portfolioMapper = new PortfolioMapper();
            String schemaSql = "select " + portfolioMapper.schema();
            schemaSql += "where mp.id = ? GROUP BY mp.id";
            return this.jdbcTemplate.queryForObject(schemaSql, portfolioMapper, new Object[] { hierarchy, portfolioId });
        } catch (final EmptyResultDataAccessException e) {
            throw new PortfolioNotFoundException(portfolioId, e);
        }
    }

    @Override
    public PortfolioData retrieveNewPortfolioTemplate() {
        this.context.authenticatedUser();

        final Collection<OfficeData> parentOfficesOptions = officeReadPlatformService
                .retrieveOfficesByHierarchyLevel(Long.valueOf(OfficeHierarchyLevel.GERENCIA.getValue()));

        // retrieve list of users under agency hierarchy level as this is the user role to access these feature
        final List<AppUserData> appUsers = new ArrayList<>(
                this.appUserReadPlatformService.retrieveUsersUnderHierarchy(Long.valueOf(OfficeHierarchyLevel.CARTERA.getValue())));
        final String hierarchy = this.context.authenticatedUser().getOffice().getHierarchy();
        final Collection<SupervisionData> supervisionOptions = this.supervisionReadPlatformService.retrieveByOfficeHierarchy(hierarchy);
        return PortfolioData.template(parentOfficesOptions, appUsers, supervisionOptions);
    }

    @Override
    public Collection<PortfolioData> retrieveAllByUser(final String name) {
        AppUser currentUser = this.context.authenticatedUser();
        final String officeHierarchy = currentUser.getOffice().getHierarchy();
        List<Object> paramList = new ArrayList<>(Collections.singletonList(officeHierarchy));
        PortfolioMapper portfolioMapper = new PortfolioMapper();
        String schemaSql = "SELECT " + portfolioMapper.schema();
        if (name != null) {
            schemaSql += " WHERE mp.name LIKE ?";
            paramList.add("%" + name + "%");
        }
        schemaSql += " GROUP BY mp.id";
        return this.jdbcTemplate.query(schemaSql, portfolioMapper, paramList.toArray());
    }

    @Override
    public PortfolioPlanningData retrievePlanningByPortfolio(Long portfolioId) {
        AppUser currentUser = this.context.authenticatedUser();

        final List<Object> officeIds = new ArrayList<>();
        final Collection<OfficeData> parentOfficesOptions = officeReadPlatformService
                .retrieveOfficesByHierarchyLevel(Long.valueOf(OfficeHierarchyLevel.SUPERVISION.getValue()));
        parentOfficesOptions.forEach(parentOffice -> officeIds.add(parentOffice.getId()));
        String inSql = String.join(",", Collections.nCopies(officeIds.size(), "?"));

        try {
            PortfolioPlanningMapper portfolioPlanningMapper = new PortfolioPlanningMapper();
            String schemaSql = "select " + portfolioPlanningMapper.schema();
            schemaSql += "where p.id = ? ";

            // check if the user has a role as FACILITATOR
            Role facilitatorRole = currentUser.getRoles().stream().filter(x -> x.getName().startsWith(FACILITATOR_ROLE_START_WITH)) //
                    .findFirst().orElse(null);
            if (facilitatorRole != null) {
                schemaSql += " and p.responsible_user_id = ?";
                return this.jdbcTemplate.queryForObject(schemaSql, portfolioPlanningMapper, portfolioId, currentUser.getId());
            } else {
                schemaSql += " and p.linked_office_id in (%s)";
                Object[] args = ArrayUtils.addAll(new Object[] { portfolioId }, officeIds.toArray());
                return this.jdbcTemplate.queryForObject(String.format(schemaSql, inSql), portfolioPlanningMapper, args);
            }
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private static final class PortfolioMapper implements RowMapper<PortfolioData> {

        private final String schema;

        public PortfolioMapper() {
            this.schema = """
                    	mp.id AS id,
                    	mp.name AS name,
                    	ms.name AS supervisionName,
                    	ms.id AS supervisionId,
                    	mp.linked_office_id AS officeId,
                    	ounder.name AS officeName,
                    	mp.responsible_user_id AS responsibleUserId,
                    	responsible_user.firstname AS responsibleUserFirstName,
                    	responsible_user.lastname AS responsibleUserLastName
                    FROM m_office mo
                    INNER JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%') AND ounder.hierarchy LIKE CONCAT(?, '%')
                    INNER JOIN m_portfolio mp ON mp.linked_office_id = ounder.id
                    LEFT JOIN m_appuser responsible_user ON responsible_user.id = mp.responsible_user_id
                    LEFT JOIN m_supervision ms ON ms.id = mp.supervision_id
                    """;
        }

        @Override
        public PortfolioData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final String supervisionName = rs.getString("supervisionName");
            final Long supervisionId = JdbcSupport.getLong(rs, "supervisionId");
            final long parentId = rs.getLong("officeId");
            final String parentName = rs.getString("officeName");
            final long responsibleUserId = rs.getLong("responsibleUserId");
            final String responsibleUserName = rs.getString("responsibleUserFirstName") + " " + rs.getString("responsibleUserLastName");
            final PortfolioData portfolioData = PortfolioData.instance(id, name, parentId, parentName, responsibleUserId,
                    responsibleUserName, supervisionName);
            portfolioData.setSupervisionId(supervisionId);
            return portfolioData;
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
            sqlBuilder.append("left join m_office AS agency ON agency.id = supervision.parent_id ");
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
            final String responsibleUserName = rs.getString("userFirstName") + " " + rs.getString("userLastName");

            return PortfolioPlanningData.instance(id, name, parentId, parentName, responsibleUserId, agencyId, agencyName,
                    responsibleUserName);
        }

        public String schema() {
            return this.schema;
        }
    }
}
