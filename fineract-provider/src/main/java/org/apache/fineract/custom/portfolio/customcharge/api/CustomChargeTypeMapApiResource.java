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
package org.apache.fineract.custom.portfolio.customcharge.api;

import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.portfolio.customcharge.constants.CustomChargeTypeMapApiConstants;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeTypeMapData;
import org.apache.fineract.custom.portfolio.customcharge.service.CustomChargeTypeMapReadWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.data.GlobalEntityType;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookPopulatorService;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/v1/customchargeentities/map")
@Component
@Scope("singleton")
public class CustomChargeTypeMapApiResource {

    private final DefaultToApiJsonSerializer<CustomChargeTypeMapData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final PlatformSecurityContext context;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final CustomChargeTypeMapReadWritePlatformService customChargeTypeMapReadWritePlatformService;
    private final BulkImportWorkbookPopulatorService bulkImportWorkbookPopulatorService;
    private final BulkImportWorkbookService bulkImportWorkbookService;

    @Autowired
    public CustomChargeTypeMapApiResource(final DefaultToApiJsonSerializer<CustomChargeTypeMapData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, final PlatformSecurityContext context,
            final ApiRequestParameterHelper apiRequestParameterHelper,
            CustomChargeTypeMapReadWritePlatformService customChargeTypeMapReadWritePlatformService,
            BulkImportWorkbookPopulatorService bulkImportWorkbookPopulatorService, BulkImportWorkbookService bulkImportWorkbookService) {
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.context = context;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.customChargeTypeMapReadWritePlatformService = customChargeTypeMapReadWritePlatformService;
        this.bulkImportWorkbookPopulatorService = bulkImportWorkbookPopulatorService;
        this.bulkImportWorkbookService = bulkImportWorkbookService;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String get(@QueryParam("customChargeTypeId") @Parameter(description = "customChargeTypeId") final Long customChargeTypeId) {
        this.context.authenticatedUser().validateHasReadPermission(CustomChargeTypeMapApiConstants.RESOURCE_NAME);
        return this.toApiJsonSerializer.serialize(this.customChargeTypeMapReadWritePlatformService.findAllActive(customChargeTypeId));
    }

    @GET
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("id") @Parameter(description = "id") final Long id, @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(CustomChargeTypeMapApiConstants.RESOURCE_NAME);
        final CustomChargeTypeMapData data = this.customChargeTypeMapReadWritePlatformService.findById(id);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, data, CustomChargeTypeMapApiConstants.REQUEST_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createChargeMap(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCustomChargeTypeMap().withJson(apiRequestBodyAsJson)
                .build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateChargeMap(@PathParam("id") @Parameter(description = "id") final Long id,
            @Parameter(hidden = true) final String jsonRequestBody) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateCustomChargeTypeMap(id).withJson(jsonRequestBody).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String delete(@PathParam("id") @Parameter(description = "id") final Long id) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteCustomChargeTypeMap(id).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("downloadtemplate")
    @Produces("application/vnd.ms-excel")
    public Response clientTemplate(@QueryParam("dateFormat") final String dateFormat) {
        return bulkImportWorkbookPopulatorService.getTemplate(GlobalEntityType.CLIENT_VIP.name(), null, null, dateFormat);
    }

    @POST
    @Path("uploadtemplate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String createClientTemplate(@FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("locale") final String locale,
            @FormDataParam("dateFormat") final String dateFormat,
            @FormDataParam("apiRequestBodyAsJson") final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCustomChargeTypeMap().withJson(apiRequestBodyAsJson)
                .build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        final Long customChargeMapId = result.getResourceId();
        if (StringUtils.isNotBlank(fileDetail.getFileName())) {
            final Map<String, Object> importAttributes = new HashMap<>();
            importAttributes.put("customChargeMapId", customChargeMapId);
            final Long importDocumentId = bulkImportWorkbookService.importWorkbook(GlobalEntityType.CLIENT_VIP.name(), uploadedInputStream,
                    fileDetail, locale, dateFormat, importAttributes);
            return this.toApiJsonSerializer.serialize(importDocumentId);
        }
        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("uploadtemplate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String editClientTemplate(@FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("locale") final String locale,
            @FormDataParam("dateFormat") final String dateFormat, @FormDataParam("id") final Long id,
            @FormDataParam("apiRequestBodyAsJson") final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateCustomChargeTypeMap(id).withJson(apiRequestBodyAsJson)
                .build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        if (StringUtils.isNotBlank(fileDetail.getFileName())) {
            final Map<String, Object> importAttributes = new HashMap<>();
            importAttributes.put("customChargeMapId", id);
            final Long importDocumentId = bulkImportWorkbookService.importWorkbook(GlobalEntityType.CLIENT_VIP.name(), uploadedInputStream,
                    fileDetail, locale, dateFormat, importAttributes);
            return this.toApiJsonSerializer.serialize(importDocumentId);
        }
        return this.toApiJsonSerializer.serialize(result);
    }
}
