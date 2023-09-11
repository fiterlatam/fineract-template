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
package org.apache.fineract.organisation.bankAccount.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.accounting.glaccount.service.GLAccountReadPlatformService;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.agency.data.AgencyData;
import org.apache.fineract.organisation.agency.service.AgencyReadPlatformService;
import org.apache.fineract.organisation.bank.data.BankData;
import org.apache.fineract.organisation.bank.service.BankReadPlatformService;
import org.apache.fineract.organisation.bankAccount.data.BankAccountData;
import org.apache.fineract.organisation.bankAccount.exception.BankAccountNotFoundException;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class BankAccountReadPlatformServiceImpl implements BankAccountReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;
    private final ColumnValidator columnValidator;
    private final AppUserReadPlatformService appUserReadPlatformService;

    private final OfficeReadPlatformService officeReadPlatformService;
    private final PaginationHelper paginationHelper;

    private final AgencyReadPlatformService agencyReadPlatformService;
    private final BankReadPlatformService bankReadPlatformService;
    private final GLAccountReadPlatformService glAccountReadPlatformService;

    @Autowired
    public BankAccountReadPlatformServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate,
            final ColumnValidator columnValidator, final DatabaseSpecificSQLGenerator sqlGenerator,
            final AppUserReadPlatformService appUserReadPlatformService, final OfficeReadPlatformService officeReadPlatformService,
            final PaginationHelper paginationHelper, final AgencyReadPlatformService agencyReadPlatformService,
            final BankReadPlatformService bankReadPlatformService, final GLAccountReadPlatformService glAccountReadPlatformService) {
        this.context = context;
        this.columnValidator = columnValidator;
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.appUserReadPlatformService = appUserReadPlatformService;
        this.officeReadPlatformService = officeReadPlatformService;
        this.paginationHelper = paginationHelper;
        this.agencyReadPlatformService = agencyReadPlatformService;
        this.bankReadPlatformService = bankReadPlatformService;
        this.glAccountReadPlatformService = glAccountReadPlatformService;
    }

    @Override
    public BankAccountData findById(Long bankAccountId) {
        try {
            this.context.authenticatedUser();

            BankAccountMapper bankAccountMapper = new BankAccountMapper();
            String schemaSql = "select " + bankAccountMapper.schema();
            schemaSql += "where ba.id = ?";

            return this.jdbcTemplate.queryForObject(schemaSql, bankAccountMapper, new Object[] { bankAccountId });
        } catch (final EmptyResultDataAccessException e) {
            throw new BankAccountNotFoundException(bankAccountId, e);
        }
    }

    @Override
    public BankAccountData retrieveNewBankAccountTemplate() {
        this.context.authenticatedUser();

        Collection<AgencyData> agencies = agencyReadPlatformService.retrieveAllByUser();
        Page<BankData> pagedBanks = bankReadPlatformService.retrieveAll(null);
        Collection<BankData> banks = pagedBanks != null ? pagedBanks.getPageItems() : new ArrayList<>();
        Collection<GLAccountData> glAccounts = glAccountReadPlatformService.retrieveAllEnabledDetailGLAccounts();

        return BankAccountData.template(agencies, banks, glAccounts);
    }

    @Override
    public Page<BankAccountData> retrieveAll(SearchParameters searchParameters) {
        this.context.authenticatedUser();

        BankAccountMapper bankAccountMapper = new BankAccountMapper();
        String schemaSql = "select " + bankAccountMapper.schema();

        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append(schemaSql);

        List<Object> paramList = new ArrayList<>();
        String extraCriteria = null;
        if (searchParameters != null) {
            extraCriteria = buildSqlStringFromBankAccountCriteria(bankAccountMapper.schema(), searchParameters, paramList);
        }

        if (StringUtils.isNotBlank(extraCriteria)) {
            sqlBuilder.append(" where (").append(extraCriteria).append(")");
        }

        if (searchParameters != null && searchParameters.isOrderByRequested()) {
            sqlBuilder.append(" order by ").append(searchParameters.getOrderBy());
            this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getOrderBy());
            if (searchParameters.isSortOrderProvided()) {
                sqlBuilder.append(' ').append(searchParameters.getSortOrder());
                this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getSortOrder());
            }
        }

        if (searchParameters.isLimited()) {
            sqlBuilder.append(" ");
            if (searchParameters.isOffset()) {
                sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit(), searchParameters.getOffset()));
            } else {
                sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit()));
            }
        }

        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), paramList.toArray(), bankAccountMapper);
    }

    private static final class BankAccountMapper implements RowMapper<BankAccountData> {

        private final String schema;

        public BankAccountMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append("ba.id as id, ba.account_number as accountNumber, ba.agency_id as agencyId, a.name as agencyName, "
                    + "ba.bank_id as bankId, b.name as bankName, b.code as bankCode, ba.gl_account_id as glAccountId, gl.name as glAccountName, ba.description as description ");

            sqlBuilder.append("from m_bank_account ba ");
            sqlBuilder.append("join m_agency a on a.id = ba.agency_id ");
            sqlBuilder.append("join m_bank b on b.id = ba.bank_id ");
            sqlBuilder.append("join acc_gl_account gl on gl.id = ba.gl_account_id ");

            this.schema = sqlBuilder.toString();
        }

        @Override
        public BankAccountData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");

            final Long agencyId = rs.getLong("agencyId");
            final String agencyName = rs.getString("agencyName");
            AgencyData agencyData = AgencyData.instance(agencyId, agencyName);

            final Long bankId = rs.getLong("bankId");
            final String bankName = rs.getString("bankName");
            final String bankCode = rs.getString("bankCode");
            BankData bankData = BankData.instance(bankId, bankCode, bankName);

            final Long glAccountId = rs.getLong("glAccountId");
            final String glAccountName = rs.getString("glAccountName");
            GLAccountData glAccountData = new GLAccountData(glAccountId, glAccountName, null);

            final Long accountNumber = rs.getLong("accountNumber");
            final String description = rs.getString("description");

            return BankAccountData.instance(id, accountNumber, agencyData, bankData, glAccountData, description);
        }

        public String schema() {
            return this.schema;
        }

    }

    private String buildSqlStringFromBankAccountCriteria(String schemaSql, final SearchParameters searchParameters,
            List<Object> paramList) {

        String sqlSearch = searchParameters.getSqlSearch();
        final Long accountNumber = searchParameters.getAccountNumber();
        final String bankName = searchParameters.getBankName();
        final String bankCode = searchParameters.getBankCode();

        String extraCriteria = "";
        if (sqlSearch != null) {
            extraCriteria = " and (" + sqlSearch + ")";
            this.columnValidator.validateSqlInjection(schemaSql, sqlSearch);
        }

        if (accountNumber != null) {
            paramList.add("%" + accountNumber + "%");
            extraCriteria += " or ba.account_number like ? ";
        }

        if (bankName != null) {
            paramList.add("%" + bankName + "%");
            extraCriteria += " or b.name like ? ";
        }

        if (bankCode != null) {
            paramList.add("%" + bankCode + "%");
            extraCriteria += " or b.code like ? ";
        }

        if (StringUtils.isNotBlank(extraCriteria)) {
            extraCriteria = extraCriteria.substring(4);
        }
        return extraCriteria;
    }

}
