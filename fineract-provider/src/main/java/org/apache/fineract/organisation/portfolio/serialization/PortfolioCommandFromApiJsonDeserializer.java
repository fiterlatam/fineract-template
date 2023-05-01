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
package org.apache.fineract.organisation.portfolio.serialization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
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
import org.apache.fineract.organisation.portfolio.service.PortfolioConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Deserializer of JSON for supervision API.
 */
@Component
public class PortfolioCommandFromApiJsonDeserializer {

    /**
     * The parameters supported for this command.
     */
    private final Set<String> supportedParameters = PortfolioConstants.PortfolioSupportedParameters.getAllValues();

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public PortfolioCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(PortfolioConstants.PORTFOLIO_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String name = this.fromApiJsonHelper.extractStringNamed(PortfolioConstants.PortfolioSupportedParameters.NAME.getValue(),
                element);
        baseDataValidator.reset().parameter(PortfolioConstants.PortfolioSupportedParameters.NAME.getValue()).value(name).notBlank()
                .notExceedingLengthOf(60);

        if (this.fromApiJsonHelper.parameterExists(PortfolioConstants.PortfolioSupportedParameters.OFFICE_PARENT_ID.getValue(), element)) {
            final Long parentId = this.fromApiJsonHelper
                    .extractLongNamed(PortfolioConstants.PortfolioSupportedParameters.OFFICE_PARENT_ID.getValue(), element);
            baseDataValidator.reset().parameter(PortfolioConstants.PortfolioSupportedParameters.OFFICE_PARENT_ID.getValue()).value(parentId)
                    .notNull().integerGreaterThanZero();
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
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("office");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String name = this.fromApiJsonHelper.extractStringNamed(PortfolioConstants.PortfolioSupportedParameters.NAME.getValue(),
                element);
        baseDataValidator.reset().parameter(PortfolioConstants.PortfolioSupportedParameters.NAME.getValue()).value(name).notBlank()
                .notExceedingLengthOf(60);

        if (this.fromApiJsonHelper.parameterExists(PortfolioConstants.PortfolioSupportedParameters.OFFICE_PARENT_ID.getValue(), element)) {
            final Long parentId = this.fromApiJsonHelper
                    .extractLongNamed(PortfolioConstants.PortfolioSupportedParameters.OFFICE_PARENT_ID.getValue(), element);
            baseDataValidator.reset().parameter(PortfolioConstants.PortfolioSupportedParameters.OFFICE_PARENT_ID.getValue()).value(parentId)
                    .notNull().integerGreaterThanZero();
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
