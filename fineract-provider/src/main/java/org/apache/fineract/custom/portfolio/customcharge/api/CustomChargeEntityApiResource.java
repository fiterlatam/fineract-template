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
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyData;
import org.apache.fineract.custom.portfolio.ally.service.ClientAllyReadWritePlatformServiceImpl;
import org.apache.fineract.custom.portfolio.customcharge.constants.CustomChargeEntityApiConstants;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeEntityData;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeMapTemplate;
import org.apache.fineract.custom.portfolio.customcharge.service.CustomChargeEntityReadWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/v1/customchargeentities")
@Component
@Scope("singleton")
public class CustomChargeEntityApiResource {

    private final DefaultToApiJsonSerializer<CustomChargeEntityData> toApiJsonSerializer;
    private final PlatformSecurityContext context;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final CustomChargeEntityReadWritePlatformService customChargeEntityReadWritePlatformService;
    private final ClientAllyReadWritePlatformServiceImpl clientAllyReadWritePlatformService;

    @Autowired
    public CustomChargeEntityApiResource(final DefaultToApiJsonSerializer<CustomChargeEntityData> toApiJsonSerializer,
            final PlatformSecurityContext context, CustomChargeEntityReadWritePlatformService customChargeEntityReadWritePlatformService,
            final ApiRequestParameterHelper apiRequestParameterHelper,
            ClientAllyReadWritePlatformServiceImpl clientAllyReadWritePlatformService) {
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.context = context;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.customChargeEntityReadWritePlatformService = customChargeEntityReadWritePlatformService;
        this.clientAllyReadWritePlatformService = clientAllyReadWritePlatformService;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String get() {
        this.context.authenticatedUser().validateHasReadPermission(CustomChargeEntityApiConstants.RESOURCE_NAME);
        return this.toApiJsonSerializer.serialize(this.customChargeEntityReadWritePlatformService.findByIsExternalService(false));
    }

    @GET
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("id") @Parameter(description = "id") final Long id, @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(CustomChargeEntityApiConstants.RESOURCE_NAME);
        final CustomChargeEntityData data = this.customChargeEntityReadWritePlatformService.findById(id);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, data, CustomChargeEntityApiConstants.REQUEST_DATA_PARAMETERS);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveTemplate() {
        this.context.authenticatedUser().validateHasReadPermission(CustomChargeEntityApiConstants.RESOURCE_NAME);
        final List<CustomChargeEntityData> customChargeOptions = this.customChargeEntityReadWritePlatformService
                .findByIsExternalService(false);
        final List<ClientAllyData> clientAllyOptions = this.clientAllyReadWritePlatformService.retrieveWithPointOfSales();
        final CustomChargeMapTemplate customChargeMapTemplate = CustomChargeMapTemplate.builder().customChargeOptions(customChargeOptions)
                .clientAllyOptions(clientAllyOptions).build();
        return this.toApiJsonSerializer.serialize(customChargeMapTemplate);
    }
}
