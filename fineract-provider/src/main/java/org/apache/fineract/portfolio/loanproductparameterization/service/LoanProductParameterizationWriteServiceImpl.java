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
package org.apache.fineract.portfolio.loanproductparameterization.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterization;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterizationRepository;
import org.apache.fineract.portfolio.loanproductparameterization.exception.LoanProductParameterizationNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanProductParameterizationWriteServiceImpl implements LoanProductParameterizationWriteService {

    private final LoanProductParameterizationRepository productParameterizationRepository;

    @Override
    public CommandProcessingResult createProductParameterization(JsonCommand command) {
        LoanProductParameterization productParameterization = LoanProductParameterization.create(command);
        LoanProductParameterization savedProductParameterization = productParameterizationRepository.save(productParameterization);

        return CommandProcessingResult.commandOnlyResult(savedProductParameterization.getId());
    }

    @Override
    public CommandProcessingResult updateProductParameterization(Long parameterId, JsonCommand command) {
        LoanProductParameterization productParameterization = findProductParameterization(parameterId);
        productParameterization.update(command);
        productParameterizationRepository.save(productParameterization);

        return CommandProcessingResult.empty();
    }

    @Override
    public CommandProcessingResult deleteProductParameterization(Long parameterId) {
        LoanProductParameterization productParameterization = findProductParameterization(parameterId);
        productParameterizationRepository.delete(productParameterization);
        return CommandProcessingResult.empty();
    }

    private LoanProductParameterization findProductParameterization(Long parameterId) {
        try {
            return productParameterizationRepository.findById(parameterId)
                    .orElseThrow(() -> new LoanProductParameterizationNotFoundException(parameterId));
        } catch (LoanProductParameterizationNotFoundException e) {
            log.error("LoanProductParameterizationNotFoundException: {}", e.getMessage());
            throw new LoanProductParameterizationNotFoundException(parameterId);
        }
    }
}
