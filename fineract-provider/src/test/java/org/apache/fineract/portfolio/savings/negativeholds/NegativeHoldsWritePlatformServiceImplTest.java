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

import static org.apache.fineract.portfolio.savings.negativeholds.GuaranteeAccountFixture.HOLD;
import static org.apache.fineract.portfolio.savings.negativeholds.GuaranteeAccountFixture.RELEASE;
import static org.apache.fineract.portfolio.savings.negativeholds.GuaranteeAccountFixture.SAVINGS_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.savings.negativeholds.data.GuaranteeAccountSnapshot;
import org.apache.fineract.portfolio.savings.negativeholds.service.NegativeHoldsReadPlatformService;
import org.apache.fineract.portfolio.savings.negativeholds.service.NegativeHoldsWritePlatformServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

class NegativeHoldsWritePlatformServiceImplTest {

    private NegativeHoldsReadPlatformService readService;
    private NamedParameterJdbcTemplate jdbc;
    private NegativeHoldsWritePlatformServiceImpl service;

    @BeforeEach
    void setUp() {
        this.readService = mock(NegativeHoldsReadPlatformService.class);
        this.jdbc = mock(NamedParameterJdbcTemplate.class);
        this.service = new NegativeHoldsWritePlatformServiceImpl(mock(PlatformSecurityContext.class), this.readService, this.jdbc);
        when(this.readService.findGuaranteeAccountIdsByClientAccountNo("000136746")).thenReturn(List.of(SAVINGS_ID));
    }

    private static JsonCommand command(final String pairsJson) {
        final String json = "{\"clientAccountNo\":\"000136746\",\"expectedPairs\":" + pairsJson + "}";
        return JsonCommand.from(json, JsonParser.parseString(json), new FromJsonHelper(), "NEGATIVEHOLDS", null, null, null, null, null,
                null, null, null, null, null, null);
    }

    private static GuaranteeAccountSnapshot corregido() {
        return GuaranteeAccountFixture.caso52().replace(4, HOLD, "350", 4, "-200", 6L, true, 0)
                .replace(6, RELEASE, "350", 6, "350", null, true, 0).build();
    }

    @Test
    void corrigeCuandoLosParesCoinciden() {
        when(this.readService.loadSnapshot(SAVINGS_ID, true)).thenReturn(GuaranteeAccountFixture.caso52().build());
        when(this.readService.loadSnapshot(SAVINGS_ID, false)).thenReturn(corregido());
        when(this.jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(2);
        when(this.jdbc.queryForList(anyString(), any(SqlParameterSource.class), eq(Long.class))).thenReturn(List.of());

        final CommandProcessingResult result = this.service.fix(command("[{\"holdId\":4,\"releaseId\":6}]"));

        final Map<String, Object> changes = result.getChanges();
        assertEquals("CORREGIDO", changes.get("resultado"));
        assertEquals(2, changes.get("filasActualizadas"));
        assertEquals(2, ((List<?>) changes.get("respaldo")).size());
        assertEquals(SAVINGS_ID, result.getSavingsId());
        verify(this.jdbc).update(anyString(), any(SqlParameterSource.class));
    }

    @Test
    void noTocaNadaSiLosParesCambiaron() {
        when(this.readService.loadSnapshot(SAVINGS_ID, true)).thenReturn(GuaranteeAccountFixture.caso52().build());

        final GeneralPlatformDomainRuleException e = assertThrows(GeneralPlatformDomainRuleException.class,
                () -> this.service.fix(command("[{\"holdId\":2,\"releaseId\":5}]")));

        assertEquals("error.msg.negativeholds.pairs.changed", e.getGlobalisationMessageCode());
        verify(this.jdbc, never()).update(anyString(), any(SqlParameterSource.class));
    }

    @Test
    void noTocaNadaSiNoEsCorregible() {
        when(this.readService.loadSnapshot(SAVINGS_ID, true))
                .thenReturn(GuaranteeAccountFixture.caso52().replace(4, HOLD, "350", 4, "-200", 6L, false, 1).build());

        final GeneralPlatformDomainRuleException e = assertThrows(GeneralPlatformDomainRuleException.class,
                () -> this.service.fix(command("[{\"holdId\":4,\"releaseId\":6}]")));

        assertEquals("error.msg.negativeholds.not.fixable", e.getGlobalisationMessageCode());
        verify(this.jdbc, never()).update(anyString(), any(SqlParameterSource.class));
    }

    @Test
    void esIdempotente() {
        when(this.readService.loadSnapshot(SAVINGS_ID, true)).thenReturn(corregido());

        final CommandProcessingResult result = this.service.fix(command("[{\"holdId\":4,\"releaseId\":6}]"));

        assertEquals("NADA_QUE_CORREGIR", result.getChanges().get("resultado"));
        verify(this.jdbc, never()).update(anyString(), any(SqlParameterSource.class));
    }

    @Test
    void revierteSiNoSeActualizaronTodasLasFilas() {
        when(this.readService.loadSnapshot(SAVINGS_ID, true)).thenReturn(GuaranteeAccountFixture.caso52().build());
        when(this.jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);

        final GeneralPlatformDomainRuleException e = assertThrows(GeneralPlatformDomainRuleException.class,
                () -> this.service.fix(command("[{\"holdId\":4,\"releaseId\":6}]")));

        assertEquals("error.msg.negativeholds.unexpected.update.count", e.getGlobalisationMessageCode());
    }

    @Test
    void revierteSiQuedanNegativos() {
        when(this.readService.loadSnapshot(SAVINGS_ID, true)).thenReturn(GuaranteeAccountFixture.caso52().build());
        when(this.jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(2);
        when(this.jdbc.queryForList(anyString(), any(SqlParameterSource.class), eq(Long.class))).thenReturn(List.of(99L));

        final GeneralPlatformDomainRuleException e = assertThrows(GeneralPlatformDomainRuleException.class,
                () -> this.service.fix(command("[{\"holdId\":4,\"releaseId\":6}]")));

        assertTrue(e.getGlobalisationMessageCode().endsWith("still.negative"));
    }
}
