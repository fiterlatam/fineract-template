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
package org.apache.fineract.infrastructure.bulkimport.importhandler.loanwriteoff;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.LoanWriteOffConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoanWriteOffImportHandler implements ImportHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LoanWriteOffImportHandler.class);
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public LoanWriteOffImportHandler(final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat,
            final Map<String, Object> importAttributes) {
        final List<LoanTransactionData> loanWriteOffs = readExcelFile(workbook, locale, dateFormat);
        return importEntity(workbook, loanWriteOffs, dateFormat, locale);
    }

    private List<LoanTransactionData> readExcelFile(final Workbook workbook, final String locale, final String dateFormat) {
        final List<LoanTransactionData> loanWriteOffs = new ArrayList<>();
        final Sheet loanWriteOffSheet = workbook.getSheet(TemplatePopulateImportConstants.LOAN_WRITE_OFF_SHEET_NAME);
        final Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(loanWriteOffSheet, LoanWriteOffConstants.WRITE_OFF_AMOUNT_COL);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = loanWriteOffSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, LoanWriteOffConstants.STATUS_COL)) {
                final LoanTransactionData loanWriteOffData = readLoanWriteOffData(row, locale, dateFormat);
                loanWriteOffs.add(loanWriteOffData);
            }
        }
        return loanWriteOffs;
    }

    private LoanTransactionData readLoanWriteOffData(final Row row, final String locale, final String dateFormat) {
        final Long loanAccountId = ImportHandlerUtils.readAsLong(LoanWriteOffConstants.LOAN_ACCOUNT_NO_COL, row);
        final BigDecimal writeOffAmount = BigDecimal
                .valueOf(ImportHandlerUtils.readAsDouble(LoanWriteOffConstants.WRITE_OFF_AMOUNT_COL, row));
        final boolean isImportedTransaction = true;
        return LoanTransactionData.writeOffInstance(loanAccountId, writeOffAmount, isImportedTransaction, row.getRowNum(), locale,
                dateFormat);
    }

    private Count importEntity(final Workbook workbook, final List<LoanTransactionData> loanWriteOffs, final String dateFormat,
            final String locale) {
        final Sheet loanWriteOffSheet = workbook.getSheet(TemplatePopulateImportConstants.LOAN_WRITE_OFF_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        String errorMessage;
        final GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat, locale));
        for (final LoanTransactionData loanWriteOffData : loanWriteOffs) {
            try {
                final JsonObject loanRepaymentJsonObj = gsonBuilder.create().toJsonTree(loanWriteOffData).getAsJsonObject();
                loanRepaymentJsonObj.remove("manuallyReversed");
                loanRepaymentJsonObj.remove("numberOfRepayments");
                final String payload = loanRepaymentJsonObj.toString();
                final CommandWrapper commandRequest = new CommandWrapperBuilder()
                        .specialWriteOffLoanTransaction(loanWriteOffData.getAccountId()).withJson(payload).build();
                this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
                successCount++;
                final Cell statusCell = loanWriteOffSheet.getRow(loanWriteOffData.getRowIndex())
                        .createCell(LoanWriteOffConstants.STATUS_COL);
                statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
            } catch (RuntimeException ex) {
                errorCount++;
                LOG.error("Problem occurred in importEntity function", ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(loanWriteOffSheet, loanWriteOffData.getRowIndex(), errorMessage,
                        LoanWriteOffConstants.STATUS_COL);
            }

        }
        loanWriteOffSheet.setColumnWidth(LoanWriteOffConstants.STATUS_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        ImportHandlerUtils.writeString(LoanWriteOffConstants.STATUS_COL,
                loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        return Count.instance(successCount, errorCount);
    }

}
