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
package org.apache.fineract.organisation.portfolioCenter.service;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.portfolioCenter.data.PortfolioCenterFrecuencyMeetingEnumData;
import org.apache.fineract.organisation.portfolioCenter.data.PortfolioCenterStatusEnumData;
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenterFrecuencyMeeting;
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenterStatus;

public final class PortfolioCenterEnumerations {

    private PortfolioCenterEnumerations() {

    }

    public static PortfolioCenterFrecuencyMeetingEnumData type(final Integer frecuencyMeetingId) {
        return type(PortfolioCenterFrecuencyMeeting.fromInt(frecuencyMeetingId));
    }

    public static PortfolioCenterFrecuencyMeetingEnumData type(final PortfolioCenterFrecuencyMeeting frecuencyMeetingId) {
        PortfolioCenterFrecuencyMeetingEnumData optionData = new PortfolioCenterFrecuencyMeetingEnumData(
                PortfolioCenterFrecuencyMeeting.INVALID.getValue().longValue(), PortfolioCenterFrecuencyMeeting.INVALID.getCode(),
                "Invalid");
        switch (frecuencyMeetingId) {
            case INVALID:
                optionData = new PortfolioCenterFrecuencyMeetingEnumData(PortfolioCenterFrecuencyMeeting.INVALID.getValue().longValue(),
                        PortfolioCenterFrecuencyMeeting.INVALID.getCode(), "Invalid");
            break;
            case MENSUAL:
                optionData = new PortfolioCenterFrecuencyMeetingEnumData(PortfolioCenterFrecuencyMeeting.MENSUAL.getValue().longValue(),
                        PortfolioCenterFrecuencyMeeting.MENSUAL.getCode(), "Mensual");
            break;
        }

        return optionData;
    }

    public static PortfolioCenterStatusEnumData status(final Integer statusId) {
        return status(PortfolioCenterStatus.fromInt(statusId));
    }

    public static PortfolioCenterStatusEnumData status(final PortfolioCenterStatus statusId) {
        PortfolioCenterStatusEnumData optionData = new PortfolioCenterStatusEnumData(PortfolioCenterStatus.INVALID.getValue().longValue(),
                PortfolioCenterStatus.INVALID.getCode(), "Invalid");

        switch (statusId) {
            case INVALID:
                optionData = new PortfolioCenterStatusEnumData(PortfolioCenterStatus.INVALID.getValue().longValue(),
                        PortfolioCenterStatus.INVALID.getCode(), "Invalid");
            break;
            case ACTIVE:
                optionData = new PortfolioCenterStatusEnumData(PortfolioCenterStatus.ACTIVE.getValue().longValue(),
                        PortfolioCenterStatus.ACTIVE.getCode(), "Active");
            break;
            case INACTIVE:
                optionData = new PortfolioCenterStatusEnumData(PortfolioCenterStatus.INACTIVE.getValue().longValue(),
                        PortfolioCenterStatus.INACTIVE.getCode(), "Inactive");
            break;
        }

        return optionData;
    }

    public static EnumOptionData statusOptionData(final int id) {
        return statusOptionData(PortfolioCenterStatus.fromInt(id));
    }

    public static EnumOptionData statusOptionData(final PortfolioCenterStatus status) {
        EnumOptionData optionData = null;
        switch (status) {
            case ACTIVE:
                optionData = new EnumOptionData(PortfolioCenterStatus.ACTIVE.getValue().longValue(), PortfolioCenterStatus.ACTIVE.getCode(),
                        "Active");
            break;
            case INACTIVE:
                optionData = new EnumOptionData(PortfolioCenterStatus.INACTIVE.getValue().longValue(),
                        PortfolioCenterStatus.INACTIVE.getCode(), "Inactive");
            break;
            default:
                optionData = new EnumOptionData(PortfolioCenterStatus.INVALID.getValue().longValue(),
                        PortfolioCenterStatus.INVALID.getCode(), "Invalid");
            break;
        }
        return optionData;
    }

}
