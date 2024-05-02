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
package org.apache.fineract.custom.portfolio.customcharge.service;

import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeTypeData;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeType;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeTypeRepository;
import org.apache.fineract.custom.portfolio.customcharge.exception.CustomChargeTypeNotFoundException;
import org.apache.fineract.custom.portfolio.customcharge.mapper.CustomChargeTypeMapper;
import org.apache.fineract.custom.portfolio.customcharge.validator.CustomChargeTypeDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CustomChargeTypeReadWritePlatformServiceImpl implements CustomChargeTypeReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final CustomChargeTypeDataValidator validatorClass;
    private final PlatformSecurityContext context;

    @Autowired
    public CustomChargeTypeReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate, final DatabaseSpecificSQLGenerator sqlGenerator,
            final CustomChargeTypeDataValidator validatorClass, final PlatformSecurityContext context) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.validatorClass = validatorClass;
        this.context = context;
    }

    @Autowired
    private CustomChargeTypeRepository repository;

    @Override
    public List<CustomChargeTypeData> findAllActive() {
        return CustomChargeTypeMapper.toDTO(repository.findAll());
    }

    @Override
    public List<CustomChargeTypeData> findAllByEntityId(Long chargeEntityId) {
        return CustomChargeTypeMapper.toDTO(repository.findByCustomChargeEntityId(chargeEntityId));
    }

    @Override
    public CustomChargeTypeData findById(Long id) {
        Optional<CustomChargeType> entity = repository.findById(id);
        if (!entity.isPresent()) {
            throw new CustomChargeTypeNotFoundException();
        }
        return CustomChargeTypeMapper.toDTO(entity.get());
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command, final Long customChargeTypeId) {

        try {
            this.context.authenticatedUser();

            final CustomChargeType entity = this.validatorClass.validateForCreate(command.json(), customChargeTypeId);
            repository.saveAndFlush(entity);

            return new CommandProcessingResultBuilder().withEntityId(entity.getId()).build();
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
    public CommandProcessingResult delete(final Long id) {
        this.context.authenticatedUser();

        Optional<CustomChargeType> entity = repository.findById(id);
        if (entity.isPresent()) {
            repository.delete(entity.get());
            repository.flush();
        } else {
            throw new CustomChargeTypeNotFoundException();
        }

        return new CommandProcessingResultBuilder().withEntityId(id).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final JsonCommand command, Long id) {

        try {
            this.context.authenticatedUser();

            final CustomChargeType entity = this.validatorClass.validateForUpdate(command.json());
            Optional<CustomChargeType> dbEntity = repository.findById(id);

            if (dbEntity.isPresent()) {
                entity.setId(id);
                repository.save(entity);
            } else {
                throw new CustomChargeTypeNotFoundException();
            }

            return new CommandProcessingResultBuilder().withEntityId(entity.getId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        throw new PlatformDataIntegrityException("error.msg.customchargetype.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }
}
