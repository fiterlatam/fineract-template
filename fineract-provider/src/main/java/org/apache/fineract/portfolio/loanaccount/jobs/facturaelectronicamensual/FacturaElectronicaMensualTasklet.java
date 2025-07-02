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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.config.TaskExecutorConstant;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterization;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterizationRepository;
import org.apache.fineract.portfolio.loanproductparameterization.exception.LoanProductParameterizationNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class FacturaElectronicaMensualTasklet implements Tasklet {

    private static final int QUEUE_SIZE = 1;
    private final Queue<List<Long>> queue = new ArrayDeque<>();
    private final ApplicationContext applicationContext;
    @Qualifier(TaskExecutorConstant.CONFIGURABLE_TASK_EXECUTOR_BEAN_NAME)
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ClientReadPlatformService clientReadPlatformService;
    private boolean dataFetched = false;
    private final ConfigurationDomainService configurationDomainService;
    private final LoanProductParameterizationRepository productParameterizationRepository;
    private final JdbcTemplate jdbcTemplate;

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
        log.info("Thread pool size for FacturaElectronicaMensualTasklet: {}", threadPoolSize);
        taskExecutor.setMaxPoolSize(threadPoolSize);
        taskExecutor.setCorePoolSize(threadPoolSize);
        final int batchSize = Integer.parseInt((String) chunkContext.getStepContext().getJobParameters().get("batch-size"));
        log.info("Batch size for FacturaElectronicaMensualTasklet: {}", batchSize);
        final int pageSize = batchSize * threadPoolSize;
        log.info("Page size for FacturaElectronicaMensualTasklet: {}", pageSize);
        Long maxClientIdInList = 0L;
        if (businessLocalDate.equals(secondLastDayOfMonth) || enableMonthlyInvoiceGenerationOnJobTrigger) {
            long start = System.currentTimeMillis();
            log.info("Starting FacturaElectronicaMensualTasklet job for the date: {}", businessLocalDate);
            List<Long> clientIds = this.clientReadPlatformService.retrieveClientIdsForInvoiceProcessing(maxClientIdInList,
                    secondLastDayOfMonth, pageSize);
            log.info("Fetched client_ids with count of: {}", clientIds.size());
            if (!clientIds.isEmpty()) {
                clientIds = Collections.synchronizedList(clientIds);
                long finish = System.currentTimeMillis();
                log.debug("Done fetching client_ids within {} milliseconds", finish - start);
                queue.add(clientIds);

                if (!CollectionUtils.isEmpty(queue)) {
                    do {
                        int totalFilteredRecords = clientIds.size();
                        log.info("Starting FacturaElectronicaMensualTasklet invoice processing - total records - {}", totalFilteredRecords);
                        List<Long> queueElement = queue.element();
                        maxClientIdInList = queueElement.get(queueElement.size() - 1);
                        this.processInvoices(queue.remove(), threadPoolSize, secondLastDayOfMonth, pageSize, maxClientIdInList);
                    } while (!CollectionUtils.isEmpty(queue));
                }
            }
        }
        this.adjustElectronicInvoiceDocumentNumbers();
        log.info("Completed FacturaElectronicaMensualTasklet job for the date: {}", businessLocalDate);
        return RepeatStatus.FINISHED;
    }

    private void processInvoices(List<Long> clientIdList, int threadPoolSize, LocalDate secondLastDayOfMonth, int pageSize,
            Long maxClientIdInList) {
        dataFetched = false;
        List<Callable<Void>> posters = new ArrayList<>();
        int fromIndex = 0;
        int size = clientIdList.size();
        int batchSize = (int) Math.ceil((double) size / threadPoolSize);

        if (batchSize == 0) {
            return;
        }

        int toIndex = (batchSize > size - 1) ? size : batchSize;
        while (toIndex < size && clientIdList.get(toIndex - 1).equals(clientIdList.get(toIndex))) {
            toIndex++;
        }
        boolean lastBatch = false;
        int loopCount = size / batchSize + 1;

        FineractContext context = ThreadLocalContextUtil.getContext();

        Callable<Void> fetchData = () -> {
            ThreadLocalContextUtil.init(context);
            Long maxId = maxClientIdInList;
            if (!queue.isEmpty()) {
                maxId = Math.max(maxClientIdInList, queue.element().get(queue.element().size() - 1));
            }

            while (queue.size() <= QUEUE_SIZE) {
                log.info("Fetching while threads are running!");
                List<Long> clientIds = Collections.synchronizedList(
                        this.clientReadPlatformService.retrieveClientIdsForInvoiceProcessing(maxId, secondLastDayOfMonth, pageSize));
                log.info("Fetched client_ids with count of: {}", clientIds.size());
                if (clientIds.isEmpty()) {
                    log.info("No more loanDocumentData to process");
                    break;
                }
                maxId = clientIds.get(clientIds.size() - 1);
                queue.add(clientIds);
            }
            dataFetched = true;
            return null;
        };
        posters.add(fetchData);

        for (long i = 0; i < loopCount; i++) {
            List<Long> subList = safeSubList(clientIdList, fromIndex, toIndex);
            FacturaElectronicaMensualPosterTask facturaElectronicaMensualPosterTask = applicationContext
                    .getBean(FacturaElectronicaMensualPosterTask.class);
            facturaElectronicaMensualPosterTask.setClientIds(subList);
            facturaElectronicaMensualPosterTask.setSecondLastDayOfMonth(secondLastDayOfMonth);
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
            while (toIndex < size && clientIdList.get(toIndex - 1).equals(clientIdList.get(toIndex))) {
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

    @SuppressWarnings("all")
    private void adjustElectronicInvoiceDocumentNumbers() {
        log.info("Start adjusting electronic invoice document numbers");
        final List<LoanProductParameterization> productParameterizations = productParameterizationRepository.findAll();
        final List<ElectronicInvoiceDto> temporaryElectronicInvoices = this.findTemporaryElectronicInvoices();
        log.info("Found {} electronic invoices to adjust", temporaryElectronicInvoices.size());
        if (!productParameterizations.isEmpty() && !temporaryElectronicInvoices.isEmpty()) {
            final Map<String, List<ElectronicInvoiceDto>> documentNumberElectronicInvoicesMap = new HashMap<>();
            for (final ElectronicInvoiceDto electronicInvoiceDto : temporaryElectronicInvoices) {
                final String mapKey = electronicInvoiceDto.getNumeroDoc();
                if (mapKey != null && !mapKey.isEmpty()) {
                    if (!documentNumberElectronicInvoicesMap.containsKey(mapKey)) {
                        documentNumberElectronicInvoicesMap.put(mapKey, new ArrayList<>(List.of(electronicInvoiceDto)));
                    } else {
                        log.info("Adjusting invoice numero_doc: Found duplicate document number: {} for client ID: {}", mapKey,
                                electronicInvoiceDto.getClientId());
                        final List<ElectronicInvoiceDto> existingList = documentNumberElectronicInvoicesMap.get(mapKey);
                        existingList.add(electronicInvoiceDto);
                    }
                }
            }
            log.info("Found {} unique document numbers to adjust", documentNumberElectronicInvoicesMap.size());
            for (final Map.Entry<String, List<ElectronicInvoiceDto>> documentNumberEntry : documentNumberElectronicInvoicesMap.entrySet()) {
                final List<ElectronicInvoiceDto> electronicInvoices = documentNumberEntry.getValue();
                if (!electronicInvoices.isEmpty()) {
                    final String documentNumber = documentNumberEntry.getKey();
                    final String productType = electronicInvoices.get(0).getTipoProd();
                    final LoanProductParameterization loanProductParameterization = productParameterizations.stream()
                            .filter(param -> param.getProductType().equals(productType)).findFirst()
                            .orElseThrow(() -> new LoanProductParameterizationNotFoundException(productType));
                    final String nextDocumentNumber = nextDocumentNumber(loanProductParameterization);
                    log.info("Adjusting document number from {} to {} for product type {}", documentNumber, nextDocumentNumber,
                            productType);
                    for (final ElectronicInvoiceDto electronicInvoiceDto : electronicInvoices) {
                        electronicInvoiceDto.setNumeroDoc(nextDocumentNumber);
                    }
                }
            }
            final List<Object[]> batchArgs = temporaryElectronicInvoices.stream()
                    .map(dto -> new Object[] { dto.getNumeroDoc(), dto.getId() }).collect(Collectors.toList());
            final int[] updateCounts = this.jdbcTemplate.batchUpdate("UPDATE c_facturacion_electronica SET numero_doc = ? WHERE id = ?",
                    batchArgs);
            log.info("Adjust numero_doc:: Updated {} records in batch", updateCounts.length);
            this.productParameterizationRepository.saveAllAndFlush(productParameterizations);
        }
        log.info("Finished adjusting electronic invoice document numbers");
    }

    private String nextDocumentNumber(final LoanProductParameterization loanProductParameterization) {
        final long rangeStartNumber = loanProductParameterization.getRangeStartNumber();
        final long invoiceCounter = loanProductParameterization.getInvoiceCounter();
        final long rangeEndNumber = loanProductParameterization.getRangeEndNumber();
        final long currentCounter = ObjectUtils.defaultIfNull(invoiceCounter, 0L) + 1L;
        final long documentNumber = rangeStartNumber + invoiceCounter;
        if (currentCounter > rangeEndNumber) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.invoice.counter.exceeds.range.end.number",
                    String.format("Invoice counter exceeds the range end number: %s and product type: %s", rangeEndNumber,
                            loanProductParameterization.getProductType()));
        }
        loanProductParameterization.setInvoiceCounter(currentCounter);
        return String.valueOf(documentNumber);
    }

    public List<ElectronicInvoiceDto> findTemporaryElectronicInvoices() {
        final String sql = "SELECT cfe.id, cfe.numero_doc, cfe.id_cliente, cfe.tipo_prod FROM c_facturacion_electronica cfe WHERE cfe.numero_doc LIKE 'TEMPORARY%'";
        RowMapper<ElectronicInvoiceDto> rowMapper = (rs, rowNum) -> new ElectronicInvoiceDto(rs.getLong("id"), rs.getString("numero_doc"),
                rs.getString("id_cliente"), rs.getString("tipo_prod"));
        return this.jdbcTemplate.query(sql, rowMapper);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ElectronicInvoiceDto {

        private Long id;
        private String numeroDoc;
        private String clientId;
        private String tipoProd;
    }
}
