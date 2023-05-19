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
package org.apache.fineract.organisation.centerGroup.service;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.centerGroup.data.CenterGroupStatusEnumData;
import org.apache.fineract.organisation.centerGroup.domain.CenterGroupStatus;

public final class CenterGroupEnumerations {

    private CenterGroupEnumerations() {

    }

    public static CenterGroupStatusEnumData status(final Integer statusId) {
        return status(CenterGroupStatus.fromInt(statusId));
    }

    public static CenterGroupStatusEnumData status(final CenterGroupStatus statusId) {
        CenterGroupStatusEnumData optionData = new CenterGroupStatusEnumData(CenterGroupStatus.INVALID.getValue().longValue(),
                CenterGroupStatus.INVALID.getCode(), "Invalid");

        switch (statusId) {
            case INVALID:
                optionData = new CenterGroupStatusEnumData(CenterGroupStatus.INVALID.getValue().longValue(),
                        CenterGroupStatus.INVALID.getCode(), "Invalid");
            break;
            case ACTIVE:
                optionData = new CenterGroupStatusEnumData(CenterGroupStatus.ACTIVE.getValue().longValue(),
                        CenterGroupStatus.ACTIVE.getCode(), "Active");
            break;
            case INACTIVE:
                optionData = new CenterGroupStatusEnumData(CenterGroupStatus.INACTIVE.getValue().longValue(),
                        CenterGroupStatus.INACTIVE.getCode(), "Inactive");
            break;
        }

        return optionData;
    }

    public static EnumOptionData groupStatusOptionData(final int id) {
        return groupStatusOptionData(CenterGroupStatus.fromInt(id));
    }

    public static EnumOptionData groupStatusOptionData(final CenterGroupStatus status) {
        EnumOptionData optionData = null;
        switch (status) {
            case ACTIVE:
                optionData = new EnumOptionData(CenterGroupStatus.ACTIVE.getValue().longValue(), CenterGroupStatus.ACTIVE.getCode(),
                        "Active");
            break;
            case INACTIVE:
                optionData = new EnumOptionData(CenterGroupStatus.INACTIVE.getValue().longValue(), CenterGroupStatus.INACTIVE.getCode(),
                        "Inactive");
            break;
            default:
                optionData = new EnumOptionData(CenterGroupStatus.INVALID.getValue().longValue(), CenterGroupStatus.INVALID.getCode(),
                        "Invalid");
            break;
        }
        return optionData;
    }

}
