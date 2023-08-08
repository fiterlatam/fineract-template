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
package org.apache.fineract.organisation.prequalification.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

/**
 * Immutable data object represent client identity data.
 */
public class MemberPrequalificationData {

    private final Long id;
    private final String name;
    private final String dpi;
    private final LocalDate dob;
    private final String workWithPuente;
    private final BigDecimal requestedAmount;
    private final EnumOptionData status;
    private final Long blacklistCount;
    private final BigDecimal totalLoanAmount;
    private final BigDecimal totalLoanBalance;
    private final BigDecimal totalGuaranteedLoanBalance;
    private final Long noOfCycles;
    private final Long additionalCreditsCount;
    private final BigDecimal additionalCreditsSum;

    public MemberPrequalificationData(final Long id, final String name, final String dpi, final LocalDate dob, final String workWithPuente,
                                      final BigDecimal requestedAmount, final EnumOptionData status, Long blacklistCount, BigDecimal totalLoanAmount, BigDecimal totalLoanBalance, BigDecimal totalGuaranteedLoanBalance, Long noOfCycles, Long additionalCreditsCount, BigDecimal additionalCreditsSum) {
        this.id = id;
        this.name = name;
        this.dpi = dpi;
        this.dob = dob;
        this.workWithPuente = workWithPuente;
        this.requestedAmount = requestedAmount;
        this.status = status;
        this.blacklistCount = blacklistCount;
        this.totalLoanAmount = totalLoanAmount;
        this.totalLoanBalance = totalLoanBalance;
        this.totalGuaranteedLoanBalance = totalGuaranteedLoanBalance;
        this.noOfCycles = noOfCycles;
        this.additionalCreditsCount = additionalCreditsCount;
        this.additionalCreditsSum = additionalCreditsSum;
    }

    public static MemberPrequalificationData instance(final Long id, final String name, final String dpi, final LocalDate dob,
                                                      final String workWithPuente, final BigDecimal requestedAmount, final EnumOptionData status, Long blacklistCount, BigDecimal totalLoanAmount, BigDecimal totalLoanBalance, BigDecimal totalGuaranteedLoanBalance, Long noOfCycles, Long additionalCreditsCount, BigDecimal additionalCreditsSum) {
        return new MemberPrequalificationData(id, name, dpi, dob, workWithPuente, requestedAmount, status, blacklistCount,totalLoanAmount,totalLoanBalance,totalGuaranteedLoanBalance,noOfCycles,additionalCreditsCount,additionalCreditsSum);
    }
}
