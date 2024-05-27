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
package org.apache.fineract.custom.infrastructure.channel.service;

import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.custom.infrastructure.channel.data.SubChannelData;
import org.apache.fineract.custom.infrastructure.channel.domain.SubChannel;
import org.apache.fineract.custom.infrastructure.channel.domain.SubChannelRepository;
import org.apache.fineract.custom.infrastructure.channel.exception.SubChannelNotFoundException;
import org.apache.fineract.custom.infrastructure.channel.mapper.SubChannelMapper;
import org.apache.fineract.custom.infrastructure.channel.validator.SubChannelDataValidator;
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
public class SubChannelReadWritePlatformServiceImpl implements SubChannelReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final SubChannelDataValidator validatorClass;
    private final PlatformSecurityContext context;

    @Autowired
    public SubChannelReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate, final DatabaseSpecificSQLGenerator sqlGenerator,
            final SubChannelDataValidator validatorClass, final PlatformSecurityContext context) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.validatorClass = validatorClass;
        this.context = context;
    }

    @Autowired
    private SubChannelRepository repository;

    @Override
    public List<SubChannelData> findAll(Long channelId) {
        return SubChannelMapper.toDTO(repository.findAllByChannelIdOrderByActiveDescNameAsc(channelId));
    }

    @Override
    public List<SubChannelData> findAllActive(Long channelId) {
        return SubChannelMapper.toDTO(repository.findAllByChannelIdAndActiveOrderByActiveDescNameAsc(channelId, true));
    }

    @Override
    public SubChannelData findById(Long id) {
        Optional<SubChannel> entity = repository.findById(id);
        if (!entity.isPresent()) {
            throw new SubChannelNotFoundException();
        }
        return SubChannelMapper.toDTO(entity.get());
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command, Long channelId) {

        try {
            this.context.authenticatedUser();

            final SubChannel entity = this.validatorClass.validateForCreate(command.json(), channelId);
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

        Optional<SubChannel> entity = repository.findById(id);
        if (entity.isPresent()) {
            entity.get().setActive(false);
            repository.saveAndFlush(entity.get());
        } else {
            throw new SubChannelNotFoundException();
        }

        return new CommandProcessingResultBuilder().withEntityId(id).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final JsonCommand command, Long channelId, Long id) {

        try {
            this.context.authenticatedUser();

            final SubChannel entity = this.validatorClass.validateForUpdate(command.json(), channelId, id);
            Optional<SubChannel> dbEntity = repository.findById(id);

            if (dbEntity.isPresent()) {
                entity.setId(id);
                repository.save(entity);
            } else {
                throw new SubChannelNotFoundException();
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
        throw new PlatformDataIntegrityException("error.msg.subchannel.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

}
