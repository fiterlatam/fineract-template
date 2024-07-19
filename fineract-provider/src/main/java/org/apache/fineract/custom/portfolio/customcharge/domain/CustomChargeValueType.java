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
package org.apache.fineract.custom.portfolio.customcharge.domain;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;

@lombok.AllArgsConstructor
@lombok.Getter
public enum CustomChargeValueType {

    VIP(1, "custom.charge.type.vip"), COMMERCE(2, "custom.charge.type.commerce"), PRODUCT(3, "custom.charge.type.product"), INVALID(0,
            "custom.charge.type.invalid");

    private final Integer value;
    private final String code;

    public static CustomChargeValueType fromInt(final Integer typeValue) {
        if (typeValue != null) {
            return switch (typeValue) {
                case 1 -> CustomChargeValueType.VIP;
                case 2 -> CustomChargeValueType.COMMERCE;
                case 3 -> CustomChargeValueType.PRODUCT;
                default -> CustomChargeValueType.INVALID;
            };
        }
        return CustomChargeValueType.INVALID;
    }

    public EnumOptionData asEnumOptionData() {
        return new EnumOptionData(this.value.longValue(), this.code, this.name());
    }

}
