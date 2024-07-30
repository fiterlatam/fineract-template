package org.apache.fineract.custom.portfolio.buyprocess.validator;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class ClientBuyProsessPoinOfSalesInactive extends AbstractPlatformResourceNotFoundException {

    public ClientBuyProsessPoinOfSalesInactive(String messageCode, String message) {
        super(message, messageCode);
    }

}
