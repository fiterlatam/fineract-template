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
package org.apache.fineract.custom.portfolio.loanproduct.validator;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.custom.portfolio.loanproduct.constants.SubChannelLoanProductApiConstants;
import org.apache.fineract.custom.portfolio.loanproduct.domain.SubChannelLoanProduct;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubChannelLoanProductDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final PlatformSecurityContext platformSecurityContext;

    @Autowired
    public SubChannelLoanProductDataValidator(final FromJsonHelper fromApiJsonHelper,
            final PlatformSecurityContext platformSecurityContext) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.platformSecurityContext = platformSecurityContext;
    }

    public SubChannelLoanProduct validateForCreate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, SubChannelLoanProductApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SubChannelLoanProductApiConstants.RESOURCE_NAME);

        final Long id = this.fromApiJsonHelper.extractLongNamed(SubChannelLoanProductApiConstants.idParamName, element);
        baseDataValidator.reset().parameter(SubChannelLoanProductApiConstants.idParamName).value(id).notNull();

        final Long subChannelId = this.fromApiJsonHelper.extractLongNamed(SubChannelLoanProductApiConstants.subChannelIdParamName, element);
        baseDataValidator.reset().parameter(SubChannelLoanProductApiConstants.subChannelIdParamName).value(subChannelId).notNull();

        final Long loanProductId = this.fromApiJsonHelper.extractLongNamed(SubChannelLoanProductApiConstants.loanProductIdParamName,
                element);
        baseDataValidator.reset().parameter(SubChannelLoanProductApiConstants.loanProductIdParamName).value(loanProductId).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return SubChannelLoanProduct.builder().id(id) //
                .subChannelId(subChannelId) //
                .loanProductId(loanProductId) //

                .build();
    }

    public SubChannelLoanProduct validateForUpdate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, SubChannelLoanProductApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SubChannelLoanProductApiConstants.RESOURCE_NAME);

        final Long id = this.fromApiJsonHelper.extractLongNamed(SubChannelLoanProductApiConstants.idParamName, element);
        baseDataValidator.reset().parameter(SubChannelLoanProductApiConstants.idParamName).value(id).notNull();

        final Long subChannelId = this.fromApiJsonHelper.extractLongNamed(SubChannelLoanProductApiConstants.subChannelIdParamName, element);
        baseDataValidator.reset().parameter(SubChannelLoanProductApiConstants.subChannelIdParamName).value(subChannelId).notNull();

        final Long loanProductId = this.fromApiJsonHelper.extractLongNamed(SubChannelLoanProductApiConstants.loanProductIdParamName,
                element);
        baseDataValidator.reset().parameter(SubChannelLoanProductApiConstants.loanProductIdParamName).value(loanProductId).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return SubChannelLoanProduct.builder().id(id) //
                .subChannelId(subChannelId) //
                .loanProductId(loanProductId) //

                .build();
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
