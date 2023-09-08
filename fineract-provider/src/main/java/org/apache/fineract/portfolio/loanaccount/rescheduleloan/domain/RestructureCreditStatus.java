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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain;

import org.apache.fineract.portfolio.client.domain.ClientStatus;
import org.springframework.util.StringUtils;

/**
 * Enum representation of client identifier status states.
 */
public enum RestructureCreditStatus {

    PENDING(100, "restructure.status.pending"), //
    APPROVED(200, "restructure.status.approved"), //
    REJECTED(300, "restructure.status.rejected"),
    INVALID(0, "prequalification.status.invalid");

    private final Integer value;
    private final String code;

    public static RestructureCreditStatus fromInt(final Integer statusValue) {

        RestructureCreditStatus enumeration = RestructureCreditStatus.INVALID;
        switch (statusValue) {
            case 100:
                enumeration = RestructureCreditStatus.PENDING;
            break;
            case 200:
                enumeration = RestructureCreditStatus.APPROVED;
            break;
            case 300:
                enumeration = RestructureCreditStatus.REJECTED;
            break;
        }
        return enumeration;
    }

    RestructureCreditStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static RestructureCreditStatus fromString(String status) {

        RestructureCreditStatus clientStatus = RestructureCreditStatus.INVALID;

        if (!StringUtils.hasLength(status)) {
            return clientStatus;
        }

        if (status.equalsIgnoreCase(RestructureCreditStatus.APPROVED.toString())) {
            clientStatus = RestructureCreditStatus.APPROVED;
        } else if (status.equalsIgnoreCase(ClientStatus.PENDING.toString())) {
            clientStatus = RestructureCreditStatus.PENDING;
        } else if (status.equalsIgnoreCase(ClientStatus.REJECTED.toString())) {
            clientStatus = RestructureCreditStatus.REJECTED;
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
        return this.value.equals(RestructureCreditStatus.PENDING.getValue());
    }

    public boolean isApproved() {
        return this.value.equals(RestructureCreditStatus.APPROVED.getValue());
    }

    public boolean isRejected() {
        return this.value.equals(RestructureCreditStatus.REJECTED.getValue());
    }
}
