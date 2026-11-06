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
package org.apache.fineract.organisation.prequalification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.List;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.prequalification.command.PrequalificationDataValidator;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroup;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupMember;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupRepositoryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FB-1073: evitar integrantes duplicados en precalificaciones por envíos repetidos (doble clic).
 */
@ExtendWith(MockitoExtension.class)
class PrequalificationWritePlatformServiceImplTest {

    private static final Long GROUP_ID = 43610L;

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper;
    @Mock
    private PrequalificationDataValidator dataValidator;

    @InjectMocks
    private PrequalificationWritePlatformServiceImpl service;

    private PrequalificationGroup group;

    @BeforeEach
    void setUp() {
        this.group = mock(PrequalificationGroup.class);
    }

    private static JsonCommand command(final String membersJson) {
        final String json = "{\"individual\":false,\"agencyId\":1,\"productId\":2,\"locale\":\"en\",\"dateFormat\":\"dd MMMM yyyy\","
                + "\"members\":" + membersJson + "}";
        return JsonCommand.from(json, JsonParser.parseString(json), new FromJsonHelper(), "PREQUALIFICATION", GROUP_ID, null, null, null,
                null, null, null, null, null, null, null);
    }

    private static String member(final Long id, final String dpi) {
        return "{" + (id != null ? "\"id\":" + id + "," : "") + "\"name\":\"INTEGRANTE " + dpi + "\",\"dpi\":\"" + dpi
                + "\",\"requestedAmount\":5000}";
    }

    private static PrequalificationGroupMember memberWithId(final Long id) {
        final PrequalificationGroupMember m = mock(PrequalificationGroupMember.class);
        when(m.getId()).thenReturn(id);
        return m;
    }

    private void givenGroupWithLoadedMembers(final Long... ids) {
        when(this.prequalificationGroupRepositoryWrapper.findOneWithNotFoundDetection(GROUP_ID)).thenReturn(this.group);
        when(this.group.getId()).thenReturn(GROUP_ID);
        // Construir los mocks antes del when(...) para no anidar stubbing
        final List<PrequalificationGroupMember> members = Arrays.stream(ids).map(PrequalificationWritePlatformServiceImplTest::memberWithId)
                .toList();
        when(this.group.getMembers()).thenReturn(members);
    }

    private void givenCommittedMembers(final Long... ids) {
        when(this.jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(GROUP_ID))).thenReturn(List.of(ids));
    }

    @Test
    void updateRejectsTheSameDpiTwiceInThePayloadBeforeLocking() {
        when(this.prequalificationGroupRepositoryWrapper.findOneWithNotFoundDetection(GROUP_ID)).thenReturn(this.group);
        final JsonCommand command = command(
                "[" + member(1L, "2551099700101") + "," + member(null, "2552278840101") + "," + member(null, " 2552278840101 ") + "]");

        final GeneralPlatformDomainRuleException ex = assertThrows(GeneralPlatformDomainRuleException.class,
                () -> this.service.processUpdatePrequalification(GROUP_ID, command));

        assertEquals("error.msg.prequalification.member.duplicate.dpi", ex.getGlobalisationMessageCode());
        verify(this.jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class), anyLong());
        verify(this.group, never()).update(any());
    }

    @Test
    void updateRejectsWhenAnotherRequestChangedTheMembersWhileWaitingForTheLock() {
        // Esta transacción cargó los integrantes 1 y 2, pero otra petición (el otro clic) ya confirmó el integrante 3
        givenGroupWithLoadedMembers(1L, 2L);
        givenCommittedMembers(1L, 2L, 3L);
        final JsonCommand command = command(
                "[" + member(1L, "2551099700101") + "," + member(2L, "2557458340101") + "," + member(null, "2552278920101") + "]");

        final GeneralPlatformDomainRuleException ex = assertThrows(GeneralPlatformDomainRuleException.class,
                () -> this.service.processUpdatePrequalification(GROUP_ID, command));

        assertEquals("error.msg.prequalification.modified.concurrently", ex.getGlobalisationMessageCode());
        verify(this.jdbcTemplate).queryForObject(eq("SELECT id FROM m_prequalification_group WHERE id = ? FOR UPDATE"), eq(Long.class),
                eq(GROUP_ID));
        verify(this.group, never()).update(any());
    }

    @Test
    void updateContinuesWhenTheMembersAreTheSameThatWereLoaded() {
        givenGroupWithLoadedMembers(1L, 2L);
        givenCommittedMembers(2L, 1L);
        final JsonCommand command = command(
                "[" + member(1L, "2551099700101") + "," + member(2L, "2557458340101") + "," + member(null, "2552278920101") + "]");
        // Marca para cortar el flujo justo después de la verificación (el resto del método no es parte de esta prueba)
        when(this.group.update(command)).thenThrow(new IllegalStateException("verificación superada"));

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> this.service.processUpdatePrequalification(GROUP_ID, command));

        assertEquals("verificación superada", ex.getMessage());
    }

    @Test
    void updateAllowsRemovingAMemberAndAddingItAgainWithTheSameDpi() {
        givenGroupWithLoadedMembers(1L, 2L);
        givenCommittedMembers(1L, 2L);
        // El integrante 2 se quita y se vuelve a agregar (sin id) con el mismo DPI: no es un duplicado
        final JsonCommand command = command("[" + member(1L, "2551099700101") + "," + member(null, "2557458340101") + "]");
        when(this.group.update(command)).thenThrow(new IllegalStateException("verificación superada"));

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> this.service.processUpdatePrequalification(GROUP_ID, command));

        assertEquals("verificación superada", ex.getMessage());
    }

    @Test
    void createRejectsTheSameDpiTwiceBeforeSavingAnything() {
        final JsonCommand command = command("[" + member(null, "2552278840101") + "," + member(null, "2552278840101") + "]");

        final GeneralPlatformDomainRuleException ex = assertThrows(GeneralPlatformDomainRuleException.class,
                () -> this.service.processPrequalification(command));

        assertEquals("error.msg.prequalification.member.duplicate.dpi", ex.getGlobalisationMessageCode());
        verify(this.prequalificationGroupRepositoryWrapper, never()).saveAndFlush(any());
    }

}
