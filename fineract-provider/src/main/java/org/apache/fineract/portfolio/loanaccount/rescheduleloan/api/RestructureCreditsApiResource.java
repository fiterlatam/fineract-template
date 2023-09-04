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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.RescheduleLoansApiConstants;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.RestructureCreditsRequestData;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.RestructureCreditsTemplateData;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.service.RestructureCreditsReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;

@Path("/restructurecredits/{clientId}")
@Component
@Scope("singleton")
@Tag(name = "Restructure Loans", description = "")
public class RestructureCreditsApiResource {

    private final DefaultToApiJsonSerializer<RestructureCreditsTemplateData> restructureCreditsRequestToApiJsonSerializer;
    private final PlatformSecurityContext platformSecurityContext;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final RestructureCreditsReadPlatformService restructureCreditsReadPlatformService;
    private final ClientReadPlatformService clientReadPlatformService;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanProductReadPlatformService loanProductReadPlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @Autowired
    public RestructureCreditsApiResource(final DefaultToApiJsonSerializer<RestructureCreditsTemplateData> restructureCreditsRequestToApiJsonSerializer,
                                         final PlatformSecurityContext platformSecurityContext,
                                         final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
                                         final RestructureCreditsReadPlatformService restructureCreditsReadPlatformService,
                                         final ApiRequestParameterHelper apiRequestParameterHelper,
                                         final LoanProductReadPlatformService loanProductReadPlatformService,
                                         final LoanReadPlatformService loanReadPlatformService,
                                         final ClientReadPlatformService clientReadPlatformService) {
        this.restructureCreditsRequestToApiJsonSerializer = restructureCreditsRequestToApiJsonSerializer;
        this.platformSecurityContext = platformSecurityContext;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.restructureCreditsReadPlatformService = restructureCreditsReadPlatformService;
        this.loanReadPlatformService = loanReadPlatformService;
        this.clientReadPlatformService = clientReadPlatformService;
        this.loanProductReadPlatformService = loanProductReadPlatformService;
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveTemplate(@Context final UriInfo uriInfo, @PathParam("clientId") final Long clientId) {

        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(RescheduleLoansApiConstants.RESTRUCTURE_CREDITS_RESOURCE);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        ClientData clientData = this.clientReadPlatformService.retrieveOneLookup(clientId);
        RestructureCreditsRequestData requestData = this.restructureCreditsReadPlatformService.retrievePendingRestructure(clientId);
        Collection<LoanAccountData> loanAccounts = this.loanReadPlatformService.retrieveClientActiveLoans(clientId);
        Collection<LoanProductData> loanProductData = this.loanProductReadPlatformService.retrieveAllLoanProducts();

        RestructureCreditsTemplateData templateData = RestructureCreditsTemplateData.instance(clientData, loanAccounts,requestData, loanProductData);

        return this.restructureCreditsRequestToApiJsonSerializer.serialize(settings, templateData);
    }

    @GET
    @Path("{requestId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String readLoanRescheduleRequest(@Context final UriInfo uriInfo, @PathParam("requestId") final Long requestId,
            @QueryParam("command") final String command) {
        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(RescheduleLoansApiConstants.ENTITY_NAME);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        if (compareIgnoreCase(command, "previewLoanReschedule")) {
            return null;
        }


        return null;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createLoanRescheduleRequest(final String apiRequestBodyAsJson, @PathParam("clientId") final Long clientId) {
        final CommandWrapper commandWrapper = new CommandWrapperBuilder()
                .createRestructureCreditsRequest(clientId).withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult commandProcessingResult = this.commandsSourceWritePlatformService.logCommandSource(commandWrapper);

        return this.restructureCreditsRequestToApiJsonSerializer.serialize(commandProcessingResult);
    }

    @POST
    @Path("{scheduleId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateLoanRescheduleRequest(@PathParam("scheduleId") final Long scheduleId, @QueryParam("command") final String command,
            final String apiRequestBodyAsJson) {
        CommandWrapper commandWrapper = null;

        if (compareIgnoreCase(command, "approve")) {
            commandWrapper = new CommandWrapperBuilder().approveLoanRescheduleRequest(RescheduleLoansApiConstants.ENTITY_NAME, scheduleId)
                    .withJson(apiRequestBodyAsJson).build();
        }

        else if (compareIgnoreCase(command, "reject")) {
            commandWrapper = new CommandWrapperBuilder().rejectLoanRescheduleRequest(RescheduleLoansApiConstants.ENTITY_NAME, scheduleId)
                    .withJson(apiRequestBodyAsJson).build();
        }

        else {
            throw new UnrecognizedQueryParamException("command", command, new Object[] { "approve", "reject" });
        }

        final CommandProcessingResult commandProcessingResult = this.commandsSourceWritePlatformService.logCommandSource(commandWrapper);

        return this.restructureCreditsRequestToApiJsonSerializer.serialize(commandProcessingResult);
    }

    /**
     * Compares two strings, ignoring differences in case
     *
     * @param firstString
     *            the first string
     * @param secondString
     *            the second string
     * @return true if the two strings are equal, else false
     **/
    private boolean compareIgnoreCase(String firstString, String secondString) {
        return StringUtils.isNotBlank(firstString) && firstString.trim().equalsIgnoreCase(secondString);
    }
}
