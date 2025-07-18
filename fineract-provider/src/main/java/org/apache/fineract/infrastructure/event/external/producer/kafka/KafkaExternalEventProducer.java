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
package org.apache.fineract.infrastructure.event.external.producer.kafka;

import static org.apache.fineract.infrastructure.core.diagnostics.performance.MeasuringUtil.measure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.event.external.exception.AcknowledgementTimeoutException;
import org.apache.fineract.infrastructure.event.external.producer.ExternalEventProducer;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(value = "fineract.events.external.producer.kafka.enabled", havingValue = "true")
public class KafkaExternalEventProducer implements ExternalEventProducer {

    private final KafkaTemplate<String, String> externalEventsKafkaTemplate;
    private final FineractProperties fineractProperties;

    @Autowired
    public KafkaExternalEventProducer(final KafkaTemplate<String, String> externalEventsKafkaTemplate,
            final FineractProperties fineractProperties) {
        this.externalEventsKafkaTemplate = externalEventsKafkaTemplate;
        this.fineractProperties = fineractProperties;
    }

    @Override
    public void sendEvents(final Map<String, List<String>> partitions) throws AcknowledgementTimeoutException {
        final FineractProperties.FineractExternalEventsProducerKafkaProperties kafkaProperties = fineractProperties.getEvents()
                .getExternal().getProducer().getKafka();
        final String topicName = kafkaProperties.getTopic().getName();
        final List<CompletableFuture<SendResult<String, String>>> sendResults = new ArrayList<>();
        measure(() -> {
            for (final Map.Entry<String, List<String>> entry : partitions.entrySet()) {
                for (final String message : entry.getValue()) {
                    final Message<String> kafkaMessage = MessageBuilder.withPayload(message).setHeader(KafkaHeaders.TOPIC, topicName)
                            .setHeader(KafkaHeaders.KEY, entry.getKey())
                            .setHeader(JobName.class.getName(), JobName.APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT.name()).build();
                    sendResults.add(externalEventsKafkaTemplate.send(kafkaMessage));
                }
            }

            try {
                final CompletableFuture<Void> allOf = CompletableFuture.allOf(sendResults.toArray(new CompletableFuture[0]));
                allOf.get(kafkaProperties.getTimeoutInSeconds(), TimeUnit.SECONDS);
            } catch (final Exception exception) {
                throw new RuntimeException("Could not send the messages", exception);
            }
        }, timeTaken -> {
            if (log.isDebugEnabled()) {
                int eventCount = partitions.values().stream().map(Collection::size).reduce(0, Integer::sum);
                int msgPerSec = (int) (((double) eventCount / timeTaken.toMillis()) * 1000);
                log.debug("Sent messages with {} msg/s", msgPerSec);
            }
        });
    }

    @Override
    public void sendEvents(final String messageJson, final JobName jobName) throws AcknowledgementTimeoutException {
        final FineractProperties.FineractExternalEventsProducerKafkaProperties kafkaProperties = fineractProperties.getEvents()
                .getExternal().getProducer().getKafka();
        final String topicName = kafkaProperties.getTopic().getName();
        final String headerKeyValue = System.currentTimeMillis() + "-" + UUID.randomUUID();
        final List<CompletableFuture<SendResult<String, String>>> sendResults = new ArrayList<>();
        measure(() -> {
            final Message<String> kafkaMessage = MessageBuilder.withPayload(messageJson).setHeader(KafkaHeaders.TOPIC, topicName)
                    .setHeader(KafkaHeaders.KEY, headerKeyValue).setHeader(JobName.class.getName(), jobName.name()).build();
            sendResults.add(externalEventsKafkaTemplate.send(kafkaMessage));
            try {
                final CompletableFuture<Void> allOf = CompletableFuture.allOf(sendResults.toArray(new CompletableFuture[0]));
                allOf.get(kafkaProperties.getTimeoutInSeconds(), TimeUnit.SECONDS);
            } catch (final Exception exception) {
                throw new RuntimeException("Could not send the messages", exception);
            }
        }, timeTaken -> {
            if (log.isDebugEnabled()) {
                final double timeInSeconds = timeTaken.toMillis() / 1000.0;
                log.debug("Sent message for job: {} with key: {}, topic: {}, time taken: {} seconds", jobName.name(), headerKeyValue,
                        topicName, timeInSeconds);
            }
        });
    }
}
