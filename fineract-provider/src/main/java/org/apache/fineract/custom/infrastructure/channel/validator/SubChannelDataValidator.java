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
package org.apache.fineract.custom.infrastructure.channel.validator;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.custom.infrastructure.channel.constants.SubChannelApiConstants;
import org.apache.fineract.custom.infrastructure.channel.domain.SubChannel;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubChannelDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final PlatformSecurityContext platformSecurityContext;

    @Autowired
    public SubChannelDataValidator(final FromJsonHelper fromApiJsonHelper, final PlatformSecurityContext platformSecurityContext) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.platformSecurityContext = platformSecurityContext;
    }

    public SubChannel validateForCreate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, SubChannelApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SubChannelApiConstants.RESOURCE_NAME);

        final Long id = this.fromApiJsonHelper.extractLongNamed(SubChannelApiConstants.idParamName, element);
        baseDataValidator.reset().parameter(SubChannelApiConstants.idParamName).value(id).notNull();

        final Long channelId = this.fromApiJsonHelper.extractLongNamed(SubChannelApiConstants.channelIdParamName, element);
        baseDataValidator.reset().parameter(SubChannelApiConstants.channelIdParamName).value(channelId).notNull();

        final String name = this.fromApiJsonHelper.extractStringNamed(SubChannelApiConstants.nameParamName, element);
        baseDataValidator.reset().parameter(SubChannelApiConstants.nameParamName).value(name).notNull().notExceedingLengthOf(100);

        final String description = this.fromApiJsonHelper.extractStringNamed(SubChannelApiConstants.descriptionParamName, element);
        baseDataValidator.reset().parameter(SubChannelApiConstants.descriptionParamName).value(description).notNull()
                .notExceedingLengthOf(1000);

        final Boolean active = this.fromApiJsonHelper.extractBooleanNamed(SubChannelApiConstants.activeParamName, element);
        baseDataValidator.reset().parameter(SubChannelApiConstants.activeParamName).value(active);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return SubChannel.builder().id(id) //
                .channelId(channelId) //
                .name(name) //
                .description(description) //
                .active(active) //

                .build();
    }

    public SubChannel validateForUpdate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, SubChannelApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SubChannelApiConstants.RESOURCE_NAME);

        final Long id = this.fromApiJsonHelper.extractLongNamed(SubChannelApiConstants.idParamName, element);
        baseDataValidator.reset().parameter(SubChannelApiConstants.idParamName).value(id).notNull();

        final Long channelId = this.fromApiJsonHelper.extractLongNamed(SubChannelApiConstants.channelIdParamName, element);
        baseDataValidator.reset().parameter(SubChannelApiConstants.channelIdParamName).value(channelId).notNull();

        final String name = this.fromApiJsonHelper.extractStringNamed(SubChannelApiConstants.nameParamName, element);
        baseDataValidator.reset().parameter(SubChannelApiConstants.nameParamName).value(name).notNull().notExceedingLengthOf(100);

        final String description = this.fromApiJsonHelper.extractStringNamed(SubChannelApiConstants.descriptionParamName, element);
        baseDataValidator.reset().parameter(SubChannelApiConstants.descriptionParamName).value(description).notNull()
                .notExceedingLengthOf(1000);

        final Boolean active = this.fromApiJsonHelper.extractBooleanNamed(SubChannelApiConstants.activeParamName, element);
        baseDataValidator.reset().parameter(SubChannelApiConstants.activeParamName).value(active);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return SubChannel.builder().id(id) //
                .channelId(channelId) //
                .name(name) //
                .description(description) //
                .active(active) //

                .build();
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
