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
package org.apache.fineract.custom.portfolio.buyprocess.domain;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@Entity
@Table(schema="custom", name = "c_client_buy_process")
@Cacheable(false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ClientBuyProcess {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "channel_id", nullable = false)
	private Long channelId;

	@Transient
	private String channelHash;

	@Column(name = "client_id", nullable = false)
	private Long clientId;

	@Column(name = "point_if_sales_id", nullable = false)
	private Long pointOfSalesId;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "credit_id", nullable = false)
	private Long creditId;

	@Column(name = "requested_date", nullable = false)
	private LocalDate requestedDate;

	@Column(name = "amount", nullable = false)
	private BigDecimal amount;

	@Column(name = "term", nullable = false)
	private Long term;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	@Column(name = "ip_details", nullable = true, length = 5000)
	private String ipDetails;

	@Column(name = "status", nullable = false)
	private Integer status;

	@Column(name = "error_message", nullable = true, length = 5000)
	private String errorMessage;

	@Column(name = "loan_id", nullable = true)
	private Long loanId;

	@Transient
	private LinkedHashMap<String, String> errorMessageHM = new LinkedHashMap<>();

	public ClientBuyProcess(Long channelId, String channelHash, Long clientId, Long pointOfSalesId, Long productId, Long creditId,
							LocalDate requestedDate, BigDecimal amount, Long term, LocalDateTime createdAt, Long createdBy,
							String ipDetails) {
		this.channelId = channelId;
		this.channelHash = channelHash;
		this.clientId = clientId;
		this.pointOfSalesId = pointOfSalesId;
		this.productId = productId;
		this.creditId = creditId;
		this.requestedDate = requestedDate;
		this.amount = amount;
		this.term = term;
		this.createdAt = createdAt;
		this.createdBy = createdBy;
		this.ipDetails = ipDetails;
	}
}


