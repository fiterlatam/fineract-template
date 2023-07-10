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
package org.apache.fineract.portfolio.blacklist.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.documentmanagement.api.FileUploadValidator;
import org.apache.fineract.infrastructure.documentmanagement.command.DocumentCommand;
import org.apache.fineract.infrastructure.documentmanagement.service.DocumentWritePlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.blacklist.data.BlacklistClientData;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistClientsRepository;
import org.apache.fineract.portfolio.blacklist.service.BlacklistClientReadPlatformService;
import org.apache.fineract.portfolio.blacklist.service.BlacklistClientWritePlatformService;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/blacklist")
@Component
@Scope("singleton")
@Tag(name = "Blacklist", description = "Clients with Bad credit history can be blacklisted. Blacklisted clients cannot be added to any loan or savings account.")
public class BlacklistApiResource {

    private static final Set<String> BLACKLIST_DATA_PARAMETERS = new HashSet<>(Arrays.asList("id", "productId", "productCode", "year",
            "typification", "dpi", "nit", "description", "agencyId", "balance", "disbursementAmount", "status", "addedBy", "createdAt"));

    private final String resourceNameForPermissions = "BLACKLIST";

    private final PlatformSecurityContext context;
    private final ClientReadPlatformService clientReadPlatformService;
    private final BlacklistClientReadPlatformService blacklistClientReadPlatformService;
    private final LoanProductReadPlatformService loanProductReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final DefaultToApiJsonSerializer<BlacklistClientData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final BlacklistClientsRepository blacklistClientsRepository;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final FileUploadValidator fileUploadValidator;
    private final DocumentWritePlatformService documentWritePlatformService;
    private final BlacklistClientWritePlatformService blacklistClientWritePlatformService;

    @Autowired
    public BlacklistApiResource(final PlatformSecurityContext context, final ClientReadPlatformService readPlatformService,
            final CodeValueReadPlatformService codeValueReadPlatformService,
            final BlacklistClientWritePlatformService blacklistClientWritePlatformService,
            final LoanProductReadPlatformService loanProductReadPlatformService,
            final DefaultToApiJsonSerializer<BlacklistClientData> toApiJsonSerializer,
            final BlacklistClientReadPlatformService blacklistClientReadPlatformService, final FileUploadValidator fileUploadValidator,
            final DocumentWritePlatformService documentWritePlatformService, final ApiRequestParameterHelper apiRequestParameterHelper,
            final BlacklistClientsRepository blacklistClientsRepository,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.context = context;
        this.clientReadPlatformService = readPlatformService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.loanProductReadPlatformService = loanProductReadPlatformService;
        this.blacklistClientsRepository = blacklistClientsRepository;
        this.blacklistClientReadPlatformService = blacklistClientReadPlatformService;
        this.fileUploadValidator = fileUploadValidator;
        this.documentWritePlatformService = documentWritePlatformService;
        this.blacklistClientWritePlatformService = blacklistClientWritePlatformService;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List all Blacklist clients", description = "Example Requests:\n" + "blacklist\n" + "\n" + "\n" + "blacklist")
    public String retrieveAllBlacklistItems(@Context final UriInfo uriInfo,
            @QueryParam("offset") @Parameter(description = "offset") final Integer offset,
            @QueryParam("limit") @Parameter(description = "limit") final Integer limit,
            @QueryParam("orderBy") @Parameter(description = "orderBy") final String orderBy,
            @QueryParam("status") @Parameter(description = "status") final String status,
            @QueryParam("searchText") @Parameter(description = "searchText") final String searchText,
            @QueryParam("sortOrder") @Parameter(description = "sortOrder") final String sortOrder) {

        this.context.authenticatedUser().validateHasViewPermission(this.resourceNameForPermissions);

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

        String clientName = queryParameters.getFirst("clientName");
        String dpi = queryParameters.getFirst("dpi");
        SearchParameters searchParameters = SearchParameters.forBlacklist(clientName, status, offset, limit, orderBy, sortOrder, dpi,
                searchText);
        final Page<BlacklistClientData> clientData = this.blacklistClientReadPlatformService.retrieveAll(searchParameters);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(queryParameters);
        return this.toApiJsonSerializer.serialize(settings, clientData, BLACKLIST_DATA_PARAMETERS);

    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Client Blacklist Template", description = "This is a convenience resource useful for building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "\n" + " Field Defaults\n" + " Allowed description Lists\n" + "\n\nExample Request:\n" + "clients/1/identifiers/template")
    public String newClientIdentifierDetails(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasViewPermission(this.resourceNameForPermissions);

        Collection<LoanProductData> loanProducts = this.loanProductReadPlatformService.retrieveAllLoanProducts();
        final Collection<CodeValueData> codeValues = this.codeValueReadPlatformService.retrieveCodeValuesByCode("Typification");
        final BlacklistClientData clientIdentifierData = BlacklistClientData.template(codeValues, loanProducts);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, clientIdentifierData, BLACKLIST_DATA_PARAMETERS);
    }

    @GET
    @Path("/{blacklistId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Blacklist Details", description = "This is a convenience resource useful for building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "\n" + " Field Defaults\n" + " Allowed description Lists\n" + "\n\nExample Request:\n" + "clients/1/identifiers/template")
    public String getBlacklistDetails(@Context final UriInfo uriInfo,
            @PathParam("blacklistId") @Parameter(description = "blacklistId") final Long blacklistId) {

        this.context.authenticatedUser().validateHasViewPermission(this.resourceNameForPermissions);

        BlacklistClientData clientData = this.blacklistClientReadPlatformService.retrieveOne(blacklistId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, clientData, BLACKLIST_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createClientIdentifier(@Parameter(hidden = true) final String apiRequestBodyAsJson) {

        try {
            final CommandWrapper commandRequest = new CommandWrapperBuilder().addClientToBlacklist().withJson(apiRequestBodyAsJson).build();

            final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

            return this.toApiJsonSerializer.serialize(result);
        } catch (final Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @POST
    @Path("/{blacklistId}/removeblacklist")
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createDocument(@PathParam("blacklistId") @Parameter(description = "blacklistId") final Long blacklistId,
            @HeaderParam("Content-Length") @Parameter(description = "Content-Length") final Long fileSize,
            @FormDataParam("file") final InputStream inputStream, @FormDataParam("file") final FormDataContentDisposition fileDetails,
            @FormDataParam("file") final FormDataBodyPart bodyPart, @FormDataParam("name") final String name,
            @FormDataParam("description") final String description) {

        fileUploadValidator.validate(fileSize, inputStream, fileDetails, bodyPart);
        final DocumentCommand documentCommand = new DocumentCommand(null, null, "blacklist", blacklistId, name, fileDetails.getFileName(),
                fileSize, bodyPart.getMediaType().toString(), description, null);
        final Long documentId = this.documentWritePlatformService.createDocument(documentCommand, inputStream);
        this.blacklistClientWritePlatformService.removeFromBlacklist(blacklistId);
        return this.toApiJsonSerializer.serialize(CommandProcessingResult.resourceResult(documentId, null));
    }
}
