package org.apache.fineract.custom.portfolio.ally.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "m_ally_compensation")
@Cacheable(false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AllyCompensation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "compensation_date")
    private LocalDate compensationDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "nit")
    private String nit;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "account_type")
    private String accontType;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "client_ally_id", nullable = false)
    private Long clientAllyId;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "purchase_amount", nullable = false)
    private BigDecimal purchaseAmount;

    @Column(name = "collection_amount", nullable = false)
    private BigDecimal collectionAmount;

    @Column(name = "comission_amount", nullable = false)
    private BigDecimal comissionAmount;

    @Column(name = "va_comission_amount", nullable = false)
    private BigDecimal vaComissionAmount;

    @Column(name = "net_purchase_amount", nullable = false)
    private BigDecimal netPurchaseAmount;

    @Column(name = "compensation_amount", nullable = false)
    private BigDecimal netOutstandingAmount;

    @Column(name = "settlement_status", nullable = false)
    private Boolean settlementStatus;
}
