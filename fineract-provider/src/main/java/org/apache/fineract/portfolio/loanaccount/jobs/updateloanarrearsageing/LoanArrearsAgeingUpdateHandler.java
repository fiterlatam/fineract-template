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
package org.apache.fineract.portfolio.loanaccount.jobs.updateloanarrearsageing;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockLevel;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingEnum;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingsRepositoryWrapper;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.service.ClientWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.domain.LoanBlockingReasonRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;
import org.apache.fineract.portfolio.loanaccount.service.LoanArrearsAgingService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanArrearsAgeingUpdateHandler {

    public static final String BLOCKING_REASON_NAME = BlockingReasonSettingEnum.CLIENT_MORA.getDatabaseString();
    private static final String BLOCKING_COMMENT = "Cliente bloqueado por defecto";
    private static final String UNBLOCKING_COMMENT = "Cliente desbloqueado por defecto";//
    private static final String BLOCKING_REASON_LISTAS_DE_CONTROL = "LISTAS DE CONTROL";
    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final LoanArrearsAgingService loanArrearsAgingService;
    private final ClientWritePlatformService clientWritePlatformService;
    private final BlockingReasonSettingsRepositoryWrapper blockingReasonSettingsRepositoryWrapper;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanBlockingReasonRepository loanBlockingReasonRepository;

    public void truncateLoanArrearsAgingDetails() {
        jdbcTemplate.execute("truncate table m_loan_arrears_aging");
    }

    private void deleteLoanArrearsAgingDetails(List<Long> loanIds) {
        // delete existing record for loan from m_loan_arrears_aging table
        for (Long loanId : loanIds) {
            jdbcTemplate.update("delete from m_loan_arrears_aging where loan_id=?", loanId);
        }
    }

    public void updateLoanArrearsAgeingDetailsForAllLoans(boolean updateStatuses) {
        if (updateStatuses) {
            log.info("Starting Update Loan Arrears Ageing Job");
        }
        truncateLoanArrearsAgingDetails();
        String insertSQLStatement = buildQueryForInsertAgeingDetails(Boolean.TRUE);
        List<String> insertStatements = updateLoanArrearsAgeingDetailsWithOriginalScheduleForAllLoans();
        insertStatements.add(0, insertSQLStatement);
        final int[] records = this.jdbcTemplate.batchUpdate(insertStatements.toArray(new String[0]));
        if (log.isInfoEnabled()) {
            int result = 0;
            for (int record : records) {
                result += record;
            }
            log.info("Records affected by updateLoanArrearsAgeingDetails: {}", result);
        }

        if (updateStatuses) {
            log.info("Updating Client and Loan Statuses...");
            handleBlockingAfterAreasAging();
            handleUnBlockingAfterArrearsAging();
            handleBlockingReasonCredit();
            handleUnblockingReasonCreditAfterArrearsAging();
            log.info("Done updating Client and Loan Statuses");
            log.info("Update Loan Arrears Ageing Job is successfully completed");
        }
    }

    public void updateLoanArrearsAgeingDetails(List<Long> loanIdsForUpdate) {

        deleteLoanArrearsAgingDetails(loanIdsForUpdate);
        String insertSQLStatement = buildQueryForInsertAgeingDetails(Boolean.FALSE);
        List<Object[]> batch = new ArrayList<Object[]>();
        if (!loanIdsForUpdate.isEmpty()) {
            for (Long loanId : loanIdsForUpdate) {
                Object[] values = new Object[] { loanId };
                batch.add(values);
            }
        }
        final int[] recordsUpdatedWithoutOriginalSchedule = this.jdbcTemplate.batchUpdate(insertSQLStatement, batch);
        int[] recordsUpdatedWithOriginalSchedule = new int[0];
        List<String> insertStatements = updateLoanArrearsAgeingDetailsWithOriginalSchedule(loanIdsForUpdate);
        if (!insertStatements.isEmpty()) {
            recordsUpdatedWithOriginalSchedule = this.jdbcTemplate.batchUpdate(insertStatements.toArray(new String[0]));

        }

        handleBlockingAfterAreasAging();
        handleUnBlockingAfterArrearsAging();
        handleBlockingReasonCredit();
        handleUnblockingReasonCreditAfterArrearsAging();
        if (log.isDebugEnabled()) {
            int result = 0;
            for (int recordWithoutOriginalSchedule : recordsUpdatedWithoutOriginalSchedule) {
                result += recordWithoutOriginalSchedule;
            }
            if (recordsUpdatedWithOriginalSchedule.length > 0) {
                for (int recordWithOriginalSchedule : recordsUpdatedWithOriginalSchedule) {
                    result += recordWithOriginalSchedule;
                }
            }
            log.debug("Records affected by updateLoanArrearsAgeingDetails: {}", result);
        }

    }

    private String buildQueryForInsertAgeingDetails(boolean isForAllLoans) {
        final StringBuilder insertSqlStatementBuilder = new StringBuilder(900);
        final String principalOverdueCalculationSql = "SUM(COALESCE(mr.principal_amount, 0) - coalesce(mr.principal_completed_derived, 0) - coalesce(mr.principal_writtenoff_derived, 0))";
        final String interestOverdueCalculationSql = "SUM(COALESCE(mr.interest_amount, 0) - coalesce(mr.interest_writtenoff_derived, 0) - coalesce(mr.interest_waived_derived, 0) - "
                + "coalesce(mr.interest_completed_derived, 0))";
        final String feeChargesOverdueCalculationSql = "SUM(COALESCE(mr.fee_charges_amount, 0) - coalesce(mr.fee_charges_writtenoff_derived, 0) - "
                + "coalesce(mr.fee_charges_waived_derived, 0) - coalesce(mr.fee_charges_completed_derived, 0))";
        final String penaltyChargesOverdueCalculationSql = "SUM(COALESCE(mr.penalty_charges_amount, 0) - coalesce(mr.penalty_charges_writtenoff_derived, 0) - "
                + "coalesce(mr.penalty_charges_waived_derived, 0) - coalesce(mr.penalty_charges_completed_derived, 0))";

        final String amountsSummationQuery = "WITH overdue_amounts AS (SELECT mr.loan_id as loanId, " + principalOverdueCalculationSql
                + " as principal_overdue, " + interestOverdueCalculationSql + " as interest_overdue, " + feeChargesOverdueCalculationSql
                + " as fee_charges_overdue, " + penaltyChargesOverdueCalculationSql + " as penalty_charges_overdue, "
                + " MIN(mr.duedate) AS overdue_since_date" + " FROM m_loan_repayment_schedule mr "
                + " INNER JOIN m_loan ml ON ml.id = mr.loan_id " + " WHERE     mr.completed_derived IS FALSE AND mr.duedate < "
                + sqlGenerator.subDate(sqlGenerator.currentBusinessDate(), "COALESCE(ml.grace_on_arrears_ageing, 0)", "day")
                + "    GROUP BY mr.loan_id) ";

        insertSqlStatementBuilder.append(amountsSummationQuery);
        insertSqlStatementBuilder.append(
                "INSERT INTO m_loan_arrears_aging(loan_id,principal_overdue_derived,interest_overdue_derived,fee_charges_overdue_derived,penalty_charges_overdue_derived,total_overdue_derived,overdue_since_date_derived)");
        insertSqlStatementBuilder.append("SELECT ml.id AS loanId,");
        insertSqlStatementBuilder.append(" COALESCE(oa.principal_overdue, 0) AS principal_overdue_derived,");
        insertSqlStatementBuilder.append(" COALESCE(oa.interest_overdue, 0) AS interest_overdue_derived,");
        insertSqlStatementBuilder.append(" COALESCE(oa.fee_charges_overdue, 0) AS fee_charges_overdue_derived,");
        insertSqlStatementBuilder.append(" COALESCE(oa.penalty_charges_overdue, 0) AS penalty_charges_overdue_derived,");
        insertSqlStatementBuilder.append(
                " COALESCE(oa.principal_overdue, 0) + COALESCE(oa.interest_overdue, 0) + COALESCE(oa.fee_charges_overdue, 0) + COALESCE(oa.penalty_charges_overdue, 0) AS total_overdue_derived,");
        insertSqlStatementBuilder.append(" oa.overdue_since_date AS overdue_since_date_derived");
        insertSqlStatementBuilder.append(" FROM m_loan ml");
        insertSqlStatementBuilder.append(" INNER JOIN  overdue_amounts oa ON ml.id = oa.loanId");
        insertSqlStatementBuilder.append(" INNER JOIN  m_product_loan mpl ON mpl.id = ml.product_id");
        insertSqlStatementBuilder.append(" LEFT JOIN  m_product_loan_recalculation_details prd ON prd.product_id = ml.product_id");
        insertSqlStatementBuilder.append(" WHERE  ml.loan_status_id = 300");
        if (!isForAllLoans) {
            insertSqlStatementBuilder.append(" AND ml.id = ?");
        }
        insertSqlStatementBuilder.append(" and (prd.arrears_based_on_original_schedule = false");
        insertSqlStatementBuilder.append(" or prd.arrears_based_on_original_schedule is null)");
        insertSqlStatementBuilder.append(" AND (mpl.overdue_amount_for_arrears IS NULL OR mpl.overdue_amount_for_arrears <");
        insertSqlStatementBuilder.append(
                " COALESCE(oa.principal_overdue, 0) + COALESCE(oa.interest_overdue, 0) + COALESCE(oa.fee_charges_overdue, 0) + COALESCE(oa.penalty_charges_overdue, 0))");
        return insertSqlStatementBuilder.toString();
    }

    private List<String> updateLoanArrearsAgeingDetailsWithOriginalSchedule(List<Long> loanIdsForUpdate) {
        List<String> insertStatement = new ArrayList<>();
        String sqlForLoanIdentifiers = buildQueryForLoanIdentifiersWithOriginalSchedule(Boolean.FALSE);
        List<Object> loanIdsForQuery = new ArrayList<>();
        for (Long loanId : loanIdsForUpdate) {
            loanIdsForQuery.add(loanId);
        }
        List<Long> loanIds = this.jdbcTemplate.queryForList(sqlForLoanIdentifiers, loanIdsForQuery.toArray(), new int[] { Types.BIGINT },
                Long.class);
        if (!loanIds.isEmpty()) {
            Map<Long, List<LoanSchedulePeriodData>> scheduleDate = getScheduleDate(loanIds);
            List<Map<String, Object>> loanSummary = getLoanSummary(loanIds);
            loanArrearsAgingService.updateScheduleWithPaidDetail(scheduleDate, loanSummary);
            loanArrearsAgingService.createInsertStatements(insertStatement, scheduleDate, true);

        }

        return insertStatement;
    }

    private List<String> updateLoanArrearsAgeingDetailsWithOriginalScheduleForAllLoans() {
        List<String> insertStatement = new ArrayList<>();
        String sqlForLoanIdentifiers = buildQueryForLoanIdentifiersWithOriginalSchedule(Boolean.TRUE);
        List<Long> loanIds = this.jdbcTemplate.queryForList(sqlForLoanIdentifiers, Long.class);
        if (!loanIds.isEmpty()) {
            Map<Long, List<LoanSchedulePeriodData>> scheduleDate = getScheduleDate(loanIds);
            List<Map<String, Object>> loanSummary = getLoanSummary(loanIds);
            loanArrearsAgingService.updateScheduleWithPaidDetail(scheduleDate, loanSummary);
            loanArrearsAgingService.createInsertStatements(insertStatement, scheduleDate, true);

        }

        return insertStatement;
    }

    private String buildQueryForLoanIdentifiersWithOriginalSchedule(boolean isForAllLoans) {
        final StringBuilder loanIdentifier = new StringBuilder();
        loanIdentifier.append("select ml.id as loanId FROM m_loan ml  ");
        loanIdentifier.append("INNER JOIN m_loan_repayment_schedule mr on mr.loan_id = ml.id ");
        loanIdentifier.append(
                "inner join m_product_loan_recalculation_details prd on prd.product_id = ml.product_id and prd.arrears_based_on_original_schedule = true  ");
        loanIdentifier.append("WHERE ml.loan_status_id = 300 ");
        if (!isForAllLoans) {
            loanIdentifier.append(" and ml.id IN (?)");
        }
        loanIdentifier.append(" and mr.completed_derived is false  and mr.duedate < ")
                .append(sqlGenerator.subDate(sqlGenerator.currentBusinessDate(), "COALESCE(ml.grace_on_arrears_ageing, 0)", "day"))
                .append(" group by ml.id");
        return loanIdentifier.toString();
    }

    private List<Map<String, Object>> getLoanSummary(final List<Long> loanIds) {
        final StringBuilder transactionsSql = new StringBuilder();
        transactionsSql.append("select ml.id as loanId, ");
        transactionsSql
                .append("ml.principal_repaid_derived as principalAmtPaid, ml.principal_writtenoff_derived as  principalAmtWrittenoff, ");
        transactionsSql.append(" ml.interest_repaid_derived as interestAmtPaid, ml.interest_waived_derived as interestAmtWaived, ");
        transactionsSql.append("ml.fee_charges_repaid_derived as feeAmtPaid, ml.fee_charges_waived_derived as feeAmtWaived, ");
        transactionsSql
                .append("ml.penalty_charges_repaid_derived as penaltyAmtPaid, ml.penalty_charges_waived_derived as penaltyAmtWaived ");
        transactionsSql.append("from m_loan ml ");
        transactionsSql.append("where ml.id IN (:loanIds)").append(" order by ml.id");

        final NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        SqlParameterSource parameters = new MapSqlParameterSource("loanIds", loanIds);
        List<Map<String, Object>> loanSummary = namedJdbcTemplate.queryForList(transactionsSql.toString(), parameters);
        return loanSummary;
    }

    private Map<Long, List<LoanSchedulePeriodData>> getScheduleDate(List<Long> loanIds) {
        LoanOriginalScheduleExtractor loanOriginalScheduleExtractor = new LoanOriginalScheduleExtractor(sqlGenerator);
        final NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        SqlParameterSource parameters = new MapSqlParameterSource("loanIds", loanIds);
        return namedJdbcTemplate.query(loanOriginalScheduleExtractor.schema, parameters, loanOriginalScheduleExtractor);
    }

    private static final class LoanOriginalScheduleExtractor implements ResultSetExtractor<Map<Long, List<LoanSchedulePeriodData>>> {

        private final String schema;

        LoanOriginalScheduleExtractor(DatabaseSpecificSQLGenerator sqlGenerator) {
            final StringBuilder scheduleDetail = new StringBuilder();
            scheduleDetail.append("select ml.id as loanId, mr.duedate as dueDate, mr.principal_amount as principalAmount, ");
            scheduleDetail.append(
                    "mr.interest_amount as interestAmount, mr.fee_charges_amount as feeAmount, mr.penalty_charges_amount as penaltyAmount, mpl.overdue_amount_for_arrears as overDueAmountForArrearsConsideration ");
            scheduleDetail.append("from m_loan ml  INNER JOIN m_loan_repayment_schedule_history mr on mr.loan_id = ml.id ");
            scheduleDetail.append(" inner join m_product_loan mpl on mpl.id = ml.product_id where mr.duedate  < "
                    + sqlGenerator.subDate(sqlGenerator.currentBusinessDate(), "COALESCE(ml.grace_on_arrears_ageing, 0)", "day") + " and ");
            scheduleDetail.append("ml.id IN(:loanIds)").append(" and  mr.version = (");
            scheduleDetail.append("select max(lrs.version) from m_loan_repayment_schedule_history lrs where mr.loan_id = lrs.loan_id");
            scheduleDetail.append(") order by ml.id,mr.duedate");
            this.schema = scheduleDetail.toString();
        }

        @Override
        public Map<Long, List<LoanSchedulePeriodData>> extractData(ResultSet rs) throws SQLException, DataAccessException {
            Map<Long, List<LoanSchedulePeriodData>> scheduleDate = new HashMap<>();

            while (rs.next()) {
                Long loanId = rs.getLong("loanId");
                List<LoanSchedulePeriodData> periodDatas = scheduleDate.computeIfAbsent(loanId, k -> new ArrayList<>());
                periodDatas.add(fetchLoanSchedulePeriodData(rs));
            }

            return scheduleDate;
        }

        private LoanSchedulePeriodData fetchLoanSchedulePeriodData(ResultSet rs) throws SQLException {
            final LocalDate dueDate = JdbcSupport.getLocalDate(rs, "dueDate");
            final BigDecimal principalDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalAmount");
            final BigDecimal interestDueOnPrincipalOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestAmount");
            final BigDecimal totalInstallmentAmount = principalDue.add(interestDueOnPrincipalOutstanding);
            final BigDecimal feeChargesDueForPeriod = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeAmount");
            final BigDecimal penaltyChargesDueForPeriod = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyAmount");
            final Integer periodNumber = null;
            final LocalDate fromDate = null;
            final BigDecimal principalOutstanding = null;
            final BigDecimal totalDueForPeriod = null;
            return LoanSchedulePeriodData.repaymentOnlyPeriod(periodNumber, fromDate, dueDate, principalDue, principalOutstanding,
                    interestDueOnPrincipalOutstanding, feeChargesDueForPeriod, penaltyChargesDueForPeriod, totalDueForPeriod,
                    totalInstallmentAmount);

        }
    }

    private void handleBlockingAfterAreasAging() {

        log.info("Blocking clients with active loans in arrears");
        log.info("Inserting records into m_client_blocking_reason table for clients with active loans in arrears");
        String query = """
                    insert into m_client_blocking_reason (client_id, blocking_reason_id, block_date, block_comment, block_by, created_by)
                    select distinct l.client_id, (select id from m_blocking_reason_setting where name_of_reason = ? and level = ?), current_date,
                    ?, ?, ?
                    from m_loan_arrears_aging mlaa
                    inner join m_loan l on l.id = mlaa.loan_id
                    left join m_client_blocking_reason mcbr
                    on mcbr.client_id = l.client_id
                    left join m_blocking_reason_setting mbrs
                    on mbrs.id = mcbr.blocking_reason_id and mbrs.name_of_reason = ?
                    where mcbr.id is null
                """;
        this.jdbcTemplate.update(query, BLOCKING_REASON_NAME, BlockLevel.CLIENT.toString(), BLOCKING_COMMENT,
                this.context.authenticatedUser().getId(), this.context.authenticatedUser().getId(), BLOCKING_REASON_NAME);

        log.info("Updating m_client table to set the block_status_id to the blocking reason id for clients with active loans in arrears");
        query = """
                    update m_client set blocking_reason_id = (select id from m_blocking_reason_setting where name_of_reason = ? and level = ?)
                    where id in (
                        select distinct l.client_id
                        from m_loan_arrears_aging mlaa
                        inner join m_loan l on l.id = mlaa.loan_id
                        inner join m_client on m_client.id = l.client_id
                        left join m_client_blocking_reason mcbr
                        on mcbr.client_id = l.client_id
                        left join m_blocking_reason_setting mbrs
                        on mbrs.id = mcbr.blocking_reason_id and mbrs.name_of_reason = ?
                    )
                """;
        this.jdbcTemplate.update(query, BLOCKING_REASON_NAME, BlockLevel.CLIENT.toString(), BLOCKING_REASON_NAME);
        log.info("Done blocking clients with active loans in arrears");
    }

    private void handleUnBlockingAfterArrearsAging() {

        log.info("Unblocking clients with no active loans in arrears");
        log.info(
                "Updating m_client_blocking_reason table to set the unblock_date to current date for clients with no active loans in arrears");
        String query = """
                update m_client_blocking_reason set unblock_date = current_date, unblock_comment = ?, unblock_by = ?
                where client_id in (
                    SELECT DISTINCT mcbr.client_id
                    FROM m_client_blocking_reason mcbr
                    JOIN m_blocking_reason_setting mbrs ON mbrs.id = mcbr.blocking_reason_id
                    AND mbrs.name_of_reason = ?
                    WHERE mcbr.client_id NOT IN (
                        SELECT DISTINCT client_id
                        FROM m_loan_arrears_aging
                    )
                )
                """;
        this.jdbcTemplate.update(query, UNBLOCKING_COMMENT, this.context.authenticatedUser().getId(), BLOCKING_REASON_NAME);
        log.info("Updating m_client table to set the block_status_id to null for clients with no active loans in arrears");
        query = """
                update m_client set blocking_reason_id = null
                where id in (
                    SELECT DISTINCT mcbr.client_id
                    FROM m_client_blocking_reason mcbr
                    JOIN m_blocking_reason_setting mbrs ON mbrs.id = mcbr.blocking_reason_id
                    AND mbrs.name_of_reason = ?
                    WHERE mcbr.client_id NOT IN (
                        SELECT DISTINCT client_id
                        FROM m_loan_arrears_aging
                    )
                and blocking_reason_id = (select id from m_blocking_reason_setting where name_of_reason = ? and level = ?)
                )
                """;
        this.jdbcTemplate.update(query, BLOCKING_REASON_NAME, BLOCKING_REASON_NAME, BlockLevel.CLIENT.toString());
        log.info("Done unblocking clients with no active loans in arrears");

        // Remove clients from m_client_block_list table
        query = """
                delete from m_client_block_list where client_id in (
                        SELECT DISTINCT client_id
                        FROM m_loan_arrears_aging
                ) and (SELECT count(id) FROM m_blocking_reason_setting where name_of_reason = ? and level = ?) > 0
                """;
        this.jdbcTemplate.update(query, BLOCKING_REASON_LISTAS_DE_CONTROL, BlockLevel.CLIENT.toString());
    }

    private void handleBlockingReasonCredit() {
        BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepositoryWrapper
                .getSingleBlockingReasonSettingByReason(BLOCKING_REASON_NAME, BlockLevel.CREDIT.toString());

        log.info("Blocking active loans in arrears");
        log.info("Inserting records into m_credit_blocking_reason table for loans with active loans in arrears");
        String query = """
                    insert into m_credit_blocking_reason (loan_id, blocking_reason_id, "comment", is_active, block_date, createdby_id)
                    select distinct mlaa.loan_id, ?, ?,
                    true, current_date, ?
                    from m_loan_arrears_aging mlaa
                    INNER JOIN m_loan l on l.id = mlaa.loan_id
                    LEFT JOIN m_credit_blocking_reason  mcbr on mcbr.loan_id  = l.id
                    LEFT join m_blocking_reason_setting mbrs
                    on mbrs.id = mcbr.blocking_reason_id and mbrs.name_of_reason = ?
                    where mcbr.id is null
                """;
        this.jdbcTemplate.update(query, blockingReasonSetting.getId(), BLOCKING_COMMENT, this.context.authenticatedUser().getId(),
                BLOCKING_REASON_NAME);

        log.info("Updating m_loan table to set the block_status_id to the blocking reason id for loans with active loans in arrears");
        query = """
                    update m_loan set block_status_id = ?
                    where id in (
                        select distinct mlaa.loan_id
                        from m_loan_arrears_aging mlaa
                        INNER JOIN m_loan l on l.id = mlaa.loan_id
                        LEFT JOIN m_credit_blocking_reason  mcbr on mcbr.loan_id  = l.id
                        LEFT join m_blocking_reason_setting mbrs
                        on mbrs.id = mcbr.blocking_reason_id and mbrs.name_of_reason = ?
                        where mcbr.id is null
                    )
                    and (block_status_id is null or block_status_id in (select id from m_blocking_reason_setting where level = ? and priority > ?))
                """;
        this.jdbcTemplate.update(query, blockingReasonSetting.getId(), BLOCKING_REASON_NAME, BlockLevel.CREDIT.toString(),
                blockingReasonSetting.getPriority());
        log.info("Done blocking active loans in arrears");
    }

    private void handleUnblockingReasonCreditAfterArrearsAging() {

        // Delete all the records from m_credit_blocking_reason table where loan_id is not in m_loan_arrears_aging
        String query = """
                    delete from m_credit_blocking_reason where loan_id in
                     (
                     	select ml.id from m_loan ml where ml.block_status_id = (select id from m_blocking_reason_setting where name_of_reason = ? and level = ?)
                     	and ml.id not in (select loan_id from m_loan_arrears_aging)
                     )
                """;

        jdbcTemplate.update(query, BLOCKING_REASON_NAME, BlockLevel.CREDIT.toString());

        // Update m_loan table block_status_id to null where id is not in m_loan_arrears_aging
        query = """
                update m_loan set block_status_id = null where block_status_id = (select id from m_blocking_reason_setting where name_of_reason = ? and level = ?)
                      and id not in (select loan_id from m_loan_arrears_aging)
                """;
        jdbcTemplate.update(query, BLOCKING_REASON_NAME, BlockLevel.CREDIT.toString());
    }
}
