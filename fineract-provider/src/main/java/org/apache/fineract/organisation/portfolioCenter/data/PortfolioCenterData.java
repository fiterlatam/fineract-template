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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
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

    private final String address;

    private final String address2;

    private final CodeValueData city;

    private final CodeValueData state;

    private final CodeValueData country;

    private final Integer zone;

    private final CodeValueData location;

    private final PortfolioCenterStatusEnumData status;

    private final Integer distance;

    private final LocalDate effectiveDate;

    private final LocalDate firstMeetingDate;

    private final PortfolioCenterFrecuencyMeetingEnumData frequencyMeeting;

    private final Integer meetingStart;

    private final Integer meetingEnd;

    private final LocalDate nextMeetingDate;

    private final Integer meetingDay;

    // template
    private final Collection<OfficeData> parentOfficesOptions;
    private final Collection<AppUserData> responsibleUserOptions;
    private final Collection<CodeValueData> cityOptions;
    private final Collection<CodeValueData> stateOptions;
    private final Collection<CodeValueData> countryOptions;
    private final Collection<CodeValueData> labourDayOptions;

    public static PortfolioCenterData instance(Long id, String name, Long portfolioId, String portfolioName, BigDecimal legacyCenterNumber,
            String address, String address2, CodeValueData city, CodeValueData state, CodeValueData country, Integer zone,
            CodeValueData location, PortfolioCenterStatusEnumData status, Integer distance, LocalDate effectiveDate,
            LocalDate firstMeetingDate, PortfolioCenterFrecuencyMeetingEnumData frequencyMeeting, Integer meetingStart, Integer meetingEnd,
            LocalDate nextMeetingDate, Integer meetingDay) {
        return new PortfolioCenterData(id, name, portfolioId, portfolioName, legacyCenterNumber, address, address2, city, state, country,
                zone, location, status, distance, effectiveDate, firstMeetingDate, frequencyMeeting, meetingStart, meetingEnd,
                nextMeetingDate, meetingDay, null, null, null, null, null, null);
    }

    public PortfolioCenterData(Long id, String name, Long portfolioId, String portfolioName, BigDecimal legacyCenterNumber, String address,
            String address2, CodeValueData city, CodeValueData state, CodeValueData country, Integer zone, CodeValueData location,
            PortfolioCenterStatusEnumData status, Integer distance, LocalDate effectiveDate, LocalDate firstMeetingDate,
            PortfolioCenterFrecuencyMeetingEnumData frequencyMeeting, Integer meetingStart, Integer meetingEnd, LocalDate nextMeetingDate,
            Integer meetingDay, Collection<OfficeData> parentOfficesOptions, Collection<AppUserData> responsibleUserOptions,

            Collection<CodeValueData> cityOptions, Collection<CodeValueData> stateOptions, Collection<CodeValueData> countryOptions,
            Collection<CodeValueData> labourDayOptions) {
        this.id = id;
        this.name = name;
        this.portfolioId = portfolioId;
        this.portfolioName = portfolioName;
        this.legacyCenterNumber = legacyCenterNumber;
        this.address = address;
        this.address2 = address2;
        this.city = city;
        this.state = state;
        this.country = country;
        this.zone = zone;
        this.location = location;
        this.status = status;
        this.distance = distance;
        this.effectiveDate = effectiveDate;
        this.firstMeetingDate = firstMeetingDate;
        this.frequencyMeeting = frequencyMeeting;
        this.meetingStart = meetingStart;
        this.meetingEnd = meetingEnd;
        this.nextMeetingDate = nextMeetingDate;
        this.meetingDay = meetingDay;
        this.parentOfficesOptions = parentOfficesOptions;
        this.responsibleUserOptions = responsibleUserOptions;
        this.cityOptions = cityOptions;
        this.stateOptions = stateOptions;
        this.countryOptions = countryOptions;
        this.labourDayOptions = labourDayOptions;
    }

    public static PortfolioCenterData template(Collection<OfficeData> parentOfficesOptions, List<AppUserData> appUsers,
            Collection<CodeValueData> cityOptions, Collection<CodeValueData> stateOptions, Collection<CodeValueData> countryOptions,
            List<CodeValueData> labourDayOptions) {
        return new PortfolioCenterData(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, parentOfficesOptions, appUsers, cityOptions, stateOptions, countryOptions, labourDayOptions);
    }
}
