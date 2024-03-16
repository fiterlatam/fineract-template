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
package org.apache.fineract.custom.ally.validator;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.custom.ally.api.ClientAllyApiConstants;
import org.apache.fineract.custom.ally.domain.ClientAlly;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClientAllyDataValidator {

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public ClientAllyDataValidator(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public ClientAlly validateForCreate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, ClientAllyApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ClientAllyApiConstants.RESOURCE_NAME);

        final String companyName = this.fromApiJsonHelper.extractStringNamed(ClientAllyApiConstants.companyNameParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.companyNameParamName).value(companyName).notNull()
                .notExceedingLengthOf(100);

        final String nit = this.fromApiJsonHelper.extractStringNamed(ClientAllyApiConstants.nitParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.nitParamName).value(nit).notNull().notExceedingLengthOf(20);

        final Integer nitDigit = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(ClientAllyApiConstants.nitDigitParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.nitDigitParamName).value(nitDigit).notNull();

        final String address = this.fromApiJsonHelper.extractStringNamed(ClientAllyApiConstants.addressParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.addressParamName).value(address).notNull().notExceedingLengthOf(100);

        final Long cityCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.cityCodeValueIdParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.cityCodeValueIdParamName).value(cityCodeValueId).notNull();

        final Long departmentCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.departmentCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.departmentCodeValueIdParamName).value(departmentCodeValueId).notNull();

        final Long liquidationFrequencyCodeValueId = this.fromApiJsonHelper
                .extractLongNamed(ClientAllyApiConstants.liquidationFrequencyCodeValueIdParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.liquidationFrequencyCodeValueIdParamName)
                .value(liquidationFrequencyCodeValueId).notNull();

        final Boolean applyCupoMaxSell = this.fromApiJsonHelper.extractBooleanNamed(ClientAllyApiConstants.applyCupoMaxSellParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.applyCupoMaxSellParamName).value(applyCupoMaxSell).notNull();

        final Integer cupoMaxSell = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(ClientAllyApiConstants.cupoMaxSellParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.cupoMaxSellParamName).value(cupoMaxSell);

        final BigDecimal settledComission = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(ClientAllyApiConstants.settledComissionParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.settledComissionParamName).value(settledComission);

        final Boolean buyEnabled = this.fromApiJsonHelper.extractBooleanNamed(ClientAllyApiConstants.buyEnabledParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.buyEnabledParamName).value(buyEnabled).notNull();

        final Boolean collectionEnabled = this.fromApiJsonHelper.extractBooleanNamed(ClientAllyApiConstants.collectionEnabledParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.collectionEnabledParamName).value(collectionEnabled).notNull();

        final Long bankEntityCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.bankEntityCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.bankEntityCodeValueIdParamName).value(bankEntityCodeValueId);

        final Long accountTypeCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.accountTypeCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.accountTypeCodeValueIdParamName).value(accountTypeCodeValueId);

        final Long accountNumber = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.accountNumberParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.accountNumberParamName).value(accountNumber);

        final Long taxProfileCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.taxProfileCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.taxProfileCodeValueIdParamName).value(taxProfileCodeValueId).notNull();

        final Long stateCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.stateCodeValueIdParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.stateCodeValueIdParamName).value(stateCodeValueId).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return ClientAlly.builder().companyName(companyName).nit(nit).nitDigit(nitDigit).address(address).cityCodeValueId(cityCodeValueId)
                .departmentCodeValueId(departmentCodeValueId).liquidationFrequencyCodeValueId(liquidationFrequencyCodeValueId)
                .applyCupoMaxSell(applyCupoMaxSell).cupoMaxSell(cupoMaxSell).settledComission(settledComission).buyEnabled(buyEnabled)
                .collectionEnabled(collectionEnabled).bankEntityCodeValueId(bankEntityCodeValueId)
                .accountTypeCodeValueId(accountTypeCodeValueId).accountNumber(accountNumber).taxProfileCodeValueId(taxProfileCodeValueId)
                .stateCodeValueId(stateCodeValueId).build();

    }

    public ClientAlly validateForUpdate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, ClientAllyApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ClientAllyApiConstants.RESOURCE_NAME);

        final String companyName = this.fromApiJsonHelper.extractStringNamed(ClientAllyApiConstants.companyNameParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.companyNameParamName).value(companyName).notNull()
                .notExceedingLengthOf(100);

        final String nit = this.fromApiJsonHelper.extractStringNamed(ClientAllyApiConstants.nitParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.nitParamName).value(nit).notNull().notExceedingLengthOf(20);

        final Integer nitDigit = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(ClientAllyApiConstants.nitDigitParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.nitDigitParamName).value(nitDigit).notNull();

        final String address = this.fromApiJsonHelper.extractStringNamed(ClientAllyApiConstants.addressParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.addressParamName).value(address).notNull().notExceedingLengthOf(100);

        final Long cityCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.cityCodeValueIdParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.cityCodeValueIdParamName).value(cityCodeValueId).notNull();

        final Long departmentCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.departmentCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.departmentCodeValueIdParamName).value(departmentCodeValueId).notNull();

        final Long liquidationFrequencyCodeValueId = this.fromApiJsonHelper
                .extractLongNamed(ClientAllyApiConstants.liquidationFrequencyCodeValueIdParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.liquidationFrequencyCodeValueIdParamName)
                .value(liquidationFrequencyCodeValueId).notNull();

        final Boolean applyCupoMaxSell = this.fromApiJsonHelper.extractBooleanNamed(ClientAllyApiConstants.applyCupoMaxSellParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.applyCupoMaxSellParamName).value(applyCupoMaxSell).notNull();

        final Integer cupoMaxSell = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(ClientAllyApiConstants.cupoMaxSellParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.cupoMaxSellParamName).value(cupoMaxSell);

        final BigDecimal settledComission = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(ClientAllyApiConstants.settledComissionParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.settledComissionParamName).value(settledComission);

        final Boolean buyEnabled = this.fromApiJsonHelper.extractBooleanNamed(ClientAllyApiConstants.buyEnabledParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.buyEnabledParamName).value(buyEnabled).notNull();

        final Boolean collectionEnabled = this.fromApiJsonHelper.extractBooleanNamed(ClientAllyApiConstants.collectionEnabledParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.collectionEnabledParamName).value(collectionEnabled).notNull();

        final Long bankEntityCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.bankEntityCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.bankEntityCodeValueIdParamName).value(bankEntityCodeValueId);

        final Long accountTypeCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.accountTypeCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.accountTypeCodeValueIdParamName).value(accountTypeCodeValueId);

        final Long accountNumber = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.accountNumberParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.accountNumberParamName).value(accountNumber);

        final Long taxProfileCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.taxProfileCodeValueIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.taxProfileCodeValueIdParamName).value(taxProfileCodeValueId).notNull();

        final Long stateCodeValueId = this.fromApiJsonHelper.extractLongNamed(ClientAllyApiConstants.stateCodeValueIdParamName, element);
        baseDataValidator.reset().parameter(ClientAllyApiConstants.stateCodeValueIdParamName).value(stateCodeValueId).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        ClientAlly ret = ClientAlly.builder().companyName(companyName).nit(nit).nitDigit(nitDigit).address(address)
                .cityCodeValueId(cityCodeValueId).departmentCodeValueId(departmentCodeValueId)
                .liquidationFrequencyCodeValueId(liquidationFrequencyCodeValueId).applyCupoMaxSell(applyCupoMaxSell)
                .cupoMaxSell(cupoMaxSell).settledComission(settledComission).buyEnabled(buyEnabled).collectionEnabled(collectionEnabled)
                .bankEntityCodeValueId(bankEntityCodeValueId).accountTypeCodeValueId(accountTypeCodeValueId).accountNumber(accountNumber)
                .taxProfileCodeValueId(taxProfileCodeValueId).stateCodeValueId(stateCodeValueId).build();

        return ret;
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
