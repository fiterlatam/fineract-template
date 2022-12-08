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

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;

@Configuration
@EnableBatchIntegration
@ConditionalOnProperty(value = "fineract.mode.batch-worker-enabled", havingValue = "true")
public class PostAccrualMultiStepPartitionJobMultithreadedRemoteWorkerConfig {

    @Autowired
    private RemotePartitioningWorkerStepBuilderFactory stepBuilderFactory;

    @Autowired
    private StepBuilderFactory localStepBuilderFactory;
    @Autowired
    private QueueChannel inboundRequests;

    @Bean(name = "postAccrualMultiStepPartitionJobWorkerStepMR")
    public Step postAccrualMultiStepPartitionJobWorkerStepMR() {
        return stepBuilderFactory.get("postAccrualMultiStepPartitionJobMR - WorkerStep").inputChannel(inboundRequests).flow(flowAMR()).build();
    }

    @Bean
    public Flow flowAMR() {
        return new FlowBuilder<Flow>("postAccrualMultiStepPartitionJobMR-postAccrualInterestFlow").start(postAccrualPartitionJobStep2MR(null)).build();
    }

    @Bean
    @StepScope
    public Step postAccrualPartitionJobStep2MR(@Value("#{stepExecutionContext['partition']}") String partitionName) {
        return localStepBuilderFactory.get("PostAccrualMultiStepPartitionJobMR - Step2 - " + partitionName).tasklet(postAccrualStep2TaskletForPartitionJobMR()).build();
    }

    @Bean
    public Tasklet postAccrualStep2TaskletForPartitionJobMR() {
        return new PostAccrualStep2TaskletForPartitionJob();
    }
}
