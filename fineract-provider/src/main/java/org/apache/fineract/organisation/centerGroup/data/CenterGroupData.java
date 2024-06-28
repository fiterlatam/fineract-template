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
package org.apache.fineract.organisation.centerGroup.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.useradministration.data.AppUserData;

/**
 * Immutable data object for center group data.
 */
public class CenterGroupData {

    private final Long id;

    private final String name;

    private final Long portfolioCenterId;

    private final String portfolioCenterName;

    private final Long legacyGroupNumber;

    private final BigDecimal latitude;

    private final BigDecimal longitude;

    private final LocalDate formationDate;

    private final EnumOptionData status;

    private final Integer size;

    private final Long responsibleUserId;

    private final LocalDate createdDate;

    private final LocalTime meetingStartTime;

    private final LocalTime meetingEndTime;

    private final Collection<EnumOptionData> centerGroupLocations;

    // template
    private final Collection<OfficeData> parentOfficesOptions;
    private final Collection<AppUserData> responsibleUserOptions;
    private final Collection<EnumOptionData> statusOptions;
    private Collection<EnumOptionData> portfolioCenterOptions;
    private EnumOptionData grouplocation;

    public CenterGroupData(Long id, String name, Long portfolioCenterId, String portfolioCenterName, Long legacyGroupNumber,
            BigDecimal latitude, BigDecimal longitude, LocalDate formationDate, EnumOptionData status, Integer size, Long responsibleUserId,
            LocalDate createdDate, LocalTime meetingStartTime, LocalTime meetingEndTime, Collection<OfficeData> parentOfficesOptions,
            Collection<AppUserData> responsibleUserOptions, Collection<EnumOptionData> statusOptions,
            Collection<EnumOptionData> centerGroupLocations, EnumOptionData locationEnum) {
        this.id = id;
        this.name = name;
        this.portfolioCenterId = portfolioCenterId;
        this.portfolioCenterName = portfolioCenterName;
        this.legacyGroupNumber = legacyGroupNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.formationDate = formationDate;
        this.status = status;
        this.size = size;
        this.responsibleUserId = responsibleUserId;
        this.createdDate = createdDate;
        this.meetingStartTime = meetingStartTime;
        this.meetingEndTime = meetingEndTime;
        this.parentOfficesOptions = parentOfficesOptions;
        this.responsibleUserOptions = responsibleUserOptions;
        this.statusOptions = statusOptions;
        this.centerGroupLocations = centerGroupLocations;
        this.grouplocation = locationEnum;
    }

    public static CenterGroupData instance(Long id, String name, Long portfolioCenterId, String portfolioCenterName, Long legacyGroupNumber,
            BigDecimal latitude, BigDecimal longitude, LocalDate formationDate, EnumOptionData status, Integer size, Long responsibleUserId,
            LocalDate createdDate, LocalTime meetingStartTime, LocalTime meetingEndTime, EnumOptionData locationEnum) {
        return new CenterGroupData(id, name, portfolioCenterId, portfolioCenterName, legacyGroupNumber, latitude, longitude, formationDate,
                status, size, responsibleUserId, createdDate, meetingStartTime, meetingEndTime, null, null, null, null, locationEnum);
    }

    public static CenterGroupData template(Collection<OfficeData> parentOfficesOptions, Collection<AppUserData> appUsers,
            Collection<EnumOptionData> statusOptions, Collection<EnumOptionData> centerGroupLocations) {
        return new CenterGroupData(null, null, null, null, null, null, null, null, null, null, null, null, null, null, parentOfficesOptions,
                appUsers, statusOptions, centerGroupLocations, null);
    }

    public LocalTime getMeetingStartTime() {
        return meetingStartTime;
    }

    public LocalTime getMeetingEndTime() {
        return meetingEndTime;
    }

}
