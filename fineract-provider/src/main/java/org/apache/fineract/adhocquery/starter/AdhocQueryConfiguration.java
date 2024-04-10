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
package org.apache.fineract.adhocquery.starter;

import org.apache.fineract.adhocquery.domain.AdHocRepository;
import org.apache.fineract.adhocquery.service.AdHocDataValidator;
import org.apache.fineract.adhocquery.service.AdHocReadPlatformService;
import org.apache.fineract.adhocquery.service.AdHocReadPlatformServiceImpl;
import org.apache.fineract.adhocquery.service.AdHocWritePlatformService;
import org.apache.fineract.adhocquery.service.AdHocWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AdhocQueryConfiguration {

    @Bean
    public AdHocReadPlatformServiceImpl.AdHocMapper adHocRowMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
        return new AdHocReadPlatformServiceImpl.AdHocMapper(sqlGenerator);
    }

    @Bean
    @ConditionalOnMissingBean(AdHocReadPlatformService.class)
    public AdHocReadPlatformService adHocReadPlatformService(JdbcTemplate jdbcTemplate, DatabaseSpecificSQLGenerator sqlGenerator,
            AdHocReadPlatformServiceImpl.AdHocMapper adHocRowMapper) {
        return new AdHocReadPlatformServiceImpl(jdbcTemplate, sqlGenerator, adHocRowMapper) {

        };
    }

    @Bean
    @ConditionalOnMissingBean(AdHocWritePlatformService.class)
    public AdHocWritePlatformService adHocWritePlatformService(PlatformSecurityContext context, AdHocRepository adHocRepository,
            AdHocDataValidator adHocCommandFromApiJsonDeserializer) {
        return new AdHocWritePlatformServiceJpaRepositoryImpl(context, adHocRepository, adHocCommandFromApiJsonDeserializer) {

        };
    }
}
