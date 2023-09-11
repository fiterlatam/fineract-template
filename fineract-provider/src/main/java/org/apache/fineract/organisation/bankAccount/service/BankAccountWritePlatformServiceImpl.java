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
package org.apache.fineract.organisation.bankAccount.service;

import java.util.Map;
import javax.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.glaccount.domain.GLAccountRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.agency.domain.Agency;
import org.apache.fineract.organisation.agency.domain.AgencyRepositoryWrapper;
import org.apache.fineract.organisation.bank.domain.Bank;
import org.apache.fineract.organisation.bank.domain.BankRepositoryWrapper;
import org.apache.fineract.organisation.bankAccount.domain.BankAccount;
import org.apache.fineract.organisation.bankAccount.domain.BankAccountRepositoryWrapper;
import org.apache.fineract.organisation.bankAccount.exception.BankAccountDuplicateException;
import org.apache.fineract.organisation.bankAccount.serialization.BankAccountCommandFromApiJsonDeserializer;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class BankAccountWritePlatformServiceImpl implements BankAccountWritePlatformService {

    private final PlatformSecurityContext context;
    private final BankAccountCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final BankAccountRepositoryWrapper bankAccountRepositoryWrapper;
    private final AgencyRepositoryWrapper agencyRepositoryWrapper;
    private final GLAccountRepositoryWrapper glAccountRepositoryWrapper;

    private final BankRepositoryWrapper bankRepositoryWrapper;

    public BankAccountWritePlatformServiceImpl(final PlatformSecurityContext context,
            final BankAccountCommandFromApiJsonDeserializer fromApiJsonDeserializer,
            final BankAccountRepositoryWrapper bankAccountRepositoryWrapper, final AgencyRepositoryWrapper agencyRepositoryWrapper,
            final GLAccountRepositoryWrapper glAccountRepositoryWrapper, final BankRepositoryWrapper bankRepositoryWrapper) {
        this.context = context;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.bankAccountRepositoryWrapper = bankAccountRepositoryWrapper;
        this.agencyRepositoryWrapper = agencyRepositoryWrapper;
        this.glAccountRepositoryWrapper = glAccountRepositoryWrapper;
        this.bankRepositoryWrapper = bankRepositoryWrapper;
    }

    @Transactional
    @Override
    public CommandProcessingResult createBankAccount(JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            Agency agency = null;
            final Long agencyId = command
                    .longValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.AGENCY_ID.getValue());
            if (agencyId != null) {
                agency = agencyRepositoryWrapper.findOneWithNotFoundDetection(agencyId);
            }

            final Long accountNumber = command
                    .longValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.ACCOUNT_NUMBER.getValue());
            // validate Bank Account and Agency aren't duplicate
            validateDuplicateBankAccountAndAgency(accountNumber, agency);

            Bank bank = null;
            final Long bankId = command.longValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.BANK_ID.getValue());
            if (bankId != null) {
                bank = bankRepositoryWrapper.findOneWithNotFoundDetection(bankId);
            }

            GLAccount glAccount = null;
            final Long glAccountId = command
                    .longValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.GLACCOUNT_ID.getValue());
            if (glAccountId != null) {
                glAccount = glAccountRepositoryWrapper.findOneWithNotFoundDetection(glAccountId);
            }

            final BankAccount bankAccount = BankAccount.fromJson(currentUser, agency, bank, glAccount, command);

            BankAccount result = this.bankAccountRepositoryWrapper.saveAndFlush(bankAccount);

            return new CommandProcessingResultBuilder() //
                    .withEntityId(result.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleBankDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleBankDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateBankAccount(JsonCommand command, Long bankAccountId) {
        try {

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            BankAccount bankAccountForUpdate = this.bankAccountRepositoryWrapper.findOneWithNotFoundDetection(bankAccountId);

            final Map<String, Object> changes = bankAccountForUpdate.update(command);

            if (command.parameterExists(BankAccountConstants.BankAccountSupportedParameters.AGENCY_ID.getValue())) {
                final Long agencyId = command
                        .longValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.AGENCY_ID.getValue());
                Agency agency = agencyRepositoryWrapper.findOneWithNotFoundDetection(agencyId);
                bankAccountForUpdate.setAgency(agency);
                changes.put(BankAccountConstants.BankAccountSupportedParameters.AGENCY_ID.getValue(), agencyId);
            }

            if (command.parameterExists(BankAccountConstants.BankAccountSupportedParameters.BANK_ID.getValue())) {
                final Long bankId = command
                        .longValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.BANK_ID.getValue());
                Bank bank = bankRepositoryWrapper.findOneWithNotFoundDetection(bankId);
                bankAccountForUpdate.setBank(bank);
                changes.put(BankAccountConstants.BankAccountSupportedParameters.BANK_ID.getValue(), bankId);
            }

            if (command.parameterExists(BankAccountConstants.BankAccountSupportedParameters.GLACCOUNT_ID.getValue())) {
                final Long glAccountId = command
                        .longValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.GLACCOUNT_ID.getValue());
                GLAccount glAccount = glAccountRepositoryWrapper.findOneWithNotFoundDetection(glAccountId);
                bankAccountForUpdate.setGlAccount(glAccount);
                changes.put(BankAccountConstants.BankAccountSupportedParameters.GLACCOUNT_ID.getValue(), glAccountId);
            }

            if (changes.containsKey(BankAccountConstants.BankAccountSupportedParameters.ACCOUNT_NUMBER.getValue())) {
                final Long accountNumber = command
                        .longValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.ACCOUNT_NUMBER.getValue());

                // validate Bank Account and Agency aren't duplicate
                validateDuplicateBankAccountAndAgency(accountNumber, bankAccountForUpdate.getAgency());

                bankAccountForUpdate.setAccountNumber(accountNumber);
            }

            if (changes.containsKey(BankAccountConstants.BankAccountSupportedParameters.DESCRIPTION.getValue())) {
                final String description = command
                        .stringValueOfParameterNamed(BankAccountConstants.BankAccountSupportedParameters.DESCRIPTION.getValue());
                bankAccountForUpdate.setDescription(description);
            }

            BankAccount result = this.bankAccountRepositoryWrapper.saveAndFlush(bankAccountForUpdate);

            return new CommandProcessingResultBuilder() //
                    .withEntityId(result.getId()) //
                    .with(changes) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleBankDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleBankDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteBankAccount(Long bankId) {
        try {

            final BankAccount bankAccount = this.bankAccountRepositoryWrapper.findOneWithNotFoundDetection(bankId);

            this.bankAccountRepositoryWrapper.delete(bankAccount);
            return new CommandProcessingResultBuilder() //
                    .withEntityId(bankAccount.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            log.error("Error occured.", throwable);
            throw new PlatformDataIntegrityException("error.msg.bankaccount.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.", dve);
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleBankDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("name")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.agency.duplicate.name", "Bank with name '" + name + "' already exists",
                    "name", name);
        }

        if (realCause.getMessage().contains("code")) {
            final String code = command.stringValueOfParameterNamed("code");
            throw new PlatformDataIntegrityException("error.msg.agency.duplicate.name", "Bank with code '" + code + "' already exists",
                    "code", code);
        }

        throw new PlatformDataIntegrityException("error.msg.agency.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private void validateDuplicateBankAccountAndAgency(Long accountNumber, Agency agency) {
        if (accountNumber != null && agency != null) {
            BankAccount bankAccount1 = bankAccountRepositoryWrapper.findOneByAccountAndAgency(accountNumber, agency);
            if (bankAccount1 != null) {
                throw new BankAccountDuplicateException(accountNumber, agency);
            }

        }
    }
}
