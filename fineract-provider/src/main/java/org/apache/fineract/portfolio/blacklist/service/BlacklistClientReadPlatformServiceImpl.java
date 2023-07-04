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
package org.apache.fineract.portfolio.blacklist.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.portfolio.blacklist.command.BlacklistDataValidator;
import org.apache.fineract.portfolio.blacklist.data.BlacklistClientData;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistEnumerations;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistStatus;
import org.apache.fineract.portfolio.client.service.ClientChargeWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BlacklistClientReadPlatformServiceImpl implements BlacklistClientReadPlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientChargeWritePlatformServiceJpaRepositoryImpl.class);

    private final PlatformSecurityContext context;
    private final BlacklistDataValidator dataValidator;
    private final LoanProductRepository loanProductRepository;
    private final ClientReadPlatformService clientReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final JdbcTemplate jdbcTemplate;
    private final PaginationHelper paginationHelper;
    private final ColumnValidator columnValidator;
    private final BlacklistMapper blacklistMapper = new BlacklistMapper();
    private final DatabaseSpecificSQLGenerator sqlGenerator;

    @Autowired
    public BlacklistClientReadPlatformServiceImpl(final PlatformSecurityContext context, final PaginationHelper paginationHelper,
            final DatabaseSpecificSQLGenerator sqlGenerator, final ColumnValidator columnValidator,
            final BlacklistDataValidator dataValidator, final LoanProductRepository loanProductRepository,
            final ClientReadPlatformService clientReadPlatformService,
            final CodeValueReadPlatformService codeValueReadPlatformService, final JdbcTemplate jdbcTemplate) {
        this.context = context;
        this.dataValidator = dataValidator;
        this.loanProductRepository = loanProductRepository;
        this.clientReadPlatformService = clientReadPlatformService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.jdbcTemplate = jdbcTemplate;
        this.paginationHelper = paginationHelper;
        this.sqlGenerator = sqlGenerator;
        this.columnValidator = columnValidator;

    }

    @Override
    public Page<BlacklistClientData> retrieveAll(SearchParameters searchParameters) {

        if (searchParameters != null && searchParameters.getStatus() != null
                && BlacklistStatus.fromString(searchParameters.getStatus()) == BlacklistStatus.INVALID) {
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final String defaultUserMessage = "The Status value '" + searchParameters.getStatus() + "' is not supported.";
            final ApiParameterError error = ApiParameterError.parameterError("validation.msg.client.status.value.is.not.supported",
                    defaultUserMessage, "status", searchParameters.getStatus());
            dataValidationErrors.add(error);
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
        List<Object> paramList = new ArrayList<>();
        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select " + sqlGenerator.calcFoundRows() + " ");
        sqlBuilder.append(this.blacklistMapper.schema());
        sqlBuilder.append(" where b.dpi is not null ");

        if (searchParameters != null) {

            final String extraCriteria = buildSqlStringFromBlacklistCriteria(this.blacklistMapper.schema(), searchParameters, paramList);

            if (StringUtils.isNotBlank(extraCriteria)) {
                sqlBuilder.append(" and (").append(extraCriteria).append(")");
            }

            if (searchParameters.isOrderByRequested()) {
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
        }
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), paramList.toArray(), this.blacklistMapper);
    }

    @Override
    public BlacklistClientData retrieveOne(Long blacklistId) {

        final String sql = "select " + this.blacklistMapper.schema() + " where b.id = ? and b.dpi is not null";
        final BlacklistClientData clientData = this.jdbcTemplate.queryForObject(sql, this.blacklistMapper, new Object[] { blacklistId });
        return clientData;

    }

    private String buildSqlStringFromBlacklistCriteria(String schemaSql, final SearchParameters searchParameters, List<Object> paramList) {

        String sqlSearch = searchParameters.getSqlSearch();
        final Long officeId = searchParameters.getOfficeId();
        final String externalId = searchParameters.getExternalId();
        final String displayName = searchParameters.getName();
        final String dpiNumber = searchParameters.getName();
        final String status = searchParameters.getStatus();

        String extraCriteria = "";
        if (sqlSearch != null) {
            sqlSearch = sqlSearch.replaceAll(" display_name ", " c.display_name ");
            sqlSearch = sqlSearch.replaceAll("display_name ", "c.display_name ");
            extraCriteria = " and (" + sqlSearch + ")";
            this.columnValidator.validateSqlInjection(schemaSql, sqlSearch);
        }

        if (officeId != null) {
            extraCriteria += " and c.office_id = ? ";
            paramList.add(officeId);
        }

        if (dpiNumber != null) {
            paramList.add(dpiNumber);
            extraCriteria += " and c.dpi = ? ";
        }

        if (displayName != null) {
            // extraCriteria += " and concatcoalesce(c.firstname, ''),
            // if(c.firstname > '',' ', '') , coalesce(c.lastname, '')) like "
            paramList.add("%" + displayName + "%");
            extraCriteria += " and c.display_name like ? ";
        }

        if (status != null) {
            BlacklistStatus blacklistStatus = BlacklistStatus.fromString(status);
            extraCriteria += " and b.status = " + blacklistStatus.getValue().toString() + " ";
        }

        if (StringUtils.isNotBlank(extraCriteria)) {
            extraCriteria = extraCriteria.substring(4);
        }
        return extraCriteria;
    }

    private static final class BlacklistMapper implements RowMapper<BlacklistClientData> {

        private final String schema;

        BlacklistMapper() {
            final StringBuilder builder = new StringBuilder(400);

            builder.append("b.id as id, b.dpi as dpiNumber, b.nit as nitNumber, b.agency_id as agencyId, ");
            builder.append("cv.id as typificationId, cv.code_description as typificationDescription, cv.code_value as typificationValue, ");
            builder.append("b.status, b.product_id as productId, b.product_code as productCode, ");
            builder.append("lp.name as productName, b.balance, b.disbursement_amount as disbursementAmount, au.firstname, au.lastname, ");
            builder.append("b.year, b.description, c.display_name as displayName ");
            builder.append("from m_client_blacklist b ");
            builder.append("inner join m_client c on c.dpi = b.dpi ");
            builder.append("inner join m_product_loan lp on lp.id = b.product_id ");
            builder.append("inner join m_code_value cv on cv.id = b.type_enum ");
            builder.append("inner join m_appuser au on au.id = b.added_by ");

            this.schema = builder.toString();
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public BlacklistClientData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final Integer statusEnum = JdbcSupport.getInteger(rs, "status");
            final EnumOptionData status = BlacklistEnumerations.status(statusEnum);

            Long typificationId = JdbcSupport.getLong(rs, "typificationId");
            String typificationDescription = rs.getString("typificationDescription");
            String typificationValue = rs.getString("typificationValue");
            final CodeValueData typification = CodeValueData.instance(typificationId, typificationDescription, typificationValue, true);

            final Long id = JdbcSupport.getLong(rs, "id");
            final Long productId = JdbcSupport.getLong(rs, "productId");
            final String dpiNumber = rs.getString("dpiNumber");
            final String nitNumber = rs.getString("nitNumber");
            final String agencyId = rs.getString("agencyId");
            final String productCode = rs.getString("productCode");
            final String productName = rs.getString("productName");
            final BigDecimal balance = rs.getBigDecimal("balance");
            final BigDecimal disbursementAmount = rs.getBigDecimal("disbursementAmount");
            final String addedBy = rs.getString("firstname") + " " + rs.getString("lastname");
            final String displayName = rs.getString("displayName");
            final String year = rs.getString("year");
            final String description = rs.getString("description");

            return BlacklistClientData.instance(id, displayName, status, typification, productId, dpiNumber, nitNumber, agencyId,
                    productCode, productName, balance, disbursementAmount, addedBy, year, description);

        }
    }

}
