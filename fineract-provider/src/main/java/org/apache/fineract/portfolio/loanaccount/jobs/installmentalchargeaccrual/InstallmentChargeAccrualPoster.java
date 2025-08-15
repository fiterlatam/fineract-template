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
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformService;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Setter
public class InstallmentChargeAccrualPoster {

    private List<Long> loanIds;
    private LocalDate accrualDate;
    private Long minimumDaysInArrearsToSuspendLoanAccount;
    private final LoanWritePlatformService loanWritePlatformService;

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = Exception.class)
    public void postInstallmentChargeAccruals() throws JobExecutionException {
        List<Throwable> errors = new ArrayList<>();
        if (!loanIds.isEmpty()) {
            log.info("Running Devengo de seguro Job for loans batch with maximum loanId {}", loanIds.get(loanIds.size() - 1));
            for (Long loanId : loanIds) {
                try {
                    this.loanWritePlatformService.persistInstallmentalChargeAccrual(loanId, accrualDate,
                            minimumDaysInArrearsToSuspendLoanAccount, false);
                } catch (Exception e) {
                    log.error("Failed to run Devengo de seguro Job for loan id {}", loanId, e);
                    errors.add(e);
                }
            }
            if (!errors.isEmpty()) {
                log.error("Failed to run Devengo de seguro Job for loans on {}", accrualDate, errors.get(0));
                throw new JobExecutionException(errors);
            }
            log.info("Completed Devengo de seguro Job for loans batch with maximum loanId {}", loanIds.get(loanIds.size() - 1));
        }
    }
}
