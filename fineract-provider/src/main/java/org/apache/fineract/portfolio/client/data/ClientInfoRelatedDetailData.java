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
package org.apache.fineract.portfolio.client.data;

public class ClientInfoRelatedDetailData {

    private Integer loanCycle;

    private String groupNumber;

    private String maidenName;

    private String othernames;

    private String groupMember;

    private String statusInGroup;

    private String retirementReason;

    private String civilStatus;

    private String educationLevel;

    private String ethinicity;

    private String nationality;

    private String languages;

    private String economicSector;

    private String economicActivity;

    private String familyReference;

    public ClientInfoRelatedDetailData(Integer loanCycle, String groupNumber, String maidenName, String othernames, String groupMember,
            String statusInGroup, String retirementReason, String civilStatus, String educationLevel, String ethinicity, String nationality,
            String languages, String economicSector, String economicActivity, String familyReference) {
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

    public static ClientInfoRelatedDetailData instance(Integer loanCycle, String groupNumber, String maidenName, String othernames,
            String groupMember, String statusInGroup, String retirementReason, String civilStatus, String educationLevel, String ethinicity,
            String nationality, String languages, String economicSector, String economicActivity, String familyReference) {

        return new ClientInfoRelatedDetailData(loanCycle, groupNumber, maidenName, othernames, groupMember, statusInGroup, retirementReason,
                civilStatus, educationLevel, ethinicity, nationality, languages, economicSector, economicActivity, familyReference);
    }

}
