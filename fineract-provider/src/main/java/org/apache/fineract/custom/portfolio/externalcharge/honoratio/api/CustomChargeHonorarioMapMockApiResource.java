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
package org.apache.fineract.custom.portfolio.externalcharge.honoratio.api;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.constants.CustomChargeHonorarioMapApiConstants;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.data.CustomChargeHonorarioMapData;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/v1/customchargehonorariomap/mock")
@Component
@Scope("singleton")
public class CustomChargeHonorarioMapMockApiResource {

    private final DefaultToApiJsonSerializer<CustomChargeHonorarioMapData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final PlatformSecurityContext context;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @Autowired
    public CustomChargeHonorarioMapMockApiResource(final DefaultToApiJsonSerializer<CustomChargeHonorarioMapData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, final PlatformSecurityContext context,
            final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.context = context;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }

    @GET
    @Path("{loanid}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String get(@PathParam("loanid") @Parameter(description = "loanid") final Long id, @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(CustomChargeHonorarioMapApiConstants.RESOURCE_NAME);

        List<Map<String, Object>> generatedFata = generateRandomMockedData(id);

        return this.toApiJsonSerializer.serialize(generatedFata);
    }

    private Long loanInstallmentNr = 1L;
    private String clientDocumentId = "0501120631-9";
    private String nit = "120843958";

    private List<Map<String, Object>> generateRandomMockedData(Long loanId) {

        List<Map<String, Object>> jsonList = new ArrayList<>();

        // Generate Random Data for each installment
        for (int i = 1; i <= 4; i++) {

            double feeBaseAmount, feeVatAmount, feeTotalAmount;
            if (i < 4) {
                feeBaseAmount = 90;
                feeVatAmount = 10;
                feeTotalAmount = 100;
            } else {
                do {
                    feeBaseAmount = (Math.random() * (120 - 100) + 100);
                    feeVatAmount = (Math.random() * (80 - 10) + 10);
                    feeTotalAmount = feeBaseAmount + feeVatAmount;
                } while (feeTotalAmount <= 100 || feeTotalAmount >= 200);
            }

            // Create JSON payload
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("loanId", loanId);
            jsonMap.put("clientDocumentId", clientDocumentId);
            jsonMap.put("nit", nit);
            jsonMap.put("loanInstallmentNr", i);
            jsonMap.put("feeTotalAmount", String.format("%.4f", feeTotalAmount));
            jsonMap.put("feeBaseAmount", String.format("%.4f", feeBaseAmount));
            jsonMap.put("feeVatAmount", String.format("%.4f", feeVatAmount));
            jsonMap.put("dateFormat", "dd/MM/yyyy");
            jsonMap.put("locale", "en");

            jsonList.add(jsonMap);
        }

        return jsonList;
    }

    // Generate a random number
    private double randomFeeTotalAmount() {
        Random rand = new Random();
        return rand.nextDouble() * (200 - 100) + 100;
    }
}
