package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class LoanWriteOffChargeData {

    private BigDecimal arrearInterest;
    private BigDecimal honorarios;
    private BigDecimal aval;
    private BigDecimal insurance;
    private BigDecimal mandatoryInsurance;
    private BigDecimal capital;
    private BigDecimal totalAmount;

    public BigDecimal getArrearInterest() {
        return arrearInterest;
    }

    public void setArrearInterest(BigDecimal arrearInterest) {
        this.arrearInterest = arrearInterest;
    }

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

    public BigDecimal getCapital() {
        return capital;
    }

    public void setCapital(BigDecimal capital) {
        this.capital = capital;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public static LoanWriteOffChargeData initWithZeroAmounts() {
        return new LoanWriteOffChargeData(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
