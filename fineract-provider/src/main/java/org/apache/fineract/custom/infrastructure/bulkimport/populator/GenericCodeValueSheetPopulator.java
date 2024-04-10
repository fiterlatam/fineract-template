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
package org.apache.fineract.custom.infrastructure.bulkimport.populator;

import java.util.List;
import java.util.Objects;
import org.apache.fineract.custom.infrastructure.codes.data.CustomCodeValueData;
import org.apache.fineract.custom.infrastructure.codes.service.CustomCodeValueReadPlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.populator.AbstractWorkbookPopulator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class GenericCodeValueSheetPopulator extends AbstractWorkbookPopulator {

    private String codeValueName;
    private List<CustomCodeValueData> codeValueList;

    private static final int ID_COL = 0;
    private static final int DESCRIPTION_COL = 1;

    public GenericCodeValueSheetPopulator(final String codeValueName, CustomCodeValueReadPlatformService codeValueReadPlatformService) {
        this.codeValueName = codeValueName;
        this.codeValueList = codeValueReadPlatformService.retrieveCodeValuesByCodeWithParent(codeValueName);
    }

    @Override
    public void populate(final Workbook workbook, String dateFormat) {
        int rowIndex = 1;
        Sheet officeSheet = workbook.createSheet(codeValueName);
        setLayout(officeSheet);

        populateSheet(officeSheet, rowIndex);
        officeSheet.protectSheet("");
    }

    private void populateSheet(Sheet officeSheet, int rowIndex) {
        for (CustomCodeValueData office : codeValueList) {
            Row row = officeSheet.createRow(rowIndex);

            StringBuilder sb = new StringBuilder();
            if (Objects.nonNull(office.getParentName())) {
                sb.append(office.getParentName()).append(" - ");
            }
            sb.append(office.getName());
            writeLong(ID_COL, row, office.getId());
            writeString(DESCRIPTION_COL, row, sb.toString().trim().replaceAll("[ )(]", "_"));

            rowIndex++;
        }
    }

    private void setLayout(Sheet worksheet) {
        worksheet.setColumnWidth(ID_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(DESCRIPTION_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        Row rowHeader = worksheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);
        writeString(ID_COL, rowHeader, "ID");
        writeString(DESCRIPTION_COL, rowHeader, "Name");
    }

    public List<CustomCodeValueData> getCodeValueDataList() {
        return codeValueList;
    }
}
