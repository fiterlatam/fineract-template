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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.domain.JobProcessedEntityRepository;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.service.LoanChargeWritePlatformService;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Setter
public class ApplyChargeToOverdueLoanInstallmentProcessor {

    private List<OverdueLoanScheduleData> overdueLoanScheduledInstallments;
    private final LoanChargeWritePlatformService loanChargeWritePlatformService;
    private final ChargeRepositoryWrapper chargeRepository;
    private final JobProcessedEntityRepository jobProcessedEntityRepository;
    private final ConfigurationDomainService configurationService;

    @SuppressWarnings({ "squid:S3776" })
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = Exception.class)
    public void processOverdueCharges() throws JobExecutionException {
        List<Throwable> exceptions = new ArrayList<>();
        log.info("Applying Charges due for overdue loans for {} installments", this.overdueLoanScheduledInstallments.size());

        // Delete historical Job execution
        jobProcessedEntityRepository.deleteByJobIdAndExecutionDateBefore(12L, DateUtils.getLocalDateOfTenant().minusDays(2));

        // Check if we need to reprocess this job
        if (!configurationService.getJobApplyPenaltyToOverdueLoansSkipWhenReprocessed()) {
            jobProcessedEntityRepository.deleteByJobId(12L);
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

            for (Map.Entry<Long, Collection<OverdueLoanScheduleData>> entry : overdueScheduleData.entrySet()) {
                try {
                    this.loanChargeWritePlatformService.applyOverdueChargesForLoan(entry.getKey(), entry.getValue());

                } catch (final PlatformApiDataValidationException e) {
                    final List<ApiParameterError> errors = e.getErrors();
                    for (final ApiParameterError error : errors) {
                        log.error("Apply Charges due for overdue loans failed for account {} with message: {}", entry.getKey(),
                                error.getDeveloperMessage(), e);
                    }
                    exceptions.add(e);
                } catch (final AbstractPlatformDomainRuleException e) {
                    log.error("Apply Charges due for overdue loans failed for account {} with message: {}", entry.getKey(),
                            e.getDefaultUserMessage(), e);
                    exceptions.add(e);
                } catch (Exception e) {
                    log.error("Apply Charges due for overdue loans failed for account {}", entry.getKey(), e);
                    exceptions.add(e);
                }
            }
            if (!exceptions.isEmpty()) {
                throw new JobExecutionException(exceptions);
            }
        }
    }
}
