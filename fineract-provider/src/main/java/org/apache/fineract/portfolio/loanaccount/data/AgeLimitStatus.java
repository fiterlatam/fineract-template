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

package org.apache.fineract.portfolio.loanaccount.data;

import org.springframework.util.StringUtils;

/**
 * Enum representation of client identifier status states.
 */
public enum AgeLimitStatus {

    WARNING(100, "age.limit.warning"), //
    BLOCK(200, "age.limit.block"), //
    CONTINUE(300, "age.limit.continue"), //
    INVALID(0, "age.limit.invalid");

    private final Integer value;
    private final String code;

    public static AgeLimitStatus fromInt(final Integer statusValue) {

        AgeLimitStatus enumeration = AgeLimitStatus.INVALID;
        switch (statusValue) {
            case 100:
                enumeration = AgeLimitStatus.WARNING;
            break;
            case 200:
                enumeration = AgeLimitStatus.BLOCK;
            case 300:
                enumeration = AgeLimitStatus.CONTINUE;
            break;
        }
        return enumeration;
    }

    AgeLimitStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static AgeLimitStatus fromString(String status) {

        AgeLimitStatus clientStatus = AgeLimitStatus.INVALID;

        if (!StringUtils.hasLength(status)) {
            return clientStatus;
        }

        if (status.equalsIgnoreCase(AgeLimitStatus.WARNING.toString())) {
            clientStatus = AgeLimitStatus.WARNING;
        } else if (status.equalsIgnoreCase(AgeLimitStatus.BLOCK.toString())) {
            clientStatus = AgeLimitStatus.BLOCK;
        } else if (status.equalsIgnoreCase(AgeLimitStatus.CONTINUE.toString())) {
            clientStatus = AgeLimitStatus.CONTINUE;
        }
        return clientStatus;

    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

}
