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
package org.apache.fineract.portfolio.savings.negativeholds;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.fineract.portfolio.savings.negativeholds.data.GuaranteeAccountSnapshot;
import org.apache.fineract.portfolio.savings.negativeholds.data.GuaranteeTransactionRow;

/** Arma cuentas de Garantías de prueba. Saldo corrido guardado explícito para poder simular datos inconsistentes. */
public final class GuaranteeAccountFixture {

    public static final long SAVINGS_ID = 32944L;
    static final int DEPOSIT = 1;
    static final int WITHDRAWAL = 2;
    static final int HOLD = 20;
    static final int RELEASE = 21;
    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    private final List<GuaranteeTransactionRow> rows = new ArrayList<>();
    private int guaranteeAccounts = 1;

    public static GuaranteeAccountFixture account() {
        return new GuaranteeAccountFixture();
    }

    /**
     * Escenario del caso 52: garantía del ciclo 1 (200) seguía retenida cuando se retuvo la del ciclo 2 (350) con solo
     * 150 disponibles. Par a reversar: 4/6.
     */
    public static GuaranteeAccountFixture caso52() {
        return account().tx(1, DEPOSIT, "200", 1, "200").hold(2, "200", 2, "0", 5L).tx(3, DEPOSIT, "150", 3, "150")
                .hold(4, "350", 4, "-200", 6L).tx(5, RELEASE, "200", 5, "0").tx(6, RELEASE, "350", 6, "350");
    }

    public GuaranteeAccountFixture tx(final long id, final int type, final String amount, final int day, final String running) {
        return add(id, type, amount, day, running, null, false, 0);
    }

    public GuaranteeAccountFixture hold(final long id, final String amount, final int day, final String running, final Long releaseId) {
        return add(id, HOLD, amount, day, running, releaseId, false, 0);
    }

    public GuaranteeAccountFixture add(final long id, final int type, final String amount, final int day, final String running,
            final Long releaseId, final boolean reversed, final long journalEntries) {
        final LocalDate date = START.plusDays(day);
        this.rows.add(new GuaranteeTransactionRow(id, SAVINGS_ID, type, new BigDecimal(amount), date, date.atStartOfDay().plusSeconds(id),
                running == null ? null : new BigDecimal(running), BigDecimal.ZERO, reversed, false, releaseId, journalEntries));
        return this;
    }

    /** Reemplaza una fila existente (por id) con otros datos. */
    public GuaranteeAccountFixture replace(final long id, final int type, final String amount, final int day, final String running,
            final Long releaseId, final boolean reversed, final long journalEntries) {
        this.rows.removeIf(r -> r.getId() == id);
        return add(id, type, amount, day, running, releaseId, reversed, journalEntries);
    }

    public GuaranteeAccountFixture guaranteeAccounts(final int count) {
        this.guaranteeAccounts = count;
        return this;
    }

    public GuaranteeAccountSnapshot build() {
        return new GuaranteeAccountSnapshot(SAVINGS_ID, "000032944", 136746L, "000136746", "CLIENTA PRUEBA", new BigDecimal("350"),
                new BigDecimal("0"), this.guaranteeAccounts, new ArrayList<>(this.rows), Map.of());
    }
}
