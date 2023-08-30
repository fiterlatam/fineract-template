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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.exception.PlatformServiceUnavailableException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.holiday.domain.Holiday;
import org.apache.fineract.organisation.holiday.domain.HolidayRepository;
import org.apache.fineract.organisation.holiday.domain.HolidayStatusType;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.workingdays.domain.WorkingDays;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.apache.fineract.portfolio.calendar.data.CalendarHistoryDataWrapper;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.domain.CalendarHistory;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstance;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstanceRepository;
import org.apache.fineract.portfolio.calendar.service.CalendarReadPlatformService;
import org.apache.fineract.portfolio.calendar.service.CalendarUtils;
import org.apache.fineract.portfolio.common.domain.DaysInYearType;
import org.apache.fineract.portfolio.floatingrates.data.FloatingRateDTO;
import org.apache.fineract.portfolio.floatingrates.data.FloatingRatePeriodData;
import org.apache.fineract.portfolio.floatingrates.exception.FloatingRateNotFoundException;
import org.apache.fineract.portfolio.floatingrates.service.FloatingRatesReadPlatformService;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.data.HolidayDetailDTO;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDisbursementDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGeneratorFactory;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRelatedDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoanUtilService {

    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final HolidayRepository holidayRepository;
    private final WorkingDaysRepositoryWrapper workingDaysRepository;
    private final LoanScheduleGeneratorFactory loanScheduleFactory;
    private final FloatingRatesReadPlatformService floatingRatesReadPlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final CalendarReadPlatformService calendarReadPlatformService;

    private final BigDecimal divisor = BigDecimal.valueOf(Double.parseDouble("100.0"));
    private final long daysInMonth = 30;

    @Autowired
    public LoanUtilService(final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository,
            final CalendarInstanceRepository calendarInstanceRepository, final ConfigurationDomainService configurationDomainService,
            final HolidayRepository holidayRepository, final WorkingDaysRepositoryWrapper workingDaysRepository,
            final LoanScheduleGeneratorFactory loanScheduleFactory, final FloatingRatesReadPlatformService floatingRatesReadPlatformService,
            final FromJsonHelper fromApiJsonHelper, final CalendarReadPlatformService calendarReadPlatformService) {
        this.applicationCurrencyRepository = applicationCurrencyRepository;
        this.calendarInstanceRepository = calendarInstanceRepository;
        this.configurationDomainService = configurationDomainService;
        this.holidayRepository = holidayRepository;
        this.workingDaysRepository = workingDaysRepository;
        this.loanScheduleFactory = loanScheduleFactory;
        this.floatingRatesReadPlatformService = floatingRatesReadPlatformService;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.calendarReadPlatformService = calendarReadPlatformService;
    }

    public ScheduleGeneratorDTO buildScheduleGeneratorDTO(final Loan loan, final LocalDate recalculateFrom) {
        final HolidayDetailDTO holidayDetailDTO = null;
        return buildScheduleGeneratorDTO(loan, recalculateFrom, holidayDetailDTO);
    }

    public ScheduleGeneratorDTO buildScheduleGeneratorDTO(final Loan loan, final LocalDate recalculateFrom,
            final HolidayDetailDTO holidayDetailDTO) {
        HolidayDetailDTO holidayDetails = holidayDetailDTO;
        if (holidayDetailDTO == null) {
            holidayDetails = constructHolidayDTO(loan);
        }
        final MonetaryCurrency currency = loan.getCurrency();
        ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);
        final CalendarInstance calendarInstance = this.calendarInstanceRepository.findCalendarInstaneByEntityId(loan.getId(),
                CalendarEntityType.LOANS.getValue());
        Calendar calendar = null;
        CalendarHistoryDataWrapper calendarHistoryDataWrapper = null;
        if (calendarInstance != null) {
            calendar = calendarInstance.getCalendar();
            Set<CalendarHistory> calendarHistory = calendar.getCalendarHistory();
            calendarHistoryDataWrapper = new CalendarHistoryDataWrapper(calendarHistory);
        }
        LocalDate calculatedRepaymentsStartingFromDate = this.getCalculatedRepaymentsStartingFromDate(loan.getDisbursementDate(), loan,
                calendarInstance, calendarHistoryDataWrapper);
        CalendarInstance restCalendarInstance = null;
        CalendarInstance compoundingCalendarInstance = null;
        Long overdurPenaltyWaitPeriod = null;
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            restCalendarInstance = calendarInstanceRepository.findCalendarInstaneByEntityId(loan.loanInterestRecalculationDetailId(),
                    CalendarEntityType.LOAN_RECALCULATION_REST_DETAIL.getValue());
            compoundingCalendarInstance = calendarInstanceRepository.findCalendarInstaneByEntityId(loan.loanInterestRecalculationDetailId(),
                    CalendarEntityType.LOAN_RECALCULATION_COMPOUNDING_DETAIL.getValue());
            overdurPenaltyWaitPeriod = this.configurationDomainService.retrievePenaltyWaitPeriod();
        }
        final Boolean isInterestChargedFromDateAsDisbursementDateEnabled = this.configurationDomainService
                .isInterestChargedFromDateSameAsDisbursementDate();
        FloatingRateDTO floatingRateDTO = constructFloatingRateDTO(loan);
        Boolean isSkipRepaymentOnFirstMonth = false;
        Integer numberOfDays = 0;
        boolean isSkipRepaymentOnFirstMonthEnabled = configurationDomainService.isSkippingMeetingOnFirstDayOfMonthEnabled();
        if (isSkipRepaymentOnFirstMonthEnabled) {
            isSkipRepaymentOnFirstMonth = isLoanRepaymentsSyncWithMeeting(loan.group(), calendar);
            if (isSkipRepaymentOnFirstMonth) {
                numberOfDays = configurationDomainService.retreivePeroidInNumberOfDaysForSkipMeetingDate().intValue();
            }
        }
        final Boolean isChangeEmiIfRepaymentDateSameAsDisbursementDateEnabled = this.configurationDomainService
                .isChangeEmiIfRepaymentDateSameAsDisbursementDateEnabled();

        boolean isFirstRepaymentDateAllowedOnHoliday = this.configurationDomainService
                .isFirstRepaymentDateAfterRescheduleAllowedOnHoliday();

        boolean isInterestToBeRecoveredFirstWhenGreaterThanEMI = this.configurationDomainService
                .isInterestToBeRecoveredFirstWhenGreaterThanEMI();
        boolean isPrincipalCompoundingDisabledForOverdueLoans = this.configurationDomainService
                .isPrincipalCompoundingDisabledForOverdueLoans();

        ScheduleGeneratorDTO scheduleGeneratorDTO = new ScheduleGeneratorDTO(loanScheduleFactory, applicationCurrency,
                calculatedRepaymentsStartingFromDate, holidayDetails, restCalendarInstance, compoundingCalendarInstance, recalculateFrom,
                overdurPenaltyWaitPeriod, floatingRateDTO, calendar, calendarHistoryDataWrapper,
                isInterestChargedFromDateAsDisbursementDateEnabled, numberOfDays, isSkipRepaymentOnFirstMonth,
                isChangeEmiIfRepaymentDateSameAsDisbursementDateEnabled, isFirstRepaymentDateAllowedOnHoliday,
                isInterestToBeRecoveredFirstWhenGreaterThanEMI, isPrincipalCompoundingDisabledForOverdueLoans);

        return scheduleGeneratorDTO;
    }

    public Boolean isLoanRepaymentsSyncWithMeeting(final Group group, final Calendar calendar) {
        Boolean isSkipRepaymentOnFirstMonth = false;
        Long entityId = null;
        Long entityTypeId = null;

        if (group != null) {
            if (group.getParent() != null) {
                entityId = group.getParent().getId();
                entityTypeId = CalendarEntityType.CENTERS.getValue().longValue();
            } else {
                entityId = group.getId();
                entityTypeId = CalendarEntityType.GROUPS.getValue().longValue();
            }
        }

        if (entityId == null || calendar == null) {
            return isSkipRepaymentOnFirstMonth;
        }
        isSkipRepaymentOnFirstMonth = this.calendarReadPlatformService.isCalendarAssociatedWithEntity(entityId, calendar.getId(),
                entityTypeId);
        return isSkipRepaymentOnFirstMonth;
    }

    public LocalDate getCalculatedRepaymentsStartingFromDate(final Loan loan) {
        final CalendarInstance calendarInstance = this.calendarInstanceRepository.findCalendarInstaneByEntityId(loan.getId(),
                CalendarEntityType.LOANS.getValue());
        final CalendarHistoryDataWrapper calendarHistoryDataWrapper = null;
        return this.getCalculatedRepaymentsStartingFromDate(loan.getDisbursementDate(), loan, calendarInstance, calendarHistoryDataWrapper);
    }

    private HolidayDetailDTO constructHolidayDTO(final Loan loan) {
        final boolean isHolidayEnabled = this.configurationDomainService.isRescheduleRepaymentsOnHolidaysEnabled();
        final List<Holiday> holidays = this.holidayRepository.findByOfficeIdAndGreaterThanDate(loan.getOfficeId(),
                loan.getDisbursementDate(), HolidayStatusType.ACTIVE.getValue());
        final WorkingDays workingDays = this.workingDaysRepository.findOne();
        final boolean allowTransactionsOnHoliday = this.configurationDomainService.allowTransactionsOnHolidayEnabled();
        final boolean allowTransactionsOnNonWorkingDay = this.configurationDomainService.allowTransactionsOnNonWorkingDayEnabled();

        HolidayDetailDTO holidayDetailDTO = new HolidayDetailDTO(isHolidayEnabled, holidays, workingDays, allowTransactionsOnHoliday,
                allowTransactionsOnNonWorkingDay);
        return holidayDetailDTO;
    }

    private FloatingRateDTO constructFloatingRateDTO(final Loan loan) {
        FloatingRateDTO floatingRateDTO = null;
        if (loan.loanProduct().isLinkedToFloatingInterestRate()) {
            boolean isFloatingInterestRate = loan.getIsFloatingInterestRate();
            BigDecimal interestRateDiff = loan.getInterestRateDifferential();
            List<FloatingRatePeriodData> baseLendingRatePeriods = null;
            try {
                baseLendingRatePeriods = this.floatingRatesReadPlatformService.retrieveBaseLendingRate().getRatePeriods();
            } catch (final FloatingRateNotFoundException ex) {
                // Do not do anything
            }

            floatingRateDTO = new FloatingRateDTO(isFloatingInterestRate, loan.getDisbursementDate(), interestRateDiff,
                    baseLendingRatePeriods);
        }
        return floatingRateDTO;
    }

    private LocalDate getCalculatedRepaymentsStartingFromDate(final LocalDate actualDisbursementDate, final Loan loan,
            final CalendarInstance calendarInstance, final CalendarHistoryDataWrapper calendarHistoryDataWrapper) {
        final Calendar calendar = calendarInstance == null ? null : calendarInstance.getCalendar();
        return calculateRepaymentStartingFromDate(actualDisbursementDate, loan, calendar, calendarHistoryDataWrapper);
    }

    public LocalDate getCalculatedRepaymentsStartingFromDate(final LocalDate actualDisbursementDate, final Loan loan,
            final Calendar calendar) {
        final CalendarHistoryDataWrapper calendarHistoryDataWrapper = null;
        if (calendar == null) {
            return getCalculatedRepaymentsStartingFromDate(loan);
        }
        return calculateRepaymentStartingFromDate(actualDisbursementDate, loan, calendar, calendarHistoryDataWrapper);

    }

    private LocalDate calculateRepaymentStartingFromDate(final LocalDate actualDisbursementDate, final Loan loan, final Calendar calendar,
            final CalendarHistoryDataWrapper calendarHistoryDataWrapper) {
        LocalDate calculatedRepaymentsStartingFromDate = loan.getExpectedFirstRepaymentOnDate();
        if (calendar != null) { // sync repayments

            if (calculatedRepaymentsStartingFromDate == null && !calendar.getCalendarHistory().isEmpty()
                    && calendarHistoryDataWrapper != null) {
                // generate the first repayment date based on calendar history
                calculatedRepaymentsStartingFromDate = generateCalculatedRepaymentStartDate(calendarHistoryDataWrapper,
                        actualDisbursementDate, loan);
                return calculatedRepaymentsStartingFromDate;
            }

            // TODO: AA - user provided first repayment date takes precedence
            // over recalculated meeting date
            if (calculatedRepaymentsStartingFromDate == null) {
                // FIXME: AA - Possibility of having next meeting date
                // immediately after disbursement date,
                // need to have minimum number of days gap between disbursement
                // and first repayment date.
                final LoanProductRelatedDetail repaymentScheduleDetails = loan.repaymentScheduleDetail();
                // Not expecting to be null
                if (repaymentScheduleDetails != null) {
                    final Integer repayEvery = repaymentScheduleDetails.getRepayEvery();
                    final String frequency = CalendarUtils
                            .getMeetingFrequencyFromPeriodFrequencyType(repaymentScheduleDetails.getRepaymentPeriodFrequencyType());
                    Boolean isSkipRepaymentOnFirstMonth = false;
                    Integer numberOfDays = 0;
                    boolean isSkipRepaymentOnFirstMonthEnabled = this.configurationDomainService
                            .isSkippingMeetingOnFirstDayOfMonthEnabled();
                    if (isSkipRepaymentOnFirstMonthEnabled) {
                        numberOfDays = configurationDomainService.retreivePeroidInNumberOfDaysForSkipMeetingDate().intValue();
                        isSkipRepaymentOnFirstMonth = isLoanRepaymentsSyncWithMeeting(loan.group(), calendar);
                    }
                    calculatedRepaymentsStartingFromDate = CalendarUtils.getFirstRepaymentMeetingDate(calendar, actualDisbursementDate,
                            repayEvery, frequency, isSkipRepaymentOnFirstMonth, numberOfDays);
                }
            }
        }
        return calculatedRepaymentsStartingFromDate;
    }

    private LocalDate generateCalculatedRepaymentStartDate(final CalendarHistoryDataWrapper calendarHistoryDataWrapper,
            LocalDate actualDisbursementDate, Loan loan) {
        final LoanProductRelatedDetail repaymentScheduleDetails = loan.repaymentScheduleDetail();
        final WorkingDays workingDays = this.workingDaysRepository.findOne();
        LocalDate calculatedRepaymentsStartingFromDate = null;

        List<CalendarHistory> historyList = calendarHistoryDataWrapper.getCalendarHistoryList();

        if (historyList != null && historyList.size() > 0) {
            if (repaymentScheduleDetails != null) {
                final Integer repayEvery = repaymentScheduleDetails.getRepayEvery();
                final String frequency = CalendarUtils
                        .getMeetingFrequencyFromPeriodFrequencyType(repaymentScheduleDetails.getRepaymentPeriodFrequencyType());
                Boolean isSkipRepaymentOnFirstMonth = false;
                Integer numberOfDays = 0;
                boolean isSkipRepaymentOnFirstMonthEnabled = this.configurationDomainService.isSkippingMeetingOnFirstDayOfMonthEnabled();
                if (isSkipRepaymentOnFirstMonthEnabled) {
                    numberOfDays = configurationDomainService.retreivePeroidInNumberOfDaysForSkipMeetingDate().intValue();
                    isSkipRepaymentOnFirstMonth = isLoanRepaymentsSyncWithMeeting(loan.group(), historyList.get(0).getCalendar());
                }
                calculatedRepaymentsStartingFromDate = CalendarUtils.getNextRepaymentMeetingDate(historyList.get(0).getRecurrence(),
                        historyList.get(0).getStartDateLocalDate(), actualDisbursementDate, repayEvery, frequency, workingDays,
                        isSkipRepaymentOnFirstMonth, numberOfDays);
            }
        }
        return calculatedRepaymentsStartingFromDate;
    }

    public List<LoanDisbursementDetails> fetchDisbursementData(final JsonObject command) {
        final Locale locale = this.fromApiJsonHelper.extractLocaleParameter(command);
        final String dateFormat = this.fromApiJsonHelper.extractDateFormatParameter(command);
        List<LoanDisbursementDetails> disbursementDatas = new ArrayList<>();
        if (command.has(LoanApiConstants.disbursementDataParameterName)) {
            final JsonArray disbursementDataArray = command.getAsJsonArray(LoanApiConstants.disbursementDataParameterName);
            if (disbursementDataArray != null && disbursementDataArray.size() > 0) {
                int i = 0;
                do {
                    final JsonObject jsonObject = disbursementDataArray.get(i).getAsJsonObject();
                    LocalDate expectedDisbursementDate = null;
                    LocalDate actualDisbursementDate = null;
                    BigDecimal principal = null;
                    BigDecimal netDisbursalAmount = null;

                    if (jsonObject.has(LoanApiConstants.disbursementDateParameterName)) {
                        expectedDisbursementDate = this.fromApiJsonHelper
                                .extractLocalDateNamed(LoanApiConstants.disbursementDateParameterName, jsonObject, dateFormat, locale);
                    }
                    if (jsonObject.has(LoanApiConstants.disbursementPrincipalParameterName)
                            && jsonObject.get(LoanApiConstants.disbursementPrincipalParameterName).isJsonPrimitive()
                            && StringUtils.isNotBlank(jsonObject.get(LoanApiConstants.disbursementPrincipalParameterName).getAsString())) {
                        principal = jsonObject.getAsJsonPrimitive(LoanApiConstants.disbursementPrincipalParameterName).getAsBigDecimal();
                    }
                    if (jsonObject.has(LoanApiConstants.disbursementNetDisbursalAmountParameterName)
                            && jsonObject.get(LoanApiConstants.disbursementNetDisbursalAmountParameterName).isJsonPrimitive()
                            && StringUtils.isNotBlank(
                                    jsonObject.get(LoanApiConstants.disbursementNetDisbursalAmountParameterName).getAsString())) {
                        netDisbursalAmount = jsonObject.getAsJsonPrimitive(LoanApiConstants.disbursementNetDisbursalAmountParameterName)
                                .getAsBigDecimal();
                    }

                    disbursementDatas.add(
                            new LoanDisbursementDetails(expectedDisbursementDate, actualDisbursementDate, principal, netDisbursalAmount));
                    i++;
                } while (i < disbursementDataArray.size());
            }
        }
        return disbursementDatas;
    }

    public void validateRepaymentTransactionType(LoanTransactionType repaymentTransactionType) {
        if (!repaymentTransactionType.isRepaymentType()) {
            throw new PlatformServiceUnavailableException("error.msg.repaymentTransactionType.provided.not.a.repayment.type",
                    "Loan :" + repaymentTransactionType.getCode() + " Repayment Transaction Type provided is not a Repayment Type",
                    repaymentTransactionType.getCode());
        }
    }

    /**
     * Calculate the CAT rate for a given loan considering the principal amount, disbursement charges and for each
     * installment, the outstanding amount to pay
     *
     * @param loan
     *            the loan
     * @param isVatRequired
     *            indicates whether VAT is required for the loan
     * @return the CAT rate
     */
    public BigDecimal getCalculatedCatRate(final Loan loan, final Boolean isVatRequired) {
        BigDecimal catRate;
        Money vatAmount = Money.zero(loan.getCurrency());
        List<CashFlowData> cashFlows = new ArrayList<>();

        // for period = 0, the disbursement amount must be negative
        double[] cash_flows = new double[loan.getRepaymentScheduleInstallments().size() + 1];
        cash_flows[0] = loan.getPrincpal().getAmount().doubleValue() * -1;

        // for period = 0, consider the charges amount
        for (LoanCharge charge : loan.charges()) {
            if (charge.isActive()) {
                // check for Disbursement Charge
                if (charge.isDisbursementCharge()) {
                    cash_flows[0] = cash_flows[0] + charge.getAmount(loan.getCurrency()).getAmount().doubleValue();
                }
                // check for Origination Charge
                if (charge.isOriginationFee()) {
                    if (!isVatRequired) {
                        vatAmount = calculateVatCharge(loan.getVatPercentage(), charge.getAmount(loan.getCurrency()));
                        cash_flows[0] = cash_flows[0] - vatAmount.getAmount().doubleValue();
                    }
                }
            }
        }
        cashFlows.add(new CashFlowData(cash_flows[0], 0));

        // calculate cat rate using XIRR implementation based on the Newton-Raphson method
        generateCashFlowsForInstallments(loan, isVatRequired, cashFlows, vatAmount);
        double xirrResult = XIRR.calculateXIRR(cashFlows);
        catRate = Money.of(loan.getCurrency(), new BigDecimal(xirrResult * 100)).getAmount();

        return catRate;
    }

    private Money calculateVatCharge(BigDecimal vatPercentage, Money chargeAmount) {
        BigDecimal vatConverted = vatPercentage.divide(BigDecimal.valueOf(100));
        final Money vatPortion = chargeAmount.multipliedBy(vatConverted);
        return vatPortion;
    }

    private void generateCashFlowsForInstallments(Loan loan, Boolean isVatRequired, List<CashFlowData> cashFlows, Money vatAmount) {
        double amount;
        LocalDate disbursementDate = loan.getDisbursementDate();

        // generate the list of pairs composed by amount and date
        for (int i = 0; i < loan.getRepaymentScheduleInstallments().size(); i++) {
            LoanRepaymentScheduleInstallment per = (LoanRepaymentScheduleInstallment) loan.getRepaymentScheduleInstallments().toArray()[i];
            if (isVatRequired) {
                amount = per.getPrincipal(loan.getCurrency()).plus(per.getFeeChargesCharged(loan.getCurrency()).getAmount().doubleValue())
                        .plus(per.getPenaltyChargesCharged(loan.getCurrency())).plus(per.getVatOnInterestCharged(loan.getCurrency()))
                        .plus(per.getVatOnChargeExpected(loan.getCurrency())).getAmount().doubleValue();
            } else {
                amount = per.getPrincipal(loan.getCurrency()).plus(per.getFeeChargesCharged(loan.getCurrency()).getAmount().doubleValue())
                        .plus(per.getPenaltyChargesCharged(loan.getCurrency())).getAmount().doubleValue();
            }
            // calculate days since disbursement date
            int numberOfDays = Math.toIntExact(daysBetween(disbursementDate, per.getDueDate()));

            // add new cash flow data into the list
            cashFlows.add(new CashFlowData(amount, numberOfDays));
        }
    }

    @SuppressWarnings("unused")
    private void calculateCashFlowsForInstallments(Loan loan, Boolean isVatRequired, double[] cash_flows) {
        for (int i = 0; i < loan.getRepaymentScheduleInstallments().size(); i++) {
            LoanRepaymentScheduleInstallment per = (LoanRepaymentScheduleInstallment) loan.getRepaymentScheduleInstallments().toArray()[i];
            if (isVatRequired) {
                cash_flows[i + 1] = per.getPrincipal(loan.getCurrency()).plus(per.getInterestCharged(loan.getCurrency()))
                        .plus(per.getFeeChargesCharged(loan.getCurrency()).getAmount().doubleValue())
                        .plus(per.getPenaltyChargesCharged(loan.getCurrency())).plus(per.getVatOnInterestCharged(loan.getCurrency()))
                        .plus(per.getVatOnChargeExpected(loan.getCurrency())).getAmount().doubleValue();
            } else {
                cash_flows[i + 1] = per.getPrincipal(loan.getCurrency()).plus(per.getInterestCharged(loan.getCurrency()))
                        .plus(per.getFeeChargesCharged(loan.getCurrency()).getAmount().doubleValue())
                        .plus(per.getPenaltyChargesCharged(loan.getCurrency())).getAmount().doubleValue();
            }
        }
    }

    private static long daysBetween(LocalDate d1, LocalDate d2) {
        return ChronoUnit.DAYS.between(d1, d2);
    }

    public BigDecimal calculateEffectiveRate(BigDecimal annualInterestRate) {
        BigDecimal exponent = BigDecimal.valueOf(DaysInYearType.DAYS_365.getValue().doubleValue()).divide(BigDecimal.valueOf(daysInMonth),
                MathContext.DECIMAL64);
        BigDecimal interestRate = annualInterestRate.divide(divisor, MathContext.DECIMAL64);
        BigDecimal base = interestRate.divide(exponent, MathContext.DECIMAL64).add(BigDecimal.ONE);
        Double effectiveRateInterest = Math.pow(base.doubleValue(), exponent.doubleValue()) - 1;
        BigDecimal effectiveRateInterestBigDecimal = BigDecimal.valueOf(effectiveRateInterest).multiply(divisor, MathContext.DECIMAL64)
                .setScale(9, MoneyHelper.getRoundingMode());

        return effectiveRateInterestBigDecimal;
    }

    public BigDecimal calculateEffectiveRateWithVat(BigDecimal annualInterestRate, BigDecimal vatRate) {
        BigDecimal exponent = BigDecimal.valueOf(DaysInYearType.DAYS_365.getValue().doubleValue()).divide(BigDecimal.valueOf(daysInMonth),
                MathContext.DECIMAL64);
        BigDecimal interestRate = annualInterestRate.divide(divisor, MathContext.DECIMAL64);
        BigDecimal vatRateForCalculation = vatRate.divide(divisor, MathContext.DECIMAL64).add(BigDecimal.ONE);
        BigDecimal base = interestRate.divide(exponent, MathContext.DECIMAL64).multiply(vatRateForCalculation, MathContext.DECIMAL64)
                .add(BigDecimal.ONE);
        Double effectiveRateInterest = Math.pow(base.doubleValue(), exponent.doubleValue()) - 1;
        BigDecimal effectiveRateInterestBigDecimal = BigDecimal.valueOf(effectiveRateInterest).multiply(divisor, MathContext.DECIMAL64)
                .setScale(9, MoneyHelper.getRoundingMode());

        return effectiveRateInterestBigDecimal;
    }

    public Integer calculateDurationOfLoan(Loan loan) {
        LoanRepaymentScheduleInstallment lastInstallment = loan.getRepaymentScheduleInstallments()
                .get(loan.getRepaymentScheduleInstallments().size() - 1);

        LocalDate disbursementDate = loan.getDisbursementDate();
        LocalDate dueDateLastInstallment = lastInstallment.getDueDate();

        // Calculate the duration of a loan between disbursement date and the last installment due date
        Integer daysInPeriod = Math.toIntExact(ChronoUnit.DAYS.between(disbursementDate, dueDateLastInstallment));
        return daysInPeriod;
    }

    public BigDecimal getLoanVatPercentage(Loan loan) {
        BigDecimal vatPercentage = BigDecimal.ZERO;
        if (loan.isVatRequired() && loan.getClient() != null && loan.getClient().getVatRate() != null
                && loan.getClient().getVatRate().getPercentage() % 1 == 0) {
            vatPercentage = BigDecimal.valueOf(loan.getClient().getVatRate().getPercentage());
        }
        return vatPercentage;
    }

    public BigDecimal calculateLoanFutureValue(Loan loan) {
        BigDecimal loanFutureValue;

        // get values to use in the formula
        BigDecimal loanAmount = loan.getNetDisbursalAmount();
        Integer loanDuration = calculateDurationOfLoan(loan);
        BigDecimal vatRate = getLoanVatPercentage(loan);
        BigDecimal effectiveAnnualRateWithVat = calculateEffectiveRateWithVat(
                loan.getLoanProductRelatedDetail().getAnnualNominalInterestRate(), vatRate);

        // calculate elements needed for the formula
        BigDecimal interestRate = effectiveAnnualRateWithVat.divide(divisor, MathContext.DECIMAL64);
        BigDecimal base = interestRate.add(BigDecimal.ONE);
        BigDecimal exponent = BigDecimal.valueOf(loanDuration.intValue())
                .divide(BigDecimal.valueOf(DaysInYearType.DAYS_365.getValue().intValue()), MathContext.DECIMAL64);
        Double powResult = Math.pow(base.doubleValue(), exponent.doubleValue());

        loanFutureValue = loanAmount.multiply(BigDecimal.valueOf(powResult), MathContext.DECIMAL64).setScale(9,
                MoneyHelper.getRoundingMode());

        return loanFutureValue;
    }

    public Map<Integer, BigDecimal> calculateFactorPerInstallment(Loan loan) {
        Map<Integer, BigDecimal> factorInstallmentsMapping = new HashMap<>();

        LoanRepaymentScheduleInstallment lastInstallment = loan.getRepaymentScheduleInstallments()
                .get(loan.getRepaymentScheduleInstallments().size() - 1);
        LocalDate dueDateLastInstallment = lastInstallment.getDueDate();

        // get values to use in the formula
        BigDecimal vatRate = getLoanVatPercentage(loan);
        BigDecimal effectiveAnnualRateWithVat = calculateEffectiveRateWithVat(
                loan.getLoanProductRelatedDetail().getAnnualNominalInterestRate(), vatRate);
        BigDecimal interestRate = effectiveAnnualRateWithVat.divide(divisor, MathContext.DECIMAL64);
        BigDecimal base = interestRate.add(BigDecimal.ONE);

        // calculate the factor per installment of the loan
        for (int i = 0; i < loan.getRepaymentScheduleInstallments().size(); i++) {
            LoanRepaymentScheduleInstallment currentInstallment = loan.getRepaymentScheduleInstallments().get(i);

            // Calculate the duration of a loan between the last installment due date and the installment due date
            Integer daysInPeriod = Math.toIntExact(ChronoUnit.DAYS.between(currentInstallment.getDueDate(), dueDateLastInstallment));
            BigDecimal exponent = BigDecimal.valueOf(daysInPeriod.intValue())
                    .divide(BigDecimal.valueOf(DaysInYearType.DAYS_365.getValue().intValue()), MathContext.DECIMAL64);

            // Calculate the factor of the current installment
            Double powResult = Math.pow(base.doubleValue(), exponent.doubleValue());
            BigDecimal factorInstallment = BigDecimal.valueOf(powResult).setScale(9, MoneyHelper.getRoundingMode());

            factorInstallmentsMapping.put(currentInstallment.getInstallmentNumber(), factorInstallment);
        }

        return factorInstallmentsMapping;
    }

    public BigDecimal calculateTotalInstallmentWithVat(Loan loan) {
        BigDecimal totalInstallmentWithVat;

        // get values to use in the formula
        BigDecimal loanDisbursedAmount = loan.getNetDisbursalAmount();
        BigDecimal loanFutureValue = calculateLoanFutureValue(loan);
        int numberOfInstallments = loan.getRepaymentScheduleInstallments().size();
        Map<Integer, BigDecimal> factorInstallmentsMap = calculateFactorPerInstallment(loan);
        BigDecimal installmentsFactorTotal = factorInstallmentsMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal installmentFee = BigDecimal.ZERO;
        BigDecimal collectionFee = BigDecimal.ZERO;

        for (LoanCharge charge : loan.getCharges()) {
            if (charge.isActive() && charge.isCollectionFee()) {
                collectionFee = collectionFee.add(charge.amountOrPercentage());
            }
        }

        BigDecimal vatPercentage = getLoanVatPercentage(loan);
        vatPercentage = vatPercentage.divide(divisor, MathContext.DECIMAL64).add(BigDecimal.ONE);

        // for period = 0, the disbursement amount must be negative
        BigDecimal loanAmountWithFeeAndCharges = loanDisbursedAmount;
        for (LoanCharge charge : loan.charges()) {
            if (charge.isActive() && charge.isDisbursementCharge()) {
                loanAmountWithFeeAndCharges = loanAmountWithFeeAndCharges.subtract(charge.getAmount(loan.getCurrency()).getAmount(),
                        MathContext.DECIMAL64);
            }
        }

        // calculate total installments with VAT
        BigDecimal futureValueOverFactors = loanFutureValue.divide(installmentsFactorTotal, MathContext.DECIMAL64);
        BigDecimal loanAmountOverNumberOfInstallments = loanAmountWithFeeAndCharges.multiply(installmentFee, MathContext.DECIMAL64)
                .divide(BigDecimal.valueOf(numberOfInstallments), MathContext.DECIMAL64).add(collectionFee);
        totalInstallmentWithVat = loanAmountOverNumberOfInstallments.multiply(vatPercentage, MathContext.DECIMAL64)
                .add(futureValueOverFactors);

        return totalInstallmentWithVat;
    }

    public BigDecimal calculatePeriodicInterestRate(BigDecimal annualNominalInterestRate, LocalDate periodStartDate,
            LocalDate periodEndDate) {
        BigDecimal periodicInterestRate;

        BigDecimal daysPerYear = BigDecimal.valueOf(365);
        int exactDaysInPeriod = Math.toIntExact(ChronoUnit.DAYS.between(periodStartDate, periodEndDate));
        BigDecimal exactDaysInPeriodBigDecimal = BigDecimal.valueOf(exactDaysInPeriod);

        periodicInterestRate = annualNominalInterestRate.divide(daysPerYear, MathContext.DECIMAL64).divide(divisor, MathContext.DECIMAL64)
                .multiply(exactDaysInPeriodBigDecimal);

        return periodicInterestRate;
    }

}
