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

package org.apache.fineract.infrastructure.bulkimport.populator.clientvip;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.bulkimport.constants.ClientVipConstants;
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
public class ClientVipControlWorkbookPopulator extends AbstractWorkbookPopulator {

    @Override
    public void populate(Workbook workbook, String dateFormat) {
        Sheet blockVipSheet = workbook.createSheet(TemplatePopulateImportConstants.CLIENT_VIP_SHEET_NAME);
        setRules(blockVipSheet);
        setLayout(blockVipSheet);
        setDefaults(blockVipSheet);
    }

    private void setRules(Sheet worksheet) {
        CellRangeAddressList idNumberRAnge = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                ClientVipConstants.ID_NUMBER_COL, ClientVipConstants.ID_NUMBER_COL);
        DataValidationHelper validationHelper = new HSSFDataValidationHelper((HSSFSheet) worksheet);
        DataValidationConstraint idNumberConstraint = validationHelper
                .createCustomConstraint("NOT(ISBLANK(INDIRECT(ADDRESS(ROW(), " + (ClientVipConstants.ID_NUMBER_COL + 1) + "))))");
        DataValidation idNumberValidation = validationHelper.createValidation(idNumberConstraint, idNumberRAnge);
        worksheet.addValidationData(idNumberValidation);

    }

    private void setLayout(Sheet worksheet) {
        Row rowHeader = worksheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);
        worksheet.setColumnWidth(ClientVipConstants.ID_NUMBER_COL, TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
        writeString(ClientVipConstants.ID_NUMBER_COL, rowHeader, "Numero de Identificación*");
    }

    private void setDefaults(Sheet worksheet) {
        for (int rowNo = 1; rowNo < 3000; rowNo++) {
            Row row = worksheet.getRow(rowNo);
            if (row == null) {
                row = worksheet.createRow(rowNo);
            }
            Workbook workbook = worksheet.getWorkbook();
            CellStyle textCellStyle = workbook.createCellStyle();
            DataFormat fmt = workbook.createDataFormat();
            textCellStyle.setDataFormat(fmt.getFormat("@"));
            row.createCell(ClientVipConstants.ID_NUMBER_COL).setCellStyle(textCellStyle);
        }
    }

}
