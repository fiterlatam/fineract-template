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
package org.apache.fineract.custom.portfolio.buyprocess.validator.chain;

import java.util.Objects;
import java.util.Optional;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelMessage;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelMessageRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.enumerator.ClientBuyProcessValidatorEnum;
import org.apache.tika.utils.StringUtils;

public abstract class BuyProcessAbstractStepProcessor implements BuyProcessValidationLayerProcessor {

    protected ChannelMessageRepository superChannelMessageRepository = null;

    public void setSuperChannelMessageRepository(ChannelMessageRepository superChannelMessageRepository) {
        this.superChannelMessageRepository = superChannelMessageRepository;
    }

    public String getTranslatedMessage(ClientBuyProcessValidatorEnum validatorEnum, Long channelId, String returnMessage) {

        // Check if there is a specific message for this error and channel
        Optional<ChannelMessage> customMessage = superChannelMessageRepository.getChannelMessage(channelId, validatorEnum.getColumnIndex());
        if (customMessage.isPresent()) {

            returnMessage = customMessage.get().getMessage();

        } else {

            // If no specific message, use the generic one
            Optional<ChannelMessage> generalMessage = superChannelMessageRepository.getChannelMessage(1L, validatorEnum.getColumnIndex());
            if (generalMessage.isPresent()) {
                returnMessage = generalMessage.get().getMessage();
            }

        }
        return returnMessage;
    }

    public void ammendErrorMessage(ClientBuyProcessValidatorEnum validatorEnum, ClientBuyProcess clientBuyProcess) {
        ammendErrorMessage(validatorEnum, clientBuyProcess,
                (Objects.isNull(clientBuyProcess.getChannelId()) ? 1L : clientBuyProcess.getChannelId()));
    }

    public void ammendErrorMessage(ClientBuyProcessValidatorEnum validatorEnum, ClientBuyProcess clientBuyProcess, Long channelId) {
        String returnMessage = StringUtils.EMPTY;

        returnMessage = getTranslatedMessage(validatorEnum, (Objects.isNull(channelId) ? 1L : channelId), returnMessage);

        clientBuyProcess.getErrorMessageHM().put(validatorEnum.getColumnName(), returnMessage);
    }
}
