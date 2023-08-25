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
package org.apache.fineract.organisation.prequalification.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.prequalification.data.PrequalificationChecklistData;
import org.apache.fineract.organisation.prequalification.service.PrequalificationChecklistReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/prequalification/checklist")
@Component
@Scope("singleton")
@Tag(name = "Prequalification checklist", description = "A prequalification checklist is used to make decision for loan offer")
public class PrequalificationChecklistApiResource {

    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PlatformSecurityContext context;
    private final DefaultToApiJsonSerializer<PrequalificationChecklistData> toApiJsonSerializer;

    private final PrequalificationChecklistReadPlatformService prequalificationChecklistReadPlatformService;
    private final String resourceNameForPermissions = "PREQUALIFICATION";
    private final Set<String> prequalificationChecklistDataParameters = new HashSet<>(
            Arrays.asList("id", "name", "description", "color", "loanProductId", "loanProductName", "prequalificationNumber",
                    "prequalificationName", "reference", "conditionalOperator", "firstValue", "secondValue", "valueList"));

    @Autowired
    public PrequalificationChecklistApiResource(final ApiRequestParameterHelper apiRequestParameterHelper,
            final PlatformSecurityContext context, DefaultToApiJsonSerializer<PrequalificationChecklistData> toApiJsonSerializer,
            PrequalificationChecklistReadPlatformService prequalificationChecklistReadPlatformService) {
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.context = context;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.prequalificationChecklistReadPlatformService = prequalificationChecklistReadPlatformService;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/{prequalificationNumber}")
    public String retrievePrequalificationChecklists(@PathParam("prequalificationNumber") final String prequalificationNumber,
            @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        Collection<PrequalificationChecklistData> prequalificationChecklistDataList = prequalificationChecklistReadPlatformService
                .retrievePrequalificationChecklists(prequalificationNumber);
        return this.toApiJsonSerializer.serialize(settings, prequalificationChecklistDataList,
                this.prequalificationChecklistDataParameters);
    }

}
