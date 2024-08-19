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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;

@Entity
@Getter
@Table(name = "m_loan_overdue_installment_charge")
public class LoanOverdueInstallmentCharge extends AbstractPersistableCustom {

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_charge_id", referencedColumnName = "id", nullable = false)
    private LoanCharge loanCharge;

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_schedule_id", referencedColumnName = "id", nullable = false)
    private LoanRepaymentScheduleInstallment installment;

    @Column(name = "frequency_number")
    private Integer frequencyNumber;

    public LoanOverdueInstallmentCharge() {

    }

    public LoanOverdueInstallmentCharge(final LoanCharge loanCharge, final LoanRepaymentScheduleInstallment installment,
            final Integer frequencyNumber) {
        this.loanCharge = loanCharge;
        this.installment = installment;
        this.frequencyNumber = frequencyNumber;
    }

    public void updateLoanRepaymentScheduleInstallment(LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment) {
        this.installment = loanRepaymentScheduleInstallment;
    }

    public Money getPenaltyAmountOutstanding(final MonetaryCurrency currency) {
        final LoanCharge loanCharge = this.loanCharge;
        Money penaltyAmountOutstanding = Money.zero(currency);
        if (loanCharge != null) {
            penaltyAmountOutstanding = loanCharge.getAmount(currency).minus(loanCharge.getAmountPaid(currency))
                    .minus(loanCharge.getAmountWaived(currency)).minus(loanCharge.getAmountWrittenOff(currency));
        }
        return penaltyAmountOutstanding;
    }
}
