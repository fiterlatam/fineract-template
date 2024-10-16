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
package org.apache.fineract.portfolio.cupo.serialization;

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
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.cupo.api.CupoApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CupoCommandFromApiJsonDeserializer {

    private final Set<String> supportedParameters = new HashSet<>(
            Arrays.asList(CupoApiConstants.amountParamName, CupoApiConstants.expirationDateParamName, CupoApiConstants.clientIdParamName,
                    CupoApiConstants.groupIdParamName, "locale", "dateFormat", CupoApiConstants.currencyCodeParamName));
    private final Set<String> supportedParametersForUpdate = new HashSet<>(
            Arrays.asList(CupoApiConstants.amountParamName, CupoApiConstants.expirationDateParamName, "locale", "dateFormat"));

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public CupoCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("cupo");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final BigDecimal amount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(CupoApiConstants.amountParamName, element);
        baseDataValidator.reset().parameter(CupoApiConstants.amountParamName).value(amount).notNull().positiveAmount();

        final LocalDate expirationDate = this.fromApiJsonHelper.extractLocalDateNamed(CupoApiConstants.expirationDateParamName, element);
        baseDataValidator.reset().parameter(CupoApiConstants.expirationDateParamName).value(expirationDate).notNull();

        final String currencyCode = this.fromApiJsonHelper.extractStringNamed(CupoApiConstants.currencyCodeParamName, element);
        baseDataValidator.reset().parameter(CupoApiConstants.currencyCodeParamName).value(currencyCode).notNull().notBlank();

        final Long clientId = this.fromApiJsonHelper.extractLongNamed(CupoApiConstants.clientIdParamName, element);
        final Long groupId = this.fromApiJsonHelper.extractLongNamed(CupoApiConstants.groupIdParamName, element);

        if (clientId == null && groupId == null) {
            baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode("error.no.client.or.group.attached.to.cupo");
        } else if (clientId != null && groupId != null) {
            baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode("error.cupo.can.only.be.attached.to.one.group.or.client");
        }

        baseDataValidator.reset().parameter(CupoApiConstants.clientIdParamName).value(clientId).ignoreIfNull().longGreaterThanZero();
        baseDataValidator.reset().parameter(CupoApiConstants.clientIdParamName).value(groupId).ignoreIfNull().longGreaterThanZero();

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
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("cupo");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final BigDecimal amount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(CupoApiConstants.amountParamName, element);
        baseDataValidator.reset().parameter(CupoApiConstants.amountParamName).value(amount).notNull().positiveAmount();

        final LocalDate expirationDate = this.fromApiJsonHelper.extractLocalDateNamed(CupoApiConstants.expirationDateParamName, element);
        baseDataValidator.reset().parameter(CupoApiConstants.expirationDateParamName).value(expirationDate).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }
}
