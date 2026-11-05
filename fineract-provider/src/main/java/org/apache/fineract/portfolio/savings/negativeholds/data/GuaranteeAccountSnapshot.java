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
package org.apache.fineract.portfolio.savings.negativeholds.data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/** Cuenta de Garantías con todos sus movimientos (de BD o armada a mano en pruebas). */
@Getter
public final class GuaranteeAccountSnapshot {

    private final Long savingsId;
    private final String accountNo;
    private final Long clientId;
    private final String clientAccountNo;
    private final String clientName;
    private final BigDecimal accountBalance;
    private final BigDecimal amountOnHold;
    /** Cuentas de Garantías activas que tiene el cliente (debe ser 1). */
    private final int guaranteeAccountsForClient;
    private final List<GuaranteeTransactionRow> transactions;
    /** Liberaciones referidas por retenciones de esta cuenta que viven en otra cuenta: releaseId -> savingsId. */
    private final Map<Long, Long> foreignReleaseAccounts;

    public GuaranteeAccountSnapshot(final Long savingsId, final String accountNo, final Long clientId, final String clientAccountNo,
            final String clientName, final BigDecimal accountBalance, final BigDecimal amountOnHold, final int guaranteeAccountsForClient,
            final List<GuaranteeTransactionRow> transactions, final Map<Long, Long> foreignReleaseAccounts) {
        this.savingsId = savingsId;
        this.accountNo = accountNo;
        this.clientId = clientId;
        this.clientAccountNo = clientAccountNo;
        this.clientName = clientName;
        this.accountBalance = accountBalance;
        this.amountOnHold = amountOnHold;
        this.guaranteeAccountsForClient = guaranteeAccountsForClient;
        this.transactions = transactions;
        this.foreignReleaseAccounts = foreignReleaseAccounts == null ? Map.of() : foreignReleaseAccounts;
    }
}
