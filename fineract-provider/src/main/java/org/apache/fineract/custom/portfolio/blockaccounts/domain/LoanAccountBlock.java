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
package org.apache.fineract.custom.portfolio.blockaccounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;

@Slf4j
@Entity
@Getter
@Setter
@Table(name = "c_loan_account_block", schema = "custom")
public class LoanAccountBlock extends AbstractAuditableWithUTCDateTimeCustom {

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false, referencedColumnName = "id")
    private Loan loan;

    @Getter
    @ManyToOne
    @JoinColumn(name = "blocking_reason_id", nullable = false, referencedColumnName = "id")
    private BlockingReasonSetting blockingReasonSetting;

    @Column(name = "application_date")
    private LocalDate applicationDate;

    @Column(name = "accelerate")
    private Boolean accelerate;

    @Column(name = "freeze_current_interest")
    private Boolean freezeCurrentInterest;

    @Column(name = "freeze_interest_arrears")
    private Boolean freezeInterestArrears;

    @Column(name = "freeze_life_insurance")
    private Boolean freezeLifeInsurance;

    @Column(name = "freeze_mypime")
    private Boolean freezeMypime;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "note")
    private String note;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "action_enum", nullable = false)
    private LoanAccountBlockAction action;

    public String getName() {
        return blockingReasonSetting != null ? blockingReasonSetting.getNameOfReason() : null;
    }

    public String getActionName() {
        return this.action != null ? this.action.name() : null;
    }

    public String getFormattedLastModifiedDate() {
        return getLastModifiedDate().map(date -> date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).orElse(null);
    }

    public LoanAccountBlock createLoanAccountBlock(Loan loan, BlockingReasonSetting blockingReasonSetting, LocalDate applicationDate,
            Boolean accelerate, Boolean freezeCurrentInterest, Boolean freezeInterestArrears, Boolean freezeLifeInsurance,
            Boolean freezeMypime, Boolean active, LoanAccountBlockAction action, String note) {

        LoanAccountBlock loanAccountBlock = new LoanAccountBlock();
        loanAccountBlock.loan = loan;
        loanAccountBlock.blockingReasonSetting = blockingReasonSetting;
        loanAccountBlock.applicationDate = applicationDate;
        loanAccountBlock.accelerate = accelerate;
        loanAccountBlock.freezeCurrentInterest = freezeCurrentInterest;
        loanAccountBlock.freezeInterestArrears = freezeInterestArrears;
        loanAccountBlock.freezeLifeInsurance = freezeLifeInsurance;
        loanAccountBlock.freezeMypime = freezeMypime;
        loanAccountBlock.active = active;
        loanAccountBlock.note = note;
        loanAccountBlock.action = action;

        return loanAccountBlock;
    }

    public LoanAccountBlock() {

    }
}
