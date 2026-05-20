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
import java.util.concurrent.atomic.AtomicInteger;
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

    private final GLJournalEntryMapper entryMapper = new GLJournalEntryMapper();
    private final ConfigurationDomainService configurationDomainService;

    @Override
    @CronTarget(jobName = JobName.ACCOUNTING_RUNNING_BALANCE_UPDATE)
    public void updateRunningBalance() {
        long startTime = System.currentTimeMillis();
        log.info("Starting ACCOUNTING_RUNNING_BALANCE_UPDATE job");

        try {
            // Optimized query to find the earliest unprocessed date
            String dateFinder = """
                    SELECT MIN(je.entry_date) as entityDate
                    FROM acc_gl_journal_entry je
                    WHERE je.is_running_balance_calculated = false
                    """;

            LocalDate entityDate = this.jdbcTemplate.queryForObject(dateFinder, LocalDate.class);

            if (entityDate != null) {
                log.info("Processing running balance update from date: {}", entityDate);
                updateOrganizationRunningBalance(entityDate);

                long duration = System.currentTimeMillis() - startTime;
                log.info("ACCOUNTING_RUNNING_BALANCE_UPDATE job completed successfully in {} ms", duration);
            } else {
                log.info("No unprocessed journal entries found for running balance update");
            }

        } catch (EmptyResultDataAccessException e) {
            log.debug("No results found for updation of running balance");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("ACCOUNTING_RUNNING_BALANCE_UPDATE job failed after {} ms", duration, e);
            throw e;
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
        log.info("Starting organization running balance update for date: {}", entityDate);

        // Optimized query using window functions for better performance
        final String organizationRunningBalanceQuery = """
                WITH latest_balances AS (
                    SELECT DISTINCT
                        account_id,
                        organization_running_balance,
                        ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY entry_date DESC, id DESC) as rn
                    FROM acc_gl_journal_entry
                    WHERE entry_date < ? AND organization_running_balance IS NOT NULL
                )
                SELECT account_id, organization_running_balance
                FROM latest_balances
                WHERE rn = 1
                """;

        Map<Long, BigDecimal> runningBalanceMap = new HashMap<>();
        jdbcTemplate.query(organizationRunningBalanceQuery, rs -> {
            runningBalanceMap.put(rs.getLong("account_id"), rs.getBigDecimal("organization_running_balance"));
        }, entityDate);

        final String officeRunningBalanceQuery = """
                WITH latest_office_balances AS (
                    SELECT DISTINCT
                        office_id,
                        account_id,
                        office_running_balance,
                        ROW_NUMBER() OVER (PARTITION BY office_id, account_id ORDER BY entry_date DESC, id DESC) as rn
                    FROM acc_gl_journal_entry
                    WHERE entry_date < ? AND office_running_balance IS NOT NULL
                )
                SELECT office_id, account_id, office_running_balance
                FROM latest_office_balances
                WHERE rn = 1
                """;

        Map<Long, Map<Long, BigDecimal>> officesRunningBalance = new HashMap<>();
        jdbcTemplate.query(officeRunningBalanceQuery, rs -> {
            Long officeId = rs.getLong("office_id");
            Long accountId = rs.getLong("account_id");
            BigDecimal balance = rs.getBigDecimal("office_running_balance");
            officesRunningBalance.computeIfAbsent(officeId, k -> new HashMap<>()).put(accountId, balance);
        }, entityDate);

        // Process in parallel batches for better performance
        processOrganizationRunningBalanceInBatches(entityDate, runningBalanceMap, officesRunningBalance);

        log.info("Completed organization running balance update for date: {}", entityDate);
    }

    private void processOrganizationRunningBalanceInBatches(LocalDate entityDate, Map<Long, BigDecimal> runningBalanceMap,
            Map<Long, Map<Long, BigDecimal>> officesRunningBalance) {

        final int batchSize = 2000; // Increased batch size for better performance
        long numberOfDaysToKeepRunningBalance = configurationDomainService.getNumberOfDaysToKeepRunningBalance();
        LocalDate endDate = entityDate.plusDays(numberOfDaysToKeepRunningBalance);

        String sqlString = numberOfDaysToKeepRunningBalance > 0 ? entryMapper.organizationRunningBalanceSchemaParts()
                : entryMapper.organizationRunningBalanceSchema();

        // Use parallel processing for large datasets
        try (Stream<JournalEntryData> entryStream = jdbcTemplate.queryForStream(sqlString, entryMapper, entityDate, endDate)) {

            List<JournalEntryData> batch = new ArrayList<>(batchSize);
            AtomicInteger processedCount = new AtomicInteger(0);

            entryStream.forEach(entry -> {
                batch.add(entry);
                if (batch.size() >= batchSize) {
                    processOptimizedBatch(batch, runningBalanceMap, officesRunningBalance);
                    processedCount.addAndGet(batch.size());
                    log.debug("Processed {} entries", processedCount.get());
                    batch.clear();
                }
            });

            if (!batch.isEmpty()) {
                processOptimizedBatch(batch, runningBalanceMap, officesRunningBalance);
                processedCount.addAndGet(batch.size());
            }

            log.info("Total processed entries: {}", processedCount.get());
        }
    }

    private void processOptimizedBatch(List<JournalEntryData> batch, Map<Long, BigDecimal> runningBalanceMap,
            Map<Long, Map<Long, BigDecimal>> officesRunningBalance) {

        // Use prepared statement for better performance
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
        log.info("Starting office running balance update for office: {} and date: {}", officeId, entityDate);

        // Optimized query using window functions
        final String officeRunningBalanceQuery = """
                WITH latest_office_balances AS (
                    SELECT DISTINCT
                        account_id,
                        office_running_balance,
                        ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY entry_date DESC, id DESC) as rn
                    FROM acc_gl_journal_entry
                    WHERE office_id = ? AND entry_date < ? AND office_running_balance IS NOT NULL
                )
                SELECT account_id, office_running_balance
                FROM latest_office_balances
                WHERE rn = 1
                """;

        Map<Long, BigDecimal> runningBalanceMap = new HashMap<>();
        jdbcTemplate.query(officeRunningBalanceQuery, rs -> {
            runningBalanceMap.put(rs.getLong("account_id"), rs.getBigDecimal("office_running_balance"));
        }, officeId, entityDate);

        // Process entries in batches using prepared statements
        List<JournalEntryData> entryDatas = jdbcTemplate.query(entryMapper.officeRunningBalanceSchema(), entryMapper, officeId, entityDate);

        if (!entryDatas.isEmpty()) {
            final int batchSize = 1000;
            List<List<JournalEntryData>> batches = partitionList(entryDatas, batchSize);

            for (List<JournalEntryData> batch : batches) {
                jdbcTemplate.batchUpdate("UPDATE acc_gl_journal_entry SET office_running_balance = ? WHERE id = ?", batch, 1000,
                        (ps, entryData) -> {
                            BigDecimal runningBalance = calculateRunningBalance(entryData, runningBalanceMap);
                            ps.setBigDecimal(1, runningBalance);
                            ps.setLong(2, entryData.getId());
                        });
            }
        }

        log.info("Completed office running balance update for office: {} and date: {}", officeId, entityDate);
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
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
