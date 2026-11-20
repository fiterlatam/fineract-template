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
package org.apache.fineract.portfolio.loanapplicationdraft.domain;

import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanapplicationdraft.api.LoanApplicationDraftConstants;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Entity
@Component
@Getter
@Table(name = "m_loan_application_draft")
public class LoanApplicationDraft extends AbstractAuditableWithUTCDateTimeCustom {

    private static final Logger LOG = LoggerFactory.getLogger(Loan.class);

    /** Disable optimistic locking till batch jobs failures can be fixed **/
    @Version
    int version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    @Column(name = "status_enum", nullable = false)
    private Integer statusEnum;

    @Column(name = "current_step")
    private String currentStep;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id")
    private Loan loan;

    protected LoanApplicationDraft() {
        //
    }

    public LoanApplicationDraft(final Client client, final LoanProduct loanProduct, final Integer statusEnum, final String currentStep,
            final String payloadJson) {
        this.client = client;
        this.loanProduct = loanProduct;
        this.statusEnum = statusEnum;
        this.currentStep = currentStep;
        this.payloadJson = payloadJson;
    }

    public void modifyApplication(final LoanProduct loanProduct, final JsonCommand command, final Map<String, Object> actualChanges) {

        if (command.isChangeInIntegerParameterNamed(LoanApplicationDraftConstants.statusValueParameterName, this.statusEnum)) {
            final Integer newValue = command.integerValueOfParameterNamed(LoanApplicationDraftConstants.statusValueParameterName);
            actualChanges.put(LoanApplicationDraftConstants.statusValueParameterName, newValue);
            this.statusEnum = newValue;
        }

        if (command.isChangeInStringParameterNamed(LoanApplicationDraftConstants.currentStepParameterName, this.currentStep)) {
            final String newValue = command.stringValueOfParameterNamed(LoanApplicationDraftConstants.currentStepParameterName);
            actualChanges.put(LoanApplicationDraftConstants.currentStepParameterName, newValue);
            this.currentStep = newValue;
        }

        if (command.isChangeInStringParameterNamed(LoanApplicationDraftConstants.payloadJsonParameterName, this.payloadJson)) {
            final String newValue = command.stringValueOfParameterNamed(LoanApplicationDraftConstants.payloadJsonParameterName);
            actualChanges.put(LoanApplicationDraftConstants.payloadJsonParameterName, newValue);
            this.payloadJson = StringUtils.defaultIfEmpty(newValue, null);
        }

        if (command.isChangeInLongParameterNamed(LoanApplicationDraftConstants.loanProductIdParameterName, this.loanProduct.getId())) {
            actualChanges.put(LoanApplicationDraftConstants.loanProductIdParameterName, loanProduct.getId());
            this.loanProduct = loanProduct;
        }

    }

    public void delete() {
        this.statusEnum = LoanApplicationDraftStatus.DELETED.getValue();
    }

}
