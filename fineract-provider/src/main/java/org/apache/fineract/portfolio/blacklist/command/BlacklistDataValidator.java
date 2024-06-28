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
package org.apache.fineract.portfolio.blacklist.command;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistClients;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistClientsRepository;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistStatus;
import org.apache.fineract.portfolio.blacklist.exception.ClientBlacklistedException;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.client.validation.ClientIdentifierDocumentValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BlacklistDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final BlacklistClientsRepository blacklistClientsRepository;

    @Autowired
    public BlacklistDataValidator(final FromJsonHelper fromApiJsonHelper, final BlacklistClientsRepository blacklistClientsRepository) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.blacklistClientsRepository = blacklistClientsRepository;
    }

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                BlacklistApiCollectionConstants.BLACKLIST_CREATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(BlacklistApiConstants.BLACKLIST_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final BigDecimal balance = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(BlacklistApiConstants.balanceParamName, element);
        baseDataValidator.reset().parameter(BlacklistApiConstants.balanceParamName).value(balance).notNull();

        final BigDecimal disbursementAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(BlacklistApiConstants.disbursementAmountParamName, element);
        baseDataValidator.reset().parameter(BlacklistApiConstants.disbursementAmountParamName).value(disbursementAmount).notNull();

        final String dpi = this.fromApiJsonHelper.extractStringNamed(BlacklistApiConstants.dpiParamName, element);
        baseDataValidator.reset().parameter(BlacklistApiConstants.dpiParamName).value(dpi).notBlank();
        ClientIdentifierDocumentValidator.checkDPI(dpi, ClientApiConstants.dpiParamName);

        BlacklistClients blacklist = this.blacklistClientsRepository.findBlacklistClientsByDpi(dpi, BlacklistStatus.ACTIVE.getValue());
        if (blacklist != null) {
            throw new ClientBlacklistedException(dpi);
        }
        final String description = this.fromApiJsonHelper.extractStringNamed(BlacklistApiConstants.descriptionParamName, element);
        baseDataValidator.reset().parameter(BlacklistApiConstants.descriptionParamName).value(description).notBlank();

        final String agencyId = this.fromApiJsonHelper.extractStringNamed(BlacklistApiConstants.agencyIdParamName, element);
        baseDataValidator.reset().parameter(BlacklistApiConstants.descriptionParamName).value(agencyId).notBlank();

        final Long productId = this.fromApiJsonHelper.extractLongNamed(BlacklistApiConstants.productIdParamName, element);
        baseDataValidator.reset().parameter(BlacklistApiConstants.productIdParamName).value(productId).notNull();

        final Long typification = this.fromApiJsonHelper.extractLongNamed(BlacklistApiConstants.typificationParamName, element);
        baseDataValidator.reset().parameter(BlacklistApiConstants.typificationParamName).value(typification).notNull();

        final Long year = this.fromApiJsonHelper.extractLongNamed(BlacklistApiConstants.yearParamName, element);
        baseDataValidator.reset().parameter(BlacklistApiConstants.yearParamName).value(year).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateUpdate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                BlacklistApiCollectionConstants.BLACKLIST_CREATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ClientApiConstants.CLIENT_CHARGES_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.amountParamName, element)) {
            final BigDecimal amount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(ClientApiConstants.amountParamName, element);
            baseDataValidator.reset().parameter(ClientApiConstants.amountParamName).value(amount).notNull().positiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.dueAsOfDateParamName, element)) {
            final LocalDate dueDate = this.fromApiJsonHelper.extractLocalDateNamed(ClientApiConstants.dueAsOfDateParamName, element);
            baseDataValidator.reset().parameter(ClientApiConstants.dueAsOfDateParamName).value(dueDate).notNull();
        }
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

}
