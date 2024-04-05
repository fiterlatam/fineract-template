package org.apache.fineract.custom.portfolio.buyprocess.validator.chain.step;

import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSales;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSalesRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ChannelMessageRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.enumerator.ClientBuyProcessValidatorEnum;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessAbstractStepProcessor;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessValidationLayerProcessor;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ClientValidatorStep extends BuyProcessAbstractStepProcessor implements BuyProcessValidationLayerProcessor {

    // Define which validator this class is
    private ClientBuyProcessValidatorEnum stepProcessorEnum = ClientBuyProcessValidatorEnum.CLIENT_VALIDATOR;


    @Autowired
    private ChannelMessageRepository channelMessageRepository;

    @Autowired
    private ClientRepository clientRepository;


    @Override
    public Long getPriority() {
        return stepProcessorEnum.getColumnIndex();
    }

    @Override
    public void validateStepChain(ClientBuyProcess clientBuyProcess) {
        setSuperChannelMessageRepository(channelMessageRepository);

        // Custom validation comes here
        Optional<Client> optObject = clientRepository.findById(clientBuyProcess.getClientId());
        if (optObject.isPresent()) {

            Client currEntity = optObject.get();
            if (Boolean.FALSE.equals(currEntity.isActive())) {
                ammendErrorMessage(stepProcessorEnum, clientBuyProcess);
            }

        } else {

            ammendErrorMessage(stepProcessorEnum, clientBuyProcess);
        }
    }
}
