package org.apache.fineract.custom.portfolio.ally.data;

import java.math.BigDecimal;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AllySettlementCompansationCollectionData {

    private String nit;
    private String companyName;
    private Long clientAllyId;
    private Long bankId;
    private String startDate;
    private String endDate;
    private String accountType;
    private String accountNumber;
    private BigDecimal purchaseAmount;
    private BigDecimal collectionAmount;
    private BigDecimal vaComissionAmount;
    private BigDecimal purchaceSettlementAmount;
    private BigDecimal netOutstandingAmount;
    private String lastCollectionDate;
    private String lastPurchaseDate;

}
