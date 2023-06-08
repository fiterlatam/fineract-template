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
package org.apache.fineract.organisation.portfolio.data;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Immutable data object for portfolio detailed planning data.
 */
public class PortfolioDetailedPlanningData {

    private final Long centerGroupId;

    private final String centerGroupName;

    private final BigDecimal legacyGroupNumber;

    private final LocalTime meetingStartTime;

    private final LocalTime meetingEndTime;

    private final Long portfolioCenterId;

    private final String portfolioCenterName;

    private final BigDecimal legacyCenterNumber;

    private final String meetingDayName;

    public PortfolioDetailedPlanningData(Long centerGroupId, String centerGroupName, BigDecimal legacyGroupNumber,
            LocalTime meetingStartTime, LocalTime meetingEndTime, Long portfolioCenterId, String portfolioCenterName,
            BigDecimal legacyCenterNumber, String meetingDayName) {
        this.centerGroupId = centerGroupId;
        this.centerGroupName = centerGroupName;
        this.legacyGroupNumber = legacyGroupNumber;
        this.meetingStartTime = meetingStartTime;
        this.meetingEndTime = meetingEndTime;
        this.portfolioCenterId = portfolioCenterId;
        this.portfolioCenterName = portfolioCenterName;
        this.legacyCenterNumber = legacyCenterNumber;
        this.meetingDayName = meetingDayName;
    }

    public static PortfolioDetailedPlanningData instance(Long centerGroupId, String centerGroupName, BigDecimal legacyGroupNumber,
            LocalTime meetingStartTime, LocalTime meetingEndTime, Long portfolioCenterId, String portfolioCenterName,
            BigDecimal legacyCenterNumber, String meetingDayName) {
        return new PortfolioDetailedPlanningData(centerGroupId, centerGroupName, legacyGroupNumber, meetingStartTime, meetingEndTime,
                portfolioCenterId, portfolioCenterName, legacyCenterNumber, meetingDayName);
    }
}
