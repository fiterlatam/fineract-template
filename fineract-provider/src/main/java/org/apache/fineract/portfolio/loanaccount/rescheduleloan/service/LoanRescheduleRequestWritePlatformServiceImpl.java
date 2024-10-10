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

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationProperty;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanRescheduledDueAdjustScheduleBusinessEvent;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.account.service.AccountTransfersWritePlatformService;
import org.apache.fineract.portfolio.delinquency.service.DelinquencyReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.CollectionData;
import org.apache.fineract.portfolio.loanaccount.data.HolidayDetailDTO;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.*;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.LoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleDTO;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.*;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.RescheduleLoansApiConstants;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.LoanRescheduleRequestDataValidator;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequestRepository;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.exception.LoanRescheduleRequestNotFoundException;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualTransactionBusinessEventService;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanUtilService;
import org.apache.fineract.portfolio.loanaccount.service.ReplayedTransactionBusinessEventService;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanRescheduleRequestWritePlatformServiceImpl implements LoanRescheduleRequestWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(LoanRescheduleRequestWritePlatformServiceImpl.class);

    private final CodeValueRepositoryWrapper codeValueRepositoryWrapper;
    private final PlatformSecurityContext platformSecurityContext;
    private final LoanRescheduleRequestDataValidator loanRescheduleRequestDataValidator;
    private final LoanRescheduleRequestRepository loanRescheduleRequestRepository;
    private final LoanRepaymentScheduleHistoryRepository loanRepaymentScheduleHistoryRepository;
    private final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanAssembler loanAssembler;
    private final LoanUtilService loanUtilService;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;
    private final LoanScheduleGeneratorFactory loanScheduleFactory;
    private final LoanSummaryWrapper loanSummaryWrapper;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private static final DefaultScheduledDateGenerator DEFAULT_SCHEDULED_DATE_GENERATOR = new DefaultScheduledDateGenerator();
    private final LoanAccountDomainService loanAccountDomainService;
    private final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository;
    private final ReplayedTransactionBusinessEventService replayedTransactionBusinessEventService;
    private final LoanAccrualTransactionBusinessEventService loanAccrualTransactionBusinessEventService;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final ExternalIdFactory externalIdFactory;
    private final GlobalConfigurationRepositoryWrapper globalConfigurationRepository;
    private final LoanReadPlatformService loanReadPlatformService;
    private final DelinquencyReadPlatformService delinquencyReadPlatformService;
    private final LoanInstalmentChargeRepository loanInstalmentChargeRepository;

    /**
     * create a new instance of the LoanRescheduleRequest object from the JsonCommand object and persist
     *
     * @return CommandProcessingResult object
     **/
    @Override
    @Transactional
    public CommandProcessingResult create(JsonCommand jsonCommand) {

        try {
            // get the loan id from the JsonCommand object
            final Long loanId = jsonCommand.longValueOfParameterNamed(RescheduleLoansApiConstants.loanIdParamName);

            // use the loan id to get a Loan entity object
            final Loan loan = this.loanAssembler.assembleFrom(loanId);

            if (loan.isChargedOff()) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.is.charged.off",
                        "Loan: " + loanId + " reschedule installment is not allowed. Loan Account is Charged-off", loanId);
            }

            // validate the request in the JsonCommand object passed as
            // parameter
            this.loanRescheduleRequestDataValidator.validateForCreateAction(jsonCommand, loan);

            // get the reschedule reason code value id from the JsonCommand
            // object
            final Long rescheduleReasonId = jsonCommand.longValueOfParameterNamed(RescheduleLoansApiConstants.rescheduleReasonIdParamName);

            // use the reschedule reason code value id to get a CodeValue entity
            // object
            final CodeValue rescheduleReasonCodeValue = this.codeValueRepositoryWrapper.findOneWithNotFoundDetection(rescheduleReasonId);

            // get the grace on principal integer value from the JsonCommand
            // object
            final Integer graceOnPrincipal = jsonCommand
                    .integerValueOfParameterNamed(RescheduleLoansApiConstants.graceOnPrincipalParamName);

            // get the grace on interest integer value from the JsonCommand
            // object
            final Integer graceOnInterest = jsonCommand.integerValueOfParameterNamed(RescheduleLoansApiConstants.graceOnInterestParamName);

            // get the extra terms to be added at the end of the new schedule
            // from the JsonCommand object
            final Integer extraTerms = jsonCommand.integerValueOfParameterNamed(RescheduleLoansApiConstants.extraTermsParamName);

            final Integer rediferirTerms = jsonCommand.integerValueOfParameterNamed(RescheduleLoansApiConstants.rediferirTermsParamName);

            // get the new interest rate that would be applied to the new loan
            // schedule
            final BigDecimal interestRate = jsonCommand
                    .bigDecimalValueOfParameterNamed(RescheduleLoansApiConstants.newInterestRateParamName);

            // get the reschedule reason comment text from the JsonCommand
            // object
            final String rescheduleReasonComment = jsonCommand
                    .stringValueOfParameterNamed(RescheduleLoansApiConstants.rescheduleReasonCommentParamName);

            // get the recalculate interest option
            final Boolean recalculateInterest = jsonCommand
                    .booleanObjectValueOfParameterNamed(RescheduleLoansApiConstants.recalculateInterestParamName);

            final LocalDate endDate = jsonCommand.localDateValueOfParameterNamed(RescheduleLoansApiConstants.endDateParamName);
            final BigDecimal emi = jsonCommand.bigDecimalValueOfParameterNamed(RescheduleLoansApiConstants.emiParamName);

            // initialize set the value to null
            LocalDate submittedOnDate = null;

            // check if the parameter is in the JsonCommand object
            if (jsonCommand.hasParameter(RescheduleLoansApiConstants.submittedOnDateParamName)) {
                // create a LocalDate object from the "submittedOnDate" Date
                // string
                submittedOnDate = jsonCommand.localDateValueOfParameterNamed(RescheduleLoansApiConstants.submittedOnDateParamName);
            }

            // initially set the value to null
            LocalDate rescheduleFromDate = null;

            // start point of the rescheduling exercise
            Integer rescheduleFromInstallment = null;

            // initially set the value to null
            LocalDate adjustedDueDate = null;

            // check if the parameter is in the JsonCommand object
            if (jsonCommand.hasParameter(RescheduleLoansApiConstants.rescheduleFromDateParamName)) {
                // create a LocalDate object from the "rescheduleFromDate" Date
                // string
                LocalDate localDate = jsonCommand.localDateValueOfParameterNamed(RescheduleLoansApiConstants.rescheduleFromDateParamName);

                if (localDate != null) {
                    // get installment by due date
                    LoanRepaymentScheduleInstallment installment = loan.getInstallmentByScheduleFromDate(localDate);
                    rescheduleFromInstallment = installment.getInstallmentNumber();

                    // update the value of the "rescheduleFromDate" variable
                    rescheduleFromDate = localDate;
                }
            }

            if (jsonCommand.hasParameter(RescheduleLoansApiConstants.adjustedDueDateParamName)) {
                // create a LocalDate object from the "adjustedDueDate" Date
                // string
                adjustedDueDate = jsonCommand.localDateValueOfParameterNamed(RescheduleLoansApiConstants.adjustedDueDateParamName);
            }

            final LoanRescheduleRequest loanRescheduleRequest = LoanRescheduleRequest.instance(loan,
                    LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(), rescheduleFromInstallment, rescheduleFromDate,
                    recalculateInterest, rescheduleReasonCodeValue, rescheduleReasonComment, submittedOnDate,
                    this.platformSecurityContext.authenticatedUser(), null, null, null, null);

            // update reschedule request to term variations mapping
            List<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings = new ArrayList<>();
            final Boolean isActive = false;
            final boolean isSpecificToInstallment = false;
            BigDecimal decimalValue = null;
            LocalDate dueDate = null;
            // create term variations for flat and declining balance loans
            createLoanTermVariationsForRegularLoans(loan, graceOnPrincipal, graceOnInterest, extraTerms, rediferirTerms, interestRate,
                    rescheduleFromDate, adjustedDueDate, loanRescheduleRequest, loanRescheduleRequestToTermVariationMappings, isActive,
                    isSpecificToInstallment, decimalValue, dueDate, endDate, emi);

            // create a new entry in the m_loan_reschedule_request table

            this.loanRescheduleRequestRepository.saveAndFlush(loanRescheduleRequest);

            return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).withEntityId(loanRescheduleRequest.getId())
                    .withLoanId(loan.getId()).withClientId(loan.getClientId()).withOfficeId(loan.getOfficeId())
                    .withGroupId(loan.getGroupId()).build();
        }

        catch (final JpaSystemException | DataIntegrityViolationException dve) {
            // handle the data integrity violation
            handleDataIntegrityViolation(dve);

            // return an empty command processing result object
            return CommandProcessingResult.empty();
        }
    }

    private void createLoanTermVariationsForRegularLoans(final Loan loan, final Integer graceOnPrincipal, final Integer graceOnInterest,
            final Integer extraTerms, final Integer rediferirTerms, final BigDecimal interestRate, LocalDate rescheduleFromDate,
            LocalDate adjustedDueDate, final LoanRescheduleRequest loanRescheduleRequest,
            List<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings, final Boolean isActive,
            final boolean isSpecificToInstallment, BigDecimal decimalValue, LocalDate dueDate, LocalDate endDate, BigDecimal emi) {

        if (rescheduleFromDate != null && endDate != null && emi != null) {
            LoanTermVariations parent = null;
            LocalDate rescheduleFromLocDate = rescheduleFromDate;
            LocalDate endDateLocDate = endDate;
            final Integer termType = LoanTermVariationType.EMI_AMOUNT.getValue();
            List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
            for (LoanRepaymentScheduleInstallment installment : installments) {
                if (!DateUtils.isBefore(installment.getDueDate(), rescheduleFromLocDate)
                        && !DateUtils.isAfter(installment.getDueDate(), endDateLocDate)) {
                    createLoanTermVariations(loanRescheduleRequest, termType, loan, installment.getDueDate(), installment.getDueDate(),
                            loanRescheduleRequestToTermVariationMappings, isActive, true, emi, parent);
                }
                if (DateUtils.isAfter(installment.getDueDate(), endDateLocDate)) {
                    break;
                }
            }
        }

        if (rescheduleFromDate != null && adjustedDueDate != null) {
            LoanTermVariations parent = null;
            final Integer termType = LoanTermVariationType.DUE_DATE.getValue();
            createLoanTermVariations(loanRescheduleRequest, termType, loan, rescheduleFromDate, adjustedDueDate,
                    loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment, decimalValue, parent);
        }

        if (rescheduleFromDate != null && interestRate != null) {
            LoanTermVariations parent = null;
            final Integer termType = LoanTermVariationType.INTEREST_RATE_FROM_INSTALLMENT.getValue();
            createLoanTermVariations(loanRescheduleRequest, termType, loan, rescheduleFromDate, dueDate,
                    loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment, interestRate, parent);
        }

        if (rescheduleFromDate != null && graceOnPrincipal != null) {
            final Integer termType = LoanTermVariationType.GRACE_ON_PRINCIPAL.getValue();
            LoanTermVariations parent = null;
            parent = createLoanTermVariations(loanRescheduleRequest, termType, loan, rescheduleFromDate, dueDate,
                    loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment, BigDecimal.valueOf(graceOnPrincipal),
                    parent);

            BigDecimal extraTermsBasedOnGracePeriods = BigDecimal.valueOf(graceOnPrincipal);
            createLoanTermVariations(loanRescheduleRequest, LoanTermVariationType.EXTEND_REPAYMENT_PERIOD.getValue(), loan,
                    rescheduleFromDate, dueDate, loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment,
                    extraTermsBasedOnGracePeriods, parent);

        }

        if (rescheduleFromDate != null && graceOnInterest != null) {
            LoanTermVariations parent = null;
            final Integer termType = LoanTermVariationType.GRACE_ON_INTEREST.getValue();
            createLoanTermVariations(loanRescheduleRequest, termType, loan, rescheduleFromDate, dueDate,
                    loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment, BigDecimal.valueOf(graceOnInterest),
                    parent);
        }

        if (rescheduleFromDate != null && extraTerms != null) {
            final LoanTermVariations parent = null;
            final Integer termType = LoanTermVariationType.EXTEND_REPAYMENT_PERIOD.getValue();
            createLoanTermVariations(loanRescheduleRequest, termType, loan, rescheduleFromDate, dueDate,
                    loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment, BigDecimal.valueOf(extraTerms),
                    parent);
        }

        if (rediferirTerms != null) {
            final GlobalConfigurationProperty globalConfigurationProperty = this.globalConfigurationRepository
                    .findOneByNameWithNotFoundDetection(RescheduleLoansApiConstants.ALLOWED_REDEFERRALS_WITHIN_SIX_MONTHS);
            final Long maximumRediferralValue = globalConfigurationProperty.getValue();
            final Integer rediferidoNumber = this.loanReadPlatformService.retrieveRediferidoNumberLast6Months(loan.getId());
            if (globalConfigurationProperty.isEnabled() && rediferidoNumber > maximumRediferralValue) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.reschedule.rediferir.exceed.max.allowed.in.6.months",
                        "Rediferir exceed max allowed in 6 months", maximumRediferralValue);
            }
            final LocalDate transactionDate = DateUtils.getBusinessLocalDate();
            if (!loanRescheduleRequestToTermVariationMappings.isEmpty()) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.reschedule.rediferir.other.variations.already.exists",
                        "Rediferir other variations already exists", rediferirTerms);
            }

            final GlobalConfigurationProperty maximumDaysInArrearsConfiguration = this.globalConfigurationRepository
                    .findOneByNameWithNotFoundDetection(RescheduleLoansApiConstants.MAXIMUM_DAYS_IN_ARREARS_FOR_REDIFERRALS);
            Long maximumArrearsValue = maximumDaysInArrearsConfiguration.getValue();
            final CollectionData collectionData = this.delinquencyReadPlatformService.calculateLoanCollectionData(loan.getId());
            final Long daysInArrears = collectionData.getPastDueDays();
            if (maximumDaysInArrearsConfiguration.isEnabled() && daysInArrears > maximumArrearsValue) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.reschedule.rediferir.exceed.max.allowed.days.in.arrears",
                        "Rediferir exceeds maximum days in arrears", rediferidoNumber);
            }
            final List<LoanRepaymentScheduleInstallment> rediferirUnpaidInstallments = findRediferirUnpaidInstallments(
                    loan.getRepaymentScheduleInstallments(), transactionDate);
            int minimumRequiredInstallmentNumber = 0;
            if (!rediferirUnpaidInstallments.isEmpty()) {
                minimumRequiredInstallmentNumber = rediferirUnpaidInstallments.size();
            }
            if (rediferirTerms <= minimumRequiredInstallmentNumber) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.reschedule.rediferir.periods.less.than.unpaid.installments",
                        "Rediferir periods less than unpaid installments", rediferirTerms);
            }

            final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = loan.fetchLoanForeclosureDetail(transactionDate);
            final MonetaryCurrency currency = loan.getCurrency();
            final BigDecimal rediferirAmount = loanRepaymentScheduleInstallment.getRediferirAmount(currency).getAmount();
            if (!Money.of(currency, rediferirAmount).isGreaterThanZero()) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.reschedule.rediferir.amount.zero", "Rediferir amount is zero",
                        rediferirAmount);
            }
            final Integer termType = LoanTermVariationType.REDIFERIR.getValue();
            createLoanTermVariations(loanRescheduleRequest, termType, loan, rescheduleFromDate, dueDate,
                    loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment, BigDecimal.valueOf(rediferirTerms),
                    null);
        }

        loanRescheduleRequest.updateLoanRescheduleRequestToTermVariationMappings(loanRescheduleRequestToTermVariationMappings);
    }

    private LoanTermVariations createLoanTermVariations(LoanRescheduleRequest loanRescheduleRequest, final Integer termType,
            final Loan loan, LocalDate rescheduleFromDate, LocalDate adjustedDueDate,
            List<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings, final Boolean isActive,
            final boolean isSpecificToInstallment, final BigDecimal decimalValue, LoanTermVariations parent) {
        LoanTermVariations loanTermVariation = new LoanTermVariations(termType, rescheduleFromDate, decimalValue, adjustedDueDate,
                isSpecificToInstallment, loan, loan.getStatus().getValue(), isActive, parent);
        loan.getLoanTermVariations().add(loanTermVariation);
        loanRescheduleRequestToTermVariationMappings
                .add(LoanRescheduleRequestToTermVariationMapping.createNew(loanRescheduleRequest, loanTermVariation));
        return loanTermVariation;
    }

    @Override
    @Transactional
    public CommandProcessingResult approve(JsonCommand jsonCommand) {

        try {
            final Long loanRescheduleRequestId = jsonCommand.entityId();

            final LoanRescheduleRequest loanRescheduleRequest = this.loanRescheduleRequestRepository.findById(loanRescheduleRequestId)
                    .orElseThrow(() -> new LoanRescheduleRequestNotFoundException(loanRescheduleRequestId));

            // validate the request in the JsonCommand object passed as
            // parameter
            this.loanRescheduleRequestDataValidator.validateForApproveAction(jsonCommand, loanRescheduleRequest);

            final AppUser appUser = this.platformSecurityContext.authenticatedUser();
            final Map<String, Object> changes = new LinkedHashMap<>();

            LocalDate approvedOnDate = jsonCommand.localDateValueOfParameterNamed("approvedOnDate");
            final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(jsonCommand.dateFormat())
                    .withLocale(jsonCommand.extractLocale());

            changes.put("locale", jsonCommand.locale());
            changes.put("dateFormat", jsonCommand.dateFormat());
            changes.put("approvedOnDate", approvedOnDate.format(dateTimeFormatter));
            changes.put("approvedByUserId", appUser.getId());

            Loan loan = loanRescheduleRequest.getLoan();
            final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
            final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());

            ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan,
                    loanRescheduleRequest.getRescheduleFromDate());

            Collection<LoanRepaymentScheduleHistory> loanRepaymentScheduleHistoryList = this.loanScheduleHistoryWritePlatformService
                    .createLoanScheduleArchive(loan.getRepaymentScheduleInstallments(), loan, loanRescheduleRequest);

            final LoanApplicationTerms loanApplicationTerms = loan.constructLoanApplicationTerms(scheduleGeneratorDTO);

            LocalDate rescheduleFromDate = null;
            Set<LoanTermVariations> activeLoanTermVariations = loan.getActiveLoanTermVariations();
            LoanTermVariations dueDateVariationInCurrentRequest = loanRescheduleRequest.getDueDateTermVariationIfExists();
            if (dueDateVariationInCurrentRequest != null && activeLoanTermVariations != null) {
                LocalDate fromScheduleDate = dueDateVariationInCurrentRequest.fetchTermApplicaDate();
                LocalDate currentScheduleDate = fromScheduleDate;
                LocalDate modifiedScheduleDate = dueDateVariationInCurrentRequest.fetchDateValue();
                Map<LocalDate, LocalDate> changeMap = new HashMap<>();
                changeMap.put(currentScheduleDate, modifiedScheduleDate);
                for (LoanTermVariations activeLoanTermVariation : activeLoanTermVariations) {
                    if (activeLoanTermVariation.getTermType().isDueDateVariation()
                            && activeLoanTermVariation.fetchDateValue().equals(dueDateVariationInCurrentRequest.fetchTermApplicaDate())) {
                        activeLoanTermVariation.markAsInactive();
                        rescheduleFromDate = activeLoanTermVariation.fetchTermApplicaDate();
                        dueDateVariationInCurrentRequest.setTermApplicableFrom(rescheduleFromDate);
                    } else if (!DateUtils.isBefore(activeLoanTermVariation.fetchTermApplicaDate(), fromScheduleDate)) {
                        while (DateUtils.isBefore(currentScheduleDate, activeLoanTermVariation.fetchTermApplicaDate())) {
                            currentScheduleDate = DEFAULT_SCHEDULED_DATE_GENERATOR.generateNextRepaymentDate(currentScheduleDate,
                                    loanApplicationTerms, false);
                            modifiedScheduleDate = DEFAULT_SCHEDULED_DATE_GENERATOR.generateNextRepaymentDate(modifiedScheduleDate,
                                    loanApplicationTerms, false);
                            changeMap.put(currentScheduleDate, modifiedScheduleDate);
                        }
                        if (changeMap.containsKey(activeLoanTermVariation.fetchTermApplicaDate())) {
                            activeLoanTermVariation.setTermApplicableFrom(changeMap.get(activeLoanTermVariation.fetchTermApplicaDate()));
                        }
                    }
                }
            }
            if (rescheduleFromDate == null) {
                rescheduleFromDate = loanRescheduleRequest.getRescheduleFromDate();
            }

            int rediferirPeriods = 0;
            for (LoanRescheduleRequestToTermVariationMapping mapping : loanRescheduleRequest
                    .getLoanRescheduleRequestToTermVariationMappings()) {
                if (mapping.getLoanTermVariations().getTermType().isRediferir()) {
                    final LoanTermVariations loanTermVariationsValue = mapping.getLoanTermVariations();
                    rediferirPeriods = loanTermVariationsValue.getTermValue().intValue();

                }
                mapping.getLoanTermVariations().updateIsActive(true);
            }
            BigDecimal annualNominalInterestRate = null;
            List<LoanTermVariationsData> loanTermVariations = new ArrayList<>();
            loan.constructLoanTermVariations(scheduleGeneratorDTO.getFloatingRateDTO(), annualNominalInterestRate, loanTermVariations);
            loanApplicationTerms.getLoanTermVariations().setExceptionData(loanTermVariations);

            /*
             * for (LoanTermVariationsData loanTermVariation :
             * loanApplicationTerms.getLoanTermVariations().getDueDateVariation( )) { if
             * (rescheduleFromDate.isBefore(loanTermVariation. getTermApplicableFrom())) { LocalDate applicableDate =
             * this.scheduledDateGenerator.generateNextRepaymentDate( rescheduleFromDate, loanApplicationTerms, false,
             * loanApplicationTerms.getHolidayDetailDTO()); if
             * (loanTermVariation.getTermApplicableFrom().equals(applicableDate) ) { LocalDate adjustedDate =
             * this.scheduledDateGenerator.generateNextRepaymentDate( adjustedApplicableDate, loanApplicationTerms,
             * false, loanApplicationTerms.getHolidayDetailDTO());
             * loanTermVariation.setApplicableFromDate(adjustedDate); } } }
             */

            final MathContext mathContext = MoneyHelper.getMathContext();
            final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.loanRepaymentScheduleTransactionProcessorFactory
                    .determineProcessor(loan.transactionProcessingStrategy());
            final LoanScheduleGenerator loanScheduleGenerator = this.loanScheduleFactory.create(loanApplicationTerms.getLoanScheduleType(),
                    loanApplicationTerms.getInterestMethod());
            final LoanLifecycleStateMachine loanLifecycleStateMachine = null;
            loan.setHelpers(loanLifecycleStateMachine, this.loanSummaryWrapper, this.loanRepaymentScheduleTransactionProcessorFactory);
            final LoanScheduleDTO loanSchedule = loanScheduleGenerator.rescheduleNextInstallments(mathContext, loanApplicationTerms, loan,
                    loanApplicationTerms.getHolidayDetailDTO(), loanRepaymentScheduleTransactionProcessor, rescheduleFromDate);

            // Either the installments got recalculated or the model
            if (loanSchedule.getInstallments() != null) {
                loan.updateLoanSchedule(loanSchedule.getInstallments());
            } else {
                loan.updateLoanSchedule(loanSchedule.getLoanScheduleModel());
            }
            /*
             * Add Validate loan charge
             */
            if (rediferirPeriods > 0) {
                final Integer rediferidoNumber = this.loanReadPlatformService.retrieveRediferidoNumberLast6Months(loan.getId());
                final GlobalConfigurationProperty globalConfigurationProperty = this.globalConfigurationRepository
                        .findOneByNameWithNotFoundDetection(RescheduleLoansApiConstants.ALLOWED_REDEFERRALS_WITHIN_SIX_MONTHS);
                this.loanRescheduleRequestDataValidator.validateRescheduleLoanCharge(loan, globalConfigurationProperty, rediferidoNumber);
            }

            loan.recalculateAllCharges();
            ChangedTransactionDetail changedTransactionDetail = loan.processTransactions();
            loan.updateLoanDerivedFields();

            if (rediferirPeriods > 0) {
                final LocalDate transactionDate = DateUtils.getBusinessLocalDate();
                final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = loan.fetchLoanForeclosureDetail(transactionDate);
                final MonetaryCurrency currency = loan.getCurrency();
                final BigDecimal rediferirAmount = loanRepaymentScheduleInstallment.getRediferirAmount(currency).getAmount();
                if (!Money.of(currency, rediferirAmount).isGreaterThanZero()) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.reschedule.rediferir.amount.zero",
                            "Rediferir amount is zero", rediferirAmount);
                }
                final BigDecimal newPrincipalAmount = loanRepaymentScheduleInstallment.getPrincipal(currency).getAmount();
                final Set<LoanCharge> loanCharges = loan.getActiveCharges();
                final HolidayDetailDTO holidayDetailDTO = loanApplicationTerms.getHolidayDetailDTO();
                final MathContext mc = MoneyHelper.getMathContext();
                final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments = loan.getRepaymentScheduleInstallments();
                final List<LoanRepaymentScheduleInstallment> rediferirUnpaidInstallments = findRediferirUnpaidInstallments(
                        repaymentScheduleInstallments, transactionDate);
                if (!rediferirUnpaidInstallments.isEmpty()) {
                    int minimumRequiredInstallmentNumber = rediferirUnpaidInstallments.size();
                    final LoanRepaymentScheduleInstallment firstRediferirUnpaidInstallment = rediferirUnpaidInstallments.get(0);
                    LocalDate installmentDueDate = firstRediferirUnpaidInstallment.getDueDate();
                    LocalDate installmentFromDate = firstRediferirUnpaidInstallment.getFromDate();
                    Integer rediferirInstallmentNumber = firstRediferirUnpaidInstallment.getInstallmentNumber();
                    if (DateUtils.isBefore(installmentFromDate, transactionDate)
                            && !DateUtils.isBefore(installmentDueDate, transactionDate)) {
                        if (DateUtils.isBefore(installmentFromDate, transactionDate)) {
                            int totalPeriodDays = Math.toIntExact(ChronoUnit.DAYS.between(firstRediferirUnpaidInstallment.getFromDate(),
                                    firstRediferirUnpaidInstallment.getDueDate()));
                            int tillDays = Math
                                    .toIntExact(ChronoUnit.DAYS.between(firstRediferirUnpaidInstallment.getFromDate(), transactionDate));
                            final Money interestForCurrentPeriod = Money.of(currency,
                                    BigDecimal.valueOf(loan.calculateInterestForDays(totalPeriodDays,
                                            firstRediferirUnpaidInstallment.getInterestCharged(currency).getAmount(), tillDays)));
                            firstRediferirUnpaidInstallment.updateInterestCharged(interestForCurrentPeriod.getAmount());
                            if (rediferirUnpaidInstallments.size() > 1) {
                                final LoanRepaymentScheduleInstallment secondRediferirUnpaidInstallment = rediferirUnpaidInstallments
                                        .get(1);
                                installmentFromDate = transactionDate;
                                installmentDueDate = secondRediferirUnpaidInstallment.getDueDate();
                            }
                        }
                    }
                    if (rediferirPeriods <= minimumRequiredInstallmentNumber) {
                        throw new GeneralPlatformDomainRuleException(
                                "error.msg.loan.reschedule.rediferir.periods.less.than.unpaid.installments",
                                "Rediferir periods less than unpaid installments", rediferirPeriods);
                    }
                    final List<LoanRepaymentScheduleInstallment> unpaidRepaymentInstallments = repaymentScheduleInstallments.stream()
                            .filter(LoanRepaymentScheduleInstallment::isNotFullyPaidOff).toList();
                    for (final LoanRepaymentScheduleInstallment unpaidInstallment : unpaidRepaymentInstallments) {
                        unpaidInstallment.resetPrincipalComponents();
                    }
                    final Money newPrincipal = Money.of(currency, newPrincipalAmount);
                    loanApplicationTerms.updateLoanTermVariations(new ArrayList<>());
                    loanApplicationTerms.updateNumberOfRepayments(rediferirPeriods);
                    loanApplicationTerms.updateLoanTermFrequency(rediferirPeriods);
                    loanApplicationTerms.setPrincipal(newPrincipal);
                    loanApplicationTerms.updateApprovedPrincipal(newPrincipal);
                    loanApplicationTerms.updateInterestChargedFromDate(installmentFromDate);
                    loanApplicationTerms.updateExpectedDisbursementDate(installmentFromDate);
                    loanApplicationTerms.updateCalculatedRepaymentsStartingFromDate(installmentDueDate);
                    loanApplicationTerms.updateRepaymentsStartingFromDate(installmentDueDate);
                    final LoanScheduleModel loanScheduleModel = loanScheduleGenerator.generate(mc, loanApplicationTerms, loanCharges,
                            holidayDetailDTO);
                    final List<LoanScheduleModelPeriod> loanScheduleModelPeriods = loanScheduleModel.getPeriods();
                    ArrayList<LoanRepaymentScheduleInstallment> installmentsToRemove = new ArrayList<>();
                    for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
                        if (installment.getInstallmentNumber() > rediferirInstallmentNumber) {
                            installmentsToRemove.add(installment);
                        }
                    }

                    repaymentScheduleInstallments.removeAll(installmentsToRemove);
                    for (final LoanScheduleModelPeriod scheduledLoanInstallment : loanScheduleModelPeriods) {
                        if (scheduledLoanInstallment.isRepaymentPeriod() || scheduledLoanInstallment.isDownPaymentPeriod()) {
                            final LoanRepaymentScheduleInstallment installment = new LoanRepaymentScheduleInstallment(loan,
                                    scheduledLoanInstallment.periodNumber(), scheduledLoanInstallment.periodFromDate(),
                                    scheduledLoanInstallment.periodDueDate(), scheduledLoanInstallment.principalDue(),
                                    scheduledLoanInstallment.interestDue(), scheduledLoanInstallment.feeChargesDue(),
                                    scheduledLoanInstallment.penaltyChargesDue(),
                                    scheduledLoanInstallment.isRecalculatedInterestComponent(),
                                    scheduledLoanInstallment.getLoanCompoundingDetails(),
                                    scheduledLoanInstallment.rescheduleInterestPortion(), scheduledLoanInstallment.isDownPaymentPeriod());
                            installment.updateLoan(loan);
                            repaymentScheduleInstallments.add(installment);
                        }
                    }
                    loan.updateTermFrequency(rediferirPeriods);
                    loan.updateNumberOfRepayments(rediferirPeriods);
                    loan.recalculateAllCharges();
                    loan.updateLoanDerivedFields();

                    ExternalId txnExternalId = externalIdFactory.create();
                    final PaymentDetail paymentDetail = null;
                    final String chargeRefundChargeType = null;
                    final String noteText = null;
                    final boolean isAccountTransfer = false;
                    final LoanTransaction newRepaymentTransaction = this.loanAccountDomainService.makeRepayment(
                            LoanTransactionType.REPAYMENT, loan, transactionDate, rediferirAmount, paymentDetail, noteText, txnExternalId,
                            false, chargeRefundChargeType, isAccountTransfer, null, false, true);
                    changedTransactionDetail = loan.processTransactions();
                    loanAccountDomainService.saveLoanTransactionWithDataIntegrityViolationChecks(newRepaymentTransaction);
                }
            }

            this.loanRepaymentScheduleHistoryRepository.saveAll(loanRepaymentScheduleHistoryList);

            loan.updateRescheduledByUser(appUser);
            loan.updateRescheduledOnDate(DateUtils.getBusinessLocalDate());

            // update the status of the request
            loanRescheduleRequest.approve(appUser, approvedOnDate);

            /***
             * TODO Vishwas Batch save is giving me a HibernateOptimisticLockingFailureException, looping and saving for
             * the time being, not a major issue for now as this loop is entered only in edge cases (when a adjustment
             * is made before the latest payment recorded against the loan)
             ***/
            if (changedTransactionDetail != null) {
                for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                    loanAccountDomainService.saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                    accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                }
                // Trigger transaction replayed event
                replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
            }
            loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
            // update the loan object
            postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
            loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
            this.loanAccountDomainService.recalculateAccruals(loan, true);

            final Boolean isJobTriggered = jsonCommand.booleanPrimitiveValueOfParameterNamed("isJobTriggered");
            if (loan.isTopup()) {
                businessEventNotifierService
                        .notifyPostBusinessEvent(new LoanRescheduledDueAdjustScheduleBusinessEvent(loan, isJobTriggered));
            }
            return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).withEntityId(loanRescheduleRequestId)
                    .withLoanId(loanRescheduleRequest.getLoan().getId()).with(changes).withClientId(loan.getClientId())
                    .withOfficeId(loan.getOfficeId()).withGroupId(loan.getGroupId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            // handle the data integrity violation
            handleDataIntegrityViolation(dve);

            // return an empty command processing result object
            return CommandProcessingResult.empty();
        }
    }

    private List<LoanRepaymentScheduleInstallment> findRediferirUnpaidInstallments(
            final List<LoanRepaymentScheduleInstallment> loanRepaymentScheduleInstallments, final LocalDate businessDate) {
        final List<LoanRepaymentScheduleInstallment> unpaidInstallments = new ArrayList<>();
        for (final LoanRepaymentScheduleInstallment installment : loanRepaymentScheduleInstallments) {
            final LocalDate dueDate = installment.getDueDate();
            if (installment.isNotFullyPaidOff() && !DateUtils.isAfter(businessDate, dueDate)) {
                unpaidInstallments.add(installment);
            }
        }
        return unpaidInstallments;
    }

    private Loan saveAndFlushLoanWithDataIntegrityViolationChecks(final Loan loan) {
        try {
            List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
            for (LoanRepaymentScheduleInstallment installment : installments) {
                if (installment.getId() == null) {
                    this.repaymentScheduleInstallmentRepository.save(installment);
                }
            }
            return this.loanRepositoryWrapper.saveAndFlush(loan);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.transaction");
            if (realCause.getMessage().toLowerCase().contains("external_id_unique")) {
                baseDataValidator.reset().parameter("externalId").failWithCode("value.must.be.unique");
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors, e);
            }
            throw e;
        }
    }

    private void postJournalEntries(Loan loan, List<Long> existingTransactionIds, List<Long> existingReversedTransactionIds) {
        final MonetaryCurrency currency = loan.getCurrency();
        boolean isAccountTransfer = false;
        final Map<String, Object> accountingBridgeData = loan.deriveAccountingBridgeData(currency.getCode(), existingTransactionIds,
                existingReversedTransactionIds, isAccountTransfer);
        this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData);
    }

    @Override
    @Transactional
    public CommandProcessingResult reject(JsonCommand jsonCommand) {

        try {
            final Long loanRescheduleRequestId = jsonCommand.entityId();

            final LoanRescheduleRequest loanRescheduleRequest = loanRescheduleRequestRepository.findById(loanRescheduleRequestId)
                    .orElseThrow(() -> new LoanRescheduleRequestNotFoundException(loanRescheduleRequestId));

            // validate the request in the JsonCommand object passed as
            // parameter
            this.loanRescheduleRequestDataValidator.validateForRejectAction(jsonCommand, loanRescheduleRequest);

            final AppUser appUser = this.platformSecurityContext.authenticatedUser();
            final Map<String, Object> changes = new LinkedHashMap<>();

            LocalDate rejectedOnDate = jsonCommand.localDateValueOfParameterNamed("rejectedOnDate");
            final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(jsonCommand.dateFormat())
                    .withLocale(jsonCommand.extractLocale());

            changes.put("locale", jsonCommand.locale());
            changes.put("dateFormat", jsonCommand.dateFormat());
            changes.put("rejectedOnDate", rejectedOnDate.format(dateTimeFormatter));
            changes.put("rejectedByUserId", appUser.getId());

            if (!changes.isEmpty()) {
                loanRescheduleRequest.reject(appUser, rejectedOnDate);
                Set<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings = loanRescheduleRequest
                        .getLoanRescheduleRequestToTermVariationMappings();
                for (LoanRescheduleRequestToTermVariationMapping loanRescheduleRequestToTermVariationMapping : loanRescheduleRequestToTermVariationMappings) {
                    loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().markAsInactive();
                }
            }

            return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).withEntityId(loanRescheduleRequestId)
                    .withLoanId(loanRescheduleRequest.getLoan().getId()).with(changes)
                    .withClientId(loanRescheduleRequest.getLoan().getClientId()).withOfficeId(loanRescheduleRequest.getLoan().getOfficeId())
                    .withGroupId(loanRescheduleRequest.getLoan().getGroupId()).build();
        }

        catch (final JpaSystemException | DataIntegrityViolationException dve) {
            // handle the data integrity violation
            handleDataIntegrityViolation(dve);

            // return an empty command processing result object
            return CommandProcessingResult.empty();
        }
    }

    /**
     * handles the data integrity violation exception for loan reschedule write services
     *
     * @param dve
     *            data integrity violation exception
     *
     **/
    private void handleDataIntegrityViolation(final NonTransientDataAccessException dve) {
        LOG.error("Error occured.", dve);
        throw ErrorHandler.getMappable(dve, "error.msg.loan.reschedule.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }
}
