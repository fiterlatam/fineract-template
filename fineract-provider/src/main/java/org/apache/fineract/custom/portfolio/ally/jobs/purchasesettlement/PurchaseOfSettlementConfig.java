package org.apache.fineract.custom.portfolio.ally.jobs.purchasesettlement;

import org.apache.fineract.custom.portfolio.ally.domain.AllyPurchaseSettlementRepository;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyRepository;
import org.apache.fineract.custom.portfolio.ally.service.AllyPurchaseSettlementReadWritePlatformService;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepository;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class PurchaseOfSettlementConfig {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ClientAllyRepository clientAllyRepository;

    @Autowired
    private WorkingDaysRepositoryWrapper workingDaysRepositoryWrapper;

    @Autowired
    private AllyPurchaseSettlementRepository allyPurchaseSettlementRepository;

    @Autowired
    private AllyPurchaseSettlementReadWritePlatformService allyPurchaseSettlementReadWritePlatformService;

    @Autowired
    private GlobalConfigurationRepository globalConfigurationRepository;

    @Autowired
    private CodeValueReadPlatformService codeValueReadPlatformService;

    @Bean
    public Step PurchaseOfSettlementStep() {

        return new StepBuilder(JobName.LIQUIDACION_DE_COMPRAS.name(), jobRepository)
                .tasklet(purchaseOfSettlementTasklet(), transactionManager).build();
    }

    @Bean
    public Job PurchaseSettlementJob() {

        return new JobBuilder(JobName.LIQUIDACION_DE_COMPRAS.name(), jobRepository).start(PurchaseOfSettlementStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public PurchaseOfSettlementTasklet purchaseOfSettlementTasklet() {
        return new PurchaseOfSettlementTasklet(clientAllyRepository, workingDaysRepositoryWrapper, allyPurchaseSettlementRepository,
                allyPurchaseSettlementReadWritePlatformService, globalConfigurationRepository, codeValueReadPlatformService);
    }
}
