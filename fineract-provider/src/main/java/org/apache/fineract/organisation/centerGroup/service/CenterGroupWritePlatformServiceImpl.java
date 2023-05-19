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
package org.apache.fineract.organisation.centerGroup.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.PersistenceException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.centerGroup.domain.CenterGroup;
import org.apache.fineract.organisation.centerGroup.domain.CenterGroupRepositoryWrapper;
import org.apache.fineract.organisation.centerGroup.serialization.CenterGroupCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenter;
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenterRepositoryWrapper;
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
public class CenterGroupWritePlatformServiceImpl implements CenterGroupWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(CenterGroupWritePlatformServiceImpl.class);

    private final PlatformSecurityContext context;
    private final CenterGroupCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final CenterGroupRepositoryWrapper centerGroupRepositoryWrapper;
    private final PortfolioCenterRepositoryWrapper portfolioCenterRepositoryWrapper;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final AppUserRepository appUserRepository;
    private final CodeValueRepository codeValueRepository;

    @Autowired
    public CenterGroupWritePlatformServiceImpl(PlatformSecurityContext context,
            CenterGroupCommandFromApiJsonDeserializer fromApiJsonDeserializer, OfficeRepositoryWrapper officeRepositoryWrapper,
            CenterGroupRepositoryWrapper centerGroupRepositoryWrapper, PortfolioCenterRepositoryWrapper portfolioCenterRepositoryWrapper,
            CodeValueReadPlatformService codeValueReadPlatformService, AppUserRepository appUserRepository,
            CodeValueRepository codeValueRepository) {
        this.context = context;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.officeRepositoryWrapper = officeRepositoryWrapper;
        this.centerGroupRepositoryWrapper = centerGroupRepositoryWrapper;
        this.portfolioCenterRepositoryWrapper = portfolioCenterRepositoryWrapper;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.appUserRepository = appUserRepository;
        this.codeValueRepository = codeValueRepository;
    }

    @Transactional
    @Override
    public CommandProcessingResult createCenterGroup(Long portfolioCenterId, JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            PortfolioCenter portfolioCenter = null;
            if (portfolioCenterId != null) {
                portfolioCenter = this.portfolioCenterRepositoryWrapper.findOneWithNotFoundDetection(portfolioCenterId);
            }

            final Long responsibleUserId = command
                    .longValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.RESPONSIBLE_USER_ID.getValue());
            AppUser responsibleUser = null;
            if (responsibleUserId != null) {
                responsibleUser = this.appUserRepository.findById(responsibleUserId)
                        .orElseThrow(() -> new UserNotFoundException(responsibleUserId));
            }

            final CenterGroup centerGroup = CenterGroup.fromJson(portfolioCenter, responsibleUser, command);

            this.centerGroupRepositoryWrapper.saveAndFlush(centerGroup);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(centerGroup.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleCenterGroupDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleCenterGroupDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateCenterGroup(Long centerGroupId, JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            CenterGroup centerGroup = this.centerGroupRepositoryWrapper.findOneWithNotFoundDetection(centerGroupId);

            final Map<String, Object> changes = centerGroup.update(command);

            this.centerGroupRepositoryWrapper.saveAndFlush(centerGroup);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(centerGroupId) //
                    .with(changes) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleCenterGroupDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleCenterGroupDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteCenterGroup(Long centerGroupId) {

        return null;
    }

    @Override
    public CommandProcessingResult transferCenterGroup(Long centerGroupId, JsonCommand command) {
        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForTransfer(command.json());

            CenterGroup centerGroup = this.centerGroupRepositoryWrapper.findOneWithNotFoundDetection(centerGroupId);

            final Map<String, Object> changes = new LinkedHashMap<>(2);

            final Long destinationPortfolioCenterId = command.longValueOfParameterNamed(
                    CenterGroupConstants.CenterGroupSupportedParameters.DESTINATION_PORTFOLIO_CENTER_ID.getValue());

            PortfolioCenter newParentPortfolioCenter = null;
            if (destinationPortfolioCenterId != null) {
                newParentPortfolioCenter = this.portfolioCenterRepositoryWrapper.findOneWithNotFoundDetection(destinationPortfolioCenterId);
                centerGroup.setPortfolioCenter(newParentPortfolioCenter);
                changes.put(CenterGroupConstants.CenterGroupSupportedParameters.DESTINATION_PORTFOLIO_CENTER_ID.getValue(),
                        destinationPortfolioCenterId);
            }

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
            String meetingStartTime = command
                    .stringValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.MEETING_START_TIME.getValue());
            if (StringUtils.isNotBlank(meetingStartTime)) {
                LocalTime newMeetingStarTime = LocalTime.parse(meetingStartTime, dateTimeFormatter);
                centerGroup.setMeetingStartTime(newMeetingStarTime);
                changes.put(CenterGroupConstants.CenterGroupSupportedParameters.MEETING_START_TIME.getValue(),
                        meetingStartTime);
            }

            String meetingEndTime = command
                    .stringValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.MEETING_END_TIME.getValue());
            if (StringUtils.isNotBlank(meetingEndTime)) {
                LocalTime newMeetingEndTime = LocalTime.parse(meetingEndTime, dateTimeFormatter);
                centerGroup.setMeetingEndTime(newMeetingEndTime);
                changes.put(CenterGroupConstants.CenterGroupSupportedParameters.MEETING_END_TIME.getValue(),
                        meetingEndTime);
            }

            this.centerGroupRepositoryWrapper.saveAndFlush(centerGroup);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(centerGroupId) //
                    .with(changes) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleCenterGroupDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleCenterGroupDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleCenterGroupDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("name") && command != null) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.center.group.duplicate.name",
                    "Group with name '" + name + "' already exists", "name", name);
        }

        throw new PlatformDataIntegrityException("error.msg.center.group.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }
}
