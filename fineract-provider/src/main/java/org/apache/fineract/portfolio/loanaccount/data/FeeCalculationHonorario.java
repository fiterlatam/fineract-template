package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class FeeCalculationHonorario {

    private final BigDecimal delinquentPortion;
    private final BigDecimal feeWithTax;
    private final BigDecimal feeBasis;
    private final BigDecimal feeVat;
    private final BigDecimal feeHono;
}
