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
package org.apache.fineract.portfolio.loanaccount.jobs.installmentalchargeaccrual;

import java.time.LocalDate;
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
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
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
public class InstallmentChargeAccrualTasklet implements Tasklet {

    private static final int QUEUE_SIZE = 1;
    private final Queue<List<Long>> queue = new ArrayDeque<>();
    private final ApplicationContext applicationContext;
    @Qualifier(TaskExecutorConstant.CONFIGURABLE_TASK_EXECUTOR_BEAN_NAME)
    private final ThreadPoolTaskExecutor taskExecutor;
    private final LoanReadPlatformService loanReadPlatformService;
    private final ConfigurationDomainService configurationDomainService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        final int threadPoolSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("thread-pool-size"));
        taskExecutor.setCorePoolSize(threadPoolSize);
        taskExecutor.setMaxPoolSize(threadPoolSize);
        final int batchSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("batch-size"));
        final int pageSize = batchSize * threadPoolSize;
        Long maxLoanIdInList = 0L;

        LocalDate accrualDate = DateUtils.getLocalDateOfTenant();

        long start = System.currentTimeMillis();
        log.info("Starting Installment Charge Accrual posting for the date: {}", accrualDate);
        log.debug("Reading Load Ids for installment charge accrual processing!");
        List<Long> loanIds = this.loanReadPlatformService.retrieveIdsForActiveLoans(pageSize, maxLoanIdInList);
        if (loanIds != null && loanIds.size() > 0) {
            loanIds = Collections.synchronizedList(loanIds);
            long finish = System.currentTimeMillis();
            log.debug("Done fetching Loan Ids within {} milliseconds", finish - start);
            queue.add(loanIds);

            if (!CollectionUtils.isEmpty(queue)) {
                do {
                    int totalFilteredRecords = loanIds.size();
                    log.debug("Starting Installment Charge Accrual posting - total records - {}", totalFilteredRecords);
                    List<Long> queueElement = queue.element();
                    maxLoanIdInList = queueElement.get(queueElement.size() - 1);
                    this.postInstallmentChargeAccruals(queue.remove(), threadPoolSize, accrualDate, pageSize, maxLoanIdInList);
                } while (!CollectionUtils.isEmpty(queue));
            }
        }
        return RepeatStatus.FINISHED;
    }

    private void postInstallmentChargeAccruals(List<Long> loanIds, int threadPoolSize, LocalDate accrualDate, int pageSize,
            Long maxLoanIdInList) {
        List<Callable<Void>> posters = new ArrayList<>();
        int fromIndex = 0;
        int size = loanIds.size();
        int batchSize = (int) Math.ceil((double) size / threadPoolSize);

        if (batchSize == 0) {
            return;
        }

        int toIndex = (batchSize > size - 1) ? size : batchSize;
        while (toIndex < size && loanIds.get(toIndex - 1).equals(loanIds.get(toIndex))) {
            toIndex++;
        }
        boolean lastBatch = false;
        int loopCount = size / batchSize + 1;

        FineractContext context = ThreadLocalContextUtil.getContext();

        Callable<Void> fetchData = () -> {
            ThreadLocalContextUtil.init(context);
            Long maxId = maxLoanIdInList;
            if (!queue.isEmpty()) {
                maxId = Math.max(maxLoanIdInList, queue.element().get(queue.element().size() - 1));
            }

            while (queue.size() <= QUEUE_SIZE) {
                log.debug("Fetching while threads are running!");
                List<Long> loanIdList = Collections
                        .synchronizedList(this.loanReadPlatformService.retrieveIdsForActiveLoans(pageSize, maxId));
                if (loanIdList.isEmpty()) {
                    break;
                }
                maxId = loanIdList.get(loanIdList.size() - 1);
                queue.add(loanIdList);
            }
            return null;
        };
        posters.add(fetchData);

        for (long i = 0; i < loopCount; i++) {
            List<Long> subList = safeSubList(loanIds, fromIndex, toIndex);
            InstallmentChargeAccrualPosterTask installmentChargeAccrualPosterTask = applicationContext
                    .getBean(InstallmentChargeAccrualPosterTask.class);
            installmentChargeAccrualPosterTask.setLoanIds(subList);
            installmentChargeAccrualPosterTask.setAccrualDate(accrualDate);
            installmentChargeAccrualPosterTask.setMinimumDaysInArrearsToSuspendLoanAccount(
                    this.configurationDomainService.retriveMinimumDaysInArrearsToSuspendLoanAccount());
            installmentChargeAccrualPosterTask.setContext(ThreadLocalContextUtil.getContext());
            posters.add(installmentChargeAccrualPosterTask);

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

        List<Future<Void>> responses = new ArrayList<>();
        posters.forEach(poster -> responses.add(taskExecutor.submit(poster)));
        Long maxId = maxLoanIdInList;
        if (!queue.isEmpty()) {
            maxId = Math.max(maxLoanIdInList, queue.element().get(queue.element().size() - 1));
        }

        while (queue.size() <= QUEUE_SIZE) {
            log.debug("Fetching while threads are running!..:: this is not supposed to run........");
            loanIds = Collections.synchronizedList(this.loanReadPlatformService.retrieveIdsForActiveLoans(pageSize, maxId));
            if (loanIds.isEmpty()) {
                break;
            }
            maxId = loanIds.get(loanIds.size() - 1);
            log.debug("Add to the Queue");
            queue.add(loanIds);
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
                log.info("Installment Charge Accrual Posting Job Completed");
            }
        } catch (InterruptedException e1) {
            log.error("Interrupted while processing installment charge accruals", e1);
        } catch (ExecutionException e2) {
            log.error("Execution exception while processing installment charge accruals", e2);
        }
    }
}
