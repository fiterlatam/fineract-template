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

import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.CLIENT_ACCOUNT_NO_PARAM;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.EXPECTED_PAIRS_PARAM;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.HOLD_ID_PARAM;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.RELEASE_ID_PARAM;
import static org.apache.fineract.portfolio.savings.negativeholds.NegativeHoldsApiConstants.SAVINGS_ID_PARAM;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.savings.negativeholds.data.GuaranteeAccountSnapshot;
import org.apache.fineract.portfolio.savings.negativeholds.data.GuaranteeTransactionRow;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldPairData;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldsValidationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NegativeHoldsWritePlatformServiceImpl implements NegativeHoldsWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(NegativeHoldsWritePlatformServiceImpl.class);

    private final PlatformSecurityContext context;
    private final NegativeHoldsReadPlatformService readService;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @Autowired
    public NegativeHoldsWritePlatformServiceImpl(final PlatformSecurityContext context, final NegativeHoldsReadPlatformService readService,
            final NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.context = context;
        this.readService = readService;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Transactional
    @Override
    public CommandProcessingResult fix(final JsonCommand command) {
        this.context.authenticatedUser();
        final String clientAccountNo = StringUtils.trimToNull(command.stringValueOfParameterNamed(CLIENT_ACCOUNT_NO_PARAM));
        final Long savingsIdParam = command.longValueOfParameterNamed(SAVINGS_ID_PARAM);
        final Set<Pair> expected = parseExpectedPairs(command, clientAccountNo, savingsIdParam);

        final Long savingsId = resolveAccount(clientAccountNo, savingsIdParam);

        // Revalidar dentro de la transacción, con la cuenta bloqueada
        final GuaranteeAccountSnapshot snapshot = this.readService.loadSnapshot(savingsId, true);
        if (clientAccountNo != null && !clientAccountNo.equals(snapshot.getClientAccountNo())) {
            throw new GeneralPlatformDomainRuleException("error.msg.negativeholds.account.client.mismatch",
                    "La cuenta " + savingsId + " no pertenece al cliente " + clientAccountNo);
        }
        final NegativeHoldsValidationData validation = NegativeHoldsValidator.validate(snapshot, false);
        final Set<Pair> current = validation.getPares().stream().map(p -> new Pair(p.getHoldId(), p.getReleaseId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Idempotencia: no quedan retenciones negativas y los pares esperados ya están reversados
        if (current.isEmpty() && allReversed(snapshot, expected)) {
            final Map<String, Object> changes = baseChanges(snapshot, validation, expected);
            changes.put("resultado", "NADA_QUE_CORREGIR");
            changes.put("mensaje", "Los pares indicados ya estaban reversados; no se modificó nada");
            return result(snapshot, changes);
        }

        if (!validation.isCorregible()) {
            throw new GeneralPlatformDomainRuleException("error.msg.negativeholds.not.fixable",
                    "La cuenta " + savingsId + " no es corregible con este método: " + String.join(" | ", validation.getMotivos()),
                    validation.getMotivos());
        }
        if (!current.equals(expected)) {
            throw new GeneralPlatformDomainRuleException("error.msg.negativeholds.pairs.changed",
                    "Los pares actuales de la cuenta " + savingsId + " no coinciden con expectedPairs (cambió algo desde la validación). "
                            + "Actuales: " + current + "; esperados: " + expected + ". No se modificó nada.",
                    current, expected);
        }

        // Respaldo de las filas a modificar (estado antes), queda en m_portfolio_command_source
        final Set<Long> ids = new LinkedHashSet<>();
        expected.forEach(p -> {
            ids.add(p.holdId);
            ids.add(p.releaseId);
        });
        final List<Map<String, Object>> backup = snapshot.getTransactions().stream().filter(t -> ids.contains(t.getId()))
                .map(NegativeHoldsWritePlatformServiceImpl::backupRow).collect(Collectors.toList());

        final int updated = this.namedJdbcTemplate.update(
                "update m_savings_account_transaction set is_reversed = 1 "
                        + "where savings_account_id = :savingsId and is_reversed = 0 and id in (:ids)",
                new MapSqlParameterSource("savingsId", savingsId).addValue("ids", ids));
        if (updated != ids.size()) {
            throw new GeneralPlatformDomainRuleException("error.msg.negativeholds.unexpected.update.count",
                    "Se esperaban " + ids.size() + " filas actualizadas y fueron " + updated + "; se revierte la transacción", updated);
        }

        // Verificación posterior: ningún saldo corrido negativo en movimientos no reversados
        final List<Long> stillNegative = this.namedJdbcTemplate.queryForList(
                "select id from m_savings_account_transaction where savings_account_id = :savingsId and is_reversed = 0 "
                        + "and is_reversal = 0 and running_balance_derived < 0",
                new MapSqlParameterSource("savingsId", savingsId), Long.class);
        if (!stillNegative.isEmpty()) {
            throw new GeneralPlatformDomainRuleException("error.msg.negativeholds.still.negative",
                    "Después de reversar quedan movimientos con saldo corrido negativo " + stillNegative + "; se revierte la transacción",
                    stillNegative);
        }

        final GuaranteeAccountSnapshot after = this.readService.loadSnapshot(savingsId, false);
        final Map<String, Object> changes = baseChanges(snapshot, validation, expected);
        changes.put("resultado", "CORREGIDO");
        changes.put("filasActualizadas", updated);
        changes.put("saldoDespues", after.getAccountBalance());
        changes.put("retenidoDespues", after.getAmountOnHold());
        changes.put("saldoMinDespues", after.getTransactions().stream().filter(GuaranteeTransactionRow::isActive)
                .map(GuaranteeTransactionRow::getRunningBalance).filter(b -> b != null).min(BigDecimal::compareTo).orElse(null));
        changes.put("respaldo", backup);
        LOG.info("negativeholds fix: cuenta {} cliente {} pares {} filas {}", savingsId, snapshot.getClientAccountNo(), expected, updated);
        return result(snapshot, changes);
    }

    private Long resolveAccount(final String clientAccountNo, final Long savingsId) {
        if (savingsId != null) {
            return savingsId;
        }
        final List<Long> ids = this.readService.findGuaranteeAccountIdsByClientAccountNo(clientAccountNo);
        if (ids.size() != 1) {
            throw new GeneralPlatformDomainRuleException("error.msg.negativeholds.account.count",
                    "El cliente " + clientAccountNo + " tiene " + ids.size() + " cuentas de Garantías activas (se espera 1); use savingsId",
                    ids.size());
        }
        return ids.get(0);
    }

    private static Set<Pair> parseExpectedPairs(final JsonCommand command, final String clientAccountNo, final Long savingsId) {
        final List<ApiParameterError> errors = new ArrayList<>();
        final DataValidatorBuilder validator = new DataValidatorBuilder(errors).resource("negativeholds");
        if (clientAccountNo == null && savingsId == null) {
            validator.reset().parameter(CLIENT_ACCOUNT_NO_PARAM).failWithCode("required", "Indique clientAccountNo o savingsId");
        }
        final Set<Pair> pairs = new LinkedHashSet<>();
        final JsonArray array = command.arrayOfParameterNamed(EXPECTED_PAIRS_PARAM);
        if (array == null || array.size() == 0) {
            validator.reset().parameter(EXPECTED_PAIRS_PARAM).failWithCode("required",
                    "expectedPairs es obligatorio (copiar los pares de la validación)");
        } else {
            for (final JsonElement element : array) {
                final JsonObject o = element.isJsonObject() ? element.getAsJsonObject() : null;
                if (o == null || !o.has(HOLD_ID_PARAM) || !o.has(RELEASE_ID_PARAM) || o.get(HOLD_ID_PARAM).isJsonNull()
                        || o.get(RELEASE_ID_PARAM).isJsonNull()) {
                    validator.reset().parameter(EXPECTED_PAIRS_PARAM).failWithCode("invalid", "Cada par requiere holdId y releaseId");
                    continue;
                }
                pairs.add(new Pair(o.get(HOLD_ID_PARAM).getAsLong(), o.get(RELEASE_ID_PARAM).getAsLong()));
            }
        }
        if (!errors.isEmpty()) {
            throw new PlatformApiDataValidationException(errors);
        }
        return pairs;
    }

    private static boolean allReversed(final GuaranteeAccountSnapshot snapshot, final Set<Pair> expected) {
        final Map<Long, GuaranteeTransactionRow> byId = snapshot.getTransactions().stream()
                .collect(Collectors.toMap(GuaranteeTransactionRow::getId, t -> t));
        return expected.stream().allMatch(p -> isReversed(byId.get(p.holdId)) && isReversed(byId.get(p.releaseId)));
    }

    private static boolean isReversed(final GuaranteeTransactionRow row) {
        return row != null && row.isReversed();
    }

    private static Map<String, Object> baseChanges(final GuaranteeAccountSnapshot snapshot, final NegativeHoldsValidationData validation,
            final Set<Pair> expected) {
        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("savingsId", snapshot.getSavingsId());
        changes.put("accountNo", snapshot.getAccountNo());
        changes.put("clientAccountNo", snapshot.getClientAccountNo());
        changes.put("pares", validation.getPares().isEmpty() ? expected.stream().map(Pair::toMap).collect(Collectors.toList())
                : validation.getPares().stream().map(NegativeHoldsWritePlatformServiceImpl::pairMap).collect(Collectors.toList()));
        changes.put("saldoAntes", snapshot.getAccountBalance());
        changes.put("retenidoAntes", snapshot.getAmountOnHold());
        changes.put("saldoMinAntes", validation.getSaldoMinActual());
        changes.put("saldoMinSimulado", validation.getSaldoMinSimulado());
        return changes;
    }

    private static Map<String, Object> pairMap(final NegativeHoldPairData p) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("holdId", p.getHoldId());
        m.put("releaseId", p.getReleaseId());
        m.put("monto", p.getMonto());
        m.put("fechaHold", p.getFechaHold() == null ? null : p.getFechaHold().toString());
        m.put("fechaRelease", p.getFechaRelease() == null ? null : p.getFechaRelease().toString());
        m.put("saldoHold", p.getSaldoHold());
        return m;
    }

    private static Map<String, Object> backupRow(final GuaranteeTransactionRow t) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("tipo", t.getTransactionType());
        m.put("monto", t.getAmount());
        m.put("fecha", t.getTransactionDate().toString());
        m.put("saldoCorrido", t.getRunningBalance());
        m.put("sobregiro", t.getOverdraftAmount());
        m.put("isReversed", t.isReversed());
        m.put("releaseIdOfHoldAmount", t.getReleaseIdOfHoldAmount());
        return m;
    }

    private static CommandProcessingResult result(final GuaranteeAccountSnapshot snapshot, final Map<String, Object> changes) {
        return new CommandProcessingResultBuilder().withEntityId(snapshot.getSavingsId()).withSavingsId(snapshot.getSavingsId())
                .withClientId(snapshot.getClientId()).with(changes).build();
    }

    private static final class Pair {

        private final long holdId;
        private final long releaseId;

        Pair(final long holdId, final long releaseId) {
            this.holdId = holdId;
            this.releaseId = releaseId;
        }

        Map<String, Object> toMap() {
            final Map<String, Object> m = new LinkedHashMap<>();
            m.put("holdId", this.holdId);
            m.put("releaseId", this.releaseId);
            return m;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Pair)) {
                return false;
            }
            final Pair other = (Pair) o;
            return this.holdId == other.holdId && this.releaseId == other.releaseId;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(this.holdId) * 31 + Long.hashCode(this.releaseId);
        }

        @Override
        public String toString() {
            return this.holdId + "/" + this.releaseId;
        }
    }
}
