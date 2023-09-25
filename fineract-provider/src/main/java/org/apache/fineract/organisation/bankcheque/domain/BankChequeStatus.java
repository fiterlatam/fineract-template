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
package org.apache.fineract.organisation.bankcheque.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

@AllArgsConstructor
@Getter
public enum BankChequeStatus {

    INVALID(0, "bank.cheque.status.invalid"), AVAILABLE(1, "bank.cheque.status.available"), ISSUED(2, "bank.cheque.status.issued"), VOIDED(
            3, "bank.cheque.status.canceled"), PENDING_VOIDANCE(4,
                    "bank.cheque.status.pending.cancellation"), PENDING_ISSUANCE(5, "bank.cheque.status.pending.issuance");

    private final Integer value;
    private final String code;

    public static BankChequeStatus fromInt(final Integer statusValue) {
        return switch (statusValue) {
            case 1 -> BankChequeStatus.AVAILABLE;
            case 2 -> BankChequeStatus.ISSUED;
            case 3 -> BankChequeStatus.VOIDED;
            case 4 -> BankChequeStatus.PENDING_VOIDANCE;
            case 5 -> BankChequeStatus.PENDING_ISSUANCE;
            default -> BankChequeStatus.INVALID;
        };
    }

    public static EnumOptionData status(final Integer statusInt) {
        return switch (statusInt) {
            case 1 -> new EnumOptionData(AVAILABLE.value.longValue(), AVAILABLE.code, AVAILABLE.name());
            case 2 -> new EnumOptionData(ISSUED.value.longValue(), ISSUED.code, ISSUED.name());
            case 3 -> new EnumOptionData(VOIDED.value.longValue(), VOIDED.code, VOIDED.name());
            case 4 -> new EnumOptionData(PENDING_VOIDANCE.value.longValue(), PENDING_VOIDANCE.code, PENDING_VOIDANCE.name());
            case 5 -> new EnumOptionData(PENDING_ISSUANCE.value.longValue(), PENDING_ISSUANCE.code, PENDING_ISSUANCE.name());
            default -> new EnumOptionData(INVALID.value.longValue(), INVALID.code, INVALID.name());
        };
    }
}
