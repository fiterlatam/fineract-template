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

import static java.lang.Boolean.TRUE;
import static org.apache.fineract.portfolio.loanproduct.service.LoanEnumerations.interestType;

import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.custom.infrastructure.channel.data.ChannelData;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelType;
import org.apache.fineract.custom.infrastructure.channel.service.ChannelReadWritePlatformService;
import org.apache.fineract.infrastructure.clientblockingreasons.data.BlockingReasonsData;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationProperty;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepository;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.organisation.staff.service.StaffReadPlatformService;
import org.apache.fineract.portfolio.account.data.AccountTransferData;
import org.apache.fineract.portfolio.accountdetails.data.LoanAccountSummaryData;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.accountdetails.service.AccountDetailsReadPlatformService;
import org.apache.fineract.portfolio.accountdetails.service.AccountEnumerations;
import org.apache.fineract.portfolio.calendar.data.CalendarData;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.service.CalendarReadPlatformService;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeCalculationType;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.client.data.ClientAdditionalFieldsData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.data.PointOfSalesData;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientEnumerations;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.common.service.CommonEnumerations;
import org.apache.fineract.portfolio.delinquency.data.DelinquencyRangeData;
import org.apache.fineract.portfolio.delinquency.service.DelinquencyReadPlatformService;
import org.apache.fineract.portfolio.floatingrates.data.InterestRatePeriodData;
import org.apache.fineract.portfolio.floatingrates.service.FloatingRatesReadPlatformService;
import org.apache.fineract.portfolio.fund.data.FundData;
import org.apache.fineract.portfolio.fund.service.FundReadPlatformService;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.group.data.GroupRoleData;
import org.apache.fineract.portfolio.group.service.GroupReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.data.*;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleTransactionProcessorFactory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanSubStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTermVariationType;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRelation;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRelationRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.exception.LoanNotFoundException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanTransactionNotFoundException;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleProcessingType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.mapper.LoanTransactionRelationMapper;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

@AllArgsConstructor
@Transactional(readOnly = true)
public class LoanReadPlatformServiceImpl implements LoanReadPlatformService, LoanReadPlatformServiceCommon {

    private static final String ACCRUAL_ON_CHARGE_SUBMITTED_ON_DATE = "submitted-date";
    private static final Logger log = LoggerFactory.getLogger(LoanReadPlatformServiceImpl.class);
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
    private final CalendarReadPlatformService calendarReadPlatformService;
    private final StaffReadPlatformService staffReadPlatformService;
    private final PaginationHelper paginationHelper;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final PaymentTypeReadPlatformService paymentTypeReadPlatformService;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;
    private final FloatingRatesReadPlatformService floatingRatesReadPlatformService;
    private final LoanUtilService loanUtilService;
    private final ConfigurationDomainService configurationDomainService;
    private final AccountDetailsReadPlatformService accountDetailsReadPlatformService;
    private final ColumnValidator columnValidator;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final DelinquencyReadPlatformService delinquencyReadPlatformService;
    private final LoanTransactionRepository loanTransactionRepository;
    private final LoanTransactionRelationRepository loanTransactionRelationRepository;
    private final LoanTransactionRelationMapper loanTransactionRelationMapper;
    private final LoanChargePaidByReadPlatformService loanChargePaidByReadPlatformService;
    private final ChannelReadWritePlatformService channelReadWritePlatformService;
    private final GlobalConfigurationRepository globalConfigurationRepository;
    private final LoanScheduleHistoryReadPlatformService loanScheduleHistoryReadPlatformService;

    @Override
    public LoanAccountData retrieveOne(final Long loanId) {

        try {
            final String hierarchy = getHierarchyString();
            final String hierarchySearchString = hierarchy + "%";

            final LoanMapper rm = new LoanMapper(sqlGenerator, delinquencyReadPlatformService);

            final StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("select ");
            sqlBuilder.append(rm.loanSchema());
            sqlBuilder.append(" join m_office o on (o.id = c.office_id or o.id = g.office_id) ");
            sqlBuilder.append(" left join m_office transferToOffice on transferToOffice.id = c.transfer_to_office_id ");
            sqlBuilder.append(" where l.id=? and ( o.hierarchy like ? or transferToOffice.hierarchy like ?)");

            List<LoanAccountData> results = this.jdbcTemplate.query(sqlBuilder.toString(), rm, loanId, hierarchySearchString,
                    hierarchySearchString);
            if (results.isEmpty()) {
                throw new LoanNotFoundException(loanId);
            }
            return results.get(0);
        } catch (final EmptyResultDataAccessException e) {
            throw new LoanNotFoundException(loanId, e);
        }
    }

    private String getHierarchyString() {
        AppUser currentUser = null;
        if (this.context != null) {
            currentUser = this.context.getAuthenticatedUserIfPresent();
        }
        return Optional.ofNullable(currentUser).map(appUser -> appUser.getOffice().getHierarchy()).orElse(".");
    }

    @Override
    public LoanAccountData retrieveLoanByLoanAccount(String loanAccountNumber) {

        // final AppUser currentUser = this.context.authenticatedUser();
        this.context.authenticatedUser();
        final LoanMapper rm = new LoanMapper(sqlGenerator, delinquencyReadPlatformService);

        final String sql = "select " + rm.loanSchema() + " where l.account_no=?";

        return this.jdbcTemplate.queryForObject(sql, rm, loanAccountNumber); // NOSONAR

    }

    @Override
    public List<LoanAccountData> retrieveGLIMChildLoansByGLIMParentAccount(String parentloanAccountNumber) {
        this.context.authenticatedUser();
        final LoanMapper rm = new LoanMapper(sqlGenerator, delinquencyReadPlatformService);

        final String sql = "select " + rm.loanSchema()
                + " left join glim_parent_child_mapping as glim on glim.glim_child_account_id=l.account_no "
                + "where glim.glim_parent_account_id=?";

        return this.jdbcTemplate.query(sql, rm, parentloanAccountNumber); // NOSONAR

    }

    @Override
    public LoanAccountData fetchRepaymentScheduleData(LoanAccountData accountData) {
        final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedData = accountData.getTimeline().repaymentScheduleRelatedData(
                accountData.getCurrency(), accountData.getPrincipal(), accountData.getApprovedPrincipal(),
                accountData.getInArrearsTolerance(), accountData.getFeeChargesAtDisbursementCharged());

        final Collection<DisbursementData> disbursementData = retrieveLoanDisbursementDetails(accountData.getId());
        final LoanScheduleData repaymentSchedule = retrieveRepaymentSchedule(accountData.getId(), repaymentScheduleRelatedData,
                disbursementData, accountData.isInterestRecalculationEnabled(),
                LoanScheduleType.fromEnumOptionData(accountData.getLoanScheduleType()));
        accountData.setRepaymentSchedule(repaymentSchedule);
        return accountData;
    }

    @Override
    public LoanScheduleData retrieveRepaymentSchedule(final Long loanId,
            final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedLoanData, Collection<DisbursementData> disbursementData,
            boolean isInterestRecalculationEnabled, LoanScheduleType loanScheduleType) {

        try {
            this.context.authenticatedUser();

            final LoanScheduleResultSetExtractor fullResultsetExtractor = new LoanScheduleResultSetExtractor(
                    repaymentScheduleRelatedLoanData, disbursementData, isInterestRecalculationEnabled, loanScheduleType);
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
            final String sql = "select " + rm.loanPaymentsSchema() + " where tr.loan_id = ? and tr.transaction_type_enum not in (0, 3) "
                    + " and (tr.is_reversed=false or tr.manually_adjusted_or_reversed = true)  order by tr.transaction_date, tr.created_on_utc, tr.id ";
            Collection<LoanTransactionData> loanTransactionData = this.jdbcTemplate.query(sql, rm, loanId); // NOSONAR
            // TODO: would worth to rework in the future. It is not nice to fetch relations one by one... might worth to
            // give a try to get rid of native queries
            for (LoanTransactionData loanTransaction : loanTransactionData) {
                loanTransaction
                        .setLoanTransactionRelations(this.retrieveLoanTransactionRelationsByLoanTransactionId(loanTransaction.getId()));
                loanTransaction.setLoanChargePaidByList(
                        loanChargePaidByReadPlatformService.getLoanChargesPaidByTransactionId(loanTransaction.getId()));
            }
            return loanTransactionData;
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public Page<LoanAccountData> retrieveAll(final SearchParameters searchParameters) {

        final AppUser currentUser = this.context.authenticatedUser();
        final String hierarchy = currentUser.getOffice().getHierarchy();
        final String hierarchySearchString = hierarchy + "%";
        final LoanMapper loanMapper = new LoanMapper(sqlGenerator, delinquencyReadPlatformService);

        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select ").append(sqlGenerator.calcFoundRows()).append(" ");
        sqlBuilder.append(loanMapper.loanSchema());

        // TODO - for time being this will data scope list of loans returned to
        // only loans that have a client associated.
        // to support scenario where loan has group_id only OR client_id will
        // probably require a UNION query
        // but that at present is an edge case
        sqlBuilder.append(" join m_office o on (o.id = c.office_id or o.id = g.office_id) ");
        sqlBuilder.append(" left join m_office transferToOffice on transferToOffice.id = c.transfer_to_office_id ");
        sqlBuilder.append(" where ( o.hierarchy like ? or transferToOffice.hierarchy like ?)");

        int arrayPos = 2;
        List<Object> extraCriterias = new ArrayList<>();
        extraCriterias.add(hierarchySearchString);
        extraCriterias.add(hierarchySearchString);

        if (searchParameters != null) {

            if (StringUtils.isNotBlank(searchParameters.getStatus())) {
                sqlBuilder.append(" and l.loan_status_id = ?");
                extraCriterias.add(Integer.parseInt(searchParameters.getStatus()));
                arrayPos = arrayPos + 1;
            }

            if (StringUtils.isNotBlank(searchParameters.getExternalId())) {
                sqlBuilder.append(" and l.external_id = ?");
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
        }
        final Object[] objectArray = extraCriterias.toArray();
        final Object[] finalObjectArray = Arrays.copyOf(objectArray, arrayPos);
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), finalObjectArray, loanMapper);
    }

    @Override
    public LoanAccountData retrieveTemplateWithClientAndProductDetails(final Long clientId, final Long productId) {

        this.context.authenticatedUser();

        final ClientData clientAccount = this.clientReadPlatformService.retrieveOne(clientId);
        final LocalDate expectedDisbursementDate = DateUtils.getBusinessLocalDate();
        LoanAccountData loanTemplateDetails = LoanAccountData.clientDefaults(clientAccount.getId(), clientAccount.getAccountNo(),
                clientAccount.getDisplayName(), clientAccount.getOfficeId(), clientAccount.getExternalId(), expectedDisbursementDate);

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
        this.context.authenticatedUser();
        RepaymentTransactionTemplateMapper mapper = new RepaymentTransactionTemplateMapper(sqlGenerator);
        String sql = "select " + mapper.schema();
        LoanTransactionData loanTransactionData = this.jdbcTemplate.queryForObject(sql, mapper, // NOSONAR
                LoanTransactionType.REPAYMENT.getValue(), LoanTransactionType.DOWN_PAYMENT.getValue(),
                LoanTransactionType.REPAYMENT.getValue(), LoanTransactionType.DOWN_PAYMENT.getValue(), loanId, loanId);
        final Collection<PaymentTypeData> paymentOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        final SearchParameters channelSearchParameters = SearchParameters.builder().channelType(ChannelType.REPAYMENT.getValue())
                .active(true).build();
        final List<ChannelData> channelOptions = channelReadWritePlatformService.findBySearchParam(channelSearchParameters);
        final LoanTransactionData loanTransactionTemplate = LoanTransactionData.templateOnTop(loanTransactionData, paymentOptions);
        final Collection<CodeValueData> bankOptions = codeValueReadPlatformService.retrieveCodeValuesByCode("Bancos");
        loanTransactionTemplate.setChannelOptions(channelOptions);
        loanTransactionTemplate.setBankOptions(bankOptions);

        loanTransactionTemplate.setTransactionProcessingStrategyTypes(LoanScheduleProcessingType.getValuesAsEnumOptionDataList());
        loanTransactionTemplate.setTransactionProcessingStrategy(loanTransactionData.getTransactionProcessingStrategy());
        loanTransactionTemplate.setLoanScheduleType(loanTransactionData.getLoanScheduleType());
        loanTransactionTemplate.setLoanProductType(loanTransactionData.getLoanProductType());

        return loanTransactionTemplate;
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
        final Collection<CodeValueData> bankOptions = codeValueReadPlatformService.retrieveCodeValuesByCode("Bancos");

        return new LoanTransactionData(null, null, null, transactionType, null, currencyData, earliestUnpaidInstallmentDate,
                loanRepaymentScheduleInstallment.getTotalOutstanding(currency).getAmount().subtract(adjustedChargeAmount),
                loan.getNetDisbursalAmount(), loanRepaymentScheduleInstallment.getPrincipalOutstanding(currency).getAmount(),
                loanRepaymentScheduleInstallment.getInterestOutstanding(currency).getAmount(),
                loanRepaymentScheduleInstallment.getFeeChargesOutstanding(currency).getAmount().subtract(adjustedChargeAmount),
                loanRepaymentScheduleInstallment.getPenaltyChargesOutstanding(currency).getAmount(), null, unrecognizedIncomePortion,
                paymentOptions, ExternalId.empty(), null, null, outstandingLoanBalance, false, loanId, loan.getExternalId(), bankOptions);
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
                loan.getNetDisbursalAmount(), null, null, null, null, null, ExternalId.empty(), null, null, outstandingLoanBalance,
                unrecognizedIncomePortion, false, loanId, loan.getExternalId());
    }

    @Override
    public LoanTransactionData retrieveNewClosureDetails() {

        this.context.authenticatedUser();
        final BigDecimal outstandingLoanBalance = null;
        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(LoanTransactionType.WRITEOFF);
        final BigDecimal unrecognizedIncomePortion = null;
        return new LoanTransactionData(null, null, null, transactionType, null, null, DateUtils.getBusinessLocalDate(), null, null, null,
                null, null, null, null, ExternalId.empty(), null, null, outstandingLoanBalance, unrecognizedIncomePortion, false, null,
                null);

    }

    @Override
    public LoanApprovalData retrieveApprovalTemplate(final Long loanId) {
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        return new LoanApprovalData(loan.getProposedPrincipal(), DateUtils.getBusinessLocalDate(), loan.getNetDisbursalAmount());
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
            LoanTransactionData loanTransactionData = this.jdbcTemplate.queryForObject(sql, rm, loanId, transactionId); // NOSONAR
            loanTransactionData.setLoanTransactionRelations(this.retrieveLoanTransactionRelationsByLoanTransactionId(transactionId));
            return loanTransactionData;
        } catch (final EmptyResultDataAccessException e) {
            throw new LoanTransactionNotFoundException(transactionId, e);
        }
    }

    @Override
    public Long getLoanIdByLoanExternalId(String externalId) {
        ExternalId loanExternalId = ExternalIdFactory.produce(externalId);
        Long loanId = loanRepositoryWrapper.findIdByExternalId(loanExternalId);
        if (Objects.isNull(loanId)) {
            throw new LoanNotFoundException(loanExternalId);
        }
        return loanId;
    }

    private static final class LoanMapper implements RowMapper<LoanAccountData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;
        private final DelinquencyReadPlatformService delinquencyReadPlatformService;

        LoanMapper(DatabaseSpecificSQLGenerator sqlGenerator, DelinquencyReadPlatformService delinquencyReadPlatformService) {
            this.sqlGenerator = sqlGenerator;
            this.delinquencyReadPlatformService = delinquencyReadPlatformService;
        }

        public String loanSchema() {
            return """
                    l.id AS id,
                        l.account_no AS accountNo,
                        l.interest_rate_points AS interestRatePoints,
                        l.external_id AS externalId,
                        l.fund_id AS fundId,
                        f.name AS fundName,
                        l.loan_type_enum AS loanType,
                        l.loanpurpose_cv_id AS loanPurposeId,
                        cv.code_value AS loanPurposeName,
                        lp.id AS loanProductId,
                        lp.name AS loanProductName,
                        lp.description AS loanProductDescription,
                        lp.is_linked_to_floating_interest_rates AS isLoanProductLinkedToFloatingRate,
                        lp.allow_variabe_installments AS isVariableInstallmentsAllowed,
                        lp.allow_multiple_disbursals AS multiDisburseLoan,
                        lp.disallow_expected_disbursements AS disallowExpectedDisbursements,
                        lp.can_define_fixed_emi_amount AS canDefineInstallmentAmount,
                        c.id AS clientId,
                        c.account_no AS clientAccountNo,
                        c.display_name AS clientName,
                        c.office_id AS clientOfficeId,
                        c.external_id AS clientExternalId,
                        g.id AS groupId,
                        g.account_no AS groupAccountNo,
                        g.display_name AS groupName,
                        g.office_id AS groupOfficeId,
                        g.staff_id AS groupStaffId,
                        g.parent_id AS groupParentId,
                        (SELECT mg.display_name FROM m_group mg WHERE mg.id = g.parent_id) AS centerName,
                        g.hierarchy AS groupHierarchy,
                        g.level_id AS groupLevel,
                        g.external_id AS groupExternalId,
                        g.status_enum AS statusEnum,
                        g.activation_date AS activationDate,
                        l.submittedon_date AS submittedOnDate,
                        sbu.username AS submittedByUsername,
                        sbu.firstname AS submittedByFirstname,
                        sbu.lastname AS submittedByLastname,
                        l.rejectedon_date AS rejectedOnDate,
                        rbu.username AS rejectedByUsername,
                        rbu.firstname AS rejectedByFirstname,
                        rbu.lastname AS rejectedByLastname,
                        l.withdrawnon_date AS withdrawnOnDate,
                        wbu.username AS withdrawnByUsername,
                        wbu.firstname AS withdrawnByFirstname,
                        wbu.lastname AS withdrawnByLastname,
                        l.approvedon_date AS approvedOnDate,
                        abu.username AS approvedByUsername,
                        abu.firstname AS approvedByFirstname,
                        abu.lastname AS approvedByLastname,
                        l.expected_disbursedon_date AS expectedDisbursementDate,
                        l.disbursedon_date AS actualDisbursementDate,
                        dbu.username AS disbursedByUsername,
                        dbu.firstname AS disbursedByFirstname,
                        dbu.lastname AS disbursedByLastname,
                        l.closedon_date AS closedOnDate,
                        cbu.username AS closedByUsername,
                        cbu.firstname AS closedByFirstname,
                        cbu.lastname AS closedByLastname,
                        l.writtenoffon_date AS writtenOffOnDate,
                        l.expected_firstrepaymenton_date AS expectedFirstRepaymentOnDate,
                        l.interest_calculated_from_date AS interestChargedFromDate,
                        l.maturedon_date AS actualMaturityDate,
                        l.expected_maturedon_date AS expectedMaturityDate,
                        l.principal_amount_proposed AS proposedPrincipal,
                        l.principal_amount AS principal,
                        l.approved_principal AS approvedPrincipal,
                        l.net_disbursal_amount AS netDisbursalAmount,
                        l.arrearstolerance_amount AS inArrearsTolerance,
                        l.number_of_repayments AS numberOfRepayments,
                        l.repay_every AS repaymentEvery,
                        l.grace_on_principal_periods AS graceOnPrincipalPayment,
                        l.recurring_moratorium_principal_periods AS recurringMoratoriumOnPrincipalPeriods,
                        l.grace_on_interest_periods AS graceOnInterestPayment,
                        l.grace_interest_free_periods AS graceOnInterestCharged,
                        l.grace_on_arrears_ageing AS graceOnArrearsAgeing,
                        l.nominal_interest_rate_per_period AS interestRatePerPeriod,
                        l.annual_nominal_interest_rate AS annualInterestRate,
                        l.repayment_period_frequency_enum AS repaymentFrequencyType,
                        l.interest_period_frequency_enum AS interestRateFrequencyType,
                        l.term_frequency AS termFrequency,
                        l.term_period_frequency_enum AS termPeriodFrequencyType,
                        l.amortization_method_enum AS amortizationType,
                        l.interest_method_enum AS interestType,
                        l.is_equal_amortization AS isEqualAmortization,
                        l.interest_calculated_in_period_enum AS interestCalculationPeriodType,
                        l.fixed_principal_percentage_per_installment AS fixedPrincipalPercentagePerInstallment,
                        l.allow_partial_period_interest_calcualtion AS allowPartialPeriodInterestCalcualtion,
                        l.loan_status_id AS lifeCycleStatusId,
                        l.loan_transaction_strategy_code AS transactionStrategyCode,
                        l.loan_transaction_strategy_name AS transactionStrategyName,
                        l.enable_installment_level_delinquency AS enableInstallmentLevelDelinquency,
                        l.currency_code AS currencyCode,
                        l.currency_digits AS currencyDigits,
                        l.currency_multiplesof AS inMultiplesOf,
                        rc.name AS currencyName,
                        rc.display_symbol AS currencyDisplaySymbol,
                        rc.internationalized_name_code AS currencyNameCode,
                        l.loan_officer_id AS loanOfficerId,
                        s.display_name AS loanOfficerName,
                        l.loan_assignor_id AS loanAssignorId,
                        l.principal_disbursed_derived AS principalDisbursed,
                        l.principal_repaid_derived AS principalPaid,
                        l.principal_adjustments_derived AS principalAdjustments,
                        l.principal_writtenoff_derived AS principalWrittenOff,
                        l.principal_outstanding_derived AS principalOutstanding,
                        l.interest_charged_derived AS interestCharged,
                        l.interest_repaid_derived AS interestPaid,
                        l.interest_waived_derived AS interestWaived,
                        l.interest_writtenoff_derived AS interestWrittenOff,
                        l.interest_outstanding_derived AS interestOutstanding,
                        l.fee_charges_charged_derived AS feeChargesCharged,
                        l.total_charges_due_at_disbursement_derived AS feeChargesDueAtDisbursementCharged,
                        l.fee_charges_repaid_derived AS feeChargesPaid,
                        l.fee_charges_waived_derived AS feeChargesWaived,
                        l.fee_charges_writtenoff_derived AS feeChargesWrittenOff,
                        l.fee_charges_outstanding_derived AS feeChargesOutstanding,
                        l.penalty_charges_charged_derived AS penaltyChargesCharged,
                        l.penalty_charges_repaid_derived AS penaltyChargesPaid,
                        l.penalty_charges_waived_derived AS penaltyChargesWaived,
                        l.penalty_charges_writtenoff_derived AS penaltyChargesWrittenOff,
                        l.penalty_charges_outstanding_derived AS penaltyChargesOutstanding,
                        l.total_expected_repayment_derived AS totalExpectedRepayment,
                        l.total_repayment_derived AS totalRepayment,
                        l.total_expected_costofloan_derived AS totalExpectedCostOfLoan,
                        l.total_costofloan_derived AS totalCostOfLoan,
                        l.total_waived_derived AS totalWaived,
                        l.total_writtenoff_derived AS totalWrittenOff,
                        l.writeoff_reason_cv_id AS writeoffReasonId,
                        codev.code_value AS writeoffReason,
                        l.total_outstanding_derived AS totalOutstanding,
                        l.total_overpaid_derived AS totalOverpaid,
                        l.fixed_emi_amount AS fixedEmiAmount,
                        l.max_outstanding_loan_balance AS outstandingLoanBalance,
                        l.loan_sub_status_id AS loanSubStatusId,
                        la.principal_overdue_derived AS principalOverdue,
                        l.is_fraud AS isFraud,
                        la.interest_overdue_derived AS interestOverdue,
                        la.fee_charges_overdue_derived AS feeChargesOverdue,
                        la.penalty_charges_overdue_derived AS penaltyChargesOverdue,
                        la.total_overdue_derived AS totalOverdue,
                        la.overdue_since_date_derived AS overdueSinceDate,
                        l.sync_disbursement_with_meeting AS syncDisbursementWithMeeting,
                        l.loan_counter AS loanCounter,
                        l.loan_product_counter AS loanProductCounter,
                        l.is_npa AS isNPA,
                        l.days_in_month_enum AS daysInMonth,
                        l.days_in_year_enum AS daysInYear,
                        l.interest_recalculation_enabled AS isInterestRecalculationEnabled,
                        lir.id AS lirId,
                        lir.loan_id AS loanId,
                        lir.compound_type_enum AS compoundType,
                        lir.reschedule_strategy_enum AS rescheduleStrategy,
                        lir.rest_frequency_type_enum AS restFrequencyEnum,
                        lir.rest_frequency_interval AS restFrequencyInterval,
                        lir.rest_frequency_nth_day_enum AS restFrequencyNthDayEnum,
                        lir.rest_frequency_weekday_enum AS restFrequencyWeekDayEnum,
                        lir.rest_frequency_on_day AS restFrequencyOnDay,
                        lir.compounding_frequency_type_enum AS compoundingFrequencyEnum,
                        lir.compounding_frequency_interval AS compoundingInterval,
                        lir.compounding_frequency_nth_day_enum AS compoundingFrequencyNthDayEnum,
                        lir.compounding_frequency_weekday_enum AS compoundingFrequencyWeekDayEnum,
                        lir.compounding_frequency_on_day AS compoundingFrequencyOnDay,
                        lir.is_compounding_to_be_posted_as_transaction AS isCompoundingToBePostedAsTransaction,
                        lir.allow_compounding_on_eod AS allowCompoundingOnEod,
                        l.is_floating_interest_rate AS isFloatingInterestRate,
                        l.interest_rate_differential AS interestRateDifferential,
                        l.create_standing_instruction_at_disbursement AS createStandingInstructionAtDisbursement,
                        lpvi.minimum_gap AS minimumInstallmentGap,
                        lpvi.maximum_gap AS maximumInstallmentGap,
                        lp.can_use_for_topup AS canUseForTopup,
                        l.is_topup AS isTopup,
                        topup.closure_loan_id AS closureLoanId,
                        l.total_recovered_derived AS totalRecovered,
                        topuploan.account_no AS closureLoanAccountNo,
                        topup.topup_amount AS topupAmount,
                        l.last_closed_business_date AS lastClosedBusinessDate,
                        l.overpaidon_date AS overpaidOnDate,
                        l.is_charged_off AS isChargedOff,
                        l.charge_off_reason_cv_id AS chargeOffReasonId,
                        codec.code_value AS chargeOffReason,
                        l.charged_off_on_date AS chargedOffOnDate,
                        l.enable_down_payment AS enableDownPayment,
                        l.disbursed_amount_percentage_for_down_payment AS disbursedAmountPercentageForDownPayment,
                        l.enable_auto_repayment_for_down_payment AS enableAutoRepaymentForDownPayment,
                        cobu.username AS chargedOffByUsername,
                        cobu.firstname AS chargedOffByFirstname,
                        cobu.lastname AS chargedOffByLastname,
                        l.loan_schedule_type AS loanScheduleType,
                        l.loan_schedule_processing_type AS loanScheduleProcessingType,
                        brs.name_of_reason AS blockStatusName,
                        brs.id AS blockStatusId,
                        brs.priority AS blockStatusPriority,
                        brs.level AS blockStatusLevel,
                        cch.name AS channel_name,
                        cch.id AS channel_id,
                        cch.description AS channel_description,
                        cch.cedula_seguro_voluntario AS cedulaSeguroVoluntario,
                        cch.codigo_seguro AS codigoSeguro,
                        pos.name AS point_of_sales_name,
                        pos.code AS point_of_sales_code,
                        l.valor_descuento,l.valor_giro
                    FROM
                        m_loan l
                        JOIN m_product_loan lp ON lp.id = l.product_id
                        LEFT JOIN m_loan_recalculation_details lir ON lir.loan_id = l.id
                        JOIN m_currency rc ON rc.code = l.currency_code
                        LEFT JOIN m_client c ON c.id = l.client_id
                        LEFT JOIN m_group g ON g.id = l.group_id
                        LEFT JOIN m_loan_arrears_aging la ON la.loan_id = l.id
                        LEFT JOIN m_fund f ON f.id = l.fund_id
                        LEFT JOIN m_staff s ON s.id = l.loan_officer_id
                        LEFT JOIN m_appuser sbu ON sbu.id = l.created_by
                        LEFT JOIN m_appuser rbu ON rbu.id = l.rejectedon_userid
                        LEFT JOIN m_appuser wbu ON wbu.id = l.withdrawnon_userid
                        LEFT JOIN m_appuser abu ON abu.id = l.approvedon_userid
                        LEFT JOIN m_appuser dbu ON dbu.id = l.disbursedon_userid
                        LEFT JOIN m_appuser cbu ON cbu.id = l.closedon_userid
                        LEFT JOIN m_appuser cobu ON cobu.id = l.charged_off_by_userid
                        LEFT JOIN m_code_value cv ON cv.id = l.loanpurpose_cv_id
                        LEFT JOIN m_code_value codev ON codev.id = l.writeoff_reason_cv_id
                        LEFT JOIN m_code_value codec ON codec.id = l.charge_off_reason_cv_id
                        LEFT JOIN m_product_loan_variable_installment_config lpvi ON lpvi.loan_product_id = l.product_id
                        LEFT JOIN m_loan_topup topup ON l.id = topup.loan_id
                        LEFT JOIN m_loan topuploan ON topuploan.id = topup.closure_loan_id
                        LEFT JOIN m_blocking_reason_setting brs ON brs.id = l.block_status_id
                        LEFT JOIN (WITH cte AS (SELECT c.*,
                                                      lpc.loan_id,
                                                      lpc.cedula_seguro_voluntario,
                                                      lpc.codigo_seguro,
                                                      ROW_NUMBER() OVER (PARTITION BY lpc.loan_id ORDER BY c.id) AS rn
                                               FROM  custom.c_client_buy_process lpc
                                                        JOIN custom.c_channel c ON lpc.channel_id = c.id)
                                  SELECT *
                                  FROM cte
                                  WHERE rn = 1)cch ON cch.loan_id = l.id
                        LEFT JOIN (
                            SELECT
                                ps.name,
                                ps.code,
                                cbp.loan_id
                            FROM
                                custom.c_client_ally_point_of_sales ps
                            JOIN custom.c_client_buy_process cbp ON cbp.point_if_sales_id = ps.id
                        ) pos ON pos.loan_id = l.id
                    """;
        }

        @Override
        public LoanAccountData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final CurrencyData currencyData = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf,
                    currencyDisplaySymbol, currencyNameCode);

            final Long id = rs.getLong("id");
            final String accountNo = rs.getString("accountNo");
            final String externalIdStr = rs.getString("externalId");
            final ExternalId externalId = ExternalIdFactory.produce(externalIdStr);

            final Long clientId = JdbcSupport.getLong(rs, "clientId");
            final String clientAccountNo = rs.getString("clientAccountNo");
            final Long clientOfficeId = JdbcSupport.getLong(rs, "clientOfficeId");
            final ExternalId clientExternalId = ExternalIdFactory.produce(rs.getString("clientExternalId"));
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
            final Long loanAssignorId = JdbcSupport.getLong(rs, "loanAssignorId");
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
            final LocalDate actualMaturityDate = JdbcSupport.getLocalDate(rs, "actualMaturityDate");
            final LocalDate expectedMaturityDate = JdbcSupport.getLocalDate(rs, "expectedMaturityDate");

            final Boolean isvariableInstallmentsAllowed = rs.getBoolean("isvariableInstallmentsAllowed");
            final Integer minimumGap = rs.getInt("minimuminstallmentgap");
            final Integer maximumGap = rs.getInt("maximuminstallmentgap");

            final LocalDate chargedOffOnDate = JdbcSupport.getLocalDate(rs, "chargedOffOnDate");
            final String chargedOffByUsername = rs.getString("chargedOffByUsername");
            final String chargedOffByFirstname = rs.getString("chargedOffByFirstname");
            final String chargedOffByLastname = rs.getString("chargedOffByLastname");
            final Long chargeOffReasonId = JdbcSupport.getLong(rs, "chargeOffReasonId");
            final String chargeOffReason = rs.getString("chargeOffReason");
            final boolean isChargedOff = rs.getBoolean("isChargedOff");

            final LoanApplicationTimelineData timeline = new LoanApplicationTimelineData(submittedOnDate, submittedByUsername,
                    submittedByFirstname, submittedByLastname, rejectedOnDate, rejectedByUsername, rejectedByFirstname, rejectedByLastname,
                    withdrawnOnDate, withdrawnByUsername, withdrawnByFirstname, withdrawnByLastname, approvedOnDate, approvedByUsername,
                    approvedByFirstname, approvedByLastname, expectedDisbursementDate, actualDisbursementDate, disbursedByUsername,
                    disbursedByFirstname, disbursedByLastname, closedOnDate, closedByUsername, closedByFirstname, closedByLastname,
                    actualMaturityDate, expectedMaturityDate, writtenOffOnDate, closedByUsername, closedByFirstname, closedByLastname,
                    chargedOffOnDate, chargedOffByUsername, chargedOffByFirstname, chargedOffByLastname);

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

            final String transactionStrategyCode = rs.getString("transactionStrategyCode");
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
            final LocalDate expectedFirstRepaymentOnDate = JdbcSupport.getLocalDate(rs, "expectedFirstRepaymentOnDate");
            final LocalDate interestChargedFromDate = JdbcSupport.getLocalDate(rs, "interestChargedFromDate");

            final Boolean syncDisbursementWithMeeting = rs.getBoolean("syncDisbursementWithMeeting");

            final BigDecimal feeChargesDueAtDisbursementCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs,
                    "feeChargesDueAtDisbursementCharged");
            LoanSummaryData loanSummary = null;
            Boolean inArrears = false;
            if (status.getId().intValue() >= 300) {

                // loan summary
                final BigDecimal principalDisbursed = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalDisbursed");
                final BigDecimal principalAdjustments = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalAdjustments");
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
                inArrears = (overdueSinceDate != null);

                loanSummary = new LoanSummaryData(currencyData, principalDisbursed, principalAdjustments, principalPaid,
                        principalWrittenOff, principalOutstanding, principalOverdue, interestCharged, interestPaid, interestWaived,
                        interestWrittenOff, interestOutstanding, interestOverdue, feeChargesCharged, feeChargesDueAtDisbursementCharged,
                        feeChargesPaid, feeChargesWaived, feeChargesWrittenOff, feeChargesOutstanding, feeChargesOverdue,
                        penaltyChargesCharged, penaltyChargesPaid, penaltyChargesWaived, penaltyChargesWrittenOff,
                        penaltyChargesOutstanding, penaltyChargesOverdue, totalExpectedRepayment, totalRepayment, totalExpectedCostOfLoan,
                        totalCostOfLoan, totalWaived, totalWrittenOff, totalOutstanding, totalOverdue, overdueSinceDate, writeoffReasonId,
                        writeoffReason, totalRecovered, chargeOffReasonId, chargeOffReason);
            }

            GroupGeneralData groupData = null;
            if (groupId != null) {
                final Integer groupStatusEnum = JdbcSupport.getInteger(rs, "statusEnum");
                final EnumOptionData groupStatus = ClientEnumerations.status(groupStatusEnum);
                final LocalDate activationDate = JdbcSupport.getLocalDate(rs, "activationDate");
                groupData = GroupGeneralData.instance(groupId, groupAccountNo, groupName, groupExternalId, groupStatus, activationDate,
                        groupOfficeId, null, groupParentId, centerName, groupStaffId, null, groupHierarchy, groupLevel, null);
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
            final boolean disallowExpectedDisbursements = rs.getBoolean("disallowExpectedDisbursements");
            // Current Delinquency Range Data
            DelinquencyRangeData delinquencyRange = this.delinquencyReadPlatformService.retrieveCurrentDelinquencyTag(id);

            final boolean isFraud = rs.getBoolean("isFraud");
            final LocalDate lastClosedBusinessDate = JdbcSupport.getLocalDate(rs, "lastClosedBusinessDate");
            final LocalDate overpaidOnDate = JdbcSupport.getLocalDate(rs, "overpaidOnDate");

            final boolean enableDownPayment = rs.getBoolean("enableDownPayment");
            final BigDecimal disbursedAmountPercentageForDownPayment = rs.getBigDecimal("disbursedAmountPercentageForDownPayment");
            final boolean enableAutoRepaymentForDownPayment = rs.getBoolean("enableAutoRepaymentForDownPayment");
            final boolean enableInstallmentLevelDelinquency = rs.getBoolean("enableInstallmentLevelDelinquency");
            final String loanScheduleTypeStr = rs.getString("loanScheduleType");
            final LoanScheduleType loanScheduleType = LoanScheduleType.valueOf(loanScheduleTypeStr);
            final String loanScheduleProcessingTypeStr = rs.getString("loanScheduleProcessingType");
            final LoanScheduleProcessingType loanScheduleProcessingType = LoanScheduleProcessingType.valueOf(loanScheduleProcessingTypeStr);
            final String blockStatusName = rs.getString("blockStatusName");
            BlockingReasonsData blockStatusData = null;
            final Long channelId = rs.getLong("channel_id");
            final String channelName = Objects.toString(rs.getString("channel_name"), "Mifos");
            final String channelDescription = rs.getString("channel_description");
            final String pointOfSalesName = rs.getString("point_of_sales_name");
            final String pointOfSalesCode = rs.getString("point_of_sales_code");

            if (!StringUtils.isEmpty(blockStatusName)) {
                blockStatusData = BlockingReasonsData.builder().id(rs.getLong("blockStatusId")).nameOfReason(blockStatusName)
                        .level(rs.getString("blockStatusLevel")).priority(JdbcSupport.getInteger(rs, "blockStatusPriority")).build();
            }
            BigDecimal valorDescuento = rs.getBigDecimal("valor_descuento");
            BigDecimal valorGiro = rs.getBigDecimal("valor_giro");

            final LoanAccountData basicLoanDetails = LoanAccountData.basicLoanDetails(id, accountNo, status, externalId, clientId,
                    clientAccountNo, clientName, clientOfficeId, clientExternalId, groupData, loanType, loanProductId, loanProductName,
                    loanProductDescription, isLoanProductLinkedToFloatingRate, fundId, fundName, loanPurposeId, loanPurposeName,
                    loanOfficerId, loanOfficerName, currencyData, proposedPrincipal, principal, approvedPrincipal, netDisbursalAmount,
                    totalOverpaid, inArrearsTolerance, termFrequency, termPeriodFrequencyType, numberOfRepayments, repaymentEvery,
                    repaymentFrequencyType, null, null, transactionStrategyCode, transactionStrategyName, amortizationType,
                    interestRatePerPeriod, interestRateFrequencyType, annualInterestRate, interestType, isFloatingInterestRate,
                    interestRateDifferential, interestCalculationPeriodType, allowPartialPeriodInterestCalcualtion,
                    expectedFirstRepaymentOnDate, graceOnPrincipalPayment, recurringMoratoriumOnPrincipalPeriods, graceOnInterestPayment,
                    graceOnInterestCharged, interestChargedFromDate, timeline, loanSummary, feeChargesDueAtDisbursementCharged,
                    syncDisbursementWithMeeting, loanCounter, loanProductCounter, multiDisburseLoan, canDefineInstallmentAmount,
                    fixedEmiAmount, outstandingLoanBalance, inArrears, graceOnArrearsAgeing, isNPA, daysInMonthType, daysInYearType,
                    isInterestRecalculationEnabled, interestRecalculationData, createStandingInstructionAtDisbursement,
                    isvariableInstallmentsAllowed, minimumGap, maximumGap, loanSubStatus, canUseForTopup, isTopup, closureLoanId,
                    closureLoanAccountNo, topupAmount, isEqualAmortization, fixedPrincipalPercentagePerInstallment, delinquencyRange,
                    disallowExpectedDisbursements, isFraud, lastClosedBusinessDate, overpaidOnDate, isChargedOff, enableDownPayment,
                    disbursedAmountPercentageForDownPayment, enableAutoRepaymentForDownPayment, enableInstallmentLevelDelinquency,
                    loanScheduleType.asEnumOptionData(), loanScheduleProcessingType.asEnumOptionData(), valorDescuento, valorGiro);
            basicLoanDetails.setLoanAssignorId(loanAssignorId);
            basicLoanDetails.setBlockStatus(blockStatusData);
            basicLoanDetails.setChannelDescription(channelDescription);
            basicLoanDetails.setChannelId(channelId);
            basicLoanDetails.setChannelName(channelName);
            basicLoanDetails.setPointOfSalesName(pointOfSalesName);
            basicLoanDetails.setPointOfSalesCode(pointOfSalesCode);
            basicLoanDetails.setValorDescuento(valorDescuento);
            basicLoanDetails.setValorGiro(valorGiro);
            final Long interestRatePoints = JdbcSupport.getLong(rs, "interestRatePoints");
            basicLoanDetails.setInterestRatePoints(interestRatePoints);

            final Long codigoSeguro = JdbcSupport.getLong(rs, "codigoSeguro");
            final Long cedulaSeguroVoluntario = JdbcSupport.getLong(rs, "cedulaSeguroVoluntario");
            basicLoanDetails.setCodigoSeguro(codigoSeguro);
            basicLoanDetails.setCedulaSeguroVoluntario(cedulaSeguroVoluntario);
            return basicLoanDetails;

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
                    + " mc.amount,mc.id as chargeId  from m_loan_repayment_schedule ls " + " inner join m_loan ml on ml.id = ls.loan_id "
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
            final String locale = Locale.ENGLISH.toLanguageTag();

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
        private final LoanScheduleType loanScheduleType;
        private LocalDate lastDueDate;
        private BigDecimal outstandingLoanPrincipalBalance;
        private boolean excludePastUnDisbursed;

        LoanScheduleResultSetExtractor(final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedLoanData,
                Collection<DisbursementData> disbursementData, boolean isInterestRecalculationEnabled, LoanScheduleType loanScheduleType) {
            this.currency = repaymentScheduleRelatedLoanData.getCurrency();
            this.disbursement = repaymentScheduleRelatedLoanData.disbursementData();
            this.totalFeeChargesDueAtDisbursement = repaymentScheduleRelatedLoanData.getTotalFeeChargesAtDisbursement();
            this.lastDueDate = this.disbursement.disbursementDate();
            this.outstandingLoanPrincipalBalance = this.disbursement.getPrincipal();
            this.disbursementData = disbursementData;
            this.excludePastUnDisbursed = isInterestRecalculationEnabled;
            this.loanScheduleType = loanScheduleType;
        }

        public String schema() {

            return " ls.loan_id as loanId, ls.installment as period, ls.fromdate as fromDate, ls.duedate as dueDate, ls.obligations_met_on_date as obligationsMetOnDate, ls.completed_derived as complete,"
                    + " ls.principal_amount as principalDue, ls.principal_completed_derived as principalPaid, ls.principal_writtenoff_derived as principalWrittenOff, ls.is_additional as isAdditional, "
                    + " ls.interest_amount as interestDue, ls.interest_completed_derived as interestPaid, ls.interest_waived_derived as interestWaived, ls.interest_writtenoff_derived as interestWrittenOff, "
                    + " ls.fee_charges_amount as feeChargesDue, ls.fee_charges_completed_derived as feeChargesPaid, ls.fee_charges_waived_derived as feeChargesWaived, ls.fee_charges_writtenoff_derived as feeChargesWrittenOff, "
                    + " ls.penalty_charges_amount as penaltyChargesDue, ls.penalty_charges_completed_derived as penaltyChargesPaid, ls.penalty_charges_waived_derived as penaltyChargesWaived, "
                    + " ls.penalty_charges_writtenoff_derived as penaltyChargesWrittenOff, ls.total_paid_in_advance_derived as totalPaidInAdvanceForPeriod, "
                    + " ls.total_paid_late_derived as totalPaidLateForPeriod, ls.credits_amount as totalCredits, ls.is_down_payment isDownPayment, ls.advance_principal_amount advancePrincipalAmount "
                    + " from m_loan_repayment_schedule ls ";
        }

        @Override
        public LoanScheduleData extractData(@NotNull final ResultSet rs) throws SQLException, DataAccessException {
            BigDecimal waivedChargeAmount = BigDecimal.ZERO;
            for (DisbursementData disbursementDetail : disbursementData) {
                waivedChargeAmount = waivedChargeAmount.add(disbursementDetail.getWaivedChargeAmount());
            }
            final LoanSchedulePeriodData disbursementPeriod = LoanSchedulePeriodData.disbursementOnlyPeriod(
                    this.disbursement.disbursementDate(), this.disbursement.getPrincipal(), this.totalFeeChargesDueAtDisbursement,
                    this.disbursement.isDisbursed());

            final List<LoanSchedulePeriodData> periods = new ArrayList<>();
            final MonetaryCurrency monCurrency = new MonetaryCurrency(this.currency.getCode(), this.currency.getDecimalPlaces(),
                    this.currency.getInMultiplesOf());
            BigDecimal totalPrincipalDisbursed = BigDecimal.ZERO;
            BigDecimal disbursementChargeAmount = this.totalFeeChargesDueAtDisbursement;
            if (disbursementData.isEmpty()) {
                periods.add(disbursementPeriod);
                totalPrincipalDisbursed = Money.of(monCurrency, this.disbursement.getPrincipal()).getAmount();
            } else {
                if (!this.disbursement.isDisbursed()) {
                    excludePastUnDisbursed = false;
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
            Money totalCredits = Money.zero(monCurrency);

            // update totals with details of fees charged during disbursement
            totalFeeChargesCharged = totalFeeChargesCharged.plus(disbursementPeriod.getFeeChargesDue().subtract(waivedChargeAmount));
            totalRepaymentExpected = totalRepaymentExpected.plus(disbursementPeriod.getFeeChargesDue()).minus(waivedChargeAmount);
            totalRepayment = totalRepayment.plus(disbursementPeriod.getFeeChargesPaid()).minus(waivedChargeAmount);
            totalOutstanding = totalOutstanding.plus(disbursementPeriod.getFeeChargesDue()).minus(disbursementPeriod.getFeeChargesPaid());

            Integer loanTermInDays = 0;
            Set<Long> disbursementPeriodIds = new HashSet<>();
            while (rs.next()) {

                final Long loanId = rs.getLong("loanId");
                final Integer period = JdbcSupport.getInteger(rs, "period");
                LocalDate fromDate = JdbcSupport.getLocalDate(rs, "fromDate");
                final LocalDate dueDate = JdbcSupport.getLocalDate(rs, "dueDate");
                final LocalDate obligationsMetOnDate = JdbcSupport.getLocalDate(rs, "obligationsMetOnDate");
                final boolean complete = rs.getBoolean("complete");
                final boolean isAdditional = rs.getBoolean("isAdditional");
                BigDecimal disbursedAmount = BigDecimal.ZERO;
                if (!isAdditional) {
                    disbursedAmount = processDisbursementData(loanScheduleType, disbursementData, fromDate, dueDate, disbursementPeriodIds,
                            disbursementChargeAmount, waivedChargeAmount, periods);

                }
                // Add the Charge back or Credits to the initial amount to avoid negative balance
                final BigDecimal credits = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalCredits");
                if (!isAdditional) {
                    this.outstandingLoanPrincipalBalance = this.outstandingLoanPrincipalBalance.add(credits);
                }

                totalPrincipalDisbursed = totalPrincipalDisbursed.add(disbursedAmount);

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

                BigDecimal outstandingPrincipalBalanceOfLoan = this.outstandingLoanPrincipalBalance.subtract(principalDue);
                if (isAdditional) {
                    outstandingPrincipalBalanceOfLoan = this.outstandingLoanPrincipalBalance.add(principalDue);
                }

                // update based on current period values
                this.lastDueDate = dueDate;
                this.outstandingLoanPrincipalBalance = this.outstandingLoanPrincipalBalance.subtract(principalDue);
                if (isAdditional) {
                    this.outstandingLoanPrincipalBalance = this.outstandingLoanPrincipalBalance.add(principalDue);
                }
                BigDecimal advancePrincipalAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs,"advancePrincipalAmount");
                this.outstandingLoanPrincipalBalance = this.outstandingLoanPrincipalBalance.subtract(advancePrincipalAmount);
                outstandingPrincipalBalanceOfLoan = outstandingPrincipalBalanceOfLoan.subtract(advancePrincipalAmount);
                final boolean isDownPayment = rs.getBoolean("isDownPayment");

                LoanSchedulePeriodData periodData;

                periodData = LoanSchedulePeriodData.periodWithPayments(loanId, period, fromDate, dueDate, obligationsMetOnDate, complete,
                        principalDue, principalPaid, principalWrittenOff, principalOutstanding, outstandingPrincipalBalanceOfLoan,
                        interestExpectedDue, interestPaid, interestWaived, interestWrittenOff, interestOutstanding, feeChargesExpectedDue,
                        feeChargesPaid, feeChargesWaived, feeChargesWrittenOff, feeChargesOutstanding, penaltyChargesExpectedDue,
                        penaltyChargesPaid, penaltyChargesWaived, penaltyChargesWrittenOff, penaltyChargesOutstanding, totalDueForPeriod,
                        totalPaidForPeriod, totalPaidInAdvanceForPeriod, totalPaidLateForPeriod, totalWaivedForPeriod,
                        totalWrittenOffForPeriod, totalOutstandingForPeriod, totalActualCostOfLoanForPeriod, totalInstallmentAmount,
                        credits, isDownPayment);

                periods.add(periodData);
            }

            return new LoanScheduleData(this.currency, periods, loanTermInDays, totalPrincipalDisbursed, totalPrincipalExpected.getAmount(),
                    totalPrincipalPaid.getAmount(), totalInterestCharged.getAmount(), totalFeeChargesCharged.getAmount(),
                    totalPenaltyChargesCharged.getAmount(), totalWaived.getAmount(), totalWrittenOff.getAmount(),
                    totalRepaymentExpected.getAmount(), totalRepayment.getAmount(), totalPaidInAdvance.getAmount(),
                    totalPaidLate.getAmount(), totalOutstanding.getAmount(), totalCredits.getAmount());
        }

        private BigDecimal processDisbursementData(LoanScheduleType loanScheduleType, Collection<DisbursementData> disbursementData,
                LocalDate fromDate, LocalDate dueDate, Set<Long> disbursementPeriodIds, BigDecimal disbursementChargeAmount,
                BigDecimal waivedChargeAmount, List<LoanSchedulePeriodData> periods) {
            BigDecimal disbursedAmount = BigDecimal.ZERO;
            for (final DisbursementData data : disbursementData) {
                boolean isDueForDisbursement = data.isDueForDisbursement(loanScheduleType, fromDate, dueDate);
                if (((fromDate.equals(this.disbursement.disbursementDate()) && data.disbursementDate().equals(fromDate))
                        || (fromDate.equals(dueDate) && data.disbursementDate().equals(fromDate))
                        || canAddDisbursementData(data, isDueForDisbursement, excludePastUnDisbursed))
                        && !disbursementPeriodIds.contains(data.getId())) {
                    disbursedAmount = disbursedAmount.add(data.getPrincipal());
                    LoanSchedulePeriodData periodData = createLoanSchedulePeriodData(data, disbursementChargeAmount, waivedChargeAmount);
                    periods.add(periodData);
                    this.outstandingLoanPrincipalBalance = this.outstandingLoanPrincipalBalance.add(periodData.getPrincipalDisbursed());
                    disbursementPeriodIds.add(data.getId());
                }
            }
            return disbursedAmount;
        }

        private LoanSchedulePeriodData createLoanSchedulePeriodData(final DisbursementData data, BigDecimal disbursementChargeAmount,
                BigDecimal waivedChargeAmount) {
            BigDecimal chargeAmount = data.getChargeAmount() == null ? disbursementChargeAmount
                    : disbursementChargeAmount.add(data.getChargeAmount()).subtract(waivedChargeAmount);
            return LoanSchedulePeriodData.disbursementOnlyPeriod(data.disbursementDate(), data.getPrincipal(), chargeAmount,
                    data.isDisbursed());
        }

        private boolean canAddDisbursementData(DisbursementData data, boolean isDueForDisbursement, boolean excludePastUnDisbursed) {
            return (!excludePastUnDisbursed || data.isDisbursed() || !DateUtils.isBeforeBusinessDate(data.disbursementDate()))
                    && isDueForDisbursement;
        }

    }

    private static final class LoanTransactionsMapper implements RowMapper<LoanTransactionData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;

        LoanTransactionsMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String loanPaymentsSchema() {

            return " tr.id as id, tr.transaction_type_enum as transactionType, tr.transaction_date as " + sqlGenerator.escape("date")
                    + ", tr.amount as total, tr.principal_portion_derived as principal, tr.interest_portion_derived as interest, "
                    + " tr.fee_charges_portion_derived as fees, tr.penalty_charges_portion_derived as penalties,  "
                    + " tr.overpayment_portion_derived as overpayment, tr.outstanding_loan_balance_derived as outstandingLoanBalance, "
                    + " tr.unrecognized_income_portion as unrecognizedIncome, tr.submitted_on_date as submittedOnDate, "
                    + " tr.manually_adjusted_or_reversed as manuallyReversed, tr.reversal_external_id as reversalExternalId, tr.reversed_on_date as reversedOnDate, "
                    + " pd.payment_type_id as paymentType,pd.account_number as accountNumber,pd.check_number as checkNumber, "
                    + " pd.receipt_number as receiptNumber, pd.bank_number as bankNumber,pd.routing_code as routingCode, l.net_disbursal_amount as netDisbursalAmount,"
                    + " l.currency_code as currencyCode, l.currency_digits as currencyDigits, l.currency_multiplesof as inMultiplesOf, rc."
                    + sqlGenerator.escape("name") + " as currencyName, l.id as loanId, l.external_id as externalLoanId, "
                    + " rc.display_symbol as currencyDisplaySymbol, rc.internationalized_name_code as currencyNameCode, "
                    + " pt.value as paymentTypeName, tr.external_id as externalId, tr.office_id as officeId, office.name as officeName, "
                    + " fromtran.id as fromTransferId, fromtran.is_reversed as fromTransferReversed,"
                    + " fromtran.transaction_date as fromTransferDate, fromtran.amount as fromTransferAmount,"
                    + " fromtran.description as fromTransferDescription, "
                    + " totran.id as toTransferId, totran.is_reversed as toTransferReversed, "
                    + " totran.transaction_date as toTransferDate, totran.amount as toTransferAmount, ch.name as channelName, pd.channel_hash as channelHash, bank.code_value AS bankName, bank.id AS bankId, "
                    + " totran.description as toTransferDescription,capos.id as pointOfSalesId,capos.name as pointOfSalesName, capos.code as pointOfSalesCode, capos.client_ally_id as clientAllyId from m_loan l join m_loan_transaction tr on tr.loan_id = l.id "
                    + " join m_currency rc on rc." + sqlGenerator.escape("code") + " = l.currency_code "
                    + " left JOIN m_payment_detail pd ON tr.payment_detail_id = pd.id"
                    + " left join m_payment_type pt on pd.payment_type_id = pt.id left join m_office office on office.id=tr.office_id"
                    + " left join m_account_transfer_transaction fromtran on fromtran.from_loan_transaction_id = tr.id "
                    + " left join m_account_transfer_transaction totran on totran.to_loan_transaction_id = tr.id "
                    + " left join custom.c_channel ch on ch.id = pd.channel_id"
                    + " left join custom.c_client_ally_point_of_sales capos on capos.code = pd.point_of_sales_code "
                    + " left join m_code_value bank on bank.id = pd.payment_bank_cv_id ";
        }

        @Override
        public LoanTransactionData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final CurrencyData currencyData = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf,
                    currencyDisplaySymbol, currencyNameCode);

            final Long id = rs.getLong("id");
            final Long loanId = rs.getLong("loanId");
            final String externalLoanIdStr = rs.getString("externalLoanId");
            final ExternalId externalLoanId = ExternalIdFactory.produce(externalLoanIdStr);
            final Long officeId = rs.getLong("officeId");
            final String officeName = rs.getString("officeName");
            final int transactionTypeInt = JdbcSupport.getInteger(rs, "transactionType");
            final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(transactionTypeInt);
            final boolean manuallyReversed = rs.getBoolean("manuallyReversed");

            PaymentDetailData paymentDetailData = null;

            final Long paymentTypeId = JdbcSupport.getLong(rs, "paymentType");
            if (paymentTypeId != null) {
                final String typeName = rs.getString("paymentTypeName");
                final PaymentTypeData paymentType = PaymentTypeData.instance(paymentTypeId, typeName);
                final String accountNumber = rs.getString("accountNumber");
                final String checkNumber = rs.getString("checkNumber");
                final String routingCode = rs.getString("routingCode");
                final String receiptNumber = rs.getString("receiptNumber");
                final String bankNumber = rs.getString("bankNumber");
                final String channelName = rs.getString("channelName");
                final String channelHash = rs.getString("channelHash");
                final Long pointOfSalesId = rs.getLong("pointOfSalesId");
                final String pointOfSalesName = rs.getString("pointOfSalesName");
                final String pointOfSalesCode = rs.getString("pointOfSalesCode");
                final Long clientAllyId = rs.getLong("clientAllyId");
                final PointOfSalesData pointOfSalesData = PointOfSalesData.instance(pointOfSalesId, pointOfSalesName, pointOfSalesCode,
                        clientAllyId);
                paymentDetailData = new PaymentDetailData(id, paymentType, accountNumber, checkNumber, routingCode, receiptNumber,
                        bankNumber, channelName, channelHash, pointOfSalesData);
                final String bankName = rs.getString("BankName");
                final Long bankId = rs.getLong("BankId");
                paymentDetailData.setBankId(bankId);
                paymentDetailData.setBankName(bankName);
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
            final String externalIdStr = rs.getString("externalId");
            final ExternalId externalId = ExternalIdFactory.produce(externalIdStr);
            final String reversalExternalIdStr = rs.getString("reversalExternalId");
            final ExternalId reversalExternalId = ExternalIdFactory.produce(reversalExternalIdStr);
            final LocalDate reversedOnDate = JdbcSupport.getLocalDate(rs, "reversedOnDate");

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
                    unrecognizedIncomePortion, externalId, transfer, null, outstandingLoanBalance, submittedOnDate, manuallyReversed,
                    reversalExternalId, reversedOnDate, loanId, externalLoanId);
        }
    }

    @Override
    public LoanAccountData retrieveLoanProductDetailsTemplate(final Long productId, final Long clientId, final Long groupId) {

        this.context.authenticatedUser();

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
                .retrieveTransactionProcessingStrategies();
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
        if (loanProduct.isUseBorrowerCycle()) {
            if (clientId == null) {
                loanCycleCounter = retriveLoanCounter(groupId, AccountType.GROUP.getValue(), loanProduct.getId());
            } else {
                loanCycleCounter = retriveLoanCounter(clientId, loanProduct.getId());
            }
        }

        Collection<LoanAccountSummaryData> activeLoanOptions = null;
        if (loanProduct.isCanUseForTopup() && clientId != null) {
            Optional<GlobalConfigurationProperty> maxReestructurar = this.globalConfigurationRepository
                    .findByName(LoanApiConstants.GLOBAL_CONFIG_MAX_RESTRUCTURE);
            Optional<GlobalConfigurationProperty> Rediferir = this.globalConfigurationRepository
                    .findByName(LoanApiConstants.GLOBAL_CONFIG_MAX_ARREARS_REDEFER);
            String maxReestructura = "0";
            String maxRediferir = "0";
            if (maxReestructurar.isPresent()) {
                maxReestructura = Long.toString(maxReestructurar.get().getValue());
            }
            if (Rediferir.isPresent()) {
                maxRediferir = Long.toString(Rediferir.get().getValue());
            }
            activeLoanOptions = this.accountDetailsReadPlatformService.retrieveClientActiveLoanAccountSummaryByConfig(clientId,
                    maxReestructura, maxRediferir);
        } else if (loanProduct.isCanUseForTopup() && groupId != null) {
            activeLoanOptions = this.accountDetailsReadPlatformService.retrieveGroupActiveLoanAccountSummary(groupId);
        }

        return LoanAccountData.loanProductWithTemplateDefaults(loanProduct, loanTermFrequencyTypeOptions, repaymentFrequencyTypeOptions,
                repaymentFrequencyNthDayTypeOptions, repaymentFrequencyDaysOfWeekTypeOptions, repaymentStrategyOptions,
                interestRateFrequencyTypeOptions, amortizationTypeOptions, interestTypeOptions, interestCalculationPeriodTypeOptions,
                fundOptions, chargeOptions, loanPurposeOptions, loanCollateralOptions, loanCycleCounter, activeLoanOptions,
                LoanScheduleType.getValuesAsEnumOptionDataList(), LoanScheduleProcessingType.getValuesAsEnumOptionDataList());
    }

    @Override
    public LoanAccountData retrieveClientDetailsTemplate(final Long clientId) {
        this.context.authenticatedUser();
        final ClientData clientAccount = this.clientReadPlatformService.retrieveOne(clientId);
        final LocalDate expectedDisbursementDate = DateUtils.getBusinessLocalDate();
        final LoanAccountData loanAccountData = LoanAccountData.clientDefaults(clientAccount.getId(), clientAccount.getAccountNo(),
                clientAccount.getDisplayName(), clientAccount.getOfficeId(), clientAccount.getExternalId(), expectedDisbursementDate);
        loanAccountData.setClientIdNumber(clientAccount.getIdNumber());
        return loanAccountData;

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

    @Override
    public Collection<OverdueLoanScheduleData> retrieveAllOverdueInstallmentsForLoan(final Loan loan) {
        Collection<OverdueLoanScheduleData> list = new ArrayList<>();

        if (!loan.isOpen()) {
            return list;
        }
        final Long penaltyWaitPeriod = configurationDomainService.retrievePenaltyWaitPeriod();
        final boolean backdatePenalties = configurationDomainService.isBackdatePenaltiesEnabled();

        for (LoanRepaymentScheduleInstallment installment : loan.getRepaymentScheduleInstallments()) {
            if (installment.isObligationsMet() || installment.isRecalculatedInterestComponent()) {
                continue;
            }

            boolean isPenaltyDue = installment.isOverdueOn(DateUtils.getBusinessLocalDate().minusDays(penaltyWaitPeriod).plusDays(1));
            boolean isDueToday = installment.getDueDate().equals(DateUtils.getBusinessLocalDate().minusDays(penaltyWaitPeriod));

            if (isPenaltyDue) {
                if (!backdatePenalties && !isDueToday) {
                    continue;
                }
                Optional<Charge> penaltyCharge = loan.getLoanProduct().getLoanProductCharges().stream()
                        .filter((e) -> ChargeTimeType.OVERDUE_INSTALLMENT.getValue().equals(e.getChargeTimeType()) && e.isLoanCharge())
                        .findFirst();

                if (penaltyCharge.isEmpty()) {
                    continue;
                }

                list.add(new OverdueLoanScheduleData(loan.getId(), penaltyCharge.get().getId(),
                        DateUtils.DEFAULT_DATE_FORMATTER.format(installment.getDueDate()), penaltyCharge.get().getAmount(),
                        DateUtils.DEFAULT_DATE_FORMAT, Locale.ENGLISH.toLanguageTag(),
                        installment.getPrincipalOutstanding(loan.getCurrency()).getAmount(),
                        installment.getInterestOutstanding(loan.getCurrency()).getAmount(), installment.getInstallmentNumber()));
            }
        }
        return list;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Integer retriveLoanCounter(final Long groupId, final Integer loanType, Long productId) {
        final String sql = "Select MAX(l.loan_product_counter) from m_loan l where l.group_id = ?  and l.loan_type_enum = ? and l.product_id=?";
        return this.jdbcTemplate.queryForObject(sql, new Object[] { groupId, loanType, productId }, Integer.class);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Integer retriveLoanCounter(final Long clientId, Long productId) {
        final String sql = "Select MAX(l.loan_product_counter) from m_loan l where l.client_id = ? and l.product_id=?";
        return this.jdbcTemplate.queryForObject(sql, new Object[] { clientId, productId }, Integer.class);
    }

    @Override
    public Collection<DisbursementData> retrieveLoanDisbursementDetails(final Long loanId) {
        final LoanDisbursementDetailMapper rm = new LoanDisbursementDetailMapper(sqlGenerator);
        final String sql = "select " + rm.schema()
                + " where dd.loan_id=? and dd.is_reversed=false group by dd.id, lc.amount_waived_derived order by dd.expected_disburse_date,dd.disbursedon_date";
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

            return new LoanTermVariationsData(id, LoanEnumerations.loanVariationType(LoanTermVariationType.EMI_AMOUNT),
                    variationApplicableFrom, decimalValue, dateValue, isSpecificToInstallment);
        }
    }

    @Override
    public Collection<LoanScheduleAccrualData> retriveScheduleAccrualData() {
        final String chargeAccrualDateCriteria = configurationDomainService.getAccrualDateConfigForCharge();
        if (chargeAccrualDateCriteria.equalsIgnoreCase(ACCRUAL_ON_CHARGE_SUBMITTED_ON_DATE)) {
            return retrieveScheduleAccrualDataForChargeSubmittedDateProcessing();
        }
        return retrieveScheduleAccrualDataForDefaultProcessing();
    }

    private Collection<LoanScheduleAccrualData> retrieveScheduleAccrualDataForDefaultProcessing() {
        LocalDate organisationStartDate = this.configurationDomainService.retrieveOrganisationStartDate();
        LoanScheduleAccrualMapper mapper = new LoanScheduleAccrualMapper();
        Map<String, Object> paramMap = new HashMap<>(3);
        final StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append("select ").append(mapper.schema()).append(
                " where (recaldet.is_compounding_to_be_posted_as_transaction is null or recaldet.is_compounding_to_be_posted_as_transaction = false) ")
                .append(" and (((ls.fee_charges_amount <> COALESCE(ls.accrual_fee_charges_derived, 0))")
                .append(" or ( ls.penalty_charges_amount <> COALESCE(ls.accrual_penalty_charges_derived, 0))")
                .append(" or ( ls.interest_amount <> COALESCE(ls.accrual_interest_derived, 0)))")
                .append(" and loan.loan_status_id=:active and mpl.accounting_type=:type and loan.is_npa=false and loan.is_charged_off = false and ls.duedate <= :currentDate) ");

        if (organisationStartDate != null) {
            sqlBuilder.append(" and ls.duedate > :organisationStartDate ");
        }
        sqlBuilder.append(" order by loan.id,ls.duedate ");

        paramMap.put("active", LoanStatus.ACTIVE.getValue());
        paramMap.put("type", AccountingRuleType.ACCRUAL_PERIODIC.getValue());
        paramMap.put("organisationStartDate", (organisationStartDate == null) ? DateUtils.getBusinessLocalDate() : organisationStartDate);
        paramMap.put("currentDate", DateUtils.getBusinessLocalDate());
        return this.namedParameterJdbcTemplate.query(sqlBuilder.toString(), paramMap, mapper);

    }

    private Collection<LoanScheduleAccrualData> retrieveScheduleAccrualDataForChargeSubmittedDateProcessing() {
        LocalDate organisationStartDate = this.configurationDomainService.retrieveOrganisationStartDate();
        LoanScheduleAccrualMapper mapper = new LoanScheduleAccrualMapper();
        Map<String, Object> paramMap = new HashMap<>(3);
        final StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append("select ").append(mapper.schema()).append(
                " where (recaldet.is_compounding_to_be_posted_as_transaction is null or recaldet.is_compounding_to_be_posted_as_transaction = false) ")
                .append(" and (((ls.fee_charges_amount <> COALESCE(ls.accrual_fee_charges_derived, 0))")
                .append(" or ( ls.penalty_charges_amount <> COALESCE(ls.accrual_penalty_charges_derived, 0))")
                .append(" or ( ls.interest_amount <> COALESCE(ls.accrual_interest_derived, 0)))")
                .append(" and loan.loan_status_id=:active and mpl.accounting_type=:type and loan.is_npa=false and loan.is_charged_off = false) ");

        if (organisationStartDate != null) {
            sqlBuilder.append(" and ls.duedate > :organisationStartDate ");
        }
        sqlBuilder.append(" order by loan.id,ls.duedate ");

        paramMap.put("active", LoanStatus.ACTIVE.getValue());
        paramMap.put("type", AccountingRuleType.ACCRUAL_PERIODIC.getValue());
        paramMap.put("organisationStartDate", (organisationStartDate == null) ? DateUtils.getBusinessLocalDate() : organisationStartDate);
        return this.namedParameterJdbcTemplate.query(sqlBuilder.toString(), paramMap, mapper);
    }

    @Override
    public Collection<LoanScheduleAccrualData> retrievePeriodicAccrualData(final LocalDate tillDate) {
        return retrievePeriodicAccrualData(tillDate, null);
    }

    @Override
    public Collection<LoanScheduleAccrualData> retrievePeriodicAccrualData(final LocalDate tillDate, final Loan loan) {
        final String chargeAccrualDateCriteria = configurationDomainService.getAccrualDateConfigForCharge();
        if (chargeAccrualDateCriteria.equalsIgnoreCase(ACCRUAL_ON_CHARGE_SUBMITTED_ON_DATE)) {
            return retrievePeriodicAccrualDataForChargeSubmittedDateProcessing(tillDate, loan);
        }
        return retrievePeriodicAccrualDataForDefaultProcessing(tillDate, loan);
    }

    private Collection<LoanScheduleAccrualData> retrievePeriodicAccrualDataForDefaultProcessing(final LocalDate tillDate, final Loan loan) {
        LoanSchedulePeriodicAccrualMapper mapper = new LoanSchedulePeriodicAccrualMapper();
        LocalDate organisationStartDate = this.configurationDomainService.retrieveOrganisationStartDate();
        final StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append("select ").append(mapper.schema()).append(
                " where (recaldet.is_compounding_to_be_posted_as_transaction is null or recaldet.is_compounding_to_be_posted_as_transaction = false) ")
                .append(" and (((ls.fee_charges_amount <> COALESCE(ls.accrual_fee_charges_derived, 0))")
                .append(" or (ls.penalty_charges_amount <> COALESCE(ls.accrual_penalty_charges_derived, 0))")
                .append(" or (ls.interest_amount <> COALESCE(ls.accrual_interest_derived, 0)))")
                .append(" and loan.loan_status_id=:active and mpl.accounting_type=:type and (loan.closedon_date <= :tillDate or loan.closedon_date is null)")
                .append(" and loan.is_npa=false and loan.is_charged_off = false and (ls.duedate <= :tillDate or (ls.duedate > :tillDate and ls.fromdate < :tillDate)")
                .append(" or (ls.installment = 1 and ls.fromdate = :tillDate))) ");
        Map<String, Object> paramMap = new HashMap<>(5);
        if (organisationStartDate != null) {
            sqlBuilder.append(" and ls.duedate > :organisationStartDate ");
            paramMap.put("organisationStartDate", organisationStartDate);
        }
        if (loan != null) {
            sqlBuilder.append(" and loan.id= :loanId ");
            paramMap.put("loanId", loan.getId());
        }
        sqlBuilder.append(" order by loan.id,ls.duedate ");
        paramMap.put("active", LoanStatus.ACTIVE.getValue());
        paramMap.put("type", AccountingRuleType.ACCRUAL_PERIODIC.getValue());
        paramMap.put("tillDate", tillDate);
        return this.namedParameterJdbcTemplate.query(sqlBuilder.toString(), paramMap, mapper);
    }

    private Collection<LoanScheduleAccrualData> retrievePeriodicAccrualDataForChargeSubmittedDateProcessing(final LocalDate tillDate,
            final Loan loan) {
        LoanSchedulePeriodicAccrualMapper mapper = new LoanSchedulePeriodicAccrualMapper();
        LocalDate organisationStartDate = this.configurationDomainService.retrieveOrganisationStartDate();
        final StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append("select ").append(mapper.schema()).append(
                " where (recaldet.is_compounding_to_be_posted_as_transaction is null or recaldet.is_compounding_to_be_posted_as_transaction = false) ")
                .append(" and (((ls.fee_charges_amount <> COALESCE(ls.accrual_fee_charges_derived, 0))")
                .append(" or (ls.penalty_charges_amount <> COALESCE(ls.accrual_penalty_charges_derived, 0))")
                .append(" or (ls.interest_amount <> COALESCE(ls.accrual_interest_derived, 0)))")
                .append(" and loan.loan_status_id=:active and mpl.accounting_type=:type and (loan.closedon_date <= :tillDate or loan.closedon_date is null)")
                .append(" and loan.is_npa=false and loan.is_charged_off = false)");
        Map<String, Object> paramMap = new HashMap<>(5);
        if (organisationStartDate != null) {
            sqlBuilder.append(" and ls.duedate > :organisationStartDate ");
            paramMap.put("organisationStartDate", organisationStartDate);
        }
        if (loan != null) {
            sqlBuilder.append(" and loan.id= :loanId ");
            paramMap.put("loanId", loan.getId());
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
                    .append("curr.display_symbol as currencyDisplaySymbol,curr.name as currencyName,curr.internationalized_name_code as currencyNameCode")
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
            final CurrencyData currencyData = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf,
                    currencyDisplaySymbol, currencyNameCode);

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
                    .append("curr.display_symbol as currencyDisplaySymbol,curr.name as currencyName,curr.internationalized_name_code as currencyNameCode")
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
            final CurrencyData currencyData = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf,
                    currencyDisplaySymbol, currencyNameCode);
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
                loan.getNetDisbursalAmount(), null, null, null, null, null, unrecognizedIncomePortion, paymentOptions, ExternalId.empty(),
                null, null, outstandingLoanBalance, false, loanId, loan.getExternalId());

    }

    @Override
    public LoanTransactionData retrieveLoanWriteoffTemplate(final Long loanId) {

        final LoanAccountData loan = this.retrieveOne(loanId);
        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(LoanTransactionType.WRITEOFF);
        final BigDecimal totalOutstanding = loan.getSummary() != null ? loan.getSummary().getTotalOutstanding() : null;
        final List<CodeValueData> writeOffReasonOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(LoanApiConstants.WRITEOFFREASONS));
        LoanTransactionData loanTransactionData = new LoanTransactionData(null, null, null, transactionType, null, loan.getCurrency(),
                DateUtils.getBusinessLocalDate(), totalOutstanding, loan.getNetDisbursalAmount(), null, null, null, null, null,
                ExternalId.empty(), null, null, null, null, false, loanId, loan.getExternalId());
        loanTransactionData.setWriteOffReasonOptions(writeOffReasonOptions);
        return loanTransactionData;
    }

    @Override
    public LoanTransactionData retrieveLoanChargeOffTemplate(final Long loanId) {

        final LoanAccountData loan = this.retrieveOne(loanId);
        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(LoanTransactionType.CHARGE_OFF);
        final BigDecimal totalOutstanding = loan.getSummary() != null ? loan.getSummary().getTotalOutstanding() : null;
        final BigDecimal totalPrincipalOutstanding = loan.getSummary() != null ? loan.getSummary().getPrincipalOutstanding() : null;
        final BigDecimal totalInterestOutstanding = loan.getSummary() != null ? loan.getSummary().getInterestOutstanding() : null;
        final BigDecimal totalFeeOutstanding = loan.getSummary() != null ? loan.getSummary().getFeeChargesOutstanding() : null;
        final BigDecimal totalPenaltyOutstanding = loan.getSummary() != null ? loan.getSummary().getPenaltyChargesOutstanding() : null;
        final List<CodeValueData> chargeOffReasonOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(LoanApiConstants.CHARGE_OFF_REASONS));
        LoanTransactionData loanTransactionData = new LoanTransactionData(null, null, null, transactionType, null, loan.getCurrency(),
                DateUtils.getBusinessLocalDate(), totalOutstanding, loan.getNetDisbursalAmount(), totalPrincipalOutstanding,
                totalInterestOutstanding, totalFeeOutstanding, totalPenaltyOutstanding, null, ExternalId.empty(), null, null, null, null,
                false, loanId, loan.getExternalId());
        loanTransactionData.setChargeOffReasonOptions(chargeOffReasonOptions);
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
        sqlBuilder.append(" and ml.is_npa = false and ml.is_charged_off = false and dd.is_reversed = false ");
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
        sqlBuilder.append(" and ml.is_npa = false and ml.is_charged_off = false and dd.is_reversed = false ");
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
                    + " where tr.loan_id = ? and tr.transaction_type_enum = ? and tr.is_reversed=false order by tr.transaction_date, tr.created_on_utc, tr.id ";
            return this.jdbcTemplate.query(sql, rm, loanId, LoanTransactionType.WAIVE_INTEREST.getValue()); // NOSONAR
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public boolean isGuaranteeRequired(final Long loanId) {
        final String sql = "select pl.hold_guarantee_funds from m_loan ml inner join m_product_loan pl on pl.id = ml.product_id where ml.id=?";
        return TRUE.equals(this.jdbcTemplate.queryForObject(sql, Boolean.class, loanId));
    }

    @Override
    public Integer retrieveRediferidoNumber(final Long loanId) {
        final String sql = """
                SELECT COUNT(*)
                FROM m_loan_reschedule_request mlrr
                INNER JOIN m_loan_reschedule_request_term_variations_mapping mlrrtvm ON mlrrtvm.loan_reschedule_request_id = mlrr.id
                INNER JOIN m_loan_term_variations mltv ON mltv.id = mlrrtvm.loan_term_variations_id
                WHERE mlrr.loan_id = ? AND mlrr.status_enum = 200 AND mltv.term_type = 11 AND mltv.is_active = TRUE AND mltv.decimal_value > 0
                """;
        return ObjectUtils.defaultIfNull(this.jdbcTemplate.queryForObject(sql, Integer.class, loanId), 0);
    }

    @Override
    public Integer retrieveRediferidoNumberLast6Months(Long loanId) {
        final String sql = """
                SELECT COUNT(*)
                FROM m_loan_reschedule_request mlrr
                INNER JOIN m_loan_reschedule_request_term_variations_mapping mlrrtvm ON mlrrtvm.loan_reschedule_request_id = mlrr.id
                INNER JOIN m_loan_term_variations mltv ON mltv.id = mlrrtvm.loan_term_variations_id
                WHERE mlrr.loan_id = ? AND mlrr.status_enum = 200 AND mltv.term_type = 11 AND mltv.is_active = TRUE AND mltv.decimal_value > 0
                AND mlrr.approved_on_date >= CURRENT_DATE - INTERVAL '6 months'
                """;
        return ObjectUtils.defaultIfNull(this.jdbcTemplate.queryForObject(sql, Integer.class, loanId), 0);
    }

    private static final class LoanTransactionDerivedComponentMapper implements RowMapper<LoanTransactionData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;

        LoanTransactionDerivedComponentMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String schema() {

            return " tr.id as id, tr.transaction_type_enum as transactionType, tr.transaction_date as " + sqlGenerator.escape("date")
                    + ", tr.amount as total, tr.principal_portion_derived as principal, tr.interest_portion_derived as interest, "
                    + " tr.fee_charges_portion_derived as fees, tr.penalty_charges_portion_derived as penalties, "
                    + " tr.overpayment_portion_derived as overpayment, tr.outstanding_loan_balance_derived as outstandingLoanBalance, "
                    + " tr.unrecognized_income_portion as unrecognizedIncome, tr.loan_id as loanId, l.external_id as externalLoanId, "
                    + " tr.external_id as externalId from m_loan_transaction tr " + " left join m_loan l on tr.loan_id = l.id";
        }

        @Override
        public LoanTransactionData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final Long loanId = rs.getLong("loanId");
            final String externalLoanIdStr = rs.getString("externalLoanId");
            final ExternalId externalLoanId = ExternalIdFactory.produce(externalLoanIdStr);
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
            final String externalIdStr = rs.getString("externalId");
            final ExternalId externalId = ExternalIdFactory.produce(externalIdStr);

            return new LoanTransactionData(id, transactionType, date, totalAmount, null, principalPortion, interestPortion,
                    feeChargesPortion, penaltyChargesPortion, overPaymentPortion, unrecognizedIncomePortion, outstandingLoanBalance, false,
                    externalId, loanId, externalLoanId);
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
            final BigDecimal totalCredits = null;

            return LoanSchedulePeriodData.periodWithPayments(loanId, period, fromDate, dueDate, obligationsMetOnDate, complete,
                    principalOriginalDue, principalPaid, principalWrittenOff, principalOutstanding, outstandingPrincipalBalanceOfLoan,
                    interestDueOnPrincipalOutstanding, interestPaid, interestWaived, interestWrittenOff, interestOutstanding, feeChargesDue,
                    feeChargesPaid, feeChargesWaived, feeChargesWrittenOff, feeChargesOutstanding, penaltyChargesDue, penaltyChargesPaid,
                    penaltyChargesWaived, penaltyChargesWrittenOff, penaltyChargesOutstanding, totalDueForPeriod, totalPaid,
                    totalPaidInAdvanceForPeriod, totalPaidLateForPeriod, totalWaived, totalWrittenOff, totalOutstanding,
                    totalActualCostOfLoanForPeriod, totalInstallmentAmount, totalCredits, false);
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
                retrieveTotalPaidInAdvance(loan.getId()).getPaidInAdvance(), loan.getNetDisbursalAmount(), loan.getExternalId());
    }

    @Override
    public LoanTransactionData retrieveCreditBalanceRefundTemplate(Long loanId) {
        this.context.authenticatedUser();

        final Collection<PaymentTypeData> paymentOptions = null;
        final BigDecimal netDisbursal = null;
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        return retrieveRefundTemplate(loanId, LoanTransactionType.CREDIT_BALANCE_REFUND, paymentOptions, loan.getCurrency(),
                loan.getTotalOverpaid(), netDisbursal, loan.getExternalId());

    }

    private LoanTransactionData retrieveRefundTemplate(Long loanId, LoanTransactionType loanTransactionType,
            Collection<PaymentTypeData> paymentOptions, MonetaryCurrency currency, BigDecimal transactionAmount, BigDecimal netDisbursal,
            ExternalId externalLoanId) {

        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);

        final CurrencyData currencyData = applicationCurrency.toData();

        final LocalDate currentDate = LocalDate.now(DateUtils.getDateTimeZoneOfTenant());

        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(loanTransactionType);
        return new LoanTransactionData(null, null, null, transactionType, null, currencyData, currentDate, transactionAmount, null,
                netDisbursal, null, null, null, null, null, paymentOptions, ExternalId.empty(), null, null, null, false, loanId,
                externalLoanId);
    }

    @Override
    public Collection<InterestRatePeriodData> retrieveLoanInterestRatePeriodData(LoanAccountData loanData) {
        this.context.authenticatedUser();

        if (loanData.isLoanProductLinkedToFloatingRate()) {
            final Collection<InterestRatePeriodData> intRatePeriodData = new ArrayList<>();
            final Collection<InterestRatePeriodData> intRates = this.floatingRatesReadPlatformService
                    .retrieveInterestRatePeriods(loanData.getLoanProductId());
            for (final InterestRatePeriodData rate : intRates) {
                if (loanData.getTimeline() == null) {
                    continue;
                }
                boolean isAfterDisbursement = DateUtils.isAfter(rate.getFromDate(), loanData.getTimeline().getDisbursementDate());
                if (isAfterDisbursement && loanData.isFloatingInterestRate()) {
                    updateInterestRatePeriodData(rate, loanData);
                    intRatePeriodData.add(rate);
                } else if (!isAfterDisbursement) {
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
        LoanProductData loanProductData = loanProductReadPlatformService.retrieveLoanProductFloatingDetails(loan.getLoanProductId());
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

        if (loan.getTimeline() != null && DateUtils.isBefore(rate.getFromDate(), loan.getTimeline().getDisbursementDate())) {
            rate.setFromDate(loan.getTimeline().getDisbursementDate());
        }
    }

    @Override
    public Collection<Long> retrieveLoanIdsWithPendingIncomePostingTransactions() {
        LocalDate currentdate = DateUtils.getBusinessLocalDate();
        StringBuilder sqlBuilder = new StringBuilder().append(" select distinct loan.id from m_loan as loan ").append(
                " inner join m_loan_recalculation_details as recdet on (recdet.loan_id = loan.id and recdet.is_compounding_to_be_posted_as_transaction is not null and recdet.is_compounding_to_be_posted_as_transaction = true) ")
                .append(" inner join m_loan_repayment_schedule as repsch on repsch.loan_id = loan.id ")
                .append(" inner join m_loan_interest_recalculation_additional_details as adddet on adddet.loan_repayment_schedule_id = repsch.id ")
                .append(" left join m_loan_transaction as trans on (trans.is_reversed <> true and trans.transaction_type_enum = 19 and trans.loan_id = loan.id and trans.transaction_date = adddet.effective_date) ")
                .append(" where loan.loan_status_id = 300 ").append(" and loan.is_npa = false and loan.is_charged_off = false ")
                .append(" and adddet.effective_date is not null ").append(" and trans.transaction_date is null ")
                .append(" and adddet.effective_date < ? ");
        try {
            return this.jdbcTemplate.queryForList(sqlBuilder.toString(), Long.class, new Object[] { currentdate });
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
                paymentTypeOptions, ExternalId.empty(), null, null, outstandingLoanBalance, isReversed, loanId, loan.getExternalId());
    }

    @Override
    public LoanTransactionData retrieveLoanSpecialWriteOffTemplate(final Long loanId) {
        final List<CodeValueData> writeOffReasonOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(LoanApiConstants.WRITEOFFREASONS));
        this.context.authenticatedUser();
        final LocalDate transactionDate = DateUtils.getBusinessLocalDate();
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        final MonetaryCurrency currency = loan.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);
        final CurrencyData currencyData = applicationCurrency.toData();
        final LocalDate earliestUnpaidInstallmentDate = DateUtils.getBusinessLocalDate();
        final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = loan.fetchLoanSpecialWriteOffDetail(transactionDate);
        BigDecimal unrecognizedIncomePortion = null;
        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(LoanTransactionType.WRITEOFF);
        final Collection<PaymentTypeData> paymentTypeOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        final BigDecimal outstandingLoanBalance = loanRepaymentScheduleInstallment.getPrincipalOutstanding(currency).getAmount();
        final boolean isReversed = false;
        final Money outStandingAmount = loanRepaymentScheduleInstallment.getTotalOutstanding(currency);
        final LoanTransactionData loanTransactionData = new LoanTransactionData(null, null, null, transactionType, null, currencyData,
                earliestUnpaidInstallmentDate, outStandingAmount.getAmount(), loan.getNetDisbursalAmount(),
                loanRepaymentScheduleInstallment.getPrincipalOutstanding(currency).getAmount(),
                loanRepaymentScheduleInstallment.getInterestOutstanding(currency).getAmount(),
                loanRepaymentScheduleInstallment.getFeeChargesOutstanding(currency).getAmount(),
                loanRepaymentScheduleInstallment.getPenaltyChargesOutstanding(currency).getAmount(), null, unrecognizedIncomePortion,
                paymentTypeOptions, ExternalId.empty(), null, null, outstandingLoanBalance, isReversed, loanId, loan.getExternalId());
        loanTransactionData.setWriteOffReasonOptions(writeOffReasonOptions);
        loanTransactionData.setCurrentOutstandingLoanCharges(loanRepaymentScheduleInstallment.getCurrentOutstandingLoanCharges());
        return loanTransactionData;
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
            return new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf, currencyDisplaySymbol, currencyNameCode);
        }

    }

    private static final class RepaymentTransactionTemplateMapper implements RowMapper<LoanTransactionData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;
        private final CurrencyMapper currencyMapper = new CurrencyMapper();

        RepaymentTransactionTemplateMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String schema() {
            // TODO: investigate whether it can be refactored to be more efficient
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("(CASE ");
            sqlBuilder.append(
                    "WHEN (select max(tr.transaction_date) as transaction_date from m_loan_transaction tr where tr.loan_id = l.id AND tr.transaction_type_enum in (?,?) AND tr.is_reversed = false) > ls.dueDate ");
            sqlBuilder.append(
                    "THEN (select max(tr.transaction_date) as transaction_date from m_loan_transaction tr where tr.loan_id = l.id AND tr.transaction_type_enum in (?,?) AND tr.is_reversed = false) ");
            sqlBuilder.append("ELSE ls.dueDate END) as transactionDate, ");
            sqlBuilder.append(
                    "ls.principal_amount - coalesce(ls.principal_writtenoff_derived, 0) - coalesce(ls.principal_completed_derived, 0) as principalDue, ");
            sqlBuilder.append(
                    "ls.interest_amount - coalesce(ls.interest_completed_derived, 0) - coalesce(ls.interest_waived_derived, 0) - coalesce(ls.interest_writtenoff_derived, 0) as interestDue, ");
            sqlBuilder.append(
                    "ls.fee_charges_amount - coalesce(ls.fee_charges_completed_derived, 0) - coalesce(ls.fee_charges_writtenoff_derived, 0) - coalesce(ls.fee_charges_waived_derived, 0) as feeDue, ");
            sqlBuilder.append(
                    "ls.penalty_charges_amount - coalesce(ls.penalty_charges_completed_derived, 0) - coalesce(ls.penalty_charges_writtenoff_derived, 0) - coalesce(ls.penalty_charges_waived_derived, 0) as penaltyDue, ");
            sqlBuilder.append(
                    "l.currency_code as currencyCode, l.currency_digits as currencyDigits, l.currency_multiplesof as inMultiplesOf, l.net_disbursal_amount as netDisbursalAmount, ");
            sqlBuilder.append("rc." + sqlGenerator.escape("name")
                    + " as currencyName, rc.display_symbol as currencyDisplaySymbol, rc.internationalized_name_code as currencyNameCode, ");
            sqlBuilder.append(" l.loan_schedule_type, l.loan_schedule_processing_type, mcv.code_value as loanProductType ");
            sqlBuilder.append("FROM m_loan l ");
            sqlBuilder.append("JOIN m_product_loan mpl on mpl.id = l.product_id ");
            sqlBuilder.append("JOIN m_code_value mcv on mcv.id = mpl.product_type ");
            sqlBuilder.append("JOIN m_currency rc on rc." + sqlGenerator.escape("code") + " = l.currency_code ");
            sqlBuilder.append("JOIN m_loan_repayment_schedule ls ON ls.loan_id = l.id AND ls.completed_derived = false ");
            sqlBuilder.append(
                    "JOIN((SELECT ls.loan_id, ls.duedate as datedue FROM m_loan_repayment_schedule ls WHERE ls.loan_id = ? and ls.completed_derived = false ORDER BY ls.duedate LIMIT 1)) asq on asq.loan_id = ls.loan_id ");
            sqlBuilder.append("AND asq.datedue = ls.duedate ");
            sqlBuilder.append("WHERE l.id = ? LIMIT 1");
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
            final BigDecimal outstandingLoanBalance = null;
            final BigDecimal unrecognizedIncomePortion = null;
            final BigDecimal overPaymentPortion = null;
            final BigDecimal netDisbursalAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "netDisbursalAmount");
            final Long id = null;
            final Long loanId = null;
            final Long officeId = null;
            final String officeName = null;
            boolean manuallyReversed = false;
            final PaymentDetailData paymentDetailData = null;
            final AccountTransferData transfer = null;
            final BigDecimal fixedEmiAmount = null;
            String loanScheduleType = rs.getString("loan_schedule_type");
            String loanScheduleProcessingType = rs.getString("loan_schedule_processing_type");
            String loanProductType = rs.getString("loanProductType");
            LoanTransactionData transactionData = new LoanTransactionData(id, officeId, officeName, transactionType, paymentDetailData,
                    currencyData, date, totalDue, netDisbursalAmount, principalPortion, interestDue, feeDue, penaltyDue, overPaymentPortion,
                    ExternalId.empty(), transfer, fixedEmiAmount, outstandingLoanBalance, unrecognizedIncomePortion, manuallyReversed,
                    loanId, ExternalId.empty());
            transactionData.setTransactionProcessingStrategy(loanScheduleProcessingType);
            transactionData.setLoanScheduleType(loanScheduleType);
            transactionData.setLoanProductType(loanProductType);
            return transactionData;
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
    public List<LoanTransactionRelationData> retrieveLoanTransactionRelationsByLoanTransactionId(Long loanTransactionId) {
        final LoanTransaction loanTransaction = this.loanTransactionRepository.getReferenceById(loanTransactionId);
        List<LoanTransactionRelation> loanTransactionRelations = this.loanTransactionRelationRepository
                .findByFromTransaction(loanTransaction);
        return loanTransactionRelationMapper.map(loanTransactionRelations);
    }

    @Override
    public Long retrieveLoanTransactionIdByExternalId(ExternalId externalId) {
        return loanTransactionRepository.findIdByExternalId(externalId);
    }

    @Override
    public Long retrieveLoanIdByExternalId(ExternalId externalId) {
        return loanRepositoryWrapper.findIdByExternalId(externalId);
    }

    private static final class LoanAssignorMapper implements RowMapper<LoanAssignorData> {

        public String schema() {
            return """
                        mc.id AS id,
                    	mc.display_name AS "displayName",
                    	mc.email_address AS "email",
                    	mc.mobile_no AS "mobileNumber",
                    	mc.submittedon_date AS "submittedOnDate",
                    	cce."NIT" AS nit,
                    	mo.name AS officeName
                    FROM m_office mo
                    INNER JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%') AND ounder.hierarchy LIKE CONCAT(?, '%')
                    INNER JOIN m_client mc ON mc.office_id = ounder.id
                    INNER JOIN campos_cliente_empresas cce  ON cce.client_id = mc.id
                    WHERE mc.status_enum = 300 AND mc.legal_form_enum = 2
                    """;
        }

        @Override
        public LoanAssignorData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final String officeName = rs.getString("officeName");
            final String displayName = rs.getString("displayName");
            final String email = rs.getString("email");
            final String nit = rs.getString("nit");
            final String mobileNumber = rs.getString("mobileNumber");
            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
            return new LoanAssignorData(id, displayName, nit, email, mobileNumber, officeName, submittedOnDate);
        }
    }

    @Override
    public List<LoanAssignorData> retrieveLoanAssignorData(String searchTerm) {
        final String currentUserHierarchy = this.context.authenticatedUser().getOffice().getHierarchy();
        final LoanAssignorMapper rowMapper = new LoanAssignorMapper();
        String sql = "SELECT " + rowMapper.schema();
        Object[] params = new Object[] { currentUserHierarchy };
        if (StringUtils.isNotBlank(searchTerm)) {
            params = new Object[] { currentUserHierarchy, searchTerm, searchTerm };
            sql = sql + " AND (mc.display_name LIKE '%?%' OR cce.\"NIT\" LIKE '%?%')";
        }
        sql = sql + " ORDER BY mc.display_name ASC";
        return this.jdbcTemplate.query(sql, rowMapper, params); // NOSONAR
    }

    @Override
    public LoanAssignorData retrieveLoanAssignorDataById(Long loanAssignorId) {
        final String currentUserHierarchy = this.context.authenticatedUser().getOffice().getHierarchy();
        final LoanAssignorMapper rowMapper = new LoanAssignorMapper();
        final String sql = "SELECT " + rowMapper.schema() + " AND mc.id = ? ";
        final Object[] params = new Object[] { currentUserHierarchy, loanAssignorId };
        final List<LoanAssignorData> resultList = this.jdbcTemplate.query(sql, rowMapper, params);
        if (!CollectionUtils.isEmpty(resultList)) {
            return resultList.get(0);
        }
        return new LoanAssignorData();
    }

    @Override
    public void exportLoanDisbursementPDF(Long loanId, HttpServletResponse httpServletResponse) throws DocumentException, IOException {
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
        final BigDecimal valorDescuento = loan.getValorDescuento();
        final BigDecimal valorGiro = loan.getValorGiro();
        String valorDescuentoAmountString = "";
        String valorGiroAmountString = "";
        boolean showDiscountFields = false;
        if (valorDescuento != null && valorDescuento.compareTo(BigDecimal.ZERO) > 0) {
            valorDescuentoAmountString = Money.of(loan.getCurrency(), valorDescuento).toString();
            valorGiroAmountString = Money.of(loan.getCurrency(), valorGiro).toString();
            showDiscountFields = true;
        }

        final Client loanClient = loan.getClient();
        final Integer legalFormId = loanClient.getLegalForm();
        final ClientAdditionalFieldsData loanAdditionalFieldsData = this.clientReadPlatformService
                .retrieveClientAdditionalData(loanClient.getId());
        final String nit = loanAdditionalFieldsData.getNit();
        final String cedula = loanAdditionalFieldsData.getCedula();
        String clientFullName;
        String clientNit;
        if (LegalForm.PERSON.getValue().equals(legalFormId)) {
            clientNit = cedula;
            final String lastName = loan.getClient().getLastname();
            final String firstName = loan.getClient().getFirstname();
            clientFullName = firstName + " " + lastName;
        } else {
            clientFullName = loanClient.getFullname();
            clientNit = nit;
        }
        final LocalDate disbursementDate = loan.getDisbursementDate();
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy").withLocale(Locale.forLanguageTag("es-ES"));
        final String disbursementDateString = disbursementDate.format(dateFormatter);
        final BigDecimal disbursementAmount = loan.getNetDisbursalAmount();
        final String disbursementAmountString = Money.of(loan.getCurrency(), disbursementAmount).toString();
        final Client loanAssignor = loan.getLoanAssignor();
        String loanAssignorDisplayName = "N/A";
        String loanAssignorNit = "N/A";
        if (loanAssignor != null) {
            final Long loanAssignorId = loanAssignor.getId();
            final LoanAssignorData loanAssignorData = this.retrieveLoanAssignorDataById(loanAssignorId);
            loanAssignorNit = loanAssignorData.getNit();
            loanAssignorDisplayName = loanAssignor.getDisplayName();
        }
        final AppUser disbursedByUser = loan.getDisbursedBy();
        String disbursedByUsername = this.context.authenticatedUser().getDisplayName();
        if (disbursedByUser != null) {
            disbursedByUsername = disbursedByUser.getDisplayName();
        }
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MMMM dd HH:mm:ss")
                .withLocale(Locale.forLanguageTag("es-ES"));
        final String generatedOnDateTime = DateUtils.getLocalDateTimeOfTenant().format(dateTimeFormatter);
        final Map<String, Object> variables = new HashMap<>();
        variables.put("clientFullName", clientFullName);
        variables.put("clientNit", clientNit);
        variables.put("disbursementDate", disbursementDateString);
        variables.put("loanId", loanId);
        variables.put("disbursementAmount", disbursementAmountString);
        variables.put("disbursedByUsername", disbursedByUsername);
        variables.put("loanAssignorDisplayName", loanAssignorDisplayName);
        variables.put("loanAssignorNit", loanAssignorNit);
        variables.put("generatedOnDateTime", generatedOnDateTime);
        variables.put("generatedByUsername", this.context.authenticatedUser().getDisplayName());
        variables.put("valorDescuento", valorDescuentoAmountString);
        variables.put("valorGiro", valorGiroAmountString);
        variables.put("showDiscountFields", showDiscountFields);
        final org.thymeleaf.context.Context thymeleafContext = new org.thymeleaf.context.Context(Locale.forLanguageTag("es-ES"), variables);
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setOrder(0);
        templateResolver.setCheckExistence(true);
        final TemplateEngine thymeleafTemplateEngine = new SpringTemplateEngine();
        thymeleafTemplateEngine.setTemplateResolver(templateResolver);
        final String html = thymeleafTemplateEngine.process("LoanDisbursementReport", thymeleafContext);
        final ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        final String filename = "Reporte_de_desembolso_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".pdf";
        httpServletResponse.setContentType("application/pdf");
        httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + filename);
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        final OutputStream outputStream = httpServletResponse.getOutputStream();
        renderer.createPDF(outputStream);
        outputStream.close();
    }

    @Override
    public void exportLoanSchedulePDF(LoanAccountData loanBasicDetails, String scheduleType, LoanScheduleData repaymentSchedule,
            HttpServletResponse httpServletResponse) throws DocumentException, IOException {

        boolean isRepaymentSchedule = scheduleType.equalsIgnoreCase("repayment");
        final ClientAdditionalFieldsData loanAdditionalFieldsData = this.clientReadPlatformService
                .retrieveClientAdditionalData(loanBasicDetails.getClientId());
        final String nit = loanAdditionalFieldsData.getNit();
        final String cedula = loanAdditionalFieldsData.getCedula();
        String clientFullName = loanBasicDetails.getClientName();
        String clientNit = Objects.toString(nit, cedula);
        LoanApplicationTimelineData timeline = loanBasicDetails.getTimeline();
        final LocalDate disbursementDate = timeline.getDisbursementDate() != null ? timeline.getActualDisbursementDate()
                : timeline.getExpectedDisbursementDate();
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy").withLocale(Locale.forLanguageTag("es-ES"));
        final String disbursementDateString = disbursementDate != null ? disbursementDate.format(dateFormatter) : "N/A";
        final BigDecimal disbursementAmount = loanBasicDetails.getNetDisbursalAmount();

        final String disbursementAmountString = Money.of(loanBasicDetails.getCurrency(), disbursementAmount).toString();

        final Map<String, String> amortizationTypes = Map.of("Equal installments", "Pagos de cuotas iguales - Sistema Frances",
                "Equal principal payments", "Pagos de capital iguales - Sistema Aleman", "Capital at end",
                "Capital al final - Sistema Americano");
        String templateName;
        String fileType;
        String reportTitle;
        String amortizationType = amortizationTypes.getOrDefault(loanBasicDetails.getAmortizationType().getValue(), "");
        String settlementSystem = "";
        if (StringUtils.isNotBlank(amortizationType)) {
            // get the second part of the hyphen . e.g Sistema Frances
            settlementSystem = amortizationType.substring(amortizationType.indexOf("-") + 1).trim();
        }

        if (isRepaymentSchedule) {
            templateName = "LoanRepaymentScheduleReport";
            fileType = "Reported_calendario_de_pago";
            reportTitle = "Calendario de Pagos";
        } else {
            templateName = "LoanOriginalScheduleReport";
            reportTitle = "Calendario Original";
            fileType = "Reported_calendario_de_original";
        }
        String loanRate = convertInterestRateToDailyRateAndMonthlyRate(loanBasicDetails.getInterestRatePerPeriod().doubleValue());

        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MMMM dd HH:mm:ss")
                .withLocale(Locale.forLanguageTag("es-ES"));
        final String generatedOnDateTime = DateUtils.getLocalDateTimeOfTenant().format(dateTimeFormatter);
        final Map<String, Object> variables = new HashMap<>();
        variables.put("clientFullName", clientFullName);
        variables.put("clientNit", clientNit);
        variables.put("disbursementDate", disbursementDateString);
        variables.put("reportTitle", reportTitle);
        variables.put("disbursementAmount", disbursementAmountString);
        variables.put("generatedOnDateTime", generatedOnDateTime);
        variables.put("generatedByUsername", this.context.authenticatedUser().getDisplayName());
        variables.put("originalScheduleDetails", repaymentSchedule);
        variables.put("loanRate", loanRate);
        variables.put("settlementSystem", settlementSystem);
        final org.thymeleaf.context.Context thymeleafContext = new org.thymeleaf.context.Context(Locale.forLanguageTag("es-ES"), variables);
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        Locale.setDefault(new Locale("es", "ES"));
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setOrder(0);
        templateResolver.setCheckExistence(true);
        final TemplateEngine thymeleafTemplateEngine = new SpringTemplateEngine();
        thymeleafTemplateEngine.setTemplateResolver(templateResolver);

        final String html = thymeleafTemplateEngine.process(templateName, thymeleafContext);
        final ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();

        final String filename = fileType + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".pdf";
        httpServletResponse.setContentType("application/pdf");
        httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + filename);
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        final OutputStream outputStream = httpServletResponse.getOutputStream();
        renderer.createPDF(outputStream);
        outputStream.close();
    }

    @Override
    public Collection<Long> retrieveClientsWithLoansInActive(Long loanProductId, Integer inactivityPeriod) {

        final String query = """
                select distinct c.id  from m_client c
                inner join m_loan l on l.client_id = c.id
                inner join m_product_loan p on p.id = l.product_id
                where l.loan_status_id in ( 100,200,300,303,304 )\s
                and c.id not in (
                	select cbr.client_id from m_client_blocking_reason cbr inner join\s
                	m_blocking_reason_setting mbrs on mbrs.id = cbr.blocking_reason_id and\s
                	mbrs.name_of_reason = 'INACTIVIDAD'
                )
                and p.id = ? and (DATE_PART('year', ?::date) * 12 + DATE_PART('month', ?::date) - DATE_PART('year', COALESCE(l.last_activity_date, CURRENT_DATE)) * 12 - DATE_PART('month', COALESCE(l.last_activity_date, CURRENT_DATE))) >= ?
                """;

        final LocalDate currentDate = DateUtils.getBusinessLocalDate();

        return this.jdbcTemplate.queryForList(query, Long.class, loanProductId, currentDate, currentDate, inactivityPeriod);
    }

    private static final class DefaultInsuranceMapper implements RowMapper<DefaultOrCancelInsuranceInstallmentData> {

        public String schema() {
            return """
                    ml.id loanId, min(mlrs.installment) installment, mlc.id loanChargeId
                        from
                            m_loan ml
                            join m_loan_repayment_schedule mlrs on mlrs.loan_id = ml.id and mlrs.completed_derived = false
                            join m_loan_charge mlc on mlc.loan_id = ml.id and mlc.charge_calculation_enum = 1034
                             join m_loan_installment_charge mlic on mlic.loan_charge_id = mlc.id and mlic.loan_schedule_id = mlrs.id
                             join m_charge mc on mc.id = mlc.charge_id
                        where
                            ml.loan_status_id = 300
                            and mlc.amount_outstanding_derived > 0
                            and mlc.default_from_installment is null
                            and mlic.amount_outstanding_derived > 0
                    """;
        }

        @Override
        public DefaultOrCancelInsuranceInstallmentData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            final Long loanId = JdbcSupport.getLong(rs, "loanId");
            final Integer installment = JdbcSupport.getInteger(rs, "installment");
            final Long loanChargeId = JdbcSupport.getLong(rs, "loanChargeId");

            return new DefaultOrCancelInsuranceInstallmentData(loanId, loanChargeId, installment);
        }
    }

    @Override
    public List<DefaultOrCancelInsuranceInstallmentData> getLoanDataWithDefaultOrCancelInsurance(Long loanId, Long insuranceCode,
            LocalDate date) {

        final DefaultInsuranceMapper rowMapper = new DefaultInsuranceMapper();
        String sql = "SELECT " + rowMapper.schema();
        Object[] params = null;
        sql = sql + " and mc.charge_calculation_enum =  " + ChargeCalculationType.FLAT_SEGOVOLUNTARIO.getValue();
        if (loanId == null && insuranceCode == null) {
            sql = sql + " and mlrs.duedate < CURRENT_DATE " + "                        and mc.days_in_arrears is not null "
                    + "                        and mc.days_in_arrears > 0 "
                    + "                        and CURRENT_DATE - mlrs.duedate > mc.days_in_arrears ";
            params = new Object[] {};
        } else if (insuranceCode == null) {
            sql = sql + " and ml.id = ? ";
            params = new Object[] { loanId };
        } else {
            sql = sql + " and ml.id = ? and mc.insurance_code = ? ";
            params = new Object[] { loanId, insuranceCode };
        }
        if (date != null) {
            sql = sql + " and mlrs.fromdate > ? ";
            final int N = params.length;
            params = Arrays.copyOf(params, N + 1);
            params[N] = date;
        }
        sql = sql + " group by ml.id, mlc.id order by ml.id";

        return this.jdbcTemplate.query(sql, rowMapper, params); // NOSONAR
    }

    private String convertInterestRateToDailyRateAndMonthlyRate(double interestRate) {
        // Convert the annual rate to a decimal form
        double annualRateDecimal = interestRate / 100.0;

        // Calculate the daily rate
        double dailyRate = annualRateDecimal / 365 * 100.0; // Assuming 365 days in a year

        // Calculate the monthly nominal rate
        double monthlyNominalRate = annualRateDecimal / 12 * 100.0; // Corrected calculation

        // Format the rates, removing decimal places for whole numbers
        String formattedDailyRate = formatRate(dailyRate);
        String formattedMonthlyRate = formatRate(monthlyNominalRate);

        // Return in the format "Tasa pactada: % diario % mnv"
        return String.format(" %s%% diario %s%% mnv", formattedDailyRate, formattedMonthlyRate);
    }

    // Helper method to format rates
    private String formatRate(double rate) {
        if (Math.abs(rate - Math.floor(rate)) < 0.000001) {
            return String.format("%.0f", rate).replace(".", ",");
        } else {
            return String.format("%.2f", rate).replace(".", ",");
        }
    }
}
