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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.data;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Immutable data object representing restructure credits request data.
 **/
public final class RestructureCreditsRequestData {

    private final Long id;
    private String clientName;
    private String productName;
    private BigDecimal totalLoanAmount;
    private EnumOptionData status;
    private LocalDateTime newDisbursementDate;
    private String comments;
    private LocalDateTime dateRequested;
    private String requestedByUser;
    private LocalDateTime approvedOnDate;
    private String approvedByUser;
    private LocalDateTime lastModifiedDate;
    private String modifiedByUser;
    private Collection<RestructureCreditsLoanMappingData> loanMappingData;


    private RestructureCreditsRequestData(
            final Long id,final String clientName, final String productName, final BigDecimal totalLoanAmount,
            final EnumOptionData status,final LocalDateTime newDisbursementDate,final String comments,
            final LocalDateTime dateRequested,final String requestedByUser,final LocalDateTime approvedOnDate,
            final String approvedByUser,final LocalDateTime lastModifiedDate,final String modifiedByUser) {

        this.id = id;
        this.clientName = clientName;
        this.productName=productName;
        this.totalLoanAmount=totalLoanAmount;
        this.status=status;
        this.newDisbursementDate=newDisbursementDate;
        this.comments=comments;
        this.dateRequested=dateRequested;
        this.requestedByUser=requestedByUser;
        this.approvedOnDate=approvedOnDate;
        this.approvedByUser=approvedByUser;
        this.lastModifiedDate=lastModifiedDate;
        this.modifiedByUser=modifiedByUser;
    }

    /**
     * template of the restructure credits params
     * @return
     */
    public static RestructureCreditsRequestData instance(final Long id,final String clientName, final String productName, final BigDecimal totalLoanAmount,
                                                         final EnumOptionData status,final LocalDateTime newDisbursementDate,final String comments,
                                                         final LocalDateTime dateRequested,final String requestedByUser,final LocalDateTime approvedOnDate,
                                                         final String approvedByUser,final LocalDateTime lastModifiedDate,final String modifiedByUser) {

        return new RestructureCreditsRequestData(id, clientName, productName, totalLoanAmount, status,
                newDisbursementDate, comments, dateRequested, requestedByUser, approvedOnDate, approvedByUser,
                lastModifiedDate, modifiedByUser);
    }

    public Long getId() {
        return id;
    }

    public Collection<RestructureCreditsLoanMappingData> getLoanMappingData() {
        return loanMappingData;
    }

    public void setLoanMappingData(Collection<RestructureCreditsLoanMappingData> loanMappingData) {
        this.loanMappingData = loanMappingData;
    }
}
