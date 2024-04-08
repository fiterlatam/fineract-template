package org.apache.fineract.custom.portfolio.buyprocess.validator.chain.step;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ChannelMessageRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientAdditionalInformation;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientAdditionalInformationRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.enumerator.ClientBuyProcessValidatorEnum;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessAbstractStepProcessor;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessValidationLayerProcessor;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RequestedVsAvailableAmountForClientValidatorStep extends BuyProcessAbstractStepProcessor
        implements BuyProcessValidationLayerProcessor {

    // Define which validator this class is
    private ClientBuyProcessValidatorEnum stepProcessorEnum = ClientBuyProcessValidatorEnum.REQUESTED_VS_AVAILABLE_AMOUNT_CLIENT_VALIDATOR;

    @Autowired
    private ChannelMessageRepository channelMessageRepository;

    @Autowired
    private ClientAdditionalInformationRepository clientAdditionalInformationRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Override
    public Long getPriority() {
        return stepProcessorEnum.getColumnIndex();
    }

    @Override
    public void validateStepChain(ClientBuyProcess clientBuyProcess) {
        setSuperChannelMessageRepository(channelMessageRepository);

        // Custom validation comes here
        Optional<ClientAdditionalInformation> optObject = clientAdditionalInformationRepository
                .findByClientId(clientBuyProcess.getClientId());
        if (optObject.isPresent() && Objects.nonNull(optObject.get().getCupoLimit())) {

            // Check client limit
            ClientAdditionalInformation currEntity = optObject.get();
            Long allowedAmount = currEntity.getCupoLimit();

            // Get used amount
            List<Loan> loanList = loanRepository.findLoanByClientId(clientBuyProcess.getClientId());

            BigDecimal usedAmount = loanList.stream().filter(act -> act.isOpen())
                    .map(outstanding -> outstanding.getLoanSummary().getTotalOutstanding()).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal availableAmount = BigDecimal.valueOf(allowedAmount).subtract(usedAmount);

            // Compare and validate
            if (availableAmount.compareTo(clientBuyProcess.getAmount()) < 0) {
                ammendErrorMessage(stepProcessorEnum, clientBuyProcess);
            }

        } else {

            ammendErrorMessage(stepProcessorEnum, clientBuyProcess);
        }
    }
}
