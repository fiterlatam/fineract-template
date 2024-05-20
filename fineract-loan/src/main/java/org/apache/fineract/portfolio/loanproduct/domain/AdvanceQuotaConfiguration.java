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

@Setter
@Entity
@Table(name = "m_advance_quota_configuration")
public class AdvanceQuotaConfiguration extends AbstractPersistableCustom {

    @Column(name = "percentage_value")
    private BigDecimal percentageValue;

    @Column(name = "is_enabled")
    private Boolean enabled;

    @Column(name = "modifiedon_date")
    private LocalDate modifiedOnDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifiedon_userid")
    private AppUser modifiedBy;

    public Map<String, Object> update(final JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();
        final String percentageValue = "percentageValue";
        if (command.isChangeInBigDecimalParameterNamed(percentageValue, this.percentageValue)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(percentageValue);
            actualChanges.put(percentageValue, newValue);
            this.percentageValue = newValue;
        }
        final String enabled = "enabled";
        if (command.isChangeInBooleanParameterNamed(enabled, this.enabled)) {
            final Boolean newValue = command.booleanObjectValueOfParameterNamed(enabled);
            actualChanges.put(enabled, newValue);
            this.enabled = newValue;
        }
        return actualChanges;
    }

}
