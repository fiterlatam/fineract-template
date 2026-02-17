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

import static org.apache.fineract.portfolio.delinquency.validator.DelinquencyActionParameters.ACTION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockDTO;
import org.apache.fineract.custom.portfolio.blockaccounts.service.LoanAccountBlockReadPlatformService;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.domain.CustomChargeHonorarioMapRepository;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockLevel;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingEnum;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingsRepositoryWrapper;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
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
import org.apache.fineract.portfolio.collectionsheet.command.CollectionSheetBulkDisbursalCommand;
import org.apache.fineract.portfolio.collectionsheet.command.CollectionSheetBulkRepaymentCommand;
import org.apache.fineract.portfolio.collectionsheet.command.SingleDisbursalCommand;
import org.apache.fineract.portfolio.collectionsheet.command.SingleRepaymentCommand;
import org.apache.fineract.portfolio.delinquency.service.DelinquencyReadPlatformService;
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
import org.apache.fineract.portfolio.loanaccount.exception.DateMismatchException;
import org.apache.fineract.portfolio.loanaccount.exception.ExceedingTrancheCountException;
import org.apache.fineract.portfolio.loanaccount.exception.InvalidLoanTransactionTypeException;
import org.apache.fineract.portfolio.loanaccount.exception.InvalidPaidInAdvanceAmountException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanDisbursalException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanForeclosureException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanMultiDisbursementException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanNotFoundException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanOfficerAssignmentException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanOfficerUnassignmentException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanTransactionNotFoundException;
import org.apache.fineract.portfolio.loanaccount.exception.MultiDisbursementDataNotAllowedException;
import org.apache.fineract.portfolio.loanaccount.exception.MultiDisbursementDataRequiredException;
import org.apache.fineract.portfolio.loanaccount.guarantor.service.GuarantorDomainService;
import org.apache.fineract.portfolio.loanaccount.invoice.data.ClasificacionConceptosData;
import org.apache.fineract.portfolio.loanaccount.invoice.data.LoanDocumentData;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicMensualRepository;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicaMensual;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.LoanDocumentConcept;
import org.apache.fineract.portfolio.loanaccount.jobs.facturaelectronicamensual.FacturaElectronicaMensualTasklet;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanApplicationTerms;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGeneratorFactory;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModelPeriod;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleProcessingType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.PrincipalInterest;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.RescheduleLoansApiConstants;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.LoanRescheduleRequestData;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequestRepository;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.service.LoanRescheduleRequestReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.service.LoanRescheduleRequestWritePlatformService;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class LoanWritePlatformServiceJpaRepositoryImpl implements LoanWritePlatformService {

    public static final String ERROR_MESSAGE_DISBURSEMENT_WHOULD_BE_DONE_USING_MENU = "error.msg.loan.undo.disbursement.using.menu";
    public static final String ERROR_MESSAGE_DISBURSEMENT_WHOULD_BE_ACTIONED_USING_TRANSACTIONS_TAB = "error.msg.loan.undo.disbursement.using.tab";
    private static final String LOAN_TRANSACTION_AMOUNT = "transactionAmount";
    private static final String CASTIGADO_PARAM = "castigado";
    private static final String TRANSACTION_DATE_PARAM = "transactionDate";
    private static final String BANCOS_PARAM = "Bancos";
    private static final String DATE_FORMAT_PARAM = "dateFormat";
    private static final String EXTERNAL_ID_PARAM = "externalId";
    private static final String CLAIM_TYPE_PARAM = "claimType";
    private static final String EXTERNAL_ID_UNIQUE_PARAM = "external_id_unique";
    private static final String ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_BLANK = "validation.msg.channel.is.blank";
    private static final String ERROR_MESSAGE_VALIDATION_CHANNEL_IS_BLANK = "Channel is blank";
    private static final String ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_NOT_ALLOWED = "validation.msg.channel.not.allowed";
    private static final String ERROR_MESSAGE_VALIDATION_CHANNEL_IS_NOT_ALLOWED = "Channel is not allowed";
    private static final String ERROR_MESSAGE_LABEL_CHARGE_BACK_NOT_ALLOWED = "error.msg.loan.chargeback.operation.not.allowed";
    private static final String ERROR_MESSAGE_LABEL_VALUE_MUST_BE_UNIQUE = "value.must.be.unique";
    private static final String ERROR_MESSAGE_LABEL_VALIDATION_ERRORS_EXIST = "validation.msg.validation.errors.exist";
    private static final String ERROR_MESSAGE_VALIDATION_ERRORS_EXIST = "Validation errors exist.";
    private static final String LOCALE_PARAM = "locale";
    private static final String NEW_INTEREST_RATE_PARAM = "newInterestRate";
    private static final String RESCHEDULE_REASON_ID_PARAM = "rescheduleReasonId";
    private static final String RESCHEDULE_REASON_COMMENT_PARAM = "rescheduleReasonComment";
    private static final String RESCHEDULE_FROM_DATE_PARAM = "rescheduleFromDate";
    private static final String WRITE_OFF_REASON_ID_PARAM = "writeoffReasonId";
    private static final String ERROR_MESSAGE_LABEL_CHANNEL_NOT_FOUND = "validation.msg.channel.not.found";
    private static final String ERROR_MESSAGE_CHANNEL_NOT_FOUND = "Channel not found";
    private static final String ERROR_MESSAGE_LABEL_CHANNEL_NOT_ACTIVE = "validation.msg.channel.not.active";
    private static final String ERROR_MESSAGE_CHANNEL_NOT_ACTIVE = "Channel is not active";
    private static final String LOAN_ID_PARAM = "loanId";
    private static final String MAXIMUM_LEGAL_RATE_RECALCULATION = "Recalcular la tasa de interés al máximo legal";
    private static final String CHANNEL_NAME_PARAM = "channelName";
    private static final String ACTUAL_DISBURSEMENT_DATE_PARAM = "actualDisbursementDate";
    private static final String ERROR_MESSAGE_LABEL_TRANSACTION_DATE_CANNOT_BE_EARLIER_THAN_CHARGE_OFF_DATE = "error.msg.transaction.date.cannot.be.earlier.than.charge.off.date";
    private static final String ERROR_MESSAGE_LABEL_CHARGE_OFF_LOAN = "error.msg.loan.is.charged.off";
    private static final String PAYMENT_TYPE_ID_PARAM = "paymentTypeId";
    private static final String LOAN_TRANSACTION_DESCRIPTION = "Loan transaction:";
    private static final String LOAN_DESCRIPTION = "Loan: ";
    private static final String SUBMITTED_ON_DATE = "submittedOnDate";
    private static final String APPROVED_DATE_PARAM = "approvedOnDate";
    private static final String GRACE_ON_PRINCIPAL_PARAM = "graceOnPrincipal";
    private static final String ADJUSTED_DUE_DATE_PARAM = "adjustedDueDate";
    private static final String EXTRA_TERMS = "extraTerms";

    // CUTOFF_DAYS: Number of days before the due date to determine if a new disbursement
    // should be included in the next installment or trigger a new set of installments.
    private static final int CUTOFF_DAYS = 15;

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
    private final BlockingReasonSettingsRepositoryWrapper loanBlockingReasonRepository;
    private final InsuranceIncidentRepository insuranceIncidentRepository;
    private final InsuranceIncidentNoveltyNewsRepository insuranceIncidentNoveltyNewsRepository;
    private final LoanScheduleGeneratorFactory loanScheduleFactory;
    private final BlockingReasonSettingsRepositoryWrapper blockingReasonSettingsRepositoryWrapper;
    private final FacturaElectronicMensualRepository facturaElectronicMensualRepository;
    private final LoanProductParameterizationRepository productParameterizationRepository;
    private final CustomChargeHonorarioMapRepository customChargeHonorarioMapRepository;
    private final DelinquencyReadPlatformService delinquencyReadPlatformService;
    private final LoanDisbursementDetailsRepository loanDisbursementDetailsRepository;
    private final LoanRescheduleRequestWritePlatformService loanRescheduleRequestWritePlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final LoanRescheduleRequestRepository loanRescheduleRequestRepository;
    private final FirstPaymentDateAdjustmentService firstPaymentDateAdjustmentService;
    private final LoanOverpaymentValidationService loanOverpaymentValidationService;
    private final LoanAdvancedPaymentValidationService loanAdvancedPaymentValidationService;
    private final LoanAccountBlockReadPlatformService loanAccountBlockReadPlatformService;

    @PostConstruct
    public void registerForNotification() {
        businessEventNotifierService.addPostBusinessEventListener(LoanDisbursalBusinessEvent.class, new DisbursementEventListener());
        businessEventNotifierService.addPostBusinessEventListener((LoanInvoiceGenerationPostBusinessEvent.class),
                new LoanInvoiceGenerationPostBusinessEventListener());
        businessEventNotifierService.addPostBusinessEventListener((LoanCreditNoteBusinessEvent.class),
                new LoanCreditNoteGenerationPostBusinessEventListener());
    }

    @SuppressWarnings({ "squid:S6809" })
    @Transactional
    @Override
    public CommandProcessingResult disburseGLIMLoan(final Long loanId, final JsonCommand command) {
        final Long parentLoanId = loanId;
        GroupLoanIndividualMonitoringAccount parentLoan = glimRepository.findById(parentLoanId).orElseThrow();
        List<Loan> childLoans = this.loanRepository.findByGlimId(loanId);
        CommandProcessingResult result = null;
        int count = 0;
        for (Loan loan : childLoans) {
            result = this.disburseLoan(loan.getId(), command, false);
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
            throw new GeneralPlatformDomainRuleException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_BLANK,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_CHANNEL_IS_BLANK);
        }
        final ChannelData channelData = this.channelReadWritePlatformService.findByNameType(channelName,
                ChannelType.DISBURSEMENT.getValue());
        if (channelData == null) {
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHANNEL_NOT_FOUND,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_CHANNEL_NOT_FOUND, channelName);
        }
        if (Boolean.FALSE.equals(channelData.getActive())) {
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHANNEL_NOT_ACTIVE,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_CHANNEL_NOT_ACTIVE, channelName);
        }
        if (ChannelType.DISBURSEMENT.getValue().longValue() != channelData.getChannelType().getId()) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.disbursement", "Channel is not disbursement channel",
                    channelName);
        }
    }

    @SuppressWarnings({ "squid:S3776" })
    @Transactional
    @Override
    public CommandProcessingResult disburseLoan(final Long loanId, final JsonCommand command, Boolean isAccountTransfer) {

        final AppUser currentUser = getAppUserIfPresent();
        final LocalDate actualDisbursementDate = this.fromApiJsonHelper
                .extractLocalDateNamed(LoanEventApiJsonValidator.ACTUAL_DISBURSEMENT_DATE_PARAM, command.parsedJson().getAsJsonObject());

        this.loanEventApiJsonValidator.validateDisbursement(command.json(), isAccountTransfer);
        LoanAccountBlockDTO accountBlockDTO = this.loanAccountBlockReadPlatformService.retrieveByLoanIdWithoutException(loanId);

        if (accountBlockDTO != null) {
            boolean canDisbursement = actualDisbursementDate.isBefore(accountBlockDTO.getApplicationDate());
            if (!canDisbursement) {
                final String message = String.format(" Cannot be disbursed, loan account is blocked by %s",
                        accountBlockDTO.getBlockingReasonName());
                throw new GeneralPlatformDomainRuleException(message, "Loan account is blocked.");
            }
            log.info("DisburseLoan: Account blocked but the effective date has not yet come into effect. {}",
                    accountBlockDTO.getApplicationDate());
        }

        Boolean isWriteoffPunish = command.booleanObjectValueOfParameterNamed("isWriteoffPunish");

        if (isWriteoffPunish == null) {
            isWriteoffPunish = false;
        }
        if (!isWriteoffPunish) {
            String channelName = command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.CHANNEL_NAME_PARAM);
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
        // Check and adjust first payment date if needed
        firstPaymentDateAdjustmentService.adjustFirstPaymentDateIfNeeded(loan, actualDisbursementDate);

        BigDecimal disbursement = command.bigDecimalValueOfParameterDefaultToZeroIfNull("transactionAmount");
        DisbursementCutoffContext.setDisbursementAmount(Money.of(loan.getCurrency(), disbursement));

        final LoanProduct loanProduct = loan.loanProduct();
        if (loan.isTopup() && !loanProduct.getCustomAllowRestructure()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.product.does.not.allow.topup",
                    "Loan product does not allow topup.");
        }

        // Fail fast if client/group is not active or actual loan status disallows disbursal
        checkClientOrGroupActive(loan);

        // Fail fast if cupo is not enough
        if (!loan.isMigratedLoan()) {
            checkCupo(loan);
        }

        final LocalDate nextPossibleRepaymentDate = loan.getNextPossibleRepaymentDateForRescheduling();
        final LocalDate rescheduledRepaymentDate = command.localDateValueOfParameterNamed("adjustRepaymentDate");

        // Check if loan request is a Credito Rotativo and if so, apply its business rules
        checkCreditoRotativo(command, loan, actualDisbursementDate);

        // validate if the loan product allows creation and disbursement
        if (Boolean.FALSE.equals(loan.loanProduct().getCustomAllowCreateOrDisburse())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.product.does.not.allow.creation.nor.disbursement",
                    "Loan product does not allow creation and disbursement.");
        }

        if (loan.isChargedOff() && DateUtils.isBefore(actualDisbursementDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_TRANSACTION_DATE_CANNOT_BE_EARLIER_THAN_CHARGE_OFF_DATE,
                    "Loan ID: " + loanId
                            + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the this loan",
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
        if (paymentDetail != null && paymentDetail.getPaymentType() != null
                && Boolean.TRUE.equals(paymentDetail.getPaymentType().getIsCashPayment())) {
            BigDecimal transactionAmount = command
                    .bigDecimalValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT);
            this.cashierTransactionDataValidator.validateOnLoanDisbursal(currentUser, loan.getCurrencyCode(), transactionAmount);
        }
        final boolean isPaymentTypeApplicableForDisbursementCharge = configurationDomainService
                .isPaymentTypeApplicableForDisbursementCharge();

        // Recalculate first repayment date based in actual disbursement date.
        updateLoanCounters(loan, actualDisbursementDate);
        // Check and adjust first payment date if needed
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
                final BigDecimal firstDisbursalAmount = loan.getFirstDisbursalAmount();
                if ((loanToClose.claimType() == null
                        || !loanToClose.claimType().equals(LoanWritePlatformServiceJpaRepositoryImpl.CASTIGADO_PARAM))
                        && loanOutstanding.compareTo(firstDisbursalAmount) > 0) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.amount.less.than.outstanding.of.loan.to.be.closed",
                            "Topup loan amount should be greater than outstanding amount of loan to be closed.");
                }

                if (loanToClose.claimType() == null
                        || !loanToClose.claimType().equals(LoanWritePlatformServiceJpaRepositoryImpl.CASTIGADO_PARAM)) {
                    // in case of castigado claim new loan will be of 1 installment and equal to outstanding amount of
                    // the existing loan
                    amountToDisburse = disburseAmount.minus(loanOutstanding);
                }
                if (!LoanWritePlatformServiceJpaRepositoryImpl.CASTIGADO_PARAM.equalsIgnoreCase(loanToClose.claimType())) { // Ensure
                                                                                                                            // the
                                                                                                                            // loan
                                                                                                                            // is
                                                                                                                            // not
                                                                                                                            // in
                                                                                                                            // castigado
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
            if (loan.getLoanProduct().isRevolvingLoanProduct()) {
                createAndSaveLoanScheduleArchiveForCreditoRotativo(loan, scheduleGeneratorDTO);
            }
            regenerateScheduleOnDisbursement(command, loan, recalculateSchedule, scheduleGeneratorDTO, nextPossibleRepaymentDate,
                    rescheduledRepaymentDate);
            // Farooq 25th June 2024 - Ensured that Loan Schedule Archive is always created
            if (!loan.getLoanProduct().getName().equalsIgnoreCase(LoanProductType.CREDITO_ROTATIVO.getCode())) {
                createAndSaveLoanScheduleArchive(loan, scheduleGeneratorDTO);
            }

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
                final LoanTransaction loanTransaction = LoanTransaction.accrueTransaction(loan, loan.getOffice(), actualDisbursementDate,
                        loanCharge.amount(), null, loanCharge.amount(), null, externalIdFactory.create());
                final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(loanTransaction, loanCharge, loanCharge.amount(), null);
                loanTransaction.getLoanChargesPaid().add(loanChargePaidBy);
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

        if (Boolean.FALSE.equals(isAccountTransfer)) {
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

        for (LoanTransaction transaction : loan.retrieveListOfAccrualTransactions()) {
            long days = loan.getRepaymentScheduleInstallmentsIgnoringTotalGrace().get(0).getDueDate()
                    .until(transaction.getTransactionDate(), ChronoUnit.DAYS);
            if (days >= minimumDaysInArrearsToSuspendLoanAccount) {
                transaction.markAsOccurredOnSuspendedAccount();
            }
        }
        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
        for (LoanTransaction transaction : loan.retrieveListOfAccrualTransactions()) {
            long days = loan.getRepaymentScheduleInstallmentsIgnoringTotalGrace().get(0).getDueDate()
                    .until(transaction.getTransactionDate(), ChronoUnit.DAYS);
            if (days >= minimumDaysInArrearsToSuspendLoanAccount) {
                transaction.markAsOccurredOnSuspendedAccount();
            }
        }
        // Clean up the DisbursementCutoffContext after disbursement processing
        DisbursementCutoffContext.clear();
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

    private void createAndSaveLoanScheduleArchiveForCreditoRotativo(final Loan loan, ScheduleGeneratorDTO scheduleGeneratorDTO) {
        LoanRescheduleRequest loanRescheduleRequest = null;
        this.loanScheduleHistoryWritePlatformService.createAndSaveLoanScheduleArchive(loan.getRepaymentScheduleInstallments(), loan,
                loanRescheduleRequest);
    }

    /**
     * create standing instruction for disbursed loan
     *
     * @param loan
     *            the disbursed loan
     **/
    private void createStandingInstruction(Loan loan) {

        if (Boolean.TRUE.equals(loan.shouldCreateStandingInstructionAtDisbursement())) {
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

    @Override
    public Loan saveAndFlushLoanWithDataIntegrityViolationChecks(final Loan loan) {
        try {
            loanRepaymentScheduleInstallmentRepository.saveAll(loan.getRepaymentScheduleInstallments());
            return this.loanRepositoryWrapper.saveAndFlush(loan);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loantransaction");
            if (realCause.getMessage().toLowerCase().contains(LoanWritePlatformServiceJpaRepositoryImpl.EXTERNAL_ID_UNIQUE_PARAM)) {
                baseDataValidator.reset().parameter(LoanApiConstants.externalIdParameterName)
                        .failWithCode(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALUE_MUST_BE_UNIQUE);
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_ERRORS_EXIST,
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_ERRORS_EXIST, dataValidationErrors, e);
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
            if (realCause.getMessage().toLowerCase().contains(LoanWritePlatformServiceJpaRepositoryImpl.EXTERNAL_ID_UNIQUE_PARAM)) {
                baseDataValidator.reset().parameter(LoanApiConstants.externalIdParameterName)
                        .failWithCode(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALUE_MUST_BE_UNIQUE);
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_ERRORS_EXIST,
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_ERRORS_EXIST, dataValidationErrors, e);
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
            if (realCause.getMessage().toLowerCase().contains(LoanWritePlatformServiceJpaRepositoryImpl.EXTERNAL_ID_UNIQUE_PARAM)) {
                baseDataValidator.reset().parameter(LoanApiConstants.externalIdParameterName)
                        .failWithCode(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALUE_MUST_BE_UNIQUE);
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_ERRORS_EXIST,
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_ERRORS_EXIST, dataValidationErrors, e);
            }
        }
    }

    @SuppressWarnings({ "squid:S3776" })
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
            final LocalDate actualDisbursementDate = command
                    .localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.ACTUAL_DISBURSEMENT_DATE_PARAM);

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
            loan.validateAccountStatus(LoanEvent.LOAN_DISBURSED);
            updateLoanCounters(loan, actualDisbursementDate);
            boolean canDisburse = loan.canDisburse(actualDisbursementDate);
            ChangedTransactionDetail changedTransactionDetail = null;
            if (canDisburse) {
                Money amountBeforeAdjust = loan.getPrincipal();
                Money disburseAmount = loan.adjustDisburseAmount(command, actualDisbursementDate);
                boolean recalculateSchedule = amountBeforeAdjust.isNotEqualTo(loan.getPrincipal());
                final ExternalId txnExternalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);
                if (Boolean.TRUE.equals(isAccountTransfer)) {
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

    @SuppressWarnings({ "squid:S6809" })
    @Transactional
    @Override
    public CommandProcessingResult undoGLIMLoanDisbursal(final Long loanId, final JsonCommand command) {
        final Long parentLoanId = loanId;
        GroupLoanIndividualMonitoringAccount parentLoan = glimRepository.findById(parentLoanId).orElseThrow();
        List<Loan> childLoans = this.loanRepository.findByGlimId(loanId);
        CommandProcessingResult result = null;
        int count = 0;
        for (Loan loan : childLoans) {
            result = this.undoLoanDisbursal(loan.getId(), command);
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
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHARGE_OFF_LOAN,
                    "Undo Loan: " + loanId + " disbursement is not allowed. Loan Account is Charged-off", loanId);
        }

        // check if there is any active repayment transaction and abort if exists
        if (loan.getLoanTransactions().stream().filter(rep -> rep.isRepayment()).filter(notRev -> !notRev.isReversed()).count() > 0) {
            throw new GeneralPlatformDomainRuleException("validation.msg.cannot.undo.disbursal.with.active.transaction",
                    "Undo Disbursal is not allowed for loans with existing transactions. You may undo the transactions first.");
        }

        if (loan.getLoanProduct().isMultiDisburseLoan()) {
            // Check if there are more than 1 active disbursal transactions. If YEST, throw an exception
            if (loan.getLoanTransactions().stream().filter(to -> LoanTransactionType.DISBURSEMENT.equals(to.getTypeOf()))
                    .filter(nr -> Boolean.FALSE.equals(nr.isReversed())).count() > 1) {

                throw new LoanDisbursalException(ERROR_MESSAGE_DISBURSEMENT_WHOULD_BE_ACTIONED_USING_TRANSACTIONS_TAB,
                        "Use transaction´s tab to undo disbursements after 1st disbursal.", loanId);
            }
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

    @SuppressWarnings({ "squid:S6809" })
    @Transactional
    @Override
    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    public CommandProcessingResult makeGLIMLoanRepayment(final Long loanId, final JsonCommand command) {
        JsonArray repayments = command.arrayOfParameterNamed("formDataArray");
        JsonCommand childCommand;
        CommandProcessingResult result = null;
        JsonObject jsonObject;

        Long[] childLoanId = new Long[repayments.size()];
        for (int i = 0; i < repayments.size(); i++) {
            jsonObject = repayments.get(i).getAsJsonObject();
            log.debug("{}", jsonObject.toString());
            childLoanId[i] = jsonObject.get(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_ID_PARAM).getAsLong();
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
            final JsonCommand command, final boolean isRecoveryRepayment, LoanChargeWritePlatformService loanChargeWritePlatformService) {
        final String chargeRefundChargeType = null;
        BigDecimal cumulativeHonoFee = BigDecimal.ZERO;
        BigDecimal cumulativeVatFee = BigDecimal.ZERO;
        return makeLoanRepaymentWithChargeRefundChargeType(repaymentTransactionType, loanId, command, isRecoveryRepayment,
                chargeRefundChargeType, loanChargeWritePlatformService);
    }

    @Transactional
    @Override
    public CommandProcessingResult makeLoanRepayment(final LoanTransactionType repaymentTransactionType, final Long loanId,
            final JsonCommand command, final boolean isRecoveryRepayment) {
        final String chargeRefundChargeType = null;
        BigDecimal cumulativeHonoFee = BigDecimal.ZERO;
        BigDecimal cumulativeVatFee = BigDecimal.ZERO;
        return makeLoanRepaymentWithChargeRefundChargeType(repaymentTransactionType, loanId, command, isRecoveryRepayment,
                chargeRefundChargeType);
    }

    @SuppressWarnings({ "squid:S3776" })
    @Transactional
    @Override
    public CommandProcessingResult makeLoanRepaymentWithChargeRefundChargeType(final LoanTransactionType repaymentTransactionType,
            final Long loanId, final JsonCommand command, final boolean isRecoveryRepayment, final String chargeRefundChargeType) {
        return makeLoanRepaymentWithChargeRefundChargeType(repaymentTransactionType, loanId, command, isRecoveryRepayment,
                chargeRefundChargeType, null);
    }

    @SuppressWarnings({ "squid:S3776" })
    private CommandProcessingResult makeLoanRepaymentWithChargeRefundChargeType(final LoanTransactionType repaymentTransactionType,
            final Long loanId, final JsonCommand command, final boolean isRecoveryRepayment, final String chargeRefundChargeType,
            final LoanChargeWritePlatformService loanChargeWritePlatformService) {
        this.loanUtilService.validateRepaymentTransactionType(repaymentTransactionType);
        this.loanEventApiJsonValidator.validateNewRepaymentTransaction(command.json());
        String channelName = command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.CHANNEL_NAME_PARAM);
        if (StringUtils.isBlank(channelName)) {
            channelName = this.platformSecurityContext.getApiRequestChannel();
        }
        final LocalDate transactionDate = command
                .localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM);
        final BigDecimal transactionAmount = command
                .bigDecimalValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT);
        final ExternalId txnExternalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        validateRepaymentDate(transactionDate);

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, command.locale());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.PAYMENT_TYPE_ID_PARAM,
                command.longValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.PAYMENT_TYPE_ID_PARAM));
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
        if (Boolean.FALSE.equals(loanProduct.getCustomAllowCollections())) {
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
        if (channelData.getName().equalsIgnoreCase(LoanWritePlatformServiceJpaRepositoryImpl.BANCOS_PARAM) && repaymentBankId == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.bank.is.required.for.bank.channel",
                    "Bank is mandatory for bank channel", LoanWritePlatformServiceJpaRepositoryImpl.BANCOS_PARAM);
        }

        // Validate all overpayment rules (channel-based and product-specific)
        LoanTransactionData foreclosureData = this.loanReadPlatformService.retrieveLoanForeclosureTemplate(loanId, transactionDate, false);
        this.loanOverpaymentValidationService.validateOverpayment(loan, transactionAmount, foreclosureData.getAmount(), channelData);

        // Validate advanced payment rules (product-specific)
        this.loanAdvancedPaymentValidationService.validateAdvancedPayment(loan, transactionAmount);

        final Long channelId = channelData.getId();
        changes.put("channelId", channelId);
        changes.put("channelHash", channelData.getHash());
        changes.put("paymentBankId", repaymentBankId);

        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);
        final Boolean isHolidayValidationDone = false;
        final HolidayDetailDTO holidayDetailDto = null;
        boolean isAccountTransfer = false;

        String loanScheduleProcessingType = command.stringValueOfParameterNamed("transactionProcessingStrategy");
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
        boolean reduceTerm = command.booleanPrimitiveValueOfParameterNamed("reduceTerm");
        loan.setRecalculateEMI(recalculateEMI);
        if (recalculateEMI && reduceTerm) {
            throw new GeneralPlatformDomainRuleException("validation.msg.loan.repayment.both.reduce.options.not.allowed",
                    "Both reduceInstallmentAmount and reduceTerm cannot be true at the same time. Please choose only one option.");
        }
        // After validation, set the reschedule strategy based on reduceTerm
        if (reduceTerm) {
            loan.setReduceTerm(true);
        }

        LoanTransaction loanTransaction = this.loanAccountDomainService.makeRepayment(repaymentTransactionType, loan, transactionDate,
                transactionAmount, paymentDetail, noteText, txnExternalId, isRecoveryRepayment, chargeRefundChargeType, isAccountTransfer,
                holidayDetailDto, isHolidayValidationDone);
        loan = loanTransaction.getLoan();

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

        // Add GAC charge realculation here
        if (Objects.nonNull(loanChargeWritePlatformService)) {
            loanChargeWritePlatformService.applyGACChargeForOverdueLoanAfterRepaymentOrReversal(loan, transactionDate);
        }

        if (loan.getStatus().isClosed()) {
            createCancellationNoveltyNews(loan, loan.getClosedOnDate());
        }

        if (loan.getStatus().isOverpaid() && !loan.isRevolvingLoan()) {
            final PortfolioAccountData linkedSavingsAccountData = this.accountAssociationsReadPlatformService
                    .retriveLoanLinkedAssociation(loanId);
            final SavingsAccount fromSavingsAccount = null;
            final boolean isRegularTransaction = true;
            final boolean isExceptionForBalanceCheck = false;
            final Locale locale = command.extractLocale();
            final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(locale);

            if (linkedSavingsAccountData == null) {
                // a linked savings account is mandatory
                throw new GeneralPlatformDomainRuleException("error.msg.loan.channel.repayment.overpaid.linked.savings.account.not.present",
                        "A linked savings account need to be set for non-revolving overpaid loans");
            }

            Money overPaidMoney = loan.getTotalOverpaidAsMoney();

            final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(transactionDate, overPaidMoney.getAmount(),
                    PortfolioAccountType.LOAN, PortfolioAccountType.SAVINGS, loan.getId(), linkedSavingsAccountData.getId(),
                    "From loan " + loan.getAccountNumber() + " to savings " + linkedSavingsAccountData.getAccountNo()
                            + " Overpaid transfer ",
                    locale, fmt, null, null, AccountTransferType.ACCOUNT_TRANSFER.getValue(), null, null,
                    LoanTransactionType.REFUND.getValue(), null, null, ExternalId.empty(), null, null, fromSavingsAccount,
                    isRegularTransaction, isExceptionForBalanceCheck);
            this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
        }

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
            throw new GeneralPlatformDomainRuleException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_BLANK,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_CHANNEL_IS_BLANK);
        }
        final ChannelData channelData = this.channelReadWritePlatformService.findByNameType(channelName, ChannelType.REPAYMENT.getValue());
        if (channelData == null) {
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHANNEL_NOT_FOUND,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_CHANNEL_NOT_FOUND, channelName);
        }
        if (Boolean.FALSE.equals(channelData.getActive())) {
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHANNEL_NOT_ACTIVE,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_CHANNEL_NOT_ACTIVE, channelName);
        }
        if (ChannelType.REPAYMENT.getValue().longValue() != channelData.getChannelType().getId()) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.repayment", "Channel is not disbursement repayment",
                    channelName);
        }
        final List<Channel> repaymentChannels = loanProduct.getRepaymentChannels();
        if (CollectionUtils.isNotEmpty(repaymentChannels)) {
            final Long channelId = channelData.getId();
            if (repaymentChannels.stream().noneMatch(repaymentChannel -> repaymentChannel.getId().equals(channelId))) {
                throw new GeneralPlatformDomainRuleException(
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_NOT_ALLOWED,
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_CHANNEL_IS_NOT_ALLOWED, channelName);
            }
        } else {
            throw new GeneralPlatformDomainRuleException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_NOT_ALLOWED,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_CHANNEL_IS_NOT_ALLOWED, channelName);
        }
        return channelData;
    }

    private ChannelData validateRepaymentChannelById(final Long repaymentChannelId, final LoanProduct loanProduct) {
        if (repaymentChannelId == null) {
            throw new GeneralPlatformDomainRuleException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_BLANK,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_CHANNEL_IS_BLANK);
        }
        final ChannelData channelData = this.channelReadWritePlatformService.findById(repaymentChannelId);
        if (channelData == null) {
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHANNEL_NOT_FOUND,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_CHANNEL_NOT_FOUND, repaymentChannelId);
        }
        if (Boolean.FALSE.equals(channelData.getActive())) {
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHANNEL_NOT_ACTIVE,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_CHANNEL_NOT_ACTIVE, repaymentChannelId);
        }
        if (ChannelType.REPAYMENT.getValue().longValue() != channelData.getChannelType().getId()) {
            throw new GeneralPlatformDomainRuleException("validation.msg.channel.not.repayment", "Channel is not disbursement repayment",
                    repaymentChannelId);
        }
        final List<Channel> repaymentChannels = loanProduct.getRepaymentChannels();
        if (CollectionUtils.isNotEmpty(repaymentChannels)) {
            final Long channelId = channelData.getId();
            if (repaymentChannels.stream().noneMatch(repaymentChannel -> repaymentChannel.getId().equals(channelId))) {
                throw new GeneralPlatformDomainRuleException(
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_NOT_ALLOWED,
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_CHANNEL_IS_NOT_ALLOWED, repaymentChannelId);
            }
        } else {
            throw new GeneralPlatformDomainRuleException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_NOT_ALLOWED,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_CHANNEL_IS_NOT_ALLOWED, repaymentChannelId);
        }
        return channelData;
    }

    @SuppressWarnings({ "squid:S3776" })
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

    @Transactional
    @Override
    public CommandProcessingResult adjustLoanTransaction(final Long loanId, final Long transactionId, final JsonCommand command,
            LoanChargeWritePlatformService loanChargeWritePlatformService) {
        final AppUser authenticatedUser = context.authenticatedUser();
        this.loanEventApiJsonValidator.validateTransaction(command.json());
        LoanTransaction transactionToAdjust = this.loanTransactionRepository.findByIdAndLoanId(command.entityId(), command.getLoanId())
                .orElseThrow(() -> new LoanTransactionNotFoundException(command.entityId(), command.getLoanId()));
        Loan loan = this.loanAssembler.assembleFrom(loanId);
        if (loan.getStatus().isClosed() && loan.getLoanSubStatus() != null
                && loan.getLoanSubStatus().equals(LoanSubStatus.FORECLOSED.getValue())) {
            final String defaultUserMessage = "The loan cannot reopened as it is foreclosed.";
            throw new LoanForeclosureException("loan.cannot.be.reopened.as.it.is.foreclosured", defaultUserMessage, loanId);
        }

        checkClientOrGroupActive(loan);

        checkIfProductAllowsCancelationOrReversal(loan);

        // Check if loan transaction typeOf == 1 (disbursal)
        if (LoanTransactionType.DISBURSEMENT.equals(transactionToAdjust.getTypeOf())) {
            final String defaultUserMessage = "Undo 1st disbursement should be done using loan´s MENU";
            LoanDisbursalException exception = new LoanDisbursalException(ERROR_MESSAGE_DISBURSEMENT_WHOULD_BE_DONE_USING_MENU,
                    defaultUserMessage, loanId);

            // For multi disbursal, we allow reversing all except 1st
            if (loan.getLoanProduct().isMultiDisburseLoan()) {
                // Check if there are more than 1 active disbursal transactions. If no, throw an exception
                Optional<LoanTransaction> ltopt = loan.getLoanTransactions().stream()
                        .filter(to -> LoanTransactionType.DISBURSEMENT.equals(to.getTypeOf()))
                        .filter(nr -> Boolean.FALSE.equals(nr.isReversed()))
                        .sorted(Comparator.comparing(LoanTransaction::getTransactionDate)).findFirst();

                if (ltopt.isPresent() && ltopt.get().getId().equals(transactionToAdjust.getId())) {
                    throw exception;
                }

            } else {
                // For other products, we don´t allow reversing through transaction tab. User need to use MENU
                throw exception;
            }
        }

        businessEventNotifierService.notifyPreBusinessEvent(
                new LoanAdjustTransactionBusinessEvent(new LoanAdjustTransactionBusinessEvent.Data(transactionToAdjust)));
        if (this.accountTransfersReadPlatformService.isAccountTransfer(transactionId, PortfolioAccountType.LOAN)) {
            throw new PlatformServiceUnavailableException("error.msg.loan.transfer.transaction.update.not.allowed",
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_DESCRIPTION + transactionId
                            + " update not allowed as it involves in account transfer",
                    transactionId);
        }
        if (loan.isClosedWrittenOff()) {
            throw new PlatformServiceUnavailableException("error.msg.loan.written.off.update.not.allowed",
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_DESCRIPTION + transactionId
                            + " update not allowed as loan status is written off",
                    transactionId);
        }

        if (transactionToAdjust.hasChargebackLoanTransactionRelations()) {
            throw new PlatformServiceUnavailableException("error.msg.loan.transaction.update.not.allowed",
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_DESCRIPTION + transactionId
                            + " update not allowed as loan transaction is linked to other transactions",
                    transactionId);
        }
        final LocalDate transactionDate = command
                .localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM);
        final BigDecimal transactionAmount = command
                .bigDecimalValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT);
        final ExternalId txnExternalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        final boolean isAdjustCommand = (transactionAmount.compareTo(BigDecimal.ZERO) > 0);
        if (isAdjustCommand && !transactionToAdjust.isEditable()) {
            final String errorMessage = "Loan transaction: " + transactionId + " update not allowed as loan transaction is a "
                    + transactionToAdjust.getTypeOf().getCode();
            throw new InvalidLoanTransactionTypeException("transaction", "error.msg.loan.transaction.update.not.allowed", errorMessage);
        }

        // Check if it is adjustment and if the transaction type is disbursement
        // If yes, check if there is only 1 active disbursement transaction. If so, trhow an exception
        if (transactionAmount.compareTo(BigDecimal.ZERO) == 0 && transactionToAdjust.isDisbursement()
                && loan.getDisbursementDetails().size() == 1) {
            throw new GeneralPlatformDomainRuleException("validation.msg.cannot.undo.last.disbursal.transaction",
                    "Undo disbursal transaction is not allowed for the 1st disbursal. Use Undo Disbursal from menu. ", transactionDate);
        }
        // We don't need auto generation for reversal external id... if it is not provided, it remains null (empty)
        final String reversalExternalId = command.stringValueOfParameterNamedAllowingNull(LoanApiConstants.REVERSAL_EXTERNAL_ID_PARAMNAME);
        final ExternalId reversalTxnExternalId = ExternalIdFactory.produce(reversalExternalId);

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, command.locale());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.PAYMENT_TYPE_ID_PARAM,
                command.longValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.PAYMENT_TYPE_ID_PARAM));

        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        final Money transactionAmountAsMoney = Money.of(loan.getCurrency(), transactionAmount);
        String channelName = command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.CHANNEL_NAME_PARAM);
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
            if (Boolean.TRUE.equals(loan.isPeriodicAccrualAccountingEnabledOnLoanProduct())) {
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
                final boolean isBankChannel = channelData.getName().equalsIgnoreCase(LoanWritePlatformServiceJpaRepositoryImpl.BANCOS_PARAM)
                        || channelData.getHash().equalsIgnoreCase("1ae8d4db830eed577c6023998337d0hags546f1a3ba08e5df1ef0d1673431a3");
                if (loanStatus.isOverpaid() && !isBankChannel) {
                    final String totalOverpaid = Money.of(loan.getCurrency(), loan.getTotalOverpaid()).toString();
                    handleOverPaidException(totalOverpaid);
                }
            }
        }
        if (changedTransactionDetail != null) {
            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                loanAccountDomainService.saveLoanTransactionWithDataIntegrityViolationChecks(mapEntry.getValue());
                accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
            }
            // Trigger transaction replayed event
            replayedTransactionBusinessEventService.raiseTransactionReplayedEvents(changedTransactionDetail);
        }
        loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled() || loan.isProgressiveLoan()) {
            scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
            loan.regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
            loan = saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
        }
        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
            Note note;
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
        if (DateUtils.isEqual(previousTransactionDate, transactionDate) && transactionToAdjust.hasOccurredOnSuspendedAccount()) {
            final Long loanTransactionId = transactionToAdjust.getId();
            final List<FacturaElectronicaMensual> facturaElectronicMensuals = this.facturaElectronicMensualRepository
                    .findByLoanTransactionId(loanTransactionId);
            final List<LoanTransaction> invoicedByTransactions = loan.getLoanTransactions().stream()
                    .filter(ltx -> Objects.equals(loanTransactionId, ltx.getInvoicedByTransactionId())).toList();
            if (CollectionUtils.isNotEmpty(invoicedByTransactions)) {
                invoicedByTransactions.forEach(LoanTransaction::resetInvoicedByTransactionId);
                this.loanTransactionRepository.saveAll(invoicedByTransactions);
            }
            if (CollectionUtils.isNotEmpty(facturaElectronicMensuals)) {
                this.facturaElectronicMensualRepository.deleteAll(facturaElectronicMensuals);
            }
        }

        // Add GAC charge realculation here
        if (Objects.nonNull(loanChargeWritePlatformService)) {
            loanChargeWritePlatformService.applyGACChargeForOverdueLoanAfterRepaymentOrReversal(loan, transactionDate);
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
            throw new PlatformServiceUnavailableException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHARGE_BACK_NOT_ALLOWED,
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_DESCRIPTION + transactionId
                            + " chargeback not allowed as loan transaction repayment is reversed",
                    transactionId);
        }

        if (!loanTransaction.isTypeAllowedForChargeback()) {
            throw new PlatformServiceUnavailableException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHARGE_BACK_NOT_ALLOWED,
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_DESCRIPTION + transactionId
                            + " chargeback not allowed for loan transaction type, its type is " + loanTransaction.getTypeOf().getCode(),
                    transactionId);
        }

        Loan loan = this.loanAssembler.assembleFrom(loanId);
        if (this.accountTransfersReadPlatformService.isAccountTransfer(transactionId, PortfolioAccountType.LOAN)) {
            throw new PlatformServiceUnavailableException("error.msg.loan.transfer.transaction.update.not.allowed",
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_DESCRIPTION + transactionId
                            + " chargeback not allowed as it involves in account transfer",
                    transactionId);
        }
        if (loan.isClosedWrittenOff()) {
            throw new PlatformServiceUnavailableException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHARGE_BACK_NOT_ALLOWED,
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_DESCRIPTION + transactionId
                            + " chargeback not allowed as loan status is written off",
                    transactionId);
        }
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            throw new PlatformServiceUnavailableException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHARGE_BACK_NOT_ALLOWED,
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_DESCRIPTION + transactionId
                            + " chargeback not allowed as loan product is interest recalculation enabled",
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
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT,
                command.stringValueOfParameterNamed(LoanApiConstants.TRANSACTION_AMOUNT_PARAMNAME));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, command.locale());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.PAYMENT_TYPE_ID_PARAM,
                command.longValueOfParameterNamed(LoanApiConstants.PAYMENT_TYPE_PARAMNAME));

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
            throw new PlatformServiceUnavailableException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHARGE_BACK_NOT_ALLOWED,
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_DESCRIPTION + loanTransaction.getId()
                            + " chargeback not allowed as loan transaction amount is not enough",
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
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, command.locale());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());
        final LocalDate transactionDate = command
                .localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM);
        final BigDecimal transactionAmount = command
                .bigDecimalValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT);
        final ExternalId externalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        Loan loan = this.loanAssembler.assembleFrom(loanId);
        checkClientOrGroupActive(loan);

        checkIfProductAllowsWaivePrincipalOrInterest(loan);

        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        final Money transactionAmountAsMoney = Money.of(loan.getCurrency(), transactionAmount);
        Money unrecognizedIncome = transactionAmountAsMoney.zero();
        Money interestComponent = transactionAmountAsMoney;
        if (Boolean.TRUE.equals(loan.isPeriodicAccrualAccountingEnabledOnLoanProduct())) {
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
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, command.locale());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());
        LocalDate transactionDate = command
                .localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM);
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        if (command.hasParameter(LoanWritePlatformServiceJpaRepositoryImpl.WRITE_OFF_REASON_ID_PARAM)) {
            Long writeoffReasonId = command.longValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.WRITE_OFF_REASON_ID_PARAM);
            CodeValue writeoffReason = this.codeValueRepository
                    .findOneByCodeNameAndIdWithNotFoundDetection(LoanApiConstants.WRITEOFFREASONS, writeoffReasonId);
            changes.put(LoanWritePlatformServiceJpaRepositoryImpl.WRITE_OFF_REASON_ID_PARAM, writeoffReasonId);
            loan.updateWriteOffReason(writeoffReason);
        }

        checkClientOrGroupActive(loan);
        if (loan.isChargedOff() && DateUtils.isBefore(transactionDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_TRANSACTION_DATE_CANNOT_BE_EARLIER_THAN_CHARGE_OFF_DATE,
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_DESCRIPTION + loanId
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
            recalculateFrom = command.localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM);
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
            if (loanCharge.getAmountOutstanding(loan.getCurrency()).isGreaterThanZero()
                    && ((incident.isMandatory() && loanCharge.isMandatoryInsurance())
                            || (incident.isVoluntary() && loanCharge.isVoluntaryInsurance()))) {
                BigDecimal cumulative = BigDecimal.ZERO;
                InsuranceIncidentNoveltyNews insuranceIncidentNoveltyNews = InsuranceIncidentNoveltyNews.instance(loan, loanCharge, null,
                        incident, writeOffDate, cumulative);

                this.insuranceIncidentNoveltyNewsRepository.saveAndFlush(insuranceIncidentNoveltyNews);
            }

        }
    }

    @SuppressWarnings({ "squid:S3776" })
    @Transactional
    @Override
    public CommandProcessingResult specialWriteOff(final Long loanId, final JsonCommand command) {
        this.loanEventApiJsonValidator.validateSpecialWriteOff(command.json());
        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, command.locale());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());
        Loan loan = this.loanAssembler.assembleFrom(loanId);
        final LoanProduct loanProduct = loan.getLoanProduct();
        if (Boolean.FALSE.equals(loanProduct.getCustomAllowForgiveness())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.product.write.off.is.disabled.on.product",
                    "Loan write-off is disabled on this product");
        }
        if (command.hasParameter(LoanWritePlatformServiceJpaRepositoryImpl.WRITE_OFF_REASON_ID_PARAM)) {
            Long writeoffReasonId = command.longValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.WRITE_OFF_REASON_ID_PARAM);
            CodeValue writeoffReason = this.codeValueRepository
                    .findOneByCodeNameAndIdWithNotFoundDetection(LoanApiConstants.WRITEOFFREASONS, writeoffReasonId);
            changes.put(LoanWritePlatformServiceJpaRepositoryImpl.WRITE_OFF_REASON_ID_PARAM, writeoffReasonId);
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
        LocalDate transactionDatetmp = command.localDateValueOfParameterNamed("transactionDate");
        if (transactionDatetmp == null) {
            transactionDatetmp = DateUtils.getBusinessLocalDate();
        }
        final LocalDate transactionDate = transactionDatetmp;
        final String txnExternalId = command
                .stringValueOfParameterNamedAllowingNull(LoanWritePlatformServiceJpaRepositoryImpl.EXTERNAL_ID_PARAM);
        ExternalId externalId = ExternalIdFactory.produce(txnExternalId);
        if (externalId.isEmpty() && TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
            externalId = ExternalId.generate();
        }
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.EXTERNAL_ID_PARAM, externalId);
        ChangedTransactionDetail changedTransactionDetail = loan.validateSpecialWrittenOff(command, existingTransactionIds,
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
            final LoanRepaymentScheduleInstallment specialWriteOffInstallment = loan
                    .fetchLoanSpecialWriteOffDetail(loan.isMigratedLoan() ? loan.getExpectedMaturityDate() : transactionDate);
            final LoanRepaymentScheduleInstallmentData loanRepaymentScheduleInstallmentData = loan.validateSpecialWriteOffConcepts(command,
                    specialWriteOffInstallment);
            final BigDecimal principalToBeWrittenOff = loanRepaymentScheduleInstallmentData.getPrincipalPortion();
            final Money remainingPrincipalPortion = specialWriteOffInstallment.getPrincipalOutstanding(currency)
                    .minus(principalToBeWrittenOff);
            final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments = loan.getRepaymentScheduleInstallments();
            final LoanRepaymentScheduleInstallment currentScheduleInstallment = fetchRepaymentInstallmentByWrittenOfDate(
                    loan.isMigratedLoan() ? loan.getExpectedMaturityDate() : transactionDate, repaymentScheduleInstallments);

            Money interestToBeChargedAndWrittenOff = currentScheduleInstallment.getInterestCharged(currency);
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
                                .filter(period -> period.isRepaymentPeriod() || period.isDownPaymentPeriod()).findFirst()
                                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.loan.schedule.period.not.found",
                                        "Loan schedule period not found"));
                        final Money midInterestForCurrentPeriod = Money.of(currency, BigDecimal.valueOf(
                                loan.calculateInterestForDays(totalPeriodDays, midScheduleInstallment.interestDue(), futureTillDays)));
                        interestToBeChargedAndWrittenOff = interestForCurrentPeriod.plus(midInterestForCurrentPeriod);
                        final LocalDate installmentFromDate = nextRescheduleInstallment.getFromDate();
                        final LocalDate installmentDueDate = nextRescheduleInstallment.getDueDate();
                        writeOffNumberOfRepayments = numberOfRepayments - currentInstallmentNumber;
                        loanApplicationTerms.updateLoanTermVariations(new ArrayList<>());
                        loanApplicationTerms.updateNumberOfRepayments(writeOffNumberOfRepayments);
                        loanApplicationTerms.updateLoanTermFrequency(writeOffNumberOfRepayments);
                        loanApplicationTerms.setPrincipal(remainingPrincipalPortion);
                        loanApplicationTerms.updateApprovedPrincipal(remainingPrincipalPortion);
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
                            if (scheduledLoanInstallment.isRepaymentPeriod() || scheduledLoanInstallment.isDownPaymentPeriod()) {
                                Integer finalRegeneratedInstallmentNumber = regeneratedInstallmentNumber;
                                LoanRepaymentScheduleInstallment updatedInstallment = repaymentScheduleInstallments.stream()
                                        .filter(installment -> installment.getInstallmentNumber().equals(finalRegeneratedInstallmentNumber))
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

                        final List<LoanRepaymentScheduleInstallment> repaymentInstallmentsToRemove = new ArrayList<>();
                        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
                            if (installment.getInstallmentNumber() > currentInstallmentNumber + numberOfRegeneratedInstallments) {
                                repaymentInstallmentsToRemove.add(installment);
                            }
                        }
                        for (final LoanRepaymentScheduleInstallment installment : repaymentInstallmentsToRemove) {
                            loan.removeLoanRepaymentScheduleInstallment(installment.getInstallmentNumber());
                        }
                        final BigDecimal adjustedPrincipalAmount = principalToBeWrittenOff
                                .subtract(unpaidPrincipalUptoCurrentInstallment.getAmount())
                                .add(currentScheduleInstallment.getPrincipalOutstanding(currency).getAmount());
                        currentScheduleInstallment.updatePrincipal(adjustedPrincipalAmount);
                        saveAndFlushLoanWithIntegrityChecks(loan);
                    }
                }
            } else {
                final Money interestToBeWrittenOff = specialWriteOffInstallment.getInterestOutstanding(currency);
                final Money feeChargesToBeWrittenOff = specialWriteOffInstallment.getFeeChargesOutstanding(currency);
                final Money penaltyChargesToBeWrittenOff = specialWriteOffInstallment.getPenaltyChargesOutstanding(currency);
                final Money interestAmountRemaining = specialWriteOffInstallment.getInterestOutstanding(currency)
                        .minus(interestToBeWrittenOff);
                final Money feeChargesAmountRemaining = specialWriteOffInstallment.getFeeChargesOutstanding(currency)
                        .minus(feeChargesToBeWrittenOff);
                final Money penaltyChargesAmountRemaining = specialWriteOffInstallment.getPenaltyChargesOutstanding(currency)
                        .minus(penaltyChargesToBeWrittenOff);
                Money futureOutstandingPrincipal = Money.zero(currency);
                final List<LoanRepaymentScheduleInstallment> repaymentInstallmentsToRemove = new ArrayList<>();
                for (final LoanRepaymentScheduleInstallment scheduleInstallment : repaymentScheduleInstallments) {
                    if (scheduleInstallment.getInstallmentNumber() > currentScheduleInstallment.getInstallmentNumber()) {
                        futureOutstandingPrincipal = futureOutstandingPrincipal.plus(scheduleInstallment.getPrincipalOutstanding(currency));
                        repaymentInstallmentsToRemove.add(scheduleInstallment);
                    }
                }
                for (final LoanRepaymentScheduleInstallment installment : repaymentInstallmentsToRemove) {
                    loan.removeLoanRepaymentScheduleInstallment(installment.getInstallmentNumber());
                }
                final BigDecimal totalPrincipalOutstanding = currentScheduleInstallment.getPrincipalOutstanding(currency)
                        .plus(futureOutstandingPrincipal).getAmount();
                currentScheduleInstallment.updatePrincipal(totalPrincipalOutstanding);
                if (interestAmountRemaining.isZero() && feeChargesAmountRemaining.isZero() && penaltyChargesAmountRemaining.isZero()) {
                    int totalPeriodDays = Math.toIntExact(
                            ChronoUnit.DAYS.between(currentScheduleInstallment.getFromDate(), currentScheduleInstallment.getDueDate()));
                    int tillDays = Math.toIntExact(ChronoUnit.DAYS.between(currentScheduleInstallment.getFromDate(), transactionDate));
                    interestToBeChargedAndWrittenOff = Money.of(currency, BigDecimal.valueOf(loan.calculateInterestForDays(totalPeriodDays,
                            currentScheduleInstallment.getInterestCharged(currency).getAmount(), tillDays)));
                }
                saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
            }
            final boolean isCreditNote = command.booleanPrimitiveValueOfParameterNamed("isCreditNote");
            writeOffTransaction = loan.writeOff(loanRepaymentScheduleInstallmentData, transactionDate, externalId, isCreditNote);
            currentScheduleInstallment.updateInterestCharged(interestToBeChargedAndWrittenOff.getAmount());
            loan.updateLoanSummaryDerivedFields();
            loan.getRepaymentScheduleInstallments().forEach(rp -> rp.checkIfRepaymentPeriodObligationsAreMet(transactionDate, currency));
            final Money totalOutstandingAmount = specialWriteOffInstallment.getTotalOutstanding(currency);
            final Money totalPaymentAmount = Money.of(currency, loanRepaymentScheduleInstallmentData.getTotalInstallmentAmount());
            if (totalPaymentAmount.isEqualTo(totalOutstandingAmount)) {
                final AppUser currentUser = getAppUserIfPresent();
                loan.closeAsWrittenOff(transactionDate, currentUser);
            }
        }
        loan = writeOffTransaction.getLoan();
        final LoanStatus loanStatus = loan.getStatus();
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
        LoanRepaymentScheduleInstallment installment = null;
        for (LoanRepaymentScheduleInstallment repaymentScheduleInstallment : repaymentScheduleInstallments) {
            if ((DateUtils.isAfter(writtenOffOnDate, repaymentScheduleInstallment.getFromDate())
                    || DateUtils.isEqual(writtenOffOnDate, repaymentScheduleInstallment.getFromDate()))
                    && !DateUtils.isAfter(writtenOffOnDate, repaymentScheduleInstallment.getDueDate())) {
                installment = repaymentScheduleInstallment;
                break;
            }

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
        LocalDate transactionDate = command
                .localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM);
        if (loan.isChargedOff() && DateUtils.isBefore(transactionDate, loan.getChargedOffOnDate())) {
            throw new GeneralPlatformDomainRuleException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_TRANSACTION_DATE_CANNOT_BE_EARLIER_THAN_CHARGE_OFF_DATE,
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_DESCRIPTION + loanId
                            + " backdated transaction is not allowed. Transaction date cannot be earlier than the charge-off date of the loan",
                    loanId);
        }

        businessEventNotifierService.notifyPreBusinessEvent(new LoanCloseBusinessEvent(loan));

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, command.locale());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());

        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        updateLoanCounters(loan, loan.getDisbursementDate());

        LocalDate recalculateFrom = null;
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            recalculateFrom = command.localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM);
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
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHARGE_OFF_LOAN,
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_DESCRIPTION + loanId
                            + " Close as rescheduled is not allowed. Loan Account is Charged-off",
                    loanId);
        }
        removeLoanCycle(loan);
        businessEventNotifierService.notifyPreBusinessEvent(new LoanCloseAsRescheduleBusinessEvent(loan));

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, command.locale());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());

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

        final LocalDate transactionDate = command
                .localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.ACTUAL_DISBURSEMENT_DATE_PARAM);
        final ExternalId txnExternalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(locale);
        final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(transactionDate, amount, PortfolioAccountType.LOAN,
                PortfolioAccountType.LOAN, loan.getId(), loan.getTopupLoanDetails().getLoanIdToClose(), "Loan Topup", locale, fmt,
                LoanTransactionType.DISBURSEMENT.getValue(), LoanTransactionType.REPAYMENT.getValue(), txnExternalId, loan, null);
        AccountTransferDetails accountTransferDetails = this.accountTransfersWritePlatformService.repayLoanWithTopup(accountTransferDTO);
        loan.getTopupLoanDetails().setAccountTransferDetails(accountTransferDetails.getId());
        loan.getTopupLoanDetails().setTopupAmount(amount);
        if (loanToClose.claimType() == null || !loanToClose.claimType().equals(LoanWritePlatformServiceJpaRepositoryImpl.CASTIGADO_PARAM)) {
            BlockingReasonSetting setting = loanBlockingReasonRepository.getSingleBlockingReasonSettingByReason(
                    BlockingReasonSettingEnum.CREDIT_RESTRUCTURE.getDatabaseString(), BlockLevel.CREDIT.toString());
            loanBlockWritePlatformService.blockLoan(loan.getId(), setting, "Reestructurada", DateUtils.getLocalDateOfTenant());
        }
    }

    private void disburseLoanToSavings(final Loan loan, final JsonCommand command, final Money amount, final PaymentDetail paymentDetail) {

        final LocalDate transactionDate = command
                .localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.ACTUAL_DISBURSEMENT_DATE_PARAM);
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

    @SuppressWarnings({ "squid:S3776", "squid:S6809" })
    @Transactional
    @Override
    public void applyMeetingDateChanges(final Calendar calendar, final Collection<CalendarInstance> loanCalendarInstances) {
        final Boolean rescheduleBasedOnMeetingDates = null;
        final LocalDate presentMeetingDate = null;
        final LocalDate newMeetingDate = null;
        applyMeetingDateChanges(calendar, loanCalendarInstances, rescheduleBasedOnMeetingDates, presentMeetingDate, newMeetingDate);
    }

    @SuppressWarnings({ "squid:S3776" })
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
                    throw new GeneralPlatformDomainRuleException(
                            LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHARGE_OFF_LOAN,
                            LoanWritePlatformServiceJpaRepositoryImpl.LOAN_DESCRIPTION + loan.getId()
                                    + " reschedule is not allowed. Loan Account is Charged-off",
                            loan.getId());
                }

                Boolean isSkipRepaymentOnFirstMonth = false;
                int numberOfDays = 0;
                boolean isSkipRepaymentOnFirstMonthEnabled = configurationDomainService.isSkippingMeetingOnFirstDayOfMonthEnabled();
                if (isSkipRepaymentOnFirstMonthEnabled) {
                    isSkipRepaymentOnFirstMonth = this.loanUtilService.isLoanRepaymentsSyncWithMeeting(loan.group(), calendar);
                    if (Boolean.TRUE.equals(isSkipRepaymentOnFirstMonth)) {
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
                } else if (Boolean.TRUE.equals(rescheduleBasedOnMeetingDates)) {
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

    @SuppressWarnings({ "squid:S3776" })
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

    @SuppressWarnings({ "squid:S3776" })
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
                + "WHERE ml.client_id = ? " + "AND ml.disbursement_date BETWEEN to_date(?, 'YYYY-MM-DD') - INTERVAL '6' MONTH "
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
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        businessEventNotifierService.notifyPostBusinessEvent(new LoanUndoWrittenOffBusinessEvent(writeOffTransaction));
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
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHARGE_OFF_LOAN,
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
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHARGE_OFF_LOAN,
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
        throw ErrorHandler.getMappable(t, null, null, "loan.recalculateinterest");
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

    private void regenerateScheduleOnDisbursement(final JsonCommand command, final Loan loan, boolean recalculateSchedule,
            final ScheduleGeneratorDTO scheduleGeneratorDTO, final LocalDate nextPossibleRepaymentDate,
            final LocalDate rescheduledRepaymentDate) {
        final LocalDate actualDisbursementDate = command
                .localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.ACTUAL_DISBURSEMENT_DATE_PARAM);
        BigDecimal emiAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.emiAmountParameterName);

        // Always recalculate schedule for multi-disbursal loans
        if (loan.isMultiDisburmentLoan()) {
            recalculateSchedule = DisbursementCutoffContext.shouldRecalculateSchedule(loan, actualDisbursementDate,
                    this::calculateInstallmentsToAdd);

            log.info("Multi-disbursement loan {}: recalculateSchedule = {}", loan.getId(), recalculateSchedule);

        }

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
        final LocalDate transactionDate = command
                .localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM);
        final BigDecimal transactionAmount = command
                .bigDecimalValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT);
        final String noteText = command.stringValueOfParameterNamedAllowingNull("note");
        final ExternalId externalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, command.locale());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());

        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
        }
        if (!externalId.isEmpty()) {
            changes.put(LoanApiConstants.externalIdParameterName, externalId);
        }
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.PAYMENT_TYPE_ID_PARAM,
                command.longValueOfParameterNamed(LoanApiConstants.PAYMENT_TYPE_PARAMNAME));

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

        final LocalDate transactionDate = command
                .localDateValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM);
        ExternalId externalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);

        final BigDecimal transactionAmount = command
                .bigDecimalValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT);
        checkIfLoanIsPaidInAdvance(loanId, transactionAmount);

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT,
                command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_TRANSACTION_AMOUNT));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, command.locale());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());
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
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHARGE_OFF_LOAN,
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
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM,
                command.stringValueOfParameterNamed(LoanApiConstants.transactionDateParamName));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.EXTERNAL_ID_PARAM, externalId);

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
                changes, true);

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
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM,
                command.stringValueOfParameterNamed(LoanApiConstants.transactionDateParamName));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.EXTERNAL_ID_PARAM, externalId);
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
        createAnulacionNoveltyNews(loan, transactionDate);
        if (transactionDate.equals(loan.getDisbursementDate())) {
            loan.setAnulado(true);
            loan.setAnuladoOnDisbursementDate(true);
        }
        final LoanTransaction foreclosureTransaction = this.loanAccountDomainService.foreCloseLoan(loan, transactionDate, noteText,
                externalId, changes, false);
        final BlockingReasonSetting blockingReasonSetting = loanBlockingReasonRepository.getSingleBlockingReasonSettingByReason(
                BlockingReasonSettingEnum.CREDIT_ANULADO.getDatabaseString(), BlockLevel.CREDIT.toString());
        // not to mess with the record , we will just ensure it does not affect client level
        blockingReasonSetting.setAffectsClientLevel(0);
        loanBlockWritePlatformService.blockLoan(loan.getId(), blockingReasonSetting, "Anulado", DateUtils.getLocalDateOfTenant());
        final CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder();
        return commandProcessingResultBuilder.withLoanId(loanId).withEntityId(foreclosureTransaction.getId())
                .withEntityExternalId(foreclosureTransaction.getExternalId()).with(changes).build();
    }

    @SuppressWarnings({ "squid:S3776" })
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
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_DESCRIPTION + loanId
                            + " Charge-off is not allowed. Loan Account is not Active",
                    loanId);
        }
        if (loan.isChargedOff()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.already.charged.off",
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_DESCRIPTION + loanId + " is already charged-off", loanId);
        }
        if (DateUtils.isBefore(transactionDate, loan.getLastUserTransactionDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.charge.off.is.before.than.the.last.user.transaction",
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_DESCRIPTION + loanId
                            + " charge-off cannot be executed. User transaction was found after the charge-off transaction date!",
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
                    cumulative = processInsuranceChargeCancellation(cumulative, loan, data, true);
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

        this.loanAccountDomainService.foreCloseLoan(loan, transactionDate, noteText, txnExternalId, changes, false);
        final BlockingReasonSetting blockingReasonSetting = loanBlockingReasonRepository.getSingleBlockingReasonSettingByReason(
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
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_DESCRIPTION + loanId
                            + " Undo Charge-off is not allowed. Loan Account is not Active",
                    loanId);
        }
        if (!loan.isChargedOff()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.not.charged.off",
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_DESCRIPTION + loanId + " is not charged-off", loanId);
        }
        LoanTransaction chargedOffTransaction = loan.findChargedOffTransaction();
        if (chargedOffTransaction == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.charge.off.transaction.not.found",
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_DESCRIPTION + loanId + " charge-off transaction was not found", loanId);
        }
        if (!chargedOffTransaction.equals(loan.getLastUserTransaction())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.charge.off.is.not.the.last.user.transaction",
                    LoanWritePlatformServiceJpaRepositoryImpl.LOAN_DESCRIPTION + loanId
                            + " charge-off cannot be undone. User transaction was found after charge-off!",
                    loanId);
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

    @SuppressWarnings({ "squid:S3776", "squid:S135" })
    @Override
    public void recalculateInterestForMaximumLegalRate(List<LoanRescheduleData> loanLoanRescheduleDataList,
            MaximumCreditRateConfigurationData maximumCreditRateConfigurationData) throws JobExecutionException {
        final List<Throwable> exceptions = new ArrayList<>();
        final LocalDate appliedOnDate = maximumCreditRateConfigurationData.getAppliedOnDate();
        final BigDecimal maximumLegalAnnualNominalRateValue = maximumCreditRateConfigurationData.getAnnualNominalRate();
        if (CollectionUtils.isNotEmpty(loanLoanRescheduleDataList)) {
            final String locale = "es";
            final String dateFormat = "dd MMMM yyyy";
            final String submittedOnDate = DateUtils.format(DateUtils.getBusinessLocalDate(), dateFormat, Locale.forLanguageTag(locale));
            LoanRescheduleRequestData loanRescheduleReasons = this.loanRescheduleRequestReadPlatformService
                    .retrieveAllRescheduleReasons(RescheduleLoansApiConstants.LOAN_RESCHEDULE_REASON, null);
            Long rescheduleReasonId = null;
            for (CodeValueData codeValueData : loanRescheduleReasons.getRescheduleReasons()) {
                if (codeValueData.getName().equalsIgnoreCase(LoanWritePlatformServiceJpaRepositoryImpl.MAXIMUM_LEGAL_RATE_RECALCULATION)) {
                    rescheduleReasonId = codeValueData.getId();
                    break;
                }
            }
            final JsonObject rescheduleJsonObject = new JsonObject();
            rescheduleJsonObject.addProperty(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, dateFormat);
            rescheduleJsonObject.addProperty(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, locale);
            rescheduleJsonObject.addProperty(RESCHEDULE_REASON_ID_PARAM, rescheduleReasonId);
            rescheduleJsonObject.addProperty(SUBMITTED_ON_DATE, submittedOnDate);
            rescheduleJsonObject.addProperty(ADJUSTED_DUE_DATE_PARAM, "");
            rescheduleJsonObject.addProperty(GRACE_ON_PRINCIPAL_PARAM, "");
            rescheduleJsonObject.addProperty(EXTRA_TERMS, "");

            for (final LoanRescheduleData loanRescheduleData : loanLoanRescheduleDataList) {
                final Long loanId = loanRescheduleData.getId();
                final Loan loan = this.loanRepository.findById(loanId).orElseThrow(() -> new LoanNotFoundException(loanId));
                final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = loan
                        .getInstallmentByScheduleFromDate(appliedOnDate);
                if (loanRepaymentScheduleInstallment == null) {
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
                    continue;
                }
                rescheduleJsonObject.addProperty(NEW_INTEREST_RATE_PARAM, newInterestRate);
                final String rescheduleFromDateString = DateUtils.format(appliedOnDate, dateFormat, Locale.forLanguageTag(locale));
                rescheduleJsonObject.addProperty(RESCHEDULE_FROM_DATE_PARAM, rescheduleFromDateString);
                rescheduleJsonObject.addProperty(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_ID_PARAM, loanId);
                final String rescheduleReasonComment = String.format(
                        "Recalcular la tasa de interés al máximo legal: [Nueva tasa de interés: %s, Tasa máxima legal: %s, Fecha de reprogramación: %s]",
                        newInterestRate, maximumLegalAnnualNominalRateValue, rescheduleFromDateString);
                rescheduleJsonObject.addProperty(RESCHEDULE_REASON_COMMENT_PARAM, rescheduleReasonComment);
                final String rescheduleRequestBodyAsJson = rescheduleJsonObject.toString();
                CommandWrapper commandWrapper = new CommandWrapperBuilder()
                        .createLoanRescheduleRequest(RescheduleLoansApiConstants.ENTITY_NAME).withJson(rescheduleRequestBodyAsJson).build();
                try {
                    log.info("Create Loan Reschedule Request with Loan ID: {}", loanId);
                    CommandProcessingResult commandProcessingResult = commandsSourceWritePlatformService.logCommandSource(commandWrapper);
                    if (commandProcessingResult.getResourceId() != null) {
                        final Long loanRescheduleId = commandProcessingResult.getResourceId();
                        final JsonObject approvalJsonObject = new JsonObject();
                        final Boolean isJobTriggered = true;
                        approvalJsonObject.addProperty(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, dateFormat);
                        approvalJsonObject.addProperty(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, locale);
                        approvalJsonObject.addProperty("isJobTriggered", isJobTriggered);
                        approvalJsonObject.addProperty(APPROVED_DATE_PARAM, submittedOnDate);
                        final String approvalRequestBodyAsJson = approvalJsonObject.toString();
                        commandWrapper = new CommandWrapperBuilder()
                                .approveLoanRescheduleRequest(RescheduleLoansApiConstants.ENTITY_NAME, loanRescheduleId)
                                .withJson(approvalRequestBodyAsJson).build();
                        log.info("Approve Loan Rescheduling with Loan ID: {}", loanId);
                        commandProcessingResult = commandsSourceWritePlatformService.logCommandSource(commandWrapper);
                        if (commandProcessingResult.getResourceId() != null) {
                            final String successMessage = "Reprogramar la cuenta de préstamo: " + loanId
                                    + " con la tasa de interés al máximo legal";
                            log.info(successMessage);
                        }
                    }
                } catch (final PlatformApiDataValidationException e) {
                    final List<ApiParameterError> errors = e.getErrors();
                    for (final ApiParameterError error : errors) {
                        log.error("Reprogramar la cuenta de préstamo {} falló con el mensaje: {}", loanId, error.getDeveloperMessage(), e);
                    }
                    exceptions.add(e);
                } catch (final AbstractPlatformDomainRuleException e) {
                    log.error("Reprogramar la cuenta de préstamo: {} falló con el mensaje: {}", loanId, e.getDefaultUserMessage(), e);
                    exceptions.add(e);
                } catch (Exception e) {
                    log.error("Reprogramar la cuenta de préstamo: {} falló con el mensaje: {}", loanId, e.getMessage(), e);
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

    @SuppressWarnings({ "squid:S3776", "squid:S135" })
    private ChannelData validateUndoRepaymentChannel(final String channelName, final LoanProduct loanProduct, Long transactionId,
            Long loanId) {
        final LoanTransaction loanTransaction = this.loanTransactionRepository.findByIdAndLoanId(transactionId, loanId)
                .orElseThrow(() -> new LoanTransactionNotFoundException(transactionId, loanId));
        final PaymentDetail paymentDetail = loanTransaction.getPaymentDetail();
        if (StringUtils.isBlank(channelName)) {
            throw new GeneralPlatformDomainRuleException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_BLANK,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_CHANNEL_IS_BLANK);
        }
        final ChannelData channelData = this.channelReadWritePlatformService.findByNameType(channelName, ChannelType.REPAYMENT.getValue());
        if (channelData == null) {
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHANNEL_NOT_FOUND,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_CHANNEL_NOT_FOUND, channelName);
        }
        if (Boolean.FALSE.equals(channelData.getActive())) {
            throw new GeneralPlatformDomainRuleException(LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_CHANNEL_NOT_ACTIVE,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_CHANNEL_NOT_ACTIVE, channelName);
        }

        final List<Channel> repaymentChannels = loanProduct.getRepaymentChannels();
        if (CollectionUtils.isNotEmpty(repaymentChannels)) {
            final Long channelId = channelData.getId();
            if (paymentDetail != null && !Objects.equals(paymentDetail.getChannelId(), channelId)
                    && !channelName.equalsIgnoreCase(ChannelApiConstants.defaultChannel)) {
                throw new GeneralPlatformDomainRuleException(
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_NOT_ALLOWED,
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_CHANNEL_IS_NOT_ALLOWED, channelName);
            }

            if (repaymentChannels.stream().noneMatch(repaymentChannel -> repaymentChannel.getId().equals(channelId))
                    && !channelName.equalsIgnoreCase(ChannelApiConstants.defaultChannel)) {
                throw new GeneralPlatformDomainRuleException(
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_NOT_ALLOWED,
                        LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_CHANNEL_IS_NOT_ALLOWED, channelName);
            }

        } else {
            throw new GeneralPlatformDomainRuleException(
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_LABEL_VALIDATION_CHANNEL_IS_NOT_ALLOWED,
                    LoanWritePlatformServiceJpaRepositoryImpl.ERROR_MESSAGE_VALIDATION_CHANNEL_IS_NOT_ALLOWED, channelName);
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
                generateLoanTransactionDocument(loanTransaction);
            }
        }
    }

    @SuppressWarnings({ "squid:S3776", "squid:S135" })
    private void generateLoanTransactionDocument(final LoanTransaction loanTransaction) {
        final Long loanTransactionId = loanTransaction.getId();
        final LocalDate transactionDate = loanTransaction.getTransactionDate();
        final YearMonth yearMonth = YearMonth.from(transactionDate);
        final LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();
        final LocalDate firstDayOfMonth = transactionDate.withDayOfMonth(1);
        final LocalDate secondLastDayOfMonth = lastDayOfMonth.minusDays(1);
        final LoanTransactionType loanTransactionType = loanTransaction.getTypeOf();
        final FacturaElectronicaMensualTasklet.LoanInvoiceMapper transactionMapper = new FacturaElectronicaMensualTasklet.LoanInvoiceMapper();
        final String transactionSQL = "SELECT " + transactionMapper.transactionSchema() + " WHERE mlt.\"transactionId\" = ? ";
        final List<LoanDocumentData> loanDocumentDataList = this.jdbcTemplate.query(transactionSQL, transactionMapper, loanTransactionId);
        if (!loanDocumentDataList.isEmpty()) {
            final LoanDocumentData loanDocumentData = loanDocumentDataList.get(0);
            final Loan loan = loanTransaction.getLoan();
            BigDecimal interestPaidRemaining = loanDocumentData.getInterestPaid();
            BigDecimal mandatoryInsurancePaidRemaining = loanDocumentData.getMandatoryInsurancePaid();
            BigDecimal voluntaryInsurancePaidRemaining = loanDocumentData.getVoluntaryInsurancePaid();
            BigDecimal honorariosPaidRemaining = loanDocumentData.getHonorariosPaid();
            BigDecimal penaltyChargesPaidRemaining = loanDocumentData.getPenaltyChargesPaid();
            final List<LoanTransactionData> invoicedByAccrualTransactionDataList = new ArrayList<>();
            final List<LoanTransaction> invoicedByAccrualTransactionList = new ArrayList<>();
            final List<LoanTransaction> accrualTransactions = loan.retrieveListOfAccrualTransactions().stream()
                    .filter(ltx -> Objects.isNull(ltx.getInvoicedByTransactionId())).toList();
            for (final LoanTransaction accrualTransaction : accrualTransactions) {
                boolean occurredOnSuspendedAccount = accrualTransaction.hasOccurredOnSuspendedAccount();
                final LoanTransactionData loanTransactionData = loanReadPlatformService.retrieveLoanTransaction(loan.getId(),
                        accrualTransaction.getId());
                loanTransactionData.setOccurredOnSuspendedAccount(occurredOnSuspendedAccount);
                final LoanChargePaidByData loanChargePaidByData = loanTransactionData.getLoanChargePaidBySummary();
                final BigDecimal interestPaid = loanTransactionData.getInterestPortion();
                final BigDecimal penaltyChargesPaid = loanTransactionData.getPenaltyChargesPortion();
                final BigDecimal mandatoryInsurancePaid = loanChargePaidByData.getMandatoryInsurance();
                final BigDecimal voluntaryInsurancePaid = loanChargePaidByData.getVoluntaryInsurance();
                final BigDecimal honorariosPaid = loanChargePaidByData.getHono();
                if (interestPaidRemaining.compareTo(interestPaid) >= 0) {
                    interestPaidRemaining = interestPaidRemaining.subtract(interestPaid);
                } else {
                    continue;
                }
                if (penaltyChargesPaidRemaining.compareTo(penaltyChargesPaid) >= 0) {
                    penaltyChargesPaidRemaining = penaltyChargesPaidRemaining.subtract(penaltyChargesPaid);
                } else {
                    continue;
                }
                if (mandatoryInsurancePaidRemaining.compareTo(mandatoryInsurancePaid) >= 0) {
                    mandatoryInsurancePaidRemaining = mandatoryInsurancePaidRemaining.subtract(mandatoryInsurancePaid);
                } else {
                    continue;
                }
                if (voluntaryInsurancePaidRemaining.compareTo(voluntaryInsurancePaid) >= 0) {
                    voluntaryInsurancePaidRemaining = voluntaryInsurancePaidRemaining.subtract(voluntaryInsurancePaid);
                } else {
                    continue;
                }
                if (honorariosPaidRemaining.compareTo(honorariosPaid) >= 0) {
                    honorariosPaidRemaining = honorariosPaidRemaining.subtract(honorariosPaid);
                } else {
                    continue;
                }
                invoicedByAccrualTransactionDataList.add(loanTransactionData);
                invoicedByAccrualTransactionList.add(accrualTransaction);
            }

            if (CollectionUtils.isNotEmpty(invoicedByAccrualTransactionDataList)) {
                final BigDecimal interestPaid = invoicedByAccrualTransactionDataList.stream()
                        .filter(LoanTransactionData::isOccurredOnSuspendedAccount).map(LoanTransactionData::getInterestPortion)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                final BigDecimal mandatoryInsurancePaid = invoicedByAccrualTransactionDataList.stream()
                        .filter(LoanTransactionData::isOccurredOnSuspendedAccount)
                        .map(loanTransactionData -> loanTransactionData.getLoanChargePaidBySummary().getMandatoryInsurance())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                final BigDecimal voluntaryInsurancePaid = invoicedByAccrualTransactionDataList.stream()
                        .filter(LoanTransactionData::isOccurredOnSuspendedAccount)
                        .map(loanTransactionData -> loanTransactionData.getLoanChargePaidBySummary().getVoluntaryInsurance())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                final BigDecimal honorariosPaid = invoicedByAccrualTransactionDataList.stream()
                        .filter(LoanTransactionData::isOccurredOnSuspendedAccount)
                        .map(loanTransactionData -> loanTransactionData.getLoanChargePaidBySummary().getHono())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                final BigDecimal penaltyChargesPaid = invoicedByAccrualTransactionDataList.stream()
                        .filter(LoanTransactionData::isOccurredOnSuspendedAccount).map(LoanTransactionData::getPenaltyChargesPortion)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                loanDocumentData.setInterestPaid(interestPaid);
                loanDocumentData.setMandatoryInsurancePaid(mandatoryInsurancePaid);
                loanDocumentData.setVoluntaryInsurancePaid(voluntaryInsurancePaid);
                loanDocumentData.setHonorariosPaid(honorariosPaid);
                loanDocumentData.setPenaltyChargesPaid(penaltyChargesPaid);
                loanDocumentData.setFirstDayOfMonth(firstDayOfMonth);
                loanDocumentData.setSecondLastDayOfMonth(secondLastDayOfMonth);
                loanDocumentData.setLastDayOfMonth(lastDayOfMonth);
                loanDocumentData.setLoanTransactionId(loanTransactionId);
                if (loanTransactionType.isRepaymentType()) {
                    loanDocumentData.setDocumentType(LoanDocumentData.LoanDocumentType.INVOICE);
                } else {
                    loanDocumentData.setDocumentType(LoanDocumentData.LoanDocumentType.CREDIT_NOTE);
                }
                processAndSaveLoanDocument(loanDocumentData);
                loanTransaction.markAsOccurredOnSuspendedAccount();
                this.loanTransactionRepository.saveAndFlush(loanTransaction);
                invoicedByAccrualTransactionList.forEach(ltx -> ltx.setInvoicedByTransactionId(loanTransactionId));
                this.loanTransactionRepository.saveAll(invoicedByAccrualTransactionList);
            }
        }
    }

    @SuppressWarnings({ "squid:S3776" })
    @Override
    public void processAndSaveLoanDocument(final LoanDocumentData loanDocumentData) {
        final List<FacturaElectronicaMensual> facturaElectronicaMensuals = new ArrayList<>();
        final FacturaElectronicaMensual facturaElectronicaMensual = loanDocumentData.toEntity();
        final Integer itemsCount = loanDocumentData.getItemsCount();
        facturaElectronicaMensual.setTotalUnidades(String.valueOf(itemsCount));
        final BigDecimal interestPaid = loanDocumentData.getInterestPaid();
        final BigDecimal penaltyChargesPaid = loanDocumentData.getPenaltyChargesPaid();
        final BigDecimal mandatoryInsurancePaid = loanDocumentData.getMandatoryInsurancePaid();
        final BigDecimal voluntaryInsurancePaid = loanDocumentData.getVoluntaryInsurancePaid();
        final BigDecimal honorariosPaid = loanDocumentData.getHonorariosPaid();
        final Long productTypeParamId = loanDocumentData.getProductTypeParamId();
        final LoanProductParameterization loanProductParameterization = this.productParameterizationRepository.findById(productTypeParamId)
                .orElseThrow(() -> new LoanProductParameterizationNotFoundException(productTypeParamId));
        final Long rangeStartNumber = loanProductParameterization.getRangeStartNumber();
        final Long invoiceCounter = loanProductParameterization.getInvoiceCounter();
        final Long creditNoteCounter = loanProductParameterization.getCreditNoteCounter();
        final Long rangeEndNumber = loanProductParameterization.getRangeEndNumber();
        long documentNumber;
        Long currentCounter;
        List<FacturaElectronicaMensual> invoicesToKnockOff = new ArrayList<>();
        final LoanDocumentData.LoanDocumentType documentType = loanDocumentData.getDocumentType();
        if (LoanDocumentData.LoanDocumentType.CREDIT_NOTE.equals(documentType)) {
            currentCounter = ObjectUtils.defaultIfNull(creditNoteCounter, 0L) + 1L;
            documentNumber = rangeStartNumber + currentCounter;
            loanProductParameterization.setCreditNoteCounter(currentCounter);
            invoicesToKnockOff = this.facturaElectronicMensualRepository.findById_clienteAndTipo_prod(loanDocumentData.getClientIdNumber(),
                    loanDocumentData.getProductTypeName());
        } else {
            currentCounter = ObjectUtils.defaultIfNull(invoiceCounter, 0L) + 1L;
            documentNumber = rangeStartNumber + currentCounter;
            loanProductParameterization.setInvoiceCounter(currentCounter);
        }
        if (currentCounter > rangeEndNumber) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.invoice.counter.exceeds.range.end.number",
                    String.format("Invoice counter exceeds the range end number: %s and product type: %s", rangeEndNumber,
                            loanProductParameterization.getProductType()));
        }
        facturaElectronicaMensual.setNumeroDoc(String.valueOf(documentNumber));
        facturaElectronicaMensual.setReferencia(String.valueOf(documentNumber));
        facturaElectronicaMensual.setCodigoDescuento("0");
        facturaElectronicaMensual.setPorcentajedescuento(BigDecimal.ZERO);
        facturaElectronicaMensual.setDescuento(BigDecimal.ZERO);
        facturaElectronicaMensual.setPorcentajeImpuestoItem(BigDecimal.ZERO);
        facturaElectronicaMensual.setImpuestoItem(BigDecimal.ZERO);
        long itemPosition = 0L;
        if (interestPaid.compareTo(BigDecimal.ZERO) > 0) {
            final LoanDocumentConcept loanDocumentConcept = LoanDocumentConcept.INT_CORRIENTE;
            final FacturaElectronicaMensual facturaElectronicaMensualDuplicate = facturaElectronicaMensual.clone();
            itemPosition = itemPosition + 1;
            facturaElectronicaMensualDuplicate.setPosicion(itemPosition);
            facturaElectronicaMensualDuplicate.setCostoTotal(interestPaid);
            facturaElectronicaMensualDuplicate.setPrecioUnitario(interestPaid);
            facturaElectronicaMensualDuplicate.setSku(loanDocumentConcept.getSku());
            facturaElectronicaMensualDuplicate.setNomArticulo(loanDocumentConcept.getName());
            facturaElectronicaMensualDuplicate.setIdMandante(null);
            facturaElectronicaMensualDuplicate.setDescripcionMandante(null);
            facturaElectronicaMensuals.add(facturaElectronicaMensualDuplicate);
            final ClasificacionConceptosData clasificacionConceptosData = this.getClasificacionConceptosData(loanDocumentConcept.name());
            if (clasificacionConceptosData != null) {
                if (!clasificacionConceptosData.isExcluido() && clasificacionConceptosData.isGravado()) {
                    final BigDecimal tarifa = clasificacionConceptosData.getTarifa();
                    final BigDecimal impuestoItem = interestPaid.multiply(tarifa).divide(BigDecimal.valueOf(100),
                            MoneyHelper.getRoundingMode());
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuestoItem(tarifa);
                    facturaElectronicaMensualDuplicate.setImpuestoItem(impuestoItem);
                } else {
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuestoItem(null);
                    facturaElectronicaMensualDuplicate.setImpuestoItem(BigDecimal.ZERO);
                }
                if (clasificacionConceptosData.isExento()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(BigDecimal.ZERO);
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(BigDecimal.ZERO);
                } else if (clasificacionConceptosData.isExcluido()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(null);
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(null);
                } else if (clasificacionConceptosData.isGravado()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(clasificacionConceptosData.getTarifa());
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(clasificacionConceptosData.getTarifa());
                }
            }

            if (LoanDocumentData.LoanDocumentType.CREDIT_NOTE.equals(documentType) && !invoicesToKnockOff.isEmpty()) {
                knockOffRecursively(invoicesToKnockOff, facturaElectronicaMensualDuplicate, facturaElectronicaMensuals);
            }

        }
        if (penaltyChargesPaid.compareTo(BigDecimal.ZERO) > 0) {
            final LoanDocumentConcept loanDocumentConcept = LoanDocumentConcept.INT_DE_MORA;
            final FacturaElectronicaMensual facturaElectronicaMensualDuplicate = facturaElectronicaMensual.clone();
            itemPosition = itemPosition + 1;
            facturaElectronicaMensualDuplicate.setPosicion(itemPosition);
            facturaElectronicaMensualDuplicate.setCostoTotal(penaltyChargesPaid);
            facturaElectronicaMensualDuplicate.setPrecioUnitario(penaltyChargesPaid);
            facturaElectronicaMensualDuplicate.setSku(loanDocumentConcept.getSku());
            facturaElectronicaMensualDuplicate.setNomArticulo(loanDocumentConcept.getName());
            facturaElectronicaMensualDuplicate.setIdMandante(null);
            facturaElectronicaMensualDuplicate.setDescripcionMandante(null);
            facturaElectronicaMensuals.add(facturaElectronicaMensualDuplicate);
            final ClasificacionConceptosData clasificacionConceptosData = this.getClasificacionConceptosData(loanDocumentConcept.name());
            if (clasificacionConceptosData != null) {
                if (!clasificacionConceptosData.isExcluido() && clasificacionConceptosData.isGravado()) {
                    final BigDecimal tarifa = clasificacionConceptosData.getTarifa();
                    final BigDecimal impuestoItem = penaltyChargesPaid.multiply(tarifa).divide(BigDecimal.valueOf(100),
                            MoneyHelper.getRoundingMode());
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuestoItem(tarifa);
                    facturaElectronicaMensualDuplicate.setImpuestoItem(impuestoItem);
                } else {
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuestoItem(null);
                    facturaElectronicaMensualDuplicate.setImpuestoItem(BigDecimal.ZERO);
                }
                if (clasificacionConceptosData.isExento()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(BigDecimal.ZERO);
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(BigDecimal.ZERO);
                } else if (clasificacionConceptosData.isExcluido()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(null);
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(null);
                } else if (clasificacionConceptosData.isGravado()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(clasificacionConceptosData.getTarifa());
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(clasificacionConceptosData.getTarifa());
                }
            }
            if (LoanDocumentData.LoanDocumentType.CREDIT_NOTE.equals(documentType) && !invoicesToKnockOff.isEmpty()) {
                knockOffRecursively(invoicesToKnockOff, facturaElectronicaMensualDuplicate, facturaElectronicaMensuals);
            }

        }
        if (mandatoryInsurancePaid.compareTo(BigDecimal.ZERO) > 0) {
            final LoanDocumentConcept loanDocumentConcept = LoanDocumentConcept.SEGURO_OBLIGATORIO;
            final FacturaElectronicaMensual facturaElectronicaMensualDuplicate = facturaElectronicaMensual.clone();
            itemPosition = itemPosition + 1;
            facturaElectronicaMensualDuplicate.setPosicion(itemPosition);
            facturaElectronicaMensualDuplicate.setCostoTotal(mandatoryInsurancePaid);
            facturaElectronicaMensualDuplicate.setPrecioUnitario(mandatoryInsurancePaid);
            facturaElectronicaMensualDuplicate.setSku(loanDocumentConcept.getSku());
            facturaElectronicaMensualDuplicate.setNomArticulo(loanDocumentConcept.getName());
            facturaElectronicaMensuals.add(facturaElectronicaMensualDuplicate);
            final ClasificacionConceptosData clasificacionConceptosData = this.getClasificacionConceptosData(loanDocumentConcept.name());
            if (clasificacionConceptosData != null) {
                final String idMandante = clasificacionConceptosData.isMandato() ? loanDocumentData.getMandatoryInsuranceCode() : null;
                final String descripcionMandante = clasificacionConceptosData.isMandato() ? loanDocumentData.getMandatoryInsuranceName()
                        : null;
                facturaElectronicaMensualDuplicate.setIdMandante(idMandante);
                facturaElectronicaMensualDuplicate.setDescripcionMandante(descripcionMandante);
                if (!clasificacionConceptosData.isExcluido() && clasificacionConceptosData.isGravado()) {
                    final BigDecimal tarifa = clasificacionConceptosData.getTarifa();
                    final BigDecimal impuestoItem = mandatoryInsurancePaid.multiply(tarifa).divide(BigDecimal.valueOf(100),
                            MoneyHelper.getRoundingMode());
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuestoItem(tarifa);
                    facturaElectronicaMensualDuplicate.setImpuestoItem(impuestoItem);
                } else {
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuestoItem(null);
                    facturaElectronicaMensualDuplicate.setImpuestoItem(BigDecimal.ZERO);
                }
                if (clasificacionConceptosData.isExento()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(BigDecimal.ZERO);
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(BigDecimal.ZERO);
                } else if (clasificacionConceptosData.isExcluido()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(null);
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(null);
                } else if (clasificacionConceptosData.isGravado()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(clasificacionConceptosData.getTarifa());
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(clasificacionConceptosData.getTarifa());
                }
            }
            if (LoanDocumentData.LoanDocumentType.CREDIT_NOTE.equals(documentType) && !invoicesToKnockOff.isEmpty()) {
                knockOffRecursively(invoicesToKnockOff, facturaElectronicaMensualDuplicate, facturaElectronicaMensuals);
            }

        }
        if (voluntaryInsurancePaid.compareTo(BigDecimal.ZERO) > 0) {
            final LoanDocumentConcept loanDocumentConcept = LoanDocumentConcept.SEGUROS_VOLUNTARIOS;
            final FacturaElectronicaMensual facturaElectronicaMensualDuplicate = facturaElectronicaMensual.clone();
            itemPosition = itemPosition + 1;
            facturaElectronicaMensualDuplicate.setPosicion(itemPosition);
            facturaElectronicaMensualDuplicate.setCostoTotal(voluntaryInsurancePaid);
            facturaElectronicaMensualDuplicate.setPrecioUnitario(voluntaryInsurancePaid);
            facturaElectronicaMensualDuplicate.setSku(loanDocumentConcept.getSku());
            facturaElectronicaMensualDuplicate.setNomArticulo(loanDocumentConcept.getName());
            facturaElectronicaMensuals.add(facturaElectronicaMensualDuplicate);
            final ClasificacionConceptosData clasificacionConceptosData = this.getClasificacionConceptosData(loanDocumentConcept.name());
            if (clasificacionConceptosData != null) {
                final String idMandante = clasificacionConceptosData.isMandato() ? loanDocumentData.getVoluntaryInsuranceCode() : null;
                final String descripcionMandante = clasificacionConceptosData.isMandato() ? loanDocumentData.getVoluntaryInsuranceName()
                        : null;
                facturaElectronicaMensualDuplicate.setIdMandante(idMandante);
                facturaElectronicaMensualDuplicate.setDescripcionMandante(descripcionMandante);
                if (clasificacionConceptosData.isExento()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(BigDecimal.ZERO);
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(BigDecimal.ZERO);
                } else if (clasificacionConceptosData.isExcluido()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(null);
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(null);
                } else if (clasificacionConceptosData.isGravado()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(clasificacionConceptosData.getTarifa());
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(clasificacionConceptosData.getTarifa());
                }
            }
            if (LoanDocumentData.LoanDocumentType.CREDIT_NOTE.equals(documentType) && !invoicesToKnockOff.isEmpty()) {
                knockOffRecursively(invoicesToKnockOff, facturaElectronicaMensualDuplicate, facturaElectronicaMensuals);
            }

        }
        if (honorariosPaid.compareTo(BigDecimal.ZERO) > 0) {
            final LoanDocumentConcept loanDocumentConcept = LoanDocumentConcept.HONORARIOS;
            final FacturaElectronicaMensual facturaElectronicaMensualDuplicate = facturaElectronicaMensual.clone();
            itemPosition = itemPosition + 1;
            facturaElectronicaMensualDuplicate.setPosicion(itemPosition);
            facturaElectronicaMensualDuplicate.setCostoTotal(honorariosPaid);
            facturaElectronicaMensualDuplicate.setPrecioUnitario(honorariosPaid);
            facturaElectronicaMensualDuplicate.setSku(loanDocumentConcept.getSku());
            facturaElectronicaMensualDuplicate.setNomArticulo(loanDocumentConcept.getName());
            facturaElectronicaMensuals.add(facturaElectronicaMensualDuplicate);
            final ClasificacionConceptosData clasificacionConceptosData = this.getClasificacionConceptosData(loanDocumentConcept.name());
            if (clasificacionConceptosData != null) {
                if (!clasificacionConceptosData.isExcluido() && clasificacionConceptosData.isGravado()) {
                    final BigDecimal tarifa = clasificacionConceptosData.getTarifa();
                    final BigDecimal impuestoItem = honorariosPaid.multiply(tarifa).divide(BigDecimal.valueOf(100),
                            MoneyHelper.getRoundingMode());
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuestoItem(tarifa);
                    facturaElectronicaMensualDuplicate.setImpuestoItem(impuestoItem);
                } else {
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuestoItem(null);
                    facturaElectronicaMensualDuplicate.setImpuestoItem(BigDecimal.ZERO);
                }
                if (clasificacionConceptosData.isExento()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(BigDecimal.ZERO);
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(BigDecimal.ZERO);
                } else if (clasificacionConceptosData.isExcluido()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(null);
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(null);
                } else if (clasificacionConceptosData.isGravado()) {
                    facturaElectronicaMensualDuplicate.setImpuesto(clasificacionConceptosData.getTarifa());
                    facturaElectronicaMensualDuplicate.setPorcentajeImpuesto(clasificacionConceptosData.getTarifa());
                }
            }
            if (LoanDocumentData.LoanDocumentType.CREDIT_NOTE.equals(documentType) && !invoicesToKnockOff.isEmpty()) {
                knockOffRecursively(invoicesToKnockOff, facturaElectronicaMensualDuplicate, facturaElectronicaMensuals);
            }

        }
        final BigDecimal totalImpuestoItem = facturaElectronicaMensuals.stream().map(FacturaElectronicaMensual::getImpuestoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (final FacturaElectronicaMensual facturaElectronicaMensualItem : facturaElectronicaMensuals) {
            final BigDecimal totalValue = facturaElectronicaMensualItem.getTotal().add(totalImpuestoItem);
            facturaElectronicaMensualItem.setTotal(totalValue);
        }
        this.facturaElectronicMensualRepository.saveAllAndFlush(facturaElectronicaMensuals);
        this.productParameterizationRepository.saveAndFlush(loanProductParameterization);
    }

    private void knockOffRecursively(final List<FacturaElectronicaMensual> invoicesToKnockOff,
            final FacturaElectronicaMensual currentElectronicaMensual, final List<FacturaElectronicaMensual> facturaElectronicaMensuals) {
        BigDecimal remainingAmount = currentElectronicaMensual.getCostoTotal();
        if (!invoicesToKnockOff.isEmpty()) {
            for (final FacturaElectronicaMensual invoiceToKnockOff : invoicesToKnockOff) {
                remainingAmount = remainingAmount.subtract(invoiceToKnockOff.getCostoTotal());
                if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    final String lastInvoiceNumber = invoiceToKnockOff.getNumeroDoc();
                    final LocalDate lastInvoiceDate = invoiceToKnockOff.getFechaFactura();
                    currentElectronicaMensual.setNumFacafect(lastInvoiceNumber);
                    currentElectronicaMensual.setFecFacafect(lastInvoiceDate);
                    break;
                } else {
                    final FacturaElectronicaMensual knockOffCreditNote = currentElectronicaMensual.clone();
                    final BigDecimal amountToKnockOff = invoiceToKnockOff.getCostoTotal();
                    currentElectronicaMensual.setCostoTotal(remainingAmount);
                    knockOffCreditNote.setCostoTotal(amountToKnockOff);
                    knockOffCreditNote.setPrecioUnitario(amountToKnockOff);
                    knockOffCreditNote.setNumFacafect(invoiceToKnockOff.getNumeroDoc());
                    knockOffCreditNote.setFecFacafect(invoiceToKnockOff.getFechaFactura());
                    remainingAmount = remainingAmount.subtract(invoiceToKnockOff.getCostoTotal());
                    facturaElectronicaMensuals.add(knockOffCreditNote);
                }
            }
        }
    }

    @SuppressWarnings({ "squid:S247", "squid:S2479" })
    @Override
    public ClasificacionConceptosData getClasificacionConceptosData(final String concepto) {
        final String sql = """
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

    @SuppressWarnings({ "squid:S3776" })
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
                            .equalsIgnoreCase(LoanWritePlatformServiceJpaRepositoryImpl.MAXIMUM_LEGAL_RATE_RECALCULATION)) {
                        rescheduleReasonId = codeValueData.getId();
                        break;
                    }
                }
                final JsonObject rescheduleJsonObject = new JsonObject();
                rescheduleJsonObject.addProperty(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, dateFormat);
                rescheduleJsonObject.addProperty(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, locale);
                rescheduleJsonObject.addProperty(RESCHEDULE_REASON_ID_PARAM, rescheduleReasonId);
                rescheduleJsonObject.addProperty(SUBMITTED_ON_DATE, submittedOnDate);
                rescheduleJsonObject.addProperty(RESCHEDULE_REASON_COMMENT_PARAM,
                        LoanWritePlatformServiceJpaRepositoryImpl.MAXIMUM_LEGAL_RATE_RECALCULATION);
                rescheduleJsonObject.addProperty(ADJUSTED_DUE_DATE_PARAM, "");
                rescheduleJsonObject.addProperty(GRACE_ON_PRINCIPAL_PARAM, "");
                rescheduleJsonObject.addProperty(EXTRA_TERMS, "");
                rescheduleJsonObject.addProperty(NEW_INTEREST_RATE_PARAM, currentRate);
                final String rescheduleFromDateString = DateUtils.format(appliedOnDate, dateFormat, Locale.forLanguageTag(locale));
                rescheduleJsonObject.addProperty(RESCHEDULE_FROM_DATE_PARAM, rescheduleFromDateString);
                rescheduleJsonObject.addProperty(LoanWritePlatformServiceJpaRepositoryImpl.LOAN_ID_PARAM, loanId);
                final String rescheduleRequestBodyAsJson = rescheduleJsonObject.toString();
                CommandWrapper commandWrapper = new CommandWrapperBuilder()
                        .createLoanRescheduleRequest(RescheduleLoansApiConstants.ENTITY_NAME).withJson(rescheduleRequestBodyAsJson).build();

                CommandProcessingResult commandProcessingResult = commandsSourceWritePlatformService.logCommandSource(commandWrapper);
                if (commandProcessingResult.getResourceId() != null) {
                    final Long loanRescheduleId = commandProcessingResult.getResourceId();
                    final JsonObject approvalJsonObject = new JsonObject();
                    final Boolean isJobTriggered = true;
                    approvalJsonObject.addProperty(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, dateFormat);
                    approvalJsonObject.addProperty(LoanWritePlatformServiceJpaRepositoryImpl.LOCALE_PARAM, locale);
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

    @Override
    @Transactional
    public void persistDailyInterestAccrual(final Long loanId, final LocalDate accrualDate) {
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        final String claimType = loan.claimType();
        if (claimType != null && claimType.equalsIgnoreCase("guarantor")) {
            return;
        }

        LocalDate lastAccrualDate = loan.getAccruedTill() != null ? loan.getAccruedTill() : loan.getDisbursementDate();
        // Loop through the days between the last accrual date and accrual date and process interest accrual for each
        // day
        log.info("Persisting daily accrual for loan: {}", loan.getId());
        while (lastAccrualDate.isBefore(accrualDate)) {
            lastAccrualDate = lastAccrualDate.plusDays(1);
            this.processInterestAccrualForDate(lastAccrualDate, loan);
        }
        log.info("Daily accrual persisted for loan: {}", loan.getId());
    }

    @SuppressWarnings({ "squid:S3776" })
    private void processInterestAccrualForDate(final LocalDate accrualDate, Loan loan) {
        final MonetaryCurrency currency = loan.getCurrency();
        ExternalId externalIdentifier = ExternalId.empty();
        if (TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
            externalIdentifier = ExternalId.generate();
        }
        final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments = loan.getRepaymentScheduleInstallments();
        BigDecimal dailyAccrualInterest = null;
        Integer accrualInstallmentNumber = null;
        Money principalLoanBalanceOutstanding = loan.getPrincipal();
        final CollectionData collectionData = this.delinquencyReadPlatformService.calculateLoanCollectionData(loan.getId());
        final Long daysInArrears = collectionData.getPastDueDays();
        Long minimumDaysInArrearsToSuspendLoanAccount = this.configurationDomainService.retriveMinimumDaysInArrearsToSuspendLoanAccount();
        if (minimumDaysInArrearsToSuspendLoanAccount == null) {
            minimumDaysInArrearsToSuspendLoanAccount = 90L;
        }
        for (final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : repaymentScheduleInstallments) {
            if (!accrualDate.isBefore(loanRepaymentScheduleInstallment.getFromDate())
                    && !accrualDate.isAfter(loanRepaymentScheduleInstallment.getDueDate().minusDays(1))) {
                final BigDecimal totalAccruedInterestForInstallment = loan
                        .getAccruedInterestForInstallment(loanRepaymentScheduleInstallment.getInstallmentNumber());
                long daysInPeriod = Math.toIntExact(ChronoUnit.DAYS.between(loanRepaymentScheduleInstallment.getFromDate(),
                        loanRepaymentScheduleInstallment.getDueDate()));
                Money interestForInstallment = loanRepaymentScheduleInstallment.getInterestOutstanding(currency);
                // Adjust interest on the last day of the period to make up the difference
                if (accrualDate.equals(loanRepaymentScheduleInstallment.getDueDate().minusDays(1))) {
                    // if the amount is whole number when divided across the days in the period, then do same for
                    // last
                    // day
                    if (interestForInstallment.getAmount().remainder(BigDecimal.valueOf(daysInPeriod)).compareTo(BigDecimal.ZERO) == 0) {
                        dailyAccrualInterest = interestForInstallment.getAmount().divide(BigDecimal.valueOf(daysInPeriod), 2,
                                RoundingMode.HALF_UP);
                    } else {
                        // This will ensure no rounding differences remain
                        if (interestForInstallment.getAmount().compareTo(totalAccruedInterestForInstallment) > 0) {
                            dailyAccrualInterest = interestForInstallment.getAmount().subtract(totalAccruedInterestForInstallment);
                        } else {
                            dailyAccrualInterest = BigDecimal.ZERO;
                        }
                    }
                } else {
                    dailyAccrualInterest = interestForInstallment.getAmount().divide(BigDecimal.valueOf(daysInPeriod), 2,
                            RoundingMode.HALF_UP);
                    final ScheduleGeneratorDTO scheduleGeneratorDTO = loanUtilService.buildScheduleGeneratorDTO(loan, null);
                    final LoanApplicationTerms loanApplicationTerms = loan.constructLoanApplicationTerms(scheduleGeneratorDTO);
                    final LoanTermVariationsDataWrapper loanTermVariationsDataWrapper = loanApplicationTerms.getLoanTermVariations();
                    if (loanTermVariationsDataWrapper != null) {
                        List<LoanTermVariationsData> interestRatesFromInstallment = loanTermVariationsDataWrapper
                                .getInterestRateFromInstallment();
                        if (interestRatesFromInstallment != null && !interestRatesFromInstallment.isEmpty()) {
                            LocalDate periodStartDate = loanRepaymentScheduleInstallment.getFromDate();
                            LocalDate periodEndDate = accrualDate;
                            final LocalDate periodEndDateFinal = periodEndDate;
                            interestRatesFromInstallment = interestRatesFromInstallment.stream().filter(interestRateVariation -> !DateUtils
                                    .isAfter(interestRateVariation.getTermVariationApplicableFrom(), periodEndDateFinal)).toList();
                            final List<LoanTermVariationsData> sortedInterestRatesFromInstallment = interestRatesFromInstallment.stream()
                                    .sorted(Comparator.comparing(LoanTermVariationsData::getLastModifiedDate,
                                            Comparator.nullsLast(Comparator.reverseOrder())))
                                    .toList();
                            if (CollectionUtils.isNotEmpty(sortedInterestRatesFromInstallment)) {
                                final LoanTermVariationsData interestRateFromInstallment = sortedInterestRatesFromInstallment.get(0);
                                final LocalDate termVariationApplicableFromDate = interestRateFromInstallment
                                        .getTermVariationApplicableFrom();
                                final BigDecimal interestRate = interestRateFromInstallment.getDecimalValue();
                                if (DateUtils.isAfter(termVariationApplicableFromDate, periodStartDate)) {
                                    periodStartDate = termVariationApplicableFromDate;
                                }
                                loanApplicationTerms.updateAnnualNominalInterestRate(interestRate);
                            }
                            final LoanScheduleGenerator loanScheduleGenerator = this.loanScheduleFactory
                                    .create(loanApplicationTerms.getLoanScheduleType(), loanApplicationTerms.getInterestMethod());
                            final int periodNumber = loanRepaymentScheduleInstallment.getInstallmentNumber();
                            if (DateUtils.isEqual(periodStartDate, periodEndDate)) {
                                periodEndDate = periodEndDate.plusDays(1);
                            }
                            final boolean ignoreCurrencyDigitsAfterDecimal = false;
                            final PrincipalInterest principalInterest = loanScheduleGenerator.calculatePrincipalInterestComponents(
                                    principalLoanBalanceOutstanding, loanApplicationTerms, periodNumber, periodStartDate, periodEndDate,
                                    ignoreCurrencyDigitsAfterDecimal);
                            final int daysDifference = Math.toIntExact(ChronoUnit.DAYS.between(periodStartDate, periodEndDate));
                            dailyAccrualInterest = principalInterest.interest().getAmount().divide(BigDecimal.valueOf(daysDifference), 2,
                                    RoundingMode.HALF_UP);
                        }
                    }
                }
                // Accumulate the daily interest to the installment's accrued interest
                dailyAccrualInterest = dailyAccrualInterest.setScale(0, RoundingMode.DOWN);
                final BigDecimal accruedInterest = totalAccruedInterestForInstallment.add(dailyAccrualInterest);
                loanRepaymentScheduleInstallment.setInterestAccrued(accruedInterest);
                accrualInstallmentNumber = loanRepaymentScheduleInstallment.getInstallmentNumber();
                break;
            }
            principalLoanBalanceOutstanding = principalLoanBalanceOutstanding
                    .minus(loanRepaymentScheduleInstallment.getPrincipal(currency));
        }

        if (dailyAccrualInterest != null && dailyAccrualInterest.compareTo(BigDecimal.ZERO) > 0) {
            final Money dailyInterestMoney = Money.of(currency, dailyAccrualInterest);
            final LoanTransaction dailyAccrualTransaction = LoanTransaction.accrueDailyInterest(loan.getOffice(), loan, dailyInterestMoney,
                    accrualDate, externalIdentifier, accrualInstallmentNumber);
            if (daysInArrears >= minimumDaysInArrearsToSuspendLoanAccount) {
                dailyAccrualTransaction.markAsOccurredOnSuspendedAccount();
            }
            loan.addLoanTransaction(dailyAccrualTransaction);
        }
        loan.setInterestAccruedTill(accrualDate);
        loanRepository.saveAndFlush(loan);
    }

    @Override
    @Transactional
    public void persistInstallmentalChargeAccrual(Long loanId, LocalDate localDate, Long minimumDaysInArrearsToSuspendLoanAccount) {
        Loan loan = this.loanAssembler.assembleFrom(loanId);
        log.info("Persisting Installment charge accrual for loan: {}", loan.getId());
        List<LoanCharge> charges = filterInstallmentCharges(loan.getActiveCharges());
        loan.handleChargeAppliedTransactionPerInstallment(charges, localDate);
        loanRepository.saveAndFlush(loan);
        log.info("Installment  charge accrual persisted for loan: {}", loan.getId());
    }

    private List<LoanCharge> filterInstallmentCharges(Set<LoanCharge> charges) {
        return charges.stream()
                .filter(loanCharge -> loanCharge.getCharge().getChargeTimeType().equals(ChargeTimeType.DISBURSEMENT.getValue())
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
                    cumulative = processInsuranceChargeCancellation(cumulative, loan, data, false);
                    InsuranceIncidentNoveltyNews insuranceIncidentNoveltyNews = InsuranceIncidentNoveltyNews.instance(loan, loanCharge,
                            data.installment(), incident, currentDate, cumulative);

                    this.insuranceIncidentNoveltyNewsRepository.saveAndFlush(insuranceIncidentNoveltyNews);
                    saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
                }
            }
        }
    }

    @SuppressWarnings({ "squid:S3776" })
    @Override
    public void temporarySuspendDefaultInsuranceCharges(List<DefaultOrCancelInsuranceInstallmentData> defaultInsuranceIds) {
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
            if ((incident.isMandatory() && loanCharge != null && loanCharge.isMandatoryInsurance())
                    || (incident.isVoluntary() && loanCharge != null && loanCharge.isVoluntaryInsurance())) {
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
            InsuranceIncident incident;
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
            cumulative = processInsuranceChargeCancellation(cumulative, loan, cancelInsuranceInstallmentData, false);

            InsuranceIncidentNoveltyNews insuranceIncidentNoveltyNews = InsuranceIncidentNoveltyNews.instance(loan, loanCharge,
                    cancelInsuranceInstallmentData.installment(), incident, cancellationDate, cumulative);

            this.insuranceIncidentNoveltyNewsRepository.saveAndFlush(insuranceIncidentNoveltyNews);
            saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        }

        return new CommandProcessingResultBuilder().withEntityId(loan.getId()).build();

    }

    @SuppressWarnings({ "squid:S3776" })
    private BigDecimal processInsuranceChargeCancellation(BigDecimal cumulative, Loan loan, DefaultOrCancelInsuranceInstallmentData data,
            boolean isForeClosure) {
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
        String claimType = command.stringValueOfParameterNamed(LoanWritePlatformServiceJpaRepositoryImpl.CLAIM_TYPE_PARAM);
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
        final String claimType = this.fromApiJsonHelper.extractStringNamed(LoanWritePlatformServiceJpaRepositoryImpl.CLAIM_TYPE_PARAM,
                element);
        final ExternalId externalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);
        this.loanEventApiJsonValidator.validateLoanClaim(command.json());
        final Map<String, Object> changes = new LinkedHashMap<>();
        // Got changed to match with the rest of the APIs
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.DATE_FORMAT_PARAM, command.dateFormat());
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.TRANSACTION_DATE_PARAM,
                command.stringValueOfParameterNamed(LoanApiConstants.transactionDateParamName));
        changes.put(LoanWritePlatformServiceJpaRepositoryImpl.CLAIM_TYPE_PARAM, claimType);

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

    private void checkCreditoRotativo(JsonCommand command, Loan loan, LocalDate actualDisbursementDate) {
        if (loan.getLoanProduct().isMultiDisburseLoan()) {
            validateFirstRepaymentDate(loan);
            BigDecimal principal = extractPrincipal(command);
            validateDisbursementAmount(loan, principal);
            processLoanDisbursementDetails(loan, actualDisbursementDate, principal);
        }
    }

    private void validateFirstRepaymentDate(Loan loan) {
        if (loan.getLoanProduct().getName().equalsIgnoreCase(LoanProductType.CREDITO_ROTATIVO.getCode())) {
            LocalDate expectedFirstRepaymentDate = loan.getExpectedFirstRepaymentOnDate();
            if (Objects.isNull(expectedFirstRepaymentDate)) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.creditorotativo.first.repayment.date.mandatory",
                        "First Repayment date shall be provided when product is Credito Rotativo");
            } else if (!loan.isMigratedLoan() && expectedFirstRepaymentDate.getDayOfMonth() != 1
                    && expectedFirstRepaymentDate.getDayOfMonth() != 10 && expectedFirstRepaymentDate.getDayOfMonth() != 20) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan..creditorotativo.first.repayment.date.must.be.day.1.10.20",
                        "Disbursement date must be 1, 10 or 20");
            }
        }
    }

    private BigDecimal extractPrincipal(JsonCommand command) {
        return this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(LoanApiConstants.principalDisbursedParameterName,
                command.parsedJson().getAsJsonObject());
    }

    private void validateDisbursementAmount(Loan loan, BigDecimal principal) {
        // For revolving credit products, we need to track available credit differently
        if (loan.isRevolvingLoan()) {
            BigDecimal disbursementAmountSum = loanDisbursementDetailsRepository.findAllByLoanId(loan.getId()).stream()
                    .filter(disbursed -> !disbursed.isReversed() && Objects.nonNull(disbursed.getDisbursementDate()))
                    .map(LoanDisbursementDetails::getPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal loanTransactionRepaymentSum = loanTransactionRepository.findAllByLoanIdAndTypeOf(loan.getId(), 2).stream()
                    .filter(nR -> !nR.isReversed() && nR.getPrincipalPortion() != null).map(LoanTransaction::getPrincipalPortion)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // For revolving credit, available credit is: approved limit - (disbursements - repayments)
            BigDecimal currentOutstanding = disbursementAmountSum.subtract(loanTransactionRepaymentSum);
            BigDecimal availableCredit = loan.getApprovedPrincipal().subtract(currentOutstanding);

            if (principal.compareTo(availableCredit) > 0) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.disbursement.exceeds.available.credit",
                        "Disbursement amount (".concat(String.valueOf(principal)).concat(") exceeds available credit (")
                                .concat(String.valueOf(availableCredit)).concat("). Current outstanding: ")
                                .concat(String.valueOf(currentOutstanding)).concat(", Approved limit: ")
                                .concat(String.valueOf(loan.getApprovedPrincipal())).concat("."));
            }
        } else {
            // Original validation for non-revolving credit products
            BigDecimal disbursementAmountSum = loanDisbursementDetailsRepository.findAllByLoanId(loan.getId()).stream()
                    .filter(disbursed -> Objects.nonNull(disbursed.getDisbursementDate())).map(LoanDisbursementDetails::getPrincipal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal loanTransactionRepaymentSum = loanTransactionRepository.findAllByLoanIdAndTypeOf(loan.getId(), 2).stream()
                    .filter(nR -> !nR.isReversed()).map(LoanTransaction::getPrincipalPortion).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal loanApprovedPrincipal = loan.getApprovedPrincipal();

            if (disbursementAmountSum.add(principal).subtract(loanTransactionRepaymentSum).compareTo(loanApprovedPrincipal) > 0) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.disbursement.exceeds.approved.principal",
                        "Sum of Loan disbursements (".concat(String.valueOf(disbursementAmountSum)).concat(") + This one (")
                                .concat(String.valueOf(principal)).concat(") - Repayments (")
                                .concat(String.valueOf(loanTransactionRepaymentSum)).concat(") exceeds approved principal (")
                                .concat(String.valueOf(loanApprovedPrincipal)).concat(")."));
            }
        }
    }

    private void processLoanDisbursementDetails(Loan loan, LocalDate actualDisbursementDate, BigDecimal principal) {
        Long nrOfDisbursalsSoFar = loanDisbursementDetailsRepository.findAllByLoanId(loan.getId()).stream()
                .filter(wasDisbursed -> wasDisbursed.getActualDisbursementDate() != null).filter(notReversed -> !notReversed.isReversed())
                .count();

        // If first disbursement was made, create a new disbursement detail for the next ones
        if (nrOfDisbursalsSoFar.compareTo(1L) >= 0) {
            LoanDisbursementDetails details = new LoanDisbursementDetails(actualDisbursementDate, actualDisbursementDate, principal, null,
                    false);
            details.updateLoan(loan);
            loanDisbursementDetailsRepository.save(details);

            loan.addDisbursementDetails(details);
            if (!loan.getLoanProduct().getName().equalsIgnoreCase(LoanProductType.CREDITO_ROTATIVO.getCode())) {
                loan.updateLoanSummaryDerivedFields();
            }
            loanRepository.save(loan);

            loan = this.loanAssembler.assembleFrom(loan.getId());
            Long loanRescheduleReasonId = getLoanRescheduleReasonId();
            if (loan.isRevolvingLoan()) {
                ImmutablePair<Integer, LocalDate> pair = calculateInstallmentsToAdd(loan, actualDisbursementDate);
                if (pair != null && pair.getKey().compareTo(0) > 0) {
                    Integer nrOfInstallmentsToAdd = pair.left;
                    LocalDate variationDate = pair.right;
                    createRescheduleRequestForCreditoRotativo(loan, variationDate, loanRescheduleReasonId, nrOfInstallmentsToAdd);
                }
            } else {
                Integer nrOfInstallmentsToAdd = calculateInstallmentsToAdd(loan);

                if (nrOfInstallmentsToAdd.compareTo(0) > 0) {
                    createRescheduleRequest(loan, actualDisbursementDate, loanRescheduleReasonId, nrOfInstallmentsToAdd);
                }
            }
        }
    }

    private Long getLoanRescheduleReasonId() {
        List<CodeValueData> codeValueList = Lists
                .newArrayList(codeValueReadPlatformService.retrieveCodeValuesByCode("LoanRescheduleReason"));

        return codeValueList.stream().filter(p -> p.getName().equals("Nuevo desembolso de Crédito Rotativo")).findFirst()
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.loan.reschedule.reason.not.found",
                        "Loan reschedule reason not found"))
                .getId();
    }

    private Integer calculateInstallmentsToAdd(Loan loan) {
        Integer productNrOfRepayments = loan.getOriginalNumberOfRepayments();
        Long notPaidInstallmentNr = loan.getRepaymentScheduleInstallments().stream().filter(p -> !p.isObligationsMet()).count();
        Long graceInstallments = loan.getRepaymentScheduleInstallments().stream().filter(LoanRepaymentScheduleInstallment::isFullyGraced)
                .count();
        notPaidInstallmentNr = notPaidInstallmentNr + graceInstallments;

        // For migrated loans that already have more installments than defined in loan spec
        if (loan.isMigratedLoan() && notPaidInstallmentNr >= productNrOfRepayments) {
            return 0; // Don't create new installments
        }

        return Math.max(0, productNrOfRepayments - notPaidInstallmentNr.intValue());
    }

    private ImmutablePair<Integer, LocalDate> calculateInstallmentsToAdd(Loan loan, LocalDate disbursementDate) {
        if (loan.isRevolvingLoan()) {
            Integer actualNumberOfRepayments = loan.getTermFrequency();
            LocalDate lastInstallmentDueDate = loan.getLastLoanRepaymentScheduleInstallment().getDueDate();

            if (RevolvingLoanUtil.isAfterCutoff(disbursementDate, lastInstallmentDueDate)) {

                ImmutablePair<Integer, LocalDate> installmentsToAdd = ImmutablePair.of(actualNumberOfRepayments, lastInstallmentDueDate);
                DisbursementCutoffContext.setAfterCutoff(loan, disbursementDate, installmentsToAdd);
                log.info("Revolving credit: Adding {} new installments starting from {}", actualNumberOfRepayments, disbursementDate);
                return installmentsToAdd;
            } else {
                DisbursementCutoffContext.clear();
            }
            // If not after cutoff, continue to rest of method
        }

        // Original logic for non-revolving or partially paid loans
        LocalDate variationDate = disbursementDate;
        Integer installmentNumberToAddDisbursement = null;
        boolean addVariationToNextInstallment = false;

        for (LoanRepaymentScheduleInstallment installment : loan.getRepaymentScheduleInstallments()) {
            if (installment.getDueDate().isAfter(disbursementDate)) {
                long diff = ChronoUnit.DAYS.between(disbursementDate, installment.getDueDate());
                if (diff < CUTOFF_DAYS) {
                    installmentNumberToAddDisbursement = installment.getInstallmentNumber() + 1;
                    variationDate = installment.getDueDate().plusDays(2);
                    addVariationToNextInstallment = true;
                    log.info("Disbursement within cutoff period. Adding variation to next installment {}. Variation date: {}",
                            installmentNumberToAddDisbursement, variationDate);
                    break;
                } else {
                    installmentNumberToAddDisbursement = installment.getInstallmentNumber();
                    variationDate = installment.getDueDate();
                    log.info("Disbursement after cutoff period. Adding variation to current installment {}. Variation date: {}",
                            installmentNumberToAddDisbursement, variationDate);
                    break;
                }
            }
        }

        if (installmentNumberToAddDisbursement == null) {
            installmentNumberToAddDisbursement = loan.getRepaymentScheduleInstallments().size();

            Optional<LocalDate> lastInstallmentDueDateOpt = loan.getRepaymentScheduleInstallments().stream()
                    .max(Comparator.comparingInt(LoanRepaymentScheduleInstallment::getInstallmentNumber))
                    .map(LoanRepaymentScheduleInstallment::getDueDate);

            if (lastInstallmentDueDateOpt.isPresent()) {
                LocalDate lastInstallmentDueDate = lastInstallmentDueDateOpt.get();
                long diff = ChronoUnit.DAYS.between(lastInstallmentDueDate, disbursementDate);
                if (diff < CUTOFF_DAYS) {
                    installmentNumberToAddDisbursement = loan.getRepaymentScheduleInstallments().size() - 1;
                    log.info("Disbursement within cutoff period of last installment. Creating {} installments",
                            installmentNumberToAddDisbursement);
                } else {
                    log.info("Disbursement after cutoff period of last installment. Creating {} installments",
                            installmentNumberToAddDisbursement);
                }
            }
            variationDate = loan.getLastLoanRepaymentScheduleInstallment().getDueDate();
            return ImmutablePair.of(installmentNumberToAddDisbursement, variationDate);
        }

        Integer actualNumberOfRepayments = loan.getTermFrequency();
        int requiredInstallments = installmentNumberToAddDisbursement + actualNumberOfRepayments - 1;
        if (requiredInstallments == 0) {
            log.info("No installments required to be added");
            return null;
        }

        int installmentsToAdd = 0;
        if (loan.isMigratedLoan() && Objects.nonNull(loan.getMambuNumberOfRepayments())
                && !loan.isSyncedUpNumberOfRepaymentsFromMambuConfig()) {

            if (loan.getMambuNumberOfRepayments()
                    .compareTo((int) loan.getRepaymentScheduleInstallments().stream().filter(l -> !l.isObligationsMet()).count()) < 0) {
                // Case when there are more existing installments in Mambu than defined at loan level - DO NOT SYNC UP
                // and
                // don´t add
                installmentsToAdd = 0;

            } else if (loan.getMambuNumberOfRepayments()
                    .compareTo((int) loan.getRepaymentScheduleInstallments().stream().filter(l -> !l.isObligationsMet()).count()) == 0) {
                // Case when there are same nr of existing installments in Mambu than defined at loan level - SYNC UP
                // and don´t add
                installmentsToAdd = 0;

                loan.syncUpNumberOfRepaymentsFromMambuConfig();
                loanRepository.save(loan);
            } else {
                // Case when there are less existing installments in Mambu than defined at loan level
                installmentsToAdd = (int) loan.getRepaymentScheduleInstallments().stream().filter(l -> l.isObligationsMet()).count();

                loan.syncUpNumberOfRepaymentsFromMambuConfig();
                loanRepository.save(loan);
            }

        } else {
            // Nr of new installments = original size - unpaid installments
            installmentsToAdd = loan.getTermFrequency()
                    - (int) loan.getRepaymentScheduleInstallments().stream().filter(l -> !l.isObligationsMet()).count();
        }

        if (installmentsToAdd <= 0) {
            log.info("No installments required to be added");
            return null;
        }

        log.info("Adding {} new installments starting from date {}", installmentsToAdd, variationDate);
        return ImmutablePair.of(installmentsToAdd, variationDate);
    }

    private void createRescheduleRequest(Loan loan, LocalDate actualDisbursementDate, Long loanRescheduleReasonId,
            Integer nrOfNewInstallments) {
        try {
            JsonCommand createRescheduleRequestCommand = createRescheduleRequestAction(fromApiJsonHelper, null, loan.getId(),
                    actualDisbursementDate, loanRescheduleReasonId, nrOfNewInstallments);
            loanRescheduleRequestWritePlatformService.create(createRescheduleRequestCommand);
        } catch (JsonProcessingException ex) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.does.cannot.create.extension",
                    "Loan could not be automatically create and approve its extension.");
        }
    }

    private void createRescheduleRequestForCreditoRotativo(Loan loan, LocalDate actualDisbursementDate, Long loanRescheduleReasonId,
            Integer nrOfNewInstallments) {
        try {
            JsonCommand createRescheduleRequestCommand = createRescheduleRequestAction(fromApiJsonHelper, null, loan.getId(),
                    actualDisbursementDate, loanRescheduleReasonId, nrOfNewInstallments);
            CommandProcessingResult result = loanRescheduleRequestWritePlatformService.create(createRescheduleRequestCommand);
            Long requestId = result.getResourceId();
            Optional<LoanRescheduleRequest> rescheduleRequestDataOpt = loanRescheduleRequestRepository.findById(requestId);
            if (rescheduleRequestDataOpt.isPresent()) {
                final AppUser appUser = this.platformSecurityContext.authenticatedUser();
                LoanRescheduleRequest request = rescheduleRequestDataOpt.get();
                loan.updateRescheduledByUser(appUser);
                loan.updateRescheduledOnDate(DateUtils.getBusinessLocalDate());
                request.approve(appUser, LocalDate.now());
                loanRescheduleRequestRepository.save(request);
                for (LoanRescheduleRequestToTermVariationMapping mapping : request.getLoanRescheduleRequestToTermVariationMappings()) {
                    LoanTermVariations variation = mapping.getLoanTermVariations();
                    variation.updateIsActive(true);
                    // loan.getLoanTermVariations().add(variation);
                }
                loanRepository.save(loan);
            }
        } catch (JsonProcessingException ex) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.does.cannot.create.extension",
                    "Loan could not be automatically create and approve its extension.");
        }
    }

    @Transactional
    public void approveRescheduleRequest(Long loanId, Long rescheduleRequestId, LocalDate actualDisbursementDate) {
        Loan loan = this.loanAssembler.assembleFrom(loanId);

        if ((loan.getLoanProduct().getName().contains(LoanProductType.CREDITO_ROTATIVO.getCode())
                || loan.getLoanProduct().getName().contains(LoanProductType.NANO_CREDITO.getCode()))
                && loan.getDisbursementDetails().size() > 1) {

            Optional<LoanRescheduleRequest> rescheduleRequestDataOpt = loanRescheduleRequestRepository.findByLoanId(loanId).stream()
                    .filter(notApp -> notApp.getStatusEnum().compareTo(300) != 0).filter(t -> Objects.isNull(t.getApprovedOnDate()))
                    .sorted(Comparator.comparing(LoanRescheduleRequest::getId).reversed()).findFirst();

            if (rescheduleRequestDataOpt.isPresent()) {
                LoanRescheduleRequest curr = rescheduleRequestDataOpt.get();

                try {
                    JsonCommand createRescheduleRequestCommand = approveRescheduleRequestAction(fromApiJsonHelper,
                            curr.getRescheduleFromDate(), curr.getId());
                    loanRescheduleRequestWritePlatformService.approve(createRescheduleRequestCommand);

                    Integer originalNrOfRepayments = loan.getOriginalNumberOfRepayments();
                    Long notPaidInstallmentNr = loan.getRepaymentScheduleInstallments().stream().filter(p -> !p.isObligationsMet()).count();
                    Integer nrOfInstallmentsToAdd = originalNrOfRepayments - notPaidInstallmentNr.intValue();

                    if (nrOfInstallmentsToAdd.compareTo(0) <= 0) {
                        loanRepository.updateRepaymentsAndTermFrequency(originalNrOfRepayments, originalNrOfRepayments, loan.getId());
                    }

                    loan.updateLoanSummaryDerivedFields();

                    loanRepository.save(loan);
                } catch (JsonProcessingException ex) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.does.cannot.create.extension",
                            "Loan could not be automatically create and approve its extension.");
                }
            }
        }
    }

    @Override
    public CommandProcessingResult restructureLoans(Long clientId, JsonCommand command) {
        return null;
    }

    @NotNull
    private JsonCommand createRescheduleRequestAction(FromJsonHelper fromApiJsonHelper, @Nullable String action, Long loanId,
            LocalDate startDate, Long rescheduleReasonIdCodeValueId, Integer nrOfNewInstallments) throws JsonProcessingException {

        Map<String, Object> map = new HashMap<>();

        Optional.ofNullable(action).ifPresent(a -> map.put(ACTION, a));
        map.put(RESCHEDULE_FROM_DATE_PARAM, DateUtils.format(startDate, DateUtils.DEFAULT_DATE_FORMAT));
        map.put(RESCHEDULE_REASON_ID_PARAM, String.valueOf(rescheduleReasonIdCodeValueId));
        map.put(SUBMITTED_ON_DATE, DateUtils.format(startDate, DateUtils.DEFAULT_DATE_FORMAT));
        map.put(RESCHEDULE_REASON_COMMENT_PARAM, "");
        map.put("adjustedDueDate", "");
        map.put("graceOnPrincipal", "");
        map.put("rediferirTerms", "");
        map.put("graceOnInterest", "");
        map.put(EXTRA_TERMS, String.valueOf(nrOfNewInstallments));
        map.put(NEW_INTEREST_RATE_PARAM, "");
        map.put(DATE_FORMAT_PARAM, DateUtils.DEFAULT_DATE_FORMAT);
        map.put(LOCALE_PARAM, "es");
        map.put(LOAN_ID_PARAM, String.valueOf(loanId));

        return createJsonCommand(fromApiJsonHelper, map, loanId);

    }

    @NotNull
    private JsonCommand approveRescheduleRequestAction(FromJsonHelper fromApiJsonHelper, LocalDate startDate, Long rescheduleRequestId)
            throws JsonProcessingException {

        Map<String, Object> map = new HashMap<>();

        map.put("approvedOnDate", DateUtils.format(startDate, DateUtils.DEFAULT_DATE_FORMAT));
        map.put(DATE_FORMAT_PARAM, DateUtils.DEFAULT_DATE_FORMAT);
        map.put(LOCALE_PARAM, "es");

        return createJsonCommand(fromApiJsonHelper, map, rescheduleRequestId);

    }

    @NotNull
    private JsonCommand createJsonCommand(FromJsonHelper fromApiJsonHelper, Map<String, Object> jsonMap, Long loanId)
            throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap);

        return new JsonCommand(fromApiJsonHelper, json, loanId, JsonParser.parseString(json));
    }
}
