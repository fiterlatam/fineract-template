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
package org.apache.fineract.portfolio.loanaccount.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

@AllArgsConstructor
@Getter
public enum LoanDisbursementMethod {

    INVALID(0, "loan.disbursement.method.invalid"), CHEQUES(1, "loan.disbursement.method.cheques"), CASH(2,
            "loan.disbursement.method.cash"), SAVINGS_ACCOUNT(3, "loan.disbursement.method.savingsaccount");

    private final Integer value;
    private final String code;

    public static LoanDisbursementMethod fromInt(final Integer statusValue) {
        return switch (statusValue) {
            case 1 -> LoanDisbursementMethod.CHEQUES;
            case 2 -> LoanDisbursementMethod.CASH;
            case 3 -> LoanDisbursementMethod.SAVINGS_ACCOUNT;
            default -> LoanDisbursementMethod.INVALID;
        };
    }

    public static EnumOptionData status(final Integer statusInt) {
        return switch (statusInt) {
            case 1 -> new EnumOptionData(CHEQUES.value.longValue(), CHEQUES.code, CHEQUES.name());
            case 2 -> new EnumOptionData(CASH.value.longValue(), CASH.code, CASH.name());
            case 3 -> new EnumOptionData(SAVINGS_ACCOUNT.value.longValue(), SAVINGS_ACCOUNT.code, SAVINGS_ACCOUNT.name());
            default -> new EnumOptionData(INVALID.value.longValue(), INVALID.code, INVALID.name());
        };
    }
}
