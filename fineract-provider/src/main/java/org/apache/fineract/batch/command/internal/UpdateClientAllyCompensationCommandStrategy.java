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
package org.apache.fineract.batch.command.internal;

import static org.apache.fineract.batch.command.CommandStrategyUtils.relativeUrlWithoutVersion;

import com.google.common.base.Splitter;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.batch.command.CommandStrategy;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.custom.portfolio.ally.api.ClientAllyApiResource;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.api.RescheduleLoansApiResource;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Implements {@link CommandStrategy} and applies a new reschedule loan request on an existing loan. It passes the
 * contents of the body from the BatchRequest to {@link RescheduleLoansApiResource} and gets back the response. This
 * class will also catch any errors raised by {@link RescheduleLoansApiResource} and map those errors to appropriate
 * status codes in BatchResponse.
 *
 * @see CommandStrategy
 * @see BatchRequest
 * @see BatchResponse
 */
@Component
@RequiredArgsConstructor
public class UpdateClientAllyCompensationCommandStrategy implements CommandStrategy {

    private final ClientAllyApiResource clientAllyApiResource;

    @Override
    public BatchResponse execute(BatchRequest request, @SuppressWarnings("unused") UriInfo uriInfo) {

        final BatchResponse response = new BatchResponse();
        final String responseBody;

        response.setRequestId(request.getRequestId());
        response.setHeaders(request.getHeaders());

        final List<String> pathParameters = Splitter.on('/').splitToList(relativeUrlWithoutVersion(request));
        System.out.println("path Id :" + pathParameters.get(2));
        final Long compensationId = Long.parseLong(pathParameters.get(2));
        responseBody = clientAllyApiResource.updateCompensation(compensationId, request.getBody());

        response.setStatusCode(HttpStatus.SC_OK);
        response.setBody(responseBody);
        return response;
    }
}
