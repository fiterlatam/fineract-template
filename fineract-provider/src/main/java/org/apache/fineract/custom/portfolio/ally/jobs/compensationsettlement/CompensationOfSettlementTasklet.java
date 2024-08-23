package org.apache.fineract.custom.portfolio.ally.jobs.compensationsettlement;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.data.AllySettlementCompansationCollectionData;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllySettlementData;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensation;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensationRepository;
import org.apache.fineract.custom.portfolio.ally.service.AllyCollectionSettlementReadWritePlatformService;
import org.apache.fineract.custom.portfolio.ally.service.AllyCompensationReadWritePlatformService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
public class CompensationOfSettlementTasklet implements Tasklet {

    private AllyCompensationReadWritePlatformService allyCompensationReadWritePlatformService;
    private AllyCompensationRepository allyCompensationRepository;

    public CompensationOfSettlementTasklet(AllyCompensationReadWritePlatformService allyCompensationReadWritePlatformService, AllyCompensationRepository allyCompensationRepository) {
        this.allyCompensationReadWritePlatformService = allyCompensationReadWritePlatformService;
        this.allyCompensationRepository = allyCompensationRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Compensation execute method called");

        List<ClientAllySettlementData> clientAllySettlementList = allyCompensationReadWritePlatformService.getClientAllySettlement();
        for (ClientAllySettlementData clientAllySettlementData : clientAllySettlementList){
            LocalDate startDate = LocalDate.parse(clientAllySettlementData.getPurchaseDate());
            LocalDate endDate = LocalDate.parse(clientAllySettlementData.getCollectionDate());
            Optional<AllySettlementCompansationCollectionData> allySettlementCompansationData = allyCompensationReadWritePlatformService.getCompensationSettlementByNit(clientAllySettlementData.getNit(), clientAllySettlementData.getPurchaseDate(), clientAllySettlementData.getCollectionDate());
            Optional<AllyCompensation> compensationCheck = allyCompensationRepository.findBynitAndDate(clientAllySettlementData.getNit(), startDate, endDate);
               if(!compensationCheck.isPresent() && allySettlementCompansationData.isPresent()) {
                   AllyCompensation allyCompensation = new AllyCompensation();
                   allyCompensation.setCompensationDate(LocalDate.now());
                   allyCompensation.setStartDate(startDate);
                   allyCompensation.setEndDate(endDate);
                   allyCompensation.setNit(allySettlementCompansationData.get().getNit());
                   allyCompensation.setClientAllyId(allySettlementCompansationData.get().getClientAllyId());
                   allyCompensation.setCompanyName(allySettlementCompansationData.get().getCompanyName());
                   allyCompensation.setBankName(allySettlementCompansationData.get().getBankName());
                   allyCompensation.setAccontType(allySettlementCompansationData.get().getAccountType());
                   allyCompensation.setAccountNumber(allySettlementCompansationData.get().getAccountNumber());
                   allyCompensation.setPurchaseAmount(allySettlementCompansationData.get().getPurchaseAmount());
                   allyCompensation.setCollectionAmount(allySettlementCompansationData.get().getCollectionAmount());
                   allyCompensation.setComissionAmount(allySettlementCompansationData.get().getCollectionAmount());
                   allyCompensation.setVaComissionAmount(allySettlementCompansationData.get().getVaComissionAmount());
                   allyCompensation.setNetPurchaseAmount(allySettlementCompansationData.get().getNetPurchaseAmount());
                   allyCompensation.setNetOutstandingAmount(allySettlementCompansationData.get().getCompensationAmount());
                   allyCompensation.setNetOutstandingAmount(allySettlementCompansationData.get().getCompensationAmount());
                   allyCompensation.setSettlementStatus(false);
                   allyCompensationReadWritePlatformService.create(allyCompensation);
               }

        }
       
        return RepeatStatus.FINISHED;
    }
}
