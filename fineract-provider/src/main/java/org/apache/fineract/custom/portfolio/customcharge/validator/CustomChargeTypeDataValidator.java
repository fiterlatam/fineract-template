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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.custom.portfolio.customcharge.constants.CustomChargeTypeApiConstants;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeType;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomChargeTypeDataValidator {

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public CustomChargeTypeDataValidator(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public CustomChargeType validateForCreate(final String json, final Long customChargeTypeId) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,  CustomChargeTypeApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CustomChargeTypeApiConstants.RESOURCE_NAME);

		final Long id = this.fromApiJsonHelper.extractLongNamed(CustomChargeTypeApiConstants.idParamName, element);
		baseDataValidator.reset().parameter(CustomChargeTypeApiConstants.idParamName).value(id).notNull();

		final Long customChargeEntityId = this.fromApiJsonHelper.extractLongNamed(CustomChargeTypeApiConstants.customChargeEntityIdParamName, element);
		baseDataValidator.reset().parameter(CustomChargeTypeApiConstants.customChargeEntityIdParamName).value(customChargeEntityId).notNull();

		final String name = this.fromApiJsonHelper.extractStringNamed(CustomChargeTypeApiConstants.nameParamName, element);
		baseDataValidator.reset().parameter(CustomChargeTypeApiConstants.nameParamName).value(name).notNull().notExceedingLengthOf(50);

		final String code = this.fromApiJsonHelper.extractStringNamed(CustomChargeTypeApiConstants.codeParamName, element);
		baseDataValidator.reset().parameter(CustomChargeTypeApiConstants.codeParamName).value(code).notNull().notExceedingLengthOf(100);


        throwExceptionIfValidationWarningsExist(dataValidationErrors);
        
        return new CustomChargeType(id,customChargeEntityId,name,code);
    }

    public CustomChargeType validateForUpdate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CustomChargeTypeApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CustomChargeTypeApiConstants.RESOURCE_NAME);
                
		final Long id = this.fromApiJsonHelper.extractLongNamed(CustomChargeTypeApiConstants.idParamName, element);
		baseDataValidator.reset().parameter(CustomChargeTypeApiConstants.idParamName).value(id).notNull();

		final Long customChargeEntityId = this.fromApiJsonHelper.extractLongNamed(CustomChargeTypeApiConstants.customChargeEntityIdParamName, element);
		baseDataValidator.reset().parameter(CustomChargeTypeApiConstants.customChargeEntityIdParamName).value(customChargeEntityId).notNull();

		final String name = this.fromApiJsonHelper.extractStringNamed(CustomChargeTypeApiConstants.nameParamName, element);
		baseDataValidator.reset().parameter(CustomChargeTypeApiConstants.nameParamName).value(name).notNull().notExceedingLengthOf(50);

		final String code = this.fromApiJsonHelper.extractStringNamed(CustomChargeTypeApiConstants.codeParamName, element);
		baseDataValidator.reset().parameter(CustomChargeTypeApiConstants.codeParamName).value(code).notNull().notExceedingLengthOf(100);
                
        
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
        
        return new CustomChargeType(id,customChargeEntityId,name,code);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
