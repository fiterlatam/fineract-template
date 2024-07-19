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

import jakarta.persistence.PersistenceException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.custom.infrastructure.codes.service.CustomCodeValueReadPlatformService;
import org.apache.fineract.custom.portfolio.ally.api.ClientAllyPointOfSalesApiConstants;
import org.apache.fineract.custom.portfolio.ally.data.CityCodeValueData;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyCodeValueData;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyData;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyPointOfSalesData;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAlly;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSales;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSalesRepository;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyRepository;
import org.apache.fineract.custom.portfolio.ally.exception.ClientAllyNotFoundException;
import org.apache.fineract.custom.portfolio.ally.mapper.ClientAllyMapper;
import org.apache.fineract.custom.portfolio.ally.validator.ClientAllyDataValidator;
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

@Service
@Slf4j
public class ClientAllyReadWritePlatformServiceImpl implements ClientAllyReadWritePlatformService {

    public static final String STRING_CODEVALUE_DEPARTAMENTO = "Departamento";
    public static final String STRING_CODEVALUE_CIUDAD = "Ciudad";
    public static final String STRING_CODEVALUE_FRECUENCIA_LIQUIDACION = "FrecuenciaLiquidacion";
    public static final String STRING_CODEVALUE_ENTIDAD_BANCARIA = "EntidadBancaria";
    public static final String STRING_CODEVALUE_TIPO_CUENTA_BANCARIA = "TipoCuentaBancaria";
    public static final String STRING_CODEVALUE_PERFIL_TRIBUTARIO_REGIMEN_IVA = "PerfilTributarioRegimenIVA";
    public static final String STRING_CODEVALUE_ESTADO = "Estado";

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final ClientAllyDataValidator validatorClass;
    private final PlatformSecurityContext context;
    private final CustomCodeValueReadPlatformService customCodeValueReadPlatformService;
    @Autowired
    private ClientAllyPointOfSalesRepository repositoryPointofSale;

    @Autowired
    public ClientAllyReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate, final DatabaseSpecificSQLGenerator sqlGenerator,
            final PlatformSecurityContext context, final CustomCodeValueReadPlatformService customCodeValueReadPlatformService,
            final ClientAllyDataValidator validatorClass) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.validatorClass = validatorClass;
        this.context = context;
        this.customCodeValueReadPlatformService = customCodeValueReadPlatformService;
    }

    @Autowired
    private ClientAllyRepository repository;

    @Override
    public List<ClientAllyData> findAllActive() {
        return ClientAllyMapper.toDTO(repository.findAll());
    }

    @Override
    public List<ClientAllyData> findByName(String name) {
        this.context.authenticatedUser();
        final ClientAllyRowMapper rm = new ClientAllyRowMapper();

        name = "%" + (Objects.isNull(name) ? "" : name) + "%";

        final String sql = "SELECT " + rm.schema() + " WHERE cca.company_name LIKE ? OR cca.nit LIKE ?"
                + " ORDER BY company_name, stateDescription";

        return this.jdbcTemplate.query(sql, rm, new Object[] { name, name });
    }

    @Override
    public List<ClientAllyData> retrieveAlliesWithPointOfSales() {
        final String sql = """
                    SELECT cca.id AS "clientAllyId",
                    cca.company_name AS "companyName",
                    cca.nit AS nit,
                    ccapos.id AS "pointOfSaleId",
                    ccapos."name" AS "pointOfSaleName",
                    ccapos.code AS "pointOfSaleCode"
                    FROM custom.c_client_ally cca
                    INNER JOIN custom.c_client_ally_point_of_sales ccapos ON ccapos.client_ally_id = cca.id\s
                    ORDER BY cca.id
                """;
        final List<ClientAllyData> clientAllies = jdbcTemplate.query(sql, resultSet -> {
            final List<ClientAllyData> clientAllyDataList = new ArrayList<>();
            final Map<Long, ClientAllyData> clientAllyMap = new HashMap<>();
            while (resultSet.next()) {
                Long clientAllyId = resultSet.getLong("clientAllyId");
                ClientAllyData clientAllyData = clientAllyMap.get(clientAllyId);
                if (clientAllyData == null) {
                    clientAllyData = ClientAllyData.builder().id(clientAllyId).companyName(resultSet.getString("companyName"))
                            .nit(resultSet.getString("nit")).pointOfSales(new ArrayList<>()).build();
                    clientAllyMap.put(clientAllyId, clientAllyData);
                    clientAllyDataList.add(clientAllyData);
                }
                final long pointOfSaleId = resultSet.getLong("pointOfSaleId");
                if (pointOfSaleId > 0) {
                    ClientAllyPointOfSalesData clientAllyPointOfSalesData = ClientAllyPointOfSalesData.builder().id(pointOfSaleId)
                            .name(resultSet.getString("pointOfSaleName")).code(resultSet.getString("pointOfSaleCode")).build();
                    clientAllyData.getPointOfSales().add(clientAllyPointOfSalesData);
                }
            }
            return clientAllyDataList;
        }, new Object[] {});
        if (CollectionUtils.isNotEmpty(clientAllies)) {
            clientAllies.sort((c1, c2) -> Integer.compare(c2.getPointOfSales().size(), c1.getPointOfSales().size()));
        }
        return clientAllies;
    }

    @Override
    public ClientAllyCodeValueData getTemplateForInsertAndUpdate() {
        return ClientAllyCodeValueData.builder()
                .departmentsList(customCodeValueReadPlatformService.retrieveCodeValuesByCode(STRING_CODEVALUE_DEPARTAMENTO))
                .liquidationFrequencyList(
                        customCodeValueReadPlatformService.retrieveCodeValuesByCode(STRING_CODEVALUE_FRECUENCIA_LIQUIDACION))
                .bankEntitiesList(customCodeValueReadPlatformService.retrieveCodeValuesByCode(STRING_CODEVALUE_ENTIDAD_BANCARIA))
                .accountTypesList(customCodeValueReadPlatformService.retrieveCodeValuesByCode(STRING_CODEVALUE_TIPO_CUENTA_BANCARIA))
                .taxProfilesList(
                        customCodeValueReadPlatformService.retrieveCodeValuesByCode(STRING_CODEVALUE_PERFIL_TRIBUTARIO_REGIMEN_IVA))
                .statesList(customCodeValueReadPlatformService.retrieveCodeValuesByCode(STRING_CODEVALUE_ESTADO)).build();
    }

    @Override
    public CityCodeValueData getCitiesByDepartment(Long departmentId) {
        return CityCodeValueData.builder()
                .citiesList(customCodeValueReadPlatformService.retrieveCodeValuesByCodeAndParent(STRING_CODEVALUE_CIUDAD, departmentId))
                .build();
    }

    @Override
    public ClientAllyData findById(Long id) {
        Optional<ClientAlly> entity = repository.findById(id);
        if (!entity.isPresent()) {
            throw new ClientAllyNotFoundException();
        }
        return ClientAllyMapper.toDTO(entity.get());
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            final ClientAlly entity = this.validatorClass.validateForCreate(command.json());
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

        Optional<ClientAlly> entity = repository.findById(id);
        if (entity.isPresent()) {
            repository.delete(entity.get());
            repository.flush();
        } else {
            throw new ClientAllyNotFoundException();
        }

        return new CommandProcessingResultBuilder().withEntityId(id).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final JsonCommand command, Long id) {

        try {
            this.context.authenticatedUser();
            final ClientAlly entity = this.validatorClass.validateForUpdate(command.json());
            Optional<ClientAlly> dbEntity = repository.findById(id);
            final Long stateCodeValueId = command.longValueOfParameterNamed("stateCodeValueId");
            if (dbEntity.isPresent()) {
                if (stateCodeValueId == ClientAllyPointOfSalesApiConstants.stateCodeValueInavtiveParamName.longValue()) {
                    final Long parentId = id;
                    List<ClientAllyPointOfSales> clientAllyPointOfSalesDataList = repositoryPointofSale
                            .findAllPointOfSalesByAllyId(parentId);
                    if (!clientAllyPointOfSalesDataList.isEmpty()) {
                        for (ClientAllyPointOfSales clientofsale : clientAllyPointOfSalesDataList) {
                            if (clientofsale.getStateCodeValueId() != stateCodeValueId) {
                                clientofsale.setStateCodeValueId(stateCodeValueId);
                                repositoryPointofSale.saveAndFlush(clientofsale);
                            }
                        }
                    }
                }
                entity.setId(id);
                repository.save(entity);
            } else {
                throw new ClientAllyNotFoundException();
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
        throw new PlatformDataIntegrityException("error.msg.clientally.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private static final class ClientAllyRowMapper implements RowMapper<ClientAllyData> {

        public String schema() {
            return "     mcv_city.code_value                as cityDescription,"
                    + "    mcv_department.code_value          as departmentDescription,"
                    + "    mcv_liquidation_freq.code_value    as liquidationFrequencyDescription,"
                    + "    mcv_bank_entity_id.code_value      as bankEntityDescription,"
                    + "    mcv_account_type_id.code_value     as accountTypeDescription,"
                    + "    mcv_tax_profile_id.code_value      as taxProfileDescription,"
                    + "    mcv_state_id.code_value            as stateDescription,"
                    + "    (SELECT COUNT(1) FROM custom.c_client_ally_point_of_sales WHERE client_ally_id = cca.id) as pointOfSalesCounter,"
                    + "    cca.* " + "from " + "    custom.c_client_ally cca "
                    + "    left join public.m_code_value mcv_city on  mcv_city.id = cca.city_id  "
                    + "    left join public.m_code_value mcv_department on  mcv_department.id = cca.department_id  "
                    + "    left join public.m_code_value mcv_liquidation_freq on  mcv_liquidation_freq.id = cca.liquidation_frequency_id "
                    + "    left join public.m_code_value mcv_bank_entity_id on  mcv_bank_entity_id.id = cca.bank_entity_id "
                    + "    left join public.m_code_value mcv_account_type_id on  mcv_account_type_id.id = cca.account_type_id"
                    + "    left join public.m_code_value mcv_tax_profile_id on  mcv_tax_profile_id.id = cca.tax_profile_id "
                    + "    left join public.m_code_value mcv_state_id on  mcv_state_id.id = cca.state_id ";
        }

        @Override
        public ClientAllyData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            return ClientAllyData.builder().id(rs.getLong("id")).companyName(rs.getString("company_name")).nit(rs.getString("nit"))
                    .nitDigit(rs.getInt("nit_digit")).address(rs.getString("address")).cityCodeValueId(rs.getLong("city_id"))
                    .cityCodeValueDescription(rs.getString("cityDescription")).departmentCodeValueId(rs.getLong("department_id"))
                    .departmentCodeValueDescription(rs.getString("departmentDescription"))
                    .liquidationFrequencyCodeValueId(rs.getLong("liquidation_frequency_id"))
                    .liquidationFrequencyCodeValueDescription(rs.getString("liquidationFrequencyDescription"))
                    .applyCupoMaxSell(rs.getBoolean("apply_cupo_max_sell")).cupoMaxSell(rs.getInt("cupo_max_sell"))
                    .settledComission(rs.getBigDecimal("settled_comission")).buyEnabled(rs.getBoolean("buy_enabled"))
                    .collectionEnabled(rs.getBoolean("collection_enabled")).bankEntityCodeValueId(rs.getLong("bank_entity_id"))
                    .bankEntityCodeValueDescription(rs.getString("bankEntityDescription"))
                    .accountTypeCodeValueId(rs.getLong("account_type_id"))
                    .accountTypeCodeValueDescription(rs.getString("accountTypeDescription")).accountNumber(rs.getString("account_number"))
                    .taxProfileCodeValueId(rs.getLong("tax_profile_id"))
                    .taxProfileCodeValueDescription(rs.getString("taxProfileDescription")).stateCodeValueId(rs.getLong("state_id"))
                    .stateCodeValueDescription(rs.getString("stateDescription")).pointOfSalesCounter(rs.getInt("pointOfSalesCounter"))
                    .build();
        }
    }

}
