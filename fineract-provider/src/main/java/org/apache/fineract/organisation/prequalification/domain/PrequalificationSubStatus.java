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

import org.springframework.util.StringUtils;

/**
 * Enum representation of prequalification status states.
 */
public enum PrequalificationSubStatus {

    PENDING(100, "prequalification.status.pending"), //
    IN_PROGRESS(200, "prequalification.status.inprogress"), //
    COMPLETED(300, "prequalification.status.completed"), INVALID(999, "prequalification.status.rejected");

    private final Integer value;
    private final String code;

    public static PrequalificationSubStatus fromInt(final Integer statusValue) {

        PrequalificationSubStatus enumeration = PrequalificationSubStatus.INVALID;
        switch (statusValue) {
            case 100:
                enumeration = PrequalificationSubStatus.PENDING;
            break;
            case 200:
                enumeration = PrequalificationSubStatus.IN_PROGRESS;
            break;
            case 300:
                enumeration = PrequalificationSubStatus.COMPLETED;
            break;
        }
        return enumeration;
    }

    PrequalificationSubStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static PrequalificationSubStatus fromString(String status) {

        PrequalificationSubStatus clientStatus = PrequalificationSubStatus.INVALID;

        if (!StringUtils.hasLength(status)) {
            return clientStatus;
        }

        if (status.equalsIgnoreCase(PrequalificationSubStatus.PENDING.toString())) {
            clientStatus = PrequalificationSubStatus.PENDING;
        } else if (status.equalsIgnoreCase(PrequalificationSubStatus.IN_PROGRESS.toString())) {
            clientStatus = PrequalificationSubStatus.IN_PROGRESS;
        } else if (status.equalsIgnoreCase(PrequalificationSubStatus.COMPLETED.toString())) {
            clientStatus = PrequalificationSubStatus.COMPLETED;
        } else {
            clientStatus = PrequalificationSubStatus.INVALID;
        }

        return clientStatus;

    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public boolean isPending() {
        return this.value.equals(PrequalificationSubStatus.PENDING.getValue());
    }

    public boolean isInProgress() {
        return this.value.equals(PrequalificationSubStatus.IN_PROGRESS.getValue());
    }

    public boolean isCompleted() {
        return this.value.equals(PrequalificationSubStatus.COMPLETED.getValue());
    }
}
