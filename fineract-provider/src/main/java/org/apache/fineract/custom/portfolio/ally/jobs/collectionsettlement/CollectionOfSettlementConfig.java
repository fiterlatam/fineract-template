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
package org.apache.fineract.custom.portfolio.ally.jobs.collectionsettlement;

import org.apache.fineract.custom.portfolio.ally.domain.AllyCollectionSettlementRepository;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensationRepository;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyRepository;
import org.apache.fineract.custom.portfolio.ally.service.AllyCollectionSettlementReadWritePlatformService;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class CollectionOfSettlementConfig {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AllyCollectionSettlementReadWritePlatformService allyCollectionSettlementReadWritePlatformService;

    @Autowired
    private AllyCollectionSettlementRepository allyCollectionSettlementRepository;

    @Autowired
    private CodeValueReadPlatformService codeValueReadPlatformService;

    @Autowired
    private WorkingDaysRepositoryWrapper daysRepositoryWrapper;

    @Autowired
    private ClientAllyRepository clientAllyRepository;

    @Autowired
    private AllyCompensationRepository allyCompensationRepository;

    @Autowired
    private CodeValueRepositoryWrapper codeValueRepositoryWrapper;

    @Bean
    public Step collectionOfSettlementStep() {
        return new StepBuilder(JobName.LIQUIDACION_DE_RECAUDOS.name(), jobRepository)
                .tasklet(collectionOfSettlementTasklet(), transactionManager).build();
    }

    @Bean
    public Job collectionOfSettlementStepJob() {
        return new JobBuilder(JobName.LIQUIDACION_DE_RECAUDOS.name(), jobRepository).start(collectionOfSettlementStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public CollectionSettlementTasklet collectionOfSettlementTasklet() {
        return new CollectionSettlementTasklet(allyCollectionSettlementReadWritePlatformService, allyCollectionSettlementRepository,
                codeValueReadPlatformService, daysRepositoryWrapper, clientAllyRepository, allyCompensationRepository,
                codeValueRepositoryWrapper);
    }

}
