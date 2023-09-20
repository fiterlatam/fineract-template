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

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.AbstractFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.bankcheque.api.BankChequeApiConstants;
import org.apache.fineract.organisation.bankcheque.command.VoidChequeCommand;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class VoidChequeCommandFromApiJsonDeserializer extends AbstractFromApiJsonDeserializer<VoidChequeCommand> {

    private final FromJsonHelper fromApiJsonHelper;

    @Override
    public VoidChequeCommand commandFromApiJson(String json) {
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                BankChequeApiConstants.SUPPORTED_VOID_CHEQUE_PARAMETERS);
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(BankChequeApiConstants.BANK_CHECK_RESOURCE_NAME.toLowerCase());
        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final String description = this.fromApiJsonHelper.extractStringNamed(BankChequeApiConstants.DESCRIPTION, element);
        baseDataValidator.reset().parameter(BankChequeApiConstants.DESCRIPTION).value(description).notBlank();
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
        return this.fromApiJsonHelper.fromJson(json, VoidChequeCommand.class);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
