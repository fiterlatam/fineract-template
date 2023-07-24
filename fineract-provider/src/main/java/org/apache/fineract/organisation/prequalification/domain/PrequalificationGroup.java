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

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.agency.domain.Agency;
import org.apache.fineract.organisation.centerGroup.domain.CenterGroup;
import org.apache.fineract.organisation.prequalification.command.PrequalificatoinApiConstants;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.useradministration.domain.AppUser;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private CenterGroup centerGroup;

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


    public static PrequalificationGroup fromJson(final AppUser appUser, final AppUser facilitator,
                                                 final Agency agency, final CenterGroup centerGroup, final LoanProduct loanProduct,
                                                 final JsonCommand command) {
        String groupName = command.stringValueOfParameterNamed("groupName");
        Long center = command.longValueOfParameterNamed(PrequalificatoinApiConstants.centerIdParamName);
        if (centerGroup!=null) {
            groupName = centerGroup.getName();
        }
        return new PrequalificationGroup(appUser, facilitator, agency, centerGroup, groupName,center, loanProduct);
    }

    protected PrequalificationGroup() {
        //
    }

    private PrequalificationGroup(final AppUser appUser, final AppUser facilitator,
                                  final Agency agency, final CenterGroup centerGroup, final String groupName, Long center, final LoanProduct loanProduct) {
        this.addedBy = appUser;
        this.facilitator = facilitator;
        this.status = PrequalificationStatus.PENDING.getValue();
        this.loanProduct = loanProduct;
        this.centerGroup = centerGroup;
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
    public void updatePrequalificationNumber(final String prequalificationNumber) {
        ;
        this.prequalificationNumber = prequalificationNumber;
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        return actualChanges;
    }

    public void updateComments(String comment) {
        this.comments = comment;
    }
}
