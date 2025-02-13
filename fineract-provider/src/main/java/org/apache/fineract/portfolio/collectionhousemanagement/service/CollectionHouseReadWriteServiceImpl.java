package org.apache.fineract.portfolio.collectionhousemanagement.service;

import java.util.Collection;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseConfigParameterizationData;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseConfigValidator;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.CollectionHouseConfigRepository;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.CollectionHouseConfiguration;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.ColletionHouseHistory;
import org.apache.fineract.portfolio.collectionhousemanagement.exception.CollectionHouseManagementNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectionHouseReadWriteServiceImpl implements CollectionHouseReadWriteService {

    private final CollectionHouseConfigRepository collectionHouseConfigRepository;
    private final CollectionHouseConfigValidator collectionHouseConfigValidator;
    private final CollectionHouseHistoryReadWriteService collectionHouseHistoryReadWriteService;

    @Override
    public Collection<CollectionHouseConfigParameterizationData> retrieveAllCollectionHouseManagement() {
        return collectionHouseConfigRepository.findAll().stream().map(CollectionHouseConfiguration::toData).toList();
    }

    @Override
    public CollectionHouseConfigParameterizationData retrieveCollectionHouse(Long collectionId) {
        try {
            return collectionHouseConfigRepository.getReferenceById(collectionId).toData();
        } catch (Exception e) {
            throw new CollectionHouseManagementNotFoundException(collectionId);
        }
    }

    @Override
    public CollectionHouseConfiguration retrieveCollectionHouseByClientFromHistory(String clientNit) {
        try {
            ColletionHouseHistory history = collectionHouseHistoryReadWriteService.findCollectionHouseHistoryByClientNit(clientNit);
            if (history != null) {
                Optional<CollectionHouseConfiguration>  collectionHouseOptional = collectionHouseConfigRepository.getCollectionByCode(history.getCollectionCode());
                return collectionHouseOptional.orElse(null);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new CollectionHouseManagementNotFoundException(clientNit);
        }
    }

    @Override
    public CommandProcessingResult createCollectionHouseConfig(JsonCommand command) {
        collectionHouseConfigValidator.validateForCreate(command.json());
        CollectionHouseConfiguration collectionHouseConfiguration = CollectionHouseConfiguration.createNewCollectionHouse(command);
        CollectionHouseConfiguration saveCollectionHouse = collectionHouseConfigRepository.saveAndFlush(collectionHouseConfiguration);
        try {
            return CommandProcessingResult.commandOnlyResult(saveCollectionHouse.getId());
        } catch (final JpaSystemException | DataIntegrityViolationException dv) {
            return CommandProcessingResult.empty();
        }
    }

    @Override
    public CommandProcessingResult updateCollectionHouseConfig(Long collectionId, JsonCommand command) {
        collectionHouseConfigValidator.validateForUpdate(command.json());

        try {
            CollectionHouseConfiguration findcollectionHouseConfiguration = collectionHouseConfigRepository.getReferenceById(collectionId);
            String collectionName = command.stringValueOfParameterNamed("collectionName");
            String colllectionNit = command.stringValueOfParameterNamed("collectionNit");
            String collectionCode = command.stringValueOfParameterNamed("collectionCode");
            Integer collectionVerificationCode = command.integerValueOfParameterNamed("collectionVerificationCode");
            findcollectionHouseConfiguration.setCollectionName(collectionName);
            findcollectionHouseConfiguration.setCollectionNit(colllectionNit);
            findcollectionHouseConfiguration.setCollectionCode(collectionCode);
            findcollectionHouseConfiguration.setCollectionVerificationCode(collectionVerificationCode);
            CollectionHouseConfiguration saveCollectionHouse = collectionHouseConfigRepository.save(findcollectionHouseConfiguration);
            return CommandProcessingResult.commandOnlyResult(saveCollectionHouse.getId());
        } catch (Exception e) {
            throw new CollectionHouseManagementNotFoundException(collectionId);
        }

    }

}
