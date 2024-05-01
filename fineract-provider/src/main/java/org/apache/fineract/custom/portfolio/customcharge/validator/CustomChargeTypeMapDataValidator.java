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
package org.apache.fineract.custom.portfolio.customcharge.validator;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.custom.portfolio.customcharge.constants.CustomChargeTypeMapApiConstants;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeTypeMap;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomChargeTypeMapDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final PlatformSecurityContext platformSecurityContext;
    private final ConfigurationReadPlatformService configurationReadPlatformService;

    @Autowired
    public CustomChargeTypeMapDataValidator(final FromJsonHelper fromApiJsonHelper, final PlatformSecurityContext platformSecurityContext,
            final ConfigurationReadPlatformService configurationReadPlatformService) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.platformSecurityContext = platformSecurityContext;
        this.configurationReadPlatformService = configurationReadPlatformService;
    }

    public CustomChargeTypeMap validateForCreate(final String json, final Long customChargeTypeId) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CustomChargeTypeMapApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CustomChargeTypeMapApiConstants.RESOURCE_NAME);

        final GlobalConfigurationPropertyData customTermLength = this.configurationReadPlatformService
                .retrieveGlobalConfiguration("custom-charge-aval-supay-max-term");

        final Long term = this.fromApiJsonHelper.extractLongNamed(CustomChargeTypeMapApiConstants.termParamName, element);
        baseDataValidator.reset().parameter(CustomChargeTypeMapApiConstants.termParamName).value(term).notNull()
                .notGreaterThanMax(customTermLength.getValue().intValue());

        final BigDecimal percentage = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(CustomChargeTypeMapApiConstants.percentageParamName, element);
        baseDataValidator.reset().parameter(CustomChargeTypeMapApiConstants.percentageParamName).value(percentage).notNull();

        final LocalDate validFrom = this.fromApiJsonHelper.extractLocalDateNamed(CustomChargeTypeMapApiConstants.validFromParamName,
                element);
        baseDataValidator.reset().parameter(CustomChargeTypeMapApiConstants.validFromParamName).value(validFrom).notNull();

        final LocalDate validTo = this.fromApiJsonHelper.extractLocalDateNamed(CustomChargeTypeMapApiConstants.validToParamName, element);
        baseDataValidator.reset().parameter(CustomChargeTypeMapApiConstants.validToParamName).value(validTo);

        final Boolean active = true;

        final Long createdBy = platformSecurityContext.authenticatedUser().getId();

        final LocalDateTime createdAt = DateUtils.getLocalDateTimeOfTenant();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return CustomChargeTypeMap.builder().customChargeTypeId(customChargeTypeId) //
                .term(term) //
                .percentage(percentage) //
                .validFrom(validFrom) //
                .validTo(validTo) //
                .active(active) //
                .createdBy(createdBy) //
                .createdAt(createdAt) //
                .build();
    }

    public CustomChargeTypeMap validateForUpdate(final String json, final Long customChargeTypeId) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CustomChargeTypeMapApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CustomChargeTypeMapApiConstants.RESOURCE_NAME);

        final GlobalConfigurationPropertyData customTermLength = this.configurationReadPlatformService
                .retrieveGlobalConfiguration("custom-charge-aval-supay-max-term");

        final Long term = this.fromApiJsonHelper.extractLongNamed(CustomChargeTypeMapApiConstants.termParamName, element);
        baseDataValidator.reset().parameter(CustomChargeTypeMapApiConstants.termParamName).value(term).notNull()
                .notGreaterThanMax(customTermLength.getValue().intValue());

        final BigDecimal percentage = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(CustomChargeTypeMapApiConstants.percentageParamName, element);
        baseDataValidator.reset().parameter(CustomChargeTypeMapApiConstants.percentageParamName).value(percentage).notNull();

        final LocalDate validFrom = this.fromApiJsonHelper.extractLocalDateNamed(CustomChargeTypeMapApiConstants.validFromParamName,
                element);
        baseDataValidator.reset().parameter(CustomChargeTypeMapApiConstants.validFromParamName).value(validFrom).notNull();

        final LocalDate validTo = this.fromApiJsonHelper.extractLocalDateNamed(CustomChargeTypeMapApiConstants.validToParamName, element);
        baseDataValidator.reset().parameter(CustomChargeTypeMapApiConstants.validToParamName).value(validTo);

        final Boolean active = true;

        final Long updatedBy = platformSecurityContext.authenticatedUser().getId();

        final LocalDateTime updatedAt = DateUtils.getLocalDateTimeOfTenant();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return CustomChargeTypeMap.builder().customChargeTypeId(customChargeTypeId) //
                .term(term) //
                .percentage(percentage) //
                .validFrom(validFrom) //
                .validTo(validTo) //
                .active(active) //
                .updatedBy(updatedBy) //
                .updatedAt(updatedAt) //
                .build();
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
