package org.apache.fineract.custom.portfolio.ally.jobs.compensationsettlement;

import org.apache.fineract.custom.portfolio.ally.service.AllyCompensationReadWritePlatformService;
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
public class CompensationOfSettlementConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    AllyCompensationReadWritePlatformService allyCompensationReadWritePlatformService;

    @Bean
    public Step CompensationOfSettlementStep() {

        return new StepBuilder(JobName.COMPENSATION.name(), jobRepository).tasklet(compensationOfSettlementTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Job CompensationOfSettlementJob() {

        return new JobBuilder(JobName.COMPENSATION.name(), jobRepository).start(CompensationOfSettlementStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public CompensationOfSettlementTasklet compensationOfSettlementTasklet() {
        return new CompensationOfSettlementTasklet(allyCompensationReadWritePlatformService);
    }
}
