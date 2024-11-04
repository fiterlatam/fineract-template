package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialWriteOffPayload {

    private Long loanId;
    private List<Map<String, Object>> charges;
    private BigDecimal principalPortion;
    private BigDecimal interestPortion;
    private BigDecimal totalWriteOffAmount;
    private String dateFormat;
    private String locale;
}
