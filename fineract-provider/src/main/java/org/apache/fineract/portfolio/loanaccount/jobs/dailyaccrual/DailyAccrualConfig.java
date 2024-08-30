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
package org.apache.fineract.portfolio.loanaccount.jobs.dailyaccrual;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class DailyAccrualConfig {

    private final JobRepository jobRepository;

    private final PlatformTransactionManager transactionManager;

    @Bean
    protected Step runDailyLoanAccrualStep(DailyAccrualTasklet dailyAccrualTasklet) {
        return new StepBuilder(JobName.DAILY_LOAN_ACCRUAL.name(), jobRepository).tasklet(dailyAccrualTasklet, transactionManager).build();
    }

    @Bean
    public Job runDailyLoanAccrualJob(DailyAccrualTasklet dailyAccrualTasklet) {
        return new JobBuilder(JobName.DAILY_LOAN_ACCRUAL.name(), jobRepository).start(runDailyLoanAccrualStep(dailyAccrualTasklet))
                .incrementer(new RunIdIncrementer()).build();
    }
}
