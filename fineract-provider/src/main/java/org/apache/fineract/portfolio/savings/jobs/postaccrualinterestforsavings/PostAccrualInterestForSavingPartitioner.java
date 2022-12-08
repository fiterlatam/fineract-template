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

import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Component
public class PostAccrualInterestForSavingPartitioner implements Partitioner {

    public static final String PARTITION_PREFIX = "partition_";

    private final List<Long> savingsAccountIds;

    public PostAccrualInterestForSavingPartitioner(List<Long> savingsAccountIds){
        this.savingsAccountIds = savingsAccountIds;
    }

    @NotNull
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        int totalSavingsAccounts = savingsAccountIds.size();
        int partitionSize = totalSavingsAccounts / gridSize + 1;
        return getPartitions(partitionSize);
    }

    @SuppressWarnings("unchecked")
    private Map<String, ExecutionContext> getPartitions(int partitionSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();

        if (CollectionUtils.isEmpty(savingsAccountIds)) {
            return Map.of();
        }
        int partitionIndex = 1;
        int remainingSpace = 0;
        createNewPartition(partitions, partitionIndex);
        for (Long loanId : savingsAccountIds) {
            if (remainingSpace == partitionSize) {
                partitionIndex++;
                createNewPartition(partitions, partitionIndex);
                remainingSpace = 0;
            }
            String key = PARTITION_PREFIX + partitionIndex;
            ExecutionContext executionContext = partitions.get(key);
            List<Long> data = (List<Long>) executionContext.get("activeSavingAccountIds");
            data.add(loanId);
            remainingSpace++;
        }
        return partitions;
    }

    private void createNewPartition(Map<String, ExecutionContext> partitions, int partitionIndex) {
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.put("activeSavingAccountIds", new ArrayList<Long>());
        executionContext.put("partition", PARTITION_PREFIX + partitionIndex);
        partitions.put(PARTITION_PREFIX + partitionIndex, executionContext);
    }
}
