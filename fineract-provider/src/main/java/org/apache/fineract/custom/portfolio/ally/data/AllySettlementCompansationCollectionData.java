package org.apache.fineract.custom.portfolio.ally.data;

import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AllySettlementCompansationCollectionData {
    private String startDate;
    private String endDate;
    private Long clientAllyId;
    private String nit;
    private String companyName;
    private String bankName;
    private String accountType;
    private String accountNumber;
    private BigDecimal purchaseAmount;
    private BigDecimal comissionAmount;
    private BigDecimal vaComissionAmount;
    private BigDecimal netPurchaseAmount;
    private BigDecimal collectionAmount;
    private BigDecimal compensationAmount;
    private String lastCollectionDate;
    private String lastPurchaseDate;
}
