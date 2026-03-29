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

import static org.apache.fineract.portfolio.charge.domain.ChargeCustomType.CAPITAL_PENDIENTE_MI_PYME;
import static org.apache.fineract.portfolio.charge.domain.ChargeCustomType.LIFE_INSURANCE;

import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockComponentEnum;
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockData;
import org.apache.fineract.custom.portfolio.blockaccounts.domain.LoanAccountBlock;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.domain.CustomChargeHonorarioMap;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.configuration.service.TemporaryConfigurationServiceContainer;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.JsonParserHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.infrastructure.security.service.RandomPasswordGenerator;
import org.apache.fineract.organisation.holiday.domain.Holiday;
import org.apache.fineract.organisation.holiday.service.HolidayUtil;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.workingdays.domain.WorkingDays;
import org.apache.fineract.organisation.workingdays.service.WorkingDaysUtil;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.calendar.data.CalendarHistoryDataWrapper;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.domain.CalendarHistory;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstance;
import org.apache.fineract.portfolio.calendar.domain.CalendarWeekDaysType;
import org.apache.fineract.portfolio.calendar.service.CalendarUtils;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeCalculationType;
import org.apache.fineract.portfolio.charge.domain.ChargeCustomType;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.charge.exception.LoanChargeCannotBeAddedException;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.collateral.domain.LoanCollateral;
import org.apache.fineract.portfolio.common.domain.DayOfWeekType;
import org.apache.fineract.portfolio.common.domain.NthDayType;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.floatingrates.data.FloatingRateDTO;
import org.apache.fineract.portfolio.floatingrates.data.FloatingRatePeriodData;
import org.apache.fineract.portfolio.fund.domain.Fund;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.command.LoanChargeCommand;
import org.apache.fineract.portfolio.loanaccount.data.DisbursementData;
import org.apache.fineract.portfolio.loanaccount.data.HolidayDetailDTO;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.data.LoanCollateralManagementData;
import org.apache.fineract.portfolio.loanaccount.data.LoanRepaymentScheduleInstallmentData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsDataWrapper;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.LoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.LoanRepaymentScheduleTransactionProcessor.TransactionCtx;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.MoneyHolder;
import org.apache.fineract.portfolio.loanaccount.exception.ExceedingTrancheCountException;
import org.apache.fineract.portfolio.loanaccount.exception.InvalidLoanStateTransitionException;
import org.apache.fineract.portfolio.loanaccount.exception.InvalidLoanTransactionTypeException;
import org.apache.fineract.portfolio.loanaccount.exception.InvalidRefundDateException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanApplicationDateException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanChargeRefundException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanDisbursalException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanForeclosureException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanOfficerAssignmentDateException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanOfficerAssignmentException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanOfficerUnassignmentDateException;
import org.apache.fineract.portfolio.loanaccount.exception.MultiDisbursementDataNotAllowedException;
import org.apache.fineract.portfolio.loanaccount.exception.MultiDisbursementDataRequiredException;
import org.apache.fineract.portfolio.loanaccount.exception.UndoLastTrancheDisbursementException;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleDTO;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.*;
import org.apache.fineract.portfolio.loanaccount.service.DisbursementCutoffContext;
import org.apache.fineract.portfolio.loanproduct.domain.AmortizationMethod;
import org.apache.fineract.portfolio.loanproduct.domain.InterestCalculationPeriodMethod;
import org.apache.fineract.portfolio.loanproduct.domain.InterestMethod;
import org.apache.fineract.portfolio.loanproduct.domain.InterestRecalculationCompoundingMethod;
import org.apache.fineract.portfolio.loanproduct.domain.LoanCustomizationDetail;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRelatedDetail;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductType;
import org.apache.fineract.portfolio.loanproduct.domain.LoanRescheduleStrategyMethod;
import org.apache.fineract.portfolio.loanproduct.domain.RecalculationFrequencyType;
import org.apache.fineract.portfolio.loanproduct.domain.RepaymentStartDateType;
import org.apache.fineract.portfolio.loanproduct.service.LoanEnumerations;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.rate.domain.Rate;
import org.apache.fineract.portfolio.repaymentwithpostdatedchecks.domain.PostDatedChecks;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.util.CollectionUtils;

@SuppressWarnings({ "squid:S6539", "squid:S7027", "squid:S7091" })
@Slf4j
@Entity
@Table(name = "m_loan", uniqueConstraints = { @UniqueConstraint(columnNames = { "account_no" }, name = "loan_account_no_UNIQUE"),
        @UniqueConstraint(columnNames = { "external_id" }, name = "loan_externalid_UNIQUE") })
public class Loan extends AbstractAuditableWithUTCDateTimeCustom {

    public static final String RECALCULATE_LOAN_SCHEDULE = "recalculateLoanSchedule";
    public static final String TRANSACTION_PARAM = "transaction";
    public static final String SUBMITTAL_PARAM = "submittal";
    public static final String ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE = "cannot.be.a.future.date";
    public static final String ERROR_MESSAGE_AMOUNT_EXCEED_THRESHOLD = "amount.exceeds.threshold";
    public static final String ACCOUNT_NO = "accountNo";
    public static final String IN_ARREARS_TOLERANCE = "inArrearsTolerance";
    public static final String CREATE_STANDING_INSTRUCTION_AT_DISBURSEMENT = "createStandingInstructionAtDisbursement";
    public static final String EXTERNAL_ID = "externalId";
    public static final String CLIENT_ID = "clientId";
    public static final String GROUP_ID = "groupId";
    public static final String PRODUCT_ID = "productId";
    public static final String IS_FLOATING_INTEREST_RATE = "isFloatingInterestRate";
    public static final String INTEREST_RATE_DIFFERENTIAL = "interestRateDifferential";
    public static final String FUND_ID = "fundId";
    public static final String LOAN_OFFICER_ID = "loanOfficerId";
    public static final String LOAN_ASSIGNOR_ID = "loanAssignorId";
    public static final String LOAN_PURPOSE_ID = "loanPurposeId";
    public static final String TRANSACTION_PROCESSING_STRATEGY_CODE = "transactionProcessingStrategyCode";
    public static final String SUBMITTED_ON_DATE = "submittedOnDate";
    public static final String DATE_FORMAT = "dateFormat";
    public static final String LOCALE = "locale";
    public static final String EXPECTED_DISBURSEMENT_DATE = "expectedDisbursementDate";
    public static final String REPAYMENTS_STARTING_FROM_DATE = "repaymentsStartingFromDate";
    public static final String SYNC_DISBURSEMENT_WITH_MEETING = "syncDisbursementWithMeeting";
    public static final String INTEREST_CHARGED_FROM_DATE = "interestChargedFromDate";
    public static final String PARAM_CHARGES = "charges";
    public static final String PARAM_COLLATERAL = "collateral";
    public static final String LOAN_TERM_FREQUENCY = "loanTermFrequency";
    public static final String LOAN_TERM_FREQUENCY_TYPE = "loanTermFrequencyType";
    public static final String PRINCIPAL = "principal";
    public static final String PARAM_STATUS = "status";
    public static final String REJECTED_ON_DATE = "rejectedOnDate";
    public static final String CLOSED_ON_DATE = "closedOnDate";
    public static final String EVENT_DATE = "eventDate";
    public static final String WITHDRAWN_ON_DATE = "withdrawnOnDate";
    public static final String APPROVED_ON_DATE = "approvedOnDate";
    public static final String ACTUAL_DISBURSEMENT_DATE = "actualDisbursementDate";
    public static final String INTEREST = "interest";
    public static final String PENALTY = "penalty";
    public static final String TRANSACTION_DATE = "transactionDate";
    public static final String WRITTEN_OFF_ON_DATE = "writtenOffOnDate";
    public static final String FEE = "fee";
    public static final String PENALTIES = "penalties";
    public static final String EARLIEST_UNPAID_DATE = "earliest-unpaid-date";
    public static final String NEXT_UNPAID_DUE_DATE = "next-unpaid-due-date";
    public static final String LOAN_CHARGE_PARAM = "loanCharge";
    public static final String REJECT_PARAM = "reject";
    public static final String WITHDRAW_PARAM = "withdraw";
    public static final String APPROVAL_PARAM = "approval";
    public static final String DISBURSAL_PARAM = "disbursal";
    public static final String WRITE_OFF_PARAM = "writeoff";
    public static final String CLOSE_PARAM = "close";
    public static final String NEW_LOAN_TRANSACTIONS_PARAM = "newLoanTransactions";
    public static final String IS_CHARGE_OFF_PARAM = "isChargeOff";
    public static final String IS_FRAUD_OFF_PARAM = "isFraud";
    public static final String ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_SUBMITTAL_DATE = "cannot.be.before.submittal.date";
    public static final String ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_CLIENT_TRANSFER_DATE = "cannot.be.before.client.transfer.date";
    public static final String ERROR_MESSAGE_LABEL_CANNOT_BE_UNDONE_BEFORE_CLIENT_TRANSFER_DATE = "cannot.be.undone.before.client.transfer.date";
    public static final String ERROR_MESSAGE_LABEL_CANNOT_BE_MADE_BEFORE_CLIENT_TRANSFER_DATE = "cannot.be.made.before.client.transfer.date";
    public static final String ERROR_MESSAGE_LABEL_CANNOT_BE_MADE_BEFORE_LAST_TRANSACTION_DATE = "cannot.be.made.before.last.transaction.date";

    /** Disable optimistic locking till batch jobs failures can be fixed **/
    @Version
    int version;

    @Column(name = "account_no", length = 20, unique = true, nullable = false)
    private String accountNumber;

    @Column(name = "external_id")
    private ExternalId externalId;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = true)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = true)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "glim_id", nullable = true)
    private GroupLoanIndividualMonitoringAccount glim;

    @Column(name = "loan_type_enum", nullable = false)
    private Integer loanType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct loanProduct;

    @ManyToOne(optional = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "fund_id", nullable = true)
    private Fund fund;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "loan_officer_id", nullable = true)
    private Staff loanOfficer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loanpurpose_cv_id", nullable = true)
    private CodeValue loanPurpose;

    @Column(name = "loan_transaction_strategy_code", nullable = false)
    private String transactionProcessingStrategyCode;

    @Column(name = "loan_transaction_strategy_name")
    private String transactionProcessingStrategyName;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LoanPaymentAllocationRule> paymentAllocationRules = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LoanCreditAllocationRule> creditAllocationRules = new ArrayList<>();

    @Embedded
    private LoanProductRelatedDetail loanRepaymentScheduleDetail;

    @Setter
    @Column(name = "term_frequency", nullable = false)
    private Integer termFrequency;

    @Column(name = "term_period_frequency_enum", nullable = false)
    private Integer termPeriodFrequencyType;

    @Column(name = "loan_status_id", nullable = false)
    private Integer loanStatus;

    @Column(name = "sync_disbursement_with_meeting", nullable = true)
    private Boolean syncDisbursementWithMeeting;

    // loan application states
    @Column(name = "submittedon_date")
    private LocalDate submittedOnDate;
    @Column(name = "rejectedon_date")
    private LocalDate rejectedOnDate;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "rejectedon_userid", nullable = true)
    private AppUser rejectedBy;

    @Column(name = "withdrawnon_date")
    private LocalDate withdrawnOnDate;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "withdrawnon_userid", nullable = true)
    private AppUser withdrawnBy;

    @Column(name = "approvedon_date")
    private LocalDate approvedOnDate;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "approvedon_userid", nullable = true)
    private AppUser approvedBy;

    @Column(name = "expected_disbursedon_date")
    private LocalDate expectedDisbursementDate;

    @Column(name = "disbursedon_date")
    private LocalDate actualDisbursementDate;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "disbursedon_userid", nullable = true)
    private AppUser disbursedBy;

    @Column(name = "closedon_date")
    private LocalDate closedOnDate;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "closedon_userid", nullable = true)
    private AppUser closedBy;

    @Column(name = "writtenoffon_date")
    private LocalDate writtenOffOnDate;

    @Column(name = "rescheduledon_date")
    private LocalDate rescheduledOnDate;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "rescheduledon_userid", nullable = true)
    private AppUser rescheduledByUser;

    @Column(name = "expected_maturedon_date")
    private LocalDate expectedMaturityDate;

    @Column(name = "maturedon_date")
    private LocalDate actualMaturityDate;

    @Column(name = "expected_firstrepaymenton_date")
    private LocalDate expectedFirstRepaymentOnDate;

    @Column(name = "interest_calculated_from_date")
    private LocalDate interestChargedFromDate;

    @Column(name = "total_overpaid_derived", scale = 6, precision = 19)
    private BigDecimal totalOverpaid;

    @Column(name = "overpaidon_date")
    private LocalDate overpaidOnDate;

    @Column(name = "loan_counter")
    private Integer loanCounter;

    @Column(name = "loan_product_counter")
    private Integer loanProductCounter;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LoanCharge> charges = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LoanTrancheCharge> trancheCharges = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LoanCollateral> collateral = null;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LoanCollateralManagement> loanCollateralManagements = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LoanOfficerAssignmentHistory> loanOfficerHistory;

    @OrderBy(value = "installmentNumber")
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments = new ArrayList<>();

    @OrderBy(value = "dateOf, createdDate, id")
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LoanTransaction> loanTransactions = new ArrayList<>();

    @Embedded
    private LoanSummary summary;

    @Transient
    private boolean accountNumberRequiresAutoGeneration = false;
    @Transient
    private transient LoanRepaymentScheduleTransactionProcessorFactory transactionProcessorFactory;

    @Transient
    private transient LoanLifecycleStateMachine loanLifecycleStateMachine;
    @Transient
    private transient LoanSummaryWrapper loanSummaryWrapper;

    @Column(name = "principal_amount_proposed", scale = 6, precision = 19, nullable = false)
    private BigDecimal proposedPrincipal;

    @Column(name = "approved_principal", scale = 6, precision = 19, nullable = false)
    private BigDecimal approvedPrincipal;

    @Column(name = "net_disbursal_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal netDisbursalAmount;

    @Column(name = "fixed_emi_amount", scale = 6, precision = 19, nullable = true)
    private BigDecimal fixedEmiAmount;

    @Column(name = "max_outstanding_loan_balance", scale = 6, precision = 19, nullable = true)
    private BigDecimal maxOutstandingLoanBalance;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy(value = "expectedDisbursementDate, id")
    private List<LoanDisbursementDetails> disbursementDetails = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PostDatedChecks> postDatedChecks = new ArrayList<>();

    @OrderBy(value = "termApplicableFrom, id")
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LoanTermVariations> loanTermVariations = new ArrayList<>();

    @Column(name = "total_recovered_derived", scale = 6, precision = 19)
    private BigDecimal totalRecovered;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "loan", optional = true, orphanRemoval = true, fetch = FetchType.LAZY)
    private LoanInterestRecalculationDetails loanInterestRecalculationDetails;

    @Column(name = "is_npa", nullable = false)
    private boolean isNpa;

    @Column(name = "accrued_till")
    private LocalDate accruedTill;

    @Column(name = "create_standing_instruction_at_disbursement", nullable = true)
    private Boolean createStandingInstructionAtDisbursement;

    @Column(name = "guarantee_amount_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal guaranteeAmountDerived;

    @Column(name = "interest_recalcualated_on")
    private LocalDate interestRecalculatedOn;

    @Column(name = "is_floating_interest_rate", nullable = true)
    private Boolean isFloatingInterestRate;

    @Column(name = "interest_rate_differential", scale = 6, precision = 19, nullable = true)
    private BigDecimal interestRateDifferential;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writeoff_reason_cv_id", nullable = true)
    private CodeValue writeOffReason;

    @Setter
    @Column(name = "loan_sub_status_id", nullable = true)
    private Integer loanSubStatus;

    @Column(name = "is_topup", nullable = false)
    private boolean isTopup = false;

    @Column(name = "is_fraud", nullable = false)
    private boolean fraud = false;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "loan", optional = true, orphanRemoval = true, fetch = FetchType.LAZY)
    private LoanTopupDetails loanTopupDetails;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "m_loan_rate", joinColumns = @JoinColumn(name = "loan_id"), inverseJoinColumns = @JoinColumn(name = "rate_id"))
    private List<Rate> rates;

    @Column(name = "fixed_principal_percentage_per_installment", scale = 2, precision = 5, nullable = true)
    private BigDecimal fixedPrincipalPercentagePerInstallment;

    @Column(name = "last_closed_business_date")
    private LocalDate lastClosedBusinessDate;

    @Column(name = "is_charged_off", nullable = false)
    private boolean chargedOff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_off_reason_cv_id")
    private CodeValue chargeOffReason;

    @Column(name = "charged_off_on_date")
    private LocalDate chargedOffOnDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charged_off_by_userid")
    private AppUser chargedOffBy;

    @Column(name = "enable_installment_level_delinquency", nullable = false)
    private boolean enableInstallmentLevelDelinquency = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "loan_assignor_id")
    private Client loanAssignor;

    @Column(name = "valor_descuento")
    private BigDecimal valorDescuento;

    @Column(name = "valor_giro")
    private BigDecimal valorGiro;

    @Embedded
    private transient LoanCustomizationDetail loanCustomizationDetail;

    @Column(name = "excluded_for_insurance_claim")
    private String excludedForInsuranceClaim;

    @Column(name = "excluded_for_aval_claim")
    private String excludedForAvalClaim;

    @Column(name = "excluded_for_castigado_claim")
    private String excludedForCastigadoClaim;
    // This attribute is used only to capture the repayment strategy (VERTICAL/HORIZONTAL). Updating all the related
    // methods to add this attribute as a parameter was creating a mess
    @Transient
    LoanScheduleProcessingType repaymentTransactionProcessingType;

    // This attribute is used only to capture the the checkbox (Reduce Installment Amount) value from repayment form.
    // Updating all the related
    // methods to add this attribute as a parameter was creating a mess
    @Transient
    boolean recalculateEMI;
    @Transient
    private boolean reduceTerm;

    @Column(name = "claim_type")
    private String claimType;

    @Column(name = "claimed_date")
    private LocalDate claimDate;

    @Column(name = "last_installment_charge_calculated_on")
    private Integer lastInstallmentChargeCalculatedOn;

    @Column(name = "interest_accrued_till")
    private LocalDate interestAccruedTill;

    @Transient
    private boolean isAnulado;

    @Transient
    private boolean isAnuladoOnDisbursementDate;

    // Columns for migrated loans
    @Column(name = "is_migrated_loan", nullable = false)
    private boolean isMigratedLoan = false;

    @Column(name = "migrar_nit")
    private String nit;

    @Column(name = "migrar_code")
    private String code;

    @Column(name = "migrar_numerocredito")
    private String numeroCredito;

    @Column(name = "migrar_cli_nroid")
    private String cedula;

    @Getter
    @Column(name = "original_number_of_repayments")
    private Integer originalNumberOfRepayments;

    @Getter
    @Column(name = "mambu_number_of_repayments")
    private Integer mambuNumberOfRepayments;

    @Getter
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loan", orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LoanAccountBlock> loanAccountBlocks;

    @SuppressWarnings({ "squid:S107" })
    public static Loan newIndividualLoanApplication(final String accountNo, final Client client, final Integer loanType,
            final LoanProduct loanProduct, final Fund fund, final Staff officer, final CodeValue loanPurpose,
            final String transactionProcessingStrategyCode, final LoanProductRelatedDetail loanRepaymentScheduleDetail,
            final Set<LoanCharge> loanCharges, final Set<LoanCollateralManagement> collateral, final BigDecimal fixedEmiAmount,
            final List<LoanDisbursementDetails> disbursementDetails, final BigDecimal maxOutstandingLoanBalance,
            final Boolean createStandingInstructionAtDisbursement, final Boolean isFloatingInterestRate,
            final BigDecimal interestRateDifferential, final List<Rate> rates, final BigDecimal fixedPrincipalPercentagePerInstallment,
            final LoanCustomizationDetail loanAdditionalDetail) {
        return new Loan(accountNo, client, null, loanType, fund, officer, loanPurpose, transactionProcessingStrategyCode, loanProduct,
                loanRepaymentScheduleDetail, null, loanCharges, collateral, null, fixedEmiAmount, disbursementDetails,
                maxOutstandingLoanBalance, createStandingInstructionAtDisbursement, isFloatingInterestRate, interestRateDifferential, rates,
                fixedPrincipalPercentagePerInstallment, loanAdditionalDetail);
    }

    @SuppressWarnings({ "squid:S107" })
    public static Loan newGroupLoanApplication(final String accountNo, final Group group, final Integer loanType,
            final LoanProduct loanProduct, final Fund fund, final Staff officer, final CodeValue loanPurpose,
            final String transactionProcessingStrategyCode, final LoanProductRelatedDetail loanRepaymentScheduleDetail,
            final Set<LoanCharge> loanCharges, final Set<LoanCollateralManagement> collateral, final Boolean syncDisbursementWithMeeting,
            final BigDecimal fixedEmiAmount, final List<LoanDisbursementDetails> disbursementDetails,
            final BigDecimal maxOutstandingLoanBalance, final Boolean createStandingInstructionAtDisbursement,
            final Boolean isFloatingInterestRate, final BigDecimal interestRateDifferential, final List<Rate> rates,
            final BigDecimal fixedPrincipalPercentagePerInstallment, final LoanCustomizationDetail loanAdditionalDetail) {
        return new Loan(accountNo, null, group, loanType, fund, officer, loanPurpose, transactionProcessingStrategyCode, loanProduct,
                loanRepaymentScheduleDetail, null, loanCharges, collateral, syncDisbursementWithMeeting, fixedEmiAmount,
                disbursementDetails, maxOutstandingLoanBalance, createStandingInstructionAtDisbursement, isFloatingInterestRate,
                interestRateDifferential, rates, fixedPrincipalPercentagePerInstallment, loanAdditionalDetail);
    }

    @SuppressWarnings({ "squid:S107" })
    public static Loan newIndividualLoanApplicationFromGroup(final String accountNo, final Client client, final Group group,
            final Integer loanType, final LoanProduct loanProduct, final Fund fund, final Staff officer, final CodeValue loanPurpose,
            final String transactionProcessingStrategyCode, final LoanProductRelatedDetail loanRepaymentScheduleDetail,
            final Set<LoanCharge> loanCharges, final Set<LoanCollateralManagement> collateral, final Boolean syncDisbursementWithMeeting,
            final BigDecimal fixedEmiAmount, final List<LoanDisbursementDetails> disbursementDetails,
            final BigDecimal maxOutstandingLoanBalance, final Boolean createStandingInstructionAtDisbursement,
            final Boolean isFloatingInterestRate, final BigDecimal interestRateDifferential, final List<Rate> rates,
            final BigDecimal fixedPrincipalPercentagePerInstallment, final LoanCustomizationDetail loanAdditionalDetail) {
        return new Loan(accountNo, client, group, loanType, fund, officer, loanPurpose, transactionProcessingStrategyCode, loanProduct,
                loanRepaymentScheduleDetail, null, loanCharges, collateral, syncDisbursementWithMeeting, fixedEmiAmount,
                disbursementDetails, maxOutstandingLoanBalance, createStandingInstructionAtDisbursement, isFloatingInterestRate,
                interestRateDifferential, rates, fixedPrincipalPercentagePerInstallment, loanAdditionalDetail);
    }

    protected Loan() {
        this.client = null;
    }

    @SuppressWarnings({ "squid:S107" })
    private Loan(final String accountNo, final Client client, final Group group, final Integer loanType, final Fund fund,
            final Staff loanOfficer, final CodeValue loanPurpose, final String transactionProcessingStrategyCode,
            final LoanProduct loanProduct, final LoanProductRelatedDetail loanRepaymentScheduleDetail, final LoanStatus loanStatus,
            final Set<LoanCharge> loanCharges, final Set<LoanCollateralManagement> collateral, final Boolean syncDisbursementWithMeeting,
            final BigDecimal fixedEmiAmount, final List<LoanDisbursementDetails> disbursementDetails,
            final BigDecimal maxOutstandingLoanBalance, final Boolean createStandingInstructionAtDisbursement,
            final Boolean isFloatingInterestRate, final BigDecimal interestRateDifferential, final List<Rate> rates,
            final BigDecimal fixedPrincipalPercentagePerInstallment, final LoanCustomizationDetail loanCustomizationDetail) {

        this.loanRepaymentScheduleDetail = loanRepaymentScheduleDetail;
        this.loanRepaymentScheduleDetail.validateRepaymentPeriodWithGraceSettings();

        this.isFloatingInterestRate = isFloatingInterestRate;
        this.interestRateDifferential = interestRateDifferential;

        if (StringUtils.isBlank(accountNo)) {
            this.accountNumber = new RandomPasswordGenerator(19).generate();
            this.accountNumberRequiresAutoGeneration = true;
        } else {
            this.accountNumber = accountNo;
        }
        this.client = client;
        this.group = group;
        this.loanType = loanType;
        this.fund = fund;
        this.loanOfficer = loanOfficer;
        this.loanPurpose = loanPurpose;

        this.transactionProcessingStrategyCode = transactionProcessingStrategyCode;
        this.loanProduct = loanProduct;
        if (loanStatus != null) {
            this.loanStatus = loanStatus.getValue();
        } else {
            this.loanStatus = null;
        }
        if (loanCharges != null && !loanCharges.isEmpty()) {
            this.charges = associateChargesWithThisLoan(loanCharges);
            this.summary = updateSummaryWithTotalFeeChargesDueAtDisbursement(deriveSumTotalOfChargesDueAtDisbursement());
        } else {
            this.charges = null;
            this.summary = new LoanSummary();
        }

        if (loanType.equals(1) && collateral != null && !collateral.isEmpty()) {
            this.loanCollateralManagements = associateWithThisLoan(collateral);
        } else {
            this.loanCollateralManagements = null;
        }
        this.loanOfficerHistory = null;

        this.syncDisbursementWithMeeting = syncDisbursementWithMeeting;
        this.fixedEmiAmount = fixedEmiAmount;
        this.maxOutstandingLoanBalance = maxOutstandingLoanBalance;
        this.disbursementDetails = disbursementDetails;
        this.approvedPrincipal = this.loanRepaymentScheduleDetail.getPrincipal().getAmount();
        this.createStandingInstructionAtDisbursement = createStandingInstructionAtDisbursement;

        /*
         * During loan origination stage and before loan is approved principal_amount, approved_principal and
         * principal_amount_demanded will same amount and that amount is same as applicant loan demanded amount.
         */

        this.proposedPrincipal = this.loanRepaymentScheduleDetail.getPrincipal().getAmount();

        // rates added here
        this.rates = rates;
        this.fixedPrincipalPercentagePerInstallment = fixedPrincipalPercentagePerInstallment;

        // Add net get net disbursal amount from charges and principal
        this.netDisbursalAmount = this.approvedPrincipal.subtract(deriveSumTotalOfChargesDueAtDisbursement());

    }

    public Integer getNumberOfRepayments() {
        return this.loanRepaymentScheduleDetail.getNumberOfRepayments();
    }

    public void syncUpNumberOfRepaymentsFromMambuConfig() {
        this.loanRepaymentScheduleDetail.setNumberOfRepayments(this.mambuNumberOfRepayments);
        this.termFrequency = this.mambuNumberOfRepayments;
        this.originalNumberOfRepayments = this.mambuNumberOfRepayments;
    }

    public boolean isSyncedUpNumberOfRepaymentsFromMambuConfig() {
        return this.mambuNumberOfRepayments != null && this.originalNumberOfRepayments != null
                && this.mambuNumberOfRepayments.equals(getNumberOfRepayments());
    }

    private LoanSummary updateSummaryWithTotalFeeChargesDueAtDisbursement(final BigDecimal feeChargesDueAtDisbursement) {
        if (this.summary == null) {
            this.summary = LoanSummary.create(feeChargesDueAtDisbursement);
        } else {
            this.summary.updateTotalFeeChargesDueAtDisbursement(feeChargesDueAtDisbursement);
        }
        return this.summary;
    }

    public void updateLoanSummaryForUndoWaiveCharge(final BigDecimal amountWaived, final boolean isPenalty) {
        if (isPenalty) {
            this.summary.updatePenaltyChargesWaived(this.summary.getTotalPenaltyChargesWaived().subtract(amountWaived));
            this.summary.updatePenaltyChargeOutstanding(this.summary.getTotalPenaltyChargesOutstanding().add(amountWaived));
        } else {
            this.summary.updateFeeChargesWaived(this.summary.getTotalFeeChargesWaived().subtract(amountWaived));
            this.summary.updateFeeChargeOutstanding(this.summary.getTotalFeeChargesOutstanding().add(amountWaived));
        }
        this.summary.updateTotalOutstanding(this.summary.getTotalOutstanding().add(amountWaived));
        this.summary.updateTotalWaived(this.summary.getTotalWaived().subtract(amountWaived));
    }

    private BigDecimal deriveSumTotalOfChargesDueAtDisbursement() {

        Money chargesDue = Money.of(getCurrency(), BigDecimal.ZERO);

        for (final LoanCharge charge : getActiveCharges()) {
            if (charge.isDueAtDisbursement()) {
                chargesDue = chargesDue.plus(charge.amount());
            }
        }

        return chargesDue.getAmount();
    }

    private Set<LoanCharge> associateChargesWithThisLoan(final Set<LoanCharge> loanCharges) {
        for (final LoanCharge loanCharge : loanCharges) {
            loanCharge.update(this);
            if (loanCharge.getTrancheDisbursementCharge() != null) {
                addTrancheLoanCharge(loanCharge.getCharge());
            }
        }
        return loanCharges;
    }

    private Set<LoanCollateralManagement> associateWithThisLoan(final Set<LoanCollateralManagement> collateral) {
        for (final LoanCollateralManagement item : collateral) {
            item.setLoan(this);
        }
        return collateral;
    }

    public boolean isAccountNumberRequiresAutoGeneration() {
        return this.accountNumberRequiresAutoGeneration;
    }

    public void setAccountNumberRequiresAutoGeneration(final boolean accountNumberRequiresAutoGeneration) {
        this.accountNumberRequiresAutoGeneration = accountNumberRequiresAutoGeneration;
    }

    public void addLoanCharge(final LoanCharge loanCharge) {

        if (isChargesAdditionAllowed() && loanCharge.isDueAtDisbursement()) {
            // Note: added this constraint to restrict adding disbursement
            // charges to a loan
            // after it is disbursed
            // if the loan charge payment type is 'Disbursement'.
            // To undo this constraint would mean resolving how charges due are
            // disbursement are handled at present.
            // When a loan is disbursed and has charges due at disbursement, a
            // transaction is created to auto record
            // payment of the charges (user has no choice in saying they were or
            // werent paid) - so its assumed they were paid.

            final String defaultUserMessage = "This charge which is due at disbursement cannot be added as the loan is already disbursed.";
            throw new LoanChargeCannotBeAddedException(Loan.LOAN_CHARGE_PARAM, "due.at.disbursement.and.loan.is.disbursed",
                    defaultUserMessage, getId(), loanCharge.name());
        }

        validateChargeHasValidSpecifiedDateIfApplicable(loanCharge, getDisbursementDate());

        loanCharge.update(this);

        final BigDecimal amount = calculateAmountPercentageAppliedTo(loanCharge);
        BigDecimal chargeAmt;
        BigDecimal totalChargeAmt = BigDecimal.ZERO;
        if (loanCharge.getChargeCalculation().isPercentageBased()) {
            chargeAmt = loanCharge.getPercentage();
            if (loanCharge.isInstalmentFee()) {
                totalChargeAmt = calculatePerInstallmentChargeAmount(loanCharge);
            } else if (loanCharge.isOverdueInstallmentCharge()) {
                totalChargeAmt = loanCharge.amountOutstanding();
            }
        } else {
            if (loanCharge.isCustomFlatDistributedCharge()) {
                chargeAmt = loanCharge.getAmount(this.getCurrency()).getAmount();
            } else {
                chargeAmt = loanCharge.amountOrPercentage();
            }
        }
        loanCharge.update(chargeAmt, loanCharge.getDueLocalDate(), amount, fetchNumberOfInstallmensAfterExceptions(), totalChargeAmt);

        // NOTE: must add new loan charge to set of loan charges before
        // reporcessing the repayment schedule.
        if (this.charges == null) {
            this.charges = new HashSet<>();
        }

        this.charges.add(loanCharge);

        this.summary = updateSummaryWithTotalFeeChargesDueAtDisbursement(deriveSumTotalOfChargesDueAtDisbursement());

        // store Id's of existing loan transactions and existing reversed loan
        // transactions
        final SingleLoanChargeRepaymentScheduleProcessingWrapper wrapper = new SingleLoanChargeRepaymentScheduleProcessingWrapper();
        wrapper.reprocess(getCurrency(), getDisbursementDate(), getRepaymentScheduleInstallments(), loanCharge);
        updateLoanSummaryDerivedFields();

        loanLifecycleStateMachine.transition(LoanEvent.LOAN_CHARGE_ADDED, this);
    }

    public ChangedTransactionDetail reprocessTransactions() {
        ChangedTransactionDetail changedTransactionDetail = null;
        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retrieveListOfTransactionsPostDisbursement();
        changedTransactionDetail = loanRepaymentScheduleTransactionProcessor.reprocessLoanTransactions(getDisbursementDate(),
                allNonContraTransactionsPostDisbursement, getCurrency(), getRepaymentScheduleInstallments(), getActiveCharges());
        for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {

            mapEntry.getValue().updateLoan(this);
        }
        this.loanTransactions.addAll(changedTransactionDetail.getNewTransactionMappings().values());
        updateLoanSummaryDerivedFields();
        return changedTransactionDetail;
    }

    // just like handleChargeAppliedTransaction , create a new method to handle charges per installment using locan
    // charge installement
    public void handleChargeAppliedTransactionPerInstallment(final List<LoanCharge> charges, final LocalDate suppliedTransactionDate) {
        for (LoanCharge loanCharge : charges) {
            validateLoanIsNotClosed(loanCharge);
            validateLoanChargeIsNotWaived(loanCharge);
        }

        // get only installements beteween the transaction date and current date
        LocalDate currentDate = DateUtils.getLocalDateOfTenant();
        List<LoanRepaymentScheduleInstallment> applicableInstallments = getRepaymentScheduleInstallments().stream()
                .filter(installment -> installment.getInstallmentNumber() > 0 && // Exclude the graced installment
                        ( // The installment overlaps with the date range
                        (!installment.getDueDate().isBefore(suppliedTransactionDate) && !installment.getFromDate().isAfter(currentDate)) ||
                        // Or the installment starts on the current date
                                installment.getFromDate().equals(currentDate)))
                .sorted(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).toList();

        for (LoanRepaymentScheduleInstallment installment : applicableInstallments) {
            int installmentNumber = installment.getInstallmentNumber();
            Integer lastProcessedInstallmentNumber = getLastInstallmentChargeCalculatedOn();
            if (lastProcessedInstallmentNumber == null) {
                lastProcessedInstallmentNumber = 0;
            }
            if (installmentNumber == 0 || installmentNumber <= lastProcessedInstallmentNumber) {
                continue;
            }

            for (LoanCharge loanCharge : charges) {
                LoanInstallmentCharge loanInstallmentCharge = loanCharge.getInstallmentLoanCharge(installmentNumber);
                LocalDate installmentDate = installment.getFromDate();
                applyInstallmentCharge(loanInstallmentCharge, installment, loanCharge, installmentDate);
            }
            // get amount to be paid for the instalment

            this.setLastInstallmentChargeCalculatedOn(installmentNumber);

        }

    }

    private void applyInstallmentCharge(LoanInstallmentCharge loanInstallmentCharge, LoanRepaymentScheduleInstallment installment,
            final LoanCharge loanCharge, final LocalDate suppliedTransactionDate) {
        if (loanInstallmentCharge == null) {
            return;
        }
        final Money chargeAmount = loanInstallmentCharge.getAmount(getCurrency());
        if (chargeAmount.isZero()) {
            return;
        }
        final Integer installmentNumber = installment.getInstallmentNumber();
        Money feeCharges = chargeAmount;
        Money penaltyCharges = Money.zero(loanCurrency());
        if (loanCharge.isPenaltyCharge()) {
            penaltyCharges = chargeAmount;
            feeCharges = Money.zero(loanCurrency());
        }

        LocalDate transactionDate = null;

        if (suppliedTransactionDate != null) {
            transactionDate = suppliedTransactionDate;
        } else {
            transactionDate = loanCharge.getDueLocalDate();
            final LocalDate currentDate = DateUtils.getBusinessLocalDate();

            // if loan charge is to be applied on a future date, the loan transaction would show today's date as applied
            // date
            if (transactionDate == null || DateUtils.isAfter(transactionDate, currentDate)) {
                transactionDate = currentDate;
            }
        }
        ExternalId externalIdEmpty = ExternalId.empty();
        if (TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
            externalIdEmpty = ExternalId.generate();
        }
        // validate if the charge has already been paid for the installment
        // we can check getLoanChargesPaid and return if the charge has already been paid

        final LoanTransaction applyLoanChargeTransaction = LoanTransaction.accrueInstallmentCharge(this, getOffice(), chargeAmount,
                transactionDate, feeCharges, penaltyCharges, externalIdEmpty);

        final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(applyLoanChargeTransaction, loanCharge, chargeAmount.getAmount(),
                installmentNumber);
        applyLoanChargeTransaction.getLoanChargesPaid().add(loanChargePaidBy);
        addLoanTransaction(applyLoanChargeTransaction);
    }

    /**
     * Creates a loanTransaction for "Apply Charge Event" with transaction date set to "suppliedTransactionDate". The
     * newly created transaction is also added to the Loan on which this method is called.
     *
     * If "suppliedTransactionDate" is not passed Id, the transaction date is set to the loans due date if the due date
     * is lesser than todays date. If not, the transaction date is set to todays date
     *
     * @param loanCharge
     * @param suppliedTransactionDate
     * @return
     */
    public LoanTransaction handleChargeAppliedTransaction(final LoanCharge loanCharge, final LocalDate suppliedTransactionDate) {
        final Money chargeAmount = loanCharge.getAmount(getCurrency());
        Money feeCharges = chargeAmount;
        Money penaltyCharges = Money.zero(loanCurrency());
        if (loanCharge.isPenaltyCharge()) {
            penaltyCharges = chargeAmount;
            feeCharges = Money.zero(loanCurrency());
        }

        LocalDate transactionDate = null;

        if (suppliedTransactionDate != null) {
            transactionDate = suppliedTransactionDate;
        } else {
            transactionDate = loanCharge.getDueLocalDate();
            final LocalDate currentDate = DateUtils.getBusinessLocalDate();

            // if loan charge is to be applied on a future date, the loan transaction would show today's date as applied
            // date
            if (transactionDate == null || DateUtils.isAfter(transactionDate, currentDate)) {
                transactionDate = currentDate;
            }
        }
        ExternalId externalIdEmpty = ExternalId.empty();
        if (TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
            externalIdEmpty = ExternalId.generate();
        }
        final LoanTransaction applyLoanChargeTransaction = LoanTransaction.accrueLoanCharge(this, getOffice(), chargeAmount,
                transactionDate, feeCharges, penaltyCharges, externalIdEmpty);

        Integer installmentNumber = null;
        final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(applyLoanChargeTransaction, loanCharge,
                loanCharge.getAmount(getCurrency()).getAmount(), installmentNumber);
        applyLoanChargeTransaction.getLoanChargesPaid().add(loanChargePaidBy);
        addLoanTransaction(applyLoanChargeTransaction);
        return applyLoanChargeTransaction;
    }

    private void handleChargePaidTransaction(final LoanCharge charge, final LoanTransaction chargesPayment,
            final LoanLifecycleStateMachine loanLifecycleStateMachine, final Integer installmentNumber) {
        chargesPayment.updateLoan(this);
        final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(chargesPayment, charge,
                chargesPayment.getAmount(getCurrency()).getAmount(), installmentNumber);
        chargesPayment.getLoanChargesPaid().add(loanChargePaidBy);
        addLoanTransaction(chargesPayment);
        loanLifecycleStateMachine.transition(LoanEvent.LOAN_CHARGE_PAYMENT, this);

        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        final List<LoanRepaymentScheduleInstallment> chargePaymentInstallments = new ArrayList<>();
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        int firstNormalInstallmentNumber = LoanRepaymentScheduleProcessingWrapper
                .fetchFirstNormalInstallmentNumber(repaymentScheduleInstallments);
        for (final LoanRepaymentScheduleInstallment installment : installments) {
            boolean isFirstNormalInstallment = installment.getInstallmentNumber().equals(firstNormalInstallmentNumber)
                    ? charge.isDueForCollectionFromIncludingAndUpToAndIncluding(installment.getFromDate(), installment.getDueDate())
                    : charge.isDueForCollectionFromAndUpToAndIncluding(installment.getFromDate(), installment.getDueDate());
            if ((installmentNumber == null && isFirstNormalInstallment) || (installment.getInstallmentNumber().equals(installmentNumber))) {
                chargePaymentInstallments.add(installment);
                break;
            }
        }
        final Set<LoanCharge> loanCharges = new HashSet<>(1);
        loanCharges.add(charge);
        loanRepaymentScheduleTransactionProcessor.processLatestTransaction(chargesPayment,
                new TransactionCtx(getCurrency(), chargePaymentInstallments, loanCharges, new MoneyHolder(getTotalOverpaidAsMoney())));

        updateLoanSummaryDerivedFields();
        doPostLoanTransactionChecks(chargesPayment.getTransactionDate(), loanLifecycleStateMachine);
    }

    private void validateLoanIsNotClosed(final LoanCharge loanCharge) {
        if (isClosed()) {
            final String defaultUserMessage = "This charge cannot be added as the loan is already closed.";
            throw new LoanChargeCannotBeAddedException(Loan.LOAN_CHARGE_PARAM, "loan.is.closed", defaultUserMessage, getId(),
                    loanCharge.name());

        }
    }

    private void validateLoanChargeIsNotWaived(final LoanCharge loanCharge) {
        if (loanCharge.isWaived()) {
            final String defaultUserMessage = "This loan charge cannot be removed as the charge as already been waived.";
            throw new LoanChargeCannotBeAddedException(Loan.LOAN_CHARGE_PARAM, "loanCharge.is.waived", defaultUserMessage, getId(),
                    loanCharge.name());

        }
    }

    private void validateChargeHasValidSpecifiedDateIfApplicable(final LoanCharge loanCharge, final LocalDate disbursementDate) {
        if (loanCharge.isSpecifiedDueDate() && DateUtils.isBefore(loanCharge.getDueLocalDate(), disbursementDate)) {
            final String defaultUserMessage = "This charge with specified due date cannot be added as the it is not in schedule range.";
            throw new LoanChargeCannotBeAddedException(Loan.LOAN_CHARGE_PARAM, "specified.due.date.outside.range", defaultUserMessage,
                    getDisbursementDate(), loanCharge.name());
        }
    }

    private LocalDate getLastRepaymentPeriodDueDate(final boolean includeRecalculatedInterestComponent) {
        LocalDate lastRepaymentDate = getDisbursementDate();
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (LoanRepaymentScheduleInstallment installment : installments) {
            if ((includeRecalculatedInterestComponent || !installment.isRecalculatedInterestComponent())
                    && DateUtils.isAfter(installment.getDueDate(), lastRepaymentDate)) {
                lastRepaymentDate = installment.getDueDate();
            }
        }
        return lastRepaymentDate;
    }

    public void removeLoanCharge(final LoanCharge loanCharge) {

        validateLoanIsNotClosed(loanCharge);

        // NOTE: to remove this constraint requires that loan transactions
        // that represent the waive of charges also be removed (or reversed)M
        // if you want ability to remove loan charges that are waived.
        validateLoanChargeIsNotWaived(loanCharge);

        final boolean removed = loanCharge.isActive();
        if (removed) {
            loanCharge.setActive(false);
            final LoanRepaymentScheduleProcessingWrapper wrapper = new LoanRepaymentScheduleProcessingWrapper();
            wrapper.reprocess(getCurrency(), getDisbursementDate(), getRepaymentScheduleInstallments(), getActiveCharges());
            updateSummaryWithTotalFeeChargesDueAtDisbursement(deriveSumTotalOfChargesDueAtDisbursement());
        }

        removeOrModifyTransactionAssociatedWithLoanChargeIfDueAtDisbursement(loanCharge);

        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        if (!loanCharge.isDueAtDisbursement() && loanCharge.isPaidOrPartiallyPaid(loanCurrency())) {
            final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retrieveListOfTransactionsPostDisbursement();
            loanRepaymentScheduleTransactionProcessor.reprocessLoanTransactions(getDisbursementDate(),
                    allNonContraTransactionsPostDisbursement, getCurrency(), getRepaymentScheduleInstallments(), getActiveCharges());
        }
        this.charges.remove(loanCharge);
        updateLoanSummaryDerivedFields();
    }

    private void removeOrModifyTransactionAssociatedWithLoanChargeIfDueAtDisbursement(final LoanCharge loanCharge) {
        if (loanCharge.isDueAtDisbursement()) {
            LoanTransaction transactionToRemove = null;
            List<LoanTransaction> transactions = getLoanTransactions();
            for (final LoanTransaction transaction : transactions) {
                if (transaction.isRepaymentAtDisbursement()
                        && doesLoanChargePaidByContainLoanCharge(transaction.getLoanChargesPaid(), loanCharge)) {

                    final MonetaryCurrency currency = loanCurrency();
                    final Money chargeAmount = Money.of(currency, loanCharge.amount());
                    if (transaction.isGreaterThan(chargeAmount)) {
                        final Money principalPortion = Money.zero(currency);
                        final Money interestPortion = Money.zero(currency);
                        final Money penaltychargesPortion = Money.zero(currency);

                        transaction.updateComponentsAndTotal(principalPortion, interestPortion, chargeAmount, penaltychargesPortion);

                    } else {
                        transactionToRemove = transaction;
                    }
                }
            }

            if (transactionToRemove != null) {
                this.loanTransactions.remove(transactionToRemove);
            }
        }
    }

    private boolean doesLoanChargePaidByContainLoanCharge(Set<LoanChargePaidBy> loanChargePaidBys, LoanCharge loanCharge) {
        for (LoanChargePaidBy loanChargePaidBy : loanChargePaidBys) {
            if (loanChargePaidBy.getLoanCharge().equals(loanCharge)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Object> updateLoanCharge(final LoanCharge loanCharge, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(3);

        validateLoanIsNotClosed(loanCharge);
        if (getActiveCharges().contains(loanCharge)) {
            final BigDecimal amount = calculateAmountPercentageAppliedTo(loanCharge);
            final Map<String, Object> loanChargeChanges = loanCharge.update(command, amount);
            actualChanges.putAll(loanChargeChanges);
            updateSummaryWithTotalFeeChargesDueAtDisbursement(deriveSumTotalOfChargesDueAtDisbursement());
        }

        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        if (!loanCharge.isDueAtDisbursement()) {
            final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retrieveListOfTransactionsPostDisbursement();
            loanRepaymentScheduleTransactionProcessor.reprocessLoanTransactions(getDisbursementDate(),
                    allNonContraTransactionsPostDisbursement, getCurrency(), getRepaymentScheduleInstallments(), getActiveCharges());
        } else {
            // reprocess loan schedule based on charge been waived.
            final LoanRepaymentScheduleProcessingWrapper wrapper = new LoanRepaymentScheduleProcessingWrapper();
            wrapper.reprocess(getCurrency(), getDisbursementDate(), getRepaymentScheduleInstallments(), getActiveCharges());
        }

        updateLoanSummaryDerivedFields();

        return actualChanges;
    }

    /**
     * @param loanCharge
     * @return
     */
    private BigDecimal calculateAmountPercentageAppliedTo(final LoanCharge loanCharge) {
        BigDecimal amount = BigDecimal.ZERO;
        if (loanCharge.isOverdueInstallmentCharge()) {
            return loanCharge.getAmountPercentageAppliedTo();
        }
        switch (loanCharge.getChargeCalculation()) {
            case PERCENT_OF_AMOUNT:
                amount = getDerivedAmountForCharge(loanCharge);
            break;
            case PERCENT_OF_AMOUNT_AND_INTEREST:
                final BigDecimal totalInterestCharged = getTotalInterest();
                if (isMultiDisburmentLoan() && loanCharge.isDisbursementCharge()) {
                    amount = getTotalAllTrancheDisbursementAmount().getAmount().add(totalInterestCharged);
                } else {
                    amount = getPrincipal().getAmount().add(totalInterestCharged);
                }
            break;
            case PERCENT_OF_INTEREST:
                amount = getTotalInterest();
            break;
            case PERCENT_OF_DISBURSEMENT_AMOUNT:
                if (loanCharge.getTrancheDisbursementCharge() != null) {
                    amount = loanCharge.getTrancheDisbursementCharge().getloanDisbursementDetails().principal();
                } else {
                    amount = getPrincipal().getAmount();
                }
            break;
            case OPRIN_SEGO:
                amount = this.getPrincipal().getAmount();
            break;
            default:
            break;
        }
        return amount;
    }

    private Money getTotalAllTrancheDisbursementAmount() {
        Money amount = Money.zero(getCurrency());
        if (isMultiDisburmentLoan()) {
            for (final LoanDisbursementDetails loanDisbursementDetail : getDisbursementDetails()) {
                amount = amount.plus(loanDisbursementDetail.principal());
            }
        }
        return amount;
    }

    /**
     * @return
     */
    public BigDecimal getTotalInterest() {
        return this.loanSummaryWrapper.calculateTotalInterestCharged(getRepaymentScheduleInstallments(), getCurrency()).getAmount();
    }

    private BigDecimal calculatePerInstallmentChargeAmount(final LoanCharge loanCharge) {
        return calculatePerInstallmentChargeAmount(loanCharge.getChargeCalculation(), loanCharge.getPercentage(),
                loanCharge.getCharge().getParentChargeId(), loanCharge);
    }

    public BigDecimal calculatePerInstallmentChargeAmount(final ChargeCalculationType calculationType, final BigDecimal percentage,
            Long parentChargeId, LoanCharge loanCharge) {
        Money amount = Money.zero(getCurrency());
        if (calculationType.isCustomPercentageBasedDistributedCharge()) { // Disbursement based mandatory insurance or
                                                                          // aval
            Money percentOf = this.getPrincipal();
            Integer numberOfRepayments = this.getLoanProductRelatedDetail().getNumberOfRepayments();
            List<LoanRepaymentScheduleInstallment> graceInstallments = this.getRepaymentScheduleInstallments().stream()
                    .filter(inst -> inst.getInstallmentNumber() != null && inst.getInstallmentNumber() == 0).toList();
            if (!graceInstallments.isEmpty()) {
                numberOfRepayments = numberOfRepayments - graceInstallments.size();
            }
            BigDecimal numberOfInstallments = BigDecimal.valueOf(numberOfRepayments);
            if (numberOfInstallments.compareTo(BigDecimal.ZERO) == 0) {
                // Skip further processing if the value is 0
                return BigDecimal.ZERO;
            }
            if (ChargeCalculationType.DISB_SEGO == loanCharge.getChargeCalculation()) {
                numberOfInstallments = BigDecimal.ONE;
            }

            BigDecimal computedAmount = LoanCharge.percentageOf(percentOf.getAmount(), percentage);
            BigDecimal finalAmount = computedAmount.divide(numberOfInstallments, 2, RoundingMode.HALF_UP);
            amount = amount.plus(finalAmount);
            return amount.getAmount();
        }
        this.outstandingBalance = this.loanRepaymentScheduleDetail.getPrincipal();
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallmentsIgnoringTotalGrace();
        for (final LoanRepaymentScheduleInstallment installment : installments) {
            amount = amount.plus(calculateInstallmentChargeAmount(calculationType, percentage, installment, parentChargeId, loanCharge));
        }
        return amount.getAmount();
    }

    public BigDecimal getTotalWrittenOff() {
        return this.summary.getTotalWrittenOff();
    }

    /**
     * @param calculationType
     * @param percentage
     * @param installment
     * @return
     */
    @Transient
    private transient Money outstandingBalance = null;

    public Money findACHGPercentOf(final Long parentChargeId, final LoanRepaymentScheduleInstallment installment) {
        Money percentOf = Money.zero(getCurrency());
        for (LoanCharge charge : this.getCharges()) {
            if (charge.getCharge().getId() != null && Objects.equals(charge.getCharge().getId(), parentChargeId)) {
                final LoanInstallmentCharge installmentCharge = charge.getInstallmentLoanCharge(installment.getInstallmentNumber());
                if (installmentCharge != null) {
                    percentOf = installmentCharge.getAmount(getCurrency());
                }
                break;
            }
        }
        return percentOf;
    }

    private Money calculateCustomPercentageBasedDistributedCharge(final BigDecimal percentage) {
        Money amount = Money.zero(getCurrency());
        Money percentOf = this.getPrincipal();
        Integer numberOfRepayments = this.getLoanProductRelatedDetail().getNumberOfRepayments();
        List<LoanRepaymentScheduleInstallment> graceInstallments = this.getRepaymentScheduleInstallments().stream()
                .filter(inst -> inst.getInstallmentNumber() != null && inst.getInstallmentNumber() == 0).toList();
        if (!graceInstallments.isEmpty()) {
            numberOfRepayments = numberOfRepayments - graceInstallments.size();
        }
        BigDecimal numberOfInstallments = BigDecimal.valueOf(numberOfRepayments);
        BigDecimal computedAmount = LoanCharge.percentageOf(percentOf.getAmount(), percentage);
        BigDecimal finalAmount = computedAmount;
        if (numberOfInstallments.compareTo(BigDecimal.ZERO) > 0) {
            finalAmount = computedAmount.divide(numberOfInstallments, 2, RoundingMode.HALF_UP);
        }
        return amount.plus(finalAmount);
    }

    @SuppressWarnings({ "java:S3776" })
    private Money calculateInstallmentChargeAmount(final ChargeCalculationType calculationType, final BigDecimal percentage,
            final LoanRepaymentScheduleInstallment installment, Long parentChargeId, LoanCharge loanCharge) {
        Money amount = Money.zero(getCurrency());
        Money percentOf = Money.zero(getCurrency());

        if (loanCharge.getCharge().getName().contains(LIFE_INSURANCE.getRootName())
                && this.containsBlockAccountFreezeLifeInsurance(installment.getDueDate())) {
            installment.setLifeInsuranceChargePortion(amount.getAmount());
            return amount;
        } else if ((loanCharge.getCharge().getName().contains(ChargeCustomType.CAPITAL_PENDIENTE_MI_PYME.getRootName())
                || loanCharge.getCharge().getName().contains(ChargeCustomType.COMISION_MI_PYME.getRootName()))
                && Objects.nonNull(loanCharge.getLoan())
                && loanCharge.getLoan().containsBlockAccountFreezeMipyme(installment.getDueDate())) {
            return amount;
        }

        switch (calculationType) {
            case PERCENT_OF_AMOUNT:
                percentOf = installment.getPrincipal(getCurrency());
            break;
            case PERCENT_OF_AMOUNT_AND_INTEREST:
                percentOf = installment.getPrincipal(getCurrency()).plus(installment.getInterestCharged(getCurrency()));
            break;
            case PERCENT_OF_INTEREST:
                percentOf = installment.getInterestCharged(getCurrency());
            break;
            case PERCENT_OF_ANOTHER_CHARGE, ACHG:
                percentOf = findACHGPercentOf(parentChargeId, installment);
            break;
            case PERCENT_OF_OUTSTANDING_PRINCIPAL_AMOUNT, OPRIN_SEGO:
                // Get percentage from the parent Charge and calculate on outstanding balance
                this.outstandingBalance = this.outstandingBalance.minus(installment.getAdvancePrincipalAmount());
                percentOf = outstandingBalance;
            break;
            default:
            break;
        }
        if (calculationType.isCustomPercentageBasedDistributedCharge()) { // Mandatory Insurance and aval based on
                                                                          // Disbursement Amount
            amount = calculateCustomPercentageBasedDistributedCharge(percentage);
        } else if (calculationType.isPercentageOfAnotherCharge() && calculationType.equals(ChargeCalculationType.ACHG)) { // Term/VAT
                                                                                                                          // on
                                                                                                                          // insurance
            if (loanCharge != null && loanCharge.isVatChargeOfHonoCharge()) {
                amount = amount.plus(Money.of(getCurrency(), loanCharge.getVatAmountOfHonoCharge(installment.getInstallmentNumber())));
            } else {
                amount = amount.plus(percentOf.getAmount().multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
        } else if (calculationType.isCustomPercentageOfOutstandingPrincipalCharge()) {
            Integer numberOfRepayments = this.getLoanProductRelatedDetail().getNumberOfRepayments();
            List<LoanRepaymentScheduleInstallment> graceInstallments = this.getRepaymentScheduleInstallments().stream()
                    .filter(inst -> inst.getInstallmentNumber() != null && inst.getInstallmentNumber() == 0).toList();
            if (!graceInstallments.isEmpty()) {
                numberOfRepayments = numberOfRepayments - graceInstallments.size();
            }
            BigDecimal numberOfInstallments = BigDecimal.valueOf(numberOfRepayments);
            BigDecimal computedAmount = LoanCharge.percentageOf(percentOf.getAmount(), percentage);
            this.outstandingBalance = this.outstandingBalance.minus(installment.getPrincipal(this.getCurrency()));

            // If charge is Capital Pendiente, do not divide by nr of installments
            if (loanCharge != null && loanCharge.getCharge().getName().contains(CAPITAL_PENDIENTE_MI_PYME.getRootName())) {
                numberOfInstallments = BigDecimal.ONE;
            }

            BigDecimal finalAmount = computedAmount.divide(numberOfInstallments, 2, RoundingMode.HALF_UP);
            amount = amount.plus(finalAmount);

        } else if (this.isMultiDisburmentLoan() && calculationType.isPercentageOfLifeInsurance()
                && calculationType.equals(ChargeCalculationType.LIIN_SEGO)
                && (PeriodFrequencyType.fromInt(this.termPeriodFrequencyType).equals(PeriodFrequencyType.WEEKS)
                        || PeriodFrequencyType.fromInt(this.termPeriodFrequencyType).equals(PeriodFrequencyType.DAYS))
                && installment.getInstallmentNumber() > 1) {

            // Get first repayment installment due date
            Integer firstRepaymentDueDate = this.repaymentScheduleInstallments.stream().filter(first -> first.getInstallmentNumber() == 1)
                    .findFirst().get().getDueDate().getDayOfMonth();

            // Check if firstRepaymentDueDate is bigger than installment due date day of month
            if (firstRepaymentDueDate > installment.getFromDate().lengthOfMonth()) {
                firstRepaymentDueDate = installment.getFromDate().lengthOfMonth();
            }

            LocalDate startDate = installment.getFromDate();
            startDate = startDate.plusDays(1);

            // Create a new date obj based on installment from date, but using the day "firstRepaymentDueDate"
            LocalDate chargeDate = installment.getFromDate().withDayOfMonth(firstRepaymentDueDate);

            // Adjust chargeDate to be in the same month as startDate when in different months
            if (!chargeDate.getMonth().equals(startDate.getMonth())) {
                chargeDate = startDate.withDayOfMonth(firstRepaymentDueDate);
            }

            // Compare if chargeDate is between installment startDate and due date
            boolean isInBetween = DateUtils.isDateWithinRange(chargeDate, startDate, installment.getDueDate());

            // If so, calculate life insurance charge portion
            if (isInBetween) {
                Money amountAux = amount.plus(loanCharge.getAmountPercentageAppliedTo().multiply(percentage).divide(BigDecimal.valueOf(100),
                        2, RoundingMode.HALF_UP));
                amount = amount.plus(amountAux);

                installment.setLifeInsuranceChargePortion(amountAux.getAmount());
            }

        } else if (calculationType.isPercentageOfLifeInsurance()) {
            // Find first installment with non-null life insurance charge portion
            Money amountAux = Money.zero(getCurrency());
            LoanRepaymentScheduleInstallment sourceInstallment = getRepaymentScheduleInstallments().stream()
                    .filter(inst -> inst.getLifeInsuranceChargePortion() != null).findFirst().orElse(null);

            if (isLifeInsuranceChargeApplicable(sourceInstallment, installment)) {
                amountAux = amount.plus(loanCharge.getAmountPercentageAppliedTo().multiply(percentage).divide(BigDecimal.valueOf(100), 2,
                        RoundingMode.HALF_UP));
                amount = amount.plus(amountAux);
            }

            installment.setLifeInsuranceChargePortion(amountAux.getAmount());
        }
        return amount;
    }

    private boolean isLifeInsuranceChargeApplicable(LoanRepaymentScheduleInstallment sourceInstallment,
            LoanRepaymentScheduleInstallment installment) {
        if (sourceInstallment == null) {
            return false;
        }
        int installmentDayOfMonth = sourceInstallment.getDueDate().getDayOfMonth();
        LocalDate periodStartDate = installment.getFromDate();
        LocalDate periodDueDate = installment.getDueDate();
        PeriodFrequencyType periodFrequencyType = PeriodFrequencyType.fromInt(installment.getLoan().getTermPeriodFrequencyType());

        Integer maxDayOfMonth = YearMonth.from(periodDueDate).lengthOfMonth();

        periodStartDate = periodStartDate.plusDays(1);

        Integer installmentDayOfMonthClone = installmentDayOfMonth;

        if (installmentDayOfMonth > maxDayOfMonth) {
            installmentDayOfMonthClone = maxDayOfMonth;
        }

        LocalDate referenceDate = LocalDate.of(periodDueDate.getYear(), periodDueDate.getMonthValue(), installmentDayOfMonthClone);

        // If weekly repayment
        if (periodFrequencyType.isWeekly() || periodFrequencyType.isDaily()) {
            // scenario: when month changes within period...
            if (periodStartDate.getMonth().compareTo(periodDueDate.getMonth()) != 0) {
                // scenario: when installment due on day 31 (or 30) and month have 28 or 29 days (feb) or 30 days
                if (installmentDayOfMonthClone.compareTo(YearMonth.from(periodStartDate).lengthOfMonth()) > 0) {
                    installmentDayOfMonthClone = YearMonth.from(periodStartDate).lengthOfMonth();
                }

                LocalDate referenceDateClone = LocalDate.of(periodStartDate.getYear(), periodStartDate.getMonthValue(),
                        installmentDayOfMonthClone);
                boolean isWithinRange = isWithinRange(periodStartDate, periodDueDate, referenceDateClone);

                if (isWithinRange) {
                    referenceDate = referenceDateClone;
                }
            }

        } else { // Daily and Monthly
            if (installment.getInstallmentNumber().compareTo(1) > 0 && installmentDayOfMonth >= 28) {
                referenceDate = LocalDate.of(periodStartDate.getYear(), periodStartDate.getMonthValue(),
                        YearMonth.from(periodStartDate).lengthOfMonth());
            }
        }

        // If charge date is beween periodStartDate and periodDueDate
        boolean isWithinRange = isWithinRange(periodStartDate, periodDueDate, referenceDate);

        return isWithinRange;
    }

    private boolean isWithinRange(LocalDate periodStartDate, LocalDate periodDueDate, LocalDate referenceDate) {
        return (referenceDate.isEqual(periodStartDate) || referenceDate.isAfter(periodStartDate))
                && (referenceDate.isEqual(periodDueDate) || referenceDate.isBefore(periodDueDate));
    }

    @SuppressWarnings({ "squid:S3776", "squid:S107" })
    public LoanTransaction waiveLoanCharge(final LoanCharge loanCharge, final LoanLifecycleStateMachine loanLifecycleStateMachine,
            final Map<String, Object> changes, final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds,
            final Integer loanInstallmentNumber, final ScheduleGeneratorDTO scheduleGeneratorDTO, final Money accruedCharge,
            final ExternalId externalId) {

        validateLoanIsNotClosed(loanCharge);

        final Money amountWaived = loanCharge.waive(loanCurrency(), loanInstallmentNumber);

        changes.put("amount", amountWaived.getAmount());

        Money unrecognizedIncome = amountWaived.zero();
        Money chargeComponent = amountWaived;
        if (Boolean.TRUE.equals(isPeriodicAccrualAccountingEnabledOnLoanProduct())) {
            Money receivableCharge;
            if (loanInstallmentNumber != null) {
                receivableCharge = accruedCharge
                        .minus(loanCharge.getInstallmentLoanCharge(loanInstallmentNumber).getAmountPaid(getCurrency()));
            } else {
                receivableCharge = accruedCharge.minus(loanCharge.getAmountPaid(getCurrency()));
            }
            if (receivableCharge.isLessThanZero()) {
                receivableCharge = amountWaived.zero();
            }
            if (amountWaived.isGreaterThan(receivableCharge)) {
                chargeComponent = receivableCharge;
                unrecognizedIncome = amountWaived.minus(receivableCharge);
            }
        }
        Money feeChargesWaived = chargeComponent;
        Money penaltyChargesWaived = Money.zero(loanCurrency());
        if (loanCharge.isPenaltyCharge()) {
            penaltyChargesWaived = chargeComponent;
            feeChargesWaived = Money.zero(loanCurrency());
        }

        LocalDate transactionDate = getDisbursementDate();
        LocalDate businessDate = DateUtils.getBusinessLocalDate();
        if (loanCharge.isDueDateCharge()) {
            if (DateUtils.isAfter(loanCharge.getDueLocalDate(), businessDate)) {
                transactionDate = businessDate;
            } else {
                transactionDate = loanCharge.getDueLocalDate();
            }
        } else if (loanCharge.isInstalmentFee()) {
            LocalDate repaymentDueDate = loanCharge.getInstallmentLoanCharge(loanInstallmentNumber).getRepaymentInstallment().getDueDate();
            if (DateUtils.isAfter(repaymentDueDate, businessDate)) {
                transactionDate = businessDate;
            } else {
                transactionDate = repaymentDueDate;
            }
        }

        scheduleGeneratorDTO.setRecalculateFrom(transactionDate);

        updateSummaryWithTotalFeeChargesDueAtDisbursement(deriveSumTotalOfChargesDueAtDisbursement());

        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());

        final LoanTransaction waiveLoanChargeTransaction = LoanTransaction.waiveLoanCharge(this, getOffice(), amountWaived, transactionDate,
                feeChargesWaived, penaltyChargesWaived, unrecognizedIncome, externalId);
        final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(waiveLoanChargeTransaction, loanCharge,
                waiveLoanChargeTransaction.getAmount(getCurrency()).getAmount(), loanInstallmentNumber);
        waiveLoanChargeTransaction.getLoanChargesPaid().add(loanChargePaidBy);
        addLoanTransaction(waiveLoanChargeTransaction);
        if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()
                && DateUtils.isBefore(loanCharge.getDueLocalDate(), businessDate)) {
            regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
        }
        // Waive of charges whose due date falls after latest 'repayment' transaction don't require entire loan schedule
        // to be reprocessed.
        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        if (!loanCharge.isDueAtDisbursement() && loanCharge.isPaidOrPartiallyPaid(loanCurrency())) {
            final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retrieveListOfTransactionsPostDisbursement();
            loanRepaymentScheduleTransactionProcessor.reprocessLoanTransactions(getDisbursementDate(),
                    allNonContraTransactionsPostDisbursement, getCurrency(), getRepaymentScheduleInstallments(), getActiveCharges());
        } else {
            // reprocess loan schedule based on charge been waived.
            final LoanRepaymentScheduleProcessingWrapper wrapper = new LoanRepaymentScheduleProcessingWrapper();
            wrapper.reprocess(getCurrency(), getDisbursementDate(), getRepaymentScheduleInstallments(), getActiveCharges());
        }

        updateLoanSummaryDerivedFields();

        doPostLoanTransactionChecks(waiveLoanChargeTransaction.getTransactionDate(), loanLifecycleStateMachine);

        return waiveLoanChargeTransaction;
    }

    public Client client() {
        return this.client;
    }

    public GroupLoanIndividualMonitoringAccount getGlim() {
        return glim;
    }

    public void setGlim(GroupLoanIndividualMonitoringAccount glim) {
        this.glim = glim;
    }

    public LoanProduct loanProduct() {
        return this.loanProduct;
    }

    public LoanProductRelatedDetail repaymentScheduleDetail() {
        return this.loanRepaymentScheduleDetail;
    }

    public void updateClient(final Client client) {
        this.client = client;
    }

    public void updateLoanProduct(final LoanProduct loanProduct) {
        this.loanProduct = loanProduct;
    }

    public void updateAccountNo(final String newAccountNo) {
        this.accountNumber = newAccountNo;
        this.accountNumberRequiresAutoGeneration = false;
    }

    public void updateFund(final Fund fund) {
        this.fund = fund;
    }

    public void updateLoanPurpose(final CodeValue loanPurpose) {
        this.loanPurpose = loanPurpose;
    }

    public void updateLoanOfficerOnLoanApplication(final Staff newLoanOfficer) {
        if (!isSubmittedAndPendingApproval()) {
            Long loanOfficerId = null;
            if (this.loanOfficer != null) {
                loanOfficerId = this.loanOfficer.getId();
            }
            throw new LoanOfficerAssignmentException(getId(), loanOfficerId);
        }
        this.loanOfficer = newLoanOfficer;
    }

    public void updateTransactionProcessingStrategy(final String transactionProcessingStrategyCode,
            final String transactionProcessingStrategyName) {
        this.transactionProcessingStrategyCode = transactionProcessingStrategyCode;
        this.transactionProcessingStrategyName = transactionProcessingStrategyName;
    }

    @SuppressWarnings({ "squid:S3776" })
    public void updateLoanCharges(final Set<LoanCharge> loanCharges) {
        List<Long> existingCharges = fetchAllLoanChargeIds();

        /** Process new and updated charges **/
        for (final LoanCharge loanCharge : loanCharges) {
            LoanCharge charge = loanCharge;
            // add new charges
            if (loanCharge.getId() == null) {
                LoanTrancheDisbursementCharge loanTrancheDisbursementCharge = null;
                loanCharge.update(this);
                if (this.loanProduct.isMultiDisburseLoan() && loanCharge.isTrancheDisbursementCharge()) {
                    loanCharge.getTrancheDisbursementCharge().getloanDisbursementDetails().updateLoan(this);
                    for (final LoanDisbursementDetails loanDisbursementDetails : getDisbursementDetails()) {
                        if (loanCharge.getTrancheDisbursementCharge().getloanDisbursementDetails().getId() == null
                                && loanCharge.getTrancheDisbursementCharge().getloanDisbursementDetails().equals(loanDisbursementDetails)) {
                            loanTrancheDisbursementCharge = new LoanTrancheDisbursementCharge(loanCharge, loanDisbursementDetails);
                            loanCharge.updateLoanTrancheDisbursementCharge(loanTrancheDisbursementCharge);
                        }
                    }
                }
                this.charges.add(loanCharge);

            } else {
                charge = fetchLoanChargesById(charge.getId());
                if (charge != null) {
                    existingCharges.remove(charge.getId());
                }
            }
            final BigDecimal amount = calculateAmountPercentageAppliedTo(loanCharge);
            BigDecimal chargeAmt;
            BigDecimal totalChargeAmt = BigDecimal.ZERO;
            if (loanCharge.getChargeCalculation().isPercentageBased()) {
                chargeAmt = loanCharge.getPercentage();
                if (loanCharge.isInstalmentFee()) {
                    totalChargeAmt = calculatePerInstallmentChargeAmount(loanCharge);
                } else if (loanCharge.isOverdueInstallmentCharge()) {
                    totalChargeAmt = loanCharge.amountOutstanding();
                }
            } else {
                if (loanCharge.isCustomFlatDistributedCharge()) {
                    chargeAmt = loanCharge.getAmount(this.getCurrency()).getAmount();
                } else {
                    chargeAmt = loanCharge.amountOrPercentage();
                }
            }
            if (charge != null) {
                charge.update(chargeAmt, loanCharge.getDueLocalDate(), amount, fetchNumberOfInstallmensAfterExceptions(), totalChargeAmt);
            }

        }

        /** Updated deleted charges **/
        for (Long id : existingCharges) {
            fetchLoanChargesById(id).setActive(false);
        }
        updateSummaryWithTotalFeeChargesDueAtDisbursement(deriveSumTotalOfChargesDueAtDisbursement());
    }

    public void updateLoanCollateral(final Set<LoanCollateralManagement> loanCollateral) {
        if (this.loanCollateralManagements == null) {
            this.loanCollateralManagements = new HashSet<>();
        }
        this.loanCollateralManagements.clear();
        this.loanCollateralManagements.addAll(associateWithThisLoan(loanCollateral));
    }

    public void updateLoanRates(final List<Rate> loanRates) {
        if (this.rates == null) {
            this.rates = new ArrayList<>();
        }
        this.rates.clear();
        this.rates.addAll(loanRates);
    }

    public void updateLoanSchedule(final LoanScheduleModel modifiedLoanSchedule) {
        this.repaymentScheduleInstallments.clear();
        int actualPaymentNumber = 1;
        for (final LoanScheduleModelPeriod scheduledLoanInstallment : modifiedLoanSchedule.getPeriods()) {
            int periodNumber;
            if (scheduledLoanInstallment.isRepaymentPeriod() || scheduledLoanInstallment.isDownPaymentPeriod()) {
                if (scheduledLoanInstallment.isTotalGracePeriod()) {
                    periodNumber = 0;
                } else {
                    periodNumber = actualPaymentNumber;
                    actualPaymentNumber++;
                }

                final LoanRepaymentScheduleInstallment installment = new LoanRepaymentScheduleInstallment(this, periodNumber,
                        scheduledLoanInstallment.periodFromDate(), scheduledLoanInstallment.periodDueDate(),
                        scheduledLoanInstallment.principalDue(), scheduledLoanInstallment.interestDue(),
                        scheduledLoanInstallment.feeChargesDue(), scheduledLoanInstallment.penaltyChargesDue(),
                        scheduledLoanInstallment.isRecalculatedInterestComponent(), scheduledLoanInstallment.getLoanCompoundingDetails(),
                        scheduledLoanInstallment.rescheduleInterestPortion(), scheduledLoanInstallment.isDownPaymentPeriod());
                installment.setLifeInsuranceChargePortion(scheduledLoanInstallment.getTotalLifeInsuranceCharged());
                addLoanRepaymentScheduleInstallment(installment);
            }
        }
    }

    public void updateLoanDerivedFields() {
        updateLoanScheduleDependentDerivedFields();
        updateLoanSummaryDerivedFields();
        applyAccruals();
    }

    // SU-527 SU-530
    // This method is created to avoid creation of duplicate installments on transaction reversal.
    // Instead of fineract built-in way of using the newely calculated installments, this function copies the component
    // amounts
    // of newely calculated installments to existing installments so that the charges remain intact. If some
    // installments are removed because
    // of advance payment then those are removed from the original installment list and vice versa added to the new
    // installment list in case of reschedule
    public void updateLoanSchedule(final LoanScheduleDTO loanSchedule) {
        List<LoanRepaymentScheduleInstallment> scheduleInstallments = loanSchedule.getInstallments();
        List<LoanRepaymentScheduleInstallment> removeInstallments = new ArrayList<>();
        int installmentIdBeforeCutOff = DisbursementCutoffContext.getInstallmentsBeforeCutoff();
        for (LoanRepaymentScheduleInstallment installment : this.repaymentScheduleInstallments) {
            // Do not touch graced or fully paid installments
            int currentInstallmentNumber = installment.getInstallmentNumber();
            if (currentInstallmentNumber == 0 || installment.isObligationsMet() || currentInstallmentNumber <= installmentIdBeforeCutOff) {
                continue;
            }
            LoanRepaymentScheduleInstallment scheduledInstallment = findByInstallmentNumber(scheduleInstallments,
                    installment.getInstallmentNumber());
            if (scheduledInstallment != null) {
                installment.updateComponents(scheduledInstallment, this.getCurrency());
            } else {
                // This can only be possible in case of big advance payment which reduces the installment count
                installment.getInstallmentCharges().clear();
                // Remove penalty charges for deleted installments
                Set<LoanCharge> removeOverdueInstallmentCharges = new HashSet<>();
                for (LoanCharge charge : this.charges) {
                    if (charge.isPenaltyCharge()
                            && Objects.equals(charge.getOverdueInstallmentCharge().getInstallment().getInstallmentNumber(),
                                    installment.getInstallmentNumber())) {
                        removeOverdueInstallmentCharges.add(charge);
                    }
                }
                removeOverdueInstallmentCharges.forEach(this.charges::remove);
                removeInstallments.add(installment);
            }
        }
        // Check if there are extra installments created because of loan rescheduling then add those newely created
        // installments
        if (this.repaymentScheduleInstallments.size() < scheduleInstallments.size()) {
            int newInstallmentsCount = scheduleInstallments.size() - this.repaymentScheduleInstallments.size();
            for (int i = newInstallmentsCount; i > 0; i--) {
                int index = scheduleInstallments.size() - i;
                addLoanRepaymentScheduleInstallment(scheduleInstallments.get(index));
            }
        }
        if (!removeInstallments.isEmpty()) {
            this.repaymentScheduleInstallments.removeAll(removeInstallments);
        }
    }

    private LoanRepaymentScheduleInstallment findByInstallmentNumber(Collection<LoanRepaymentScheduleInstallment> installments,
            Integer installmentNumber) {
        for (LoanRepaymentScheduleInstallment installment : installments) {
            if (Objects.equals(installment.getInstallmentNumber(), installmentNumber)) {
                return installment;
            }
        }
        return null;
    }

    /**
     * method updates accrual derived fields on installments and reverse the unprocessed transactions
     */
    private void applyAccruals() {
        Collection<LoanTransaction> accruals = retrieveListOfAccrualTransactions();
        if (!accruals.isEmpty()) {
            if (Boolean.TRUE.equals(isPeriodicAccrualAccountingEnabledOnLoanProduct())) {
                applyPeriodicAccruals(accruals);
            } else if (Boolean.TRUE.equals(isNoneOrCashOrUpfrontAccrualAccountingEnabledOnLoanProduct())) {
                updateAccrualsForNonPeriodicAccruals(accruals);
            }
        }
    }

    @SuppressWarnings({ "squid:S3776" })
    private void applyPeriodicAccruals(final Collection<LoanTransaction> accruals) {
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        boolean isBasedOnSubmittedOnDate = TemporaryConfigurationServiceContainer.getAccrualDateConfigForCharge()
                .equalsIgnoreCase("submitted-date");
        for (LoanRepaymentScheduleInstallment installment : installments) {
            Money interest = Money.zero(getCurrency());
            Money fee = Money.zero(getCurrency());
            Money penality = Money.zero(getCurrency());
            for (LoanTransaction loanTransaction : accruals) {
                LocalDate transactionDateForRange;
                if (isBasedOnSubmittedOnDate) {
                    final Optional<LoanChargePaidBy> loanChargePaidByOptional = loanTransaction.getLoanChargesPaid().stream().findFirst();
                    if (loanChargePaidByOptional.isPresent()) {
                        final LoanChargePaidBy loanChargePaidBy = loanChargePaidByOptional.get();
                        transactionDateForRange = loanChargePaidBy.getLoanCharge().getDueDate();
                    } else {
                        transactionDateForRange = loanTransaction.getTransactionDate();
                    }
                } else {
                    transactionDateForRange = loanTransaction.getTransactionDate();
                }
                boolean isInPeriod = LoanRepaymentScheduleProcessingWrapper.isInPeriod(transactionDateForRange, installment, installments);
                if (isInPeriod) {
                    interest = interest.plus(loanTransaction.getInterestPortion(getCurrency()));
                    fee = fee.plus(loanTransaction.getFeeChargesPortion(getCurrency()));
                    penality = penality.plus(loanTransaction.getPenaltyChargesPortion(getCurrency()));
                    if (installment.getFeeChargesCharged(getCurrency()).isLessThan(fee)
                            || installment.getInterestCharged(getCurrency()).isLessThan(interest)
                            || installment.getPenaltyChargesCharged(getCurrency()).isLessThan(penality)
                            || (isInterestBearing() && DateUtils.isEqual(getAccruedTill(), loanTransaction.getTransactionDate())
                                    && !DateUtils.isEqual(getAccruedTill(), installment.getDueDate()))) {
                        interest = interest.minus(loanTransaction.getInterestPortion(getCurrency()));
                        fee = fee.minus(loanTransaction.getFeeChargesPortion(getCurrency()));
                        penality = penality.minus(loanTransaction.getPenaltyChargesPortion(getCurrency()));
                        loanTransaction.reverse();
                    }

                }
            }
            installment.updateAccrualPortion(interest, fee, penality);
        }
        LoanRepaymentScheduleInstallment lastInstallment = getLastLoanRepaymentScheduleInstallment();
        for (LoanTransaction loanTransaction : accruals) {
            if (!loanTransaction.isReversed() && DateUtils.isAfter(loanTransaction.getTransactionDate(), lastInstallment.getDueDate())) {
                loanTransaction.reverse();
            }
        }
    }

    @SuppressWarnings({ "squid:S3776" })
    private void updateAccrualsForNonPeriodicAccruals(final Collection<LoanTransaction> accruals) {
        final Money interestApplied = Money.of(getCurrency(), this.summary.getTotalInterestCharged());
        ExternalId externalIdEmpty = ExternalId.empty();
        boolean isExternalIdAutoGenerationEnabled = TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled();
        for (LoanTransaction loanTransaction : accruals) {
            if (loanTransaction.getInterestPortion(getCurrency()).isGreaterThanZero()) {
                if (loanTransaction.getInterestPortion(getCurrency()).isNotEqualTo(interestApplied) && !loanTransaction.isDailyAccrual()) {
                    loanTransaction.reverse();
                    if (isExternalIdAutoGenerationEnabled) {
                        externalIdEmpty = ExternalId.generate();
                    }
                    final LoanTransaction interestAppliedTransaction = LoanTransaction.accrueInterest(getOffice(), this, interestApplied,
                            getDisbursementDate(), externalIdEmpty);
                    addLoanTransaction(interestAppliedTransaction);
                }
            } else {
                Set<LoanChargePaidBy> chargePaidBies = loanTransaction.getLoanChargesPaid();
                for (final LoanChargePaidBy chargePaidBy : chargePaidBies) {
                    LoanCharge loanCharge = chargePaidBy.getLoanCharge();
                    Money chargeAmount = loanCharge.getAmount(getCurrency());
                    if (chargeAmount.isNotEqualTo(loanTransaction.getAmount(getCurrency())) && !loanTransaction.isInstallmentAccrual()) {
                        loanTransaction.reverse();
                        handleChargeAppliedTransaction(loanCharge, loanTransaction.getTransactionDate());
                    }

                }
            }
        }

    }

    public void updateLoanScheduleDependentDerivedFields() {
        if (this.getLoanRepaymentScheduleInstallmentsSize() > 0) {
            this.expectedMaturityDate = determineExpectedMaturityDate();
            this.actualMaturityDate = determineExpectedMaturityDate();
        }
    }

    public void updateLoanSummaryDerivedFields() {

        if (isNotDisbursed()) {
            this.summary.zeroFields();
            this.totalOverpaid = null;
        } else {
            final Money overpaidBy = calculateTotalOverpayment();
            this.totalOverpaid = null;
            if (!overpaidBy.isLessThanZero()) {
                this.totalOverpaid = overpaidBy.getAmountDefaultedToNullIfZero();
            }

            final Money recoveredAmount = calculateTotalRecoveredPayments();
            this.totalRecovered = recoveredAmount.getAmountDefaultedToNullIfZero();

            Money principal = this.loanRepaymentScheduleDetail.getPrincipal();

            if (this.getLoanProduct().isMultiDisburseLoan()) {

                BigDecimal totalDisbursedAmount = disbursementDetails.stream()
                        .filter(notReversed -> Boolean.FALSE.equals(notReversed.isReversed()))
                        .filter(act -> Objects.nonNull(act.getActualDisbursementDate())).map(obj -> obj.getPrincipal())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                principal = Money.of(getCurrency(), totalDisbursedAmount);

                this.loanRepaymentScheduleDetail.setPrincipal(totalDisbursedAmount);
            }

            this.summary.updateSummary(loanCurrency(), principal, getRepaymentScheduleInstallments(), this.loanSummaryWrapper,
                    this.charges);
            updateLoanOutstandingBalances();
        }

    }

    public void updateLoanSummaryAndStatus() {
        updateLoanSummaryDerivedFields();
        doPostLoanTransactionChecks(getLastUserTransactionDate(), loanLifecycleStateMachine);
    }

    @SuppressWarnings({ "squid:S3776" })
    public Map<String, Object> loanApplicationModification(final JsonCommand command, final Set<LoanCharge> possiblyModifedLoanCharges,
            final Set<LoanCollateralManagement> possiblyModifiedLoanCollateralItems, final AprCalculator aprCalculator,
            boolean isChargesModified, final LoanProduct loanProduct) {

        final Map<String, Object> actualChanges = this.loanRepaymentScheduleDetail.updateLoanApplicationAttributes(command, aprCalculator);
        final MonetaryCurrency currency = new MonetaryCurrency(loanProduct.getCurrency().getCode(),
                loanProduct.getCurrency().getDigitsAfterDecimal(), loanProduct.getCurrency().getCurrencyInMultiplesOf());
        this.loanRepaymentScheduleDetail.updateCurrency(currency);

        if (!actualChanges.isEmpty()) {
            final boolean recalculateLoanSchedule = !(actualChanges.size() == 1 && actualChanges.containsKey(IN_ARREARS_TOLERANCE));
            actualChanges.put(RECALCULATE_LOAN_SCHEDULE, recalculateLoanSchedule);
            isChargesModified = true;
        }

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();

        final String accountNoParamName = ACCOUNT_NO;
        if (command.isChangeInStringParameterNamed(accountNoParamName, this.accountNumber)) {
            final String newValue = command.stringValueOfParameterNamed(accountNoParamName);
            actualChanges.put(accountNoParamName, newValue);
            this.accountNumber = StringUtils.defaultIfEmpty(newValue, null);
        }

        if (command.isChangeInBooleanParameterNamed(CREATE_STANDING_INSTRUCTION_AT_DISBURSEMENT,
                shouldCreateStandingInstructionAtDisbursement())) {
            final Boolean valueAsInput = command.booleanObjectValueOfParameterNamed(CREATE_STANDING_INSTRUCTION_AT_DISBURSEMENT);
            actualChanges.put(CREATE_STANDING_INSTRUCTION_AT_DISBURSEMENT, valueAsInput);
            this.createStandingInstructionAtDisbursement = valueAsInput;
        }

        if (command.isChangeInStringParameterNamed(EXTERNAL_ID, this.externalId.getValue())) {
            final String newValue = command.stringValueOfParameterNamed(EXTERNAL_ID);
            ExternalId externalIdV2 = ExternalIdFactory.produce(newValue);
            if (externalIdV2.isEmpty() && TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
                externalIdV2 = ExternalId.generate();
            }
            actualChanges.put(EXTERNAL_ID, externalIdV2);
            this.externalId = externalIdV2;
        }

        // add clientId, groupId and loanType changes to actual changes

        final Long clientId = this.client == null ? null : this.client.getId();
        if (command.isChangeInLongParameterNamed(CLIENT_ID, clientId)) {
            final Long newValue = command.longValueOfParameterNamed(CLIENT_ID);
            actualChanges.put(CLIENT_ID, newValue);
        }
        final Long groupId = this.group == null ? null : this.group.getId();
        if (command.isChangeInLongParameterNamed(GROUP_ID, groupId)) {
            final Long newValue = command.longValueOfParameterNamed(GROUP_ID);
            actualChanges.put(GROUP_ID, newValue);
        }

        if (command.isChangeInLongParameterNamed(PRODUCT_ID, this.loanProduct.getId())) {
            final Long newValue = command.longValueOfParameterNamed(PRODUCT_ID);
            actualChanges.put(PRODUCT_ID, newValue);
            actualChanges.put(RECALCULATE_LOAN_SCHEDULE, true);
        }

        if (command.isChangeInBooleanParameterNamed(IS_FLOATING_INTEREST_RATE, this.isFloatingInterestRate)) {
            final Boolean newValue = command.booleanObjectValueOfParameterNamed(IS_FLOATING_INTEREST_RATE);
            actualChanges.put(IS_FLOATING_INTEREST_RATE, newValue);
            this.isFloatingInterestRate = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamed(INTEREST_RATE_DIFFERENTIAL, this.interestRateDifferential)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(INTEREST_RATE_DIFFERENTIAL);
            actualChanges.put(INTEREST_RATE_DIFFERENTIAL, newValue);
            this.interestRateDifferential = newValue;
        }

        Long existingFundId = null;
        if (this.fund != null) {
            existingFundId = this.fund.getId();
        }
        if (command.isChangeInLongParameterNamed(FUND_ID, existingFundId)) {
            final Long newValue = command.longValueOfParameterNamed(FUND_ID);
            actualChanges.put(FUND_ID, newValue);
        }

        Long existingLoanOfficerId = null;
        if (this.loanOfficer != null) {
            existingLoanOfficerId = this.loanOfficer.getId();
        }

        if (command.isChangeInLongParameterNamed(LOAN_OFFICER_ID, existingLoanOfficerId)) {
            final Long newValue = command.longValueOfParameterNamed(LOAN_OFFICER_ID);
            actualChanges.put(LOAN_OFFICER_ID, newValue);
        }

        Long existingLoanAssignorId = null;
        if (this.loanAssignor != null) {
            existingLoanAssignorId = this.loanAssignor.getId();
        }

        if (command.isChangeInLongParameterNamed(LOAN_ASSIGNOR_ID, existingLoanAssignorId)) {
            final Long newValue = command.longValueOfParameterNamed(LOAN_ASSIGNOR_ID);
            actualChanges.put(LOAN_ASSIGNOR_ID, newValue);
        }

        Long existingLoanPurposeId = null;
        if (this.loanPurpose != null) {
            existingLoanPurposeId = this.loanPurpose.getId();
        }

        if (command.isChangeInLongParameterNamed(LOAN_PURPOSE_ID, existingLoanPurposeId)) {
            final Long newValue = command.longValueOfParameterNamed(LOAN_PURPOSE_ID);
            actualChanges.put(LOAN_PURPOSE_ID, newValue);
        }

        if (command.isChangeInStringParameterNamed(TRANSACTION_PROCESSING_STRATEGY_CODE, transactionProcessingStrategyCode)) {
            final String newValue = command.stringValueOfParameterNamed(TRANSACTION_PROCESSING_STRATEGY_CODE);
            actualChanges.put(TRANSACTION_PROCESSING_STRATEGY_CODE, newValue);
        }

        if (command.isChangeInLocalDateParameterNamed(SUBMITTED_ON_DATE, getSubmittedOnDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(SUBMITTED_ON_DATE);
            actualChanges.put(SUBMITTED_ON_DATE, valueAsInput);
            actualChanges.put(DATE_FORMAT, dateFormatAsInput);
            actualChanges.put(LOCALE, localeAsInput);

            this.submittedOnDate = command.localDateValueOfParameterNamed(SUBMITTED_ON_DATE);
        }

        if (command.isChangeInLocalDateParameterNamed(EXPECTED_DISBURSEMENT_DATE, getExpectedDisbursedOnLocalDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(EXPECTED_DISBURSEMENT_DATE);
            actualChanges.put(EXPECTED_DISBURSEMENT_DATE, valueAsInput);
            actualChanges.put(DATE_FORMAT, dateFormatAsInput);
            actualChanges.put(LOCALE, localeAsInput);
            actualChanges.put(RECALCULATE_LOAN_SCHEDULE, true);

            this.expectedDisbursementDate = command.localDateValueOfParameterNamed(EXPECTED_DISBURSEMENT_DATE);
            removeFirstDisbursementTransaction();
        }

        if (command.isChangeInLocalDateParameterNamed(REPAYMENTS_STARTING_FROM_DATE, getExpectedFirstRepaymentOnDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(REPAYMENTS_STARTING_FROM_DATE);
            actualChanges.put(REPAYMENTS_STARTING_FROM_DATE, valueAsInput);
            actualChanges.put(DATE_FORMAT, dateFormatAsInput);
            actualChanges.put(LOCALE, localeAsInput);
            actualChanges.put(RECALCULATE_LOAN_SCHEDULE, true);

            this.expectedFirstRepaymentOnDate = command.localDateValueOfParameterNamed(REPAYMENTS_STARTING_FROM_DATE);
        }

        if (command.isChangeInBooleanParameterNamed(SYNC_DISBURSEMENT_WITH_MEETING, isSyncDisbursementWithMeeting())) {
            final Boolean valueAsInput = command.booleanObjectValueOfParameterNamed(SYNC_DISBURSEMENT_WITH_MEETING);
            actualChanges.put(SYNC_DISBURSEMENT_WITH_MEETING, valueAsInput);
            this.syncDisbursementWithMeeting = valueAsInput;
        }

        if (command.isChangeInLocalDateParameterNamed(INTEREST_CHARGED_FROM_DATE, getInterestChargedFromDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(INTEREST_CHARGED_FROM_DATE);
            actualChanges.put(INTEREST_CHARGED_FROM_DATE, valueAsInput);
            actualChanges.put(DATE_FORMAT, dateFormatAsInput);
            actualChanges.put(LOCALE, localeAsInput);
            actualChanges.put(RECALCULATE_LOAN_SCHEDULE, true);

            this.interestChargedFromDate = command.localDateValueOfParameterNamed(INTEREST_CHARGED_FROM_DATE);
        }

        // the comparison should be done with the tenant date
        // (DateUtils.getBusinessDate()) and not the server date (new
        // LocalDate())
        if (DateUtils.isDateInTheFuture(getSubmittedOnDate())) {
            final String errorMessage = "The date on which a loan is submitted cannot be in the future.";
            throw new InvalidLoanStateTransitionException(Loan.SUBMITTAL_PARAM, Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage,
                    getSubmittedOnDate());
        }

        if (this.client != null && DateUtils.isBefore(getSubmittedOnDate(), this.client.getActivationDate())) {
            final String errorMessage = "The date on which a loan is submitted cannot be earlier than client's activation date.";
            throw new InvalidLoanStateTransitionException(Loan.SUBMITTAL_PARAM, "cannot.be.before.client.activation.date", errorMessage,
                    getSubmittedOnDate());
        } else if (this.group != null && DateUtils.isBefore(getSubmittedOnDate(), this.group.getActivationDate())) {
            final String errorMessage = "The date on which a loan is submitted cannot be earlier than groups's activation date.";
            throw new InvalidLoanStateTransitionException(Loan.SUBMITTAL_PARAM, "cannot.be.before.group.activation.date", errorMessage,
                    getSubmittedOnDate());
        }

        if (DateUtils.isAfter(getSubmittedOnDate(), getExpectedDisbursedOnLocalDate())) {
            final String errorMessage = "The date on which a loan is submitted cannot be after its expected disbursement date: "
                    + getExpectedDisbursedOnLocalDate().toString();
            throw new InvalidLoanStateTransitionException(Loan.SUBMITTAL_PARAM, "cannot.be.after.expected.disbursement.date", errorMessage,
                    getSubmittedOnDate(), getExpectedDisbursedOnLocalDate());
        }

        if (isChargesModified) {
            actualChanges.put(PARAM_CHARGES, getLoanCharges(possiblyModifedLoanCharges));
            actualChanges.put(RECALCULATE_LOAN_SCHEDULE, true);
        }

        if (command.parameterExists(PARAM_COLLATERAL) && possiblyModifiedLoanCollateralItems != null
                && !possiblyModifiedLoanCollateralItems.isEmpty()) {
            Set<LoanCollateralManagement> loanCollateralManagementsV2 = this.loanCollateralManagements;
            boolean isTrue = possiblyModifiedLoanCollateralItems.equals(loanCollateralManagementsV2);

            if (!isTrue) {
                actualChanges.put(PARAM_COLLATERAL, getLoanCollateralDataFormCommand(possiblyModifiedLoanCollateralItems));
            }
        }

        if (command.isChangeInIntegerParameterNamed(LOAN_TERM_FREQUENCY, this.termFrequency)) {
            final Integer newValue = command.integerValueOfParameterNamed(LOAN_TERM_FREQUENCY);
            actualChanges.put(LOAN_TERM_FREQUENCY, newValue);
            this.termFrequency = newValue;
        }

        if (command.isChangeInIntegerParameterNamed(LOAN_TERM_FREQUENCY_TYPE, this.termPeriodFrequencyType)) {
            final Integer newValue = command.integerValueOfParameterNamed(LOAN_TERM_FREQUENCY_TYPE);
            final PeriodFrequencyType newTermPeriodFrequencyType = PeriodFrequencyType.fromInt(newValue);
            actualChanges.put(LOAN_TERM_FREQUENCY_TYPE, newTermPeriodFrequencyType.getValue());
            this.termPeriodFrequencyType = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamed(PRINCIPAL, this.approvedPrincipal)) {
            this.approvedPrincipal = command.bigDecimalValueOfParameterNamed(PRINCIPAL);
        }

        if (command.isChangeInBigDecimalParameterNamed(PRINCIPAL, this.proposedPrincipal)) {
            this.proposedPrincipal = command.bigDecimalValueOfParameterNamed(PRINCIPAL);
        }

        if (loanProduct.isMultiDisburseLoan()) {
            updateDisbursementDetails(command, actualChanges);
            if (command.isChangeInBigDecimalParameterNamed(LoanApiConstants.maxOutstandingBalanceParameterName,
                    this.maxOutstandingLoanBalance)) {
                this.maxOutstandingLoanBalance = command
                        .bigDecimalValueOfParameterNamed(LoanApiConstants.maxOutstandingBalanceParameterName);
            }
            final JsonArray disbursementDataArray = command.arrayOfParameterNamed(LoanApiConstants.disbursementDataParameterName);

            if (loanProduct.isDisallowExpectedDisbursements()) {
                if (disbursementDataArray != null && !disbursementDataArray.isEmpty()) {
                    final String errorMessage = "For this loan product, disbursement details are not allowed";
                    throw new MultiDisbursementDataNotAllowedException(LoanApiConstants.disbursementDataParameterName, errorMessage);
                }
            } else {
                if (disbursementDataArray == null || disbursementDataArray.size() == 0) {
                    final String errorMessage = "For this loan product, disbursement details must be provided";
                    throw new MultiDisbursementDataRequiredException(LoanApiConstants.disbursementDataParameterName, errorMessage);
                }

                if (disbursementDataArray.size() > loanProduct.maxTrancheCount()) {
                    final String errorMessage = "Number of tranche shouldn't be greter than " + loanProduct.maxTrancheCount();
                    throw new ExceedingTrancheCountException(LoanApiConstants.disbursementDataParameterName, errorMessage,
                            loanProduct.maxTrancheCount(), disbursementDetails.size());
                }
            }
        } else {
            this.disbursementDetails.clear();
        }

        if (loanProduct.isMultiDisburseLoan() || loanProduct.canDefineInstallmentAmount()) {
            if (command.isChangeInBigDecimalParameterNamed(LoanApiConstants.emiAmountParameterName, this.fixedEmiAmount)) {
                this.fixedEmiAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.emiAmountParameterName);
                actualChanges.put(LoanApiConstants.emiAmountParameterName, this.fixedEmiAmount);
                actualChanges.put(RECALCULATE_LOAN_SCHEDULE, true);
            }
        } else {
            this.fixedEmiAmount = null;
        }

        if (command.isChangeInBigDecimalParameterNamed(LoanApiConstants.fixedPrincipalPercentagePerInstallmentParamName,
                this.fixedPrincipalPercentagePerInstallment)) {
            this.fixedPrincipalPercentagePerInstallment = command
                    .bigDecimalValueOfParameterNamed(LoanApiConstants.fixedPrincipalPercentagePerInstallmentParamName);
            actualChanges.put(LoanApiConstants.fixedPrincipalPercentagePerInstallmentParamName,
                    this.fixedPrincipalPercentagePerInstallment);
        }

        return actualChanges;
    }

    public void recalculateAllCharges() {
        Set<LoanCharge> loanCharges = this.getActiveCharges();
        for (final LoanCharge loanCharge : loanCharges) {
            recalculateLoanCharge(loanCharge);
        }
        updateSummaryWithTotalFeeChargesDueAtDisbursement(deriveSumTotalOfChargesDueAtDisbursement());
    }

    public boolean isInterestRecalculationEnabledForProduct() {
        return this.loanProduct.isInterestRecalculationEnabled();
    }

    public boolean isMultiDisburmentLoan() {
        return this.loanProduct.isMultiDisburseLoan();
    }

    /**
     * Update interest recalculation settings if product configuration changes
     */

    private void updateOverdueScheduleInstallment(final LoanCharge loanCharge) {
        if (loanCharge.isOverdueInstallmentCharge() && loanCharge.isActive()) {
            LoanOverdueInstallmentCharge overdueInstallmentCharge = loanCharge.getOverdueInstallmentCharge();
            if (overdueInstallmentCharge != null) {
                Integer installmentNumber = overdueInstallmentCharge.getInstallment().getInstallmentNumber();
                LoanRepaymentScheduleInstallment installment = fetchRepaymentScheduleInstallment(installmentNumber);
                overdueInstallmentCharge.updateLoanRepaymentScheduleInstallment(installment);
            }
        }
    }

    private void updateLifeInsuranceChargePortionsForRevolvingLoan() {
        if (!isRevolvingLoan() || getRepaymentScheduleInstallments().isEmpty()) {
            return;
        }

        // Find first installment with non-null life insurance charge portion
        LoanRepaymentScheduleInstallment sourceInstallment = getRepaymentScheduleInstallments().stream()
                .filter(installment -> installment.getLifeInsuranceChargePortion() != null).findFirst().orElse(null);

        if (sourceInstallment == null) {
            return; // No source installment found with life insurance charge portion
        }

        int firstRepaymentDueDate = sourceInstallment.getDueDate().getDayOfMonth();

        for (LoanRepaymentScheduleInstallment installment : getRepaymentScheduleInstallments()) {
            LocalDate startDate = installment.getFromDate();

            startDate = startDate.plusDays(1);

            // Check if firstRepaymentDueDate day is bigger than installment due date max day of month
            if (firstRepaymentDueDate > installment.getFromDate().lengthOfMonth()) {
                firstRepaymentDueDate = installment.getFromDate().lengthOfMonth();
            }

            // Create a new date obj based on installment from date, but using the day "firstRepaymentDueDate"
            LocalDate chargeDate = installment.getFromDate().withDayOfMonth(firstRepaymentDueDate);

            // Adjust chargeDate to be in the same month as startDate when in different months
            if (!chargeDate.getMonth().equals(startDate.getMonth())) {
                chargeDate = startDate.withDayOfMonth(firstRepaymentDueDate);
            }

            // If so, calculate life insurance charge portion
            if (DateUtils.isDateWithinRange(chargeDate, startDate, installment.getDueDate())) {
                installment.setLifeInsuranceChargePortion(sourceInstallment.getLifeInsuranceChargePortion());
            } else {
                installment.setLifeInsuranceChargePortion(null);
            }
        }
    }

    private void recalculateLoanCharge(final LoanCharge loanCharge) {

        updateLifeInsuranceChargePortionsForRevolvingLoan();
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal chargeAmt;
        BigDecimal totalChargeAmt = BigDecimal.ZERO;
        if (!loanCharge.isOverdueInstallmentCharge()) {
            if (loanCharge.getChargeCalculation().isPercentageBased() && !loanCharge.getLoan().isMigratedLoan()) {
                amount = calculateAmountPercentageAppliedTo(loanCharge);
                chargeAmt = loanCharge.getPercentage();
                if (loanCharge.isInstalmentFee()) {
                    totalChargeAmt = calculatePerInstallmentChargeAmount(loanCharge);
                }
            } else {
                if (loanCharge.isCustomFlatDistributedCharge()) {
                    chargeAmt = loanCharge.getAmount(loanCharge.getLoan().getCurrency()).getAmount();
                } else {
                    chargeAmt = loanCharge.amountOrPercentage();
                }
            }
            if (loanCharge.isActive()) {
                clearLoanInstallmentChargesBeforeRegeneration(loanCharge);
                loanCharge.update(chargeAmt, loanCharge.getDueLocalDate(), amount, fetchNumberOfInstallmensAfterExceptions(),
                        totalChargeAmt);
                validateChargeHasValidSpecifiedDateIfApplicable(loanCharge, getDisbursementDate());
            }
        }
    }

    @SuppressWarnings({ "squid:S3776" })
    public void clearLoanInstallmentChargesBeforeRegeneration(final LoanCharge loanCharge) {
        Loan loan = loanCharge.getLoan();
        if (loan.isDisbursed() && this.getLoanProductRelatedDetail().getLoanScheduleType().equals(LoanScheduleType.PROGRESSIVE)
                && loanCharge.isInstalmentFee()) {

            loanCharge.clearLoanInstallmentCharges();
            for (final LoanRepaymentScheduleInstallment installment : getRepaymentScheduleInstallments()) {
                if (installment.isRecalculatedInterestComponent()) {
                    continue; // JW: does this in generateInstallmentLoanCharges - but don't understand it
                }
                for (LoanInstallmentCharge installmentCharge : installment.getInstallmentCharges()) {
                    if (Objects.equals(installmentCharge.getLoanCharge().getId(), loanCharge.getId())) {
                        installment.getInstallmentCharges().remove(installmentCharge);
                        break;
                    }
                }
            }
        }

        if (!loan.isSubmittedAndPendingApproval() && !loan.isApproved()) {
            return;
        } // doing for both just in case status is not
          // updated at this points
        if (loanCharge.isInstalmentFee()) {
            loanCharge.clearLoanInstallmentCharges();
            for (final LoanRepaymentScheduleInstallment installment : getRepaymentScheduleInstallments()) {
                if (installment.isRecalculatedInterestComponent()) {
                    continue; // JW: does this in generateInstallmentLoanCharges - but don't understand it
                }
                installment.getInstallmentCharges().clear();
            }
        }
    }

    // This method returns date format and locale if present in the JsonCommand
    private Map<String, String> getDateFormatAndLocale(final JsonCommand jsonCommand) {
        Map<String, String> returnObject = new HashMap<>();
        JsonElement jsonElement = jsonCommand.parsedJson();
        if (jsonElement.isJsonObject()) {
            JsonObject topLevel = jsonElement.getAsJsonObject();
            if (topLevel.has(LoanApiConstants.dateFormatParameterName)
                    && topLevel.get(LoanApiConstants.dateFormatParameterName).isJsonPrimitive()) {
                final JsonPrimitive primitive = topLevel.get(LoanApiConstants.dateFormatParameterName).getAsJsonPrimitive();
                returnObject.put(LoanApiConstants.dateFormatParameterName, primitive.getAsString());
            }
            if (topLevel.has(LoanApiConstants.localeParameterName)
                    && topLevel.get(LoanApiConstants.localeParameterName).isJsonPrimitive()) {
                final JsonPrimitive primitive = topLevel.get(LoanApiConstants.localeParameterName).getAsJsonPrimitive();
                String localeString = primitive.getAsString();
                returnObject.put(LoanApiConstants.localeParameterName, localeString);
            }
        }
        return returnObject;
    }

    private Map<String, Object> parseDisbursementDetails(final JsonObject jsonObject, String dateFormat, Locale locale) {
        Map<String, Object> returnObject = new HashMap<>();
        if (jsonObject.get(LoanApiConstants.expectedDisbursementDateParameterName) != null
                && jsonObject.get(LoanApiConstants.expectedDisbursementDateParameterName).isJsonPrimitive()) {
            final JsonPrimitive primitive = jsonObject.get(LoanApiConstants.expectedDisbursementDateParameterName).getAsJsonPrimitive();
            final String valueAsString = primitive.getAsString();
            if (StringUtils.isNotBlank(valueAsString)) {
                LocalDate date = JsonParserHelper.convertFrom(valueAsString, LoanApiConstants.expectedDisbursementDateParameterName,
                        dateFormat, locale);
                if (date != null) {
                    returnObject.put(LoanApiConstants.expectedDisbursementDateParameterName, date);
                }
            }
        }

        if (jsonObject.get(LoanApiConstants.disbursementPrincipalParameterName).isJsonPrimitive()
                && StringUtils.isNotBlank(jsonObject.get(LoanApiConstants.disbursementPrincipalParameterName).getAsString())) {
            BigDecimal principal = jsonObject.getAsJsonPrimitive(LoanApiConstants.disbursementPrincipalParameterName).getAsBigDecimal();
            returnObject.put(LoanApiConstants.disbursementPrincipalParameterName, principal);
        }

        if (jsonObject.has(LoanApiConstants.disbursementIdParameterName)
                && jsonObject.get(LoanApiConstants.disbursementIdParameterName).isJsonPrimitive()
                && StringUtils.isNotBlank(jsonObject.get(LoanApiConstants.disbursementIdParameterName).getAsString())) {
            Long id = jsonObject.getAsJsonPrimitive(LoanApiConstants.disbursementIdParameterName).getAsLong();
            returnObject.put(LoanApiConstants.disbursementIdParameterName, id);
        }

        if (jsonObject.has(LoanApiConstants.loanChargeIdParameterName)
                && jsonObject.get(LoanApiConstants.loanChargeIdParameterName).isJsonPrimitive()
                && StringUtils.isNotBlank(jsonObject.get(LoanApiConstants.loanChargeIdParameterName).getAsString())) {
            returnObject.put(LoanApiConstants.loanChargeIdParameterName,
                    jsonObject.getAsJsonPrimitive(LoanApiConstants.loanChargeIdParameterName).getAsString());
        }
        return returnObject;
    }

    @SuppressWarnings({ "squid:S3776" })
    public void updateDisbursementDetails(final JsonCommand jsonCommand, final Map<String, Object> actualChanges) {

        List<Long> disbursementList = fetchDisbursementIds();
        List<Long> loanChargeIds = fetchLoanTrancheChargeIds();
        int chargeIdLength = loanChargeIds.size();
        String chargeIds = null;
        // From modify application page, if user removes all charges, we should
        // get empty array.
        // So we need to remove all charges applied for this loan
        boolean removeAllChages = false;
        if (jsonCommand.parameterExists(LoanApiConstants.chargesParameterName)) {
            JsonArray chargesArray = jsonCommand.arrayOfParameterNamed(LoanApiConstants.chargesParameterName);
            if (chargesArray.size() == 0) {
                removeAllChages = true;
            }
        }

        if (jsonCommand.parameterExists(LoanApiConstants.disbursementDataParameterName)) {
            final JsonArray disbursementDataArray = jsonCommand.arrayOfParameterNamed(LoanApiConstants.disbursementDataParameterName);
            if (disbursementDataArray != null && disbursementDataArray.size() > 0) {
                String dateFormat = null;
                Locale locale = null;
                // Gets date format and locate
                Map<String, String> dateAndLocale = getDateFormatAndLocale(jsonCommand);
                dateFormat = dateAndLocale.get(LoanApiConstants.dateFormatParameterName);
                if (dateAndLocale.containsKey(LoanApiConstants.localeParameterName)) {
                    locale = JsonParserHelper.localeFromString(dateAndLocale.get(LoanApiConstants.localeParameterName));
                }
                for (JsonElement jsonElement : disbursementDataArray) {
                    final JsonObject jsonObject = jsonElement.getAsJsonObject();
                    Map<String, Object> parsedDisbursementData = parseDisbursementDetails(jsonObject, dateFormat, locale);
                    LocalDate expectedDisbursementDateV2 = (LocalDate) parsedDisbursementData
                            .get(LoanApiConstants.expectedDisbursementDateParameterName);
                    BigDecimal principal = (BigDecimal) parsedDisbursementData.get(LoanApiConstants.disbursementPrincipalParameterName);
                    Long disbursementID = (Long) parsedDisbursementData.get(LoanApiConstants.disbursementIdParameterName);
                    chargeIds = (String) parsedDisbursementData.get(LoanApiConstants.loanChargeIdParameterName);
                    if (chargeIds != null) {
                        if (chargeIds.indexOf(",") != -1) {
                            Iterable<String> chargeId = Splitter.on(',').split(chargeIds);
                            for (String loanChargeId : chargeId) {
                                loanChargeIds.remove(Long.parseLong(loanChargeId));
                            }
                        } else {
                            loanChargeIds.remove(Long.parseLong(chargeIds));
                        }
                    }
                    createOrUpdateDisbursementDetails(disbursementID, actualChanges, expectedDisbursementDateV2, principal,
                            disbursementList);
                }
                removeDisbursementAndAssociatedCharges(actualChanges, disbursementList, loanChargeIds, chargeIdLength, removeAllChages);
            }
        }
    }

    private void removeDisbursementAndAssociatedCharges(final Map<String, Object> actualChanges, List<Long> disbursementList,
            List<Long> loanChargeIds, int chargeIdLength, boolean removeAllChages) {
        if (removeAllChages) {
            LoanCharge[] tempCharges = new LoanCharge[this.charges.size()];
            this.charges.toArray(tempCharges);
            for (LoanCharge loanCharge : tempCharges) {
                removeLoanCharge(loanCharge);
            }
            this.trancheCharges.clear();
        } else {
            if (!loanChargeIds.isEmpty() && loanChargeIds.size() != chargeIdLength) {
                for (Long chargeId : loanChargeIds) {
                    LoanCharge deleteCharge = fetchLoanChargesById(chargeId);
                    if (this.charges.contains(deleteCharge)) {
                        removeLoanCharge(deleteCharge);
                    }
                }
            }
        }
        for (Long id : disbursementList) {
            removeChargesByDisbursementID(id);
            this.disbursementDetails.remove(fetchLoanDisbursementsById(id));
            actualChanges.put(RECALCULATE_LOAN_SCHEDULE, true);
        }
    }

    private void createOrUpdateDisbursementDetails(Long disbursementID, final Map<String, Object> actualChanges,
            LocalDate expectedDisbursementDate, BigDecimal principal, List<Long> existingDisbursementList) {

        if (disbursementID != null) {
            LoanDisbursementDetails loanDisbursementDetail = fetchLoanDisbursementsById(disbursementID);
            existingDisbursementList.remove(disbursementID);
            if (loanDisbursementDetail.actualDisbursementDate() == null) {
                LocalDate actualDisbursementDateV2 = null;
                LoanDisbursementDetails disbursementDetailsV2 = new LoanDisbursementDetails(expectedDisbursementDate,
                        actualDisbursementDateV2, principal, this.netDisbursalAmount, false);
                disbursementDetailsV2.updateLoan(this);
                if (!loanDisbursementDetail.equals(disbursementDetailsV2)) {
                    loanDisbursementDetail.copy(disbursementDetailsV2);
                    actualChanges.put("disbursementDetailId", disbursementID);
                    actualChanges.put(RECALCULATE_LOAN_SCHEDULE, true);
                }
            }
        } else {
            LocalDate actualDisbursementDateV2 = null;
            LoanDisbursementDetails disbursementDetailsV2 = new LoanDisbursementDetails(expectedDisbursementDate, actualDisbursementDateV2,
                    principal, this.netDisbursalAmount, false);
            disbursementDetailsV2.updateLoan(this);
            this.disbursementDetails.add(disbursementDetailsV2);
            for (LoanTrancheCharge trancheCharge : trancheCharges) {
                Charge chargeDefinition = trancheCharge.getCharge();
                ExternalId externalIdV2 = ExternalId.empty();
                if (TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
                    externalIdV2 = ExternalId.generate();
                }
                final LoanCharge loanCharge = new LoanCharge(this, chargeDefinition, principal, null, null, null, expectedDisbursementDate,
                        null, null, BigDecimal.ZERO, externalIdV2, false, null);
                LoanTrancheDisbursementCharge loanTrancheDisbursementCharge = new LoanTrancheDisbursementCharge(loanCharge,
                        disbursementDetailsV2);
                loanCharge.updateLoanTrancheDisbursementCharge(loanTrancheDisbursementCharge);
                addLoanCharge(loanCharge);
            }
            actualChanges.put(LoanApiConstants.disbursementDataParameterName, expectedDisbursementDate + "-" + principal);
            actualChanges.put(RECALCULATE_LOAN_SCHEDULE, true);
        }
    }

    private void removeChargesByDisbursementID(Long id) {
        List<LoanCharge> tempCharges = new ArrayList<>();
        for (LoanCharge charge : getCharges()) {
            LoanTrancheDisbursementCharge transCharge = charge.getTrancheDisbursementCharge();
            if (transCharge != null && id.equals(transCharge.getloanDisbursementDetails().getId())) {
                tempCharges.add(charge);
            }
        }
        for (LoanCharge charge : tempCharges) {
            removeLoanCharge(charge);
        }
    }

    private List<Long> fetchLoanTrancheChargeIds() {
        List<Long> list = new ArrayList<>();
        for (LoanCharge charge : getCharges()) {
            if (charge.isTrancheDisbursementCharge() && charge.isActive()) {
                list.add(charge.getId());
            }
        }
        return list;
    }

    public LoanDisbursementDetails fetchLoanDisbursementsById(Long id) {
        LoanDisbursementDetails loanDisbursementDetail = null;
        for (LoanDisbursementDetails disbursementDetail : getDisbursementDetails()) {
            if (id.equals(disbursementDetail.getId())) {
                loanDisbursementDetail = disbursementDetail;
                break;
            }
        }
        return loanDisbursementDetail;
    }

    private List<Long> fetchDisbursementIds() {
        List<Long> list = new ArrayList<>();
        for (LoanDisbursementDetails disbursementDetailsV2 : getDisbursementDetails()) {
            list.add(disbursementDetailsV2.getId());
        }
        return list;
    }

    private LoanCollateralManagementData[] getLoanCollateralDataFormCommand(final Set<LoanCollateralManagement> setOfLoanCollateral) {

        LoanCollateralManagementData[] existingLoanCollateral = null;

        final List<LoanCollateralManagementData> loanCollateralList = new ArrayList<>();
        for (final LoanCollateralManagement loanCollateral : setOfLoanCollateral) {

            loanCollateralList.add(loanCollateral.toCommand());

        }

        existingLoanCollateral = loanCollateralList.toArray(new LoanCollateralManagementData[0]);

        return existingLoanCollateral;
    }

    private LoanChargeCommand[] getLoanCharges(final Set<LoanCharge> setOfLoanCharges) {

        LoanChargeCommand[] existingLoanCharges = null;

        final List<LoanChargeCommand> loanChargesList = new ArrayList<>();
        for (final LoanCharge loanCharge : setOfLoanCharges) {
            loanChargesList.add(loanCharge.toCommand());
        }

        existingLoanCharges = loanChargesList.toArray(new LoanChargeCommand[0]);

        return existingLoanCharges;
    }

    private void removeFirstDisbursementTransaction() {
        List<LoanTransaction> transactions = getLoanTransactions();
        for (final LoanTransaction loanTransaction : transactions) {
            if (loanTransaction.isDisbursement()) {
                removeLoanTransaction(loanTransaction);
                break;
            }
        }
    }

    @SuppressWarnings({ "squid:S3776", "squid:S3655" })
    public void loanApplicationSubmittal(final LoanScheduleModel loanSchedule, final LoanApplicationTerms loanApplicationTerms,
            final LoanLifecycleStateMachine lifecycleStateMachine, final LocalDate submittedOn, final ExternalId externalId,
            final boolean allowTransactionsOnHoliday, final List<Holiday> holidays, final WorkingDays workingDays,
            final boolean allowTransactionsOnNonWorkingDay) {
        updateLoanSchedule(loanSchedule);
        updateLoanDerivedFields();

        lifecycleStateMachine.transition(LoanEvent.LOAN_CREATED, this);

        this.externalId = externalId;
        this.termFrequency = loanApplicationTerms.getLoanTermFrequency();
        this.termPeriodFrequencyType = loanApplicationTerms.getLoanTermPeriodFrequencyType().getValue();
        this.submittedOnDate = submittedOn;
        this.expectedDisbursementDate = loanApplicationTerms.getExpectedDisbursementDate();
        this.expectedFirstRepaymentOnDate = loanApplicationTerms.getRepaymentStartFromDate();
        this.interestChargedFromDate = loanApplicationTerms.getInterestChargedFromDate();

        updateLoanScheduleDependentDerivedFields();

        if (DateUtils.isDateInTheFuture(submittedOn)) {
            final String errorMessage = "The date on which a loan is submitted cannot be in the future.";
            throw new InvalidLoanStateTransitionException(Loan.SUBMITTAL_PARAM, Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage,
                    submittedOn, DateUtils.getBusinessLocalDate());
        }

        if (this.client != null && this.client.isActivatedAfter(submittedOn)) {
            final String errorMessage = "The date on which a loan is submitted cannot be earlier than client's activation date.";
            throw new InvalidLoanStateTransitionException(Loan.SUBMITTAL_PARAM, "cannot.be.before.client.activation.date", errorMessage,
                    submittedOn, client.getActivationDate());
        }

        validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.LOAN_CREATED, submittedOn);

        if (this.group != null && this.group.isActivatedAfter(submittedOn)) {
            final String errorMessage = "The date on which a loan is submitted cannot be earlier than groups's activation date.";
            throw new InvalidLoanStateTransitionException(Loan.SUBMITTAL_PARAM, "cannot.be.before.group.activation.date", errorMessage,
                    submittedOn, group.getActivationDate());
        }

        if (DateUtils.isAfter(submittedOn, getExpectedDisbursedOnLocalDate())) {
            final String errorMessage = "The date on which a loan is submitted cannot be after its expected disbursement date: "
                    + getExpectedDisbursedOnLocalDate().toString();
            throw new InvalidLoanStateTransitionException(Loan.SUBMITTAL_PARAM, "cannot.be.after.expected.disbursement.date", errorMessage,
                    submittedOn, getExpectedDisbursedOnLocalDate());
        }
        for (final LoanCharge loanCharge : getActiveCharges()) {
            recalculateLoanCharge(loanCharge);
        }

        updateSummaryWithTotalFeeChargesDueAtDisbursement(deriveSumTotalOfChargesDueAtDisbursement());

        // validate if disbursement date is a holiday or a non-working day
        validateDisbursementDateIsOnNonWorkingDay(workingDays, allowTransactionsOnNonWorkingDay);
        validateDisbursementDateIsOnHoliday(allowTransactionsOnHoliday, holidays);

        // Copy interest recalculation settings if interest recalculation is enabled
        if (this.loanRepaymentScheduleDetail.isInterestRecalculationEnabled()) {
            this.loanInterestRecalculationDetails = LoanInterestRecalculationDetails
                    .createFrom(this.loanProduct.getProductInterestRecalculationDetails());
            this.loanInterestRecalculationDetails.updateLoan(this);
        }

    }

    private LocalDate determineExpectedMaturityDate() {
        final int numberOfInstallments = this.repaymentScheduleInstallments.size();
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        LocalDate maturityDate = installments.get(numberOfInstallments - 1).getDueDate();
        ListIterator<LoanRepaymentScheduleInstallment> iterator = installments.listIterator(numberOfInstallments);
        while (iterator.hasPrevious()) {
            LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = iterator.previous();
            if (!loanRepaymentScheduleInstallment.isRecalculatedInterestComponent()) {
                maturityDate = loanRepaymentScheduleInstallment.getDueDate();
                break;
            }
        }
        return maturityDate;
    }

    public Map<String, Object> loanApplicationRejection(final AppUser currentUser, final JsonCommand command,
            final LoanLifecycleStateMachine loanLifecycleStateMachine) {

        validateAccountStatus(LoanEvent.LOAN_REJECTED);

        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final LoanStatus statusEnum = loanLifecycleStateMachine.dryTransition(LoanEvent.LOAN_REJECTED, this);
        if (!statusEnum.hasStateOf(LoanStatus.fromInt(this.loanStatus))) {
            final LocalDate rejectedOn = command.localDateValueOfParameterNamed(REJECTED_ON_DATE);

            final Locale locale = new Locale(command.locale());
            final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(locale);

            this.rejectedOnDate = rejectedOn;
            this.rejectedBy = currentUser;
            this.closedOnDate = rejectedOn;
            this.closedBy = currentUser;

            loanLifecycleStateMachine.transition(LoanEvent.LOAN_REJECTED, this);
            actualChanges.put(PARAM_STATUS, LoanEnumerations.status(this.loanStatus));

            actualChanges.put(LOCALE, command.locale());
            actualChanges.put(DATE_FORMAT, command.dateFormat());
            actualChanges.put(REJECTED_ON_DATE, rejectedOn.format(fmt));
            actualChanges.put(CLOSED_ON_DATE, rejectedOn.format(fmt));

            if (DateUtils.isBefore(rejectedOn, getSubmittedOnDate())) {
                final String errorMessage = "The date on which a loan is rejected cannot be before its submittal date: "
                        + getSubmittedOnDate().toString();
                throw new InvalidLoanStateTransitionException(Loan.REJECT_PARAM, Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_SUBMITTAL_DATE,
                        errorMessage, rejectedOn, getSubmittedOnDate());
            }

            validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.LOAN_REJECTED, rejectedOn);

            if (DateUtils.isDateInTheFuture(rejectedOn)) {
                final String errorMessage = "The date on which a loan is rejected cannot be in the future.";
                throw new InvalidLoanStateTransitionException(Loan.REJECT_PARAM, Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage,
                        rejectedOn);
            }
        } else {
            final String errorMessage = "Only the loan applications with status 'Submitted and pending approval' are allowed to be rejected.";
            throw new InvalidLoanStateTransitionException(Loan.REJECT_PARAM, "cannot.reject", errorMessage);
        }

        return actualChanges;
    }

    public Map<String, Object> loanApplicationWithdrawnByApplicant(final AppUser currentUser, final JsonCommand command,
            final LoanLifecycleStateMachine loanLifecycleStateMachine) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final LoanStatus statusEnum = loanLifecycleStateMachine.dryTransition(LoanEvent.LOAN_WITHDRAWN, this);
        if (!statusEnum.hasStateOf(LoanStatus.fromInt(this.loanStatus))) {
            loanLifecycleStateMachine.transition(LoanEvent.LOAN_WITHDRAWN, this);
            actualChanges.put(PARAM_STATUS, LoanEnumerations.status(this.loanStatus));

            LocalDate withdrawnOn = command.localDateValueOfParameterNamed(WITHDRAWN_ON_DATE);
            if (withdrawnOn == null) {
                withdrawnOn = command.localDateValueOfParameterNamed(EVENT_DATE);
            }

            final Locale locale = new Locale(command.locale());
            final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(locale);

            this.withdrawnOnDate = withdrawnOn;
            this.withdrawnBy = currentUser;
            this.closedOnDate = withdrawnOn;
            this.closedBy = currentUser;

            actualChanges.put(LOCALE, command.locale());
            actualChanges.put(DATE_FORMAT, command.dateFormat());
            actualChanges.put(WITHDRAWN_ON_DATE, withdrawnOn.format(fmt));
            actualChanges.put(CLOSED_ON_DATE, withdrawnOn.format(fmt));

            if (DateUtils.isBefore(withdrawnOn, getSubmittedOnDate())) {
                final String errorMessage = "The date on which a loan is withdrawn cannot be before its submittal date: "
                        + getSubmittedOnDate().toString();
                throw new InvalidLoanStateTransitionException(Loan.WITHDRAW_PARAM, Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_SUBMITTAL_DATE,
                        errorMessage, command, getSubmittedOnDate());
            }

            validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.LOAN_WITHDRAWN, withdrawnOn);

            if (DateUtils.isDateInTheFuture(withdrawnOn)) {
                final String errorMessage = "The date on which a loan is withdrawn cannot be in the future.";
                throw new InvalidLoanStateTransitionException(Loan.WITHDRAW_PARAM, Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage,
                        command);
            }
        } else {
            final String errorMessage = "Only the loan applications with status 'Submitted and pending approval' are allowed to be withdrawn by applicant.";
            throw new InvalidLoanStateTransitionException(Loan.WITHDRAW_PARAM, "cannot.withdraw", errorMessage);
        }

        return actualChanges;
    }

    @SuppressWarnings({ "squid:S3776" })
    public Map<String, Object> loanApplicationApproval(final AppUser currentUser, final JsonCommand command,
            final JsonArray disbursementDataArray, final LoanLifecycleStateMachine loanLifecycleStateMachine) {

        validateAccountStatus(LoanEvent.LOAN_APPROVED);

        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        /*
         * statusEnum is holding the possible new status derived from loanLifecycleStateMachine.transition.
         */

        final LoanStatus newStatusEnum = loanLifecycleStateMachine.dryTransition(LoanEvent.LOAN_APPROVED, this);

        if (!newStatusEnum.hasStateOf(LoanStatus.fromInt(this.loanStatus))) {
            loanLifecycleStateMachine.transition(LoanEvent.LOAN_APPROVED, this);
            actualChanges.put(PARAM_STATUS, LoanEnumerations.status(this.loanStatus));

            // only do below if status has changed in the 'approval' case
            LocalDate approvedOn = command.localDateValueOfParameterNamed(APPROVED_ON_DATE);
            String approvedOnDateChange = command.stringValueOfParameterNamed(APPROVED_ON_DATE);
            if (approvedOn == null) {
                approvedOn = command.localDateValueOfParameterNamed(EVENT_DATE);
                approvedOnDateChange = command.stringValueOfParameterNamed(EVENT_DATE);
            }

            LocalDate expecteddisbursementDate = command.localDateValueOfParameterNamed(EXPECTED_DISBURSEMENT_DATE);

            BigDecimal approvedLoanAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.approvedLoanAmountParameterName);
            if (approvedLoanAmount != null) {
                compareApprovedToProposedPrincipal(approvedLoanAmount);

                /*
                 * All the calculations are done based on the principal amount, so it is necessary to set principal
                 * amount to approved amount
                 */
                this.approvedPrincipal = approvedLoanAmount;

                this.loanRepaymentScheduleDetail.setPrincipal(approvedLoanAmount);
                actualChanges.put(LoanApiConstants.approvedLoanAmountParameterName, approvedLoanAmount);
                actualChanges.put(LoanApiConstants.disbursementPrincipalParameterName, approvedLoanAmount);
                actualChanges.put(LoanApiConstants.disbursementNetDisbursalAmountParameterName, netDisbursalAmount);

                /* Update disbursement details */
                if (disbursementDataArray != null) {
                    updateDisbursementDetails(command, actualChanges);
                }
            }

            recalculateAllCharges();

            if (loanProduct.isMultiDisburseLoan()) {
                List<LoanDisbursementDetails> currentDisbursementDetails = getDisbursementDetails();
                if (loanProduct.isDisallowExpectedDisbursements()) {
                    if (!currentDisbursementDetails.isEmpty()) {
                        final String errorMessage = "For this loan product, disbursement details are not allowed";
                        throw new MultiDisbursementDataNotAllowedException(LoanApiConstants.disbursementDataParameterName, errorMessage);
                    }
                } else {
                    if (currentDisbursementDetails.isEmpty()) {
                        final String errorMessage = "For this loan product, disbursement details must be provided";
                        throw new MultiDisbursementDataRequiredException(LoanApiConstants.disbursementDataParameterName, errorMessage);
                    }
                }

                if (currentDisbursementDetails.size() > loanProduct.maxTrancheCount()) {
                    final String errorMessage = "Number of tranche shouldn't be greter than " + loanProduct.maxTrancheCount();
                    throw new ExceedingTrancheCountException(LoanApiConstants.disbursementDataParameterName, errorMessage,
                            loanProduct.maxTrancheCount(), currentDisbursementDetails.size());
                }
            }
            this.approvedOnDate = approvedOn;
            this.approvedBy = currentUser;
            actualChanges.put(LOCALE, command.locale());
            actualChanges.put(DATE_FORMAT, command.dateFormat());
            actualChanges.put(APPROVED_ON_DATE, approvedOnDateChange);

            final LocalDate submittalDate = this.submittedOnDate;
            if (DateUtils.isBefore(approvedOn, submittalDate)) {
                final String errorMessage = "The date on which a loan is approved cannot be before its submittal date: " + submittalDate;
                throw new InvalidLoanStateTransitionException(Loan.APPROVAL_PARAM, Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_SUBMITTAL_DATE,
                        errorMessage, getApprovedOnDate(), submittalDate);
            }

            if (expecteddisbursementDate != null) {
                this.expectedDisbursementDate = expecteddisbursementDate;
                actualChanges.put(EXPECTED_DISBURSEMENT_DATE, expectedDisbursementDate);

                if (DateUtils.isBefore(expecteddisbursementDate, approvedOn)) {
                    final String errorMessage = "The expected disbursement date should be either on or after the approval date: "
                            + approvedOn.toString();
                    throw new InvalidLoanStateTransitionException("expecteddisbursal", "should.be.on.or.after.approval.date", errorMessage,
                            getApprovedOnDate(), expecteddisbursementDate);
                }
            }

            validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.LOAN_APPROVED, approvedOn);

            if (DateUtils.isDateInTheFuture(approvedOn)) {
                final String errorMessage = "The date on which a loan is approved cannot be in the future.";
                throw new InvalidLoanStateTransitionException(Loan.APPROVAL_PARAM, Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage,
                        getApprovedOnDate());
            }

            if (this.loanOfficer != null) {
                final LoanOfficerAssignmentHistory loanOfficerAssignmentHistory = LoanOfficerAssignmentHistory.createNew(this,
                        this.loanOfficer, approvedOn);
                this.loanOfficerHistory.add(loanOfficerAssignmentHistory);
            }
            this.adjustNetDisbursalAmount(this.approvedPrincipal);
        }

        return actualChanges;

    }

    private void compareApprovedToProposedPrincipal(BigDecimal approvedLoanAmount) {

        if (this.loanProduct().isDisallowExpectedDisbursements() && this.loanProduct().isAllowApprovedDisbursedAmountsOverApplied()) {
            BigDecimal maxApprovedLoanAmount = getOverAppliedMax();
            if (approvedLoanAmount.compareTo(maxApprovedLoanAmount) > 0) {
                final String errorMessage = "Loan approved amount can't be greater than maximum applied loan amount calculation.";
                throw new InvalidLoanStateTransitionException(Loan.APPROVAL_PARAM,
                        "amount.can't.be.greater.than.maximum.applied.loan.amount.calculation", errorMessage, approvedLoanAmount,
                        maxApprovedLoanAmount);
            }
        } else {
            if (approvedLoanAmount.compareTo(this.proposedPrincipal) > 0) {
                final String errorMessage = "Loan approved amount can't be greater than loan amount demanded.";
                throw new InvalidLoanStateTransitionException(Loan.APPROVAL_PARAM, "amount.can't.be.greater.than.loan.amount.demanded",
                        errorMessage, this.proposedPrincipal, approvedLoanAmount);
            }
        }
    }

    private BigDecimal getOverAppliedMax() {
        BigDecimal maxAmount = null;
        if (this.getLoanProduct().getOverAppliedCalculationType().equals("percentage")) {
            BigDecimal overAppliedNumber = BigDecimal.valueOf(getLoanProduct().getOverAppliedNumber());
            BigDecimal x = overAppliedNumber.divide(BigDecimal.valueOf(100));
            BigDecimal totalPercentage = BigDecimal.valueOf(1).add(x);
            maxAmount = this.proposedPrincipal.multiply(totalPercentage);
        } else {
            maxAmount = this.proposedPrincipal.add(BigDecimal.valueOf(getLoanProduct().getOverAppliedNumber()));
        }
        return maxAmount;
    }

    public Map<String, Object> undoApproval(final LoanLifecycleStateMachine loanLifecycleStateMachine) {

        validateAccountStatus(LoanEvent.LOAN_APPROVAL_UNDO);
        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final LoanStatus currentStatus = LoanStatus.fromInt(this.loanStatus);
        final LoanStatus statusEnum = loanLifecycleStateMachine.dryTransition(LoanEvent.LOAN_APPROVAL_UNDO, this);
        if (!statusEnum.hasStateOf(currentStatus)) {
            loanLifecycleStateMachine.transition(LoanEvent.LOAN_APPROVAL_UNDO, this);
            actualChanges.put(PARAM_STATUS, LoanEnumerations.status(this.loanStatus));

            this.approvedOnDate = null;
            this.approvedBy = null;

            if (this.approvedPrincipal.compareTo(this.proposedPrincipal) != 0) {
                this.approvedPrincipal = this.proposedPrincipal;
                this.loanRepaymentScheduleDetail.setPrincipal(this.proposedPrincipal);

                actualChanges.put(LoanApiConstants.approvedLoanAmountParameterName, this.proposedPrincipal);
                actualChanges.put(LoanApiConstants.disbursementPrincipalParameterName, this.proposedPrincipal);

            }

            actualChanges.put(APPROVED_ON_DATE, "");

            this.loanOfficerHistory.clear();
        }

        return actualChanges;
    }

    public List<Long> findExistingTransactionIds() {
        final List<Long> ids = new ArrayList<>();
        List<LoanTransaction> transactions = getLoanTransactions();
        for (final LoanTransaction transaction : transactions) {
            ids.add(transaction.getId());
        }

        return ids;
    }

    public List<Long> findExistingReversedTransactionIds() {

        final List<Long> ids = new ArrayList<>();
        List<LoanTransaction> transactions = getLoanTransactions();
        for (final LoanTransaction transaction : transactions) {
            if (transaction.isReversed()) {
                ids.add(transaction.getId());
            }
        }

        return ids;
    }

    public ChangedTransactionDetail disburse(final AppUser currentUser, final JsonCommand command, final Map<String, Object> actualChanges,
            final ScheduleGeneratorDTO scheduleGeneratorDTO, final PaymentDetail paymentDetail) {
        final LocalDate actualDisbursementDateV2 = command.localDateValueOfParameterNamed(ACTUAL_DISBURSEMENT_DATE);
        this.disbursedBy = currentUser;
        updateLoanScheduleDependentDerivedFields();

        actualChanges.put(LOCALE, command.locale());
        actualChanges.put(DATE_FORMAT, command.dateFormat());
        actualChanges.put(ACTUAL_DISBURSEMENT_DATE, command.stringValueOfParameterNamed(ACTUAL_DISBURSEMENT_DATE));

        HolidayDetailDTO holidayDetailDTO = scheduleGeneratorDTO.getHolidayDetailDTO();

        // validate if disbursement date is a holiday or a non-working day
        validateDisbursementDateIsOnNonWorkingDay(holidayDetailDTO.getWorkingDays(), holidayDetailDTO.isAllowTransactionsOnNonWorkingDay());
        validateDisbursementDateIsOnHoliday(holidayDetailDTO.isAllowTransactionsOnHoliday(), holidayDetailDTO.getHolidays());

        regenerateRepaymentScheduleWithInterestRecalculationIfNeeded(this.repaymentScheduleDetail().isInterestRecalculationEnabled(),
                isDisbursementMissed(), scheduleGeneratorDTO);

        updateSummaryWithTotalFeeChargesDueAtDisbursement(deriveSumTotalOfChargesDueAtDisbursement());
        updateLoanRepaymentPeriodsDerivedFields(actualDisbursementDateV2);
        handleDisbursementTransaction(actualDisbursementDateV2, paymentDetail);
        updateLoanSummaryDerivedFields();

        /**
         * Add an interest applied transaction of the interest is accrued upfront (Up front accrual), no accounting or
         * cash based accounting is selected
         **/

        if (((isMultiDisburmentLoan() && getDisbursedLoanDisbursementDetails().size() == 1) || !isMultiDisburmentLoan())
                && Boolean.TRUE.equals(isNoneOrCashOrUpfrontAccrualAccountingEnabledOnLoanProduct()) && !this.isMigratedLoan) {
            // according to SU-314 , rather than post the whole accrual , apply just the daily accruals
            // from the disbursed date till cuurent date , apply the daily accruals
            LocalDate currentDate = DateUtils.getLocalDateOfTenant();
            for (LocalDate selectedDate = actualDisbursementDateV2; selectedDate
                    .isBefore(currentDate); selectedDate = selectedDate.plusDays(1)) {
                applyDailyAccrualsAtDisbursement(selectedDate);
            }
        }

        ChangedTransactionDetail result = reprocessTransactionForDisbursement();
        try {
            this.loanLifecycleStateMachine.transition(LoanEvent.LOAN_DISBURSED, this);
        } catch (Exception e) {
            log.error("loanLifecycleStateMachine is null");
        }
        actualChanges.put(PARAM_STATUS, LoanEnumerations.status(this.loanStatus));
        return result;

    }

    @SuppressWarnings({ "squid:S3776" })
    public void applyDailyAccrualsAtDisbursement(LocalDate currentDate) {
        ExternalId externalIdentifier = ExternalId.empty();
        if (TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
            externalIdentifier = ExternalId.generate();
        }
        Money dailyAccrualInterest = Money.zero(this.getCurrency());
        Integer accrualInstallmentNumber = null;
        final List<LoanRepaymentScheduleInstallment> loanRepaymentScheduleInstallments = getRepaymentScheduleInstallments();
        for (LoanRepaymentScheduleInstallment repaymentScheduleInstallment : loanRepaymentScheduleInstallments) {
            if (!currentDate.isBefore(repaymentScheduleInstallment.getFromDate())
                    && !currentDate.isAfter(repaymentScheduleInstallment.getDueDate().minusDays(1))) {
                long daysInPeriod = Math.toIntExact(
                        ChronoUnit.DAYS.between(repaymentScheduleInstallment.getFromDate(), repaymentScheduleInstallment.getDueDate()));
                Money interestForInstallment = repaymentScheduleInstallment.getInterestCharged(getCurrency());
                BigDecimal dailyInterest;
                // Adjust interest on the last day of the period to make up the difference
                if (currentDate.equals(repaymentScheduleInstallment.getDueDate().minusDays(1))) {
                    // if the amount is whole number when divided across the days in the period, then do same for last
                    // day
                    if (interestForInstallment.getAmount().remainder(BigDecimal.valueOf(daysInPeriod)).compareTo(BigDecimal.ZERO) == 0) {
                        dailyInterest = interestForInstallment.getAmount().divide(BigDecimal.valueOf(daysInPeriod), 2,
                                RoundingMode.HALF_UP);
                    } else {
                        BigDecimal totalAccruedInterest = repaymentScheduleInstallment.getAccruedInterest(getCurrency()).getAmount();
                        // This will ensure no rounding differences remain
                        dailyInterest = interestForInstallment.getAmount().subtract(totalAccruedInterest);
                    }

                } else {
                    dailyInterest = interestForInstallment.getAmount().divide(BigDecimal.valueOf(daysInPeriod), 2, RoundingMode.HALF_UP);
                }

                // Accumulate the daily interest to the installment's accrued interest
                BigDecimal roundedDownDailyInterest = dailyInterest.setScale(0, RoundingMode.DOWN);
                repaymentScheduleInstallment.addAccruedInterest(Money.of(getCurrency(), roundedDownDailyInterest));
                dailyAccrualInterest = Money.of(this.getCurrency(), roundedDownDailyInterest);
                accrualInstallmentNumber = repaymentScheduleInstallment.getInstallmentNumber();
                break;
            }
        }
        if (accrualInstallmentNumber != null && dailyAccrualInterest.isGreaterThanZero()) {
            LoanTransaction dailyAccrualTransaction = LoanTransaction.accrueDailyInterest(getOffice(), this, dailyAccrualInterest,
                    currentDate, externalIdentifier, accrualInstallmentNumber);
            addLoanTransaction(dailyAccrualTransaction);
            setInterestAccruedTill(currentDate);
        }

    }

    private void regenerateRepaymentScheduleWithInterestRecalculationIfNeeded(boolean interestRecalculationEnabledParam,
            boolean disbursementMissedParam, ScheduleGeneratorDTO scheduleGeneratorDTO) {

        LocalDate firstInstallmentDueDate = fetchRepaymentScheduleInstallment(1).getDueDate();
        if ((interestRecalculationEnabledParam && (DateUtils.isBeforeBusinessDate(firstInstallmentDueDate) || disbursementMissedParam))) {
            regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
        }
    }

    private List<LoanDisbursementDetails> getDisbursedLoanDisbursementDetails() {
        List<LoanDisbursementDetails> ret = new ArrayList<>();
        if (this.disbursementDetails != null && !this.disbursementDetails.isEmpty()) {
            for (LoanDisbursementDetails disbursementDetail : getDisbursementDetails()) {
                if (disbursementDetail.actualDisbursementDate() != null) {
                    ret.add(disbursementDetail);
                }
            }
        }
        return ret;
    }

    @SuppressWarnings({ "squid:S3776" })
    public void regenerateScheduleOnDisbursement(final ScheduleGeneratorDTO scheduleGeneratorDTO, final boolean recalculateSchedule,
            final LocalDate actualDisbursementDate, BigDecimal emiAmount, LocalDate nextPossibleRepaymentDate,
            LocalDate rescheduledRepaymentDate) {
        boolean isEmiAmountChanged = false;
        if ((this.loanProduct.isMultiDisburseLoan() || this.loanProduct.canDefineInstallmentAmount()) && emiAmount != null
                && emiAmount.compareTo(retriveLastEmiAmount()) != 0) {
            if (this.loanProduct.isMultiDisburseLoan()) {
                final LocalDate dateValue = null;
                final boolean isSpecificToInstallment = false;
                final Boolean isChangeEmiIfRepaymentDateSameAsDisbursementDateEnabled = scheduleGeneratorDTO
                        .isChangeEmiIfRepaymentDateSameAsDisbursementDateEnabled();
                LocalDate effectiveDateFrom = actualDisbursementDate;
                if (Boolean.TRUE.equals(!isChangeEmiIfRepaymentDateSameAsDisbursementDateEnabled)
                        && actualDisbursementDate.equals(nextPossibleRepaymentDate)) {
                    effectiveDateFrom = nextPossibleRepaymentDate.plusDays(1);
                }
                LoanTermVariations loanVariationTerms = new LoanTermVariations(LoanTermVariationType.EMI_AMOUNT.getValue(),
                        effectiveDateFrom, emiAmount, dateValue, isSpecificToInstallment, this, LoanStatus.ACTIVE.getValue());
                this.loanTermVariations.add(loanVariationTerms);
            } else {
                this.fixedEmiAmount = emiAmount;
            }
            isEmiAmountChanged = true;
        }
        if (rescheduledRepaymentDate != null && this.loanProduct.isMultiDisburseLoan()) {
            final boolean isSpecificToInstallment = false;
            LoanTermVariations loanVariationTerms = new LoanTermVariations(LoanTermVariationType.DUE_DATE.getValue(),
                    nextPossibleRepaymentDate, emiAmount, rescheduledRepaymentDate, isSpecificToInstallment, this,
                    LoanStatus.ACTIVE.getValue());
            this.loanTermVariations.add(loanVariationTerms);
        }

        if (isRepaymentScheduleRegenerationRequiredForDisbursement(actualDisbursementDate) || recalculateSchedule || isEmiAmountChanged
                || rescheduledRepaymentDate != null) {
            if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
                regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
            } else {
                if (this.isMultiDisburmentLoan()
                        && this.disbursementDetails.stream().filter(notrev -> Boolean.FALSE.equals(notrev.isReversed())).count() > 1) {
                    scheduleGeneratorDTO.setRecalculateFrom(actualDisbursementDate);
                    regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
                } else {
                    regenerateRepaymentSchedule(scheduleGeneratorDTO);
                }
            }
        }
    }

    public boolean canDisburse(final LocalDate actualDisbursementDate) {
        // For revolving credit products, we still need to validate basic loan state
        boolean isRevolvingCredit = this.getLoanProduct().getName().toLowerCase()
                .contains(LoanProductType.CREDITO_ROTATIVO.getCode().toLowerCase(Locale.ROOT))
                || this.getLoanProduct().getName().toLowerCase().contains(LoanProductType.NANO_CREDITO.getCode().toLowerCase(Locale.ROOT));

        LocalDate loanSubmittedOnDate = this.submittedOnDate;
        final LoanStatus statusEnum = this.loanLifecycleStateMachine.dryTransition(LoanEvent.LOAN_DISBURSED, this);

        boolean isMultiTrancheDisburse = false;
        LoanStatus actualLoanStatus = LoanStatus.fromInt(this.loanStatus);
        if ((actualLoanStatus.isActive() || actualLoanStatus.isClosedObligationsMet() || actualLoanStatus.isOverpaid())
                && isAllTranchesNotDisbursed()) {
            if (DateUtils.isBefore(actualDisbursementDate, loanSubmittedOnDate)) {
                final String errorMsg = "Loan can't be disbursed before " + loanSubmittedOnDate;
                throw new LoanDisbursalException(errorMsg, "actualdisbursementdate.before.submittedDate", loanSubmittedOnDate,
                        actualDisbursementDate);
            }
            isMultiTrancheDisburse = true;
        }

        // For revolving credit, we allow disbursement if the loan is in a valid state
        if (isRevolvingCredit) {
            return !statusEnum.hasStateOf(actualLoanStatus) || isMultiTrancheDisburse;
        }

        return !statusEnum.hasStateOf(actualLoanStatus) || isMultiTrancheDisburse;
    }

    @SuppressWarnings({ "squid:S3776" })
    public Money adjustDisburseAmount(@NotNull JsonCommand command, @NotNull LocalDate actualDisbursementDate) {
        Money disburseAmount = this.loanRepaymentScheduleDetail.getPrincipal().zero();
        BigDecimal principalDisbursed = command.bigDecimalValueOfParameterNamed(LoanApiConstants.principalDisbursedParameterName);
        if (this.actualDisbursementDate == null || DateUtils.isBefore(actualDisbursementDate, this.actualDisbursementDate)) {
            this.actualDisbursementDate = actualDisbursementDate;
        }
        BigDecimal diff = BigDecimal.ZERO;
        Collection<LoanDisbursementDetails> details = fetchUndisbursedDetail();
        if (principalDisbursed == null) {
            disburseAmount = this.loanRepaymentScheduleDetail.getPrincipal();
            if (!details.isEmpty()) {
                disburseAmount = disburseAmount.zero();
                for (LoanDisbursementDetails disbursementDetailsV2 : details) {
                    disbursementDetailsV2.updateActualDisbursementDate(actualDisbursementDate);
                    disburseAmount = disburseAmount.plus(disbursementDetailsV2.principal());
                }
            }
        } else {
            if (this.loanProduct.isMultiDisburseLoan()) {
                disburseAmount = Money.of(getCurrency(), principalDisbursed);
            } else {
                disburseAmount = disburseAmount.plus(principalDisbursed);
            }

            if (details.isEmpty()) {
                diff = this.loanRepaymentScheduleDetail.getPrincipal().minus(principalDisbursed).getAmount();
            } else {
                for (LoanDisbursementDetails disbursementDetailsV2 : details) {
                    disbursementDetailsV2.updateActualDisbursementDate(actualDisbursementDate);
                    disbursementDetailsV2.updatePrincipal(principalDisbursed);
                }
            }
            if (this.loanProduct().isMultiDisburseLoan()) {
                Collection<LoanDisbursementDetails> loanDisburseDetails = this.getDisbursementDetails();
                BigDecimal setPrincipalAmount = BigDecimal.ZERO;
                BigDecimal totalAmount = BigDecimal.ZERO;
                for (LoanDisbursementDetails disbursementDetailsV2 : loanDisburseDetails) {
                    if (disbursementDetailsV2.actualDisbursementDate() != null) {
                        setPrincipalAmount = setPrincipalAmount.add(disbursementDetailsV2.principal());
                    }
                    totalAmount = totalAmount.add(disbursementDetailsV2.principal());
                }
                this.loanRepaymentScheduleDetail.setPrincipal(setPrincipalAmount);
                compareDisbursedToApprovedOrProposedPrincipal(disburseAmount.getAmount(), totalAmount);
            } else {
                this.loanRepaymentScheduleDetail.setPrincipal(this.loanRepaymentScheduleDetail.getPrincipal().minus(diff).getAmount());
            }
            if (!this.loanProduct().isMultiDisburseLoan() && diff.compareTo(BigDecimal.ZERO) < 0) {
                final String errorMsg = "Loan can't be disbursed,disburse amount is exceeding approved amount ";
                throw new LoanDisbursalException(errorMsg, "disburse.amount.must.be.less.than.approved.amount", principalDisbursed,
                        this.loanRepaymentScheduleDetail.getPrincipal().getAmount());
            }
        }
        return disburseAmount;
    }

    private void compareDisbursedToApprovedOrProposedPrincipal(BigDecimal disbursedAmount, BigDecimal totalDisbursed) {

        if (this.loanProduct().isDisallowExpectedDisbursements() && this.loanProduct().isAllowApprovedDisbursedAmountsOverApplied()) {
            BigDecimal maxDisbursedAmount = getOverAppliedMax();
            if (totalDisbursed.compareTo(maxDisbursedAmount) > 0) {
                final String errorMessage = String.format(
                        "Loan disbursal amount can't be greater than maximum applied loan amount calculation. "
                                + "Total disbursed amount: %s  Maximum disbursal amount: %s",
                        totalDisbursed.stripTrailingZeros().toPlainString(), maxDisbursedAmount.stripTrailingZeros().toPlainString());
                throw new InvalidLoanStateTransitionException(Loan.DISBURSAL_PARAM,
                        "amount.can't.be.greater.than.maximum.applied.loan.amount.calculation", errorMessage, disbursedAmount,
                        maxDisbursedAmount);
            }
        } else {
            if (totalDisbursed.compareTo(this.approvedPrincipal) > 0 && !isRevolvingLoan()) {
                final String errorMsg = "Loan can't be disbursed,disburse amount is exceeding approved principal ";
                throw new LoanDisbursalException(errorMsg, "disburse.amount.must.be.less.than.approved.principal", totalDisbursed,
                        this.approvedPrincipal);
            }
        }
    }

    private ChangedTransactionDetail reprocessTransactionForDisbursement() {
        ChangedTransactionDetail changedTransactionDetail = null;
        if (this.loanProduct.isMultiDisburseLoan()) {
            final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retrieveListOfTransactionsPostDisbursement();
            if (!allNonContraTransactionsPostDisbursement.isEmpty()) {
                final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                        .determineProcessor(this.transactionProcessingStrategyCode);
                changedTransactionDetail = loanRepaymentScheduleTransactionProcessor.reprocessLoanTransactions(getDisbursementDate(),
                        allNonContraTransactionsPostDisbursement, getCurrency(), getRepaymentScheduleInstallments(), getActiveCharges());
                for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                    mapEntry.getValue().updateLoan(this);
                }
                this.loanTransactions.addAll(changedTransactionDetail.getNewTransactionMappings().values());
            }
            updateLoanSummaryDerivedFields();
        }

        return changedTransactionDetail;
    }

    private Collection<LoanDisbursementDetails> fetchUndisbursedDetail() {
        Collection<LoanDisbursementDetails> disbursementDetailsV2 = new ArrayList<>();
        LocalDate date = null;
        for (LoanDisbursementDetails disbursementDetail : getDisbursementDetails()) {
            if (disbursementDetail.actualDisbursementDate() == null) {
                LocalDate expectedDate = disbursementDetail.expectedDisbursementDate();
                if (date == null || DateUtils.isEqual(expectedDate, date)) {
                    disbursementDetailsV2.add(disbursementDetail);
                    date = expectedDate;
                } else if (DateUtils.isBefore(expectedDate, date)) {
                    disbursementDetailsV2.clear();
                    disbursementDetailsV2.add(disbursementDetail);
                    date = expectedDate;
                }
            }
        }
        return disbursementDetailsV2;
    }

    private LoanDisbursementDetails fetchLastDisburseDetail() {
        LoanDisbursementDetails details = null;
        LocalDate date = this.actualDisbursementDate;
        if (date != null) {
            for (LoanDisbursementDetails disbursementDetail : getDisbursementDetails()) {
                LocalDate actualDate = disbursementDetail.actualDisbursementDate();
                if (!DateUtils.isBefore(actualDate, date)) {
                    date = actualDate;
                    details = disbursementDetail;
                }
            }
        }
        return details;
    }

    private boolean isDisbursementMissed() {
        boolean isDisbursementMissed = false;
        for (LoanDisbursementDetails disbursementDetail : getDisbursementDetails()) {
            if (disbursementDetail.actualDisbursementDate() == null
                    && DateUtils.isBeforeBusinessDate(disbursementDetail.expectedDisbursementDateAsLocalDate())) {
                isDisbursementMissed = true;
                break;
            }
        }
        return isDisbursementMissed;
    }

    public BigDecimal getDisbursedAmount() {
        BigDecimal principal = BigDecimal.ZERO;
        for (LoanDisbursementDetails disbursementDetail : getDisbursementDetails()) {
            if (disbursementDetail.actualDisbursementDate() != null) {
                principal = principal.add(disbursementDetail.principal());
            }
        }
        return principal;
    }

    private void removeDisbursementDetail() {
        Set<LoanDisbursementDetails> details = new HashSet<>(getDisbursementDetails());
        for (LoanDisbursementDetails disbursementDetail : details) {
            if (disbursementDetail.actualDisbursementDate() == null) {
                this.disbursementDetails.remove(disbursementDetail);
            }
        }
    }

    private boolean isDisbursementAllowed() {
        boolean isAllowed = false;

        // If product is credito Rotativo or baboo credito, disburse as much as available. Checked before.
        if (this.getLoanProduct().getName().toLowerCase().contains(LoanProductType.CREDITO_ROTATIVO.getCode().toLowerCase())
                || this.getLoanProduct().getName().toLowerCase().contains(LoanProductType.NANO_CREDITO.getCode().toLowerCase())) {
            return true;
        }

        List<LoanDisbursementDetails> disbursementDetailsV2 = getDisbursementDetails();
        if (disbursementDetailsV2 == null || disbursementDetailsV2.isEmpty()) {
            isAllowed = true;
        } else {
            for (LoanDisbursementDetails disbursementDetail : disbursementDetailsV2) {
                if (disbursementDetail.actualDisbursementDate() == null) {
                    isAllowed = true;
                    break;
                }
            }
        }
        return isAllowed;
    }

    private boolean atleastOnceDisbursed() {
        boolean isDisbursed = false;
        for (LoanDisbursementDetails disbursementDetail : getDisbursementDetails()) {
            if (disbursementDetail.actualDisbursementDate() != null) {
                isDisbursed = true;
                break;
            }
        }
        return isDisbursed;
    }

    private void updateLoanRepaymentPeriodsDerivedFields(final LocalDate actualDisbursementDate) {
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment repaymentPeriod : installments) {
            repaymentPeriod.updateDerivedFields(loanCurrency(), actualDisbursementDate);
        }
    }

    /*
     * Ability to regenerate the repayment schedule based on the loans current details/state.
     */
    public void regenerateRepaymentSchedule(final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        final LoanScheduleModel loanSchedule = regenerateScheduleModel(scheduleGeneratorDTO);
        if (loanSchedule == null) {
            return;
        }
        updateLoanSchedule(loanSchedule);
        final Set<LoanCharge> activeCharges = this.getActiveCharges();
        for (final LoanCharge loanCharge : activeCharges) {
            if (!loanCharge.isWaived()) {
                recalculateLoanCharge(loanCharge);
            }
        }
        updateLoanDerivedFields();
    }

    public LoanScheduleModel regenerateScheduleModel(final ScheduleGeneratorDTO scheduleGeneratorDTO) {

        final MathContext mc = MoneyHelper.getMathContext();

        final LoanApplicationTerms loanApplicationTerms = constructLoanApplicationTerms(scheduleGeneratorDTO);
        loanApplicationTerms.setExtendTermForMonthlyRepayment(this.loanProduct.getExtendTermForMonthlyRepayments());
        LoanScheduleGenerator loanScheduleGenerator;
        if (loanApplicationTerms.isEqualAmortization()) {
            if (loanApplicationTerms.getInterestMethod().isDecliningBalance()) {
                final LoanScheduleGenerator decliningLoanScheduleGenerator = scheduleGeneratorDTO.getLoanScheduleFactory()
                        .create(loanApplicationTerms.getLoanScheduleType(), InterestMethod.DECLINING_BALANCE);
                Set<LoanCharge> loanCharges = getActiveCharges();
                LoanScheduleModel loanSchedule = decliningLoanScheduleGenerator.generate(mc, loanApplicationTerms, loanCharges,
                        scheduleGeneratorDTO.getHolidayDetailDTO());

                loanApplicationTerms
                        .updateTotalInterestDue(Money.of(loanApplicationTerms.getCurrency(), loanSchedule.getTotalInterestCharged()));

            }
            loanScheduleGenerator = scheduleGeneratorDTO.getLoanScheduleFactory().create(loanApplicationTerms.getLoanScheduleType(),
                    InterestMethod.FLAT);
        } else {
            loanScheduleGenerator = scheduleGeneratorDTO.getLoanScheduleFactory().create(loanApplicationTerms.getLoanScheduleType(),
                    loanApplicationTerms.getInterestMethod());
        }

        if (this.getRepaymentScheduleInstallments().size() > 0) {
            loanApplicationTerms.setExistentInstallments(this.getRepaymentScheduleInstallments());
        }

        loanApplicationTerms.setLoanProductName(this.getLoanProduct().getName());

        return loanScheduleGenerator.generate(mc, loanApplicationTerms, getActiveCharges(), scheduleGeneratorDTO.getHolidayDetailDTO());
    }

    private BigDecimal constructFloatingInterestRates(final BigDecimal annualNominalInterestRate, final FloatingRateDTO floatingRateDTO,
            final List<LoanTermVariationsData> loanTermVariations) {
        final LocalDate dateValue = null;
        final boolean isSpecificToInstallment = false;
        BigDecimal interestRate = annualNominalInterestRate;
        if (loanProduct.isLinkedToFloatingInterestRate()) {
            floatingRateDTO.resetInterestRateDiff();
            Collection<FloatingRatePeriodData> applicableRates = loanProduct.fetchInterestRates(floatingRateDTO);
            LocalDate interestRateStartDate = DateUtils.getBusinessLocalDate();
            for (FloatingRatePeriodData periodData : applicableRates) {
                LoanTermVariationsData loanTermVariation = new LoanTermVariationsData(
                        LoanEnumerations.loanVariationType(LoanTermVariationType.INTEREST_RATE), periodData.getFromDateAsLocalDate(),
                        periodData.getInterestRate(), dateValue, isSpecificToInstallment);
                if (!DateUtils.isBefore(interestRateStartDate, periodData.getFromDateAsLocalDate())) {
                    interestRateStartDate = periodData.getFromDateAsLocalDate();
                    interestRate = periodData.getInterestRate();
                }
                loanTermVariations.add(loanTermVariation);
            }
        }
        return interestRate;
    }

    @SuppressWarnings({ "squid:S3776" })
    private void handleDisbursementTransaction(final LocalDate disbursedOn, final PaymentDetail paymentDetail) {
        final Money totalFeeChargesDueAtDisbursement = this.summary.getTotalFeeChargesDueAtDisbursement(loanCurrency());
        Money disbursentMoney = Money.zero(getCurrency());
        final LoanTransaction chargesPayment = LoanTransaction.repaymentAtDisbursement(getOffice(), disbursentMoney, paymentDetail,
                disbursedOn, null);
        final Integer installmentNumber = null;
        List<LoanCharge> installmentalCharges = new ArrayList<>();
        for (final LoanCharge charge : getActiveCharges()) {
            LocalDate actualLoanDisbursementDate = getActualDisbursementDate(charge);
            if (charge.getCharge().getChargeTimeType().equals(ChargeTimeType.DISBURSEMENT.getValue())
                    && disbursedOn.equals(actualLoanDisbursementDate) && !charge.isWaived() && !charge.isFullyPaid()
                    || charge.getCharge().getChargeTimeType().equals(ChargeTimeType.TRANCHE_DISBURSEMENT.getValue())
                            && disbursedOn.equals(actualLoanDisbursementDate) && actualLoanDisbursementDate != null && !charge.isWaived()
                            && !charge.isFullyPaid()) {
                if (totalFeeChargesDueAtDisbursement.isGreaterThanZero() && !charge.getChargePaymentMode().isPaymentModeAccountTransfer()) {
                    charge.markAsFullyPaid();
                    // Add "Loan Charge Paid By" details to this transaction
                    final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(chargesPayment, charge, charge.amount(),
                            installmentNumber);
                    chargesPayment.getLoanChargesPaid().add(loanChargePaidBy);
                    disbursentMoney = disbursentMoney.plus(charge.amount());
                }
            } else if (disbursedOn.equals(this.actualDisbursementDate)
                    && Boolean.TRUE.equals(isNoneOrCashOrUpfrontAccrualAccountingEnabledOnLoanProduct())) {

                installmentalCharges.add(charge);

            }

        }
        // SU-444 generate charges from disbursement day
        if (!installmentalCharges.isEmpty()) {
            handleChargeAppliedTransactionPerInstallment(installmentalCharges, disbursedOn);
        }

        if (disbursentMoney.isGreaterThanZero()) {
            final Money zero = Money.zero(getCurrency());
            chargesPayment.updateComponentsAndTotal(zero, zero, disbursentMoney, zero);
            chargesPayment.updateLoan(this);
            addLoanTransaction(chargesPayment);
            updateLoanOutstandingBalances();
        }

        if (getApprovedOnDate() != null && DateUtils.isBefore(disbursedOn, getApprovedOnDate())) {
            final String errorMessage = "The date on which a loan is disbursed cannot be before its approval date: "
                    + getApprovedOnDate().toString();
            throw new InvalidLoanStateTransitionException(Loan.DISBURSAL_PARAM, "cannot.be.before.approval.date", errorMessage, disbursedOn,
                    getApprovedOnDate());
        }

        LocalDate expectedDate = getExpectedFirstRepaymentOnDate();
        if (expectedDate != null && (DateUtils.isAfter(disbursedOn, this.fetchRepaymentScheduleInstallment(1).getDueDate())
                || DateUtils.isAfter(disbursedOn, expectedDate)) && DateUtils.isEqual(disbursedOn, this.actualDisbursementDate)) {
            final String errorMessage = "submittedOnDate cannot be after the loans  expectedFirstRepaymentOnDate: "
                    + expectedDate.toString();
            throw new InvalidLoanStateTransitionException(Loan.DISBURSAL_PARAM, "cannot.be.after.expected.first.repayment.date",
                    errorMessage, disbursedOn, expectedDate);
        }

        validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.LOAN_DISBURSED, disbursedOn);

        if (DateUtils.isDateInTheFuture(disbursedOn)) {
            final String errorMessage = "The date on which a loan with identifier : " + this.accountNumber
                    + " is disbursed cannot be in the future.";
            throw new InvalidLoanStateTransitionException(Loan.DISBURSAL_PARAM, Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage,
                    disbursedOn);
        }

    }

    public LoanTransaction handleDownPayment(final BigDecimal disbursedAmount, final JsonCommand command,
            final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        LocalDate disbursedOn = command.localDateValueOfParameterNamed(ACTUAL_DISBURSEMENT_DATE);
        BigDecimal disbursedAmountPercentageForDownPayment = this.loanRepaymentScheduleDetail.getDisbursedAmountPercentageForDownPayment();
        ExternalId externalIdV2 = ExternalId.empty();
        if (TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
            externalIdV2 = ExternalId.generate();
        }
        Money downPaymentMoney = Money.of(getCurrency(),
                MathUtil.percentageOf(disbursedAmount, disbursedAmountPercentageForDownPayment, 19));
        LoanTransaction downPaymentTransaction = LoanTransaction.downPayment(getOffice(), downPaymentMoney, null, disbursedOn,
                externalIdV2);

        LoanEvent event = LoanEvent.LOAN_REPAYMENT_OR_WAIVER;
        validateRepaymentTypeAccountStatus(downPaymentTransaction, event);
        HolidayDetailDTO holidayDetailDTO = scheduleGeneratorDTO.getHolidayDetailDTO();
        validateRepaymentDateIsOnHoliday(downPaymentTransaction.getTransactionDate(), holidayDetailDTO.isAllowTransactionsOnHoliday(),
                holidayDetailDTO.getHolidays());
        validateRepaymentDateIsOnNonWorkingDay(downPaymentTransaction.getTransactionDate(), holidayDetailDTO.getWorkingDays(),
                holidayDetailDTO.isAllowTransactionsOnNonWorkingDay());

        handleRepaymentOrRecoveryOrWaiverTransaction(downPaymentTransaction, loanLifecycleStateMachine, null, scheduleGeneratorDTO);
        return downPaymentTransaction;
    }

    public boolean isAutoRepaymentForDownPaymentEnabled() {
        return this.loanRepaymentScheduleDetail.isEnableDownPayment()
                && this.loanRepaymentScheduleDetail.isEnableAutoRepaymentForDownPayment();
    }

    public LoanTransaction handlePayDisbursementTransaction(final Long chargeId, final LoanTransaction chargesPayment,
            final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds) {
        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());
        LoanCharge charge = null;
        for (final LoanCharge loanCharge : this.charges) {
            if (loanCharge.isActive() && chargeId.equals(loanCharge.getId())) {
                charge = loanCharge;
            }
        }
        @SuppressWarnings("null")
        final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(chargesPayment, charge, charge.amount(), null);
        chargesPayment.getLoanChargesPaid().add(loanChargePaidBy);
        final Money zero = Money.zero(getCurrency());
        chargesPayment.updateComponents(zero, zero, charge.getAmount(getCurrency()), zero);
        chargesPayment.updateLoan(this);
        addLoanTransaction(chargesPayment);
        updateLoanOutstandingBalances();
        charge.markAsFullyPaid();
        return chargesPayment;
    }

    public void removePostDatedChecks() {
        this.postDatedChecks = new ArrayList<>();
    }

    @SuppressWarnings({ "squid:S3776" })
    public Map<String, Object> undoDisbursal(final ScheduleGeneratorDTO scheduleGeneratorDTO, final List<Long> existingTransactionIds,
            final List<Long> existingReversedTransactionIds) {

        validateAccountStatus(LoanEvent.LOAN_DISBURSAL_UNDO);

        final Map<String, Object> actualChanges = new LinkedHashMap<>();
        final LoanStatus currentStatus = LoanStatus.fromInt(this.loanStatus);
        final LoanStatus statusEnum = this.loanLifecycleStateMachine.dryTransition(LoanEvent.LOAN_DISBURSAL_UNDO, this);
        validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.LOAN_DISBURSAL_UNDO, getDisbursementDate());
        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());
        if (!statusEnum.hasStateOf(currentStatus)) {
            this.loanLifecycleStateMachine.transition(LoanEvent.LOAN_DISBURSAL_UNDO, this);
            actualChanges.put(PARAM_STATUS, LoanEnumerations.status(this.loanStatus));

            final LocalDate actualDisbursementDateV2 = getDisbursementDate();
            final boolean isScheduleRegenerateRequired = isRepaymentScheduleRegenerationRequiredForDisbursement(actualDisbursementDateV2);
            this.actualDisbursementDate = null;
            this.disbursedBy = null;
            boolean isDisbursedAmountChanged = !MathUtil.isEqualTo(approvedPrincipal,
                    this.loanRepaymentScheduleDetail.getPrincipal().getAmount());
            this.loanRepaymentScheduleDetail.setPrincipal(this.approvedPrincipal);

            boolean updateLoanToPreDisbursalState = false;

            boolean isEmiAmountChanged = !this.loanTermVariations.isEmpty();

            // Remove All the Disbursement Details If the Loan Product is disabled and exists one
            if (this.loanProduct().isDisallowExpectedDisbursements() && !getDisbursementDetails().isEmpty()) {
                for (LoanDisbursementDetails disbursementDetail : getAllDisbursementDetails()) {
                    disbursementDetail.reverse();
                }
            } else {
                if (this.containsRevolvingLoan()) {
                    // Pick last active disbursal
                    Optional<LoanDisbursementDetails> lastActiveDDOpt = this.disbursementDetails.stream()
                            .filter(isNotRev -> Boolean.FALSE.equals(isNotRev.isReversed()))
                            .filter(disb -> Objects.nonNull(disb.actualDisbursementDate()))
                            .sorted(Comparator.comparing(LoanDisbursementDetails::expectedDisbursementDateAsLocalDate).reversed())
                            .findFirst();

                    // Check how many active disbursals
                    Long activeDisbursalsCounter = this.disbursementDetails.stream()
                            .filter(isNotRev -> Boolean.FALSE.equals(isNotRev.isReversed()))
                            .filter(disb -> Objects.nonNull(disb.actualDisbursementDate())).count();

                    if (lastActiveDDOpt.isPresent()) {
                        LoanDisbursementDetails details = lastActiveDDOpt.get();

                        // If there is only 1 active disbursal remaining, restore principal to approved amount
                        if (activeDisbursalsCounter.compareTo(1L) == 0) {
                            details.updateActualDisbursementDate(null);
                            details.updatePrincipal(this.getApprovedPrincipal());

                            updateLoanToPreDisbursalState = true;
                        } else {
                            throw new GeneralPlatformDomainRuleException("validation.msg.undo.disbursal.transaction.first",
                                    "Undo disbursal is only allowed when there is only 1 active disbursal. Use Undo from Transactions Tab instead.");
                        }

                        isDisbursedAmountChanged = true;
                    }
                } else {
                    updateLoanToPreDisbursalState = true;
                }
            }

            if (updateLoanToPreDisbursalState) {
                updateLoanToPreDisbursalState();
            }

            if (isScheduleRegenerateRequired || isDisbursedAmountChanged || isEmiAmountChanged
                    || this.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
                // clear off actual disbusrement date so schedule regeneration
                // uses expected date.

                regenerateRepaymentSchedule(scheduleGeneratorDTO);
                if (isDisbursedAmountChanged) {
                    updateSummaryWithTotalFeeChargesDueAtDisbursement(deriveSumTotalOfChargesDueAtDisbursement());
                }
            } else if (Boolean.TRUE.equals(isPeriodicAccrualAccountingEnabledOnLoanProduct())) {
                for (final LoanRepaymentScheduleInstallment period : getRepaymentScheduleInstallments()) {
                    period.resetAccrualComponents();
                }
            }

            if (this.isTopup) {
                this.loanTopupDetails.setAccountTransferDetails(null);
                this.loanTopupDetails.setTopupAmount(null);
            }

            this.adjustNetDisbursalAmount(this.approvedPrincipal);

            actualChanges.put(ACTUAL_DISBURSEMENT_DATE, "");

            updateLoanSummaryDerivedFields();

        }

        return actualChanges;
    }

    private void reverseExistingTransactions() {
        Collection<LoanTransaction> retainTransactions = new ArrayList<>();
        for (final LoanTransaction transaction : this.loanTransactions) {
            transaction.reverse();
            if (transaction.getId() != null) {
                retainTransactions.add(transaction);
            }
        }
        this.loanTransactions.retainAll(retainTransactions);
    }

    private void updateLoanToPreDisbursalState() {
        this.actualDisbursementDate = null;

        this.accruedTill = null;
        reverseExistingTransactions();

        for (final LoanCharge charge : getActiveCharges()) {
            if (charge.isOverdueInstallmentCharge()) {
                charge.setActive(false);
            } else {
                charge.resetToOriginal(loanCurrency());
            }
        }
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
            currentInstallment.resetDerivedComponents();
        }
        for (LoanTermVariations variations : this.loanTermVariations) {
            if (variations.getOnLoanStatus().equals(LoanStatus.ACTIVE.getValue())) {
                variations.markAsInactive();
            }
        }
        final LoanRepaymentScheduleProcessingWrapper wrapper = new LoanRepaymentScheduleProcessingWrapper();
        wrapper.reprocess(getCurrency(), getDisbursementDate(), getRepaymentScheduleInstallments(), getActiveCharges());

        updateLoanSummaryDerivedFields();
    }

    public ChangedTransactionDetail waiveInterest(final LoanTransaction waiveInterestTransaction,
            final LoanLifecycleStateMachine loanLifecycleStateMachine, final List<Long> existingTransactionIds,
            final List<Long> existingReversedTransactionIds, final ScheduleGeneratorDTO scheduleGeneratorDTO) {

        validateAccountStatus(LoanEvent.LOAN_REPAYMENT_OR_WAIVER);

        validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.LOAN_REPAYMENT_OR_WAIVER,
                waiveInterestTransaction.getTransactionDate());
        validateActivityNotBeforeLastTransactionDate(LoanEvent.LOAN_REPAYMENT_OR_WAIVER, waiveInterestTransaction.getTransactionDate());

        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());

        return handleRepaymentOrRecoveryOrWaiverTransaction(waiveInterestTransaction, loanLifecycleStateMachine, null,
                scheduleGeneratorDTO);
    }

    @SuppressWarnings("null")
    public ChangedTransactionDetail makeRepayment(final LoanTransaction repaymentTransaction,
            final LoanLifecycleStateMachine loanLifecycleStateMachine, final List<Long> existingTransactionIds,
            final List<Long> existingReversedTransactionIds, boolean isRecoveryRepayment, final ScheduleGeneratorDTO scheduleGeneratorDTO,
            Boolean isHolidayValidationDone) {
        HolidayDetailDTO holidayDetailDTO = null;
        LoanEvent event = null;
        if (isRecoveryRepayment) {
            event = LoanEvent.LOAN_RECOVERY_PAYMENT;
        } else {
            event = LoanEvent.LOAN_REPAYMENT_OR_WAIVER;
        }
        if (Boolean.FALSE.equals(isHolidayValidationDone)) {
            holidayDetailDTO = scheduleGeneratorDTO.getHolidayDetailDTO();
        }
        validateRepaymentTypeAccountStatus(repaymentTransaction, event);
        validateActivityNotBeforeClientOrGroupTransferDate(event, repaymentTransaction.getTransactionDate());
        validateRepaymentTypeTransactionNotBeforeAChargeRefund(repaymentTransaction, "created");
        validateActivityNotBeforeLastTransactionDate(event, repaymentTransaction.getTransactionDate());
        if (Boolean.FALSE.equals(isHolidayValidationDone)) {
            validateRepaymentDateIsOnHoliday(repaymentTransaction.getTransactionDate(), holidayDetailDTO.isAllowTransactionsOnHoliday(),
                    holidayDetailDTO.getHolidays());
            validateRepaymentDateIsOnNonWorkingDay(repaymentTransaction.getTransactionDate(), holidayDetailDTO.getWorkingDays(),
                    holidayDetailDTO.isAllowTransactionsOnNonWorkingDay());
        }
        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());

        return handleRepaymentOrRecoveryOrWaiverTransaction(repaymentTransaction, loanLifecycleStateMachine, null, scheduleGeneratorDTO);
    }

    private void validateRepaymentTypeAccountStatus(LoanTransaction repaymentTransaction, LoanEvent event) {
        if (repaymentTransaction.isGoodwillCredit() || repaymentTransaction.isMerchantIssuedRefund()
                || repaymentTransaction.isPayoutRefund() || repaymentTransaction.isChargeRefund() || repaymentTransaction.isRepayment()
                || repaymentTransaction.isDownPayment()) {

            if (!(isOpen() || isClosedObligationsMet() || isOverPaid())) {
                final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
                final String defaultUserMessage = "Loan must be Active, Fully Paid or Overpaid";
                final ApiParameterError error = ApiParameterError.generalError("error.msg.loan.must.be.active.fully.paid.or.overpaid",
                        defaultUserMessage);
                dataValidationErrors.add(error);
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        } else {
            validateAccountStatus(event);
        }

    }

    public void makeChargePayment(final Long chargeId, final LoanLifecycleStateMachine loanLifecycleStateMachine,
            final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds,
            final HolidayDetailDTO holidayDetailDTO, final LoanTransaction paymentTransaction, final Integer installmentNumber) {

        validateAccountStatus(LoanEvent.LOAN_CHARGE_PAYMENT);
        validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.LOAN_CHARGE_PAYMENT, paymentTransaction.getTransactionDate());
        validateActivityNotBeforeLastTransactionDate(LoanEvent.LOAN_CHARGE_PAYMENT, paymentTransaction.getTransactionDate());
        validateRepaymentDateIsOnHoliday(paymentTransaction.getTransactionDate(), holidayDetailDTO.isAllowTransactionsOnHoliday(),
                holidayDetailDTO.getHolidays());
        validateRepaymentDateIsOnNonWorkingDay(paymentTransaction.getTransactionDate(), holidayDetailDTO.getWorkingDays(),
                holidayDetailDTO.isAllowTransactionsOnNonWorkingDay());

        if (DateUtils.isDateInTheFuture(paymentTransaction.getTransactionDate())) {
            final String errorMessage = "The date on which a loan charge paid cannot be in the future.";
            throw new InvalidLoanStateTransitionException("charge.payments", Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage,
                    paymentTransaction.getTransactionDate());
        }
        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());
        LoanCharge charge = null;
        for (final LoanCharge loanCharge : this.charges) {
            if (loanCharge.isActive() && chargeId.equals(loanCharge.getId())) {
                charge = loanCharge;
            }
        }
        handleChargePaidTransaction(charge, paymentTransaction, loanLifecycleStateMachine, installmentNumber);
    }

    public void makeRefund(final LoanTransaction loanTransaction, final LoanLifecycleStateMachine loanLifecycleStateMachine,
            final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds,
            final boolean allowTransactionsOnHoliday, final List<Holiday> holidays, final WorkingDays workingDays,
            final boolean allowTransactionsOnNonWorkingDay) {

        validateRepaymentDateIsOnHoliday(loanTransaction.getTransactionDate(), allowTransactionsOnHoliday, holidays);
        validateRepaymentDateIsOnNonWorkingDay(loanTransaction.getTransactionDate(), workingDays, allowTransactionsOnNonWorkingDay);

        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());

        if (getStatus().isOverpaid()) {
            if (this.totalOverpaid.compareTo(loanTransaction.getAmount(getCurrency()).getAmount()) < 0) {
                final String errorMessage = "The refund amount must be less than or equal to overpaid amount ";
                throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, "is.exceeding.overpaid.amount", errorMessage,
                        this.totalOverpaid, loanTransaction.getAmount(getCurrency()).getAmount());
            } else if (!isAfterLatRepayment(loanTransaction, getLoanTransactions())) {
                final String errorMessage = "Transfer funds is allowed only after last repayment date";
                throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, "is.not.after.repayment.date", errorMessage);
            }
        } else {
            final String errorMessage = "Transfer funds is allowed only for loan accounts with overpaid status ";
            throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, "is.not.a.overpaid.loan", errorMessage);
        }

        loanTransaction.updateLoan(this);

        if (loanTransaction.isNotZero(loanCurrency())) {
            addLoanTransaction(loanTransaction);
        }
        updateLoanSummaryDerivedFields();
        doPostLoanTransactionChecks(loanTransaction.getTransactionDate(), loanLifecycleStateMachine);
    }

    @SuppressWarnings({ "squid:S3776" })
    private ChangedTransactionDetail handleRepaymentOrRecoveryOrWaiverTransaction(final LoanTransaction loanTransaction,
            final LoanLifecycleStateMachine loanLifecycleStateMachine, final LoanTransaction adjustedTransaction,
            final ScheduleGeneratorDTO scheduleGeneratorDTO) {

        ChangedTransactionDetail changedTransactionDetail = null;

        if (loanTransaction.isRecoveryRepayment()) {
            loanLifecycleStateMachine.transition(LoanEvent.LOAN_RECOVERY_PAYMENT, this);
        }

        if (loanTransaction.isRecoveryRepayment()
                && loanTransaction.getAmount(loanCurrency()).getAmount().compareTo(getLoanSummary().getTotalWrittenOff()) > 0) {
            final String errorMessage = "The transaction amount cannot greater than the remaining written off amount.";
            throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, "cannot.be.greater.than.total.written.off", errorMessage);
        }

        loanTransaction.updateLoan(this);

        boolean isTransactionChronologicallyLatest = true;
        if (isProgressiveLoan()) {
            isTransactionChronologicallyLatest = isChronologicallyLatestRepaymentOrWaiverForProgressiveLoans(loanTransaction,
                    getLoanTransactions());
        } else {
            isTransactionChronologicallyLatest = isChronologicallyLatestRepaymentOrWaiver(loanTransaction, getLoanTransactions());
        }

        if (loanTransaction.isNotZero(loanCurrency())) {
            addLoanTransaction(loanTransaction);
        }

        if (loanTransaction.isNotRepaymentLikeType() && loanTransaction.isNotWaiver() && loanTransaction.isNotRecoveryRepayment()) {
            final String errorMessage = "A transaction of type repayment or recovery repayment or waiver was expected but not received.";
            throw new InvalidLoanTransactionTypeException(Loan.TRANSACTION_PARAM, "is.not.a.repayment.or.waiver.or.recovery.transaction",
                    errorMessage);
        }

        final LocalDate loanTransactionDate = loanTransaction.getTransactionDate();
        if (DateUtils.isBefore(loanTransactionDate, getDisbursementDate())) {
            final String errorMessage = "The transaction date cannot be before the loan disbursement date: "
                    + getDisbursementDate().toString();
            throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, "cannot.be.before.disbursement.date", errorMessage,
                    loanTransactionDate, getDisbursementDate());
        }

        if (DateUtils.isDateInTheFuture(loanTransactionDate)) {
            final String errorMessage = "The transaction date cannot be in the future.";
            throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage,
                    loanTransactionDate);
        }

        if (!isTransactionBeforeLastRepaymentTransaction(loanTransaction, getLoanTransactions())) {
            final String errorMessage = "The transaction date cannot be before last valid transaction: " + getDisbursementDate().toString();
            throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, "cannot.be.before.last.valid.transaction", errorMessage,
                    loanTransactionDate, getDisbursementDate());
        }

        if (loanTransaction.isInterestWaiver()) {
            Money totalInterestOutstandingOnLoan = getTotalInterestOutstandingOnLoan();
            if (adjustedTransaction != null) {
                totalInterestOutstandingOnLoan = totalInterestOutstandingOnLoan.plus(adjustedTransaction.getAmount(loanCurrency()));
            }
            if (loanTransaction.getAmount(loanCurrency()).isGreaterThan(totalInterestOutstandingOnLoan)) {
                final String errorMessage = "The amount of interest to waive cannot be greater than total interest outstanding on loan.";
                throw new InvalidLoanStateTransitionException("waive.interest", "amount.exceeds.total.outstanding.interest", errorMessage,
                        loanTransaction.getAmount(loanCurrency()), totalInterestOutstandingOnLoan.getAmount());
            }
        }

        if (this.loanProduct.isMultiDisburseLoan() && adjustedTransaction == null) {
            BigDecimal totalDisbursed = getDisbursedAmount();
            BigDecimal totalPrincipalAdjusted = this.summary.getTotalPrincipalAdjustments();
            BigDecimal totalPrincipalCredited = totalDisbursed.add(totalPrincipalAdjusted);
            if (totalPrincipalCredited.compareTo(this.summary.getTotalPrincipalRepaid()) < 0) {
                final String errorMessage = "The transaction amount cannot exceed threshold ";
                throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, Loan.ERROR_MESSAGE_AMOUNT_EXCEED_THRESHOLD,
                        errorMessage);
            }
        }

        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);

        final LoanRepaymentScheduleInstallment currentInstallment = fetchLoanRepaymentScheduleInstallment(
                loanTransaction.getTransactionDate());
        boolean reprocess = true;

        if (!isClaim() && !isForeclosure() && isTransactionChronologicallyLatest && adjustedTransaction == null
                && DateUtils.isEqualBusinessDate(loanTransaction.getTransactionDate()) && currentInstallment != null
                && currentInstallment.getTotalOutstanding(getCurrency()).isEqualTo(loanTransaction.getAmount(getCurrency()))) {
            reprocess = false;
        }

        if (isTransactionChronologicallyLatest && adjustedTransaction == null
                && (!reprocess || !this.repaymentScheduleDetail().isInterestRecalculationEnabled()) && (!isForeclosure() && !isClaim())) {
            loanRepaymentScheduleTransactionProcessor.processLatestTransaction(loanTransaction, new TransactionCtx(getCurrency(),
                    getRepaymentScheduleInstallments(), getActiveCharges(), new MoneyHolder(getTotalOverpaidAsMoney())));
            reprocess = false;
            if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
                if (currentInstallment == null || currentInstallment.isNotFullyPaidOff()) {
                    reprocess = true;
                } else {
                    final LoanRepaymentScheduleInstallment nextInstallment = fetchRepaymentScheduleInstallment(
                            currentInstallment.getInstallmentNumber() + 1);
                    if (nextInstallment != null && nextInstallment.getTotalPaidInAdvance(getCurrency()).isGreaterThanZero()) {
                        reprocess = true;
                    }
                }
            }

            if (isProgressiveLoan()) {
                reprocess = true;
            }
        }
        if (reprocess) {
            if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()
                    || (isProgressiveLoan() && !isForeclosure() && !isClaim() && adjustedTransaction == null)) {
                regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
            }

            final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retrieveListOfTransactionsPostDisbursement();
            changedTransactionDetail = loanRepaymentScheduleTransactionProcessor.reprocessLoanTransactions(getDisbursementDate(),
                    allNonContraTransactionsPostDisbursement, getCurrency(), getRepaymentScheduleInstallments(), getActiveCharges());
            if (isProgressiveLoan() && !isForeclosure() && !isClaim() && !loanTransaction.isReversed()
                    && loanTransaction.getAmount().equals(BigDecimal.ZERO) && adjustedTransaction != null
                    && adjustedTransaction.isReversed()) {
                regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
            }

            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                mapEntry.getValue().updateLoan(this);
            }
            /***
             * Commented since throwing exception if external id present for one of the transactions. for this need to
             * save the reversed transactions first and then new transactions.
             */
            this.loanTransactions.addAll(changedTransactionDetail.getNewTransactionMappings().values());
        }

        updateLoanSummaryDerivedFields();
        if (loanTransaction.isNotRecoveryRepayment()) {
            doPostLoanTransactionChecks(loanTransaction.getTransactionDate(), loanLifecycleStateMachine);
        }

        if (this.loanProduct.isMultiDisburseLoan()) {
            BigDecimal totalDisbursed = getDisbursedAmount();
            BigDecimal totalPrincipalAdjusted = this.summary.getTotalPrincipalAdjustments();
            BigDecimal totalPrincipalCredited = totalDisbursed.add(totalPrincipalAdjusted);
            if (totalPrincipalCredited.compareTo(this.summary.getTotalPrincipalRepaid()) < 0
                    && this.repaymentScheduleDetail().getPrincipal().minus(totalDisbursed).isGreaterThanZero()) {
                final String errorMessage = "The transaction amount cannot exceed threshold.";
                throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, Loan.ERROR_MESSAGE_AMOUNT_EXCEED_THRESHOLD,
                        errorMessage);
            }
        }

        return changedTransactionDetail;
    }

    private LoanRepaymentScheduleInstallment fetchLoanRepaymentScheduleInstallment(LocalDate dueDate) {
        LoanRepaymentScheduleInstallment installment = null;
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : installments) {
            if (dueDate.equals(loanRepaymentScheduleInstallment.getDueDate())) {
                installment = loanRepaymentScheduleInstallment;
                break;
            }
        }
        return installment;
    }

    private List<LoanTransaction> retrieveListOfIncomePostingTransactions() {
        final List<LoanTransaction> incomePostTransactions = new ArrayList<>();
        List<LoanTransaction> trans = getLoanTransactions();
        for (final LoanTransaction transaction : trans) {
            if (transaction.isNotReversed() && transaction.isIncomePosting()) {
                incomePostTransactions.add(transaction);
            }
        }
        incomePostTransactions.sort(LoanTransactionComparator.INSTANCE);
        return incomePostTransactions;
    }

    private List<LoanTransaction> retrieveListOfTransactionsPostDisbursement() {
        final List<LoanTransaction> repaymentsOrWaivers = new ArrayList<>();
        List<LoanTransaction> trans = getLoanTransactions();
        for (final LoanTransaction transaction : trans) {
            if (transaction.isNotReversed() && (transaction.isChargeOff() || !transaction.isNonMonetaryTransaction())) {
                repaymentsOrWaivers.add(transaction);
            }
        }
        repaymentsOrWaivers.sort(LoanTransactionComparator.INSTANCE);
        return repaymentsOrWaivers;
    }

    public List<LoanTransaction> retrieveListOfTransactionsPostDisbursementExcludeAccruals() {
        final List<LoanTransaction> repaymentsOrWaivers = new ArrayList<>();
        for (final LoanTransaction transaction : this.loanTransactions) {
            if (transaction.isNotReversed() && !(transaction.isAccrual() || transaction.isRepaymentAtDisbursement()
                    || transaction.isNonMonetaryTransaction() || transaction.isIncomePosting())) {
                repaymentsOrWaivers.add(transaction);
            }
        }
        repaymentsOrWaivers.sort(LoanTransactionComparator.INSTANCE);
        return repaymentsOrWaivers;
    }

    private List<LoanTransaction> retrieveListOfTransactionsExcludeAccruals() {
        final List<LoanTransaction> repaymentsOrWaivers = new ArrayList<>();
        for (final LoanTransaction transaction : this.loanTransactions) {
            if (transaction.isNotReversed() && !(transaction.isAccrual() || transaction.isNonMonetaryTransaction())) {
                repaymentsOrWaivers.add(transaction);
            }
        }
        repaymentsOrWaivers.sort(LoanTransactionComparator.INSTANCE);
        return repaymentsOrWaivers;
    }

    public List<LoanTransaction> retrieveListOfAccrualTransactions() {
        final List<LoanTransaction> transactions = new ArrayList<>();
        for (final LoanTransaction transaction : this.loanTransactions) {
            if (transaction.isNotReversed() && transaction.isAccrual()) {
                transactions.add(transaction);
            }
        }
        transactions.sort(LoanTransactionComparator.INSTANCE);
        return transactions;
    }

    public List<LoanTransaction> retrieveListOfTransactionsByType(final LoanTransactionType transactionType) {
        final List<LoanTransaction> transactions = new ArrayList<>();
        for (final LoanTransaction transaction : this.loanTransactions) {
            if (transaction.isNotReversed() && transaction.getTypeOf().equals(transactionType)) {
                transactions.add(transaction);
            }
        }
        transactions.sort(LoanTransactionComparator.INSTANCE);
        return transactions;
    }

    private boolean doPostLoanTransactionChecks(final LocalDate transactionDate,
            final LoanLifecycleStateMachine loanLifecycleStateMachine) {
        boolean statusChanged = false;
        boolean isOverpaid = getTotalOverpaid() != null && getTotalOverpaid().compareTo(BigDecimal.ZERO) > 0;
        if (isOverpaid) {
            handleLoanOverpayment(transactionDate, loanLifecycleStateMachine);
            statusChanged = true;
        } else if (this.summary.isRepaidInFull(loanCurrency())) {
            handleLoanRepaymentInFull(transactionDate, loanLifecycleStateMachine);
            statusChanged = true;
        } else {
            loanLifecycleStateMachine.transition(LoanEvent.LOAN_REPAYMENT_OR_WAIVER, this);
        }
        if (this.totalOverpaid == null || BigDecimal.ZERO.compareTo(this.totalOverpaid) == 0) {
            this.overpaidOnDate = null;
        }
        return statusChanged;
    }

    private void handleLoanRepaymentInFull(final LocalDate transactionDate, final LoanLifecycleStateMachine loanLifecycleStateMachine) {

        boolean isAllChargesPaid = true;
        // check if foreclosure is happening on a loan created today, this means charges would not be paid
        boolean isChargeRepaymentRequired = getDisburseDonDate().equals(DateUtils.getLocalDateOfTenant());

        for (final LoanCharge loanCharge : this.charges) {
            if (loanCharge.isActive() && loanCharge.amount().compareTo(BigDecimal.ZERO) > 0
                    && !(loanCharge.isPaid() || loanCharge.isWaived())) {
                isAllChargesPaid = false;
                break;
            }
        }
        if (isAllChargesPaid || isChargeRepaymentRequired) {
            // Only close the loan if it's not a revolving credit loan
            if (!isRevolvingLoan()) {
                this.closedOnDate = transactionDate;
                this.actualMaturityDate = transactionDate;
                loanLifecycleStateMachine.transition(LoanEvent.REPAID_IN_FULL, this);
            } else {
                // For revolving credit, just handle it as a regular repayment to keep the loan active
                loanLifecycleStateMachine.transition(LoanEvent.LOAN_REPAYMENT_OR_WAIVER, this);
            }
        } else if (LoanStatus.fromInt(this.loanStatus).isOverpaid()) {
            if (this.totalOverpaid == null || BigDecimal.ZERO.compareTo(this.totalOverpaid) == 0) {
                this.overpaidOnDate = null;
            }
            loanLifecycleStateMachine.transition(LoanEvent.LOAN_REPAYMENT_OR_WAIVER, this);
        }
        processIncomeAccrualTransactionOnLoanClosure();
    }

    private void processIncomeAccrualTransactionOnLoanClosure() {
        if (this.loanInterestRecalculationDetails != null && this.loanInterestRecalculationDetails.isCompoundingToBePostedAsTransaction()
                && this.getStatus().isClosedObligationsMet() && !isNpa() && !isChargedOff()) {

            ExternalId externalIdV2 = ExternalId.empty();
            boolean isExternalIdAutoGenerationEnabled = TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled();

            LocalDate closedDate = this.getClosedOnDate();
            reverseTransactionsOnOrAfter(retrieveListOfIncomePostingTransactions(), closedDate);
            reverseTransactionsOnOrAfter(retrieveListOfAccrualTransactions(), closedDate);
            HashMap<String, BigDecimal> cumulativeIncomeFromInstallments = new HashMap<>();
            determineCumulativeIncomeFromInstallments(cumulativeIncomeFromInstallments);
            HashMap<String, BigDecimal> cumulativeIncomeFromIncomePosting = new HashMap<>();
            determineCumulativeIncomeDetails(retrieveListOfIncomePostingTransactions(), cumulativeIncomeFromIncomePosting);
            BigDecimal interestToPost = cumulativeIncomeFromInstallments.get(INTEREST)
                    .subtract(cumulativeIncomeFromIncomePosting.get(INTEREST));
            BigDecimal feeToPost = cumulativeIncomeFromInstallments.get(FEE).subtract(cumulativeIncomeFromIncomePosting.get(FEE));
            BigDecimal penaltyToPost = cumulativeIncomeFromInstallments.get(PENALTY)
                    .subtract(cumulativeIncomeFromIncomePosting.get(PENALTY));
            BigDecimal amountToPost = interestToPost.add(feeToPost).add(penaltyToPost);
            if (isExternalIdAutoGenerationEnabled) {
                externalIdV2 = ExternalId.generate();
            }
            LoanTransaction finalIncomeTransaction = LoanTransaction.incomePosting(this, this.getOffice(), closedDate, amountToPost,
                    interestToPost, feeToPost, penaltyToPost, externalIdV2);
            addLoanTransaction(finalIncomeTransaction);
            if (Boolean.TRUE.equals(isPeriodicAccrualAccountingEnabledOnLoanProduct())) {
                List<LoanTransaction> updatedAccrualTransactions = retrieveListOfAccrualTransactions();
                LocalDate lastAccruedDate = this.getDisbursementDate();
                if (!updatedAccrualTransactions.isEmpty()) {
                    lastAccruedDate = updatedAccrualTransactions.get(updatedAccrualTransactions.size() - 1).getTransactionDate();
                }
                HashMap<String, Object> feeDetails = new HashMap<>();
                determineFeeDetails(lastAccruedDate, closedDate, feeDetails);
                if (isExternalIdAutoGenerationEnabled) {
                    externalIdV2 = ExternalId.generate();
                }
                LoanTransaction finalAccrual = LoanTransaction.accrueTransaction(this, this.getOffice(), closedDate, amountToPost,
                        interestToPost, feeToPost, penaltyToPost, externalIdV2);
                updateLoanChargesPaidBy(finalAccrual, feeDetails, null);
                addLoanTransaction(finalAccrual);
            }
        }
        updateLoanOutstandingBalances();
    }

    private void determineCumulativeIncomeFromInstallments(HashMap<String, BigDecimal> cumulativeIncomeFromInstallments) {
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal penalty = BigDecimal.ZERO;
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (LoanRepaymentScheduleInstallment installment : installments) {
            interest = interest.add(installment.getInterestCharged(getCurrency()).getAmount());
            fee = fee.add(installment.getFeeChargesCharged(getCurrency()).getAmount());
            penalty = penalty.add(installment.getPenaltyChargesCharged(getCurrency()).getAmount());
        }
        cumulativeIncomeFromInstallments.put(INTEREST, interest);
        cumulativeIncomeFromInstallments.put(FEE, fee);
        cumulativeIncomeFromInstallments.put(PENALTY, penalty);
    }

    private void determineCumulativeIncomeDetails(Collection<LoanTransaction> transactions, HashMap<String, BigDecimal> incomeDetailsMap) {
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal penalty = BigDecimal.ZERO;
        for (LoanTransaction transaction : transactions) {
            interest = interest.add(transaction.getInterestPortion(getCurrency()).getAmount());
            fee = fee.add(transaction.getFeeChargesPortion(getCurrency()).getAmount());
            penalty = penalty.add(transaction.getPenaltyChargesPortion(getCurrency()).getAmount());
        }
        incomeDetailsMap.put(INTEREST, interest);
        incomeDetailsMap.put(FEE, fee);
        incomeDetailsMap.put(PENALTY, penalty);
    }

    private void handleLoanOverpayment(LocalDate transactionDate, final LoanLifecycleStateMachine loanLifecycleStateMachine) {
        this.overpaidOnDate = transactionDate;
        loanLifecycleStateMachine.transition(LoanEvent.LOAN_OVERPAYMENT, this);
        this.closedOnDate = null;
        this.actualMaturityDate = null;
    }

    private boolean isChronologicallyLatestRepaymentOrWaiver(final LoanTransaction loanTransaction,
            final List<LoanTransaction> loanTransactions) {
        boolean isChronologicallyLatestRepaymentOrWaiver = true;

        final LocalDate currentTransactionDate = loanTransaction.getTransactionDate();
        for (final LoanTransaction previousTransaction : loanTransactions) {
            if (!previousTransaction.isDisbursement() && previousTransaction.isNotReversed()
                    && !DateUtils.isAfter(currentTransactionDate, previousTransaction.getTransactionDate())) {
                isChronologicallyLatestRepaymentOrWaiver = false;
                break;
            }
        }
        return isChronologicallyLatestRepaymentOrWaiver;
    }

    private boolean isChronologicallyLatestRepaymentOrWaiverForProgressiveLoans(final LoanTransaction loanTransaction,
            final List<LoanTransaction> loanTransactions) {
        boolean isChronologicallyLatestRepaymentOrWaiver = true;

        final LocalDate currentTransactionDate = loanTransaction.getTransactionDate();
        for (final LoanTransaction previousTransaction : loanTransactions) {
            if (!previousTransaction.isDisbursement() && previousTransaction.isNotReversed() && !previousTransaction.isAccrual()
                    && !DateUtils.isOnOrAfter(currentTransactionDate, previousTransaction.getTransactionDate())) {
                isChronologicallyLatestRepaymentOrWaiver = false;
                break;
            }
        }
        return isChronologicallyLatestRepaymentOrWaiver;
    }

    private boolean isTransactionBeforeLastRepaymentTransaction(final LoanTransaction loanTransaction,
            final List<LoanTransaction> loanTransactions) {
        boolean isTransactionNotBeforeLastRepaymentTransaction = true;

        final LocalDate currentTransactionDate = loanTransaction.getTransactionDate();
        for (final LoanTransaction previousTransaction : loanTransactions) {
            if (!previousTransaction.isDisbursement() && previousTransaction.isNotReversed() && !previousTransaction.isAccrual()
                    && !DateUtils.isOnOrAfter(currentTransactionDate, previousTransaction.getTransactionDate())) {
                isTransactionNotBeforeLastRepaymentTransaction = false;
                break;
            }
        }
        return isTransactionNotBeforeLastRepaymentTransaction;
    }

    private boolean isAfterLatRepayment(final LoanTransaction loanTransaction, final List<LoanTransaction> loanTransactions) {
        boolean isAfterLatRepayment = true;

        final LocalDate currentTransactionDate = loanTransaction.getTransactionDate();
        for (final LoanTransaction previousTransaction : loanTransactions) {
            if (previousTransaction.isRepaymentLikeType() && previousTransaction.isNotReversed()
                    && DateUtils.isBefore(currentTransactionDate, previousTransaction.getTransactionDate())) {
                isAfterLatRepayment = false;
                break;
            }
        }
        return isAfterLatRepayment;
    }

    private boolean isChronologicallyLatestTransaction(final LoanTransaction loanTransaction,
            final List<LoanTransaction> loanTransactions) {
        boolean isChronologicallyLatestRepaymentOrWaiver = true;

        final LocalDate currentTransactionDate = loanTransaction.getTransactionDate();
        for (final LoanTransaction previousTransaction : loanTransactions) {
            if (previousTransaction.isNotReversed()
                    && !DateUtils.isAfter(currentTransactionDate, previousTransaction.getTransactionDate())) {
                isChronologicallyLatestRepaymentOrWaiver = false;
                break;
            }
        }
        return isChronologicallyLatestRepaymentOrWaiver;
    }

    public LocalDate possibleNextRepaymentDate(final String nextPaymentDueDateConfig) {
        LocalDate nextPossibleRepaymentDate = null;
        if (EARLIEST_UNPAID_DATE.equalsIgnoreCase(nextPaymentDueDateConfig)) {
            nextPossibleRepaymentDate = getEarliestUnpaidInstallmentDate();
        } else if (NEXT_UNPAID_DUE_DATE.equalsIgnoreCase(nextPaymentDueDateConfig)) {
            nextPossibleRepaymentDate = getNextUnpaidInstallmentDueDate();
        }
        return nextPossibleRepaymentDate;
    }

    private LocalDate getNextUnpaidInstallmentDueDate() {
        LocalDate nextUnpaidInstallmentDate = null;
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        LocalDate currentBusinessDate = DateUtils.getBusinessLocalDate();
        LocalDate expectedMaturityDateV2 = determineExpectedMaturityDate();

        for (final LoanRepaymentScheduleInstallment installment : installments) {
            boolean isCurrentDateBeforeInstallmentAndLoanPeriod = DateUtils.isBefore(currentBusinessDate, installment.getDueDate())
                    && DateUtils.isBefore(currentBusinessDate, expectedMaturityDateV2);
            if (installment.isDownPayment()) {
                isCurrentDateBeforeInstallmentAndLoanPeriod = DateUtils.isEqual(currentBusinessDate, installment.getDueDate())
                        && DateUtils.isBefore(currentBusinessDate, expectedMaturityDateV2);
            }
            if (isCurrentDateBeforeInstallmentAndLoanPeriod && installment.isNotFullyPaidOff()) {
                nextUnpaidInstallmentDate = installment.getDueDate();
                break;
            }

        }
        return nextUnpaidInstallmentDate;
    }

    private LocalDate getEarliestUnpaidInstallmentDate() {
        LocalDate earliestUnpaidInstallmentDate = DateUtils.getBusinessLocalDate();
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment installment : installments) {
            if (installment.isNotFullyPaidOff()) {
                earliestUnpaidInstallmentDate = installment.getDueDate();
                break;
            }
        }

        LocalDate lastTransactionDate = null;
        for (final LoanTransaction transaction : this.loanTransactions) {
            if (transaction.isRepaymentLikeType() && transaction.isNonZero()) {
                lastTransactionDate = transaction.getTransactionDate();
            }
        }

        LocalDate possibleNextRepaymentDate = earliestUnpaidInstallmentDate;
        if (DateUtils.isAfter(lastTransactionDate, earliestUnpaidInstallmentDate)) {
            possibleNextRepaymentDate = lastTransactionDate;
        }

        return possibleNextRepaymentDate;
    }

    public LoanRepaymentScheduleInstallment possibleNextRepaymentInstallment() {
        LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = null;
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment installment : installments) {
            if (installment.isNotFullyPaidOff()) {
                loanRepaymentScheduleInstallment = installment;
                break;
            }
        }

        return loanRepaymentScheduleInstallment;
    }

    public LoanTransaction deriveDefaultInterestWaiverTransaction() {

        final Money totalInterestOutstanding = getTotalInterestOutstandingOnLoan();
        Money possibleInterestToWaive = totalInterestOutstanding.copy();
        LocalDate transactionDate = DateUtils.getBusinessLocalDate();

        if (totalInterestOutstanding.isGreaterThanZero()) {
            // find earliest known instance of overdue interest and default to
            // that
            List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
            for (final LoanRepaymentScheduleInstallment scheduledRepayment : installments) {

                final Money outstandingForPeriod = scheduledRepayment.getInterestOutstanding(loanCurrency());
                if (scheduledRepayment.isOverdueOn(DateUtils.getBusinessLocalDate()) && scheduledRepayment.isNotFullyPaidOff()
                        && outstandingForPeriod.isGreaterThanZero()) {
                    transactionDate = scheduledRepayment.getDueDate();
                    possibleInterestToWaive = outstandingForPeriod;
                    break;
                }
            }
        }

        return LoanTransaction.waiver(getOffice(), this, possibleInterestToWaive, transactionDate, possibleInterestToWaive,
                possibleInterestToWaive.zero(), ExternalId.empty());
    }

    public ChangedTransactionDetail adjustExistingTransaction(final LoanTransaction newTransactionDetail,
            final LoanLifecycleStateMachine loanLifecycleStateMachine, final LoanTransaction transactionForAdjustment,
            final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds,
            final ScheduleGeneratorDTO scheduleGeneratorDTO, final ExternalId reversalExternalId) {

        HolidayDetailDTO holidayDetailDTO = scheduleGeneratorDTO.getHolidayDetailDTO();
        validateActivityNotBeforeLastTransactionDate(LoanEvent.LOAN_REPAYMENT_OR_WAIVER, transactionForAdjustment.getTransactionDate());
        validateRepaymentDateIsOnHoliday(newTransactionDetail.getTransactionDate(), holidayDetailDTO.isAllowTransactionsOnHoliday(),
                holidayDetailDTO.getHolidays());
        validateRepaymentDateIsOnNonWorkingDay(newTransactionDetail.getTransactionDate(), holidayDetailDTO.getWorkingDays(),
                holidayDetailDTO.isAllowTransactionsOnNonWorkingDay());

        ChangedTransactionDetail changedTransactionDetail = null;

        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());

        validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.LOAN_REPAYMENT_OR_WAIVER,
                transactionForAdjustment.getTransactionDate());

        if (transactionForAdjustment.isNotRepaymentLikeType() && transactionForAdjustment.isNotWaiver()
                && transactionForAdjustment.isNotCreditBalanceRefund() && !transactionForAdjustment.isDisbursement()) {
            final String errorMessage = "Only (non-reversed) transactions of type repayment, waiver, credit balance refund, or disbursement can be adjusted.";
            throw new InvalidLoanTransactionTypeException(Loan.TRANSACTION_PARAM,
                    "adjustment.is.only.allowed.to.repayment.or.waiver.or.creditbalancerefund.or.disbursement.transactions", errorMessage);
        }

        transactionForAdjustment.reverse(reversalExternalId);
        transactionForAdjustment.manuallyAdjustedOrReversed();

        if (isClosedWrittenOff()) {
            // find write off transaction and reverse it
            final LoanTransaction writeOffTransaction = findWriteOffTransaction();
            writeOffTransaction.reverse();
        }

        if (isClosedObligationsMet() || isClosedWrittenOff() || isClosedWithOutsandingAmountMarkedForReschedule()) {
            loanLifecycleStateMachine.transition(LoanEvent.LOAN_ADJUST_TRANSACTION, this);
        }

        if (newTransactionDetail.isRepaymentLikeType() || newTransactionDetail.isInterestWaiver()) {

            // Reverse the entry from disbursement_detail before regenerating the schedule to affect totals
            reverseDisbursementDetaisAndRescheduleRequest(transactionForAdjustment);

            changedTransactionDetail = handleRepaymentOrRecoveryOrWaiverTransaction(newTransactionDetail, loanLifecycleStateMachine,
                    transactionForAdjustment, scheduleGeneratorDTO);
        } else if (newTransactionDetail.isDisbursement()) {
            // Handle disbursement adjustment
            validateAccountStatus(LoanEvent.LOAN_DISBURSED);
            existingTransactionIds.addAll(findExistingTransactionIds());
            existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());

            newTransactionDetail.updateLoan(this);
            if (newTransactionDetail.isNotZero(loanCurrency())) {
                addLoanTransaction(newTransactionDetail);
            }

            if (this.repaymentScheduleDetail().isInterestRecalculationEnabled() || isProgressiveLoan()) {
                regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
            }

            updateLoanSummaryDerivedFields();
            doPostLoanTransactionChecks(newTransactionDetail.getTransactionDate(), loanLifecycleStateMachine);

            changedTransactionDetail = new ChangedTransactionDetail();
            changedTransactionDetail.getNewTransactionMappings().put(newTransactionDetail.getId(), newTransactionDetail);
        }

        return changedTransactionDetail;
    }

    private void reverseDisbursementDetaisAndRescheduleRequest(LoanTransaction transactionForAdjustment) {

        if (this.isMultiDisburmentLoan() && LoanTransactionType.DISBURSEMENT.equals(transactionForAdjustment.getTypeOf())
                && transactionForAdjustment.isReversed()) {

            // Revert entry from disbursement details and leave repayment schedule without this disbursal
            disbursementDetails.stream().filter(disbursementDetail -> Boolean.FALSE.equals(disbursementDetail.isReversed()))
                    .filter(samePrincipal -> samePrincipal.getPrincipal().compareTo(transactionForAdjustment.getAmount()) == 0)
                    .sorted(Comparator.comparing(LoanDisbursementDetails::getId).reversed()) //
                    .findFirst() //
                    .ifPresent(LoanDisbursementDetails::reverse);

            // Check edge case when new installmentes were added by this disbursal, then we need to revert the
            // reschedule request as well
            loanTermVariations.stream()
                    .filter(loanTermVariation -> LoanTermVariationType.EXTEND_REPAYMENT_PERIOD.equals(loanTermVariation.getTermType()))
                    .filter(createdByMifos -> createdByMifos.getCreatedBy().isPresent()
                            && createdByMifos.getCreatedBy().get().compareTo(1L) == 0)
                    .filter(active -> active.isActive()) //
                    .sorted(Comparator.comparing(LoanTermVariations::getId).reversed()) //
                    .findFirst() //
                    .ifPresent(loanTermVariation -> {
                        loanTermVariation.setDecimalValue(BigDecimal.ZERO);
                        loanTermVariation.updateIsActive(false);
                    });
        }
    }

    public ChangedTransactionDetail undoWrittenOff(LoanLifecycleStateMachine loanLifecycleStateMachine,
            final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds,
            final ScheduleGeneratorDTO scheduleGeneratorDTO) {

        validateAccountStatus(LoanEvent.WRITE_OFF_OUTSTANDING_UNDO);
        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());
        final LoanTransaction writeOffTransaction = findWriteOffTransaction();
        writeOffTransaction.reverse();
        loanLifecycleStateMachine.transition(LoanEvent.WRITE_OFF_OUTSTANDING_UNDO, this);
        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retrieveListOfTransactionsPostDisbursement();
        if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
        }
        ChangedTransactionDetail changedTransactionDetail = loanRepaymentScheduleTransactionProcessor.reprocessLoanTransactions(
                getDisbursementDate(), allNonContraTransactionsPostDisbursement, getCurrency(), getRepaymentScheduleInstallments(),
                getActiveCharges());
        updateLoanSummaryDerivedFields();
        return changedTransactionDetail;
    }

    public LoanTransaction findWriteOffTransaction() {

        LoanTransaction writeOff = null;
        for (final LoanTransaction transaction : this.loanTransactions) {
            if (!transaction.isReversed() && transaction.isWriteOff()) {
                writeOff = transaction;
            }
        }

        return writeOff;
    }

    private boolean isOverPaid() {
        return calculateTotalOverpayment().isGreaterThanZero();
    }

    private Money calculateTotalOverpayment() {

        Money totalPaidInRepayments = getTotalPaidInRepayments();

        // Check if this is a foreclosure scenario
        if (isForeclosure()) {
            log.info("Loan ID {} is in foreclosure status, applying foreclosure-specific overpayment calculation", getId());

            // Get the total outstanding balance before foreclosure
            Money totalOutstandingBalance = getLoanSummary().getTotalOutstanding(loanCurrency());

            log.info("Foreclosure calculation - totalPaidInRepayments: {}, totalOutstandingBalance: {}", totalPaidInRepayments,
                    totalOutstandingBalance);

            // If the total paid in repayments equals the outstanding balance, return zero overpayment
            if (totalPaidInRepayments.isEqualTo(totalOutstandingBalance)) {
                log.info("Foreclosure transaction amount ({}) equals outstanding balance ({}), returning zero overpayment for loan ID {}",
                        totalPaidInRepayments, totalOutstandingBalance, getId());
                return Money.zero(loanCurrency());
            }

            // If there's a small difference (likely due to rounding), also return zero overpayment
            Money difference = totalPaidInRepayments.minus(totalOutstandingBalance);
            Money tolerance = Money.of(loanCurrency(), new BigDecimal("0.01"));
            if (difference.isLessThan(tolerance) || difference.isEqualTo(tolerance)) {
                log.info(
                        "Foreclosure transaction amount ({}) is within rounding tolerance of outstanding balance ({}), returning zero overpayment for loan ID {}",
                        totalPaidInRepayments, totalOutstandingBalance, getId());
                return Money.zero(loanCurrency());
            }

            log.info(
                    "Foreclosure transaction amount ({}) differs significantly from outstanding balance ({}), applying standard calculation",
                    totalPaidInRepayments, totalOutstandingBalance);
        }

        final MonetaryCurrency currency = loanCurrency();
        Money cumulativeTotalPaidOnInstallments = Money.zero(currency);
        Money cumulativeTotalWaivedOnInstallments = Money.zero(currency);
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment scheduledRepayment : installments) {
            final Money scheduleWrittenOffValue = scheduledRepayment.getPrincipalWrittenOff(currency)
                    .plus(scheduledRepayment.getInterestWrittenOff(currency)).plus(scheduledRepayment.getFeeChargesWrittenOff(currency))
                    .plus(scheduledRepayment.getPenaltyChargesWrittenOff(currency));
            cumulativeTotalPaidOnInstallments = cumulativeTotalPaidOnInstallments
                    .plus(scheduledRepayment.getPrincipalCompleted(currency).plus(scheduledRepayment.getInterestPaid(currency)))
                    .plus(scheduledRepayment.getFeeChargesPaid(currency)).plus(scheduledRepayment.getPenaltyChargesPaid(currency))
                    .plus(scheduleWrittenOffValue);
            if (scheduledRepayment.isLastInstallment(installments) && scheduledRepayment.isOverpaidInAdvance(currency)
                    && scheduledRepayment.getAdvancePrincipalAmount().compareTo(BigDecimal.ZERO) > 0) {
                cumulativeTotalWaivedOnInstallments = cumulativeTotalWaivedOnInstallments
                        .plus(scheduledRepayment.getInterestWaived(currency));
                // Do not add advance payment amount if installment was overpaid
                continue;
            } else {
                cumulativeTotalPaidOnInstallments = cumulativeTotalPaidOnInstallments.plus(scheduledRepayment.getAdvancePrincipalAmount());
            }

            cumulativeTotalWaivedOnInstallments = cumulativeTotalWaivedOnInstallments.plus(scheduledRepayment.getInterestWaived(currency));
        }

        for (final LoanTransaction loanTransaction : this.loanTransactions) {
            if (loanTransaction.isReversed()) {
                continue;
            }
            if (loanTransaction.isRefund() || loanTransaction.isRefundForActiveLoan()) {
                totalPaidInRepayments = totalPaidInRepayments.minus(loanTransaction.getAmount(currency));
            } else if (loanTransaction.isCreditBalanceRefund() || loanTransaction.isChargeback() || loanTransaction.isDisbursement()) {
                totalPaidInRepayments = totalPaidInRepayments.minus(loanTransaction.getOverPaymentPortion(currency));
            }
        }

        Money overpayment = totalPaidInRepayments.minus(cumulativeTotalPaidOnInstallments);

        if (isForeclosure()) {
            log.info(
                    "Final foreclosure overpayment calculation: totalPaidInRepayments={}, cumulativeTotalPaidOnInstallments={}, overpayment={}",
                    totalPaidInRepayments, cumulativeTotalPaidOnInstallments, overpayment);
        }

        return overpayment;
    }

    public Money calculateTotalRecoveredPayments() {
        // in case logic for reversing recovered payment is implemented handle
        // subtraction from totalRecoveredPayments
        return getTotalRecoveredPayments();
    }

    private MonetaryCurrency loanCurrency() {
        return this.loanRepaymentScheduleDetail.getCurrency();
    }

    public void closeAsWrittenOff(final LocalDate writtenOffOnLocalDate, final AppUser currentUser) {
        this.loanStatus = LoanStatus.CLOSED_WRITTEN_OFF.getValue();
        this.closedOnDate = writtenOffOnLocalDate;
        this.writtenOffOnDate = writtenOffOnLocalDate;
        this.closedBy = currentUser;
    }

    public ChangedTransactionDetail closeAsWrittenOff(final JsonCommand command, final LoanLifecycleStateMachine loanLifecycleStateMachine,
            final Map<String, Object> changes, final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds,
            final AppUser currentUser, final ScheduleGeneratorDTO scheduleGeneratorDTO) {

        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        ChangedTransactionDetail changedTransactionDetail = closeDisbursements(scheduleGeneratorDTO,
                loanRepaymentScheduleTransactionProcessor);

        validateAccountStatus(LoanEvent.WRITE_OFF_OUTSTANDING);

        final LocalDate writtenOffOnLocalDate = command.localDateValueOfParameterNamed(TRANSACTION_DATE);
        this.closedOnDate = writtenOffOnLocalDate;
        this.writtenOffOnDate = writtenOffOnLocalDate;
        this.closedBy = currentUser;
        final LoanStatus statusEnum = loanLifecycleStateMachine.dryTransition(LoanEvent.WRITE_OFF_OUTSTANDING, this);

        LoanTransaction loanTransaction = null;
        if (!statusEnum.hasStateOf(LoanStatus.fromInt(this.loanStatus))) {
            loanLifecycleStateMachine.transition(LoanEvent.WRITE_OFF_OUTSTANDING, this);
            changes.put(PARAM_STATUS, LoanEnumerations.status(this.loanStatus));

            existingTransactionIds.addAll(findExistingTransactionIds());
            existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());

            final String txnExternalId = command.stringValueOfParameterNamedAllowingNull(EXTERNAL_ID);

            ExternalId externalIdV2 = ExternalIdFactory.produce(txnExternalId);

            if (externalIdV2.isEmpty() && TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
                externalIdV2 = ExternalId.generate();
            }

            changes.put(CLOSED_ON_DATE, command.stringValueOfParameterNamed(TRANSACTION_DATE));
            changes.put(WRITTEN_OFF_ON_DATE, command.stringValueOfParameterNamed(TRANSACTION_DATE));
            changes.put(Loan.EXTERNAL_ID, externalIdV2);

            if (DateUtils.isBefore(writtenOffOnLocalDate, getDisbursementDate())) {
                final String errorMessage = "The date on which a loan is written off cannot be before the loan disbursement date: "
                        + getDisbursementDate().toString();
                throw new InvalidLoanStateTransitionException(Loan.WRITE_OFF_PARAM,
                        Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_SUBMITTAL_DATE, errorMessage, writtenOffOnLocalDate,
                        getDisbursementDate());
            }

            validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.WRITE_OFF_OUTSTANDING, writtenOffOnLocalDate);

            if (DateUtils.isDateInTheFuture(writtenOffOnLocalDate)) {
                final String errorMessage = "The date on which a loan is written off cannot be in the future.";
                throw new InvalidLoanStateTransitionException(Loan.WRITE_OFF_PARAM, Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE,
                        errorMessage, writtenOffOnLocalDate);
            }
            final BigDecimal totalOutstanding = summary.getTotalOutstanding();
            loanTransaction = LoanTransaction.writeoff(this, getOffice(), writtenOffOnLocalDate, externalIdV2, totalOutstanding);
            LocalDate lastTransactionDate = getLastUserTransactionDate();
            if (DateUtils.isAfter(lastTransactionDate, writtenOffOnLocalDate)) {
                final String errorMessage = "The date of the writeoff transaction must occur on or before previous transactions.";
                throw new InvalidLoanStateTransitionException(Loan.WRITE_OFF_PARAM, "must.occur.on.or.after.other.transaction.dates",
                        errorMessage, writtenOffOnLocalDate);
            }

            addLoanTransaction(loanTransaction);

            loanRepaymentScheduleTransactionProcessor.processLatestTransaction(loanTransaction, new TransactionCtx(loanCurrency(),
                    getRepaymentScheduleInstallments(), getActiveCharges(), new MoneyHolder(getTotalOverpaidAsMoney())));
            updateLoanSummaryDerivedFields();
        }
        if (changedTransactionDetail == null) {
            changedTransactionDetail = new ChangedTransactionDetail();
        }
        changedTransactionDetail.getNewTransactionMappings().put(0L, loanTransaction);
        return changedTransactionDetail;
    }

    public ChangedTransactionDetail validateSpecialWrittenOff(final JsonCommand command, final List<Long> existingTransactionIds,
            final List<Long> existingReversedTransactionIds, final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        ChangedTransactionDetail changedTransactionDetail = closeDisbursements(scheduleGeneratorDTO,
                loanRepaymentScheduleTransactionProcessor);
        validateAccountStatus(LoanEvent.WRITE_OFF_OUTSTANDING);
        final LocalDate writtenOffOnLocalDate = DateUtils.getBusinessLocalDate();
        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());
        if (DateUtils.isBefore(writtenOffOnLocalDate, getDisbursementDate())) {
            final LocalDate disbursementDate = getDisbursementDate();
            final String disburseDateString = DateUtils.format(disbursementDate, command.dateFormat(),
                    Locale.forLanguageTag(command.locale()));
            throw new GeneralPlatformDomainRuleException("error.msg.loan.written.off.date.cannot.before.disbursement.date",
                    "The date on which a loan is written off cannot be before the loan disbursement date: "
                            + getDisbursementDate().toString(),
                    disburseDateString);
        }
        validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.WRITE_OFF_OUTSTANDING, writtenOffOnLocalDate);
        if (DateUtils.isDateInTheFuture(writtenOffOnLocalDate)) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.written.off.date.cannot.be.in.the.future",
                    "The date on which a loan is written off cannot be in the future.", writtenOffOnLocalDate);
        }
        LocalDate lastTransactionDate = getLastUserTransactionDate();
        if (DateUtils.isAfter(lastTransactionDate, writtenOffOnLocalDate)) {
            final String lastTransactionDateString = DateUtils.format(lastTransactionDate, command.dateFormat(),
                    Locale.forLanguageTag(command.locale()));
            throw new GeneralPlatformDomainRuleException("error.msg.loan.written.off.date.must.occur.on.or.before.other.transaction.dates",
                    "The date of the writeoff transaction must occur on or before previous transactions.", lastTransactionDateString);
        }
        if (changedTransactionDetail == null) {
            changedTransactionDetail = new ChangedTransactionDetail();
        }
        return changedTransactionDetail;
    }

    @SuppressWarnings({ "squid:S3776" })
    public LoanRepaymentScheduleInstallmentData validateSpecialWriteOffConcepts(final JsonCommand command,
            final LoanRepaymentScheduleInstallment specialWriteOffInstallment) {
        final MonetaryCurrency currency = loanCurrency();
        final Money maximumPrincipalPortion = specialWriteOffInstallment.getPrincipalOutstanding(currency);
        final Money maximumInterestPortion = specialWriteOffInstallment.getInterestOutstanding(currency);
        final List<LoanChargeData> currentOutstandingLoanCharges = specialWriteOffInstallment.getCurrentOutstandingLoanCharges();
        final Money principalPortion = Money.of(currency, command.bigDecimalValueOfParameterNamed("principalPortion"));
        final Money interestPortion = Money.of(currency, command.bigDecimalValueOfParameterNamed("interestPortion"));
        if (principalPortion.isGreaterThan(maximumPrincipalPortion)) {
            throw new GeneralPlatformDomainRuleException("principal.portion.must.be.less.than.outstanding.principal",
                    "The principal portion of the special write off transaction must be less than or equal to the outstanding principal amount of the loan.",
                    maximumPrincipalPortion.toString());
        }
        if (interestPortion.isGreaterThan(specialWriteOffInstallment.getInterestOutstanding(currency))) {
            throw new GeneralPlatformDomainRuleException("interest.portion.must.be.less.than.outstanding.interest",
                    "The interest portion of the special write off transaction must be less than or equal to the outstanding interest amount of the loan.",
                    maximumInterestPortion.toString());
        }
        final Set<LoanChargeData> penaltyCharges = new HashSet<>();
        final Set<LoanChargeData> feeCharges = new HashSet<>();
        final JsonArray writeOffCharges = command.arrayOfParameterNamed(Loan.PARAM_CHARGES);
        if (writeOffCharges != null && !writeOffCharges.isEmpty()) {
            for (JsonElement jsonElement : writeOffCharges) {
                final JsonObject jsonObject = jsonElement.getAsJsonObject();
                final Long chargeId = jsonObject.get("chargeId").getAsLong();
                final BigDecimal writeOffAmount = jsonObject.get("writeOffAmount").getAsBigDecimal();
                if (writeOffAmount != null && writeOffAmount.compareTo(BigDecimal.ZERO) > 0) {
                    final LoanChargeData writeOffLoanChargeData = LoanChargeData.builder().chargeId(chargeId)
                            .amountOutstanding(writeOffAmount).build();
                    final Optional<LoanChargeData> optionLoanCharge = currentOutstandingLoanCharges.stream()
                            .filter(c -> chargeId.equals(c.getChargeId())).findFirst();
                    if (optionLoanCharge.isPresent()) {
                        final LoanChargeData loanCharge = optionLoanCharge.get();
                        final String chargeName = loanCharge.getName();
                        final BigDecimal maximumChargeAmount = loanCharge.getAmountOutstanding();
                        if (writeOffLoanChargeData.getAmountOutstanding().compareTo(maximumChargeAmount) > 0) {
                            throw new GeneralPlatformDomainRuleException("charge.amount.must.be.less.than.outstanding.charge.amount",
                                    "The charge amount of the special write off transaction must be less than or equal to the outstanding charge amount of the loan.",
                                    maximumChargeAmount, chargeName);
                        }
                        if (Boolean.TRUE.equals(loanCharge.getPenalty())) {
                            penaltyCharges.add(writeOffLoanChargeData);
                        } else {
                            feeCharges.add(writeOffLoanChargeData);
                        }
                    } else {
                        throw new GeneralPlatformDomainRuleException("error.msg.loan.charge.not.found",
                                "The charge with id " + chargeId + " does not exist.", chargeId);
                    }
                }
            }
        }
        final BigDecimal penaltyChargesPortion = penaltyCharges.stream().map(LoanChargeData::getAmountOutstanding).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        final BigDecimal feeChargesPortion = feeCharges.stream().map(LoanChargeData::getAmountOutstanding).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        final BigDecimal totalInstallmentAmount = principalPortion.getAmount().add(interestPortion.getAmount()).add(penaltyChargesPortion)
                .add(feeChargesPortion);
        return LoanRepaymentScheduleInstallmentData.builder().principalPortion(principalPortion.getAmount())
                .interestPortion(interestPortion.getAmount()).penaltyChargesPortion(penaltyChargesPortion)
                .feeChargesPortion(feeChargesPortion).penaltyCharges(penaltyCharges.stream().toList())
                .feeCharges(feeCharges.stream().toList()).totalInstallmentAmount(totalInstallmentAmount).build();
    }

    public LoanTransaction writeOff(final LoanRepaymentScheduleInstallmentData loanRepaymentScheduleInstallmentData,
            final LocalDate writtenOffOnLocalDate, final ExternalId externalId, boolean isCreditNote) {
        final MonetaryCurrency currency = loanCurrency();
        final Money principalPortion = Money.of(currency, loanRepaymentScheduleInstallmentData.getPrincipalPortion());
        final Money interestPortion = Money.of(currency, loanRepaymentScheduleInstallmentData.getInterestPortion());
        final Money feeChargesPortion = Money.of(currency, loanRepaymentScheduleInstallmentData.getFeeChargesPortion());
        final Money penaltychargesPortion = Money.of(currency, loanRepaymentScheduleInstallmentData.getPenaltyChargesPortion());
        LoanTransaction loanTransaction;
        if (isCreditNote) {
            loanTransaction = LoanTransaction.creditNote(this, getOffice(), writtenOffOnLocalDate, externalId);
        } else {
            loanTransaction = LoanTransaction.writeoff(this, getOffice(), writtenOffOnLocalDate, externalId);
        }
        loanTransaction.setSpecialWriteOff(true);
        loanTransaction.updateComponentsAndTotal(principalPortion, interestPortion, feeChargesPortion, penaltychargesPortion);
        loanTransaction.updateLoan(this);
        addLoanTransaction(loanTransaction);
        final List<LoanRepaymentScheduleInstallment> repaymentInstallments = getRepaymentScheduleInstallments();
        final Set<LoanCharge> activeLoanCharges = getActiveCharges();
        final MoneyHolder overpaymentHolder = new MoneyHolder(getTotalOverpaidAsMoney());
        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        loanRepaymentScheduleTransactionProcessor.processLatestTransaction(loanTransaction,
                new TransactionCtx(getCurrency(), repaymentInstallments, activeLoanCharges, overpaymentHolder));
        updateLoanSummaryDerivedFields();
        return loanTransaction;
    }

    private ChangedTransactionDetail closeDisbursements(final ScheduleGeneratorDTO scheduleGeneratorDTO,
            final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor) {
        ChangedTransactionDetail changedTransactionDetail = null;
        if (isDisbursementAllowed() && atleastOnceDisbursed()) {
            this.loanRepaymentScheduleDetail.setPrincipal(getDisbursedAmount());
            removeDisbursementDetail();
            regenerateRepaymentSchedule(scheduleGeneratorDTO);
            if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
                regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
            }
            final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retrieveListOfTransactionsPostDisbursement();
            changedTransactionDetail = loanRepaymentScheduleTransactionProcessor.reprocessLoanTransactions(getDisbursementDate(),
                    allNonContraTransactionsPostDisbursement, getCurrency(), getRepaymentScheduleInstallments(), getActiveCharges());
            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                mapEntry.getValue().updateLoan(this);
                addLoanTransaction(mapEntry.getValue());
            }
            updateLoanSummaryDerivedFields();
            LoanTransaction loanTransaction = getLatestTransaction();
            doPostLoanTransactionChecks(loanTransaction.getTransactionDate(), loanLifecycleStateMachine);
        }
        return changedTransactionDetail;
    }

    public LoanTransaction getLatestTransaction() {
        LoanTransaction transaction = null;
        for (LoanTransaction loanTransaction : this.loanTransactions) {
            if (!loanTransaction.isReversed() && (transaction == null
                    || DateUtils.isBefore(transaction.getTransactionDate(), loanTransaction.getTransactionDate()))) {
                transaction = loanTransaction;
            }
        }
        return transaction;
    }

    @SuppressWarnings({ "squid:S3776" })
    public ChangedTransactionDetail close(final JsonCommand command, final LoanLifecycleStateMachine loanLifecycleStateMachine,
            final Map<String, Object> changes, final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds,
            final ScheduleGeneratorDTO scheduleGeneratorDTO) {

        validateAccountStatus(LoanEvent.LOAN_CLOSED);

        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());

        final LocalDate closureDate = command.localDateValueOfParameterNamed(TRANSACTION_DATE);
        final String txnExternalId = command.stringValueOfParameterNamedAllowingNull(EXTERNAL_ID);

        ExternalId externalIdV2 = ExternalIdFactory.produce(txnExternalId);
        if (externalIdV2.isEmpty() && TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
            externalIdV2 = ExternalId.generate();
        }

        this.closedOnDate = closureDate;
        changes.put(CLOSED_ON_DATE, command.stringValueOfParameterNamed(TRANSACTION_DATE));

        validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.REPAID_IN_FULL, closureDate);
        if (DateUtils.isBefore(closureDate, getDisbursementDate())) {
            final String errorMessage = "The date on which a loan is closed cannot be before the loan disbursement date: "
                    + getDisbursementDate().toString();
            throw new InvalidLoanStateTransitionException(Loan.CLOSE_PARAM, Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_SUBMITTAL_DATE,
                    errorMessage, closureDate, getDisbursementDate());
        }

        if (DateUtils.isDateInTheFuture(closureDate)) {
            final String errorMessage = "The date on which a loan is closed cannot be in the future.";
            throw new InvalidLoanStateTransitionException(Loan.CLOSE_PARAM, Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage,
                    closureDate);
        }
        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        ChangedTransactionDetail changedTransactionDetail = closeDisbursements(scheduleGeneratorDTO,
                loanRepaymentScheduleTransactionProcessor);

        LoanTransaction loanTransaction = null;
        if (isOpen()) {
            final Money totalOutstanding = this.summary.getTotalOutstanding(loanCurrency());
            if (totalOutstanding.isGreaterThanZero() && getInArrearsTolerance().isGreaterThanOrEqualTo(totalOutstanding)) {

                this.closedOnDate = closureDate;
                final LoanStatus statusEnum = loanLifecycleStateMachine.dryTransition(LoanEvent.REPAID_IN_FULL, this);
                if (!statusEnum.hasStateOf(LoanStatus.fromInt(this.loanStatus))) {
                    loanLifecycleStateMachine.transition(LoanEvent.REPAID_IN_FULL, this);
                    changes.put(PARAM_STATUS, LoanEnumerations.status(this.loanStatus));
                }
                changes.put(Loan.EXTERNAL_ID, externalIdV2);
                loanTransaction = LoanTransaction.writeoff(this, getOffice(), closureDate, externalIdV2);
                final boolean isLastTransaction = isChronologicallyLatestTransaction(loanTransaction, getLoanTransactions());
                if (!isLastTransaction) {
                    final String errorMessage = "The closing date of the loan must be on or after latest transaction date.";
                    throw new InvalidLoanStateTransitionException("close.loan", "must.occur.on.or.after.latest.transaction.date",
                            errorMessage, closureDate);
                }

                addLoanTransaction(loanTransaction);
                loanRepaymentScheduleTransactionProcessor.processLatestTransaction(loanTransaction, new TransactionCtx(loanCurrency(),
                        getRepaymentScheduleInstallments(), getActiveCharges(), new MoneyHolder(getTotalOverpaidAsMoney())));

                updateLoanSummaryDerivedFields();
            } else if (totalOutstanding.isGreaterThanZero()) {
                final String errorMessage = "A loan with money outstanding cannot be closed";
                throw new InvalidLoanStateTransitionException(Loan.CLOSE_PARAM, "loan.has.money.outstanding", errorMessage,
                        totalOutstanding.toString());
            }
        }

        if (isOverPaid()) {
            final Money totalLoanOverpayment = calculateTotalOverpayment();
            if (totalLoanOverpayment.isGreaterThanZero() && getInArrearsTolerance().isGreaterThanOrEqualTo(totalLoanOverpayment)) {
                this.closedOnDate = closureDate;
                final LoanStatus statusEnum = loanLifecycleStateMachine.dryTransition(LoanEvent.REPAID_IN_FULL, this);
                if (!statusEnum.hasStateOf(LoanStatus.fromInt(this.loanStatus))) {
                    loanLifecycleStateMachine.transition(LoanEvent.REPAID_IN_FULL, this);
                    changes.put(PARAM_STATUS, LoanEnumerations.status(this.loanStatus));
                }
            } else if (totalLoanOverpayment.isGreaterThanZero()) {
                final String errorMessage = "The loan is marked as 'Overpaid' and cannot be moved to 'Closed (obligations met).";
                throw new InvalidLoanStateTransitionException(Loan.CLOSE_PARAM, "loan.is.overpaid", errorMessage,
                        totalLoanOverpayment.toString());
            }
        }

        if (changedTransactionDetail == null) {
            changedTransactionDetail = new ChangedTransactionDetail();
        }
        changedTransactionDetail.getNewTransactionMappings().put(0L, loanTransaction);
        return changedTransactionDetail;
    }

    /**
     * Behaviour added to comply with capability of previous mifos product to support easier transition to fineract
     * platform.
     */
    public void closeAsMarkedForReschedule(final JsonCommand command, final LoanLifecycleStateMachine loanLifecycleStateMachine,
            final Map<String, Object> changes) {

        final LocalDate rescheduledOn = command.localDateValueOfParameterNamed(TRANSACTION_DATE);

        this.closedOnDate = rescheduledOn;
        final LoanStatus statusEnum = loanLifecycleStateMachine.dryTransition(LoanEvent.LOAN_RESCHEDULE, this);
        if (!statusEnum.hasStateOf(LoanStatus.fromInt(this.loanStatus))) {
            loanLifecycleStateMachine.transition(LoanEvent.LOAN_RESCHEDULE, this);
            changes.put(PARAM_STATUS, LoanEnumerations.status(this.loanStatus));
        }

        this.rescheduledOnDate = rescheduledOn;
        changes.put(CLOSED_ON_DATE, command.stringValueOfParameterNamed(TRANSACTION_DATE));
        changes.put("rescheduledOnDate", command.stringValueOfParameterNamed(TRANSACTION_DATE));

        if (DateUtils.isBefore(this.rescheduledOnDate, getDisbursementDate())) {
            final String errorMessage = "The date on which a loan is rescheduled cannot be before the loan disbursement date: "
                    + getDisbursementDate().toString();
            throw new InvalidLoanStateTransitionException("close.reschedule", Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_SUBMITTAL_DATE,
                    errorMessage, this.rescheduledOnDate, getDisbursementDate());
        }

        if (DateUtils.isDateInTheFuture(this.rescheduledOnDate)) {
            final String errorMessage = "The date on which a loan is rescheduled cannot be in the future.";
            throw new InvalidLoanStateTransitionException("close.reschedule", Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage,
                    this.rescheduledOnDate);
        }
    }

    public boolean isNotSubmittedAndPendingApproval() {
        return !isSubmittedAndPendingApproval();
    }

    public LoanStatus getStatus() {
        return LoanStatus.fromInt(this.loanStatus);
    }

    public Integer getPlainStatus() {
        return this.loanStatus;
    }

    public boolean isSubmittedAndPendingApproval() {
        return getStatus().isSubmittedAndPendingApproval();
    }

    public boolean isApproved() {
        return getStatus().isApproved();
    }

    private boolean isNotDisbursed() {
        return !isDisbursed();
    }

    public boolean isChargesAdditionAllowed() {
        boolean isDisbursed = false;
        if (this.loanProduct.isMultiDisburseLoan()) {
            isDisbursed = !isDisbursementAllowed();
        } else {
            isDisbursed = hasDisbursementTransaction();
        }
        return isDisbursed;
    }

    public boolean isDisbursed() {
        return hasDisbursementTransaction();
    }

    public boolean isClosed() {
        return getStatus().isClosed() || isCancelled();
    }

    public boolean isClosedAndNotCancelled() {
        return getStatus().isClosed();
    }

    private boolean isClosedObligationsMet() {
        return getStatus().isClosedObligationsMet();
    }

    public boolean isClosedWrittenOff() {
        return getStatus().isClosedWrittenOff();
    }

    private boolean isClosedWithOutsandingAmountMarkedForReschedule() {
        return getStatus().isClosedWithOutsandingAmountMarkedForReschedule();
    }

    private boolean isCancelled() {
        return isRejected() || isWithdrawn();
    }

    private boolean isWithdrawn() {
        return getStatus().isWithdrawnByClient();
    }

    private boolean isRejected() {
        return getStatus().isRejected();
    }

    public boolean isOpen() {
        return getStatus().isActive();
    }

    private boolean isAllTranchesNotDisbursed() {
        LoanStatus actualLoanStatus = LoanStatus.fromInt(this.loanStatus);
        return this.loanProduct.isMultiDisburseLoan() && (actualLoanStatus.isActive() || actualLoanStatus.isApproved()
                || actualLoanStatus.isClosedObligationsMet() || actualLoanStatus.isOverpaid()) && isDisbursementAllowed();
    }

    private boolean hasDisbursementTransaction() {
        boolean hasRepaymentTransaction = false;
        for (final LoanTransaction loanTransaction : this.loanTransactions) {
            if (loanTransaction.isDisbursement() && loanTransaction.isNotReversed()) {
                hasRepaymentTransaction = true;
                break;
            }
        }
        return hasRepaymentTransaction;
    }

    public boolean isSubmittedOnDateAfter(final LocalDate compareDate) {
        return DateUtils.isAfter(this.submittedOnDate, compareDate);
    }

    public LocalDate getSubmittedOnDate() {
        return this.submittedOnDate;
    }

    public LocalDate getApprovedOnDate() {
        return this.approvedOnDate;
    }

    public LocalDate getExpectedDisbursedOnLocalDate() {
        return this.expectedDisbursementDate;
    }

    public LocalDate getDisbursementDate() {
        LocalDate disbursementDate = getExpectedDisbursedOnLocalDate();
        if (this.actualDisbursementDate != null) {
            disbursementDate = this.actualDisbursementDate;
        }
        return disbursementDate;
    }

    public LocalDate getDisburseDonDate() {
        return this.actualDisbursementDate;
    }

    public void setActualDisbursementDate(LocalDate actualDisbursementDate) {
        this.actualDisbursementDate = actualDisbursementDate;
    }

    public LocalDate getWrittenOffDate() {
        return this.writtenOffOnDate;
    }

    public LocalDate getExpectedDisbursedOnLocalDateForTemplate() {
        LocalDate expectedDisbursementDateV2 = null;
        if (this.expectedDisbursementDate != null) {
            expectedDisbursementDateV2 = this.expectedDisbursementDate;
        }

        Collection<LoanDisbursementDetails> details = fetchUndisbursedDetail();
        if (!details.isEmpty()) {
            for (LoanDisbursementDetails disbursementDetailsV2 : details) {
                expectedDisbursementDateV2 = disbursementDetailsV2.expectedDisbursementDate();
            }
        }
        return expectedDisbursementDateV2;
    }

    public BigDecimal getDisburseAmountForTemplate() {
        BigDecimal principal = this.loanRepaymentScheduleDetail.getPrincipal().getAmount();
        Collection<LoanDisbursementDetails> details = fetchUndisbursedDetail();
        if (!details.isEmpty()) {
            principal = BigDecimal.ZERO;
            for (LoanDisbursementDetails disbursementDetailsV2 : details) {
                principal = principal.add(disbursementDetailsV2.principal());
            }
        }
        return principal;
    }

    public LocalDate getExpectedFirstRepaymentOnDate() {
        return this.expectedFirstRepaymentOnDate;
    }

    private boolean isActualDisbursedOnDateEarlierOrLaterThanExpected(final LocalDate actualDisbursedOnDate) {
        boolean isRegenerationRequired = false;
        if (this.loanProduct.isMultiDisburseLoan()) {
            LoanDisbursementDetails details = fetchLastDisburseDetail();
            if (details != null && !DateUtils.isEqual(details.expectedDisbursementDate(), details.actualDisbursementDate())) {
                isRegenerationRequired = true;
            }
        }
        return isRegenerationRequired || !DateUtils.isEqual(actualDisbursedOnDate, this.expectedDisbursementDate);
    }

    private boolean isRepaymentScheduleRegenerationRequiredForDisbursement(final LocalDate actualDisbursementDate) {
        return isActualDisbursedOnDateEarlierOrLaterThanExpected(actualDisbursementDate);
    }

    private Money getTotalPaidInRepayments() {
        Money cumulativePaid = Money.zero(loanCurrency());

        for (final LoanTransaction repayment : this.loanTransactions) {
            if (repayment.isRepaymentLikeType() && !repayment.isReversed()) {
                cumulativePaid = cumulativePaid.plus(repayment.getAmount(loanCurrency()));
            }
        }

        return cumulativePaid;
    }

    public Money getTotalRecoveredPayments() {
        Money cumulativePaid = Money.zero(loanCurrency());

        for (final LoanTransaction recoveredPayment : this.loanTransactions) {
            if (recoveredPayment.isRecoveryRepayment()) {
                cumulativePaid = cumulativePaid.plus(recoveredPayment.getAmount(loanCurrency()));
            }
        }
        return cumulativePaid;
    }

    private Money getTotalInterestOutstandingOnLoan() {
        Money cumulativeInterest = Money.zero(loanCurrency());

        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment scheduledRepayment : installments) {
            cumulativeInterest = cumulativeInterest.plus(scheduledRepayment.getInterestOutstanding(loanCurrency()));
        }

        return cumulativeInterest;
    }

    @SuppressWarnings("unused")
    private Money getTotalInterestOverdueOnLoan() {
        Money cumulativeInterestOverdue = Money.zero(this.loanRepaymentScheduleDetail.getPrincipal().getCurrency());
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment scheduledRepayment : installments) {

            final Money interestOutstandingForPeriod = scheduledRepayment.getInterestOutstanding(loanCurrency());
            if (scheduledRepayment.isOverdueOn(DateUtils.getBusinessLocalDate())) {
                cumulativeInterestOverdue = cumulativeInterestOverdue.plus(interestOutstandingForPeriod);
            }
        }

        return cumulativeInterestOverdue;
    }

    private Money getInArrearsTolerance() {
        return this.loanRepaymentScheduleDetail.getInArrearsTolerance();
    }

    public boolean hasIdentifyOf(final Long loanId) {
        return loanId.equals(getId());
    }

    public boolean hasLoanOfficer(final Staff fromLoanOfficer) {

        boolean matchesCurrentLoanOfficer = false;
        if (this.loanOfficer != null) {
            matchesCurrentLoanOfficer = this.loanOfficer.identifiedBy(fromLoanOfficer);
        } else {
            matchesCurrentLoanOfficer = fromLoanOfficer == null;
        }

        return matchesCurrentLoanOfficer;
    }

    public LocalDate getInterestChargedFromDate() {
        return this.interestChargedFromDate;
    }

    public Money getPrincipal() {
        return this.loanRepaymentScheduleDetail.getPrincipal();
    }

    public boolean hasCurrencyCodeOf(final String matchingCurrencyCode) {
        return getCurrencyCode().equalsIgnoreCase(matchingCurrencyCode);
    }

    public String getCurrencyCode() {
        return this.loanRepaymentScheduleDetail.getPrincipal().getCurrencyCode();
    }

    public MonetaryCurrency getCurrency() {
        return this.loanRepaymentScheduleDetail.getCurrency();
    }

    public void reassignLoanOfficer(final Staff newLoanOfficer, final LocalDate assignmentDate) {
        final LoanOfficerAssignmentHistory latestHistoryRecord = findLatestIncompleteHistoryRecord();
        final LoanOfficerAssignmentHistory lastAssignmentRecord = findLastAssignmentHistoryRecord(newLoanOfficer);

        // assignment date should not be less than loan submitted date
        if (isSubmittedOnDateAfter(assignmentDate)) {
            final String errorMessage = "The Loan Officer assignment date ( " + assignmentDate.toString()
                    + ") cannot be before loan submitted date (" + getSubmittedOnDate().toString() + ").";
            throw new LoanOfficerAssignmentDateException("cannot.be.before.loan.submittal.date", errorMessage, assignmentDate,
                    getSubmittedOnDate());
        } else if (lastAssignmentRecord != null && lastAssignmentRecord.isEndDateAfter(assignmentDate)) {
            final String errorMessage = "The Loan Officer assignment date (" + assignmentDate
                    + ") cannot be before previous Loan Officer unassigned date (" + lastAssignmentRecord.getEndDate() + ").";
            throw new LoanOfficerAssignmentDateException("cannot.be.before.previous.unassignement.date", errorMessage, assignmentDate,
                    lastAssignmentRecord.getEndDate());
        } else if (DateUtils.isDateInTheFuture(assignmentDate)) {
            final String errorMessage = "The Loan Officer assignment date ( " + assignmentDate + ") cannot be in the future.";
            throw new LoanOfficerAssignmentDateException(Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage, assignmentDate);
        } else if (latestHistoryRecord != null && this.loanOfficer.identifiedBy(newLoanOfficer)) {
            latestHistoryRecord.updateStartDate(assignmentDate);
        } else if (latestHistoryRecord != null && latestHistoryRecord.matchesStartDateOf(assignmentDate)) {
            latestHistoryRecord.updateLoanOfficer(newLoanOfficer);
            this.loanOfficer = newLoanOfficer;
        } else if (latestHistoryRecord != null && latestHistoryRecord.isBeforeStartDate(assignmentDate)) {
            final String errorMessage = "Loan with identifier " + getId() + " was already assigned before date " + assignmentDate;
            throw new LoanOfficerAssignmentDateException("is.before.last.assignment.date", errorMessage, getId(), assignmentDate);
        } else {
            if (latestHistoryRecord != null) {
                // loan officer correctly changed from previous loan officer to
                // new loan officer
                latestHistoryRecord.updateEndDate(assignmentDate);
            }

            this.loanOfficer = newLoanOfficer;
            if (isNotSubmittedAndPendingApproval()) {
                final LoanOfficerAssignmentHistory loanOfficerAssignmentHistory = LoanOfficerAssignmentHistory.createNew(this,
                        this.loanOfficer, assignmentDate);
                this.loanOfficerHistory.add(loanOfficerAssignmentHistory);
            }
        }
    }

    public void removeLoanOfficer(final LocalDate unassignDate) {

        final LoanOfficerAssignmentHistory latestHistoryRecord = findLatestIncompleteHistoryRecord();

        if (latestHistoryRecord != null) {
            validateUnassignDate(latestHistoryRecord, unassignDate);
            latestHistoryRecord.updateEndDate(unassignDate);
        }

        this.loanOfficer = null;
    }

    private void validateUnassignDate(final LoanOfficerAssignmentHistory latestHistoryRecord, final LocalDate unassignDate) {
        if (DateUtils.isAfter(latestHistoryRecord.getStartDate(), unassignDate)) {
            final String errorMessage = "The Loan officer Unassign date(" + unassignDate + ") cannot be before its assignment date ("
                    + latestHistoryRecord.getStartDate() + ").";
            throw new LoanOfficerUnassignmentDateException("cannot.be.before.assignment.date", errorMessage, getId(),
                    getLoanOfficer().getId(), latestHistoryRecord.getStartDate(), unassignDate);
        } else if (DateUtils.isDateInTheFuture(unassignDate)) {
            final String errorMessage = "The Loan Officer Unassign date (" + unassignDate + ") cannot be in the future.";
            throw new LoanOfficerUnassignmentDateException(Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage, unassignDate);
        }
    }

    private LoanOfficerAssignmentHistory findLatestIncompleteHistoryRecord() {
        LoanOfficerAssignmentHistory latestRecordWithNoEndDate = null;
        for (final LoanOfficerAssignmentHistory historyRecord : this.loanOfficerHistory) {
            if (historyRecord.isCurrentRecord()) {
                latestRecordWithNoEndDate = historyRecord;
                break;
            }
        }
        return latestRecordWithNoEndDate;
    }

    private LoanOfficerAssignmentHistory findLastAssignmentHistoryRecord(final Staff newLoanOfficer) {
        LoanOfficerAssignmentHistory lastAssignmentRecordLatestEndDate = null;
        for (final LoanOfficerAssignmentHistory historyRecord : this.loanOfficerHistory) {
            if (historyRecord.isCurrentRecord() && !historyRecord.isSameLoanOfficer(newLoanOfficer)) {
                lastAssignmentRecordLatestEndDate = historyRecord;
                break;
            }

            if (lastAssignmentRecordLatestEndDate == null || (historyRecord.isEndDateAfter(lastAssignmentRecordLatestEndDate.getEndDate())
                    && !historyRecord.isSameLoanOfficer(newLoanOfficer))) {
                lastAssignmentRecordLatestEndDate = historyRecord;
            }
        }
        return lastAssignmentRecordLatestEndDate;
    }

    public Long getClientId() {
        Long clientId = null;
        if (this.client != null) {
            clientId = this.client.getId();
        }
        return clientId;
    }

    public Long getGroupId() {
        Long groupId = null;
        if (this.group != null) {
            groupId = this.group.getId();
        }
        return groupId;
    }

    public Long getGlimId() {
        Long glimId = null;
        if (this.glim != null) {
            glimId = this.glim.getId();
        }
        return glimId;
    }

    public Long getOfficeId() {
        Long officeId = null;
        if (this.client != null) {
            officeId = this.client.officeId();
        } else {
            officeId = this.group.officeId();
        }
        return officeId;
    }

    public Office getOffice() {
        Office office = null;
        if (this.client != null) {
            office = this.client.getOffice();
        } else {
            office = this.group.getOffice();
        }
        return office;
    }

    private Boolean isCashBasedAccountingEnabledOnLoanProduct() {
        return this.loanProduct.isCashBasedAccountingEnabled();
    }

    public Boolean isUpfrontAccrualAccountingEnabledOnLoanProduct() {
        return this.loanProduct.isUpfrontAccrualAccountingEnabled();
    }

    public Boolean isAccountingDisabledOnLoanProduct() {
        return this.loanProduct.isAccountingDisabled();
    }

    public Boolean isNoneOrCashOrUpfrontAccrualAccountingEnabledOnLoanProduct() {
        return isCashBasedAccountingEnabledOnLoanProduct() || isUpfrontAccrualAccountingEnabledOnLoanProduct()
                || isAccountingDisabledOnLoanProduct();
    }

    public Boolean isPeriodicAccrualAccountingEnabledOnLoanProduct() {
        return this.loanProduct.isPeriodicAccrualAccountingEnabled();
    }

    public Long productId() {
        return this.loanProduct.getId();
    }

    public Staff getLoanOfficer() {
        return this.loanOfficer;
    }

    public Set<LoanCollateral> getCollateral() {
        return this.collateral;
    }

    public BigDecimal getProposedPrincipal() {
        return this.proposedPrincipal;
    }

    public List<Map<String, Object>> deriveAccountingBridgeDataForChargeOff(final String currencyCode,
            final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds, boolean isAccountTransfer) {

        final List<Map<String, Object>> accountingBridgeData = new ArrayList<>();
        final List<Map<String, Object>> newLoanTransactionsBeforeChargeOff = new ArrayList<>();
        final List<Map<String, Object>> newLoanTransactionsAfterChargeOff = new ArrayList<>();
        // get map before charge-off
        final Map<String, Object> accountingBridgeDataBeforeChargeOff = buildAccountingMapForChargeOffDateCriteria(currencyCode,
                isAccountTransfer, true);
        // get map after charge-off
        final Map<String, Object> accountingBridgeDataAfterChargeOff = buildAccountingMapForChargeOffDateCriteria(currencyCode,
                isAccountTransfer, false);

        // split the transactions according charge-off date
        classifyTransactionsBasedOnChargeOffDate(newLoanTransactionsBeforeChargeOff, newLoanTransactionsAfterChargeOff,
                existingTransactionIds, existingReversedTransactionIds, currencyCode);

        accountingBridgeDataBeforeChargeOff.put(Loan.NEW_LOAN_TRANSACTIONS_PARAM, newLoanTransactionsBeforeChargeOff);
        accountingBridgeData.add(accountingBridgeDataBeforeChargeOff);

        accountingBridgeDataAfterChargeOff.put(Loan.NEW_LOAN_TRANSACTIONS_PARAM, newLoanTransactionsAfterChargeOff);
        accountingBridgeData.add(accountingBridgeDataAfterChargeOff);

        return accountingBridgeData;
    }

    private void classifyTransactionsBasedOnChargeOffDate(List<Map<String, Object>> newLoanTransactionsBeforeChargeOff,
            List<Map<String, Object>> newLoanTransactionsAfterChargeOff, List<Long> existingTransactionIds,
            List<Long> existingReversedTransactionIds, String currencyCode) {
        // Before
        filterTransactionsByChargeOffDate(newLoanTransactionsBeforeChargeOff, currencyCode, existingTransactionIds,
                existingReversedTransactionIds, transaction -> DateUtils.isBefore(transaction.getTransactionDate(), getChargedOffOnDate()));
        // On
        filterTransactionsByChargeOffDate(newLoanTransactionsBeforeChargeOff, newLoanTransactionsAfterChargeOff, currencyCode,
                existingTransactionIds, existingReversedTransactionIds,
                transaction -> DateUtils.isEqual(transaction.getTransactionDate(), getChargedOffOnDate()));
        // After
        filterTransactionsByChargeOffDate(newLoanTransactionsAfterChargeOff, currencyCode, existingTransactionIds,
                existingReversedTransactionIds, transaction -> DateUtils.isAfter(transaction.getTransactionDate(), getChargedOffOnDate()));
    }

    private Map<String, Object> getAccountingBridgeDataGenericAttributes(final String currencyCode, boolean isAccountTransfer) {
        final Map<String, Object> accountingBridgeDataGenericAttributes = new LinkedHashMap<>();
        accountingBridgeDataGenericAttributes.put("loanId", getId());
        accountingBridgeDataGenericAttributes.put("loanProductId", productId());
        accountingBridgeDataGenericAttributes.put("officeId", getOfficeId());
        accountingBridgeDataGenericAttributes.put("currencyCode", currencyCode);
        accountingBridgeDataGenericAttributes.put("calculatedInterest", this.summary.getTotalInterestCharged());
        accountingBridgeDataGenericAttributes.put("cashBasedAccountingEnabled", isCashBasedAccountingEnabledOnLoanProduct());
        accountingBridgeDataGenericAttributes.put("upfrontAccrualBasedAccountingEnabled", isUpfrontAccrualAccountingEnabledOnLoanProduct());
        accountingBridgeDataGenericAttributes.put("periodicAccrualBasedAccountingEnabled",
                isPeriodicAccrualAccountingEnabledOnLoanProduct());
        accountingBridgeDataGenericAttributes.put("isAccountTransfer", isAccountTransfer);
        return accountingBridgeDataGenericAttributes;
    }

    private Map<String, Object> buildAccountingMapForChargeOffDateCriteria(final String currencyCode, boolean isAccountTransfer,
            boolean isBeforeChargeOffDate) {
        final Map<String, Object> accountingBridgeDataChargeOff = new LinkedHashMap<>(
                getAccountingBridgeDataGenericAttributes(currencyCode, isAccountTransfer));
        if (isBeforeChargeOffDate) {
            accountingBridgeDataChargeOff.put(Loan.IS_CHARGE_OFF_PARAM, false);
            accountingBridgeDataChargeOff.put(Loan.IS_FRAUD_OFF_PARAM, false);
        } else {
            accountingBridgeDataChargeOff.put(Loan.IS_CHARGE_OFF_PARAM, isChargedOff());
            accountingBridgeDataChargeOff.put(Loan.IS_FRAUD_OFF_PARAM, isFraud());
        }
        return accountingBridgeDataChargeOff;
    }

    private void filterTransactionsByChargeOffDate(List<Map<String, Object>> filteredTransactions, final String currencyCode,
            final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds,
            Predicate<LoanTransaction> chargeOffDateCriteria) {
        filteredTransactions.addAll(this.loanTransactions.stream().filter(chargeOffDateCriteria).filter(transaction -> {
            boolean isExistingTransaction = existingTransactionIds.contains(transaction.getId());
            boolean isExistingReversedTransaction = existingReversedTransactionIds.contains(transaction.getId());

            if (transaction.isReversed() && isExistingTransaction && !isExistingReversedTransaction) {
                return true;
            } else {
                return !isExistingTransaction;
            }
        }).map(transaction -> transaction.toMapData(currencyCode)).toList());
    }

    private void filterTransactionsByChargeOffDate(List<Map<String, Object>> newLoanTransactionsBeforeChargeOff,
            List<Map<String, Object>> newLoanTransactionsAfterChargeOff, String currencyCode, List<Long> existingTransactionIds,
            List<Long> existingReversedTransactionIds, Predicate<LoanTransaction> chargeOffDateCriteria) {

        final Optional<LoanTransaction> chargeOffTransactionOptional = this.loanTransactions.stream().filter(LoanTransaction::isChargeOff)
                .filter(LoanTransaction::isNotReversed).findFirst();
        if (chargeOffTransactionOptional.isPresent()) {
            final LoanTransaction chargeOffTransaction = chargeOffTransactionOptional.get();
            this.loanTransactions.stream().filter(chargeOffDateCriteria).forEach(transaction -> {
                boolean isExistingTransaction = existingTransactionIds.contains(transaction.getId());
                boolean isExistingReversedTransaction = existingReversedTransactionIds.contains(transaction.getId());
                List<Map<String, Object>> targetList = (transaction.getId().compareTo(chargeOffTransaction.getId()) < 0)
                        ? newLoanTransactionsBeforeChargeOff
                        : newLoanTransactionsAfterChargeOff;
                if ((transaction.isReversed() && isExistingTransaction && !isExistingReversedTransaction) || !isExistingTransaction) {
                    targetList.add(transaction.toMapData(currencyCode));
                }
            });
        }
    }

    public Map<String, Object> deriveAccountingBridgeData(final String currencyCode, final List<Long> existingTransactionIds,
            final List<Long> existingReversedTransactionIds, boolean isAccountTransfer) {

        final Map<String, Object> accountingBridgeData = new LinkedHashMap<>();
        accountingBridgeData.put("loanId", getId());
        accountingBridgeData.put("loanProductId", productId());
        accountingBridgeData.put("officeId", getOfficeId());
        accountingBridgeData.put("currencyCode", currencyCode);
        accountingBridgeData.put("calculatedInterest", this.summary.getTotalInterestCharged());
        accountingBridgeData.put("cashBasedAccountingEnabled", isCashBasedAccountingEnabledOnLoanProduct());
        accountingBridgeData.put("upfrontAccrualBasedAccountingEnabled", isUpfrontAccrualAccountingEnabledOnLoanProduct());
        accountingBridgeData.put("periodicAccrualBasedAccountingEnabled", isPeriodicAccrualAccountingEnabledOnLoanProduct());
        accountingBridgeData.put("isAccountTransfer", isAccountTransfer);
        accountingBridgeData.put(Loan.IS_CHARGE_OFF_PARAM, isChargedOff());
        accountingBridgeData.put(Loan.IS_FRAUD_OFF_PARAM, isFraud());

        final List<Map<String, Object>> newLoanTransactions = new ArrayList<>();
        for (final LoanTransaction transaction : this.loanTransactions) {
            if ((transaction.isReversed() && existingTransactionIds.contains(transaction.getId())
                    && !existingReversedTransactionIds.contains(transaction.getId()))
                    || !existingTransactionIds.contains(transaction.getId())) {
                newLoanTransactions.add(transaction.toMapData(currencyCode));
            }
        }

        accountingBridgeData.put(Loan.NEW_LOAN_TRANSACTIONS_PARAM, newLoanTransactions);
        return accountingBridgeData;
    }

    public Money getReceivableInterest(final LocalDate tillDate) {
        Money receivableInterest = Money.zero(getCurrency());
        for (final LoanTransaction transaction : this.loanTransactions) {
            if (transaction.isNotReversed() && !transaction.isRepaymentAtDisbursement() && !transaction.isDisbursement()
                    && !DateUtils.isAfter(transaction.getTransactionDate(), tillDate)) {
                if (transaction.isAccrual()) {
                    receivableInterest = receivableInterest.plus(transaction.getInterestPortion(getCurrency()));
                } else if (transaction.isRepaymentLikeType() || transaction.isInterestWaiver()) {
                    receivableInterest = receivableInterest.minus(transaction.getInterestPortion(getCurrency()));
                }
            }
            if (receivableInterest.isLessThanZero()) {
                receivableInterest = receivableInterest.zero();
            }
        }
        return receivableInterest;
    }

    public void setHelpers(final LoanLifecycleStateMachine loanLifecycleStateMachine, final LoanSummaryWrapper loanSummaryWrapper,
            final LoanRepaymentScheduleTransactionProcessorFactory transactionProcessorFactory) {
        this.loanLifecycleStateMachine = loanLifecycleStateMachine;
        this.loanSummaryWrapper = loanSummaryWrapper;
        this.transactionProcessorFactory = transactionProcessorFactory;
    }

    public boolean isSyncDisbursementWithMeeting() {
        return this.syncDisbursementWithMeeting != null && this.syncDisbursementWithMeeting;
    }

    public LocalDate getClosedOnDate() {
        return this.closedOnDate;
    }

    @SuppressWarnings({ "squid:S3776" })
    public void updateLoanRepaymentScheduleDates(final String recurringRule, final boolean isHolidayEnabled, final List<Holiday> holidays,
            final WorkingDays workingDays, final LocalDate presentMeetingDate, final LocalDate newMeetingDate,
            final boolean isSkipRepaymentOnFirstDayOfMonth, final Integer numberOfDays) {
        // first repayment's from date is same as disbursement date.
        // meetingStartDate is used as seedDate Capture the seedDate from user and use the seedDate as meetingStart date

        LocalDate tmpFromDate = getDisbursementDate();
        final PeriodFrequencyType repaymentPeriodFrequencyType = this.loanRepaymentScheduleDetail.getRepaymentPeriodFrequencyType();
        final Integer loanRepaymentInterval = this.loanRepaymentScheduleDetail.getRepayEvery();
        final String frequency = CalendarUtils.getMeetingFrequencyFromPeriodFrequencyType(repaymentPeriodFrequencyType);

        LocalDate newRepaymentDate = null;
        boolean isFirstTime = true;
        LocalDate latestRepaymentDate = null;
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : installments) {
            LocalDate oldDueDate = loanRepaymentScheduleInstallment.getDueDate();
            if (!DateUtils.isBefore(oldDueDate, presentMeetingDate)) {
                if (isFirstTime) {
                    isFirstTime = false;
                    newRepaymentDate = newMeetingDate;
                } else {
                    // tmpFromDate.plusDays(1) is done to make sure
                    // getNewRepaymentMeetingDate method returns next meeting
                    // date and not the same as tmpFromDate
                    newRepaymentDate = CalendarUtils.getNewRepaymentMeetingDate(recurringRule, tmpFromDate, tmpFromDate.plusDays(1),
                            loanRepaymentInterval, frequency, workingDays, isSkipRepaymentOnFirstDayOfMonth, numberOfDays);
                }
                if (isHolidayEnabled) {
                    newRepaymentDate = HolidayUtil.getRepaymentRescheduleDateToIfHoliday(newRepaymentDate, holidays);
                }
                if (DateUtils.isBefore(latestRepaymentDate, newRepaymentDate)) {
                    latestRepaymentDate = newRepaymentDate;
                }
                loanRepaymentScheduleInstallment.updateDueDate(newRepaymentDate);
                // reset from date to get actual daysInPeriod

                loanRepaymentScheduleInstallment.updateFromDate(tmpFromDate);

                tmpFromDate = newRepaymentDate;// update with new repayment
                // date
            } else {
                tmpFromDate = oldDueDate;
            }
        }
        if (latestRepaymentDate != null) {
            this.expectedMaturityDate = latestRepaymentDate;
        }
    }

    public void updateLoanRepaymentScheduleDates(final LocalDate meetingStartDate, final String recuringRule,
            final boolean isHolidayEnabled, final List<Holiday> holidays, final WorkingDays workingDays,
            final boolean isSkipRepaymentonfirstdayofmonth, final Integer numberofDays) {
        // first repayment's from date is same as disbursement date.
        LocalDate tmpFromDate = getDisbursementDate();
        final PeriodFrequencyType repaymentPeriodFrequencyType = this.loanRepaymentScheduleDetail.getRepaymentPeriodFrequencyType();
        final Integer loanRepaymentInterval = this.loanRepaymentScheduleDetail.getRepayEvery();
        final String frequency = CalendarUtils.getMeetingFrequencyFromPeriodFrequencyType(repaymentPeriodFrequencyType);

        LocalDate newRepaymentDate = null;
        LocalDate seedDate = meetingStartDate;
        LocalDate latestRepaymentDate = null;
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : installments) {
            LocalDate oldDueDate = loanRepaymentScheduleInstallment.getDueDate();
            if (DateUtils.isAfter(oldDueDate, seedDate) && DateUtils.isDateInTheFuture(oldDueDate)) {
                newRepaymentDate = CalendarUtils.getNewRepaymentMeetingDate(recuringRule, seedDate, oldDueDate, loanRepaymentInterval,
                        frequency, workingDays, isSkipRepaymentonfirstdayofmonth, numberofDays);

                final LocalDate maxDateLimitForNewRepayment = getMaxDateLimitForNewRepayment(repaymentPeriodFrequencyType,
                        loanRepaymentInterval, tmpFromDate);

                if (DateUtils.isAfter(newRepaymentDate, maxDateLimitForNewRepayment)) {
                    newRepaymentDate = CalendarUtils.getNextRepaymentMeetingDate(recuringRule, seedDate, tmpFromDate, loanRepaymentInterval,
                            frequency, workingDays, isSkipRepaymentonfirstdayofmonth, numberofDays);
                }

                if (isHolidayEnabled) {
                    newRepaymentDate = HolidayUtil.getRepaymentRescheduleDateToIfHoliday(newRepaymentDate, holidays);
                }
                if (DateUtils.isBefore(latestRepaymentDate, newRepaymentDate)) {
                    latestRepaymentDate = newRepaymentDate;
                }

                loanRepaymentScheduleInstallment.updateDueDate(newRepaymentDate);
                // reset from date to get actual daysInPeriod
                loanRepaymentScheduleInstallment.updateFromDate(tmpFromDate);
                tmpFromDate = newRepaymentDate;// update with new repayment
                // date
            } else {
                tmpFromDate = oldDueDate;
            }
        }
        if (latestRepaymentDate != null) {
            this.expectedMaturityDate = latestRepaymentDate;
        }
    }

    private LocalDate getMaxDateLimitForNewRepayment(final PeriodFrequencyType periodFrequencyType, final Integer loanRepaymentInterval,
            final LocalDate startDate) {
        LocalDate dueRepaymentPeriodDate = startDate;
        final Integer repaidEvery = 2 * loanRepaymentInterval;
        switch (periodFrequencyType) {
            case DAYS:
                dueRepaymentPeriodDate = startDate.plusDays(repaidEvery);
            break;
            case WEEKS:
                dueRepaymentPeriodDate = startDate.plusWeeks(repaidEvery);
            break;
            case MONTHS:
                dueRepaymentPeriodDate = startDate.plusMonths(repaidEvery);
            break;
            case YEARS:
                dueRepaymentPeriodDate = startDate.plusYears(repaidEvery);
            break;
            case INVALID:
            break;
            case WHOLE_TERM:
            break;
        }
        return dueRepaymentPeriodDate.minusDays(1);// get 2n-1 range date from
                                                   // startDate
    }

    private void validateDisbursementDateIsOnNonWorkingDay(final WorkingDays workingDays, final boolean allowTransactionsOnNonWorkingDay) {
        if (!allowTransactionsOnNonWorkingDay && !WorkingDaysUtil.isWorkingDay(workingDays, getDisbursementDate())) {
            final String errorMessage = "Expected disbursement date cannot be on a non working day";
            throw new LoanApplicationDateException("disbursement.date.on.non.working.day", errorMessage, getExpectedDisbursedOnLocalDate());
        }
    }

    private void validateDisbursementDateIsOnHoliday(final boolean allowTransactionsOnHoliday, final List<Holiday> holidays) {
        if (!allowTransactionsOnHoliday && HolidayUtil.isHoliday(getDisbursementDate(), holidays)) {
            final String errorMessage = "Expected disbursement date cannot be on a holiday";
            throw new LoanApplicationDateException("disbursement.date.on.holiday", errorMessage, getExpectedDisbursedOnLocalDate());
        }
    }

    public void validateRepaymentDateIsOnNonWorkingDay(final LocalDate repaymentDate, final WorkingDays workingDays,
            final boolean allowTransactionsOnNonWorkingDay) {
        if (!allowTransactionsOnNonWorkingDay && !WorkingDaysUtil.isWorkingDay(workingDays, repaymentDate)) {
            final String errorMessage = "Repayment date cannot be on a non working day";
            throw new LoanApplicationDateException("repayment.date.on.non.working.day", errorMessage, repaymentDate);
        }
    }

    public void validateRepaymentDateIsOnHoliday(final LocalDate repaymentDate, final boolean allowTransactionsOnHoliday,
            final List<Holiday> holidays) {
        if (!allowTransactionsOnHoliday && HolidayUtil.isHoliday(repaymentDate, holidays)) {
            final String errorMessage = "Repayment date cannot be on a holiday";
            throw new LoanApplicationDateException("repayment.date.on.holiday", errorMessage, repaymentDate);
        }
    }

    public Group group() {
        return this.group;
    }

    public void updateGroup(final Group newGroup) {
        this.group = newGroup;
    }

    public Integer getCurrentLoanCounter() {
        return this.loanCounter;
    }

    public Integer getLoanProductLoanCounter() {
        if (this.loanProductCounter == null) {
            return 0;
        }
        return this.loanProductCounter;
    }

    public void updateClientLoanCounter(final Integer newLoanCounter) {
        this.loanCounter = newLoanCounter;
    }

    public void updateLoanProductLoanCounter(final Integer newLoanProductLoanCounter) {
        this.loanProductCounter = newLoanProductLoanCounter;
    }

    public boolean isGroupLoan() {
        return AccountType.fromInt(this.loanType).isGroupAccount();
    }

    public boolean isJLGLoan() {
        return AccountType.fromInt(this.loanType).isJLGAccount();
    }

    public void updateInterestRateFrequencyType() {
        this.loanRepaymentScheduleDetail.updateInterestPeriodFrequencyType(this.loanProduct.getInterestPeriodFrequencyType());
    }

    public Integer getTermFrequency() {
        return this.termFrequency;
    }

    public Integer getTermPeriodFrequencyType() {
        return this.termPeriodFrequencyType;
    }

    // This method returns copy of all transactions
    public List<LoanTransaction> getLoanTransactions() {
        return this.loanTransactions;
    }

    public void addLoanTransaction(final LoanTransaction loanTransaction) {
        this.loanTransactions.add(loanTransaction);
    }

    public void removeLoanTransaction(final LoanTransaction loanTransaction) {
        this.loanTransactions.remove(loanTransaction);
    }

    // Intentionally kept as package-private. Nobody should set the status directly but use the
    // DefaultLoanLifecycleStateMachine to transition
    void setLoanStatus(final Integer loanStatus) {
        this.loanStatus = loanStatus;
    }

    public void validateExpectedDisbursementForHolidayAndNonWorkingDay(final WorkingDays workingDays,
            final boolean allowTransactionsOnHoliday, final List<Holiday> holidays, final boolean allowTransactionsOnNonWorkingDay) {
        // validate if disbursement date is a holiday or a non-working day
        validateDisbursementDateIsOnNonWorkingDay(workingDays, allowTransactionsOnNonWorkingDay);
        validateDisbursementDateIsOnHoliday(allowTransactionsOnHoliday, holidays);

    }

    private void validateActivityNotBeforeClientOrGroupTransferDate(final LoanEvent event, final LocalDate activityDate) {
        if (this.client != null && this.client.getOfficeJoiningDate() != null) {
            final LocalDate clientOfficeJoiningDate = this.client.getOfficeJoiningDate();
            if (DateUtils.isBefore(activityDate, clientOfficeJoiningDate)) {
                String errorMessage = null;
                String action = null;
                String postfix = null;
                switch (event) {
                    case LOAN_CREATED:
                        errorMessage = "The date on which a loan is submitted cannot be earlier than client's transfer date to this office";
                        action = Loan.SUBMITTAL_PARAM;
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    case LOAN_APPROVED:
                        errorMessage = "The date on which a loan is approved cannot be earlier than client's transfer date to this office";
                        action = Loan.APPROVAL_PARAM;
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    case LOAN_APPROVAL_UNDO:
                        errorMessage = "The date on which a loan is approved cannot be earlier than client's transfer date to this office";
                        action = Loan.APPROVAL_PARAM;
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_UNDONE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    case LOAN_DISBURSED:
                        errorMessage = "The date on which a loan is disbursed cannot be earlier than client's transfer date to this office";
                        action = Loan.DISBURSAL_PARAM;
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    case LOAN_DISBURSAL_UNDO:
                        errorMessage = "Cannot undo a disbursal done in another branch";
                        action = Loan.DISBURSAL_PARAM;
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_UNDONE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    case LOAN_REPAYMENT_OR_WAIVER:
                        errorMessage = "The date on which a repayment or waiver is made cannot be earlier than client's transfer date to this office";
                        action = "repayment.or.waiver";
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_MADE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    case LOAN_REJECTED:
                        errorMessage = "The date on which a loan is rejected cannot be earlier than client's transfer date to this office";
                        action = Loan.REJECT_PARAM;
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    case LOAN_WITHDRAWN:
                        errorMessage = "The date on which a loan is withdrawn cannot be earlier than client's transfer date to this office";
                        action = Loan.WITHDRAW_PARAM;
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    case WRITE_OFF_OUTSTANDING:
                        errorMessage = "The date on which a write off is made cannot be earlier than client's transfer date to this office";
                        action = Loan.WRITE_OFF_PARAM;
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_UNDONE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    case REPAID_IN_FULL:
                        errorMessage = "The date on which the loan is repaid in full cannot be earlier than client's transfer date to this office";
                        action = Loan.CLOSE_PARAM;
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_UNDONE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    case LOAN_CHARGE_PAYMENT:
                        errorMessage = "The date on which a charge payment is made cannot be earlier than client's transfer date to this office";
                        action = "charge.payment";
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_MADE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    case LOAN_REFUND:
                        errorMessage = "The date on which a refund is made cannot be earlier than client's transfer date to this office";
                        action = "refund";
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_MADE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    case LOAN_DISBURSAL_UNDO_LAST:
                        errorMessage = "Cannot undo a last disbursal in another branch";
                        action = Loan.DISBURSAL_PARAM;
                        postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_UNDONE_BEFORE_CLIENT_TRANSFER_DATE;
                    break;
                    default:
                    break;
                }
                throw new InvalidLoanStateTransitionException(action, postfix, errorMessage, clientOfficeJoiningDate);
            }
        }
    }

    private void validateActivityNotBeforeLastTransactionDate(final LoanEvent event, final LocalDate activityDate) {
        if (!(this.repaymentScheduleDetail().isInterestRecalculationEnabled() || this.loanProduct().isHoldGuaranteeFundsEnabled())) {
            return;
        }
        LocalDate lastTransactionDate = getLastUserTransactionDate();
        if (DateUtils.isAfter(lastTransactionDate, activityDate)) {
            String errorMessage = null;
            String action = null;
            String postfix = null;
            switch (event) {
                case LOAN_REPAYMENT_OR_WAIVER:
                    errorMessage = "The date on which a repayment or waiver is made cannot be earlier than last transaction date";
                    action = "repayment.or.waiver";
                    postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_MADE_BEFORE_LAST_TRANSACTION_DATE;
                break;
                case WRITE_OFF_OUTSTANDING:
                    errorMessage = "The date on which a write off is made cannot be earlier than last transaction date";
                    action = Loan.WRITE_OFF_PARAM;
                    postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_MADE_BEFORE_LAST_TRANSACTION_DATE;
                break;
                case LOAN_CHARGE_PAYMENT:
                    errorMessage = "The date on which a charge payment is made cannot be earlier than last transaction date";
                    action = "charge.payment";
                    postfix = Loan.ERROR_MESSAGE_LABEL_CANNOT_BE_MADE_BEFORE_LAST_TRANSACTION_DATE;
                break;
                default:
                break;
            }
            throw new InvalidLoanStateTransitionException(action, postfix, errorMessage, lastTransactionDate);
        }
    }

    public void validateRepaymentTypeTransactionNotBeforeAChargeRefund(final LoanTransaction repaymentTransaction,
            final String reversedOrCreated) {
        if (repaymentTransaction.isRepaymentLikeType() && !repaymentTransaction.isChargeRefund()) {
            for (LoanTransaction txn : this.getLoanTransactions()) {
                if (txn.isChargeRefund() && DateUtils.isBefore(repaymentTransaction.getTransactionDate(), txn.getTransactionDate())) {
                    final String errorMessage = "loan.transaction.cant.be." + reversedOrCreated + ".because.later.charge.refund.exists";
                    final String details = "Loan Transaction: " + this.getId() + " Can't be " + reversedOrCreated
                            + " because a Later Charge Refund Exists.";
                    throw new LoanChargeRefundException(errorMessage, details);
                }
            }
        }
    }

    public LocalDate getLastUserTransactionDate() {
        LocalDate currentTransactionDate = getDisbursementDate();
        for (final LoanTransaction previousTransaction : this.loanTransactions) {
            if (!(previousTransaction.isReversed() || previousTransaction.isAccrual() || previousTransaction.isIncomePosting())
                    && DateUtils.isBefore(currentTransactionDate, previousTransaction.getTransactionDate())) {
                currentTransactionDate = previousTransaction.getTransactionDate();
            }
        }
        return currentTransactionDate;
    }

    public LocalDate getLastRepaymentDate() {
        LocalDate currentTransactionDate = getDisbursementDate();
        for (final LoanTransaction previousTransaction : this.loanTransactions) {
            if (previousTransaction.isRepaymentLikeType()
                    && DateUtils.isBefore(currentTransactionDate, previousTransaction.getTransactionDate())) {
                currentTransactionDate = previousTransaction.getTransactionDate();
            }
        }
        return currentTransactionDate;
    }

    public LoanTransaction getLastPaymentTransaction() {
        return loanTransactions.stream() //
                .filter(loanTransaction -> !loanTransaction.isReversed()) //
                .filter(LoanTransaction::isRepaymentLikeType) //
                .reduce((first, second) -> second) //
                .orElse(null);
    }

    public LoanTransaction getLastRepaymentTransaction() {
        return loanTransactions.stream() //
                .filter(loanTransaction -> !loanTransaction.isReversed()) //
                .filter(LoanTransaction::isRepayment) //
                .reduce((first, second) -> second) //
                .orElse(null);
    }

    public LocalDate getLastUserTransactionForChargeCalc() {
        LocalDate lastTransaction = getDisbursementDate();
        if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            lastTransaction = getLastUserTransactionDate();
        }
        return lastTransaction;
    }

    public Set<LoanCharge> getActiveCharges() {
        // LinkedHashset is required here to maintain the charge order.
        // In case of charge calculation as percentage of another charge, parent charge must be processed first
        LinkedHashSet<LoanCharge> loanCharges = new LinkedHashSet<>();
        if (this.charges != null) {
            for (LoanCharge charge : this.charges) {
                if (charge.isActive()) {
                    loanCharges.add(charge);
                }
            }
        }
        return loanCharges;
    }

    public Set<LoanTrancheCharge> trancheCharges() {
        Set<LoanTrancheCharge> loanCharges = new HashSet<>();
        if (this.trancheCharges != null) {
            for (LoanTrancheCharge charge : this.trancheCharges) {
                loanCharges.add(charge);
            }
        }
        return loanCharges;
    }

    @SuppressWarnings({ "squid:S3776" })
    public List<LoanInstallmentCharge> generateInstallmentLoanCharges(final LoanCharge loanCharge) {
        final List<LoanInstallmentCharge> loanChargePerInstallments = new ArrayList<>();
        if (loanCharge.isInstalmentFee()) {
            // Loan canceled on disbursement date. Only charge hono
            if (this.isAnulado && this.isAnuladoOnDisbursementDate) {
                if (loanCharge.isCustomPercentageBasedOfAnotherCharge()) {
                    for (LoanCharge charge : this.getCharges()) {
                        if (charge.getCharge().getId() != null
                                && charge.getCharge().getId().equals(loanCharge.getCharge().getParentChargeId()) && !charge.isFlatHono()) {
                            return loanChargePerInstallments;
                        }

                    }
                }
                if (!loanCharge.isFlatHono()) {
                    return loanChargePerInstallments;
                }
            }
            List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallmentsIgnoringTotalGrace().stream()
                    .sorted(Comparator.comparingInt(LoanRepaymentScheduleInstallment::getInstallmentNumber)).toList();
            this.outstandingBalance = this.loanRepaymentScheduleDetail.getPrincipal();

            for (final LoanRepaymentScheduleInstallment installment : installments) {
                if (installment.isRecalculatedInterestComponent() || (loanCharge.getApplicableFromInstallment() != null
                        && loanCharge.getApplicableFromInstallment() > installment.getInstallmentNumber())) {
                    continue;
                }
                BigDecimal amount;
                if (loanCharge.getChargeCalculation().isFlat()) {
                    amount = loanCharge.amountOrPercentage();

                    if (loanCharge.isCustomFlatVoluntaryInsurenceCharge() && loanCharge.defaultFromInstallment() != null
                            && loanCharge.defaultFromInstallment() > 0
                            && installment.getInstallmentNumber() >= loanCharge.defaultFromInstallment()) {
                        amount = BigDecimal.ZERO;
                    }

                    if (loanCharge.getChargeCalculation().isFlatHono() && !loanCharge.getCustomChargeHonorarioMaps().isEmpty()) {
                        BigDecimal honoAmount = BigDecimal.ZERO;
                        for (CustomChargeHonorarioMap customCharge : loanCharge.getCustomChargeHonorarioMaps()) {
                            if (customCharge.getLoanInstallmentNr().equals(installment.getInstallmentNumber())) {
                                honoAmount = honoAmount.add(customCharge.getFeeBaseAmount());
                            }
                        }
                        amount = honoAmount;
                    } else if (loanCharge.isFlatHono()) {
                        amount = BigDecimal.ZERO;
                    }
                } else {
                    amount = calculateInstallmentChargeAmount(loanCharge.getChargeCalculation(), loanCharge.getPercentage(), installment,
                            loanCharge.getCharge().getParentChargeId(), loanCharge).getAmount();
                }
                if (((loanCharge.isCustomFlatDistributedCharge() || loanCharge.isCustomPercentageBasedDistributedCharge()
                        || loanCharge.isCustomPercentageOfOutstandingPrincipalCharge())
                        && loanCharge.getApplicableFromInstallment() > installment.getInstallmentNumber())) {
                    amount = BigDecimal.ZERO;
                }
                if (ChargeCalculationType.DISB_SEGO == loanCharge.getChargeCalculation() && Boolean.FALSE
                        .equals(loanCharge.getCharge().getName().contains(ChargeCustomType.COMISION_MI_PYME.getRootName()))) {
                    amount = loanCharge.amount();
                }

                if (loanCharge.getCharge().getName().contains(LIFE_INSURANCE.getRootName())
                        && containsBlockAccountFreezeLifeInsurance(installment.getDueDate())) {
                    amount = BigDecimal.ZERO;
                }

                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    final LoanInstallmentCharge loanInstallmentCharge = new LoanInstallmentCharge(amount, loanCharge, installment);
                    installment.getInstallmentCharges().add(loanInstallmentCharge);
                    loanChargePerInstallments.add(loanInstallmentCharge);
                }
            }
        }
        return loanChargePerInstallments;
    }

    @SuppressWarnings({ "squid:S3776" })
    public void validateAccountStatus(final LoanEvent event) {

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        switch (event) {
            case LOAN_APPROVED:
                if (!isSubmittedAndPendingApproval()) {
                    final String defaultUserMessage = "Loan Account Approval is not allowed. Loan Account is not in submitted and pending approval state.";
                    final ApiParameterError error = ApiParameterError
                            .generalError("error.msg.loan.approve.account.is.not.submitted.and.pending.state", defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_APPROVAL_UNDO:
                if (!isApproved()) {
                    final String defaultUserMessage = "Loan Account Undo Approval is not allowed. Loan Account is not in approved state.";
                    final ApiParameterError error = ApiParameterError.generalError("error.msg.loan.undo.approval.account.is.not.approved",
                            defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_DISBURSED:
                if ((!(isApproved() && isNotDisbursed()) && !this.loanProduct.isMultiDisburseLoan()) || (Boolean.FALSE.equals(
                        this.getLoanProduct().getName().toLowerCase().contains(LoanProductType.CREDITO_ROTATIVO.getCode().toLowerCase()))
                        && Boolean.FALSE.equals(this.getLoanProduct().getName().toLowerCase()
                                .contains(LoanProductType.NANO_CREDITO.getCode().toLowerCase()))
                        && this.loanProduct.isMultiDisburseLoan() && !isAllTranchesNotDisbursed())) {
                    final String defaultUserMessage = "Loan Disbursal is not allowed. Loan Account is not in approved and not disbursed state.";
                    final ApiParameterError error = ApiParameterError
                            .generalError("error.msg.loan.disbursal.account.is.not.approve.not.disbursed.state", defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_DISBURSAL_UNDO:
                if (!isOpen()) {
                    final String defaultUserMessage = "Loan Undo disbursal is not allowed. Loan Account is not active.";
                    final ApiParameterError error = ApiParameterError.generalError("error.msg.loan.undo.disbursal.account.is.not.active",
                            defaultUserMessage);
                    dataValidationErrors.add(error);
                }
                if (isOpen() && this.isTopup()) {
                    final String defaultUserMessage = "Loan Undo disbursal is not allowed on Topup Loans";
                    final ApiParameterError error = ApiParameterError
                            .generalError("error.msg.loan.undo.disbursal.not.allowed.on.topup.loan", defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_REPAYMENT_OR_WAIVER:
                if (!isOpen()) {
                    final String defaultUserMessage = "Loan Repayment (or its types) or Waiver is not allowed. Loan Account is not active.";
                    final ApiParameterError error = ApiParameterError
                            .generalError("error.msg.loan.repayment.or.waiver.account.is.not.active", defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_REJECTED:
                if (!isSubmittedAndPendingApproval()) {
                    final String defaultUserMessage = "Loan application cannot be rejected. Loan Account is not in Submitted and Pending approval state.";
                    final ApiParameterError error = ApiParameterError
                            .generalError("error.msg.loan.reject.account.is.not.submitted.pending.approval.state", defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_WITHDRAWN:
                if (!isSubmittedAndPendingApproval()) {
                    final String defaultUserMessage = "Loan application cannot be withdrawn. Loan Account is not in Submitted and Pending approval state.";
                    final ApiParameterError error = ApiParameterError
                            .generalError("error.msg.loan.withdrawn.account.is.not.submitted.pending.approval.state", defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case WRITE_OFF_OUTSTANDING:
                if (!isOpen()) {
                    final String defaultUserMessage = "No se permite la condonación del préstamo. La cuenta del préstamo no está activa";
                    final ApiParameterError error = ApiParameterError.generalError("error.msg.loan.writtenoff.account.is.not.active",
                            defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case WRITE_OFF_OUTSTANDING_UNDO:
                if (!isClosedWrittenOff()) {
                    final String defaultUserMessage = "Loan Undo Written off is not allowed. Loan Account is not Written off.";
                    final ApiParameterError error = ApiParameterError
                            .generalError("error.msg.loan.undo.writtenoff.account.is.not.written.off", defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_CHARGE_PAYMENT:
                if (!isOpen()) {
                    final String defaultUserMessage = "Charge payment is not allowed. Loan Account is not Active.";
                    final ApiParameterError error = ApiParameterError.generalError("error.msg.loan.charge.payment.account.is.not.active",
                            defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_CLOSED:
                if (!isOpen()) {
                    final String defaultUserMessage = "Closing Loan Account is not allowed. Loan Account is not Active.";
                    final ApiParameterError error = ApiParameterError.generalError("error.msg.loan.close.account.is.not.active",
                            defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_EDIT_MULTI_DISBURSE_DATE:
                if (isClosed()) {
                    final String defaultUserMessage = "Edit disbursement is not allowed. Loan Account is not active.";
                    final ApiParameterError error = ApiParameterError.generalError("error.msg.loan.edit.disbursement.account.is.not.active",
                            defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_RECOVERY_PAYMENT:
                if (!isClosedWrittenOff()) {
                    final String defaultUserMessage = "Recovery repayments may only be made on loans which are written off";
                    final ApiParameterError error = ApiParameterError.generalError("error.msg.loan.account.is.not.written.off",
                            defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_REFUND:
                if (!isOpen()) {
                    final String defaultUserMessage = "Loan Refund is not allowed. Loan Account is not active.";
                    final ApiParameterError error = ApiParameterError.generalError("error.msg.loan.refund.account.is.not.active",
                            defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_DISBURSAL_UNDO_LAST:
                if (!isOpen()) {
                    final String defaultUserMessage = "Loan Undo last disbursal is not allowed. Loan Account is not active.";
                    final ApiParameterError error = ApiParameterError
                            .generalError("error.msg.loan.undo.last.disbursal.account.is.not.active", defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_FORECLOSURE:
                if (!isOpen()) {
                    final String defaultUserMessage = "Loan foreclosure is not allowed. Loan Account is not active.";
                    final ApiParameterError error = ApiParameterError.generalError("error.msg.loan.foreclosure.account.is.not.active",
                            defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_CREDIT_BALANCE_REFUND:
                if (!getStatus().isOverpaid()) {
                    final String defaultUserMessage = "Loan Credit Balance Refund is not allowed. Loan Account is not Overpaid.";
                    final ApiParameterError error = ApiParameterError
                            .generalError("error.msg.loan.credit.balance.refund.account.is.not.overpaid", defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            case LOAN_CHARGE_ADJUSTMENT:
                if (!(getStatus().isActive() || getStatus().isClosedObligationsMet() || getStatus().isOverpaid())) {
                    final String defaultUserMessage = "Loan Charge Adjustment is not allowed. Loan Account must be either Active, Fully repaid or Overpaid.";
                    final ApiParameterError error = ApiParameterError
                            .generalError("error.msg.loan.charge.adjustment.account.is.not.in.valid.state", defaultUserMessage);
                    dataValidationErrors.add(error);
                }
            break;
            default:
            break;
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }

    }

    public LoanCharge fetchLoanChargesById(Long id) {
        LoanCharge charge = null;
        for (LoanCharge loanCharge : this.charges) {
            if (id.equals(loanCharge.getId())) {
                charge = loanCharge;
                break;
            }
        }
        return charge;
    }

    private List<Long> fetchAllLoanChargeIds() {
        List<Long> list = new ArrayList<>();
        for (LoanCharge loanCharge : this.charges) {
            list.add(loanCharge.getId());
        }
        return list;
    }

    public List<LoanDisbursementDetails> getAllDisbursementDetails() {
        return this.disbursementDetails;
    }

    public List<LoanDisbursementDetails> getDisbursementDetails() {
        List<LoanDisbursementDetails> currentDisbursementDetails = new ArrayList<>();
        for (LoanDisbursementDetails disbursementDetail : this.disbursementDetails) {
            if (!disbursementDetail.isReversed()) {
                currentDisbursementDetails.add(disbursementDetail);
            }
        }
        return currentDisbursementDetails;
    }

    public void addDisbursementDetails(LoanDisbursementDetails disbursementDetails) {
        if (Objects.nonNull(this.disbursementDetails)) {
            this.disbursementDetails.add(disbursementDetails);
        }
    }

    public LoanDisbursementDetails getDisbursementDetails(final LocalDate transactionDate, final BigDecimal transactionAmount) {
        for (LoanDisbursementDetails disbursementDetail : this.disbursementDetails) {
            if (!disbursementDetail.isReversed() && disbursementDetail.getDisbursementDate().equals(transactionDate)
                    && (disbursementDetail.principal().compareTo(transactionAmount) == 0)) {
                return disbursementDetail;
            }
        }
        return null;
    }

    public ChangedTransactionDetail updateDisbursementDateAndAmountForTranche(final LoanDisbursementDetails disbursementDetails,
            final JsonCommand command, final Map<String, Object> actualChanges, final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        final Locale locale = command.extractLocale();
        validateAccountStatus(LoanEvent.LOAN_EDIT_MULTI_DISBURSE_DATE);
        final BigDecimal principal = command.bigDecimalValueOfParameterNamed(LoanApiConstants.updatedDisbursementPrincipalParameterName,
                locale);
        final LocalDate expectedDisbursementDateV2 = command
                .localDateValueOfParameterNamed(LoanApiConstants.updatedDisbursementDateParameterName);
        disbursementDetails.updateExpectedDisbursementDateAndAmount(expectedDisbursementDateV2, principal);
        actualChanges.put(LoanApiConstants.expectedDisbursementDateParameterName,
                command.stringValueOfParameterNamed(LoanApiConstants.expectedDisbursementDateParameterName));
        actualChanges.put(LoanApiConstants.disbursementIdParameterName,
                command.stringValueOfParameterNamed(LoanApiConstants.disbursementIdParameterName));
        actualChanges.put(LoanApiConstants.disbursementPrincipalParameterName,
                command.bigDecimalValueOfParameterNamed(LoanApiConstants.disbursementPrincipalParameterName, locale));

        this.loanRepaymentScheduleDetail.setPrincipal(getPrincipalAmountForRepaymentSchedule());

        if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO);
        } else {
            regenerateRepaymentSchedule(scheduleGeneratorDTO);
        }

        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retrieveListOfTransactionsPostDisbursement();
        ChangedTransactionDetail changedTransactionDetail = loanRepaymentScheduleTransactionProcessor.reprocessLoanTransactions(
                getDisbursementDate(), allNonContraTransactionsPostDisbursement, getCurrency(), getRepaymentScheduleInstallments(),
                getActiveCharges());
        for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
            mapEntry.getValue().updateLoan(this);
            addLoanTransaction(mapEntry.getValue());
        }

        return changedTransactionDetail;
    }

    public BigDecimal getPrincipalAmountForRepaymentSchedule() {
        BigDecimal principalAmount = BigDecimal.ZERO;

        if (isMultiDisburmentLoan() && isDisbursed()) {
            Collection<LoanDisbursementDetails> loanDisburseDetails = this.getDisbursementDetails();
            for (LoanDisbursementDetails details : loanDisburseDetails) {
                if (details.actualDisbursementDate() != null) {
                    principalAmount = principalAmount.add(details.principal());
                }
            }
        } else if (isApproved()) {
            principalAmount = getApprovedPrincipal();
        } else {
            principalAmount = getPrincipal().getAmount();
        }

        return principalAmount;
    }

    public BigDecimal retriveLastEmiAmount() {
        BigDecimal emiAmount = this.fixedEmiAmount;
        LocalDate startDate = this.getDisbursementDate();
        for (LoanTermVariations loanTermVariationItem : this.loanTermVariations) {
            if (loanTermVariationItem.getTermType().isEMIAmountVariation()
                    && !DateUtils.isAfter(startDate, loanTermVariationItem.getTermApplicableFrom())) {
                startDate = loanTermVariationItem.getTermApplicableFrom();
                emiAmount = loanTermVariationItem.getTermValue();
            }
        }
        return emiAmount;
    }

    public LoanRepaymentScheduleInstallment fetchRepaymentScheduleInstallment(final Integer installmentNumber) {
        LoanRepaymentScheduleInstallment installment = null;
        if (installmentNumber == null) {
            return installment;
        }
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment scheduleInstallment : installments) {
            if (scheduleInstallment.getInstallmentNumber().equals(installmentNumber)) {
                installment = scheduleInstallment;
                break;
            }
        }
        return installment;
    }

    public BigDecimal getApprovedPrincipal() {
        return this.approvedPrincipal;
    }

    public BigDecimal getNetDisbursalAmount() {
        return netDisbursalAmount;
    }

    public BigDecimal deductFromNetDisbursalAmount(final BigDecimal subtrahend) {
        this.netDisbursalAmount = this.netDisbursalAmount.subtract(subtrahend);
        return netDisbursalAmount;
    }

    public void setNetDisbursalAmount(BigDecimal netDisbursalAmount) {
        this.netDisbursalAmount = netDisbursalAmount;
    }

    public BigDecimal getTotalOverpaid() {
        return this.totalOverpaid;
    }

    public Money getTotalOverpaidAsMoney() {
        return Money.of(this.repaymentScheduleDetail().getCurrency(), this.totalOverpaid);
    }

    public LocalDate getOverpaidOnDate() {
        return this.overpaidOnDate;
    }

    public void updateIsInterestRecalculationEnabled() {
        this.loanRepaymentScheduleDetail.updateIsInterestRecalculationEnabled(isInterestRecalculationEnabledForProduct());
    }

    public LoanInterestRecalculationDetails loanInterestRecalculationDetails() {
        return this.loanInterestRecalculationDetails;
    }

    public Long loanInterestRecalculationDetailId() {
        if (loanInterestRecalculationDetails() != null) {
            return this.loanInterestRecalculationDetails.getId();
        }
        return null;
    }

    public boolean isInterestBearing() {
        return BigDecimal.ZERO.compareTo(getLoanRepaymentScheduleDetail().getAnnualNominalInterestRate()) < 0;
    }

    public LocalDate getExpectedMaturityDate() {
        return this.expectedMaturityDate;
    }

    public LocalDate getMaturityDate() {
        return this.actualMaturityDate;
    }

    public ChangedTransactionDetail recalculateScheduleFromLastTransaction(final ScheduleGeneratorDTO generatorDTO,
            final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds) {
        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());
        if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            regenerateRepaymentScheduleWithInterestRecalculation(generatorDTO);
        } else {
            regenerateRepaymentSchedule(generatorDTO);
        }
        return processTransactions();

    }

    public ChangedTransactionDetail recalculateScheduleFromLastTransaction(final ScheduleGeneratorDTO generatorDTO) {
        if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            regenerateRepaymentScheduleWithInterestRecalculation(generatorDTO);
        } else {
            regenerateRepaymentSchedule(generatorDTO);
        }
        return processTransactions();

    }

    public ChangedTransactionDetail handleRegenerateRepaymentScheduleWithInterestRecalculation(final ScheduleGeneratorDTO generatorDTO) {
        regenerateRepaymentScheduleWithInterestRecalculation(generatorDTO);
        return processTransactions();

    }

    public ChangedTransactionDetail processTransactions() {
        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retrieveListOfTransactionsPostDisbursement();
        ChangedTransactionDetail changedTransactionDetail = loanRepaymentScheduleTransactionProcessor.reprocessLoanTransactions(
                getDisbursementDate(), allNonContraTransactionsPostDisbursement, getCurrency(), getRepaymentScheduleInstallments(),
                getActiveCharges());
        for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
            mapEntry.getValue().updateLoan(this);
        }
        /***
         * Commented since throwing exception if external id present for one of the transactions. for this need to save
         * the reversed transactions first and then new transactions.
         */
        this.loanTransactions.addAll(changedTransactionDetail.getNewTransactionMappings().values());
        updateLoanSummaryDerivedFields();

        return changedTransactionDetail;
    }

    @SuppressWarnings({ "squid:S3776" })
    public void regenerateRepaymentScheduleWithInterestRecalculation(final ScheduleGeneratorDTO generatorDTO) {
        LocalDate lastTransactionDate = getLastUserTransactionDate();
        if (this.summary != null && this.summary.getTotalPrincipalRepaid() != null) {
            generatorDTO.setTotalRepaid(this.summary.getTotalPrincipalRepaid());
        }
        final LoanScheduleDTO loanSchedule = getRecalculatedSchedule(generatorDTO);
        if (loanSchedule == null) {
            return;
        }
        if (loanSchedule.getInstallments() != null) {
            updateLoanSchedule(loanSchedule);
        } else {
            updateLoanSchedule(loanSchedule.getLoanScheduleModel());
        }
        this.interestRecalculatedOn = DateUtils.getBusinessLocalDate();
        LocalDate lastRepaymentDate = this.getLastRepaymentPeriodDueDate(true);
        final Set<LoanCharge> activeCharges = this.getActiveCharges();
        for (final LoanCharge loanCharge : activeCharges) {
            if (!loanCharge.isDueAtDisbursement()) {
                updateOverdueScheduleInstallment(loanCharge);
                if (loanCharge.getDueLocalDate() == null || !DateUtils.isBefore(lastRepaymentDate, loanCharge.getDueLocalDate())) {
                    if ((loanCharge.isInstalmentFee() || !loanCharge.isWaived()) && ((loanCharge.getDueLocalDate() == null
                            || !DateUtils.isAfter(lastTransactionDate, loanCharge.getDueLocalDate()))
                            || loanCharge.isFlatSpecificDueDateCharge())) {
                        recalculateLoanCharge(loanCharge);
                        loanCharge.updateWaivedAmount(getCurrency());
                    }
                } else {
                    loanCharge.setActive(false);
                }
            }
        }
        processPostDisbursementTransactions();
        processIncomeTransactions();
        updateLoanDerivedFields();
    }

    private void updateLoanChargesPaidBy(LoanTransaction accrual, HashMap<String, Object> feeDetails,
            LoanRepaymentScheduleInstallment installment) {
        @SuppressWarnings("unchecked")
        List<LoanCharge> loanCharges = (List<LoanCharge>) feeDetails.get("loanCharges");
        @SuppressWarnings("unchecked")
        List<LoanInstallmentCharge> loanInstallmentCharges = (List<LoanInstallmentCharge>) feeDetails.get("loanInstallmentCharges");
        if (loanCharges != null) {
            for (LoanCharge loanCharge : loanCharges) {
                Integer installmentNumber = null == installment ? null : installment.getInstallmentNumber();
                final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(accrual, loanCharge,
                        loanCharge.getAmount(getCurrency()).getAmount(), installmentNumber);
                accrual.getLoanChargesPaid().add(loanChargePaidBy);
            }
        }
        if (loanInstallmentCharges != null) {
            for (LoanInstallmentCharge loanInstallmentCharge : loanInstallmentCharges) {
                Integer installmentNumber = null == loanInstallmentCharge.getInstallment() ? null
                        : loanInstallmentCharge.getInstallment().getInstallmentNumber();
                final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(accrual, loanInstallmentCharge.getLoanCharge(),
                        loanInstallmentCharge.getAmount(getCurrency()).getAmount(), installmentNumber);
                accrual.getLoanChargesPaid().add(loanChargePaidBy);
            }
        }
    }

    public void processIncomeTransactions() {
        if (this.loanInterestRecalculationDetails != null && this.loanInterestRecalculationDetails.isCompoundingToBePostedAsTransaction()) {
            LocalDate lastCompoundingDate = this.getDisbursementDate();
            List<LoanInterestRecalcualtionAdditionalDetails> compoundingDetails = extractInterestRecalculationAdditionalDetails();
            List<LoanTransaction> incomeTransactions = retrieveListOfIncomePostingTransactions();
            List<LoanTransaction> accrualTransactions = retrieveListOfAccrualTransactions();
            for (LoanInterestRecalcualtionAdditionalDetails compoundingDetail : compoundingDetails) {
                if (!DateUtils.isBeforeBusinessDate(compoundingDetail.getEffectiveDate())) {
                    break;
                }
                LoanTransaction incomeTransaction = getTransactionForDate(incomeTransactions, compoundingDetail.getEffectiveDate());
                LoanTransaction accrualTransaction = getTransactionForDate(accrualTransactions, compoundingDetail.getEffectiveDate());
                addUpdateIncomeAndAccrualTransaction(compoundingDetail, lastCompoundingDate, incomeTransaction, accrualTransaction);
                lastCompoundingDate = compoundingDetail.getEffectiveDate();
            }
            List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
            LoanRepaymentScheduleInstallment lastInstallment = installments.get(installments.size() - 1);
            reverseTransactionsPostEffectiveDate(incomeTransactions, lastInstallment.getDueDate());
            reverseTransactionsPostEffectiveDate(accrualTransactions, lastInstallment.getDueDate());
        }
    }

    private void reverseTransactionsOnOrAfter(List<LoanTransaction> transactions, LocalDate date) {
        for (LoanTransaction loanTransaction : transactions) {
            if (!DateUtils.isBefore(loanTransaction.getTransactionDate(), date)) {
                loanTransaction.reverse();
            }
        }
    }

    private void addUpdateIncomeAndAccrualTransaction(LoanInterestRecalcualtionAdditionalDetails compoundingDetail,
            LocalDate lastCompoundingDate, LoanTransaction existingIncomeTransaction, LoanTransaction existingAccrualTransaction) {
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal penalties = BigDecimal.ZERO;
        HashMap<String, Object> feeDetails = new HashMap<>();

        if (this.loanInterestRecalculationDetails.getInterestRecalculationCompoundingMethod()
                .equals(InterestRecalculationCompoundingMethod.INTEREST)) {
            interest = compoundingDetail.getAmount();
        } else if (this.loanInterestRecalculationDetails.getInterestRecalculationCompoundingMethod()
                .equals(InterestRecalculationCompoundingMethod.FEE)) {
            determineFeeDetails(lastCompoundingDate, compoundingDetail.getEffectiveDate(), feeDetails);
            fee = (BigDecimal) feeDetails.get(FEE);
            penalties = (BigDecimal) feeDetails.get(PENALTIES);
        } else if (this.loanInterestRecalculationDetails.getInterestRecalculationCompoundingMethod()
                .equals(InterestRecalculationCompoundingMethod.INTEREST_AND_FEE)) {
            determineFeeDetails(lastCompoundingDate, compoundingDetail.getEffectiveDate(), feeDetails);
            fee = (BigDecimal) feeDetails.get(FEE);
            penalties = (BigDecimal) feeDetails.get(PENALTIES);
            interest = compoundingDetail.getAmount().subtract(fee).subtract(penalties);
        }

        ExternalId externalIdEmpty = ExternalId.empty();
        if (TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
            externalIdEmpty = ExternalId.generate();
        }

        if (existingIncomeTransaction == null) {
            LoanTransaction transaction = LoanTransaction.incomePosting(this, this.getOffice(), compoundingDetail.getEffectiveDate(),
                    compoundingDetail.getAmount(), interest, fee, penalties, externalIdEmpty);
            addLoanTransaction(transaction);
        } else if (existingIncomeTransaction.getAmount(getCurrency()).getAmount().compareTo(compoundingDetail.getAmount()) != 0) {
            existingIncomeTransaction.reverse();
            LoanTransaction transaction = LoanTransaction.incomePosting(this, this.getOffice(), compoundingDetail.getEffectiveDate(),
                    compoundingDetail.getAmount(), interest, fee, penalties, externalIdEmpty);
            addLoanTransaction(transaction);
        }

        if (TemporaryConfigurationServiceContainer.isExternalIdAutoGenerationEnabled()) {
            externalIdEmpty = ExternalId.generate();
        }

        if (Boolean.TRUE.equals(isPeriodicAccrualAccountingEnabledOnLoanProduct())) {
            if (existingAccrualTransaction == null) {
                LoanTransaction accrual = LoanTransaction.accrueTransaction(this, this.getOffice(), compoundingDetail.getEffectiveDate(),
                        compoundingDetail.getAmount(), interest, fee, penalties, externalIdEmpty);
                updateLoanChargesPaidBy(accrual, feeDetails, null);
                addLoanTransaction(accrual);
            } else if (existingAccrualTransaction.getAmount(getCurrency()).getAmount().compareTo(compoundingDetail.getAmount()) != 0) {
                existingAccrualTransaction.reverse();
                LoanTransaction accrual = LoanTransaction.accrueTransaction(this, this.getOffice(), compoundingDetail.getEffectiveDate(),
                        compoundingDetail.getAmount(), interest, fee, penalties, externalIdEmpty);
                updateLoanChargesPaidBy(accrual, feeDetails, null);
                addLoanTransaction(accrual);
            }
        }
        updateLoanOutstandingBalances();
    }

    @SuppressWarnings({ "squid:S3776" })
    private void determineFeeDetails(LocalDate fromDate, LocalDate toDate, HashMap<String, Object> feeDetails) {
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal penalties = BigDecimal.ZERO;

        List<Integer> installments = new ArrayList<>();
        List<LoanRepaymentScheduleInstallment> repaymentSchedule = getRepaymentScheduleInstallments();
        for (LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : repaymentSchedule) {
            if (DateUtils.isAfter(loanRepaymentScheduleInstallment.getDueDate(), fromDate)
                    && !DateUtils.isAfter(loanRepaymentScheduleInstallment.getDueDate(), toDate)) {
                installments.add(loanRepaymentScheduleInstallment.getInstallmentNumber());
            }
        }

        List<LoanCharge> loanCharges = new ArrayList<>();
        List<LoanInstallmentCharge> loanInstallmentCharges = new ArrayList<>();
        for (LoanCharge loanCharge : this.getActiveCharges()) {
            boolean isDue = DateUtils.isEqual(fromDate, this.getDisbursementDate())
                    ? loanCharge.isDueForCollectionFromIncludingAndUpToAndIncluding(fromDate, toDate)
                    : loanCharge.isDueForCollectionFromAndUpToAndIncluding(fromDate, toDate);
            if (isDue) {
                if (loanCharge.isPenaltyCharge() && !loanCharge.isInstalmentFee()) {
                    penalties = penalties.add(loanCharge.amount());
                    loanCharges.add(loanCharge);
                } else if (!loanCharge.isInstalmentFee()) {
                    fee = fee.add(loanCharge.amount());
                    loanCharges.add(loanCharge);
                }
            } else if (loanCharge.isInstalmentFee()) {
                for (LoanInstallmentCharge installmentCharge : loanCharge.installmentCharges()) {
                    if (installments.contains(installmentCharge.getRepaymentInstallment().getInstallmentNumber())) {
                        fee = fee.add(installmentCharge.getAmount());
                        loanInstallmentCharges.add(installmentCharge);
                    }
                }
            }
        }

        feeDetails.put(FEE, fee);
        feeDetails.put(PENALTIES, penalties);
        feeDetails.put("loanCharges", loanCharges);
        feeDetails.put("loanInstallmentCharges", loanInstallmentCharges);
    }

    private LoanTransaction getTransactionForDate(List<LoanTransaction> transactions, LocalDate effectiveDate) {
        for (LoanTransaction loanTransaction : transactions) {
            if (DateUtils.isEqual(effectiveDate, loanTransaction.getTransactionDate())) {
                return loanTransaction;
            }
        }
        return null;
    }

    private void reverseTransactionsPostEffectiveDate(List<LoanTransaction> transactions, LocalDate effectiveDate) {
        for (LoanTransaction loanTransaction : transactions) {
            if (DateUtils.isAfter(loanTransaction.getTransactionDate(), effectiveDate)) {
                loanTransaction.reverse();
            }
        }
    }

    private List<LoanInterestRecalcualtionAdditionalDetails> extractInterestRecalculationAdditionalDetails() {
        List<LoanInterestRecalcualtionAdditionalDetails> retDetails = new ArrayList<>();
        List<LoanRepaymentScheduleInstallment> repaymentSchedule = getRepaymentScheduleInstallments();
        if (null != repaymentSchedule) {
            for (LoanRepaymentScheduleInstallment installment : repaymentSchedule) {
                if (null != installment.getLoanCompoundingDetails()) {
                    retDetails.addAll(installment.getLoanCompoundingDetails());
                }
            }
        }
        retDetails.sort(Comparator.comparing(LoanInterestRecalcualtionAdditionalDetails::getEffectiveDate));
        return retDetails;
    }

    public void processPostDisbursementTransactions() {
        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retrieveListOfTransactionsPostDisbursement();
        final List<LoanTransaction> copyTransactions = new ArrayList<>();
        if (!allNonContraTransactionsPostDisbursement.isEmpty()) {
            for (LoanTransaction loanTransaction : allNonContraTransactionsPostDisbursement) {
                copyTransactions.add(LoanTransaction.copyTransactionProperties(loanTransaction));
            }
            loanRepaymentScheduleTransactionProcessor.reprocessLoanTransactions(getDisbursementDate(), copyTransactions, getCurrency(),
                    getRepaymentScheduleInstallments(), getActiveCharges());

            updateLoanSummaryDerivedFields();
        }
    }

    private LoanScheduleDTO getRecalculatedSchedule(final ScheduleGeneratorDTO generatorDTO) {
        if (!this.repaymentScheduleDetail().isEnableDownPayment()
                && (!this.repaymentScheduleDetail().isInterestRecalculationEnabled() || isNpa || isChargedOff())
                && !this.getLoanProductRelatedDetail().getLoanScheduleType().equals(LoanScheduleType.PROGRESSIVE)) {
            return null;
        }

        final InterestMethod interestMethod = this.loanRepaymentScheduleDetail.getInterestMethod();
        final LoanScheduleGenerator loanScheduleGenerator = generatorDTO.getLoanScheduleFactory()
                .create(this.loanRepaymentScheduleDetail.getLoanScheduleType(), interestMethod);

        final MathContext mc = MoneyHelper.getMathContext();

        LoanApplicationTerms loanApplicationTerms = constructLoanApplicationTerms(generatorDTO);
        loanApplicationTerms.setLoan(Optional.of(this));

        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);
        return loanScheduleGenerator.rescheduleNextInstallmentsForProgressiveLoans(mc, loanApplicationTerms, this,
                generatorDTO.getHolidayDetailDTO(), loanRepaymentScheduleTransactionProcessor, generatorDTO.getRecalculateFrom());
    }

    public LoanRepaymentScheduleInstallment fetchPrepaymentDetail(final ScheduleGeneratorDTO scheduleGeneratorDTO, final LocalDate onDate) {
        LoanRepaymentScheduleInstallment installment;
        if (this.loanRepaymentScheduleDetail.isInterestRecalculationEnabled()) {

            final MathContext mc = MoneyHelper.getMathContext();

            final InterestMethod interestMethod = this.loanRepaymentScheduleDetail.getInterestMethod();
            final LoanApplicationTerms loanApplicationTerms = constructLoanApplicationTerms(scheduleGeneratorDTO);

            final LoanScheduleGenerator loanScheduleGenerator = scheduleGeneratorDTO.getLoanScheduleFactory()
                    .create(loanApplicationTerms.getLoanScheduleType(), interestMethod);
            final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                    .determineProcessor(this.transactionProcessingStrategyCode);
            installment = loanScheduleGenerator.calculatePrepaymentAmount(getCurrency(), onDate, loanApplicationTerms, mc, this,
                    scheduleGeneratorDTO.getHolidayDetailDTO(), loanRepaymentScheduleTransactionProcessor);
        } else {
            installment = this.getTotalOutstandingOnLoan();
        }
        return installment;
    }

    public LoanApplicationTerms constructLoanApplicationTerms(final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        final Integer loanTermFrequency = this.termFrequency;
        final PeriodFrequencyType loanTermPeriodFrequencyType = PeriodFrequencyType.fromInt(this.termPeriodFrequencyType);
        NthDayType nthDayType = null;
        DayOfWeekType dayOfWeekType = null;
        final List<DisbursementData> disbursementData = new ArrayList<>();
        for (LoanDisbursementDetails disbursementDetailsV2 : getDisbursementDetails()) {
            disbursementData.add(disbursementDetailsV2.toData());
        }

        Calendar calendar = scheduleGeneratorDTO.getCalendar();
        if (calendar != null) {
            nthDayType = CalendarUtils.getRepeatsOnNthDayOfMonth(calendar.getRecurrence());
            dayOfWeekType = DayOfWeekType.fromInt(CalendarUtils.getRepeatsOnDay(calendar.getRecurrence()).getValue());
        }
        HolidayDetailDTO holidayDetailDTO = scheduleGeneratorDTO.getHolidayDetailDTO();
        CalendarInstance restCalendarInstance = null;
        CalendarInstance compoundingCalendarInstance = null;
        RecalculationFrequencyType recalculationFrequencyType = null;
        InterestRecalculationCompoundingMethod compoundingMethod = null;
        RecalculationFrequencyType compoundingFrequencyType = null;
        LoanRescheduleStrategyMethod rescheduleStrategyMethod = null;
        CalendarHistoryDataWrapper calendarHistoryDataWrapper;
        RepaymentStartDateType repaymentStartDateType = this.getLoanProduct().getRepaymentStartDateType();
        boolean allowCompoundingOnEod = false;
        if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            restCalendarInstance = scheduleGeneratorDTO.getCalendarInstanceForInterestRecalculation();
            compoundingCalendarInstance = scheduleGeneratorDTO.getCompoundingCalendarInstance();
            recalculationFrequencyType = this.loanInterestRecalculationDetails.getRestFrequencyType();
            compoundingMethod = this.loanInterestRecalculationDetails.getInterestRecalculationCompoundingMethod();
            compoundingFrequencyType = this.loanInterestRecalculationDetails.getCompoundingFrequencyType();
            rescheduleStrategyMethod = this.loanInterestRecalculationDetails.getRescheduleStrategyMethod();
            allowCompoundingOnEod = this.loanInterestRecalculationDetails.allowCompoundingOnEod();
        }
        calendar = scheduleGeneratorDTO.getCalendar();
        calendarHistoryDataWrapper = scheduleGeneratorDTO.getCalendarHistoryDataWrapper();

        BigDecimal annualNominalInterestRate = this.loanRepaymentScheduleDetail.getAnnualNominalInterestRate();
        FloatingRateDTO floatingRateDTO = scheduleGeneratorDTO.getFloatingRateDTO();
        List<LoanTermVariationsData> loanTermVariationsV2 = new ArrayList<>();
        annualNominalInterestRate = constructLoanTermVariations(floatingRateDTO, annualNominalInterestRate, loanTermVariationsV2);
        LocalDate interestChargedFromDateV2 = getInterestChargedFromDate();
        if (interestChargedFromDateV2 == null && scheduleGeneratorDTO.isInterestChargedFromDateAsDisbursementDateEnabled()) {
            interestChargedFromDateV2 = getDisbursementDate();
        }

        LoanApplicationTerms loanApplicationTerms = LoanApplicationTerms.assembleFrom(scheduleGeneratorDTO.getApplicationCurrency(),
                loanTermFrequency, loanTermPeriodFrequencyType, nthDayType, dayOfWeekType, getDisbursementDate(),
                getExpectedFirstRepaymentOnDate(), scheduleGeneratorDTO.getCalculatedRepaymentsStartingFromDate(), getInArrearsTolerance(),
                this.loanRepaymentScheduleDetail, this.loanProduct.isMultiDisburseLoan(), this.fixedEmiAmount, disbursementData,
                this.maxOutstandingLoanBalance, interestChargedFromDateV2, this.loanProduct.getPrincipalThresholdForLastInstallment(),
                this.loanProduct.getInstallmentAmountInMultiplesOf(), recalculationFrequencyType, restCalendarInstance, compoundingMethod,
                compoundingCalendarInstance, compoundingFrequencyType, this.loanProduct.preCloseInterestCalculationStrategy(),
                rescheduleStrategyMethod, calendar, getApprovedPrincipal(), annualNominalInterestRate, loanTermVariationsV2,
                calendarHistoryDataWrapper, scheduleGeneratorDTO.getNumberOfdays(), scheduleGeneratorDTO.isSkipRepaymentOnFirstDayofMonth(),
                holidayDetailDTO, allowCompoundingOnEod, scheduleGeneratorDTO.isFirstRepaymentDateAllowedOnHoliday(),
                scheduleGeneratorDTO.isInterestToBeRecoveredFirstWhenGreaterThanEMI(), this.fixedPrincipalPercentagePerInstallment,
                scheduleGeneratorDTO.isPrincipalCompoundingDisabledForOverdueLoans(), repaymentStartDateType, getSubmittedOnDate());
        if (this.getLoanProductRelatedDetail().hasTotalGracePeriod()) {
            loanApplicationTerms.setNumberOfInstallmentsToIgnore(this.getLoanProductRelatedDetail().getGraceOnPrincipalPayment());
        }

        // For schedule preview, before creating a new loan or for quotation, we can use principal amount
        // OR before approving new loan, we can use principal amount
        if (loanApplicationTerms.getApprovedPrincipal().getAmount().compareTo(BigDecimal.ZERO) == 0) {
            loanApplicationTerms.setPrincipalAmountApproved(getProposedPrincipal());
        } else {
            loanApplicationTerms.setPrincipalAmountApproved(getApprovedPrincipal());
        }

        loanApplicationTerms.setPrincipalAmountProposed(getProposedPrincipal());

        if (Objects.nonNull(this.getExpectedDisbursedOnLocalDate())) {
            loanApplicationTerms.setInstallmentDayOfMonth(this.getExpectedDisbursedOnLocalDate().getDayOfMonth());
        }
        loanApplicationTerms.setReduceTermForInstallment(this.shouldReduceTerm());
        loanApplicationTerms.setLoanProductName(this.getLoanProduct().getName());
        if (scheduleGeneratorDTO.getTotalRepaid() != null) {
            loanApplicationTerms.setTotalRepaid(scheduleGeneratorDTO.getTotalRepaid());
        }
        return loanApplicationTerms;
    }

    public BigDecimal constructLoanTermVariations(FloatingRateDTO floatingRateDTO, BigDecimal annualNominalInterestRate,
            List<LoanTermVariationsData> loanTermVariations) {
        for (LoanTermVariations variationTerms : this.loanTermVariations) {
            if (Boolean.TRUE.equals(variationTerms.isActive())) {
                loanTermVariations.add(variationTerms.toData());
            }
        }
        annualNominalInterestRate = constructFloatingInterestRates(annualNominalInterestRate, floatingRateDTO, loanTermVariations);
        return annualNominalInterestRate;
    }

    private LoanRepaymentScheduleInstallment getTotalOutstandingOnLoan() {
        Money feeCharges = Money.zero(loanCurrency());
        Money penaltyCharges = Money.zero(loanCurrency());
        Money totalPrincipal = Money.zero(loanCurrency());
        Money totalInterest = Money.zero(loanCurrency());
        final Set<LoanInterestRecalcualtionAdditionalDetails> compoundingDetails = null;
        List<LoanRepaymentScheduleInstallment> repaymentSchedule = getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment scheduledRepayment : repaymentSchedule) {
            totalPrincipal = totalPrincipal.plus(scheduledRepayment.getPrincipalOutstanding(loanCurrency()));
            totalInterest = totalInterest.plus(scheduledRepayment.getInterestOutstanding(loanCurrency()));
            feeCharges = feeCharges.plus(scheduledRepayment.getFeeChargesOutstanding(loanCurrency()));
            penaltyCharges = penaltyCharges.plus(scheduledRepayment.getPenaltyChargesOutstanding(loanCurrency()));
        }
        LocalDate businessDate = DateUtils.getBusinessLocalDate();
        return new LoanRepaymentScheduleInstallment(null, 0, businessDate, businessDate, totalPrincipal.getAmount(),
                totalInterest.getAmount(), feeCharges.getAmount(), penaltyCharges.getAmount(), false, compoundingDetails);
    }

    public LocalDate getAccruedTill() {
        return this.accruedTill;
    }

    public LocalDate fetchInterestRecalculateFromDate() {
        LocalDate interestRecalculatedOnV2 = null;
        if (this.interestRecalculatedOn == null) {
            interestRecalculatedOnV2 = getDisbursementDate();
        } else {
            interestRecalculatedOnV2 = this.interestRecalculatedOn;
        }
        return interestRecalculatedOnV2;
    }

    @SuppressWarnings({ "squid:S3776" })
    private void updateLoanOutstandingBalances() {
        Money outstanding = Money.zero(getCurrency());
        List<LoanTransaction> loanTransactionsV2 = retrieveListOfTransactionsExcludeAccruals();
        for (LoanTransaction loanTransaction : loanTransactionsV2) {
            if (loanTransaction.isDisbursement() || loanTransaction.isIncomePosting()) {
                outstanding = outstanding.plus(loanTransaction.getAmount(getCurrency()))
                        .minus(loanTransaction.getOverPaymentPortion(getCurrency()));
                loanTransaction.updateOutstandingLoanBalance(outstanding.getAmount());
            } else if (loanTransaction.isChargeback() || loanTransaction.isCreditBalanceRefund()) {
                Money transactionOutstanding = loanTransaction.getAmount(getCurrency());
                if (!loanTransaction.getOverPaymentPortion(getCurrency()).isZero()) {
                    transactionOutstanding = loanTransaction.getAmount(getCurrency())
                            .minus(loanTransaction.getOverPaymentPortion(getCurrency()));
                    if (transactionOutstanding.isLessThanZero()) {
                        transactionOutstanding = Money.zero(getCurrency());
                    }
                }
                outstanding = outstanding.plus(transactionOutstanding);
                loanTransaction.updateOutstandingLoanBalance(outstanding.getAmount());

            } else {
                if (this.loanInterestRecalculationDetails != null
                        && this.loanInterestRecalculationDetails.isCompoundingToBePostedAsTransaction()
                        && !loanTransaction.isRepaymentAtDisbursement()) {
                    outstanding = outstanding.minus(loanTransaction.getAmount(getCurrency()));
                } else {
                    outstanding = outstanding.minus(loanTransaction.getPrincipalPortion(getCurrency()));
                }
                loanTransaction.updateOutstandingLoanBalance(outstanding.getAmount());
            }
        }
    }

    public String transactionProcessingStrategy() {
        return this.transactionProcessingStrategyCode;
    }

    public boolean isNpa() {
        return this.isNpa;
    }

    /**
     * @return List of loan repayments schedule objects
     **/
    public List<LoanRepaymentScheduleInstallment> getRepaymentScheduleInstallments() {
        return this.repaymentScheduleInstallments;
    }

    public List<LoanRepaymentScheduleInstallment> getRepaymentScheduleInstallmentsIgnoringTotalGrace() {
        return this.repaymentScheduleInstallments.stream()
                .filter(installment -> installment.getInstallmentNumber() != null && installment.getInstallmentNumber() > 0).toList();
    }

    public Integer getLoanRepaymentScheduleInstallmentsSize() {
        return this.repaymentScheduleInstallments.size();
    }

    public void addLoanRepaymentScheduleInstallment(final LoanRepaymentScheduleInstallment installment) {
        installment.updateLoan(this);
        this.repaymentScheduleInstallments.add(installment);
    }

    public void removeLoanRepaymentScheduleInstallment(final Integer installmentNumber) {
        final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = fetchRepaymentScheduleInstallment(installmentNumber);
        if (loanRepaymentScheduleInstallment != null) {
            loanRepaymentScheduleInstallment.updateLoan(null);
            loanRepaymentScheduleInstallment.getInstallmentCharges().clear();
            loanRepaymentScheduleInstallment.getLoanTransactionToRepaymentScheduleMappings().clear();
            this.repaymentScheduleInstallments.remove(loanRepaymentScheduleInstallment);
        }
    }

    /**
     * @return Loan product minimum repayments schedule related detail
     **/
    public LoanProductRelatedDetail getLoanRepaymentScheduleDetail() {
        return this.loanRepaymentScheduleDetail;
    }

    /**
     * @return Loan Fixed Emi amount
     **/
    public BigDecimal getFixedEmiAmount() {
        return this.fixedEmiAmount;
    }

    /**
     * @return maximum outstanding loan balance
     **/
    public BigDecimal getMaxOutstandingLoanBalance() {
        return this.maxOutstandingLoanBalance;
    }

    /**
     * @param rescheduleFromDate
     *            the date from which the rescheduling is to be done
     * @return a schedule installment which contains the rescheduleFromDate provided
     **/
    public LoanRepaymentScheduleInstallment getInstallmentByScheduleFromDate(LocalDate rescheduleFromDate) {
        LoanRepaymentScheduleInstallment installment = null;
        if (rescheduleFromDate != null) {
            List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
            for (LoanRepaymentScheduleInstallment repaymentScheduleInstallment : installments) {
                if (!repaymentScheduleInstallment.isObligationsMet() && !repaymentScheduleInstallment.hasOverdueCharges()
                        && (DateUtils.isBefore(rescheduleFromDate, repaymentScheduleInstallment.getDueDate())
                                || DateUtils.isEqual(rescheduleFromDate, repaymentScheduleInstallment.getDueDate()))) {
                    boolean isOverdueInstallment = isOverdueInstallment(repaymentScheduleInstallment);
                    if (!isOverdueInstallment) {
                        installment = repaymentScheduleInstallment;
                        break;
                    }
                }

            }
        }
        return installment;
    }

    private boolean isOverdueInstallment(LoanRepaymentScheduleInstallment repaymentScheduleInstallment) {
        final LocalDate fromDate = repaymentScheduleInstallment.getFromDate();
        boolean isOverdueInstallment = false;
        Collection<LoanCharge> loanCharges = this.getLoanCharges();
        for (LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isOverdueInstallmentCharge() && DateUtils.isAfter(loanCharge.getDueLocalDate(), fromDate)
                    && loanCharge.isActive()) {
                isOverdueInstallment = true;
            }
        }
        return isOverdueInstallment;
    }

    /**
     * @return loan disbursement data
     **/
    public List<DisbursementData> getDisbursmentData() {
        Iterator<LoanDisbursementDetails> iterator = this.getDisbursementDetails().iterator();
        List<DisbursementData> disbursementData = new ArrayList<>();

        while (iterator.hasNext()) {
            LoanDisbursementDetails loanDisbursementDetails = iterator.next();

            LocalDate expectedDisbursementDateV2 = null;
            LocalDate actualDisbursementDateV2 = null;

            if (loanDisbursementDetails.expectedDisbursementDate() != null) {
                expectedDisbursementDateV2 = loanDisbursementDetails.expectedDisbursementDate();
            }

            if (loanDisbursementDetails.actualDisbursementDate() != null) {
                actualDisbursementDateV2 = loanDisbursementDetails.actualDisbursementDate();
            }
            BigDecimal waivedChargeAmount = null;
            disbursementData.add(new DisbursementData(loanDisbursementDetails.getId(), expectedDisbursementDateV2, actualDisbursementDateV2,
                    loanDisbursementDetails.principal(), this.netDisbursalAmount, null, null, waivedChargeAmount));
        }

        return disbursementData;
    }

    @SuppressWarnings({ "unused" })
    public LoanApplicationTerms getLoanApplicationTerms(final ApplicationCurrency applicationCurrency,
            final CalendarInstance restCalendarInstance, CalendarInstance compoundingCalendarInstance, final Calendar loanCalendar,
            final FloatingRateDTO floatingRateDTO, final boolean isSkipRepaymentonmonthFirst, final Integer numberofdays,
            final HolidayDetailDTO holidayDetailDTO) {
        LoanProduct loanProductV2 = loanProduct();
        final MonetaryCurrency currency = this.loanRepaymentScheduleDetail.getCurrency();
        final Integer loanTermFrequency = getTermFrequency();
        final PeriodFrequencyType loanTermPeriodFrequencyType = this.loanRepaymentScheduleDetail.getInterestPeriodFrequencyType();
        NthDayType nthDayType = null;
        DayOfWeekType dayOfWeekType = null;
        if (loanCalendar != null) {
            nthDayType = CalendarUtils.getRepeatsOnNthDayOfMonth(loanCalendar.getRecurrence());
            CalendarWeekDaysType getRepeatsOnDay = CalendarUtils.getRepeatsOnDay(loanCalendar.getRecurrence());
            Integer getRepeatsOnDayValue = null;
            if (getRepeatsOnDay != null) {
                getRepeatsOnDayValue = getRepeatsOnDay.getValue();
            }
            if (getRepeatsOnDayValue != null) {
                dayOfWeekType = DayOfWeekType.fromInt(getRepeatsOnDayValue);
            }
        }

        final Integer numberOfRepayments = this.loanRepaymentScheduleDetail.getNumberOfRepayments();
        final Integer repayEvery = this.loanRepaymentScheduleDetail.getRepayEvery();
        final PeriodFrequencyType repaymentPeriodFrequencyType = this.loanRepaymentScheduleDetail.getRepaymentPeriodFrequencyType();

        final AmortizationMethod amortizationMethod = this.loanRepaymentScheduleDetail.getAmortizationMethod();

        final InterestMethod interestMethod = this.loanRepaymentScheduleDetail.getInterestMethod();
        final InterestCalculationPeriodMethod interestCalculationPeriodMethod = this.loanRepaymentScheduleDetail
                .getInterestCalculationPeriodMethod();

        final BigDecimal interestRatePerPeriod = this.loanRepaymentScheduleDetail.getNominalInterestRatePerPeriod();
        final PeriodFrequencyType interestRatePeriodFrequencyType = this.loanRepaymentScheduleDetail.getInterestPeriodFrequencyType();

        BigDecimal annualNominalInterestRate = this.loanRepaymentScheduleDetail.getAnnualNominalInterestRate();
        final Money principalMoney = this.loanRepaymentScheduleDetail.getPrincipal();

        final LocalDate expectedDisbursementDateV2 = getExpectedDisbursedOnLocalDate();
        final LocalDate repaymentsStartingFromDate = getExpectedFirstRepaymentOnDate();
        final Integer graceOnPrincipalPayment = this.loanRepaymentScheduleDetail.graceOnPrincipalPayment();
        final Integer graceOnInterestPayment = this.loanRepaymentScheduleDetail.graceOnInterestPayment();
        final Integer graceOnChargesPayment = this.loanRepaymentScheduleDetail.graceOnChargesPayment();
        final Integer graceOnInterestCharged = this.loanRepaymentScheduleDetail.graceOnInterestCharged();
        final LocalDate interestChargedFromDateV2 = getInterestChargedFromDate();
        final Integer graceOnArrearsAgeing = this.loanRepaymentScheduleDetail.getGraceOnDueDate();

        final Money inArrearsToleranceMoney = this.loanRepaymentScheduleDetail.getInArrearsTolerance();
        final BigDecimal emiAmount = getFixedEmiAmount();
        final BigDecimal maxOutstandingBalance = getMaxOutstandingLoanBalance();

        final List<DisbursementData> disbursementData = getDisbursmentData();

        CalendarHistoryDataWrapper calendarHistoryDataWrapper = null;
        if (loanCalendar != null) {
            Set<CalendarHistory> calendarHistory = loanCalendar.getCalendarHistory();
            calendarHistoryDataWrapper = new CalendarHistoryDataWrapper(calendarHistory);
        }

        RecalculationFrequencyType recalculationFrequencyType = null;
        InterestRecalculationCompoundingMethod compoundingMethod = null;
        RecalculationFrequencyType compoundingFrequencyType = null;
        LoanRescheduleStrategyMethod rescheduleStrategyMethod = null;
        RepaymentStartDateType repaymentStartDateType = loanProductV2.getRepaymentStartDateType();
        boolean allowCompoundingOnEod = false;
        if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            recalculationFrequencyType = this.loanInterestRecalculationDetails.getRestFrequencyType();
            compoundingMethod = this.loanInterestRecalculationDetails.getInterestRecalculationCompoundingMethod();
            compoundingFrequencyType = this.loanInterestRecalculationDetails.getCompoundingFrequencyType();
            rescheduleStrategyMethod = this.loanInterestRecalculationDetails.getRescheduleStrategyMethod();
            allowCompoundingOnEod = this.loanInterestRecalculationDetails.allowCompoundingOnEod();
        }

        List<LoanTermVariationsData> loanTermVariationsV2 = new ArrayList<>();
        annualNominalInterestRate = constructFloatingInterestRates(annualNominalInterestRate, floatingRateDTO, loanTermVariationsV2);

        return LoanApplicationTerms.assembleFrom(applicationCurrency, loanTermFrequency, loanTermPeriodFrequencyType, nthDayType,
                dayOfWeekType, expectedDisbursementDateV2, repaymentsStartingFromDate, repaymentsStartingFromDate, inArrearsToleranceMoney,
                this.loanRepaymentScheduleDetail, loanProductV2.isMultiDisburseLoan(), emiAmount, disbursementData, maxOutstandingBalance,
                interestChargedFromDateV2, this.loanProduct.getPrincipalThresholdForLastInstallment(),
                this.loanProduct.getInstallmentAmountInMultiplesOf(), recalculationFrequencyType, restCalendarInstance, compoundingMethod,
                compoundingCalendarInstance, compoundingFrequencyType, this.loanProduct.preCloseInterestCalculationStrategy(),
                rescheduleStrategyMethod, loanCalendar, getApprovedPrincipal(), annualNominalInterestRate, loanTermVariationsV2,
                calendarHistoryDataWrapper, numberofdays, isSkipRepaymentonmonthFirst, holidayDetailDTO, allowCompoundingOnEod, false,
                false, this.fixedPrincipalPercentagePerInstallment, false, repaymentStartDateType, getSubmittedOnDate());
    }

    /**
     * @return Loan summary embedded object
     **/
    public LoanSummary getLoanSummary() {
        return this.summary;
    }

    public void updateRescheduledByUser(AppUser rescheduledByUser) {
        this.rescheduledByUser = rescheduledByUser;
    }

    public LoanProductRelatedDetail getLoanProductRelatedDetail() {
        return this.loanRepaymentScheduleDetail;
    }

    public void updateNumberOfRepayments(Integer numberOfRepayments) {
        this.loanRepaymentScheduleDetail.updateNumberOfRepayments(numberOfRepayments);
    }

    public void updateRescheduledOnDate(LocalDate rescheduledOnDate) {

        if (rescheduledOnDate != null) {
            this.rescheduledOnDate = rescheduledOnDate;
        }
    }

    public void updateTermFrequency(Integer termFrequency) {

        if (termFrequency != null) {
            this.termFrequency = termFrequency;
        }
    }

    public boolean isFeeCompoundingEnabledForInterestRecalculation() {
        boolean isEnabled = false;
        if (this.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            isEnabled = this.loanInterestRecalculationDetails.getInterestRecalculationCompoundingMethod().isFeeCompoundingEnabled();
        }
        return isEnabled;
    }

    public String getAccountNumber() {
        return this.accountNumber;
    }

    public ExternalId getExternalId() {
        return this.externalId;
    }

    public Client getClient() {
        return this.client;
    }

    public Boolean shouldCreateStandingInstructionAtDisbursement() {
        return this.createStandingInstructionAtDisbursement != null && this.createStandingInstructionAtDisbursement;
    }

    public Collection<LoanCharge> getLoanCharges(LocalDate dueDate) {
        Collection<LoanCharge> loanCharges = new ArrayList<>();

        for (LoanCharge loanCharge : charges) {

            if (loanCharge.getDueLocalDate() != null && loanCharge.getDueLocalDate().equals(dueDate)) {
                loanCharges.add(loanCharge);
            }
        }

        return loanCharges;
    }

    public void setGuaranteeAmount(BigDecimal guaranteeAmountDerived) {
        this.guaranteeAmountDerived = guaranteeAmountDerived;
    }

    public void updateGuaranteeAmount(BigDecimal guaranteeAmount) {
        this.guaranteeAmountDerived = getGuaranteeAmount().add(guaranteeAmount);
    }

    public BigDecimal getGuaranteeAmount() {
        return this.guaranteeAmountDerived == null ? BigDecimal.ZERO : this.guaranteeAmountDerived;
    }

    public void creditBalanceRefund(LoanTransaction newCreditBalanceRefundTransaction,
            LoanLifecycleStateMachine defaultLoanLifecycleStateMachine, List<Long> existingTransactionIds,
            List<Long> existingReversedTransactionIds) {
        validateAccountStatus(LoanEvent.LOAN_CREDIT_BALANCE_REFUND);

        validateRefundDateIsAfterLastRepayment(newCreditBalanceRefundTransaction.getTransactionDate());

        if (!newCreditBalanceRefundTransaction.isGreaterThanZeroAndLessThanOrEqualTo(this.totalOverpaid)) {
            final String errorMessage = "Transaction Amount ("
                    + newCreditBalanceRefundTransaction.getAmount(getCurrency()).getAmount().toString()
                    + ") must be > zero and <= Overpaid amount (" + this.totalOverpaid.toString() + ").";
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final ApiParameterError error = ApiParameterError.parameterError(
                    "error.msg.transactionAmount.invalid.must.be.>zero.and<=overpaidamount", errorMessage, "transactionAmount",
                    newCreditBalanceRefundTransaction.getAmount(getCurrency()));
            dataValidationErrors.add(error);

            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }

        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());

        this.loanTransactions.add(newCreditBalanceRefundTransaction);

        updateLoanSummaryDerivedFields();

        if (this.totalOverpaid == null || BigDecimal.ZERO.compareTo(this.totalOverpaid) == 0) {
            this.overpaidOnDate = null;
            this.closedOnDate = newCreditBalanceRefundTransaction.getTransactionDate();
            defaultLoanLifecycleStateMachine.transition(LoanEvent.LOAN_CREDIT_BALANCE_REFUND, this);
        }

    }

    public ChangedTransactionDetail makeRefundForActiveLoan(final LoanTransaction loanTransaction,
            final LoanLifecycleStateMachine loanLifecycleStateMachine, final List<Long> existingTransactionIds,
            final List<Long> existingReversedTransactionIds, final boolean allowTransactionsOnHoliday, final List<Holiday> holidays,
            final WorkingDays workingDays, final boolean allowTransactionsOnNonWorkingDay) {

        validateAccountStatus(LoanEvent.LOAN_REFUND);
        validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.LOAN_REFUND, loanTransaction.getTransactionDate());

        validateRefundDateIsAfterLastRepayment(loanTransaction.getTransactionDate());

        validateRepaymentDateIsOnHoliday(loanTransaction.getTransactionDate(), allowTransactionsOnHoliday, holidays);
        validateRepaymentDateIsOnNonWorkingDay(loanTransaction.getTransactionDate(), workingDays, allowTransactionsOnNonWorkingDay);

        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());

        return handleRefundTransaction(loanTransaction, loanLifecycleStateMachine, null);

    }

    private void validateRefundDateIsAfterLastRepayment(final LocalDate refundTransactionDate) {
        final LocalDate possibleNextRefundDate = possibleNextRefundDate();

        if (possibleNextRefundDate == null || DateUtils.isBefore(refundTransactionDate, possibleNextRefundDate)) {
            throw new InvalidRefundDateException(refundTransactionDate.toString());
        }
    }

    private ChangedTransactionDetail handleRefundTransaction(final LoanTransaction loanTransaction,
            final LoanLifecycleStateMachine loanLifecycleStateMachine, final LoanTransaction adjustedTransaction) {
        ChangedTransactionDetail changedTransactionDetail = null;

        loanLifecycleStateMachine.transition(LoanEvent.LOAN_REFUND, this);

        loanTransaction.updateLoan(this);

        if (getStatus().isOverpaid() || getStatus().isClosed()) {
            final String errorMessage = "This refund option is only for active loans ";
            throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, "is.exceeding.overpaid.amount", errorMessage,
                    this.totalOverpaid, loanTransaction.getAmount(getCurrency()).getAmount());
        } else if (this.getTotalPaidInRepayments().isZero()) {
            final String errorMessage = "Cannot refund when no payment has been made";
            throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, "no.payment.yet.made.for.loan", errorMessage);
        }

        if (loanTransaction.isNotZero(loanCurrency())) {
            addLoanTransaction(loanTransaction);
        }
        if (loanTransaction.isNotRefundForActiveLoan()) {
            final String errorMessage = "A transaction of type refund was expected but not received.";
            throw new InvalidLoanTransactionTypeException(Loan.TRANSACTION_PARAM, "is.not.a.refund.transaction", errorMessage);
        }

        final LocalDate loanTransactionDate = loanTransaction.getTransactionDate();
        if (DateUtils.isBefore(loanTransactionDate, getDisbursementDate())) {
            final String errorMessage = "The transaction date cannot be before the loan disbursement date: "
                    + getDisbursementDate().toString();
            throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, "cannot.be.before.disbursement.date", errorMessage,
                    loanTransactionDate, getDisbursementDate());
        }

        if (DateUtils.isDateInTheFuture(loanTransactionDate)) {
            final String errorMessage = "The transaction date cannot be in the future.";
            throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, Loan.ERROR_MESSAGE_CANNOT_BE_A_FUTURE_DATE, errorMessage,
                    loanTransactionDate);
        }

        if (this.loanProduct.isMultiDisburseLoan() && adjustedTransaction == null) {
            BigDecimal totalDisbursed = getDisbursedAmount();
            BigDecimal totalPrincipalAdjusted = this.summary.getTotalPrincipalAdjustments();
            BigDecimal totalPrincipalCredited = totalDisbursed.add(totalPrincipalAdjusted);
            if (totalPrincipalCredited.compareTo(this.summary.getTotalPrincipalRepaid()) < 0) {
                final String errorMessage = "The transaction amount cannot exceed threshold.";
                throw new InvalidLoanStateTransitionException(Loan.TRANSACTION_PARAM, Loan.ERROR_MESSAGE_AMOUNT_EXCEED_THRESHOLD,
                        errorMessage);
            }
        }

        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);

        // If is a refund
        if (adjustedTransaction == null) {
            loanRepaymentScheduleTransactionProcessor.processLatestTransaction(loanTransaction, new TransactionCtx(getCurrency(),
                    getRepaymentScheduleInstallments(), getActiveCharges(), new MoneyHolder(getTotalOverpaidAsMoney())));
        } else {
            final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retrieveListOfTransactionsPostDisbursement();
            changedTransactionDetail = loanRepaymentScheduleTransactionProcessor.reprocessLoanTransactions(getDisbursementDate(),
                    allNonContraTransactionsPostDisbursement, getCurrency(), getRepaymentScheduleInstallments(), getActiveCharges());
            for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                mapEntry.getValue().updateLoan(this);
            }

        }

        updateLoanSummaryDerivedFields();

        doPostLoanTransactionChecks(loanTransaction.getTransactionDate(), loanLifecycleStateMachine);

        return changedTransactionDetail;
    }

    public void handleChargebackTransaction(final LoanTransaction chargebackTransaction,
            final LoanLifecycleStateMachine loanLifecycleStateMachine) {

        if (!chargebackTransaction.isChargeback()) {
            final String errorMessage = "A transaction of type chargeback was expected but not received.";
            throw new InvalidLoanTransactionTypeException(Loan.TRANSACTION_PARAM, "is.not.a.chargeback.transaction", errorMessage);
        }

        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.transactionProcessorFactory
                .determineProcessor(this.transactionProcessingStrategyCode);

        addLoanTransaction(chargebackTransaction);
        loanRepaymentScheduleTransactionProcessor.processLatestTransaction(chargebackTransaction, new TransactionCtx(getCurrency(),
                getRepaymentScheduleInstallments(), getActiveCharges(), new MoneyHolder(getTotalOverpaidAsMoney())));

        updateLoanSummaryDerivedFields();
        if (!doPostLoanTransactionChecks(chargebackTransaction.getTransactionDate(), loanLifecycleStateMachine)) {
            loanLifecycleStateMachine.transition(LoanEvent.LOAN_CHARGEBACK, this);
        }
    }

    public LocalDate possibleNextRefundDate() {

        final LocalDate now = DateUtils.getBusinessLocalDate();

        LocalDate lastTransactionDate = null;
        for (final LoanTransaction transaction : this.loanTransactions) {
            if ((transaction.isRepaymentLikeType() || transaction.isRefundForActiveLoan() || transaction.isCreditBalanceRefund())
                    && transaction.isNonZero() && transaction.isNotReversed()) {
                lastTransactionDate = transaction.getTransactionDate();
            }
        }

        return lastTransactionDate == null ? now : lastTransactionDate;
    }

    private LocalDate getActualDisbursementDate(final LoanCharge loanCharge) {
        LocalDate actualDisbursementDateV2 = this.actualDisbursementDate;
        if (loanCharge.isDueAtDisbursement() && loanCharge.isActive()) {
            LoanTrancheDisbursementCharge trancheDisbursementCharge = loanCharge.getTrancheDisbursementCharge();
            if (trancheDisbursementCharge != null) {
                LoanDisbursementDetails details = trancheDisbursementCharge.getloanDisbursementDetails();
                actualDisbursementDateV2 = details.actualDisbursementDate();
            }
        }
        return actualDisbursementDateV2;
    }

    public void addTrancheLoanCharge(final Charge charge) {
        final List<Charge> appliedCharges = new ArrayList<>();
        for (final LoanTrancheCharge loanTrancheCharge : this.trancheCharges) {
            appliedCharges.add(loanTrancheCharge.getCharge());
        }
        if (!appliedCharges.contains(charge)) {
            this.trancheCharges.add(new LoanTrancheCharge(charge, this));
        }
    }

    public Map<String, Object> undoLastDisbursal(ScheduleGeneratorDTO scheduleGeneratorDTO, List<Long> existingTransactionIds,
            List<Long> existingReversedTransactionIds, Loan loan) {
        validateAccountStatus(LoanEvent.LOAN_DISBURSAL_UNDO_LAST);
        validateActivityNotBeforeClientOrGroupTransferDate(LoanEvent.LOAN_DISBURSAL_UNDO_LAST, getDisbursementDate());

        final Map<String, Object> actualChanges = new LinkedHashMap<>();
        List<LoanTransaction> loanTransactionsV2 = retrieveListOfTransactionsByType(LoanTransactionType.DISBURSEMENT);
        loanTransactionsV2.sort(Comparator.comparing(LoanTransaction::getId));
        final LoanTransaction lastDisbursalTransaction = loanTransactionsV2.get(loanTransactionsV2.size() - 1);
        final LocalDate lastTransactionDate = lastDisbursalTransaction.getTransactionDate();

        existingTransactionIds.addAll(findExistingTransactionIds());
        existingReversedTransactionIds.addAll(findExistingReversedTransactionIds());

        loanTransactionsV2 = retrieveListOfTransactionsExcludeAccruals();
        Collections.reverse(loanTransactionsV2);
        for (final LoanTransaction previousTransaction : loanTransactionsV2) {
            if (DateUtils.isBefore(lastTransactionDate, previousTransaction.getTransactionDate())
                    && (previousTransaction.isRepaymentLikeType() || previousTransaction.isWaiver()
                            || previousTransaction.isChargePayment())) {
                throw new UndoLastTrancheDisbursementException(previousTransaction.getId());
            }
            if (previousTransaction.getId().compareTo(lastDisbursalTransaction.getId()) < 0) {
                break;
            }
        }
        final LoanDisbursementDetails disbursementDetail = loan.getDisbursementDetails(lastTransactionDate,
                lastDisbursalTransaction.getAmount());
        updateLoanToLastDisbursalState(disbursementDetail);
        for (Iterator<LoanTermVariations> iterator = this.loanTermVariations.iterator(); iterator.hasNext();) {
            LoanTermVariations loanTermVariationsV2 = iterator.next();
            if ((loanTermVariationsV2.getTermType().isDueDateVariation()
                    && DateUtils.isAfter(loanTermVariationsV2.fetchDateValue(), lastTransactionDate))
                    || (loanTermVariationsV2.getTermType().isEMIAmountVariation()
                            && DateUtils.isEqual(loanTermVariationsV2.getTermApplicableFrom(), lastTransactionDate))
                    || DateUtils.isAfter(loanTermVariationsV2.getTermApplicableFrom(), lastTransactionDate)) {
                iterator.remove();
            }
        }
        reverseExistingTransactionsTillLastDisbursal(lastDisbursalTransaction);
        loan.recalculateScheduleFromLastTransaction(scheduleGeneratorDTO);
        actualChanges.put("undolastdisbursal", "true");
        actualChanges.put("disbursedAmount", this.getDisbursedAmount());
        updateLoanSummaryDerivedFields();

        doPostLoanTransactionChecks(getLastUserTransactionDate(), loanLifecycleStateMachine);

        return actualChanges;
    }

    /**
     * Reverse only disbursement, accruals, and repayments at disbursal transactions
     *
     * @param lastDisbursalTransaction
     * @return
     */
    public void reverseExistingTransactionsTillLastDisbursal(LoanTransaction lastDisbursalTransaction) {
        if (lastDisbursalTransaction != null && lastDisbursalTransaction.getId() != null) {
            for (final LoanTransaction transaction : this.loanTransactions) {
                final Long transactionId = transaction.getId();
                if (transactionId != null
                        && !DateUtils.isBefore(transaction.getTransactionDate(), lastDisbursalTransaction.getTransactionDate())
                        && transactionId.compareTo(lastDisbursalTransaction.getId()) >= 0
                        && Boolean.TRUE.equals(transaction.isAllowTypeTransactionAtTheTimeOfLastUndo())) {
                    transaction.reverse();
                }

            }
        }
        if (isAutoRepaymentForDownPaymentEnabled()) {
            // identify down-payment amount for the transaction
            BigDecimal disbursedAmountPercentageForDownPayment = this.loanRepaymentScheduleDetail
                    .getDisbursedAmountPercentageForDownPayment();
            Money downPaymentMoney = Money.of(getCurrency(),
                    MathUtil.percentageOf(lastDisbursalTransaction.getAmount(), disbursedAmountPercentageForDownPayment, 19));

            // find the latest matching down-payment transaction based on date, amount and transaction type
            Optional<LoanTransaction> downPaymentTransaction = this.loanTransactions.stream()
                    .filter(tr -> tr.getTransactionDate().equals(lastDisbursalTransaction.getTransactionDate())
                            && tr.getTypeOf().isDownPayment() && tr.getAmount().compareTo(downPaymentMoney.getAmount()) == 0)
                    .max(Comparator.comparing(LoanTransaction::getId));

            // reverse the down-payment transaction
            downPaymentTransaction.ifPresent(LoanTransaction::reverse);
        }
    }

    private void updateLoanToLastDisbursalState(LoanDisbursementDetails disbursementDetail) {
        for (final LoanCharge charge : getActiveCharges()) {
            if (charge.isOverdueInstallmentCharge()) {
                charge.setActive(false);
            } else if (charge.isTrancheDisbursementCharge() && disbursementDetail.getDisbursementDate()
                    .equals(charge.getTrancheDisbursementCharge().getloanDisbursementDetails().actualDisbursementDate())) {
                charge.resetToOriginal(loanCurrency());
            }
        }
        this.loanRepaymentScheduleDetail.setPrincipal(getDisbursedAmount().subtract(disbursementDetail.principal()));
        disbursementDetail.updateActualDisbursementDate(null);
        disbursementDetail.reverse();
        updateLoanSummaryDerivedFields();
    }

    public Boolean getIsFloatingInterestRate() {
        return this.isFloatingInterestRate;
    }

    public BigDecimal getInterestRateDifferential() {
        return this.interestRateDifferential;
    }

    public void setIsFloatingInterestRate(Boolean isFloatingInterestRate) {
        this.isFloatingInterestRate = isFloatingInterestRate;
    }

    public void setInterestRateDifferential(BigDecimal interestRateDifferential) {
        this.interestRateDifferential = interestRateDifferential;
    }

    public List<LoanTermVariations> getLoanTermVariations() {
        return this.loanTermVariations;
    }

    private int adjustNumberOfRepayments() {
        int repaymetsForAdjust = 0;
        for (LoanTermVariations loanTermVariationsV2 : this.loanTermVariations) {
            if (loanTermVariationsV2.getTermType().isInsertInstallment()) {
                repaymetsForAdjust++;
            } else if (loanTermVariationsV2.getTermType().isDeleteInstallment()) {
                repaymetsForAdjust--;
            }
            if (loanTermVariationsV2.getTermType().isExtendRepaymentPeriod()) {
                repaymetsForAdjust = repaymetsForAdjust + loanTermVariationsV2.getTermValue().intValue();
            }
        }
        return repaymetsForAdjust;
    }

    public int fetchNumberOfInstallmensAfterExceptions() {
        if (!this.repaymentScheduleInstallments.isEmpty()) {
            List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallmentsIgnoringTotalGrace();
            int numberOfInstallments = 0;
            for (final LoanRepaymentScheduleInstallment installment : installments) {
                if (!installment.isRecalculatedInterestComponent()) {
                    numberOfInstallments++;
                }
            }
            return numberOfInstallments;
        }
        return this.repaymentScheduleDetail().getNumberOfRepayments() + adjustNumberOfRepayments();
    }

    public int fetchUnpaidNumberOfInstallments(Integer applicableFromInstallment) {

        if (!this.repaymentScheduleInstallments.isEmpty()) {
            List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
            int numberOfInstallments = 0;
            for (final LoanRepaymentScheduleInstallment installment : installments) {
                if (applicableFromInstallment == null) {
                    if (!installment.isObligationsMet()) {
                        numberOfInstallments++;
                    }
                } else {
                    numberOfInstallments++;
                }
            }
            return numberOfInstallments;
        }
        return this.repaymentScheduleDetail().getNumberOfRepayments() + adjustNumberOfRepayments();
    }

    public void setExpectedFirstRepaymentOnDate(LocalDate expectedFirstRepaymentOnDate) {
        this.expectedFirstRepaymentOnDate = expectedFirstRepaymentOnDate;
    }

    /*
     * get the next repayment LocalDate for rescheduling at the time of disbursement
     */
    public LocalDate getNextPossibleRepaymentDateForRescheduling() {
        List<LoanDisbursementDetails> loanDisbursementDetails = getDisbursementDetails();
        LocalDate nextRepaymentDate = DateUtils.getBusinessLocalDate();
        for (LoanDisbursementDetails loanDisbursementDetail : loanDisbursementDetails) {
            if (loanDisbursementDetail.actualDisbursementDate() == null) {
                List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
                for (final LoanRepaymentScheduleInstallment installment : installments) {
                    if (!DateUtils.isBefore(installment.getDueDate(), loanDisbursementDetail.expectedDisbursementDateAsLocalDate())
                            && installment.isNotFullyPaidOff()) {
                        nextRepaymentDate = installment.getDueDate();
                        break;
                    }
                }
                break;
            }
        }
        return nextRepaymentDate;
    }

    public BigDecimal getDerivedAmountForCharge(final LoanCharge loanCharge) {
        BigDecimal amount = BigDecimal.ZERO;
        if (isMultiDisburmentLoan() && loanCharge.getCharge().getChargeTimeType().equals(ChargeTimeType.DISBURSEMENT.getValue())) {
            amount = getApprovedPrincipal();
        } else {
            // If charge type is specified due date and loan is multi disburment
            // loan.
            // Then we need to get as of this loan charge due date how much
            // amount disbursed.
            if (loanCharge.isSpecifiedDueDate() && this.isMultiDisburmentLoan()) {
                for (final LoanDisbursementDetails loanDisbursementDetails : this.getDisbursementDetails()) {
                    if (!DateUtils.isAfter(loanDisbursementDetails.expectedDisbursementDate(), loanCharge.getDueDate())) {
                        amount = amount.add(loanDisbursementDetails.principal());
                    }
                }
            } else {
                amount = getPrincipal().getAmount();
            }
        }
        return amount;
    }

    public List<PostDatedChecks> getPostDatedChecks() {
        return this.postDatedChecks;
    }

    public void updateWriteOffReason(CodeValue writeOffReason) {
        this.writeOffReason = writeOffReason;
    }

    public Group getGroup() {
        return group;
    }

    public LoanProduct getLoanProduct() {
        return loanProduct;
    }

    public LoanRepaymentScheduleInstallment fetchLoanForeclosureDetail(final LocalDate closureDate,
            final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        Money[] receivables = retrieveIncomeOutstandingTillDate(closureDate, scheduleGeneratorDTO);
        Money totalPrincipal = Money.of(getCurrency(), this.getLoanSummary().getTotalPrincipalOutstanding());
        totalPrincipal = totalPrincipal.minus(receivables[3]);
        final LocalDate currentDate = DateUtils.getBusinessLocalDate();
        return new LoanRepaymentScheduleInstallment(null, 0, currentDate, currentDate, totalPrincipal.getAmount(),
                receivables[0].getAmount(), receivables[1].getAmount(), receivables[2].getAmount(), false, null);
    }

    @SuppressWarnings({ "squid:S3776" })
    public LoanRepaymentScheduleInstallment fetchLoanSpecialWriteOffDetail(final LocalDate writtenOffOnDate) {
        final MonetaryCurrency currency = getCurrency();
        Money interest = Money.zero(currency);
        Money paidFromFutureInstallments = Money.zero(currency);
        Money fee = Money.zero(currency);
        Money penalty = Money.zero(currency);
        final List<LoanChargeData> currentOutstandingLoanCharges = new ArrayList<>();
        for (LoanCharge loanCharge : this.charges) {
            if (loanCharge.isActive() && !loanCharge.isDueAtDisbursement()) {
                final Charge charge = loanCharge.getCharge();
                final Long chargeId = charge.getId();
                if (currentOutstandingLoanCharges.stream().noneMatch(loanChargeData -> loanChargeData.getChargeId().equals(chargeId))) {
                    final LoanChargeData loanChargeData = LoanChargeData.builder().chargeId(chargeId).name(charge.getName())
                            .amountOutstanding(BigDecimal.ZERO).penalty(loanCharge.isPenaltyCharge()).build();
                    currentOutstandingLoanCharges.add(loanChargeData);
                }
            }
        }

        for (final LoanRepaymentScheduleInstallment installment : this.repaymentScheduleInstallments) {
            if (!DateUtils.isBefore(writtenOffOnDate, installment.getDueDate())) {
                interest = interest.plus(installment.getInterestOutstanding(currency));
                penalty = penalty.plus(installment.getPenaltyChargesOutstanding(currency));
                fee = fee.plus(installment.getFeeChargesOutstanding(currency));
                this.incrementOutstandingInstallmentChargeBalances(installment.getInstallmentNumber(), currentOutstandingLoanCharges);
                this.incrementOutstandingPenaltyChargeBalances(installment.getInstallmentNumber(), currentOutstandingLoanCharges);
            } else if (DateUtils.isAfter(writtenOffOnDate, installment.getFromDate())) {
                int totalPeriodDays = Math.toIntExact(ChronoUnit.DAYS.between(installment.getFromDate(), installment.getDueDate()));
                int tillDays = Math.toIntExact(ChronoUnit.DAYS.between(installment.getFromDate(), writtenOffOnDate));
                Money interestForCurrentPeriod = Money.of(getCurrency(), BigDecimal.valueOf(
                        calculateInterestForDays(totalPeriodDays, installment.getInterestCharged(getCurrency()).getAmount(), tillDays)));
                Money interestAccountedForCurrentPeriod = installment.getInterestWaived(getCurrency())
                        .plus(installment.getInterestPaid(getCurrency())).plus(installment.getInterestWrittenOff(getCurrency()));
                Money feeForCurrentPeriod = installment.getFeeChargesCharged(getCurrency());
                Money feeAccountedForCurrentPeriod = installment.getFeeChargesWaived(getCurrency())
                        .plus(installment.getFeeChargesPaid(getCurrency())).plus(installment.getFeeChargesWrittenOff(getCurrency()));
                this.incrementOutstandingInstallmentChargeBalances(installment.getInstallmentNumber(), currentOutstandingLoanCharges);
                if (interestForCurrentPeriod.isGreaterThan(interestAccountedForCurrentPeriod)) {
                    interest = interest.plus(interestForCurrentPeriod).minus(interestAccountedForCurrentPeriod);
                } else {
                    paidFromFutureInstallments = paidFromFutureInstallments.plus(interestAccountedForCurrentPeriod)
                            .minus(interestAccountedForCurrentPeriod);
                }
                if (feeForCurrentPeriod.isGreaterThan(feeAccountedForCurrentPeriod)) {
                    fee = fee.plus(feeForCurrentPeriod.minus(feeAccountedForCurrentPeriod));
                } else {
                    paidFromFutureInstallments = paidFromFutureInstallments.plus(feeAccountedForCurrentPeriod.minus(feeForCurrentPeriod));
                }
            } else {
                paidFromFutureInstallments = paidFromFutureInstallments.plus(installment.getInterestPaid(currency))
                        .plus(installment.getPenaltyChargesPaid(currency)).plus(installment.getFeeChargesPaid(currency));
            }

        }
        Money totalPrincipal = Money.of(getCurrency(), this.getLoanSummary().getTotalPrincipalOutstanding());
        totalPrincipal = totalPrincipal.minus(paidFromFutureInstallments);
        final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = new LoanRepaymentScheduleInstallment(null, 0,
                writtenOffOnDate, writtenOffOnDate, totalPrincipal.getAmount(), interest.getAmount(), fee.getAmount(), penalty.getAmount(),
                false, null);
        currentOutstandingLoanCharges.removeIf(loanChargeData -> loanChargeData.getAmountOutstanding().compareTo(BigDecimal.ZERO) == 0);
        loanRepaymentScheduleInstallment.setCurrentOutstandingLoanCharges(currentOutstandingLoanCharges);
        return loanRepaymentScheduleInstallment;
    }

    @SuppressWarnings({ "squid:S3776" })
    private void incrementOutstandingInstallmentChargeBalances(final Integer installmentNumber,
            final List<LoanChargeData> currentLoanCharges) {
        if (!CollectionUtils.isEmpty(this.charges)) {
            for (final LoanCharge loanCharge : this.charges) {
                if (loanCharge != null && loanCharge.isActive() && !loanCharge.isDueAtDisbursement() && loanCharge.isNotFullyPaid()) {
                    final LoanInstallmentCharge loanInstallmentCharge = loanCharge.getInstallmentLoanCharge(installmentNumber);
                    if (loanInstallmentCharge != null) {
                        final Optional<LoanChargeData> currentLoanChargeoptional = currentLoanCharges.stream()
                                .filter(loanChargeData -> loanChargeData.getChargeId().equals(loanCharge.getCharge().getId())).findFirst();
                        if (currentLoanChargeoptional.isPresent()) {
                            final LoanChargeData currentLoanCharge = currentLoanChargeoptional.get();
                            BigDecimal amountOutstanding = loanInstallmentCharge.getAmountOutstanding();
                            if (amountOutstanding != null && currentLoanCharge.getAmountOutstanding() != null) {
                                amountOutstanding = currentLoanCharge.getAmountOutstanding().add(amountOutstanding);
                                currentLoanCharge.updateAmountOutstanding(amountOutstanding);
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings({ "squid:S3776" })
    private void incrementOutstandingPenaltyChargeBalances(final Integer installmentNumber, final List<LoanChargeData> currentLoanCharges) {
        if (!CollectionUtils.isEmpty(this.charges)) {
            for (final LoanCharge loanCharge : this.charges) {
                if (loanCharge != null && loanCharge.isActive() && !loanCharge.isDueAtDisbursement() && loanCharge.isNotFullyPaid()
                        && loanCharge.isOverdueInstallmentCharge()) {
                    final LoanOverdueInstallmentCharge overdueInstallmentCharge = loanCharge.getOverdueInstallmentCharge();
                    if (overdueInstallmentCharge != null) {
                        final LoanRepaymentScheduleInstallment overdueInstallment = overdueInstallmentCharge.getInstallment();
                        if (overdueInstallment != null && installmentNumber.equals(overdueInstallment.getInstallmentNumber())) {
                            final Optional<LoanChargeData> currentLoanChargeoptional = currentLoanCharges.stream()
                                    .filter(loanChargeData -> loanChargeData.getChargeId().equals(loanCharge.getCharge().getId()))
                                    .findFirst();
                            if (currentLoanChargeoptional.isPresent()) {
                                final LoanChargeData currentLoanCharge = currentLoanChargeoptional.get();
                                final Money amountOutstanding = loanCharge.getAmountOutstanding(loanCurrency());
                                if (amountOutstanding != null && currentLoanCharge.getAmountOutstanding() != null) {
                                    BigDecimal amountOutstandingBalance = currentLoanCharge.getAmountOutstanding()
                                            .add(amountOutstanding.getAmount());
                                    currentLoanCharge.updateAmountOutstanding(amountOutstandingBalance);
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    @SuppressWarnings({ "squid:S3776" })
    public Money[] retrieveIncomeOutstandingTillDate(final LocalDate paymentDate, final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        Money[] balances = new Money[4];
        final MonetaryCurrency currency = getCurrency();
        Money interest = Money.zero(currency);
        Money paidFromFutureInstallments = Money.zero(currency);
        Money fee = Money.zero(currency);
        Money penalty = Money.zero(currency);
        Money principalLoanBalanceOutstanding = this.getPrincipal();
        for (final LoanRepaymentScheduleInstallment installment : this.repaymentScheduleInstallments) {
            if (!DateUtils.isBefore(paymentDate, installment.getDueDate())) {
                interest = interest.plus(installment.getInterestOutstanding(currency));
                penalty = penalty.plus(installment.getPenaltyChargesOutstanding(currency));
                fee = fee.plus(installment.getFeeChargesOutstanding(currency));
            } else if (DateUtils.isAfter(paymentDate, installment.getFromDate())) {
                final Money[] balancesForCurrentPeriod = fetchInterestFeeAndPenaltyTillDate(paymentDate, installment,
                        principalLoanBalanceOutstanding, scheduleGeneratorDTO);
                if (balancesForCurrentPeriod[0].isGreaterThan(balancesForCurrentPeriod[5])) {
                    interest = interest.plus(balancesForCurrentPeriod[0]).minus(balancesForCurrentPeriod[5]);
                } else {
                    paidFromFutureInstallments = paidFromFutureInstallments.plus(balancesForCurrentPeriod[5])
                            .minus(balancesForCurrentPeriod[0]);
                }
                if (balancesForCurrentPeriod[1].isGreaterThan(balancesForCurrentPeriod[3])) {
                    fee = fee.plus(balancesForCurrentPeriod[1].minus(balancesForCurrentPeriod[3]));
                } else {
                    paidFromFutureInstallments = paidFromFutureInstallments
                            .plus(balancesForCurrentPeriod[3].minus(balancesForCurrentPeriod[1]));
                }
                if (balancesForCurrentPeriod[2].isGreaterThan(balancesForCurrentPeriod[4])) {
                    penalty = penalty.plus(balancesForCurrentPeriod[2].minus(balancesForCurrentPeriod[4]));
                } else {
                    paidFromFutureInstallments = paidFromFutureInstallments.plus(balancesForCurrentPeriod[4])
                            .minus(balancesForCurrentPeriod[2]);
                }
            } else {
                // When Foreclosure on disbursement date then pay fee charges
                // When loan is canceled on same date then only pay hono charges
                if (installment.getInstallmentNumber() == 1 && DateUtils.isEqual(paymentDate, installment.getFromDate())) {
                    fee = fee.plus(installment.getFeeChargesOutstanding(currency));
                    if (this.isAnulado) {
                        fee = getPendingHonoAmountOfAnuladoLoanForInstallment(this, installment.getInstallmentNumber());
                    }
                }
                paidFromFutureInstallments = paidFromFutureInstallments.plus(installment.getInterestPaid(currency))
                        .plus(installment.getPenaltyChargesPaid(currency)).plus(installment.getFeeChargesPaid(currency));
            }
            principalLoanBalanceOutstanding = principalLoanBalanceOutstanding.minus(installment.getPrincipal(currency));
        }
        balances[0] = interest;
        balances[1] = fee;
        balances[2] = penalty;
        balances[3] = paidFromFutureInstallments;
        return balances;
    }

    private Money[] fetchInterestFeeAndPenaltyTillDate(final LocalDate transactionDate,
            final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment, final Money principalLoanBalanceOutstanding,
            final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        final MonetaryCurrency currency = getCurrency();
        final Integer currencyInMultiplesOf = currency.getCurrencyInMultiplesOf();
        final String currencyCode = currency.getCode();
        final MonetaryCurrency interestCalculationCurrency = new MonetaryCurrency(currencyCode, 5, currencyInMultiplesOf);
        final Money penaltyForCurrentPeriod = Money.zero(currency);
        final Money penaltyAccountedForCurrentPeriod = Money.zero(currency);
        principalLoanBalanceOutstanding.setCurrency(interestCalculationCurrency);
        int totalPeriodDays = Math.toIntExact(
                ChronoUnit.DAYS.between(loanRepaymentScheduleInstallment.getFromDate(), loanRepaymentScheduleInstallment.getDueDate()));
        int tillDays = Math.toIntExact(ChronoUnit.DAYS.between(loanRepaymentScheduleInstallment.getFromDate(), transactionDate));
        BigDecimal interestCharged = loanRepaymentScheduleInstallment.getInterestCharged(getCurrency()).getAmount();
        if (loanRepaymentScheduleInstallment.originalInterestChargedAmount() != null
                && loanRepaymentScheduleInstallment.originalInterestChargedAmount().compareTo(BigDecimal.ZERO) > 0) {
            interestCharged = loanRepaymentScheduleInstallment.originalInterestChargedAmount();
        }
        Money interestForCurrentPeriod = Money.of(getCurrency(),
                BigDecimal.valueOf(calculateInterestForDays(totalPeriodDays, interestCharged, tillDays)));
        final Money interestAccountedForCurrentPeriod = loanRepaymentScheduleInstallment.getInterestWaived(currency)
                .plus(loanRepaymentScheduleInstallment.getInterestPaid(currency))
                .plus(loanRepaymentScheduleInstallment.getInterestWrittenOff(currency));
        final LoanApplicationTerms loanApplicationTerms = this.constructLoanApplicationTerms(scheduleGeneratorDTO);
        final LoanTermVariationsDataWrapper loanTermVariationsDataWrapper = loanApplicationTerms.getLoanTermVariations();
        if (loanTermVariationsDataWrapper != null) {
            List<LoanTermVariationsData> interestRatesFromInstallment = loanTermVariationsDataWrapper.getInterestRateFromInstallment();
            if (interestRatesFromInstallment != null && !interestRatesFromInstallment.isEmpty()) {
                final LocalDate periodStartDate = loanRepaymentScheduleInstallment.getFromDate();
                interestRatesFromInstallment = interestRatesFromInstallment.stream().filter(interestRateVariation -> !DateUtils
                        .isAfter(interestRateVariation.getTermVariationApplicableFrom(), transactionDate)).toList();
                final LoanScheduleGenerator loanScheduleGenerator = scheduleGeneratorDTO.getLoanScheduleFactory()
                        .create(loanApplicationTerms.getLoanScheduleType(), loanApplicationTerms.getInterestMethod());
                if (!CollectionUtils.isEmpty(interestRatesFromInstallment)) {
                    final BigDecimal annualNominalInterestRate = loanApplicationTerms.getAnnualNominalInterestRate();
                    Money totalForeclosureDailyInterestEarned = Money.zero(interestCalculationCurrency);
                    for (LocalDate localDate = periodStartDate; DateUtils.isBefore(localDate,
                            transactionDate); localDate = localDate.plusDays(1)) {
                        final Money foreclosureDailyInterestEarned = this.calculateForeclosureDailyInterestEarned(
                                interestRatesFromInstallment, localDate, principalLoanBalanceOutstanding,
                                loanRepaymentScheduleInstallment.getInstallmentNumber(), loanApplicationTerms, loanScheduleGenerator);
                        loanApplicationTerms.updateAnnualNominalInterestRate(annualNominalInterestRate);
                        totalForeclosureDailyInterestEarned = totalForeclosureDailyInterestEarned.plus(foreclosureDailyInterestEarned);
                    }
                    interestForCurrentPeriod = totalForeclosureDailyInterestEarned;
                }
            }
        }

        final Money feeForCurrentPeriod = loanRepaymentScheduleInstallment.getFeeChargesCharged(currency);
        final Money feeAccountedForCurrentPeriod = loanRepaymentScheduleInstallment.getFeeChargesWaived(currency)
                .plus(loanRepaymentScheduleInstallment.getFeeChargesPaid(currency))
                .plus(loanRepaymentScheduleInstallment.getFeeChargesWrittenOff(currency));
        final Money[] balances = new Money[6];
        balances[0] = interestForCurrentPeriod;
        balances[1] = feeForCurrentPeriod;
        balances[2] = penaltyForCurrentPeriod;
        balances[3] = feeAccountedForCurrentPeriod;
        balances[4] = penaltyAccountedForCurrentPeriod;
        balances[5] = interestAccountedForCurrentPeriod;
        return balances;
    }

    public Money calculateForeclosureDailyInterestEarned(final List<LoanTermVariationsData> interestRatesFromInstallment,
            final LocalDate currentDate, final Money principalLoanBalanceOutstanding, final int periodNumber,
            final LoanApplicationTerms loanApplicationTerms, final LoanScheduleGenerator loanScheduleGenerator) {
        final List<LoanTermVariationsData> interestRatesFromInstallmentV1 = interestRatesFromInstallment.stream()
                .filter(interestRateVariation -> !DateUtils.isAfter(interestRateVariation.getTermVariationApplicableFrom(), currentDate))
                .toList();
        if (!CollectionUtils.isEmpty(interestRatesFromInstallmentV1)) {
            final List<LoanTermVariationsData> interestRatesFromInstallmentV2 = interestRatesFromInstallment.stream().sorted(
                    Comparator.comparing(LoanTermVariationsData::getLastModifiedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            final LoanTermVariationsData interestRateFromInstallment = interestRatesFromInstallmentV2.get(0);
            final BigDecimal interestRate = interestRateFromInstallment.getDecimalValue();
            loanApplicationTerms.updateAnnualNominalInterestRate(interestRate);
        }
        final LocalDate currentDatePlusOne = currentDate.plusDays(1);
        final boolean ignoreCurrencyDigitsAfterDecimal = true;
        final PrincipalInterest principalInterest = loanScheduleGenerator.calculatePrincipalInterestComponents(
                principalLoanBalanceOutstanding, loanApplicationTerms, periodNumber, currentDate, currentDatePlusOne,
                ignoreCurrencyDigitsAfterDecimal);
        return principalInterest.interest();
    }

    public Money[] retriveIncomeForOverlappingPeriod(final LocalDate paymentDate, final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        Money[] balances = new Money[3];
        final MonetaryCurrency currency = getCurrency();
        balances[0] = balances[1] = balances[2] = Money.zero(currency);
        Money principalLoanBalanceOutstanding = this.getPrincipal();
        for (final LoanRepaymentScheduleInstallment installment : this.repaymentScheduleInstallments) {
            if (DateUtils.isEqual(paymentDate, installment.getDueDate())) {
                Money interest = installment.getInterestCharged(currency);
                Money fee = installment.getFeeChargesCharged(currency);
                Money penalty = installment.getPenaltyChargesCharged(currency);
                balances[0] = interest;
                balances[1] = fee;
                balances[2] = penalty;
                break;
            } else if (DateUtils.isAfter(paymentDate, installment.getFromDate())
                    && DateUtils.isBefore(paymentDate, installment.getDueDate())) {
                balances = fetchInterestFeeAndPenaltyTillDate(paymentDate, installment, principalLoanBalanceOutstanding,
                        scheduleGeneratorDTO);
                break;
            } else if (installment.getInstallmentNumber() == 1 && DateUtils.isEqual(paymentDate, installment.getFromDate())) {
                // Foreclosure being done on same date as disbursement date. Fee charge must be paid
                // If loan is canceled on same date then only pay hono charge
                Money fee = installment.getFeeChargesOutstanding(currency);
                if (this.isAnulado) {
                    fee = getPendingHonoAmountOfAnuladoLoanForInstallment(this, installment.getInstallmentNumber());
                }
                balances[1] = fee;
            }
            principalLoanBalanceOutstanding = principalLoanBalanceOutstanding.minus(installment.getPrincipal(currency));
        }

        return balances;
    }

    public double calculateInterestForDays(int daysInPeriod, BigDecimal interest, int days) {
        if (interest.doubleValue() == 0) {
            return 0;
        }
        return interest.doubleValue() / daysInPeriod * days;
    }

    public Money[] getReceivableIncome(final LocalDate tillDate) {
        MonetaryCurrency currency = getCurrency();
        Money receivableInterest = Money.zero(currency);
        Money receivableFee = Money.zero(currency);
        Money receivablePenalty = Money.zero(currency);
        Money[] receivables = new Money[3];
        for (final LoanTransaction transaction : this.loanTransactions) {
            if (transaction.isNotReversed() && !transaction.isRepaymentAtDisbursement() && !transaction.isDisbursement()
                    && !DateUtils.isAfter(transaction.getTransactionDate(), tillDate)) {
                if (transaction.isAccrual()) {
                    receivableInterest = receivableInterest.plus(transaction.getInterestPortion(currency));
                    receivableFee = receivableFee.plus(transaction.getFeeChargesPortion(currency));
                    receivablePenalty = receivablePenalty.plus(transaction.getPenaltyChargesPortion(currency));
                } else if (transaction.isRepaymentLikeType() || transaction.isChargePayment()) {
                    receivableInterest = receivableInterest.minus(transaction.getInterestPortion(currency));
                    receivableFee = receivableFee.minus(transaction.getFeeChargesPortion(currency));
                    receivablePenalty = receivablePenalty.minus(transaction.getPenaltyChargesPortion(currency));
                }
            }
            if (receivableInterest.isLessThanZero()) {
                receivableInterest = receivableInterest.zero();
            }
            if (receivableFee.isLessThanZero()) {
                receivableFee = receivableFee.zero();
            }
            if (receivablePenalty.isLessThanZero()) {
                receivablePenalty = receivablePenalty.zero();
            }
        }
        receivables[0] = receivableInterest;
        receivables[1] = receivableFee;
        receivables[2] = receivablePenalty;
        return receivables;
    }

    public void reverseAccrualsAfter(final LocalDate tillDate) {
        for (final LoanTransaction transaction : this.loanTransactions) {
            if (transaction.isAccrual() && DateUtils.isAfter(transaction.getTransactionDate(), tillDate)) {
                transaction.reverse();
            }
        }
    }

    public ChangedTransactionDetail handleForeClosureTransactions(final LoanTransaction repaymentTransaction,
            final LoanLifecycleStateMachine loanLifecycleStateMachine, final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        LoanEvent event = LoanEvent.LOAN_FORECLOSURE;
        validateAccountStatus(event);
        validateForForeclosure(repaymentTransaction.getTransactionDate());
        this.loanSubStatus = LoanSubStatus.FORECLOSED.getValue();
        applyAccruals();
        return handleRepaymentOrRecoveryOrWaiverTransaction(repaymentTransaction, loanLifecycleStateMachine, null, scheduleGeneratorDTO);
    }

    public ChangedTransactionDetail handleClaimTransactions(final LoanTransaction repaymentTransaction,
            final LoanLifecycleStateMachine loanLifecycleStateMachine, final ScheduleGeneratorDTO scheduleGeneratorDTO) {

        validateForForeclosure(repaymentTransaction.getTransactionDate());
        applyAccruals();
        return handleRepaymentOrRecoveryOrWaiverTransaction(repaymentTransaction, loanLifecycleStateMachine, null, scheduleGeneratorDTO);
    }

    public void validateForForeclosure(final LocalDate transactionDate) {
        if (isInterestRecalculationEnabledForProduct()) {
            final String defaultUserMessage = "The loan with interest recalculation enabled cannot be foreclosed.";
            throw new LoanForeclosureException("loan.with.interest.recalculation.enabled.cannot.be.foreclosured", defaultUserMessage,
                    getId());
        }

        LocalDate lastUserTransactionDate = getLastUserTransactionDate();

        if (DateUtils.isDateInTheFuture(transactionDate)) {
            final String defaultUserMessage = "The transactionDate cannot be in the future.";
            throw new LoanForeclosureException("loan.foreclosure.transaction.date.is.in.future", defaultUserMessage, transactionDate);
        }

        if (DateUtils.isBefore(transactionDate, lastUserTransactionDate)) {
            final String defaultUserMessage = "The transactionDate cannot be earlier than the last transaction date.";
            throw new LoanForeclosureException("loan.foreclosure.transaction.date.cannot.before.the.last.transaction.date",
                    defaultUserMessage, transactionDate);
        }
    }

    @SuppressWarnings({ "squid:S3776" })
    public void updateInstallmentsPostDate(final LocalDate transactionDate, final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        List<LoanRepaymentScheduleInstallment> newInstallments = new ArrayList<>(this.repaymentScheduleInstallments);
        final MonetaryCurrency currency = getCurrency();
        Money totalPrincipal = Money.zero(currency);
        Money[] balances = retriveIncomeForOverlappingPeriod(transactionDate, scheduleGeneratorDTO);
        boolean isInterestComponent = false;
        for (final LoanRepaymentScheduleInstallment installment : this.repaymentScheduleInstallments) {
            if (!DateUtils.isAfter(transactionDate, installment.getDueDate())) {
                totalPrincipal = totalPrincipal.plus(installment.getPrincipal(currency));
                if (installment.getAdvancePrincipalAmount() != null
                        && installment.getAdvancePrincipalAmount().compareTo(BigDecimal.ZERO) > 0) {
                    totalPrincipal = totalPrincipal.add(installment.getAdvancePrincipalAmount());
                }
                newInstallments.remove(installment);
            }
        }
        for (LoanDisbursementDetails loanDisbursementDetails : getDisbursementDetails()) {
            if (loanDisbursementDetails.actualDisbursementDate() == null) {
                totalPrincipal = Money.of(currency, totalPrincipal.getAmount().subtract(loanDisbursementDetails.principal()));
            }
        }

        LocalDate installmentStartDate = getDisbursementDate();

        if (!newInstallments.isEmpty()) {
            installmentStartDate = newInstallments.get(newInstallments.size() - 1).getDueDate();
        }

        int installmentNumber = newInstallments.size();

        if (!isInterestComponent) {
            installmentNumber++;
        }

        LoanRepaymentScheduleInstallment newInstallment = new LoanRepaymentScheduleInstallment(null, newInstallments.size() + 1,
                installmentStartDate, transactionDate, totalPrincipal.getAmount(), balances[0].getAmount(), balances[1].getAmount(),
                balances[2].getAmount(), isInterestComponent, null);
        newInstallment.updateInstallmentNumber(newInstallments.size() + 1);
        newInstallments.add(newInstallment);
        updateLoanScheduleOnForeclosure(newInstallments);

        final Set<LoanCharge> activeCharges = this.getActiveCharges();
        for (LoanCharge loanCharge : activeCharges) {
            if (DateUtils.isAfter(loanCharge.getDueLocalDate(), transactionDate)) {
                loanCharge.setActive(false);
            } else if (loanCharge.getDueLocalDate() == null) {
                boolean ivaHono = false;
                if (this.isAnulado && this.isAnuladoOnDisbursementDate && loanCharge.isCustomPercentageBasedOfAnotherCharge()) {
                    for (LoanCharge charge : activeCharges) {
                        if (charge.isFlatHono() && charge.getCharge().getId() != null
                                && charge.getCharge().getId().equals(loanCharge.getCharge().getParentChargeId())) {
                            ivaHono = true;
                        }
                    }
                }

                if (this.isAnulado && this.isAnuladoOnDisbursementDate && (!loanCharge.isFlatHono() && !ivaHono)) {
                    // If loan is canceled on same day as disbursement then only charge hono charges
                    this.clearLoanInstallmentChargesBeforeRegeneration(loanCharge);
                    loanCharge.setAmount(loanCharge.getAmountPaid(currency).getAmount());
                    loanCharge.setOutstandingAmount(BigDecimal.ZERO);

                } else {
                    recalculateLoanCharge(loanCharge);
                    loanCharge.updateWaivedAmount(currency);
                }
            }
        }

        for (LoanTransaction loanTransaction : getLoanTransactions()) {
            if (loanTransaction.isChargesWaiver()) {
                for (LoanChargePaidBy chargePaidBy : loanTransaction.getLoanChargesPaid()) {
                    if ((chargePaidBy.getLoanCharge().isDueDateCharge()
                            && DateUtils.isBefore(transactionDate, chargePaidBy.getLoanCharge().getDueLocalDate()))
                            || (chargePaidBy.getLoanCharge().isInstalmentFee() && chargePaidBy.getInstallmentNumber() != null
                                    && chargePaidBy.getInstallmentNumber() > installmentNumber)) {
                        loanTransaction.reverse();
                    }
                }

            }
        }
    }

    public void updateLoanScheduleOnForeclosure(final Collection<LoanRepaymentScheduleInstallment> installments) {
        this.repaymentScheduleInstallments.clear();
        for (final LoanRepaymentScheduleInstallment installment : installments) {
            addLoanRepaymentScheduleInstallment(installment);
        }
    }

    public Money getPendingHonoAmountOfAnuladoLoanForInstallment(Loan loan, Integer installmentNumber) {
        BigDecimal honorariosAmount = BigDecimal.ZERO;
        Collection<LoanCharge> honorariosCharges = loan.getLoanCharges().stream().filter(LoanCharge::isFlatHono).toList();
        Collection<LoanCharge> ivaCharges = loan.getLoanCharges().stream().filter(LoanCharge::isCustomPercentageBasedOfAnotherCharge)
                .toList();

        BigDecimal chargeAmount = honorariosCharges.stream().flatMap(lic -> lic.installmentCharges().stream())
                .filter(lc -> Objects.equals(installmentNumber, lc.getInstallment().getInstallmentNumber()))
                .map(LoanInstallmentCharge::getAmountOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal honorariosTermChargeAmount = ivaCharges.stream()
                .filter(lc -> honorariosCharges.stream()
                        .anyMatch(mic -> mic.getCharge().getId().equals(lc.getCharge().getParentChargeId())))
                .flatMap(lic -> lic.installmentCharges().stream())
                .filter(lc -> Objects.equals(installmentNumber, lc.getInstallment().getInstallmentNumber()))
                .map(LoanInstallmentCharge::getAmountOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        honorariosAmount = honorariosAmount.add(honorariosTermChargeAmount).add(chargeAmount);

        return Money.of(loan.getCurrency(), honorariosAmount);
    }

    public BigDecimal getAccruedInterestForInstallment(final Integer installmentNumber) {
        return this.loanTransactions.stream()
                .filter(loanTransaction -> loanTransaction.isAccrual() && loanTransaction.isDailyAccrual()
                        && installmentNumber.equals(loanTransaction.getInstallmentNumber()))
                .map(loanTransaction -> loanTransaction.getInterestPortion(getCurrency()).getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Integer getLoanSubStatus() {
        return this.loanSubStatus;
    }

    private boolean isForeclosure() {
        boolean isForeClosure = false;
        if (this.loanSubStatus != null) {
            isForeClosure = LoanSubStatus.fromInt(loanSubStatus).isForeclosed();
        }

        return isForeClosure;
    }

    private boolean isClaim() {
        return this.claimType != null;
    }

    public Set<LoanTermVariations> getActiveLoanTermVariations() {
        Set<LoanTermVariations> retData = new HashSet<>();
        if (this.loanTermVariations != null && !this.loanTermVariations.isEmpty()) {
            for (LoanTermVariations loanTermVariationsEach : this.loanTermVariations) {
                if (Boolean.TRUE.equals(loanTermVariationsEach.isActive())) {
                    retData.add(loanTermVariationsEach);
                }
            }
        }
        return !retData.isEmpty() ? retData : null;
    }

    public void setIsTopup(final boolean isTopup) {
        this.isTopup = isTopup;
    }

    public boolean isTopup() {
        return this.isTopup;
    }

    public void markAsFraud(final boolean value) {
        this.fraud = value;
    }

    public boolean isFraud() {
        return this.fraud;
    }

    public BigDecimal getFirstDisbursalAmount() {
        BigDecimal firstDisbursalAmount;

        if (this.isMultiDisburmentLoan()) {
            List<DisbursementData> disbursementData = getDisbursmentData();
            Collections.sort(disbursementData);
            firstDisbursalAmount = disbursementData.get(disbursementData.size() - 1).getPrincipal();
        } else {
            firstDisbursalAmount = this.getLoanRepaymentScheduleDetail().getPrincipal().getAmount();
        }
        return firstDisbursalAmount;
    }

    public void setTopupLoanDetails(LoanTopupDetails topupLoanDetails) {
        this.loanTopupDetails = topupLoanDetails;
    }

    public LoanTopupDetails getTopupLoanDetails() {
        return this.loanTopupDetails;
    }

    public Collection<LoanCharge> getLoanCharges() {
        return this.charges;
    }

    public void initializeLazyCollections() {
        checkAndFetchLazyCollection(this.charges);
        checkAndFetchLazyCollection(this.trancheCharges);
        checkAndFetchLazyCollection(this.repaymentScheduleInstallments);
        checkAndFetchLazyCollection(this.loanTransactions);
        checkAndFetchLazyCollection(this.disbursementDetails);
        checkAndFetchLazyCollection(this.loanTermVariations);
        checkAndFetchLazyCollection(this.collateral);
        checkAndFetchLazyCollection(this.loanOfficerHistory);
        checkAndFetchLazyCollection(this.loanCollateralManagements);
    }

    @SuppressWarnings({ "squid:S3740", "squid:S2201" })
    private void checkAndFetchLazyCollection(Collection lazyCollection) {
        if (lazyCollection != null) {
            lazyCollection.size();
        }
    }

    public void initializeLoanOfficerHistory() {
        this.loanOfficerHistory.size(); // NOSONAR
    }

    public void initializeTransactions() {
        this.loanTransactions.size(); // NOSONAR
    }

    public void initializeRepaymentSchedule() {
        this.repaymentScheduleInstallments.size(); // NOSONAR
    }

    public boolean hasInvalidLoanType() {
        return AccountType.fromInt(this.loanType).isInvalid();
    }

    public boolean isIndividualLoan() {
        return AccountType.fromInt(this.loanType).isIndividualAccount();
    }

    public void setRates(List<Rate> rates) {
        this.rates = rates;
    }

    public List<Rate> getRates() {
        return rates;
    }

    public Integer getLoanType() {
        return loanType;
    }

    public void setLoanType(Integer loanType) {
        this.loanType = loanType;
    }

    public Set<LoanCollateralManagement> getLoanCollateralManagements() {
        return this.loanCollateralManagements;
    }

    public void adjustNetDisbursalAmount(BigDecimal adjustedAmount) {
        this.netDisbursalAmount = adjustedAmount.subtract(this.deriveSumTotalOfChargesDueAtDisbursement());
    }

    /**
     * Get the charges.
     *
     * @return the charges
     */
    public Collection<LoanCharge> getCharges() {
        // At the time of loan creation, "this.charges" will be null if no charges found in the request.
        // In that case, fetch loan (before commit) will return null for the charges.
        // Return empty set instead of null to avoid NPE
        return Optional.ofNullable(this.charges).orElse(new LinkedHashSet<>());
    }

    public boolean hasDelinquencyBucket() {
        return (getLoanProduct().getDelinquencyBucket() != null);
    }

    public Long getAgeOfOverdueDays(LocalDate baseDate) {
        Long ageOfOverdueDays = 0L;

        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        for (final LoanRepaymentScheduleInstallment installment : installments) {
            if (!installment.isObligationsMet()) {
                ageOfOverdueDays = DateUtils.getDifferenceInDays(installment.getDueDate(), baseDate);
                break;
            }
        }
        return ageOfOverdueDays;
    }

    public LocalDate getLastClosedBusinessDate() {
        return this.lastClosedBusinessDate;
    }

    public void setLastClosedBusinessDate(LocalDate lastClosedBusinessDate) {
        this.lastClosedBusinessDate = lastClosedBusinessDate;
    }

    public void markAsChargedOff(final LocalDate chargedOffOn, final AppUser chargedOffBy, final CodeValue chargeOffReason) {
        this.chargedOff = true;
        this.chargedOffBy = chargedOffBy;
        this.chargedOffOnDate = chargedOffOn;
        this.chargeOffReason = chargeOffReason;
    }

    public void liftChargeOff() {
        this.chargedOff = false;
        this.chargedOffBy = null;
        this.chargedOffOnDate = null;
        this.chargeOffReason = null;
    }

    public boolean isChargedOff() {
        return this.chargedOff;
    }

    public LoanRepaymentScheduleInstallment getLastLoanRepaymentScheduleInstallment() {
        return getRepaymentScheduleInstallments().get(getRepaymentScheduleInstallments().size() - 1);
    }

    public List<LoanTransaction> getLoanTransactions(Predicate<LoanTransaction> predicate) {
        return getLoanTransactions().stream().filter(predicate).toList();
    }

    public LoanTransaction findChargedOffTransaction() {
        return getLoanTransactions().stream() //
                .filter(LoanTransaction::isNotReversed) //
                .filter(LoanTransaction::isChargeOff) //
                .findFirst() //
                .orElse(null);
    }

    public void handleMaturityDateActivate() {
        if (this.expectedMaturityDate != null && !this.expectedMaturityDate.equals(this.actualMaturityDate)) {
            this.actualMaturityDate = this.expectedMaturityDate;
        }
    }

    public LoanTransaction getLastUserTransaction() {
        return getLoanTransactions().stream() //
                .filter(LoanTransaction::isNotReversed) //
                .filter(t -> !(t.isAccrualTransaction() || t.isIncomePosting())) //
                .reduce((first, second) -> second) //
                .orElse(null);
    }

    public LocalDate getChargedOffOnDate() {
        return chargedOffOnDate;
    }

    public LoanInterestRecalculationDetails getLoanInterestRecalculationDetails() {
        return loanInterestRecalculationDetails;
    }

    public List<LoanPaymentAllocationRule> getPaymentAllocationRules() {
        return paymentAllocationRules;
    }

    public void setPaymentAllocationRules(List<LoanPaymentAllocationRule> loanPaymentAllocationRules) {
        this.paymentAllocationRules = loanPaymentAllocationRules;
    }

    public List<LoanCreditAllocationRule> getCreditAllocationRules() {
        return creditAllocationRules;
    }

    public void setCreditAllocationRules(List<LoanCreditAllocationRule> loanCreditAllocationRules) {
        this.creditAllocationRules = loanCreditAllocationRules;
    }

    public String getTransactionProcessingStrategyCode() {
        return transactionProcessingStrategyCode;
    }

    public String getTransactionProcessingStrategyName() {
        return transactionProcessingStrategyName;
    }

    public boolean isEnableInstallmentLevelDelinquency() {
        return this.enableInstallmentLevelDelinquency;
    }

    public void updateEnableInstallmentLevelDelinquency(boolean enableInstallmentLevelDelinquency) {
        this.enableInstallmentLevelDelinquency = enableInstallmentLevelDelinquency;
    }

    public Client getLoanAssignor() {
        return loanAssignor;
    }

    public void setLoanAssignor(Client loanAssignor) {
        this.loanAssignor = loanAssignor;
    }

    public AppUser getDisbursedBy() {
        return disbursedBy;
    }

    public LoanCustomizationDetail getLoanCustomizationDetail() {
        if (this.loanCustomizationDetail == null) {
            this.loanCustomizationDetail = new LoanCustomizationDetail();
        }
        return this.loanCustomizationDetail;
    }

    public void updateLoanScheduleAfterCustomChargeApplied() {
        final LoanRepaymentScheduleProcessingWrapper wrapper = new LoanRepaymentScheduleProcessingWrapper();
        wrapper.reprocess(getCurrency(), getDisbursementDate(), getRepaymentScheduleInstallments(), getActiveCharges());

        updateLoanSummaryDerivedFields();
    }

    public LoanScheduleProcessingType getRepaymentTransactionProcessingType() {
        return repaymentTransactionProcessingType;
    }

    public void setRepaymentTransactionProcessingType(LoanScheduleProcessingType repaymentTransactionProcessingType) {
        this.repaymentTransactionProcessingType = repaymentTransactionProcessingType;
    }

    public BigDecimal getValorGiro() {
        return valorGiro;
    }

    public BigDecimal getValorDescuento() {
        return valorDescuento;
    }

    public void updateValorDescuento(BigDecimal valorDescuento) {
        if (valorDescuento == null && this.approvedPrincipal != null) {
            this.valorDescuento = BigDecimal.ZERO;
            this.valorGiro = this.getApprovedPrincipal();
            return;
        }
        this.valorDescuento = valorDescuento;
        this.valorGiro = this.getApprovedPrincipal().subtract(valorDescuento);
    }

    public void updateLoanStatus(LoanStatus loanStatus) {
        this.loanStatus = loanStatus.getValue();
    }

    public boolean recalculateEMI() {
        return recalculateEMI;
    }

    public void setRecalculateEMI(boolean recalculateEMI) {
        this.recalculateEMI = recalculateEMI;
    }

    public void setReduceTerm(boolean reduceTerm) {
        this.reduceTerm = reduceTerm;
    }

    public boolean shouldReduceTerm() {
        return reduceTerm;
    }

    public String excludedForInsuranceClaim() {
        return excludedForInsuranceClaim;
    }

    public void setExcludedForInsuranceClaim(String excludedForInsuranceClaim) {
        this.excludedForInsuranceClaim = excludedForInsuranceClaim;
    }

    public String excludedForAvalClaim() {
        return excludedForAvalClaim;
    }

    public void setExcludedForAvalClaim(String excludedForAvalClaim) {
        this.excludedForAvalClaim = excludedForAvalClaim;
    }

    public String excludedForCastigadoClaim() {
        return excludedForCastigadoClaim;
    }

    public void setExcludedForCastigadoClaim(String excludedForCastigadoClaim) {
        this.excludedForCastigadoClaim = excludedForCastigadoClaim;
    }

    public String claimType() {
        return claimType;
    }

    public void setClaimType(String claimType) {
        this.claimType = claimType;
    }

    public LocalDate claimDate() {
        return claimDate;
    }

    public void setClaimDate(LocalDate claimDate) {
        this.claimDate = claimDate;
    }

    public Integer getLastInstallmentChargeCalculatedOn() {
        return lastInstallmentChargeCalculatedOn;
    }

    public void setLastInstallmentChargeCalculatedOn(Integer lastInstallmentChargeCalculatedOn) {
        this.lastInstallmentChargeCalculatedOn = lastInstallmentChargeCalculatedOn;
    }

    public LocalDate getInterestAccruedTill() {
        return interestAccruedTill;
    }

    public void setInterestAccruedTill(LocalDate interestAccruedTill) {
        this.interestAccruedTill = interestAccruedTill;
    }

    public boolean isProgressiveLoan() {
        return this.getLoanProductRelatedDetail().getLoanScheduleType().equals(LoanScheduleType.PROGRESSIVE);
    }

    public Collection<LoanCharge> getInsuranceChargesForNoveltyIncidentReporting(boolean mandatoryCharges, boolean voluntaryCharges) {
        if (mandatoryCharges && voluntaryCharges) {
            return this.getCharges().stream().filter(charge -> charge.isMandatoryInsurance() || charge.isVoluntaryInsurance()).toList();
        } else if (mandatoryCharges) {
            return this.getCharges().stream().filter(LoanCharge::isMandatoryInsurance).toList();
        } else {
            return this.getCharges().stream().filter(LoanCharge::isVoluntaryInsurance).toList();
        }

    }

    public boolean isAnulado() {
        return isAnulado;
    }

    public void setAnulado(boolean anulado) {
        this.isAnulado = anulado;
    }

    public boolean isAnuladoOnDisbursementDate() {
        return isAnuladoOnDisbursementDate;
    }

    public void setAnuladoOnDisbursementDate(boolean anuladoOnDisbursementDate) {
        isAnuladoOnDisbursementDate = anuladoOnDisbursementDate;
    }

    public String cedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String numeroCredito() {
        return numeroCredito;
    }

    public void setNumeroCredito(String numeroCredito) {
        this.numeroCredito = numeroCredito;
    }

    public String code() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String nit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public boolean isMigratedLoan() {
        return isMigratedLoan;
    }

    public void setMigratedLoan(boolean migratedLoan) {
        isMigratedLoan = migratedLoan;
    }

    public boolean hasPenaltiesInRepaymentSchedules() {
        return this.getRepaymentScheduleInstallments().stream().anyMatch(LoanRepaymentScheduleInstallment::hasPenalties);
    }

    public void setOriginalNrOfRepayments() {
        this.originalNumberOfRepayments = this.getNumberOfRepayments();
    }

    private String normalizeString(String input) {
        if (input == null) return "";
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    public boolean isRevolvingLoan() {
        String loanProductName = normalizeString(getLoanProduct().getName());
        String loanProductType = normalizeString(getLoanProduct().getProductType().getLabel());
        String creditProductType = normalizeString(LoanProductType.CREDITO_ROTATIVO.getCode());
        return creditProductType.equalsIgnoreCase(loanProductName) || creditProductType.equalsIgnoreCase(loanProductType);
    }

    public boolean containsRevolvingLoan() {
        return this.getLoanProduct().getName().contains(LoanProductType.CREDITO_ROTATIVO.getCode())
                || this.getLoanProduct().getName().contains(LoanProductType.NANO_CREDITO.getCode());
    }

    public boolean hasAllInstallmentsPaid() {
        List<LoanRepaymentScheduleInstallment> installments = getRepaymentScheduleInstallments();
        if (installments.isEmpty()) {
            return false;
        }

        return installments.stream().filter(installment -> !installment.isFullyGraced()) // Exclude grace period
                                                                                         // installments
                .allMatch(LoanRepaymentScheduleInstallment::isObligationsMet);
    }

    public boolean isAdvancedPaymentsAllowed() {
        return this.loanProduct.isAdvancedPaymentsEnabled();
    }

    public BigDecimal maxPercentageOfPrincipalAllowedForAdvancedPayments() {
        if (isAdvancedPaymentsAllowed()) {
            return this.loanProduct.getMaxPercentagePrincipalPaymentAllowed();
        }
        return null;
    }

    /**
     * Debug method to check foreclosure overpayment calculation details. This method helps diagnose foreclosure
     * overpayment issues.
     *
     * @return String containing foreclosure calculation details
     */
    public String getForeclosureOverpaymentDebugInfo() {
        if (!isForeclosure()) {
            return "Loan is not in foreclosure status";
        }

        Money totalPaidInRepayments = getTotalPaidInRepayments();
        Money totalOutstandingBalance = getLoanSummary().getTotalOutstanding(loanCurrency());
        Money calculatedOverpayment = calculateTotalOverpayment();

        return String.format("Foreclosure Debug Info - Loan ID: %d, Total Paid: %s, Outstanding Balance: %s, Calculated Overpayment: %s",
                getId(), totalPaidInRepayments, totalOutstandingBalance, calculatedOverpayment);
    }

    public Long getId() {
        return super.getId();
    }

    public Optional<LocalDate> getActiveBlockAccountDate() {
        Optional<LocalDate> ret = null;

        if (Objects.nonNull(this.loanAccountBlocks)) {
            Optional<LoanAccountBlock> loanAccountBlockOpt = this.loanAccountBlocks.stream().filter(LoanAccountBlock::getActive)
                    .findFirst();

            if (loanAccountBlockOpt.isPresent()) {
                ret = Optional.ofNullable(loanAccountBlockOpt.get().getApplicationDate());
            }
        }
        return ret;
    }

    public LoanAccountBlockData checkBlockAccountComponents(LocalDate givenDate) {
        List<LoanAccountBlockComponentEnum> blockedComponents = new ArrayList<>();

        if (Objects.nonNull(this.loanAccountBlocks)) {
            Optional<LoanAccountBlock> loanAccountBlockOpt = this.loanAccountBlocks.stream().filter(LoanAccountBlock::getActive)
                    .filter(dt -> !givenDate.isBefore(dt.getApplicationDate())).findFirst();

            if (loanAccountBlockOpt.isPresent()) {
                LoanAccountBlock loanAccountBlock = loanAccountBlockOpt.get();

                if (Objects.nonNull(loanAccountBlock.getAccelerate()) && loanAccountBlock.getAccelerate()) {
                    blockedComponents.add(LoanAccountBlockComponentEnum.ACCELERATE);
                }
                if (Objects.nonNull(loanAccountBlock.getFreezeCurrentInterest()) && loanAccountBlock.getFreezeCurrentInterest()) {
                    blockedComponents.add(LoanAccountBlockComponentEnum.FREEZE_INTEREST);
                }
                if (Objects.nonNull(loanAccountBlock.getFreezeInterestArrears()) && loanAccountBlock.getFreezeInterestArrears()) {
                    blockedComponents.add(LoanAccountBlockComponentEnum.FREEZE_MORA);
                }
                if (Objects.nonNull(loanAccountBlock.getFreezeLifeInsurance()) && loanAccountBlock.getFreezeLifeInsurance()) {
                    blockedComponents.add(LoanAccountBlockComponentEnum.FREEZE_LIFE_INSURANCE);
                }
                if (Objects.nonNull(loanAccountBlock.getFreezeMypime()) && loanAccountBlock.getFreezeMypime()) {
                    blockedComponents.add(LoanAccountBlockComponentEnum.FREEZE_MIPYME);
                }

                if (blockedComponents.isEmpty()) {
                    blockedComponents.add(LoanAccountBlockComponentEnum.BLOCK_DISBURSAL);
                }
            }
        }
        return LoanAccountBlockData.builder().loanId(this.getId()).providedDate(givenDate)
                .loanAccountBlockComponentEnumList(blockedComponents).build();
    }

    public boolean containsBlockAccountDisbursal(LocalDate givenDate) {
        return containsBlockAccount(givenDate, LoanAccountBlockComponentEnum.BLOCK_DISBURSAL);
    }

    public boolean containsBlockAccountAccelerate(LocalDate givenDate) {
        return containsBlockAccount(givenDate, LoanAccountBlockComponentEnum.ACCELERATE);
    }

    public boolean containsBlockAccountFreezeInterest(LocalDate givenDate) {
        return containsBlockAccount(givenDate, LoanAccountBlockComponentEnum.FREEZE_INTEREST);
    }

    public boolean containsBlockAccountFreezeMora(LocalDate givenDate) {
        return containsBlockAccount(givenDate, LoanAccountBlockComponentEnum.FREEZE_MORA);
    }

    public boolean containsBlockAccountFreezeLifeInsurance(LocalDate givenDate) {
        return containsBlockAccount(givenDate, LoanAccountBlockComponentEnum.FREEZE_LIFE_INSURANCE);
    }

    public boolean containsBlockAccountFreezeMipyme(LocalDate givenDate) {
        return containsBlockAccount(givenDate, LoanAccountBlockComponentEnum.FREEZE_MIPYME);
    }

    public boolean containsBlockAccountFreezeGAC(LocalDate givenDate) {
        return containsBlockAccount(givenDate, LoanAccountBlockComponentEnum.FREEZE_GAC);
    }

    private boolean containsBlockAccount(LocalDate givenDate, LoanAccountBlockComponentEnum blockComponentEnum) {
        return checkBlockAccountComponents(givenDate).getLoanAccountBlockComponentEnumList().stream()
                .anyMatch(disb -> disb.equals(blockComponentEnum));
    }
}
