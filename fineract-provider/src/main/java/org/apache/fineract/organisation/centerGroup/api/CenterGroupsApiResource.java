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
package org.apache.fineract.organisation.centerGroup.api;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.centerGroup.data.CenterGroupData;
import org.apache.fineract.organisation.centerGroup.service.CenterGroupConstants;
import org.apache.fineract.organisation.centerGroup.service.CenterGroupReadPlatformService;
import org.apache.fineract.organisation.portfolio.data.PortfolioData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/centers/{portfolioCenterId}/groups")
@Component
@Scope("singleton")
@Tag(name = "Center Groups", description = "Center groups are used to extend the office model, and they are related to centers.")
public class CenterGroupsApiResource {

    private final PlatformSecurityContext context;
    private final DefaultToApiJsonSerializer<PortfolioData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final CenterGroupReadPlatformService centerGroupReadPlatformService;

    @Autowired
    public CenterGroupsApiResource(final PlatformSecurityContext context,
            final DefaultToApiJsonSerializer<PortfolioData> toApiJsonSerializer, final ApiRequestParameterHelper apiRequestParameterHelper,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final CenterGroupReadPlatformService centerGroupReadPlatformService) {
        this.context = context;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.centerGroupReadPlatformService = centerGroupReadPlatformService;
    }

    @GET
    @Path("/{centerGroupId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveCenterGroup(@Context final UriInfo uriInfo, @PathParam("centerGroupId") final Long centerGroupId) {
        this.context.authenticatedUser().validateHasReadPermission(CenterGroupConstants.CENTER_GROUP_RESOURCE_NAME);
        CenterGroupData centerGroupData = this.centerGroupReadPlatformService.findById(centerGroupId);

        return this.toApiJsonSerializer.serialize(centerGroupData);
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAllCenterGroupsByPortfolioCenter(@Context final UriInfo uriInfo,
            @PathParam("portfolioCenterId") final Long portfolioCenterId) {
        this.context.authenticatedUser().validateHasReadPermission(CenterGroupConstants.CENTER_GROUP_RESOURCE_NAME);
        Collection<CenterGroupData> centerGroups = this.centerGroupReadPlatformService.retrieveAllByCenter(portfolioCenterId);

        return this.toApiJsonSerializer.serialize(centerGroups);
    }

    @GET
    @Path("/template")
    @Produces({ MediaType.APPLICATION_JSON })
    public String template() {
        CenterGroupData centerGroupData = this.centerGroupReadPlatformService.retrieveNewCenterGroupTemplate();

        return this.toApiJsonSerializer.serialize(centerGroupData);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a Group", description = "Mandatory Fields\n" + "name, parentId")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CenterGroupsApiResourceSwagger.PostCenterGroupsRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CenterGroupsApiResourceSwagger.PostCenterGroupsResponse.class))) })
    public String createCenterGroup(@PathParam("portfolioCenterId") final long portfolioCenterId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createCenterGroup(portfolioCenterId) //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{centerGroupId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update Center Group", description = "")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CenterGroupsApiResourceSwagger.PutCenterGroupsCenterGroupIdRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CenterGroupsApiResourceSwagger.PutCenterGroupsCenterGroupIdResponse.class))) })
    public String updateCenterGroup(
            @PathParam("portfolioCenterId") @Parameter(description = "portfolioCenterId") final Long portfolioCenterId,
            @PathParam("centerGroupId") @Parameter(description = "centerGroupId") final Long centerGroupId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .updateCenterGroup(portfolioCenterId, centerGroupId) //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{centerGroupId}/transfer")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Transfer a Center Group to another center", description = "")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CenterGroupsApiResourceSwagger.PutCenterGroupsCenterGroupIdRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CenterGroupsApiResourceSwagger.PutCenterGroupsCenterGroupIdResponse.class))) })
    public String transferCenterGroup(
            @PathParam("portfolioCenterId") @Parameter(description = "portfolioCenterId") final Long portfolioCenterId,
            @PathParam("centerGroupId") @Parameter(description = "centerGroupId") final Long centerGroupId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .transferCenterGroup(portfolioCenterId, centerGroupId) //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{centerGroupId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a center group", description = "A center group can be deleted if it has no association")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CenterGroupsApiResourceSwagger.DeleteCenterGroupsCenterGroupIdResponse.class))) })
    public String delete(@PathParam("portfolioCenterId") @Parameter(description = "portfolioCenterId") final Long portfolioCenterId,
            @PathParam("centerGroupId") @Parameter(description = "centerGroupId") final Long centerGroupId) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .deleteCenterGroup(portfolioCenterId, centerGroupId) //
                .build(); //
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

}
