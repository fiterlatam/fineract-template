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

import org.apache.fineract.portfolio.client.domain.ClientStatus;
import org.springframework.util.StringUtils;

/**
 * Enum representation of client identifier status states.
 */
public enum PrequalificationMemberIndication {

    ACTIVE(100, "prequalification.member.blacklist.active"), //
    INACTIVE(200, "prequalification.member.blacklist.inactive"), //
    NONE(300, "prequalification.member.blacklist.none"), //
    INVALID(0, "prequalification.invalid");

    private final Integer value;
    private final String code;

    public static PrequalificationMemberIndication fromInt(final Integer statusValue) {

        PrequalificationMemberIndication enumeration = PrequalificationMemberIndication.INVALID;
        switch (statusValue) {
            case 100:
                enumeration = PrequalificationMemberIndication.ACTIVE;
                break;
            case 200:
                enumeration = PrequalificationMemberIndication.INACTIVE;
            case 300:
                enumeration = PrequalificationMemberIndication.NONE;
                break;
        }
        return enumeration;
    }

    PrequalificationMemberIndication(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static PrequalificationMemberIndication fromString(String status) {

        PrequalificationMemberIndication clientStatus = PrequalificationMemberIndication.INVALID;

        if (!StringUtils.hasLength(status)) {
            return clientStatus;
        }

        if (status.equalsIgnoreCase(PrequalificationMemberIndication.INACTIVE.toString())) {
            clientStatus = PrequalificationMemberIndication.INACTIVE;
        } else if (status.equalsIgnoreCase(ClientStatus.PENDING.toString())) {
            clientStatus = PrequalificationMemberIndication.ACTIVE;
        } else if (status.equalsIgnoreCase(ClientStatus.REJECTED.toString())) {
            clientStatus = PrequalificationMemberIndication.NONE;
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

    public boolean isActive() {
        return this.value.equals(PrequalificationMemberIndication.ACTIVE.getValue());
    }

    public boolean isInactive() {
        return this.value.equals(PrequalificationMemberIndication.INACTIVE.getValue());
    }

    public boolean isNotBlacklisted() {
        return this.value.equals(PrequalificationMemberIndication.NONE.getValue());
    }
}
