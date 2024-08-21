package org.apache.fineract.custom.portfolio.ally.jobs.compensationsettlement;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.service.AllyCompensationReadWritePlatformService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class CompensationOfSettlementTasklet implements Tasklet {

    AllyCompensationReadWritePlatformService allyCompensationReadWritePlatformService;

    public CompensationOfSettlementTasklet(AllyCompensationReadWritePlatformService allyCompensationReadWritePlatformService) {
        this.allyCompensationReadWritePlatformService = allyCompensationReadWritePlatformService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Compensation execute method called");
        return null;
    }
}
