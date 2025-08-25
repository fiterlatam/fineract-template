package org.apache.fineract.portfolio.loanaccount.loanschedule.domain;

import org.apache.fineract.portfolio.loanaccount.domain.LoanInstallmentCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LoanInstalmentChargeRepository
        extends JpaRepository<LoanInstallmentCharge, Long>, JpaSpecificationExecutor<LoanInstallmentCharge> {

    @Modifying
    @Query(value = "DELETE FROM m_loan_installment_charge WHERE loan_schedule_id = ?", nativeQuery = true)
    void deleteByLoanScheduleId(Long id);
}
