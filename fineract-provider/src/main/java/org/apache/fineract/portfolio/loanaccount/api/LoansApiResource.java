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
import com.lowagie.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.data.GlobalEntityType;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookPopulatorService;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookService;
import org.apache.fineract.infrastructure.clientblockingreasons.data.BlockingReasonsData;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockLevel;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingEnum;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainServiceJpa;
import org.apache.fineract.infrastructure.core.api.ApiParameterHelper;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.api.JsonQuery;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.data.UploadRequest;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.CommandParameterUtil;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant;
import org.apache.fineract.infrastructure.dataqueries.data.DatatableData;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.GenericResultsetData;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksReadService;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
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
import org.apache.fineract.portfolio.charge.domain.ChargeCalculationType;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.collateralmanagement.data.LoanCollateralResponseData;
import org.apache.fineract.portfolio.collateralmanagement.service.LoanCollateralManagementReadPlatformService;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.delinquency.api.DelinquencyApiResourceSwagger;
import org.apache.fineract.portfolio.delinquency.data.LoanDelinquencyTagHistoryData;
import org.apache.fineract.portfolio.delinquency.domain.LoanDelinquencyAction;
import org.apache.fineract.portfolio.delinquency.service.DelinquencyReadPlatformService;
import org.apache.fineract.portfolio.delinquency.validator.LoanDelinquencyActionData;
import org.apache.fineract.portfolio.floatingrates.data.InterestRatePeriodData;
import org.apache.fineract.portfolio.fund.data.FundData;
import org.apache.fineract.portfolio.fund.service.FundReadPlatformService;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.group.service.GroupReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.CollectionData;
import org.apache.fineract.portfolio.loanaccount.data.DisbursementData;
import org.apache.fineract.portfolio.loanaccount.data.GlimRepaymentTemplate;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.LoanApprovalData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAssignorData;
import org.apache.fineract.portfolio.loanaccount.data.LoanBlockingReasonData;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.data.LoanCollateralManagementData;
import org.apache.fineract.portfolio.loanaccount.data.LoanDebtProjectionData;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentChargeData;
import org.apache.fineract.portfolio.loanaccount.data.LoanReclaimData;
import org.apache.fineract.portfolio.loanaccount.data.LoanSummaryData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.data.PaidInAdvanceData;
import org.apache.fineract.portfolio.loanaccount.data.ReclaimData;
import org.apache.fineract.portfolio.loanaccount.data.RepaymentScheduleRelatedLoanData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTermVariationType;
import org.apache.fineract.portfolio.loanaccount.exception.LoanNotFoundException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanTemplateTypeRequiredException;
import org.apache.fineract.portfolio.loanaccount.exception.NotSupportedLoanTemplateTypeException;
import org.apache.fineract.portfolio.loanaccount.guarantor.data.GuarantorData;
import org.apache.fineract.portfolio.loanaccount.guarantor.service.GuarantorReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.FeeDetails;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleProcessingType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleCalculationPlatformService;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.GLIMAccountInfoReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanBlockReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanChargeReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanDebtProjectionService;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformService;
import org.apache.fineract.portfolio.loanproduct.LoanProductConstants;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.loanproduct.data.MaximumCreditRateConfigurationData;
import org.apache.fineract.portfolio.loanproduct.data.TransactionProcessingStrategyData;
import org.apache.fineract.portfolio.loanproduct.domain.InterestMethod;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductType;
import org.apache.fineract.portfolio.loanproduct.service.LoanDropdownReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.fineract.portfolio.note.data.NoteData;
import org.apache.fineract.portfolio.note.domain.NoteType;
import org.apache.fineract.portfolio.note.service.NoteReadPlatformService;
import org.apache.fineract.portfolio.rate.data.RateData;
import org.apache.fineract.portfolio.rate.service.RateReadService;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Path("/v1/loans")
@Component
@Controller
@Tag(name = "Loans", description = """
         The API concept of loans models the loan application process and the loan contract/monitoring process.
         Field Descriptions
         accountNo
         The account no. associated with this loan. Is auto generated if not provided at loan application creation time.
         externalId
         A place to put an external reference for this loan e.g. The ID another system uses
         If provided, it must be unique.
         fundId
         Optional: For associating a loan with a given fund.
         loanOfficerId
         Optional: For associating a loan with a given staff member who is a loan officer.
         loanPurposeId
         Optional: For marking a loan with a given loan purpose option. Loan purposes are configurable and can be setup by system admin through code/code values screens.
         principal
         The loan amount to be disbursed to through loan.
         loanTermFrequency
         The length of loan term
         Used like: loanTermFrequency loanTermFrequencyType
         e.g. 12 Months
         loanTermFrequencyType
         The loan term period to use. Used like: loanTermFrequency loanTermFrequencyType
         e.g. 12 Months Example Values: 0=Days, 1=Weeks, 2=Months, 3=Years
         numberOfRepayments
         Number of installments to repay
         Used like: numberOfRepayments Every repaymentEvery repaymentFrequencyType
         e.g. 10 (repayments) Every 12 Weeks
         repaymentEvery
         Used like: numberOfRepayments Every repaymentEvery repaymentFrequencyType
         e.g. 10 (repayments) Every 12 Weeks
         repaymentFrequencyType
         Used like: numberOfRepayments Every repaymentEvery repaymentFrequencyType
         e.g. 10 (repayments) Every 12 Weeks
         Example Values: 0=Days, 1=Weeks, 2=Months
         interestRatePerPeriod
         Interest Rate.
         Used like: interestRatePerPeriod % interestRateFrequencyType - interestType
         e.g. 12.0000% Per year - Declining Balance
         interestRateFrequencyType
         Used like: interestRatePerPeriod% interestRateFrequencyType - interestType
         e.g. 12.0000% Per year - Declining Balance
         Example Values: 2=Per month, 3=Per year
         graceOnPrincipalPayment
         Optional: Integer - represents the number of repayment periods that grace should apply to the principal component of a repayment period.
         graceOnInterestPayment
         Optional: Integer - represents the number of repayment periods that grace should apply to the interest component of a repayment period. Interest is still calculated but offset to later repayment periods.
         graceOnInterestCharged
         Optional: Integer - represents the number of repayment periods that should be interest-free.
         graceOnArrearsAgeing
         Optional: Integer - Used in Arrears calculation to only take into account loans that are more than graceOnArrearsAgeing days overdue.
         interestChargedFromDate Optional: Date - The date from with interest is to start being charged
         expectedDisbursementDate
         The proposed disbursement date of the loan so a proposed repayment schedule can be provided
        submittedOnDate
         The date the loan application was submitted by applicant.
         linkAccountId
         The Savings Account id for linking with loan account for payments.
         amortizationType
         Example Values: 0=Equal principle payments, 1=Equal installments
         interestType
         Used like: interestRatePerPeriod% interestRateFrequencyType - interestType
         e.g. 12.0000% Per year - Declining Balance
         Example Values: 0=Declining Balance, 1=Flat interestCalculationPeriodType
         Example Values: 0=Daily, 1=Same as repayment period
         allowPartialPeriodInterestCalcualtion
         This value will be supported along with interestCalculationPeriodType as Same as repayment period to calculate interest for partial periods. Example: Interest charged from is 5th of April , Principal is 10000 and interest is 1% per month then the interest will be (10000 * 1%)* (25/30) , it calculates for the month first then calculates exact periods between start date and end date(can be a decimal)
         inArrearsTolerance
         The amount that can be 'waived' at end of all loan payments because it is too small to worry about.
         This is also the tolerance amount assessed when determining if a loan is in arrears.
         transactionProcessingStrategyCode
         An enumeration that indicates the type of transaction processing strategy to be used. This relates to functionality that is also known as Payment Application Logic.
         A number of out of the box approaches exist, some are custom to specific MFIs, some are more general and indicate the order in which payments are processed.

         Refer to the Payment Application Logic / Transaction Processing Strategy section in the appendix for more detailed overview of each available payment application logic provided out of the box.
         List of current approaches:
         1 = Mifos style (Similar to Old Mifos)
         2 = Heavensfamily (Custom MFI approach)
         3 = Creocore (Custom MFI approach)
         4 = RBI (India)
         5 = Principal Interest Penalties Fees Order
         Interest Principal Penalties Fees Order
         7 = Early Payment Strategy
         loanType
         To represent different type of loans.
         At present there are three type of loans are supported.
         Available loan types:
         individual: Loan given to individual member
         group: Loan given to group as a whole
         jlg: Joint liability group loan given to members in a group on individual basis. JLG loan can be given to one or more members in a group.
         recalculationRestFrequencyDate
         Specifies rest frequency start date for interest recalculation. This date must be before or equal to disbursement date
         recalculationCompoundingFrequencyDate
         Specifies compounding frequency start date for interest recalculation. This date must be equal to disbursement date""")
@RequiredArgsConstructor
public class LoansApiResource {

    private static final Set<String> LOAN_DATA_PARAMETERS = new HashSet<>(Arrays.asList("id", "accountNo", "status", "externalId",
            "clientId", "group", "loanProductId", "loanProductName", "loanProductDescription", "isLoanProductLinkedToFloatingRate",
            "fundId", "fundName", "loanPurposeId", "loanPurposeName", "loanOfficerId", "loanOfficerName", "currency", "principal",
            "totalOverpaid", "inArrearsTolerance", "termFrequency", "termPeriodFrequencyType", "numberOfRepayments", "repaymentEvery",
            "interestRatePerPeriod", "annualInterestRate", "repaymentFrequencyType", "transactionProcessingStrategyCode",
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
            "isFloatingInterestRate", "interestRatesPeriods", "lastClosedBusinessDate", LoanApiConstants.canUseForTopup,
            LoanApiConstants.isTopup, LoanApiConstants.loanIdToClose, LoanApiConstants.topupAmount,
            LoanApiConstants.clientActiveLoanOptions, LoanApiConstants.datatables, LoanProductConstants.RATES_PARAM_NAME,
            LoanApiConstants.MULTIDISBURSE_DETAILS_PARAMNAME, LoanApiConstants.EMI_AMOUNT_VARIATIONS_PARAMNAME,
            LoanApiConstants.COLLECTION_PARAMNAME, LoanApiConstants.CHANNEL_NAME, LoanApiConstants.CHANNEL_ID,
            LoanApiConstants.CHANNEL_DESCRIPTION, LoanApiConstants.POINT_OF_SALES_NAME, LoanApiConstants.DISCOUNT_TRANSFER_VALUE,
            LoanApiConstants.DISCOUNT_VALUE, "cedulaSeguroVoluntario", LoanApiConstants.DISCOUNT_VALUE, LoanApiConstants.AllYID,
            LoanApiConstants.POINT_OF_SALE_CODE));

    private static final Set<String> LOAN_APPROVAL_DATA_PARAMETERS = new HashSet<>(Arrays.asList("approvalDate", "approvalAmount"));
    private static final Set<String> GLIM_ACCOUNTS_DATA_PARAMETERS = new HashSet<>(Arrays.asList("glimId", "groupId", "clientId",
            "parentLoanAccountNo", "parentPrincipalAmount", "childLoanAccountNo", "childPrincipalAmount", "clientName"));
    private static final String RESOURCE_NAME_FOR_PERMISSIONS = "LOAN";
    private static final String RESOURCE_NAME_FOR_DELINQUENCY_ACTION_PERMISSIONS = "DELINQUENCY_ACTION";

    private final PlatformSecurityContext context;
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
    private final DefaultToApiJsonSerializer<LoanScheduleData> loanScheduleToApiJsonSerializer;
    private final DefaultToApiJsonSerializer<LoanDelinquencyActionData> delinquencyActionSerializer;
    private final DefaultToApiJsonSerializer<ReclaimData> reclainApiJsonSerializer;
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
    private final DefaultToApiJsonSerializer<LoanDelinquencyTagHistoryData> jsonSerializerTagHistory;
    private final DelinquencyReadPlatformService delinquencyReadPlatformService;
    private final LoanBlockReadPlatformService loanBlockingReasonReadPlatformService;
    private final LoanDebtProjectionService loanDebtProjectionService;
    private final ConfigurationDomainServiceJpa configurationDomainServiceJpa;
    private final ReadWriteNonCoreDataService readWriteNonCoreDataService;
    private final LoanWritePlatformService loanWritePlatformService;
    private static final String DISBURSE_ACTION = "disburse";
    private final LoanRepository loanRepository;

    @GET
    @Path("{loanId}/template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.GetLoansApprovalTemplateResponse.class)))
    public String retrieveApprovalTemplate(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId,
            @QueryParam("templateType") @Parameter(description = "templateType") final String templateType,
            @Context final UriInfo uriInfo) {
        return retrieveApprovalTemplate(loanId, null, templateType, uriInfo);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Loan Details Template", description = """
             This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:
             Field Defaults
             Allowed description Lists
             Example Requests:
            loans/template?templateType=individual&clientId=1
            loans/template?templateType=individual&clientId=1&productId=1""")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.GetLoansTemplateResponse.class)))
    @SuppressWarnings({ "squid:S3776" })
    public String template(@QueryParam("clientId") @Parameter(description = "clientId") final Long clientId,
            @QueryParam("groupId") @Parameter(description = "groupId") final Long groupId,
            @QueryParam("productId") @Parameter(description = "productId") final Long productId,
            @QueryParam("templateType") @Parameter(description = "templateType") final String templateType,
            @DefaultValue("false") @QueryParam("staffInSelectedOfficeOnly") @Parameter(description = "staffInSelectedOfficeOnly") final boolean staffInSelectedOfficeOnly,
            @DefaultValue("false") @QueryParam("activeOnly") @Parameter(description = "activeOnly") final boolean onlyActive,
            @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

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
            newLoanAccount = this.loanReadPlatformService.retrieveLoanProductDetailsTemplate(productId, clientId, groupId);
        }

        if (templateType == null) {
            final String errorMsg = "Loan template type must be provided";
            throw new LoanTemplateTypeRequiredException(errorMsg);
        } else if (templateType.equals("collateral")) {
            loanCollateralOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("LoanCollateral");
            newLoanAccount = LoanAccountData.collateralTemplate(loanCollateralOptions);
        } else {
            // for JLG loan both client and group details are required
            if (templateType.equals("individual") || templateType.equals("jlg")) {

                if (clientId == null) {
                    newLoanAccount = newLoanAccount == null ? LoanAccountData.collateralTemplate(null) : newLoanAccount;
                } else {
                    final LoanAccountData loanAccountClientDetails = this.loanReadPlatformService.retrieveClientDetailsTemplate(clientId);

                    officeId = loanAccountClientDetails.getClientOfficeId();

                    if (officeId == null && loanAccountClientDetails.getGroup() != null) {
                        officeId = loanAccountClientDetails.getGroup().getOfficeId();
                    }
                    newLoanAccount = newLoanAccount == null ? loanAccountClientDetails
                            : LoanAccountData.populateClientDefaults(newLoanAccount, loanAccountClientDetails);
                }

                // if it's JLG loan add group details
                if (templateType.equals("jlg")) {
                    final GroupGeneralData group = this.groupReadPlatformService.retrieveOne(groupId);
                    newLoanAccount = LoanAccountData.associateGroup(newLoanAccount, group);
                    calendarOptions = this.loanReadPlatformService.retrieveCalendars(groupId);
                }

            } else if (templateType.equals("group")) {

                final LoanAccountData loanAccountGroupData = this.loanReadPlatformService.retrieveGroupDetailsTemplate(groupId);
                officeId = loanAccountGroupData.getGroup() != null ? loanAccountGroupData.getGroup().getOfficeId() : null;
                calendarOptions = this.loanReadPlatformService.retrieveCalendars(groupId);
                newLoanAccount = newLoanAccount == null ? loanAccountGroupData
                        : LoanAccountData.populateGroupDefaults(newLoanAccount, loanAccountGroupData);
                accountLinkingOptions = getAccountLinkingOptions(newLoanAccount, clientId, groupId);

            } else if (templateType.equals("jlgbulk")) {
                // get group details along with members in that group
                final LoanAccountData loanAccountGroupData = this.loanReadPlatformService.retrieveGroupAndMembersDetailsTemplate(groupId);
                officeId = loanAccountGroupData.getGroup() != null ? loanAccountGroupData.getGroup().getOfficeId() : null;
                calendarOptions = this.loanReadPlatformService.retrieveCalendars(groupId);
                newLoanAccount = newLoanAccount == null ? loanAccountGroupData
                        : LoanAccountData.populateGroupDefaults(newLoanAccount, loanAccountGroupData);
                if (productId != null) {
                    Map<Long, Integer> memberLoanCycle = new HashMap<>();
                    Collection<ClientData> members = loanAccountGroupData.getGroup().clientMembers();
                    accountLinkingOptions = new ArrayList<>();
                    if (members != null) {
                        for (ClientData clientData : members) {
                            Integer loanCounter = this.loanReadPlatformService.retriveLoanCounter(clientData.getId(), productId);
                            memberLoanCycle.put(clientData.getId(), loanCounter);
                            accountLinkingOptions.addAll(getAccountLinkingOptions(newLoanAccount, clientData.getId(), groupId));
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
                accountLinkingOptions = getAccountLinkingOptions(newLoanAccount, clientId, groupId);
            }

            // add product options, allowed loan officers and calendar options
            // (calendar options will be null in individual loan)
            newLoanAccount = LoanAccountData.associationsAndTemplate(newLoanAccount, productOptions, allowedLoanOfficers, calendarOptions,
                    accountLinkingOptions, isRatesEnabled);
        }
        final List<DatatableData> datatableTemplates = this.entityDatatableChecksReadService
                .retrieveTemplates(StatusEnum.CREATE.getCode().longValue(), EntityTables.LOAN.getName(), productId);
        newLoanAccount.setDatatables(datatableTemplates);

        final List<LoanAssignorData> loanAssignorOptions = this.loanReadPlatformService.retrieveLoanAssignorData(null);
        newLoanAccount.setLoanAssignorOptions(loanAssignorOptions);

        final MaximumCreditRateConfigurationData maximumCreditRateConfigurationData = this.loanProductReadPlatformService
                .retrieveMaximumCreditRateConfigurationData();
        newLoanAccount.setMaximumCreditRateConfiguration(maximumCreditRateConfigurationData);

        // Add smvl info to the template in order to pick the correct charges
        newLoanAccount.setSmvl(configurationDomainServiceJpa.retrieveSMVLLimit());

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, newLoanAccount, LOAN_DATA_PARAMETERS);
    }

    private Collection<PortfolioAccountData> getAccountLinkingOptions(final LoanAccountData newLoanAccount, final Long clientId,
            final Long groupId) {
        final CurrencyData currencyData = newLoanAccount.getCurrency();
        String currencyCode = null;
        if (currencyData != null) {
            currencyCode = currencyData.getCode();
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
    @Operation(summary = "Retrieve a Loan", description = """
            Note: template=true parameter doesn't apply to this resource."
            Example Requests:
            loans/1
            loans/1?fields=id,principal,annualInterestRate
            loans/1?associations=all
            loans/1?associations=all&exclude=guarantors
            loans/1?fields=id,principal,annualInterestRate&associations=repaymentSchedule,transactions""")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.GetLoansLoanIdResponse.class)))
    public String retrieveLoan(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId,
            @DefaultValue("false") @QueryParam("staffInSelectedOfficeOnly") @Parameter(description = "staffInSelectedOfficeOnly") final boolean staffInSelectedOfficeOnly,
            @DefaultValue("all") @QueryParam("associations") @Parameter(in = ParameterIn.QUERY, name = "associations", description = "Loan object relations to be included in the response", required = false, examples = {
                    @ExampleObject(value = "all"), @ExampleObject(value = "repaymentSchedule,transactions") }) final String associations,
            @QueryParam("exclude") @Parameter(in = ParameterIn.QUERY, name = "exclude", description = "Optional Loan object relation list to be filtered in the response", required = false, example = "guarantors,futureSchedule") final String exclude,
            @QueryParam("fields") @Parameter(in = ParameterIn.QUERY, name = "fields", description = "Optional Loan attribute list to be in the response", required = false, example = "id,principal,annualInterestRate") final String fields,
            @Context final UriInfo uriInfo) {
        return retrieveLoan(loanId, null, staffInSelectedOfficeOnly, exclude, uriInfo);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List Loans", description = """
            The list capability of loans can support pagination and sorting.
            Example Requests:
            loans?fields=accountNo
            loans?offset=10&limit=50
            loans?orderBy=accountNo&sortOrder=DESC""")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.GetLoansResponse.class)))
    public String retrieveAll(@Context final UriInfo uriInfo,
            @QueryParam("externalId") @Parameter(description = "externalId") final String externalId,
            @QueryParam("offset") @Parameter(description = "offset") final Integer offset,
            @QueryParam("limit") @Parameter(description = "limit") final Integer limit,
            @QueryParam("orderBy") @Parameter(description = "orderBy") final String orderBy,
            @QueryParam("sortOrder") @Parameter(description = "sortOrder") final String sortOrder,
            @QueryParam("accountNo") @Parameter(description = "accountNo") final String accountNo,
            @QueryParam("status") @Parameter(description = "status") final String status) {

        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final SearchParameters searchParameters = SearchParameters.builder().accountNo(accountNo).sortOrder(sortOrder)
                .externalId(externalId).offset(offset).limit(limit).orderBy(orderBy).status(status).build();

        final Page<LoanAccountData> loanBasicDetails = this.loanReadPlatformService.retrieveAll(searchParameters);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, loanBasicDetails, LOAN_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Calculate loan repayment schedule | Submit a new Loan Application", description = """
            It calculates the loan repayment Schedule
            Submits a new loan application
            Mandatory Fields: clientId, productId, principal, loanTermFrequency, loanTermFrequencyType, loanType, numberOfRepayments, repaymentEvery, repaymentFrequencyType, interestRatePerPeriod, amortizationType, interestType, interestCalculationPeriodType, transactionProcessingStrategyCode, expectedDisbursementDate, submittedOnDate, loanType
            Optional Fields: graceOnPrincipalPayment, graceOnInterestPayment, graceOnInterestCharged, linkAccountId, allowPartialPeriodInterestCalcualtion, fixedEmiAmount, maxOutstandingLoanBalance, disbursementData, graceOnArrearsAgeing, createStandingInstructionAtDisbursement (requires linkedAccountId if set to true)
            Additional Mandatory Fields if interest recalculation is enabled for product and Rest frequency not same as repayment period: recalculationRestFrequencyDate
            Additional Mandatory Fields if interest recalculation with interest/fee compounding is enabled for product and compounding frequency not same as repayment period: recalculationCompoundingFrequencyDate
            Additional Mandatory Field if Entity-Datatable Check is enabled for the entity of type loan: datatables""")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansRequest.class)))
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansResponse.class)))
    public String calculateLoanScheduleOrSubmitLoanApplication(
            @QueryParam("command") @Parameter(description = "command") final String commandParam, @Context final UriInfo uriInfo,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        if (CommandParameterUtil.is(commandParam, "calculateLoanSchedule")) {

            final JsonElement parsedQuery = this.fromJsonHelper.parse(apiRequestBodyAsJson);

            Boolean validateParams = Boolean.TRUE;

            // If there is no attribute client_id, then validateParams is set to false
            if (Objects.isNull(parsedQuery.getAsJsonObject().get(ClientApiConstants.clientIdParamName))) {
                validateParams = Boolean.FALSE;

                // If not exists parameter "loanType" with value "individual", add it
                if (Objects.isNull(parsedQuery.getAsJsonObject().get(LoanApiConstants.loanTypeParameterName))) {
                    parsedQuery.getAsJsonObject().addProperty(LoanApiConstants.loanTypeParameterName,
                            ClientApiConstants.CLIENT_TYPE_INDIVIDUAL);
                }
            }

            final JsonQuery query = JsonQuery.from(apiRequestBodyAsJson, parsedQuery, this.fromJsonHelper);

            final LoanScheduleModel loanSchedule = this.calculationPlatformService.calculateLoanSchedule(query, validateParams);

            final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
            return this.loanScheduleToApiJsonSerializer.serialize(settings, loanSchedule.toData(), new HashSet<>());
        }

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createLoanApplication().withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Modify a loan application", description = "Loan application can only be modified when in 'Submitted and pending approval' state. Once the application is approved, the details cannot be changed using this method.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PutLoansLoanIdRequest.class)))
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PutLoansLoanIdResponse.class)))
    public String modifyLoanApplication(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId,
            @QueryParam("command") @Parameter(description = "command") final String commandParam,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        return modifyLoanApplication(loanId, null, commandParam, apiRequestBodyAsJson);
    }

    @DELETE
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a Loan Application", description = "Note: Only loans in \"Submitted and awaiting approval\" status can be deleted.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.DeleteLoansLoanIdResponse.class)))
    public String deleteLoanApplication(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId) {
        return deleteLoanApplication(loanId, null);
    }

    @POST
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = """
            Approve Loan Application | Recover Loan Guarantee | Undo Loan Application Approval | Assign a Loan Officer | Unassign a Loan Officer | Reject Loan Application | Applicant Withdraws from Loan Application | Disburse Loan Disburse Loan To Savings Account | Undo Loan Disbursal", description = "Approve Loan Application:
            Mandatory Fields: approvedOnDate
            Optional Fields: approvedLoanAmount and expectedDisbursementDate
            Approves the loan application
            Recover Loan Guarantee:
            Recovers the loan guarantee
            Undo Loan Application Approval:
            Undoes the Loan Application Approval
            Assign a Loan Officer:
            Allows you to assign Loan Officer for existing Loan.
            Unassign a Loan Officer:
            Allows you to unassign the Loan Officer.
            Reject Loan Application:
            Mandatory Fields: rejectedOnDate
            Allows you to reject the loan application
            Applicant Withdraws from Loan Application:
            Mandatory Fields: withdrawnOnDate
            Allows the applicant to withdraw the loan application
            Disburse Loan:
            Mandatory Fields: actualDisbursementDate
            Optional Fields: transactionAmount and fixedEmiAmount
            Disburses the Loan
            Disburse Loan To Savings Account:
            Mandatory Fields: actualDisbursementDate
            Optional Fields: transactionAmount and fixedEmiAmount
            Disburses the loan to Saving Account
            Undo Loan Disbursal:Undoes the Loan Disbursal
            Showing request and response for Assign a Loan Officer""")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansLoanIdRequest.class)))
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansLoanIdResponse.class)))
    public String stateTransitions(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId,
            @QueryParam("command") @Parameter(description = "command") final String commandParam,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        return stateTransitions(loanId, null, commandParam, apiRequestBodyAsJson);
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
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        Collection<GlimRepaymentTemplate> glimRepaymentTemplate = this.glimAccountInfoReadPlatformService.findglimRepaymentTemplate(glimId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.glimTemplateToApiJsonSerializer.serialize(settings, glimRepaymentTemplate, GLIM_ACCOUNTS_DATA_PARAMETERS);
    }

    @POST
    @Path("glimAccount/{glimId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = """
            Approve GLIM Application | Undo GLIM Application Approval | Reject GLIM Application | Disburse Loan Disburse Loan To Savings Account | Undo Loan Disbursal", description = "Approve GLIM Application:
            Mandatory Fields: approvedOnDate
            Optional Fields: approvedLoanAmount and expectedDisbursementDate
            Approves the GLIM application
            Undo GLIM Application Approval:
            Undoes the GLIM Application Approval
            Reject GLIM Application:
            Mandatory Fields: rejectedOnDate
            Allows you to reject the GLIM application
            Disburse Loan:
            Mandatory Fields: actualDisbursementDate
            Optional Fields: transactionAmount and fixedEmiAmount
            Disburses the Loan
            Disburse Loan To Savings Account:
            Mandatory Fields: actualDisbursementDate
            Optional Fields: transactionAmount and fixedEmiAmount
            Disburses the loan to Saving Account
            Undo Loan Disbursal:
            Undoes the Loan Disbursal""")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansLoanIdRequest.class)))
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansLoanIdResponse.class)))
    public String glimStateTransitions(@PathParam("glimId") final Long glimId, @QueryParam("command") final String commandParam,
            final String apiRequestBodyAsJson) {

        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);

        CommandWrapper commandRequest = null;
        if (CommandParameterUtil.is(commandParam, "reject")) {
            commandRequest = builder.rejectGLIMApplication(glimId).build();
        } else if (CommandParameterUtil.is(commandParam, "approve")) {
            commandRequest = builder.approveGLIMLoanApplication(glimId).build();
        } else if (CommandParameterUtil.is(commandParam, DISBURSE_ACTION)) {
            commandRequest = builder.disburseGlimLoanApplication(glimId).build();
        } else if (CommandParameterUtil.is(commandParam, "glimrepayment")) {
            commandRequest = builder.repaymentGlimLoanApplication(glimId).build();
        } else if (CommandParameterUtil.is(commandParam, "undodisbursal")) {
            commandRequest = builder.undoGLIMLoanDisbursal(glimId).build();
        } else if (CommandParameterUtil.is(commandParam, "undoapproval")) {
            commandRequest = builder.undoGLIMLoanApproval(glimId).build();
        }

        if (commandRequest == null) {
            throw new UnrecognizedQueryParamException("command", commandParam);
        }
        CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("repayments/downloadtemplate")
    @Produces("application/vnd.ms-excel")
    public Response getLoanRepaymentTemplate(@QueryParam("officeId") final Long officeId,
            @QueryParam("dateFormat") final String dateFormat) {
        return bulkImportWorkbookPopulatorService.getTemplate(GlobalEntityType.LOAN_TRANSACTIONS.toString(), officeId, null, dateFormat);
    }

    @GET
    @Path("loanwriteoffs/downloadtemplate")
    @Produces("application/vnd.ms-excel")
    public Response getLoanWriteOffTemplate(@QueryParam("officeId") final Long officeId,
            @QueryParam("dateFormat") final String dateFormat) {
        return bulkImportWorkbookPopulatorService.getTemplate(GlobalEntityType.LOAN_WRITE_OFFS.toString(), officeId, null, dateFormat);
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
                fileDetail, locale, dateFormat, null);
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
                uploadedInputStream, fileDetail, locale, dateFormat, null);
        return this.toApiJsonSerializer.serialize(importDocumentId);
    }

    @POST
    @Path("loanwriteoffs/uploadtemplate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestBody(description = "Upload Loan Write Off template", content = {
            @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = UploadRequest.class)) })
    public String postLoanWriteOffTemplate(@FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("locale") final String locale,
            @FormDataParam("dateFormat") final String dateFormat) {
        final Long importDocumentId = this.bulkImportWorkbookService.importWorkbook(GlobalEntityType.LOAN_WRITE_OFFS.toString(),
                uploadedInputStream, fileDetail, locale, dateFormat, new HashMap<>());
        return this.toApiJsonSerializer.serialize(importDocumentId);
    }

    @GET
    @Path("{loanId}/delinquencytags")
    @Consumes({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the Loan Delinquency Tag history using the Loan Id", description = "")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DelinquencyApiResourceSwagger.GetDelinquencyTagHistoryResponse.class))))
    public String getDelinquencyTagHistory(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId) {
        return getDelinquencyTagHistory(loanId, null);
    }

    // External id related APIs
    @GET
    @Path("external-id/{loanExternalId}/template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.GetLoansApprovalTemplateResponse.class)))
    public String retrieveApprovalTemplate(
            @PathParam("loanExternalId") @Parameter(description = "loanExternalId", required = true) final String loanExternalId,
            @QueryParam("templateType") @Parameter(description = "templateType") final String templateType,
            @Context final UriInfo uriInfo) {
        return retrieveApprovalTemplate(null, loanExternalId, templateType, uriInfo);
    }

    @GET
    @Path("external-id/{loanExternalId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a Loan", description = """
            Note: template=true parameter doesn't apply to this resource
            Example Requests:
            loans/external-id/7dd80a7c-ycba-a446-t378-91eb6f53e854
            loans/external-id/7dd80a7c-ycba-a446-t378-91eb6f53e854?fields=id,principal,annualInterestRate
            loans/external-id/7dd80a7c-ycba-a446-t378-91eb6f53e854?associations=all
            /external-id/7dd80a7c-ycba-a446-t378-91eb6f53e854?associations=all&exclude=guarantors
            loans/external-id/7dd80a7c-ycba-a446-t378-91eb6f53e854?fields=id,principal,annualInterestRate&associations=repaymentSchedule,transactions""")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.GetLoansLoanIdResponse.class)))
    public String retrieveLoan(
            @PathParam("loanExternalId") @Parameter(description = "loanExternalId", required = true) final String loanExternalId,
            @DefaultValue("false") @QueryParam("staffInSelectedOfficeOnly") @Parameter(description = "staffInSelectedOfficeOnly") final boolean staffInSelectedOfficeOnly,
            @DefaultValue("all") @QueryParam("associations") @Parameter(in = ParameterIn.QUERY, name = "associations", description = "Loan object relations to be included in the response", required = false, examples = {
                    @ExampleObject(value = "all"), @ExampleObject(value = "repaymentSchedule,transactions") }) final String associations,
            @QueryParam("exclude") @Parameter(in = ParameterIn.QUERY, name = "exclude", description = "Optional Loan object relation list to be filtered in the response", required = false, example = "guarantors,futureSchedule") final String exclude,
            @QueryParam("fields") @Parameter(in = ParameterIn.QUERY, name = "fields", description = "Optional Loan attribute list to be in the response", required = false, example = "id,principal,annualInterestRate") final String fields,
            @Context final UriInfo uriInfo) {
        return retrieveLoan(null, loanExternalId, staffInSelectedOfficeOnly, exclude, uriInfo);
    }

    @PUT
    @Path("external-id/{loanExternalId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Modify a loan application", description = "Loan application can only be modified when in 'Submitted and pending approval' state. Once the application is approved, the details cannot be changed using this method.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PutLoansLoanIdRequest.class)))
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PutLoansLoanIdResponse.class)))
    public String modifyLoanApplication(
            @PathParam("loanExternalId") @Parameter(description = "loanExternalId", required = true) final String loanExternalId,
            @QueryParam("command") @Parameter(description = "command") final String commandParam,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        return modifyLoanApplication(null, loanExternalId, commandParam, apiRequestBodyAsJson);
    }

    @DELETE
    @Path("external-id/{loanExternalId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a Loan Application", description = "Note: Only loans in \"Submitted and awaiting approval\" status can be deleted.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.DeleteLoansLoanIdResponse.class)))
    public String deleteLoanApplication(
            @PathParam("loanExternalId") @Parameter(description = "loanExternalId", required = true) final String loanExternalId) {
        return deleteLoanApplication(null, loanExternalId);
    }

    @POST
    @Path("external-id/{loanExternalId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = """
            Approve Loan Application | Recover Loan Guarantee | Undo Loan Application Approval | Assign a Loan Officer | Unassign a Loan Officer | Reject Loan Application | Applicant Withdraws from Loan Application | Disburse Loan Disburse Loan To Savings Account | Undo Loan Disbursal", description = "Approve Loan Application:
            Mandatory Fields: approvedOnDate
            Optional Fields: approvedLoanAmount and expectedDisbursementDate
            Approves the loan application
            Recover Loan Guarantee:
            Recovers the loan guarantee
            Undo Loan Application Approval:
            Undoes the Loan Application Approval
            Assign a Loan Officer:
            Allows you to assign Loan Officer for existing Loan.
            Unassign a Loan Officer:
            Allows you to unassign the Loan Officer.
            Reject Loan Application:
            Mandatory Fields: rejectedOnDate
            Allows you to reject the loan application
            Applicant Withdraws from Loan Application:
            Mandatory Fields: withdrawnOnDate
            Allows the applicant to withdraw the loan application
            Disburse Loan:
            Mandatory Fields: actualDisbursementDate
            Optional Fields: transactionAmount and fixedEmiAmount
            Disburses the Loan
            Disburse Loan To Savings Account:
            Mandatory Fields: actualDisbursementDate
            Optional Fields: transactionAmount and fixedEmiAmount
            Disburses the loan to Saving Account
            Undo Loan Disbursal:
            Undoes the Loan Disbursal
            Showing request and response for Assign a Loan Officer""")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansLoanIdRequest.class)))
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.PostLoansLoanIdResponse.class)))
    public String stateTransitions(
            @PathParam("loanExternalId") @Parameter(description = "loanExternalId", required = true) final String loanExternalId,
            @QueryParam("command") @Parameter(description = "command") final String commandParam,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        return stateTransitions(null, loanExternalId, commandParam, apiRequestBodyAsJson);
    }

    @GET
    @Path("external-id/{loanExternalId}/delinquencytags")
    @Consumes({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the Loan Delinquency Tag history using the Loan Id", description = "")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DelinquencyApiResourceSwagger.GetDelinquencyTagHistoryResponse.class))))
    public String getDelinquencyTagHistory(
            @PathParam("loanExternalId") @Parameter(description = "loanExternalId", required = true) final String loanExternalId) {
        return getDelinquencyTagHistory(null, loanExternalId);
    }

    @GET
    @Path("{loanId}/delinquency-actions")
    @Consumes({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve delinquency actions related to the loan", description = "")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DelinquencyApiResourceSwagger.GetDelinquencyActionsResponse.class))))
    public String getLoanDelinquencyActions(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId) {
        return getLoanDelinquencyActions(loanId, null);
    }

    @GET
    @Path("external-id/{loanExternalId}/delinquency-actions")
    @Consumes({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve delinquency actions related to the loan", description = "")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DelinquencyApiResourceSwagger.GetDelinquencyActionsResponse.class))))
    public String getLoanDelinquencyActions(
            @PathParam("loanExternalId") @Parameter(description = "loanExternalId", required = true) final String loanExternalId) {
        return getLoanDelinquencyActions(null, loanExternalId);
    }

    @POST
    @Path("{loanId}/delinquency-actions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Adds a new delinquency action for a loan", description = "")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = DelinquencyApiResourceSwagger.PostLoansDelinquencyActionRequest.class)))
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = DelinquencyApiResourceSwagger.PostLoansDelinquencyActionResponse.class)))
    public String createLoanDelinquencyAction(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        return createLoanDelinquencyAction(loanId, ExternalId.empty(), apiRequestBodyAsJson);
    }

    @POST
    @Path("external-id/{loanExternalId}/delinquency-actions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Adds a new delinquency action for a loan", description = "")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = DelinquencyApiResourceSwagger.PostLoansDelinquencyActionRequest.class)))
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = DelinquencyApiResourceSwagger.PostLoansDelinquencyActionResponse.class)))
    public String createLoanDelinquencyAction(
            @PathParam("loanExternalId") @Parameter(description = "loanExternalId", required = true) final String loanExternalId,
            @Context final UriInfo uriInfo, @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        return createLoanDelinquencyAction(null, ExternalIdFactory.produce(loanExternalId), apiRequestBodyAsJson);
    }

    @GET
    @ModelAttribute
    @Path("{loanId}/disbursement-report")
    @GetMapping("/disbursement-report")
    @Operation(summary = "Download loan disbursement report in PDF format", description = "Download loan disbursement report in PDF format")
    public void getDisbursementReportPDF(@Context HttpServletResponse response,
            @PathParam("loanId") @Parameter(description = "loanId") final Long loanId) throws DocumentException, IOException {
        context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        this.loanReadPlatformService.exportLoanDisbursementPDF(loanId, response);
    }

    @GET
    @ModelAttribute
    @Path("{loanId}/schedule-report")
    @Operation(summary = "Download loan Schedule report in PDF format", description = "Download loan Schedule report in PDF format")
    public void getScheduleReportPDF(@Context HttpServletResponse response,
            @PathParam("loanId") @Parameter(description = "loanId") final Long loanId, @QueryParam("scheduleType") String scheduleType)
            throws DocumentException, IOException {
        context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        boolean isRepaymentSchedule = scheduleType.equalsIgnoreCase("repayment");
        LoanAccountData loanBasicDetails = this.loanReadPlatformService.retrieveOne(loanId);
        LoanScheduleData repaymentSchedule = null;
        Collection<DisbursementData> disbursementData = this.loanReadPlatformService.retrieveLoanDisbursementDetails(loanId);
        final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedData = loanBasicDetails.getTimeline().repaymentScheduleRelatedData(
                loanBasicDetails.getCurrency(), loanBasicDetails.getPrincipal(), loanBasicDetails.getApprovedPrincipal(),
                loanBasicDetails.getInArrearsTolerance(), loanBasicDetails.getFeeChargesAtDisbursementCharged());
        if (isRepaymentSchedule) {
            repaymentSchedule = this.loanReadPlatformService.retrieveRepaymentSchedule(loanId, repaymentScheduleRelatedData,
                    disbursementData, loanBasicDetails.isInterestRecalculationEnabled(),
                    LoanScheduleType.fromEnumOptionData(loanBasicDetails.getLoanScheduleType()));
            this.calculationPlatformService.getFeeChargesDetail(repaymentSchedule, loanId);
        } else {
            repaymentSchedule = this.loanScheduleHistoryReadPlatformService.retrieveRepaymentArchiveSchedule(loanId,
                    repaymentScheduleRelatedData, disbursementData,
                    LoanScheduleType.fromEnumOptionData(loanBasicDetails.getLoanScheduleType()));
        }
        this.loanReadPlatformService.exportLoanSchedulePDF(loanBasicDetails, scheduleType, repaymentSchedule, response);
    }

    @GET
    @Path("{loanId}/loanblockingreasons")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve loan blocking reasons", description = """
            Retrieve loan blocking reasons
            Example Requests:
            loans/1/loanblockingreasons""")
    public String retrieveBlockingReasons(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId,
            @Context final UriInfo uriInfo) {

        final Collection<LoanBlockingReasonData> blockingReasons = this.loanBlockingReasonReadPlatformService
                .retrieveLoanBlockingReason(loanId);
        return this.toApiJsonSerializer.serialize(blockingReasons);
    }

    @PUT
    @Path("{loanId}/loanblockingreasons")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Deletes a blocking reason from a loan", description = "")
    public String deleteLoanBlockingReasons(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId,
            @Context final UriInfo uriInfo, @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteLoanBlockingReason(loanId).withJson(apiRequestBodyAsJson)
                .build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.jsonSerializerTagHistory.serialize(result);
    }

    @GET
    @Path("block-guarantee/template")
    @Operation(summary = "Get the template for block guarantee")
    public String getBlockGuaranteeTemplate() throws DocumentException {
        context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        final BlockingReasonsData blockingReasonSetting = loanBlockingReasonReadPlatformService.retrieveLoanBlockingSettings(
                BlockLevel.CREDIT.toString(), BlockingReasonSettingEnum.CREDIT_RECLAMADO_A_AVALADORA.getDatabaseString());
        return this.toApiJsonSerializer.serialize(blockingReasonSetting);
    }

    @POST
    @Path("{loanId}/loanblockingreasons")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Adds a new delinquency action for a loan", description = "")
    public String addBlockingReasonToLoan(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId,
            @Context final UriInfo uriInfo, @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().addLoanBlockingReason(loanId).withJson(apiRequestBodyAsJson)
                .build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.jsonSerializerTagHistory.serialize(result);
    }

    @POST
    @Path("loanblockingreason/unblock")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Undo block loan with blocking reason", description = "Undo block loan with blocking reason")
    @ApiResponse(responseCode = "200", description = "OK")
    public String unblockLoanWithBlockingReason(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .unblockLoanWithBlockingReason() //
                .withJson(apiRequestBodyAsJson) //
                .build(); //

        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("loanblockingreason/{blockingReasonId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve all loan with blocking reason", description = "Retrieve all loan with blocking reason")
    @ApiResponse(responseCode = "200", description = "OK")
    public String retrieveAllLoanWithBlockingReasonData(@PathParam("blockingReasonId") final Long blockingReasonId) {
        return retrieveAllLoanWithBlockingReason(blockingReasonId);
    }

    private String retrieveApprovalTemplate(final Long loanId, final String loanExternalIdStr, final String templateType,
            final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        LoanApprovalData loanApprovalTemplate = null;
        ExternalId loanExternalId = ExternalIdFactory.produce(loanExternalIdStr);
        Long resolvedLoanId = getResolvedLoanId(loanId, loanExternalId);
        if (templateType == null) {
            final String errorMsg = "Loan template type must be provided";
            throw new LoanTemplateTypeRequiredException(errorMsg);
        } else if (templateType.equals("approval")) {
            loanApprovalTemplate = this.loanReadPlatformService.retrieveApprovalTemplate(resolvedLoanId);
        }

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.loanApprovalDataToApiJsonSerializer.serialize(settings, loanApprovalTemplate, LOAN_APPROVAL_DATA_PARAMETERS);
    }

    @SuppressWarnings({ "squid:S3776" })
    private String retrieveLoan(final Long loanId, final String loanExternalIdStr, boolean staffInSelectedOfficeOnly, final String exclude,
            final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        ExternalId loanExternalId = ExternalIdFactory.produce(loanExternalIdStr);
        Long resolvedLoanId = getResolvedLoanId(loanId, loanExternalId);
        LoanAccountData loanBasicDetails = this.loanReadPlatformService.retrieveOne(resolvedLoanId);
        final Long InterestRatePoints = loanBasicDetails.getInterestRatePoints();
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
        if (loanBasicDetails.getRepaymentFrequencyType() != null
                && loanBasicDetails.getRepaymentFrequencyType().getId().intValue() == PeriodFrequencyType.MONTHS.getValue()) {
            Collection<CalendarData> loanCalendarDatas = this.calendarReadPlatformService.retrieveCalendarsByEntity(resolvedLoanId,
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
        Collection<NoteData> notes = null;
        PortfolioAccountData linkedAccount = null;
        Collection<DisbursementData> disbursementData = null;
        Collection<LoanTermVariationsData> emiAmountVariations = null;
        Collection<LoanCollateralResponseData> loanCollateralManagements;
        Collection<LoanCollateralManagementData> loanCollateralManagementData = new ArrayList<>();
        CollectionData collectionData = this.delinquencyReadPlatformService.calculateLoanCollectionData(resolvedLoanId);

        final Set<String> mandatoryResponseParameters = new HashSet<>();
        final Set<String> associationParameters = ApiParameterHelper.extractAssociationsForResponseIfProvided(uriInfo.getQueryParameters());
        final Collection<LoanTransactionData> currentLoanRepayments = this.loanReadPlatformService.retrieveLoanTransactions(resolvedLoanId);
        if (!associationParameters.isEmpty()) {
            if (associationParameters.contains(DataTableApiConstant.allAssociateParamName)) {
                associationParameters.addAll(Arrays.asList(DataTableApiConstant.repaymentScheduleAssociateParamName,
                        DataTableApiConstant.futureScheduleAssociateParamName, DataTableApiConstant.originalScheduleAssociateParamName,
                        DataTableApiConstant.transactionsAssociateParamName, DataTableApiConstant.chargesAssociateParamName,
                        DataTableApiConstant.guarantorsAssociateParamName, DataTableApiConstant.collateralAssociateParamName,
                        DataTableApiConstant.notesAssociateParamName, DataTableApiConstant.linkedAccountAssociateParamName,
                        DataTableApiConstant.multiDisburseDetailsAssociateParamName, DataTableApiConstant.collectionAssociateParamName));
            }

            ApiParameterHelper.excludeAssociationsForResponseIfProvided(exclude, associationParameters);

            if (associationParameters.contains(DataTableApiConstant.guarantorsAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.guarantorsAssociateParamName);
                guarantors = this.guarantorReadPlatformService.retrieveGuarantorsForLoan(resolvedLoanId);
                if (CollectionUtils.isEmpty(guarantors)) {
                    guarantors = null;
                }
            }

            if (associationParameters.contains(DataTableApiConstant.transactionsAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.transactionsAssociateParamName);
                if (!CollectionUtils.isEmpty(currentLoanRepayments)) {
                    loanRepayments = currentLoanRepayments;
                }
            }

            if (associationParameters.contains(DataTableApiConstant.multiDisburseDetailsAssociateParamName)
                    || associationParameters.contains(DataTableApiConstant.repaymentScheduleAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.multiDisburseDetailsAssociateParamName);
                disbursementData = this.loanReadPlatformService.retrieveLoanDisbursementDetails(resolvedLoanId);
            }

            if (associationParameters.contains(DataTableApiConstant.emiAmountVariationsAssociateParamName)
                    || associationParameters.contains(DataTableApiConstant.repaymentScheduleAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.emiAmountVariationsAssociateParamName);
                emiAmountVariations = this.loanReadPlatformService.retrieveLoanTermVariations(resolvedLoanId,
                        LoanTermVariationType.EMI_AMOUNT.getValue());
            }

            if (associationParameters.contains(DataTableApiConstant.repaymentScheduleAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.repaymentScheduleAssociateParamName);
                final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedData = loanBasicDetails.getTimeline()
                        .repaymentScheduleRelatedData(loanBasicDetails.getCurrency(), loanBasicDetails.getPrincipal(),
                                loanBasicDetails.getApprovedPrincipal(), loanBasicDetails.getInArrearsTolerance(),
                                loanBasicDetails.getFeeChargesAtDisbursementCharged());
                repaymentSchedule = this.loanReadPlatformService.retrieveRepaymentSchedule(resolvedLoanId, repaymentScheduleRelatedData,
                        disbursementData, loanBasicDetails.isInterestRecalculationEnabled(),
                        LoanScheduleType.fromEnumOptionData(loanBasicDetails.getLoanScheduleType()));
                this.calculationPlatformService.getFeeChargesDetail(repaymentSchedule, resolvedLoanId);

                repaymentSchedule.getPeriods().stream().forEach(period -> {
                    Collection<LoanInstallmentChargeData> loanInstallmentChargeData = this.loanChargeReadPlatformService
                            .retrieveInstallmentLoanCharges(period.getInstallmentId());
                    if (!loanInstallmentChargeData.isEmpty()) {
                        Collection<FeeDetails> feeDetails = new ArrayList<>();
                        loanInstallmentChargeData.forEach(charge -> feeDetails.add(new FeeDetails(charge.getChargeName(),
                                charge.getAmount(), charge.getAmountPaid(), charge.getAmountOutstanding())));
                        // update repayment schedule period with fee details
                        period.setFeeDetails(feeDetails);
                    }
                });
                if (associationParameters.contains(DataTableApiConstant.futureScheduleAssociateParamName)
                        && loanBasicDetails.isInterestRecalculationEnabled()) {
                    mandatoryResponseParameters.add(DataTableApiConstant.futureScheduleAssociateParamName);
                    this.calculationPlatformService.updateFutureSchedule(repaymentSchedule, resolvedLoanId);
                }

                // Farooq - 14/06/2024 - Added to retrieve original schedule irrespective of loan status and interest
                // recalculation
                if (associationParameters.contains(DataTableApiConstant.originalScheduleAssociateParamName)) {
                    mandatoryResponseParameters.add(DataTableApiConstant.originalScheduleAssociateParamName);
                    LoanScheduleData loanScheduleData = this.loanScheduleHistoryReadPlatformService.retrieveRepaymentArchiveSchedule(
                            resolvedLoanId, repaymentScheduleRelatedData, disbursementData,
                            LoanScheduleType.fromEnumOptionData(loanBasicDetails.getLoanScheduleType()));
                    loanBasicDetails = LoanAccountData.withOriginalSchedule(loanBasicDetails, loanScheduleData);
                }
            }

            if (associationParameters.contains(DataTableApiConstant.chargesAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.chargesAssociateParamName);
                charges = this.loanChargeReadPlatformService.retrieveLoanCharges(resolvedLoanId);
                if (CollectionUtils.isEmpty(charges)) {
                    charges = null;
                }
            }

            if (associationParameters.contains(DataTableApiConstant.collateralAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.collateralAssociateParamName);
                loanCollateralManagements = this.loanCollateralManagementReadPlatformService
                        .getLoanCollateralResponseDataList(resolvedLoanId);
                for (LoanCollateralResponseData loanCollateralManagement : loanCollateralManagements) {
                    loanCollateralManagementData.add(loanCollateralManagement.toCommand());
                }
            }

            if (associationParameters.contains(DataTableApiConstant.meetingAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.meetingAssociateParamName);
            }

            if (associationParameters.contains(DataTableApiConstant.notesAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.notesAssociateParamName);
                notes = this.noteReadPlatformService.retrieveNotesByResource(resolvedLoanId, NoteType.LOAN.getValue());
                if (CollectionUtils.isEmpty(notes)) {
                    notes = null;
                }
            }

            if (associationParameters.contains(DataTableApiConstant.linkedAccountAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.linkedAccountAssociateParamName);
                linkedAccount = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(resolvedLoanId);
            }
        }

        Collection<LoanProductData> productOptions = null;
        LoanProductData product;
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
        PaidInAdvanceData paidInAdvanceTemplate;
        Collection<LoanAccountSummaryData> clientActiveLoanOptions = null;

        final boolean template = ApiParameterHelper.template(uriInfo.getQueryParameters());
        if (template) {
            productOptions = this.loanProductReadPlatformService.retrieveAllLoanProductsForLookup();
            product = this.loanProductReadPlatformService.retrieveLoanProduct(loanBasicDetails.getLoanProductId());
            loanBasicDetails.setProduct(product);
            loanTermFrequencyTypeOptions = this.dropdownReadPlatformService.retrieveLoanTermFrequencyTypeOptions();
            repaymentFrequencyTypeOptions = this.dropdownReadPlatformService.retrieveRepaymentFrequencyTypeOptions();
            repaymentFrequencyNthDayTypeOptions = this.dropdownReadPlatformService.retrieveRepaymentFrequencyOptionsForNthDayOfMonth();
            repaymentFrequencyDayOfWeekTypeOptions = this.dropdownReadPlatformService.retrieveRepaymentFrequencyOptionsForDaysOfWeek();
            interestRateFrequencyTypeOptions = this.dropdownReadPlatformService.retrieveInterestRateFrequencyTypeOptions();

            amortizationTypeOptions = this.dropdownReadPlatformService.retrieveLoanAmortizationTypeOptions();
            if (product.isLinkedToFloatingInterestRates()) {
                interestTypeOptions = Collections.singletonList(interestType(InterestMethod.DECLINING_BALANCE));
            } else {
                interestTypeOptions = this.dropdownReadPlatformService.retrieveLoanInterestTypeOptions();
            }
            interestCalculationPeriodTypeOptions = this.dropdownReadPlatformService.retrieveLoanInterestRateCalculatedInPeriodOptions();

            fundOptions = this.fundReadPlatformService.retrieveAllFunds();
            repaymentStrategyOptions = this.dropdownReadPlatformService.retrieveTransactionProcessingStrategies();
            if (Boolean.TRUE.equals(product.getMultiDisburseLoan())) {
                chargeOptions = this.chargeReadPlatformService.retrieveLoanAccountApplicableCharges(resolvedLoanId,
                        new ChargeTimeType[] { ChargeTimeType.OVERDUE_INSTALLMENT });
            } else {
                chargeOptions = this.chargeReadPlatformService.retrieveLoanAccountApplicableCharges(resolvedLoanId,
                        new ChargeTimeType[] { ChargeTimeType.OVERDUE_INSTALLMENT, ChargeTimeType.TRANCHE_DISBURSEMENT });
            }
            chargeTemplate = this.loanChargeReadPlatformService.retrieveLoanChargeTemplate();

            Long officeId = loanBasicDetails.getClientOfficeId();

            if (officeId == null && loanBasicDetails.getGroup() != null) {
                officeId = loanBasicDetails.getGroup().getOfficeId();
            }
            allowedLoanOfficers = this.loanReadPlatformService.retrieveAllowedLoanOfficers(officeId, staffInSelectedOfficeOnly);

            loanPurposeOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("LoanPurpose");
            loanCollateralOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("LoanCollateral");
            final CurrencyData currencyData = loanBasicDetails.getCurrency();
            String currencyCode = null;
            if (currencyData != null) {
                currencyCode = currencyData.getCode();
            }
            final long[] accountStatus = { SavingsAccountStatusType.ACTIVE.getValue() };
            PortfolioAccountDTO portfolioAccountDTO = new PortfolioAccountDTO(PortfolioAccountType.SAVINGS.getValue(),
                    loanBasicDetails.getClientId(), currencyCode, accountStatus, DepositAccountType.SAVINGS_DEPOSIT.getValue());
            accountLinkingOptions = this.portfolioAccountReadPlatformService.retrieveAllForLookup(portfolioAccountDTO);

            if (!associationParameters.contains(DataTableApiConstant.linkedAccountAssociateParamName)) {
                mandatoryResponseParameters.add(DataTableApiConstant.linkedAccountAssociateParamName);
                linkedAccount = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(resolvedLoanId);
            }
            if (loanBasicDetails.getGroup() != null && loanBasicDetails.getGroup().getId() != null) {
                calendarOptions = this.loanReadPlatformService.retrieveCalendars(loanBasicDetails.getGroup().getId());
            }

            if (loanBasicDetails.getProduct().isCanUseForTopup() && loanBasicDetails.getClientId() != null) {
                clientActiveLoanOptions = this.accountDetailsReadPlatformService
                        .retrieveClientActiveLoanAccountSummary(loanBasicDetails.getClientId());
            }

        }

        Collection<ChargeData> overdueCharges = this.chargeReadPlatformService
                .retrieveLoanProductCharges(loanBasicDetails.getLoanProductId(), ChargeTimeType.OVERDUE_INSTALLMENT);

        paidInAdvanceTemplate = this.loanReadPlatformService.retrieveTotalPaidInAdvance(resolvedLoanId);

        // Get rates from Loan
        boolean isRatesEnabled = this.configurationDomainService.isSubRatesEnabled();
        List<RateData> rates = null;
        if (isRatesEnabled) {
            rates = this.rateReadService.retrieveLoanRates(resolvedLoanId);
        }

        // updating summary with transaction amounts summary
        if (loanBasicDetails.getSummary() != null) {
            loanBasicDetails
                    .setSummary(LoanSummaryData.withTransactionAmountsSummary(loanBasicDetails.getSummary(), currentLoanRepayments));
        }

        final LoanAccountData loanAccount = LoanAccountData.associationsAndTemplate(loanBasicDetails, repaymentSchedule, loanRepayments,
                charges, loanCollateralManagementData, guarantors, productOptions, loanTermFrequencyTypeOptions,
                repaymentFrequencyTypeOptions, repaymentFrequencyNthDayTypeOptions, repaymentFrequencyDayOfWeekTypeOptions,
                repaymentStrategyOptions, interestRateFrequencyTypeOptions, amortizationTypeOptions, interestTypeOptions,
                interestCalculationPeriodTypeOptions, fundOptions, chargeOptions, chargeTemplate, allowedLoanOfficers, loanPurposeOptions,
                loanCollateralOptions, calendarOptions, notes, accountLinkingOptions, linkedAccount, disbursementData, emiAmountVariations,
                overdueCharges, paidInAdvanceTemplate, interestRatesPeriods, clientActiveLoanOptions, rates, isRatesEnabled, collectionData,
                LoanScheduleType.getValuesAsEnumOptionDataList(), LoanScheduleProcessingType.getValuesAsEnumOptionDataList());

        final Long loanAssignorId = loanBasicDetails.getLoanAssignorId();
        if (loanAssignorId != null) {
            LoanAssignorData loanAssignorData = this.loanReadPlatformService.retrieveLoanAssignorDataById(loanAssignorId);
            loanAccount.setLoanAssignorData(loanAssignorData);
        }
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters(),
                mandatoryResponseParameters);
        loanAccount.setInterestRatePoints(InterestRatePoints);

        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(charges)) {
            final Optional<LoanChargeData> voluntaryInsuranceOptional = charges.stream().filter(c -> {
                final EnumOptionData chargeCalculationTypeEnumOption = c.getChargeCalculationType();
                return ChargeCalculationType.fromInt(chargeCalculationTypeEnumOption.getId().intValue()).isVoluntaryInsurance();
            }).findFirst();
            if (voluntaryInsuranceOptional.isPresent()) {
                final LoanChargeData voluntaryInsurance = voluntaryInsuranceOptional.get();
                voluntaryInsurance.setCodigoSeguro(loanAccount.getCodigoSeguro());
                voluntaryInsurance.setCedulaSeguroVoluntario(loanAccount.getCedulaSeguroVoluntario());
                loanAccount.setVoluntaryInsurance(voluntaryInsurance);
            }
        }

        // Add Datatables info and data to the response
        final List<DatatableData> datatableNamesList = this.readWriteNonCoreDataService.retrieveDatatableNames("m_loan");
        datatableNamesList.stream().forEach(name -> {
            GenericResultsetData results = this.readWriteNonCoreDataService.retrieveDataTableGenericResultSet(name.getRegisteredTableName(),
                    resolvedLoanId, "", null);

            name.setColumnHeaderData(null);

            if (Boolean.FALSE.equals(results.getData().isEmpty())) {
                name.setGenericResultSet(results);
            }
        });

        loanAccount.setDatatables(datatableNamesList);

        loanAccount.setSmvl(configurationDomainServiceJpa.retrieveSMVLLimit());

        final boolean isRediferir = this.loanReadPlatformService.retrieveRediferidoNumber(loanId) > 0;
        loanAccount.setRediferir(isRediferir);
        return this.toApiJsonSerializer.serialize(settings, loanAccount, LOAN_DATA_PARAMETERS);
    }

    private String modifyLoanApplication(final Long loanId, final String loanExternalIdStr, final String commandParam,
            final String apiRequestBodyAsJson) {
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);
        CommandWrapper commandRequest;
        ExternalId loanExternalId = ExternalIdFactory.produce(loanExternalIdStr);
        Long resolvedLoanId = getResolvedLoanId(loanId, loanExternalId);
        if (CommandParameterUtil.is(commandParam, LoanApiConstants.MARK_AS_FRAUD_COMMAND)) {
            commandRequest = builder.markAsFraud(resolvedLoanId).build();
        } else {
            commandRequest = builder.updateLoanApplication(resolvedLoanId).build();
        }

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    private String deleteLoanApplication(final Long loanId, final String loanExternalIdStr) {
        ExternalId loanExternalId = ExternalIdFactory.produce(loanExternalIdStr);
        Long resolvedLoanId = getResolvedLoanId(loanId, loanExternalId);
        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteLoanApplication(resolvedLoanId).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    private String stateTransitions(final Long loanId, final String loanExternalIdStr, final String commandParam,
            final String apiRequestBodyAsJson) {
        ExternalId loanExternalId = ExternalIdFactory.produce(loanExternalIdStr);
        Long resolvedLoanId = getResolvedLoanId(loanId, loanExternalId);
        CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);
        CommandWrapper commandRequest = null;
        if (CommandParameterUtil.is(commandParam, "reject")) {
            commandRequest = builder.rejectLoanApplication(resolvedLoanId).build();
        } else if (CommandParameterUtil.is(commandParam, "withdrawnByApplicant")) {
            commandRequest = builder.withdrawLoanApplication(resolvedLoanId).build();
        } else if (CommandParameterUtil.is(commandParam, "approve")) {
            commandRequest = builder.approveLoanApplication(resolvedLoanId).build();
        } else if (CommandParameterUtil.is(commandParam, DISBURSE_ACTION)) {
            commandRequest = builder.disburseLoanApplication(resolvedLoanId).build();
        } else if (CommandParameterUtil.is(commandParam, "disburseToSavings")) {
            commandRequest = builder.disburseLoanToSavingsApplication(resolvedLoanId).build();
        } else if (CommandParameterUtil.is(commandParam, "undoapproval")) {
            commandRequest = builder.undoLoanApplicationApproval(resolvedLoanId).build();
        } else if (CommandParameterUtil.is(commandParam, "undodisbursal")) {
            commandRequest = builder.undoLoanApplicationDisbursal(resolvedLoanId).build();
        } else if (CommandParameterUtil.is(commandParam, "undolastdisbursal")) {
            commandRequest = builder.undoLastDisbursalLoanApplication(resolvedLoanId).build();
        } else if (CommandParameterUtil.is(commandParam, "assignloanofficer")) {
            commandRequest = builder.assignLoanOfficer(resolvedLoanId).build();
        } else if (CommandParameterUtil.is(commandParam, "unassignloanofficer")) {
            commandRequest = builder.unassignLoanOfficer(resolvedLoanId).build();
        } else if (CommandParameterUtil.is(commandParam, "recoverGuarantees")) {
            commandRequest = new CommandWrapperBuilder().recoverFromGuarantor(resolvedLoanId).build();
        } else if (CommandParameterUtil.is(commandParam, "assigndelinquency")) {
            commandRequest = builder.assignDelinquency(resolvedLoanId).build();
        }

        if (commandRequest == null) {
            throw new UnrecognizedQueryParamException("command", commandParam);
        }
        CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        Optional<Loan> loanOpt = this.loanRepository.findById(resolvedLoanId);
        String productName = "";
        if (loanOpt.isPresent()) {
            Loan loan = loanOpt.get();
            productName = loan.getLoanProduct().getName();
        }
        // Check here if there is any pending reschedule request associated with this Credito Rotativo and approve it.
        if (CommandParameterUtil.is(commandParam, DISBURSE_ACTION)) {
            if (!productName.equalsIgnoreCase(LoanProductType.CREDITO_ROTATIVO.getCode())) {
                loanWritePlatformService.approveRescheduleRequest(resolvedLoanId, result.getResourceId(), null);
            }
        }

        return this.toApiJsonSerializer.serialize(result);
    }

    private String getDelinquencyTagHistory(final Long loanId, final String loanExternalIdStr) {
        context.authenticatedUser().validateHasReadPermission("DELINQUENCY_TAGS");
        ExternalId loanExternalId = ExternalIdFactory.produce(loanExternalIdStr);
        Long resolvedLoanId = getResolvedLoanId(loanId, loanExternalId);
        final Collection<LoanDelinquencyTagHistoryData> loanDelinquencyTagHistoryData = this.delinquencyReadPlatformService
                .retrieveDelinquencyRangeHistory(resolvedLoanId);
        return this.jsonSerializerTagHistory.serialize(loanDelinquencyTagHistoryData);
    }

    private Long getResolvedLoanId(final Long loanId, final ExternalId loanExternalId) {
        Long resolvedLoanId = loanId;
        if (resolvedLoanId == null) {
            loanExternalId.throwExceptionIfEmpty();
            resolvedLoanId = this.loanReadPlatformService.retrieveLoanIdByExternalId(loanExternalId);
            if (resolvedLoanId == null) {
                throw new LoanNotFoundException(loanExternalId);
            }
        }
        return resolvedLoanId;
    }

    private String getLoanDelinquencyActions(final Long loanId, final String loanExternalIdStr) {
        context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        ExternalId loanExternalId = ExternalIdFactory.produce(loanExternalIdStr);
        Long resolvedLoanId = getResolvedLoanId(loanId, loanExternalId);

        final Collection<LoanDelinquencyAction> delinquencyActions = this.delinquencyReadPlatformService
                .retrieveLoanDelinquencyActions(resolvedLoanId);
        List<LoanDelinquencyActionData> result = delinquencyActions.stream().map(LoanDelinquencyActionData::new).toList();
        return this.jsonSerializerTagHistory.serialize(result);
    }

    private String createLoanDelinquencyAction(Long loanId, ExternalId loanExternalId, String apiRequestBodyAsJson) {
        context.authenticatedUser().validateHasCreatePermission(RESOURCE_NAME_FOR_DELINQUENCY_ACTION_PERMISSIONS);
        Long resolvedLoanId = getResolvedLoanId(loanId, loanExternalId);

        CommandWrapperBuilder builder = new CommandWrapperBuilder().createDelinquencyAction(resolvedLoanId);
        builder.withJson(apiRequestBodyAsJson);
        CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(builder.build());
        return delinquencyActionSerializer.serialize(result);
    }

    private String retrieveAllLoanWithBlockingReason(Long blockingReasonId) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        final Collection<LoanBlockingReasonData> loanBlockingReasonData = this.loanBlockingReasonReadPlatformService
                .retrieveAllLoanWithBlockingReason(blockingReasonId);
        return this.toApiJsonSerializer.serialize(loanBlockingReasonData);
    }

    @POST
    @Path("cancelinsurance/voluntary")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Cancel Voluntary Insurance", description = "Cancel Voluntary Insurance")
    @ApiResponse(responseCode = "200", description = "OK")
    public String cancelVoluntaryInsurance(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .cancelVoluntaryInsurance() //
                .withJson(apiRequestBodyAsJson) //
                .build(); //

        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("cancelinsurance/badsale")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Cancel Insurance Due To Bad Sale", description = "Cancel Insurance Due To Bad Sale")
    @ApiResponse(responseCode = "200", description = "OK")
    public String cancelInsuranceBadSale(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .cancelInsuranceDueToBadSale() //
                .withJson(apiRequestBodyAsJson) //
                .build(); //

        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("reclaim/template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List Loans", description = """
            The list capability of loans can support pagination and sorting.
            Example Requests:loans
            loans?fields=accountNo
            loans?offset=10&limit=50
            loans?orderBy=accountNo&sortOrder=DESC""")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.GetLoansResponse.class)))
    public String retrieveAllForReclaimOrWriteOff(@Context final UriInfo uriInfo,
            @QueryParam("reclaimType") @Parameter(description = "reclaimType") final String reclaimType) {

        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final List<LoanReclaimData> reclaimData = this.loanReadPlatformService.retrieveClaimTemplate(reclaimType);
        final List<LoanReclaimData> excludedData = this.loanReadPlatformService.retrieveExcludedTemplate(reclaimType);
        ReclaimData data = new ReclaimData(reclaimData, excludedData);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.reclainApiJsonSerializer.serialize(settings, data, LOAN_DATA_PARAMETERS);
    }

    @POST
    @Path("reclaim/exclude/{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Exclude loan from reclaim screen", description = "Exclude loan from reclaim screen")
    @ApiResponse(responseCode = "200", description = "OK")
    public String excludeLoanFromReclaim(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .excludeFromReclaim(loanId) //
                .withJson(apiRequestBodyAsJson) //
                .build(); //

        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return toApiJsonSerializer.serialize(result);
    }

    @GET()
    @Path("/{loanId}/projections")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveDebtProjection(@PathParam("loanId") final Long loanId, @QueryParam("projectionDate") final String projectionDate,
            @QueryParam("dateFormat") final String dateFormat, @Context final UriInfo uriInfo) {

        // Verify user has permissions
        this.context.authenticatedUser();

        // Calculate projection
        LoanDebtProjectionData projection = this.loanDebtProjectionService.calculateDebtProjection(loanId, projectionDate, dateFormat);

        return this.toApiJsonSerializer.serialize(projection);
    }

    @GET
    @Path("{loanId}/calculateHonorariosAmount")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoansApiResourceSwagger.GetLoansApprovalTemplateResponse.class)))
    public BigDecimal calculateHonorariosAmount(@PathParam("loanId") @Parameter(description = "loanId", required = true) final Long loanId,
            @QueryParam("amount") @Parameter(description = "amount") final BigDecimal amount, @Context final UriInfo uriInfo) {
        this.context.authenticatedUser();
        return this.loanReadPlatformService.calculateHonorariosAmount(loanId, amount);
    }

}
