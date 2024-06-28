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
package org.apache.fineract.organisation.committee.service;

import java.util.List;
import javax.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.committee.data.CommitteeData;
import org.apache.fineract.organisation.committee.domain.Committee;
import org.apache.fineract.organisation.committee.domain.CommitteeRepositoryWrapper;
import org.apache.fineract.organisation.committee.serialization.CommitteeCommandFromApiJsonDeserializer;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CommitteeWritePlatformServiceJpaRepositoryImpl implements CommitteeWritePlatformService {

    private final PlatformSecurityContext context;
    private final CommitteeCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final CommitteeRepositoryWrapper committeeRepositoryWrapper;
    private final CodeValueRepository codeValueRepository;
    private final AppUserRepository appUserRepository;
    private final CommitteeReadPlatformService committeeReadPlatformService;

    public CommitteeWritePlatformServiceJpaRepositoryImpl(final PlatformSecurityContext context,
            final CommitteeCommandFromApiJsonDeserializer fromApiJsonDeserializer,
            final CommitteeRepositoryWrapper committeeRepositoryWrapper, final CodeValueRepository codeValueRepository,
            final AppUserRepository appUserRepository, final CommitteeReadPlatformService committeeReadPlatformService) {
        this.context = context;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.committeeRepositoryWrapper = committeeRepositoryWrapper;
        this.codeValueRepository = codeValueRepository;
        this.appUserRepository = appUserRepository;
        this.committeeReadPlatformService = committeeReadPlatformService;
    }

    @Transactional
    @Override
    public CommandProcessingResult createCommittee(JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            // Get code values for committee
            CodeValue committeeCode = null;
            final Long committeeId = command
                    .longValueOfParameterNamed(CommitteeConstants.CommitteeSupportedParameters.CODE_VALUE_ID.getValue());
            committeeCode = codeValueRepository.getReferenceById(committeeId);

            final String[] usersIds = getUsersIds(command);
            for (String userId : usersIds) {
                AppUser appUser = appUserRepository.findById(Long.valueOf(userId)).orElseThrow();

                // validate user assignment
                boolean isUserAlreadyAssigned = isUserAlreadyAssigned(committeeCode, appUser);
                if (isUserAlreadyAssigned) {
                    throw new PlatformDataIntegrityException("error.msg.committee.user.already.assigned",
                            "User with name `" + appUser.getDisplayName() + "` already assigned to committee", "name",
                            appUser.getDisplayName());
                }

                Committee committee = new Committee(committeeCode, appUser);
                this.committeeRepositoryWrapper.save(committee);
            }

            return new CommandProcessingResultBuilder() //
                    .withEntityId(committeeId) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleCommitteeDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleCommitteeDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateCommittee(Long committeeId, JsonCommand command) {
        try {

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            CommitteeData committeeData = committeeReadPlatformService.findByCommitteeId(committeeId);

            // Get code values for committee
            CodeValue committeeCode = null;
            committeeCode = codeValueRepository.getReferenceById(committeeId);

            if (committeeData != null) {
                committeeRepositoryWrapper.deleteByCommittee(committeeCode);
            }

            final String[] usersIds = getUsersIds(command);
            for (String userId : usersIds) {
                AppUser appUser = appUserRepository.findById(Long.valueOf(userId)).orElseThrow();

                // validate user assignment
                boolean isUserAlreadyAssigned = isUserAlreadyAssigned(committeeCode, appUser);
                if (isUserAlreadyAssigned) {
                    throw new PlatformDataIntegrityException("error.msg.committee.user.already.assigned",
                            "User with name `" + appUser.getDisplayName() + "` already assigned to committee", "name",
                            appUser.getDisplayName());
                }

                Committee committee = new Committee(committeeCode, appUser);
                this.committeeRepositoryWrapper.save(committee);
            }

            return new CommandProcessingResultBuilder() //
                    .withEntityId(committeeId) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleCommitteeDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleCommitteeDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteCommittee(Long committeeId) {
        try {
            // Get code values for committee
            CodeValue committeeCode = null;
            if (committeeId != null) {
                committeeCode = codeValueRepository.getReferenceById(committeeId);
            }

            committeeRepositoryWrapper.deleteByCommittee(committeeCode);

            return new CommandProcessingResultBuilder() //
                    .withEntityId(committeeId) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            log.error("Error occurred.", throwable);
            throw new PlatformDataIntegrityException("error.msg.committee.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.", dve);
        }
    }

    private String[] getUsersIds(final JsonCommand command) {
        final String[] usersIds = command.arrayValueOfParameterNamed(CommitteeConstants.CommitteeSupportedParameters.USERS.getValue());
        return usersIds;
    }

    private boolean isUserAlreadyAssigned(final CodeValue actualCommitteeCode, final AppUser user) {
        final List<Committee> committeeList = committeeRepositoryWrapper.getUserAssignmentsToCommittees(user);
        if (committeeList != null && !committeeList.isEmpty()) {
            for (Committee committeeAssignment : committeeList) {
                if (!committeeAssignment.getCommittee().getId().equals(actualCommitteeCode.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleCommitteeDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("committeeId")) {
            final String committeeId = command.stringValueOfParameterNamed("committeeId");
            throw new PlatformDataIntegrityException("error.msg.committee.duplicate.id",
                    "Bank with id '" + committeeId + "' already exists", "id", committeeId);
        }

        throw new PlatformDataIntegrityException("error.msg.agency.committee.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }
}
