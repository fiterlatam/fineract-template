package org.apache.fineract.custom.portfolio.buyprocess.validator.chain.step;

import java.math.BigDecimal;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ChannelMessageRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.enumerator.ClientBuyProcessValidatorEnum;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessAbstractStepProcessor;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessValidationLayerProcessor;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MiniummAmountValidatorStep extends BuyProcessAbstractStepProcessor implements BuyProcessValidationLayerProcessor {

    // Define which validator this class is
    private ClientBuyProcessValidatorEnum stepProcessorEnum = ClientBuyProcessValidatorEnum.MINIMUM_AMOUNT_VALIDATOR;

    @Autowired
    private ChannelMessageRepository channelMessageRepository;

    @Autowired
    private ConfigurationReadPlatformService configurationReadPlatformService;

    @Override
    public Long getPriority() {
        return stepProcessorEnum.getColumnIndex();
    }

    @Override
    public void validateStepChain(ClientBuyProcess clientBuyProcess) {
        setSuperChannelMessageRepository(channelMessageRepository);

        final GlobalConfigurationPropertyData customLength = this.configurationReadPlatformService
                .retrieveGlobalConfiguration("buy-process-minimum-amount");

        Long minimumValue = customLength.getValue();

        // Custom validation comes here
        if (clientBuyProcess.getAmount().compareTo(BigDecimal.valueOf(minimumValue)) < 0) {
            ammendErrorMessage(stepProcessorEnum, clientBuyProcess);
        }
    }
}
