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
package org.apache.fineract.organisation.committee.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@Entity
@Getter
@Table(name = "committee_approval_limits", uniqueConstraints = { @UniqueConstraint(columnNames = { "committee_id" }, name = "user_id") })
public class CommitteeApprovalLimits extends AbstractPersistableCustom {

    @Column(name = "committee_id")
    private Long committee;

    @Column(name = "from_amount")
    private BigDecimal fromAmount;

    @Column(name = "to_amount")
    private BigDecimal toAmount;

    @Column(name = "condition")
    private String condition;

    @Column(name = "limit")
    private Integer limit;

    @Column(name = "created_at")
    private LocalDateTime dateCreated;

    @Column(name = "created_by")
    private Long createdBy;

    protected CommitteeApprovalLimits() {
        //
    }

    public CommitteeApprovalLimits(Long committee, BigDecimal fromAmount, BigDecimal toAmount, String condition, Integer limit,
            Long createdBy) {
        this.committee = committee;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.condition = condition;
        this.limit = limit;
        this.dateCreated = DateUtils.getLocalDateTimeOfSystem();
        this.createdBy = createdBy;
    }

}
