package org.apache.fineract.infrastructure.bulkimport.importhandler.saleofinsuranceorassistance;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.infrastructure.channel.domain.Channel;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelRepository;
import org.apache.fineract.custom.portfolio.buyprocess.data.ClientBuyProcessData;
import org.apache.fineract.infrastructure.bulkimport.constants.GuarantorConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.SaleOfInsuranceOrAssistanceConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SaleOfInsuranceOrAssistanceImportHandler implements ImportHandler {

    public static final String EMPTY_STR = "";
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ChannelRepository channelRepository;

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat,
            final Map<String, Object> importAttributes) {
        List<ClientBuyProcessData> saleOfInsuranceData = readExcelFile(workbook, locale, dateFormat);
        return importEntity(workbook, saleOfInsuranceData, dateFormat);
    }

    private List<ClientBuyProcessData> readExcelFile(final Workbook workbook, final String locale, final String dateFormat) {
        List<ClientBuyProcessData> saleOfInsuranceData = new ArrayList<>();
        Sheet saleOfInsuranceSheet = workbook.getSheet(TemplatePopulateImportConstants.SALE_OF_INSURANCE_OR_ASSISTANCE_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(saleOfInsuranceSheet,
                SaleOfInsuranceOrAssistanceConstants.CUSTOMER_ID_COL);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = saleOfInsuranceSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, SaleOfInsuranceOrAssistanceConstants.STATUS_COL)) {
                saleOfInsuranceData.add(readInsuranceSaleData(workbook, row, locale, dateFormat));
            }
        }
        return saleOfInsuranceData;
    }

    private ClientBuyProcessData readInsuranceSaleData(final Workbook workbook, final Row row, final String locale,
            final String dateFormat) {
        String clientId = ImportHandlerUtils.readAsString(SaleOfInsuranceOrAssistanceConstants.CUSTOMER_ID_COL, row);
        String productName = ImportHandlerUtils.readAsString(SaleOfInsuranceOrAssistanceConstants.INSURANCE_PRODUCT_COL, row);
        Long productId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.PRODUCT_SHEET_NAME), productName);
        Long term = ImportHandlerUtils.readAsLong(SaleOfInsuranceOrAssistanceConstants.TERM_COL, row);
        Long insuranceCode = ImportHandlerUtils.readAsLong(SaleOfInsuranceOrAssistanceConstants.INSURANCE_CODE_COL, row);
        Long advisorId = ImportHandlerUtils.readAsLong(SaleOfInsuranceOrAssistanceConstants.ADVISOR_ID_COL, row);
        String channelName = ImportHandlerUtils.readAsString(SaleOfInsuranceOrAssistanceConstants.SALES_CHANNEL_COL, row);
        String channelHash = "";
        Optional<Channel> optionalChannel = this.channelRepository.findByNameIgnoreCase(channelName);
        if (optionalChannel.isPresent()) {
            Channel channel = optionalChannel.get();
            channelHash = channel.getHash();
        }

        ClientBuyProcessData data = new ClientBuyProcessData();
        data.setClientDocumentId(clientId);
        data.setProductId(productId);
        data.setTerm(term);
        data.setCedulaSeguroVoluntario(advisorId);
        data.setCodigoSeguro(insuranceCode);
        data.setRequestedDate(LocalDate.now());
        data.setChannelHash(channelHash);
        data.setRowIndex(row.getRowNum());
        data.setSaleOfInsuranceOrAssistance(true);
        data.setDateFormat(dateFormat);
        data.setLocale(locale);

        return data;
    }

    private Count importEntity(final Workbook workbook, final List<ClientBuyProcessData> salesList, final String dateFormat) {
        Sheet saleOfInsuranceSheet = workbook.getSheet(TemplatePopulateImportConstants.SALE_OF_INSURANCE_OR_ASSISTANCE_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        String errorMessage = "";
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        for (ClientBuyProcessData salesData : salesList) {
            try {
                JsonObject guarantorJsonob = gsonBuilder.create().toJsonTree(salesData).getAsJsonObject();
                guarantorJsonob.remove("status");
                String payload = guarantorJsonob.toString();
                final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                        .createClientBuyProcess() //
                        .withJson(payload) //
                        .build(); //
                commandsSourceWritePlatformService.logCommandSource(commandRequest);
                successCount++;
                Cell statusCell = saleOfInsuranceSheet.getRow(salesData.getRowIndex()).createCell(GuarantorConstants.STATUS_COL);
                statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
            } catch (RuntimeException ex) {
                errorCount++;
                log.error("Problem occurred in importEntity function", ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(saleOfInsuranceSheet, salesData.getRowIndex(), errorMessage,
                        SaleOfInsuranceOrAssistanceConstants.STATUS_COL);
            }

        }
        saleOfInsuranceSheet.setColumnWidth(SaleOfInsuranceOrAssistanceConstants.STATUS_COL,
                TemplatePopulateImportConstants.SMALL_COL_SIZE);
        ImportHandlerUtils.writeString(SaleOfInsuranceOrAssistanceConstants.STATUS_COL,
                saleOfInsuranceSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        return Count.instance(successCount, errorCount);
    }
}
