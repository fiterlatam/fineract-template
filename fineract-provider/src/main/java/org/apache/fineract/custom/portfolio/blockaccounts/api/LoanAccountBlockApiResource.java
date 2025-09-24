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
package org.apache.fineract.custom.portfolio.blockaccounts.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockDTO;
import org.apache.fineract.custom.portfolio.blockaccounts.service.LoanAccountBlockReadPlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformUserRightsContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

@Path("/v1/blockaccount")
@Component
@Controller
@Tag(name = "blockaccount", description = """
        Las cuentas de préstamo pueden tener algunos componentes bloqueados por diferentes motivos. Estas interfaces
        interactúan con acciones como bloquear, editar y desbloquear una cuenta de préstamo.
        Puede bloquear componentes como Desembolso, Intereses, Mora, Seguro de Vida, Seguro de Vida Nano, MiPyme y GAC.
        """)
@RequiredArgsConstructor
public class LoanAccountBlockApiResource {

    private final PlatformUserRightsContext platformUserRightsContext;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer<LoanAccountBlockDTO> apiJsonSerializerService;
    private final LoanAccountBlockReadPlatformService loanAccountBlockReadPlatformService;

    @POST
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Bloquear cuenta de préstamo", description = "Crea un bloqueo sobre la cuenta de préstamo especificada por loanId.", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoanAccountBlockApiResourceSwagger.PostLoanAccountBlockRequest.class))), responses = {
            @ApiResponse(responseCode = "200", description = "Bloqueo creado correctamente", content = @Content(schema = @Schema(implementation = LoanAccountBlockApiResourceSwagger.PostLoanAccountBlockResponse.class))) })
    public String addBlockAccount(@PathParam("loanId") final Long loanId, final String apiRequestBodyAsJson) {
        platformUserRightsContext.isAuthenticated();
        final CommandWrapper commandWrapper = new CommandWrapperBuilder().withLoanId(loanId).withJson(apiRequestBodyAsJson)
                .createLoanBlockAccount().build();
        CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandWrapper);
        return apiJsonSerializerService.serialize(result);
    }

    @POST
    @Path("{loanId}/unblock")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Desbloquear cuenta de préstamo", description = "Elimina un bloqueo activo de la cuenta de préstamo.", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoanAccountBlockApiResourceSwagger.PostUnblockLoanAccountRequest.class))), responses = {
            @ApiResponse(responseCode = "200", description = "Cuenta desbloqueada", content = @Content(schema = @Schema(implementation = LoanAccountBlockApiResourceSwagger.PostUnblockLoanAccountResponse.class))) })
    public String unblockBlockAccount(@PathParam("loanId") final Long loanId, final String apiRequestBodyAsJson) {
        platformUserRightsContext.isAuthenticated();
        final CommandWrapper commandWrapper = new CommandWrapperBuilder().withLoanId(loanId).withJson(apiRequestBodyAsJson)
                .unblockLoanBlockAccount(loanId).build();
        CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandWrapper);
        return apiJsonSerializerService.serialize(result);
    }

    @GET
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Obtener bloqueo activo", description = "Devuelve los datos del bloqueo activo para el préstamo dado.", responses = {
            @ApiResponse(responseCode = "200", description = "Datos del bloqueo", content = @Content(schema = @Schema(implementation = LoanAccountBlockApiResourceSwagger.GetLoanAccountBlockResponse.class))) })
    public String getBlockAccounts(@PathParam("loanId") Long loanId) {
        platformUserRightsContext.isAuthenticated();
        LoanAccountBlockDTO loanAccountBlockDTO = loanAccountBlockReadPlatformService.retrieveByLoanId(loanId);
        if (loanAccountBlockDTO == null) {
            throw new NotFoundException(String.valueOf(loanId));
        }
        return apiJsonSerializerService.serialize(loanAccountBlockDTO);
    }

    @GET
    @Path("{loanId}/history")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Historial de bloqueos", description = "Lista el historial completo de bloqueos para el préstamo.", responses = {
            @ApiResponse(responseCode = "200", description = "Historial recuperado", content = @Content(schema = @Schema(implementation = LoanAccountBlockApiResourceSwagger.GetLoanAccountBlockHistoryResponse.class))) })
    public String getBlockAccountsHistory(@PathParam("loanId") Long loanId) {
        platformUserRightsContext.isAuthenticated();
        List<LoanAccountBlockDTO> loanAccountBlockDTO = loanAccountBlockReadPlatformService.retrieveHistoryByLoanId(loanId);
        return apiJsonSerializerService.serialize(loanAccountBlockDTO);
    }

    @PUT
    @Path("{loanAccountBlockId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Actualizar bloqueo de cuenta", description = "Actualiza los datos de un bloqueo existente.", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoanAccountBlockApiResourceSwagger.PutLoanAccountBlockRequest.class))), responses = {
            @ApiResponse(responseCode = "200", description = "Bloqueo actualizado", content = @Content(schema = @Schema(implementation = LoanAccountBlockApiResourceSwagger.PutLoanAccountBlockResponse.class))) })
    public String updateBlockAccount(@PathParam("loanAccountBlockId") final Long loanAccountBlockId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        platformUserRightsContext.isAuthenticated();
        final CommandWrapper commandWrapper = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson)
                .updateLoanBlockAccount(loanAccountBlockId).build();
        CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandWrapper);
        return apiJsonSerializerService.serialize(result);
    }
}
