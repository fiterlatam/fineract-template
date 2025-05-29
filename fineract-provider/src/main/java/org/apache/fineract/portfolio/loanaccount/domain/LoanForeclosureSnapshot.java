package org.apache.fineract.portfolio.loanaccount.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;

import java.time.OffsetDateTime;

@Entity
@Table(name = "m_loan_foreclosure_snapshot")
@Getter
@Setter
public class LoanForeclosureSnapshot extends AbstractAuditableWithUTCDateTimeCustom {

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "loan_transaction_id")
    private Long loanTransactionId;

    @Column(name = "loan_summary_json")
    private String loanSummaryJson;

    @Column(name = "loan_transactions_json")
    private String loanTransactionsJson;

    @Column(name = "loan_repayment_schedule_json")
    private String loanRepaymentScheduleJson;

    @Column(name = "loan_charges_json")
    private String loanChargesJson;

    @Column(name = "is_restored")
    private boolean isRestored;

    @Column(name = "restored_on_date")
    private OffsetDateTime restoredOnDate;

}
