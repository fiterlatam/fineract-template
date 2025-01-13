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
package org.apache.fineract.portfolio.loanaccount.jobs.applychargetooverdueloaninstallment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.config.TaskExecutorConstant;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ApplyChargeToOverdueLoanInstallmentTasklet implements Tasklet {

    private static final int QUEUE_SIZE = 1;
    private final Queue<List<OverdueLoanScheduleData>> queue = new ArrayDeque<>();
    private final ApplicationContext applicationContext;
    @Qualifier(TaskExecutorConstant.CONFIGURABLE_TASK_EXECUTOR_BEAN_NAME)
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ConfigurationDomainService configurationDomainService;
    private final LoanReadPlatformService loanReadPlatformService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        final int threadPoolSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("thread-pool-size"));
        taskExecutor.setCorePoolSize(threadPoolSize);
        taskExecutor.setMaxPoolSize(threadPoolSize);
        final int batchSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("batch-size"));
        final int pageSize = batchSize * threadPoolSize;
        Long maxInstallmentId = 0L;
        final Long penaltyWaitPeriodValue = configurationDomainService.retrievePenaltyWaitPeriod();
        final Boolean backdatePenalties = configurationDomainService.isBackdatePenaltiesEnabled();

        long start = System.currentTimeMillis();
        log.info("Starting Apply Penalties to Overdue Loans job");
        log.debug("Reading overdue loan scheduled installments for processing!");
        List<OverdueLoanScheduleData> overdueLoanScheduledInstallments = loanReadPlatformService
                .retrieveAllLoansWithOverdueInstallments(penaltyWaitPeriodValue, backdatePenalties, pageSize, maxInstallmentId);
        if (overdueLoanScheduledInstallments != null && !overdueLoanScheduledInstallments.isEmpty()) {
            overdueLoanScheduledInstallments = Collections.synchronizedList(overdueLoanScheduledInstallments);
            long finish = System.currentTimeMillis();
            log.debug("Done fetching overdue loan scheduled installments within {} milliseconds", finish - start);
            queue.add(overdueLoanScheduledInstallments);

            if (!CollectionUtils.isEmpty(queue)) {
                do {
                    List<OverdueLoanScheduleData> queueElement = queue.element();
                    maxInstallmentId = queueElement.get(queueElement.size() - 1).getInstallmentId();
                    this.applyPenaltiesToOverdueInstallments(queue.remove(), threadPoolSize, pageSize, maxInstallmentId,
                            penaltyWaitPeriodValue, backdatePenalties);
                } while (!CollectionUtils.isEmpty(queue));
            }
        }
        return RepeatStatus.FINISHED;
    }

    private void applyPenaltiesToOverdueInstallments(List<OverdueLoanScheduleData> overdueLoanScheduledInstallments, int threadPoolSize,
            int pageSize, Long maxInstallmentId, Long penaltyWaitPeriodValue, Boolean backdatePenalties) {
        List<Callable<Void>> posters = new ArrayList<>();
        int fromIndex = 0;
        int size = overdueLoanScheduledInstallments.size();
        int batchSize = (int) Math.ceil((double) size / threadPoolSize);

        if (batchSize == 0) {
            return;
        }

        int toIndex = (batchSize > size - 1) ? size : batchSize;
        while (toIndex < size && overdueLoanScheduledInstallments.get(toIndex - 1).getInstallmentId()
                .equals(overdueLoanScheduledInstallments.get(toIndex).getInstallmentId())) {
            toIndex++;
        }
        boolean lastBatch = false;
        int loopCount = size / batchSize + 1;

        FineractContext context = ThreadLocalContextUtil.getContext();

        Callable<Void> fetchData = () -> {
            ThreadLocalContextUtil.init(context);
            Long maxId = maxInstallmentId;
            if (!queue.isEmpty()) {
                maxId = Math.max(maxInstallmentId, queue.element().get(queue.element().size() - 1).getInstallmentId());
            }
            while (queue.size() <= QUEUE_SIZE) {

                log.debug("Fetching while threads are running!");
                List<OverdueLoanScheduleData> overdueLoanScheduleData = Collections.synchronizedList(this.loanReadPlatformService
                        .retrieveAllLoansWithOverdueInstallments(penaltyWaitPeriodValue, backdatePenalties, pageSize, maxId));
                if (overdueLoanScheduleData.isEmpty()) {
                    break;
                }
                maxId = overdueLoanScheduleData.get(overdueLoanScheduleData.size() - 1).getInstallmentId();
                queue.add(overdueLoanScheduleData);
            }
            return null;
        };
        posters.add(fetchData);

        for (long i = 0; i < loopCount; i++) {
            List<OverdueLoanScheduleData> subList = safeSubList(overdueLoanScheduledInstallments, fromIndex, toIndex);
            ApplyChargeToOverdueLoanInstallmentProcessorTask applyChargeToOverdueLoanInstallmentProcessorTask = applicationContext
                    .getBean(ApplyChargeToOverdueLoanInstallmentProcessorTask.class);
            applyChargeToOverdueLoanInstallmentProcessorTask.setOverdueLoanScheduledInstallments(subList);
            applyChargeToOverdueLoanInstallmentProcessorTask.setContext(ThreadLocalContextUtil.getContext());
            posters.add(applyChargeToOverdueLoanInstallmentProcessorTask);

            if (lastBatch) {
                break;
            }
            if (toIndex + batchSize > size - 1) {
                lastBatch = true;
            }
            fromIndex = fromIndex + (toIndex - fromIndex);
            toIndex = (toIndex + batchSize > size - 1) ? size : toIndex + batchSize;
            while (toIndex < size && overdueLoanScheduledInstallments.get(toIndex - 1).getInstallmentId()
                    .equals(overdueLoanScheduledInstallments.get(toIndex).getInstallmentId())) {
                toIndex++;
            }
        }

        List<Future<Void>> responses = new ArrayList<>();
        posters.forEach(poster -> responses.add(taskExecutor.submit(poster)));
        Long maxId = maxInstallmentId;
        if (!queue.isEmpty()) {
            maxId = Math.max(maxInstallmentId, queue.element().get(queue.element().size() - 1).getInstallmentId());
        }

        while (queue.size() <= QUEUE_SIZE) {
            log.debug("Fetching while threads are running!..:: this is not supposed to run........");
            overdueLoanScheduledInstallments = Collections.synchronizedList(this.loanReadPlatformService
                    .retrieveAllLoansWithOverdueInstallments(penaltyWaitPeriodValue, backdatePenalties, pageSize, maxId));
            if (overdueLoanScheduledInstallments.isEmpty()) {
                break;
            }
            maxId = overdueLoanScheduledInstallments.get(overdueLoanScheduledInstallments.size() - 1).getInstallmentId();
            log.debug("Add to the Queue");
            queue.add(overdueLoanScheduledInstallments);
        }

        checkCompletion(responses);
        log.debug("Queue size {}", queue.size());
    }

    private <T> List<T> safeSubList(List<T> list, int fromIndex, int toIndex) {
        int size = list.size();
        if (fromIndex >= size || toIndex <= 0 || fromIndex >= toIndex) {
            return Collections.emptyList();
        }

        fromIndex = Math.max(0, fromIndex);
        toIndex = Math.min(size, toIndex);

        return list.subList(fromIndex, toIndex);
    }

    private void checkCompletion(List<Future<Void>> responses) {
        try {
            for (Future<Void> f : responses) {
                f.get();
            }
            boolean allThreadsExecuted;
            int noOfThreadsExecuted = 0;
            for (Future<Void> future : responses) {
                if (future.isDone()) {
                    noOfThreadsExecuted++;
                }
            }
            allThreadsExecuted = noOfThreadsExecuted == responses.size();
            if (!allThreadsExecuted) {
                log.error("All threads could not execute.");
            } else {
                log.info("Apply Penalties to Overdue Loans Job Completed");
            }
        } catch (InterruptedException e1) {
            log.error("Interrupted while Applying Penalties to Overdue Loans", e1);
        } catch (ExecutionException e2) {
            log.error("Execution exception while Applying Penalties to Overdue Loans", e2);
        }
    }
}
