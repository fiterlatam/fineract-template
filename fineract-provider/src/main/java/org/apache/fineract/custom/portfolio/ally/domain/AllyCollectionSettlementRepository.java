package org.apache.fineract.custom.portfolio.ally.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AllyCollectionSettlementRepository
        extends JpaRepository<AllyCollectionSettlement, Long>, JpaSpecificationExecutor<AllyCollectionSettlement> {

    @Override
    Optional<AllyCollectionSettlement> findById(Long aLong);

    @Query(value = "SELECT * FROM m_ally_collection_settlement macs WHERE macs.loan_id = ? ORDER BY macs.collection_date DESC LIMIT 1", nativeQuery = true)
    Optional<AllyCollectionSettlement> findCollectionByLoanId(@Param("loanId") Long loanId);

    List<AllyCollectionSettlement> findByLoanIdAndCollectionDate(Long loanId, LocalDate collectionDate);

    List<AllyCollectionSettlement> findByClientAllyId(Long clientAllyId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AllyCollectionSettlement macs WHERE macs.loanId = :loanId and macs.collectionDate != :collectionDate")
    void deleteByLoanIdAndNotCollectionDate(@Param("loanId") Long loanId, @Param("collectionDate") LocalDate collectionDate);

}
