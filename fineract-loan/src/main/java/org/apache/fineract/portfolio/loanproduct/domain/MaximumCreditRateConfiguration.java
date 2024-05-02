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
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.useradministration.domain.AppUser;

/**
 * Entity for setting Maximum Credit Rate Configuration
 *
 */

@Setter
@Entity
@Table(name = "m_maximum_credit_rate_configuration")
public class MaximumCreditRateConfiguration extends AbstractPersistableCustom {

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

    public Map<String, Object> update(final JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();
        final String eaRate = "eaRate";
        if (command.isChangeInBigDecimalParameterNamed(eaRate, this.eaRate)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(eaRate);
            actualChanges.put(eaRate, newValue);
            this.eaRate = newValue;
        }
        final String annualNominalRate = "annualNominalRate";
        if (command.isChangeInBigDecimalParameterNamed(eaRate, this.annualNominalRate)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(annualNominalRate);
            actualChanges.put(annualNominalRate, newValue);
            this.annualNominalRate = newValue;
        }
        final String monthlyNominalRate = "monthlyNominalRate";
        if (command.isChangeInBigDecimalParameterNamed(eaRate, this.monthlyNominalRate)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(monthlyNominalRate);
            actualChanges.put(monthlyNominalRate, newValue);
            this.monthlyNominalRate = newValue;
        }
        final String dailyNominalRate = "dailyNominalRate";
        if (command.isChangeInBigDecimalParameterNamed(eaRate, this.dailyNominalRate)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(dailyNominalRate);
            actualChanges.put(dailyNominalRate, newValue);
            this.dailyNominalRate = newValue;
        }
        final String appliedOnDate = "appliedOnDate";
        if (command.isChangeInLocalDateParameterNamed(appliedOnDate, this.appliedOnDate)) {
            final LocalDate newValue = command.localDateValueOfParameterNamed(appliedOnDate);
            actualChanges.put(appliedOnDate, newValue);
            this.appliedOnDate = newValue;
        }
        return actualChanges;
    }

}
