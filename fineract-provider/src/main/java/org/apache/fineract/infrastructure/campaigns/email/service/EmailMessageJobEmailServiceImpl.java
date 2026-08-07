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
package org.apache.fineract.infrastructure.campaigns.email.service;

import java.io.File;
import java.util.List;
import java.util.Properties;
import javax.mail.internet.MimeMessage;
import org.apache.fineract.infrastructure.campaigns.email.data.EmailMessageWithAttachmentData;
import org.apache.fineract.infrastructure.configuration.data.SMTPCredentialsData;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public final class EmailMessageJobEmailServiceImpl implements EmailMessageJobEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailMessageJobEmailServiceImpl.class);
    private final ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService;

    @Autowired
    public EmailMessageJobEmailServiceImpl(ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService) {
        this.externalServicesReadPlatformService = externalServicesReadPlatformService;
    }

    @Override
    public void sendEmailWithAttachment(EmailMessageWithAttachmentData emailMessageWithAttachmentData) {
        final SMTPCredentialsData smtpCredentialsData = this.externalServicesReadPlatformService.getSMTPCredentials();
        try {
            JavaMailSenderImpl javaMailSenderImpl = new JavaMailSenderImpl();
            javaMailSenderImpl.setHost(smtpCredentialsData.getHost());
            javaMailSenderImpl.setPort(Integer.parseInt(smtpCredentialsData.getPort()));
            javaMailSenderImpl.setUsername(smtpCredentialsData.getUsername());
            javaMailSenderImpl.setPassword(smtpCredentialsData.getPassword());
            javaMailSenderImpl
                    .setJavaMailProperties(this.getJavaMailProperties(smtpCredentialsData, javaMailSenderImpl.getJavaMailProperties()));

            MimeMessage mimeMessage = javaMailSenderImpl.createMimeMessage();

            // use the true flag to indicate you need a multipart message
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setFrom(smtpCredentialsData.getFromEmail());
            mimeMessageHelper.setTo(emailMessageWithAttachmentData.getTo());
            mimeMessageHelper.setText(emailMessageWithAttachmentData.getText(), true);
            mimeMessageHelper.setSubject(emailMessageWithAttachmentData.getSubject());
            final List<File> attachments = emailMessageWithAttachmentData.getAttachments();
            if (attachments != null && attachments.size() > 0) {
                for (final File attachment : attachments) {
                    if (attachment != null) {
                        mimeMessageHelper.addAttachment(attachment.getName(), attachment);
                    }
                }
            }

            javaMailSenderImpl.send(mimeMessage);

        } catch (Exception e) {
            LOG.error("Could not send email. Problem occurred in sendEmailWithAttachment function", e);
        }

    }

    private Properties getJavaMailProperties(SMTPCredentialsData smtpCredentialsData, Properties properties) {
        final String port = smtpCredentialsData.getPort();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.trust", smtpCredentialsData.getHost());

        // Port 465 expects implicit SSL (SMTPS). Port 587 uses STARTTLS.
        if ("465".equals(port)) {
            properties.put("mail.smtp.starttls.enable", "false");
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.socketFactory.port", port);
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.socketFactory.fallback", "false");
        } else {
            properties.put("mail.smtp.starttls.enable", String.valueOf(smtpCredentialsData.isUseTLS()));
            if (smtpCredentialsData.isUseTLS()) {
                properties.put("mail.smtp.starttls.required", "true");
            }
        }
        return properties;
    }
}
