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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.fineract.organisation.agency.domain.Agency;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

public class BankAccountId implements Serializable {

    @Column(name = "bank_account", nullable = false)
    private Long bankAccount;

    @ManyToOne
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    public BankAccountId(Long bankAccount, Agency agency){
        this.bankAccount = bankAccount;
        this.agency = agency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || o instanceof BankAccountId) return false;

        BankAccountId that = (BankAccountId) o;

        return new EqualsBuilder().append(bankAccount, that.bankAccount).append(agency, that.agency).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(bankAccount).append(agency).toHashCode();
    }
}
