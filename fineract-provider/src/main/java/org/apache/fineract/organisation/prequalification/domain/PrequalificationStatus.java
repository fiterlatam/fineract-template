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
    INVALID(0, "prequalification.invalid");

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
            case 300:
                enumeration = PrequalificationStatus.REJECTED;
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
        }else if (status.equalsIgnoreCase(ClientStatus.REJECTED.toString())) {
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
