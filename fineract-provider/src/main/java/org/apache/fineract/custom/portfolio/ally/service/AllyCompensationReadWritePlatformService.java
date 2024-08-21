package org.apache.fineract.custom.portfolio.ally.service;

import java.util.List;
import org.apache.fineract.custom.portfolio.ally.data.AllySettlementCompansationCollectionData;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensation;

public interface AllyCompensationReadWritePlatformService {

    List<AllySettlementCompansationCollectionData> getListCompensationSettlement();

    void create(AllyCompensation allyCollectionSettlement);

    void update(AllyCompensation allyCollectionSettlement);
}
