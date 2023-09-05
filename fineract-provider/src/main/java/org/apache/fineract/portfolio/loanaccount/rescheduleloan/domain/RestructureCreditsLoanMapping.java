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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain;

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "m_restructure_credits_loans_mapping")
public class RestructureCreditsLoanMapping extends AbstractPersistableCustom {
//    loan_id
//
//
    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private RestructureCreditsRequest restructureCreditsRequest;

    @Column(name = "status")
    private Integer statusEnum;


    @Column(name = "outstanding_balance")
    private BigDecimal outstandingBalance;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "maturity_Date")
    private LocalDate maturityDate;

    protected RestructureCreditsLoanMapping() {}

    /**
     * LoanRescheduleRequest constructor
     **/
    private RestructureCreditsLoanMapping(final Loan loan, final Integer statusEnum,
                                          final RestructureCreditsRequest restructureCreditsRequest,
                                          final BigDecimal outstandingBalance,
                                          final LocalDate disbursementDate,final LocalDate maturityDate) {
        this.loan = loan;
        this.statusEnum = statusEnum;
        this.restructureCreditsRequest = restructureCreditsRequest;
        this.outstandingBalance = outstandingBalance;
        this.disbursementDate = disbursementDate;
        this.maturityDate = maturityDate;

    }

    /**
     * @return a new instance of the LoanRescheduleRequest class
     **/
    public static RestructureCreditsLoanMapping instance(final Loan loan, final Integer statusEnum,
                                                         final RestructureCreditsRequest restructureCreditsRequest) {

        return new RestructureCreditsLoanMapping(loan, statusEnum, restructureCreditsRequest, loan.getSummary().getTotalOutstanding(),loan.getDisbursementDate(),loan.getMaturityDate());
    }

    /**
     * @return the reschedule request loan object
     **/
    public Loan getLoan() {
        return this.loan;
    }

    /**
     * @return the status enum
     **/
    public Integer getStatusEnum() {
        return this.statusEnum;
    }

    /**
     * @return installment number of the rescheduling start point
     **/
    public RestructureCreditsRequest getRestructureCreditsRequest() {
        return this.restructureCreditsRequest;
    }
}
