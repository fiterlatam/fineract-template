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
import org.apache.fineract.useradministration.domain.AppUser;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "m_prequalification_group_members")
public class PrequalificationGroupMember extends AbstractPersistableCustom {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "dpi", nullable = false)
    private String dpi;

    @Column(name = "dob", nullable = false)
    private LocalDate dob;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "work_with_puente", nullable = false)
    private String workWithPuente;

    @Column(name = "requested_amount", nullable = false)
    private BigDecimal requestedAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private PrequalificationGroup prequalificationGroup;

    @ManyToOne
    @JoinColumn(name = "added_by")
    private AppUser addedBy;

    protected PrequalificationGroupMember() {
        //
    }

    private PrequalificationGroupMember(final AppUser appUser, final String clientName, final String dpi, final LocalDate dob,
                                        final String puente, final PrequalificationGroup groupId, final  BigDecimal requestedAmount, final  Integer status) {
        this.status = PrequalificationStatus.PENDING.getValue();
        this.createdAt = DateUtils.getLocalDateTimeOfTenant();
        this.dob = dob;
        this.dpi = dpi;
        this.name = clientName;
        this.prequalificationGroup = groupId;
        this.workWithPuente = puente;
        this.requestedAmount = requestedAmount;
        this.addedBy = appUser;
        this.status = status;
    }

    public static PrequalificationGroupMember fromJson(PrequalificationGroup groupId, String name, String dpi, LocalDate dateOfBirth, BigDecimal requestedAmount, String puente, AppUser addedBy, Integer status) {
        // TODO Auto-generated method stub
        return new PrequalificationGroupMember(addedBy, name, dpi, dateOfBirth, puente, groupId, requestedAmount, status);
    }

    public void updateStatus(final PrequalificationStatus prequalificationStatus) {
        ;
        this.status = prequalificationStatus.getValue();
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        // final String documentTypeIdParamName = "documentTypeId";
        // if (command.isChangeInLongParameterNamed(documentTypeIdParamName, this.documentType.getId())) {
        // final Long newValue = command.longValueOfParameterNamed(documentTypeIdParamName);
        // actualChanges.put(documentTypeIdParamName, newValue);
        // }
        //
        // final String documentKeyParamName = "documentKey";
        // if (command.isChangeInStringParameterNamed(documentKeyParamName, this.documentKey)) {
        // final String newValue = command.stringValueOfParameterNamed(documentKeyParamName);
        // actualChanges.put(documentKeyParamName, newValue);
        // this.documentKey = StringUtils.defaultIfEmpty(newValue, null);
        // }
        //
        // final String descriptionParamName = "description";
        // if (command.isChangeInStringParameterNamed(descriptionParamName, this.description)) {
        // final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
        // actualChanges.put(descriptionParamName, newValue);
        // this.description = StringUtils.defaultIfEmpty(newValue, null);
        // }
        //
        // final String statusParamName = "status";
        // if (command.isChangeInStringParameterNamed(statusParamName,
        // ClientIdentifierStatus.fromInt(this.status).getCode())) {
        // final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
        // actualChanges.put(descriptionParamName, ClientIdentifierStatus.valueOf(newValue));
        // this.status = ClientIdentifierStatus.valueOf(newValue).getValue();
        // }

        return actualChanges;
    }
}
