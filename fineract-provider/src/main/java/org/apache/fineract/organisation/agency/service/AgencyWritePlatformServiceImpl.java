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
package org.apache.fineract.organisation.agency.service;

import java.util.Map;
import javax.persistence.PersistenceException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.exception.NoAuthorizationException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.agency.domain.Agency;
import org.apache.fineract.organisation.agency.domain.AgencyRepositoryWrapper;
import org.apache.fineract.organisation.agency.serialization.AgencyCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.office.domain.OrganisationCurrency;
import org.apache.fineract.organisation.office.domain.OrganisationCurrencyRepositoryWrapper;
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
public class AgencyWritePlatformServiceImpl implements AgencyWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(AgencyWritePlatformServiceImpl.class);

    private final PlatformSecurityContext context;
    private final AgencyCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final AgencyRepositoryWrapper agencyRepositoryWrapper;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final CodeValueRepository codeValueRepository;
    private final OrganisationCurrencyRepositoryWrapper organisationCurrencyRepositoryWrapper;
    private final AppUserRepository appUserRepository;

    @Autowired
    public AgencyWritePlatformServiceImpl(PlatformSecurityContext context, AgencyCommandFromApiJsonDeserializer fromApiJsonDeserializer,
            AgencyRepositoryWrapper agencyRepositoryWrapper, OfficeRepositoryWrapper officeRepositoryWrapper,
            CodeValueRepository codeValueRepository, OrganisationCurrencyRepositoryWrapper organisationCurrencyRepositoryWrapper,
            AppUserRepository appUserRepository) {
        this.context = context;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.agencyRepositoryWrapper = agencyRepositoryWrapper;
        this.officeRepositoryWrapper = officeRepositoryWrapper;
        this.codeValueRepository = codeValueRepository;
        this.organisationCurrencyRepositoryWrapper = organisationCurrencyRepositoryWrapper;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    @Override
    public CommandProcessingResult createAgency(JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            Long parentId = null;
            if (command.parameterExists(AgencyConstants.AgencySupportedParameters.OFFICE_PARENT_ID.getValue())) {
                parentId = command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.OFFICE_PARENT_ID.getValue());
            }

            final Office parentOffice = validateUserPrivilegeOnOfficeAndRetrieve(currentUser, parentId);

            // Get code values for fields
            CodeValue city = null;
            final Long cityId = command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.CITY_ID.getValue());
            if (cityId != null) {
                city = codeValueRepository.getReferenceById(cityId);
            }

            CodeValue stateProvince = null;
            final Long stateId = command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.STATE_ID.getValue());
            if (stateId != null) {
                stateProvince = codeValueRepository.getReferenceById(stateId);
            }

            CodeValue country = null;
            final Long countryId = command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.COUNTRY_ID.getValue());
            if (countryId != null) {
                country = codeValueRepository.getReferenceById(countryId);
            }

            CodeValue entityCode = null;
            final Long entityCodeId = command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.ENTITY_CODE.getValue());
            if (entityCodeId != null) {
                entityCode = codeValueRepository.getReferenceById(entityCodeId);
            }

            final String currencyCode = command
                    .stringValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.CURRENCY_CODE.getValue());
            OrganisationCurrency currency = this.organisationCurrencyRepositoryWrapper.findOneWithNotFoundDetection(currencyCode);

            CodeValue agencyType = null;
            final Long agencyTypeId = command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.AGENCY_TYPE.getValue());
            if (agencyTypeId != null) {
                agencyType = codeValueRepository.getReferenceById(agencyTypeId);
            }

            CodeValue labourDayFrom = null;
            final Long labourDayFromId = command
                    .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.LABOUR_DAY_FROM.getValue());
            if (labourDayFromId != null) {
                labourDayFrom = codeValueRepository.getReferenceById(labourDayFromId);
            }

            CodeValue labourDayTo = null;
            final Long labourDayToId = command
                    .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.LABOUR_DAY_TO.getValue());
            if (labourDayToId != null) {
                labourDayTo = codeValueRepository.getReferenceById(labourDayToId);
            }

            CodeValue financialYearFrom = null;
            final Long financialYearFromId = command
                    .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.FINANCIAL_YEAR_FROM.getValue());
            if (financialYearFromId != null) {
                financialYearFrom = codeValueRepository.getReferenceById(financialYearFromId);
            }

            CodeValue financialYearTo = null;
            final Long financialYearToId = command
                    .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.FINANCIAL_YEAR_TO.getValue());
            if (financialYearToId != null) {
                financialYearTo = codeValueRepository.getReferenceById(financialYearToId);
            }

            CodeValue nonBusinessDay1 = null;
            final Long nonBusinessDay1Id = command
                    .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.NON_BUSINESS_DAY1.getValue());
            if (nonBusinessDay1Id != null) {
                nonBusinessDay1 = codeValueRepository.getReferenceById(nonBusinessDay1Id);
            }

            CodeValue nonBusinessDay2 = null;
            final Long nonBusinessDay2Id = command
                    .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.NON_BUSINESS_DAY2.getValue());
            if (nonBusinessDay2Id != null) {
                nonBusinessDay2 = codeValueRepository.getReferenceById(nonBusinessDay2Id);
            }

            CodeValue halfBusinessDay1 = null;
            final Long halfBusinessDay1Id = command
                    .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.HALF_BUSINESS_DAY1.getValue());
            if (halfBusinessDay1Id != null) {
                halfBusinessDay1 = codeValueRepository.getReferenceById(halfBusinessDay1Id);
            }

            CodeValue halfBusinessDay2 = null;
            final Long halfBusinessDay2Id = command
                    .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.HALF_BUSINESS_DAY2.getValue());
            if (halfBusinessDay2Id != null) {
                halfBusinessDay2 = codeValueRepository.getReferenceById(halfBusinessDay2Id);
            }

            final Long responsibleUserId = command
                    .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.RESPONSIBLE_USER_ID.getValue());
            AppUser responsibleUser = null;
            if (responsibleUserId != null) {
                responsibleUser = this.appUserRepository.findById(responsibleUserId)
                        .orElseThrow(() -> new UserNotFoundException(responsibleUserId));
            }

            final Agency agency = Agency.fromJson(parentOffice, city, stateProvince, country, entityCode, currency, agencyType,
                    labourDayFrom, labourDayTo, financialYearFrom, financialYearTo, nonBusinessDay1, nonBusinessDay2, halfBusinessDay1,
                    halfBusinessDay2, responsibleUser, command);

            this.agencyRepositoryWrapper.saveAndFlush(agency);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(agency.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleAgencyDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleAgencyDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateAgency(Long agencyId, JsonCommand command) {

        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            Agency agency = this.agencyRepositoryWrapper.findOneWithNotFoundDetection(agencyId);

            final Map<String, Object> changes = agency.update(command);

            Long parentId;
            if (command.parameterExists(AgencyConstants.AgencySupportedParameters.OFFICE_PARENT_ID.getValue())) {
                parentId = command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.OFFICE_PARENT_ID.getValue());

                final Office parentOffice = validateUserPrivilegeOnOfficeAndRetrieve(currentUser, parentId);
                agency.setParentOffice(parentOffice);
            }

            // Get code values for fields
            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.CITY_ID.getValue()) != 0) {
                final Long cityId = command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.CITY_ID.getValue());
                CodeValue city = codeValueRepository.getReferenceById(cityId);
                agency.setCity(city);
                changes.put(AgencyConstants.AgencySupportedParameters.CITY_ID.getValue(), cityId);
            }

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.STATE_ID.getValue()) != 0) {
                final Long stateId = command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.STATE_ID.getValue());
                CodeValue setStateProvince = codeValueRepository.getReferenceById(stateId);
                agency.setStateProvince(setStateProvince);
                changes.put(AgencyConstants.AgencySupportedParameters.STATE_ID.getValue(), stateId);
            }

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.COUNTRY_ID.getValue()) != 0) {
                final Long countryId = command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.COUNTRY_ID.getValue());
                CodeValue country = codeValueRepository.getReferenceById(countryId);
                agency.setCountry(country);
                changes.put(AgencyConstants.AgencySupportedParameters.COUNTRY_ID.getValue(), countryId);
            }

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.ENTITY_CODE.getValue()) != 0) {
                final Long entityCodeId = command
                        .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.ENTITY_CODE.getValue());
                CodeValue entityCode = codeValueRepository.getReferenceById(entityCodeId);
                agency.setEntityCode(entityCode);
                changes.put(AgencyConstants.AgencySupportedParameters.ENTITY_CODE.getValue(), entityCodeId);
            }

            final String currencyCode = command
                    .stringValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.CURRENCY_CODE.getValue());
            OrganisationCurrency currency = this.organisationCurrencyRepositoryWrapper.findOneWithNotFoundDetection(currencyCode);
            agency.setCurrencyCode(currency.getCode());

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.AGENCY_TYPE.getValue()) != 0) {
                final Long agencyTypeId = command
                        .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.AGENCY_TYPE.getValue());
                CodeValue agencyType = codeValueRepository.getReferenceById(agencyTypeId);
                agency.setAgencyType(agencyType);
                changes.put(AgencyConstants.AgencySupportedParameters.AGENCY_TYPE.getValue(), agencyTypeId);
            }

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.LABOUR_DAY_FROM.getValue()) != 0) {
                final Long labourDayFromId = command
                        .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.LABOUR_DAY_FROM.getValue());
                CodeValue labourDayFrom = codeValueRepository.getReferenceById(labourDayFromId);
                agency.setLabourDayFrom(labourDayFrom);
                changes.put(AgencyConstants.AgencySupportedParameters.LABOUR_DAY_FROM.getValue(), labourDayFromId);
            }

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.LABOUR_DAY_TO.getValue()) != 0) {
                final Long labourDayToId = command
                        .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.LABOUR_DAY_TO.getValue());
                CodeValue labourDayTo = codeValueRepository.getReferenceById(labourDayToId);
                agency.setLabourDayTo(labourDayTo);
                changes.put(AgencyConstants.AgencySupportedParameters.LABOUR_DAY_TO.getValue(), labourDayToId);
            }

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.FINANCIAL_YEAR_FROM.getValue()) != 0) {
                final Long financialYearFromId = command
                        .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.FINANCIAL_YEAR_FROM.getValue());
                CodeValue financialYearFrom = codeValueRepository.getReferenceById(financialYearFromId);
                agency.setFinancialYearFrom(financialYearFrom);
                changes.put(AgencyConstants.AgencySupportedParameters.FINANCIAL_YEAR_FROM.getValue(), financialYearFromId);
            }

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.FINANCIAL_YEAR_TO.getValue()) != 0) {
                final Long financialYearToId = command
                        .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.FINANCIAL_YEAR_TO.getValue());
                CodeValue financialYearTo = codeValueRepository.getReferenceById(financialYearToId);
                agency.setFinancialYearTo(financialYearTo);
                changes.put(AgencyConstants.AgencySupportedParameters.FINANCIAL_YEAR_TO.getValue(), financialYearToId);
            }

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.NON_BUSINESS_DAY1.getValue()) != 0) {
                final Long nonBusinessDay1Id = command
                        .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.NON_BUSINESS_DAY1.getValue());
                CodeValue nonBusinessDay1 = codeValueRepository.getReferenceById(nonBusinessDay1Id);
                agency.setNonBusinessDay1(nonBusinessDay1);
                changes.put(AgencyConstants.AgencySupportedParameters.NON_BUSINESS_DAY1.getValue(), nonBusinessDay1Id);
            }

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.NON_BUSINESS_DAY2.getValue()) != 0) {
                final Long nonBusinessDay2Id = command
                        .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.NON_BUSINESS_DAY2.getValue());
                CodeValue nonBusinessDay2 = codeValueRepository.getReferenceById(nonBusinessDay2Id);
                agency.setNonBusinessDay2(nonBusinessDay2);
                changes.put(AgencyConstants.AgencySupportedParameters.NON_BUSINESS_DAY2.getValue(), nonBusinessDay2Id);
            }

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.HALF_BUSINESS_DAY1.getValue()) != 0) {
                final Long halfBusinessDay1Id = command
                        .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.HALF_BUSINESS_DAY1.getValue());
                CodeValue halfBusinessDay1 = this.codeValueRepository.getReferenceById(halfBusinessDay1Id);
                agency.setHalfBusinessDay1(halfBusinessDay1);
                changes.put(AgencyConstants.AgencySupportedParameters.HALF_BUSINESS_DAY1.getValue(), halfBusinessDay1Id);
            }

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.HALF_BUSINESS_DAY2.getValue()) != 0) {
                final Long halfBusinessDay2Id = command
                        .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.HALF_BUSINESS_DAY2.getValue());
                CodeValue halfBusinessDay2 = this.codeValueRepository.getReferenceById(halfBusinessDay2Id);
                agency.setHalfBusinessDay2(halfBusinessDay2);
                changes.put(AgencyConstants.AgencySupportedParameters.HALF_BUSINESS_DAY2.getValue(), halfBusinessDay2Id);
            }

            if (command.longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.RESPONSIBLE_USER_ID.getValue()) != 0) {
                final Long responsibleUserId = command
                        .longValueOfParameterNamed(AgencyConstants.AgencySupportedParameters.RESPONSIBLE_USER_ID.getValue());
                AppUser responsibleUser = null;
                if (responsibleUserId != null) {
                    responsibleUser = this.appUserRepository.findById(responsibleUserId)
                            .orElseThrow(() -> new UserNotFoundException(responsibleUserId));
                    agency.setResponsibleUser(responsibleUser);
                    changes.put(AgencyConstants.AgencySupportedParameters.RESPONSIBLE_USER_ID.getValue(), responsibleUserId);
                }
            }

            this.agencyRepositoryWrapper.saveAndFlush(agency);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(agencyId) //
                    .with(changes) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleAgencyDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleAgencyDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteAgency(final Long agencyId) {
        try {

            final Agency agency = this.agencyRepositoryWrapper.findOneWithNotFoundDetection(agencyId);

            this.agencyRepositoryWrapper.delete(agency);
            return new CommandProcessingResultBuilder() //
                    .withEntityId(agency.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            LOG.error("Error occurred.", dve);
            throw new PlatformDataIntegrityException("error.msg.agency.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.", dve);
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleAgencyDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("name")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.agency.duplicate.name", "Agency with name '" + name + "' already exists",
                    "name", name);
        }

        throw new PlatformDataIntegrityException("error.msg.agency.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    /*
     * used to restrict modifying operations to office that are either the users office or lower (child) in the office
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
