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
package org.apache.fineract.organisation.bankcheque.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.data.PaginationParameters;
import org.apache.fineract.infrastructure.core.data.PaginationParametersDataValidator;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.infrastructure.security.utils.SQLBuilder;
import org.apache.fineract.organisation.bankcheque.data.BatchData;
import org.apache.fineract.organisation.bankcheque.data.ChequeData;
import org.apache.fineract.organisation.bankcheque.domain.BankChequeStatus;
import org.apache.fineract.organisation.bankcheque.exception.BatchNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChequeReadPlatformServiceImpl implements ChequeReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final BatchMapper batchMapper = new BatchMapper();
    private final ChequeMapper chequeMapper = new ChequeMapper();
    private final PaginationParametersDataValidator paginationParametersDataValidator;
    private final ColumnValidator columnValidator;
    private final PaginationHelper paginationHelper;
    private final DatabaseSpecificSQLGenerator sqlGenerator;

    @Override
    public BatchData retrieveBatch(final Long batchId) {
        final String batchSql = batchMapper.schema() + " WHERE mpb.id = ?";
        final List<BatchData> batchDataList = this.jdbcTemplate.query(batchSql, this.batchMapper, batchId);
        if (!batchDataList.isEmpty()) {
            return batchDataList.get(0);
        } else {
            throw new BatchNotFoundException(batchId);
        }

    }

    @Override
    public BatchData retrieveTemplate(Long bankAccId) {
        String maxChequeNoSql = """
                SELECT IFNULL(MAX(mbc.check_no), 0) AS maxChequeNo
                FROM m_bank_check mbc
                INNER JOIN m_payment_batch mpb ON mpb.id = mbc.batch_id
                LEFT JOIN m_bank_account mba ON mba.id = mpb.bank_acc_id
                WHERE mba.id = ?
                """;
        final Long maxChequeNo = this.jdbcTemplate.queryForObject(maxChequeNoSql, Long.class, new Object[] { bankAccId });
        Long formValue = ObjectUtils.defaultIfNull(maxChequeNo, 0L) + 1;

        List<EnumOptionData> chequeStatusOptions = List.of(BankChequeStatus.status(1), BankChequeStatus.status(2),
                BankChequeStatus.status(3), BankChequeStatus.status(4), BankChequeStatus.status(5));
        return BatchData.builder().from(formValue).statusOptions(chequeStatusOptions).build();
    }

    private static final class ChequeMapper implements RowMapper<ChequeData> {

        private final String schema;

        ChequeMapper() {
            this.schema = """
                    	mbc.id AS chequeId,
                    	mpb.id as batchId,
                    	mbc.check_no AS chequeNo,
                    	mbc.status_enum AS statusEnum,
                    	mbc.description AS description,
                    	mpb.batch_no AS batchNo,
                    	mba.account_number AS bankAccNo,
                        mba.id AS bankAccId,
                    	mb.name AS bankName,
                    	mag.name AS agencyName,
                    	mbc.voided_date AS voidedDate,
                    	mbc.created_date AS createdDate,
                    	mbc.usedon_date AS usedOnDate,
                    	mbc.printed_date AS printedDate,
                    	mbc.void_authorized_date AS voidAuthorizedDate,
                    	voidedby.username AS voidedByUsername,
                    	createdby.username AS createdByUsername,
                    	printedby.username AS printedByUsername,
                    	voidauthorizedby.username AS voidAuthorizedByUsername,
                    	lastmodifiedby.username AS lastModifiedByUsername
                    FROM m_bank_check mbc
                    LEFT JOIN m_payment_batch mpb ON mpb.id = mbc.batch_id
                    LEFT JOIN m_bank_account mba ON mba.id = mpb.bank_acc_id
                    LEFT JOIN m_bank mb ON mb.id = mba.bank_id
                    LEFT JOIN m_agency mag ON mag.id = mba.agency_id
                    LEFT JOIN m_appuser voidedby ON voidedby.id = mbc.voidedby_id
                    LEFT JOIN m_appuser createdby ON createdby.id = mbc.createdby_id
                    LEFT JOIN m_appuser printedby ON printedby.id = mbc.printedby_id
                    LEFT JOIN m_appuser voidauthorizedby ON voidauthorizedby.id = mbc.void_authorizedby_id
                    LEFT JOIN m_appuser lastmodifiedby ON lastmodifiedby.id = mbc.lastmodifiedby_id
                    """;
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public ChequeData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
            EnumOptionData status = BankChequeStatus.status(statusEnum);
            final Long id = JdbcSupport.getLong(rs, "chequeId");
            final Long batchId = JdbcSupport.getLong(rs, "batchId");
            final Long chequeNo = JdbcSupport.getLong(rs, "chequeNo");
            final Long batchNo = JdbcSupport.getLong(rs, "batchNo");
            final String description = rs.getString("description");
            final String bankAccNo = rs.getString("bankAccNo");
            final Long bankAccId = JdbcSupport.getLong(rs, "bankAccId");
            final String agencyName = rs.getString("agencyName");
            final String bankName = rs.getString("bankName");
            final LocalDate voidedDate = JdbcSupport.getLocalDate(rs, "voidedDate");
            final LocalDate createdDate = JdbcSupport.getLocalDate(rs, "createdDate");
            final LocalDate usedOnDate = JdbcSupport.getLocalDate(rs, "usedOnDate");
            final LocalDate printedDate = JdbcSupport.getLocalDate(rs, "printedDate");
            final LocalDate voidAuthorizedDate = JdbcSupport.getLocalDate(rs, "voidAuthorizedDate");
            final String voidedByUsername = rs.getString("voidedByUsername");
            final String createdByUsername = rs.getString("createdByUsername");
            final String printedByUsername = rs.getString("printedByUsername");
            final String voidAuthorizedByUsername = rs.getString("voidAuthorizedByUsername");
            final String lastModifiedByUsername = rs.getString("lastModifiedByUsername");
            return ChequeData.builder().id(id).status(status).batchId(batchId).batchNo(batchNo).description(description)
                    .agencyName(agencyName).bankAccNo(bankAccNo).bankName(bankName).bankAccId(bankAccId).voidedDate(voidedDate)
                    .chequeNo(chequeNo).createdDate(createdDate).usedOnDate(usedOnDate).printedDate(printedDate)
                    .voidAuthorizedDate(voidAuthorizedDate).voidedByUsername(voidedByUsername).createdByUsername(createdByUsername)
                    .printedByUsername(printedByUsername).voidAuthorizedByUsername(voidAuthorizedByUsername)
                    .lastModifiedByUsername(lastModifiedByUsername).build();

        }
    }

    public static final class BatchMapper implements RowMapper<BatchData> {

        private final String schema;

        public BatchMapper() {
            this.schema = """
                    SELECT
                    	mba.account_number AS bankAccNo,
                    	mba.id AS bankAccId,
                    	mb.name AS bankName,
                    	mb.code AS bankCode,
                    	mag.name AS agencyName,
                    	mag.id AS agencyId,
                    	mpb.id AS batchId,
                    	mpb.description as description,
                    	mpb.batch_no AS batchNo,
                    	mpb.`from` AS 'from',
                    	mpb.`to` AS 'to',
                    	mpb.created_date AS createdDate,
                    	ma.id AS createdByUserId,
                    	ma.username AS createdByUsername
                    FROM m_bank_account mba
                    INNER JOIN m_agency mag ON mag.id = mba.agency_id
                    INNER JOIN m_bank mb ON mb.id = mba.bank_id
                    INNER JOIN m_payment_batch mpb ON mpb.bank_acc_id = mba.id
                    LEFT JOIN m_appuser ma ON ma.id = mpb.createdby_id
                    """;
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public BatchData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final Long batchId = JdbcSupport.getLong(rs, "batchId");
            final Long bankAccNo = JdbcSupport.getLong(rs, "bankAccNo");
            final Long bankAccId = JdbcSupport.getLong(rs, "bankAccId");
            final String bankName = rs.getString("bankName");
            final String bankCode = rs.getString("bankCode");
            final String agencyName = rs.getString("agencyName");
            final Long agencyId = JdbcSupport.getLong(rs, "agencyId");
            final Long batchNo = JdbcSupport.getLong(rs, "batchNo");
            final Long from = JdbcSupport.getLong(rs, "from");
            final Long to = JdbcSupport.getLong(rs, "to");
            final Long createdByUserId = JdbcSupport.getLong(rs, "createdByUserId");
            final LocalDate createdDate = JdbcSupport.getLocalDate(rs, "createdDate");
            final String createdByUsername = rs.getString("createdByUsername");
            final String description = rs.getString("description");
            return BatchData.builder().id(batchId).batchNo(batchNo).bankAccNo(bankAccNo).bankAccId(bankAccId).bankName(bankName)
                    .bankCode(bankCode).agencyId(agencyId).agencyName(agencyName).description(description).from(from).to(to)
                    .createdDate(createdDate).createdByUserId(createdByUserId).createdByUsername(createdByUsername).build();

        }
    }

    @Override
    public Page<ChequeData> retrieveAll(SearchParameters searchParameters, PaginationParameters parameters) {
        final Set<String> supportedOrderByValues = new HashSet<>(List.of("chequeNo"));
        this.paginationParametersDataValidator.validateParameterValues(parameters, supportedOrderByValues, "cheques");
        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("SELECT ").append(this.sqlGenerator.calcFoundRows()).append(" ");
        sqlBuilder.append(this.chequeMapper.schema());
        final SQLBuilder extraCriteria = new SQLBuilder();
        final Long batchId = searchParameters.getBatchId();
        final Long chequeId = searchParameters.getChequeId();
        final Long agencyId = searchParameters.getAgencyId();
        final String chequeNo = searchParameters.getChequeNo();
        final String status = searchParameters.getStatus();
        if (batchId != null) {
            extraCriteria.addNonNullCriteria("mpb.id = ", batchId);
        }
        if (agencyId != null) {
            extraCriteria.addNonNullCriteria("mba.agency_id = ", agencyId);
        }
        if (chequeId != null) {
            extraCriteria.addNonNullCriteria("mbc.id = ", chequeId);
        }
        if (chequeNo != null) {
            extraCriteria.addNonNullCriteria("mbc.check_no LIKE", "%" + chequeNo + "%");
        }
        if (status != null) {
            extraCriteria.addNonNullCriteria("mbc.status_enum LIKE", "%" + status + "%");
        }
        sqlBuilder.append(" ").append(extraCriteria.getSQLTemplate());
        if (parameters.isOrderByRequested()) {
            sqlBuilder.append(" order by ").append(searchParameters.getOrderBy()).append(' ').append(searchParameters.getSortOrder());
            this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getOrderBy(),
                    searchParameters.getSortOrder());
        }

        if (parameters.isLimited()) {
            sqlBuilder.append(" limit ").append(searchParameters.getLimit());
            if (searchParameters.isOffset()) {
                sqlBuilder.append(" offset ").append(searchParameters.getOffset());
            }
        }
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), extraCriteria.getArguments(), this.chequeMapper);

    }

}
