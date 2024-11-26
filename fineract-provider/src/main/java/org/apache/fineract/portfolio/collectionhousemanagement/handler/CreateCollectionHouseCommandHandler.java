package org.apache.fineract.portfolio.collectionhousemanagement.handler;

import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.collectionhousemanagement.service.CollectionHouseReadWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CommandType(entity = "PRODUCTCOLLECTIONHOUSE", action = "CREATE")
public class CreateCollectionHouseCommandHandler implements NewCommandSourceHandler {

    private final CollectionHouseReadWriteService collectionHouseConfigReadWriteServices;

    @Autowired
    public CreateCollectionHouseCommandHandler(final CollectionHouseReadWriteService collectionHouseConfigReadWriteServices) {
        this.collectionHouseConfigReadWriteServices = collectionHouseConfigReadWriteServices;
    }

    @Transactional
    @Override
    public CommandProcessingResult processCommand(final JsonCommand command) {

        return this.collectionHouseConfigReadWriteServices.createCollectionHouseConfig(command);
    }
}
