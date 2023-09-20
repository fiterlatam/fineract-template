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
public enum PrequalificationStatus {

    PENDING(100, "prequalification.status.pending"), //
    APPROVED(200, "prequalification.status.approved"), //
    REJECTED(300, "prequalification.status.rejected"), //
    BLACKLIST_CHECKED(400, "prequalification.status.blacklist.checked"), //
    BLACKLIST_REJECTED(500, "prequalification.status.blacklist.rejected"),
    BURO_CHECKED(600, "prequalification.status.buro.checked"),
    HARD_POLICY_CHECKED(700, "prequalification.status.hard.policy.checked"),
    TIME_EXPIRED(800, "prequalification.status.expired"),
    COMPLETED(900, "prequalification.status.completed"),
    CONSENT_ADDED(901, "prequalification.status.concent.added"),
    INVALID(0, "prequalification.status.invalid");
    private final Integer value;
    private final String code;

    public static PrequalificationStatus fromInt(final Integer statusValue) {

        PrequalificationStatus enumeration = PrequalificationStatus.INVALID;
        switch (statusValue) {
            case 100:
                enumeration = PrequalificationStatus.PENDING;
                break;
            case 200:
                enumeration = PrequalificationStatus.APPROVED;
                break;
            case 300:
                enumeration = PrequalificationStatus.REJECTED;
                break;
            case 400:
                enumeration = PrequalificationStatus.BLACKLIST_CHECKED;
                break;
            case 500:
                enumeration = PrequalificationStatus.BLACKLIST_REJECTED;
                break;
            case 600:
                enumeration = PrequalificationStatus.BURO_CHECKED;
                break;
            case 700:
                enumeration = PrequalificationStatus.HARD_POLICY_CHECKED;
                break;
            case 800:
                enumeration = PrequalificationStatus.TIME_EXPIRED;
                break;
            case 900:
                enumeration = PrequalificationStatus.COMPLETED;
                break;
            case 901:
                enumeration = PrequalificationStatus.CONSENT_ADDED;
                break;
        }
        return enumeration;
    }

    PrequalificationStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static PrequalificationStatus fromString(String status) {

        PrequalificationStatus clientStatus = PrequalificationStatus.INVALID;

        if (!StringUtils.hasLength(status)) {
            return clientStatus;
        }

        if (status.equalsIgnoreCase(PrequalificationStatus.APPROVED.toString())) {
            clientStatus = PrequalificationStatus.APPROVED;
        } else if (status.equalsIgnoreCase(ClientStatus.PENDING.toString())) {
            clientStatus = PrequalificationStatus.PENDING;
        } else if (status.equalsIgnoreCase(ClientStatus.REJECTED.toString())) {
            clientStatus = PrequalificationStatus.REJECTED;
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

    public boolean isPending() {
        return this.value.equals(PrequalificationStatus.PENDING.getValue());
    }

    public boolean isApproved() {
        return this.value.equals(PrequalificationStatus.APPROVED.getValue());
    }

    public boolean isRejected() {
        return this.value.equals(PrequalificationStatus.REJECTED.getValue());
    }
}
