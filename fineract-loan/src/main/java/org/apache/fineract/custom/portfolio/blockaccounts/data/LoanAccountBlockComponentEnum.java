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
package org.apache.fineract.custom.portfolio.blockaccounts.data;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum LoanAccountBlockComponentEnum {

    BLOCK_DISBURSAL(0, "Freeze Disbursal"), //
    ACCELERATE(1, "Accelerate"), //
    FREEZE_INTEREST(2, "Freeze Interest"), //
    FREEZE_MORA(3, "Freeze Mora"), //
    FREEZE_LIFE_INSURANCE(4, "Freeze Life Insurance"), //
    FREEZE_MIPYME(5, "Freeze MiPyme"), //
    FREEZE_GAC(6, "Freeze GAC"), //
    UNDEFINED(-1, "Undefined"), //
    ;

    private Integer id;
    private String value;

    public static LoanAccountBlockComponentEnum findByStatus(String status) {
        return Arrays.asList(LoanAccountBlockComponentEnum.values()).stream().filter(obj -> obj.getValue().equalsIgnoreCase(status))
                .findFirst().orElse(UNDEFINED);
    }
}
