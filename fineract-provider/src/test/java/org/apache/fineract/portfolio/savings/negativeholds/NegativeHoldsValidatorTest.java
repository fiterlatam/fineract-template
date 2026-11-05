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

import static org.apache.fineract.portfolio.savings.negativeholds.GuaranteeAccountFixture.DEPOSIT;
import static org.apache.fineract.portfolio.savings.negativeholds.GuaranteeAccountFixture.HOLD;
import static org.apache.fineract.portfolio.savings.negativeholds.GuaranteeAccountFixture.RELEASE;
import static org.apache.fineract.portfolio.savings.negativeholds.GuaranteeAccountFixture.WITHDRAWAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldPairData;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldsValidationData;
import org.apache.fineract.portfolio.savings.negativeholds.service.NegativeHoldsValidator;
import org.junit.jupiter.api.Test;

class NegativeHoldsValidatorTest {

    private static NegativeHoldsValidationData validate(final GuaranteeAccountFixture fixture) {
        return NegativeHoldsValidator.validate(fixture.build(), true);
    }

    private static boolean hasMotivo(final NegativeHoldsValidationData data, final String code) {
        return data.getMotivos().stream().anyMatch(m -> m.startsWith(code));
    }

    @Test
    void caso52EsCorregible() {
        final NegativeHoldsValidationData data = validate(GuaranteeAccountFixture.caso52());

        assertTrue(data.isCorregible(), data.getMotivos().toString());
        assertEquals(1, data.getPares().size());
        final NegativeHoldPairData pair = data.getPares().get(0);
        assertEquals(4L, pair.getHoldId());
        assertEquals(6L, pair.getReleaseId());
        assertTrue(pair.isOk());
        assertEquals(0, new BigDecimal("-200").compareTo(pair.getSaldoHold()));
        assertEquals(0, new BigDecimal("-200").compareTo(data.getSaldoMinActual()));
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getSaldoMinSimulado()));
        assertTrue(data.getAdvertencias().isEmpty(), data.getAdvertencias().toString());
        assertEquals(6, data.getMovimientos().size());
    }

    @Test
    void simulacionRecalculaComoFineract() {
        // Sin el par 4/6: 200, 0 (hold 200), 150, 350 (release 200)
        final NegativeHoldsValidationData data = validate(GuaranteeAccountFixture.caso52());
        assertEquals(0, new BigDecimal("350").compareTo(data.getMovimientos().get(4).getSaldoSimulado()));
        assertEquals(null, data.getMovimientos().get(3).getSaldoSimulado());
    }

    @Test
    void negativoDeOtroOrigenNoEsCorregible() {
        // Retiro entre la retención y su liberación que también quedó negativo en BD
        final NegativeHoldsValidationData data = validate(GuaranteeAccountFixture.caso52().tx(7, WITHDRAWAL, "100", 4, "-300")
                .replace(5, RELEASE, "200", 5, "-100", null, false, 0).replace(6, RELEASE, "350", 6, "250", null, false, 0));

        assertFalse(data.isCorregible());
        assertTrue(hasMotivo(data, NegativeHoldsApiConstants.MOTIVO_NEGATIVO_OTRO_ORIGEN), data.getMotivos().toString());
    }

    @Test
    void parConAsientosNoEsCorregible() {
        final NegativeHoldsValidationData data = validate(
                GuaranteeAccountFixture.caso52().replace(4, HOLD, "350", 4, "-200", 6L, false, 2));

        assertFalse(data.isCorregible());
        assertTrue(hasMotivo(data, NegativeHoldsApiConstants.MOTIVO_PAR_INVALIDO));
        assertTrue(data.getPares().get(0).getProblemas().get(0).startsWith(NegativeHoldsApiConstants.MOTIVO_PAR_CON_ASIENTOS));
    }

    @Test
    void simulacionNegativaNoEsCorregible() {
        // Retiro de 400 con saldo guardado inconsistente (50): al recalcular sin el par queda en -50
        final NegativeHoldsValidationData data = validate(GuaranteeAccountFixture.caso52().tx(7, WITHDRAWAL, "400", 7, "50"));

        assertFalse(data.isCorregible());
        assertTrue(hasMotivo(data, NegativeHoldsApiConstants.MOTIVO_SIMULACION_NEGATIVA), data.getMotivos().toString());
        assertEquals(0, new BigDecimal("-50").compareTo(data.getSaldoMinSimulado()));
        assertTrue(data.getAdvertencias().stream().anyMatch(a -> a.startsWith(NegativeHoldsApiConstants.ADV_SALDOS_NO_COINCIDEN)));
    }

    @Test
    void retencionActivaSinLiberarNoEsCorregible() {
        final NegativeHoldsValidationData data = validate(GuaranteeAccountFixture.account().tx(1, DEPOSIT, "200", 1, "200")
                .hold(2, "200", 2, "0", null).tx(3, DEPOSIT, "150", 3, "150").hold(4, "350", 4, "-200", null));

        assertFalse(data.isCorregible());
        assertTrue(hasMotivo(data, NegativeHoldsApiConstants.MOTIVO_RETENCION_SIN_LIBERAR));
        assertTrue(data.getPares().isEmpty());
    }

    @Test
    void liberacionDeOtroMontoNoEsCorregible() {
        final NegativeHoldsValidationData data = validate(
                GuaranteeAccountFixture.caso52().replace(6, RELEASE, "300", 6, "300", null, false, 0));

        assertFalse(data.isCorregible());
        assertTrue(data.getPares().get(0).getProblemas().get(0).startsWith("MONTO_DISTINTO"));
    }

    @Test
    void liberacionReversadaNoEsCorregible() {
        final NegativeHoldsValidationData data = validate(
                GuaranteeAccountFixture.caso52().replace(6, RELEASE, "350", 6, "350", null, true, 0));

        assertFalse(data.isCorregible());
        assertTrue(data.getPares().get(0).getProblemas().contains("LIBERACION_REVERSADA"));
    }

    @Test
    void variasCuentasDeGarantiasNoEsCorregible() {
        final NegativeHoldsValidationData data = validate(GuaranteeAccountFixture.caso52().guaranteeAccounts(2));

        assertFalse(data.isCorregible());
        assertTrue(hasMotivo(data, NegativeHoldsApiConstants.MOTIVO_MULTIPLES_CUENTAS));
    }

    @Test
    void cuentaYaCorregidaNoTieneNadaQueCorregir() {
        // Estado después de corregir: par 4/6 reversado
        final NegativeHoldsValidationData data = validate(GuaranteeAccountFixture.caso52().replace(4, HOLD, "350", 4, "-200", 6L, true, 0)
                .replace(6, RELEASE, "350", 6, "350", null, true, 0));

        assertFalse(data.isCorregible());
        assertEquals(1, data.getMotivos().size());
        assertTrue(hasMotivo(data, NegativeHoldsApiConstants.MOTIVO_NADA_QUE_CORREGIR));
    }
}
