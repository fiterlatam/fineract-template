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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.portfolio.customcharge.constants.CustomChargeTypeMapApiConstants;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeTypeMapData;
import org.apache.fineract.custom.portfolio.customcharge.service.CustomChargeTypeMapReadWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/v1/customchargeentities/{chargeEntityid}/customchargetypes/{customChargeTypeId}/map")
@Component
@Scope("singleton")
public class CustomChargeTypeMapApiResource {

    private final DefaultToApiJsonSerializer<CustomChargeTypeMapData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final PlatformSecurityContext context;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @Autowired
    public CustomChargeTypeMapApiResource(final DefaultToApiJsonSerializer<CustomChargeTypeMapData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, final PlatformSecurityContext context,
            final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.context = context;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }

    @Autowired
    private CustomChargeTypeMapReadWritePlatformService service;

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String get(@Context final UriInfo uriInfo,
            @PathParam("chargeEntityid") @Parameter(description = "chargeEntityid") final Long chargeEntityid,
            @PathParam("customChargeTypeId") @Parameter(description = "customChargeTypeId") final Long customChargeTypeId) {
        this.context.authenticatedUser().validateHasReadPermission(CustomChargeTypeMapApiConstants.RESOURCE_NAME);
        return this.toApiJsonSerializer.serialize(this.service.findAllActive(customChargeTypeId));
    }

    @GET
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("id") @Parameter(description = "id") final Long id, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(CustomChargeTypeMapApiConstants.RESOURCE_NAME);

        final CustomChargeTypeMapData data = this.service.findById(id);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return this.toApiJsonSerializer.serialize(settings, data, CustomChargeTypeMapApiConstants.REQUEST_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createNewHoliday(@Parameter(hidden = true) final String apiRequestBodyAsJson,
            @PathParam("customChargeTypeId") @Parameter(description = "customChargeTypeId") final Long customChargeTypeId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCustomChargeTypeMap(customChargeTypeId)
                .withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String update(@PathParam("id") @Parameter(description = "id") final Long id,
            @PathParam("customChargeTypeId") @Parameter(description = "customChargeTypeId") final Long customChargeTypeId,
            @Parameter(hidden = true) final String jsonRequestBody) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateCustomChargeTypeMap(customChargeTypeId, id)
                .withJson(jsonRequestBody).build();

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
}
