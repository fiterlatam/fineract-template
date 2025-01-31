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
package org.apache.fineract.custom.portfolio.buyprocess.validator.chain.step;

import java.util.Optional;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelMessageRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.enumerator.ClientBuyProcessValidatorEnum;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessAbstractStepProcessor;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessValidationLayerProcessor;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RequestedVsProductTermValidatorStep extends BuyProcessAbstractStepProcessor implements BuyProcessValidationLayerProcessor {

    // Define which validator this class is
    private ClientBuyProcessValidatorEnum stepProcessorEnum = ClientBuyProcessValidatorEnum.TERM_VALIDATOR;

    @Autowired
    private ChannelMessageRepository channelMessageRepository;

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Override
    public Long getPriority() {
        return stepProcessorEnum.getColumnIndex();
    }

    @Override
    public void validateStepChain(ClientBuyProcess clientBuyProcess) {
        setSuperChannelMessageRepository(channelMessageRepository);

        // Custom validation comes here
        Optional<LoanProduct> entityOpt = loanProductRepository.findById(clientBuyProcess.getProductId());
        if (entityOpt.isPresent()) {

            LoanProduct entity = entityOpt.get();
            if (entity.getMaxNumberOfRepayments() != null && clientBuyProcess.getTerm() > entity.getMaxNumberOfRepayments()) {
                ammendErrorMessage(stepProcessorEnum, clientBuyProcess);
            }

        } else {

            ammendErrorMessage(stepProcessorEnum, clientBuyProcess);
        }
    }
}
