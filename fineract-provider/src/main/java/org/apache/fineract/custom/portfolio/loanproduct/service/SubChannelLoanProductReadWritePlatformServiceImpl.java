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
package org.apache.fineract.custom.portfolio.loanproduct.service;

import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.custom.portfolio.loanproduct.data.SubChannelLoanProductData;
import org.apache.fineract.custom.portfolio.loanproduct.domain.SubChannelLoanProduct;
import org.apache.fineract.custom.portfolio.loanproduct.domain.SubChannelLoanProductRepository;
import org.apache.fineract.custom.portfolio.loanproduct.exception.SubChannelLoanProductNotFoundException;
import org.apache.fineract.custom.portfolio.loanproduct.mapper.SubChannelLoanProductMapper;
import org.apache.fineract.custom.portfolio.loanproduct.validator.SubChannelLoanProductDataValidator;
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
public class SubChannelLoanProductReadWritePlatformServiceImpl implements SubChannelLoanProductReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final SubChannelLoanProductDataValidator validatorClass;
    private final PlatformSecurityContext context;

    @Autowired
    public SubChannelLoanProductReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate,
            final DatabaseSpecificSQLGenerator sqlGenerator, final SubChannelLoanProductDataValidator validatorClass,
            final PlatformSecurityContext context) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.validatorClass = validatorClass;
        this.context = context;
    }

    @Autowired
    private SubChannelLoanProductRepository repository;

    @Override
    public List<SubChannelLoanProductData> findAllActive() {
        return SubChannelLoanProductMapper.toDTO(repository.findAll());
    }

    @Override
    public List<SubChannelLoanProductData> findAllByProductId(Long productId) {
        return SubChannelLoanProductMapper.toDTO(repository.findByLoanProductId(productId));
    }

    @Override
    public SubChannelLoanProductData findById(Long id) {
        Optional<SubChannelLoanProduct> entity = repository.findById(id);
        if (!entity.isPresent()) {
            throw new SubChannelLoanProductNotFoundException();
        }
        return SubChannelLoanProductMapper.toDTO(entity.get());
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            final SubChannelLoanProduct entity = this.validatorClass.validateForCreate(command.json());
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

        Optional<SubChannelLoanProduct> entity = repository.findById(id);
        if (entity.isPresent()) {
            repository.delete(entity.get());
            repository.flush();
        } else {
            throw new SubChannelLoanProductNotFoundException();
        }

        return new CommandProcessingResultBuilder().withEntityId(id).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final JsonCommand command, Long id) {

        try {
            this.context.authenticatedUser();

            final SubChannelLoanProduct entity = this.validatorClass.validateForUpdate(command.json());
            Optional<SubChannelLoanProduct> dbEntity = repository.findById(id);

            if (dbEntity.isPresent()) {
                entity.setId(id);
                repository.save(entity);
            } else {
                throw new SubChannelLoanProductNotFoundException();
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
        throw new PlatformDataIntegrityException("error.msg.subchannelloanproduct.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }
}
