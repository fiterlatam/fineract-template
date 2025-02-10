package org.apache.fineract.portfolio.collectionhousemanagement.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.data.CustomChargeHonorarioMapData;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseUpdate;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseUpdates;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.ColletionHouseHistory;
import org.apache.fineract.portfolio.collectionhousemanagement.service.CollectionHouseHistoryReadWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/v1/collectionhousehistory")
@Component
@Scope("singleton")
public class CollectionHouseHistoryApiResource {

    private static final String COLLECTION_HOUSE_PERMISSIONS = "COLLECTION_HOUSE";
    private final DefaultToApiJsonSerializer<CustomChargeHonorarioMapData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final PlatformSecurityContext context;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final CollectionHouseHistoryReadWriteService collectionHouseHistoryReadWriteService;

    @Autowired
    public CollectionHouseHistoryApiResource(DefaultToApiJsonSerializer<CustomChargeHonorarioMapData> toApiJsonSerializer,
            PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, PlatformSecurityContext context,
            ApiRequestParameterHelper apiRequestParameterHelper,
            CollectionHouseHistoryReadWriteService collectionHouseHistoryReadWriteService) {
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.context = context;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.collectionHouseHistoryReadWriteService = collectionHouseHistoryReadWriteService;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createCollectionHouse(final String apiRequestBodyAsJson) {
        this.context.authenticatedUser().validateHasReadPermission(COLLECTION_HOUSE_PERMISSIONS);
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCollectionHouseHistory().withJson(apiRequestBodyAsJson)
                .build();
        final CommandProcessingResult commandProcessingResult = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(commandProcessingResult);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getCollectionHouseHistory(@Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(COLLECTION_HOUSE_PERMISSIONS);
        List<ColletionHouseHistory> collectionHouseHistoryList = collectionHouseHistoryReadWriteService.findAllCollectionHouseHistory();

        CollectionHouseUpdates updates = new CollectionHouseUpdates();
        for (ColletionHouseHistory history : collectionHouseHistoryList) {
            CollectionHouseUpdate data = new CollectionHouseUpdate();
            data.setCollectionHouseCode(history.getCollectionCode());
            data.setClientAccountNo(history.getClientAccountNumber());
            data.setNit(history.getCollectionNit());
            updates.getCollectionHouseUpdates().add(data);
        }
        return this.toApiJsonSerializer.serialize(updates);
    }

}
