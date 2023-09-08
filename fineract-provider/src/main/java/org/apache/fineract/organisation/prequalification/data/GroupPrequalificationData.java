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
<<<<<<< HEAD
=======
import lombok.Data;
>>>>>>> fiter/fb/dev
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.agency.data.AgencyData;
import org.apache.fineract.portfolio.group.data.CenterData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.useradministration.data.AppUserData;

/**
 * Immutable data object represent client identity data.
 */
<<<<<<< HEAD
=======
@Data
>>>>>>> fiter/fb/dev
public class GroupPrequalificationData {

    private final Long id;
    private final String productName;
<<<<<<< HEAD
=======

    private final Long productId;
>>>>>>> fiter/fb/dev
    private final String prequalificationNumber;
    private final String groupName;
    private final String portforlioName;
    private final String centerName;
<<<<<<< HEAD
    private final String agencyName;
    private final EnumOptionData status;
    private final String addedBy;
=======

    private final Long centerId;
    private final String agencyName;

    private final Long agencyId;
    private EnumOptionData status;
    private final String addedBy;

    private final String facilitatorName;
    private final Long facilitatorId;
>>>>>>> fiter/fb/dev
    private final String comments;
    private final Long groupId;
    private final LocalDate createdAt;
    private final Collection<AgencyData> agencies;
    private final Collection<CenterData> centerData;
    private final Collection<LoanProductData> loanProducts;
    private final Collection<AppUserData> facilitators;
<<<<<<< HEAD
    private Collection<MemberPrequalificationData> groupMembers;

    public GroupPrequalificationData(final Long id, final String productName, final String prequalificationNumber, final String agencyName,
                                     final String portforlioName, final String centerName, final String groupName, final String addedBy, final LocalDate createdAt,
                                     final EnumOptionData status, String comments, Long groupId, final Collection<MemberPrequalificationData> groupMembers,
                                     final Collection<AgencyData> agencies, Collection<CenterData> centerData, Collection<LoanProductData> loanProducts,
                                     Collection<AppUserData> appUsers) {
=======
    private Long greenValidationCount;
    private Long yellowValidationCount;
    private Long orangeValidationCount;
    private Long redValidationCount;
    private Collection<MemberPrequalificationData> groupMembers;

    public GroupPrequalificationData(final Long id, final String productName, final String prequalificationNumber, final String agencyName,
            final String portforlioName, final String centerName, final String groupName, final String addedBy, final LocalDate createdAt,
            final EnumOptionData status, String comments, Long groupId, final Collection<MemberPrequalificationData> groupMembers,
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
        this.groupId = groupId;
        this.agencyId = null;
        this.centerId = null;
        this.productId = null;
        this.facilitatorId = null;
        this.facilitatorName = null;
    }

    public GroupPrequalificationData(final Long id, final String productName, final String prequalificationNumber, final String agencyName,
            final String portforlioName, final String centerName, final String groupName, final String addedBy, final LocalDate createdAt,
            final EnumOptionData status, String comments, Long groupId, final Collection<MemberPrequalificationData> groupMembers,
            final Collection<AgencyData> agencies, Collection<CenterData> centerData, Collection<LoanProductData> loanProducts,
            Collection<AppUserData> appUsers, final Long agencyId, final Long centerId, final Long productId, final Long facilitatorId,
            final String facilitatorName, Long greenValidationCount, Long yellowValidationCount, Long orangeValidationCount,
            Long redValidationCount) {
>>>>>>> fiter/fb/dev
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
        this.groupId = groupId;
<<<<<<< HEAD
    }

    public static GroupPrequalificationData template(final Collection<AgencyData> agencies, Collection<CenterData> centerData,
                                                     Collection<LoanProductData> loanProducts, Collection<AppUserData> appUsers) {
=======
        this.agencyId = agencyId;
        this.centerId = centerId;
        this.productId = productId;
        this.facilitatorId = facilitatorId;
        this.facilitatorName = facilitatorName;
        this.greenValidationCount = greenValidationCount;
        this.yellowValidationCount = yellowValidationCount;
        this.orangeValidationCount = orangeValidationCount;
        this.redValidationCount = redValidationCount;
    }

    public static GroupPrequalificationData template(final Collection<AgencyData> agencies, Collection<CenterData> centerData,
            Collection<LoanProductData> loanProducts, Collection<AppUserData> appUsers) {
>>>>>>> fiter/fb/dev
        return new GroupPrequalificationData(null, null, null, null, null, null, null, null, null, null, null, null, null, agencies,
                centerData, loanProducts, appUsers);
    }

    public static GroupPrequalificationData instance(Long id, String prequalificationNumber, EnumOptionData status, String agencyName,
<<<<<<< HEAD
                                                     String portfolioName, String centerName, String groupName, String productName, String addedBy, LocalDate createdAt,
                                                     String comments, Long groupId) {
=======
            String portfolioName, String centerName, String groupName, String productName, String addedBy, LocalDate createdAt,
            String comments, Long groupId) {
>>>>>>> fiter/fb/dev
        return new GroupPrequalificationData(id, productName, prequalificationNumber, agencyName, portfolioName, centerName, groupName,
                addedBy, createdAt, status, comments, groupId, null, null, null, null, null);
    }

<<<<<<< HEAD
=======
    public static GroupPrequalificationData instance(Long id, String prequalificationNumber, EnumOptionData status, String agencyName,
            String portfolioName, String centerName, String groupName, String productName, String addedBy, LocalDate createdAt,
            String comments, Long groupId, final Long agencyId, final Long centerId, final Long productId, final Long facilitatorId,
            final String facilitatorName, Long greenValidationCount, Long yellowValidationCount, Long orangeValidationCount,
            Long redValidationCount) {
        return new GroupPrequalificationData(id, productName, prequalificationNumber, agencyName, portfolioName, centerName, groupName,
                addedBy, createdAt, status, comments, groupId, null, null, null, null, null, agencyId, centerId, productId, facilitatorId,
                facilitatorName, greenValidationCount, yellowValidationCount, orangeValidationCount, redValidationCount);
    }

>>>>>>> fiter/fb/dev
    public void updateMembers(Collection<MemberPrequalificationData> groupMembers) {
        this.groupMembers = groupMembers;
    }
}
