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

package org.apache.fineract.portfolio.loanaccount.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.event.BaseCustomWebhookEventProcessorImpl;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductType;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanCreditoRotativoFirstUseEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    public static final String LOAN_ID_PARAM = "loanId";
    public static final String MOBILE_PHONE_PARAM = "mobilePhone";
    public static final String PRODUCT_NAME_PARAM = "productName";
    public static final String EXTERNAL_ID_PARAM = "externalId";
    public static final String LOAN_AMOUNT_PARAM = "loanAmount";
    public static final String FULL_NAME_PARAM = "fullName";

    private final LoanRepositoryWrapper loanRepositoryWrapper;

    @Override
    protected List<Map<String, String>> getSupportedEvents() {
        Map<String, String> loanEvent = Map.of("entityName", "LOAN", "actionName", "DISBURSE");
        return Collections.singletonList(loanEvent);
    }

    @Override
    public Map<String, Object> transform(String entityName, String actionName, JsonCommand command, Object result) {
        if (result instanceof CommandProcessingResult successResult) {
            return generateSuccessResponse(CommandProcessingResult.fromCommandProcessingResult(successResult));
        }
        return Collections.emptyMap();
    }

    public Map<String, Object> generateSuccessResponse(CommandProcessingResult result) {

        Map<String, Object> requestBody = new HashMap<>();
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(result.getLoanId());

        // Check if product is equals to credito rotativo or Nano Credito (all variations)
        if (loan.getLoanProduct().getName().equals(LoanProductType.CREDITO_ROTATIVO.getCode())
                || loan.getLoanProduct().getName().contains(LoanProductType.NANO_CREDITO.getCode())) {

            // Check if it is the very first dibursal
            Long disubursalCounter = loan.getDisbursementDetails().stream()
                    .filter(wasDisbursed -> Objects.nonNull(wasDisbursed.getDisbursementDate()))
                    .filter(notRversed -> Boolean.FALSE.equals(notRversed.isReversed())).count();

            // Create payload
            if (disubursalCounter.compareTo(1L) == 0) {
                Client client = loan.client();

                requestBody.put(LOAN_ID_PARAM, result.getLoanId());
                requestBody.put(MOBILE_PHONE_PARAM, client.mobileNo());
                requestBody.put(PRODUCT_NAME_PARAM, loan.getLoanProduct().getName());
                if (Objects.nonNull(client.getExternalId())) {
                    requestBody.put(EXTERNAL_ID_PARAM, client.getExternalId().getValue());
                }
                requestBody.put(LOAN_AMOUNT_PARAM, loan.getApprovedPrincipal());
                requestBody.put(FULL_NAME_PARAM, client.getDisplayName());
            }
        }

        return requestBody;
    }
}
