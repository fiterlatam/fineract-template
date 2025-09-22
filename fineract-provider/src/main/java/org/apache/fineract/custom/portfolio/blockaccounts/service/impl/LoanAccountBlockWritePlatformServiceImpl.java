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
package org.apache.fineract.custom.portfolio.blockaccounts.service.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.blockaccounts.api.LoanAccountBlockConstants;
import org.apache.fineract.custom.portfolio.blockaccounts.domain.LoanAccountBlock;
import org.apache.fineract.custom.portfolio.blockaccounts.domain.LoanAccountBlockAction;
import org.apache.fineract.custom.portfolio.blockaccounts.domain.LoanAccountBlockRepository;
import org.apache.fineract.custom.portfolio.blockaccounts.service.LoanAccountBlockWritePlatformService;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingsRepository;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanSubStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanaccount.service.LoanUtilService;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformServiceJpaRepositoryImpl;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoanAccountBlockWritePlatformServiceImpl implements LoanAccountBlockWritePlatformService {

    private final FromJsonHelper fromApiJsonHelper;
    private final LoanRepository loanRepository;
    private final LoanAccountBlockRepository loanAccountBlockRepository;
    private final BlockingReasonSettingsRepository blockingReasonSettingsRepository;
    private final LoanUtilService loanUtilService;
    private final LoanWritePlatformServiceJpaRepositoryImpl loanWritePlatformService;
    private final LoanAssembler loanAssembler;
    private final ConfigurationDomainService configurationService;

    @Override
    public CommandProcessingResult createLoanAccountBlock(JsonCommand command) {

        validateForCreate(command.json());

        LoanAccountBlock loanAccountBlock = null;

        final Long loanId = command.getLoanId();
        final Long blockingReasonId = command.longValueOfParameterNamed(LoanAccountBlockConstants.blockingReasonIdParamName);
        final LocalDate applicationDate = command.dateValueOfParameterNamed(LoanAccountBlockConstants.applicationDateParamName);
        final Boolean accelerate = command.booleanObjectValueOfParameterNamed(LoanAccountBlockConstants.accelerateParamName);
        final Boolean freezeCurrentInterest = command
                .booleanObjectValueOfParameterNamed(LoanAccountBlockConstants.freezeCurrentInterestParamName);
        final Boolean freezeInterestArrears = command
                .booleanObjectValueOfParameterNamed(LoanAccountBlockConstants.freezeInterestArrearsParamName);
        final Boolean freezeLifeInsurance = command
                .booleanObjectValueOfParameterNamed(LoanAccountBlockConstants.freezeLifeInsuranceParamName);
        final Boolean freezeMypime = command.booleanObjectValueOfParameterNamed(LoanAccountBlockConstants.freezeMypimeParamName);
        final Boolean active = command.booleanObjectValueOfParameterNamed(LoanAccountBlockConstants.activeParamName);
        final LocalDate businessDate = DateUtils.getBusinessLocalDate();

        Loan loan = this.loanAssembler.assembleFrom(loanId);

        boolean withoutActions = validateCreation(loanId, applicationDate, loan, businessDate);

        loan.setLoanSubStatus(LoanSubStatus.BLOCKED.getValue());
        loan = loanRepository.saveAndFlush(loan);
        BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepository.getReferenceById(blockingReasonId);

        loanAccountBlock = new LoanAccountBlock().createLoanAccountBlock(loan, blockingReasonSetting, applicationDate, accelerate,
                freezeCurrentInterest, freezeInterestArrears, freezeLifeInsurance, freezeMypime, active, LoanAccountBlockAction.BLOCK,
                null);

        if (withoutActions && !configurationService.getLoanBlockTestEnabled()) {
            loanAccountBlock.setAccelerate(false);
            loanAccountBlock.setFreezeCurrentInterest(false);
            loanAccountBlock.setFreezeInterestArrears(false);
            loanAccountBlock.setFreezeLifeInsurance(false);
            loanAccountBlock.setFreezeMypime(false);
        }

        loanAccountBlock = loanAccountBlockRepository.saveAndFlush(loanAccountBlock);

        // Regenerates schedule in case of blocking with freeze interest, life insurance or any MiPyme charge
        if (freezeCurrentInterest || freezeLifeInsurance || freezeMypime) {
            loan.getLoanAccountBlocks().add(loanAccountBlock);

            ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, applicationDate);
            loan.regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);

            loanWritePlatformService.saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdAsString(loanAccountBlock.getId().toString()) //
                .build();
    }

    @Override
    public CommandProcessingResult updateLoanAccountBlock(final Long loanAccountBlockId, JsonCommand command) {
        final JsonElement json = fromApiJsonHelper.parse(command.json());
        final JsonObject topLevelJsonElement = json.getAsJsonObject();
        validateForCreate(command.json());
        final Locale locale = fromApiJsonHelper.extractLocaleParameter(topLevelJsonElement);

        final Optional<LoanAccountBlock> optLoanAccountBlock = loanAccountBlockRepository.findById(loanAccountBlockId);

        if (!optLoanAccountBlock.isPresent()) {
            throw new NotFoundException(String.valueOf(command.getLoanId()));
        }

        LoanAccountBlock accountBlock = optLoanAccountBlock.get();
        accountBlock.setActive(false);
        loanAccountBlockRepository.save(accountBlock);

        LoanAccountBlock loanAccountBlock = new LoanAccountBlock();
        loanAccountBlock.setLoan(accountBlock.getLoan());

        final Long blockingReasonId = command.longValueOfParameterNamed(LoanAccountBlockConstants.blockingReasonIdParamName);
        BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepository.getReferenceById(blockingReasonId);

        loanAccountBlock.setBlockingReasonSetting(
                accountBlock.getBlockingReasonSetting() != null && !accountBlock.getBlockingReasonSetting().equals(blockingReasonSetting)
                        ? blockingReasonSetting
                        : accountBlock.getBlockingReasonSetting());

        final LocalDate applicationDate = command.dateValueOfParameterNamed(LoanAccountBlockConstants.applicationDateParamName);

        loanAccountBlock.setApplicationDate(
                accountBlock.getApplicationDate() != null && !accountBlock.getApplicationDate().equals(applicationDate) ? applicationDate
                        : accountBlock.getApplicationDate());

        final Boolean accelerate = command.booleanPrimitiveValueOfParameterNamed(LoanAccountBlockConstants.accelerateParamName);

        loanAccountBlock.setAccelerate(
                accountBlock.getAccelerate() != null && !accountBlock.getAccelerate() && !accountBlock.getAccelerate().equals(accelerate)
                        ? accelerate
                        : accountBlock.getAccelerate());

        final Boolean freezeCurrentInterest = command
                .booleanPrimitiveValueOfParameterNamed(LoanAccountBlockConstants.freezeCurrentInterestParamName);

        loanAccountBlock
                .setFreezeCurrentInterest(accountBlock.getFreezeCurrentInterest() != null && !accountBlock.getFreezeCurrentInterest()
                        && !accountBlock.getFreezeCurrentInterest().equals(freezeCurrentInterest) ? freezeCurrentInterest
                                : accountBlock.getFreezeCurrentInterest());

        final Boolean freezeInterestArrears = command
                .booleanPrimitiveValueOfParameterNamed(LoanAccountBlockConstants.freezeInterestArrearsParamName);

        loanAccountBlock
                .setFreezeInterestArrears(accountBlock.getFreezeInterestArrears() != null && !accountBlock.getFreezeInterestArrears()
                        && !accountBlock.getFreezeInterestArrears().equals(freezeInterestArrears) ? freezeInterestArrears
                                : accountBlock.getFreezeInterestArrears());

        final Boolean freezeLifeInsurance = command
                .booleanPrimitiveValueOfParameterNamed(LoanAccountBlockConstants.freezeLifeInsuranceParamName);

        loanAccountBlock.setFreezeLifeInsurance(accountBlock.getFreezeLifeInsurance() != null && !accountBlock.getFreezeLifeInsurance()
                && !accountBlock.getFreezeLifeInsurance().equals(freezeLifeInsurance) ? freezeLifeInsurance
                        : accountBlock.getFreezeLifeInsurance());

        final Boolean freezeMypime = command.booleanPrimitiveValueOfParameterNamed(LoanAccountBlockConstants.freezeMypimeParamName);

        loanAccountBlock.setFreezeMypime(accountBlock.getFreezeMypime() != null && !accountBlock.getFreezeMypime()
                && !accountBlock.getFreezeMypime().equals(freezeMypime) ? freezeMypime : accountBlock.getFreezeMypime());

        loanAccountBlock.setAction(accountBlock.getAction());
        loanAccountBlock.setActive(true);
        loanAccountBlockRepository.save(loanAccountBlock);

        regenerateScheduleIfNecessary(freezeCurrentInterest, freezeLifeInsurance, freezeMypime, loanAccountBlock, applicationDate);

        return new CommandProcessingResultBuilder().withEntityId(loanAccountBlockId).build();
    }

    private void regenerateScheduleIfNecessary(Boolean freezeCurrentInterest, Boolean freezeLifeInsurance, Boolean freezeMypime,
            LoanAccountBlock loanAccountBlock, LocalDate applicationDate) {
        // Regenerates schedule in case of blocking with freeze interest, life insurance or any MiPyme charge
        if (freezeCurrentInterest || freezeLifeInsurance || freezeMypime) {
            regenerateSchedule(loanAccountBlock, applicationDate);
        }
    }

    private void regenerateSchedule(LoanAccountBlock loanAccountBlock, LocalDate applicationDate) {
        Loan loan = this.loanAssembler.assembleFrom(loanAccountBlock.getLoan().getId());

        loan.getLoanAccountBlocks().add(loanAccountBlock);

        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, applicationDate);
        loan.regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);

        loanWritePlatformService.saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
    }

    @Override
    public CommandProcessingResult unblockLoanAccount(JsonCommand command) {
        validateForUnblock(command.json());
        Optional<LoanAccountBlock> optLoanAccountBlock = loanAccountBlockRepository.retrieveByLoanIdAndStatusActive(command.getLoanId());
        if (optLoanAccountBlock.isEmpty()) {
            throw new NotFoundException(String.valueOf(command.getLoanId()));
        }

        LoanAccountBlock loanAccountBlock = optLoanAccountBlock.get();
        loanAccountBlock.setActive(false);
        loanAccountBlockRepository.save(loanAccountBlock);

        final String note = command.stringValueOfParameterNamed(LoanAccountBlockConstants.noteParamName);
        final LocalDate applicationDate = command.dateValueOfParameterNamed(LoanAccountBlockConstants.applicationDateParamName);

        LoanAccountBlock loanAccountUnblock = new LoanAccountBlock().createLoanAccountBlock(loanAccountBlock.getLoan(),
                loanAccountBlock.getBlockingReasonSetting(), applicationDate, loanAccountBlock.getAccelerate(),
                loanAccountBlock.getFreezeCurrentInterest(), loanAccountBlock.getFreezeInterestArrears(),
                loanAccountBlock.getFreezeLifeInsurance(), loanAccountBlock.getFreezeMypime(), false, LoanAccountBlockAction.UNBLOCK, note);

        loanAccountUnblock = loanAccountBlockRepository.saveAndFlush(loanAccountUnblock);

        regenerateSchedule(loanAccountBlock, applicationDate);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdAsString(loanAccountUnblock.getId().toString()) //
                .build();
    }

    private boolean validateCreation(final Long loanId, LocalDate applicationDate, Loan loan, LocalDate businessDate) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>(1);
        Optional<LoanAccountBlock> existingBlock = loanAccountBlockRepository.retrieveByLoanIdAndStatusActive(loanId);

        if (existingBlock.isPresent()) {
            ApiParameterError apiParameterError = ApiParameterError.parameterError("already.blocked", "The account is already blocked",
                    "loanId", loanId);
            dataValidationErrors.add(apiParameterError);
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "The account is already blocked",
                    dataValidationErrors);
        }

        /* Transaction is in the past */
        if (applicationDate.isBefore(businessDate)) {

            Optional<LocalDate> lastDisbursement = loan.getLoanTransactions().stream().filter(txn -> txn.getTypeOf().isDisbursement())
                    .map(LoanTransaction::getTransactionDate).max(Comparator.naturalOrder());

            if (lastDisbursement.isPresent()) {
                LocalDate disbursementDate = lastDisbursement.get();

                if (applicationDate.isAfter(disbursementDate)) { // CASE 1: PASS WITHOUT ACTIONS
                    return true;
                } else if (applicationDate.isBefore(disbursementDate)) { // CASE 2: FAIL
                    ApiParameterError apiParameterError = ApiParameterError.parameterError("disbursement.after.selected.date",
                            "The block could not be applied because there are disbursements after the selected application date.",
                            "applicationDate", applicationDate);
                    dataValidationErrors.add(apiParameterError);
                    throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
                            "There are disbursements after selected application date.", dataValidationErrors);
                } else {
                    log.info("Selected application date it's equals to last disbursement date, applying with actions.");
                    return false;
                }
            }
        }

        return false;

    }

    private void validateForUnblock(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, LoanAccountBlockConstants.REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(LoanAccountBlockConstants.loanIdParamName);

        final JsonElement jsonElement = fromApiJsonHelper.parse(json);
        final Long loanId = this.fromApiJsonHelper.extractLongNamed(LoanAccountBlockConstants.loanIdParamName, jsonElement);
        baseDataValidator.reset().parameter(LoanAccountBlockConstants.blockingReasonIdParamName).value(loanId).notBlank().notNull();

        final LocalDate date = this.fromApiJsonHelper.extractLocalDateNamed(LoanAccountBlockConstants.applicationDateParamName,
                jsonElement);
        baseDataValidator.reset().parameter(LoanAccountBlockConstants.applicationDateParamName).value(date).notBlank().notNull();

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }

    private void validateForCreate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, LoanAccountBlockConstants.REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(LoanAccountBlockConstants.loanIdParamName);

        final JsonElement jsonElement = fromApiJsonHelper.parse(json);
        final Locale locale = this.fromApiJsonHelper.extractLocaleParameter(jsonElement.getAsJsonObject());

        final LocalDate date = this.fromApiJsonHelper.extractLocalDateNamed(LoanAccountBlockConstants.applicationDateParamName,
                jsonElement);
        baseDataValidator.reset().parameter(LoanAccountBlockConstants.applicationDateParamName).value(date).notBlank().notNull();

        final Long blockingReason = this.fromApiJsonHelper.extractLongNamed(LoanAccountBlockConstants.blockingReasonIdParamName,
                jsonElement);
        baseDataValidator.reset().parameter(LoanAccountBlockConstants.blockingReasonIdParamName).value(blockingReason).notBlank().notNull();

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }
}
