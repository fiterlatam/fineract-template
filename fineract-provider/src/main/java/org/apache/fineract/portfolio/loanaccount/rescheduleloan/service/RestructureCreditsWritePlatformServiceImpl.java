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
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
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
import org.apache.fineract.portfolio.products.exception.ProductNotFoundException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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



    /**
     * LoanRescheduleRequestWritePlatformServiceImpl constructor
     *
     *
     **/
    @Autowired
    public RestructureCreditsWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate,final LoanApplicationWritePlatformService loanApplicationWritePlatformService,
                                                      final PlatformSecurityContext platformSecurityContext,
                                                      final RescheduleCreditsDataValidator rescheduleCreditsDataValidator,
                                                      final LoanProductRepository loanProductRepository,final FromJsonHelper fromApiJsonHelper,
                                                      final LoanAssembler loanAssembler,final LoanWritePlatformService loanWritePlatformService,
                                                      final RestructureCreditsRequestRepository restructureCreditsRequestRepository,
                                                      final ClientRepositoryWrapper clientRepositoryWrapper) {
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
    }

    @Override
    public CommandProcessingResult create(JsonCommand jsonCommand) {
        Long clientId = jsonCommand.getClientId();
        Client client = clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        this.rescheduleCreditsDataValidator.validateForCreateAction(jsonCommand);
        String countsql = "select coalesce((select id from m_restructure_credit_requests where client_id = ? and status=? limit 1),0)";
        Long pendingApproval = this.jdbcTemplate.queryForObject(countsql, Long.class, clientId, RestructureCreditStatus.PENDING.getValue());
        if(pendingApproval>0){
            throw new RestructureCreditPendingApprovalException(pendingApproval);
        }


        final Long productId = jsonCommand.longValueOfParameterNamed("productId");
        Optional<LoanProduct> loanProducts = this.loanProductRepository.findById(productId);
        if (loanProducts.isEmpty()) throw new ProductNotFoundException(productId,"loan");
        String disbursementDateString = jsonCommand.stringValueOfParameterNamed("disbursementDate");
        String dateFormat = jsonCommand.stringValueOfParameterNamed("dateFormat");
        final DateTimeFormatter simpleDateFormat = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient()
                .appendPattern(dateFormat).toFormatter();
        LocalDateTime disbursementDate = LocalDateTime.parse(disbursementDateString, simpleDateFormat);

        List<Loan> loanAccounts = resolveLoanAccounts(jsonCommand.arrayValueOfParameterNamed("selectedLoanIds"));
        if (loanAccounts.isEmpty()) throw new NoSelectedLoansFoundException();

        String comments = jsonCommand.stringValueOfParameterNamed("comments");
        BigDecimal totalOutstanding = getTotalOutstanding(loanAccounts);
        AppUser appUser = this.platformSecurityContext.authenticatedUser();
        LocalDateTime localDateTimeOfSystem = DateUtils.getLocalDateTimeOfSystem();
        RestructureCreditsRequest request = RestructureCreditsRequest.fromJSON(client, RestructureCreditStatus.PENDING.getValue(),
                loanProducts.get(), totalOutstanding, disbursementDate, comments, localDateTimeOfSystem, appUser);
        restructureCreditsRequestRepository.save(request);
        List<RestructureCreditsLoanMapping> mappings = createRestructureMappings(loanAccounts,request);
        request.updateMappings(mappings);
        restructureCreditsRequestRepository.save(request);
        return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).
                withEntityId(request.getId()).
                withClientId(clientId).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult approve(JsonCommand command) {
        Long requestId = command.longValueOfParameterNamed("requestId");
        RestructureCreditsRequest request = restructureCreditsRequestRepository.findById(requestId).orElseThrow(() ->
                new RestructureRequestNotFoundException(requestId));
        List<RestructureCreditsLoanMapping> creditMappings = request.getCreditMappings();
        processLoanClosures(creditMappings, command);
        Long loanId = openNewLoanAccount(request,command);
        AppUser appUser = this.platformSecurityContext.authenticatedUser();
        request.approve(appUser, DateUtils.getLocalDateTimeOfSystem());
        restructureCreditsRequestRepository.save(request);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).
                withEntityId(request.getId()).
                withLoanId(loanId).build();
    }

    @Override
    public CommandProcessingResult reject(JsonCommand command) {
        Long requestId = command.longValueOfParameterNamed("requestId");
        RestructureCreditsRequest request = restructureCreditsRequestRepository.findById(requestId).orElseThrow(() ->
                new RestructureRequestNotFoundException(requestId));
        AppUser appUser = this.platformSecurityContext.authenticatedUser();
        request.modify(appUser, DateUtils.getLocalDateTimeOfSystem());
        restructureCreditsRequestRepository.save(request);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).
                withEntityId(request.getId()).build();
    }

    private Long openNewLoanAccount(RestructureCreditsRequest request, JsonCommand command) {
        JsonElement loanDataElelement = command.jsonElement("loanData");
        JsonObject loanObject = loanDataElelement.getAsJsonObject();

        String dateFormat = command.stringValueOfParameterNamed("dateFormat");
        final DateTimeFormatter simpleDateFormat = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient()
                .appendPattern(dateFormat).toFormatter();

        String disbursementDate = request.getNewDisbursementDate().toLocalDate().format(simpleDateFormat);
        JsonElement parse = this.fromApiJsonHelper.parse(this.fromApiJsonHelper.getGsonConverter().toJson(disbursementDate));
        loanObject.add("expectedDisbursementDate", parse);
        loanObject.add("submittedOnDate", parse);
        loanObject.add("principal", this.fromApiJsonHelper.parse(request.getTotalLoanAmount().toPlainString()));
        loanObject.add("locale",command.jsonElement("locale"));
        loanObject.add("dateFormat",command.jsonElement("dateFormat"));
        JsonElement finalCommand = this.fromApiJsonHelper.parse(loanObject.toString());

        JsonCommand jsonCommand = JsonCommand.fromExistingCommand(command, finalCommand);
        CommandProcessingResult commandProcessingResult = this.loanApplicationWritePlatformService.submitApplication(jsonCommand);
        return commandProcessingResult.getLoanId();
    }

    private void processLoanClosures(List<RestructureCreditsLoanMapping> creditMappings, JsonCommand command) {
        JsonObject closeObject  = new JsonObject();
        closeObject.add("transactionDate",command.jsonElement("transactionDate"));
        closeObject.add("locale",command.jsonElement("locale"));
        closeObject.add("note",command.jsonElement("notes"));
        closeObject.add("dateFormat",command.jsonElement("dateFormat"));
        JsonElement finalCommand = this.fromApiJsonHelper.parse(closeObject.toString());

        JsonCommand jsonCommand = JsonCommand.fromExistingCommand(command, finalCommand);
        for (RestructureCreditsLoanMapping mapping :
                creditMappings) {
            Loan loan = mapping.getLoan();
            loanWritePlatformService.closeAsRescheduled(loan.getId(), jsonCommand);
        }
    }

    private List<RestructureCreditsLoanMapping> createRestructureMappings(List<Loan> loanAccounts, RestructureCreditsRequest request) {
        List<RestructureCreditsLoanMapping> mappings = new ArrayList<>();
        for (Loan loan :
                loanAccounts) {
            RestructureCreditsLoanMapping creditsLoanMapping = RestructureCreditsLoanMapping.instance(loan,RestructureCreditStatus.PENDING.getValue(),request);
            mappings.add(creditsLoanMapping);
        }
        return mappings;
    }

    private BigDecimal getTotalOutstanding(List<Loan> loanAccounts) {
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        for (Loan loan :
                loanAccounts) {
            totalOutstanding = totalOutstanding.add(loan.getSummary().getTotalOutstanding());
        }
        return totalOutstanding;
    }

    private List<Loan> resolveLoanAccounts(String[] selectedLoanIds) {
        List<Loan> selectedLoans= new ArrayList<>();
        for (String loanId: selectedLoanIds){

            long loanIdLong = Long.valueOf(loanId);
            Loan loan = this.loanAssembler.assembleFrom(loanIdLong);

            if (loan==null) throw new LoanNotFoundException(loanIdLong);
            selectedLoans.add(loan);
        }

        return selectedLoans;
    }
}
