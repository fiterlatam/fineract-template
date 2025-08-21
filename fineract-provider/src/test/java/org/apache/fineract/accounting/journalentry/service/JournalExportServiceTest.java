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
package org.apache.fineract.accounting.journalentry.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import org.apache.fineract.accounting.journalentry.data.JournalExportRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test class for JournalExportService
 */
@ExtendWith(MockitoExtension.class)
class JournalExportServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private JournalExportServiceImpl journalExportService;

    @Test
    void shouldGenerateCorrectFilename() {
        // Given
        JournalExportRequest request = new JournalExportRequest();
        request.setDateFrom("2024-07-01");
        request.setDateTo("2024-07-31");
        request.setDateFormat("yyyy-MM-dd");
        request.setLocale("en");

        // When
        String filename = journalExportService.generateFilename(request);

        // Then
        assertEquals("journal_entries_20240701_20240731.txt", filename);
    }

    @Test
    void shouldHandleNullAmounts() {
        // Given
        JournalExportRequest request = new JournalExportRequest();
        request.setDateFrom("2024-07-01");
        request.setDateTo("2024-07-31");
        request.setDateFormat("yyyy-MM-dd");
        request.setLocale("en");
        request.setOfficeId(1L);

        // When & Then
        assertDoesNotThrow(() -> {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            journalExportService.exportJournalEntries(request, outputStream, "test.txt");
        });
    }
}
