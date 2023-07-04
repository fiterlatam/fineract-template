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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.PersistenceException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.centerGroup.domain.CenterGroup;
import org.apache.fineract.organisation.centerGroup.domain.CenterGroupRepositoryWrapper;
import org.apache.fineract.organisation.centerGroup.exception.CenterGroupMeetingTimeCollisionException;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CenterGroupWritePlatformServiceImpl implements CenterGroupWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(CenterGroupWritePlatformServiceImpl.class);
    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;
    private final CenterGroupCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final FromJsonHelper fromJsonHelper;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final CenterGroupRepositoryWrapper centerGroupRepositoryWrapper;
    private final PortfolioCenterRepositoryWrapper portfolioCenterRepositoryWrapper;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final AppUserRepository appUserRepository;
    private final CodeValueRepository codeValueRepository;
    private final ConfigurationReadPlatformService configurationReadPlatformService;

    @Autowired
    public CenterGroupWritePlatformServiceImpl(PlatformSecurityContext context, final JdbcTemplate jdbcTemplate,
            CenterGroupCommandFromApiJsonDeserializer fromApiJsonDeserializer, OfficeRepositoryWrapper officeRepositoryWrapper,
            CenterGroupRepositoryWrapper centerGroupRepositoryWrapper, PortfolioCenterRepositoryWrapper portfolioCenterRepositoryWrapper,
            CodeValueReadPlatformService codeValueReadPlatformService, AppUserRepository appUserRepository,
            CodeValueRepository codeValueRepository, ConfigurationReadPlatformService configurationReadPlatformService,
            FromJsonHelper fromJsonHelper) {
        this.context = context;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.officeRepositoryWrapper = officeRepositoryWrapper;
        this.centerGroupRepositoryWrapper = centerGroupRepositoryWrapper;
        this.portfolioCenterRepositoryWrapper = portfolioCenterRepositoryWrapper;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.appUserRepository = appUserRepository;
        this.codeValueRepository = codeValueRepository;
        this.configurationReadPlatformService = configurationReadPlatformService;
        this.jdbcTemplate = jdbcTemplate;
        this.fromJsonHelper = fromJsonHelper;
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

            Integer meetingDefaultDuration = 0;
            Integer timeBetweenMeetings = 0;

            GlobalConfigurationPropertyData meetingDefaultDurationConfig = configurationReadPlatformService
                    .retrieveGlobalConfiguration("meeting-default-duration");
            if (meetingDefaultDurationConfig != null) {
                meetingDefaultDuration = meetingDefaultDurationConfig.getValue().intValue();
            }
            GlobalConfigurationPropertyData timeBetweenMeetingsConfig = configurationReadPlatformService
                    .retrieveGlobalConfiguration("time-between-meetings");
            if (timeBetweenMeetingsConfig != null) {
                timeBetweenMeetings = timeBetweenMeetingsConfig.getValue().intValue();
            }

            final Long responsibleUserId = command
                    .longValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.RESPONSIBLE_USER_ID.getValue());
            AppUser responsibleUser = null;
            if (responsibleUserId != null) {
                responsibleUser = this.appUserRepository.findById(responsibleUserId)
                        .orElseThrow(() -> new UserNotFoundException(responsibleUserId));
            }

            final CenterGroup centerGroup = CenterGroup.fromJson(portfolioCenter, responsibleUser, command, meetingDefaultDuration,
                    timeBetweenMeetings);

            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

            // confirm meeting start time is not after meeting end time
            if (centerGroup.getMeetingStartTime().isAfter(centerGroup.getMeetingEndTime())) {
                final ApiParameterError error = ApiParameterError.parameterErrorWithValue(
                        "error.msg.centerGroup.meetingStartTime.after.meetingEndTime",
                        "Meeting start time '" + centerGroup.getMeetingStartTime() + "' cannot be after meeting end time '"
                                + centerGroup.getMeetingEndTime() + "'",
                        CenterGroupConstants.CenterGroupSupportedParameters.MEETING_START_TIME.getValue(),
                        centerGroup.getMeetingStartTime().toString());
                dataValidationErrors.add(error);
            }

            // Check if centerGroup stand and end date is within the PortfolioCenter start and end dates
            if (portfolioCenter != null) {
                if (centerGroup.getMeetingStartTime().isBefore(portfolioCenter.getMeetingStartTime())) {
                    final ApiParameterError error = ApiParameterError.parameterErrorWithValue(
                            "error.msg.centerGroup.startDate.before.portfolioCenterStartDate",
                            "Center group start date '" + centerGroup.getMeetingStartTime()
                                    + "' cannot be before portfolio center start date '" + portfolioCenter.getMeetingStartTime() + "'",
                            CenterGroupConstants.CenterGroupSupportedParameters.MEETING_START_TIME.getValue(),
                            centerGroup.getMeetingStartTime().toString());
                    dataValidationErrors.add(error);
                }

                if (centerGroup.getMeetingEndTime().isAfter(portfolioCenter.getMeetingEndTime())) {
                    final ApiParameterError error = ApiParameterError.parameterErrorWithValue(
                            "error.msg.centerGroup.endDate.after.portfolioCenterEndDate",
                            "Center group end date '" + centerGroup.getMeetingEndTime() + "' cannot be after portfolio center end date '"
                                    + portfolioCenter.getMeetingEndTime() + "'",
                            CenterGroupConstants.CenterGroupSupportedParameters.MEETING_END_TIME.getValue(),
                            centerGroup.getMeetingEndTime().toString());
                    dataValidationErrors.add(error);
                }
            }

            // check for overlapping center groups
//            List<CenterGroup> overLappingCenterGroups = centerGroupRepositoryWrapper.findOverLappingCenterGroups(portfolioCenterId,
//                    centerGroup.getMeetingStartTime(), centerGroup.getMeetingEndTime());
//            if (overLappingCenterGroups.size() > 0) {
//                for (CenterGroup centerGroup1 : overLappingCenterGroups) {
//
//                    final ApiParameterError error = ApiParameterError.parameterErrorWithValue("error.msg.centerGroup.overlapping",
//                            "Center Group with id " + centerGroup1.getId() + " with duration '" + centerGroup1.getMeetingStartTime() + " - "
//                                    + centerGroup1.getMeetingEndTime() + "' overlaps with the new center group",
//                            CenterGroupConstants.CenterGroupSupportedParameters.FORMATION_DATE.getValue(),
//                            centerGroup.getMeetingStartTime().toString());
//                    dataValidationErrors.add(error);
//                }
//            }

            String schemaSql = "Select cgroup.id from m_center_group cgroup where cgroup.portfolio_center_id = ? and "
                    + "( ( ? >= cgroup.meeting_start_time and ? < cgroup.meeting_end_time) OR "
                    + "( ? > cgroup.meeting_start_time and ? < cgroup.meeting_end_time) ) order by id desc";
            List<Long> groupIds = jdbcTemplate.queryForList(schemaSql, Long.class, portfolioCenterId, centerGroup.getMeetingStartTime(),
                    centerGroup.getMeetingStartTime(), centerGroup.getMeetingEndTime(), centerGroup.getMeetingEndTime());

            if (groupIds.size() > 0) {
                CenterGroup group = centerGroupRepositoryWrapper.findOneWithNotFoundDetection(groupIds.get(0));

                throw new CenterGroupMeetingTimeCollisionException(group.getName(), group.getId(), group.getMeetingStartTime(),
                        group.getMeetingEndTime());
            }

            if (CollectionUtils.isNotEmpty(dataValidationErrors)) {
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors);
            }

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
            LocalTime newMeetingStarTime = null;
            if (StringUtils.isNotBlank(meetingStartTime)) {
                newMeetingStarTime = LocalTime.parse(meetingStartTime, dateTimeFormatter);
                centerGroup.setMeetingStartTime(newMeetingStarTime);
                changes.put(CenterGroupConstants.CenterGroupSupportedParameters.MEETING_START_TIME.getValue(), meetingStartTime);
            }

            String meetingEndTime = command
                    .stringValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.MEETING_END_TIME.getValue());
            LocalTime newMeetingEndTime = null;
            if (StringUtils.isNotBlank(meetingEndTime)) {
                newMeetingEndTime = LocalTime.parse(meetingEndTime, dateTimeFormatter);
                centerGroup.setMeetingEndTime(newMeetingEndTime);
                changes.put(CenterGroupConstants.CenterGroupSupportedParameters.MEETING_END_TIME.getValue(), meetingEndTime);
            }
            String schemaSql = "Select cgroup.id from m_center_group cgroup where cgroup.portfolio_center_id = ? and "
                    + "( ( ? >= cgroup.meeting_start_time and ? < cgroup.meeting_end_time) OR "
                    + "( ? > cgroup.meeting_start_time and ? < cgroup.meeting_end_time) ) order by id desc";
            List<Long> groupIds = jdbcTemplate.queryForList(schemaSql, Long.class, newParentPortfolioCenter.getId(), newMeetingStarTime,
                    newMeetingStarTime, newMeetingEndTime, newMeetingEndTime);

            if (groupIds.size() > 0) {
                CenterGroup group = centerGroupRepositoryWrapper.findOneWithNotFoundDetection(groupIds.get(0));

                throw new CenterGroupMeetingTimeCollisionException(group.getName(), group.getId(), group.getMeetingStartTime(),
                        group.getMeetingEndTime());
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
