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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.service;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.RestructureCreditsLoanMappingData;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.RestructureCreditsRequestData;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.RestructureCreditStatus;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.RestructureStatusEnumerations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RestructureCreditsReadPlatformServiceImpl implements RestructureCreditsReadPlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(RestructureCreditsReadPlatformServiceImpl.class);
    private final RestructureCreditsRequestMapper restructureCreditsRequestMapper = new RestructureCreditsRequestMapper();
    private final RestructureCreditsLoanMappingsMapper creditsLoanMappingsMapper = new RestructureCreditsLoanMappingsMapper();
    private final JdbcTemplate jdbcTemplate;



    /**
     * LoanRescheduleRequestWritePlatformServiceImpl constructor
     *
     *
     **/
    @Autowired
    public RestructureCreditsReadPlatformServiceImpl(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public LoanScheduleModel previewCreditRestructure(Long clientId) {
        return null;
    }

    @Override
    public RestructureCreditsRequestData retrievePendingRestructure(Long clientId) {
        final String sql = "select " + this.restructureCreditsRequestMapper.schema() + " WHERE rcr.client_id = ? and rcr.status = ?";

        final List<RestructureCreditsRequestData> requestDataList = this.jdbcTemplate.query(sql, this.restructureCreditsRequestMapper,
                new Object[]{clientId, RestructureCreditStatus.PENDING.getValue()});

        RestructureCreditsRequestData requestData = null;
        if (requestDataList != null && requestDataList.size() > 0) {
            requestData = requestDataList.get(0);

            final String membersql = "select " + this.creditsLoanMappingsMapper.schema() + " WHERE rcm.request_id = ? ";

            List<RestructureCreditsLoanMappingData> loans = this.jdbcTemplate.query(membersql, this.creditsLoanMappingsMapper,
                    new Object[]{requestData.getId()});


            requestData.setLoanMappingData(loans);
        }
        return requestData;
    }

    private static final class RestructureCreditsRequestMapper implements RowMapper<RestructureCreditsRequestData> {

        private final String schema;

        RestructureCreditsRequestMapper() {
            this.schema = " " +
                    "rcr.id, " +
                    "rcr.total_loan_amount, " +
                    "mc.display_name, " +
                    "pl.name as productName, " +
                    "rcr.status, " +
                    "rcr.new_disbursement_date, " +
                    "rcr.comments, " +
                    "rcr.date_requested, " +
                    "creator.username as createdBy, " +
                    "approver.username as approvedBy," +
                    "modifier.username as modifiedBy," +
                    "rcr.date_approved, " +
                    "rcr.product_id as productId, " +
                    "rcr.lastmodified_date " +
                    "from m_restructure_credit_requests rcr " +
                    "inner join m_client mc on mc.id = rcr.client_id "+
                    "inner join m_product_loan pl on pl.id = rcr.product_id "+
                    "inner join m_appuser creator on creator.id = rcr.requested_by "+
                    "left join m_appuser approver on rcr.approved_by = approver.id "+
                    "left join m_appuser modifier on rcr.lastmodifiedby_id = modifier.id ";
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public RestructureCreditsRequestData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final Long id = JdbcSupport.getLong(rs, "id");
            final Long productId = JdbcSupport.getLong(rs, "productId");
            final Integer statusEnum = JdbcSupport.getInteger(rs, "status");
            final EnumOptionData status = RestructureStatusEnumerations.status(statusEnum);
            String clientName = rs.getString("display_name");
            String productName = rs.getString("productName");
            String comments = rs.getString("comments");
            String createdBy = rs.getString("createdBy");
            String approvedBy = rs.getString("approvedBy");
            String modifiedBy = rs.getString("modifiedBy");
            final LocalDateTime dateRequested = JdbcSupport.getLocalDateTime(rs, "date_requested");
            final LocalDateTime dateApproved = JdbcSupport.getLocalDateTime(rs, "date_approved");
            final LocalDateTime dateModified = JdbcSupport.getLocalDateTime(rs, "lastmodified_date");
            final LocalDateTime newDisbursementDate = JdbcSupport.getLocalDateTime(rs, "new_disbursement_date");
            final BigDecimal totalLoanAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "total_loan_amount");

            return RestructureCreditsRequestData.instance(id, clientName, productName, productId, totalLoanAmount, status, newDisbursementDate,
                    comments, dateRequested, createdBy, dateApproved, approvedBy, dateModified, modifiedBy);

        }
    }

    private static final class RestructureCreditsLoanMappingsMapper implements RowMapper<RestructureCreditsLoanMappingData> {

        private final String mschema;

        RestructureCreditsLoanMappingsMapper() {

            this.mschema = " " +
                    "rcm.id, " +
                    "mpl.name as productName, " +
                    "rcm.outstanding_balance, " +
                    "rcm.disbursement_date, " +
                    "rcm.maturity_Date, " +
                    "rcm.status " +
                    "from m_restructure_credits_loans_mapping rcm "+
                    "inner join m_loan ml on ml.id = rcm.loan_id "+
                    "inner join m_product_loan mpl on mpl.id = ml.product_id ";
        }

        public String schema() {
            return this.mschema;
        }

        @Override
        public RestructureCreditsLoanMappingData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final LocalDate disbursementDate = JdbcSupport.getLocalDate(rs, "disbursement_date");
            final LocalDate maturityDate = JdbcSupport.getLocalDate(rs, "maturity_Date");
            final BigDecimal outstandingBalance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "outstanding_balance");
            String productName = rs.getString("productName");

            return RestructureCreditsLoanMappingData.instance(id,productName,outstandingBalance,disbursementDate,maturityDate);

        }
    }

}
