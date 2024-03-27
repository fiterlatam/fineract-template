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
package org.apache.fineract.useradministration.jobs;

import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.useradministration.service.AppUserWritePlatformService;
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

@Configuration
public class ReactivateAppUsersConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AppUserWritePlatformService appUserWritePlatformService;

    @Autowired
    public ReactivateAppUsersConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            AppUserWritePlatformService appUserWritePlatformService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.appUserWritePlatformService = appUserWritePlatformService;
    }

    @Bean
    protected Step reactivateAppUsersStep() {
        return new StepBuilder(JobName.REACTIVATE_BLOCKED_APP_USERS.name(), jobRepository)
                .tasklet(reactivateAppUsersTasklet(), transactionManager).build();
    }

    @Bean
    public Job reactivateAppUsersJob() {
        return new JobBuilder(JobName.REACTIVATE_BLOCKED_APP_USERS.name(), jobRepository).start(reactivateAppUsersStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public ReactivateAppUserTasklet reactivateAppUsersTasklet() {
        return new ReactivateAppUserTasklet(appUserWritePlatformService);
    }
}
