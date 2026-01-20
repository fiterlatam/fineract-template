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

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "m_prequalification_status_log")
@Getter
public class PrequalificationStatusLog extends AbstractPersistableCustom implements Comparable<PrequalificationStatusLog> {

    @ManyToOne
    @JoinColumn(name = "prequalification_id")
    private PrequalificationGroup prequalificationGroup;

    @ManyToOne
    @JoinColumn(name = "updatedby_id", nullable = false)
    private AppUser addedBy;

    @Column(name = "from_status", nullable = false)
    private Integer fromStatus;

    @Column(name = "to_status", nullable = false)
    private Integer toStatus;

    @Column(name = "date_created", nullable = false)
    private LocalDate dateCreated;

    @Column(name = "comments", nullable = false)
    private String comments;

    @Column(name = "sub_status", nullable = false)
    private Integer subStatus;

    @ManyToOne
    @JoinColumn(name = "assigned_to", nullable = false)
    private AppUser assignedTo;

    @ManyToOne
    @JoinColumn(name = "reason_code_id")
    private CodeValue reasonCode;

    // Only when will send it through unit analysis in first phase D
    @Column(name = "with_exceptions")
    private Boolean withExceptions;

    @Column(name = "is_exception")
    private Boolean exception;

    protected PrequalificationStatusLog() {
        //
    }

    private PrequalificationStatusLog(final AppUser appUser, final Integer fromStatus, final Integer toStatus, final String comments,
                                           final PrequalificationGroup group, final CodeValue reasonCode, final Boolean withExceptions) {
        this.dateCreated = DateUtils.getLocalDateOfTenant();
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.prequalificationGroup = group;
        this.comments = comments;
        this.addedBy = appUser;
        this.reasonCode = reasonCode;
        this.withExceptions = withExceptions;
    }

    private PrequalificationStatusLog(final AppUser appUser, final Integer fromStatus, final Integer toStatus, final String comments,
                                      final PrequalificationGroup group, final CodeValue reasonCode, final Boolean withExceptions, Boolean exception) {
        this.dateCreated = DateUtils.getLocalDateOfTenant();
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.prequalificationGroup = group;
        this.comments = comments;
        this.addedBy = appUser;
        this.reasonCode = reasonCode;
        this.withExceptions = withExceptions;
        this.exception = exception;
    }

    public static PrequalificationStatusLog fromJson(final AppUser appUser, final Integer fromStatus, final Integer toStatus,
            final String comments, final PrequalificationGroup group, CodeValue reasonCode, Boolean withExceptions) {
        return new PrequalificationStatusLog(appUser, fromStatus, toStatus, comments, group, reasonCode, withExceptions);
    }

    public static PrequalificationStatusLog fromJson(final AppUser appUser, final Integer fromStatus, final Integer toStatus,
                                                     final String comments, final PrequalificationGroup group, CodeValue reasonCode, Boolean withExceptions, Boolean exception) {
        return new PrequalificationStatusLog(appUser, fromStatus, toStatus, comments, group, reasonCode, withExceptions, exception);
    }

    public void updateAssignedTo(final AppUser assignedTo) {
        this.assignedTo = assignedTo;
    }

    public void updateSubStatus(final Integer subStatus) {
        this.subStatus = subStatus;
    }

    @Override
    public int compareTo(PrequalificationStatusLog entry) {
        return this.getId().compareTo(entry.getId());
    }

}
