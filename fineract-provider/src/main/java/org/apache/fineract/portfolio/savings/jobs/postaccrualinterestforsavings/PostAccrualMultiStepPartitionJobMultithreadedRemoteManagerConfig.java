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
package org.apache.fineract.portfolio.savings.jobs.postaccrualinterestforsavings;

import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningManagerStepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;

import java.util.List;

@Configuration
@EnableBatchIntegration
@ConditionalOnProperty(value = "fineract.mode.batch-manager-enabled", havingValue = "true")
public class PostAccrualMultiStepPartitionJobMultithreadedRemoteManagerConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    @Autowired
    private StepBuilderFactory localStepBuilderFactory;
    @Autowired
    private RemotePartitioningManagerStepBuilderFactory stepBuilderFactory;
    @Autowired
    private DirectChannel outboundRequests;

    @Bean
    public Step postAccrualPartitionJobStep1MR() {
        return localStepBuilderFactory.get("PostAccrualMultiStepPartitionJobMR - Step1").tasklet(postAccrualStep1TaskletForPartitionJobMR()).build();
    }

    @Bean
    @JobScope
    public Tasklet postAccrualStep1TaskletForPartitionJobMR() {
        return new PostAccrualStep1TaskletForPartitionJob();
    }

    @Bean
    @JobScope
    public PostAccrualInterestForSavingPartitioner partitionerAAMR(@Value("#{jobExecutionContext['activeSavingAccountIds']}") List<Long> savingAccountIds) {
        return new PostAccrualInterestForSavingPartitioner(savingAccountIds);
    }

    @Bean
    public Step postAccrualPartitionJobMasterStepMR() {
        return stepBuilderFactory.get("PostAccrualMultiStepPartitionJobMR - MasterStep")
                .partitioner("postAccrualMultiStepPartitionJobWorkerStepMR", partitionerAAMR(null)).gridSize(3).outputChannel(outboundRequests).build();
    }

    @Bean
    public Job postAccrualMultiStepJob() {
        return jobBuilderFactory.get(JobName.APPLY_ANNUAL_FEE_FOR_SAVINGS.name())
                .start(postAccrualPartitionJobStep1MR())
                .next(postAccrualPartitionJobMasterStepMR())
                .incrementer(new RunIdIncrementer()).build();
    }
}
