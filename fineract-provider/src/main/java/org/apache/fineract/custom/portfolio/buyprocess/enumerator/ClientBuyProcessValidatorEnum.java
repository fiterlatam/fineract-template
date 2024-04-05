package org.apache.fineract.custom.portfolio.buyprocess.enumerator;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants.LARGE_COL_SIZE;

@AllArgsConstructor
@Getter
public enum ClientBuyProcessValidatorEnum {

    CHANNEL_VALIDATOR(100L, "channel", null, LARGE_COL_SIZE, true, String.class, 100L), //
    CURRENT_DATE_VALIDATOR(200L, "current.date", null, LARGE_COL_SIZE, true, String.class, 100L), //
    POINT_OF_SALES_VALIDATOR(300L, "point.of.sales", null, LARGE_COL_SIZE, true, String.class, 100L), //
    CLIENT_VALIDATOR(400L, "client", null, LARGE_COL_SIZE, true, String.class, 100L), //

    REQUESTED_VS_AVAILABLE_AMOUNT_CLIENT_VALIDATOR(600L, "client.available.amount", null, LARGE_COL_SIZE, true, String.class, 100L), //
    TERM_VALIDATOR(700L, "term", null, LARGE_COL_SIZE, true, String.class, 100L), //
    MINIMUM_AMOUNT_VALIDATOR(800L, "minimum.amount", null, LARGE_COL_SIZE, true, String.class, 100L), //
    REQUESTED_VS_AVAILABLE_AMOUNT_ALLY_VALIDATOR(900L, "ally.available.amount", null, LARGE_COL_SIZE, true, String.class, 100L), //

    ;

    private Long columnIndex;
    private String columnName;
    private String codeValueName;
    private int columnSize;
    private Boolean mandatory;
    private Class clazz;
    private Long maxLength;

    ClientBuyProcessValidatorEnum(Long columnIndex, String columnName, String codeValueName, int columnSize, Boolean mandatory,
                                  Class clazz) {
        this.columnIndex = columnIndex;
        this.columnName = columnName;
        this.codeValueName = codeValueName;
        this.columnSize = columnSize;
        this.mandatory = mandatory;
        this.clazz = clazz;
    }

    public static Optional<ClientBuyProcessValidatorEnum> findByColumnName(String columnName) {
        return Arrays.asList(ClientBuyProcessValidatorEnum.values()).stream()
                .filter(obj -> obj.getColumnName().equalsIgnoreCase(columnName)).findFirst();
    }

    public static List<ClientBuyProcessValidatorEnum> getOrdered() {
        return Arrays.asList(ClientBuyProcessValidatorEnum.values()).stream()
                .sorted(Comparator.comparing(ClientBuyProcessValidatorEnum::getColumnIndex)).collect(Collectors.toList());
    }

}
