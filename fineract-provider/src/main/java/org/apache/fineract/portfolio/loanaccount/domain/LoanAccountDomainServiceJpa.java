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
package org.apache.fineract.portfolio.loanaccount.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.domain.CustomChargeHonorarioMap;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.domain.CustomChargeHonorarioMapRepository;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockLevel;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingEnum;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingsRepositoryWrapper;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.MultiException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanBalanceChangedBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanAccrualTransactionCreatedBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanChargePaymentPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanChargePaymentPreBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanCreditBalanceRefundPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanCreditBalanceRefundPreBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanForeClosurePostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanForeClosurePreBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanInvoiceGenerationPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanRefundPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanRefundPreBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionDownPaymentPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionDownPaymentPreBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionGoodwillCreditPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionGoodwillCreditPreBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionMakeRepaymentPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionMakeRepaymentPreBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionMerchantIssuedRefundPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionMerchantIssuedRefundPreBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionPayoutRefundPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionPayoutRefundPreBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionRecoveryPaymentPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanTransactionRecoveryPaymentPreBusinessEvent;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.holiday.domain.Holiday;
import org.apache.fineract.organisation.holiday.domain.HolidayRepository;
import org.apache.fineract.organisation.holiday.domain.HolidayStatusType;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.domain.*;
import org.apache.fineract.organisation.workingdays.domain.WorkingDays;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.apache.fineract.portfolio.account.domain.AccountTransferRepository;
import org.apache.fineract.portfolio.account.domain.AccountTransferStandingInstruction;
import org.apache.fineract.portfolio.account.domain.AccountTransferTransaction;
import org.apache.fineract.portfolio.account.domain.StandingInstructionRepository;
import org.apache.fineract.portfolio.account.domain.StandingInstructionStatus;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.client.data.ClientAdditionalFieldsData;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.CollectionHouseConfiguration;
import org.apache.fineract.portfolio.collectionhousemanagement.service.CollectionHouseReadWriteServiceImpl;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.delinquency.domain.DelinquencyRange;
import org.apache.fineract.portfolio.delinquency.domain.LoanDelinquencyAction;
import org.apache.fineract.portfolio.delinquency.helper.DelinquencyEffectivePauseHelper;
import org.apache.fineract.portfolio.delinquency.service.DelinquencyReadPlatformService;
import org.apache.fineract.portfolio.delinquency.service.DelinquencyWritePlatformService;
import org.apache.fineract.portfolio.delinquency.validator.LoanDelinquencyActionData;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.loanaccount.data.*;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualTransactionBusinessEventService;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanaccount.service.LoanUtilService;
import org.apache.fineract.portfolio.loanaccount.service.ReplayedTransactionBusinessEventService;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.repaymentwithpostdatedchecks.data.PostDatedChecksStatus;
import org.apache.fineract.portfolio.repaymentwithpostdatedchecks.domain.PostDatedChecks;
import org.apache.fineract.portfolio.repaymentwithpostdatedchecks.domain.PostDatedChecksRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanAccountDomainServiceJpa implements LoanAccountDomainService {

    private final LoanAssembler loanAccountAssembler;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanTransactionRepository loanTransactionRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final HolidayRepository holidayRepository;
    private final WorkingDaysRepositoryWrapper workingDaysRepository;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final NoteRepository noteRepository;
    private final AccountTransferRepository accountTransferRepository;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository;
    private final LoanAccrualPlatformService loanAccrualPlatformService;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final LoanUtilService loanUtilService;
    private final StandingInstructionRepository standingInstructionRepository;
    private final PostDatedChecksRepository postDatedChecksRepository;
    private final LoanCollateralManagementRepository loanCollateralManagementRepository;
    private final DelinquencyWritePlatformService delinquencyWritePlatformService;
    private final LoanLifecycleStateMachine defaultLoanLifecycleStateMachine;
    private final ExternalIdFactory externalIdFactory;
    private final ReplayedTransactionBusinessEventService replayedTransactionBusinessEventService;
    private final LoanAccrualTransactionBusinessEventService loanAccrualTransactionBusinessEventService;
    private final DelinquencyEffectivePauseHelper delinquencyEffectivePauseHelper;
    private final DelinquencyReadPlatformService delinquencyReadPlatformService;
    private final BlockingReasonSettingsRepositoryWrapper blockingReasonSettingsRepositoryWrapper;
    private final LoanBlockingReasonRepository loanBlockingReasonRepository;
    private final PlatformSecurityContext platformSecurityContext;
    private final JdbcTemplate jdbcTemplate;
    private final CollectionHouseReadWriteServiceImpl collectionHouseReadWriteService;
    private final ClientReadPlatformService clientReadPlatformService;
    @Autowired
    private CustomChargeHonorarioMapRepository customChargeHonorarioMapRepository;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @Override
    public LoanTransaction makeRepayment(final LoanTransactionType repaymentTransactionType, final Loan loan,
            final LocalDate transactionDate, final BigDecimal transactionAmount, final PaymentDetail paymentDetail, final String noteText,
            final ExternalId txnExternalId, final boolean isRecoveryRepayment, final String chargeRefundChargeType,
            boolean isAccountTransfer, HolidayDetailDTO holidayDetailDto, Boolean isHolidayValidationDone) {
        return makeRepayment(repaymentTransactionType, loan, transactionDate, transactionAmount, paymentDetail, noteText, txnExternalId,
                isRecoveryRepayment, chargeRefundChargeType, isAccountTransfer, holidayDetailDto, isHolidayValidationDone, false);
    }

    @Transactional
    @Override
    public void updateLoanCollateralTransaction(Set<LoanCollateralManagement> loanCollateralManagementSet) {
        this.loanCollateralManagementRepository.saveAll(loanCollateralManagementSet);
    }

    @Transactional
    @Override
    public void updateLoanCollateralStatus(Set<LoanCollateralManagement> loanCollateralManagementSet, boolean isReleased) {
        for (LoanCollateralManagement loanCollateralManagement : loanCollateralManagementSet) {
            loanCollateralManagement.setIsReleased(isReleased);
        }
        this.loanCollateralManagementRepository.saveAll(loanCollateralManagementSet);
    }

    @Transactional
    @Override
    public LoanTransaction makeRepayment(final LoanTransactionType repaymentTransactionType, Loan loan, final LocalDate transactionDate,
            final BigDecimal transactionAmount, final PaymentDetail paymentDetail, final String noteText, final ExternalId txnExternalId,
            final boolean isRecoveryRepayment, final String chargeRefundChargeType, boolean isAccountTransfer,
            HolidayDetailDTO holidayDetailDto, Boolean isHolidayValidationDone, final boolean isLoanToLoanTransfer) {
        checkClientOrGroupActive(loan);
        LoanBusinessEvent repaymentEvent = getLoanRepaymentTypeBusinessEvent(repaymentTransactionType, isRecoveryRepayment, loan);
        if (repaymentEvent != null) {
            businessEventNotifierService.notifyPreBusinessEvent(repaymentEvent);
        }

        // TODO: Is it required to validate transaction date with meeting dates
        // if repayments is synced with meeting?
        /*
         * if(loan.isSyncDisbursementWithMeeting()){ // validate actual disbursement date against meeting date
         * CalendarInstance calendarInstance = this.calendarInstanceRepository.findCalendarInstaneByLoanId
         * (loan.getId(), CalendarEntityType.LOANS.getValue()); this.loanEventApiJsonValidator
         * .validateRepaymentDateWithMeetingDate(transactionDate, calendarInstance); }
         */

        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();
        final Money repaymentAmount = Money.of(loan.getCurrency(), transactionAmount);
        LoanTransaction newRepaymentTransaction;
        if (isRecoveryRepayment) {
            newRepaymentTransaction = LoanTransaction.recoveryRepayment(loan.getOffice(), repaymentAmount, paymentDetail, transactionDate,
                    txnExternalId);
        } else {
            newRepaymentTransaction = LoanTransaction.repaymentType(repaymentTransactionType, loan.getOffice(), repaymentAmount,
                    paymentDetail, transactionDate, txnExternalId, chargeRefundChargeType, loan.getRepaymentTransactionProcessingType(),
                    loan.recalculateEMI());
        }

        ClientAdditionalFieldsData clientAdditionalInformation = this.clientReadPlatformService
                .retrieveClientAdditionalData(loan.getClientId());
        String nit = ObjectUtils.defaultIfNull(clientAdditionalInformation.getNit(), clientAdditionalInformation.getCedula());
        CollectionHouseConfiguration collectionHouse = this.collectionHouseReadWriteService.retrieveCollectionHouseByClientFromHistory(nit);
        if (collectionHouse != null) {
            newRepaymentTransaction.setCollectionHouse(collectionHouse);
        }

        LocalDate recalculateFrom = null;
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()
                || loan.getLoanProductRelatedDetail().getLoanScheduleType().equals(LoanScheduleType.PROGRESSIVE)) {
            recalculateFrom = transactionDate;
        }
        final ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom,
                holidayDetailDto);

        if (this.isLoanExpectedToBeFullyRepaid(loan, transactionDate, repaymentAmount, scheduleGeneratorDTO)) {
            /**
             * Add all missing accrual transactions that happened before the closure date, but not yet posted.
             */
            final Long minimumDaysInArrearsToSuspendLoanAccount = configurationDomainService
                    .retriveMinimumDaysInArrearsToSuspendLoanAccount();
            loanAccrualPlatformService.addTransactionAccrualsAfterLoanClosure(loan.getId(), transactionDate,
                    minimumDaysInArrearsToSuspendLoanAccount);
        }

        final ChangedTransactionDetail changedTransactionDetail = loan.makeRepayment(newRepaymentTransaction,
                defaultLoanLifecycleStateMachine, existingTransactionIds, existingReversedTransactionIds, isRecoveryRepayment,
                scheduleGeneratorDTO, isHolidayValidationDone);

        saveLoanTransactionWithDataIntegrityViolationChecks(newRepaymentTransaction);

        /***
         * TODO Vishwas Batch save is giving me a HibernateOptimisticLockingFailureException, looping and saving for the
         * time being, not a major issue for now as this loop is entered only in edge cases (when a payment is made
         * before the latest payment recorded against the loan)
         ***/
        if (changedTransactionDetail != null) {
            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
            }
            // Trigger transaction replayed event
            replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
        }
        loan.getLoanCustomizationDetail().recordActivity();
        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        if (StringUtils.isNotBlank(noteText)) {
            final Note note = Note.loanTransactionNote(loan, newRepaymentTransaction, noteText);
            this.noteRepository.save(note);
        }

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer, isLoanToLoanTransfer);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        recalculateAccruals(loan);

        setLoanDelinquencyTag(loan, transactionDate);
        if (!repaymentTransactionType.isChargeRefund()) {
            final LoanTransactionBusinessEvent transactionRepaymentEvent = getTransactionRepaymentTypeBusinessEvent(
                    repaymentTransactionType, isRecoveryRepayment, newRepaymentTransaction);
            businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
            businessEventNotifierService.notifyPostBusinessEvent(transactionRepaymentEvent);
            businessEventNotifierService.notifyPostBusinessEvent(new LoanInvoiceGenerationPostBusinessEvent(newRepaymentTransaction));
        }

        // disable all active standing orders linked to this loan if status
        // changes to closed
        disableStandingInstructionsLinkedToClosedLoan(loan);

        if (AccountType.fromInt(loan.getLoanType()).isIndividualAccount()) {
            // Mark Post Dated Check as paid.
            final Set<LoanTransactionToRepaymentScheduleMapping> loanTransactionToRepaymentScheduleMappings = newRepaymentTransaction
                    .getLoanTransactionToRepaymentScheduleMappings();

            if (loanTransactionToRepaymentScheduleMappings != null) {
                for (LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping : loanTransactionToRepaymentScheduleMappings) {
                    LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = loanTransactionToRepaymentScheduleMapping
                            .getLoanRepaymentScheduleInstallment();
                    if (loanRepaymentScheduleInstallment != null) {
                        final boolean isPaid = loanRepaymentScheduleInstallment.isNotFullyPaidOff();
                        PostDatedChecks postDatedChecks = this.postDatedChecksRepository
                                .getPendingPostDatedCheck(loanRepaymentScheduleInstallment);

                        if (postDatedChecks != null) {
                            if (!isPaid) {
                                postDatedChecks.setStatus(PostDatedChecksStatus.POST_DATED_CHECKS_PAID);
                            } else {
                                postDatedChecks.setStatus(PostDatedChecksStatus.POST_DATED_CHECKS_PENDING);
                            }
                            this.postDatedChecksRepository.saveAndFlush(postDatedChecks);
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        setStatusToCanceledOnClosedLoan(loan, transactionDate);
        return newRepaymentTransaction;
    }

    private boolean isLoanExpectedToBeFullyRepaid(final Loan loan, final LocalDate transactionDate, final Money repaymentAmount,
            final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = loan.fetchLoanForeclosureDetail(transactionDate,
                scheduleGeneratorDTO);
        final Money totalOutstandingAmount = loanRepaymentScheduleInstallment.getTotalOutstanding(loan.getCurrency());
        return repaymentAmount.isGreaterThan(totalOutstandingAmount) || repaymentAmount.isEqualTo(totalOutstandingAmount);
    }

    @Override
    public void updateRepaymentInstalmentCharge(LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment,
            Integer numberOfRepayment) {
        if (loanRepaymentScheduleInstallment.getInstallmentNumber() != numberOfRepayment) {

            MonetaryCurrency currency = loanRepaymentScheduleInstallment.getLoan().getCurrency();
            BigDecimal feeChargePortion = BigDecimal.ZERO;
            Collection<LoanCharge> loanCharges = loanRepaymentScheduleInstallment.getLoan().getLoanCharges();
            for (LoanCharge loanCharge : loanCharges) {
                LoanInstallmentCharge installmentCharge = loanCharge
                        .getInstallmentLoanCharge(loanRepaymentScheduleInstallment.getInstallmentNumber());
                if (installmentCharge != null) {
                    feeChargePortion = feeChargePortion.add(installmentCharge.getAmount());
                }

            }

            loanRepaymentScheduleInstallment.updateChargePortion(Money.of(currency, feeChargePortion),
                    loanRepaymentScheduleInstallment.getFeeChargesWaived(currency),
                    loanRepaymentScheduleInstallment.getFeeChargesWrittenOff(currency),
                    loanRepaymentScheduleInstallment.getPenaltyChargesCharged(currency),
                    loanRepaymentScheduleInstallment.getPenaltyChargesWaived(currency),
                    loanRepaymentScheduleInstallment.getPenaltyChargesWrittenOff(currency));

        }
    }

    @Override
    public FeeCalculationHonorario calculateFeeHonorario(LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment,
            BigDecimal repaymentAmount, LocalDate transactionDate) {
        BigDecimal delinquencyValue = BigDecimal.ZERO;
        Integer ageOverdue = loanRepaymentScheduleInstallment.getLoan().getAgeOfOverdueDays(transactionDate).intValue();

        // Retrieve VAT configuration and percentage
        Integer vatConfig = configurationDomainService.retriveIvaConfiguration();
        BigDecimal vatPercentage = BigDecimal.valueOf(vatConfig).divide(new BigDecimal(100), 2, MoneyHelper.getRoundingMode());

        // Retrieve delinquency percentage
        DelinquencyRange delinquencyRange = delinquencyReadPlatformService.retrieveDelinquencyRangeCategeory(ageOverdue);
        if (delinquencyRange != null) {
            delinquencyValue = BigDecimal.valueOf(delinquencyRange.getPercentageValue());
        }

        // Calculate delinquent portion, fee with VAT, fee basis, and fee VAT
        BigDecimal delinquencyRate = delinquencyValue.divide(new BigDecimal(100), 2, MoneyHelper.getRoundingMode());
        BigDecimal delinquentPortion = repaymentAmount
                .divide(BigDecimal.ONE.add(delinquencyRate.multiply(BigDecimal.ONE.add(vatPercentage))), 2, MoneyHelper.getRoundingMode());
        BigDecimal feeWithTax = delinquentPortion.multiply(delinquencyRate.multiply(BigDecimal.ONE.add(vatPercentage))).setScale(0,
                RoundingMode.HALF_UP);
        BigDecimal feeBasis = feeWithTax.divide(BigDecimal.ONE.add(vatPercentage), 0, RoundingMode.HALF_UP);
        BigDecimal feeVat = feeWithTax.subtract(feeBasis).setScale(0, RoundingMode.HALF_UP);
        BigDecimal feeHono = feeVat.add(feeBasis).setScale(0, MoneyHelper.getRoundingMode());

        // Return results as an object
        return new FeeCalculationHonorario(delinquentPortion, feeWithTax, feeBasis, feeVat, feeHono);
    }

    @Override
    public FeeCalculationHonorario calculateFeeHonorario(LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment,
            BigDecimal repaymentAmount) {
        return calculateFeeHonorario(loanRepaymentScheduleInstallment, repaymentAmount, null);
    }

    @Override
    public FeeCalculationHonorario updateCalculationHonoLoanChargeOverDueVat(BigDecimal repaymentAmount,
            LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment, Integer installmentNumberToBeCharged, Long version,
            LocalDate transactionDate) {
        FeeCalculationHonorario feeCalculationHonorario = new FeeCalculationHonorario(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        if (loanRepaymentScheduleInstallment.isObligationsMet()) {
            return feeCalculationHonorario;
        }
        Loan loan = loanRepaymentScheduleInstallment.getLoan();
        Optional<LoanCharge> honoChargeOptional = loan.getActiveCharges().stream()
                .filter(charge -> charge.getChargeCalculation().isFlatHono()).findFirst();

        if (honoChargeOptional.isPresent()) {
            LoanCharge chargeHono = honoChargeOptional.get();
            Optional<LoanCharge> vatCharge = loan.getActiveCharges().stream().filter(vt -> vt.isCustomPercentageBasedOfAnotherCharge()
                    && vt.getCharge().getParentChargeId().equals(chargeHono.getCharge().getId())).findFirst();

            feeCalculationHonorario = this.calculateFeeHonorario(loanRepaymentScheduleInstallment, repaymentAmount, transactionDate);

            CustomChargeHonorarioMap newCustomChargeHonorarioMap = new CustomChargeHonorarioMap();
            newCustomChargeHonorarioMap.setNit("120843958");
            newCustomChargeHonorarioMap.setLoanId(loan.getId());
            newCustomChargeHonorarioMap.setLoanInstallmentNr(installmentNumberToBeCharged);
            newCustomChargeHonorarioMap.setFeeBaseAmount(feeCalculationHonorario.getFeeBasis());
            newCustomChargeHonorarioMap.setFeeTotalAmount(feeCalculationHonorario.getFeeHono());
            newCustomChargeHonorarioMap.setFeeVatAmount(feeCalculationHonorario.getFeeVat());
            newCustomChargeHonorarioMap.setCreatedBy(this.platformSecurityContext.authenticatedUser().getId());
            newCustomChargeHonorarioMap.setCreatedAt(DateUtils.getLocalDateTimeOfTenant());
            newCustomChargeHonorarioMap.setLoanChargeId(chargeHono.getId());
            newCustomChargeHonorarioMap.setVersion(version);
            newCustomChargeHonorarioMap = customChargeHonorarioMapRepository.saveAndFlush(newCustomChargeHonorarioMap);
            if (chargeHono.getCustomChargeHonorarioMaps() != null && !chargeHono.getCustomChargeHonorarioMaps().isEmpty()) {
                chargeHono.getCustomChargeHonorarioMaps().add(newCustomChargeHonorarioMap);
            } else {
                Set<CustomChargeHonorarioMap> customChargeHonorarioMapSet = new HashSet<>();
                customChargeHonorarioMapSet.add(newCustomChargeHonorarioMap);
                chargeHono.setCustomChargeHonorarioMaps(customChargeHonorarioMapSet);
            }
            chargeHono.update(feeCalculationHonorario.getFeeBasis(), null, installmentNumberToBeCharged);

            // Update vat charge
            if (vatCharge.isPresent()) {
                LoanCharge vat = vatCharge.get();
                vat.update(feeCalculationHonorario.getFeeVat(), null, installmentNumberToBeCharged);
            }
            //////////
            loan.updateLoanScheduleAfterCustomChargeApplied();
            saveLoanWithDataIntegrityViolationChecks(loan);
        }
        return feeCalculationHonorario;

    }

    /**
     * Regenerates all CustomChargeHonorarioMaps for a flat honorario charge. This method should be called after
     * clearing existing maps to recreate them.
     *
     * @param loanCharge
     *            The loan charge for which to regenerate the maps
     */
    public void regenerateCustomChargeHonorarioMaps(LoanCharge loanCharge) {

        if (!loanCharge.isFlatHono()) {
            return;
        }

        Loan loan = loanCharge.getLoan();
        if (loan == null) {
            return;
        }

        // Get all installments that need honorario charges
        List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallmentsIgnoringTotalGrace().stream()
                .sorted(Comparator.comparingInt(LoanRepaymentScheduleInstallment::getInstallmentNumber))
                .filter(installment -> !installment.isRecalculatedInterestComponent())
                .filter(installment -> loanCharge.getApplicableFromInstallment() == null
                        || loanCharge.getApplicableFromInstallment() <= installment.getInstallmentNumber())
                .toList();

        // Get the next version number for batch processing
        Long version = customChargeHonorarioMapRepository.getMaxVersionByLoan(loan.getId()) + 1;

        // For each installment, create a CustomChargeHonorarioMap
        for (LoanRepaymentScheduleInstallment installment : installments) {
            // Calculate the outstanding amount for this installment
            BigDecimal installmentOutstandingAmount = installment.getPrincipalOutstanding(loan.getCurrency()).getAmount();

            // Calculate fee honorario for this installment
            FeeCalculationHonorario feeCalculationHonorario = this.calculateFeeHonorario(installment, installmentOutstandingAmount, null);

            // Create new CustomChargeHonorarioMap
            CustomChargeHonorarioMap newCustomChargeHonorarioMap = new CustomChargeHonorarioMap();
            newCustomChargeHonorarioMap.setNit("120843958");
            newCustomChargeHonorarioMap.setLoanId(loan.getId());
            newCustomChargeHonorarioMap.setLoanInstallmentNr(installment.getInstallmentNumber());
            newCustomChargeHonorarioMap.setFeeBaseAmount(feeCalculationHonorario.getFeeBasis());
            newCustomChargeHonorarioMap.setFeeTotalAmount(feeCalculationHonorario.getFeeHono());
            newCustomChargeHonorarioMap.setFeeVatAmount(feeCalculationHonorario.getFeeVat());
            newCustomChargeHonorarioMap.setCreatedBy(this.platformSecurityContext.authenticatedUser().getId());
            newCustomChargeHonorarioMap.setCreatedAt(DateUtils.getLocalDateTimeOfTenant());
            newCustomChargeHonorarioMap.setLoanChargeId(loanCharge.getId());
            newCustomChargeHonorarioMap.setVersion(version);

            // Save the map
            newCustomChargeHonorarioMap = customChargeHonorarioMapRepository.saveAndFlush(newCustomChargeHonorarioMap);

            // Add to the loan charge's maps
            if (loanCharge.getCustomChargeHonorarioMaps() != null && !loanCharge.getCustomChargeHonorarioMaps().isEmpty()) {
                loanCharge.getCustomChargeHonorarioMaps().add(newCustomChargeHonorarioMap);
            } else {
                Set<CustomChargeHonorarioMap> customChargeHonorarioMapSet = new HashSet<>();
                customChargeHonorarioMapSet.add(newCustomChargeHonorarioMap);
                loanCharge.setCustomChargeHonorarioMaps(customChargeHonorarioMapSet);
            }
        }

        // Update the loan charge amounts
        loanCharge.updateAmountOutstanding();

        // Update the loan schedule
        loan.updateLoanScheduleAfterCustomChargeApplied();
        saveLoanWithDataIntegrityViolationChecks(loan);
    }

    private void setStatusToCanceledOnClosedLoan(final Loan loan, final LocalDate transactionDate) {
        if ((loan != null) && (loan.getStatus() != null) && loan.getStatus().isClosedObligationsMet()) {
            final BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepositoryWrapper
                    .getSingleBlockingReasonSettingByReason(BlockingReasonSettingEnum.CREDIT_CANCELADO.getDatabaseString(),
                            BlockLevel.CREDIT.toString());
            if (blockingReasonSetting != null) {
                loan.getLoanCustomizationDetail().setBlockStatus(blockingReasonSetting);
                LoanBlockingReason loanBlockingReason = LoanBlockingReason.instance(loan, blockingReasonSetting,
                        "Préstamo cerrado con saldo cero", transactionDate);
                loanBlockingReasonRepository.saveAndFlush(loanBlockingReason);
            }
        }
    }

    private LoanBusinessEvent getLoanRepaymentTypeBusinessEvent(LoanTransactionType repaymentTransactionType, boolean isRecoveryRepayment,
            Loan loan) {
        LoanBusinessEvent repaymentEvent = null;
        if (repaymentTransactionType.isRepayment()) {
            repaymentEvent = new LoanTransactionMakeRepaymentPreBusinessEvent(loan);
        } else if (repaymentTransactionType.isMerchantIssuedRefund()) {
            repaymentEvent = new LoanTransactionMerchantIssuedRefundPreBusinessEvent(loan);
        } else if (repaymentTransactionType.isPayoutRefund()) {
            repaymentEvent = new LoanTransactionPayoutRefundPreBusinessEvent(loan);
        } else if (repaymentTransactionType.isGoodwillCredit()) {
            repaymentEvent = new LoanTransactionGoodwillCreditPreBusinessEvent(loan);
        } else if (repaymentTransactionType.isChargeRefund()) {
            repaymentEvent = new LoanChargePaymentPreBusinessEvent(loan);
        } else if (isRecoveryRepayment) {
            repaymentEvent = new LoanTransactionRecoveryPaymentPreBusinessEvent(loan);
        } else if (repaymentTransactionType.isDownPayment()) {
            repaymentEvent = new LoanTransactionDownPaymentPreBusinessEvent(loan);
        }
        return repaymentEvent;
    }

    private LoanTransactionBusinessEvent getTransactionRepaymentTypeBusinessEvent(LoanTransactionType repaymentTransactionType,
            boolean isRecoveryRepayment, LoanTransaction transaction) {
        LoanTransactionBusinessEvent repaymentEvent = null;
        if (repaymentTransactionType.isRepayment()) {
            repaymentEvent = new LoanTransactionMakeRepaymentPostBusinessEvent(transaction);
        } else if (repaymentTransactionType.isMerchantIssuedRefund()) {
            repaymentEvent = new LoanTransactionMerchantIssuedRefundPostBusinessEvent(transaction);
        } else if (repaymentTransactionType.isPayoutRefund()) {
            repaymentEvent = new LoanTransactionPayoutRefundPostBusinessEvent(transaction);
        } else if (repaymentTransactionType.isGoodwillCredit()) {
            repaymentEvent = new LoanTransactionGoodwillCreditPostBusinessEvent(transaction);
        } else if (repaymentTransactionType.isChargeRefund()) {
            repaymentEvent = new LoanChargePaymentPostBusinessEvent(transaction);
        } else if (isRecoveryRepayment) {
            repaymentEvent = new LoanTransactionRecoveryPaymentPostBusinessEvent(transaction);
        } else if (repaymentTransactionType.isDownPayment()) {
            repaymentEvent = new LoanTransactionDownPaymentPostBusinessEvent(transaction);
        }
        return repaymentEvent;
    }

    @Override
    public LoanTransaction saveLoanTransactionWithDataIntegrityViolationChecks(LoanTransaction newRepaymentTransaction) {
        try {
            return this.loanTransactionRepository.saveAndFlush(newRepaymentTransaction);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            raiseValidationExceptionForUniqueConstraintViolation(e);
            throw e;
        }
    }

    @Override
    public Loan saveAndFlushLoanWithDataIntegrityViolationChecks(final Loan loan) {
        try {
            return this.loanRepositoryWrapper.saveAndFlush(loan);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            raiseValidationExceptionForUniqueConstraintViolation(e);
            throw e;
        }
    }

    @Override
    public Loan saveLoanWithDataIntegrityViolationChecks(final Loan loan) {
        try {
            return this.loanRepositoryWrapper.save(loan);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            raiseValidationExceptionForUniqueConstraintViolation(e);
            throw e;
        }
    }

    private void raiseValidationExceptionForUniqueConstraintViolation(Exception e) {
        final Throwable realCause = e.getCause();
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.transaction");
        if (realCause.getMessage().toLowerCase().contains("external_id_unique") || realCause.getMessage()
                .contains("duplicate key value violates unique constraint \"m_loan_transaction_external_id_key\"")) {
            baseDataValidator.reset().parameter("externalId").failWithCode("value.must.be.unique");
        }
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors, e);
        }
    }

    @Override
    @Transactional
    public LoanTransaction makeChargePayment(final Loan loan, final Long chargeId, final LocalDate transactionDate,
            final BigDecimal transactionAmount, final PaymentDetail paymentDetail, final String noteText, final ExternalId txnExternalId,
            final Integer transactionType, Integer installmentNumber) {
        boolean isAccountTransfer = true;
        checkClientOrGroupActive(loan);
        if (loan.isChargedOff() && DateUtils.isBefore(transactionDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.earlier.than.charge.off.date", "Loan: "
                    + loan.getId()
                    + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the loan",
                    loan.getId());
        }
        businessEventNotifierService.notifyPreBusinessEvent(new LoanChargePaymentPreBusinessEvent(loan));

        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        final Money paymentAmout = Money.of(loan.getCurrency(), transactionAmount);
        final LoanTransactionType loanTransactionType = LoanTransactionType.fromInt(transactionType);

        final LoanTransaction newPaymentTransaction = LoanTransaction.loanPayment(null, loan.getOffice(), paymentAmout, paymentDetail,
                transactionDate, txnExternalId, loanTransactionType);

        if (loanTransactionType.isRepaymentAtDisbursement()) {
            loan.handlePayDisbursementTransaction(chargeId, newPaymentTransaction, existingTransactionIds, existingReversedTransactionIds);
        } else {
            final boolean allowTransactionsOnHoliday = this.configurationDomainService.allowTransactionsOnHolidayEnabled();
            final List<Holiday> holidays = this.holidayRepository.findByOfficeIdAndGreaterThanDate(loan.getOfficeId(), transactionDate,
                    HolidayStatusType.ACTIVE.getValue());
            final WorkingDays workingDays = this.workingDaysRepository.findOne();
            final boolean allowTransactionsOnNonWorkingDay = this.configurationDomainService.allowTransactionsOnNonWorkingDayEnabled();
            boolean isHolidayEnabled = this.configurationDomainService.isRescheduleRepaymentsOnHolidaysEnabled();
            isHolidayEnabled = loan.getLoanProduct().enableHoliday(isHolidayEnabled);

            HolidayDetailDTO holidayDetailDTO = new HolidayDetailDTO(isHolidayEnabled, holidays, workingDays, allowTransactionsOnHoliday,
                    allowTransactionsOnNonWorkingDay);

            loan.makeChargePayment(chargeId, defaultLoanLifecycleStateMachine, existingTransactionIds, existingReversedTransactionIds,
                    holidayDetailDTO, newPaymentTransaction, installmentNumber);
        }
        saveLoanTransactionWithDataIntegrityViolationChecks(newPaymentTransaction);
        loan.getLoanCustomizationDetail().recordActivity();
        saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        if (StringUtils.isNotBlank(noteText)) {
            final Note note = Note.loanTransactionNote(loan, newPaymentTransaction, noteText);
            this.noteRepository.save(note);
        }

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);

        recalculateAccruals(loan);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        businessEventNotifierService.notifyPostBusinessEvent(new LoanChargePaymentPostBusinessEvent(newPaymentTransaction));
        return newPaymentTransaction;
    }

    private void postJournalEntries(final Loan loanAccount, final List<Long> existingTransactionIds,
            final List<Long> existingReversedTransactionIds, boolean isAccountTransfer) {
        postJournalEntries(loanAccount, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer, false);
    }

    private void postJournalEntries(final Loan loanAccount, final List<Long> existingTransactionIds,
            final List<Long> existingReversedTransactionIds, boolean isAccountTransfer, boolean isLoanToLoanTransfer) {

        final MonetaryCurrency currency = loanAccount.getCurrency();

        List<Map<String, Object>> accountingBridgeData = new ArrayList<>();
        if (loanAccount.isChargedOff()) {
            accountingBridgeData = loanAccount.deriveAccountingBridgeDataForChargeOff(currency.getCode(), existingTransactionIds,
                    existingReversedTransactionIds, isAccountTransfer);
        } else {
            accountingBridgeData.add(loanAccount.deriveAccountingBridgeData(currency.getCode(), existingTransactionIds,
                    existingReversedTransactionIds, isAccountTransfer));
        }
        for (Map<String, Object> accountingData : accountingBridgeData) {
            accountingData.put("isLoanToLoanTransfer", isLoanToLoanTransfer);
            this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingData);
        }

    }

    private void checkClientOrGroupActive(final Loan loan) {
        final Client client = loan.client();
        if (client != null) {
            if (client.isNotActive()) {
                throw new ClientNotActiveException(client.getId());
            }
        }
        final Group group = loan.group();
        if (group != null) {
            if (group.isNotActive()) {
                throw new GroupNotActiveException(group.getId());
            }
        }
    }

    @Override
    public LoanTransaction makeRefund(final Long accountId, final CommandProcessingResultBuilder builderResult,
            final LocalDate transactionDate, final BigDecimal transactionAmount, final PaymentDetail paymentDetail, final String noteText,
            final ExternalId txnExternalId) {
        boolean isAccountTransfer = true;
        final Loan loan = this.loanAccountAssembler.assembleFrom(accountId);
        checkClientOrGroupActive(loan);
        if (loan.isChargedOff() && DateUtils.isBefore(transactionDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.earlier.than.charge.off.date", "Loan: "
                    + loan.getId()
                    + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the loan",
                    loan.getId());
        }
        businessEventNotifierService.notifyPreBusinessEvent(new LoanRefundPreBusinessEvent(loan));
        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        final Money refundAmount = Money.of(loan.getCurrency(), transactionAmount);
        final LoanTransaction newRefundTransaction = LoanTransaction.refund(loan.getOffice(), refundAmount, paymentDetail, transactionDate,
                txnExternalId);
        final boolean allowTransactionsOnHoliday = this.configurationDomainService.allowTransactionsOnHolidayEnabled();
        final List<Holiday> holidays = this.holidayRepository.findByOfficeIdAndGreaterThanDate(loan.getOfficeId(), transactionDate,
                HolidayStatusType.ACTIVE.getValue());
        final WorkingDays workingDays = this.workingDaysRepository.findOne();
        final boolean allowTransactionsOnNonWorkingDay = this.configurationDomainService.allowTransactionsOnNonWorkingDayEnabled();

        loan.makeRefund(newRefundTransaction, defaultLoanLifecycleStateMachine, existingTransactionIds, existingReversedTransactionIds,
                allowTransactionsOnHoliday, holidays, workingDays, allowTransactionsOnNonWorkingDay);

        saveLoanTransactionWithDataIntegrityViolationChecks(newRefundTransaction);
        loan.getLoanCustomizationDetail().recordActivity();
        this.loanRepositoryWrapper.saveAndFlush(loan);

        if (StringUtils.isNotBlank(noteText)) {
            final Note note = Note.loanTransactionNote(loan, newRefundTransaction, noteText);
            this.noteRepository.save(note);
        }

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        businessEventNotifierService.notifyPostBusinessEvent(new LoanRefundPostBusinessEvent(newRefundTransaction));
        builderResult.withEntityId(newRefundTransaction.getId()).withOfficeId(loan.getOfficeId()).withClientId(loan.getClientId())
                .withGroupId(loan.getGroupId());

        return newRefundTransaction;
    }

    @Transactional
    @Override
    public LoanTransaction makeDisburseTransaction(final Long loanId, final LocalDate transactionDate, final BigDecimal transactionAmount,
            final PaymentDetail paymentDetail, final String noteText, final ExternalId txnExternalId) {
        return makeDisburseTransaction(loanId, transactionDate, transactionAmount, paymentDetail, noteText, txnExternalId, false);
    }

    @Transactional
    @Override
    public LoanTransaction makeDisburseTransaction(final Long loanId, final LocalDate transactionDate, final BigDecimal transactionAmount,
            final PaymentDetail paymentDetail, final String noteText, final ExternalId txnExternalId, final boolean isLoanToLoanTransfer) {
        final Loan loan = this.loanAccountAssembler.assembleFrom(loanId);
        checkClientOrGroupActive(loan);
        if (loan.isChargedOff() && DateUtils.isBefore(transactionDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.earlier.than.charge.off.date", "Loan: "
                    + loan.getId()
                    + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the loan",
                    loan.getId());
        }
        boolean isAccountTransfer = true;
        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();
        final Money amount = Money.of(loan.getCurrency(), transactionAmount);
        LoanTransaction disbursementTransaction = LoanTransaction.disbursement(loan.getOffice(), amount, paymentDetail, transactionDate,
                txnExternalId);

        // Subtract Previous loan outstanding balance from netDisbursalAmount
        loan.deductFromNetDisbursalAmount(transactionAmount);

        disbursementTransaction.updateLoan(loan);
        loan.addLoanTransaction(disbursementTransaction);
        saveLoanTransactionWithDataIntegrityViolationChecks(disbursementTransaction);
        loan.getLoanCustomizationDetail().recordActivity();
        saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        if (StringUtils.isNotBlank(noteText)) {
            final Note note = Note.loanTransactionNote(loan, disbursementTransaction, noteText);
            this.noteRepository.save(note);
        }

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer, isLoanToLoanTransfer);
        return disbursementTransaction;
    }

    @Override
    public void reverseTransfer(final LoanTransaction loanTransaction) {
        if (loanTransaction.getLoan().isChargedOff()
                && DateUtils.isBefore(loanTransaction.getTransactionDate(), loanTransaction.getLoan().getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.earlier.than.charge.off.date",
                    "Loan transaction: " + loanTransaction.getId()
                            + " reversal is not allowed before or on the date when the loan got charged-off",
                    loanTransaction.getId());
        }
        loanTransaction.reverse();
        saveLoanTransactionWithDataIntegrityViolationChecks(loanTransaction);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.fineract.portfolio.loanaccount.domain.LoanAccountDomainService
     * #recalculateAccruals(org.apache.fineract.portfolio.loanaccount.domain. Loan)
     */
    @Override
    public void recalculateAccruals(Loan loan) {
        recalculateAccruals(loan, loan.repaymentScheduleDetail().isInterestRecalculationEnabled());
    }

    @Override
    public void setLoanDelinquencyTag(final Loan loan, final LocalDate transactionDate) {
        LoanScheduleDelinquencyData loanDelinquencyData = new LoanScheduleDelinquencyData(loan.getId(), transactionDate, null, loan);
        final List<LoanDelinquencyAction> savedDelinquencyList = delinquencyReadPlatformService
                .retrieveLoanDelinquencyActions(loan.getId());
        List<LoanDelinquencyActionData> effectiveDelinquencyList = delinquencyEffectivePauseHelper
                .calculateEffectiveDelinquencyList(savedDelinquencyList);
        loanDelinquencyData = this.delinquencyWritePlatformService.calculateDelinquencyData(loanDelinquencyData, effectiveDelinquencyList);
        log.debug("Processing Loan {} with {} overdue days since date {}", loanDelinquencyData.getLoanId(),
                loanDelinquencyData.getOverdueDays(), loanDelinquencyData.getOverdueSinceDate());
        // Set or Unset the Delinquency Classification Tag
        if (loanDelinquencyData.getOverdueDays() > 0) {
            this.delinquencyWritePlatformService.applyDelinquencyTagToLoan(loanDelinquencyData, effectiveDelinquencyList);
        } else {
            this.delinquencyWritePlatformService.removeDelinquencyTagToLoan(loanDelinquencyData.getLoan());
        }
    }

    @Override
    public void setLoanDelinquencyTag(Loan loan, LocalDate transactionDate, List<LoanDelinquencyActionData> effectiveDelinquencyList) {
        LoanScheduleDelinquencyData loanDelinquencyData = new LoanScheduleDelinquencyData(loan.getId(), transactionDate, null, loan);
        loanDelinquencyData = this.delinquencyWritePlatformService.calculateDelinquencyData(loanDelinquencyData, effectiveDelinquencyList);
        log.debug("Processing Loan {} with {} overdue days since date {}", loanDelinquencyData.getLoanId(),
                loanDelinquencyData.getOverdueDays(), loanDelinquencyData.getOverdueSinceDate());
        // Set or Unset the Delinquency Classification Tag
        if (loanDelinquencyData.getOverdueDays() > 0) {
            this.delinquencyWritePlatformService.applyDelinquencyTagToLoan(loanDelinquencyData, effectiveDelinquencyList);
        } else {
            this.delinquencyWritePlatformService.removeDelinquencyTagToLoan(loanDelinquencyData.getLoan());
        }
    }

    @Override
    public void recalculateAccruals(Loan loan, boolean isInterestCalculationHappened) {
        LocalDate accruedTill = loan.getAccruedTill();
        if (!loan.isPeriodicAccrualAccountingEnabledOnLoanProduct() || !isInterestCalculationHappened || accruedTill == null || loan.isNpa()
                || !loan.getStatus().isActive() || loan.isChargedOff()) {
            return;
        }

        boolean isOrganisationDateEnabled = this.configurationDomainService.isOrganisationstartDateEnabled();
        LocalDate organisationStartDate = DateUtils.getBusinessLocalDate();
        if (isOrganisationDateEnabled) {
            organisationStartDate = this.configurationDomainService.retrieveOrganisationStartDate();
        }
        Collection<LoanScheduleAccrualData> loanScheduleAccrualList = new ArrayList<>();
        List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
        Long loanId = loan.getId();
        Long officeId = loan.getOfficeId();
        LocalDate accrualStartDate = null;
        PeriodFrequencyType repaymentFrequency = loan.repaymentScheduleDetail().getRepaymentPeriodFrequencyType();
        Integer repayEvery = loan.repaymentScheduleDetail().getRepayEvery();
        LocalDate interestCalculatedFrom = loan.getInterestChargedFromDate();
        Long loanProductId = loan.productId();
        MonetaryCurrency currency = loan.getCurrency();
        ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);
        CurrencyData currencyData = applicationCurrency.toData();
        Set<LoanCharge> loanCharges = loan.getActiveCharges();
        int firstNormalInstallmentNumber = LoanRepaymentScheduleProcessingWrapper.fetchFirstNormalInstallmentNumber(installments);

        for (LoanRepaymentScheduleInstallment installment : installments) {
            if (DateUtils.isAfter(installment.getDueDate(), loan.getMaturityDate())) {
                accruedTill = DateUtils.getBusinessLocalDate();
            }
            if (!isOrganisationDateEnabled || DateUtils.isBefore(organisationStartDate, installment.getDueDate())) {
                boolean isFirstNormalInstallment = installment.getInstallmentNumber().equals(firstNormalInstallmentNumber);
                generateLoanScheduleAccrualData(accruedTill, loanScheduleAccrualList, loanId, officeId, accrualStartDate,
                        repaymentFrequency, repayEvery, interestCalculatedFrom, loanProductId, currency, currencyData, loanCharges,
                        installment, isFirstNormalInstallment);
            }
        }

        if (!loanScheduleAccrualList.isEmpty()) {
            try {
                this.loanAccrualPlatformService.addPeriodicAccruals(accruedTill, loanScheduleAccrualList);
            } catch (MultiException e) {
                String globalisationMessageCode = "error.msg.accrual.exception";
                throw new GeneralPlatformDomainRuleException(globalisationMessageCode, e.getMessage(), e);
            }
        }

    }

    @Override
    public void recalculateInterestAccrualsOnMaximumLegalRate(final Loan loan, final LocalDate rescheduleFromDate) {
        final LocalDate interestAccruedTillDate = rescheduleFromDate.minusDays(1L);
        final LocalDate accrualDate = DateUtils.getLocalDateOfTenant().minusDays(1);
        final List<LoanTransaction> interestAccrualTransactions = loan.getLoanTransactions().stream()
                .filter(LoanTransaction::isAccrualTransaction).filter(LoanTransaction::isDailyAccrual)
                .filter(ltx -> ltx.getInterestPortion(loan.getCurrency()).isGreaterThanZero())
                .filter(loanTransaction -> !DateUtils.isBefore(loanTransaction.getTransactionDate(), rescheduleFromDate)).toList();
        interestAccrualTransactions.forEach(LoanTransaction::reverse);
        loan.setInterestAccruedTill(interestAccruedTillDate);
        this.saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
    }

    private void generateLoanScheduleAccrualData(final LocalDate accruedTill,
            final Collection<LoanScheduleAccrualData> loanScheduleAccrualDatas, final Long loanId, Long officeId,
            final LocalDate accrualStartDate, final PeriodFrequencyType repaymentFrequency, final Integer repayEvery,
            final LocalDate interestCalculatedFrom, final Long loanProductId, final MonetaryCurrency currency,
            final CurrencyData currencyData, final Set<LoanCharge> loanCharges, final LoanRepaymentScheduleInstallment installment,
            boolean isFirstNormalInstallment) {

        if (!DateUtils.isBefore(accruedTill, installment.getDueDate()) || (DateUtils.isAfter(accruedTill, installment.getFromDate())
                && !DateUtils.isAfter(accruedTill, installment.getDueDate()))) {
            BigDecimal dueDateFeeIncome = BigDecimal.ZERO;
            BigDecimal dueDatePenaltyIncome = BigDecimal.ZERO;
            LocalDate chargesTillDate = installment.getDueDate();
            if (!DateUtils.isAfter(accruedTill, installment.getDueDate())) {
                chargesTillDate = accruedTill;
            }

            for (final LoanCharge loanCharge : loanCharges) {
                boolean isDue = isFirstNormalInstallment
                        ? loanCharge.isDueForCollectionFromIncludingAndUpToAndIncluding(installment.getFromDate(), chargesTillDate)
                        : loanCharge.isDueForCollectionFromAndUpToAndIncluding(installment.getFromDate(), chargesTillDate);
                if (isDue) {
                    if (loanCharge.isFeeCharge()) {
                        dueDateFeeIncome = dueDateFeeIncome.add(loanCharge.amount());
                    } else if (loanCharge.isPenaltyCharge()) {
                        dueDatePenaltyIncome = dueDatePenaltyIncome.add(loanCharge.amount());
                    }
                }
            }

            LoanScheduleAccrualData accrualData = new LoanScheduleAccrualData(loanId, officeId, installment.getInstallmentNumber(),
                    accrualStartDate, repaymentFrequency, repayEvery, installment.getDueDate(), installment.getFromDate(),
                    installment.getId(), loanProductId, installment.getInterestCharged(currency).getAmount(),
                    installment.getFeeChargesCharged(currency).getAmount(), installment.getPenaltyChargesCharged(currency).getAmount(),
                    installment.getInterestAccrued(currency).getAmount(), installment.getFeeAccrued(currency).getAmount(),
                    installment.getPenaltyAccrued(currency).getAmount(), currencyData, interestCalculatedFrom,
                    installment.getInterestWaived(currency).getAmount());
            loanScheduleAccrualDatas.add(accrualData);

        }
    }

    private void updateLoanTransaction(final Long loanTransactionId, final LoanTransaction newLoanTransaction) {
        final AccountTransferTransaction transferTransaction = this.accountTransferRepository.findByToLoanTransactionId(loanTransactionId);
        if (transferTransaction != null) {
            transferTransaction.updateToLoanTransaction(newLoanTransaction);
            this.accountTransferRepository.save(transferTransaction);
        }
    }

    @Override
    public LoanTransaction creditBalanceRefund(final Loan loan, final LocalDate transactionDate, final BigDecimal transactionAmount,
            final String noteText, final ExternalId externalId, PaymentDetail paymentDetail) {
        if (transactionDate.isAfter(DateUtils.getBusinessLocalDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.in.the.future",
                    "Loan: " + loan.getId() + ", Credit Balance Refund transaction cannot be created for the future.", loan.getId());
        }
        if (loan.isChargedOff() && DateUtils.isBefore(transactionDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.earlier.than.charge.off.date", "Loan: "
                    + loan.getId()
                    + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the loan",
                    loan.getId());
        }

        businessEventNotifierService.notifyPreBusinessEvent(new LoanCreditBalanceRefundPreBusinessEvent(loan));
        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        final Money refundAmount = Money.of(loan.getCurrency(), transactionAmount);
        LoanTransaction newCreditBalanceRefundTransaction = LoanTransaction.creditBalanceRefund(loan, loan.getOffice(), refundAmount,
                transactionDate, externalId, paymentDetail);

        loan.creditBalanceRefund(newCreditBalanceRefundTransaction, defaultLoanLifecycleStateMachine, existingTransactionIds,
                existingReversedTransactionIds);

        newCreditBalanceRefundTransaction = this.loanTransactionRepository.saveAndFlush(newCreditBalanceRefundTransaction);

        if (StringUtils.isNotBlank(noteText)) {
            final Note note = Note.loanTransactionNote(loan, newCreditBalanceRefundTransaction, noteText);
            this.noteRepository.save(note);
        }

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, false);
        recalculateAccruals(loan);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        businessEventNotifierService
                .notifyPostBusinessEvent(new LoanCreditBalanceRefundPostBusinessEvent(newCreditBalanceRefundTransaction));

        return newCreditBalanceRefundTransaction;
    }

    @Override
    public LoanTransaction makeRefundForActiveLoan(Long accountId, CommandProcessingResultBuilder builderResult, LocalDate transactionDate,
            BigDecimal transactionAmount, PaymentDetail paymentDetail, String noteText, ExternalId txnExternalId) {
        final Loan loan = this.loanAccountAssembler.assembleFrom(accountId);
        checkClientOrGroupActive(loan);
        businessEventNotifierService.notifyPreBusinessEvent(new LoanRefundPreBusinessEvent(loan));
        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        final Money refundAmount = Money.of(loan.getCurrency(), transactionAmount);
        if (loan.isChargedOff() && DateUtils.isBefore(transactionDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.earlier.than.charge.off.date", "Loan: "
                    + loan.getId()
                    + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the loan",
                    loan.getId());
        }
        final LoanTransaction newRefundTransaction = LoanTransaction.refundForActiveLoan(loan.getOffice(), refundAmount, paymentDetail,
                transactionDate, txnExternalId);
        final boolean allowTransactionsOnHoliday = this.configurationDomainService.allowTransactionsOnHolidayEnabled();
        final List<Holiday> holidays = this.holidayRepository.findByOfficeIdAndGreaterThanDate(loan.getOfficeId(), transactionDate,
                HolidayStatusType.ACTIVE.getValue());
        final WorkingDays workingDays = this.workingDaysRepository.findOne();
        final boolean allowTransactionsOnNonWorkingDay = this.configurationDomainService.allowTransactionsOnNonWorkingDayEnabled();

        loan.makeRefundForActiveLoan(newRefundTransaction, defaultLoanLifecycleStateMachine, existingTransactionIds,
                existingReversedTransactionIds, allowTransactionsOnHoliday, holidays, workingDays, allowTransactionsOnNonWorkingDay);

        this.loanTransactionRepository.saveAndFlush(newRefundTransaction);
        loan.getLoanCustomizationDetail().recordActivity();
        saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        if (StringUtils.isNotBlank(noteText)) {
            final Note note = Note.loanTransactionNote(loan, newRefundTransaction, noteText);
            this.noteRepository.save(note);
        }

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, false);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        recalculateAccruals(loan);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        businessEventNotifierService.notifyPostBusinessEvent(new LoanRefundPostBusinessEvent(newRefundTransaction));

        builderResult.withEntityId(newRefundTransaction.getId()).withOfficeId(loan.getOfficeId()).withClientId(loan.getClientId())
                .withGroupId(loan.getGroupId());

        return newRefundTransaction;
    }

    @SuppressWarnings("all")
    @Override
    public LoanTransaction foreCloseLoan(Loan loan, final LocalDate foreClosureDate, final String noteText, final ExternalId externalId,
            Map<String, Object> changes, boolean isForCloureAction) {
        if (loan.isChargedOff() && DateUtils.isBefore(foreClosureDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.earlier.than.charge.off.date", "Loan: "
                    + loan.getId()
                    + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the loan",
                    loan.getId());
        }
        businessEventNotifierService.notifyPreBusinessEvent(new LoanForeClosurePreBusinessEvent(loan));
        MonetaryCurrency currency = loan.getCurrency();
        List<LoanTransaction> newTransactions = new ArrayList<>();

        final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
        final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());
        final ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, null);
        final LoanRepaymentScheduleInstallment foreCloseDetail = loan.fetchLoanForeclosureDetail(foreClosureDate, scheduleGeneratorDTO);
        if (Boolean.TRUE.equals(loan.isPeriodicAccrualAccountingEnabledOnLoanProduct())
                && (loan.getAccruedTill() == null || !DateUtils.isEqual(foreClosureDate, loan.getAccruedTill()))) {
            loan.reverseAccrualsAfter(foreClosureDate);
            Money[] accruedReceivables = loan.getReceivableIncome(foreClosureDate);
            Money interestPortion = foreCloseDetail.getInterestCharged(currency).minus(accruedReceivables[0]);
            Money feePortion = foreCloseDetail.getFeeChargesCharged(currency).minus(accruedReceivables[1]);
            // If foreclosure or cancel on disbursement date
            LoanRepaymentScheduleInstallment inst = loan.getRepaymentScheduleInstallments().get(0);
            if (DateUtils.isEqual(foreClosureDate, inst.getFromDate())) {
                feePortion = inst.getFeeChargesOutstanding(currency);
                if (loan.isAnulado()) {
                    feePortion = loan.getPendingHonoAmountOfAnuladoLoanForInstallment(loan, inst.getInstallmentNumber());
                }
            }
            Money penaltyPortion = foreCloseDetail.getPenaltyChargesCharged(currency).minus(accruedReceivables[2]);

            Money total = interestPortion.plus(feePortion).plus(penaltyPortion);
            if (total.isGreaterThanZero()) {
                ExternalId accrualExternalId = externalIdFactory.create();
                LoanTransaction accrualTransaction = LoanTransaction.accrueTransaction(loan, loan.getOffice(), foreClosureDate,
                        total.getAmount(), interestPortion.getAmount(), feePortion.getAmount(), penaltyPortion.getAmount(),
                        accrualExternalId);
                LocalDate fromDate = loan.getDisbursementDate();
                if (loan.getAccruedTill() != null) {
                    fromDate = loan.getAccruedTill();
                }
                final ClientAdditionalFieldsData clientAdditionalInformation = this.clientReadPlatformService
                        .retrieveClientAdditionalData(loan.getClientId());
                final String nit = ObjectUtils.defaultIfNull(clientAdditionalInformation.getNit(), clientAdditionalInformation.getCedula());
                final CollectionHouseConfiguration collectionHouse = this.collectionHouseReadWriteService
                        .retrieveCollectionHouseByClientFromHistory(nit);
                if (collectionHouse != null) {
                    accrualTransaction.setCollectionHouse(collectionHouse);
                }
                newTransactions.add(accrualTransaction);
                loan.addLoanTransaction(accrualTransaction);
                Set<LoanChargePaidBy> accrualCharges = accrualTransaction.getLoanChargesPaid();
                for (LoanCharge loanCharge : loan.getActiveCharges()) {
                    boolean isDue = DateUtils.isEqual(fromDate, loan.getDisbursementDate())
                            ? loanCharge.isDueForCollectionFromIncludingAndUpToAndIncluding(fromDate, foreClosureDate)
                            : loanCharge.isDueForCollectionFromAndUpToAndIncluding(fromDate, foreClosureDate);
                    if (loanCharge.isActive() && !loanCharge.isPaid() && (isDue || loanCharge.isInstalmentFee())) {
                        final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(accrualTransaction, loanCharge,
                                loanCharge.getAmountOutstanding(currency).getAmount(), null);
                        accrualCharges.add(loanChargePaidBy);
                    }
                }
            }
        }

        Money interestPayable = foreCloseDetail.getInterestCharged(currency);
        Money feePayable = foreCloseDetail.getFeeChargesCharged(currency);
        Money penaltyPayable = foreCloseDetail.getPenaltyChargesCharged(currency);
        Money payPrincipal = foreCloseDetail.getPrincipal(currency);
        loan.setForeClosing(true);
        loan.updateInstallmentsPostDate(foreClosureDate, scheduleGeneratorDTO);
        LoanTransaction payment = null;

        if (payPrincipal.plus(interestPayable).plus(feePayable).plus(penaltyPayable).isGreaterThanZero()) {
            BigDecimal honoFee;
            if (isForCloureAction) {
                honoFee = calculateHonoForForeclosure(loan,
                        payPrincipal.plus(interestPayable).plus(feePayable).plus(penaltyPayable).getAmount(), foreClosureDate);
                feePayable = feePayable.add(honoFee);
            }
            final PaymentDetail paymentDetail = null;
            payment = LoanTransaction.repayment(loan.getOffice(), payPrincipal.plus(interestPayable).plus(feePayable).plus(penaltyPayable),
                    paymentDetail, foreClosureDate, externalId);
            payment.updateLoan(loan);

            final ClientAdditionalFieldsData clientAdditionalInformation = this.clientReadPlatformService
                    .retrieveClientAdditionalData(loan.getClientId());
            final String nit = ObjectUtils.defaultIfNull(clientAdditionalInformation.getNit(), clientAdditionalInformation.getCedula());
            final CollectionHouseConfiguration collectionHouse = this.collectionHouseReadWriteService
                    .retrieveCollectionHouseByClientFromHistory(nit);
            if (collectionHouse != null) {
                payment.setCollectionHouse(collectionHouse);
            }
            payment.setForeclosure(isForCloureAction);

            newTransactions.add(payment);
        }

        /**
         * Add all missing accrual transactions that happened before the fore closure date, but not yet posted.
         */
        final Long minimumDaysInArrearsToSuspendLoanAccount = configurationDomainService.retriveMinimumDaysInArrearsToSuspendLoanAccount();
        loanAccrualPlatformService.addTransactionAccrualsAfterLoanClosure(loan.getId(), foreClosureDate,
                minimumDaysInArrearsToSuspendLoanAccount);

        List<Long> transactionIds = new ArrayList<>();
        final ChangedTransactionDetail changedTransactionDetail = loan.handleForeClosureTransactions(payment,
                defaultLoanLifecycleStateMachine, scheduleGeneratorDTO);

        /***
         * TODO Vishwas Batch save is giving me a HibernateOptimisticLockingFailureException, looping and saving for the
         * time being, not a major issue for now as this loop is entered only in edge cases (when a payment is made
         * before the latest payment recorded against the loan)
         ***/

        for (LoanTransaction newTransaction : newTransactions) {
            saveLoanTransactionWithDataIntegrityViolationChecks(newTransaction);
            transactionIds.add(newTransaction.getId());
        }
        changes.put("transactions", transactionIds);
        changes.put("eventAmount", payPrincipal.getAmount().negate());

        if (changedTransactionDetail != null) {
            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
            }
            // Trigger transaction replayed event
            replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
        }
        for (LoanRepaymentScheduleInstallment installment : loan.getRepaymentScheduleInstallments()) {
            if (installment.isOverdueOn(foreClosureDate)) {
                updateRepaymentInstalmentCharge(installment, installment.getInstallmentNumber());
            }
        }
        if (loan.getLoanSummary().isRepaidInFull(loan.getCurrency())) {
            loan.closeAsObligationsMet(foreClosureDate, this.platformSecurityContext.authenticatedUser());
        }
        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
            final Note note = Note.loanNote(loan, noteText);
            this.noteRepository.save(note);
        }

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, false);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        businessEventNotifierService.notifyPostBusinessEvent(new LoanForeClosurePostBusinessEvent(payment));
        businessEventNotifierService.notifyPostBusinessEvent(new LoanInvoiceGenerationPostBusinessEvent(payment));
        return payment;
    }

    private BigDecimal calculateHonoForForeclosure(Loan loan, BigDecimal transactionAmount, LocalDate transactionDate) {
        /// SU-516 Calculate Hono Charge
        BigDecimal cumulativeHonoFee = BigDecimal.ZERO;
        BigDecimal cumulativeVatFee = BigDecimal.ZERO;
        Optional<LoanCharge> honoChargeOptional = loan.getLoanCharges().stream().filter(LoanCharge::isFlatHono).findFirst();
        if (honoChargeOptional.isPresent() && loan.getAgeOfOverdueDays(DateUtils.getBusinessLocalDate()) > 0) {
            LoanCharge honoCharge = honoChargeOptional.get();
            Optional<LoanCharge> vatChargeOptional = loan.getLoanCharges().stream()
                    .filter(chg -> chg.isCustomPercentageBasedOfAnotherCharge()
                            && chg.getCharge().getParentChargeId().equals(honoCharge.getCharge().getId()))
                    .findFirst();
            Money remainingAmount = Money.of(loan.getCurrency(), transactionAmount);
            Integer installmentNumber = -1;
            Long version = 0L;
            if (honoCharge.getCustomChargeHonorarioMaps() != null && !honoCharge.getCustomChargeHonorarioMaps().isEmpty()) {
                for (CustomChargeHonorarioMap map : honoCharge.getCustomChargeHonorarioMaps()) {
                    if (map.getVersion() > version) {
                        version = map.getVersion();
                    }
                }
            }
            version = version + 1;
            for (LoanRepaymentScheduleInstallment installment : loan.getRepaymentScheduleInstallments()) {
                if (installment.isOverdueOn(transactionDate) && !installment.isObligationsMet()) {
                    if (installmentNumber == -1) {
                        installmentNumber = installment.getInstallmentNumber();
                    }
                    BigDecimal installmentOutstandingAmount = installment.getTotalOutstanding(loan.getCurrency()).getAmount();
                    FeeCalculationHonorario fee = new FeeCalculationHonorario(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO);
                    if (remainingAmount.isGreaterThanZero()
                            && remainingAmount.isGreaterThanOrEqualTo(installment.getTotalOutstanding(loan.getCurrency()))) {
                        fee = this.updateCalculationHonoLoanChargeOverDueVat(installmentOutstandingAmount, installment, installmentNumber,
                                version, transactionDate);
                        remainingAmount = remainingAmount.minus(installmentOutstandingAmount);

                    } else {
                        fee = this.updateCalculationHonoLoanChargeOverDueVat(remainingAmount.getAmount(), installment, installmentNumber,
                                version, transactionDate);
                    }
                    cumulativeHonoFee = cumulativeHonoFee.add(fee.getFeeBasis());
                    if (vatChargeOptional.isPresent()) {
                        cumulativeVatFee = cumulativeVatFee.add(fee.getFeeVat());
                    }

                    if (remainingAmount.isZero() || remainingAmount.isLessThanZero()) {
                        break;
                    }

                }
            }

            // Add Accrual Transaction
            boolean isSuspendedAccount = false;
            Long minimumDaysInArrearsToSuspendLoanAccount = this.configurationDomainService
                    .retriveMinimumDaysInArrearsToSuspendLoanAccount();
            if (minimumDaysInArrearsToSuspendLoanAccount == null) {
                minimumDaysInArrearsToSuspendLoanAccount = 90L;
            }
            LocalDate arrearsStartDate = LocalDate.now();
            try {
                arrearsStartDate = this.jdbcTemplate.queryForObject(
                        "select overdue_since_date_derived aging_days from m_loan_arrears_aging mlaa where mlaa.loan_id =?",
                        LocalDate.class, loan.getId());
            } catch (final EmptyResultDataAccessException e) {
                // not in arrears
                arrearsStartDate = LocalDate.now();
            }
            long days = 0L;
            if (arrearsStartDate != null) {
                days = arrearsStartDate.until(transactionDate, ChronoUnit.DAYS);
            }
            if (days >= minimumDaysInArrearsToSuspendLoanAccount) {
                isSuspendedAccount = true;
            }

            Money accrualAmount = Money.of(loan.getCurrency(), cumulativeHonoFee.add(cumulativeVatFee));
            final LoanTransaction applyLoanChargeTransaction = LoanTransaction.accrueInstallmentCharge(loan, loan.getOffice(),
                    accrualAmount, transactionDate, accrualAmount, Money.zero(loan.getCurrency()), ExternalId.empty());
            if (isSuspendedAccount) {
                applyLoanChargeTransaction.markAsOccurredOnSuspendedAccount();
            }
            final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(applyLoanChargeTransaction, honoCharge, cumulativeHonoFee,
                    installmentNumber);
            applyLoanChargeTransaction.getLoanChargesPaid().add(loanChargePaidBy);

            if (vatChargeOptional.isPresent()) {
                LoanCharge vat = vatChargeOptional.get();

                final LoanChargePaidBy vatChargePaidBy = new LoanChargePaidBy(applyLoanChargeTransaction, vat, cumulativeVatFee,
                        installmentNumber);
                applyLoanChargeTransaction.getLoanChargesPaid().add(vatChargePaidBy);
            }
            final ClientAdditionalFieldsData clientAdditionalInformation = this.clientReadPlatformService
                    .retrieveClientAdditionalData(loan.getClientId());
            final String nit = ObjectUtils.defaultIfNull(clientAdditionalInformation.getNit(), clientAdditionalInformation.getCedula());
            final CollectionHouseConfiguration collectionHouse = this.collectionHouseReadWriteService
                    .retrieveCollectionHouseByClientFromHistory(nit);
            if (collectionHouse != null) {
                applyLoanChargeTransaction.setCollectionHouse(collectionHouse);
            }
            loan.addLoanTransaction(applyLoanChargeTransaction);
        }
        return cumulativeHonoFee.add(cumulativeVatFee);
        //////
    }

    @Override
    public LoanTransaction claimLoan(Loan loan, final LocalDate claimDate, final ExternalId externalId, Map<String, Object> changes) {
        if (loan.isChargedOff() && DateUtils.isBefore(claimDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.earlier.than.charge.off.date", "Loan: "
                    + loan.getId()
                    + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the loan",
                    loan.getId());
        }
        MonetaryCurrency currency = loan.getCurrency();
        List<LoanTransaction> newTransactions = new ArrayList<>();

        final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
        final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());
        final ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, null);
        final LoanRepaymentScheduleInstallment foreCloseDetail = loan.fetchLoanForeclosureDetail(claimDate, scheduleGeneratorDTO);
        if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()
                && (loan.getAccruedTill() == null || !DateUtils.isEqual(claimDate, loan.getAccruedTill()))) {
            loan.reverseAccrualsAfter(claimDate);
            Money[] accruedReceivables = loan.getReceivableIncome(claimDate);
            Money interestPortion = foreCloseDetail.getInterestCharged(currency).minus(accruedReceivables[0]);
            Money feePortion = foreCloseDetail.getFeeChargesCharged(currency).minus(accruedReceivables[1]);
            Money penaltyPortion = foreCloseDetail.getPenaltyChargesCharged(currency).minus(accruedReceivables[2]);
            Money total = interestPortion.plus(feePortion).plus(penaltyPortion);
            if (total.isGreaterThanZero()) {
                ExternalId accrualExternalId = externalIdFactory.create();
                LoanTransaction accrualTransaction = LoanTransaction.accrueTransaction(loan, loan.getOffice(), claimDate, total.getAmount(),
                        interestPortion.getAmount(), feePortion.getAmount(), penaltyPortion.getAmount(), accrualExternalId);
                LocalDate fromDate = loan.getDisbursementDate();
                if (loan.getAccruedTill() != null) {
                    fromDate = loan.getAccruedTill();
                }
                newTransactions.add(accrualTransaction);
                loan.addLoanTransaction(accrualTransaction);
                Set<LoanChargePaidBy> accrualCharges = accrualTransaction.getLoanChargesPaid();
                for (LoanCharge loanCharge : loan.getActiveCharges()) {
                    boolean isDue = DateUtils.isEqual(fromDate, loan.getDisbursementDate())
                            ? loanCharge.isDueForCollectionFromIncludingAndUpToAndIncluding(fromDate, claimDate)
                            : loanCharge.isDueForCollectionFromAndUpToAndIncluding(fromDate, claimDate);
                    if (loanCharge.isActive() && !loanCharge.isPaid() && (isDue || loanCharge.isInstalmentFee())) {
                        final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(accrualTransaction, loanCharge,
                                loanCharge.getAmountOutstanding(currency).getAmount(), null);
                        accrualCharges.add(loanChargePaidBy);
                    }
                }
            }
        }

        Money interestPayable = foreCloseDetail.getInterestCharged(currency);
        Money feePayable = foreCloseDetail.getFeeChargesCharged(currency);
        Money penaltyPayable = foreCloseDetail.getPenaltyChargesCharged(currency);
        Money payPrincipal = foreCloseDetail.getPrincipal(currency);

        /////////////
        BigDecimal outstandingFeeAmount = BigDecimal.ZERO;
        if (loan.claimType() != null) {
            for (final LoanRepaymentScheduleInstallment installment : loan.getRepaymentScheduleInstallments()) {
                if (DateUtils.isAfter(claimDate, installment.getDueDate())) {
                    if (loan.claimType().equals("insurance")) {
                        outstandingFeeAmount = outstandingFeeAmount
                                .add(installment.getFeeChargesOutstandingByType(currency, "MandatoryInsurance").getAmount());
                    } else if (loan.claimType().equals("guarantor")) {
                        outstandingFeeAmount = outstandingFeeAmount
                                .add(installment.getFeeChargesOutstandingByType(currency, "Aval").getAmount());
                    }
                    outstandingFeeAmount = outstandingFeeAmount
                            .add(installment.getFeeChargesOutstandingByType(currency, "Honorarios").getAmount());
                }
            }
        }
        /////////////
        loan.updateInstallmentsPostDate(claimDate, scheduleGeneratorDTO);

        if (loan.claimType() != null) {
            LoanRepaymentScheduleInstallment lastInstallment = loan.getLastLoanRepaymentScheduleInstallment();
            if (loan.claimType().equals("insurance")) {
                outstandingFeeAmount = outstandingFeeAmount
                        .add(lastInstallment.getFeeChargesOutstandingByType(currency, "MandatoryInsurance").getAmount());
            } else if (loan.claimType().equals("guarantor")) {
                outstandingFeeAmount = outstandingFeeAmount
                        .add(lastInstallment.getFeeChargesOutstandingByType(currency, "Aval").getAmount());
            }
            outstandingFeeAmount = outstandingFeeAmount
                    .add(lastInstallment.getFeeChargesOutstandingByType(currency, "Honorarios").getAmount());
            feePayable = feePayable.minus(outstandingFeeAmount);
        }

        LoanTransaction payment = null;
        if (payPrincipal.plus(interestPayable).plus(feePayable).plus(penaltyPayable).isGreaterThanZero()) {
            final PaymentDetail paymentDetail = null;
            payment = LoanTransaction.repayment(loan.getOffice(), payPrincipal.plus(interestPayable).plus(feePayable).plus(penaltyPayable),
                    paymentDetail, claimDate, externalId);
            payment.setClaimType(loan.claimType());
            payment.updateLoan(loan);
            newTransactions.add(payment);
        }

        List<Long> transactionIds = new ArrayList<>();
        final ChangedTransactionDetail changedTransactionDetail = loan.handleClaimTransactions(payment, defaultLoanLifecycleStateMachine,
                scheduleGeneratorDTO);

        /***
         * TODO Vishwas Batch save is giving me a HibernateOptimisticLockingFailureException, looping and saving for the
         * time being, not a major issue for now as this loop is entered only in edge cases (when a payment is made
         * before the latest payment recorded against the loan)
         ***/

        for (LoanTransaction newTransaction : newTransactions) {
            saveLoanTransactionWithDataIntegrityViolationChecks(newTransaction);
            transactionIds.add(newTransaction.getId());
        }
        changes.put("transactions", transactionIds);
        changes.put("eventAmount", payPrincipal.getAmount().negate());

        if (changedTransactionDetail != null) {
            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
            }
            // Trigger transaction replayed event
            replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
        }
        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        this.loanRepositoryWrapper.removeLoanExclusion(loan.claimType());

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, false);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        return payment;
    }

    @Override
    @Transactional
    public void disableStandingInstructionsLinkedToClosedLoan(Loan loan) {
        if ((loan != null) && (loan.getStatus() != null) && loan.getStatus().isClosed()) {
            final Integer standingInstructionStatus = StandingInstructionStatus.ACTIVE.getValue();
            final Collection<AccountTransferStandingInstruction> accountTransferStandingInstructions = this.standingInstructionRepository
                    .findByLoanAccountAndStatus(loan, standingInstructionStatus);

            if (!accountTransferStandingInstructions.isEmpty()) {
                for (AccountTransferStandingInstruction accountTransferStandingInstruction : accountTransferStandingInstructions) {
                    accountTransferStandingInstruction.updateStatus(StandingInstructionStatus.DISABLED.getValue());
                    this.standingInstructionRepository.save(accountTransferStandingInstruction);
                }
            }
        }
    }

    @Override
    public void applyFinalIncomeAccrualTransaction(Loan loan) {
        if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()
                // to avoid collision with processIncomeAccrualTransactionOnLoanClosure()
                && !(loan.getLoanInterestRecalculationDetails() != null
                        && loan.getLoanInterestRecalculationDetails().isCompoundingToBePostedAsTransaction())
                && !loan.isNpa() && !loan.isChargedOff()) {

            MonetaryCurrency currency = loan.getCurrency();
            Money interestPortion = Money.zero(currency);
            Money feePortion = Money.zero(currency);
            Money penaltyPortion = Money.zero(currency);

            for (LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : loan.getRepaymentScheduleInstallments()) {
                interestPortion = interestPortion.add(loanRepaymentScheduleInstallment.getInterestCharged(currency))
                        .minus(loanRepaymentScheduleInstallment.getInterestAccrued(currency))
                        .minus(loanRepaymentScheduleInstallment.getInterestWaived(currency));
                feePortion = feePortion.add(loanRepaymentScheduleInstallment.getFeeChargesCharged(currency))
                        .minus(loanRepaymentScheduleInstallment.getFeeAccrued(currency))
                        .minus(loanRepaymentScheduleInstallment.getFeeChargesWaived(currency));
                penaltyPortion = penaltyPortion.add(loanRepaymentScheduleInstallment.getPenaltyChargesCharged(currency))
                        .minus(loanRepaymentScheduleInstallment.getPenaltyAccrued(currency))
                        .minus(loanRepaymentScheduleInstallment.getPenaltyChargesWaived(currency));
            }
            Money total = interestPortion.plus(feePortion).plus(penaltyPortion);

            if (total.isGreaterThanZero()) {
                ExternalId externalId = externalIdFactory.create();

                LocalDate accrualTransactionDate = getFinalAccrualTransactionDate(loan);

                LoanTransaction accrualTransaction = LoanTransaction.accrueTransaction(loan, loan.getOffice(), accrualTransactionDate,
                        total.getAmount(), interestPortion.getAmount(), feePortion.getAmount(), penaltyPortion.getAmount(), externalId);

                Set<LoanChargePaidBy> accrualCharges = accrualTransaction.getLoanChargesPaid();

                Map<Long, Money> accrualDetails = loan.getActiveCharges().stream()
                        .collect(Collectors.toMap(LoanCharge::getId, v -> Money.zero(currency)));

                loan.getLoanTransactions(LoanTransaction::isAccrual).forEach(transaction -> {
                    transaction.getLoanChargesPaid().forEach(loanChargePaid -> {
                        accrualDetails.computeIfPresent(loanChargePaid.getLoanCharge().getId(),
                                (mappedKey, mappedValue) -> mappedValue.add(Money.of(currency, loanChargePaid.getAmount())));
                    });
                });

                loan.getActiveCharges().forEach(loanCharge -> {
                    Money amount = loanCharge.getAmount(currency).minus(loanCharge.getAmountWaived(currency));
                    if (!loanCharge.isInstalmentFee() && loanCharge.isActive()
                            && accrualDetails.get(loanCharge.getId()).isLessThan(amount)) {
                        Money amountToBeAccrued = amount.minus(accrualDetails.get(loanCharge.getId()));
                        final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(accrualTransaction, loanCharge,
                                amountToBeAccrued.getAmount(), null);
                        accrualCharges.add(loanChargePaidBy);
                    }
                });

                for (LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : loan.getRepaymentScheduleInstallments()) {
                    for (LoanInstallmentCharge installmentCharge : loanRepaymentScheduleInstallment.getInstallmentCharges()) {
                        if (installmentCharge.getLoanCharge().isActive()) {
                            Money notWaivedAmount = installmentCharge.getAmount(currency)
                                    .minus(installmentCharge.getAmountWaived(currency));
                            if (notWaivedAmount.isGreaterThanZero()) {
                                Money amountToBeAccrued = notWaivedAmount
                                        .minus(accrualDetails.get(installmentCharge.getLoanCharge().getId()));
                                if (amountToBeAccrued.isGreaterThanZero()) {
                                    final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(accrualTransaction,
                                            installmentCharge.getLoanCharge(), amountToBeAccrued.getAmount(),
                                            installmentCharge.getInstallment().getInstallmentNumber());
                                    accrualCharges.add(loanChargePaidBy);
                                    accrualDetails.computeIfPresent(installmentCharge.getLoanCharge().getId(),
                                            (mappedKey, mappedValue) -> mappedValue.add(amountToBeAccrued));
                                }
                                accrualDetails.computeIfPresent(installmentCharge.getLoanCharge().getId(),
                                        (mappedKey, mappedValue) -> MathUtil
                                                .negativeToZero(mappedValue.minus(Money.of(currency, installmentCharge.getAmount()))));
                            }
                        }
                    }
                }
                saveLoanTransactionWithDataIntegrityViolationChecks(accrualTransaction);
                loan.addLoanTransaction(accrualTransaction);
                businessEventNotifierService.notifyPostBusinessEvent(new LoanAccrualTransactionCreatedBusinessEvent(accrualTransaction));

                loan.getRepaymentScheduleInstallments().forEach(installment -> {
                    installment.updateAccrualPortion(
                            installment.getInterestCharged(currency).minus(installment.getInterestWaived(currency)),
                            installment.getFeeChargesCharged(currency).minus(installment.getFeeChargesWaived(currency)),
                            installment.getPenaltyChargesCharged(currency).minus(installment.getPenaltyChargesWaived(currency)));
                });
            }
        }
    }

    private LocalDate getFinalAccrualTransactionDate(Loan loan) {
        return switch (loan.getStatus()) {
            case CLOSED_OBLIGATIONS_MET -> loan.getClosedOnDate();
            case OVERPAID -> loan.getOverpaidOnDate();
            default -> throw new IllegalStateException("Unexpected value: " + loan.getStatus());
        };
    }

    @Override
    public LoanTransaction writeoffPunishLoan(Loan loan, final LocalDate writeOffDate, final String noteText, final ExternalId externalId,
            Map<String, Object> changes) {
        if (loan.isChargedOff() && DateUtils.isBefore(writeOffDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.earlier.than.charge.off.date", "Loan: "
                    + loan.getId()
                    + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the loan",
                    loan.getId());
        }
        MonetaryCurrency currency = loan.getCurrency();
        List<LoanTransaction> newTransactions = new ArrayList<>();

        final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
        final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());
        final ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, null);
        final LoanRepaymentScheduleInstallment foreCloseDetail = loan.fetchLoanForeclosureDetail(writeOffDate, scheduleGeneratorDTO);
        if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()
                && (loan.getAccruedTill() == null || !DateUtils.isEqual(writeOffDate, loan.getAccruedTill()))) {
            loan.reverseAccrualsAfter(writeOffDate);
            Money[] accruedReceivables = loan.getReceivableIncome(writeOffDate);
            Money interestPortion = foreCloseDetail.getInterestCharged(currency).minus(accruedReceivables[0]);
            Money feePortion = foreCloseDetail.getFeeChargesCharged(currency).minus(accruedReceivables[1]);
            Money penaltyPortion = foreCloseDetail.getPenaltyChargesCharged(currency).minus(accruedReceivables[2]);
            Money total = interestPortion.plus(feePortion).plus(penaltyPortion);
            if (total.isGreaterThanZero()) {
                ExternalId accrualExternalId = externalIdFactory.create();
                LoanTransaction accrualTransaction = LoanTransaction.accrueTransaction(loan, loan.getOffice(), writeOffDate,
                        total.getAmount(), interestPortion.getAmount(), feePortion.getAmount(), penaltyPortion.getAmount(),
                        accrualExternalId);
                LocalDate fromDate = loan.getDisbursementDate();
                if (loan.getAccruedTill() != null) {
                    fromDate = loan.getAccruedTill();
                }
                newTransactions.add(accrualTransaction);
                loan.addLoanTransaction(accrualTransaction);
                Set<LoanChargePaidBy> accrualCharges = accrualTransaction.getLoanChargesPaid();
                for (LoanCharge loanCharge : loan.getActiveCharges()) {
                    boolean isDue = DateUtils.isEqual(fromDate, loan.getDisbursementDate())
                            ? loanCharge.isDueForCollectionFromIncludingAndUpToAndIncluding(fromDate, writeOffDate)
                            : loanCharge.isDueForCollectionFromAndUpToAndIncluding(fromDate, writeOffDate);
                    if (loanCharge.isActive() && !loanCharge.isPaid() && (isDue || loanCharge.isInstalmentFee())) {
                        final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(accrualTransaction, loanCharge,
                                loanCharge.getAmountOutstanding(currency).getAmount(), null);
                        accrualCharges.add(loanChargePaidBy);
                    }
                }
            }
        }

        Money interestPayable = foreCloseDetail.getInterestCharged(currency);
        Money feePayable = foreCloseDetail.getFeeChargesCharged(currency);
        Money penaltyPayable = foreCloseDetail.getPenaltyChargesCharged(currency);
        Money payPrincipal = foreCloseDetail.getPrincipal(currency);
        loan.updateInstallmentsPostDate(writeOffDate, scheduleGeneratorDTO);

        LoanTransaction payment = null;

        if (payPrincipal.plus(interestPayable).plus(feePayable).plus(penaltyPayable).isGreaterThanZero()) {
            final PaymentDetail paymentDetail = null;
            payment = LoanTransaction.repayment(loan.getOffice(), payPrincipal.plus(interestPayable).plus(feePayable).plus(penaltyPayable),
                    paymentDetail, writeOffDate, externalId);
            payment.setClaimType(loan.claimType());
            payment.updateLoan(loan);
            newTransactions.add(payment);
        }

        List<Long> transactionIds = new ArrayList<>();
        final ChangedTransactionDetail changedTransactionDetail = loan.handleClaimTransactions(payment, defaultLoanLifecycleStateMachine,
                scheduleGeneratorDTO);

        /***
         * TODO Vishwas Batch save is giving me a HibernateOptimisticLockingFailureException, looping and saving for the
         * time being, not a major issue for now as this loop is entered only in edge cases (when a payment is made
         * before the latest payment recorded against the loan)
         ***/

        for (LoanTransaction newTransaction : newTransactions) {
            saveLoanTransactionWithDataIntegrityViolationChecks(newTransaction);
            transactionIds.add(newTransaction.getId());
        }
        if (changedTransactionDetail != null) {
            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
            }
            // Trigger transaction replayed event
            replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
        }
        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, false);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        return payment;
    }

    private Money getPendingHonoAmountForAnuladoLoan(Loan loan) {
        BigDecimal honorariosAmount = BigDecimal.ZERO;
        Collection<LoanCharge> honorariosCharges = loan.getLoanCharges().stream().filter(LoanCharge::isFlatHono).toList();
        Collection<LoanCharge> ivaCharges = loan.getLoanCharges().stream().filter(LoanCharge::isCustomPercentageBasedOfAnotherCharge)
                .toList();
        for (LoanRepaymentScheduleInstallment repaymentScheduleInstallment : loan.getRepaymentScheduleInstallments()) {

            BigDecimal chargeAmount = honorariosCharges.stream().flatMap(lic -> lic.installmentCharges().stream()).filter(
                    lc -> Objects.equals(repaymentScheduleInstallment.getInstallmentNumber(), lc.getInstallment().getInstallmentNumber()))
                    .map(LoanInstallmentCharge::getAmountOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal honorariosTermChargeAmount = ivaCharges.stream()
                    .filter(lc -> honorariosCharges.stream()
                            .anyMatch(mic -> mic.getCharge().getId().equals(lc.getCharge().getParentChargeId())))
                    .flatMap(lic -> lic.installmentCharges().stream())
                    .filter(lc -> Objects.equals(repaymentScheduleInstallment.getInstallmentNumber(),
                            lc.getInstallment().getInstallmentNumber()))
                    .map(LoanInstallmentCharge::getAmountOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
            honorariosAmount = honorariosAmount.add(honorariosTermChargeAmount).add(chargeAmount);
        }
        return Money.of(loan.getCurrency(), honorariosAmount);
    }

    @SuppressWarnings("all")
    @Transactional
    @Override
    public void cleanUpLoan(final Long loanId) {
        log.info("Starting cleanup for Loan ID: {}", loanId);
        log.info("Step 1: Unset loan sub status and set loan to active for Loan ID: {}", loanId);
        this.unsetLoanSubStatus(loanId);
        log.info("Step 2: Remove non-migrated repayments for Loan ID: {}", loanId);
        this.removeNonMigratedRepayments(loanId);
        log.info("Step 3: Reset repayment schedule for Loan ID: {}", loanId);
        this.resetRepaymentSchedule(loanId);
        log.info("Step 4: Create honorarios and aval charges for recreated clinstallments for Loan ID: {}", loanId);
        this.recreateInstallmentCharges(loanId);
        log.info("Step 5: Update loan balances for Loan ID: {}", loanId);
        this.updateLoanBalances(loanId);
        log.info("Step 6: Repost transactions from the portfolio command source for Loan ID: {}", loanId);
        this.entityManager.flush();
        String sql = "select * from m_portfolio_command_source where loan_id = ? and action_name in ('REPAYMENT', 'FORECLOSURE') order by made_on_date_utc";
        final List<Map<String, Object>> results = this.jdbcTemplate.queryForList(sql, loanId);
        if (!results.isEmpty()) {
            for (final Map<String, Object> result : results) {
                if (result.get("action_name").toString().equals("REPAYMENT")) {
                    log.info("Reposting repayment for Loan ID: {}", loanId);
                    String payload = result.get("command_as_json").toString();
                    final CommandWrapper commandWrapper = new CommandWrapperBuilder().loanRepaymentTransaction(loanId).withJson(payload)
                            .build();
                    try {
                        log.info("Reposting repayment for Loan ID: {}", loanId);
                        commandsSourceWritePlatformService.logCommandSource(commandWrapper);
                    } catch (Exception ex) {
                        log.error("Failed to repost repayment for Loan ID: {}, with error: {}", loanId, ex.getMessage());
                        throw ex;
                    }
                } else {
                    log.info("Reposting foreclosure for Loan ID: {}", loanId);
                    this.updatePrincipalDueBeforeForeclosure(loanId);
                    final Loan loan = this.loanAccountAssembler.assembleFrom(loanId);
                    final ChangedTransactionDetail changedTransactionDetail = loan.reprocessAfterCleanUp();
                    if (changedTransactionDetail != null) {
                        for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings()
                                .entrySet()) {
                            saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                            updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                        }
                        replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
                    }
                    final String payloadJson = result.get("command_as_json").toString();
                    final CommandWrapper commandWrapper = new CommandWrapperBuilder().loanForeclosure(loanId).withJson(payloadJson).build();
                    try {
                        log.info("Reposting foreclosure for Loan ID: {}", loanId);
                        commandsSourceWritePlatformService.logCommandSource(commandWrapper);
                    } catch (Exception ex) {
                        log.error("Failed to repost foreclosure for Loan ID: {}, with error: {}", loanId, ex.getMessage());
                        throw ex;
                    }
                }
            }
        }
        log.info("Step 7: Marking loan as processed for Loan ID: {}", loanId);
        sql = "UPDATE tmp_loan_cleanup SET processed = true, date_processed = NOW() WHERE loan_id = ?";
        this.jdbcTemplate.update(sql, loanId);
        log.info("Step 8: Removing arrears aging for Loan ID: {}", loanId);
        sql = "delete from m_loan_arrears_aging mlaa where loan_id = ?";
        this.jdbcTemplate.update(sql, loanId);
        this.entityManager.flush();
    }

    private void unsetLoanSubStatus(Long loanId) {
        String sql = "UPDATE m_loan SET loan_sub_status_id = null, loan_status_id = 300 WHERE id = ?";
        this.jdbcTemplate.update(sql, loanId);
        // delete from arrears_aging just in case
        sql = "delete from m_loan_arrears_aging mlaa where loan_id = ?";
        this.jdbcTemplate.update(sql, loanId);
        this.entityManager.flush();
    }

    private void removeNonMigratedRepayments(Long loanId) {
        String sql = "update m_loan_transaction set is_reversed = false where loan_id = ? and transaction_type_enum = 2 and installment_id is null and is_reversed = true";
        this.jdbcTemplate.update(sql, loanId);
        sql = "delete from m_loan_transaction_repayment_schedule_mapping where loan_transaction_id in (select id from m_loan_transaction where loan_id = ? and transaction_type_enum = 2 and installment_id is null)";
        this.jdbcTemplate.update(sql, loanId);
        sql = "delete from m_loan_charge_paid_by where loan_transaction_id in (select id from m_loan_transaction where loan_id = ? and transaction_type_enum = 2 and installment_id is null)";
        this.jdbcTemplate.update(sql, loanId);

        sql = "delete from m_payment_detail_forclousure where loan_transaction_id in (select id from m_loan_transaction where loan_id = ? and transaction_type_enum = 2 and installment_id is null)";
        this.jdbcTemplate.update(sql, loanId);
        sql = "delete from m_partial_invoiced_transaction where repayment_transaction_id in (select id from m_loan_transaction where loan_id = ? and transaction_type_enum = 2 and installment_id is null)";
        this.jdbcTemplate.update(sql, loanId);

        sql = "delete from m_loan_transaction_relation where from_loan_transaction_id in (select id from m_loan_transaction where loan_id = ? and transaction_type_enum = 2 and installment_id is null)";
        this.jdbcTemplate.update(sql, loanId);

        sql = "delete from m_loan_charge_paid_by where loan_transaction_id in (select id from m_loan_transaction where loan_id = ? and transaction_type_enum = 2 and installment_id is null)";
        this.jdbcTemplate.update(sql, loanId);

        sql = "delete from m_loan_transaction_repayment_schedule_mapping where loan_transaction_id in (select id from m_loan_transaction where loan_id = ? and transaction_type_enum = 2 and installment_id is null)";
        this.jdbcTemplate.update(sql, loanId);

        sql = "delete from m_loan_transaction where loan_id = ? and transaction_type_enum = 2 and installment_id is null";
        this.jdbcTemplate.update(sql, loanId);
        this.entityManager.flush();
    }

    private void resetRepaymentSchedule(Long loanId) {
        String sql = """
                INSERT INTO public.m_loan_repayment_schedule
                (loan_id, fromdate, duedate, installment, principal_amount, interest_amount, fee_charges_amount, penalty_charges_amount, completed_derived, created_by, created_date, lastmodified_date, last_modified_by, created_on_utc, last_modified_on_utc)
                SELECT loan_id, fromdate, duedate, installment, principal_amount, interest_amount, fee_charges_amount, penalty_charges_amount, false, createdby_id, created_date, lastmodified_date, lastmodifiedby_id, created_on_utc, last_modified_on_utc
                FROM public.m_loan_repayment_schedule_history
                where loan_id = ?
                and version = 1
                and installment not in (select installment from m_loan_repayment_schedule mlrs where mlrs.loan_id = ?)
                order by installment
                """;
        this.jdbcTemplate.update(sql, loanId, loanId);

        sql = """
                    update m_loan_repayment_schedule mlrs
                    	set fromdate = mlrsh.fromdate,
                    	duedate = mlrsh.duedate,
                    	principal_amount = mlrsh.principal_amount,
                    	interest_amount = mlrsh.interest_amount,
                    	fee_charges_amount = mlrsh.fee_charges_amount\s
                    	from m_loan_repayment_schedule_history mlrsh
                    	where mlrs.loan_id = mlrsh.loan_id
                    	and mlrs.loan_id = ?
                    	and mlrsh.version = 2
                    	and mlrs.installment = mlrsh.installment
                """;
        this.jdbcTemplate.update(sql, loanId);

        // reset non migrated loan installments
        sql = """
                update m_loan_repayment_schedule set principal_completed_derived = null, interest_completed_derived = null, interest_writtenoff_derived = null,
                                fee_charges_completed_derived = null, penalty_charges_completed_derived = null, principal_writtenoff_derived = null, advance_principal_amount = null,
                                fee_charges_writtenoff_derived = null, penalty_charges_writtenoff_derived = null, completed_derived = false, obligations_met_on_date = null,
                                accrual_interest_derived = null, reschedule_interest_portion = null, total_paid_in_advance_derived = null, original_interest_charged = null
                        where loan_id = ? and migrated_installment = false
                """;

        this.jdbcTemplate.update(sql, loanId);

        sql = "update m_loan_repayment_schedule set migrated_installment = completed_derived where loan_id = ?";
        this.jdbcTemplate.update(sql, loanId);
        this.entityManager.flush();
    }

    private void recreateInstallmentCharges(Long loanId) {
        // NOTE: ref SU-702: this being throwaway code, I have hardcoded produciton ids. The plan is to verify
        // this using a dump from production and then finally on production. After the clean up this code
        // should be discarded.
        String sql = """
                INSERT INTO m_loan_installment_charge
                (loan_charge_id, loan_schedule_id, due_date, amount)
                select mlc.id loan_charge_id, mlrs.id loan_schedule_id, null::date due_date, 0 amount from m_loan ml join m_loan_charge mlc on ml.id = mlc.loan_id
                join m_loan_repayment_schedule mlrs on ml.id = mlrs.loan_id
                where mlc.charge_id in (4,5)
                and mlc.loan_id = ?
                and mlc.id not in (select loan_charge_id from m_loan_installment_charge where loan_charge_id = mlc.id and loan_schedule_id = mlrs.id)
                and mlrs.id not in (select loan_schedule_id from m_loan_installment_charge where loan_charge_id = mlc.id)
                and mlrs.installment > 0
                order by mlc.id, mlrs.installment
                """;
        this.jdbcTemplate.update(sql, loanId);

        sql = """
                    INSERT INTO m_loan_installment_charge
                (loan_charge_id, loan_schedule_id, due_date, amount)
                select mlc.id loan_charge_id, mlrs.id loan_schedule_id, null::date due_date, mlc.charge_amount_or_percentage amount\s
                from m_loan ml join m_loan_charge mlc on ml.id = mlc.loan_id
                join m_loan_repayment_schedule mlrs on ml.id = mlrs.loan_id
                where mlc.charge_id = 6
                and mlc.loan_id = ?
                and mlc.id not in (select loan_charge_id from m_loan_installment_charge where loan_charge_id = mlc.id and loan_schedule_id = mlrs.id)
                and mlrs.id not in (select loan_schedule_id from m_loan_installment_charge where loan_charge_id = mlc.id)
                and mlrs.installment > 0
                order by mlc.id, mlrs.installment
                """;
        this.jdbcTemplate.update(sql, loanId);

        sql = """
                INSERT INTO m_loan_installment_charge
                (loan_charge_id, loan_schedule_id, due_date, amount)
                select mlc.id loan_charge_id, mlrs.id loan_schedule_id, null::date due_date, ((mlc.calculation_percentage * mlc.calculation_on_amount / 100))::int amount\s
                from m_loan ml join m_loan_charge mlc on ml.id = mlc.loan_id
                join m_loan_repayment_schedule mlrs on ml.id = mlrs.loan_id
                where mlc.charge_id = 7
                and mlc.loan_id = ?
                and mlc.id not in (select loan_charge_id from m_loan_installment_charge where loan_charge_id = mlc.id and loan_schedule_id = mlrs.id)
                and mlrs.id not in (select loan_schedule_id from m_loan_installment_charge where loan_charge_id = mlc.id)
                and mlrs.installment > 0
                order by mlc.id, mlrs.installment
                """;
        this.jdbcTemplate.update(sql, loanId);
        this.entityManager.flush();
    }

    private void updateLoanBalances(Long loanId) {
        String sql = """
                UPDATE m_loan_transaction lt
                SET outstanding_loan_balance_derived = (
                    SELECT ml.principal_disbursed_derived - COALESCE(SUM(lt2.principal_portion_derived), 0)
                    FROM m_loan ml
                    LEFT JOIN m_loan_transaction lt2 ON lt2.loan_id = ml.id
                    where ml.id = lt.loan_id and lt2.transaction_date <= lt.transaction_date
                    and lt2.transaction_type_enum = 2
                    group by ml.principal_disbursed_derived
                )
                where lt.outstanding_loan_balance_derived IS DISTINCT FROM (
                    SELECT ml.principal_disbursed_derived - COALESCE(SUM(lt2.principal_portion_derived), 0)
                    FROM m_loan ml
                    LEFT JOIN m_loan_transaction lt2 ON lt2.loan_id = lt.loan_id
                    WHERE ml.id = lt.loan_id
                    AND lt2.transaction_date <= lt.transaction_date
                    and lt2.transaction_type_enum = 2
                    group by ml.principal_disbursed_derived
                ) and lt.loan_id = ?
                """;
        this.jdbcTemplate.update(sql, loanId);

        sql = """

                    update
                	m_loan ml
                set
                	principal_repaid_derived = (
                	select
                		coalesce(SUM(mlrs.principal_completed_derived),
                		0)
                	from
                		m_loan_repayment_schedule mlrs
                	where
                		mlrs.principal_completed_derived is not null
                		and mlrs.loan_id = ml.id
                ),
                	interest_repaid_derived = (
                	select
                		coalesce(SUM(mlrs.interest_completed_derived),
                		0)
                	from
                		m_loan_repayment_schedule mlrs
                	where
                		mlrs.interest_completed_derived is not null
                		and mlrs.loan_id = ml.id
                ),
                	fee_charges_repaid_derived = (
                	select
                		coalesce(SUM(mlrs.fee_charges_completed_derived),
                		0)
                	from
                		m_loan_repayment_schedule mlrs
                	where
                		mlrs.fee_charges_completed_derived is not null
                		and mlrs.loan_id = ml.id
                ),
                	penalty_charges_repaid_derived = (
                	select
                		coalesce(SUM(mlrs.penalty_charges_completed_derived),
                		0)
                	from
                		m_loan_repayment_schedule mlrs
                	where
                		mlrs.penalty_charges_completed_derived is not null
                		and mlrs.loan_id = ml.id
                ),
                principal_outstanding_derived = principal_disbursed_derived - principal_repaid_derived,
                interest_outstanding_derived  = interest_charged_derived - interest_repaid_derived,
                fee_charges_outstanding_derived = fee_charges_charged_derived - fee_charges_repaid_derived,
                total_repayment_derived = principal_repaid_derived + interest_repaid_derived + fee_charges_repaid_derived + penalty_charges_repaid_derived,
                total_outstanding_derived = principal_outstanding_derived + interest_outstanding_derived + fee_charges_outstanding_derived,
                total_overpaid_derived = null
                where ml.id = ?
                """;
        // run this thrice for accuracy
        this.jdbcTemplate.update(sql, loanId);
        this.jdbcTemplate.update(sql, loanId);
        this.jdbcTemplate.update(sql, loanId);
        this.entityManager.flush();
    }

    private void updatePrincipalDueBeforeForeclosure(Long loanId) {
        String sql = """
                update m_loan_repayment_schedule
                set principal_amount = (select ml.principal_disbursed_derived - rs.total_principal diff from m_loan ml
                join
                (select sum(principal_amount) + sum(coalesce(advance_principal_amount, 0)) total_principal, loan_id from m_loan_repayment_schedule mlrs
                where mlrs.loan_id = ?
                and mlrs.installment < (select max(installment) from m_loan_repayment_schedule where loan_id = ?)
                group by loan_id) rs
                on ml.id = rs.loan_id
                where ml.id = ?)
                where loan_id = ? and installment = (select max(installment) from m_loan_repayment_schedule where loan_id = ?)
                """;
        this.jdbcTemplate.update(sql, loanId, loanId, loanId, loanId, loanId);
    }

}
