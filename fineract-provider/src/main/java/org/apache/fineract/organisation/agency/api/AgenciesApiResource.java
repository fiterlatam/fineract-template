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
package org.apache.fineract.organisation.agency.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.agency.data.AgencyData;
import org.apache.fineract.organisation.agency.service.AgencyConstants;
import org.apache.fineract.organisation.agency.service.AgencyReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/agencies")
@Component
@Scope("singleton")
@Tag(name = "Agencies", description = "Agencies are used to extend the office model, when the hierarchy indicates the office is an agency.")
public class AgenciesApiResource {

    private final PlatformSecurityContext context;
    private final AgencyReadPlatformService readPlatformService;
    private final DefaultToApiJsonSerializer<AgencyData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public AgenciesApiResource(final PlatformSecurityContext context, final AgencyReadPlatformService readPlatformService,
            final DefaultToApiJsonSerializer<AgencyData> toApiJsonSerializer, final ApiRequestParameterHelper apiRequestParameterHelper,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.context = context;
        this.readPlatformService = readPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll() {
        this.context.authenticatedUser().validateHasReadPermission(AgencyConstants.AGENCY_RESOURCE_NAME);
        Collection<AgencyData> agencies = this.readPlatformService.retrieveAllByUser();
        return this.toApiJsonSerializer.serialize(agencies);
    }

    @GET
    @Path("/{agencyId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAgency(@PathParam("agencyId") final Long agencyId) {
        this.context.authenticatedUser().validateHasReadPermission(AgencyConstants.AGENCY_RESOURCE_NAME);
        AgencyData agencyData = this.readPlatformService.findById(agencyId);
        return this.toApiJsonSerializer.serialize(agencyData);
    }

    @GET
    @Path("/template")
    @Produces({ MediaType.APPLICATION_JSON })
    public String template() {
        AgencyData agencyData = this.readPlatformService.retrieveNewAgencyTemplate();
        return this.toApiJsonSerializer.serialize(agencyData);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create an Agency", description = "Mandatory Fields\n" + "name, parentId")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AgenciesApiResourceSwagger.PostAgenciesRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AgenciesApiResourceSwagger.PostAgenciesResponse.class))) })
    public String createAgency(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createAgency() //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{agencyId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update Agency", description = "")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AgenciesApiResourceSwagger.PutAgenciesAgencyIdRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AgenciesApiResourceSwagger.PutAgencyAgencyIdResponse.class))) })
    public String updateOffice(@PathParam("agencyId") @Parameter(description = "agencyId") final Long agencyId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .updateAgency(agencyId) //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{agencyId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete an agency", description = "An agency can be deleted if it has no association")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AgenciesApiResourceSwagger.DeleteAgenciesAgencyIdResponse.class))) })
    public String delete(@PathParam("agencyId") @Parameter(description = "agencyId") final Long agencyId) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .deleteAgency(agencyId) //
                .build(); //
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

}
