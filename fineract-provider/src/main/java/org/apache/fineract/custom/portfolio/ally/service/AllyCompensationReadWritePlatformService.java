package org.apache.fineract.custom.portfolio.ally.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.fineract.custom.portfolio.ally.data.AllySettlementCompansationCollectionData;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllySettlementData;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensation;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;

public interface AllyCompensationReadWritePlatformService {

    List<AllySettlementCompansationCollectionData> getListCompensationSettlement();

    Optional<AllySettlementCompansationCollectionData> getCompensationSettlementByNit(String nit, LocalDate startDate, LocalDate endDate);

    List<ClientAllySettlementData> getClientAllySettlement();

    void create(AllyCompensation allyCollectionSettlement);

    CommandProcessingResult update(JsonCommand command, Long id);
}
