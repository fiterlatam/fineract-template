package org.apache.fineract.custom.portfolio.ally.domain;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;

@lombok.AllArgsConstructor
@lombok.Getter
public enum LiquidationFrequency {

    DAILY(1172, "ally.liquidationFrequency.daily"), WEEKLY(1173, "ally.liquidationFrequency.weekly"), BIWEEKLY(2461,
            "ally.liquidationFrequency.beweekly"), MONTHLY(1174, "ally.liquidationFrequency.monthly");

    private final Integer value;
    private final String code;

    public static LiquidationFrequency fromInt(final Integer codeValue) {
        if (codeValue != null) {
            return switch (codeValue) {
                case 1174 -> LiquidationFrequency.MONTHLY;
                case 1173 -> LiquidationFrequency.WEEKLY;
                case 2461 -> LiquidationFrequency.BIWEEKLY;
                default -> LiquidationFrequency.DAILY;
            };
        }
        return LiquidationFrequency.DAILY;
    }

    public EnumOptionData asEnumOptionData() {
        return new EnumOptionData(this.value.longValue(), this.code, this.name());
    }
}
