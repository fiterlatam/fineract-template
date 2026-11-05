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
package org.apache.fineract.portfolio.savings.negativeholds.service;

import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.GUARANTEE_PRODUCT_NAME_LIKE;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.MOTIVO_SIN_CUENTA;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountNotFoundException;
import org.apache.fineract.portfolio.savings.negativeholds.data.GuaranteeAccountSnapshot;
import org.apache.fineract.portfolio.savings.negativeholds.data.GuaranteeTransactionRow;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldsValidationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NegativeHoldsReadPlatformServiceImpl implements NegativeHoldsReadPlatformService {

    private static final int ACTIVE = SavingsAccountStatusType.ACTIVE.getValue();
    private static final int HOLD = SavingsAccountTransactionType.AMOUNT_HOLD.getValue();
    private static final int RELEASE = SavingsAccountTransactionType.AMOUNT_RELEASE.getValue();

    private static final String ACCOUNT_SELECT = "select sa.id as savingsId, sa.account_no as accountNo, sa.client_id as clientId, "
            + "c.account_no as clientAccountNo, c.display_name as clientName, sa.account_balance_derived as balance, "
            + "sa.total_savings_amount_on_hold as onHold, "
            + "(select count(*) from m_savings_account sa2 join m_savings_product sp2 on sp2.id = sa2.product_id "
            + "  where sa2.client_id = sa.client_id and sa2.status_enum = " + ACTIVE + " and sp2.name like '" + GUARANTEE_PRODUCT_NAME_LIKE
            + "') as guaranteeAccounts " + "from m_savings_account sa join m_savings_product sp on sp.id = sa.product_id "
            + "left join m_client c on c.id = sa.client_id " + "where sp.name like '" + GUARANTEE_PRODUCT_NAME_LIKE + "' ";

    private static final String TRANSACTIONS_SELECT = "select tr.id, tr.savings_account_id, tr.transaction_type_enum, tr.amount, "
            + "tr.transaction_date, tr.created_date, tr.running_balance_derived, tr.overdraft_amount_derived, tr.is_reversed, "
            + "tr.is_reversal, tr.release_id_of_hold_amount, " + "case when tr.transaction_type_enum in (" + HOLD + ", " + RELEASE + ") "
            + "  then (select count(*) from acc_gl_journal_entry je where je.savings_transaction_id = tr.id) else 0 end as journal_count "
            + "from m_savings_account_transaction tr where tr.savings_account_id = ? "
            + "order by tr.transaction_date, tr.created_date, tr.id";

    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @Autowired
    public NegativeHoldsReadPlatformServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate,
            final NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    public List<NegativeHoldsValidationData> validate(final List<String> clientAccountNos, final Long savingsId,
            final boolean includeMovements) {
        this.context.authenticatedUser();
        final List<NegativeHoldsValidationData> results = new ArrayList<>();
        if (savingsId != null) {
            results.add(NegativeHoldsValidator.validate(loadSnapshot(savingsId, false), includeMovements));
        }
        for (final String clientAccountNo : clientAccountNos) {
            final List<Long> accountIds = findGuaranteeAccountIdsByClientAccountNo(clientAccountNo);
            if (accountIds.isEmpty()) {
                results.add(NegativeHoldsValidationData.withoutAccount(clientAccountNo,
                        MOTIVO_SIN_CUENTA + ": no se encontró cuenta de Garantías activa para el cliente " + clientAccountNo));
            }
            for (final Long accountId : accountIds) {
                results.add(NegativeHoldsValidator.validate(loadSnapshot(accountId, false), includeMovements));
            }
        }
        return results;
    }

    @Override
    public List<NegativeHoldsValidationData> report(final boolean includeMovements) {
        this.context.authenticatedUser();
        final String sql = "select distinct sa.id from m_savings_account sa join m_savings_product sp on sp.id = sa.product_id "
                + "join m_savings_account_transaction tr on tr.savings_account_id = sa.id " + "where sp.name like '"
                + GUARANTEE_PRODUCT_NAME_LIKE + "' and sa.status_enum = " + ACTIVE + " and tr.transaction_type_enum = " + HOLD
                + " and tr.is_reversed = 0 and tr.running_balance_derived < 0 order by sa.id";
        final List<Long> ids = this.jdbcTemplate.queryForList(sql, Long.class);
        return ids.stream().map(id -> NegativeHoldsValidator.validate(loadSnapshot(id, false), includeMovements))
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findGuaranteeAccountIdsByClientAccountNo(final String clientAccountNo) {
        final String sql = "select sa.id from m_savings_account sa join m_savings_product sp on sp.id = sa.product_id "
                + "join m_client c on c.id = sa.client_id where c.account_no = ? and sa.status_enum = " + ACTIVE + " and sp.name like '"
                + GUARANTEE_PRODUCT_NAME_LIKE + "' order by sa.id";
        return this.jdbcTemplate.queryForList(sql, Long.class, clientAccountNo);
    }

    @Override
    public GuaranteeAccountSnapshot loadSnapshot(final Long savingsId, final boolean lockAccount) {
        if (lockAccount) {
            // Bloquea la cuenta mientras se revalida y corrige (otras operaciones sobre la cuenta esperan).
            this.jdbcTemplate.queryForList("select id from m_savings_account where id = ? for update", Long.class, savingsId);
        }
        final List<Map<String, Object>> accounts = this.jdbcTemplate.queryForList(ACCOUNT_SELECT + "and sa.id = ?", savingsId);
        if (accounts.isEmpty()) {
            throw new SavingsAccountNotFoundException(savingsId);
        }
        final Map<String, Object> a = accounts.get(0);

        final List<GuaranteeTransactionRow> transactions = this.jdbcTemplate.query(TRANSACTIONS_SELECT, (rs, rowNum) -> mapTransaction(rs),
                savingsId);

        final Set<Long> ownIds = transactions.stream().map(GuaranteeTransactionRow::getId).collect(Collectors.toSet());
        final Set<Long> missingReleaseIds = transactions.stream().filter(t -> t.isHold() && t.getReleaseIdOfHoldAmount() != null)
                .map(GuaranteeTransactionRow::getReleaseIdOfHoldAmount).filter(id -> !ownIds.contains(id)).collect(Collectors.toSet());
        final Map<Long, Long> foreign = missingReleaseIds.isEmpty() ? Collections.emptyMap() : findForeignReleases(missingReleaseIds);

        return new GuaranteeAccountSnapshot(toLong(a.get("savingsId")), (String) a.get("accountNo"), toLong(a.get("clientId")),
                (String) a.get("clientAccountNo"), (String) a.get("clientName"), (java.math.BigDecimal) a.get("balance"),
                (java.math.BigDecimal) a.get("onHold"), ((Number) a.get("guaranteeAccounts")).intValue(), transactions, foreign);
    }

    private Map<Long, Long> findForeignReleases(final Set<Long> releaseIds) {
        final Map<Long, Long> result = new HashMap<>();
        this.namedJdbcTemplate.query("select id, savings_account_id from m_savings_account_transaction where id in (:ids)",
                new MapSqlParameterSource("ids", releaseIds), rs -> {
                    result.put(rs.getLong("id"), rs.getLong("savings_account_id"));
                });
        return result;
    }

    private static GuaranteeTransactionRow mapTransaction(final ResultSet rs) throws SQLException {
        long releaseId = rs.getLong("release_id_of_hold_amount");
        final Long release = rs.wasNull() ? null : releaseId;
        return new GuaranteeTransactionRow(rs.getLong("id"), rs.getLong("savings_account_id"), rs.getInt("transaction_type_enum"),
                rs.getBigDecimal("amount"), JdbcSupport.getLocalDate(rs, "transaction_date"),
                JdbcSupport.getLocalDateTime(rs, "created_date"), rs.getBigDecimal("running_balance_derived"),
                rs.getBigDecimal("overdraft_amount_derived"), rs.getBoolean("is_reversed"), rs.getBoolean("is_reversal"), release,
                rs.getLong("journal_count"));
    }

    private static Long toLong(final Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
