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
package org.apache.fineract.infrastructure.bulkimport.populator.clientcupoincrement;

import org.apache.fineract.infrastructure.bulkimport.constants.ClientCupoIncrementConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.populator.AbstractWorkbookPopulator;
import org.apache.poi.hssf.usermodel.HSSFDataValidationHelper;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;

public class ClientCupoIncrementWorkbookPopulator extends AbstractWorkbookPopulator {

    @Override
    public void populate(Workbook workbook, String dateFormat) {
        final Sheet clientCupoIncrementSheet = workbook.createSheet(TemplatePopulateImportConstants.CLIENT_CUPO_INCREMENT_SHEET_NAME);
        setLayout(clientCupoIncrementSheet, workbook);
        setRules(clientCupoIncrementSheet, dateFormat);
        setDefaults(clientCupoIncrementSheet, dateFormat);
    }

    private void setRules(final Sheet worksheet, final String dateFormat) {
        final CellRangeAddressList documentTypeRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                ClientCupoIncrementConstants.DOCUMENT_TYPE_COL, ClientCupoIncrementConstants.DOCUMENT_TYPE_COL);
        final String[] documentTypes = { "NIT", "CEDULA" };
        final DataValidationHelper validationHelper = new HSSFDataValidationHelper((HSSFSheet) worksheet);
        final DataValidationConstraint documentTypeValidator = validationHelper.createExplicitListConstraint(documentTypes);
        final DataValidation documentTypeValidation = validationHelper.createValidation(documentTypeValidator, documentTypeRange);

        final DataValidationConstraint dateConstraint = validationHelper
                .createDateConstraint(DataValidationConstraint.OperatorType.LESS_OR_EQUAL, "=TODAY()", null, dateFormat);
        final CellRangeAddressList startOnDateRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                ClientCupoIncrementConstants.START_ON_DATE_COL, ClientCupoIncrementConstants.START_ON_DATE_COL);
        final DataValidation startOnDateValidation = validationHelper.createValidation(dateConstraint, startOnDateRange);

        final CellRangeAddressList endOnDateRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                ClientCupoIncrementConstants.END_ON_DATE_COL, ClientCupoIncrementConstants.END_ON_DATE_COL);
        final DataValidation endOnDateValidation = validationHelper.createValidation(dateConstraint, endOnDateRange);
        worksheet.addValidationData(documentTypeValidation);
        worksheet.addValidationData(startOnDateValidation);
        worksheet.addValidationData(endOnDateValidation);
    }

    private void setLayout(final Sheet worksheet, final Workbook workbook) {
        final Row rowHeader = worksheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);
        worksheet.setColumnWidth(ClientCupoIncrementConstants.DOCUMENT_TYPE_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(ClientCupoIncrementConstants.DOCUMENT_NUMBER_COL, TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
        worksheet.setColumnWidth(ClientCupoIncrementConstants.MAXIMUM_CUPO_AMOUNT_COL, TemplatePopulateImportConstants.LARGE_COL_SIZE);
        worksheet.setColumnWidth(ClientCupoIncrementConstants.START_ON_DATE_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(ClientCupoIncrementConstants.END_ON_DATE_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(ClientCupoIncrementConstants.STATUS_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);

        writeString(ClientCupoIncrementConstants.DOCUMENT_TYPE_COL, rowHeader, "el tipo de documento");
        writeString(ClientCupoIncrementConstants.DOCUMENT_NUMBER_COL, rowHeader, "el número de documento del cliente");
        writeString(ClientCupoIncrementConstants.MAXIMUM_CUPO_AMOUNT_COL, rowHeader, "valor de cupo principal máximo");
        writeString(ClientCupoIncrementConstants.START_ON_DATE_COL, rowHeader, "fecha desde");
        writeString(ClientCupoIncrementConstants.END_ON_DATE_COL, rowHeader, "fecha hasta");

        final CellStyle headerStyle = headerStyle(workbook);
        rowHeader.getCell(ClientCupoIncrementConstants.DOCUMENT_TYPE_COL).setCellStyle(headerStyle);
        rowHeader.getCell(ClientCupoIncrementConstants.DOCUMENT_NUMBER_COL).setCellStyle(headerStyle);
        rowHeader.getCell(ClientCupoIncrementConstants.MAXIMUM_CUPO_AMOUNT_COL).setCellStyle(headerStyle);
        rowHeader.getCell(ClientCupoIncrementConstants.START_ON_DATE_COL).setCellStyle(headerStyle);
        rowHeader.getCell(ClientCupoIncrementConstants.END_ON_DATE_COL).setCellStyle(headerStyle);
    }

    private void setDefaults(final Sheet worksheet, final String dateFormat) {
        final Workbook workbook = worksheet.getWorkbook();
        final CellStyle textCellStyle = workbook.createCellStyle();
        final DataFormat fmt = workbook.createDataFormat();
        textCellStyle.setDataFormat(fmt.getFormat("@"));
        final CellStyle dateCellStyle = workbook.createCellStyle();
        final short df = workbook.createDataFormat().getFormat(dateFormat);
        dateCellStyle.setDataFormat(df);
        for (int rowNo = 1; rowNo < 3000; rowNo++) {
            Row row = worksheet.getRow(rowNo);
            if (row == null) {
                row = worksheet.createRow(rowNo);
            }
            row.createCell(ClientCupoIncrementConstants.DOCUMENT_NUMBER_COL).setCellStyle(textCellStyle);
            row.createCell(ClientCupoIncrementConstants.START_ON_DATE_COL).setCellStyle(dateCellStyle);
            row.createCell(ClientCupoIncrementConstants.END_ON_DATE_COL).setCellStyle(dateCellStyle);
        }
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
