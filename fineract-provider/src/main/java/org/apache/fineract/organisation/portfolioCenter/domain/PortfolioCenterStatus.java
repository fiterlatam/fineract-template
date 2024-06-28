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
package org.apache.fineract.organisation.portfolioCenter.domain;

/**
 * Enum representation of portfolio center status states.
 */
public enum PortfolioCenterStatus {

    INVALID(0, "portfolioCenterStatus.invalid"), //
    ACTIVE(1, "portfolioCenterStatus.active"), //
    INACTIVE(2, "portfolioCenterStatus.inactive");

    private final Integer value;
    private final String code;

    public static PortfolioCenterStatus fromInt(final Integer statusValue) {

        PortfolioCenterStatus enumeration = PortfolioCenterStatus.INVALID;
        switch (statusValue) {
            case 1:
                enumeration = PortfolioCenterStatus.ACTIVE;
            break;
            case 2:
                enumeration = PortfolioCenterStatus.INACTIVE;
            break;
        }
        return enumeration;
    }

    PortfolioCenterStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public boolean hasStateOf(final PortfolioCenterStatus state) {
        return this.value.equals(state.getValue());
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public boolean isActive() {
        return this.value.equals(PortfolioCenterStatus.ACTIVE.getValue());
    }

    public boolean isInactive() {
        return this.value.equals(PortfolioCenterStatus.INACTIVE.getValue());
    }
}
