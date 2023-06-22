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
package org.apache.fineract.organisation.portfolio.api;

import static org.apache.fineract.organisation.portfolio.data.PortfolioDetailedPlanningComparator.createPortfolioDetailedPlanningComparator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import java.util.stream.Collectors;
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
import org.apache.fineract.organisation.portfolio.data.PortfolioData;
import org.apache.fineract.organisation.portfolio.data.PortfolioDetailedPlanningData;
import org.apache.fineract.organisation.portfolio.data.PortfolioPlanningData;
import org.apache.fineract.organisation.portfolio.service.PortfolioConstants;
import org.apache.fineract.organisation.portfolio.service.PortfolioReadPlatformService;
import org.apache.fineract.organisation.portfolioCenter.data.PortfolioCenterData;
import org.apache.fineract.organisation.portfolioCenter.service.PortfolioCenterReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/portfolios")
@Component
@Scope("singleton")
@Tag(name = "Portfolios", description = "Portfolios are used to extend the office model, and they are related to supervisions.")
public class PortfoliosApiResource {

    private final PlatformSecurityContext context;
    private final PortfolioReadPlatformService readPlatformService;
    private final DefaultToApiJsonSerializer<PortfolioData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final PortfolioCenterReadPlatformService centerReadPlatformService;

    @Autowired
    public PortfoliosApiResource(final PlatformSecurityContext context, final PortfolioReadPlatformService readPlatformService,
            final DefaultToApiJsonSerializer<PortfolioData> toApiJsonSerializer, final ApiRequestParameterHelper apiRequestParameterHelper,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final PortfolioCenterReadPlatformService centerReadPlatformService) {
        this.context = context;
        this.readPlatformService = readPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.centerReadPlatformService = centerReadPlatformService;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll() {
        this.context.authenticatedUser().validateHasReadPermission(PortfolioConstants.PORTFOLIO_RESOURCE_NAME);
        Collection<PortfolioData> portfolios = this.readPlatformService.retrieveAllByUser();
        return this.toApiJsonSerializer.serialize(portfolios);
    }

    @GET
    @Path("/{portfolioId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrievePortfolio(@PathParam("portfolioId") final Long portfolioId) {
        this.context.authenticatedUser().validateHasReadPermission(PortfolioConstants.PORTFOLIO_RESOURCE_NAME);
        PortfolioData portfolioData = this.readPlatformService.findById(portfolioId);

        // get list of centers if any
        Collection<PortfolioCenterData> centers = centerReadPlatformService.retrieveAllByPortfolio(portfolioId);
        if (centers != null && !centers.isEmpty()) {
            portfolioData.setCenters(centers);
        }

        return this.toApiJsonSerializer.serialize(portfolioData);
    }

    @GET
    @Path("/template")
    @Produces({ MediaType.APPLICATION_JSON })
    public String template() {
        PortfolioData portfolioData = this.readPlatformService.retrieveNewPortfolioTemplate();
        return this.toApiJsonSerializer.serialize(portfolioData);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a Portfolio", description = "Mandatory Fields\n" + "name, parentId")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = PortfoliosApiResourceSwagger.PostPortfoliosRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PortfoliosApiResourceSwagger.PostPortfoliosResponse.class))) })
    public String createPortfolio(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createPortfolio() //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{portfolioId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update Portfolio", description = "")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = PortfoliosApiResourceSwagger.PutPortfolioPortfolioIdRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PortfoliosApiResourceSwagger.PutPortfolioPortfolioIdResponse.class))) })
    public String updatePortfolio(@PathParam("portfolioId") @Parameter(description = "portfolioId") final Long portfolioId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .updatePortfolio(portfolioId) //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{portfolioId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a portfolio", description = "A portfolio can be deleted if it has no association")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PortfoliosApiResourceSwagger.DeletePortfolioPortfolioIdResponse.class))) })
    public String delete(@PathParam("portfolioId") @Parameter(description = "portfolioId") final Long portfolioId) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .deletePortfolio(portfolioId) //
                .build(); //
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("{portfolioId}/planning")
    @Produces({ MediaType.APPLICATION_JSON })
    public String planning(@PathParam("portfolioId") final Long portfolioId) {
        final String taskPermissionName = "PLANNING_PORTFOLIO";
        this.context.authenticatedUser().validateHasPermissionTo(taskPermissionName);

        PortfolioPlanningData portfoliosPlanning = this.readPlatformService.retrievePlanningByPortfolio(portfolioId);

        // get planning
        Collection<PortfolioDetailedPlanningData> planning = centerReadPlatformService.retrievePlanningByPortfolio(portfolioId);
        if (planning != null) {
            planning = planning.stream().sorted(createPortfolioDetailedPlanningComparator()).collect(Collectors.toList());
            portfoliosPlanning.setDetailedPlanningData(planning);
        }

        return this.toApiJsonSerializer.serialize(portfoliosPlanning);
    }

}
