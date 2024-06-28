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
package org.apache.fineract.portfolio.cupo.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.cupo.data.CupoData;
import org.apache.fineract.portfolio.cupo.data.CupoTransactionData;
import org.apache.fineract.portfolio.cupo.domain.CupoStatus;
import org.apache.fineract.portfolio.cupo.domain.CupoTransactionType;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.group.service.GroupReadPlatformService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class CupoReadServiceImpl implements CupoReadService {

    private final JdbcTemplate jdbcTemplate;
    private final ClientReadPlatformService clientReadPlatformService;
    private final GroupReadPlatformService groupReadPlatformService;
    private final CurrencyReadPlatformService currencyReadPlatformService;

    public CupoReadServiceImpl(final RoutingDataSource dataSource, final ClientReadPlatformService clientReadPlatformService,
            final GroupReadPlatformService groupReadPlatformService, final CurrencyReadPlatformService currencyReadPlatformService) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.clientReadPlatformService = clientReadPlatformService;
        this.groupReadPlatformService = groupReadPlatformService;
        this.currencyReadPlatformService = currencyReadPlatformService;
    }

    @Override
    public List<CupoData> findAllActiveCuposByClientId(Long clientId, CupoStatus status, String currencyCode) {
        List<Object> params = new ArrayList<>();
        CupoMapper cupoMapper = new CupoMapper();
        String schemaSql = cupoMapper.schema();
        schemaSql += "where c.client_id = ? ";
        params.add(clientId);
        if (status.isValid()) {
            schemaSql += "and c.status_enum = ? ";
            params.add(status.getValue());
        }
        if (StringUtils.isNotEmpty(currencyCode)) {
            schemaSql += "and c.currency_code = ? ";
            params.add(currencyCode);
        }

        return this.jdbcTemplate.query(schemaSql, params.toArray(), cupoMapper);
    }

    @Override
    public List<CupoData> findAllActiveCuposByGroupId(Long groupId, CupoStatus status, String currencyCode) {
        List<Object> params = new ArrayList<>();
        CupoMapper cupoMapper = new CupoMapper();
        String schemaSql = cupoMapper.schema();
        schemaSql += "where c.group_id = ? ";
        params.add(groupId);
        if (status.isValid()) {
            schemaSql += "and c.status_enum = ?";
            params.add(status.getValue());
        }
        if (StringUtils.isNotEmpty(currencyCode)) {
            schemaSql += "and c.currency_code = ? ";
            params.add(currencyCode);
        }
        return this.jdbcTemplate.query(schemaSql, params.toArray(), cupoMapper);
    }

    @Override
    public CupoData findById(Long cupoId) {
        CupoMapper cupoMapper = new CupoMapper();
        String schemaSql = cupoMapper.schema();
        schemaSql += "where c.id = ?";
        return this.jdbcTemplate.queryForObject(schemaSql, cupoMapper, new Object[] { cupoId });
    }

    @Override
    public CupoData template(Long clientId, Long groupId) {
        ClientData clientData = null;
        GroupGeneralData groupGeneralData = null;
        if (clientId != null) {
            clientData = this.clientReadPlatformService.retrieveOneLookup(clientId);
        }
        if (groupId != null) {
            groupGeneralData = this.groupReadPlatformService.retrieveOneLookup(groupId);
        }
        Collection<CurrencyData> currencyOptions = this.currencyReadPlatformService.retrieveAllowedCurrencies();
        return CupoData.template(clientData, groupGeneralData, currencyOptions);
    }

    @Override
    public LocalDate getLastUseOfCupo(Long cupoId) {
        StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("SELECT max(l.disbursedon_date) ");
        sqlBuilder.append("FROM m_loan_charge lc ");
        sqlBuilder.append("join m_charge c on c.id = lc.charge_id ");
        sqlBuilder.append("join m_loan l on l.id = lc.loan_id ");
        sqlBuilder.append("join m_portfolio_account_associations la on la.loan_account_id = l.id ");
        sqlBuilder.append("where la.linked_cupo_id = ? and c.is_annual_fee_disburse = 1 and lc.is_penalty = 0 ");
        sqlBuilder.append("and c.charge_time_enum = 1 ");
        sqlBuilder.append("and disbursedon_date is not null");
        LocalDate lastUse = null;

        lastUse = this.jdbcTemplate.queryForObject(sqlBuilder.toString(), LocalDate.class, new Object[] { cupoId });
        return lastUse;
    }

    @Override
    public List<CupoTransactionData> getAllTransactionsFromCupo(Long cupoId) {
        CupoTransactionsMapper mapper = new CupoTransactionsMapper();
        String schemaSql = mapper.schema();
        schemaSql += "where ct.cupo_id = ? and ct.is_reversed = 0";
        return this.jdbcTemplate.query(schemaSql, mapper, new Object[] { cupoId });
    }

    private static final class CupoTransactionsMapper implements RowMapper<CupoTransactionData> {

        private final String schema;

        public CupoTransactionsMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(100);
            sqlBuilder.append("select ct.id, ");
            sqlBuilder.append("ct.cupo_id cupoId, ct.transaction_date transactionDate, ");
            sqlBuilder.append("ct.transaction_type_enum type, ct.amount amount, ");
            sqlBuilder.append("ct.is_reversed reversed, ct.loan_transaction_id loanTransactionId, ");
            sqlBuilder.append("ct.loan_id as loanId ");
            sqlBuilder.append("from m_cupo_transaction ct ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public CupoTransactionData mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long id = rs.getLong("id");
            Long cupoId = rs.getLong("cupoId");
            LocalDate transactionDate = JdbcSupport.getLocalDate(rs, "transactionDate");
            CupoTransactionType transactionType = CupoTransactionType.fromInt(rs.getInt("type"));
            BigDecimal amount = rs.getBigDecimal("amount");
            Long loanTransactiondId = rs.getLong("loanTransactionId");
            Long loanId = rs.getLong("loanId");
            return new CupoTransactionData(id, cupoId, transactionDate, CupoTransactionType.toDataEnum(transactionType), amount,
                    loanTransactiondId, loanId);
        }

        public String schema() {
            return schema;
        }
    }

    private static final class CupoMapper implements RowMapper<CupoData> {

        private final String schema;

        public CupoMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(100);
            sqlBuilder.append("select c.id, ");
            sqlBuilder.append("c.amount_submitted amountSubmitted, c.amount_approved amountApproved, ");
            sqlBuilder.append("c.amount amount, c.amount_available amountAvailable, ");
            sqlBuilder.append("c.amount_in_hold amountInHold, ");
            sqlBuilder.append("c.expiration_date expirationDate, c.approval_date approvalDate, ");
            sqlBuilder.append("cl.id clientId, cl.display_name as clientName, ");
            sqlBuilder.append("g.id groupId, g.display_name as groupName, c.currency_code currencyCode, ");
            sqlBuilder.append("c.status_enum status ");
            sqlBuilder.append("from m_cupo c ");
            sqlBuilder.append("left join m_client cl on cl.id = c.client_id ");
            sqlBuilder.append("left join m_group g on g.id = c.group_id ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public CupoData mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long id = rs.getLong("id");
            BigDecimal amountSubmitted = rs.getBigDecimal("amountSubmitted");
            BigDecimal amountApproved = rs.getBigDecimal("amountApproved");
            BigDecimal amount = rs.getBigDecimal("amount");
            BigDecimal amountAvailable = rs.getBigDecimal("amountAvailable");
            BigDecimal amountInHold = rs.getBigDecimal("amountInHold");
            LocalDate expirationDate = JdbcSupport.getLocalDate(rs, "expirationDate");
            LocalDate approvalDate = JdbcSupport.getLocalDate(rs, "approvalDate");
            Integer status = rs.getInt("status");
            CupoStatus statusEnum = CupoStatus.fromInt(status);

            Long clientId = JdbcSupport.getLong(rs, "clientId");
            String clientName = rs.getString("clientName");
            ClientData clientData = null;
            if (clientId != null) {
                clientData = ClientData.instance(clientId, clientName);
            }

            Long groupId = JdbcSupport.getLong(rs, "groupId");
            String groupName = rs.getString("groupName");
            GroupGeneralData groupGeneralData = null;
            if (groupId != null) {
                groupGeneralData = GroupGeneralData.lookup(groupId, null, groupName);
            }
            String currencyCode = rs.getString("currencyCode");
            CurrencyData currencyData = new CurrencyData(currencyCode);
            return CupoData.instance(id, amount, amountSubmitted, amountApproved, clientData, groupGeneralData, expirationDate,
                    approvalDate, CupoStatus.toDataEnum(statusEnum), amountInHold, amountAvailable, currencyData);
        }

        public String schema() {
            return this.schema;
        }
    }
}
