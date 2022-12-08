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
package org.apache.fineract.portfolio.savings.jobs.postaccrualinterestforsavings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

@Component
public class PostAccrualStep1TaskletForPartitionJob implements Tasklet {

    private static final Logger LOG = LoggerFactory.getLogger(PostAccrualStep1TaskletForPartitionJob.class);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<Long> activeSavingsAccounts = Collections.unmodifiableList(LongStream.rangeClosed(1, 25).boxed().toList());
        LOG.info("Total number of active saving accounts for post accrual job is : " + activeSavingsAccounts.size());
        contribution.getStepExecution().getJobExecution().getExecutionContext().put("activeSavingAccountIds", activeSavingsAccounts);//// needs to come from properties
        return RepeatStatus.FINISHED;
    }
}
