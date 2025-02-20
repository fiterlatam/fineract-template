package org.apache.fineract.portfolio.loanaccount.loanschedule.data;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeeDetails {

    private final String chargeName;
    private final BigDecimal expected;
    private final BigDecimal paid;
    private final BigDecimal due;

    public FeeDetails(String chargeName, BigDecimal expected, BigDecimal paid, BigDecimal due) {
        this.chargeName = chargeName;
        this.expected = expected;
        this.paid = paid;
        this.due = due;
    }
}
