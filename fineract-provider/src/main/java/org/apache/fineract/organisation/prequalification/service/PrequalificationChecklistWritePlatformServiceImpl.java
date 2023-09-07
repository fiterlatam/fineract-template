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
import java.util.List;
import javax.transaction.Transactional;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.prequalification.data.ClientData;
import org.apache.fineract.organisation.prequalification.data.HardPolicyCategoryData;
import org.apache.fineract.organisation.prequalification.domain.CheckValidationColor;
import org.apache.fineract.organisation.prequalification.domain.HardPolicyCategory;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroup;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatus;
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
public class PrequalificationChecklistWritePlatformServiceImpl implements PrequalificationChecklistWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(PrequalificationChecklistWritePlatformServiceImpl.class);
    private final PlatformSecurityContext context;
    private final PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper;
    private final ValidationChecklistResultRepository validationChecklistResultRepository;
    private final PlatformSecurityContext platformSecurityContext;
    private final JdbcTemplate jdbcTemplate;

    public PrequalificationChecklistWritePlatformServiceImpl(PlatformSecurityContext context,
            PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper,
            ValidationChecklistResultRepository validationChecklistResultRepository, PlatformSecurityContext platformSecurityContext,
            JdbcTemplate jdbcTemplate) {
        this.context = context;
        this.prequalificationGroupRepositoryWrapper = prequalificationGroupRepositoryWrapper;
        this.validationChecklistResultRepository = validationChecklistResultRepository;
        this.platformSecurityContext = platformSecurityContext;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public CommandProcessingResult validatePrequalificationHardPolicies(Long prequalificationId, JsonCommand command) {
        this.context.authenticatedUser();
        PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(prequalificationId);
        final Long productId = prequalificationGroup.getLoanProduct().getId();
        List<ValidationChecklistResult> validationChecklistResults = new ArrayList<>();
        CheckCategoryMapper checkCategoryMapper = new CheckCategoryMapper();
        List<HardPolicyCategoryData> groupPolicies = this.jdbcTemplate.query(checkCategoryMapper.schema(), checkCategoryMapper, productId,
                PrequalificationType.GROUP.getValue());
        List<HardPolicyCategoryData> individualPolicies = this.jdbcTemplate.query(checkCategoryMapper.schema(), checkCategoryMapper,
                productId, PrequalificationType.INDIVIDUAL.getValue());
        final String deleteStatement = "DELETE FROM m_checklist_validation_result WHERE prequalification_id = ?";
        this.jdbcTemplate.update(deleteStatement, prequalificationId);
        for (HardPolicyCategoryData policyCategoryData : groupPolicies) {
            ValidationChecklistResult prequalificationChecklistResult = new ValidationChecklistResult();
            prequalificationChecklistResult.setPrequalificationId(prequalificationId);
            prequalificationChecklistResult.setCategoryId(policyCategoryData.getId());
            prequalificationChecklistResult.setPrequalificationType(PrequalificationType.GROUP.getValue());
            CheckValidationColor checkValidationColor = this.validateGenericPolicy(HardPolicyCategory.fromInt(policyCategoryData.getId()),
                    null, prequalificationGroup);
            prequalificationChecklistResult.setValidationColor(checkValidationColor.getValue());
            AppUser authenticatedUser = platformSecurityContext.authenticatedUser();
            final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
            prequalificationChecklistResult.setCreatedBy(authenticatedUser.getId());
            prequalificationChecklistResult.setLastModifiedBy(authenticatedUser.getId());
            prequalificationChecklistResult.setCreatedDate(localDateTime);
            prequalificationChecklistResult.setLastModifiedDate(localDateTime);
            validationChecklistResults.add(prequalificationChecklistResult);
        }

        for (HardPolicyCategoryData policyCategoryData : individualPolicies) {
            final ClientDataMapper clientDataMapper = new ClientDataMapper();
            List<ClientData> clientDatas = this.jdbcTemplate.query(clientDataMapper.schema(), clientDataMapper, prequalificationId);
            for (ClientData clientData : clientDatas) {
                ValidationChecklistResult validationChecklistResult = new ValidationChecklistResult();
                validationChecklistResult.setPrequalificationId(prequalificationId);
                validationChecklistResult.setCategoryId(policyCategoryData.getId());
                validationChecklistResult.setClientId(clientData.getClientId());
                validationChecklistResult.setPrequalificationMemberId(clientData.getPrequalificationMemberId());
                validationChecklistResult.setPrequalificationType(PrequalificationType.INDIVIDUAL.getValue());
                CheckValidationColor checkValidationColor = this
                        .validateGenericPolicy(HardPolicyCategory.fromInt(policyCategoryData.getId()), clientData, prequalificationGroup);
                validationChecklistResult.setValidationColor(checkValidationColor.getValue());
                AppUser authenticatedUser = platformSecurityContext.authenticatedUser();
                final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
                validationChecklistResult.setCreatedBy(authenticatedUser.getId());
                validationChecklistResult.setLastModifiedBy(authenticatedUser.getId());
                validationChecklistResult.setCreatedDate(localDateTime);
                validationChecklistResult.setLastModifiedDate(localDateTime);
                validationChecklistResults.add(validationChecklistResult);
            }
        }
        prequalificationGroup.updateStatus(PrequalificationStatus.HARD_POLICY_CHECKED);
        prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);
        this.validationChecklistResultRepository.saveAll(validationChecklistResults);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationId).build();
    }

    static final class CheckCategoryMapper implements RowMapper<HardPolicyCategoryData> {

        public String schema() {
            return """
                    SELECT cc.id, cc.name, cc.description \s
                    FROM checklist_decision_making cdm\s
                    INNER JOIN checklist_categories cc ON cc.id = cdm.checklist_category_id\s
                    INNER JOIN m_product_loan mpl ON mpl.id = cdm.product_id\s
                    WHERE mpl.id = ? AND cdm.validation_type_enum = ?
                    GROUP BY cc.id""";
        }

        @Override
        public HardPolicyCategoryData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            final Integer id = JdbcSupport.getInteger(rs, "id");
            final String name = rs.getString("name");
            final String description = rs.getString("description");
            return new HardPolicyCategoryData(id, name, description);
        }
    }

    private static final class ClientDataMapper implements RowMapper<ClientData> {

        public String schema() {
            return """
                    SELECT mc.id AS clientId, mpgm.id AS prequalificationMemberId, IFNULL(mc.display_name,mpgm.name) AS name, mpg.id AS prequalificationId,\s
                    mpgm.requested_amount AS requestedAmount, IFNULL(mc.date_of_birth, mpgm.dob) AS dateOfBirth, IFNULL(mc.dpi, mpgm.dpi) AS dpi,
                    mpgm.work_with_puente AS workWithPuente
                    FROM m_prequalification_group_members mpgm\s
                    LEFT JOIN m_client mc ON mc.dpi = mpgm.dpi\s
                    LEFT JOIN m_prequalification_group mpg ON mpg.id = mpgm.group_id
                    WHERE mpg.id = ?
                    """;
        }

        @Override
        public ClientData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            final Integer clientId = JdbcSupport.getInteger(rs, "clientId");
            final Integer prequalificationMemberId = JdbcSupport.getInteger(rs, "prequalificationMemberId");
            final Integer prequalificationId = JdbcSupport.getInteger(rs, "prequalificationId");
            final String name = rs.getString("name");
            final Date dateOfBirth = rs.getDate("dateOfBirth");
            final String dpi = rs.getString("dpi");
            final BigDecimal requestedAmount = rs.getBigDecimal("requestedAmount");
            final String workWithPuente = rs.getString("workWithPuente");
            return new ClientData(clientId, prequalificationId, prequalificationMemberId, name, dateOfBirth, dpi, requestedAmount,
                    workWithPuente);
        }
    }

    private CheckValidationColor validateGenericPolicy(final HardPolicyCategory hardPolicyCategory, final ClientData clientData,
            final PrequalificationGroup prequalificationGroup) {
        CheckValidationColor checkValidationColor;
        switch (hardPolicyCategory) {
            case NEW_CLIENT -> checkValidationColor = this.handleNewClientCheck(clientData);
            case RECURRING_CUSTOMER -> checkValidationColor = this.handleRecurringClientCheck();
            case INCREASE_PERCENTAGE -> checkValidationColor = this.handleIncreasePercentageCheck();
            case MANDATORY_PHOTO_GRAPH -> checkValidationColor = this.handleMandatoryPhotographCheck();
            case CLIENT_AGE -> checkValidationColor = this.handleClientAgeCheck();
            case NUMBER_OF_MEMBERS_ACCORDING_TO_POLICY -> checkValidationColor = this.handleNumberOfMembersAccordingToPolicyCheck();
            case MINIMUM_AND_MAXIMUM_AMOUNT -> checkValidationColor = this.handleMinAndMaxAmountCheck();
            case DISPARITY_OF_VALUES -> checkValidationColor = this.handleDisparitiesOfValuesCheck();
            case PERCENTAGE_OF_MEMBERS_STARTING_BUSINESS -> checkValidationColor = this.handlePercentageOfMemberStartingBusinessCheck();
            case PERCENTAGE_OF_MEMBERS_WITH_THEIR_OWN_HOME -> checkValidationColor = this.handlePercentageOfMemberWithHomeCheck();
            case CHAIRMAN_OF_THE_BC_BOARD_OF_DIRECTORS -> checkValidationColor = this.handleChairmanBoardOfDirectorsCheck();
            case OVERALL_CONDITION -> checkValidationColor = this.handleOverallConditionCheck();
            case CATEGORIES_OF_CLIENT_TO_ACCEPT -> checkValidationColor = this.handleCategoriesOfClientToAcceptCheck();
            case REQUESTED_AMOUNT -> checkValidationColor = this.handleRequestedAmountCheck();
            case ADD_ENDORSEMENT -> checkValidationColor = this.handleAddEndorsementCheck();
            case PAYMENTS_OUTSIDE_CURRENT_TERM -> checkValidationColor = this.handlePaymentOutsideCurrentTermCheck();
            case PERCENTAGE_OF_MEMBERS_THAT_CAN_HAVE_PRODUCT -> checkValidationColor = this.handPercentageOfProductMemberCheck();
            case GENDER -> checkValidationColor = this.handleGenderCheck();
            case NATIONALITY -> checkValidationColor = this.handleNationalityCheck();
            case INTERNAL_CREDIT_HISTORY -> checkValidationColor = this.handleInternalCreditHistoryCheck();
            case EXTERNAL_CREDIT_HISTORY -> checkValidationColor = this.handleExternalCreditHistoryCheck();
            case CLAIMS_REGISTERED -> checkValidationColor = this.handleClaimedRegisteredCheck();
            case HOUSING_TYPE -> checkValidationColor = this.handleHousingTypeCheck();
            case RENTAL_AGE -> checkValidationColor = this.handleRentalAgeCheck();
            case AGE_OF_BUSINESS -> checkValidationColor = this.handleBusinessAgeCheck();
            case CREDITS -> checkValidationColor = this.handleCreditCheck();
            case CANCELLED_CYCLES_COUNT -> checkValidationColor = this.handleCancelledCyclesCheck();
            case SUBMIT_AGRICULTURAL_TECHNICAL_DIAGNOSIS -> checkValidationColor = this.handleAgriculturalDiagnosisCheck();
            case ACCEPTANCE_OF_NEW_CLIENTS -> checkValidationColor = this.handleAcceptanceOfNewClientCheck();
            default -> checkValidationColor = CheckValidationColor.INVALID;
        }
        return checkValidationColor;
    }

    private CheckValidationColor handleNewClientCheck(final ClientData clientData) {
        CheckValidationColor checkValidationColor = CheckValidationColor.GREEN;
        final Integer clientId = clientData.getClientId();
        if (clientId != null) {
            final String sql = "SELECT COALESCE(MAX(ml.loan_counter), 0) FROM m_loan ml WHERE ml.client_id = ?";
            Integer loanCycleCount = this.jdbcTemplate.queryForObject(sql, Integer.class, clientId);
            if (0 == loanCycleCount) {
                checkValidationColor = CheckValidationColor.GREEN;
            }

        }
        return checkValidationColor;
    }

    private CheckValidationColor handleRecurringClientCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleIncreasePercentageCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleMandatoryPhotographCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleClientAgeCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleNumberOfMembersAccordingToPolicyCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleMinAndMaxAmountCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleDisparitiesOfValuesCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handlePercentageOfMemberStartingBusinessCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handlePercentageOfMemberWithHomeCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleChairmanBoardOfDirectorsCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleOverallConditionCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleCategoriesOfClientToAcceptCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleRequestedAmountCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleAddEndorsementCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handlePaymentOutsideCurrentTermCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handPercentageOfProductMemberCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleGenderCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleNationalityCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleInternalCreditHistoryCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleExternalCreditHistoryCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleClaimedRegisteredCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleHousingTypeCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleRentalAgeCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleBusinessAgeCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleCreditCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleCancelledCyclesCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleAgriculturalDiagnosisCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleAcceptanceOfNewClientCheck() {
        return CheckValidationColor.GREEN;
    }
}
