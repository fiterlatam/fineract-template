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
package org.apache.fineract.custom.portfolio.ally.validator;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.custom.portfolio.ally.api.ClientAllyPointOfSalesApiConstants;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSales;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClientAllyPointOfSalesDataValidator {

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public ClientAllyPointOfSalesDataValidator(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public ClientAllyPointOfSales validateForCreate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, ClientAllyPointOfSalesApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ClientAllyPointOfSalesApiConstants.RESOURCE_NAME);

        final String code = this.fromApiJsonHelper.extractStringNamed(ClientAllyPointOfSalesApiConstants.codeParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.codeParamName).value(code).notNull().notExceedingLengthOf(4);

        final String name = this.fromApiJsonHelper.extractStringNamed(ClientAllyPointOfSalesApiConstants.nameParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.nameParamName).value(name).notNull()
                .notExceedingLengthOf(100);

        final Long brand = this.fromApiJsonHelper.extractLongNamed(ClientAllyPointOfSalesApiConstants.brandParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.brandParamName).value(brand).notNull();

        final Long cityCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyPointOfSalesApiConstants.cityCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.cityCodeValueIdParamName).value(cityCodeValueId).notNull();

        final Long departmentCodeValueId = this.fromApiJsonHelper
                .extractLongNamed(ClientAllyPointOfSalesApiConstants.departmentCodeValueIdParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.departmentCodeValueIdParamName).value(departmentCodeValueId)
                .notNull();

        final Long categoryCodeValueId = this.fromApiJsonHelper
                .extractLongNamed(ClientAllyPointOfSalesApiConstants.categoryCodeValueIdParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.categoryCodeValueIdParamName).value(categoryCodeValueId)
                .notNull();

        final Long segmentCodeValueId = this.fromApiJsonHelper
                .extractLongNamed(ClientAllyPointOfSalesApiConstants.segmentCodeValueIdParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.segmentCodeValueIdParamName).value(segmentCodeValueId)
                .notNull();

        final Long typeCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyPointOfSalesApiConstants.typeCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.typeCodeValueIdParamName).value(typeCodeValueId).notNull();

        final BigDecimal settledComission = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(ClientAllyPointOfSalesApiConstants.settledComissionParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.settledComissionParamName).value(settledComission).notNull()
                .notGreaterThanMax(BigDecimal.valueOf(100));

        final Boolean buyEnabled = this.fromApiJsonHelper.extractBooleanNamed(ClientAllyPointOfSalesApiConstants.buyEnabledParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.buyEnabledParamName).value(buyEnabled).notNull();

        final Boolean collectionEnabled = this.fromApiJsonHelper
                .extractBooleanNamed(ClientAllyPointOfSalesApiConstants.collectionEnabledParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.collectionEnabledParamName).value(collectionEnabled)
                .notNull();

        final Long stateCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyPointOfSalesApiConstants.stateCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.stateCodeValueIdParamName).value(stateCodeValueId).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return ClientAllyPointOfSales.builder().code(code).name(name).brandCodeValueId(brand).cityCodeValueId(cityCodeValueId)
                .departmentCodeValueId(departmentCodeValueId).categoryCodeValueId(categoryCodeValueId)
                .segmentCodeValueId(segmentCodeValueId).typeCodeValueId(typeCodeValueId).settledComission(settledComission)
                .buyEnabled(buyEnabled).collectionEnabled(collectionEnabled).stateCodeValueId(stateCodeValueId).build();
    }

    public ClientAllyPointOfSales validateForUpdate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, ClientAllyPointOfSalesApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ClientAllyPointOfSalesApiConstants.RESOURCE_NAME);

        final String code = this.fromApiJsonHelper.extractStringNamed(ClientAllyPointOfSalesApiConstants.codeParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.codeParamName).value(code).notNull().notExceedingLengthOf(4);

        final String name = this.fromApiJsonHelper.extractStringNamed(ClientAllyPointOfSalesApiConstants.nameParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.nameParamName).value(name).notNull()
                .notExceedingLengthOf(100);

        final Long brand = this.fromApiJsonHelper.extractLongNamed(ClientAllyPointOfSalesApiConstants.brandParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.brandParamName).value(brand).notNull();

        final Long cityCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyPointOfSalesApiConstants.cityCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.cityCodeValueIdParamName).value(cityCodeValueId).notNull();

        final Long departmentCodeValueId = this.fromApiJsonHelper
                .extractLongNamed(ClientAllyPointOfSalesApiConstants.departmentCodeValueIdParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.departmentCodeValueIdParamName).value(departmentCodeValueId)
                .notNull();

        final Long categoryCodeValueId = this.fromApiJsonHelper
                .extractLongNamed(ClientAllyPointOfSalesApiConstants.categoryCodeValueIdParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.categoryCodeValueIdParamName).value(categoryCodeValueId)
                .notNull();

        final Long segmentCodeValueId = this.fromApiJsonHelper
                .extractLongNamed(ClientAllyPointOfSalesApiConstants.segmentCodeValueIdParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.segmentCodeValueIdParamName).value(segmentCodeValueId)
                .notNull();

        final Long typeCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyPointOfSalesApiConstants.typeCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.typeCodeValueIdParamName).value(typeCodeValueId).notNull();

        final BigDecimal settledComission = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(ClientAllyPointOfSalesApiConstants.settledComissionParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.settledComissionParamName).value(settledComission).notNull()
                .notGreaterThanMax(BigDecimal.valueOf(100));

        final Boolean buyEnabled = this.fromApiJsonHelper.extractBooleanNamed(ClientAllyPointOfSalesApiConstants.buyEnabledParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.buyEnabledParamName).value(buyEnabled).notNull();

        final Boolean collectionEnabled = this.fromApiJsonHelper
                .extractBooleanNamed(ClientAllyPointOfSalesApiConstants.collectionEnabledParamName, element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.collectionEnabledParamName).value(collectionEnabled)
                .notNull();

        final Long stateCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyPointOfSalesApiConstants.stateCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyPointOfSalesApiConstants.stateCodeValueIdParamName).value(stateCodeValueId).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return ClientAllyPointOfSales.builder().code(code).name(name).brandCodeValueId(brand).cityCodeValueId(cityCodeValueId)
                .departmentCodeValueId(departmentCodeValueId).categoryCodeValueId(categoryCodeValueId)
                .segmentCodeValueId(segmentCodeValueId).typeCodeValueId(typeCodeValueId).settledComission(settledComission)
                .buyEnabled(buyEnabled).collectionEnabled(collectionEnabled).stateCodeValueId(stateCodeValueId).build();
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
