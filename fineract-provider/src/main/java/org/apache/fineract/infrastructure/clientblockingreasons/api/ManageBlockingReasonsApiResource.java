/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.clientblockingreasons.api;

import static org.apache.fineract.infrastructure.bulkimport.data.GlobalEntityType.CLIENT_BLOCK;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookPopulatorService;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookService;
import org.apache.fineract.infrastructure.clientblockingreasons.data.BlockingReasonsData;
import org.apache.fineract.infrastructure.clientblockingreasons.service.ManageBlockingReasonsReadPlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.UploadRequest;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

@Path(BlockingReasonsConstants.RESOURCE_URL)
@Component
@Tag(name = "Manage Blocking Reasons", description = "Manage Blocking Reasons settings for client Account")
@RequiredArgsConstructor
public class ManageBlockingReasonsApiResource {

    private final PlatformSecurityContext context;
    private final ManageBlockingReasonsReadPlatformService manageBlockingReasonsReadPlatformService;
    private final ToApiJsonSerializer<BlockingReasonsData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private static final Set<String> MANAGE_BLOCKING_REASONS_DATA_PARAMETERS = new HashSet<>(Arrays.asList(
            BlockingReasonsConstants.ID_PARAM, BlockingReasonsConstants.CREDIT_LEVEL_PARAM, BlockingReasonsConstants.CUSTOMER_LEVEL_PARAM,
            BlockingReasonsConstants.CREDIT_LEVEL_OPTIONS_PARAM, BlockingReasonsConstants.CUSTOMER_LEVEL_OPTIONS_PARAM));
    private final BulkImportWorkbookPopulatorService bulkImportWorkbookPopulatorService;
    private final BulkImportWorkbookService bulkImportWorkbookService;

    @POST
    @Path("createBlockingReasonSettings")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createBlockReasonSetting(@Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createBlockReasonSetting().withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("retrieveAllBlockingReasons")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAllBlockingReasons(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(BlockingReasonsConstants.ENTITY_NAME);

        Collection<BlockingReasonsData> blockingReasonsDataCollection = this.manageBlockingReasonsReadPlatformService
                .retrieveAllBlockingReasons(null);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, blockingReasonsDataCollection, MANAGE_BLOCKING_REASONS_DATA_PARAMETERS);
    }

    @GET
    @Path("getBlockingReasonsById/{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getBlockingReasonsById(@Context final UriInfo uriInfo, @PathParam("id") final Long id) {

        this.context.authenticatedUser().validateHasReadPermission(BlockingReasonsConstants.ENTITY_NAME);

        BlockingReasonsData blockingReasonsDataCollection = this.manageBlockingReasonsReadPlatformService.getBlockingReasonsById(id);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, blockingReasonsDataCollection, MANAGE_BLOCKING_REASONS_DATA_PARAMETERS);
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateBlockReasonSetting(@PathParam("id") final Long id, @Parameter(hidden = true) final String apiRequestBodyAsJson,
            @QueryParam("command") final String command) {

        CommandWrapper commandRequest = null;
        if ("disable".equalsIgnoreCase(command)) {
            commandRequest = new CommandWrapperBuilder().disableBlockReasonSetting(id).withJson(apiRequestBodyAsJson).build();
        } else if ("enable".equalsIgnoreCase(command)) {
            commandRequest = new CommandWrapperBuilder().enableBlockReasonSetting(id).build();
        } else {
            commandRequest = new CommandWrapperBuilder().updateBlockReasonSetting(id).withJson(apiRequestBodyAsJson).build();
        }

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("downloadtemplate")
    @Produces("application/vnd.ms-excel")
    public Response getBlockClientListTemplate(final Long staffId, @QueryParam("dateFormat") final String dateFormat) {
        return bulkImportWorkbookPopulatorService.getTemplate(CLIENT_BLOCK.toString(), null, staffId, dateFormat);
    }

    @POST
    @Path("uploadtemplate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestBody(description = "Upload block client list template", content = {
            @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = UploadRequest.class)) })
    public String postBlockClientList(@FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("locale") final String locale,
            @FormDataParam("dateFormat") final String dateFormat) {
        Long importDocumentId = bulkImportWorkbookService.importWorkbook(CLIENT_BLOCK.toString(), uploadedInputStream, fileDetail, locale,
                dateFormat, null);
        return this.toApiJsonSerializer.serialize(importDocumentId);
    }

}
