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
