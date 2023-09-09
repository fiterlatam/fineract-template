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
package org.apache.fineract.organisation.bank.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.bank.data.BankData;
import org.apache.fineract.organisation.bank.exception.BankNotFoundException;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class BankReadPlatformServiceImpl implements BankReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;
    private final ColumnValidator columnValidator;
    private final AppUserReadPlatformService appUserReadPlatformService;

    private final OfficeReadPlatformService officeReadPlatformService;
    private final PaginationHelper paginationHelper;

    @Autowired
    public BankReadPlatformServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate,
            final ColumnValidator columnValidator, final DatabaseSpecificSQLGenerator sqlGenerator,
            final AppUserReadPlatformService appUserReadPlatformService, final OfficeReadPlatformService officeReadPlatformService,
            final PaginationHelper paginationHelper) {
        this.context = context;
        this.columnValidator = columnValidator;
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.appUserReadPlatformService = appUserReadPlatformService;
        this.officeReadPlatformService = officeReadPlatformService;
        this.paginationHelper = paginationHelper;
    }

    @Override
    public BankData findById(Long bankId) {
        try {
            this.context.authenticatedUser();

            BankMapper bankMapper = new BankMapper();
            String schemaSql = "select " + bankMapper.schema();
            schemaSql += "where b.id = ?";

            return this.jdbcTemplate.queryForObject(schemaSql, bankMapper, new Object[] { bankId });
        } catch (final EmptyResultDataAccessException e) {
            throw new BankNotFoundException(bankId, e);
        }
    }

    @Override
    public BankData retrieveNewBankTemplate() {
        this.context.authenticatedUser();

        return BankData.template();
    }

    @Override
    public Page<BankData> retrieveAll(SearchParameters searchParameters) {
        this.context.authenticatedUser();

        BankReadPlatformServiceImpl.BankMapper bankMapper = new BankReadPlatformServiceImpl.BankMapper();
        String schemaSql = "select " + bankMapper.schema();

        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append(schemaSql);

        if (searchParameters.isLimited()) {
            sqlBuilder.append(" ");
            if (searchParameters.isOffset()) {
                sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit(), searchParameters.getOffset()));
            } else {
                sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit()));
            }
        }

        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), null, bankMapper);
    }

    private static final class BankMapper implements RowMapper<BankData> {

        private final String schema;

        public BankMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append("b.id as id, b.code as code, b.name as name ");

            sqlBuilder.append("from m_bank b ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public BankData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final String code = rs.getString("code");

            return BankData.instance(id, code, name);
        }

        public String schema() {
            return this.schema;
        }

    }

}
