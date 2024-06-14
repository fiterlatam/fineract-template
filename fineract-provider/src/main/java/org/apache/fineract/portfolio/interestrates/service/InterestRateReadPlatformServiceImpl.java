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
package org.apache.fineract.portfolio.interestrates.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.portfolio.floatingrates.domain.InterestRateType;
import org.apache.fineract.portfolio.interestrates.data.InterestRateData;
import org.apache.fineract.portfolio.interestrates.data.InterestRateHistoryData;
import org.apache.fineract.portfolio.interestrates.exception.InterestRateException;
import org.apache.fineract.portfolio.loanproduct.data.MaximumCreditRateConfigurationData;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class InterestRateReadPlatformServiceImpl implements InterestRateReadPlatformService {

    private final InterestRateRowMapper interestRateRowMapper = new InterestRateRowMapper();
    private final InterestRateHistoryRowMapper interestRateHistoryRowMapper = new InterestRateHistoryRowMapper();
    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final ColumnValidator columnValidator;
    private final PaginationHelper paginationHelper;
    private final LoanProductReadPlatformService loanProductReadPlatformService;

    @Override
    public List<InterestRateData> retrieveAll() {
        final String sql = "SELECT " + this.interestRateRowMapper.schema();
        return this.jdbcTemplate.query(sql, this.interestRateRowMapper);
    }

    @Override
    public List<InterestRateData> retrieveBySearchParams(SearchParameters searchParameters) {
        String sql = "SELECT " + this.interestRateRowMapper.schema();
        final BigDecimal currentRate = searchParameters.getCurrentRate();
        List<Object> paramList = new ArrayList<>();
        if (currentRate != null) {
            paramList.add(currentRate);
            sql = sql + " WHERE mir.current_rate > ?";
        }
        return this.jdbcTemplate.query(sql, this.interestRateRowMapper, paramList.toArray());
    }

    @Override
    public InterestRateData retrieveOne(final Long interestRateId) {
        try {
            final String sql = "SELECT " + this.interestRateRowMapper.schema() + " WHERE mir.id = ?";
            return this.jdbcTemplate.queryForObject(sql, this.interestRateRowMapper, new Object[] { interestRateId });
        } catch (final EmptyResultDataAccessException e) {
            log.error(e.getMessage(), e);
            throw new InterestRateException(interestRateId);
        }
    }

    @Override
    public InterestRateData retrieveTemplate() {
        final List<EnumOptionData> interestRateTypeOptions = List.of(InterestRateType.REGULAR.asEnumOptionData(),
                InterestRateType.OVERDUE.asEnumOptionData());
        final MaximumCreditRateConfigurationData maximumCreditRateConfigurationData = this.loanProductReadPlatformService
                .retrieveMaximumCreditRateConfigurationData();
        return InterestRateData.builder().interestRateTypeOptions(interestRateTypeOptions)
                .maximumCreditRateConfiguration(maximumCreditRateConfigurationData).build();
    }

    @Override
    public List<InterestRateData> retrieveInterestRates(SearchParameters searchParameters) {
        final List<Object> paramList = new ArrayList<>(Collections.singletonList(searchParameters.getActive()));
        String sql = "SELECT " + this.interestRateRowMapper.schema() + " WHERE mir.is_active = ? ";
        final Integer interestRateTypeId = searchParameters.getInterestRateTypeId();
        if (interestRateTypeId != null) {
            sql = sql + " AND mir.interest_rate_type_id = ? ";
            paramList.add(interestRateTypeId);
        }
        return this.jdbcTemplate.query(sql, this.interestRateRowMapper, paramList.toArray());
    }

    public static final class InterestRateRowMapper implements RowMapper<InterestRateData> {

        public String schema() {
            return """
                    mir.id AS id,
                    mir."name" AS name,
                    mir.interest_rate_type_id AS "interestRateTypeId",
                    mir.current_rate AS "currentRate",
                    mir.appliedon_date AS "appliedOnDate",
                    mir.is_active AS active,
                    CONCAT(created_by.firstname, ' ', created_by.lastname) AS "createdBy",
                    mir.created_on_utc AS "createdDate",
                    CONCAT(last_modified_by.firstname, ' ', last_modified_by.lastname) AS "lastModifiedBy",
                    mir.last_modified_on_utc AS "lastModifiedDate"
                    FROM m_interest_rate mir
                    LEFT JOIN m_appuser created_by ON created_by.id = mir.created_by
                    LEFT JOIN m_appuser last_modified_by ON last_modified_by.id = mir.last_modified_by
                    """;
        }

        @Override
        public InterestRateData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final EnumOptionData interestRateType = InterestRateType.fromInt(rs.getInt("interestRateTypeId")).asEnumOptionData();
            final BigDecimal currentRate = rs.getBigDecimal("currentRate");
            final boolean active = rs.getBoolean("active");
            LocalDate appliedOnDate = JdbcSupport.getLocalDate(rs, "appliedOnDate");
            final String createdBy = rs.getString("createdBy");
            LocalDate createdDate = JdbcSupport.getLocalDate(rs, "createdDate");
            final String lastModifiedBy = rs.getString("lastModifiedBy");
            LocalDate lastModifiedDate = JdbcSupport.getLocalDate(rs, "lastModifiedDate");
            return InterestRateData.builder().id(id).name(name).currentRate(currentRate).active(active).appliedOnDate(appliedOnDate)
                    .createdBy(createdBy).createdDate(createdDate).lastModifiedBy(lastModifiedBy).lastModifiedDate(lastModifiedDate)
                    .interestRateType(interestRateType).build();
        }
    }

    @Override
    public Page<InterestRateHistoryData> retrieveHistory(SearchParameters searchParameters) {
        final List<Object> paramList = new ArrayList<>(Collections.singletonList(searchParameters.getInterestRateId()));
        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("SELECT ").append(sqlGenerator.calcFoundRows()).append(" ");
        sqlBuilder.append(this.interestRateHistoryRowMapper.schema());
        sqlBuilder.append(" WHERE mirh.interest_rate_id = ? ");
        if (searchParameters.isOrderByRequested()) {
            sqlBuilder.append(" ORDER BY ").append(searchParameters.getOrderBy());
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
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), paramList.toArray(),
                this.interestRateHistoryRowMapper);
    }

    private static final class InterestRateHistoryRowMapper implements RowMapper<InterestRateHistoryData> {

        public String schema() {
            return """
                    mirh.id AS id,
                    mirh.interest_rate_id AS "interestRateId",
                    mirh.interest_rate_type_id AS "interestRateTypeId",
                    mirh."name" AS name,
                    mirh.current_rate AS "currentRate",
                    mirh.appliedon_date AS "appliedOnDate",
                    mirh.is_active AS active,
                    CONCAT(created_by.firstname, ' ', created_by.lastname) AS "createdBy",
                    mirh.created_on_utc AS "createdDate"
                    FROM m_interest_rate_history mirh
                    LEFT JOIN m_appuser created_by ON created_by.id = mirh.created_by
                    """;
        }

        @Override
        public InterestRateHistoryData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final Long interestRateId = rs.getLong("interestRateId");
            final EnumOptionData interestRateType = InterestRateType.fromInt(rs.getInt("interestRateTypeId")).asEnumOptionData();
            final String name = rs.getString("name");
            final BigDecimal currentRate = rs.getBigDecimal("currentRate");
            final boolean active = rs.getBoolean("active");
            LocalDate appliedOnDate = JdbcSupport.getLocalDate(rs, "appliedOnDate");
            final String createdBy = rs.getString("createdBy");
            LocalDate createdDate = JdbcSupport.getLocalDate(rs, "createdDate");
            return InterestRateHistoryData.builder().id(id).name(name).interestRateId(interestRateId).currentRate(currentRate)
                    .active(active).appliedOnDate(appliedOnDate).createdBy(createdBy).createdDate(createdDate)
                    .interestRateType(interestRateType).build();
        }
    }
}
