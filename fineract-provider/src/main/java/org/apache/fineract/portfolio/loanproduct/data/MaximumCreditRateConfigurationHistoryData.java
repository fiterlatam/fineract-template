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
package org.apache.fineract.portfolio.loanproduct.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Immutable data object representing Maximum Credit Rate Configuration details.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MaximumCreditRateConfigurationHistoryData {

    private Long id;
    private BigDecimal eaRate;
    private String appliedByUsername;
    private BigDecimal annualNominalRate;
    private BigDecimal monthlyNominalRate;
    private BigDecimal dailyNominalRate;
    private BigDecimal currentInterestRate;
    private BigDecimal overdueInterestRate;
    private LocalDate appliedOnDate;
}
