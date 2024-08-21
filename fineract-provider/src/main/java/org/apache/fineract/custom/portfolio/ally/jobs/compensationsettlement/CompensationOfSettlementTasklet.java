package org.apache.fineract.custom.portfolio.ally.jobs.compensationsettlement;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCollectionSettlement;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCollectionSettlementRepository;
import org.apache.fineract.custom.portfolio.ally.domain.AllyPurchaseSettlementRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class CompensationOfSettlementTasklet implements Tasklet {

    private final AllyPurchaseSettlementRepository allyPurchaseSettlementRepository;
    private final AllyCollectionSettlementRepository allyCollectionSettlementRepository;

    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Compensation execute method called");
        return null;
    }
}
