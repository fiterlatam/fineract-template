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
package org.apache.fineract.portfolio.exchange.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.portfolio.exchange.data.ExchangeData;
import org.apache.fineract.portfolio.exchange.domain.Exchange;
import org.apache.fineract.portfolio.exchange.domain.ExchangeRepositoryWrapper;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ExchangeReadPlatformServiceImpl implements ExchangeReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PaginationHelper paginationHelper;
    private AppUserReadPlatformService userReadPlatformService;
    private ExchangeRepositoryWrapper repositoryWrapper;

    @Autowired
    public ExchangeReadPlatformServiceImpl(final RoutingDataSource dataSource, AppUserReadPlatformService userReadPlatformService,
            ExchangeRepositoryWrapper repositoryWrapper, PaginationHelper paginationHelper) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.userReadPlatformService = userReadPlatformService;
        this.repositoryWrapper = repositoryWrapper;
        this.paginationHelper = paginationHelper;
    }

    @Override
    public Page<ExchangeData> retrieveAll(final SearchParameters searchParameters) {
        ExchangeMapper mapper = new ExchangeMapper();
        List<Object> paramList = new ArrayList<>();
        final StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append(mapper.schema());
        if (searchParameters != null) {
            if (searchParameters.isOrderByRequested()) {
                sqlBuilder.append(" order by ").append(searchParameters.getOrderBy());
                if (searchParameters.isSortOrderProvided()) {
                    sqlBuilder.append(' ').append(searchParameters.getSortOrder());
                }
            }

            if (searchParameters.isLimited()) {
                sqlBuilder.append(" limit ").append(searchParameters.getLimit());
                if (searchParameters.isOffset()) {
                    sqlBuilder.append(" offset ").append(searchParameters.getOffset());
                }
            }
        }

        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), paramList.toArray(), mapper);
    }

    @Override
    public ExchangeData retrieveOne(Long id) {
        final Exchange exchange = this.repositoryWrapper.findOneWithNotFoundDetection(id);
        AppUser createdBy = exchange.getCreatedBy();
        AppUserData createdByUserData = this.userReadPlatformService.retrieveUser(createdBy.getId());

        ExchangeData exchangeData = ExchangeData.instance(exchange.getId(), exchange.getDestinationCurrency(), exchange.getOriginCurrency(),
                exchange.getExchangeRate(), exchange.getValidFrom(), exchange.getCreatedOn(), createdByUserData, createdByUserData.getId());

        return exchangeData;
    }

    @Override
    public ExchangeData retrieveTemplate() {
        return ExchangeData.template();
    }

    private static final class ExchangeMapper implements RowMapper<ExchangeData> {

        private final String schema;

        ExchangeMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(100);
            sqlBuilder.append(
                    "select ex.id, ex.MonedaDestino, ex.MonedaOrigen, ex.TipoCambio, ex.AplicaDesde, ex.FechaIngreso, ex.UsuarioIngreso");
            sqlBuilder.append(" from m_exchange ex");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public ExchangeData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Integer destinationCurrency = rs.getInt("MonedaDestino");
            final Integer originCurrency = rs.getInt("MonedaOrigen");
            final BigDecimal exchangeRate = rs.getBigDecimal("TipoCambio");
            final Date validFrom = rs.getDate("AplicaDesde");
            final Date createdOn = rs.getTimestamp("FechaIngreso");
            final Long createdById = rs.getLong("UsuarioIngreso");
            final Long id = rs.getLong("id");

            return ExchangeData.instance(id, destinationCurrency, originCurrency, exchangeRate, validFrom, createdOn, null, createdById);
        }

        public String schema() {
            return this.schema;
        }
    }
}
