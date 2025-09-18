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
package org.apache.fineract.custom.portfolio.blockaccounts.domain;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public enum LoanAccountBlockAction {

    BLOCK(0, "block"), UNBLOCK(1, "unblock"), INVALID(2, "invalid");

    private final Integer value;
    private final String code;

    LoanAccountBlockAction(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public static LoanAccountBlockAction fromInt(final Integer action) {

        if (action == null) {
            return LoanAccountBlockAction.INVALID;
        }

        return switch (action) {
            case 0 -> LoanAccountBlockAction.BLOCK;
            case 1 -> LoanAccountBlockAction.UNBLOCK;
            default -> LoanAccountBlockAction.INVALID;
        };
    }

    public EnumOptionData toEnumData() {
        return new EnumOptionData(value.longValue(), getCode(), getCode());
    }

}
