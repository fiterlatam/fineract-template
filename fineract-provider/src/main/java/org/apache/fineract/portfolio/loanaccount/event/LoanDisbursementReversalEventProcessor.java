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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.event.BaseCustomWebhookEventProcessorImpl;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanDisbursementReversalEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanTransactionRepository loanTransactionRepository;

    @Override
    protected String hookName() {
        return CustomHookEventProcessorEnum.fromClazz(this.getClass().getName()).getHookName();
    }

    @Override
    protected List<Map<String, String>> getSupportedEvents() {
        Map<String, String> loanEvent = Map.of("entityName", "LOAN", "actionName", "ADJUST");
        return Collections.singletonList(loanEvent);
    }

    @Override
    public Map<String, Object> transform(String entityName, String actionName, JsonCommand command, Object result) {
        if (result instanceof CommandProcessingResult successResult) {
            return generateSuccessResponse(CommandProcessingResult.fromCommandProcessingResult(successResult), true);
        }
        return Collections.emptyMap();
    }

    public Map<String, Object> generateSuccessResponse(CommandProcessingResult result, Boolean validateResourceId) {
        Map<String, Object> requestBody = new HashMap<>();
        final Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(result.getLoanId());
        final Long resourceId = result.getResourceId();

        requestBody.put("loanId", loan.getAccountNumber());
        requestBody.put("transactionType", "Reversal");
        requestBody.put("productName", loan.getLoanProduct().getName());
        requestBody.put("userId", loan.getClient().getExternalId());

        Long disbursementTransactionId = resourceId;

        Optional<LoanTransaction> ltOpt = loan.getLoanTransactions().stream() //
                .filter(LoanTransaction::isReversed) //
                .filter(t -> t.getTypeOf().equals(LoanTransactionType.DISBURSEMENT)).max(Comparator.comparing(LoanTransaction::getId));

        if (loan.isMultiDisburmentLoan()) {
            ltOpt = loan.getLoanTransactions().stream() //
                    .filter(LoanTransaction::isReversed) //
                    .filter(t -> t.getTypeOf().equals(LoanTransactionType.DISBURSEMENT)).min(Comparator.comparing(LoanTransaction::getId));
        }

        if (ltOpt.isPresent()) {
            disbursementTransactionId = ltOpt.get().getId();
        }

        if (validateResourceId) {
            final LoanTransaction loanTransaction = loanTransactionRepository.findById(resourceId)
                    .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.error.sending.hook.resource.id.is.null",
                            "Error creating hook request, resource id is null"));

            if (loanTransaction.isDisbursementWithoutReverseValidation()) {
                try {
                    requestBody.put("reversalTransactionId", disbursementTransactionId);
                    requestBody.put("reversedTransactionId", disbursementTransactionId);
                } catch (EmptyResultDataAccessException e) {
                    return requestBody;
                }

                return requestBody;
            }
        } else {
            requestBody.put("reversalTransactionId", disbursementTransactionId);
            requestBody.put("reversedTransactionId", disbursementTransactionId);
            return requestBody;
        }

        return new HashMap<>();

    }
}
