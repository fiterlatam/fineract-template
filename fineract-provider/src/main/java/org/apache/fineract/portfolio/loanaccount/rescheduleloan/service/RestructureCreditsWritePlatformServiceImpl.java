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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
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
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.products.exception.ProductNotFoundException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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



    /**
     * LoanRescheduleRequestWritePlatformServiceImpl constructor
     *
     *
     **/
    @Autowired
    public RestructureCreditsWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate,
                                                      final PlatformSecurityContext platformSecurityContext,
                                                      final RescheduleCreditsDataValidator rescheduleCreditsDataValidator,
                                                      final LoanProductRepository loanProductRepository,
                                                      final LoanAssembler loanAssembler,
                                                      final RestructureCreditsRequestRepository restructureCreditsRequestRepository,
                                                      final ClientRepositoryWrapper clientRepositoryWrapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.clientRepositoryWrapper = clientRepositoryWrapper;
        this.rescheduleCreditsDataValidator = rescheduleCreditsDataValidator;
        this.loanProductRepository = loanProductRepository;
        this.loanAssembler = loanAssembler;
        this.platformSecurityContext = platformSecurityContext;
        this.restructureCreditsRequestRepository = restructureCreditsRequestRepository;
    }

    @Override
    public CommandProcessingResult create(JsonCommand jsonCommand) {
        Long clientId = jsonCommand.getClientId();
        Client client = clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        this.rescheduleCreditsDataValidator.validateForCreateAction(jsonCommand);
        String countsql = "select id from m_restructure_credit_requests where client_id = ? and status=? limit 1";
        Long pendingApproval = this.jdbcTemplate.queryForObject(countsql, Long.class, clientId, RestructureCreditStatus.PENDING.getValue());
        if(pendingApproval>0){
            throw new RestructureCreditPendingApprovalException(pendingApproval);
        }


        final Long productId = jsonCommand.longValueOfParameterNamed("productId");
        Optional<LoanProduct> loanProducts = this.loanProductRepository.findById(productId);
        if (loanProducts.isEmpty()) throw new ProductNotFoundException(productId,"loan");
        LocalDateTime disbursementDate = jsonCommand.localDateTimeValueOfParameterNamed("disbursementDate");

        List<Loan> loanAccounts = resolveLoanProducts(jsonCommand.arrayOfParameterNamed("selectedLoanIds"));
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
        restructureCreditsRequestRepository.saveAndFlush(request);
        return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).
                withEntityId(request.getId()).
                withClientId(clientId).build();
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

    private List<Loan> resolveLoanProducts(JsonArray selectedLoanIds) {
        JsonElement jsonElement = selectedLoanIds.get(0);
        List<Loan> selectedLoans= new ArrayList<>();
        for (JsonElement loanId: selectedLoanIds){

            long loanIdLong = loanId.getAsLong();
            Loan loan = this.loanAssembler.assembleFrom(loanIdLong);

            if (loan==null) throw new LoanNotFoundException(loanIdLong);
            selectedLoans.add(loan);
        }

        return selectedLoans;
    }
}
