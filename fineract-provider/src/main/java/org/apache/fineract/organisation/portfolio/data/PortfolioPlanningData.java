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

/**
 * Immutable data object for portfolio planning data.
 */
public final class PortfolioPlanningData {

    private final Long id;

    private final String name;

    private final Long parentId;

    private final String parentName;

    private final Long responsibleUserId;

    private final Long agencyId;

    private final String agencyName;

    private final String responsibleUserName;

    private Collection<PortfolioDetailedPlanningData> detailedPlanningData;

    public static PortfolioPlanningData instance(Long id, String name, Long parentId, String parentName, Long responsibleUserId,
            Long agencyId, String agencyName, String responsibleUserName) {
        return new PortfolioPlanningData(id, name, parentId, parentName, responsibleUserId, agencyId, agencyName, responsibleUserName,
                null);
    }

    public PortfolioPlanningData(Long id, String name, Long parentId, String parentName, Long responsibleUserId, Long agencyId,
            String agencyName, String responsibleUserName, Collection<PortfolioDetailedPlanningData> detailedPlanningData) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.parentName = parentName;
        this.responsibleUserId = responsibleUserId;
        this.agencyId = agencyId;
        this.agencyName = agencyName;
        this.detailedPlanningData = detailedPlanningData;
        this.responsibleUserName = responsibleUserName;
    }

    public Long getId() {
        return id;
    }

    public void setDetailedPlanningData(Collection<PortfolioDetailedPlanningData> detailedPlanningData) {
        this.detailedPlanningData = detailedPlanningData;
    }
}
