package org.apache.fineract.custom.portfolio.ally.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AllyCollectionSettlementRepository
        extends JpaRepository<AllyCollectionSettlement, Long>, JpaSpecificationExecutor<AllyCollectionSettlement> {

    @Override
    Optional<AllyCollectionSettlement> findById(Long aLong);

    Optional<AllyCollectionSettlement> findByLoanId(Long loanId);

    List<AllyCollectionSettlement> findByLoanIdAndCollectionDate(Long loanId, LocalDate collectionDate);

    List<AllyCollectionSettlement> findByClientAllyId(Long clientAllyId);

}
