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

    @Override
    public void publish(Map<String, Object> transformedPayload, String entityName, String actionName, AppUser user,
            FineractContext fineractContext) {
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
