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

package org.apache.fineract.portfolio.loanaccount.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.loanaccount.domain.ClasificacionConceptos;
import org.apache.fineract.portfolio.loanaccount.domain.ClasificacionConceptosRepository;
import org.apache.fineract.portfolio.loanaccount.exception.ClasificacionConceptosNotFound;
import org.apache.fineract.portfolio.loanaccount.invoice.data.ClasificacionConceptosData;
import org.apache.fineract.portfolio.loanaccount.serialization.ClasificacionConceptosCommandFromApiValidator;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ClasificacionConceptosServiceImpl implements ClasificacionConceptosService {

    private final ClasificacionConceptosRepository clasificacionConceptosRepository;
    private final ClasificacionConceptosCommandFromApiValidator fromApiValidator;

    @Override
    public List<ClasificacionConceptosData> retrieveAll() {
        List<ClasificacionConceptos> clasificacionConceptos = this.clasificacionConceptosRepository.findAll();
        return clasificacionConceptos.stream().map(ClasificacionConceptosData::new).toList();
    }

    @Override
    public ClasificacionConceptosData retrieveOne(Long id) {
        ClasificacionConceptos clasificacionConceptos = this.clasificacionConceptosRepository.findById(id)
                .orElseThrow(() -> new ClasificacionConceptosNotFound(id));
        return new ClasificacionConceptosData(clasificacionConceptos);
    }

    @Override
    public CommandProcessingResult delete(Long id) {
        ClasificacionConceptos clasificacionConceptos = this.clasificacionConceptosRepository.findById(id)
                .orElseThrow(() -> new ClasificacionConceptosNotFound(id));
        this.clasificacionConceptosRepository.delete(clasificacionConceptos);
        return CommandProcessingResult.resourceResult(id);
    }

    @Override
    public CommandProcessingResult update(Long id, JsonCommand command) {
        this.fromApiValidator.validate(command.json());
        ClasificacionConceptos clasificacionConceptos = this.clasificacionConceptosRepository.findById(id)
                .orElseThrow(() -> new ClasificacionConceptosNotFound(id));
        clasificacionConceptos.update(command);
        this.clasificacionConceptosRepository.save(clasificacionConceptos);
        return CommandProcessingResult.resourceResult(clasificacionConceptos.getId());
    }

    @Override
    public CommandProcessingResult create(JsonCommand command) {
        this.fromApiValidator.validate(command.json());
        ClasificacionConceptos clasificacionConceptos = ClasificacionConceptos.create(command);
        this.clasificacionConceptosRepository.save(clasificacionConceptos);
        return CommandProcessingResult.resourceResult(clasificacionConceptos.getId());
    }
}
