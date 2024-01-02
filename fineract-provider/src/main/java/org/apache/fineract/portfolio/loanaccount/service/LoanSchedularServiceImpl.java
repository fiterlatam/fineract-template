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

import com.google.gson.JsonObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.cob.loan.ApplyChargeToOverdueLoansBusinessStep;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformServiceUnavailableException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.JsonParserHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.office.exception.OfficeNotFoundException;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.LoanRepaymentImportData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentImport;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentImportRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentImportStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.paymentdetail.PaymentDetailConstants;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanSchedularServiceImpl implements LoanSchedularService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final ConfigurationDomainService configurationDomainService;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanWritePlatformService loanWritePlatformService;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final ApplicationContext applicationContext;
    private final ApplyChargeToOverdueLoansBusinessStep applyChargeToOverdueLoansBusinessStep;
    private final LoanRepository loanRepository;
    private final JdbcTemplate jdbcTemplate;
    private final LoanRepaymentImportMapper loanRepaymentImportMapper = new LoanRepaymentImportMapper();
    private final FromJsonHelper fromApiJsonHelper;
    private final LoanRepaymentImportRepository loanRepaymentImportRepository;

    @Override
    @CronTarget(jobName = JobName.APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT)
    public void applyChargeForOverdueLoans() throws JobExecutionException {

        final Long penaltyWaitPeriodValue = this.configurationDomainService.retrievePenaltyWaitPeriod();
        final Boolean backdatePenalties = this.configurationDomainService.isBackdatePenaltiesEnabled();
        final Collection<OverdueLoanScheduleData> overdueLoanScheduledInstallments = this.loanReadPlatformService
                .retrieveAllLoansWithOverdueInstallments(penaltyWaitPeriodValue, backdatePenalties);

        Set<Long> loanIds = overdueLoanScheduledInstallments.stream().map(OverdueLoanScheduleData::getLoanId).collect(Collectors.toSet());

        if (!loanIds.isEmpty()) {
            List<Throwable> exceptions = new ArrayList<>();
            for (final Long loanId : loanIds) {
                try {
                    applyChargeToOverdueLoansBusinessStep.execute(loanRepository.getReferenceById(loanId));
                } catch (final PlatformApiDataValidationException e) {
                    final List<ApiParameterError> errors = e.getErrors();
                    for (final ApiParameterError error : errors) {
                        log.error("Apply Charges due for overdue loans failed for account {} with message: {}", loanId,
                                error.getDeveloperMessage(), e);
                    }
                    exceptions.add(e);
                } catch (final AbstractPlatformDomainRuleException e) {
                    log.error("Apply Charges due for overdue loans failed for account {} with message: {}", loanId,
                            e.getDefaultUserMessage(), e);
                    exceptions.add(e);
                } catch (Exception e) {
                    log.error("Apply Charges due for overdue loans failed for account {}", loanId, e);
                    exceptions.add(e);
                }
            }
            if (!exceptions.isEmpty()) {
                throw new JobExecutionException(exceptions);
            }
        }
    }

    @Override
    @CronTarget(jobName = JobName.RECALCULATE_INTEREST_FOR_LOAN)
    @SuppressFBWarnings(value = {
            "DMI_RANDOM_USED_ONLY_ONCE" }, justification = "False positive for random object created and used only once")
    public void recalculateInterest() throws JobExecutionException {
        Integer maxNumberOfRetries = ThreadLocalContextUtil.getTenant().getConnection().getMaxRetriesOnDeadlock();
        Integer maxIntervalBetweenRetries = ThreadLocalContextUtil.getTenant().getConnection().getMaxIntervalBetweenRetries();
        Collection<Long> loanIds = this.loanReadPlatformService.fetchLoansForInterestRecalculation();
        int i = 0;
        if (!loanIds.isEmpty()) {
            List<Throwable> errors = new ArrayList<>();
            for (Long loanId : loanIds) {
                log.info("recalculateInterest: Loan ID = {}", loanId);
                Integer numberOfRetries = 0;
                while (numberOfRetries <= maxNumberOfRetries) {
                    try {
                        this.loanWritePlatformService.recalculateInterest(loanId);
                        numberOfRetries = maxNumberOfRetries + 1;
                    } catch (CannotAcquireLockException | ObjectOptimisticLockingFailureException exception) {
                        log.info("Recalulate interest job has been retried {} time(s)", numberOfRetries);
                        // Fail if the transaction has been retried for
                        // maxNumberOfRetries
                        if (numberOfRetries >= maxNumberOfRetries) {
                            log.error("Recalulate interest job has been retried for the max allowed attempts of {} and will be rolled back",
                                    numberOfRetries);
                            errors.add(exception);
                            break;
                        }
                        // Else sleep for a random time (between 1 to 10
                        // seconds) and continue
                        try {
                            int randomNum = RANDOM.nextInt(maxIntervalBetweenRetries + 1);
                            Thread.sleep(1000 + (randomNum * 1000));
                            numberOfRetries = numberOfRetries + 1;
                        } catch (InterruptedException e) {
                            log.error("Interest recalculation for loans retry failed due to InterruptedException", e);
                            errors.add(e);
                            break;
                        }
                    } catch (Exception e) {
                        log.error("Interest recalculation for loans failed for account {}", loanId, e);
                        numberOfRetries = maxNumberOfRetries + 1;
                        errors.add(e);
                    }
                    i++;
                }
                log.info("recalculateInterest: Loans count {}", i);
            }
            if (!errors.isEmpty()) {
                throw new JobExecutionException(errors);
            }
        }

    }

    @Override
    @CronTarget(jobName = JobName.RECALCULATE_INTEREST_FOR_LOAN)
    public void recalculateInterest(Map<String, String> jobParameters) {
        // gets the officeId
        final String officeId = jobParameters.get("officeId");
        log.info("recalculateInterest: officeId={}", officeId);
        Long officeIdLong = Long.valueOf(officeId);

        // gets the Office object
        final OfficeData office = this.officeReadPlatformService.retrieveOffice(officeIdLong);
        if (office == null) {
            throw new OfficeNotFoundException(officeIdLong);
        }
        final int threadPoolSize = Integer.parseInt(jobParameters.get("thread-pool-size"));
        final int batchSize = Integer.parseInt(jobParameters.get("batch-size"));

        recalculateInterest(office, threadPoolSize, batchSize);
    }

    private void recalculateInterest(OfficeData office, int threadPoolSize, int batchSize) {
        final int pageSize = batchSize * threadPoolSize;

        // initialise the executor service with fetched configurations
        final ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        Long maxLoanIdInList = 0L;
        final String officeHierarchy = office.getHierarchy() + "%";

        // get the loanIds from service
        List<Long> loanIds = Collections.synchronizedList(
                this.loanReadPlatformService.fetchLoansForInterestRecalculation(pageSize, maxLoanIdInList, officeHierarchy));

        // gets the loanIds data set iteratively and call addAccuruals for that
        // paginated dataset
        do {
            int totalFilteredRecords = loanIds.size();
            log.info("Starting accrual - total filtered records - {}", totalFilteredRecords);
            recalculateInterest(loanIds, threadPoolSize, batchSize, executorService);
            maxLoanIdInList += pageSize + 1;
            loanIds = Collections.synchronizedList(
                    this.loanReadPlatformService.fetchLoansForInterestRecalculation(pageSize, maxLoanIdInList, officeHierarchy));
        } while (!CollectionUtils.isEmpty(loanIds));

        // shutdown the executor when done
        executorService.shutdownNow();
    }

    private void recalculateInterest(List<Long> loanIds, int threadPoolSize, int batchSize, final ExecutorService executorService) {

        List<Callable<Void>> posters = new ArrayList<>();
        int fromIndex = 0;
        // get the size of current paginated dataset
        int size = loanIds.size();
        // calculate the batch size
        double toGetCeilValue = size / threadPoolSize;
        batchSize = (int) Math.ceil(toGetCeilValue);

        if (batchSize == 0) {
            return;
        }

        int toIndex = (batchSize > size - 1) ? size : batchSize;
        while (toIndex < size && loanIds.get(toIndex - 1).equals(loanIds.get(toIndex))) {
            toIndex++;
        }
        boolean lastBatch = false;
        int loopCount = size / batchSize + 1;

        for (long i = 0; i < loopCount; i++) {
            List<Long> subList = safeSubList(loanIds, fromIndex, toIndex);
            RecalculateInterestPoster poster = (RecalculateInterestPoster) this.applicationContext.getBean("recalculateInterestPoster");
            poster.setLoanIds(subList);
            poster.setLoanWritePlatformService(loanWritePlatformService);
            posters.add(poster);
            if (lastBatch) {
                break;
            }
            if (toIndex + batchSize > size - 1) {
                lastBatch = true;
            }
            fromIndex = fromIndex + (toIndex - fromIndex);
            toIndex = (toIndex + batchSize > size - 1) ? size : toIndex + batchSize;
            while (toIndex < size && loanIds.get(toIndex - 1).equals(loanIds.get(toIndex))) {
                toIndex++;
            }
        }

        try {
            List<Future<Void>> responses = executorService.invokeAll(posters);
            checkCompletion(responses);
        } catch (InterruptedException e1) {
            log.error("Interrupted while recalculateInterest", e1);
        }
    }

    // break the lists into sub lists
    private <T> List<T> safeSubList(List<T> list, int fromIndex, int toIndex) {
        int size = list.size();
        if (fromIndex >= size || toIndex <= 0 || fromIndex >= toIndex) {
            return Collections.emptyList();
        }

        fromIndex = Math.max(0, fromIndex);
        toIndex = Math.min(size, toIndex);

        return list.subList(fromIndex, toIndex);
    }

    // checks the execution of task by each thread in the executor service
    private void checkCompletion(List<Future<Void>> responses) {
        try {
            for (Future<Void> f : responses) {
                f.get();
            }
            boolean allThreadsExecuted = false;
            int noOfThreadsExecuted = 0;
            for (Future<Void> future : responses) {
                if (future.isDone()) {
                    noOfThreadsExecuted++;
                }
            }
            allThreadsExecuted = noOfThreadsExecuted == responses.size();
            if (!allThreadsExecuted) {
                log.error("All threads could not execute.");
            }
        } catch (InterruptedException e1) {
            log.error("Interrupted while posting IR entries", e1);
        } catch (ExecutionException e2) {
            log.error("Execution exception while posting IR entries", e2);
        }
    }

    @Override
    @CronTarget(jobName = JobName.IMPORT_BATCHES_OF_LOAN_REPAYMENTS)
    public void importLoanRepaymentBatches() throws JobExecutionException {
        final String sql = "SELECT " + this.loanRepaymentImportMapper.getSchema() + " WHERE pp.Estado = ? ";
        List<LoanRepaymentImportData> loanRepayments = this.jdbcTemplate.query(sql, this.loanRepaymentImportMapper, 1);
        List<Throwable> exceptions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(loanRepayments)) {
            int iterations = 0;
            for (final LoanRepaymentImportData loanRepaymentImportData : loanRepayments) {
                iterations++;
                final String loanCode = loanRepaymentImportData.getLoanCode();
                final LoanRepaymentImport loanRepaymentImport = this.loanRepaymentImportRepository.findById(loanRepaymentImportData.getId())
                        .orElseThrow(() -> new NotFoundException("error.msg.loanRepaymentImport.not.found."));
                loanRepaymentImport.resetMifosFields();
                final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
                loanRepaymentImport.setMifosProcessingDate(localDateTime);
                loanRepaymentImport.setMifosProcessingTime(localDateTime.toLocalTime());
                loanRepaymentImport.setMifosFileName("Mifos-Run-" + iterations);
                loanRepaymentImport.setLastPayment("N");
                try {
                    final Optional<Loan> loanAccountOptional = loanRepository.findLoanByExternalId(loanCode);

                    // Loan not found
                    if (loanAccountOptional.isEmpty()) {
                        final String failMessage = "error.msg.loan.not.found: Loan ID :: " + loanCode;
                        loanRepaymentImport.setStatus(LoanRepaymentImportStatus.ERROR.getId());
                        loanRepaymentImport.setErrorId(1L);
                        loanRepaymentImport.setOperationResult(failMessage);
                        exceptions.add(new PlatformServiceUnavailableException(failMessage, failMessage));
                        log.error(failMessage);
                        this.loanRepaymentImportRepository.saveAndFlush(loanRepaymentImport);
                        continue;
                    }
                    final Loan loanAccount = loanAccountOptional.get();

                    // Loan is already closed/cancelled
                    if (loanAccount.isClosed()) {
                        final String failMessage = "error.msg.loan.is.closed.or.cancelled: Loan ID :: " + loanCode;
                        loanRepaymentImport.setStatus(LoanRepaymentImportStatus.ERROR.getId());
                        loanRepaymentImport.setErrorId(2L);
                        loanRepaymentImport.setOperationResult(failMessage);
                        log.error(failMessage);
                        exceptions.add(new PlatformServiceUnavailableException(failMessage, failMessage));
                        this.loanRepaymentImportRepository.saveAndFlush(loanRepaymentImport);
                        continue;
                    }
                    final MonetaryCurrency currency = loanAccount.getCurrency();
                    final boolean isRecoveryRepayment = false;
                    final Long loanId = loanAccount.getId();
                    final LocalDate paymentDate = loanRepaymentImport.getPaymentDate();
                    final String localeAsString = "en";
                    final String dateFormat = "dd MMMM yyyy";
                    Locale locale = JsonParserHelper.localeFromString(localeAsString);
                    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat).withLocale(locale);
                    final String transactionDate = paymentDate.format(dateTimeFormatter);
                    final BigDecimal transactionAmount = loanRepaymentImport.getAmount();
                    final String checkNumber = "";
                    final String accountNumber = loanRepaymentImport.getBankingAgency();
                    final String bankNumber = loanRepaymentImport.getBankName();
                    final String receiptNumber = loanRepaymentImport.getReceiptNumber();
                    final BigDecimal paymentToleranceLimit = loanAccount.getLoanProduct().getPaymentToleranceLimit();
                    final LoanTransactionData transactionData = this.loanReadPlatformService.retrieveLoanTransactionTemplate(loanId);
                    final Collection<PaymentTypeData> paymentTypeOptions = transactionData.getPaymentTypeOptions();
                    Long paymentTypeId = 1L;
                    if (!paymentTypeOptions.isEmpty()) {
                        paymentTypeId = new ArrayList<>(paymentTypeOptions).get(0).getId();
                    }

                    final BigDecimal scheduledAmount = transactionData.getAmount();
                    final Integer installmentNumber = transactionData.getInstallmentNumber();
                    final BigDecimal outstandingLoanBalance = transactionData.getOutstandingLoanBalance();
                    String tolerance;
                    if (Money.of(currency, transactionAmount).isGreaterThan(Money.of(currency, scheduledAmount))) {
                        tolerance = "Mayor";
                    } else if (Money.of(currency, transactionAmount).isLessThan(Money.of(currency, scheduledAmount))) {
                        tolerance = "Menor";
                    } else {
                        tolerance = "Igual";
                    }
                    loanRepaymentImport.setScheduledPaymentAmount(scheduledAmount);
                    loanRepaymentImport.setTolerance(tolerance);
                    loanRepaymentImport.setPaymentNumber(installmentNumber.longValue());
                    final Integer numberOfInstallments = loanAccount.getNumberOfRepayments();
                    final String lastInstallment = Objects.equals(numberOfInstallments, installmentNumber) ? "S" : "N";
                    loanRepaymentImport.setLastPayment(lastInstallment);
                    // Tolerance limit is not met
                    if (paymentToleranceLimit.compareTo(BigDecimal.ZERO) > 0) {
                        final BigDecimal limitPortion = scheduledAmount.multiply(paymentToleranceLimit).divide(BigDecimal.valueOf(100L),
                                MoneyHelper.getRoundingMode());
                        final BigDecimal upperLimitAmount = scheduledAmount.add(limitPortion);
                        final BigDecimal lowerLimitAmount = scheduledAmount.subtract(limitPortion);
                        if (transactionAmount.compareTo(upperLimitAmount) > 0) {
                            final String failMessage = "error.msg.the.provided.transaction.amount.is.greater.than.tolerance.limit:: Loan ID = "
                                    + loanCode + " and transaction amount = " + transactionAmount;
                            loanRepaymentImport.setStatus(LoanRepaymentImportStatus.ERROR.getId());
                            loanRepaymentImport.setErrorId(4L);
                            loanRepaymentImport.setOperationResult(failMessage);
                            log.error(failMessage);
                            exceptions.add(new PlatformServiceUnavailableException(failMessage, failMessage));
                            this.loanRepaymentImportRepository.saveAndFlush(loanRepaymentImport);
                            continue;
                        }

                        if (transactionAmount.compareTo(lowerLimitAmount) < 0) {
                            final String failMessage = "error.msg.the.provided.transaction.amount.is.less.than.tolerance.limit:: Loan ID = "
                                    + loanCode + " and transaction amount = " + transactionAmount;
                            loanRepaymentImport.setStatus(LoanRepaymentImportStatus.ERROR.getId());
                            loanRepaymentImport.setErrorId(5L);
                            loanRepaymentImport.setOperationResult(failMessage);
                            log.error(failMessage);
                            exceptions.add(new PlatformServiceUnavailableException(failMessage, failMessage));
                            this.loanRepaymentImportRepository.saveAndFlush(loanRepaymentImport);
                            continue;
                        }
                    }

                    // Payment amount greater than outstanding
                    if (Money.of(currency, transactionAmount).isGreaterThan(Money.of(currency, outstandingLoanBalance))) {
                        final String failMessage = "error.msg.the.provided.transaction.amount.exceeds.the.amount.to.payoff.the.loans:: Loan ID = "
                                + loanCode + " and transaction amount = " + transactionAmount;
                        loanRepaymentImport.setStatus(LoanRepaymentImportStatus.ERROR.getId());
                        loanRepaymentImport.setErrorId(3L);
                        loanRepaymentImport.setOperationResult(failMessage);
                        log.error(failMessage);
                        exceptions.add(new PlatformServiceUnavailableException(failMessage, failMessage));
                        this.loanRepaymentImportRepository.saveAndFlush(loanRepaymentImport);
                        continue;
                    }

                    // Make loan repayment
                    final JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty(PaymentDetailConstants.paymentTypeParamName, paymentTypeId);
                    jsonObject.addProperty("transactionAmount", transactionAmount);
                    jsonObject.addProperty("transactionDate", transactionDate);
                    jsonObject.addProperty(PaymentDetailConstants.accountNumberParamName, accountNumber);
                    jsonObject.addProperty("checkNumber", checkNumber);
                    jsonObject.addProperty(PaymentDetailConstants.bankNumberParamName, bankNumber);
                    jsonObject.addProperty(PaymentDetailConstants.receiptNumberParamName, receiptNumber);
                    jsonObject.addProperty("locale", localeAsString);
                    jsonObject.addProperty("dateFormat", dateFormat);
                    if (StringUtils.equalsIgnoreCase(lastInstallment, "S") || outstandingLoanBalance.compareTo(transactionAmount)<= 0) {
                        jsonObject.addProperty("adjustGuarantee", true);
                    }
                    final JsonCommand command = JsonCommand.fromJsonElement(loanId, jsonObject, fromApiJsonHelper);
                    command.setJsonCommand(jsonObject.toString());
                    CommandProcessingResult result = loanWritePlatformService.makeLoanRepayment(LoanTransactionType.REPAYMENT, loanId,
                            command, isRecoveryRepayment);
                    loanRepaymentImport.setStatus(LoanRepaymentImportStatus.PROCESSED.getId());
                    loanRepaymentImport.setOperationResult("Aplicado");
                    log.info("Import loan repayment successful for loan code {}", result.getLoanId());
                } catch (final PlatformApiDataValidationException e) {
                    loanRepaymentImport.setErrorId(6L);
                    loanRepaymentImport.setStatus(LoanRepaymentImportStatus.ERROR.getId());
                    final List<ApiParameterError> errors = e.getErrors();
                    String developerMessages = "";
                    for (final ApiParameterError error : errors) {
                        developerMessages = developerMessages + error.getDeveloperMessage() + "\n";
                        log.error("Import loan repayment failed for loan code {} with message: {}", loanCode, error.getDeveloperMessage(),
                                e);
                    }
                    loanRepaymentImport.setOperationResult(developerMessages);
                    exceptions.add(e);
                } catch (final AbstractPlatformDomainRuleException e) {
                    final String defaultUserMessage = e.getDefaultUserMessage();
                    loanRepaymentImport.setErrorId(6L);
                    loanRepaymentImport.setStatus(LoanRepaymentImportStatus.ERROR.getId());
                    loanRepaymentImport.setOperationResult(defaultUserMessage);
                    log.error("Import loan repayment failed for loan code {} with message: {}", loanCode, e.getDefaultUserMessage(), e);
                    exceptions.add(e);
                } catch (Exception e) {
                    final String localizedMessage = e.getLocalizedMessage();
                    loanRepaymentImport.setErrorId(6L);
                    loanRepaymentImport.setStatus(LoanRepaymentImportStatus.ERROR.getId());
                    loanRepaymentImport.setOperationResult(localizedMessage);
                    log.error("Import loan repayment failed for loan code {}", loanCode, e);
                    exceptions.add(e);
                }
                this.loanRepaymentImportRepository.saveAndFlush(loanRepaymentImport);
            }
        }

        if (!exceptions.isEmpty()) {
            throw new JobExecutionException(exceptions);
        }
    }

    public static final class LoanRepaymentImportMapper implements RowMapper<LoanRepaymentImportData> {

        private final String schema;

        LoanRepaymentImportMapper() {
            this.schema = """
                    	pp.Id_pagos AS id,
                    	pp.Agencia_bancaria AS bankingAgency,
                    	pp.Codigo_cliente AS customerCode,
                    	pp.Agencia AS agency,
                    	pp.Codigo_prestamo AS loanCode,
                    	pp.Codigo_producto AS productCode,
                    	pp.Monto AS amount,
                    	pp.Boleta AS receiptNumber,
                    	pp.Estado AS status,
                    	pp.Fecha_carga AS uploadDate,
                    	pp.Hora_carga AS uploadTime,
                    	pp.Feha_procesamiento_mifos AS mifosProcessingDate,
                    	pp.Hora_procesamiento_mifos AS mifosProcessingTime,
                    	pp.Nombre_archivo AS mifosFileName,
                    	pp.Banco AS bankName,
                    	pp.Grupo AS groupNumber,
                    	pp.Codigo_producto_mifos AS mifosProductCode,
                    	pp.Fecha_pago AS paymentDate,
                    	pp.Resultado_operacion AS operationResult,
                    	pp.Monto_pago_programado AS scheduledPaymentAmount,
                    	pp.Numero_de_pago AS paymentNumber,
                    	pp.Ultimo_pago AS lastPayment,
                    	pp.Tolerancia AS tolerance,
                    	pp.Id_error AS errorId
                    FROM PDA_Pagos pp
                    """;
        }

        public String getSchema() {
            return schema;
        }

        @Override
        public LoanRepaymentImportData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final String bankingAgency = rs.getString("bankingAgency");
            final Long customerCode = JdbcSupport.getLong(rs, "customerCode");
            final String agency = rs.getString("agency");
            final String loanCode = rs.getString("loanCode");
            final String productCode = rs.getString("productCode");
            final BigDecimal amount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "amount");
            final String receiptNumber = rs.getString("receiptNumber");
            final Long status = JdbcSupport.getLong(rs, "status");
            final LocalDateTime uploadDate = JdbcSupport.getLocalDateTime(rs, "uploadDate");
            final LocalTime uploadTime = JdbcSupport.getLocalTime(rs, "uploadTime");
            final LocalDateTime mifosProcessingDate = JdbcSupport.getLocalDateTime(rs, "mifosProcessingDate");
            final LocalTime mifosProcessingTime = JdbcSupport.getLocalTime(rs, "mifosProcessingTime");
            final String mifosFileName = rs.getString("mifosFileName");
            final String bankName = rs.getString("bankName");
            final Long groupNumber = JdbcSupport.getLong(rs, "groupNumber");
            final Long mifosProductCode = JdbcSupport.getLong(rs, "mifosProductCode");
            final LocalDate paymentDate = JdbcSupport.getLocalDate(rs, "paymentDate");
            final String operationResult = rs.getString("operationResult");
            final BigDecimal scheduledPaymentAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "scheduledPaymentAmount");
            final Long paymentNumber = JdbcSupport.getLong(rs, "paymentNumber");
            final String lastPayment = rs.getString("lastPayment");
            final String tolerance = rs.getString("tolerance");
            final Long errorId = JdbcSupport.getLong(rs, "errorId");
            return LoanRepaymentImportData.builder().id(id).bankingAgency(bankingAgency).agency(agency).customerCode(customerCode)
                    .loanCode(loanCode).productCode(productCode).amount(amount).receiptNumber(receiptNumber).status(status)
                    .uploadDate(uploadDate).uploadTime(uploadTime).mifosProcessingDate(mifosProcessingDate)
                    .mifosProcessingTime(mifosProcessingTime).mifosFileName(mifosFileName).bankName(bankName).groupNumber(groupNumber)
                    .mifosProductCode(mifosProductCode).paymentDate(paymentDate).operationResult(operationResult)
                    .scheduledPaymentAmount(scheduledPaymentAmount).paymentNumber(paymentNumber).lastPayment(lastPayment)
                    .tolerance(tolerance).errorId(errorId).build();
        }
    }
}
