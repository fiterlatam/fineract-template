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
package org.apache.fineract.infrastructure.clientblockingreasons.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.clientblockingreasons.api.BlockingReasonsConstants;
import org.apache.fineract.infrastructure.clientblockingreasons.data.BlockingReasonsDataValidator;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockLevel;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.ManageBlockingReasonSettingsRepositoryWrapper;
import org.apache.fineract.infrastructure.clientblockingreasons.exception.BlockLevelEntityException;
import org.apache.fineract.infrastructure.clientblockingreasons.exception.BlockingReasonExceptionNotFoundException;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ManageBlockingReasonsWritePlatformServiceJpaRepositoryImpl implements ManageBlockingReasonsWritePlatformService {

    private final ManageBlockingReasonSettingsRepositoryWrapper blockingReasonSettingsRepositoryWrapper;
    private final BlockingReasonsDataValidator blockingReasonsDataValidator;

    @Autowired
    ManageBlockingReasonsWritePlatformServiceJpaRepositoryImpl(
            final ManageBlockingReasonSettingsRepositoryWrapper blockingReasonSettingsRepositoryWrapper,
            final BlockingReasonsDataValidator blockingReasonsDataValidator, final CodeValueRepositoryWrapper codeValueRepository) {
        this.blockingReasonSettingsRepositoryWrapper = blockingReasonSettingsRepositoryWrapper;
        this.blockingReasonsDataValidator = blockingReasonsDataValidator;
    }

    @Override
    @Transactional
    public CommandProcessingResult createBlockReasonSetting(JsonCommand command) {
        this.blockingReasonsDataValidator.validateForCreate(command.json());

        final Integer priority = command.integerValueSansLocaleOfParameterNamed(BlockingReasonsConstants.PRIORITY_PARAM);
        final String nameOfReason = command.stringValueOfParameterNamed(BlockingReasonsConstants.NAME_OF_REASON_PARAM).trim();
        final String level = command.stringValueOfParameterNamed(BlockingReasonsConstants.LEVEL_PARAM);
        final String description = command.stringValueOfParameterNamed(BlockingReasonsConstants.DESCRIPTION_PARAM);

        BlockLevel blockLevel = BlockLevel.valueOf(level);
        if (blockLevel == null) {
            throw new BlockLevelEntityException(level);
        }
        List<BlockingReasonSetting> priorities = this.blockingReasonSettingsRepositoryWrapper.getBlockingReasonSettingByPriority(priority,
                blockLevel.name());
        if (!CollectionUtils.isEmpty(priorities)) {
            throw new BlockingReasonExceptionNotFoundException(priority, blockLevel.getCode());
        }

        List<BlockingReasonSetting> reasons = this.blockingReasonSettingsRepositoryWrapper.getBlockingReasonSettingByReason(nameOfReason,
                blockLevel.name());

        if (!CollectionUtils.isEmpty(reasons)) {

            throw new BlockingReasonExceptionNotFoundException(nameOfReason, blockLevel.getCode());

        }

        BlockingReasonSetting blockingReasonSetting = new BlockingReasonSetting();
        blockingReasonSetting.setPriority(priority);
        blockingReasonSetting.setNameOfReason(nameOfReason);
        blockingReasonSetting.setLevel(level);
        blockingReasonSetting.setDescription(description);

        this.blockingReasonSettingsRepositoryWrapper.saveAndFlush(blockingReasonSetting);

        return new CommandProcessingResultBuilder() //
                .withEntityId(blockingReasonSetting.getId()) //
                .build();
    }

    @Override
    public CommandProcessingResult updateBlockReasonSetting(Long id, JsonCommand command) {

        BlockingReasonSetting blockingReasonSetting = this.blockingReasonSettingsRepositoryWrapper.findOneWithNotFoundDetection(id);
        if (blockingReasonSetting == null) {
            throw new BlockingReasonExceptionNotFoundException(id);
        }

        this.blockingReasonsDataValidator.validateForUpdate(command.json());
        final String levelChange = command.stringValueOfParameterNamed(BlockingReasonsConstants.LEVEL_PARAM);

        validateUniqueReason(command, blockingReasonSetting, levelChange);
        validateUniquePriority(command, blockingReasonSetting, levelChange);

        final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

        if (command.isChangeInStringParameterNamed(BlockingReasonsConstants.NAME_OF_REASON_PARAM,
                blockingReasonSetting.getNameOfReason())) {
            final String newValue = command.stringValueOfParameterNamed(BlockingReasonsConstants.NAME_OF_REASON_PARAM).trim();
            actualChanges.put(BlockingReasonsConstants.NAME_OF_REASON_PARAM, newValue);
            blockingReasonSetting.setNameOfReason(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(BlockingReasonsConstants.DESCRIPTION_PARAM, blockingReasonSetting.getDescription())) {
            final String newValue = command.stringValueOfParameterNamed(BlockingReasonsConstants.DESCRIPTION_PARAM);
            actualChanges.put(BlockingReasonsConstants.DESCRIPTION_PARAM, newValue);
            blockingReasonSetting.setDescription(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(BlockingReasonsConstants.LEVEL_PARAM, blockingReasonSetting.getLevel())) {
            final String newValue = command.stringValueOfParameterNamed(BlockingReasonsConstants.LEVEL_PARAM);
            actualChanges.put(BlockingReasonsConstants.LEVEL_PARAM, newValue);
            blockingReasonSetting.setLevel(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInIntegerSansLocaleParameterNamed(BlockingReasonsConstants.PRIORITY_PARAM,
                blockingReasonSetting.getPriority())) {
            final Integer newValue = command.integerValueSansLocaleOfParameterNamed(BlockingReasonsConstants.PRIORITY_PARAM);
            actualChanges.put(BlockingReasonsConstants.PRIORITY_PARAM, newValue);
            blockingReasonSetting.setPriority(newValue);
        }

        if (!actualChanges.isEmpty()) {
            this.blockingReasonSettingsRepositoryWrapper.saveAndFlush(blockingReasonSetting);
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(id).with(actualChanges).build();
    }

    private void validateUniqueReason(JsonCommand command, BlockingReasonSetting blockingReasonSetting, String levelChange) {

        final String nameOfReasonChange = command.stringValueOfParameterNamed(BlockingReasonsConstants.NAME_OF_REASON_PARAM).trim();
        BlockLevel blockLevel = BlockLevel.valueOf(levelChange);
        if (blockLevel == null) {
            throw new BlockLevelEntityException(levelChange);
        }

        if (command.isChangeInStringParameterNamed(BlockingReasonsConstants.NAME_OF_REASON_PARAM,
                blockingReasonSetting.getNameOfReason())) {
            if (nameOfReasonChange != null) {
                List<BlockingReasonSetting> reasons = this.blockingReasonSettingsRepositoryWrapper
                        .getBlockingReasonSettingByReason(nameOfReasonChange, blockLevel.name());
                if (!CollectionUtils.isEmpty(reasons)) {
                    throw new BlockingReasonExceptionNotFoundException(nameOfReasonChange, blockLevel.getCode());
                }
            }
        }

        if (command.isChangeInStringParameterNamed(BlockingReasonsConstants.LEVEL_PARAM, blockingReasonSetting.getLevel())) {

            if (nameOfReasonChange != null) {
                List<BlockingReasonSetting> reasons = this.blockingReasonSettingsRepositoryWrapper
                        .getBlockingReasonSettingByReason(nameOfReasonChange, blockLevel.name());
                if (!CollectionUtils.isEmpty(reasons)) {
                    throw new BlockingReasonExceptionNotFoundException(nameOfReasonChange, blockLevel.getCode());
                }
            }
        }
    }

    private void validateUniquePriority(JsonCommand command, BlockingReasonSetting blockingReasonSetting, String levelChange) {
        final Integer priorityChange = command.integerValueSansLocaleOfParameterNamed(BlockingReasonsConstants.PRIORITY_PARAM);

        BlockLevel blockLevel = BlockLevel.valueOf(levelChange);
        if (blockLevel == null) {
            throw new BlockLevelEntityException(levelChange);
        }

        if (command.isChangeInIntegerSansLocaleParameterNamed(BlockingReasonsConstants.PRIORITY_PARAM,
                blockingReasonSetting.getPriority())) {
            if (priorityChange != null) {
                List<BlockingReasonSetting> reasons = this.blockingReasonSettingsRepositoryWrapper
                        .getBlockingReasonSettingByPriority(priorityChange, blockLevel.name());
                if (!CollectionUtils.isEmpty(reasons)) {
                    throw new BlockingReasonExceptionNotFoundException(priorityChange, blockLevel.getCode());
                }
            }
        }

        if (command.isChangeInStringParameterNamed(BlockingReasonsConstants.LEVEL_PARAM, blockingReasonSetting.getLevel())) {

            if (priorityChange != null) {
                List<BlockingReasonSetting> reasons = this.blockingReasonSettingsRepositoryWrapper
                        .getBlockingReasonSettingByPriority(priorityChange, blockLevel.name());
                if (!CollectionUtils.isEmpty(reasons)) {
                    throw new BlockingReasonExceptionNotFoundException(priorityChange, blockLevel.getCode());
                }
            }
        }
    }
}
