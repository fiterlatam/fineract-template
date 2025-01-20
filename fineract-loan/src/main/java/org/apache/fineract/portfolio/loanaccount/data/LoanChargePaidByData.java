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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargePaidBy;

@Getter
@Setter
@RequiredArgsConstructor
public class LoanChargePaidByData {

    private final Long id;
    private final BigDecimal amount;
    private final Integer installmentNumber;
    private final Long chargeId;
    private final Long transactionId;
    private final String name;
    private BigDecimal mandatoryInsurance;
    private BigDecimal voluntaryInsurance;
    private BigDecimal aval;
    private BigDecimal hono;
    private BigDecimal penalty;
    private boolean penaltyCharge;

    // Invoice related fields
    private BigDecimal penaltyPortion;
    private BigDecimal penaltyVatPortion;
    private BigDecimal honorariosPortion;
    private BigDecimal honorariosVatPortion;
    private BigDecimal voluntaryInsurancePortion;
    private BigDecimal voluntaryInsuranceVatPortion;
    private BigDecimal mandatoryInsurancePortion;
    private BigDecimal mandatoryInsuranceVatPortion;

    public LoanChargePaidByData(LoanChargePaidBy originalData) {
        this.id = originalData.getId();
        this.amount = originalData.getAmount();
        this.installmentNumber = originalData.getInstallmentNumber();
        this.chargeId = originalData.getLoanCharge().getId();
        this.transactionId = originalData.getLoanTransaction().getId();
        this.penaltyCharge = originalData.getLoanCharge().isPenaltyCharge();
        this.name = "";
    }
}
