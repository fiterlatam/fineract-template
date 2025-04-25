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
package org.apache.fineract.infrastructure.dataqueries.service;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.dataqueries.events.DatatableEntryEvent;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.event.LoanApprovalContactabilityEventProcessor;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactabilityEventHandler implements DatatableEventHandler {

    private static final String DATATABLE_NAME = "Validacion Contacta";
    private static final String ENTITY_TYPE = "LOAN";
    private static final String ACTION = "NONE";

    private final LoanApprovalContactabilityEventProcessor loanApprovalContactabilityEventProcessor;
    private final PlatformSecurityContext context;

    @Override
    public String getDatatableName() {
        return DATATABLE_NAME;
    }

    @Override
    public void handle(DatatableEntryEvent event) {
        publishWebhook(event.getAppTableId());
    }

    private void publishWebhook(Long loanId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("dummyNotEmptyPayload", "dummyNotEmptyPayload");

        try {
            loanApprovalContactabilityEventProcessor.publish(payload, ENTITY_TYPE, ACTION, context.authenticatedUser(),
                    ThreadLocalContextUtil.getContext());
        } catch (SpelEvaluationException e) {
            log.info(e.getMessage());
        }
    }
}
