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

package org.apache.fineract.infrastructure.bulkimport.populator.clientblock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.bulkimport.constants.BlockClientConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.populator.AbstractWorkbookPopulator;
import org.apache.poi.hssf.usermodel.HSSFDataValidationHelper;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;

@RequiredArgsConstructor
@Slf4j
public class ClientBlockControlWorkbookPopulator extends AbstractWorkbookPopulator {

    @Override
    public void populate(Workbook workbook, String dateFormat) {
        Sheet blockClientSheet = workbook.createSheet(TemplatePopulateImportConstants.BLOCK_CLIENT_SHEET_NAME);
        setRules(blockClientSheet);
        setLayout(blockClientSheet);
    }

    private void setRules(Sheet worksheet) {

        CellRangeAddressList idTypeRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                BlockClientConstants.ID_TYPE_COL, BlockClientConstants.ID_TYPE_COL);
        CellRangeAddressList idNumberRAnge = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                BlockClientConstants.ID_NUMBER_COL, BlockClientConstants.ID_NUMBER_COL);
        CellRangeAddressList companyNameRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                BlockClientConstants.COMPANY_NAME_COL, BlockClientConstants.COMPANY_NAME_COL);

        DataValidationHelper validationHelper = new HSSFDataValidationHelper((HSSFSheet) worksheet);

        String[] idTypes = { "NIT", "CEDULA" };
        DataValidationConstraint idTypeValidator = validationHelper.createExplicitListConstraint(idTypes);

        DataValidationConstraint idNumberConstraint = validationHelper
                .createCustomConstraint("NOT(ISBLANK(INDIRECT(ADDRESS(ROW(), " + (BlockClientConstants.ID_NUMBER_COL + 1) + "))))");

        DataValidationConstraint companyNameConstraint = validationHelper
                .createCustomConstraint("IF(INDIRECT(ADDRESS(ROW(), " + (BlockClientConstants.ID_TYPE_COL + 1) + "))=\"NIT\", "
                        + "NOT(ISBLANK(INDIRECT(ADDRESS(ROW(), " + (BlockClientConstants.COMPANY_NAME_COL + 1) + ")))), TRUE)");

        DataValidation idTypeValidation = validationHelper.createValidation(idTypeValidator, idTypeRange);
        DataValidation idNumberValidation = validationHelper.createValidation(idNumberConstraint, idNumberRAnge);
        DataValidation companyNameValidation = validationHelper.createValidation(companyNameConstraint, companyNameRange);

        worksheet.addValidationData(idTypeValidation);
        worksheet.addValidationData(companyNameValidation);
        worksheet.addValidationData(idNumberValidation);

    }

    private void setLayout(Sheet worksheet) {
        Row rowHeader = worksheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);
        worksheet.setColumnWidth(BlockClientConstants.ID_TYPE_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(BlockClientConstants.ID_NUMBER_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(BlockClientConstants.FIRST_NAME_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(BlockClientConstants.SECOND_NAME_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(BlockClientConstants.SURNAME_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(BlockClientConstants.SECOND_SURNAME_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(BlockClientConstants.COMPANY_NAME_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(BlockClientConstants.CAUSAL_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(BlockClientConstants.OBSERVATION_COL, TemplatePopulateImportConstants.LARGE_COL_SIZE);

        writeString(BlockClientConstants.ID_TYPE_COL, rowHeader, "Tipo de Identificación*");
        writeString(BlockClientConstants.ID_NUMBER_COL, rowHeader, "Numero de Identificación*");
        writeString(BlockClientConstants.FIRST_NAME_COL, rowHeader, "Primer Nombre*");
        writeString(BlockClientConstants.SECOND_NAME_COL, rowHeader, "Segundo Nombre");
        writeString(BlockClientConstants.SURNAME_COL, rowHeader, "Primer Apellido");
        writeString(BlockClientConstants.SECOND_SURNAME_COL, rowHeader, "Segundo Apellido");
        writeString(BlockClientConstants.COMPANY_NAME_COL, rowHeader, "Nombre de la Empresa");
        writeString(BlockClientConstants.CAUSAL_COL, rowHeader, "Causal");
        writeString(BlockClientConstants.OBSERVATION_COL, rowHeader, "Observaciones");

    }

}
