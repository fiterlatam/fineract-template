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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings(value = "RV_EXCEPTION_NOT_THROWN", justification = "False positive")
class KafkaExternalEventProducerTest {

    public static final String TOPIC_NAME = "unit-test";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private FromJsonHelper fromJsonHelper;

    @Mock
    private PlatformSecurityContext platformSecurityContext;

    @Mock
    private SendResult<String, String> sendResult1;

    @Mock
    private SendResult<String, String> sendResult2;

    @Mock
    private SendResult<String, String> sendResult3;

    private static final String FIRST = "first";
    private static final String SECOND = "second";
    private static final String THIRD = "third";

    @Test
    public void testSendOK() {
        // given
        KafkaExternalEventProducer underTest = new KafkaExternalEventProducer(kafkaTemplate, createProperties(), fromJsonHelper,
                platformSecurityContext);
        Mockito.when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(sendResult1));

        // when
        Map<String, List<String>> strMap = Map.of("1", List.of(FIRST, SECOND), "2", List.of(THIRD));
        underTest.sendEvents(strMap);

        // then
        Mockito.verify(kafkaTemplate, times(3)).send(any(Message.class));
        Mockito.verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    public void testSendOneFails() {
        // given
        KafkaExternalEventProducer underTest = new KafkaExternalEventProducer(kafkaTemplate, createProperties(), fromJsonHelper,
                platformSecurityContext);
        Mockito.when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(sendResult1))
                .thenReturn(CompletableFuture.completedFuture(sendResult2))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka error")));

        // when
        Map<String, List<String>> strMap = Map.of("1", List.of(FIRST, SECOND), "2", List.of(THIRD));
        Assertions.assertThrows(RuntimeException.class, () -> underTest.sendEvents(strMap));

        // then
        Mockito.verify(kafkaTemplate, times(3)).send(any(Message.class));
        Mockito.verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    public void testTimeOut() {
        // given
        KafkaExternalEventProducer underTest = new KafkaExternalEventProducer(kafkaTemplate, createProperties(), fromJsonHelper,
                platformSecurityContext);
        Mockito.when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(sendResult1))
                .thenReturn(CompletableFuture.completedFuture(sendResult2)).thenReturn(new CompletableFuture<>());

        // when
        Map<String, List<String>> strMap = Map.of("1", List.of(FIRST, SECOND), "2", List.of(THIRD));
        Assertions.assertThrows(RuntimeException.class, () -> underTest.sendEvents(strMap));

        // then
        Mockito.verify(kafkaTemplate, times(3)).send(any(Message.class));
        Mockito.verifyNoMoreInteractions(kafkaTemplate);
    }

    @NotNull
    private static FineractProperties createProperties() {
        FineractProperties props = new FineractProperties();

        FineractProperties.FineractEventsProperties fineractEventsProperties = new FineractProperties.FineractEventsProperties();
        props.setEvents(fineractEventsProperties);

        FineractProperties.FineractExternalEventsProperties externalEventsProperties = new FineractProperties.FineractExternalEventsProperties();
        fineractEventsProperties.setExternal(externalEventsProperties);

        FineractProperties.FineractExternalEventsProducerProperties producer = new FineractProperties.FineractExternalEventsProducerProperties();
        externalEventsProperties.setProducer(producer);

        FineractProperties.FineractExternalEventsProducerKafkaProperties kafkaProperties = new FineractProperties.FineractExternalEventsProducerKafkaProperties();
        producer.setKafka(kafkaProperties);

        FineractProperties.KafkaTopicProperties kafkaTopicProperties = new FineractProperties.KafkaTopicProperties();
        kafkaProperties.setTopic(kafkaTopicProperties);
        kafkaProperties.setTimeoutInSeconds(1);

        kafkaTopicProperties.setName(TOPIC_NAME);
        return props;
    }
}
