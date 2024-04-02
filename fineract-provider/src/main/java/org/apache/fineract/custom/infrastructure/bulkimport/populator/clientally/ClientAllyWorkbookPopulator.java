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
package org.apache.fineract.custom.infrastructure.bulkimport.populator.clientally;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.fineract.custom.infrastructure.bulkimport.constants.CustomTemplatePopulateImportConstants;
import org.apache.fineract.custom.infrastructure.bulkimport.data.CustomGlobalEntityType;
import org.apache.fineract.custom.infrastructure.bulkimport.enumerator.ClientAllyTemplatePopulateImportEnum;
import org.apache.fineract.custom.infrastructure.bulkimport.populator.GenericCodeValueSheetPopulator;
import org.apache.fineract.custom.infrastructure.codes.data.CustomCodeValueData;
import org.apache.fineract.custom.infrastructure.codes.service.CustomCodeValueReadPlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.populator.AbstractWorkbookPopulator;
import org.apache.poi.hssf.usermodel.HSSFDataValidationHelper;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;

public class ClientAllyWorkbookPopulator extends AbstractWorkbookPopulator {

    private final CustomCodeValueReadPlatformService codeValueReadPlatformService;
    private List<GenericCodeValueSheetPopulator> genericCodeValueSheetPopulatorList = new ArrayList<>();

    public ClientAllyWorkbookPopulator(CustomCodeValueReadPlatformService codeValueReadPlatformService) {
        this.codeValueReadPlatformService = codeValueReadPlatformService;

        ClientAllyTemplatePopulateImportEnum.getOrdered().stream().filter(isCodeValue -> Objects.nonNull(isCodeValue.getCodeValueName()))
                .forEach(obj -> genericCodeValueSheetPopulatorList
                        .add(new GenericCodeValueSheetPopulator(obj.getCodeValueName(), codeValueReadPlatformService)));
    }

    @Override
    public void populate(Workbook workbook, String dateFormat) {
        Sheet staffSheet = workbook.createSheet(CustomGlobalEntityType.CLIENT_ALLY.getCode());

        genericCodeValueSheetPopulatorList.stream().forEach(sheetName -> sheetName.populate(workbook, dateFormat));

        writeHeaders(staffSheet, CustomTemplatePopulateImportConstants.ROWHEADER_INDEX);
        setRules(staffSheet, dateFormat);
    }

    private void writeHeaders(Sheet staffSheet, Integer rowIndex) {
        Row rowHeader = staffSheet.createRow(rowIndex);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);

        ClientAllyTemplatePopulateImportEnum.getOrdered().stream().forEach(obj -> headerSetup(staffSheet, rowHeader, obj));
    }

    private void headerSetup(Sheet staffSheet, Row rowHeader, ClientAllyTemplatePopulateImportEnum fieldDetailsEnum) {
        staffSheet.setColumnWidth(fieldDetailsEnum.getColumnIndex(), fieldDetailsEnum.getColumnSize());
        String required = (fieldDetailsEnum.getMandatory() ? " *" : "");
        writeString(fieldDetailsEnum.getColumnIndex(), rowHeader, fieldDetailsEnum.getColumnName() + required);
    }

    private void setRules(Sheet staffSheet, String dateFormat) {
        DataValidationHelper validationHelper = new HSSFDataValidationHelper((HSSFSheet) staffSheet);

        // Just add automatic rules
        ClientAllyTemplatePopulateImportEnum.getOrdered().stream()
                .forEach(obj -> createGenericCellValidatorsDependingOnTheFieldType(obj, staffSheet, validationHelper));
    }

    private void createGenericCellValidatorsDependingOnTheFieldType(ClientAllyTemplatePopulateImportEnum currentCodeValueEnum,
            Sheet staffSheet, DataValidationHelper validationHelper) {

        if (Boolean.class.equals(currentCodeValueEnum.getClazz()) || Objects.nonNull(currentCodeValueEnum.getCodeValueName())) {
            CellRangeAddressList cellRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                    currentCodeValueEnum.getColumnIndex(), currentCodeValueEnum.getColumnIndex());

            DataValidationConstraint departmentNameConstraint = null;

            if (Boolean.class.equals(currentCodeValueEnum.getClazz())) {
                departmentNameConstraint = validationHelper.createExplicitListConstraint(new String[] { "True", "False" });
            } else if (Objects.nonNull(currentCodeValueEnum.getCodeValueName())) {
                List<CustomCodeValueData> departments = codeValueReadPlatformService
                        .retrieveCodeValuesByCodeWithParent(currentCodeValueEnum.getCodeValueName());

                setNames(currentCodeValueEnum.getCodeValueName(), staffSheet, departments.size());
                departmentNameConstraint = validationHelper.createFormulaListConstraint(currentCodeValueEnum.getCodeValueName());
            }

            DataValidation departmentValidation = validationHelper.createValidation(departmentNameConstraint, cellRange);
            staffSheet.addValidationData(departmentValidation);
        }
    }

    private void setNames(String schemaName, Sheet staffSheet, Integer officesListSize) {
        Workbook staffWorkBook = staffSheet.getWorkbook();
        Name officeGroup = staffWorkBook.createName();
        officeGroup.setNameName(schemaName);
        officeGroup.setRefersToFormula(schemaName + "!$B$2:$B$" + (officesListSize + 1));
    }
}
