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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSales;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSalesRepository;
import org.apache.fineract.custom.portfolio.ally.exception.ClientAllyPointOfSalesNotFoundException;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeTypeMapData;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeTypeMap;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeTypeMapRepository;
import org.apache.fineract.custom.portfolio.customcharge.exception.CustomChargeTypeMapNotFoundException;
import org.apache.fineract.custom.portfolio.customcharge.mapper.CustomChargeTypeMapMapper;
import org.apache.fineract.custom.portfolio.customcharge.validator.CustomChargeTypeMapDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.data.PointOfSalesData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CustomChargeTypeMapReadWritePlatformServiceImpl implements CustomChargeTypeMapReadWritePlatformService {

    private final CustomChargeTypeMapDataValidator validatorClass;
    private final PlatformSecurityContext context;
    private final CustomChargeTypeMapRepository customChargeTypeMapRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository;

    @Autowired
    public CustomChargeTypeMapReadWritePlatformServiceImpl(final CustomChargeTypeMapDataValidator validatorClass,
            final PlatformSecurityContext context, CustomChargeTypeMapRepository customChargeTypeMapRepository, JdbcTemplate jdbcTemplate,
            ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository) {
        this.validatorClass = validatorClass;
        this.context = context;
        this.customChargeTypeMapRepository = customChargeTypeMapRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.clientAllyPointOfSalesRepository = clientAllyPointOfSalesRepository;
    }

    @Override
    public List<CustomChargeTypeMapData> findAllActive(Long customChargeTypeId) {
        return CustomChargeTypeMapMapper
                .toDTO(this.customChargeTypeMapRepository.findByCustomChargeTypeIdAndActive(customChargeTypeId, true).stream()
                        .sorted(Comparator.comparing(CustomChargeTypeMap::getTerm)).collect(Collectors.toList()));
    }

    @Override
    public CustomChargeTypeMapData findById(Long id) {
        final ChargeTypeMapRowMapper rm = new ChargeTypeMapRowMapper();
        final String sql = "SELECT " + rm.schema() + " WHERE ctm.id = ? ";
        final CustomChargeTypeMapData customChargeTypeMapData = this.jdbcTemplate.queryForObject(sql, rm, new Object[] { id });
        final List<PointOfSalesData> pointOfSales = retrievePointOfSales(id);
        List<ClientData> clients = retrieveClients(id);
        if (customChargeTypeMapData != null) {
            customChargeTypeMapData.setPointOfSales(pointOfSales);
            customChargeTypeMapData.setClients(clients);
        }
        return customChargeTypeMapData;
    }

    public List<PointOfSalesData> retrievePointOfSales(Long chargeMapId) {
        final String pointOfSaleSQL = """
                  SELECT ccapos.id, ccapos.name, ccapos.code
                  FROM custom.c_custom_charge_type_map ccctm
                  INNER JOIN custom.c_charge_map_point_sale ccmps ON ccmps.custom_charge_id = ccctm.id
                  INNER JOIN custom.c_client_ally_point_of_sales ccapos ON ccapos.id = ccmps.point_of_sales_id\s
                  WHERE ccctm.id = ?
                """;
        return jdbcTemplate.query(pointOfSaleSQL, resultSet -> {
            List<PointOfSalesData> pointOfSalesDataList = new ArrayList<>();
            while (resultSet.next()) {
                final Long pointOfSaleId = resultSet.getLong("id");
                final String name = resultSet.getString("name");
                final String code = resultSet.getString("code");
                PointOfSalesData pointOfSalesData = PointOfSalesData.instance(pointOfSaleId, name, code, null);
                pointOfSalesDataList.add(pointOfSalesData);
            }
            return pointOfSalesDataList;
        }, chargeMapId);
    }

    public List<ClientData> retrieveClients(Long chargeMapId) {
        final String pointOfSaleSQL = """
                  SELECT mc.id AS clientId,
                  mc.display_name AS name,
                  COALESCE(cce."NIT", ccp."Cedula") AS idNumber
                  FROM custom.c_custom_charge_type_map ccctm
                  INNER JOIN custom.c_custom_charge_map_client cccmc ON cccmc.custom_charge_map_id = ccctm.id
                  INNER JOIN m_client mc ON mc.id = cccmc.client_id
                  LEFT JOIN campos_cliente_empresas cce ON cce.client_id = mc.id
                  LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                  WHERE ccctm.id = ?
                """;
        return jdbcTemplate.query(pointOfSaleSQL, resultSet -> {
            List<ClientData> clientDataList = new ArrayList<>();
            while (resultSet.next()) {
                final Long clientId = resultSet.getLong("clientId");
                final String name = resultSet.getString("name");
                final String idNumber = resultSet.getString("idNumber");
                final ClientData clientData = new ClientData();
                clientData.setId(clientId);
                clientData.setDisplayName(name);
                clientData.setIdNumber(idNumber);
                clientDataList.add(clientData);
            }
            return clientDataList;
        }, chargeMapId);
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command) {

        try {
            this.context.authenticatedUser();
            final CustomChargeTypeMap entity = this.validatorClass.validateForCreate(command.json());
            final Long customChargeTypeId = entity.getCustomChargeTypeId();
            List<CustomChargeTypeMap> customChargeTypeMapList = this.customChargeTypeMapRepository
                    .findByCustomChargeTypeIdAndTermAndActive(customChargeTypeId, entity.getTerm(), true);
            // From Date must be bigger than on last active from date
            // Consider the case of not having a previous record
            Optional<CustomChargeTypeMap> lastActive = customChargeTypeMapList.stream().filter(CustomChargeTypeMap::getActive).findFirst();
            if (lastActive.isPresent()) {
                CustomChargeTypeMap existent = lastActive.get();
                // If date is before the last active date
                if (entity.getValidFrom().isBefore(existent.getValidFrom())) {
                    throw new PlatformDataIntegrityException("error.msg.from.date.must.be.after.last.active.from.date",
                            "From Date must be after last active From Date", "fromDate", entity.getValidFrom());
                }
            }
            final List<ClientAllyPointOfSales> clientAllyPointOfSaleList = assembleListOfPointOfSales(command);
            if (CollectionUtils.isNotEmpty(clientAllyPointOfSaleList)) {
                entity.setClientAllyPointOfSales(clientAllyPointOfSaleList);
            }
            // Not updating! ETF!!!
            this.customChargeTypeMapRepository.deactivatePreviousTermData(customChargeTypeId, entity.getTerm(),
                    entity.getValidFrom().minusDays(1), DateUtils.getLocalDateTimeOfTenant(), this.context.authenticatedUser().getId());
            this.customChargeTypeMapRepository.saveAndFlush(entity);
            return new CommandProcessingResultBuilder().withEntityId(entity.getId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(dve.getMostSpecificCause());
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(throwable);
            return CommandProcessingResult.empty();
        }
    }

    private List<ClientAllyPointOfSales> assembleListOfPointOfSales(final JsonCommand command) {
        final List<ClientAllyPointOfSales> pointOfSales = new ArrayList<>();
        if (command.parameterExists("pointOfSales")) {
            final JsonArray pointOfSalesArray = command.arrayOfParameterNamed("pointOfSales");
            if (pointOfSalesArray != null) {
                for (int i = 0; i < pointOfSalesArray.size(); i++) {
                    final JsonObject jsonObject = pointOfSalesArray.get(i).getAsJsonObject();
                    if (jsonObject.has("id")) {
                        final Long pointOfSaleId = jsonObject.get("id").getAsLong();
                        final ClientAllyPointOfSales ClientAllyPointOfSales = this.clientAllyPointOfSalesRepository.findById(pointOfSaleId)
                                .orElseThrow(() -> new ClientAllyPointOfSalesNotFoundException(pointOfSaleId));
                        pointOfSales.add(ClientAllyPointOfSales);
                    }
                }
            }
        }
        return pointOfSales;
    }

    @Transactional
    @Override
    public CommandProcessingResult delete(final Long id) {
        this.context.authenticatedUser();

        Optional<CustomChargeTypeMap> entity = this.customChargeTypeMapRepository.findById(id);
        if (entity.isPresent()) {
            final CustomChargeTypeMap currEntity = entity.get();
            // It is only allowed to delete the last active record
            if (Boolean.FALSE.equals(currEntity.getActive())) {
                throw new PlatformDataIntegrityException("error.msg.cannot.delete.not.last.active.record",
                        "Cannot delete a record that is not the last active record", "id", id);
            }
            // Only allowed to delete validFrom date in the future
            if (currEntity.getValidFrom().isBefore(DateUtils.getLocalDateOfTenant())) {
                throw new PlatformDataIntegrityException("error.msg.cannot.delete.record.with.from.date.in.the.past",
                        "Cannot delete a record with From Date in the past", "id", id);
            }

            // Revert last record, if any, to active status
            Optional<CustomChargeTypeMap> customChargeTypeMapList = this.customChargeTypeMapRepository
                    .findByCustomChargeTypeIdAndTermAndActive(currEntity.getCustomChargeTypeId(), currEntity.getTerm(), false).stream()
                    .max(Comparator.comparing(CustomChargeTypeMap::getId));

            customChargeTypeMapList.ifPresent(customChargeTypeMap -> {
                customChargeTypeMap.setActive(true);
                customChargeTypeMap.setValidTo(null);
                customChargeTypeMap.setUpdatedAt(DateUtils.getLocalDateTimeOfTenant());
                customChargeTypeMap.setUpdatedBy(this.context.authenticatedUser().getId());

                this.customChargeTypeMapRepository.save(customChargeTypeMap);
            });

            this.customChargeTypeMapRepository.delete(entity.get());
            this.customChargeTypeMapRepository.flush();
        } else {
            throw new CustomChargeTypeMapNotFoundException();
        }

        return new CommandProcessingResultBuilder().withEntityId(id).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final JsonCommand command, Long id) {
        try {
            this.context.authenticatedUser();
            final CustomChargeTypeMap entity = this.validatorClass.validateForUpdate(command.json());
            Optional<CustomChargeTypeMap> dbEntityOptional = this.customChargeTypeMapRepository.findById(id);
            if (dbEntityOptional.isPresent()) {
                final CustomChargeTypeMap dbEntity = dbEntityOptional.get();
                // It is only allowed to delete the last active record
                if (Boolean.FALSE.equals(dbEntityOptional.get().getActive())) {
                    throw new PlatformDataIntegrityException("error.msg.cannot.update.not.last.active.record",
                            "Cannot update a record that is not the last active record", "id", id);
                }
                // Only allowed to delete validFrom date in the future
                if (dbEntityOptional.get().getValidFrom().isBefore(DateUtils.getLocalDateOfTenant())) {
                    throw new PlatformDataIntegrityException("error.msg.cannot.update.record.with.from.date.in.the.past",
                            "Cannot update a record with From Date in the past", "id", id);
                }
                final List<ClientAllyPointOfSales> clientAllyPointOfSaleList = assembleListOfPointOfSales(command);
                if (CollectionUtils.isNotEmpty(clientAllyPointOfSaleList)) {
                    entity.setClientAllyPointOfSales(clientAllyPointOfSaleList);
                }
                entity.setCustomChargeTypeId(dbEntity.getCustomChargeTypeId());
                entity.setId(id);
                entity.setCreatedAt(dbEntity.getCreatedAt());
                entity.setCreatedBy(dbEntity.getCreatedBy());
                this.customChargeTypeMapRepository.save(entity);
            } else {
                throw new CustomChargeTypeMapNotFoundException();
            }

            return new CommandProcessingResultBuilder().withEntityId(entity.getId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(dve.getMostSpecificCause());
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(throwable);
            return CommandProcessingResult.empty();
        }
    }

    private void handleDataIntegrityIssues(final Throwable realCause) {
        if (realCause != null) {
            if (realCause.getMessage().contains("unique_c_custom_charge_type_map")) {
                throw new PlatformDataIntegrityException("error.msg.customchargetypemap.duplicate.entry",
                        "A record with the same custom charge type, term and valid from date already exists",
                        "customChargeTypeId, term, validFrom");
            }
            log.error(realCause.getMessage(), realCause);
        }
        throw new PlatformDataIntegrityException("error.msg.customchargetypemap.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    public static final class ChargeTypeMapRowMapper implements RowMapper<CustomChargeTypeMapData> {

        public String schema() {
            return """
                    ctm.id AS id,
                    ccce."name" AS "chargeName",
                    ccce.id AS "chargeId",
                    ccce.code AS "chargeCode",
                    ccct.id AS "chargeTypeId",
                    ccct."name" AS "chargeTypeName",
                    ccct.code AS "chargeTypeCode",
                    ctm.percentage AS "percentage",
                    ctm.term AS term,
                    ctm.valid_from AS "validFromDate",
                    ctm.active AS active,
                    CONCAT(ma.firstname, ' ', ma.lastname) AS "createdBy"
                    FROM custom.c_custom_charge_type_map ctm
                    INNER JOIN custom.c_custom_charge_type ccct ON ccct.id = ctm.c_custom_charge_type_id
                    INNER JOIN custom.c_custom_charge_entity ccce ON ccce.id  = ccct.custom_charge_entity_id
                    LEFT JOIN m_appuser ma ON ma.id = ctm.created_by
                    """;
        }

        @Override
        public CustomChargeTypeMapData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final Long chargeId = rs.getLong("chargeId");
            final String chargeName = rs.getString("chargeName");
            final String chargeCode = rs.getString("chargeCode");
            final EnumOptionData charge = new EnumOptionData(chargeId, chargeCode, chargeName);

            final Long chargeTypeId = rs.getLong("chargeTypeId");
            final String chargeTypeName = rs.getString("chargeTypeName");
            final String chargeTypeCode = rs.getString("chargeTypeCode");
            final EnumOptionData chargeType = new EnumOptionData(chargeTypeId, chargeTypeCode, chargeTypeName);

            final BigDecimal percentage = rs.getBigDecimal("percentage");
            final Long term = rs.getLong("term");
            final boolean active = rs.getBoolean("active");
            LocalDate validFromDate = JdbcSupport.getLocalDate(rs, "validFromDate");
            final String createdBy = rs.getString("createdBy");

            return CustomChargeTypeMapData.builder().id(id).percentage(percentage).term(term).chargeType(chargeType).charge(charge)
                    .active(active).validFromDate(validFromDate).createdBy(createdBy).build();
        }
    }
}
