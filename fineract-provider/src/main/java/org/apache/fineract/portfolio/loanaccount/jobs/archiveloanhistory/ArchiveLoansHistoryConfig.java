package org.apache.fineract.portfolio.loanaccount.jobs.archiveloanhistory;

import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArchiveLoansHistoryConfig {

    @Autowired
    private JobRepository jobRepository;

    @Bean
    protected Step archiveLoanHistoryStep(ArchiveLoansHistoryTasklet archiveLoansHistoryTasklet) {
        return new StepBuilder(JobName.ARCHIVE_LOAN_HISTORY.name(), jobRepository)
                .tasklet(archiveLoansHistoryTasklet, new ResourcelessTransactionManager()).build();
    }

    @Bean
    public Job archiveLoansHistoryJob(ArchiveLoansHistoryTasklet archiveLoansHistoryTasklet) {
        return new JobBuilder(JobName.ARCHIVE_LOAN_HISTORY.name(), jobRepository).start(archiveLoanHistoryStep(archiveLoansHistoryTasklet))
                .incrementer(new RunIdIncrementer()).build();
    }
}
