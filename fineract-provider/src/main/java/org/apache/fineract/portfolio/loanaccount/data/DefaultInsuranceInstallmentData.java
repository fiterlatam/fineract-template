package org.apache.fineract.portfolio.loanaccount.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DefaultInsuranceInstallmentData {
    private Long loanId;
    private Long loanChargeId;
    private Integer installment;

    public Long loanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public Long loanChargeId() {
        return loanChargeId;
    }

    public void setLoanChargeId(Long loanChargeId) {
        this.loanChargeId = loanChargeId;
    }

    public Integer installment() {
        return installment;
    }

    public void setInstallment(Integer installment) {
        this.installment = installment;
    }
}
