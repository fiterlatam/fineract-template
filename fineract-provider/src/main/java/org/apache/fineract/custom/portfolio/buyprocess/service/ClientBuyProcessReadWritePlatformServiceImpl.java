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
package org.apache.fineract.custom.portfolio.buyprocess.service;

import com.google.gson.GsonBuilder;
import jakarta.persistence.PersistenceException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.infrastructure.core.service.CustomDateUtils;
import org.apache.fineract.custom.portfolio.buyprocess.data.ApproveLoanPayloadData;
import org.apache.fineract.custom.portfolio.buyprocess.data.ClientBuyProcessData;
import org.apache.fineract.custom.portfolio.buyprocess.data.CreateLoanPayloadData;
import org.apache.fineract.custom.portfolio.buyprocess.data.DisburseLoanPayloadData;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcessRepository;
import org.apache.fineract.custom.portfolio.buyprocess.exception.ClientBuyProcessNotCompletedException;
import org.apache.fineract.custom.portfolio.buyprocess.exception.ClientBuyProcessNotFoundException;
import org.apache.fineract.custom.portfolio.buyprocess.mapper.ClientBuyProcessMapper;
import org.apache.fineract.custom.portfolio.buyprocess.validator.ClientBuyProcessDataValidator;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ClientBuyProcessReadWritePlatformServiceImpl implements ClientBuyProcessReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final ClientBuyProcessDataValidator validatorClass;
    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public ClientBuyProcessReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate, final DatabaseSpecificSQLGenerator sqlGenerator,
            final ClientBuyProcessDataValidator validatorClass, final PlatformSecurityContext context,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.validatorClass = validatorClass;
        this.context = context;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @Autowired
    private ClientBuyProcessRepository repository;

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Override
    public List<ClientBuyProcessData> findAllActive() {
        return ClientBuyProcessMapper.toDTO(repository.findAll());
    }

    @Override
    public ClientBuyProcessData findById(Long id) {
        Optional<ClientBuyProcess> entity = repository.findById(id);
        if (!entity.isPresent()) {
            throw new ClientBuyProcessNotFoundException();
        }
        return ClientBuyProcessMapper.toDTO(entity.get());
    }

    @Override
    public CommandProcessingResult create(final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            final ClientBuyProcess entity = this.validatorClass.validateForCreate(command.json(), repository);

            // Create Loan and disburse
            createApproveAndDisburseLoan(entity);

            repository.saveAndFlush(entity);

            return new CommandProcessingResultBuilder().withEntityId(entity.getId()).withLoanId(entity.getLoanId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    private void createApproveAndDisburseLoan(final ClientBuyProcess entity) {

        // Get data from product
        Optional<LoanProduct> entityOpt = loanProductRepository.findById(entity.getProductId());
        if (entityOpt.isPresent()) {

            LoanProduct prodiuctEntity = entityOpt.get();

            // Build create loan dto
            createLoanApplication(entity, prodiuctEntity);

            // Approve loan
            approveLoanApplication(entity, prodiuctEntity);

            // disburse loan
            disburseLoanApplication(entity, prodiuctEntity);

            log.info("Loan created and disbursed");
        } else {
            throw new ClientBuyProcessNotCompletedException();
        }
    }

    private void disburseLoanApplication(ClientBuyProcess entity, LoanProduct prodiuctEntity) {

        DisburseLoanPayloadData payloadData = DisburseLoanPayloadData.builder()
                .actualDisbursementDate(DateUtils.format(entity.getRequestedDate(), CustomDateUtils.SPANISH_DATE_FORMAT))
                .transactionAmount(entity.getAmount()).locale("en").dateFormat(CustomDateUtils.SPANISH_DATE_FORMAT).build();

        // Execute create loan command
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(CustomDateUtils.SPANISH_DATE_FORMAT));

        String payload = gsonBuilder.create().toJson(payloadData);
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .disburseLoanApplication(entity.getLoanId()).withJson(payload) //
                .build(); //
        CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private void approveLoanApplication(ClientBuyProcess entity, LoanProduct prodiuctEntity) {

        ApproveLoanPayloadData payloadData = ApproveLoanPayloadData.builder()
                .approvedOnDate(DateUtils.format(entity.getRequestedDate(), CustomDateUtils.SPANISH_DATE_FORMAT))
                .expectedDisbursementDate(DateUtils.format(entity.getRequestedDate(), CustomDateUtils.SPANISH_DATE_FORMAT))
                .approvedLoanAmount(entity.getAmount()).dateFormat(CustomDateUtils.SPANISH_DATE_FORMAT).locale("en").build();

        // Execute create loan command
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(CustomDateUtils.SPANISH_DATE_FORMAT));

        String payload = gsonBuilder.create().toJson(payloadData);
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .approveLoanApplication(entity.getLoanId()).withJson(payload) //
                .build(); //
        CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private void createLoanApplication(ClientBuyProcess entity, LoanProduct prodiuctEntity) {

        CreateLoanPayloadData payloadData = CreateLoanPayloadData.builder().productId(entity.getProductId())
                .submittedOnDate(DateUtils.format(entity.getRequestedDate(), CustomDateUtils.SPANISH_DATE_FORMAT))
                .expectedDisbursementDate(DateUtils.format(entity.getRequestedDate(), CustomDateUtils.SPANISH_DATE_FORMAT))
                .loanTermFrequency(entity.getTerm())
                .loanTermFrequencyType(prodiuctEntity.getLoanProductRelatedDetail().getRepaymentPeriodFrequencyType().getValue()) // From
                                                                                                                                  // product
                .numberOfRepayments(entity.getTerm()).repaymentEvery(prodiuctEntity.getLoanProductRelatedDetail().getRepayEvery()) // From
                                                                                                                                   // product
                .repaymentFrequencyType(prodiuctEntity.getLoanProductRelatedDetail().getRepaymentPeriodFrequencyType().getValue()) // From
                                                                                                                                   // product
                .interestRatePerPeriod(prodiuctEntity.getLoanProductRelatedDetail().getNominalInterestRatePerPeriod()) // From
                                                                                                                       // product
                .interestType(prodiuctEntity.getLoanProductRelatedDetail().getInterestMethod().getValue()) // From
                                                                                                           // product
                .amortizationType(prodiuctEntity.getLoanProductRelatedDetail().getAmortizationMethod().getValue()) // From
                                                                                                                   // product
                .interestCalculationPeriodType(prodiuctEntity.getLoanProductRelatedDetail().getInterestCalculationPeriodMethod().getValue()) // From
                                                                                                                                             // product
                .transactionProcessingStrategyCode(prodiuctEntity.getTransactionProcessingStrategyCode()).charges(Collections.emptyList())
                .collateral(Collections.emptyList()).dateFormat(CustomDateUtils.SPANISH_DATE_FORMAT).locale("es")
                .clientId(entity.getClientId()).loanType("individual").principal(entity.getAmount()).build();

        // Execute create loan command
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(CustomDateUtils.SPANISH_DATE_FORMAT));

        String payload = gsonBuilder.create().toJson(payloadData);
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createLoanApplication().withJson(payload) //
                .build(); //
        CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

        // Set Loan ID
        entity.setLoanId(result.getLoanId());
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        throw new PlatformDataIntegrityException("error.msg.clientbuyprocess.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource." + dve.getMessage());
    }
}
