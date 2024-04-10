package org.apache.fineract.custom.portfolio.buyprocess.validator.chain.step;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSales;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSalesRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ChannelMessageRepository;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.enumerator.ClientBuyProcessValidatorEnum;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessAbstractStepProcessor;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessValidationLayerProcessor;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RequestedVsAvailableAmountForAllyValidatorStep extends BuyProcessAbstractStepProcessor
        implements BuyProcessValidationLayerProcessor {

    // Define which validator this class is
    private ClientBuyProcessValidatorEnum stepProcessorEnum = ClientBuyProcessValidatorEnum.REQUESTED_VS_AVAILABLE_AMOUNT_ALLY_VALIDATOR;
    private ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository;
    private ChannelMessageRepository channelMessageRepository;

    private final JdbcTemplate jdbcTemplate;

    public RequestedVsAvailableAmountForAllyValidatorStep(JdbcTemplate jdbcTemplate, ChannelMessageRepository channelMessageRepository,
            ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.channelMessageRepository = channelMessageRepository;
        this.clientAllyPointOfSalesRepository = clientAllyPointOfSalesRepository;
    }

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
        Optional<ClientAllyPointOfSales> optObject = clientAllyPointOfSalesRepository
                .findAllyByPointOfSaleId(clientBuyProcess.getPointOfSalesId());
        if (optObject.isPresent() && Objects.nonNull(optObject.get().getClientAlly().getCupoMaxSell())) {

            // Check Ally limit
            ClientAllyPointOfSales currEntity = optObject.get();
            Long allowedAmount = Long.valueOf(currEntity.getClientAlly().getCupoMaxSell());

            // Get used amount
            BigDecimal usedAmount = BigDecimal.ZERO;
            StringBuilder sqlBuilder = new StringBuilder();

            sqlBuilder.append("select ");
            sqlBuilder.append("     SUM(ml.total_outstanding_derived) as totalOutstandingDerived ");
            sqlBuilder.append("from  ");
            sqlBuilder.append("     custom.c_client_ally_point_of_sales ccaposForAllyPOSList ");
            sqlBuilder.append("join custom.c_client_buy_process buyProcessByAlly ");
            sqlBuilder.append("	    on buyProcessByAlly.point_if_sales_id = ccaposForAllyPOSList.id ");
            sqlBuilder.append("join public.m_loan ml ");
            sqlBuilder.append("	    on ml.id = buyProcessByAlly.loan_id ");
            sqlBuilder.append("where ");
            sqlBuilder.append("		buyProcessByAlly.point_if_sales_id in ( ");
            sqlBuilder.append("			select ");
            sqlBuilder.append("				ccbp.point_if_sales_id ");
            sqlBuilder.append("			from ");
            sqlBuilder.append("				custom.c_client_buy_process ccbp ");
            sqlBuilder.append("			join custom.c_client_ally_point_of_sales ccaposForAlly ");
            sqlBuilder.append("				on ccaposForAlly.id  = ccbp.point_if_sales_id ");
            sqlBuilder.append("			where ccaposForAlly.client_ally_id = ? ");
            sqlBuilder.append("             and ccbp.status = 200 ");
            sqlBuilder.append("		) ");
            sqlBuilder.append("and ccaposForAllyPOSList.client_ally_id = ? ");
            sqlBuilder.append("and ml.loan_status_id = 300 ");
            sqlBuilder.append("and buyProcessByAlly.status = 200 ");

            usedAmount = this.jdbcTemplate.queryForObject(sqlBuilder.toString(), BigDecimal.class, currEntity.getAllyId(),
                    currEntity.getAllyId());
            ;
            if (Objects.isNull(usedAmount)) {
                usedAmount = BigDecimal.ZERO;
            }

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
