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
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
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
import org.apache.fineract.organisation.prequalification.data.ClientData;
import org.apache.fineract.organisation.prequalification.data.GroupData;
import org.apache.fineract.organisation.prequalification.data.PolicyData;
import org.apache.fineract.organisation.prequalification.domain.CheckValidationColor;
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
import org.apache.fineract.useradministration.domain.AppUser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

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
    private final ReadReportingService readReportingService;

    @Override
    @Transactional
    public CommandProcessingResult validatePrequalificationHardPolicies(Long prequalificationId, JsonCommand command) {
        AppUser appUser = this.context.authenticatedUser();
        PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(prequalificationId);
        final Long productId = prequalificationGroup.getLoanProduct().getId();
        final Integer noOfMembers = prequalificationGroup.getMembers().size();
        final BigDecimal totalAmountRequested = prequalificationGroup.getMembers().stream()
                .map(PrequalificationGroupMember::getRequestedAmount).reduce(BigDecimal.ONE, BigDecimal::add);
        final GroupData groupData = GroupData.builder().id(prequalificationId).productId(productId).noOfMembers(noOfMembers)
                .requestedAmount(totalAmountRequested).build();
        Integer fromStatus = prequalificationGroup.getStatus();
        List<ValidationChecklistResult> validationChecklistResults = new ArrayList<>();
        final List<PolicyData> groupPolicies = this.jdbcTemplate.query(this.policyMapper.schema(), this.policyMapper, productId,
                PrequalificationType.GROUP.name());
        final List<PolicyData> individualPolicies = this.jdbcTemplate.query(policyMapper.schema(), policyMapper, productId,
                PrequalificationType.INDIVIDUAL.name());
        final String deleteStatement = "DELETE FROM m_checklist_validation_result WHERE prequalification_id = ?";
        this.jdbcTemplate.update(deleteStatement, prequalificationId);
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

        for (PolicyData policyCategoryData : individualPolicies) {
            final ClientDataMapper clientDataMapper = new ClientDataMapper();
            List<ClientData> clientDatas = this.jdbcTemplate.query(clientDataMapper.schema(), clientDataMapper, prequalificationId);
            for (final ClientData clientData : clientDatas) {
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
        prequalificationGroup.updateStatus(PrequalificationStatus.HARD_POLICY_CHECKED);
        prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);
        this.validationChecklistResultRepository.saveAll(validationChecklistResults);

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(appUser, fromStatus, prequalificationGroup.getStatus(),
                null, prequalificationGroup);
        this.preQualificationStatusLogRepository.saveAndFlush(statusLog);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationId).build();
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
                    mpgm.work_with_puente AS workWithPuente, mcv.code_value As gender
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
            final Date dateOfBirth = rs.getDate("dateOfBirth");
            final String dpi = rs.getString("dpi");
            final BigDecimal requestedAmount = rs.getBigDecimal("requestedAmount");
            final String workWithPuente = rs.getString("workWithPuente");
            final String gender = rs.getString("gender");
            return ClientData.builder().clientId(clientId).prequalificationId(prequalificationId)
                    .prequalificationMemberId(prequalificationMemberId).name(name).dateOfBirth(dateOfBirth).dpi(dpi)
                    .requestedAmount(requestedAmount).gender(gender).workWithPuente(workWithPuente).build();
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
            case SEVEN -> checkValidationColor = this.runCheck7(groupData, clientData);
            case EIGHT -> checkValidationColor = this.runCheck8();
            case NINE -> checkValidationColor = this.runCheck9();
            case TEN -> checkValidationColor = this.runCheck10();
            case ELEVEN -> checkValidationColor = this.runCheck11();
            case TWELVE -> checkValidationColor = this.runCheck12();
            case THIRTEEN -> checkValidationColor = this.runCheck13();
            case FOURTEEN -> checkValidationColor = this.runCheck14(clientData);
            case FIFTEEN -> checkValidationColor = this.runCheck15();
            case SIXTEEN -> checkValidationColor = this.runCheck16();
            case SEVENTEEN -> checkValidationColor = this.runCheck17();
            case EIGHTEEN -> checkValidationColor = this.runCheck18(clientData);
            case NINETEEN -> checkValidationColor = this.runCheck19();
            case TWENTY -> checkValidationColor = this.runCheck20();
            case TWENTY_ONE -> checkValidationColor = this.runCheck21();
            case TWENTY_TWO -> checkValidationColor = this.runCheck22();
            case TWENTY_THREE -> checkValidationColor = this.runCheck23();
            case TWENTY_FOUR -> checkValidationColor = this.runCheck24();
            case TWENTY_FIVE -> checkValidationColor = this.runCheck25();
            case TWENTY_SIX -> checkValidationColor = this.runCheck26();
            case TWENTY_SEVEN -> checkValidationColor = this.runCheck27();
            case TWENTY_EIGHT -> checkValidationColor = this.runCheck28();
            case TWENTY_NINE -> checkValidationColor = this.runCheck29();
            case THIRTY -> checkValidationColor = this.runCheck30();
            case THIRTY_ONE -> checkValidationColor = this.runCheck31();
            case THIRTY_TWO -> checkValidationColor = this.runCheck32();
            case THIRTY_THREE -> checkValidationColor = this.runCheck33();
            case THIRTY_FOUR -> checkValidationColor = this.runCheck34();
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
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${requestedAmount}", String.valueOf(clientData.getRequestedAmount()));
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
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${requestedAmount}", String.valueOf(clientData.getRequestedAmount()));
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
        final String clientCategorization = retrieveClientCategorization(clientData.getClientId(), clientData.getProductId());
        final Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", clientId);
        reportParams.put("${loanProductId}", productId);
        reportParams.put("${clientCategorization}", clientCategorization);
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        return extractColorFromResultset(result);
    }

    private String retrieveClientCategorization(final Long clientId, final Long productId) {
        final String reportName = "Client Categorization Policy Check";
        ;
        Map<String, String> reportParams = new HashMap<>();
        reportParams.put("${clientId}", String.valueOf(clientId));
        reportParams.put("${loanProductId}", String.valueOf(productId));
        final GenericResultsetData result = this.readReportingService.retrieveGenericResultset(reportName, "report", reportParams, false);
        final List<ResultsetColumnHeaderData> columnHeaders = result.getColumnHeaders();
        List<ResultsetRowData> rowDataList = result.getData();
        String clientCategorization = "NEW";
        if (!rowDataList.isEmpty()) {
            for (int i = 0; i < columnHeaders.size(); i++) {
                final ResultsetColumnHeaderData columnHeaderData = columnHeaders.get(i);
                if ("clientCategorization".equals(columnHeaderData.getColumnName())) {
                    List<String> rowList = rowDataList.get(0).getRow();
                    clientCategorization = rowList.get(i);
                }
            }
        }
        return clientCategorization;
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
                    colorResult = CheckValidationColor.valueOf(color);
                }
            }
        }
        return colorResult;
    }

    /**
     * Number of members according to policy
     */
    private CheckValidationColor runCheck6(final GroupData groupData) {
        return CheckValidationColor.RED;
    }

    /**
     * Minimum and maximum amount
     */
    private CheckValidationColor runCheck7(final GroupData groupData, final ClientData clientData) {
        return CheckValidationColor.RED;
    }

    /**
     * Value disparity
     */
    private CheckValidationColor runCheck8() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Percentage of members starting business
     */
    private CheckValidationColor runCheck9() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Percentage of members with their own home
     */
    private CheckValidationColor runCheck10() {
        return CheckValidationColor.GREEN;
    }

    /**
     * President of the Board of Directors of the BC
     */
    private CheckValidationColor runCheck11() {
        return CheckValidationColor.GREEN;
    }

    /**
     * General condition
     */
    private CheckValidationColor runCheck12() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Categories of clients to accept
     */
    private CheckValidationColor runCheck13() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Amount requested in relation to the current amount of main products
     */
    private CheckValidationColor runCheck14(final ClientData clientData) {
        return CheckValidationColor.RED;
    }

    /**
     * Add endorsement
     */
    private CheckValidationColor runCheck15() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Payments outside the current term of the main product
     */
    private CheckValidationColor runCheck16() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Percentage of members of the same group who they can have parallel product
     */
    private CheckValidationColor runCheck17() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Gender
     */
    private CheckValidationColor runCheck18(final ClientData clientData) {
        return CheckValidationColor.RED;
    }

    /**
     * Nationality
     */
    private CheckValidationColor runCheck19() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Internal Credit History
     */
    private CheckValidationColor runCheck20() {
        return CheckValidationColor.GREEN;
    }

    /**
     * External Credit History
     */
    private CheckValidationColor runCheck21() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Do you register any lawsuit?
     */
    private CheckValidationColor runCheck22() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Housing Type
     */
    private CheckValidationColor runCheck23() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Rental Age
     */
    private CheckValidationColor runCheck24() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Age Of Business
     */
    private CheckValidationColor runCheck25() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Credits
     */
    private CheckValidationColor runCheck26() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Cancelled Cycles Count
     */
    private CheckValidationColor runCheck27() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Mandatory to attach photographs and investment plan
     */
    private CheckValidationColor runCheck28() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Acceptance of new clients
     */
    private CheckValidationColor runCheck29() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Present agricultural technical diagnosis (Commcare)
     */
    private CheckValidationColor runCheck30() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Age
     */
    private CheckValidationColor runCheck31() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Amount
     */
    private CheckValidationColor runCheck32() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Percentage of members with agricultural business
     */
    private CheckValidationColor runCheck33() {
        return CheckValidationColor.GREEN;
    }

    /**
     * Percentage of members with their own business
     */
    private CheckValidationColor runCheck34() {
        return CheckValidationColor.GREEN;
    }
}
