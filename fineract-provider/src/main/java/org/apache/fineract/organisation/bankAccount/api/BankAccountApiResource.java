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
package org.apache.fineract.organisation.bankAccount.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
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
import org.apache.fineract.organisation.bankAccount.data.BankAccountData;
import org.apache.fineract.organisation.bankAccount.service.BankAccountConstants;
import org.apache.fineract.organisation.bankAccount.service.BankAccountReadPlatformService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/bankaccounts")
@Component
@Scope("singleton")
@Tag(name = "Bank Accounts", description = "Bank Accounts are used to represent client's bank.")
public class BankAccountApiResource {

    private final PlatformSecurityContext platformSecurityContext;
    private final DefaultToApiJsonSerializer<BankAccountData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    private final BankAccountReadPlatformService bankAccountReadPlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    public BankAccountApiResource(final PlatformSecurityContext platformSecurityContext,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final DefaultToApiJsonSerializer<BankAccountData> toApiJsonSerializer,
            final BankAccountReadPlatformService bankReadPlatformService, final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.platformSecurityContext = platformSecurityContext;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.bankAccountReadPlatformService = bankReadPlatformService;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a Bank Account", description = "Mandatory Fields\n"
            + "accountNumber, agencyId, bankId, glAccountId, description")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = BankAccountsApiResourceSwagger.PostBanksRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BankAccountsApiResourceSwagger.PostBanksResponse.class))) })
    public String createBankAccount(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createBankAccount() //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{bankAccountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update Bank Account", description = "")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = BankAccountsApiResourceSwagger.PutBanksBankIdRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BankAccountsApiResourceSwagger.PutBanksBankIdResponse.class))) })
    public String updateBankAccount(@PathParam("bankAccountId") @Parameter(description = "bankAccountId") final Long bankAccountId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .updateBankAccount(bankAccountId) //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{bankAccountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a bank account", description = "A bank account can be deleted if it has no association")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BankAccountsApiResourceSwagger.DeleteBanksBankIdResponse.class))) })
    public String delete(@PathParam("bankAccountId") @Parameter(description = "bankAccountId") final Long bankAccountId) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .deleteBankAccount(bankAccountId) //
                .build(); //
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll(@Context final UriInfo uriInfo,
            @QueryParam("sqlSearch") @Parameter(description = "sqlSearch") final String sqlSearch,
            @QueryParam("offset") @Parameter(description = "offset") final Integer offset,
            @QueryParam("limit") @Parameter(description = "limit") final Integer limit,
            @QueryParam("orderBy") @Parameter(description = "orderBy") final String orderBy,
            @QueryParam("sortOrder") @Parameter(description = "sortOrder") final String sortOrder,
            @QueryParam("accountNumber") @Parameter(description = "accountNumber") final String accountNumber,
            @QueryParam("bankName") @Parameter(description = "bankName") final String bankName,
            @QueryParam("bankCode") @Parameter(description = "bankCode") final String bankCode) {

        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(BankAccountConstants.BANK_ACCOUNT_RESOURCE_NAME);

        final SearchParameters searchParameters = SearchParameters.forBankAccounts(offset, limit, orderBy, sortOrder, sqlSearch,
                accountNumber, bankName, bankCode);
        Page<BankAccountData> bankAccounts = this.bankAccountReadPlatformService.retrieveAll(searchParameters);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, bankAccounts, BankAccountApiConstants.BANK_ACCOUNT_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("/{bankAccountId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveBankAccount(@PathParam("bankAccountId") final Long bankAccountId) {
        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(BankAccountConstants.BANK_ACCOUNT_RESOURCE_NAME);
        BankAccountData bankAccountData = this.bankAccountReadPlatformService.findById(bankAccountId);
        return this.toApiJsonSerializer.serialize(bankAccountData);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Bank Account Template", description = "This is a convenience resource. "
            + "It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "\n" + "Field Defaults\n" + "Allowed Value Lists\n\n" + "Example Request:\n" + "\n" + "bankaccounts/template")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BankAccountsApiResourceSwagger.GetBankAccountsTemplateResponse.class))) })
    public String retrieveTemplate(@Context final UriInfo uriInfo,
            @QueryParam("commandParam") @Parameter(description = "commandParam") final String commandParam) {

        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(BankAccountConstants.BANK_ACCOUNT_RESOURCE_NAME);

        BankAccountData bankAccountData = null;
        bankAccountData = this.bankAccountReadPlatformService.retrieveNewBankAccountTemplate();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, bankAccountData, BankAccountApiConstants.BANK_ACCOUNT_RESPONSE_DATA_PARAMETERS);
    }
}
