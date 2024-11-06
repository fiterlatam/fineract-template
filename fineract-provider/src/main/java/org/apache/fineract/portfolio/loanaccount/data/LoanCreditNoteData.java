package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanCreditNoteData {

    private Long id;
    private Long loanId;
    private LocalDate creditNoteDate;
    private BigDecimal arrearInterest;
    private BigDecimal currentInterest;
    private BigDecimal honorarios;
    private BigDecimal aval;
    private BigDecimal insurance;
    private BigDecimal mandatoryInsurance;
    private BigDecimal capital;
    private BigDecimal totalAmount;
    private Long documentId;
    private String documentName;
    private Long transactionId;

}
