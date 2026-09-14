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
package org.apache.fineract.infrastructure.codes.service;

import java.util.Map;
import javax.persistence.PersistenceException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeCause;
import org.apache.fineract.infrastructure.codes.domain.CodeCauseRepository;
import org.apache.fineract.infrastructure.codes.domain.CodeCauseRepositoryWrapper;
import org.apache.fineract.infrastructure.codes.serialization.CodeCauseCommandFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodeCauseWritePlatformServiceImpl implements CodeCauseWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeCauseWritePlatformServiceImpl.class);

    private final PlatformSecurityContext context;
    private final CodeCauseRepository codeRepository;
    private final CodeCauseCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final CodeCauseRepositoryWrapper codeRepositoryWrapper;
    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public CodeCauseWritePlatformServiceImpl(final PlatformSecurityContext context, final CodeCauseRepository codeRepository,
            final CodeCauseCommandFromApiJsonDeserializer fromApiJsonDeserializer, final CodeCauseRepositoryWrapper codeRepositoryWrapper,
            final FromJsonHelper fromApiJsonHelper) {
        this.context = context;
        this.codeRepository = codeRepository;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.codeRepositoryWrapper = codeRepositoryWrapper;
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    @Transactional
    @Override
    public CommandProcessingResult createCode(final JsonCommand command) {
        try {
            this.context.authenticatedUser();

            final CodeCause code = this.fromApiJsonDeserializer.validateForCreate(command.json());

            this.codeRepository.saveAndFlush(code);

            return new CommandProcessingResultBuilder().withCommandId(command.commandId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateCode(Long causeId, final JsonCommand command) {
        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            CodeCause codeCauseToUpdate = this.codeRepositoryWrapper.findOneWithNotFoundDetection(causeId.intValue());
            final Map<String, Object> changes = codeCauseToUpdate.update(command);

            if (!changes.isEmpty()) {
                this.codeRepository.saveAndFlush(codeCauseToUpdate);
            }

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(causeId) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteCode(Long causeId) {

        CodeCause codeCauseToDelete = this.codeRepositoryWrapper.findOneWithNotFoundDetection(causeId.intValue());

        try {
            this.context.authenticatedUser();

            this.codeRepository.delete(codeCauseToDelete);

            this.codeRepository.flush();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            final Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            LOG.error("Error occurred.", throwable);
            throw new PlatformDataIntegrityException("error.msg.code.cause.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.", dve);
        }
        return new CommandProcessingResultBuilder().withEntityId(causeId).build();
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {

        if (realCause.getMessage().contains("m_causal_process_mapping")) {
            throw new PlatformDataIntegrityException("error.msg.code.cause.association.exists",
                    "cannot.delete.code.cause.with.association");
        }

        if (realCause.getMessage().contains("Duplicate entry")) {
            final String id = command.stringValueOfParameterNamed("id");
            throw new PlatformDataIntegrityException("error.msg.code.cause.duplicate.id", "Code causal with ID `" + id + "` already exists",
                    "name", id);
        }
        logAsErrorUnexpectedDataIntegrityException(dve);
        throw new PlatformDataIntegrityException("error.msg.code.cause.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private void logAsErrorUnexpectedDataIntegrityException(final Exception dve) {
        LOG.error("Error occurred.", dve);
    }

}
