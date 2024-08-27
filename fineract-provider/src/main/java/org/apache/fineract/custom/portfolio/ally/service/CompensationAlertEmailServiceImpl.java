package org.apache.fineract.custom.portfolio.ally.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.Properties;
import org.apache.fineract.custom.portfolio.ally.service.CompensationAlertEmailService;
import org.apache.fineract.infrastructure.reportmailingjob.ReportMailingJobConstants;
import org.apache.fineract.infrastructure.reportmailingjob.data.ReportMailingJobConfigurationData;
import org.apache.fineract.infrastructure.reportmailingjob.service.ReportMailingJobConfigurationReadPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class CompensationAlertEmailServiceImpl implements CompensationAlertEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(CompensationAlertEmailServiceImpl.class);
    private final ReportMailingJobConfigurationReadPlatformService reportMailingJobConfigurationReadPlatformService;

    @Autowired
    public CompensationAlertEmailServiceImpl(
            ReportMailingJobConfigurationReadPlatformService reportMailingJobConfigurationReadPlatformService) {
        this.reportMailingJobConfigurationReadPlatformService = reportMailingJobConfigurationReadPlatformService;
    }

    @Override
    public void sendCompensationAlertEmail(String to, String subject, String body) {
        try {
            Collection<ReportMailingJobConfigurationData> configData = this.reportMailingJobConfigurationReadPlatformService
                    .retrieveAllReportMailingJobConfigurations();

            JavaMailSenderImpl javaMailSender = configureMailSender(configData);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);

            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            LOG.error("Problem occurred in sendCompensationAlertEmail function", e);
            throw new RuntimeException("Failed to send compensation alert email", e);
        }
    }

    private JavaMailSenderImpl configureMailSender(Collection<ReportMailingJobConfigurationData> configData) {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(getGmailSmtpServer(configData));
        javaMailSender.setPort(getGmailSmtpPort(configData));
        javaMailSender.setUsername(getGmailSmtpUsername(configData));
        javaMailSender.setPassword(getGmailSmtpPassword(configData));
        javaMailSender.setJavaMailProperties(getJavaMailProperties(configData));
        return javaMailSender;
    }

    private Properties getJavaMailProperties(Collection<ReportMailingJobConfigurationData> configData) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.ssl.trust", getGmailSmtpServer(configData));
        return properties;
    }

    private String getGmailSmtpServer(Collection<ReportMailingJobConfigurationData> reportMailingJobConfigurationDataCollection) {
        final ReportMailingJobConfigurationData reportMailingJobConfigurationData = this.getReportMailingJobConfigurationData(
                reportMailingJobConfigurationDataCollection, ReportMailingJobConstants.GMAIL_SMTP_SERVER);

        return (reportMailingJobConfigurationData != null) ? reportMailingJobConfigurationData.getValue() : null;
    }

    private Integer getGmailSmtpPort(Collection<ReportMailingJobConfigurationData> reportMailingJobConfigurationDataCollection) {
        final ReportMailingJobConfigurationData reportMailingJobConfigurationData = this.getReportMailingJobConfigurationData(
                reportMailingJobConfigurationDataCollection, ReportMailingJobConstants.GMAIL_SMTP_PORT);
        final String portNumber = (reportMailingJobConfigurationData != null) ? reportMailingJobConfigurationData.getValue() : null;

        return (portNumber != null) ? Integer.parseInt(portNumber) : null;
    }

    private String getGmailSmtpUsername(Collection<ReportMailingJobConfigurationData> reportMailingJobConfigurationDataCollection) {
        final ReportMailingJobConfigurationData reportMailingJobConfigurationData = this.getReportMailingJobConfigurationData(
                reportMailingJobConfigurationDataCollection, ReportMailingJobConstants.GMAIL_SMTP_USERNAME);

        return (reportMailingJobConfigurationData != null) ? reportMailingJobConfigurationData.getValue() : null;
    }

    private String getGmailSmtpPassword(Collection<ReportMailingJobConfigurationData> reportMailingJobConfigurationDataCollection) {
        final ReportMailingJobConfigurationData reportMailingJobConfigurationData = this.getReportMailingJobConfigurationData(
                reportMailingJobConfigurationDataCollection, ReportMailingJobConstants.GMAIL_SMTP_PASSWORD);

        return (reportMailingJobConfigurationData != null) ? reportMailingJobConfigurationData.getValue() : null;
    }

    private ReportMailingJobConfigurationData getReportMailingJobConfigurationData(
            final Collection<ReportMailingJobConfigurationData> reportMailingJobConfigurationDataCollection, final String name) {
        return reportMailingJobConfigurationDataCollection.stream().filter(config -> name.equals(config.getName())).findFirst()
                .orElse(null);
    }
}
