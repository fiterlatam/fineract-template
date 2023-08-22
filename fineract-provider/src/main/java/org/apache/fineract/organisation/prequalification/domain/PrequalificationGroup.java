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
package org.apache.fineract.organisation.prequalification.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.agency.domain.Agency;
import org.apache.fineract.organisation.prequalification.command.PrequalificatoinApiConstants;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "m_prequalification_group")
public class PrequalificationGroup extends AbstractPersistableCustom {

    @Column(name = "prequalification_number", nullable = false)
    private String prequalificationNumber;

    @ManyToOne
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = true)
    private Group group;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct loanProduct;

    @ManyToOne
    @JoinColumn(name = "added_by", nullable = false)
    private AppUser addedBy;

    @ManyToOne
    @JoinColumn(name = "facilitator", nullable = false)
    private AppUser facilitator;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "comments", nullable = false)
    private String comments;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "prequalificationGroup", cascade = CascadeType.ALL)
    private List<PrequalificationGroupMember> members;

    public static PrequalificationGroup fromJson(final AppUser appUser, final AppUser facilitator, final Agency agency, final Group group,
            final LoanProduct loanProduct, final JsonCommand command) {
        String groupName = command.stringValueOfParameterNamed("groupName");
        Long center = command.longValueOfParameterNamed(PrequalificatoinApiConstants.centerIdParamName);
        if (group != null) {
            groupName = group.getName();
        }
        return new PrequalificationGroup(appUser, facilitator, agency, group, groupName, center, loanProduct);
    }

    protected PrequalificationGroup() {
        //
    }

    private PrequalificationGroup(final AppUser appUser, final AppUser facilitator, final Agency agency, final Group group,
            final String groupName, Long center, final LoanProduct loanProduct) {
        this.addedBy = appUser;
        this.facilitator = facilitator;
        this.status = PrequalificationStatus.PENDING.getValue();
        this.loanProduct = loanProduct;
        this.group = group;
        this.groupName = groupName;
        this.centerId = center;
        this.agency = agency;
        this.createdAt = DateUtils.getLocalDateTimeOfTenant();
    }

    public void updateStatus(final PrequalificationStatus prequalificationStatus) {
        ;
        this.status = prequalificationStatus.getValue();
    }

    public void updateMembers(final List<PrequalificationGroupMember> members) {
        ;
        this.members = members;
    }

    public List<PrequalificationGroupMember> getMembers() {
        return members;
    }

    public void updatePrequalificationNumber(final String prequalificationNumber) {
        ;
        this.prequalificationNumber = prequalificationNumber;
    }

    public void updateAgency(final Agency agency){
        this.agency = agency;
    }

    public void updateCenter(final Long centerId){
        this.centerId = centerId;
    }

    public void updateProduct(final LoanProduct product){
        this.loanProduct = product;
    }

    public void updateFacilitator(final AppUser facilitator){
        this.facilitator = facilitator;
    }

    public void updateGroupName(final String groupName){
        this.groupName = groupName;
    }

    public AppUser getAddedBy() {
        return addedBy;
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        if (command.isChangeInStringParameterNamed(PrequalificatoinApiConstants.groupNameParamName, this.groupName)) {
            final String newValue = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.groupNameParamName);
            actualChanges.put(PrequalificatoinApiConstants.groupNameParamName, newValue);
            this.groupName = newValue;
        }

        if (command.isChangeInLongParameterNamed(PrequalificatoinApiConstants.agencyIdParamName, this.agency.getId())) {
            final Long newValue = command.longValueOfParameterNamed(PrequalificatoinApiConstants.agencyIdParamName);
            actualChanges.put(PrequalificatoinApiConstants.agencyIdParamName, newValue);
            //this.ag = newValue;
        }

        if (command.isChangeInLongParameterNamed(PrequalificatoinApiConstants.centerIdParamName, this.centerId)) {
            final Long newValue = command.longValueOfParameterNamed(PrequalificatoinApiConstants.centerIdParamName);
            actualChanges.put(PrequalificatoinApiConstants.centerIdParamName, newValue);
            this.centerId = newValue;
        }

        if (command.isChangeInLongParameterNamed(PrequalificatoinApiConstants.productIdParamName, this.loanProduct.getId())) {
            final Long newValue = command.longValueOfParameterNamed(PrequalificatoinApiConstants.productIdParamName);
            actualChanges.put(PrequalificatoinApiConstants.productIdParamName, newValue);
            //this.loanProduct. = newValue;
        }

        if (command.isChangeInLongParameterNamed(PrequalificatoinApiConstants.facilitatorParamName, this.facilitator.getId())) {
            final Long newValue = command.longValueOfParameterNamed(PrequalificatoinApiConstants.facilitatorParamName);
            actualChanges.put(PrequalificatoinApiConstants.facilitatorParamName, newValue);
            //this.centerId = newValue;
        }

        //TODO: process changes in members

        return actualChanges;
    }

    public void updateComments(String comment) {
        this.comments = comment;
    }
}
