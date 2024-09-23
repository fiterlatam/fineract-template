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
package org.apache.fineract.infrastructure.bulkimport.importhandler.clientcupodecrement;

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
import org.apache.fineract.infrastructure.bulkimport.constants.ClientCupoDecrementConstants;
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
import org.apache.fineract.portfolio.client.data.ClientCupoTemporaryData;
import org.apache.fineract.portfolio.client.domain.ClientCupoTemporaryModification;
import org.apache.fineract.portfolio.client.domain.ClientCupoTemporaryModificationRateRepository;
import org.apache.fineract.portfolio.client.domain.ClientStatus;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformServiceImpl;
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
public class ClientCupoDecrementImportHandler implements ImportHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ClientCupoDecrementImportHandler.class);
    private final PlatformSecurityContext context;
    private final ClientReadPlatformService clientReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final JdbcTemplate jdbcTemplate;
    private final ClientCupoTemporaryModificationRateRepository clientCupoTemporaryModificationRateRepository;

    @Autowired
    public ClientCupoDecrementImportHandler(final PlatformSecurityContext context,
            final ClientReadPlatformService clientReadPlatformService, final CodeValueReadPlatformService codeValueReadPlatformService,
            final JdbcTemplate jdbcTemplate,
            final ClientCupoTemporaryModificationRateRepository clientCupoTemporaryModificationRateRepository) {
        this.context = context;
        this.clientReadPlatformService = clientReadPlatformService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.jdbcTemplate = jdbcTemplate;
        this.clientCupoTemporaryModificationRateRepository = clientCupoTemporaryModificationRateRepository;
    }

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat,
            final Map<String, Object> importAttributes) {
        final List<ClientCupoDecrementData> clientCupoDecrements = readExcelFile(workbook, locale, dateFormat);
        return importEntity(workbook, clientCupoDecrements, dateFormat, locale);
    }

    private List<ClientCupoDecrementData> readExcelFile(final Workbook workbook, final String locale, final String dateFormat) {
        final List<ClientCupoDecrementData> clientCupoDecrements = new ArrayList<>();
        final Sheet clientCupoDecrementSheet = workbook.getSheet(TemplatePopulateImportConstants.CLIENT_CUPO_DECREMENT_SHEET_NAME);
        final Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(clientCupoDecrementSheet,
                TemplatePopulateImportConstants.FIRST_COLUMN_INDEX);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = clientCupoDecrementSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, ClientCupoDecrementConstants.STATUS_COL)) {
                final ClientCupoDecrementData clientCupoDecrementData = readClientCupoDecrementData(row, locale, dateFormat);
                clientCupoDecrements.add(clientCupoDecrementData);
            }
        }
        return clientCupoDecrements;
    }

    private ClientCupoDecrementData readClientCupoDecrementData(final Row row, final String locale, final String dateFormat) {
        final String documentType = ImportHandlerUtils.readAsString(ClientCupoDecrementConstants.DOCUMENT_TYPE_COL, row);
        final String documentNumber = ImportHandlerUtils.readAsString(ClientCupoDecrementConstants.DOCUMENT_NUMBER_COL, row);
        final Double maximumCupoAmountDouble = ImportHandlerUtils.readAsDouble(ClientCupoDecrementConstants.MAXIMUM_CUPO_AMOUNT_COL, row);
        BigDecimal maximumCupoAmount = BigDecimal.valueOf(maximumCupoAmountDouble);
        String max = maximumCupoAmount.toPlainString();
        maximumCupoAmount = new BigDecimal(max);
        final LocalDate startOnDate = ImportHandlerUtils.readAsDate(ClientCupoDecrementConstants.START_ON_DATE_COL, row);
        final LocalDate endOnDate = ImportHandlerUtils.readAsDate(ClientCupoDecrementConstants.END_ON_DATE_COL, row);
        return ClientCupoDecrementData.builder().documentNumber(documentNumber).documentType(documentType)
                .maximumCupoAmount(maximumCupoAmount).startOnDate(startOnDate).endOnDate(endOnDate).dateFormat(dateFormat).locale(locale)
                .rowIndex(row.getRowNum()).build();
    }

    private Count importEntity(final Workbook workbook, final List<ClientCupoDecrementData> clientCupoDecrements, final String dateFormat,
            final String locale) {
        final Sheet clientCupDecrementSheet = workbook.getSheet(TemplatePopulateImportConstants.CLIENT_CUPO_DECREMENT_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        String errorMessage;
        final GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat, locale));
        ImportHandlerUtils.writeString(ClientCupoDecrementConstants.STATUS_COL,
                clientCupDecrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        ImportHandlerUtils.writeString(ClientCupoDecrementConstants.MODIFICATION_DATE,
                clientCupDecrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "fecha de modificación");
        ImportHandlerUtils.writeString(ClientCupoDecrementConstants.CREATED_BY_USER_NAME_COL,
                clientCupDecrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "usuario que modificó");
        ImportHandlerUtils.writeString(ClientCupoDecrementConstants.CLIENT_NAME_COL,
                clientCupDecrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "Nombre del cliente");
        ImportHandlerUtils.writeString(ClientCupoDecrementConstants.PREVIOUS_MAXIMUM_CUPO_AMOUNT_COL,
                clientCupDecrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), "cupo anterior");

        clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.MODIFICATION_DATE,
                TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.CREATED_BY_USER_NAME_COL,
                TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.CLIENT_NAME_COL,
                TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.PREVIOUS_MAXIMUM_CUPO_AMOUNT_COL,
                TemplatePopulateImportConstants.MEDIUM_COL_SIZE);

        final CellStyle headerStyle = headerStyle(workbook);
        clientCupDecrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX).getCell(ClientCupoDecrementConstants.STATUS_COL)
                .setCellStyle(headerStyle);
        clientCupDecrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX)
                .getCell(ClientCupoDecrementConstants.MODIFICATION_DATE).setCellStyle(headerStyle);
        clientCupDecrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX)
                .getCell(ClientCupoDecrementConstants.CREATED_BY_USER_NAME_COL).setCellStyle(headerStyle);
        clientCupDecrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX)
                .getCell(ClientCupoDecrementConstants.CLIENT_NAME_COL).setCellStyle(headerStyle);
        clientCupDecrementSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX)
                .getCell(ClientCupoDecrementConstants.PREVIOUS_MAXIMUM_CUPO_AMOUNT_COL).setCellStyle(headerStyle);

        final LocalDate modificationDate = DateUtils.getBusinessLocalDate();
        final CellStyle dateCellStyle = workbook.createCellStyle();
        final short dataFormat = workbook.createDataFormat().getFormat(dateFormat);
        dateCellStyle.setDataFormat(dataFormat);

        final AppUser createdByUser = context.authenticatedUser();
        final String createdByUsername = createdByUser.getDisplayName();

        for (final ClientCupoDecrementData clientCupoDecrementData : clientCupoDecrements) {
            try {
                final String documentType = clientCupoDecrementData.getDocumentType();
                final String documentNumber = clientCupoDecrementData.getDocumentNumber();
                final BigDecimal maximumCupoAmount = clientCupoDecrementData.getMaximumCupoAmount();
                final LocalDate startOnDate = clientCupoDecrementData.getStartOnDate();
                final LocalDate endOnDate = clientCupoDecrementData.getEndOnDate();

                final Cell writeOffDateCell = clientCupDecrementSheet.getRow(clientCupoDecrementData.getRowIndex())
                        .createCell(ClientCupoDecrementConstants.MODIFICATION_DATE);
                writeOffDateCell.setCellStyle(dateCellStyle);
                writeOffDateCell.setCellValue(modificationDate);
                clientCupDecrementSheet.getRow(clientCupoDecrementData.getRowIndex())
                        .createCell(ClientCupoDecrementConstants.CREATED_BY_USER_NAME_COL).setCellValue(createdByUsername);
                final Collection<CodeValueData> documentTypes = codeValueReadPlatformService.retrieveCodeValuesByCode("Tipo ID");
                final CodeValueData codeValueData = documentTypes.stream()
                        .filter(docType -> docType.getName().equalsIgnoreCase(documentType)).findFirst().orElseThrow();
                final List<ClientAdditionalFieldsData> clients = clientReadPlatformService.retrieveAdditionalFieldsData(codeValueData,
                        documentNumber);
                if (CollectionUtils.isEmpty(clients)) {
                    errorCount++;
                    errorMessage = "La combinación de Documento y tipo son inválidas";
                    ImportHandlerUtils.writeErrorMessage(clientCupDecrementSheet, clientCupoDecrementData.getRowIndex(), errorMessage,
                            ClientCupoDecrementConstants.STATUS_COL);
                    clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.STATUS_COL,
                            TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
                    continue;
                }
                final ClientAdditionalFieldsData client = clients.get(0);
                final Long clientId = client.getClientId();
                final String clientName = client.getClientName();
                final EnumOptionData clientStatus = client.getStatus();
                final BigDecimal previousMaximumCupoAmount = client.getCupo();
                clientCupDecrementSheet.getRow(clientCupoDecrementData.getRowIndex())
                        .createCell(ClientCupoDecrementConstants.CLIENT_NAME_COL).setCellValue(clientName);
                clientCupDecrementSheet.getRow(clientCupoDecrementData.getRowIndex())
                        .createCell(ClientCupoDecrementConstants.PREVIOUS_MAXIMUM_CUPO_AMOUNT_COL)
                        .setCellValue(previousMaximumCupoAmount.doubleValue());
                if (maximumCupoAmount.compareTo(previousMaximumCupoAmount) > 0) {
                    errorCount++;
                    errorMessage = "No puede modificarse ya que el nuevo cupo que sugiere es mayor al actual";
                    ImportHandlerUtils.writeErrorMessage(clientCupDecrementSheet, clientCupoDecrementData.getRowIndex(), errorMessage,
                            ClientCupoDecrementConstants.STATUS_COL);
                    clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.STATUS_COL,
                            TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
                    continue;
                }
                if (!ClientStatus.ACTIVE.getValue().equals(clientStatus.getId().intValue())) {
                    errorCount++;
                    errorMessage = "El cliente NO esta activo, no puede ejectuarse el cambio de cupo";
                    ImportHandlerUtils.writeErrorMessage(clientCupDecrementSheet, clientCupoDecrementData.getRowIndex(), errorMessage,
                            ClientCupoDecrementConstants.STATUS_COL);
                    clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.STATUS_COL,
                            TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
                    continue;
                }
                if (startOnDate != null && endOnDate != null) {
                    if (DateUtils.isBeforeBusinessDate(startOnDate) || DateUtils.isBeforeBusinessDate(endOnDate)) {
                        errorCount++;
                        errorMessage = "La fecha de inicio o fin no puede ser menor a la fecha actual";
                        ImportHandlerUtils.writeErrorMessage(clientCupDecrementSheet, clientCupoDecrementData.getRowIndex(), errorMessage,
                                ClientCupoDecrementConstants.STATUS_COL);
                        clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.STATUS_COL,
                                TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
                        continue;
                    }
                    if (DateUtils.isAfter(startOnDate, endOnDate)) {
                        errorCount++;
                        errorMessage = "La fecha de inicio no puede ser mayor a la fecha de fin";
                        ImportHandlerUtils.writeErrorMessage(clientCupDecrementSheet, clientCupoDecrementData.getRowIndex(), errorMessage,
                                ClientCupoDecrementConstants.STATUS_COL);
                        clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.STATUS_COL,
                                TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
                        continue;
                    }
                    final List<ClientCupoTemporaryModification> modifications = this.clientCupoTemporaryModificationRateRepository
                            .findByClientIdAndDocumentTypeAndStartOnDateAndEndOnDate(clientId, documentType, startOnDate, endOnDate);
                    if (CollectionUtils.isNotEmpty(modifications)) {
                        modifications.forEach(modification -> {
                            if (modification.isIncrement()) {
                                modification.setCupoMaxAmount(maximumCupoAmount);
                            }
                        });
                        this.clientCupoTemporaryModificationRateRepository.saveAll(modifications);
                    } else {
                        final ClientReadPlatformServiceImpl.ClientTemporaryMapper clientTemporaryMapper = new ClientReadPlatformServiceImpl.ClientTemporaryMapper();
                        final String sql = "SELECT " + clientTemporaryMapper.schema()
                                + " WHERE mctm.client_id = ? AND mctm.document_type = ? AND ((mctm.start_date, mctm.end_date) OVERLAPS (?, ?)) ";
                        final List<ClientCupoTemporaryData> clientTemporaryDataList = jdbcTemplate.query(sql, clientTemporaryMapper,
                                clientId, documentType, startOnDate, endOnDate);
                        if (CollectionUtils.isNotEmpty(clientTemporaryDataList)) {
                            errorCount++;
                            errorMessage = "El rango de fecha se esta solapando con otro, Por favor revisar";
                            ImportHandlerUtils.writeErrorMessage(clientCupDecrementSheet, clientCupoDecrementData.getRowIndex(),
                                    errorMessage, ClientCupoDecrementConstants.STATUS_COL);
                            clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.STATUS_COL,
                                    TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
                            continue;
                        }
                        final boolean isTemporaryIncrement = false;
                        final ClientCupoTemporaryModification clientCupoTemporaryModification = ClientCupoTemporaryModification.createNew(
                                clientId, documentType, isTemporaryIncrement, maximumCupoAmount, previousMaximumCupoAmount, startOnDate,
                                endOnDate);
                        this.clientCupoTemporaryModificationRateRepository.saveAndFlush(clientCupoTemporaryModification);
                    }
                } else {
                    String sql;
                    if (client.isPerson()) {
                        sql = "UPDATE campos_cliente_persona SET \"Cupo aprobado\" = ? WHERE client_id = ? AND \"Cedula\" = ?";
                    } else {
                        sql = "UPDATE campos_cliente_empresas SET \"Cupo\" = ? WHERE client_id = ? AND \"NIT\" = ?";
                    }
                    final int affectedRows = jdbcTemplate.update(sql, maximumCupoAmount, clientId, documentNumber);
                    if (affectedRows == 0) {
                        errorCount++;
                        errorMessage = "No se pudo modificar el cupo";
                        ImportHandlerUtils.writeErrorMessage(clientCupDecrementSheet, clientCupoDecrementData.getRowIndex(), errorMessage,
                                ClientCupoDecrementConstants.STATUS_COL);
                        clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.STATUS_COL,
                                TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
                        continue;
                    }
                }
                final Cell statusCell = clientCupDecrementSheet.getRow(clientCupoDecrementData.getRowIndex())
                        .createCell(ClientCupoDecrementConstants.STATUS_COL);
                statusCell.setCellValue("Modificado con éxito");
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
                clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.STATUS_COL,
                        TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
                successCount++;
            } catch (RuntimeException ex) {
                errorCount++;
                LOG.error("Problem occurred in importEntity function", ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(clientCupDecrementSheet, clientCupoDecrementData.getRowIndex(), errorMessage,
                        ClientCupoDecrementConstants.STATUS_COL);
                clientCupDecrementSheet.setColumnWidth(ClientCupoDecrementConstants.STATUS_COL,
                        TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
            }

        }
        return Count.instance(successCount, errorCount);
    }

    @Builder
    @Data
    private static class ClientCupoDecrementData {

        private String documentType;
        private String documentNumber;
        private BigDecimal maximumCupoAmount;
        private LocalDate startOnDate;
        private LocalDate endOnDate;
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
