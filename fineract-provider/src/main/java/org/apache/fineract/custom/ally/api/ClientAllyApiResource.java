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
package org.apache.fineract.custom.ally.api;

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
import jakarta.ws.rs.core.UriInfo;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.ally.data.ClientAllyData;
import org.apache.fineract.custom.ally.service.ClientAllyReadWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Path("/v1/clientsallies")
@Component
@Scope("singleton")
public class ClientAllyApiResource {

    private final DefaultToApiJsonSerializer<ClientAllyData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final PlatformSecurityContext context;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @Autowired
    public ClientAllyApiResource(final DefaultToApiJsonSerializer<ClientAllyData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, final PlatformSecurityContext context,
            final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.context = context;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }

	@Autowired
    private ClientAllyReadWritePlatformService service;

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String get(@Context final UriInfo uriInfo, @QueryParam("sqlSearch") @Parameter(description = "sqlSearch") final String sqlSearch) {
        this.context.authenticatedUser().validateHasReadPermission(ClientAllyApiConstants.RESOURCE_NAME);
        return this.toApiJsonSerializer.serialize(this.service.findByName(sqlSearch));
    }

    @GET
    @Path("/template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getTemplate(@Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(ClientAllyApiConstants.RESOURCE_NAME);
        return this.toApiJsonSerializer.serialize(this.service.getTemplateForInsertAndUpdate());
    }

    @GET
    @Path("/department/{id}/cities")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getCitiesByDepartment(@Context final UriInfo uriInfo,
                                        @PathParam("id") @Parameter(description = "id") final Long id) {
        this.context.authenticatedUser().validateHasReadPermission(ClientAllyApiConstants.RESOURCE_NAME);
        return this.toApiJsonSerializer.serialize(this.service.getCitiesByDepartment(id));
    }


    @GET
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("id") @Parameter(description = "id") final Long id,
            @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(ClientAllyApiConstants.RESOURCE_NAME);

        final ClientAllyData data = this.service.findById(id);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return this.toApiJsonSerializer.serialize(settings, data, ClientAllyApiConstants.REQUEST_DATA_PARAMETERS);
    }


    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createNewHoliday(@Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createClientAlly().withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }


    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String update(@PathParam("id") @Parameter(description = "id") final Long id,
            @Parameter(hidden = true) final String jsonRequestBody) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateClientAlly(id).withJson(jsonRequestBody).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }


    @DELETE
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String delete(@PathParam("id") @Parameter(description = "id") final Long id) {
    
        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteClientAlly(id).build();
        
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        
        return this.toApiJsonSerializer.serialize(result);
    }

}
