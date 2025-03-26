package org.apache.fineract.portfolio.loanaccount.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class WriteOffChargesDetail {

    @Column(name = "writeoff_honorarios_amount", scale = 6, precision = 19)
    private BigDecimal honorarios;

    @Column(name = "writeoff_aval_amount", scale = 6, precision = 19)
    private BigDecimal aval;

    @Column(name = "writeoff_insurance_amount", scale = 6, precision = 19)
    private BigDecimal insurance;

    @Column(name = "writeoff_mandatory_insurance_amount", scale = 6, precision = 19)
    private BigDecimal mandatoryInsurance;

    @Column(name = "writeoff_arrears_interest_amount", scale = 6, precision = 19)
    private BigDecimal arrearInterest;

    public BigDecimal getHonorarios() {
        return honorarios;
    }

    public void setHonorarios(BigDecimal honorarios) {
        this.honorarios = honorarios;
    }

    public BigDecimal getAval() {
        return aval;
    }

    public void setAval(BigDecimal aval) {
        this.aval = aval;
    }

    public BigDecimal getInsurance() {
        return insurance;
    }

    public void setInsurance(BigDecimal insurance) {
        this.insurance = insurance;
    }

    public BigDecimal getMandatoryInsurance() {
        return mandatoryInsurance;
    }

    public void setMandatoryInsurance(BigDecimal mandatoryInsurance) {
        this.mandatoryInsurance = mandatoryInsurance;
    }

    public BigDecimal getArrearInterest() {
        return arrearInterest;
    }

    public void setArrearInterest(BigDecimal arrearInterest) {
        this.arrearInterest = arrearInterest;
    }
}
