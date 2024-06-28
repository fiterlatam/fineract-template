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
package org.apache.fineract.organisation.bankAccount.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.agency.domain.Agency;
import org.apache.fineract.organisation.bank.domain.Bank;
import org.apache.fineract.organisation.bankAccount.service.BankAccountConstants;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Getter
@Table(name = "m_bank_account")
public class BankAccount extends AbstractPersistableCustom {

    @Column(name = "account_number", nullable = false, length = 15)
    private Long accountNumber;

    @ManyToOne
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @ManyToOne
    @JoinColumn(name = "bank_id", nullable = false)
    private Bank bank;

    @ManyToOne
    @JoinColumn(name = "gl_account_id", nullable = false)
    private GLAccount glAccount;

    @Column(name = "description", nullable = false, length = 100)
    private String description;

    @ManyToOne
    @JoinColumn(name = "added_by", nullable = false)
    private AppUser addedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public BankAccount() {
        //
    }

    public void setAccountNumber(Long accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setAgency(Agency agency) {
        this.agency = agency;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public void setGlAccount(GLAccount glAccount) {
        this.glAccount = glAccount;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BankAccount(Long accountNumber, Agency agency, Bank bank, GLAccount glAccount, String description, AppUser currentUser) {
        this.accountNumber = accountNumber;
        this.agency = agency;
        this.bank = bank;
        this.glAccount = glAccount;
        this.description = description;
        this.addedBy = currentUser;
        this.createdAt = DateUtils.getLocalDateTimeOfTenant();
    }

    public static BankAccount fromJson(AppUser currentUser, Agency agency, Bank bank, GLAccount glAccount, JsonCommand command) {
        final String description = command
                .stringValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.DESCRIPTION.getValue());
        final Long bankAccount = command
                .longValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.ACCOUNT_NUMBER.getValue());

        return new BankAccount(bankAccount, agency, bank, glAccount, description, currentUser);
    }

    public Map<String, Object> update(JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(2);

        if (command.isChangeInLongParameterNamed(BankAccountConstants.BankAccountSupportedParameters.ACCOUNT_NUMBER.getValue(),
                this.accountNumber)) {
            final Long newValue = command
                    .longValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.ACCOUNT_NUMBER.getValue());
            actualChanges.put(BankAccountConstants.BankAccountSupportedParameters.ACCOUNT_NUMBER.getValue(), newValue);
            // this.accountNumber = newValue;
        }

        if (command.isChangeInStringParameterNamed(BankAccountConstants.BankAccountSupportedParameters.DESCRIPTION.getValue(),
                this.description)) {
            final String newValue = command
                    .stringValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.DESCRIPTION.getValue());
            actualChanges.put(BankAccountConstants.BankAccountSupportedParameters.DESCRIPTION.getValue(), newValue);
            this.description = newValue;
        }

        return actualChanges;
    }

}
