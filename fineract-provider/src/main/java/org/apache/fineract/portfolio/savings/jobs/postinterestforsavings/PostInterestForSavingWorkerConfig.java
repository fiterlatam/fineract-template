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
package org.apache.fineract.portfolio.savings.jobs.postinterestforsavings;

import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;

@Configuration
@EnableBatchIntegration
@ConditionalOnProperty(value = "fineract.mode.batch-worker-enabled", havingValue = "true")
public class PostInterestForSavingWorkerConfig {

    @Autowired
    private RemotePartitioningWorkerStepBuilderFactory stepBuilderFactory;
    @Autowired
    private StepBuilderFactory localStepBuilderFactory;
    @Autowired
    private QueueChannel inboundRequests;
    @Autowired
    private SavingsAccountReadPlatformService savingAccountReadPlatformService;
    @Autowired
    private ConfigurationDomainService configurationDomainService;
    @Autowired
    private ApplicationContext applicationContext;

    @Bean(name = "postInterestJobWorkerStep")
    public Step postInterestJobWorkerStep() {
        return stepBuilderFactory.get("PostInterestJob - WorkerStep").inputChannel(inboundRequests).flow(flow()).build();
    }

    @Bean
    public Flow flow() {
        return new FlowBuilder<Flow>("PostInterestJob - WorkerFlow").start(postInterestForSavingsWorkerStep(null)).build();
    }

    @Bean
    @StepScope
    public Step postInterestForSavingsWorkerStep(@Value("#{stepExecutionContext['partition']}") String partitionName) {
        return localStepBuilderFactory.get("PostInterestJob - PostInterestForSavingsWorkerTasklet - " + partitionName).tasklet(postInterestForSavingsWorkerTasklet()).build();
    }

    @Bean
    public Tasklet postInterestForSavingsWorkerTasklet() {
        return new PostInterestForSavingsWorkerTasklet(savingAccountReadPlatformService, configurationDomainService, applicationContext);
    }
}
