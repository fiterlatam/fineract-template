package org.apache.fineract.custom.portfolio.ally.jobs.collectionsettlement;

import org.apache.fineract.custom.portfolio.ally.domain.AllyCollectionSettlementRepository;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyRepository;
import org.apache.fineract.custom.portfolio.ally.service.AllyCollectionSettlementReadWritePlatformService;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
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
public class CollectionOfSettlementConfig {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AllyCollectionSettlementReadWritePlatformService allyCollectionSettlementReadWritePlatformService;

    @Autowired
    private AllyCollectionSettlementRepository allyCollectionSettlementRepository;

    @Autowired
    private CodeValueReadPlatformService codeValueReadPlatformService;

    @Autowired
    private WorkingDaysRepositoryWrapper daysRepositoryWrapper;

    @Autowired
    private ClientAllyRepository clientAllyRepository;

    @Bean
    public Step collectionOfSettlementStep() {
        return new StepBuilder(JobName.LIQUIDACION_DE_RECAUDOS.name(), jobRepository)
                .tasklet(collectionOfSettlementTasklet(), transactionManager).build();
    }

    @Bean
    public Job collectionOfSettlementStepJob() {
        return new JobBuilder(JobName.LIQUIDACION_DE_RECAUDOS.name(), jobRepository).start(collectionOfSettlementStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public CollectionSettlementTasklet collectionOfSettlementTasklet() {
        return new CollectionSettlementTasklet(allyCollectionSettlementReadWritePlatformService, allyCollectionSettlementRepository,
                codeValueReadPlatformService, daysRepositoryWrapper, clientAllyRepository);
    }

}
