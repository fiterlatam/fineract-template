package org.apache.fineract.portfolio.collectionhousemanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CollectionHouseConfigRepository
        extends JpaRepository<CollectionHouseConfiguration, Long>, JpaSpecificationExecutor<CollectionHouseConfiguration> {

}
