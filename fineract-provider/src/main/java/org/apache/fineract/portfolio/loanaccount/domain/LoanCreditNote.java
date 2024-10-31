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

    @Column(name = "capital", scale = 6, precision = 19)
    private BigDecimal capital;

    @Column(name = "total_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal totalAmount;

    public LoanCreditNoteData toData() {
        Long documentId = null;
        String documentName = null;
        if (this.document != null) {
            documentId = this.document.getId();
            documentName = this.document.getName();
        }
        return new LoanCreditNoteData(this.getId(), loan.getId(), this.creditNoteDate, this.arrearInterest, this.currentInterest,
                this.honorarios, this.aval, this.insurance, this.capital, this.totalAmount, documentId, documentName);
    }
}
