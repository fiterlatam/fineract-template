package org.apache.fineract.portfolio.loanaccount.jobs.archiveloanhistory;

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
import org.apache.fineract.infrastructure.core.config.TaskExecutorConstant;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.loanaccount.data.LoanArchiveHistoryData;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.service.LoanArchiveHistoryReadWritePlatformService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ArchiveLoansHistoryTasklet implements Tasklet {

    private final LoanArchiveHistoryReadWritePlatformService loanArchiveHistoryService;
    private final JdbcTemplate jdbcTemplate;
    private static final int QUEUE_SIZE = 1;
    private final Queue<List<LoanArchiveHistoryData>> queue = new ArrayDeque<>();
    private final ApplicationContext applicationContext;
    @Qualifier(TaskExecutorConstant.CONFIGURABLE_TASK_EXECUTOR_BEAN_NAME)
    private final ThreadPoolTaskExecutor taskExecutor;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        log.info("Archive Loans History Tasklet:: Available processors: {}", availableProcessors);
        final int threadPoolSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("thread-pool-size"));
        taskExecutor.setMaxPoolSize(threadPoolSize);
        taskExecutor.setCorePoolSize(threadPoolSize);
        final int batchSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("batch-size"));
        final int pageSize = batchSize * threadPoolSize;
        int maxClientIdInList = 0;

        long start = System.currentTimeMillis();
        LocalDate archiveDate = DateUtils.getLocalDateOfTenant();
        log.info("Running Archivo de cartera for date: {}", archiveDate);
        this.loanArchiveHistoryService.truncateLoanHistory();
        log.info("Reading Loans for archiving!");
        List<LoanArchiveHistoryData> listLoan = loanArchiveHistoryService.getLoanArchiveCollectionData(maxClientIdInList, pageSize);
        if (listLoan != null && !listLoan.isEmpty()) {
            listLoan = Collections.synchronizedList(listLoan);
            long finish = System.currentTimeMillis();
            log.debug("Done fetching Loans within {} milliseconds", finish - start);
            queue.add(listLoan);

            if (!CollectionUtils.isEmpty(queue)) {
                do {
                    List<LoanArchiveHistoryData> queueElement = queue.element();
                    maxClientIdInList = queueElement.get(queueElement.size() - 1).getIdentificacion();
                    this.archiveLoans(queue.remove(), threadPoolSize, pageSize, maxClientIdInList);
                } while (!CollectionUtils.isEmpty(queue));
            }
        }
        return RepeatStatus.FINISHED;
    }

    private void archiveLoans(List<LoanArchiveHistoryData> loansForArchival, int threadPoolSize, int pageSize, int maxClientIdInList) {
        List<Callable<Void>> posters = new ArrayList<>();
        int fromIndex = 0;
        int size = loansForArchival.size();
        int batchSize = (int) Math.ceil((double) size / threadPoolSize);

        if (batchSize == 0) {
            return;
        }

        int toIndex = (batchSize > size - 1) ? size : batchSize;
        while (toIndex < size
                && loansForArchival.get(toIndex - 1).getIdentificacion().equals(loansForArchival.get(toIndex).getIdentificacion())) {
            toIndex++;
        }
        boolean lastBatch = false;
        int loopCount = size / batchSize + 1;

        FineractContext context = ThreadLocalContextUtil.getContext();

        Callable<Void> fetchData = () -> {
            ThreadLocalContextUtil.init(context);
            int maxId = maxClientIdInList;
            if (!queue.isEmpty()) {
                maxId = Math.max(maxClientIdInList, queue.element().get(queue.element().size() - 1).getIdentificacion());
            }

            while (queue.size() <= QUEUE_SIZE) {
                log.info("Fetching while threads are running!");
                List<LoanArchiveHistoryData> loanArchiveHistoryData = Collections
                        .synchronizedList(this.loanArchiveHistoryService.getLoanArchiveCollectionData(maxId, pageSize));
                if (loanArchiveHistoryData.isEmpty()) {
                    break;
                }
                maxId = loanArchiveHistoryData.get(loanArchiveHistoryData.size() - 1).getIdentificacion();
                log.info("Add to the Queue");
                queue.add(loanArchiveHistoryData);
            }
            return null;
        };
        posters.add(fetchData);

        for (long i = 0; i < loopCount; i++) {
            List<LoanArchiveHistoryData> subList = safeSubList(loansForArchival, fromIndex, toIndex);
            LoanHistoryArchivalTask loanHistoryArchivalTask = applicationContext.getBean(LoanHistoryArchivalTask.class);
            loanHistoryArchivalTask.setLoansForArchival(subList);
            loanHistoryArchivalTask.setContext(ThreadLocalContextUtil.getContext());
            posters.add(loanHistoryArchivalTask);

            if (lastBatch) {
                break;
            }
            if (toIndex + batchSize > size - 1) {
                lastBatch = true;
            }
            fromIndex = fromIndex + (toIndex - fromIndex);
            toIndex = (toIndex + batchSize > size - 1) ? size : toIndex + batchSize;
            while (toIndex < size && loansForArchival.get(toIndex - 1).equals(loansForArchival.get(toIndex))) {
                toIndex++;
            }
        }

        List<Future<Void>> responses = new ArrayList<>();
        posters.forEach(poster -> responses.add(taskExecutor.submit(poster)));
        int maxId = maxClientIdInList;
        if (!queue.isEmpty()) {
            maxId = Math.max(maxClientIdInList, queue.element().get(queue.element().size() - 1).getIdentificacion());
        }

        while (queue.size() <= QUEUE_SIZE) {
            log.info("Fetching while threads are running!..:: this is not supposed to run........");
            loansForArchival = Collections.synchronizedList(this.loanArchiveHistoryService.getLoanArchiveCollectionData(maxId, pageSize));
            if (loansForArchival.isEmpty()) {
                break;
            }
            maxId = loansForArchival.get(loansForArchival.size() - 1).getIdentificacion();
            log.info("Add to the Queue");
            queue.add(loansForArchival);
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
                log.info("Archivo de cartera Job Completed");
            }
        } catch (InterruptedException e1) {
            log.error("Interrupted while running Archivo de cartera", e1);
        } catch (ExecutionException e2) {
            log.error("Execution exception while running Archivo de cartera", e2);
        }
    }

}
