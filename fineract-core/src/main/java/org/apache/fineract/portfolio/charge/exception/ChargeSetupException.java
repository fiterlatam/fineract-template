package org.apache.fineract.portfolio.charge.exception;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformDomainRuleException;
import org.apache.fineract.portfolio.charge.domain.Charge;

public class ChargeSetupException extends AbstractPlatformDomainRuleException {

    private final String validationContext;

    public ChargeSetupException(String chargeName, String validationContext) {
        super(Charge.ERROR_MESSAGE_LABEL_INCORRECT_CHARGE_SETUP, Charge.ERROR_MESSAGE_LABEL_INCORRECT_CHARGE_SETUP, chargeName);
        this.validationContext = validationContext;
    }

    public String getValidationContext() {
        return validationContext;
    }
}
