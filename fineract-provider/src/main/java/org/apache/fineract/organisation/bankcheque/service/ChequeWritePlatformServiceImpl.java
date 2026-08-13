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
package org.apache.fineract.organisation.bankcheque.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.campaigns.email.data.EmailMessageWithAttachmentData;
import org.apache.fineract.infrastructure.campaigns.email.service.EmailMessageJobEmailService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.JsonParserHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.documentmanagement.contentrepository.FileSystemContentRepository;
import org.apache.fineract.infrastructure.report.provider.ReportingProcessServiceProvider;
import org.apache.fineract.infrastructure.report.service.ReportingProcessService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.agency.domain.Agency;
import org.apache.fineract.organisation.bankAccount.domain.BankAccount;
import org.apache.fineract.organisation.bankAccount.domain.BankAccountRepositoryWrapper;
import org.apache.fineract.organisation.bankcheque.api.BankChequeApiConstants;
import org.apache.fineract.organisation.bankcheque.command.ApproveChequeIssuanceCommand;
import org.apache.fineract.organisation.bankcheque.command.AuthorizeChequeIssuanceCommand;
import org.apache.fineract.organisation.bankcheque.command.CreateChequeCommand;
import org.apache.fineract.organisation.bankcheque.command.PayGuaranteeByChequeCommand;
import org.apache.fineract.organisation.bankcheque.command.PrintChequeCommand;
import org.apache.fineract.organisation.bankcheque.command.ReassignChequeCommand;
import org.apache.fineract.organisation.bankcheque.command.UpdateChequeCommand;
import org.apache.fineract.organisation.bankcheque.command.VoidChequeCommand;
import org.apache.fineract.organisation.bankcheque.data.ChequeData;
import org.apache.fineract.organisation.bankcheque.domain.BankChequeStatus;
import org.apache.fineract.organisation.bankcheque.domain.Batch;
import org.apache.fineract.organisation.bankcheque.domain.BatchChequeRequest;
import org.apache.fineract.organisation.bankcheque.domain.BatchChequeRequestRepository;
import org.apache.fineract.organisation.bankcheque.domain.BatchChequeRequestStatus;
import org.apache.fineract.organisation.bankcheque.domain.Cheque;
import org.apache.fineract.organisation.bankcheque.domain.ChequeBatchRepositoryWrapper;
import org.apache.fineract.organisation.bankcheque.domain.ChequeJpaRepository;
import org.apache.fineract.organisation.bankcheque.event.BatchChequePrintEvent;
import org.apache.fineract.organisation.bankcheque.exception.BankChequeException;
import org.apache.fineract.organisation.bankcheque.serialization.ApproveChequeIssuanceCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.bankcheque.serialization.AuthorizeChequeIssuanceCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.bankcheque.serialization.CreateChequeCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.bankcheque.serialization.PayGuaranteeByChequeCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.bankcheque.serialization.PrintChequeCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.bankcheque.serialization.ReassignChequeCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.bankcheque.serialization.UpdateChequeCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.bankcheque.serialization.VoidChequeCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.monetary.domain.NumberToWordsConverter;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformService;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.paymentdetail.service.PaymentDetailWritePlatformService;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.apache.fineract.portfolio.paymenttype.service.PaymentTypeReadPlatformService;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountDomainService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountWritePlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChequeWritePlatformServiceImpl implements ChequeWritePlatformService {

    private final PlatformSecurityContext context;
    private final CreateChequeCommandFromApiJsonDeserializer createChequeCommandFromApiJsonDeserializer;
    private final UpdateChequeCommandFromApiJsonDeserializer updateChequeCommandFromApiJsonDeserializer;
    private final BankAccountRepositoryWrapper bankAccountRepositoryWrapper;
    private final JdbcTemplate jdbcTemplate;
    private final ChequeBatchRepositoryWrapper chequeBatchRepositoryWrapper;
    private final ChequeJpaRepository chequeJpaRepository;
    private final ReassignChequeCommandFromApiJsonDeserializer reassignChequeCommandFromApiJsonDeserializer;
    private final VoidChequeCommandFromApiJsonDeserializer voidChequeCommandFromApiJsonDeserializer;
    private final ApproveChequeIssuanceCommandFromApiJsonDeserializer approveChequeIssuanceCommandFromApiJsonDeserializer;
    private final AuthorizeChequeIssuanceCommandFromApiJsonDeserializer authorizeChequeIssuanceCommandFromApiJsonDeserializer;
    private final PayGuaranteeByChequeCommandFromApiJsonDeserializer payGuaranteeByChequeCommandFromApiJsonDeserializer;
    private final PrintChequeCommandFromApiJsonDeserializer printChequeCommandFromApiJsonDeserializer;
    private final ChequeReadPlatformServiceImpl.ChequeMapper chequeMapper = new ChequeReadPlatformServiceImpl.ChequeMapper();
    private final LoanWritePlatformService loanWritePlatformService;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final ChequeReadPlatformService chequeReadPlatformService;
    private final SavingsAccountReadPlatformService savingsAccountReadPlatformService;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final SavingsAccountWritePlatformService savingsAccountWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final PaymentTypeReadPlatformService paymentTypeReadPlatformService;
    private final SavingsAccountAssembler savingsAccountAssembler;
    private final SavingsAccountDomainService savingsAccountDomainService;
    private final NoteRepository noteRepository;
    private final PaymentDetailWritePlatformService paymentDetailWritePlatformService;
    private final BatchChequeRequestRepository batchChequeRequestRepository;
    private final ReportingProcessServiceProvider reportingProcessServiceProvider;
    private final EmailMessageJobEmailService emailMessageJobEmailService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PlatformTransactionManager transactionManager;

    @Override
    public CommandProcessingResult createBatch(JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        CreateChequeCommand createChequeCommand = this.createChequeCommandFromApiJsonDeserializer.commandFromApiJson(command.json());
        final Long bankAccId = createChequeCommand.getBankAccId();
        final Long from = createChequeCommand.getFrom();
        final Long to = createChequeCommand.getTo();
        BankAccount bankAccount = this.bankAccountRepositoryWrapper.findOneWithNotFoundDetection(bankAccId);
        Agency agency = bankAccount.getAgency();
        String maxBatchNoSql = "SELECT IFNULL(MAX(mpb.batch_no), 0) AS maxBatchNo FROM m_payment_batch mpb WHERE mpb.bank_acc_id = ?";
        final Long maxBatchNo = this.jdbcTemplate.queryForObject(maxBatchNoSql, Long.class, new Object[] { bankAccId });
        final Long batchNo = ObjectUtils.defaultIfNull(maxBatchNo, 0L) + 1;
        Batch batch = new Batch().setBatchNo(batchNo).setAgency(agency).setBankAccount(bankAccount)
                .setBankAccNo(bankAccount.getAccountNumber()).setFrom(from).setTo(to).setDescription(createChequeCommand.getDescription());
        final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
        final Long currentUserId = currentUser.getId();
        batch.stampAudit(currentUserId, localDateTime);
        this.validateToAndFromValues(from, to, bankAccId);
        this.chequeBatchRepositoryWrapper.createBatch(batch);
        Set<Cheque> chequeList = new HashSet<>();
        for (Long i = from; i <= to; i++) {
            Cheque cheque = new Cheque().setBatch(batch).setChequeNo(i).setStatus(BankChequeStatus.AVAILABLE.getValue());
            cheque.stampAudit(currentUserId, localDateTime);
            chequeList.add(cheque);
        }
        batch.setCheques(chequeList);
        this.chequeBatchRepositoryWrapper.updateBatch(batch);
        return new CommandProcessingResultBuilder().withEntityId(batch.getId()).withCommandId(command.commandId())
                .withResourceIdAsString(batch.toString()).build();
    }

    private void validateToAndFromValues(final Long from, final Long to, Long bankAccId) {
        String maxChequeNoSql = """
                SELECT IFNULL(MAX(mbc.check_no), 0) AS maxChequeNo
                FROM m_bank_check mbc
                INNER JOIN m_payment_batch mpb ON mpb.id = mbc.batch_id
                LEFT JOIN m_bank_account mba ON mba.id = mpb.bank_acc_id
                WHERE mba.id = ?
                """;
        final Long maxChequeNo = this.jdbcTemplate.queryForObject(maxChequeNoSql, Long.class, new Object[] { bankAccId });
        Long startValue = ObjectUtils.defaultIfNull(maxChequeNo, 0L) + 1;
        if (!startValue.equals(from)) {
            throw new BankChequeException("from", "from value is not equal to the maximum cheque number.");
        }
        if (from >= to) {
            throw new BankChequeException("to", "to value is less than the from cheque value.");
        }
    }

    @Override
    public CommandProcessingResult updateBatch(Long batchId, JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        UpdateChequeCommand updateChequeCommand = updateChequeCommandFromApiJsonDeserializer.commandFromApiJson(command.json());
        final Batch batchForUpdate = this.chequeBatchRepositoryWrapper.findOneBatchWithNotFoundDetection(batchId);
        final Long bankAccId = batchForUpdate.getBankAccount().getId();
        final Long from = updateChequeCommand.getFrom();
        final Long to = updateChequeCommand.getTo();
        this.validateToAndFromValues(from, to, bankAccId);
        final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
        final Long currentUserId = currentUser.getId();
        Map<String, Object> changes = batchForUpdate.update(command);
        if (!changes.isEmpty()) {
            batchForUpdate.stampAudit(currentUserId, localDateTime);
            Set<Cheque> chequeList = new HashSet<>();
            for (Long i = from; i <= to; i++) {
                Cheque cheque = new Cheque().setBatch(batchForUpdate).setChequeNo(i).setStatus(BankChequeStatus.AVAILABLE.getValue())
                        .setDescription(updateChequeCommand.getDescription());
                cheque.stampAudit(currentUserId, localDateTime);
                chequeList.add(cheque);
            }
            batchForUpdate.setCheques(chequeList);
            this.chequeBatchRepositoryWrapper.updateBatch(batchForUpdate);
        }
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withResourceIdAsString(batchId.toString())
                .with(changes).withEntityId(batchId).build();
    }

    @Override
    public CommandProcessingResult deleteBatch(Long batchId, JsonCommand command) {
        final Batch batchForUpdate = this.chequeBatchRepositoryWrapper.findOneBatchWithNotFoundDetection(batchId);
        this.chequeBatchRepositoryWrapper.deleteBatch(batchForUpdate);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withResourceIdAsString(batchId.toString())
                .withEntityId(batchId).build();
    }

    @Override
    public CommandProcessingResult reassignCheque(final Long chequeId, JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        ReassignChequeCommand reassignChequeCommand = reassignChequeCommandFromApiJsonDeserializer.commandFromApiJson(command.json());
        Cheque newCheque = chequeBatchRepositoryWrapper.findOneChequeWithNotFoundDetection(reassignChequeCommand.getChequeId());
        Cheque oldCheque = chequeBatchRepositoryWrapper.findOneChequeWithNotFoundDetection(reassignChequeCommand.getOldChequeId());
        if (!BankChequeStatus.AVAILABLE.getValue().equals(newCheque.getStatus())) {
            throw new BankChequeException("status", "invalid.loan.status.for.cheque.reassignment");
        }

        final ChequeData chequeData = this.chequeReadPlatformService.retrieveChequeById(oldCheque.getId());
        if (chequeData != null && chequeData.getLoanAccId() != null) {
            final Long loanId = chequeData.getLoanAccId();
            final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
            loan.setCheque(newCheque);
            this.loanRepositoryWrapper.saveAndFlush(loan);
        }
        final String newChequeDescription = "Emitido por sustitución de Desembolso cheque " + oldCheque.getChequeNo();
        final String oldChequeDescription = "Cheque anulado por proceso de Reasignación, cheque nuevo " + newCheque.getChequeNo();
        final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
        LocalDate localDate = DateUtils.getBusinessLocalDate();
        final Long currentUserId = currentUser.getId();
        oldCheque.setStatus(BankChequeStatus.PENDING_VOIDANCE.getValue());
        oldCheque.setDescription(oldChequeDescription);
        oldCheque.stampAudit(currentUserId, localDateTime);
        oldCheque.setVoidedDate(localDate);
        oldCheque.setVoidedBy(currentUser);
        newCheque.setStatus(BankChequeStatus.READY_TO_BE_PRINTED.getValue());
        newCheque.setDescription(newChequeDescription);
        newCheque.stampAudit(currentUserId, localDateTime);
        newCheque.setPrintedDate(localDate);
        newCheque.setPrintedBy(currentUser);
        newCheque.setGuaranteeAmount(oldCheque.getGuaranteeAmount());
        newCheque.setRequiredGuaranteeAmount(oldCheque.getRequiredGuaranteeAmount());
        newCheque.setDepositGuaranteeNo(oldCheque.getDepositGuaranteeNo());
        newCheque.setCaseId(oldCheque.getCaseId());
        newCheque.setGuaranteeId(oldCheque.getGuaranteeId());
        newCheque.setGuaranteeName(oldCheque.getGuaranteeName());
        newCheque.setAmountInWords(oldCheque.getAmountInWords());
        newCheque.setIssuanceApprovedOnDate(oldCheque.getIssuanceApprovedOnDate());
        newCheque.setIssuanceApprovedBy(oldCheque.getIssuanceApprovedBy());
        newCheque.setIssuanceAuthorizeBy(oldCheque.getIssuanceAuthorizeBy());
        newCheque.setIssuanceAuthorizeOnDate(oldCheque.getIssuanceAuthorizeOnDate());
        newCheque.setNumeroCliente(oldCheque.getNumeroCliente());
        newCheque.setIsReassigned(true);
        newCheque.setReassignedFrom(oldCheque.getId());
        this.chequeJpaRepository.saveAll(List.of(oldCheque, newCheque));
        // update journal entries with this cheque number.
        this.jdbcTemplate.update("UPDATE m_payment_detail SET check_number = ? WHERE check_number = ?",
                String.valueOf(newCheque.getChequeNo()), String.valueOf(oldCheque.getChequeNo()));
        return new CommandProcessingResultBuilder().withCommandId(command.commandId())
                .withResourceIdAsString(reassignChequeCommand.getOldChequeId().toString())
                .withEntityId(reassignChequeCommand.getOldChequeId()).build();
    }

    @Override
    public CommandProcessingResult authorizedChequeReassignment(final Long chequeId, JsonCommand command) {
        return new CommandProcessingResultBuilder().withCommandId(command.commandId())
                .withResourceIdAsString(String.valueOf(command.entityId())).withEntityId(command.entityId()).build();
    }

    @Override
    public CommandProcessingResult authorizedChequeVoidance(final Long chequeId, JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        Cheque cheque = chequeBatchRepositoryWrapper.findOneChequeWithNotFoundDetection(chequeId);
        final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
        LocalDate localDate = DateUtils.getBusinessLocalDate();
        final Long currentUserId = currentUser.getId();
        cheque.stampAudit(currentUserId, localDateTime);
        cheque.setVoidAuthorizedDate(localDate);
        cheque.setVoidAuthorizedBy(currentUser);
        cheque.setStatus(BankChequeStatus.VOIDED.getValue());
        final Collection<Loan> loans = this.loanRepositoryWrapper.findLoanByChequeId(chequeId);
        loans.forEach(l -> l.setCheque(null));
        this.loanRepositoryWrapper.save(new ArrayList<>(loans));
        chequeJpaRepository.saveAndFlush(cheque);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId())
                .withResourceIdAsString(String.valueOf(command.entityId())).withEntityId(command.entityId()).build();
    }

    @Override
    @Transactional
    public CommandProcessingResult voidCheque(final Long chequeId, JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        VoidChequeCommand voidChequeCommand = voidChequeCommandFromApiJsonDeserializer.commandFromApiJson(command.json());
        Cheque cheque = chequeBatchRepositoryWrapper.findOneChequeWithNotFoundDetection(chequeId);
        final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
        LocalDate localDate = DateUtils.getBusinessLocalDate();
        final Long currentUserId = currentUser.getId();
        cheque.stampAudit(currentUserId, localDateTime);
        cheque.setVoidedDate(localDate);
        cheque.setVoidedBy(currentUser);
        cheque.setDescription(voidChequeCommand.getDescription());
        cheque.setStatus(BankChequeStatus.PENDING_VOIDANCE.getValue());
        chequeJpaRepository.saveAndFlush(cheque);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId())
                .withResourceIdAsString(String.valueOf(command.entityId())).withEntityId(command.entityId()).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult approveChequeIssuance(JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        List<ApproveChequeIssuanceCommand> approveChequeIssuanceCommands = this.approveChequeIssuanceCommandFromApiJsonDeserializer
                .commandFromApiJson(command.json());
        for (final ApproveChequeIssuanceCommand approveChequeIssuanceCommand : approveChequeIssuanceCommands) {
            final Cheque cheque = this.chequeBatchRepositoryWrapper
                    .findOneChequeWithNotFoundDetection(approveChequeIssuanceCommand.getChequeId());
            cheque.setStatus(BankChequeStatus.PENDING_AUTHORIZATION_BY_ACCOUNTING.getValue());
            if (approveChequeIssuanceCommand.getDescription() != null) {
                cheque.setDescription(approveChequeIssuanceCommand.getDescription());
            }
            final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
            LocalDate localDate = DateUtils.getBusinessLocalDate();
            final Long currentUserId = currentUser.getId();
            cheque.stampAudit(currentUserId, localDateTime);
            cheque.setIssuanceApprovedBy(currentUser);
            cheque.setIssuanceApprovedOnDate(localDate);
            this.chequeBatchRepositoryWrapper.updateCheque(cheque);
        }
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).build();
    }

    @Override
    @Transactional
    public CommandProcessingResult authorizeChequeIssuance(JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        List<AuthorizeChequeIssuanceCommand> authorizeChequeIssuanceCommands = this.authorizeChequeIssuanceCommandFromApiJsonDeserializer
                .commandFromApiJson(command.json());
        for (final AuthorizeChequeIssuanceCommand authorizeChequeIssuanceCommand : authorizeChequeIssuanceCommands) {
            final Cheque cheque = this.chequeBatchRepositoryWrapper
                    .findOneChequeWithNotFoundDetection(authorizeChequeIssuanceCommand.getChequeId());
            if (!BankChequeStatus.PENDING_AUTHORIZATION_BY_ACCOUNTING.getValue().equals(cheque.getStatus())) {
                throw new BankChequeException("status", "invalid.loan.status.for.cheque.authorization");
            }
            cheque.setStatus(BankChequeStatus.READY_TO_BE_PRINTED.getValue());
            if (authorizeChequeIssuanceCommand.getDescription() != null) {
                cheque.setDescription(authorizeChequeIssuanceCommand.getDescription());
            }
            final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
            LocalDate localDate = DateUtils.getBusinessLocalDate();
            final Long currentUserId = currentUser.getId();
            cheque.stampAudit(currentUserId, localDateTime);
            cheque.setIssuanceAuthorizeBy(currentUser);
            cheque.setIssuanceAuthorizeOnDate(localDate);
            this.chequeBatchRepositoryWrapper.updateCheque(cheque);
        }
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).build();
    }

    @Override
    @Transactional
    public CommandProcessingResult payGuaranteeByCheque(JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        List<PayGuaranteeByChequeCommand> payGuaranteeByChequeCommands = this.payGuaranteeByChequeCommandFromApiJsonDeserializer
                .commandFromApiJson(command.json());
        for (final PayGuaranteeByChequeCommand payGuaranteeByChequeCommand : payGuaranteeByChequeCommands) {
            final Cheque cheque = this.chequeBatchRepositoryWrapper
                    .findOneChequeWithNotFoundDetection(payGuaranteeByChequeCommand.getChequeId());
            if (!BankChequeStatus.AVAILABLE.getValue().equals(cheque.getStatus())) {
                throw new BankChequeException("status", "invalid.loan.status.for.pay.guarantee.by.cheque");
            }
            final BigDecimal guaranteeAmount = payGuaranteeByChequeCommand.getGuaranteeAmount();
            final String clientExternalId = payGuaranteeByChequeCommand.getClientExternalId();
            final Client client = clientRepositoryWrapper.getClientByExternalId(clientExternalId);
            final Long clientId = client.getId();
            final Collection<SavingsAccountData> clientSavingsAccounts = this.savingsAccountReadPlatformService
                    .retrieveAllForLookup(clientId);
            if (CollectionUtils.isEmpty(clientSavingsAccounts)) {
                throw new BankChequeException("guarantee.savings.account.not.found",
                        "Guarantee savings is not found for client ID" + clientExternalId);
            }
            final Optional<SavingsAccountData> savingsAccountDataOptional = clientSavingsAccounts.stream()
                    .filter(accountData -> "Garantías".equals(accountData.getSavingsProductName())).findFirst();
            if (savingsAccountDataOptional.isEmpty()) {
                throw new BankChequeException("guarantee.savings.account.not.found",
                        "Guarantee savings is not found for client ID" + clientExternalId);
            }
            BigDecimal availableBalance = BigDecimal.ZERO;
            final SavingsAccountData savingsAccountData = savingsAccountDataOptional.get();
            if (savingsAccountData.getSummary() != null) {
                availableBalance = savingsAccountData.getSummary().getAvailableBalance();
            }
            if (guaranteeAmount.compareTo(availableBalance) > 0) {
                throw new BankChequeException("guarantee.amount.greater.than.available.savings.account.balance",
                        "Guarantee amount is greater than savings account balance of" + availableBalance);
            }

            String accountNo = savingsAccountData.getAccountNo();
            final boolean backdatedTxnsAllowedTill = false;

            final SavingsAccount fromSavingsAccount = this.savingsAccountAssembler.assembleFrom(savingsAccountData.getId(),
                    backdatedTxnsAllowedTill);

            cheque.setStatus(BankChequeStatus.PENDING_ISSUANCE.getValue());
            cheque.setCaseId(payGuaranteeByChequeCommand.getCaseId());
            cheque.setGuaranteeId(payGuaranteeByChequeCommand.getGuaranteeId());
            cheque.setGuaranteeName(payGuaranteeByChequeCommand.getGuaranteeName());
            cheque.setDescription(payGuaranteeByChequeCommand.getDescription());
            cheque.setGuaranteeAmount(payGuaranteeByChequeCommand.getGuaranteeAmount());
            cheque.setNumeroCliente(clientExternalId);
            final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
            final Long currentUserId = currentUser.getId();
            cheque.stampAudit(currentUserId, localDateTime);
            this.chequeBatchRepositoryWrapper.updateCheque(cheque);
        }
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).build();
    }

    @Override
    @Transactional
    public CommandProcessingResult printCheques(JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        List<PrintChequeCommand> printChequeCommandList = this.printChequeCommandFromApiJsonDeserializer.commandFromApiJson(command.json());
        for (final PrintChequeCommand printChequeCommand : printChequeCommandList) {
            this.printSingleCheque(printChequeCommand, currentUser, command);
        }
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).build();
    }

    @Override
    @Transactional
    public CommandProcessingResult printChequeBatches(JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        List<PrintChequeCommand> printChequeCommandList = this.printChequeCommandFromApiJsonDeserializer.commandFromApiJson(command.json());
        if (CollectionUtils.isEmpty(printChequeCommandList) || printChequeCommandList.size() < 2) {
            throw new BankChequeException("print.cheque.batches",
                    "Batch print requires more than one cheque. Use printcheques for a single cheque.");
        }
        for (final PrintChequeCommand printChequeCommand : printChequeCommandList) {
            final Cheque cheque = this.chequeBatchRepositoryWrapper.findOneChequeWithNotFoundDetection(printChequeCommand.getChequeId());
            if (!BankChequeStatus.READY_TO_BE_PRINTED.getValue().equals(cheque.getStatus())) {
                throw new BankChequeException("status", "invalid.loan.status.for.cheque.print");
            }
        }
        final String chequeIds = printChequeCommandList.stream().map(PrintChequeCommand::getChequeId).map(String::valueOf)
                .collect(Collectors.joining(","));
        final BatchChequeRequest batchChequeRequest = BatchChequeRequest.create(currentUser, chequeIds,
                DateUtils.getLocalDateTimeOfSystem());
        this.batchChequeRequestRepository.saveAndFlush(batchChequeRequest);

        final Long batchChequeRequestId = batchChequeRequest.getId();
        this.scheduleAsyncBatchChequeProcessing(batchChequeRequestId);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(batchChequeRequestId)
                .withResourceIdAsString(String.valueOf(batchChequeRequestId)).build();
    }

    private void scheduleAsyncBatchChequeProcessing(final Long batchChequeRequestId) {
        final BatchChequePrintEvent event = BatchChequePrintEvent.instance(this, batchChequeRequestId, ThreadLocalContextUtil.getContext());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    ChequeWritePlatformServiceImpl.this.applicationEventPublisher.publishEvent(event);
                }
            });
        } else {
            this.applicationEventPublisher.publishEvent(event);
        }
    }

    @Override
    public void processBatchChequeRequestById(final Long batchChequeRequestId) {
        final int claimed = this.batchChequeRequestRepository.claimForProcessing(batchChequeRequestId, BatchChequeRequestStatus.PROCESSING,
                BatchChequeRequestStatus.PENDING);
        if (claimed == 0) {
            log.info("Batch cheque request {} was already claimed or does not exist", batchChequeRequestId);
            return;
        }

        final List<Long> chequeIds;
        try {
            chequeIds = this.executeInTransaction(() -> this.printAllChequesInBatch(batchChequeRequestId));
        } catch (final RuntimeException e) {
            log.error("Failed to process batch cheque request {} - rolling back all cheque changes", batchChequeRequestId, e);
            this.markBatchChequeRequestFailed(batchChequeRequestId, resolveErrorMessage(e));
            throw e;
        }

        try {
            this.completeBatchChequeRequestPostProcessing(batchChequeRequestId, chequeIds);
        } catch (final RuntimeException e) {
            log.error("Cheques for batch request {} were printed, but post-processing failed", batchChequeRequestId, e);
            this.markBatchChequeRequestFailed(batchChequeRequestId,
                    "Cheques were printed successfully, but post-processing failed: " + resolveErrorMessage(e));
            throw e;
        }
    }

    private List<Long> printAllChequesInBatch(final Long batchChequeRequestId) {
        final BatchChequeRequest batchChequeRequest = this.batchChequeRequestRepository.findById(batchChequeRequestId)
                .orElseThrow(() -> new BankChequeException("print.cheque.batches",
                        "Batch cheque request " + batchChequeRequestId + " not found"));
        final AppUser requestedBy = batchChequeRequest.getRequestedBy();
        final List<Long> chequeIds = Arrays.stream(StringUtils.split(batchChequeRequest.getChequeIds(), ",")).map(String::trim)
                .filter(StringUtils::isNotBlank).map(Long::valueOf).collect(Collectors.toList());

        for (final Long chequeId : chequeIds) {
            try {
                final JsonObject root = new JsonObject();
                final JsonArray selectedCheques = new JsonArray();
                final JsonObject chequeObject = new JsonObject();
                chequeObject.addProperty("chequeId", chequeId);
                selectedCheques.add(chequeObject);
                root.add("selectedCheques", selectedCheques);
                final String json = root.toString();
                final JsonCommand command = JsonCommand.fromExistingCommand(null, json, this.fromApiJsonHelper.parse(json),
                        this.fromApiJsonHelper, null, null, null, null, null, null, null);
                final PrintChequeCommand printChequeCommand = PrintChequeCommand.builder().chequeId(chequeId).build();
                this.printSingleCheque(printChequeCommand, requestedBy, command);
            } catch (final Exception e) {
                log.error("Failed to print cheque {} for batch request {}", chequeId, batchChequeRequestId, e);
                throw new BankChequeException("print.cheque.batches", "Cheque " + chequeId + ": " + resolveErrorMessage(e));
            }
        }
        return chequeIds;
    }

    private String resolveErrorMessage(final Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        PlatformApiDataValidationException validationException = findValidationException(throwable);
        if (validationException != null && !CollectionUtils.isEmpty(validationException.getErrors())) {
            return validationException.getErrors().stream().map(this::formatApiParameterError).collect(Collectors.joining("; "));
        }

        if (throwable instanceof AbstractPlatformException) {
            final String defaultUserMessage = ((AbstractPlatformException) throwable).getDefaultUserMessage();
            if (StringUtils.isNotBlank(defaultUserMessage) && !isGenericValidationMessage(defaultUserMessage)) {
                return defaultUserMessage;
            }
        }

        if (StringUtils.isNotBlank(throwable.getMessage()) && !isGenericValidationMessage(throwable.getMessage())) {
            return throwable.getMessage();
        }

        if (throwable.getCause() != null && throwable.getCause() != throwable) {
            final String nestedMessage = resolveErrorMessage(throwable.getCause());
            if (StringUtils.isNotBlank(nestedMessage) && !isGenericValidationMessage(nestedMessage)) {
                return nestedMessage;
            }
        }

        return StringUtils.defaultIfBlank(throwable.getMessage(), "Unknown error");
    }

    private PlatformApiDataValidationException findValidationException(final Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof PlatformApiDataValidationException) {
                return (PlatformApiDataValidationException) current;
            }
            if (current.getCause() == null || current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return null;
    }

    private String formatApiParameterError(final ApiParameterError error) {
        final String parameter = StringUtils.defaultIfBlank(error.getParameterName(), "parameter");
        String message = error.getDeveloperMessage();
        if (StringUtils.isBlank(message)) {
            message = error.getDefaultUserMessage();
        }
        if (StringUtils.isBlank(message)) {
            message = error.getUserMessageGlobalisationCode();
        }
        if (StringUtils.isBlank(message)) {
            message = "Validation error";
        }
        return parameter + " - " + message;
    }

    private boolean isGenericValidationMessage(final String message) {
        return StringUtils.containsIgnoreCase(message, "Validation errors exist");
    }

    private void completeBatchChequeRequestPostProcessing(final Long batchChequeRequestId, final List<Long> chequeIds) {
        final BatchChequeRequest batchChequeRequest = this.batchChequeRequestRepository.findById(batchChequeRequestId)
                .orElseThrow(() -> new BankChequeException("print.cheque.batches",
                        "Batch cheque request " + batchChequeRequestId + " not found"));
        final AppUser requestedBy = batchChequeRequest.getRequestedBy();

        final StringBuilder reportErrorLog = new StringBuilder();
        final File batchChequeReport = this.generateBatchChequeReportAttachment(batchChequeRequestId, chequeIds, reportErrorLog);
        if (reportErrorLog.length() > 0) {
            log.warn("Pentaho errors while generating batch cheque report for request {}: {}", batchChequeRequestId, reportErrorLog);
        }

        final String email = requestedBy.getEmail();
        if (StringUtils.isNotBlank(email) && batchChequeReport != null) {
            final EmailMessageWithAttachmentData emailMessage = EmailMessageWithAttachmentData.createNew(email,
                    "Los cheques solicitados han sido procesados. Se adjunta el archivo generado.",
                    "Impresión de cheques - lote " + batchChequeRequestId, List.of(batchChequeReport));
            this.emailMessageJobEmailService.sendEmailWithAttachment(emailMessage);
        } else if (StringUtils.isBlank(email)) {
            log.warn("Batch cheque request {} has no email address for user {}", batchChequeRequestId, requestedBy.getId());
        } else {
            throw new BankChequeException("print.cheque.batches",
                    "Failed to generate Print Bank Cheque PDF for batch request " + batchChequeRequestId
                            + (reportErrorLog.length() > 0 ? ": " + reportErrorLog : ""));
        }

        this.markBatchChequeRequestCompleted(batchChequeRequestId);
    }

    private <T> T executeInTransaction(final java.util.function.Supplier<T> action) {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return transactionTemplate.execute(status -> action.get());
    }

    private void markBatchChequeRequestCompleted(final Long batchChequeRequestId) {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> {
            final BatchChequeRequest batchChequeRequest = this.batchChequeRequestRepository.findById(batchChequeRequestId).orElse(null);
            if (batchChequeRequest == null) {
                return;
            }
            batchChequeRequest.setStatus(BatchChequeRequestStatus.COMPLETED);
            batchChequeRequest.setDateProcessed(DateUtils.getLocalDateTimeOfSystem());
            batchChequeRequest.setProcessErrors(null);
            this.batchChequeRequestRepository.saveAndFlush(batchChequeRequest);
        });
    }

    private void markBatchChequeRequestFailed(final Long batchChequeRequestId, final String processErrors) {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> {
            final BatchChequeRequest batchChequeRequest = this.batchChequeRequestRepository.findById(batchChequeRequestId).orElse(null);
            if (batchChequeRequest == null) {
                return;
            }
            batchChequeRequest.setStatus(BatchChequeRequestStatus.FAILED);
            batchChequeRequest.setDateProcessed(DateUtils.getLocalDateTimeOfSystem());
            batchChequeRequest.setProcessErrors(StringUtils.abbreviate(StringUtils.defaultString(processErrors), 65000));
            this.batchChequeRequestRepository.saveAndFlush(batchChequeRequest);
        });
    }

    private File generateBatchChequeReportAttachment(final Long batchRequestId, final List<Long> chequeIds, final StringBuilder errorLog) {
        try {
            // Same format used by the UI: R_selectedCheques=,6061,6060,6059,
            final String selectedChequesParam = "," + chequeIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ",";
            final MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
            queryParams.putSingle("output-type", "PDF");
            queryParams.putSingle("R_selectedCheques", selectedChequesParam);

            final ReportingProcessService reportingProcessService = this.reportingProcessServiceProvider
                    .findReportingProcessService("Pentaho");
            if (reportingProcessService == null) {
                errorLog.append("Pentaho reporting service is not available; ");
                return null;
            }

            final Response processReport = reportingProcessService.processRequest(BankChequeApiConstants.PRINT_BANK_CHEQUE_REPORT_NAME,
                    queryParams);
            final Object responseObject = processReport != null ? processReport.getEntity() : null;
            byte[] pdfBytes = null;
            if (responseObject instanceof byte[]) {
                pdfBytes = (byte[]) responseObject;
            } else if (responseObject instanceof ByteArrayOutputStream) {
                pdfBytes = ((ByteArrayOutputStream) responseObject).toByteArray();
            }

            if (pdfBytes == null || pdfBytes.length == 0) {
                errorLog.append("Empty pentaho output for selectedCheques=").append(selectedChequesParam).append("; ");
                return null;
            }

            final String fileLocation = FileSystemContentRepository.FINERACT_BASE_DIR + File.separator + "batch-cheques";
            if (!new File(fileLocation).isDirectory()) {
                new File(fileLocation).mkdirs();
            }
            final File file = new File(fileLocation + File.separator + "batch-cheques-" + batchRequestId + ".pdf");
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                fileOutputStream.write(pdfBytes);
            }
            return file;
        } catch (Exception e) {
            errorLog.append("Exception generating batch cheque report: ").append(e.getMessage()).append("; ");
            log.error("Failed to generate Print Bank Cheque PDF for batch request {}", batchRequestId, e);
            return null;
        }
    }

    private void printSingleCheque(final PrintChequeCommand printChequeCommand, final AppUser currentUser, final JsonCommand command) {
        final Cheque cheque = this.chequeBatchRepositoryWrapper.findOneChequeWithNotFoundDetection(printChequeCommand.getChequeId());
        if (!BankChequeStatus.READY_TO_BE_PRINTED.getValue().equals(cheque.getStatus())) {
            throw new BankChequeException("status", "invalid.loan.status.for.cheque.print");
        }
        final String query = "SELECT " + this.chequeMapper.schema() + " WHERE mbc.id = ? ";
        ChequeData chequeData = this.jdbcTemplate.queryForObject(query, this.chequeMapper, cheque.getId());
        BigDecimal chequeAmount = chequeData.getGuaranteeAmount();
        final Long loanAccId = chequeData.getLoanAccId();
        final String bankAccNo = chequeData.getBankAccNo();
        final String numeroCliente = chequeData.getNumeroCliente();
        final Long guaranteeId = chequeData.getGuaranteeId();
        final BigDecimal guaranteeAmount = chequeData.getGuaranteeAmount();
        final Collection<PaymentTypeData> paymentTypeOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        if (loanAccId != null && chequeData.getLoanAmount() != null) {
            final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanAccId);
            if (loan.getCheque() != null && (loan.getCheque().getId() != null && !loan.getCheque().getId().equals(cheque.getId()))) {
                throw new BankChequeException("print.cheques", "Loan: " + loan.getAccountNumber() + " has a cheque assigned already");
            }
            if (!loan.isPendingDisbursementAuthorization() && !chequeData.getReassingedCheque()) {
                throw new BankChequeException(
                        "print.cheques.loan:" + loan.getAccountNumber() + " is.not.in.disbursement.authorization.status",
                        "No se pudo imprimir el cheque porque la cuenta de préstamo "+loan.getAccountNumber()+
                        " no está en estado de autorización de desembolso.");
            }

            if (!chequeData.getReassingedCheque()) {
                final JsonObject jsonObject = command.parsedJson() != null && command.parsedJson().isJsonObject()
                        ? command.parsedJson().getAsJsonObject().deepCopy()
                        : new JsonObject();
                // Remove print-only payload that is unsupported by loan disbursement validation.
                jsonObject.remove("selectedCheques");

                if (!jsonObject.has("locale") || jsonObject.get("locale").isJsonNull()) {
                    jsonObject.addProperty("locale", "en");
                }
                if (!jsonObject.has("dateFormat") || jsonObject.get("dateFormat").isJsonNull()) {
                    jsonObject.addProperty("dateFormat", "dd MMMM yyyy");
                }
                if (!jsonObject.has("actualDisbursementDate") || jsonObject.get("actualDisbursementDate").isJsonNull()) {
                    final String localeAsString = jsonObject.get("locale").getAsString();
                    final String dateFormat = jsonObject.get("dateFormat").getAsString();
                    final Locale locale = JsonParserHelper.localeFromString(localeAsString);
                    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat).withLocale(locale);
                    jsonObject.addProperty("actualDisbursementDate", DateUtils.getBusinessLocalDate().format(dateTimeFormatter));
                }

                jsonObject.addProperty("glAccountId", chequeData.getGlAccountId());
                jsonObject.addProperty("accountNumber", bankAccNo);
                jsonObject.addProperty("checkNumber", chequeData.getChequeNo());
                jsonObject.addProperty("routingCode", "");
                jsonObject.addProperty("receiptNumber", "");
                jsonObject.addProperty("note", "Desembolso de préstamo mediante cheque número " + chequeData.getChequeNo());
                if (!CollectionUtils.isEmpty(paymentTypeOptions)) {
                    final Optional<PaymentTypeData> paymentTypeDataOptional = new ArrayList<>(paymentTypeOptions).stream()
                            .filter(p -> BankChequeApiConstants.BANK_CHEQUE_PAYMENT_TYPE.equalsIgnoreCase(p.getName())).findFirst();
                    if (paymentTypeDataOptional.isPresent()) {
                        final PaymentTypeData paymentType = paymentTypeDataOptional.get();
                        jsonObject.addProperty("paymentTypeId", paymentType.getId());
                    }
                }
                final JsonCommand commandJson = JsonCommand.fromJsonElement(loanAccId, jsonObject, this.fromApiJsonHelper);
                commandJson.setJsonCommand(jsonObject.toString());
                CommandProcessingResult result = this.loanWritePlatformService.disburseLoan(loanAccId, commandJson, false);
                if (result.getLoanId() == null) {
                    throw new BankChequeException("print.cheques", "failed.to.disburse.loan " + loanAccId);
                }
            }
            chequeAmount = loan.getNetDisbursalAmount();
        }

        if (guaranteeId != null && guaranteeAmount != null) {
            final Client client = clientRepositoryWrapper.getClientByExternalId(numeroCliente);
            final Long clientId = client.getId();
            final Collection<SavingsAccountData> clientSavingsAccounts = this.savingsAccountReadPlatformService
                    .retrieveAllForLookup(clientId);
            if (CollectionUtils.isEmpty(clientSavingsAccounts)) {
                throw new BankChequeException("guarantee.savings.account.not.found",
                        "No se encontraron ahorros garantizados para el ID del cliente." + numeroCliente);
            }
            final Optional<SavingsAccountData> savingsAccountDataOptional = clientSavingsAccounts.stream()
                    .filter(accountData -> "Garantías".equals(accountData.getSavingsProductName())).findFirst();
            if (savingsAccountDataOptional.isEmpty()) {
                throw new BankChequeException("guarantee.savings.account.not.found",
                        "No se encontraron ahorros garantizados para el ID del cliente." + numeroCliente);
            }

            if (!chequeData.getReassingedCheque()) {
                BigDecimal availableBalance = BigDecimal.ZERO;
                final SavingsAccountData savingsAccountData = savingsAccountDataOptional.get();
                final Long savingsAccountId = savingsAccountData.getId();
                if (savingsAccountData.getSummary() != null) {
                    availableBalance = savingsAccountData.getSummary().getAvailableBalance();
                }
                if (guaranteeAmount.compareTo(availableBalance) > 0) {
                    throw new BankChequeException("guarantee.amount.greater.than.available.savings.account.balance",
                            "El importe de la garantía es mayor que el saldo de la cuenta de ahorros de" + availableBalance);
                }
                final String localeAsString = "en";
                final String dateFormat = "dd MMMM yyyy";
                final JsonObject jsonObject = new JsonObject();
                final LocalDate localDate = DateUtils.getBusinessLocalDate();
                Locale locale = JsonParserHelper.localeFromString(localeAsString);
                final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat).withLocale(locale);
                final String localDateString = localDate.format(dateTimeFormatter);
                jsonObject.addProperty("locale", localeAsString);
                jsonObject.addProperty("dateFormat", dateFormat);
                jsonObject.addProperty("transactionAmount", guaranteeAmount);
                jsonObject.addProperty("transactionDate", localDateString);
                if (!CollectionUtils.isEmpty(paymentTypeOptions)) {
                    Optional<PaymentTypeData> paymentTypeOptional = new ArrayList<>(paymentTypeOptions).stream()
                            .filter(pt -> BankChequeApiConstants.BANK_CHEQUE_PAYMENT_TYPE.equalsIgnoreCase(pt.getName())).findFirst();
                    if (paymentTypeOptional.isPresent()) {
                        PaymentTypeData paymentType = paymentTypeOptional.get();
                        jsonObject.addProperty("paymentTypeId", paymentType.getId());
                    }
                }
                jsonObject.addProperty("accountNumber", bankAccNo);
                jsonObject.addProperty("checkNumber", chequeData.getChequeNo());
                jsonObject.addProperty("receiptNumber", chequeData.getGuaranteeId());
                jsonObject.addProperty("bankNumber", chequeData.getBankName());
                jsonObject.addProperty("glAccountId", chequeData.getGlAccountId());
                jsonObject.addProperty("routingCode", "");
                final String note = "Retiro de garantía por ID de garantía " + guaranteeId;
                jsonObject.addProperty("note", note);
                final JsonCommand withdrawalJsonCommand = JsonCommand.fromJsonElement(savingsAccountId, jsonObject, this.fromApiJsonHelper);
                withdrawalJsonCommand.setJsonCommand(jsonObject.toString());
                CommandProcessingResult result = this.savingsAccountWritePlatformService.withdrawal(savingsAccountId,
                        withdrawalJsonCommand);
                if (result != null) {
                    log.info("Guarantee withdrawal is successful for savings account ID {}", result.getSavingsId());
                }
            }
        }
        if (chequeAmount != null) {
            final String amountInWords = NumberToWordsConverter.convertToWords(chequeAmount.intValue(),
                    NumberToWordsConverter.Language.SPANISH);
            String decimalValues = extractDecimals(chequeAmount);
            cheque.setAmountInWords(
                    new StringBuilder().append(amountInWords).append(" con ").append(decimalValues).append("/100").toString());
        }
        cheque.setStatus(BankChequeStatus.ISSUED.getValue());
        final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
        LocalDate localDate = DateUtils.getBusinessLocalDate();
        final Long currentUserId = currentUser.getId();
        cheque.stampAudit(currentUserId, localDateTime);
        cheque.setPrintedBy(currentUser);
        cheque.setPrintedDate(localDate);
        this.chequeBatchRepositoryWrapper.updateCheque(cheque);
        if (Boolean.TRUE.equals(chequeData.getReassingedCheque()) && chequeData.getReassignedFrom() != null) {
            Cheque reassignedCheque = this.chequeBatchRepositoryWrapper.findOneChequeWithNotFoundDetection(chequeData.getReassignedFrom());
            reassignedCheque.setStatus(BankChequeStatus.VOIDED.getValue());
            reassignedCheque.setVoidedDate(localDate);
            reassignedCheque.setVoidedBy(currentUser);
            this.chequeBatchRepositoryWrapper.updateCheque(reassignedCheque);
        }
    }

    public String extractDecimals(BigDecimal value) {
        String[] parts = StringUtils.split(value.toPlainString(), ".");
        if (parts.length > 1) {
            String padded = StringUtils.rightPad(parts[1], 2, "0");
            return padded.substring(0, 2);
        }
        return "00";
    }
}
