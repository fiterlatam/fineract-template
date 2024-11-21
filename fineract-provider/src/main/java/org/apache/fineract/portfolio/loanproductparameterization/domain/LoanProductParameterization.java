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
package org.apache.fineract.portfolio.loanproductparameterization.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductType;
import org.apache.fineract.portfolio.loanproductparameterization.data.LoanProductParameterizationData;

@Entity
@Table(name = "m_product_type_parameters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanProductParameterization extends AbstractAuditableWithUTCDateTimeCustom {

    @Column(name = "product_type", nullable = false)
    private String productType;

    @Column(name = "billing_prefix")
    private String billingPrefix;

    @Column(name = "billing_resolution_number")
    private Long billingResolutionNumber;

    @Column(name = "generation_date")
    private LocalDate generationDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "range_start_number")
    private Long rangeStartNumber;

    @Column(name = "range_end_number")
    private Long rangeEndNumber;

    @Column(name = "last_invoice_number")
    private Long lastInvoiceNumber;

    @Column(name = "last_credit_note_number")
    private Long lastCreditNoteNumber;

    @Column(name = "last_debit_note_number")
    private Long lastDebitNoteNumber;

    public LoanProductParameterizationData toData() {
        return new LoanProductParameterizationData(getId(), productType, billingPrefix, billingResolutionNumber, generationDate,
                expirationDate, rangeStartNumber, rangeEndNumber, lastInvoiceNumber, lastCreditNoteNumber, lastDebitNoteNumber);
    }

    public static LoanProductParameterization create(JsonCommand command) {
        return extractParameters(command);
    }

    public void update(JsonCommand command) {
        LoanProductParameterization updatedParameters = extractParameters(command);

        if (updatedParameters.getProductType() != null) {
            this.productType = updatedParameters.getProductType();
        }
        if (updatedParameters.getBillingPrefix() != null) {
            this.billingPrefix = updatedParameters.getBillingPrefix();
        }
        if (updatedParameters.getBillingResolutionNumber() != null) {
            this.billingResolutionNumber = updatedParameters.getBillingResolutionNumber();
        }
        if (updatedParameters.getGenerationDate() != null) {
            this.generationDate = updatedParameters.getGenerationDate();
        }
        if (updatedParameters.getExpirationDate() != null) {
            this.expirationDate = updatedParameters.getExpirationDate();
        }
        if (updatedParameters.getRangeStartNumber() != null) {
            this.rangeStartNumber = updatedParameters.getRangeStartNumber();
        }
        if (updatedParameters.getRangeEndNumber() != null) {
            this.rangeEndNumber = updatedParameters.getRangeEndNumber();
        }
        if (updatedParameters.getLastInvoiceNumber() != null) {
            this.lastInvoiceNumber = updatedParameters.getLastInvoiceNumber();
        }
        if (updatedParameters.getLastCreditNoteNumber() != null) {
            this.lastCreditNoteNumber = updatedParameters.getLastCreditNoteNumber();
        }
        if (updatedParameters.getLastDebitNoteNumber() != null) {
            this.lastDebitNoteNumber = updatedParameters.getLastDebitNoteNumber();
        }
    }

    private static LoanProductParameterization extractParameters(JsonCommand command) {
        final String productType = command.stringValueOfParameterNamed("productType");
        final String billingPrefix = command.stringValueOfParameterNamed("billingPrefix");
        final Long billingResolutionNumber = command.longValueOfParameterNamed("billingResolutionNumber");
        final LocalDate generationDate = command.localDateValueOfParameterNamed("generationDate");
        final LocalDate expirationDate = command.localDateValueOfParameterNamed("expirationDate");
        final Long rangeStartNumber = command.longValueOfParameterNamed("rangeStartNumber");
        final Long rangeEndNumber = command.longValueOfParameterNamed("rangeEndNumber");
        final Long lastInvoiceNumber = command.longValueOfParameterNamed("lastInvoiceNumber");
        final Long lastCreditNoteNumber = command.longValueOfParameterNamed("lastCreditNoteNumber");
        final Long lastDebitNoteNumber = command.longValueOfParameterNamed("lastDebitNoteNumber");

        // validate that rangeStartNumber is less than rangeEndNumber
        if (rangeStartNumber > rangeEndNumber) {
            throw new GeneralPlatformDomainRuleException("validation.error.range.start.number.greater.than.range.end.number",
                    "The range start number must be less than the range end number.");
        }

        // check that the product type is valid
        if (StringUtils.isBlank(productType) || !LoanProductType.isValidProductType(productType)) {
            throw new GeneralPlatformDomainRuleException("validation.error.invalid.product.type",
                    "The product type is invalid. The product type must be one of the following: "
                            + Arrays.toString(LoanProductType.values()));
        }

        return new LoanProductParameterization(productType, billingPrefix, billingResolutionNumber, generationDate, expirationDate,
                rangeStartNumber, rangeEndNumber, lastInvoiceNumber, lastCreditNoteNumber, lastDebitNoteNumber);
    }

    public boolean isInvoiceResolutionExpiring(Long daysPrior) {
        LocalDate currentDate = DateUtils.getLocalDateOfTenant();
        LocalDate warningDate = currentDate.plusDays(daysPrior);
        // Implement logic to check if the invoice resolution is expiring within the specified days
        // Return true if the condition is met, otherwise false

        // Check if the expiration date is within the specified days
        return expirationDate != null && (warningDate.isAfter(expirationDate) || warningDate.isEqual(expirationDate));
    }

    public boolean isInvoiceNumberingLimitReached(Long threshold) {
        // Implement logic to check if the invoice numbering limit is reached within the specified quantity
        // Return true if the condition is met, otherwise false
        long maximumInvoiceNumber = rangeEndNumber;
        long usedInvoiceNumber = lastInvoiceNumber;
        long remainingInvoiceNumber = maximumInvoiceNumber - usedInvoiceNumber;

        // Check if the last invoice number is within the specified quantity
        return remainingInvoiceNumber <= threshold;
    }

    public Long getInvoiceNumberingRemaining() {
        long maximumInvoiceNumber = rangeEndNumber;
        long usedInvoiceNumber = lastInvoiceNumber;
        return maximumInvoiceNumber - usedInvoiceNumber;
    }

    public Long getInvoiceResolutionExpiryDays() {
        LocalDate currentDate = DateUtils.getLocalDateOfTenant();
        return DateUtils.getDifferenceInDays(currentDate, expirationDate);
    }

    public void validateForCreate(final String json) {
        // Implement validation logic for creating a new loan product parameterization
    }
}
