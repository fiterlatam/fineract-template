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


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.configuration.data.ExternalServicesPropertiesData;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesConstants;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.data.PaginationParameters;
import org.apache.fineract.infrastructure.core.data.PaginationParametersDataValidator;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.infrastructure.security.utils.SQLBuilder;
import org.apache.fineract.organisation.agency.data.AgencyData;
import org.apache.fineract.organisation.agency.service.AgencyReadPlatformServiceImpl;
import org.apache.fineract.organisation.bankcheque.data.BatchData;
import org.apache.fineract.organisation.bankcheque.data.ChequeData;
import org.apache.fineract.organisation.bankcheque.data.ChequeSearchParams;
import org.apache.fineract.organisation.bankcheque.data.GuaranteeData;
import org.apache.fineract.organisation.bankcheque.domain.BankChequeStatus;
import org.apache.fineract.organisation.bankcheque.exception.BatchNotFoundException;
import org.apache.fineract.organisation.office.domain.OfficeHierarchyLevel;
import org.apache.fineract.portfolio.group.data.CenterData;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.group.service.CenterReadPlatformServiceImpl;
import org.apache.fineract.portfolio.group.service.GroupReadPlatformService;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
    private final AgencyReadPlatformServiceImpl agencyReadPlatformService;
    private final PlatformSecurityContext context;
    private final CenterReadPlatformServiceImpl centerReadPlatformService;
    private final AppUserReadPlatformService appUserReadPlatformService;
    private final GroupReadPlatformService groupReadPlatformService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final FromJsonHelper fromApiJsonHelper;
    private final ExternalServicesPropertiesReadPlatformService externalServicePropertiesReadPlatformService;

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

        List<EnumOptionData> chequeStatusOptions = BankChequeStatus.listAllChequeStatusOptions();
        final Collection<AgencyData> agencyOptions = this.agencyReadPlatformService.retrieveAllByUser();
        final Collection<CenterData> centerOptions = this.centerReadPlatformService
                .retrieveAllForDropdown(this.context.authenticatedUser().getOffice().getId());
        final List<AppUserData> facilitatorOptions = new ArrayList<>(
                this.appUserReadPlatformService.retrieveUsersUnderHierarchy(Long.valueOf(OfficeHierarchyLevel.GRUPO.getValue())));
        final Collection<GroupGeneralData> groupOptions = this.groupReadPlatformService.retrieveAll(null, null);
        return BatchData.builder().from(formValue).statusOptions(chequeStatusOptions).agencyOptions(agencyOptions)
                .groupOptions(groupOptions).centerOptions(centerOptions).facilitatorOptions(facilitatorOptions).build();
    }

    static final class ChequeMapper implements RowMapper<ChequeData> {

        private final String schema;

        ChequeMapper() {
            this.schema = """
                    	mbc.id AS chequeId,
                    	mpb.id as batchId,
                    	mbc.check_no AS chequeNo,
                    	mbc.is_reassigned AS reassinged,
                    	mbc.status_enum AS statusEnum,
                    	mbc.description AS description,
                    	mbc.guarantee_amount As guaranteeAmount,
                    	ml.approved_principal as loanAmount,
                    	(case when ml.approved_principal>0 then ml.approved_principal else mbc.guarantee_amount end) as chequeAmount,
                    	mbc.case_id AS caseId,
                    	mbc.guarantee_id AS guaranteeId,
                    	mc.account_no AS clientNo,
                    	mc.display_name AS clientName,
                    	mbc.guarantee_name AS guaranteeName,
                    	mg.display_name AS groupName,
                    	mg.account_no AS groupNo,
                    	ml.account_no AS loanAccNo,
                    	ml.id AS loanAccId,
                    	mpb.batch_no AS batchNo,
                    	mba.account_number AS bankAccNo,
                        mba.id AS bankAccId,
                    	mb.name AS bankName,
                    	mag.name AS agencyName,
                    	mag.id AS agencyId,
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
                    LEFT JOIN m_loan ml ON ml.cheque_id = mbc.id
                    LEFT JOIN m_group mg ON mg.id = ml.group_id
                    LEFT JOIN m_group mpg ON mpg.id = mg.parent_id
                    LEFT JOIN m_payment_batch mpb ON mpb.id = mbc.batch_id
                    LEFT JOIN m_bank_account mba ON mba.id = mpb.bank_acc_id
                    LEFT JOIN m_bank mb ON mb.id = mba.bank_id
                    LEFT JOIN m_agency mag ON mag.id = mba.agency_id
                    LEFT JOIN m_appuser voidedby ON voidedby.id = mbc.voidedby_id
                    LEFT JOIN m_appuser createdby ON createdby.id = mbc.createdby_id
                    LEFT JOIN m_appuser printedby ON printedby.id = mbc.printedby_id
                    LEFT JOIN m_appuser voidauthorizedby ON voidauthorizedby.id = mbc.void_authorizedby_id
                    LEFT JOIN m_appuser lastmodifiedby ON lastmodifiedby.id = mbc.lastmodifiedby_id
                    LEFT JOIN m_client mc ON mc.id = ml.client_id
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
            final Long guaranteeId = JdbcSupport.getLong(rs, "guaranteeId");
            final Long chequeNo = JdbcSupport.getLong(rs, "chequeNo");
            final Long batchNo = JdbcSupport.getLong(rs, "batchNo");
            final String description = rs.getString("description");
            final String bankAccNo = rs.getString("bankAccNo");
            final Long bankAccId = JdbcSupport.getLong(rs, "bankAccId");
            final Long agencyId = JdbcSupport.getLong(rs, "agencyId");
            final String agencyName = rs.getString("agencyName");
            final String caseId = rs.getString("caseId");
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
            final Boolean reassinged = rs.getBoolean("reassinged");
            String clientName = rs.getString("clientName");
            final String guaranteeName = rs.getString("guaranteeName");
            if (StringUtils.isBlank(clientName)) {
                clientName = guaranteeName;
            }
            final String clientNo = rs.getString("clientNo");
            final String groupName = rs.getString("groupName");
            final String loanAccNo = rs.getString("loanAccNo");
            final Long loanAccId = JdbcSupport.getLong(rs, "loanAccId");
            final String groupNo = rs.getString("groupNo");
            final BigDecimal loanAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "loanAmount");
            final BigDecimal guaranteeAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "guaranteeAmount");
            final BigDecimal chequeAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "chequeAmount");
            return ChequeData.builder().id(id).status(status).batchId(batchId).batchNo(batchNo).description(description)
                    .agencyName(agencyName).bankAccNo(bankAccNo).bankName(bankName).bankAccId(bankAccId).voidedDate(voidedDate)
                    .chequeNo(chequeNo).createdDate(createdDate).usedOnDate(usedOnDate).printedDate(printedDate)
                    .voidAuthorizedDate(voidAuthorizedDate).voidedByUsername(voidedByUsername).createdByUsername(createdByUsername)
                    .printedByUsername(printedByUsername).voidAuthorizedByUsername(voidAuthorizedByUsername)
                    .lastModifiedByUsername(lastModifiedByUsername).clientName(clientName).clientNo(clientNo).groupName(groupName)
                    .loanAccNo(loanAccNo).loanAmount(loanAmount).guaranteeAmount(guaranteeAmount).groupNo(groupNo).guaranteeId(guaranteeId)
                    .caseId(caseId).chequeAmount(chequeAmount).agencyId(agencyId).loanAccId(loanAccId).reassingedCheque(Boolean.valueOf(reassinged)).build();

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
    public Page<ChequeData> retrieveAll(final ChequeSearchParams chequeSearchParams, PaginationParameters parameters) {
        final Set<String> supportedOrderByValues = new HashSet<>(List.of("chequeNo"));
        this.paginationParametersDataValidator.validateParameterValues(parameters, supportedOrderByValues, "cheques");
        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("SELECT ").append(this.sqlGenerator.calcFoundRows()).append(" ");
        sqlBuilder.append(this.chequeMapper.schema());
        final SQLBuilder extraCriteria = new SQLBuilder();
        final Long batchId = chequeSearchParams.getBatchId();
        final Long chequeId = chequeSearchParams.getChequeId();
        final Long agencyId = chequeSearchParams.getAgencyId();
        final String chequeNo = chequeSearchParams.getChequeNo();
        final String status = chequeSearchParams.getStatus();
        final String bankAccNo = chequeSearchParams.getBankAccNo();
        final Long bankAccId = chequeSearchParams.getBankAccId();
        final Long from = chequeSearchParams.getFrom();
        final Long to = chequeSearchParams.getTo();
        final Long groupId = chequeSearchParams.getGroupId();
        final Long centerId = chequeSearchParams.getCenterId();
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
        if (bankAccNo != null) {
            extraCriteria.addNonNullCriteria("mba.account_number LIKE", "%" + bankAccNo + "%");
        }
        if (status != null) {
            extraCriteria.addNonNullCriteria("mbc.status_enum = ", status);
        }
        if (bankAccId != null) {
            extraCriteria.addNonNullCriteria("mba.id = ", bankAccId);
        }
        if (from != null) {
            extraCriteria.addNonNullCriteria("mbc.check_no >= ", from);
        }
        if (to != null) {
            extraCriteria.addNonNullCriteria("mbc.check_no <= ", to);
        }
        if (groupId != null) {
            extraCriteria.addNonNullCriteria("mg.id = ", groupId);
        }
        if (centerId != null) {
            extraCriteria.addNonNullCriteria("mpg.id = ", centerId);
        }

        sqlBuilder.append(" ").append(extraCriteria.getSQLTemplate());
        final Collection<AgencyData> agencyOptions = this.agencyReadPlatformService.retrieveAllByUser();
        final Set<Long> agencyIds = agencyOptions.stream().map(AgencyData::getId).collect(Collectors.toSet());
        final String agencyIdParams = StringUtils.join(agencyIds, ", ");
        sqlBuilder.append(" AND mba.agency_id IN ( ").append(agencyIdParams).append(")");
        if (chequeSearchParams.getOrderBy() != null) {
            sqlBuilder.append(" order by ").append(chequeSearchParams.getOrderBy()).append(' ').append(chequeSearchParams.getSortOrder());
            this.columnValidator.validateSqlInjection(sqlBuilder.toString(), chequeSearchParams.getOrderBy(),
                    chequeSearchParams.getSortOrder());
        }

        if (chequeSearchParams.getLimit() != null) {
            sqlBuilder.append(" limit ").append(chequeSearchParams.getLimit());
            if (chequeSearchParams.getOffset() != null) {
                sqlBuilder.append(" offset ").append(chequeSearchParams.getOffset());
            }
        }
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), extraCriteria.getArguments(), this.chequeMapper);

    }

    @Override
    public List<GuaranteeData> retrieveGuarantees(String caseId, final String locale) {
        final Locale reqLocale = new Locale(locale);
        GuaranteeData guaranteeData = GuaranteeData.builder().caseId(caseId).build();
        final Collection<ExternalServicesPropertiesData> externalServicesPropertiesDatas = this.externalServicePropertiesReadPlatformService
                .retrieveOne(ExternalServicesConstants.GUARANTEE_SERVICE_NAME);

        String guaranteeApiUsername = null;
        String guaranteeApiPassword = null;
        String guaranteeApiHost = null;

        for (final ExternalServicesPropertiesData externalServicesPropertiesData : externalServicesPropertiesDatas) {
            if ("guaranteeApiUsername".equalsIgnoreCase(externalServicesPropertiesData.getName())) {
                guaranteeApiUsername = externalServicesPropertiesData.getValue();
            } else if ("guaranteeApiPassword".equalsIgnoreCase(externalServicesPropertiesData.getName())) {
                guaranteeApiPassword = externalServicesPropertiesData.getValue();
            } else if ("guaranteeApiHost".equalsIgnoreCase(externalServicesPropertiesData.getName())) {
                guaranteeApiHost = externalServicesPropertiesData.getValue();
            }
        }
        final String credentials = guaranteeApiUsername + ":" + guaranteeApiPassword;
        final String basicAuth = new String(Base64.encodeBase64(credentials.getBytes(Charset.defaultCharset())), Charset.defaultCharset());
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(List.of(MediaType.ALL));
        httpHeaders.add("Authorization", "Basic " + basicAuth);
        final String url = guaranteeApiHost + "?caseid=" + caseId;
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(guaranteeData, httpHeaders),
                String.class);
        final List<GuaranteeData> guaranteeDataList = new ArrayList<>();
        if (responseEntity.hasBody()) {
            final JsonElement jsonElement = this.fromApiJsonHelper.parse(responseEntity.getBody());
            if (jsonElement.isJsonArray()) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                for (int i = 0; i < jsonArray.size(); i++) {
                    final JsonElement element = jsonArray.get(i);
                    final Long id = this.fromApiJsonHelper.extractLongNamed("id", element);
                    final String status = ObjectUtils.defaultIfNull(this.fromApiJsonHelper.extractStringNamed("estado", element),
                            "Nueva Solicitud");
                    final JsonElement data = this.fromApiJsonHelper.extractJsonObjectNamed("datos", element);
                    final String clientNo = this.fromApiJsonHelper.extractStringNamed("numero_cliente", data);
                    final String clientName = this.fromApiJsonHelper.extractStringNamed("name", data);
                    final String withdrawalReason = this.fromApiJsonHelper.extractStringNamed("razon_retiro", data);
                    final BigDecimal requestedAmount = this.fromApiJsonHelper.extractBigDecimalNamed("monto", data, reqLocale);
                    final GuaranteeData guarantee = GuaranteeData.builder().id(id).caseId(caseId).clientNo(clientNo).clientName(clientName)
                            .withdrawalReason(withdrawalReason).requestedAmount(requestedAmount).status(status).build();
                    guaranteeDataList.add(guarantee);
                }
            }
        }

        final String query = "SELECT " + this.chequeMapper.schema() + " WHERE mbc.case_id = ? ";
        List<ChequeData> chequeDataList = this.jdbcTemplate.query(query, this.chequeMapper, caseId);
        int index = 0;
        for (final GuaranteeData data : List.copyOf(guaranteeDataList)) {
            for (final ChequeData chequeData : chequeDataList) {
                if (!(chequeData.getStatus().getId().equals(BankChequeStatus.VOIDED.getValue().longValue())
                        && chequeData.getGuaranteeId().equals(data.getId()))) {
                    guaranteeDataList.remove(index);
                }
            }
            index++;
        }
        return guaranteeDataList;
    }
}
