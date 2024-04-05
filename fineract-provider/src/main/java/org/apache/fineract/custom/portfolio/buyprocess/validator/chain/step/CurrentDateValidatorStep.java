package org.apache.fineract.custom.portfolio.buyprocess.validator.chain.step;

import org.apache.fineract.custom.portfolio.buyprocess.domain.ChannelMessageRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ChannelRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.enumerator.ClientBuyProcessValidatorEnum;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessAbstractStepProcessor;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessValidationLayerProcessor;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.tika.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CurrentDateValidatorStep extends BuyProcessAbstractStepProcessor implements BuyProcessValidationLayerProcessor {

    @Autowired
    private ChannelMessageRepository channelMessageRepository;


    @Override
    public Long getPriority() {
        return ClientBuyProcessValidatorEnum.CURRENT_DATE_VALIDATOR.getColumnIndex();
    }

    @Override
    public void validateStepChain(ClientBuyProcess clientBuyProcess) {
        setSuperChannelMessageRepository(channelMessageRepository);

        // Check if current date is valid
        if (Boolean.FALSE.equals(DateUtils.getLocalDateOfTenant().isEqual(clientBuyProcess.getRequestedDate()))) {

            ammendErrorMessage(ClientBuyProcessValidatorEnum.CURRENT_DATE_VALIDATOR, clientBuyProcess);
        }
    }
}
