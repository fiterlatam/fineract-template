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
package org.apache.fineract.portfolio.loanaccount.jobs.applygactooverdueloaninstallment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.config.TaskExecutorConstant;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.jobs.domain.JobProcessedEntityRepository;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.service.LoanChargeWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ApplyGACToOverdueLoanInstallmentTasklet implements Tasklet {

    private final ConfigurationDomainService configurationDomainService;
    private final LoanReadPlatformService loanReadPlatformService;
    private final JobProcessedEntityRepository jobProcessedEntityRepository;
    private final ChargeRepositoryWrapper chargeRepository;
    private final LoanChargeWritePlatformService loanChargeWritePlatformService;

    @Qualifier(TaskExecutorConstant.CONFIGURABLE_TASK_EXECUTOR_BEAN_NAME)
    private final ThreadPoolTaskExecutor taskExecutor;

    int threadPoolSize = 0;
    int batchSize = 0;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        this.threadPoolSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("thread-pool-size"));
        this.batchSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("batch-size"));

        Long maxLoanId = 0L;
        final Long penaltyWaitPeriodValue = configurationDomainService.retrievePenaltyWaitPeriod();
        final Boolean backdatePenalties = configurationDomainService.isBackdatePenaltiesEnabled();

        long start = System.currentTimeMillis();
        log.info("Starting Apply GAC to Overdue Loans job");
        log.info("GAC Job is Using {} threads and pagesize (loans per thread) = {} ", this.threadPoolSize, this.batchSize);
        log.info("Reading overdue loan scheduled installments for processing!");
        List<OverdueLoanScheduleData> overdueLoanScheduledInstallments = loanReadPlatformService
                .retrieveAllLoansWithOverdueInstallments(penaltyWaitPeriodValue, backdatePenalties, 999999999, maxLoanId);

        processOverdueCharges(overdueLoanScheduledInstallments);
        return RepeatStatus.FINISHED;
    }

    @SuppressWarnings({ "squid:S3776" })
    // @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = Exception.class)
    public void processOverdueCharges(List<OverdueLoanScheduleData> overdueLoanScheduledInstallments) throws JobExecutionException {
        List<Throwable> exceptions = new ArrayList<>();
        log.info("Applying GAC Charges due for overdue loans for {} installments", overdueLoanScheduledInstallments.size());

        // Delete historical Job execution
        jobProcessedEntityRepository.deleteByJobIdAndExecutionDateBefore(57L, DateUtils.getLocalDateOfTenant().minusDays(2));

        // Check if we need to reprocess this job
        if (!configurationDomainService.getJobApplyPenaltyToOverdueLoansSkipWhenReprocessed()) {
            jobProcessedEntityRepository.deleteByJobId(57L);
        }

        if (!overdueLoanScheduledInstallments.isEmpty()) {
            final Map<Long, Collection<OverdueLoanScheduleData>> overdueScheduleData = new HashMap<>();
            for (final OverdueLoanScheduleData overdueInstallment : overdueLoanScheduledInstallments) {
                log.info("Processing overdue installment for loanId: {}, chargeId: {}, dueDate: {}", overdueInstallment.getLoanId(),
                        overdueInstallment.getChargeId(), overdueInstallment.getDueDate());

                final Charge chargeDefinition = this.chargeRepository.findOneWithNotFoundDetection(overdueInstallment.getChargeId());
                log.info("Processing charge with ID: {}, name: {} ", chargeDefinition.getId(), chargeDefinition.getName());

                if (chargeDefinition.getParentChargeId() == null) {
                    if (overdueScheduleData.containsKey(overdueInstallment.getLoanId())) {
                        overdueScheduleData.get(overdueInstallment.getLoanId()).add(overdueInstallment);
                    } else {
                        Collection<OverdueLoanScheduleData> loanData = new ArrayList<>();
                        loanData.add(overdueInstallment);
                        overdueScheduleData.put(overdueInstallment.getLoanId(), loanData);
                    }
                }
            }

            for (final OverdueLoanScheduleData overdueInstallment : overdueLoanScheduledInstallments) {
                final Charge chargeDefinition = this.chargeRepository.findOneWithNotFoundDetection(overdueInstallment.getChargeId());
                if (chargeDefinition.getParentChargeId() != null) {
                    if (overdueScheduleData.containsKey(overdueInstallment.getLoanId())) {
                        overdueScheduleData.get(overdueInstallment.getLoanId()).add(overdueInstallment);
                    } else {
                        Collection<OverdueLoanScheduleData> loanData = new ArrayList<>();
                        loanData.add(overdueInstallment);
                        overdueScheduleData.put(overdueInstallment.getLoanId(), loanData);
                    }
                }
            }
            log.info("Total accounts with overdue installments: {}", overdueScheduleData.size());

            processOverdueLoansInstallmentsInBatch(overdueScheduleData, exceptions);

            if (!exceptions.isEmpty()) {
                throw new JobExecutionException(exceptions);
            }

        }
    }

    private void processOverdueLoansInstallmentsInBatch(Map<Long, Collection<OverdueLoanScheduleData>> overdueScheduleData,
            List<Throwable> exceptions) {

        ExecutorService executor = Executors.newFixedThreadPool(this.threadPoolSize, r -> {
            Thread t = new Thread(r);
            t.setPriority(Thread.MAX_PRIORITY); // 10
            return t;
        });

        // Sort collection by loan_id descending to process higher loan ids first
        int batchSize = this.batchSize;
        List<Long> loanIds = new ArrayList<>(overdueScheduleData.keySet());
        loanIds.sort(Collections.reverseOrder());

        List<List<Long>> batches = new ArrayList<>();

        for (int i = 0; i < loanIds.size(); i += batchSize) {
            batches.add(loanIds.subList(i, Math.min(i + batchSize, loanIds.size())));
        }

        List<Future<Void>> futures = new ArrayList<>();

        for (List<Long> batch : batches) {

            FineractContext context = ThreadLocalContextUtil.getContext();

            Callable<Void> task = () -> {
                ThreadLocalContextUtil.init(context);
                try {

                    for (Long loanId : batch) {
                        try {
                            loanChargeWritePlatformService.applyOverdueGACForLoan(loanId, overdueScheduleData.get(loanId));

                            overdueScheduleData.remove(loanId);

                        } catch (Exception e) {
                            log.error("Error processing loan {}: {}", loanId, e.getMessage(), e);
                            exceptions.add(e);
                        }
                    }

                } finally {
                    ThreadLocalContextUtil.reset();
                }
                return null;
            };

            futures.add(executor.submit(task));
        }

        for (Future<Void> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
    }
}
