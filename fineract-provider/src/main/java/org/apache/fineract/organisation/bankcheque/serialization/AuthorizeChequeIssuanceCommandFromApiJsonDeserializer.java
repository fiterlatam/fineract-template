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
package org.apache.fineract.organisation.bankcheque.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.AbstractFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.bankcheque.command.AuthorizeChequeIssuanceCommand;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizeChequeIssuanceCommandFromApiJsonDeserializer
        extends AbstractFromApiJsonDeserializer<List<AuthorizeChequeIssuanceCommand>> {

    private final FromJsonHelper fromApiJsonHelper;

    @Override
    public List<AuthorizeChequeIssuanceCommand> commandFromApiJson(String json) {
        final JsonElement jsonElement = this.fromApiJsonHelper.parse(json);
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        List<AuthorizeChequeIssuanceCommand> authorizeChequeIssuanceCommands = new ArrayList<>();
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
        for (int i = 0; i < jsonArray.size(); i++) {
            final JsonElement element = jsonArray.get(i);
            final Long chequeId = this.fromApiJsonHelper.extractLongNamed(LoanApiConstants.CHEQUE_ID, element);
            baseDataValidator.reset().parameter(LoanApiConstants.CHEQUE_ID).value(chequeId).notBlank();
            final String description = this.fromApiJsonHelper.extractStringNamed(LoanApiConstants.CHEQUE_DESCRIPTION, element);
            baseDataValidator.reset().parameter(LoanApiConstants.CHEQUE_DESCRIPTION).value(description).ignoreIfNull()
                    .notExceedingLengthOf(1000);
            final AuthorizeChequeIssuanceCommand authorizeChequeIssuanceCommand = AuthorizeChequeIssuanceCommand.builder()
                    .chequeId(chequeId).description(description).build();
            authorizeChequeIssuanceCommands.add(authorizeChequeIssuanceCommand);
        }
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
        return authorizeChequeIssuanceCommands;
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
