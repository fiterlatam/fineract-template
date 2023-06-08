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
package org.apache.fineract.organisation.portfolioCenter.service;

import java.util.HashSet;
import java.util.Set;

public class PortfolioCenterConstants {

    private PortfolioCenterConstants() {

    }

    public static final String PORTFOLIO_CENTER_RESOURCE_NAME = "portfolio_center";

    // template
    public static final String MEETING_DAYS = "DiaReunion";

    public static final String PORTFOLIO_CENTER_TYPE = "TipoCentro";

    public static final String PORTFOLIO_CENTER_DEPARTMENTS = "Ldepartamento";

    public static final String PORTFOLIO_CENTER_MUNICIPALITIES = "Lmunicipio";

    public enum PortfolioCenterSupportedParameters {

        PORTFOLIO_CENTER_ID("id"), NAME("name"), PORTFOLIO_ID("portfolioId"), PORTFOLIO_NAME("portfolioName"), OFFICE_PARENT_ID(
                "parentId"), RESPONSIBLE_USER_ID("responsibleUserId"), CITY_ID("cityId"), STATE_ID("stateId"), CENTER_TYPE(
                        "centerTypeId"), LEGACY_CENTER_NUMBER("legacyCenterNumber"), DISTANCE("distance"), CREATED_DATE(
                                "createdDate"), STATUS_ID("statusId"), MEETING_START(
                                        "meetingStart"), MEETING_END("meetingEnd"), MEETING_DAY("meetingDay"), MEETING_START_TIME(
                                                "meetingStartTime"), MEETING_END_TIME("meetingEndTime"), REFERENCE_POINT(
                                                        "referencePoint"), LOCALE("locale"), DATEFORMAT("dateFormat");

        private final String value;

        PortfolioCenterSupportedParameters(final String value) {
            this.value = value;
        }

        private static final Set<String> values = new HashSet<>();

        static {
            for (final PortfolioCenterSupportedParameters param : PortfolioCenterSupportedParameters.values()) {
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
