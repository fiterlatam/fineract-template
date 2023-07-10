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

package org.apache.fineract.portfolio.blacklist.domain;

import org.apache.fineract.portfolio.client.domain.ClientStatus;
import org.springframework.util.StringUtils;

/**
 * Enum representation of client identifier status states.
 */
public enum BlacklistStatus {

    INACTIVE(100, "blacklist.status.inactive"), //
    ACTIVE(200, "blacklist.status.active"), //
    INVALID(0, "clientIdentifierStatusType.invalid");

    private final Integer value;
    private final String code;

    public static BlacklistStatus fromInt(final Integer statusValue) {

        BlacklistStatus enumeration = BlacklistStatus.INVALID;
        switch (statusValue) {
            case 100:
                enumeration = BlacklistStatus.INACTIVE;
                break;
            case 200:
                enumeration = BlacklistStatus.ACTIVE;
                break;
        }
        return enumeration;
    }

    BlacklistStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static BlacklistStatus fromString(String status) {

        BlacklistStatus clientStatus = BlacklistStatus.INVALID;

        if (!StringUtils.hasLength(status)) {
            return clientStatus;
        }

        if (status.equalsIgnoreCase(BlacklistStatus.INACTIVE.toString())) {
            clientStatus = BlacklistStatus.INACTIVE;
        } else if (status.equalsIgnoreCase(ClientStatus.ACTIVE.toString())) {
            clientStatus = BlacklistStatus.ACTIVE;
        }

        return clientStatus;

    }

    // public boolean hasStateOf(final ClientIdentifierStatus state) {
    // return this.value.equals(state.getValue());
    // }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public boolean isInactive() {
        return this.value.equals(BlacklistStatus.INACTIVE.getValue());
    }

    public boolean isActive() {
        return this.value.equals(BlacklistStatus.ACTIVE.getValue());
    }
}
