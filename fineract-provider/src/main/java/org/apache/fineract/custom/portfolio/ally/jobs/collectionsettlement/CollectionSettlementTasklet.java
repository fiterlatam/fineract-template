package org.apache.fineract.custom.portfolio.ally.jobs.collectionsettlement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyPointOfSalesCollectionData;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCollectionSettlement;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCollectionSettlementRepository;
import org.apache.fineract.custom.portfolio.ally.service.ClientAllyPointOfSalesReadWritePlatformService;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class CollectionSettlementTasklet implements Tasklet {

    private final ClientAllyPointOfSalesReadWritePlatformService clientAllyPointOfSalesReadWritePlatformService;
    private AllyCollectionSettlementRepository allyCollectionSettlementRepository;

    public CollectionSettlementTasklet(ClientAllyPointOfSalesReadWritePlatformService clientAllyPointOfSalesReadWritePlatformService,
            AllyCollectionSettlementRepository allyCollectionSettlementRepository) {
        this.clientAllyPointOfSalesReadWritePlatformService = clientAllyPointOfSalesReadWritePlatformService;
        this.allyCollectionSettlementRepository = allyCollectionSettlementRepository;
    }

    @Override
    public RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext) throws Exception {
        log.info("Collection Of Settlemet Ally execute method called");
        List<ClientAllyPointOfSalesCollectionData> collectionData = clientAllyPointOfSalesReadWritePlatformService.getCollectionData();
        for (ClientAllyPointOfSalesCollectionData data : collectionData) {
            Optional<AllyCollectionSettlement> collect = allyCollectionSettlementRepository.findByLoanId(data.getLoanId());
            if (!collect.isPresent()) {

                AllyCollectionSettlement allyCollectionSettlement = new AllyCollectionSettlement();
                LocalDate collectDate = LocalDate.parse(data.getCollectionDate());
                allyCollectionSettlement.setCollectionDate(collectDate);
                allyCollectionSettlement.setNit(data.getNit());
                allyCollectionSettlement.setClientAllyId(data.getClientAllyId());
                allyCollectionSettlement.setPointOfSalesId(data.getPointOfSalesId());
                allyCollectionSettlement.setCityId(data.getCityId());
                allyCollectionSettlement.setPrincipalAmount(data.getAmount());
                allyCollectionSettlement.setSettledComission(data.getSettledComission());
                allyCollectionSettlement.setTaxProfileId(data.getTaxId());
                allyCollectionSettlement.setLoanId(data.getLoanId());
                allyCollectionSettlement.setClientId(data.getClientId());
                allyCollectionSettlement.setChannelId(data.getChannelId());

                allyCollectionSettlementRepository.saveAndFlush(allyCollectionSettlement);
            }

        }
        return RepeatStatus.FINISHED;
    }
}
