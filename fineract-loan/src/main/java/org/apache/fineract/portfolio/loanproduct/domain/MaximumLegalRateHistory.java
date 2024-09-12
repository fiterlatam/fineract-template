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
package org.apache.fineract.portfolio.loanproduct.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.domain.AuditableFieldsConstants;
import org.apache.fineract.useradministration.domain.AppUser;

@Setter
@Entity
@Table(name = "m_maximum_legal_rate_history")
public class MaximumLegalRateHistory extends AbstractPersistableCustom {

    @Column(name = "ea_rate")
    private BigDecimal eaRate;

    @Column(name = "annual_nominal_rate")
    private BigDecimal annualNominalRate;

    @Column(name = "monthly_nominal_rate")
    private BigDecimal monthlyNominalRate;

    @Column(name = "daily_nominal_rate")
    private BigDecimal dailyNominalRate;

    @Column(name = "appliedon_date")
    private LocalDate appliedOnDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appliedon_userid")
    private AppUser appliedBy;

    @Column(name = "current_interest_rate")
    private BigDecimal currentInterestRate;

    @Column(name = "overdue_interest_rate")
    private BigDecimal overdueInterestRate;

    @Column(name = AuditableFieldsConstants.CREATED_DATE_DB_FIELD)
    private OffsetDateTime createdDate;

    public static MaximumLegalRateHistory createNew(final MaximumCreditRateConfiguration maximumCreditRateConfiguration) {
        final MaximumLegalRateHistory maximumLegalRateHistory = new MaximumLegalRateHistory();
        maximumLegalRateHistory.eaRate = maximumCreditRateConfiguration.getEaRate();
        maximumLegalRateHistory.annualNominalRate = maximumCreditRateConfiguration.getAnnualNominalRate();
        maximumLegalRateHistory.monthlyNominalRate = maximumCreditRateConfiguration.getMonthlyNominalRate();
        maximumLegalRateHistory.dailyNominalRate = maximumCreditRateConfiguration.getDailyNominalRate();
        maximumLegalRateHistory.appliedOnDate = maximumCreditRateConfiguration.getAppliedOnDate();
        maximumLegalRateHistory.appliedBy = maximumCreditRateConfiguration.getAppliedBy();
        maximumLegalRateHistory.currentInterestRate = maximumCreditRateConfiguration.getCurrentInterestRate();
        maximumLegalRateHistory.overdueInterestRate = maximumCreditRateConfiguration.getOverdueInterestRate();
        maximumLegalRateHistory.createdDate = OffsetDateTime.now();
        return maximumLegalRateHistory;
    }
}
