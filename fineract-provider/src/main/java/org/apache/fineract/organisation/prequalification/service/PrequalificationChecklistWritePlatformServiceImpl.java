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

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.dataqueries.data.GenericResultsetData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetRowData;
import org.apache.fineract.infrastructure.dataqueries.service.ReadReportingService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.prequalification.data.ClientData;
import org.apache.fineract.organisation.prequalification.data.GroupData;
import org.apache.fineract.organisation.prequalification.data.LoanData;
import org.apache.fineract.organisation.prequalification.data.PolicyData;
import org.apache.fineract.organisation.prequalification.domain.BuroCheckClassification;
import org.apache.fineract.organisation.prequalification.domain.CheckValidationColor;
import org.apache.fineract.organisation.prequalification.domain.LoanAdditionProperties;
import org.apache.fineract.organisation.prequalification.domain.LoanAdditionalPropertiesRepository;
import org.apache.fineract.organisation.prequalification.domain.Policies;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationStatusLogRepository;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroup;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupMember;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatus;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatusLog;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationType;
import org.apache.fineract.organisation.prequalification.domain.ValidationChecklistResult;
import org.apache.fineract.organisation.prequalification.domain.ValidationChecklistResultRepository;
import org.apache.fineract.organisation.prequalification.exception.MemberHasNoPendingLoanException;
import org.apache.fineract.organisation.prequalification.exception.MemberSubmittedLoanNotFoundException;
import org.apache.fineract.organisation.prequalification.exception.PrequalificationNotMappedException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@AllArgsConstructor
public class PrequalificationChecklistWritePlatformServiceImpl implements PrequalificationChecklistWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(PrequalificationChecklistWritePlatformServiceImpl.class);
    private final PlatformSecurityContext context;
    private final PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper;
    private final ValidationChecklistResultRepository validationChecklistResultRepository;
    private final PreQualificationStatusLogRepository preQualificationStatusLogRepository;
    private final PlatformSecurityContext platformSecurityContext;
    private final JdbcTemplate jdbcTemplate;
    final PolicyMapper policyMapper = new PolicyMapper();
    final ClientDataMapper clientDataMapper = new ClientDataMapper();
    private final ReadReportingService readReportingService;
    private final PrequalificationWritePlatformServiceImpl.GroupTypeLoanMapper groupTypeLoanMapper = new PrequalificationWritePlatformServiceImpl.GroupTypeLoanMapper();
    private final PrequalificationWritePlatformServiceImpl.IndividualTypeLoanMapper individualTypeLoanMapper = new PrequalificationWritePlatformServiceImpl.IndividualTypeLoanMapper();
    private final LoanAdditionalPropertiesRepository loanAdditionalPropertiesRepository;

    @Override
    @Transactional
    public CommandProcessingResult validatePrequalificationHardPolicies(Long prequalificationId, JsonCommand command) {
        AppUser appUser = this.context.authenticatedUser();
        PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(prequalificationId);

        validateMemberLoanRequest(prequalificationGroup);

        String blistSql = "select count(*) from m_group where prequalification_id=?";
        Long attachedGroup = this.jdbcTemplate.queryForObject(blistSql, Long.class, prequalificationId);
        if (attachedGroup <= 0 && prequalificationGroup.getPrequalificationType().equals(PrequalificationType.GROUP.getValue()))
            throw new PrequalificationNotMappedException(prequalificationGroup.getPrequalificationNumber());

        List<ClientData> clientDatas = this.jdbcTemplate.query(clientDataMapper.schema(), clientDataMapper, prequalificationId);
        final Long productId = prequalificationGroup.getLoanProduct().getId();
        final Integer noOfMembers = prequalificationGroup.getMembers().size();
        final BigDecimal totalAmountRequested = prequalificationGroup.getMembers().stream()
                .map(PrequalificationGroupMember::getRequestedAmount).reduce(BigDecimal.ONE, BigDecimal::add);
        final GroupData groupData = GroupData.builder().id(prequalificationId).productId(productId).numberOfMembers(noOfMembers)
                .requestedAmount(totalAmountRequested).members(clientDatas).build();
        Integer fromStatus = prequalificationGroup.getStatus();
        List<ValidationChecklistResult> validationChecklistResults = new ArrayList<>();
        final List<PolicyData> groupPolicies = this.jdbcTemplate.query(this.policyMapper.schema(), this.policyMapper, productId,
                PrequalificationType.GROUP.name());
        final List<PolicyData> memberPolicies = this.jdbcTemplate.query(policyMapper.schema(), policyMapper, productId,
                PrequalificationType.INDIVIDUAL.name());
        final String deleteStatement = "DELETE FROM m_checklist_validation_result WHERE prequalification_id = ?";
        this.jdbcTemplate.update(deleteStatement, prequalificationId);
        for (PolicyData policyCategoryData : memberPolicies) {
            for (final ClientData clientData : clientDatas) {
                List<LoanData> submittedLoans;
                if (prequalificationGroup.isPrequalificationTypeGroup()) {
                    submittedLoans = jdbcTemplate.query(this.groupTypeLoanMapper.schema(), this.groupTypeLoanMapper, prequalificationId,
                            clientData.getDpi(), prequalificationId);
                } else {
                    submittedLoans = jdbcTemplate.query(this.individualTypeLoanMapper.schema(), this.individualTypeLoanMapper,
                            prequalificationId, clientData.getDpi(), prequalificationId);
                }
                if (submittedLoans.isEmpty()) {
                    throw new MemberSubmittedLoanNotFoundException(clientData.getDpi());
                }
                final LoanData submittedLoanData = submittedLoans.get(0);
                clientData.setLoanId(submittedLoanData.getLoanId());
                clientData.setProductId(productId);
                ValidationChecklistResult validationChecklistResult = new ValidationChecklistResult();
                validationChecklistResult.setPrequalificationId(prequalificationId);
                validationChecklistResult.setPolicyId(policyCategoryData.getId());
                validationChecklistResult.setClientId(clientData.getClientId());
                validationChecklistResult.setPrequalificationMemberId(clientData.getPrequalificationMemberId());
                validationChecklistResult.setPrequalificationType(PrequalificationType.INDIVIDUAL.getValue());
                CheckValidationColor checkValidationColor = this.validateGenericPolicy(Policies.fromInt(policyCategoryData.getId()),
                        clientData, groupData);
                validationChecklistResult.setValidationColor(checkValidationColor.getValue());
                AppUser authenticatedUser = platformSecurityContext.authenticatedUser();
                final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
                if (authenticatedUser != null && authenticatedUser.getId() != null) {
                    validationChecklistResult.setCreatedBy(authenticatedUser.getId());
                    validationChecklistResult.setLastModifiedBy(authenticatedUser.getId());
                }
                validationChecklistResult.setCreatedDate(localDateTime);
                validationChecklistResult.setLastModifiedDate(localDateTime);
                validationChecklistResults.add(validationChecklistResult);
            }
        }

        for (final PolicyData groupPolicy : groupPolicies) {
            ValidationChecklistResult prequalificationChecklistResult = new ValidationChecklistResult();
            prequalificationChecklistResult.setPrequalificationId(prequalificationId);
            prequalificationChecklistResult.setPolicyId(groupPolicy.getId());
            prequalificationChecklistResult.setPrequalificationType(PrequalificationType.GROUP.getValue());
            CheckValidationColor checkValidationColor = this.validateGenericPolicy(Policies.fromInt(groupPolicy.getId()), null, groupData);
            prequalificationChecklistResult.setValidationColor(checkValidationColor.getValue());
            AppUser authenticatedUser = platformSecurityContext.getAuthenticatedUserIfPresent();
            final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
            if (authenticatedUser != null && authenticatedUser.getId() != null) {
                prequalificationChecklistResult.setCreatedBy(authenticatedUser.getId());
                prequalificationChecklistResult.setLastModifiedBy(authenticatedUser.getId());
            }
            prequalificationChecklistResult.setCreatedDate(localDateTime);
            prequalificationChecklistResult.setLastModifiedDate(localDateTime);
            validationChecklistResults.add(prequalificationChecklistResult);
        }
        prequalificationGroup.updateStatus(PrequalificationStatus.HARD_POLICY_CHECKED);
        prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);
        this.validationChecklistResultRepository.saveAll(validationChecklistResults);

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(appUser, fromStatus, prequalificationGroup.getStatus(),
                null, prequalificationGroup);
        this.preQualificationStatusLogRepository.saveAndFlush(statusLog);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationId).build();
    }

    private void validateMemberLoanRequest(PrequalificationGroup group) {

        List<PrequalificationGroupMember> members = group.getMembers();
        for (PrequalificationGroupMember member : members) {
            String pendingLoanRequest = "select count(*) from m_loan ml inner join m_client mc on mc.id = ml.client_id where mc.dpi=? AND ml.loan_status_id = 100 and ml.product_id = ?";
            Long loanCount = this.jdbcTemplate.queryForObject(pendingLoanRequest, Long.class, member.getDpi(),
                    group.getLoanProduct().getId());
            if (loanCount <= 0)
                throw new MemberHasNoPendingLoanException(member.getName(), member.getDpi(), group.getLoanProduct().getName());

        }
    }

    static final class PolicyMapper implements RowMapper<PolicyData> {

        public String schema() {
            return """
                    SELECT mp.id AS id,\s
                    mp.name AS name,\s
                    mp.label AS label,
                    mp.description AS description,
                    mpl.name AS productName
                    FROM m_policy mp\s
                    INNER JOIN m_product_policy mpp ON mpp.policy_id = mp.id\s
                    INNER JOIN m_product_loan mpl ON mpl.id = mpp.product_id\s
                    WHERE mpl.id = ? AND mpp.evaluation_type = ?
                    GROUP BY mp.id
                    """;
        }

        @Override
        public PolicyData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            final Integer id = JdbcSupport.getInteger(rs, "id");
            final String name = rs.getString("name");
            final String label = rs.getString("label");
            final String description = rs.getString("description");
            return PolicyData.builder().id(id).name(name).label(label).description(description).build();
        }
    }

    private static final class ClientDataMapper implements RowMapper<ClientData> {

        public String schema() {
            return """
                    SELECT mc.id AS clientId, mpgm.id AS prequalificationMemberId, IFNULL(mc.display_name,mpgm.name) AS name, mpg.id AS prequalificationId,\s
                    mpgm.requested_amount AS requestedAmount, IFNULL(mc.date_of_birth, mpgm.dob) AS dateOfBirth, IFNULL(mc.dpi, mpgm.dpi) AS dpi,
                    mpgm.work_with_puente AS workWithPuente, mcv.code_value As gender, mpgm.is_president AS president, mpgm.buro_check_status buroCheckStatus
                    FROM m_prequalification_group_members mpgm\s
                    LEFT JOIN m_client mc ON mc.dpi = mpgm.dpi\s
                    LEFT JOIN m_code_value mcv ON mcv.id = mc.gender_cv_id
                    LEFT JOIN m_prequalification_group mpg ON mpg.id = mpgm.group_id
                    WHERE mpg.id = ?
                    """;
        }

        @Override
        public ClientData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            final Long clientId = JdbcSupport.getLong(rs, "clientId");
            final Integer prequalificationMemberId = JdbcSupport.getInteger(rs, "prequalificationMemberId");
            final Integer prequalificationId = JdbcSupport.getInteger(rs, "prequalificationId");
            final String name = rs.getString("name");
            final boolean president = rs.getBoolean("president");
            final Integer buroCheckStatus = JdbcSupport.getInteger(rs, "buroCheckStatus");
            final Date dateOfBirth = rs.getDate("dateOfBirth");
            final String dpi = rs.getString("dpi");
            final BigDecimal requestedAmount = rs.getBigDecimal("requestedAmount");
            final String workWithPuente = rs.getString("workWithPuente");
            final String gender = rs.getString("gender");
            return ClientData.builder().clientId(clientId).prequalificationId(prequalificationId)
                    .prequalificationMemberId(prequalificationMemberId).name(name).dateOfBirth(dateOfBirth).dpi(dpi)
                    .requestedAmount(requestedAmount).gender(gender).workWithPuente(workWithPuente).president(president)
                    .buroCheckStatus(buroCheckStatus).build();
        }
    }

    private CheckValidationColor validateGenericPolicy(final Policies policy, final ClientData clientData, final GroupData groupData) {
        CheckValidationColor checkValidationColor;
        switch (policy) {
            case ONE -> checkValidationColor = this.runCheck1();
            case TWO -> checkValidationColor = this.runCheck2();
            case THREE -> checkValidationColor = this.runCheck3(clientData);
            case FOUR -> checkValidationColor = this.runCheck4(clientData);
            case FIVE -> checkValidationColor = this.runCheck5(clientData);
            case SIX -> checkValidationColor = this.runCheck6(groupData);
            case SEVEN -> checkValidationColor = this.runCheck7(groupData);
            case EIGHT -> checkValidationColor = this.runCheck8(groupData);
            case NINE -> checkValidationColor = this.runCheck9(groupData);
            case TEN -> checkValidationColor = this.runCheck10(groupData);
            case ELEVEN -> checkValidationColor = this.runCheck11(groupData);
            case TWELVE -> checkValidationColor = this.runCheck12(clientData);
            case THIRTEEN -> checkValidationColor = this.runCheck13(clientData);
            case FOURTEEN -> checkValidationColor = this.runCheck14(clientData);
            case FIFTEEN -> checkValidationColor = this.runCheck15(clientData);
            case SIXTEEN -> checkValidationColor = this.runCheck16(clientData);
            case SEVENTEEN -> checkValidationColor = this.runCheck17(groupData);
            case EIGHTEEN -> checkValidationColor = this.runCheck18(clientData);
            case NINETEEN -> checkValidationColor = this.runCheck19(clientData);
            case TWENTY -> checkValidationColor = this.runCheck20(clientData);
            case TWENTY_ONE -> checkValidationColor = this.runCheck21(clientData);
            case TWENTY_TWO -> checkValidationColor = this.runCheck22(clientData);
            case TWENTY_THREE -> checkValidationColor = this.runCheck23(clientData);
            case TWENTY_FOUR -> checkValidationColor = this.runCheck24(clientData);
            case TWENTY_FIVE -> checkValidationColor = this.runCheck25(clientData);
            case TWENTY_SIX -> checkValidationColor = this.runCheck26(clientData);
            case TWENTY_SEVEN -> checkValidationColor = this.runCheck27(groupData);
            case TWENTY_EIGHT -> checkValidationColor = this.runCheck28(groupData);
            case TWENTY_NINE -> checkValidationColor = this.runCheck29(clientData);
            case THIRTY -> checkValidationColor = this.runCheck30(clientData);
            case THIRTY_ONE -> checkValidationColor = this.runCheck31(clientData);
            case THIRTY_TWO -> checkValidationColor = this.runCheck32(groupData);
            case THIRTY_THREE -> checkValidationColor = this.runCheck33(groupData);
            default -> checkValidationColor = CheckValidationColor.INVALID;
        }
        return checkValidationColor;
    }

    /**
     * New client categorization
     */
    private CheckValidationColor runCheck1() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Recurring customer categorization
     */
    private CheckValidationColor runCheck2() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Increase percentage
     */
    private CheckValidationColor runCheck3(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.THREE.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final Long loanId = clientData.getLoanId();
        final String percentageIncreaseSQL = """
                SELECT
                CASE WHEN (mlag.current_credit_value <= 0) THEN 0
                     ELSE (mlag.requested_value/mlag.current_credit_value) * 100
                END AS percentageIncrease
                FROM m_loan_additionals_group mlag
                INNER JOIN m_loan ml ON ml.id = mlag.loan_id
                WHERE ml.id = ?
                """;
        Object[] params = new Object[] { loanId };
        final BigDecimal percentageIncrease = this.jdbcTemplate.queryForObject(percentageIncreaseSQL, BigDecimal.class, params);
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${percentageIncrease}", String.valueOf(percentageIncrease));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Mandatory to attach photographs and investment plan
     */
    private CheckValidationColor runCheck4(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.FOUR.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final Long loanId = clientData.getLoanId();
        final String firstDocumentCountSql = """
                        SELECT COUNT(*)
                        FROM m_document md
                        LEFT JOIN m_code_value mcvd ON md.document_type = mcvd.id
                        WHERE md.parent_entity_type = 'loans' AND md.parent_entity_id = ? AND (md.name = ? OR mcvd.code_value = ?)
                """;
        Object[] firstDocumentParams = new Object[] { loanId, "Plan de inversión", "Plan de inversión" };
        final Long firstDocumentCount = ObjectUtils
                .defaultIfNull(this.jdbcTemplate.queryForObject(firstDocumentCountSql, Long.class, firstDocumentParams), 0L);
        final String secondDocumentCountSql = """
                        SELECT COUNT(*)
                        FROM m_document md
                        LEFT JOIN m_code_value mcvd ON md.document_type = mcvd.id
                        WHERE md.parent_entity_type = 'loans' AND md.parent_entity_id = ? AND (md.name = ? OR mcvd.code_value = ?)
                """;
        Object[] secondDocumentParams = new Object[] { loanId, "Fotografias", "Fotografias" };
        final Long secondDocumentCount = ObjectUtils
                .defaultIfNull(this.jdbcTemplate.queryForObject(secondDocumentCountSql, Long.class, secondDocumentParams), 0L);
        final String percentageIncreaseSQL = """
                SELECT
                CASE WHEN (mlag.current_credit_value <= 0) THEN 0
                     ELSE (mlag.requested_value/mlag.current_credit_value) * 100
                END AS percentageIncrease
                FROM m_loan_additionals_group mlag
                INNER JOIN m_loan ml ON ml.id = mlag.loan_id
                WHERE ml.id = ?
                """;
        Object[] params = new Object[] { loanId };

        BigDecimal percentageIncrease;
        try {
            percentageIncrease = ObjectUtils
                    .defaultIfNull(this.jdbcTemplate.queryForObject(percentageIncreaseSQL, BigDecimal.class, params), BigDecimal.ZERO);
        } catch (EmptyResultDataAccessException e) {
            percentageIncrease = BigDecimal.ZERO;
        }
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${numberOfFirstDocument}", Long.toString(firstDocumentCount));
        reportParams.put("${numberOfSecondDocument}", Long.toString(secondDocumentCount));
        reportParams.put("${percentageIncrease}", String.valueOf(percentageIncrease));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Client age check
     */
    private CheckValidationColor runCheck5(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.FIVE.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    private ClientData retrieveClientParams(final Long clientId, final Long productId) {
        final String reportName = "Client Categorization Policy Check";
        Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", String.valueOf(clientId));
        reportParams.put("${loanProductId}", String.valueOf(productId));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        final List<ResultsetColumnHeaderData> columnHeaders = result.getColumnHeaders();
        List<ResultsetRowData> rowDataList = result.getData();
        String clientCategorization = "NEW";
        String clientArea = "RURAL";
        String recreditCategorization = "NUEVO";
        if (!rowDataList.isEmpty()) {
            for (int i = 0; i < columnHeaders.size(); i++) {
                final ResultsetColumnHeaderData columnHeaderData = columnHeaders.get(i);
                if ("clientCategorization".equals(columnHeaderData.getColumnName())) {
                    List<String> rowList = rowDataList.get(0).getRow();
                    clientCategorization = rowList.get(i);
                }
                if ("clientArea".equals(columnHeaderData.getColumnName())) {
                    List<String> rowList = rowDataList.get(0).getRow();
                    clientArea = rowList.get(i);
                }
                if ("recreditCategorization".equals(columnHeaderData.getColumnName())) {
                    List<String> rowList = rowDataList.get(0).getRow();
                    recreditCategorization = rowList.get(i);
                }
            }
        }
        return ClientData.builder().clientArea(clientArea).clientCategorization(clientCategorization)
                .recreditCategorization(recreditCategorization).build();
    }

    private CheckValidationColor extractColorFromResultset(final GenericResultsetData resultset) {
        final List<ResultsetColumnHeaderData> columnHeaders = resultset.getColumnHeaders();
        final List<ResultsetRowData> rowDataList = resultset.getData();
        CheckValidationColor colorResult = CheckValidationColor.RED;
        if (!rowDataList.isEmpty()) {
            for (int i = 0; i < columnHeaders.size(); i++) {
                final ResultsetColumnHeaderData columnHeaderData = columnHeaders.get(i);
                if ("color".equals(columnHeaderData.getColumnName())) {
                    final List<String> rowList = rowDataList.get(0).getRow();
                    final String color = rowList.get(i);
                    if (StringUtils.isNotEmpty(color)) {
                        colorResult = CheckValidationColor.valueOf(color);
                    }

                }
            }
        }
        return colorResult;
    }

    /**
     * Number of members according to policy
     */
    private CheckValidationColor runCheck6(final GroupData groupData) {
        String clientArea = "RURAL";
        String clientCategorization = "NEW";
        String recreditCategorization = "NUEVO";
        if (!CollectionUtils.isEmpty(groupData.getMembers())) {
            final ClientData clientData = groupData.getMembers().get(0);
            final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
            clientArea = params.getClientArea();
            recreditCategorization = params.getRecreditCategorization();
            clientCategorization = params.getClientCategorization();
        }
        final String prequalificationId = String.valueOf(groupData.getId());
        final String reportName = Policies.SIX.getName() + " Policy Check";
        final String productId = Long.toString(groupData.getProductId());
        final String numberOfMembers = String.valueOf(groupData.getNumberOfMembers());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${prequalificationId}", prequalificationId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", clientCategorization);
        reportParams.put("${clientArea}", clientArea);
        reportParams.put("${recreditCategorization}", recreditCategorization);
        reportParams.put("${numberOfMembers}", numberOfMembers);
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Minimum and maximum amount
     */
    private CheckValidationColor runCheck7(final GroupData groupData) {
        String clientArea = "RURAL";
        if (!CollectionUtils.isEmpty(groupData.getMembers())) {
            final ClientData clientData = groupData.getMembers().get(0);
            final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
            clientArea = params.getClientArea();
        }
        final String prequalificationId = String.valueOf(groupData.getId());
        final String reportName = Policies.SEVEN.getName() + " Policy Check";
        final String productId = Long.toString(groupData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${prequalificationId}", prequalificationId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientArea}", clientArea);
        reportParams.put("${requestedAmount}", String.valueOf(groupData.getRequestedAmount()));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Value disparity
     */
    private CheckValidationColor runCheck8(final GroupData groupData) {
        String clientArea = "RURAL";
        String disparityRatio = "1";
        BigDecimal minimumAmount = BigDecimal.ZERO;
        BigDecimal maximumAmount = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(groupData.getMembers())) {
            final ClientData clientData = groupData.getMembers().get(0);
            final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
            clientArea = params.getClientArea();
            minimumAmount = groupData.getMembers().get(0).getRequestedAmount();
            for (final ClientData memberData : groupData.getMembers()) {
                final BigDecimal requestedAmount = memberData.getRequestedAmount();
                if (maximumAmount != null && maximumAmount.compareTo(requestedAmount) < 0) {
                    maximumAmount = requestedAmount;
                }
                if (minimumAmount != null && minimumAmount.compareTo(requestedAmount) > 0) {
                    minimumAmount = requestedAmount;
                }
            }
        }
        if (maximumAmount != null) {
            disparityRatio = String.valueOf(maximumAmount.divide(minimumAmount, MoneyHelper.getRoundingMode()));
        }
        final String prequalificationId = String.valueOf(groupData.getId());
        final String reportName = Policies.EIGHT.getName() + " Policy Check";
        final String productId = Long.toString(groupData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${prequalificationId}", prequalificationId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientArea}", clientArea);
        reportParams.put("${disparityRatio}", disparityRatio);
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Percentage of members starting business
     */
    private CheckValidationColor runCheck9(final GroupData groupData) {
        final String reportName = Policies.NINE.getName() + " Policy Check";
        final List<ClientData> members = groupData.getMembers();
        final Integer totalMembers = members.size();
        final String numberOfMemberSQL = """
                SELECT COUNT(DISTINCT mlag.loan_id) totalCount
                FROM m_loan_additionals_group mlag
                LEFT JOIN m_code_value mcv ON mcv.id = mlag.business_experience
                WHERE mlag.loan_id IN ( %s ) AND mcv.code_value = '<6m'
                   """;
        String stmt = String.format(numberOfMemberSQL, members.stream().map(v -> "?").collect(Collectors.joining(", ")));
        List<Object> params = new ArrayList<>();
        members.forEach(m -> params.add(m.getLoanId()));
        final Long numberOfMembers = this.jdbcTemplate.queryForObject(stmt, Long.class, params.toArray());
        final Map<String, String> reportParams = new HashMap<>();
        BigDecimal membersPercentage = BigDecimal.valueOf(100L);
        if (numberOfMembers != null && numberOfMembers < Long.valueOf(totalMembers)) {
            membersPercentage = BigDecimal.valueOf(100L)
                    .multiply(BigDecimal.valueOf(numberOfMembers).divide(BigDecimal.valueOf(totalMembers), MoneyHelper.getRoundingMode()));
        }
        reportParams.put("${membersPercentage}", String.valueOf(membersPercentage));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Percentage of members with their own home
     *
     * @param groupData
     */
    private CheckValidationColor runCheck10(GroupData groupData) {
        final String reportName = Policies.TEN.getName() + " Policy Check";
        final String prequalificationId = String.valueOf(groupData.getId());
        final String productId = String.valueOf(groupData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${prequalificationId}", prequalificationId);
        reportParams.put("${loanProductId}", productId);
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * President of the Board of Directors of the BC
     */
    private CheckValidationColor runCheck11(final GroupData groupData) {
        final List<ClientData> members = groupData.getMembers();
        final Optional<ClientData> presidentOptionalData = members.stream().filter(ClientData::getPresident).findFirst();
        if (presidentOptionalData.isPresent()) {
            final ClientData presidentData = presidentOptionalData.get();
            final ClientData params = retrieveClientParams(presidentData.getClientId(), presidentData.getProductId());
            final String clientArea = params.getClientArea();
            final Integer bureauStatus = presidentData.getBuroCheckStatus();
            final BuroCheckClassification buroCheckClassification = BuroCheckClassification.fromInt(bureauStatus);
            final String prequalificationId = String.valueOf(groupData.getId());
            final String reportName = Policies.ELEVEN.getName() + " Policy Check";
            final String productId = Long.toString(groupData.getProductId());
            final Map<String, String> reportParams = new HashMap<>();
            reportParams.put("${prequalificationId}", prequalificationId);
            reportParams.put("${loanProductId}", productId);
            reportParams.put("${clientArea}", clientArea);
            reportParams.put("${buroCheckClassification}", buroCheckClassification.getLetter());
            final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams,
                    false);
            return extractColorFromResultset(result);
        }
        return CheckValidationColor.RED;
    }

    /**
     * General condition
     */
    private CheckValidationColor runCheck12(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.TWELVE.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Categories of clients to accept
     */
    private CheckValidationColor runCheck13(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.THIRTEEN.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Integer bureauStatus = clientData.getBuroCheckStatus();
        final BuroCheckClassification buroCheckClassification = BuroCheckClassification.fromInt(bureauStatus);
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        reportParams.put("${buroCheckClassification}", buroCheckClassification.getLetter());
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Amount requested in relation to the current amount of main products
     */
    private CheckValidationColor runCheck14(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.FOURTEEN.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final String requestedAmount = clientData.getRequestedAmount().toPlainString();
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${requestedAmount}", requestedAmount);
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Add endorsement
     */
    private CheckValidationColor runCheck15(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.FIFTEEN.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final Long loanId = clientData.getLoanId();
        final String documentCountSql = """
                        SELECT COUNT(*)
                        FROM m_document md
                        LEFT JOIN m_code_value mcvd ON md.document_type = mcvd.id
                        WHERE md.parent_entity_type = 'loans' AND md.parent_entity_id = ? AND (md.name = ? OR mcvd.code_value = ?)
                """;
        Object[] params = new Object[] { loanId, "Agregar aval", "Agregar aval" };
        final Long documentCount = this.jdbcTemplate.queryForObject(documentCountSql, Long.class, params);
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${numberOfDocuments}", String.valueOf(documentCount));
        reportParams.put("${requestedAmount}", String.valueOf(clientData.getRequestedAmount()));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Payments outside the current term of the main product
     *
     * @param clientData
     */
    private CheckValidationColor runCheck16(ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.SIXTEEN.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Percentage of members of the same group who they can have parallel product
     */
    private CheckValidationColor runCheck17(final GroupData groupData) {
        String clientArea = "RURAL";
        if (!CollectionUtils.isEmpty(groupData.getMembers())) {
            final ClientData clientData = groupData.getMembers().get(0);
            final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
            clientArea = params.getClientArea();
        }
        final String prequalificationId = String.valueOf(groupData.getId());
        final String reportName = Policies.SEVENTEEN.getName() + " Policy Check";
        final String productId = Long.toString(groupData.getProductId());
        final String numberOfMembers = String.valueOf(groupData.getNumberOfMembers());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${prequalificationId}", prequalificationId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientArea}", clientArea);
        reportParams.put("${numberOfMembers}", numberOfMembers);
        reportParams.put("${requestedAmount}", String.valueOf(groupData.getRequestedAmount()));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Gender
     */
    private CheckValidationColor runCheck18(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.EIGHTEEN.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Nationality
     */
    private CheckValidationColor runCheck19(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.NINETEEN.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Internal Credit History
     */
    private CheckValidationColor runCheck20(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.TWENTY.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * External Credit History
     */
    private CheckValidationColor runCheck21(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.TWENTY_ONE.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Do you register any lawsuit?
     */
    private CheckValidationColor runCheck22(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.TWENTY_TWO.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final List<LoanAdditionProperties> loanAdditionPropertiesList = this.loanAdditionalPropertiesRepository
                .findByClientIdAndLoanId(clientData.getClientId(), clientData.getLoanId());
        String registerLawSuit = "si";
        if (!CollectionUtils.isEmpty(loanAdditionPropertiesList)) {
            final LoanAdditionProperties loanAdditionProperties = loanAdditionPropertiesList.get(0);
            if (!StringUtils.isEmpty(loanAdditionProperties.getCaseId())) {
                registerLawSuit = "no";
            }
        }
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        reportParams.put("${registerLawSuit}", registerLawSuit);
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Housing Type
     */
    private CheckValidationColor runCheck23(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.TWENTY_THREE.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Rental Age
     */
    private CheckValidationColor runCheck24(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.TWENTY_FOUR.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Age Of Business
     */
    private CheckValidationColor runCheck25(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.TWENTY_FIVE.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final List<LoanAdditionProperties> loanAdditionPropertiesList = this.loanAdditionalPropertiesRepository
                .findByClientIdAndLoanId(clientData.getClientId(), clientData.getLoanId());
        int yearsInBusiness = 0;
        if (!CollectionUtils.isEmpty(loanAdditionPropertiesList)) {
            final LoanAdditionProperties loanAdditionProperties = loanAdditionPropertiesList.get(0);
            String antiguedadNegocio = ObjectUtils.defaultIfNull(loanAdditionProperties.getAntiguedadNegocio(), "");
            yearsInBusiness = NumberUtils.toInt(antiguedadNegocio.replaceAll("[^0-9]", ""));
        }
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        reportParams.put("${businessAge}", String.valueOf(yearsInBusiness));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Credits
     */
    private CheckValidationColor runCheck26(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.TWENTY_SIX.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Cancelled Cycles Count
     */
    private CheckValidationColor runCheck27(final GroupData groupData) {
        String clientArea = "RURAL";
        if (!CollectionUtils.isEmpty(groupData.getMembers())) {
            final ClientData clientData = groupData.getMembers().get(0);
            final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
            clientArea = params.getClientArea();
        }
        final String prequalificationId = String.valueOf(groupData.getId());
        final String reportName = Policies.TWENTY_SEVEN.getName() + " Policy Check";
        final String productId = Long.toString(groupData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${prequalificationId}", prequalificationId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientArea}", clientArea);
        reportParams.put("${requestedAmount}", String.valueOf(groupData.getRequestedAmount()));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Acceptance of new clients
     */
    private CheckValidationColor runCheck28(final GroupData groupData) {
        String clientArea = "RURAL";
        String clientsRatio = "1";
        int newMembersCount = 0;
        int recurringMembersCount = 0;
        if (!CollectionUtils.isEmpty(groupData.getMembers())) {
            for (final ClientData memberData : groupData.getMembers()) {
                final ClientData params = retrieveClientParams(memberData.getClientId(), memberData.getProductId());
                clientArea = params.getClientArea();
                if ("NEW".equals(params.getClientCategorization())) {
                    newMembersCount++;
                } else {
                    recurringMembersCount++;
                }
            }
        }
        if (newMembersCount > 0 && recurringMembersCount > 0) {
            final Integer ratio = recurringMembersCount / newMembersCount;
            clientsRatio = String.valueOf(ratio);
        }
        final String prequalificationId = String.valueOf(groupData.getId());
        final String reportName = Policies.TWENTY_EIGHT.getName() + " Policy Check";
        final String productId = Long.toString(groupData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${prequalificationId}", prequalificationId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientArea}", clientArea);
        reportParams.put("${clientsRatio}", clientsRatio);
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Present agricultural technical diagnosis (Commcare)
     */
    private CheckValidationColor runCheck29(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.TWENTY_NINE.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final Long loanId = clientData.getLoanId();
        final String documentCountSql = """
                        SELECT COUNT(*)
                        FROM m_document md
                        LEFT JOIN m_code_value mcvd ON md.document_type = mcvd.id
                        WHERE md.parent_entity_type = 'loans' AND md.parent_entity_id = ? AND (md.name = ? OR mcvd.code_value = ?)
                """;
        Object[] params = new Object[] { loanId, "Diagnóstico técnico agrícola", "Diagnóstico técnico agrícola" };
        final Long documentCount = this.jdbcTemplate.queryForObject(documentCountSql, Long.class, params);
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${numberOfDocuments}", String.valueOf(documentCount));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Age
     */
    private CheckValidationColor runCheck30(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.THIRTY.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Amount
     */
    private CheckValidationColor runCheck31(final ClientData clientData) {
        final String clientId = String.valueOf(clientData.getClientId());
        final String reportName = Policies.THIRTY_ONE.getName() + " Policy Check";
        final String productId = Long.toString(clientData.getProductId());
        final ClientData params = retrieveClientParams(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", params.getClientCategorization());
        reportParams.put("${recreditCategorization}", params.getRecreditCategorization());
        reportParams.put("${requestedAmount}", String.valueOf(clientData.getRequestedAmount()));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Percentage of members with agricultural business
     */
    private CheckValidationColor runCheck32(final GroupData groupData) {
        final String reportName = Policies.THIRTY_TWO.getName() + " Policy Check";
        final List<ClientData> members = groupData.getMembers();
        final Integer totalMembers = members.size();
        final String numberOfMemberSQL = """
                SELECT COUNT(DISTINCT mc.id)
                FROM m_client mc
                INNER JOIN m_sector_economico mse ON mse.id = mc.economic_sector
                WHERE mc.id IN ( %s ) AND mse.name = 'Sector Agrícola'
                """;
        final String stmt = String.format(numberOfMemberSQL, members.stream().map(v -> "?").collect(Collectors.joining(", ")));
        List<Object> params = new ArrayList<>();
        members.forEach(m -> params.add(m.getClientId()));
        final Long numberOfMembers = this.jdbcTemplate.queryForObject(stmt, Long.class, params.toArray());
        final Map<String, String> reportParams = new HashMap<>();
        BigDecimal membersPercentage = BigDecimal.valueOf(100L);
        if (numberOfMembers != null && numberOfMembers < Long.valueOf(totalMembers)) {
            membersPercentage = BigDecimal.valueOf(100L)
                    .multiply(BigDecimal.valueOf(numberOfMembers).divide(BigDecimal.valueOf(totalMembers), MoneyHelper.getRoundingMode()));
        }
        reportParams.put("${membersPercentage}", String.valueOf(membersPercentage));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    /**
     * Percentage of members with their own business
     */
    private CheckValidationColor runCheck33(final GroupData groupData) {
        final String reportName = Policies.THIRTY_THREE.getName() + " Policy Check";
        final List<ClientData> members = groupData.getMembers();
        final Integer totalMembers = members.size();
        final String numberOfMemberSQL = """
                SELECT COUNT(DISTINCT mlag.loan_id) AS totalCount
                FROM m_loan_additionals_group mlag
                INNER JOIN m_code_value mcv ON mcv.id = mlag.job_type
                WHERE mlag.loan_id IN ( %s ) AND mcv.code_value = 'microentreprenuer'
                """;
        String stmt = String.format(numberOfMemberSQL, members.stream().map(v -> "?").collect(Collectors.joining(", ")));
        List<Object> params = new ArrayList<>();
        members.forEach(m -> params.add(m.getLoanId()));
        final Long numberOfMembers = this.jdbcTemplate.queryForObject(stmt, Long.class, params.toArray());
        final Map<String, String> reportParams = new HashMap<>();
        BigDecimal membersPercentage = BigDecimal.valueOf(100L);
        if (numberOfMembers != null && numberOfMembers < Long.valueOf(totalMembers)) {
            membersPercentage = BigDecimal.valueOf(100L)
                    .multiply(BigDecimal.valueOf(numberOfMembers).divide(BigDecimal.valueOf(totalMembers), MoneyHelper.getRoundingMode()));
        }
        reportParams.put("${membersPercentage}", String.valueOf(membersPercentage));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }
}
