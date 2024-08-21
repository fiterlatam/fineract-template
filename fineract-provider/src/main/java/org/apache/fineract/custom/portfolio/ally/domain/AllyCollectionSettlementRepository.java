package org.apache.fineract.custom.portfolio.ally.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AllyCollectionSettlementRepository
        extends JpaRepository<AllyCollectionSettlement, Long>, JpaSpecificationExecutor<AllyCollectionSettlement> {

    @Query("SELECT collection FROM AllyCollectionSettlement collection WHERE collection.collectionDate <=:endDate and collection.collectionDate >=:startDate")
    List<AllyCollectionSettlement> findByCollectionDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Override
    Optional<AllyCollectionSettlement> findById(Long aLong);

    Optional<AllyCollectionSettlement> findByLoanId(Long loanId);

    List<AllyCollectionSettlement> findByLoanIdAndCollectionDate(Long loanId, LocalDate collectionDate);

    List<AllyCollectionSettlement> findByClientAllyId(Long clientAllyId);

}
