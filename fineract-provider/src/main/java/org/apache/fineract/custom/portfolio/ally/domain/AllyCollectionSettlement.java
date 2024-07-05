package org.apache.fineract.custom.portfolio.ally.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "m_ally_collection_settlement")
@Cacheable(false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AllyCollectionSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collection_date")
    private LocalDate collectionDate;

    @Column(name = "nit", nullable = false, length = 20)
    private String nit;

    @Column(name = "client_ally_id", nullable = false)
    private Long clientAllyId;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "point_of_sales_id", nullable = false)
    private Long pointOfSalesId;

    @Column(name = "point_of_sales_name")
    private String pointOfSalesName;

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Column(name = "principal_amount", nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "settled_comission", nullable = false)
    private Integer settledComission;

    @Column(name = "tax_profile_id", nullable = false)
    private Integer taxProfileId;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "collection_status")
    private Integer collectionStatus;
}
