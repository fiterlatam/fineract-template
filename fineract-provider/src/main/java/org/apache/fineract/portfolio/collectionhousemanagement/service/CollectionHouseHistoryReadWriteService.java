package org.apache.fineract.portfolio.collectionhousemanagement.service;

import java.io.IOException;
import java.util.List;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseUpdate;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseUpdates;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.ColletionHouseHistory;

public interface CollectionHouseHistoryReadWriteService {

    CommandProcessingResult createCollectionHouseHistory(JsonCommand command);

    void createCollectionHouseHistory(List<CollectionHouseUpdate> list);

    CommandProcessingResult updateCollectionHouseHistory(JsonCommand command);

    ColletionHouseHistory findCollectionHouseHistoryByAcctountNo(String accountNo);

    List<ColletionHouseHistory> findAllCollectionHouseHistory();

    CollectionHouseUpdates fetchDataFromExternalProvider() throws IOException;
}
