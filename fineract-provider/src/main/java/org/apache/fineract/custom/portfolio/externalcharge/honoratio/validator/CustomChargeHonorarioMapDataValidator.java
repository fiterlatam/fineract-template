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
package org.apache.fineract.custom.portfolio.externalcharge.honoratio.validator;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.constants.CustomChargeHonorarioMapApiConstants;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.domain.CustomChargeHonorarioMap;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.service.external.data.ExternalCustomChargeHonorarioMapData;
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
public class CustomChargeHonorarioMapDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final PlatformSecurityContext context;

    @Autowired
    public CustomChargeHonorarioMapDataValidator(final FromJsonHelper fromApiJsonHelper,
            final PlatformSecurityContext platformSecurityContext) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.context = platformSecurityContext;
    }

    public CustomChargeHonorarioMap validateForCreate(final ExternalCustomChargeHonorarioMapData dto) {

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CustomChargeHonorarioMapApiConstants.RESOURCE_NAME);

        final String nit = dto.getNit();
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.nitParamName).value(nit).notNull()
                .notExceedingLengthOf(20);

        final Long loanId = dto.getLoanId();
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.loanIdParamName).value(loanId).notNull();

        final Integer loanInstallmentNr = dto.getLoanInstallmentNr();
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.loanInstallmentNrParamName).value(loanInstallmentNr)
                .notNull();

        final BigDecimal feeTotalAmount = dto.getFeeTotalAmount();
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.feeTotalAmountParamName).value(feeTotalAmount).notNull();

        final BigDecimal feeBaseAmount = dto.getFeeBaseAmount();
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.feeBaseAmountParamName).value(feeBaseAmount).notNull();

        final BigDecimal feeVatAmount = dto.getFeeVatAmount();
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.feeVatAmountParamName).value(feeVatAmount).notNull();

        final Long createdBy = context.authenticatedUser().getId();

        final LocalDateTime createdAt = DateUtils.getLocalDateTimeOfTenant();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return CustomChargeHonorarioMap.builder() //
                .loanId(loanId) //
                .loanInstallmentNr(loanInstallmentNr) //
                .feeTotalAmount(feeTotalAmount) //
                .feeBaseAmount(feeBaseAmount) //
                .feeVatAmount(feeVatAmount) //
                .nit(nit) //
                .createdBy(createdBy) //
                .createdAt(createdAt) //
                .build();
    }

    public CustomChargeHonorarioMap validateForCreate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CustomChargeHonorarioMapApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CustomChargeHonorarioMapApiConstants.RESOURCE_NAME);

        final Long loanId = this.fromApiJsonHelper.extractLongNamed(CustomChargeHonorarioMapApiConstants.loanIdParamName, element);
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.loanIdParamName).value(loanId).notNull();

        final Integer loanInstallmentNr = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(CustomChargeHonorarioMapApiConstants.loanInstallmentNrParamName, element);
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.loanInstallmentNrParamName).value(loanInstallmentNr)
                .notNull();

        final BigDecimal feeTotalAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(CustomChargeHonorarioMapApiConstants.feeTotalAmountParamName, element);
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.feeTotalAmountParamName).value(feeTotalAmount).notNull();

        final BigDecimal feeBaseAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(CustomChargeHonorarioMapApiConstants.feeBaseAmountParamName, element);
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.feeBaseAmountParamName).value(feeBaseAmount).notNull();

        final BigDecimal feeVatAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(CustomChargeHonorarioMapApiConstants.feeVatAmountParamName, element);
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.feeVatAmountParamName).value(feeVatAmount).notNull();

        final String nit = this.fromApiJsonHelper.extractStringNamed(CustomChargeHonorarioMapApiConstants.nitParamName, element);
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.nitParamName).value(nit).notNull()
                .notExceedingLengthOf(20);

        final Long createdBy = context.authenticatedUser().getId();

        final LocalDateTime createdAt = DateUtils.getLocalDateTimeOfTenant();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return CustomChargeHonorarioMap.builder() //
                .loanId(loanId) //
                .loanInstallmentNr(loanInstallmentNr) //
                .feeTotalAmount(feeTotalAmount) //
                .feeBaseAmount(feeBaseAmount) //
                .feeVatAmount(feeVatAmount) //
                .nit(nit) //
                .createdBy(createdBy) //
                .createdAt(createdAt) //
                .build();
    }

    public CustomChargeHonorarioMap validateForUpdate(final String json, Long id) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CustomChargeHonorarioMapApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CustomChargeHonorarioMapApiConstants.RESOURCE_NAME);

        final Long loanId = this.fromApiJsonHelper.extractLongNamed(CustomChargeHonorarioMapApiConstants.loanIdParamName, element);
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.loanIdParamName).value(loanId).notNull();

        final Integer loanInstallmentNr = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(CustomChargeHonorarioMapApiConstants.loanInstallmentNrParamName, element);
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.loanInstallmentNrParamName).value(loanInstallmentNr)
                .notNull();

        final BigDecimal feeTotalAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(CustomChargeHonorarioMapApiConstants.feeTotalAmountParamName, element);
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.feeTotalAmountParamName).value(feeTotalAmount).notNull();

        final BigDecimal feeBaseAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(CustomChargeHonorarioMapApiConstants.feeBaseAmountParamName, element);
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.feeBaseAmountParamName).value(feeBaseAmount).notNull();

        final BigDecimal feeVatAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(CustomChargeHonorarioMapApiConstants.feeVatAmountParamName, element);
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.feeVatAmountParamName).value(feeVatAmount).notNull();

        final String nit = this.fromApiJsonHelper.extractStringNamed(CustomChargeHonorarioMapApiConstants.nitParamName, element);
        baseDataValidator.reset().parameter(CustomChargeHonorarioMapApiConstants.nitParamName).value(nit).notNull()
                .notExceedingLengthOf(20);

        final Long updatedBy = context.authenticatedUser().getId();
        ;

        final LocalDateTime updatedAt = DateUtils.getLocalDateTimeOfTenant();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
        CustomChargeHonorarioMap map = CustomChargeHonorarioMap.builder().loanId(loanId) //
                .loanInstallmentNr(loanInstallmentNr) //
                .feeTotalAmount(feeTotalAmount) //
                .feeBaseAmount(feeBaseAmount) //
                .feeVatAmount(feeVatAmount) //
                .nit(nit) //
                .updatedBy(updatedBy) //
                .updatedAt(updatedAt) //
                .build();
        map.setId(id);
        return map;
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
