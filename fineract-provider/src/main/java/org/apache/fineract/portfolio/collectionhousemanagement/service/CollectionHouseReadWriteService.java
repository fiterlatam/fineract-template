package org.apache.fineract.portfolio.collectionhousemanagement.service;

import java.util.Collection;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseConfigParameterizationData;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.CollectionHouseConfiguration;

public interface CollectionHouseReadWriteService {

    Collection<CollectionHouseConfigParameterizationData> retrieveAllCollectionHouseManagement();

    CollectionHouseConfigParameterizationData retrieveCollectionHouse(Long collectionId);

    CollectionHouseConfiguration retrieveCollectionHouseByClientFromHistory(String clientNit);

    CommandProcessingResult createCollectionHouseConfig(JsonCommand command);

    CommandProcessingResult updateCollectionHouseConfig(Long Id, JsonCommand command);
}
