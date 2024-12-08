package org.apache.fineract.custom.portfolio.ally.jobs.compensationsettlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.data.AllySettlementCompansationCollectionData;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllySettlementData;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensation;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensationRepository;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAlly;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyRepository;
import org.apache.fineract.custom.portfolio.ally.service.AllyCompensationReadWritePlatformService;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class CompensationOfSettlementTasklet implements Tasklet {

    private AllyCompensationReadWritePlatformService allyCompensationReadWritePlatformService;
    private AllyCompensationRepository allyCompensationRepository;
    private ClientAllyRepository allyRepository;
    private CodeValueRepository codeValueRepository;

    public CompensationOfSettlementTasklet(AllyCompensationReadWritePlatformService allyCompensationReadWritePlatformService,
            AllyCompensationRepository allyCompensationRepository, ClientAllyRepository allyRepository,
            CodeValueRepository codeValueRepository) {
        this.allyCompensationReadWritePlatformService = allyCompensationReadWritePlatformService;
        this.allyCompensationRepository = allyCompensationRepository;
        this.allyRepository = allyRepository;
        this.codeValueRepository = codeValueRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Compensation execute method called");

        List<ClientAllySettlementData> clientAllySettlementList = allyCompensationReadWritePlatformService.getClientAllySettlement();
        LocalDate startDate;
        LocalDate endDate;
        for (ClientAllySettlementData clientAllySettlementData : clientAllySettlementList) {
            LocalDate purchaseJobDate = LocalDate.parse(clientAllySettlementData.getLastClientPurchaseJobRun());
            LocalDate collectionJobDate = LocalDate.parse(clientAllySettlementData.getLastClientPurchaseJobRun());
            endDate = LocalDate.parse(clientAllySettlementData.getLastClientPurchaseJobRun());
            LocalDate purchaseDate = LocalDate.parse(clientAllySettlementData.getPurchaseDate());
            LocalDate collectionDate = LocalDate.parse(clientAllySettlementData.getCollectionDate());
            String frequency = clientAllySettlementData.getLiquidationFrequency();
            frequency = frequency.replaceAll("\\s", "");

            switch (frequency.toUpperCase()) {
                case "SEMANAL":
                    startDate = endDate.minusWeeks(1).plusDays(1);
                break;
                case "QUINCENAL":
                    startDate = endDate.minusWeeks(2).plusDays(1);

                break;
                case "MENSUAL":
                    startDate = endDate.minusMonths(1).plusDays(1);
                break;
                default:
                    startDate = endDate;
            }

            Optional<AllySettlementCompansationCollectionData> allySettlementCompansationData = allyCompensationReadWritePlatformService
                    .getCompensationSettlementByNit(clientAllySettlementData.getNit(), purchaseDate, collectionDate);

            List<Long> duplicateIds = allyCompensationRepository.findListDuplicateIdsToDeleteByNitDate();
            if (!duplicateIds.isEmpty()) {
                allyCompensationRepository.deleteAllById(duplicateIds);
            }

            if (allySettlementCompansationData.isPresent() && purchaseJobDate.isEqual(collectionJobDate)) {

                Optional<AllyCompensation> compensationCheck = allyCompensationRepository
                        .findBynitAndDate(clientAllySettlementData.getNit(), startDate, endDate);
                AllyCompensation allyCompensation = new AllyCompensation();
                if (!compensationCheck.isPresent()) {
                    Optional<ClientAlly> clientAlly = allyRepository.findById(allySettlementCompansationData.get().getClientAllyId());
                    BigDecimal percentageCommission = BigDecimal.ZERO;
                    BigDecimal vatCommissionAmount = BigDecimal.ZERO;
                    if (clientAlly.isPresent()) {
                        percentageCommission = clientAlly.get().getSettledComission().divide(BigDecimal.valueOf(100));
                    }

                    BigDecimal commissionAmount = allySettlementCompansationData.get().getPurchaseAmount();
                    if (percentageCommission.compareTo(BigDecimal.ZERO) == 1) {
                        vatCommissionAmount = commissionAmount.multiply(percentageCommission);
                    }
                    allyCompensation.setCompensationDate(purchaseDate);
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
                    allyCompensation.setComissionAmount(allySettlementCompansationData.get().getComissionAmount());
                    allyCompensation.setVaComissionAmount(vatCommissionAmount);
                    allyCompensation.setNetPurchaseAmount(allySettlementCompansationData.get().getNetPurchaseAmount());
                    allyCompensation.setNetOutstandingAmount(allySettlementCompansationData.get().getCompensationAmount());
                    allyCompensation.setNetOutstandingAmount(allySettlementCompansationData.get().getCompensationAmount());
                    allyCompensationReadWritePlatformService.create(allyCompensation);

                } else {
                    AllyCompensation exisiting = compensationCheck.get();
                    if (exisiting.getSettlementStatus() != null) {
                        Optional<ClientAlly> clientAlly = allyRepository.findById(allySettlementCompansationData.get().getClientAllyId());
                        BigDecimal percentageCommission = BigDecimal.ZERO;
                        BigDecimal vatCommissionAmount = BigDecimal.ZERO;
                        if (clientAlly.isPresent()) {
                            percentageCommission = clientAlly.get().getSettledComission().divide(BigDecimal.valueOf(100));
                        }
                        BigDecimal commissionAmount = allySettlementCompansationData.get().getPurchaseAmount();
                        if (percentageCommission.compareTo(BigDecimal.ZERO) == 1) {
                            vatCommissionAmount = commissionAmount.multiply(percentageCommission);
                        }
                        if (!exisiting.getSettlementStatus()) {
                            exisiting.setPurchaseAmount(allySettlementCompansationData.get().getPurchaseAmount());
                            exisiting.setCollectionAmount(allySettlementCompansationData.get().getCollectionAmount());
                            exisiting.setComissionAmount(allySettlementCompansationData.get().getComissionAmount());
                            exisiting.setVaComissionAmount(vatCommissionAmount);
                            exisiting.setNetPurchaseAmount(allySettlementCompansationData.get().getNetPurchaseAmount());
                            exisiting.setNetOutstandingAmount(allySettlementCompansationData.get().getCompensationAmount());
                            exisiting.setNetOutstandingAmount(allySettlementCompansationData.get().getCompensationAmount());
                            exisiting.setSettlementStatus(exisiting.getSettlementStatus());
                            allyCompensationRepository.save(exisiting);
                        }
                    } else {
                        this.getAllyCompensationCheck();
                    }
                }
            }
        }

        return RepeatStatus.FINISHED;
    }

    public void getAllyCompensationCheck() {
        List<AllyCompensation> compensations = allyCompensationRepository.findBySettlementStatus();
        for (AllyCompensation allyCompensation : compensations) {
            Optional<ClientAlly> clientAlly = allyRepository.findById(allyCompensation.getClientAllyId());

            if (clientAlly.isPresent()) {
                Optional<CodeValue> codeValue = codeValueRepository.findById(clientAlly.get().getLiquidationFrequencyCodeValueId());
                if (codeValue.isPresent()) {
                    CodeValue codeValue1 = codeValue.get();
                    String frequency = codeValue1.getLabel();
                    frequency = frequency.replaceAll("\\s", "");
                    LocalDate startDate;
                    LocalDate endDate = allyCompensation.getCompensationDate();
                    switch (frequency.toUpperCase()) {
                        case "SEMANAL":
                            startDate = endDate.minusWeeks(1).plusDays(1);
                        break;
                        case "QUINCENAL":
                            startDate = endDate.minusWeeks(2).plusDays(1);

                        break;
                        case "MENSUAL":
                            startDate = endDate.minusMonths(1).plusDays(1);
                        break;
                        default:
                            startDate = endDate;
                    }
                    Optional<AllySettlementCompansationCollectionData> allySettlementCompansationData = allyCompensationReadWritePlatformService
                            .getCompensationSettlementByNit(allyCompensation.getNit(), startDate, endDate);
                    if (allySettlementCompansationData.isPresent()) {
                        BigDecimal percentageCommission = BigDecimal.ZERO;
                        BigDecimal vatCommissionAmount = BigDecimal.ZERO;
                        if (clientAlly.isPresent()) {
                            percentageCommission = clientAlly.get().getSettledComission().divide(BigDecimal.valueOf(100));
                        }
                        if (clientAlly.get().getId() == 9) {
                            System.out.println(" " + percentageCommission);
                        }
                        BigDecimal commissionAmount = allySettlementCompansationData.get().getPurchaseAmount();
                        if (percentageCommission.compareTo(BigDecimal.ZERO) == 1) {
                            vatCommissionAmount = commissionAmount.multiply(percentageCommission);
                        }
                        allyCompensation.setStartDate(startDate);
                        allyCompensation.setEndDate(endDate);
                        allyCompensation.setComissionAmount(vatCommissionAmount);
                        allyCompensationRepository.save(allyCompensation);
                    }
                }

            }

        }
    }
}
