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
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.LoanWriteOffConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.delinquency.service.DelinquencyReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.CollectionData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
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
    private final LoanReadPlatformService loanReadPlatformService;
    private final PlatformSecurityContext context;
    private final DelinquencyReadPlatformService delinquencyReadPlatformService;
    private final ClientReadPlatformService clientReadPlatformService;

    @Autowired
    public LoanWriteOffImportHandler(final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            LoanReadPlatformService loanReadPlatformService, PlatformSecurityContext context,
            DelinquencyReadPlatformService delinquencyReadPlatformService, ClientReadPlatformService clientReadPlatformService) {
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.loanReadPlatformService = loanReadPlatformService;
        this.context = context;
        this.delinquencyReadPlatformService = delinquencyReadPlatformService;
        this.clientReadPlatformService = clientReadPlatformService;
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
        ImportHandlerUtils.writeString(LoanWriteOffConstants.STATUS_COL,
                loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        ImportHandlerUtils.writeString(LoanWriteOffConstants.WRITE_OFF_DATE_COL,
                loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "Fecha de la operación");
        ImportHandlerUtils.writeString(LoanWriteOffConstants.OUTSTANDING_AMOUNT_COL,
                loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "Monto de deuda");
        ImportHandlerUtils.writeString(LoanWriteOffConstants.CLIENT_ID_COL,
                loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "Identificador del cliente");
        ImportHandlerUtils.writeString(LoanWriteOffConstants.CLIENT_NAME_COL,
                loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "Nombre del cliente");
        ImportHandlerUtils.writeString(LoanWriteOffConstants.LOAN_PRODUCT_NAME_COL,
                loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "Producto de crédito");
        ImportHandlerUtils.writeString(LoanWriteOffConstants.DAYS_IN_ARREARS_COL,
                loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "Días de mora");
        ImportHandlerUtils.writeString(LoanWriteOffConstants.CREATED_BY_USER_NAME_COL,
                loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "Usuario que aplicó la condonación");

        loanWriteOffSheet.setColumnWidth(LoanWriteOffConstants.WRITE_OFF_DATE_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        loanWriteOffSheet.setColumnWidth(LoanWriteOffConstants.OUTSTANDING_AMOUNT_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        loanWriteOffSheet.setColumnWidth(LoanWriteOffConstants.CLIENT_ID_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        loanWriteOffSheet.setColumnWidth(LoanWriteOffConstants.CLIENT_NAME_COL, TemplatePopulateImportConstants.LARGE_COL_SIZE);
        loanWriteOffSheet.setColumnWidth(LoanWriteOffConstants.LOAN_PRODUCT_NAME_COL, TemplatePopulateImportConstants.LARGE_COL_SIZE);
        loanWriteOffSheet.setColumnWidth(LoanWriteOffConstants.DAYS_IN_ARREARS_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        loanWriteOffSheet.setColumnWidth(LoanWriteOffConstants.CREATED_BY_USER_NAME_COL, TemplatePopulateImportConstants.LARGE_COL_SIZE);

        final CellStyle headerStyle = headerStyle(workbook);
        loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX).getCell(LoanWriteOffConstants.STATUS_COL)
                .setCellStyle(headerStyle);
        loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX).getCell(LoanWriteOffConstants.WRITE_OFF_DATE_COL)
                .setCellStyle(headerStyle);
        loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX).getCell(LoanWriteOffConstants.OUTSTANDING_AMOUNT_COL)
                .setCellStyle(headerStyle);
        loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX).getCell(LoanWriteOffConstants.CLIENT_ID_COL)
                .setCellStyle(headerStyle);
        loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX).getCell(LoanWriteOffConstants.CLIENT_NAME_COL)
                .setCellStyle(headerStyle);
        loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX).getCell(LoanWriteOffConstants.LOAN_PRODUCT_NAME_COL)
                .setCellStyle(headerStyle);
        loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX).getCell(LoanWriteOffConstants.DAYS_IN_ARREARS_COL)
                .setCellStyle(headerStyle);
        loanWriteOffSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX).getCell(LoanWriteOffConstants.CREATED_BY_USER_NAME_COL)
                .setCellStyle(headerStyle);

        final LocalDate transactionDate = DateUtils.getBusinessLocalDate();
        final CellStyle dateCellStyle = workbook.createCellStyle();
        final short dataFormat = workbook.createDataFormat().getFormat(dateFormat);
        dateCellStyle.setDataFormat(dataFormat);

        final AppUser createdByUser = context.authenticatedUser();
        final String createdByUsername = createdByUser.getDisplayName();

        for (final LoanTransactionData loanWriteOffData : loanWriteOffs) {
            try {
                loanWriteOffSheet.getRow(loanWriteOffData.getRowIndex()).createCell(LoanWriteOffConstants.CREATED_BY_USER_NAME_COL)
                        .setCellValue(createdByUsername);
                final Long loanAccountId = loanWriteOffData.getAccountId();
                final LoanAccountData loanAccountData = this.loanReadPlatformService.retrieveOne(loanAccountId);
                final CollectionData collectionData = this.delinquencyReadPlatformService.calculateLoanCollectionData(loanAccountId);
                final ClientData clientData = clientReadPlatformService.retrieveOne(loanAccountData.getClientId());
                final String loanClientId = clientData.getIdNumber();
                final String loanClientName = loanAccountData.getClientName();
                final String loanProductName = loanAccountData.getLoanProductName();
                final Long daysInArrears = collectionData.getPastDueDays();
                final BigDecimal outstandingPrincipalAmount = loanAccountData.getSummary().getPrincipalOutstanding();
                loanWriteOffSheet.getRow(loanWriteOffData.getRowIndex()).createCell(LoanWriteOffConstants.OUTSTANDING_AMOUNT_COL)
                        .setCellValue(outstandingPrincipalAmount.doubleValue());
                loanWriteOffSheet.getRow(loanWriteOffData.getRowIndex()).createCell(LoanWriteOffConstants.CLIENT_ID_COL)
                        .setCellValue(loanClientId);
                loanWriteOffSheet.getRow(loanWriteOffData.getRowIndex()).createCell(LoanWriteOffConstants.CLIENT_NAME_COL)
                        .setCellValue(loanClientName);
                loanWriteOffSheet.getRow(loanWriteOffData.getRowIndex()).createCell(LoanWriteOffConstants.LOAN_PRODUCT_NAME_COL);
                loanWriteOffSheet.getRow(loanWriteOffData.getRowIndex()).createCell(LoanWriteOffConstants.LOAN_PRODUCT_NAME_COL)
                        .setCellValue(loanProductName);
                loanWriteOffSheet.getRow(loanWriteOffData.getRowIndex()).createCell(LoanWriteOffConstants.DAYS_IN_ARREARS_COL)
                        .setCellValue(daysInArrears);

                final Cell writeOffDateCell = loanWriteOffSheet.getRow(loanWriteOffData.getRowIndex())
                        .createCell(LoanWriteOffConstants.WRITE_OFF_DATE_COL);
                writeOffDateCell.setCellStyle(dateCellStyle);
                writeOffDateCell.setCellValue(transactionDate);

                final JsonObject loanRepaymentJsonObj = gsonBuilder.create().toJsonTree(loanWriteOffData).getAsJsonObject();
                loanRepaymentJsonObj.remove("manuallyReversed");
                loanRepaymentJsonObj.remove("numberOfRepayments");
                final String payload = loanRepaymentJsonObj.toString();
                final CommandWrapper commandRequest = new CommandWrapperBuilder().specialWriteOffLoanTransaction(loanAccountId)
                        .withJson(payload).build();
                this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
                successCount++;
                final Cell statusCell = loanWriteOffSheet.getRow(loanWriteOffData.getRowIndex())
                        .createCell(LoanWriteOffConstants.STATUS_COL);
                statusCell.setCellValue("Crédito condonado");
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
                loanWriteOffSheet.setColumnWidth(LoanWriteOffConstants.STATUS_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
            } catch (RuntimeException ex) {
                errorCount++;
                LOG.error("Problem occurred in importEntity function", ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                if (ex instanceof GeneralPlatformDomainRuleException generalPlatformDomainRuleException) {
                    final String globalisationMessageCode = generalPlatformDomainRuleException.getGlobalisationMessageCode();
                    if (globalisationMessageCode != null
                            && globalisationMessageCode.equals("error.msg.loan.write.off.amount.is.greater.than.outstanding.loan.amount")) {
                        final Object[] defaultUserMessageArgs = generalPlatformDomainRuleException.getDefaultUserMessageArgs();
                        if (defaultUserMessageArgs != null && defaultUserMessageArgs.length > 2) {
                            final BigDecimal outstandingLoanAmount = (BigDecimal) defaultUserMessageArgs[2];
                            final Cell outstandingAmountCell = loanWriteOffSheet.getRow(loanWriteOffData.getRowIndex())
                                    .createCell(LoanWriteOffConstants.OUTSTANDING_AMOUNT_COL);
                            outstandingAmountCell.setCellValue(outstandingLoanAmount.doubleValue());
                        }
                    }
                    errorMessage = generalPlatformDomainRuleException.getDefaultUserMessage();
                } else if (ex instanceof PlatformApiDataValidationException platformApiDataValidationException) {
                    errorMessage = platformApiDataValidationException.getDefaultUserMessage();
                    final List<ApiParameterError> errors = platformApiDataValidationException.getErrors();
                    if (CollectionUtils.isNotEmpty(errors)) {
                        errorMessage = errorMessage + ": "
                                + errors.stream().map(ApiParameterError::getDefaultUserMessage).collect(Collectors.joining(", "));
                    }
                }
                ImportHandlerUtils.writeErrorMessage(loanWriteOffSheet, loanWriteOffData.getRowIndex(), errorMessage,
                        LoanWriteOffConstants.STATUS_COL);
                loanWriteOffSheet.setColumnWidth(LoanWriteOffConstants.STATUS_COL, TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
            }

        }
        return Count.instance(successCount, errorCount);
    }

    private CellStyle headerStyle(final Workbook workbook) {
        final Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        final CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

}
