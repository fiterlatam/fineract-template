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
package org.apache.fineract.portfolio.savings.jobs.postinterestforsavings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsSchedularInterestPoster;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

import static org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType.ACTIVE;

@RequiredArgsConstructor
@Slf4j
@Component
public class PostInterestForSavingsWorkerTasklet implements Tasklet {

    private final SavingsAccountReadPlatformService savingAccountReadPlatformService;
    private final ConfigurationDomainService configurationDomainService;
    private final ApplicationContext applicationContext;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        log.info(" ===== Starting Interest posting for savings account partition tasket ===== ");
        ExecutionContext executionContext = contribution.getStepExecution().getExecutionContext();
        List<Long> savingsAccountIds = (List<Long>) executionContext.get("savingsAccountIdsForInterestPostingWorker");

        //final int threadPoolSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("thread-pool-size"));
        final int threadPoolSize = 10; // this needs to comme from property
        //final int batchSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("batch-size"));
        //final int pageSize = batchSize * threadPoolSize;
        final ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        final boolean backdatedTxnsAllowedTill = this.configurationDomainService.retrievePivotDateConfig();

        long start = System.currentTimeMillis();

        log.debug("Reading Savings Account Data!");
        List<SavingsAccountData> savingsAccounts = savingAccountReadPlatformService
                .retrieveSavingsDataForForInterestPostingByIds(backdatedTxnsAllowedTill, ACTIVE.getValue(), savingsAccountIds);

        if (savingsAccounts != null && savingsAccounts.size() > 0) {
            savingsAccounts = Collections.synchronizedList(savingsAccounts);
            long finish = System.currentTimeMillis();
            log.debug("Done fetching Data within {} milliseconds", finish - start);

            int totalFilteredRecords = savingsAccounts.size();
            log.info("Starting Interest posting - total records - {}", totalFilteredRecords);
            postInterest(savingsAccounts, threadPoolSize, executorService, backdatedTxnsAllowedTill);

            executorService.shutdownNow();
        }
        return RepeatStatus.FINISHED;
    }

    private void postInterest(List<SavingsAccountData> savingsAccounts, int threadPoolSize, ExecutorService executorService,
                              final boolean backdatedTxnsAllowedTill) {
        List<Callable<Void>> posters = new ArrayList<>();
        int fromIndex = 0;
        int size = savingsAccounts.size();
        int batchSize = (int) Math.ceil((double) size / threadPoolSize);

        if (batchSize == 0) {
            return;
        }

        int toIndex = (batchSize > size - 1) ? size : batchSize;
        while (toIndex < size && savingsAccounts.get(toIndex - 1).getId().equals(savingsAccounts.get(toIndex).getId())) {
            toIndex++;
        }
        boolean lastBatch = false;
        int loopCount = size / batchSize + 1;

        for (long i = 0; i < loopCount; i++) {
            List<SavingsAccountData> subList = safeSubList(savingsAccounts, fromIndex, toIndex);
            SavingsSchedularInterestPoster savingsSchedularInterestPoster = applicationContext
                    .getBean(SavingsSchedularInterestPoster.class);
            savingsSchedularInterestPoster.setSavingAccounts(subList);
            savingsSchedularInterestPoster.setBackdatedTxnsAllowedTill(backdatedTxnsAllowedTill);
            savingsSchedularInterestPoster.setContext(ThreadLocalContextUtil.getContext());

            posters.add(savingsSchedularInterestPoster);

            if (lastBatch) {
                break;
            }
            if (toIndex + batchSize > size - 1) {
                lastBatch = true;
            }
            fromIndex = fromIndex + (toIndex - fromIndex);
            toIndex = (toIndex + batchSize > size - 1) ? size : toIndex + batchSize;
            while (toIndex < size && savingsAccounts.get(toIndex - 1).getId().equals(savingsAccounts.get(toIndex).getId())) {
                toIndex++;
            }
        }

        try {
            List<Future<Void>> responses = executorService.invokeAll(posters);
            checkCompletion(responses);
        } catch (InterruptedException e1) {
            log.error("Interrupted while postInterest", e1);
        }
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
            }
        } catch (InterruptedException e1) {
            log.error("Interrupted while interest posting entries", e1);
        } catch (ExecutionException e2) {
            log.error("Execution exception while interest posting entries", e2);
        }
    }
}