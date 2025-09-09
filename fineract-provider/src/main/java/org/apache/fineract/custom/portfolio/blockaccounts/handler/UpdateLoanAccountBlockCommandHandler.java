package org.apache.fineract.custom.portfolio.blockaccounts.handler;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.custom.portfolio.blockaccounts.service.LoanAccountBlockWritePlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@CommandType(entity = "LOAN_ACCOUNT_BLOCK", action = "UPDATE")
public class UpdateLoanAccountBlockCommandHandler implements NewCommandSourceHandler {

    private final LoanAccountBlockWritePlatformService loanAccountBlockWritePlatformService;

    @Override
    public CommandProcessingResult processCommand(JsonCommand command) {
        return loanAccountBlockWritePlatformService.updateLoanAccountBlock(command);
    }
}
