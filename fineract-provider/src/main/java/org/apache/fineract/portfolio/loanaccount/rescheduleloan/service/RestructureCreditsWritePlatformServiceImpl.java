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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.JsonParserHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanSummary;
import org.apache.fineract.portfolio.loanaccount.exception.LoanNotFoundException;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.RescheduleCreditsDataValidator;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.RestructureCreditStatus;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.RestructureCreditsLoanMapping;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.RestructureCreditsRequest;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.RestructureCreditsRequestRepository;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.exception.NoSelectedLoansFoundException;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.exception.RestructureCreditPendingApprovalException;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.exception.RestructureRequestNotFoundException;
import org.apache.fineract.portfolio.loanaccount.service.LoanApplicationWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformService;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentType;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentTypeRepositoryWrapper;
import org.apache.fineract.portfolio.products.exception.ProductNotFoundException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestructureCreditsWritePlatformServiceImpl implements RestructureCreditsWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(RestructureCreditsWritePlatformServiceImpl.class);
    private final JdbcTemplate jdbcTemplate;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final RescheduleCreditsDataValidator rescheduleCreditsDataValidator;
    private final LoanProductRepository loanProductRepository;
    private final LoanAssembler loanAssembler;
    private final PlatformSecurityContext platformSecurityContext;
    private final RestructureCreditsRequestRepository restructureCreditsRequestRepository;
    private final LoanWritePlatformService loanWritePlatformService;
    private final LoanApplicationWritePlatformService loanApplicationWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final PaymentTypeRepositoryWrapper paymentTypeRepositoryWrapper;
    private final LoanRepositoryWrapper loanRepositoryWrapper;

    /**
     * LoanRescheduleRequestWritePlatformServiceImpl constructor
     *
     *
     **/
    @Autowired
    public RestructureCreditsWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate,
            final LoanApplicationWritePlatformService loanApplicationWritePlatformService,
            final PlatformSecurityContext platformSecurityContext, final RescheduleCreditsDataValidator rescheduleCreditsDataValidator,
            final LoanProductRepository loanProductRepository, final FromJsonHelper fromApiJsonHelper, final LoanAssembler loanAssembler,
            final LoanWritePlatformService loanWritePlatformService,
            final RestructureCreditsRequestRepository restructureCreditsRequestRepository,
            final ClientRepositoryWrapper clientRepositoryWrapper, final PaymentTypeRepositoryWrapper paymentTypeRepositoryWrapper,
            final LoanRepositoryWrapper loanRepositoryWrapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.clientRepositoryWrapper = clientRepositoryWrapper;
        this.rescheduleCreditsDataValidator = rescheduleCreditsDataValidator;
        this.loanProductRepository = loanProductRepository;
        this.loanAssembler = loanAssembler;
        this.platformSecurityContext = platformSecurityContext;
        this.restructureCreditsRequestRepository = restructureCreditsRequestRepository;
        this.loanWritePlatformService = loanWritePlatformService;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.loanApplicationWritePlatformService = loanApplicationWritePlatformService;
        this.paymentTypeRepositoryWrapper = paymentTypeRepositoryWrapper;
        this.loanRepositoryWrapper = loanRepositoryWrapper;
    }

    @Override
    public CommandProcessingResult create(JsonCommand jsonCommand) {
        Long clientId = jsonCommand.getClientId();
        Client client = clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        this.rescheduleCreditsDataValidator.validateForCreateAction(jsonCommand);
        String countsql = "select coalesce((select id from m_restructure_credit_requests where client_id = ? and status=? limit 1),0)";
        Long pendingApproval = this.jdbcTemplate.queryForObject(countsql, Long.class, clientId, RestructureCreditStatus.PENDING.getValue());
        if (pendingApproval > 0) {
            throw new RestructureCreditPendingApprovalException(pendingApproval);
        }

        final Long productId = jsonCommand.longValueOfParameterNamed("productId");
        final Long prequalificationId = jsonCommand.longValueOfParameterNamed("prequalificationId");
        final BigDecimal totalRequestedAmount = jsonCommand.bigDecimalValueOfParameterNamed("totalRequestedAmount");
        Optional<LoanProduct> loanProducts = this.loanProductRepository.findById(productId);
        if (loanProducts.isEmpty()) throw new ProductNotFoundException(productId, "loan");
        String disbursementDateString = jsonCommand.stringValueOfParameterNamed("disbursementDate");
        String dateFormat = jsonCommand.stringValueOfParameterNamed("dateFormat");
        Locale clientApplicationLocale = jsonCommand.extractLocale();
        final DateTimeFormatter simpleDateFormat = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient()
                .appendPattern(dateFormat).toFormatter(clientApplicationLocale);
        LocalDateTime disbursementDate = LocalDateTime.parse(disbursementDateString, simpleDateFormat);

        List<Loan> loanAccounts = resolveLoanAccounts(jsonCommand.arrayValueOfParameterNamed("selectedLoanIds"));
        if (loanAccounts.isEmpty()) throw new NoSelectedLoansFoundException();

        String comments = jsonCommand.stringValueOfParameterNamed("comments");
        BigDecimal totalOutstanding = getTotalOutstanding(loanAccounts);

        AppUser appUser = this.platformSecurityContext.authenticatedUser();
        LocalDateTime localDateTimeOfSystem = DateUtils.getLocalDateTimeOfSystem();
        BigDecimal extensionAmount = totalRequestedAmount.subtract(totalOutstanding);
        RestructureCreditsRequest request = RestructureCreditsRequest.fromJSON(client, RestructureCreditStatus.PENDING.getValue(),
                loanProducts.get(), totalRequestedAmount, disbursementDate, comments, localDateTimeOfSystem, appUser, prequalificationId,
                extensionAmount);
        restructureCreditsRequestRepository.save(request);
        List<RestructureCreditsLoanMapping> mappings = createRestructureMappings(loanAccounts, request);
        request.updateMappings(mappings);
        restructureCreditsRequestRepository.save(request);
        return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).withEntityId(request.getId())
                .withClientId(clientId).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult approve(JsonCommand command) {
        Long requestId = command.longValueOfParameterNamed("requestId");
        RestructureCreditsRequest request = restructureCreditsRequestRepository.findById(requestId)
                .orElseThrow(() -> new RestructureRequestNotFoundException(requestId));
        List<RestructureCreditsLoanMapping> creditMappings = request.getCreditMappings();
        processLoanClosures(creditMappings, command, request);

        Long loanId = openNewLoanAccount(request, command);
        AppUser appUser = this.platformSecurityContext.authenticatedUser();
        request.approve(appUser, DateUtils.getLocalDateTimeOfSystem());
        creditMappings.forEach(mapping -> {
            Long mappingId = mapping.getId();
            jdbcTemplate.update("update m_restructure_credits_loans_mapping set new_loan_id=? where id=?", loanId, mappingId);
        });
        restructureCreditsRequestRepository.save(request);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(request.getId()).withLoanId(loanId)
                .build();
    }

    @Override
    public CommandProcessingResult reject(JsonCommand command) {
        Long requestId = command.longValueOfParameterNamed("requestId");
        RestructureCreditsRequest request = restructureCreditsRequestRepository.findById(requestId)
                .orElseThrow(() -> new RestructureRequestNotFoundException(requestId));
        AppUser appUser = this.platformSecurityContext.authenticatedUser();
        request.modify(appUser, DateUtils.getLocalDateTimeOfSystem());
        restructureCreditsRequestRepository.save(request);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(request.getId()).build();
    }

    private Long openNewLoanAccount(RestructureCreditsRequest request, JsonCommand command) {
        JsonElement loanDataElelement = command.jsonElement("loanData");
        JsonObject loanObject = loanDataElelement.getAsJsonObject();

        String dateFormat = command.stringValueOfParameterNamed("dateFormat");
        Locale clientApplicationLocale = command.extractLocale();
        final DateTimeFormatter simpleDateFormat = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient()
                .appendPattern(dateFormat).toFormatter(clientApplicationLocale);

        String disbursementDate = request.getNewDisbursementDate().toLocalDate().format(simpleDateFormat);
        JsonElement parse = this.fromApiJsonHelper.parse(this.fromApiJsonHelper.getGsonConverter().toJson(disbursementDate));
        loanObject.addProperty("prequalificationId", request.getPrequalificationId());
        loanObject.add("expectedDisbursementDate", parse);
        loanObject.add("submittedOnDate", parse);
        loanObject.add("principal", this.fromApiJsonHelper.parse(request.getTotalLoanAmount().toPlainString()));
        loanObject.add("locale", command.jsonElement("locale"));
        loanObject.add("dateFormat", command.jsonElement("dateFormat"));
        loanObject.addProperty("isRestructuredLoan", true);
        loanObject.addProperty("restructuredFromLoanId", request.getId());
        JsonElement finalCommand = this.fromApiJsonHelper.parse(loanObject.toString());

        JsonCommand jsonCommand = JsonCommand.fromExistingCommand(command, finalCommand);
        CommandProcessingResult commandProcessingResult = this.loanApplicationWritePlatformService.submitApplication(jsonCommand);
        return commandProcessingResult.getLoanId();
    }

    private void processLoanClosures(List<RestructureCreditsLoanMapping> creditMappings, JsonCommand command,
            RestructureCreditsRequest restructureRequest) {
        JsonObject closeObject = new JsonObject();
        closeObject.add("actualDisbursementDate", command.jsonElement("transactionDate"));
        closeObject.add("locale", command.jsonElement("locale"));
        closeObject.add("note", command.jsonElement("notes"));
        closeObject.add("dateFormat", command.jsonElement("dateFormat"));
        LocalDate disbursementDate = restructureRequest.getNewDisbursementDate().toLocalDate();
        BigDecimal totalLoanAmount = restructureRequest.getTotalLoanAmount();

        Collection<PaymentType> allPaymentTypes = this.paymentTypeRepositoryWrapper.findAllPaymentTypes();
        Long paymentTypeId = 1L;
        if (!allPaymentTypes.isEmpty()) {
            Optional<PaymentType> firstPaymentType = allPaymentTypes.stream().findFirst();
            if (firstPaymentType.isPresent()) {
                paymentTypeId = firstPaymentType.get().getId();
            }
        }
        closeObject.addProperty("paymentTypeId", paymentTypeId);

        for (RestructureCreditsLoanMapping mapping : creditMappings) {
            JsonElement finalCommand = this.fromApiJsonHelper.parse(closeObject.toString());

            JsonCommand jsonCommand = JsonCommand.fromExistingCommand(command, finalCommand);
            Loan loan = mapping.getLoan();
            Long loanIdToClose = loan.getId();

            Loan loanToClose = this.loanRepositoryWrapper.findNonClosedLoanThatBelongsToClient(loanIdToClose, loan.getClientId());
            if (loanToClose == null) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.to.be.closed.with.topup.is.not.active",
                        "Loan to be closed with this topup is not active.");
            }

            LoanSummary summary = loanToClose.getSummary();
            final LoanRepaymentScheduleInstallment foreCloseDetail = loanToClose.fetchLoanForeclosureDetail(disbursementDate);
            MonetaryCurrency currency = loanToClose.getCurrency();
            Money chargedInterest = foreCloseDetail.getInterestCharged(currency);
            BigDecimal futureInterest = loanToClose.getSummary().getTotalInterestOutstanding().subtract(chargedInterest.getAmount());

            if (chargedInterest.isGreaterThanZero()) {
                if (chargedInterest.isGreaterThanZero()) {
                    final String localeAsString = "en";
                    final String dateFormat = "dd MMMM yyyy";
                    final JsonObject jsonObject = new JsonObject();
                    final LocalDate localDate = DateUtils.getBusinessLocalDate();
                    Locale locale = JsonParserHelper.localeFromString(localeAsString);
                    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat).withLocale(locale);
                    final String localDateString = localDate.format(dateTimeFormatter);
                    jsonObject.addProperty("locale", localeAsString);
                    jsonObject.addProperty("dateFormat", dateFormat);
                    jsonObject.addProperty("transactionAmount", chargedInterest.getAmount());
                    jsonObject.addProperty("transactionDate", localDateString);
                    jsonObject.addProperty("postAccountingForWaivers", true);
                    final String note = "Restructure Request " + restructureRequest.getId();
                    jsonObject.addProperty("note", note);
                    final JsonCommand waiveInterestJsonCommand = JsonCommand.from(jsonObject.toString(), jsonObject, this.fromApiJsonHelper,
                            null, loanIdToClose, null, null, null, loanIdToClose, null, null, null, null, null, null);
                    final CommandProcessingResult waiveInterestResult = this.loanWritePlatformService.waiveInterestOnLoan(loanIdToClose,
                            waiveInterestJsonCommand);
                    if (waiveInterestResult.getLoanId() == null) {
                        throw new GeneralPlatformDomainRuleException("error.message.loan.failed.to.waive.interest.on.loan",
                                "Failed to waive interest on loan application " + loanIdToClose);
                    }
                }
            }

            if (futureInterest.compareTo(BigDecimal.ZERO) > 0) {
                final Money waiveInterestTransactionAmount = Money.of(currency, futureInterest);
                if (waiveInterestTransactionAmount.isGreaterThanZero()) {
                    final String localeAsString = "en";
                    final String dateFormat = "dd MMMM yyyy";
                    final JsonObject jsonObject = new JsonObject();
                    final LocalDate localDate = DateUtils.getBusinessLocalDate();
                    Locale locale = JsonParserHelper.localeFromString(localeAsString);
                    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat).withLocale(locale);
                    final String localDateString = localDate.format(dateTimeFormatter);
                    jsonObject.addProperty("locale", localeAsString);
                    jsonObject.addProperty("dateFormat", dateFormat);
                    jsonObject.addProperty("transactionAmount", futureInterest);
                    jsonObject.addProperty("transactionDate", localDateString);
                    jsonObject.addProperty("postAccountingForWaivers", false);
                    final String note = "Préstamo complementario " + restructureRequest.getId();
                    jsonObject.addProperty("note", note);
                    final JsonCommand waiveInterestJsonCommand = JsonCommand.from(jsonObject.toString(), jsonObject, this.fromApiJsonHelper,
                            null, loanIdToClose, null, null, null, loanIdToClose, null, null, null, null, null, null);
                    final CommandProcessingResult waiveInterestResult = this.loanWritePlatformService.waiveInterestOnLoan(loanIdToClose,
                            waiveInterestJsonCommand);
                    if (waiveInterestResult.getLoanId() == null) {
                        throw new GeneralPlatformDomainRuleException("error.message.loan.failed.to.waive.interest.on.loan",
                                "Failed to waive interest on loan application " + loanIdToClose);
                    }
                }
            }

            final BigDecimal principalOutstanding = summary.getTotalPrincipalOutstanding();
            BigDecimal totalFeeChargesOutstanding = summary.getTotalFeeChargesOutstanding();
            BigDecimal totalPenaltyChargesOutstanding = summary.getTotalPenaltyChargesOutstanding();

            BigDecimal totalLoanOutStanding = principalOutstanding.add(totalFeeChargesOutstanding).add(totalPenaltyChargesOutstanding);

            if (totalLoanOutStanding.compareTo(totalLoanAmount) > 0) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.amount.less.than.outstanding.of.loan.to.be.closed",
                        "Topup loan amount should be greater than outstanding amount of loan to be closed.");
            }
            JsonCommand paymentCommand = JsonCommand.fromExistingCommand(command, finalCommand);

            this.loanWritePlatformService.makeLoanRestructureRepayment(paymentCommand, totalLoanOutStanding, loanToClose);

            totalLoanAmount = totalLoanAmount.subtract(totalLoanOutStanding);
        }
    }

    private List<RestructureCreditsLoanMapping> createRestructureMappings(List<Loan> loanAccounts, RestructureCreditsRequest request) {
        List<RestructureCreditsLoanMapping> mappings = new ArrayList<>();
        for (Loan loan : loanAccounts) {
            RestructureCreditsLoanMapping creditsLoanMapping = RestructureCreditsLoanMapping.instance(loan,
                    RestructureCreditStatus.PENDING.getValue(), request);
            mappings.add(creditsLoanMapping);
        }
        return mappings;
    }

    private BigDecimal getTotalOutstanding(List<Loan> loanAccounts) {
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        for (Loan loan : loanAccounts) {
            totalOutstanding = totalOutstanding.add(loan.getSummary().getTotalPrincipalOutstanding()
                    .add(loan.getSummary().getTotalFeeChargesOutstanding().add(loan.getSummary().getTotalPenaltyChargesOutstanding())));
        }
        return totalOutstanding;
    }

    private List<Loan> resolveLoanAccounts(String[] selectedLoanIds) {
        List<Loan> selectedLoans = new ArrayList<>();
        for (String loanId : selectedLoanIds) {

            long loanIdLong = Long.valueOf(loanId);
            Loan loan = this.loanAssembler.assembleFrom(loanIdLong);

            if (loan == null) throw new LoanNotFoundException(loanIdLong);
            selectedLoans.add(loan);
        }

        return selectedLoans;
    }
}
