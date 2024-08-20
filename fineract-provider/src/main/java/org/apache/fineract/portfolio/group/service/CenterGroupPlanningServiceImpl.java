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
package org.apache.fineract.portfolio.group.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainServiceJpa;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.portfolio.data.PortfolioDetailedPlanningData;
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenterFrecuencyMeeting;
import org.apache.fineract.organisation.portfolioCenter.service.PortfolioCenterGroupUtil;
import org.apache.fineract.portfolio.accountdetails.service.AccountDetailsReadPlatformService;
import org.apache.fineract.portfolio.group.data.GroupLoanSummaryData;
import org.apache.fineract.portfolio.group.domain.PlanningType;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class CenterGroupPlanningServiceImpl implements CenterGroupPlanningService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;
    private final ColumnValidator columnValidator;
    private final AppUserReadPlatformService appUserReadPlatformService;
    private final ConfigurationDomainServiceJpa configurationDomainServiceJpa;
    private final AccountDetailsReadPlatformService accountDetailsReadPlatformService;
    private final LoanReadPlatformService loanReadPlatformService;

    @Autowired
    public CenterGroupPlanningServiceImpl(final JdbcTemplate jdbcTemplate, final DatabaseSpecificSQLGenerator sqlGenerator,
            final PlatformSecurityContext context, final ColumnValidator columnValidator,
            final AppUserReadPlatformService appUserReadPlatformService, final ConfigurationDomainServiceJpa configurationDomainServiceJpa,
            final AccountDetailsReadPlatformService accountDetailsReadPlatformService, LoanReadPlatformService loanReadPlatformService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.context = context;
        this.columnValidator = columnValidator;
        this.appUserReadPlatformService = appUserReadPlatformService;
        this.configurationDomainServiceJpa = configurationDomainServiceJpa;
        this.accountDetailsReadPlatformService = accountDetailsReadPlatformService;
        this.loanReadPlatformService = loanReadPlatformService;
    }

    @Override
    public Collection<PortfolioDetailedPlanningData> retrievePlanningByPortfolio(Long portfolioId, LocalDate startDateRange,
            LocalDate endDateRange) {
        PortfolioDetailedPlanningMapper portfolioDetailedPlanningMapper = new PortfolioDetailedPlanningMapper();
        String schemaSql = "select " + portfolioDetailedPlanningMapper.schema();
        schemaSql += "where pc.portfolio_id = ? ";
        schemaSql += "order by centerGroupName, position";

        Collection<PortfolioDetailedPlanningData> portfolioPlanningList = this.jdbcTemplate.query(schemaSql,
                portfolioDetailedPlanningMapper, portfolioId);

        // generate the planning based on the dates provided as parameters
        Collection<PortfolioDetailedPlanningData> portfolioPlanningDetailed = new ArrayList<>();

        // calculate the next meeting date
        for (PortfolioDetailedPlanningData portfolioPlanning : portfolioPlanningList) {

            final int meetingDayOfWeek = portfolioPlanning.getMeetingDayOfWeek();
            final int rangeStartDay = portfolioPlanning.getRangeStartDay();
            final int rangeEndDay = portfolioPlanning.getRangeEndDay();

            // check if needed to add more planning date for the current center and group
            LocalDate currentNextMeetingDate = PortfolioCenterGroupUtil.getNextMeetingDate(startDateRange, rangeStartDay, rangeEndDay,
                    meetingDayOfWeek, startDateRange, endDateRange);

            if (currentNextMeetingDate.isBefore(startDateRange)) {
                currentNextMeetingDate = PortfolioCenterGroupUtil.calculateNextMeetingDate(currentNextMeetingDate, rangeStartDay,
                        rangeEndDay, meetingDayOfWeek, PortfolioCenterFrecuencyMeeting.MENSUAL);
            }
            portfolioPlanning.setMeetingDate(currentNextMeetingDate);

            // generate the future planning for this group && loan
            while ((currentNextMeetingDate.isAfter(startDateRange) || currentNextMeetingDate.equals(startDateRange))
                    && (currentNextMeetingDate.isBefore(endDateRange) || currentNextMeetingDate.isEqual(endDateRange))) {
                List<GroupLoanSummaryData> groupLoanSummaryList = retrieveGroupLoanSummary(portfolioPlanning.getCenterGroupId(),
                        portfolioPlanning.getMeetingDate());

                List<GroupLoanSummaryData> individualLoanSummaryList = retrieveIndividualLoanSummary(portfolioPlanning.getCenterGroupId(),
                        startDateRange, endDateRange);

                if (groupLoanSummaryList != null && !groupLoanSummaryList.isEmpty()) {
                    for (GroupLoanSummaryData groupLoanSummaryData : groupLoanSummaryList) {
                        if (currentNextMeetingDate.isBefore(endDateRange) || currentNextMeetingDate.isEqual(endDateRange)) {
                            final PortfolioDetailedPlanningData newPortfolioPlanning = PortfolioDetailedPlanningData.instance(
                                    portfolioPlanning.getCenterGroupId(), portfolioPlanning.getCenterGroupName(),
                                    portfolioPlanning.getLegacyGroupNumber(), portfolioPlanning.getMeetingStartTime(),
                                    portfolioPlanning.getMeetingEndTime(), portfolioPlanning.getPortfolioCenterId(),
                                    portfolioPlanning.getPortfolioCenterName(), portfolioPlanning.getLegacyCenterNumber(),
                                    portfolioPlanning.getMeetingDayName(), currentNextMeetingDate, meetingDayOfWeek, rangeStartDay,
                                    rangeEndDay);

                            newPortfolioPlanning.setLoanShortProductName(groupLoanSummaryData.getLoanShortProductName());
                            newPortfolioPlanning.setTotalRepayment(groupLoanSummaryData.getTotalRepayment());
                            newPortfolioPlanning.setTotalOverdue(groupLoanSummaryData.getTotalOverdue());
                            newPortfolioPlanning.setTotalPaymentExpected(groupLoanSummaryData.getTotalPaymentExpected());
                            newPortfolioPlanning.setNumberOfClients(groupLoanSummaryData.getClientCounter());
                            newPortfolioPlanning.setPlanningType(PlanningType.GROUP.getCode());

                            portfolioPlanningDetailed.add(newPortfolioPlanning);
                        }
                    }
                } else {
                    if (currentNextMeetingDate.isBefore(endDateRange) || currentNextMeetingDate.isEqual(endDateRange)) {
                        final PortfolioDetailedPlanningData newPortfolioPlanning = PortfolioDetailedPlanningData.instance(
                                portfolioPlanning.getCenterGroupId(), portfolioPlanning.getCenterGroupName(),
                                portfolioPlanning.getLegacyGroupNumber(), portfolioPlanning.getMeetingStartTime(),
                                portfolioPlanning.getMeetingEndTime(), portfolioPlanning.getPortfolioCenterId(),
                                portfolioPlanning.getPortfolioCenterName(), portfolioPlanning.getLegacyCenterNumber(),
                                portfolioPlanning.getMeetingDayName(), currentNextMeetingDate, meetingDayOfWeek, rangeStartDay,
                                rangeEndDay);

                        newPortfolioPlanning.setLoanShortProductName("");
                        newPortfolioPlanning.setTotalRepayment(BigDecimal.ZERO);
                        newPortfolioPlanning.setTotalOverdue(BigDecimal.ZERO);
                        newPortfolioPlanning.setTotalPaymentExpected(BigDecimal.ZERO);
                        newPortfolioPlanning.setNumberOfClients(0);
                        newPortfolioPlanning.setPlanningType(PlanningType.GROUP.getCode());

                        portfolioPlanningDetailed.add(newPortfolioPlanning);
                    }
                }

                if (individualLoanSummaryList != null && !individualLoanSummaryList.isEmpty()) {
                    for (GroupLoanSummaryData individualLoanSummaryData : individualLoanSummaryList) {
                        if (currentNextMeetingDate.isBefore(endDateRange) || currentNextMeetingDate.isEqual(endDateRange)) {
                            LocalDate individualInstallmentDate = individualLoanSummaryData.getInstallmentDate() != null
                                    ? individualLoanSummaryData.getInstallmentDate()
                                    : currentNextMeetingDate;
                            final PortfolioDetailedPlanningData newPortfolioPlanning = PortfolioDetailedPlanningData.instance(
                                    portfolioPlanning.getCenterGroupId(), individualLoanSummaryData.getClientName(),
                                    portfolioPlanning.getLegacyGroupNumber(), portfolioPlanning.getMeetingStartTime(),
                                    portfolioPlanning.getMeetingEndTime(), portfolioPlanning.getPortfolioCenterId(),
                                    portfolioPlanning.getPortfolioCenterName(), portfolioPlanning.getLegacyCenterNumber(),
                                    portfolioPlanning.getMeetingDayName(), individualInstallmentDate, meetingDayOfWeek, rangeStartDay,
                                    rangeEndDay);

                            newPortfolioPlanning.setLoanShortProductName(individualLoanSummaryData.getLoanShortProductName());
                            newPortfolioPlanning.setTotalRepayment(individualLoanSummaryData.getTotalRepayment());
                            newPortfolioPlanning.setTotalOverdue(individualLoanSummaryData.getTotalOverdue());
                            newPortfolioPlanning.setTotalPaymentExpected(individualLoanSummaryData.getTotalPaymentExpected());
                            newPortfolioPlanning.setNumberOfClients(individualLoanSummaryData.getClientCounter());
                            newPortfolioPlanning.setPlanningType(PlanningType.INDIVIDUAL.getCode());

                            portfolioPlanningDetailed.add(newPortfolioPlanning);
                        }
                    }
                }

                // meetings with groups have a monthly frequency
                LocalDate nextMeetingDate = PortfolioCenterGroupUtil.calculateNextMeetingDate(currentNextMeetingDate, rangeStartDay,
                        rangeEndDay, meetingDayOfWeek, PortfolioCenterFrecuencyMeeting.MENSUAL);

                currentNextMeetingDate = nextMeetingDate;
            }
        }
        return portfolioPlanningDetailed;
    }

    private List<GroupLoanSummaryData> retrieveGroupLoanSummary(Long groupId, LocalDate dueDate) {
        String sql = """
                SELECT
                	gc.group_id AS groupId,
                	coalesce(overdueSummary.totalOverdue,0) as totalOverdue,
                	coalesce(paymentsSummary.totalRepayment,0) as totalRepayment,
                	count( gc.client_id ) AS clientCounter
                FROM
                	m_group_client gc
                	JOIN m_client mc on mc.id = gc.client_id and mc.status_enum = 300
                	LEFT JOIN (
                	SELECT
                		gc2.group_id AS groupId,
                		coalesce (sum(larr.total_overdue_derived),0) AS totalOverdue
                	FROM
                		m_loan l2
                		INNER JOIN m_product_loan lp2 ON lp2.id = l2.product_id
                		INNER JOIN m_group_client gc2 ON l2.client_id = gc2.client_id
                		INNER JOIN m_client mc2 on mc2.id = gc2.client_id and mc2.status_enum = 300
                		INNER JOIN m_group grp ON gc2.group_id = grp.id
                		LEFT JOIN m_loan_arrears_aging larr ON larr.loan_id = l2.id
                		LEFT JOIN m_prequalification_group preq ON preq.id = grp.prequalification_id AND preq.product_id = l2.product_id
                	WHERE
                		gc2.group_id = ?
                		AND l2.loan_status_id = 300
                	    AND lp2.owner_type_enum = 2
                	    ) overdueSummary ON overdueSummary.groupId = gc.group_id
                	LEFT JOIN (
                	SELECT
                		gc2.group_id AS groupId,
                		coalesce (sum(
                			(
                				COALESCE ( lrs2.principal_amount, 0 ) + COALESCE ( lrs2.interest_amount, 0 ) +
                				COALESCE ( lrs2.penalty_charges_amount, 0 ) + COALESCE ( lrs2.fee_charges_amount, 0 ) +
                				COALESCE ( lrs2.fee_charges_amount, 0 )) -
                				(COALESCE ( lrs2.total_paid_late_derived, 0 ) + COALESCE ( lrs2.interest_completed_derived, 0 ) +
                				COALESCE ( lrs2.principal_completed_derived, 0 ) + COALESCE ( lrs2.total_paid_in_advance_derived, 0 ) +
                				COALESCE ( lrs2.interest_writtenoff_derived, 0 ) + COALESCE ( lrs2.principal_writtenoff_derived, 0 ) +
                				COALESCE ( lrs2.interest_waived_derived, 0 ) +
                				COALESCE ( lrs2.penalty_charges_writtenoff_derived, 0 ) + COALESCE ( lrs2.penalty_charges_waived_derived, 0 )
                			)
                		),0) AS totalRepayment
                	FROM
                		m_loan_repayment_schedule lrs2
                		INNER JOIN m_loan l2 ON l2.id = lrs2.loan_id
                		INNER JOIN m_product_loan lp2 ON lp2.id = l2.product_id
                		INNER JOIN m_group_client gc2 ON l2.client_id = gc2.client_id
                		INNER JOIN m_client mc2 on mc2.id = gc2.client_id and mc2.status_enum = 300
                		INNER JOIN m_group grp ON gc2.group_id = grp.id
                		LEFT JOIN m_prequalification_group preq ON preq.id = grp.prequalification_id AND preq.product_id = l2.product_id
                	WHERE
                		gc2.group_id = ?
                		AND lrs2.duedate = ?
                		AND l2.loan_status_id = 300
                		AND lrs2.completed_derived = 0
                		AND lp2.owner_type_enum = 2
                	) paymentsSummary ON paymentsSummary.groupId = gc.group_id

                WHERE
                	gc.group_id = ?
                """;
        List<GroupLoanSummaryData> groupLoanSummaryData = jdbcTemplate.query(sql, new BeanPropertyRowMapper(GroupLoanSummaryData.class),
                new Object[] { groupId, groupId, dueDate, groupId });

        return groupLoanSummaryData;
    }

    private List<GroupLoanSummaryData> retrieveIndividualLoanSummary(Long groupId, LocalDate startDateRange, LocalDate endDateRange) {
        String sql = """
                SELECT
                	gc.group_id AS groupId,
                	gc.client_id AS clientId,
                	mc.display_name AS clientName,
                	COALESCE ( overdueSummary.totalOverdue, 0 ) AS totalOverdue,
                	COALESCE ( paymentsSummary.totalRepayment, 0 ) AS totalRepayment,
                	paymentsSummary.installmentDate,
                	count( gc.client_id ) AS clientCounter
                FROM
                	m_group_client gc
                	JOIN m_client mc ON mc.id = gc.client_id AND mc.status_enum = 300
                		LEFT JOIN (
                		SELECT
                		gc2.client_id AS clientId,
                		COALESCE ( sum( larr.total_overdue_derived ), 0 ) AS totalOverdue
                	FROM
                		m_loan l2
                		INNER JOIN m_product_loan lp2 ON lp2.id = l2.product_id
                		INNER JOIN m_group_client gc2 ON l2.client_id = gc2.client_id
                		INNER JOIN m_client mc2 ON mc2.id = gc2.client_id AND mc2.status_enum = 300
                		INNER JOIN m_group grp ON gc2.group_id = grp.id
                		LEFT JOIN m_loan_arrears_aging larr ON larr.loan_id = l2.id
                	WHERE
                		gc2.group_id = ?
                		AND l2.loan_status_id = 300
                        AND lp2.id in (3,7)
                		GROUP BY gc2.client_id
                	) overdueSummary ON overdueSummary.clientId = gc.client_id
                	LEFT JOIN (
                        SELECT
                            gc2.client_id AS clientId,
                            lrs2.duedate AS installmentDate,
                            coalesce (sum((
                                COALESCE ( lrs2.principal_amount, 0 ) + COALESCE ( lrs2.interest_amount, 0 ) +
                                COALESCE ( lrs2.penalty_charges_amount, 0 ) + COALESCE ( lrs2.fee_charges_amount, 0 ) +
                                COALESCE ( lrs2.fee_charges_amount, 0 )) -
                                (COALESCE ( lrs2.total_paid_late_derived, 0 ) + COALESCE ( lrs2.interest_completed_derived, 0 ) +
                                COALESCE ( lrs2.principal_completed_derived, 0 ) + COALESCE ( lrs2.total_paid_in_advance_derived, 0 ) +
                                COALESCE ( lrs2.interest_writtenoff_derived, 0 ) + COALESCE ( lrs2.principal_writtenoff_derived, 0 ) +
                                COALESCE ( lrs2.interest_waived_derived, 0 ) +
                                COALESCE ( lrs2.penalty_charges_writtenoff_derived, 0 ) + COALESCE ( lrs2.penalty_charges_waived_derived, 0 ))),0) AS totalRepayment
                            FROM
                            m_loan_repayment_schedule lrs2
                            INNER JOIN m_loan l2 ON l2.id = lrs2.loan_id
                            INNER JOIN m_product_loan lp2 ON lp2.id = l2.product_id
                            INNER JOIN m_group_client gc2 ON l2.client_id = gc2.client_id
                            INNER JOIN m_client mc2 on mc2.id = gc2.client_id and mc2.status_enum = 300
                            INNER JOIN m_group grp ON gc2.group_id = grp.id
                			WHERE
                            gc2.group_id = ?
                            AND lrs2.duedate between ? AND ?
                            AND l2.loan_status_id = 300
                            AND lrs2.completed_derived = 0
                            AND lp2.id in (3,7)
                			GROUP BY gc2.client_id ) paymentsSummary ON paymentsSummary.clientId = gc.client_id
                WHERE
                	gc.group_id = ?
                	and (totalOverdue >0 OR totalRepayment>0)
                GROUP BY gc.client_id
                """;
        List<GroupLoanSummaryData> groupLoanSummaryData = jdbcTemplate.query(sql, new BeanPropertyRowMapper(GroupLoanSummaryData.class),
                new Object[] { groupId, groupId, startDateRange, endDateRange, groupId });

        return groupLoanSummaryData;
    }

    private static final class PortfolioDetailedPlanningMapper implements RowMapper<PortfolioDetailedPlanningData> {

        private final String schema;

        public PortfolioDetailedPlanningMapper() {
            // FBR-193 - the query to get the center and group information must be based on m_group table
            final StringBuilder sqlBuilder = new StringBuilder(300);
            sqlBuilder.append("cvMeetingDay.code_value as meetingDayName, cg.id as centerGroupId, cg.display_name as centerGroupName, ");
            sqlBuilder.append("cg.legacy_number as legacyGroupNumber, cg.meeting_start_time as meetingStartTime, ");
            sqlBuilder.append(" cg.meeting_end_time as meetingEndTime, pc.id as portfolioCenterId, ");
            sqlBuilder.append(
                    "pc.display_name as portfolioCenterName, pc.legacy_number as legacyCenterNumber,pc.meeting_day as meetingDay, ");
            sqlBuilder.append(
                    "cvMeetingDay.order_position as position, pc.meeting_start_date as rangeStartDay, pc.meeting_end_date as rangeEndDay ");
            sqlBuilder.append("from m_group cg ");
            sqlBuilder.append("left join m_group pc on pc.id = cg.parent_id ");
            sqlBuilder.append("left join m_code_value cvMeetingDay on cvMeetingDay.id = pc.meeting_day ");

            this.schema = sqlBuilder.toString();
        }

        @Override
        public PortfolioDetailedPlanningData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long centerGroupId = rs.getLong("centerGroupId");
            final String centerGroupName = rs.getString("centerGroupName");
            final BigDecimal legacyGroupNumber = rs.getBigDecimal("legacyGroupNumber");

            final Long portfolioCenterId = rs.getLong("portfolioCenterId");
            final String portfolioCenterName = rs.getString("portfolioCenterName");
            final BigDecimal legacyCenterNumber = rs.getBigDecimal("legacyCenterNumber");

            final LocalTime meetingStartTime = JdbcSupport.getLocalTime(rs, "meetingStartTime");
            final LocalTime meetingEndTime = JdbcSupport.getLocalTime(rs, "meetingEndTime");

            final String meetingDayName = rs.getString("meetingDayName");

            // set the next meeting date as the current date just for initialization
            final int meetingDayOfWeek = rs.getInt("position");
            final int rangeStartDay = rs.getInt("rangeStartDay");
            final int rangeEndDay = rs.getInt("rangeEndDay");
            LocalDate nextMeetingDate = DateUtils.getLocalDateOfTenant();

            return PortfolioDetailedPlanningData.instance(centerGroupId, centerGroupName, legacyGroupNumber, meetingStartTime,
                    meetingEndTime, portfolioCenterId, portfolioCenterName, legacyCenterNumber, meetingDayName, nextMeetingDate,
                    meetingDayOfWeek, rangeStartDay, rangeEndDay);
        }

        public String schema() {
            return this.schema;
        }

    }
}
