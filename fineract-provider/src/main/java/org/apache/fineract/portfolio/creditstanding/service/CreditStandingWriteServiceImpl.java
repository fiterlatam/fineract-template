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
package org.apache.fineract.portfolio.creditstanding.service;

import java.util.Map;
import javax.persistence.PersistenceException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.creditstanding.api.CreditStandingApiConstants;
import org.apache.fineract.portfolio.creditstanding.domain.CreditStanding;
import org.apache.fineract.portfolio.creditstanding.domain.CreditStandingRepository;
import org.apache.fineract.portfolio.creditstanding.exception.CreditStandingNotFoundException;
import org.apache.fineract.portfolio.creditstanding.serialization.CreditStandingCommandFromApiJsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

@Service
public class CreditStandingWriteServiceImpl implements CreditStandingWriteService {

    private static final Logger LOG = LoggerFactory.getLogger(CreditStandingWriteServiceImpl.class);

    private CreditStandingCommandFromApiJsonDeserializer creditStandingCommandFromApiJsonDeserializer;
    private ClientRepositoryWrapper clientRepositoryWrapper;
    private CreditStandingRepository creditStandingRepository;

    @Autowired
    public CreditStandingWriteServiceImpl(CreditStandingCommandFromApiJsonDeserializer creditStandingCommandFromApiJsonDeserializer,
            ClientRepositoryWrapper clientRepositoryWrapper, CreditStandingRepository creditStandingRepository) {
        this.creditStandingCommandFromApiJsonDeserializer = creditStandingCommandFromApiJsonDeserializer;
        this.clientRepositoryWrapper = clientRepositoryWrapper;
        this.creditStandingRepository = creditStandingRepository;
    }

    @Override
    public CommandProcessingResult createCreditStanding(JsonCommand command) {
        this.creditStandingCommandFromApiJsonDeserializer.validateForCreate(command.json());
        Long clientId = command.longValueOfParameterNamed(CreditStandingApiConstants.clientIdParamName);
        Client client = null;
        try {
            if (clientId != null) {
                client = clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);
            }

            CreditStanding creditStanding = CreditStanding.fromJson(command, client);

            this.creditStandingRepository.saveAndFlush(creditStanding);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(creditStanding.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleCreditStandingDataIntegrityViolation(clientId, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleCreditStandingDataIntegrityViolation(clientId, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Override
    public CommandProcessingResult updateCreditStanding(Long clientId, JsonCommand command) {
        this.creditStandingCommandFromApiJsonDeserializer.validateForUpdate(command.json());

        try {
            CreditStanding creditStanding = this.creditStandingRepository.findCreditStandingByClientId(clientId);

            if (creditStanding == null) {
                throw new CreditStandingNotFoundException(clientId);
            }
            Map<String, Object> changes = creditStanding.update(command);

            this.creditStandingRepository.saveAndFlush(creditStanding);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(creditStanding.getId()) //
                    .with(changes) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleCreditStandingDataIntegrityViolation(clientId, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult(Long.valueOf(-1));
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleCreditStandingDataIntegrityViolation(clientId, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    private void handleCreditStandingDataIntegrityViolation(final Long clientId, final Throwable cause, final Exception dve) {
        logAsErrorUnexpectedDataIntegrityException(dve);
        throw new PlatformDataIntegrityException("error.msg.creditstanding.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private void logAsErrorUnexpectedDataIntegrityException(final Exception dve) {
        LOG.error("Error occured.", dve);
    }
}
