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
package org.apache.fineract.portfolio.loanaccount.jobs.facturaelectronicamensual;

import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicMensualRepository;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleCalculationPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class FacturaElectronicaMensualConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final FacturaElectronicMensualRepository facturaElectronicMensualRepository;
    private final JdbcTemplate jdbcTemplate;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanScheduleCalculationPlatformService calculationPlatformService;

    @Autowired
    public FacturaElectronicaMensualConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            FacturaElectronicMensualRepository facturaElectronicMensualRepository, JdbcTemplate jdbcTemplate,
            LoanReadPlatformService loanReadPlatformService, LoanScheduleCalculationPlatformService calculationPlatformService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.facturaElectronicMensualRepository = facturaElectronicMensualRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.loanReadPlatformService = loanReadPlatformService;
        this.calculationPlatformService = calculationPlatformService;
    }

    @Bean
    protected Step facturaElectronicaMensualTaskletStep() {
        return new StepBuilder(JobName.FACTURA_ELECTRONICA_MENSUAL.name(), jobRepository)
                .tasklet(facturaElectronicaMensualTaskletStepTasklet(), transactionManager).build();
    }

    @Bean
    public Job facturaElectronicaMensualJob() {
        return new JobBuilder(JobName.FACTURA_ELECTRONICA_MENSUAL.name(), jobRepository).start(facturaElectronicaMensualTaskletStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public FacturaElectronicaMensualTasklet facturaElectronicaMensualTaskletStepTasklet() {
        return new FacturaElectronicaMensualTasklet(facturaElectronicMensualRepository, jdbcTemplate, loanReadPlatformService,
                calculationPlatformService);
    }
}
