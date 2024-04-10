package org.apache.fineract.custom.portfolio.buyprocess.validator.chain;

import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;

public interface BuyProcessValidationLayerProcessor {

    // Defines the execution order
    Long getPriority();

    void validateStepChain(ClientBuyProcess clientBuyProcess);

}
