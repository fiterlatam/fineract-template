package org.apache.fineract.portfolio.collectionhousemanagement.exception;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class CollectionHouseManagementNotFoundException extends AbstractPlatformResourceNotFoundException {

    public CollectionHouseManagementNotFoundException(Long id) {
        super("error.msg.collectionhouse.id.invalid", "Collection House with identifier " + id + " does not exist", id);
    }

    public CollectionHouseManagementNotFoundException(String clientNit) {
        super("error.msg.collectionhouse.id.invalid", "Collection House for client with identifier " + clientNit + " does not exist", clientNit);
    }
}
