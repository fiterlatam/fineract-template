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

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.portfolio.loanproductparameterization.data.LoanProductParameterizationData;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterization;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterizationRepository;
import org.apache.fineract.portfolio.loanproductparameterization.exception.LoanProductParameterizationNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanProductParameterizationReadServiceImpl implements LoanProductParameterizationReadService {

    private final LoanProductParameterizationRepository productParameterizationRepository;

    @Override
    public Collection<LoanProductParameterizationData> retrieveAllProductParameterizationList() {
        return productParameterizationRepository.findAll().stream().map(LoanProductParameterization::toData).toList();
    }

    @Override
    public LoanProductParameterizationData retrieveProductParameterization(Long parameterId) {

        try {
            return productParameterizationRepository.getReferenceById(parameterId).toData();
        } catch (Exception e) {
            throw new LoanProductParameterizationNotFoundException(parameterId);
        }
    }
}
