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
package org.apache.fineract.organisation.agency.domain;

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
import org.apache.fineract.organisation.agency.service.AgencyConstants.AgencySupportedParameters;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OrganisationCurrency;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "m_agency")
public class Agency extends AbstractAuditableCustom {

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_office_id")
    private Office parentOffice;

    @Column(name = "address", nullable = false, length = 250)
    private String address;

    @ManyToOne
    @JoinColumn(name = "city_id")
    private CodeValue city;

    @ManyToOne
    @JoinColumn(name = "state_province_id")
    private CodeValue stateProvince;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private CodeValue country;

    @ManyToOne
    @JoinColumn(name = "entity_code")
    private CodeValue entityCode;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @ManyToOne
    @JoinColumn(name = "agency_type")
    private CodeValue agencyType;

    @Column(name = "phone_number", length = 20)
    private String phone;

    @Column(name = "telex_number", length = 50)
    private String telex;

    @ManyToOne
    @JoinColumn(name = "labour_day_from")
    private CodeValue labourDayFrom;

    @ManyToOne
    @JoinColumn(name = "labour_day_to")
    private CodeValue labourDayTo;

    @Column(name = "open_hour_morning", nullable = true)
    private LocalTime openHourMorning;

    @Column(name = "open_hour_afternoon", nullable = true)
    private LocalTime openHourAfternoon;

    @ManyToOne
    @JoinColumn(name = "financial_year_from")
    private CodeValue financialYearFrom;

    @ManyToOne
    @JoinColumn(name = "financial_year_to")
    private CodeValue financialYearTo;

    @ManyToOne
    @JoinColumn(name = "non_business_day_1")
    private CodeValue nonBusinessDay1;

    @ManyToOne
    @JoinColumn(name = "non_business_day_2")
    private CodeValue nonBusinessDay2;

    @ManyToOne
    @JoinColumn(name = "half_business_day_1")
    private CodeValue halfBusinessDay1;

    @ManyToOne
    @JoinColumn(name = "half_business_day_2")
    private CodeValue halfBusinessDay2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private AppUser responsibleUser;

    protected Agency() {

    }

    public Agency(Office parentOffice, String name, String address, String phone, String telex, CodeValue city, CodeValue stateProvince,
            CodeValue country, CodeValue entityCode, String currencyCode, CodeValue agencyType, CodeValue labourDayFrom,
            CodeValue labourDayTo, CodeValue financialYearFrom, CodeValue financialYearTo, CodeValue nonBusinessDay1,
            CodeValue nonBusinessDay2, CodeValue halfBusinessDay1, CodeValue halfBusinessDay2, AppUser responsibleUser) {
        this.parentOffice = parentOffice;
        this.name = StringUtils.defaultIfEmpty(name, null);
        this.address = StringUtils.defaultIfEmpty(address, null);
        this.phone = StringUtils.defaultIfEmpty(phone, null);
        this.telex = StringUtils.defaultIfEmpty(telex, null);
        this.city = city;
        this.stateProvince = stateProvince;
        this.country = country;
        this.entityCode = entityCode;
        this.currencyCode = currencyCode;
        this.agencyType = agencyType;
        this.labourDayFrom = labourDayFrom;
        this.labourDayTo = labourDayTo;
        this.financialYearFrom = financialYearFrom;
        this.financialYearTo = financialYearTo;
        this.nonBusinessDay1 = nonBusinessDay1;
        this.nonBusinessDay2 = nonBusinessDay2;
        this.halfBusinessDay1 = halfBusinessDay1;
        this.halfBusinessDay2 = halfBusinessDay2;
        this.responsibleUser = responsibleUser;
    }

    public static Agency fromJson(Office parentOffice, CodeValue city, CodeValue stateProvince, CodeValue country, CodeValue entityCode,
            OrganisationCurrency currency, CodeValue agencyType, CodeValue labourDayFrom, CodeValue labourDayTo,
            CodeValue financialYearFrom, CodeValue financialYearTo, CodeValue nonBusinessDay1, CodeValue nonBusinessDay2,
            CodeValue halfBusinessDay1, CodeValue halfBusinessDay2, AppUser responsibleUser, JsonCommand command) {
        final String name = command.stringValueOfParameterNamed(AgencySupportedParameters.NAME.getValue());
        final String address = command.stringValueOfParameterNamed(AgencySupportedParameters.ADDRESS.getValue());
        final String phone = command.stringValueOfParameterNamed(AgencySupportedParameters.PHONE.getValue());
        final String telex = command.stringValueOfParameterNamed(AgencySupportedParameters.TELEX.getValue());

        return new Agency(parentOffice, name, address, phone, telex, city, stateProvince, country, entityCode, currency.getCode(),
                agencyType, labourDayFrom, labourDayTo, financialYearFrom, financialYearTo, nonBusinessDay1, nonBusinessDay2,
                halfBusinessDay1, halfBusinessDay2, responsibleUser);
    }

    public Map<String, Object> update(JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(2);

        if (command.isChangeInStringParameterNamed(AgencySupportedParameters.NAME.getValue(), this.name)) {
            final String newValue = command.stringValueOfParameterNamed(AgencySupportedParameters.NAME.getValue());
            actualChanges.put(AgencySupportedParameters.NAME.getValue(), newValue);
            this.name = newValue;
        }

        if (command.isChangeInStringParameterNamed(AgencySupportedParameters.ADDRESS.getValue(), this.address)) {
            final String newValue = command.stringValueOfParameterNamed(AgencySupportedParameters.ADDRESS.getValue());
            actualChanges.put(AgencySupportedParameters.ADDRESS.getValue(), newValue);
            this.address = newValue;
        }

        if (command.isChangeInStringParameterNamed(AgencySupportedParameters.PHONE.getValue(), this.phone)) {
            final String newValue = command.stringValueOfParameterNamed(AgencySupportedParameters.PHONE.getValue());
            actualChanges.put(AgencySupportedParameters.PHONE.getValue(), newValue);
            this.phone = newValue;
        }

        if (command.isChangeInStringParameterNamed(AgencySupportedParameters.TELEX.getValue(), this.telex)) {
            final String newValue = command.stringValueOfParameterNamed(AgencySupportedParameters.TELEX.getValue());
            actualChanges.put(AgencySupportedParameters.TELEX.getValue(), newValue);
            this.telex = newValue;
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
        String morningTime = command.stringValueOfParameterNamed(AgencySupportedParameters.OPEN_HOUR_MORNING.getValue());
        if (StringUtils.isNotBlank(morningTime)) {
            LocalTime newOpenHourMorning = LocalTime.parse(morningTime, dateTimeFormatter);
            this.openHourMorning = newOpenHourMorning;
        }

        String afternoonTime = command.stringValueOfParameterNamed(AgencySupportedParameters.OPEN_HOUR_AFTERNOON.getValue());
        if (StringUtils.isNotBlank(afternoonTime)) {
            LocalTime newOpenHourAfternoon = LocalTime.parse(afternoonTime, dateTimeFormatter);
            this.openHourAfternoon = newOpenHourAfternoon;
        }

        return actualChanges;
    }

    public String getName() {
        return name;
    }

    public CodeValue getCity() {
        return city;
    }

    public Office getParentOffice() {
        return parentOffice;
    }

    public CodeValue getStateProvince() {
        return stateProvince;
    }

    public CodeValue getCountry() {
        return country;
    }

    public CodeValue getEntityCode() {
        return entityCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public CodeValue getAgencyType() {
        return agencyType;
    }

    public CodeValue getLabourDayFrom() {
        return labourDayFrom;
    }

    public CodeValue getLabourDayTo() {
        return labourDayTo;
    }

    public CodeValue getFinancialYearFrom() {
        return financialYearFrom;
    }

    public CodeValue getFinancialYearTo() {
        return financialYearTo;
    }

    public CodeValue getNonBusinessDay1() {
        return nonBusinessDay1;
    }

    public CodeValue getNonBusinessDay2() {
        return nonBusinessDay2;
    }

    public CodeValue getHalfBusinessDay1() {
        return halfBusinessDay1;
    }

    public CodeValue getHalfBusinessDay2() {
        return halfBusinessDay2;
    }

    public void setParentOffice(Office parentOffice) {
        this.parentOffice = parentOffice;
    }

    public void setCity(CodeValue city) {
        this.city = city;
    }

    public void setStateProvince(CodeValue stateProvince) {
        this.stateProvince = stateProvince;
    }

    public void setCountry(CodeValue country) {
        this.country = country;
    }

    public void setEntityCode(CodeValue entityCode) {
        this.entityCode = entityCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public void setAgencyType(CodeValue agencyType) {
        this.agencyType = agencyType;
    }

    public void setLabourDayFrom(CodeValue labourDayFrom) {
        this.labourDayFrom = labourDayFrom;
    }

    public void setLabourDayTo(CodeValue labourDayTo) {
        this.labourDayTo = labourDayTo;
    }

    public void setFinancialYearFrom(CodeValue financialYearFrom) {
        this.financialYearFrom = financialYearFrom;
    }

    public void setFinancialYearTo(CodeValue financialYearTo) {
        this.financialYearTo = financialYearTo;
    }

    public void setNonBusinessDay1(CodeValue nonBusinessDay1) {
        this.nonBusinessDay1 = nonBusinessDay1;
    }

    public void setNonBusinessDay2(CodeValue nonBusinessDay2) {
        this.nonBusinessDay2 = nonBusinessDay2;
    }

    public void setHalfBusinessDay1(CodeValue halfBusinessDay1) {
        this.halfBusinessDay1 = halfBusinessDay1;
    }

    public void setHalfBusinessDay2(CodeValue halfBusinessDay2) {
        this.halfBusinessDay2 = halfBusinessDay2;
    }

    public void setResponsibleUser(AppUser responsibleUser) {
        this.responsibleUser = responsibleUser;
    }
}
