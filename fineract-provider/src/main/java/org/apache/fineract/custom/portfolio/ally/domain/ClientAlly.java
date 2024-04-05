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

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.spm.domain.Response;

@Entity
@Table(schema = "custom", name = "c_client_ally")
@Cacheable(false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ClientAlly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "nit", nullable = false, length = 20)
    private String nit;

    @Column(name = "nit_digit", nullable = false)
    private Integer nitDigit;

    @Column(name = "address", nullable = false, length = 100)
    private String address;

    @Column(name = "city_id", nullable = false)
    private Long cityCodeValueId;

    @Column(name = "department_id", nullable = false)
    private Long departmentCodeValueId;

    @Column(name = "liquidation_frequency_id", nullable = false)
    private Long liquidationFrequencyCodeValueId;

    @Column(name = "apply_cupo_max_sell", nullable = false)
    private Boolean applyCupoMaxSell;

    @Column(name = "cupo_max_sell", nullable = true)
    private Integer cupoMaxSell;

    @Column(name = "settled_comission", nullable = true)
    private BigDecimal settledComission;

    @Column(name = "buy_enabled", nullable = false)
    private Boolean buyEnabled;

    @Column(name = "collection_enabled", nullable = false)
    private Boolean collectionEnabled;

    @Column(name = "bank_entity_id", nullable = true)
    private Long bankEntityCodeValueId;

    @Column(name = "account_type_id", nullable = false)
    private Long accountTypeCodeValueId;

    @Column(name = "account_number", nullable = false)
    private Long accountNumber;

    @Column(name = "tax_profile_id", nullable = false)
    private Long taxProfileCodeValueId;

    @Column(name = "state_id", nullable = false)
    private Long stateCodeValueId;

//    @OneToMany(fetch = FetchType.EAGER)
//    @JoinColumn(name = "client_ally_id", referencedColumnName = "id", insertable=false, updatable=false)
//    private List<ClientAllyPointOfSales> clientAllyPointOfSalesList;

}
