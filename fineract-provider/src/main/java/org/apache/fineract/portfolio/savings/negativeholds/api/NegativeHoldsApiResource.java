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
package org.apache.fineract.portfolio.savings.negativeholds.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldsValidationData;
import org.apache.fineract.portfolio.savings.negativeholds.service.NegativeHoldsReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Validar y corregir retenciones (tipo 20) con saldo corrido negativo en cuentas de Garantías, reversando el par
 * retención + liberación. Permisos: READ_NEGATIVEHOLDS (validar/reporte) y FIX_NEGATIVEHOLDS (corregir).
 */
@Path("/guaranteeaccounts/negativeholds")
@Component
@Scope("singleton")
@Tag(name = "Guarantee Accounts Negative Holds", description = "Validate and fix guarantee holds with negative running balance")
public class NegativeHoldsApiResource {

    private final PlatformSecurityContext context;
    private final NegativeHoldsReadPlatformService readService;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer<NegativeHoldsValidationData> toApiJsonSerializer;

    @Autowired
    public NegativeHoldsApiResource(final PlatformSecurityContext context, final NegativeHoldsReadPlatformService readService,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final DefaultToApiJsonSerializer<NegativeHoldsValidationData> toApiJsonSerializer) {
        this.context = context;
        this.readService = readService;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Validar retenciones negativas", description = "Solo lectura. clientAccountNo acepta varios separados por coma. "
            + "Ejemplo: guaranteeaccounts/negativeholds?clientAccountNo=000136746")
    public String validate(
            @QueryParam(NegativeHoldsApiConstants.CLIENT_ACCOUNT_NO_PARAM) @Parameter(description = "clientAccountNo (uno o varios, separados por coma)") final String clientAccountNo,
            @QueryParam(NegativeHoldsApiConstants.SAVINGS_ID_PARAM) @Parameter(description = "savingsId") final Long savingsId,
            @DefaultValue("true") @QueryParam("movimientos") @Parameter(description = "incluir movimientos") final boolean includeMovements) {
        this.context.authenticatedUser().validateHasReadPermission(NegativeHoldsApiConstants.RESOURCE_NAME);
        final List<String> clients = StringUtils.isBlank(clientAccountNo) ? Collections.emptyList()
                : Arrays.stream(clientAccountNo.split(",")).map(String::trim).filter(StringUtils::isNotEmpty).distinct()
                        .collect(Collectors.toList());
        if (clients.isEmpty() && savingsId == null) {
            throw new UnrecognizedQueryParamException(NegativeHoldsApiConstants.CLIENT_ACCOUNT_NO_PARAM, null,
                    "Indique clientAccountNo o savingsId");
        }
        return this.toApiJsonSerializer.serialize(this.readService.validate(clients, savingsId, includeMovements));
    }

    @GET
    @Path("report")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Reporte de cuentas de Garantías con retenciones de saldo corrido negativo")
    public String report(
            @DefaultValue("false") @QueryParam("movimientos") @Parameter(description = "incluir movimientos") final boolean includeMovements) {
        this.context.authenticatedUser().validateHasReadPermission(NegativeHoldsApiConstants.RESOURCE_NAME);
        return this.toApiJsonSerializer.serialize(this.readService.report(includeMovements));
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Corregir retenciones negativas", description = "command=fix. Body: {\"clientAccountNo\":\"000136746\","
            + "\"expectedPairs\":[{\"holdId\":1,\"releaseId\":2}]}. Revalida dentro de la transacción y solo aplica si los pares "
            + "actuales son exactamente expectedPairs.")
    public String handleCommand(@QueryParam("command") @Parameter(description = "command") final String commandParam,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        if (!NegativeHoldsApiConstants.COMMAND_FIX.equalsIgnoreCase(StringUtils.trimToEmpty(commandParam))) {
            throw new UnrecognizedQueryParamException("command", commandParam, NegativeHoldsApiConstants.COMMAND_FIX);
        }
        final CommandWrapper commandRequest = new CommandWrapperBuilder().fixNegativeGuaranteeHolds().withJson(apiRequestBodyAsJson)
                .build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }
}
