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
package org.apache.fineract.organisation.prequalification.serialization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.prequalification.command.PrequalificatoinApiConstants;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.client.validation.ClientIdentifierDocumentValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class PrequalificationMemberCommandFromApiJsonDeserializer {

    private final FromJsonHelper fromApiJsonHelper;
    private final Set<String> supportedParameters = new HashSet<>(
            Arrays.asList("id", "clientId", "name", "dpi", "dob", "locale", "dateFormat", "amount", "puente", "individual",
                    "workWithPuente", "productId", "members", "prequalilficationTimespan", "status", "groupPresident"));

    private final Set<String> supportedParametersForUpdate = new HashSet<>(
            Arrays.asList("id", "clientId", "name", "dpi", "dob", "locale", "dateFormat", "amount", "puente", "individual", "productId",
                    "members", "prequalilficationTimespan", "status", "groupPresident"));

    @Autowired
    public PrequalificationMemberCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("members");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String name = this.fromApiJsonHelper.extractStringNamed("name", element);
        baseDataValidator.reset().parameter("name").value(name).notNull().notBlank().notExceedingLengthOf(100);

        final String dpi = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.dpiParamName, element);
        baseDataValidator.reset().parameter(ClientApiConstants.dpiParamName).value(dpi).notNull().notBlank().notExceedingLengthOf(20);

        ClientIdentifierDocumentValidator.checkDPI(dpi, ClientApiConstants.dpiParamName);

        final BigDecimal requestedAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("amount", element);
        baseDataValidator.reset().parameter("amount").value(requestedAmount).notNull().positiveAmount();

        if (this.fromApiJsonHelper.extractLocalDateNamed("dob", element) != null) {
            final LocalDate dateOfBirth = this.fromApiJsonHelper.extractLocalDateNamed("dob", element);
            baseDataValidator.reset().parameter("dob").value(dateOfBirth).value(dateOfBirth).notNull()
                    .validateDateBefore(DateUtils.getBusinessLocalDate());
        }
    }

    public void validateForUpdate(String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParametersForUpdate);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("members");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String name = this.fromApiJsonHelper.extractStringNamed(PrequalificatoinApiConstants.memberNameParamName, element);
        baseDataValidator.reset().parameter(PrequalificatoinApiConstants.memberNameParamName).value(name).notNull().notBlank()
                .notExceedingLengthOf(100);

        final String dpi = this.fromApiJsonHelper.extractStringNamed(PrequalificatoinApiConstants.memberDpiParamName, element);
        baseDataValidator.reset().parameter(PrequalificatoinApiConstants.memberDpiParamName).value(dpi).notNull().notBlank()
                .notExceedingLengthOf(20);
        ClientIdentifierDocumentValidator.checkDPI(dpi, PrequalificatoinApiConstants.memberDpiParamName);

        final BigDecimal requestedAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(PrequalificatoinApiConstants.memberRequestedAmountParamName, element);
        baseDataValidator.reset().parameter(PrequalificatoinApiConstants.memberRequestedAmountParamName).value(requestedAmount).notNull()
                .positiveAmount();

        if (this.fromApiJsonHelper.extractLocalDateNamed(PrequalificatoinApiConstants.memberDobParamName, element) != null) {
            final LocalDate dateOfBirth = this.fromApiJsonHelper.extractLocalDateNamed(PrequalificatoinApiConstants.memberDobParamName,
                    element);
            baseDataValidator.reset().parameter(PrequalificatoinApiConstants.memberDobParamName).value(dateOfBirth).value(dateOfBirth)
                    .notNull().validateDateBefore(DateUtils.getBusinessLocalDate());
        }

        final String workWithPuente = this.fromApiJsonHelper.extractStringNamed(PrequalificatoinApiConstants.memberWorkWithPuenteParamName,
                element);
        baseDataValidator.reset().parameter(PrequalificatoinApiConstants.memberWorkWithPuenteParamName).value(workWithPuente).notNull()
                .notBlank().notExceedingLengthOf(100);

    }
}
