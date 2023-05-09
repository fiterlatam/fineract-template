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
package org.apache.fineract.organisation.portfolioCenter.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.persistence.PersistenceException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.portfolio.domain.Portfolio;
import org.apache.fineract.organisation.portfolio.domain.PortfolioRepositoryWrapper;
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenter;
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenterFrecuencyMeeting;
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenterRepositoryWrapper;
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenterStatus;
import org.apache.fineract.organisation.portfolioCenter.serialization.PortfolioCenterCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.rangeTemplate.data.RangeTemplateData;
import org.apache.fineract.organisation.rangeTemplate.service.RangeTemplateReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioCenterWritePlatformServiceImpl implements PortfolioCenterWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(PortfolioCenterWritePlatformServiceImpl.class);

    private final PlatformSecurityContext context;
    private final PortfolioCenterCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final PortfolioRepositoryWrapper portfolioRepositoryWrapper;
    private final PortfolioCenterRepositoryWrapper portfolioCenterRepositoryWrapper;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final RangeTemplateReadPlatformService rangeTemplateReadPlatformService;
    private final AppUserRepository appUserRepository;
    private final CodeValueRepository codeValueRepository;

    public PortfolioCenterWritePlatformServiceImpl(PlatformSecurityContext context,
            PortfolioCenterCommandFromApiJsonDeserializer fromApiJsonDeserializer, PortfolioRepositoryWrapper portfolioRepositoryWrapper,
            PortfolioCenterRepositoryWrapper portfolioCenterRepositoryWrapper, CodeValueReadPlatformService codeValueReadPlatformService,
            RangeTemplateReadPlatformService rangeTemplateReadPlatformService, AppUserRepository appUserRepository,
            CodeValueRepository codeValueRepository) {
        this.context = context;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.portfolioRepositoryWrapper = portfolioRepositoryWrapper;
        this.portfolioCenterRepositoryWrapper = portfolioCenterRepositoryWrapper;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.rangeTemplateReadPlatformService = rangeTemplateReadPlatformService;
        this.appUserRepository = appUserRepository;
        this.codeValueRepository = codeValueRepository;
    }

    @Transactional
    @Override
    public CommandProcessingResult generateAllCentersByPortfolio(Portfolio portfolio) {

        try {
            final List<CodeValueData> meetingDayOptions = new ArrayList<>(
                    this.codeValueReadPlatformService.retrieveCodeValuesByCode(PortfolioCenterConstants.MEETING_DAYS));

            LocalDate currentTenantDate = DateUtils.getLocalDateOfTenant();
            int week = 1;

            // get the range template and generate the centers for the parent portfolio
            Collection<RangeTemplateData> rangeTemplateDataCollection = rangeTemplateReadPlatformService.retrieveAll();
            for (RangeTemplateData rangeTemplateData : rangeTemplateDataCollection) {

                for (CodeValueData meetingDay : meetingDayOptions) {
                    // complete required fields for entity
                    final String centerName = generateCenterName(portfolio.getId(), rangeTemplateData, meetingDay);

                    PortfolioCenter entity = PortfolioCenter.assembleFrom(centerName, portfolio, PortfolioCenterStatus.ACTIVE.getValue());

                    portfolioCenterRepositoryWrapper.save(entity);
                }
                // increment week
                week = week + 1;
            }

            return new CommandProcessingResultBuilder().withEntityId(portfolio.getId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handlePortfolioCenterDataIntegrityIssues(null, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handlePortfolioCenterDataIntegrityIssues(null, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updatePortfolioCenter(Long portfolioCenterId, JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            PortfolioCenter portfolioCenter = this.portfolioCenterRepositoryWrapper.findOneWithNotFoundDetection(portfolioCenterId);

            final Map<String, Object> changes = portfolioCenter.update(command);

            // Get code values for fields
            if (command.longValueOfParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.CITY_ID.getValue()) != 0) {
                final Long cityId = command
                        .longValueOfParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.CITY_ID.getValue());
                CodeValue city = codeValueRepository.getReferenceById(cityId);
                portfolioCenter.setCity(city);
                changes.put(PortfolioCenterConstants.PortfolioCenterSupportedParameters.CITY_ID.getValue(), cityId);
            }

            if (command.longValueOfParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.STATE_ID.getValue()) != 0) {
                final Long stateId = command
                        .longValueOfParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.STATE_ID.getValue());
                CodeValue setStateProvince = codeValueRepository.getReferenceById(stateId);
                portfolioCenter.setStateProvince(setStateProvince);
                changes.put(PortfolioCenterConstants.PortfolioCenterSupportedParameters.STATE_ID.getValue(), stateId);
            }

            if (command
                    .longValueOfParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.CENTER_TYPE.getValue()) != 0) {
                final Long centerTypeId = command
                        .longValueOfParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.CENTER_TYPE.getValue());
                CodeValue centerType = codeValueRepository.getReferenceById(centerTypeId);
                portfolioCenter.setType(centerType);
                changes.put(PortfolioCenterConstants.PortfolioCenterSupportedParameters.CENTER_TYPE.getValue(), centerTypeId);
            }

            this.portfolioCenterRepositoryWrapper.saveAndFlush(portfolioCenter);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(portfolioCenterId) //
                    .with(changes) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handlePortfolioCenterDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handlePortfolioCenterDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handlePortfolioCenterDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("name") && command != null) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.portfolio.duplicate.name",
                    "Portfolio with name '" + name + "' already exists", "name", name);
        }

        throw new PlatformDataIntegrityException("error.msg.portfolio.center.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private String generateCenterName(Long portfolioId, RangeTemplateData rangeTemplateData, CodeValueData meetingDay) {
        final StringBuilder centerName = new StringBuilder();
        centerName.append(portfolioId);
        centerName.append("-");
        centerName.append(rangeTemplateData.getCode());
        centerName.append("-");
        centerName.append(meetingDay.getName());

        return centerName.toString();
    }

    @SuppressWarnings("unused")
    private static LocalDate calculateMeetingDate(LocalDate startingDate, RangeTemplateData rangeTemplate, int dayOfWeekNumber,
            int weekOfMonth, PortfolioCenterFrecuencyMeeting frecuencyMeeting) {
        LocalDate meetingDate = null;
        boolean isMeeetingDateInRange;

        switch (frecuencyMeeting) {
            case MENSUAL:
                // calculate first meeting date
                DayOfWeek dow = DayOfWeek.of(dayOfWeekNumber);
                meetingDate = startingDate.with(WeekFields.ISO.weekOfMonth(), weekOfMonth).with(dow);
                isMeeetingDateInRange = dateInRange(meetingDate.getDayOfMonth(), rangeTemplate.getStartDay(), rangeTemplate.getEndDay());

                if (meetingDate.isBefore(startingDate) || meetingDate.isEqual(startingDate)
                        || meetingDate.getMonthValue() == startingDate.getMonthValue() || !isMeeetingDateInRange) {
                    // Use next month instead
                    meetingDate = startingDate.plusMonths(1).with(WeekFields.ISO.weekOfMonth(), weekOfMonth).with(dow);
                } else {
                    return meetingDate;
                }
            break;
            default:
            break;
        }
        return meetingDate;
    }

    @SuppressWarnings("unused")
    private static LocalDate calculateNextMeetingDate(LocalDate startingDate, RangeTemplateData rangeTemplate, int dayOfWeekNumber,
            PortfolioCenterFrecuencyMeeting frecuencyMeeting) {
        LocalDate meetingDate = null;
        DayOfWeek dow = DayOfWeek.of(dayOfWeekNumber);

        if (frecuencyMeeting.isMensual()) {
            meetingDate = startingDate.with(TemporalAdjusters.firstDayOfNextMonth()).with(TemporalAdjusters.next(dow));
            boolean isMeeetingDateInRange;
            do {
                isMeeetingDateInRange = dateInRange(meetingDate.getDayOfMonth(), rangeTemplate.getStartDay(), rangeTemplate.getEndDay());
                if (!isMeeetingDateInRange) {
                    meetingDate = meetingDate.with(TemporalAdjusters.next(dow));
                }
            } while (!isMeeetingDateInRange);
        }
        return meetingDate;
    }

    @SuppressWarnings("unused")
    private static boolean dateInRange(int day, int startDay, int endDay) {
        if (day >= startDay && day <= endDay)
            return true;
        else
            return false;
    }
}
