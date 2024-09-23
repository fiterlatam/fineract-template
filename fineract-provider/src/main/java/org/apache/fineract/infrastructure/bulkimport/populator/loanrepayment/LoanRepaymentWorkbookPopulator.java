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
package org.apache.fineract.infrastructure.bulkimport.populator.loanrepayment;

import java.util.ArrayList;
import org.apache.fineract.infrastructure.bulkimport.constants.LoanRepaymentConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.populator.AbstractWorkbookPopulator;
import org.apache.fineract.infrastructure.bulkimport.populator.ExtrasSheetPopulator;
import org.apache.fineract.infrastructure.bulkimport.populator.OfficeSheetPopulator;
import org.apache.poi.hssf.usermodel.HSSFDataValidationHelper;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;

public class LoanRepaymentWorkbookPopulator extends AbstractWorkbookPopulator {

    private final OfficeSheetPopulator officeSheetPopulator;
    private final ExtrasSheetPopulator extrasSheetPopulator;

    public LoanRepaymentWorkbookPopulator(OfficeSheetPopulator officeSheetPopulator, ExtrasSheetPopulator extrasSheetPopulator) {
        this.officeSheetPopulator = officeSheetPopulator;
        this.extrasSheetPopulator = extrasSheetPopulator;
    }

    @Override
    public void populate(Workbook workbook, String dateFormat) {
        final Sheet loanRepaymentSheet = workbook.createSheet(TemplatePopulateImportConstants.LOAN_REPAYMENT_SHEET_NAME);
        setLayout(loanRepaymentSheet, workbook);
        this.officeSheetPopulator.populate(workbook, dateFormat);
        this.extrasSheetPopulator.populate(workbook, dateFormat);
        setRules(loanRepaymentSheet, dateFormat);
        setDefaults(loanRepaymentSheet, dateFormat);
    }

    private void setDefaults(Sheet worksheet, String dateFormat) {
        final Workbook workbook = worksheet.getWorkbook();
        final CellStyle textCellStyle = workbook.createCellStyle();
        final DataFormat fmt = workbook.createDataFormat();
        textCellStyle.setDataFormat(fmt.getFormat("@"));
        final CellStyle dateCellStyle = workbook.createCellStyle();
        final short df = workbook.createDataFormat().getFormat(dateFormat);
        for (int rowNo = 1; rowNo < 3000; rowNo++) {
            Row row = worksheet.getRow(rowNo);
            if (row == null) {
                row = worksheet.createRow(rowNo);
            }
            dateCellStyle.setDataFormat(df);
            row.createCell(LoanRepaymentConstants.REPAID_ON_DATE_COL).setCellStyle(dateCellStyle);
            row.createCell(LoanRepaymentConstants.CLIENT_ID_COL).setCellStyle(textCellStyle);
        }
    }

    private void setRules(Sheet worksheet, String dateFormat) {
        CellRangeAddressList officeNameRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                LoanRepaymentConstants.OFFICE_NAME_COL, LoanRepaymentConstants.OFFICE_NAME_COL);
        CellRangeAddressList repaymentTypeRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                LoanRepaymentConstants.REPAYMENT_TYPE_COL, LoanRepaymentConstants.REPAYMENT_TYPE_COL);
        CellRangeAddressList repaymentBankRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                LoanRepaymentConstants.REPAYMENT_BANK_COL, LoanRepaymentConstants.REPAYMENT_BANK_COL);
        CellRangeAddressList repaymentDateRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                LoanRepaymentConstants.REPAID_ON_DATE_COL, LoanRepaymentConstants.REPAID_ON_DATE_COL);
        DataValidationHelper validationHelper = new HSSFDataValidationHelper((HSSFSheet) worksheet);
        setNames(worksheet);
        DataValidationConstraint officeNameConstraint = validationHelper.createFormulaListConstraint("Office");
        validationHelper.createFormulaListConstraint("INDIRECT(CONCATENATE(\"Client_\",$A1))");
        DataValidationConstraint paymentTypeConstraint = validationHelper.createFormulaListConstraint("PaymentTypes");
        DataValidationConstraint repaymentBankConstraint = validationHelper.createFormulaListConstraint("RepaymentBanks");
        DataValidationConstraint repaymentDateConstraint = validationHelper
                .createDateConstraint(DataValidationConstraint.OperatorType.LESS_OR_EQUAL, "=TODAY()", null, dateFormat);

        DataValidation officeValidation = validationHelper.createValidation(officeNameConstraint, officeNameRange);
        DataValidation repaymentTypeValidation = validationHelper.createValidation(paymentTypeConstraint, repaymentTypeRange);
        DataValidation repaymentBankValidation = validationHelper.createValidation(repaymentBankConstraint, repaymentBankRange);
        DataValidation repaymentDateValidation = validationHelper.createValidation(repaymentDateConstraint, repaymentDateRange);

        worksheet.addValidationData(officeValidation);
        worksheet.addValidationData(repaymentTypeValidation);
        worksheet.addValidationData(repaymentBankValidation);
        worksheet.addValidationData(repaymentDateValidation);

    }

    private void setNames(Sheet worksheet) {
        final ArrayList<String> officeNames = new ArrayList<>(officeSheetPopulator.getOfficeNames());
        final Workbook loanRepaymentWorkbook = worksheet.getWorkbook();
        final Name officeGroup = loanRepaymentWorkbook.createName();
        officeGroup.setNameName("Office");
        officeGroup.setRefersToFormula(TemplatePopulateImportConstants.OFFICE_SHEET_NAME + "!$B$2:$B$" + (officeNames.size() + 1));

        final Name paymentTypeGroup = loanRepaymentWorkbook.createName();
        paymentTypeGroup.setNameName("PaymentTypes");
        final String paymentTypesRefersToFormula = TemplatePopulateImportConstants.EXTRAS_SHEET_NAME + "!$D$2:$D$"
                + (extrasSheetPopulator.getPaymentTypesSize() + 1);
        paymentTypeGroup.setRefersToFormula(paymentTypesRefersToFormula);

        final Name repaymentChannelGroup = loanRepaymentWorkbook.createName();
        repaymentChannelGroup.setNameName("RepaymentChannels");
        repaymentChannelGroup.setRefersToFormula(
                TemplatePopulateImportConstants.EXTRAS_SHEET_NAME + "!$H$2:$H$" + (extrasSheetPopulator.getChannelOptions().size() + 1));

        final Name repaymentBankGroup = loanRepaymentWorkbook.createName();
        repaymentBankGroup.setNameName("RepaymentBanks");
        repaymentBankGroup.setRefersToFormula(
                TemplatePopulateImportConstants.EXTRAS_SHEET_NAME + "!$J$2:$J$" + (extrasSheetPopulator.getBankOptions().size() + 1));
    }

    private void setLayout(final Sheet worksheet, final Workbook workbook) {
        Row rowHeader = worksheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);
        worksheet.setColumnWidth(LoanRepaymentConstants.OFFICE_NAME_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.CLIENT_ID_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.LOAN_ACCOUNT_NO_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.PAYMENT_AMOUNT_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.REPAID_ON_DATE_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.REPAYMENT_TYPE_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.REPAYMENT_BANK_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.STATUS_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);

        writeString(LoanRepaymentConstants.OFFICE_NAME_COL, rowHeader, "nombre oficina");
        writeString(LoanRepaymentConstants.CLIENT_ID_COL, rowHeader, "cédula del cliente");
        writeString(LoanRepaymentConstants.LOAN_ACCOUNT_NO_COL, rowHeader, "No de crédito");
        writeString(LoanRepaymentConstants.PAYMENT_AMOUNT_COL, rowHeader, "Valor del pago");
        writeString(LoanRepaymentConstants.REPAID_ON_DATE_COL, rowHeader, "fecha");
        writeString(LoanRepaymentConstants.REPAYMENT_TYPE_COL, rowHeader, "tipo");
        writeString(LoanRepaymentConstants.REPAYMENT_BANK_COL, rowHeader, "banco");

        final CellStyle headerStyle = headerStyle(workbook);
        rowHeader.getCell(LoanRepaymentConstants.OFFICE_NAME_COL).setCellStyle(headerStyle);
        rowHeader.getCell(LoanRepaymentConstants.CLIENT_ID_COL).setCellStyle(headerStyle);
        rowHeader.getCell(LoanRepaymentConstants.LOAN_ACCOUNT_NO_COL).setCellStyle(headerStyle);
        rowHeader.getCell(LoanRepaymentConstants.PAYMENT_AMOUNT_COL).setCellStyle(headerStyle);
        rowHeader.getCell(LoanRepaymentConstants.REPAID_ON_DATE_COL).setCellStyle(headerStyle);
        rowHeader.getCell(LoanRepaymentConstants.REPAYMENT_TYPE_COL).setCellStyle(headerStyle);
        rowHeader.getCell(LoanRepaymentConstants.REPAYMENT_BANK_COL).setCellStyle(headerStyle);
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
