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
package org.apache.fineract.portfolio.interestrates.service;

import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.interestrates.domain.InterestRate;
import org.apache.fineract.portfolio.interestrates.domain.InterestRateHistory;
import org.apache.fineract.portfolio.interestrates.domain.InterestRateHistoryRepository;
import org.apache.fineract.portfolio.interestrates.domain.InterestRateRepository;
import org.apache.fineract.portfolio.interestrates.exception.InterestRateException;
import org.apache.fineract.portfolio.interestrates.serialization.InterestRateDataValidator;
import org.apache.fineract.portfolio.loanproduct.data.MaximumCreditRateConfigurationData;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class InterestRateWritePlatformServiceImpl implements InterestRateWritePlatformService {

    private final InterestRateDataValidator fromApiJsonDeserializer;
    private final InterestRateRepository interestRateRepository;
    private final InterestRateHistoryRepository interestRateHistoryRepository;
    private final PlatformSecurityContext context;
    private final LoanProductReadPlatformService loanProductReadPlatformService;

    @Transactional
    @Override
    public CommandProcessingResult createInterestRate(final JsonCommand command) {
        try {
            final AppUser appUser = this.context.authenticatedUser();
            this.fromApiJsonDeserializer.validateForCreate(command.json());
            final MaximumCreditRateConfigurationData maximumCreditRateConfigurationData = this.loanProductReadPlatformService
                    .retrieveMaximumCreditRateConfigurationData();
            final BigDecimal annualNominalRate = maximumCreditRateConfigurationData.getAnnualNominalRate();
            if (command.hasParameter("currentRate")
                    && command.bigDecimalValueOfParameterNamed("currentRate").compareTo(annualNominalRate) > 0) {
                throw new InterestRateException(command.bigDecimalValueOfParameterNamed("currentRate"), annualNominalRate);
            }
            final InterestRate interestRate = InterestRate.createNew(command);
            interestRate.setCreatedBy(appUser.getId());
            interestRate.setCreatedDate(DateUtils.getAuditOffsetDateTime());
            interestRate.setLastModifiedBy(appUser.getId());
            interestRate.setLastModifiedDate(DateUtils.getAuditOffsetDateTime());
            this.interestRateRepository.saveAndFlush(interestRate);
            final InterestRateHistory interestRateHistory = InterestRateHistory.createNew(interestRate);
            interestRateHistoryRepository.saveAndFlush(interestRateHistory);
            return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(interestRate.getId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateInterestRate(final Long interestRateId, final JsonCommand command) {
        try {
            final AppUser appUser = this.context.authenticatedUser();
            final InterestRate interestRate = this.interestRateRepository.findById(interestRateId)
                    .orElseThrow(() -> new InterestRateException(interestRateId));
            this.fromApiJsonDeserializer.validateForUpdate(command.json());
            final MaximumCreditRateConfigurationData maximumCreditRateConfigurationData = this.loanProductReadPlatformService
                    .retrieveMaximumCreditRateConfigurationData();
            final BigDecimal annualNominalRate = maximumCreditRateConfigurationData.getAnnualNominalRate();
            if (command.hasParameter("currentRate")
                    && command.bigDecimalValueOfParameterNamed("currentRate").compareTo(annualNominalRate) > 0) {
                throw new InterestRateException(command.bigDecimalValueOfParameterNamed("currentRate"), annualNominalRate);
            }
            final Map<String, Object> changes = interestRate.update(command);
            interestRate.setLastModifiedBy(appUser.getId());
            interestRate.setLastModifiedDate(DateUtils.getAuditOffsetDateTime());
            if (!changes.isEmpty()) {
                this.interestRateRepository.saveAndFlush(interestRate);
                final InterestRateHistory interestRateHistory = InterestRateHistory.createNew(interestRate);
                interestRateHistoryRepository.saveAndFlush(interestRateHistory);
            }
            return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(command.entityId()).with(changes)
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("unq_name")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.interest.rate.duplicate.name",
                    "Interest Rate with name `" + name + "` already exists", "name", name);
        }
        log.error("Error occured.", dve);
        throw ErrorHandler.getMappable(dve, "error.msg.interest.rate.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }
}
