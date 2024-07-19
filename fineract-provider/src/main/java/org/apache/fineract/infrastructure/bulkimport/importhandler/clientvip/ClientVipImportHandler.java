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
package org.apache.fineract.infrastructure.bulkimport.importhandler.clientvip;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.bulkimport.constants.ClientVipConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ClientVipImportHandler implements ImportHandler {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat, Map<String, Object> importAttribute) {
        List<ClientData> clients = readExcelFile(workbook);
        return importEntity(workbook, clients);
    }

    public List<ClientData> readExcelFile(final Workbook workbook) {
        List<ClientData> clients = new ArrayList<>();
        Sheet clientVipBlockSheet = workbook.getSheet(TemplatePopulateImportConstants.CLIENT_VIP_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(clientVipBlockSheet, 0);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = clientVipBlockSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, ClientVipConstants.STATUS_COL)) {
                clients.add(readClient(row, rowIndex));
            }
        }
        return clients;
    }

    private ClientData readClient(final Row row, final Integer rowIndex) {
        final String idNumber = readStringSpecial(row);
        final ClientData clientData = new ClientData();
        clientData.setIdNumber(idNumber);
        clientData.setRowIndex(rowIndex);
        return clientData;
    }

    public Count importEntity(final Workbook workbook, final List<ClientData> clients) {
        final Sheet clientVipSheet = workbook.getSheet(TemplatePopulateImportConstants.CLIENT_VIP_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        if (clients != null) {
            successCount = clients.size();
            this.jdbcTemplate.execute("TRUNCATE TABLE custom.c_vip_client");
            String sql = "INSERT INTO custom.c_vip_client (client_id) VALUES (?)";
            for (final ClientData clientData : clients) {
                if (clientData.getIdNumber() != null) {
                    this.jdbcTemplate.update(sql, clientData.getIdNumber());
                }
            }
        }
        clientVipSheet.setColumnWidth(ClientVipConstants.STATUS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        ImportHandlerUtils.writeString(ClientVipConstants.STATUS_COL,
                clientVipSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COLUMN_HEADER);
        return Count.instance(successCount, errorCount);
    }

    private String readStringSpecial(final Row row) {
        Cell c = row.getCell(ClientVipConstants.ID_NUMBER_COL);
        if (c == null || c.getCellType() == CellType.BLANK) {
            return null;
        }
        FormulaEvaluator eval = row.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
        if (c.getCellType() == CellType.FORMULA) {
            if (eval != null) {
                CellValue value;
                try {
                    value = eval.evaluate(c);

                    String res = ImportHandlerUtils.trimEmptyDecimalPortion(value.getStringValue());

                    if (!StringUtils.isNotEmpty(res)) {
                        return res.trim();
                    }
                } catch (Exception e) {
                    log.error("Cell evaluation error: ", e);
                }
            }
            return null;
        } else if (c.getCellType() == CellType.STRING) {
            String res = ImportHandlerUtils.trimEmptyDecimalPortion(c.getStringCellValue().trim());
            return res.trim();
        } else if (c.getCellType() == CellType.NUMERIC) {
            DecimalFormat df = new DecimalFormat("#");
            df.setMaximumFractionDigits(0);
            return df.format(c.getNumericCellValue());
        } else if (c.getCellType() == CellType.BOOLEAN) {
            return c.getBooleanCellValue() + "";
        } else {
            return null;
        }
    }

}
