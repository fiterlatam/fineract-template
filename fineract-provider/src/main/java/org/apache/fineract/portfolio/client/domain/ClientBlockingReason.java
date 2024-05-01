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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import java.time.LocalDate;

@Entity
@Table(name = "m_client_blocking_reason")
public class ClientBlockingReason extends AbstractPersistableCustom {
    @Column(name = "client_id", nullable = false)
    private Long clientId;
    @Column(name = "blocking_reason_id", nullable = false)
    private Long blockingReasonId;
    @Column(name = "block_date")
    private LocalDate blockDate;
    @Column(name = "block_comment")
    private String blockComment;
    @Column(name = "block_by")
    private Long blockBy;
    @Column(name = "unblock_date")
    private LocalDate unblockDate;
    @Column(name = "unblock_comment")
    private String unblockComment;
    @Column(name = "unblock_by")
    private Long unblockBy;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    public static ClientBlockingReason instance(final Long clientId, final Long blockingReasonId, final Long createdBy,
                                                final LocalDate blockDate, final String blockComment, final Long blockBy) {
        return new ClientBlockingReason(clientId, blockingReasonId, createdBy, blockDate, blockComment, blockBy);
    }

    private ClientBlockingReason(final Long clientId, final Long blockingReasonId, final Long createdBy,
                                 final LocalDate blockDate, final String blockComment, final Long blockBy) {
        this.clientId = clientId;
        this.blockingReasonId = blockingReasonId;
        this.createdBy = createdBy;
        this.blockDate = blockDate;
        this.blockComment = blockComment;
        this.blockBy = blockBy;
    }

    private ClientBlockingReason() {

    }

    public void updateAfterUnblock(final LocalDate unblockDate, final String unblockComment, final Long unblockBy) {
        this.unblockDate = unblockDate;
        this.unblockComment = unblockComment;
        this.unblockBy = unblockBy;
    }

    public Long getClientId() {
        return clientId;
    }

    public Long getBlockingReasonId() {
        return blockingReasonId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }
}
