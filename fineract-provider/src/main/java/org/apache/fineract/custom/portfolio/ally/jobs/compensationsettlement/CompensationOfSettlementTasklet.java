package org.apache.fineract.custom.portfolio.ally.jobs.compensationsettlement;

import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.data.AllySettlementCompansationCollectionData;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensation;
import org.apache.fineract.custom.portfolio.ally.service.AllyCollectionSettlementReadWritePlatformService;
import org.apache.fineract.custom.portfolio.ally.service.AllyCompensationReadWritePlatformService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class CompensationOfSettlementTasklet implements Tasklet {

    private AllyCompensationReadWritePlatformService allyCompensationReadWritePlatformService;
    private AllyCollectionSettlementReadWritePlatformService allyCollectionSettlementReadWritePlatformService;

    public CompensationOfSettlementTasklet(AllyCompensationReadWritePlatformService allyCompensationReadWritePlatformService) {
        this.allyCompensationReadWritePlatformService = allyCompensationReadWritePlatformService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Compensation execute method called");
        List<AllySettlementCompansationCollectionData> allySettlementCompansationCollectionDataList = allyCompensationReadWritePlatformService
                .getListCompensationSettlement();
        for (AllySettlementCompansationCollectionData allySettlementCompansationCollectionData : allySettlementCompansationCollectionDataList) {
            LocalDate startDate = LocalDate.parse(allySettlementCompansationCollectionData.getLastCollectionDate());
            LocalDate endDate = LocalDate.parse(allySettlementCompansationCollectionData.getLastPurchaseDate());
            if (startDate.isEqual(endDate)) {
                System.out.println("data " + allySettlementCompansationCollectionData.getNetOutstandingAmount());
                AllyCompensation allyCompensation = new AllyCompensation();
                allyCompensation.setCompensationDate(LocalDate.now());
                allyCompensation.setNit(allySettlementCompansationCollectionData.getNit());
                allyCompensation.setDateStart(startDate);
                allyCompensation.setDateEnd(endDate);
                allyCompensation.setCompanyName(allySettlementCompansationCollectionData.getCompanyName());
                allyCompensation.setBankId(allySettlementCompansationCollectionData.getBankId());
                allyCompensation.setPurchaseAmount(allySettlementCompansationCollectionData.getPurchaseAmount());
                allyCompensation.setCollectionAmount(allySettlementCompansationCollectionData.getCollectionAmount());
                allyCompensation.setComissionAmount(allySettlementCompansationCollectionData.getCollectionAmount());
                allyCompensation.setVaComissionAmount(allySettlementCompansationCollectionData.getVaComissionAmount());
                allyCompensation.setNetPurchaseAmount(allySettlementCompansationCollectionData.getPurchaceSettlementAmount());
                allyCompensation.setNetOutstandingAmount(allySettlementCompansationCollectionData.getNetOutstandingAmount());
                allyCompensationReadWritePlatformService.create(allyCompensation);
            }
        }
        return null;
    }
}
