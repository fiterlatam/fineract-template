package org.apache.fineract.portfolio.loanaccount.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;

@Entity
@Table(name = "m_clone_loan_audit_trail")
@Getter
@Setter
public class LoanCloneAuditTrail extends AbstractAuditableWithUTCDateTimeCustom {

    @Column(name = "from_loan_id")
    private Long fromLoanId;

    @Column(name = "to_loan_id")
    private Long toLoanId;

    @Column(name = "is_applied")
    private boolean isApplied;

    @Column(name = "error_log")
    private String errorLog;
}
