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
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.event.core.MessagingEvent;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;

@Configuration
@EnableBatchIntegration
@ConditionalOnProperty(value = "fineract.mode.batch-worker-enabled", havingValue = "true")
public class PostAccrualMultiStepPartitionJobWorkerConfig {

    @Autowired
    private RemotePartitioningWorkerStepBuilderFactory stepBuilderFactory;

    @Autowired
    private StepBuilderFactory localStepBuilderFactory;

    /*
     * Configure inbound flow (requests coming from the master)
     */
    @Bean
    public QueueChannel inboundRequests() {
        return new QueueChannel();
    }

    @Bean
    public IntegrationFlow inboundFlow() {
        return IntegrationFlows.from(eventListener()) //
                .channel(inboundRequests()) //
                .get(); //
    }

    @Bean
    public ApplicationEventListeningMessageProducer eventListener() {
        ApplicationEventListeningMessageProducer producer = new ApplicationEventListeningMessageProducer();
        producer.setEventTypes(MessagingEvent.class);
        return producer;
    }

    @Bean(name = "postAccrualMultiStepPartitionJobWorkerStep")
    public Step postAccrualMultiStepPartitionJobWorkerStep() {
        return stepBuilderFactory.get("postAccrualMultiStepPartitionJob - WorkerStep").inputChannel(inboundRequests()).flow(flowA()).build();
    }

    @Bean
    public Flow flowA() {
        return new FlowBuilder<Flow>("postAccrualMultiStepPartitionJob-postAccrualInterestFlow").start(postAccrualPartitionJobStep2(null)).build();
    }

    @Bean
    @StepScope
    public Step postAccrualPartitionJobStep2(@Value("#{stepExecutionContext['partition']}") String partitionName) {
        return localStepBuilderFactory.get("PostAccrualMultiStepPartitionJob - Step2 - " + partitionName).tasklet(postAccrualStep2TaskletForPartitionJob()).build();
    }

    @Bean
    public Tasklet postAccrualStep2TaskletForPartitionJob() {
        return new PostAccrualStep2TaskletForPartitionJob();
    }
}
