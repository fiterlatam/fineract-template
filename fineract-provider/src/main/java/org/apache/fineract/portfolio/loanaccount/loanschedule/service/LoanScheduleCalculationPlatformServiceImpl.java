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
package org.apache.fineract.portfolio.loanaccount.loanschedule.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonQuery;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.*;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.LoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanApplicationTerms;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import org.apache.fineract.portfolio.loanaccount.serialization.CalculateLoanScheduleQueryFromApiJsonHelper;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanApplicationCommandFromApiJsonHelper;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanUtilService;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.apache.fineract.portfolio.loanproduct.serialization.LoanProductDataValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoanScheduleCalculationPlatformServiceImpl implements LoanScheduleCalculationPlatformService {

    private final CalculateLoanScheduleQueryFromApiJsonHelper fromApiJsonDeserializer;
    private final LoanScheduleAssembler loanScheduleAssembler;
    private final FromJsonHelper fromJsonHelper;
    private final LoanProductRepository loanProductRepository;
    private final LoanProductDataValidator loanProductCommandFromApiJsonDeserializer;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanApplicationCommandFromApiJsonHelper loanApiJsonDeserializer;
    private final LoanAssembler loanAssembler;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;
    private final ConfigurationDomainService configurationDomainService;
    private final CurrencyReadPlatformService currencyReadPlatformService;
    private final LoanUtilService loanUtilService;

    @Autowired
    public LoanScheduleCalculationPlatformServiceImpl(final CalculateLoanScheduleQueryFromApiJsonHelper fromApiJsonDeserializer,
            final LoanScheduleAssembler loanScheduleAssembler, final FromJsonHelper fromJsonHelper,
            final LoanProductRepository loanProductRepository, final LoanProductDataValidator loanProductCommandFromApiJsonDeserializer,
            final LoanReadPlatformService loanReadPlatformService, final LoanApplicationCommandFromApiJsonHelper loanApiJsonDeserializer,
            final LoanAssembler loanAssembler,
            final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory,
            final ConfigurationDomainService configurationDomainService, final CurrencyReadPlatformService currencyReadPlatformService,
            final LoanUtilService loanUtilService) {
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.loanScheduleAssembler = loanScheduleAssembler;
        this.fromJsonHelper = fromJsonHelper;
        this.loanProductRepository = loanProductRepository;
        this.loanProductCommandFromApiJsonDeserializer = loanProductCommandFromApiJsonDeserializer;
        this.loanReadPlatformService = loanReadPlatformService;
        this.loanApiJsonDeserializer = loanApiJsonDeserializer;
        this.loanAssembler = loanAssembler;
        this.loanRepaymentScheduleTransactionProcessorFactory = loanRepaymentScheduleTransactionProcessorFactory;
        this.configurationDomainService = configurationDomainService;
        this.currencyReadPlatformService = currencyReadPlatformService;
        this.loanUtilService = loanUtilService;
    }

    @Override
    public LoanScheduleModel calculateLoanSchedule(final JsonQuery query, Boolean validateParams) {

        /***
         * TODO: Vishwas, this is probably not required, test and remove the same
         **/
        final Long productId = this.fromJsonHelper.extractLongNamed("productId", query.parsedJson());
        final LoanProduct loanProduct = this.loanProductRepository.findById(productId)
                .orElseThrow(() -> new LoanProductNotFoundException(productId));

        if (validateParams) {
            boolean isMeetingMandatoryForJLGLoans = configurationDomainService.isMeetingMandatoryForJLGLoans();
            this.loanApiJsonDeserializer.validateForCreate(query.json(), isMeetingMandatoryForJLGLoans, loanProduct);
        }
        this.fromApiJsonDeserializer.validate(query.json());

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");

        if (loanProduct.useBorrowerCycle()) {
            final Long clientId = this.fromJsonHelper.extractLongNamed("clientId", query.parsedJson());
            final Long groupId = this.fromJsonHelper.extractLongNamed("groupId", query.parsedJson());
            Integer cycleNumber = 0;
            if (clientId != null) {
                cycleNumber = this.loanReadPlatformService.retriveLoanCounter(clientId, loanProduct.getId());
            } else if (groupId != null) {
                cycleNumber = this.loanReadPlatformService.retriveLoanCounter(groupId, AccountType.GROUP.getValue(), loanProduct.getId());
            }
            this.loanProductCommandFromApiJsonDeserializer.validateMinMaxConstraints(query.parsedJson(), baseDataValidator, loanProduct,
                    cycleNumber);
        } else {
            this.loanProductCommandFromApiJsonDeserializer.validateMinMaxConstraints(query.parsedJson(), baseDataValidator, loanProduct);
        }
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }

        return this.loanScheduleAssembler.assembleLoanScheduleFrom(query.parsedJson(), loanProduct);
    }

    @Override
    public void updateFutureSchedule(LoanScheduleData loanScheduleData, final Long loanId) {

        final Loan loan = this.loanAssembler.assembleFrom(loanId);

        LocalDate today = DateUtils.getBusinessLocalDate();
        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = loanRepaymentScheduleTransactionProcessorFactory
                .determineProcessor(loan.transactionProcessingStrategy());

        if (!loan.repaymentScheduleDetail().isInterestRecalculationEnabled() || loan.isNpa() || loan.isChargedOff()
                || !loan.getStatus().isActive()
                || !loanRepaymentScheduleTransactionProcessor.isInterestFirstRepaymentScheduleTransactionProcessor()) {
            return;
        }

        if (loan.loanProduct().isMultiDisburseLoan()) {
            BigDecimal disbursedAmount = loan.getDisbursedAmount();
            BigDecimal principalRepaid = loan.getLoanSummary().getTotalPrincipalRepaid();
            BigDecimal principalWrittenOff = loan.getLoanSummary().getTotalPrincipalWrittenOff();
            if (disbursedAmount.subtract(principalWrittenOff).subtract(principalRepaid).compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
        }
        MonetaryCurrency currency = loan.getCurrency();
        Money totalPrincipal = Money.zero(currency);
        final List<LoanSchedulePeriodData> futureInstallments = new ArrayList<>();
        List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
            if (currentInstallment.isNotFullyPaidOff()) {
                if (!DateUtils.isAfter(currentInstallment.getDueDate(), today)) {
                    totalPrincipal = totalPrincipal.plus(currentInstallment.getPrincipalOutstanding(currency));
                }
            }
        }
        LoanApplicationTerms loanApplicationTerms = constructLoanApplicationTerms(loan);
        LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = this.loanScheduleAssembler.calculatePrepaymentAmount(currency,
                today, loanApplicationTerms, loan, loan.getOfficeId(), loanRepaymentScheduleTransactionProcessor);
        Money totalAmount = totalPrincipal.plus(loanRepaymentScheduleInstallment.getFeeChargesOutstanding(currency))
                .plus(loanRepaymentScheduleInstallment.getPenaltyChargesOutstanding(currency));
        Money interestDue = Money.zero(currency);
        if (loanRepaymentScheduleInstallment.isInterestDue(currency)) {
            interestDue = loanRepaymentScheduleInstallment.getInterestOutstanding(currency);
            totalAmount = totalAmount.plus(interestDue);
        }
        boolean isNewPaymentRequired = loanRepaymentScheduleInstallment.isInterestDue(currency) || totalPrincipal.isGreaterThanZero();

        LoanScheduleModel model = this.loanScheduleAssembler.assembleForInterestRecalculation(loanApplicationTerms, loan.getOfficeId(),
                loan, loanRepaymentScheduleTransactionProcessor, loan.fetchInterestRecalculateFromDate());
        LoanScheduleData scheduleDate = model.toData();
        Collection<LoanSchedulePeriodData> periodDatas = scheduleDate.getPeriods();
        for (LoanSchedulePeriodData periodData : periodDatas) {
            if (isNewPaymentRequired && !DateUtils.isBefore(periodData.getDueDate(), today)) {
                LoanSchedulePeriodData loanSchedulePeriodData = LoanSchedulePeriodData.repaymentOnlyPeriod(periodData.getPeriod(),
                        periodData.getFromDate(), periodData.getDueDate(), totalPrincipal.getAmount(),
                        periodData.getPrincipalLoanBalanceOutstanding(), interestDue.getAmount(),
                        loanRepaymentScheduleInstallment.getFeeChargesCharged(currency).getAmount(),
                        loanRepaymentScheduleInstallment.getPenaltyChargesCharged(currency).getAmount(), totalAmount.getAmount(),
                        totalPrincipal.plus(interestDue).getAmount());
                futureInstallments.add(loanSchedulePeriodData);
                isNewPaymentRequired = false;
            } else if (DateUtils.isAfter(periodData.getDueDate(), today)) {
                futureInstallments.add(periodData);
            }

        }
        loanScheduleData.updateFuturePeriods(futureInstallments);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanScheduleData generateLoanScheduleForVariableInstallmentRequest(Long loanId, final String json) {
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        this.loanScheduleAssembler.assempleVariableScheduleFrom(loan, json);
        return constructLoanScheduleData(loan);
    }

    private LoanScheduleData constructLoanScheduleData(Loan loan) {
        Collection<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
        final List<LoanSchedulePeriodData> installmentData = new ArrayList<>();
        final MonetaryCurrency currency = loan.getCurrency();
        Money outstanding = loan.getPrincipal();

        List<LoanDisbursementDetails> disbursementDetails = new ArrayList<>();
        if (loan.isMultiDisburmentLoan()) {
            disbursementDetails = loan.getDisbursementDetails();
            outstanding = outstanding.zero();
        }
        Money principal = outstanding;
        Iterator<LoanDisbursementDetails> disbursementItr = disbursementDetails.iterator();
        LoanDisbursementDetails loanDisbursementDetails = null;
        if (disbursementItr.hasNext()) {
            loanDisbursementDetails = disbursementItr.next();
        }

        Money totalInterest = principal.zero();
        Money totalCharge = principal.zero();
        Money totalPenalty = principal.zero();

        for (LoanRepaymentScheduleInstallment installment : installments) {
            if (loanDisbursementDetails != null
                    && !DateUtils.isAfter(loanDisbursementDetails.expectedDisbursementDateAsLocalDate(), installment.getDueDate())) {
                outstanding = outstanding.plus(loanDisbursementDetails.principal());
                principal = principal.plus(loanDisbursementDetails.principal());
                if (disbursementItr.hasNext()) {
                    loanDisbursementDetails = disbursementItr.next();
                } else {
                    loanDisbursementDetails = null;
                }
            }
            outstanding = outstanding.minus(installment.getPrincipal(currency));
            LoanSchedulePeriodData loanSchedulePeriodData = LoanSchedulePeriodData.repaymentOnlyPeriod(installment.getInstallmentNumber(),
                    installment.getFromDate(), installment.getDueDate(), installment.getPrincipal(currency).getAmount(),
                    outstanding.getAmount(), installment.getInterestCharged(currency).getAmount(),
                    installment.getFeeChargesCharged(currency).getAmount(), installment.getPenaltyChargesCharged(currency).getAmount(),
                    installment.getDue(currency).getAmount(), installment.getTotalPrincipalAndInterest(currency).getAmount());
            installmentData.add(loanSchedulePeriodData);
            totalInterest = totalInterest.plus(installment.getInterestCharged(currency));
            totalCharge = totalCharge.plus(installment.getFeeChargesCharged(currency));
            totalPenalty = totalPenalty.plus(installment.getPenaltyChargesCharged(currency));
        }

        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(currency.getCode());

        LoanScheduleData scheduleData = new LoanScheduleData(currencyData, installmentData,
                loan.getLoanRepaymentScheduleDetail().getNumberOfRepayments(), principal.getAmount(), principal.getAmount(),
                totalInterest.getAmount(), totalCharge.getAmount(), totalPenalty.getAmount(),
                principal.plus(totalCharge).plus(totalInterest).plus(totalPenalty).getAmount());

        return scheduleData;
    }

    private LoanApplicationTerms constructLoanApplicationTerms(final Loan loan) {
        final LocalDate recalculateFrom = null;
        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
        LoanApplicationTerms loanApplicationTerms = loan.constructLoanApplicationTerms(scheduleGeneratorDTO);
        return loanApplicationTerms;
    }

    @Override
    public void getFeeChargesDetail(LoanScheduleData loanScheduleData, final Long loanId) {

        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        BigDecimal totalMandatoryInsuranceCharged = BigDecimal.ZERO;
        BigDecimal totalVoluntaryInsuranceCharged = BigDecimal.ZERO;
        BigDecimal totalAvalCharged = BigDecimal.ZERO;
        BigDecimal totalHonorariosCharged = BigDecimal.ZERO;

        for (LoanRepaymentScheduleInstallment repaymentScheduleInstallment : loan.getRepaymentScheduleInstallments()) {
            // Calculate individual charge amounts

            Collection<LoanCharge> mandatoryInsuranceCharges = loan.getLoanCharges().stream().filter(LoanCharge::isMandatoryInsurance)
                    .toList();
            Collection<LoanCharge> voluntaryInsuranceCharges = loan.getLoanCharges().stream().filter(LoanCharge::isVoluntaryInsurance)
                    .toList();
            Collection<LoanCharge> avalCharges = loan.getLoanCharges().stream().filter(LoanCharge::isAvalCharge).toList();
            Collection<LoanCharge> honorariosCharges = loan.getLoanCharges().stream().filter(LoanCharge::isFlatHono).toList();
            Collection<LoanCharge> ivaCharges = loan.getLoanCharges().stream().filter(LoanCharge::isCustomPercentageBasedOfAnotherCharge)
                    .toList();

            BigDecimal mandatoryInsuranceAmount = mandatoryInsuranceCharges.stream().flatMap(lic -> lic.installmentCharges().stream())
                    .filter(lc -> Objects.equals(repaymentScheduleInstallment.getInstallmentNumber(),
                            lc.getInstallment().getInstallmentNumber()))
                    .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal voluntaryInsuranceAmount = voluntaryInsuranceCharges.stream().flatMap(lic -> lic.installmentCharges().stream())
                    .filter(lc -> Objects.equals(repaymentScheduleInstallment.getInstallmentNumber(),
                            lc.getInstallment().getInstallmentNumber()))
                    .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avalAmount = avalCharges.stream().flatMap(lic -> lic.installmentCharges().stream()).filter(
                    lc -> Objects.equals(repaymentScheduleInstallment.getInstallmentNumber(), lc.getInstallment().getInstallmentNumber()))
                    .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal honorariosAmount = honorariosCharges.stream().flatMap(lic -> lic.installmentCharges().stream()).filter(
                    lc -> Objects.equals(repaymentScheduleInstallment.getInstallmentNumber(), lc.getInstallment().getInstallmentNumber()))
                    .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate term Charge
            BigDecimal mandatoryInsuranceTermChargeAmount = ivaCharges.stream()
                    .filter(lc -> mandatoryInsuranceCharges.stream()
                            .anyMatch(mic -> mic.getCharge().getId().equals(lc.getCharge().getParentChargeId())))
                    .flatMap(lic -> lic.installmentCharges().stream())
                    .filter(lc -> Objects.equals(repaymentScheduleInstallment.getInstallmentNumber(),
                            lc.getInstallment().getInstallmentNumber()))
                    .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal voluntaryInsuranceTermChargeAmount = ivaCharges.stream()
                    .filter(lc -> voluntaryInsuranceCharges.stream()
                            .anyMatch(mic -> mic.getCharge().getId().equals(lc.getCharge().getParentChargeId())))
                    .flatMap(lic -> lic.installmentCharges().stream())
                    .filter(lc -> Objects.equals(repaymentScheduleInstallment.getInstallmentNumber(),
                            lc.getInstallment().getInstallmentNumber()))
                    .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avalTermChargeAmount = ivaCharges.stream()
                    .filter(lc -> avalCharges.stream().anyMatch(mic -> mic.getCharge().getId().equals(lc.getCharge().getParentChargeId())))
                    .flatMap(lic -> lic.installmentCharges().stream())
                    .filter(lc -> Objects.equals(repaymentScheduleInstallment.getInstallmentNumber(),
                            lc.getInstallment().getInstallmentNumber()))
                    .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal honorariosTermChargeAmount = ivaCharges.stream()
                    .filter(lc -> honorariosCharges.stream()
                            .anyMatch(mic -> mic.getCharge().getId().equals(lc.getCharge().getParentChargeId())))
                    .flatMap(lic -> lic.installmentCharges().stream())
                    .filter(lc -> Objects.equals(repaymentScheduleInstallment.getInstallmentNumber(),
                            lc.getInstallment().getInstallmentNumber()))
                    .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            mandatoryInsuranceAmount = mandatoryInsuranceAmount.add(mandatoryInsuranceTermChargeAmount);
            voluntaryInsuranceAmount = voluntaryInsuranceAmount.add(voluntaryInsuranceTermChargeAmount);
            avalAmount = avalAmount.add(avalTermChargeAmount);
            honorariosAmount = honorariosAmount.add(honorariosTermChargeAmount);

            totalMandatoryInsuranceCharged = totalMandatoryInsuranceCharged.add(mandatoryInsuranceAmount);
            totalVoluntaryInsuranceCharged = totalVoluntaryInsuranceCharged.add(voluntaryInsuranceAmount);
            totalAvalCharged = totalAvalCharged.add(avalAmount);
            totalHonorariosCharged = totalHonorariosCharged.add(honorariosAmount);

            for (LoanSchedulePeriodData periodData : loanScheduleData.getPeriods()) {
                if (periodData.getPeriod() != null && periodData.getPeriod().equals(repaymentScheduleInstallment.getInstallmentNumber())) {
                    periodData.setAvalDue(avalAmount);
                    periodData.setHonorariosDue(honorariosAmount);
                    periodData.setMandatoryInsuranceDue(mandatoryInsuranceAmount);
                    periodData.setVoluntaryInsuranceDue(voluntaryInsuranceAmount);
                    break;
                }
            }
        }

        loanScheduleData.setTotalMandatoryInsuranceCharged(totalMandatoryInsuranceCharged);
        loanScheduleData.setTotalVoluntaryInsuranceCharged(totalVoluntaryInsuranceCharged);
        loanScheduleData.setTotalAvalCharged(totalAvalCharged);
        loanScheduleData.setTotalHonorariosCharged(totalHonorariosCharged);
    }

}
