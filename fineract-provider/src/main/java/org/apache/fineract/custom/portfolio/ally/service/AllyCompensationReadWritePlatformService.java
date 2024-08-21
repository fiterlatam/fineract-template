package org.apache.fineract.custom.portfolio.ally.service;

import java.util.List;
import org.apache.fineract.custom.portfolio.ally.data.AllySettlementCompansationCollectionData;

public interface AllyCompensationReadWritePlatformService {

    List<AllySettlementCompansationCollectionData> getListCompensationSettlement();
}
