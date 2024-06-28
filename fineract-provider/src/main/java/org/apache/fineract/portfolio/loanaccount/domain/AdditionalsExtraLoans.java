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
package org.apache.fineract.portfolio.loanaccount.domain;

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

/**
 * All monetary transactions against a loan are modelled through this entity. Disbursements, Repayments, Waivers,
 * Write-off etc
 */
@Entity
@Table(name = "m_loan_external_existing_loans")
public class AdditionalsExtraLoans extends AbstractPersistableCustom {

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "institution_type", nullable = false)
    private Long institutionType;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    @Column(name = "loan_amount", nullable = false)
    private BigDecimal loanAmount;

    @ManyToOne
    @JoinColumn(name = "additionals_id")
    private GroupLoanAdditionals groupLoanAdditionals;

    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name = "fees")
    private BigDecimal fees;

    @Column(name = "loan_status", nullable = false)
    private Long loanStatus;

    protected AdditionalsExtraLoans() {}

    public AdditionalsExtraLoans(final GroupLoanAdditionals additionals, final Long institutionType, final BigDecimal loanAmount,
            final BigDecimal balance, final BigDecimal fees, final Long loanStatus, final String institutionName) {

        this.loan = additionals.getLoan();
        this.groupLoanAdditionals = additionals;
        this.institutionType = institutionType;
        this.loanAmount = loanAmount;
        this.balance = balance;
        this.fees = fees;
        this.loanStatus = loanStatus;
        this.institutionName = institutionName;
    }
}
