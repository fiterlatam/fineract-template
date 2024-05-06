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
package org.apache.fineract.custom.portfolio.externalcharge.honoratio.job;

import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.service.CustomChargeHonorarioMapReadWritePlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import retrofit2.Retrofit;

@Configuration
public class CustomChargeHonorarioMapConfig {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ConfigurationDomainService configurationDomainService;

    @Autowired
    private Retrofit retrofit;
    @Autowired
    private PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private CustomChargeHonorarioMapReadWritePlatformService customChargeHonorarioMapReadWritePlatformService;

    @Bean
    protected Step customChargeHonorarioMapStep() {
        return new StepBuilder(JobName.LOAN_CUSTOM_CHARGE_HONORARIO_UPDATE.name(), jobRepository)
                .tasklet(customChargeHonorarioMapTasklet(), transactionManager).build();
    }

    @Bean
    public Job customChargeHonorarioMapJob() {
        return new JobBuilder(JobName.LOAN_CUSTOM_CHARGE_HONORARIO_UPDATE.name(), jobRepository).start(customChargeHonorarioMapStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public CustomChargeHonorarioMapTasklet customChargeHonorarioMapTasklet() {
        return new CustomChargeHonorarioMapTasklet(configurationDomainService, retrofit, commandsSourceWritePlatformService, loanRepository,
                customChargeHonorarioMapReadWritePlatformService);
    }
}
