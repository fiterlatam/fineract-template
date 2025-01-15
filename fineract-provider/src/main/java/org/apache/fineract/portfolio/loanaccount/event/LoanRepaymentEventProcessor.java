/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.loanaccount.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.event.BaseCustomWebhookEventProcessorImpl;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.springframework.stereotype.Component;

@Component()
@Slf4j
public class LoanRepaymentEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    @Override
    protected List<Map<String, String>> getSupportedEvents() {
        Map<String, String> loanEvent = Map.of("entityName", "LOAN", "actionName", "REPAYMENT");
        return Collections.singletonList(loanEvent);
    }

    @Override
    public Map<String, Object> transform(String entityName, String actionName, JsonCommand command, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("loanId", "000123");
        response.put("amount", "1.234,56");
        response.put("arrearDue", "234,50");
        response.put("paymentDate", "2024-01-15");
        response.put("document", "123456789");
        response.put("documentType", "CC");
        response.put("fullname", "John Doe");
        response.put("loanAmount", "5.000,00");
        response.put("mobilePhone", "+1234567890");
        response.put("principalBalance", "3.765,44");
        response.put("totalDue", "4.000,00");
        response.put("externalId", "EXT123");

        return response;
    }
}
