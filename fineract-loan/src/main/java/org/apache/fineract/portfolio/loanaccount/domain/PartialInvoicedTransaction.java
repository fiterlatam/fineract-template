package org.apache.fineract.portfolio.loanaccount.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "m_partial_invoiced_transaction")
@Getter
@Setter
public class PartialInvoicedTransaction extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "repayment_transaction_id")
    private LoanTransaction repaymentTransaction;

    @ManyToOne
    @JoinColumn(name = "accrual_transaction_id")
    private LoanTransaction accrualTransaction;

    @Column(name = "interest")
    private BigDecimal interest;

    @Column(name = "honorarios")
    private BigDecimal honorarios;

    @Column(name = "honorarios_vat")
    private BigDecimal honorariosVat;

    @Column(name = "mandatory_insurance")
    private BigDecimal mandatoryInsurance;

    @Column(name = "mandatory_insurance_vat")
    private BigDecimal mandatoryInsuranceVat;

    @Column(name = "voluntary_insurance")
    private BigDecimal voluntaryInsurance;

    @Column(name = "voluntary_insurance_vat")
    private BigDecimal voluntaryInsuranceVat;

    @Column(name = "penalty")
    private BigDecimal penalty;

    @Column(name = "penalty_vat")
    private BigDecimal penaltyVat;

    public BigDecimal getInterest() {
        return defaultToZeroIfNull(interest);
    }

    public BigDecimal getHonorarios() {
        return defaultToZeroIfNull(honorarios);
    }

    public BigDecimal getHonorariosVat() {
        return defaultToZeroIfNull(honorariosVat);
    }

    public BigDecimal getMandatoryInsurance() {
        return defaultToZeroIfNull(mandatoryInsurance);
    }

    public BigDecimal getMandatoryInsuranceVat() {
        return defaultToZeroIfNull(mandatoryInsuranceVat);
    }

    public BigDecimal getVoluntaryInsurance() {
        return defaultToZeroIfNull(voluntaryInsurance);
    }

    public BigDecimal getVoluntaryInsuranceVat() {
        return defaultToZeroIfNull(voluntaryInsuranceVat);
    }

    public BigDecimal getPenalty() {
        return defaultToZeroIfNull(penalty);
    }

    public BigDecimal getPenaltyVat() {
        return defaultToZeroIfNull(penaltyVat);
    }

    private BigDecimal defaultToZeroIfNull(final BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
