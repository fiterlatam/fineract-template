package org.apache.fineract.custom.portfolio.ally.jobs.collectionsettlement;

import org.apache.fineract.custom.portfolio.ally.domain.AllyCollectionSettlementRepository;
import org.apache.fineract.custom.portfolio.ally.service.ClientAllyPointOfSalesReadWritePlatformService;
import org.apache.fineract.infrastructure.jobs.service.JobName;
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
    private ClientAllyPointOfSalesReadWritePlatformService clientAllyPointOfSalesService;

    @Autowired
    private AllyCollectionSettlementRepository allyCollectionSettlementRepository;

    @Bean
    public Step collectionOfSettlementStep() {
        return new StepBuilder(JobName.ALLY_COLLECTION_SETTLEMENT.name(), jobRepository)
                .tasklet(collectionOfSettlementTasklet(), transactionManager).build();
    }

    @Bean
    public Job collectionOfSettlementStepJob() {
        return new JobBuilder(JobName.ALLY_COLLECTION_SETTLEMENT.name(), jobRepository).start(collectionOfSettlementStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public CollectionSettlementTasklet collectionOfSettlementTasklet() {
        return new CollectionSettlementTasklet(clientAllyPointOfSalesService, allyCollectionSettlementRepository);
    }

}
