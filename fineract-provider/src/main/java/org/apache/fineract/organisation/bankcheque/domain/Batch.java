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
package org.apache.fineract.organisation.bankcheque.domain;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.organisation.agency.domain.Agency;
import org.apache.fineract.organisation.bankAccount.domain.BankAccount;
import org.apache.fineract.organisation.bankcheque.api.BankChequeApiConstants;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Entity
@Table(name = "m_payment_batch")
public class Batch extends AbstractAuditableCustom {

    @Column(name = "batch_no", nullable = false)
    private Long batchNo;

    @Column(name = "bank_acc_no", nullable = false)
    private Long bankAccNo;

    @ManyToOne
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @ManyToOne
    @JoinColumn(name = "bank_acc_id", nullable = false)
    private BankAccount bankAccount;

    @Column(name = "from", nullable = false)
    private Long from;

    @Column(name = "to", nullable = false)
    private Long to;

    @Column(name = "description")
    private String description;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "batch", orphanRemoval = true)
    private Set<Cheque> cheques = new HashSet<>();


    public Batch setBatchNo(Long batchNo) {
        this.batchNo = batchNo;
        return this;
    }

    public Batch setBankAccNo(Long bankAccNo) {
        this.bankAccNo = bankAccNo;
        return this;
    }

    public Batch setAgency(Agency agency) {
        this.agency = agency;
        return this;
    }

    public Batch setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
        return this;
    }

    public Batch setFrom(Long from) {
        this.from = from;
        return this;
    }

    public Batch setTo(Long to) {
        this.to = to;
        return this;
    }

    public Batch setDescription(String description) {
        this.description = description;
        return this;
    }

    public Batch setCheques(Set<Cheque> cheques) {
        this.cheques = cheques;
        return this;
    }

    public Map<String, Object> update(final JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(9);
        if (command.isChangeInStringParameterNamed(BankChequeApiConstants.DESCRIPTION, this.description)) {
            final String newValue = command.stringValueOfParameterNamed(BankChequeApiConstants.DESCRIPTION);
            actualChanges.put(BankChequeApiConstants.DESCRIPTION, newValue);
            this.description = StringUtils.defaultIfEmpty(newValue, null);
        }

        if (command.isChangeInLongParameterNamed(BankChequeApiConstants.FROM, this.from)) {
            final Long newValue = command.longValueOfParameterNamed(BankChequeApiConstants.FROM);
            actualChanges.put(BankChequeApiConstants.FROM, newValue);
            this.from = newValue;
        }

        if (command.isChangeInLongParameterNamed(BankChequeApiConstants.TO, this.from)) {
            final Long newValue = command.longValueOfParameterNamed(BankChequeApiConstants.TO);
            actualChanges.put(BankChequeApiConstants.TO, newValue);
            this.to = newValue;
        }

        if (command.isChangeInLongParameterNamed(BankChequeApiConstants.AGENCY_ID, this.agency.getId())) {
            final Long newValue = command.longValueOfParameterNamed(BankChequeApiConstants.TO);
            actualChanges.put(BankChequeApiConstants.AGENCY_ID, newValue);
        }

        if (command.isChangeInLongParameterNamed(BankChequeApiConstants.BANK_ACC_NO, this.bankAccNo)) {
            final Long newValue = command.longValueOfParameterNamed(BankChequeApiConstants.BANK_ACC_NO);
            actualChanges.put(BankChequeApiConstants.BANK_ACC_NO, newValue);
        }
        return actualChanges;
    }

}
