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
package org.apache.fineract.organisation.bankcheque.domain;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.useradministration.domain.AppUser;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "batch_cheque_requests")
public class BatchChequeRequest extends AbstractPersistableCustom {

    @ManyToOne(optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private AppUser requestedBy;

    @Column(name = "status", nullable = false, length = 50)
    private String status = BatchChequeRequestStatus.PENDING;

    @Column(name = "date_requested", nullable = false)
    private LocalDateTime dateRequested;

    @Column(name = "date_processed")
    private LocalDateTime dateProcessed;

    @Column(name = "cheque_ids", nullable = false, columnDefinition = "TEXT")
    private String chequeIds;

    public static BatchChequeRequest create(final AppUser requestedBy, final String chequeIds, final LocalDateTime dateRequested) {
        return new BatchChequeRequest().setRequestedBy(requestedBy).setStatus(BatchChequeRequestStatus.PENDING)
                .setDateRequested(dateRequested).setChequeIds(chequeIds);
    }
}
