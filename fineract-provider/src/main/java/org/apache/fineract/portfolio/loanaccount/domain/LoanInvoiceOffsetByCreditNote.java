package org.apache.fineract.portfolio.loanaccount.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicaMensual;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.LoanDocumentConcept;

@Entity
@Table(name = "m_invoice_offset_by_credit_note")
@Getter
@Setter
public class LoanInvoiceOffsetByCreditNote extends AbstractAuditableWithUTCDateTimeCustom {

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private FacturaElectronicaMensual facturaElectronicaMensual;

    @ManyToOne
    @JoinColumn(name = "credit_note_id", nullable = false)
    private LoanCreditNote loanCreditNote;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "interest_portion")
    private BigDecimal interestPortion;

    @Column(name = "penalty_portion")
    private BigDecimal penaltyPortion;

    @Column(name = "honorarios_portion")
    private BigDecimal honorariosPortion;

    @Column(name = "mandatory_insurance_portion")
    private BigDecimal mandatoryInsurancePortion;

    @Column(name = "voluntary_insurance_portion")
    private BigDecimal voluntaryInsurancePortion;

    @Column(name = "is_active")
    private boolean isActive;

    private void resetAllPortions() {
        this.interestPortion = BigDecimal.ZERO;
        this.penaltyPortion = BigDecimal.ZERO;
        this.honorariosPortion = BigDecimal.ZERO;
        this.mandatoryInsurancePortion = BigDecimal.ZERO;
        this.voluntaryInsurancePortion = BigDecimal.ZERO;
    }

    public void adjustPortionByConcept(final LoanDocumentConcept loanDocumentConcept, final BigDecimal amountToBeOffset) {
        this.amount = amountToBeOffset;
        this.resetAllPortions();
        switch (loanDocumentConcept) {
            case INT_CORRIENTE:
                this.interestPortion = amountToBeOffset;
            break;
            case INT_DE_MORA:
                this.penaltyPortion = amountToBeOffset;
            break;
            case HONORARIOS:
                this.honorariosPortion = amountToBeOffset;
            break;
            case SEGURO_OBLIGATORIO:
                this.mandatoryInsurancePortion = amountToBeOffset;
            break;
            case SEGUROS_VOLUNTARIOS:
                this.voluntaryInsurancePortion = amountToBeOffset;
            break;
            default:
            break;
        }
    }
}
