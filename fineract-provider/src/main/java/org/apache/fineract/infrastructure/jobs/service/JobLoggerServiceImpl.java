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
package org.apache.fineract.infrastructure.jobs.service;

import org.apache.fineract.infrastructure.jobs.domain.JobProcessedEntity;
import org.apache.fineract.infrastructure.jobs.domain.JobProcessedEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobLoggerServiceImpl implements JobLoggerService {

    @Autowired
    private JobProcessedEntityRepository jobProcessedEntityRepository;

    // Esta é a chave: O Spring inicia uma NOVA transação.
    // Se a transação chamadora falhar, esta aqui já terá commitado.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void logProcessedEntity(JobProcessedEntity dbLogger) {
        jobProcessedEntityRepository.save(dbLogger);
        // O commit da nova transação ocorre imediatamente após este método retornar
    }
}
