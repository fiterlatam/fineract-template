package org.apache.fineract.custom.portfolio.ally.data;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ClientAllySettlementData {
    private Long clientAllyId;
    private String nit;
    private String collectionDate;
    private String purchaseDate;
    private String lastClientCollectionJobRun;
    private String lastClientPurchaseJobRun;

}
