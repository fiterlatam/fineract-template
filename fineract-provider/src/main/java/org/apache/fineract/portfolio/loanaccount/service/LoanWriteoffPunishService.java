package org.apache.fineract.portfolio.loanaccount.service;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;

public interface LoanWriteoffPunishService {

    public CommandProcessingResult writeOffPunishLoan(final Long loanId, JsonCommand command);
}
