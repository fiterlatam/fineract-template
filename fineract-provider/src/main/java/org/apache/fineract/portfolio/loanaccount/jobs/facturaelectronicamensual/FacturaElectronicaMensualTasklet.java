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
package org.apache.fineract.portfolio.loanaccount.jobs.facturaelectronicamensual;

import java.time.LocalDate;
import java.time.YearMonth;
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
import org.apache.fineract.portfolio.loanaccount.invoice.data.LoanDocumentData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
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
public class FacturaElectronicaMensualTasklet implements Tasklet {

    private static final int QUEUE_SIZE = 1;
    private final Queue<List<LoanDocumentData>> queue = new ArrayDeque<>();
    private final ApplicationContext applicationContext;
    @Qualifier(TaskExecutorConstant.CONFIGURABLE_TASK_EXECUTOR_BEAN_NAME)
    private final ThreadPoolTaskExecutor taskExecutor;
    private final LoanReadPlatformService loanReadPlatformService;
    private boolean dataFetched = false;
    private final ConfigurationDomainService configurationDomainService;

    @Override
    public RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext) throws Exception {
        log.info("FacturaElectronicaMensualTasklet execute method called");
        final LocalDate businessLocalDate = DateUtils.getBusinessLocalDate();
        final YearMonth yearMonth = YearMonth.from(businessLocalDate);
        final LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();
        final LocalDate secondLastDayOfMonth = lastDayOfMonth.minusDays(1);
        final boolean enableMonthlyInvoiceGenerationOnJobTrigger = this.configurationDomainService
                .enableMonthlyInvoiceGenerationOnJobTrigger();
        final int threadPoolSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("thread-pool-size"));
        taskExecutor.setMaxPoolSize(threadPoolSize);
        taskExecutor.setCorePoolSize(threadPoolSize);
        final int batchSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("batch-size"));
        final int pageSize = batchSize * threadPoolSize;
        Long maxLoanIdInList = 0L;

        if (businessLocalDate.equals(secondLastDayOfMonth) || enableMonthlyInvoiceGenerationOnJobTrigger) {
            long start = System.currentTimeMillis();
            log.info("Starting FacturaElectronicaMensualTasklet job for the date: {}", businessLocalDate);
            List<LoanDocumentData> loanInvoiceDataList = this.loanReadPlatformService.retrieveLoanInvoiceDataList(pageSize, maxLoanIdInList,
                    secondLastDayOfMonth);
            log.info("Fetched LoanDocumentDataList with count of: {}", loanInvoiceDataList.size());
            if (loanInvoiceDataList != null && !loanInvoiceDataList.isEmpty()) {
                loanInvoiceDataList = Collections.synchronizedList(loanInvoiceDataList);
                long finish = System.currentTimeMillis();
                log.debug("Done fetching LoanDocumentDataList within {} milliseconds", finish - start);
                queue.add(loanInvoiceDataList);

                if (!CollectionUtils.isEmpty(queue)) {
                    do {
                        int totalFilteredRecords = loanInvoiceDataList.size();
                        log.info("Starting FacturaElectronicaMensualTasklet invoice processing - total records - {}", totalFilteredRecords);
                        List<LoanDocumentData> queueElement = queue.element();
                        maxLoanIdInList = queueElement.get(queueElement.size() - 1).getLoanId();
                        this.processInvoices(queue.remove(), threadPoolSize, secondLastDayOfMonth, pageSize, maxLoanIdInList);
                    } while (!CollectionUtils.isEmpty(queue));
                }
            }
        }
        log.info("Completed FacturaElectronicaMensualTasklet job for the date: {}", businessLocalDate);
        return RepeatStatus.FINISHED;
    }

    private void processInvoices(List<LoanDocumentData> loanInvoiceDataList, int threadPoolSize, LocalDate secondLastDayOfMonth,
            int pageSize, Long maxLoanIdInList) {
        dataFetched = false;
        List<Callable<Void>> posters = new ArrayList<>();
        int fromIndex = 0;
        int size = loanInvoiceDataList.size();
        int batchSize = (int) Math.ceil((double) size / threadPoolSize);

        if (batchSize == 0) {
            return;
        }

        int toIndex = (batchSize > size - 1) ? size : batchSize;
        while (toIndex < size && loanInvoiceDataList.get(toIndex - 1).getLoanId().equals(loanInvoiceDataList.get(toIndex).getLoanId())) {
            toIndex++;
        }
        boolean lastBatch = false;
        int loopCount = size / batchSize + 1;

        FineractContext context = ThreadLocalContextUtil.getContext();

        Callable<Void> fetchData = () -> {
            ThreadLocalContextUtil.init(context);
            Long maxId = maxLoanIdInList;
            if (!queue.isEmpty()) {
                maxId = Math.max(maxLoanIdInList, queue.element().get(queue.element().size() - 1).getLoanId());
            }

            while (queue.size() <= QUEUE_SIZE) {
                log.info("Fetching while threads are running!");
                List<LoanDocumentData> loanDocumentData = Collections
                        .synchronizedList(this.loanReadPlatformService.retrieveLoanInvoiceDataList(pageSize, maxId, secondLastDayOfMonth));
                log.info("Fetched LoanDocumentDataList with count of: {}", loanDocumentData.size());
                if (loanDocumentData.isEmpty()) {
                    log.info("No more loanDocumentData to process");
                    break;
                }
                maxId = loanDocumentData.get(loanDocumentData.size() - 1).getLoanId();
                queue.add(loanDocumentData);
            }
            dataFetched = true;
            return null;
        };
        posters.add(fetchData);

        for (long i = 0; i < loopCount; i++) {
            List<LoanDocumentData> subList = safeSubList(loanInvoiceDataList, fromIndex, toIndex);
            FacturaElectronicaMensualPosterTask facturaElectronicaMensualPosterTask = applicationContext
                    .getBean(FacturaElectronicaMensualPosterTask.class);
            facturaElectronicaMensualPosterTask.setLoanInvoiceDataList(subList);
            facturaElectronicaMensualPosterTask.setContext(ThreadLocalContextUtil.getContext());
            posters.add(facturaElectronicaMensualPosterTask);

            if (lastBatch) {
                log.info("Last batch processed");
                break;
            }
            if (toIndex + batchSize > size - 1) {
                lastBatch = true;
            }
            fromIndex = fromIndex + (toIndex - fromIndex);
            toIndex = (toIndex + batchSize > size - 1) ? size : toIndex + batchSize;
            while (toIndex < size
                    && loanInvoiceDataList.get(toIndex - 1).getLoanId().equals(loanInvoiceDataList.get(toIndex).getLoanId())) {
                toIndex++;
            }
        }

        List<Future<Void>> responses = new ArrayList<>();
        posters.forEach(poster -> responses.add(taskExecutor.submit(poster)));
        // delay as data is being fetched.
        while (!dataFetched) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Error while waiting for data fetch", e);
            }
        }

        log.info("Data fetched, checking completion...");
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
                log.info("FacturaElectronicaMensualTasklet Job: all threads executed for this batch!!!");
            }
        } catch (InterruptedException e1) {
            log.error("Interrupted while processing invoices for FacturaElectronicaMensualTasklet", e1);
        } catch (ExecutionException e2) {
            log.error("Execution exception while processing invoices for FacturaElectronicaMensualTasklet", e2);
        }
    }
}
