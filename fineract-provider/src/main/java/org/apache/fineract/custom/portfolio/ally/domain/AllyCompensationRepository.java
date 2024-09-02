package org.apache.fineract.custom.portfolio.ally.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AllyCompensationRepository extends JpaRepository<AllyCompensation, Long>, JpaSpecificationExecutor<AllyCompensation> {

    Optional<AllyCompensation> findById(Long id);

    Optional<AllyCompensation> findFirst1ByNit(String nit);

    @Query("select allyCompensation from AllyCompensation allyCompensation where allyCompensation.settlementStatus=false")
    List<AllyCompensation> findBySettlementStatus();

    @Query("select allyCompensation from AllyCompensation allyCompensation where allyCompensation.nit = :nit and allyCompensation.startDate = :startDate and allyCompensation.endDate = :endDate")
    Optional<AllyCompensation> findBynitAndDate(@Param("nit") String nit, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("select allyCompensation from AllyCompensation allyCompensation where allyCompensation.netOutstandingAmount < 0")
    List<AllyCompensation> findNegativeCompensations();
}
