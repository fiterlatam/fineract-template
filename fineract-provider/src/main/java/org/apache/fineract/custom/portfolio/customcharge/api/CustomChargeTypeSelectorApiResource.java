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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.portfolio.customcharge.constants.CustomChargeTypeApiConstants;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeSelectorData;
import org.apache.fineract.custom.portfolio.customcharge.service.CustomChargeSelectorReadWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/v1/customchargeyypeselectors")
@Component
@Scope("singleton")
public class CustomChargeTypeSelectorApiResource {

    private final DefaultToApiJsonSerializer<CustomChargeSelectorData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final PlatformSecurityContext context;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @Autowired
    public CustomChargeTypeSelectorApiResource(final DefaultToApiJsonSerializer<CustomChargeSelectorData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, final PlatformSecurityContext context,
            final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.context = context;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }

    @Autowired
    private CustomChargeSelectorReadWritePlatformService service;

    @GET
    @Path("{clientId}/{clientAllyId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("clientId") @Parameter(description = "clientId") final Long clientId,
            @PathParam("clientAllyId") @Parameter(description = "clientAllyId") final Long clientAllyId, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(CustomChargeTypeApiConstants.RESOURCE_NAME);

        final List<CustomChargeSelectorData> data = this.service.getCustomChargeAval(clientId, clientAllyId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return this.toApiJsonSerializer.serialize(settings, data, CustomChargeTypeApiConstants.REQUEST_DATA_PARAMETERS);
    }
}
