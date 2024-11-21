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
package org.apache.fineract.portfolio.loanproductparameterization.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.campaigns.email.data.EmailMessageWithAttachmentData;
import org.apache.fineract.infrastructure.campaigns.email.service.EmailMessageJobEmailService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.loanproductparameterization.data.LoanProductParameterDataValidator;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterization;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterizationRepository;
import org.apache.fineract.portfolio.loanproductparameterization.exception.LoanProductParameterizationNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanProductParameterizationWriteServiceImpl implements LoanProductParameterizationWriteService {

    private final LoanProductParameterizationRepository productParameterizationRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final EmailMessageJobEmailService emailMessageJobEmailService;
    private final LoanProductParameterDataValidator loanProductParameterDataValidator;

    @Override
    public CommandProcessingResult createProductParameterization(JsonCommand command) {

        LoanProductParameterization productParameterization = LoanProductParameterization.create(command);
        loanProductParameterDataValidator.validateForCreate(command.json());
        LoanProductParameterization savedProductParameterization = productParameterizationRepository.save(productParameterization);

        return CommandProcessingResult.commandOnlyResult(savedProductParameterization.getId());
    }

    @Override
    public CommandProcessingResult updateProductParameterization(Long parameterId, JsonCommand command) {
        LoanProductParameterization productParameterization = findProductParameterization(parameterId);
        loanProductParameterDataValidator.validateForUpdate(command.json());
        productParameterization.update(command);
        productParameterizationRepository.save(productParameterization);

        return CommandProcessingResult.empty();
    }

    @Override
    public CommandProcessingResult deleteProductParameterization(Long parameterId) {
        LoanProductParameterization productParameterization = findProductParameterization(parameterId);
        productParameterizationRepository.delete(productParameterization);
        return CommandProcessingResult.empty();
    }

    @Override
    public void sendInvoiceNumberingLimitNotification() {
        Long invoiceNumberingLimit = configurationDomainService.retrieveInvoiceThreshold();
        if (invoiceNumberingLimit == null) {
            log.warn("Invoice numbering limit is not set , No notification will be sent");
            return;
        }

        List<String> emailAddresses = configurationDomainService.retrieveInvoiceJobNotificationEmails();
        if (emailAddresses.isEmpty()) {
            log.warn("No email addresses found for invoice numbering limit notification");
            return;
        }

        List<LoanProductParameterization> loanProductParameterizationList = productParameterizationRepository.findAll().stream()
                .filter(loanProductParameterization -> loanProductParameterization.isInvoiceNumberingLimitReached(invoiceNumberingLimit))
                .toList();

        if (loanProductParameterizationList.isEmpty()) {
            log.info("No loan product parameterization reached the invoice numbering limit");
            return;
        }
        loanProductParameterizationList.forEach(i -> log.info(
                "Invoice numbering limit reached for loan product parameterization with id: {} with  last invoice used as {} and threshold as {}",
                i.getId(), i.getLastInvoiceNumber(), invoiceNumberingLimit));

        final String subject = "Facura electrónica: Agotamiento de numerácion de factura";

        loanProductParameterizationList.forEach(loanProductParameterization -> {
            final String body = String.format("Se notifica que faltan %d Facturas de que se agote la numeración",
                    loanProductParameterization.getInvoiceNumberingRemaining());
            sendEmail(subject, body, emailAddresses);
        });

    }

    @Override
    public void sendIInvoiceResolutionExpiryNotification() {
        Long invoiceResolutionExpiryDays = configurationDomainService.retrieveInvoiceResolutionExpiryDays();
        if (invoiceResolutionExpiryDays == null) {
            log.warn("Invoice resolution expiry days is not set , No notification will be sent");
            return;
        }

        List<String> emailAddresses = configurationDomainService.retrieveInvoiceJobNotificationEmails();
        if (emailAddresses.isEmpty()) {
            log.warn("No email addresses found for invoice resolution expiry notification");
            return;
        }

        // this is in assumption that the list of loan product parameterization is not huge, if it is huge then we need
        // to
        // implement a pagination mechanism or use a query to fetch only the required data
        List<LoanProductParameterization> loanProductParameterizationList = productParameterizationRepository.findAll().stream()
                .filter(loanProductParameterization -> loanProductParameterization.isInvoiceResolutionExpiring(invoiceResolutionExpiryDays))
                .toList();

        if (loanProductParameterizationList.isEmpty()) {
            log.info("No loan product parameterization reached the invoice resolution expiry");
            return;
        }

        final String subject = "Facura electrónica: Vencimiento de la resolución";

        loanProductParameterizationList.forEach(loanProductParameterization -> {
            String body = String.format("Se notifica que estamos a %d días que se venzan la resolución de las facturas",
                    loanProductParameterization.getInvoiceResolutionExpiryDays());
            sendEmail(subject, body, emailAddresses);
        });
    }

    private LoanProductParameterization findProductParameterization(Long parameterId) {
        try {
            return productParameterizationRepository.findById(parameterId)
                    .orElseThrow(() -> new LoanProductParameterizationNotFoundException(parameterId));
        } catch (LoanProductParameterizationNotFoundException e) {
            log.error("LoanProductParameterizationNotFoundException: {}", e.getMessage());
            throw new LoanProductParameterizationNotFoundException(parameterId);
        }
    }

    private void sendEmail(String subject, String body, List<String> emailAddressList) {
        EmailMessageWithAttachmentData emailMessageWithAttachmentData = EmailMessageWithAttachmentData.createNew(body, subject, null,
                emailAddressList);
        emailMessageJobEmailService.sendEmailWithAttachment(emailMessageWithAttachmentData);
    }

}
