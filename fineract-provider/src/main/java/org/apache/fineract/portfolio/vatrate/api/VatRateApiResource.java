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
package org.apache.fineract.portfolio.vatrate.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.vatrate.VatRateApiConstants;
import org.apache.fineract.portfolio.vatrate.data.VatRateData;
import org.apache.fineract.portfolio.vatrate.service.ReadVatRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/vatrate")
@Component
@Scope("singleton")
@Tag(name = "Vat Rate", description = "This api returns a list of all configured vat rates bootstrapped within the application")
public class VatRateApiResource {

    private final PlatformSecurityContext context;
    private final ReadVatRateService readVatRateService;
    private final DefaultToApiJsonSerializer<VatRateData> toApiJsonSerializer;

    @Autowired
    public VatRateApiResource(final PlatformSecurityContext context, final ReadVatRateService readVatRateService,
            final DefaultToApiJsonSerializer<VatRateData> toApiJsonSerializer) {
        this.context = context;
        this.readVatRateService = readVatRateService;
        this.toApiJsonSerializer = toApiJsonSerializer;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve all vat rates")
    public String retrieveAll() {
        this.context.authenticatedUser().validateHasReadPermission(VatRateApiConstants.VAT_RATE_RESOURCE_NAME);
        Collection<VatRateData> vatRates = this.readVatRateService.retrieveAllVatRates();
        return this.toApiJsonSerializer.serialize(vatRates);
    }

    @GET
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve all vat rates")
    public String retrieve(@PathParam("id") @Parameter(description = "id") final Long id) {
        this.context.authenticatedUser().validateHasReadPermission(VatRateApiConstants.VAT_RATE_RESOURCE_NAME);
        VatRateData vatRate = this.readVatRateService.retrieveVatRateById(id);
        return this.toApiJsonSerializer.serialize(vatRate);
    }
}
