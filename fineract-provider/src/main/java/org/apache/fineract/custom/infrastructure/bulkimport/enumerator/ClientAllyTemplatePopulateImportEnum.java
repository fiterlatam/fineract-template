package org.apache.fineract.custom.infrastructure.bulkimport.enumerator;

import static org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants.LARGE_COL_SIZE;
import static org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants.MEDIUM_COL_SIZE;
import static org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants.SMALL_COL_SIZE;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ClientAllyTemplatePopulateImportEnum {

    COMPANY_NAME(0, "Razón social", null, LARGE_COL_SIZE, true, String.class, 100L), //
    NIT(1, "NIT", null, MEDIUM_COL_SIZE, true, String.class, 20L), //
    NIT_DIGIT(2, "Dígito NIT", null, SMALL_COL_SIZE, true, Integer.class, 4L), //
    ADDRESS(3, "Dirección", null, LARGE_COL_SIZE, true, String.class, 100L), //
    DEPARTMENT_ID(4, "Departamento", "Departamento", SMALL_COL_SIZE, true, Long.class), //
    CITY_ID(5, "Ciudad", "Ciudad", LARGE_COL_SIZE, true, Long.class), //
    LIQUIDATION_FREQUENCY_ID(6, "Frec. Liquidacion", "FrecuenciaLiquidacion", MEDIUM_COL_SIZE, true, Long.class), //
    APPLY_CUPO_MAX_SELL(7, "Aplica cupo máximo de ventas", null, LARGE_COL_SIZE, true, Boolean.class), //
    CUPO_MAX_SELL(8, "Cupo máximo de ventas", null, LARGE_COL_SIZE, false, BigDecimal.class), //
    SETTLED_COMISSION(9, "Comisión pactada", null, MEDIUM_COL_SIZE, true, Double.class), //
    BUY_ENABLED(10, "Habilitado para compras", null, LARGE_COL_SIZE, true, Boolean.class), //
    COLLECTION_ENABLED(11, "Habilitado para recaudos", null, LARGE_COL_SIZE, true, Boolean.class), //
    BANK_ENTITY_ID(12, "Entidad Bancaria", "EntidadBancaria", LARGE_COL_SIZE, false, Long.class), //
    ACCOUNT_TYPE_ID(13, "Tipo Cuenta", "TipoCuentaBancaria", SMALL_COL_SIZE, false, Long.class), //
    ACCOUNT_NUMBER(14, "Numero da Cuenta", null, LARGE_COL_SIZE, false, String.class, 20L), //
    TAX_PROFILE_ID(15, "Perfil Tributario Regimen IVA", "PerfilTributarioRegimenIVA", LARGE_COL_SIZE, true, Long.class), //
    STATE_ID(16, "Estado", "Estado", SMALL_COL_SIZE, true, Long.class), //
    IMPORT_STATUS_COLUMN(17, "Importacion", null, MEDIUM_COL_SIZE, false, String.class, 0L), //
    ;

    private Integer columnIndex;
    private String columnName;
    private String codeValueName;
    private int columnSize;
    private Boolean mandatory;
    private Class clazz;
    private Long maxLength;

    ClientAllyTemplatePopulateImportEnum(Integer columnIndex, String columnName, String codeValueName, int columnSize, Boolean mandatory,
            Class clazz) {
        this.columnIndex = columnIndex;
        this.columnName = columnName;
        this.codeValueName = codeValueName;
        this.columnSize = columnSize;
        this.mandatory = mandatory;
        this.clazz = clazz;
    }

    public static Optional<ClientAllyTemplatePopulateImportEnum> findByColumnName(String columnName) {
        return Arrays.asList(ClientAllyTemplatePopulateImportEnum.values()).stream()
                .filter(obj -> obj.getColumnName().equalsIgnoreCase(columnName)).findFirst();
    }

    public static List<ClientAllyTemplatePopulateImportEnum> getOrdered() {
        return Arrays.asList(ClientAllyTemplatePopulateImportEnum.values()).stream()
                .sorted(Comparator.comparing(ClientAllyTemplatePopulateImportEnum::getColumnIndex)).collect(Collectors.toList());
    }

}
