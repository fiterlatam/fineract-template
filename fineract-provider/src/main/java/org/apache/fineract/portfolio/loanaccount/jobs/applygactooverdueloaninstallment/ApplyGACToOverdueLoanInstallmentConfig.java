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
package org.apache.fineract.portfolio.loanaccount.jobs.applygactooverdueloaninstallment;

import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.config.TaskExecutorConstant;
import org.apache.fineract.infrastructure.jobs.domain.JobProcessedEntityRepository;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.service.LoanChargeWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ApplyGACToOverdueLoanInstallmentConfig {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ConfigurationDomainService configurationDomainService;

    @Autowired
    private LoanReadPlatformService loanReadPlatformService;

    @Autowired
    private JobProcessedEntityRepository jobProcessedEntityRepository;

    @Autowired
    private ChargeRepositoryWrapper chargeRepository;

    @Autowired
    private LoanChargeWritePlatformService loanChargeWritePlatformService;

    @Qualifier(TaskExecutorConstant.CONFIGURABLE_TASK_EXECUTOR_BEAN_NAME)
    private ThreadPoolTaskExecutor taskExecutor;

    @Bean
    public ApplyGACToOverdueLoanInstallmentTasklet cancelDefaultInsuranceChargesTasklet() {
        return new ApplyGACToOverdueLoanInstallmentTasklet(configurationDomainService, loanReadPlatformService,
                jobProcessedEntityRepository, chargeRepository, loanChargeWritePlatformService, taskExecutor);
    }

    @Bean
    protected Step applyGACToOverdueLoanInstallmentStep(ApplyGACToOverdueLoanInstallmentTasklet applyGACToOverdueLoanInstallmentTasklet) {
        return new StepBuilder(JobName.APPLY_GAC_TO_OVERDUE_LOAN_INSTALLMENT.name(), jobRepository)
                .tasklet(applyGACToOverdueLoanInstallmentTasklet, transactionManager).build();
    }

    @Bean
    public Job applyGACToOverdueLoanInstallmentsJob(ApplyGACToOverdueLoanInstallmentTasklet applyGACToOverdueLoanInstallmentTasklet) {
        return new JobBuilder(JobName.APPLY_GAC_TO_OVERDUE_LOAN_INSTALLMENT.name(), jobRepository)
                .start(applyGACToOverdueLoanInstallmentStep(applyGACToOverdueLoanInstallmentTasklet)).incrementer(new RunIdIncrementer())
                .build();
    }
}
