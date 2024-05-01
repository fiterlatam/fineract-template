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

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeEntityData;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeEntity;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeEntityRepository;
import org.apache.fineract.custom.portfolio.customcharge.exception.CustomChargeEntityNotFoundException;
import org.apache.fineract.custom.portfolio.customcharge.mapper.CustomChargeEntityMapper;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomChargeEntityReadWritePlatformServiceImpl implements CustomChargeEntityReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;

    @Autowired
    public CustomChargeEntityReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate, final DatabaseSpecificSQLGenerator sqlGenerator,
            final PlatformSecurityContext context) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.context = context;
    }

    @Autowired
    private CustomChargeEntityRepository repository;

    @Override
    public List<CustomChargeEntityData> findAllActive() {
        return CustomChargeEntityMapper.toDTO(repository.findAll());
    }

    @Override
    public CustomChargeEntityData findById(Long id) {
        Optional<CustomChargeEntity> entity = repository.findById(id);
        if (!entity.isPresent()) {
            throw new CustomChargeEntityNotFoundException();
        }
        return CustomChargeEntityMapper.toDTO(entity.get());
    }
}
