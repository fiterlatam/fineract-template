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

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.springbatch.SpringBatchJobConstants;
import org.apache.fineract.infrastructure.springbatch.messagehandler.conditions.kafka.KafkaWorkerCondition;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepositoryWrapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Conditional(KafkaWorkerCondition.class)
public class KafkaRemoteMessageListener {

    private static final String KAFKA_PROCESSING_TIME_MESSAGE = "Processing Apache Kafka message for Job name : {} and took {} seconds";

    private final LoanWritePlatformService loanWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final AppUserRepositoryWrapper appUserRepositoryWrapper;

    @Autowired
    public KafkaRemoteMessageListener(final LoanWritePlatformService loanWritePlatformService, final FromJsonHelper fromApiJsonHelper,
            final AppUserRepositoryWrapper appUserRepositoryWrapper) {
        this.loanWritePlatformService = loanWritePlatformService;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.appUserRepositoryWrapper = appUserRepositoryWrapper;
    }

    @KafkaListener(topics = "fineract-scheduler-jobs", groupId = "fineract-scheduler-jobs")
    public void onMessage(final ConsumerRecord<String, String> consumerRecord, final Acknowledgment acknowledgment) throws Exception {
        final String messageJson = consumerRecord.value();
        final String key = consumerRecord.key();
        final String topic = consumerRecord.topic();
        final long offset = consumerRecord.offset();
        final int partition = consumerRecord.partition();
        final Header jobNameHeader = consumerRecord.headers().lastHeader(SpringBatchJobConstants.KAFKA_FINERACT_JOB_ID_KEY);
        final String fineractContextJson = consumerRecord.headers().lastHeader(SpringBatchJobConstants.KAFKA_FINERACT_CONTEXT_KEY) == null
                ? null
                : new String(consumerRecord.headers().lastHeader(SpringBatchJobConstants.KAFKA_FINERACT_CONTEXT_KEY).value());
        this.initializeApplicationContext(fineractContextJson);
        final String jobNameHeaderValue = jobNameHeader == null ? null : new String(jobNameHeader.value());
        log.info("Received Apache Kafka message with key: {}, topic: {}, offset: {}, partition: {}, job name: {}", key, topic, offset,
                partition, jobNameHeaderValue);
        try {
            this.processKafkaMessage(messageJson, jobNameHeaderValue);
            log.info("Processed Apache Kafka message for job: {}", jobNameHeaderValue);
        } catch (Exception e) {
            log.error("Exception while processing Apache Kafka message", e);
        }
        acknowledgment.acknowledge();
        log.debug("Message was acknowledged {}", acknowledgment);
    }

    private void initializeApplicationContext(final String fineractContextJson) throws JsonProcessingException {
        if (fineractContextJson != null && !fineractContextJson.isEmpty()) {
            final FineractContext fineractContext = this.fromApiJsonHelper.fromJsonToPojo(fineractContextJson, FineractContext.class);
            ThreadLocalContextUtil.init(fineractContext);
            final AppUser user = this.appUserRepositoryWrapper.fetchSystemUser();
            final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(user,
                    user.getPassword(), user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        } else {
            log.warn("No Fineract context found in Kafka message headers, using default context");
            throw new IllegalArgumentException("Fineract context is required to process Kafka messages");
        }
    }

    public void processKafkaMessage(final String messageJson, final String jobNameHeaderValue) throws Exception {
        final JobName jobName = JobName.fromString(jobNameHeaderValue);
        final long startTime = System.currentTimeMillis();
        switch (jobName) {
            case RECALCULATE_LOAN_INTEREST_AFTER_MAXIMUM_LEGAL_RATE_CHANGE:
                log.info("Processing Apache Kafka message for Job name : {} ", jobNameHeaderValue);
                this.loanWritePlatformService.maximumLegalRateKafkaMessageHandler(messageJson);
                final long maximumLegalRateJobEndTime = System.currentTimeMillis();
                log.info(KAFKA_PROCESSING_TIME_MESSAGE, jobNameHeaderValue, (maximumLegalRateJobEndTime - startTime) / 1000);
            break;
            case APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT:
                final long penaltyJobEndTime = System.currentTimeMillis();
                log.info(KAFKA_PROCESSING_TIME_MESSAGE, jobNameHeaderValue, (penaltyJobEndTime - startTime) / 1000);
            break;
            case DAILY_LOAN_ACCRUAL:
                final long dailyAccrualJobEndTime = System.currentTimeMillis();
                log.info(KAFKA_PROCESSING_TIME_MESSAGE, jobNameHeaderValue, (dailyAccrualJobEndTime - startTime) / 1000.0);
            break;
            case INSTALLMENT_LOAN_CHARGE_ACCRUAL:
                final long installmentChargeJobEndTime = System.currentTimeMillis();
                log.info(KAFKA_PROCESSING_TIME_MESSAGE, jobNameHeaderValue, (installmentChargeJobEndTime - startTime) / 1000.00);
            break;
            case SEND_ASYNCHRONOUS_EVENTS:
                final long eventsJobFinishTime = System.currentTimeMillis();
                log.info(KAFKA_PROCESSING_TIME_MESSAGE, jobNameHeaderValue, (eventsJobFinishTime - startTime) / 1000.000);
            break;
            default:
                log.warn("Cannot process Apache Kafka message for unknown/unsupported job name: {}", jobNameHeaderValue);
        }
    }
}
