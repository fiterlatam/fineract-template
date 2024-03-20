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
package org.apache.fineract.custom.ally.domain;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(schema="custom", name = "c_client_ally_point_of_sales")
@Cacheable(false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ClientAllyPointOfSales {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "client_ally_id", nullable = false)
	private Long clientAllyId;

	@Column(name = "code", nullable = false, length = 4)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "brand_id", nullable = false)
	private Long brandCodeValueId;

	@Column(name = "city_id", nullable = false)
	private Long cityCodeValueId;

	@Column(name = "department_id", nullable = false)
	private Long departmentCodeValueId;

	@Column(name = "category_id", nullable = false)
	private Long categoryCodeValueId;

	@Column(name = "segment_id", nullable = false)
	private Long segmentCodeValueId;

	@Column(name = "type_id", nullable = false)
	private Long typeCodeValueId;

	@Column(name = "settled_comission", nullable = false)
	private BigDecimal settledComission;

	@Column(name = "buy_enabled", nullable = false)
	private Boolean buyEnabled;

	@Column(name = "collection_enabled", nullable = false)
	private Boolean collectionEnabled;

	@Column(name = "state_id", nullable = false)
	private Long stateCodeValueId;
}
