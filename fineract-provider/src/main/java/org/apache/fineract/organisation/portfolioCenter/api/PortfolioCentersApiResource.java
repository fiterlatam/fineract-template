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
package org.apache.fineract.organisation.portfolioCenter.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import org.apache.fineract.organisation.portfolio.data.PortfolioData;
import org.apache.fineract.organisation.portfolio.service.PortfolioReadPlatformService;
import org.apache.fineract.organisation.portfolioCenter.data.PortfolioCenterData;
import org.apache.fineract.organisation.portfolioCenter.service.PortfolioCenterConstants;
import org.apache.fineract.organisation.portfolioCenter.service.PortfolioCenterReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/portfolios/{portfolioId}/centers")
@Component
@Scope("singleton")
@Tag(name = "Portfolio Centers", description = "Portfolio centers are used to extend the office model, and they are related to supervisions.")
public class PortfolioCentersApiResource {

    private final PlatformSecurityContext context;
    private final PortfolioReadPlatformService readPlatformService;
    private final DefaultToApiJsonSerializer<PortfolioData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final PortfolioCenterReadPlatformService portfolioCenterReadPlatformService;

    @Autowired
    public PortfolioCentersApiResource(final PlatformSecurityContext context, final PortfolioReadPlatformService readPlatformService,
            final DefaultToApiJsonSerializer<PortfolioData> toApiJsonSerializer, final ApiRequestParameterHelper apiRequestParameterHelper,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final PortfolioCenterReadPlatformService portfolioCenterReadPlatformService) {
        this.context = context;
        this.readPlatformService = readPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.portfolioCenterReadPlatformService = portfolioCenterReadPlatformService;
    }

    @GET
    @Path("/{portfolioCenterId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrievePortfolioCenter(@Context final UriInfo uriInfo, @PathParam("portfolioCenterId") final Long portfolioCenterId) {
        this.context.authenticatedUser().validateHasReadPermission(PortfolioCenterConstants.PORTFOLIO_CENTER_RESOURCE_NAME);
        PortfolioCenterData portfolioCenterData = this.portfolioCenterReadPlatformService.findById(portfolioCenterId);

        return this.toApiJsonSerializer.serialize(portfolioCenterData);
    }

    @GET
    @Path("/template")
    @Produces({ MediaType.APPLICATION_JSON })
    public String template() {
        PortfolioCenterData portfolioCenterData = this.portfolioCenterReadPlatformService.retrievePortfolioCenterTemplate();

        return this.toApiJsonSerializer.serialize(portfolioCenterData);
    }

    @PUT
    @Path("/{portfolioCenterId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update Portfolio Center", description = "")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = PortfolioCentersApiResourceSwagger.PutPortfolioCenterPortfolioIdRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PortfolioCentersApiResourceSwagger.PutPortfolioCenterPortfolioIdResponse.class))) })
    public String updatePortfolioCenter(@PathParam("portfolioId") @Parameter(description = "portfolioId") final Long portfolioId,
            @PathParam("portfolioCenterId") @Parameter(description = "portfolioCenterId") final Long portfolioCenterId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .updatePortfolioCenter(portfolioId, portfolioCenterId) //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

}
