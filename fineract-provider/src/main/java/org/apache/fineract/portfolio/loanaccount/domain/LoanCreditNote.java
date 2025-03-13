package org.apache.fineract.portfolio.loanaccount.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.documentmanagement.domain.Document;
import org.apache.fineract.portfolio.loanaccount.data.LoanCreditNoteData;
import org.apache.fineract.portfolio.loanaccount.invoice.data.LoanDocumentData;

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

    @Column(name = "is_fully_used_by_invoice")
    private boolean isFullyUsedByInvoice;

    @Column(name = "mandatory_insurance_vat")
    private BigDecimal mandatoryInsuranceVat;

    @Column(name = "voluntary_insurance_vat")
    private BigDecimal voluntaryInsuranceVat;

    @Column(name = "honorarios_vat")
    private BigDecimal honorariosVat;

    @Column(name = "penalty_vat")
    private BigDecimal penaltyVat;

    @OneToMany(mappedBy = "loanCreditNote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<LoanInvoiceOffsetByCreditNote> loanInvoiceOffsetByCreditNoteSet = new HashSet<>();

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

    public void addPortionsFromLoanTransaction(final LoanDocumentData creditNoteTransactionData) {
        this.arrearInterest = creditNoteTransactionData.getPenaltyChargesPaid();
        this.penaltyVat = creditNoteTransactionData.getPenaltyChargesVatPaid();
        this.honorarios = creditNoteTransactionData.getHonorariosPaid();
        this.honorariosVat = creditNoteTransactionData.getHonorariosVatPaid();
        this.insurance = creditNoteTransactionData.getVoluntaryInsurancePaid();
        this.voluntaryInsuranceVat = creditNoteTransactionData.getVoluntaryInsuranceVatPaid();
        this.mandatoryInsurance = creditNoteTransactionData.getMandatoryInsurancePaid();
        this.mandatoryInsuranceVat = creditNoteTransactionData.getMandatoryInsuranceVatPaid();
    }

    public BigDecimal getArrearInterest() {
        return this.arrearInterest == null ? BigDecimal.ZERO : this.arrearInterest;
    }

    public BigDecimal getCurrentInterest() {
        return this.currentInterest == null ? BigDecimal.ZERO : this.currentInterest;
    }

    public BigDecimal getHonorarios() {
        return this.honorarios == null ? BigDecimal.ZERO : this.honorarios;
    }

    public BigDecimal getInsurance() {
        return this.insurance == null ? BigDecimal.ZERO : this.insurance;
    }

    public BigDecimal getMandatoryInsurance() {
        return this.mandatoryInsurance == null ? BigDecimal.ZERO : this.mandatoryInsurance;
    }

    public BigDecimal getMandatoryInsuranceVat() {
        return this.mandatoryInsuranceVat == null ? BigDecimal.ZERO : this.mandatoryInsuranceVat;
    }

    public BigDecimal getVoluntaryInsuranceVat() {
        return this.voluntaryInsuranceVat == null ? BigDecimal.ZERO : this.voluntaryInsuranceVat;
    }

    public BigDecimal getHonorariosVat() {
        return this.honorariosVat == null ? BigDecimal.ZERO : this.honorariosVat;
    }

    public BigDecimal getPenaltyVat() {
        return this.penaltyVat == null ? BigDecimal.ZERO : this.penaltyVat;
    }

}
