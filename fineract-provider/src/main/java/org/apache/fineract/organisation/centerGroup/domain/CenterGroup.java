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
package org.apache.fineract.organisation.centerGroup.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.organisation.centerGroup.service.CenterGroupConstants;
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenter;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.stereotype.Component;

@Entity
@Component
@Table(name = "m_center_group")
public class CenterGroup extends AbstractAuditableCustom {

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_center_id")
    private PortfolioCenter portfolioCenter;

    @Column(name = "legacy_group_number")
    private Long legacyGroupNumber;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "formation_date", nullable = false)
    private LocalDate formationDate;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "size", nullable = false)
    private Integer size;

    @Column(name = "meeting_start_time")
    private LocalTime meetingStartTime;

    @Column(name = "meeting_end_time")
    private LocalTime meetingEndTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private AppUser responsibleUser;

    protected CenterGroup() {

    }

    public CenterGroup(String name, PortfolioCenter portfolioCenter, Long legacyGroupNumber, BigDecimal latitude, BigDecimal longitude,
                       LocalDate formationDate, Integer status, Integer size, LocalTime meetingStartTime, LocalTime meetingEndTime,
                       AppUser responsibleUser) {
        this.name = name;
        this.portfolioCenter = portfolioCenter;
        this.legacyGroupNumber = legacyGroupNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.formationDate = formationDate;
        this.status = status;
        this.size = size;
        this.meetingStartTime = meetingStartTime;
        this.meetingEndTime = meetingEndTime;
        this.responsibleUser = responsibleUser;
    }

    public static CenterGroup fromJson(PortfolioCenter portfolioCenter, AppUser responsibleUser, JsonCommand command,
                                       Integer meetingDefaultDuration, Integer timeBetweenMeetings) {
        final String name = command.stringValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.NAME.getValue());
        final Integer size = command.integerValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.SIZE.getValue());
        final LocalDate formationDate = command
                .localDateValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.FORMATION_DATE.getValue());
        final Integer status = command
                .integerValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.STATUS_ID.getValue());

        Long legacyGroupNumber = null;
        if (command.parameterExists(CenterGroupConstants.CenterGroupSupportedParameters.LEGACY_GROUP_NUMBER.getValue())) {
            legacyGroupNumber = command.longValueOfParameterNamed(
                    CenterGroupConstants.CenterGroupSupportedParameters.LEGACY_GROUP_NUMBER.LEGACY_GROUP_NUMBER.getValue());
        }

        BigDecimal latitude = null;
        if (command.parameterExists(CenterGroupConstants.CenterGroupSupportedParameters.LATITUDE.getValue())) {
            latitude = command.bigDecimalValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.LATITUDE.getValue());
        }

        BigDecimal longitude = null;
        if (command.parameterExists(CenterGroupConstants.CenterGroupSupportedParameters.LONGITUDE.getValue())) {
            longitude = command.bigDecimalValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.LONGITUDE.getValue());
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
        LocalTime meetingStartTime = null;
        LocalTime meetingEndTime = null;
        String meetingStartTimeAsString = command
                .stringValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.MEETING_START_TIME.getValue());
        if (StringUtils.isNotBlank(meetingStartTimeAsString)) {
            LocalTime newMeetingStarTime = LocalTime.parse(meetingStartTimeAsString, dateTimeFormatter);
            meetingStartTime = newMeetingStarTime;

            LocalTime newMeetingEndTime = newMeetingStarTime.plusMinutes(meetingDefaultDuration).plusMinutes(timeBetweenMeetings);
            meetingEndTime = newMeetingEndTime;
        }

        return new CenterGroup(name, portfolioCenter, legacyGroupNumber, latitude, longitude, formationDate, status, size, meetingStartTime,
                meetingEndTime, responsibleUser);
    }

    public Map<String, Object> update(JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(2);

        if (command.isChangeInStringParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.NAME.getValue(), this.getName())) {
            final String newValue = command
                    .stringValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.NAME.getValue());
            actualChanges.put(CenterGroupConstants.CenterGroupSupportedParameters.NAME.getValue(), newValue);
            this.name = newValue;
        }

        if (command.isChangeInIntegerParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.STATUS_ID.getValue(),
                this.status)) {
            final Integer newValue = command
                    .integerValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.STATUS_ID.getValue());
            actualChanges.put(CenterGroupConstants.CenterGroupSupportedParameters.STATUS_ID.getValue(), newValue);
            this.status = newValue;
        }

        if (command.isChangeInIntegerParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.SIZE.getValue(), this.size)) {
            final Integer newValue = command
                    .integerValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.SIZE.getValue());
            actualChanges.put(CenterGroupConstants.CenterGroupSupportedParameters.SIZE.getValue(), newValue);
            this.size = newValue;
        }

        if (command.isChangeInLocalDateParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.FORMATION_DATE.getValue(),
                this.formationDate)) {
            final LocalDate newValue = command
                    .localDateValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.FORMATION_DATE.getValue());
            actualChanges.put(CenterGroupConstants.CenterGroupSupportedParameters.FORMATION_DATE.getValue(), newValue);
            this.formationDate = newValue;
        }

        if (command.isChangeInLongParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.LEGACY_GROUP_NUMBER.getValue(),
                this.legacyGroupNumber)) {
            final Long newValue = command
                    .longValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.LEGACY_GROUP_NUMBER.getValue());
            actualChanges.put(CenterGroupConstants.CenterGroupSupportedParameters.LEGACY_GROUP_NUMBER.getValue(), newValue);
            this.legacyGroupNumber = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.LATITUDE.getValue(),
                this.latitude)) {
            final BigDecimal newValue = command
                    .bigDecimalValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.LATITUDE.getValue());
            actualChanges.put(CenterGroupConstants.CenterGroupSupportedParameters.LATITUDE.getValue(), newValue);
            this.latitude = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.LONGITUDE.getValue(),
                this.longitude)) {
            final BigDecimal newValue = command
                    .bigDecimalValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.LONGITUDE.getValue());
            actualChanges.put(CenterGroupConstants.CenterGroupSupportedParameters.LONGITUDE.getValue(), newValue);
            this.longitude = newValue;
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
        String meetingStartTime = command
                .stringValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.MEETING_START_TIME.getValue());
        if (StringUtils.isNotBlank(meetingStartTime)) {
            LocalTime newMeetingStarTime = LocalTime.parse(meetingStartTime, dateTimeFormatter);
            this.meetingStartTime = newMeetingStarTime;
        }

        String meetingEndTime = command
                .stringValueOfParameterNamed(CenterGroupConstants.CenterGroupSupportedParameters.MEETING_END_TIME.getValue());
        if (StringUtils.isNotBlank(meetingEndTime)) {
            LocalTime newMeetingEndTime = LocalTime.parse(meetingEndTime, dateTimeFormatter);
            this.meetingEndTime = newMeetingEndTime;
        }

        return actualChanges;
    }

    public void setPortfolioCenter(PortfolioCenter portfolioCenter) {
        this.portfolioCenter = portfolioCenter;
    }

    public void setMeetingStartTime(LocalTime meetingStartTime) {
        this.meetingStartTime = meetingStartTime;
    }

    public void setMeetingEndTime(LocalTime meetingEndTime) {
        this.meetingEndTime = meetingEndTime;
    }

    public String getName() {
        return name;
    }

    public LocalTime getMeetingStartTime() {
        return meetingStartTime;
    }

    public LocalTime getMeetingEndTime() {
        return meetingEndTime;
    }

    public PortfolioCenter getPortfolioCenter() {
        return portfolioCenter;
    }
}
