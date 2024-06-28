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
package org.apache.fineract.organisation.committee.service;

import static org.apache.fineract.organisation.committee.service.CommitteeConstants.COMMITTEE_CODE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.committee.data.CommitteeData;
import org.apache.fineract.organisation.committee.data.CommitteeUserData;
import org.apache.fineract.organisation.committee.domain.CommitteeRepository;
import org.apache.fineract.organisation.committee.exception.CommitteeNotFoundException;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class CommitteeReadPlatformServiceImpl implements CommitteeReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;
    private final ColumnValidator columnValidator;
    private final AppUserReadPlatformService appUserReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final CommitteeRepository committeeRepository;
    private final PaginationHelper paginationHelper;

    @Autowired
    public CommitteeReadPlatformServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate,
            final ColumnValidator columnValidator, final DatabaseSpecificSQLGenerator sqlGenerator,
            final AppUserReadPlatformService appUserReadPlatformService, final CodeValueReadPlatformService codeValueReadPlatformService,
            final CommitteeRepository committeeRepository, final PaginationHelper paginationHelper) {
        this.context = context;
        this.columnValidator = columnValidator;
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.appUserReadPlatformService = appUserReadPlatformService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.committeeRepository = committeeRepository;
        this.paginationHelper = paginationHelper;
    }

    @Override
    public CommitteeData findByCommitteeId(Long committeeId) {
        try {
            this.context.authenticatedUser();

            CommitteeMapper committeeMapper = new CommitteeMapper();
            String schemaSql = "select " + committeeMapper.schema();

            final StringBuilder sqlBuilder = new StringBuilder(200);
            sqlBuilder.append(schemaSql);

            if (committeeId != null) {
                sqlBuilder.append(" where cu.committee_id = ?");
            }

            CommitteeData committeeData = this.jdbcTemplate.queryForObject(sqlBuilder.toString(), committeeMapper,
                    new Object[] { committeeId });

            // get the selected users in the committee
            Collection<CommitteeUserData> selectedUsers = retrieveCommitteeUsers(committeeId);

            // get the available users to set in the committe
            final Collection<AppUserData> availableAppUsers = this.appUserReadPlatformService.retrieveUsersForCommittees();
            final Collection<CommitteeUserData> availableUsers = new ArrayList<>();
            assembleUserToData(availableAppUsers, availableUsers);

            if (committeeData != null && selectedUsers != null) {
                committeeData.setSelectedUsers(selectedUsers);
                committeeData.setAvailableUsers(availableUsers);
            }

            return committeeData;
        } catch (EmptyResultDataAccessException e) {
            throw new CommitteeNotFoundException(committeeId);
        }
    }

    @Override
    public CommitteeData retrieveNewCommitteeTemplate() {
        this.context.authenticatedUser();

        final Collection<CodeValueData> committees = this.codeValueReadPlatformService.retrieveCodeValuesByCode(COMMITTEE_CODE);
        final Collection<AppUserData> availableAppUsers = this.appUserReadPlatformService.retrieveUsersForCommittees();

        final Collection<CommitteeUserData> availableUsers = new ArrayList<>();
        assembleUserToData(availableAppUsers, availableUsers);

        return CommitteeData.template(committees, availableUsers);
    }

    private void assembleUserToData(Collection<AppUserData> availableAppUsers, Collection<CommitteeUserData> availableUsers) {
        for (final AppUserData appUserData : availableAppUsers) {
            CommitteeUserData committeeUserData = CommitteeUserData.instance(appUserData.getId(), appUserData.getFirstname(),
                    appUserData.getLastname());
            availableUsers.add(committeeUserData);
        }
    }

    @Override
    public Page<CommitteeData> retrieveAll(SearchParameters searchParameters) {
        this.context.authenticatedUser();

        CommitteeMapper committeeMapper = new CommitteeMapper();
        String schemaSql = "select " + committeeMapper.schema();

        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append(schemaSql);

        if (searchParameters != null && searchParameters.isLimited()) {
            sqlBuilder.append(" ");
            if (searchParameters.isOffset()) {
                sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit(), searchParameters.getOffset()));
            } else {
                sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit()));
            }
        }

        Page<CommitteeData> committeesEntries = this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), null,
                committeeMapper);
        if (!committeesEntries.getPageItems().isEmpty()) {
            for (CommitteeData committeeData : committeesEntries.getPageItems()) {
                Collection<CommitteeUserData> selectedUsers = retrieveCommitteeUsers(committeeData.getId());
                committeeData.setSelectedUsers(selectedUsers);
            }
        }
        return committeesEntries;
    }

    private static final class CommitteeMapper implements RowMapper<CommitteeData> {

        private final String schema;

        public CommitteeMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append(" distinct cu.committee_id as id, cvCommittee.code_value as name ");
            sqlBuilder.append("from m_committee_user cu ");
            sqlBuilder.append("left join m_code_value cvCommittee on cu.committee_id = cvCommittee.id ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public CommitteeData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String name = rs.getString("name");

            Collection<CommitteeUserData> users = null;
            return CommitteeData.instance(id, name, users);
        }

        public String schema() {
            return this.schema;
        }

    }

    @Override
    public Collection<CommitteeUserData> retrieveCommitteeUsers(Long committeeId) {
        try {

            CommitteeUserMapper committeeUserMapper = new CommitteeUserMapper();
            String schemaSql = "select " + committeeUserMapper.schema();
            schemaSql += "where c.committee_id = ?";

            return jdbcTemplate.query(schemaSql, committeeUserMapper, new Object[] { committeeId });
        } catch (final EmptyResultDataAccessException e) {
            throw new CommitteeNotFoundException(committeeId, e);
        }
    }

    private static final class CommitteeUserMapper implements RowMapper<CommitteeUserData> {

        private final String schema;

        public CommitteeUserMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append(" distinct c.user_id as id, u.firstname as firstname, u.lastname as lastname ");
            sqlBuilder.append("from m_committee_user c ");
            sqlBuilder.append("left join m_appuser u on c.user_id = u.id ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public CommitteeUserData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long userId = rs.getLong("id");
            final String firstname = rs.getString("firstname");
            final String lastname = rs.getString("lastname");

            return CommitteeUserData.instance(userId, firstname, lastname);
        }

        public String schema() {
            return this.schema;
        }

    }

}
