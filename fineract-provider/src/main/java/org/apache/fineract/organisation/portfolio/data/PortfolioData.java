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
package org.apache.fineract.organisation.portfolio.data;

import java.util.Collection;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.portfolioCenter.data.PortfolioCenterData;
import org.apache.fineract.organisation.supervision.data.SupervisionData;
import org.apache.fineract.useradministration.data.AppUserData;

/**
 * Immutable data object for portfolio data.
 */
public final class PortfolioData {

    private final Long id;

    private final String name;

    private final Long parentId;

    private final String parentName;

    private final Long responsibleUserId;

    private final String responsibleUserName;
    private final String supervisionName;
    private Long supervisionId;

    private Collection<PortfolioCenterData> centers;

    // template
    private final Collection<OfficeData> parentOfficesOptions;
    private final Collection<AppUserData> responsibleUserOptions;
    private final Collection<SupervisionData> supervisionOptions;

    public static PortfolioData instance(Long id, String name, Long parentId, String parentName, Long responsibleUserId,
            String responsibleUserName, final String supervisionName) {
        return new PortfolioData(id, name, parentId, parentName, responsibleUserId, responsibleUserName, null, null, null, null,
                supervisionName);
    }

    public PortfolioData(Long id, String name, Long parentId, String parentName, Long responsibleUserId, String responsibleUserName,
            Collection<PortfolioCenterData> centers, Collection<OfficeData> parentOfficesOptions,
            Collection<AppUserData> responsibleUserOptions, final Collection<SupervisionData> supervisionOptions,
            final String supervisionName) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.parentName = parentName;
        this.responsibleUserId = responsibleUserId;
        this.responsibleUserName = responsibleUserName;
        this.centers = centers;
        // template
        this.parentOfficesOptions = parentOfficesOptions;
        this.responsibleUserOptions = responsibleUserOptions;
        this.supervisionOptions = supervisionOptions;
        this.supervisionName = supervisionName;
    }

    public static PortfolioData template(Collection<OfficeData> parentOfficesOptions, Collection<AppUserData> responsibleUserOptions,
            final Collection<SupervisionData> supervisionOptions) {
        return new PortfolioData(null, null, null, null, null, null, null, parentOfficesOptions, responsibleUserOptions, supervisionOptions,
                null);
    }

    public void setCenters(Collection<PortfolioCenterData> centers) {
        this.centers = centers;
    }

    public Long getSupervisionId() {
        return supervisionId;
    }

    public void setSupervisionId(Long supervisionId) {
        this.supervisionId = supervisionId;
    }
}
