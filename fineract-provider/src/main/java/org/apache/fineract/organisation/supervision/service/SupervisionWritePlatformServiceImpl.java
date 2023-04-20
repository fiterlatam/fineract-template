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
package org.apache.fineract.organisation.supervision.service;

import java.util.Map;
import javax.persistence.PersistenceException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.exception.NoAuthorizationException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.supervision.domain.Supervision;
import org.apache.fineract.organisation.supervision.domain.SupervisionRepositoryWrapper;
import org.apache.fineract.organisation.supervision.serialization.SupervisionCommandFromApiJsonDeserializer;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupervisionWritePlatformServiceImpl implements SupervisionWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(SupervisionWritePlatformServiceImpl.class);

    private final PlatformSecurityContext context;
    private final SupervisionCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final SupervisionRepositoryWrapper supervisionRepositoryWrapper;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final AppUserRepository appUserRepository;

    @Autowired
    public SupervisionWritePlatformServiceImpl(PlatformSecurityContext context,
            SupervisionCommandFromApiJsonDeserializer fromApiJsonDeserializer, SupervisionRepositoryWrapper supervisionRepositoryWrapper,
            OfficeRepositoryWrapper officeRepositoryWrapper,

            AppUserRepository appUserRepository) {
        this.context = context;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.supervisionRepositoryWrapper = supervisionRepositoryWrapper;
        this.officeRepositoryWrapper = officeRepositoryWrapper;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    @Override
    public CommandProcessingResult createSupervision(JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            Long parentId = null;
            if (command.parameterExists(SupervisionConstants.SupervisionSupportedParameters.OFFICE_PARENT_ID.getValue())) {
                parentId = command
                        .longValueOfParameterNamed(SupervisionConstants.SupervisionSupportedParameters.OFFICE_PARENT_ID.getValue());
            }

            final Office parentOffice = validateUserPrivilegeOnOfficeAndRetrieve(currentUser, parentId);

            final Long responsibleUserId = command
                    .longValueOfParameterNamed(SupervisionConstants.SupervisionSupportedParameters.RESPONSIBLE_USER_ID.getValue());
            AppUser responsibleUser = null;
            if (responsibleUserId != null) {
                responsibleUser = this.appUserRepository.findById(responsibleUserId)
                        .orElseThrow(() -> new UserNotFoundException(responsibleUserId));
            }

            final Supervision supervision = Supervision.fromJson(parentOffice, responsibleUser, command);

            this.supervisionRepositoryWrapper.saveAndFlush(supervision);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(supervision.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleSupervisionDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleSupervisionDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateSupervision(Long supervisionId, JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            Supervision supervision = this.supervisionRepositoryWrapper.findOneWithNotFoundDetection(supervisionId);

            final Map<String, Object> changes = supervision.update(command);

            Long parentId;
            if (command.parameterExists(SupervisionConstants.SupervisionSupportedParameters.OFFICE_PARENT_ID.getValue())) {
                parentId = command
                        .longValueOfParameterNamed(SupervisionConstants.SupervisionSupportedParameters.OFFICE_PARENT_ID.getValue());

                final Office parentOffice = validateUserPrivilegeOnOfficeAndRetrieve(currentUser, parentId);
                supervision.setParentOffice(parentOffice);
            }

            if (command
                    .longValueOfParameterNamed(SupervisionConstants.SupervisionSupportedParameters.RESPONSIBLE_USER_ID.getValue()) != 0) {
                final Long responsibleUserId = command
                        .longValueOfParameterNamed(SupervisionConstants.SupervisionSupportedParameters.RESPONSIBLE_USER_ID.getValue());
                AppUser responsibleUser = null;
                if (responsibleUserId != null) {
                    responsibleUser = this.appUserRepository.findById(responsibleUserId)
                            .orElseThrow(() -> new UserNotFoundException(responsibleUserId));
                    supervision.setResponsibleUser(responsibleUser);
                    changes.put(SupervisionConstants.SupervisionSupportedParameters.RESPONSIBLE_USER_ID.getValue(), responsibleUserId);
                }
            }

            this.supervisionRepositoryWrapper.saveAndFlush(supervision);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(supervisionId) //
                    .with(changes) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleSupervisionDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleSupervisionDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteSupervision(Long supervisionId) {
        try {

            final Supervision supervision = this.supervisionRepositoryWrapper.findOneWithNotFoundDetection(supervisionId);

            this.supervisionRepositoryWrapper.delete(supervision);
            return new CommandProcessingResultBuilder() //
                    .withEntityId(supervision.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            LOG.error("Error occurred.", dve);
            throw new PlatformDataIntegrityException("error.msg.supervision.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.", dve);
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleSupervisionDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("name")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.supervision.duplicate.name",
                    "Supervision with name '" + name + "' already exists", "name", name);
        }

        throw new PlatformDataIntegrityException("error.msg.supervision.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    /*
     * Used to restrict modifying operations to office that are either the users office or lower (child) in the office
     * hierarchy
     */
    private Office validateUserPrivilegeOnOfficeAndRetrieve(final AppUser currentUser, final Long officeId) {

        final Long userOfficeId = currentUser.getOffice().getId();
        final Office userOffice = this.officeRepositoryWrapper.findOfficeHierarchy(userOfficeId);
        if (userOffice.doesNotHaveAnOfficeInHierarchyWithId(officeId)) {
            throw new NoAuthorizationException("User does not have sufficient privileges to act on the provided office.");
        }

        Office officeToReturn = userOffice;
        if (!userOffice.identifiedBy(officeId)) {
            officeToReturn = this.officeRepositoryWrapper.findOfficeHierarchy(officeId);
        }

        return officeToReturn;
    }
}
