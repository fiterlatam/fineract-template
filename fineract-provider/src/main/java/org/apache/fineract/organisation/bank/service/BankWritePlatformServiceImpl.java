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
package org.apache.fineract.organisation.bank.service;

import java.util.Map;
import javax.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.bank.domain.Bank;
import org.apache.fineract.organisation.bank.domain.BankRepositoryWrapper;
import org.apache.fineract.organisation.bank.serialization.BankCommandFromApiJsonDeserializer;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class BankWritePlatformServiceImpl implements BankWritePlatformService {

    private final PlatformSecurityContext context;
    private final BankCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final BankRepositoryWrapper bankRepositoryWrapper;

    public BankWritePlatformServiceImpl(final PlatformSecurityContext context,
            final BankCommandFromApiJsonDeserializer fromApiJsonDeserializer, final BankRepositoryWrapper bankRepositoryWrapper) {
        this.context = context;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.bankRepositoryWrapper = bankRepositoryWrapper;
    }

    @Transactional
    @Override
    public CommandProcessingResult createBank(JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            final Bank bank = Bank.fromJson(currentUser, command);

            Bank result = this.bankRepositoryWrapper.saveAndFlush(bank);

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
    public CommandProcessingResult updateBank(JsonCommand command, Long bankId) {
        try {

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            Bank bankForUpdate = this.bankRepositoryWrapper.findOneWithNotFoundDetection(bankId);

            final Map<String, Object> changes = bankForUpdate.update(command);

            Bank result = this.bankRepositoryWrapper.saveAndFlush(bankForUpdate);

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
    public CommandProcessingResult deleteBank(Long bankId) {
        try {

            final Bank bank = this.bankRepositoryWrapper.findOneWithNotFoundDetection(bankId);

            this.bankRepositoryWrapper.delete(bank);
            return new CommandProcessingResultBuilder() //
                    .withEntityId(bank.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            log.error("Error occured.", throwable);
            throw new PlatformDataIntegrityException("error.msg.bank.unknown.data.integrity.issue",
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
}
