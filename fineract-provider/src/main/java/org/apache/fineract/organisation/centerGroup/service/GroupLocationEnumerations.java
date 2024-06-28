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
import org.apache.fineract.organisation.centerGroup.data.GroupLocationEnumData;
import org.apache.fineract.organisation.centerGroup.domain.CenterGroupLocation;

public final class GroupLocationEnumerations {

    private GroupLocationEnumerations() {

    }

    public static GroupLocationEnumData status(final Integer statusId) {
        return status(CenterGroupLocation.fromInt(statusId));
    }

    public static GroupLocationEnumData status(final CenterGroupLocation statusId) {
        GroupLocationEnumData optionData = new GroupLocationEnumData(CenterGroupLocation.INVALID.getValue().longValue(),
                CenterGroupLocation.INVALID.getCode(), "Invalid");

        switch (statusId) {
            case INVALID:
                optionData = new GroupLocationEnumData(CenterGroupLocation.INVALID.getValue().longValue(),
                        CenterGroupLocation.INVALID.getCode(), "Invalid");
            break;
            case URBAN:
                optionData = new GroupLocationEnumData(CenterGroupLocation.URBAN.getValue().longValue(),
                        CenterGroupLocation.URBAN.getCode(), "Urban");
            break;
            case RURAL:
                optionData = new GroupLocationEnumData(CenterGroupLocation.RURAL.getValue().longValue(),
                        CenterGroupLocation.RURAL.getCode(), "Rural");
            break;
        }

        return optionData;
    }

    public static EnumOptionData groupLocationsOptionData(final int id) {
        return groupLocationsOptionData(CenterGroupLocation.fromInt(id));
    }

    public static EnumOptionData groupLocationsOptionData(final CenterGroupLocation status) {
        EnumOptionData optionData = null;
        switch (status) {
            case URBAN:
                optionData = new EnumOptionData(CenterGroupLocation.URBAN.getValue().longValue(), CenterGroupLocation.URBAN.getCode(),
                        "Urban");
            break;
            case RURAL:
                optionData = new EnumOptionData(CenterGroupLocation.RURAL.getValue().longValue(), CenterGroupLocation.RURAL.getCode(),
                        "Rural");
            break;
            default:
                optionData = new EnumOptionData(CenterGroupLocation.INVALID.getValue().longValue(), CenterGroupLocation.INVALID.getCode(),
                        "Not Set");
            break;
        }
        return optionData;
    }

}
