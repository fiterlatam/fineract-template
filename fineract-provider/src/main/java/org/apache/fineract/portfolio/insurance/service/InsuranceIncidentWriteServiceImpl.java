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
package org.apache.fineract.portfolio.insurance.service;

import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.portfolio.insurance.domain.InsuranceIncident;
import org.apache.fineract.portfolio.insurance.domain.InsuranceIncidentRepository;
import org.apache.fineract.portfolio.insurance.domain.InsuranceIncidentType;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsuranceIncidentWriteServiceImpl implements InsuranceIncidentWriteService {

    private final InsuranceIncidentRepository insuranceIncidentRepository;
    private final InsuranceIncidentReadService insuranceIncidentReadService;
    private static final String MANDATORY_FIELD = "isMandatory";
    private static final String VOLUNTARY_FIELD = "isVoluntary";
    private static final String INCIDENT_TYPE_FIELD = "incidentType";

    @Override
    public CommandProcessingResult createInsuranceIncident(JsonCommand command) {
        // get the name of the insurance incident
        String name = command.stringValueOfParameterNamed("name");
        // validate name cannot be null
        if (StringUtils.isBlank(name)) {
            throw new GeneralPlatformDomainRuleException("error.msg.insurance.incident.name.required",
                    "Name of the insurance incident is required", "name");
        }
        name = name.trim();
        // get value of is mandatory
        boolean isMandatory = command.booleanPrimitiveValueOfParameterNamed(MANDATORY_FIELD);
        // get value of is voluntary
        boolean isVoluntary = command.booleanPrimitiveValueOfParameterNamed(VOLUNTARY_FIELD);
        Integer incidentTypeValue = command.integerValueOfParameterNamed(INCIDENT_TYPE_FIELD);

        if (incidentTypeValue == null || incidentTypeValue.intValue() == 0) {
            throw new GeneralPlatformDomainRuleException("error.msg.insurance.incident.type.required",
                    "Insurance incident type is required");
        }

        InsuranceIncidentType incidentType = InsuranceIncidentType.fromInt(incidentTypeValue);

        // validate that at least one of isMandatory or isVoluntary is true
        if (!isMandatory && !isVoluntary) {
            throw new GeneralPlatformDomainRuleException("error.msg.insurance.incident.mandatory.or.voluntary.required",
                    "At least one of isMandatory or isVoluntary must be true", MANDATORY_FIELD, VOLUNTARY_FIELD);
        }

        // check if insurance incident with name already exists
        if (insuranceIncidentRepository.existsByNameIgnoreCase(name)) {
            throw new PlatformDataIntegrityException("error.msg.insurance.incident.duplicate.name",
                    "Insurance incident with name `" + name + "` already exists", "name", name);
        }

        if (insuranceIncidentRepository.existsByIncidentType(incidentType)) {
            throw new PlatformDataIntegrityException("error.msg.insurance.incident.duplicate.type",
                    "Insurance incident with type `" + incidentType.getCode() + "` already exists", "type", incidentType.getCode());
        }

        InsuranceIncident insuranceIncident = InsuranceIncident.instance(name, isMandatory, isVoluntary, incidentType);
        try {
            InsuranceIncident savedInsuranceIncident = insuranceIncidentRepository.save(insuranceIncident);
            return CommandProcessingResult.commandOnlyResult(savedInsuranceIncident.getId());
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleInsuranceIncidentIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException | DatabaseException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleInsuranceIncidentIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Override
    public CommandProcessingResult updateInsuranceIncident(Long incidentId, JsonCommand command) {
        InsuranceIncident insuranceIncident = insuranceIncidentReadService.retrieveInsuranceIncidentById(incidentId);

        // get the name of the insurance incident
        String name = command.stringValueOfParameterNamed("name");
        // get value of is mandatory if passed
        Boolean isMandatory = command.booleanObjectValueOfParameterNamed("isMandatory");
        // get value of is voluntary if passed
        Boolean isVoluntary = command.booleanObjectValueOfParameterNamed("isVoluntary");
        Integer incidentTypeValue = command.integerValueOfParameterNamed(INCIDENT_TYPE_FIELD);

        if (StringUtils.isNotBlank(name)) {
            if (!insuranceIncident.getName().equalsIgnoreCase(name) && insuranceIncidentRepository.existsByNameIgnoreCase(name)) {
                // check if insurance incident with name already exists
                throw new PlatformDataIntegrityException("error.msg.insurance.incident.duplicate.name",
                        "Insurance incident with name `" + name + "` already exists", "name", name);
            }
            insuranceIncident.setName(name);
        }
        if (isMandatory != null) {
            insuranceIncident.setMandatory(isMandatory);
        }
        if (isVoluntary != null) {
            insuranceIncident.setVoluntary(isVoluntary);
        }

        // validate that at least one of isMandatory or isVoluntary is true
        if (!insuranceIncident.isMandatory() && !insuranceIncident.isVoluntary()) {
            throw new GeneralPlatformDomainRuleException("error.msg.insurance.incident.mandatory.or.voluntary.required",
                    "At least one of isMandatory or isVoluntary must be true", MANDATORY_FIELD, VOLUNTARY_FIELD);
        }

        if (incidentTypeValue == null || incidentTypeValue.intValue() == 0) {
            throw new GeneralPlatformDomainRuleException("error.msg.insurance.incident.type.required",
                    "Insurance incident type is required");
        }

        InsuranceIncidentType incidentType = InsuranceIncidentType.fromInt(incidentTypeValue);
        if (!insuranceIncident.getIncidentType().equals(incidentType) && insuranceIncidentRepository.existsByIncidentType(incidentType)) {
            throw new PlatformDataIntegrityException("error.msg.insurance.incident.duplicate.type",
                    "Insurance incident with type `" + incidentType.getCode() + "` already exists", "type", incidentType.getCode());
        }

        insuranceIncident.setIncidentType(incidentType);

        InsuranceIncident savedInsuranceIncident = insuranceIncidentRepository.save(insuranceIncident);

        return CommandProcessingResult.commandOnlyResult(savedInsuranceIncident.getId());
    }

    @Override
    public CommandProcessingResult deleteInsuranceIncident(Long incidentId) {
        InsuranceIncident insuranceIncident = insuranceIncidentReadService.retrieveInsuranceIncidentById(incidentId);
        insuranceIncidentRepository.delete(insuranceIncident);
        return CommandProcessingResult.empty();
    }

    private void handleInsuranceIncidentIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        log.info("real cuase is {}", realCause.getMessage());
        if (realCause.getMessage().contains("name")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.insurance.incident.duplicate.name",
                    "Insurance incident with name `" + name + "` already exists", "name", name);
        }
        throw ErrorHandler.getMappable(dve, "error.msg.insurance.incident.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }
}
