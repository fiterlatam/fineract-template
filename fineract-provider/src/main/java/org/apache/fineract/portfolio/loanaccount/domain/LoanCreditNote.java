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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.documentmanagement.domain.Document;
import org.apache.fineract.portfolio.loanaccount.data.LoanCreditNoteData;

@Entity
@Table(name = "m_loan_credit_note")
@Getter
@Setter
public class LoanCreditNote extends AbstractAuditableWithUTCDateTimeCustom {

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @OneToOne
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(name = "credit_note_date", nullable = false)
    private LocalDate creditNoteDate;

    @Column(name = "arrear_interest", scale = 6, precision = 19)
    private BigDecimal arrearInterest;

    @Column(name = "current_interest", scale = 6, precision = 19)
    private BigDecimal currentInterest;

    @Column(name = "honorarios", scale = 6, precision = 19)
    private BigDecimal honorarios;

    @Column(name = "aval", scale = 6, precision = 19)
    private BigDecimal aval;

    @Column(name = "insurance", scale = 6, precision = 19)
    private BigDecimal insurance;

    @Column(name = "mandatory_insurance", scale = 6, precision = 19)
    private BigDecimal mandatoryInsurance;

    @Column(name = "capital", scale = 6, precision = 19)
    private BigDecimal capital;

    @Column(name = "total_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    public LoanCreditNoteData toData() {
        Long documentId = null;
        String documentName = null;
        if (this.document != null) {
            documentId = this.document.getId();
            documentName = this.document.getName();
        }
        return new LoanCreditNoteData(this.getId(), loan.getId(), this.creditNoteDate, this.arrearInterest, this.currentInterest,
                this.honorarios, this.aval, this.insurance, this.mandatoryInsurance, this.capital, this.totalAmount, documentId,
                documentName, this.transactionId);
    }

    public void calculateTotalAmount() {
        this.totalAmount = this.arrearInterest.add(this.currentInterest).add(this.honorarios).add(this.aval).add(this.insurance)
                .add(this.capital).add(this.mandatoryInsurance);
    }

    public boolean includesCharges() {
        return this.honorarios.compareTo(BigDecimal.ZERO) > 0 || this.aval.compareTo(BigDecimal.ZERO) > 0
                || this.insurance.compareTo(BigDecimal.ZERO) > 0 || this.mandatoryInsurance.compareTo(BigDecimal.ZERO) > 0
                || this.arrearInterest.compareTo(BigDecimal.ZERO) > 0;
    }

    public void resetCharges() {
        this.honorarios = BigDecimal.ZERO;
        this.aval = BigDecimal.ZERO;
        this.insurance = BigDecimal.ZERO;
        this.mandatoryInsurance = BigDecimal.ZERO;
        this.arrearInterest = BigDecimal.ZERO;
    }
}
