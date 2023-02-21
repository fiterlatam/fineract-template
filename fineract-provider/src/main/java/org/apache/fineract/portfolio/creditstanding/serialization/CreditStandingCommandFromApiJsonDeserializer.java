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
package org.apache.fineract.portfolio.creditstanding.serialization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
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
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.creditstanding.api.CreditStandingApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreditStandingCommandFromApiJsonDeserializer {

    private final Set<String> supportedParameters = new HashSet<>(Arrays.asList(CreditStandingApiConstants.mraParamName,
            CreditStandingApiConstants.rciMaxParamName, CreditStandingApiConstants.clientIdParamName, "locale"));

    private final Set<String> supportedParametersForUpdate = new HashSet<>(Arrays.asList(CreditStandingApiConstants.mraParamName,
            CreditStandingApiConstants.rciMaxParamName, CreditStandingApiConstants.clientIdParamName, "locale"));

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public CreditStandingCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CreditStandingApiConstants.CREDIT_STANDING_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final BigDecimal mra = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(CreditStandingApiConstants.mraParamName, element);
        baseDataValidator.reset().parameter(CreditStandingApiConstants.mraParamName).value(mra).integerGreaterThanZero();

        final BigDecimal rciMax = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(CreditStandingApiConstants.rciMaxParamName,
                element);
        baseDataValidator.reset().parameter(CreditStandingApiConstants.rciMaxParamName).value(rciMax).integerZeroOrGreater();

        final Long clientId = this.fromApiJsonHelper.extractLongNamed(CreditStandingApiConstants.clientIdParamName, element);

        if (clientId == null) {
            baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode("error.no.client.attached.to.creditStanding");
        }

        baseDataValidator.reset().parameter(CreditStandingApiConstants.clientIdParamName).value(clientId).ignoreIfNull()
                .longGreaterThanZero();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }

    public void validateForUpdate(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParametersForUpdate);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CreditStandingApiConstants.CREDIT_STANDING_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final BigDecimal mra = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(CreditStandingApiConstants.mraParamName, element);
        baseDataValidator.reset().parameter(CreditStandingApiConstants.mraParamName).value(mra).integerGreaterThanZero();

        final BigDecimal rciMax = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(CreditStandingApiConstants.rciMaxParamName,
                element);
        baseDataValidator.reset().parameter(CreditStandingApiConstants.rciMaxParamName).value(rciMax).integerZeroOrGreater();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }
}
