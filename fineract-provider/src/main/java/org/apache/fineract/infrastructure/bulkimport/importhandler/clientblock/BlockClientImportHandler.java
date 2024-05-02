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
package org.apache.fineract.infrastructure.bulkimport.importhandler.clientblock;

import com.google.api.client.util.ArrayMap;
import com.google.gson.GsonBuilder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.BlockClientConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.ClientPersonConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.ManageBlockingReasonSettingsRepositoryWrapper;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.client.data.ClientAdditionalFieldsData;
import org.apache.fineract.portfolio.client.data.ClientBlockingListData;
import org.apache.fineract.portfolio.client.domain.ClientBlockList;
import org.apache.fineract.portfolio.client.domain.ClientBlockListRepository;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class BlockClientImportHandler implements ImportHandler {

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final ClientReadPlatformService clientReadPlatformService;
    private final ManageBlockingReasonSettingsRepositoryWrapper blockingReasonSettingsRepositoryWrapper;
    private final ClientBlockListRepository clientBlockListRepository;

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat) {

        List<ClientBlockingListData> clientBlockingListData = readExcelFile(workbook, locale, dateFormat);
        return importEntity(workbook, clientBlockingListData, dateFormat, locale);
    }

    public List<ClientBlockingListData> readExcelFile(final Workbook workbook, final String locale, final String dateFormat) {
        List<ClientBlockingListData> clients = new ArrayList<>();
        Sheet clientBlockSheet = workbook.getSheet(TemplatePopulateImportConstants.BLOCK_CLIENT_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(clientBlockSheet, 0);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = clientBlockSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, ClientPersonConstants.STATUS_COL)) {
                clients.add(readClient(workbook, row, locale, dateFormat));
            }
        }
        return clients;
    }

    private ClientBlockingListData readClient(final Workbook workbook, final Row row, final String locale, final String dateFormat) {

        BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepositoryWrapper
                .getBlockingReasonSettingByReason("LISTAS DE CONTROL", "CLIENT").stream().findFirst().orElseThrow();
        String blockedOnDate = DateUtils.format(DateUtils.getLocalDateOfTenant(), dateFormat);
        String idType = ImportHandlerUtils.readAsString(BlockClientConstants.ID_TYPE_COL, row);
        String idNumber = ImportHandlerUtils.readAsString(BlockClientConstants.ID_NUMBER_COL, row);
        String firstName = ImportHandlerUtils.readAsString(BlockClientConstants.FIRST_NAME_COL, row);
        String secondName = ImportHandlerUtils.readAsString(BlockClientConstants.SECOND_NAME_COL, row);
        String surname = ImportHandlerUtils.readAsString(BlockClientConstants.SURNAME_COL, row);
        String secondSurname = ImportHandlerUtils.readAsString(BlockClientConstants.SECOND_SURNAME_COL, row);
        String companyName = ImportHandlerUtils.readAsString(BlockClientConstants.COMPANY_NAME_COL, row);
        String causal = ImportHandlerUtils.readAsString(BlockClientConstants.CAUSAL_COL, row);
        String observation = ImportHandlerUtils.readAsString(BlockClientConstants.OBSERVATION_COL, row);

        if (Strings.isEmpty(idType) || Strings.isEmpty(idNumber)) {
            throw new IllegalArgumentException("idType and idNumber cannot be null");
        }

        if (idType.equalsIgnoreCase("NIT") && Strings.isEmpty(companyName)) {
            throw new IllegalArgumentException("companyName cannot be null");
        }

        return ClientBlockingListData.builder().idType(idType).idNumber(idNumber).firstName(firstName).secondName(secondName)
                .surname(surname).secondSurname(secondSurname).companyName(companyName).causal(causal).blockingComment(observation)
                .blockingReasonId(blockingReasonSetting.getId()).blockedOnDate(blockedOnDate).dateFormat(dateFormat).locale(locale)
                .rowIndex(row.getRowNum()).build();
    }

    public Count importEntity(final Workbook workbook, final List<ClientBlockingListData> clientBlockListData, final String dateFormat,
            final String locale) {
        final Sheet blockClientSheet = workbook.getSheet(TemplatePopulateImportConstants.BLOCK_CLIENT_SHEET_NAME);
        int successCount;
        int errorCount = 0;
        String errorMessage;
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        Collection<CodeValueData> idTypes = codeValueReadPlatformService.retrieveCodeValuesByCode("Tipo ID");

        final List<ClientBlockList> currentClientList = clientBlockListRepository.findAll();
        final List<ClientBlockList> toDelete = new ArrayList<>();
        final List<ClientBlockingListData> toAdd = new ArrayList<>();

        updateRemovedClientList(currentClientList, clientBlockListData, toDelete);
        successCount = updateNewClientList(toAdd, currentClientList, clientBlockListData, blockClientSheet, workbook);

        for (ClientBlockingListData client : toAdd) {
            try {

                String payload = gsonBuilder.create().toJson(client);

                CodeValueData data = idTypes.stream().filter(idType -> idType.getName().equalsIgnoreCase(client.idType())).findFirst()
                        .orElseThrow();
                List<ClientAdditionalFieldsData> additionalFieldsData = clientReadPlatformService.retrieveAdditionalFieldsData(data,
                        client.idNumber());

                final Long clientId = additionalFieldsData.getFirst().getClientId();

                final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                        .blockClient(clientId) //
                        .withJson(payload) //
                        .build(); //
                final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

                if (!result.isRollbackTransaction() && result.getResourceId() > 0) {
                    addClientToBlockList(client, clientId);
                }

                successCount++;
                Cell statusCell = blockClientSheet.getRow(client.rowIndex()).createCell(BlockClientConstants.STATUS_COL);
                statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
            } catch (RuntimeException ex) {
                errorCount++;
                log.error("Problem occurred in importEntity function", ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(blockClientSheet, client.rowIndex(), errorMessage, ClientPersonConstants.STATUS_COL);
            }
        }

        blockClientSheet.setColumnWidth(ClientPersonConstants.STATUS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        ImportHandlerUtils.writeString(ClientPersonConstants.STATUS_COL,
                blockClientSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COLUMN_HEADER);

        unBlockClientsInBlockTableAndNotonNewList(toDelete, dateFormat, locale, gsonBuilder);

        return Count.instance(successCount, errorCount);
    }

    private int updateNewClientList(List<ClientBlockingListData> toAdd, List<ClientBlockList> currentClientList,
            List<ClientBlockingListData> clientBlockListData, Sheet blockClientSheet, Workbook workbook) {
        int successCount = 0;
        for (ClientBlockingListData clientData : clientBlockListData) {
            boolean add = true;
            for (ClientBlockList client : currentClientList) {
                if (client.getIdType().equalsIgnoreCase(clientData.idType())
                        && client.getIdNumber().equalsIgnoreCase(clientData.idNumber())) {
                    add = false;
                    successCount++;
                    Cell statusCell = blockClientSheet.getRow(clientData.rowIndex()).createCell(BlockClientConstants.STATUS_COL);
                    statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                    statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
                }
            }

            if (add) {
                toAdd.add(clientData);
            }
        }
        return successCount;
    }

    private void updateRemovedClientList(List<ClientBlockList> currentClientList, List<ClientBlockingListData> clientBlockListData,
            List<ClientBlockList> toDelete) {
        for (ClientBlockList client : currentClientList) {
            boolean delete = true;
            for (ClientBlockingListData clientData : clientBlockListData) {
                if (client.getIdType().equalsIgnoreCase(clientData.idType())
                        && client.getIdNumber().equalsIgnoreCase(clientData.idNumber())) {
                    delete = false;
                }
            }

            if (delete) {
                toDelete.add(client);
            }
        }
    }

    private void addClientToBlockList(final ClientBlockingListData client, final Long clientId) {
        clientBlockListRepository.save(ClientBlockList.builder().clientId(clientId).idType(client.idType()).idNumber(client.idNumber())
                .firstName(client.firstName()).secondName(client.secondName()).surname(client.surname())
                .secondSurname(client.secondSurname()).companyName(client.companyName()).causal(client.causal())
                .observation(client.blockingComment()).build());
    }

    private void unBlockClientsInBlockTableAndNotonNewList(final List<ClientBlockList> toDelete, final String dateFormat,
            final String locale, final GsonBuilder gsonBuilder) {

        Map<String, String> payloadMap = new ArrayMap<>();
        payloadMap.put("undoBlockedOnDate", DateUtils.format(DateUtils.getLocalDateOfTenant(), dateFormat));
        payloadMap.put("undoBlockingComment", "Desbloqueo por importación de lista de control");
        payloadMap.put("dateFormat", dateFormat);
        payloadMap.put("locale", locale);

        final String payload = gsonBuilder.create().toJson(payloadMap);

        for (ClientBlockList client : toDelete) {
            try {

                final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                        .undoBlockClient(client.getClientId())//
                        .withJson(payload) //
                        .build(); //
                final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

            } catch (RuntimeException ex) {
                log.error("Problem deleting user from block list {1}", client.getIdNumber(), ex);
            }
        }
    }

}
