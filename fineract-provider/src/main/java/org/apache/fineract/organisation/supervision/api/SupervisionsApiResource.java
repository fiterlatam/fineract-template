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
package org.apache.fineract.organisation.supervision.api;

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
import org.apache.fineract.organisation.supervision.data.SupervisionData;
import org.apache.fineract.organisation.supervision.service.SupervisionConstants;
import org.apache.fineract.organisation.supervision.service.SupervisionReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/supervisions")
@Component
@Scope("singleton")
@Tag(name = "Supervisions", description = "Supervisions are used to extend the office model, and they are related to agencies.")
public class SupervisionsApiResource {

    private final PlatformSecurityContext context;
    private final SupervisionReadPlatformService readPlatformService;
    private final DefaultToApiJsonSerializer<SupervisionData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public SupervisionsApiResource(final PlatformSecurityContext context, final SupervisionReadPlatformService readPlatformService,
            final DefaultToApiJsonSerializer<SupervisionData> toApiJsonSerializer,
            final ApiRequestParameterHelper apiRequestParameterHelper,
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
        this.context.authenticatedUser().validateHasReadPermission(SupervisionConstants.SUPERVISION_RESOURCE_NAME);
        Collection<SupervisionData> supervisions = this.readPlatformService.retrieveAllByUser();
        return this.toApiJsonSerializer.serialize(supervisions);
    }

    @GET
    @Path("/{supervisionId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveSupervision(@PathParam("supervisionId") final Long supervisionId) {
        this.context.authenticatedUser().validateHasReadPermission(SupervisionConstants.SUPERVISION_RESOURCE_NAME);
        SupervisionData supervisionData = this.readPlatformService.findById(supervisionId);
        return this.toApiJsonSerializer.serialize(supervisionData);
    }

    @GET
    @Path("/template")
    @Produces({ MediaType.APPLICATION_JSON })
    public String template() {
        SupervisionData supervisionData = this.readPlatformService.retrieveNewSupervisionTemplate();
        return this.toApiJsonSerializer.serialize(supervisionData);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a Supervision", description = "Mandatory Fields\n" + "name, parentId")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SupervisionsApiResourceSwagger.PostSupervisionsRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SupervisionsApiResourceSwagger.PostSupervisionsResponse.class))) })
    public String createSupervision(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createSupervision() //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{supervisionId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update Supervision", description = "")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SupervisionsApiResourceSwagger.PutSupervisionSupervisionIdRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SupervisionsApiResourceSwagger.PutSupervisionSupervisionIdResponse.class))) })
    public String updateOffice(@PathParam("supervisionId") @Parameter(description = "supervisionId") final Long supervisionId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .updateSupervision(supervisionId) //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{supervisionId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a supervision", description = "A supervision can be deleted if it has no association")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SupervisionsApiResourceSwagger.DeleteSupervisionSupervisionIdResponse.class))) })
    public String delete(@PathParam("supervisionId") @Parameter(description = "supervisionId") final Long supervisionId) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .deleteSupervision(supervisionId) //
                .build(); //
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

}
