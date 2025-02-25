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

import java.math.BigDecimal;
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
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDisbursementDetailsRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductType;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanDisbursementRemainingAmountEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    public static final String LOAN_ID_PARAM = "loanId";

    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanDisbursementDetailsRepository loanDisbursementDetailsRepository;
    private final LoanTransactionRepository loanTransactionRepository;

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
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(result.getLoanId(), true);

        if (loan.getLoanProduct().getName().equals(LoanProductType.CREDITO_ROTATIVO.getCode())
                || loan.getLoanProduct().getName().contains(LoanProductType.NANO_CREDITO.getCode())) {

            // Create response object
            getRequestBody(result, requestBody, loan);
        }

        return requestBody;
    }

    private void getRequestBody(CommandProcessingResult result, Map<String, Object> requestBody, Loan loan) {

        // If Available amount == 0 (totally used), call the hook
        BigDecimal disbursementAmountSum = loanDisbursementDetailsRepository.findAllByLoanId(loan.getId()).stream()
                .filter(disbursed -> Objects.nonNull(disbursed.getDisbursementDate())).map(x -> x.getPrincipal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal loanTransactionRepaymentSum = loanTransactionRepository.findAllByLoanIdAndTypeOf(loan.getId(), 2).stream()
                .filter(nR -> !nR.isReversed()).map(x -> x.getPrincipalPortion()).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal loanApprovedPrincipal = loan.getApprovedPrincipal();

        if (disbursementAmountSum.subtract(loanTransactionRepaymentSum).compareTo(loanApprovedPrincipal) >= 0) {
            requestBody.put(LOAN_ID_PARAM, result.getLoanId());
        }
    }
}
