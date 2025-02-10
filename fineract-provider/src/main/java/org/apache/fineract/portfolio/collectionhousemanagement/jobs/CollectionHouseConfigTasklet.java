package org.apache.fineract.portfolio.collectionhousemanagement.jobs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseUpdate;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseUpdates;
import org.apache.fineract.portfolio.collectionhousemanagement.service.CollectionHouseHistoryReadWriteService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.ArrayList;
import java.util.List;

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
            CollectionHouseUpdates collectionHouseHistoryUpdatesData = new CollectionHouseUpdates();
            List<CollectionHouseUpdate> collectionHouseHistoryList = new ArrayList<>();
            collectionHouseHistoryUpdatesData = collectionHouseHistoryReadWriteService.fetchDataFromExternalProvider();

            if (collectionHouseHistoryUpdatesData != null) {
                collectionHouseHistoryList = collectionHouseHistoryUpdatesData.getCollectionHouseUpdates();
            }

            collectionHouseHistoryReadWriteService.createCollectionHouseHistory(collectionHouseHistoryList);

        } catch (Exception e) {
            log.error("Error executing collection house config tasklet", e);
            throw e;
        }
        return RepeatStatus.FINISHED;
    }
}
