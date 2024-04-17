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
package org.apache.fineract.infrastructure.clientBlockingSettings.service;

import org.apache.fineract.infrastructure.clientBlockingSettings.data.BlockingReasonsDataValidator;
import org.apache.fineract.infrastructure.clientBlockingSettings.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.clientBlockingSettings.domain.ManageBlockingReasonSettingsRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageBlockingReasonsWritePlatformServiceJpaRepositoryImpl implements ManageBlockingReasonsWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(ManageBlockingReasonsWritePlatformServiceJpaRepositoryImpl.class);
    private final ManageBlockingReasonSettingsRepositoryWrapper blockingReasonSettingsRepositoryWrapper;
    private final BlockingReasonsDataValidator blockingReasonsDataValidator;

    @Autowired
    ManageBlockingReasonsWritePlatformServiceJpaRepositoryImpl(
            final ManageBlockingReasonSettingsRepositoryWrapper blockingReasonSettingsRepositoryWrapper,
            final BlockingReasonsDataValidator blockingReasonsDataValidator) {
        this.blockingReasonSettingsRepositoryWrapper = blockingReasonSettingsRepositoryWrapper;
        this.blockingReasonsDataValidator = blockingReasonsDataValidator;
    }

    @Override
    @Transactional
    public CommandProcessingResult createBlockReasonSetting(JsonCommand command) {
        this.blockingReasonsDataValidator.validateForCreate(command.json());

        final Integer priority = command.integerValueSansLocaleOfParameterNamed(BlockingReasonsConstants.PRIORITY_PARAM);
        final String nameOfReason = command.stringValueOfParameterNamed(BlockingReasonsConstants.NAME_OF_REASON_PARAM);
        final String level = command.stringValueOfParameterNamed(BlockingReasonsConstants.LEVEL_PARAM);
        final String description = command.stringValueOfParameterNamed(BlockingReasonsConstants.DESCRIPTION_PARAM);
        final Integer customerLevel = command.integerValueSansLocaleOfParameterNamed(BlockingReasonsConstants.CUSTOMER_LEVEL_PARAM);
        final Integer creditLevel = command.integerValueSansLocaleOfParameterNamed(BlockingReasonsConstants.CREDIT_LEVEL_PARAM);

        BlockingReasonSetting blockingReasonSetting = new BlockingReasonSetting();
        blockingReasonSetting.setPriority(priority);
        blockingReasonSetting.setNameOfReason(nameOfReason);
        blockingReasonSetting.setLevel(level);
        blockingReasonSetting.setDescription(description);
        blockingReasonSetting.setCustomerLevel(customerLevel);
        blockingReasonSetting.setCreditLevel(creditLevel);

        this.blockingReasonSettingsRepositoryWrapper.saveAndFlush(blockingReasonSetting);

        return new CommandProcessingResultBuilder() //
                .withEntityId(blockingReasonSetting.getId()) //
                .build();
    }
}
