package org.apache.fineract.portfolio.collectionhousemanagement.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionHouseConfigRepository
        extends JpaRepository<CollectionHouseConfiguration, Long>, JpaSpecificationExecutor<CollectionHouseConfiguration> {

    @Query("select collectionHouseConfiguration from CollectionHouseConfiguration collectionHouseConfiguration where collectionHouseConfiguration.collectionCode = :collectionCode")
    Optional<CollectionHouseConfiguration> getCollectionByCode(@Param("collectionCode") String collectionCode);

}
