package org.apache.fineract.custom.portfolio.ally.handler;

import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.custom.portfolio.ally.service.AllyCompensationReadWritePlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CommandType(entity = "CLIENTALLYCOMPENSATION", action = "UPDATE")
public class UpdateClientAllyCompensation implements NewCommandSourceHandler {

    @Autowired
    private AllyCompensationReadWritePlatformService service;

    @Transactional
    @Override
    public CommandProcessingResult processCommand(final JsonCommand command) {

        return service.update(command, command.entityId());
    }

}
