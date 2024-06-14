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
import org.apache.fineract.custom.infrastructure.channel.constants.ChannelApiConstants;
import org.apache.fineract.custom.insfrastructure.channel.domain.Channel;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChannelDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final PlatformSecurityContext platformSecurityContext;

    @Autowired
    public ChannelDataValidator(final FromJsonHelper fromApiJsonHelper, final PlatformSecurityContext platformSecurityContext) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.platformSecurityContext = platformSecurityContext;
    }

    public Channel validateForCreate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, ChannelApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ChannelApiConstants.RESOURCE_NAME);

        final String hash = this.fromApiJsonHelper.extractStringNamed(ChannelApiConstants.hashParamName, element);
        baseDataValidator.reset().parameter(ChannelApiConstants.hashParamName).value(hash).notNull().notExceedingLengthOf(5000);

        final String name = this.fromApiJsonHelper.extractStringNamed(ChannelApiConstants.nameParamName, element);
        baseDataValidator.reset().parameter(ChannelApiConstants.nameParamName).value(name).notNull().notExceedingLengthOf(100);

        final Integer channelType = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(ChannelApiConstants.CHANNEL_TYPE, element);
        baseDataValidator.reset().parameter(ChannelApiConstants.CHANNEL_TYPE).value(channelType).notNull().inMinMaxRange(1, 2);

        final String description = this.fromApiJsonHelper.extractStringNamed(ChannelApiConstants.descriptionParamName, element);
        baseDataValidator.reset().parameter(ChannelApiConstants.descriptionParamName).value(description).notExceedingLengthOf(1000);

        final Boolean active = this.fromApiJsonHelper.extractBooleanNamed(ChannelApiConstants.activeParamName, element);
        baseDataValidator.reset().parameter(ChannelApiConstants.activeParamName).value(active).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return Channel.builder().hash(hash) //
                .name(name) //
                .channelType(channelType).description(description) //
                .active(active) //
                .build();
    }

    public Channel validateForUpdate(final String json, Long channelId) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, ChannelApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ChannelApiConstants.RESOURCE_NAME);

        final String hash = this.fromApiJsonHelper.extractStringNamed(ChannelApiConstants.hashParamName, element);
        baseDataValidator.reset().parameter(ChannelApiConstants.hashParamName).value(hash).notNull().notExceedingLengthOf(5000);

        final String name = this.fromApiJsonHelper.extractStringNamed(ChannelApiConstants.nameParamName, element);
        baseDataValidator.reset().parameter(ChannelApiConstants.nameParamName).value(name).notNull().notExceedingLengthOf(100);

        final Integer channelType = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(ChannelApiConstants.CHANNEL_TYPE, element);
        baseDataValidator.reset().parameter(ChannelApiConstants.CHANNEL_TYPE).value(channelType).notNull().inMinMaxRange(1, 2);

        final String description = this.fromApiJsonHelper.extractStringNamed(ChannelApiConstants.descriptionParamName, element);
        baseDataValidator.reset().parameter(ChannelApiConstants.descriptionParamName).value(description).notExceedingLengthOf(1000);

        final Boolean active = this.fromApiJsonHelper.extractBooleanNamed(ChannelApiConstants.activeParamName, element);
        baseDataValidator.reset().parameter(ChannelApiConstants.activeParamName).value(active).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return Channel.builder() //
                .id(channelId) //
                .hash(hash) //
                .name(name) //
                .channelType(channelType).description(description) //
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
