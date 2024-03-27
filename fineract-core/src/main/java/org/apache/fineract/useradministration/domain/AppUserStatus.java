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
package org.apache.fineract.useradministration.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AppUserStatus {

    INVALID(0, "appUserStatus.invalid"), ACTIVE(300, "appUserStatus.active"), INACTIVE_TEMPORARY(400,
            "appUserStatus.inactive.temporary"), INACTIVE_PERMANENT(500, "appUserStatus.inactive.permanent");

    private final Integer value;
    private final String code;

    public static AppUserStatus fromInt(final Integer statusValue) {
        return switch (statusValue) {
            case 300 -> AppUserStatus.ACTIVE;
            case 400 -> AppUserStatus.INACTIVE_TEMPORARY;
            case 500 -> AppUserStatus.INACTIVE_PERMANENT;
            default -> AppUserStatus.INVALID;
        };
    }

    public Boolean isActive() {
        return AppUserStatus.ACTIVE.value.equals(this.value);
    }

    public Boolean isInactive() {
        return !isActive();
    }

    public Boolean isTemporarilyInactive() {
        return AppUserStatus.INACTIVE_TEMPORARY.value.equals(this.value);
    }

    public Boolean isPermanentlyInactive() {
        return AppUserStatus.INACTIVE_PERMANENT.value.equals(this.value);
    }

}
