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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.prequalification.command.PrequalificatoinApiConstants;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "m_prequalification_group_members")
@Getter
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

    @Column(name = "client_id")
    private Long clientId;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private PrequalificationGroup prequalificationGroup;

    @ManyToOne
    @JoinColumn(name = "added_by")
    private AppUser addedBy;

    @Column(name = "buro_check_status", nullable = false)
    private Integer buroCheckStatus;


    protected PrequalificationGroupMember() {
        //
    }

    private PrequalificationGroupMember(final AppUser appUser, final String clientName, final String dpi, final LocalDate dob,
            final String puente, final PrequalificationGroup group, final BigDecimal requestedAmount, final Integer status,
            final Long clientId) {
        this.status = PrequalificationStatus.PENDING.getValue();
        this.createdAt = DateUtils.getLocalDateTimeOfTenant();
        this.dob = dob;
        this.dpi = dpi;
        this.name = clientName;
        this.clientId = clientId;
        this.prequalificationGroup = group;
        this.workWithPuente = puente;
        this.requestedAmount = requestedAmount;
        this.addedBy = appUser;
        this.status = status;
    }

    public static PrequalificationGroupMember fromJson(PrequalificationGroup group, String name, String dpi, Long clientId,
            LocalDate dateOfBirth, BigDecimal requestedAmount, String puente, AppUser addedBy, Integer status) {
        // TODO Auto-generated method stub
        return new PrequalificationGroupMember(addedBy, name, dpi, dateOfBirth, puente, group, requestedAmount, status, clientId);
    }

    public void updateStatus(final PrequalificationMemberIndication prequalificationStatus) {
        ;
        this.status = prequalificationStatus.getValue();
    }

    public void updateName(final String name) {
        this.name = name;
    }

    public void updateDPI(final String dpi) {
        this.dpi = dpi;
    }

    public void updateDOB(final LocalDate dob) {
        this.dob = dob;
    }

    public void updateBuroCheckStatus(final Integer buroCheckStatus) {
        this.buroCheckStatus = buroCheckStatus;
    }

    public void updateAmountRequested(final BigDecimal amountRequested) {
        this.requestedAmount = amountRequested;
    }

    public void updateWorkWithPuente(final String workWithPuente) {
        this.workWithPuente = workWithPuente;
    }

    public Map<String, Object> update(JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        if (command.isChangeInStringParameterNamed(PrequalificatoinApiConstants.memberNameParamName, this.name)) {
            final String newValue = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.memberNameParamName);
            actualChanges.put(PrequalificatoinApiConstants.memberNameParamName, newValue);
        }

        if (command.isChangeInStringParameterNamed(PrequalificatoinApiConstants.memberDpiParamName, this.dpi)) {
            final String newValue = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.memberDpiParamName);
            actualChanges.put(PrequalificatoinApiConstants.memberDpiParamName, newValue);
        }

        if (command.isChangeInDateParameterNamed(PrequalificatoinApiConstants.memberDobParamName, this.dob)) {
            final LocalDate newValue = command.dateValueOfParameterNamed(PrequalificatoinApiConstants.memberDobParamName);
            actualChanges.put(PrequalificatoinApiConstants.memberDobParamName, newValue);
        }

        if (command.isChangeInBigDecimalParameterNamed(PrequalificatoinApiConstants.memberRequestedAmountParamName, this.requestedAmount)) {
            final BigDecimal newValue = command
                    .bigDecimalValueOfParameterNamed(PrequalificatoinApiConstants.memberRequestedAmountParamName);
            actualChanges.put(PrequalificatoinApiConstants.memberRequestedAmountParamName, newValue);
        }

        if (command.isChangeInStringParameterNamed(PrequalificatoinApiConstants.memberWorkWithPuenteParamName, this.workWithPuente)) {
            final String newValue = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.memberWorkWithPuenteParamName);
            actualChanges.put(PrequalificatoinApiConstants.memberWorkWithPuenteParamName, newValue);
        }

        return actualChanges;
    }
}
