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
package org.apache.fineract.portfolio.vatrate.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.vatrate.data.VatRateData;
import org.apache.fineract.portfolio.vatrate.domain.VatRate;
import org.apache.fineract.portfolio.vatrate.domain.VatRateRepository;
import org.apache.fineract.portfolio.vatrate.exception.VatRatenotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ReadVatRateServiceImpl implements ReadVatRateService {

    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;
    private final VatRateDataMapper vatRateDataMapper;
    private final VatRateRepository vatRateRepository;

    @Autowired
    public ReadVatRateServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate,
            final VatRateRepository vatRateRepository) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
        this.vatRateDataMapper = new VatRateDataMapper();
        this.vatRateRepository = vatRateRepository;
    }

    @Override
    public Collection<VatRateData> retrieveAllVatRates() {
        final StringBuilder sqlBuilder = new StringBuilder("select " + this.vatRateDataMapper.schema());
        return this.jdbcTemplate.query(sqlBuilder.toString(), this.vatRateDataMapper, new Object[] {});
    }

    @Override
    public VatRateData retrieveVatRateById(Long id) {
        findById(id);
        final StringBuilder sqlBuilder = new StringBuilder("select " + this.vatRateDataMapper.schema() + " where r.id = ?");
        return this.jdbcTemplate.queryForObject(sqlBuilder.toString(), this.vatRateDataMapper, new Object[] { id });
    }

    private static final class VatRateDataMapper implements RowMapper<VatRateData> {

        private final String schemaSql;

        VatRateDataMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append(
                    "r.id as id, r.name, r.percentage as name, r.active as active, r.created_date as created_date, r.createdby_id as createdby_id, ")
                    .append("r.lastmodifiedby_id as lastmodifiedby_id, r.lastmodified_date as lastmodified_date, u.username as submittedByUsername ")
                    .append("FROM m_vat_rate r ").append("LEFT JOIN m_appuser u ON u.id = r.createdby_id");
            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public VatRateData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final BigDecimal percentage = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "percentage");
            final boolean active = rs.getBoolean("active");
            final String submittedByUsername = rs.getString("submittedByUsername");
            final LocalDate createdDate = JdbcSupport.getLocalDate(rs, "created_date");
            return new VatRateData(id, name, percentage, active, submittedByUsername, createdDate);
        }
    }

    @Override
    public VatRate findById(Long id) {
        return this.vatRateRepository.findById(id).orElseThrow(() -> new VatRatenotFoundException(id));
    }

}
