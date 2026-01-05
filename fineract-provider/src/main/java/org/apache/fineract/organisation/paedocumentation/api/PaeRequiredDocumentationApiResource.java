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
package org.apache.fineract.organisation.paedocumentation.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
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
import org.apache.fineract.infrastructure.documentmanagement.api.FileUploadValidator;
import org.apache.fineract.infrastructure.documentmanagement.command.DocumentCommand;
import org.apache.fineract.infrastructure.documentmanagement.service.DocumentWritePlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.paedocumentation.data.PaeRequiredDocumentData;
import org.apache.fineract.organisation.paedocumentation.service.PaeRequiredDocumentReadPlatformService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/paedocumentation")
@Component
@Scope("singleton")
@Tag(name = "PAE Required Documents", description = "Manage PAE required documents per category")
public class PaeRequiredDocumentationApiResource {

    private final Set<String> responseDataParameters = new HashSet<>(
            Arrays.asList("id", "categoryId", "documentName", "description", "acceptedFormat"));

    private final String resourceNameForPermissions = "PAE_DOCUMENTS";

    private final PlatformSecurityContext context;
    private final PaeRequiredDocumentReadPlatformService readPlatformService;
    private final DefaultToApiJsonSerializer<PaeRequiredDocumentData> toApiJsonSerializer;
    private final DefaultToApiJsonSerializer<CommandProcessingResult> resultSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final FileUploadValidator fileUploadValidator;
    private final DocumentWritePlatformService documentWritePlatformService;

    @Autowired
    public PaeRequiredDocumentationApiResource(PlatformSecurityContext context, PaeRequiredDocumentReadPlatformService readPlatformService,
            DefaultToApiJsonSerializer<PaeRequiredDocumentData> toApiJsonSerializer,
            DefaultToApiJsonSerializer<CommandProcessingResult> resultSerializer, ApiRequestParameterHelper apiRequestParameterHelper,
            PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, FileUploadValidator fileUploadValidator,
            DocumentWritePlatformService documentWritePlatformService) {
        this.context = context;
        this.readPlatformService = readPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.resultSerializer = resultSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.fileUploadValidator = fileUploadValidator;
        this.documentWritePlatformService = documentWritePlatformService;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List required documents by category", description = "Retrieve PAE required documents for a given category (code value id)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PaeRequiredDocumentationApiResourceSwagger.GetPaeRequiredDocumentsResponse.class)))) })
    public String retrieveByCategory(@Context UriInfo uriInfo,
            @QueryParam("categoryId") @Parameter(description = "categoryId", required = true) Long categoryId) {

        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        List<PaeRequiredDocumentData> documents = readPlatformService.retrieveByCategory(categoryId);
        ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, documents, responseDataParameters);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a required document for a category")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = PaeRequiredDocumentationApiResourceSwagger.PostPaeRequiredDocumentRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PaeRequiredDocumentationApiResourceSwagger.PostPaeRequiredDocumentResponse.class))) })
    public String createRequiredDocument(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        this.context.authenticatedUser().validateHasCreatePermission(this.resourceNameForPermissions);

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createPaeDocuments() //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult commandProcessingResult = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.resultSerializer.serialize(commandProcessingResult);
    }

    @DELETE
    @Path("{documentId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a required document")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PaeRequiredDocumentationApiResourceSwagger.DeletePaeRequiredDocumentResponse.class))) })
    public String deleteRequiredDocument(@PathParam("documentId") @Parameter(description = "documentId", required = true) Long documentId) {
        this.context.authenticatedUser().validateHasPermissionTo("DELETE_" + this.resourceNameForPermissions);

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .deletePaeDocuments(documentId) //
                .build();

        final CommandProcessingResult commandProcessingResult = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.resultSerializer.serialize(commandProcessingResult);
    }

    @POST
    @Path("/{loanId}/paedocument")
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createDocument(@PathParam("loanId") @Parameter(description = "loanId") final Long loanId,
            @HeaderParam("Content-Length") @Parameter(description = "Content-Length") final Long fileSize,
            @FormDataParam("file") final InputStream inputStream, @FormDataParam("file") final FormDataContentDisposition fileDetails,
            @FormDataParam("file") final FormDataBodyPart bodyPart, @FormDataParam("name") final String name,
            @FormDataParam("categoryId") final String categoryId, @FormDataParam("guaranteeNo") final String guaranteeNo,
            @FormDataParam("description") final String description, @FormDataParam("comment") final String comment) {

        if (inputStream != null) {
            fileUploadValidator.validate(fileSize, inputStream, fileDetails, bodyPart);
            final DocumentCommand documentCommand = new DocumentCommand(null, null, "paedocumentation", loanId, name,
                    fileDetails.getFileName(), fileSize, bodyPart.getMediaType().toString(), description, null);
            documentCommand.setDocumentType(categoryId);
            documentCommand.setDocumentPurpose(guaranteeNo);
            final Long documentId = this.documentWritePlatformService.createDocument(documentCommand, inputStream);
        }
        return this.toApiJsonSerializer.serialize(CommandProcessingResult.resourceResult(loanId, null));
    }

}
