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
package org.apache.fineract.organisation.portfolio.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.PersistenceException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.exception.NoAuthorizationException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.portfolio.domain.Portfolio;
import org.apache.fineract.organisation.portfolio.domain.PortfolioRepositoryWrapper;
import org.apache.fineract.organisation.portfolio.serialization.PortfolioCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.portfolioCenter.service.PortfolioCenterWritePlatformService;
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
public class PortfolioWritePlatformServiceImpl implements PortfolioWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(PortfolioWritePlatformServiceImpl.class);

    private final PlatformSecurityContext context;
    private final PortfolioCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final PortfolioRepositoryWrapper portfolioRepositoryWrapper;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final AppUserRepository appUserRepository;
    private final PortfolioCenterWritePlatformService portfolioCenterWritePlatformService;

    @Autowired
    public PortfolioWritePlatformServiceImpl(PlatformSecurityContext context,
            PortfolioCommandFromApiJsonDeserializer fromApiJsonDeserializer, PortfolioRepositoryWrapper portfolioRepositoryWrapper,
            OfficeRepositoryWrapper officeRepositoryWrapper, AppUserRepository appUserRepository,
            PortfolioCenterWritePlatformService portfolioCenterWritePlatformService) {
        this.context = context;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.portfolioRepositoryWrapper = portfolioRepositoryWrapper;
        this.officeRepositoryWrapper = officeRepositoryWrapper;
        this.appUserRepository = appUserRepository;
        this.portfolioCenterWritePlatformService = portfolioCenterWritePlatformService;
    }

    @Transactional
    @Override
    public CommandProcessingResult createPortfolio(JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            Long parentId = null;
            if (command.parameterExists(PortfolioConstants.PortfolioSupportedParameters.OFFICE_PARENT_ID.getValue())) {
                parentId = command.longValueOfParameterNamed(PortfolioConstants.PortfolioSupportedParameters.OFFICE_PARENT_ID.getValue());
            }

            final Office parentOffice = validateUserPrivilegeOnOfficeAndRetrieve(currentUser, parentId);

            final Long responsibleUserId = command
                    .longValueOfParameterNamed(PortfolioConstants.PortfolioSupportedParameters.RESPONSIBLE_USER_ID.getValue());
            AppUser responsibleUser = null;
            if (responsibleUserId != null) {
                responsibleUser = this.appUserRepository.findById(responsibleUserId)
                        .orElseThrow(() -> new UserNotFoundException(responsibleUserId));
            }

            final Portfolio portfolio = Portfolio.fromJson(parentOffice, responsibleUser, command);

            saveAndFlushPortfolioWithDataIntegrityViolationChecks(portfolio);

            // generate all centers for the portfolio
            portfolioCenterWritePlatformService.generateAllCentersByPortfolio(portfolio);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(portfolio.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handlePortfolioDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handlePortfolioDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updatePortfolio(Long portfolioId, JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            Portfolio portfolio = this.portfolioRepositoryWrapper.findOneWithNotFoundDetection(portfolioId);

            final Map<String, Object> changes = portfolio.update(command);

            Long parentId;
            if (command.parameterExists(PortfolioConstants.PortfolioSupportedParameters.OFFICE_PARENT_ID.getValue())) {
                parentId = command.longValueOfParameterNamed(PortfolioConstants.PortfolioSupportedParameters.OFFICE_PARENT_ID.getValue());

                final Office parentOffice = validateUserPrivilegeOnOfficeAndRetrieve(currentUser, parentId);
                portfolio.setParentOffice(parentOffice);
            }

            if (command.longValueOfParameterNamed(PortfolioConstants.PortfolioSupportedParameters.RESPONSIBLE_USER_ID.getValue()) != 0) {
                final Long responsibleUserId = command
                        .longValueOfParameterNamed(PortfolioConstants.PortfolioSupportedParameters.RESPONSIBLE_USER_ID.getValue());
                AppUser responsibleUser = null;
                if (responsibleUserId != null) {
                    responsibleUser = this.appUserRepository.findById(responsibleUserId)
                            .orElseThrow(() -> new UserNotFoundException(responsibleUserId));
                    portfolio.setResponsibleUser(responsibleUser);
                    changes.put(PortfolioConstants.PortfolioSupportedParameters.RESPONSIBLE_USER_ID.getValue(), responsibleUserId);
                }
            }

            this.portfolioRepositoryWrapper.saveAndFlush(portfolio);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(portfolioId) //
                    .with(changes) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handlePortfolioDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handlePortfolioDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult deletePortfolio(Long portfolioId) {
        try {

            final Portfolio portfolio = this.portfolioRepositoryWrapper.findOneWithNotFoundDetection(portfolioId);

            this.portfolioRepositoryWrapper.delete(portfolio);
            return new CommandProcessingResultBuilder() //
                    .withEntityId(portfolio.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            LOG.error("Error occurred.", dve);
            throw new PlatformDataIntegrityException("error.msg.portfolio.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.", dve);
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handlePortfolioDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("name")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.portfolio.duplicate.name",
                    "Portfolio with name '" + name + "' already exists", "name", name);
        }

        throw new PlatformDataIntegrityException("error.msg.portfolio.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private void saveAndFlushPortfolioWithDataIntegrityViolationChecks(final Portfolio portfolio) {
        try {
            this.portfolioRepositoryWrapper.saveAndFlush(portfolio);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("portfolio.name");
            if (realCause.getMessage().toLowerCase().contains("name")) {
                baseDataValidator.reset().parameter("name").failWithCode("value.must.be.unique");
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors, e);
            }
        }
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
