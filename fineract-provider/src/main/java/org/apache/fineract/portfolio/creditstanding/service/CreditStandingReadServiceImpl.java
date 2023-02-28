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
package org.apache.fineract.portfolio.creditstanding.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.portfolio.creditstanding.data.CreditStandingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class CreditStandingReadServiceImpl implements CreditStandingReadService {

    private static final Logger LOG = LoggerFactory.getLogger(CreditStandingReadServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;

    public CreditStandingReadServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CreditStandingData findById(Long creditStandingId) {
        CreditStandingMapper creditStandingMapper = new CreditStandingMapper();
        String schemaSql = creditStandingMapper.schema();
        schemaSql += "where cs.id = ?";
        return this.jdbcTemplate.queryForObject(schemaSql, creditStandingMapper, new Object[] { creditStandingId });
    }

    @Override
    public CreditStandingData findByClientId(Long clientId) {

        try {
            CreditStandingMapper creditStandingMapper = new CreditStandingMapper();
            String schemaSql = creditStandingMapper.schema();
            schemaSql += "where cs.client_id = ?";
            return this.jdbcTemplate.queryForObject(schemaSql, creditStandingMapper, new Object[] { clientId });
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    private BigDecimal calculateAvailableMRA(Long clientId) {

        String sql = "select ((SUM(ifnull(mr.principal_amount, 0))) +  (SUM(ifnull(mr.interest_amount, 0))) + "
                + " (SUM(ifnull(mr.fee_charges_amount, 0))) + (SUM(ifnull(mr.penalty_charges_amount, 0)))) - "
                + " ((SUM(ifnull(mr.principal_completed_derived, 0))) +  (SUM(ifnull(mr.interest_completed_derived, 0))) + "
                + " (SUM(ifnull(mr.fee_charges_completed_derived, 0))) + (SUM(ifnull(mr.penalty_charges_completed_derived, 0)))) as total_pending_to_pay "
                + " from m_loan ml INNER JOIN m_loan_repayment_schedule mr on mr.loan_id = ml.id "
                + " where mr.completed_derived = 0 and ml.client_id = ?";
        try {
            return this.jdbcTemplate.queryForObject(sql, BigDecimal.class, clientId);
        } catch (EmptyResultDataAccessException e) {
            LOG.error("Problem encountered in calculateAvailableMRA()", e);
            return null;
        }
    }

    private BigDecimal calculateMonthlyCommitment(Long clientId) {
        String sql = " select SUM( " + " (select ((SUM(ifnull(mr.principal_amount, 0))) +  (SUM(ifnull(mr.interest_amount, 0))) + "
                + " (SUM(ifnull(mr.fee_charges_amount, 0))) + (SUM(ifnull(mr.penalty_charges_amount, 0)))) - "
                + " ((SUM(ifnull(mr.principal_completed_derived, 0))) +  (SUM(ifnull(mr.interest_completed_derived, 0))) + "
                + " (SUM(ifnull(mr.fee_charges_completed_derived, 0))) + (SUM(ifnull(mr.penalty_charges_completed_derived, 0)))) as total_pending_to_pay "
                + " from m_loan ml INNER JOIN m_loan_repayment_schedule mr on mr.loan_id = ml.id " + " where mr.completed_derived = 0 "
                + " and ml.id = l.id and ml.client_id = ? " + " group by  ml.id, mr.installment " + " order by 1 desc LIMIT 1 "
                + " )) as max_installment_to_pay " + " from m_loan l ";

        try {
            return this.jdbcTemplate.queryForObject(sql, BigDecimal.class, clientId);
        } catch (EmptyResultDataAccessException e) {
            LOG.error("Problem encountered in calculateMonthlyCommitment()", e);
            return null;
        }
    }

    private BigDecimal calculateTotalDebt(Long clientId) {

        String sql = "select ((SUM(ifnull(mr.principal_amount, 0))) + (SUM(ifnull(mr.interest_amount, 0))) + "
                + " (SUM(ifnull(mr.fee_charges_amount, 0))) + (SUM(ifnull(mr.penalty_charges_amount, 0)))) as total_debt "
                + " from m_loan ml INNER JOIN m_loan_repayment_schedule mr on mr.loan_id = ml.id " + " where ml.client_id = ?";
        try {
            return this.jdbcTemplate.queryForObject(sql, BigDecimal.class, clientId);
        } catch (EmptyResultDataAccessException e) {
            LOG.error("Problem encountered in calculateTotalDebt()", e);
            return null;
        }
    }

    private BigDecimal calculateCurrentDebt(Long clientId) {

        String sql = "select ((SUM(ifnull(mr.principal_amount, 0))) + (SUM(ifnull(mr.interest_amount, 0))) + "
                + " (SUM(ifnull(mr.fee_charges_amount, 0))) + (SUM(ifnull(mr.penalty_charges_amount, 0)))) as total_debt_no_expired "
                + " from m_loan ml INNER JOIN m_loan_repayment_schedule mr on mr.loan_id = ml.id "
                + " where mr.duedate > CURDATE() and ml.client_id = ?";
        try {
            return this.jdbcTemplate.queryForObject(sql, BigDecimal.class, clientId);
        } catch (EmptyResultDataAccessException e) {
            LOG.error("Problem encountered in calculateCurrentDebt()", e);
            return null;
        }
    }

    private BigDecimal calculateExpiredDebt(Long clientId) {

        String sql = "select ((SUM(ifnull(mr.principal_amount, 0))) + (SUM(ifnull(mr.interest_amount, 0))) + "
                + " (SUM(ifnull(mr.fee_charges_amount, 0))) + (SUM(ifnull(mr.penalty_charges_amount, 0)))) as total_debt_no_expired "
                + " from m_loan ml INNER JOIN m_loan_repayment_schedule mr on mr.loan_id = ml.id "
                + " where mr.duedate < CURDATE() and ml.client_id = ?";
        try {
            return this.jdbcTemplate.queryForObject(sql, BigDecimal.class, clientId);
        } catch (EmptyResultDataAccessException e) {
            LOG.error("Problem encountered in calculateExpiredDebt()", e);
            return null;
        }
    }

    private Integer getMaxDelayInDays(Long clientId) {

        String sql = " select (curdate() - mr.duedate) as delay_days "
                + " from m_loan_repayment_schedule mr INNER JOIN m_loan ml on ml.id = mr.loan_id " + " where ml.client_id = ? "
                + " having delay_days > 0 " + " order by 1 desc limit 1 ";

        try {
            return this.jdbcTemplate.queryForObject(sql, Integer.class, clientId);
        } catch (EmptyResultDataAccessException e) {
            LOG.error("No loans for this client in getMaxDelayInDays()", e);
            return null;
        }
    }

    private final class CreditStandingMapper implements RowMapper<CreditStandingData> {

        private final String schema;

        public CreditStandingMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(100);
            sqlBuilder.append("select cs.id as id, ");
            sqlBuilder.append("cs.total_credit_line as totalCreditLine, cs.rci_max as rciMax, ");
            sqlBuilder.append("cl.id as clientId, cl.display_name as clientName ");
            sqlBuilder.append("from m_credit_standing cs ");
            sqlBuilder.append("left join m_client cl on cl.id = cs.client_id ");
            this.schema = sqlBuilder.toString();
        }

        @Override
        public CreditStandingData mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long id = rs.getLong("id");
            BigDecimal totalCreditLine = rs.getBigDecimal("totalCreditLine");
            BigDecimal rciMax = rs.getBigDecimal("rciMax");

            Long clientId = JdbcSupport.getLong(rs, "clientId");

            CreditStandingData creditStandingDataReturn = CreditStandingData.instance(id, clientId, totalCreditLine, rciMax);
            creditStandingDataReturn.setMraAvailable(calculateAvailableMRA(clientId));
            creditStandingDataReturn.setMonthlyCommitment(calculateMonthlyCommitment(clientId));
            creditStandingDataReturn.setTotalDebt(calculateTotalDebt(clientId));
            creditStandingDataReturn.setCurrentDebt(calculateCurrentDebt(clientId));
            creditStandingDataReturn.setCurrentDebt(calculateCurrentDebt(clientId));
            creditStandingDataReturn.setExpiredDebt(calculateExpiredDebt(clientId));
            creditStandingDataReturn.setDelayInDays(getMaxDelayInDays(clientId));
            return creditStandingDataReturn;
        }

        public String schema() {
            return this.schema;
        }

    }
}
