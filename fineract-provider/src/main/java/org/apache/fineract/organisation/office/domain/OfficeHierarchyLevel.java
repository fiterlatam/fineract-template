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
package org.apache.fineract.organisation.office.domain;

import org.apache.fineract.organisation.holiday.domain.HolidayStatusType;

/**
 * Enum representation of {@link Office} hierarchy levels.
 */
public enum OfficeHierarchyLevel {

    INVALID(0, "holidayStatusType.invalid"), //
    GERENCIA(1, "officehierarchylevel.gerencia"), //
    REGION(2, "officehierarchylevel.region");

    private final Integer value;
    private final String code;

    public static OfficeHierarchyLevel fromInt(final Integer type) {
        OfficeHierarchyLevel enumeration = OfficeHierarchyLevel.INVALID;
        switch (type) {
            case 1:
                enumeration = OfficeHierarchyLevel.GERENCIA;
            break;
            case 2:
                enumeration = OfficeHierarchyLevel.REGION;
            break;
        }
        return enumeration;
    }

    OfficeHierarchyLevel(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public boolean hasStateOf(final HolidayStatusType state) {
        return this.value.equals(state.getValue());
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

}
