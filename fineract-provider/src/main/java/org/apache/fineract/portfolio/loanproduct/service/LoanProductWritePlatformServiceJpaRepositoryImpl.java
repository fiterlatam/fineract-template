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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import jakarta.persistence.PersistenceException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.accounting.producttoaccountmapping.service.ProductToGLAccountMappingWritePlatformService;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelRepository;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelType;
import org.apache.fineract.custom.infrastructure.channel.exception.ChannelNotFoundException;
import org.apache.fineract.custom.insfrastructure.channel.domain.Channel;
import org.apache.fineract.custom.portfolio.loanproduct.data.SubChannelLoanProductData;
import org.apache.fineract.custom.portfolio.loanproduct.domain.SubChannelLoanProduct;
import org.apache.fineract.custom.portfolio.loanproduct.domain.SubChannelLoanProductRepository;
import org.apache.fineract.custom.portfolio.loanproduct.service.SubChannelLoanProductReadWritePlatformService;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityAccessType;
import org.apache.fineract.infrastructure.entityaccess.service.FineractEntityAccessUtil;
import org.apache.fineract.infrastructure.event.business.domain.loan.product.LoanProductCreateBusinessEvent;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.monetary.exception.InvalidCurrencyException;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.delinquency.domain.DelinquencyBucket;
import org.apache.fineract.portfolio.delinquency.domain.DelinquencyBucketRepository;
import org.apache.fineract.portfolio.floatingrates.domain.FloatingRate;
import org.apache.fineract.portfolio.floatingrates.domain.FloatingRateRepositoryWrapper;
import org.apache.fineract.portfolio.fund.domain.Fund;
import org.apache.fineract.portfolio.fund.domain.FundRepository;
import org.apache.fineract.portfolio.fund.exception.FundNotFoundException;
import org.apache.fineract.portfolio.interestrates.data.InterestRateData;
import org.apache.fineract.portfolio.interestrates.domain.InterestRate;
import org.apache.fineract.portfolio.interestrates.domain.InterestRateRepository;
import org.apache.fineract.portfolio.interestrates.exception.InterestRateException;
import org.apache.fineract.portfolio.interestrates.service.InterestRateReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleTransactionProcessorFactory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.AprCalculator;
import org.apache.fineract.portfolio.loanproduct.LoanProductConstants;
import org.apache.fineract.portfolio.loanproduct.data.ClientCupoData;
import org.apache.fineract.portfolio.loanproduct.data.MaximumCreditRateConfigurationData;
import org.apache.fineract.portfolio.loanproduct.domain.AdvanceQuotaConfiguration;
import org.apache.fineract.portfolio.loanproduct.domain.AdvanceQuotaRepository;
import org.apache.fineract.portfolio.loanproduct.domain.AdvancedPaymentAllocationsJsonParser;
import org.apache.fineract.portfolio.loanproduct.domain.CreditAllocationsJsonParser;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductCreditAllocationRule;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductPaymentAllocationRule;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.domain.MaximumCreditRateConfiguration;
import org.apache.fineract.portfolio.loanproduct.domain.MaximumRateRepository;
import org.apache.fineract.portfolio.loanproduct.exception.AdvanceQuotaExceptions;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductCannotBeModifiedDueToNonClosedLoansException;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductDateException;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.apache.fineract.portfolio.loanproduct.exception.MaximumLegalRateExceptions;
import org.apache.fineract.portfolio.loanproduct.serialization.LoanProductDataValidator;
import org.apache.fineract.portfolio.rate.domain.Rate;
import org.apache.fineract.portfolio.rate.domain.RateRepositoryWrapper;
import org.apache.fineract.useradministration.domain.AppUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class LoanProductWritePlatformServiceJpaRepositoryImpl implements LoanProductWritePlatformService {

    private final PlatformSecurityContext context;
    private final LoanProductDataValidator fromApiJsonDeserializer;
    private final LoanProductRepository loanProductRepository;
    private final MaximumRateRepository maximumRateRepository;
    private final AdvanceQuotaRepository advanceQuotaRepository;
    private final AprCalculator aprCalculator;
    private final FundRepository fundRepository;
    private final ChargeRepositoryWrapper chargeRepository;
    private final RateRepositoryWrapper rateRepository;
    private final ProductToGLAccountMappingWritePlatformService accountMappingWritePlatformService;
    private final FineractEntityAccessUtil fineractEntityAccessUtil;
    private final FloatingRateRepositoryWrapper floatingRateRepository;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final DelinquencyBucketRepository delinquencyBucketRepository;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;
    private final AdvancedPaymentAllocationsJsonParser advancedPaymentJsonParser;
    private final CreditAllocationsJsonParser creditAllocationsJsonParser;
    private final LoanProductPaymentAllocationRuleMerger loanProductPaymentAllocationRuleMerger = new LoanProductPaymentAllocationRuleMerger();
    private final LoanProductCreditAllocationRuleMerger loanProductCreditAllocationRuleMerger = new LoanProductCreditAllocationRuleMerger();
    private final LoanProductReadPlatformService loanProductReadPlatformService;
    private final CodeValueRepositoryWrapper codeValueRepository;
    private final SubChannelLoanProductReadWritePlatformService subChannelLoanProductReadWritePlatformService;
    private final SubChannelLoanProductRepository subChannelLoanProductRepository;
    private final JdbcTemplate jdbcTemplate;
    private final InterestRateRepository interestRateRepository;
    private final InterestRateReadPlatformService interestRateReadPlatformService;
    private final ChannelRepository channelRepository;

    @Transactional
    @Override
    public CommandProcessingResult createLoanProduct(final JsonCommand command) {

        try {

            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command);
            validateInputDates(command);

            final Fund fund = findFundByIdIfProvided(command.longValueOfParameterNamed("fundId"));
            final Long interestRateId = command.longValueOfParameterNamed("interestRateId");
            final InterestRate interestRate = this.interestRateRepository.findById(interestRateId)
                    .orElseThrow(() -> new InterestRateException(interestRateId));
            if (!interestRate.isActive()) {
                throw new InterestRateException(interestRateId);
            }
            final String loanTransactionProcessingStrategyCode = command.stringValueOfParameterNamed("transactionProcessingStrategyCode");

            final String currencyCode = command.stringValueOfParameterNamed("currencyCode");
            final List<Charge> charges = assembleListOfProductCharges(command, currencyCode);
            final List<Rate> rates = assembleListOfProductRates(command);
            final List<LoanProductPaymentAllocationRule> loanProductPaymentAllocationRules = advancedPaymentJsonParser
                    .assembleLoanProductPaymentAllocationRules(command, loanTransactionProcessingStrategyCode);
            final List<LoanProductCreditAllocationRule> loanProductCreditAllocationRules = creditAllocationsJsonParser
                    .assembleLoanProductCreditAllocationRules(command, loanTransactionProcessingStrategyCode);
            FloatingRate floatingRate = null;
            if (command.parameterExists("floatingRatesId")) {
                floatingRate = this.floatingRateRepository
                        .findOneWithNotFoundDetection(command.longValueOfParameterNamed("floatingRatesId"));
            }
            final CodeValue productType = findProductTypeByIdIfProvided(
                    command.longValueOfParameterNamed(LoanProductConstants.PRODUCT_TYPE));
            final LoanProduct loanProduct = LoanProduct.assembleFromJson(fund, loanTransactionProcessingStrategyCode, charges, command,
                    floatingRate, rates, loanProductPaymentAllocationRules, loanProductCreditAllocationRules, interestRate);
            this.validateMaximumInterestRate(loanProduct);
            loanProduct.updateLoanProductInRelatedClasses();
            loanProduct.setTransactionProcessingStrategyName(
                    loanRepaymentScheduleTransactionProcessorFactory.determineProcessor(loanTransactionProcessingStrategyCode).getName());
            loanProduct.setProductType(productType);

            if (command.parameterExists("delinquencyBucketId")) {
                loanProduct
                        .setDelinquencyBucket(findDelinquencyBucketIdIfProvided(command.longValueOfParameterNamed("delinquencyBucketId")));
            }

            populateProductCustomAllowance(command, loanProduct);
            loanProduct.setInterestRate(interestRate);

            if (command.parameterExists("repaymentChannels")) {
                final List<Channel> repaymentChannels = assembleListOfRepaymentChannels(command);
                loanProduct.setRepaymentChannels(repaymentChannels);
            }

            this.loanProductRepository.save(loanProduct);

            // save accounting mappings
            this.accountMappingWritePlatformService.createLoanProductToGLAccountMapping(loanProduct.getId(), command);
            // check if the office specific products are enabled. If yes, then
            // save this savings product against a specific office
            // i.e. this savings product is specific for this office.
            fineractEntityAccessUtil.checkConfigurationAndAddProductResrictionsForUserOffice(
                    FineractEntityAccessType.OFFICE_ACCESS_TO_LOAN_PRODUCTS, loanProduct.getId());

            businessEventNotifierService.notifyPostBusinessEvent(new LoanProductCreateBusinessEvent(loanProduct));

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(loanProduct.getId()) //
                    .build();

        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }

    }

    private void populateProductCustomAllowance(JsonCommand command, LoanProduct loanProduct) {
        if (command.parameterExists(LoanProductConstants.CUSTOM_ALLOW_CREATE_OR_DISBUSE_PARAM_NAME)) {
            Boolean res = command.booleanPrimitiveValueOfParameterNamed(LoanProductConstants.CUSTOM_ALLOW_CREATE_OR_DISBUSE_PARAM_NAME);
            loanProduct.setCustomAllowCreateOrDisburse(res);
        }

        if (command.parameterExists(LoanProductConstants.CUSTOM_ALLOW_COLLECTIONS_PARAM_NAME)) {
            Boolean res = command.booleanPrimitiveValueOfParameterNamed(LoanProductConstants.CUSTOM_ALLOW_COLLECTIONS_PARAM_NAME);
            loanProduct.setCustomAllowCollections(res);
        }

        if (command.parameterExists(LoanProductConstants.CUSTOM_ALLOW_CREDIT_NOTE_PARAM_NAME)) {
            Boolean res = command.booleanPrimitiveValueOfParameterNamed(LoanProductConstants.CUSTOM_ALLOW_CREDIT_NOTE_PARAM_NAME);
            loanProduct.setCustomAllowCreditNote(res);
        }

        if (command.parameterExists(LoanProductConstants.CUSTOM_ALLOW_DEBIT_NOTE_PARAM_NAME)) {
            Boolean res = command.booleanPrimitiveValueOfParameterNamed(LoanProductConstants.CUSTOM_ALLOW_DEBIT_NOTE_PARAM_NAME);
            loanProduct.setCustomAllowDebitNote(res);
        }

        if (command.parameterExists(LoanProductConstants.CUSTOM_ALLOW_FORGIVENESS_PARAM_NAME)) {
            Boolean res = command.booleanPrimitiveValueOfParameterNamed(LoanProductConstants.CUSTOM_ALLOW_FORGIVENESS_PARAM_NAME);
            loanProduct.setCustomAllowForgiveness(res);
        }

        if (command.parameterExists(LoanProductConstants.CUSTOM_ALLOW_REVERSAL_OR_CANCELATION_PARAM_NAME)) {
            Boolean res = command
                    .booleanPrimitiveValueOfParameterNamed(LoanProductConstants.CUSTOM_ALLOW_REVERSAL_OR_CANCELATION_PARAM_NAME);
            loanProduct.setCustomAllowReversalCancellation(res);
        }

        this.loanProductRepository.saveAndFlush(loanProduct);

        JsonArray jsonArraySubChannelLoanProductMapper = command.arrayOfParameterNamed("subChannelLoanProductMapper");
        Gson gson = new Gson();
        Type listType = new TypeToken<List<SubChannelLoanProductData>>() {}.getType();
        List<SubChannelLoanProductData> dataList = gson.fromJson(jsonArraySubChannelLoanProductMapper, listType);

        // Retrieve the list with current mapping
        List<SubChannelLoanProductData> subChannelLoanProductDataList = subChannelLoanProductReadWritePlatformService
                .findAllByProductId(loanProduct.getId());

        // Exclude the ones that are not in the new list
        subChannelLoanProductRepository.findByLoanProductId(loanProduct.getId()).forEach(subChannelLoanProduct -> {
            if (Boolean.FALSE.equals(loanProduct.getCustomAllowCollections())
                    || dataList.stream().noneMatch(data -> data.getSubChannelId().equals(subChannelLoanProduct.getSubChannelId()))) {
                subChannelLoanProductRepository.delete(subChannelLoanProduct);
            }
        });

        // Add new ones (negative ids)
        if (Objects.nonNull(dataList)) {
            dataList.stream().filter(data -> data.getId() < 0).forEach(data -> {
                SubChannelLoanProduct subChannelLoanProduct = SubChannelLoanProduct.builder().loanProductId(loanProduct.getId())
                        .subChannelId(data.getSubChannelId()).build();
                subChannelLoanProductRepository.save(subChannelLoanProduct);
            });
        }

    }

    private Fund findFundByIdIfProvided(final Long fundId) {
        Fund fund = null;
        if (fundId != null) {
            fund = this.fundRepository.findById(fundId).orElseThrow(() -> new FundNotFoundException(fundId));
        }
        return fund;
    }

    private DelinquencyBucket findDelinquencyBucketIdIfProvided(final Long delinquencyBucketId) {
        DelinquencyBucket delinquencyBucket = null;
        if (delinquencyBucketId != null) {
            delinquencyBucket = delinquencyBucketRepository.findById(delinquencyBucketId)
                    .orElseThrow(() -> new FundNotFoundException(delinquencyBucketId));
        }
        return delinquencyBucket;
    }

    private CodeValue findProductTypeByIdIfProvided(final Long productTypeId) {
        CodeValue productType = null;
        if (productTypeId != null) {
            productType = this.codeValueRepository.findOneWithNotFoundDetection(productTypeId);
        }
        return productType;
    }

    @Transactional
    @Override
    public CommandProcessingResult updateLoanProduct(final Long loanProductId, final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            final LoanProduct product = this.loanProductRepository.findById(loanProductId)
                    .orElseThrow(() -> new LoanProductNotFoundException(loanProductId));

            this.fromApiJsonDeserializer.validateForUpdate(command, product);
            validateInputDates(command);

            if (anyChangeInCriticalFloatingRateLinkedParams(command, product)
                    && this.loanRepositoryWrapper.doNonClosedLoanAccountsExistForProduct(product.getId())) {
                throw new LoanProductCannotBeModifiedDueToNonClosedLoansException(product.getId());
            }

            FloatingRate floatingRate = null;
            if (command.parameterExists("floatingRatesId")) {
                floatingRate = this.floatingRateRepository
                        .findOneWithNotFoundDetection(command.longValueOfParameterNamed("floatingRatesId"));
            }

            final Map<String, Object> changes = product.update(command, this.aprCalculator, floatingRate);

            if (changes.containsKey("interestRateId")) {
                final Long interestRateId = (Long) changes.get("interestRateId");
                final InterestRate interestRate = this.interestRateRepository.findById(interestRateId)
                        .orElseThrow(() -> new InterestRateException(interestRateId));
                product.setInterestRate(interestRate);
                final BigDecimal currentRate = interestRate.getCurrentRate();
                final PeriodFrequencyType interestFrequencyType = PeriodFrequencyType.YEARS;
                product.loanProductMinMaxConstraints().setMinNominalInterestRatePerPeriod(currentRate);
                product.loanProductMinMaxConstraints().setMaxNominalInterestRatePerPeriod(currentRate);
                product.getLoanProductRelatedDetail().setNominalInterestRatePerPeriod(currentRate);
                product.getLoanProductRelatedDetail().setAnnualNominalInterestRate(currentRate);
                product.getLoanProductRelatedDetail().updateInterestPeriodFrequencyType(interestFrequencyType);
            }

            if (changes.containsKey("fundId")) {
                final Long fundId = (Long) changes.get("fundId");
                final Fund fund = findFundByIdIfProvided(fundId);
                product.update(fund);
            }

            if (changes.containsKey("repaymentChannels")) {
                final List<Channel> repaymentChannels = assembleListOfRepaymentChannels(command);
                final boolean updated = product.updateChannels(repaymentChannels);
                if (!updated) {
                    changes.remove("repaymentChannels");
                }
            }

            if (changes.containsKey(LoanProductConstants.PRODUCT_TYPE)) {
                final Long parameterTypeId = (Long) changes.get(LoanProductConstants.PRODUCT_TYPE);
                final CodeValue productType = this.codeValueRepository.findOneWithNotFoundDetection(parameterTypeId);
                product.setProductType(productType);
            }

            if (changes.containsKey("delinquencyBucketId")) {
                product.setDelinquencyBucket(findDelinquencyBucketIdIfProvided((Long) changes.get("delinquencyBucketId")));
            }

            if (changes.containsKey("transactionProcessingStrategyCode")) {
                final String transactionProcessingStrategyCode = (String) changes.get("transactionProcessingStrategyCode");
                final String transactionProcessingStrategyName = loanRepaymentScheduleTransactionProcessorFactory
                        .determineProcessor(transactionProcessingStrategyCode).getName();
                product.setTransactionProcessingStrategyCode(transactionProcessingStrategyCode);
                product.setTransactionProcessingStrategyName(transactionProcessingStrategyName);
            }

            if (changes.containsKey("charges")) {
                final List<Charge> productCharges = assembleListOfProductCharges(command, product.getCurrency().getCode());
                final boolean updated = product.update(productCharges);
                if (!updated) {
                    changes.remove("charges");
                }
            }

            if (changes.containsKey("paymentAllocation")) {
                final List<LoanProductPaymentAllocationRule> loanProductPaymentAllocationRules = advancedPaymentJsonParser
                        .assembleLoanProductPaymentAllocationRules(command, product.getTransactionProcessingStrategyCode());
                loanProductPaymentAllocationRules.forEach(lppar -> lppar.setLoanProduct(product));
                final boolean updated = loanProductPaymentAllocationRuleMerger.updateProductPaymentAllocationRules(product,
                        loanProductPaymentAllocationRules);
                if (!updated) {
                    changes.remove("paymentAllocation");
                }
            }

            if (changes.containsKey("creditAllocation")) {
                final List<LoanProductCreditAllocationRule> loanProductCreditAllocationRules = creditAllocationsJsonParser
                        .assembleLoanProductCreditAllocationRules(command, product.getTransactionProcessingStrategyCode());
                loanProductCreditAllocationRules.forEach(lpcar -> lpcar.setLoanProduct(product));
                final boolean updated = loanProductCreditAllocationRuleMerger.updateCreditAllocationRules(product,
                        loanProductCreditAllocationRules);
                if (!updated) {
                    changes.remove("creditAllocation");
                }
            }

            // accounting related changes
            final boolean accountingTypeChanged = changes.containsKey("accountingRule");
            final Map<String, Object> accountingMappingChanges = this.accountMappingWritePlatformService
                    .updateLoanProductToGLAccountMapping(product.getId(), command, accountingTypeChanged, product.getAccountingType());
            changes.putAll(accountingMappingChanges);

            if (changes.containsKey(LoanProductConstants.RATES_PARAM_NAME)) {
                final List<Rate> productRates = assembleListOfProductRates(command);
                final boolean updated = product.updateRates(productRates);
                if (!updated) {
                    changes.remove(LoanProductConstants.RATES_PARAM_NAME);
                }
            }
            this.validateMaximumInterestRate(product);

            populateProductCustomAllowance(command, product);

            if (!changes.isEmpty()) {
                product.validateLoanProductPreSave();
                this.loanProductRepository.saveAndFlush(product);
            }

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(loanProductId) //
                    .with(changes) //
                    .build();

        } catch (final DataIntegrityViolationException | JpaSystemException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.resourceResult(-1L);
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }

    }

    private void validateMaximumInterestRate(final LoanProduct loanProduct) {
        final BigDecimal maxNominalInterestRatePerPeriod = loanProduct.getMaxNominalInterestRatePerPeriod();
        final BigDecimal minNominalInterestRatePerPeriod = loanProduct.getMinNominalInterestRatePerPeriod();
        final BigDecimal nominalInterestRatePerPeriod = loanProduct.getNominalInterestRatePerPeriod();
        final PeriodFrequencyType interestPeriodFrequencyType = loanProduct.getInterestPeriodFrequencyType();
        final MaximumCreditRateConfigurationData maximumCreditRateConfigurationData = this.loanProductReadPlatformService
                .retrieveMaximumCreditRateConfigurationData();
        switch (interestPeriodFrequencyType) {
            case MONTHS -> {
                final BigDecimal monthlyNominalRate = maximumCreditRateConfigurationData.getMonthlyNominalRate();
                if (maxNominalInterestRatePerPeriod != null && maxNominalInterestRatePerPeriod.compareTo(monthlyNominalRate) > 0) {
                    throw new PlatformDataIntegrityException("error.msg.loanproduct.max.nominal.interest.rate.per.period",
                            "Maximum nominal interest rate per period must be less than or equal to maximum legal rate for monthly interest period frequency",
                            "maxNominalInterestRatePerPeriod", maxNominalInterestRatePerPeriod, monthlyNominalRate);
                }
                if (minNominalInterestRatePerPeriod != null && minNominalInterestRatePerPeriod.compareTo(monthlyNominalRate) > 0) {
                    throw new PlatformDataIntegrityException("error.msg.loanproduct.min.nominal.interest.rate.per.period",
                            "Minimum nominal interest rate per period must be greater than or equal to maximum legal rate  for monthly interest period frequency",
                            "minNominalInterestRatePerPeriod", minNominalInterestRatePerPeriod, monthlyNominalRate);
                }
                if (nominalInterestRatePerPeriod != null && nominalInterestRatePerPeriod.compareTo(monthlyNominalRate) > 0) {
                    throw new PlatformDataIntegrityException("error.msg.loanproduct.nominal.interest.rate.per.period",
                            "Nominal interest rate per period must be greater than or equal to maximum legal rate  for monthly interest period frequency",
                            "nominalInterestRatePerPeriod", nominalInterestRatePerPeriod, monthlyNominalRate);
                }
            }

            case YEARS -> {
                final BigDecimal annualNominalRate = maximumCreditRateConfigurationData.getAnnualNominalRate();
                if (maxNominalInterestRatePerPeriod != null && maxNominalInterestRatePerPeriod.compareTo(annualNominalRate) > 0) {
                    throw new PlatformDataIntegrityException("error.msg.loanproduct.max.nominal.interest.rate.per.period",
                            "Maximum nominal interest rate per period must be less than or equal to maximum legal rate for monthly interest period frequency",
                            "maxNominalInterestRatePerPeriod", maxNominalInterestRatePerPeriod, annualNominalRate);
                }
                if (minNominalInterestRatePerPeriod != null && minNominalInterestRatePerPeriod.compareTo(annualNominalRate) > 0) {
                    throw new PlatformDataIntegrityException("error.msg.loanproduct.min.nominal.interest.rate.per.period",
                            "Minimum nominal interest rate per period must be greater than or equal to maximum legal rate  for monthly interest period frequency",
                            "minNominalInterestRatePerPeriod", minNominalInterestRatePerPeriod, annualNominalRate);
                }
                if (nominalInterestRatePerPeriod != null && nominalInterestRatePerPeriod.compareTo(annualNominalRate) > 0) {
                    throw new PlatformDataIntegrityException("error.msg.loanproduct.nominal.interest.rate.per.period",
                            "Nominal interest rate per period must be greater than or equal to maximum legal rate  for monthly interest period frequency",
                            "nominalInterestRatePerPeriod", nominalInterestRatePerPeriod, annualNominalRate);
                }
            }
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateMaximumRate(final JsonCommand command) {

        try {
            final AppUser appliedBy = this.context.authenticatedUser();
            final List<MaximumCreditRateConfiguration> maximumCreditRateConfigurations = this.maximumRateRepository.findAll();
            if (CollectionUtils.isEmpty(maximumCreditRateConfigurations)) {
                throw new MaximumLegalRateExceptions();
            }
            final MaximumCreditRateConfiguration maximumCreditRateConfiguration = maximumCreditRateConfigurations.get(0);
            final Long id = maximumCreditRateConfiguration.getId();
            this.fromApiJsonDeserializer.validateMaximumRateForUpdate(command);
            final BigDecimal eaRate = command.bigDecimalValueOfParameterNamed("eaRate");
            final Map<String, Object> changes = maximumCreditRateConfiguration.update(command);
            final BigDecimal annualNominalRate = maximumCreditRateConfiguration.getAnnualNominalRate();
            final SearchParameters searchParameters = SearchParameters.builder().currentRate(annualNominalRate).build();
            List<InterestRateData> interestRateDataList = this.interestRateReadPlatformService.retrieveBySearchParams(searchParameters);
            if (CollectionUtils.isNotEmpty(interestRateDataList)) {
                throw new GeneralPlatformDomainRuleException("error.msg.interest.rates.exist.above.this.rate",
                        "The change cannot be made since there is interest rates above the new rate, you must first modify the rates",
                        annualNominalRate);
            }
            maximumCreditRateConfiguration.setAppliedBy(appliedBy);
            this.maximumRateRepository.saveAndFlush(maximumCreditRateConfiguration);
            return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(id).withEaRate(eaRate).with(changes)
                    .build();
        } catch (final DataIntegrityViolationException | JpaSystemException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.resourceResult(-1L);
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }

    }

    @Transactional
    @Override
    public CommandProcessingResult updateAdvanceQuota(final JsonCommand command) {
        try {
            final AppUser modifiedBy = this.context.authenticatedUser();
            final List<AdvanceQuotaConfiguration> advanceQuotaConfigurations = this.advanceQuotaRepository.findAll();
            if (CollectionUtils.isEmpty(advanceQuotaConfigurations)) {
                throw new AdvanceQuotaExceptions();
            }
            final AdvanceQuotaConfiguration advanceQuotaConfiguration = advanceQuotaConfigurations.get(0);
            final Long id = advanceQuotaConfiguration.getId();
            this.fromApiJsonDeserializer.validateAdvanceQuotaForUpdate(command);
            final Map<String, Object> changes = advanceQuotaConfiguration.update(command);
            advanceQuotaConfiguration.setModifiedBy(modifiedBy);
            advanceQuotaConfiguration.setModifiedOnDate(DateUtils.getLocalDateOfTenant());
            final ClientCupoMapper rowMapper = new ClientCupoMapper();
            final String sql = "SELECT " + rowMapper.schema();
            List<ClientCupoData> resultList = this.jdbcTemplate.query(sql, rowMapper, new Object[] {});
            final List<Long> clientIDs = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(resultList)) {
                final BigDecimal percentageValue = advanceQuotaConfiguration.getPercentageValue();
                for (ClientCupoData clientCupoData : resultList) {
                    final BigDecimal maximumAvailableCupo = percentageValue.multiply(clientCupoData.getCupoAmount())
                            .divide(BigDecimal.valueOf(100L), MoneyHelper.getRoundingMode());
                    if (clientCupoData.getTotalOutstandingAmount().compareTo(maximumAvailableCupo) > 0) {
                        clientIDs.add(clientCupoData.getClientId());
                    }
                }
            }
            if (!clientIDs.isEmpty()) {
                throw new AdvanceQuotaExceptions(clientIDs);
            }
            this.advanceQuotaRepository.saveAndFlush(advanceQuotaConfiguration);
            return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(id).with(changes).build();

        } catch (final DataIntegrityViolationException | JpaSystemException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.resourceResult(-1L);
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }

    }

    private static final class ClientCupoMapper implements RowMapper<ClientCupoData> {

        public String schema() {
            return """
                        mc.id AS clientId,
                        COALESCE(SUM(ml.principal_outstanding_derived), 0) AS "totalOutstandingAmount",
                        COALESCE(ccp."Cupo aprobado", cce."Cupo") AS cupo
                        FROM m_loan ml
                        INNER JOIN m_client mc ON mc.id = ml.client_id
                        INNER JOIN m_product_loan mpl ON mpl.id = ml.product_id
                        LEFT JOIN campos_cliente_empresas cce ON cce.client_id = mc.id
                        LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                        WHERE ml.loan_status_id = 300 AND mpl.is_advance = TRUE
                        GROUP BY mc.id, ccp."Cupo aprobado", cce."Cupo"
                    """;
        }

        @Override
        public ClientCupoData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            final Long clientId = JdbcSupport.getLong(rs, "clientId");
            final BigDecimal totalOutstandingAmount = rs.getBigDecimal("totalOutstandingAmount");
            final BigDecimal cupo = rs.getBigDecimal("cupo");
            return new ClientCupoData(clientId, totalOutstandingAmount, cupo);
        }
    }

    private boolean anyChangeInCriticalFloatingRateLinkedParams(JsonCommand command, LoanProduct product) {
        final boolean isChangeFromFloatingToFlatOrViceVersa = command.isChangeInBooleanParameterNamed("isLinkedToFloatingInterestRates",
                product.isLinkedToFloatingInterestRate());
        final boolean isChangeInCriticalFloatingRateParams = product.getFloatingRates() != null
                && (command.isChangeInLongParameterNamed("floatingRatesId", product.getFloatingRates().getFloatingRate().getId())
                        || command.isChangeInBigDecimalParameterNamed("interestRateDifferential",
                                product.getFloatingRates().getInterestRateDifferential()));
        return isChangeFromFloatingToFlatOrViceVersa || isChangeInCriticalFloatingRateParams;
    }

    private List<Charge> assembleListOfProductCharges(final JsonCommand command, final String currencyCode) {

        final List<Charge> charges = new ArrayList<>();

        String loanProductCurrencyCode = command.stringValueOfParameterNamed("currencyCode");
        if (loanProductCurrencyCode == null) {
            loanProductCurrencyCode = currencyCode;
        }

        if (command.parameterExists("charges")) {
            final JsonArray chargesArray = command.arrayOfParameterNamed("charges");
            if (chargesArray != null) {
                for (int i = 0; i < chargesArray.size(); i++) {

                    final JsonObject jsonObject = chargesArray.get(i).getAsJsonObject();
                    if (jsonObject.has("id")) {
                        final Long id = jsonObject.get("id").getAsLong();

                        final Charge charge = this.chargeRepository.findOneWithNotFoundDetection(id);

                        if (!loanProductCurrencyCode.equals(charge.getCurrencyCode())) {
                            final String errorMessage = "Charge and Loan Product must have the same currency.";
                            throw new InvalidCurrencyException("charge", "attach.to.loan.product", errorMessage);
                        }
                        charges.add(charge);
                    }
                }
            }
        }

        return charges;
    }

    private List<Channel> assembleListOfRepaymentChannels(final JsonCommand command) {
        final List<Channel> repaymentChannels = new ArrayList<>();
        if (command.parameterExists("repaymentChannels")) {
            final JsonArray channelsArray = command.arrayOfParameterNamed("repaymentChannels");
            if (channelsArray != null) {
                for (int i = 0; i < channelsArray.size(); i++) {
                    final JsonObject jsonObject = channelsArray.get(i).getAsJsonObject();
                    if (jsonObject.has("id")) {
                        final Long channelId = jsonObject.get("id").getAsLong();
                        final Channel repaymentChannel = this.channelRepository.findById(channelId)
                                .orElseThrow(() -> new ChannelNotFoundException(channelId));
                        if (!repaymentChannel.getActive()) {
                            throw new GeneralPlatformDomainRuleException("error.msg.channel.inactive", "Channel is inactive.", channelId);
                        }
                        if (!ChannelType.REPAYMENT.getValue().equals(repaymentChannel.getChannelType())) {
                            throw new GeneralPlatformDomainRuleException("error.msg.channel.not.repayment",
                                    "Channel is not a repayment channel.", channelId);
                        }
                        repaymentChannels.add(repaymentChannel);
                    }
                }
            }
        }
        return repaymentChannels;
    }

    private List<Rate> assembleListOfProductRates(final JsonCommand command) {

        final List<Rate> rates = new ArrayList<>();

        if (command.parameterExists("rates")) {
            final JsonArray ratesArray = command.arrayOfParameterNamed("rates");
            if (ratesArray != null) {
                List<Long> idList = new ArrayList<>();
                for (int i = 0; i < ratesArray.size(); i++) {

                    final JsonObject jsonObject = ratesArray.get(i).getAsJsonObject();
                    if (jsonObject.has("id")) {
                        idList.add(jsonObject.get("id").getAsLong());
                    }
                }
                rates.addAll(this.rateRepository.findMultipleWithNotFoundDetection(idList));
            }
        }

        return rates;
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("'external_id'")) {

            final String externalId = command.stringValueOfParameterNamed("externalId");
            throw new PlatformDataIntegrityException("error.msg.product.loan.duplicate.externalId",
                    "Loan Product with externalId `" + externalId + "` already exists", "externalId", externalId, realCause);
        } else if (realCause.getMessage().contains("'unq_name'")) {

            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.product.loan.duplicate.name",
                    "Loan product with name `" + name + "` already exists", "name", name, realCause);
        } else if (realCause.getMessage().contains("'unq_short_name'") || containsDuplicateShortnameErrorForPostgreSQL(realCause)
                || containsDuplicateShortnameErrorForMySQL(realCause)) {

            final String shortName = command.stringValueOfParameterNamed("shortName");
            throw new PlatformDataIntegrityException("error.msg.product.loan.duplicate.short.name",
                    "Loan product with short name `" + shortName + "` already exists", "shortName", shortName, realCause);
        } else if (realCause.getMessage().contains("Duplicate entry")) {
            throw new PlatformDataIntegrityException("error.msg.product.loan.duplicate.charge",
                    "Loan product may only have one charge of each type.`", "charges", realCause);
        }

        logAsErrorUnexpectedDataIntegrityException(dve);
        throw ErrorHandler.getMappable(dve, "error.msg.product.loan.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.", null, realCause);
    }

    private static boolean containsDuplicateShortnameErrorForPostgreSQL(Throwable realCause) {
        return realCause.getMessage().contains("m_product_loan_short_name_key");
    }

    private static boolean containsDuplicateShortnameErrorForMySQL(Throwable realCause) {
        return (realCause.getMessage().contains("short_name") && realCause.getMessage().toLowerCase().contains("duplicate"));
    }

    private void validateInputDates(final JsonCommand command) {
        final LocalDate startDate = command.localDateValueOfParameterNamed("startDate");
        final LocalDate closeDate = command.localDateValueOfParameterNamed("closeDate");

        if (closeDate != null && DateUtils.isBefore(closeDate, startDate)) {
            throw new LoanProductDateException(startDate.toString(), closeDate.toString());
        }
    }

    private void logAsErrorUnexpectedDataIntegrityException(final Exception dve) {
        log.error("Error occurred.", dve);
    }
}
