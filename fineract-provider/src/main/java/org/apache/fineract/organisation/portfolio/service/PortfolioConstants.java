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
package org.apache.fineract.organisation.portfolio.service;

import java.util.HashSet;
import java.util.Set;

public class PortfolioConstants {

    private PortfolioConstants() {

    }

    public static final String PORTFOLIO_RESOURCE_NAME = "portfolio";

    public enum PortfolioSupportedParameters {

        PORTFOLIO_ID("id"), NAME("name"), OFFICE_PARENT_ID("parentId"), RESPONSIBLE_USER_ID("responsibleUserId"), LOCALE(
                "locale"), DATEFORMAT("dateFormat");

        private final String value;

        PortfolioSupportedParameters(final String value) {
            this.value = value;
        }

        private static final Set<String> values = new HashSet<>();

        static {
            for (final PortfolioSupportedParameters param : PortfolioSupportedParameters.values()) {
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
