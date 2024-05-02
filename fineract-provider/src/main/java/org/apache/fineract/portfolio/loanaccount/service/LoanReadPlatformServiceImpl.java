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
package org.apache.fineract.portfolio.loanaccount.service;

import static org.apache.fineract.portfolio.loanproduct.service.LoanEnumerations.interestType;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.infrastructure.security.utils.SQLInjectionValidator;
import org.apache.fineract.organisation.bankAccount.data.BankAccountData;
import org.apache.fineract.organisation.bankAccount.service.BankAccountReadPlatformServiceImpl;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.prequalification.data.GroupPrequalificationData;
import org.apache.fineract.organisation.prequalification.data.LoanAdditionalData;
import org.apache.fineract.organisation.prequalification.domain.LoanAdditionProperties;
import org.apache.fineract.organisation.prequalification.domain.LoanAdditionalPropertiesRepository;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationType;
import org.apache.fineract.organisation.prequalification.service.PrequalificationReadPlatformServiceImpl;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.organisation.staff.service.StaffReadPlatformService;
import org.apache.fineract.portfolio.account.data.AccountTransferData;
import org.apache.fineract.portfolio.accountdetails.data.LoanAccountSummaryData;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.accountdetails.service.AccountDetailsReadPlatformService;
import org.apache.fineract.portfolio.accountdetails.service.AccountEnumerations;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistStatus;
import org.apache.fineract.portfolio.calendar.data.CalendarData;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.service.CalendarReadPlatformService;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.domain.ClientEnumerations;
import org.apache.fineract.portfolio.client.exception.ClientBlacklistedException;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.common.service.CommonEnumerations;
import org.apache.fineract.portfolio.floatingrates.data.InterestRatePeriodData;
import org.apache.fineract.portfolio.floatingrates.service.FloatingRatesReadPlatformService;
import org.apache.fineract.portfolio.fund.data.FundData;
import org.apache.fineract.portfolio.fund.service.FundReadPlatformService;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.group.data.GroupRoleData;
import org.apache.fineract.portfolio.group.service.GroupReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.data.AdditionalsExtraLoansData;
import org.apache.fineract.portfolio.loanaccount.data.CollectionData;
import org.apache.fineract.portfolio.loanaccount.data.DisbursementData;
import org.apache.fineract.portfolio.loanaccount.data.GroupLoanAdditionalData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.LoanApplicationTimelineData;
import org.apache.fineract.portfolio.loanaccount.data.LoanApprovalData;
import org.apache.fineract.portfolio.loanaccount.data.LoanInterestRecalculationData;
import org.apache.fineract.portfolio.loanaccount.data.LoanPaymentSimulationData;
import org.apache.fineract.portfolio.loanaccount.data.LoanRepaymentScheduleInstallmentData;
import org.apache.fineract.portfolio.loanaccount.data.LoanScheduleAccrualData;
import org.apache.fineract.portfolio.loanaccount.data.LoanStatusEnumData;
import org.apache.fineract.portfolio.loanaccount.data.LoanSummaryData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionEnumData;
import org.apache.fineract.portfolio.loanaccount.data.PaidInAdvanceData;
import org.apache.fineract.portfolio.loanaccount.data.RepaymentScheduleRelatedLoanData;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleTransactionProcessorFactory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanSubStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTermVariationType;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.exception.LoanNotFoundException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanTransactionNotFoundException;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.loanproduct.data.TransactionProcessingStrategyData;
import org.apache.fineract.portfolio.loanproduct.domain.InterestMethod;
import org.apache.fineract.portfolio.loanproduct.service.LoanDropdownReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.service.LoanEnumerations;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.fineract.portfolio.paymentdetail.data.PaymentDetailData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.apache.fineract.portfolio.paymenttype.service.PaymentTypeReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional(readOnly = true)
public class LoanReadPlatformServiceImpl implements LoanReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository;
    private final LoanProductReadPlatformService loanProductReadPlatformService;
    private final ClientReadPlatformService clientReadPlatformService;
    private final GroupReadPlatformService groupReadPlatformService;
    private final LoanDropdownReadPlatformService loanDropdownReadPlatformService;
    private final FundReadPlatformService fundReadPlatformService;
    private final ChargeReadPlatformService chargeReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final CodeValueRepositoryWrapper codeValueRepositoryWrapper;
    private final CalendarReadPlatformService calendarReadPlatformService;
    private final StaffReadPlatformService staffReadPlatformService;
    private final PaginationHelper paginationHelper;
    private final LoanMapper loaanLoanMapper;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final PaymentTypeReadPlatformService paymentTypeReadPlatformService;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;
    private final FloatingRatesReadPlatformService floatingRatesReadPlatformService;
    private final LoanUtilService loanUtilService;
    private final ConfigurationDomainService configurationDomainService;
    private final AccountDetailsReadPlatformService accountDetailsReadPlatformService;
    private final ColumnValidator columnValidator;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final LoanAdditionalPropertiesRepository loanAdditionalPropertiesRepository;
    private final PrequalificationReadPlatformServiceImpl.PrequalificationIndividualMappingsMapper prequalificationIndividualMappingsMapper = new PrequalificationReadPlatformServiceImpl.PrequalificationIndividualMappingsMapper();

    @Autowired
    public LoanReadPlatformServiceImpl(final PlatformSecurityContext context,
            final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository,
            final LoanProductReadPlatformService loanProductReadPlatformService, final ClientReadPlatformService clientReadPlatformService,
            final GroupReadPlatformService groupReadPlatformService, final LoanDropdownReadPlatformService loanDropdownReadPlatformService,
            final FundReadPlatformService fundReadPlatformService, final ChargeReadPlatformService chargeReadPlatformService,
            final CodeValueReadPlatformService codeValueReadPlatformService, final JdbcTemplate jdbcTemplate,
            final NamedParameterJdbcTemplate namedParameterJdbcTemplate, final CalendarReadPlatformService calendarReadPlatformService,
            final StaffReadPlatformService staffReadPlatformService, final PaymentTypeReadPlatformService paymentTypeReadPlatformService,
            final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory,
            final FloatingRatesReadPlatformService floatingRatesReadPlatformService, final LoanUtilService loanUtilService,
            final ConfigurationDomainService configurationDomainService, final CodeValueRepositoryWrapper codeValueRepositoryWrapper,
            final AccountDetailsReadPlatformService accountDetailsReadPlatformService, final LoanRepositoryWrapper loanRepositoryWrapper,
            final ColumnValidator columnValidator, DatabaseSpecificSQLGenerator sqlGenerator, PaginationHelper paginationHelper,
            LoanAdditionalPropertiesRepository loanAdditionalPropertiesRepository) {
        this.context = context;
        this.loanRepositoryWrapper = loanRepositoryWrapper;
        this.applicationCurrencyRepository = applicationCurrencyRepository;
        this.loanProductReadPlatformService = loanProductReadPlatformService;
        this.clientReadPlatformService = clientReadPlatformService;
        this.groupReadPlatformService = groupReadPlatformService;
        this.loanDropdownReadPlatformService = loanDropdownReadPlatformService;
        this.fundReadPlatformService = fundReadPlatformService;
        this.chargeReadPlatformService = chargeReadPlatformService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.calendarReadPlatformService = calendarReadPlatformService;
        this.staffReadPlatformService = staffReadPlatformService;
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.paymentTypeReadPlatformService = paymentTypeReadPlatformService;
        this.loanRepaymentScheduleTransactionProcessorFactory = loanRepaymentScheduleTransactionProcessorFactory;
        this.floatingRatesReadPlatformService = floatingRatesReadPlatformService;
        this.loanUtilService = loanUtilService;
        this.configurationDomainService = configurationDomainService;
        this.accountDetailsReadPlatformService = accountDetailsReadPlatformService;
        this.columnValidator = columnValidator;
        this.loaanLoanMapper = new LoanMapper(sqlGenerator);
        this.sqlGenerator = sqlGenerator;
        this.paginationHelper = paginationHelper;
        this.codeValueRepositoryWrapper = codeValueRepositoryWrapper;
        this.loanAdditionalPropertiesRepository = loanAdditionalPropertiesRepository;
    }

    @Override
    public LoanAccountData retrieveOne(final Long loanId) {

        try {
            final AppUser currentUser = this.context.authenticatedUser();
            final String hierarchy = currentUser.getOffice().getHierarchy();
            final String hierarchySearchString = hierarchy + "%";

            final LoanMapper rm = new LoanMapper(sqlGenerator);

            final StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("select ");
            sqlBuilder.append(rm.loanSchema());
            sqlBuilder.append(" join m_office o on (o.id = c.office_id or o.id = g.office_id) ");
            sqlBuilder.append(" left join m_office transferToOffice on transferToOffice.id = c.transfer_to_office_id ");
            sqlBuilder.append(" where l.id=? and ( o.hierarchy like ? or transferToOffice.hierarchy like ?)");
            sqlBuilder.append(" GROUP BY l.id");

            LoanAccountData loanAccountData = this.jdbcTemplate.queryForObject(sqlBuilder.toString(), rm, loanId, hierarchySearchString,
                    hierarchySearchString);
            if (loanAccountData != null) {
                final Long prequalificationId = loanAccountData.getPrequalificationId();
                final Long clientId = loanAccountData.getClientId();
                if (prequalificationId != null && clientId != null) {
                    final String sql = "select " + this.prequalificationIndividualMappingsMapper.schema()
                            + " WHERE mc.id = ? AND mpg.id = ?";
                    final Collection<GroupPrequalificationData> prequalificationGroups = this.jdbcTemplate.query(sql,
                            this.prequalificationIndividualMappingsMapper, new Object[] { clientId, prequalificationId });
                    if (!CollectionUtils.isEmpty(prequalificationGroups)) {
                        final GroupPrequalificationData groupPrequalificationData = new ArrayList<>(prequalificationGroups).get(0);
                        loanAccountData.setPrequalificationData(groupPrequalificationData);
                        final EnumOptionData prequalificationType = groupPrequalificationData.getPrequalificationType();
                        if (prequalificationType != null
                                && PrequalificationType.INDIVIDUAL.name().equals(prequalificationType.getValue())) {
                            List<LoanAdditionProperties> loanAdditionalPropertiesList = this.loanAdditionalPropertiesRepository
                                    .findByClientIdAndLoanId(loanAccountData.getClientId(), loanId);
                            if (!CollectionUtils.isEmpty(loanAdditionalPropertiesList)) {
                                final LoanAdditionProperties loanAdditionProperties = loanAdditionalPropertiesList.get(0);
                                final LoanAdditionalData loanAdditionalData = loanAdditionProperties.toData();
                                loanAccountData.setLoanAdditionalData(loanAdditionalData);
                            }
                        }
                    }
                }
            }
            return loanAccountData;
        } catch (final EmptyResultDataAccessException e) {
            throw new LoanNotFoundException(loanId, e);
        }
    }

    @Override
    public LoanAccountData retrieveLoanByLoanAccount(String loanAccountNumber) {

        // final AppUser currentUser = this.context.authenticatedUser();
        this.context.authenticatedUser();
        final LoanMapper rm = new LoanMapper(sqlGenerator);

        final String sql = "select " + rm.loanSchema() + " where l.account_no=?";

        return this.jdbcTemplate.queryForObject(sql, rm, loanAccountNumber); // NOSONAR

    }

    @Override
    public List<LoanAccountData> retrieveGLIMChildLoansByGLIMParentAccount(String parentloanAccountNumber) {
        this.context.authenticatedUser();
        final LoanMapper rm = new LoanMapper(sqlGenerator);

        final String sql = "select " + rm.loanSchema()
                + " left join glim_parent_child_mapping as glim on glim.glim_child_account_id=l.account_no "
                + "where glim.glim_parent_account_id=?";

        return this.jdbcTemplate.query(sql, rm, parentloanAccountNumber); // NOSONAR

    }

    @Override
    public LoanScheduleData retrieveRepaymentSchedule(final Long loanId,
            final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedLoanData, Collection<DisbursementData> disbursementData,
            boolean isInterestRecalculationEnabled, BigDecimal totalPaidFeeCharges) {

        try {
            this.context.authenticatedUser();

            final LoanScheduleResultSetExtractor fullResultsetExtractor = new LoanScheduleResultSetExtractor(
                    repaymentScheduleRelatedLoanData, disbursementData, isInterestRecalculationEnabled, totalPaidFeeCharges);
            final String sql = "select " + fullResultsetExtractor.schema() + " where ls.loan_id = ? order by ls.loan_id, ls.installment";

            return this.jdbcTemplate.query(sql, fullResultsetExtractor, loanId); // NOSONAR
        } catch (final EmptyResultDataAccessException e) {
            throw new LoanNotFoundException(loanId, e);
        }
    }

    @Override
    public Collection<LoanTransactionData> retrieveLoanTransactions(final Long loanId) {
        try {
            this.context.authenticatedUser();

            final LoanTransactionsMapper rm = new LoanTransactionsMapper(sqlGenerator);

            // retrieve all loan transactions that are not invalid and have not
            // been 'contra'ed by another transaction
            // repayments at time of disbursement (e.g. charges)

            /***
             * TODO Vishwas: Remove references to "Contra" from the codebase
             ***/
            final String sql = "select " + rm.loanPaymentsSchema()
                    + " where tr.loan_id = ? and tr.transaction_type_enum not in (0, 3) and  (tr.is_reversed=false or tr.manually_adjusted_or_reversed = true) order by tr.transaction_date ASC,id ";
            return this.jdbcTemplate.query(sql, rm, loanId); // NOSONAR
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public Page<LoanAccountData> retrieveAll(final SearchParameters searchParameters) {

        final AppUser currentUser = this.context.authenticatedUser();
        final String hierarchy = currentUser.getOffice().getHierarchy();
        final String hierarchySearchString = hierarchy + "%";

        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select " + sqlGenerator.calcFoundRows() + " ");
        sqlBuilder.append(this.loaanLoanMapper.loanSchema());

        // TODO - for time being this will data scope list of loans returned to
        // only loans that have a client associated.
        // to support senario where loan has group_id only OR client_id will
        // probably require a UNION query
        // but that at present is an edge case
        sqlBuilder.append(" join m_office o on (o.id = c.office_id or o.id = g.office_id) ");
        sqlBuilder.append(" left join m_office transferToOffice on transferToOffice.id = c.transfer_to_office_id ");
        sqlBuilder.append(" where (o.hierarchy LIKE CONCAT(?, '%') OR ? like CONCAT(o.hierarchy, '%'))");

//        sqlBuilder.append(" where ( o.hierarchy like ? or transferToOffice.hierarchy like ?)");

        //        final Collection<AgencyData> agencyOptions = this.agencyReadPlatformService.retrieveAllByUser();
//        final Set<Long> agencyIds = agencyOptions.stream().map(AgencyData::getId).collect(Collectors.toSet());
//        final String agencyIdParams = StringUtils.join(agencyIds, ", ");
//        sqlBuilder.append(" WHERE agency.id IN ( ").append(agencyIdParams).append(")");

        int arrayPos = 2;
        List<Object> extraCriterias = new ArrayList<>();
        extraCriterias.add(hierarchySearchString);
        extraCriterias.add(hierarchySearchString);

        if (searchParameters != null) {
            final LocalDate disbursementStartDate = searchParameters.getDisbursementStartDate();
            final LocalDate disbursementEndDate = searchParameters.getDisbursementEndDate();
            final LocalDate approvalStartDate = searchParameters.getApprovalStartDate();
            final LocalDate approvalEndDate = searchParameters.getApprovalEndDate();
            final String clientNo = searchParameters.getClientNo();
            final Long agencyId = searchParameters.getAgencyId();
            final Long groupId = searchParameters.getGroupId();
            final Long centerId = searchParameters.getCenterId();
            final Long facilitatorId = searchParameters.getFacilitatorId();
            final Boolean isIndividualBusinessLoan = searchParameters.getIndividualBusinessLoan();

            String sqlQueryCriteria = searchParameters.getSqlSearch();
            if (StringUtils.isNotBlank(sqlQueryCriteria)) {
                SQLInjectionValidator.validateSQLInput(sqlQueryCriteria);
                sqlQueryCriteria = sqlQueryCriteria.replace("accountNo", "l.account_no");
                this.columnValidator.validateSqlInjection(sqlBuilder.toString(), sqlQueryCriteria);
                sqlBuilder.append(" and (").append(sqlQueryCriteria).append(")");
            }

            if (StringUtils.isNotBlank(searchParameters.getExternalId())) {
                sqlBuilder.append(" and l.external_id = ? ");
                extraCriterias.add(searchParameters.getExternalId());
                arrayPos = arrayPos + 1;
            }
            if (searchParameters.getOfficeId() != null) {
                sqlBuilder.append("and c.office_id =?");
                extraCriterias.add(searchParameters.getOfficeId());
                arrayPos = arrayPos + 1;
            }

            if (StringUtils.isNotBlank(searchParameters.getAccountNo())) {
                sqlBuilder.append(" and l.account_no = ?");
                extraCriterias.add(searchParameters.getAccountNo());
                arrayPos = arrayPos + 1;
            }
            if (StringUtils.isNotBlank(clientNo)) {
                sqlBuilder.append(" and c.account_no = ?");
                extraCriterias.add(clientNo);
                arrayPos = arrayPos + 1;
            }
            if (agencyId != null) {
                sqlBuilder.append(" and agency.id = ?");
                extraCriterias.add(agencyId);
                arrayPos = arrayPos + 1;
            }

            if (groupId != null) {
                sqlBuilder.append(" and g.id = ? ");
                extraCriterias.add(groupId);
                arrayPos = arrayPos + 1;
            }

            if (centerId != null) {
                sqlBuilder.append(" and center.id = ? ");
                extraCriterias.add(centerId);
                arrayPos = arrayPos + 1;
            }

            if (facilitatorId != null) {
                sqlBuilder.append(" and mpg.facilitator = ? ");
                extraCriterias.add(facilitatorId);
                arrayPos = arrayPos + 1;
            }

            if (Boolean.TRUE.equals(isIndividualBusinessLoan)) {
                sqlBuilder.append(" and ( c.id is not null and g.id is null ) ");
            }

            final DateTimeFormatter df = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd").toFormatter();
            if (disbursementStartDate != null || disbursementEndDate != null) {
                String disbursementStartDateString;
                String disbursementEndDateDateString;
                if (disbursementStartDate != null && disbursementEndDate != null) {
                    disbursementStartDateString = df.format(disbursementStartDate);
                    disbursementEndDateDateString = df.format(disbursementEndDate);
                    sqlBuilder.append(" and ( l.expected_disbursedon_date between '").append(disbursementStartDateString).append("' and '")
                            .append(disbursementEndDateDateString).append("' ) ");
                } else if (disbursementStartDate != null) {
                    disbursementStartDateString = df.format(disbursementStartDate);
                    sqlBuilder.append(" and ( l.expected_disbursedon_date >= '").append(disbursementStartDateString).append("' ) ");
                } else {
                    disbursementEndDateDateString = df.format(disbursementEndDate);
                    sqlBuilder.append(" and ( l.expected_disbursedon_date <= '").append(disbursementEndDateDateString).append("' ) ");
                }
            }

            if (approvalStartDate != null || approvalEndDate != null) {
                String approvalStartDateString;
                String approvalEndDateDateString;
                if (approvalStartDate != null && approvalEndDate != null) {
                    approvalStartDateString = df.format(approvalStartDate);
                    approvalEndDateDateString = df.format(approvalEndDate);
                    sqlBuilder.append(" and ( l.approvedon_date between '").append(approvalStartDateString).append("' and '")
                            .append(approvalEndDateDateString).append("' ) ");
                } else if (approvalStartDate != null) {
                    approvalStartDateString = df.format(approvalStartDate);
                    sqlBuilder.append(" and ( l.approvedon_date >= '").append(approvalStartDateString).append("' ) ");
                } else {
                    approvalEndDateDateString = df.format(approvalEndDate);
                    sqlBuilder.append(" and ( l.approvedon_date <= '").append(approvalEndDateDateString).append("' ) ");
                }
            }
            sqlBuilder.append(" group by l.id");
            if (searchParameters.isOrderByRequested()) {
                sqlBuilder.append(" order by ").append(searchParameters.getOrderBy());
                this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getOrderBy());

                if (searchParameters.isSortOrderProvided()) {
                    sqlBuilder.append(' ').append(searchParameters.getSortOrder());
                    this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getSortOrder());
                }
            }

            if (searchParameters.isLimited()) {
                sqlBuilder.append(" ");
                if (searchParameters.isOffset()) {
                    sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit(), searchParameters.getOffset()));
                } else {
                    sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit()));
                }
            }
        } else {
            sqlBuilder.append(" group by l.id");
        }
        final Object[] objectArray = extraCriterias.toArray();
        final Object[] finalObjectArray = Arrays.copyOf(objectArray, arrayPos);
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), finalObjectArray, this.loaanLoanMapper);
    }

    @Override
    public LoanAccountData retrieveTemplateWithClientAndProductDetails(final Long clientId, final Long productId) {

        this.context.authenticatedUser();

        final ClientData clientAccount = this.clientReadPlatformService.retrieveOne(clientId);
        final LocalDate expectedDisbursementDate = DateUtils.getBusinessLocalDate();
        LoanAccountData loanTemplateDetails = LoanAccountData.clientDefaults(clientAccount.id(), clientAccount.accountNo(),
                clientAccount.displayName(), clientAccount.officeId(), expectedDisbursementDate);

        if (productId != null) {
            final LoanProductData selectedProduct = this.loanProductReadPlatformService.retrieveLoanProduct(productId);
            loanTemplateDetails = LoanAccountData.populateLoanProductDefaults(loanTemplateDetails, selectedProduct);
        }

        return loanTemplateDetails;
    }

    @Override
    public LoanAccountData retrieveTemplateWithGroupAndProductDetails(final Long groupId, final Long productId) {

        this.context.authenticatedUser();

        final GroupGeneralData groupAccount = this.groupReadPlatformService.retrieveOne(groupId);
        final LocalDate expectedDisbursementDate = DateUtils.getBusinessLocalDate();
        LoanAccountData loanDetails = LoanAccountData.groupDefaults(groupAccount, expectedDisbursementDate);

        if (productId != null) {
            final LoanProductData selectedProduct = this.loanProductReadPlatformService.retrieveLoanProduct(productId);
            loanDetails = LoanAccountData.populateLoanProductDefaults(loanDetails, selectedProduct);
        }

        return loanDetails;
    }

    @Override
    public LoanAccountData retrieveTemplateWithCompleteGroupAndProductDetails(final Long groupId, final Long productId) {

        this.context.authenticatedUser();

        GroupGeneralData groupAccount = this.groupReadPlatformService.retrieveOne(groupId);
        // get group associations
        final Collection<ClientData> membersOfGroup = this.clientReadPlatformService.retrieveClientMembersOfGroup(groupId);
        if (!CollectionUtils.isEmpty(membersOfGroup)) {
            final Collection<ClientData> activeClientMembers = null;
            final Collection<CalendarData> calendarsData = null;
            final CalendarData collectionMeetingCalendar = null;
            final Collection<GroupRoleData> groupRoles = null;
            groupAccount = GroupGeneralData.withAssocations(groupAccount, membersOfGroup, activeClientMembers, groupRoles, calendarsData,
                    collectionMeetingCalendar);
        }

        final LocalDate expectedDisbursementDate = DateUtils.getBusinessLocalDate();
        LoanAccountData loanDetails = LoanAccountData.groupDefaults(groupAccount, expectedDisbursementDate);

        if (productId != null) {
            final LoanProductData selectedProduct = this.loanProductReadPlatformService.retrieveLoanProduct(productId);
            loanDetails = LoanAccountData.populateLoanProductDefaults(loanDetails, selectedProduct);
        }

        return loanDetails;
    }

    @Override
    public LoanTransactionData retrieveLoanTransactionTemplate(final Long loanId) {

        String hierarchy = this.context.authenticatedUser().getOffice().getHierarchy();

        RepaymentTransactionTemplateMapper mapper = new RepaymentTransactionTemplateMapper(sqlGenerator);
        String sql = "select " + mapper.schema();
        LoanTransactionData loanTransactionData = this.jdbcTemplate.queryForObject(sql, mapper, // NOSONAR
                LoanTransactionType.REPAYMENT.getValue(), LoanTransactionType.REPAYMENT.getValue(), loanId, loanId);
        final Collection<PaymentTypeData> paymentOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        BankAccountReadPlatformServiceImpl.BankAccountMapper bankAccountMapper = new BankAccountReadPlatformServiceImpl.BankAccountMapper();
        String bankAccSql = "select " + bankAccountMapper.schema();
        bankAccSql = bankAccSql + " where (mo.hierarchy LIKE CONCAT(?, '%') OR ? like CONCAT(mo.hierarchy, '%')) ";
        List<BankAccountData> bankAccounts = this.jdbcTemplate.query(bankAccSql, bankAccountMapper, hierarchy, hierarchy);
        LoanTransactionData loanTransactionDataTemplate = LoanTransactionData.templateOnTop(loanTransactionData, paymentOptions);
        loanTransactionDataTemplate.setBankAccounts(bankAccounts);
        return loanTransactionDataTemplate;
    }

    @Override
    public LoanTransactionData retrieveLoanPrePaymentTemplate(final LoanTransactionType repaymentTransactionType, final Long loanId,
            LocalDate onDate) {

        this.context.authenticatedUser();
        this.loanUtilService.validateRepaymentTransactionType(repaymentTransactionType);

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        loan.setHelpers(null, null, loanRepaymentScheduleTransactionProcessorFactory);

        final MonetaryCurrency currency = loan.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);

        final CurrencyData currencyData = applicationCurrency.toData();

        final LocalDate earliestUnpaidInstallmentDate = DateUtils.getBusinessLocalDate();
        final LocalDate recalculateFrom = null;
        final ScheduleGeneratorDTO scheduleGeneratorDTO = loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
        final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = loan.fetchPrepaymentDetail(scheduleGeneratorDTO, onDate);
        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(repaymentTransactionType);
        final Collection<PaymentTypeData> paymentOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        final BigDecimal outstandingLoanBalance = loanRepaymentScheduleInstallment.getPrincipalOutstanding(currency).getAmount();
        final BigDecimal unrecognizedIncomePortion = null;
        BigDecimal adjustedChargeAmount = adjustPrepayInstallmentCharge(loan, onDate);
        return new LoanTransactionData(null, null, null, transactionType, null, currencyData, earliestUnpaidInstallmentDate,
                loanRepaymentScheduleInstallment.getTotalOutstanding(currency).getAmount().subtract(adjustedChargeAmount),
                loan.getNetDisbursalAmount(), loanRepaymentScheduleInstallment.getPrincipalOutstanding(currency).getAmount(),
                loanRepaymentScheduleInstallment.getInterestOutstanding(currency).getAmount(),
                loanRepaymentScheduleInstallment.getFeeChargesOutstanding(currency).getAmount().subtract(adjustedChargeAmount),
                loanRepaymentScheduleInstallment.getPenaltyChargesOutstanding(currency).getAmount(), null, unrecognizedIncomePortion,
                paymentOptions, null, null, null, outstandingLoanBalance, false);
    }

    private BigDecimal adjustPrepayInstallmentCharge(Loan loan, final LocalDate onDate) {
        BigDecimal chargeAmount = BigDecimal.ZERO;
        /*
         * for(LoanCharge loanCharge: loan.charges()){ if(loanCharge.isInstalmentFee() &&
         * loanCharge.getCharge().getChargeCalculation()==ChargeCalculationType. FLAT.getValue()){ for
         * (LoanRepaymentScheduleInstallment installment : loan.getRepaymentScheduleInstallments()) {
         * if(onDate.isBefore(installment.getDueDate())){ LoanInstallmentCharge loanInstallmentCharge =
         * loanCharge.getInstallmentLoanCharge(installment.getInstallmentNumber( )); if(loanInstallmentCharge != null){
         * chargeAmount = chargeAmount.add(loanInstallmentCharge.getAmountOutstanding()); }
         *
         * break; } } } }
         */
        return chargeAmount;
    }

    @Override
    public LoanTransactionData retrieveWaiveInterestDetails(final Long loanId) {

        AppUser currentUser = this.context.authenticatedUser();

        // TODO - KW -OPTIMIZE - write simple sql query to fetch back overdue
        // interest that can be waived along with the date of repayment period
        // interest is overdue.
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        final MonetaryCurrency currency = loan.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);
        final CurrencyData currencyData = applicationCurrency.toData();

        final LoanTransaction waiveOfInterest = loan.deriveDefaultInterestWaiverTransaction();

        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(LoanTransactionType.WAIVE_INTEREST);

        final BigDecimal amount = waiveOfInterest.getAmount(currency).getAmount();
        final BigDecimal outstandingLoanBalance = null;
        final BigDecimal unrecognizedIncomePortion = null;
        return new LoanTransactionData(null, null, null, transactionType, null, currencyData, waiveOfInterest.getTransactionDate(), amount,
                loan.getNetDisbursalAmount(), null, null, null, null, null, null, null, null, outstandingLoanBalance,
                unrecognizedIncomePortion, false);
    }

    @Override
    public LoanTransactionData retrieveNewClosureDetails() {

        this.context.authenticatedUser();
        final BigDecimal outstandingLoanBalance = null;
        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(LoanTransactionType.WRITEOFF);
        final BigDecimal unrecognizedIncomePortion = null;
        return new LoanTransactionData(null, null, null, transactionType, null, null, DateUtils.getBusinessLocalDate(), null, null, null,
                null, null, null, null, null, null, null, outstandingLoanBalance, unrecognizedIncomePortion, false);

    }

    @Override
    public LoanApprovalData retrieveApprovalTemplate(final Long loanId) {
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        return new LoanApprovalData(loan.getProposedPrincipal(), DateUtils.getBusinessLocalDate(), loan.getNetDisbursalAmount(), null);
    }

    @Override
    public LoanTransactionData retrieveDisbursalTemplate(final Long loanId, boolean paymentDetailsRequired) {
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(LoanTransactionType.DISBURSEMENT);
        Collection<PaymentTypeData> paymentOptions = null;
        if (paymentDetailsRequired) {
            paymentOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        }

        return LoanTransactionData.loanTransactionDataForDisbursalTemplate(transactionType,
                loan.getExpectedDisbursedOnLocalDateForTemplate(), loan.getDisburseAmountForTemplate(), loan.getNetDisbursalAmount(),
                paymentOptions, loan.retriveLastEmiAmount(), loan.getNextPossibleRepaymentDateForRescheduling());

    }

    @Override
    public Integer retrieveNumberOfRepayments(final Long loanId) {
        this.context.authenticatedUser();
        return this.loanRepositoryWrapper.getNumberOfRepayments(loanId);
    }

    @Override
    public List<LoanRepaymentScheduleInstallmentData> getRepaymentDataResponse(final Long loanId) {
        this.context.authenticatedUser();
        final List<LoanRepaymentScheduleInstallment> loanRepaymentScheduleInstallments = this.loanRepositoryWrapper
                .getLoanRepaymentScheduleInstallments(loanId);
        List<LoanRepaymentScheduleInstallmentData> loanRepaymentScheduleInstallmentData = new ArrayList<>();

        for (LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : loanRepaymentScheduleInstallments) {
            loanRepaymentScheduleInstallmentData.add(LoanRepaymentScheduleInstallmentData.instanceOf(
                    loanRepaymentScheduleInstallment.getId(), loanRepaymentScheduleInstallment.getInstallmentNumber(),
                    loanRepaymentScheduleInstallment.getDueDate(), loanRepaymentScheduleInstallment
                            .getTotalOutstanding(loanRepaymentScheduleInstallment.getLoan().getCurrency()).getAmount()));
        }
        return loanRepaymentScheduleInstallmentData;
    }

    @Override
    public LoanTransactionData retrieveLoanTransaction(final Long loanId, final Long transactionId) {
        this.context.authenticatedUser();
        try {
            final LoanTransactionsMapper rm = new LoanTransactionsMapper(sqlGenerator);
            final String sql = "select " + rm.loanPaymentsSchema() + " where l.id = ? and tr.id = ? ";
            return this.jdbcTemplate.queryForObject(sql, rm, loanId, transactionId); // NOSONAR
        } catch (final EmptyResultDataAccessException e) {
            throw new LoanTransactionNotFoundException(transactionId, e);
        }
    }

    private static final class LoanMapper implements RowMapper<LoanAccountData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;

        LoanMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String loanSchema() {

            return "l.id as id, mlrs.first_duedate As firstInstallmentDate, l.account_no as accountNo, l.contract as contractNo, l.external_id as externalId, l.fund_id as fundId, f.name as fundName, l.prequalification_id AS prequalificationId, "
                    + " l.loan_type_enum as loanType, l.loanpurpose_cv_id as loanPurposeId, cv.code_value as loanPurposeName,"
                    + " lp.id as loanProductId, lp.name as loanProductName, lp.description as loanProductDescription,"
                    + " coalesce(lp.required_guarantee_percent, 0) * l.principal_amount as requiredGuaranteeAmount,"
                    + " lp.is_linked_to_floating_interest_rates as isLoanProductLinkedToFloatingRate, "
                    + " lp.allow_variabe_installments as isvariableInstallmentsAllowed, "
                    + " lp.allow_multiple_disbursals as multiDisburseLoan,"
                    + " lp.can_define_fixed_emi_amount as canDefineInstallmentAmount,"
                    + " c.id as clientId, c.account_no as clientAccountNo, c.display_name as clientName, c.office_id as clientOfficeId,"
                    + " g.id as groupId, g.account_no as groupAccountNo, g.display_name as groupName,"
                    + " g.office_id as groupOfficeId, g.staff_id As groupStaffId , g.parent_id as groupParentId, (select mg.display_name from m_group mg where mg.id = g.parent_id) as centerName, "
                    + " g.hierarchy As groupHierarchy , g.level_id as groupLevel, g.external_id As groupExternalId, "
                    + " g.status_enum as statusEnum, g.activation_date as activationDate, "
                    + " l.submittedon_date as submittedOnDate, sbu.username as submittedByUsername, sbu.firstname as submittedByFirstname, sbu.lastname as submittedByLastname,"
                    + " l.rejectedon_date as rejectedOnDate, rbu.username as rejectedByUsername, rbu.firstname as rejectedByFirstname, rbu.lastname as rejectedByLastname,"
                    + " l.withdrawnon_date as withdrawnOnDate, wbu.username as withdrawnByUsername, wbu.firstname as withdrawnByFirstname, wbu.lastname as withdrawnByLastname,"
                    + " l.approvedon_date as approvedOnDate, abu.username as approvedByUsername, abu.firstname as approvedByFirstname, abu.lastname as approvedByLastname,"
                    + " l.expected_disbursedon_date as expectedDisbursementDate, l.disbursedon_date as actualDisbursementDate, dbu.username as disbursedByUsername, dbu.firstname as disbursedByFirstname, dbu.lastname as disbursedByLastname,"
                    + " l.closedon_date as closedOnDate, cbu.username as closedByUsername, cbu.firstname as closedByFirstname, cbu.lastname as closedByLastname, l.writtenoffon_date as writtenOffOnDate, "
                    + " l.expected_firstrepaymenton_date as expectedFirstRepaymentOnDate, l.interest_calculated_from_date as interestChargedFromDate, l.expected_maturedon_date as expectedMaturityDate, "
                    + " l.principal_amount_proposed as proposedPrincipal, l.principal_amount as principal, l.approved_principal as approvedPrincipal, l.net_disbursal_amount as netDisbursalAmount, l.arrearstolerance_amount as inArrearsTolerance, l.number_of_repayments as numberOfRepayments, l.repay_every as repaymentEvery,"
                    + " l.grace_on_principal_periods as graceOnPrincipalPayment, l.recurring_moratorium_principal_periods as recurringMoratoriumOnPrincipalPeriods, l.grace_on_interest_periods as graceOnInterestPayment, l.grace_interest_free_periods as graceOnInterestCharged,l.grace_on_arrears_ageing as graceOnArrearsAgeing,"
                    + " l.nominal_interest_rate_per_period as interestRatePerPeriod, l.annual_nominal_interest_rate as annualInterestRate, "
                    + " l.repayment_period_frequency_enum as repaymentFrequencyType, l.interest_period_frequency_enum as interestRateFrequencyType, "
                    + " l.term_frequency as termFrequency, l.term_period_frequency_enum as termPeriodFrequencyType, "
                    + " l.amortization_method_enum as amortizationType, l.interest_method_enum as interestType, l.is_equal_amortization as isEqualAmortization, l.interest_calculated_in_period_enum as interestCalculationPeriodType,"
                    + " l.fixed_principal_percentage_per_installment fixedPrincipalPercentagePerInstallment, "
                    + " l.allow_partial_period_interest_calcualtion as allowPartialPeriodInterestCalcualtion,"
                    + " l.loan_status_id as lifeCycleStatusId, l.loan_transaction_strategy_id as transactionStrategyId, "
                    + " lps.name as transactionStrategyName, "
                    + " l.currency_code as currencyCode, l.currency_digits as currencyDigits, l.currency_multiplesof as inMultiplesOf, rc."
                    + sqlGenerator.escape("name")
                    + " as currencyName, rc.display_symbol as currencyDisplaySymbol, rc.internationalized_name_code as currencyNameCode, rc.int_code as intCode, "
                    + " l.loan_officer_id as loanOfficerId, s.display_name as loanOfficerName, "
                    + " l.principal_disbursed_derived as principalDisbursed," + " l.principal_repaid_derived as principalPaid,"
                    + " l.principal_writtenoff_derived as principalWrittenOff,"
                    + " l.principal_outstanding_derived as principalOutstanding," + " l.interest_charged_derived as interestCharged,"
                    + " l.interest_repaid_derived as interestPaid," + " l.interest_waived_derived as interestWaived,"
                    + " l.interest_writtenoff_derived as interestWrittenOff," + " l.interest_outstanding_derived as interestOutstanding,"
                    + " l.fee_charges_charged_derived as feeChargesCharged,"
                    + " l.total_charges_due_at_disbursement_derived as feeChargesDueAtDisbursementCharged,"
                    + " l.fee_charges_repaid_derived as feeChargesPaid," + " l.fee_charges_waived_derived as feeChargesWaived,"
                    + " l.fee_charges_writtenoff_derived as feeChargesWrittenOff,"
                    + " l.fee_charges_outstanding_derived as feeChargesOutstanding,"
                    + " l.penalty_charges_charged_derived as penaltyChargesCharged,"
                    + " l.penalty_charges_repaid_derived as penaltyChargesPaid,"
                    + " l.penalty_charges_waived_derived as penaltyChargesWaived,"
                    + " l.penalty_charges_writtenoff_derived as penaltyChargesWrittenOff,"
                    + " l.penalty_charges_outstanding_derived as penaltyChargesOutstanding,"
                    + " l.total_expected_repayment_derived as totalExpectedRepayment," + " l.total_repayment_derived as totalRepayment,"
                    + " l.total_expected_costofloan_derived as totalExpectedCostOfLoan," + " l.total_costofloan_derived as totalCostOfLoan,"
                    + " l.total_waived_derived as totalWaived," + " l.total_writtenoff_derived as totalWrittenOff,"
                    + " l.writeoff_reason_cv_id as writeoffReasonId," + " codev.code_value as writeoffReason,"
                    + " l.total_outstanding_derived as totalOutstanding," + " l.total_overpaid_derived as totalOverpaid,"
                    + " l.fixed_emi_amount as fixedEmiAmount," + " l.max_outstanding_loan_balance as outstandingLoanBalance,"
                    + " l.loan_sub_status_id as loanSubStatusId," + " la.principal_overdue_derived as principalOverdue,"
                    + " la.interest_overdue_derived as interestOverdue," + " la.fee_charges_overdue_derived as feeChargesOverdue,"
                    + " la.penalty_charges_overdue_derived as penaltyChargesOverdue," + " la.total_overdue_derived as totalOverdue,"
                    + " la.overdue_since_date_derived as overdueSinceDate,"
                    + " l.sync_disbursement_with_meeting as syncDisbursementWithMeeting,"
                    + " l.loan_counter as loanCounter, l.loan_product_counter as loanProductCounter,"
                    + " l.is_npa as isNPA, l.days_in_month_enum as daysInMonth, l.days_in_year_enum as daysInYear, "
                    + " l.interest_recalculation_enabled as isInterestRecalculationEnabled, "
                    + " lir.id as lirId, lir.loan_id as loanId, lir.compound_type_enum as compoundType, lir.reschedule_strategy_enum as rescheduleStrategy, "
                    + " lir.rest_frequency_type_enum as restFrequencyEnum, lir.rest_frequency_interval as restFrequencyInterval, "
                    + " lir.rest_frequency_nth_day_enum as restFrequencyNthDayEnum, "
                    + " lir.rest_frequency_weekday_enum as restFrequencyWeekDayEnum, "
                    + " lir.rest_frequency_on_day as restFrequencyOnDay, "
                    + " lir.compounding_frequency_type_enum as compoundingFrequencyEnum, lir.compounding_frequency_interval as compoundingInterval, "
                    + " lir.compounding_frequency_nth_day_enum as compoundingFrequencyNthDayEnum, "
                    + " lir.compounding_frequency_weekday_enum as compoundingFrequencyWeekDayEnum, "
                    + " lir.compounding_frequency_on_day as compoundingFrequencyOnDay, "
                    + " lir.is_compounding_to_be_posted_as_transaction as isCompoundingToBePostedAsTransaction, "
                    + " lir.allow_compounding_on_eod as allowCompoundingOnEod, "
                    + " l.is_floating_interest_rate as isFloatingInterestRate, "
                    + " l.interest_rate_differential as interestRateDifferential, "
                    + " l.create_standing_instruction_at_disbursement as createStandingInstructionAtDisbursement, "
                    + " lpvi.minimum_gap as minimuminstallmentgap, lpvi.maximum_gap as maximuminstallmentgap, "
                    + " lp.can_use_for_topup as canUseForTopup, " + " l.is_topup as isTopup, " + " topup.closure_loan_id as closureLoanId, "
                    + " l.total_recovered_derived as totalRecovered" + ", topuploan.account_no as closureLoanAccountNo, "
                    + " (ifnull(sa.account_balance_derived, 0)- ifnull(sa.total_savings_amount_on_hold,0) - ifnull(on_hold_funds_derived,0)) as actualGuaranteeAmount, "
                    + " topup.topup_amount as topupAmount " + " from m_loan l" //
                    + " join m_product_loan lp on lp.id = l.product_id" //
                    + " left join m_loan_recalculation_details lir on lir.loan_id = l.id " + " join m_currency rc on rc."
                    + sqlGenerator.escape("code") + " = l.currency_code" //
                    + " left join m_client c on c.id = l.client_id" //
                    + " left join m_group g on g.id = l.group_id" //
                    + " left join m_group center on center.id = g.parent_id" //
                    + " left join m_office centeroffice on centeroffice.id = center.office_id"
                    + " left join m_office centerounder on centeroffice.hierarchy LIKE CONCAT(centerounder.hierarchy, '%')"
                    + " left join m_agency agency on agency.linked_office_id = centerounder.id"
                    + " left join m_agency mag on mag.responsible_user_id = center.responsible_user_id "
                    + " left join m_prequalification_group mpg on mpg.id = g.prequalification_id" //
                    + " left join m_loan_arrears_aging la on la.loan_id = l.id" //
                    + " left join m_fund f on f.id = l.fund_id" //
                    + " left join m_staff s on s.id = l.loan_officer_id" //
                    + " left join m_appuser sbu on sbu.id = l.created_by" + " left join m_appuser rbu on rbu.id = l.rejectedon_userid"
                    + " left join m_appuser wbu on wbu.id = l.withdrawnon_userid"
                    + " left join m_appuser abu on abu.id = l.approvedon_userid"
                    + " left join m_appuser dbu on dbu.id = l.disbursedon_userid" + " left join m_appuser cbu on cbu.id = l.closedon_userid"
                    + " left join m_code_value cv on cv.id = l.loanpurpose_cv_id"
                    + " left join m_code_value codev on codev.id = l.writeoff_reason_cv_id"
                    + " left join ref_loan_transaction_processing_strategy lps on lps.id = l.loan_transaction_strategy_id"
                    + " left join m_product_loan_variable_installment_config lpvi on lpvi.loan_product_id = l.product_id"
                    + " left join m_loan_topup as topup on l.id = topup.loan_id"
                    + " left join m_loan as topuploan on topuploan.id = topup.closure_loan_id"
                    + " LEFT JOIN (SELECT DISTINCT sc.loan_id AS loan_id, sc.duedate AS first_duedate"
                    + "            FROM m_loan_repayment_schedule sc " + "            WHERE sc.installment = 1 "
                    + "            ORDER BY sc.duedate DESC " + "            ) mlrs ON mlrs.loan_id = l.id "
                    + "left join m_savings_account sa on sa.client_id = c.id";

        }

        @Override
        public LoanAccountData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final Integer intCode = JdbcSupport.getInteger(rs, "intCode");
            final CurrencyData currencyData = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf,
                    currencyDisplaySymbol, currencyNameCode, intCode);

            final Long id = rs.getLong("id");
            final Long prequalificationId = rs.getLong("prequalificationId");
            final String accountNo = rs.getString("accountNo");
            final String externalId = rs.getString("externalId");

            final Long clientId = JdbcSupport.getLong(rs, "clientId");
            final String clientAccountNo = rs.getString("clientAccountNo");
            final Long clientOfficeId = JdbcSupport.getLong(rs, "clientOfficeId");
            final String clientName = rs.getString("clientName");

            final Long groupId = JdbcSupport.getLong(rs, "groupId");
            final String groupName = rs.getString("groupName");
            final String groupAccountNo = rs.getString("groupAccountNo");
            final String groupExternalId = rs.getString("groupExternalId");
            final Long groupOfficeId = JdbcSupport.getLong(rs, "groupOfficeId");
            final Long groupStaffId = JdbcSupport.getLong(rs, "groupStaffId");
            final Long groupParentId = JdbcSupport.getLong(rs, "groupParentId");
            final String centerName = rs.getString("centerName");
            final String groupHierarchy = rs.getString("groupHierarchy");
            final String groupLevel = rs.getString("groupLevel");

            final Integer loanTypeId = JdbcSupport.getInteger(rs, "loanType");
            final EnumOptionData loanType = AccountEnumerations.loanType(loanTypeId);

            final Long fundId = JdbcSupport.getLong(rs, "fundId");
            final String fundName = rs.getString("fundName");

            final Long loanOfficerId = JdbcSupport.getLong(rs, "loanOfficerId");
            final String loanOfficerName = rs.getString("loanOfficerName");

            final Long loanPurposeId = JdbcSupport.getLong(rs, "loanPurposeId");
            final String loanPurposeName = rs.getString("loanPurposeName");

            final Long loanProductId = JdbcSupport.getLong(rs, "loanProductId");
            final String loanProductName = rs.getString("loanProductName");
            final String loanProductDescription = rs.getString("loanProductDescription");
            final boolean isLoanProductLinkedToFloatingRate = rs.getBoolean("isLoanProductLinkedToFloatingRate");
            final Boolean multiDisburseLoan = rs.getBoolean("multiDisburseLoan");
            final Boolean canDefineInstallmentAmount = rs.getBoolean("canDefineInstallmentAmount");
            final BigDecimal outstandingLoanBalance = rs.getBigDecimal("outstandingLoanBalance");

            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
            final String submittedByUsername = rs.getString("submittedByUsername");
            final String submittedByFirstname = rs.getString("submittedByFirstname");
            final String submittedByLastname = rs.getString("submittedByLastname");

            final LocalDate rejectedOnDate = JdbcSupport.getLocalDate(rs, "rejectedOnDate");
            final String rejectedByUsername = rs.getString("rejectedByUsername");
            final String rejectedByFirstname = rs.getString("rejectedByFirstname");
            final String rejectedByLastname = rs.getString("rejectedByLastname");

            final LocalDate withdrawnOnDate = JdbcSupport.getLocalDate(rs, "withdrawnOnDate");
            final String withdrawnByUsername = rs.getString("withdrawnByUsername");
            final String withdrawnByFirstname = rs.getString("withdrawnByFirstname");
            final String withdrawnByLastname = rs.getString("withdrawnByLastname");

            final LocalDate approvedOnDate = JdbcSupport.getLocalDate(rs, "approvedOnDate");
            final String approvedByUsername = rs.getString("approvedByUsername");
            final String approvedByFirstname = rs.getString("approvedByFirstname");
            final String approvedByLastname = rs.getString("approvedByLastname");

            final LocalDate expectedDisbursementDate = JdbcSupport.getLocalDate(rs, "expectedDisbursementDate");
            final LocalDate actualDisbursementDate = JdbcSupport.getLocalDate(rs, "actualDisbursementDate");
            final String disbursedByUsername = rs.getString("disbursedByUsername");
            final String disbursedByFirstname = rs.getString("disbursedByFirstname");
            final String disbursedByLastname = rs.getString("disbursedByLastname");

            final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedOnDate");
            final String closedByUsername = rs.getString("closedByUsername");
            final String closedByFirstname = rs.getString("closedByFirstname");
            final String closedByLastname = rs.getString("closedByLastname");

            final LocalDate writtenOffOnDate = JdbcSupport.getLocalDate(rs, "writtenOffOnDate");
            final Long writeoffReasonId = JdbcSupport.getLong(rs, "writeoffReasonId");
            final String writeoffReason = rs.getString("writeoffReason");
            final LocalDate expectedMaturityDate = JdbcSupport.getLocalDate(rs, "expectedMaturityDate");

            final Boolean isvariableInstallmentsAllowed = rs.getBoolean("isvariableInstallmentsAllowed");
            final Integer minimumGap = rs.getInt("minimuminstallmentgap");
            final Integer maximumGap = rs.getInt("maximuminstallmentgap");

            final LoanApplicationTimelineData timeline = new LoanApplicationTimelineData(submittedOnDate, submittedByUsername,
                    submittedByFirstname, submittedByLastname, rejectedOnDate, rejectedByUsername, rejectedByFirstname, rejectedByLastname,
                    withdrawnOnDate, withdrawnByUsername, withdrawnByFirstname, withdrawnByLastname, approvedOnDate, approvedByUsername,
                    approvedByFirstname, approvedByLastname, expectedDisbursementDate, actualDisbursementDate, disbursedByUsername,
                    disbursedByFirstname, disbursedByLastname, closedOnDate, closedByUsername, closedByFirstname, closedByLastname,
                    expectedMaturityDate, writtenOffOnDate, closedByUsername, closedByFirstname, closedByLastname);

            final BigDecimal principal = rs.getBigDecimal("principal");
            final BigDecimal approvedPrincipal = rs.getBigDecimal("approvedPrincipal");
            final BigDecimal proposedPrincipal = rs.getBigDecimal("proposedPrincipal");
            final BigDecimal netDisbursalAmount = rs.getBigDecimal("netDisbursalAmount");
            final BigDecimal totalOverpaid = rs.getBigDecimal("totalOverpaid");
            final BigDecimal inArrearsTolerance = rs.getBigDecimal("inArrearsTolerance");

            final Integer numberOfRepayments = JdbcSupport.getInteger(rs, "numberOfRepayments");
            final Integer repaymentEvery = JdbcSupport.getInteger(rs, "repaymentEvery");
            final BigDecimal interestRatePerPeriod = rs.getBigDecimal("interestRatePerPeriod");
            final BigDecimal annualInterestRate = rs.getBigDecimal("annualInterestRate");
            final BigDecimal interestRateDifferential = rs.getBigDecimal("interestRateDifferential");
            final boolean isFloatingInterestRate = rs.getBoolean("isFloatingInterestRate");

            final Integer graceOnPrincipalPayment = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnPrincipalPayment");
            final Integer recurringMoratoriumOnPrincipalPeriods = JdbcSupport.getIntegerDefaultToNullIfZero(rs,
                    "recurringMoratoriumOnPrincipalPeriods");
            final Integer graceOnInterestPayment = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnInterestPayment");
            final Integer graceOnInterestCharged = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnInterestCharged");
            final Integer graceOnArrearsAgeing = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnArrearsAgeing");

            final Integer termFrequency = JdbcSupport.getInteger(rs, "termFrequency");
            final Integer termPeriodFrequencyTypeInt = JdbcSupport.getInteger(rs, "termPeriodFrequencyType");
            final EnumOptionData termPeriodFrequencyType = LoanEnumerations.termFrequencyType(termPeriodFrequencyTypeInt);

            final int repaymentFrequencyTypeInt = JdbcSupport.getInteger(rs, "repaymentFrequencyType");
            final EnumOptionData repaymentFrequencyType = LoanEnumerations.repaymentFrequencyType(repaymentFrequencyTypeInt);

            final int interestRateFrequencyTypeInt = JdbcSupport.getInteger(rs, "interestRateFrequencyType");
            final EnumOptionData interestRateFrequencyType = LoanEnumerations.interestRateFrequencyType(interestRateFrequencyTypeInt);

            final Long transactionStrategyId = JdbcSupport.getLong(rs, "transactionStrategyId");
            final String transactionStrategyName = rs.getString("transactionStrategyName");

            final int amortizationTypeInt = JdbcSupport.getInteger(rs, "amortizationType");
            final int interestTypeInt = JdbcSupport.getInteger(rs, "interestType");
            final int interestCalculationPeriodTypeInt = JdbcSupport.getInteger(rs, "interestCalculationPeriodType");
            final boolean isEqualAmortization = rs.getBoolean("isEqualAmortization");
            final EnumOptionData amortizationType = LoanEnumerations.amortizationType(amortizationTypeInt);
            final BigDecimal fixedPrincipalPercentagePerInstallment = rs.getBigDecimal("fixedPrincipalPercentagePerInstallment");
            final EnumOptionData interestType = LoanEnumerations.interestType(interestTypeInt);
            final EnumOptionData interestCalculationPeriodType = LoanEnumerations
                    .interestCalculationPeriodType(interestCalculationPeriodTypeInt);
            final Boolean allowPartialPeriodInterestCalcualtion = rs.getBoolean("allowPartialPeriodInterestCalcualtion");

            final Integer lifeCycleStatusId = JdbcSupport.getInteger(rs, "lifeCycleStatusId");
            final LoanStatusEnumData status = LoanEnumerations.status(lifeCycleStatusId);

            final Integer loanSubStatusId = JdbcSupport.getInteger(rs, "loanSubStatusId");
            EnumOptionData loanSubStatus = null;
            if (loanSubStatusId != null) {
                loanSubStatus = LoanSubStatus.loanSubStatus(loanSubStatusId);
            }

            // settings
            LocalDate expectedFirstRepaymentOnDate = JdbcSupport.getLocalDate(rs, "expectedFirstRepaymentOnDate");
            if (expectedFirstRepaymentOnDate == null) {
                expectedFirstRepaymentOnDate = JdbcSupport.getLocalDate(rs, "firstInstallmentDate");
            }

            final LocalDate interestChargedFromDate = JdbcSupport.getLocalDate(rs, "interestChargedFromDate");

            final Boolean syncDisbursementWithMeeting = rs.getBoolean("syncDisbursementWithMeeting");

            final BigDecimal feeChargesDueAtDisbursementCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs,
                    "feeChargesDueAtDisbursementCharged");

            LoanSummaryData loanSummary = null;
            Boolean inArrears = false;
            if (status.id().intValue() >= 300) {

                // loan summary
                final BigDecimal principalDisbursed = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalDisbursed");
                final BigDecimal principalPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalPaid");
                final BigDecimal principalWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalWrittenOff");
                final BigDecimal principalOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalOutstanding");
                final BigDecimal principalOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalOverdue");

                final BigDecimal interestCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestCharged");
                final BigDecimal interestPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestPaid");
                final BigDecimal interestWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWaived");
                final BigDecimal interestWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWrittenOff");
                final BigDecimal interestOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestOutstanding");
                final BigDecimal interestOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestOverdue");

                final BigDecimal feeChargesCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesCharged");
                final BigDecimal feeChargesPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesPaid");
                final BigDecimal feeChargesWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesWaived");
                final BigDecimal feeChargesWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesWrittenOff");
                final BigDecimal feeChargesOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesOutstanding");
                final BigDecimal feeChargesOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesOverdue");

                final BigDecimal penaltyChargesCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesCharged");
                final BigDecimal penaltyChargesPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesPaid");
                final BigDecimal penaltyChargesWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesWaived");
                final BigDecimal penaltyChargesWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesWrittenOff");
                final BigDecimal penaltyChargesOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesOutstanding");
                final BigDecimal penaltyChargesOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesOverdue");

                final BigDecimal totalExpectedRepayment = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalExpectedRepayment");
                final BigDecimal totalRepayment = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalRepayment");
                final BigDecimal totalExpectedCostOfLoan = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalExpectedCostOfLoan");
                final BigDecimal totalCostOfLoan = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalCostOfLoan");
                final BigDecimal totalWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalWaived");
                final BigDecimal totalWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalWrittenOff");
                final BigDecimal totalOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalOutstanding");
                final BigDecimal totalOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalOverdue");
                final BigDecimal totalRecovered = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalRecovered");

                final LocalDate overdueSinceDate = JdbcSupport.getLocalDate(rs, "overdueSinceDate");
                if (overdueSinceDate != null) {
                    inArrears = true;
                }

                loanSummary = new LoanSummaryData(currencyData, principalDisbursed, principalPaid, principalWrittenOff,
                        principalOutstanding, principalOverdue, interestCharged, interestPaid, interestWaived, interestWrittenOff,
                        interestOutstanding, interestOverdue, feeChargesCharged, feeChargesDueAtDisbursementCharged, feeChargesPaid,
                        feeChargesWaived, feeChargesWrittenOff, feeChargesOutstanding, feeChargesOverdue, penaltyChargesCharged,
                        penaltyChargesPaid, penaltyChargesWaived, penaltyChargesWrittenOff, penaltyChargesOutstanding,
                        penaltyChargesOverdue, totalExpectedRepayment, totalRepayment, totalExpectedCostOfLoan, totalCostOfLoan,
                        totalWaived, totalWrittenOff, totalOutstanding, totalOverdue, overdueSinceDate, writeoffReasonId, writeoffReason,
                        totalRecovered);
            }

            GroupGeneralData groupData = null;
            if (groupId != null) {
                final Integer groupStatusEnum = JdbcSupport.getInteger(rs, "statusEnum");
                final EnumOptionData groupStatus = ClientEnumerations.status(groupStatusEnum);
                final LocalDate activationDate = JdbcSupport.getLocalDate(rs, "activationDate");
                groupData = GroupGeneralData.instance(groupId, groupAccountNo, groupName, groupExternalId, groupStatus, activationDate,
                        groupOfficeId, null, groupParentId, centerName, groupStaffId, null, groupHierarchy, groupLevel, null, null, null);
            }

            final Integer loanCounter = JdbcSupport.getInteger(rs, "loanCounter");
            final Integer loanProductCounter = JdbcSupport.getInteger(rs, "loanProductCounter");
            final BigDecimal fixedEmiAmount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "fixedEmiAmount");
            final Boolean isNPA = rs.getBoolean("isNPA");

            final int daysInMonth = JdbcSupport.getInteger(rs, "daysInMonth");
            final EnumOptionData daysInMonthType = CommonEnumerations.daysInMonthType(daysInMonth);
            final int daysInYear = JdbcSupport.getInteger(rs, "daysInYear");
            final EnumOptionData daysInYearType = CommonEnumerations.daysInYearType(daysInYear);
            final boolean isInterestRecalculationEnabled = rs.getBoolean("isInterestRecalculationEnabled");
            final Boolean createStandingInstructionAtDisbursement = rs.getBoolean("createStandingInstructionAtDisbursement");

            LoanInterestRecalculationData interestRecalculationData = null;
            if (isInterestRecalculationEnabled) {

                final Long lprId = JdbcSupport.getLong(rs, "lirId");
                final Long productId = JdbcSupport.getLong(rs, "loanId");
                final int compoundTypeEnumValue = JdbcSupport.getInteger(rs, "compoundType");
                final EnumOptionData interestRecalculationCompoundingType = LoanEnumerations
                        .interestRecalculationCompoundingType(compoundTypeEnumValue);
                final int rescheduleStrategyEnumValue = JdbcSupport.getInteger(rs, "rescheduleStrategy");
                final EnumOptionData rescheduleStrategyType = LoanEnumerations.rescheduleStrategyType(rescheduleStrategyEnumValue);
                final CalendarData calendarData = null;
                final int restFrequencyEnumValue = JdbcSupport.getInteger(rs, "restFrequencyEnum");
                final EnumOptionData restFrequencyType = LoanEnumerations.interestRecalculationFrequencyType(restFrequencyEnumValue);
                final int restFrequencyInterval = JdbcSupport.getInteger(rs, "restFrequencyInterval");
                final Integer restFrequencyNthDayEnumValue = JdbcSupport.getInteger(rs, "restFrequencyNthDayEnum");
                EnumOptionData restFrequencyNthDayEnum = null;
                if (restFrequencyNthDayEnumValue != null) {
                    restFrequencyNthDayEnum = LoanEnumerations.interestRecalculationCompoundingNthDayType(restFrequencyNthDayEnumValue);
                }
                final Integer restFrequencyWeekDayEnumValue = JdbcSupport.getInteger(rs, "restFrequencyWeekDayEnum");
                EnumOptionData restFrequencyWeekDayEnum = null;
                if (restFrequencyWeekDayEnumValue != null) {
                    restFrequencyWeekDayEnum = LoanEnumerations
                            .interestRecalculationCompoundingDayOfWeekType(restFrequencyWeekDayEnumValue);
                }
                final Integer restFrequencyOnDay = JdbcSupport.getInteger(rs, "restFrequencyOnDay");
                final CalendarData compoundingCalendarData = null;
                final Integer compoundingFrequencyEnumValue = JdbcSupport.getInteger(rs, "compoundingFrequencyEnum");
                EnumOptionData compoundingFrequencyType = null;
                if (compoundingFrequencyEnumValue != null) {
                    compoundingFrequencyType = LoanEnumerations.interestRecalculationFrequencyType(compoundingFrequencyEnumValue);
                }
                final Integer compoundingInterval = JdbcSupport.getInteger(rs, "compoundingInterval");
                final Integer compoundingFrequencyNthDayEnumValue = JdbcSupport.getInteger(rs, "compoundingFrequencyNthDayEnum");
                EnumOptionData compoundingFrequencyNthDayEnum = null;
                if (compoundingFrequencyNthDayEnumValue != null) {
                    compoundingFrequencyNthDayEnum = LoanEnumerations
                            .interestRecalculationCompoundingNthDayType(compoundingFrequencyNthDayEnumValue);
                }
                final Integer compoundingFrequencyWeekDayEnumValue = JdbcSupport.getInteger(rs, "compoundingFrequencyWeekDayEnum");
                EnumOptionData compoundingFrequencyWeekDayEnum = null;
                if (compoundingFrequencyWeekDayEnumValue != null) {
                    compoundingFrequencyWeekDayEnum = LoanEnumerations
                            .interestRecalculationCompoundingDayOfWeekType(compoundingFrequencyWeekDayEnumValue);
                }
                final Integer compoundingFrequencyOnDay = JdbcSupport.getInteger(rs, "compoundingFrequencyOnDay");

                final Boolean isCompoundingToBePostedAsTransaction = rs.getBoolean("isCompoundingToBePostedAsTransaction");
                final Boolean allowCompoundingOnEod = rs.getBoolean("allowCompoundingOnEod");
                interestRecalculationData = new LoanInterestRecalculationData(lprId, productId, interestRecalculationCompoundingType,
                        rescheduleStrategyType, calendarData, restFrequencyType, restFrequencyInterval, restFrequencyNthDayEnum,
                        restFrequencyWeekDayEnum, restFrequencyOnDay, compoundingCalendarData, compoundingFrequencyType,
                        compoundingInterval, compoundingFrequencyNthDayEnum, compoundingFrequencyWeekDayEnum, compoundingFrequencyOnDay,
                        isCompoundingToBePostedAsTransaction, allowCompoundingOnEod);
            }

            final boolean canUseForTopup = rs.getBoolean("canUseForTopup");
            final boolean isTopup = rs.getBoolean("isTopup");
            final Long closureLoanId = rs.getLong("closureLoanId");
            final String closureLoanAccountNo = rs.getString("closureLoanAccountNo");
            final BigDecimal topupAmount = rs.getBigDecimal("topupAmount");
            final String contractNo = rs.getString("contractNo");
            final BigDecimal requiredGuaranteeAmount = rs.getBigDecimal("requiredGuaranteeAmount");
            final BigDecimal requiredGuaranteeAmountPercent = requiredGuaranteeAmount.compareTo(BigDecimal.ZERO) > 0
                    ? requiredGuaranteeAmount.divide(new BigDecimal(100))
                    : BigDecimal.ZERO;
            final BigDecimal actualGuaranteeAmount = rs.getBigDecimal("actualGuaranteeAmount");
            final LoanAccountData loanAccountData = LoanAccountData.basicLoanDetails(id, accountNo, status, externalId, clientId,
                    clientAccountNo, clientName, clientOfficeId, groupData, loanType, loanProductId, loanProductName,
                    loanProductDescription, isLoanProductLinkedToFloatingRate, fundId, fundName, loanPurposeId, loanPurposeName,
                    loanOfficerId, loanOfficerName, currencyData, proposedPrincipal, principal, approvedPrincipal, netDisbursalAmount,
                    totalOverpaid, inArrearsTolerance, termFrequency, termPeriodFrequencyType, numberOfRepayments, repaymentEvery,
                    repaymentFrequencyType, null, null, transactionStrategyId, transactionStrategyName, amortizationType,
                    interestRatePerPeriod, interestRateFrequencyType, annualInterestRate, interestType, isFloatingInterestRate,
                    interestRateDifferential, interestCalculationPeriodType, allowPartialPeriodInterestCalcualtion,
                    expectedFirstRepaymentOnDate, graceOnPrincipalPayment, recurringMoratoriumOnPrincipalPeriods, graceOnInterestPayment,
                    graceOnInterestCharged, interestChargedFromDate, timeline, loanSummary, feeChargesDueAtDisbursementCharged,
                    syncDisbursementWithMeeting, loanCounter, loanProductCounter, multiDisburseLoan, canDefineInstallmentAmount,
                    fixedEmiAmount, outstandingLoanBalance, inArrears, graceOnArrearsAgeing, isNPA, daysInMonthType, daysInYearType,
                    isInterestRecalculationEnabled, interestRecalculationData, createStandingInstructionAtDisbursement,
                    isvariableInstallmentsAllowed, minimumGap, maximumGap, loanSubStatus, canUseForTopup, isTopup, closureLoanId,
                    closureLoanAccountNo, topupAmount, isEqualAmortization, fixedPrincipalPercentagePerInstallment, contractNo,
                    requiredGuaranteeAmountPercent, actualGuaranteeAmount);
            loanAccountData.setPrequalificationId(prequalificationId);
            return loanAccountData;

        }
    }

    private static final class MusoniOverdueLoanScheduleMapper implements RowMapper<OverdueLoanScheduleData> {

        public String schema() {
            return " ls.loan_id as loanId, ls.installment as period, ls.fromdate as fromDate, ls.duedate as dueDate, ls.obligations_met_on_date as obligationsMetOnDate, ls.completed_derived as complete,"
                    + " ls.principal_amount as principalDue, ls.principal_completed_derived as principalPaid, ls.principal_writtenoff_derived as principalWrittenOff, "
                    + " ls.interest_amount as interestDue, ls.interest_completed_derived as interestPaid, ls.interest_waived_derived as interestWaived, ls.interest_writtenoff_derived as interestWrittenOff, "
                    + " ls.fee_charges_amount as feeChargesDue, ls.fee_charges_completed_derived as feeChargesPaid, ls.fee_charges_waived_derived as feeChargesWaived, ls.fee_charges_writtenoff_derived as feeChargesWrittenOff, "
                    + " ls.penalty_charges_amount as penaltyChargesDue, ls.penalty_charges_completed_derived as penaltyChargesPaid, ls.penalty_charges_waived_derived as penaltyChargesWaived, ls.penalty_charges_writtenoff_derived as penaltyChargesWrittenOff, "
                    + " ls.total_paid_in_advance_derived as totalPaidInAdvanceForPeriod, ls.total_paid_late_derived as totalPaidLateForPeriod, "
                    + " mc.amount,mc.id as chargeId " + " from m_loan_repayment_schedule ls "
                    + " inner join m_loan ml on ml.id = ls.loan_id "
                    + " join m_product_loan_charge plc on plc.product_loan_id = ml.product_id "
                    + " join m_charge mc on mc.id = plc.charge_id ";

        }

        @Override
        public OverdueLoanScheduleData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long chargeId = rs.getLong("chargeId");
            final Long loanId = rs.getLong("loanId");
            final BigDecimal amount = rs.getBigDecimal("amount");
            final String dateFormat = "yyyy-MM-dd";
            final String dueDate = rs.getString("dueDate");
            final String locale = "en_GB";

            final BigDecimal principalDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalDue");
            final BigDecimal principalPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalPaid");
            final BigDecimal principalWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalWrittenOff");

            final BigDecimal principalOutstanding = principalDue.subtract(principalPaid).subtract(principalWrittenOff);

            final BigDecimal interestExpectedDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestDue");
            final BigDecimal interestPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestPaid");
            final BigDecimal interestWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWaived");
            final BigDecimal interestWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWrittenOff");

            final BigDecimal interestActualDue = interestExpectedDue.subtract(interestWaived).subtract(interestWrittenOff);
            final BigDecimal interestOutstanding = interestActualDue.subtract(interestPaid);

            final Integer installmentNumber = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "period");

            return new OverdueLoanScheduleData(loanId, chargeId, dueDate, amount, dateFormat, locale, principalOutstanding,
                    interestOutstanding, installmentNumber);
        }
    }

    private static final class LoanScheduleResultSetExtractor implements ResultSetExtractor<LoanScheduleData> {

        private final CurrencyData currency;
        private final DisbursementData disbursement;
        private final BigDecimal totalFeeChargesDueAtDisbursement;
        private final Collection<DisbursementData> disbursementData;
        private LocalDate lastDueDate;
        private BigDecimal outstandingLoanPrincipalBalance;
        private boolean excludePastUndisbursed;
        private final BigDecimal totalPaidFeeCharges;

        LoanScheduleResultSetExtractor(final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedLoanData,
                Collection<DisbursementData> disbursementData, boolean isInterestRecalculationEnabled, BigDecimal totalPaidFeeCharges) {
            this.currency = repaymentScheduleRelatedLoanData.getCurrency();
            this.disbursement = repaymentScheduleRelatedLoanData.disbursementData();
            this.totalFeeChargesDueAtDisbursement = repaymentScheduleRelatedLoanData.getTotalFeeChargesAtDisbursement();
            this.lastDueDate = this.disbursement.disbursementDate();
            this.outstandingLoanPrincipalBalance = this.disbursement.amount();
            this.disbursementData = disbursementData;
            this.excludePastUndisbursed = isInterestRecalculationEnabled;
            this.totalPaidFeeCharges = totalPaidFeeCharges;
        }

        public String schema() {

            return " ls.loan_id as loanId, ls.installment as period, ls.fromdate as fromDate, ls.duedate as dueDate, ls.obligations_met_on_date as obligationsMetOnDate, ls.completed_derived as complete,"
                    + " ls.principal_amount as principalDue, ls.principal_completed_derived as principalPaid, ls.principal_writtenoff_derived as principalWrittenOff, "
                    + " ls.interest_amount as interestDue, ls.interest_completed_derived as interestPaid, ls.interest_waived_derived as interestWaived, ls.interest_writtenoff_derived as interestWrittenOff, "
                    + " ls.fee_charges_amount as feeChargesDue, ls.fee_charges_completed_derived as feeChargesPaid, ls.fee_charges_waived_derived as feeChargesWaived, ls.fee_charges_writtenoff_derived as feeChargesWrittenOff, "
                    + " ls.penalty_charges_amount as penaltyChargesDue, ls.penalty_charges_completed_derived as penaltyChargesPaid, ls.penalty_charges_waived_derived as penaltyChargesWaived, ls.penalty_charges_writtenoff_derived as penaltyChargesWrittenOff, "
                    + " ls.total_paid_in_advance_derived as totalPaidInAdvanceForPeriod, ls.total_paid_late_derived as totalPaidLateForPeriod "
                    + " from m_loan_repayment_schedule ls ";
        }

        @Override
        public LoanScheduleData extractData(final ResultSet rs) throws SQLException, DataAccessException {
            BigDecimal waivedChargeAmount = BigDecimal.ZERO;
            for (DisbursementData disbursementDetail : disbursementData) {
                waivedChargeAmount = waivedChargeAmount.add(disbursementDetail.getWaivedChargeAmount());
            }
            final LoanSchedulePeriodData disbursementPeriod = LoanSchedulePeriodData.disbursementOnlyPeriod(
                    this.disbursement.disbursementDate(), this.disbursement.amount(), this.totalFeeChargesDueAtDisbursement,
                    this.disbursement.isDisbursed());

            final Collection<LoanSchedulePeriodData> periods = new ArrayList<>();
            final MonetaryCurrency monCurrency = new MonetaryCurrency(this.currency.code(), this.currency.decimalPlaces(),
                    this.currency.currencyInMultiplesOf());
            BigDecimal totalPrincipalDisbursed = BigDecimal.ZERO;
            BigDecimal disbursementChargeAmount = this.totalFeeChargesDueAtDisbursement;
            if (disbursementData.isEmpty()) {
                periods.add(disbursementPeriod);
                totalPrincipalDisbursed = Money.of(monCurrency, this.disbursement.amount()).getAmount();
            } else {
                if (!this.disbursement.isDisbursed()) {
                    excludePastUndisbursed = false;
                }
                for (DisbursementData data : disbursementData) {
                    if (data.getChargeAmount() != null) {
                        disbursementChargeAmount = disbursementChargeAmount.subtract(data.getChargeAmount());
                    }
                }
                this.outstandingLoanPrincipalBalance = BigDecimal.ZERO;
            }

            Money totalPrincipalExpected = Money.zero(monCurrency);
            Money totalPrincipalPaid = Money.zero(monCurrency);
            Money totalInterestCharged = Money.zero(monCurrency);
            Money totalFeeChargesCharged = Money.zero(monCurrency);
            Money totalPenaltyChargesCharged = Money.zero(monCurrency);
            Money totalWaived = Money.zero(monCurrency);
            Money totalWrittenOff = Money.zero(monCurrency);
            Money totalRepaymentExpected = Money.zero(monCurrency);
            Money totalRepayment = Money.zero(monCurrency);
            Money totalPaidInAdvance = Money.zero(monCurrency);
            Money totalPaidLate = Money.zero(monCurrency);
            Money totalOutstanding = Money.zero(monCurrency);

            // update totals with details of fees charged during disbursement
            totalFeeChargesCharged = totalFeeChargesCharged.plus(disbursementPeriod.feeChargesDue().subtract(waivedChargeAmount));
            totalRepaymentExpected = totalRepaymentExpected.plus(disbursementPeriod.feeChargesDue()).minus(waivedChargeAmount);
            totalRepayment = totalRepayment.plus(disbursementPeriod.feeChargesPaid()).minus(waivedChargeAmount);
            totalOutstanding = totalOutstanding.plus(disbursementPeriod.feeChargesDue()).minus(disbursementPeriod.feeChargesPaid());

            Integer loanTermInDays = 0;
            while (rs.next()) {

                final Long loanId = rs.getLong("loanId");
                final Integer period = JdbcSupport.getInteger(rs, "period");
                LocalDate fromDate = JdbcSupport.getLocalDate(rs, "fromDate");
                final LocalDate dueDate = JdbcSupport.getLocalDate(rs, "dueDate");
                final LocalDate obligationsMetOnDate = JdbcSupport.getLocalDate(rs, "obligationsMetOnDate");
                final boolean complete = rs.getBoolean("complete");
                BigDecimal principal = BigDecimal.ZERO;
                for (final DisbursementData data : disbursementData) {
                    if (fromDate.equals(this.disbursement.disbursementDate()) && data.disbursementDate().equals(fromDate)) {
                        principal = principal.add(data.amount());
                        LoanSchedulePeriodData periodData = null;
                        if (data.getChargeAmount() == null) {
                            periodData = LoanSchedulePeriodData.disbursementOnlyPeriod(data.disbursementDate(), data.amount(),
                                    disbursementChargeAmount, data.isDisbursed());
                        } else {
                            periodData = LoanSchedulePeriodData.disbursementOnlyPeriod(data.disbursementDate(), data.amount(),
                                    disbursementChargeAmount.add(data.getChargeAmount()).subtract(waivedChargeAmount), data.isDisbursed());
                        }
                        periods.add(periodData);
                        this.outstandingLoanPrincipalBalance = this.outstandingLoanPrincipalBalance.add(data.amount());
                    } else if (data.isDueForDisbursement(fromDate, dueDate)) {
                        if (!excludePastUndisbursed || data.isDisbursed()
                                || !data.disbursementDate().isBefore(DateUtils.getBusinessLocalDate())) {
                            principal = principal.add(data.amount());
                            LoanSchedulePeriodData periodData;
                            if (data.getChargeAmount() == null) {
                                periodData = LoanSchedulePeriodData.disbursementOnlyPeriod(data.disbursementDate(), data.amount(),
                                        BigDecimal.ZERO, data.isDisbursed());
                            } else {
                                periodData = LoanSchedulePeriodData.disbursementOnlyPeriod(data.disbursementDate(), data.amount(),
                                        data.getChargeAmount(), data.isDisbursed());
                            }
                            periods.add(periodData);
                            this.outstandingLoanPrincipalBalance = this.outstandingLoanPrincipalBalance.add(data.amount());
                        }
                    }
                }
                totalPrincipalDisbursed = totalPrincipalDisbursed.add(principal);

                Integer daysInPeriod = 0;
                if (fromDate != null) {
                    daysInPeriod = Math.toIntExact(ChronoUnit.DAYS.between(fromDate, dueDate));
                    loanTermInDays = loanTermInDays + daysInPeriod;
                }

                final BigDecimal principalDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalDue");
                totalPrincipalExpected = totalPrincipalExpected.plus(principalDue);
                final BigDecimal principalPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalPaid");
                totalPrincipalPaid = totalPrincipalPaid.plus(principalPaid);
                final BigDecimal principalWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalWrittenOff");

                final BigDecimal principalOutstanding = principalDue.subtract(principalPaid).subtract(principalWrittenOff);

                final BigDecimal interestExpectedDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestDue");
                totalInterestCharged = totalInterestCharged.plus(interestExpectedDue);
                final BigDecimal interestPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestPaid");
                final BigDecimal interestWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWaived");
                final BigDecimal interestWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWrittenOff");
                final BigDecimal totalInstallmentAmount = totalPrincipalPaid.zero().plus(principalDue).plus(interestExpectedDue)
                        .getAmount();

                final BigDecimal interestActualDue = interestExpectedDue.subtract(interestWaived).subtract(interestWrittenOff);
                final BigDecimal interestOutstanding = interestActualDue.subtract(interestPaid);

                final BigDecimal feeChargesExpectedDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesDue");
                totalFeeChargesCharged = totalFeeChargesCharged.plus(feeChargesExpectedDue);
                final BigDecimal feeChargesPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesPaid");
                final BigDecimal feeChargesWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesWaived");
                final BigDecimal feeChargesWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesWrittenOff");

                final BigDecimal feeChargesActualDue = feeChargesExpectedDue.subtract(feeChargesWaived).subtract(feeChargesWrittenOff);
                final BigDecimal feeChargesOutstanding = feeChargesActualDue.subtract(feeChargesPaid);

                final BigDecimal penaltyChargesExpectedDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesDue");
                totalPenaltyChargesCharged = totalPenaltyChargesCharged.plus(penaltyChargesExpectedDue);
                final BigDecimal penaltyChargesPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesPaid");
                final BigDecimal penaltyChargesWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesWaived");
                final BigDecimal penaltyChargesWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesWrittenOff");

                final BigDecimal totalPaidInAdvanceForPeriod = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs,
                        "totalPaidInAdvanceForPeriod");
                final BigDecimal totalPaidLateForPeriod = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalPaidLateForPeriod");

                final BigDecimal penaltyChargesActualDue = penaltyChargesExpectedDue.subtract(penaltyChargesWaived)
                        .subtract(penaltyChargesWrittenOff);
                final BigDecimal penaltyChargesOutstanding = penaltyChargesActualDue.subtract(penaltyChargesPaid);

                final BigDecimal totalExpectedCostOfLoanForPeriod = interestExpectedDue.add(feeChargesExpectedDue)
                        .add(penaltyChargesExpectedDue);

                final BigDecimal totalDueForPeriod = principalDue.add(totalExpectedCostOfLoanForPeriod);
                final BigDecimal totalPaidForPeriod = principalPaid.add(interestPaid).add(feeChargesPaid).add(penaltyChargesPaid);
                final BigDecimal totalWaivedForPeriod = interestWaived.add(feeChargesWaived).add(penaltyChargesWaived);
                totalWaived = totalWaived.plus(totalWaivedForPeriod);
                final BigDecimal totalWrittenOffForPeriod = principalWrittenOff.add(interestWrittenOff).add(feeChargesWrittenOff)
                        .add(penaltyChargesWrittenOff);
                totalWrittenOff = totalWrittenOff.plus(totalWrittenOffForPeriod);
                final BigDecimal totalOutstandingForPeriod = principalOutstanding.add(interestOutstanding).add(feeChargesOutstanding)
                        .add(penaltyChargesOutstanding);

                final BigDecimal totalActualCostOfLoanForPeriod = interestActualDue.add(feeChargesActualDue).add(penaltyChargesActualDue);

                totalRepaymentExpected = totalRepaymentExpected.plus(totalDueForPeriod);
                totalRepayment = totalRepayment.plus(totalPaidForPeriod);
                totalPaidInAdvance = totalPaidInAdvance.plus(totalPaidInAdvanceForPeriod);
                totalPaidLate = totalPaidLate.plus(totalPaidLateForPeriod);
                totalOutstanding = totalOutstanding.plus(totalOutstandingForPeriod);

                if (fromDate == null) {
                    fromDate = this.lastDueDate;
                }
                final BigDecimal outstandingPrincipalBalanceOfLoan = this.outstandingLoanPrincipalBalance.subtract(principalDue);

                // update based on current period values
                this.lastDueDate = dueDate;
                this.outstandingLoanPrincipalBalance = this.outstandingLoanPrincipalBalance.subtract(principalDue);

                final LoanSchedulePeriodData periodData = LoanSchedulePeriodData.repaymentPeriodWithPayments(loanId, period, fromDate,
                        dueDate, obligationsMetOnDate, complete, principalDue, principalPaid, principalWrittenOff, principalOutstanding,
                        outstandingPrincipalBalanceOfLoan, interestExpectedDue, interestPaid, interestWaived, interestWrittenOff,
                        interestOutstanding, feeChargesExpectedDue, feeChargesPaid, feeChargesWaived, feeChargesWrittenOff,
                        feeChargesOutstanding, penaltyChargesExpectedDue, penaltyChargesPaid, penaltyChargesWaived,
                        penaltyChargesWrittenOff, penaltyChargesOutstanding, totalDueForPeriod, totalPaidForPeriod,
                        totalPaidInAdvanceForPeriod, totalPaidLateForPeriod, totalWaivedForPeriod, totalWrittenOffForPeriod,
                        totalOutstandingForPeriod, totalActualCostOfLoanForPeriod, totalInstallmentAmount);

                periods.add(periodData);
            }

            return new LoanScheduleData(this.currency, periods, loanTermInDays, totalPrincipalDisbursed, totalPrincipalExpected.getAmount(),
                    totalPrincipalPaid.getAmount(), totalInterestCharged.getAmount(), totalFeeChargesCharged.getAmount(),
                    totalPenaltyChargesCharged.getAmount(), totalWaived.getAmount(), totalWrittenOff.getAmount(),
                    totalRepaymentExpected.getAmount(), totalRepayment.getAmount(), totalPaidInAdvance.getAmount(),
                    totalPaidLate.getAmount(), totalOutstanding.getAmount());
        }

    }

    private static final class LoanTransactionsMapper implements RowMapper<LoanTransactionData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;

        LoanTransactionsMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String loanPaymentsSchema() {

            return " tr.id as id, tr.transaction_type_enum as transactionType, tr.transaction_date as " + sqlGenerator.escape("date")
                    + ", tr.amount as total, " + " tr.principal_portion_derived as principal, tr.interest_portion_derived as interest, "
                    + " tr.fee_charges_portion_derived as fees, tr.penalty_charges_portion_derived as penalties, "
                    + " tr.overpayment_portion_derived as overpayment, tr.outstanding_loan_balance_derived as outstandingLoanBalance, "
                    + " tr.unrecognized_income_portion as unrecognizedIncome," + " tr.submitted_on_date as submittedOnDate, "
                    + " tr.manually_adjusted_or_reversed as manuallyReversed, "
                    + " pd.payment_type_id as paymentType, COALESCE(gla.name, pd.account_number) as accountNumber,pd.check_number as checkNumber, pd.bill_number AS billNumber, "
                    + " pd.receipt_number as receiptNumber, pd.bank_number as bankNumber,pd.routing_code as routingCode, l.net_disbursal_amount as netDisbursalAmount,"
                    + " l.currency_code as currencyCode, l.currency_digits as currencyDigits, l.currency_multiplesof as inMultiplesOf, rc."
                    + sqlGenerator.escape("name") + " as currencyName, "
                    + " rc.display_symbol as currencyDisplaySymbol, rc.internationalized_name_code as currencyNameCode, rc.int_code as intCode, "
                    + " pt.value as paymentTypeName, tr.external_id as externalId, tr.office_id as officeId, office.name as officeName, "
                    + " fromtran.id as fromTransferId, fromtran.is_reversed as fromTransferReversed,"
                    + " fromtran.transaction_date as fromTransferDate, fromtran.amount as fromTransferAmount,"
                    + " fromtran.description as fromTransferDescription,"
                    + " totran.id as toTransferId, totran.is_reversed as toTransferReversed,"
                    + " totran.transaction_date as toTransferDate, totran.amount as toTransferAmount,"
                    + " totran.description as toTransferDescription " + " from m_loan l join m_loan_transaction tr on tr.loan_id = l.id"
                    + " join m_currency rc on rc." + sqlGenerator.escape("code") + " = l.currency_code "
                    + " left JOIN m_payment_detail pd ON tr.payment_detail_id = pd.id"
                    + " left join m_payment_type pt on pd.payment_type_id = pt.id" + " left join m_office office on office.id=tr.office_id"
                    + " left join m_account_transfer_transaction fromtran on fromtran.from_loan_transaction_id = tr.id "
                    + " left join m_account_transfer_transaction totran on totran.to_loan_transaction_id = tr.id "
                    + " left join acc_gl_account gla ON gla.id = pd.gl_account_id";
        }

        @Override
        public LoanTransactionData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final Integer intCode = JdbcSupport.getInteger(rs, "intCode");
            final CurrencyData currencyData = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf,
                    currencyDisplaySymbol, currencyNameCode, intCode);

            final Long id = rs.getLong("id");
            final Long officeId = rs.getLong("officeId");
            final String officeName = rs.getString("officeName");
            final int transactionTypeInt = JdbcSupport.getInteger(rs, "transactionType");
            final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(transactionTypeInt);
            final boolean manuallyReversed = rs.getBoolean("manuallyReversed");

            PaymentDetailData paymentDetailData = null;

            if (transactionType.isPaymentOrReceipt()) {
                final Long paymentTypeId = JdbcSupport.getLong(rs, "paymentType");
                if (paymentTypeId != null) {
                    final String typeName = rs.getString("paymentTypeName");
                    final PaymentTypeData paymentType = PaymentTypeData.instance(paymentTypeId, typeName);
                    final String accountNumber = rs.getString("accountNumber");
                    final String checkNumber = rs.getString("checkNumber");
                    final String routingCode = rs.getString("routingCode");
                    final String receiptNumber = rs.getString("receiptNumber");
                    final String bankNumber = rs.getString("bankNumber");
                    final String billNumber = rs.getString("billNumber");
                    paymentDetailData = new PaymentDetailData(id, paymentType, accountNumber, checkNumber, routingCode, receiptNumber,
                            bankNumber);
                    paymentDetailData.setBillNumber(billNumber);
                }
            }
            final LocalDate date = JdbcSupport.getLocalDate(rs, "date");
            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
            final BigDecimal totalAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "total");
            final BigDecimal principalPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principal");
            final BigDecimal interestPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interest");
            final BigDecimal feeChargesPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "fees");
            final BigDecimal penaltyChargesPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penalties");
            final BigDecimal overPaymentPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "overpayment");
            final BigDecimal unrecognizedIncomePortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "unrecognizedIncome");
            final BigDecimal outstandingLoanBalance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "outstandingLoanBalance");
            final String externalId = rs.getString("externalId");

            final BigDecimal netDisbursalAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "netDisbursalAmount");

            AccountTransferData transfer = null;
            final Long fromTransferId = JdbcSupport.getLong(rs, "fromTransferId");
            final Long toTransferId = JdbcSupport.getLong(rs, "toTransferId");
            if (fromTransferId != null) {
                final LocalDate fromTransferDate = JdbcSupport.getLocalDate(rs, "fromTransferDate");
                final BigDecimal fromTransferAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "fromTransferAmount");
                final boolean fromTransferReversed = rs.getBoolean("fromTransferReversed");
                final String fromTransferDescription = rs.getString("fromTransferDescription");

                transfer = AccountTransferData.transferBasicDetails(fromTransferId, currencyData, fromTransferAmount, fromTransferDate,
                        fromTransferDescription, fromTransferReversed);
            } else if (toTransferId != null) {
                final LocalDate toTransferDate = JdbcSupport.getLocalDate(rs, "toTransferDate");
                final BigDecimal toTransferAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "toTransferAmount");
                final boolean toTransferReversed = rs.getBoolean("toTransferReversed");
                final String toTransferDescription = rs.getString("toTransferDescription");

                transfer = AccountTransferData.transferBasicDetails(toTransferId, currencyData, toTransferAmount, toTransferDate,
                        toTransferDescription, toTransferReversed);
            }
            return new LoanTransactionData(id, officeId, officeName, transactionType, paymentDetailData, currencyData, date, totalAmount,
                    netDisbursalAmount, principalPortion, interestPortion, feeChargesPortion, penaltyChargesPortion, overPaymentPortion,
                    unrecognizedIncomePortion, externalId, transfer, null, outstandingLoanBalance, submittedOnDate, manuallyReversed);
        }
    }

    @Override
    public LoanAccountData retrieveLoanProductDetailsTemplate(final Long productId, final Long clientId, final Long groupId,
            String templateType) {

        this.context.authenticatedUser();

        if (!"individual".equals(templateType) && clientId != null) {
            ClientData clientData = this.clientReadPlatformService.retrieveOne(clientId);
            String blacklistString = "select count(*) from m_client_blacklist where dpi=? and status=?";
            String dpiNumber = clientData.getDpiNumber();
            Long blacklisted = jdbcTemplate.queryForObject(blacklistString, Long.class, dpiNumber, BlacklistStatus.ACTIVE.getValue());
            if (blacklisted > 0) {
                String blacklistReason = "select type_enum from m_client_blacklist where dpi=? and status=?";
                Integer typification = jdbcTemplate.queryForObject(blacklistReason, Integer.class, dpiNumber,
                        BlacklistStatus.ACTIVE.getValue());
                CodeValue typificationCodeValue = this.codeValueRepositoryWrapper.findOneWithNotFoundDetection(typification.longValue());
                throw new ClientBlacklistedException(typificationCodeValue.getDescription());
            }
        }

        final LoanProductData loanProduct = this.loanProductReadPlatformService.retrieveLoanProduct(productId);
        final Collection<EnumOptionData> loanTermFrequencyTypeOptions = this.loanDropdownReadPlatformService
                .retrieveLoanTermFrequencyTypeOptions();
        final Collection<EnumOptionData> repaymentFrequencyTypeOptions = this.loanDropdownReadPlatformService
                .retrieveRepaymentFrequencyTypeOptions();
        final Collection<EnumOptionData> repaymentFrequencyNthDayTypeOptions = this.loanDropdownReadPlatformService
                .retrieveRepaymentFrequencyOptionsForNthDayOfMonth();
        final Collection<EnumOptionData> repaymentFrequencyDaysOfWeekTypeOptions = this.loanDropdownReadPlatformService
                .retrieveRepaymentFrequencyOptionsForDaysOfWeek();
        final Collection<EnumOptionData> interestRateFrequencyTypeOptions = this.loanDropdownReadPlatformService
                .retrieveInterestRateFrequencyTypeOptions();
        final Collection<EnumOptionData> amortizationTypeOptions = this.loanDropdownReadPlatformService
                .retrieveLoanAmortizationTypeOptions();
        Collection<EnumOptionData> interestTypeOptions = null;
        if (loanProduct.isLinkedToFloatingInterestRates()) {
            interestTypeOptions = Arrays.asList(interestType(InterestMethod.DECLINING_BALANCE));
        } else {
            interestTypeOptions = this.loanDropdownReadPlatformService.retrieveLoanInterestTypeOptions();
        }
        final Collection<EnumOptionData> interestCalculationPeriodTypeOptions = this.loanDropdownReadPlatformService
                .retrieveLoanInterestRateCalculatedInPeriodOptions();
        final Collection<FundData> fundOptions = this.fundReadPlatformService.retrieveAllFunds();
        final Collection<TransactionProcessingStrategyData> repaymentStrategyOptions = this.loanDropdownReadPlatformService
                .retreiveTransactionProcessingStrategies();
        final Collection<CodeValueData> loanPurposeOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("LoanPurpose");
        final Collection<CodeValueData> loanCollateralOptions = this.codeValueReadPlatformService
                .retrieveCodeValuesByCode("LoanCollateral");
        Collection<ChargeData> chargeOptions = null;
        if (loanProduct.getMultiDisburseLoan()) {
            chargeOptions = this.chargeReadPlatformService.retrieveLoanProductApplicableCharges(productId,
                    new ChargeTimeType[] { ChargeTimeType.OVERDUE_INSTALLMENT });
        } else {
            chargeOptions = this.chargeReadPlatformService.retrieveLoanProductApplicableCharges(productId,
                    new ChargeTimeType[] { ChargeTimeType.OVERDUE_INSTALLMENT, ChargeTimeType.TRANCHE_DISBURSEMENT });
        }

        Integer loanCycleCounter = null;
        if (loanProduct.useBorrowerCycle()) {
            if (clientId == null) {
                loanCycleCounter = retriveLoanCounter(groupId, AccountType.GROUP.getValue(), loanProduct.getId());
            } else {
                loanCycleCounter = retriveLoanCounter(clientId, loanProduct.getId());
            }
        }

        Collection<LoanAccountSummaryData> activeLoanOptions = null;
        if (loanProduct.canUseForTopup() && clientId != null) {
            activeLoanOptions = this.accountDetailsReadPlatformService.retrieveClientActiveLoanAccountSummary(clientId);
        } else if (loanProduct.canUseForTopup() && groupId != null) {
            activeLoanOptions = this.accountDetailsReadPlatformService.retrieveGroupActiveLoanAccountSummary(groupId);
        }

        return LoanAccountData.loanProductWithTemplateDefaults(loanProduct, loanTermFrequencyTypeOptions, repaymentFrequencyTypeOptions,
                repaymentFrequencyNthDayTypeOptions, repaymentFrequencyDaysOfWeekTypeOptions, repaymentStrategyOptions,
                interestRateFrequencyTypeOptions, amortizationTypeOptions, interestTypeOptions, interestCalculationPeriodTypeOptions,
                fundOptions, chargeOptions, loanPurposeOptions, loanCollateralOptions, loanCycleCounter, activeLoanOptions);
    }

    @Override
    public LoanAccountData retrieveClientDetailsTemplate(final Long clientId) {

        this.context.authenticatedUser();

        final ClientData clientAccount = this.clientReadPlatformService.retrieveOne(clientId);
        final LocalDate expectedDisbursementDate = DateUtils.getBusinessLocalDate();

        return LoanAccountData.clientDefaults(clientAccount.id(), clientAccount.accountNo(), clientAccount.displayName(),
                clientAccount.officeId(), expectedDisbursementDate);
    }

    @Override
    public LoanAccountData retrieveGroupDetailsTemplate(final Long groupId) {
        this.context.authenticatedUser();
        final GroupGeneralData groupAccount = this.groupReadPlatformService.retrieveOne(groupId);
        final LocalDate expectedDisbursementDate = DateUtils.getBusinessLocalDate();
        return LoanAccountData.groupDefaults(groupAccount, expectedDisbursementDate);
    }

    @Override
    public LoanAccountData retrieveGroupAndMembersDetailsTemplate(final Long groupId) {
        GroupGeneralData groupAccount = this.groupReadPlatformService.retrieveOne(groupId);
        final LocalDate expectedDisbursementDate = DateUtils.getBusinessLocalDate();

        // get group associations
        final Collection<ClientData> membersOfGroup = this.clientReadPlatformService.retrieveActiveClientMembersOfGroup(groupId);
        if (!CollectionUtils.isEmpty(membersOfGroup)) {
            final Collection<ClientData> activeClientMembers = null;
            final Collection<CalendarData> calendarsData = null;
            final CalendarData collectionMeetingCalendar = null;
            final Collection<GroupRoleData> groupRoles = null;
            groupAccount = GroupGeneralData.withAssocations(groupAccount, membersOfGroup, activeClientMembers, groupRoles, calendarsData,
                    collectionMeetingCalendar);
        }

        return LoanAccountData.groupDefaults(groupAccount, expectedDisbursementDate);
    }

    @Override
    public Collection<CalendarData> retrieveCalendars(final Long groupId) {
        Collection<CalendarData> calendarsData = new ArrayList<>();
        calendarsData.addAll(
                this.calendarReadPlatformService.retrieveParentCalendarsByEntity(groupId, CalendarEntityType.GROUPS.getValue(), null));
        calendarsData
                .addAll(this.calendarReadPlatformService.retrieveCalendarsByEntity(groupId, CalendarEntityType.GROUPS.getValue(), null));
        calendarsData = this.calendarReadPlatformService.updateWithRecurringDates(calendarsData);
        return calendarsData;
    }

    @Override
    public Collection<StaffData> retrieveAllowedLoanOfficers(final Long selectedOfficeId, final boolean staffInSelectedOfficeOnly) {
        if (selectedOfficeId == null) {
            return null;
        }

        Collection<StaffData> allowedLoanOfficers = null;

        if (staffInSelectedOfficeOnly) {
            // only bring back loan officers in selected branch/office
            allowedLoanOfficers = this.staffReadPlatformService.retrieveAllLoanOfficersInOfficeById(selectedOfficeId);
        } else {
            // by default bring back all loan officers in selected
            // branch/office as well as loan officers in officer above
            // this office
            final boolean restrictToLoanOfficersOnly = true;
            allowedLoanOfficers = this.staffReadPlatformService.retrieveAllStaffInOfficeAndItsParentOfficeHierarchy(selectedOfficeId,
                    restrictToLoanOfficersOnly);
        }

        return allowedLoanOfficers;
    }

    @Override
    public Collection<OverdueLoanScheduleData> retrieveAllLoansWithOverdueInstallments(final Long penaltyWaitPeriod,
            final Boolean backdatePenalties) {
        final MusoniOverdueLoanScheduleMapper rm = new MusoniOverdueLoanScheduleMapper();

        final StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append("select ").append(rm.schema())
                .append(" where " + sqlGenerator.subDate(sqlGenerator.currentBusinessDate(), "?", "day") + " > ls.duedate ")
                .append(" and ls.completed_derived <> true and mc.charge_applies_to_enum =1 ")
                .append(" and ls.recalculated_interest_component <> true ")
                .append(" and mc.charge_time_enum = 9 and ml.loan_status_id = 300 ");

        if (backdatePenalties) {
            return this.jdbcTemplate.query(sqlBuilder.toString(), rm, penaltyWaitPeriod);
        }
        // Only apply for duedate = yesterday (so that we don't apply
        // penalties on the duedate itself)
        sqlBuilder.append(" and ls.duedate >= " + sqlGenerator.subDate(sqlGenerator.currentBusinessDate(), "(? + 1)", "day"));

        return this.jdbcTemplate.query(sqlBuilder.toString(), rm, penaltyWaitPeriod, penaltyWaitPeriod);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Integer retriveLoanCounter(final Long groupId, final Integer loanType, Long productId) {
        final String sql = "Select MAX(l.loan_product_counter) from m_loan l where l.group_id = ?  and l.loan_type_enum = ? and l.product_id=?";
        Integer loanCounter = this.jdbcTemplate.queryForObject(sql, new Object[] { groupId, loanType, productId }, Integer.class);
        if (loanCounter != null) {
            return loanCounter;
        } else {
            return 0;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Integer retriveLoanCounter(final Long clientId, Long productId) {
        Integer loanCounter = 0;
        final String sql = "Select coalesce(MAX(l.loan_product_counter),0) from m_loan l where l.client_id = ? and l.product_id=?";
        loanCounter = this.jdbcTemplate.queryForObject(sql, new Object[] { clientId, productId }, Integer.class);

        final String clientDefinedLoanCycle = "Select c.loan_cycle as loanCycle from m_client c where c.id = ?";
        Integer clientLoanCycle = this.jdbcTemplate.queryForObject(clientDefinedLoanCycle, new Object[] { clientId }, Integer.class);
        if (clientLoanCycle != null) {
            return clientLoanCycle;
        } else {
            return loanCounter;
        }
    }

    @Override
    public Integer retriveLoanCounterByClient(final Long clientId) {
        final String sql = "Select COUNT(ml.id) from m_loan ml where ml.client_id = ? and ml.loan_status_id >=300 "
                + "and ml.loan_status_id not in (400, 500, 601, 602)";
        Integer loanCounter = this.jdbcTemplate.queryForObject(sql, new Object[] { clientId }, Integer.class);
        if (loanCounter != null) {
            return loanCounter;
        } else {
            return 0;
        }
    }

    @Override
    public Collection<DisbursementData> retrieveLoanDisbursementDetails(final Long loanId) {
        final LoanDisbursementDetailMapper rm = new LoanDisbursementDetailMapper(sqlGenerator);
        final String sql = "select " + rm.schema()
                + " where dd.loan_id=? group by dd.id, lc.amount_waived_derived order by dd.expected_disburse_date";
        return this.jdbcTemplate.query(sql, rm, loanId); // NOSONAR
    }

    private static final class LoanDisbursementDetailMapper implements RowMapper<DisbursementData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;

        LoanDisbursementDetailMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String schema() {
            return "dd.id as id,dd.expected_disburse_date as expectedDisbursementdate, dd.disbursedon_date as actualDisbursementdate,dd.principal as principal,dd.net_disbursal_amount as netDisbursalAmount,sum(lc.amount) chargeAmount, lc.amount_waived_derived waivedAmount, "
                    + sqlGenerator.groupConcat("lc.id") + " loanChargeId "
                    + "from m_loan l inner join m_loan_disbursement_detail dd on dd.loan_id = l.id left join m_loan_tranche_disbursement_charge tdc on tdc.disbursement_detail_id=dd.id "
                    + "left join m_loan_charge lc on  lc.id=tdc.loan_charge_id and lc.is_active=true";
        }

        @Override
        public DisbursementData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final LocalDate expectedDisbursementdate = JdbcSupport.getLocalDate(rs, "expectedDisbursementdate");
            final LocalDate actualDisbursementdate = JdbcSupport.getLocalDate(rs, "actualDisbursementdate");
            final BigDecimal principal = rs.getBigDecimal("principal");
            final String loanChargeId = rs.getString("loanChargeId");
            final BigDecimal netDisbursalAmount = rs.getBigDecimal("netDisbursalAmount");
            BigDecimal chargeAmount = rs.getBigDecimal("chargeAmount");
            final BigDecimal waivedAmount = rs.getBigDecimal("waivedAmount");
            if (chargeAmount != null && waivedAmount != null) {
                chargeAmount = chargeAmount.subtract(waivedAmount);
            }
            return new DisbursementData(id, expectedDisbursementdate, actualDisbursementdate, principal, netDisbursalAmount, loanChargeId,
                    chargeAmount, waivedAmount);
        }

    }

    @Override
    public DisbursementData retrieveLoanDisbursementDetail(Long loanId, Long disbursementId) {
        final LoanDisbursementDetailMapper rm = new LoanDisbursementDetailMapper(sqlGenerator);
        final String sql = "select " + rm.schema() + " where dd.loan_id=? and dd.id=? group by dd.id, lc.amount_waived_derived";
        return this.jdbcTemplate.queryForObject(sql, rm, loanId, disbursementId); // NOSONAR
    }

    @Override
    public Collection<LoanTermVariationsData> retrieveLoanTermVariations(Long loanId, Integer termType) {
        final LoanTermVariationsMapper rm = new LoanTermVariationsMapper();
        final String sql = "select " + rm.schema() + " where tv.loan_id=? and tv.term_type=?";
        return this.jdbcTemplate.query(sql, rm, loanId, termType); // NOSONAR
    }

    private static final class LoanTermVariationsMapper implements RowMapper<LoanTermVariationsData> {

        public String schema() {
            return "tv.id as id,tv.applicable_date as variationApplicableFrom,tv.decimal_value as decimalValue, tv.date_value as dateValue, tv.is_specific_to_installment as isSpecificToInstallment "
                    + "from m_loan_term_variations tv";
        }

        @Override
        public LoanTermVariationsData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final LocalDate variationApplicableFrom = JdbcSupport.getLocalDate(rs, "variationApplicableFrom");
            final BigDecimal decimalValue = rs.getBigDecimal("decimalValue");
            final LocalDate dateValue = JdbcSupport.getLocalDate(rs, "dateValue");
            final boolean isSpecificToInstallment = rs.getBoolean("isSpecificToInstallment");

            return new LoanTermVariationsData(id, LoanEnumerations.loanvariationType(LoanTermVariationType.EMI_AMOUNT),
                    variationApplicableFrom, decimalValue, dateValue, isSpecificToInstallment);
        }

    }

    @Override
    public Collection<LoanScheduleAccrualData> retriveScheduleAccrualData() {

        LoanScheduleAccrualMapper mapper = new LoanScheduleAccrualMapper();
        LocalDate organisationStartDate = this.configurationDomainService.retrieveOrganisationStartDate();
        final StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append("select ").append(mapper.schema()).append(
                " where (recaldet.is_compounding_to_be_posted_as_transaction is null or recaldet.is_compounding_to_be_posted_as_transaction = false) ")
                .append(" and (((ls.fee_charges_amount <> COALESCE(ls.accrual_fee_charges_derived, 0))")
                .append(" or ( ls.penalty_charges_amount <> COALESCE(ls.accrual_penalty_charges_derived, 0))")
                .append(" or ( ls.interest_amount <> COALESCE(ls.accrual_interest_derived, 0)))")
                .append(" and loan.loan_status_id=:active and mpl.accounting_type=:type and loan.is_npa=false and ls.duedate <= :currentDate) ");
        if (organisationStartDate != null) {
            sqlBuilder.append(" and ls.duedate > :organisationStartDate ");
        }
        sqlBuilder.append(" order by loan.id,ls.duedate ");
        Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("active", LoanStatus.ACTIVE.getValue());
        paramMap.put("type", AccountingRuleType.ACCRUAL_PERIODIC.getValue());
        paramMap.put("organisationStartDate", (organisationStartDate == null) ? DateUtils.getBusinessLocalDate() : organisationStartDate);
        paramMap.put("currentDate", DateUtils.getBusinessLocalDate());

        return this.namedParameterJdbcTemplate.query(sqlBuilder.toString(), paramMap, mapper);
    }

    @Override
    public Collection<LoanScheduleAccrualData> retrivePeriodicAccrualData(final LocalDate tillDate) {

        LoanSchedulePeriodicAccrualMapper mapper = new LoanSchedulePeriodicAccrualMapper();
        LocalDate organisationStartDate = this.configurationDomainService.retrieveOrganisationStartDate();
        final StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append("select ").append(mapper.schema()).append(
                " where  (recaldet.is_compounding_to_be_posted_as_transaction is null or recaldet.is_compounding_to_be_posted_as_transaction = false) ")
                .append(" and (((ls.fee_charges_amount <> COALESCE(ls.accrual_fee_charges_derived, 0))")
                .append(" or (ls.penalty_charges_amount <> COALESCE(ls.accrual_penalty_charges_derived, 0))")
                .append(" or (ls.interest_amount <> COALESCE(ls.accrual_interest_derived, 0)))")
                .append(" and loan.loan_status_id=:active and mpl.accounting_type=:type and (loan.closedon_date <= :tillDate or loan.closedon_date is null)")
                .append(" and loan.is_npa=false and (ls.duedate <= :tillDate or (ls.duedate > :tillDate and ls.fromdate < :tillDate))) ");
        Map<String, Object> paramMap = new HashMap<>(4);
        if (organisationStartDate != null) {
            sqlBuilder.append(" and ls.duedate > :organisationStartDate ");
            paramMap.put("organisationStartDate", organisationStartDate);
        }
        sqlBuilder.append(" order by loan.id,ls.duedate ");
        paramMap.put("active", LoanStatus.ACTIVE.getValue());
        paramMap.put("type", AccountingRuleType.ACCRUAL_PERIODIC.getValue());
        paramMap.put("tillDate", tillDate);

        return this.namedParameterJdbcTemplate.query(sqlBuilder.toString(), paramMap, mapper);
    }

    private static final class LoanSchedulePeriodicAccrualMapper implements RowMapper<LoanScheduleAccrualData> {

        public String schema() {
            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append("loan.id as loanId , (CASE WHEN loan.client_id is null THEN mg.office_id ELSE mc.office_id END) as officeId,")
                    .append("loan.accrued_till as accruedTill, loan.repayment_period_frequency_enum as frequencyEnum, ")
                    .append("loan.interest_calculated_from_date as interestCalculatedFrom, ").append("loan.repay_every as repayEvery,")
                    .append("ls.installment as installmentNumber, ")
                    .append("ls.duedate as duedate,ls.fromdate as fromdate ,ls.id as scheduleId,loan.product_id as productId,")
                    .append("ls.interest_amount as interest, ls.interest_waived_derived as interestWaived,")
                    .append("ls.penalty_charges_amount as penalty, ").append("ls.fee_charges_amount as charges, ")
                    .append("ls.accrual_interest_derived as accinterest,ls.accrual_fee_charges_derived as accfeecharege,ls.accrual_penalty_charges_derived as accpenalty,")
                    .append(" loan.currency_code as currencyCode,loan.currency_digits as currencyDigits,loan.currency_multiplesof as inMultiplesOf,")
                    .append("curr.display_symbol as currencyDisplaySymbol,curr.name as currencyName,curr.internationalized_name_code as currencyNameCode, curr.int_code as intCode")
                    .append(" from m_loan_repayment_schedule ls ").append(" left join m_loan loan on loan.id=ls.loan_id ")
                    .append(" left join m_product_loan mpl on mpl.id = loan.product_id")
                    .append(" left join m_client mc on mc.id = loan.client_id ").append(" left join m_group mg on mg.id = loan.group_id")
                    .append(" left join m_currency curr on curr.code = loan.currency_code")
                    .append(" left join m_loan_recalculation_details as recaldet on loan.id = recaldet.loan_id ");
            return sqlBuilder.toString();
        }

        @Override
        public LoanScheduleAccrualData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {

            final Long loanId = rs.getLong("loanId");
            final Long officeId = rs.getLong("officeId");
            final LocalDate accruedTill = JdbcSupport.getLocalDate(rs, "accruedTill");
            final LocalDate interestCalculatedFrom = JdbcSupport.getLocalDate(rs, "interestCalculatedFrom");
            final Integer installmentNumber = JdbcSupport.getInteger(rs, "installmentNumber");

            final Integer frequencyEnum = JdbcSupport.getInteger(rs, "frequencyEnum");
            final Integer repayEvery = JdbcSupport.getInteger(rs, "repayEvery");
            final PeriodFrequencyType frequency = PeriodFrequencyType.fromInt(frequencyEnum);
            final LocalDate dueDate = JdbcSupport.getLocalDate(rs, "duedate");
            final LocalDate fromDate = JdbcSupport.getLocalDate(rs, "fromdate");
            final Long repaymentScheduleId = rs.getLong("scheduleId");
            final Long loanProductId = rs.getLong("productId");
            final BigDecimal interestIncome = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "interest");
            final BigDecimal feeIncome = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "charges");
            final BigDecimal penaltyIncome = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "penalty");
            final BigDecimal interestIncomeWaived = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "interestWaived");
            final BigDecimal accruedInterestIncome = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "accinterest");
            final BigDecimal accruedFeeIncome = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "accfeecharege");
            final BigDecimal accruedPenaltyIncome = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "accpenalty");

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final Integer intCode = JdbcSupport.getInteger(rs, "intCode");
            final CurrencyData currencyData = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf,
                    currencyDisplaySymbol, currencyNameCode, intCode);

            return new LoanScheduleAccrualData(loanId, officeId, installmentNumber, accruedTill, frequency, repayEvery, dueDate, fromDate,
                    repaymentScheduleId, loanProductId, interestIncome, feeIncome, penaltyIncome, accruedInterestIncome, accruedFeeIncome,
                    accruedPenaltyIncome, currencyData, interestCalculatedFrom, interestIncomeWaived);
        }

    }

    private static final class LoanScheduleAccrualMapper implements RowMapper<LoanScheduleAccrualData> {

        public String schema() {
            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append("loan.id as loanId, (CASE WHEN loan.client_id is null THEN mg.office_id ELSE mc.office_id END) as officeId,")
                    .append("ls.duedate as duedate,ls.fromdate as fromdate,ls.id as scheduleId,loan.product_id as productId,")
                    .append("ls.installment as installmentNumber, ")
                    .append("ls.interest_amount as interest, ls.interest_waived_derived as interestWaived,")
                    .append("ls.penalty_charges_amount as penalty, ").append("ls.fee_charges_amount as charges, ")
                    .append("ls.accrual_interest_derived as accinterest,ls.accrual_fee_charges_derived as accfeecharege,ls.accrual_penalty_charges_derived as accpenalty,")
                    .append(" loan.currency_code as currencyCode,loan.currency_digits as currencyDigits,loan.currency_multiplesof as inMultiplesOf,")
                    .append("curr.display_symbol as currencyDisplaySymbol,curr.name as currencyName,curr.internationalized_name_code as currencyNameCode, curr.int_code as intCode ")
                    .append(" from m_loan_repayment_schedule ls ").append(" left join m_loan loan on loan.id=ls.loan_id ")
                    .append(" left join m_product_loan mpl on mpl.id = loan.product_id")
                    .append(" left join m_client mc on mc.id = loan.client_id ").append(" left join m_group mg on mg.id = loan.group_id")
                    .append(" left join m_currency curr on curr.code = loan.currency_code")
                    .append(" left join m_loan_recalculation_details as recaldet on loan.id = recaldet.loan_id ");
            return sqlBuilder.toString();
        }

        @Override
        public LoanScheduleAccrualData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {

            final Long loanId = rs.getLong("loanId");
            final Long officeId = rs.getLong("officeId");
            final Integer installmentNumber = JdbcSupport.getInteger(rs, "installmentNumber");
            final LocalDate dueDate = JdbcSupport.getLocalDate(rs, "duedate");
            final LocalDate fromdate = JdbcSupport.getLocalDate(rs, "fromdate");
            final Long repaymentScheduleId = rs.getLong("scheduleId");
            final Long loanProductId = rs.getLong("productId");
            final BigDecimal interestIncome = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "interest");
            final BigDecimal feeIncome = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "charges");
            final BigDecimal penaltyIncome = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "penalty");
            final BigDecimal interestIncomeWaived = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "interestWaived");
            final BigDecimal accruedInterestIncome = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "accinterest");
            final BigDecimal accruedFeeIncome = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "accfeecharege");
            final BigDecimal accruedPenaltyIncome = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "accpenalty");

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final Integer intCode = JdbcSupport.getInteger(rs, "intCode");
            final CurrencyData currencyData = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf,
                    currencyDisplaySymbol, currencyNameCode, intCode);
            final LocalDate accruedTill = null;
            final PeriodFrequencyType frequency = null;
            final Integer repayEvery = null;
            final LocalDate interestCalculatedFrom = null;
            return new LoanScheduleAccrualData(loanId, officeId, installmentNumber, accruedTill, frequency, repayEvery, dueDate, fromdate,
                    repaymentScheduleId, loanProductId, interestIncome, feeIncome, penaltyIncome, accruedInterestIncome, accruedFeeIncome,
                    accruedPenaltyIncome, currencyData, interestCalculatedFrom, interestIncomeWaived);
        }
    }

    @Override
    public LoanTransactionData retrieveRecoveryPaymentTemplate(Long loanId) {
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(LoanTransactionType.RECOVERY_REPAYMENT);
        final Collection<PaymentTypeData> paymentOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        BigDecimal outstandingLoanBalance = null;
        final BigDecimal unrecognizedIncomePortion = null;
        return new LoanTransactionData(null, null, null, transactionType, null, null, null, loan.getTotalWrittenOff(),
                loan.getNetDisbursalAmount(), null, null, null, null, null, unrecognizedIncomePortion, paymentOptions, null, null, null,
                outstandingLoanBalance, false);

    }

    @Override
    public LoanTransactionData retrieveLoanWriteoffTemplate(final Long loanId) {

        final LoanAccountData loan = this.retrieveOne(loanId);
        final BigDecimal outstandingLoanBalance = null;
        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(LoanTransactionType.WRITEOFF);
        final BigDecimal unrecognizedIncomePortion = null;
        final List<CodeValueData> writeOffReasonOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(LoanApiConstants.WRITEOFFREASONS));
        LoanTransactionData loanTransactionData = new LoanTransactionData(null, null, null, transactionType, null, loan.currency(),
                DateUtils.getBusinessLocalDate(), loan.getTotalOutstandingAmount(), loan.getNetDisbursalAmount(), null, null, null, null,
                null, null, null, null, outstandingLoanBalance, unrecognizedIncomePortion, false);
        loanTransactionData.setWriteOffReasonOptions(writeOffReasonOptions);
        return loanTransactionData;
    }

    @Override
    public Collection<Long> fetchLoansForInterestRecalculation() {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT ml.id FROM m_loan ml ");
        sqlBuilder.append(" INNER JOIN m_loan_repayment_schedule mr on mr.loan_id = ml.id ");
        sqlBuilder.append(" LEFT JOIN m_loan_disbursement_detail dd on dd.loan_id=ml.id and dd.disbursedon_date is null ");
        // For Floating rate changes
        sqlBuilder.append(
                " left join m_product_loan_floating_rates pfr on ml.product_id = pfr.loan_product_id and ml.is_floating_interest_rate = true");
        sqlBuilder.append(" left join m_floating_rates fr on  pfr.floating_rates_id = fr.id");
        sqlBuilder.append(" left join m_floating_rates_periods frp on fr.id = frp.floating_rates_id ");
        sqlBuilder.append(" left join m_loan_reschedule_request lrr on lrr.loan_id = ml.id");
        // this is to identify the applicable rates when base rate is changed
        sqlBuilder.append(" left join  m_floating_rates bfr on  bfr.is_base_lending_rate = true");
        sqlBuilder.append(" left join  m_floating_rates_periods bfrp on  bfr.id = bfrp.floating_rates_id and bfrp.created_date >= ?");
        sqlBuilder.append(" WHERE ml.loan_status_id = ? ");
        sqlBuilder.append(" and ml.is_npa = false ");
        sqlBuilder.append(" and ((");
        sqlBuilder.append("ml.interest_recalculation_enabled = true ");
        sqlBuilder.append(" and (ml.interest_recalcualated_on is null or ml.interest_recalcualated_on <> ?)");
        sqlBuilder.append(" and ((");
        sqlBuilder.append(" mr.completed_derived is false ");
        sqlBuilder.append(" and mr.duedate < ? )");
        sqlBuilder.append(" or dd.expected_disburse_date < ? )) ");
        sqlBuilder.append(" or (");
        sqlBuilder.append(" fr.is_active = true and  frp.is_active = true");
        sqlBuilder.append(" and (frp.created_date >= ?  or ");
        sqlBuilder
                .append("(bfrp.id is not null and frp.is_differential_to_base_lending_rate = true and frp.from_date >= bfrp.from_date)) ");
        sqlBuilder.append("and lrr.loan_id is null");
        sqlBuilder.append(" ))");
        sqlBuilder.append(" group by ml.id");
        try {
            LocalDate currentdate = DateUtils.getBusinessLocalDate();
            // will look only for yesterday modified rates
            LocalDate yesterday = DateUtils.getBusinessLocalDate().minusDays(1);
            return this.jdbcTemplate.queryForList(sqlBuilder.toString(), Long.class, yesterday, LoanStatus.ACTIVE.getValue(), currentdate,
                    currentdate, currentdate, yesterday);
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<Long> fetchLoansForInterestRecalculation(Integer pageSize, Long maxLoanIdInList, String officeHierarchy) {
        LocalDate currentdate = DateUtils.getBusinessLocalDate();
        // will look only for yesterday modified rates
        LocalDate yesterday = DateUtils.getBusinessLocalDate().minusDays(1);
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT ml.id FROM m_loan ml ");
        sqlBuilder.append(" left join m_client mc on mc.id = ml.client_id ");
        sqlBuilder.append(" left join m_office o on mc.office_id = o.id  ");
        sqlBuilder.append(" INNER JOIN m_loan_repayment_schedule mr on mr.loan_id = ml.id ");
        sqlBuilder.append(" LEFT JOIN m_loan_disbursement_detail dd on dd.loan_id=ml.id and dd.disbursedon_date is null ");
        // For Floating rate changes
        sqlBuilder.append(
                " left join m_product_loan_floating_rates pfr on ml.product_id = pfr.loan_product_id and ml.is_floating_interest_rate = true");
        sqlBuilder.append(" left join m_floating_rates fr on  pfr.floating_rates_id = fr.id");
        sqlBuilder.append(" left join m_floating_rates_periods frp on fr.id = frp.floating_rates_id ");
        sqlBuilder.append(" left join m_loan_reschedule_request lrr on lrr.loan_id = ml.id");
        // this is to identify the applicable rates when base rate is changed
        sqlBuilder.append(" left join  m_floating_rates bfr on  bfr.is_base_lending_rate = true");
        sqlBuilder.append(" left join  m_floating_rates_periods bfrp on  bfr.id = bfrp.floating_rates_id and bfrp.created_date >= ?");
        sqlBuilder.append(" WHERE ml.loan_status_id = ? ");
        sqlBuilder.append(" and ml.is_npa = false ");
        sqlBuilder.append(" and ((");
        sqlBuilder.append("ml.interest_recalculation_enabled = true ");
        sqlBuilder.append(" and (ml.interest_recalcualated_on is null or ml.interest_recalcualated_on <> ? )");
        sqlBuilder.append(" and ((");
        sqlBuilder.append(" mr.completed_derived is false ");
        sqlBuilder.append(" and mr.duedate < ? )");
        sqlBuilder.append(" or dd.expected_disburse_date < ? )) ");
        sqlBuilder.append(" or (");
        sqlBuilder.append(" fr.is_active = true and  frp.is_active = true");
        sqlBuilder.append(" and (frp.created_date >=  ?  or ");
        sqlBuilder
                .append("(bfrp.id is not null and frp.is_differential_to_base_lending_rate = true and frp.from_date >= bfrp.from_date)) ");
        sqlBuilder.append("and lrr.loan_id is null");
        sqlBuilder.append(" ))");
        sqlBuilder.append(" and ml.id >= ?  and o.hierarchy like ? ");
        sqlBuilder.append(" group by ml.id ");
        sqlBuilder.append(" limit ? ");
        try {
            return Collections.synchronizedList(
                    this.jdbcTemplate.queryForList(sqlBuilder.toString(), Long.class, yesterday, LoanStatus.ACTIVE.getValue(), currentdate,
                            currentdate, currentdate, yesterday, maxLoanIdInList, officeHierarchy, pageSize));
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public Collection<LoanTransactionData> retrieveWaiverLoanTransactions(final Long loanId) {
        try {

            final LoanTransactionDerivedComponentMapper rm = new LoanTransactionDerivedComponentMapper(sqlGenerator);

            final String sql = "select " + rm.schema()
                    + " where tr.loan_id = ? and tr.transaction_type_enum = ? and tr.is_reversed=false order by tr.transaction_date ASC,id ";
            return this.jdbcTemplate.query(sql, rm, loanId, LoanTransactionType.WAIVE_INTEREST.getValue()); // NOSONAR
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public boolean isGuaranteeRequired(final Long loanId) {
        final String sql = "select pl.hold_guarantee_funds from m_loan ml inner join m_product_loan pl on pl.id = ml.product_id where ml.id=?";
        return this.jdbcTemplate.queryForObject(sql, Boolean.class, loanId);
    }

    private static final class LoanTransactionDerivedComponentMapper implements RowMapper<LoanTransactionData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;

        LoanTransactionDerivedComponentMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String schema() {

            return " tr.id as id, tr.transaction_type_enum as transactionType, tr.transaction_date as " + sqlGenerator.escape("date")
                    + ", tr.amount as total, " + " tr.principal_portion_derived as principal, tr.interest_portion_derived as interest, "
                    + " tr.fee_charges_portion_derived as fees, tr.penalty_charges_portion_derived as penalties, "
                    + " tr.overpayment_portion_derived as overpayment, tr.outstanding_loan_balance_derived as outstandingLoanBalance, "
                    + " tr.unrecognized_income_portion as unrecognizedIncome " + " from m_loan_transaction tr ";
        }

        @Override
        public LoanTransactionData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final int transactionTypeInt = JdbcSupport.getInteger(rs, "transactionType");
            final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(transactionTypeInt);

            final LocalDate date = JdbcSupport.getLocalDate(rs, "date");
            final BigDecimal totalAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "total");
            final BigDecimal principalPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principal");
            final BigDecimal interestPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interest");
            final BigDecimal feeChargesPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "fees");
            final BigDecimal penaltyChargesPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penalties");
            final BigDecimal overPaymentPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "overpayment");
            final BigDecimal unrecognizedIncomePortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "unrecognizedIncome");
            final BigDecimal outstandingLoanBalance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "outstandingLoanBalance");

            return new LoanTransactionData(id, transactionType, date, totalAmount, null, principalPortion, interestPortion,
                    feeChargesPortion, penaltyChargesPortion, overPaymentPortion, unrecognizedIncomePortion, outstandingLoanBalance, false);
        }
    }

    @Override
    public Collection<LoanSchedulePeriodData> fetchWaiverInterestRepaymentData(final Long loanId) {
        try {

            final LoanRepaymentWaiverMapper rm = new LoanRepaymentWaiverMapper();

            final String sql = "select " + rm.getSchema()
                    + " where lrs.loan_id = ? and lrs.interest_waived_derived is not null order by lrs.installment ASC ";
            return this.jdbcTemplate.query(sql, rm, loanId); // NOSONAR
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }

    }

    private static final class LoanRepaymentWaiverMapper implements RowMapper<LoanSchedulePeriodData> {

        private final String sqlSchema;

        public String getSchema() {
            return this.sqlSchema;
        }

        LoanRepaymentWaiverMapper() {
            StringBuilder sb = new StringBuilder();
            sb.append("lrs.duedate as dueDate,lrs.interest_waived_derived interestWaived, lrs.installment as installment");
            sb.append(" from m_loan_repayment_schedule lrs ");
            sqlSchema = sb.toString();
        }

        @Override
        public LoanSchedulePeriodData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {

            final Integer period = JdbcSupport.getInteger(rs, "installment");
            final LocalDate dueDate = JdbcSupport.getLocalDate(rs, "dueDate");
            final BigDecimal interestWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWaived");

            final LocalDate fromDate = null;
            final LocalDate obligationsMetOnDate = null;
            final Boolean complete = false;
            final BigDecimal principalOriginalDue = null;
            final BigDecimal principalPaid = null;
            final BigDecimal principalWrittenOff = null;
            final BigDecimal principalOutstanding = null;
            final BigDecimal interestPaid = null;
            final BigDecimal interestWrittenOff = null;
            final BigDecimal interestOutstanding = null;
            final BigDecimal feeChargesDue = null;
            final BigDecimal feeChargesPaid = null;
            final BigDecimal feeChargesWaived = null;
            final BigDecimal feeChargesWrittenOff = null;
            final BigDecimal feeChargesOutstanding = null;
            final BigDecimal penaltyChargesDue = null;
            final BigDecimal penaltyChargesPaid = null;
            final BigDecimal penaltyChargesWaived = null;
            final BigDecimal penaltyChargesWrittenOff = null;
            final BigDecimal penaltyChargesOutstanding = null;

            final BigDecimal totalDueForPeriod = null;
            final BigDecimal totalPaidInAdvanceForPeriod = null;
            final BigDecimal totalPaidLateForPeriod = null;
            final BigDecimal totalActualCostOfLoanForPeriod = null;
            final BigDecimal outstandingPrincipalBalanceOfLoan = null;
            final BigDecimal interestDueOnPrincipalOutstanding = null;
            Long loanId = null;
            final BigDecimal totalWaived = null;
            final BigDecimal totalWrittenOff = null;
            final BigDecimal totalOutstanding = null;
            final BigDecimal totalPaid = null;
            final BigDecimal totalInstallmentAmount = null;

            return LoanSchedulePeriodData.repaymentPeriodWithPayments(loanId, period, fromDate, dueDate, obligationsMetOnDate, complete,
                    principalOriginalDue, principalPaid, principalWrittenOff, principalOutstanding, outstandingPrincipalBalanceOfLoan,
                    interestDueOnPrincipalOutstanding, interestPaid, interestWaived, interestWrittenOff, interestOutstanding, feeChargesDue,
                    feeChargesPaid, feeChargesWaived, feeChargesWrittenOff, feeChargesOutstanding, penaltyChargesDue, penaltyChargesPaid,
                    penaltyChargesWaived, penaltyChargesWrittenOff, penaltyChargesOutstanding, totalDueForPeriod, totalPaid,
                    totalPaidInAdvanceForPeriod, totalPaidLateForPeriod, totalWaived, totalWrittenOff, totalOutstanding,
                    totalActualCostOfLoanForPeriod, totalInstallmentAmount);
        }
    }

    @Override
    public LocalDate retrieveMinimumDateOfRepaymentTransaction(Long loanId) {
        return this.jdbcTemplate.queryForObject(
                "select min(transaction_date) from m_loan_transaction where loan_id=? and transaction_type_enum=2", LocalDate.class,
                loanId);
    }

    @Override
    public PaidInAdvanceData retrieveTotalPaidInAdvance(Long loanId) {
        try {
            final String sql = "  select (SUM(COALESCE(mr.principal_completed_derived, 0))"
                    + " + SUM(COALESCE(mr.interest_completed_derived, 0)) " + " + SUM(COALESCE(mr.fee_charges_completed_derived, 0)) "
                    + " + SUM(COALESCE(mr.penalty_charges_completed_derived, 0))) as total_in_advance_derived "
                    + " from m_loan ml INNER JOIN m_loan_repayment_schedule mr on mr.loan_id = ml.id "
                    + " where ml.id=? and  mr.duedate >= " + sqlGenerator.currentBusinessDate() + " group by ml.id having "
                    + " (SUM(COALESCE(mr.principal_completed_derived, 0))  " + " + SUM(COALESCE(mr.interest_completed_derived, 0)) "
                    + " + SUM(COALESCE(mr.fee_charges_completed_derived, 0)) "
                    + "+  SUM(COALESCE(mr.penalty_charges_completed_derived, 0))) > 0";
            BigDecimal bigDecimal = this.jdbcTemplate.queryForObject(sql, BigDecimal.class, loanId); // NOSONAR
            return new PaidInAdvanceData(bigDecimal);
        } catch (DataAccessException e) {
            return new PaidInAdvanceData(new BigDecimal(0));
        }
    }

    @Override
    public LoanTransactionData retrieveRefundByCashTemplate(Long loanId) {
        this.context.authenticatedUser();

        final Collection<PaymentTypeData> paymentOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        return retrieveRefundTemplate(loanId, LoanTransactionType.REFUND_FOR_ACTIVE_LOAN, paymentOptions, loan.getCurrency(),
                retrieveTotalPaidInAdvance(loan.getId()).getPaidInAdvance(), loan.getNetDisbursalAmount());
    }

    @Override
    public LoanTransactionData retrieveCreditBalanceRefundTemplate(Long loanId) {
        this.context.authenticatedUser();

        final Collection<PaymentTypeData> paymentOptions = null;
        final BigDecimal netDisbursal = null;
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        return retrieveRefundTemplate(loanId, LoanTransactionType.CREDIT_BALANCE_REFUND, paymentOptions, loan.getCurrency(),
                loan.getTotalOverpaid(), netDisbursal);

    }

    private LoanTransactionData retrieveRefundTemplate(Long loanId, LoanTransactionType loanTransactionType,
            Collection<PaymentTypeData> paymentOptions, MonetaryCurrency currency, BigDecimal transactionAmount, BigDecimal netDisbursal) {

        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);

        final CurrencyData currencyData = applicationCurrency.toData();

        final LocalDate currentDate = LocalDate.now(DateUtils.getDateTimeZoneOfTenant());

        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(loanTransactionType);
        return new LoanTransactionData(null, null, null, transactionType, null, currencyData, currentDate, transactionAmount, null,
                netDisbursal, null, null, null, null, null, paymentOptions, null, null, null, null, false);
    }

    @Override
    public Collection<InterestRatePeriodData> retrieveLoanInterestRatePeriodData(LoanAccountData loanData) {
        this.context.authenticatedUser();

        if (loanData.isLoanProductLinkedToFloatingRate()) {
            final Collection<InterestRatePeriodData> intRatePeriodData = new ArrayList<>();
            final Collection<InterestRatePeriodData> intRates = this.floatingRatesReadPlatformService
                    .retrieveInterestRatePeriods(loanData.loanProductId());
            for (final InterestRatePeriodData rate : intRates) {
                if (rate.getFromDate().compareTo(loanData.getDisbursementDate()) > 0 && loanData.isFloatingInterestRate()) {
                    updateInterestRatePeriodData(rate, loanData);
                    intRatePeriodData.add(rate);
                } else if (rate.getFromDate().compareTo(loanData.getDisbursementDate()) <= 0) {
                    updateInterestRatePeriodData(rate, loanData);
                    intRatePeriodData.add(rate);
                    break;
                }
            }

            return intRatePeriodData;
        }
        return null;
    }

    private void updateInterestRatePeriodData(InterestRatePeriodData rate, LoanAccountData loan) {
        LoanProductData loanProductData = loanProductReadPlatformService.retrieveLoanProductFloatingDetails(loan.loanProductId());
        rate.setLoanProductDifferentialInterestRate(loanProductData.getInterestRateDifferential());
        rate.setLoanDifferentialInterestRate(loan.getInterestRateDifferential());

        BigDecimal effectiveInterestRate = BigDecimal.ZERO;
        effectiveInterestRate = effectiveInterestRate.add(rate.getLoanDifferentialInterestRate());
        effectiveInterestRate = effectiveInterestRate.add(rate.getLoanProductDifferentialInterestRate());
        effectiveInterestRate = effectiveInterestRate.add(rate.getInterestRate());
        if (rate.getBlrInterestRate() != null && rate.isDifferentialToBLR()) {
            effectiveInterestRate = effectiveInterestRate.add(rate.getBlrInterestRate());
        }
        rate.setEffectiveInterestRate(effectiveInterestRate);

        if (rate.getFromDate().compareTo(loan.getDisbursementDate()) < 0) {
            rate.setFromDate(loan.getDisbursementDate());
        }
    }

    @Override
    public Collection<Long> retrieveLoanIdsWithPendingIncomePostingTransactions() {
        LocalDate currentdate = DateUtils.getBusinessLocalDate();
        StringBuilder sqlBuilder = new StringBuilder().append(" select distinct loan.id ").append(" from m_loan as loan ").append(
                " inner join m_loan_recalculation_details as recdet on (recdet.loan_id = loan.id and recdet.is_compounding_to_be_posted_as_transaction is not null and recdet.is_compounding_to_be_posted_as_transaction = true) ")
                .append(" inner join m_loan_repayment_schedule as repsch on repsch.loan_id = loan.id ")
                .append(" inner join m_loan_interest_recalculation_additional_details as adddet on adddet.loan_repayment_schedule_id = repsch.id ")
                .append(" left join m_loan_transaction as trans on (trans.is_reversed <> true and trans.transaction_type_enum = 19 and trans.loan_id = loan.id and trans.transaction_date = adddet.effective_date) ")
                .append(" where loan.loan_status_id = 300 ").append(" and loan.is_npa = false ")
                .append(" and adddet.effective_date is not null ").append(" and trans.transaction_date is null ")
                .append(" and adddet.effective_date < ? ");
        try {
            return this.jdbcTemplate.queryForList(sqlBuilder.toString(), new Object[] { currentdate }, Long.class);
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public LoanTransactionData retrieveLoanForeclosureTemplate(final Long loanId, final LocalDate transactionDate) {
        this.context.authenticatedUser();

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        loan.validateForForeclosure(transactionDate);
        final MonetaryCurrency currency = loan.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);

        final CurrencyData currencyData = applicationCurrency.toData();

        final LocalDate earliestUnpaidInstallmentDate = DateUtils.getBusinessLocalDate();

        final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = loan.fetchLoanForeclosureDetail(transactionDate);
        BigDecimal unrecognizedIncomePortion = null;
        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(LoanTransactionType.REPAYMENT);
        final Collection<PaymentTypeData> paymentTypeOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        final BigDecimal outstandingLoanBalance = loanRepaymentScheduleInstallment.getPrincipalOutstanding(currency).getAmount();
        final Boolean isReversed = false;

        final Money outStandingAmount = loanRepaymentScheduleInstallment.getTotalOutstanding(currency);

        return new LoanTransactionData(null, null, null, transactionType, null, currencyData, earliestUnpaidInstallmentDate,
                outStandingAmount.getAmount(), loan.getNetDisbursalAmount(),
                loanRepaymentScheduleInstallment.getPrincipalOutstanding(currency).getAmount(),
                loanRepaymentScheduleInstallment.getInterestOutstanding(currency).getAmount(),
                loanRepaymentScheduleInstallment.getFeeChargesOutstanding(currency).getAmount(),
                loanRepaymentScheduleInstallment.getPenaltyChargesOutstanding(currency).getAmount(), null, unrecognizedIncomePortion,
                paymentTypeOptions, null, null, null, outstandingLoanBalance, isReversed);
    }

    private static final class CurrencyMapper implements RowMapper<CurrencyData> {

        @Override
        public CurrencyData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {
            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final Integer intCode = JdbcSupport.getInteger(rs, "intCode");
            return new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf, currencyDisplaySymbol, currencyNameCode,
                    intCode);
        }

    }

    private static final class RepaymentTransactionTemplateMapper implements RowMapper<LoanTransactionData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;
        private final CurrencyMapper currencyMapper = new CurrencyMapper();

        RepaymentTransactionTemplateMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String schema() {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("(CASE ");
            sqlBuilder.append(
                    "WHEN (select max(tr.transaction_date) as transaction_date from m_loan_transaction tr where tr.loan_id = l.id AND tr.transaction_type_enum = ? AND tr.is_reversed = false) > ls.dueDate ");
            sqlBuilder.append(
                    "THEN (select max(tr.transaction_date) as transaction_date from m_loan_transaction tr where tr.loan_id = l.id AND tr.transaction_type_enum = ? AND tr.is_reversed = false) ");
            sqlBuilder.append("ELSE ls.dueDate END) as transactionDate, ");
            sqlBuilder.append(
                    "ls.principal_amount - coalesce(ls.principal_writtenoff_derived, 0) - coalesce(ls.principal_completed_derived, 0) as principalDue, ");
            sqlBuilder.append("mbc.required_guarantee_amount as collateralAmount, ");
            sqlBuilder.append(
                    "ls.interest_amount - coalesce(ls.interest_completed_derived, 0) - coalesce(ls.interest_waived_derived, 0) - coalesce(ls.interest_writtenoff_derived, 0) as interestDue, ");
            sqlBuilder.append(
                    "ls.fee_charges_amount - coalesce(ls.fee_charges_completed_derived, 0) - coalesce(ls.fee_charges_writtenoff_derived, 0) - coalesce(ls.fee_charges_waived_derived, 0) as feeDue, ");
            sqlBuilder.append(
                    "ls.penalty_charges_amount - coalesce(ls.penalty_charges_completed_derived, 0) - coalesce(ls.penalty_charges_writtenoff_derived, 0) - coalesce(ls.penalty_charges_waived_derived, 0) as penaltyDue, ");
            sqlBuilder.append(
                    "l.currency_code as currencyCode, l.currency_digits as currencyDigits, l.currency_multiplesof as inMultiplesOf, l.net_disbursal_amount as netDisbursalAmount, ");
            sqlBuilder.append("ls.installment AS installmentNumber, l.total_outstanding_derived AS totalOutstandingBalance, ");
            sqlBuilder.append("l.number_of_repayments AS numberOfRepayments, ");
            sqlBuilder.append("rc." + sqlGenerator.escape("name")
                    + " as currencyName, rc.display_symbol as currencyDisplaySymbol, rc.internationalized_name_code as currencyNameCode, rc.int_code AS intCode ");
            sqlBuilder.append("FROM m_loan l ");
            sqlBuilder.append("JOIN m_currency rc on rc." + sqlGenerator.escape("code") + " = l.currency_code ");
            sqlBuilder.append("JOIN m_loan_repayment_schedule ls ON ls.loan_id = l.id AND ls.completed_derived = false ");
            sqlBuilder.append("LEFT JOIN m_bank_check mbc ON mbc.id = l.cheque_id ");
            sqlBuilder.append(
                    "JOIN((SELECT ls.loan_id, ls.duedate as datedue FROM m_loan_repayment_schedule ls WHERE ls.loan_id = ? and ls.completed_derived = false ORDER BY ls.duedate LIMIT 1)) asq on asq.loan_id = ls.loan_id ");
            sqlBuilder.append("AND asq.datedue = ls.duedate ");
            sqlBuilder.append("WHERE l.id = ?");
            return sqlBuilder.toString();
        }

        @Override
        public LoanTransactionData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(LoanTransactionType.REPAYMENT);
            final CurrencyData currencyData = this.currencyMapper.mapRow(rs, rowNum);
            final LocalDate date = JdbcSupport.getLocalDate(rs, "transactionDate");
            final BigDecimal principalPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalDue");
            final BigDecimal interestDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestDue");
            final BigDecimal feeDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeDue");
            final BigDecimal penaltyDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyDue");
            final BigDecimal totalDue = principalPortion.add(interestDue).add(feeDue).add(penaltyDue);
            final BigDecimal outstandingLoanBalance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalOutstandingBalance");
            final BigDecimal collateralAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "collateralAmount");
            final Integer installmentNumber = JdbcSupport.getInteger(rs, "installmentNumber");
            final BigDecimal unrecognizedIncomePortion = null;
            final BigDecimal overPaymentPortion = null;
            final BigDecimal netDisbursalAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "netDisbursalAmount");
            final Long id = null;
            final Long officeId = null;
            final String officeName = null;
            boolean manuallyReversed = false;
            final PaymentDetailData paymentDetailData = null;
            final String externalId = null;
            final AccountTransferData transfer = null;
            final BigDecimal fixedEmiAmount = null;
            final Integer numberOfRepayments = JdbcSupport.getInteger(rs, "numberOfRepayments");

            LoanTransactionData loanTransactionData = new LoanTransactionData(id, officeId, officeName, transactionType, paymentDetailData,
                    currencyData, date, totalDue, netDisbursalAmount, principalPortion, interestDue, feeDue, penaltyDue, overPaymentPortion,
                    externalId, transfer, fixedEmiAmount, outstandingLoanBalance, unrecognizedIncomePortion, manuallyReversed);
            loanTransactionData.setInstallmentNumber(installmentNumber);
            loanTransactionData.setNumberOfRepayments(numberOfRepayments);
            loanTransactionData.setCollateralAmount(collateralAmount);
            return loanTransactionData;
        }

    }

    @Override
    public Long retrieveLoanIdByAccountNumber(String loanAccountNumber) {
        try {
            return this.jdbcTemplate.queryForObject("select l.id from m_loan l where l.account_no = ?", Long.class, loanAccountNumber);

        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public String retrieveAccountNumberByAccountId(Long accountId) {
        try {
            final String sql = "select loan.account_no from m_loan loan where loan.id = ?";
            return this.jdbcTemplate.queryForObject(sql, String.class, accountId);
        } catch (final EmptyResultDataAccessException e) {
            throw new LoanNotFoundException(accountId, e);
        }
    }

    @Override
    public Integer retrieveNumberOfActiveLoans() {
        final String sql = "select count(*) from m_loan";
        return this.jdbcTemplate.queryForObject(sql, Integer.class);
    }

    @Override
    public CollectionData retrieveLoanCollectionData(Long loanId) {
        final CollectionDataMapper mapper = new CollectionDataMapper(sqlGenerator);
        String sql = "select " + mapper.schema();
        CollectionData collectionData = this.jdbcTemplate.queryForObject(sql, mapper, loanId); // NOSONAR
        return collectionData;
    }

    @Override
    public LoanPaymentSimulationData retrieveLoanFuturePaymentTemplate(Long loanId, LocalDate paymentDate, String paymentType) {
        this.context.authenticatedUser();

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        loan.validateForFuturePayment(paymentDate);

        final List<CodeValueData> bankAgreements = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(LoanApiConstants.BANKAGREEMENTS));

        final MonetaryCurrency currency = loan.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);

        final CurrencyData currencyData = applicationCurrency.toData();
        final String paymentCode = loan.getLoanProduct().getName() + "(" + loan.getAccountNumber() + ")";

        BigDecimal outstandingLoanBalance = BigDecimal.ZERO;
        Money outStandingAmount = Money.zero(currency);
        BigDecimal interestPortion = BigDecimal.ZERO;
        BigDecimal lateInterestPortion = BigDecimal.ZERO;
        BigDecimal availableGuaranteeAmount = BigDecimal.ZERO;
        LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = null;

        LoanPaymentSimulationData loanPaymentSimulationData;
        List<String> loanBankAgreementsDataList = new ArrayList<>();
        for (CodeValueData bankAgreement : bankAgreements) {
            loanBankAgreementsDataList.add(bankAgreement.getName());
        }

        if (paymentType.equals(LoanApiConstants.totalPayment)) {
            loanRepaymentScheduleInstallment = loan.fetchLoanForeclosureDetail(paymentDate);
            outstandingLoanBalance = loanRepaymentScheduleInstallment.getPrincipalOutstanding(currency).getAmount();
            outStandingAmount = loanRepaymentScheduleInstallment.getTotalOutstanding(currency);
            interestPortion = loanRepaymentScheduleInstallment.getInterestOutstanding(currency).getAmount();
            lateInterestPortion = loanRepaymentScheduleInstallment.getPenaltyChargesCharged(currency).getAmount();
        } else if (paymentType.equals(LoanApiConstants.partialPayment)) {
            loanRepaymentScheduleInstallment = loan.fetchLoanFuturePaymentDetail(paymentDate);
            outstandingLoanBalance = loanRepaymentScheduleInstallment.getPrincipalOutstanding(currency).getAmount();
            outStandingAmount = loanRepaymentScheduleInstallment.getTotalOutstanding(currency);
            interestPortion = loanRepaymentScheduleInstallment.getInterestOutstanding(currency).getAmount();
            lateInterestPortion = loanRepaymentScheduleInstallment.getPenaltyChargesCharged(currency).getAmount();
        }

        loanPaymentSimulationData = new LoanPaymentSimulationData(paymentDate, paymentCode, outstandingLoanBalance, interestPortion,
                lateInterestPortion, outStandingAmount.getAmount(), availableGuaranteeAmount, currencyData);
        loanPaymentSimulationData.setLoanBankAgreements(loanBankAgreementsDataList);

        return loanPaymentSimulationData;
    }

    @Override
    public Collection<LoanAccountData> retrieveClientActiveLoans(Long clientId) {
        final LoanMapper rm = new LoanMapper(sqlGenerator);
        final String sql = "select distinct " + rm.loanSchema() + " where l.client_id = ? and l.loan_status_id = ?";
        return this.jdbcTemplate.query(sql, rm, new Object[] { clientId, LoanStatus.ACTIVE.getValue() });
    }

    @Override
    public GroupLoanAdditionalData retrieveAdditionalData(Long loanId) {
        final AdditionalGroupLoanData mapper = new AdditionalGroupLoanData(sqlGenerator);
        String sql = "select " + mapper.schema();
        GroupLoanAdditionalData groupLoanAdditionalData = null;
        try {
            groupLoanAdditionalData = this.jdbcTemplate.queryForObject(sql, mapper, loanId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }

        if (groupLoanAdditionalData != null) {
            AdditionalDataExtraLoansMapper extraLoansMapper = new AdditionalDataExtraLoansMapper(sqlGenerator);
            final String membersql = "select " + extraLoansMapper.schema();

            List<AdditionalsExtraLoansData> extraLoansData = this.jdbcTemplate.query(membersql, extraLoansMapper,
                    new Object[] { groupLoanAdditionalData.getId() });

            groupLoanAdditionalData.setExtraLoansData(extraLoansData);
        }

        return groupLoanAdditionalData;
    }

    private static final class AdditionalGroupLoanData implements RowMapper<GroupLoanAdditionalData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;
        private final CurrencyMapper currencyMapper = new CurrencyMapper();

        AdditionalGroupLoanData(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String schema() {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
                                gla.id as additionalId,
                                concat(user.firstname, ' ', user.lastname) as facilitatorName,
                                user.id as facilitatorId,
                                gla.loan_cycle_completed as loanCycleCompleted,
                                loanCycleCV.code_description as loanCycleCompletedValue,
                                cancellationReasonCV.code_description as earlyCancellationReasonValue,
                                gla.early_cancellation_reason as earlyCancellationReason,
                                gla.source_of_funds as sourceOfFunds,
                                sourceOfFundsCV.code_description as sourceOfFundsValue,
                                gla.client_loan_request_number as clientLoanRequestNumber,
                                gla.date_requested as dateRequested,
                                gla.date_of_birth as dateOfBirth,
                                gla.position,
                                groupPositionCV.code_description as positionValue,
                                gla.full_name as fullName,
                                gla.last_name as lastName,
                                gla.marital_status as maritalStatus,
                                maritalStatusCV.code_description as maritalStatusValue,
                                gla.education_level as educationLevel,
                                educationLevelCV.code_description as educationLevelValue,
                                gla.years_of_schooling as schoolingYears,
                                gla.number_of_children as noOfChildren,
                                gla.nationality as nationality,
                                gla.language as language,
                                gla.dpi as dpi,
                                gla.nit as nit,
                                gla.job_type as jobType,
                                jobtypeCV.code_description as jobTypeValue,
                                gla.occupancy_classification as occupancyClassification,
                                eco.name as occupancyClassificationValue,
                                gla.acts_own_behalf as actsOwnBehalf,
                                ownBehalfCV.code_description as actsOwnBehalfValue,
                                gla.on_behalf_of as onBehalfOf,
                                gla.political_position as politicalPosition,
                                gla.political_office as politicalOffice,
                                gla.housing_type as housingType,
                                housingTypeCV.code_description as housingTypeValue,
                                gla.address as address,
                                gla.populated_place as populatedPlace,
                                gla.reference_point as referencePoint,
                                gla.phone_number as phoneNumber,
                                gla.relative_number as relativeNumber,
                                gla.years_in_community as yearsInCommunity,
                                gla.rent_fee as rentFee,
                                gla.mortgage_fee as mortgageFee,
                                gla.monthly_income as monthlyIncome,
                                gla.other_income as otherIncome,
                                gla.family_expenses as familyExpenses,
                                gla.total_external_loan_amount as totalExternalLoanAmount,
                                gla.total_installments as totalInstallments,
                                gla.client_type clientType,
                                clientTypeCV.code_description as clientTypeValue,
                                gla.house_hold_goods as houseHoldGoods,
                                gla.business_activities as businessActivities,
                                gla.business_location as businessLocation,
                                businessLocationCV.code_description as businessLocationValue,
                                gla.business_experience as businessExperience,
                                businessExperienceCV.code_description as businessExperienceValue,
                                gla.sales_value as salesValue,
                                gla.business_purchases as businessPurchases,
                                gla.business_profit as businessProfit,
                                gla.client_profit as clientProfit,
                                gla.inventories as inventories,
                                gla.visit_business as visitBusiness,
                                visitBusinessCV.code_description as visitBusinessValue,
                                gla.family_support as familySupport,
                                famSupportCV.code_description as familySupportValue,
                                gla.business_evolution as businessEvolution,
                                businessEvolutionCV.code_description as businessEvolutionValue,
                                gla.number_of_approvals as numberOfApprovals,
                                gla.recommender_name as recommenderName,
                                gla.monthly_payment_capacity as monthlyPaymentCapacity,
                                gla.loan_purpose as loanPurpose,
                                loanPurposeCV.code_description as loanPurposeValue,
                                gla.current_credit_value as currentCreditValue,
                                gla.requested_value as requestedValue,
                                gla.group_authorized_value as groupAuthorizedValue,
                                gla.facilitator_proposed_value as facilitatorProposedValue,
                                gla.proposed_fee as proposedFee,
                                gla.agency_authorized_amount as agencyAuthorizedAmount,
                                gla.authorized_fee as authorizedFee,
                                gla.total_income as totalIncome,
                                gla.total_expenditures as totalExpenditures,
                                gla.available_monthly as availableMonthly,
                                gla.f_a_c as facValue,
                                gla.debt_level as debtLevel
                    FROM m_loan_additionals_group gla
                    LEFT JOIN m_appuser user ON user.id = gla.facilitator
                    LEFT JOIN m_code_value loanCycleCV ON loanCycleCV.id = gla.loan_cycle_completed
                    LEFT JOIN m_code_value businessEvolutionCV ON businessEvolutionCV.id = gla.business_evolution
                    LEFT JOIN m_code_value businessExperienceCV ON businessExperienceCV.id = gla.business_experience
                    LEFT JOIN m_code_value businessLocationCV ON businessLocationCV.id = gla.business_location
                    LEFT JOIN m_sector_economico eco ON eco.id = gla.occupancy_classification
                    LEFT JOIN m_code_value educationLevelCV ON educationLevelCV.id = gla.education_level
                    LEFT JOIN m_code_value maritalStatusCV ON maritalStatusCV.id = gla.marital_status
                    LEFT JOIN m_code_value groupPositionCV ON groupPositionCV.id = gla.position
                    LEFT JOIN m_code_value sourceOfFundsCV ON sourceOfFundsCV.id = gla.source_of_funds
                    LEFT JOIN m_code_value ownBehalfCV ON ownBehalfCV.id = gla.acts_own_behalf
                    LEFT JOIN m_code_value jobtypeCV ON jobtypeCV.id = gla.job_type
                    LEFT JOIN m_code_value clientTypeCV ON clientTypeCV.id = gla.client_type
                    LEFT JOIN m_code_value housingTypeCV ON housingTypeCV.id = gla.housing_type
                    LEFT JOIN m_code_value visitBusinessCV ON visitBusinessCV.id = gla.visit_business
                    LEFT JOIN m_code_value famSupportCV ON famSupportCV.id = gla.family_support
                    LEFT JOIN m_code_value loanPurposeCV ON loanPurposeCV.id = gla.loan_purpose
                    LEFT JOIN m_code_value cancellationReasonCV ON cancellationReasonCV.id = gla.early_cancellation_reason
                    WHERE gla.loan_id = ?
                    """);
            return sqlBuilder.toString();
        }

        @Override
        public GroupLoanAdditionalData mapRow(ResultSet rs, int rowNum) throws SQLException {

            final Long id = rs.getLong("additionalId");
            final String facilitatorName = rs.getString("facilitatorName");
            final Long facilitatorId = rs.getLong("facilitatorId");
            final Integer loanCycleCompleted = rs.getInt("loanCycleCompleted");
            final String loanCycleCompletedValue = rs.getString("loanCycleCompletedValue");
            final String earlyCancellationReasonValue = rs.getString("earlyCancellationReasonValue");
            final Long earlyCancellationReason = rs.getLong("earlyCancellationReason");
            final Long sourceOfFunds = rs.getLong("sourceOfFunds");
            final String sourceOfFundsValue = rs.getString("sourceOfFundsValue");
            final String clientLoanRequestNumber = rs.getString("clientLoanRequestNumber");
            final LocalDate dateRequested = JdbcSupport.getLocalDate(rs, "dateRequested");
            final LocalDate dateOfBirth = JdbcSupport.getLocalDate(rs, "dateOfBirth");
            final Long position = rs.getLong("position");
            final String positionValue = rs.getString("positionValue");
            final String fullName = rs.getString("fullName");
            final String lastName = rs.getString("lastName");
            final Long maritalStatus = rs.getLong("maritalStatus");
            final String maritalStatusValue = rs.getString("maritalStatusValue");
            final Long educationLevel = rs.getLong("educationLevel");
            final String educationLevelValue = rs.getString("educationLevelValue");
            final Integer schoolingYears = rs.getInt("schoolingYears");
            final Integer noOfChildren = rs.getInt("noOfChildren");
            final String nationality = rs.getString("nationality");
            final String language = rs.getString("language");
            final String dpi = rs.getString("dpi");
            final String nit = rs.getString("nit");
            final Long jobType = rs.getLong("jobType");
            final String jobTypeValue = rs.getString("jobTypeValue");
            final Long occupancyClassification = rs.getLong("occupancyClassification");
            final String occupancyClassificationValue = rs.getString("occupancyClassificationValue");
            final Long actsOwnBehalf = rs.getLong("actsOwnBehalf");
            final String actsOwnBehalfValue = rs.getString("actsOwnBehalfValue");
            final String onBehalfOf = rs.getString("onBehalfOf");
            final String politicalPosition = rs.getString("politicalPosition");
            final String politicalOffice = rs.getString("politicalOffice");
            final Long housingType = rs.getLong("housingType");
            final String housingTypeValue = rs.getString("housingTypeValue");
            final String address = rs.getString("address");
            final String populatedPlace = rs.getString("populatedPlace");
            final String referencePoint = rs.getString("referencePoint");
            final String phoneNumber = rs.getString("phoneNumber");
            final String relativeNumber = rs.getString("relativeNumber");
            final Integer yearsInCommunity = rs.getInt("yearsInCommunity");

            final BigDecimal rentFee = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "rentFee");
            final BigDecimal mortgageFee = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "mortgageFee");
            final BigDecimal monthlyIncome = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "monthlyIncome");
            final BigDecimal otherIncome = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "otherIncome");
            final BigDecimal familyExpenses = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "familyExpenses");
            final BigDecimal totalExternalLoanAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalExternalLoanAmount");
            final Integer totalInstallments = rs.getInt("totalInstallments");
            final Integer clientType = rs.getInt("clientType");
            final String clientTypeValue = rs.getString("clientTypeValue");
            final String houseHoldGoods = rs.getString("houseHoldGoods");
            final String businessActivities = rs.getString("businessActivities");
            final Long businessLocation = rs.getLong("businessLocation");
            final String businessLocationValue = rs.getString("businessLocationValue");
            final Integer businessExperience = rs.getInt("businessExperience");
            final String businessExperienceValue = rs.getString("businessExperienceValue");
            final BigDecimal salesValue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "salesValue");
            final BigDecimal businessPurchases = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "businessPurchases");
            final BigDecimal businessProfit = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "businessProfit");
            final BigDecimal clientProfit = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "clientProfit");
            final BigDecimal inventories = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "inventories");
            final Long visitBusiness = rs.getLong("visitBusiness");
            final String visitBusinessValue = rs.getString("visitBusinessValue");
            final Long familySupport = rs.getLong("familySupport");
            final String familySupportValue = rs.getString("familySupportValue");
            final Long businessEvolution = rs.getLong("businessEvolution");
            final String businessEvolutionValue = rs.getString("businessEvolutionValue");
            final Integer numberOfApprovals = rs.getInt("numberOfApprovals");
            final String recommenderName = rs.getString("recommenderName");
            final BigDecimal monthlyPaymentCapacity = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "monthlyPaymentCapacity");
            final Long loanPurpose = rs.getLong("loanPurpose");
            final String loanPurposeValue = rs.getString("loanPurposeValue");
            final BigDecimal currentCreditValue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "currentCreditValue");
            final BigDecimal requestedValue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "requestedValue");
            final BigDecimal groupAuthorizedValue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "groupAuthorizedValue");
            final BigDecimal facilitatorProposedValue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "facilitatorProposedValue");
            final BigDecimal proposedFee = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "proposedFee");
            final BigDecimal agencyAuthorizedAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "agencyAuthorizedAmount");
            final BigDecimal authorizedFee = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "authorizedFee");
            final BigDecimal totalIncome = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalIncome");
            final BigDecimal totalExpenditures = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalExpenditures");
            final BigDecimal availableMonthly = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "availableMonthly");
            final BigDecimal facValue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "facValue");
            final BigDecimal debtLevel = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "debtLevel");

            GroupLoanAdditionalData additionalGroupLoanData = GroupLoanAdditionalData.builder().id(id).facilitatorName(facilitatorName)
                    .facilitatorId(facilitatorId).loanCycleCompleted(loanCycleCompleted).loanCycleCompletedValue(loanCycleCompletedValue)
                    .earlyCancellationReasonValue(earlyCancellationReasonValue).earlyCancellationReason(earlyCancellationReason)
                    .sourceOfFunds(sourceOfFunds).sourceOfFundsValue(sourceOfFundsValue).clientLoanRequestNumber(clientLoanRequestNumber)
                    .dateRequested(dateRequested).position(position).positionValue(positionValue).fullName(fullName).lastName(lastName)
                    .maritalStatus(maritalStatus).maritalStatusValue(maritalStatusValue).educationLevel(educationLevel)
                    .educationLevelValue(educationLevelValue).schoolingYears(schoolingYears).noOfChildren(noOfChildren)
                    .nationality(nationality).language(language).dpi(dpi).nit(nit).jobType(jobType).jobTypeValue(jobTypeValue)
                    .occupancyClassification(occupancyClassification).occupancyClassificationValue(occupancyClassificationValue)
                    .actsOwnBehalf(actsOwnBehalf).actsOwnBehalfValue(actsOwnBehalfValue).onBehalfOf(onBehalfOf)
                    .politicalPosition(politicalPosition).politicalOffice(politicalOffice).housingType(housingType)
                    .housingTypeValue(housingTypeValue).address(address).populatedPlace(populatedPlace).referencePoint(referencePoint)
                    .phoneNumber(phoneNumber).relativeNumber(relativeNumber).yearsInCommunity(yearsInCommunity).rentFee(rentFee)
                    .mortgageFee(mortgageFee).monthlyIncome(monthlyIncome).familyExpenses(familyExpenses)
                    .totalExternalLoanAmount(totalExternalLoanAmount).totalInstallments(totalInstallments).clientType(clientType)
                    .clientTypeValue(clientTypeValue).houseHoldGoods(houseHoldGoods).businessActivities(businessActivities)
                    .businessLocation(businessLocation).businessLocationValue(businessLocationValue).businessExperience(businessExperience)
                    .businessExperienceValue(businessExperienceValue).salesValue(salesValue).businessPurchases(businessPurchases)
                    .businessProfit(businessProfit).clientProfit(clientProfit).inventories(inventories).visitBusiness(visitBusiness)
                    .visitBusinessValue(visitBusinessValue).familySupport(familySupport).familySupportValue(familySupportValue)
                    .businessEvolution(businessEvolution).businessEvolutionValue(businessEvolutionValue)
                    .numberOfApprovals(numberOfApprovals).recommenderName(recommenderName).monthlyPaymentCapacity(monthlyPaymentCapacity)
                    .loanPurpose(loanPurpose).loanPurposeValue(loanPurposeValue).currentCreditValue(currentCreditValue)
                    .requestedValue(requestedValue).groupAuthorizedValue(groupAuthorizedValue)
                    .facilitatorProposedValue(facilitatorProposedValue).proposedFee(proposedFee)
                    .agencyAuthorizedAmount(agencyAuthorizedAmount).authorizedFee(authorizedFee).totalIncome(totalIncome)
                    .totalExpenditures(totalExpenditures).availableMonthly(availableMonthly).facValue(facValue).debtLevel(debtLevel)
                    .dateOfBirth(dateOfBirth).otherIncome(otherIncome).build();
            return additionalGroupLoanData;
        }

    }

    private static final class AdditionalDataExtraLoansMapper implements RowMapper<AdditionalsExtraLoansData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;

        AdditionalDataExtraLoansMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String schema() {
            StringBuilder sqlBuilder = new StringBuilder();

            sqlBuilder.append("""
                                gla.institution_name as institutionName,
                                gla.institution_type as institutionType,
                                institutionTypeCV.code_description as institutionTypeValue,
                                gla.loan_amount as loanAmount,
                                gla.balance as balance,
                                gla.fees as fees,
                                gla.loan_status as loanStatus,
                                loanStatusCV.code_description as loanStatusValue
                    FROM m_loan_external_existing_loans gla
                    LEFT JOIN m_code_value institutionTypeCV ON institutionTypeCV.id = gla.institution_type
                    LEFT JOIN m_code_value loanStatusCV ON loanStatusCV.id = gla.loan_status
                    WHERE gla.additionals_id = ?
                    """);

            return sqlBuilder.toString();
        }

        @Override
        public AdditionalsExtraLoansData mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long institutionType = rs.getLong("institutionType");
            String institutionTypeValue = rs.getString("institutionTypeValue");
            BigDecimal loanAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "loanAmount");
            BigDecimal balance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "balance");
            BigDecimal fees = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "fees");
            Long loanStatus = rs.getLong("loanStatus");
            String loanStatusValue = rs.getString("loanStatusValue");
            String institutionName = rs.getString("institutionName");
            AdditionalsExtraLoansData additionalExtraLoansData = AdditionalsExtraLoansData.builder().institutionName(institutionName)
                    .institutionType(institutionType).institutionTypeValue(institutionTypeValue).loanAmount(loanAmount).balance(balance)
                    .fees(fees).loanStatus(loanStatus).loanStatusValue(loanStatusValue).build();
            return additionalExtraLoansData;
        }
    }

    private static final class CollectionDataMapper implements RowMapper<CollectionData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;

        CollectionDataMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String schema() {
            StringBuilder sqlBuilder = new StringBuilder();

            sqlBuilder.append(
                    "l.id as loanId, coalesce((l.approved_principal - l.principal_disbursed_derived), 0) as availableDisbursementAmount, ");
            sqlBuilder.append(
                    sqlGenerator.dateDiff(sqlGenerator.currentBusinessDate(), "laa.overdue_since_date_derived") + " as pastDueDays, ");
            sqlBuilder.append(
                    "(select coalesce(min(lrs.duedate), null) as duedate from m_loan_repayment_schedule lrs where lrs.loan_id=l.id and lrs.completed_derived is false and lrs.duedate >= "
                            + sqlGenerator.currentBusinessDate() + ") as nextPaymentDueDate, ");
            sqlBuilder.append(
                    sqlGenerator.dateDiff(sqlGenerator.currentBusinessDate(), "laa.overdue_since_date_derived") + " as delinquentDays, ");
            sqlBuilder.append(sqlGenerator.currentBusinessDate()
                    + " as delinquentDate, coalesce(laa.total_overdue_derived, 0) as delinquentAmount, ");
            sqlBuilder.append("lre.transactionDate as lastPaymentDate, coalesce(lre.amount, 0) as lastPaymentAmount ");
            sqlBuilder.append("from m_loan l left join m_loan_arrears_aging laa on laa.loan_id = l.id ");
            sqlBuilder.append(
                    "left join (select lt.loan_id, lt.transaction_date as transactionDate, lt.amount as amount from m_loan_transaction lt ");
            sqlBuilder.append(
                    "where lt.is_reversed = false and lt.transaction_type_enum=2 order by lt.transaction_date desc limit 1) lre on lre.loan_id = l.id ");
            sqlBuilder.append("where l.id=? ");
            return sqlBuilder.toString();
        }

        @Override
        public CollectionData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final LocalDate nextPaymentDueDate = JdbcSupport.getLocalDate(rs, "nextPaymentDueDate");
            final LocalDate delinquentDate = JdbcSupport.getLocalDate(rs, "delinquentDate");
            final LocalDate lastPaymentDate = JdbcSupport.getLocalDate(rs, "lastPaymentDate");
            final BigDecimal availableDisbursementAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "availableDisbursementAmount");
            final BigDecimal delinquentAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "delinquentAmount");
            final BigDecimal lastPaymentAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "lastPaymentAmount");
            final int pastDueDays = rs.getInt("pastDueDays");
            final int delinquentDays = rs.getInt("delinquentDays");

            return CollectionData.instance(availableDisbursementAmount, pastDueDays, nextPaymentDueDate, delinquentDays, delinquentDate,
                    delinquentAmount, lastPaymentDate, lastPaymentAmount);
        }
    }

}
