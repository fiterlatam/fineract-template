package org.apache.fineract.custom.portfolio.ally.jobs.compensationemail;

import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensationRepository;
import org.apache.fineract.custom.portfolio.ally.service.CompensationAlertEmailService;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepository;
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
public class CompensationAlertEmailConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private GlobalConfigurationRepository globalConfigurationRepository;

    @Autowired
    private AllyCompensationRepository allyCompensationRepository;

    @Autowired
    private CompensationAlertEmailService compensationAlertEmailService;

    @Bean
    public Step compensationEmailStep() {
        return new StepBuilder(JobName.COMPENSATION_ALERT_EMAIL.name(), jobRepository)
                .tasklet(compensationAlertEmailTasklet(), transactionManager).build();
    }

    @Bean
    public Job compensationEmailJob() {
        return new JobBuilder(JobName.COMPENSATION_ALERT_EMAIL.name(), jobRepository).start(compensationEmailStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public CompensationAlertEmailTasklet compensationAlertEmailTasklet() {
        return new CompensationAlertEmailTasklet(globalConfigurationRepository, allyCompensationRepository, compensationAlertEmailService);
    }
}
