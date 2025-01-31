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
package org.apache.fineract.custom.portfolio.ally.jobs.compensationsettlement;

import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensationRepository;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyRepository;
import org.apache.fineract.custom.portfolio.ally.service.AllyCompensationReadWritePlatformService;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.apache.fineract.infrastructure.jobs.service.JobName;
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
public class CompensationOfSettlementConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    AllyCompensationReadWritePlatformService allyCompensationReadWritePlatformService;

    @Autowired
    AllyCompensationRepository allyCompensationRepository;

    @Autowired
    ClientAllyRepository allyRepository;

    @Autowired
    CodeValueRepository codeValueRepository;

    @Bean
    public Step CompensationOfSettlementStep() {

        return new StepBuilder(JobName.COMPENSATION.name(), jobRepository).tasklet(compensationOfSettlementTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Job CompensationOfSettlementJob() {

        return new JobBuilder(JobName.COMPENSATION.name(), jobRepository).start(CompensationOfSettlementStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public CompensationOfSettlementTasklet compensationOfSettlementTasklet() {
        return new CompensationOfSettlementTasklet(allyCompensationReadWritePlatformService, allyCompensationRepository, allyRepository,
                codeValueRepository);
    }
}
