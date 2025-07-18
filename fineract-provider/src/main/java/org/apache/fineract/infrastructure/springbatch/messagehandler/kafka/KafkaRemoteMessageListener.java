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
package org.apache.fineract.infrastructure.springbatch.messagehandler.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.springbatch.messagehandler.conditions.kafka.KafkaWorkerCondition;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Conditional(KafkaWorkerCondition.class)
public class KafkaRemoteMessageListener {

    final LoanWritePlatformService loanWritePlatformService;

    @Autowired
    public KafkaRemoteMessageListener(LoanWritePlatformService loanWritePlatformService) {
        this.loanWritePlatformService = loanWritePlatformService;
    }

    @KafkaListener(topics = "${fineract.remote-job-message-handler.kafka.topic.name}", groupId = "${fineract.remote-job-message-handler.kafka.consumer.group-id}")
    public void onMessage(final ConsumerRecord<String, String> consumerRecord, final Acknowledgment acknowledgment) {
        try {
            final String messageJson = consumerRecord.value();
            final String key = consumerRecord.key();
            final String topic = consumerRecord.topic();
            final long offset = consumerRecord.offset();
            final int partition = consumerRecord.partition();
            final Header jobNameHeader = consumerRecord.headers().lastHeader(JobName.class.getName());
            final String jobNameHeaderValue = jobNameHeader == null ? null : new String(jobNameHeader.value());
            log.info("Received Apache Kafka message with key: {}, topic: {}, offset: {}, partition: {}, job name: {}", key, topic, offset,
                    partition, jobNameHeaderValue);
            try {
                this.processKafkaMessage(messageJson, jobNameHeaderValue);
                log.info("Processed Apache Kafka message for job: {}", jobNameHeaderValue);
            } catch (final Exception e) {
                log.error("Error occur while processing Apache Kafka Messages", e);
            }
        } catch (Exception e) {
            log.error("Exception while processing Apache Kafka message", e);
        }
        acknowledgment.acknowledge();
        log.debug("Message was acknowledged {}", acknowledgment);
    }

    public void processKafkaMessage(final String messageJson, final String jobNameHeaderValue) throws Exception {
        final JobName jobName = JobName.fromString(jobNameHeaderValue);
        switch (jobName) {
            case RECALCULATE_LOAN_INTEREST_AFTER_MAXIMUM_LEGAL_RATE_CHANGE:
                final long startTime = System.currentTimeMillis();
                log.info("Processing Apache Kafka message for Job name : {} ", jobNameHeaderValue);
                this.loanWritePlatformService.maximumLegalRateKafkaMessageHandler(messageJson);
                final long endTime = System.currentTimeMillis();
                log.info("Processed Apache Kafka message for Job name : {} and took {} seconds", jobNameHeaderValue,
                        (endTime - startTime) / 1000);
            break;
            case APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT:
                log.info("Processing Apache Kafka message for Job name : {}", jobNameHeaderValue);
            break;
            case DAILY_LOAN_ACCRUAL:
                log.info("Processing Apache Kafka message for Job name : {}", jobNameHeaderValue);
            break;
            case INSTALLMENT_LOAN_CHARGE_ACCRUAL:
                log.info("Processing Apache Kafka message for Job name : {}", jobNameHeaderValue);
            break;
            case SEND_ASYNCHRONOUS_EVENTS:
                log.info("Processing Apache Kafka message for Job name : {}", jobNameHeaderValue);
            break;
            default:
                log.warn("Cannot process Apache Kafka message for unknown job name: {}", jobNameHeaderValue);
        }
    }
}
