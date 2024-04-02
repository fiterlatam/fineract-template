package org.apache.fineract.custom.infrastructure.bulkimport.data;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CustomGlobalEntityType {

    CLIENT_ALLY(1000, "Aliados", "clients.allies"), //
    CLIENT_ALLY_POINTS_OF_SALES(1001, "PuntosDeVentas", "clients.allies.points.of.sales"), //
    ; //

    private final Integer value;
    private final String code;
    private final String alias;

    private static final Map<Integer, CustomGlobalEntityType> intToEnumMap = new HashMap<>();
    private static final Map<String, CustomGlobalEntityType> stringToEnumMap = new HashMap<>();
    private static int minValue;
    private static int maxValue;

    static {
        int i = 0;
        for (final CustomGlobalEntityType entityType : CustomGlobalEntityType.values()) {
            if (i == 0) {
                minValue = entityType.getValue();
            }
            intToEnumMap.put(entityType.getValue(), entityType);
            stringToEnumMap.put(entityType.getCode(), entityType);
            if (minValue >= entityType.getValue()) {
                minValue = entityType.getValue();
            }
            if (maxValue < entityType.getValue()) {
                maxValue = entityType.getValue();
            }
            i = i + 1;
        }
    }

    public static CustomGlobalEntityType fromInt(final int i) {
        final CustomGlobalEntityType entityType = intToEnumMap.get(Integer.valueOf(i));
        return entityType;
    }

    public static CustomGlobalEntityType fromCode(final String key) {
        final CustomGlobalEntityType entityType = stringToEnumMap.get(key);
        return entityType;
    }

    @Override
    public String toString() {
        return name().toString();
    }
}
