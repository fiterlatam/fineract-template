package org.apache.fineract.custom.portfolio.ally.data;

import java.math.BigDecimal;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AllySettlementCompansationCollectionData {

    private String purchaseDate;
    private String nit;
    private String companyName;
    private Long clientAllyId;
    private Long bankId;
    private String accountType;
    private String accountNumber;
    private BigDecimal purchaseAmount;
    private BigDecimal collectionAmount;
    private BigDecimal vaCommissionAmount;
    private BigDecimal purchaceSettlementAmount;
    private BigDecimal netOutstandingAmount;

}
