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

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.useradministration.domain.AppUser;

@Getter
@Entity
@Table(name = "m_bank_check")
public class Cheque extends AbstractAuditableCustom {

    @Column(name = "check_no", nullable = false)
    private Long chequeNo;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "status_enum", nullable = false)
    private Integer status;

    @Column(name = "description")
    private String description;

    @Column(name = "voided_date")
    private LocalDate voidedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voidedby_id")
    private AppUser voidedBy;

    @Column(name = "void_authorized_date")
    private LocalDate voidAuthorizedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "void_authorizedby_id")
    private AppUser voidAuthorizedBy;

    @Column(name = "printed_date")
    private LocalDate printedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "printedby_id")
    private AppUser printedBy;

    @Column(name = "usedon_date")
    private LocalDate usedOnDate;

    public Cheque setChequeNo(Long chequeNo) {
        this.chequeNo = chequeNo;
        return this;
    }

    public Cheque setBatch(Batch batch) {
        this.batch = batch;
        return this;
    }

    public Cheque setStatus(Integer status) {
        this.status = status;
        return this;
    }

    public Cheque setDescription(String description) {
        this.description = description;
        return this;
    }

    public LocalDate getChequeedDate() {
        return voidedDate;
    }

    public Cheque setVoidedDate(LocalDate voidedDate) {
        this.voidedDate = voidedDate;
        return this;
    }

    public Cheque setVoidedBy(AppUser voidedBy) {
        this.voidedBy = voidedBy;
        return this;
    }

    public Cheque setVoidAuthorizedDate(LocalDate voidAuthorizedDate) {
        this.voidAuthorizedDate = voidAuthorizedDate;
        return this;
    }

    public Cheque setVoidAuthorizedBy(AppUser voidAuthorizedBy) {
        this.voidAuthorizedBy = voidAuthorizedBy;
        return this;
    }

    public Cheque setPrintedDate(LocalDate printedDate) {
        this.printedDate = printedDate;
        return this;
    }

    public Cheque setPrintedBy(AppUser printedBy) {
        this.printedBy = printedBy;
        return this;
    }

    public Cheque setUsedOnDate(LocalDate usedOnDate) {
        this.usedOnDate = usedOnDate;
        return this;
    }
}
