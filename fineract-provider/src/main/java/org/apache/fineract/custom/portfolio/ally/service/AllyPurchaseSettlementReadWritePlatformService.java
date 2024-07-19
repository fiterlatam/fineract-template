package org.apache.fineract.custom.portfolio.ally.service;

import java.util.List;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyPointOfSalesCollectionData;
import org.apache.fineract.custom.portfolio.ally.domain.AllyPurchaseSettlement;

public interface AllyPurchaseSettlementReadWritePlatformService {

    List<ClientAllyPointOfSalesCollectionData> getPurchaseData();

    //
    ClientAllyPointOfSalesCollectionData getPurchaseDataByLoanId(Long loanId);

    List<ClientAllyPointOfSalesCollectionData> getPurchaseDataByClientAllyId(Long clientId);

    void create(AllyPurchaseSettlement allyPurchaseSettlement);

    void update(AllyPurchaseSettlement allyPurchaseSettlement);
}
