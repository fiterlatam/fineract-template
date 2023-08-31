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
public enum CheckValidationColor {

    INVALID(0, "prequalification.evaluation.color.invalid"), GREEN(1, "prequalification.evaluation.color.green"), YELLOW(2,
            "prequalification.evaluation.color.yellow"), ORANGE(3,
                    "prequalification.evaluation.color.orange"), RED(4, "prequalification.evaluation.color.red");

    private final Integer value;
    private final String code;

    CheckValidationColor(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static CheckValidationColor fromInt(final Integer statusValue) {
        return switch (statusValue) {
            case 1 -> CheckValidationColor.GREEN;
            case 2 -> CheckValidationColor.YELLOW;
            case 3 -> CheckValidationColor.ORANGE;
            case 4 -> CheckValidationColor.RED;
            default -> CheckValidationColor.INVALID;
        };
    }
}
