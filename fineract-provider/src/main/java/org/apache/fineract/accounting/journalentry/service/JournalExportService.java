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

import java.io.OutputStream;
import org.apache.fineract.accounting.journalentry.data.JournalExportRequest;

/**
 * Service interface for exporting journal entries in Colombian format
 */
public interface JournalExportService {

    /**
     * Export journal entries as Colombian accounting-compliant TXT file
     *
     * @param request
     *            The export request parameters
     * @param outputStream
     *            The output stream to write the file to
     * @param filename
     *            The filename for the exported file
     */
    void exportJournalEntries(JournalExportRequest request, OutputStream outputStream, String filename);

    /**
     * Generate filename for the export based on date range
     *
     * @param request
     *            The export request
     * @return The generated filename
     */
    String generateFilename(JournalExportRequest request);
}
