package org.apache.fineract.portfolio.loanproduct.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;

import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MaximumCreditRateTemplateData {
    private Collection<CodeValueData> productTypeOptions;
    private Collection<MaximumCreditRateConfigurationData> rates;
}
