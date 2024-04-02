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
package org.apache.fineract.custom.portfolio.ally.service;

import static org.apache.fineract.custom.portfolio.ally.service.ClientAllyReadWritePlatformServiceImpl.STRING_CODEVALUE_DEPARTAMENTO;
import static org.apache.fineract.custom.portfolio.ally.service.ClientAllyReadWritePlatformServiceImpl.STRING_CODEVALUE_ESTADO;

import jakarta.persistence.PersistenceException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.custom.infrastructure.codes.service.CustomCodeValueReadPlatformService;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyPoibfOfSaleCodeValueData;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyPointOfSalesData;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSales;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSalesRepository;
import org.apache.fineract.custom.portfolio.ally.exception.ClientAllyPointOfSalesNotFoundException;
import org.apache.fineract.custom.portfolio.ally.mapper.ClientAllyPointOfSalesMapper;
import org.apache.fineract.custom.portfolio.ally.validator.ClientAllyPointOfSalesDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
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
public class ClientAllyPointOfSalesReadWritePlatformServiceImpl implements ClientAllyPointOfSalesReadWritePlatformService {

    public static final String STRING_CODEVALUE_MARCA = "Marca";
    public static final String STRING_CODEVALUE_CATEGORIA = "CategoriaPuntoDeVenta";
    public static final String STRING_CODEVALUE_SEGMENTO = "SegmentoPuntoDeVenta";
    public static final String STRING_CODEVALUE_TIPO = "TipoPuntoDeVenta";
    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final ClientAllyPointOfSalesDataValidator validatorClass;
    private final PlatformSecurityContext context;
    private final CustomCodeValueReadPlatformService customCodeValueReadPlatformService;

    @Autowired
    public ClientAllyPointOfSalesReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate,
            final DatabaseSpecificSQLGenerator sqlGenerator, final ClientAllyPointOfSalesDataValidator validatorClass,
            final PlatformSecurityContext context, final CustomCodeValueReadPlatformService customCodeValueReadPlatformService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.validatorClass = validatorClass;
        this.context = context;
        this.customCodeValueReadPlatformService = customCodeValueReadPlatformService;
    }

    @Autowired
    private ClientAllyPointOfSalesRepository repository;

    @Override
    public ClientAllyPoibfOfSaleCodeValueData getTemplateForInsertAndUpdate() {
        return ClientAllyPoibfOfSaleCodeValueData.builder()
                .brandsList(customCodeValueReadPlatformService.retrieveCodeValuesByCode(STRING_CODEVALUE_MARCA))
                .departmentsList(customCodeValueReadPlatformService.retrieveCodeValuesByCode(STRING_CODEVALUE_DEPARTAMENTO))
                .categoriesList(customCodeValueReadPlatformService.retrieveCodeValuesByCode(STRING_CODEVALUE_CATEGORIA))
                .segmentsList(customCodeValueReadPlatformService.retrieveCodeValuesByCode(STRING_CODEVALUE_SEGMENTO))
                .typesList(customCodeValueReadPlatformService.retrieveCodeValuesByCode(STRING_CODEVALUE_TIPO))
                .statesList(customCodeValueReadPlatformService.retrieveCodeValuesByCode(STRING_CODEVALUE_ESTADO)).build();
    }

    @Override
    public List<ClientAllyPointOfSalesData> findAllActive() {
        return ClientAllyPointOfSalesMapper.toDTO(repository.findAll());
    }

    @Override
    public List<ClientAllyPointOfSalesData> findByName(Long parentId, String name) {
        this.context.authenticatedUser();
        final ClientAllyPointOfSalesRowMapper rm = new ClientAllyPointOfSalesRowMapper();

        name = "%" + (Objects.isNull(name) ? "" : name) + "%";

        final String sql = "SELECT " + rm.schema() + " WHERE cca.client_ally_id = ? AND (cca.name LIKE ? OR cca.code LIKE ?)"
                + " ORDER BY code, name, stateDescription";

        return this.jdbcTemplate.query(sql, rm, new Object[] { parentId, name, name });
    }

    @Override
    public ClientAllyPointOfSalesData findById(Long id) {
        Optional<ClientAllyPointOfSales> entity = repository.findById(id);
        if (!entity.isPresent()) {
            throw new ClientAllyPointOfSalesNotFoundException();
        }
        return ClientAllyPointOfSalesMapper.toDTO(entity.get());
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command, Long clientAllyId) {

        try {
            this.context.authenticatedUser();

            final ClientAllyPointOfSales entity = this.validatorClass.validateForCreate(command.json());
            entity.setClientAllyId(clientAllyId);
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

        Optional<ClientAllyPointOfSales> entity = repository.findById(id);
        if (entity.isPresent()) {
            repository.delete(entity.get());
            repository.flush();
        } else {
            throw new ClientAllyPointOfSalesNotFoundException();
        }

        return new CommandProcessingResultBuilder().withEntityId(id).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final JsonCommand command, Long clientAllyId, Long id) {

        try {
            this.context.authenticatedUser();

            final ClientAllyPointOfSales entity = this.validatorClass.validateForUpdate(command.json());
            entity.setClientAllyId(clientAllyId);
            Optional<ClientAllyPointOfSales> dbEntity = repository.findById(id);

            if (dbEntity.isPresent()) {
                entity.setId(id);
                repository.save(entity);
            } else {
                throw new ClientAllyPointOfSalesNotFoundException();
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
        throw new PlatformDataIntegrityException("error.msg.clientallypointofsales.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private static final class ClientAllyPointOfSalesRowMapper implements RowMapper<ClientAllyPointOfSalesData> {

        public String schema() {
            return "    mcv_brand.code_value               as brandDescription,"
                    + "    mcv_city.code_value                as cityDescription,"
                    + "    mcv_department.code_value          as departmentDescription,"
                    + "    mcv_category.code_value            as categoryDescription,"
                    + "    mcv_segment.code_value             as segmentDescription,"
                    + "    mcv_type.code_value                as typeDescription,"
                    + "    mcv_state_id.code_value            as stateDescription," + "    cca.* " + "from "
                    + "    custom.c_client_ally_point_of_sales cca "
                    + "    left join public.m_code_value mcv_city on  mcv_city.id = cca.city_id  "
                    + "    left join public.m_code_value mcv_department on  mcv_department.id = cca.department_id  "
                    + "    left join public.m_code_value mcv_brand on  mcv_brand.id = cca.brand_id "
                    + "    left join public.m_code_value mcv_category on  mcv_category.id = cca.category_id "
                    + "    left join public.m_code_value mcv_segment on  mcv_segment.id = cca.segment_id"
                    + "    left join public.m_code_value mcv_type on  mcv_type.id = cca.type_id "
                    + "    left join public.m_code_value mcv_state_id on  mcv_state_id.id = cca.state_id ";
        }

        @Override
        public ClientAllyPointOfSalesData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            return ClientAllyPointOfSalesData.builder().id(rs.getLong("id")).clientAllyId(rs.getLong("client_ally_id"))
                    .name(rs.getString("name")).code(rs.getString("code")).brandCodeValueId(rs.getLong("brand_id"))
                    .brandCodeValueDescription(rs.getString("brandDescription")).cityCodeValueId(rs.getLong("city_id"))
                    .cityCodeValueDescription(rs.getString("cityDescription")).departmentCodeValueId(rs.getLong("department_id"))
                    .departmentCodeValueDescription(rs.getString("departmentDescription")).categoryCodeValueId(rs.getLong("category_id"))
                    .categoryCodeValueDescription(rs.getString("categoryDescription")).segmentCodeValueId(rs.getLong("segment_id"))
                    .segmentCodeValueDescription(rs.getString("segmentDescription")).typeCodeValueId(rs.getLong("type_id"))
                    .typeCodeValueDescription(rs.getString("typeDescription")).settledComission(rs.getBigDecimal("settled_comission"))
                    .buyEnabled(rs.getBoolean("buy_enabled")).collectionEnabled(rs.getBoolean("collection_enabled"))
                    .stateCodeValueId(rs.getLong("state_id")).stateCodeValueDescription(rs.getString("stateDescription")).build();
        }
    }
}
