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
package org.apache.fineract.custom.infrastructure.bulkimport.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import org.apache.fineract.custom.infrastructure.bulkimport.data.CustomGlobalEntityType;
import org.apache.fineract.infrastructure.bulkimport.data.BulkImportEvent;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.domain.ImportDocument;
import org.apache.fineract.infrastructure.bulkimport.domain.ImportDocumentRepository;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.documentmanagement.command.DocumentCommand;
import org.apache.fineract.infrastructure.documentmanagement.domain.Document;
import org.apache.fineract.infrastructure.documentmanagement.service.DocumentWritePlatformService;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
public class CustomBulkImportEventListener implements ApplicationListener<BulkImportEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(CustomBulkImportEventListener.class);
    private final ApplicationContext applicationContext;
    private final ImportDocumentRepository importRepository;
    private final DocumentWritePlatformService documentService;

    @Autowired
    public CustomBulkImportEventListener(final ApplicationContext context, final ImportDocumentRepository importRepository,
            final DocumentWritePlatformService documentService) {
        this.applicationContext = context;
        this.importRepository = importRepository;
        this.documentService = documentService;
    }

    @Override
    public void onApplicationEvent(final BulkImportEvent event) {
        ThreadLocalContextUtil.init(event.getContext());
        ImportHandler importHandler = null;
        final ImportDocument importDocument = this.importRepository.findById(event.getImportId())
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.import.document.id.invalid",
                        "Import document with id " + event.getImportId() + " does not exist"));
        final CustomGlobalEntityType entityType = CustomGlobalEntityType.fromInt(importDocument.getEntityType());
        if (entityType != null) {
            importHandler = switch (entityType) {
                case CLIENT_ALLY -> this.applicationContext.getBean("clientAllyImportHandler", ImportHandler.class);
                case CLIENT_ALLY_POINTS_OF_SALES ->
                    this.applicationContext.getBean("clientAllyPointsOfSalesImportHandler", ImportHandler.class);
            };
        }
        if (importHandler != null) {
            final Workbook workbook = event.getWorkbook();
            final Count count = importHandler.process(workbook, event.getLocale(), event.getDateFormat(), event.getImportAttributeMap());
            importDocument.update(DateUtils.getLocalDateTimeOfTenant(), count.getSuccessCount(), count.getErrorCount());
            this.importRepository.saveAndFlush(importDocument);

            final Set<String> modifiedParams = new HashSet<>();
            modifiedParams.add("fileName");
            modifiedParams.add("size");
            modifiedParams.add("type");
            modifiedParams.add("location");
            Document document = importDocument.getDocument();

            DocumentCommand documentCommand = new DocumentCommand(modifiedParams, document.getId(), entityType.name(), null,
                    document.getName(), document.getFileName(), document.getSize(),
                    URLConnection.guessContentTypeFromName(document.getFileName()), null, null);

            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                try {
                    workbook.write(bos);
                } finally {
                    bos.close();
                }
            } catch (IOException io) {
                LOG.error("Problem occurred in onApplicationEvent function", io);
            }
            byte[] bytes = bos.toByteArray();
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            this.documentService.updateDocument(documentCommand, bis);
        }
    }
}
