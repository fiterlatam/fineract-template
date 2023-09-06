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

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;

/**
 * LoanRepaymentScheduleDetail encapsulates all the details of a {@link Client} that are also used and persisted by
 * a {@link Loan}.
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
    private String economicSector;
    
    @Column(name = "economic_activity", nullable = true)
    private String economicActivity;

    public ClientInfoRelatedDetail(Integer loanCycle, String groupNumber, String maidenName, String othernames, String groupMember,
                                   String statusInGroup, String retirementReason, String civilStatus,
                                   String educationLevel, String ethinicity, String nationality, String languages,
                                   String economicSector,String economicActivity) {
        this.loanCycle=loanCycle;
        this.groupNumber = groupNumber;
        this.maidenName = maidenName;
        this.othernames = othernames;
        this.groupMember = groupMember;
        this.statusInGroup = statusInGroup;
        this.retirementReason = retirementReason;
        this.civilStatus = civilStatus;
        this.educationLevel = educationLevel;
        this.ethinicity = ethinicity;
        this.nationality=nationality;
        this.languages = languages;
        this.economicSector = economicSector;
        this.economicActivity = economicActivity;
    }

    protected ClientInfoRelatedDetail() {
        //
    }

    public static ClientInfoRelatedDetail createFrom(JsonCommand command) {
        final Integer loanCycle= command.integerValueOfParameterNamed(ClientApiConstants.loanCycleParamName);
        final String groupNumber= command.stringValueOfParameterNamed(ClientApiConstants.groupNumberParamName);
        final String maidenName= command.stringValueOfParameterNamed(ClientApiConstants.maidenNameParamName);
        final String othernames= command.stringValueOfParameterNamed(ClientApiConstants.otherNamesParamName);
        final String groupMember= command.stringValueOfParameterNamed(ClientApiConstants.groupMemberParamName);
        final String statusInGroup= command.stringValueOfParameterNamed(ClientApiConstants.statusInGroupParamName);
        final String retirementReason= command.stringValueOfParameterNamed(ClientApiConstants.retirementReasonParamName);
        final String civilStatus= command.stringValueOfParameterNamed(ClientApiConstants.civilStatusParamName);
        final String educationLevel= command.stringValueOfParameterNamed(ClientApiConstants.educationLevelParamName);
        final String ethinicity= command.stringValueOfParameterNamed(ClientApiConstants.ethinicityParamName);
        final String nationality= command.stringValueOfParameterNamed(ClientApiConstants.nationalityParamName);
        final String languages= command.stringValueOfParameterNamed(ClientApiConstants.languagesParamName);
        final String economicSector= command.stringValueOfParameterNamed(ClientApiConstants.economicSectorParamName);
        final String economicActivity= command.stringValueOfParameterNamed(ClientApiConstants.economicActivityParamName);

        return new ClientInfoRelatedDetail(loanCycle,groupNumber, maidenName, othernames, groupMember, statusInGroup,
                retirementReason, civilStatus, educationLevel, ethinicity, nationality, languages,economicSector, economicActivity);
    }
}
