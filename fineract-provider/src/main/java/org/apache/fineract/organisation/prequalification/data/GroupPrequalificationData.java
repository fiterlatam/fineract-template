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
package org.apache.fineract.organisation.prequalification.data;

import java.time.LocalDate;
import java.util.Collection;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.agency.data.AgencyData;
import org.apache.fineract.portfolio.group.data.CenterData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.useradministration.data.AppUserData;

/**
 * Immutable data object represent client identity data.
 */
public class GroupPrequalificationData {

    private final Long id;
    private final String productName;
    private final String prequalificationNumber;
    private final String groupName;
    private final String portforlioName;
    private final String centerName;
    private final String agencyName;
    private final EnumOptionData status;
    private final String addedBy;
    private final String comments;
    private final LocalDate createdAt;
    private final Collection<AgencyData> agencies;
    private final Collection<CenterData> centerData;
    private final Collection<LoanProductData> loanProducts;
    private final Collection<AppUserData> facilitators;
    private Collection<MemberPrequalificationData> groupMembers;

    public GroupPrequalificationData(final Long id, final String productName, final String prequalificationNumber, final String agencyName,
            final String portforlioName, final String centerName, final String groupName, final String addedBy, final LocalDate createdAt,
            final EnumOptionData status, String comments, final Collection<MemberPrequalificationData> groupMembers,
            final Collection<AgencyData> agencies, Collection<CenterData> centerData, Collection<LoanProductData> loanProducts,
            Collection<AppUserData> appUsers) {
        this.id = id;
        this.productName = productName;
        this.prequalificationNumber = prequalificationNumber;
        this.agencyName = agencyName;
        this.groupName = groupName;
        this.portforlioName = portforlioName;
        this.centerName = centerName;
        this.status = status;
        this.addedBy = addedBy;
        this.createdAt = createdAt;
        this.agencies = agencies;
        this.centerData = centerData;
        this.loanProducts = loanProducts;
        this.facilitators = appUsers;
        this.groupMembers = groupMembers;
        this.comments = comments;
    }

    public static GroupPrequalificationData template(final Collection<AgencyData> agencies, Collection<CenterData> centerData,
            Collection<LoanProductData> loanProducts, Collection<AppUserData> appUsers) {
        return new GroupPrequalificationData(null, null, null, null, null, null, null, null, null, null, null, null, agencies, centerData,
                loanProducts, appUsers);
    }

    public static GroupPrequalificationData instance(Long id, String prequalificationNumber, EnumOptionData status, String agencyName,
            String portfolioName, String centerName, String groupName, String productName, String addedBy, LocalDate createdAt,
            String comments) {
        return new GroupPrequalificationData(id, productName, prequalificationNumber, agencyName, portfolioName, centerName, groupName,
                addedBy, createdAt, status, comments, null, null, null, null, null);
    }

    public void updateMembers(Collection<MemberPrequalificationData> groupMembers) {
        this.groupMembers = groupMembers;
    }
}
