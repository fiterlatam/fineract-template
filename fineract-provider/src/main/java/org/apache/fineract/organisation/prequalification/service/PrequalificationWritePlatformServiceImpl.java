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
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.transaction.Transactional;
import javax.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandSource;
import org.apache.fineract.commands.domain.CommandSourceRepository;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.JsonParserHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
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
import org.apache.fineract.organisation.committee.data.CommitteeApprovalsData;
import org.apache.fineract.organisation.committee.mappers.RequiredCommitteeApprovalsMapper;
import org.apache.fineract.organisation.prequalification.command.PrequalificationDataValidator;
import org.apache.fineract.organisation.prequalification.command.PrequalificatoinApiConstants;
import org.apache.fineract.organisation.prequalification.data.GenericValidationResultSet;
import org.apache.fineract.organisation.prequalification.data.GroupPrequalificationData;
import org.apache.fineract.organisation.prequalification.data.LoanData;
import org.apache.fineract.organisation.prequalification.data.MemberPrequalificationData;
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
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatusRangeRepository;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationSubStatus;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationType;
import org.apache.fineract.organisation.prequalification.domain.Renegotiation;
import org.apache.fineract.organisation.prequalification.domain.RenegotiationRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.exception.ApprovedAmountGreaterThanRequestedException;
import org.apache.fineract.organisation.prequalification.exception.GroupMemberPreQualificationNotFound;
import org.apache.fineract.organisation.prequalification.exception.MemberNotSelectedException;
import org.apache.fineract.organisation.prequalification.exception.MemberSubmittedLoanNotFoundException;
import org.apache.fineract.organisation.prequalification.exception.PrequalificationStatusNotChangedException;
import org.apache.fineract.organisation.prequalification.exception.PrequalificationStatusNotCompletedException;
import org.apache.fineract.organisation.prequalification.exception.RenegotiationNotFoundException;
import org.apache.fineract.organisation.prequalification.exception.RequestedAmountGreaterThanOriginalException;
import org.apache.fineract.organisation.prequalification.serialization.PrequalificationMemberCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistStatus;
import org.apache.fineract.portfolio.client.service.ClientChargeWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.domain.GroupRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.GroupLoanAdditionals;
import org.apache.fineract.portfolio.loanaccount.domain.GroupLoanAdditionalsRepository;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.exception.LoanNotFoundException;
import org.apache.fineract.portfolio.loanaccount.service.LoanApplicationWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanUtilService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
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
    private final FromJsonHelper fromApiJsonHelper;
    private final GroupTypeLoanMapper groupTypeLoanMapper = new GroupTypeLoanMapper();
    private final IndividualTypeLoanMapper individualTypeLoanMapper = new IndividualTypeLoanMapper();
    private final LoanApplicationWritePlatformService loanApplicationWritePlatformService;
    private final GroupLoanAdditionalsRepository groupLoanAdditionalsRepository;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final PrequalificationChecklistWritePlatformService prequalificationChecklistWritePlatformService;
    private final RequiredCommitteeApprovalsMapper committeeApprovalsMapper = new RequiredCommitteeApprovalsMapper();
    private final CommandSourceRepository commandSourceRepository;
    private final CodeValueRepository codeValueRepository;
    private final RenegotiationRepositoryWrapper renegotiationRepository;
    private final PreQualificationStatusLogRepository preQualificationStatusLogRepository;
    private final LoanUtilService loanUtilService;

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
        String requalificationGroupName = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.groupNameParamName);

        if (!individualPrequalification) {

            if (centerGroupId != null) {
                group = this.groupRepositoryWrapper.findOneWithNotFoundDetection(centerGroupId);
                requalificationGroupName = group.getName();
            }
            if (parentGroup != null) {
                String mappingSql = "select count(*) from m_group_prequalification_relationship where group_id=?";
                Long mappingCount = jdbcTemplate.queryForObject(mappingSql, Long.class, existingGroupParentGroup.getId());
                Long mappingNumber = mappingCount + 1;
                requalificationGroupName = parentGroup.getGroupName() + "-" + mappingNumber;
            }

            agency = this.agencyRepositoryWrapper.findOneWithNotFoundDetection(agencyId);

            Long facilitatorId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.facilitatorParamName);
            if (facilitatorId != null) {
                facilitator = this.appUserRepository.findById(facilitatorId).orElseThrow(() -> new UserNotFoundException(facilitatorId));
            }
        }

        AppUser addedBy = this.context.getAuthenticatedUserIfPresent();

        PrequalificationGroup prequalificationGroup = PrequalificationGroup.fromJson(addedBy, facilitator, agency, group, loanProduct,
                parentGroup, command, requalificationGroupName);

        PrequalificationType prequalificationType = resolvePrequalificationType(loanProduct);
        prequalificationGroup.setPrequalificationType(prequalificationType.getValue());

        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);

        String prequalificationNumberAsString = resolvePrequalificationNumber(individualPrequalification, agency, prequalificationGroup);
        prequalificationGroup.updatePrequalificationNumber(prequalificationNumberAsString);
        List<PrequalificationGroupMember> members = assembNewMembers(command, prequalificationGroup, addedBy);
        prequalificationGroup.updateMembers(members);
        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(addedBy, PrequalificationStatus.PENDING.getValue(),
                prequalificationGroup.getStatus(), null, prequalificationGroup, null, null);

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

                final LocalDate dateOfBirth = this.fromApiJsonHelper.extractLocalDateNamed("dob", member);

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
                comment, prequalificationGroup, null, null);
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

    @Transactional
    @Override
    public CommandProcessingResult updatePrequalificationGroupMember(Long memberId, JsonCommand command) {
        PrequalificationGroupMember member = this.preQualificationMemberRepository.findOneWithNotFoundDetection(memberId);
        this.dataValidator.validateUpdateMember(command.json());

        final Map<String, Object> changes = member.update(command);
        if (changes.containsKey(PrequalificatoinApiConstants.approvedAmountParamName)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(PrequalificatoinApiConstants.approvedAmountParamName);
            if (newValue.compareTo(member.getOriginalAmount()) > 0) {
                throw new ApprovedAmountGreaterThanRequestedException(member.getDpi(), member.getName(), newValue,
                        member.getRequestedAmount());
            }
            member.updateApprovedAmount(newValue);
        }
        if (changes.containsKey(PrequalificatoinApiConstants.memberCommentsParamName)) {
            final String newValue = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.memberCommentsParamName);
            member.updateComments(newValue);
        }
        PrequalificationGroup prequalificationGroup = member.getPrequalificationGroup();
        if (changes.containsKey(PrequalificatoinApiConstants.memberAgencyBureauStatusParamName)) {
            final String newValue = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.memberAgencyBureauStatusParamName);
            member.updateAgencyBureauStatus(newValue);

            Integer status = prequalificationGroup.getStatus();
            List<PrequalificationStatusLog> statusLogList = this.preQualificationLogRepository.groupStatusLogs(status,
                    prequalificationGroup);
            if (statusLogList.isEmpty())
                throw new PrequalificationStatusNotCompletedException(PrequalificationStatus.fromInt(status).toString());

            // retrieve latest log update assignee
            PrequalificationStatusLog statusLog = statusLogList.get(0);
            statusLog.updateSubStatus(PrequalificationSubStatus.BURO_EVIDENCE.getValue());

            List<LoanData> submittedLoans;
            Long prequalificationId = prequalificationGroup.getId();

            if (prequalificationGroup.isPrequalificationTypeGroup()) {
                submittedLoans = jdbcTemplate.query(this.groupTypeLoanMapper.schema(), this.groupTypeLoanMapper, prequalificationId,
                        member.getDpi(), prequalificationId);
            } else if (prequalificationGroup.isPrequalificationTypePAE()) {
                submittedLoans = jdbcTemplate.query(this.individualTypeLoanMapper.schema(), this.individualTypeLoanMapper,
                        prequalificationId, PrequalificationType.PAE.getValue(), member.getDpi(), prequalificationId);
            } else {
                submittedLoans = jdbcTemplate.query(this.individualTypeLoanMapper.schema(), this.individualTypeLoanMapper,
                        prequalificationId, PrequalificationType.PAE.getValue(), member.getDpi(), prequalificationId);
            }
            if (submittedLoans.isEmpty()) {
                throw new MemberSubmittedLoanNotFoundException(member.getDpi());
            }
            LoanData loanData = submittedLoans.get(0);
            Long loanId = loanData.getLoanId();
            Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

            GroupLoanAdditionals groupLoanAdditionalsByLoan = this.groupLoanAdditionalsRepository.getGroupLoanAdditionalsByLoan(loan);
            if (groupLoanAdditionalsByLoan != null) {
                Collection<CodeValueData> clientTypeOptions = this.codeValueReadPlatformService
                        .retrieveCodeValuesByCode("clientTypeOptions");
                for (CodeValueData clientType : clientTypeOptions) {
                    if (clientType.getName().equals(newValue)) {
                        groupLoanAdditionalsByLoan.updateClientType(clientType.getId());
                        this.groupLoanAdditionalsRepository.saveAndFlush(groupLoanAdditionalsByLoan);
                        break;
                    }
                }
            }

            this.preQualificationLogRepository.saveAndFlush(statusLog);
            this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);
        }
        if (changes.containsKey(PrequalificatoinApiConstants.memberRequestedAmountParamName)) {
            final BigDecimal newValue = command
                    .bigDecimalValueOfParameterNamed(PrequalificatoinApiConstants.memberRequestedAmountParamName);
            if (newValue.compareTo(member.getOriginalAmount()) > 0) {
                throw new RequestedAmountGreaterThanOriginalException(member.getDpi(), member.getName(), newValue,
                        member.getOriginalAmount());
            }
            member.updateAmountRequested(newValue);
            member.updateApprovedAmount(newValue);
        }

        this.preQualificationMemberRepository.saveAndFlush(member);
        updateLoanAssociated(command);
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
                prequalificationGroupMember.updateApprovedAmount(newValue);
                prequalificationGroupMember.updateOriginalAmount(newValue);
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
        if (member.get("requestedAmount") != null) {
            requestedAmount = new BigDecimal(member.get("requestedAmount").getAsString().replace(",", ""));
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
        final LocalDate dateOfBirth = this.fromApiJsonHelper.extractLocalDateNamed("dob", member);
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
                comments, prequalificationGroup, null, null);

        this.preQualificationLogRepository.saveAndFlush(statusLog);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId()).build();
    }

    @Override
    public CommandProcessingResult sendForAnalysis(Long entityId, JsonCommand command, Boolean withExceptions) {
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

        if (withExceptions) {
            status.set(PrequalificationStatus.ANALYSIS_UNIT_PENDING_APPROVAL_WITH_EXCEPTIONS);
        }

        prequalificationGroup.updateStatus(status.get());

        String comments = command.stringValueOfParameterNamed("comments");
        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(appUser, fromStatus, prequalificationGroup.getStatus(),
                comments, prequalificationGroup, null, null);

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
        GenericValidationResultSet members = prequalificationChecklistData.getMembers();
        Integer fromStatus = prequalificationGroup.getStatus();
        List<String> exceptionsList = List.of("ORANGE", "RED", "YELLOW");
        List<List<String>> rows = prequalification.getRows();
        List<List<String>> membersRows = members.getRows();
        rows.addAll(membersRows);
        AtomicReference<PrequalificationStatus> status = new AtomicReference<>(PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL);
        for (List<String> innerList : rows) {
            innerList.forEach(item -> {
                if (exceptionsList.contains(item)) {
                    status.set(PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL_WITH_EXCEPTIONS);
                }
            });
        }

        prequalificationGroup.updateStatus(status.get());

        String comments = command.stringValueOfParameterNamed("comments");
        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(appUser, fromStatus, prequalificationGroup.getStatus(),
                comments, prequalificationGroup, null, null);

        this.preQualificationLogRepository.saveAndFlush(statusLog);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId()).build();
    }

    @Override
    public CommandProcessingResult sendToFirstPhaseApproveCommitteeD(Long entityId, JsonCommand command, boolean withExceptions,
            boolean nextPhase) {
        final PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(entityId);

        GroupPrequalificationData prequalificationData = prequalificationReadPlatformService.retrieveOne(prequalificationGroup.getId());

        AppUser appUser = this.context.authenticatedUser();
        Integer fromStatus = prequalificationGroup.getStatus();
        String action = command.stringValueOfParameterNamed("action");
        /*
         * BigDecimal amount = prequalificationData.getTotalRequestedAmount(); if (amount != null) { if
         * (amount.compareTo(new BigDecimal("20000")) < 0) { // Monto < 20.000 → Comité D
         * prequalificationGroup.updateStatus( PrequalificationStatus.PRE_COMMITTEE_D_PENDING_APPROVAL ); } else if
         * (amount.compareTo(new BigDecimal("80000")) < 0) { // 20.000 ≤ Monto < 80.000 → Comité C
         * prequalificationGroup.updateStatus( PrequalificationStatus.PRE_COMMITTEE_C_PENDING_APPROVAL ); } else if
         * (amount.compareTo(new BigDecimal("250000")) <= 0) { // 80.000 ≤ Monto ≤ 250.000 → Comité B
         * prequalificationGroup.updateStatus( PrequalificationStatus.PRE_COMMITTEE_B_PENDING_APPROVAL ); } else { //
         * Monto > 250.000 → Comité A prequalificationGroup.updateStatus(
         * PrequalificationStatus.PRE_COMMITTEE_A_PENDING_APPROVAL ); } }
         */
        if ((action.equals("approvepreviouscommitee") || action.equals("approveRenegotiation")) && !nextPhase) {

            PrequalificationStatus lastStatus = PrequalificationStatus
                    .fromInt(prequalificationData.getLastPrequalificationStatus().getId().intValue());

            prequalificationGroup.updateStatus(lastStatus);

        } else {
            if (!nextPhase) {
                prequalificationGroup.updateStatus(withExceptions ? PrequalificationStatus.PRE_COMMITTEE_D_PENDING_APPROVAL_WITH_EXCEPTIONS
                        : PrequalificationStatus.PRE_COMMITTEE_D_PENDING_APPROVAL);
            } else {
                if (PrequalificationStatus.fromInt(prequalificationGroup.getStatus())
                        .equals(PrequalificationStatus.RENEGOTIATION_AGENCY_LEAD)) {
                    PrequalificationStatus lastStatus = PrequalificationStatus
                            .fromInt(prequalificationData.getLastPrequalificationStatus().getId().intValue());

                    prequalificationGroup.updateStatus(lastStatus);
                }
                PrequalificationStatus currentStatus = PrequalificationStatus.fromInt(prequalificationGroup.getStatus());

                if (currentStatus == PrequalificationStatus.PRE_COMMITTEE_D_PENDING_APPROVAL
                        || currentStatus == PrequalificationStatus.PRE_COMMITTEE_D_PENDING_APPROVAL_WITH_EXCEPTIONS) {

                    prequalificationGroup
                            .updateStatus(withExceptions ? PrequalificationStatus.PRE_COMMITTEE_C_PENDING_APPROVAL_WITH_EXCEPTIONS
                                    : PrequalificationStatus.PRE_COMMITTEE_C_PENDING_APPROVAL);

                } else if (currentStatus == PrequalificationStatus.PRE_COMMITTEE_C_PENDING_APPROVAL
                        || currentStatus == PrequalificationStatus.PRE_COMMITTEE_C_PENDING_APPROVAL_WITH_EXCEPTIONS) {

                    prequalificationGroup
                            .updateStatus(withExceptions ? PrequalificationStatus.PRE_COMMITTEE_B_PENDING_APPROVAL_WITH_EXCEPTIONS
                                    : PrequalificationStatus.PRE_COMMITTEE_B_PENDING_APPROVAL);

                } else if (currentStatus == PrequalificationStatus.PRE_COMMITTEE_B_PENDING_APPROVAL
                        || currentStatus == PrequalificationStatus.PRE_COMMITTEE_B_PENDING_APPROVAL_WITH_EXCEPTIONS) {

                    prequalificationGroup
                            .updateStatus(withExceptions ? PrequalificationStatus.PRE_COMMITTEE_A_PENDING_APPROVAL_WITH_EXCEPTIONS
                                    : PrequalificationStatus.PRE_COMMITTEE_A_PENDING_APPROVAL);

                }
            }
        }

        String comments = command.stringValueOfParameterNamed("comments");
        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(appUser, fromStatus, prequalificationGroup.getStatus(),
                comments, prequalificationGroup, null, withExceptions);
        this.preQualificationLogRepository.saveAndFlush(statusLog);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId()).build();
    }

    @Override
    public CommandProcessingResult restartFlow(Long entityId, JsonCommand command) {
        final PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(entityId);

        PrequalificationStatusLog statusLog = preQualificationStatusLogRepository
                .findTopByPrequalificationGroupIdAndFromStatusOrderByIdDesc(prequalificationGroup.getId(),
                        PrequalificationStatus.BLACKLIST_CHECKED.getValue())
                .copy();

        statusLog.setFromStatus(prequalificationGroup.getStatus());

        this.preQualificationLogRepository.saveAndFlush(statusLog);
        prequalificationGroup.updateStatus(PrequalificationStatus.fromInt(statusLog.getToStatus()));
        this.prequalificationGroupRepositoryWrapper.save(prequalificationGroup);

        Loan loan = loanRepositoryWrapper.retrieveByPrequalificationId(prequalificationGroup.getId());
        if (loan != null && loan.isApproved()) {

            log.info("Rechazando aprobación para pre-calificación con id: {}", prequalificationGroup.getId());

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("note", loan.getId());
            final String jsonCommand = jsonObject.toString();
            final JsonCommand undoCommand = JsonCommand.from(jsonCommand, jsonObject, this.fromApiJsonHelper, null, loan.getId(), null,
                    null, loan.getClientId(), loan.getId(), null, null, null, null, null, null);
            this.loanApplicationWritePlatformService.undoApplicationApproval(loan.getId(), undoCommand);
        } else {
            log.info("La pre-calificación no tiene crédito relacionado.");
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId())
                .withResourceIdAsString(loan != null ? loan.getId().toString() : "").withEntityId(prequalificationGroup.getId()).build();
    }

    private int getCommitteeLevel(PrequalificationStatus status) {
        switch (status) {
            case PRE_COMMITTEE_D_PENDING_APPROVAL:
                return 1;
            case PRE_COMMITTEE_C_PENDING_APPROVAL:
                return 2;
            case PRE_COMMITTEE_B_PENDING_APPROVAL:
                return 3;
            case PRE_COMMITTEE_A_PENDING_APPROVAL:
                return 4;
            default:
                return 0;
        }
    }

    @Override
    @Transactional
    public CommandProcessingResult processAnalysisRequest(Long entityId, JsonCommand command) {
        String comments = command.stringValueOfParameterNamed("comments");
        String action = command.stringValueOfParameterNamed("action");
        final Long reasonCode = command.longValueOfParameterNamed("reasonId");
        AppUser addedBy = this.context.authenticatedUser();
        final PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(entityId);
        Integer fromStatus = prequalificationGroup.getStatus();
        if (action.equals("sendtoagency")) {
            return sendToAgency(entityId, command);
        }
        if (action.equals("revalidateHardPolicy")) {
            return revalidateHardPolicy(entityId, command);
        }
        // first phase
        if (action.equals("approvecommiteeWhitExc") || (action.equals("sendtoexception")
                && (PrequalificationStatus.fromInt(fromStatus).equals(PrequalificationStatus.ANALYSIS_UNIT_PENDING_APPROVAL_WITH_EXCEPTIONS)
                        || PrequalificationStatus.fromInt(fromStatus).equals(PrequalificationStatus.ANALYSIS_UNIT_PENDING_APPROVAL))
                && PrequalificationType.fromInt(prequalificationGroup.getPrequalificationType()).equals(PrequalificationType.PAE))) {
            return sendToFirstPhaseApproveCommitteeD(entityId, command, true, false);
        }
        // first phase
        if (action.equals("approvecommitee") || action.equals("approvepreviouscommitee")) {
            return sendToFirstPhaseApproveCommitteeD(entityId, command, false, false);
        }
        if (action.equals("recommendCommittee")) {
            return sendToFirstPhaseApproveCommitteeD(entityId, command, false, true);
        }
        // send to renegotiation
        if (action.equals("sendToRenegotiation")) {
            return sendToRenegotiation(prequalificationGroup, addedBy, command);
        } // send to renegotiation
        if (action.equals("approveRenegotiation")) {
            approveRenegotiation(prequalificationGroup, addedBy, command);
            GroupPrequalificationData prequalificationData = prequalificationReadPlatformService.retrieveOne(prequalificationGroup.getId());

            PrequalificationStatus lastStatus = PrequalificationStatus
                    .fromInt(prequalificationData.getLastPrequalificationStatus().getId().intValue());
            final Long renegotiationId = command.longValueOfParameterNamed("renegotiationId");
            Renegotiation renegotiation = renegotiationRepository.getRenegotiationById(renegotiationId);
            BigDecimal amount = renegotiation.getProposedAmount();
            PrequalificationStatus targetCommittee = null;

            if (amount.compareTo(new BigDecimal("20000")) < 0) {
                targetCommittee = PrequalificationStatus.PRE_COMMITTEE_D_PENDING_APPROVAL;
            } else if (amount.compareTo(new BigDecimal("80000")) < 0) {
                targetCommittee = PrequalificationStatus.PRE_COMMITTEE_C_PENDING_APPROVAL;
            } else if (amount.compareTo(new BigDecimal("250000")) <= 0) {
                targetCommittee = PrequalificationStatus.PRE_COMMITTEE_B_PENDING_APPROVAL;
            } else {
                targetCommittee = PrequalificationStatus.PRE_COMMITTEE_A_PENDING_APPROVAL;
            }

            int lastLevel = getCommitteeLevel(lastStatus);
            int targetLevel = getCommitteeLevel(targetCommittee);

            if (lastLevel == targetLevel /*|| lastLevel == targetLevel - 1*/) {
                action = "approveCommittee";
            } else {
                return sendToFirstPhaseApproveCommitteeD(entityId, command, false, true);
            }
        }
        if (action.equals("rejectRenegotiation")) {
            return rejectRenegotiation(prequalificationGroup, addedBy, command);
        }
        if (action.equals("sendtoexception")
                && (PrequalificationStatus.fromInt(fromStatus).equals(PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL)
                        || PrequalificationStatus.fromInt(fromStatus)
                                .equals(PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL_WITH_EXCEPTIONS))
                && PrequalificationType.fromInt(prequalificationGroup.getPrequalificationType()).equals(PrequalificationType.PAE)) {
            return sendForAnalysis(entityId, command, true);
        }

        if (action.equals("restartflow")) {
            return restartFlow(entityId, command);
        }
        PrequalificationStatus prequalificationStatus = resolveStatus(action);
        final List<MemberPrequalificationData> memberPrequalificationDataList = new ArrayList<>();
        if (command.parameterExists("members")) {
            final JsonElement jsonElement = command.jsonElement("members");
            if (jsonElement != null) {
                final JsonArray members = jsonElement.getAsJsonArray();
                if (!members.isEmpty()) {
                    for (int i = 0; i < members.size(); i++) {
                        JsonElement memberJson = members.get(i);
                        final Long memberId = this.fromApiJsonHelper.extractLongNamed("id", memberJson);
                        final Boolean isSelected = this.fromApiJsonHelper.extractBooleanNamed("isSelected", memberJson);
                        final MemberPrequalificationData memberPrequalificationData = MemberPrequalificationData.instance(memberId,
                                isSelected);
                        memberPrequalificationDataList.add(memberPrequalificationData);
                    }
                }
            }
        }

        final Long productId = prequalificationGroup.getLoanProduct().getId();
        final LoanProduct loanProduct = this.loanProductRepository.findById(productId)
                .orElseThrow(() -> new LoanProductNotFoundException(productId));
        final Boolean requireCommitteeApproval = ObjectUtils.defaultIfNull(loanProduct.getRequireCommitteeApproval(), Boolean.FALSE);
        if (prequalificationGroup.isPrequalificationTypeIndividual() && action.equals("approveanalysis") && requireCommitteeApproval) {
            Integer statusRange = resolveIndividualStatusRange(prequalificationGroup, action);
            prequalificationStatus = PrequalificationStatus.fromInt(statusRange);

        }
        String reportToPrint = null;
        Long loanId = null;
        if ((prequalificationGroup.isPrequalificationTypeIndividual() || prequalificationGroup.isPrequalificationTypePAE())
                && action.equals("approveCommittee")) {
            Integer statusRange = resolveIndividualStatusRange(prequalificationGroup, action);
            prequalificationStatus = PrequalificationStatus.fromInt(statusRange);
            if (prequalificationStatus.equals(PrequalificationStatus.COMPLETED)) {
                reportToPrint = "Commitee Approval Report";
                List<Loan> allLoans = this.loanRepositoryWrapper.retrieveAllByPrequalificationId(entityId);
                if (allLoans.isEmpty()) {
                    throw new LoanNotFoundException(loanId);
                }
                Loan loan = allLoans.get(0);
                loanId = loan != null ? loan.getId() : null;
            }
        }

        // check if status has changed after resolving the new status
        if (fromStatus.equals(prequalificationStatus.getValue())) {
            throw new PrequalificationStatusNotChangedException(prequalificationStatus.toString());
        }

        prequalificationGroup.updateStatus(prequalificationStatus);
        prequalificationGroup.updateComments(comments);

        CodeValue code = null;
        if (reasonCode != null) {
            Optional<CodeValue> codeValue;
            codeValue = this.codeValueRepository.findById(reasonCode);
            if (codeValue.isPresent()) {
                code = codeValue.get();
            }
        }

        PrequalificationStatusLog newStatusLog = PrequalificationStatusLog.fromJson(addedBy, fromStatus, prequalificationGroup.getStatus(),
                comments, prequalificationGroup, code, null);
        this.approveOrRejectLoanApplications(prequalificationGroup, prequalificationStatus, memberPrequalificationDataList);
        this.preQualificationLogRepository.saveAndFlush(newStatusLog);

        List<PrequalificationStatusLog> currentLogs = this.preQualificationLogRepository.groupStatusLogs(fromStatus, prequalificationGroup);
        if (!currentLogs.isEmpty()) {
            PrequalificationStatusLog currentStatusLog = currentLogs.get(0);
            currentStatusLog.updateSubStatus(PrequalificationSubStatus.COMPLETED.getValue());
            this.preQualificationLogRepository.save(currentStatusLog);
        }
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId())
                .withReportToPrint(reportToPrint).withLoanId(loanId).build();
    }

    private CommandProcessingResult approveRenegotiation(PrequalificationGroup prequalificationGroup, AppUser addedBy,
            JsonCommand command) {
        Long renegotiationId = command.longValueOfParameterNamed("renegotiationId");
        Renegotiation renegotiationById = this.renegotiationRepository.getRenegotiationById(renegotiationId);
        if (renegotiationById == null || !renegotiationById.getPrequalificationGroup().getId().equals(prequalificationGroup.getId())) {
            throw new RenegotiationNotFoundException(renegotiationId);
        }
        // approve renegotiation
        renegotiationById.setStatus("APPROVED");
        renegotiationById.setApprovedDate(DateUtils.getLocalDateTimeOfSystem());
        renegotiationById.setApprovedBy(this.context.authenticatedUser());
        this.renegotiationRepository.saveRenegotiation(renegotiationById);

        // AFTER APPROVING ONE, CANCEL ALL PENDING RENEGOTIATIONS
        List<Renegotiation> renegotiationByPrequalificationId = this.renegotiationRepository
                .getRenegotiationByPrequalificationId(prequalificationGroup.getId());
        for (Renegotiation renegotiation : renegotiationByPrequalificationId) {
            if (renegotiation.getStatus().equals("PENDING")) {
                renegotiation.setStatus("CANCELED");
                this.renegotiationRepository.saveRenegotiation(renegotiation);
            }
        }

        Loan loan = this.loanRepositoryWrapper.retrieveByPrequalificationId(prequalificationGroup.getId());

        final String localeAsString = "en";
        final String dateFormat = "dd MMMM yyyy";
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("locale", localeAsString);
        jsonObject.addProperty("dateFormat", dateFormat);
        Locale locale = JsonParserHelper.localeFromString(localeAsString);
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat).withLocale(locale);
        jsonObject.addProperty("productId", loan.getLoanProduct().getId());
        jsonObject.addProperty("expectedDisbursementDate", loan.getExpectedDisbursedOnLocalDate().format(dateTimeFormatter));
        jsonObject.addProperty("interestCalculationPeriodType",
                loan.getLoanProductRelatedDetail().getInterestCalculationPeriodMethod().getValue());
        jsonObject.addProperty("interestType", loan.getLoanProductRelatedDetail().getInterestMethod().getValue());
        jsonObject.addProperty("loanType", AccountType.fromInt(loan.getLoanType()).getName());
        jsonObject.addProperty("interestRatePerPeriod", renegotiationById.getProposedInterest());
        jsonObject.addProperty("principal", renegotiationById.getProposedAmount());
        jsonObject.addProperty("isEqualAmortization", loan.getLoanProductRelatedDetail().isEqualAmortization());
        jsonObject.addProperty("amortizationType", loan.getLoanProductRelatedDetail().getAmortizationMethod().getValue());

        // loan term and repayment structure
        final Integer loanTermFrequency = renegotiationById.getProposedTerm();
        final Integer loanTermFrequencyType = PeriodFrequencyType.MONTHS.getValue();
        final Integer repaymentEvery = loan.getLoanProductRelatedDetail().getRepayEvery();
        final Integer repaymentFrequencyType = loan.getLoanProductRelatedDetail().getRepaymentPeriodFrequencyType().getValue();

        jsonObject.addProperty("loanTermFrequency", loanTermFrequency);
        jsonObject.addProperty("loanTermFrequencyType", loanTermFrequencyType);
        jsonObject.addProperty("repaymentEvery", repaymentEvery);
        jsonObject.addProperty("repaymentFrequencyType", repaymentFrequencyType);

        // compute number of repayments from term and frequency
        final Integer computedNumberOfRepayments = computeNumberOfRepayments(loanTermFrequency, loanTermFrequencyType, repaymentEvery,
                repaymentFrequencyType, loan.getLoanProductRelatedDetail().getNumberOfRepayments());
        jsonObject.addProperty("numberOfRepayments", computedNumberOfRepayments);

        final String jsonCommand = jsonObject.toString();
        final JsonCommand loancommand = JsonCommand.from(jsonCommand, jsonObject, this.fromApiJsonHelper, null, loan.getId(), null, null,
                loan.getClientId(), loan.getId(), null, null, null, null, null, null);
        loancommand.setJsonCommand(jsonObject.toString());
        this.loanApplicationWritePlatformService.modifyApplication(loan.getId(), loancommand);

        PrequalificationGroupMember prequalificationGroupMember = prequalificationGroup.getMembers().get(0);
        prequalificationGroupMember.updateApprovedAmount(renegotiationById.getProposedAmount());
        this.preQualificationMemberRepository.saveAndFlush(prequalificationGroupMember);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId()).build();
    }

    /**
     * Computes the number of repayments using the relationship between loan term and repayment structure. Assumes
     * loanTermFrequency and repaymentEvery are expressed in the same PeriodFrequencyType. Falls back to the
     * originalNumberOfRepayments if frequency types differ or inputs are invalid.
     */
    private Integer computeNumberOfRepayments(final Integer loanTermFrequency, final Integer loanTermFrequencyType,
            final Integer repaymentEvery, final Integer repaymentFrequencyType, final Integer originalNumberOfRepayments) {
        if (loanTermFrequency == null || repaymentEvery == null || repaymentEvery == 0) {
            return originalNumberOfRepayments;
        }
        // Ensure frequency types are consistent with Fineract validation rules
        if (loanTermFrequencyType != null && repaymentFrequencyType != null && !loanTermFrequencyType.equals(repaymentFrequencyType)) {
            // Fall back to existing behaviour by not altering the number of repayments when types mismatch
            return originalNumberOfRepayments;
        }
        int computed = loanTermFrequency / repaymentEvery;
        if (computed <= 0) {
            computed = 1;
        }
        return computed;
    }

    private CommandProcessingResult rejectRenegotiation(PrequalificationGroup prequalificationGroup, AppUser addedBy, JsonCommand command) {
        Long renegotiationId = command.longValueOfParameterNamed("renegotiationId");
        Renegotiation renegotiationById = this.renegotiationRepository.getRenegotiationById(renegotiationId);
        if (renegotiationById == null || !renegotiationById.getPrequalificationGroup().getId().equals(prequalificationGroup.getId())) {
            throw new RenegotiationNotFoundException(renegotiationId);
        }
        // approve renegotiation
        renegotiationById.setStatus("REJECTED");
        renegotiationById.setApprovedDate(DateUtils.getLocalDateTimeOfSystem());
        renegotiationById.setApprovedBy(this.context.authenticatedUser());
        this.renegotiationRepository.saveRenegotiation(renegotiationById);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId()).build();
    }

    private CommandProcessingResult sendToRenegotiation(PrequalificationGroup prequalificationGroup, AppUser addedBy, JsonCommand command) {
        Integer fromStatus = prequalificationGroup.getStatus();

        prequalificationGroup.updateStatus(PrequalificationStatus.RENEGOTIATION_AGENCY_LEAD);

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(addedBy, fromStatus, prequalificationGroup.getStatus(),
                "Renegotiation", prequalificationGroup, null, false);
        this.preQualificationLogRepository.saveAndFlush(statusLog);
        JsonElement renegotiationData = command.jsonElement("renegotiationData");
        JsonObject renegotiationObject = renegotiationData.getAsJsonObject();
        if (renegotiationObject != null) {
            final BigDecimal newProposedAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("proposedAmount",
                    renegotiationObject);
            final Integer newProposedTerm = this.fromApiJsonHelper.extractIntegerSansLocaleNamed("proposedTerm", renegotiationObject);
            final String comments = this.fromApiJsonHelper.extractStringNamed("comments", renegotiationObject);
            final BigDecimal newProposedInterestRate = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("proposedInterestRate",
                    renegotiationObject);

            Renegotiation renegotiation = Renegotiation.create(prequalificationGroup, newProposedInterestRate, newProposedAmount,
                    newProposedTerm, comments, DateUtils.getLocalDateTimeOfSystem(), addedBy);
            this.renegotiationRepository.saveRenegotiation(renegotiation);
            this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);
        }
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(prequalificationGroup.getId()).build();
    }

    private CommandProcessingResult revalidateHardPolicy(Long entityId, JsonCommand command) {
        return this.prequalificationChecklistWritePlatformService.validatePrequalificationHardPolicies(entityId, command);
    }

    private void approveOrRejectLoanApplications(final PrequalificationGroup prequalificationGroup,
            final PrequalificationStatus prequalificationStatus, final List<MemberPrequalificationData> prequalificationMembers) {
        final Long prequalificationId = prequalificationGroup.getId();
        final List<PrequalificationGroupMember> groupMembers = prequalificationGroup.getMembers();
        final List<MemberPrequalificationData> approvedPrequalificationMembers = prequalificationMembers.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsSelected())).toList();
        if (approvedPrequalificationMembers.isEmpty() && PrequalificationStatus.COMPLETED.equals(prequalificationStatus)) {
            throw new MemberNotSelectedException(prequalificationGroup.getId());
        }
        for (final MemberPrequalificationData memberPrequalificationData : prequalificationMembers) {
            final Optional<PrequalificationGroupMember> memberOptional = groupMembers.stream()
                    .filter(m -> memberPrequalificationData.getId().equals(m.getId())).findFirst();
            if (memberOptional.isEmpty()) {
                throw new GroupMemberPreQualificationNotFound(memberPrequalificationData.getId());
            }
            final PrequalificationGroupMember prequalificationGroupMember = memberOptional.get();
            final boolean isApproved = PrequalificationStatus.COMPLETED.equals(prequalificationStatus)
                    && (memberPrequalificationData.getIsSelected() || prequalificationGroup.isPrequalificationTypeIndividual());
            final boolean isRejected = PrequalificationStatus.REJECTED.equals(prequalificationStatus)
                    || (memberPrequalificationData.getIsSelected() != null && !memberPrequalificationData.getIsSelected()
                            && PrequalificationStatus.COMPLETED.equals(prequalificationStatus)
                            && prequalificationGroup.isPrequalificationTypeGroup());
            final BigDecimal approvedLoanAmount = prequalificationGroupMember.getApprovedAmount();
            final String dpi = prequalificationGroupMember.getDpi();
            List<LoanData> submittedLoans;
            if (prequalificationGroup.isPrequalificationTypeGroup()) {
                submittedLoans = jdbcTemplate.query(this.groupTypeLoanMapper.schema(), this.groupTypeLoanMapper,
                        new Object[] { prequalificationId, dpi, prequalificationId });
            } else if (prequalificationGroup.isPrequalificationTypePAE()) {
                submittedLoans = jdbcTemplate.query(this.individualTypeLoanMapper.schema(), this.individualTypeLoanMapper,
                        new Object[] { prequalificationId, PrequalificationType.PAE.getValue(), dpi, prequalificationId });
            } else {
                submittedLoans = jdbcTemplate.query(this.individualTypeLoanMapper.schema(), this.individualTypeLoanMapper,
                        new Object[] { prequalificationId, PrequalificationType.INDIVIDUAL.getValue(), dpi, prequalificationId });
            }
            if (submittedLoans.isEmpty()) {
                throw new MemberSubmittedLoanNotFoundException(dpi);
            }
            for (final LoanData submittedLoan : submittedLoans) {
                final Long groupId = submittedLoan.getGroupId();
                final String localeAsString = "en";
                final String dateFormat = "dd MMMM yyyy";
                final Long loanId = submittedLoan.getLoanId();
                final Long clientId = submittedLoan.getClientId();
                final JsonObject jsonObject = new JsonObject();
                final LocalDate localDate = DateUtils.getBusinessLocalDate();
                Locale locale = JsonParserHelper.localeFromString(localeAsString);
                final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat).withLocale(locale);
                final String localDateString = localDate.format(dateTimeFormatter);
                jsonObject.addProperty("locale", localeAsString);
                jsonObject.addProperty("dateFormat", dateFormat);
                if (isApproved) {
                    jsonObject.addProperty("approvedLoanAmount", approvedLoanAmount);
                    jsonObject.addProperty("approvedOnDate", localDateString);
                    jsonObject.add("disbursementData", new JsonArray());
                    jsonObject.addProperty("expectedDisbursementDate", localDateString);
                    final String note = "Precalificación Aprobada " + prequalificationGroup.getPrequalificationNumber();
                    jsonObject.addProperty("note", note);
                    final String jsonCommand = jsonObject.toString();
                    final JsonCommand command = JsonCommand.from(jsonCommand, jsonObject, this.fromApiJsonHelper, null, loanId, null,
                            groupId, clientId, loanId, null, null, null, null, null, null);
                    command.setJsonCommand(jsonObject.toString());
                    this.loanApplicationWritePlatformService.approveApplication(loanId, command);
                } else if (isRejected) {
                    final String note = "Rechazada la precalificación " + prequalificationGroup.getPrequalificationNumber();
                    jsonObject.addProperty("note", note);
                    jsonObject.addProperty("rejectedOnDate", localDateString);
                    final String jsonCommand = jsonObject.toString();
                    final JsonCommand command = JsonCommand.from(jsonCommand, jsonObject, this.fromApiJsonHelper, null, loanId, null,
                            groupId, clientId, loanId, null, null, null, null, null, null);
                    command.setJsonCommand(jsonObject.toString());
                    this.loanApplicationWritePlatformService.rejectApplication(loanId, command);
                }
            }
        }
    }

    static final class GroupTypeLoanMapper implements RowMapper<LoanData> {

        private final String schema;

        GroupTypeLoanMapper() {
            this.schema = """
                    SELECT ml.id AS loanId,
                    mc.id AS clientId,
                    ml.is_topup AS isTopup,
                    mpg.id AS prequalificationId,
                    mg.id AS groupId,
                    mcv.code_value AS loanCycleCompleted,
                    ml.principal_amount AS principalAmount
                    FROM m_prequalification_group mpg
                    INNER JOIN m_prequalification_group_members mpgm ON mpgm.group_id = mpg.id
                    INNER  JOIN m_group_prequalification_relationship mgpr ON mgpr.prequalification_id = mpg.id
                    INNER JOIN m_group_client mgc ON mgc.group_id = mgpr.group_id
                    INNER JOIN m_group mg ON mg.id = mgc.group_id
                    INNER JOIN m_client mc ON (mgc.client_id = mc.id AND mpgm.dpi = mc.dpi)
                    INNER JOIN m_loan ml ON (ml.client_id = mc.id OR ml.group_id = mg.id)
                    LEFT JOIN m_loan_additionals_group mlad ON mlad.loan_id = ml.id
                    LEFT JOIN m_code_value mcv ON mcv.id = mlad.loan_cycle_completed
                    WHERE mpg.id = ? AND mpg.prequalification_type_enum = 2 AND (ml.client_id = (SELECT mt.id FROM m_client mt WHERE mt.dpi = ?))
                    AND ml.loan_status_id = 100 AND ml.prequalification_id = ?
                    GROUP BY ml.id
                    """;
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public LoanData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long loanId = JdbcSupport.getLong(rs, "loanId");
            final Long clientId = JdbcSupport.getLong(rs, "clientId");
            final Long prequalificationId = JdbcSupport.getLong(rs, "prequalificationId");
            final Boolean isTopup = rs.getBoolean("isTopup");
            final String loanCycleCompleted = rs.getString("loanCycleCompleted");
            final Long groupId = JdbcSupport.getLong(rs, "groupId");
            final BigDecimal principalAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalAmount");
            return LoanData.builder().loanId(loanId).clientId(clientId).prequalificationId(prequalificationId).groupId(groupId)
                    .principalAmount(principalAmount).isTopup(isTopup).loanCycleCompleted(loanCycleCompleted).build();

        }
    }

    static final class IndividualTypeLoanMapper implements RowMapper<LoanData> {

        private final String schema;

        IndividualTypeLoanMapper() {
            this.schema = """
                        SELECT ml.id AS loanId,
                        mc.id AS clientId,
                        mpg.id AS prequalificationId,
                        ml.is_topup AS isTopup,
                        ml.principal_amount AS principalAmount
                        FROM m_prequalification_group mpg
                        INNER JOIN m_prequalification_group_members mpgm ON mpg.id = mpgm.group_id
                        INNER JOIN m_client mc ON mc.dpi = mpgm.dpi
                        INNER JOIN m_loan ml ON ml.client_id = mc.id
                        WHERE mpg.id = ? AND mpg.prequalification_type_enum = ? AND (ml.client_id = (SELECT mt.id FROM m_client mt WHERE mt.dpi = ?))
                        AND ml.loan_status_id = 100 AND ml.prequalification_id = ?
                    """;
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public LoanData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long loanId = JdbcSupport.getLong(rs, "loanId");
            final Long clientId = JdbcSupport.getLong(rs, "clientId");
            final Long prequalificationId = JdbcSupport.getLong(rs, "prequalificationId");
            final Boolean isTopup = rs.getBoolean("isTopup");
            final BigDecimal principalAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalAmount");
            return LoanData.builder().loanId(loanId).clientId(clientId).prequalificationId(prequalificationId)
                    .principalAmount(principalAmount).isTopup(isTopup).build();
        }
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

    @Override
    public void uploadMemberDocs(Long memberId) {
        PrequalificationGroupMember groupMember = this.preQualificationMemberRepository.findOneWithNotFoundDetection(memberId);
        PrequalificationGroup prequalificationGroup = groupMember.getPrequalificationGroup();
        Integer status = prequalificationGroup.getStatus();
        List<PrequalificationStatusLog> statusLogList = this.preQualificationLogRepository.groupStatusLogs(status, prequalificationGroup);
        if (statusLogList.isEmpty())
            throw new PrequalificationStatusNotCompletedException(PrequalificationStatus.fromInt(status).toString());

        PrequalificationStatusLog prequalificationStatusLog = statusLogList.get(0);
        prequalificationStatusLog.updateSubStatus(PrequalificationSubStatus.RE_VALIDATE.getValue());
        this.preQualificationLogRepository.saveAndFlush(prequalificationStatusLog);
    }

    private PrequalificationStatus resolveStatus(String action) {
        PrequalificationStatus status = null;
        if (action.equalsIgnoreCase("sendtoagency")) {
            status = PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL;
        } else if (action.equalsIgnoreCase("sendtoexception")) {
            status = PrequalificationStatus.AGENCY_LEAD_APPROVED_WITH_EXCEPTIONS;
        } else if (action.equalsIgnoreCase("requestupdates")) {
            status = PrequalificationStatus.PREQUALIFICATION_UPDATE_REQUESTED;
        } else if (action.equalsIgnoreCase("rejectanalysis")) {
            status = PrequalificationStatus.REJECTED;
        } else if (action.equalsIgnoreCase("approveanalysis")) {
            status = PrequalificationStatus.COMPLETED;
        } else if (action.equalsIgnoreCase("pending")) {
            status = PrequalificationStatus.PENDING;
        } else if (action.equalsIgnoreCase("reject")) {
            status = PrequalificationStatus.REJECTED;
        }
        return status;
    }

    private Integer resolveIndividualStatusRange(PrequalificationGroup prequalificationGroup, @NotNull String action) {

        List<PrequalificationStatusLog> statusLogs = this.preQualificationStatusLogRepository.groupStatusLogs(prequalificationGroup.getId());
        final boolean ignoreExceptions = statusLogs.stream().anyMatch(statusLog -> PrequalificationStatus.PRE_COMMITTEE_D_PENDING_APPROVAL
                .getValue().equals(statusLog.getToStatus()) && Boolean.FALSE.equals(statusLog.getWithExceptions()));

        Integer finalStatus = null;
        if (action.equalsIgnoreCase("approveanalysis") || action.equalsIgnoreCase("approveCommittee")) {
            Integer fromStatus = prequalificationGroup.getStatus();

            List<PrequalificationGroupMember> members = prequalificationGroup.getMembers();
            BigDecimal totalApprovedAmount = BigDecimal.ZERO;
            for (PrequalificationGroupMember member : members) {
                totalApprovedAmount = totalApprovedAmount.add(member.getApprovedAmount());
            }

            PrequalificationChecklistData prequalificationChecklistData = this.prequalificationChecklistReadPlatformService
                    .retrieveHardPolicyValidationResults(prequalificationGroup.getId());
            List<List<String>> rows = prequalificationChecklistData.getMembers().getRows();
            AtomicReference<Integer> redCountRef = new AtomicReference<>(0);
            for (List<String> innerList : rows) {
                innerList.forEach(item -> {
                    if ("RED".equalsIgnoreCase(item) || "ORANGE".equalsIgnoreCase(item) || "YELLOW".equalsIgnoreCase(item)) {
                        redCountRef.getAndSet(redCountRef.get() + 1);
                    }
                });
            }
            Integer errorWarningsCount = ignoreExceptions ? 0 : redCountRef.get();

            final String membersql = MessageFormat.format(
                    """
                            select {0}
                            WHERE ? BETWEEN c.from_amount AND c.to_amount AND ((? > c.limit AND c.condition = ''GREATER_THAN'')
                            OR (? <= c.limit AND c.condition = ''LESS_THAN'') )
                             ORDER BY cv.code_value desc;""", this.committeeApprovalsMapper.schema());

            List<CommitteeApprovalsData> approvalsRequired = this.jdbcTemplate.query(membersql, this.committeeApprovalsMapper,
                    new Object[] { totalApprovedAmount, errorWarningsCount, errorWarningsCount });

            approvalsRequired.sort(Comparator.comparing(CommitteeApprovalsData::getCommittee).reversed());
            finalStatus = PrequalificationStatus.COMPLETED.getValue();
            if (!approvalsRequired.isEmpty()) {
                for (CommitteeApprovalsData approvalsData : approvalsRequired) {
                    Integer committeeRequired = PrequalificationStatus.resolveCommitteeStatus(approvalsData.getCommittee()).getValue();
                    if (fromStatus < committeeRequired) {
                        finalStatus = committeeRequired;
                        break;
                    }
                }
            }
        }

        return finalStatus;
    }

    // private PrequalificationStatusRange resolveIndividualStatusRangeOld(PrequalificationGroup prequalificationGroup,
    // String action) {
    // PrequalificationStatusRange finalRange = null;
    //
    // if (action.equalsIgnoreCase("approveanalysis") || action.equalsIgnoreCase("approveCommittee")) {
    // BigDecimal amount = prequalificationGroup.getTotalRequestedAmount();
    // PrequalificationChecklistData prequalificationChecklistData = this.prequalificationChecklistReadPlatformService
    // .retrieveHardPolicyValidationResults(prequalificationGroup.getId());
    // List<List<String>> rows = prequalificationChecklistData.getMembers().getRows();
    // AtomicReference<Integer> redCountRef = new AtomicReference<>(0);
    // for (List<String> innerList : rows) {
    // innerList.forEach(item -> {
    // if ("RED".equalsIgnoreCase(item) || "ORANGE".equalsIgnoreCase(item) || "YELLOW".equalsIgnoreCase(item)) {
    // redCountRef.getAndSet(redCountRef.get() + 1);
    // }
    // });
    // }
    // Integer errorWarningsCount = redCountRef.get();
    //
    // List<PrequalificationStatusRange> statusRangeList = this.prequalificationStatusRangeRepository
    // .findByPrequalificationTypeAndNumberOfErrors(prequalificationGroup.getPrequalificationType(),
    // errorWarningsCount);
    //
    // if (statusRangeList.size() == 1) {
    // finalRange = statusRangeList.get(0);
    // } else {
    // for (PrequalificationStatusRange statusRange : statusRangeList) {
    // if (amount.compareTo(statusRange.getMinAmount()) >= 0
    // && (statusRange.getMaxAmount() != null && amount.compareTo(statusRange.getMaxAmount()) <= 0)) {
    // finalRange = statusRange;
    // break;
    // } else if (amount.compareTo(statusRange.getMinAmount()) >= 0 && statusRange.getMaxAmount() == null) {
    // finalRange = statusRange;
    // break;
    // }
    // }
    // }
    //
    // }
    //
    // return finalRange;
    // }

    private PrequalificationType resolvePrequalificationType(LoanProduct loanProduct) {
        if (loanProduct.getOwnerType() != null) {
            LoanProductOwnerType ownerType = LoanProductOwnerType.fromInt(loanProduct.getOwnerType());
            if (ownerType.equals(LoanProductOwnerType.INDIVIDUAL)) {
                return PrequalificationType.INDIVIDUAL;
            }
            if (ownerType.equals(LoanProductOwnerType.GROUP)) {
                return PrequalificationType.GROUP;
            }
            if (ownerType.equals(LoanProductOwnerType.PAE)) {
                return PrequalificationType.PAE;
            }
        }
        return PrequalificationType.INVALID;
    }

    private void updateLoanAssociated(JsonCommand jsonCommand) {
        final Long groupId = jsonCommand.getGroupId();
        Loan loan = loanRepositoryWrapper.retrieveByPrequalificationId(groupId);
        if (loan == null) {
            throw new NotFoundException("Loan with group id " + groupId + " not found");
        }
        modify(loan.getId(), jsonCommand);
    }

    private void modify(Long loanId, JsonCommand command) {

        final BigDecimal rate = command.bigDecimalValueOfParameterNamed("interestRatePerPeriod");
        final BigDecimal principal = command.bigDecimalValueOfParameterNamed("principal");
        final Long loanTermFrequency = command.longValueOfParameterNamed("loanTermFrequency");

        CommandSource source = commandSourceRepository.findByLoanIdAndLastModification(loanId);
        JsonElement element = JsonParser.parseString(source.getCommandAsJson());
        JsonObject object = element.getAsJsonObject();

        object.addProperty("interestRatePerPeriod", rate);
        object.addProperty("principal", principal);
        object.addProperty("loanTermFrequency", loanTermFrequency);
        object.addProperty("numberOfRepayments", loanTermFrequency);
        element = JsonParser.parseString(object.toString());
        JsonCommand jsonCommand = JsonCommand.fromJsonElement(loanId, element, command.getFromApiJsonHelper());
        jsonCommand.setJsonCommand(object.toString());

        loanApplicationWritePlatformService.modifyApplication(loanId, jsonCommand);
    }

    @Override
    public void addExceptionCommentsToPrequalification(Long groupId, String comment, String description) {
        PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper.findOneWithNotFoundDetection(groupId);
        boolean isException = false;
        if (PrequalificatoinApiConstants.exceptionComments.equalsIgnoreCase(description)) {
            prequalificationGroup.updateExceptionComments(comment);
            isException = true;
        } else {
            prequalificationGroup.updateComments(comment);
        }
        Integer fromStatus = prequalificationGroup.getStatus();
        Integer toStatus = fromStatus;

        PrequalificationStatusLog lastLog = this.preQualificationStatusLogRepository.findTopByPrequalificationGroupIdOrderByIdDesc(groupId);
        if (lastLog != null) {
            fromStatus = lastLog.getFromStatus();
            toStatus = lastLog.getToStatus();
        }
        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);
        AppUser addedBy = this.context.getAuthenticatedUserIfPresent();
        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(addedBy, fromStatus, toStatus, comment,
                prequalificationGroup, null, null, isException);
        this.preQualificationLogRepository.saveAndFlush(statusLog);
    }

}
