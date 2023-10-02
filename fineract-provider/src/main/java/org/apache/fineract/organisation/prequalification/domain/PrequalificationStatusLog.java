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
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "m_prequalification_status_log")
@Getter
public class PrequalificationStatusLog extends AbstractPersistableCustom {

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

    protected PrequalificationStatusLog() {
        //
    }

    private PrequalificationStatusLog(final AppUser appUser, final Integer fromStatus, final Integer toStatus, final String comments,
            final PrequalificationGroup group) {
        this.dateCreated = DateUtils.getLocalDateOfTenant();
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.prequalificationGroup = group;
        this.comments = comments;
        this.addedBy = appUser;
    }

    public static PrequalificationStatusLog fromJson(final AppUser appUser, final Integer fromStatus, final Integer toStatus,
            final String comments, final PrequalificationGroup group) {
        return new PrequalificationStatusLog(appUser, fromStatus, toStatus, comments, group);
    }
}
