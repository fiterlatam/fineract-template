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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.useradministration.domain.AppUser;

@Setter
@Entity
@Table(name = "m_credit_blocking_reason")
public class LoanBlockingReason extends AbstractAuditableCustom {

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Getter
    @ManyToOne
    @JoinColumn(name = "blocking_reason_id", nullable = false)
    private BlockingReasonSetting blockingReasonSetting;

    @Getter
    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "comment")
    private String comment;

    @Setter
    @Getter
    @Column(name = "block_date")
    private LocalDate blockDate;

    @Setter
    @Column(name = "unblock_comment")
    private String unblockComment;

    @ManyToOne
    @JoinColumn(name = "deactivatedby_id")
    private AppUser deactivatedBy;

    @Column(name = "deactivated_on")
    private LocalDate deactivatedOn;

    public LoanBlockingReason() {

    }

    private LoanBlockingReason(Loan loan, BlockingReasonSetting blockingReasonSetting, boolean isActive, String comment,
            AppUser deactivatedBy, LocalDate deactivatedOn, LocalDate blockDate) {
        this.loan = loan;
        this.blockingReasonSetting = blockingReasonSetting;
        this.isActive = isActive;
        this.comment = comment;
        this.deactivatedBy = deactivatedBy;
        this.deactivatedOn = deactivatedOn;
        this.blockDate = blockDate;
    }

    public static LoanBlockingReason instance(Loan loan, BlockingReasonSetting blockingReasonSetting, boolean isActive, String comment,
            AppUser deactivatedBy, LocalDate deactivatedOn, LocalDate blockDate) {
        return new LoanBlockingReason(loan, blockingReasonSetting, isActive, comment, deactivatedBy, deactivatedOn, blockDate);
    }

    public static LoanBlockingReason instance(final Loan loan, final BlockingReasonSetting blockingReasonSetting, final String comment,
            final LocalDate blockDate) {
        return new LoanBlockingReason(loan, blockingReasonSetting, true, comment, null, null, blockDate);
    }

}
