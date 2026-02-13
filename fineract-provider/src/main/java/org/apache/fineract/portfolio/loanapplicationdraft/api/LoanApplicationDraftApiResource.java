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
package org.apache.fineract.portfolio.loanapplicationdraft.api;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.organisation.staff.data.BulkTransferLoanOfficerData;
import org.apache.fineract.portfolio.loanapplicationdraft.service.LoanApplicationDraftReadPlatformService;
import org.springframework.stereotype.Component;

@Path("/loandraft")
@Component
@Tag(name = "loandraft", description = "loandraft")
@RequiredArgsConstructor
public class LoanApplicationDraftApiResource {

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer<BulkTransferLoanOfficerData> toApiJsonSerializer;
    private final LoanApplicationDraftReadPlatformService loanApplicationDraftReadPlatformService;

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String addLoanApplicationDraft(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson).createLoanApplicationDraft()
                .build();
        CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{draftId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateLoanApplicationDraft(@PathParam("draftId") @Parameter(description = "draftId") final Long draftId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson).updateLoanApplicationDraft(draftId)
                .build();
        CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("{draftId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getLoanApplicationDraft(@PathParam("draftId") @Parameter(description = "draftId") final Long draftId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        return toApiJsonSerializer.serialize(loanApplicationDraftReadPlatformService.retrieveById(draftId));
    }

    @DELETE
    @Path("{draftId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteLoanApplicationDraft(@PathParam("draftId") @Parameter(description = "draftId") final Long draftId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        return toApiJsonSerializer.serialize(loanApplicationDraftReadPlatformService.retrieveById(draftId));
    }

}
