package org.apache.fineract.portfolio.collectionhousemanagement.jobs;

import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.collectionhousemanagement.service.CollectionHouseHistoryReadWriteService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class CollectionHouseHistoryConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    CollectionHouseHistoryReadWriteService collectionHouseHistoryReadWriteService;

    @Bean
    protected Step collectionHouseHistoryMapStep() {
        return new StepBuilder(JobName.COLLECTION_HOUSE_HISTORY.name(), jobRepository)
                .tasklet(collectionHouseHistoryMapTasklet(), transactionManager).build();
    }

    @Bean
    public Job collectionHouseHistoryMapJob() {
        return new JobBuilder(JobName.COLLECTION_HOUSE_HISTORY.name(), jobRepository).start(collectionHouseHistoryMapStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public CollectionHouseConfigTasklet collectionHouseHistoryMapTasklet() {
        return new CollectionHouseConfigTasklet(commandsSourceWritePlatformService, collectionHouseHistoryReadWriteService);
    }
}
