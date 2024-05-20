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

package org.apache.fineract.portfolio.loanaccount.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockLevel;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingEnum;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingsRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.exception.InvalidClientStateTransitionException;
import org.apache.fineract.portfolio.client.service.ClientWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanBlockingReason;
import org.apache.fineract.portfolio.loanaccount.domain.LoanBlockingReasonRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.exception.LoanBlockingReasonNotFoundException;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanBlockCommandFromApiValidator;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanBlockWritePlatformServiceImpl implements LoanBlockWritePlatformService {

    private final LoanBlockCommandFromApiValidator loanBlockCommandFromApiValidator;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanRepository loanRepository;
    private final LoanBlockingReasonRepository loanBlockingReasonRepository;
    private final PlatformSecurityContext context;
    private final BlockingReasonSettingsRepositoryWrapper blockingReasonSettingRepositoryWrapper;
    private final ClientWritePlatformService clientWritePlatformService;

    @Transactional
    @Override
    public CommandProcessingResult deleteLoanBlockReason(final JsonCommand command) {

        this.loanBlockCommandFromApiValidator.validateForDelete(command.json());

        final String[] loanBlockIds = command.arrayValueOfParameterNamed(LoanBlockCommandFromApiValidator.LOAN_BLOCK_IDS);
        final String comment = command.stringValueOfParameterNamed(LoanBlockCommandFromApiValidator.BLOCKING_COMMENT);
        final LocalDate unblockDate = command.dateValueOfParameterNamed(LoanBlockCommandFromApiValidator.BLOCKING_DATE);
        final Long loanId = command.entityId();
        final AppUser currentUser = this.context.authenticatedUser();

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
        final BlockingReasonSetting blockingReasonSetting = loan.getLoanCustomizationDetail().getBlockStatus();

        final Collection<LoanBlockingReason> blockingReasons = loanBlockingReasonRepository.findActiveByLoanIdAndBlocks(loanId,
                loanBlockIds);

        final Collection<LoanBlockingReason> unBlocked = new ArrayList();

        // Deactivate the blocking reasons
        for (String loanBlockId : loanBlockIds) {
            for (LoanBlockingReason blockingReason : blockingReasons) {
                if (blockingReason.getId().equals(Long.valueOf(loanBlockId))) {
                    handleDelete(blockingReason, unblockDate, currentUser, comment);
                    unBlocked.add(blockingReason);
                }

                if (blockingReasonSetting != null) {
                    // Check if the loan is still blocked
                    blockingReasonSetting.equals(blockingReason.getBlockingReasonSetting());
                    loan.getLoanCustomizationDetail().setBlockStatus(null);
                }
            }
        }

        // Replace with the highest priority that's active
        if (loan.getLoanCustomizationDetail().getBlockStatus() != null) {
            final Optional<LoanBlockingReason> highestPriorityReason = blockingReasons.stream().filter(LoanBlockingReason::isActive)
                    .sorted(Comparator.comparingInt(t -> t.getBlockingReasonSetting().getPriority())).findFirst();

            if (highestPriorityReason.isPresent()) {
                loan.getLoanCustomizationDetail().setBlockStatus(highestPriorityReason.get().getBlockingReasonSetting());
            }
        }

        this.loanRepository.save(loan);
        this.loanBlockingReasonRepository.saveAllAndFlush(blockingReasons);

        // Check if reason affects client too
        deleteBlockReasonFromClientIfPresent(unBlocked, loan.client(), currentUser, unblockDate, comment);

        return CommandProcessingResult.commandOnlyResult(command.commandId());

    }

    private void handleDelete(final LoanBlockingReason blockingReason, final LocalDate unblockDate, final AppUser currentUser,
            final String comment) {
        if (DateUtils.isAfter(blockingReason.getBlockDate(), unblockDate)) {
            final String errorMessage = "The loan unblock date cannot be before the loan block date.";
            throw new InvalidClientStateTransitionException("undoBlock", "date.cannot.before.loan.blockedOnDate.date", errorMessage,
                    unblockDate, blockingReason.getBlockDate());
        }

        blockingReason.setActive(false);
        blockingReason.setDeactivatedBy(currentUser);
        blockingReason.setUnblockComment(comment);
        blockingReason.setDeactivatedOn(unblockDate);
    }

    private void deleteBlockReasonFromClientIfPresent(final Collection<LoanBlockingReason> unBlocked, final Client client,
            final AppUser currentUser, final LocalDate unblockDate, final String comment) {
        for (LoanBlockingReason obj : unBlocked) {
            if (obj.getBlockingReasonSetting().isAffectsClientLevel()) {
                final List<BlockingReasonSetting> settings = blockingReasonSettingRepositoryWrapper
                        .getBlockingReasonSettingByReason(obj.getBlockingReasonSetting().getNameOfReason(), BlockLevel.CLIENT.toString());
                final Long blockReasonId = settings.get(0).getId();
                clientWritePlatformService.unblockClientBlockingReason(currentUser, client, unblockDate, blockReasonId, comment);
            }
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult blockLoanWithGuarantee(final JsonCommand command) {

        this.loanBlockCommandFromApiValidator.validateForBlockGuarantee(command.json());
        final Long loanId = command.entityId();
        final String comment = command.stringValueOfParameterNamed(LoanBlockCommandFromApiValidator.BLOCK_COMMENT);
        final Long blockId = command.longValueOfParameterNamed(LoanBlockCommandFromApiValidator.BLOCKING_REASON_ID);
        final LocalDate blockDate = command.localDateValueOfParameterNamed(LoanBlockCommandFromApiValidator.BLOCK_DATE);

        final BlockingReasonSetting blockingReasonSetting = this.blockingReasonSettingRepositoryWrapper
                .findOneWithNotFoundDetection(blockId);

        if (!blockingReasonSetting.getNameOfReason()
                .equalsIgnoreCase(BlockingReasonSettingEnum.CREDIT_RECLAMADO_A_AVALADORA.getDatabaseString())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.blocking.reason.not.allowed",
                    "Operation supported only for RECLAMADO A AVALADORA blocking reason");
        }

        final LoanBlockingReason blockingReason = this.blockLoan(loanId, blockingReasonSetting, comment, blockDate);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withLoanId(loanId).withEntityId(blockingReason.getId()) //
                .build();

    }

    @Transactional
    @Override
    public LoanBlockingReason blockLoan(final Long loanId, final BlockingReasonSetting blockingReasonSetting, final String comment,
            final LocalDate blockDate) {

        final Optional<LoanBlockingReason> existingBlockingReason = this.loanBlockingReasonRepository.findExistingBlockingReason(loanId,
                blockingReasonSetting.getId());

        if (existingBlockingReason.isPresent()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.blocking.reason.already.exists",
                    "Loan is already blocked with blocking reason");
        }

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

        // check if current blocking reason on loan has higher priority, if so, replace it with this blocking reason
        if (loan.getLoanCustomizationDetail().getBlockStatus() == null
                || loan.getLoanCustomizationDetail().getBlockStatus().getPriority() > blockingReasonSetting.getPriority()) {
            loan.getLoanCustomizationDetail().setBlockStatus(blockingReasonSetting);
        }

        final LoanBlockingReason loanBlockingReason = LoanBlockingReason.instance(loan, blockingReasonSetting, comment, blockDate);
        loanBlockingReasonRepository.saveAndFlush(loanBlockingReason);

        // Check if reason affects client too
        if (blockingReasonSetting.isAffectsClientLevel()) {
            clientWritePlatformService.blockClientWithInActiveLoan(loan.getClientId(), blockingReasonSetting.getNameOfReason(), comment,
                    false);
        }

        return loanBlockingReason;
    }

    @Transactional
    @Override
    public CommandProcessingResult unblockLoanMassively(JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();
            this.loanBlockCommandFromApiValidator.validateUnblockLoanMassively(command.json());

            final LocalDate unblockDate = command.localDateValueOfParameterNamed("unblockDate");
            final Long blockingReasonId = command.longValueOfParameterNamed("blockingReasonId");
            final String unblockComment = command.stringValueOfParameterNamed("unblockComment");
            final Set<String> loanIds = new HashSet<>(Arrays.asList(command.arrayValueOfParameterNamed("loanId")));

            final Set<Long> loanIdsLong = new HashSet<>();
            for (String str : loanIds) {
                long number = Long.parseLong(str);
                loanIdsLong.add(number);
            }

            final List<Loan> loans = this.loanRepository.findAllById(loanIdsLong);
            for (Loan loan : loans) {
                LoanBlockingReason loanBlockingReason = this.loanBlockingReasonRepository
                        .findExistingBlockingReason(loan.getId(), blockingReasonId)
                        .orElseThrow(() -> new LoanBlockingReasonNotFoundException(loan.getId(), blockingReasonId));
                handleDelete(loanBlockingReason, unblockDate, currentUser, unblockComment);

                final BlockingReasonSetting blockingReasonSetting = loan.getLoanCustomizationDetail().getBlockStatus();
                if (blockingReasonSetting != null) {
                    // Check if the loan is still blocked
                    blockingReasonSetting.equals(loanBlockingReason.getBlockingReasonSetting());
                    loan.getLoanCustomizationDetail().setBlockStatus(null);
                }

                this.loanRepository.save(loan);
                this.loanBlockingReasonRepository.saveAndFlush(loanBlockingReason);

                final Collection<LoanBlockingReason> unBlocked = new ArrayList();
                unBlocked.add(loanBlockingReason);
                Client client = loan.getClient();
                if (client.isBlocked()) {
                    deleteBlockReasonFromClientIfPresent(unBlocked, client, currentUser, unblockDate, unblockComment);
                }
            }

            return new CommandProcessingResultBuilder().build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        }
    }

    private void logAsErrorUnexpectedDataIntegrityException(final Exception dve) {
        log.error("Error occured.", dve);
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        logAsErrorUnexpectedDataIntegrityException(dve);
        throw ErrorHandler.getMappable(dve, "error.msg.loan.blocking.reason.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }
}
