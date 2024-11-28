package org.apache.fineract.portfolio.collectionhousemanagement.handler;

import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.collectionhousemanagement.service.CollectionHouseHistoryReadWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CommandType(entity = "COLLECTIONHOUSEHISTORY", action = "CREATE")
public class CollectionHouseCommandHandler implements NewCommandSourceHandler {

    private final CollectionHouseHistoryReadWriteService collectionHouseHistoryReadWriteService;

    @Autowired
    public CollectionHouseCommandHandler(CollectionHouseHistoryReadWriteService collectionHouseHistoryReadWriteService) {
        this.collectionHouseHistoryReadWriteService = collectionHouseHistoryReadWriteService;
    }

    @Transactional
    @Override
    public CommandProcessingResult processCommand(final JsonCommand command) {

        return this.collectionHouseHistoryReadWriteService.createCollectionHouseHistory(command);
    }
}
