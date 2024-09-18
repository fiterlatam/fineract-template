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
package org.apache.fineract.infrastructure.bulkimport.importhandler.clientcupoincrement;

import com.google.gson.GsonBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.fineract.infrastructure.bulkimport.constants.ClientCupoIncrementConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.data.ClientAdditionalFieldsData;
import org.apache.fineract.portfolio.client.domain.ClientStatus;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClientCupoIncrementImportHandler implements ImportHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ClientCupoIncrementImportHandler.class);
    private final PlatformSecurityContext context;
    private final ClientReadPlatformService clientReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ClientCupoIncrementImportHandler(final PlatformSecurityContext context,
            final ClientReadPlatformService clientReadPlatformService, final CodeValueReadPlatformService codeValueReadPlatformService,
            JdbcTemplate jdbcTemplate) {
        this.context = context;
        this.clientReadPlatformService = clientReadPlatformService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat,
            final Map<String, Object> importAttributes) {
        final List<ClientCupoIncrementData> clientCupoIncrements = readExcelFile(workbook, locale, dateFormat);
        return importEntity(workbook, clientCupoIncrements, dateFormat, locale);
    }

    private List<ClientCupoIncrementData> readExcelFile(final Workbook workbook, final String locale, final String dateFormat) {
        final List<ClientCupoIncrementData> clientCupoIncrements = new ArrayList<>();
        final Sheet clientCupoIncrementSheet = workbook.getSheet(TemplatePopulateImportConstants.CLIENT_CUPO_INCREMENT_SHEET_NAME);
        final Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(clientCupoIncrementSheet,
                TemplatePopulateImportConstants.FIRST_COLUMN_INDEX);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = clientCupoIncrementSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, ClientCupoIncrementConstants.STATUS_COL)) {
                final ClientCupoIncrementData clientCupoIncrementData = readClientCupoIncrementData(row, locale, dateFormat);
                clientCupoIncrements.add(clientCupoIncrementData);
            }
        }
        return clientCupoIncrements;
    }

    private ClientCupoIncrementData readClientCupoIncrementData(final Row row, final String locale, final String dateFormat) {
        final String documentType = ImportHandlerUtils.readAsString(ClientCupoIncrementConstants.DOCUMENT_TYPE_COL, row);
        final String documentNumber = ImportHandlerUtils.readAsString(ClientCupoIncrementConstants.DOCUMENT_NUMBER_COL, row);
        final BigDecimal maximumCupoAmount = BigDecimal
                .valueOf(ImportHandlerUtils.readAsDouble(ClientCupoIncrementConstants.MAXIMUM_CUPO_AMOUNT_COL, row));
        return ClientCupoIncrementData.builder().documentNumber(documentNumber).documentType(documentType)
                .maximumCupoAmount(maximumCupoAmount).dateFormat(dateFormat).locale(locale).rowIndex(row.getRowNum()).build();
    }

    private Count importEntity(final Workbook workbook, final List<ClientCupoIncrementData> clientCupoIncrements, final String dateFormat,
            final String locale) {
        final Sheet clientCupoIncrementSheet = workbook.getSheet(TemplatePopulateImportConstants.CLIENT_CUPO_INCREMENT_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        String errorMessage;
        final GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat, locale));
        ImportHandlerUtils.writeString(ClientCupoIncrementConstants.STATUS_COL,
                clientCupoIncrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        ImportHandlerUtils.writeString(ClientCupoIncrementConstants.MODIFICATION_DATE,
                clientCupoIncrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "fecha de modificación");
        ImportHandlerUtils.writeString(ClientCupoIncrementConstants.CREATED_BY_USER_NAME_COL,
                clientCupoIncrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "usuario que modificó");
        ImportHandlerUtils.writeString(ClientCupoIncrementConstants.CLIENT_NAME_COL,
                clientCupoIncrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "Nombre del cliente");
        ImportHandlerUtils.writeString(ClientCupoIncrementConstants.PREVIOUS_MAXIMUM_CUPO_AMOUNT_COL,
                clientCupoIncrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "cupo anterior");

        clientCupoIncrementSheet.setColumnWidth(ClientCupoIncrementConstants.MODIFICATION_DATE,
                TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        clientCupoIncrementSheet.setColumnWidth(ClientCupoIncrementConstants.CREATED_BY_USER_NAME_COL,
                TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        clientCupoIncrementSheet.setColumnWidth(ClientCupoIncrementConstants.CLIENT_NAME_COL,
                TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        clientCupoIncrementSheet.setColumnWidth(ClientCupoIncrementConstants.PREVIOUS_MAXIMUM_CUPO_AMOUNT_COL,
                TemplatePopulateImportConstants.MEDIUM_COL_SIZE);

        final CellStyle headerStyle = headerStyle(workbook);
        clientCupoIncrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX).getCell(ClientCupoIncrementConstants.STATUS_COL)
                .setCellStyle(headerStyle);
        clientCupoIncrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX)
                .getCell(ClientCupoIncrementConstants.MODIFICATION_DATE).setCellStyle(headerStyle);
        clientCupoIncrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX)
                .getCell(ClientCupoIncrementConstants.CREATED_BY_USER_NAME_COL).setCellStyle(headerStyle);
        clientCupoIncrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX)
                .getCell(ClientCupoIncrementConstants.CLIENT_NAME_COL).setCellStyle(headerStyle);
        clientCupoIncrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX)
                .getCell(ClientCupoIncrementConstants.PREVIOUS_MAXIMUM_CUPO_AMOUNT_COL).setCellStyle(headerStyle);

        final LocalDate modificationDate = DateUtils.getBusinessLocalDate();
        final CellStyle dateCellStyle = workbook.createCellStyle();
        final short dataFormat = workbook.createDataFormat().getFormat(dateFormat);
        dateCellStyle.setDataFormat(dataFormat);

        final AppUser createdByUser = context.authenticatedUser();
        final String createdByUsername = createdByUser.getDisplayName();

        for (final ClientCupoIncrementData clientCupoIncrementData : clientCupoIncrements) {
            try {
                final String documentType = clientCupoIncrementData.getDocumentType();
                final String documentNumber = clientCupoIncrementData.getDocumentNumber();
                final BigDecimal maximumCupoAmount = clientCupoIncrementData.getMaximumCupoAmount();

                final Cell writeOffDateCell = clientCupoIncrementSheet.getRow(clientCupoIncrementData.getRowIndex())
                        .createCell(ClientCupoIncrementConstants.MODIFICATION_DATE);
                writeOffDateCell.setCellStyle(dateCellStyle);
                writeOffDateCell.setCellValue(modificationDate);
                clientCupoIncrementSheet.getRow(clientCupoIncrementData.getRowIndex())
                        .createCell(ClientCupoIncrementConstants.CREATED_BY_USER_NAME_COL).setCellValue(createdByUsername);

                final Collection<CodeValueData> documentTypes = codeValueReadPlatformService.retrieveCodeValuesByCode("Tipo ID");
                final CodeValueData codeValueData = documentTypes.stream()
                        .filter(docType -> docType.getName().equalsIgnoreCase(documentType)).findFirst().orElseThrow();
                final List<ClientAdditionalFieldsData> clients = clientReadPlatformService.retrieveAdditionalFieldsData(codeValueData,
                        documentNumber);

                if (CollectionUtils.isEmpty(clients)) {
                    errorCount++;
                    errorMessage = "La combinación de Documento y tipo son inválidas";
                    ImportHandlerUtils.writeErrorMessage(clientCupoIncrementSheet, clientCupoIncrementData.getRowIndex(), errorMessage,
                            ClientCupoIncrementConstants.STATUS_COL);
                    clientCupoIncrementSheet.setColumnWidth(ClientCupoIncrementConstants.STATUS_COL,
                            TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
                    continue;
                }
                final ClientAdditionalFieldsData client = clients.get(0);
                final Long clientId = client.getClientId();
                final String clientName = client.getClientName();
                final EnumOptionData clientStatus = client.getStatus();
                final BigDecimal previousMaximumCupoAmount = client.getCupo();
                clientCupoIncrementSheet.getRow(clientCupoIncrementData.getRowIndex())
                        .createCell(ClientCupoIncrementConstants.CLIENT_NAME_COL).setCellValue(clientName);
                clientCupoIncrementSheet.getRow(clientCupoIncrementData.getRowIndex())
                        .createCell(ClientCupoIncrementConstants.PREVIOUS_MAXIMUM_CUPO_AMOUNT_COL)
                        .setCellValue(previousMaximumCupoAmount.doubleValue());
                if (maximumCupoAmount.compareTo(previousMaximumCupoAmount) < 0) {
                    errorCount++;
                    errorMessage = "No puede modificarse ya que el nuevo cupo que sugiere es menor al actual";
                    ImportHandlerUtils.writeErrorMessage(clientCupoIncrementSheet, clientCupoIncrementData.getRowIndex(), errorMessage,
                            ClientCupoIncrementConstants.STATUS_COL);
                    clientCupoIncrementSheet.setColumnWidth(ClientCupoIncrementConstants.STATUS_COL,
                            TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
                    continue;
                }
                if (!ClientStatus.ACTIVE.getValue().equals(clientStatus.getId().intValue())) {
                    errorCount++;
                    errorMessage = "El cliente NO esta activo, no puede ejectuarse el cambio de cupo";
                    ImportHandlerUtils.writeErrorMessage(clientCupoIncrementSheet, clientCupoIncrementData.getRowIndex(), errorMessage,
                            ClientCupoIncrementConstants.STATUS_COL);
                    clientCupoIncrementSheet.setColumnWidth(ClientCupoIncrementConstants.STATUS_COL,
                            TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
                    continue;
                }
                String sql;
                if ("NIT".equalsIgnoreCase(documentType)) {
                    sql = "UPDATE campos_cliente_empresas SET \"Cupo\" = ? WHERE client_id = ? AND \"NIT\" = ?";
                } else {
                    sql = "UPDATE campos_cliente_persona SET \"Cupo aprobado\" = ? WHERE client_id = ? AND \"Cedula\" = ?";
                }
                final int affectedRows = jdbcTemplate.update(sql, maximumCupoAmount, clientId, documentNumber);
                if (affectedRows == 0) {
                    errorCount++;
                    errorMessage = "No se pudo modificar el cupo";
                    ImportHandlerUtils.writeErrorMessage(clientCupoIncrementSheet, clientCupoIncrementData.getRowIndex(), errorMessage,
                            ClientCupoIncrementConstants.STATUS_COL);
                    clientCupoIncrementSheet.setColumnWidth(ClientCupoIncrementConstants.STATUS_COL,
                            TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
                    continue;
                }
                final Cell statusCell = clientCupoIncrementSheet.getRow(clientCupoIncrementData.getRowIndex())
                        .createCell(ClientCupoIncrementConstants.STATUS_COL);
                statusCell.setCellValue("Modificado con éxito");
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
                clientCupoIncrementSheet.setColumnWidth(ClientCupoIncrementConstants.STATUS_COL,
                        TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
                successCount++;
            } catch (RuntimeException ex) {
                errorCount++;
                LOG.error("Problem occurred in importEntity function", ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(clientCupoIncrementSheet, clientCupoIncrementData.getRowIndex(), errorMessage,
                        ClientCupoIncrementConstants.STATUS_COL);
                clientCupoIncrementSheet.setColumnWidth(ClientCupoIncrementConstants.STATUS_COL,
                        TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
            }

        }
        return Count.instance(successCount, errorCount);
    }

    @Builder
    @Data
    private static class ClientCupoIncrementData {

        private String documentType;
        private String documentNumber;
        private BigDecimal maximumCupoAmount;
        private String locale;
        private String dateFormat;
        private Integer rowIndex;
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
