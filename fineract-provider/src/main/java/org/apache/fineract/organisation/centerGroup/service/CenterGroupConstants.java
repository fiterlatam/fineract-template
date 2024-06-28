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
package org.apache.fineract.organisation.centerGroup.service;

import java.util.HashSet;
import java.util.Set;

public class CenterGroupConstants {

    private CenterGroupConstants() {

    }

    public static final String CENTER_GROUP_RESOURCE_NAME = "center_group";

    // template

    public enum CenterGroupSupportedParameters {

        CENTER_GROUP_ID("id"), PORTFOLIO_CENTER_ID("portfolioCenterId"), NAME("name"), RESPONSIBLE_USER_ID(
                "responsibleUserId"), LEGACY_GROUP_NUMBER("legacyGroupNumber"), LATITUDE(
                        "latitude"), LONGITUDE("longitude"), FORMATION_DATE("formationDate"), SIZE("size"), CREATED_DATE(
                                "createdDate"), STATUS_ID("statusId"), MEETING_START_TIME("meetingStartTime"), MEETING_END_TIME(
                                        "meetingEndTime"), LOCALE("locale"), DESTINATION_PORTFOLIO_CENTER_ID(
                                                "newPortfolioCenterId"), CENTER_GROUP_LOCATION("grouplocation"), DATEFORMAT("dateFormat");

        private final String value;

        CenterGroupSupportedParameters(final String value) {
            this.value = value;
        }

        private static final Set<String> values = new HashSet<>();

        static {
            for (final CenterGroupSupportedParameters param : CenterGroupSupportedParameters.values()) {
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
