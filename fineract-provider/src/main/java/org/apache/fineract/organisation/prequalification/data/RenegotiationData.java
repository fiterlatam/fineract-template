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
package org.apache.fineract.organisation.prequalification.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.apache.fineract.organisation.prequalification.domain.Renegotiation;

/**
 * Lightweight DTO for Renegotiation entity to expose necessary data without JPA entity leakage.
 */
@Data
public class RenegotiationData {

    private final Long id;
    private final BigDecimal proposedInterest;
    private final BigDecimal proposedAmount;
    private final Integer proposedTerm;
    private final String comments;
    private final String status;
    private final LocalDateTime createdDate;
    private final Long createdById;
    private final String createdByUsername;
    private final String createdByFirstname;
    private final String createdByLastname;
    private final String approvalComments;
    private final Long approvedById;
    private final String approvedByUsername;
    private final String approvedByFirstname;
    private final String approvedByLastname;
    private final LocalDateTime approvedDate;

    public static RenegotiationData of(Renegotiation entity) {
        if (entity == null) {
            return null;
        }
        Long createdById = null;
        String username = null;
        String firstname = null;
        String lastname = null;
        if (entity.getCreatedBy() != null) {
            createdById = entity.getCreatedBy().getId();
            username = entity.getCreatedBy().getUsername();
            firstname = entity.getCreatedBy().getFirstname();
            lastname = entity.getCreatedBy().getLastname();
        }
        Long approvedById = null;
        String approvedByUsername = null;
        String approvedByFirstname = null;
        String approvedByLastname = null;
        if (entity.getApprovedBy() != null) {
            approvedById = entity.getApprovedBy().getId();
            approvedByUsername = entity.getApprovedBy().getUsername();
            approvedByFirstname = entity.getApprovedBy().getFirstname();
            approvedByLastname = entity.getApprovedBy().getLastname();
        }
        return new RenegotiationData(entity.getId(), entity.getProposedInterest(), entity.getProposedAmount(), entity.getProposedTerm(),
                entity.getComments(), entity.getStatus(), entity.getCreatedDate(), createdById, username, firstname, lastname,
                entity.getApprovalComments(), approvedById, approvedByUsername, approvedByFirstname, approvedByLastname,
                entity.getApprovedDate());
    }
}
