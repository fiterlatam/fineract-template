package org.apache.fineract.custom.portfolio.ally.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AllyCompensationRepository extends JpaRepository<AllyCompensation, Long>, JpaSpecificationExecutor<AllyCompensation> {

    Optional<AllyCompensation> findById(Long id);
}
