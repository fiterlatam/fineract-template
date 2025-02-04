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
package org.apache.fineract.portfolio.collectionhousemanagement.jobs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.ColletionHouseHistory;
import org.apache.fineract.portfolio.collectionhousemanagement.service.CollectionHouseHistoryReadWriteService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class CollectionHouseConfigTasklet implements Tasklet {

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    private final CollectionHouseHistoryReadWriteService collectionHouseHistoryReadWriteService;

    public CollectionHouseConfigTasklet(PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            CollectionHouseHistoryReadWriteService collectionHouseHistoryReadWriteService) {
        this.collectionHouseHistoryReadWriteService = collectionHouseHistoryReadWriteService;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        try {
            // Fetch all collection house history records
            List<ColletionHouseHistory> collectionHouseHistoryList = collectionHouseHistoryReadWriteService.findAllCollectionHouseHistory();

            JsonArray updatesArray = new JsonArray();
            for (ColletionHouseHistory colletionHouseHistory : collectionHouseHistoryList) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("clientAccountNo", colletionHouseHistory.getClientAccountNumber());
                jsonObject.addProperty("nit", colletionHouseHistory.getCollectionNit());
                jsonObject.addProperty("collectionHouseCode", colletionHouseHistory.getCollectionCode());
                updatesArray.add(jsonObject);
            }

            JsonObject jsonCommandData = new JsonObject();
            jsonCommandData.add("collectionHouseUpdates", updatesArray);

            CommandWrapper commandRequest = new CommandWrapperBuilder().createCollectionHouseHistory()

                    .withJson(jsonCommandData.toString()).build();

            commandsSourceWritePlatformService.logCommandSource(commandRequest);

        } catch (Exception e) {
            log.error("Error executing collection house config tasklet", e);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }
}
