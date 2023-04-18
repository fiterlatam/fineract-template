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
package org.apache.fineract.organisation.agency.data;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.useradministration.data.AppUserData;

/**
 * Immutable data object for agency data.
 */
public class AgencyData {

    private final Long id;

    private final String name;

    private final Long parentId;

    private final String parentName;

    private final String address;

    private final CodeValueData city;

    private final CodeValueData state;

    private final CodeValueData country;

    private final CodeValueData entityCode;

    private final CodeValueData agencyType;

    private CurrencyData currency;

    private final String phone;

    private final String telex;

    private final CodeValueData labourDayFrom;

    private final CodeValueData labourDayTo;

    private final LocalTime openHourMorning;

    private final LocalTime openHourAfternoon;

    private final CodeValueData financialYearFrom;

    private final CodeValueData financialYearTo;

    private final CodeValueData nonBusinessDay1;

    private final CodeValueData nonBusinessDay2;

    private final CodeValueData halfBusinessDay1;

    private final CodeValueData halfBusinessDay2;

    private final Long responsibleUserId;

    private final String responsibleUserName;

    // template
    private final Collection<CurrencyData> currencyOptions;
    private final Collection<CodeValueData> cityOptions;
    private final Collection<CodeValueData> stateOptions;
    private final Collection<CodeValueData> countryOptions;
    private final Collection<CodeValueData> agencyEntityCodesOptions;
    private final Collection<CodeValueData> agencyTypeOptions;
    private final Collection<OfficeData> parentOfficesOptions;
    private final Collection<CodeValueData> labourDayOptions;
    private final Collection<CodeValueData> financialMonthOptions;
    private final Collection<AppUserData> responsibleUserOptions;

    public static AgencyData instance(Long id, String name, Long parentId, String parentName, String address, CodeValueData city,
            CodeValueData state, CodeValueData country, CodeValueData entityCode, CurrencyData currency, CodeValueData agencyType,
            String phone, String telex, CodeValueData labourDayFrom, CodeValueData labourDayTo, LocalTime openHourMorning,
            LocalTime openHourAfternoon, Long responsibleUserId, String responsibleUserName, CodeValueData financialYearFrom,
            CodeValueData financialYearTo, CodeValueData nonBusinessDay1, CodeValueData nonBusinessDay2, CodeValueData halfBusinessDay1,
            CodeValueData halfBusinessDay2) {
        return new AgencyData(id, name, parentId, parentName, address, city, state, country, entityCode, currency, agencyType, phone, telex,
                labourDayFrom, labourDayTo, openHourMorning, openHourAfternoon, responsibleUserId, responsibleUserName, financialYearFrom,
                financialYearTo, nonBusinessDay1, nonBusinessDay2, halfBusinessDay1, halfBusinessDay2, null, null, null, null, null, null,
                null, null, null, null);
    }

    public AgencyData(Long id, String name, Long parentId, String parentName, String address, CodeValueData city, CodeValueData state,
            CodeValueData country, CodeValueData entityCode, CurrencyData currency, CodeValueData agencyType, String phone, String telex,
            CodeValueData labourDayFrom, CodeValueData labourDayTo, LocalTime openHourMorning, LocalTime openHourAfternoon,
            Long responsibleUserId, String responsibleUserName, CodeValueData financialYearFrom, CodeValueData financialYearTo,
            CodeValueData nonBusinessDay1, CodeValueData nonBusinessDay2, CodeValueData halfBusinessDay1, CodeValueData halfBusinessDay2,
            Collection<OfficeData> parentOfficesOptions, Collection<CurrencyData> currencyOptions, Collection<CodeValueData> cityOptions,
            Collection<CodeValueData> stateOptions, Collection<CodeValueData> countryOptions,
            Collection<CodeValueData> agencyEntityCodesOptions, Collection<CodeValueData> agencyTypeOptions,
            Collection<CodeValueData> labourDayOptions, Collection<CodeValueData> financialMonthOptions,
            Collection<AppUserData> responsibleUserOptions) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.parentName = parentName;
        this.address = address;
        this.city = city;
        this.state = state;
        this.country = country;
        this.entityCode = entityCode;
        this.currency = currency;
        this.agencyType = agencyType;
        this.phone = phone;
        this.telex = telex;
        this.labourDayFrom = labourDayFrom;
        this.labourDayTo = labourDayTo;
        this.openHourMorning = openHourMorning;
        this.openHourAfternoon = openHourAfternoon;
        this.financialYearFrom = financialYearFrom;
        this.financialYearTo = financialYearTo;
        this.nonBusinessDay1 = nonBusinessDay1;
        this.nonBusinessDay2 = nonBusinessDay2;
        this.halfBusinessDay1 = halfBusinessDay1;
        this.halfBusinessDay2 = halfBusinessDay2;
        this.responsibleUserId = responsibleUserId;
        this.responsibleUserName = responsibleUserName;
        // template
        this.parentOfficesOptions = parentOfficesOptions;
        this.currencyOptions = currencyOptions;
        this.cityOptions = cityOptions;
        this.stateOptions = stateOptions;
        this.countryOptions = countryOptions;
        this.agencyEntityCodesOptions = agencyEntityCodesOptions;
        this.agencyTypeOptions = agencyTypeOptions;
        this.labourDayOptions = labourDayOptions;
        this.financialMonthOptions = financialMonthOptions;
        this.responsibleUserOptions = responsibleUserOptions;
    }

    public static AgencyData template(Collection<OfficeData> parentOfficesOptions, Collection<CurrencyData> currencyOptions,
            Collection<CodeValueData> cityOptions, Collection<CodeValueData> stateOptions, Collection<CodeValueData> countryOptions,
            Collection<CodeValueData> agencyEntityCodesOptions, Collection<CodeValueData> agencyTypeOptions,
            List<CodeValueData> labourDayOptions, List<CodeValueData> financialMonthOptions,
            Collection<AppUserData> responsibleUserOptions) {
        return new AgencyData(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, parentOfficesOptions, currencyOptions, cityOptions, stateOptions, countryOptions,
                agencyEntityCodesOptions, agencyTypeOptions, labourDayOptions, financialMonthOptions, responsibleUserOptions);
    }
}
