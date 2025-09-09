package org.apache.fineract.custom.portfolio.blockaccounts.data;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanAccountBlockDTO {

    private Long id;
    private Long loanId;
    private Long blockingReasonId;
    private String blockingReasonName;
    private LocalDate applicationDate;
    private Boolean accelerate;
    private Boolean freezeCurrentInterest;
    private Boolean freezeInterestArrears;
    private Boolean freezeLifeInsurance;
    private Boolean freezeMypime;
    private Boolean active;
}
