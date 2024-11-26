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
package org.apache.fineract.infrastructure.bulkimport.importhandler.commercepointofsale;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.bulkimport.constants.CommercePointOfSaleConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.portfolio.client.data.PointOfSalesData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class CommercePointOfSaleImportHandler implements ImportHandler {

    private final JdbcTemplate jdbcTemplate;
    private static final Logger LOG = LoggerFactory.getLogger(CommercePointOfSaleImportHandler.class);

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat, Map<String, Object> importAttribute) {
        final List<PointOfSalesData> pointOfSalesDataList = readExcelFile(workbook);
        return importEntity(workbook, pointOfSalesDataList);
    }

    public List<PointOfSalesData> readExcelFile(final Workbook workbook) {
        final List<PointOfSalesData> pointOfSalesDataList = new ArrayList<>();
        final Sheet commercePointOfSaleSheet = workbook.getSheet(TemplatePopulateImportConstants.COMMERCE_POINT_OF_SALE_SHEET_NAME);
        final Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(commercePointOfSaleSheet, 0);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            final Row row = commercePointOfSaleSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, CommercePointOfSaleConstants.STATUS_COL)) {
                pointOfSalesDataList.add(readPointOfSale(row, rowIndex));
            }
        }
        return pointOfSalesDataList;
    }

    private PointOfSalesData readPointOfSale(final Row row, final Integer rowIndex) {
        final String code = readStringSpecial(row);
        return PointOfSalesData.instance(code, rowIndex);
    }

    public Count importEntity(final Workbook workbook, List<PointOfSalesData> pointOfSalesDataList) {
        final Sheet commercePointOfSaleSheet = workbook.getSheet(TemplatePopulateImportConstants.COMMERCE_POINT_OF_SALE_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        if (pointOfSalesDataList != null) {
            pointOfSalesDataList = pointOfSalesDataList.stream().filter(pos -> pos.getCode() != null).toList();
            this.jdbcTemplate.execute("TRUNCATE TABLE custom.c_commerce_point_of_sale RESTART IDENTITY CASCADE ");
            final String sql = "INSERT INTO custom.c_commerce_point_of_sale (point_of_sale_code) VALUES (?) ON CONFLICT (point_of_sale_code) DO NOTHING ";
            for (final PointOfSalesData pointOfSalesData : pointOfSalesDataList) {
                String errorMessage;
                try {
                    this.jdbcTemplate.update(sql, pointOfSalesData.getCode());
                    successCount++;
                    Cell statusCell = commercePointOfSaleSheet.getRow(pointOfSalesData.getRowIndex())
                            .createCell(CommercePointOfSaleConstants.STATUS_COL);
                    statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                    statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
                } catch (DataIntegrityViolationException ex) {
                    errorCount++;
                    LOG.error("Problem occurred in importEntity function", ex);
                    if (ex.getMessage().contains("ERROR: value too long for type character varying(6)")) {
                        errorMessage = "El código del punto de venta es demasiado largo (6)";
                    } else {
                        errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                    }
                    ImportHandlerUtils.writeErrorMessage(commercePointOfSaleSheet, pointOfSalesData.getRowIndex(), errorMessage,
                            CommercePointOfSaleConstants.STATUS_COL);
                } catch (RuntimeException ex) {
                    errorCount++;
                    LOG.error("Problem occurred in importEntity function", ex);
                    errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                    ImportHandlerUtils.writeErrorMessage(commercePointOfSaleSheet, pointOfSalesData.getRowIndex(), errorMessage,
                            CommercePointOfSaleConstants.STATUS_COL);
                }
            }
        }
        commercePointOfSaleSheet.setColumnWidth(CommercePointOfSaleConstants.STATUS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        ImportHandlerUtils.writeString(CommercePointOfSaleConstants.STATUS_COL,
                commercePointOfSaleSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COLUMN_HEADER);
        return Count.instance(successCount, errorCount);
    }

    private String readStringSpecial(final Row row) {
        Cell c = row.getCell(CommercePointOfSaleConstants.POINT_OF_SALE_CODE);
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
