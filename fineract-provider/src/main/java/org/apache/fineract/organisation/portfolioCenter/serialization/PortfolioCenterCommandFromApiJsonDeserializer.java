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
package org.apache.fineract.organisation.portfolioCenter.serialization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.portfolioCenter.service.PortfolioCenterConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Deserializer of JSON for supervision API.
 */
@Component
public class PortfolioCenterCommandFromApiJsonDeserializer {

    /**
     * The parameters supported for this command.
     */
    private final Set<String> supportedParameters = PortfolioCenterConstants.PortfolioCenterSupportedParameters.getAllValues();

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public PortfolioCenterCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForUpdate(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("portfolioCenter");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String name = this.fromApiJsonHelper
                .extractStringNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.NAME.getValue(), element);
        baseDataValidator.reset().parameter(PortfolioCenterConstants.PortfolioCenterSupportedParameters.NAME.getValue()).value(name)
                .notBlank().notExceedingLengthOf(60);

        if (this.fromApiJsonHelper
                .parameterExists(PortfolioCenterConstants.PortfolioCenterSupportedParameters.LEGACY_CENTER_NUMBER.getValue(), element)) {
            final BigDecimal legacyCenterNumber = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(
                    PortfolioCenterConstants.PortfolioCenterSupportedParameters.LEGACY_CENTER_NUMBER.getValue(), element);
            baseDataValidator.reset().parameter(PortfolioCenterConstants.PortfolioCenterSupportedParameters.LEGACY_CENTER_NUMBER.getValue())
                    .value(legacyCenterNumber).ignoreIfNull().zeroOrPositiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists(PortfolioCenterConstants.PortfolioCenterSupportedParameters.CITY_ID.getValue(),
                element)) {
            final Long cityId = this.fromApiJsonHelper
                    .extractLongNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.CITY_ID.getValue(), element);
            baseDataValidator.reset().parameter(PortfolioCenterConstants.PortfolioCenterSupportedParameters.CITY_ID.getValue())
                    .value(cityId).ignoreIfNull();
        }

        if (this.fromApiJsonHelper.parameterExists(PortfolioCenterConstants.PortfolioCenterSupportedParameters.STATE_ID.getValue(),
                element)) {
            final Long stateId = this.fromApiJsonHelper
                    .extractLongNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.STATE_ID.getValue(), element);
            baseDataValidator.reset().parameter(PortfolioCenterConstants.PortfolioCenterSupportedParameters.STATE_ID.getValue())
                    .value(stateId).ignoreIfNull();
        }

        if (this.fromApiJsonHelper.parameterExists(PortfolioCenterConstants.PortfolioCenterSupportedParameters.CENTER_TYPE.getValue(),
                element)) {
            final Long centerType = this.fromApiJsonHelper
                    .extractLongNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.CENTER_TYPE.getValue(), element);
            baseDataValidator.reset().parameter(PortfolioCenterConstants.PortfolioCenterSupportedParameters.CENTER_TYPE.getValue())
                    .value(centerType).ignoreIfNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(PortfolioCenterConstants.PortfolioCenterSupportedParameters.STATUS_ID.getValue(),
                element)) {
            final Integer statusId = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(
                    PortfolioCenterConstants.PortfolioCenterSupportedParameters.STATUS_ID.getValue(), element);
            baseDataValidator.reset().parameter(PortfolioCenterConstants.PortfolioCenterSupportedParameters.STATUS_ID.getValue())
                    .value(statusId).notNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(PortfolioCenterConstants.PortfolioCenterSupportedParameters.DISTANCE.getValue(),
                element)) {
            final Integer distance = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(
                    PortfolioCenterConstants.PortfolioCenterSupportedParameters.DISTANCE.getValue(), element);
            baseDataValidator.reset().parameter(PortfolioCenterConstants.PortfolioCenterSupportedParameters.DISTANCE.getValue())
                    .value(distance).ignoreIfNull().integerZeroOrGreater();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }
}
