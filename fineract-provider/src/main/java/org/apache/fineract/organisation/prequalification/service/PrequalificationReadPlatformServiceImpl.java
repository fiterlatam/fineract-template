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
package org.apache.fineract.organisation.prequalification.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import org.apache.fineract.organisation.prequalification.command.PrequalificationDataValidator;
import org.apache.fineract.organisation.prequalification.data.GroupPrequalificationData;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationsEnumerations;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatus;
import org.apache.fineract.portfolio.client.service.ClientChargeWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PrequalificationReadPlatformServiceImpl implements PrequalificationReadPlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientChargeWritePlatformServiceJpaRepositoryImpl.class);

    private final PlatformSecurityContext context;
    private final PrequalificationDataValidator dataValidator;
    private final LoanProductRepository loanProductRepository;
    private final ClientReadPlatformService clientReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final JdbcTemplate jdbcTemplate;
    private final PaginationHelper paginationHelper;
    private final ColumnValidator columnValidator;
    private final PrequalificationsMapper prequalificationsMapper = new PrequalificationsMapper();
    private final DatabaseSpecificSQLGenerator sqlGenerator;

    @Autowired
    public PrequalificationReadPlatformServiceImpl(final PlatformSecurityContext context, final PaginationHelper paginationHelper,
                                                   final DatabaseSpecificSQLGenerator sqlGenerator, final ColumnValidator columnValidator,
                                                   final PrequalificationDataValidator dataValidator, final LoanProductRepository loanProductRepository,
                                                   final ClientReadPlatformService clientReadPlatformService, final CodeValueReadPlatformService codeValueReadPlatformService,
                                                   final JdbcTemplate jdbcTemplate) {
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
    public Page<GroupPrequalificationData> retrieveAll(SearchParameters searchParameters) {

        if (searchParameters != null && searchParameters.getStatus() != null
                && PrequalificationStatus.fromString(searchParameters.getStatus()) == PrequalificationStatus.INVALID) {
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final String defaultUserMessage = "The status value '" + searchParameters.getStatus() + "' is not supported.";
            final ApiParameterError error = ApiParameterError.parameterError("validation.msg.client.status.value.is.not.supported",
                    defaultUserMessage, "status", searchParameters.getStatus());
            dataValidationErrors.add(error);
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
        List<Object> paramList = new ArrayList<>();
        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select " + sqlGenerator.calcFoundRows() + " ");
        sqlBuilder.append(this.prequalificationsMapper.schema());
        sqlBuilder.append(" where g.prequalification_number is not null ");

        if (searchParameters != null) {

            final String extraCriteria = buildSqlStringFromBlacklistCriteria(searchParameters, paramList);

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
            } else {
                sqlBuilder.append(" order by g.id desc ");
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
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), paramList.toArray(), this.prequalificationsMapper);
    }

    @Override
    public GroupPrequalificationData retrieveOne(Long groupId) {

        final String sql = "select " + this.prequalificationsMapper.schema() + " where g.id = ? ";
        final GroupPrequalificationData clientData = this.jdbcTemplate.queryForObject(sql, this.prequalificationsMapper, new Object[] { groupId });
        return clientData;

    }

    private String buildSqlStringFromBlacklistCriteria(final SearchParameters searchParameters, List<Object> paramList) {

        String sqlSearch = searchParameters.getSqlSearch();
        final Long officeId = searchParameters.getOfficeId();
        final String dpiNumber = searchParameters.getName();
        final String status = searchParameters.getStatus();

        String extraCriteria = "";
        if (sqlSearch != null) {
            extraCriteria = " and (b.client_name like '%" + sqlSearch + "%' OR b.dpi='" + sqlSearch + "') ";
        }

        if (officeId != null) {
            extraCriteria += " and c.office_id = ? ";
            paramList.add(officeId);
        }

        if (dpiNumber != null) {
            paramList.add(dpiNumber);
            extraCriteria += " and g.prequalification_number like %?% ";
        }

        if (status != null) {
            PrequalificationStatus prequalificationStatus = PrequalificationStatus.fromString(status);
            extraCriteria += " and g.status = " + prequalificationStatus.getValue().toString() + " ";
        }

        if (StringUtils.isNotBlank(extraCriteria)) {
            extraCriteria = extraCriteria.substring(4);
        }
        return extraCriteria;
    }

    private static final class PrequalificationsMapper implements RowMapper<GroupPrequalificationData> {

        private final String schema;

        PrequalificationsMapper() {
            final StringBuilder builder = new StringBuilder(400);

            builder.append("g.id as id, g.prequalification_number as prequalificationNumber, g.status, g.created_at, " +
                    "ma.name as agencyName, cg.name as groupName, pc.name as centerName, mp.name as portfolioName, ");
            builder.append("lp.name as productName, au.firstname, au.lastname ");
            builder.append("from m_prequalification_group g ");
            builder.append("inner join m_appuser au on au.id = g.added_by ");
            builder.append("inner join m_product_loan lp on g.product_id = lp.id ");
            builder.append("inner join m_agency ma on g.agency_id = ma.id ");
            builder.append("inner join m_center_group cg on cg.id = g.group_id ");
            builder.append("inner join m_portfolio_center pc on pc.id = cg.portfolio_center_id ");
            builder.append("inner join m_portfolio mp on mp.id = pc.portfolio_id ");

            this.schema = builder.toString();
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public GroupPrequalificationData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final Integer statusEnum = JdbcSupport.getInteger(rs, "status");
            final EnumOptionData status = PreQualificationsEnumerations.status(statusEnum);


            final Long id = JdbcSupport.getLong(rs, "id");
            final String prequalificationNumber = rs.getString("prequalificationNumber");
            final String groupName = rs.getString("groupName");
            final String agencyName = rs.getString("agencyName");
            final String centerName = rs.getString("centerName");
            final String portfolioName = rs.getString("portfolioName");
            final String productName = rs.getString("productName");
            final LocalDate createdAt = JdbcSupport.getLocalDate(rs, "created_at");

            final String addedBy = rs.getString("firstname") + " " + rs.getString("lastname");

            return GroupPrequalificationData.instance(id, prequalificationNumber, status, agencyName, portfolioName, centerName, groupName, productName, addedBy, createdAt);

        }
    }

}
