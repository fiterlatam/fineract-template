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
package org.apache.fineract.infrastructure.bulkimport.importhandler.loanrepayment;

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
import org.apache.fineract.custom.infrastructure.channel.data.ChannelData;
import org.apache.fineract.custom.infrastructure.channel.service.ChannelReadWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.LoanRepaymentConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
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
public class LoanRepaymentImportHandler implements ImportHandler {

    public static final String SEPARATOR = "-";
    public static final String EMPTY_STR = "";
    private static final Logger LOG = LoggerFactory.getLogger(LoanRepaymentImportHandler.class);
    private final LoanReadPlatformService loanReadPlatformService;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ChannelReadWritePlatformService channelReadWritePlatformService;

    @Autowired
    public LoanRepaymentImportHandler(final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final LoanReadPlatformService loanReadPlatformService, ChannelReadWritePlatformService channelReadWritePlatformService) {
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.loanReadPlatformService = loanReadPlatformService;
        this.channelReadWritePlatformService = channelReadWritePlatformService;
    }

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat,
            final Map<String, Object> importAttributes) {
        final List<LoanTransactionData> loanRepayments = readExcelFile(workbook, locale, dateFormat);
        return importEntity(workbook, loanRepayments, dateFormat, locale);
    }

    private List<LoanTransactionData> readExcelFile(final Workbook workbook, final String locale, final String dateFormat) {
        List<LoanTransactionData> loanRepayments = new ArrayList<>();
        Sheet loanRepaymentSheet = workbook.getSheet(TemplatePopulateImportConstants.LOAN_REPAYMENT_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(loanRepaymentSheet, LoanRepaymentConstants.PAYMENT_AMOUNT_COL);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = loanRepaymentSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, LoanRepaymentConstants.STATUS_COL)) {
                loanRepayments.add(readLoanRepayment(workbook, row, locale, dateFormat));
            }
        }
        return loanRepayments;
    }

    private LoanTransactionData readLoanRepayment(final Workbook workbook, final Row row, final String locale, final String dateFormat) {
        final String clientIdNumber = ImportHandlerUtils.readAsString(LoanRepaymentConstants.CLIENT_ID_COL, row);
        final Long loanAccountId = ImportHandlerUtils.readAsLong(LoanRepaymentConstants.LOAN_ACCOUNT_NO_COL, row);
        final BigDecimal repaymentAmount = BigDecimal
                .valueOf(ImportHandlerUtils.readAsDouble(LoanRepaymentConstants.PAYMENT_AMOUNT_COL, row));
        final LocalDate repaymentDate = ImportHandlerUtils.readAsDate(LoanRepaymentConstants.REPAID_ON_DATE_COL, row);
        final String repaymentType = ImportHandlerUtils.readAsString(LoanRepaymentConstants.REPAYMENT_TYPE_COL, row);
        final Long repaymentTypeId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.EXTRAS_SHEET_NAME),
                repaymentType);
        final LoanTransactionData loanTransactionData = LoanTransactionData.importInstance(repaymentAmount, repaymentDate, repaymentTypeId,
                null, null, null, null, null, loanAccountId, EMPTY_STR, row.getRowNum(), locale, dateFormat);
        final ChannelData channelData = this.channelReadWritePlatformService.findByName("Bancos");
        Long repaymentChannelId = null;
        if (channelData != null) {
            repaymentChannelId = channelData.getId();
        }
        final String repaymentBank = ImportHandlerUtils.readAsString(LoanRepaymentConstants.REPAYMENT_BANK_COL, row);
        final Long repaymentBankId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.EXTRAS_SHEET_NAME),
                repaymentBank);
        loanTransactionData.setRepaymentChannelId(repaymentChannelId);
        loanTransactionData.setRepaymentBankId(repaymentBankId);
        loanTransactionData.setImportedRepaymentTransaction(true);
        loanTransactionData.setClientIdNumber(clientIdNumber);
        return loanTransactionData;
    }

    private Count importEntity(final Workbook workbook, final List<LoanTransactionData> loanRepayments, final String dateFormat,
            final String locale) {
        final Sheet loanRepaymentSheet = workbook.getSheet(TemplatePopulateImportConstants.LOAN_REPAYMENT_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        String errorMessage;
        final GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat, locale));
        for (LoanTransactionData loanRepayment : loanRepayments) {
            try {
                final JsonObject loanRepaymentJsonObj = gsonBuilder.create().toJsonTree(loanRepayment).getAsJsonObject();
                loanRepaymentJsonObj.remove("manuallyReversed");
                loanRepaymentJsonObj.remove("numberOfRepayments");
                final String payload = loanRepaymentJsonObj.toString();
                final CommandWrapper commandRequest = new CommandWrapperBuilder().loanRepaymentTransaction(loanRepayment.getAccountId())
                        .withJson(payload).build();
                this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
                successCount++;
                final Cell statusCell = loanRepaymentSheet.getRow(loanRepayment.getRowIndex())
                        .createCell(LoanRepaymentConstants.STATUS_COL);
                statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
            } catch (RuntimeException ex) {
                errorCount++;
                LOG.error("Problem occurred in importEntity function", ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(loanRepaymentSheet, loanRepayment.getRowIndex(), errorMessage,
                        LoanRepaymentConstants.STATUS_COL);
            }

        }
        loanRepaymentSheet.setColumnWidth(LoanRepaymentConstants.STATUS_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        ImportHandlerUtils.writeString(LoanRepaymentConstants.STATUS_COL,
                loanRepaymentSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        return Count.instance(successCount, errorCount);
    }

}
