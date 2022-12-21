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
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

import static org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType.ACTIVE;

@Slf4j
@RequiredArgsConstructor
public class FetchSavingsAccountIdsTasklet implements Tasklet {

    private final SavingsAccountReadPlatformService savingsAccountReadPlatformService;
    private final ConfigurationDomainService configurationDomainService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        final boolean backdatedTxnsAllowedTill = this.configurationDomainService.retrievePivotDateConfig();
        log.debug("Reading Savings Account Ids!");
        List<Long> savingsAccountIds = savingsAccountReadPlatformService
                .retrieveAllSavingsAccountIdsForInterestPosting(backdatedTxnsAllowedTill, ACTIVE.getValue());
        contribution.getStepExecution().getJobExecution().getExecutionContext().put("savingsAccountIdsForInterestPosting", savingsAccountIds);
        return RepeatStatus.FINISHED;
    }
}
