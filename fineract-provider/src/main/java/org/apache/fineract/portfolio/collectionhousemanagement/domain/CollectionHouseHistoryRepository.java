package org.apache.fineract.portfolio.collectionhousemanagement.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionHouseHistoryRepository
        extends JpaRepository<ColletionHouseHistory, Long>, JpaSpecificationExecutor<ColletionHouseHistory> {

    @Query("select colletionHouseHistory from ColletionHouseHistory colletionHouseHistory where colletionHouseHistory.clientAccountNumber = :clientAccountNumber")
    Optional<ColletionHouseHistory> getCollectionHouseHistoryByAccountNo(@Param("clientAccountNumber") String clientAccountNumber);
}
