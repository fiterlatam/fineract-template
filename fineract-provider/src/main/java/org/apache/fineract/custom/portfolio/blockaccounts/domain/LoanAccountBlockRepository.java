package org.apache.fineract.custom.portfolio.blockaccounts.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanAccountBlockRepository extends JpaRepository<LoanAccountBlock, Long> {

    @Query(value = "SELECT ab FROM LoanAccountBlock ab WHERE ab.loan.id = :loanId AND ab.active = true")
    Optional<LoanAccountBlock> retrieveByLoanIdAndStatusActive(@Param(value = "loanId") Long loanId);
}
