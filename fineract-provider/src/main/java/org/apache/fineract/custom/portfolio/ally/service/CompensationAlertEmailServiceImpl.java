package org.apache.fineract.custom.portfolio.ally.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.apache.fineract.custom.portfolio.ally.service.CompensationAlertEmailService;
import org.apache.fineract.infrastructure.configuration.data.SMTPCredentialsData;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class CompensationAlertEmailServiceImpl implements CompensationAlertEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(CompensationAlertEmailServiceImpl.class);
    private final ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService;

    @Autowired
    public CompensationAlertEmailServiceImpl(ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService) {
        this.externalServicesReadPlatformService = externalServicesReadPlatformService;
    }

    @Override
    public void sendCompensationAlertEmail(String to, String subject, String body) {
        final SMTPCredentialsData smtpCredentialsData = this.externalServicesReadPlatformService.getSMTPCredentials();
        try {
            JavaMailSenderImpl javaMailSenderImpl = new JavaMailSenderImpl();
            javaMailSenderImpl.setHost(smtpCredentialsData.getHost());
            javaMailSenderImpl.setPort(Integer.parseInt(smtpCredentialsData.getPort()));
            javaMailSenderImpl.setUsername(smtpCredentialsData.getUsername());
            javaMailSenderImpl.setPassword(smtpCredentialsData.getPassword());
            javaMailSenderImpl.setJavaMailProperties(this.getJavaMailProperties(smtpCredentialsData));

            MimeMessage mimeMessage = javaMailSenderImpl.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false);

            helper.setFrom(smtpCredentialsData.getFromEmail());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);

            javaMailSenderImpl.send(mimeMessage);
        } catch (MessagingException e) {
            LOG.error("Problem occurred in sendCompensationAlertEmail function", e);
            throw new RuntimeException("Failed to send compensation alert email", e);
        }
    }

    private Properties getJavaMailProperties(SMTPCredentialsData smtpCredentialsData) {
        Properties properties = new Properties();
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.trust", smtpCredentialsData.getHost());
        if (smtpCredentialsData.isUseTLS()) {
            if (smtpCredentialsData.getPort().equals("465")) {
                properties.put("mail.smtp.starttls.enable", "false");
            }
        }
        return properties;
    }
}
