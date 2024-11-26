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
package org.apache.fineract.custom.infrastructure.channel.domain;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;

@lombok.AllArgsConstructor
@lombok.Getter
public enum ChannelType {

    DISBURSEMENT(1, "channel.type.disbursement"), REPAYMENT(2, "channel.type.repayment"), INVALID(0, "channel.type.invalid");

    private final Integer value;
    private final String code;

    public static ChannelType fromInt(final Integer typeValue) {
        if (typeValue != null) {
            return switch (typeValue) {
                case 1 -> ChannelType.DISBURSEMENT;
                case 2 -> ChannelType.REPAYMENT;
                default -> ChannelType.INVALID;
            };
        }
        return ChannelType.INVALID;
    }

    public EnumOptionData asEnumOptionData() {
        return new EnumOptionData(this.value.longValue(), this.code, this.name());
    }

}
