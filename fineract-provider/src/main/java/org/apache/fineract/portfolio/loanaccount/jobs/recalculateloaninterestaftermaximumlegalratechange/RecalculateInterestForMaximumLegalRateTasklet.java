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
package org.apache.fineract.portfolio.loanaccount.jobs.recalculateloaninterestaftermaximumlegalratechange;

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
import org.apache.fineract.infrastructure.core.config.TaskExecutorConstant;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.loanaccount.data.LoanRescheduleData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.data.MaximumCreditRateConfigurationData;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.jetbrains.annotations.NotNull;
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
public class RecalculateInterestForMaximumLegalRateTasklet implements Tasklet {

    private static final int QUEUE_SIZE = 1;
    private final Queue<List<LoanRescheduleData>> queue = new ArrayDeque<>();
    private final ApplicationContext applicationContext;
    @Qualifier(TaskExecutorConstant.CONFIGURABLE_TASK_EXECUTOR_BEAN_NAME)
    private final ThreadPoolTaskExecutor taskExecutor;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanProductReadPlatformService loanProductReadPlatformService;

    @Override
    public RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext) throws Exception {
        final int threadPoolSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("thread-pool-size"));
        taskExecutor.setMaxPoolSize(threadPoolSize);
        taskExecutor.setCorePoolSize(threadPoolSize);
        final int batchSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("batch-size"));
        final int pageSize = batchSize * threadPoolSize;
        Long maximumLoanId = 0L;
        final MaximumCreditRateConfigurationData maximumCreditRateConfigurationData = this.loanProductReadPlatformService
                .retrieveMaximumCreditRateConfigurationData();

        long start = System.currentTimeMillis();
        log.info("Starting Recalculate Loan Interest After Maximum Legal Rate Change job");
        log.debug("Reading loans for processing!");
        List<LoanRescheduleData> loanScheduleInstallments = loanReadPlatformService
                .retrieveLoansForInterestRecalculation(maximumCreditRateConfigurationData, pageSize, maximumLoanId);
        if (loanScheduleInstallments != null && !loanScheduleInstallments.isEmpty()) {
            loanScheduleInstallments = Collections.synchronizedList(loanScheduleInstallments);
            long finish = System.currentTimeMillis();
            log.debug("Done fetching loans within {} milliseconds", finish - start);
            queue.add(loanScheduleInstallments);

            if (!CollectionUtils.isEmpty(queue)) {
                do {
                    List<LoanRescheduleData> queueElement = queue.element();
                    maximumLoanId = queueElement.get(queueElement.size() - 1).getId();
                    this.recalculateInterestForMaximumLegalRate(queue.remove(), threadPoolSize, pageSize, maximumLoanId,
                            maximumCreditRateConfigurationData);
                } while (!CollectionUtils.isEmpty(queue));
            }
        }
        return RepeatStatus.FINISHED;
    }

    private void recalculateInterestForMaximumLegalRate(List<LoanRescheduleData> loanRescheduleInstallments, int threadPoolSize,
            int pageSize, Long maxLoanId, MaximumCreditRateConfigurationData maximumCreditRateConfigurationData) {
        List<Callable<Void>> posters = new ArrayList<>();
        int fromIndex = 0;
        int size = loanRescheduleInstallments.size();
        int batchSize = (int) Math.ceil((double) size / threadPoolSize);

        if (batchSize == 0) {
            return;
        }

        int toIndex = (batchSize > size - 1) ? size : batchSize;
        while (toIndex < size
                && loanRescheduleInstallments.get(toIndex - 1).getId().equals(loanRescheduleInstallments.get(toIndex).getId())) {
            toIndex++;
        }
        boolean lastBatch = false;
        int loopCount = size / batchSize + 1;

        FineractContext context = ThreadLocalContextUtil.getContext();

        Callable<Void> fetchData = () -> {
            ThreadLocalContextUtil.init(context);
            Long maxId = maxLoanId;
            if (!queue.isEmpty()) {
                maxId = Math.max(maxLoanId, queue.element().get(queue.element().size() - 1).getId());
            }
            while (queue.size() <= QUEUE_SIZE) {

                log.debug("Fetching while threads are running!");
                List<LoanRescheduleData> loanRescheduleData = Collections.synchronizedList(this.loanReadPlatformService
                        .retrieveLoansForInterestRecalculation(maximumCreditRateConfigurationData, pageSize, maxId));
                if (loanRescheduleData.isEmpty()) {
                    break;
                }
                maxId = loanRescheduleData.get(loanRescheduleData.size() - 1).getId();
                queue.add(loanRescheduleData);
            }
            return null;
        };
        posters.add(fetchData);

        for (long i = 0; i < loopCount; i++) {
            List<LoanRescheduleData> subList = safeSubList(loanRescheduleInstallments, fromIndex, toIndex);
            RecalculateInterestForMLRProcessorTask recalculateInterestForMLRProcessorTask = applicationContext
                    .getBean(RecalculateInterestForMLRProcessorTask.class);
            recalculateInterestForMLRProcessorTask.setLoanRescheduleData(subList);
            recalculateInterestForMLRProcessorTask.setMaximumCreditRateConfigurationData(maximumCreditRateConfigurationData);
            recalculateInterestForMLRProcessorTask.setContext(ThreadLocalContextUtil.getContext());
            posters.add(recalculateInterestForMLRProcessorTask);

            if (lastBatch) {
                break;
            }
            if (toIndex + batchSize > size - 1) {
                lastBatch = true;
            }
            fromIndex = fromIndex + (toIndex - fromIndex);
            toIndex = (toIndex + batchSize > size - 1) ? size : toIndex + batchSize;
            while (toIndex < size
                    && loanRescheduleInstallments.get(toIndex - 1).getId().equals(loanRescheduleInstallments.get(toIndex).getId())) {
                toIndex++;
            }
        }

        List<Future<Void>> responses = new ArrayList<>();
        posters.forEach(poster -> responses.add(taskExecutor.submit(poster)));
        Long maxId = maxLoanId;
        if (!queue.isEmpty()) {
            maxId = Math.max(maxLoanId, queue.element().get(queue.element().size() - 1).getId());
        }

        while (queue.size() <= QUEUE_SIZE) {
            log.debug("Fetching while threads are running!..:: this is not supposed to run........");
            loanRescheduleInstallments = Collections.synchronizedList(this.loanReadPlatformService
                    .retrieveLoansForInterestRecalculation(maximumCreditRateConfigurationData, pageSize, maxId));
            if (loanRescheduleInstallments.isEmpty()) {
                break;
            }
            maxId = loanRescheduleInstallments.get(loanRescheduleInstallments.size() - 1).getId();
            log.debug("Add to the Queue");
            queue.add(loanRescheduleInstallments);
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
                log.info("Recalculate Loan Interest After Maximum Legal Rate Change Job Completed");
            }
        } catch (InterruptedException e1) {
            log.error("Interrupted while Recalculating Loan Interest After Maximum Legal Rate Change", e1);
        } catch (ExecutionException e2) {
            log.error("Execution exception while Recalculating Loan Interest After Maximum Legal Rate Change", e2);
        }
    }
}
