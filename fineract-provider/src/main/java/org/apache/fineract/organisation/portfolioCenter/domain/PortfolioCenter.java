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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.organisation.portfolio.domain.Portfolio;
import org.apache.fineract.organisation.portfolioCenter.service.PortfolioCenterConstants;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private CodeValue city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_province_id")
    private CodeValue stateProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id")
    private CodeValue type;

    @Column(name = "center_status", nullable = false)
    private Integer status;

    @Column(name = "distance_from_agency")
    private Integer distance;

    @Column(name = "meeting_start_date")
    private Integer meetingStart;

    @Column(name = "meeting_end_date")
    private Integer meetingEnd;

    @Column(name = "meeting_day")
    private Integer meetingDay;

    @Column(name = "meeting_start_time")
    private LocalTime meetingStartTime;

    @Column(name = "meeting_end_time")
    private LocalTime meetingEndTime;

    @Column(name = "reference_point", nullable = false, length = 60)
    private String referencePoint;

    protected PortfolioCenter() {

    }

    public static PortfolioCenter assembleFrom(String name, Portfolio portfolio, Integer status, Integer meetingStart, Integer meetingEnd,
            Integer meetingDay, LocalTime meetingStartTime, LocalTime meetingEndTime) {
        return new PortfolioCenter(name, portfolio, null, null, null, null, status, null, meetingStart, meetingEnd, meetingDay,
                meetingStartTime, meetingEndTime, null);
    }

    public PortfolioCenter(String name, Portfolio portfolio, BigDecimal legacyCenterNumber, CodeValue city, CodeValue stateProvince,
            CodeValue type, Integer status, Integer distance, Integer meetingStart, Integer meetingEnd, Integer meetingDay,
            LocalTime meetingStartTime, LocalTime meetingEndTime, String referencePoint) {
        this.name = name;
        this.portfolio = portfolio;
        this.legacyCenterNumber = legacyCenterNumber;
        this.city = city;
        this.stateProvince = stateProvince;
        this.type = type;
        this.status = status;
        this.distance = distance;
        this.meetingStart = meetingStart;
        this.meetingEnd = meetingEnd;
        this.meetingDay = meetingDay;
        this.meetingStartTime = meetingStartTime;
        this.meetingEndTime = meetingEndTime;
        this.referencePoint = referencePoint;
    }

    public Map<String, Object> update(JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(2);

        if (command.isChangeInStringParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.NAME.getValue(),
                this.getName())) {
            final String newValue = command
                    .stringValueOfParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.NAME.getValue());
            actualChanges.put(PortfolioCenterConstants.PortfolioCenterSupportedParameters.NAME.getValue(), newValue);
            this.name = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamed(
                PortfolioCenterConstants.PortfolioCenterSupportedParameters.LEGACY_CENTER_NUMBER.getValue(), this.legacyCenterNumber)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(
                    PortfolioCenterConstants.PortfolioCenterSupportedParameters.LEGACY_CENTER_NUMBER.getValue());
            actualChanges.put(PortfolioCenterConstants.PortfolioCenterSupportedParameters.LEGACY_CENTER_NUMBER.getValue(), newValue);
            this.legacyCenterNumber = newValue;
        }

        if (command.isChangeInIntegerParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.STATUS_ID.getValue(),
                this.status)) {
            final Integer newValue = command
                    .integerValueOfParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.STATUS_ID.getValue());
            actualChanges.put(PortfolioCenterConstants.PortfolioCenterSupportedParameters.STATUS_ID.getValue(), newValue);
            this.status = newValue;
        }

        if (command.isChangeInIntegerParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.DISTANCE.getValue(),
                this.distance)) {
            final Integer newValue = command
                    .integerValueOfParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.DISTANCE.getValue());
            actualChanges.put(PortfolioCenterConstants.PortfolioCenterSupportedParameters.DISTANCE.getValue(), newValue);
            this.distance = newValue;
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
        String meetingStartTime = command
                .stringValueOfParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.MEETING_START_TIME.getValue());
        if (StringUtils.isNotBlank(meetingStartTime)) {
            LocalTime newMeetingStarTime = LocalTime.parse(meetingStartTime, dateTimeFormatter);
            this.meetingStartTime = newMeetingStarTime;
        }

        String meetingEndTime = command
                .stringValueOfParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.MEETING_END_TIME.getValue());
        if (StringUtils.isNotBlank(meetingEndTime)) {
            LocalTime newMeetingEndTime = LocalTime.parse(meetingEndTime, dateTimeFormatter);
            this.meetingEndTime = newMeetingEndTime;
        }

        if (command.isChangeInStringParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.REFERENCE_POINT.getValue(),
                this.referencePoint)) {
            final String newValue = command
                    .stringValueOfParameterNamed(PortfolioCenterConstants.PortfolioCenterSupportedParameters.REFERENCE_POINT.getValue());
            actualChanges.put(PortfolioCenterConstants.PortfolioCenterSupportedParameters.REFERENCE_POINT.getValue(), newValue);
            this.referencePoint = newValue;
        }

        return actualChanges;
    }

    public void setCity(CodeValue city) {
        this.city = city;
    }

    public void setStateProvince(CodeValue stateProvince) {
        this.stateProvince = stateProvince;
    }

    public void setType(CodeValue type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }


    public LocalTime getMeetingStartTime() {
        return meetingStartTime;
    }

    public void setMeetingStartTime(LocalTime meetingStartTime) {
        this.meetingStartTime = meetingStartTime;
    }

    public LocalTime getMeetingEndTime() {
        return meetingEndTime;
    }

    public void setMeetingEndTime(LocalTime meetingEndTime) {
        this.meetingEndTime = meetingEndTime;
    }
}
