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
package org.apache.fineract.organisation.paedocumentation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.paedocumentation.domain.PaeRequiredDocument;
import org.apache.fineract.organisation.paedocumentation.domain.PaeRequiredDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaeRequiredDocumentWritePlatformServiceJpaRepositoryImpl implements PaeRequiredDocumentWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(PaeRequiredDocumentWritePlatformServiceJpaRepositoryImpl.class);

    private final PlatformSecurityContext context;
    private final PaeRequiredDocumentRepository repository;
    private final CodeValueRepositoryWrapper codeValueRepositoryWrapper;

    public PaeRequiredDocumentWritePlatformServiceJpaRepositoryImpl(PlatformSecurityContext context,
            PaeRequiredDocumentRepository repository, CodeValueRepositoryWrapper codeValueRepositoryWrapper) {
        this.context = context;
        this.repository = repository;
        this.codeValueRepositoryWrapper = codeValueRepositoryWrapper;
    }

    @Override
    @Transactional
    public CommandProcessingResult create(JsonCommand command) {
        this.context.authenticatedUser();

        try {
            validateForCreate(command);

            final Long categoryId = command.longValueOfParameterNamed("categoryId");
            final String documentName = command.stringValueOfParameterNamed("documentName");
            final String description = command.stringValueOfParameterNamed("description");
            final boolean required = command.booleanPrimitiveValueOfParameterNamed("required");
            final String acceptedFormat = command.stringValueOfParameterNamed("acceptedFormat");

            CodeValue category = this.codeValueRepositoryWrapper.findOneWithNotFoundDetection(categoryId);

            Long createdBy = this.context.authenticatedUser().getId();
            LocalDateTime createdOn = DateUtils.getLocalDateTimeOfSystem();

            PaeRequiredDocument entity = new PaeRequiredDocument(category, documentName, description, acceptedFormat, required, createdBy,
                    createdOn);
            entity = this.repository.saveAndFlush(entity);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(entity.getId()) //
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

    @Override
    @Transactional
    public CommandProcessingResult delete(Long documentId) {
        this.context.authenticatedUser();
        PaeRequiredDocument entity = this.repository.findById(documentId)
                .orElseThrow(() -> new PlatformDataIntegrityException("error.msg.pae.required.document.not.found",
                        "PAE required document with id " + documentId + " not found", documentId));

        this.repository.delete(entity);
        this.repository.flush();

        return new CommandProcessingResultBuilder() //
                .withEntityId(documentId) //
                .build();
    }

    private void validateForCreate(JsonCommand command) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final Long categoryId = command.longValueOfParameterNamed("categoryId");
        if (categoryId == null || categoryId <= 0) {
            dataValidationErrors.add(ApiParameterError.parameterError("validation.msg.pae.required.document.categoryId.invalid",
                    "The categoryId parameter is required and must be greater than zero", "categoryId", categoryId));
        }

        final String documentName = command.stringValueOfParameterNamed("documentName");
        if (StringUtils.isBlank(documentName)) {
            dataValidationErrors.add(ApiParameterError.parameterError("validation.msg.pae.required.document.documentName.cannot.be.blank",
                    "The documentName parameter is required", "documentName", documentName));
        } else if (documentName.length() > 255) {
            dataValidationErrors
                    .add(ApiParameterError.parameterError("validation.msg.pae.required.document.documentName.exceeds.max.length",
                            "The documentName parameter exceeds max length of 255 characters", "documentName", documentName));
        }

        final String acceptedFormat = command.stringValueOfParameterNamed("acceptedFormat");
        if (acceptedFormat != null && acceptedFormat.length() > 100) {
            dataValidationErrors
                    .add(ApiParameterError.parameterError("validation.msg.pae.required.document.acceptedFormat.exceeds.max.length",
                            "The acceptedFormat parameter exceeds max length of 100 characters", "acceptedFormat", acceptedFormat));
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.pae.required.document.validation.errors.exist",
                    "Validation errors exist for creating PAE required document", dataValidationErrors);
        }
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        LOG.error("Error occurred while creating PAE required document", dve);
        throw new PlatformDataIntegrityException("error.msg.pae.required.document.unknown.data.integrity.issue",
                "Unknown data integrity issue with PAE required document resource: " + realCause.getMessage());
    }
}
