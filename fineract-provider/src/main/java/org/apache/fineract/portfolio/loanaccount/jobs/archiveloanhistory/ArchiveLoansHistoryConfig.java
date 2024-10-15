package org.apache.fineract.portfolio.loanaccount.jobs.archiveloanhistory;

import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.delinquency.service.DelinquencyReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.domain.LoanArchiveHistoryRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.service.LoanArchiveHistoryReadWritePlatformService;
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
public class ArchiveLoansHistoryConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private LoanArchiveHistoryReadWritePlatformService loanArchiveHistoryService;

    @Autowired
    private LoanArchiveHistoryRepository loanArchiveHistoryRepository;

    @Autowired
    private LoanRepositoryWrapper loanRepository;

    @Autowired
    private DelinquencyReadPlatformService delinquencyReadPlatformService;

    @Bean
    protected Step archiveLoanHistoryStep() {
        return new StepBuilder(JobName.ARCHIVE_LOAN_HISTORY.name(), jobRepository).tasklet(archiveLoansHistoryTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Job archiveLoansHistoryJob() {
        return new JobBuilder(JobName.ARCHIVE_LOAN_HISTORY.name(), jobRepository).start(archiveLoanHistoryStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public ArchiveLoansHistoryTasklet archiveLoansHistoryTasklet() {
        return new ArchiveLoansHistoryTasklet(loanArchiveHistoryService, loanArchiveHistoryRepository, loanRepository,
                delinquencyReadPlatformService);
    }
}
