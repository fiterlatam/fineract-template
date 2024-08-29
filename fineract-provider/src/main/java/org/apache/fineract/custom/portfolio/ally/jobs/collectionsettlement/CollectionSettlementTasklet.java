package org.apache.fineract.custom.portfolio.ally.jobs.collectionsettlement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyPointOfSalesCollectionData;
import org.apache.fineract.custom.portfolio.ally.domain.*;
import org.apache.fineract.custom.portfolio.ally.service.AllyCollectionSettlementReadWritePlatformService;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.organisation.workingdays.domain.WorkingDays;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.apache.fineract.organisation.workingdays.service.WorkingDaysUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class CollectionSettlementTasklet implements Tasklet {

    private final AllyCollectionSettlementReadWritePlatformService allyCollectionSettlementReadWritePlatformService;
    private AllyCollectionSettlementRepository allyCollectionSettlementRepository;
    private CodeValueReadPlatformService codeValueReadPlatformService;
    private final WorkingDaysRepositoryWrapper daysRepositoryWrapper;
    private final ClientAllyRepository clientAllyRepository;

    public CollectionSettlementTasklet(AllyCollectionSettlementReadWritePlatformService allyCollectionSettlementReadWritePlatformService,
            AllyCollectionSettlementRepository allyCollectionSettlementRepository,
            CodeValueReadPlatformService codeValueReadPlatformService, WorkingDaysRepositoryWrapper daysRepositoryWrapper,
            ClientAllyRepository clientAllyRepository) {
        this.allyCollectionSettlementReadWritePlatformService = allyCollectionSettlementReadWritePlatformService;
        this.allyCollectionSettlementRepository = allyCollectionSettlementRepository;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.daysRepositoryWrapper = daysRepositoryWrapper;
        this.clientAllyRepository = clientAllyRepository;

    }

    @Override
    public RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext) throws Exception {
        log.info("Liquidación de Recaudos Ally execute method called");
        List<ClientAllyPointOfSalesCollectionData> collectionData = allyCollectionSettlementReadWritePlatformService.getCollectionData();
        final WorkingDays workingDays = this.daysRepositoryWrapper.findOne();
        LocalDate now = LocalDate.now();

        for (ClientAllyPointOfSalesCollectionData data : collectionData) {
            LocalDate collectDate = LocalDate.parse(data.getCollectionDate());
            List<AllyCollectionSettlement> collect = allyCollectionSettlementRepository.findByLoanIdAndCollectionDate(data.getLoanId(),
                    collectDate);
            String freq = LiquidationFrequency.fromInt(data.getLiquidationFrequencyId().intValue()).toString();
            LocalDate period;
            boolean isEqual = true;
            if (data.getLastJobsRun() != null) {
                period = LocalDate.parse(data.getLastJobsRun());
                switch (freq) {
                    case "WEEKLY":
                        if(period.isBefore(now.minusWeeks(1))){
                            period = now;
                        }else{
                            period = period.plusWeeks(1);
                        }
                        break;
                    case "BIWEEKLY":
                        if(period.isBefore(now.minusWeeks(2))){
                            period = now;
                        }else{
                            period = period.plusWeeks(2);
                        }
                        break;
                    case "MONTHLY":
                        if(period.isBefore(now.minusMonths(1))){
                            period = now;
                        }else{
                            period = period.plusMonths(1);
                        }
                        break;
                    case "DAILY":
                        if(period.isBefore(now.minusDays(1))){
                            period = now;
                        }else{
                            period = period.plusDays(1);
                        }
                        break;
                }
            } else {
                period = now;
            }

            String worksday = workingDays.getRecurrence();
            String[] arrayworksday = worksday.split(";");
            String[] arrayweekdays = arrayworksday[2].split("BYDAY=");
            String[] arrayCount = arrayweekdays[1].split(",");
            Integer countWokringDay = arrayCount.length - 1;

            if (!WorkingDaysUtil.isWorkingDay(workingDays, period)) {
                do {
                    period = period.plusDays(1);
                } while (period.getDayOfWeek().getValue() >= countWokringDay);

            }
            isEqual = now.isEqual(period);
            if (isEqual) {
                boolean isNewCollection = false;
                if (!collect.isEmpty()) {
                    for (AllyCollectionSettlement allyCollectionSettlementdata : collect) {
                        if (collectDate != allyCollectionSettlementdata.getCollectionDate()
                                && data.getLoanId() != allyCollectionSettlementdata.getLoanId()
                                && data.getAmount() != allyCollectionSettlementdata.getCollectionAmount()) {
                            isNewCollection = true;
                        }
                    }
                } else {
                    isNewCollection = true;
                }

                if (isNewCollection) {
                    AllyCollectionSettlement allyCollectionSettlement = new AllyCollectionSettlement();
                    CodeValueData city = codeValueReadPlatformService.retrieveCodeValue(data.getCityId());
                    allyCollectionSettlement.setCollectionDate(collectDate);
                    allyCollectionSettlement.setNit(data.getNit());
                    allyCollectionSettlement.setCompanyName(data.getName());
                    allyCollectionSettlement.setClientAllyId(data.getClientAllyId());
                    allyCollectionSettlement.setPointOfSalesId(data.getPointOfSalesId());
                    allyCollectionSettlement.setPointOfSalesName(data.getPointOfSalesName());
                    allyCollectionSettlement.setCityId(data.getCityId());
                    allyCollectionSettlement.setCityName(city.getName());
                    allyCollectionSettlement.setCollectionAmount(data.getAmount());
                    allyCollectionSettlement.setTaxProfileId(data.getTaxId());
                    allyCollectionSettlement.setLoanId(data.getLoanId());
                    allyCollectionSettlement.setClientId(data.getClientId());
                    allyCollectionSettlement.setChannelId(data.getChannelId());
                    allyCollectionSettlementReadWritePlatformService.create(allyCollectionSettlement);
                }
                Optional<ClientAlly> clientAlly = clientAllyRepository.findById(data.getClientAllyId());
                if (clientAlly.isPresent()) {
                    ClientAlly clientAllyjobs = clientAlly.get();
                    clientAllyjobs.setLastJobRun(period);
                    clientAllyRepository.save(clientAllyjobs);
                }

            }
        }
        return RepeatStatus.FINISHED;
    }
}
