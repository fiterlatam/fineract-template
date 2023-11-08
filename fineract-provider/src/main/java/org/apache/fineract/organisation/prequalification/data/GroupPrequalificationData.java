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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import lombok.Data;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.agency.data.AgencyData;
import org.apache.fineract.portfolio.group.data.CenterData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.useradministration.data.AppUserData;

/**
 * Immutable data object represent client identity data.
 */
@Data
public class GroupPrequalificationData {

    private final Long id;
    private final String productName;

    private final Long productId;
    private final String prequalificationNumber;
    private final String groupName;
    private final String portforlioName;
    private final String centerName;

    private final Long centerId;
    private final String agencyName;

    private final Long agencyId;
    private final EnumOptionData lastPrequalificationStatus;
    private final String statusChangedBy;
    private final LocalDate statusChangedOn;
    private final String processType;
    private final String processQuality;
    private EnumOptionData status;
    private final String addedBy;

    private final String facilitatorName;
    private final Long facilitatorId;
    private final String comments;
    private final Long groupId;
    private final LocalDate createdAt;
    private final Collection<AgencyData> agencies;
    private final Collection<CenterData> centerData;
    private final Collection<LoanProductData> loanProducts;
    private final Collection<AppUserData> facilitators;
    private Long greenValidationCount;
    private Long yellowValidationCount;
    private Long orangeValidationCount;
    private Long redValidationCount;
    private Long prequalilficationTimespan;
    private Collection<MemberPrequalificationData> groupMembers;
    private Collection<EnumOptionData> groupStatusOptions;

    private BigDecimal totalRequestedAmount;
    private BigDecimal totalApprovedAmount;

    public GroupPrequalificationData(final Long id, final String productName, final String prequalificationNumber, final String agencyName,
            final String portforlioName, final String centerName, final String groupName, final String addedBy, final LocalDate createdAt,
            final EnumOptionData status, String comments, Long groupId, final Collection<MemberPrequalificationData> groupMembers,
            final Collection<AgencyData> agencies, Collection<CenterData> centerData, Collection<LoanProductData> loanProducts,
            Collection<AppUserData> appUsers, Long prequalilficationTimespan, List<EnumOptionData> groupStatusOptions) {
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
        this.prequalilficationTimespan = prequalilficationTimespan;
        this.groupStatusOptions = groupStatusOptions;
        this.lastPrequalificationStatus = null;
        this.statusChangedBy = null;
        this.statusChangedOn = null;
        this.processType = null;
        this.processQuality = null;
        this.totalRequestedAmount = null;
        this.totalApprovedAmount = null;
    }

    public GroupPrequalificationData(final Long id, final String productName, final String prequalificationNumber, final String agencyName,
            final String portforlioName, final String centerName, final String groupName, final String addedBy, final LocalDate createdAt,
            final EnumOptionData status, String comments, Long groupId, final Collection<MemberPrequalificationData> groupMembers,
            final Collection<AgencyData> agencies, Collection<CenterData> centerData, Collection<LoanProductData> loanProducts,
            Collection<AppUserData> appUsers, final Long agencyId, final Long centerId, final Long productId, final Long facilitatorId,
            final String facilitatorName, Long greenValidationCount, Long yellowValidationCount, Long orangeValidationCount,
            Long redValidationCount, Long prequalilficationTimespan, EnumOptionData lastPrequalificationStatus, String statusChangedBy,
            LocalDate statusChangedOn, String processType, String processQuality, BigDecimal totalRequestedAmount,
            BigDecimal totalApprovedAmount) {
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
        this.agencyId = agencyId;
        this.centerId = centerId;
        this.productId = productId;
        this.facilitatorId = facilitatorId;
        this.facilitatorName = facilitatorName;
        this.greenValidationCount = greenValidationCount;
        this.yellowValidationCount = yellowValidationCount;
        this.orangeValidationCount = orangeValidationCount;
        this.redValidationCount = redValidationCount;
        this.prequalilficationTimespan = prequalilficationTimespan;
        this.lastPrequalificationStatus = lastPrequalificationStatus;
        this.statusChangedBy = statusChangedBy;
        this.statusChangedOn = statusChangedOn;
        this.processType = processType;
        this.processQuality = processQuality;
        this.totalRequestedAmount = totalRequestedAmount;
        this.totalApprovedAmount = totalApprovedAmount;
    }

    public static GroupPrequalificationData template(final Collection<AgencyData> agencies, Collection<CenterData> centerData,
            Collection<LoanProductData> loanProducts, Collection<AppUserData> appUsers, GlobalConfigurationPropertyData timespan,
            List<EnumOptionData> statusOptions) {

        Long prequalilficationTimespan = null;
        if (timespan != null) prequalilficationTimespan = timespan.getValue();
        return new GroupPrequalificationData(null, null, null, null, null, null, null, null, null, null, null, null, null, agencies,
                centerData, loanProducts, appUsers, prequalilficationTimespan, statusOptions);
    }

    public static GroupPrequalificationData instance(Long id, String prequalificationNumber, EnumOptionData status, String agencyName,
            String portfolioName, String centerName, String groupName, String productName, String addedBy, LocalDate createdAt,
            String comments, Long groupId, Long prequalilficationTimespan) {
        return new GroupPrequalificationData(id, productName, prequalificationNumber, agencyName, portfolioName, centerName, groupName,
                addedBy, createdAt, status, comments, groupId, null, null, null, null, null, prequalilficationTimespan, null);
    }

    public static GroupPrequalificationData instance(Long id, String prequalificationNumber, EnumOptionData status, String agencyName,
            String portfolioName, String centerName, String groupName, String productName, String addedBy, LocalDate createdAt,
            String comments, Long groupId, final Long agencyId, final Long centerId, final Long productId, final Long facilitatorId,
            final String facilitatorName, Long greenValidationCount, Long yellowValidationCount, Long orangeValidationCount,
            Long redValidationCount, Long prequalilficationTimespan, EnumOptionData lastPrequalificationStatus, String statusChangedBy,
            LocalDate statusChangedOn, String processType, String processQuality, BigDecimal totalRequestedAmount,
            BigDecimal totalApprovedAmount) {
        return new GroupPrequalificationData(id, productName, prequalificationNumber, agencyName, portfolioName, centerName, groupName,
                addedBy, createdAt, status, comments, groupId, null, null, null, null, null, agencyId, centerId, productId, facilitatorId,
                facilitatorName, greenValidationCount, yellowValidationCount, orangeValidationCount, redValidationCount,
                prequalilficationTimespan, lastPrequalificationStatus, statusChangedBy, statusChangedOn, processType, processQuality,
                totalRequestedAmount, totalApprovedAmount);
    }

    public void updateMembers(Collection<MemberPrequalificationData> groupMembers) {
        this.groupMembers = groupMembers;
    }
}
