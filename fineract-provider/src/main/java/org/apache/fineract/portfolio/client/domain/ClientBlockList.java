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

package org.apache.fineract.portfolio.client.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@lombok.RequiredArgsConstructor
@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
@lombok.Builder
@Entity
@Table(name = "m_client_block_list", uniqueConstraints = {
        @UniqueConstraint(name = "uk_identification", columnNames = { "id_type", "id_number" }) })
public class ClientBlockList extends AbstractPersistableCustom {

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "id_type")
    private String idType;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "second_name")
    private String secondName;

    @Column(name = "surname")
    private String surname;

    @Column(name = "second_surname")
    private String secondSurname;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "causal")
    private String causal;

    @Column(name = "observation")
    private String observation;

    // getters and setters

}
