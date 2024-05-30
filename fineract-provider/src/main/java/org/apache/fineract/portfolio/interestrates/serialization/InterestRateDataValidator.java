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
package org.apache.fineract.portfolio.interestrates.serialization;

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
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InterestRateDataValidator {

    public static final String NAME = "name";
    public static final String CURRENT_RATE = "currentRate";
    public static final String ACTIVE = "active";
    public static final String APPLIED_ON_DATE = "appliedOnDate";
    public static final String LOCALE = "locale";
    public static final String DATE_FORMAT = "dateFormat";
    private static final Set<String> SUPPORTED_PARAMETERS_FOR_INTEREST_RATES = new HashSet<>(
            Arrays.asList(InterestRateDataValidator.NAME, InterestRateDataValidator.CURRENT_RATE, InterestRateDataValidator.ACTIVE,
                    InterestRateDataValidator.APPLIED_ON_DATE, InterestRateDataValidator.LOCALE, InterestRateDataValidator.DATE_FORMAT));
    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public InterestRateDataValidator(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(String json) {
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                InterestRateDataValidator.SUPPORTED_PARAMETERS_FOR_INTEREST_RATES);
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("interest.rate");
        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final String name = this.fromApiJsonHelper.extractStringNamed(InterestRateDataValidator.NAME, element);
        baseDataValidator.reset().parameter(InterestRateDataValidator.NAME).value(name).notBlank().notExceedingLengthOf(100);
        final BigDecimal currentRate = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(InterestRateDataValidator.CURRENT_RATE,
                element);
        baseDataValidator.reset().parameter(InterestRateDataValidator.CURRENT_RATE).value(currentRate).notBlank()
                .inMinAndMaxAmountRange(BigDecimal.ZERO, BigDecimal.valueOf(100));
        final Boolean active = this.fromApiJsonHelper.extractBooleanNamed(InterestRateDataValidator.ACTIVE, element);
        baseDataValidator.reset().parameter(InterestRateDataValidator.ACTIVE).value(active).notNull();
        final LocalDate appliedOnDate = this.fromApiJsonHelper.extractLocalDateNamed(InterestRateDataValidator.APPLIED_ON_DATE, element);
        baseDataValidator.reset().parameter(InterestRateDataValidator.APPLIED_ON_DATE).value(appliedOnDate).notNull();
        if (DateUtils.getBusinessLocalDate().isAfter(appliedOnDate)) {
            baseDataValidator.reset().parameter(InterestRateDataValidator.APPLIED_ON_DATE).value(appliedOnDate)
                    .failWithCode("cannot.be.before.today");
        }
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForUpdate(String json) {
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                InterestRateDataValidator.SUPPORTED_PARAMETERS_FOR_INTEREST_RATES);
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("interest.rate");
        final JsonElement element = this.fromApiJsonHelper.parse(json);
        if (this.fromApiJsonHelper.parameterExists(InterestRateDataValidator.NAME, element)) {
            final String name = this.fromApiJsonHelper.extractStringNamed(InterestRateDataValidator.NAME, element);
            baseDataValidator.reset().parameter(InterestRateDataValidator.NAME).value(name).notBlank().notExceedingLengthOf(100);
        }
        if (this.fromApiJsonHelper.parameterExists(InterestRateDataValidator.CURRENT_RATE, element)) {
            final BigDecimal currentRate = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(InterestRateDataValidator.CURRENT_RATE,
                    element);
            baseDataValidator.reset().parameter(InterestRateDataValidator.CURRENT_RATE).value(currentRate).notBlank()
                    .inMinAndMaxAmountRange(BigDecimal.ZERO, BigDecimal.valueOf(100));
        }
        if (this.fromApiJsonHelper.parameterExists(InterestRateDataValidator.ACTIVE, element)) {
            final Boolean active = this.fromApiJsonHelper.extractBooleanNamed(InterestRateDataValidator.ACTIVE, element);
            baseDataValidator.reset().parameter(InterestRateDataValidator.ACTIVE).value(active).notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(InterestRateDataValidator.APPLIED_ON_DATE, element)) {
            final LocalDate appliedOnDate = this.fromApiJsonHelper.extractLocalDateNamed(InterestRateDataValidator.APPLIED_ON_DATE,
                    element);
            baseDataValidator.reset().parameter(InterestRateDataValidator.APPLIED_ON_DATE).value(appliedOnDate).notNull();
            if (DateUtils.getBusinessLocalDate().isAfter(appliedOnDate)) {
                baseDataValidator.reset().parameter(InterestRateDataValidator.APPLIED_ON_DATE).value(appliedOnDate)
                        .failWithCode("cannot.be.before.today");
            }
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
