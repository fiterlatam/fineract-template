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
package org.apache.fineract.portfolio.loanapplicationdraft.service.impl;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.loanapplicationdraft.api.LoanApplicationDraftConstants;
import org.apache.fineract.portfolio.loanapplicationdraft.domain.LoanApplicationDraft;
import org.apache.fineract.portfolio.loanapplicationdraft.domain.LoanApplicationDraftRepository;
import org.apache.fineract.portfolio.loanapplicationdraft.domain.LoanApplicationDraftStatus;
import org.apache.fineract.portfolio.loanapplicationdraft.exception.LoanApplicationDraftNotFoundException;
import org.apache.fineract.portfolio.loanapplicationdraft.service.LoanApplicationDraftWritePlatformService;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class LoanApplicationDraftWritePlatformServiceImpl implements LoanApplicationDraftWritePlatformService {

    private final FromJsonHelper fromApiJsonHelper;
    private final LoanApplicationDraftRepository loanApplicationDraftRepository;
    private final ClientRepositoryWrapper clientRepository;
    private final LoanProductRepository loanProductRepository;

    @Override
    public CommandProcessingResult createLoanApplicationDraft(JsonCommand command) {
        this.validateForCreate(command.json());

        final Long clientId = command.longValueOfParameterNamed(LoanApplicationDraftConstants.clientIdParameterName);
        final Long loanProductId = command.longValueOfParameterNamed(LoanApplicationDraftConstants.loanProductIdParameterName);
        final String currentStep = command.stringValueOfParameterNamed(LoanApplicationDraftConstants.currentStepParameterName);
        final String payloadJson = command.stringValueOfParameterNamed(LoanApplicationDraftConstants.payloadJsonParameterName);

        final Client client = clientRepository.findOneWithNotFoundDetection(clientId);
        final LoanProduct loanProduct = loanProductRepository.findById(loanProductId)
                .orElseThrow(() -> new LoanProductNotFoundException(loanProductId));

        final LoanApplicationDraft loanApplicationDraft = new LoanApplicationDraft(client, loanProduct,
                LoanApplicationDraftStatus.IN_PROGRESS.getValue(), currentStep, payloadJson);

        loanApplicationDraftRepository.saveAndFlush(loanApplicationDraft);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(loanApplicationDraft.getId()).build();
    }

    @Override
    public CommandProcessingResult updateLoanApplicationDraft(Long draftId, JsonCommand command) {

        LoanApplicationDraft loanApplicationDraft = loanApplicationDraftRepository.findById(draftId)
                .orElseThrow(() -> new LoanApplicationDraftNotFoundException(draftId));

        this.validateForUpdate(command.json());

        final Map<String, Object> changes = new LinkedHashMap<>(20);

        final Long loanProductId = command.longValueOfParameterNamed(LoanApplicationDraftConstants.loanProductIdParameterName);
        final LoanProduct loanProduct = loanProductRepository.findById(loanProductId)
                .orElseThrow(() -> new LoanProductNotFoundException(loanProductId));

        loanApplicationDraft.modifyApplication(loanProduct, command, changes);

        loanApplicationDraftRepository.saveAndFlush(loanApplicationDraft);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(draftId) //
                .with(changes) //
                .build();
    }

    @Override
    public CommandProcessingResult deleteById(Long id) {
        LoanApplicationDraft loanApplicationDraft = loanApplicationDraftRepository.findById(id)
                .orElseThrow(() -> new LoanApplicationDraftNotFoundException(id));

        final Map<String, Object> changes = new LinkedHashMap<>(20);

        loanApplicationDraft.delete();

        loanApplicationDraftRepository.saveAndFlush(loanApplicationDraft);

        changes.put(LoanApplicationDraftConstants.statusValueParameterName, LoanApplicationDraftStatus.DELETED.getValue());

        return new CommandProcessingResultBuilder() //
                .withEntityId(id) //
                .with(changes) //
                .build();
    }

    @Override
    public void deleteLoanApplicationDraft(Long draftId) {
        LoanApplicationDraft loanApplicationDraft = this.loanApplicationDraftRepository.findById(draftId)
                .orElseThrow(() -> new LoanApplicationDraftNotFoundException(draftId));

        loanApplicationDraft.delete();

        loanApplicationDraftRepository.saveAndFlush(loanApplicationDraft);
    }

    private void validateForCreate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                LoanApplicationDraftConstants.LOAN_APPLICATION_DRAFT_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loanApplicationDraft");
        final JsonElement jsonElement = fromApiJsonHelper.parse(json);

        final Long clientIdParam = fromApiJsonHelper.extractLongNamed(LoanApplicationDraftConstants.clientIdParameterName, jsonElement);
        baseDataValidator.reset().parameter(LoanApplicationDraftConstants.clientIdParameterName).value(clientIdParam).notBlank().notNull()
                .longGreaterThanZero();

        final Long loanProductIdParam = fromApiJsonHelper.extractLongNamed(LoanApplicationDraftConstants.loanProductIdParameterName,
                jsonElement);
        baseDataValidator.reset().parameter(LoanApplicationDraftConstants.loanProductIdParameterName).value(loanProductIdParam).notBlank()
                .notNull().longGreaterThanZero();

        final String currentStepParam = fromApiJsonHelper.extractStringNamed(LoanApplicationDraftConstants.currentStepParameterName,
                jsonElement);
        baseDataValidator.reset().parameter(LoanApplicationDraftConstants.currentStepParameterName).value(currentStepParam).notNull()
                .notBlank();

        final String payloadJsonParam = fromApiJsonHelper.extractStringNamed(LoanApplicationDraftConstants.payloadJsonParameterName,
                jsonElement);
        baseDataValidator.reset().parameter(LoanApplicationDraftConstants.payloadJsonParameterName).value(payloadJsonParam).notBlank()
                .notNull();

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }

    }

    private void validateForUpdate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                LoanApplicationDraftConstants.LOAN_APPLICATION_DRAFT_FOR_UPDATE_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loanApplicationDraft");
        final JsonElement jsonElement = fromApiJsonHelper.parse(json);

        final Long loanProductIdParam = fromApiJsonHelper.extractLongNamed(LoanApplicationDraftConstants.loanProductIdParameterName,
                jsonElement);
        baseDataValidator.reset().parameter(LoanApplicationDraftConstants.loanProductIdParameterName).value(loanProductIdParam).notBlank()
                .notNull().longGreaterThanZero();

        final String currentStepParam = fromApiJsonHelper.extractStringNamed(LoanApplicationDraftConstants.currentStepParameterName,
                jsonElement);
        baseDataValidator.reset().parameter(LoanApplicationDraftConstants.currentStepParameterName).value(currentStepParam).notNull()
                .notBlank();

        final String payloadJsonParam = fromApiJsonHelper.extractStringNamed(LoanApplicationDraftConstants.payloadJsonParameterName,
                jsonElement);
        baseDataValidator.reset().parameter(LoanApplicationDraftConstants.payloadJsonParameterName).value(payloadJsonParam).notBlank()
                .notNull();

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }

    }

}
