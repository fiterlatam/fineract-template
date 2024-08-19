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

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.insurance.data.InsuranceIncidentData;
import org.apache.fineract.portfolio.insurance.domain.InsuranceIncident;
import org.apache.fineract.portfolio.insurance.domain.InsuranceIncidentRepository;
import org.apache.fineract.portfolio.insurance.exception.InsuranceIncidentNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InsuranceIncidentReadServiceImpl implements InsuranceIncidentReadService {

    private final InsuranceIncidentRepository insuranceIncidentRepository;

    @Override
    public Collection<InsuranceIncidentData> retrieveAllInsuranceIncidents() {
        log.info("Retrieving all insurance incidents");
        // TODO return based on offset and limit
        return insuranceIncidentRepository.findAll().stream().map(InsuranceIncident::toData).toList();
    }

    @Override
    public InsuranceIncidentData retrieveInsuranceIncident(Long incidentId) {
        return retrieveInsuranceIncidentById(incidentId).toData();
    }

    @Override
    public InsuranceIncident retrieveInsuranceIncidentById(Long incidentId) {
        try {
            return insuranceIncidentRepository.getReferenceById(incidentId);

        } catch (Exception e) {
            log.error("Error while retrieving insurance incident with id: {}", incidentId);
            throw new InsuranceIncidentNotFoundException(incidentId);
        }
    }

}
