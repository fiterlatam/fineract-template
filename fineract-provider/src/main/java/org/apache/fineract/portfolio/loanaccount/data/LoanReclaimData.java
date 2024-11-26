package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class LoanReclaimData {

    private Long id;
    private String clientName;
    private String loanAccountNumber;
    private String productName;
    private Long daysInArrears;
    private BigDecimal outstandingPrincipalAmount;
    private BigDecimal outstandingInterestAmount;
    private BigDecimal outstandingAvalAmount;
    private BigDecimal outstandingMandatoryInsuranceAmount;
    private BigDecimal outstandingAllOtherChargesAmount;
    private BigDecimal outstandingPenaltyAmount;
    private BigDecimal outstandingTotalAmount;

}
