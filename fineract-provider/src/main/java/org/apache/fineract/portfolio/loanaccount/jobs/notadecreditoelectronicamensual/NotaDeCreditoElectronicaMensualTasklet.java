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
package org.apache.fineract.portfolio.loanaccount.jobs.notadecreditoelectronicamensual;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.portfolio.loanaccount.service.LoanCreditNoteReadService;
import org.apache.fineract.portfolio.loanaccount.service.LoanCreditNoteWriteService;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotaDeCreditoElectronicaMensualTasklet implements Tasklet {

    private final ConfigurationDomainService configurationDomainService;
    private final LoanCreditNoteReadService loanCreditNoteReadService;
    private final LoanCreditNoteWriteService loanCreditNoteWriteService;

    @Override
    @SuppressWarnings("all")
    public RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext) throws Exception {
        log.info("NotaDeCreditoElectronicaMensualTasklet execute method called");
        final LocalDate businessLocalDate = DateUtils.getBusinessLocalDate();
        final YearMonth yearMonth = YearMonth.from(businessLocalDate);
        final LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();
        final LocalDate secondLastDateOfMonth = lastDayOfMonth.minusDays(1);
        final boolean enableMonthlyInvoiceGenerationOnJobTrigger = this.configurationDomainService
                .enableMonthlyInvoiceGenerationOnJobTrigger();
        if (businessLocalDate.equals(secondLastDateOfMonth) || enableMonthlyInvoiceGenerationOnJobTrigger) {
            long start = System.currentTimeMillis();
            log.info("Starting NotaDeCreditoElectronicaMensualTasklet job for the date: {}", businessLocalDate);
            final List<Long> creditNoteIds = Collections
                    .synchronizedList(this.loanCreditNoteReadService.retrieveCreditNoteIdsForInvoiceProcessing());
            long finish = System.currentTimeMillis();
            log.info("Fetched credit notes with count of: {}", creditNoteIds.size());
            log.debug("Done fetching credit notes within {} milliseconds", finish - start);
            final List<Throwable> errors = new ArrayList<>();
            try {
                if (creditNoteIds.isEmpty()) {
                    log.info("No credit notes to process for Nota de Credito Electronica Mensual");
                }
                log.info("Processing Nota de Credito Electronica Mensual for credit note IDs: {}", StringUtils.join(creditNoteIds, ","));
                for (final Long creditNoteId : creditNoteIds) {
                    try {
                        this.loanCreditNoteWriteService.processInvoiceOffsetByCreditNote(creditNoteId);
                    } catch (Exception e) {
                        log.error("Failed to process credit note ID: {}", creditNoteId, e);
                        errors.add(e);
                    }
                }
            } catch (final ConstraintViolationException e) {
                Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
                for (ConstraintViolation<?> violation : violations) {
                    log.error("Failed to run Nota De Credito Electronica Mensual: invalid value for field {}: {}. Details: {}",
                            violation.getPropertyPath().toString(), violation.getInvalidValue(), violation.getMessage());
                }
                errors.add(e);
            } catch (final Exception e) {
                log.error("Failed to run Nota De Credito Electronica Mensual", e);
                errors.add(e);
            }
            if (!errors.isEmpty()) {
                log.error("Failed to run Nota De Credito Electronica Mensual", errors.get(0));
                throw new JobExecutionException(errors);
            }

        }
        log.info("Completed NotaDeCreditoElectronicaMensualTasklet job for the date: {}", businessLocalDate);
        return RepeatStatus.FINISHED;
    }

}
