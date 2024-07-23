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

package org.apache.fineract.infrastructure.bulkimport.populator.commercepointofsale.clientvip;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.bulkimport.constants.CommercePointOfSaleConstants;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;

@RequiredArgsConstructor
@Slf4j
public class CommercePointOfSaleControlWorkbookPopulator extends AbstractWorkbookPopulator {

    @Override
    public void populate(Workbook workbook, String dateFormat) {
        Sheet commercePointOfSaleSheet = workbook.createSheet(TemplatePopulateImportConstants.COMMERCE_POINT_OF_SALE_SHEET_NAME);
        setRules(commercePointOfSaleSheet);
        setLayout(commercePointOfSaleSheet);
        setDefaults(commercePointOfSaleSheet);
    }

    private void setRules(Sheet worksheet) {
        final CellRangeAddressList pointOfSaleCodeRAnge = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                CommercePointOfSaleConstants.POINT_OF_SALE_CODE, CommercePointOfSaleConstants.POINT_OF_SALE_CODE);
        final DataValidationHelper validationHelper = new HSSFDataValidationHelper((HSSFSheet) worksheet);
        final DataValidationConstraint pointOfSaleCodeConstraint = validationHelper.createCustomConstraint(
                "NOT(ISBLANK(INDIRECT(ADDRESS(ROW(), " + (CommercePointOfSaleConstants.POINT_OF_SALE_CODE + 1) + "))))");
        final DataValidation pointOfSaleCodeValidation = validationHelper.createValidation(pointOfSaleCodeConstraint, pointOfSaleCodeRAnge);
        final String lengthFormula = "LEN(TEXT(,))<=6";
        final DataValidationConstraint pointOfSaleCodeLengthConstraint = validationHelper.createCustomConstraint(lengthFormula);
        final DataValidation pointOfSaleCodeLengthValidation = validationHelper.createValidation(pointOfSaleCodeLengthConstraint,
                pointOfSaleCodeRAnge);
        pointOfSaleCodeLengthValidation.setShowErrorBox(true);
        pointOfSaleCodeLengthValidation.createErrorBox("Error", "El código de punto de venta no puede tener más de 6 caracteres");
        worksheet.addValidationData(pointOfSaleCodeValidation);
        worksheet.addValidationData(pointOfSaleCodeLengthValidation);

    }

    private void setLayout(Sheet worksheet) {
        Row rowHeader = worksheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);
        worksheet.setColumnWidth(CommercePointOfSaleConstants.POINT_OF_SALE_CODE, TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
        writeString(CommercePointOfSaleConstants.POINT_OF_SALE_CODE, rowHeader, "Código de punto de venta*");
    }

    private void setDefaults(final Sheet worksheet) {
        for (int rowNo = 1; rowNo < 3000; rowNo++) {
            Row row = worksheet.getRow(rowNo);
            if (row == null) {
                row = worksheet.createRow(rowNo);
            }
            final Workbook workbook = worksheet.getWorkbook();
            final CellStyle textCellStyle = workbook.createCellStyle();
            final DataFormat fmt = workbook.createDataFormat();
            textCellStyle.setDataFormat(fmt.getFormat("@"));
            row.createCell(CommercePointOfSaleConstants.POINT_OF_SALE_CODE).setCellStyle(textCellStyle);
        }
    }

}
