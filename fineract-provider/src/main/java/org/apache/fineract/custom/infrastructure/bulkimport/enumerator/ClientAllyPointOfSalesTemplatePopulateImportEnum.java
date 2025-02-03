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
package org.apache.fineract.custom.infrastructure.bulkimport.enumerator;

import static org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants.EXTRALARGE_COL_SIZE;
import static org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants.LARGE_COL_SIZE;
import static org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants.MEDIUM_COL_SIZE;
import static org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants.SMALL_COL_SIZE;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ClientAllyPointOfSalesTemplatePopulateImportEnum {

    COMPANY_NAME_ID(0, "Razón social", null, EXTRALARGE_COL_SIZE, true, Long.class), //
    NAME(1, "Nombre del Punto de Venta", null, LARGE_COL_SIZE, true, String.class, 100L), //
    CODE(2, "Codigo", null, SMALL_COL_SIZE, true, String.class, 6l), //
    BRAND_ID(3, "Marca", "Marca", MEDIUM_COL_SIZE, true, Long.class), //
    DEPARTMENT_ID(4, "Departamento", "Departamento", SMALL_COL_SIZE, true, Long.class), //
    CITY_ID(5, "Ciudad", "Ciudad", LARGE_COL_SIZE, true, Long.class), //
    CATEGORY_ID(6, "Categoria", "CategoriaPuntoDeVenta", LARGE_COL_SIZE, true, Long.class), //
    SEGMENT_ID(7, "Segmento", "SegmentoPuntoDeVenta", LARGE_COL_SIZE, true, Long.class), //
    TYPE_ID(8, "Tipo", "TipoPuntoDeVenta", LARGE_COL_SIZE, true, Long.class), //
    SETTLED_COMISSION(9, "Comisión pactada", null, MEDIUM_COL_SIZE, true, Double.class), //
    BUY_ENABLED(10, "Habilitado para compras", null, LARGE_COL_SIZE, true, Boolean.class), //
    COLLECTION_ENABLED(11, "Habilitado para recaudos", null, LARGE_COL_SIZE, true, Boolean.class), //
    STATE_ID(12, "Estado", "Estado", SMALL_COL_SIZE, true, Long.class), //
    IMPORT_STATUS_COLUMN(13, "Importacion", null, MEDIUM_COL_SIZE, false, String.class, 0L), //
    ;

    private Integer columnIndex;
    private String columnName;
    private String codeValueName;
    private int columnSize;
    private Boolean mandatory;
    private Class clazz;
    private Long maxLength;

    ClientAllyPointOfSalesTemplatePopulateImportEnum(Integer columnIndex, String columnName, String codeValueName, int columnSize,
            Boolean mandatory, Class clazz) {
        this.columnIndex = columnIndex;
        this.columnName = columnName;
        this.codeValueName = codeValueName;
        this.columnSize = columnSize;
        this.mandatory = mandatory;
        this.clazz = clazz;
    }

    public static Optional<ClientAllyPointOfSalesTemplatePopulateImportEnum> findByColumnName(String columnName) {
        return Arrays.asList(ClientAllyPointOfSalesTemplatePopulateImportEnum.values()).stream()
                .filter(obj -> obj.getColumnName().equalsIgnoreCase(columnName)).findFirst();
    }

    public static List<ClientAllyPointOfSalesTemplatePopulateImportEnum> getOrdered() {
        return Arrays.asList(ClientAllyPointOfSalesTemplatePopulateImportEnum.values()).stream()
                .sorted(Comparator.comparing(ClientAllyPointOfSalesTemplatePopulateImportEnum::getColumnIndex))
                .collect(Collectors.toList());
    }

}
