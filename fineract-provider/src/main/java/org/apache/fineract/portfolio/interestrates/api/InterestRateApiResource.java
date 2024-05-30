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
package org.apache.fineract.portfolio.interestrates.api;

import io.swagger.v3.oas.annotations.Parameter;
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
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.interestrates.data.InterestRateData;
import org.apache.fineract.portfolio.interestrates.data.InterestRateHistoryData;
import org.apache.fineract.portfolio.interestrates.service.InterestRateReadPlatformService;
import org.springframework.stereotype.Component;

@Path("/v1/interestRates")
@Component
@RequiredArgsConstructor
public class InterestRateApiResource {

    private static final String RESOURCE_NAME = "INTEREST_RATE";
    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer<InterestRateData> toApiJsonSerializer;
    private final InterestRateReadPlatformService interestRateReadPlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private static final Set<String> PARAMETER_LIST = Set.of("id", "name", "active", "currentRate", "appliedOnDate", "createdBy",
            "createdDate", "lastModifiedBy", "lastModifiedDate");

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createInterestRate(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createInterestRate().withJson(apiRequestBodyAsJson).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll(@Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME);
        final List<InterestRateData> interestRates = this.interestRateReadPlatformService.retrieveAll();
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, interestRates, InterestRateApiResource.PARAMETER_LIST);
    }

    @GET
    @Path("{interestRateId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("interestRateId") final Long interestRateId, @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME);
        final InterestRateData interestRateData = this.interestRateReadPlatformService.retrieveOne(interestRateId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, interestRateData, InterestRateApiResource.PARAMETER_LIST);
    }

    @GET
    @Path("{interestRateId}/history")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveHistory(@PathParam("interestRateId") final Long interestRateId, @QueryParam("offset") final Integer offset,
            @QueryParam("limit") final Integer limit, @QueryParam("orderBy") final String orderBy,
            @QueryParam("sortOrder") final String sortOrder) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME);
        final SearchParameters searchParameters = SearchParameters.builder().interestRateId(interestRateId).offset(offset).limit(limit)
                .orderBy(orderBy).sortOrder(sortOrder).build();
        final Page<InterestRateHistoryData> interestRateHistoryDataList = this.interestRateReadPlatformService
                .retrieveHistory(searchParameters);
        return this.toApiJsonSerializer.serialize(interestRateHistoryDataList);
    }

    @PUT
    @Path("{interestRateId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateInterestRate(@PathParam("interestRateId") final Long interestRateId, final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateInterestRate(interestRateId).withJson(apiRequestBodyAsJson)
                .build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

}
