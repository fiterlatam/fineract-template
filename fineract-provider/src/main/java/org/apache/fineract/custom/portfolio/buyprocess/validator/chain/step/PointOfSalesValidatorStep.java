package org.apache.fineract.custom.portfolio.buyprocess.validator.chain.step;

import java.util.Optional;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSales;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSalesRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ChannelMessageRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.enumerator.ClientBuyProcessValidatorEnum;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessAbstractStepProcessor;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessValidationLayerProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PointOfSalesValidatorStep extends BuyProcessAbstractStepProcessor implements BuyProcessValidationLayerProcessor {

    // Define which validator this class is
    private ClientBuyProcessValidatorEnum stepProcessorEnum = ClientBuyProcessValidatorEnum.POINT_OF_SALES_VALIDATOR;

    @Autowired
    private ChannelMessageRepository channelMessageRepository;

    @Autowired
    private ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository;

    @Override
    public Long getPriority() {
        return stepProcessorEnum.getColumnIndex();
    }

    @Override
    public void validateStepChain(ClientBuyProcess clientBuyProcess) {
        setSuperChannelMessageRepository(channelMessageRepository);

        // Custom validation comes here
        Optional<ClientAllyPointOfSales> pointOfSalesOpt = clientAllyPointOfSalesRepository.findById(clientBuyProcess.getPointOfSalesId());
        if (pointOfSalesOpt.isPresent()) {

            ClientAllyPointOfSales pointOfSales = pointOfSalesOpt.get();
            if (Boolean.FALSE.equals(pointOfSales.getBuyEnabled())) {
                ammendErrorMessage(stepProcessorEnum, clientBuyProcess);
            }

        } else {

            ammendErrorMessage(stepProcessorEnum, clientBuyProcess);
        }
    }
}
