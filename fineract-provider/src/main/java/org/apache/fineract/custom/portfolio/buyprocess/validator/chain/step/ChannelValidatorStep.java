package org.apache.fineract.custom.portfolio.buyprocess.validator.chain.step;

import java.util.Optional;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelMessageRepository;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelRepository;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelType;
import org.apache.fineract.custom.insfrastructure.channel.domain.Channel;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.enumerator.ClientBuyProcessValidatorEnum;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessAbstractStepProcessor;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessValidationLayerProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChannelValidatorStep extends BuyProcessAbstractStepProcessor implements BuyProcessValidationLayerProcessor {

    ClientBuyProcessValidatorEnum clazzEnum = ClientBuyProcessValidatorEnum.CHANNEL_VALIDATOR;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ChannelMessageRepository channelMessageRepository;

    @Override
    public Long getPriority() {
        return clazzEnum.getColumnIndex();
    }

    @Override
    public void validateStepChain(ClientBuyProcess clientBuyProcess) {
        setSuperChannelMessageRepository(channelMessageRepository);

        String channelName = clientBuyProcess.getChannelName();

        Optional<Channel> optionalChannel = channelRepository.findByHash(channelName);

        if (optionalChannel.isPresent()) {

            Channel curr = optionalChannel.get();
            clientBuyProcess.setChannelId(curr.getId());

            // Check if channel is enabled and off type Compra/Avance
            if (Boolean.FALSE.equals(curr.getActive()) || !ChannelType.DISBURSEMENT.getValue().equals(curr.getChannelType())) {
                ammendErrorMessage(clazzEnum, clientBuyProcess, curr.getId());
            }

        } else {

            clientBuyProcess.setChannelId(1L);
            ammendErrorMessage(clazzEnum, clientBuyProcess);
        }
    }
}
