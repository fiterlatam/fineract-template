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
import java.util.Collection;
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
import org.apache.fineract.infrastructure.documentmanagement.contentrepository.ContentRepository;
import org.apache.fineract.infrastructure.documentmanagement.contentrepository.ContentRepositoryFactory;
import org.apache.fineract.infrastructure.documentmanagement.data.DocumentData;
import org.apache.fineract.infrastructure.documentmanagement.domain.Document;
import org.apache.fineract.infrastructure.documentmanagement.domain.DocumentRepository;
import org.apache.fineract.infrastructure.documentmanagement.exception.DocumentNotFoundException;
import org.apache.fineract.infrastructure.documentmanagement.service.DocumentReadPlatformService;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.agency.domain.Agency;
import org.apache.fineract.organisation.agency.domain.AgencyRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.command.PrequalificationDataValidator;
import org.apache.fineract.organisation.prequalification.command.PrequalificatoinApiConstants;
import org.apache.fineract.organisation.prequalification.data.GenericValidationResultSet;
import org.apache.fineract.organisation.prequalification.data.GroupPrequalificationData;
import org.apache.fineract.organisation.prequalification.data.PrequalificationChecklistData;
import org.apache.fineract.organisation.prequalification.domain.GroupPrequalificationRelationship;
import org.apache.fineract.organisation.prequalification.domain.GroupPrequalificationRelationshipRepository;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationStatusLogRepository;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroup;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupMember;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupMemberRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationMemberIndication;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatus;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatusLog;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatusRange;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatusRangeRepository;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationSubStatus;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationType;
import org.apache.fineract.organisation.prequalification.exception.PrequalificationStatusNotChangedException;
import org.apache.fineract.organisation.prequalification.exception.PrequalificationStatusNotCompletedException;
import org.apache.fineract.organisation.prequalification.serialization.PrequalificationMemberCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistStatus;
import org.apache.fineract.portfolio.client.service.ClientChargeWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.domain.GroupRepositoryWrapper;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductOwnerType;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.jetbrains.annotations.NotNull;
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
    private final GroupPrequalificationRelationshipRepository groupPrequalificationRelationshipRepository;
    private final AppUserRepository appUserRepository;
    private final AgencyRepositoryWrapper agencyRepositoryWrapper;
    private final PrequalificationMemberCommandFromApiJsonDeserializer apiJsonDeserializer;
    private final JdbcTemplate jdbcTemplate;
    private final DocumentRepository documentRepository;
    private final ContentRepositoryFactory contentRepositoryFactory;
    private final DocumentReadPlatformService documentReadPlatformService;
    private final PrequalificationStatusRangeRepository prequalificationStatusRangeRepository;
    private final PrequalificationReadPlatformService prequalificationReadPlatformService;

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
            final ContentRepositoryFactory contentRepositoryFactory, final DocumentRepository documentRepository,
            final DocumentReadPlatformService documentReadPlatformService,
            final GroupPrequalificationRelationshipRepository groupPrequalificationRelationshipRepository,
            final PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper,
            final PrequalificationStatusRangeRepository prequalificationStatusRangeRepository,
            PrequalificationReadPlatformService prequalificationReadPlatformService) {
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
        this.contentRepositoryFactory = contentRepositoryFactory;
        this.documentRepository = documentRepository;
        this.documentReadPlatformService = documentReadPlatformService;
        this.groupPrequalificationRelationshipRepository = groupPrequalificationRelationshipRepository;
        this.prequalificationStatusRangeRepository = prequalificationStatusRangeRepository;
        this.prequalificationReadPlatformService = prequalificationReadPlatformService;
    }

    @Transactional
    @Override
    public CommandProcessingResult processPrequalification(JsonCommand command) {

        final Boolean individualPrequalification = command.booleanPrimitiveValueOfParameterNamed("individual");

        this.dataValidator.validateForCreate(command.json());
        final Long productId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.productIdParamName);
        final Long centerGroupId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.groupIdParamName);
        final Long agencyId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.agencyIdParamName);
        final Long previousPrequalificationId = command
                .longValueOfParameterNamed(PrequalificatoinApiConstants.previousPrequalificationParamName);

        PrequalificationGroup parentGroup = null;
        Group existingGroupParentGroup = null;
        if (previousPrequalificationId != null) {
            parentGroup = this.prequalificationGroupRepositoryWrapper.findOneWithNotFoundDetection(previousPrequalificationId);
            if (!parentGroup.getStatus().equals(PrequalificationStatus.COMPLETED.getValue())
                    && !parentGroup.getStatus().equals(PrequalificationStatus.REJECTED.getValue())
                    && !parentGroup.getStatus().equals(PrequalificationStatus.TIME_EXPIRED.getValue())) {
                throw new PrequalificationStatusNotCompletedException(PrequalificationStatus.fromInt(parentGroup.getStatus()).toString());
            }
            existingGroupParentGroup = this.groupRepositoryWrapper.findOneWithPrequalificationIdNotFoundDetection(parentGroup);

        }
        Optional<LoanProduct> productOption = this.loanProductRepository.findById(productId);
        if (productOption.isEmpty()) throw new LoanProductNotFoundException(productId);
        LoanProduct loanProduct = productOption.get();

        AppUser facilitator = null;
        Agency agency = null;
        Group group = null;

        if (!individualPrequalification) {
            String groupName = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.groupNameParamName);

            if (centerGroupId != null) {
                group = this.groupRepositoryWrapper.findOneWithNotFoundDetection(centerGroupId);
                groupName = group.getName();
            }

            agency = this.agencyRepositoryWrapper.findOneWithNotFoundDetection(agencyId);

            Long facilitatorId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.facilitatorParamName);
            if (facilitatorId != null) {
                facilitator = this.appUserRepository.findById(facilitatorId).orElseThrow(() -> new UserNotFoundException(facilitatorId));
            }
        }

        AppUser addedBy = this.context.getAuthenticatedUserIfPresent();

        PrequalificationGroup prequalificationGroup = PrequalificationGroup.fromJson(addedBy, facilitator, agency, group, loanProduct,
                parentGroup, command);

        PrequalificationType prequalificationType = resolvePrequalificationType(loanProduct);
        prequalificationGroup.setPrequalificationType(prequalificationType.getValue());

        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);

        String prequalificationNumberAsString = resolvePrequalificationNumber(individualPrequalification, agency, prequalificationGroup);
        prequalificationGroup.updatePrequalificationNumber(prequalificationNumberAsString);
        List<PrequalificationGroupMember> members = assembNewMembers(command, prequalificationGroup, addedBy);
        prequalificationGroup.updateMembers(members);
        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(addedBy, PrequalificationStatus.PENDING.getValue(),
                prequalificationGroup.getStatus(), null, prequalificationGroup);

        this.preQualificationLogRepository.saveAndFlush(statusLog);

        if (existingGroupParentGroup != null) {
            existingGroupParentGroup.updatePrequalification(prequalificationGroup);
            this.groupRepositoryWrapper.saveAndFlush(existingGroupParentGroup);
            GroupPrequalificationRelationship relationship = GroupPrequalificationRelationship.addRelationship(addedBy,
                    existingGroupParentGroup, prequalificationGroup);
            this.groupPrequalificationRelationshipRepository.saveAndFlush(relationship);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdAsString(prequalificationGroup.getId().toString()) //
                .withEntityId(prequalificationGroup.getId()) //
                .build();
    }

    @NotNull
    private String resolvePrequalificationNumber(Boolean individualPrequalification, Agency agency,
            PrequalificationGroup prequalificationGroup) {
        StringBuilder prequalSB = new StringBuilder();
        prequalSB.append("PRECAL-");
        String prequalificationNumber = StringUtils.leftPad(prequalificationGroup.getId().toString(), 4, '0');

        if (!individualPrequalification) {
            prequalSB.append(agency.getId()).append("-");
        }
        prequalSB.append(prequalificationNumber);
        return prequalSB.toString();
    }

    @SuppressWarnings("unused")
    private CommandProcessingResult prequalifyIndividual(JsonCommand command) {
        AppUser addedBy = this.context.getAuthenticatedUserIfPresent();

        apiJsonDeserializer.validateForCreate(command.json());

        final JsonArray members = command.arrayOfParameterNamed(PrequalificatoinApiConstants.membersParamName);
        final JsonObject jsonObject = members.get(0).getAsJsonObject();

        final String clientName = jsonObject.get("name").getAsString();
        final String dpi = jsonObject.get("dpi").getAsString();
        final String puente = jsonObject.get("puente").getAsString();
        final BigDecimal amount = jsonObject.get("amount").getAsBigDecimal();
        final Boolean groupPresident = jsonObject.get("groupPresident").getAsBoolean();

        LocalDate dateOfBirth = null;
        if (jsonObject.has("dob")) {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern(jsonObject.get("dateFormat").getAsString())
                    .toFormatter();
            LocalDate date;
            try {
                date = LocalDate.parse(jsonObject.get("dob").getAsString(), formatter);
                dateOfBirth = date;
            } catch (DateTimeParseException e) {
                LOG.error("Problem occurred in processing pre qualification for Individual", e);
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
        }

        PrequalificationGroupMember groupMember = PrequalificationGroupMember.fromJson(null, clientName, dpi, null, dateOfBirth, amount,
                puente, addedBy, status, groupPresident);

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

                Boolean groupPresident = null;
                if (member.get("groupPresident") != null) {
                    groupPresident = member.get("groupPresident").getAsBoolean();
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
                        requestedAmount, puente, addedBy, memberStatus, groupPresident);
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

    @Transactional
    @Override
    public CommandProcessingResult processUpdatePrequalification(Long groupId, JsonCommand command) {
        final Boolean individualPrequalification = command.booleanPrimitiveValueOfParameterNamed("individual");

        PrequalificationGroup prequalificationGroup = prequalificationGroupRepositoryWrapper.findOneWithNotFoundDetection(groupId);

        this.dataValidator.validateUpdate(command.json());

        final Map<String, Object> changes = prequalificationGroup.update(command);

        if (!individualPrequalification) {
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

            PrequalificationType prequalificationType = resolvePrequalificationType(newLoanProduct);
            prequalificationGroup.setPrequalificationType(prequalificationType.getValue());
        }

        Collection<DocumentData> prequalificationDocs = this.documentReadPlatformService.retrieveAllDocuments("prequalifications",
                prequalificationGroup.getId());
        if (!prequalificationDocs.isEmpty()) {
            DocumentData documentData = prequalificationDocs.iterator().next();
            deletePrequalificationDocument(documentData);
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

    public void deletePrequalificationDocument(DocumentData documentData) {
        final Document document = this.documentRepository.findById(documentData.getId()).orElseThrow(
                () -> new DocumentNotFoundException("prequalification", documentData.getParentEntityId(), documentData.getId()));
        this.documentRepository.delete(document);

        final ContentRepository contentRepository = this.contentRepositoryFactory.getRepository(document.storageType());
        contentRepository.deleteFile(document.getLocation());
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
        if (changes.containsKey(PrequalificatoinApiConstants.groupPresidentParamName)) {
            final Boolean newValue = command.booleanObjectValueOfParameterNamed(PrequalificatoinApiConstants.groupPresidentParamName);
            if (newValue != null) {
                prequalificationGroupMember.updatePresident(newValue);
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
        Boolean groupPresident = null;
        if (member.get("groupPresident") != null) {
            groupPresident = member.get("groupPresident").getAsBoolean();
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
                requestedAmount, puente, addedBy, status, groupPresident);

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

        if (prequalificationGroup.isPrequalificationTypeIndividual() && action.equals("approveanalysis")) {
            PrequalificationStatusRange statusRange = resolveIndividualStatusRange(prequalificationGroup, action);
            prequalificationStatus = PrequalificationStatus.fromInt(statusRange.getStatus());

        }
        if (prequalificationGroup.isPrequalificationTypeIndividual() && action.equals("approveCommittee")) {
            prequalificationStatus = resolveCommitteeStatus(prequalificationGroup, action);
        }

        // check if status has changed after resolving the new status
        if (fromStatus.equals(prequalificationStatus.getValue())) {
            throw new PrequalificationStatusNotChangedException(prequalificationStatus.toString());
        }

        prequalificationGroup.updateStatus(prequalificationStatus);
        prequalificationGroup.updateComments(comments);

        PrequalificationStatusLog newStatusLog = PrequalificationStatusLog.fromJson(addedBy, fromStatus, prequalificationGroup.getStatus(),
                comments, prequalificationGroup);

        this.preQualificationLogRepository.saveAndFlush(newStatusLog);

        List<PrequalificationStatusLog> currentLogs = this.preQualificationLogRepository.groupStatusLogs(fromStatus, prequalificationGroup);
        if (!currentLogs.isEmpty()) {
            PrequalificationStatusLog currentStatusLog = currentLogs.get(0);
            currentStatusLog.updateSubStatus(PrequalificationSubStatus.COMPLETED.getValue());
            this.preQualificationLogRepository.save(currentStatusLog);
        }
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId()).build();
    }

    @Override
    public CommandProcessingResult assignPrequalification(Long entityId, JsonCommand command) {
        final PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(entityId);
        AppUser currentUser = this.context.getAuthenticatedUserIfPresent();

        Integer status = prequalificationGroup.getStatus();
        List<PrequalificationStatusLog> statusLogList = this.preQualificationLogRepository.groupStatusLogs(status, prequalificationGroup);
        if (statusLogList.isEmpty())
            throw new PrequalificationStatusNotCompletedException(PrequalificationStatus.fromInt(status).toString());

        // retrieve latest log update assignee
        PrequalificationStatusLog prequalificationStatusLog = statusLogList.get(0);
        prequalificationStatusLog.updateSubStatus(PrequalificationSubStatus.IN_PROGRESS.getValue());
        prequalificationStatusLog.updateAssignedTo(currentUser);
        this.preQualificationLogRepository.saveAndFlush(prequalificationStatusLog);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId()).build();
    }

    private PrequalificationStatus resolveCommitteeStatus(PrequalificationGroup prequalificationGroup, String action) {
        // TODO ---CHECK IF THE COMMITTEE IS THE LAST COMMITTEE
        PrequalificationStatusRange initialStatusRange = resolveIndividualStatusRange(prequalificationGroup, action);

        PrequalificationStatus initialStatus = PrequalificationStatus.fromInt(initialStatusRange.getStatus());
        PrequalificationStatus currentStatus = PrequalificationStatus.fromInt(prequalificationGroup.getStatus());

        PrequalificationStatus finalStatus = currentStatus;
        if (initialStatus.getValue().equals(currentStatus.getValue())) {
            if (currentStatus.equals(PrequalificationStatus.PRE_COMMITTEE_D_PENDING_APPROVAL)) {
                finalStatus = PrequalificationStatus.PRE_COMMITTEE_C_PENDING_APPROVAL;
            } else if (currentStatus.equals(PrequalificationStatus.PRE_COMMITTEE_C_PENDING_APPROVAL)) {
                finalStatus = PrequalificationStatus.PRE_COMMITTEE_B_PENDING_APPROVAL;
            } else if (currentStatus.equals(PrequalificationStatus.PRE_COMMITTEE_B_PENDING_APPROVAL)) {
                finalStatus = PrequalificationStatus.PRE_COMMITTEE_A_PENDING_APPROVAL;
            } else if (currentStatus.equals(PrequalificationStatus.PRE_COMMITTEE_A_PENDING_APPROVAL)) {
                finalStatus = PrequalificationStatus.COMPLETED;
            }
        } else {
            finalStatus = PrequalificationStatus.COMPLETED;
        }

        return finalStatus;
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
            status = PrequalificationStatus.COMPLETED;
        }
        return status;
    }

    private PrequalificationStatusRange resolveIndividualStatusRange(PrequalificationGroup prequalificationGroup, String action) {
        PrequalificationStatusRange finalRange = null;

        if (action.equalsIgnoreCase("approveanalysis") || action.equalsIgnoreCase("approveCommittee")) {

            GroupPrequalificationData prequalificationData = prequalificationReadPlatformService.retrieveOne(prequalificationGroup.getId());
            int numberOfErrors = prequalificationData.getRedValidationCount() > 0
                    ? Math.toIntExact(prequalificationData.getRedValidationCount())
                    : 0;

            BigDecimal amount = prequalificationGroup.getTotalRequestedAmount();

            List<PrequalificationStatusRange> statusRangeList = this.prequalificationStatusRangeRepository
                    .findByPrequalificationTypeAndNumberOfErrors(prequalificationGroup.getPrequalificationType(), numberOfErrors);

            for (PrequalificationStatusRange statusRange : statusRangeList) {
                if (amount.compareTo(statusRange.getMinAmount()) >= 0
                        && (statusRange.getMaxAmount() != null && amount.compareTo(statusRange.getMaxAmount()) <= 0)) {
                    finalRange = statusRange;
                    break;
                } else if (amount.compareTo(statusRange.getMinAmount()) >= 0 && statusRange.getMaxAmount() == null) {
                    finalRange = statusRange;
                    break;
                }
            }
        }

        return finalRange;
    }

    private PrequalificationType resolvePrequalificationType(LoanProduct loanProduct) {
        if (loanProduct.getOwnerType() != null) {
            LoanProductOwnerType ownerType = LoanProductOwnerType.fromInt(loanProduct.getOwnerType());
            if (ownerType.equals(LoanProductOwnerType.INDIVIDUAL)) {
                return PrequalificationType.INDIVIDUAL;
            }
            if (ownerType.equals(LoanProductOwnerType.GROUP)) {
                return PrequalificationType.GROUP;
            }
        }
        return PrequalificationType.INVALID;
    }

}
