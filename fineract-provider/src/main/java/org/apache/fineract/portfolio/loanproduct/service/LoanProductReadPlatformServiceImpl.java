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
package org.apache.fineract.portfolio.loanproduct.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fineract.accounting.common.AccountingEnumerations;
import org.apache.fineract.custom.infrastructure.channel.data.ChannelData;
import org.apache.fineract.custom.infrastructure.channel.service.ChannelReadWritePlatformService;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityType;
import org.apache.fineract.infrastructure.entityaccess.service.FineractEntityAccessUtil;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysEnumerations;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.common.service.CommonEnumerations;
import org.apache.fineract.portfolio.delinquency.data.DelinquencyBucketData;
import org.apache.fineract.portfolio.delinquency.service.DelinquencyReadPlatformService;
import org.apache.fineract.portfolio.interestrates.data.InterestRateData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleProcessingType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.apache.fineract.portfolio.loanproduct.data.AdvanceQuotaConfigurationData;
import org.apache.fineract.portfolio.loanproduct.data.AdvancedPaymentData;
import org.apache.fineract.portfolio.loanproduct.data.AdvancedPaymentData.PaymentAllocationOrder;
import org.apache.fineract.portfolio.loanproduct.data.CreditAllocationData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductBorrowerCycleVariationData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductGuaranteeData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductInterestRecalculationData;
import org.apache.fineract.portfolio.loanproduct.data.MaximumCreditRateConfigurationData;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductConfigurableAttributes;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductParamType;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.apache.fineract.portfolio.rate.data.RateData;
import org.apache.fineract.portfolio.rate.service.RateReadService;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@RequiredArgsConstructor
public class LoanProductReadPlatformServiceImpl implements LoanProductReadPlatformService {

    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;
    private final ChargeReadPlatformService chargeReadPlatformService;
    private final RateReadService rateReadService;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final FineractEntityAccessUtil fineractEntityAccessUtil;
    private final DelinquencyReadPlatformService delinquencyReadPlatformService;
    private final LoanProductRepository loanProductRepository;
    private final ChannelReadWritePlatformService channelReadPlatformService;

    @Override
    public LoanProductData retrieveLoanProduct(final Long loanProductId) {
        try {
            final Collection<ChargeData> charges = this.chargeReadPlatformService.retrieveLoanProductCharges(loanProductId);
            final Collection<RateData> rates = this.rateReadService.retrieveProductLoanRates(loanProductId);
            final SearchParameters channelSearchParameters = SearchParameters.builder().productId(loanProductId).build();
            final List<ChannelData> repaymentChannels = this.channelReadPlatformService.findBySearchParam(channelSearchParameters);
            final Collection<LoanProductBorrowerCycleVariationData> borrowerCycleVariationDatas = retrieveLoanProductBorrowerCycleVariations(
                    loanProductId);
            final Collection<AdvancedPaymentData> advancedPaymentData = retrieveAdvancedPaymentData(loanProductId);
            final Collection<CreditAllocationData> creditAllocationData = retrieveCreditAllocationData(loanProductId);
            final Collection<DelinquencyBucketData> delinquencyBucketOptions = this.delinquencyReadPlatformService
                    .retrieveAllDelinquencyBuckets();
            final LoanProductMapper rm = new LoanProductMapper(charges, borrowerCycleVariationDatas, rates, delinquencyBucketOptions,
                    advancedPaymentData, creditAllocationData);
            final String sql = "select " + rm.loanProductSchema() + " where lp.id = ? ";
            LoanProductData loanProductData = this.jdbcTemplate.queryForObject(sql, rm, loanProductId);
            if (loanProductData != null) {
                loanProductData.setRepaymentChannels(repaymentChannels);
            }
            return loanProductData;
        } catch (final EmptyResultDataAccessException e) {
            throw new LoanProductNotFoundException(loanProductId, e);
        }
    }

    @Override
    public LoanProduct retrieveLoanProductByExternalId(final ExternalId externalId) {
        return loanProductRepository.findByExternalId(externalId);
    }

    @Override
    public Collection<LoanProductBorrowerCycleVariationData> retrieveLoanProductBorrowerCycleVariations(final Long loanProductId) {
        final LoanProductBorrowerCycleMapper rm = new LoanProductBorrowerCycleMapper();
        final String sql = "select " + rm.schema() + " where bc.loan_product_id=?  order by bc.borrower_cycle_number,bc.value_condition";
        return this.jdbcTemplate.query(sql, rm, loanProductId); // NOSONAR
    }

    @Override
    public List<AdvancedPaymentData> retrieveAdvancedPaymentData(final Long loanProductId) {
        final AdvancedPaymentDataMapper apdm = new AdvancedPaymentDataMapper();
        final String sql = "select " + apdm.schema() + " where loan_product_id = ?";
        return this.jdbcTemplate.query(sql, apdm, loanProductId); // NOSONAR
    }

    @Override
    public List<CreditAllocationData> retrieveCreditAllocationData(final Long loanProductId) {
        final CreditAllocationDataMapper cadm = new CreditAllocationDataMapper();
        final String sql = "select " + cadm.schema() + " where loan_product_id = ?";
        return this.jdbcTemplate.query(sql, cadm, loanProductId); // NOSONAR
    }

    @Override
    public Collection<LoanProductData> retrieveAllLoanProducts() {

        this.context.authenticatedUser();

        final LoanProductMapper rm = new LoanProductMapper(null, null, null, null, null, null);

        String sql = "select " + rm.loanProductSchema();

        // Check if branch specific products are enabled. If yes, fetch only
        // products mapped to current user's office
        String inClause = fineractEntityAccessUtil
                .getSQLWhereClauseForProductIDsForUserOffice_ifGlobalConfigEnabled(FineractEntityType.LOAN_PRODUCT);
        if (inClause != null && !inClause.trim().isEmpty()) {
            sql += " where lp.id in ( " + inClause + " ) ";
        }

        return this.jdbcTemplate.query(sql, rm); // NOSONAR
    }

    @Override
    public Collection<LoanProductData> retrieveAllLoanProductsForLookup(String inClause) {

        this.context.authenticatedUser();

        final LoanProductLookupMapper rm = new LoanProductLookupMapper(sqlGenerator);

        String sql = "select " + rm.schema();

        if (inClause != null && !inClause.trim().isEmpty()) {
            sql += " where lp.id in (" + inClause + ") ";
            // Here no need to check injection as this is internal where clause
            // SQLInjectionValidator.validateSQLInput(inClause);
        }

        return this.jdbcTemplate.query(sql, rm); // NOSONAR
    }

    @Override
    public Collection<LoanProductData> retrieveAllLoanProductsForLookup() {
        return retrieveAllLoanProductsForLookup(false);
    }

    @Override
    public Collection<LoanProductData> retrieveAllLoanProductsForLookup(final boolean activeOnly) {
        this.context.authenticatedUser();

        final LoanProductLookupMapper rm = new LoanProductLookupMapper(sqlGenerator);

        String sql = "select ";
        if (activeOnly) {
            sql += rm.activeOnlySchema();
        } else {
            sql += rm.schema();
        }

        // Check if branch specific products are enabled. If yes, fetch only
        // products mapped to current user's office
        String inClause = fineractEntityAccessUtil
                .getSQLWhereClauseForProductIDsForUserOffice_ifGlobalConfigEnabled(FineractEntityType.LOAN_PRODUCT);
        if (inClause != null && !inClause.trim().isEmpty()) {
            if (activeOnly) {
                sql += " and id in ( " + inClause + " )";
            } else {
                sql += " where id in ( " + inClause + " ) ";
            }
        }

        return this.jdbcTemplate.query(sql, rm); // NOSONAR
    }

    @Override
    public LoanProductData retrieveNewLoanProductDetails() {
        return LoanProductData.sensibleDefaultsForNewLoanProductCreation();
    }

    private static final class LoanProductMapper implements RowMapper<LoanProductData> {

        private final Collection<ChargeData> charges;

        private final Collection<LoanProductBorrowerCycleVariationData> borrowerCycleVariationDatas;

        private final Collection<AdvancedPaymentData> advancedPaymentData;

        private final Collection<CreditAllocationData> creditAllocationData;

        private final Collection<RateData> rates;

        private final Collection<DelinquencyBucketData> delinquencyBucketOptions;

        LoanProductMapper(final Collection<ChargeData> charges,
                final Collection<LoanProductBorrowerCycleVariationData> borrowerCycleVariationDatas, final Collection<RateData> rates,
                final Collection<DelinquencyBucketData> delinquencyBucketOptions, Collection<AdvancedPaymentData> advancedPaymentData,
                Collection<CreditAllocationData> creditAllocationData) {
            this.charges = charges;
            this.borrowerCycleVariationDatas = borrowerCycleVariationDatas;
            this.rates = rates;
            this.delinquencyBucketOptions = delinquencyBucketOptions;
            this.advancedPaymentData = advancedPaymentData;
            this.creditAllocationData = creditAllocationData;
        }

        public String loanProductSchema() {
            return """
                                	lp.id AS id,
                                	lp.fund_id AS "fundId",
                                	lp.is_advance AS "advance",
                                	lp.require_points AS "requirePoints",
                                	f.name AS "fundName",
                                	lp.loan_transaction_strategy_code AS "transactionStrategyCode",
                                	lp.loan_transaction_strategy_name AS "transactionStrategyName",
                                	mir.id AS "interestRateId",
                                	mir.name AS "interestRateName",
                                	mir.current_rate AS "interestCurrentRate",
                                	mir.appliedon_date AS "interestRateAppliedOnDate",
                                	mir.is_active AS "interestRateActive",
                                	lp.name AS name,
                                	lp.short_name AS "shortName",
                                	lp.description AS "description",
                                	lp.principal_amount AS "principal",
                                	lp.min_principal_amount AS "minPrincipal",
                                	lp.max_principal_amount AS "maxPrincipal",
                                	lp.currency_code AS "currencyCode",
                                	lp.currency_digits AS "currencyDigits",
                                	lp.currency_multiplesof AS "inMultiplesOf",
                                	lp.nominal_interest_rate_per_period AS "interestRatePerPeriod",
                                	lp.min_nominal_interest_rate_per_period AS "minInterestRatePerPeriod",
                                	lp.max_nominal_interest_rate_per_period AS "maxInterestRatePerPeriod",
                                	lp.interest_period_frequency_enum AS "interestRatePerPeriodFreq",
                                	lp.annual_nominal_interest_rate AS "annualInterestRate",
                                	lp.interest_method_enum AS "interestMethod",
                                	lp.interest_calculated_in_period_enum AS "interestCalculationInPeriodMethod",
                                	lp.allow_partial_period_interest_calcualtion AS "allowPartialPeriodInterestCalcualtion",
                                	lp.repay_every AS "repaidEvery",
                                	lp.repayment_period_frequency_enum AS "repaymentPeriodFrequency",
                                	lp.number_of_repayments AS "numberOfRepayments",
                                	lp.min_number_of_repayments AS "minNumberOfRepayments",
                                	lp.max_number_of_repayments AS "maxNumberOfRepayments",
                                	lp.grace_on_principal_periods AS "graceOnPrincipalPayment",
                                	lp.recurring_moratorium_principal_periods AS "recurringMoratoriumOnPrincipalPeriods",
                                	lp.grace_on_interest_periods AS "graceOnInterestPayment",
                                	lp.grace_on_charges_periods AS "graceOnChargesPayment",
                                	lp.grace_interest_free_periods AS "graceOnInterestCharged",
                                	lp.grace_on_arrears_ageing AS "graceOnArrearsAgeing",
                                	lp.overdue_days_for_npa AS "overdueDaysForNPA",
                                	lp.min_days_between_disbursal_and_first_repayment AS "minimumDaysBetweenDisbursalAndFirstRepayment",
                                	lp.amortization_method_enum AS "amortizationMethod",
                                	lp.arrearstolerance_amount AS "tolerance",
                                	lp.accounting_type AS "accountingType",
                                	lp.include_in_borrower_cycle AS "includeInBorrowerCycle",
                                	lp.use_borrower_cycle AS "useBorrowerCycle",
                                	lp.start_date AS "startDate",
                                	lp.close_date AS "closeDate",
                                	lp.allow_multiple_disbursals AS "multiDisburseLoan",
                                	lp.max_disbursals AS "maxTrancheCount",
                                	lp.max_outstanding_loan_balance AS "outstandingLoanBalance",
                                	lp.disallow_expected_disbursements AS "disallowExpectedDisbursements",
                                	lp.allow_approved_disbursed_amounts_over_applied AS "allowApprovedDisbursedAmountsOverApplied",
                                	lp.over_applied_calculation_type AS "overAppliedCalculationType",
                                	over_applied_number AS "overAppliedNumber",
                                	lp.days_in_month_enum AS "daysInMonth",
                                	lp.days_in_year_enum AS "daysInYear",
                                	lp.interest_recalculation_enabled AS "isInterestRecalculationEnabled",
                                	lp.can_define_fixed_emi_amount AS "canDefineInstallmentAmount",
                                	lp.instalment_amount_in_multiples_of AS "installmentAmountInMultiplesOf",
                                	lp.due_days_for_repayment_event AS "dueDaysForRepaymentEvent",
                                	lp.overdue_days_for_repayment_event AS "overDueDaysForRepaymentEvent",
                                	lp.enable_down_payment AS "enableDownPayment",
                                	lp.disbursed_amount_percentage_for_down_payment AS "disbursedAmountPercentageForDownPayment",
                                	lp.enable_auto_repayment_for_down_payment AS "enableAutoRepaymentForDownPayment",
                                	lp.repayment_start_date_type_enum AS "repaymentStartDateType",
                                	lp.enable_installment_level_delinquency AS "enableInstallmentLevelDelinquency",
                                	lpr.pre_close_interest_calculation_strategy AS "preCloseInterestCalculationStrategy",
                                	lpr.id AS "lprId",
                                	lpr.product_id AS "productId",
                                	lpr.compound_type_enum AS "compoundType",
                                	lpr.reschedule_strategy_enum AS "rescheduleStrategy",
                                	lpr.rest_frequency_type_enum AS "restFrequencyEnum",
                                	lpr.rest_frequency_interval AS "restFrequencyInterval",
                                	lpr.rest_frequency_nth_day_enum AS "restFrequencyNthDayEnum",
                                	lpr.rest_frequency_weekday_enum AS "restFrequencyWeekDayEnum",
                                	lpr.rest_frequency_on_day AS "restFrequencyOnDay",
                                	lpr.arrears_based_on_original_schedule AS "isArrearsBasedOnOriginalSchedule",
                                	lpr.compounding_frequency_type_enum AS "compoundingFrequencyTypeEnum",
                                	lpr.compounding_frequency_interval AS "compoundingInterval",
                                	lpr.compounding_frequency_nth_day_enum AS "compoundingFrequencyNthDayEnum",
                                	lpr.compounding_frequency_weekday_enum AS "compoundingFrequencyWeekDayEnum",
                                	lpr.compounding_frequency_on_day AS "compoundingFrequencyOnDay",
                                	lpr.is_compounding_to_be_posted_as_transaction AS "isCompoundingToBePostedAsTransaction",
                                	lpr.allow_compounding_on_eod AS "allowCompoundingOnEod",
                                	lp.hold_guarantee_funds AS "holdGuaranteeFunds",
                                	lp.principal_threshold_for_last_installment AS "principalThresholdForLastInstallment",
                                	lp.fixed_principal_percentage_per_installment "fixedPrincipalPercentagePerInstallment",
                                	lp.sync_expected_with_disbursement_date AS "syncExpectedWithDisbursementDate",
                                	lpg.id AS "lpgId",
                                	lpg.mandatory_guarantee AS "mandatoryGuarantee",
                                	lpg.minimum_guarantee_from_own_funds AS "minimumGuaranteeFromOwnFunds",
                                	lpg.minimum_guarantee_from_guarantor_funds AS "minimumGuaranteeFromGuarantor",
                                	lp.account_moves_out_of_npa_only_on_arrears_completion AS "accountMovesOutOfNPAOnlyOnArrearsCompletion",
                                	curr.name AS "currencyName",
                                	curr.internationalized_name_code AS "currencyNameCode",
                                	curr.display_symbol AS "currencyDisplaySymbol",
                                	lp.external_id AS "externalId",
                                	lca.id AS "lcaId",
                                	lca.amortization_method_enum AS "amortizationBoolean",
                                	lca.interest_method_enum AS "interestMethodConfigBoolean",
                                	lca.loan_transaction_strategy_code AS "transactionProcessingStrategyBoolean",
                                	lca.interest_calculated_in_period_enum AS "interestCalcPeriodBoolean",
                                	lca.arrearstolerance_amount AS "arrearsToleranceBoolean",
                                	lca.repay_every AS "repaymentFrequencyBoolean",
                                	lca.moratorium AS "graceOnPrincipalAndInterestBoolean",
                                	lca.grace_on_arrears_ageing AS "graceOnArrearsAgingBoolean",
                                	lp.is_linked_to_floating_interest_rates AS "isLinkedToFloatingInterestRates",
                                	lfr.floating_rates_id AS "floatingRateId",
                                	fr.name AS "floatingRateName",
                                	lfr.interest_rate_differential AS "interestRateDifferential",
                                	lfr.min_differential_lending_rate AS "minDifferentialLendingRate",
                                	lfr.default_differential_lending_rate AS "defaultDifferentialLendingRate",
                                	lfr.max_differential_lending_rate AS "maxDifferentialLendingRate",
                                	lfr.is_floating_interest_rate_calculation_allowed AS "isFloatingInterestRateCalculationAllowed",
                                	lp.allow_variabe_installments AS "isVariableIntallmentsAllowed",
                                	lvi.minimum_gap AS "minimumGap",
                                	lvi.maximum_gap AS "maximumGap",
                                	dbuc.id AS "delinquencyBucketId",
                                	dbuc.name AS "delinquencyBucketName",
                                	lp.can_use_for_topup AS "canUseForTopup",
                                	lp.is_equal_amortization AS "isEqualAmortization",
                                	lp.loan_schedule_type AS "loanScheduleType",
                                	lp.loan_schedule_processing_type AS "loanScheduleProcessingType",
                                	lp.repayment_rescheduling_enum AS "repaymentReschedulingType",
                                	lp.max_client_inactivity_period AS "maxClientInactivityPeriod",
                                	lp.overdue_amount_for_arrears "overdueAmountForArrears",
                                	lp.extend_term_monthly_repayments AS "extendTermForMonthlyRepayments",
                                	pty.code_value AS "productTypeValue",
                                	pty.id AS "productTypeId",
                                	lp.overdue_days_for_npa AS "overdueDaysForNPA",
                                	lp.custom_allow_create_or_disburse AS "customAllowCreateOrDisburse",
                                	lp.custom_allow_collections AS "customAllowCollections",
                                	lp.custom_allow_credit_note AS "customAllowCreditNote",
                                	lp.custom_allow_debit_note AS "customAllowDebitNote",
                                	lp.custom_allow_forgiveness AS "customAllowForgiveness",
                                	lp.custom_allow_reversal_cancellation AS "customAllowReversalCancellation"
                                FROM m_product_loan lp
                                JOIN m_currency curr ON curr.code = lp.currency_code
                                LEFT JOIN m_fund f ON f.id = lp.fund_id
                                LEFT JOIN m_product_loan_recalculation_details lpr ON lpr.product_id = lp.id
                                LEFT JOIN m_product_loan_guarantee_details lpg ON lpg.loan_product_id = lp.id
                                LEFT JOIN m_product_loan_configurable_attributes lca ON lca.loan_product_id = lp.id
                                LEFT JOIN m_product_loan_floating_rates AS lfr ON lfr.loan_product_id = lp.id
                                LEFT JOIN m_floating_rates AS fr ON lfr.floating_rates_id = fr.id
                                LEFT JOIN m_product_loan_variable_installment_config AS lvi ON lvi.loan_product_id = lp.id
                                LEFT JOIN m_delinquency_bucket AS dbuc ON dbuc.id = lp.delinquency_bucket_id
                                LEFT JOIN m_code_value AS pty ON pty.id = lp.product_type
                                LEFT JOIN m_interest_rate mir ON mir.id = lp.interest_rate_id
                    """;

        }

        @Override
        public LoanProductData mapRow(@NotNull final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = JdbcSupport.getLong(rs, "id");
            final String name = rs.getString("name");
            final String shortName = rs.getString("shortName");
            final String description = rs.getString("description");
            final Long fundId = JdbcSupport.getLong(rs, "fundId");
            final String fundName = rs.getString("fundName");
            final String transactionStrategyCode = rs.getString("transactionStrategyCode");
            final String transactionStrategyName = rs.getString("transactionStrategyName");

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");

            final CurrencyData currency = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf, currencyDisplaySymbol,
                    currencyNameCode);

            final BigDecimal principal = rs.getBigDecimal("principal");
            final BigDecimal minPrincipal = rs.getBigDecimal("minPrincipal");
            final BigDecimal maxPrincipal = rs.getBigDecimal("maxPrincipal");
            final BigDecimal tolerance = rs.getBigDecimal("tolerance");

            final Integer numberOfRepayments = JdbcSupport.getInteger(rs, "numberOfRepayments");
            final Integer minNumberOfRepayments = JdbcSupport.getInteger(rs, "minNumberOfRepayments");
            final Integer maxNumberOfRepayments = JdbcSupport.getInteger(rs, "maxNumberOfRepayments");
            final Integer repaymentEvery = JdbcSupport.getInteger(rs, "repaidEvery");

            final Integer graceOnPrincipalPayment = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnPrincipalPayment");
            final Integer recurringMoratoriumOnPrincipalPeriods = JdbcSupport.getIntegerDefaultToNullIfZero(rs,
                    "recurringMoratoriumOnPrincipalPeriods");
            final Integer graceOnInterestPayment = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnInterestPayment");
            final Integer graceOnChargesPayment = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnChargesPayment");
            final Integer graceOnInterestCharged = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnInterestCharged");
            final Integer graceOnArrearsAgeing = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnArrearsAgeing");
            final Integer overdueDaysForNPA = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "overdueDaysForNPA");
            final Integer minimumDaysBetweenDisbursalAndFirstRepayment = JdbcSupport.getInteger(rs,
                    "minimumDaysBetweenDisbursalAndFirstRepayment");

            final Integer accountingRuleId = JdbcSupport.getInteger(rs, "accountingType");
            final EnumOptionData accountingRuleType = AccountingEnumerations.accountingRuleType(accountingRuleId);

            final BigDecimal interestRatePerPeriod = rs.getBigDecimal("interestRatePerPeriod");
            final BigDecimal minInterestRatePerPeriod = rs.getBigDecimal("minInterestRatePerPeriod");
            final BigDecimal maxInterestRatePerPeriod = rs.getBigDecimal("maxInterestRatePerPeriod");
            final BigDecimal annualInterestRate = rs.getBigDecimal("annualInterestRate");

            final boolean isLinkedToFloatingInterestRates = rs.getBoolean("isLinkedToFloatingInterestRates");
            final Integer floatingRateId = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "floatingRateId");
            final String floatingRateName = rs.getString("floatingRateName");
            final BigDecimal interestRateDifferential = rs.getBigDecimal("interestRateDifferential");
            final BigDecimal minDifferentialLendingRate = rs.getBigDecimal("minDifferentialLendingRate");
            final BigDecimal defaultDifferentialLendingRate = rs.getBigDecimal("defaultDifferentialLendingRate");
            final BigDecimal maxDifferentialLendingRate = rs.getBigDecimal("maxDifferentialLendingRate");
            final boolean isFloatingInterestRateCalculationAllowed = rs.getBoolean("isFloatingInterestRateCalculationAllowed");

            final boolean isVariableIntallmentsAllowed = rs.getBoolean("isVariableIntallmentsAllowed");
            final Integer minimumGap = rs.getInt("minimumGap");
            final Integer maximumGap = rs.getInt("maximumGap");

            final int repaymentFrequencyTypeId = JdbcSupport.getInteger(rs, "repaymentPeriodFrequency");
            final EnumOptionData repaymentFrequencyType = LoanEnumerations.repaymentFrequencyType(repaymentFrequencyTypeId);

            final int amortizationTypeId = JdbcSupport.getInteger(rs, "amortizationMethod");
            final EnumOptionData amortizationType = LoanEnumerations.amortizationType(amortizationTypeId);
            final boolean isEqualAmortization = rs.getBoolean("isEqualAmortization");

            final Integer interestRateFrequencyTypeId = JdbcSupport.getInteger(rs, "interestRatePerPeriodFreq");
            final EnumOptionData interestRateFrequencyType = LoanEnumerations.interestRateFrequencyType(interestRateFrequencyTypeId);

            final int interestTypeId = JdbcSupport.getInteger(rs, "interestMethod");
            final EnumOptionData interestType = LoanEnumerations.interestType(interestTypeId);

            final int interestCalculationPeriodTypeId = JdbcSupport.getInteger(rs, "interestCalculationInPeriodMethod");
            final Boolean allowPartialPeriodInterestCalcualtion = rs.getBoolean("allowPartialPeriodInterestCalcualtion");
            final EnumOptionData interestCalculationPeriodType = LoanEnumerations
                    .interestCalculationPeriodType(interestCalculationPeriodTypeId);

            final boolean includeInBorrowerCycle = rs.getBoolean("includeInBorrowerCycle");
            final boolean useBorrowerCycle = rs.getBoolean("useBorrowerCycle");
            final LocalDate startDate = JdbcSupport.getLocalDate(rs, "startDate");
            final LocalDate closeDate = JdbcSupport.getLocalDate(rs, "closeDate");
            final Integer dueDaysForRepaymentEvent = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "dueDaysForRepaymentEvent");
            final Integer overDueDaysForRepaymentEvent = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "overDueDaysForRepaymentEvent");
            final boolean enableDownPayment = rs.getBoolean("enableDownPayment");
            final BigDecimal disbursedAmountPercentageForDownPayment = rs.getBigDecimal("disbursedAmountPercentageForDownPayment");
            final boolean enableAutoRepaymentForDownPayment = rs.getBoolean("enableAutoRepaymentForDownPayment");
            final Integer repaymentStartDateTypeId = JdbcSupport.getInteger(rs, "repaymentStartDateType");
            final EnumOptionData repaymentStartDateType = LoanEnumerations.repaymentStartDateType(repaymentStartDateTypeId);
            final boolean enableInstallmentLevelDelinquency = rs.getBoolean("enableInstallmentLevelDelinquency");

            String status = "";
            if (closeDate != null && DateUtils.isBeforeBusinessDate(closeDate)) {
                status = "loanProduct.inActive";
            } else {
                status = "loanProduct.active";
            }
            final String externalId = rs.getString("externalId");
            final Collection<LoanProductBorrowerCycleVariationData> principalVariationsForBorrowerCycle = new ArrayList<>();
            final Collection<LoanProductBorrowerCycleVariationData> interestRateVariationsForBorrowerCycle = new ArrayList<>();
            final Collection<LoanProductBorrowerCycleVariationData> numberOfRepaymentVariationsForBorrowerCycle = new ArrayList<>();
            if (this.borrowerCycleVariationDatas != null) {
                for (final LoanProductBorrowerCycleVariationData borrowerCycleVariationData : this.borrowerCycleVariationDatas) {
                    final LoanProductParamType loanProductParamType = borrowerCycleVariationData.getLoanProductParamType();
                    if (loanProductParamType.isParamTypePrincipal()) {
                        principalVariationsForBorrowerCycle.add(borrowerCycleVariationData);
                    } else if (loanProductParamType.isParamTypeInterestTate()) {
                        interestRateVariationsForBorrowerCycle.add(borrowerCycleVariationData);
                    } else if (loanProductParamType.isParamTypeRepayment()) {
                        numberOfRepaymentVariationsForBorrowerCycle.add(borrowerCycleVariationData);
                    }
                }
            }

            final Boolean multiDisburseLoan = rs.getBoolean("multiDisburseLoan");
            final Integer maxTrancheCount = rs.getInt("maxTrancheCount");
            final BigDecimal outstandingLoanBalance = rs.getBigDecimal("outstandingLoanBalance");
            final Boolean disallowExpectedDisbursements = rs.getBoolean("disallowExpectedDisbursements");
            final Boolean allowApprovedDisbursedAmountsOverApplied = rs.getBoolean("allowApprovedDisbursedAmountsOverApplied");
            final String overAppliedCalculationType = rs.getString("overAppliedCalculationType");
            final Integer overAppliedNumber = rs.getInt("overAppliedNumber");

            final int daysInMonth = JdbcSupport.getInteger(rs, "daysInMonth");
            final EnumOptionData daysInMonthType = CommonEnumerations.daysInMonthType(daysInMonth);
            final int daysInYear = JdbcSupport.getInteger(rs, "daysInYear");
            final EnumOptionData daysInYearType = CommonEnumerations.daysInYearType(daysInYear);
            final Integer installmentAmountInMultiplesOf = JdbcSupport.getInteger(rs, "installmentAmountInMultiplesOf");
            final boolean canDefineInstallmentAmount = rs.getBoolean("canDefineInstallmentAmount");
            final boolean isInterestRecalculationEnabled = rs.getBoolean("isInterestRecalculationEnabled");

            LoanProductInterestRecalculationData interestRecalculationData = null;
            if (isInterestRecalculationEnabled) {

                final Long lprId = JdbcSupport.getLong(rs, "lprId");
                final Long productId = JdbcSupport.getLong(rs, "productId");
                final int compoundTypeEnumValue = JdbcSupport.getInteger(rs, "compoundType");
                final EnumOptionData interestRecalculationCompoundingType = LoanEnumerations
                        .interestRecalculationCompoundingType(compoundTypeEnumValue);
                final int rescheduleStrategyEnumValue = JdbcSupport.getInteger(rs, "rescheduleStrategy");
                final EnumOptionData rescheduleStrategyType = LoanEnumerations.rescheduleStrategyType(rescheduleStrategyEnumValue);
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
                final Integer compoundingFrequencyEnumValue = JdbcSupport.getInteger(rs, "compoundingFrequencyTypeEnum");
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
                final boolean isArrearsBasedOnOriginalSchedule = rs.getBoolean("isArrearsBasedOnOriginalSchedule");
                final boolean isCompoundingToBePostedAsTransaction = rs.getBoolean("isCompoundingToBePostedAsTransaction");
                final int preCloseInterestCalculationStrategyEnumValue = JdbcSupport.getInteger(rs, "preCloseInterestCalculationStrategy");
                final EnumOptionData preCloseInterestCalculationStrategy = LoanEnumerations
                        .preCloseInterestCalculationStrategy(preCloseInterestCalculationStrategyEnumValue);
                final boolean allowCompoundingOnEod = rs.getBoolean("allowCompoundingOnEod");

                interestRecalculationData = new LoanProductInterestRecalculationData(lprId, productId, interestRecalculationCompoundingType,
                        rescheduleStrategyType, restFrequencyType, restFrequencyInterval, restFrequencyNthDayEnum, restFrequencyWeekDayEnum,
                        restFrequencyOnDay, compoundingFrequencyType, compoundingInterval, compoundingFrequencyNthDayEnum,
                        compoundingFrequencyWeekDayEnum, compoundingFrequencyOnDay, isArrearsBasedOnOriginalSchedule,
                        isCompoundingToBePostedAsTransaction, preCloseInterestCalculationStrategy, allowCompoundingOnEod);
            }

            final boolean amortization = rs.getBoolean("amortizationBoolean");
            final boolean interestMethod = rs.getBoolean("interestMethodConfigBoolean");
            final boolean transactionProcessingStrategy = rs.getBoolean("transactionProcessingStrategyBoolean");
            final boolean interestCalcPeriod = rs.getBoolean("interestCalcPeriodBoolean");
            final boolean arrearsTolerance = rs.getBoolean("arrearsToleranceBoolean");
            final boolean repaymentFrequency = rs.getBoolean("repaymentFrequencyBoolean");
            final boolean graceOnPrincipalAndInterest = rs.getBoolean("graceOnPrincipalAndInterestBoolean");
            final boolean graceOnArrearsAging = rs.getBoolean("graceOnArrearsAgingBoolean");

            LoanProductConfigurableAttributes allowAttributeOverrides = null;

            allowAttributeOverrides = new LoanProductConfigurableAttributes(amortization, interestMethod, transactionProcessingStrategy,
                    interestCalcPeriod, arrearsTolerance, repaymentFrequency, graceOnPrincipalAndInterest, graceOnArrearsAging);

            final boolean holdGuaranteeFunds = rs.getBoolean("holdGuaranteeFunds");
            LoanProductGuaranteeData loanProductGuaranteeData = null;
            if (holdGuaranteeFunds) {
                final Long lpgId = JdbcSupport.getLong(rs, "lpgId");
                final BigDecimal mandatoryGuarantee = rs.getBigDecimal("mandatoryGuarantee");
                final BigDecimal minimumGuaranteeFromOwnFunds = rs.getBigDecimal("minimumGuaranteeFromOwnFunds");
                final BigDecimal minimumGuaranteeFromGuarantor = rs.getBigDecimal("minimumGuaranteeFromGuarantor");
                loanProductGuaranteeData = LoanProductGuaranteeData.instance(lpgId, id, mandatoryGuarantee, minimumGuaranteeFromOwnFunds,
                        minimumGuaranteeFromGuarantor);
            }

            final BigDecimal principalThresholdForLastInstallment = rs.getBigDecimal("principalThresholdForLastInstallment");
            final BigDecimal fixedPrincipalPercentagePerInstallment = rs.getBigDecimal("fixedPrincipalPercentagePerInstallment");
            final boolean accountMovesOutOfNPAOnlyOnArrearsCompletion = rs.getBoolean("accountMovesOutOfNPAOnlyOnArrearsCompletion");
            final boolean syncExpectedWithDisbursementDate = rs.getBoolean("syncExpectedWithDisbursementDate");

            final boolean canUseForTopup = rs.getBoolean("canUseForTopup");
            final Collection<RateData> rateOptions = null;
            final boolean isRatesEnabled = false;

            // Delinquency Buckets
            final Long delinquencyBucketId = JdbcSupport.getLong(rs, "delinquencyBucketId");
            final String delinquencyBucketName = rs.getString("delinquencyBucketName");
            final DelinquencyBucketData delinquencyBucket = new DelinquencyBucketData(delinquencyBucketId, delinquencyBucketName,
                    new ArrayList<>());

            final String loanScheduleTypeStr = rs.getString("loanScheduleType");
            final LoanScheduleType loanScheduleType = LoanScheduleType.valueOf(loanScheduleTypeStr);
            final String loanScheduleProcessingTypeStr = rs.getString("loanScheduleProcessingType");
            final LoanScheduleProcessingType loanScheduleProcessingType = LoanScheduleProcessingType.valueOf(loanScheduleProcessingTypeStr);
            final Integer repaymentReschedulingTypeInt = JdbcSupport.getInteger(rs, "repaymentReschedulingType");
            final EnumOptionData repaymentReschedulingType = (repaymentReschedulingTypeInt == null) ? null
                    : WorkingDaysEnumerations.workingDaysStatusType(repaymentReschedulingTypeInt);
            final Integer maxClientInactivityPeriod = rs.getInt("maxClientInactivityPeriod");
            final BigDecimal overdueAmountForArrears = rs.getBigDecimal("overdueAmountForArrears");
            final boolean extendTermForMonthlyRepayments = rs.getBoolean("extendTermForMonthlyRepayments");
            final Long productTypeId = rs.getLong("productTypeId");

            CodeValueData productType = null;
            if (productTypeId != null) {
                final String productTypeValue = rs.getString("productTypeValue");
                productType = CodeValueData.instance(productTypeId, productTypeValue);
            }
            final boolean advance = rs.getBoolean("advance");
            final boolean requirePoints = rs.getBoolean("requirePoints");

            final Boolean customAllowCreateOrDisburse = rs.getBoolean("customAllowCreateOrDisburse");
            final Boolean customAllowCollections = rs.getBoolean("customAllowCollections");
            final Boolean customAllowDebitNote = rs.getBoolean("customAllowDebitNote");
            final Boolean customAllowCreditNote = rs.getBoolean("customAllowCreditNote");
            final Boolean customAllowForgiveness = rs.getBoolean("customAllowForgiveness");
            final Boolean customAllowReversalCancellation = rs.getBoolean("customAllowReversalCancellation");

            LoanProductData loanProductData = new LoanProductData(id, name, shortName, description, currency, principal, minPrincipal,
                    maxPrincipal, tolerance, numberOfRepayments, minNumberOfRepayments, maxNumberOfRepayments, repaymentEvery,
                    interestRatePerPeriod, minInterestRatePerPeriod, maxInterestRatePerPeriod, annualInterestRate, repaymentFrequencyType,
                    interestRateFrequencyType, amortizationType, interestType, interestCalculationPeriodType,
                    allowPartialPeriodInterestCalcualtion, fundId, fundName, transactionStrategyCode, transactionStrategyName,
                    graceOnPrincipalPayment, recurringMoratoriumOnPrincipalPeriods, graceOnInterestPayment, graceOnInterestCharged,
                    this.charges, accountingRuleType, includeInBorrowerCycle, useBorrowerCycle, startDate, closeDate, status, externalId,
                    principalVariationsForBorrowerCycle, interestRateVariationsForBorrowerCycle,
                    numberOfRepaymentVariationsForBorrowerCycle, multiDisburseLoan, maxTrancheCount, outstandingLoanBalance,
                    disallowExpectedDisbursements, allowApprovedDisbursedAmountsOverApplied, overAppliedCalculationType, overAppliedNumber,
                    graceOnArrearsAgeing, overdueDaysForNPA, daysInMonthType, daysInYearType, isInterestRecalculationEnabled,
                    interestRecalculationData, minimumDaysBetweenDisbursalAndFirstRepayment, holdGuaranteeFunds, loanProductGuaranteeData,
                    principalThresholdForLastInstallment, accountMovesOutOfNPAOnlyOnArrearsCompletion, canDefineInstallmentAmount,
                    installmentAmountInMultiplesOf, allowAttributeOverrides, isLinkedToFloatingInterestRates, floatingRateId,
                    floatingRateName, interestRateDifferential, minDifferentialLendingRate, defaultDifferentialLendingRate,
                    maxDifferentialLendingRate, isFloatingInterestRateCalculationAllowed, isVariableIntallmentsAllowed, minimumGap,
                    maximumGap, syncExpectedWithDisbursementDate, canUseForTopup, isEqualAmortization, rateOptions, this.rates,
                    isRatesEnabled, fixedPrincipalPercentagePerInstallment, delinquencyBucketOptions, delinquencyBucket,
                    dueDaysForRepaymentEvent, overDueDaysForRepaymentEvent, enableDownPayment, disbursedAmountPercentageForDownPayment,
                    enableAutoRepaymentForDownPayment, advancedPaymentData, creditAllocationData, repaymentStartDateType,
                    enableInstallmentLevelDelinquency, loanScheduleType.asEnumOptionData(), loanScheduleProcessingType.asEnumOptionData(),
                    repaymentReschedulingType);
            loanProductData.setMaxClientInactivityPeriod(maxClientInactivityPeriod);
            loanProductData.setOverdueAmountForArrearsConsideration(overdueAmountForArrears);
            loanProductData.setExtendTermForMonthlyRepayments(extendTermForMonthlyRepayments);
            loanProductData.setProductType(productType);
            loanProductData.setAdvance(advance);
            loanProductData.setRequirePoints(requirePoints);
            loanProductData.setCustomAllowCreateOrDisburse(customAllowCreateOrDisburse);
            loanProductData.setCustomAllowCollections(customAllowCollections);
            loanProductData.setCustomAllowDebitNote(customAllowDebitNote);
            loanProductData.setCustomAllowCreditNote(customAllowCreditNote);
            loanProductData.setCustomAllowForgiveness(customAllowForgiveness);
            loanProductData.setCustomAllowReversalCancellation(customAllowReversalCancellation);

            final Long interestRateId = rs.getLong("interestRateId");
            final String interestRateName = rs.getString("interestRateName");
            final BigDecimal interestCurrentRate = rs.getBigDecimal("interestCurrentRate");
            final LocalDate interestRateAppliedOnDate = JdbcSupport.getLocalDate(rs, "interestRateAppliedOnDate");
            final Boolean InterestRateActive = rs.getBoolean("InterestRateActive");
            final InterestRateData interestRateData = InterestRateData.builder().id(interestRateId).name(interestRateName)
                    .currentRate(interestCurrentRate).appliedOnDate(interestRateAppliedOnDate).active(InterestRateActive).build();
            loanProductData.setInterestRate(interestRateData);
            loanProductData.setGraceOnChargesPayment(graceOnChargesPayment);
            return loanProductData;
        }
    }

    private static final class LoanProductLookupMapper implements RowMapper<LoanProductData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;

        LoanProductLookupMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String schema() {
            return "lp.id as id, lp.name as name, lp.allow_multiple_disbursals as multiDisburseLoan from m_product_loan lp";
        }

        public String activeOnlySchema() {
            return schema() + " where (close_date is null or close_date >= " + sqlGenerator.currentBusinessDate() + ")";
        }

        public String productMixSchema() {
            return "lp.id as id, lp.name as name, lp.allow_multiple_disbursals as multiDisburseLoan FROM m_product_loan lp left join m_product_mix pm on pm.product_id=lp.id where lp.id not IN("
                    + "select lp.id from m_product_loan lp inner join m_product_mix pm on pm.product_id=lp.id)";
        }

        public String restrictedProductsSchema() {
            return "pm.restricted_product_id as id, rp.name as name, rp.allow_multiple_disbursals as multiDisburseLoan from m_product_mix pm join m_product_loan rp on rp.id = pm.restricted_product_id ";
        }

        public String derivedRestrictedProductsSchema() {
            return "pm.product_id as id, lp.name as name, lp.allow_multiple_disbursals as multiDisburseLoan from m_product_mix pm join m_product_loan lp on lp.id=pm.product_id";
        }

        @Override
        public LoanProductData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final Boolean multiDisburseLoan = rs.getBoolean("multiDisburseLoan");

            return LoanProductData.lookup(id, name, multiDisburseLoan);
        }
    }

    private static final class AdvancedPaymentDataMapper implements RowMapper<AdvancedPaymentData> {

        public String schema() {
            return "mpl.id, mpl.loan_schedule_type loanScheduleType, mpl.loan_schedule_processing_type loanScheduleProcessingType, transaction_type, allocation_types, future_installment_allocation_rule "
                    + "from m_loan_product_payment_allocation_rule mlpar " + "join m_product_loan mpl on mpl.id = mlpar.loan_product_id ";
        }

        @Override
        public AdvancedPaymentData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final String transactionType = rs.getString("transaction_type");
            final String allocationTypes = rs.getString("allocation_types");
            final String futureInstallmentAllocationRule = rs.getString("future_installment_allocation_rule");
            final String loanScheduleTypeStr = rs.getString("loanScheduleType");
            final LoanScheduleType loanScheduleType = LoanScheduleType.valueOf(loanScheduleTypeStr);
            final String loanScheduleProcessingTypeStr = rs.getString("loanScheduleProcessingType");
            final LoanScheduleProcessingType loanScheduleProcessingType = LoanScheduleProcessingType.valueOf(loanScheduleProcessingTypeStr);
            return new AdvancedPaymentData(transactionType, futureInstallmentAllocationRule,
                    convert(allocationTypes, loanScheduleType, loanScheduleProcessingType));
        }

        private List<PaymentAllocationOrder> convert(String allocationOrders, final LoanScheduleType loanScheduleType,
                final LoanScheduleProcessingType loanScheduleProcessingType) {
            String[] allocationRule = allocationOrders.split(",");
            String[] convertedAllocationRule = new String[7];
            int index = 0;
            if (loanScheduleType.equals(LoanScheduleType.PROGRESSIVE)
                    && loanScheduleProcessingType.equals(LoanScheduleProcessingType.VERTICAL)) {
                for (int i = 0; i < allocationRule.length; i += 3) {
                    String rule = allocationRule[i];
                    // PAST_DUE will always be the one captured here. Strip PAST_DUE from the string to get the
                    // Allocation Type
                    String allocationType = rule.substring(9);
                    convertedAllocationRule[index] = allocationType;
                    index++;
                }
            } else if (loanScheduleType.equals(LoanScheduleType.PROGRESSIVE)
                    && loanScheduleProcessingType.equals(LoanScheduleProcessingType.HORIZONTAL)) {
                for (int i = 0; i < 7; i++) {
                    String rule = allocationRule[i];
                    // PAST_DUE will always be the one captured here. Strip PAST_DUE from the string to get the
                    // Allocation Type
                    String allocationType = rule.substring(9);
                    convertedAllocationRule[index] = allocationType;
                    index++;
                }
            }
            AtomicInteger order = new AtomicInteger(1);
            return Arrays.stream(convertedAllocationRule) //
                    .map(s -> new PaymentAllocationOrder(s, order.getAndIncrement())) //
                    .toList();
        }

    }

    private static final class CreditAllocationDataMapper implements RowMapper<CreditAllocationData> {

        public String schema() {
            return "transaction_type, allocation_types from m_loan_product_credit_allocation_rule";
        }

        @Override
        public CreditAllocationData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final String transactionType = rs.getString("transaction_type");
            final String allocationTypes = rs.getString("allocation_types");
            return new CreditAllocationData(transactionType, convert(allocationTypes));
        }

        private List<CreditAllocationData.CreditAllocationOrder> convert(String allocationOrders) {
            String[] allocationRule = allocationOrders.split(",");
            AtomicInteger order = new AtomicInteger(1);
            return Arrays.stream(allocationRule) //
                    .map(s -> new CreditAllocationData.CreditAllocationOrder(s, order.getAndIncrement())) //
                    .toList();
        }

    }

    private static final class LoanProductBorrowerCycleMapper implements RowMapper<LoanProductBorrowerCycleVariationData> {

        public String schema() {
            return "bc.id as id,bc.borrower_cycle_number as cycleNumber,bc.value_condition as conditionType,bc.param_type as paramType,"
                    + "bc.default_value as defaultValue,bc.max_value as maxVal,bc.min_value as minVal "
                    + "from m_product_loan_variations_borrower_cycle bc";
        }

        @Override
        public LoanProductBorrowerCycleVariationData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
                throws SQLException {
            final Long id = rs.getLong("id");
            final Integer cycleNumber = JdbcSupport.getInteger(rs, "cycleNumber");
            final Integer conditionType = JdbcSupport.getInteger(rs, "conditionType");
            final EnumOptionData conditionTypeData = LoanEnumerations.loanCycleValueConditionType(conditionType);
            final Integer paramType = JdbcSupport.getInteger(rs, "paramType");
            final EnumOptionData paramTypeData = LoanEnumerations.loanCycleParamType(paramType);
            final BigDecimal defaultValue = rs.getBigDecimal("defaultValue");
            final BigDecimal maxValue = rs.getBigDecimal("maxVal");
            final BigDecimal minValue = rs.getBigDecimal("minVal");

            return new LoanProductBorrowerCycleVariationData(id, cycleNumber, paramTypeData, conditionTypeData, defaultValue, minValue,
                    maxValue);
        }

    }

    @Override
    public Collection<LoanProductData> retrieveAllLoanProductsForCurrency(String currencyCode) {
        this.context.authenticatedUser();

        final LoanProductMapper rm = new LoanProductMapper(null, null, null, null, null, null);

        String sql = "select " + rm.loanProductSchema() + " where lp.currency_code= ? ";

        // Check if branch specific products are enabled. If yes, fetch only
        // products mapped to current user's office
        String inClause = fineractEntityAccessUtil
                .getSQLWhereClauseForProductIDsForUserOffice_ifGlobalConfigEnabled(FineractEntityType.LOAN_PRODUCT);
        if (inClause != null && !inClause.trim().isEmpty()) {
            sql += " and id in (" + inClause + ") ";
        }

        return this.jdbcTemplate.query(sql, rm, currencyCode); // NOSONAR
    }

    @Override
    public Collection<LoanProductData> retrieveAvailableLoanProductsForMix() {

        this.context.authenticatedUser();

        final LoanProductLookupMapper rm = new LoanProductLookupMapper(sqlGenerator);

        String sql = "Select " + rm.productMixSchema();

        // Check if branch specific products are enabled. If yes, fetch only
        // products mapped to current user's office
        String inClause = fineractEntityAccessUtil
                .getSQLWhereClauseForProductIDsForUserOffice_ifGlobalConfigEnabled(FineractEntityType.LOAN_PRODUCT);
        if (inClause != null && !inClause.trim().isEmpty()) {
            sql += " and lp.id in ( " + inClause + " ) ";
        }

        return this.jdbcTemplate.query(sql, rm); // NOSONAR
    }

    @Override
    public Collection<LoanProductData> retrieveRestrictedProductsForMix(final Long productId) {

        this.context.authenticatedUser();

        final LoanProductLookupMapper rm = new LoanProductLookupMapper(sqlGenerator);

        String sql = "Select " + rm.restrictedProductsSchema() + " where pm.product_id=? ";
        // Check if branch specific products are enabled. If yes, fetch only
        // products mapped to current user's office
        String inClause1 = fineractEntityAccessUtil
                .getSQLWhereClauseForProductIDsForUserOffice_ifGlobalConfigEnabled(FineractEntityType.LOAN_PRODUCT);
        if (inClause1 != null && !inClause1.trim().isEmpty()) {
            sql += " and rp.id in ( " + inClause1 + " ) ";
        }

        sql += " UNION Select " + rm.derivedRestrictedProductsSchema() + " where pm.restricted_product_id=?";

        // Check if branch specific products are enabled. If yes, fetch only
        // products mapped to current user's office
        String inClause2 = fineractEntityAccessUtil
                .getSQLWhereClauseForProductIDsForUserOffice_ifGlobalConfigEnabled(FineractEntityType.LOAN_PRODUCT);
        if (inClause2 != null && !inClause2.trim().isEmpty()) {
            sql += " and lp.id in ( " + inClause2 + " ) ";
        }

        return this.jdbcTemplate.query(sql, rm, productId, productId); // NOSONAR
    }

    @Override
    public Collection<LoanProductData> retrieveAllowedProductsForMix(final Long productId) {

        this.context.authenticatedUser();

        final LoanProductLookupMapper rm = new LoanProductLookupMapper(sqlGenerator);

        String sql = "Select " + rm.schema() + " where ";

        // Check if branch specific products are enabled. If yes, fetch only
        // products mapped to current user's office
        String inClause = fineractEntityAccessUtil
                .getSQLWhereClauseForProductIDsForUserOffice_ifGlobalConfigEnabled(FineractEntityType.LOAN_PRODUCT);
        if (inClause != null && !inClause.trim().isEmpty()) {
            sql += " lp.id in ( " + inClause + " ) and ";
        }

        sql += "lp.id not in (" + "Select pm.restricted_product_id from m_product_mix pm where pm.product_id=? " + "UNION "
                + "Select pm.product_id from m_product_mix pm where pm.restricted_product_id=?)";

        return this.jdbcTemplate.query(sql, rm, productId, productId); // NOSONAR
    }

    @Override
    public LoanProductData retrieveLoanProductFloatingDetails(final Long loanProductId) {

        try {
            final LoanProductFloatingRateMapper rm = new LoanProductFloatingRateMapper();
            final String sql = "select " + rm.schema() + " where lp.id = ?";

            return this.jdbcTemplate.queryForObject(sql, rm, loanProductId); // NOSONAR

        } catch (final EmptyResultDataAccessException e) {
            throw new LoanProductNotFoundException(loanProductId, e);
        }
    }

    private static final class LoanProductFloatingRateMapper implements RowMapper<LoanProductData> {

        LoanProductFloatingRateMapper() {}

        public String schema() {
            return "lp.id as id,  lp.name as name," + "lp.is_linked_to_floating_interest_rates as isLinkedToFloatingInterestRates, "
                    + "lfr.floating_rates_id as floatingRateId, " + "fr.name as floatingRateName, "
                    + "lfr.interest_rate_differential as interestRateDifferential, "
                    + "lfr.min_differential_lending_rate as minDifferentialLendingRate, "
                    + "lfr.default_differential_lending_rate as defaultDifferentialLendingRate, "
                    + "lfr.max_differential_lending_rate as maxDifferentialLendingRate, "
                    + "lfr.is_floating_interest_rate_calculation_allowed as isFloatingInterestRateCalculationAllowed "
                    + " from m_product_loan lp " + " left join m_product_loan_floating_rates as lfr on lfr.loan_product_id = lp.id "
                    + " left join m_floating_rates as fr on lfr.floating_rates_id = fr.id ";
        }

        @Override
        public LoanProductData mapRow(@NotNull final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = JdbcSupport.getLong(rs, "id");
            final String name = rs.getString("name");

            final boolean isLinkedToFloatingInterestRates = rs.getBoolean("isLinkedToFloatingInterestRates");
            final Integer floatingRateId = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "floatingRateId");
            final String floatingRateName = rs.getString("floatingRateName");
            final BigDecimal interestRateDifferential = rs.getBigDecimal("interestRateDifferential");
            final BigDecimal minDifferentialLendingRate = rs.getBigDecimal("minDifferentialLendingRate");
            final BigDecimal defaultDifferentialLendingRate = rs.getBigDecimal("defaultDifferentialLendingRate");
            final BigDecimal maxDifferentialLendingRate = rs.getBigDecimal("maxDifferentialLendingRate");
            final boolean isFloatingInterestRateCalculationAllowed = rs.getBoolean("isFloatingInterestRateCalculationAllowed");

            return LoanProductData.loanProductWithFloatingRates(id, name, isLinkedToFloatingInterestRates, floatingRateId, floatingRateName,
                    interestRateDifferential, minDifferentialLendingRate, defaultDifferentialLendingRate, maxDifferentialLendingRate,
                    isFloatingInterestRateCalculationAllowed);
        }
    }

    @Override
    public MaximumCreditRateConfigurationData retrieveMaximumCreditRateConfigurationData() {
        final MaximumRateMapper rm = new MaximumRateMapper();
        final String sql = "SELECT " + rm.schema() + " WHERE mcrc.id IS NOT NULL";
        return this.jdbcTemplate.queryForObject(sql, rm, new Object[] {});
    }

    private static final class MaximumRateMapper implements RowMapper<MaximumCreditRateConfigurationData> {

        public String schema() {
            return """
                    mcrc.id AS id,
                    mcrc.ea_rate AS "eaRate",
                    mcrc.annual_nominal_rate AS "annualNominalRate",
                    mcrc.monthly_nominal_rate AS "monthlyNominalRate",
                    mcrc.daily_nominal_rate AS "dailyNominalRate",
                    mcrc.current_interest_rate AS "currentInterestRate",
                    mcrc.overdue_interest_rate AS "overdueInterestRate",
                    mcrc.appliedon_date AS "appliedOnDate",
                    CONCAT(ma.firstname, ' ', ma.lastname) AS "appliedByUsername"
                    FROM m_maximum_credit_rate_configuration mcrc
                    LEFT OUTER JOIN m_appuser ma ON ma.id = mcrc.appliedon_userid
                    """;
        }

        @Override
        public MaximumCreditRateConfigurationData mapRow(@NotNull final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
                throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final BigDecimal eaRate = rs.getBigDecimal("eaRate");
            final String appliedByUsername = rs.getString("appliedByUsername");
            final BigDecimal annualNominalRate = rs.getBigDecimal("annualNominalRate");
            final BigDecimal monthlyNominalRate = rs.getBigDecimal("monthlyNominalRate");
            final BigDecimal dailyNominalRate = rs.getBigDecimal("dailyNominalRate");
            final BigDecimal currentInterestRate = rs.getBigDecimal("currentInterestRate");
            final BigDecimal overdueInterestRate = rs.getBigDecimal("overdueInterestRate");
            final LocalDate appliedOnDate = JdbcSupport.getLocalDate(rs, "appliedOnDate");
            return MaximumCreditRateConfigurationData.builder().id(id).appliedByUsername(appliedByUsername).eaRate(eaRate)
                    .annualNominalRate(annualNominalRate).monthlyNominalRate(monthlyNominalRate).dailyNominalRate(dailyNominalRate)
                    .currentInterestRate(currentInterestRate).overdueInterestRate(overdueInterestRate).appliedOnDate(appliedOnDate).build();
        }
    }

    @Override
    public Collection<Pair<Long, Integer>> retrieveLoanProductClientInActivityConfiguration() {
        final String query = "Select id,max_client_inactivity_period from m_product_loan where max_client_inactivity_period is not null";
        return this.jdbcTemplate.query(query, (rs, rowNum) -> {
            final Long id = rs.getLong("id");
            final Integer maxClientInactivityPeriod = rs.getInt("max_client_inactivity_period");
            return Pair.of(id, maxClientInactivityPeriod);
        });
    }

    @Override
    public Collection<Long> retrieveAnyProductsThatCanBlockCredit(final Long clientId, String productType) {

        final String query = "select mpl.id from m_client mc " + "inner join m_loan ml on ml.client_id = mc.id  "
                + "inner join m_product_loan mpl on mpl.id = ml.product_id  "
                + "inner join m_loan_arrears_aging mla on ml.id = mla.loan_id  " + "where  " + "mc.id = ? " + "and mpl.product_type in (  "
                + " Select m_code_value.id from m_code_value inner join m_code on m_code.id = m_code_value.code_id  "
                + " where m_code.code_name = 'ProductType'  " + " and m_code_value.code_value = ?)";

        return jdbcTemplate.queryForList(query, Long.class, clientId, productType);
    }

    @Override
    public AdvanceQuotaConfigurationData retrieveAdvanceQuotaConfigurationData() {
        final AdvanceQuotaMapper rm = new AdvanceQuotaMapper();
        final String sql = "SELECT " + rm.schema() + " WHERE maqc.id IS NOT NULL";
        return this.jdbcTemplate.queryForObject(sql, rm, new Object[] {});
    }

    private static final class AdvanceQuotaMapper implements RowMapper<AdvanceQuotaConfigurationData> {

        public String schema() {
            return """
                    maqc.id AS id,
                    maqc.percentage_value AS "percentageValue",
                    maqc.is_enabled AS "enabled",
                    maqc.modifiedon_date AS "modifiedOnDate",
                    CONCAT(ma.firstname, ' ', ma.lastname) AS "modifiedByUsername"
                    FROM m_advance_quota_configuration maqc
                    LEFT OUTER JOIN m_appuser ma ON ma.id = maqc.modifiedon_userid
                    """;
        }

        @Override
        public AdvanceQuotaConfigurationData mapRow(@NotNull final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
                throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final BigDecimal percentageValue = rs.getBigDecimal("percentageValue");
            final String modifiedByUsername = rs.getString("modifiedByUsername");
            final LocalDate modifiedOnDate = JdbcSupport.getLocalDate(rs, "modifiedOnDate");
            final Boolean enabled = rs.getBoolean("enabled");
            return AdvanceQuotaConfigurationData.builder().id(id).modifiedByUsername(modifiedByUsername).percentageValue(percentageValue)
                    .modifiedOnDate(modifiedOnDate).enabled(enabled).build();
        }
    }
}
