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
package org.apache.fineract.infrastructure.bulkimport.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import org.apache.fineract.infrastructure.bulkimport.data.BulkImportEvent;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.data.GlobalEntityType;
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
public class BulkImportEventListener implements ApplicationListener<BulkImportEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(BulkImportEventListener.class);
    private final ApplicationContext applicationContext;
    private final ImportDocumentRepository importRepository;
    private final DocumentWritePlatformService documentService;

    @Autowired
    public BulkImportEventListener(final ApplicationContext context, final ImportDocumentRepository importRepository,
            final DocumentWritePlatformService documentService) {
        this.applicationContext = context;
        this.importRepository = importRepository;
        this.documentService = documentService;
    }

    @Override
    public void onApplicationEvent(final BulkImportEvent event) {
        ThreadLocalContextUtil.init(event.getContext());
        ImportHandler importHandler;
        final ImportDocument importDocument = this.importRepository.findById(event.getImportId())
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.import.document.id.invalid",
                        "Import document with id " + event.getImportId() + " does not exist"));
        final GlobalEntityType entityType = GlobalEntityType.fromInt(importDocument.getEntityType());
        importHandler = switch (entityType) {
            case OFFICES -> this.applicationContext.getBean("officeImportHandler", ImportHandler.class);
            case CENTERS -> this.applicationContext.getBean("centerImportHandler", ImportHandler.class);
            case CHART_OF_ACCOUNTS -> this.applicationContext.getBean("chartOfAccountsImportHandler", ImportHandler.class);
            case CLIENTS_ENTITY -> this.applicationContext.getBean("clientEntityImportHandler", ImportHandler.class);
            case CLIENTS_PERSON -> this.applicationContext.getBean("clientPersonImportHandler", ImportHandler.class);
            case FIXED_DEPOSIT_ACCOUNTS -> this.applicationContext.getBean("fixedDepositImportHandler", ImportHandler.class);
            case FIXED_DEPOSIT_TRANSACTIONS -> this.applicationContext.getBean("fixedDepositTransactionImportHandler", ImportHandler.class);
            case GROUPS -> this.applicationContext.getBean("groupImportHandler", ImportHandler.class);
            case GUARANTORS -> this.applicationContext.getBean("guarantorImportHandler", ImportHandler.class);
            case GL_JOURNAL_ENTRIES -> this.applicationContext.getBean("journalEntriesImportHandler", ImportHandler.class);
            case LOANS -> this.applicationContext.getBean("loanImportHandler", ImportHandler.class);
            case LOAN_TRANSACTIONS -> this.applicationContext.getBean("loanRepaymentImportHandler", ImportHandler.class);
            case LOAN_WRITE_OFFS -> this.applicationContext.getBean("loanWriteOffImportHandler", ImportHandler.class);
            case RECURRING_DEPOSIT_ACCOUNTS -> this.applicationContext.getBean("recurringDepositImportHandler", ImportHandler.class);
            case RECURRING_DEPOSIT_ACCOUNTS_TRANSACTIONS ->
                this.applicationContext.getBean("recurringDepositTransactionImportHandler", ImportHandler.class);
            case SAVINGS_ACCOUNT -> this.applicationContext.getBean("savingsImportHandler", ImportHandler.class);
            case SAVINGS_TRANSACTIONS -> this.applicationContext.getBean("savingsTransactionImportHandler", ImportHandler.class);
            case SHARE_ACCOUNTS -> this.applicationContext.getBean("sharedAccountImportHandler", ImportHandler.class);
            case STAFF -> this.applicationContext.getBean("staffImportHandler", ImportHandler.class);
            case USERS -> this.applicationContext.getBean("userImportHandler", ImportHandler.class);
            case CLIENT_BLOCK -> this.applicationContext.getBean("blockClientImportHandler", ImportHandler.class);
            case CLIENT_VIP -> this.applicationContext.getBean("clientVipImportHandler", ImportHandler.class);
            case COMMERCE_POINT_OF_SALE -> this.applicationContext.getBean("commercePointOfSaleImportHandler", ImportHandler.class);
            default ->
                throw new GeneralPlatformDomainRuleException("error.msg.unable.to.find.resource", "Unable to find requested resource");
        };

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

        DocumentCommand documentCommand = new DocumentCommand(modifiedParams, document.getId(), entityType.name(),
                document.getParentEntityId(), document.getName(), document.getFileName(), document.getSize(),
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
