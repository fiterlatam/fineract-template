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
package org.apache.fineract.portfolio.loanproduct.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

@RequiredArgsConstructor
@Getter
public enum AllocationType {

    PENALTY("Penalty", "labels.allocations.types.penalty"), //
    FEE("Fee", "labels.allocations.types.fee"), //
    PRINCIPAL("Principal", "labels.allocations.types.principal"), //
    INTEREST("Interest", "labels.allocations.types.interest"), //
    FEES("Honorarios", "labels.allocations.types.fees"), //
    AVAL("Aval", "labels.allocations.types.aval"), //
    MANDATORY_INSURANCE("Mandatory Insurance", "labels.allocations.types.mandatory.insurance"),
    VOLUNTARY_INSURANCE("Voluntary Insurance", "labels.allocations.types.voluntary.insurance");

    private final String humanReadableName;
    private final String code;

    public static List<EnumOptionData> getValuesAsEnumOptionDataList() {
        List<EnumOptionData> list = new ArrayList<>(Arrays.stream(values())
                .map(v -> new EnumOptionData((long) (v.ordinal() + 1), v.name(), v.getCode())).toList());
        // Remove FEE enum from the list as it is split into FEES, AVAL, MANDATORY_INSURANCE and VOLUNTARY_INSURANCE.
        list.removeIf(x -> x.getValue().equals("Fee"));
        return list;
    }
}
