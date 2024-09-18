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
package org.apache.fineract.infrastructure.bulkimport.populator.loanwriteoff;

import org.apache.fineract.infrastructure.bulkimport.constants.LoanWriteOffConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.populator.AbstractWorkbookPopulator;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class LoanWriteOffWorkbookPopulator extends AbstractWorkbookPopulator {

    @Override
    public void populate(Workbook workbook, String dateFormat) {
        final Sheet loanWriteOffSheet = workbook.createSheet(TemplatePopulateImportConstants.LOAN_WRITE_OFF_SHEET_NAME);
        setLayout(loanWriteOffSheet, workbook);
    }

    private void setLayout(final Sheet worksheet, final Workbook workbook) {
        final Row rowHeader = worksheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);
        worksheet.setColumnWidth(LoanWriteOffConstants.LOAN_ACCOUNT_NO_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(LoanWriteOffConstants.WRITE_OFF_AMOUNT_COL, TemplatePopulateImportConstants.LARGE_COL_SIZE);
        worksheet.setColumnWidth(LoanWriteOffConstants.STATUS_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);

        writeString(LoanWriteOffConstants.LOAN_ACCOUNT_NO_COL, rowHeader, "No de crédito");
        writeString(LoanWriteOffConstants.WRITE_OFF_AMOUNT_COL, rowHeader, "Valor total a condonar");

        final CellStyle headerStyle = headerStyle(workbook);
        rowHeader.getCell(LoanWriteOffConstants.LOAN_ACCOUNT_NO_COL).setCellStyle(headerStyle);
        rowHeader.getCell(LoanWriteOffConstants.WRITE_OFF_AMOUNT_COL).setCellStyle(headerStyle);
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
