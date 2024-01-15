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
package org.apache.fineract.portfolio.client.domain;

import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;

/**
 * a {@link ClientInfoRelatedDetail}.
 */
@Embeddable
public class ClientInfoRelatedDetail {

    @Column(name = "loan_cycle", nullable = false)
    private Integer loanCycle;

    @Column(name = "group_number", nullable = true)
    private String groupNumber;

    @Column(name = "maiden_name", nullable = true)
    private String maidenName;

    @Column(name = "other_names", nullable = true)
    private String othernames;

    @Column(name = "group_member", nullable = true)
    private String groupMember;

    @Column(name = "status_in_group", nullable = true)
    private String statusInGroup;

    @Column(name = "retirement_reason", nullable = true)
    private String retirementReason;

    @Column(name = "civil_status", nullable = true)
    private String civilStatus;

    @Column(name = "education_level", nullable = true)
    private String educationLevel;

    @Column(name = "ethinicity", nullable = true)
    private String ethinicity;

    @Column(name = "nationality", nullable = true)
    private String nationality;

    @Column(name = "languages", nullable = true)
    private String languages;

    @Column(name = "economic_sector", nullable = true)
    private Long economicSector;

    @Column(name = "economic_activity", nullable = true)
    private Long economicActivity;

    @Column(name = "family_reference", nullable = true)
    private String familyReference;

    public ClientInfoRelatedDetail(Integer loanCycle, String groupNumber, String maidenName, String othernames, String groupMember,
            String statusInGroup, String retirementReason, String civilStatus, String educationLevel, String ethinicity, String nationality,
            String languages, Long economicSector, Long economicActivity, String familyReference) {
        this.loanCycle = loanCycle;
        this.groupNumber = groupNumber;
        this.maidenName = maidenName;
        this.othernames = othernames;
        this.groupMember = groupMember;
        this.statusInGroup = statusInGroup;
        this.retirementReason = retirementReason;
        this.civilStatus = civilStatus;
        this.educationLevel = educationLevel;
        this.ethinicity = ethinicity;
        this.nationality = nationality;
        this.languages = languages;
        this.economicSector = economicSector;
        this.economicActivity = economicActivity;
        this.familyReference = familyReference;
    }

    protected ClientInfoRelatedDetail() {
        //
    }

    public static ClientInfoRelatedDetail createFrom(JsonCommand command) {
        final Integer loanCycle = command.integerValueOfParameterNamed(ClientApiConstants.loanCycleParamName);
        final String groupNumber = command.stringValueOfParameterNamed(ClientApiConstants.groupNumberParamName);
        final String maidenName = command.stringValueOfParameterNamed(ClientApiConstants.maidenNameParamName);
        final String othernames = command.stringValueOfParameterNamed(ClientApiConstants.otherNamesParamName);
        final String groupMember = command.stringValueOfParameterNamed(ClientApiConstants.groupMemberParamName);
        final String statusInGroup = command.stringValueOfParameterNamed(ClientApiConstants.statusInGroupParamName);
        final String retirementReason = command.stringValueOfParameterNamed(ClientApiConstants.retirementReasonParamName);
        final String civilStatus = command.stringValueOfParameterNamed(ClientApiConstants.civilStatusParamName);
        final String educationLevel = command.stringValueOfParameterNamed(ClientApiConstants.educationLevelParamName);
        final String ethinicity = command.stringValueOfParameterNamed(ClientApiConstants.ethinicityParamName);
        final String nationality = command.stringValueOfParameterNamed(ClientApiConstants.nationalityParamName);
        final String languages = command.stringValueOfParameterNamed(ClientApiConstants.languagesParamName);
        final Long economicSector = command.longValueOfParameterNamed(ClientApiConstants.economicSectorParamName);
        final Long economicActivity = command.longValueOfParameterNamed(ClientApiConstants.economicActivityParamName);
        final String familyReference = command.stringValueOfParameterNamed(ClientApiConstants.familyReferenceParamName);

        return new ClientInfoRelatedDetail(loanCycle, groupNumber, maidenName, othernames, groupMember, statusInGroup, retirementReason,
                civilStatus, educationLevel, ethinicity, nationality, languages, economicSector, economicActivity, familyReference);
    }

    public void update(JsonCommand command, Map<String, Object> actualChanges) {
        if (command.isChangeInIntegerParameterNamed(ClientApiConstants.loanCycleParamName, this.loanCycle)) {
            final Integer newValue = command.integerValueOfParameterNamed(ClientApiConstants.loanCycleParamName);
            actualChanges.put(ClientApiConstants.loanCycleParamName, newValue);
            this.loanCycle = newValue;
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.groupMemberParamName, this.groupNumber)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.groupMemberParamName);
            actualChanges.put(ClientApiConstants.groupMemberParamName, newValue);
            this.groupNumber = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.maidenNameParamName, this.maidenName)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.maidenNameParamName);
            actualChanges.put(ClientApiConstants.maidenNameParamName, newValue);
            this.maidenName = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.otherNamesParamName, this.othernames)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.maidenNameParamName);
            actualChanges.put(ClientApiConstants.maidenNameParamName, newValue);
            this.othernames = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.groupMemberParamName, this.groupMember)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.groupMemberParamName);
            actualChanges.put(ClientApiConstants.groupMemberParamName, newValue);
            this.groupMember = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.statusInGroupParamName, this.statusInGroup)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.statusInGroupParamName);
            actualChanges.put(ClientApiConstants.statusInGroupParamName, newValue);
            this.statusInGroup = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.retirementReasonParamName, this.retirementReason)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.retirementReasonParamName);
            actualChanges.put(ClientApiConstants.retirementReasonParamName, newValue);
            this.retirementReason = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.civilStatusParamName, this.civilStatus)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.civilStatusParamName);
            actualChanges.put(ClientApiConstants.civilStatusParamName, newValue);
            this.civilStatus = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.educationLevelParamName, this.educationLevel)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.educationLevelParamName);
            actualChanges.put(ClientApiConstants.educationLevelParamName, newValue);
            this.educationLevel = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.ethinicityParamName, this.ethinicity)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.ethinicityParamName);
            actualChanges.put(ClientApiConstants.ethinicityParamName, newValue);
            this.ethinicity = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.nationalityParamName, this.nationality)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.nationalityParamName);
            actualChanges.put(ClientApiConstants.nationalityParamName, newValue);
            this.nationality = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.languagesParamName, this.languages)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.languagesParamName);
            actualChanges.put(ClientApiConstants.languagesParamName, newValue);
            this.languages = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInLongParameterNamed(ClientApiConstants.economicSectorParamName, this.economicSector)) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.economicSectorParamName);
            actualChanges.put(ClientApiConstants.economicSectorParamName, newValue);
            this.economicSector = newValue;
        }
        if (command.isChangeInLongParameterNamed(ClientApiConstants.economicActivityParamName, this.economicActivity)) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.economicActivityParamName);
            actualChanges.put(ClientApiConstants.economicActivityParamName, newValue);
            this.economicActivity = newValue;
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.familyReferenceParamName, this.familyReference)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.familyReferenceParamName);
            actualChanges.put(ClientApiConstants.familyReferenceParamName, newValue);
            this.familyReference = StringUtils.defaultIfEmpty(newValue, null);
        }
    }

    public Integer getLoanCycle() {
        return this.loanCycle;
    }

    public void setLoanCycle(Integer loanCycle) {
        this.loanCycle = loanCycle;
    }
}
