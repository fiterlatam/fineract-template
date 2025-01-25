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
package org.apache.fineract.accounting.journalentry.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.common.AccountingEnumerations;
import org.apache.fineract.accounting.glaccount.domain.GLAccountType;
import org.apache.fineract.accounting.journalentry.api.JournalEntryJsonInputParams;
import org.apache.fineract.accounting.journalentry.data.JournalEntryData;
import org.apache.fineract.accounting.journalentry.data.JournalEntryDataValidator;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryType;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalEntryRunningBalanceUpdateServiceImpl implements JournalEntryRunningBalanceUpdateService {

    private final JdbcTemplate jdbcTemplate;

    private final OfficeRepositoryWrapper officeRepositoryWrapper;

    private final JournalEntryDataValidator dataValidator;

    private final FromJsonHelper fromApiJsonHelper;
    private final DatabaseSpecificSQLGenerator sqlGenerator;

    private final GLJournalEntryMapper entryMapper = new GLJournalEntryMapper();
    private final ConfigurationDomainService configurationDomainService;

    @Override
    @CronTarget(jobName = JobName.ACCOUNTING_RUNNING_BALANCE_UPDATE)
    public void updateRunningBalance() {
        String dateFinder = "select MIN(je.entry_date) as entityDate from acc_gl_journal_entry  je "
                + "where je.is_running_balance_calculated=false ";
        try {
            LocalDate entityDate = this.jdbcTemplate.queryForObject(dateFinder, LocalDate.class);
            if (entityDate!=null) updateOrganizationRunningBalance(entityDate);
        } catch (EmptyResultDataAccessException e) {
            log.debug("No results found for updation of running balance ");
        }
    }

    @Override
    public CommandProcessingResult updateOfficeRunningBalance(JsonCommand command) {
        this.dataValidator.validateForUpdateRunningbalance(command);
        final Long officeId = this.fromApiJsonHelper.extractLongNamed(JournalEntryJsonInputParams.OFFICE_ID.getValue(),
                command.parsedJson());
        CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder()
                .withCommandId(command.commandId());
        if (officeId == null) {
            updateRunningBalance();
        } else {
            this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);
            String dateFinder = "select MIN(je.entry_date) as entityDate " + "from acc_gl_journal_entry  je "
                    + "where je.is_running_balance_calculated=false  and je.office_id=?";
            try {
                LocalDate entityDate = this.jdbcTemplate.queryForObject(dateFinder, LocalDate.class, officeId);
                updateRunningBalance(officeId, entityDate);
            } catch (EmptyResultDataAccessException e) {
                log.debug("No results found for updation of office running balance with office id: {}", officeId);
            }
            commandProcessingResultBuilder.withOfficeId(officeId);
        }
        return commandProcessingResultBuilder.build();
    }

    private void updateOrganizationRunningBalance(LocalDate entityDate) {
        Map<Long, BigDecimal> runningBalanceMap = new HashMap<>(5);
        Map<Long, Map<Long, BigDecimal>> officesRunningBalance = new HashMap<>();

        final String organizationRunningBalanceQuery = """
                    SELECT
                    	je.organization_running_balance AS runningBalance,
                    	je.account_id AS accountId
                    FROM
                    	acc_gl_journal_entry je
                    	INNER JOIN ( SELECT max( id ) AS id FROM acc_gl_journal_entry WHERE entry_date < ? GROUP BY account_id, entry_date ) je2 ON je2.id = je.id
                    	INNER JOIN ( SELECT max( entry_date ) AS entrydt FROM acc_gl_journal_entry WHERE entry_date < ? GROUP BY account_id ) je3 ON je.entry_date = je3.entrydt
                    GROUP BY
                    	je.id
                    ORDER BY
                    	je.entry_date DESC LIMIT 10000;
                """;

        List<Map<String, Object>> list = jdbcTemplate.queryForList(organizationRunningBalanceQuery, // NOSONAR
                entityDate, entityDate);

        list.forEach(entry -> {
            Long accountId = Long.parseLong(entry.get("accountId").toString());
            runningBalanceMap.putIfAbsent(accountId, (BigDecimal) entry.get("runningBalance"));
        });

        final String offlineRunningBalanceQuery = """
                    SELECT
                     	je.office_running_balance AS runningBalance,
                     	je.account_id AS accountId,
                     	je.office_id AS officeId
                    FROM
                     	acc_gl_journal_entry je
                     	INNER JOIN ( SELECT max( id ) AS id FROM acc_gl_journal_entry WHERE entry_date < ? GROUP BY office_id, account_id, entry_date ) je2 ON je2.id = je.id
                     	INNER JOIN ( SELECT max( entry_date ) AS entrydt FROM acc_gl_journal_entry WHERE entry_date < ? GROUP BY office_id, account_id ) je3 ON je.entry_date = je3.entrydt
                    GROUP BY
                     	je.id
                    ORDER BY
                     	je.entry_date DESC LIMIT 10000;
                """;

        List<Map<String, Object>> officesRunningBalanceList = jdbcTemplate.queryForList(offlineRunningBalanceQuery, // NOSONAR
                entityDate, entityDate);

        officesRunningBalanceList.forEach(entry -> {
            Long accountId = Long.parseLong(entry.get("accountId").toString());
            Long officeId = Long.parseLong(entry.get("officeId").toString());
            officesRunningBalance.computeIfAbsent(officeId, k -> new HashMap<>()).putIfAbsent(accountId,
                    (BigDecimal) entry.get("runningBalance"));
        });

        // run a batch update of 1000 SQL statements at a time
        final Integer batchUpdateSize = 1000;

        // Batch update using JdbcTemplate with PreparedStatement
        long numberOfDaysToKeepRunningBalance = configurationDomainService.getNumberOfDaysToKeepRunningBalance();
        LocalDate endDate = entityDate.plusDays(numberOfDaysToKeepRunningBalance);
        String sqlString = numberOfDaysToKeepRunningBalance > 0 ? entryMapper.organizationRunningBalanceSchemaParts() : entryMapper.organizationRunningBalanceSchema();
        try (Stream<JournalEntryData> entryStream = jdbcTemplate.queryForStream(
                sqlString, entryMapper, entityDate, endDate)) {
                List<JournalEntryData> batch = new ArrayList<>();
                entryStream.forEach(entry -> {
                    batch.add(entry);
                    if (batch.size() == batchUpdateSize) {
                        processBatch(batch, jdbcTemplate, officesRunningBalance, runningBalanceMap);
                        batch.clear();
                    }
                });

                if (!batch.isEmpty()) {
                    processBatch(batch, jdbcTemplate, officesRunningBalance, runningBalanceMap);
                }
        }
    }

    private void processBatch(List<JournalEntryData> batch, JdbcTemplate jdbcTemplate,
            Map<Long, Map<Long, BigDecimal>> officesRunningBalance, Map<Long, BigDecimal> runningBalanceMap) {
        jdbcTemplate.batchUpdate("UPDATE acc_gl_journal_entry SET is_running_balance_calculated=true, "
                + "organization_running_balance = ?, office_running_balance = ? WHERE id = ?", batch, 1000, (ps, entryData) -> {
                    Map<Long, BigDecimal> officeRunningBalanceMap = officesRunningBalance.computeIfAbsent(entryData.getOfficeId(),
                            k -> new HashMap<>());

                    BigDecimal officeRunningBalance = calculateRunningBalance(entryData, officeRunningBalanceMap);
                    BigDecimal runningBalance = calculateRunningBalance(entryData, runningBalanceMap);

                    ps.setBigDecimal(1, runningBalance);
                    ps.setBigDecimal(2, officeRunningBalance);
                    ps.setLong(3, entryData.getId());
                });
    }

    private void updateRunningBalance(Long officeId, LocalDate entityDate) {
        Map<Long, BigDecimal> runningBalanceMap = new HashMap<>(5);

        final String offlineRunningBalanceQuery = "select je.office_running_balance as runningBalance,je.account_id as accountId from acc_gl_journal_entry je "
                + "inner join (select max(id) as id from acc_gl_journal_entry where office_id=?  and entry_date < ? group by account_id,entry_date) je2 ON je2.id = je.id "
                + "inner join (select max(entry_date) as date from acc_gl_journal_entry where office_id=? and entry_date < ? group by account_id) je3 ON je.entry_date = je3.date "
                + "group by je.id order by je.entry_date DESC " + sqlGenerator.limit(10000, 0);

        List<Map<String, Object>> list = jdbcTemplate.queryForList(offlineRunningBalanceQuery, // NOSONAR
                officeId, entityDate, officeId, entityDate);
        for (Map<String, Object> entries : list) {
            Long accountId = (Long) entries.get("accountId");
            if (!runningBalanceMap.containsKey(accountId)) {
                runningBalanceMap.put(accountId, (BigDecimal) entries.get("runningBalance"));
            }
        }
        List<JournalEntryData> entryDatas = jdbcTemplate.query(entryMapper.officeRunningBalanceSchema(), entryMapper, officeId, entityDate);
        String[] updateSql = new String[entryDatas.size()];
        int i = 0;
        for (JournalEntryData entryData : entryDatas) {
            BigDecimal runningBalance = calculateRunningBalance(entryData, runningBalanceMap);
            String sql = new StringBuilder().append("UPDATE acc_gl_journal_entry SET office_running_balance=").append(runningBalance)
                    .append(" WHERE id=").append(entryData.getId()).toString();
            updateSql[i++] = sql;
        }
        this.jdbcTemplate.batchUpdate(updateSql);
    }

    private BigDecimal calculateRunningBalance(JournalEntryData entry, Map<Long, BigDecimal> runningBalanceMap) {
        BigDecimal currentBalance = runningBalanceMap.getOrDefault(entry.getGlAccountId(), BigDecimal.ZERO);
        GLAccountType accountType = GLAccountType.fromInt(entry.getGlAccountType().getId().intValue());
        JournalEntryType entryType = JournalEntryType.fromInt(entry.getEntryType().getId().intValue());

        boolean isIncrease = (accountType == GLAccountType.ASSET && entryType.isDebitType())
                || (accountType == GLAccountType.EQUITY && entryType.isCreditType())
                || (accountType == GLAccountType.EXPENSE && entryType.isDebitType())
                || (accountType == GLAccountType.INCOME && entryType.isCreditType())
                || (accountType == GLAccountType.LIABILITY && entryType.isCreditType());

        BigDecimal updatedBalance = isIncrease ? currentBalance.add(entry.getAmount()) : currentBalance.subtract(entry.getAmount());
        runningBalanceMap.put(entry.getGlAccountId(), updatedBalance);
        return updatedBalance;
    }

    private static final class GLJournalEntryMapper implements RowMapper<JournalEntryData> {

        public String officeRunningBalanceSchema() {
            return "select je.id as id,je.account_id as glAccountId,je.type_enum as entryType,je.amount as amount, "
                    + "glAccount.classification_enum as classification,je.office_id as officeId "
                    + "from acc_gl_journal_entry je  JOIN acc_gl_account glAccount on je.account_id = glAccount.id "
                    + "WHERE je.office_id=? and je.entry_date >= ? order by je.entry_date,je.id";
        }

        public String organizationRunningBalanceSchema() {
            return "select je.id as id,je.account_id as glAccountId," + "je.type_enum as entryType,je.amount as amount, "
                    + "glAccount.classification_enum as classification,je.office_id as officeId  "
                    + "from acc_gl_journal_entry je  JOIN acc_gl_account glAccount on je.account_id = glAccount.id "
                    + "WHERE je.entry_date >= ? order by je.entry_date,je.id";
        }

        public String organizationRunningBalanceSchemaParts() {
            return "select je.id as id,je.account_id as glAccountId," + "je.type_enum as entryType,je.amount as amount, "
                    + "glAccount.classification_enum as classification,je.office_id as officeId  "
                    + "from acc_gl_journal_entry je  JOIN acc_gl_account glAccount on je.account_id = glAccount.id "
                    + "WHERE je.entry_date >= ? and je.entry_date < ? order by je.entry_date,je.id";
        }

        @Override
        public JournalEntryData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final Long glAccountId = rs.getLong("glAccountId");
            final Long officeId = rs.getLong("officeId");
            final int accountTypeId = JdbcSupport.getInteger(rs, "classification");
            final EnumOptionData accountType = AccountingEnumerations.gLAccountType(accountTypeId);
            final BigDecimal amount = rs.getBigDecimal("amount");
            final int entryTypeId = JdbcSupport.getInteger(rs, "entryType");
            final EnumOptionData entryType = AccountingEnumerations.journalEntryType(entryTypeId);

            return new JournalEntryData(id, officeId, null, null, glAccountId, null, accountType, null, entryType, amount, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null);
        }
    }

}
