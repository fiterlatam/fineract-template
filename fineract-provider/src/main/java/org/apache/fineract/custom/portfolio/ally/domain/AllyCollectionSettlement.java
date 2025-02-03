/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "collection_amount", nullable = false)
    private BigDecimal collectionAmount;

    @Column(name = "tax_profile_id", nullable = false)
    private Integer taxProfileId;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "settlement_status")
    private Boolean settlementStatus;
}
