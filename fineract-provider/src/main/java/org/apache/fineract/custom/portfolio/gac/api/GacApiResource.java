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
package org.apache.fineract.custom.portfolio.gac.api;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.portfolio.gac.data.GacData;
import org.apache.fineract.custom.portfolio.gac.service.GacReadPlatformService;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformUserRightsContext;
import org.springframework.stereotype.Component;

@Path("/v1/gac")
@Component
@Tag(name = "Gac", description = "Gac")
@RequiredArgsConstructor
public class GacApiResource {

    private final PlatformUserRightsContext platformUserRightsContext;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer<GacData> apiJsonSerializerService;
    private final GacReadPlatformService gacReadPlatformService;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = GacApiResourceSwagger.PostAddGacResponse.class))),
            @ApiResponse(responseCode = "403", description = "OK", content = @Content(schema = @Schema(implementation = GacApiResourceSwagger.PostAddGacResponse.class))) })
    public String getAllGacs(@QueryParam("limit") @Parameter(description = "limit") final Integer limit,
            @QueryParam("status") @Parameter(description = "status") final String status,
            @QueryParam("savingAccountId") @Parameter(description = "savingAccountId") final Integer savingAccountId,
            @QueryParam("loanAccountId") @Parameter(description = "loanAccountId") final Integer loanId) {

        platformUserRightsContext.isAuthenticated();

        List<GacData> gacs = gacReadPlatformService.retrieveAll();

        return apiJsonSerializerService.serialize(gacs);
    }

    @GET
    @Path("{gacId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = GacApiResourceSwagger.PostAddGacResponse.class))) })
    public String getGac(@PathParam("gacId") final Long gacId, @QueryParam("template") final boolean template) {
        platformUserRightsContext.isAuthenticated();

        final GacData gac = gacReadPlatformService.retrieveOne(gacId);

        return apiJsonSerializerService.serialize(gac);
    }

}
