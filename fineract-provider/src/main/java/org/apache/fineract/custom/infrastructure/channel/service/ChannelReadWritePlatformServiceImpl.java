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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.custom.infrastructure.channel.data.ChannelData;
import org.apache.fineract.custom.infrastructure.channel.domain.Channel;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelRepository;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelType;
import org.apache.fineract.custom.infrastructure.channel.exception.ChannelNotFoundException;
import org.apache.fineract.custom.infrastructure.channel.mapper.ChannelMapper;
import org.apache.fineract.custom.infrastructure.channel.validator.ChannelDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ChannelReadWritePlatformServiceImpl implements ChannelReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final ChannelDataValidator validatorClass;
    private final PlatformSecurityContext context;
    private final ChannelRepository channelRepository;

    @Autowired
    public ChannelReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate, final ChannelDataValidator validatorClass,
            final PlatformSecurityContext context, ChannelRepository channelRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.validatorClass = validatorClass;
        this.context = context;
        this.channelRepository = channelRepository;
    }

    @Override
    public List<ChannelData> findAllActive() {
        return ChannelMapper.toDTO(channelRepository.findAllActive(true));
    }

    @Override
    public List<ChannelData> findBySearchParam(final SearchParameters searchParameters) {
        this.context.authenticatedUser();
        final List<Object> params = new ArrayList<>();
        final ChannelRowMapper rm = new ChannelRowMapper();
        String sql = "SELECT " + rm.schema();
        if (searchParameters != null) {
            String name = searchParameters.getName();
            final Long loanProductId = searchParameters.getProductId();
            final Integer channelType = searchParameters.getChannelType();
            final Boolean active = searchParameters.getActive();
            name = "%" + ObjectUtils.defaultIfNull(name, "") + "%";
            sql = sql + " WHERE (c.name LIKE ? OR c.hash LIKE ? OR c.description LIKE ?) ";
            params.add(name);
            params.add(name);
            params.add(name);
            if (loanProductId != null) {
                sql = sql + " AND mlpc.loan_product_id = ? ";
                params.add(loanProductId);
            }
            if (channelType != null) {
                sql = sql + " AND c.channel_type = ? ";
                params.add(channelType);
            }
            if (active) {
                sql = sql + " AND c.active = ? ";
                params.add(active);
            }
        }
        sql = sql + " GROUP BY c.id ORDER BY c.active desc, c.name";
        return this.jdbcTemplate.query(sql, rm, params.toArray());
    }

    @Override
    public ChannelData findByName(String name) {
        this.context.authenticatedUser();
        final ChannelRowMapper rm = new ChannelRowMapper();
        final String sql = "SELECT " + rm.schema() + " WHERE c.name = ? ";
        List<ChannelData> channelDataList = this.jdbcTemplate.query(sql, rm, new Object[] { name });
        if (channelDataList.isEmpty()) {
            return null;
        }
        return channelDataList.get(0);
    }

    @Override
    public ChannelData findById(Long id) {
        Optional<Channel> entity = channelRepository.findById(id);
        if (entity.isEmpty()) {
            throw new ChannelNotFoundException(id);
        }
        return ChannelMapper.toDTO(entity.get());
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command) {
        try {
            this.context.authenticatedUser();
            final Channel entity = this.validatorClass.validateForCreate(command.json());
            channelRepository.saveAndFlush(entity);
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
        Optional<Channel> entity = channelRepository.findById(id);
        if (entity.isPresent()) {
            entity.get().setActive(false);
            channelRepository.saveAndFlush(entity.get());
        } else {
            throw new ChannelNotFoundException(id);
        }
        return new CommandProcessingResultBuilder().withEntityId(id).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final JsonCommand command, Long channelId) {
        try {
            this.context.authenticatedUser();

            Optional<Channel> dbEntity = channelRepository.findById(channelId);
            final Channel entity;
            if (dbEntity.isPresent()) {
                entity = this.validatorClass.validateForUpdate(command.json(), dbEntity.get());
                entity.setId(channelId);
                channelRepository.save(entity);
            } else {
                throw new ChannelNotFoundException(channelId);
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

    private static final class ChannelRowMapper implements RowMapper<ChannelData> {

        public String schema() {
            return """
                    	c.id AS id,
                    	c.hash AS hash,
                    	c.name AS name,
                    	c.channel_type AS "channelType",
                    	c.description AS description,
                    	c.active AS active
                    FROM custom.c_channel c
                    LEFT JOIN m_loan_product_channel mlpc ON mlpc.channel_id = c.id
                    """;
        }

        @Override
        public ChannelData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            EnumOptionData channelType = ChannelType.fromInt(rs.getInt("channelType")).asEnumOptionData();
            return ChannelData.builder().id(rs.getLong("id")).hash(rs.getString("hash")).name(rs.getString("name"))
                    .description(rs.getString("description")).channelType(channelType).active(rs.getBoolean("active")).build();
        }
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        throw new PlatformDataIntegrityException("error.msg.channel.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }
}
