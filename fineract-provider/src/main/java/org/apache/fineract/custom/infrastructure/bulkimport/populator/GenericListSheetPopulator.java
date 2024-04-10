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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.populator.AbstractWorkbookPopulator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

@Slf4j
public class GenericListSheetPopulator extends AbstractWorkbookPopulator {

    private String sheetName;
    private List<?> dtoList;
    private String methodName;
    private String aditionalMethodName;
    private Class<?> clazz;

    private static final int ID_COL = 0;
    private static final int DESCRIPTION_COL = 1;

    public GenericListSheetPopulator(final String sheetName, List<?> dtoList, Class<?> clazz, String methodName,
            String aditionalMethodName) {
        this.sheetName = sheetName.replaceAll(" ", "_");
        this.dtoList = dtoList;
        this.clazz = clazz;
        this.methodName = methodName;
        this.aditionalMethodName = aditionalMethodName;
    }

    @Override
    public void populate(final Workbook workbook, String dateFormat) {
        int rowIndex = 1;
        Sheet officeSheet = workbook.createSheet(sheetName);
        setLayout(officeSheet);

        populateSheet(officeSheet, rowIndex);
        officeSheet.protectSheet("");
    }

    private void populateSheet(Sheet officeSheet, int rowIndex) {

        dtoList = dtoList.stream().sorted(Comparator.comparing(dto -> {
            try {
                Method getNameMethod = clazz.getDeclaredMethod(methodName);
                return (String) getNameMethod.invoke(dto);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.error("Error while sorting the list", e);
            }
            return null;
        })).collect(Collectors.toList());

        for (Object currentDTO : dtoList) {
            Row row = officeSheet.createRow(rowIndex);

            try {
                Method getIdMethod = clazz.getDeclaredMethod("getId");
                Method getNameMethod = clazz.getDeclaredMethod(methodName);

                Long idValue = (Long) getIdMethod.invoke(currentDTO);
                String nameValue = (String) getNameMethod.invoke(currentDTO);

                if (Objects.nonNull(aditionalMethodName)) {
                    String additionalMethodNameRaw = aditionalMethodName.replaceAll("get", "");

                    Method getAditionalMethod = clazz.getDeclaredMethod(aditionalMethodName);
                    nameValue = nameValue.concat(" - ").concat(additionalMethodNameRaw).concat(" ")
                            .concat((String) getAditionalMethod.invoke(currentDTO));
                }

                writeLong(ID_COL, row, idValue);
                writeString(DESCRIPTION_COL, row, nameValue.trim().replaceAll("[ )(]", "_"));

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.error("error stack: ", e);
            }

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
}
