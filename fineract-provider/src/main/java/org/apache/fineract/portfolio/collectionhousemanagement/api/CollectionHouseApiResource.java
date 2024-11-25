package org.apache.fineract.portfolio.collectionhousemanagement.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseConfigParameterizationData;
import org.apache.fineract.portfolio.collectionhousemanagement.service.CollectionHouseReadWriteService;
import org.springframework.stereotype.Component;

@Path("/v1/collectionhousemanagement")
@Component
@Tag(name = "Collection House Management", description = "Product configuration for Collection House Management")
@RequiredArgsConstructor
public class CollectionHouseApiResource {

    private static final String COLLECTION_HOUSE_PERMISSIONS = "COLLECTION_HOUSE";
    private final PlatformSecurityContext context;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer<CollectionHouseConfigParameterizationData> toApiJsonSerializer;
    private final CollectionHouseReadWriteService collectionHouseReadWriteService;

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll(@Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(COLLECTION_HOUSE_PERMISSIONS);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        final Collection<CollectionHouseConfigParameterizationData> collectionHouseConfigParameterizationData = collectionHouseReadWriteService
                .retrieveAllCollectionHouseManagement();
        return this.toApiJsonSerializer.serialize(settings, collectionHouseConfigParameterizationData);
    }

    @GET
    @Path("{collectionId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOneCollectionHouse(@PathParam("collectionId") final Long collectionId, @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(COLLECTION_HOUSE_PERMISSIONS);
        final CollectionHouseConfigParameterizationData collectionHouseConfigParameterizationData = collectionHouseReadWriteService
                .retrieveCollectionHouse(collectionId);
        return this.toApiJsonSerializer.serialize(collectionHouseConfigParameterizationData);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createCollectionHouse(final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCollectionHouse().withJson(apiRequestBodyAsJson).build();
        final CommandProcessingResult commandProcessingResult = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(commandProcessingResult);
    }

    @PUT
    @Path("{collectionId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String UpdateCollectionHouse(@PathParam("collectionId") final Long collectionId, final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateCollectionHouse(collectionId).withJson(apiRequestBodyAsJson)
                .build();
        final CommandProcessingResult commandProcessingResult = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(commandProcessingResult);
    }

}
