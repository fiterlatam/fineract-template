/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.loanaccount.service;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.MathContext;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.cob.exceptions.LoanAccountLockCannotBeOverruledException;
import org.apache.fineract.cob.service.LoanAccountLockService;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.infrastructure.channel.constants.ChannelApiConstants;
import org.apache.fineract.custom.infrastructure.channel.data.ChannelData;
import org.apache.fineract.custom.infrastructure.channel.domain.Channel;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelType;
import org.apache.fineract.custom.infrastructure.channel.service.ChannelReadWritePlatformService;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.domain.CustomChargeHonorarioMap;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.domain.CustomChargeHonorarioMapRepository;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockLevel;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingEnum;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingsRepositoryWrapper;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationProperty;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepository;
import org.apache.fineract.infrastructure.configuration.service.TemporaryConfigurationServiceContainer;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformServiceUnavailableException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.creditbureau.exception.CreditReportNotFoundException;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksWritePlatformService;
import org.apache.fineract.infrastructure.event.business.BusinessEventListener;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanAcceptTransferBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanAdjustTransactionBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanBalanceChangedBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanChargebackTransactionBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanCloseAsRescheduleBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanCloseBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanCreditNoteBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanDebitNoteBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanDisbursalBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanInitiateTransferBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanInterestRecalculationBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanReassignOfficerBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanRejectTransferBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanRemoveOfficerBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanRescheduledDueCalendarChangeBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanTopUpBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanTxReversalBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanUndoDisbursalBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanUndoLastDisbursalBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanUpdateDisbursementDataBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanWithdrawTransferBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanAccrualTransactionCreatedBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanChargeOffPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanChargeOffPreBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanDisbursalTransactionBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanInvoiceGenerationPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanUndoChargeOffBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanUndoWrittenOffBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanWaiveInterestBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanWrittenOffPostBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanWrittenOffPreBusinessEvent;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.holiday.domain.Holiday;
import org.apache.fineract.organisation.holiday.domain.HolidayRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.teller.data.CashierTransactionDataValidator;
import org.apache.fineract.organisation.workingdays.domain.WorkingDays;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.data.AccountTransferDTO;
import org.apache.fineract.portfolio.account.data.PortfolioAccountData;
import org.apache.fineract.portfolio.account.domain.AccountAssociationType;
import org.apache.fineract.portfolio.account.domain.AccountAssociations;
import org.apache.fineract.portfolio.account.domain.AccountAssociationsRepository;
import org.apache.fineract.portfolio.account.domain.AccountTransferDetailRepository;
import org.apache.fineract.portfolio.account.domain.AccountTransferDetails;
import org.apache.fineract.portfolio.account.domain.AccountTransferRecurrenceType;
import org.apache.fineract.portfolio.account.domain.AccountTransferStandingInstruction;
import org.apache.fineract.portfolio.account.domain.AccountTransferType;
import org.apache.fineract.portfolio.account.domain.StandingInstructionPriority;
import org.apache.fineract.portfolio.account.domain.StandingInstructionStatus;
import org.apache.fineract.portfolio.account.domain.StandingInstructionType;
import org.apache.fineract.portfolio.account.service.AccountAssociationsReadPlatformService;
import org.apache.fineract.portfolio.account.service.AccountTransfersReadPlatformService;
import org.apache.fineract.portfolio.account.service.AccountTransfersWritePlatformService;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstance;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstanceRepository;
import org.apache.fineract.portfolio.calendar.domain.CalendarRepository;
import org.apache.fineract.portfolio.calendar.domain.CalendarType;
import org.apache.fineract.portfolio.calendar.exception.CalendarParameterUpdateNotSupportedException;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.charge.exception.LoanChargeNotFoundException;
import org.apache.fineract.portfolio.client.data.ClientAdditionalFieldsData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.collateralmanagement.domain.ClientCollateralManagement;
import org.apache.fineract.portfolio.collateralmanagement.exception.LoanCollateralAmountNotSufficientException;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.CollectionHouseConfiguration;
import org.apache.fineract.portfolio.collectionhousemanagement.service.CollectionHouseReadWriteServiceImpl;
import org.apache.fineract.portfolio.collectionsheet.command.CollectionSheetBulkDisbursalCommand;
import org.apache.fineract.portfolio.collectionsheet.command.CollectionSheetBulkRepaymentCommand;
import org.apache.fineract.portfolio.collectionsheet.command.SingleDisbursalCommand;
import org.apache.fineract.portfolio.collectionsheet.command.SingleRepaymentCommand;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.insurance.domain.InsuranceIncident;
import org.apache.fineract.portfolio.insurance.domain.InsuranceIncidentNoveltyNews;
import org.apache.fineract.portfolio.insurance.domain.InsuranceIncidentNoveltyNewsRepository;
import org.apache.fineract.portfolio.insurance.domain.InsuranceIncidentRepository;
import org.apache.fineract.portfolio.insurance.domain.InsuranceIncidentType;
import org.apache.fineract.portfolio.insurance.exception.InsuranceIncidentNotFoundException;
import org.apache.fineract.portfolio.interestrates.domain.InterestRate;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.command.LoanUpdateCommand;
import org.apache.fineract.portfolio.loanaccount.data.*;
import org.apache.fineract.portfolio.loanaccount.domain.*;
import org.apache.fineract.portfolio.loanaccount.exception.*;
import org.apache.fineract.portfolio.loanaccount.guarantor.service.GuarantorDomainService;
import org.apache.fineract.portfolio.loanaccount.invoice.data.ClasificacionConceptosData;
import org.apache.fineract.portfolio.loanaccount.invoice.data.LoanDocumentData;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicMensualRepository;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicaMensual;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.LoanDocumentConcept;
import org.apache.fineract.portfolio.loanaccount.jobs.updateloanarrearsageing.LoanArrearsAgeingUpdateHandler;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanApplicationTerms;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanInstalmentChargeRepository;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGeneratorFactory;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModelPeriod;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleProcessingType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.RescheduleLoansApiConstants;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.LoanRescheduleRequestData;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.service.LoanRescheduleRequestReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.service.LoanRescheduleRequestWritePlatformServiceImpl;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanApplicationCommandFromApiJsonHelper;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanEventApiJsonValidator;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanUpdateCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.loanproduct.data.AdvanceQuotaConfigurationData;
import org.apache.fineract.portfolio.loanproduct.data.MaximumCreditRateConfigurationData;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductType;
import org.apache.fineract.portfolio.loanproduct.exception.LinkedAccountRequiredException;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterization;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterizationRepository;
import org.apache.fineract.portfolio.loanproductparameterization.exception.LoanProductParameterizationNotFoundException;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.paymentdetail.service.PaymentDetailWritePlatformService;
import org.apache.fineract.portfolio.repaymentwithpostdatedchecks.domain.PostDatedChecks;
import org.apache.fineract.portfolio.repaymentwithpostdatedchecks.domain.PostDatedChecksRepository;
import org.apache.fineract.portfolio.repaymentwithpostdatedchecks.service.RepaymentWithPostDatedChecksAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.transfer.api.TransferApiConstants;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Scope("singleton")
public class LoanWritePlatformServiceJpaRepositoryImpl implements LoanWritePlatformService {

    private final PlatformSecurityContext context;
    private final LoanEventApiJsonValidator loanEventApiJsonValidator;
    private final LoanUpdateCommandFromApiJsonDeserializer loanUpdateCommandFromApiJsonDeserializer;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanAccountDomainService loanAccountDomainService;
    private final NoteRepository noteRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final LoanTransactionRelationRepository loanTransactionRelationRepository;
    private final LoanAssembler loanAssembler;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final PaymentDetailWritePlatformService paymentDetailWritePlatformService;
    private final HolidayRepositoryWrapper holidayRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final WorkingDaysRepositoryWrapper workingDaysRepository;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final AccountTransfersReadPlatformService accountTransfersReadPlatformService;
    private final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService;
    private final LoanReadPlatformService loanReadPlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final CalendarRepository calendarRepository;
    private final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService;
    private final LoanApplicationCommandFromApiJsonHelper loanApplicationCommandFromApiJsonHelper;
    private final AccountAssociationsRepository accountAssociationRepository;
    private final AccountTransferDetailRepository accountTransferDetailRepository;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final GuarantorDomainService guarantorDomainService;
    private final LoanUtilService loanUtilService;
    private final LoanSummaryWrapper loanSummaryWrapper;
    private final EntityDatatableChecksWritePlatformService entityDatatableChecksWritePlatformService;
    private final LoanRepaymentScheduleTransactionProcessorFactory transactionProcessingStrategy;
    private final CodeValueRepositoryWrapper codeValueRepository;
    private final CashierTransactionDataValidator cashierTransactionDataValidator;
    private final GLIMAccountInfoRepository glimRepository;
    private final LoanRepository loanRepository;
    private final RepaymentWithPostDatedChecksAssembler repaymentWithPostDatedChecksAssembler;
    private final PostDatedChecksRepository postDatedChecksRepository;
    private final LoanRepaymentScheduleInstallmentRepository loanRepaymentScheduleInstallmentRepository;
    private final LoanLifecycleStateMachine defaultLoanLifecycleStateMachine;
    private final LoanAccountLockService loanAccountLockService;
    private final ExternalIdFactory externalIdFactory;
    private final ReplayedTransactionBusinessEventService replayedTransactionBusinessEventService;
    private final LoanAccrualTransactionBusinessEventService loanAccrualTransactionBusinessEventService;
    private final ErrorHandler errorHandler;
    private final LoanDownPaymentHandlerService loanDownPaymentHandlerService;
    private final LoanProductReadPlatformService loanProductReadPlatformService;
    private final JdbcTemplate jdbcTemplate;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final LoanRescheduleRequestReadPlatformService loanRescheduleRequestReadPlatformService;
    private final ClientReadPlatformService clientReadPlatformService;
    private final ChannelReadWritePlatformService channelReadWritePlatformService;
    private final PlatformSecurityContext platformSecurityContext;
    private final GlobalConfigurationRepository globalConfigurationRepository;
    private final LoanBlockWritePlatformService loanBlockWritePlatformService;
    private final BlockingReasonSettingsRepositoryWrapper loanBlockingReasonRepositoryWrapper;
    private final LoanBlockingReasonRepository loanBlockingReasonRepository;
    private final InsuranceIncidentRepository insuranceIncidentRepository;
    private final InsuranceIncidentNoveltyNewsRepository insuranceIncidentNoveltyNewsRepository;
    private final LoanScheduleGeneratorFactory loanScheduleFactory;
    private final BlockingReasonSettingsRepositoryWrapper blockingReasonSettingsRepositoryWrapper;
    private final FacturaElectronicMensualRepository facturaElectronicMensualRepository;
    private final LoanProductParameterizationRepository productParameterizationRepository;
    private final CustomChargeHonorarioMapRepository customChargeHonorarioMapRepository;
    private final LoanInstalmentChargeRepository loanInstalmentChargeRepository;
    private final LoanCreditNoteRepository loanCreditNoteRepository;
    private final LoanAccrualPlatformService loanAccrualPlatformService;
    private final CollectionHouseReadWriteServiceImpl collectionHouseReadWriteService;

    @PostConstruct
    public void registerForNotification() {
        businessEventNotifierService.addPostBusinessEventListener(LoanDisbursalBusinessEvent.class, new DisbursementEventListener());
        businessEventNotifierService.addPostBusinessEventListener((LoanInvoiceGenerationPostBusinessEvent.class),
                new LoanInvoiceGenerationPostBusinessEventListener());
        businessEventNotifierService.addPostBusinessEventListener((LoanCreditNoteBusinessEvent.class),
                new LoanCreditNoteGenerationPostBusinessEventListener());
    }

    @Transactional
    @Override
    public CommandProcessingResult disburseGLIMLoan(final Long loanId, final JsonCommand command) {
        final Long parentLoanId = loanId;
        GroupLoanIndividualMonitoringAccount parentLoan = glimRepository.findById(parentLoanId).orElseThrow();
        List<Loan> childLoans = this.loanRepository.findByGlimId(loanId);
        CommandProcessingResult result = null;
        int count = 0;
        for (Loan loan : childLoans) {
            result = disburseLoan(loan.getId(), command, false);
            if (result.getLoanId() != null) {
                count++;
                // if all the child loans are approved, mark the parent loan as
                // approved
                if (count == parentLoan.getChildAccountsCount()) {
                    parentLoan.setLoanStatus(LoanStatus.ACTIVE.getValue());
                    glimRepository.save(parentLoan);
                }
            }
        }
        return result;
    }

    private void validatedDisbursementChannel(final String channelName) {
        if (StringUtils.isBlank(channelName)) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.is.blank", "Channel is blank");
        }
        final ChannelData channelData = this.channelReadWritePlatformService.findByNameType(channelName,
                ChannelType.DISBURSEMENT.getValue());
        if (channelData == null) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.found", "Channel not found", channelName);
        }
        if (!channelData.getActive()) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.active", "Channel is not active", channelName);
        }
        if (ChannelType.DISBURSEMENT.getValue().longValue() != channelData.getChannelType().getId()) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.disbursement", "Channel is not disbursement channel",
                    channelName);
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult disburseLoan(final Long loanId, final JsonCommand command, Boolean isAccountTransfer) {

        final AppUser currentUser = getAppUserIfPresent();

        this.loanEventApiJsonValidator.validateDisbursement(command.json(), isAccountTransfer);
        Boolean isWriteoffPunish = command.booleanObjectValueOfParameterNamed("isWriteoffPunish");
        if (isWriteoffPunish == null) {
            isWriteoffPunish = false;
        }
        if (!isWriteoffPunish) {
            String channelName = command.stringValueOfParameterNamed("channelName");
            if (channelName == null) {
                channelName = this.platformSecurityContext.getApiRequestChannel();
            }
            this.validatedDisbursementChannel(channelName);
        }

        if (command.parameterExists("postDatedChecks")) {
            // validate with post dated checks for the disbursement
            this.loanEventApiJsonValidator.validateDisbursementWithPostDatedChecks(command.json(), loanId);
        }

        Loan loan = this.loanAssembler.assembleFrom(loanId);
        final LoanProduct loanProduct = loan.loanProduct();
        if (loan.isTopup() && !loanProduct.getCustomAllowRestructure()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.product.does.not.allow.topup",
                    "Loan product does not allow topup.");
        }

        // Fail fast if client/group is not active or actual loan status disallows disbursal
        checkClientOrGroupActive(loan);

        // Fail fast if cupo is not enough
        checkCupo(loan);

        // validate if the loan product allows creation and disbursement
        if (Boolean.FALSE.equals(loan.loanProduct().getCustomAllowCreateOrDisburse())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.product.does.not.allow.creation.nor.disbursement",
                    "Loan product does not allow creation and disbursement.");
        }

        final LocalDate actualDisbursementDate = command.localDateValueOfParameterNamed("actualDisbursementDate");

        if (loan.isChargedOff() && DateUtils.isBefore(actualDisbursementDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.earlier.than.charge.off.date", "Loan: "
                    + loanId
                    + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the loan",
                    loanId);
        }

        if (loan.loanProduct().isDisallowExpectedDisbursements()) {
            List<LoanDisbursementDetails> filteredList = loan.getDisbursementDetails().stream()
                    .filter(disbursementDetails -> disbursementDetails.actualDisbursementDate() == null).toList();
            // Check whether a new LoanDisbursementDetails is required
            if (filteredList.isEmpty()) {
                // create artificial 'tranche/expected disbursal' as current disburse code expects it for
                // multi-disbursal
                // products
                final LocalDate artificialExpectedDate = loan.getExpectedDisbursedOnLocalDate();
                LoanDisbursementDetails disbursementDetail = new LoanDisbursementDetails(artificialExpectedDate, null,
                        loan.getDisbursedAmount(), null, false);
                disbursementDetail.updateLoan(loan);
                loan.getAllDisbursementDetails().add(disbursementDetail);
            }
        }
        loan.validateAccountStatus(LoanEvent.LOAN_DISBURSED);

        // Get disbursedAmount
        final BigDecimal disbursedAmount = loan.getDisbursedAmount();
        final Set<LoanCollateralManagement> loanCollateralManagements = loan.getLoanCollateralManagements();

        // Get relevant loan collateral modules
        if ((loanCollateralManagements != null && !loanCollateralManagements.isEmpty())
                && AccountType.fromInt(loan.getLoanType()).isIndividualAccount()) {

            BigDecimal totalCollateral = BigDecimal.valueOf(0);

            for (LoanCollateralManagement loanCollateralManagement : loanCollateralManagements) {
                BigDecimal quantity = loanCollateralManagement.getQuantity();
                BigDecimal pctToBase = loanCollateralManagement.getClientCollateralManagement().getCollaterals().getPctToBase();
                BigDecimal basePrice = loanCollateralManagement.getClientCollateralManagement().getCollaterals().getBasePrice();
                totalCollateral = totalCollateral.add(quantity.multiply(basePrice).multiply(pctToBase).divide(BigDecimal.valueOf(100)));
            }

            // Validate the loan collateral value against the disbursedAmount
            if (disbursedAmount.compareTo(totalCollateral) > 0) {
                throw new LoanCollateralAmountNotSufficientException(disbursedAmount);
            }
        }

        // validate ActualDisbursement Date Against Expected Disbursement Date
        if (loanProduct.syncExpectedWithDisbursementDate()) {
            syncExpectedDateWithActualDisbursementDate(loan, actualDisbursementDate);
        }

        final LocalDate nextPossibleRepaymentDate = loan.getNextPossibleRepaymentDateForRescheduling();
        final LocalDate rescheduledRepaymentDate = command.localDateValueOfParameterNamed("adjustRepaymentDate");

        entityDatatableChecksWritePlatformService.runTheCheckForProduct(loanId, EntityTables.LOAN.getName(),
                StatusEnum.DISBURSE.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(), loan.productId());

        LocalDate recalculateFrom = null;
        if (!loan.isMultiDisburmentLoan()) {
            loan.setActualDisbursementDate(actualDisbursementDate);
        }
        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

        // validate actual disbursement date against meeting date
        final CalendarInstance calendarInstance = this.calendarInstanceRepository.findCalendarInstaneByEntityId(loan.getId(),
                CalendarEntityType.LOANS.getValue());
        if (loan.isSyncDisbursementWithMeeting()) {
            this.loanEventApiJsonValidator.validateDisbursementDateWithMeetingDate(actualDisbursementDate, calendarInstance,
                    scheduleGeneratorDTO.isSkipRepaymentOnFirstDayofMonth(), scheduleGeneratorDTO.getNumberOfdays());
        }

        businessEventNotifierService.notifyPreBusinessEvent(new LoanDisbursalBusinessEvent(loan));

        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        final Map<String, Object> changes = new LinkedHashMap<>();

        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);
        if (paymentDetail != null && paymentDetail.getPaymentType() != null && paymentDetail.getPaymentType().getIsCashPayment()) {
            BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
            this.cashierTransactionDataValidator.validateOnLoanDisbursal(currentUser, loan.getCurrencyCode(), transactionAmount);
        }
        final boolean isPaymentTypeApplicableForDisbursementCharge = configurationDomainService
                .isPaymentTypeApplicableForDisbursementCharge();

        // Recalculate first repayment date based in actual disbursement date.
        updateLoanCounters(loan, actualDisbursementDate);
        Money amountBeforeAdjust = loan.getPrincipal();
        boolean canDisburse = loan.canDisburse(actualDisbursementDate);
        ChangedTransactionDetail changedTransactionDetail = null;
        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(locale);
        if (canDisburse) {

            // Get netDisbursalAmount from disbursal screen field.
            final BigDecimal netDisbursalAmount = command
                    .bigDecimalValueOfParameterNamed(LoanApiConstants.disbursementNetDisbursalAmountParameterName);
            if (netDisbursalAmount != null) {
                loan.setNetDisbursalAmount(netDisbursalAmount);
            }
            Money disburseAmount = loan.adjustDisburseAmount(command, actualDisbursementDate);
            Money amountToDisburse = disburseAmount.copy();
            boolean recalculateSchedule = amountBeforeAdjust.isNotEqualTo(loan.getPrincipal());
            final ExternalId txnExternalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

            if (loan.isTopup() && loan.getClientId() != null) {
                final Long loanIdToClose = loan.getTopupLoanDetails().getLoanIdToClose();
                final Loan loanToClose = this.loanRepositoryWrapper.findNonClosedLoanThatBelongsToClient(loanIdToClose, loan.getClientId());
                Optional<GlobalConfigurationProperty> getmaxReestructurar = this.globalConfigurationRepository
                        .findByName(LoanApiConstants.GLOBAL_CONFIG_MAX_RESTRUCTURE_WITHIN_6_MONTHS);
                Long maxReestructurar = getmaxReestructurar.orElse(new GlobalConfigurationProperty().setValue(2L)).getValue();

                LocalDate businessDate = ThreadLocalContextUtil.getBusinessDateByType(BusinessDateType.BUSINESS_DATE);
                if (businessDate == null) {
                    businessDate = LocalDate.now();
                }
                Long topupCount = countRecentTopups(loan.getClientId(), businessDate);

                if (topupCount > maxReestructurar) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.outside.the.off.restriction.period",
                            "Maximum number of restructures within 6 months exceeded");
                }

                if (loanToClose == null) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.to.be.closed.with.topup.is.not.active",
                            "Loan to be closed with this topup is not active.");
                }
                final LocalDate lastUserTransactionOnLoanToClose = loanToClose.getLastUserTransactionDate();
                if (DateUtils.isBefore(loan.getDisbursementDate(), lastUserTransactionOnLoanToClose)) {
                    throw new GeneralPlatformDomainRuleException(
                            "error.msg.loan.disbursal.date.should.be.after.last.transaction.date.of.loan.to.be.closed",
                            "Disbursal date of this loan application " + loan.getDisbursementDate()
                                    + " should be after last transaction date of loan to be closed " + lastUserTransactionOnLoanToClose);
                }
                final LoanRepaymentScheduleInstallment foreCloseDetail = loanToClose.fetchLoanForeclosureDetail(actualDisbursementDate,
                        scheduleGeneratorDTO);
                BigDecimal loanOutstanding = foreCloseDetail.getTotalOutstanding(loanToClose.getCurrency()).getAmount();
                /*
                 * BigDecimal loanOutstanding = this.loanReadPlatformService
                 * .retrieveLoanPrePaymentTemplate(LoanTransactionType.REPAYMENT, loanIdToClose,
                 * actualDisbursementDate).getAmount();
                 */
                final BigDecimal firstDisbursalAmount = loan.getFirstDisbursalAmount();
                if (loanToClose.claimType() == null || !loanToClose.claimType().equals("castigado")) {
                    if (loanOutstanding.compareTo(firstDisbursalAmount) > 0) {
                        throw new GeneralPlatformDomainRuleException("error.msg.loan.amount.less.than.outstanding.of.loan.to.be.closed",
                                "Topup loan amount should be greater than outstanding amount of loan to be closed.");
                    }
                }
                if (loanToClose.claimType() == null || !loanToClose.claimType().equals("castigado")) {
                    // in case of castigado claim new loan will be of 1 installment and equal to outstanding amount of
                    // the existing loan
                    amountToDisburse = disburseAmount.minus(loanOutstanding);
                }
                if (!"castigado".equalsIgnoreCase(loanToClose.claimType())) { // Ensure the loan is not in castigado
                                                                              // state
                    createRestructuringCancellationEvent(loanToClose); // Generate the event
                }

                disburseLoanToLoan(loan, command, loanOutstanding, loanToClose);
            }
            LoanTransaction disbursementTransaction = null;
            if (Boolean.TRUE.equals(isAccountTransfer)) {
                disburseLoanToSavings(loan, command, amountToDisburse, paymentDetail);
                existingTransactionIds.addAll(loan.findExistingTransactionIds());
                existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());
            } else {
                existingTransactionIds.addAll(loan.findExistingTransactionIds());
                existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());
                disbursementTransaction = LoanTransaction.disbursement(loan.getOffice(), amountToDisburse, paymentDetail,
                        actualDisbursementDate, txnExternalId);
                disbursementTransaction.updateLoan(loan);
                loan.addLoanTransaction(disbursementTransaction);
            }
            if (loan.getRepaymentScheduleInstallments().isEmpty()) {
                /*
                 * If no schedule, generate one (applicable to non-tranche multi-disbursal loans)
                 */
                recalculateSchedule = true;
            }
            regenerateScheduleOnDisbursement(command, loan, recalculateSchedule, scheduleGeneratorDTO, nextPossibleRepaymentDate,
                    rescheduledRepaymentDate);
            // Farooq 25th June 2024 - Ensured that Loan Schedule Archive is always created

            createAndSaveLoanScheduleArchive(loan, scheduleGeneratorDTO);

            if (isPaymentTypeApplicableForDisbursementCharge) {
                changedTransactionDetail = loan.disburse(currentUser, command, changes, scheduleGeneratorDTO, paymentDetail);
            } else {
                changedTransactionDetail = loan.disburse(currentUser, command, changes, scheduleGeneratorDTO, null);
            }
            loan.adjustNetDisbursalAmount(amountToDisburse.getAmount());
            if (disbursementTransaction != null) {
                loanTransactionRepository.saveAndFlush(disbursementTransaction);
            }
            if (loan.isAutoRepaymentForDownPaymentEnabled()) {
                // updating linked savings account for auto down payment transaction for disbursement to savings account
                if (isAccountTransfer && loan.shouldCreateStandingInstructionAtDisbursement()) {
                    final PortfolioAccountData linkedSavingsAccountData = this.accountAssociationsReadPlatformService
                            .retriveLoanLinkedAssociation(loanId);
                    final SavingsAccount fromSavingsAccount = null;
                    final boolean isRegularTransaction = true;
                    final boolean isExceptionForBalanceCheck = false;

                    BigDecimal disbursedAmountPercentageForDownPayment = loan.getLoanRepaymentScheduleDetail()
                            .getDisbursedAmountPercentageForDownPayment();
                    Money downPaymentMoney = Money.of(loan.getCurrency(),
                            MathUtil.percentageOf(amountToDisburse.getAmount(), disbursedAmountPercentageForDownPayment, 19));

                    final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(actualDisbursementDate,
                            downPaymentMoney.getAmount(), PortfolioAccountType.SAVINGS, PortfolioAccountType.LOAN,
                            linkedSavingsAccountData.getId(), loan.getId(),
                            "To loan " + loan.getAccountNumber() + " from savings " + linkedSavingsAccountData.getAccountNo()
                                    + " Standing instruction transfer ",
                            locale, fmt, null, null, LoanTransactionType.DOWN_PAYMENT.getValue(), null, null,
                            AccountTransferType.LOAN_DOWN_PAYMENT.getValue(), null, null, ExternalId.empty(), null, null,
                            fromSavingsAccount, isRegularTransaction, isExceptionForBalanceCheck);
                    this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
                } else {
                    loanDownPaymentHandlerService.handleDownPayment(scheduleGeneratorDTO, command, amountToDisburse, loan);
                }
            }
        }
        if (!changes.isEmpty()) {
            if (changedTransactionDetail != null) {
                for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                    loanAccountDomainService.saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                    accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                }
                // Trigger transaction replayed event
                replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
            }
            loan.getLoanCustomizationDetail().recordActivity();
            loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.loanNote(loan, noteText);
                this.noteRepository.save(note);
            }
            // auto create standing instruction
            createStandingInstruction(loan);

            postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
            loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        }

        final Set<LoanCharge> loanCharges = loan.getActiveCharges();
        final Map<Long, BigDecimal> disBuLoanCharges = new HashMap<>();
        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isDueAtDisbursement() && loanCharge.getChargePaymentMode().isPaymentModeAccountTransfer()
                    && loanCharge.isChargePending()) {
                disBuLoanCharges.put(loanCharge.getId(), loanCharge.amountOutstanding());
            }
            if (loanCharge.isDisbursementCharge()) {
                LoanTransaction loanTransaction = LoanTransaction.accrueTransaction(loan, loan.getOffice(), actualDisbursementDate,
                        loanCharge.amount(), null, loanCharge.amount(), null, externalIdFactory.create());
                LoanTransaction savedLoanTransaction = loanTransactionRepository.saveAndFlush(loanTransaction);
                businessEventNotifierService.notifyPostBusinessEvent(new LoanAccrualTransactionCreatedBusinessEvent(savedLoanTransaction));
            }
        }
        for (final Map.Entry<Long, BigDecimal> entrySet : disBuLoanCharges.entrySet()) {
            final PortfolioAccountData savingAccountData = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(loanId);
            final SavingsAccount fromSavingsAccount = null;
            final boolean isRegularTransaction = true;
            final boolean isExceptionForBalanceCheck = false;
            final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(actualDisbursementDate, entrySet.getValue(),
                    PortfolioAccountType.SAVINGS, PortfolioAccountType.LOAN, savingAccountData.getId(), loanId, "Loan Charge Payment",
                    locale, fmt, null, null, LoanTransactionType.REPAYMENT_AT_DISBURSEMENT.getValue(), entrySet.getKey(), null,
                    AccountTransferType.CHARGE_PAYMENT.getValue(), null, null, ExternalId.empty(), null, null, fromSavingsAccount,
                    isRegularTransaction, isExceptionForBalanceCheck);
            this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
        }
        updateRecurringCalendarDatesForInterestRecalculation(loan);
        this.loanAccountDomainService.recalculateAccruals(loan);
        this.loanAccountDomainService.setLoanDelinquencyTag(loan, DateUtils.getBusinessLocalDate());

        // Post Dated Checks
        if (command.parameterExists("postDatedChecks")) {
            // get repayment with post dates checks to update
            Set<PostDatedChecks> postDatedChecks = this.repaymentWithPostDatedChecksAssembler.fromParsedJson(command.json(), loan);
            updatePostDatedChecks(postDatedChecks);
        }

        businessEventNotifierService.notifyPostBusinessEvent(new LoanDisbursalBusinessEvent(loan));

        Long disbursalTransactionId = null;
        ExternalId disbursalTransactionExternalId = null;

        if (!isAccountTransfer) {
            // If accounting is not periodic accrual, the last transaction might be the accrual not the disbursement
            LoanTransaction disbursalTransaction = Lists.reverse(loan.getLoanTransactions()).stream()
                    .filter(e -> LoanTransactionType.DISBURSEMENT.equals(e.getTypeOf())).findFirst().orElseThrow();
            disbursalTransactionId = disbursalTransaction.getId();
            disbursalTransactionExternalId = disbursalTransaction.getExternalId();
            businessEventNotifierService.notifyPostBusinessEvent(new LoanDisbursalTransactionBusinessEvent(disbursalTransaction));
            if ("Ajuste".equalsIgnoreCase(loanProduct.getName())) {
                this.businessEventNotifierService.notifyPostBusinessEvent(new LoanDebitNoteBusinessEvent(disbursalTransaction));
            }
        }
        if (loan.isTopup() && loan.getClientId() != null) {
            this.businessEventNotifierService.notifyPostBusinessEvent(new LoanTopUpBusinessEvent(loan));
        }
        Long minimumDaysInArrearsToSuspendLoanAccount = this.configurationDomainService.retriveMinimumDaysInArrearsToSuspendLoanAccount();
        if (minimumDaysInArrearsToSuspendLoanAccount == null) {
            minimumDaysInArrearsToSuspendLoanAccount = 90L;
        }

        if (!loan.isMigratedLoan()) {
            final LocalDate accrualDate = DateUtils.getLocalDateOfTenant().minusDays(1);
            this.loanAccrualPlatformService.persistDailyInterestAccrual(loanId, accrualDate);
        }

        for (LoanTransaction transaction : loan.retrieveListOfAccrualTransactions()) {
            long days = loan.getRepaymentScheduleInstallmentsIgnoringTotalGrace().get(0).getDueDate()
                    .until(transaction.getTransactionDate(), ChronoUnit.DAYS);
            if (days >= minimumDaysInArrearsToSuspendLoanAccount) {
                transaction.markAsOccurredOnSuspendedAccount();
            } else {
                transaction.markAsNotOccurredOnSuspendedAccount();
            }
        }
        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withEntityExternalId(loan.getExternalId()) //
                .withSubEntityId(disbursalTransactionId) //
                .withSubEntityExternalId(disbursalTransactionExternalId) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    private void updatePostDatedChecks(Set<PostDatedChecks> postDatedChecks) {
        this.postDatedChecksRepository.saveAll(postDatedChecks);
    }

    private void createAndSaveLoanScheduleArchive(final Loan loan, ScheduleGeneratorDTO scheduleGeneratorDTO) {
        LoanRescheduleRequest loanRescheduleRequest = null;
        LoanScheduleModel loanScheduleModel = loan.regenerateScheduleModel(scheduleGeneratorDTO);
        List<LoanRepaymentScheduleInstallment> installments = retrieveRepaymentScheduleFromModel(loanScheduleModel);
        this.loanScheduleHistoryWritePlatformService.createAndSaveLoanScheduleArchive(installments, loan, loanRescheduleRequest);
    }

    /**
     * create standing instruction for disbursed loan
     *
     * @param loan
     *            the disbursed loan
     **/
    private void createStandingInstruction(Loan loan) {

        if (loan.shouldCreateStandingInstructionAtDisbursement()) {
            AccountAssociations accountAssociations = this.accountAssociationRepository.findByLoanIdAndType(loan.getId(),
                    AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue());

            if (accountAssociations != null) {

                SavingsAccount linkedSavingsAccount = accountAssociations.linkedSavingsAccount();

                // name is auto-generated
                final String name = "To loan " + loan.getAccountNumber() + " from savings " + linkedSavingsAccount.getAccountNumber();
                final Office fromOffice = loan.getOffice();
                final Client fromClient = loan.getClient();
                final Office toOffice = loan.getOffice();
                final Client toClient = loan.getClient();
                final Integer priority = StandingInstructionPriority.MEDIUM.getValue();
                final Integer transferType = AccountTransferType.LOAN_REPAYMENT.getValue();
                final Integer instructionType = StandingInstructionType.DUES.getValue();
                final Integer status = StandingInstructionStatus.ACTIVE.getValue();
                final Integer recurrenceType = AccountTransferRecurrenceType.AS_PER_DUES.getValue();
                final LocalDate validFrom = DateUtils.getBusinessLocalDate();

                AccountTransferDetails accountTransferDetails = AccountTransferDetails.savingsToLoanTransfer(fromOffice, fromClient,
                        linkedSavingsAccount, toOffice, toClient, loan, transferType);

                AccountTransferStandingInstruction accountTransferStandingInstruction = AccountTransferStandingInstruction.create(
                        accountTransferDetails, name, priority, instructionType, status, null, validFrom, null, recurrenceType, null, null,
                        null);
                accountTransferDetails.updateAccountTransferStandingInstruction(accountTransferStandingInstruction);

                this.accountTransferDetailRepository.save(accountTransferDetails);
            }
        }
    }

    private void updateRecurringCalendarDatesForInterestRecalculation(final Loan loan) {

        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()
                && loan.loanInterestRecalculationDetails().getRestFrequencyType().isSameAsRepayment()) {
            final CalendarInstance calendarInstanceForInterestRecalculation = this.calendarInstanceRepository
                    .findByEntityIdAndEntityTypeIdAndCalendarTypeId(loan.loanInterestRecalculationDetailId(),
                            CalendarEntityType.LOAN_RECALCULATION_REST_DETAIL.getValue(), CalendarType.COLLECTION.getValue());

            Calendar calendarForInterestRecalculation = calendarInstanceForInterestRecalculation.getCalendar();
            calendarForInterestRecalculation.updateStartAndEndDate(loan.getDisbursementDate(), loan.getMaturityDate());
            this.calendarRepository.save(calendarForInterestRecalculation);
        }

    }

    private Loan saveAndFlushLoanWithDataIntegrityViolationChecks(final Loan loan) {
        /*
         * Due to the "saveAndFlushLoanWithDataIntegrityViolationChecks" method the loan is saved and flushed in the
         * middle of the transaction. EclipseLink is in some situations are saving inconsistently the newly created
         * associations, like the newly created repayment schedule installments. The save and flush cannot be removed
         * safely till any native queries are used as part of this transaction either. See:
         * this.loanAccountDomainService.recalculateAccruals(loan);
         */
        try {
            loanRepaymentScheduleInstallmentRepository.saveAll(loan.getRepaymentScheduleInstallments());
            return this.loanRepositoryWrapper.saveAndFlush(loan);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.transaction");
            if (realCause.getMessage().toLowerCase().contains("external_id_unique")) {
                baseDataValidator.reset().parameter(LoanApiConstants.externalIdParameterName).failWithCode("value.must.be.unique");
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors, e);
            }
            throw e;
        }
    }

    private void saveAndFlushLoanWithIntegrityChecks(final Loan loan) {
        try {
            this.loanRepositoryWrapper.saveAndFlush(loan);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.transaction");
            if (realCause.getMessage().toLowerCase().contains("external_id_unique")) {
                baseDataValidator.reset().parameter(LoanApiConstants.externalIdParameterName).failWithCode("value.must.be.unique");
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors, e);
            }
        }
    }

    private void saveLoanWithDataIntegrityViolationChecks(final Loan loan) {
        try {
            this.loanRepositoryWrapper.save(loan);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.transaction");
            if (realCause.getMessage().toLowerCase().contains("external_id_unique")) {
                baseDataValidator.reset().parameter(LoanApiConstants.externalIdParameterName).failWithCode("value.must.be.unique");
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors, e);
            }
        }
    }

    /****
     * TODO Vishwas: Pair with Ashok and re-factor collection sheet code-base
     *
     * May of the changes made to disburseLoan aren't being made here, should refactor to reuse disburseLoan ASAP
     *****/
    @Transactional
    @Override
    public Map<String, Object> bulkLoanDisbursal(final JsonCommand command, final CollectionSheetBulkDisbursalCommand bulkDisbursalCommand,
            Boolean isAccountTransfer) {
        final AppUser currentUser = getAppUserIfPresent();

        final SingleDisbursalCommand[] disbursalCommand = bulkDisbursalCommand.getDisburseTransactions();
        final Map<String, Object> changes = new LinkedHashMap<>();
        if (disbursalCommand == null) {
            return changes;
        }

        final LocalDate nextPossibleRepaymentDate = null;
        final LocalDate rescheduledRepaymentDate = null;

        for (final SingleDisbursalCommand singleLoanDisbursalCommand : disbursalCommand) {
            Loan loan = this.loanAssembler.assembleFrom(singleLoanDisbursalCommand.getLoanId());
            final LocalDate actualDisbursementDate = command.localDateValueOfParameterNamed("actualDisbursementDate");

            // validate ActualDisbursement Date Against Expected Disbursement
            // Date
            LoanProduct loanProduct = loan.loanProduct();
            if (loanProduct.syncExpectedWithDisbursementDate()) {
                syncExpectedDateWithActualDisbursementDate(loan, actualDisbursementDate);
            }
            checkClientOrGroupActive(loan);
            businessEventNotifierService.notifyPreBusinessEvent(new LoanDisbursalBusinessEvent(loan));

            final List<Long> existingTransactionIds = new ArrayList<>();
            final List<Long> existingReversedTransactionIds = new ArrayList<>();

            final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);

            // Bulk disbursement should happen on meeting date (mostly from
            // collection sheet).
            // FIXME: AA - this should be first meeting date based on
            // disbursement date and next available meeting dates
            // assuming repayment schedule won't regenerate because expected
            // disbursement and actual disbursement happens on same date
            loan.validateAccountStatus(LoanEvent.LOAN_DISBURSED);
            updateLoanCounters(loan, actualDisbursementDate);
            boolean canDisburse = loan.canDisburse(actualDisbursementDate);
            ChangedTransactionDetail changedTransactionDetail = null;
            if (canDisburse) {
                Money amountBeforeAdjust = loan.getPrincipal();
                Money disburseAmount = loan.adjustDisburseAmount(command, actualDisbursementDate);
                boolean recalculateSchedule = amountBeforeAdjust.isNotEqualTo(loan.getPrincipal());
                final ExternalId txnExternalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);
                if (isAccountTransfer) {
                    disburseLoanToSavings(loan, command, disburseAmount, paymentDetail);
                    existingTransactionIds.addAll(loan.findExistingTransactionIds());
                    existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());

                } else {
                    existingTransactionIds.addAll(loan.findExistingTransactionIds());
                    existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());
                    LoanTransaction disbursementTransaction = LoanTransaction.disbursement(loan.getOffice(), disburseAmount, paymentDetail,
                            actualDisbursementDate, txnExternalId);
                    disbursementTransaction.updateLoan(loan);
                    loan.addLoanTransaction(disbursementTransaction);
                    businessEventNotifierService
                            .notifyPostBusinessEvent(new LoanDisbursalTransactionBusinessEvent(disbursementTransaction));
                }
                LocalDate recalculateFrom = null;
                final ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
                regenerateScheduleOnDisbursement(command, loan, recalculateSchedule, scheduleGeneratorDTO, nextPossibleRepaymentDate,
                        rescheduledRepaymentDate);
                boolean downPaymentEnabled = loan.repaymentScheduleDetail().isEnableDownPayment();
                if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled() || downPaymentEnabled) {
                    createAndSaveLoanScheduleArchive(loan, scheduleGeneratorDTO);
                }
                if (configurationDomainService.isPaymentTypeApplicableForDisbursementCharge()) {
                    changedTransactionDetail = loan.disburse(currentUser, command, changes, scheduleGeneratorDTO, paymentDetail);
                } else {
                    changedTransactionDetail = loan.disburse(currentUser, command, changes, scheduleGeneratorDTO, null);
                }
            }
            if (!changes.isEmpty()) {

                final String noteText = command.stringValueOfParameterNamed("note");
                if (StringUtils.isNotBlank(noteText)) {
                    final Note note = Note.loanNote(loan, noteText);
                    this.noteRepository.save(note);
                }
                if (changedTransactionDetail != null) {
                    for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings()
                            .entrySet()) {
                        loanAccountDomainService.saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                        accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                    }
                    // Trigger transaction replayed event
                    replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
                }
                loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
                postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
                loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
            }
            final Set<LoanCharge> loanCharges = loan.getActiveCharges();
            final Map<Long, BigDecimal> disBuLoanCharges = new HashMap<>();
            for (final LoanCharge loanCharge : loanCharges) {
                if (loanCharge.isDueAtDisbursement() && loanCharge.getChargePaymentMode().isPaymentModeAccountTransfer()
                        && loanCharge.isChargePending()) {
                    disBuLoanCharges.put(loanCharge.getId(), loanCharge.amountOutstanding());
                }
            }
            final Locale locale = command.extractLocale();
            final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(locale);
            for (final Map.Entry<Long, BigDecimal> entrySet : disBuLoanCharges.entrySet()) {
                final PortfolioAccountData savingAccountData = this.accountAssociationsReadPlatformService
                        .retriveLoanLinkedAssociation(loan.getId());
                final SavingsAccount fromSavingsAccount = null;
                final boolean isRegularTransaction = true;
                final boolean isExceptionForBalanceCheck = false;
                final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(actualDisbursementDate, entrySet.getValue(),
                        PortfolioAccountType.SAVINGS, PortfolioAccountType.LOAN, savingAccountData.getId(), loan.getId(),
                        "Loan Charge Payment", locale, fmt, null, null, LoanTransactionType.REPAYMENT_AT_DISBURSEMENT.getValue(),
                        entrySet.getKey(), null, AccountTransferType.CHARGE_PAYMENT.getValue(), null, null, ExternalId.empty(), null, null,
                        fromSavingsAccount, isRegularTransaction, isExceptionForBalanceCheck);
                this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
            }
            updateRecurringCalendarDatesForInterestRecalculation(loan);
            loanAccountDomainService.recalculateAccruals(loan);
            loanAccountDomainService.setLoanDelinquencyTag(loan, DateUtils.getBusinessLocalDate());
            businessEventNotifierService.notifyPostBusinessEvent(new LoanDisbursalBusinessEvent(loan));
        }

        return changes;
    }

    @Transactional
    @Override
    public CommandProcessingResult undoGLIMLoanDisbursal(final Long loanId, final JsonCommand command) {
        final Long parentLoanId = loanId;
        GroupLoanIndividualMonitoringAccount parentLoan = glimRepository.findById(parentLoanId).orElseThrow();
        List<Loan> childLoans = this.loanRepository.findByGlimId(loanId);
        CommandProcessingResult result = null;
        int count = 0;
        for (Loan loan : childLoans) {
            result = undoLoanDisbursal(loan.getId(), command);
            if (result.getLoanId() != null) {
                count++;
                // if all the child loans are approved, mark the parent loan as
                // approved
                if (count == parentLoan.getChildAccountsCount()) {
                    parentLoan.setLoanStatus(LoanStatus.APPROVED.getValue());
                    glimRepository.save(parentLoan);
                }
            }
        }
        return result;
    }

    @Transactional
    @Override
    public CommandProcessingResult undoLoanDisbursal(final Long loanId, final JsonCommand command) {

        Loan loan = this.loanAssembler.assembleFrom(loanId);
        checkClientOrGroupActive(loan);
        if (loan.isChargedOff()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.charged.off",
                    "Undo Loan: " + loanId + " disbursement is not allowed. Loan Account is Charged-off", loanId);
        }
        businessEventNotifierService.notifyPreBusinessEvent(new LoanUndoDisbursalBusinessEvent(loan));
        removeLoanCycle(loan);
        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();
        //
        final MonetaryCurrency currency = loan.getCurrency();

        final LocalDate recalculateFrom = null;
        loan.setActualDisbursementDate(null);
        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

        // Remove post dated checks if added.
        loan.removePostDatedChecks();

        final Map<String, Object> changes = loan.undoDisbursal(scheduleGeneratorDTO, existingTransactionIds,
                existingReversedTransactionIds);

        if (!changes.isEmpty()) {
            if (loan.isTopup() && loan.getClientId() != null) {
                final Long loanIdToClose = loan.getTopupLoanDetails().getLoanIdToClose();
                final LocalDate expectedDisbursementDate = command
                        .localDateValueOfParameterNamed(LoanApiConstants.expectedDisbursementDateParameterName);
                BigDecimal loanOutstanding = this.loanReadPlatformService
                        .retrieveLoanPrePaymentTemplate(LoanTransactionType.REPAYMENT, loanIdToClose, expectedDisbursementDate).getAmount();
                BigDecimal netDisbursalAmount = loan.getApprovedPrincipal().subtract(loanOutstanding);
                loan.adjustNetDisbursalAmount(netDisbursalAmount);
            }
            loan.getLoanCustomizationDetail().recordActivity();
            loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
            this.accountTransfersWritePlatformService.reverseAllTransactions(loanId, PortfolioAccountType.LOAN);
            String noteText;
            if (command.hasParameter("note")) {
                noteText = command.stringValueOfParameterNamed("note");
                if (StringUtils.isNotBlank(noteText)) {
                    final Note note = Note.loanNote(loan, noteText);
                    this.noteRepository.save(note);
                }
            }
            boolean isAccountTransfer = false;
            final Map<String, Object> accountingBridgeData = loan.deriveAccountingBridgeData(currency.getCode(), existingTransactionIds,
                    existingReversedTransactionIds, isAccountTransfer);
            journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData);
            loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
            businessEventNotifierService.notifyPostBusinessEvent(new LoanUndoDisbursalBusinessEvent(loan));
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withEntityExternalId(loan.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    @Transactional
    @Override
    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    public CommandProcessingResult makeGLIMLoanRepayment(final Long loanId, final JsonCommand command) {

        final Long parentLoanId = loanId;

        glimRepository.findById(parentLoanId).orElseThrow();

        JsonArray repayments = command.arrayOfParameterNamed("formDataArray");
        JsonCommand childCommand;
        CommandProcessingResult result = null;
        JsonObject jsonObject;

        Long[] childLoanId = new Long[repayments.size()];
        for (int i = 0; i < repayments.size(); i++) {
            jsonObject = repayments.get(i).getAsJsonObject();
            log.debug("{}", jsonObject.toString());
            childLoanId[i] = jsonObject.get("loanId").getAsLong();
        }
        int j = 0;
        for (JsonElement element : repayments) {
            childCommand = JsonCommand.fromExistingCommand(command, element);
            result = makeLoanRepayment(LoanTransactionType.REPAYMENT, childLoanId[j++], childCommand, false);
        }
        return result;
    }

    @Transactional
    @Override
    public CommandProcessingResult makeLoanRepayment(final LoanTransactionType repaymentTransactionType, final Long loanId,
            final JsonCommand command, final boolean isRecoveryRepayment) {
        final String chargeRefundChargeType = null;
        BigDecimal cumulativeHonoFee = BigDecimal.ZERO;
        BigDecimal cumulativeVatFee = BigDecimal.ZERO;
        // SU-516 Calculate the hono charge for repayment only
        if (!isRecoveryRepayment) {
            Loan loan = this.loanAssembler.assembleFrom(loanId);
            Optional<LoanCharge> honoChargeOptional = loan.getLoanCharges().stream().filter(LoanCharge::isFlatHono).findFirst();
            if (honoChargeOptional.isPresent() && loan.getAgeOfOverdueDays(DateUtils.getBusinessLocalDate()) > 0) {
                LoanCharge honoCharge = honoChargeOptional.get();
                Optional<LoanCharge> vatChargeOptional = loan.getLoanCharges().stream()
                        .filter(chg -> chg.isCustomPercentageBasedOfAnotherCharge()
                                && chg.getCharge().getParentChargeId().equals(honoCharge.getCharge().getId()))
                        .findFirst();
                final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
                BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");

                BigDecimal honoAmount = command.bigDecimalValueOfParameterNamed("honorariosAmount");
                if (honoAmount == null) {
                    honoAmount = BigDecimal.ZERO;
                }

                Money remainingAmount = Money.of(loan.getCurrency(), transactionAmount);
                // SU-516 Transaction amount may contain hono amount as well. ReCalculate hono charge amount based on
                // the actual transaction amount
                remainingAmount = remainingAmount.minus(honoAmount);
                Integer installmentNumber = -1;
                // increment the batch id which will be used to delete the rows from db table when a transaction is
                // rollbacked. The rows with highest version will be roll backed
                // because only the latest transaction can be reversed
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
                            fee = this.loanAccountDomainService.updateCalculationHonoLoanChargeOverDueVat(installmentOutstandingAmount,
                                    installment, installmentNumber, version, transactionDate);
                            remainingAmount = remainingAmount.minus(installmentOutstandingAmount);
                        } else {
                            fee = this.loanAccountDomainService.updateCalculationHonoLoanChargeOverDueVat(remainingAmount.getAmount(),
                                    installment, installmentNumber, version, transactionDate);
                            remainingAmount = remainingAmount.zero();
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
                Integer daysInArrears = 0;
                boolean isSuspendedAccount = false;
                Long minimumDaysInArrearsToSuspendLoanAccount = this.configurationDomainService
                        .retriveMinimumDaysInArrearsToSuspendLoanAccount();
                if (minimumDaysInArrearsToSuspendLoanAccount == null) {
                    minimumDaysInArrearsToSuspendLoanAccount = 90L;
                }
                try {
                    daysInArrears = this.jdbcTemplate.queryForObject(
                            "select COALESCE(current_date - overdue_since_date_derived,0) aging_days from m_loan_arrears_aging mlaa where mlaa.loan_id =?",
                            Integer.class, loan.getId());
                } catch (final EmptyResultDataAccessException e) {
                    // not in arrears
                    daysInArrears = 0;
                }
                if (daysInArrears >= minimumDaysInArrearsToSuspendLoanAccount) {
                    isSuspendedAccount = true;
                }
                final Money accrualAmount = Money.of(loan.getCurrency(), cumulativeHonoFee.add(cumulativeVatFee));
                if (accrualAmount.isGreaterThanZero()) {
                    final LoanTransaction applyLoanChargeTransaction = LoanTransaction.accrueInstallmentCharge(loan, loan.getOffice(),
                            accrualAmount, transactionDate, accrualAmount, Money.zero(loan.getCurrency()), ExternalId.empty());
                    if (isSuspendedAccount) {
                        applyLoanChargeTransaction.markAsOccurredOnSuspendedAccount();
                    }
                    final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(applyLoanChargeTransaction, honoCharge,
                            cumulativeHonoFee, installmentNumber);
                    applyLoanChargeTransaction.getLoanChargesPaid().add(loanChargePaidBy);

                    if (vatChargeOptional.isPresent()) {
                        LoanCharge vat = vatChargeOptional.get();

                        final LoanChargePaidBy vatChargePaidBy = new LoanChargePaidBy(applyLoanChargeTransaction, vat, cumulativeVatFee,
                                installmentNumber);
                        applyLoanChargeTransaction.getLoanChargesPaid().add(vatChargePaidBy);
                    }
                    final ClientAdditionalFieldsData clientAdditionalInformation = this.clientReadPlatformService
                            .retrieveClientAdditionalData(loan.getClientId());
                    final String nit = ObjectUtils.defaultIfNull(clientAdditionalInformation.getNit(),
                            clientAdditionalInformation.getCedula());
                    final CollectionHouseConfiguration collectionHouse = this.collectionHouseReadWriteService
                            .retrieveCollectionHouseByClientFromHistory(nit);
                    if (collectionHouse != null) {
                        applyLoanChargeTransaction.setCollectionHouse(collectionHouse);
                    }
                    loan.addLoanTransaction(applyLoanChargeTransaction);
                }
            }
        }
        return makeLoanRepaymentWithChargeRefundChargeType(repaymentTransactionType, loanId, command, isRecoveryRepayment,
                chargeRefundChargeType);
    }

    @Transactional
    @Override
    public CommandProcessingResult makeLoanRepaymentWithChargeRefundChargeType(final LoanTransactionType repaymentTransactionType,
            final Long loanId, final JsonCommand command, final boolean isRecoveryRepayment, final String chargeRefundChargeType) {
        this.loanUtilService.validateRepaymentTransactionType(repaymentTransactionType);
        this.loanEventApiJsonValidator.validateNewRepaymentTransaction(command.json());
        String channelName = command.stringValueOfParameterNamed("channelName");
        final boolean cleanUp = command.booleanPrimitiveValueOfParameterNamed("cleanUp");
        if (StringUtils.isBlank(channelName)) {
            channelName = this.platformSecurityContext.getApiRequestChannel();
        }
        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
        final ExternalId txnExternalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        validateRepaymentDate(transactionDate);

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
        changes.put("transactionAmount", command.stringValueOfParameterNamed("transactionAmount"));
        changes.put("locale", command.locale());
        changes.put("dateFormat", command.dateFormat());
        changes.put("paymentTypeId", command.longValueOfParameterNamed("paymentTypeId"));
        changes.put("pointOfSalesCode", command.stringValueOfParameterNamed("pointOfSalesCode"));

        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
        }
        if (!txnExternalId.isEmpty()) {
            changes.put(LoanApiConstants.externalIdParameterName, txnExternalId);
        }
        Loan loan = this.loanAssembler.assembleFrom(loanId);
        final LoanProduct loanProduct = loan.loanProduct();
        if (!loanProduct.getCustomAllowCollections()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.collection.not.allowed.on.this.product",
                    "Collection is not allowed for this loan product", loanProduct.getName());
        }
        final Long repaymentChannelId = command.longValueOfParameterNamed("repaymentChannelId");
        final boolean isImportedTransaction = command.booleanPrimitiveValueOfParameterNamed("isImportedTransaction");
        ChannelData channelData;
        if (isImportedTransaction) {
            final String clientIdNumber = command.stringValueOfParameterNamed("clientIdNumber");
            final Long clientId = loan.getClientId();
            List<ClientData> clients = this.clientReadPlatformService.retrieveByIdNumber(clientIdNumber);
            if (clients.isEmpty()) {
                throw new ClientNotFoundException("No exite cliente con el NIT/Cedula : " + clientIdNumber, clientIdNumber);
            }
            if (clients.stream().noneMatch(client -> client.getId().equals(clientId))) {
                throw new ClientNotFoundException("El cliente con el NIT/Cedula : " + clientIdNumber + " no pertenece al prestamo",
                        loan.getAccountNumber());
            }
            channelData = this.validateRepaymentChannelById(repaymentChannelId, loanProduct);
        } else {
            channelData = this.validateRepaymentChannel(channelName, loanProduct);
        }
        final Long repaymentBankId = command.longValueOfParameterNamed("repaymentBankId");
        if (channelData.getName().equalsIgnoreCase("Bancos") && repaymentBankId == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.bank.is.required.for.bank.channel",
                    "Bank is mandatory for bank channel", "Bancos");
        }
        final Long channelId = channelData.getId();
        changes.put("channelId", channelId);
        changes.put("channelHash", channelData.getHash());
        changes.put("paymentBankId", repaymentBankId);

        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);
        final Boolean isHolidayValidationDone = false;
        final HolidayDetailDTO holidayDetailDto = null;
        boolean isAccountTransfer = false;

        String loanScheduleProcessingType = command.stringValueOfParameterNamedAllowingNull("transactionProcessingStrategy");
        if (loan.getLoanProductRelatedDetail().getLoanScheduleType().equals(LoanScheduleType.PROGRESSIVE)
                && !StringUtils.isEmpty(loanScheduleProcessingType) && StringUtils.isNotBlank(loanScheduleProcessingType)) {
            if (!loan.getLoanProduct().getProductType().getLabel().equals("SU+ Empresas")) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.transaction.repayment.incorrect.processing.type",
                        String.format("Invalid transaction processing type %s", loanScheduleProcessingType), loanScheduleProcessingType);
            }
            LoanScheduleProcessingType type = null;
            if (loanScheduleProcessingType.equals(LoanScheduleProcessingType.HORIZONTAL.name())) {
                type = LoanScheduleProcessingType.HORIZONTAL;
            } else if (loanScheduleProcessingType.equals(LoanScheduleProcessingType.VERTICAL.name())) {
                type = LoanScheduleProcessingType.VERTICAL;
            } else {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.transaction.repayment.incorrect.processing.type",
                        String.format("Invalid transaction processing type %s", loanScheduleProcessingType), loanScheduleProcessingType);
            }
            loan.setRepaymentTransactionProcessingType(type);
        }

        boolean recalculateEMI = command.booleanPrimitiveValueOfParameterNamed("reduceInstallmentAmount");
        loan.setRecalculateEMI(recalculateEMI);

        final ScheduleGeneratorDTO scheduleGeneratorDTO = loanUtilService.buildScheduleGeneratorDTO(loan, null);
        LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = loan.fetchLoanForeclosureDetail(transactionDate,
                scheduleGeneratorDTO);
        final BigDecimal totalExpectedRepayment = loanRepaymentScheduleInstallment.getTotalOutstanding(loan.getCurrency()).getAmount();
        final boolean isBankChannel = channelData.getName().equalsIgnoreCase("Bancos")
                || channelData.getHash().equalsIgnoreCase("1ae8d4db830eed577c6023998337d0hags546f1a3ba08e5df1ef0d1673431a3");

        if (!isImportedTransaction && !cleanUp) {
            // Add a small tolerance to account for rounding differences and edge cases
            // This prevents false positives when the amount is very close to the outstanding amount
            final BigDecimal tolerance = BigDecimal.valueOf(0.01); // 1 cent tolerance
            if ((transactionAmount.compareTo(totalExpectedRepayment.add(tolerance)) > 0 && !isBankChannel)) {
                log.warn("Repayment validation failed for loan {}: transactionAmount={}, totalExpectedRepayment={}, difference={}",
                        loan.getId(), transactionAmount, totalExpectedRepayment, transactionAmount.subtract(totalExpectedRepayment));
                final String totalOverpaid = transactionAmount.subtract(totalExpectedRepayment).toString();
                handleOverPaidException(totalOverpaid);
            }
        }
        loan.setCleanUp(cleanUp);
        LoanTransaction loanTransaction = this.loanAccountDomainService.makeRepayment(repaymentTransactionType, loan, transactionDate,
                transactionAmount, paymentDetail, noteText, txnExternalId, isRecoveryRepayment, chargeRefundChargeType, isAccountTransfer,
                holidayDetailDto, isHolidayValidationDone);
        loan = loanTransaction.getLoan();

        loanRepaymentScheduleInstallment = loan.fetchLoanForeclosureDetail(transactionDate, scheduleGeneratorDTO);
        final BigDecimal totalOutstandingAmount = loanRepaymentScheduleInstallment.getTotalOutstanding(loan.getCurrency()).getAmount();
        final BigDecimal overpaidAmount = loan.getTotalOverpaid();
        this.handleLoanStatusChange(loan, transactionDate, totalOutstandingAmount, overpaidAmount, isBankChannel, isImportedTransaction);
        // Update loan transaction on repayment.
        if (AccountType.fromInt(loan.getLoanType()).isIndividualAccount()) {
            Set<LoanCollateralManagement> loanCollateralManagements = loan.getLoanCollateralManagements();
            for (LoanCollateralManagement loanCollateralManagement : loanCollateralManagements) {
                loanCollateralManagement.setLoanTransactionData(loanTransaction);
                ClientCollateralManagement clientCollateralManagement = loanCollateralManagement.getClientCollateralManagement();

                if (loan.getStatus().isClosed()) {
                    loanCollateralManagement.setIsReleased(true);
                    BigDecimal quantity = loanCollateralManagement.getQuantity();
                    clientCollateralManagement.updateQuantity(clientCollateralManagement.getQuantity().add(quantity));
                    loanCollateralManagement.setClientCollateralManagement(clientCollateralManagement);
                }
            }
            this.loanAccountDomainService.updateLoanCollateralTransaction(loanCollateralManagements);
        }

        if (loan.getStatus().isClosed()) {
            createCancellationNoveltyNews(loan, loan.getClosedOnDate());
        }

        saveLoanWithDataIntegrityViolationChecks(loan);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()) //
                .withLoanId(loan.getId()) //
                .withEntityId(loanTransaction.getId()) //
                .withEntityExternalId(loanTransaction.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .with(changes) //
                .build();
    }

    private void handleLoanStatusChange(final Loan loan, final LocalDate transactionDate, final BigDecimal totalOutstandingAmount,
            final BigDecimal totalOverpaidAmount, final boolean isBankChannel, final boolean isImportedTransaction) {
        final AppUser currentUser = getAppUserIfPresent();
        if (totalOutstandingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            if (totalOverpaidAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (!isBankChannel && !isImportedTransaction) {
                    handleOverPaidException(totalOutstandingAmount.toString());
                }
                loan.closeAsOverPaid(transactionDate, currentUser);
            } else {
                loan.closeAsObligationsMet(transactionDate, currentUser);
            }
            loan.markInstallmentsAsObligationsMet();
            final BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepositoryWrapper
                    .getSingleBlockingReasonSettingByReason(BlockingReasonSettingEnum.CREDIT_CANCELADO.getDatabaseString(),
                            BlockLevel.CREDIT.toString());
            blockingReasonSetting.setAffectsClientLevel(0);
            loanBlockWritePlatformService.blockLoan(loan.getId(), blockingReasonSetting, "CANCELADO", DateUtils.getLocalDateOfTenant());
        } else {
            loan.updateLoanStatus(LoanStatus.ACTIVE);
        }
    }

    private static void handleOverPaidException(String totalOverpaid) {
        throw new GeneralPlatformDomainRuleException("error.msg.loan.channel.repayment.is.greater.than.outstanding.amount",
                String.format("Repayment rejected for this channel! Repayment amount is greater than the outstanding amount by %s",
                        totalOverpaid),
                totalOverpaid);
    }

    private void createCancellationNoveltyNews(Loan loan, LocalDate writeOffDate) {
        createNoveltyNews(loan, writeOffDate, InsuranceIncidentType.DEFINITIVE_FINAL_CANCELLATION);
    }

    private void createAnulacionNoveltyNews(Loan loan, LocalDate writeOffDate) {
        // Implementation for creating the novelty "Anulación"
        createNoveltyNews(loan, writeOffDate, InsuranceIncidentType.DEFINITIVE_FINAL_INVALIDATION);
    }

    private void createNoveltyNews(Loan loan, LocalDate transactionDate, InsuranceIncidentType incidentType) {
        // Fetch the Insurance Incident based on the provided type
        InsuranceIncident incident = fetchValidIncident(incidentType);

        if (incidentType == InsuranceIncidentType.DEFINITIVE_RESTRUCTURING_CANCELLATION) {
            handleDefinitiveRestructuringCancellation(loan, incident, transactionDate);
        } else {
            processLoanChargesForNoveltyNews(loan, incident, transactionDate);
        }
    }

    private InsuranceIncident fetchValidIncident(InsuranceIncidentType incidentType) {
        InsuranceIncident incident = this.insuranceIncidentRepository.findByIncidentType(incidentType);
        if (incident == null || !incident.isValid()) {
            throw new InsuranceIncidentNotFoundException(incidentType.name());
        }
        return incident;
    }

    private void handleDefinitiveRestructuringCancellation(Loan loan, InsuranceIncident incident, LocalDate transactionDate) {
        boolean doesIncidentExist = this.insuranceIncidentNoveltyNewsRepository.existsByLoanAndIncident(loan.getId(), incident.getId());
        if (!doesIncidentExist) {
            LoanCharge loanCharge = loan.getLoanCharges().stream()
                    .filter(charge -> isChargeEligibleForNoveltyNews(charge, loan.getCurrency(), incident)).findFirst().orElse(null);
            if (loanCharge == null) {
                log.warn("No loan charge found for loan with id: {} and incident type: {}", loan.getId(), incident.getIncidentType());
                return;
            }
            InsuranceIncidentNoveltyNews noveltyNews = InsuranceIncidentNoveltyNews.instance(loan, loanCharge, null, incident,
                    transactionDate, BigDecimal.ZERO);
            this.insuranceIncidentNoveltyNewsRepository.saveAndFlush(noveltyNews);
        }
    }

    private void processLoanChargesForNoveltyNews(Loan loan, InsuranceIncident incident, LocalDate transactionDate) {
        for (LoanCharge loanCharge : loan.getCharges()) {
            if (isChargeEligibleForNoveltyNews(loanCharge, loan.getCurrency(), incident)) {
                InsuranceIncidentNoveltyNews noveltyNews = InsuranceIncidentNoveltyNews.instance(loan, loanCharge, null, incident,
                        transactionDate, BigDecimal.ZERO);
                this.insuranceIncidentNoveltyNewsRepository.saveAndFlush(noveltyNews);
            }
        }
    }

    private boolean isChargeEligibleForNoveltyNews(LoanCharge loanCharge, MonetaryCurrency currency, InsuranceIncident incident) {
        return loanCharge.getAmountOutstanding(currency).isGreaterThanZero()
                && ((incident.isMandatory() && loanCharge.isMandatoryInsurance())
                        || (incident.isVoluntary() && loanCharge.isVoluntaryInsurance()));
    }

    private ChannelData validateRepaymentChannel(final String channelName, final LoanProduct loanProduct) {
        if (StringUtils.isBlank(channelName)) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.is.blank", "Channel is blank");
        }
        final ChannelData channelData = this.channelReadWritePlatformService.findByNameType(channelName, ChannelType.REPAYMENT.getValue());
        if (channelData == null) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.found", "Channel not found", channelName);
        }
        if (!channelData.getActive()) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.active", "Channel is not active", channelName);
        }
        if (ChannelType.REPAYMENT.getValue().longValue() != channelData.getChannelType().getId()) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.repayment", "Channel is not disbursement repayment",
                    channelName);
        }
        final List<Channel> repaymentChannels = loanProduct.getRepaymentChannels();
        if (CollectionUtils.isNotEmpty(repaymentChannels)) {
            final Long channelId = channelData.getId();
            if (repaymentChannels.stream().noneMatch(repaymentChannel -> repaymentChannel.getId().equals(channelId))) {
                throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.allowed", "Channel is not allowed", channelName);
            }
        } else {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.allowed", "Channel is not allowed", channelName);
        }
        return channelData;
    }

    private ChannelData validateRepaymentChannelById(final Long repaymentChannelId, final LoanProduct loanProduct) {
        if (repaymentChannelId == null) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.is.blank", "Channel is blank");
        }
        final ChannelData channelData = this.channelReadWritePlatformService.findById(repaymentChannelId);
        if (channelData == null) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.found", "Channel not found", repaymentChannelId);
        }
        if (!channelData.getActive()) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.active", "Channel is not active", repaymentChannelId);
        }
        if (ChannelType.REPAYMENT.getValue().longValue() != channelData.getChannelType().getId()) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.repayment", "Channel is not disbursement repayment",
                    repaymentChannelId);
        }
        final List<Channel> repaymentChannels = loanProduct.getRepaymentChannels();
        if (CollectionUtils.isNotEmpty(repaymentChannels)) {
            final Long channelId = channelData.getId();
            if (repaymentChannels.stream().noneMatch(repaymentChannel -> repaymentChannel.getId().equals(channelId))) {
                throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.allowed", "Channel is not allowed",
                        repaymentChannelId);
            }
        } else {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.allowed", "Channel is not allowed",
                    repaymentChannelId);
        }
        return channelData;
    }

    @Transactional
    @Override
    public Map<String, Object> makeLoanBulkRepayment(final CollectionSheetBulkRepaymentCommand bulkRepaymentCommand) {

        final SingleRepaymentCommand[] repaymentCommand = bulkRepaymentCommand.getLoanTransactions();
        final Map<String, Object> changes = new LinkedHashMap<>();
        final boolean isRecoveryRepayment = false;

        if (repaymentCommand == null) {
            return changes;
        }
        List<Long> transactionIds = new ArrayList<>();
        boolean isAccountTransfer = false;
        HolidayDetailDTO holidayDetailDTO = null;
        boolean isHolidayValidationDone = false;
        final boolean allowTransactionsOnHoliday = this.configurationDomainService.allowTransactionsOnHolidayEnabled();
        for (final SingleRepaymentCommand singleLoanRepaymentCommand : repaymentCommand) {
            if (singleLoanRepaymentCommand != null) {
                Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(singleLoanRepaymentCommand.getLoanId());
                final List<Holiday> holidays = this.holidayRepository.findByOfficeIdAndGreaterThanDate(loan.getOfficeId(),
                        singleLoanRepaymentCommand.getTransactionDate());
                final WorkingDays workingDays = this.workingDaysRepository.findOne();
                final boolean allowTransactionsOnNonWorkingDay = this.configurationDomainService.allowTransactionsOnNonWorkingDayEnabled();
                boolean isHolidayEnabled;
                isHolidayEnabled = this.configurationDomainService.isRescheduleRepaymentsOnHolidaysEnabled();
                holidayDetailDTO = new HolidayDetailDTO(isHolidayEnabled, holidays, workingDays, allowTransactionsOnHoliday,
                        allowTransactionsOnNonWorkingDay);
                loan.validateRepaymentDateIsOnHoliday(singleLoanRepaymentCommand.getTransactionDate(),
                        holidayDetailDTO.isAllowTransactionsOnHoliday(), holidayDetailDTO.getHolidays());
                loan.validateRepaymentDateIsOnNonWorkingDay(singleLoanRepaymentCommand.getTransactionDate(),
                        holidayDetailDTO.getWorkingDays(), holidayDetailDTO.isAllowTransactionsOnNonWorkingDay());
                isHolidayValidationDone = true;
                break;
            }

        }
        for (final SingleRepaymentCommand singleLoanRepaymentCommand : repaymentCommand) {
            if (singleLoanRepaymentCommand != null) {
                final Loan loan = this.loanAssembler.assembleFrom(singleLoanRepaymentCommand.getLoanId());
                final PaymentDetail paymentDetail = singleLoanRepaymentCommand.getPaymentDetail();
                ExternalId externalId = singleLoanRepaymentCommand.getExternalId();
                if (externalId.isEmpty() && configurationDomainService.isExternalIdAutoGenerationEnabled()) {
                    externalId = ExternalId.generate();
                }
                if (paymentDetail != null && paymentDetail.getId() == null) {
                    this.paymentDetailWritePlatformService.persistPaymentDetail(paymentDetail);
                }
                final String chargeRefundChargeType = null;
                LoanTransaction loanTransaction = this.loanAccountDomainService.makeRepayment(LoanTransactionType.REPAYMENT, loan,
                        bulkRepaymentCommand.getTransactionDate(), singleLoanRepaymentCommand.getTransactionAmount(), paymentDetail,
                        bulkRepaymentCommand.getNote(), externalId, isRecoveryRepayment, chargeRefundChargeType, isAccountTransfer,
                        holidayDetailDTO, isHolidayValidationDone);
                transactionIds.add(loanTransaction.getId());

                if (loan.getStatus().isClosed()) {
                    createCancellationNoveltyNews(loan, loan.getClosedOnDate());
                }
            }
        }
        changes.put("loanTransactions", transactionIds);
        return changes;
    }

    private boolean isTransactionBeforeLastRepaymentTransaction(final LoanTransaction loanTransaction,
            final List<LoanTransaction> loanTransactions) {
        boolean isTransactionNotBeforeLastRepaymentTransaction = true;

        final LocalDate currentTransactionDate = loanTransaction.getTransactionDate();
        for (final LoanTransaction previousTransaction : loanTransactions) {
            if (!previousTransaction.isDisbursement() && previousTransaction.isNotReversed() && !previousTransaction.isAccrual()
                    && DateUtils.compare(currentTransactionDate, previousTransaction.getTransactionDate()) < 0
                    && !Objects.equals(loanTransaction.getId(), previousTransaction.getId())) {
                isTransactionNotBeforeLastRepaymentTransaction = false;
                break;
            }
        }
        return isTransactionNotBeforeLastRepaymentTransaction;
    }

    @Transactional
    @Override
    public CommandProcessingResult adjustLoanTransaction(final Long loanId, final Long transactionId, final JsonCommand command) {
        final AppUser authenticatedUser = context.authenticatedUser();
        this.loanEventApiJsonValidator.validateTransaction(command.json());
        LoanTransaction transactionToAdjust = this.loanTransactionRepository.findByIdAndLoanId(command.entityId(), command.getLoanId())
                .orElseThrow(() -> new LoanTransactionNotFoundException(command.entityId(), command.getLoanId()));
        Loan loan = this.loanAssembler.assembleFrom(loanId);

        if (!isTransactionBeforeLastRepaymentTransaction(transactionToAdjust, loan.getLoanTransactions())) {
            final String errorMessage = "The transaction date cannot be before last valid transaction: "
                    + loan.getDisbursementDate().toString();
            throw new InvalidLoanStateTransitionException("transaction", "cannot.be.before.last.valid.transaction", errorMessage,
                    transactionToAdjust.getTransactionDate(), loan.getDisbursementDate());
        }

        /**
         * if (loan.getStatus().isClosed() && loan.getLoanSubStatus() != null &&
         * loan.getLoanSubStatus().equals(LoanSubStatus.FORECLOSED.getValue())) { final String defaultUserMessage = "The
         * loan cannot reopened as it is foreclosed."; throw new
         * LoanForeclosureException("loan.cannot.be.reopened.as.it.is.foreclosured", defaultUserMessage, loanId); }
         */

        checkClientOrGroupActive(loan);

        checkIfProductAllowsCancelationOrReversal(loan);

        businessEventNotifierService.notifyPreBusinessEvent(
                new LoanAdjustTransactionBusinessEvent(new LoanAdjustTransactionBusinessEvent.Data(transactionToAdjust)));
        if (this.accountTransfersReadPlatformService.isAccountTransfer(transactionId, PortfolioAccountType.LOAN)) {
            throw new PlatformServiceUnavailableException("error.msg.loan.transfer.transaction.update.not.allowed",
                    "Loan transaction:" + transactionId + " update not allowed as it involves in account transfer", transactionId);
        }
        if (loan.isClosedWrittenOff()) {
            throw new PlatformServiceUnavailableException("error.msg.loan.written.off.update.not.allowed",
                    "Loan transaction:" + transactionId + " update not allowed as loan status is written off", transactionId);
        }

        if (transactionToAdjust.hasChargebackLoanTransactionRelations()) {
            throw new PlatformServiceUnavailableException("error.msg.loan.transaction.update.not.allowed",
                    "Loan transaction:" + transactionId + " update not allowed as loan transaction is linked to other transactions",
                    transactionId);
        }
        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
        final ExternalId txnExternalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        final boolean isAdjustCommand = (transactionAmount.compareTo(BigDecimal.ZERO) > 0);
        if (isAdjustCommand && !transactionToAdjust.isEditable()) {
            final String errorMessage = "Loan transaction: " + transactionId + " update not allowed as loan transaction is a "
                    + transactionToAdjust.getTypeOf().getCode();
            throw new InvalidLoanTransactionTypeException("transaction", "error.msg.loan.transaction.update.not.allowed", errorMessage);
        }

        // SU-516 if transaction has a hono charge paid then delete the latest version in hono charge map
        for (LoanChargePaidBy chargePaidBy : transactionToAdjust.getLoanChargesPaid()) {
            if (chargePaidBy.getLoanCharge().isFlatHono()) {
                List<CustomChargeHonorarioMap> remove = new ArrayList<>();
                Long versionToBeDeleted = customChargeHonorarioMapRepository.getMaxVersionByLoan(loanId);
                for (CustomChargeHonorarioMap map : chargePaidBy.getLoanCharge().getCustomChargeHonorarioMaps()) {
                    if (map.getVersion().equals(versionToBeDeleted)) {
                        remove.add(map);
                    }
                }
                remove.forEach(chargePaidBy.getLoanCharge().getCustomChargeHonorarioMaps()::remove);
                customChargeHonorarioMapRepository.deleteLatestVersionMapEntryOnReversal(loanId, versionToBeDeleted);
                transactionToAdjust.getLoanTransactionToRepaymentScheduleMappings().clear();

                // Reverse Accrual Transaction
                for (int i = loan.getLoanTransactions().size(); i >= 0; i--) {
                    LoanTransaction lastTransaction = loan.getLoanTransactions().get(i - 1);
                    if (!lastTransaction.isReversed() && lastTransaction.isAccrual()
                            && lastTransaction.getTransactionDate().equals(transactionToAdjust.getTransactionDate())) {
                        for (LoanChargePaidBy accrualChargePaidBy : lastTransaction.getLoanChargesPaid()) {
                            if (accrualChargePaidBy.getLoanCharge().isFlatHono()) {
                                lastTransaction.manuallyAdjustedOrReversed();
                                lastTransaction.reverse();
                                i = -1;
                                break;
                            }
                        }
                    }
                }
                final LoanRepaymentScheduleProcessingWrapper wrapper = new LoanRepaymentScheduleProcessingWrapper();
                wrapper.reprocess(loan.getCurrency(), loan.getDisbursementDate(), loan.getRepaymentScheduleInstallments(),
                        loan.getActiveCharges());
                break;
            }
        }
        // We don't need auto generation for reversal external id... if it is not provided, it remains null (empty)
        final String reversalExternalId = command.stringValueOfParameterNamedAllowingNull(LoanApiConstants.REVERSAL_EXTERNAL_ID_PARAMNAME);
        final ExternalId reversalTxnExternalId = ExternalIdFactory.produce(reversalExternalId);

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
        changes.put("transactionAmount", command.stringValueOfParameterNamed("transactionAmount"));
        changes.put("locale", command.locale());
        changes.put("dateFormat", command.dateFormat());
        changes.put("paymentTypeId", command.longValueOfParameterNamed("paymentTypeId"));

        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        final Money transactionAmountAsMoney = Money.of(loan.getCurrency(), transactionAmount);
        String channelName = command.stringValueOfParameterNamed("channelName");
        if (StringUtils.isBlank(channelName)) {
            channelName = this.platformSecurityContext.getApiRequestChannel();
        }

        final LoanProduct loanProduct = loan.loanProduct();
        ChannelData channelData;
        if (isAdjustCommand) {
            channelData = this.validateRepaymentChannel(channelName, loanProduct);
            final Long channelId = channelData.getId();
            changes.put("channelId", channelId);
            changes.put("channelHash", channelData.getHash());
            changes.put("paymentBankId", command.longValueOfParameterNamed("repaymentBankId"));
        } else {
            channelData = this.validateUndoRepaymentChannel(channelName, loanProduct, transactionId, loanId);
            if (!authenticatedUser.hasAnyPermission("ALL_FUNCTIONS", "UNDO_REPAYMENT_LOAN")) {
                final LoanTransaction loanTransaction = this.loanTransactionRepository.findByIdAndLoanId(transactionId, loanId)
                        .orElseThrow(() -> new LoanTransactionNotFoundException(transactionId, loanId));
                final LocalDate loanTransactionDate = loanTransaction.getTransactionDate();
                if (!DateUtils.isEqual(DateUtils.getBusinessLocalDate(), loanTransactionDate)) {
                    throw new GeneralPlatformDomainRuleException("validation.msg.undo.repayment.is.permitted.on.the.same.day",
                            "Undo repayment is permitted on the same day", transactionDate);
                }
            }
        }

        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createPaymentDetail(command, changes);
        LoanTransaction newTransactionDetail = LoanTransaction.repayment(loan.getOffice(), transactionAmountAsMoney, paymentDetail,
                transactionDate, txnExternalId);
        if (transactionToAdjust.isInterestWaiver()) {
            Money unrecognizedIncome = transactionAmountAsMoney.zero();
            Money interestComponent = transactionAmountAsMoney;
            if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()) {
                Money receivableInterest = loan.getReceivableInterest(transactionDate);
                if (transactionAmountAsMoney.isGreaterThan(receivableInterest)) {
                    interestComponent = receivableInterest;
                    unrecognizedIncome = transactionAmountAsMoney.minus(receivableInterest);
                }
            }
            newTransactionDetail = LoanTransaction.waiver(loan.getOffice(), loan, transactionAmountAsMoney, transactionDate,
                    interestComponent, unrecognizedIncome, txnExternalId);
        }

        LocalDate recalculateFrom = null;

        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled() || loan.isProgressiveLoan()) {
            recalculateFrom = DateUtils.isAfter(transactionToAdjust.getTransactionDate(), transactionDate) ? transactionDate
                    : transactionToAdjust.getTransactionDate();
        }

        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
        final ChangedTransactionDetail changedTransactionDetail = loan.adjustExistingTransaction(newTransactionDetail,
                defaultLoanLifecycleStateMachine, transactionToAdjust, existingTransactionIds, existingReversedTransactionIds,
                scheduleGeneratorDTO, reversalTxnExternalId);
        boolean thereIsNewTransaction = newTransactionDetail.isGreaterThanZero(loan.getPrincipal().getCurrency());
        if (thereIsNewTransaction) {
            if (paymentDetail != null) {
                this.paymentDetailWritePlatformService.persistPaymentDetail(paymentDetail);
            }
            this.loanTransactionRepository.saveAndFlush(newTransactionDetail);
            final LoanStatus loanStatus = loan.getStatus();
            if (channelData != null) {
                final boolean isBankChannel = channelData.getName().equalsIgnoreCase("Bancos")
                        || channelData.getHash().equalsIgnoreCase("1ae8d4db830eed577c6023998337d0hags546f1a3ba08e5df1ef0d1673431a3");
                if (loanStatus.isOverpaid() && !isBankChannel) {
                    final String totalOverpaid = Money.of(loan.getCurrency(), loan.getTotalOverpaid()).toString();
                    handleOverPaidException(totalOverpaid);
                }
            }
        }

        /*
         * TODO Vishwas Batch save is giving me a HibernateOptimisticLockingFailureException, looping and saving for the
         * time being, not a major issue for now as this loop is entered only in edge cases (when a adjustment is made
         * before the latest payment recorded against the loan)
         */
        if (changedTransactionDetail != null) {
            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                loanAccountDomainService.saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
            }
            // Trigger transaction replayed event
            replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
        }
        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
        // UPDATE BLOCK STATUS ID
        if (loan.getLoanSummary().getTotalOutstanding().compareTo(BigDecimal.ZERO) > 0) {
            BlockingReasonSetting blockStatus = loan.getLoanCustomizationDetail().getBlockStatus();
            if (blockStatus != null) {
                handleUnBlockingCredit(loan, blockStatus.getId());
                saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
            }
        }
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled() || loan.isProgressiveLoan()) {
            scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
            loan.regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
            loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
        }
        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
            Note note;
            /**
             * If a new transaction is not created, associate note with the transaction to be adjusted
             **/
            if (thereIsNewTransaction) {
                note = Note.loanTransactionNote(loan, newTransactionDetail, noteText);
            } else {
                note = Note.loanTransactionNote(loan, transactionToAdjust, noteText);
            }
            this.noteRepository.save(note);
        }

        Collection<Long> transactionIds = new ArrayList<>();
        List<LoanTransaction> transactions = loan.getLoanTransactions();
        for (LoanTransaction transaction : transactions) {
            if (transaction.isRefund() && transaction.isNotReversed()) {
                transactionIds.add(transaction.getId());
            }
        }

        if (!transactionIds.isEmpty()) {
            this.accountTransfersWritePlatformService.reverseTransfersWithFromAccountTransactions(transactionIds,
                    PortfolioAccountType.LOAN);
            loan.updateLoanSummaryAndStatus();
        }

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);

        this.loanAccountDomainService.recalculateAccruals(loan);

        this.loanAccountDomainService.setLoanDelinquencyTag(loan, DateUtils.getBusinessLocalDate());

        LoanAdjustTransactionBusinessEvent.Data eventData = new LoanAdjustTransactionBusinessEvent.Data(transactionToAdjust);
        if (newTransactionDetail.isRepaymentLikeType() && thereIsNewTransaction) {
            eventData.setNewTransactionDetail(newTransactionDetail);
        }
        Long entityId = transactionToAdjust.getId();
        ExternalId entityExternalId = transactionToAdjust.getExternalId();

        if (thereIsNewTransaction) {
            entityId = newTransactionDetail.getId();
            entityExternalId = newTransactionDetail.getExternalId();
        }
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        businessEventNotifierService.notifyPostBusinessEvent(new LoanAdjustTransactionBusinessEvent(eventData));
        if (!isAdjustCommand) {
            this.businessEventNotifierService.notifyPostBusinessEvent(new LoanTxReversalBusinessEvent(transactionToAdjust));
        }
        final LocalDate previousTransactionDate = transactionToAdjust.getTransactionDate();
        if (DateUtils.isEqual(previousTransactionDate, transactionDate)) {
            final Long loanTransactionId = transactionToAdjust.getId();
            final List<FacturaElectronicaMensual> facturaElectronicMensuals = this.facturaElectronicMensualRepository
                    .findByLoanTransactionId(loanTransactionId);
            final List<LoanTransaction> invoicedByTransactions = loan.getLoanTransactions().stream()
                    .filter(ltx -> Objects.equals(loanTransactionId, ltx.getInvoicedByTransactionId())).toList();

            final List<LoanTransaction> partialInvoicedAccrualTransactions = loan.getLoanTransactions().stream()
                    .filter(LoanTransaction::isPartiallyInvoiced).filter(ltx -> {
                        final Set<PartialInvoicedTransaction> partialInvoicedTransactions = ltx.getPartialInvoicedTransactions();
                        return CollectionUtils.isNotEmpty(partialInvoicedTransactions) && partialInvoicedTransactions.stream()
                                .anyMatch(pit -> Objects.equals(loanTransactionId, pit.getRepaymentTransaction().getId()));
                    }).toList();

            for (final LoanTransaction partialInvoicedAccrualTransaction : partialInvoicedAccrualTransactions) {
                final Set<PartialInvoicedTransaction> transactionsToRemove = partialInvoicedAccrualTransaction
                        .getPartialInvoicedTransactions().stream()
                        .filter(pit -> Objects.equals(loanTransactionId, pit.getRepaymentTransaction().getId()))
                        .collect(Collectors.toSet());
                partialInvoicedAccrualTransaction.getPartialInvoicedTransactions().removeAll(transactionsToRemove);
                if (partialInvoicedAccrualTransaction.getPartialInvoicedTransactions().isEmpty()) {
                    partialInvoicedAccrualTransaction.resetPartiallyInvoiced();
                }
            }
            if (CollectionUtils.isNotEmpty(partialInvoicedAccrualTransactions)) {
                this.loanTransactionRepository.saveAll(partialInvoicedAccrualTransactions);
            }
            if (CollectionUtils.isNotEmpty(invoicedByTransactions)) {
                invoicedByTransactions.forEach(LoanTransaction::resetInvoicedByTransactionId);
                this.loanTransactionRepository.saveAll(invoicedByTransactions);
            }
            if (CollectionUtils.isNotEmpty(facturaElectronicMensuals)) {
                this.decrementInvoiceCounterOnProduct(transactionToAdjust, facturaElectronicMensuals);
                this.facturaElectronicMensualRepository.deleteAll(facturaElectronicMensuals);
            }
        }

        if (transactionToAdjust.isForeclosure()) {
            // Delete existing CustomChargeHonorarioMaps for this loan
            this.customChargeHonorarioMapRepository.deleteByLoanId(loanId);
            // Delete existing LoanInstallmentCharges for this loan
            this.loanInstalmentChargeRepository.deleteByLoanId(loanId);
            this.saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
            loan.regenerateRepaymentSchedule(scheduleGeneratorDTO);
            loan.reapplyInsuranceCharges();
            loan.processPostDisbursementTransactions();
            saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
        }

        /* Undo loan block status if loan is still outstanding */
        if (loan.getLoanSummary().getTotalOutstanding().compareTo(BigDecimal.ZERO) > 0) {
            final BlockingReasonSetting blockStatus = loan.getLoanCustomizationDetail().getBlockStatus();
            if (blockStatus != null) {
                handleUnBlockingCredit(loan, blockStatus.getId());
                saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
            }
        }
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(entityId) //
                .withEntityExternalId(entityExternalId) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes).build();
    }

    private void handleUnBlockingCredit(final Loan loan, final Long blockingReasonSettingId) {
        final BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepositoryWrapper
                .findOneWithNotFoundDetection(blockingReasonSettingId);
        final Optional<LoanBlockingReason> existingBlockingReason = this.loanBlockingReasonRepository
                .findExistingBlockingReason(loan.getId(), blockingReasonSetting.getId());
        final AppUser currentUser = context.authenticatedUser();
        if (existingBlockingReason.isPresent()) {
            final LoanBlockingReason blockingReason = this.loanBlockingReasonRepository

                    .findExistingBlockingReason(loan.getId(), blockingReasonSetting.getId())
                    .orElseThrow(() -> new LoanBlockingReasonNotFoundException(loan.getId(), blockingReasonSetting.getId()));
            blockingReason.setActive(false);
            blockingReason.setDeactivatedBy(currentUser);

            blockingReason.setUnblockComment(LoanArrearsAgeingUpdateHandler.UNBLOCKING_COMMENT);
            blockingReason.setDeactivatedOn(DateUtils.getLocalDateOfTenant());
            final BlockingReasonSetting existingBlockingReasonSetting = loan.getLoanCustomizationDetail().getBlockStatus();
            if (existingBlockingReasonSetting != null) {
                final Long existingBlockingSettingId = existingBlockingReasonSetting.getId();
                if (existingBlockingSettingId != null && existingBlockingSettingId.equals(blockingReasonSetting.getId())) {
                    loan.getLoanCustomizationDetail().setBlockStatus(null);
                }
            }
            loanBlockingReasonRepository.saveAndFlush(blockingReason);
        }
    }

    private void decrementInvoiceCounterOnProduct(LoanTransaction transactionToAdjust,
            List<FacturaElectronicaMensual> facturaElectronicMensuals) {
        // If this invoice has the last invoice number, then decrement it on the product
        final String productTypeName = transactionToAdjust.getLoan().loanProduct().getProductType() != null
                ? transactionToAdjust.getLoan().loanProduct().getProductType().getLabel()
                : "";
        final List<LoanProductParameterization> loanProductParameterizations = this.productParameterizationRepository
                .findByProductType(productTypeName);
        if (!loanProductParameterizations.isEmpty()) {
            // we expect exactly one product parameterization
            final LoanProductParameterization loanProductParameterization = loanProductParameterizations.get(0);
            Long invoiceNumber = Long.parseLong(facturaElectronicMensuals.get(0).getNumero_doc());
            if (invoiceNumber.equals(loanProductParameterization.getInvoiceCounter())) {
                loanProductParameterization.setInvoiceCounter(loanProductParameterization.getInvoiceCounter() - 1L);
                this.productParameterizationRepository.saveAndFlush(loanProductParameterization);
            }
        }
    }

    private void checkIfProductAllowsCancelationOrReversal(Loan loan) {
        // validate if the loan product allows Cancellation or Reversal
        if (Boolean.FALSE.equals(loan.loanProduct().getCustomAllowReversalCancellation())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.product.does.not.allow.reversal.nor.cancellation",
                    "Loan product does not allow Reversal nor Cancellation.");
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult chargebackLoanTransaction(final Long loanId, final Long transactionId, final JsonCommand command) {
        this.loanEventApiJsonValidator.validateChargebackTransaction(command.json());

        LoanTransaction loanTransaction = this.loanTransactionRepository.findByIdAndLoanId(command.entityId(), command.getLoanId())
                .orElseThrow(() -> new LoanTransactionNotFoundException(command.entityId(), command.getLoanId()));

        if (loanTransaction.isReversed()) {
            throw new PlatformServiceUnavailableException("error.msg.loan.chargeback.operation.not.allowed",
                    "Loan transaction:" + transactionId + " chargeback not allowed as loan transaction repayment is reversed",
                    transactionId);
        }

        if (!loanTransaction.isTypeAllowedForChargeback()) {
            throw new PlatformServiceUnavailableException(
                    "error.msg.loan.chargeback.operation.not.allowed", "Loan transaction:" + transactionId
                            + " chargeback not allowed for loan transaction type, its type is " + loanTransaction.getTypeOf().getCode(),
                    transactionId);
        }

        Loan loan = this.loanAssembler.assembleFrom(loanId);
        if (this.accountTransfersReadPlatformService.isAccountTransfer(transactionId, PortfolioAccountType.LOAN)) {
            throw new PlatformServiceUnavailableException("error.msg.loan.transfer.transaction.update.not.allowed",
                    "Loan transaction:" + transactionId + " chargeback not allowed as it involves in account transfer", transactionId);
        }
        if (loan.isClosedWrittenOff()) {
            throw new PlatformServiceUnavailableException("error.msg.loan.chargeback.operation.not.allowed",
                    "Loan transaction:" + transactionId + " chargeback not allowed as loan status is written off", transactionId);
        }
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            throw new PlatformServiceUnavailableException("error.msg.loan.chargeback.operation.not.allowed",
                    "Loan transaction:" + transactionId + " chargeback not allowed as loan product is interest recalculation enabled",
                    transactionId);
        }
        checkClientOrGroupActive(loan);

        final List<Long> existingTransactionIds = loan.findExistingTransactionIds();
        final List<Long> existingReversedTransactionIds = loan.findExistingReversedTransactionIds();

        businessEventNotifierService.notifyPreBusinessEvent(new LoanChargebackTransactionBusinessEvent(loanTransaction));

        final LocalDate transactionDate = DateUtils.getBusinessLocalDate();
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.TRANSACTION_AMOUNT_PARAMNAME);
        final ExternalId txnExternalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("transactionAmount", command.stringValueOfParameterNamed(LoanApiConstants.TRANSACTION_AMOUNT_PARAMNAME));
        changes.put("locale", command.locale());
        changes.put("dateFormat", command.dateFormat());
        changes.put("paymentTypeId", command.longValueOfParameterNamed(LoanApiConstants.PAYMENT_TYPE_PARAMNAME));

        final Money transactionAmountAsMoney = Money.of(loan.getCurrency(), transactionAmount);
        PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createPaymentDetail(command, changes);
        if (paymentDetail != null) {
            paymentDetail = this.paymentDetailWritePlatformService.persistPaymentDetail(paymentDetail);
        }
        LoanTransaction newTransaction = LoanTransaction.chargeback(loan, transactionAmountAsMoney, paymentDetail, transactionDate,
                txnExternalId);

        validateLoanTransactionAmountChargeBack(loanTransaction, newTransaction);

        // Store the Loan Transaction Relation
        LoanTransactionRelation loanTransactionRelation = LoanTransactionRelation.linkToTransaction(loanTransaction, newTransaction,
                LoanTransactionRelationTypeEnum.CHARGEBACK);
        this.loanTransactionRelationRepository.save(loanTransactionRelation);

        newTransaction = this.loanTransactionRepository.saveAndFlush(newTransaction);

        loan.handleChargebackTransaction(newTransaction, defaultLoanLifecycleStateMachine);

        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        final String noteText = command.stringValueOfParameterNamed(LoanApiConstants.noteParamName);
        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
            Note note = Note.loanTransactionNote(loan, newTransaction, noteText);
            this.noteRepository.save(note);
        }

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        this.loanAccountDomainService.setLoanDelinquencyTag(loan, transactionDate);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanChargebackTransactionBusinessEvent(newTransaction));
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(newTransaction.getId()) //
                .withEntityExternalId(newTransaction.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes).build();
    }

    private void validateLoanTransactionAmountChargeBack(LoanTransaction loanTransaction, LoanTransaction chargebackTransaction) {
        BigDecimal actualAmount = BigDecimal.ZERO;
        for (LoanTransactionRelation loanTransactionRelation : loanTransaction.getLoanTransactionRelations()) {
            if (loanTransactionRelation.getRelationType().equals(LoanTransactionRelationTypeEnum.CHARGEBACK)
                    && loanTransactionRelation.getToTransaction().isNotReversed()) {
                actualAmount = actualAmount.add(loanTransactionRelation.getToTransaction().getAmount());
            }
        }
        actualAmount = actualAmount.add(chargebackTransaction.getAmount());
        if (loanTransaction.getAmount() != null && actualAmount.compareTo(loanTransaction.getAmount()) > 0) {
            throw new PlatformServiceUnavailableException("error.msg.loan.chargeback.operation.not.allowed",
                    "Loan transaction:" + loanTransaction.getId() + " chargeback not allowed as loan transaction amount is not enough",
                    loanTransaction.getId());
        }
    }

    private void checkIfProductAllowsWaivePrincipalOrInterest(Loan loan) {
        if (Boolean.FALSE.equals(loan.loanProduct().getCustomAllowForgiveness())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.product.does.not.allow.forgiveness",
                    "Loan product does not allow Waive Principal Nor Interest.");
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult waiveInterestOnLoan(final Long loanId, final JsonCommand command) {

        this.loanEventApiJsonValidator.validateTransaction(command.json());

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
        changes.put("transactionAmount", command.stringValueOfParameterNamed("transactionAmount"));
        changes.put("locale", command.locale());
        changes.put("dateFormat", command.dateFormat());
        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
        final ExternalId externalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        Loan loan = this.loanAssembler.assembleFrom(loanId);
        checkClientOrGroupActive(loan);

        checkIfProductAllowsWaivePrincipalOrInterest(loan);

        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        final Money transactionAmountAsMoney = Money.of(loan.getCurrency(), transactionAmount);
        Money unrecognizedIncome = transactionAmountAsMoney.zero();
        Money interestComponent = transactionAmountAsMoney;
        if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()) {
            Money receivableInterest = loan.getReceivableInterest(transactionDate);
            if (transactionAmountAsMoney.isGreaterThan(receivableInterest)) {
                interestComponent = receivableInterest;
                unrecognizedIncome = transactionAmountAsMoney.minus(receivableInterest);
            }
        }
        final LoanTransaction waiveInterestTransaction = LoanTransaction.waiver(loan.getOffice(), loan, transactionAmountAsMoney,
                transactionDate, interestComponent, unrecognizedIncome, externalId);
        businessEventNotifierService.notifyPreBusinessEvent(new LoanWaiveInterestBusinessEvent(waiveInterestTransaction));
        LocalDate recalculateFrom = null;
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            recalculateFrom = transactionDate;
        }

        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
        final ChangedTransactionDetail changedTransactionDetail = loan.waiveInterest(waiveInterestTransaction,
                defaultLoanLifecycleStateMachine, existingTransactionIds, existingReversedTransactionIds, scheduleGeneratorDTO);

        this.loanTransactionRepository.saveAndFlush(waiveInterestTransaction);

        /***
         * TODO Vishwas Batch save is giving me a HibernateOptimisticLockingFailureException, looping and saving for the
         * time being, not a major issue for now as this loop is entered only in edge cases (when a waiver is made
         * before the latest payment recorded against the loan)
         ***/

        if (changedTransactionDetail != null) {
            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                loanAccountDomainService.saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
            }
            // Trigger transaction replayed event
            replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
        }
        loan.getLoanCustomizationDetail().recordActivity();
        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
            final Note note = Note.loanTransactionNote(loan, waiveInterestTransaction, noteText);
            this.noteRepository.save(note);
        }

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        loanAccountDomainService.recalculateAccruals(loan);
        loanAccountDomainService.setLoanDelinquencyTag(loan, DateUtils.getBusinessLocalDate());

        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        businessEventNotifierService.notifyPostBusinessEvent(new LoanWaiveInterestBusinessEvent(waiveInterestTransaction));
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(waiveInterestTransaction.getId()) //
                .withEntityExternalId(waiveInterestTransaction.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult writeOff(final Long loanId, final JsonCommand command) {
        final AppUser currentUser = getAppUserIfPresent();

        this.loanEventApiJsonValidator.validateTransactionWithNoAmount(command.json());

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
        changes.put("locale", command.locale());
        changes.put("dateFormat", command.dateFormat());
        LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        if (command.hasParameter("writeoffReasonId")) {
            Long writeoffReasonId = command.longValueOfParameterNamed("writeoffReasonId");
            CodeValue writeoffReason = this.codeValueRepository
                    .findOneByCodeNameAndIdWithNotFoundDetection(LoanApiConstants.WRITEOFFREASONS, writeoffReasonId);
            changes.put("writeoffReasonId", writeoffReasonId);
            loan.updateWriteOffReason(writeoffReason);
        }

        checkClientOrGroupActive(loan);
        if (loan.isChargedOff() && DateUtils.isBefore(transactionDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.earlier.than.charge.off.date", "Loan: "
                    + loanId
                    + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the loan",
                    loanId);
        }
        businessEventNotifierService.notifyPreBusinessEvent(new LoanWrittenOffPreBusinessEvent(loan));
        entityDatatableChecksWritePlatformService.runTheCheckForProduct(loanId, EntityTables.LOAN.getName(),
                StatusEnum.WRITE_OFF.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(), loan.productId());

        removeLoanCycle(loan);

        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        updateLoanCounters(loan, loan.getDisbursementDate());

        LocalDate recalculateFrom = null;
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            recalculateFrom = command.localDateValueOfParameterNamed("transactionDate");
        }

        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

        final ChangedTransactionDetail changedTransactionDetail = loan.closeAsWrittenOff(command, defaultLoanLifecycleStateMachine, changes,
                existingTransactionIds, existingReversedTransactionIds, currentUser, scheduleGeneratorDTO);
        LoanTransaction writeOff = changedTransactionDetail.getNewTransactionMappings().remove(0L);
        this.loanTransactionRepository.saveAndFlush(writeOff);
        for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
            this.loanTransactionRepository.save(mapEntry.getValue());
            this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
        }
        saveLoanWithDataIntegrityViolationChecks(loan);
        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
            final Note note = Note.loanTransactionNote(loan, writeOff, noteText);
            this.noteRepository.save(note);
        }

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        loanAccountDomainService.recalculateAccruals(loan);
        loanAccountDomainService.setLoanDelinquencyTag(loan, DateUtils.getBusinessLocalDate());
        createWriteOffNoveltyNews(loan, transactionDate);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        businessEventNotifierService.notifyPostBusinessEvent(new LoanWrittenOffPostBusinessEvent(writeOff));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(writeOff.getId()) //
                .withEntityExternalId(writeOff.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes).build();
    }

    private void createWriteOffNoveltyNews(Loan loan, LocalDate writeOffDate) {
        InsuranceIncident incident = this.insuranceIncidentRepository
                .findByIncidentType(InsuranceIncidentType.PORTFOLIO_WRITE_OFF_CANCELLATION);
        if (incident == null || (!incident.isMandatory() && !incident.isVoluntary())) {
            throw new InsuranceIncidentNotFoundException(InsuranceIncidentType.PORTFOLIO_WRITE_OFF_CANCELLATION.name());
        }

        for (LoanCharge loanCharge : loan.getCharges()) {
            if (loanCharge.getAmountOutstanding(loan.getCurrency()).isGreaterThanZero()) {
                if ((incident.isMandatory() && loanCharge.isMandatoryInsurance())
                        || (incident.isVoluntary() && loanCharge.isVoluntaryInsurance())) {
                    BigDecimal cumulative = BigDecimal.ZERO;
                    InsuranceIncidentNoveltyNews insuranceIncidentNoveltyNews = InsuranceIncidentNoveltyNews.instance(loan, loanCharge,
                            null, incident, writeOffDate, cumulative);

                    this.insuranceIncidentNoveltyNewsRepository.saveAndFlush(insuranceIncidentNoveltyNews);
                }
            }
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult specialWriteOff(final Long loanId, final JsonCommand command, LoanCreditNote creditNote) {
        this.loanEventApiJsonValidator.validateSpecialWriteOff(command.json());
        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("locale", command.locale());
        changes.put("dateFormat", command.dateFormat());
        Loan loan = this.loanAssembler.assembleFrom(loanId);
        final LoanProduct loanProduct = loan.getLoanProduct();
        if (!loanProduct.getCustomAllowForgiveness()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.product.write.off.is.disabled.on.product",
                    "Loan write-off is disabled on this product");
        }
        if (command.hasParameter("writeoffReasonId")) {
            Long writeoffReasonId = command.longValueOfParameterNamed("writeoffReasonId");
            CodeValue writeoffReason = this.codeValueRepository
                    .findOneByCodeNameAndIdWithNotFoundDetection(LoanApiConstants.WRITEOFFREASONS, writeoffReasonId);
            changes.put("writeoffReasonId", writeoffReasonId);
            loan.updateWriteOffReason(writeoffReason);
        }
        checkClientOrGroupActive(loan);
        businessEventNotifierService.notifyPreBusinessEvent(new LoanWrittenOffPreBusinessEvent(loan));
        entityDatatableChecksWritePlatformService.runTheCheckForProduct(loanId, EntityTables.LOAN.getName(),
                StatusEnum.WRITE_OFF.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(), loan.productId());
        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();
        final LocalDate recalculateFrom = null;
        final ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
        final LocalDate transactionDate = DateUtils.getBusinessLocalDate();
        final String txnExternalId = command.stringValueOfParameterNamedAllowingNull("externalId");
        ExternalId externalId = ExternalIdFactory.produce(txnExternalId);
        if (externalId.isEmpty() && TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
            externalId = ExternalId.generate();
        }
        changes.put("externalId", externalId);
        ChangedTransactionDetail changedTransactionDetail = loan.validateSpecialWrittenOff(command, changes, existingTransactionIds,
                existingReversedTransactionIds, scheduleGeneratorDTO);
        final String noteText = command.stringValueOfParameterNamed("note");
        final boolean isImportedTransaction = command.booleanPrimitiveValueOfParameterNamed("isImportedTransaction");
        LoanTransaction writeOffTransaction;
        if (isImportedTransaction) {
            final BigDecimal totalWriteOffAmount = command.bigDecimalValueOfParameterNamed("totalWriteOffAmount");
            final BigDecimal totalOutstandingAmount = loan.getLoanSummary().getTotalOutstanding();
            if (totalWriteOffAmount != null && totalOutstandingAmount != null
                    && totalWriteOffAmount.compareTo(totalOutstandingAmount) > 0) {
                final BigDecimal totalOverpaidAmount = totalWriteOffAmount.subtract(totalOutstandingAmount);
                throw new GeneralPlatformDomainRuleException("error.msg.loan.write.off.amount.is.greater.than.outstanding.loan.amount",
                        "Condonación supera deuda", totalWriteOffAmount, totalOverpaidAmount, totalOutstandingAmount);
            }
            final PaymentDetail paymentDetail = null;
            final boolean isRecoveryRepayment = false;
            final String chargeRefundChargeType = null;
            final boolean isAccountTransfer = false;
            final HolidayDetailDTO holidayDetailDto = null;
            final boolean isHolidayValidationDone = false;
            final boolean isCreditNote = command.booleanPrimitiveValueOfParameterNamed("isCreditNote");
            LoanTransactionType transactionType = null;
            if (isCreditNote) {
                transactionType = LoanTransactionType.CREDIT_NOTE;
            } else {
                transactionType = LoanTransactionType.WRITEOFF;
            }
            writeOffTransaction = this.loanAccountDomainService.makeRepayment(transactionType, loan, transactionDate, totalWriteOffAmount,
                    paymentDetail, noteText, externalId, isRecoveryRepayment, chargeRefundChargeType, isAccountTransfer, holidayDetailDto,
                    isHolidayValidationDone);
        } else {
            final MonetaryCurrency currency = loan.getCurrency();
            final LoanRepaymentScheduleInstallment specialWriteOffInstallment = loan.fetchLoanSpecialWriteOffDetail(transactionDate,
                    scheduleGeneratorDTO);
            final LoanRepaymentScheduleInstallmentData loanRepaymentScheduleInstallmentData = loan.validateSpecialWriteOffConcepts(command,
                    specialWriteOffInstallment);
            final BigDecimal principalToBeWrittenOff = loanRepaymentScheduleInstallmentData.getPrincipalPortion();
            final Money remainingPrincipalPortion = specialWriteOffInstallment.getPrincipalOutstanding(currency)
                    .minus(principalToBeWrittenOff);
            final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments = loan.getRepaymentScheduleInstallments();
            final LoanRepaymentScheduleInstallment currentScheduleInstallment = fetchRepaymentInstallmentByWrittenOfDate(transactionDate,
                    repaymentScheduleInstallments);

            Money interestToBeChargedAfterWriteOff = currentScheduleInstallment.getInterestCharged(currency);
            if (remainingPrincipalPortion.isGreaterThanZero()
                    && specialWriteOffInstallment.getPrincipalOutstanding(currency).isGreaterThanZero()) {
                final Integer currentInstallmentNumber = currentScheduleInstallment.getInstallmentNumber();
                Money unpaidPrincipalUptoCurrentInstallment = Money.zero(currency);
                for (final LoanRepaymentScheduleInstallment repaymentScheduleInstallment : loan.getRepaymentScheduleInstallments()) {
                    if (repaymentScheduleInstallment.getInstallmentNumber() <= currentInstallmentNumber) {
                        final Money outstandingPrincipalAmount = repaymentScheduleInstallment.getPrincipalOutstanding(currency);
                        unpaidPrincipalUptoCurrentInstallment = unpaidPrincipalUptoCurrentInstallment.plus(outstandingPrincipalAmount);
                    }
                }
                if (Money.of(currency, principalToBeWrittenOff).isGreaterThan(unpaidPrincipalUptoCurrentInstallment)) {
                    final LoanApplicationTerms loanApplicationTerms = loan.constructLoanApplicationTerms(scheduleGeneratorDTO);
                    final LoanScheduleGenerator loanScheduleGenerator = this.loanScheduleFactory
                            .create(loanApplicationTerms.getLoanScheduleType(), loanApplicationTerms.getInterestMethod());
                    final Set<LoanCharge> loanCharges = loan.getActiveCharges();
                    final HolidayDetailDTO holidayDetailDTO = loanApplicationTerms.getHolidayDetailDTO();
                    final MathContext mc = MoneyHelper.getMathContext();
                    final Integer numberOfRepayments = loanApplicationTerms.getNumberOfRepayments();
                    if (currentInstallmentNumber < numberOfRepayments) {
                        final LoanRepaymentScheduleInstallment nextRescheduleInstallment = repaymentScheduleInstallments
                                .get(currentInstallmentNumber);
                        int totalPeriodDays = Math.toIntExact(
                                ChronoUnit.DAYS.between(currentScheduleInstallment.getFromDate(), currentScheduleInstallment.getDueDate()));
                        int currentTillDays = Math
                                .toIntExact(ChronoUnit.DAYS.between(currentScheduleInstallment.getFromDate(), transactionDate));
                        int futureTillDays = Math
                                .toIntExact(ChronoUnit.DAYS.between(transactionDate, currentScheduleInstallment.getDueDate()));
                        final Money interestForCurrentPeriod = Money.of(currency,
                                BigDecimal.valueOf(loan.calculateInterestForDays(totalPeriodDays,
                                        currentScheduleInstallment.getInterestCharged(currency).getAmount(), currentTillDays)));
                        final Money fixedEmiAmount = nextRescheduleInstallment.getInterestCharged(currency)
                                .plus(nextRescheduleInstallment.getPrincipal(currency));
                        Integer writeOffNumberOfRepayments = numberOfRepayments - currentInstallmentNumber + 1;
                        loanApplicationTerms.updateLoanTermVariations(new ArrayList<>());
                        loanApplicationTerms.updateNumberOfRepayments(writeOffNumberOfRepayments);
                        loanApplicationTerms.updateLoanTermFrequency(writeOffNumberOfRepayments);
                        loanApplicationTerms.setPrincipal(remainingPrincipalPortion);
                        loanApplicationTerms.updateApprovedPrincipal(remainingPrincipalPortion);
                        loanApplicationTerms.updateInterestChargedFromDate(transactionDate);
                        loanApplicationTerms.updateExpectedDisbursementDate(transactionDate);
                        loanApplicationTerms.updateCalculatedRepaymentsStartingFromDate(currentScheduleInstallment.getDueDate());
                        loanApplicationTerms.updateRepaymentsStartingFromDate(currentScheduleInstallment.getDueDate());
                        loanApplicationTerms.setFixedEmiAmount(fixedEmiAmount.getAmount());

                        LoanScheduleModel loanScheduleModel = loanScheduleGenerator.generate(mc, loanApplicationTerms, loanCharges,
                                holidayDetailDTO);
                        final LoanScheduleModelPeriod midScheduleInstallment = loanScheduleModel.getPeriods().stream()
                                .filter(period -> period.periodNumber() != null && period.periodNumber() > 0)
                                .filter(period -> period.isRepaymentPeriod() || period.isDownPaymentPeriod()).findFirst()
                                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.loan.schedule.period.not.found",
                                        "Loan schedule period not found"));
                        final Money midInterestForCurrentPeriod = Money.of(currency, BigDecimal.valueOf(
                                loan.calculateInterestForDays(totalPeriodDays, midScheduleInstallment.interestDue(), futureTillDays)));
                        interestToBeChargedAfterWriteOff = interestForCurrentPeriod.plus(midInterestForCurrentPeriod);

                        final Money restPeriodPrincipalAmount = repaymentScheduleInstallments.stream()
                                .filter(installment -> installment.getInstallmentNumber() > currentInstallmentNumber)
                                .map(installment -> installment.getPrincipal(currency)).reduce(Money.zero(currency), Money::add);
                        final LocalDate installmentFromDate = nextRescheduleInstallment.getFromDate();
                        final LocalDate installmentDueDate = nextRescheduleInstallment.getDueDate();
                        writeOffNumberOfRepayments = numberOfRepayments - currentInstallmentNumber;
                        loanApplicationTerms.updateLoanTermVariations(new ArrayList<>());
                        loanApplicationTerms.updateNumberOfRepayments(writeOffNumberOfRepayments);
                        loanApplicationTerms.updateLoanTermFrequency(writeOffNumberOfRepayments);
                        loanApplicationTerms.setPrincipal(restPeriodPrincipalAmount);
                        loanApplicationTerms.updateApprovedPrincipal(restPeriodPrincipalAmount);
                        loanApplicationTerms.updateInterestChargedFromDate(installmentFromDate);
                        loanApplicationTerms.updateExpectedDisbursementDate(installmentFromDate);
                        loanApplicationTerms.updateCalculatedRepaymentsStartingFromDate(installmentDueDate);
                        loanApplicationTerms.updateRepaymentsStartingFromDate(installmentDueDate);
                        loanApplicationTerms.setFixedEmiAmount(fixedEmiAmount.getAmount());

                        loanScheduleModel = loanScheduleGenerator.generate(mc, loanApplicationTerms, loanCharges, holidayDetailDTO);
                        final List<LoanScheduleModelPeriod> loanScheduleModelPeriods = loanScheduleModel.getPeriods();
                        int numberOfRegeneratedInstallments = 0;
                        int regeneratedInstallmentNumber = currentInstallmentNumber + 1;
                        for (final LoanScheduleModelPeriod scheduledLoanInstallment : loanScheduleModelPeriods) {
                            if (scheduledLoanInstallment.periodNumber() != null && scheduledLoanInstallment.periodNumber() > 0) {
                                if (scheduledLoanInstallment.isRepaymentPeriod() || scheduledLoanInstallment.isDownPaymentPeriod()) {
                                    Integer finalRegeneratedInstallmentNumber = regeneratedInstallmentNumber;
                                    LoanRepaymentScheduleInstallment updatedInstallment = repaymentScheduleInstallments.stream().filter(
                                            installment -> installment.getInstallmentNumber().equals(finalRegeneratedInstallmentNumber))
                                            .findFirst().orElseThrow(() -> new GeneralPlatformDomainRuleException(
                                                    "error.msg.loan.schedule.period.not.found", "Loan schedule period not found"));
                                    updatedInstallment.adjustSpecialWriteOff(scheduledLoanInstallment.periodFromDate(),
                                            scheduledLoanInstallment.periodDueDate(), scheduledLoanInstallment.principalDue(),
                                            scheduledLoanInstallment.interestDue(), scheduledLoanInstallment.feeChargesDue(),
                                            scheduledLoanInstallment.penaltyChargesDue(),
                                            scheduledLoanInstallment.isRecalculatedInterestComponent(),
                                            scheduledLoanInstallment.getLoanCompoundingDetails(),
                                            scheduledLoanInstallment.rescheduleInterestPortion(),
                                            scheduledLoanInstallment.isDownPaymentPeriod());
                                    numberOfRegeneratedInstallments++;
                                    regeneratedInstallmentNumber++;
                                }
                            }
                        }

                        final List<LoanRepaymentScheduleInstallment> repaymentInstallmentsToRemove = new ArrayList<>();
                        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
                            if (installment.getInstallmentNumber() > currentInstallmentNumber + numberOfRegeneratedInstallments) {
                                repaymentInstallmentsToRemove.add(installment);
                            }
                        }
                        for (final LoanRepaymentScheduleInstallment installment : repaymentInstallmentsToRemove) {
                            loan.removeLoanRepaymentScheduleInstallment(installment.getInstallmentNumber());
                        }
                        final Money futurePrincipal = repaymentInstallmentsToRemove.stream()
                                .map(installment -> installment.getPrincipal(currency)).reduce(Money.zero(currency), Money::add);
                        final Money futurePrincipalWrittenOff = repaymentInstallmentsToRemove.stream()
                                .map(installment -> installment.getPrincipalWrittenOff(currency)).reduce(Money.zero(currency), Money::add);
                        final Money futurePrincipalPaid = repaymentInstallmentsToRemove.stream()
                                .map(installment -> installment.getPrincipalCompleted(currency)).reduce(Money.zero(currency), Money::add);
                        final BigDecimal adjustedPrincipalForCurrentPeriod = currentScheduleInstallment.getPrincipal(currency)
                                .plus(futurePrincipal).getAmount();
                        final BigDecimal adjustedPrincipalWrittenOffForCurrentPeriod = currentScheduleInstallment
                                .getPrincipalWrittenOff(currency).plus(futurePrincipalWrittenOff).getAmount();
                        final BigDecimal adjustedPrincipalPaidForCurrentPeriod = currentScheduleInstallment.getPrincipalCompleted(currency)
                                .plus(futurePrincipalPaid).getAmount();
                        currentScheduleInstallment.updatePrincipal(adjustedPrincipalForCurrentPeriod);
                        currentScheduleInstallment.updatePrincipalPaid(adjustedPrincipalPaidForCurrentPeriod);
                        currentScheduleInstallment.updatePrincipalWrittenOff(adjustedPrincipalWrittenOffForCurrentPeriod);
                        saveAndFlushLoanWithIntegrityChecks(loan);
                    }
                }
            } else {
                final Money interestToBeWrittenOff = loanRepaymentScheduleInstallmentData.getInterestPortion(currency);
                final Money feeChargesToBeWrittenOff = loanRepaymentScheduleInstallmentData.getFeeChargesPortion(currency);
                final Money penaltyChargesToBeWrittenOff = loanRepaymentScheduleInstallmentData.getPenaltyChargesPortion(currency);
                final Money interestAmountRemaining = specialWriteOffInstallment.getInterestOutstanding(currency)
                        .minus(interestToBeWrittenOff);
                final Money feeChargesAmountRemaining = specialWriteOffInstallment.getFeeChargesOutstanding(currency)
                        .minus(feeChargesToBeWrittenOff);
                final Money penaltyChargesAmountRemaining = specialWriteOffInstallment.getPenaltyChargesOutstanding(currency)
                        .minus(penaltyChargesToBeWrittenOff);
                Money futurePrincipal = Money.zero(currency);
                Money futurePrincipalWrittenOff = Money.zero(currency);
                Money futurePrincipalPaid = Money.zero(currency);
                final List<LoanRepaymentScheduleInstallment> repaymentInstallmentsToRemove = new ArrayList<>();
                for (final LoanRepaymentScheduleInstallment scheduleInstallment : repaymentScheduleInstallments) {
                    if (scheduleInstallment.getInstallmentNumber() > currentScheduleInstallment.getInstallmentNumber()) {
                        futurePrincipal = futurePrincipal.plus(scheduleInstallment.getPrincipal(currency));
                        futurePrincipalWrittenOff = futurePrincipal.plus(scheduleInstallment.getPrincipalWrittenOff(currency));
                        futurePrincipalPaid = futurePrincipalPaid.plus(scheduleInstallment.getPrincipalCompleted(currency));
                        repaymentInstallmentsToRemove.add(scheduleInstallment);
                    }
                }
                for (final LoanRepaymentScheduleInstallment installment : repaymentInstallmentsToRemove) {
                    loan.removeLoanRepaymentScheduleInstallment(installment.getInstallmentNumber());
                }
                final BigDecimal adjustedPrincipalForCurrentPeriod = currentScheduleInstallment.getPrincipal(currency).plus(futurePrincipal)
                        .getAmount();
                final BigDecimal adjustedPrincipalWrittenOffForCurrentPeriod = currentScheduleInstallment.getPrincipalWrittenOff(currency)
                        .plus(futurePrincipalWrittenOff).getAmount();
                final BigDecimal adjustedPrincipalPaidForCurrentPeriod = currentScheduleInstallment.getPrincipalCompleted(currency)
                        .plus(futurePrincipalPaid).getAmount();
                currentScheduleInstallment.updatePrincipal(adjustedPrincipalForCurrentPeriod);
                currentScheduleInstallment.updatePrincipalPaid(adjustedPrincipalPaidForCurrentPeriod);
                currentScheduleInstallment.updatePrincipalWrittenOff(adjustedPrincipalWrittenOffForCurrentPeriod);

                if (interestAmountRemaining.isZero() && feeChargesAmountRemaining.isZero() && penaltyChargesAmountRemaining.isZero()) {
                    int totalPeriodDays = Math.toIntExact(
                            ChronoUnit.DAYS.between(currentScheduleInstallment.getFromDate(), currentScheduleInstallment.getDueDate()));
                    int tillDays = Math.toIntExact(ChronoUnit.DAYS.between(currentScheduleInstallment.getFromDate(), transactionDate));
                    if (!DateUtils.isAfter(transactionDate, currentScheduleInstallment.getDueDate())) {
                        interestToBeChargedAfterWriteOff = Money.of(currency,
                                BigDecimal.valueOf(loan.calculateInterestForDays(totalPeriodDays,
                                        currentScheduleInstallment.getInterestCharged(currency).getAmount(), tillDays)));
                    }
                }
                saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
            }
            final boolean isCreditNote = command.booleanPrimitiveValueOfParameterNamed("isCreditNote");
            if (isCreditNote && creditNote != null) {
                loanRepaymentScheduleInstallmentData.setLoanWriteOffChargeData(creditNote.toChargeData());
            }
            writeOffTransaction = loan.writeOff(loanRepaymentScheduleInstallmentData, transactionDate, externalId, isCreditNote);

            final Money totalWriteOffAmount = Money.of(currency, loanRepaymentScheduleInstallmentData.getTotalInstallmentAmount());
            final Money totalOutstandingAmount = specialWriteOffInstallment.getTotalOutstanding(currency);
            if (totalWriteOffAmount.isGreaterThanOrEqualTo(totalOutstandingAmount)) {
                currentScheduleInstallment.updateComponentsAfterClosureAsWriteOff();
            } else {
                currentScheduleInstallment.updateInterestCharged(interestToBeChargedAfterWriteOff.getAmount());
            }

            loan.updateLoanSummaryDerivedFields();
            loan.getRepaymentScheduleInstallments().forEach(rp -> rp.checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency));
            final Money totalPaymentAmount = Money.of(currency, loanRepaymentScheduleInstallmentData.getTotalInstallmentAmount());
            if (totalPaymentAmount.isEqualTo(totalOutstandingAmount) || loan.getLoanSummary().isRepaidInFull(loan.getCurrency())) {
                final AppUser currentUser = getAppUserIfPresent();
                loan.closeAsWrittenOff(transactionDate, currentUser);
            }
        }
        loan = writeOffTransaction.getLoan();
        final LoanStatus loanStatus = loan.getStatus();
        if (LoanStatus.CLOSED_WRITTEN_OFF.equals(loanStatus)) {
            final BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepositoryWrapper
                    .getSingleBlockingReasonSettingByReason(BlockingReasonSettingEnum.CREDIT_CANCELADO.getDatabaseString(),
                            BlockLevel.CREDIT.toString());
            blockingReasonSetting.setAffectsClientLevel(0);
            loanBlockWritePlatformService.blockLoan(loan.getId(), blockingReasonSetting, "CANCELADO", DateUtils.getLocalDateOfTenant());
        }

        if (loanStatus.isOverpaid()) {
            final Money writeOffAmount = writeOffTransaction.getAmount(loan.getCurrency());
            final Money totalOverpaidBy = Money.of(loan.getCurrency(), loan.getTotalOverpaid());
            final Money totalOutstanding = writeOffAmount.minus(totalOverpaidBy);
            throw new GeneralPlatformDomainRuleException("error.msg.loan.write.off.amount.is.greater.than.outstanding.loan.amount",
                    "Condonación supera deuda", writeOffAmount.getAmount(), totalOverpaidBy.getAmount(), totalOutstanding.getAmount());
        }
        this.loanAccountDomainService.saveLoanTransactionWithDataIntegrityViolationChecks(writeOffTransaction);
        for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
            this.loanTransactionRepository.save(mapEntry.getValue());
            this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
        }
        saveLoanWithDataIntegrityViolationChecks(loan);
        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
            final Note note = Note.loanTransactionNote(loan, writeOffTransaction, noteText);
            this.noteRepository.save(note);
        }
        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        loanAccountDomainService.recalculateAccruals(loan);
        loanAccountDomainService.setLoanDelinquencyTag(loan, DateUtils.getBusinessLocalDate());
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        businessEventNotifierService.notifyPostBusinessEvent(new LoanWrittenOffPostBusinessEvent(writeOffTransaction));
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(writeOffTransaction.getId())
                .withEntityExternalId(writeOffTransaction.getExternalId()).withOfficeId(loan.getOfficeId()).withClientId(loan.getClientId())
                .withGroupId(loan.getGroupId()).withLoanId(loanId).with(changes).build();
    }

    private LoanRepaymentScheduleInstallment fetchRepaymentInstallmentByWrittenOfDate(final LocalDate writtenOffOnDate,
            final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments) {
        final LoanRepaymentScheduleInstallment lastRepaymentInstallment = repaymentScheduleInstallments.stream()
                .max(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber))
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.loan.special.write.off.installment.not.found",
                        "No repayment installment found for the special write off date", writtenOffOnDate));
        LoanRepaymentScheduleInstallment installment = null;
        for (LoanRepaymentScheduleInstallment repaymentScheduleInstallment : repaymentScheduleInstallments) {
            if (repaymentScheduleInstallment.isNotFullyPaidOff()
                    && !DateUtils.isAfter(writtenOffOnDate, repaymentScheduleInstallment.getDueDate())) {
                if (!DateUtils.isBefore(writtenOffOnDate, repaymentScheduleInstallment.getFromDate())
                        || !DateUtils.isAfter(writtenOffOnDate, repaymentScheduleInstallment.getDueDate())) {
                    installment = repaymentScheduleInstallment;
                    break;
                }
            }
        }
        if (installment == null && DateUtils.isAfter(writtenOffOnDate, lastRepaymentInstallment.getDueDate())) {
            installment = lastRepaymentInstallment;
        }
        if (installment == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.special.write.off.installment.not.found",
                    "No repayment installment found for the special write off date", writtenOffOnDate);
        }
        return installment;
    }

    @Transactional
    @Override
    public CommandProcessingResult closeLoan(final Long loanId, final JsonCommand command) {

        this.loanEventApiJsonValidator.validateTransactionWithNoAmount(command.json());

        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        checkClientOrGroupActive(loan);
        LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        if (loan.isChargedOff() && DateUtils.isBefore(transactionDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.transaction.date.cannot.be.earlier.than.charge.off.date", "Loan: "
                    + loanId
                    + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the loan",
                    loanId);
        }

        businessEventNotifierService.notifyPreBusinessEvent(new LoanCloseBusinessEvent(loan));

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
        changes.put("locale", command.locale());
        changes.put("dateFormat", command.dateFormat());

        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        updateLoanCounters(loan, loan.getDisbursementDate());

        LocalDate recalculateFrom = null;
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            recalculateFrom = command.localDateValueOfParameterNamed("transactionDate");
        }

        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
        ChangedTransactionDetail changedTransactionDetail = loan.close(command, defaultLoanLifecycleStateMachine, changes,
                existingTransactionIds, existingReversedTransactionIds, scheduleGeneratorDTO);
        final LoanTransaction possibleClosingTransaction = changedTransactionDetail.getNewTransactionMappings().remove(0L);
        if (possibleClosingTransaction != null) {
            this.loanTransactionRepository.saveAndFlush(possibleClosingTransaction);
        }
        for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
            this.loanTransactionRepository.save(mapEntry.getValue());
            this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
        }
        saveLoanWithDataIntegrityViolationChecks(loan);

        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
            final Note note = Note.loanNote(loan, noteText);
            this.noteRepository.save(note);
        }

        if (possibleClosingTransaction != null) {
            postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        }
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        loanAccountDomainService.recalculateAccruals(loan);

        loanAccountDomainService.setLoanDelinquencyTag(loan, DateUtils.getBusinessLocalDate());

        businessEventNotifierService.notifyPostBusinessEvent(new LoanCloseBusinessEvent(loan));

        // Update loan transaction on repayment.
        if (AccountType.fromInt(loan.getLoanType()).isIndividualAccount()) {
            Set<LoanCollateralManagement> loanCollateralManagements = loan.getLoanCollateralManagements();
            for (LoanCollateralManagement loanCollateralManagement : loanCollateralManagements) {
                ClientCollateralManagement clientCollateralManagement = loanCollateralManagement.getClientCollateralManagement();

                if (loan.getStatus().isClosed()) {
                    loanCollateralManagement.setIsReleased(true);
                    BigDecimal quantity = loanCollateralManagement.getQuantity();
                    clientCollateralManagement.updateQuantity(clientCollateralManagement.getQuantity().add(quantity));
                    loanCollateralManagement.setClientCollateralManagement(clientCollateralManagement);
                }
            }
            this.loanAccountDomainService.updateLoanCollateralTransaction(loanCollateralManagements);
        }

        // disable all active standing instructions linked to the loan
        this.loanAccountDomainService.disableStandingInstructionsLinkedToClosedLoan(loan);

        CommandProcessingResult result;
        if (possibleClosingTransaction != null) {

            result = new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(possibleClosingTransaction.getId()) //
                    .withEntityExternalId(possibleClosingTransaction.getExternalId()) //
                    .withOfficeId(loan.getOfficeId()) //
                    .withClientId(loan.getClientId()) //
                    .withGroupId(loan.getGroupId()) //
                    .withLoanId(loanId) //
                    .with(changes).build();
        } else {
            result = new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(loanId) //
                    .withEntityExternalId(loan.getExternalId()) //
                    .withOfficeId(loan.getOfficeId()) //
                    .withClientId(loan.getClientId()) //
                    .withGroupId(loan.getGroupId()) //
                    .withLoanId(loanId) //
                    .with(changes).build();
        }

        return result;
    }

    @Transactional
    @Override
    public CommandProcessingResult closeAsRescheduled(final Long loanId, final JsonCommand command) {

        this.loanEventApiJsonValidator.validateTransactionWithNoAmount(command.json());

        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        checkClientOrGroupActive(loan);
        if (loan.isChargedOff()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.charged.off",
                    "Loan: " + loanId + " Close as rescheduled is not allowed. Loan Account is Charged-off", loanId);
        }
        removeLoanCycle(loan);
        businessEventNotifierService.notifyPreBusinessEvent(new LoanCloseAsRescheduleBusinessEvent(loan));

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
        changes.put("locale", command.locale());
        changes.put("dateFormat", command.dateFormat());

        loan.closeAsMarkedForReschedule(command, defaultLoanLifecycleStateMachine, changes);

        saveLoanWithDataIntegrityViolationChecks(loan);

        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
            final Note note = Note.loanNote(loan, noteText);
            this.noteRepository.save(note);
        }
        businessEventNotifierService.notifyPostBusinessEvent(new LoanCloseAsRescheduleBusinessEvent(loan));

        // disable all active standing instructions linked to the loan
        this.loanAccountDomainService.disableStandingInstructionsLinkedToClosedLoan(loan);

        // Update loan transaction on repayment.
        if (AccountType.fromInt(loan.getLoanType()).isIndividualAccount()) {
            Set<LoanCollateralManagement> loanCollateralManagements = loan.getLoanCollateralManagements();
            for (LoanCollateralManagement loanCollateralManagement : loanCollateralManagements) {
                ClientCollateralManagement clientCollateralManagement = loanCollateralManagement.getClientCollateralManagement();

                if (loan.getStatus().isClosed()) {
                    loanCollateralManagement.setIsReleased(true);
                    BigDecimal quantity = loanCollateralManagement.getQuantity();
                    clientCollateralManagement.updateQuantity(clientCollateralManagement.getQuantity().add(quantity));
                    loanCollateralManagement.setClientCollateralManagement(clientCollateralManagement);
                }
            }
            this.loanAccountDomainService.updateLoanCollateralTransaction(loanCollateralManagements);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loanId) //
                .withEntityExternalId(loan.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    private void createRestructuringCancellationEvent(Loan loan) {
        createNoveltyNews(loan, DateUtils.getBusinessLocalDate(), InsuranceIncidentType.DEFINITIVE_RESTRUCTURING_CANCELLATION);
    }

    private void disburseLoanToLoan(final Loan loan, final JsonCommand command, final BigDecimal amount, final Loan loanToClose) {

        final LocalDate transactionDate = command.localDateValueOfParameterNamed("actualDisbursementDate");
        final ExternalId txnExternalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(locale);
        final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(transactionDate, amount, PortfolioAccountType.LOAN,
                PortfolioAccountType.LOAN, loan.getId(), loan.getTopupLoanDetails().getLoanIdToClose(), "Loan Topup", locale, fmt,
                LoanTransactionType.DISBURSEMENT.getValue(), LoanTransactionType.REPAYMENT.getValue(), txnExternalId, loan, null);
        AccountTransferDetails accountTransferDetails = this.accountTransfersWritePlatformService.repayLoanWithTopup(accountTransferDTO);
        loan.getTopupLoanDetails().setAccountTransferDetails(accountTransferDetails.getId());
        loan.getTopupLoanDetails().setTopupAmount(amount);
        if (loanToClose.claimType() == null || !loanToClose.claimType().equals("castigado")) {
            BlockingReasonSetting setting = loanBlockingReasonRepositoryWrapper.getSingleBlockingReasonSettingByReason(
                    BlockingReasonSettingEnum.CREDIT_RESTRUCTURE.getDatabaseString(), BlockLevel.CREDIT.toString());
            loanBlockWritePlatformService.blockLoan(loan.getId(), setting, "Reestructurada", DateUtils.getLocalDateOfTenant());
        }
    }

    private void disburseLoanToSavings(final Loan loan, final JsonCommand command, final Money amount, final PaymentDetail paymentDetail) {

        final LocalDate transactionDate = command.localDateValueOfParameterNamed("actualDisbursementDate");
        final ExternalId txnExternalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(locale);
        final PortfolioAccountData portfolioAccountData = this.accountAssociationsReadPlatformService
                .retriveLoanLinkedAssociation(loan.getId());
        if (portfolioAccountData == null) {
            final String errorMessage = "Disburse Loan with id:" + loan.getId() + " requires linked savings account for payment";
            throw new LinkedAccountRequiredException("loan.disburse.to.savings", errorMessage, loan.getId());
        }
        final SavingsAccount fromSavingsAccount = null;
        final boolean isExceptionForBalanceCheck = false;
        final boolean isRegularTransaction = true;
        final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(transactionDate, amount.getAmount(), PortfolioAccountType.LOAN,
                PortfolioAccountType.SAVINGS, loan.getId(), portfolioAccountData.getId(), "Loan Disbursement", locale, fmt, paymentDetail,
                LoanTransactionType.DISBURSEMENT.getValue(), null, null, null, AccountTransferType.ACCOUNT_TRANSFER.getValue(), null, null,
                txnExternalId, loan, null, fromSavingsAccount, isRegularTransaction, isExceptionForBalanceCheck);
        this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);

    }

    @Transactional
    @Override
    public LoanTransaction initiateLoanTransfer(final Loan loan, final LocalDate transferDate) {

        this.loanAssembler.setHelpers(loan);
        checkClientOrGroupActive(loan);
        validateTransactionsForTransfer(loan, transferDate);

        businessEventNotifierService.notifyPreBusinessEvent(new LoanInitiateTransferBusinessEvent(loan));

        final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
        final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());
        ExternalId externalId = externalIdFactory.create();
        final LoanTransaction newTransferTransaction = LoanTransaction.initiateTransfer(loan.getOffice(), loan, transferDate, externalId);
        loan.addLoanTransaction(newTransferTransaction);
        LoanLifecycleStateMachine loanLifecycleStateMachine = defaultLoanLifecycleStateMachine;
        loanLifecycleStateMachine.transition(LoanEvent.LOAN_INITIATE_TRANSFER, loan);

        this.loanTransactionRepository.saveAndFlush(newTransferTransaction);
        saveLoanWithDataIntegrityViolationChecks(loan);

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanInitiateTransferBusinessEvent(loan));
        return newTransferTransaction;
    }

    @Transactional
    @Override
    public LoanTransaction acceptLoanTransfer(final Loan loan, final LocalDate transferDate, final Office acceptedInOffice,
            final Staff loanOfficer) {
        this.loanAssembler.setHelpers(loan);
        businessEventNotifierService.notifyPreBusinessEvent(new LoanAcceptTransferBusinessEvent(loan));
        final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
        final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());
        ExternalId externalId = externalIdFactory.create();
        final LoanTransaction newTransferAcceptanceTransaction = LoanTransaction.approveTransfer(acceptedInOffice, loan, transferDate,
                externalId);
        loan.addLoanTransaction(newTransferAcceptanceTransaction);
        LoanLifecycleStateMachine loanLifecycleStateMachine = defaultLoanLifecycleStateMachine;
        if (loan.getTotalOverpaid() != null) {
            loanLifecycleStateMachine.transition(LoanEvent.LOAN_OVERPAYMENT, loan);
        } else {
            loanLifecycleStateMachine.transition(LoanEvent.LOAN_REPAYMENT_OR_WAIVER, loan);
        }
        if (loanOfficer != null) {
            loan.reassignLoanOfficer(loanOfficer, transferDate);
        }

        this.loanTransactionRepository.saveAndFlush(newTransferAcceptanceTransaction);
        saveLoanWithDataIntegrityViolationChecks(loan);

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanAcceptTransferBusinessEvent(loan));

        return newTransferAcceptanceTransaction;
    }

    @Transactional
    @Override
    public LoanTransaction withdrawLoanTransfer(final Loan loan, final LocalDate transferDate) {
        this.loanAssembler.setHelpers(loan);
        businessEventNotifierService.notifyPreBusinessEvent(new LoanWithdrawTransferBusinessEvent(loan));

        final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
        final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());

        ExternalId externalId = externalIdFactory.create();

        final LoanTransaction newTransferAcceptanceTransaction = LoanTransaction.withdrawTransfer(loan.getOffice(), loan, transferDate,
                externalId);
        loan.addLoanTransaction(newTransferAcceptanceTransaction);
        LoanLifecycleStateMachine loanLifecycleStateMachine = defaultLoanLifecycleStateMachine;
        loanLifecycleStateMachine.transition(LoanEvent.LOAN_WITHDRAW_TRANSFER, loan);

        this.loanTransactionRepository.saveAndFlush(newTransferAcceptanceTransaction);
        saveLoanWithDataIntegrityViolationChecks(loan);

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanWithdrawTransferBusinessEvent(loan));

        return newTransferAcceptanceTransaction;
    }

    @Transactional
    @Override
    public void rejectLoanTransfer(final Loan loan) {
        this.loanAssembler.setHelpers(loan);
        businessEventNotifierService.notifyPreBusinessEvent(new LoanRejectTransferBusinessEvent(loan));
        LoanLifecycleStateMachine loanLifecycleStateMachine = defaultLoanLifecycleStateMachine;
        loanLifecycleStateMachine.transition(LoanEvent.LOAN_REJECT_TRANSFER, loan);
        saveLoanWithDataIntegrityViolationChecks(loan);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanRejectTransferBusinessEvent(loan));
    }

    @Transactional
    @Override
    public CommandProcessingResult loanReassignment(final Long loanId, final JsonCommand command) {

        this.loanEventApiJsonValidator.validateUpdateOfLoanOfficer(command.json());

        final Long fromLoanOfficerId = command.longValueOfParameterNamed("fromLoanOfficerId");
        final Long toLoanOfficerId = command.longValueOfParameterNamed("toLoanOfficerId");

        final Staff fromLoanOfficer = this.loanAssembler.findLoanOfficerByIdIfProvided(fromLoanOfficerId);
        final Staff toLoanOfficer = this.loanAssembler.findLoanOfficerByIdIfProvided(toLoanOfficerId);
        final LocalDate dateOfLoanOfficerAssignment = command.localDateValueOfParameterNamed("assignmentDate");

        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        checkClientOrGroupActive(loan);
        businessEventNotifierService.notifyPreBusinessEvent(new LoanReassignOfficerBusinessEvent(loan));
        if (!loan.hasLoanOfficer(fromLoanOfficer)) {
            throw new LoanOfficerAssignmentException(loanId, fromLoanOfficerId);
        }

        loan.reassignLoanOfficer(toLoanOfficer, dateOfLoanOfficerAssignment);

        saveLoanWithDataIntegrityViolationChecks(loan);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanReassignOfficerBusinessEvent(loan));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loanId) //
                .withEntityExternalId(loan.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult bulkLoanReassignment(final JsonCommand command) {

        this.loanEventApiJsonValidator.validateForBulkLoanReassignment(command.json());

        final Long fromLoanOfficerId = command.longValueOfParameterNamed("fromLoanOfficerId");
        final Long toLoanOfficerId = command.longValueOfParameterNamed("toLoanOfficerId");
        final String[] loanIds = command.arrayValueOfParameterNamed("loans");

        final LocalDate dateOfLoanOfficerAssignment = command.localDateValueOfParameterNamed("assignmentDate");

        final Staff fromLoanOfficer = this.loanAssembler.findLoanOfficerByIdIfProvided(fromLoanOfficerId);
        final Staff toLoanOfficer = this.loanAssembler.findLoanOfficerByIdIfProvided(toLoanOfficerId);
        List<Long> lockedLoanIds = new ArrayList<>();

        for (final String loanIdString : loanIds) {
            final Long loanId = Long.valueOf(loanIdString);
            final Loan loan = this.loanAssembler.assembleFrom(loanId);
            if (loanAccountLockService.isLoanHardLocked(loanId)) {
                lockedLoanIds.add(loanId);
            } else {
                businessEventNotifierService.notifyPreBusinessEvent(new LoanReassignOfficerBusinessEvent(loan));
                checkClientOrGroupActive(loan);

                if (!loan.hasLoanOfficer(fromLoanOfficer)) {
                    throw new LoanOfficerAssignmentException(loanId, fromLoanOfficerId);
                }

                loan.reassignLoanOfficer(toLoanOfficer, dateOfLoanOfficerAssignment);
                saveLoanWithDataIntegrityViolationChecks(loan);
                businessEventNotifierService.notifyPostBusinessEvent(new LoanReassignOfficerBusinessEvent(loan));
            }
        }
        if (!lockedLoanIds.isEmpty()) {
            throw new LoanAccountLockCannotBeOverruledException("There are hard-lcoked loan accounts: " + lockedLoanIds);
        }
        this.loanRepositoryWrapper.flush();

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult removeLoanOfficer(final Long loanId, final JsonCommand command) {

        final LoanUpdateCommand loanUpdateCommand = this.loanUpdateCommandFromApiJsonDeserializer.commandFromApiJson(command.json());

        loanUpdateCommand.validate();

        final LocalDate dateOfLoanOfficerUnassigned = command.localDateValueOfParameterNamed("unassignedDate");

        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        checkClientOrGroupActive(loan);

        if (loan.getLoanOfficer() == null) {
            throw new LoanOfficerUnassignmentException(loanId);
        }
        businessEventNotifierService.notifyPreBusinessEvent(new LoanRemoveOfficerBusinessEvent(loan));

        loan.removeLoanOfficer(dateOfLoanOfficerUnassigned);

        saveLoanWithDataIntegrityViolationChecks(loan);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanRemoveOfficerBusinessEvent(loan));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loanId) //
                .withEntityExternalId(loan.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .build();
    }

    private void postJournalEntries(final Loan loan, final List<Long> existingTransactionIds,
            final List<Long> existingReversedTransactionIds) {

        final MonetaryCurrency currency = loan.getCurrency();
        boolean isAccountTransfer = false;
        List<Map<String, Object>> accountingBridgeData = new ArrayList<>();
        if (loan.isChargedOff()) {
            accountingBridgeData = loan.deriveAccountingBridgeDataForChargeOff(currency.getCode(), existingTransactionIds,
                    existingReversedTransactionIds, isAccountTransfer);
        } else {
            accountingBridgeData.add(loan.deriveAccountingBridgeData(currency.getCode(), existingTransactionIds,
                    existingReversedTransactionIds, isAccountTransfer));
        }
        for (Map<String, Object> accountingData : accountingBridgeData) {
            this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingData);
        }

    }

    @Transactional
    @Override
    public void applyMeetingDateChanges(final Calendar calendar, final Collection<CalendarInstance> loanCalendarInstances) {

        final Boolean rescheduleBasedOnMeetingDates = null;
        final LocalDate presentMeetingDate = null;
        final LocalDate newMeetingDate = null;

        applyMeetingDateChanges(calendar, loanCalendarInstances, rescheduleBasedOnMeetingDates, presentMeetingDate, newMeetingDate);

    }

    @Transactional
    @Override
    public void applyMeetingDateChanges(final Calendar calendar, final Collection<CalendarInstance> loanCalendarInstances,
            final Boolean rescheduleBasedOnMeetingDates, final LocalDate presentMeetingDate, final LocalDate newMeetingDate) {

        final boolean isHolidayEnabled = this.configurationDomainService.isRescheduleRepaymentsOnHolidaysEnabled();
        final WorkingDays workingDays = this.workingDaysRepository.findOne();
        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();
        final Collection<Integer> loanStatuses = new ArrayList<>(Arrays.asList(LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(),
                LoanStatus.APPROVED.getValue(), LoanStatus.ACTIVE.getValue()));
        final Collection<Integer> loanTypes = new ArrayList<>(Arrays.asList(AccountType.GROUP.getValue(), AccountType.JLG.getValue()));
        final Collection<Long> loanIds = new ArrayList<>(loanCalendarInstances.size());
        // loop through loanCalendarInstances to get loan ids
        for (final CalendarInstance calendarInstance : loanCalendarInstances) {
            loanIds.add(calendarInstance.getEntityId());
        }

        final List<Loan> loans = this.loanRepositoryWrapper.findByIdsAndLoanStatusAndLoanType(loanIds, loanStatuses, loanTypes);
        List<Holiday> holidays;
        final LocalDate recalculateFrom = null;
        // loop through each loan to reschedule the repayment dates
        for (final Loan loan : loans) {
            if (loan != null) {
                if (loan.getExpectedFirstRepaymentOnDate() != null && loan.getExpectedFirstRepaymentOnDate().equals(presentMeetingDate)) {
                    final String defaultUserMessage = "Meeting calendar date update is not supported since its a first repayment date";
                    throw new CalendarParameterUpdateNotSupportedException("meeting.for.first.repayment.date", defaultUserMessage,
                            loan.getExpectedFirstRepaymentOnDate(), presentMeetingDate);
                }

                if (loan.isChargedOff()) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.is.charged.off",
                            "Loan: " + loan.getId() + " reschedule is not allowed. Loan Account is Charged-off", loan.getId());
                }

                Boolean isSkipRepaymentOnFirstMonth = false;
                int numberOfDays = 0;
                boolean isSkipRepaymentOnFirstMonthEnabled = configurationDomainService.isSkippingMeetingOnFirstDayOfMonthEnabled();
                if (isSkipRepaymentOnFirstMonthEnabled) {
                    isSkipRepaymentOnFirstMonth = this.loanUtilService.isLoanRepaymentsSyncWithMeeting(loan.group(), calendar);
                    if (isSkipRepaymentOnFirstMonth) {
                        numberOfDays = configurationDomainService.retreivePeroidInNumberOfDaysForSkipMeetingDate().intValue();
                    }
                }

                holidays = this.holidayRepository.findByOfficeIdAndGreaterThanDate(loan.getOfficeId(), loan.getDisbursementDate());
                if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
                    ScheduleGeneratorDTO scheduleGeneratorDTO = loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
                    loan.setHelpers(null, this.loanSummaryWrapper, this.transactionProcessingStrategy);
                    loan.recalculateScheduleFromLastTransaction(scheduleGeneratorDTO, existingTransactionIds,
                            existingReversedTransactionIds);
                    createAndSaveLoanScheduleArchive(loan, scheduleGeneratorDTO);
                } else if (rescheduleBasedOnMeetingDates != null && rescheduleBasedOnMeetingDates) {
                    loan.updateLoanRepaymentScheduleDates(calendar.getRecurrence(), isHolidayEnabled, holidays, workingDays,
                            presentMeetingDate, newMeetingDate, isSkipRepaymentOnFirstMonth, numberOfDays);
                } else {
                    loan.updateLoanRepaymentScheduleDates(calendar.getStartDateLocalDate(), calendar.getRecurrence(), isHolidayEnabled,
                            holidays, workingDays, isSkipRepaymentOnFirstMonth, numberOfDays);
                }

                saveLoanWithDataIntegrityViolationChecks(loan);
                businessEventNotifierService.notifyPostBusinessEvent(new LoanRescheduledDueCalendarChangeBusinessEvent(loan));
                loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
            }
        }
    }

    private void removeLoanCycle(final Loan loan) {
        final List<Loan> loansToUpdate;
        if (loan.isGroupLoan()) {
            if (loan.loanProduct().isIncludeInBorrowerCycle()) {
                loansToUpdate = this.loanRepositoryWrapper.getGroupLoansToUpdateLoanCounter(loan.getCurrentLoanCounter(), loan.getGroupId(),
                        AccountType.GROUP.getValue());
            } else {
                loansToUpdate = this.loanRepositoryWrapper.getGroupLoansToUpdateLoanProductCounter(loan.getLoanProductLoanCounter(),
                        loan.getGroupId(), AccountType.GROUP.getValue());
            }

        } else {
            if (loan.loanProduct().isIncludeInBorrowerCycle()) {
                loansToUpdate = this.loanRepositoryWrapper.getClientOrJLGLoansToUpdateLoanCounter(loan.getCurrentLoanCounter(),
                        loan.getClientId());
            } else {
                loansToUpdate = this.loanRepositoryWrapper.getClientLoansToUpdateLoanProductCounter(loan.getLoanProductLoanCounter(),
                        loan.getClientId());
            }

        }
        if (loansToUpdate != null) {
            updateLoanCycleCounter(loansToUpdate, loan);
        }
        loan.updateClientLoanCounter(null);
        loan.updateLoanProductLoanCounter(null);

    }

    private void updateLoanCounters(final Loan loan, final LocalDate actualDisbursementDate) {

        if (loan.isGroupLoan()) {
            final List<Loan> loansToUpdateForLoanCounter = this.loanRepositoryWrapper.getGroupLoansDisbursedAfter(actualDisbursementDate,
                    loan.getGroupId(), AccountType.GROUP.getValue());
            final Integer newLoanCounter = getNewGroupLoanCounter(loan);
            final Integer newLoanProductCounter = getNewGroupLoanProductCounter(loan);
            updateLoanCounter(loan, loansToUpdateForLoanCounter, newLoanCounter, newLoanProductCounter);
        } else {
            final List<Loan> loansToUpdateForLoanCounter = this.loanRepositoryWrapper
                    .getClientOrJLGLoansDisbursedAfter(actualDisbursementDate, loan.getClientId());
            final Integer newLoanCounter = getNewClientOrJLGLoanCounter(loan);
            final Integer newLoanProductCounter = getNewClientOrJLGLoanProductCounter(loan);
            updateLoanCounter(loan, loansToUpdateForLoanCounter, newLoanCounter, newLoanProductCounter);
        }
    }

    private Integer getNewGroupLoanCounter(final Loan loan) {

        Integer maxClientLoanCounter = this.loanRepositoryWrapper.getMaxGroupLoanCounter(loan.getGroupId(), AccountType.GROUP.getValue());
        if (maxClientLoanCounter == null) {
            maxClientLoanCounter = 1;
        } else {
            maxClientLoanCounter = maxClientLoanCounter + 1;
        }
        return maxClientLoanCounter;
    }

    private Integer getNewGroupLoanProductCounter(final Loan loan) {

        Integer maxLoanProductLoanCounter = this.loanRepositoryWrapper.getMaxGroupLoanProductCounter(loan.loanProduct().getId(),
                loan.getGroupId(), AccountType.GROUP.getValue());
        if (maxLoanProductLoanCounter == null) {
            maxLoanProductLoanCounter = 1;
        } else {
            maxLoanProductLoanCounter = maxLoanProductLoanCounter + 1;
        }
        return maxLoanProductLoanCounter;
    }

    private void updateLoanCounter(final Loan loan, final List<Loan> loansToUpdateForLoanCounter, Integer newLoanCounter,
            Integer newLoanProductCounter) {

        final boolean includeInBorrowerCycle = loan.loanProduct().isIncludeInBorrowerCycle();
        for (final Loan loanToUpdate : loansToUpdateForLoanCounter) {
            // Update client loan counter if loan product includeInBorrowerCycle
            // is true
            if (loanToUpdate.loanProduct().isIncludeInBorrowerCycle()) {
                Integer currentLoanCounter = loanToUpdate.getCurrentLoanCounter() == null ? 1 : loanToUpdate.getCurrentLoanCounter();
                if (newLoanCounter > currentLoanCounter) {
                    newLoanCounter = currentLoanCounter;
                }
                loanToUpdate.updateClientLoanCounter(++currentLoanCounter);
            }

            if (Objects.equals(loan.loanProduct().getId(), loanToUpdate.loanProduct().getId())) {
                Integer loanProductLoanCounter = loanToUpdate.getLoanProductLoanCounter();
                if (newLoanProductCounter > loanProductLoanCounter) {
                    newLoanProductCounter = loanProductLoanCounter;
                }
                loanToUpdate.updateLoanProductLoanCounter(++loanProductLoanCounter);
            }
        }

        if (includeInBorrowerCycle) {
            loan.updateClientLoanCounter(newLoanCounter);
        } else {
            loan.updateClientLoanCounter(null);
        }
        loan.updateLoanProductLoanCounter(newLoanProductCounter);
        this.loanRepositoryWrapper.save(loansToUpdateForLoanCounter);
    }

    private Integer getNewClientOrJLGLoanCounter(final Loan loan) {

        Integer maxClientLoanCounter = this.loanRepositoryWrapper.getMaxClientOrJLGLoanCounter(loan.getClientId());
        if (maxClientLoanCounter == null) {
            maxClientLoanCounter = 1;
        } else {
            maxClientLoanCounter = maxClientLoanCounter + 1;
        }
        return maxClientLoanCounter;
    }

    private Integer getNewClientOrJLGLoanProductCounter(final Loan loan) {

        Integer maxLoanProductLoanCounter = this.loanRepositoryWrapper.getMaxClientOrJLGLoanProductCounter(loan.loanProduct().getId(),
                loan.getClientId());
        if (maxLoanProductLoanCounter == null) {
            maxLoanProductLoanCounter = 1;
        } else {
            maxLoanProductLoanCounter = maxLoanProductLoanCounter + 1;
        }
        return maxLoanProductLoanCounter;
    }

    private void updateLoanCycleCounter(final List<Loan> loansToUpdate, final Loan loan) {

        final Integer currentLoanCounter = loan.getCurrentLoanCounter();
        final Integer currentLoanProductCounter = loan.getLoanProductLoanCounter();

        for (final Loan loanToUpdate : loansToUpdate) {
            if (loan.loanProduct().isIncludeInBorrowerCycle()) {
                Integer runningLoanCounter = loanToUpdate.getCurrentLoanCounter();
                if (runningLoanCounter > currentLoanCounter) {
                    loanToUpdate.updateClientLoanCounter(--runningLoanCounter);
                }
            }
            if (Objects.equals(loan.loanProduct().getId(), loanToUpdate.loanProduct().getId())) {
                Integer runningLoanProductCounter = loanToUpdate.getLoanProductLoanCounter();
                if (runningLoanProductCounter > currentLoanProductCounter) {
                    loanToUpdate.updateLoanProductLoanCounter(--runningLoanProductCounter);
                }
            }
        }
        this.loanRepositoryWrapper.save(loansToUpdate);
    }

    private void checkClientOrGroupActive(final Loan loan) {
        final Client client = loan.client();
        if (client != null && client.isNotActive()) {
            throw new ClientNotActiveException(client.getId());
        }
        final Group group = loan.group();
        if (group != null && group.isNotActive()) {
            throw new GroupNotActiveException(group.getId());
        }
    }

    private void checkCupo(final Loan loan) {
        final Client client = loan.client();
        final MonetaryCurrency currency = loan.getCurrency();
        if (client != null) {
            final Long clientId = client.getId();
            final Money approvedPrincipal = Money.of(currency, loan.getApprovedPrincipal());
            final boolean isAdvanceLoanProduct = loan.getLoanProduct().isAdvance();
            final ClientAdditionalFieldsData loanAdditionalFieldsData = this.clientReadPlatformService
                    .retrieveClientAdditionalData(clientId);
            Money cupo;
            String sql = """
                        SELECT COALESCE(SUM(ml.principal_outstanding_derived), 0) AS totalOutstandingPrincipalAmount
                        FROM m_loan ml
                        INNER JOIN m_product_loan mpl ON mpl.id = ml.product_id
                        INNER JOIN m_code_value mcv ON mcv.id = mpl.product_type
                        WHERE ml.loan_status_id = 300 AND ml.client_id = ? AND mpl.is_advance = ?
                    """;
            Money advanceTotalOutstandingPrincipalAmount;
            Money purchaseTotalOutstandingPrincipalAmount;
            final LoanProduct loanProduct = loan.loanProduct();
            final CodeValue loanProductType = loanProduct.getProductType();
            if (loanProductType != null && LoanProductType.SUMAS_VEHICULOS.getCode().equals(loanProductType.getLabel())
                    && loanProduct.isUseOtherLoansCupo()) {
                sql = sql + " AND mcv.code_value = ? AND mpl.use_other_loans_cupo = true ";
                cupo = Money.of(currency, loanAdditionalFieldsData.getOtherLoansCupo());
                advanceTotalOutstandingPrincipalAmount = Money.of(currency,
                        this.jdbcTemplate.queryForObject(sql, BigDecimal.class, clientId, true, LoanProductType.SUMAS_VEHICULOS.getCode()));
                purchaseTotalOutstandingPrincipalAmount = Money.of(currency, this.jdbcTemplate.queryForObject(sql, BigDecimal.class,
                        clientId, false, LoanProductType.SUMAS_VEHICULOS.getCode()));
            } else {
                cupo = Money.of(currency, loanAdditionalFieldsData.getCupo());
                advanceTotalOutstandingPrincipalAmount = Money.of(currency,
                        this.jdbcTemplate.queryForObject(sql, BigDecimal.class, clientId, true));
                purchaseTotalOutstandingPrincipalAmount = Money.of(currency,
                        this.jdbcTemplate.queryForObject(sql, BigDecimal.class, clientId, false));
            }

            if (isAdvanceLoanProduct) {
                advanceTotalOutstandingPrincipalAmount = advanceTotalOutstandingPrincipalAmount.add(approvedPrincipal);
            } else {
                purchaseTotalOutstandingPrincipalAmount = purchaseTotalOutstandingPrincipalAmount.add(approvedPrincipal);
            }
            final Money totalOutstandingPrincipalAmount = advanceTotalOutstandingPrincipalAmount
                    .add(purchaseTotalOutstandingPrincipalAmount);
            final AdvanceQuotaConfigurationData advanceQuotaConfigurationData = this.loanProductReadPlatformService
                    .retrieveAdvanceQuotaConfigurationData();
            final Money advanceQuotaPercentage = Money.of(currency, advanceQuotaConfigurationData.getPercentageValue());
            final boolean isAdvanceQuotaEnabled = advanceQuotaConfigurationData.getEnabled();
            if (isAdvanceQuotaEnabled && isAdvanceLoanProduct && !loanProduct.isUseOtherLoansCupo()) {
                final Money maximumAdvanceQuota = cupo.multipliedBy(advanceQuotaPercentage.getAmount()).dividedBy(BigDecimal.valueOf(100L),
                        MoneyHelper.getRoundingMode());
                if (approvedPrincipal.isGreaterThan(maximumAdvanceQuota)) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.maximum.advance.cupo.limit.exceeded",
                            String.format("Límite de cupo adelantado excedido. Límite Total: %s y tu enviaste: %s", maximumAdvanceQuota,
                                    approvedPrincipal),
                            maximumAdvanceQuota.toString());
                }
                if (advanceTotalOutstandingPrincipalAmount.isGreaterThan(maximumAdvanceQuota)) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.maximum.advance.cupo.limit.exceeded", String.format(
                            "Límite de cupo adelantado excedido. Límite Total: %s y Total del monto principal pendiente de adelanto: %s",
                            maximumAdvanceQuota, advanceTotalOutstandingPrincipalAmount), maximumAdvanceQuota.toString());

                }
                if (purchaseTotalOutstandingPrincipalAmount.isGreaterThan(cupo)) {
                    // Calculate available limit
                    final Money availablePurchaseQuota = cupo.minus(purchaseTotalOutstandingPrincipalAmount);
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.maximum.purchase.cupo.limit.exceeded", String.format(
                            "Límite de cupo de compra excedido. Límite disponible: %s y Total del monto principal pendiente de compra: %s",
                            availablePurchaseQuota, purchaseTotalOutstandingPrincipalAmount), availablePurchaseQuota.toString());
                }
            }
            if (totalOutstandingPrincipalAmount.isGreaterThan(cupo)) {
                // Calculate available limit
                final Money availableQuota = cupo.minus(totalOutstandingPrincipalAmount);
                throw new GeneralPlatformDomainRuleException("error.msg.loan.maximum.cupo.limit.exceeded",
                        String.format("Límite de cupo total excedido. Límite disponible: %s y Total del monto principal pendiente: %s",
                                availableQuota, totalOutstandingPrincipalAmount),
                        availableQuota.toString());
            }

        }
    }

    private Long countRecentTopups(Long clientId, LocalDate businessDate) {
        String sql = "SELECT COUNT(ml.disbursedon_date) " + "FROM m_loan ml " + "INNER JOIN m_loan_topup mlt ON mlt.loan_id = ml.id "
                + "WHERE ml.client_id = ? " + "AND ml.disbursedon_date BETWEEN to_date(?, 'YYYY-MM-DD') - INTERVAL '6' MONTH "
                + "AND to_date(?, 'YYYY-MM-DD')";

        return jdbcTemplate.queryForObject(sql, Long.class, clientId, businessDate.toString(), businessDate.toString());
    }

    @Override
    public CommandProcessingResult undoWriteOff(Long loanId) {

        Loan loan = this.loanAssembler.assembleFrom(loanId);
        checkClientOrGroupActive(loan);
        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();
        if (!loan.isClosedWrittenOff()) {
            throw new PlatformServiceUnavailableException("error.msg.loan.status.not.written.off.update.not.allowed",
                    "Loan :" + loanId + " update not allowed as loan status is not written off", loanId);
        }
        LocalDate recalculateFrom = null;
        LoanTransaction writeOffTransaction = loan.findWriteOffTransaction();
        if (writeOffTransaction == null) {
            throw new PlatformServiceUnavailableException("error.msg.loan.write.off.transaction.not.found",
                    "Loan :" + loanId + " write off transaction not found", loanId);
        }
        businessEventNotifierService.notifyPreBusinessEvent(new LoanUndoWrittenOffBusinessEvent(writeOffTransaction));

        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

        ChangedTransactionDetail changedTransactionDetail = loan.undoWrittenOff(defaultLoanLifecycleStateMachine, existingTransactionIds,
                existingReversedTransactionIds, scheduleGeneratorDTO);
        if (changedTransactionDetail != null) {
            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                loanAccountDomainService.saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
            }
            // Trigger transaction replayed event
            replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
        }
        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        this.loanAccountDomainService.recalculateAccruals(loan);
        if (writeOffTransaction != null) {
            businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
            businessEventNotifierService.notifyPostBusinessEvent(new LoanUndoWrittenOffBusinessEvent(writeOffTransaction));
        }
        this.loanAccountDomainService.setLoanDelinquencyTag(loan, DateUtils.getBusinessLocalDate());
        return new CommandProcessingResultBuilder() //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .withEntityId(writeOffTransaction.getId()) //
                .withEntityExternalId(writeOffTransaction.getExternalId()) //
                .build();
    }

    private void validateMultiDisbursementData(final JsonCommand command, LocalDate expectedDisbursementDate,
            boolean isDisallowExpectedDisbursements) {
        final String json = command.json();
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
        final JsonArray disbursementDataArray = command.arrayOfParameterNamed(LoanApiConstants.disbursementDataParameterName);

        if (isDisallowExpectedDisbursements) {
            if (!disbursementDataArray.isEmpty()) {
                final String errorMessage = "For this loan product, disbursement details are not allowed";
                throw new MultiDisbursementDataNotAllowedException(LoanApiConstants.disbursementDataParameterName, errorMessage);
            }
        } else {
            if (disbursementDataArray == null || disbursementDataArray.size() == 0) {
                final String errorMessage = "For this loan product, disbursement details must be provided";
                throw new MultiDisbursementDataRequiredException(LoanApiConstants.disbursementDataParameterName, errorMessage);
            }
        }

        final BigDecimal principal = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("approvedLoanAmount", element);

        loanApplicationCommandFromApiJsonHelper.validateLoanMultiDisbursementDate(element, baseDataValidator, expectedDisbursementDate,
                principal);
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    private void validateForAddAndDeleteTranche(final Loan loan) {

        BigDecimal totalDisbursedAmount = BigDecimal.ZERO;
        Collection<LoanDisbursementDetails> loanDisburseDetails = loan.getDisbursementDetails();
        for (LoanDisbursementDetails disbursementDetails : loanDisburseDetails) {
            if (disbursementDetails.actualDisbursementDate() != null) {
                totalDisbursedAmount = totalDisbursedAmount.add(disbursementDetails.principal());
            }
        }
        if (totalDisbursedAmount.compareTo(loan.getApprovedPrincipal()) == 0) {
            final String errorMessage = "loan.disbursement.cannot.be.a.edited";
            throw new LoanMultiDisbursementException(errorMessage);
        }
    }

    @Override
    @Transactional
    public CommandProcessingResult addAndDeleteLoanDisburseDetails(Long loanId, JsonCommand command) {

        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        checkClientOrGroupActive(loan);
        if (loan.isChargedOff()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.charged.off",
                    "Update Loan: " + loanId + " disbursement details is not allowed. Loan Account is Charged-off", loanId);
        }
        final Map<String, Object> actualChanges = new LinkedHashMap<>();
        LocalDate expectedDisbursementDate = loan.getExpectedDisbursedOnLocalDate();
        if (!loan.loanProduct().isMultiDisburseLoan()) {
            final String errorMessage = "loan.product.does.not.support.multiple.disbursals";
            throw new LoanMultiDisbursementException(errorMessage);
        }
        if (loan.isSubmittedAndPendingApproval() || loan.isClosed() || loan.isClosedWrittenOff()
                || loan.getStatus().isClosedObligationsMet() || loan.getStatus().isOverpaid()) {
            final String errorMessage = "cannot.modify.tranches.if.loan.is.pendingapproval.closed.overpaid.writtenoff";
            throw new LoanMultiDisbursementException(errorMessage);
        }
        validateMultiDisbursementData(command, expectedDisbursementDate, loan.loanProduct().isDisallowExpectedDisbursements());

        this.validateForAddAndDeleteTranche(loan);

        loan.updateDisbursementDetails(command, actualChanges);

        if (loan.loanProduct().isDisallowExpectedDisbursements()) {
            if (!loan.getDisbursementDetails().isEmpty()) {
                final String errorMessage = "For this loan product, disbursement details are not allowed";
                throw new MultiDisbursementDataNotAllowedException(LoanApiConstants.disbursementDataParameterName, errorMessage);
            }
        } else {
            if (loan.getDisbursementDetails().isEmpty()) {
                final String errorMessage = "For this loan product, disbursement details must be provided";
                throw new MultiDisbursementDataRequiredException(LoanApiConstants.disbursementDataParameterName, errorMessage);
            }
        }

        if (loan.getDisbursementDetails().size() > loan.loanProduct().maxTrancheCount()) {
            final String errorMessage = "Number of tranche shouldn't be greater than " + loan.loanProduct().maxTrancheCount();
            throw new ExceedingTrancheCountException(LoanApiConstants.disbursementDataParameterName, errorMessage,
                    loan.loanProduct().maxTrancheCount(), loan.getDisbursementDetails().size());
        }
        LoanDisbursementDetails updateDetails = null;
        CommandProcessingResult result = processLoanDisbursementDetail(loan, loanId, command, updateDetails);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanUpdateDisbursementDataBusinessEvent(loan));
        return result;

    }

    private CommandProcessingResult processLoanDisbursementDetail(Loan loan, Long loanId, JsonCommand command,
            LoanDisbursementDetails loanDisbursementDetails) {
        final List<Long> existingTransactionIds = loan.findExistingTransactionIds();
        final List<Long> existingReversedTransactionIds = loan.findExistingReversedTransactionIds();
        final Map<String, Object> changes = new LinkedHashMap<>();
        LocalDate recalculateFrom = null;
        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

        ChangedTransactionDetail changedTransactionDetail = null;

        if (command.entityId() != null) {
            changedTransactionDetail = loan.updateDisbursementDateAndAmountForTranche(loanDisbursementDetails, command, changes,
                    scheduleGeneratorDTO);
        } else {
            loan.repaymentScheduleDetail().setPrincipal(loan.getPrincipalAmountForRepaymentSchedule());

            if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
                loan.regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
            } else {
                loan.regenerateRepaymentSchedule(scheduleGeneratorDTO);
                loan.processPostDisbursementTransactions();
            }
        }

        if (command.entityId() != null && changedTransactionDetail != null) {
            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                loanAccountDomainService.saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
            }
            // Trigger transaction replayed event
            replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
        }
        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            createLoanScheduleArchive(loan, scheduleGeneratorDTO);
        }
        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        this.loanAccountDomainService.recalculateAccruals(loan);
        this.loanAccountDomainService.setLoanDelinquencyTag(loan, DateUtils.getBusinessLocalDate());
        return new CommandProcessingResultBuilder() //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes).build();
    }

    @Override
    @Transactional
    public CommandProcessingResult updateDisbursementDateAndAmountForTranche(final Long loanId, final Long disbursementId,
            final JsonCommand command) {

        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        checkClientOrGroupActive(loan);
        if (loan.isChargedOff()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.charged.off",
                    "Update Loan: " + loanId + " disbursement details is not allowed. Loan Account is Charged-off", loanId);
        }
        LoanDisbursementDetails loanDisbursementDetails = loan.fetchLoanDisbursementsById(disbursementId);
        this.loanEventApiJsonValidator.validateUpdateDisbursementDateAndAmount(command.json(), loanDisbursementDetails);

        CommandProcessingResult result = processLoanDisbursementDetail(loan, loanId, command, loanDisbursementDetails);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanUpdateDisbursementDataBusinessEvent(loan));
        return result;

    }

    @Transactional
    @Override
    @Retry(name = "recalculateInterest", fallbackMethod = "fallbackRecalculateInterest")
    public void recalculateInterest(final long loanId) {
        Loan loan = this.loanAssembler.assembleFrom(loanId);
        LocalDate recalculateFrom = loan.fetchInterestRecalculateFromDate();
        businessEventNotifierService.notifyPreBusinessEvent(new LoanInterestRecalculationBusinessEvent(loan));
        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        ScheduleGeneratorDTO generatorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

        ChangedTransactionDetail changedTransactionDetail = loan.recalculateScheduleFromLastTransaction(generatorDTO,
                existingTransactionIds, existingReversedTransactionIds);

        if (changedTransactionDetail != null) {
            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                loanAccountDomainService.saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
            }
            // Trigger transaction replayed event
            replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
        }
        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        loanAccountDomainService.recalculateAccruals(loan);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanInterestRecalculationBusinessEvent(loan));
    }

    @Override
    public CommandProcessingResult recoverFromGuarantor(final Long loanId) {
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        this.guarantorDomainService.transferFundsFromGuarantor(loan);
        return new CommandProcessingResultBuilder().withLoanId(loanId).withEntityId(loanId).withEntityExternalId(loan.getExternalId())
                .build();
    }

    @SuppressWarnings("unused")
    public void fallbackRecalculateInterest(Throwable t) {
        // NOTE: allow caller to catch the exceptions
        // NOTE: wrap throwable only if really necessary
        throw errorHandler.getMappable(t, null, null, "loan.recalculateinterest");
    }

    @Override
    public void updateOriginalSchedule(Loan loan) {
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            final LocalDate recalculateFrom = null;
            ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
            createLoanScheduleArchive(loan, scheduleGeneratorDTO);
        }
    }

    private void createLoanScheduleArchive(final Loan loan, final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        createAndSaveLoanScheduleArchive(loan, scheduleGeneratorDTO);

    }

    private void regenerateScheduleOnDisbursement(final JsonCommand command, final Loan loan, final boolean recalculateSchedule,
            final ScheduleGeneratorDTO scheduleGeneratorDTO, final LocalDate nextPossibleRepaymentDate,
            final LocalDate rescheduledRepaymentDate) {
        final LocalDate actualDisbursementDate = command.localDateValueOfParameterNamed("actualDisbursementDate");
        BigDecimal emiAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.emiAmountParameterName);
        loan.regenerateScheduleOnDisbursement(scheduleGeneratorDTO, recalculateSchedule, actualDisbursementDate, emiAmount,
                nextPossibleRepaymentDate, rescheduledRepaymentDate);
    }

    private List<LoanRepaymentScheduleInstallment> retrieveRepaymentScheduleFromModel(LoanScheduleModel model) {
        final List<LoanRepaymentScheduleInstallment> installments = new ArrayList<>();
        for (final LoanScheduleModelPeriod scheduledLoanInstallment : model.getPeriods()) {
            if (scheduledLoanInstallment.isRepaymentPeriod() || scheduledLoanInstallment.isDownPaymentPeriod()) {
                final LoanRepaymentScheduleInstallment installment = new LoanRepaymentScheduleInstallment(null,
                        scheduledLoanInstallment.periodNumber(), scheduledLoanInstallment.periodFromDate(),
                        scheduledLoanInstallment.periodDueDate(), scheduledLoanInstallment.principalDue(),
                        scheduledLoanInstallment.interestDue(), scheduledLoanInstallment.feeChargesDue(),
                        scheduledLoanInstallment.penaltyChargesDue(), scheduledLoanInstallment.isRecalculatedInterestComponent(),
                        scheduledLoanInstallment.getLoanCompoundingDetails());
                installments.add(installment);
            }
        }
        return installments;
    }

    @Override
    public CommandProcessingResult creditBalanceRefund(Long loanId, JsonCommand command) {
        this.loanEventApiJsonValidator.validateNewRefundTransaction(command.json());

        Loan loan = this.loanAssembler.assembleFrom(loanId);
        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
        final String noteText = command.stringValueOfParameterNamedAllowingNull("note");
        final ExternalId externalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
        changes.put("transactionAmount", command.stringValueOfParameterNamed("transactionAmount"));
        changes.put("locale", command.locale());
        changes.put("dateFormat", command.dateFormat());

        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
        }
        if (!externalId.isEmpty()) {
            changes.put(LoanApiConstants.externalIdParameterName, externalId);
        }
        changes.put("paymentTypeId", command.longValueOfParameterNamed(LoanApiConstants.PAYMENT_TYPE_PARAMNAME));

        PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createPaymentDetail(command, changes);
        if (paymentDetail != null) {
            paymentDetail = this.paymentDetailWritePlatformService.persistPaymentDetail(paymentDetail);
        }

        final LoanTransaction loanTransaction = this.loanAccountDomainService.creditBalanceRefund(loan, transactionDate, transactionAmount,
                noteText, externalId, paymentDetail);
        loan.getLoanCustomizationDetail().recordActivity();
        loanAccountDomainService.saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        return new CommandProcessingResultBuilder() //
                .withEntityId(loanTransaction.getId()) //
                .withEntityExternalId(loanTransaction.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withCommandId(command.commandId()) //
                .with(changes) //
                .build();

    }

    @Override
    @Transactional
    public CommandProcessingResult markLoanAsFraud(Long loanId, JsonCommand command) {
        this.loanEventApiJsonValidator.validateMarkAsFraudLoan(command.json());

        Loan loan = this.loanAssembler.assembleFrom(loanId);
        final Map<String, Object> changes = new LinkedHashMap<>();

        if (loan.isApproved() || loan.isSubmittedAndPendingApproval()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.mark.as.fraud.not.allowed",
                    "Loan Id: " + loanId + " mark as fraud is not allowed as loan status is not active", loan.getStatus().getCode());
        }
        final boolean fraud = command.booleanPrimitiveValueOfParameterNamed(LoanApiConstants.FRAUD_ATTRIBUTE_NAME);
        if (loan.isFraud() != fraud) {
            loan.markAsFraud(fraud);
            loan.getLoanCustomizationDetail().recordActivity();
            this.loanRepository.save(loan);
            changes.put(LoanApiConstants.FRAUD_ATTRIBUTE_NAME, fraud);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withEntityExternalId(loan.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    @Override
    @Transactional
    public CommandProcessingResult makeLoanRefund(Long loanId, JsonCommand command) {

        this.loanEventApiJsonValidator.validateNewRefundTransaction(command.json());

        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        ExternalId externalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        // checkRefundDateIsAfterAtLeastOneRepayment(loanId, transactionDate);

        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
        checkIfLoanIsPaidInAdvance(loanId, transactionAmount);

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
        changes.put("transactionAmount", command.stringValueOfParameterNamed("transactionAmount"));
        changes.put("locale", command.locale());
        changes.put("dateFormat", command.dateFormat());
        changes.put(LoanApiConstants.externalIdParameterName, externalId);

        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
        }

        final PaymentDetail paymentDetail = null;

        final CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder();

        LoanTransaction loanTransaction = this.loanAccountDomainService.makeRefundForActiveLoan(loanId, commandProcessingResultBuilder,
                transactionDate, transactionAmount, paymentDetail, noteText, externalId);

        return commandProcessingResultBuilder //
                .withCommandId(command.commandId()) //
                .withLoanId(loanId) //
                .withEntityId(loanTransaction.getId()) //
                .withEntityExternalId(loanTransaction.getExternalId()) //
                .with(changes) //
                .build();

    }

    private void checkIfLoanIsPaidInAdvance(final Long loanId, final BigDecimal transactionAmount) {
        BigDecimal overpaid = this.loanReadPlatformService.retrieveTotalPaidInAdvance(loanId).getPaidInAdvance();

        if (overpaid == null || overpaid.compareTo(BigDecimal.ZERO) == 0 || transactionAmount.floatValue() > overpaid.floatValue()) {
            if (overpaid == null) {
                overpaid = BigDecimal.ZERO;
            }
            throw new InvalidPaidInAdvanceAmountException(overpaid.toPlainString());
        }
    }

    private AppUser getAppUserIfPresent() {
        AppUser user = null;
        if (this.context != null) {
            user = this.context.getAuthenticatedUserIfPresent();
        }
        return user;
    }

    @Override
    @Transactional
    public CommandProcessingResult undoLastLoanDisbursal(Long loanId, JsonCommand command) {

        Loan loan = this.loanAssembler.assembleFrom(loanId);
        final LocalDate recalculateFromDate = loan.getLastRepaymentDate();
        validateIsMultiDisbursalLoanAndDisbursedMoreThanOneTranche(loan);
        checkClientOrGroupActive(loan);
        if (loan.isChargedOff()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.charged.off",
                    "Undo Loan: " + loanId + " last disbursement is not allowed. Loan Account is Charged-off", loanId);
        }
        businessEventNotifierService.notifyPreBusinessEvent(new LoanUndoLastDisbursalBusinessEvent(loan));

        final MonetaryCurrency currency = loan.getCurrency();
        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFromDate);

        final Map<String, Object> changes = loan.undoLastDisbursal(scheduleGeneratorDTO, existingTransactionIds,
                existingReversedTransactionIds, loan);
        if (!changes.isEmpty()) {
            loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
            String noteText;
            if (command.hasParameter("note")) {
                noteText = command.stringValueOfParameterNamed("note");
                if (StringUtils.isNotBlank(noteText)) {
                    final Note note = Note.loanNote(loan, noteText);
                    this.noteRepository.save(note);
                }
            }
            boolean isAccountTransfer = false;
            final Map<String, Object> accountingBridgeData = loan.deriveAccountingBridgeData(currency.getCode(), existingTransactionIds,
                    existingReversedTransactionIds, isAccountTransfer);
            journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData);
            loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
            businessEventNotifierService.notifyPostBusinessEvent(new LoanUndoLastDisbursalBusinessEvent(loan));
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withEntityExternalId(loan.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    @Override
    @Transactional
    public CommandProcessingResult forecloseLoan(final Long loanId, final JsonCommand command) {
        final String json = command.json();
        final JsonElement element = fromApiJsonHelper.parse(json);
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        final LocalDate transactionDate = this.fromApiJsonHelper.extractLocalDateNamed(LoanApiConstants.transactionDateParamName, element);
        final ExternalId externalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);
        this.loanEventApiJsonValidator.validateLoanForeclosure(command.json());
        final Map<String, Object> changes = new LinkedHashMap<>();
        // Got changed to match with the rest of the APIs
        changes.put("dateFormat", command.dateFormat());
        changes.put("transactionDate", command.stringValueOfParameterNamed(LoanApiConstants.transactionDateParamName));
        changes.put("externalId", externalId);

        String noteText = this.fromApiJsonHelper.extractStringNamed(LoanApiConstants.noteParamName, element);
        LoanRescheduleRequest loanRescheduleRequest = null;
        for (LoanDisbursementDetails loanDisbursementDetails : loan.getDisbursementDetails()) {
            if (!DateUtils.isAfter(loanDisbursementDetails.expectedDisbursementDateAsLocalDate(), transactionDate)
                    && loanDisbursementDetails.actualDisbursementDate() == null) {
                final String defaultUserMessage = "The loan with undisbursed tranche before foreclosure cannot be foreclosed.";
                throw new LoanForeclosureException("loan.with.undisbursed.tranche.before.foreclosure.cannot.be.foreclosured",
                        defaultUserMessage, transactionDate);
            }
        }

        this.loanScheduleHistoryWritePlatformService.createAndSaveLoanScheduleArchive(loan.getRepaymentScheduleInstallments(), loan,
                loanRescheduleRequest);

        createCancellationNoveltyNews(loan, transactionDate);

        LoanTransaction foreclosureTransaction = this.loanAccountDomainService.foreCloseLoan(loan, transactionDate, noteText, externalId,
                changes);

        final BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepositoryWrapper.getSingleBlockingReasonSettingByReason(
                BlockingReasonSettingEnum.CREDIT_CANCELADO.getDatabaseString(), BlockLevel.CREDIT.toString());
        blockingReasonSetting.setAffectsClientLevel(0);
        loanBlockWritePlatformService.blockLoan(loan.getId(), blockingReasonSetting, "CANCELADO", DateUtils.getLocalDateOfTenant());

        final CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder();
        return commandProcessingResultBuilder //
                .withLoanId(loanId) //
                .withEntityId(foreclosureTransaction.getId()) //
                .withEntityExternalId(foreclosureTransaction.getExternalId()) //
                .with(changes) //
                .build();
    }

    @Override
    @Transactional
    public CommandProcessingResult cancelLoan(final Long loanId, final JsonCommand command) {
        final String json = command.json();
        final JsonElement element = fromApiJsonHelper.parse(json);
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        final LocalDate transactionDate = this.fromApiJsonHelper.extractLocalDateNamed(LoanApiConstants.transactionDateParamName, element);
        final ExternalId externalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);
        this.loanEventApiJsonValidator.validateLoanForeclosure(command.json());
        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("dateFormat", command.dateFormat());
        changes.put("transactionDate", command.stringValueOfParameterNamed(LoanApiConstants.transactionDateParamName));
        changes.put("externalId", externalId);
        String noteText = this.fromApiJsonHelper.extractStringNamed(LoanApiConstants.noteParamName, element);
        for (LoanDisbursementDetails loanDisbursementDetails : loan.getDisbursementDetails()) {
            if (!DateUtils.isAfter(loanDisbursementDetails.expectedDisbursementDateAsLocalDate(), transactionDate)
                    && loanDisbursementDetails.actualDisbursementDate() == null) {
                final String defaultUserMessage = "The loan with undisbursed tranche before foreclosure cannot be foreclosed.";
                throw new LoanForeclosureException("loan.with.undisbursed.tranche.before.foreclosure.cannot.be.foreclosured",
                        defaultUserMessage, transactionDate);
            }
        }
        this.loanScheduleHistoryWritePlatformService.createAndSaveLoanScheduleArchive(loan.getRepaymentScheduleInstallments(), loan, null);
        /*
         * List<DefaultOrCancelInsuranceInstallmentData> cancelInsuranceInstallmentIds = this.loanReadPlatformService
         * .getLoanDataWithDefaultOrCancelInsurance(loanId, null, transactionDate); InsuranceIncident incident =
         * this.insuranceIncidentRepository .findByIncidentType(InsuranceIncidentType.DEFINITIVE_FINAL_CANCELLATION); if
         * (incident == null) { throw new
         * InsuranceIncidentNotFoundException(InsuranceIncidentType.DEFINITIVE_FINAL_CANCELLATION.name()); } for (final
         * DefaultOrCancelInsuranceInstallmentData data : cancelInsuranceInstallmentIds) { LoanCharge loanCharge = null;
         * Optional<LoanCharge> loanChargeOptional = loan.getLoanCharges().stream() .filter(lc ->
         * Objects.equals(lc.getId(), data.loanChargeId())).findFirst(); if (loanChargeOptional.isPresent()) {
         * loanCharge = loanChargeOptional.get(); } BigDecimal cumulative = BigDecimal.ZERO; cumulative =
         * processInsuranceChargeCancellation(cumulative, loan, loanCharge, data, true); InsuranceIncidentNoveltyNews
         * insuranceIncidentNoveltyNews = InsuranceIncidentNoveltyNews.instance(loan, loanCharge, data.installment(),
         * incident, transactionDate, cumulative);
         * this.insuranceIncidentNoveltyNewsRepository.saveAndFlush(insuranceIncidentNoveltyNews); }
         */
        // Generate novelty "Anulación"
        createAnulacionNoveltyNews(loan, transactionDate);
        if (transactionDate.equals(loan.getDisbursementDate())) {
            loan.setAnulado(true);
            loan.setAnuladoOnDisbursementDate(true);
        }
        final LoanTransaction foreclosureTransaction = this.loanAccountDomainService.foreCloseLoan(loan, transactionDate, noteText,
                externalId, changes);
        final BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepositoryWrapper.getSingleBlockingReasonSettingByReason(
                BlockingReasonSettingEnum.CREDIT_ANULADO.getDatabaseString(), BlockLevel.CREDIT.toString());
        blockingReasonSetting.setAffectsClientLevel(0);
        loanBlockWritePlatformService.blockLoan(loan.getId(), blockingReasonSetting, "ANULADO", DateUtils.getLocalDateOfTenant());

        final CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder();
        return commandProcessingResultBuilder.withLoanId(loanId).withEntityId(foreclosureTransaction.getId())
                .withEntityExternalId(foreclosureTransaction.getExternalId()).with(changes).build();
    }

    @Override
    @Transactional
    public CommandProcessingResult chargeOff(JsonCommand command) {

        loanEventApiJsonValidator.validateChargeOffTransaction(command.json());

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put(LoanApiConstants.transactionDateParamName,
                command.stringValueOfParameterNamed(LoanApiConstants.transactionDateParamName));
        changes.put(LoanApiConstants.localeParameterName, command.locale());
        changes.put(LoanApiConstants.dateFormatParameterName, command.dateFormat());
        final LocalDate transactionDate = command.localDateValueOfParameterNamed(LoanApiConstants.transactionDateParamName);
        final ExternalId txnExternalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);
        final AppUser currentUser = getAppUserIfPresent();

        Loan loan = loanAssembler.assembleFrom(command.getLoanId());
        final Long loanId = loan.getId();
        if (!loan.isOpen()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.not.active",
                    "Loan: " + loanId + " Charge-off is not allowed. Loan Account is not Active", loanId);
        }
        if (loan.isChargedOff()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.already.charged.off",
                    "Loan: " + loanId + " is already charged-off", loanId);
        }
        if (DateUtils.isBefore(transactionDate, loan.getLastUserTransactionDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.charge.off.is.before.than.the.last.user.transaction",
                    "Loan: " + loanId + " charge-off cannot be executed. User transaction was found after the charge-off transaction date!",
                    loanId);
        }
        if (DateUtils.isDateInTheFuture(transactionDate)) {
            final String errorMessage = "The transaction date cannot be in the future.";
            throw new GeneralPlatformDomainRuleException("error.msg.loan.transaction.cannot.be.a.future.date", errorMessage,
                    transactionDate);
        }

        checkIfProductAllowsCancelationOrReversal(loan);

        loan.markAsChargedOff(transactionDate, currentUser, null);

        InsuranceIncidentType incidentType = InsuranceIncidentType.DEATH_CANCELLATION;
        if (command.hasParameter("incidentTypeId")) {
            Integer incidentTypeId = command.integerValueOfParameterNamed("incidentTypeId");
            incidentType = InsuranceIncidentType.fromInt(incidentTypeId);
        }

        this.loanScheduleHistoryWritePlatformService.createAndSaveLoanScheduleArchive(loan.getRepaymentScheduleInstallments(), loan, null);
        List<DefaultOrCancelInsuranceInstallmentData> cancelInsuranceInstallmentIds = this.loanReadPlatformService
                .getLoanDataWithDefaultOrCancelInsurance(loanId, null, transactionDate);
        InsuranceIncident incident = this.insuranceIncidentRepository.findByIncidentType(incidentType);
        if (incident == null) {
            throw new InsuranceIncidentNotFoundException(InsuranceIncidentType.DEATH_CANCELLATION.name());
        }
        for (final DefaultOrCancelInsuranceInstallmentData data : cancelInsuranceInstallmentIds) {
            LoanCharge loanCharge = null;
            Optional<LoanCharge> loanChargeOptional = loan.getLoanCharges().stream()
                    .filter(lc -> Objects.equals(lc.getId(), data.loanChargeId())).findFirst();
            if (loanChargeOptional.isPresent()) {
                loanCharge = loanChargeOptional.get();
                if ((incident.isMandatory() && loanCharge.isMandatoryInsurance())
                        || (incident.isVoluntary() && loanCharge.isVoluntaryInsurance())) {
                    BigDecimal cumulative = BigDecimal.ZERO;
                    cumulative = processInsuranceChargeCancellation(cumulative, loan, loanCharge, data, true);
                    InsuranceIncidentNoveltyNews insuranceIncidentNoveltyNews = InsuranceIncidentNoveltyNews.instance(loan, loanCharge,
                            data.installment(), incident, transactionDate, cumulative);
                    this.insuranceIncidentNoveltyNewsRepository.saveAndFlush(insuranceIncidentNoveltyNews);
                }
            }
        }

        businessEventNotifierService.notifyPreBusinessEvent(new LoanChargeOffPreBusinessEvent(loan));

        final List<Long> existingTransactionIds = loan.findExistingTransactionIds();
        final List<Long> existingReversedTransactionIds = loan.findExistingReversedTransactionIds();
        loan.getLoanCustomizationDetail().recordActivity();

        LoanTransaction chargeOffTransaction = LoanTransaction.chargeOff(loan, transactionDate, txnExternalId);
        loanTransactionRepository.saveAndFlush(chargeOffTransaction);
        loan.addLoanTransaction(chargeOffTransaction);
        saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        String noteText = command.stringValueOfParameterNamed(LoanApiConstants.noteParameterName);
        if (StringUtils.isNotBlank(noteText)) {
            changes.put(LoanApiConstants.noteParameterName, noteText);
            final Note note = Note.loanTransactionNote(loan, chargeOffTransaction, noteText);
            this.noteRepository.save(note);
        }

        this.loanAccountDomainService.foreCloseLoan(loan, transactionDate, noteText, txnExternalId, changes);
        final BlockingReasonSetting blockingReasonSetting = loanBlockingReasonRepositoryWrapper.getSingleBlockingReasonSettingByReason(
                BlockingReasonSettingEnum.CREDIT_CANCELADO.getDatabaseString(), BlockLevel.CREDIT.toString());
        loanBlockWritePlatformService.blockLoan(loan.getId(), blockingReasonSetting, "CANCELADO", DateUtils.getLocalDateOfTenant());

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanChargeOffPostBusinessEvent(chargeOffTransaction));
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(chargeOffTransaction.getId()) //
                .withEntityExternalId(chargeOffTransaction.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(command.getLoanId()) //
                .with(changes).build();
    }

    @Override
    @Transactional
    public CommandProcessingResult undoChargeOff(JsonCommand command) {
        this.loanEventApiJsonValidator.validateUndoChargeOff(command.json());
        final Long loanId = command.getLoanId();
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        final List<Long> existingTransactionIds = loan.findExistingTransactionIds();
        final List<Long> existingReversedTransactionIds = loan.findExistingReversedTransactionIds();
        checkClientOrGroupActive(loan);
        if (!loan.isOpen()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.not.active",
                    "Loan: " + loanId + " Undo Charge-off is not allowed. Loan Account is not Active", loanId);
        }
        if (!loan.isChargedOff()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.not.charged.off", "Loan: " + loanId + " is not charged-off",
                    loanId);
        }
        LoanTransaction chargedOffTransaction = loan.findChargedOffTransaction();
        if (chargedOffTransaction == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.charge.off.transaction.not.found",
                    "Loan: " + loanId + " charge-off transaction was not found", loanId);
        }
        if (!chargedOffTransaction.equals(loan.getLastUserTransaction())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.charge.off.is.not.the.last.user.transaction",
                    "Loan: " + loanId + " charge-off cannot be undone. User transaction was found after charge-off!", loanId);
        }
        businessEventNotifierService.notifyPreBusinessEvent(new LoanUndoChargeOffBusinessEvent(chargedOffTransaction));

        // check if reversalExternalId is provided
        final String reversalExternalId = command.stringValueOfParameterNamedAllowingNull(LoanApiConstants.REVERSAL_EXTERNAL_ID_PARAMNAME);
        final ExternalId reversalTxnExternalId = ExternalIdFactory.produce(reversalExternalId);

        chargedOffTransaction.reverse(reversalTxnExternalId);
        chargedOffTransaction.manuallyAdjustedOrReversed();

        loan.liftChargeOff();
        loan.getLoanCustomizationDetail().recordActivity();
        loanTransactionRepository.saveAndFlush(chargedOffTransaction);
        saveLoanWithDataIntegrityViolationChecks(loan);
        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        businessEventNotifierService.notifyPostBusinessEvent(new LoanUndoChargeOffBusinessEvent(chargedOffTransaction));
        return new CommandProcessingResultBuilder() //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .withEntityId(chargedOffTransaction.getId()) //
                .withEntityExternalId(chargedOffTransaction.getExternalId()) //
                .build();
    }

    private void validateIsMultiDisbursalLoanAndDisbursedMoreThanOneTranche(Loan loan) {
        if (!loan.isMultiDisburmentLoan()) {
            final String errorMessage = "loan.product.does.not.support.multiple.disbursals.cannot.undo.last.disbursal";
            throw new LoanMultiDisbursementException(errorMessage);
        }
        Integer trancheDisbursedCount = 0;
        for (LoanDisbursementDetails disbursementDetails : loan.getDisbursementDetails()) {
            if (disbursementDetails.actualDisbursementDate() != null) {
                trancheDisbursedCount++;
            }
        }
        if (trancheDisbursedCount <= 1) {
            final String errorMessage = "tranches.should.be.disbursed.more.than.one.to.undo.last.disbursal";
            throw new LoanMultiDisbursementException(errorMessage);
        }
    }

    private void syncExpectedDateWithActualDisbursementDate(final Loan loan, LocalDate actualDisbursementDate) {
        if (!loan.getExpectedDisbursedOnLocalDate().equals(actualDisbursementDate)) {
            throw new DateMismatchException(actualDisbursementDate, loan.getExpectedDisbursedOnLocalDate());
        }
    }

    private void validateTransactionsForTransfer(final Loan loan, final LocalDate transferDate) {
        for (LoanTransaction transaction : loan.getLoanTransactions()) {
            if ((DateUtils.isEqual(transferDate, transaction.getTransactionDate())
                    && DateUtils.isEqual(transferDate, transaction.getSubmittedOnDate()))
                    || DateUtils.isBefore(transferDate, transaction.getTransactionDate())) {
                throw new GeneralPlatformDomainRuleException(TransferApiConstants.transferClientLoanException,
                        TransferApiConstants.transferClientLoanExceptionMessage, transaction.getCreatedDateTime().toLocalDate(),
                        transferDate);
            }
        }
    }

    @Override
    public void recalculateInterestForMaximumLegalRate(List<LoanRescheduleData> loanLoanRescheduleDataList,
            MaximumCreditRateConfigurationData maximumCreditRateConfigurationData) throws JobExecutionException {
        log.info("Recalculate Loan Interest After Maximum Legal Rate Change:: Recalculating interest for maximum legal rate for {} loans",
                loanLoanRescheduleDataList.size());
        final List<Throwable> exceptions = new ArrayList<>();
        final LocalDate appliedOnDate = maximumCreditRateConfigurationData.getAppliedOnDate();
        final BigDecimal maximumLegalAnnualNominalRateValue = maximumCreditRateConfigurationData.getAnnualNominalRate();
        log.info(
                "Recalculate Loan Interest After Maximum Legal Rate Change:: Applied on date: {}, Maximum legal annual nominal rate value: {}",
                appliedOnDate, maximumLegalAnnualNominalRateValue);
        if (CollectionUtils.isNotEmpty(loanLoanRescheduleDataList)) {
            final String locale = "es";
            final String dateFormat = "dd MMMM yyyy";
            final String submittedOnDate = DateUtils.format(DateUtils.getBusinessLocalDate(), dateFormat, Locale.forLanguageTag(locale));
            LoanRescheduleRequestData loanRescheduleReasons = this.loanRescheduleRequestReadPlatformService
                    .retrieveAllRescheduleReasons(RescheduleLoansApiConstants.LOAN_RESCHEDULE_REASON, null);
            Long rescheduleReasonId = null;
            for (CodeValueData codeValueData : loanRescheduleReasons.getRescheduleReasons()) {
                if (codeValueData.getName()
                        .equalsIgnoreCase(LoanRescheduleRequestWritePlatformServiceImpl.MAX_LEGAL_RATE_REASON_FOR_RESCHEDULE)) {
                    rescheduleReasonId = codeValueData.getId();
                    log.info("Recalculate Loan Interest After Maximum Legal Rate Change:: Reschedule reason ID: {}", rescheduleReasonId);
                    break;
                }
            }
            final JsonObject rescheduleJsonObject = new JsonObject();
            rescheduleJsonObject.addProperty("dateFormat", dateFormat);
            rescheduleJsonObject.addProperty("locale", locale);
            rescheduleJsonObject.addProperty("rescheduleReasonId", rescheduleReasonId);
            rescheduleJsonObject.addProperty("submittedOnDate", submittedOnDate);
            rescheduleJsonObject.addProperty("adjustedDueDate", "");
            rescheduleJsonObject.addProperty("graceOnPrincipal", "");
            rescheduleJsonObject.addProperty("extraTerms", "");
            log.info("Recalculate Loan Interest After Maximum Legal Rate Change:: Reschedule JSON Object: {}", rescheduleJsonObject);

            for (final LoanRescheduleData loanRescheduleData : loanLoanRescheduleDataList) {
                log.info(
                        "Recalculate Loan Interest After Maximum Legal Rate Change:: Started processing loan reschedule data for loan ID: {}",
                        loanRescheduleData.getId());
                final Long loanId = loanRescheduleData.getId();
                final Loan loan = this.loanRepository.findById(loanId).orElseThrow(() -> new LoanNotFoundException(loanId));
                log.info("Recalculate Loan Interest After Maximum Legal Rate Change:: Processing loan ID: {} of product type: {}", loanId,
                        loan.getLoanProduct().getName());
                final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = loan
                        .getInstallmentByScheduleFromDate(appliedOnDate);
                if (loanRepaymentScheduleInstallment == null) {
                    log.warn("Recalculate Loan Interest After Maximum Legal Rate Change:: No installment found for loan ID: {} on date: {}",
                            loanId, appliedOnDate);
                    continue;
                }
                final BigDecimal rescheduledAnnualRate = ObjectUtils.defaultIfNull(loanRescheduleData.getRescheduledAnnualRate(),
                        BigDecimal.ZERO);
                BigDecimal newInterestRate;

                if (maximumLegalAnnualNominalRateValue.compareTo(loanRescheduleData.getAnnualNominalRate()) > 0
                        && rescheduledAnnualRate.compareTo(loanRescheduleData.getAnnualNominalRate()) != 0) {
                    newInterestRate = loanRescheduleData.getAnnualNominalRate();
                } else if (maximumLegalAnnualNominalRateValue.compareTo(loanRescheduleData.getAnnualNominalRate()) < 0
                        && rescheduledAnnualRate.compareTo(maximumLegalAnnualNominalRateValue) != 0) {
                    newInterestRate = maximumLegalAnnualNominalRateValue;
                } else {
                    log.info(
                            "Recalculate Loan Interest After Maximum Legal Rate Change:: No reschedule needed for loan ID: {} as the interest rate is already at maximum legal rate or below",
                            loanId);
                    continue;
                }
                rescheduleJsonObject.addProperty("newInterestRate", newInterestRate);
                final String rescheduleFromDateString = DateUtils.format(appliedOnDate, dateFormat, Locale.forLanguageTag(locale));
                rescheduleJsonObject.addProperty("rescheduleFromDate", rescheduleFromDateString);
                rescheduleJsonObject.addProperty("loanId", loanId);
                final String rescheduleReasonComment = String.format(
                        LoanRescheduleRequestWritePlatformServiceImpl.MAX_LEGAL_RATE_REASON_FOR_RESCHEDULE
                                + ": [Nueva tasa de interés: %s, Tasa máxima legal: %s, Fecha de reprogramación: %s]",
                        newInterestRate, maximumLegalAnnualNominalRateValue, rescheduleFromDateString);
                rescheduleJsonObject.addProperty("rescheduleReasonComment", rescheduleReasonComment);
                final String rescheduleRequestBodyAsJson = rescheduleJsonObject.toString();
                CommandWrapper commandWrapper = new CommandWrapperBuilder()
                        .createLoanRescheduleRequest(RescheduleLoansApiConstants.ENTITY_NAME).withJson(rescheduleRequestBodyAsJson).build();
                try {
                    log.info("Recalculate Loan Interest After Maximum Legal Rate Change:: Create Loan Reschedule Request with Loan ID: {}",
                            loanId);
                    final long startTime = System.currentTimeMillis();
                    CommandProcessingResult commandProcessingResult = commandsSourceWritePlatformService.logCommandSource(commandWrapper);
                    final long createRescheduleRequestEndTime = System.currentTimeMillis();
                    log.info(
                            "Recalculate Loan Interest After Maximum Legal Rate Change:: Create Loan Reschedule Request took {} seconds for Loan ID: {}",
                            (createRescheduleRequestEndTime - startTime) / 1000.0, loanId);
                    if (commandProcessingResult.getResourceId() != null) {
                        final Long loanRescheduleId = commandProcessingResult.getResourceId();
                        final JsonObject approvalJsonObject = new JsonObject();
                        final Boolean isJobTriggered = true;
                        approvalJsonObject.addProperty("dateFormat", dateFormat);
                        approvalJsonObject.addProperty("locale", locale);
                        approvalJsonObject.addProperty("isJobTriggered", isJobTriggered);
                        approvalJsonObject.addProperty("approvedOnDate", submittedOnDate);
                        final String approvalRequestBodyAsJson = approvalJsonObject.toString();
                        commandWrapper = new CommandWrapperBuilder()
                                .approveLoanRescheduleRequest(RescheduleLoansApiConstants.ENTITY_NAME, loanRescheduleId)
                                .withJson(approvalRequestBodyAsJson).build();
                        log.info("Recalculate Loan Interest After Maximum Legal Rate Change:: Approve Loan Rescheduling with Loan ID: {}",
                                loanId);
                        commandProcessingResult = commandsSourceWritePlatformService.logCommandSource(commandWrapper);
                        final long approveRescheduleRequestEndTime = System.currentTimeMillis();
                        log.info(
                                "Recalculate Loan Interest After Maximum Legal Rate Change:: Approve Loan Rescheduling took {} seconds for Loan ID: {}",
                                (approveRescheduleRequestEndTime - createRescheduleRequestEndTime) / 1000.0, loanId);
                        if (commandProcessingResult.getResourceId() != null) {
                            final String successMessage = "Reprogramar la cuenta de préstamo: " + loanId
                                    + " con la tasa de interés al máximo legal";
                            log.info("Recalculate Loan Interest After Maximum Legal Rate Change:: " + successMessage);
                        }
                    }
                } catch (final PlatformApiDataValidationException e) {
                    final List<ApiParameterError> errors = e.getErrors();
                    for (final ApiParameterError error : errors) {
                        log.error(
                                "Recalculate Loan Interest After Maximum Legal Rate Change:: Reprogramar la cuenta de préstamo {} falló con el mensaje: {}",
                                loanId, error.getDeveloperMessage(), e);
                    }
                    exceptions.add(e);
                } catch (final AbstractPlatformDomainRuleException e) {
                    log.error(
                            "Recalculate Loan Interest After Maximum Legal Rate Change:: Reprogramar la cuenta de préstamo: {} falló con el mensaje: {}",
                            loanId, e.getDefaultUserMessage(), e);
                    exceptions.add(e);
                } catch (Exception e) {
                    log.error(
                            "Recalculate Loan Interest After Maximum Legal Rate Change:: Reprogramar la cuenta de préstamo: {} falló con el mensaje: {}",
                            loanId, e.getMessage(), e);
                    exceptions.add(e);
                }
            }
        }
        if (!exceptions.isEmpty()) {
            throw new JobExecutionException(exceptions);
        }
    }

    public void updateLoanScheduleAfterCustomChargeApplied(Loan loan) {
        loan.setHelpers(defaultLoanLifecycleStateMachine, this.loanSummaryWrapper, this.transactionProcessingStrategy);
        for (LoanCharge loanCharge : loan.getCharges()) {
            if (loanCharge.getChargeCalculation().isFlatHono()) {
                loanCharge.updateCustomFeeCharge();
            }

        }
        loan.updateLoanScheduleAfterCustomChargeApplied();
        saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
    }

    private ChannelData validateUndoRepaymentChannel(final String channelName, final LoanProduct loanProduct, Long transactionId,
            Long loanId) {
        final LoanTransaction loanTransaction = this.loanTransactionRepository.findByIdAndLoanId(transactionId, loanId)
                .orElseThrow(() -> new LoanTransactionNotFoundException(transactionId, loanId));
        final PaymentDetail paymentDetail = loanTransaction.getPaymentDetail();
        if (StringUtils.isBlank(channelName)) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.is.blank", "Channel is blank");
        }
        final ChannelData channelData = this.channelReadWritePlatformService.findByNameType(channelName, ChannelType.REPAYMENT.getValue());
        if (channelData == null) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.found", "Channel not found", channelName);
        }
        if (!channelData.getActive()) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.active", "Channel is not active", channelName);
        }

        final List<Channel> repaymentChannels = loanProduct.getRepaymentChannels();
        if (CollectionUtils.isNotEmpty(repaymentChannels)) {
            final Long channelId = channelData.getId();
            if (paymentDetail != null) {
                if (!Objects.equals(paymentDetail.getChannelId(), channelId)
                        && !channelName.equalsIgnoreCase(ChannelApiConstants.defaultChannel)) {
                    throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.allowed", "Channel is not allowed",
                            channelName);
                }
            }
            if (repaymentChannels.stream().noneMatch(repaymentChannel -> repaymentChannel.getId().equals(channelId))) {
                if (!channelName.equalsIgnoreCase(ChannelApiConstants.defaultChannel)) {

                    throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.allowed", "Channel is not allowed",
                            channelName);
                }
            }
        } else {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.allowed", "Channel is not allowed", channelName);
        }
        return channelData;
    }

    private final class DisbursementEventListener implements BusinessEventListener<LoanDisbursalBusinessEvent> {

        @SuppressWarnings("unused")
        @Override
        public void onBusinessEvent(LoanDisbursalBusinessEvent event) {
            final Loan loan = event.get();
            if (loan.getLoanProduct() == null) {
                recalculateInterestRate(loan);
            }
        }
    }

    private final class LoanInvoiceGenerationPostBusinessEventListener
            implements BusinessEventListener<LoanInvoiceGenerationPostBusinessEvent> {

        @Override
        public void onBusinessEvent(final LoanInvoiceGenerationPostBusinessEvent event) {
            final LoanTransaction loanTransaction = event.get();
            if (loanTransaction != null && loanTransaction.getTypeOf().isRepaymentType()) {
                generateLoanTransactionDocument(loanTransaction);
            }
        }
    }

    private final class LoanCreditNoteGenerationPostBusinessEventListener implements BusinessEventListener<LoanCreditNoteBusinessEvent> {

        @Override
        public void onBusinessEvent(final LoanCreditNoteBusinessEvent event) {
            final LoanTransaction loanTransaction = event.get();
            if (loanTransaction != null) {
                addCreditNoteVatPortions(loanTransaction);
            }
        }
    }

    private void addCreditNoteVatPortions(final LoanTransaction creditNoteTransaction) {
        if (creditNoteTransaction != null && creditNoteTransaction.isCreditNote()) {
            final Long loanTransactionId = creditNoteTransaction.getId();
            final List<LoanDocumentData> loanDocumentDataList = this.loanReadPlatformService
                    .retrieveLoanInvoiceDataListByTransactionId(loanTransactionId);
            if (!loanDocumentDataList.isEmpty()) {
                final LoanDocumentData creditNoteTransactionData = loanDocumentDataList.get(0);
                final LoanCreditNote loanCreditNote = loanCreditNoteRepository.findByTransactionId(loanTransactionId)
                        .orElseThrow(() -> new CreditReportNotFoundException(loanTransactionId));
                loanCreditNote.addPortionsFromLoanTransaction(creditNoteTransactionData);
                loanCreditNoteRepository.saveAndFlush(loanCreditNote);
            }
        }
    }

    private void generateLoanTransactionDocument(final LoanTransaction loanTransaction) {
        final Long loanTransactionId = loanTransaction.getId();
        final LocalDate transactionDate = loanTransaction.getTransactionDate();
        final YearMonth yearMonth = YearMonth.from(transactionDate);
        final LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();
        final LocalDate firstDayOfMonth = transactionDate.withDayOfMonth(1);
        final LocalDate secondLastDayOfMonth = lastDayOfMonth.minusDays(1);
        final LoanTransactionType loanTransactionType = loanTransaction.getTypeOf();
        final List<LoanDocumentData> loanDocumentDataList = this.loanReadPlatformService
                .retrieveLoanInvoiceDataListByTransactionId(loanTransactionId);
        if (!loanDocumentDataList.isEmpty()) {
            final LoanDocumentData loanDocumentData = loanDocumentDataList.get(0);
            loanDocumentData.setFirstDayOfMonth(firstDayOfMonth);
            loanDocumentData.setSecondLastDayOfMonth(secondLastDayOfMonth);
            loanDocumentData.setLastDayOfMonth(lastDayOfMonth);
            loanDocumentData.setLoanTransactionId(loanTransactionId);
            if (!loanTransactionType.isCreditNote()) {
                final Loan loan = loanTransaction.getLoan();
                BigDecimal interestPaidRemaining = loanDocumentData.getInterestPaid();
                BigDecimal mandatoryInsurancePaidRemaining = loanDocumentData.getMandatoryInsurancePaid();
                BigDecimal mandatoryInsuranceVatPaidRemaining = loanDocumentData.getMandatoryInsuranceVatPaid();
                BigDecimal voluntaryInsurancePaidRemaining = loanDocumentData.getVoluntaryInsurancePaid();
                BigDecimal voluntaryInsuranceVatPaidRemaining = loanDocumentData.getVoluntaryInsuranceVatPaid();
                BigDecimal honorariosPaidRemaining = loanDocumentData.getHonorariosPaid();
                BigDecimal honorariosVatPaidRemaining = loanDocumentData.getHonorariosVatPaid();
                BigDecimal penaltyChargesPaidRemaining = loanDocumentData.getPenaltyChargesPaid();
                BigDecimal penaltyChargesVatPaidRemaining = loanDocumentData.getPenaltyChargesVatPaid();
                final Set<LoanTransactionData> invoicedByAccrualTransactionDataSet = new HashSet<>();
                final Set<LoanTransaction> invoicedByAccrualTransactionSet = new HashSet<>();
                final List<LoanTransaction> accrualTransactions = loan.retrieveListOfAccrualTransactions().stream()
                        .filter(ltx -> Objects.isNull(ltx.getInvoicedByTransactionId()) || ltx.isPartiallyInvoiced()).toList();
                for (final LoanTransaction accrualTransaction : accrualTransactions) {
                    final boolean occurredOnSuspendedAccount = accrualTransaction.hasOccurredOnSuspendedAccount();
                    final boolean isPartiallyInvoicedTransaction = accrualTransaction.isPartiallyInvoiced();
                    final LoanTransactionData loanTransactionData = loanReadPlatformService.retrieveLoanTransaction(loan.getId(),
                            accrualTransaction.getId());
                    loanTransactionData.setOccurredOnSuspendedAccount(occurredOnSuspendedAccount);
                    loanTransactionData.setPartiallyInvoiced(isPartiallyInvoicedTransaction);
                    final LoanChargePaidByData loanChargePaidByData = loanTransactionData.getLoanChargePaidBySummary();
                    final BigDecimal penaltyPortion = loanChargePaidByData.getPenaltyPortion();
                    final BigDecimal penaltyVatPortion = loanChargePaidByData.getPenaltyVatPortion();
                    final BigDecimal honorariosPortion = loanChargePaidByData.getHonorariosPortion();
                    final BigDecimal honorariosVatPortion = loanChargePaidByData.getHonorariosVatPortion();
                    final BigDecimal mandatoryInsurancePortion = loanChargePaidByData.getMandatoryInsurancePortion();
                    final BigDecimal mandatoryInsuranceVatPortion = loanChargePaidByData.getMandatoryInsuranceVatPortion();
                    final BigDecimal voluntaryInsurancePortion = loanChargePaidByData.getVoluntaryInsurancePortion();
                    final BigDecimal voluntaryInsuranceVatPortion = loanChargePaidByData.getVoluntaryInsuranceVatPortion();
                    final BigDecimal interestPortion = loanTransactionData.getInterestPortion();

                    final PartialInvoicedTransaction partialInvoicedTransaction = new PartialInvoicedTransaction();
                    partialInvoicedTransaction.setRepaymentTransaction(loanTransaction);
                    partialInvoicedTransaction.setAccrualTransaction(accrualTransaction);

                    if (!accrualTransaction.isPartiallyInvoiced()) {
                        if (interestPaidRemaining.compareTo(BigDecimal.ZERO) > 0 && interestPortion.compareTo(BigDecimal.ZERO) > 0) {
                            if (interestPaidRemaining.compareTo(interestPortion) >= 0) {
                                interestPaidRemaining = interestPaidRemaining.subtract(interestPortion);
                            } else {
                                accrualTransaction.markAsPartiallyInvoiced();
                                loanTransactionData.markAsPartiallyInvoiced();
                                partialInvoicedTransaction.setInterest(interestPaidRemaining);
                                accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                                interestPaidRemaining = BigDecimal.ZERO;
                            }
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (penaltyChargesPaidRemaining.compareTo(BigDecimal.ZERO) > 0 && penaltyPortion.compareTo(BigDecimal.ZERO) > 0) {
                            if (penaltyChargesPaidRemaining.compareTo(penaltyPortion) >= 0) {
                                penaltyChargesPaidRemaining = penaltyChargesPaidRemaining.subtract(penaltyPortion);
                            } else {
                                accrualTransaction.markAsPartiallyInvoiced();
                                loanTransactionData.markAsPartiallyInvoiced();
                                partialInvoicedTransaction.setPenalty(penaltyChargesPaidRemaining);
                                accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                                penaltyChargesPaidRemaining = BigDecimal.ZERO;
                            }
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (penaltyChargesVatPaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && penaltyVatPortion.compareTo(BigDecimal.ZERO) > 0) {
                            if (penaltyChargesVatPaidRemaining.compareTo(penaltyVatPortion) >= 0) {
                                penaltyChargesVatPaidRemaining = penaltyChargesVatPaidRemaining.subtract(penaltyVatPortion);
                            } else {
                                accrualTransaction.markAsPartiallyInvoiced();
                                loanTransactionData.markAsPartiallyInvoiced();
                                partialInvoicedTransaction.setPenaltyVat(penaltyChargesVatPaidRemaining);
                                accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                                penaltyChargesVatPaidRemaining = BigDecimal.ZERO;
                            }
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (mandatoryInsurancePaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && mandatoryInsurancePortion.compareTo(BigDecimal.ZERO) > 0) {
                            if (mandatoryInsurancePaidRemaining.compareTo(mandatoryInsurancePortion) >= 0) {
                                mandatoryInsurancePaidRemaining = mandatoryInsurancePaidRemaining.subtract(mandatoryInsurancePortion);
                            } else {
                                accrualTransaction.markAsPartiallyInvoiced();
                                loanTransactionData.markAsPartiallyInvoiced();
                                partialInvoicedTransaction.setMandatoryInsurance(mandatoryInsurancePaidRemaining);
                                accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                                mandatoryInsurancePaidRemaining = BigDecimal.ZERO;
                            }
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (mandatoryInsuranceVatPaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && mandatoryInsuranceVatPortion.compareTo(BigDecimal.ZERO) > 0) {
                            if (mandatoryInsuranceVatPaidRemaining.compareTo(mandatoryInsuranceVatPortion) >= 0) {
                                mandatoryInsuranceVatPaidRemaining = mandatoryInsuranceVatPaidRemaining
                                        .subtract(mandatoryInsuranceVatPortion);
                            } else {
                                accrualTransaction.markAsPartiallyInvoiced();
                                loanTransactionData.markAsPartiallyInvoiced();
                                partialInvoicedTransaction.setMandatoryInsuranceVat(mandatoryInsuranceVatPaidRemaining);
                                accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                                mandatoryInsuranceVatPaidRemaining = BigDecimal.ZERO;
                            }
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (voluntaryInsurancePaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && voluntaryInsurancePortion.compareTo(BigDecimal.ZERO) > 0) {
                            if (voluntaryInsurancePaidRemaining.compareTo(voluntaryInsurancePortion) >= 0) {
                                voluntaryInsurancePaidRemaining = voluntaryInsurancePaidRemaining.subtract(voluntaryInsurancePortion);
                            } else {
                                accrualTransaction.markAsPartiallyInvoiced();
                                loanTransactionData.markAsPartiallyInvoiced();
                                partialInvoicedTransaction.setVoluntaryInsurance(voluntaryInsurancePaidRemaining);
                                accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                                voluntaryInsurancePaidRemaining = BigDecimal.ZERO;
                            }
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (voluntaryInsuranceVatPaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && voluntaryInsuranceVatPortion.compareTo(BigDecimal.ZERO) > 0) {
                            if (voluntaryInsuranceVatPaidRemaining.compareTo(voluntaryInsuranceVatPortion) >= 0) {
                                voluntaryInsuranceVatPaidRemaining = voluntaryInsuranceVatPaidRemaining
                                        .subtract(voluntaryInsuranceVatPortion);
                            } else {
                                accrualTransaction.markAsPartiallyInvoiced();
                                loanTransactionData.markAsPartiallyInvoiced();
                                partialInvoicedTransaction.setVoluntaryInsuranceVat(voluntaryInsuranceVatPaidRemaining);
                                accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                                voluntaryInsuranceVatPaidRemaining = BigDecimal.ZERO;
                            }
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (honorariosPaidRemaining.compareTo(BigDecimal.ZERO) > 0 && honorariosPortion.compareTo(BigDecimal.ZERO) > 0) {
                            if (honorariosPaidRemaining.compareTo(honorariosPortion) >= 0) {
                                honorariosPaidRemaining = honorariosPaidRemaining.subtract(honorariosPortion);
                            } else {
                                accrualTransaction.markAsPartiallyInvoiced();
                                loanTransactionData.markAsPartiallyInvoiced();
                                partialInvoicedTransaction.setHonorarios(honorariosPaidRemaining);
                                accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                                honorariosPaidRemaining = BigDecimal.ZERO;
                            }
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (honorariosVatPaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && honorariosVatPortion.compareTo(BigDecimal.ZERO) > 0) {
                            if (honorariosVatPaidRemaining.compareTo(honorariosVatPortion) >= 0) {
                                honorariosVatPaidRemaining = honorariosVatPaidRemaining.subtract(honorariosVatPortion);
                            } else {
                                accrualTransaction.markAsPartiallyInvoiced();
                                loanTransactionData.markAsPartiallyInvoiced();
                                partialInvoicedTransaction.setHonorariosVat(honorariosVatPaidRemaining);
                                accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                                honorariosVatPaidRemaining = BigDecimal.ZERO;
                            }
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }
                    } else {
                        final Set<PartialInvoicedTransaction> partialInvoicedTransactions = accrualTransaction
                                .getPartialInvoicedTransactions();
                        final BigDecimal penaltyPortionAccountedFor = partialInvoicedTransactions.stream()
                                .map(PartialInvoicedTransaction::getPenalty).reduce(BigDecimal.ZERO, BigDecimal::add);
                        final BigDecimal penaltyVatPortionAccountedFor = partialInvoicedTransactions.stream()
                                .map(PartialInvoicedTransaction::getPenaltyVat).reduce(BigDecimal.ZERO, BigDecimal::add);
                        final BigDecimal honorariosPortionAccountedFor = partialInvoicedTransactions.stream()
                                .map(PartialInvoicedTransaction::getHonorarios).reduce(BigDecimal.ZERO, BigDecimal::add);
                        final BigDecimal honorariosVatPortionAccountedFor = partialInvoicedTransactions.stream()
                                .map(PartialInvoicedTransaction::getHonorariosVat).reduce(BigDecimal.ZERO, BigDecimal::add);
                        final BigDecimal mandatoryInsurancePortionAccountedFor = partialInvoicedTransactions.stream()
                                .map(PartialInvoicedTransaction::getMandatoryInsurance).reduce(BigDecimal.ZERO, BigDecimal::add);
                        final BigDecimal mandatoryInsuranceVatPortionAccountedFor = partialInvoicedTransactions.stream()
                                .map(PartialInvoicedTransaction::getMandatoryInsuranceVat).reduce(BigDecimal.ZERO, BigDecimal::add);
                        final BigDecimal voluntaryInsurancePortionAccountedFor = partialInvoicedTransactions.stream()
                                .map(PartialInvoicedTransaction::getVoluntaryInsurance).reduce(BigDecimal.ZERO, BigDecimal::add);
                        final BigDecimal voluntaryInsuranceVatPortionAccountedFor = partialInvoicedTransactions.stream()
                                .map(PartialInvoicedTransaction::getVoluntaryInsuranceVat).reduce(BigDecimal.ZERO, BigDecimal::add);
                        final BigDecimal interestPortionAccountedFor = partialInvoicedTransactions.stream()
                                .map(PartialInvoicedTransaction::getInterest).reduce(BigDecimal.ZERO, BigDecimal::add);

                        final BigDecimal interestToBeInvoiced = interestPortion.subtract(interestPortionAccountedFor);
                        final BigDecimal mandatoryInsuranceToBeInvoiced = mandatoryInsurancePortion
                                .subtract(mandatoryInsurancePortionAccountedFor);
                        final BigDecimal mandatoryInsuranceVatToBeInvoiced = mandatoryInsuranceVatPortion
                                .subtract(mandatoryInsuranceVatPortionAccountedFor);
                        final BigDecimal voluntaryInsuranceToBeInvoiced = voluntaryInsurancePortion
                                .subtract(voluntaryInsurancePortionAccountedFor);
                        final BigDecimal voluntaryInsuranceVatToBeInvoiced = voluntaryInsuranceVatPortion
                                .subtract(voluntaryInsuranceVatPortionAccountedFor);
                        final BigDecimal honorariosToBeInvoiced = honorariosPortion.subtract(honorariosPortionAccountedFor);
                        final BigDecimal honorariosVatToBeInvoiced = honorariosVatPortion.subtract(honorariosVatPortionAccountedFor);
                        final BigDecimal penaltyToBeInvoiced = penaltyPortion.subtract(penaltyPortionAccountedFor);
                        final BigDecimal penaltyVatToBeInvoiced = penaltyVatPortion.subtract(penaltyVatPortionAccountedFor);

                        if (interestPaidRemaining.compareTo(BigDecimal.ZERO) > 0 && interestToBeInvoiced.compareTo(BigDecimal.ZERO) > 0) {
                            if (interestPaidRemaining.compareTo(interestToBeInvoiced) >= 0) {
                                partialInvoicedTransaction.setInterest(interestToBeInvoiced);
                                interestPaidRemaining = interestPaidRemaining.subtract(interestToBeInvoiced);
                            } else {
                                partialInvoicedTransaction.setInterest(interestPaidRemaining);
                                interestPaidRemaining = BigDecimal.ZERO;
                            }
                            accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (penaltyChargesPaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && penaltyToBeInvoiced.compareTo(BigDecimal.ZERO) > 0) {
                            if (penaltyChargesPaidRemaining.compareTo(penaltyToBeInvoiced) >= 0) {
                                partialInvoicedTransaction.setPenalty(penaltyToBeInvoiced);
                                penaltyChargesPaidRemaining = penaltyChargesPaidRemaining.subtract(penaltyToBeInvoiced);
                            } else {
                                partialInvoicedTransaction.setPenalty(penaltyChargesPaidRemaining);
                                penaltyChargesPaidRemaining = BigDecimal.ZERO;
                            }
                            accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (penaltyChargesVatPaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && penaltyVatToBeInvoiced.compareTo(BigDecimal.ZERO) > 0) {
                            if (penaltyChargesVatPaidRemaining.compareTo(penaltyVatToBeInvoiced) >= 0) {
                                partialInvoicedTransaction.setPenaltyVat(penaltyVatToBeInvoiced);
                                penaltyChargesVatPaidRemaining = penaltyChargesVatPaidRemaining.subtract(penaltyVatToBeInvoiced);
                            } else {
                                partialInvoicedTransaction.setPenaltyVat(penaltyChargesVatPaidRemaining);
                                penaltyChargesVatPaidRemaining = BigDecimal.ZERO;
                            }
                            accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (mandatoryInsurancePaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && mandatoryInsuranceToBeInvoiced.compareTo(BigDecimal.ZERO) > 0) {
                            if (mandatoryInsurancePaidRemaining.compareTo(mandatoryInsuranceToBeInvoiced) >= 0) {
                                partialInvoicedTransaction.setMandatoryInsurance(mandatoryInsuranceToBeInvoiced);
                                mandatoryInsurancePaidRemaining = mandatoryInsurancePaidRemaining.subtract(mandatoryInsuranceToBeInvoiced);
                            } else {
                                partialInvoicedTransaction.setMandatoryInsurance(mandatoryInsurancePaidRemaining);
                                mandatoryInsurancePaidRemaining = BigDecimal.ZERO;
                            }
                            accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (mandatoryInsuranceVatPaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && mandatoryInsuranceVatToBeInvoiced.compareTo(BigDecimal.ZERO) > 0) {
                            if (mandatoryInsuranceVatPaidRemaining.compareTo(mandatoryInsuranceVatToBeInvoiced) >= 0) {
                                partialInvoicedTransaction.setMandatoryInsuranceVat(mandatoryInsuranceVatToBeInvoiced);
                                mandatoryInsuranceVatPaidRemaining = mandatoryInsuranceVatPaidRemaining
                                        .subtract(mandatoryInsuranceVatToBeInvoiced);
                            } else {
                                partialInvoicedTransaction.setMandatoryInsuranceVat(mandatoryInsuranceVatPaidRemaining);
                                mandatoryInsuranceVatPaidRemaining = BigDecimal.ZERO;
                            }
                            accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (voluntaryInsurancePaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && voluntaryInsuranceToBeInvoiced.compareTo(BigDecimal.ZERO) > 0) {
                            if (voluntaryInsurancePaidRemaining.compareTo(voluntaryInsuranceToBeInvoiced) >= 0) {
                                partialInvoicedTransaction.setVoluntaryInsurance(voluntaryInsuranceToBeInvoiced);
                                voluntaryInsurancePaidRemaining = voluntaryInsurancePaidRemaining.subtract(voluntaryInsuranceToBeInvoiced);
                            } else {
                                partialInvoicedTransaction.setVoluntaryInsurance(voluntaryInsurancePaidRemaining);
                                voluntaryInsurancePaidRemaining = BigDecimal.ZERO;
                            }
                            accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (voluntaryInsuranceVatPaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && voluntaryInsuranceVatToBeInvoiced.compareTo(BigDecimal.ZERO) > 0) {
                            if (voluntaryInsuranceVatPaidRemaining.compareTo(voluntaryInsuranceVatToBeInvoiced) >= 0) {
                                partialInvoicedTransaction.setVoluntaryInsuranceVat(voluntaryInsuranceVatToBeInvoiced);
                                voluntaryInsuranceVatPaidRemaining = voluntaryInsuranceVatPaidRemaining
                                        .subtract(voluntaryInsuranceVatToBeInvoiced);
                            } else {
                                partialInvoicedTransaction.setVoluntaryInsuranceVat(voluntaryInsuranceVatPaidRemaining);
                                voluntaryInsuranceVatPaidRemaining = BigDecimal.ZERO;
                            }
                            accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (honorariosPaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && honorariosToBeInvoiced.compareTo(BigDecimal.ZERO) > 0) {
                            if (honorariosPaidRemaining.compareTo(honorariosToBeInvoiced) >= 0) {
                                partialInvoicedTransaction.setHonorarios(honorariosToBeInvoiced);
                                honorariosPaidRemaining = honorariosPaidRemaining.subtract(honorariosToBeInvoiced);
                            } else {
                                partialInvoicedTransaction.setHonorarios(honorariosPaidRemaining);
                                honorariosPaidRemaining = BigDecimal.ZERO;
                            }
                            accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }

                        if (honorariosVatPaidRemaining.compareTo(BigDecimal.ZERO) > 0
                                && honorariosVatToBeInvoiced.compareTo(BigDecimal.ZERO) > 0) {
                            if (honorariosVatPaidRemaining.compareTo(honorariosVatToBeInvoiced) >= 0) {
                                partialInvoicedTransaction.setHonorariosVat(honorariosVatToBeInvoiced);
                                honorariosVatPaidRemaining = honorariosVatPaidRemaining.subtract(honorariosVatToBeInvoiced);
                            } else {
                                partialInvoicedTransaction.setHonorariosVat(honorariosVatPaidRemaining);
                                honorariosVatPaidRemaining = BigDecimal.ZERO;
                            }
                            accrualTransaction.getPartialInvoicedTransactions().add(partialInvoicedTransaction);
                            invoicedByAccrualTransactionDataSet.add(loanTransactionData);
                            invoicedByAccrualTransactionSet.add(accrualTransaction);
                        }
                    }
                }

                if (CollectionUtils.isNotEmpty(invoicedByAccrualTransactionDataSet)) {
                    final BigDecimal interestPaid = invoicedByAccrualTransactionDataSet.stream()
                            .filter(LoanTransactionData::isOccurredOnSuspendedAccount).map(ltd -> {
                                if (ltd.isPartiallyInvoiced()) {
                                    final Optional<LoanTransaction> txOptional = invoicedByAccrualTransactionSet.stream()
                                            .filter(ltx -> Objects.equals(ltx.getId(), ltd.getId())).findFirst();
                                    if (txOptional.isPresent()) {
                                        final LoanTransaction tx = txOptional.get();
                                        final Set<PartialInvoicedTransaction> partialInvoicedTransactions = tx
                                                .getPartialInvoicedTransactions();
                                        return partialInvoicedTransactions.stream()
                                                .filter(i -> Objects.equals(i.getRepaymentTransaction().getId(), loanTransactionId))
                                                .map(PartialInvoicedTransaction::getInterest).reduce(BigDecimal.ZERO, BigDecimal::add);
                                    } else {
                                        return BigDecimal.ZERO;
                                    }
                                } else {
                                    return ltd.getInterestPortion();
                                }
                            }).reduce(BigDecimal.ZERO, BigDecimal::add);

                    final BigDecimal mandatoryInsurancePaid = invoicedByAccrualTransactionDataSet.stream()
                            .filter(LoanTransactionData::isOccurredOnSuspendedAccount).map(ltd -> {
                                if (ltd.isPartiallyInvoiced()) {
                                    final Optional<LoanTransaction> txOptional = invoicedByAccrualTransactionSet.stream()
                                            .filter(ltx -> Objects.equals(ltx.getId(), ltd.getId())).findFirst();
                                    if (txOptional.isPresent()) {
                                        final LoanTransaction tx = txOptional.get();
                                        final Set<PartialInvoicedTransaction> partialInvoicedTransactions = tx
                                                .getPartialInvoicedTransactions();
                                        return partialInvoicedTransactions.stream()
                                                .filter(i -> Objects.equals(i.getRepaymentTransaction().getId(), loanTransactionId))
                                                .map(PartialInvoicedTransaction::getMandatoryInsurance)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    } else {
                                        return BigDecimal.ZERO;
                                    }
                                } else {
                                    return ltd.getLoanChargePaidBySummary().getMandatoryInsurancePortion();
                                }
                            })

                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    final BigDecimal mandatoryInsuranceVatPaid = invoicedByAccrualTransactionDataSet.stream()
                            .filter(LoanTransactionData::isOccurredOnSuspendedAccount).map(ltd -> {
                                if (ltd.isPartiallyInvoiced()) {
                                    final Optional<LoanTransaction> txOptional = invoicedByAccrualTransactionSet.stream()
                                            .filter(ltx -> Objects.equals(ltx.getId(), ltd.getId())).findFirst();
                                    if (txOptional.isPresent()) {
                                        final LoanTransaction tx = txOptional.get();
                                        final Set<PartialInvoicedTransaction> partialInvoicedTransactions = tx
                                                .getPartialInvoicedTransactions();
                                        return partialInvoicedTransactions.stream()
                                                .filter(i -> Objects.equals(i.getRepaymentTransaction().getId(), loanTransactionId))
                                                .map(PartialInvoicedTransaction::getMandatoryInsuranceVat)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    } else {
                                        return BigDecimal.ZERO;
                                    }
                                } else {
                                    return ltd.getLoanChargePaidBySummary().getMandatoryInsuranceVatPortion();
                                }
                            }).reduce(BigDecimal.ZERO, BigDecimal::add);

                    final BigDecimal voluntaryInsurancePaid = invoicedByAccrualTransactionDataSet.stream()
                            .filter(LoanTransactionData::isOccurredOnSuspendedAccount).map(ltd -> {
                                if (ltd.isPartiallyInvoiced()) {
                                    final Optional<LoanTransaction> txOptional = invoicedByAccrualTransactionSet.stream()
                                            .filter(ltx -> Objects.equals(ltx.getId(), ltd.getId())).findFirst();
                                    if (txOptional.isPresent()) {
                                        final LoanTransaction tx = txOptional.get();
                                        final Set<PartialInvoicedTransaction> partialInvoicedTransactions = tx
                                                .getPartialInvoicedTransactions();
                                        return partialInvoicedTransactions.stream()
                                                .filter(i -> Objects.equals(i.getRepaymentTransaction().getId(), loanTransactionId))
                                                .map(PartialInvoicedTransaction::getVoluntaryInsurance)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    } else {
                                        return BigDecimal.ZERO;
                                    }
                                } else {
                                    return ltd.getLoanChargePaidBySummary().getVoluntaryInsurancePortion();
                                }
                            }).reduce(BigDecimal.ZERO, BigDecimal::add);

                    final BigDecimal voluntaryInsuranceVatPaid = invoicedByAccrualTransactionDataSet.stream()
                            .filter(LoanTransactionData::isOccurredOnSuspendedAccount).map(ltd -> {
                                if (ltd.isPartiallyInvoiced()) {
                                    final Optional<LoanTransaction> txOptional = invoicedByAccrualTransactionSet.stream()
                                            .filter(ltx -> Objects.equals(ltx.getId(), ltd.getId())).findFirst();
                                    if (txOptional.isPresent()) {
                                        final LoanTransaction tx = txOptional.get();
                                        final Set<PartialInvoicedTransaction> partialInvoicedTransactions = tx
                                                .getPartialInvoicedTransactions();
                                        return partialInvoicedTransactions.stream()
                                                .filter(i -> Objects.equals(i.getRepaymentTransaction().getId(), loanTransactionId))
                                                .map(PartialInvoicedTransaction::getVoluntaryInsuranceVat)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    } else {
                                        return BigDecimal.ZERO;
                                    }
                                } else {
                                    return ltd.getLoanChargePaidBySummary().getVoluntaryInsuranceVatPortion();
                                }
                            }).reduce(BigDecimal.ZERO, BigDecimal::add);

                    final BigDecimal honorariosPaid = invoicedByAccrualTransactionDataSet.stream()
                            .filter(LoanTransactionData::isOccurredOnSuspendedAccount).map(ltd -> {
                                if (ltd.isPartiallyInvoiced()) {
                                    final Optional<LoanTransaction> txOptional = invoicedByAccrualTransactionSet.stream()
                                            .filter(ltx -> Objects.equals(ltx.getId(), ltd.getId())).findFirst();
                                    if (txOptional.isPresent()) {
                                        final LoanTransaction tx = txOptional.get();
                                        final Set<PartialInvoicedTransaction> partialInvoicedTransactions = tx
                                                .getPartialInvoicedTransactions();
                                        return partialInvoicedTransactions.stream()
                                                .filter(i -> Objects.equals(i.getRepaymentTransaction().getId(), loanTransactionId))
                                                .map(PartialInvoicedTransaction::getHonorarios).reduce(BigDecimal.ZERO, BigDecimal::add);
                                    } else {
                                        return BigDecimal.ZERO;
                                    }
                                } else {
                                    return ltd.getLoanChargePaidBySummary().getHonorariosPortion();
                                }
                            }).reduce(BigDecimal.ZERO, BigDecimal::add);

                    final BigDecimal honorariosVatPaid = invoicedByAccrualTransactionDataSet.stream()
                            .filter(LoanTransactionData::isOccurredOnSuspendedAccount).map(ltd -> {
                                if (ltd.isPartiallyInvoiced()) {
                                    final Optional<LoanTransaction> txOptional = invoicedByAccrualTransactionSet.stream()
                                            .filter(ltx -> Objects.equals(ltx.getId(), ltd.getId())).findFirst();
                                    if (txOptional.isPresent()) {
                                        final LoanTransaction tx = txOptional.get();
                                        final Set<PartialInvoicedTransaction> partialInvoicedTransactions = tx
                                                .getPartialInvoicedTransactions();
                                        return partialInvoicedTransactions.stream()
                                                .filter(i -> Objects.equals(i.getRepaymentTransaction().getId(), loanTransactionId))
                                                .map(PartialInvoicedTransaction::getHonorariosVat).reduce(BigDecimal.ZERO, BigDecimal::add);
                                    } else {
                                        return BigDecimal.ZERO;
                                    }
                                } else {
                                    return ltd.getLoanChargePaidBySummary().getHonorariosVatPortion();
                                }
                            }).reduce(BigDecimal.ZERO, BigDecimal::add);

                    final BigDecimal penaltyChargesPaid = invoicedByAccrualTransactionDataSet.stream()
                            .filter(LoanTransactionData::isOccurredOnSuspendedAccount).map(ltd -> {
                                if (ltd.isPartiallyInvoiced()) {
                                    final Optional<LoanTransaction> txOptional = invoicedByAccrualTransactionSet.stream()
                                            .filter(ltx -> Objects.equals(ltx.getId(), ltd.getId())).findFirst();
                                    if (txOptional.isPresent()) {
                                        final LoanTransaction tx = txOptional.get();
                                        final Set<PartialInvoicedTransaction> partialInvoicedTransactions = tx
                                                .getPartialInvoicedTransactions();
                                        return partialInvoicedTransactions.stream()
                                                .filter(i -> Objects.equals(i.getRepaymentTransaction().getId(), loanTransactionId))
                                                .map(PartialInvoicedTransaction::getPenalty).reduce(BigDecimal.ZERO, BigDecimal::add);
                                    } else {
                                        return BigDecimal.ZERO;
                                    }
                                } else {
                                    return ltd.getLoanChargePaidBySummary().getPenaltyPortion();
                                }
                            }).reduce(BigDecimal.ZERO, BigDecimal::add);

                    final BigDecimal penaltyChargesVatPaid = invoicedByAccrualTransactionDataSet.stream()
                            .filter(LoanTransactionData::isOccurredOnSuspendedAccount).map(ltd -> {
                                if (ltd.isPartiallyInvoiced()) {
                                    final Optional<LoanTransaction> txOptional = invoicedByAccrualTransactionSet.stream()
                                            .filter(ltx -> Objects.equals(ltx.getId(), ltd.getId())).findFirst();
                                    if (txOptional.isPresent()) {
                                        final LoanTransaction tx = txOptional.get();
                                        final Set<PartialInvoicedTransaction> partialInvoicedTransactions = tx
                                                .getPartialInvoicedTransactions();
                                        return partialInvoicedTransactions.stream()
                                                .filter(i -> Objects.equals(i.getRepaymentTransaction().getId(), loanTransactionId))
                                                .map(PartialInvoicedTransaction::getPenaltyVat).reduce(BigDecimal.ZERO, BigDecimal::add);
                                    } else {
                                        return BigDecimal.ZERO;
                                    }
                                } else {
                                    return ltd.getLoanChargePaidBySummary().getPenaltyVatPortion();
                                }
                            }).reduce(BigDecimal.ZERO, BigDecimal::add);

                    loanDocumentData.setInterestPaid(interestPaid);

                    loanDocumentData.setMandatoryInsurancePaid(mandatoryInsurancePaid);
                    loanDocumentData.setMandatoryInsuranceVatPaid(mandatoryInsuranceVatPaid);

                    loanDocumentData.setVoluntaryInsurancePaid(voluntaryInsurancePaid);
                    loanDocumentData.setVoluntaryInsuranceVatPaid(voluntaryInsuranceVatPaid);

                    loanDocumentData.setHonorariosPaid(honorariosPaid);
                    loanDocumentData.setHonorariosVatPaid(honorariosVatPaid);

                    loanDocumentData.setPenaltyChargesPaid(penaltyChargesPaid);
                    loanDocumentData.setPenaltyChargesVatPaid(penaltyChargesVatPaid);
                    final boolean isTriggeredByJob = false;
                    this.processInvoicesForClientIdAndProductType(List.of(loanDocumentData), isTriggeredByJob);
                    this.loanTransactionRepository.saveAndFlush(loanTransaction);
                    for (final LoanTransaction accrualTransaction : invoicedByAccrualTransactionSet) {
                        if (!accrualTransaction.isPartiallyInvoiced()) {
                            accrualTransaction.setInvoicedByTransactionId(loanTransactionId);
                        }
                    }
                    this.loanTransactionRepository.saveAll(invoicedByAccrualTransactionSet);
                }
            }
        }
    }

    public void populateImpuestoItem(final FacturaElectronicaMensual facturaElectronicaMensual,
            final ClasificacionConceptosData clasificacionConceptosData, final BigDecimal impuestoItem) {
        if (clasificacionConceptosData != null) {
            if (clasificacionConceptosData.isExento()) {
                facturaElectronicaMensual.setPorcentaje_impuesto_item(BigDecimal.ZERO);
                facturaElectronicaMensual.setImpuesto_item(BigDecimal.ZERO);
            } else if (clasificacionConceptosData.isGravado()) {
                final BigDecimal tarifa = clasificacionConceptosData.getTarifa();
                facturaElectronicaMensual.setPorcentaje_impuesto_item(tarifa);
                facturaElectronicaMensual.setImpuesto_item(impuestoItem);
            } else {
                facturaElectronicaMensual.setPorcentaje_impuesto_item(null);
                facturaElectronicaMensual.setImpuesto_item(null);
            }
        } else {
            facturaElectronicaMensual.setPorcentaje_impuesto_item(null);
            facturaElectronicaMensual.setImpuesto_item(null);
        }
    }

    @SuppressWarnings("all")
    @Override
    public void processInvoicesForClientIdAndProductType(final List<LoanDocumentData> loanDocumentDataList,
            final boolean isTriggeredByJob) {
        if (CollectionUtils.isNotEmpty(loanDocumentDataList)) {
            log.info("Processing invoices for client id: {} and product type: {}", loanDocumentDataList.get(0).getClientIdNumber(),
                    loanDocumentDataList.get(0).getProductTypeName());
            final LoanDocumentData firstLoanDocumentData = loanDocumentDataList.get(0);
            final LoanProductParameterization loanProductParameterization = this.productParameterizationRepository
                    .findById(firstLoanDocumentData.getProductTypeParamId())
                    .orElseThrow(() -> new LoanProductParameterizationNotFoundException(firstLoanDocumentData.getProductTypeParamId()));
            log.info("Generating document number to be the same for all invoices for client id: {} and product type: {}",
                    firstLoanDocumentData.getClientIdNumber(), loanProductParameterization.getProductType());
            final String documentNumber = isTriggeredByJob ? generateInvoiceNumber() : nextDocumentNumber(loanProductParameterization);
            log.info("Document number generated: {}", documentNumber);
            final List<FacturaElectronicaMensual> facturaElectronicaMensuals = new ArrayList<>();

            BigDecimal totalInvoiceAmount = BigDecimal.ZERO;

            for (final LoanDocumentData loanDocumentData : loanDocumentDataList) {
                loanDocumentData.setDocumentType(LoanDocumentData.LoanDocumentType.INVOICE);
                final BigDecimal interestPaid = loanDocumentData.getInterestPaid();
                final BigDecimal interestVatPaid = BigDecimal.ZERO;

                final BigDecimal penaltyChargesPaid = loanDocumentData.getPenaltyChargesPaid();
                final BigDecimal penaltyChargesVatPaid = loanDocumentData.getPenaltyChargesVatPaid();

                final BigDecimal mandatoryInsurancePaid = loanDocumentData.getMandatoryInsurancePaid();
                final BigDecimal mandatoryInsuranceVatPaid = loanDocumentData.getMandatoryInsuranceVatPaid();

                final BigDecimal voluntaryInsurancePaid = loanDocumentData.getVoluntaryInsurancePaid();
                final BigDecimal voluntaryInsuranceVatPaid = loanDocumentData.getVoluntaryInsuranceVatPaid();

                final BigDecimal honorariosPaid = loanDocumentData.getHonorariosPaid();
                final BigDecimal honorariosVatPaid = loanDocumentData.getHonorariosVatPaid();
                final FacturaElectronicaMensual facturaElectronicaMensual = generateInvoice(loanDocumentData, loanProductParameterization,
                        documentNumber);
                totalInvoiceAmount = totalInvoiceAmount.add(facturaElectronicaMensual.getTotal());
                facturaElectronicaMensual.setAccrualTransactionIds(loanDocumentData.getTransactionIds());

                if (interestPaid.compareTo(BigDecimal.ZERO) > 0) {
                    final LoanDocumentConcept loanDocumentConcept = LoanDocumentConcept.INT_CORRIENTE;
                    final FacturaElectronicaMensual facturaElectronicaMensualDuplicate = facturaElectronicaMensual.clone();
                    facturaElectronicaMensualDuplicate.setCosto_total(interestPaid);
                    facturaElectronicaMensualDuplicate.setPrecio_unitario(interestPaid);
                    facturaElectronicaMensualDuplicate.setSku(loanDocumentConcept.getSku());
                    facturaElectronicaMensualDuplicate.setNom_articulo(loanDocumentConcept.getName());
                    facturaElectronicaMensualDuplicate.setId_mandante(null);
                    facturaElectronicaMensualDuplicate.setDescripcion_mandante(null);

                    final ClasificacionConceptosData clasificacionConceptosData = this
                            .getClasificacionConceptosData(loanDocumentConcept.name());
                    this.populateImpuestoItem(facturaElectronicaMensualDuplicate, clasificacionConceptosData, interestVatPaid);
                    facturaElectronicaMensuals.add(facturaElectronicaMensualDuplicate);
                    log.info("Adding interest invoice item for client id: {} and product type: {}",
                            firstLoanDocumentData.getClientIdNumber(), firstLoanDocumentData.getProductTypeName());
                }
                if (penaltyChargesPaid.compareTo(BigDecimal.ZERO) > 0) {
                    final LoanDocumentConcept loanDocumentConcept = LoanDocumentConcept.INT_DE_MORA;
                    final FacturaElectronicaMensual facturaElectronicaMensualDuplicate = facturaElectronicaMensual.clone();
                    facturaElectronicaMensualDuplicate.setCosto_total(penaltyChargesPaid);
                    facturaElectronicaMensualDuplicate.setPrecio_unitario(penaltyChargesPaid);
                    facturaElectronicaMensualDuplicate.setSku(loanDocumentConcept.getSku());
                    facturaElectronicaMensualDuplicate.setNom_articulo(loanDocumentConcept.getName());
                    facturaElectronicaMensualDuplicate.setId_mandante(null);
                    facturaElectronicaMensualDuplicate.setDescripcion_mandante(null);
                    final ClasificacionConceptosData clasificacionConceptosData = this
                            .getClasificacionConceptosData(loanDocumentConcept.name());
                    this.populateImpuestoItem(facturaElectronicaMensualDuplicate, clasificacionConceptosData, penaltyChargesVatPaid);
                    facturaElectronicaMensuals.add(facturaElectronicaMensualDuplicate);
                    log.info("Adding penalty invoice item for client id: {} and product type: {}",
                            firstLoanDocumentData.getClientIdNumber(), firstLoanDocumentData.getProductTypeName());
                }
                if (mandatoryInsurancePaid.compareTo(BigDecimal.ZERO) > 0) {
                    final LoanDocumentConcept loanDocumentConcept = LoanDocumentConcept.SEGURO_OBLIGATORIO;
                    final FacturaElectronicaMensual facturaElectronicaMensualDuplicate = facturaElectronicaMensual.clone();
                    facturaElectronicaMensualDuplicate.setCosto_total(mandatoryInsurancePaid);
                    facturaElectronicaMensualDuplicate.setPrecio_unitario(mandatoryInsurancePaid);
                    facturaElectronicaMensualDuplicate.setSku(loanDocumentConcept.getSku());
                    facturaElectronicaMensualDuplicate.setNom_articulo(loanDocumentConcept.getName());

                    final String mandatoryInsuranceNIT = loanDocumentData.getMandatoryInsuranceNIT();
                    final String mandatoryInsuranceName = loanDocumentData.getMandatoryInsuranceName();

                    facturaElectronicaMensualDuplicate.setId_mandante(mandatoryInsuranceNIT);
                    facturaElectronicaMensualDuplicate.setDescripcion_mandante(mandatoryInsuranceName);

                    final ClasificacionConceptosData clasificacionConceptosData = this
                            .getClasificacionConceptosData(loanDocumentConcept.name());
                    this.populateImpuestoItem(facturaElectronicaMensualDuplicate, clasificacionConceptosData, mandatoryInsuranceVatPaid);
                    facturaElectronicaMensuals.add(facturaElectronicaMensualDuplicate);
                    log.info("Adding mandatory insurance invoice item for client id: {} and product type: {}",
                            firstLoanDocumentData.getClientIdNumber(), firstLoanDocumentData.getProductTypeName());
                }
                if (voluntaryInsurancePaid.compareTo(BigDecimal.ZERO) > 0) {
                    final LoanDocumentConcept loanDocumentConcept = LoanDocumentConcept.SEGUROS_VOLUNTARIOS;
                    final FacturaElectronicaMensual facturaElectronicaMensualDuplicate = facturaElectronicaMensual.clone();
                    facturaElectronicaMensualDuplicate.setCosto_total(voluntaryInsurancePaid);
                    facturaElectronicaMensualDuplicate.setPrecio_unitario(voluntaryInsurancePaid);
                    facturaElectronicaMensualDuplicate.setSku(loanDocumentConcept.getSku());
                    facturaElectronicaMensualDuplicate.setNom_articulo(loanDocumentConcept.getName());

                    final String voluntaryInsuranceNIT = loanDocumentData.getVoluntaryInsuranceNIT();
                    final String voluntaryInsuranceName = loanDocumentData.getVoluntaryInsuranceName();

                    facturaElectronicaMensualDuplicate.setId_mandante(voluntaryInsuranceNIT);
                    facturaElectronicaMensualDuplicate.setDescripcion_mandante(voluntaryInsuranceName);

                    final ClasificacionConceptosData clasificacionConceptosData = this
                            .getClasificacionConceptosData(loanDocumentConcept.name());
                    this.populateImpuestoItem(facturaElectronicaMensualDuplicate, clasificacionConceptosData, voluntaryInsuranceVatPaid);
                    facturaElectronicaMensuals.add(facturaElectronicaMensualDuplicate);
                    log.info("Adding voluntary insurance invoice item for client id: {} and product type: {}",
                            firstLoanDocumentData.getClientIdNumber(), firstLoanDocumentData.getProductTypeName());
                }
                if (honorariosPaid.compareTo(BigDecimal.ZERO) > 0) {
                    final LoanDocumentConcept loanDocumentConcept = LoanDocumentConcept.HONORARIOS;
                    final FacturaElectronicaMensual facturaElectronicaMensualDuplicate = facturaElectronicaMensual.clone();
                    facturaElectronicaMensualDuplicate.setCosto_total(honorariosPaid);
                    facturaElectronicaMensualDuplicate.setPrecio_unitario(honorariosPaid);
                    facturaElectronicaMensualDuplicate.setSku(loanDocumentConcept.getSku());
                    facturaElectronicaMensualDuplicate.setNom_articulo(loanDocumentConcept.getName());
                    facturaElectronicaMensualDuplicate.setId_mandante(loanDocumentData.getClientCollectionHouseNit());
                    facturaElectronicaMensualDuplicate.setDescripcion_mandante(loanDocumentData.getClientCollectionHouseName());
                    final ClasificacionConceptosData clasificacionConceptosData = this
                            .getClasificacionConceptosData(loanDocumentConcept.name());
                    this.populateImpuestoItem(facturaElectronicaMensualDuplicate, clasificacionConceptosData, honorariosVatPaid);
                    facturaElectronicaMensuals.add(facturaElectronicaMensualDuplicate);
                    log.info("Adding honorarios invoice item for client id: {} and product type: {}",
                            firstLoanDocumentData.getClientIdNumber(), firstLoanDocumentData.getProductTypeName());
                }
            }

            log.info("Final processing and persisting to the underlying database for client id: {} and product type: {}",
                    firstLoanDocumentData.getClientIdNumber(), firstLoanDocumentData.getProductTypeName());
            final BigDecimal totalImpuestoItem = facturaElectronicaMensuals.stream().map(FacturaElectronicaMensual::getImpuesto_item)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            final BigDecimal porcentajeImpuestoItem = facturaElectronicaMensuals.stream()
                    .filter(f -> f.getPorcentaje_impuesto_item() != null).findFirst().orElse(new FacturaElectronicaMensual())
                    .getPorcentaje_impuesto_item();

            long itemPosition = 1L;
            long itemCounts = facturaElectronicaMensuals.size();
            for (final FacturaElectronicaMensual facturaElectronicaMensualItem : facturaElectronicaMensuals) {
                facturaElectronicaMensualItem.setBase(totalInvoiceAmount);
                facturaElectronicaMensualItem.setTotal(totalInvoiceAmount);
                final BigDecimal totalValue = facturaElectronicaMensualItem.getTotal().add(totalImpuestoItem);
                facturaElectronicaMensualItem.setTotal(totalValue);
                facturaElectronicaMensualItem.setImpuesto(totalImpuestoItem);
                facturaElectronicaMensualItem.setPorcentaje_impuesto(porcentajeImpuestoItem);
                facturaElectronicaMensualItem.setPosicion(itemPosition);
                facturaElectronicaMensualItem.setTotal_unidades(String.valueOf(itemCounts));
                itemPosition = itemPosition + 1L;
            }
            if (!facturaElectronicaMensuals.isEmpty()) {
                log.info("Saving invoice data for client id: {} and product type: {}", firstLoanDocumentData.getClientIdNumber(),
                        firstLoanDocumentData.getProductTypeName());
                this.facturaElectronicMensualRepository.saveAllAndFlush(facturaElectronicaMensuals);
                if (!isTriggeredByJob) {
                    log.info("Updating invoice counter for product type: {}", loanProductParameterization.getProductType());
                    loanProductParameterization.setInvoiceCounter(loanProductParameterization.getNextInvoiceCounter());
                    this.productParameterizationRepository.saveAndFlush(loanProductParameterization);
                }
            }
            log.info("Completed processing invoices for client ID: {} and product type: {}",
                    loanDocumentDataList.get(0).getClientIdNumber(), loanDocumentDataList.get(0).getProductTypeName());
        }
    }

    private String generateInvoiceNumber() {
        final long timestamp = Instant.now().toEpochMilli();
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        final SecureRandom random = new SecureRandom();
        final long randomLong = random.nextLong();
        final String combinedNumber = Long.toHexString(timestamp) + uuid + Long.toHexString(randomLong);
        return "TEMPORARY_" + combinedNumber.substring(0, 25).toUpperCase();
    }

    private String nextDocumentNumber(final LoanProductParameterization loanProductParameterization) {
        final long rangeStartNumber = loanProductParameterization.getRangeStartNumber();
        final long invoiceCounter = loanProductParameterization.getInvoiceCounter();
        final long rangeEndNumber = loanProductParameterization.getRangeEndNumber();
        final long currentCounter = ObjectUtils.defaultIfNull(invoiceCounter, 0L) + 1L;
        final long documentNumber = rangeStartNumber + invoiceCounter;
        if (currentCounter > rangeEndNumber) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.invoice.counter.exceeds.range.end.number",
                    String.format("Invoice counter exceeds the range end number: %s and product type: %s", rangeEndNumber,
                            loanProductParameterization.getProductType()));
        }
        loanProductParameterization.setNextInvoiceCounter(currentCounter);
        return String.valueOf(documentNumber);
    }

    private FacturaElectronicaMensual generateInvoice(final LoanDocumentData loanDocumentData,
            final LoanProductParameterization loanProductParameterization, final String documentNumber) {
        final FacturaElectronicaMensual facturaElectronicaMensual = loanDocumentData.toEntity();
        facturaElectronicaMensual.setFec_desde(loanProductParameterization.getGenerationDate());
        facturaElectronicaMensual.setFec_hasta(loanProductParameterization.getExpirationDate());
        facturaElectronicaMensual.setNumero_doc(documentNumber);
        facturaElectronicaMensual.setReferencia(documentNumber);
        facturaElectronicaMensual.setCodigo_descuento("0");
        facturaElectronicaMensual.setPorcentajedescuento(BigDecimal.ZERO);
        facturaElectronicaMensual.setDescuento(BigDecimal.ZERO);
        facturaElectronicaMensual.setPorcentaje_impuesto_item(BigDecimal.ZERO);
        facturaElectronicaMensual.setImpuesto_item(BigDecimal.ZERO);
        return facturaElectronicaMensual;
    }

    @Override
    public ClasificacionConceptosData getClasificacionConceptosData(final String concepto) {
        String sql = """
                    SELECT
                    	ccc.id AS id,
                    	ccc.concepto AS concepto,
                    	ccc.mandato AS mandato,
                    	ccc.excluido AS excluido,
                    	ccc.exento AS exento,
                    	ccc.gravado AS gravado,
                    	ccc.norma AS norma,
                    	ccc.tarifa AS tarifa
                    FROM c_clasificacion_conceptos ccc
                    WHERE ccc.concepto = ?
                """;
        final List<ClasificacionConceptosData> results = this.jdbcTemplate.query(sql, (rs, rowNum) -> {
            final Long id = rs.getLong("id");
            final String concept = rs.getString("concepto");
            final boolean mandato = rs.getBoolean("mandato");
            final boolean excluido = rs.getBoolean("excluido");
            final boolean exento = rs.getBoolean("exento");
            final boolean gravado = rs.getBoolean("gravado");
            final String norma = rs.getString("norma");
            final BigDecimal tarifa = rs.getBigDecimal("tarifa");
            return ClasificacionConceptosData.builder().id(id).concepto(concept).mandato(mandato).excluido(excluido).exento(exento)
                    .gravado(gravado).norma(norma).tarifa(tarifa).build();
        }, concepto);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public void recalculateInterestRate(final Loan loan) {
        final Long loanId = loan.getId();
        final InterestRate interestRate = loan.getLoanProduct().getInterestRate();
        if (interestRate != null) {
            BigDecimal annualNominalInterestRate = loan.getLoanProductRelatedDetail().getAnnualNominalInterestRate();
            if (loan.getLoanProduct().isRequirePoints()) {
                final Long interestRatePoints = ObjectUtils.defaultIfNull(loan.getLoanProductRelatedDetail().getInterestRatePoints(), 0L);
                annualNominalInterestRate = annualNominalInterestRate.add(BigDecimal.valueOf(interestRatePoints));
            }
            final BigDecimal currentRate = interestRate.getCurrentRate();
            final LocalDate appliedOnDate = interestRate.getAppliedOnDate();
            final LoanRepaymentScheduleInstallment firstInstallment = loan.fetchRepaymentScheduleInstallment(1);
            final LocalDate firstInstallmentFromDate = firstInstallment.getFromDate();
            if (!annualNominalInterestRate.equals(currentRate) && DateUtils.isAfter(appliedOnDate, firstInstallmentFromDate)) {
                final LocalDate actualMaturityDate = loan.getMaturityDate();
                if (DateUtils.isAfter(appliedOnDate, actualMaturityDate)) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.interest.rate.applied.on.date.is.after.maturity.date",
                            "Interest rate applied on date is after the loan maturity date", appliedOnDate, actualMaturityDate);
                }
                final MaximumCreditRateConfigurationData maximumCreditRateConfigurationData = this.loanProductReadPlatformService
                        .retrieveMaximumCreditRateConfigurationData();
                final BigDecimal maximumLegalAnnualNominalRate = maximumCreditRateConfigurationData.getAnnualNominalRate();
                if (currentRate.compareTo(maximumLegalAnnualNominalRate) > 0) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.interest.rate.exceeds.maximum.legal.rate",
                            "La tasa de interés del préstamo: " + loan.getId() + " excede la tasa de interés máxima legal");
                }
                final String locale = "en";
                final String dateFormat = "dd MMMM yyyy";
                final String submittedOnDate = DateUtils.format(DateUtils.getBusinessLocalDate(), dateFormat,
                        Locale.forLanguageTag(locale));
                LoanRescheduleRequestData loanRescheduleReasons = this.loanRescheduleRequestReadPlatformService
                        .retrieveAllRescheduleReasons(RescheduleLoansApiConstants.LOAN_RESCHEDULE_REASON, null);
                Long rescheduleReasonId = null;
                for (CodeValueData codeValueData : loanRescheduleReasons.getRescheduleReasons()) {
                    if (codeValueData.getName()
                            .equalsIgnoreCase(LoanRescheduleRequestWritePlatformServiceImpl.MAX_LEGAL_RATE_REASON_FOR_RESCHEDULE)) {
                        rescheduleReasonId = codeValueData.getId();
                        break;
                    }
                }
                final JsonObject rescheduleJsonObject = new JsonObject();
                rescheduleJsonObject.addProperty("dateFormat", dateFormat);
                rescheduleJsonObject.addProperty("locale", locale);
                rescheduleJsonObject.addProperty("rescheduleReasonId", rescheduleReasonId);
                rescheduleJsonObject.addProperty("submittedOnDate", submittedOnDate);
                rescheduleJsonObject.addProperty("rescheduleReasonComment",
                        LoanRescheduleRequestWritePlatformServiceImpl.MAX_LEGAL_RATE_REASON_FOR_RESCHEDULE);
                rescheduleJsonObject.addProperty("adjustedDueDate", "");
                rescheduleJsonObject.addProperty("graceOnPrincipal", "");
                rescheduleJsonObject.addProperty("extraTerms", "");
                rescheduleJsonObject.addProperty("newInterestRate", currentRate);
                final String rescheduleFromDateString = DateUtils.format(appliedOnDate, dateFormat, Locale.forLanguageTag(locale));
                rescheduleJsonObject.addProperty("rescheduleFromDate", rescheduleFromDateString);
                rescheduleJsonObject.addProperty("loanId", loanId);
                final String rescheduleRequestBodyAsJson = rescheduleJsonObject.toString();
                CommandWrapper commandWrapper = new CommandWrapperBuilder()
                        .createLoanRescheduleRequest(RescheduleLoansApiConstants.ENTITY_NAME).withJson(rescheduleRequestBodyAsJson).build();

                CommandProcessingResult commandProcessingResult = commandsSourceWritePlatformService.logCommandSource(commandWrapper);
                if (commandProcessingResult.getResourceId() != null) {
                    final Long loanRescheduleId = commandProcessingResult.getResourceId();
                    final JsonObject approvalJsonObject = new JsonObject();
                    final Boolean isJobTriggered = true;
                    approvalJsonObject.addProperty("dateFormat", dateFormat);
                    approvalJsonObject.addProperty("locale", locale);
                    approvalJsonObject.addProperty("isJobTriggered", isJobTriggered);
                    approvalJsonObject.addProperty("approvedOnDate", submittedOnDate);
                    final String approvalRequestBodyAsJson = approvalJsonObject.toString();
                    commandWrapper = new CommandWrapperBuilder()
                            .approveLoanRescheduleRequest(RescheduleLoansApiConstants.ENTITY_NAME, loanRescheduleId)
                            .withJson(approvalRequestBodyAsJson).build();
                    commandProcessingResult = commandsSourceWritePlatformService.logCommandSource(commandWrapper);
                    if (commandProcessingResult.getResourceId() != null) {
                        final String successMessage = "Reprogramar la cuenta de préstamo: " + loanId
                                + " con la tasa de interés al máximo legal";
                        log.info(successMessage);
                    }
                }
            }
        }
    }

    private long getDaysInArrears(final Long loanId) {
        String query = "SELECT overdue_since_date_derived FROM m_loan_arrears_aging WHERE loan_id = ?";
        List<LocalDate> results = this.jdbcTemplate.queryForList(query, LocalDate.class, loanId);
        if (results.isEmpty()) {
            return 0;
        }
        LocalDate overdueSinceDate = results.get(0);
        return overdueSinceDate != null ? ChronoUnit.DAYS.between(overdueSinceDate, DateUtils.getLocalDateOfTenant()) : 0;
    }

    @Override
    @Transactional
    public void persistInstallmentalChargeAccrual(Long loanId, LocalDate localDate, Long minimumDaysInArrearsToSuspendLoanAccount) {
        Loan loan = this.loanAssembler.assembleFrom(loanId);
        log.debug("Persisting Installment charge accrual for loan: {}", loan.getId());
        List<LoanCharge> charges = filterInstallmentCharges(loan.getActiveCharges());

        if (minimumDaysInArrearsToSuspendLoanAccount == null) {
            minimumDaysInArrearsToSuspendLoanAccount = 90L;
        }
        final Long daysInArrears = this.getDaysInArrears(loanId);
        final boolean hasOccurredOnSuspendedAccount = daysInArrears >= minimumDaysInArrearsToSuspendLoanAccount;
        loan.handleChargeAppliedTransactionPerInstallment(charges, localDate, hasOccurredOnSuspendedAccount);
        loanRepository.saveAndFlush(loan);
        log.debug("Installment  charge accrual persisted for loan: {}", loan.getId());
    }

    private List<LoanCharge> filterInstallmentCharges(Set<LoanCharge> charges) {
        return charges.stream()
                .filter(loanCharge -> loanCharge.getCharge().getChargeTimeType().equals(ChargeTimeType.INSTALMENT_FEE.getValue())
                        && !loanCharge.isWaived() && !loanCharge.isFullyPaid())
                .toList();
    }

    @Override
    public void cancelDefaultInsuranceCharges(List<DefaultOrCancelInsuranceInstallmentData> defaultInsuranceIds) {
        final LocalDate currentDate = DateUtils.getBusinessLocalDate();
        InsuranceIncident incident = this.insuranceIncidentRepository
                .findByIncidentType(InsuranceIncidentType.DEFINITIVE_CANCELLATION_DEFAULT);
        if (incident == null) {
            throw new InsuranceIncidentNotFoundException(InsuranceIncidentType.DEFINITIVE_CANCELLATION_DEFAULT.name());
        }
        for (DefaultOrCancelInsuranceInstallmentData data : defaultInsuranceIds) {
            Loan loan = this.loanAssembler.assembleFrom(data.loanId());
            LoanCharge loanCharge = null;
            Optional<LoanCharge> loanChargeOptional = loan.getLoanCharges().stream()
                    .filter(lc -> Objects.equals(lc.getId(), data.loanChargeId())).findFirst();
            if (loanChargeOptional.isPresent()) {
                loanCharge = loanChargeOptional.get();
                if ((incident.isMandatory() && loanCharge.isMandatoryInsurance())
                        || (incident.isVoluntary() && loanCharge.isVoluntaryInsurance())) {
                    BigDecimal cumulative = BigDecimal.ZERO;
                    cumulative = processInsuranceChargeCancellation(cumulative, loan, loanCharge, data, false);
                    InsuranceIncidentNoveltyNews insuranceIncidentNoveltyNews = InsuranceIncidentNoveltyNews.instance(loan, loanCharge,
                            data.installment(), incident, currentDate, cumulative);

                    this.insuranceIncidentNoveltyNewsRepository.saveAndFlush(insuranceIncidentNoveltyNews);
                    saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
                }
            }
        }
    }

    @Override
    public void temporarySuspendDefaultInsuranceCharges(List<DefaultOrCancelInsuranceInstallmentData> defaultInsuranceIds) {
        final LocalDate currentDate = DateUtils.getBusinessLocalDate();
        InsuranceIncident incident = this.insuranceIncidentRepository
                .findByIncidentType(InsuranceIncidentType.TEMPORARY_SUSPENSION_DUE_TO_DEFAULT);
        InsuranceIncident suspensionRemovedIncident = this.insuranceIncidentRepository
                .findByIncidentType(InsuranceIncidentType.SUSPENSION_REMOVED);
        if (incident == null || (!incident.isMandatory() && !incident.isVoluntary())) {
            throw new InsuranceIncidentNotFoundException(InsuranceIncidentType.TEMPORARY_SUSPENSION_DUE_TO_DEFAULT.name());
        }
        for (DefaultOrCancelInsuranceInstallmentData data : defaultInsuranceIds) {
            Loan loan = this.loanAssembler.assembleFrom(data.loanId());
            Optional<InsuranceIncidentNoveltyNews> lastSuspensionNewsOptional = this.insuranceIncidentNoveltyNewsRepository
                    .findLastSuspensionIfPresent(loan.getId(), incident.getId(), suspensionRemovedIncident.getId());
            if (lastSuspensionNewsOptional.isPresent()) {
                InsuranceIncidentNoveltyNews news = lastSuspensionNewsOptional.get();
                if (news.getInsuranceIncident().getIncidentType().equals(InsuranceIncidentType.TEMPORARY_SUSPENSION_DUE_TO_DEFAULT)) {
                    // Do not add suspension news if loan is already in suspension
                    continue;
                }
            }
            LoanCharge loanCharge = null;
            Optional<LoanCharge> loanChargeOptional = loan.getLoanCharges().stream()
                    .filter(lc -> Objects.equals(lc.getId(), data.loanChargeId())).findFirst();
            if (loanChargeOptional.isPresent()) {
                loanCharge = loanChargeOptional.get();
            }
            if ((incident.isMandatory() && loanCharge.isMandatoryInsurance())
                    || (incident.isVoluntary() && loanCharge.isVoluntaryInsurance())) {
                BigDecimal cumulative = BigDecimal.ZERO;
                InsuranceIncidentNoveltyNews insuranceIncidentNoveltyNews = InsuranceIncidentNoveltyNews.instance(loan, loanCharge, null,
                        incident, data.suspensionDate(), cumulative);

                this.insuranceIncidentNoveltyNewsRepository.saveAndFlush(insuranceIncidentNoveltyNews);
            }
        }
    }

    @Override
    public CommandProcessingResult cancelInsurance(JsonCommand command, boolean isVoluntaryInsurance) {
        this.loanEventApiJsonValidator.validateForInsuranceCancellation(command.json());
        Long loanId = command.longValueOfParameterNamed("creditId");
        Long insuranceCode = command.longValueOfParameterNamed("codigoSeguro");
        LocalDate cancellationDate = command.localDateValueOfParameterNamed("date");

        LoanCharge loanCharge = null;

        Loan loan = this.loanAssembler.assembleFrom(loanId);

        Optional<LoanCharge> loanChargeOptional = loan.getLoanCharges().stream()
                .filter(lc -> lc.getCharge().getChargeInsuranceDetail() != null
                        && Objects.equals(lc.getCharge().getChargeInsuranceDetail().getInsuranceCode(), insuranceCode))
                .findFirst();
        if (loanChargeOptional.isPresent()) {
            loanCharge = loanChargeOptional.get();
        } else {
            throw new LoanChargeNotFoundException(
                    "No se encontró cargo de préstamo contra identificación de crédito [" + loan.getId() + "]");
        }

        List<DefaultOrCancelInsuranceInstallmentData> cancelInsuranceInstallmentIds;
        if (isVoluntaryInsurance) {
            cancelInsuranceInstallmentIds = this.loanReadPlatformService.getLoanDataWithDefaultOrCancelInsurance(loanId, insuranceCode,
                    cancellationDate);
        } else {
            cancelInsuranceInstallmentIds = this.loanReadPlatformService.getLoanDataWithDefaultOrCancelInsurance(loanId, insuranceCode,
                    null);
        }

        if (!cancelInsuranceInstallmentIds.isEmpty()) {
            DefaultOrCancelInsuranceInstallmentData cancelInsuranceInstallmentData = cancelInsuranceInstallmentIds.get(0);
            final LocalDate currentDate = DateUtils.getBusinessLocalDate();
            InsuranceIncident incident = null;
            if (isVoluntaryInsurance) {
                incident = this.insuranceIncidentRepository.findByIncidentType(InsuranceIncidentType.DEFINITIVE_VOLUNTARY_CANCELLATION);
            } else {
                incident = this.insuranceIncidentRepository.findByIncidentType(InsuranceIncidentType.BAD_SALE_CANCELLATION);
                if (incident == null) {
                    throw new InsuranceIncidentNotFoundException(InsuranceIncidentType.DEFINITIVE_CANCELLATION_DEFAULT.name());
                }
            }
            if (incident == null) {
                throw new InsuranceIncidentNotFoundException(InsuranceIncidentType.DEFINITIVE_CANCELLATION_DEFAULT.name());
            }

            BigDecimal cumulative = BigDecimal.ZERO;
            cumulative = processInsuranceChargeCancellation(cumulative, loan, loanCharge, cancelInsuranceInstallmentData, false);

            InsuranceIncidentNoveltyNews insuranceIncidentNoveltyNews = InsuranceIncidentNoveltyNews.instance(loan, loanCharge,
                    cancelInsuranceInstallmentData.installment(), incident, cancellationDate, cumulative);

            this.insuranceIncidentNoveltyNewsRepository.saveAndFlush(insuranceIncidentNoveltyNews);
            saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        }

        return new CommandProcessingResultBuilder().withEntityId(loan.getId()).build();

    }

    private BigDecimal processInsuranceChargeCancellation(BigDecimal cumulative, Loan loan, LoanCharge loanCharge,
            DefaultOrCancelInsuranceInstallmentData data, boolean isForeClosure) {
        List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments().stream()
                .sorted(Comparator.comparingInt(LoanRepaymentScheduleInstallment::getInstallmentNumber)).toList();

        for (LoanRepaymentScheduleInstallment installment : installments) {
            if (installment.getInstallmentNumber().compareTo(data.installment()) > -1) {
                for (LoanInstallmentCharge installmentCharge : installment.getInstallmentCharges()) {
                    if (Objects.equals(installmentCharge.getLoanCharge().getId(), data.loanChargeId())) {
                        if (installment.getInstallmentNumber().compareTo(data.installment()) == 0) {
                            if (!isForeClosure) {
                                installmentCharge.getLoanCharge().setDefaultFromInstallment(data.installment());
                            }
                            if (!isForeClosure && installmentCharge.getAmountPaid(loan.getCurrency()).isGreaterThanZero()) {
                                // First default installment could have partially paid amount
                                installmentCharge.getLoanCharge().setPartialAmountPaidInFirstDefaultInstallment(
                                        installmentCharge.getAmountPaid(loan.getCurrency()).getAmount());
                            }
                        }
                        cumulative = cumulative.add(installmentCharge.getAmountOutstanding());
                        if (!isForeClosure) {
                            installment.adjustFeeChargePortion(Money.of(loan.getCurrency(), installmentCharge.getAmountOutstanding()));
                            installmentCharge.adjustChargeAmount(Money.of(loan.getCurrency(), installmentCharge.getAmountOutstanding()));
                        }
                    }
                }
            }
        }
        return cumulative;
    }

    @Transactional
    @Override
    public CommandProcessingResult excludeLoanFromReclaim(final Long loanId, final JsonCommand command) {
        Loan loan = this.loanAssembler.assembleFrom(loanId);
        String claimType = command.stringValueOfParameterNamed("claimType");
        if (claimType.equals("guarantor")) {
            loan.setExcludedForAvalClaim(claimType);
        } else if (claimType.equals("insurance")) {
            loan.setExcludedForInsuranceClaim(claimType);
        } else {
            loan.setExcludedForCastigadoClaim(claimType);
        }
        this.loanRepositoryWrapper.saveAndFlush(loan);
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withEntityExternalId(loan.getExternalId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .build();

    }

    @Override
    @Transactional
    public CommandProcessingResult claimLoan(final Long loanId, final JsonCommand command) {
        final String json = command.json();
        final JsonElement element = fromApiJsonHelper.parse(json);
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        final LocalDate transactionDate = this.fromApiJsonHelper.extractLocalDateNamed(LoanApiConstants.transactionDateParamName, element);
        final String claimType = this.fromApiJsonHelper.extractStringNamed("claimType", element);
        final ExternalId externalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);
        this.loanEventApiJsonValidator.validateLoanClaim(command.json());
        final Map<String, Object> changes = new LinkedHashMap<>();
        // Got changed to match with the rest of the APIs
        changes.put("dateFormat", command.dateFormat());
        changes.put("transactionDate", command.stringValueOfParameterNamed(LoanApiConstants.transactionDateParamName));
        changes.put("claimType", claimType);

        loan.setClaimType(claimType);
        loan.setClaimDate(transactionDate);
        LoanTransaction foreclosureTransaction = this.loanAccountDomainService.claimLoan(loan, transactionDate, externalId, changes);

        BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepositoryWrapper
                .getSingleBlockingReasonSettingByReason("Reclamación avaladora/aseguradora", BlockLevel.CREDIT.toString());

        loanBlockWritePlatformService.blockLoan(loan.getId(), blockingReasonSetting, "Reclamación avaladora/aseguradora",
                DateUtils.getLocalDateOfTenant());
        this.loanRepository.saveAndFlush(loan);

        final CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder();
        return commandProcessingResultBuilder //
                .withLoanId(loanId) //
                .withEntityId(foreclosureTransaction.getId()) //
                .withEntityExternalId(foreclosureTransaction.getExternalId()) //
                .with(changes) //
                .build();
    }

    private void validateRepaymentDate(LocalDate transactionDate) {
        // check the configuration if backdated transactions are allowed , if yes , do nothing , else , validate that
        // transaction date is not before current date

        if (this.configurationDomainService.allowPaymentsWithPreviousDateEnabled()) {
            return;
        }

        LocalDate currentDate = DateUtils.getLocalDateOfTenant();

        if (DateUtils.isBefore(transactionDate, currentDate)) {
            final String errorMessage = "The transaction date cannot be in the past.";
            throw new GeneralPlatformDomainRuleException("error.msg.loan.transaction.cannot.be.a.past.date", errorMessage, transactionDate);
        }

    }

    @Override
    public void cleanUpLoans() {
        final String sql = "SELECT loan_id from tmp_loan_cleanup WHERE processed = false";
        final List<Long> loanIds = this.jdbcTemplate.queryForList(sql, Long.class);
        for (final Long loanId : loanIds) {
            try {
                log.info("Started clean up for Loan with ID: {}", loanId);
                this.loanAccountDomainService.cleanUpLoan(loanId);
                log.info("Loan with ID: {} cleaned up successfully", loanId);
            } catch (final Exception ex) {
                log.error("Loan clean up failed for Loan ID: " + loanId, ex);
            }
        }
    }

    @Transactional
    @Override
    public JsonArray regenerateLoanSchedule(String apiRequestBodyAsJson) {
        final JsonElement element = fromApiJsonHelper.parse(apiRequestBodyAsJson);
        JsonArray loanIds = this.fromApiJsonHelper.extractJsonArrayNamed("loanIds", element);
        JsonArray failedLoans = new JsonArray();

        for (JsonElement loansList : loanIds) {
            long loanId = loansList.getAsLong();
            try {
                // Delete existing CustomChargeHonorarioMaps for this loan
                this.loanAccountDomainService.cleanUpLoan(loanId);
                log.info("Successfully regenerated loan schedule for loan ID: {}", loanId);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Failed to regenerate loan schedule for loan ID: {}", loanId, e);
                failedLoans.add(loansList);
            }
        }

        return failedLoans;
    }

    public Long regenerateLoanRepaymentSchedule(long loanId) {

        return loanId;
    }
}
