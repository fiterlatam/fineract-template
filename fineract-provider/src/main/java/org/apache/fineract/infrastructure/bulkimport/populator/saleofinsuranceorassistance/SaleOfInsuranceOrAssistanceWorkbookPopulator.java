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

package org.apache.fineract.infrastructure.bulkimport.populator.saleofinsuranceorassistance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.bulkimport.constants.CommercePointOfSaleConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.FixedDepositConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.SaleOfInsuranceOrAssistanceConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.populator.*;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.products.data.ProductData;
import org.apache.fineract.portfolio.savings.data.FixedDepositProductData;
import org.apache.poi.hssf.usermodel.HSSFDataValidationHelper;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;

import java.util.List;

@Slf4j
public class SaleOfInsuranceOrAssistanceWorkbookPopulator extends AbstractWorkbookPopulator {

    private final LoanProductSheetPopulator productSheetPopulator;

    public SaleOfInsuranceOrAssistanceWorkbookPopulator(LoanProductSheetPopulator loanProductSheetPopulator) {
        this.productSheetPopulator = loanProductSheetPopulator;
    }

    @Override
    public void populate(Workbook workbook, String dateFormat) {
        Sheet saleOfInsuranceOrAssistance = workbook.createSheet(TemplatePopulateImportConstants.SALE_OF_INSURANCE_OR_ASSISTANCE_SHEET_NAME);
        productSheetPopulator.populate(workbook, dateFormat);
        setLayout(saleOfInsuranceOrAssistance);
        setRules(saleOfInsuranceOrAssistance);
        setDefaults(saleOfInsuranceOrAssistance);
    }

    private void setRules(Sheet worksheet) {
        CellRangeAddressList productNameRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                SaleOfInsuranceOrAssistanceConstants.INSURANCE_PRODUCT_COL, SaleOfInsuranceOrAssistanceConstants.INSURANCE_PRODUCT_COL);
        setNames(worksheet);

        DataValidationHelper validationHelper = new HSSFDataValidationHelper((HSSFSheet) worksheet);
        DataValidationConstraint productNameConstraint = validationHelper.createFormulaListConstraint("Productos");
        DataValidation productNameValidation = validationHelper.createValidation(productNameConstraint, productNameRange);
        worksheet.addValidationData(productNameValidation);


    }

    private void setLayout(Sheet worksheet) {
        Row rowHeader = worksheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);
        worksheet.setColumnWidth(SaleOfInsuranceOrAssistanceConstants.CUSTOMER_ID_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(SaleOfInsuranceOrAssistanceConstants.INSURANCE_PRODUCT_COL, TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
        worksheet.setColumnWidth(SaleOfInsuranceOrAssistanceConstants.TERM_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(SaleOfInsuranceOrAssistanceConstants.INSURANCE_CODE_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(SaleOfInsuranceOrAssistanceConstants.ADVISOR_ID_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(SaleOfInsuranceOrAssistanceConstants.SALES_CHANNEL_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        writeString(SaleOfInsuranceOrAssistanceConstants.CUSTOMER_ID_COL, rowHeader, "Cédula del cliente*");
        writeString(SaleOfInsuranceOrAssistanceConstants.INSURANCE_PRODUCT_COL, rowHeader, "Producto de seguro*");
        writeString(SaleOfInsuranceOrAssistanceConstants.TERM_COL, rowHeader, "Plazo*");
        writeString(SaleOfInsuranceOrAssistanceConstants.INSURANCE_CODE_COL, rowHeader, "Código del seguro*");
        writeString(SaleOfInsuranceOrAssistanceConstants.ADVISOR_ID_COL, rowHeader, "Cédula del asesor*");
        writeString(SaleOfInsuranceOrAssistanceConstants.SALES_CHANNEL_COL, rowHeader, "Canal de venta *");
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

    private void setNames(Sheet worksheet) {
        Workbook salesOfInsuranceWorkbook = worksheet.getWorkbook();

        List<LoanProductData> products = productSheetPopulator.getProducts();

        // Product Name
        Name productGroup = salesOfInsuranceWorkbook.createName();
        productGroup.setNameName("Productos");
        productGroup.setRefersToFormula(
                TemplatePopulateImportConstants.PRODUCT_SHEET_NAME + "!$B$2:$B$" + (productSheetPopulator.getProductsSize() + 1));

    }

}
