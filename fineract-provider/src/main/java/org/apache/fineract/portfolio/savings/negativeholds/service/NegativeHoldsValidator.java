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
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.TIPO_PAR_CRUZADO;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.TIPO_PAR_PROPIO;

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
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldBalanceAdjustmentData;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldMovementData;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldPairData;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldsValidationData;

/**
 * Validaciones y simulación para corregir retenciones (20) con saldo corrido negativo reversando pares retención +
 * liberación (21). Lógica pura, sin acceso a BD: la usan el endpoint de validación y, dentro de la transacción, el de
 * corrección.
 *
 * Reglas:
 * <ol>
 * <li>Una sola cuenta de Garantías activa por cliente.</li>
 * <li>Por cada retención activa con saldo corrido &lt; 0:
 * <ul>
 * <li>con liberación: par PROPIO (la retención y su liberación);</li>
 * <li>sin liberación (sigue retenida): pares CRUZADOS, es decir garantías anteriores que seguían retenidas cuando se
 * hizo y que se liberaron después de ella, de la más reciente hacia atrás hasta cubrir el negativo. Si no alcanzan, no
 * es corregible.</li>
 * </ul>
 * Todo par exige liberación en la misma cuenta, tipo 21, activa, del mismo monto, posterior a la retención y no
 * referida por otra retención.</li>
 * <li>Un movimiento activo con saldo guardado &lt; 0 fuera de la ventana de los pares (entre su retención y su
 * liberación) = negativo de otro origen.</li>
 * <li>Los movimientos de los pares no deben tener asientos contables.</li>
 * <li>Simulación: recalculando el saldo corrido sin los pares (como lo hace Fineract), el mínimo debe ser &ge; 0.</li>
 * </ol>
 * Ajustes: a los movimientos activos dentro de la ventana de un par se les guarda el saldo simulado y sobregiro 0. El
 * recálculo de Fineract compara el sobregiro guardado de cada movimiento con el calculado y, si difiere, reversa y
 * copia el movimiento, lo que en una retención termina en el error de contabilidad.
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
        // Liberaciones referidas por más de una retención activa
        final Map<Long, Long> releaseUsage = ordered.stream()
                .filter(t -> t.isActive() && t.isHold() && t.getReleaseIdOfHoldAmount() != null)
                .collect(Collectors.groupingBy(GuaranteeTransactionRow::getReleaseIdOfHoldAmount, Collectors.counting()));

        // 2. Retenciones negativas y sus pares
        final List<GuaranteeTransactionRow> negativeHolds = ordered.stream()
                .filter(t -> t.isActive() && t.isHold() && isNegative(t.getRunningBalance())).collect(Collectors.toList());
        final Set<Long> pairHoldIds = new HashSet<>();
        final Set<Long> pairMemberIds = new HashSet<>();
        final Set<Long> reportedNegatives = new HashSet<>();

        for (final GuaranteeTransactionRow hold : negativeHolds) {
            if (hold.getReleaseIdOfHoldAmount() != null) {
                if (pairHoldIds.add(hold.getId())) {
                    final NegativeHoldPairData pair = buildPair(hold, TIPO_PAR_PROPIO, hold.getId(), byId, position, releaseUsage,
                            snapshot);
                    pairMemberIds.add(hold.getId());
                    if (pair.getReleaseId() != null && byId.containsKey(pair.getReleaseId())) {
                        pairMemberIds.add(pair.getReleaseId());
                    }
                    if (!pair.isOk()) {
                        motivos.add(MOTIVO_PAR_INVALIDO + ": " + hold.getId() + "/" + pair.getReleaseId() + " " + pair.getProblemas());
                    }
                    result.getPares().add(pair);
                }
                continue;
            }
            // Sigue retenida: buscar garantías anteriores que la cubrían y se liberaron después
            final int negPos = position.get(hold.getId());
            final List<NegativeHoldPairData> crossCandidates = new ArrayList<>();
            for (int i = negPos - 1; i >= 0; i--) {
                final GuaranteeTransactionRow previous = ordered.get(i);
                if (!previous.isActive() || !previous.isHold() || previous.getReleaseIdOfHoldAmount() == null
                        || pairHoldIds.contains(previous.getId())) {
                    continue;
                }
                final GuaranteeTransactionRow release = byId.get(previous.getReleaseIdOfHoldAmount());
                if (release == null || position.get(release.getId()) <= negPos) {
                    continue; // ya se había liberado antes de la retención negativa: no la cubría
                }
                final NegativeHoldPairData candidate = buildPair(previous, TIPO_PAR_CRUZADO, hold.getId(), byId, position, releaseUsage,
                        snapshot);
                if (candidate.isOk()) {
                    crossCandidates.add(candidate);
                }
            }
            // Lo que ya cubren los pares elegidos antes cuya ventana contiene a esta retención
            BigDecimal covered = hold.getRunningBalance();
            for (final NegativeHoldPairData chosen : result.getPares()) {
                if (chosen.isOk() && position.get(chosen.getHoldId()) < negPos && negPos < position.get(chosen.getReleaseId())) {
                    covered = covered.add(chosen.getMonto());
                }
            }
            if (covered.signum() >= 0) {
                continue;
            }
            final List<NegativeHoldPairData> selected = new ArrayList<>();
            for (final NegativeHoldPairData candidate : crossCandidates) {
                if (covered.signum() >= 0) {
                    break;
                }
                selected.add(candidate);
                covered = covered.add(candidate.getMonto());
            }
            if (selected.isEmpty() || covered.signum() < 0) {
                reportedNegatives.add(hold.getId());
                motivos.add(MOTIVO_RETENCION_SIN_LIBERAR + ": retención " + hold.getId() + " (" + hold.getAmount().toPlainString()
                        + ", saldo " + hold.getRunningBalance().toPlainString() + ") sigue retenida y "
                        + (selected.isEmpty() ? "ninguna garantía anterior liberada después de ella la cubre"
                                : "las garantías anteriores liberadas después de ella no cubren el negativo"));
                continue;
            }
            for (final NegativeHoldPairData pair : selected) {
                pairHoldIds.add(pair.getHoldId());
                pairMemberIds.add(pair.getHoldId());
                pairMemberIds.add(pair.getReleaseId());
                result.getPares().add(pair);
            }
        }

        // Ventanas de los pares válidos: movimientos entre la retención y la liberación que se reversan
        final Set<Long> removedIds = new HashSet<>();
        final List<int[]> windows = new ArrayList<>();
        result.getPares().stream().filter(NegativeHoldPairData::isOk).forEach(p -> {
            removedIds.add(p.getHoldId());
            removedIds.add(p.getReleaseId());
            windows.add(new int[] { position.get(p.getHoldId()), position.get(p.getReleaseId()) });
        });
        final Set<Long> inWindow = ordered.stream().filter(t -> t.isActive() && !removedIds.contains(t.getId())).filter(t -> {
            final int pos = position.get(t.getId());
            return windows.stream().anyMatch(w -> w[0] < pos && pos < w[1]);
        }).map(GuaranteeTransactionRow::getId).collect(Collectors.toSet());

        // 3. Negativos de otro origen: saldo guardado < 0 fuera de toda ventana
        final List<Long> otherNegatives = ordered.stream()
                .filter(t -> t.isActive() && isNegative(t.getRunningBalance()) && !removedIds.contains(t.getId())
                        && !inWindow.contains(t.getId()) && !reportedNegatives.contains(t.getId()) && !pairMemberIds.contains(t.getId()))
                .map(GuaranteeTransactionRow::getId).collect(Collectors.toList());
        if (!otherNegatives.isEmpty()) {
            motivos.add(MOTIVO_NEGATIVO_OTRO_ORIGEN + ": movimientos " + otherNegatives);
        }
        final List<Long> otherOverdrafts = ordered.stream()
                .filter(t -> t.isActive() && !removedIds.contains(t.getId()) && !inWindow.contains(t.getId())
                        && t.getOverdraftAmount() != null && t.getOverdraftAmount().signum() > 0)
                .map(GuaranteeTransactionRow::getId).collect(Collectors.toList());
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

        final Map<Long, BigDecimal> simulated = recalculate(
                active.stream().filter(t -> !removedIds.contains(t.getId())).collect(Collectors.toList()));
        final BigDecimal minSimulated = simulated.values().stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        result.setSaldoMinSimulado(minSimulated);
        if (minSimulated.signum() < 0) {
            motivos.add(MOTIVO_SIMULACION_NEGATIVA + ": saldo corrido mínimo sin los pares = " + minSimulated.toPlainString());
        }

        // Ajustes de saldo y sobregiro en las ventanas
        final Map<Long, BigDecimal> adjusted = new HashMap<>();
        for (final GuaranteeTransactionRow t : active) {
            if (!inWindow.contains(t.getId())) {
                continue;
            }
            final BigDecimal target = simulated.get(t.getId());
            final boolean balanceDiffers = t.getRunningBalance() == null || t.getRunningBalance().compareTo(target) != 0;
            final boolean overdraftDiffers = t.getOverdraftAmount() != null && t.getOverdraftAmount().signum() != 0;
            if (balanceDiffers || overdraftDiffers) {
                adjusted.put(t.getId(), target);
                result.getAjustesSaldo()
                        .add(new NegativeHoldBalanceAdjustmentData(t.getId(), t.getRunningBalance(), target, t.getOverdraftAmount()));
            }
        }

        if (negativeHolds.isEmpty() && motivos.isEmpty()) {
            motivos.add(MOTIVO_NADA_QUE_CORREGIR);
        }
        result.setCorregible(motivos.isEmpty() && !result.getPares().isEmpty());

        if (includeMovements) {
            final List<NegativeHoldMovementData> movements = new ArrayList<>();
            for (final GuaranteeTransactionRow t : ordered) {
                movements.add(new NegativeHoldMovementData(t.getId(), t.getTransactionType(), t.getAmount(), t.getTransactionDate(),
                        t.getRunningBalance(), simulated.get(t.getId()), !t.isActive(), removedIds.contains(t.getId()),
                        t.getReleaseIdOfHoldAmount(), t.getJournalEntryCount(), t.getOverdraftAmount(), adjusted.get(t.getId())));
            }
            result.setMovimientos(movements);
        }
        return result;
    }

    /** Arma el par retención + liberación y anota sus problemas (reglas 2 y 4). */
    private static NegativeHoldPairData buildPair(final GuaranteeTransactionRow hold, final String tipo, final Long negativeHoldId,
            final Map<Long, GuaranteeTransactionRow> byId, final Map<Long, Integer> position, final Map<Long, Long> releaseUsage,
            final GuaranteeAccountSnapshot snapshot) {
        final Long releaseId = hold.getReleaseIdOfHoldAmount();
        final GuaranteeTransactionRow release = byId.get(releaseId);
        final NegativeHoldPairData pair = new NegativeHoldPairData(hold.getId(), releaseId, hold.getAmount(), hold.getTransactionDate(),
                release == null ? null : release.getTransactionDate(), hold.getRunningBalance(), tipo, negativeHoldId);
        if (release == null) {
            final Long otherAccount = snapshot.getForeignReleaseAccounts().get(releaseId);
            pair.addProblema(otherAccount == null ? "LIBERACION_NO_EXISTE" : "LIBERACION_EN_OTRA_CUENTA (" + otherAccount + ")");
            return pair;
        }
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
        final long asientos = hold.getJournalEntryCount() + release.getJournalEntryCount();
        if (asientos > 0) {
            pair.addProblema(MOTIVO_PAR_CON_ASIENTOS + " (" + asientos + ")");
        }
        return pair;
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
