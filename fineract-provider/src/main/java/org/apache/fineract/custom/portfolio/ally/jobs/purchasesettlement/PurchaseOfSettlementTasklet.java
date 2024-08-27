package org.apache.fineract.custom.portfolio.ally.jobs.purchasesettlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyPointOfSalesCollectionData;
import org.apache.fineract.custom.portfolio.ally.domain.*;
import org.apache.fineract.custom.portfolio.ally.service.AllyPurchaseSettlementReadWritePlatformService;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
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
    private final AllyPurchaseSettlementRepository allyPurchaseSettlementRepository;
    private final AllyPurchaseSettlementReadWritePlatformService allyPurchaseSettlementReadWritePlatformService;
    private final GlobalConfigurationRepository globalConfigurationRepository;
    private CodeValueReadPlatformService codeValueReadPlatformService;

    public PurchaseOfSettlementTasklet(ClientAllyRepository clientAllyRepository, WorkingDaysRepositoryWrapper workingDaysRepositoryWrapper,
            AllyPurchaseSettlementRepository allyPurchaseSettlementRepository,
            AllyPurchaseSettlementReadWritePlatformService allyPurchaseSettlementReadWritePlatformService,
            GlobalConfigurationRepository globalConfigurationRepository, CodeValueReadPlatformService codeValueReadPlatformService) {
        this.clientAllyRepository = clientAllyRepository;
        this.workingDaysRepositoryWrapper = workingDaysRepositoryWrapper;
        this.allyPurchaseSettlementRepository = allyPurchaseSettlementRepository;
        this.allyPurchaseSettlementReadWritePlatformService = allyPurchaseSettlementReadWritePlatformService;
        this.globalConfigurationRepository = globalConfigurationRepository;
        this.codeValueReadPlatformService = codeValueReadPlatformService;

    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Liquidación de compras execute method called");
        List<ClientAlly> clientAllyList = clientAllyRepository.findAll();
        final WorkingDays workingDays = this.workingDaysRepositoryWrapper.findOne();
        GlobalConfigurationProperty globalConfigurationProperty = globalConfigurationRepository.findOneByName("IVA Por comision");
        LocalDate now = LocalDate.now();

        for (ClientAlly clientAlly : clientAllyList) {
            String freq = LiquidationFrequency.fromInt(clientAlly.getLiquidationFrequencyCodeValueId().intValue()).toString();
            LocalDate period;
            boolean isEqual = true;
            if (clientAlly.getLastJobRunPurchase() != null) {
                period = clientAlly.getLastJobRunPurchase();
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
                Long clientAllyId = clientAlly.getId();
                List<ClientAllyPointOfSalesCollectionData> collectionDatas = allyPurchaseSettlementReadWritePlatformService
                        .getPurchaseDataByClientAllyId(clientAllyId);

                for (ClientAllyPointOfSalesCollectionData collectionData : collectionDatas) {
                    Optional<AllyPurchaseSettlement> purchase = allyPurchaseSettlementRepository.findByLoanId(collectionData.getLoanId());
                    if (!purchase.isPresent()) {
                        AllyPurchaseSettlement allyPurchaseSettlement = new AllyPurchaseSettlement();
                        LocalDate collectDate = LocalDate.parse(collectionData.getCollectionDate());
                        CodeValueData city = codeValueReadPlatformService.retrieveCodeValue(collectionData.getCityId());
                        allyPurchaseSettlement.setPurchaseDate(collectDate);
                        allyPurchaseSettlement.setNit(collectionData.getNit());
                        allyPurchaseSettlement.setCompanyName(collectionData.getName());
                        allyPurchaseSettlement.setClientAllyId(collectionData.getClientAllyId());
                        allyPurchaseSettlement.setPointOfSalesId(collectionData.getPointOfSalesId());
                        allyPurchaseSettlement.setPointOfSalesName(collectionData.getPointOfSalesName());
                        allyPurchaseSettlement.setCityId(collectionData.getCityId());
                        allyPurchaseSettlement.setCityName(city.getName());
                        allyPurchaseSettlement.setBuyAmount(collectionData.getAmount());
                        allyPurchaseSettlement.setSettledComission(collectionData.getSettledComission());
                        allyPurchaseSettlement.setTaxProfileId(collectionData.getTaxId());
                        allyPurchaseSettlement.setLoanId(collectionData.getLoanId());
                        allyPurchaseSettlement.setClientId(collectionData.getClientId());
                        allyPurchaseSettlement.setChannelId(collectionData.getChannelId());

                        BigDecimal principal = collectionData.getAmount();
                        BigDecimal amountComission = principal
                                .multiply(BigDecimal.valueOf(collectionData.getSettledComission()).divide(BigDecimal.valueOf(100)));
                        BigDecimal amountVaCommision = amountComission
                                .multiply(BigDecimal.valueOf(globalConfigurationProperty.getValue()).divide(BigDecimal.valueOf(100)));
                        BigDecimal amountToPay = principal.subtract(amountVaCommision).subtract(amountComission);

                        allyPurchaseSettlement.setAmountComission(amountComission.setScale(2, BigDecimal.ROUND_HALF_EVEN));
                        allyPurchaseSettlement.setAmountVaCommision(amountVaCommision.setScale(2, BigDecimal.ROUND_HALF_EVEN));
                        allyPurchaseSettlement.setAmountToPay(amountToPay.setScale(2, BigDecimal.ROUND_HALF_EVEN));
                        boolean status = false;if (collectionData.getLoanStatusId() == 600) {
                            status = true;
                        }
                        allyPurchaseSettlement.setSettlementStatus(status);
                        allyPurchaseSettlementReadWritePlatformService.create(allyPurchaseSettlement);
                    } else {
                        if (purchase.get().getSettlementStatus() == false && collectionData.getLoanStatusId() == 600) {
                            AllyPurchaseSettlement allypurchase = purchase.get();
                            allypurchase.setSettlementStatus(true);
                            allyPurchaseSettlementReadWritePlatformService.update(allypurchase);
                        }

                    }
                }
                clientAlly.setLastJobRunPurchase(period);
                clientAllyRepository.save(clientAlly);
            }

        }
        return RepeatStatus.FINISHED;
    }
}
