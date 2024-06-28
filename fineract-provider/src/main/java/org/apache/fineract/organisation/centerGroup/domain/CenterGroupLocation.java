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

package org.apache.fineract.organisation.centerGroup.domain;

import org.apache.fineract.portfolio.client.domain.ClientStatus;
import org.springframework.util.StringUtils;

/**
 * Enum representation of client identifier status states.
 */
public enum CenterGroupLocation {

    URBAN(100, "center.group.location.urban"), //
    RURAL(200, "center.group.location.rural"), //
    INVALID(0, "center.group.location.invalid");

    private final Integer value;
    private final String code;

    public static CenterGroupLocation fromInt(final Integer statusValue) {

        CenterGroupLocation enumeration = CenterGroupLocation.INVALID;
        switch (statusValue) {
            case 100:
                enumeration = CenterGroupLocation.URBAN;
            break;
            case 200:
                enumeration = CenterGroupLocation.RURAL;
            break;
        }
        return enumeration;
    }

    CenterGroupLocation(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static CenterGroupLocation fromString(String status) {

        CenterGroupLocation groupLocation = CenterGroupLocation.INVALID;

        if (!StringUtils.hasLength(status)) {
            return groupLocation;
        }

        if (status.equalsIgnoreCase(CenterGroupLocation.URBAN.toString())) {
            groupLocation = CenterGroupLocation.URBAN;
        } else if (status.equalsIgnoreCase(ClientStatus.ACTIVE.toString())) {
            groupLocation = CenterGroupLocation.RURAL;
        }

        return groupLocation;

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
        return this.value.equals(CenterGroupLocation.URBAN.getValue());
    }

    public boolean isActive() {
        return this.value.equals(CenterGroupLocation.RURAL.getValue());
    }
}
