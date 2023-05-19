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
package org.apache.fineract.organisation.centerGroup.serialization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.agency.service.AgencyConstants;
import org.apache.fineract.organisation.centerGroup.service.CenterGroupConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Deserializer of JSON for supervision API.
 */
@Component
public class CenterGroupCommandFromApiJsonDeserializer {

    /**
     * The parameters supported for this command.
     */
    private final Set<String> supportedParameters = CenterGroupConstants.CenterGroupSupportedParameters.getAllValues();

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public CenterGroupCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
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
                .resource(AgencyConstants.AGENCY_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String name = this.fromApiJsonHelper.extractStringNamed(CenterGroupConstants.CenterGroupSupportedParameters.NAME.getValue(),
                element);
        baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.NAME.getValue()).value(name).notBlank()
                .notExceedingLengthOf(60);

        final Long portfolioCenterId = this.fromApiJsonHelper
                .extractLongNamed(CenterGroupConstants.CenterGroupSupportedParameters.PORTFOLIO_CENTER_ID.getValue(), element);
        baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.PORTFOLIO_CENTER_ID.getValue())
                .value(portfolioCenterId).notNull().integerGreaterThanZero();

        final Integer size = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(CenterGroupConstants.CenterGroupSupportedParameters.SIZE.getValue(), element);
        baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.SIZE.getValue()).value(size).notNull()
                .integerZeroOrGreater();

        final Integer statusId = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(CenterGroupConstants.CenterGroupSupportedParameters.STATUS_ID.getValue(), element);
        baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.STATUS_ID.getValue()).value(statusId)
                .notNull().integerGreaterThanZero();

        if (this.fromApiJsonHelper.parameterExists(CenterGroupConstants.CenterGroupSupportedParameters.LEGACY_GROUP_NUMBER.getValue(),
                element)) {
            final Long legacyCenterNumber = this.fromApiJsonHelper
                    .extractLongNamed(CenterGroupConstants.CenterGroupSupportedParameters.LEGACY_GROUP_NUMBER.getValue(), element);
            baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.LEGACY_GROUP_NUMBER.getValue())
                    .value(legacyCenterNumber).ignoreIfNull().longGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(CenterGroupConstants.CenterGroupSupportedParameters.LATITUDE.getValue(), element)) {
            final BigDecimal latitude = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(CenterGroupConstants.CenterGroupSupportedParameters.LATITUDE.getValue(), element);
            baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.LATITUDE.getValue()).value(latitude)
                    .ignoreIfNull().zeroOrPositiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists(CenterGroupConstants.CenterGroupSupportedParameters.LONGITUDE.getValue(), element)) {
            final BigDecimal longitude = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(CenterGroupConstants.CenterGroupSupportedParameters.LONGITUDE.getValue(), element);
            baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.LONGITUDE.getValue()).value(longitude)
                    .ignoreIfNull().zeroOrPositiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists(CenterGroupConstants.CenterGroupSupportedParameters.FORMATION_DATE.getValue(),
                element)) {
            final LocalDate formationDate = this.fromApiJsonHelper
                    .extractLocalDateNamed(CenterGroupConstants.CenterGroupSupportedParameters.FORMATION_DATE.getValue(), element);
            baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.FORMATION_DATE.getValue())
                    .value(formationDate).ignoreIfNull().validateDateBefore(DateUtils.getBusinessLocalDate());
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForUpdate(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CenterGroupConstants.CENTER_GROUP_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String name = this.fromApiJsonHelper.extractStringNamed(CenterGroupConstants.CenterGroupSupportedParameters.NAME.getValue(),
                element);
        baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.NAME.getValue()).value(name).notBlank()
                .notExceedingLengthOf(60);

        final Long portfolioCenterId = this.fromApiJsonHelper
                .extractLongNamed(CenterGroupConstants.CenterGroupSupportedParameters.PORTFOLIO_CENTER_ID.getValue(), element);
        baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.PORTFOLIO_CENTER_ID.getValue())
                .value(portfolioCenterId).notNull().integerGreaterThanZero();

        final Integer size = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(CenterGroupConstants.CenterGroupSupportedParameters.SIZE.getValue(), element);
        baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.SIZE.getValue()).value(size).notNull()
                .integerZeroOrGreater();

        final Integer statusId = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(CenterGroupConstants.CenterGroupSupportedParameters.STATUS_ID.getValue(), element);
        baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.STATUS_ID.getValue()).value(statusId)
                .notNull().integerGreaterThanZero();

        if (this.fromApiJsonHelper.parameterExists(CenterGroupConstants.CenterGroupSupportedParameters.LEGACY_GROUP_NUMBER.getValue(),
                element)) {
            final Long legacyCenterNumber = this.fromApiJsonHelper
                    .extractLongNamed(CenterGroupConstants.CenterGroupSupportedParameters.LEGACY_GROUP_NUMBER.getValue(), element);
            baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.LEGACY_GROUP_NUMBER.getValue())
                    .value(legacyCenterNumber).ignoreIfNull().longGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(CenterGroupConstants.CenterGroupSupportedParameters.LATITUDE.getValue(), element)) {
            final BigDecimal latitude = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(CenterGroupConstants.CenterGroupSupportedParameters.LATITUDE.getValue(), element);
            baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.LATITUDE.getValue()).value(latitude)
                    .ignoreIfNull().zeroOrPositiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists(CenterGroupConstants.CenterGroupSupportedParameters.LONGITUDE.getValue(), element)) {
            final BigDecimal longitude = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(CenterGroupConstants.CenterGroupSupportedParameters.LONGITUDE.getValue(), element);
            baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.LONGITUDE.getValue()).value(longitude)
                    .ignoreIfNull().zeroOrPositiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists(CenterGroupConstants.CenterGroupSupportedParameters.FORMATION_DATE.getValue(),
                element)) {
            final LocalDate formationDate = this.fromApiJsonHelper
                    .extractLocalDateNamed(CenterGroupConstants.CenterGroupSupportedParameters.FORMATION_DATE.getValue(), element);
            baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.FORMATION_DATE.getValue())
                    .value(formationDate).ignoreIfNull().validateDateBefore(DateUtils.getBusinessLocalDate());
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForTransfer(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CenterGroupConstants.CENTER_GROUP_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final Long destinationPortfolioCenterId = this.fromApiJsonHelper
                .extractLongNamed(CenterGroupConstants.CenterGroupSupportedParameters.DESTINATION_PORTFOLIO_CENTER_ID.getValue(), element);
        baseDataValidator.reset().parameter(CenterGroupConstants.CenterGroupSupportedParameters.DESTINATION_PORTFOLIO_CENTER_ID.getValue())
                .value(destinationPortfolioCenterId).notNull().integerGreaterThanZero();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }
}
