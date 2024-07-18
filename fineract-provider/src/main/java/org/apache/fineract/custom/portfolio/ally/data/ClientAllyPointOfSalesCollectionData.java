package org.apache.fineract.custom.portfolio.ally.data;

import java.math.BigDecimal;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ClientAllyPointOfSalesCollectionData {

    private String collectionDate;
    private String nit;
    private String name;
    private Long clientAllyId;
    private Long liquidationFrequencyId;
    private Long pointOfSalesId;
    private String pointOfSalesName;
    private Long cityId;
    private String cityName;
    private BigDecimal amount;
    private Integer settledComission;
    private Integer taxId;
    private Long channelId;
    private Long loanId;
    private Long clientId;
    private String lastJobsRun;
    private Integer loanStatusId;

}
