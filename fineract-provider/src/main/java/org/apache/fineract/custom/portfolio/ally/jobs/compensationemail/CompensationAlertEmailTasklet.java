package org.apache.fineract.custom.portfolio.ally.jobs.compensationemail;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensation;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensationRepository;
import org.apache.fineract.custom.portfolio.ally.service.CompensationAlertEmailService;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationProperty;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepository;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class CompensationAlertEmailTasklet implements Tasklet {

    private final GlobalConfigurationRepository globalConfigurationRepository;
    private final AllyCompensationRepository allyCompensationRepository;
    private final CompensationAlertEmailService compensationAlertEmailService;

    public CompensationAlertEmailTasklet(GlobalConfigurationRepository globalConfigurationRepository,
            AllyCompensationRepository allyCompensationRepository, CompensationAlertEmailService compensationAlertEmailService) {
        this.globalConfigurationRepository = globalConfigurationRepository;
        this.allyCompensationRepository = allyCompensationRepository;
        this.compensationAlertEmailService = compensationAlertEmailService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        Optional<GlobalConfigurationProperty> getEmailCompensation = this.globalConfigurationRepository
                .findByName(LoanApiConstants.GLOBAL_CONFIG_COMPENSATION_ALERT_EMAIL);
        String compensationEmailsString = getEmailCompensation.map(prop -> String.valueOf(prop.getValue())).orElse("");
        List<String> compensationEmails = separateEmails(compensationEmailsString);

        if (compensationEmails.isEmpty()) {
            System.out.println("JOB IS FINISHED WITH NO EMAILS RECIPIENT");
            return RepeatStatus.FINISHED; // No recipients configured, exit
        }

        List<AllyCompensation> negativeCompensations = allyCompensationRepository.findNegativeCompensations();

        for (AllyCompensation compensation : negativeCompensations) {
            String emailBody = String.format(
                    "Se generó una cuenta por cobrar al comercio %s (Razón social) con nit %s por valor de $%.2f, la cual se debe gestionar",
                    compensation.getCompanyName(), compensation.getNit(), compensation.getNetOutstandingAmount());

            for (String email : compensationEmails) {
                compensationAlertEmailService.sendCompensationAlertEmail(email, "Compensación: Cuenta por cobrar", emailBody);
            }
        }

        return RepeatStatus.FINISHED;
    }

    private List<String> separateEmails(String emailString) {
        if (emailString == null || emailString.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(emailString.split(";")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }
}
