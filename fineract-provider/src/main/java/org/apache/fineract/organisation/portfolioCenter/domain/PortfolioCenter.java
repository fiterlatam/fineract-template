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

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.organisation.portfolio.domain.Portfolio;
import org.springframework.stereotype.Component;

@Entity
@Component
@Table(name = "m_portfolio_center")
public class PortfolioCenter extends AbstractAuditableCustom {

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Column(name = "legacy_center_number")
    private BigDecimal legacyCenterNumber;

    @Column(name = "address", nullable = false, length = 150)
    private String address;

    @Column(name = "address2", nullable = false, length = 150)
    private String address2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private CodeValue city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_province_id")
    private CodeValue stateProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private CodeValue country;

    @JoinColumn(name = "zone")
    private Integer zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private CodeValue location;

    @Column(name = "center_status", nullable = false)
    private Integer status;

    @Column(name = "distance_from_agency")
    private Integer distance;

    @Column(name = "facilitator_effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "first_meeting_date", nullable = false)
    private LocalDate firstMeetingDate;

    @Column(name = "frequency_meeting", nullable = false)
    private Integer frequencyMeeting;

    @Column(name = "meeting_start_date", nullable = false)
    private Integer meetingStart;

    @Column(name = "meeting_end_date", nullable = false)
    private Integer meetingEnd;

    @Column(name = "next_meeting_date", nullable = false)
    private LocalDate nextMeetingDate;

    @Column(name = "meeting_day", nullable = false)
    private Integer meetingDay;

    protected PortfolioCenter() {

    }

    public static PortfolioCenter assembleFrom(String name, Portfolio portfolio, Integer status, LocalDate effectiveDate,
            LocalDate firstMeetingDate, Integer frequencyMeeting, Integer meetingStart, Integer meetingEnd, LocalDate nextMeetingDate,
            Integer meetingDay) {
        return new PortfolioCenter(name, portfolio, null, null, null, null, null, null, null, null, status, null, effectiveDate,
                firstMeetingDate, frequencyMeeting, meetingStart, meetingEnd, nextMeetingDate, meetingDay);
    }

    public PortfolioCenter(String name, Portfolio portfolio, BigDecimal legacyCenterNumber, String address, String address2, CodeValue city,
            CodeValue stateProvince, CodeValue country, Integer zone, CodeValue location, Integer status, Integer distance,
            LocalDate effectiveDate, LocalDate firstMeetingDate, Integer frequencyMeeting, Integer meetingStart, Integer meetingEnd,
            LocalDate nextMeetingDate, Integer meetingDay) {
        this.name = name;
        this.portfolio = portfolio;
        this.legacyCenterNumber = legacyCenterNumber;
        this.address = address;
        this.address2 = address2;
        this.city = city;
        this.stateProvince = stateProvince;
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
    }

}
