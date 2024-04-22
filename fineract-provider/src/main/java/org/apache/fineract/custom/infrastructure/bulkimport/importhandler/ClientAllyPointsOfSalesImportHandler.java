package org.apache.fineract.custom.infrastructure.bulkimport.importhandler;

import com.google.gson.GsonBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.infrastructure.bulkimport.data.CustomGlobalEntityType;
import org.apache.fineract.custom.infrastructure.bulkimport.enumerator.ClientAllyPointOfSalesTemplatePopulateImportEnum;
import org.apache.fineract.custom.infrastructure.bulkimport.enumerator.ClientAllyTemplatePopulateImportEnum;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyPointOfSalesData;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.tika.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ClientAllyPointsOfSalesImportHandler implements ImportHandler {

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private String dateFormat = "dd MMMM yyyy";
    private String locale = "en";

    @Autowired
    public ClientAllyPointsOfSalesImportHandler(final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat) {
        this.dateFormat = dateFormat;
        this.locale = locale;
        List<ClientAllyPointOfSalesData> staffList = readExcelFile(workbook, locale, dateFormat);
        return importEntity(workbook, staffList, dateFormat);
    }

    private List<ClientAllyPointOfSalesData> readExcelFile(final Workbook workbook, final String locale, final String dateFormat) {
        List<ClientAllyPointOfSalesData> staffList = new ArrayList<>();
        Sheet staffSheet = workbook.getSheet(CustomGlobalEntityType.CLIENT_ALLY_POINTS_OF_SALES.getCode().replaceAll(" ", "_"));
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(staffSheet, TemplatePopulateImportConstants.FIRST_COLUMN_INDEX);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = staffSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row,
                    ClientAllyPointOfSalesTemplatePopulateImportEnum.IMPORT_STATUS_COLUMN.getColumnIndex())) {
                staffList.add(readExcelRow(workbook, row, locale, dateFormat));
            }
        }
        return staffList;
    }

    private ClientAllyPointOfSalesData readExcelRow(final Workbook workbook, final Row row, final String locale, final String dateFormat) {

        ClientAllyPointOfSalesData ret = ClientAllyPointOfSalesData.builder().build();
        StringBuilder ammendedValidationMessages = new StringBuilder();

        // Validate values based on enum fields
        for (ClientAllyPointOfSalesTemplatePopulateImportEnum currEnum : ClientAllyPointOfSalesTemplatePopulateImportEnum.getOrdered()) {
            String cellValueAsString = ImportHandlerUtils.readAsString(currEnum.getColumnIndex(), row);
            Object clazz = currEnum.getClazz();

            validateTypeAndCasting(ammendedValidationMessages, currEnum, cellValueAsString, clazz);
        }

        executeCustomValidation(row, ammendedValidationMessages);

        if (StringUtils.EMPTY.equalsIgnoreCase(ammendedValidationMessages.toString())) {

            String settledComissionAsString = getSettledComissionAsString(row);

            ret = ClientAllyPointOfSalesData.builder()
                    .clientAllyId(readCodeValueIdFromName(workbook, row, ClientAllyPointOfSalesTemplatePopulateImportEnum.COMPANY_NAME_ID, //
                            CustomGlobalEntityType.CLIENT_ALLY.getCode())) //
                    .name(ImportHandlerUtils.readAsString(ClientAllyPointOfSalesTemplatePopulateImportEnum.NAME.getColumnIndex(), row)) //
                    .code(ImportHandlerUtils.readAsString(ClientAllyPointOfSalesTemplatePopulateImportEnum.CODE.getColumnIndex(), row)) //
                    .brandCodeValueId(readCodeValueIdFromName(workbook, row, ClientAllyPointOfSalesTemplatePopulateImportEnum.BRAND_ID)) //
                    .departmentCodeValueId(
                            readCodeValueIdFromName(workbook, row, ClientAllyPointOfSalesTemplatePopulateImportEnum.DEPARTMENT_ID)) //
                    .cityCodeValueId(readCodeValueIdFromName(workbook, row, ClientAllyPointOfSalesTemplatePopulateImportEnum.CITY_ID)) //
                    .categoryCodeValueId(
                            readCodeValueIdFromName(workbook, row, ClientAllyPointOfSalesTemplatePopulateImportEnum.CATEGORY_ID)) //
                    .segmentCodeValueId(readCodeValueIdFromName(workbook, row, ClientAllyPointOfSalesTemplatePopulateImportEnum.SEGMENT_ID)) //
                    .typeCodeValueId(readCodeValueIdFromName(workbook, row, ClientAllyPointOfSalesTemplatePopulateImportEnum.TYPE_ID)) //
                    .settledComission(new BigDecimal(settledComissionAsString)) //
                    .buyEnabled(ImportHandlerUtils
                            .readAsBoolean(ClientAllyPointOfSalesTemplatePopulateImportEnum.BUY_ENABLED.getColumnIndex(), row)) //
                    .collectionEnabled(ImportHandlerUtils
                            .readAsBoolean(ClientAllyPointOfSalesTemplatePopulateImportEnum.COLLECTION_ENABLED.getColumnIndex(), row)) //
                    .stateCodeValueId(readCodeValueIdFromName(workbook, row, ClientAllyPointOfSalesTemplatePopulateImportEnum.STATE_ID)) //
                    .build();
        }

        if (Boolean.FALSE.equals(StringUtils.EMPTY.equals(ammendedValidationMessages.toString()))) {
            ret.setValidationErrorMessage(ammendedValidationMessages.toString());
        } else {
            ret.setValidationErrorMessage(null);
        }

        ret.setRowIndex(row.getRowNum());
        ret.setLocale(locale);
        ret.setDateFormat(dateFormat);

        return ret;
    }

    private void executeCustomValidation(Row row, StringBuilder ammenedEValidationMessages) {

        // Check departments and cities
        String selectedDepartment = ImportHandlerUtils
                .readAsString(ClientAllyPointOfSalesTemplatePopulateImportEnum.DEPARTMENT_ID.getColumnIndex(), row);
        String selectedCity = ImportHandlerUtils.readAsString(ClientAllyPointOfSalesTemplatePopulateImportEnum.CITY_ID.getColumnIndex(),
                row);
        if (Objects.nonNull(selectedDepartment) && Objects.nonNull(selectedCity)) {
            if (Boolean.FALSE.equals(selectedCity.contains(selectedDepartment))) {
                ammenedEValidationMessages.append("La " + ClientAllyPointOfSalesTemplatePopulateImportEnum.CITY_ID.getColumnName()
                        + " seleccionada no corresponde al "
                        + ClientAllyPointOfSalesTemplatePopulateImportEnum.DEPARTMENT_ID.getColumnName() + " seleccionado; ");
            }
        }

        BigDecimal settledComission = new BigDecimal(getSettledComissionAsString(row));
        if (Objects.nonNull(settledComission) && settledComission.compareTo(BigDecimal.valueOf(99.99)) > 0) {
            ammenedEValidationMessages.append(
                    "El campo " + ClientAllyTemplatePopulateImportEnum.SETTLED_COMISSION.getColumnName() + " no debe ser que 99.99; ");
        }
    }

    @NotNull
    private String getSettledComissionAsString(Row row) {
        String settledComissionAsString = ImportHandlerUtils.readAsString(
                ClientAllyPointOfSalesTemplatePopulateImportEnum.SETTLED_COMISSION.getColumnIndex(), row);
        return fixUpDecimalSeparator(settledComissionAsString);
    }

    @NotNull
    private String fixUpDecimalSeparator(String settledComissionAsString) {
        return settledComissionAsString.replace(",", ".");
    }


    private void validateTypeAndCasting(StringBuilder ammendedValidationMessages, ClientAllyPointOfSalesTemplatePopulateImportEnum currEnum,
            String representation, Object clazz) {

        if (currEnum.equals(ClientAllyPointOfSalesTemplatePopulateImportEnum.IMPORT_STATUS_COLUMN)) {
            return;
        }

        // Check Mandatory
        if (currEnum.getMandatory() && Objects.isNull(representation) || StringUtils.EMPTY.equalsIgnoreCase(representation)) {
            ammendedValidationMessages.append(currEnum.getColumnName()).append(" es obligatorio; ");
        }

        if (currEnum.name().toUpperCase().endsWith("_ID")) {
            return;
        }

        if (Objects.nonNull(representation) && Objects.isNull(currEnum.getCodeValueName())) {
            if (clazz.equals(String.class)) {
                if (Objects.nonNull(currEnum.getMaxLength())
                        && currEnum.getMaxLength().compareTo(Long.valueOf(representation.length())) < 0) {
                    ammendedValidationMessages.append(currEnum.getColumnName()).append(" tiene una longitud máxima de ")
                            .append(currEnum.getMaxLength()).append("; ");
                }
            } else if (clazz.equals(Integer.class)) {
                try {
                    Integer.valueOf(representation);
                } catch (NumberFormatException e) {
                    ammendedValidationMessages.append(currEnum.getColumnName()).append(" debe ser un número entero; ");
                }
            } else if (clazz.equals(Boolean.class)) {
                try {
                    Boolean.valueOf(representation);
                } catch (NumberFormatException e) {
                    ammendedValidationMessages.append(currEnum.getColumnName()).append(" debe ser un valor booleano (True/False); ");
                }
            } else if (clazz.equals(BigDecimal.class)) {
                try {
                    new BigDecimal(representation);
                } catch (NumberFormatException e) {
                    ammendedValidationMessages.append(currEnum.getColumnName()).append(" debe ser un valor BigDecimal (ex 138.6547); ");
                }

            } else if (clazz.equals(Long.class)) {
                try {
                    Long.valueOf(representation);
                } catch (NumberFormatException e) {
                    ammendedValidationMessages.append(currEnum.getColumnName()).append(" debe ser un valor Long; ");
                }

            } else if (clazz.equals(Double.class)) {
                try {
                    Double.valueOf(representation);
                } catch (NumberFormatException e) {
                    try {
                        Double.valueOf(fixUpDecimalSeparator(representation));
                    } catch (NumberFormatException ex) {
                        ammendedValidationMessages.append(currEnum.getColumnName()).append(" debe ser un valor Double (ex 138.6547). ");
                    }
                }
            } else {
                // TODO Not supported
            }
        }
    }

    private Long readCodeValueIdFromName(final Workbook workbook, final Row row, ClientAllyPointOfSalesTemplatePopulateImportEnum codeEnum,
            String customListSheetName) {
        String cellValue = ImportHandlerUtils.readAsString(codeEnum.getColumnIndex(), row);

        return getIdByName(workbook.getSheet(customListSheetName), cellValue);
    }

    private Long readCodeValueIdFromName(final Workbook workbook, final Row row,
            ClientAllyPointOfSalesTemplatePopulateImportEnum codeEnum) {
        String cellValue = ImportHandlerUtils.readAsString(codeEnum.getColumnIndex(), row);

        return getIdByName(workbook.getSheet(codeEnum.getCodeValueName()), cellValue);
    }

    public static Long getIdByName(Sheet sheet, String name) {

        if (Objects.isNull(name) || StringUtils.EMPTY.equals(name)) {
            return null;
        }

        for (Row row : sheet) {

            Cell cellId = row.getCell(0);
            String cellValue = String.valueOf(row.getCell(1));

            if (name.equalsIgnoreCase(cellValue)) {
                return (long) cellId.getNumericCellValue();
            }
        }

        return 0L;
    }

    private Count importEntity(final Workbook workbook, final List<ClientAllyPointOfSalesData> staffList, final String dateFormat) {
        Sheet staffSheet = workbook.getSheet(CustomGlobalEntityType.CLIENT_ALLY_POINTS_OF_SALES.getCode());
        int successCount = 0;
        int errorCount = 0;
        String errorMessage;
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));

        for (ClientAllyPointOfSalesData staff : staffList) {
            // Check if there is any validation error in client data
            if (Objects.nonNull(staff.getValidationErrorMessage())
                    && Boolean.FALSE.equals(StringUtils.EMPTY.equalsIgnoreCase(staff.getValidationErrorMessage()))) {

                ImportHandlerUtils.writeErrorMessage(staffSheet, staff.getRowIndex(), staff.getValidationErrorMessage(),
                        ClientAllyPointOfSalesTemplatePopulateImportEnum.IMPORT_STATUS_COLUMN.getColumnIndex());

                errorCount++;
            } else {
                // If not, call the service with the payload
                try {
                    String payload = gsonBuilder.create().toJson(staff);
                    final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                            .createClientAllyPointOfSales(staff.getClientAllyId())//
                            .withJson(payload) //
                            .build(); //
                    commandsSourceWritePlatformService.logCommandSource(commandRequest);

                    successCount++;

                    Cell statusCell = staffSheet.getRow(staff.getRowIndex())
                            .createCell(ClientAllyPointOfSalesTemplatePopulateImportEnum.IMPORT_STATUS_COLUMN.getColumnIndex());
                    statusCell.setAsActiveCell();
                    statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                    statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
                } catch (RuntimeException ex) {
                    errorCount++;
                    log.error("Problem occurred in importEntity function", ex);
                    errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                    ImportHandlerUtils.writeErrorMessage(staffSheet, staff.getRowIndex(), errorMessage,
                            ClientAllyPointOfSalesTemplatePopulateImportEnum.IMPORT_STATUS_COLUMN.getColumnIndex());
                }
            }
        }

        staffSheet.setColumnWidth(ClientAllyPointOfSalesTemplatePopulateImportEnum.IMPORT_STATUS_COLUMN.getColumnIndex(),
                TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE);
        ImportHandlerUtils.writeString(ClientAllyPointOfSalesTemplatePopulateImportEnum.IMPORT_STATUS_COLUMN.getColumnIndex(),
                staffSheet.getRow(0), TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        return Count.instance(successCount, errorCount);
    }
}
