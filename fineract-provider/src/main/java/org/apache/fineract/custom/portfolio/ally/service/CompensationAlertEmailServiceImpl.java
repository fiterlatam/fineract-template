/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.custom.portfolio.ally.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
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
            // javaMailSenderImpl.setUsername(smtpCredentialsData.getUsername());
            // javaMailSenderImpl.setPassword(smtpCredentialsData.getPassword());
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
        properties.put("mail.smtp.starttls.enable", "false");
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", "false");
        properties.put("mail.smtp.ssl.trust", smtpCredentialsData.getHost());
        return properties;
    }
}
