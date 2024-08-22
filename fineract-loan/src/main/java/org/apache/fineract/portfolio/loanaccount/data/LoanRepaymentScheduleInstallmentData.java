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
package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public final class LoanRepaymentScheduleInstallmentData {

    private Long id;
    private Integer installmentId;
    private LocalDate date;
    private BigDecimal amount;

    private BigDecimal principalPortion;
    private BigDecimal interestPortion;
    private BigDecimal feeChargesPortion;
    private BigDecimal penaltyChargesPortion;
    private BigDecimal totalInstallmentAmount;
    private List<LoanChargeData> penaltyCharges;
    private List<LoanChargeData> feeCharges;

    public static LoanRepaymentScheduleInstallmentData instanceOf(final Long id, final Integer installmentId, final LocalDate date,
            final BigDecimal amount) {
        return new LoanRepaymentScheduleInstallmentData(id, installmentId, date, amount, null, null, null, null, null, null, null);
    }

}
