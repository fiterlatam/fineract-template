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
package org.apache.fineract.infrastructure.sms.scheduler;

import com.google.gson.JsonObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.campaigns.masivian.data.MasivianConfigurationData;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsCampaign;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.apache.fineract.infrastructure.core.config.TaskExecutorConstant;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.gcm.service.NotificationSenderService;
import org.apache.fineract.infrastructure.sms.data.SmsMessageApiQueueResourceData;
import org.apache.fineract.infrastructure.sms.domain.SmsMessage;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageRepository;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageStatusType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Scheduled job services that send SMS messages and get delivery reports for the sent SMS messages
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsMessageScheduledJobServiceImpl implements SmsMessageScheduledJobService {

    private final SmsMessageRepository smsMessageRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final NotificationSenderService notificationSenderService;
    @Qualifier(TaskExecutorConstant.DEFAULT_TASK_EXECUTOR_BEAN_NAME)
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService;

    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    private void connectAndSendToIntermediateServer(Collection<SmsMessageApiQueueResourceData> apiQueueResourceDatas) {
        final MasivianConfigurationData masivianConfigurationData = this.externalServicesReadPlatformService.getMasivianConfiguration();
        if (masivianConfigurationData.isSmsApiEnabled()) {
            final String smsApiURL = masivianConfigurationData.getSmsApiUrl();
            final String smsApiAuthorizationToken = masivianConfigurationData.getSmsAuthorization();
            final HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            requestHeaders.setBasicAuth(smsApiAuthorizationToken);
            for (final SmsMessageApiQueueResourceData apiQueueResourceData : apiQueueResourceDatas) {
                final JsonObject requestBody = new JsonObject();
                requestBody.addProperty("To", apiQueueResourceData.getMobileNumber());
                requestBody.addProperty("text", apiQueueResourceData.getMessage());
                final HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), requestHeaders);
                try {
                    final ResponseEntity<String> response = restTemplate.exchange(smsApiURL, HttpMethod.POST, requestEntity, String.class);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("SMS sent successfully to {} | Response: {}", apiQueueResourceData.getMobileNumber(), response.getBody());
                    } else {
                        log.error("Failed to send SMS to {} | Response: {}", apiQueueResourceData.getMobileNumber(), response.getBody());
                    }
                } catch (Exception e) {
                    log.error("Failed to send SMS message: {} to mobile: {}", apiQueueResourceData.getMessage(),
                            apiQueueResourceData.getMobileNumber(), e);
                    log.error("Error occurred.", e);
                }
            }
        }
    }

    @Override
    public void sendTriggeredMessages(Map<SmsCampaign, Collection<SmsMessage>> smsDataMap) {
        try {
            if (!smsDataMap.isEmpty()) {
                List<SmsMessage> toSaveMessages = new ArrayList<>();
                List<SmsMessage> toSendNotificationMessages = new ArrayList<>();
                for (Map.Entry<SmsCampaign, Collection<SmsMessage>> entry : smsDataMap.entrySet()) {
                    Iterator<SmsMessage> smsMessageIterator = entry.getValue().iterator();
                    Collection<SmsMessageApiQueueResourceData> apiQueueResourceDatas = new ArrayList<>();
                    while (smsMessageIterator.hasNext()) {
                        SmsMessage smsMessage = smsMessageIterator.next();
                        if (smsMessage.isNotification()) {
                            smsMessage.setStatusType(SmsMessageStatusType.WAITING_FOR_DELIVERY_REPORT.getValue());
                            toSendNotificationMessages.add(smsMessage);
                        } else {
                            SmsMessageApiQueueResourceData apiQueueResourceData = SmsMessageApiQueueResourceData.instance(
                                    smsMessage.getId(), null, null, null, smsMessage.getMobileNo(), smsMessage.getMessage(),
                                    entry.getKey().getProviderId());
                            apiQueueResourceDatas.add(apiQueueResourceData);
                            smsMessage.setStatusType(SmsMessageStatusType.WAITING_FOR_DELIVERY_REPORT.getValue());
                            toSaveMessages.add(smsMessage);
                        }
                    }
                    if (!toSaveMessages.isEmpty()) {
                        this.smsMessageRepository.saveAll(toSaveMessages);
                        this.smsMessageRepository.flush();
                        this.taskExecutor.execute(new SmsTask(apiQueueResourceDatas, ThreadLocalContextUtil.getContext()));
                    }
                    if (!toSendNotificationMessages.isEmpty()) {
                        this.notificationSenderService.sendNotification(toSendNotificationMessages);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error occured.", e);
        }
    }

    @Override
    public void sendTriggeredMessage(Collection<SmsMessage> smsMessages, long providerId) {
        try {
            final MasivianConfigurationData masivianConfigurationData = this.externalServicesReadPlatformService.getMasivianConfiguration();
            if (masivianConfigurationData != null && masivianConfigurationData.isSmsApiEnabled()) {
                Collection<SmsMessageApiQueueResourceData> apiQueueResourceDatas = new ArrayList<>();
                StringBuilder request = new StringBuilder();
                for (SmsMessage smsMessage : smsMessages) {
                    SmsMessageApiQueueResourceData apiQueueResourceData = SmsMessageApiQueueResourceData.instance(smsMessage.getId(), null,
                            null, null, smsMessage.getMobileNo(), smsMessage.getMessage(), providerId);
                    apiQueueResourceDatas.add(apiQueueResourceData);
                    smsMessage.setStatusType(SmsMessageStatusType.WAITING_FOR_DELIVERY_REPORT.getValue());
                }
                this.smsMessageRepository.saveAll(smsMessages);
                request.append(SmsMessageApiQueueResourceData.toJsonString(apiQueueResourceDatas));
                log.debug("Sending triggered SMS to specific provider with request - {}", request);
                this.taskExecutor.execute(new SmsTask(apiQueueResourceDatas, ThreadLocalContextUtil.getContext()));
            }
        } catch (Exception e) {
            log.error("Error occured.", e);
        }
    }

    class SmsTask implements Runnable, ApplicationListener<ContextClosedEvent> {

        private final FineractContext context;
        private final Collection<SmsMessageApiQueueResourceData> apiQueueResourceDatas;

        SmsTask(final Collection<SmsMessageApiQueueResourceData> apiQueueResourceDatas, final FineractContext context) {
            this.context = context;
            this.apiQueueResourceDatas = apiQueueResourceDatas;
        }

        @Override
        public void run() {
            ThreadLocalContextUtil.init(context);
            connectAndSendToIntermediateServer(apiQueueResourceDatas);
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            taskExecutor.shutdown();
            log.info("Shutting down the ExecutorService");
        }
    }
}
