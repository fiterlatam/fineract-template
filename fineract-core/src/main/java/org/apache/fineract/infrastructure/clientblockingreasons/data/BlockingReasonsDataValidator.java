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
package org.apache.fineract.infrastructure.clientblockingreasons.data;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.clientblockingreasons.api.BlockingReasonsConstants;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BlockingReasonsDataValidator {

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public BlockingReasonsDataValidator(FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    private static final Logger LOG = LoggerFactory.getLogger(BlockingReasonsDataValidator.class);

    private static final Set<String> MANAGE_BLOCKING_REASONS_DATA_PARAMETERS = new HashSet<>(Arrays.asList(
            BlockingReasonsConstants.ID_PARAM, BlockingReasonsConstants.CREDIT_LEVEL_PARAM, BlockingReasonsConstants.CUSTOMER_LEVEL_PARAM,
            BlockingReasonsConstants.NAME_OF_REASON_PARAM, BlockingReasonsConstants.DESCRIPTION_PARAM, BlockingReasonsConstants.LEVEL_PARAM,
            BlockingReasonsConstants.PRIORITY_PARAM));

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, MANAGE_BLOCKING_REASONS_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(BlockingReasonsConstants.ENTITY_NAME);

        final Integer priority = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(BlockingReasonsConstants.PRIORITY_PARAM, element);
        baseDataValidator.reset().parameter(BlockingReasonsConstants.PRIORITY_PARAM).value(priority).notNull().integerGreaterThanZero()
                .inMinMaxRange(1, 100);

        final String nameOfReason = this.fromApiJsonHelper.extractStringNamed(BlockingReasonsConstants.NAME_OF_REASON_PARAM, element);
        baseDataValidator.reset().parameter(BlockingReasonsConstants.NAME_OF_REASON_PARAM).value(nameOfReason).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

    }

    public void validateForUpdate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, MANAGE_BLOCKING_REASONS_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(BlockingReasonsConstants.ENTITY_NAME);
        boolean atLeastOneParameterPassedForUpdate = false;

        if (this.fromApiJsonHelper.parameterExists(BlockingReasonsConstants.PRIORITY_PARAM, element)) {
            atLeastOneParameterPassedForUpdate = true;

            final Integer priority = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(BlockingReasonsConstants.PRIORITY_PARAM, element);
            baseDataValidator.reset().parameter(BlockingReasonsConstants.PRIORITY_PARAM).value(priority).notNull().integerGreaterThanZero()
                    .inMinMaxRange(1, 100);
        }

        if (this.fromApiJsonHelper.parameterExists(BlockingReasonsConstants.NAME_OF_REASON_PARAM, element)) {
            atLeastOneParameterPassedForUpdate = true;

            final String nameOfReason = this.fromApiJsonHelper.extractStringNamed(BlockingReasonsConstants.NAME_OF_REASON_PARAM, element);
            baseDataValidator.reset().parameter(BlockingReasonsConstants.NAME_OF_REASON_PARAM).value(nameOfReason).notNull();
        }
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
