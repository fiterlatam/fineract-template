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
package org.apache.fineract.portfolio.loanproductparameterization.data;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LoanProductParameterDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final ConfigurationReadPlatformService configurationReadPlatformService;
    private static final String PRODUCT_TYPE = "productType";
    private static final String BILLING_PREFIX = "billingPrefix";
    private static final String BILLING_RESOLUTION_NUMBER = "billingResolutionNumber";

    public void validateForCreate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource("loanproductparameterization");

        // check tha product type exists
        String productType = this.fromApiJsonHelper.extractStringNamed(PRODUCT_TYPE, element);
        baseDataValidator.reset().parameter(PRODUCT_TYPE).value(productType).notBlank();

        // validate that billing prefix is not more than 6 characters
        if (this.fromApiJsonHelper.parameterExists(BILLING_PREFIX, element)) {
            final String billingPrefix = this.fromApiJsonHelper.extractStringNamed(BILLING_PREFIX, element);
            baseDataValidator.reset().parameter(BILLING_PREFIX).value(billingPrefix).notNull();
            if (StringUtils.isNotBlank(billingPrefix)) {
                baseDataValidator.reset().parameter(BILLING_PREFIX).value(billingPrefix).notExceedingLengthOf(6);
            }
        }

        // validate that billing resolution number is not more than 50 characters
        if (this.fromApiJsonHelper.parameterExists(BILLING_RESOLUTION_NUMBER, element)) {
            final Long billingResolutionNumber = this.fromApiJsonHelper.extractLongNamed(BILLING_RESOLUTION_NUMBER, element);
            baseDataValidator.reset().parameter(BILLING_RESOLUTION_NUMBER).value(billingResolutionNumber).notNull();
            if (billingResolutionNumber != null) {
                baseDataValidator.reset().parameter(BILLING_RESOLUTION_NUMBER).value(billingResolutionNumber).notExceedingLengthOf(50);
            }
        }
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    public void validateForUpdate(final String json) {
        validateForCreate(json);
    }
}
