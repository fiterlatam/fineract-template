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
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanBlockingReason;
import org.apache.fineract.portfolio.loanaccount.domain.LoanBlockingReasonRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanBlockCommandFromApiValidator;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanBlockWritePlatformServiceImpl implements LoanBlockWritePlatformService {

    private final LoanBlockCommandFromApiValidator loanBlockCommandFromApiValidator;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanRepository loanRepository;
    private final LoanBlockingReasonRepository loanBlockingReasonRepository;
    private final PlatformSecurityContext context;

    @Override
    public CommandProcessingResult deleteLoanBlockReason(final JsonCommand command) {

        this.loanBlockCommandFromApiValidator.validateForDelete(command.json());

        final String[] loanBlockIds = command.arrayValueOfParameterNamed(LoanBlockCommandFromApiValidator.LOAN_BLOCK_IDS);
        final String comment = command.stringValueOfParameterNamed(LoanBlockCommandFromApiValidator.BLOCKING_COMMENT);
        final LocalDate date = command.dateValueOfParameterNamed(LoanBlockCommandFromApiValidator.BLOCKING_DATE);
        final Long loanId = command.entityId();
        final AppUser currentUser = this.context.authenticatedUser();

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
        final BlockingReasonSetting blockingReasonSetting = loan.getLoanCustomizationDetail().getBlockStatus();

        final Collection<LoanBlockingReason> blockingReasons = loanBlockingReasonRepository.findActiveByLoanIdAndBlocks(loanId,
                loanBlockIds);

        // Deactivate the blocking reasons
        for (String loanBlockId : loanBlockIds) {
            for (LoanBlockingReason blockingReason : blockingReasons) {
                if (blockingReason.getId().equals(Long.valueOf(loanBlockId))) {
                    blockingReason.setActive(false);
                    blockingReason.setDeactivatedBy(currentUser);
                    blockingReason.setUnblockComment(comment);
                    blockingReason.setDeactivatedOn(date);
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

        return CommandProcessingResult.commandOnlyResult(command.commandId());

    }

}
