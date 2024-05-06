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
package org.apache.fineract.custom.portfolio.externalcharge.honoratio.job;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.data.CustomChargeHonorarioMapData;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.service.CustomChargeHonorarioMapReadWritePlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import retrofit2.Retrofit;

@Slf4j
@RequiredArgsConstructor
@AllArgsConstructor
public class CustomChargeHonorarioMapTasklet implements Tasklet {

    private final ConfigurationDomainService configurationDomainService;

    private Retrofit retrofit;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final LoanRepository loanRepository;
    private final CustomChargeHonorarioMapReadWritePlatformService customChargeHonorarioMapReadWritePlatformService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        List<Throwable> exceptions = new ArrayList<>();
        List<CustomChargeHonorarioMapData> honorarioToPersistList = new ArrayList<>();

        List<Long> activeLoans = loanRepository.findAllNonClosedLoanIds();

        for (Long currentLoanId : activeLoans) {
            customChargeHonorarioMapReadWritePlatformService.executeJobLoanCustomChargeHonorarioUpdate(exceptions, currentLoanId);
        }

        if (!exceptions.isEmpty()) {
            throw new JobExecutionException(exceptions);
        }

        return RepeatStatus.FINISHED;
    }
}
