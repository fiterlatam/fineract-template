package org.apache.fineract.portfolio.loanaccount.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanArchiveHistoryRepository
        extends JpaRepository<LoanArchiveHistory, Long>, JpaSpecificationExecutor<LoanArchiveHistory> {

    @Query("SELECT lah FROM LoanArchiveHistory lah WHERE lah.numeroObligacion NOT IN :numeroObligacionList")
    List<LoanArchiveHistory> findByNumeroObligacionNotIn(@Param("numeroObligacionList") List<String> numeroObligacionList);

    Optional<LoanArchiveHistory> findByTitle(String title);

}
