/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.organisation.prequalification.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.transaction.Transactional;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.prequalification.data.ClientData;
import org.apache.fineract.organisation.prequalification.data.HardPolicyCategoryData;
import org.apache.fineract.organisation.prequalification.domain.CheckValidationColor;
import org.apache.fineract.organisation.prequalification.domain.HardPolicyCategory;
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
public class PrequalificationChecklistWritePlatformServiceImpl implements PrequalificationChecklistWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(PrequalificationChecklistWritePlatformServiceImpl.class);
    private final PlatformSecurityContext context;
    private final PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper;
    private final ValidationChecklistResultRepository validationChecklistResultRepository;
    private final PreQualificationStatusLogRepository preQualificationStatusLogRepository;
    private final PlatformSecurityContext platformSecurityContext;
    private final JdbcTemplate jdbcTemplate;

    public PrequalificationChecklistWritePlatformServiceImpl(PlatformSecurityContext context,
                                                             PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper,
                                                             final PreQualificationStatusLogRepository preQualificationStatusLogRepository,
                                                             ValidationChecklistResultRepository validationChecklistResultRepository, PlatformSecurityContext platformSecurityContext,
                                                             JdbcTemplate jdbcTemplate) {
        this.context = context;
        this.prequalificationGroupRepositoryWrapper = prequalificationGroupRepositoryWrapper;
        this.validationChecklistResultRepository = validationChecklistResultRepository;
        this.preQualificationStatusLogRepository = preQualificationStatusLogRepository;
        this.platformSecurityContext = platformSecurityContext;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public CommandProcessingResult validatePrequalificationHardPolicies(Long prequalificationId, JsonCommand command) {
        AppUser appUser = this.context.authenticatedUser();
        PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(prequalificationId);
        final Long productId = prequalificationGroup.getLoanProduct().getId();

        Integer fromStatus = prequalificationGroup.getStatus();

        List<ValidationChecklistResult> validationChecklistResults = new ArrayList<>();
        CheckCategoryMapper checkCategoryMapper = new CheckCategoryMapper();
        List<HardPolicyCategoryData> groupPolicies = this.jdbcTemplate.query(checkCategoryMapper.schema(), checkCategoryMapper, productId,
                PrequalificationType.GROUP.getValue());
        List<HardPolicyCategoryData> individualPolicies = this.jdbcTemplate.query(checkCategoryMapper.schema(), checkCategoryMapper,
                productId, PrequalificationType.INDIVIDUAL.getValue());

        final String deleteStatement = "DELETE FROM m_checklist_validation_result WHERE prequalification_id = ?";
        this.jdbcTemplate.update(deleteStatement, prequalificationId);
        BigDecimal totalAmount = prequalificationGroup.getMembers().stream().map(PrequalificationGroupMember::getRequestedAmount)
                .reduce(BigDecimal.ONE, BigDecimal::add);
        Boolean recredito = prequalificationGroup.getParent() != null;
        final ClientData groupData = ClientData.builder().requestedAmount(totalAmount).recredito(recredito).build();
        for (HardPolicyCategoryData policyCategoryData : groupPolicies) {
            ValidationChecklistResult prequalificationChecklistResult = new ValidationChecklistResult();
            prequalificationChecklistResult.setPrequalificationId(prequalificationId);
            prequalificationChecklistResult.setCategoryId(policyCategoryData.getId());
            prequalificationChecklistResult.setPrequalificationType(PrequalificationType.GROUP.getValue());
            CheckValidationColor checkValidationColor = this.validateGenericPolicy(HardPolicyCategory.fromInt(policyCategoryData.getId()),
                    groupData, prequalificationGroup);
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
            for (final ClientData clientData : clientDatas) {
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

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(appUser, fromStatus, prequalificationGroup.getStatus(),
                null, prequalificationGroup);

        this.preQualificationStatusLogRepository.saveAndFlush(statusLog);

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
                    SELECT mc.id AS clientId, mpgm.id AS prequalificationMemberId, IFNULL(mc.display_name,mpgm.name) AS name, 
                    mpg.id AS prequalificationId, 
                    (select count(id) from m_loan where client_id = mc.id and loan_status_id = 100)as pendingLoansCount,
                    mpgm.requested_amount AS requestedAmount, IFNULL(mc.date_of_birth, mpgm.dob) AS dateOfBirth, 
                    IFNULL(mc.dpi, mpgm.dpi) AS dpi, mca.code_value as clientArea,
                    mpgm.work_with_puente AS workWithPuente, mcv.code_value As gender
                    FROM m_prequalification_group_members mpgm 
                    LEFT JOIN m_client mc ON mc.dpi = mpgm.dpi 
                    LEFT JOIN m_client_contact_info mci ON mci.client_id = mc.id 
                    LEFT JOIN m_code_value mca ON mca.id = mci.area
                    LEFT JOIN m_code_value mcv ON mcv.id = mc.gender_cv_id
                    LEFT JOIN m_prequalification_group mpg ON mpg.id = mpgm.group_id
                    WHERE mpg.id = ?
                    """;
        }

        @Override
        public ClientData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            final Integer clientId = JdbcSupport.getInteger(rs, "clientId");
            final Integer prequalificationMemberId = JdbcSupport.getInteger(rs, "prequalificationMemberId");
            final Integer prequalificationId = JdbcSupport.getInteger(rs, "prequalificationId");
            final Integer pendingLoansCount = JdbcSupport.getInteger(rs, "pendingLoansCount");
            final String name = rs.getString("name");
            final Date dateOfBirth = rs.getDate("dateOfBirth");
            final String dpi = rs.getString("dpi");
            final BigDecimal requestedAmount = rs.getBigDecimal("requestedAmount");
            final String workWithPuente = rs.getString("workWithPuente");
            final String gender = rs.getString("gender");
            final String clientArea = rs.getString("clientArea");

            return ClientData.builder().clientId(clientId).prequalificationId(prequalificationId)
                    .prequalificationMemberId(prequalificationMemberId).name(name).dateOfBirth(dateOfBirth).dpi(dpi)
                    .requestedAmount(requestedAmount).gender(gender).workWithPuente(workWithPuente).ruralUrban(clientArea).recredito(pendingLoansCount > 0).build();
        }
    }

    private CheckValidationColor validateGenericPolicy(final HardPolicyCategory hardPolicyCategory, final ClientData clientData,
                                                       final PrequalificationGroup prequalificationGroup) {
        CheckValidationColor checkValidationColor;
        switch (hardPolicyCategory) {
            case NEW_CLIENT -> checkValidationColor = this.handleNewClientCheck(clientData);
            case RECURRING_CUSTOMER -> checkValidationColor = this.handleRecurringClientCheck(clientData, prequalificationGroup);
            case INCREASE_PERCENTAGE -> checkValidationColor = this.handleIncreasePercentageCheck(clientData, prequalificationGroup);
            case MANDATORY_PHOTO_GRAPH -> checkValidationColor = this.handleMandatoryPhotographCheck();
            case CLIENT_AGE -> checkValidationColor = this.handleClientAgeCheck(clientData);
            case NUMBER_OF_MEMBERS_ACCORDING_TO_POLICY -> checkValidationColor = this
                    .handleNumberOfMembersAccordingToPolicyCheck(clientData,prequalificationGroup);
            case MINIMUM_AND_MAXIMUM_AMOUNT -> checkValidationColor = this.handleMinAndMaxAmountCheck(prequalificationGroup, clientData);
            case DISPARITY_OF_VALUES -> checkValidationColor = this.handleDisparitiesOfValuesCheck();
            case PERCENTAGE_OF_MEMBERS_STARTING_BUSINESS -> checkValidationColor = this.handlePercentageOfMemberStartingBusinessCheck();
            case PERCENTAGE_OF_MEMBERS_WITH_THEIR_OWN_HOME -> checkValidationColor = this.handlePercentageOfMemberWithHomeCheck();
            case CHAIRMAN_OF_THE_BC_BOARD_OF_DIRECTORS -> checkValidationColor = this.handleChairmanBoardOfDirectorsCheck();
            case OVERALL_CONDITION -> checkValidationColor = this.handleOverallConditionCheck();
            case CATEGORIES_OF_CLIENT_TO_ACCEPT -> checkValidationColor = this.handleCategoriesOfClientToAcceptCheck();
            case REQUESTED_AMOUNT -> checkValidationColor = this.handleRequestedAmountCheck(clientData);
            case ADD_ENDORSEMENT -> checkValidationColor = this.handleAddEndorsementCheck();
            case PAYMENTS_OUTSIDE_CURRENT_TERM -> checkValidationColor = this.handlePaymentOutsideCurrentTermCheck();
            case PERCENTAGE_OF_MEMBERS_THAT_CAN_HAVE_PRODUCT -> checkValidationColor = this.handPercentageOfProductMemberCheck();
            case GENDER -> checkValidationColor = this.handleGenderCheck(clientData);
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
        CheckValidationColor checkValidationColor;
        final Integer clientId = clientData.getClientId();
        final String loanCycle = "SELECT COALESCE(MAX(ml.loan_counter), 0) FROM m_loan ml WHERE ml.client_id = ?";
        final Integer loanCycleCount = this.jdbcTemplate.queryForObject(loanCycle, Integer.class, clientId);
        final String closedLoanCountSql = "SELECT COALESCE(COUNT(*), 0) FROM m_loan ml WHERE ml.loan_status_id IN (700, 600) AND  ml.client_id = ?  ";
        final Integer closedLoanCount = this.jdbcTemplate.queryForObject(closedLoanCountSql, Integer.class, clientId);
        final String notActiveSql = "SELECT COALESCE(COUNT(*), 0) FROM m_loan ml WHERE ml.loan_status_id NOT IN (100, 200, 300, 303, 304, 400, 500, 601, 602) AND  ml.client_id = ?  ";
        final Integer notActiveCount = this.jdbcTemplate.queryForObject(notActiveSql, Integer.class, clientId);
        if (ObjectUtils.defaultIfNull(loanCycleCount, 0) == 0) {
            checkValidationColor = CheckValidationColor.GREEN;
        } else if (ObjectUtils.defaultIfNull(closedLoanCount, 0) > 0) {
            checkValidationColor = CheckValidationColor.GREEN;
        } else if (ObjectUtils.defaultIfNull(notActiveCount, 0) == 1) {
            checkValidationColor = CheckValidationColor.GREEN;
        } else {
            checkValidationColor = CheckValidationColor.RED;
        }
        return checkValidationColor;
    }

    private CheckValidationColor handleRecurringClientCheck(final ClientData clientData,
                                                            final PrequalificationGroup prequalificationGroup) {
        final CheckValidationColor checkValidationColor;
        final String loanCycle = "SELECT COALESCE(MAX(ml.loan_counter), 0) FROM m_loan ml WHERE ml.client_id = ?";
        final Integer clientId = clientData.getClientId();
        final Long productId = prequalificationGroup.getLoanProduct().getId();
        final Integer loanCycleCount = this.jdbcTemplate.queryForObject(loanCycle, Integer.class, clientId);
        if (List.of(2L, 8L, 9L).contains(productId) && ObjectUtils.defaultIfNull(loanCycleCount, 0) > 0) {
            checkValidationColor = CheckValidationColor.GREEN;
        } else if (List.of(4L, 5L).contains(productId) && ObjectUtils.defaultIfNull(loanCycleCount, 0) > 3) {
            checkValidationColor = CheckValidationColor.GREEN;
        } else {
            checkValidationColor = CheckValidationColor.RED;
        }
        return checkValidationColor;
    }

    private CheckValidationColor handleIncreasePercentageCheck(final ClientData clientData,
                                                               final PrequalificationGroup prequalificationGroup) {
        final CheckValidationColor checkValidationColor;
        final Integer clientId = clientData.getClientId();
        final Long productId = prequalificationGroup.getLoanProduct().getId();
        BigDecimal requestedAmount = clientData.getRequestedAmount();
        final String principalAmtSql = "SELECT COALESCE(ml.principal_amount, 0) AS principalAmount FROM m_loan ml WHERE ml.client_id = ? "
                + "AND ml.loan_status_id IN (700, 600) ORDER BY ml.disbursedon_date DESC";
        final List<BigDecimal> principalAmounts = jdbcTemplate.queryForList(principalAmtSql, BigDecimal.class, clientId);
        BigDecimal previousPrincipalAmount = !principalAmounts.isEmpty() ? principalAmounts.get(0) : BigDecimal.ZERO;
        BigDecimal percentageIncrease = BigDecimal.valueOf(100);
        final MathContext mc = new MathContext(8, MoneyHelper.getRoundingMode());
        if (previousPrincipalAmount.compareTo(BigDecimal.ZERO) > 0) {
            percentageIncrease = requestedAmount.subtract(previousPrincipalAmount).multiply(BigDecimal.valueOf(100))
                    .divide(previousPrincipalAmount, mc);
        }
        if (List.of(2L, 9L).contains(productId) && percentageIncrease.compareTo(BigDecimal.valueOf(200)) == 0) {
            checkValidationColor = CheckValidationColor.GREEN;
        } else if (List.of(2L, 9L).contains(productId) && percentageIncrease.compareTo(BigDecimal.valueOf(201)) >= 0
                && percentageIncrease.compareTo(BigDecimal.valueOf(500)) <= 0) {
            checkValidationColor = CheckValidationColor.YELLOW;
        } else if (List.of(2L, 9L).contains(productId) && percentageIncrease.compareTo(BigDecimal.valueOf(500)) >= 0) {
            checkValidationColor = CheckValidationColor.RED;
        } else if (List.of(4L, 5L).contains(productId) && percentageIncrease.compareTo(BigDecimal.valueOf(60)) <= 0) {
            checkValidationColor = CheckValidationColor.GREEN;
        } else if (List.of(4L, 5L).contains(productId) && percentageIncrease.compareTo(BigDecimal.valueOf(60)) > 0) {
            checkValidationColor = CheckValidationColor.ORANGE;
        } else {
            checkValidationColor = CheckValidationColor.RED;
        }
        return checkValidationColor;
    }

    private CheckValidationColor handleMandatoryPhotographCheck() {
        return CheckValidationColor.GREEN;
    }

    private CheckValidationColor handleClientAgeCheck(final ClientData clientData) {
        final CheckValidationColor checkValidationColor;
        java.util.Date dateOfBirth = clientData.getDateOfBirth(); // .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate localDate = Instant.ofEpochMilli(dateOfBirth.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate businessDate = DateUtils.getBusinessLocalDate();
        int age = businessDate.getYear() - localDate.getYear();
        if (age >= 20 && age <= 60) {
            checkValidationColor = CheckValidationColor.GREEN;
        } else {
            checkValidationColor = CheckValidationColor.RED;
        }
        return checkValidationColor;
    }

    private CheckValidationColor handleNumberOfMembersAccordingToPolicyCheck(ClientData clientData, final PrequalificationGroup prequalificationGroup) {
        final CheckValidationColor checkValidationColor;
        final int membersCount = prequalificationGroup.getMembers().size();
        //default for new
        int maximumNumber = 10;
        int minimumNumber = 4;
        //check if recredito urban and rural
        if (clientData.getRecredito()){
            maximumNumber = clientData.getRuralUrban().equals("Rural") ? 10 : 15;
            minimumNumber = clientData.getRuralUrban().equals("Rural") ? 3 : 4;
        }

        if (membersCount > maximumNumber) {
            checkValidationColor = CheckValidationColor.GREEN;
        } else if (membersCount < minimumNumber) {
            checkValidationColor = CheckValidationColor.ORANGE;
        } else {
            checkValidationColor = CheckValidationColor.RED;
        }

        return checkValidationColor;
    }

    private CheckValidationColor handleMinAndMaxAmountCheck(final PrequalificationGroup prequalificationGroup,
                                                            final ClientData clientData) {
        final CheckValidationColor checkValidationColor;


        //TODO -RESOLVE MAX AND MIN AMOUNTS FROM PRODUCT
        BigDecimal minimumAmount = BigDecimal.valueOf(1000);
        BigDecimal maximumAmount = BigDecimal.valueOf(20000);

        //check for recredit urban and rural
        if (clientData.getRecredito()){
            minimumAmount = new BigDecimal(clientData.getRuralUrban().equals("Rural") ? 5000 : 6000);
            maximumAmount = new BigDecimal(clientData.getRuralUrban().equals("Rural") ? 15000 : 20000);
        }

        BigDecimal requestedAmount = ObjectUtils.defaultIfNull(clientData.getRequestedAmount(), BigDecimal.ZERO);

        if (minimumAmount.compareTo(requestedAmount) > 0 && maximumAmount.compareTo(requestedAmount) < 0) {
            checkValidationColor = CheckValidationColor.GREEN;
        } else {
            checkValidationColor = CheckValidationColor.RED;
        }
        return checkValidationColor;
    }

    private CheckValidationColor handleDisparitiesOfValuesCheck() {
        //TODO -RESOLVE DISPARITIES OF VALUES
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

    private CheckValidationColor handleRequestedAmountCheck(final ClientData clientData) {
        final CheckValidationColor checkValidationColor;
        BigDecimal minimumAmount = BigDecimal.valueOf(0);
        BigDecimal maximumAmount = BigDecimal.valueOf(3000);

        //check for recredit client.
        if (clientData.getRecredito()){
            minimumAmount = new BigDecimal(1000);
            maximumAmount = new BigDecimal(200000);
        }

        BigDecimal requestedAmount = ObjectUtils.defaultIfNull(clientData.getRequestedAmount(), BigDecimal.ZERO);
        if (requestedAmount.compareTo(minimumAmount) >= 0 && requestedAmount.compareTo(maximumAmount) <= 0) {
            checkValidationColor = CheckValidationColor.GREEN;
        } else {
            checkValidationColor = CheckValidationColor.RED;
        }
        return checkValidationColor;
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

    private CheckValidationColor handleGenderCheck(final ClientData clientData) {
        final CheckValidationColor checkValidationColor;
        if ("Mujer".equalsIgnoreCase(clientData.getGender())) {
            checkValidationColor = CheckValidationColor.GREEN;
        } else {
            checkValidationColor = CheckValidationColor.ORANGE;
        }
        return checkValidationColor;
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
