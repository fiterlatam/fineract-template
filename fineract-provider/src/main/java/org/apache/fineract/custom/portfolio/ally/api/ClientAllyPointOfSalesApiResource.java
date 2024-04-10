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
package org.apache.fineract.custom.portfolio.ally.api;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.infrastructure.bulkimport.data.CustomGlobalEntityType;
import org.apache.fineract.custom.infrastructure.bulkimport.service.CustomBulkImportWorkbookPopulatorServiceImpl;
import org.apache.fineract.custom.infrastructure.bulkimport.service.CustomBulkImportWorkbookServiceImpl;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyPointOfSalesData;
import org.apache.fineract.custom.portfolio.ally.service.ClientAllyPointOfSalesReadWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.UploadRequest;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/v1/clientally/{parentId}/pointofsales")
@Component
@Scope("singleton")
public class ClientAllyPointOfSalesApiResource {

    private final DefaultToApiJsonSerializer<ClientAllyPointOfSalesData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final PlatformSecurityContext context;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final CustomBulkImportWorkbookPopulatorServiceImpl customWorkbookPopulatorService;
    private final CustomBulkImportWorkbookServiceImpl customBulkImportWorkbookService;

    @Autowired
    public ClientAllyPointOfSalesApiResource(final DefaultToApiJsonSerializer<ClientAllyPointOfSalesData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, final PlatformSecurityContext context,
            final ApiRequestParameterHelper apiRequestParameterHelper,
            final CustomBulkImportWorkbookPopulatorServiceImpl customWorkbookPopulatorService,
            final CustomBulkImportWorkbookServiceImpl customBulkImportWorkbookService) {
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.context = context;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.customWorkbookPopulatorService = customWorkbookPopulatorService;
        this.customBulkImportWorkbookService = customBulkImportWorkbookService;
    }

    @Autowired
    private ClientAllyPointOfSalesReadWritePlatformService service;

    @GET
    @Path("/template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getTemplate(@Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(ClientAllyApiConstants.RESOURCE_NAME);
        return this.toApiJsonSerializer.serialize(this.service.getTemplateForInsertAndUpdate());
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String get(@Context final UriInfo uriInfo, @PathParam("parentId") @Parameter(description = "parentId") final Long parentId,
            @QueryParam("sqlSearch") @Parameter(description = "sqlSearch") final String sqlSearch) {

        this.context.authenticatedUser().validateHasReadPermission(ClientAllyPointOfSalesApiConstants.RESOURCE_NAME);
        return this.toApiJsonSerializer.serialize(this.service.findByName(parentId, sqlSearch));
    }

    @GET
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("id") @Parameter(description = "id") final Long id, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(ClientAllyPointOfSalesApiConstants.RESOURCE_NAME);

        final ClientAllyPointOfSalesData data = this.service.findById(id);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return this.toApiJsonSerializer.serialize(settings, data, ClientAllyPointOfSalesApiConstants.REQUEST_DATA_PARAMETERS);
    }

    @GET
    @Path("/downloadtemplate")
    @Produces("application/vnd.ms-excel")
    public Response downloadTemplate(@QueryParam("officeId") final Long officeId, @QueryParam("dateFormat") final String dateFormat) {

        this.context.authenticatedUser().validateHasReadPermission(ClientAllyApiConstants.RESOURCE_NAME);

        return this.customWorkbookPopulatorService.getTemplate(CustomGlobalEntityType.CLIENT_ALLY_POINTS_OF_SALES.getCode(), officeId, null,
                dateFormat);
    }

    @POST
    @Path("uploadtemplate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestBody(description = "Upload staff template", content = {
            @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = UploadRequest.class)) })
    public String postTemplate(@FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("locale") final String locale,
            @FormDataParam("dateFormat") final String dateFormat) {
        final Long importDocumentId = customBulkImportWorkbookService.importWorkbook(
                CustomGlobalEntityType.CLIENT_ALLY_POINTS_OF_SALES.getAlias(), uploadedInputStream, fileDetail, locale, dateFormat);
        return toApiJsonSerializer.serialize(importDocumentId);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createNewHoliday(@Parameter(hidden = true) final String apiRequestBodyAsJson,
            @PathParam("parentId") @Parameter(description = "parentId") final Long parentId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createClientAllyPointOfSales(parentId)
                .withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String update(@PathParam("id") @Parameter(description = "id") final Long id,
            @PathParam("parentId") @Parameter(description = "parentId") final Long parentId,
            @Parameter(hidden = true) final String jsonRequestBody) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateClientAllyPointOfSales(parentId, id)
                .withJson(jsonRequestBody).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String delete(@PathParam("id") @Parameter(description = "id") final Long id) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteClientAllyPointOfSales(id).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }
}
