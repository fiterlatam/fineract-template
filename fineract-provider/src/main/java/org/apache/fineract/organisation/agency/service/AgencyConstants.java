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
package org.apache.fineract.organisation.agency.service;

import java.util.HashSet;
import java.util.Set;

public final class AgencyConstants {

    private AgencyConstants() {

    }

    public static final String AGENCY_RESOURCE_NAME = "agency";

    // template
    public static final String AGENCY_DEPARTMENTS = "Ldepartamento";

    public static final String AGENCY_MUNICIPALITIES = "Lmunicipio";

    public static final String AGENCY_COUNTRIES = "Lpaises";

    public static final String AGENCY_ENTITY_CODE = "CódigoEntidad";

    public static final String AGENCY_TYPE = "TipoAgencia";

    public static final String LABOUR_DAYS = "LabourDay";

    public static final String FINANCIAL_MONTHS = "FinancialYearMonths";

    public enum AgencySupportedParameters {

        AGENCY_ID("id"), NAME("name"), OFFICE_PARENT_ID("parentId"), ADDRESS("address"), CITY_ID("cityId"), STATE_ID("stateId"), COUNTRY_ID(
                "countryId"), ENTITY_CODE("entityCode"), CURRENCY_CODE("currencyCode"), AGENCY_TYPE("agencyType"), PHONE("phone"), TELEX(
                        "telex"), LABOUR_DAY_FROM("labourDayFrom"), LABOUR_DAY_TO("labourDayTo"), OPEN_HOUR_MORNING(
                                "openHourMorning"), OPEN_HOUR_AFTERNOON("openHourAfternoon"), FINANCIAL_YEAR_FROM(
                                        "financialYearFrom"), FINANCIAL_YEAR_TO("financialYearTo"), NON_BUSINESS_DAY1(
                                                "nonBusinessDay1"), NON_BUSINESS_DAY2("nonBusinessDay2"), HALF_BUSINESS_DAY1(
                                                        "halfBusinessDay1"), HALF_BUSINESS_DAY2("halfBusinessDay2"), RESPONSIBLE_USER_ID(
                                                                "responsibleUserId"), LOCALE("locale"), DATEFORMAT("dateFormat");

        private final String value;

        AgencySupportedParameters(final String value) {
            this.value = value;
        }

        private static final Set<String> values = new HashSet<>();

        static {
            for (final AgencyConstants.AgencySupportedParameters param : AgencyConstants.AgencySupportedParameters.values()) {
                values.add(param.value);
            }
        }

        public static Set<String> getAllValues() {
            return values;
        }

        @Override
        public String toString() {
            return name().replaceAll("_", " ");
        }

        public String getValue() {
            return this.value;
        }
    }
}
