package org.apache.fineract.portfolio.loanaccount.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ReclaimData {
    private List<LoanReclaimData> reclaimData;
    private List<LoanReclaimData> excludedData;
}
