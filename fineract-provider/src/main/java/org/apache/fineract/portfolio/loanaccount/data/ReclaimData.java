package org.apache.fineract.portfolio.loanaccount.data;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ReclaimData {

    private List<LoanReclaimData> reclaimData;
    private List<LoanReclaimData> excludedData;
}
