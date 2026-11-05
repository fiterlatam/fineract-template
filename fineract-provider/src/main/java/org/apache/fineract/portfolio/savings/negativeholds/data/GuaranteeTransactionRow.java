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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;

/** Fila de m_savings_account_transaction tal como se usa para validar/simular. */
@Getter
public final class GuaranteeTransactionRow {

    private final Long id;
    private final Long savingsAccountId;
    private final Integer transactionType;
    private final BigDecimal amount;
    private final LocalDate transactionDate;
    private final LocalDateTime createdDate;
    private final BigDecimal runningBalance;
    private final BigDecimal overdraftAmount;
    private final boolean reversed;
    private final boolean reversal;
    private final Long releaseIdOfHoldAmount;
    private final long journalEntryCount;

    public GuaranteeTransactionRow(final Long id, final Long savingsAccountId, final Integer transactionType, final BigDecimal amount,
            final LocalDate transactionDate, final LocalDateTime createdDate, final BigDecimal runningBalance,
            final BigDecimal overdraftAmount, final boolean reversed, final boolean reversal, final Long releaseIdOfHoldAmount,
            final long journalEntryCount) {
        this.id = id;
        this.savingsAccountId = savingsAccountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.createdDate = createdDate;
        this.runningBalance = runningBalance;
        this.overdraftAmount = overdraftAmount;
        this.reversed = reversed;
        this.reversal = reversal;
        this.releaseIdOfHoldAmount = releaseIdOfHoldAmount;
        this.journalEntryCount = journalEntryCount;
    }

    /** Igual que el recálculo de Fineract: los reversados y las transacciones de reverso no cuentan. */
    public boolean isActive() {
        return !this.reversed && !this.reversal;
    }

    public SavingsAccountTransactionType type() {
        return SavingsAccountTransactionType.fromInt(this.transactionType);
    }

    public boolean isHold() {
        return type().isAmountOnHold();
    }

    public boolean isRelease() {
        return type().isAmountRelease();
    }
}
