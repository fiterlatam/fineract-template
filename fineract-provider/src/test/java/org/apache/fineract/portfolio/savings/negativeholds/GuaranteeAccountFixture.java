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

    /**
     * Cuenta 11724 (prod, 19-nov-2026): la garantía A (400) seguía retenida cuando se retuvo B (200) con 120
     * disponibles; B sigue activa sin liberar (saldo -80) y A se liberó después. Par cruzado a reversar: 11/15. Incluye
     * la retención de 0 (12) cuyo saldo guardado quedó desfasado.
     */
    public static GuaranteeAccountFixture cuenta11724() {
        return account().tx(1, DEPOSIT, "390", 1, "390").hold(2, "390", 2, "0", 3L).tx(3, RELEASE, "390", 3, "390")
                .tx(4, WITHDRAWAL, "390", 4, "0").tx(5, DEPOSIT, "390", 5, "390").hold(6, "390", 5, "0", 7L).tx(7, RELEASE, "390", 6, "390")
                .tx(8, DEPOSIT, "130", 7, "520").hold(9, "520", 7, "0", 10L).tx(10, RELEASE, "520", 8, "520").hold(11, "400", 8, "120", 15L)
                .hold(12, "0", 9, "0", 13L).tx(13, RELEASE, "0", 10, "120").hold(14, "200", 11, "-80", null)
                .tx(15, RELEASE, "400", 12, "320");
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
