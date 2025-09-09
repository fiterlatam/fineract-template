package org.apache.fineract.custom.portfolio.blockaccounts.service;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;

public interface LoanAccountBlockWritePlatformService {

    CommandProcessingResult crateLoanAccountBlock(JsonCommand command);

    CommandProcessingResult updateLoanAccountBlock(JsonCommand command);
}
