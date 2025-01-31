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
package org.apache.fineract.commands.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.hooks.event.HookEvent;
import org.apache.fineract.infrastructure.hooks.event.HookEventSource;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public abstract class BaseCustomWebhookEventProcessorImpl implements CustomWebhookEventProcessor {

    private ApplicationContext applicationContext;
    private ToApiJsonSerializer<CommandProcessingResult> toApiResultJsonSerializer;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Autowired
    public void setToApiResultJsonSerializer(ToApiJsonSerializer<CommandProcessingResult> toApiResultJsonSerializer) {
        this.toApiResultJsonSerializer = toApiResultJsonSerializer;
    }

    @SuppressWarnings("all")
    @Override
    public void publish(Map<String, Object> transformedPayload, String entityName, String actionName, AppUser user,
            FineractContext fineractContext) {
        if (transformedPayload.isEmpty()) {
            log.warn("Skipping webhook event publishing for {}.{} as the transformed payload is empty", entityName, actionName);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String serializedPayload = toApiResultJsonSerializer.serialize(transformedPayload);
                HookEventSource source = new HookEventSource(entityName, actionName);
                HookEvent event = new HookEvent(source, serializedPayload, user, fineractContext);

                applicationContext.publishEvent(event);
            } catch (Exception e) {
                log.error("Error publishing webhook event for {}.{}", entityName, actionName, e);
            }
        });
    }

    @Override
    public boolean supports(String entityName, String actionName) {

        return getSupportedEvents().stream()
                .anyMatch(event -> event.get("entityName").equals(entityName) && event.get("actionName").equals(actionName));
    }

    protected abstract List<Map<String, String>> getSupportedEvents();

    // we need a method to check is the event is supported
    // we need a method to publish the event
}
