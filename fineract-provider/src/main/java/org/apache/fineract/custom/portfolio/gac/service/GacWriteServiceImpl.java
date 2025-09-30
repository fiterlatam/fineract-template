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
package org.apache.fineract.custom.portfolio.gac.service;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.blockaccounts.api.LoanAccountBlockConstants;
import org.apache.fineract.custom.portfolio.gac.api.GacConstants;
import org.apache.fineract.custom.portfolio.gac.domain.Gac;
import org.apache.fineract.custom.portfolio.gac.domain.GacRepository;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingsRepository;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.delinquency.api.DelinquencyApiConstants;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class GacWriteServiceImpl {

    private final FromJsonHelper fromApiJsonHelper;
    private final GacRepository gacRepository;
    private final BlockingReasonSettingsRepository blockingReasonSettingsRepository;

    public CommandProcessingResult addGac(JsonCommand command) {
        validateGacRequestBody(command);
        validateNoOverlappingGac(command, null);
        Gac gac = createGac(fromApiJsonHelper.parse(command.json()));

        gac = gacRepository.saveAndFlush(gac);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(gac.getId()).build();
    }

    public Gac createGac(JsonElement json) {
        Gac gac = new Gac();
        final JsonObject topLevelJsonElement = json.getAsJsonObject();
        final Locale locale = fromApiJsonHelper.extractLocaleParameter(topLevelJsonElement);

        final String classification = fromApiJsonHelper.extractStringNamed(GacConstants.CLASSIFICATION_PARAM_NAME, json);
        gac.setClassification(classification);
        final Integer minimumAge = fromApiJsonHelper.extractIntegerNamed(GacConstants.MINIMUMAGEDAYS_PARAM_NAME, json, locale);
        gac.setMinimumAgeDays(minimumAge);
        final Integer maximumAge = fromApiJsonHelper.extractIntegerNamed(GacConstants.MAXIMUMAGEDAYS_PARAM_NAME, json, locale);
        gac.setMaximumAgeDays(maximumAge);

        final Long blockingReasonId = fromApiJsonHelper.extractLongNamed(LoanAccountBlockConstants.blockingReasonIdParamName, json);
        if (blockingReasonId != null) {
            BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepository.getReferenceById(blockingReasonId);
            gac.setBlockingReasonSetting(blockingReasonSetting);
        }

        final Integer percentageValue = fromApiJsonHelper.extractIntegerNamed(GacConstants.PERCENTAGE_PARAM_NAME, json, locale);
        gac.setPercentageValue(percentageValue);

        return gac;
    }

    public void validateGacRequestBody(final JsonCommand command) {
        final String apiRequestBodyAsJson = command.json();
        final Set<String> requestParameters = new HashSet<>(Arrays.asList(GacConstants.MAXIMUMAGEDAYS_PARAM_NAME,
                GacConstants.PERCENTAGE_PARAM_NAME, GacConstants.MINIMUMAGEDAYS_PARAM_NAME, GacConstants.BLOCKING_REASON_ID_PARAM_NAME,
                GacConstants.LOCALE_PARAM_NAME));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {

        }.getType();

        final Locale locale = fromApiJsonHelper.extractLocaleParameter(fromApiJsonHelper.parse(command.json()).getAsJsonObject());

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("gac");
        final JsonElement json = fromApiJsonHelper.parse(apiRequestBodyAsJson);

        final String classification = fromApiJsonHelper.extractStringNamed(GacConstants.CLASSIFICATION_PARAM_NAME, json);
        baseDataValidator.reset().parameter(GacConstants.CLASSIFICATION_PARAM_NAME).value(classification).notBlank()
                .notExceedingLengthOf(100);

        final Integer maximumAgeDays = fromApiJsonHelper.extractIntegerNamed(GacConstants.MAXIMUMAGEDAYS_PARAM_NAME, json, locale);
        baseDataValidator.reset().parameter(DelinquencyApiConstants.MINIMUMAGEDAYS_PARAM_NAME).value(maximumAgeDays).notBlank()
                .integerGreaterThanNumber(0);

        final Integer minimumAgeDays = fromApiJsonHelper.extractIntegerNamed(GacConstants.MINIMUMAGEDAYS_PARAM_NAME, json, locale);
        baseDataValidator.reset().parameter(DelinquencyApiConstants.MINIMUMAGEDAYS_PARAM_NAME).value(minimumAgeDays).notBlank()
                .integerGreaterThanNumber(0);

        if (minimumAgeDays != null && maximumAgeDays != null && maximumAgeDays <= minimumAgeDays) {
            baseDataValidator.reset().parameter(GacConstants.MAXIMUMAGEDAYS_PARAM_NAME).value(maximumAgeDays)
                    .failWithCode("must.be.greater.than.minimum", "Maximum age days must be greater than minimum age days");
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }

    /**
     * Valida que no existan franjas GAC que se superpongan en rango de días y condición de bloqueo
     */
    private void validateNoOverlappingGac(final JsonCommand command, final Long existingGacId) {
        final JsonElement json = fromApiJsonHelper.parse(command.json());
        final Locale locale = fromApiJsonHelper.extractLocaleParameter(json.getAsJsonObject());

        final Integer minimumAgeDays = fromApiJsonHelper.extractIntegerNamed(GacConstants.MINIMUMAGEDAYS_PARAM_NAME, json, locale);
        final Integer maximumAgeDays = fromApiJsonHelper.extractIntegerNamed(GacConstants.MAXIMUMAGEDAYS_PARAM_NAME, json, locale);
        final Long blockingReasonId = fromApiJsonHelper.extractLongNamed(GacConstants.BLOCKING_REASON_ID_PARAM_NAME, json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("gac");

        // Validar que los rangos no sean nulos
        if (minimumAgeDays == null || maximumAgeDays == null) {
            return; // La validación básica ya manejará estos casos
        }

        // Obtener todas las franjas GAC existentes
        List<Gac> allGacs = gacRepository.findAll();

        // Buscar franjas que se superpongan
        boolean hasOverlap = allGacs.stream()
                .anyMatch(existingGac -> isOverlappingGac(existingGac, minimumAgeDays, maximumAgeDays, blockingReasonId, existingGacId));

        if (hasOverlap) {
            baseDataValidator.reset().parameter(GacConstants.MINIMUMAGEDAYS_PARAM_NAME).failWithCode("overlapping.gac.range",
                    "Las condiciones de esta franja entran en conflicto con una franja existente");
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }

    /**
     * Verifica si una franja GAC existente se superpone con la nueva franja
     */
    private boolean isOverlappingGac(Gac existingGac, Integer newMinAge, Integer newMaxAge, Long newBlockingReasonId, Long excludeGacId) {
        // Excluir la franja actual en caso de edición
        if (excludeGacId != null && existingGac.getId().equals(excludeGacId)) {
            return false;
        }

        // Verificar si las condiciones de bloqueo coinciden (ambas nulas o mismo ID)
        boolean blockingReasonMatches = (newBlockingReasonId == null && existingGac.getBlockingReasonSetting() == null)
                || (newBlockingReasonId != null && existingGac.getBlockingReasonSetting() != null
                        && newBlockingReasonId.equals(existingGac.getBlockingReasonSetting().getId()));

        if (!blockingReasonMatches) {
            return false; // No hay superposición si las condiciones de bloqueo son diferentes
        }

        // Verificar superposición de rangos
        return isRangeOverlapping(newMinAge, newMaxAge, existingGac.getMinimumAgeDays(), existingGac.getMaximumAgeDays());
    }

    /**
     * Verifica si dos rangos numéricos se superponen
     */
    private boolean isRangeOverlapping(Integer min1, Integer max1, Integer min2, Integer max2) {
        return (min1 <= max2) && (max1 >= min2);
    }

    public CommandProcessingResult updateGac(final Long gacId, JsonCommand command) {
        final JsonElement json = fromApiJsonHelper.parse(command.json());
        final JsonObject topLevelJsonElement = json.getAsJsonObject();
        validateGacRequestBody(command);
        validateNoOverlappingGac(command, gacId);
        final Locale locale = fromApiJsonHelper.extractLocaleParameter(topLevelJsonElement);

        final Optional<Gac> optGac = gacRepository.findById(gacId);

        if (!optGac.isPresent()) {
            throw new NotFoundException(gacId.toString());
        }

        Gac gac = optGac.get();

        final Long blockingReasonId = this.fromApiJsonHelper.extractLongNamed(GacConstants.BLOCKING_REASON_ID_PARAM_NAME, json);

        if (blockingReasonId == null) {
            if (gac.getBlockingReasonSetting() != null) {
                gac.setBlockingReasonSetting(null);
            }
        } else {
            Optional<BlockingReasonSetting> blockingReasonSettingOpt = blockingReasonSettingsRepository.findById(blockingReasonId);
            if (blockingReasonSettingOpt.isPresent()) {
                BlockingReasonSetting blockingReasonSetting = blockingReasonSettingOpt.get();
                if (!blockingReasonSetting.equals(gac.getBlockingReasonSetting())) {
                    gac.setBlockingReasonSetting(blockingReasonSetting);
                }
            }
        }

        final String classification = fromApiJsonHelper.extractStringNamed(GacConstants.CLASSIFICATION_PARAM_NAME, json);
        if (!gac.getClassification().equals(classification)) {
            gac.setClassification(classification);
        }
        final Integer minimumAge = fromApiJsonHelper.extractIntegerNamed(GacConstants.MINIMUMAGEDAYS_PARAM_NAME, json, locale);
        if (!gac.getMinimumAgeDays().equals(minimumAge)) {
            gac.setMinimumAgeDays(minimumAge);
        }
        final Integer maximumAge = fromApiJsonHelper.extractIntegerNamed(GacConstants.MAXIMUMAGEDAYS_PARAM_NAME, json, locale);
        if (!gac.getMaximumAgeDays().equals(maximumAge)) {
            gac.setMaximumAgeDays(maximumAge);
        }
        final Integer percentageValue = fromApiJsonHelper.extractIntegerNamed(GacConstants.PERCENTAGE_PARAM_NAME, json, locale);
        if (!gac.getPercentageValue().equals(percentageValue)) {
            gac.setPercentageValue(percentageValue);
        }

        gacRepository.save(gac);

        return new CommandProcessingResultBuilder().withEntityId(gacId).build();
    }

    public CommandProcessingResult deleteGac(final Long gacId, final JsonCommand command) {
        this.gacRepository.deleteById(gacId);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(gacId).build();
    }

}
