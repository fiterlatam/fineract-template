package org.apache.fineract.custom.portfolio.buyprocess.validator.chain;

import org.apache.fineract.custom.portfolio.buyprocess.domain.Channel;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ChannelMessage;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ChannelMessageRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.enumerator.ClientBuyProcessValidatorEnum;
import org.apache.tika.utils.StringUtils;

import java.util.Objects;
import java.util.Optional;

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
        ammendErrorMessage(validatorEnum, clientBuyProcess, (Objects.isNull(clientBuyProcess.getChannelId()) ? 1L : clientBuyProcess.getChannelId()));
    }

    public void ammendErrorMessage(ClientBuyProcessValidatorEnum validatorEnum, ClientBuyProcess clientBuyProcess, Long channelId) {
        String returnMessage = StringUtils.EMPTY;

        returnMessage = getTranslatedMessage(validatorEnum,
                (Objects.isNull(channelId) ? 1L : channelId), returnMessage);

        clientBuyProcess.getErrorMessageHM().put(validatorEnum.getColumnName(), returnMessage);
    }

//    public void ammendErrorMessage(ClientBuyProcessValidatorEnum validatorEnum, ClientBuyProcess clientBuyProcess, Long channelId) {
//        String returnMessage = StringUtils.EMPTY;
//
//        returnMessage = getTranslatedMessage(validatorEnum,
//                (Objects.isNull(channelId) ? 1L : channelId), returnMessage);
//
//        clientBuyProcess.setErrorMessage(clientBuyProcess.getErrorMessage().concat(returnMessage).concat(" | ").);
//    }
}
