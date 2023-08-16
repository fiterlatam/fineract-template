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
package org.apache.fineract.organisation.portfolioCenter.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.centerGroup.data.CenterGroupData;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.useradministration.data.AppUserData;

/**
 * Immutable data object for portfolio center data.
 */
public final class PortfolioCenterData {

    private final Long id;

    private final String name;

    private final Long portfolioId;

    private final String portfolioName;

    private final BigDecimal legacyCenterNumber;

    private final CodeValueData city;

    private final CodeValueData state;

    private final CodeValueData type;

    private final EnumOptionData status;

    private final Integer distance;

    private final LocalDate createdDate;

    private final Integer meetingStart;

    private final Integer meetingEnd;

    private final Integer meetingDay;

    private final String meetingDayName;

    private final String referencePoint;

    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    private final String meetingStartTime;

    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    private final String meetingEndTime;

    private Collection<CenterGroupData> groups;

    // template
    private final Collection<OfficeData> parentOfficesOptions;
    private final Collection<AppUserData> responsibleUserOptions;
    private final Collection<CodeValueData> cityOptions;
    private final Collection<CodeValueData> stateOptions;
    private final Collection<CodeValueData> typeOptions;
    private final Collection<EnumOptionData> statusOptions;
    private final Collection<CodeValueData> meetingDayOptions;
    private final EnumOptionData centerLocation;

    public PortfolioCenterData(Long id, String name, Long portfolioId, String portfolioName, BigDecimal legacyCenterNumber,
            CodeValueData city, CodeValueData state, CodeValueData type, EnumOptionData status, Integer distance, LocalDate createdDate,
            Integer meetingStart, Integer meetingEnd, Integer meetingDay, String meetingStartTime, String meetingEndTime,
            String meetingDayName, String referencePoint, Collection<OfficeData> parentOfficesOptions,
            Collection<AppUserData> responsibleUserOptions, Collection<CodeValueData> cityOptions, Collection<CodeValueData> stateOptions,
            Collection<CodeValueData> typeOptions, Collection<EnumOptionData> statusOptions, Collection<CodeValueData> meetingDayOptions,
            EnumOptionData centerLocation) {
        this.id = id;
        this.name = name;
        this.portfolioId = portfolioId;
        this.portfolioName = portfolioName;
        this.legacyCenterNumber = legacyCenterNumber;
        this.city = city;
        this.state = state;
        this.type = type;
        this.status = status;
        this.distance = distance;
        this.createdDate = createdDate;
        this.meetingStart = meetingStart;
        this.meetingEnd = meetingEnd;
        this.meetingDay = meetingDay;
        this.meetingDayName = meetingDayName;
        this.meetingStartTime = meetingStartTime;
        this.meetingEndTime = meetingEndTime;
        this.referencePoint = referencePoint;
        this.parentOfficesOptions = parentOfficesOptions;
        this.responsibleUserOptions = responsibleUserOptions;
        this.cityOptions = cityOptions;
        this.stateOptions = stateOptions;
        this.typeOptions = typeOptions;
        this.statusOptions = statusOptions;
        this.meetingDayOptions = meetingDayOptions;
        this.centerLocation = centerLocation;
    }

    public static PortfolioCenterData instance(Long id, String name, Long portfolioId, String portfolioName, BigDecimal legacyCenterNumber,
            CodeValueData city, CodeValueData state, CodeValueData type, EnumOptionData status, Integer distance, LocalDate createdDate,
            Integer meetingStart, Integer meetingEnd, Integer meetingDay, String meetingStartTime, String meetingEndTime,
            String meetingDayName, String referencePoint, EnumOptionData centerLocation) {
        return new PortfolioCenterData(id, name, portfolioId, portfolioName, legacyCenterNumber, city, state, type, status, distance,
                createdDate, meetingStart, meetingEnd, meetingDay, meetingStartTime, meetingEndTime, meetingDayName, referencePoint, null,
                null, null, null, null, null, null, centerLocation);
    }

    public static PortfolioCenterData template(Collection<OfficeData> parentOfficesOptions, List<AppUserData> appUsers,
            Collection<CodeValueData> cityOptions, Collection<CodeValueData> stateOptions, Collection<CodeValueData> typeOptions,
            Collection<EnumOptionData> statusOptions, Collection<CodeValueData> meetingDayOptions) {
        return new PortfolioCenterData(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, parentOfficesOptions, appUsers, cityOptions, stateOptions, typeOptions, statusOptions, meetingDayOptions, null);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setCenterGroups(Collection<CenterGroupData> groups) {
        this.groups = groups;
    }
}
