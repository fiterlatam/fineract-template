package org.apache.fineract.custom.portfolio.ally.jobs.purchasesettlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.domain.*;
import org.apache.fineract.custom.portfolio.ally.service.AllyCollectionSettlementReadWritePlatformService;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationProperty;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepository;
import org.apache.fineract.organisation.workingdays.domain.WorkingDays;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.apache.fineract.organisation.workingdays.service.WorkingDaysUtil;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class PurchaseOfSettlementTasklet implements Tasklet {

    private final ClientAllyRepository clientAllyRepository;
    private final WorkingDaysRepositoryWrapper workingDaysRepositoryWrapper;
    private final AllyCollectionSettlementRepository allyCollectionSettlementRepository;
    private final AllyCollectionSettlementReadWritePlatformService allyCollectionSettlementReadWritePlatformService;
    private final GlobalConfigurationRepository globalConfigurationRepository;

    public PurchaseOfSettlementTasklet(ClientAllyRepository clientAllyRepository, WorkingDaysRepositoryWrapper workingDaysRepositoryWrapper,
            AllyCollectionSettlementRepository allyCollectionSettlementRepository,
            AllyCollectionSettlementReadWritePlatformService allyCollectionSettlementReadWritePlatformService,
            GlobalConfigurationRepository globalConfigurationRepository) {
        this.clientAllyRepository = clientAllyRepository;
        this.workingDaysRepositoryWrapper = workingDaysRepositoryWrapper;
        this.allyCollectionSettlementRepository = allyCollectionSettlementRepository;
        this.allyCollectionSettlementReadWritePlatformService = allyCollectionSettlementReadWritePlatformService;
        this.globalConfigurationRepository = globalConfigurationRepository;

    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Purchase Of Settlemet Ally execute method called");
        List<ClientAlly> clientAllyList = clientAllyRepository.findAll();
        final WorkingDays workingDays = this.workingDaysRepositoryWrapper.findOne();
        LocalDate now = LocalDate.now();
        for (ClientAlly clientAlly : clientAllyList) {
            LocalDate period;
            if (clientAlly.getLastJobRunPurchase() != null) {
                period = clientAlly.getLastJobRunPurchase();
            } else {
                period = now;
            }
            String freq = LiquidationFrequency.fromInt(clientAlly.getLiquidationFrequencyCodeValueId().intValue()).toString();

            switch (freq) {
                case "WEEKLY":
                    period = period.plusWeeks(1);
                break;
                case "BIWEEKLY":
                    period = period.plusWeeks(2);
                break;
                case "MONTHLY":
                    period = period.plusMonths(1);
                break;
                case "DAILY":
                    period = period.plusDays(1);
                break;
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

            boolean isEqual = now.isEqual(period);
            if (isEqual) {
                Long clientAllyId = clientAlly.getId();
                List<AllyCollectionSettlement> collectionData = allyCollectionSettlementRepository.findByClientAllyId(clientAllyId);
                for (AllyCollectionSettlement allyCollectionSettlement : collectionData) {
                    GlobalConfigurationProperty globalConfigurationProperty = globalConfigurationRepository
                            .findOneByName("VAT-commission-percentage");
                    BigDecimal principal = allyCollectionSettlement.getPrincipalAmount();
                    BigDecimal amountComission = principal
                            .multiply(BigDecimal.valueOf(allyCollectionSettlement.getSettledComission()).divide(BigDecimal.valueOf(100)));
                    BigDecimal amountVaCommision = amountComission
                            .multiply(BigDecimal.valueOf(globalConfigurationProperty.getValue()).divide(BigDecimal.valueOf(100)));
                    BigDecimal amountToPay = principal.subtract(amountVaCommision).subtract(amountComission);
                    allyCollectionSettlement.setAmountComission(amountComission.setScale(2, BigDecimal.ROUND_HALF_EVEN));
                    allyCollectionSettlement.setAmountVaCommision(amountVaCommision.setScale(2, BigDecimal.ROUND_HALF_EVEN));
                    allyCollectionSettlement.setAmountToPay(amountToPay.setScale(2, BigDecimal.ROUND_HALF_EVEN));
                    allyCollectionSettlementReadWritePlatformService.update(allyCollectionSettlement);
                    clientAlly.setLastJobRunPurchase(period);
                    clientAllyRepository.save(clientAlly);
                }
            }

        }
        return RepeatStatus.FINISHED;
    }
}
