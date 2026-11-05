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
package org.apache.fineract.portfolio.savings.negativeholds.service;

import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.ADV_SALDOS_NO_COINCIDEN;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.ADV_SOBREGIRO_REGISTRADO;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.MOTIVO_MULTIPLES_CUENTAS;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.MOTIVO_NADA_QUE_CORREGIR;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.MOTIVO_NEGATIVO_OTRO_ORIGEN;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.MOTIVO_PAR_CON_ASIENTOS;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.MOTIVO_PAR_INVALIDO;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.MOTIVO_RETENCION_SIN_LIBERAR;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.MOTIVO_SIMULACION_NEGATIVA;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.negativeholds.data.GuaranteeAccountSnapshot;
import org.apache.fineract.portfolio.savings.negativeholds.data.GuaranteeTransactionRow;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldMovementData;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldPairData;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldsValidationData;

/**
 * Validaciones y simulación para corregir retenciones (20) con saldo corrido negativo reversando el par retención +
 * liberación (21). Lógica pura, sin acceso a BD: la usan el endpoint de validación y, dentro de la transacción, el de
 * corrección.
 *
 * Reglas:
 * <ol>
 * <li>Una sola cuenta de Garantías activa por cliente.</li>
 * <li>Pares candidatos: retenciones activas con saldo corrido &lt; 0; cada una debe tener su liberación en la misma
 * cuenta, tipo 21, activa, del mismo monto, posterior a la retención y no compartida con otra retención.</li>
 * <li>Cualquier otro movimiento activo con saldo corrido guardado &lt; 0 (fuera de los pares) = negativo de otro
 * origen. Se usa el saldo guardado a propósito: el arreglo no recalcula la cuenta, así que un negativo intermedio
 * seguiría en BD después de corregir.</li>
 * <li>Los movimientos del par no deben tener asientos contables.</li>
 * <li>Simulación: recalculando el saldo corrido sin los pares (como lo hace Fineract), el mínimo debe ser &ge; 0.</li>
 * <li>Retención activa negativa sin liberación: no corregible con este método.</li>
 * </ol>
 */
public final class NegativeHoldsValidator {

    /** Mismo orden que SavingsAccount.transactions (@OrderBy "dateOf, createdDate, id"). */
    public static final Comparator<GuaranteeTransactionRow> FINERACT_ORDER = Comparator
            .comparing(GuaranteeTransactionRow::getTransactionDate)
            .thenComparing(GuaranteeTransactionRow::getCreatedDate, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(GuaranteeTransactionRow::getId);

    private static final int CURRENCY_SCALE = 2;

    private NegativeHoldsValidator() {}

    public static NegativeHoldsValidationData validate(final GuaranteeAccountSnapshot snapshot, final boolean includeMovements) {
        final NegativeHoldsValidationData result = new NegativeHoldsValidationData();
        result.setSavingsId(snapshot.getSavingsId());
        result.setAccountNo(snapshot.getAccountNo());
        result.setClientId(snapshot.getClientId());
        result.setClientAccountNo(snapshot.getClientAccountNo());
        result.setClientName(snapshot.getClientName());
        result.setSaldo(snapshot.getAccountBalance());
        result.setRetenido(snapshot.getAmountOnHold());

        final List<String> motivos = result.getMotivos();
        final List<String> advertencias = result.getAdvertencias();

        // 1. Una cuenta de Garantías por cliente
        if (snapshot.getGuaranteeAccountsForClient() > 1) {
            motivos.add(MOTIVO_MULTIPLES_CUENTAS + ": el cliente tiene " + snapshot.getGuaranteeAccountsForClient()
                    + " cuentas de Garantías activas");
        }

        final List<GuaranteeTransactionRow> ordered = new ArrayList<>(snapshot.getTransactions());
        ordered.sort(FINERACT_ORDER);
        final Map<Long, Integer> position = new HashMap<>();
        final Map<Long, GuaranteeTransactionRow> byId = new LinkedHashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            position.put(ordered.get(i).getId(), i);
            byId.put(ordered.get(i).getId(), ordered.get(i));
        }

        // 2 y 6. Retenciones candidatas y sus liberaciones
        final List<GuaranteeTransactionRow> negativeHolds = ordered.stream()
                .filter(t -> t.isActive() && t.isHold() && isNegative(t.getRunningBalance())).collect(Collectors.toList());
        final Map<Long, Long> releaseUsage = negativeHolds.stream().filter(h -> h.getReleaseIdOfHoldAmount() != null)
                .collect(Collectors.groupingBy(GuaranteeTransactionRow::getReleaseIdOfHoldAmount, Collectors.counting()));
        final Set<Long> pairMemberIds = new HashSet<>();

        for (final GuaranteeTransactionRow hold : negativeHolds) {
            pairMemberIds.add(hold.getId());
            final Long releaseId = hold.getReleaseIdOfHoldAmount();
            if (releaseId == null) {
                motivos.add(MOTIVO_RETENCION_SIN_LIBERAR + ": retención " + hold.getId() + " (" + hold.getAmount().toPlainString()
                        + ", saldo " + hold.getRunningBalance().toPlainString() + ") no tiene liberación con quién cancelarse");
                continue;
            }
            final GuaranteeTransactionRow release = byId.get(releaseId);
            final NegativeHoldPairData pair = new NegativeHoldPairData(hold.getId(), releaseId, hold.getAmount(), hold.getTransactionDate(),
                    release == null ? null : release.getTransactionDate(), hold.getRunningBalance());
            if (release == null) {
                final Long otherAccount = snapshot.getForeignReleaseAccounts().get(releaseId);
                pair.addProblema(otherAccount == null ? "LIBERACION_NO_EXISTE" : "LIBERACION_EN_OTRA_CUENTA (" + otherAccount + ")");
            } else {
                pairMemberIds.add(release.getId());
                if (!release.isRelease()) {
                    pair.addProblema("LIBERACION_TIPO_INVALIDO (" + release.getTransactionType() + ")");
                }
                if (!release.isActive()) {
                    pair.addProblema("LIBERACION_REVERSADA");
                }
                if (release.getAmount().compareTo(hold.getAmount()) != 0) {
                    pair.addProblema("MONTO_DISTINTO (retención " + hold.getAmount().toPlainString() + ", liberación "
                            + release.getAmount().toPlainString() + ")");
                }
                if (position.get(release.getId()) < position.get(hold.getId())) {
                    pair.addProblema("LIBERACION_ANTERIOR_A_RETENCION");
                }
                if (releaseUsage.getOrDefault(releaseId, 0L) > 1) {
                    pair.addProblema("LIBERACION_COMPARTIDA_POR_VARIAS_RETENCIONES");
                }
                // 4. Asientos contables
                final long asientos = hold.getJournalEntryCount() + release.getJournalEntryCount();
                if (asientos > 0) {
                    pair.addProblema(MOTIVO_PAR_CON_ASIENTOS + " (" + asientos + ")");
                }
            }
            if (!pair.isOk()) {
                motivos.add(MOTIVO_PAR_INVALIDO + ": " + hold.getId() + "/" + releaseId + " " + pair.getProblemas());
            }
            result.getPares().add(pair);
        }

        // 3. Negativos de otro origen (saldo guardado, fuera de los pares)
        final List<Long> otherNegatives = ordered.stream()
                .filter(t -> t.isActive() && isNegative(t.getRunningBalance()) && !pairMemberIds.contains(t.getId()))
                .map(GuaranteeTransactionRow::getId).collect(Collectors.toList());
        if (!otherNegatives.isEmpty()) {
            motivos.add(MOTIVO_NEGATIVO_OTRO_ORIGEN + ": movimientos " + otherNegatives);
        }
        final List<Long> otherOverdrafts = ordered.stream().filter(t -> t.isActive() && !pairMemberIds.contains(t.getId())
                && t.getOverdraftAmount() != null && t.getOverdraftAmount().signum() > 0).map(GuaranteeTransactionRow::getId)
                .collect(Collectors.toList());
        if (!otherOverdrafts.isEmpty()) {
            advertencias.add(ADV_SOBREGIRO_REGISTRADO + ": movimientos " + otherOverdrafts);
        }

        // 5. Simulación
        final List<GuaranteeTransactionRow> active = ordered.stream().filter(GuaranteeTransactionRow::isActive)
                .collect(Collectors.toList());
        result.setSaldoMinActual(minStored(active));

        final Map<Long, BigDecimal> recalculated = recalculate(active);
        final List<Long> mismatches = active.stream().filter(
                t -> t.getRunningBalance() == null || round(t.getRunningBalance()).compareTo(round(recalculated.get(t.getId()))) != 0)
                .map(GuaranteeTransactionRow::getId).collect(Collectors.toList());
        if (!mismatches.isEmpty()) {
            advertencias.add(ADV_SALDOS_NO_COINCIDEN + ": " + mismatches.size() + " movimientos, primero " + mismatches.get(0));
        }

        final Set<Long> removedIds = new HashSet<>();
        result.getPares().stream().filter(NegativeHoldPairData::isOk).forEach(p -> {
            removedIds.add(p.getHoldId());
            removedIds.add(p.getReleaseId());
        });
        final Map<Long, BigDecimal> simulated = recalculate(
                active.stream().filter(t -> !removedIds.contains(t.getId())).collect(Collectors.toList()));
        final BigDecimal minSimulated = simulated.values().stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        result.setSaldoMinSimulado(minSimulated);
        if (minSimulated.signum() < 0) {
            motivos.add(MOTIVO_SIMULACION_NEGATIVA + ": saldo corrido mínimo sin los pares = " + minSimulated.toPlainString());
        }

        if (negativeHolds.isEmpty() && motivos.isEmpty()) {
            motivos.add(MOTIVO_NADA_QUE_CORREGIR);
        }
        result.setCorregible(motivos.isEmpty() && !result.getPares().isEmpty());

        if (includeMovements) {
            final List<NegativeHoldMovementData> movements = new ArrayList<>();
            for (final GuaranteeTransactionRow t : ordered) {
                movements.add(new NegativeHoldMovementData(t.getId(), t.getTransactionType(), t.getAmount(), t.getTransactionDate(),
                        t.getRunningBalance(), simulated.get(t.getId()), !t.isActive(), pairMemberIds.contains(t.getId()),
                        t.getReleaseIdOfHoldAmount(), t.getJournalEntryCount()));
            }
            result.setMovimientos(movements);
        }
        return result;
    }

    /**
     * Saldo corrido como lo calcula SavingsAccount.recalculateDailyBalances: parte de 0, suma créditos y liberaciones,
     * resta débitos y retenciones; el resto de tipos no mueve el saldo. Recibe solo movimientos activos y en orden.
     */
    public static Map<Long, BigDecimal> recalculate(final List<GuaranteeTransactionRow> activeOrdered) {
        final Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        BigDecimal running = BigDecimal.ZERO;
        for (final GuaranteeTransactionRow t : activeOrdered) {
            final SavingsAccountTransactionType type = t.type();
            if (type.isCredit() || type.isAmountRelease()) {
                running = running.add(t.getAmount());
            } else if (type.isDebit() || type.isAmountOnHold()) {
                running = running.subtract(t.getAmount());
            }
            balances.put(t.getId(), running);
        }
        return balances;
    }

    private static BigDecimal minStored(final List<GuaranteeTransactionRow> active) {
        return active.stream().map(GuaranteeTransactionRow::getRunningBalance).filter(b -> b != null).min(BigDecimal::compareTo)
                .orElse(null);
    }

    private static boolean isNegative(final BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    private static BigDecimal round(final BigDecimal value) {
        return value.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }
}
