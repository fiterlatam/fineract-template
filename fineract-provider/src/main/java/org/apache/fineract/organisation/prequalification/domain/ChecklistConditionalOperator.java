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
package org.apache.fineract.organisation.prequalification.domain;

import lombok.Getter;

@Getter
public enum ChecklistConditionalOperator {

    INVALID(0), EQUAL(1), NOT_EQUAL(2), GREATER(3), GREATER_OR_EQUAL(4), LESS(5), LESS_OR_EQUAL(6), BETWEEN(7), NOT_BETWEEN(8);

    private final Integer value;

    ChecklistConditionalOperator(Integer value) {
        this.value = value;
    }

    public static ChecklistConditionalOperator fromInt(final Integer statusValue) {
        return switch (statusValue) {
            case 1 -> ChecklistConditionalOperator.EQUAL;
            case 2 -> ChecklistConditionalOperator.NOT_EQUAL;
            case 3 -> ChecklistConditionalOperator.GREATER;
            case 4 -> ChecklistConditionalOperator.GREATER_OR_EQUAL;
            case 5 -> ChecklistConditionalOperator.LESS;
            case 6 -> ChecklistConditionalOperator.LESS_OR_EQUAL;
            case 7 -> ChecklistConditionalOperator.BETWEEN;
            case 8 -> ChecklistConditionalOperator.NOT_BETWEEN;
            default -> ChecklistConditionalOperator.INVALID;
        };
    }

}
