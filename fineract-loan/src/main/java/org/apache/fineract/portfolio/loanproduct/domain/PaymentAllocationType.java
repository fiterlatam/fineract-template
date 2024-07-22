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

import static org.apache.fineract.portfolio.loanproduct.domain.AllocationType.*;
import static org.apache.fineract.portfolio.loanproduct.domain.DueType.DUE;
import static org.apache.fineract.portfolio.loanproduct.domain.DueType.IN_ADVANCE;
import static org.apache.fineract.portfolio.loanproduct.domain.DueType.PAST_DUE;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

@Getter
@RequiredArgsConstructor
public enum PaymentAllocationType {

    PAST_DUE_PENALTY(PAST_DUE, PENALTY, "Past due penalty"), //
    PAST_DUE_FEE(PAST_DUE, FEE, "Past due fee"), //
    PAST_DUE_FEES(PAST_DUE, FEES, "Past due fees"), // Honorarios
    PAST_DUE_PRINCIPAL(PAST_DUE, PRINCIPAL, "Past due principal"), //
    PAST_DUE_INTEREST(PAST_DUE, INTEREST, "Past due interest"), //
    PAST_DUE_AVAL(PAST_DUE, AVAL, "Past due aval"), //
    PAST_DUE_MANDATORY_INSURANCE(PAST_DUE, MANDATORY_INSURANCE, "Past due mandatory insurance"), //
    PAST_DUE_VOLUNTARY_INSURANCE(PAST_DUE, VOLUNTARY_INSURANCE, "Past due mandatory insurance"), //
    DUE_PENALTY(DUE, PENALTY, "Due penalty"), //
    DUE_FEE(DUE, FEE, "Due fee"), //
    DUE_FEES(DUE, FEES, "Due fees"), // Honorarios
    DUE_PRINCIPAL(DUE, PRINCIPAL, "Due principal"), //
    DUE_INTEREST(DUE, INTEREST, "Due interest"), //
    DUE_AVAL(DUE, AVAL, "Due aval"), //
    DUE_MANDATORY_INSURANCE(DUE, MANDATORY_INSURANCE, "Due mandatory insurance"), //
    DUE_VOLUNTARY_INSURANCE(DUE, VOLUNTARY_INSURANCE, "Due mandatory insurance"), //
    IN_ADVANCE_PENALTY(IN_ADVANCE, PENALTY, "In advance penalty"), //
    IN_ADVANCE_FEE(IN_ADVANCE, FEE, "In advance fee"), //
    IN_ADVANCE_FEES(IN_ADVANCE, FEES, "In advance fees"), // Honorarios
    IN_ADVANCE_PRINCIPAL(IN_ADVANCE, PRINCIPAL, "In advance principal"), //
    IN_ADVANCE_INTEREST(IN_ADVANCE, INTEREST, "In advanced interest"), //
    IN_ADVANCE_AVAL(IN_ADVANCE, AVAL, "In advance aval"), //
    IN_ADVANCE_MANDATORY_INSURANCE(IN_ADVANCE, MANDATORY_INSURANCE, "In advance mandatory insurance"), //
    IN_ADVANCE_VOLUNTARY_INSURANCE(IN_ADVANCE, VOLUNTARY_INSURANCE, "In advance mandatory insurance"); //

    private final DueType dueType;
    private final AllocationType allocationType;
    private final String humanReadableName;

    public static List<EnumOptionData> getValuesAsEnumOptionDataList() {
        return AllocationType.getValuesAsEnumOptionDataList();
        // return Arrays.stream(values()).map(v -> new EnumOptionData((long) (v.ordinal() + 1), v.name(),
        // v.getHumanReadableName())).toList();
    }

}
