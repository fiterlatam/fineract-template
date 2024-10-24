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
package org.apache.fineract.portfolio.account.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.cupo.domain.Cupo;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;

@Entity
@Table(name = "m_portfolio_account_associations")
public class AccountAssociations extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "loan_account_id", nullable = true)
    private Loan loanAccount;

    @ManyToOne
    @JoinColumn(name = "savings_account_id", nullable = true)
    private SavingsAccount savingsAccount;

    @ManyToOne
    @JoinColumn(name = "linked_loan_account_id", nullable = true)
    private Loan linkedLoanAccount;

    @ManyToOne
    @JoinColumn(name = "linked_savings_account_id", nullable = true)
    private SavingsAccount linkedSavingsAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_cupo_id", nullable = true)
    private Cupo linkedCupo;

    @Column(name = "association_type_enum", nullable = false)
    private Integer associationType;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected AccountAssociations() {}

    private AccountAssociations(final Loan loanAccount, final SavingsAccount savingsAccount, final Loan linkedLoanAccount,
            final SavingsAccount linkedSavingsAccount, final Integer associationType, boolean active, final Cupo linkedCupo) {
        this.loanAccount = loanAccount;
        this.savingsAccount = savingsAccount;
        this.linkedLoanAccount = linkedLoanAccount;
        this.linkedSavingsAccount = linkedSavingsAccount;
        this.associationType = associationType;
        this.active = active;
        this.linkedCupo = linkedCupo;
    }

    public static AccountAssociations associateSavingsAccount(final Loan loan, final SavingsAccount savingsAccount,
            final Integer associationType, boolean isActive) {
        return new AccountAssociations(loan, null, null, savingsAccount, associationType, isActive, null);
    }

    public static AccountAssociations associateSavingsAccount(final SavingsAccount savingsAccount,
            final SavingsAccount linkedSavingsAccount, final Integer associationType, boolean isActive) {
        return new AccountAssociations(null, savingsAccount, null, linkedSavingsAccount, associationType, isActive, null);
    }

    public static AccountAssociations associateCupo(final Loan loan, final Cupo linkedCupo, final Integer associationType,
            boolean isActive) {
        return new AccountAssociations(loan, null, null, null, associationType, isActive, linkedCupo);
    }

    public SavingsAccount linkedSavingsAccount() {
        return this.linkedSavingsAccount;
    }

    public void updateLinkedSavingsAccount(final SavingsAccount savingsAccount) {
        this.linkedSavingsAccount = savingsAccount;
    }

    public Cupo linkedCupo() {
        return this.linkedCupo;
    }

    public void updateLinkedCupo(final Cupo cupo) {
        this.linkedCupo = cupo;
    }
}
