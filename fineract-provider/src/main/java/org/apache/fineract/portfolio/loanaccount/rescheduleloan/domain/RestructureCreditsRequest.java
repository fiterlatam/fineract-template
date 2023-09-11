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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "m_restructure_credit_requests")
public class RestructureCreditsRequest extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct loanProduct;

    @Column(name = "total_loan_amount", nullable = false)
    private BigDecimal totalLoanAmount;

    @Column(name = "status")
    private Integer statusEnum;

    @Column(name = "new_disbursement_date")
    private LocalDateTime newDisbursementDate;

    @Column(name = "comments")
    private String comments;

    @Column(name = "date_requested")
    private LocalDateTime dateRequested;

    @ManyToOne
    @JoinColumn(name = "requested_by")
    private AppUser requestedByUser;

    @Column(name = "date_approved")
    private LocalDateTime approvedOnDate;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private AppUser approvedByUser;

    @Column(name = "lastmodified_date")
    private LocalDateTime lastModifiedDate;

    @ManyToOne
    @JoinColumn(name = "lastmodifiedby_id")
    private AppUser modifiedByUser;

    @OneToMany(mappedBy = "restructureCreditsRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RestructureCreditsLoanMapping> restructureCreditsLoanMappings = new ArrayList<>();

    /**
     * LoanRescheduleRequest constructor
     **/
    protected RestructureCreditsRequest() {}

    /**
     * LoanRescheduleRequest constructor
     **/
    private RestructureCreditsRequest(final Client client, final Integer statusEnum, final LoanProduct product,
            final BigDecimal totalLoanAmount, final LocalDateTime newDisbursementDate, final String comments,
            final LocalDateTime dateRequested, final AppUser requestedByUser, final LocalDateTime approvedOnDate,
            final AppUser approvedByUser, final LocalDateTime lastModifiedDate, final AppUser modifiedByUser,
            final List<RestructureCreditsLoanMapping> restructureCreditsLoanMappings) {
        this.client = client;
        this.statusEnum = statusEnum;
        this.loanProduct = product;
        this.totalLoanAmount = totalLoanAmount;
        this.newDisbursementDate = newDisbursementDate;
        this.dateRequested = dateRequested;
        this.requestedByUser = requestedByUser;
        this.comments = comments;
        this.restructureCreditsLoanMappings = restructureCreditsLoanMappings;
        this.approvedByUser = approvedByUser;
        this.approvedOnDate = approvedOnDate;
        this.modifiedByUser = modifiedByUser;
        this.lastModifiedDate = lastModifiedDate;

    }

    public static RestructureCreditsRequest instance(final Client client, final Integer statusEnum, final LoanProduct product,
            final BigDecimal totalLoanAmount, final LocalDateTime newDisbursementDate, final String comments,
            final LocalDateTime dateRequested, final AppUser requestedByUser, final LocalDateTime approvedOnDate,
            final AppUser approvedByUser, final LocalDateTime lastModifiedDate, final AppUser modifiedByUser,
            final List<RestructureCreditsLoanMapping> restructureCreditsLoanMappings) {

        return new RestructureCreditsRequest(client, statusEnum, product, totalLoanAmount, newDisbursementDate, comments, dateRequested,
                requestedByUser, approvedOnDate, approvedByUser, lastModifiedDate, modifiedByUser, restructureCreditsLoanMappings);
    }

    public static RestructureCreditsRequest fromJSON(Client client, Integer statusEnum, LoanProduct loanProduct,
            BigDecimal totalOutstanding, LocalDateTime disbursementDate, String comments, LocalDateTime localDateTimeOfSystem,
            AppUser appUser) {

        return new RestructureCreditsRequest(client, statusEnum, loanProduct, totalOutstanding, disbursementDate, comments,
                localDateTimeOfSystem, appUser, null, null, null, null, null);
    }

    /**
     * @return the reschedule request loan object
     **/
    public Client getClient() {
        return this.client;
    }

    /**
     * @return the status enum
     **/
    public Integer getStatusEnum() {
        return this.statusEnum;
    }

    /**
     * change the status of the loan reschedule request to approved, also updating the approvedByUser and approvedOnDate
     * properties
     *
     * @param approvedByUser
     *            the user who approved the request
     * @param approvedOnDate
     *            the date of the approval
     *
     **/
    public void approve(final AppUser approvedByUser, final LocalDateTime approvedOnDate) {

        if (approvedOnDate != null) {
            this.approvedByUser = approvedByUser;
            this.approvedOnDate = approvedOnDate;
            this.statusEnum = LoanStatus.APPROVED.getValue();
        }
    }

    /**
     * change the status of the loan reschedule request to rejected, also updating the approvedByUser and approvedOnDate
     * properties
     *
     * @param approvedByUser
     *            the user who approved the request
     * @param approvedOnDate
     *            the date of the approval
     *
     **/
    public void modify(final AppUser approvedByUser, final LocalDateTime approvedOnDate) {

        if (approvedOnDate != null) {
            this.modifiedByUser = approvedByUser;
            this.lastModifiedDate = approvedOnDate;
            this.statusEnum = LoanStatus.REJECTED.getValue();
        }
    }

    public void updateMappings(final List<RestructureCreditsLoanMapping> mapping) {
        this.restructureCreditsLoanMappings.addAll(mapping);
    }

    public LocalDateTime getNewDisbursementDate() {
        return newDisbursementDate;
    }

    public List<RestructureCreditsLoanMapping> getCreditMappings() {
        return this.restructureCreditsLoanMappings;
    }

    public BigDecimal getTotalLoanAmount() {
        return totalLoanAmount;
    }
}
