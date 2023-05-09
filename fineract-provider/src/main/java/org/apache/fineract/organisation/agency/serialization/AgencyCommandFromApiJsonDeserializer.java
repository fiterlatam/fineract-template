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
package org.apache.fineract.organisation.agency.serialization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import org.apache.fineract.organisation.agency.service.AgencyConstants;
import org.apache.fineract.organisation.agency.service.AgencyConstants.AgencySupportedParameters;
import org.apache.fineract.portfolio.cupo.api.CupoApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Deserializer of JSON for agency API.
 */
@Component
public final class AgencyCommandFromApiJsonDeserializer {

    /**
     * The parameters supported for this command.
     */
    private final Set<String> supportedParameters = AgencyConstants.AgencySupportedParameters.getAllValues();

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public AgencyCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
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

        final String name = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.NAME.getValue(), element);
        baseDataValidator.reset().parameter(AgencySupportedParameters.NAME.getValue()).value(name).notBlank().notExceedingLengthOf(60);

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.OFFICE_PARENT_ID.getValue(), element)) {
            final Long parentId = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.OFFICE_PARENT_ID.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.OFFICE_PARENT_ID.getValue()).value(parentId).notNull()
                    .integerGreaterThanZero();
        }

        final String address = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.ADDRESS.getValue(), element);
        baseDataValidator.reset().parameter(AgencySupportedParameters.ADDRESS.getValue()).value(address).notBlank()
                .notExceedingLengthOf(250);

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.CITY_ID.getValue(), element)) {
            final Long cityId = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.CITY_ID.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.CITY_ID.getValue()).value(cityId).ignoreIfNull();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.STATE_ID.getValue(), element)) {
            final Long stateId = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.STATE_ID.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.STATE_ID.getValue()).value(stateId).ignoreIfNull();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.COUNTRY_ID.getValue(), element)) {
            final Long countryId = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.COUNTRY_ID.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.COUNTRY_ID.getValue()).value(countryId).ignoreIfNull()
                    .integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.ENTITY_CODE.getValue(), element)) {
            final Long entityCode = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.ENTITY_CODE.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.ENTITY_CODE.getValue()).value(entityCode).notNull()
                    .integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.CURRENCY_CODE.getValue(), element)) {
            final String currencyCode = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.CURRENCY_CODE.getValue(),
                    element);
            baseDataValidator.reset().parameter(CupoApiConstants.currencyCodeParamName).value(currencyCode).notNull().notBlank();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.AGENCY_TYPE.getValue(), element)) {
            final Long agencyType = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.AGENCY_TYPE.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.AGENCY_TYPE.getValue()).value(agencyType).notNull()
                    .integerGreaterThanZero();
        }

        final String phone = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.PHONE.getValue(), element);
        baseDataValidator.reset().parameter(AgencySupportedParameters.PHONE.getValue()).value(phone).ignoreIfNull()
                .notExceedingLengthOf(20);

        final String telex = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.TELEX.getValue(), element);
        baseDataValidator.reset().parameter(AgencySupportedParameters.TELEX.getValue()).value(telex).ignoreIfNull()
                .notExceedingLengthOf(50);

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.LABOUR_DAY_FROM.getValue(), element)) {
            final Long labourDayFrom = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.LABOUR_DAY_FROM.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.LABOUR_DAY_FROM.getValue()).value(labourDayFrom).ignoreIfNull()
                    .integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.LABOUR_DAY_TO.getValue(), element)) {
            final Long labourDayTo = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.LABOUR_DAY_TO.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.LABOUR_DAY_TO.getValue()).value(labourDayTo).ignoreIfNull()
                    .integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.OPEN_HOUR_MORNING.getValue(), element)) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
            String time = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.OPEN_HOUR_MORNING.getValue(), element);
            if (time != null) {
                try {
                    LocalTime openHourMorning = LocalTime.parse(time, dateTimeFormatter);
                } catch (DateTimeParseException parseException) {
                    dataValidationErrors.add(ApiParameterError.parameterError("error.msg.agency.openHourMorning.cannot.be.parsed",
                            "Open Hour Morning can not be parsed as a valid time", AgencySupportedParameters.OPEN_HOUR_MORNING.getValue()));
                }
            }
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.OPEN_HOUR_AFTERNOON.getValue(), element)) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
            String time = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.OPEN_HOUR_AFTERNOON.getValue(), element);
            if (time != null) {
                try {
                    LocalTime openHourAfternoon = LocalTime.parse(time, dateTimeFormatter);
                } catch (DateTimeParseException parseException) {
                    dataValidationErrors.add(ApiParameterError.parameterError("error.msg.agency.openHourAfternoon.cannot.be.parsed",
                            "Open Hour Afternoon can not be parsed as a valid time",
                            AgencySupportedParameters.OPEN_HOUR_AFTERNOON.getValue()));
                }
            }
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.FINANCIAL_YEAR_FROM.getValue(), element)) {
            final Long financialYearFrom = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.FINANCIAL_YEAR_FROM.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.FINANCIAL_YEAR_FROM.getValue()).value(financialYearFrom)
                    .ignoreIfNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.FINANCIAL_YEAR_TO.getValue(), element)) {
            final Long financialYearTo = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.FINANCIAL_YEAR_TO.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.FINANCIAL_YEAR_TO.getValue()).value(financialYearTo)
                    .ignoreIfNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.NON_BUSINESS_DAY1.getValue(), element)) {
            final Long nonBusinessDay1 = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.NON_BUSINESS_DAY1.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.NON_BUSINESS_DAY1.getValue()).value(nonBusinessDay1)
                    .ignoreIfNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.NON_BUSINESS_DAY2.getValue(), element)) {
            final Long nonBusinessDay2 = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.NON_BUSINESS_DAY2.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.NON_BUSINESS_DAY2.getValue()).value(nonBusinessDay2)
                    .ignoreIfNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.HALF_BUSINESS_DAY1.getValue(), element)) {
            final Long halfBusinessDay1 = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.HALF_BUSINESS_DAY1.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.HALF_BUSINESS_DAY1.getValue()).value(halfBusinessDay1)
                    .ignoreIfNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.HALF_BUSINESS_DAY2.getValue(), element)) {
            final Long halfBusinessDay2 = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.HALF_BUSINESS_DAY2.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.HALF_BUSINESS_DAY2.getValue()).value(halfBusinessDay2)
                    .ignoreIfNull().integerGreaterThanZero();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForUpdate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("agency");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        // Get the formatter
        final String dateFormat = this.fromApiJsonHelper.extractDateFormatParameter(element.getAsJsonObject());
        final String timeFormat = "HH:mm z";

        final String name = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.NAME.getValue(), element);
        baseDataValidator.reset().parameter(AgencySupportedParameters.NAME.getValue()).value(name).notBlank().notExceedingLengthOf(60);

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.OFFICE_PARENT_ID.getValue(), element)) {
            final Long parentId = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.OFFICE_PARENT_ID.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.OFFICE_PARENT_ID.getValue()).value(parentId).notNull()
                    .integerGreaterThanZero();
        }

        final String address = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.ADDRESS.getValue(), element);
        baseDataValidator.reset().parameter(AgencySupportedParameters.ADDRESS.getValue()).value(address).notBlank()
                .notExceedingLengthOf(250);

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.CITY_ID.getValue(), element)) {
            final Long cityId = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.CITY_ID.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.CITY_ID.getValue()).value(cityId).ignoreIfNull();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.STATE_ID.getValue(), element)) {
            final Long stateId = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.STATE_ID.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.STATE_ID.getValue()).value(stateId).ignoreIfNull();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.COUNTRY_ID.getValue(), element)) {
            final Long countryId = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.COUNTRY_ID.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.COUNTRY_ID.getValue()).value(countryId).ignoreIfNull()
                    .integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.ENTITY_CODE.getValue(), element)) {
            final Long entityCode = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.ENTITY_CODE.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.ENTITY_CODE.getValue()).value(entityCode).notNull()
                    .integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.CURRENCY_CODE.getValue(), element)) {
            final String currencyCode = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.CURRENCY_CODE.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.CURRENCY_CODE.getValue()).value(currencyCode).notNull()
                    .notBlank();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.AGENCY_TYPE.getValue(), element)) {
            final Long agencyType = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.AGENCY_TYPE.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.AGENCY_TYPE.getValue()).value(agencyType).notNull()
                    .integerGreaterThanZero();
        }

        final String phone = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.PHONE.getValue(), element);
        baseDataValidator.reset().parameter(AgencySupportedParameters.PHONE.getValue()).value(phone).ignoreIfNull()
                .notExceedingLengthOf(20);

        final String telex = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.TELEX.getValue(), element);
        baseDataValidator.reset().parameter(AgencySupportedParameters.TELEX.getValue()).value(telex).ignoreIfNull()
                .notExceedingLengthOf(50);

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.LABOUR_DAY_FROM.getValue(), element)) {
            final Long labourDayFrom = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.LABOUR_DAY_FROM.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.LABOUR_DAY_FROM.getValue()).value(labourDayFrom).ignoreIfNull()
                    .integerZeroOrGreater();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.LABOUR_DAY_TO.getValue(), element)) {
            final Long labourDayTo = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.LABOUR_DAY_TO.getValue(), element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.LABOUR_DAY_TO.getValue()).value(labourDayTo).ignoreIfNull()
                    .integerZeroOrGreater();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.OPEN_HOUR_MORNING.getValue(), element)) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
            String time = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.OPEN_HOUR_MORNING.getValue(), element);
            if (time != null) {
                try {
                    LocalTime openHourMorning = LocalTime.parse(time, dateTimeFormatter);
                } catch (DateTimeParseException parseException) {
                    dataValidationErrors.add(ApiParameterError.parameterError("error.msg.agency.openHourMorning.cannot.be.parsed",
                            "Open Hour Morning can not be parsed as a valid time", AgencySupportedParameters.OPEN_HOUR_MORNING.getValue()));
                }
            }
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.OPEN_HOUR_AFTERNOON.getValue(), element)) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
            String time = this.fromApiJsonHelper.extractStringNamed(AgencySupportedParameters.OPEN_HOUR_AFTERNOON.getValue(), element);
            if (time != null) {
                try {
                    LocalTime openHourAfternoon = LocalTime.parse(time, dateTimeFormatter);
                } catch (DateTimeParseException parseException) {
                    dataValidationErrors.add(ApiParameterError.parameterError("error.msg.agency.openHourAfternoon.cannot.be.parsed",
                            "Open Hour Afternoon can not be parsed as a valid time",
                            AgencySupportedParameters.OPEN_HOUR_AFTERNOON.getValue()));
                }
            }
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.FINANCIAL_YEAR_FROM.getValue(), element)) {
            final Long financialYearFrom = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.FINANCIAL_YEAR_FROM.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.FINANCIAL_YEAR_FROM.getValue()).value(financialYearFrom)
                    .ignoreIfNull().integerZeroOrGreater();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.FINANCIAL_YEAR_TO.getValue(), element)) {
            final Long financialYearTo = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.FINANCIAL_YEAR_TO.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.FINANCIAL_YEAR_TO.getValue()).value(financialYearTo)
                    .ignoreIfNull().integerZeroOrGreater();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.NON_BUSINESS_DAY1.getValue(), element)) {
            final Long nonBusinessDay1 = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.NON_BUSINESS_DAY1.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.NON_BUSINESS_DAY1.getValue()).value(nonBusinessDay1)
                    .ignoreIfNull().integerZeroOrGreater();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.NON_BUSINESS_DAY2.getValue(), element)) {
            final Long nonBusinessDay2 = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.NON_BUSINESS_DAY2.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.NON_BUSINESS_DAY2.getValue()).value(nonBusinessDay2)
                    .ignoreIfNull().integerZeroOrGreater();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.HALF_BUSINESS_DAY1.getValue(), element)) {
            final Long halfBusinessDay1 = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.HALF_BUSINESS_DAY1.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.HALF_BUSINESS_DAY1.getValue()).value(halfBusinessDay1)
                    .ignoreIfNull().integerZeroOrGreater();
        }

        if (this.fromApiJsonHelper.parameterExists(AgencySupportedParameters.HALF_BUSINESS_DAY2.getValue(), element)) {
            final Long halfBusinessDay2 = this.fromApiJsonHelper.extractLongNamed(AgencySupportedParameters.HALF_BUSINESS_DAY2.getValue(),
                    element);
            baseDataValidator.reset().parameter(AgencySupportedParameters.HALF_BUSINESS_DAY2.getValue()).value(halfBusinessDay2)
                    .ignoreIfNull().integerZeroOrGreater();
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
