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
package org.apache.fineract.organisation.bankcheque.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.PaginationParameters;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.bankcheque.data.BatchData;
import org.apache.fineract.organisation.bankcheque.data.ChequeData;
import org.apache.fineract.organisation.bankcheque.data.ChequeSearchParams;
import org.apache.fineract.organisation.bankcheque.data.GuaranteeData;
import org.apache.fineract.organisation.bankcheque.service.ChequeReadPlatformService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/bankcheques")
@Component
@Scope("singleton")
@Tag(name = "Bank Cheques", description = "Cheques are categorized into batches which are later used to disbursed loans to customers")
@RequiredArgsConstructor
@Slf4j
public class BankChequeApiResource {

    private final PlatformSecurityContext context;
    private final ToApiJsonSerializer<Object> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final ChequeReadPlatformService chequeReadPlatformService;

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a cheque batch", description = "Mandatory Fields: from cheque, to cheque, bank account no, agency ID"
            + "Optional Fields: No optional fields")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = BankChequeApiSwagger.PostChequeBatchRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BankChequeApiSwagger.PostChequeBatchResponse.class))) })
    public String chequeRequests(@Parameter(hidden = true) final String apiRequestBodyAsJson,
            @QueryParam("commandParam") @Parameter(description = "commandParam") final String commandParam,
            @QueryParam("chequeId") @Parameter(description = "chequeId") final Long chequeId) {
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);
        CommandProcessingResult result = null;
        CommandWrapper commandRequest;
        if (is(commandParam, "createbatch")) {
            commandRequest = builder.createChequeBatch().withJson(apiRequestBodyAsJson).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "reassigncheque")) {
            commandRequest = builder.reassignCheque(chequeId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "authorizereassignment")) {
            commandRequest = builder.authorizedChequeReassignment(chequeId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "voidcheque")) {
            commandRequest = builder.voidCheque(chequeId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "authorizevoidance")) {
            commandRequest = builder.authorizeChequeVoidance(chequeId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "approveissuance")) {
            commandRequest = builder.approveChequesIssuance().build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "authorizeissuance")) {
            commandRequest = builder.authorizeChequesIssuance().build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "payguaranteesbycheques")) {
            commandRequest = builder.payGuaranteesByCheques().build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (result == null) {
            throw new UnrecognizedQueryParamException("command", commandParam, "reassigncheque", "authorizereassignment", "createbatch",
                    "voidcheque", "authorizevoidance", "approveissuance", "payguaranteesbycheques");
        }
        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("{batchId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a cheque batch", description = "Example Requests: /bankcheques/56")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BankChequeApiSwagger.GetChequeBatchResponse.class))) })
    public String retrieveOne(@PathParam("batchId") @Parameter(description = "batchId") final Long batchId,
            @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(BankChequeApiConstants.BANK_CHECK_RESOURCE_NAME);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        BatchData batchData = this.chequeReadPlatformService.retrieveBatch(batchId);
        return this.toApiJsonSerializer.serialize(settings, batchData, BankChequeApiConstants.BATCH_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Cheque Batch Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BankChequeApiSwagger.GetChequeBatchResponse.class))) })
    public String retrieveTemplate(@Context final UriInfo uriInfo,
            @Parameter(description = "officeId") @QueryParam("officeId") final Long officeId,
            @QueryParam("bankAccId") @Parameter(description = "backAccId") final Long backAccId) {
        this.context.authenticatedUser().validateHasReadPermission(BankChequeApiConstants.BANK_CHECK_RESOURCE_NAME);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        BatchData batchData = this.chequeReadPlatformService.retrieveTemplate(backAccId);
        return this.toApiJsonSerializer.serialize(settings, batchData, BankChequeApiConstants.BATCH_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("guarantees")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Guarantees", description = "Retrieve Guarantees by search parameters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BankChequeApiSwagger.GetChequeBatchResponse.class))) })
    public String retrieveGuarantees(@Context final UriInfo uriInfo,
            @QueryParam("caseId") @Parameter(description = "caseId") final String caseId,
            @QueryParam("locale") @Parameter(description = "locale") final String locale) {
        this.context.authenticatedUser().validateHasReadPermission(BankChequeApiConstants.BANK_CHECK_RESOURCE_NAME);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        List<GuaranteeData> garanteeDataList = this.chequeReadPlatformService.retrieveGuarantees(caseId, locale);
        return this.toApiJsonSerializer.serialize(settings, garanteeDataList);
    }

    @GET
    @Path("/search")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List Cheques", description = "Search cheques by parameters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BankChequeApiSwagger.GetChequeBatchResponse.GetBankCheque.class))) })
    public String retrieveAll(@Context final UriInfo uriInfo, @QueryParam("paged") @Parameter(description = "paged") final Boolean paged,
            @QueryParam("status") @Parameter(description = "status") final String status,
            @QueryParam("chequeNo") @Parameter(description = "chequeNo") final String chequeNo,
            @QueryParam("bankAccNo") @Parameter(description = "bankAccNo") final String bankAccNo,
            @QueryParam("bankAccId") @Parameter(description = "bankAccId") final Long bankAccId,
            @QueryParam("from") @Parameter(description = "from") final Long from,
            @QueryParam("to") @Parameter(description = "from") final Long to,
            @QueryParam("facilitatorId") @Parameter(description = "facilitatorId") final Long facilitatorId,
            @QueryParam("groupId") @Parameter(description = "groupId") final Long groupId,
            @QueryParam("centerId") @Parameter(description = "centerId") final Long centerId,
            @QueryParam("batchId") @Parameter(description = "batchId") final Long batchId,
            @QueryParam("agencyId") @Parameter(description = "agencyId") final Long agencyId,
            @QueryParam("chequeId") @Parameter(description = "batchId") final Long chequeId,
            @QueryParam("offset") @Parameter(description = "offset") final Integer offset,
            @QueryParam("limit") @Parameter(description = "limit") final Integer limit,
            @QueryParam("orderBy") @Parameter(description = "orderBy") final String orderBy,
            @QueryParam("sortOrder") @Parameter(description = "sortOrder") final String sortOrder) {
        this.context.authenticatedUser().validateHasReadPermission(BankChequeApiConstants.BANK_CHECK_RESOURCE_NAME);
        final PaginationParameters parameters = PaginationParameters.instance(paged, offset, limit, orderBy, sortOrder);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        final ChequeSearchParams chequeSearchParams = ChequeSearchParams.builder().bankAccId(bankAccId).agencyId(agencyId)
                .chequeNo(chequeNo).bankAccNo(bankAccNo).batchId(batchId).chequeId(chequeId).status(status).offset(offset).limit(limit)
                .orderBy(orderBy).sortOrder(sortOrder).from(from).to(to).facilitatorId(facilitatorId).groupId(groupId).centerId(centerId)
                .build();
        final Page<ChequeData> cheques = this.chequeReadPlatformService.retrieveAll(chequeSearchParams, parameters);
        return this.toApiJsonSerializer.serialize(settings, cheques);
    }

    @PUT
    @Path("{batchId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update a cheque batch", description = "Mandatory Fields: from cheque, to cheque, bank account no, agency ID"
            + "Optional Fields: No optional fields")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = BankChequeApiSwagger.PostChequeBatchRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BankChequeApiSwagger.PostChequeBatchResponse.class))) })
    public String updateBatch(@PathParam("batchId") @Parameter(description = "batchId") final Long batchId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateChequeBatch(batchId).withJson(apiRequestBodyAsJson).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{batchId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a cheque batch", description = "Cannot delete batch with available cheques, the delete is a 'hard delete' and cannot be recovered from")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = BankChequeApiSwagger.PostChequeBatchRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BankChequeApiSwagger.PostChequeBatchResponse.class))) })
    public String deleteBatch(@PathParam("batchId") @Parameter(description = "batchId") final Long batchId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteChequeBatch(batchId).withJson(apiRequestBodyAsJson).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }
}
