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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeTypeMapData;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeTypeMap;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeTypeMapRepository;
import org.apache.fineract.custom.portfolio.customcharge.exception.CustomChargeTypeMapNotFoundException;
import org.apache.fineract.custom.portfolio.customcharge.mapper.CustomChargeTypeMapMapper;
import org.apache.fineract.custom.portfolio.customcharge.validator.CustomChargeTypeMapDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
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
public class CustomChargeTypeMapReadWritePlatformServiceImpl implements CustomChargeTypeMapReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final CustomChargeTypeMapDataValidator validatorClass;
    private final PlatformSecurityContext context;

    @Autowired
    public CustomChargeTypeMapReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate, final DatabaseSpecificSQLGenerator sqlGenerator,
            final CustomChargeTypeMapDataValidator validatorClass, final PlatformSecurityContext context) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.validatorClass = validatorClass;
        this.context = context;
    }

    @Autowired
    private CustomChargeTypeMapRepository repository;

    @Override
    public List<CustomChargeTypeMapData> findAllActive(Long customChargeTypeId) {
        return CustomChargeTypeMapMapper.toDTO(repository.findByCustomChargeTypeIdAndActive(customChargeTypeId, true).stream()
                .sorted(Comparator.comparing(CustomChargeTypeMap::getTerm)).collect(Collectors.toList()));
    }

    @Override
    public CustomChargeTypeMapData findById(Long id) {
        Optional<CustomChargeTypeMap> entity = repository.findById(id);
        if (!entity.isPresent()) {
            throw new CustomChargeTypeMapNotFoundException();
        }
        return CustomChargeTypeMapMapper.toDTO(entity.get());
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command, Long customChargeTypeId) {

        try {
            this.context.authenticatedUser();

            final CustomChargeTypeMap entity = this.validatorClass.validateForCreate(command.json(), customChargeTypeId);
            entity.setCustomChargeTypeId(customChargeTypeId);

            List<CustomChargeTypeMap> customChargeTypeMapList = repository.findByCustomChargeTypeIdAndTermAndActive(customChargeTypeId,
                    entity.getTerm(), true);

            // From Date must be bigger than on last active from date
            // Consider the case of not having a previous record
            Optional<CustomChargeTypeMap> lastActive = customChargeTypeMapList.stream().filter(active -> active.getActive()).findFirst();

            if (lastActive.isPresent()) {
                CustomChargeTypeMap existent = lastActive.get();

                // If date is before the last active date
                if (entity.getValidFrom().isBefore(existent.getValidFrom())) {
                    throw new PlatformDataIntegrityException("error.msg.from.date.must.be.after.last.active.from.date",
                            "From Date must be after last active From Date", "fromDate", entity.getValidFrom());
                }
            }

            // Not updating! ETF!!!
            int rowsAffected = repository.deactivatePreviousTermData(customChargeTypeId, entity.getTerm(),
                    entity.getValidFrom().minusDays(1), DateUtils.getLocalDateTimeOfTenant(), this.context.authenticatedUser().getId());

            repository.saveAndFlush(entity);

            return new CommandProcessingResultBuilder().withEntityId(customChargeTypeId).withSubEntityId(entity.getId()).build();
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

        Optional<CustomChargeTypeMap> entity = repository.findById(id);
        if (entity.isPresent()) {
            CustomChargeTypeMap currEntity = entity.get();

            // It is only allowed to delete the last active record
            if (Boolean.FALSE.equals(currEntity.getActive())) {
                throw new PlatformDataIntegrityException("error.msg.cannot.delete.not.last.active.record",
                        "Cannot delete a record that is not the last active record", "id", id);
            }

            // Only allowe4d to delete validFrom date in the future
            if (currEntity.getValidFrom().isBefore(DateUtils.getLocalDateOfTenant())) {
                throw new PlatformDataIntegrityException("error.msg.cannot.delete.record.with.from.date.in.the.past",
                        "Cannot delete a record with From Date in the past", "id", id);
            }

            // Revert last record, if any, to active status
            Optional<CustomChargeTypeMap> customChargeTypeMapList = repository
                    .findByCustomChargeTypeIdAndTermAndActive(currEntity.getCustomChargeTypeId(), currEntity.getTerm(), false).stream()
                    .sorted(Comparator.comparing(CustomChargeTypeMap::getId).reversed()).findFirst();

            customChargeTypeMapList.ifPresent(customChargeTypeMap -> {
                customChargeTypeMap.setActive(true);
                customChargeTypeMap.setValidTo(null);
                customChargeTypeMap.setUpdatedAt(DateUtils.getLocalDateTimeOfTenant());
                customChargeTypeMap.setUpdatedBy(this.context.authenticatedUser().getId());

                repository.save(customChargeTypeMap);
            });

            repository.delete(entity.get());
            repository.flush();
        } else {
            throw new CustomChargeTypeMapNotFoundException();
        }

        return new CommandProcessingResultBuilder().withEntityId(id).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final JsonCommand command, Long id, Long customChargeTypeId) {

        try {
            this.context.authenticatedUser();

            final CustomChargeTypeMap entity = this.validatorClass.validateForUpdate(command.json(), customChargeTypeId);
            Optional<CustomChargeTypeMap> dbEntity = repository.findById(id);

            if (dbEntity.isPresent()) {

                // It is only allowed to delete the last active record
                if (Boolean.FALSE.equals(dbEntity.get().getActive())) {
                    throw new PlatformDataIntegrityException("error.msg.cannot.update.not.last.active.record",
                            "Cannot update a record that is not the last active record", "id", id);
                }

                // Only allowe4d to delete validFrom date in the future
                if (dbEntity.get().getValidFrom().isBefore(DateUtils.getLocalDateOfTenant())) {
                    throw new PlatformDataIntegrityException("error.msg.cannot.update.record.with.from.date.in.the.past",
                            "Cannot update a record with From Date in the past", "id", id);
                }

                entity.setId(id);
                entity.setCreatedAt(dbEntity.get().getCreatedAt());
                entity.setCreatedBy(dbEntity.get().getCreatedBy());
                repository.save(entity);
            } else {
                throw new CustomChargeTypeMapNotFoundException();
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
        throw new PlatformDataIntegrityException("error.msg.customchargetypemap.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }
}
