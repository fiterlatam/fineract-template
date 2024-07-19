package org.apache.fineract.custom.portfolio.ally.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AllyPurchaseSettlementRepository
        extends JpaRepository<AllyPurchaseSettlement, Long>, JpaSpecificationExecutor<AllyPurchaseSettlement> {

    @Override
    Optional<AllyPurchaseSettlement> findById(Long aLong);

    Optional<AllyPurchaseSettlement> findByLoanId(Long loanId);

    List<AllyPurchaseSettlement> findByClientAllyId(Long clientAllyId);
}
