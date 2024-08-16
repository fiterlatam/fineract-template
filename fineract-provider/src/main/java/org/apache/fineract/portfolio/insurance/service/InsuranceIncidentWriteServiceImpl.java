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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.insurance.domain.InsuranceIncident;
import org.apache.fineract.portfolio.insurance.domain.InsuranceIncidentRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsuranceIncidentWriteServiceImpl implements InsuranceIncidentWriteService {

    private final InsuranceIncidentRepository insuranceIncidentRepository;
    private final InsuranceIncidentReadService insuranceIncidentReadService;

    @Override
    public CommandProcessingResult createInsuranceIncident(JsonCommand command) {
        // get the name of the insurance incident
        String name = command.stringValueOfParameterNamed("name");
        // get value of is mandatory
        boolean isMandatory = command.booleanPrimitiveValueOfParameterNamed("isMandatory");
        // get value of is voluntary
        boolean isVoluntary = command.booleanPrimitiveValueOfParameterNamed("isVoluntary");

        InsuranceIncident insuranceIncident = InsuranceIncident.instance(name, isMandatory, isVoluntary);
        InsuranceIncident savedInsuranceIncident = insuranceIncidentRepository.save(insuranceIncident);

        return CommandProcessingResult.commandOnlyResult(savedInsuranceIncident.getId());
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

        if (StringUtils.isNotBlank(name)) {
            insuranceIncident.setName(name);
        }
        if (isMandatory != null) {
            insuranceIncident.setMandatory(isMandatory);
        }
        if (isVoluntary != null) {
            insuranceIncident.setVoluntary(isVoluntary);
        }

        InsuranceIncident savedInsuranceIncident = insuranceIncidentRepository.save(insuranceIncident);

        return CommandProcessingResult.commandOnlyResult(savedInsuranceIncident.getId());
    }

    @Override
    public CommandProcessingResult deleteInsuranceIncident(Long incidentId) {
        InsuranceIncident insuranceIncident = insuranceIncidentReadService.retrieveInsuranceIncidentById(incidentId);
        insuranceIncidentRepository.delete(insuranceIncident);
        return CommandProcessingResult.empty();
    }
}
