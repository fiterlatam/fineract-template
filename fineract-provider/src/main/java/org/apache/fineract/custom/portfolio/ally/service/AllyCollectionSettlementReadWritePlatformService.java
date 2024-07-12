package org.apache.fineract.custom.portfolio.ally.service;

import java.util.List;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyPointOfSalesCollectionData;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCollectionSettlement;

public interface AllyCollectionSettlementReadWritePlatformService {

    List<ClientAllyPointOfSalesCollectionData> getCollectionData();

    void create(AllyCollectionSettlement allyCollectionSettlement);

    void update(AllyCollectionSettlement allyCollectionSettlement);
}
