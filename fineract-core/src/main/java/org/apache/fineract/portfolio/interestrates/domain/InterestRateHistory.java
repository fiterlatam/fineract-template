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
package org.apache.fineract.portfolio.interestrates.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.domain.AuditableFieldsConstants;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

@Setter
@Getter
@Entity
@Table(name = "m_interest_rate_history")
public class InterestRateHistory extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "interest_rate_id")
    private InterestRate interestRate;

    @Column(name = "name")
    private String name;

    @Column(name = "current_rate")
    private BigDecimal currentRate;

    @Column(name = "interest_rate_type_id")
    private Integer interestRateTypeId;

    @Column(name = "appliedon_date")
    private LocalDate appliedOnDate;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = AuditableFieldsConstants.CREATED_BY_DB_FIELD)
    private Long createdBy;

    @Column(name = AuditableFieldsConstants.CREATED_DATE_DB_FIELD)
    private OffsetDateTime createdDate;

    @Column(name = "min_rate")
    private BigDecimal minRate;

    @Column(name = "max_rate")
    private BigDecimal maxRate;

    @SuppressWarnings("all")
    public static InterestRateHistory createNew(final InterestRate interestRate) {
        final InterestRateHistory interestRateHistory = new InterestRateHistory();
        interestRateHistory.name = interestRate.getName();
        interestRateHistory.interestRate = interestRate;
        interestRateHistory.active = interestRate.isActive();
        interestRateHistory.interestRateTypeId = interestRate.getInterestRateTypeId();
        interestRateHistory.currentRate = interestRate.getCurrentRate();
        interestRateHistory.appliedOnDate = interestRate.getAppliedOnDate();
        interestRateHistory.minRate = interestRate.getMinRate();
        interestRateHistory.maxRate = interestRate.getMaxRate();
        interestRateHistory.createdBy = interestRate.getCreatedBy()
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.interest.rate.history.createdBy.not.found",
                        "Created by not found", interestRate.getId()));
        interestRateHistory.createdDate = OffsetDateTime.now();
        return interestRateHistory;
    }
}
