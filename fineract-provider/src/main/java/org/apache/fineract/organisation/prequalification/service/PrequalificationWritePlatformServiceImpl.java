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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.agency.domain.Agency;
import org.apache.fineract.organisation.agency.domain.AgencyRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.command.PrequalificationDataValidator;
import org.apache.fineract.organisation.prequalification.command.PrequalificatoinApiConstants;
import org.apache.fineract.organisation.prequalification.data.GenericValidationResultSet;
import org.apache.fineract.organisation.prequalification.data.PrequalificationChecklistData;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationStatusLogRepository;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroup;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupMember;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupMemberRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationMemberIndication;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatus;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatusLog;
import org.apache.fineract.organisation.prequalification.exception.PrequalificationStatusNotChangedException;
import org.apache.fineract.organisation.prequalification.serialization.PrequalificationMemberCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistStatus;
import org.apache.fineract.portfolio.client.service.ClientChargeWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.domain.GroupRepositoryWrapper;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
public class PrequalificationWritePlatformServiceImpl implements PrequalificationWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientChargeWritePlatformServiceJpaRepositoryImpl.class);

    private final PlatformSecurityContext context;
    private final PrequalificationDataValidator dataValidator;
    private final LoanProductRepository loanProductRepository;
    private final ClientReadPlatformService clientReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper;
    private final PreQualificationStatusLogRepository preQualificationLogRepository;
    private final PrequalificationChecklistReadPlatformService prequalificationChecklistReadPlatformService;
    private final PrequalificationGroupMemberRepositoryWrapper preQualificationMemberRepository;
    private final GroupRepositoryWrapper groupRepositoryWrapper;
    private final AppUserRepository appUserRepository;
    private final AgencyRepositoryWrapper agencyRepositoryWrapper;
    private final PrequalificationMemberCommandFromApiJsonDeserializer apiJsonDeserializer;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PrequalificationWritePlatformServiceImpl(final PlatformSecurityContext context,
            final PrequalificationDataValidator dataValidator, final GroupRepositoryWrapper groupRepositoryWrapper,
            final AppUserRepository appUserRepository, final LoanProductRepository loanProductRepository,
            final ClientReadPlatformService clientReadPlatformService, final AgencyRepositoryWrapper agencyRepositoryWrapper,
            final PrequalificationMemberCommandFromApiJsonDeserializer apiJsonDeserializer,
            final PrequalificationGroupMemberRepositoryWrapper preQualificationMemberRepository,
            final PreQualificationStatusLogRepository preQualificationLogRepository,
            final PrequalificationChecklistReadPlatformService prequalificationChecklistReadPlatformService,
            final CodeValueReadPlatformService codeValueReadPlatformService, final JdbcTemplate jdbcTemplate,
            final PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper) {
        this.context = context;
        this.dataValidator = dataValidator;
        this.loanProductRepository = loanProductRepository;
        this.clientReadPlatformService = clientReadPlatformService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.prequalificationGroupRepositoryWrapper = prequalificationGroupRepositoryWrapper;
        this.groupRepositoryWrapper = groupRepositoryWrapper;
        this.appUserRepository = appUserRepository;
        this.agencyRepositoryWrapper = agencyRepositoryWrapper;
        this.apiJsonDeserializer = apiJsonDeserializer;
        this.preQualificationMemberRepository = preQualificationMemberRepository;
        this.prequalificationChecklistReadPlatformService = prequalificationChecklistReadPlatformService;
        this.preQualificationLogRepository = preQualificationLogRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    @Override
    public CommandProcessingResult processPrequalification(JsonCommand command) {

        final Boolean individualPrequalification = command.booleanPrimitiveValueOfParameterNamed("individual");
        if (individualPrequalification) {
            return prequalifyIndividual(command);
        }

        this.dataValidator.validateForCreate(command.json());
        final Long productId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.productIdParamName);
        final Long centerGroupId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.groupIdParamName);
        final Long agencyId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.agencyIdParamName);
        final Long previousPrequalificationId = command
                .longValueOfParameterNamed(PrequalificatoinApiConstants.previousPrequalificationParamName);

        PrequalificationGroup parentGroup = null;
        if (previousPrequalificationId != null) {
            parentGroup = this.prequalificationGroupRepositoryWrapper.findOneWithNotFoundDetection(previousPrequalificationId);
        }
        Optional<LoanProduct> productOption = this.loanProductRepository.findById(productId);
        if (productOption.isEmpty()) throw new LoanProductNotFoundException(productId);
        LoanProduct loanProduct = productOption.get();
        String groupName = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.groupNameParamName);

        Group group = null;
        if (centerGroupId != null) {
            group = this.groupRepositoryWrapper.findOneWithNotFoundDetection(centerGroupId);
            groupName = group.getName();
        }

        Agency agency = this.agencyRepositoryWrapper.findOneWithNotFoundDetection(agencyId);

        AppUser addedBy = this.context.getAuthenticatedUserIfPresent();
        Long facilitatorId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.facilitatorParamName);
        AppUser facilitator = null;
        if (facilitatorId != null) {
            facilitator = this.appUserRepository.findById(facilitatorId).orElseThrow(() -> new UserNotFoundException(facilitatorId));
        }
        PrequalificationGroup prequalificationGroup = PrequalificationGroup.fromJson(addedBy, facilitator, agency, group, loanProduct,
                parentGroup, command);

        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);
        StringBuilder prequalSB = new StringBuilder();
        prequalSB.append("PRECAL-");
        prequalSB.append(agency.getId()).append("-");
        String prequalificationNumber = StringUtils.leftPad(prequalificationGroup.getId().toString(), 4, '0');
        prequalSB.append(prequalificationNumber);
        prequalificationGroup.updatePrequalificationNumber(prequalSB.toString());
        List<PrequalificationGroupMember> members = assembNewMembers(command, prequalificationGroup, addedBy);
        prequalificationGroup.updateMembers(members);
        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(addedBy, PrequalificationStatus.PENDING.getValue(),
                prequalificationGroup.getStatus(), null, prequalificationGroup);

        this.preQualificationLogRepository.saveAndFlush(statusLog);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdAsString(prequalificationGroup.getId().toString()) //
                .withEntityId(prequalificationGroup.getId()) //
                .build();
    }

    private CommandProcessingResult prequalifyIndividual(JsonCommand command) {
        AppUser addedBy = this.context.getAuthenticatedUserIfPresent();

        apiJsonDeserializer.validateForCreate(command.json());
        final String clientName = command.stringValueOfParameterNamed("name");
        final String dpi = command.stringValueOfParameterNamed("dpi");
        final String puente = command.stringValueOfParameterNamed("puente");
        final BigDecimal amount = command.bigDecimalValueOfParameterNamed("amount");
        LocalDate dateOfBirth = command.localDateValueOfParameterNamed("dob");

        // get light indicator
        String blistSql = "select count(*) from m_client_blacklist where dpi=? and status=?";
        Long activeBlacklisted = jdbcTemplate.queryForObject(blistSql, Long.class, dpi, BlacklistStatus.ACTIVE.getValue());
        Long inactiveBlacklisted = jdbcTemplate.queryForObject(blistSql, Long.class, dpi, BlacklistStatus.INACTIVE.getValue());
        Integer status = PrequalificationMemberIndication.NONE.getValue();
        if (activeBlacklisted <= 0 && inactiveBlacklisted <= 0) {
            status = PrequalificationMemberIndication.NONE.getValue();
        }
        if (activeBlacklisted <= 0 && inactiveBlacklisted > 0) {
            status = PrequalificationMemberIndication.INACTIVE.getValue();
        }

        if (activeBlacklisted > 0) {
            status = PrequalificationMemberIndication.ACTIVE.getValue();
        }

        PrequalificationGroupMember groupMember = PrequalificationGroupMember.fromJson(null, clientName, dpi, null, dateOfBirth, amount,
                puente, addedBy, status);

        this.preQualificationMemberRepository.saveAndFlush(groupMember);
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdAsString(groupMember.getId().toString()) //
                .withEntityId(groupMember.getId()) //
                .build();
    }

    private List<PrequalificationGroupMember> assembNewMembers(JsonCommand command, PrequalificationGroup group, AppUser addedBy) {
        final List<PrequalificationGroupMember> allMembers = new ArrayList<>();

        JsonArray groupMembers = command.arrayOfParameterNamed(PrequalificatoinApiConstants.membersParamName);
        if (!ObjectUtils.isEmpty(groupMembers)) {
            for (JsonElement members : groupMembers) {

                apiJsonDeserializer.validateForCreate(members.toString());

                JsonObject member = members.getAsJsonObject();

                String name = null;
                if (member.get("name") != null) {
                    name = member.get("name").getAsString();
                }
                String dpi = null;
                if (member.get("dpi") != null) {
                    dpi = member.get("dpi").getAsString();
                }

                BigDecimal requestedAmount = null;
                if (member.get("amount") != null) {
                    requestedAmount = new BigDecimal(member.get("amount").getAsString().replace(",", ""));
                }

                String puente = null;
                if (member.get("puente") != null) {
                    puente = member.get("puente").getAsString();
                }

                Long clientId = null;
                if (member.get("clientId") != null) {
                    clientId = member.get("clientId").getAsLong();
                }

                LocalDate dateOfBirth = null;
                if (member.get("dob") != null) {

                    DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern(member.get("dateFormat").getAsString())
                            .toFormatter();
                    LocalDate date;
                    try {
                        date = LocalDate.parse(member.get("dob").getAsString(), formatter);
                        dateOfBirth = date;
                    } catch (DateTimeParseException e) {
                        LOG.error("Problem occurred in addClientFamilyMember function", e);
                    }

                }

                // get light indicator
                String blistSql = "select count(*) from m_client_blacklist where dpi=? and status=?";
                Long activeBlacklisted = jdbcTemplate.queryForObject(blistSql, Long.class, dpi, BlacklistStatus.ACTIVE.getValue());
                Long inactiveBlacklisted = jdbcTemplate.queryForObject(blistSql, Long.class, dpi, BlacklistStatus.INACTIVE.getValue());
                Integer memberStatus = PrequalificationMemberIndication.NONE.getValue();
                Integer groupStatus = PrequalificationStatus.BLACKLIST_CHECKED.getValue();
                if (activeBlacklisted <= 0 && inactiveBlacklisted <= 0) {
                    memberStatus = PrequalificationMemberIndication.NONE.getValue();
                }
                if (activeBlacklisted <= 0 && inactiveBlacklisted > 0) {
                    memberStatus = PrequalificationMemberIndication.INACTIVE.getValue();
                }

                if (activeBlacklisted > 0) {
                    memberStatus = PrequalificationMemberIndication.ACTIVE.getValue();
                    group.updateStatus(PrequalificationStatus.BLACKLIST_REJECTED);
                }

                PrequalificationGroupMember groupMember = PrequalificationGroupMember.fromJson(group, name, dpi, clientId, dateOfBirth,
                        requestedAmount, puente, addedBy, memberStatus);
                allMembers.add(groupMember);
            }
        }

        return allMembers;
    }

    @Override
    public Long addCommentsToPrequalification(Long groupId, String comment) {
        PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper.findOneWithNotFoundDetection(groupId);
        prequalificationGroup.updateComments(comment);
        Integer fromStatus = prequalificationGroup.getStatus();
        prequalificationGroup.updateStatus(PrequalificationStatus.CONSENT_ADDED);
        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);
        AppUser addedBy = this.context.getAuthenticatedUserIfPresent();
        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(addedBy, fromStatus, prequalificationGroup.getStatus(),
                comment, prequalificationGroup);
        this.preQualificationLogRepository.saveAndFlush(statusLog);
        return groupId;
    }

    @Override
    public CommandProcessingResult processUpdatePrequalification(Long groupId, JsonCommand command) {
        final Boolean individualPrequalification = command.booleanPrimitiveValueOfParameterNamed("individual");
        if (individualPrequalification) {
            return prequalifyIndividual(command);
        }

        PrequalificationGroup prequalificationGroup = prequalificationGroupRepositoryWrapper.findOneWithNotFoundDetection(groupId);

        this.dataValidator.validateUpdate(command.json());

        final Map<String, Object> changes = prequalificationGroup.update(command);

        if (changes.containsKey(PrequalificatoinApiConstants.agencyIdParamName)) {

            final Long newValue = command.longValueOfParameterNamed(PrequalificatoinApiConstants.agencyIdParamName);
            Agency newAgency = null;
            if (newValue != null) {
                newAgency = this.agencyRepositoryWrapper.findOneWithNotFoundDetection(newValue);
            }
            prequalificationGroup.updateAgency(newAgency);
        }

        if (changes.containsKey(PrequalificatoinApiConstants.centerIdParamName)) {

            final Long newValue = command.longValueOfParameterNamed(PrequalificatoinApiConstants.centerIdParamName);
            Group newCenter = null;
            if (newValue != null) {
                newCenter = this.groupRepositoryWrapper.findOneWithNotFoundDetection(newValue);
            }
            prequalificationGroup.updateCenter(newCenter.getId());
        }

        if (changes.containsKey(PrequalificatoinApiConstants.productIdParamName)) {

            final Long newValue = command.longValueOfParameterNamed(PrequalificatoinApiConstants.productIdParamName);
            LoanProduct newLoanProduct = null;
            if (newValue != null) {
                Optional<LoanProduct> productOption = this.loanProductRepository.findById(newValue);
                if (productOption.isEmpty()) throw new LoanProductNotFoundException(newValue);
                newLoanProduct = productOption.get();
            }
            prequalificationGroup.updateProduct(newLoanProduct);
        }

        if (changes.containsKey(PrequalificatoinApiConstants.facilitatorParamName)) {

            final Long newValue = command.longValueOfParameterNamed(PrequalificatoinApiConstants.facilitatorParamName);
            AppUser newFacilitator = null;
            if (newValue != null) {
                newFacilitator = this.appUserRepository.findById(newValue).orElseThrow(() -> new UserNotFoundException(newValue));
            }
            prequalificationGroup.updateFacilitator(newFacilitator);
        }

        if (changes.containsKey(PrequalificatoinApiConstants.groupNameParamName)) {

            final String newValue = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.groupNameParamName);
            if (newValue != null) {
                prequalificationGroup.updateGroupName(newValue);
            }
        }

        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);

        // TODO: FBR-220 process changes in members
        List<PrequalificationGroupMember> members = assembleMembersForUpdate(command, prequalificationGroup,
                prequalificationGroup.getAddedBy());
        prequalificationGroup.updateMembers(members);
        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdAsString(prequalificationGroup.getId().toString()) //
                .withEntityId(prequalificationGroup.getId()) //
                .build();
    }

    @Override
    public CommandProcessingResult updatePrequalificationGroupMember(Long memberId, JsonCommand command) {
        PrequalificationGroupMember member = this.preQualificationMemberRepository.findOneWithNotFoundDetection(memberId);
        this.dataValidator.validateUpdateMember(command.json());

        final Map<String, Object> changes = member.update(command);
        if (changes.containsKey(PrequalificatoinApiConstants.approvedAmountParamName)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(PrequalificatoinApiConstants.approvedAmountParamName);
            member.updateApprovedAmount(newValue);
        }

        this.preQualificationMemberRepository.saveAndFlush(member);
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdAsString(memberId.toString()) //
                .build();
    }

    private List<PrequalificationGroupMember> assembleMembersForUpdate(JsonCommand command, PrequalificationGroup prequalificationGroup,
            AppUser addedBy) {

        final List<PrequalificationGroupMember> allMembers = new ArrayList<>();

        JsonArray groupMembers = command.arrayOfParameterNamed(PrequalificatoinApiConstants.membersParamName);
        if (!ObjectUtils.isEmpty(groupMembers)) {
            prequalificationGroup.updateStatus(PrequalificationStatus.BLACKLIST_CHECKED);

            for (JsonElement memberElement : groupMembers) {

                JsonObject member = memberElement.getAsJsonObject();

                if (member.get("id") != null) {
                    Optional<PrequalificationGroupMember> pMember = prequalificationGroup.getMembers().stream()
                            .filter(m -> m.getId() == member.get("id").getAsLong()).findFirst();

                    if (pMember.isPresent()) {

                        PrequalificationGroupMember editedMember = assembleMemberForUpdate(memberElement, pMember.get(), addedBy,
                                prequalificationGroup);

                        allMembers.add(editedMember);
                    }
                } else {
                    // Handle new members
                    PrequalificationGroupMember newMember = assembleNewMember(memberElement, prequalificationGroup, addedBy);
                    allMembers.add(newMember);
                }

            }
        }

        return allMembers;
    }

    private PrequalificationGroupMember assembleMemberForUpdate(JsonElement memberElement,
            PrequalificationGroupMember prequalificationGroupMember, AppUser addedBy, PrequalificationGroup prequalificationGroup) {
        apiJsonDeserializer.validateForUpdate(memberElement.toString());

        JsonCommand command = JsonCommand.fromJsonElement(prequalificationGroupMember.getId(), memberElement, new FromJsonHelper());
        final Map<String, Object> changes = prequalificationGroupMember.update(command);

        if (changes.containsKey(PrequalificatoinApiConstants.memberNameParamName)) {
            final String newValue = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.memberNameParamName);
            if (newValue != null) {
                prequalificationGroupMember.updateName(newValue);
            }
        }

        if (changes.containsKey(PrequalificatoinApiConstants.memberDpiParamName)) {
            final String newValue = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.memberDpiParamName);
            if (newValue != null) {
                prequalificationGroupMember.updateDPI(newValue);
            }
        }

        if (changes.containsKey(PrequalificatoinApiConstants.memberDobParamName)) {
            final LocalDate newValue = command.dateValueOfParameterNamed(PrequalificatoinApiConstants.memberDobParamName);
            if (newValue != null) {
                prequalificationGroupMember.updateDOB(newValue);
            }
        }

        if (changes.containsKey(PrequalificatoinApiConstants.memberRequestedAmountParamName)) {
            final BigDecimal newValue = command
                    .bigDecimalValueOfParameterNamed(PrequalificatoinApiConstants.memberRequestedAmountParamName);
            if (newValue != null) {
                prequalificationGroupMember.updateAmountRequested(newValue);
            }
        }

        if (changes.containsKey(PrequalificatoinApiConstants.memberWorkWithPuenteParamName)) {
            final String newValue = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.memberWorkWithPuenteParamName);
            if (newValue != null) {
                prequalificationGroupMember.updateWorkWithPuente(newValue);
            }
        }
        String blistSql = "select count(*) from m_client_blacklist where dpi=? and status=?";
        Long activeBlacklisted = jdbcTemplate.queryForObject(blistSql, Long.class, prequalificationGroupMember.getDpi(),
                BlacklistStatus.ACTIVE.getValue());
        Long inactiveBlacklisted = jdbcTemplate.queryForObject(blistSql, Long.class, prequalificationGroupMember.getDpi(),
                BlacklistStatus.INACTIVE.getValue());
        PrequalificationMemberIndication status = PrequalificationMemberIndication.NONE;
        if (activeBlacklisted <= 0 && inactiveBlacklisted <= 0) {
            status = PrequalificationMemberIndication.NONE;
        }
        if (activeBlacklisted <= 0 && inactiveBlacklisted > 0) {
            status = PrequalificationMemberIndication.INACTIVE;
        }

        if (activeBlacklisted > 0) {
            status = PrequalificationMemberIndication.ACTIVE;
            prequalificationGroup.updateStatus(PrequalificationStatus.BLACKLIST_REJECTED);
        }
        prequalificationGroupMember.updateStatus(status);

        return prequalificationGroupMember;
    }

    private PrequalificationGroupMember assembleNewMember(JsonElement memberElement, PrequalificationGroup group, AppUser addedBy) {

        apiJsonDeserializer.validateForCreate(memberElement.toString());

        JsonObject member = memberElement.getAsJsonObject();

        String name = null;
        if (member.get("name") != null) {
            name = member.get("name").getAsString();
        }
        String dpi = null;
        if (member.get("dpi") != null) {
            dpi = member.get("dpi").getAsString();
        }

        BigDecimal requestedAmount = null;
        if (member.get("amount") != null) {
            requestedAmount = new BigDecimal(member.get("amount").getAsString().replace(",", ""));
        }

        String puente = null;
        if (member.get("puente") != null) {
            puente = member.get("puente").getAsString();
        }

        Long clientId = null;
        if (member.get("clientId") != null) {
            clientId = member.get("clientId").getAsLong();
        }

        LocalDate dateOfBirth = null;
        if (member.get("dob") != null) {

            DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern(member.get("dateFormat").getAsString())
                    .toFormatter();
            LocalDate date;
            try {
                date = LocalDate.parse(member.get("dob").getAsString(), formatter);
                dateOfBirth = date;
            } catch (DateTimeParseException e) {
                LOG.error("Problem occurred in addClientFamilyMember function", e);
            }

        }

        // get light indicator
        String blistSql = "select count(*) from m_client_blacklist where dpi=? and status=?";
        Long activeBlacklisted = jdbcTemplate.queryForObject(blistSql, Long.class, dpi, BlacklistStatus.ACTIVE.getValue());
        Long inactiveBlacklisted = jdbcTemplate.queryForObject(blistSql, Long.class, dpi, BlacklistStatus.INACTIVE.getValue());
        Integer status = PrequalificationMemberIndication.NONE.getValue();
        if (activeBlacklisted <= 0 && inactiveBlacklisted <= 0) {
            status = PrequalificationMemberIndication.NONE.getValue();
        }
        if (activeBlacklisted <= 0 && inactiveBlacklisted > 0) {
            status = PrequalificationMemberIndication.INACTIVE.getValue();
        }

        if (activeBlacklisted > 0) {
            status = PrequalificationMemberIndication.ACTIVE.getValue();
            group.updateStatus(PrequalificationStatus.BLACKLIST_REJECTED);
        }

        PrequalificationGroupMember groupMember = PrequalificationGroupMember.fromJson(group, name, dpi, clientId, dateOfBirth,
                requestedAmount, puente, addedBy, status);

        return groupMember;
    }

    @Override
    @CronTarget(jobName = JobName.DISABLE_EXPIRED_PREQUALIFICATIONS)
    public void disableExpiredPrequalifications() throws JobExecutionException {
        try {
            final String sql = "select m.id from m_prequalification_group m where m.status!=? and current_date > (SELECT DATE_ADD(m.created_at, INTERVAL m.prequalification_duration DAY))";
            final List<Long> expiredPrequalificationIds = this.jdbcTemplate.queryForList(sql, Long.class,
                    PrequalificationStatus.COMPLETED.getValue());
            if (expiredPrequalificationIds.size() > 0) {
                for (Long prequalificationId : expiredPrequalificationIds) {
                    final String updateSql = "update m_prequalification_group m set m.status=? where m.id=?";
                    this.jdbcTemplate.update(updateSql, PrequalificationStatus.TIME_EXPIRED.getValue(), prequalificationId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            List<Throwable> problems = new ArrayList<>();
            problems.add(e);
            throw new JobExecutionException(problems);
        }

    }

    @Override
    public CommandProcessingResult requestUpdates(Long entityId, JsonCommand command) {
        AppUser addedBy = this.context.authenticatedUser();
        final PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(entityId);
        Integer fromStatus = prequalificationGroup.getStatus();
        prequalificationGroup.updateStatus(PrequalificationStatus.PREQUALIFICATION_UPDATE_REQUESTED);
        String comments = command.stringValueOfParameterNamed("comments");
        prequalificationGroup.updateComments(comments);
        this.prequalificationGroupRepositoryWrapper.save(prequalificationGroup);

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(addedBy, fromStatus, prequalificationGroup.getStatus(),
                comments, prequalificationGroup);

        this.preQualificationLogRepository.saveAndFlush(statusLog);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId()).build();
    }

    @Override
    public CommandProcessingResult sendForAnalysis(Long entityId, JsonCommand command) {
        final PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(entityId);

        AppUser appUser = this.context.authenticatedUser();
        PrequalificationChecklistData prequalificationChecklistData = this.prequalificationChecklistReadPlatformService
                .retrieveHardPolicyValidationResults(entityId);
        GenericValidationResultSet prequalification = prequalificationChecklistData.getPrequalification();
        Integer fromStatus = prequalificationGroup.getStatus();
        List<String> exceptionsList = List.of("ORANGE", "RED", "YELLOW");
        List<List<String>> rows = prequalification.getRows();
        AtomicReference<PrequalificationStatus> status = new AtomicReference<>(PrequalificationStatus.ANALYSIS_UNIT_PENDING_APPROVAL);
        for (List<String> innerList : rows) {
            innerList.forEach(item -> {
                if (exceptionsList.contains(item)) {
                    status.set(PrequalificationStatus.ANALYSIS_UNIT_PENDING_APPROVAL_WITH_EXCEPTIONS);
                }
            });
        }

        prequalificationGroup.updateStatus(status.get());

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(appUser, fromStatus, prequalificationGroup.getStatus(),
                null, prequalificationGroup);

        this.preQualificationLogRepository.saveAndFlush(statusLog);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId()).build();
    }

    @Override
    public CommandProcessingResult sendToAgency(Long entityId, JsonCommand command) {
        final PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(entityId);

        AppUser appUser = this.context.authenticatedUser();
        PrequalificationChecklistData prequalificationChecklistData = this.prequalificationChecklistReadPlatformService
                .retrieveHardPolicyValidationResults(entityId);
        GenericValidationResultSet prequalification = prequalificationChecklistData.getPrequalification();
        Integer fromStatus = prequalificationGroup.getStatus();
        List<String> exceptionsList = List.of("ORANGE", "RED", "YELLOW");
        List<List<String>> rows = prequalification.getRows();
        AtomicReference<PrequalificationStatus> status = new AtomicReference<>(PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL);
        for (List<String> innerList : rows) {
            innerList.forEach(item -> {
                if (exceptionsList.contains(item)) {
                    status.set(PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL_WITH_EXCEPTIONS);
                }
            });
        }

        prequalificationGroup.updateStatus(status.get());

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(appUser, fromStatus, prequalificationGroup.getStatus(),
                null, prequalificationGroup);

        this.preQualificationLogRepository.saveAndFlush(statusLog);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId()).build();
    }

    @Override
    public CommandProcessingResult processAnalysisRequest(Long entityId, JsonCommand command) {
        String comments = command.stringValueOfParameterNamed("comments");
        String action = command.stringValueOfParameterNamed("action");
        AppUser addedBy = this.context.authenticatedUser();
        final PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(entityId);
        Integer fromStatus = prequalificationGroup.getStatus();
        if (action.equals("sendtoagency")) {
            return sendToAgency(entityId, command);
        }
        PrequalificationStatus prequalificationStatus = resolveStatus(action);
        if (fromStatus.equals(prequalificationStatus.getValue())) {
            throw new PrequalificationStatusNotChangedException(prequalificationStatus.toString());
        }
        prequalificationGroup.updateStatus(prequalificationStatus);
        prequalificationGroup.updateComments(comments);
        // this.prequalificationGroupRepositoryWrapper.save(prequalificationGroup);

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(addedBy, fromStatus, prequalificationGroup.getStatus(),
                comments, prequalificationGroup);

        this.preQualificationLogRepository.saveAndFlush(statusLog);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId()).build();
    }

    private PrequalificationStatus resolveStatus(String action) {
        PrequalificationStatus status = null;
        if (action.equalsIgnoreCase("sendtoagency")) {
            status = PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL;
        } else if (action.equalsIgnoreCase("sendtoexception")) {
            status = PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL_WITH_EXCEPTIONS;
        } else if (action.equalsIgnoreCase("requestupdates")) {
            status = PrequalificationStatus.PREQUALIFICATION_UPDATE_REQUESTED;
        } else if (action.equalsIgnoreCase("rejectanalysis")) {
            status = PrequalificationStatus.REJECTED;
        } else if (action.equalsIgnoreCase("approveanalysis")) {
            status = PrequalificationStatus.APPROVED;
        }
        return status;
    }
}
