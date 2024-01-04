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
package org.apache.fineract.portfolio.loanaccount.api;

import static org.apache.fineract.portfolio.loanproduct.service.LoanEnumerations.interestType;

import com.google.gson.JsonElement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.accounting.journalentry.api.DateParam;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.data.GlobalEntityType;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookPopulatorService;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookService;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.ApiParameterHelper;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.api.JsonQuery;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.data.UploadRequest;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant;
import org.apache.fineract.infrastructure.dataqueries.data.DatatableData;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksReadService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.agency.data.AgencyData;
import org.apache.fineract.organisation.agency.service.AgencyReadPlatformServiceImpl;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.office.domain.OfficeHierarchyLevel;
import org.apache.fineract.organisation.prequalification.data.GroupPrequalificationData;
import org.apache.fineract.organisation.prequalification.data.LoanAdditionalData;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationType;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.data.PortfolioAccountDTO;
import org.apache.fineract.portfolio.account.data.PortfolioAccountData;
import org.apache.fineract.portfolio.account.service.AccountAssociationsReadPlatformService;
import org.apache.fineract.portfolio.account.service.PortfolioAccountReadPlatformService;
import org.apache.fineract.portfolio.accountdetails.data.LoanAccountSummaryData;
import org.apache.fineract.portfolio.accountdetails.service.AccountDetailsReadPlatformService;
import org.apache.fineract.portfolio.calendar.data.CalendarData;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.service.CalendarReadPlatformService;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.collateralmanagement.data.LoanCollateralResponseData;
import org.apache.fineract.portfolio.collateralmanagement.service.LoanCollateralManagementReadPlatformService;
import org.apache.fineract.portfolio.cupo.data.CupoData;
import org.apache.fineract.portfolio.cupo.domain.CupoStatus;
import org.apache.fineract.portfolio.cupo.service.CupoReadService;
import org.apache.fineract.portfolio.floatingrates.data.InterestRatePeriodData;
import org.apache.fineract.portfolio.fund.data.FundData;
import org.apache.fineract.portfolio.fund.service.FundReadPlatformService;
import org.apache.fineract.portfolio.group.data.CenterData;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.group.service.CenterReadPlatformServiceImpl;
import org.apache.fineract.portfolio.group.service.GroupReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.AgeLimitStatus;
import org.apache.fineract.portfolio.loanaccount.data.AgeLimitStatusEnumerations;
import org.apache.fineract.portfolio.loanaccount.data.CollectionData;
import org.apache.fineract.portfolio.loanaccount.data.DisbursementData;
import org.apache.fineract.portfolio.loanaccount.data.GlimRepaymentTemplate;
import org.apache.fineract.portfolio.loanaccount.data.GroupLoanAdditionalData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.LoanApprovalData;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.data.LoanCollateralManagementData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.data.PaidInAdvanceData;
import org.apache.fineract.portfolio.loanaccount.data.RepaymentScheduleRelatedLoanData;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDisbursementMethod;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTermVariationType;
import org.apache.fineract.portfolio.loanaccount.exception.LoanTemplateTypeRequiredException;
import org.apache.fineract.portfolio.loanaccount.exception.NotSupportedLoanTemplateTypeException;
import org.apache.fineract.portfolio.loanaccount.guarantor.data.GuarantorData;
import org.apache.fineract.portfolio.loanaccount.guarantor.service.GuarantorReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleCalculationPlatformService;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.GLIMAccountInfoReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanChargeReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.LoanProductConstants;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.loanproduct.data.TransactionProcessingStrategyData;
import org.apache.fineract.portfolio.loanproduct.domain.InterestMethod;
import org.apache.fineract.portfolio.loanproduct.service.LoanDropdownReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.fineract.portfolio.note.data.NoteData;
import org.apache.fineract.portfolio.note.domain.NoteType;
import org.apache.fineract.portfolio.note.service.NoteReadPlatformService;
import org.apache.fineract.portfolio.rate.data.RateData;
import org.apache.fineract.portfolio.rate.service.RateReadService;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Path("/loans")
@Component
@Scope("singleton")
@Tag(name = "Loans", description = "The API concept of loans models the loan application process and the loan contract/monitoring process.\n"
        + "\n" + "Field Descriptions\n" + "accountNo\n"
        + "The account no. associated with this loan. Is auto generated if not provided at loan application creation time.\n"
        + "externalId\n" + "A place to put an external reference for this loan e.g. The ID another system uses.\n"
        + "If provided, it must be unique.\n" + "fundId\n" + "Optional: For associating a loan with a given fund.\n" + "loanOfficerId\n"
        + "Optional: For associating a loan with a given staff member who is a loan officer.\n" + "loanPurposeId\n"
        + "Optional: For marking a loan with a given loan purpose option. Loan purposes are configurable and can be setup by system admin through code/code values screens.\n"
        + "principal\n" + "The loan amount to be disbursed to through loan.\n" + "loanTermFrequency\n" + "The length of loan term\n"
        + "Used like: loanTermFrequency loanTermFrequencyType\n" + "e.g. 12 Months\n" + "loanTermFrequencyType\n"
        + "The loan term period to use. Used like: loanTermFrequency loanTermFrequencyType\n"
        + "e.g. 12 Months Example Values: 0=Days, 1=Weeks, 2=Months, 3=Years\n" + "numberOfRepayments\n"
        + "Number of installments to repay.\n" + "Used like: numberOfRepayments Every repaymentEvery repaymentFrequencyType\n"
        + "e.g. 10 (repayments) Every 12 Weeks\n" + "repaymentEvery\n"
        + "Used like: numberOfRepayments Every repaymentEvery repaymentFrequencyType\n" + "e.g. 10 (repayments) Every 12 Weeks\n"
        + "repaymentFrequencyType\n" + "Used like: numberOfRepayments Every repaymentEvery repaymentFrequencyType\n"
        + "e.g. 10 (repayments) Every 12 Weeks \n" + "Example Values: 0=Days, 1=Weeks, 2=Months\n" + "interestRatePerPeriod\n"
        + "Interest Rate.\n" + "Used like: interestRatePerPeriod % interestRateFrequencyType - interestType\n"
        + "e.g. 12.0000% Per year - Declining Balance\n" + "interestRateFrequencyType\n"
        + "Used like: interestRatePerPeriod% interestRateFrequencyType - interestType\n" + "e.g. 12.0000% Per year - Declining Balance \n"
        + "Example Values: 2=Per month, 3=Per year\n" + "graceOnPrincipalPayment\n"
        + "Optional: Integer - represents the number of repayment periods that grace should apply to the principal component of a repayment period.\n"
        + "graceOnInterestPayment\n"
        + "Optional: Integer - represents the number of repayment periods that grace should apply to the interest component of a repayment period. Interest is still calculated but offset to later repayment periods.\n"
        + "graceOnInterestCharged\n" + "Optional: Integer - represents the number of repayment periods that should be interest-free.\n"
        + "graceOnArrearsAgeing\n"
        + "Optional: Integer - Used in Arrears calculation to only take into account loans that are more than graceOnArrearsAgeing days overdue.\n"
        + "interestChargedFromDate\n" + "Optional: Date - The date from with interest is to start being charged.\n"
        + "expectedDisbursementDate\n" + "The proposed disbursement date of the loan so a proposed repayment schedule can be provided.\n"
        + "submittedOnDate\n" + "The date the loan application was submitted by applicant.\n" + "linkAccountId\n"
        + "The Savings Account id for linking with loan account for payments.\n" + "amortizationType\n"
        + "Example Values: 0=Equal principle payments, 1=Equal installments\n" + "interestType\n"
        + "Used like: interestRatePerPeriod% interestRateFrequencyType - interestType\n" + "e.g. 12.0000% Per year - Declining Balance \n"
        + "Example Values: 0=Declining Balance, 1=Flat\n" + "interestCalculationPeriodType\n"
        + "Example Values: 0=Daily, 1=Same as repayment period\n" + "allowPartialPeriodInterestCalcualtion\n"
        + "This value will be supported along with interestCalculationPeriodType as Same as repayment period to calculate interest for partial periods. Example: Interest charged from is 5th of April , Principal is 10000 and interest is 1% per month then the interest will be (10000 * 1%)* (25/30) , it calculates for the month first then calculates exact periods between start date and end date(can be a decimal)\n"
        + "inArrearsTolerance\n" + "The amount that can be 'waived' at end of all loan payments because it is too small to worry about.\n"
        + "This is also the tolerance amount assessed when determining if a loan is in arrears.\n" + "transactionProcessingStrategyId\n"
        + "An enumeration that indicates the type of transaction processing strategy to be used. This relates to functionality that is also known as Payment Application Logic.\n"
        + "A number of out of the box approaches exist, some are custom to specific MFIs, some are more general and indicate the order in which payments are processed.\n"
        + "\n"
        + "Refer to the Payment Application Logic / Transaction Processing Strategy section in the appendix for more detailed overview of each available payment application logic provided out of the box.\n"
        + "\n" + "List of current approaches:\n" + "1 = Mifos style (Similar to Old Mifos)\n" + "2 = Heavensfamily (Custom MFI approach)\n"
        + "3 = Creocore (Custom MFI approach)\n" + "4 = RBI (India)\n" + "5 = Principal Interest Penalties Fees Order\n"
        + "6 = Interest Principal Penalties Fees Order\n" + "7 = Early Payment Strategy\n" + "loanType\n"
        + "To represent different type of loans.\n" + "At present there are three type of loans are supported. \n"
        + "Available loan types:\n" + "individual: Loan given to individual member\n" + "group: Loan given to group as a whole\n"
        + "jlg: Joint liability group loan given to members in a group on individual basis. JLG loan can be given to one or more members in a group.\n"
        + "recalculationRestFrequencyDate\n"
        + "Specifies rest frequency start date for interest recalculation. This date must be before or equal to disbursement date\n"
        + "recalculationCompoundingFrequencyDate\n"
        + "Specifies compounding frequency start date for interest recalculation. This date must be equal to disbursement date")
public class LoansApiResource {

    private final Set<String> loanDataParameters = new HashSet<>(Arrays.asList("id", "accountNo", "status", "externalId", "clientId",
            "group", "loanProductId", "loanProductName", "loanProductDescription", "isLoanProductLinkedToFloatingRate", "fundId",
            "fundName", "loanPurposeId", "loanPurposeName", "loanOfficerId", "loanOfficerName", "currency", "principal", "totalOverpaid",
            "inArrearsTolerance", "termFrequency", "termPeriodFrequencyType", "numberOfRepayments", "repaymentEvery",
            "interestRatePerPeriod", "annualInterestRate", "repaymentFrequencyType", "transactionProcessingStrategyId",
            "transactionProcessingStrategyName", "interestRateFrequencyType", "amortizationType", "interestType",
            "interestCalculationPeriodType", LoanProductConstants.ALLOW_PARTIAL_PERIOD_INTEREST_CALCUALTION_PARAM_NAME,
            "expectedFirstRepaymentOnDate", "graceOnPrincipalPayment", "recurringMoratoriumOnPrincipalPeriods", "graceOnInterestPayment",
            "graceOnInterestCharged", "interestChargedFromDate", "timeline", "totalFeeChargesAtDisbursement", "summary",
            "repaymentSchedule", "transactions", "charges", "collateral", "guarantors", "meeting", "productOptions",
            "amortizationTypeOptions", "interestTypeOptions", "interestCalculationPeriodTypeOptions", "repaymentFrequencyTypeOptions",
            "repaymentFrequencyNthDayTypeOptions", "repaymentFrequencyDaysOfWeekTypeOptions", "termFrequencyTypeOptions",
            "interestRateFrequencyTypeOptions", "fundOptions", "repaymentStrategyOptions", "chargeOptions", "loanOfficerOptions",
            "loanPurposeOptions", "loanCollateralOptions", "chargeTemplate", "calendarOptions", "syncDisbursementWithMeeting",
            "loanCounter", "loanProductCounter", "notes", "accountLinkingOptions", "linkedAccount", "interestRateDifferential",
            "isFloatingInterestRate", "interestRatesPeriods", LoanApiConstants.canUseForTopup, LoanApiConstants.isTopup,
            LoanApiConstants.loanIdToClose, LoanApiConstants.topupAmount, LoanApiConstants.clientActiveLoanOptions,
            LoanApiConstants.datatables, LoanProductConstants.RATES_PARAM_NAME, LoanApiConstants.MULTIDISBURSE_DETAILS_PARAMNAME,
            LoanApiConstants.EMI_AMOUNT_VARIATIONS_PARAMNAME, LoanApiConstants.COLLECTION_PARAMNAME));

    private final Set<String> loanApprovalDataParameters = new HashSet<>(Arrays.asList("approvalDate", "approvalAmount"));
    final Set<String> glimAccountsDataParameters = new HashSet<>(Arrays.asList("glimId", "groupId", "clientId", "parentLoanAccountNo",
            "parentPrincipalAmount", "childLoanAccountNo", "childPrincipalAmount", "clientName"));
    private final String resourceNameForPermissions = "LOAN";

    private final PlatformSecurityContext context;
    private final ClientReadPlatformService clientReadPlatformService;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanProductReadPlatformService loanProductReadPlatformService;
    private final LoanDropdownReadPlatformService dropdownReadPlatformService;
    private final FundReadPlatformService fundReadPlatformService;
    private final ChargeReadPlatformService chargeReadPlatformService;
    private final LoanChargeReadPlatformService loanChargeReadPlatformService;
    private final LoanScheduleCalculationPlatformService calculationPlatformService;
    private final GuarantorReadPlatformService guarantorReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final GroupReadPlatformService groupReadPlatformService;
    private final DefaultToApiJsonSerializer<LoanAccountData> toApiJsonSerializer;
    private final DefaultToApiJsonSerializer<LoanApprovalData> loanApprovalDataToApiJsonSerializer;
    private final DefaultToApiJsonSerializer<GroupLoanAdditionalData> loanAdditionalDataToApiJsonSerializer;
    private final DefaultToApiJsonSerializer<LoanScheduleData> loanScheduleToApiJsonSerializer;
    private final DefaultToApiJsonSerializer<EnumOptionData> ageLimitValidationJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final FromJsonHelper fromJsonHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final CalendarReadPlatformService calendarReadPlatformService;
    private final NoteReadPlatformService noteReadPlatformService;
    private final PortfolioAccountReadPlatformService portfolioAccountReadPlatformService;
    private final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService;
    private final LoanScheduleHistoryReadPlatformService loanScheduleHistoryReadPlatformService;
    private final AccountDetailsReadPlatformService accountDetailsReadPlatformService;
    private final EntityDatatableChecksReadService entityDatatableChecksReadService;
    private final BulkImportWorkbookService bulkImportWorkbookService;
    private final BulkImportWorkbookPopulatorService bulkImportWorkbookPopulatorService;
    private final RateReadService rateReadService;
    private final ConfigurationDomainService configurationDomainService;
    private final DefaultToApiJsonSerializer<GlimRepaymentTemplate> glimTemplateToApiJsonSerializer;
    private final GLIMAccountInfoReadPlatformService glimAccountInfoReadPlatformService;
    private final LoanCollateralManagementReadPlatformService loanCollateralManagementReadPlatformService;
    private final CupoReadService cupoReadService;
    private final AppUserReadPlatformService appUserReadPlatformService;
    private final AgencyReadPlatformServiceImpl agencyReadPlatformService;
    private final CenterReadPlatformServiceImpl centerReadPlatformService;

    public LoansApiResource(final PlatformSecurityContext context, final LoanReadPlatformService loanReadPlatformService,
                            final LoanProductReadPlatformService loanProductReadPlatformService,
                            final LoanDropdownReadPlatformService dropdownReadPlatformService, final FundReadPlatformService fundReadPlatformService,
                            final ChargeReadPlatformService chargeReadPlatformService, final LoanChargeReadPlatformService loanChargeReadPlatformService,
                            final LoanScheduleCalculationPlatformService calculationPlatformService,
                            final GuarantorReadPlatformService guarantorReadPlatformService, final ClientReadPlatformService clientReadPlatformService,
                            final CodeValueReadPlatformService codeValueReadPlatformService, final GroupReadPlatformService groupReadPlatformService,
                            final DefaultToApiJsonSerializer<LoanAccountData> toApiJsonSerializer,
                            final DefaultToApiJsonSerializer<LoanApprovalData> loanApprovalDataToApiJsonSerializer,
                            final DefaultToApiJsonSerializer<LoanScheduleData> loanScheduleToApiJsonSerializer,
                            final DefaultToApiJsonSerializer<EnumOptionData> ageLimitValidationJsonSerializer,
                            final DefaultToApiJsonSerializer<GroupLoanAdditionalData> loanAdditionalDataToApiJsonSerializer,
                            final ApiRequestParameterHelper apiRequestParameterHelper, final FromJsonHelper fromJsonHelper,
                            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
                            final CalendarReadPlatformService calendarReadPlatformService, final NoteReadPlatformService noteReadPlatformService,
                            final PortfolioAccountReadPlatformService portfolioAccountReadPlatformServiceImpl,
                            final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService,
                            final LoanScheduleHistoryReadPlatformService loanScheduleHistoryReadPlatformService,
                            final AccountDetailsReadPlatformService accountDetailsReadPlatformService,
                            final EntityDatatableChecksReadService entityDatatableChecksReadService,
                            final BulkImportWorkbookService bulkImportWorkbookService,
                            final BulkImportWorkbookPopulatorService bulkImportWorkbookPopulatorService, final RateReadService rateReadService,
                            final ConfigurationDomainService configurationDomainService,
                            final DefaultToApiJsonSerializer<GlimRepaymentTemplate> glimTemplateToApiJsonSerializer,
                            final GLIMAccountInfoReadPlatformService glimAccountInfoReadPlatformService,
                            final LoanCollateralManagementReadPlatformService loanCollateralManagementReadPlatformService,
                            final CupoReadService cupoReadService, AppUserReadPlatformService appUserReadPlatformService,
                            AgencyReadPlatformServiceImpl agencyReadPlatformService, CenterReadPlatformServiceImpl centerReadPlatformService) {
        this.context = context;
        this.loanReadPlatformService = loanReadPlatformService;
        this.loanProductReadPlatformService = loanProductReadPlatformService;
        this.dropdownReadPlatformService = dropdownReadPlatformService;
        this.fundReadPlatformService = fundReadPlatformService;
        this.chargeReadPlatformService = chargeReadPlatformService;
        this.loanChargeReadPlatformService = loanChargeReadPlatformService;
        this.calculationPlatformService = calculationPlatformService;
        this.guarantorReadPlatformService = guarantorReadPlatformService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.groupReadPlatformService = groupReadPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.loanApprovalDataToApiJsonSerializer = loanApprovalDataToApiJsonSerializer;
        this.loanScheduleToApiJsonSerializer = loanScheduleToApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.fromJsonHelper = fromJsonHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.calendarReadPlatformService = calendarReadPlatformService;
        this.noteReadPlatformService = noteReadPlatformService;
        this.portfolioAccountReadPlatformService = portfolioAccountReadPlatformServiceImpl;
        this.accountAssociationsReadPlatformService = accountAssociationsReadPlatformService;
        this.loanScheduleHistoryReadPlatformService = loanScheduleHistoryReadPlatformService;
        this.accountDetailsReadPlatformService = accountDetailsReadPlatformService;
        this.entityDatatableChecksReadService = entityDatatableChecksReadService;
        this.rateReadService = rateReadService;
        this.bulkImportWorkbookService = bulkImportWorkbookService;
        this.bulkImportWorkbookPopulatorService = bulkImportWorkbookPopulatorService;
        this.configurationDomainService = configurationDomainService;
        this.glimTemplateToApiJsonSerializer = glimTemplateToApiJsonSerializer;
        this.glimAccountInfoReadPlatformService = glimAccountInfoReadPlatformService;
        this.loanCollateralManagementReadPlatformService = loanCollateralManagementReadPlatformService;
        this.cupoReadService = cupoReadService;
        this.clientReadPlatformService = clientReadPlatformService;
        this.ageLimitValidationJsonSerializer = ageLimitValidationJsonSerializer;
        this.appUserReadPlatformService = appUserReadPlatformService;
        this.agencyReadPlatformService = agencyReadPlatformService;
        this.centerReadPlatformService = centerReadPlatformService;
        this.loanAdditionalDataToApiJsonSerializer = loanAdditionalDataToApiJsonSerializer;
    }

    /*
     * This template API is used for loan approval, ideally this should be invoked on loan that are pending for
     * approval. But system does not validate the status of the loan, it returns the template irrespective of loan
     * status
     */

    @GET
    @Path("{loanId}/template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveApprovalTemplate(@PathParam("loanId") @Parameter(description = "loanId") final Long loanId,
                                           @QueryParam("templateType") @Parameter(description = "templateType") final String templateType,
                                           @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        LoanApprovalData loanApprovalTemplate = null;

        if (templateType == null) {
            final String errorMsg = "Loan template type must be provided";
            throw new LoanTemplateTypeRequiredException(errorMsg);
        } else if (templateType.equals("approval")) {
            loanApprovalTemplate = this.loanReadPlatformService.retrieveApprovalTemplate(loanId);
        }

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.loanApprovalDataToApiJsonSerializer.serialize(settings, loanApprovalTemplate, this.loanApprovalDataParameters);

    }

    @GET
    @Path("{loanId}/additionals")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveLoanAdditionals(@PathParam("loanId") @Parameter(description = "loanId") final Long loanId,
                                          @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        GroupLoanAdditionalData groupLoanAdditionalsData = this.loanReadPlatformService.retrieveAdditionalData(loanId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.loanAdditionalDataToApiJsonSerializer.serialize(settings, groupLoanAdditionalsData, this.loanApprovalDataParameters);

    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Loan Details Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "\n" + "Field Defaults\n" + "Allowed description Lists\n" + "Example Requests:\n" + "\n"
            + "loans/template?templateType=individual&clientId=1\n" + "\n" + "\n"
            + "loans/template?templateType=individual&clientId=1&productId=1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.GetLoansTemplateResponse.class))) })
    public String template(@QueryParam("clientId") @Parameter(description = "clientId") final Long clientId,
                           @QueryParam("groupId") @Parameter(description = "groupId") final Long groupId,
                           @QueryParam("productId") @Parameter(description = "productId") final Long productId,
                           @QueryParam("templateType") @Parameter(description = "templateType") final String templateType,
                           @DefaultValue("false") @QueryParam("staffInSelectedOfficeOnly") @Parameter(description = "staffInSelectedOfficeOnly") final boolean staffInSelectedOfficeOnly,
                           @DefaultValue("false") @QueryParam("activeOnly") @Parameter(description = "activeOnly") final boolean onlyActive,
                           @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        // template
        final Collection<LoanProductData> productOptions = this.loanProductReadPlatformService.retrieveAllLoanProductsForLookup(onlyActive);

        // options
        Collection<StaffData> allowedLoanOfficers = null;
        Collection<CodeValueData> loanCollateralOptions = null;
        Collection<CalendarData> calendarOptions = null;
        LoanAccountData newLoanAccount = null;
        Long officeId = null;
        Collection<PortfolioAccountData> accountLinkingOptions = null;
        boolean isRatesEnabled = this.configurationDomainService.isSubRatesEnabled();

        if (productId != null) {
            newLoanAccount = this.loanReadPlatformService.retrieveLoanProductDetailsTemplate(productId, clientId, groupId, templateType);
        }

        if (templateType == null) {
            final String errorMsg = "Loan template type must be provided";
            throw new LoanTemplateTypeRequiredException(errorMsg);
        } else if (templateType.equals("collateral")) {
            loanCollateralOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("LoanCollateral");
            newLoanAccount = LoanAccountData.collateralTemplate(loanCollateralOptions);
        } else if (templateType.equals("groupAdditionals")) {
            Collection<CodeValueData> loanCycleCompletedOptions = this.codeValueReadPlatformService
                    .retrieveCodeValuesByCode("loanCycleCompletedOptions");
            Collection<CodeValueData> loanPurposeOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("loanPurposeOptions");
            Collection<CodeValueData> businessEvolutionOptions = this.codeValueReadPlatformService
                    .retrieveCodeValuesByCode("businessEvolutionOptions");
            Collection<CodeValueData> yesnoOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("yesnoOptions");
            Collection<CodeValueData> businessExperienceOptions = this.codeValueReadPlatformService
                    .retrieveCodeValuesByCode("businessExperienceOptions");
            Collection<CodeValueData> businessLocationOptions = this.codeValueReadPlatformService
                    .retrieveCodeValuesByCode("businessLocationOptions");
            Collection<CodeValueData> clientTypeOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("clientTypeOptions");
            Collection<CodeValueData> loanStatusOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("loanStatusOptions");
            Collection<CodeValueData> institutionTypeOptions = this.codeValueReadPlatformService
                    .retrieveCodeValuesByCode("institutionTypeOptions");
            Collection<CodeValueData> housingTypeOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("housingTypeOptions");
            Collection<CodeValueData> classificationOptions = this.codeValueReadPlatformService
                    .retrieveCodeValuesByCode("classificationOptions");
            Collection<CodeValueData> jobTypeOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("jobTypeOptions");
            Collection<CodeValueData> educationLevelOptions = this.codeValueReadPlatformService
                    .retrieveCodeValuesByCode("educationLevelOptions");
            Collection<CodeValueData> maritalStatusOptions = this.codeValueReadPlatformService
                    .retrieveCodeValuesByCode("maritalStatusOptions");
            Collection<CodeValueData> groupPositionOptions = this.codeValueReadPlatformService
                    .retrieveCodeValuesByCode("groupPositionOptions");
            Collection<CodeValueData> sourceOfFundsOptions = this.codeValueReadPlatformService
                    .retrieveCodeValuesByCode("sourceOfFundsOptions");
            Collection<CodeValueData> cancellationReasonOptions = this.codeValueReadPlatformService
                    .retrieveCodeValuesByCode("cancellationReasonOptions");
            Collection<CodeValueData> documentTypeOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("Document Type");

            final List<AppUserData> facilitatorOptions = new ArrayList<>(
                    this.appUserReadPlatformService.retrieveUsersUnderHierarchy(Long.valueOf(OfficeHierarchyLevel.GRUPO.getValue())));

            newLoanAccount = new LoanAccountData(loanCycleCompletedOptions, loanPurposeOptions, businessEvolutionOptions, yesnoOptions,
                    businessExperienceOptions, businessLocationOptions, clientTypeOptions, loanStatusOptions, institutionTypeOptions,
                    housingTypeOptions, classificationOptions, jobTypeOptions, educationLevelOptions, maritalStatusOptions,
                    groupPositionOptions, sourceOfFundsOptions, cancellationReasonOptions, facilitatorOptions, documentTypeOptions);
            return this.toApiJsonSerializer.serialize(settings, newLoanAccount, this.loanDataParameters);
        } else if ("cheque".equalsIgnoreCase(templateType)) {
            final Collection<CenterData> centerOptions = this.centerReadPlatformService
                    .retrieveAllForDropdown(this.context.authenticatedUser().getOffice().getId());
            final Collection<AgencyData> agencyOptions = this.agencyReadPlatformService.retrieveAllByUser();
            final List<AppUserData> facilitatorOptions = new ArrayList<>(
                    this.appUserReadPlatformService.retrieveUsersUnderHierarchy(Long.valueOf(OfficeHierarchyLevel.GRUPO.getValue())));
            final Collection<GroupGeneralData> groupOptions = this.groupReadPlatformService.retrieveAll(null, null);
            final Collection<EnumOptionData> disbursementMethodOptions = List.of(LoanDisbursementMethod.status(1),
                    LoanDisbursementMethod.status(2), LoanDisbursementMethod.status(3));
            newLoanAccount = LoanAccountData.disburseLoanByCheques(agencyOptions, centerOptions, groupOptions, facilitatorOptions,
                    disbursementMethodOptions);

        } else {
            // for JLG loan both client and group details are required
            if (templateType.equals("individual") || templateType.equals("jlg")) {

                if (clientId == null) {
                    newLoanAccount = newLoanAccount == null ? LoanAccountData.emptyTemplate() : newLoanAccount;
                } else {
                    final LoanAccountData loanAccountClientDetails = this.loanReadPlatformService.retrieveClientDetailsTemplate(clientId);

                    officeId = loanAccountClientDetails.officeId();
                    newLoanAccount = newLoanAccount == null ? loanAccountClientDetails
                            : LoanAccountData.populateClientDefaults(newLoanAccount, loanAccountClientDetails);

                    if (groupId != null) {
                        final GroupGeneralData group = this.groupReadPlatformService.retrieveOne(groupId);
                        newLoanAccount = LoanAccountData.associateGroup(newLoanAccount, group);
                        calendarOptions = this.loanReadPlatformService.retrieveCalendars(groupId);
                    }

                }

                // if it's JLG loan add group details
                if (templateType.equals("jlg")) {
                    final GroupGeneralData group = this.groupReadPlatformService.retrieveOne(groupId);
                    newLoanAccount = LoanAccountData.associateGroup(newLoanAccount, group);
                    calendarOptions = this.loanReadPlatformService.retrieveCalendars(groupId);
                }

            } else if (templateType.equals("group")) {

                final LoanAccountData loanAccountGroupData = this.loanReadPlatformService.retrieveGroupDetailsTemplate(groupId);
                officeId = loanAccountGroupData.groupOfficeId();
                calendarOptions = this.loanReadPlatformService.retrieveCalendars(groupId);
                newLoanAccount = newLoanAccount == null ? loanAccountGroupData
                        : LoanAccountData.populateGroupDefaults(newLoanAccount, loanAccountGroupData);
                accountLinkingOptions = getaccountLinkingOptions(newLoanAccount, clientId, groupId);

            } else if (templateType.equals("jlgbulk")) {
                // get group details along with members in that group
                final LoanAccountData loanAccountGroupData = this.loanReadPlatformService.retrieveGroupAndMembersDetailsTemplate(groupId);
                officeId = loanAccountGroupData.groupOfficeId();
                calendarOptions = this.loanReadPlatformService.retrieveCalendars(groupId);
                newLoanAccount = newLoanAccount == null ? loanAccountGroupData
                        : LoanAccountData.populateGroupDefaults(newLoanAccount, loanAccountGroupData);
                if (productId != null) {
                    Map<Long, Integer> memberLoanCycle = new HashMap<>();
                    Collection<ClientData> members = loanAccountGroupData.groupData().clientMembers();
                    accountLinkingOptions = new ArrayList<>();
                    if (members != null) {
                        for (ClientData clientData : members) {
                            Integer loanCounter = this.loanReadPlatformService.retriveLoanCounter(clientData.id(), productId);
                            memberLoanCycle.put(clientData.id(), loanCounter);
                            accountLinkingOptions.addAll(getaccountLinkingOptions(newLoanAccount, clientData.id(), groupId));
                        }
                    }

                    newLoanAccount = LoanAccountData.associateMemberVariations(newLoanAccount, memberLoanCycle);
                }

            } else {
                final String errorMsg = "Loan template type '" + templateType + "' is not supported";
                throw new NotSupportedLoanTemplateTypeException(errorMsg, templateType);
            }

            allowedLoanOfficers = this.loanReadPlatformService.retrieveAllowedLoanOfficers(officeId, staffInSelectedOfficeOnly);

            if (clientId != null) {
                accountLinkingOptions = getaccountLinkingOptions(newLoanAccount, clientId, groupId);
            }

            // add product options, allowed loan officers and calendar options
            // (calendar options will be null in individual loan)
            newLoanAccount = LoanAccountData.associationsAndTemplate(newLoanAccount, productOptions, allowedLoanOfficers, calendarOptions,
                    accountLinkingOptions, isRatesEnabled);
        }
        final List<DatatableData> datatableTemplates = this.entityDatatableChecksReadService
                .retrieveTemplates(StatusEnum.CREATE.getCode().longValue(), EntityTables.LOAN.getName(), productId);
        newLoanAccount.setDatatables(datatableTemplates);

        if (clientId != null && newLoanAccount.currency() != null) {
            newLoanAccount.setCupoLinkingOptions(
                    this.cupoReadService.findAllActiveCuposByClientId(clientId, CupoStatus.ACTIVE, newLoanAccount.currency().code()));
        } else if (groupId != null && newLoanAccount.currency() != null) {
            newLoanAccount.setCupoLinkingOptions(
                    this.cupoReadService.findAllActiveCuposByGroupId(groupId, CupoStatus.ACTIVE, newLoanAccount.currency().code()));
        }

        return this.toApiJsonSerializer.serialize(settings, newLoanAccount, this.loanDataParameters);
    }

    @GET
    @Path("validateAgeLimits/{clientId}/{productId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Validate Client age limits", description = "This is a resource used for validating the age limits of the client on loan application:\n"
            + "\n" + "Field Defaults\n" + "Allowed description Lists\n" + "Example Requests:\n" + "\n"
            + "loans/validateAgeLimits?clientId=1\n" + "\n" + "\n" + "loans/validateAgeLimits?clientId=1&productId=1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.GetLoansTemplateResponse.class))) })
    public String template(@PathParam("clientId") @Parameter(description = "clientId") final Long clientId,
                           @PathParam("productId") @Parameter(description = "productId") final Long productId, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        // template
        ClientData clientData = this.clientReadPlatformService.retrieveOne(clientId);
        LocalDate dateOfBirth = clientData.getDateOfBirth();
        LocalDate businessLocalDate = DateUtils.getBusinessLocalDate();

        LoanProductData loanProductData = loanProductReadPlatformService.retrieveLoanProduct(productId);

        Integer ageLimitWarning = loanProductData.getAgeLimitWarning();
        Integer ageLimitBlock = loanProductData.getAgeLimitBlock();

        AgeLimitStatus status = AgeLimitStatus.CONTINUE;
        if (dateOfBirth != null && ageLimitWarning != null && ageLimitBlock != null) {
            Integer age = businessLocalDate.getYear() - dateOfBirth.getYear();
            if (age >= ageLimitBlock) {
                status = AgeLimitStatus.BLOCK;

            }
            if (age > ageLimitWarning && age < ageLimitBlock) {
                status = AgeLimitStatus.WARNING;
            }
        }

        EnumOptionData enumSatus = AgeLimitStatusEnumerations.status(status);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.ageLimitValidationJsonSerializer.serialize(settings, enumSatus, this.loanDataParameters);
    }

    private Collection<PortfolioAccountData> getaccountLinkingOptions(final LoanAccountData newLoanAccount, final Long clientId,
                                                                      final Long groupId) {
        final CurrencyData currencyData = newLoanAccount.currency();
        String currencyCode = null;
        if (currencyData != null) {
            currencyCode = currencyData.code();
        }
        final long[] accountStatus = { SavingsAccountStatusType.ACTIVE.getValue() };
        final PortfolioAccountDTO portfolioAccountDTO = new PortfolioAccountDTO(PortfolioAccountType.SAVINGS.getValue(), clientId,
                currencyCode, accountStatus, DepositAccountType.SAVINGS_DEPOSIT.getValue());
        if (groupId != null) {
            portfolioAccountDTO.setGroupId(groupId);
        }
        return this.portfolioAccountReadPlatformService.retrieveAllForLookup(portfolioAccountDTO);
    }

    @GET
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a Loan", description = "Note: template=true parameter doesn't apply to this resource."
            + "Example Requests:\n" + "\n" + "loans/1\n" + "\n" + "\n" + "loans/1?fields=id,principal,annualInterestRate\n" + "\n" + "\n"
            + "loans/1?associations=all\n" + "\n" + "loans/1?associations=all&exclude=guarantors\n" + "\n" + "\n"
            + "loans/1?fields=id,principal,annualInterestRate&associations=repaymentSchedule,transactions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.GetLoansLoanIdResponse.class))) })
    public String retrieveLoan(@PathParam("loanId") @Parameter(description = "loanId") final Long loanId,
                               @DefaultValue("false") @QueryParam("staffInSelectedOfficeOnly") @Parameter(description = "staffInSelectedOfficeOnly") final boolean staffInSelectedOfficeOnly,
                               @DefaultValue("all") @QueryParam("associations") @Parameter(in = ParameterIn.QUERY, name = "associations", description = "Loan object relations to be included in the response", required = false, examples = {
                                       @ExampleObject(value = "all"), @ExampleObject(value = "repaymentSchedule,transactions") }) final String associations,
                               @QueryParam("exclude") @Parameter(in = ParameterIn.QUERY, name = "exclude", description = "Optional Loan object relation list to be filtered in the response", required = false, example = "guarantors,futureSchedule") final String exclude,
                               @QueryParam("fields") @Parameter(in = ParameterIn.QUERY, name = "fields", description = "Optional Loan attribute list to be in the response", required = false, example = "id,principal,annualInterestRate") final String fields,
                               @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        LoanAccountData loanBasicDetails = this.loanReadPlatformService.retrieveOne(loanId);
        final Long prequalificationId = loanBasicDetails.getPrequalificationId();
        final GroupPrequalificationData prequalificationData = loanBasicDetails.getPrequalificationData();
        final LoanAdditionalData loanAdditionalData = loanBasicDetails.getLoanAdditionalData();
        if (loanBasicDetails.isInterestRecalculationEnabled()) {
            Collection<CalendarData> interestRecalculationCalendarDatas = this.calendarReadPlatformService.retrieveCalendarsByEntity(
                    loanBasicDetails.getInterestRecalculationDetailId(), CalendarEntityType.LOAN_RECALCULATION_REST_DETAIL.getValue(),
                    null);
            CalendarData calendarData = null;
            if (!CollectionUtils.isEmpty(interestRecalculationCalendarDatas)) {
                calendarData = interestRecalculationCalendarDatas.iterator().next();
            }

            Collection<CalendarData> interestRecalculationCompoundingCalendarDatas = this.calendarReadPlatformService
                    .retrieveCalendarsByEntity(loanBasicDetails.getInterestRecalculationDetailId(),
                            CalendarEntityType.LOAN_RECALCULATION_COMPOUNDING_DETAIL.getValue(), null);
            CalendarData compoundingCalendarData = null;
            if (!CollectionUtils.isEmpty(interestRecalculationCompoundingCalendarDatas)) {
                compoundingCalendarData = interestRecalculationCompoundingCalendarDatas.iterator().next();
            }
            loanBasicDetails = LoanAccountData.withInterestRecalculationCalendarData(loanBasicDetails, calendarData,
                    compoundingCalendarData);
        }
        if (loanBasicDetails.isMonthlyRepaymentFrequencyType()) {
            Collection<CalendarData> loanCalendarDatas = this.calendarReadPlatformService.retrieveCalendarsByEntity(loanId,
                    CalendarEntityType.LOANS.getValue(), null);
            CalendarData calendarData = null;
            if (!CollectionUtils.isEmpty(loanCalendarDatas)) {
                calendarData = loanCalendarDatas.iterator().next();
            }
            if (calendarData != null) {
                loanBasicDetails = LoanAccountData.withLoanCalendarData(loanBasicDetails, calendarData);
            }
        }
        Collection<InterestRatePeriodData> interestRatesPeriods = this.loanReadPlatformService
                .retrieveLoanInterestRatePeriodData(loanBasicDetails);
        Collection<LoanTransactionData> loanRepayments = null;
        LoanScheduleData repaymentSchedule = null;
        Collection<LoanChargeData> charges = null;
        Collection<GuarantorData> guarantors = null;
        CalendarData meeting = null;
        Collection<NoteData> notes = null;
        PortfolioAccountData linkedAccount = null;
        CupoData linkedCupo = this.accountAssociationsReadPlatformService.retrieveLinkedCupo(loanId);
        Collection<DisbursementData> disbursementData = null;
        Collection<LoanTermVariationsData> emiAmountVariations = null;
        Collection<LoanCollateralResponseData> loanCollateralManagements = null;
        Collection<LoanCollateralManagementData> loanCollateralManagementData = new ArrayList<>();
        CollectionData collectionData = CollectionData.template();

        final Set<String> mandatoryResponseParameters = new HashSet<>();
        final Set<String> associationParameters = ApiParameterHelper.extractAssociationsForResponseIfProvided(uriInfo.getQueryParameters());
        if (!associationParameters.isEmpty()) {
            if (associationParameters.contains(DataTableApiConstant.allAssociateParamName)) {
                associationParameters.addAll(Arrays.asList(DataTableApiConstant.repaymentScheduleAssociateParamName,
                        DataTableApiConstant.futureScheduleAssociateParamName, DataTableApiConstant.originalScheduleAssociateParamName,
                        DataTableApiConstant.transactionsAssociateParamName, DataTableApiConstant.chargesAssociateParamName,
                        DataTableApiConstant.guarantorsAssociateParamName, DataTableApiConstant.collateralAssociateParamName,
                        DataTableApiConstant.notesAssociateParamName, DataTableApiConstant.linkedAccountAssociateParamName,
                        DataTableApiConstant.multiDisburseDetailsAssociateParamName, DataTableApiConstant.collectionAssociateParamName,
                        DataTableApiConstant.additionalDetailsParamName));
            }

            ApiParameterHelper.excludeAssociationsForResponseIfProvided(exclude, associationParameters);

            if (associationParameters.contains(DataTableApiConstant.guarantorsAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.guarantorsAssociateParamName);
                guarantors = this.guarantorReadPlatformService.retrieveGuarantorsForLoan(loanId);
                if (CollectionUtils.isEmpty(guarantors)) {
                    guarantors = null;
                }
            }

            if (associationParameters.contains(DataTableApiConstant.transactionsAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.transactionsAssociateParamName);
                final Collection<LoanTransactionData> currentLoanRepayments = this.loanReadPlatformService.retrieveLoanTransactions(loanId);
                if (!CollectionUtils.isEmpty(currentLoanRepayments)) {
                    loanRepayments = currentLoanRepayments;
                }
            }

            if (associationParameters.contains(DataTableApiConstant.multiDisburseDetailsAssociateParamName)
                    || associationParameters.contains(DataTableApiConstant.repaymentScheduleAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.multiDisburseDetailsAssociateParamName);
                disbursementData = this.loanReadPlatformService.retrieveLoanDisbursementDetails(loanId);
            }

            if (associationParameters.contains(DataTableApiConstant.emiAmountVariationsAssociateParamName)
                    || associationParameters.contains(DataTableApiConstant.repaymentScheduleAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.emiAmountVariationsAssociateParamName);
                emiAmountVariations = this.loanReadPlatformService.retrieveLoanTermVariations(loanId,
                        LoanTermVariationType.EMI_AMOUNT.getValue());
            }

            if (associationParameters.contains(DataTableApiConstant.repaymentScheduleAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.repaymentScheduleAssociateParamName);
                final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedData = loanBasicDetails.repaymentScheduleRelatedData();
                repaymentSchedule = this.loanReadPlatformService.retrieveRepaymentSchedule(loanId, repaymentScheduleRelatedData,
                        disbursementData, loanBasicDetails.isInterestRecalculationEnabled(), loanBasicDetails.getTotalPaidFeeCharges());

                if (associationParameters.contains(DataTableApiConstant.futureScheduleAssociateParamName)
                        && loanBasicDetails.isInterestRecalculationEnabled()) {
                    mandatoryResponseParameters.add(DataTableApiConstant.futureScheduleAssociateParamName);
                    this.calculationPlatformService.updateFutureSchedule(repaymentSchedule, loanId);
                }

                if (associationParameters.contains(DataTableApiConstant.originalScheduleAssociateParamName)
                        && loanBasicDetails.isInterestRecalculationEnabled() && loanBasicDetails.isActive()) {
                    mandatoryResponseParameters.add(DataTableApiConstant.originalScheduleAssociateParamName);
                    LoanScheduleData loanScheduleData = this.loanScheduleHistoryReadPlatformService.retrieveRepaymentArchiveSchedule(loanId,
                            repaymentScheduleRelatedData, disbursementData);
                    loanBasicDetails = LoanAccountData.withOriginalSchedule(loanBasicDetails, loanScheduleData);
                }
            }

            if (associationParameters.contains(DataTableApiConstant.additionalDetailsParamName)) {
                final EnumOptionData prequalificationType = prequalificationData != null ? prequalificationData.getPrequalificationType()
                        : null;
                if (prequalificationType != null && PrequalificationType.GROUP.name().equals(prequalificationType.getValue())) {
                    GroupLoanAdditionalData groupLoanAdditionalData = this.loanReadPlatformService.retrieveAdditionalData(loanId);
                    loanBasicDetails = LoanAccountData.withAdditionalDetails(loanBasicDetails, groupLoanAdditionalData);
                }
            }

            if (associationParameters.contains(DataTableApiConstant.chargesAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.chargesAssociateParamName);
                charges = this.loanChargeReadPlatformService.retrieveLoanCharges(loanId);
                if (CollectionUtils.isEmpty(charges)) {
                    charges = null;
                }
            }

            if (associationParameters.contains(DataTableApiConstant.collateralAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.collateralAssociateParamName);
                loanCollateralManagements = this.loanCollateralManagementReadPlatformService.getLoanCollateralResponseDataList(loanId);
                for (LoanCollateralResponseData loanCollateralManagement : loanCollateralManagements) {
                    loanCollateralManagementData.add(loanCollateralManagement.toCommand());
                }
                if (CollectionUtils.isEmpty(loanCollateralManagements)) {
                    loanCollateralManagements = null;
                }
            }

            if (associationParameters.contains(DataTableApiConstant.meetingAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.meetingAssociateParamName);
                meeting = this.calendarReadPlatformService.retrieveLoanCalendar(loanId);
            }

            if (associationParameters.contains(DataTableApiConstant.notesAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.notesAssociateParamName);
                notes = this.noteReadPlatformService.retrieveNotesByResource(loanId, NoteType.LOAN.getValue());
                if (CollectionUtils.isEmpty(notes)) {
                    notes = null;
                }
            }

            if (associationParameters.contains(DataTableApiConstant.linkedAccountAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.linkedAccountAssociateParamName);
                linkedAccount = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(loanId);
            }

            if (associationParameters.contains(DataTableApiConstant.collectionAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.collectionAssociateParamName);
                if (loanBasicDetails.isActive()) {
                    collectionData = this.loanReadPlatformService.retrieveLoanCollectionData(loanId);
                }
            }
        }

        Collection<LoanProductData> productOptions = null;
        LoanProductData product = null;
        Collection<EnumOptionData> loanTermFrequencyTypeOptions = null;
        Collection<EnumOptionData> repaymentFrequencyTypeOptions = null;
        Collection<EnumOptionData> repaymentFrequencyNthDayTypeOptions = null;
        Collection<EnumOptionData> repaymentFrequencyDayOfWeekTypeOptions = null;
        Collection<TransactionProcessingStrategyData> repaymentStrategyOptions = null;
        Collection<EnumOptionData> interestRateFrequencyTypeOptions = null;
        Collection<EnumOptionData> amortizationTypeOptions = null;
        Collection<EnumOptionData> interestTypeOptions = null;
        Collection<EnumOptionData> interestCalculationPeriodTypeOptions = null;
        Collection<FundData> fundOptions = null;
        Collection<StaffData> allowedLoanOfficers = null;
        Collection<ChargeData> chargeOptions = null;
        ChargeData chargeTemplate = null;
        Collection<CodeValueData> loanPurposeOptions = null;
        Collection<CodeValueData> loanCollateralOptions = null;
        Collection<CalendarData> calendarOptions = null;
        Collection<PortfolioAccountData> accountLinkingOptions = null;
        PaidInAdvanceData paidInAdvanceTemplate = null;
        Collection<LoanAccountSummaryData> clientActiveLoanOptions = null;
        Collection<CupoData> cupoLinkingOptions = null;

        final boolean template = ApiParameterHelper.template(uriInfo.getQueryParameters());
        if (template) {
            productOptions = this.loanProductReadPlatformService.retrieveAllLoanProductsForLookup();
            product = this.loanProductReadPlatformService.retrieveLoanProduct(loanBasicDetails.loanProductId());
            loanBasicDetails.setProduct(product);
            loanTermFrequencyTypeOptions = this.dropdownReadPlatformService.retrieveLoanTermFrequencyTypeOptions();
            repaymentFrequencyTypeOptions = this.dropdownReadPlatformService.retrieveRepaymentFrequencyTypeOptions();
            repaymentFrequencyNthDayTypeOptions = this.dropdownReadPlatformService.retrieveRepaymentFrequencyOptionsForNthDayOfMonth();
            repaymentFrequencyDayOfWeekTypeOptions = this.dropdownReadPlatformService.retrieveRepaymentFrequencyOptionsForDaysOfWeek();
            interestRateFrequencyTypeOptions = this.dropdownReadPlatformService.retrieveInterestRateFrequencyTypeOptions();

            amortizationTypeOptions = this.dropdownReadPlatformService.retrieveLoanAmortizationTypeOptions();
            if (product.isLinkedToFloatingInterestRates()) {
                interestTypeOptions = Arrays.asList(interestType(InterestMethod.DECLINING_BALANCE));
            } else {
                interestTypeOptions = this.dropdownReadPlatformService.retrieveLoanInterestTypeOptions();
            }
            interestCalculationPeriodTypeOptions = this.dropdownReadPlatformService.retrieveLoanInterestRateCalculatedInPeriodOptions();

            fundOptions = this.fundReadPlatformService.retrieveAllFunds();
            repaymentStrategyOptions = this.dropdownReadPlatformService.retreiveTransactionProcessingStrategies();
            if (product.getMultiDisburseLoan()) {
                chargeOptions = this.chargeReadPlatformService.retrieveLoanAccountApplicableCharges(loanId,
                        new ChargeTimeType[] { ChargeTimeType.OVERDUE_INSTALLMENT });
            } else {
                chargeOptions = this.chargeReadPlatformService.retrieveLoanAccountApplicableCharges(loanId,
                        new ChargeTimeType[] { ChargeTimeType.OVERDUE_INSTALLMENT, ChargeTimeType.TRANCHE_DISBURSEMENT });
            }
            chargeTemplate = this.loanChargeReadPlatformService.retrieveLoanChargeTemplate();

            allowedLoanOfficers = this.loanReadPlatformService.retrieveAllowedLoanOfficers(loanBasicDetails.officeId(),
                    staffInSelectedOfficeOnly);

            loanPurposeOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("LoanPurpose");
            loanCollateralOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("LoanCollateral");
            final CurrencyData currencyData = loanBasicDetails.currency();
            String currencyCode = null;
            if (currencyData != null) {
                currencyCode = currencyData.code();
            }
            final long[] accountStatus = { SavingsAccountStatusType.ACTIVE.getValue() };
            PortfolioAccountDTO portfolioAccountDTO = new PortfolioAccountDTO(PortfolioAccountType.SAVINGS.getValue(),
                    loanBasicDetails.clientId(), currencyCode, accountStatus, DepositAccountType.SAVINGS_DEPOSIT.getValue());
            accountLinkingOptions = this.portfolioAccountReadPlatformService.retrieveAllForLookup(portfolioAccountDTO);

            if (!associationParameters.contains(DataTableApiConstant.linkedAccountAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.linkedAccountAssociateParamName);
                linkedAccount = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(loanId);
            }
            if (loanBasicDetails.groupId() != null) {
                calendarOptions = this.loanReadPlatformService.retrieveCalendars(loanBasicDetails.groupId());
            }

            if (loanBasicDetails.product().canUseForTopup() && loanBasicDetails.clientId() != null) {
                clientActiveLoanOptions = this.accountDetailsReadPlatformService
                        .retrieveClientActiveLoanAccountSummary(loanBasicDetails.clientId());
            }

            if (loanBasicDetails.clientId() != null) {
                cupoLinkingOptions = this.cupoReadService.findAllActiveCuposByClientId(loanBasicDetails.clientId(), CupoStatus.ACTIVE,
                        loanBasicDetails.currency().code());
            } else if (loanBasicDetails.groupId() != null) {
                cupoLinkingOptions = this.cupoReadService.findAllActiveCuposByGroupId(loanBasicDetails.groupId(), CupoStatus.ACTIVE,
                        loanBasicDetails.currency().code());
            }
        }

        Collection<ChargeData> overdueCharges = this.chargeReadPlatformService.retrieveLoanProductCharges(loanBasicDetails.loanProductId(),
                ChargeTimeType.OVERDUE_INSTALLMENT);

        paidInAdvanceTemplate = this.loanReadPlatformService.retrieveTotalPaidInAdvance(loanId);

        // Get rates from Loan
        boolean isRatesEnabled = this.configurationDomainService.isSubRatesEnabled();
        List<RateData> rates = null;
        if (isRatesEnabled) {
            rates = this.rateReadService.retrieveLoanRates(loanId);
        }

        final LoanAccountData loanAccount = LoanAccountData.associationsAndTemplate(loanBasicDetails, repaymentSchedule, loanRepayments,
                charges, loanCollateralManagementData, guarantors, meeting, productOptions, loanTermFrequencyTypeOptions,
                repaymentFrequencyTypeOptions, repaymentFrequencyNthDayTypeOptions, repaymentFrequencyDayOfWeekTypeOptions,
                repaymentStrategyOptions, interestRateFrequencyTypeOptions, amortizationTypeOptions, interestTypeOptions,
                interestCalculationPeriodTypeOptions, fundOptions, chargeOptions, chargeTemplate, allowedLoanOfficers, loanPurposeOptions,
                loanCollateralOptions, calendarOptions, notes, accountLinkingOptions, linkedAccount, disbursementData, emiAmountVariations,
                overdueCharges, paidInAdvanceTemplate, interestRatesPeriods, clientActiveLoanOptions, rates, isRatesEnabled,
                collectionData);
        loanAccount.setLinkedCupo(linkedCupo);
        loanAccount.setCupoLinkingOptions(cupoLinkingOptions);
        loanAccount.setPrequalificationId(prequalificationId);
        loanAccount.setPrequalificationData(prequalificationData);
        loanAccount.setLoanAdditionalData(loanAdditionalData);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters(),
                mandatoryResponseParameters);
        return this.toApiJsonSerializer.serialize(settings, loanAccount, this.loanDataParameters);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List Loans", description = "The list capability of loans can support pagination and sorting.\n"
            + "Example Requests:\n" + "\n" + "loans\n" + "\n" + "loans?fields=accountNo\n" + "\n" + "loans?offset=10&limit=50\n" + "\n"
            + "loans?orderBy=accountNo&sortOrder=DESC")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.GetLoansResponse.class))) })
    public String retrieveAll(@Context final UriInfo uriInfo,
                              @QueryParam("sqlSearch") @Parameter(description = "sqlSearch") final String sqlSearch,
                              @QueryParam("externalId") @Parameter(description = "externalId") final String externalId,
                              // @QueryParam("underHierarchy") final String hierarchy,
                              @QueryParam("offset") @Parameter(description = "offset") final Integer offset,
                              @QueryParam("limit") @Parameter(description = "limit") final Integer limit,
                              @QueryParam("orderBy") @Parameter(description = "orderBy") final String orderBy,
                              @QueryParam("sortOrder") @Parameter(description = "sortOrder") final String sortOrder,
                              @QueryParam("accountNo") @Parameter(description = "accountNo") final String accountNo,
                              @QueryParam("clientNo") @Parameter(description = "accountNo") final String clientNo,
                              @QueryParam("agencyId") @Parameter(description = "agencyId") final Long agencyId,
                              @QueryParam("groupId") @Parameter(description = "groupId") final Long groupId,
                              @QueryParam("centerId") @Parameter(description = "centerId") final Long centerId,
                              @QueryParam("facilitatorId") @Parameter(description = "facilitatorId") final Long facilitatorId,
                              @DefaultValue("false") @QueryParam("isIndividualBusinessLoan") @Parameter(description = "isIndividualBusinessLoan") final boolean isIndividualBusinessLoan,
                              @QueryParam("approvalEndDate") @Parameter(description = "approvalEndDate") final DateParam approvalEndDateParam,
                              @QueryParam("approvalStartDate") @Parameter(description = "approvalStartDate") final DateParam approvalStartDateParam,
                              @QueryParam("disbursementStartDate") @Parameter(description = "disbursementStartDate") final DateParam disbursementStartDateParam,
                              @QueryParam("disbursementEndDate") @Parameter(description = "disbursementEndDate") final DateParam disbursementEndDateParam,
                              @QueryParam("locale") @Parameter(description = "locale") final String locale,
                              @QueryParam("dateFormat") @Parameter(description = "dateFormat") final String dateFormat) {
        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);
        LocalDate approvalStartDate = null;
        if (approvalStartDateParam != null) {
            approvalStartDate = approvalStartDateParam.getDate("approvalStartDate", dateFormat, locale);
        }
        LocalDate approvalEndDate = null;
        if (approvalEndDateParam != null) {
            approvalEndDate = approvalEndDateParam.getDate("approvalEndDate", dateFormat, locale);
        }
        LocalDate disbursementStartDate = null;
        if (disbursementStartDateParam != null) {
            disbursementStartDate = disbursementStartDateParam.getDate("disbursementStartDate", dateFormat, locale);
        }
        LocalDate disbursementEndDate = null;
        if (disbursementEndDateParam != null) {
            disbursementEndDate = disbursementEndDateParam.getDate("disbursementStartDate", dateFormat, locale);
        }

        final SearchParameters searchParameters = SearchParameters.forLoans(sqlSearch, externalId, offset, limit, orderBy, sortOrder,
                accountNo, disbursementStartDate, disbursementEndDate, approvalStartDate, approvalEndDate, clientNo, agencyId, groupId,
                centerId, facilitatorId, isIndividualBusinessLoan);

        final Page<LoanAccountData> loanBasicDetails = this.loanReadPlatformService.retrieveAll(searchParameters);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, loanBasicDetails, this.loanDataParameters);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Calculate loan repayment schedule | Submit a new Loan Application", description = "It calculates the loan repayment Schedule\n"
            + "Submits a new loan application\n"
            + "Mandatory Fields: clientId, productId, principal, loanTermFrequency, loanTermFrequencyType, loanType, numberOfRepayments, repaymentEvery, repaymentFrequencyType, interestRatePerPeriod, amortizationType, interestType, interestCalculationPeriodType, transactionProcessingStrategyId, expectedDisbursementDate, submittedOnDate, loanType\n"
            + "Optional Fields: graceOnPrincipalPayment, graceOnInterestPayment, graceOnInterestCharged, linkAccountId, allowPartialPeriodInterestCalcualtion, fixedEmiAmount, maxOutstandingLoanBalance, disbursementData, graceOnArrearsAgeing, createStandingInstructionAtDisbursement (requires linkedAccountId if set to true)\n"
            + "Additional Mandatory Fields if interest recalculation is enabled for product and Rest frequency not same as repayment period: recalculationRestFrequencyDate\n"
            + "Additional Mandatory Fields if interest recalculation with interest/fee compounding is enabled for product and compounding frequency not same as repayment period: recalculationCompoundingFrequencyDate\n"
            + "Additional Mandatory Field if Entity-Datatable Check is enabled for the entity of type loan: datatables")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansResponse.class))) })
    public String calculateLoanScheduleOrSubmitLoanApplication(
            @QueryParam("command") @Parameter(description = "command") final String commandParam, @Context final UriInfo uriInfo,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);
        final CommandWrapper commandRequest;
        final CommandProcessingResult result;
        if (is(commandParam, "calculateLoanSchedule")) {
            final JsonElement parsedQuery = this.fromJsonHelper.parse(apiRequestBodyAsJson);
            final JsonQuery query = JsonQuery.from(apiRequestBodyAsJson, parsedQuery, this.fromJsonHelper);
            final LoanScheduleModel loanSchedule = this.calculationPlatformService.calculateLoanSchedule(query, true);
            final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
            return this.loanScheduleToApiJsonSerializer.serialize(settings, loanSchedule.toData(), new HashSet<String>());
        } else if (is(commandParam, "disbursebycheques")) {
            commandRequest = builder.disburseLoanByCheques().build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else {
            commandRequest = builder.createLoanApplication().withJson(apiRequestBodyAsJson).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }
        return this.toApiJsonSerializer.serialize(result);

    }

    @PUT
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Modify a loan application", description = "Loan application can only be modified when in 'Submitted and pending approval' state. Once the application is approved, the details cannot be changed using this method.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PutLoansLoanIdRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PutLoansLoanIdResponse.class))) })
    public String modifyLoanApplication(@PathParam("loanId") @Parameter(description = "loanId") final Long loanId,
                                        @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateLoanApplication(loanId).withJson(apiRequestBodyAsJson)
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a Loan Application", description = "Note: Only loans in \"Submitted and awaiting approval\" status can be deleted.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.DeleteLoansLoanIdResponse.class))) })
    public String deleteLoanApplication(@PathParam("loanId") @Parameter(description = "loanId") final Long loanId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteLoanApplication(loanId).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Approve Loan Application | Recover Loan Guarantee | Undo Loan Application Approval | Assign a Loan Officer | Unassign a Loan Officer | Reject Loan Application | Applicant Withdraws from Loan Application | Disburse Loan Disburse Loan To Savings Account | Undo Loan Disbursal", description = "Approve Loan Application:\n"
            + "Mandatory Fields: approvedOnDate\n" + "Optional Fields: approvedLoanAmount and expectedDisbursementDate\n"
            + "Approves the loan application\n\n" + "Recover Loan Guarantee:\n" + "Recovers the loan guarantee\n\n"
            + "Undo Loan Application Approval:\n" + "Undoes the Loan Application Approval\n\n" + "Assign a Loan Officer:\n"
            + "Allows you to assign Loan Officer for existing Loan.\n\n" + "Unassign a Loan Officer:\n"
            + "Allows you to unassign the Loan Officer.\n\n" + "Reject Loan Application:\n" + "Mandatory Fields: rejectedOnDate\n"
            + "Allows you to reject the loan application\n\n" + "Applicant Withdraws from Loan Application:\n"
            + "Mandatory Fields: withdrawnOnDate\n" + "Allows the applicant to withdraw the loan application\n\n" + "Disburse Loan:\n"
            + "Mandatory Fields: actualDisbursementDate\n" + "Optional Fields: transactionAmount and fixedEmiAmount\n"
            + "Disburses the Loan\n\n" + "Disburse Loan To Savings Account:\n" + "Mandatory Fields: actualDisbursementDate\n"
            + "Optional Fields: transactionAmount and fixedEmiAmount\n" + "Disburses the loan to Saving Account\n\n"
            + "Undo Loan Disbursal:\n" + "Undoes the Loan Disbursal\n" + "Showing request and response for Assign a Loan Officer")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansLoanIdRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansLoanIdResponse.class))) })
    public String stateTransitions(@PathParam("loanId") @Parameter(description = "loanId") final Long loanId,
                                   @QueryParam("command") @Parameter(description = "command") final String commandParam,
                                   @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);

        CommandProcessingResult result = null;

        if (is(commandParam, "reject")) {
            final CommandWrapper commandRequest = builder.rejectLoanApplication(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "withdrawnByApplicant")) {
            final CommandWrapper commandRequest = builder.withdrawLoanApplication(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "approve")) {
            final CommandWrapper commandRequest = builder.approveLoanApplication(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "disburse")) {
            final CommandWrapper commandRequest = builder.disburseLoanApplication(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "disburseToSavings")) {
            final CommandWrapper commandRequest = builder.disburseLoanToSavingsApplication(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (is(commandParam, "undoapproval")) {
            final CommandWrapper commandRequest = builder.undoLoanApplicationApproval(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "undodisbursal")) {
            final CommandWrapper commandRequest = builder.undoLoanApplicationDisbursal(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "undolastdisbursal")) {
            final CommandWrapper commandRequest = builder.undoLastDisbursalLoanApplication(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (is(commandParam, "assignloanofficer")) {
            final CommandWrapper commandRequest = builder.assignLoanOfficer(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "unassignloanofficer")) {
            final CommandWrapper commandRequest = builder.unassignLoanOfficer(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "recoverGuarantees")) {
            final CommandWrapper commandRequest = new CommandWrapperBuilder().recoverFromGuarantor(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "editloanfund")) {
            final CommandWrapper commandRequest = builder.editLoanFund(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (result == null) {
            throw new UnrecognizedQueryParamException("command", commandParam);
        }

        return this.toApiJsonSerializer.serialize(result);
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }

    @GET
    @Path("downloadtemplate")
    @Produces("application/vnd.ms-excel")
    public Response getLoansTemplate(@QueryParam("officeId") final Long officeId, @QueryParam("staffId") final Long staffId,
                                     @QueryParam("dateFormat") final String dateFormat) {
        return bulkImportWorkbookPopulatorService.getTemplate(GlobalEntityType.LOANS.toString(), officeId, staffId, dateFormat);
    }

    @GET
    @Path("glimAccount/{glimId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getGlimRepaymentTemplate(@PathParam("glimId") final Long glimId, @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);
        Collection<GlimRepaymentTemplate> glimRepaymentTemplate = this.glimAccountInfoReadPlatformService.findglimRepaymentTemplate(glimId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.glimTemplateToApiJsonSerializer.serialize(settings, glimRepaymentTemplate, this.glimAccountsDataParameters);
    }

    @POST
    @Path("glimAccount/{glimId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Approve GLIM Application | Undo GLIM Application Approval | Reject GLIM Application | Disburse Loan Disburse Loan To Savings Account | Undo Loan Disbursal", description = "Approve GLIM Application:\n"
            + "Mandatory Fields: approvedOnDate\n" + "Optional Fields: approvedLoanAmount and expectedDisbursementDate\n"
            + "Approves the GLIM application\n\n" + "Undo GLIM Application Approval:\n" + "Undoes the GLIM Application Approval\n\n"
            + "Reject GLIM Application:\n" + "Mandatory Fields: rejectedOnDate\n" + "Allows you to reject the GLIM application\n\n"
            + "Disburse Loan:\n" + "Mandatory Fields: actualDisbursementDate\n" + "Optional Fields: transactionAmount and fixedEmiAmount\n"
            + "Disburses the Loan\n\n" + "Disburse Loan To Savings Account:\n" + "Mandatory Fields: actualDisbursementDate\n"
            + "Optional Fields: transactionAmount and fixedEmiAmount\n" + "Disburses the loan to Saving Account\n\n"
            + "Undo Loan Disbursal:\n" + "Undoes the Loan Disbursal\n")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansLoanIdRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansLoanIdResponse.class))) })
    public String glimStateTransitions(@PathParam("glimId") final Long glimId, @QueryParam("command") final String commandParam,
                                       final String apiRequestBodyAsJson) {

        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);

        CommandProcessingResult result = null;

        if (is(commandParam, "reject")) {
            final CommandWrapper commandRequest = builder.rejectGLIMApplication(glimId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "approve")) {
            final CommandWrapper commandRequest = builder.approveGLIMLoanApplication(glimId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "disburse")) {
            final CommandWrapper commandRequest = builder.disburseGlimLoanApplication(glimId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "glimrepayment")) {
            final CommandWrapper commandRequest = builder.repaymentGlimLoanApplication(glimId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "undodisbursal")) {
            final CommandWrapper commandRequest = builder.undoGLIMLoanDisbursal(glimId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "undoapproval")) {
            final CommandWrapper commandRequest = builder.undoGLIMLoanApproval(glimId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (result == null) {
            throw new UnrecognizedQueryParamException("command", commandParam);
        }

        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("repayments/downloadtemplate")
    @Produces("application/vnd.ms-excel")
    public Response getLoanRepaymentTemplate(@QueryParam("officeId") final Long officeId,
                                             @QueryParam("dateFormat") final String dateFormat) {
        return bulkImportWorkbookPopulatorService.getTemplate(GlobalEntityType.LOAN_TRANSACTIONS.toString(), officeId, null, dateFormat);
    }

    @POST
    @Path("uploadtemplate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestBody(description = "Upload Loan template", content = {
            @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = UploadRequest.class)) })
    public String postLoanTemplate(@FormDataParam("file") InputStream uploadedInputStream,
                                   @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("locale") final String locale,
                                   @FormDataParam("dateFormat") final String dateFormat) {
        final Long importDocumentId = this.bulkImportWorkbookService.importWorkbook(GlobalEntityType.LOANS.toString(), uploadedInputStream,
                fileDetail, locale, dateFormat);
        return this.toApiJsonSerializer.serialize(importDocumentId);
    }

    @POST
    @Path("repayments/uploadtemplate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestBody(description = "Upload Loan repayments template", content = {
            @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = UploadRequest.class)) })
    public String postLoanRepaymentTemplate(@FormDataParam("file") InputStream uploadedInputStream,
                                            @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("locale") final String locale,
                                            @FormDataParam("dateFormat") final String dateFormat) {
        final Long importDocumentId = this.bulkImportWorkbookService.importWorkbook(GlobalEntityType.LOAN_TRANSACTIONS.toString(),
                uploadedInputStream, fileDetail, locale, dateFormat);
        return this.toApiJsonSerializer.serialize(importDocumentId);
    }
}
