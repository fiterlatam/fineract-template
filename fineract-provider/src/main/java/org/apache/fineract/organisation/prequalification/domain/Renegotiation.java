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
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "renegotiations")
public class Renegotiation extends AbstractPersistableCustom {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prequalification_id", nullable = false)
    private PrequalificationGroup prequalificationGroup;

    @Column(name = "proposed_interest", precision = 19, scale = 6)
    private BigDecimal proposedInterest;

    @Column(name = "proposed_amount", precision = 19, scale = 6)
    private BigDecimal proposedAmount;

    @Column(name = "proposed_term")
    private Integer proposedTerm;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "approval_comments", length = 255)
    private String approvalComments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private AppUser approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    public Renegotiation(PrequalificationGroup prequalificationGroup, BigDecimal proposedInterest, BigDecimal proposedAmount,
            Integer proposedTerm, String comments, String status, LocalDateTime createdDate, AppUser createdBy) {
        this(prequalificationGroup, proposedInterest, proposedAmount, proposedTerm, comments, status, createdDate, createdBy, null, null,
                null);
    }

    public Renegotiation(PrequalificationGroup prequalificationGroup, BigDecimal proposedInterest, BigDecimal proposedAmount,
            Integer proposedTerm, String comments, String status, LocalDateTime createdDate, AppUser createdBy, String approvalComments,
            AppUser approvedBy, LocalDateTime approvedDate) {
        this.prequalificationGroup = prequalificationGroup;
        this.proposedInterest = proposedInterest;
        this.proposedAmount = proposedAmount;
        this.proposedTerm = proposedTerm;
        this.comments = comments;
        this.status = status != null ? status : "PENDING";
        this.createdDate = createdDate;
        this.createdBy = createdBy;
        this.approvalComments = approvalComments;
        this.approvedBy = approvedBy;
        this.approvedDate = approvedDate;
    }

    public static Renegotiation create(PrequalificationGroup prequalificationGroup, BigDecimal proposedInterest, BigDecimal proposedAmount,
            Integer proposedTerm, String comments, LocalDateTime createdDate, AppUser createdBy) {
        return new Renegotiation(prequalificationGroup, proposedInterest, proposedAmount, proposedTerm, comments, "PENDING", createdDate,
                createdBy);
    }

    public void approve(String approvalComments, AppUser approvedBy, LocalDateTime approvedDate) {
        this.approvalComments = approvalComments;
        this.approvedBy = approvedBy;
        this.approvedDate = approvedDate;
        this.status = "APPROVED"; // business rule assumption
    }
}
