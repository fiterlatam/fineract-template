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
package org.apache.fineract.portfolio.creditstanding.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.creditstanding.api.CreditStandingApiConstants;
import org.springframework.stereotype.Component;

@Entity
@Component
@Table(name = "m_credit_standing")
public class CreditStanding extends AbstractAuditableCustom {

    @Column(name = "total_credit_line", nullable = false, scale = 6, precision = 19)
    private BigDecimal totalCreditLine;

    @Column(name = "rci_max", scale = 6, precision = 19)
    private BigDecimal rciMax;

    @OneToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    protected CreditStanding() {
        //
    }

    public static CreditStanding fromJson(JsonCommand command, Client client) {
        BigDecimal mra = command.bigDecimalValueOfParameterNamed(CreditStandingApiConstants.mraParamName);
        BigDecimal rciMax = command.bigDecimalValueOfParameterNamed(CreditStandingApiConstants.rciMaxParamName);
        return new CreditStanding(client, mra, rciMax);
    }

    public CreditStanding(Client client, BigDecimal mra, BigDecimal rciMax) {
        this.client = client;
        this.totalCreditLine = mra;
        this.rciMax = rciMax;
    }

    public Map<String, Object> update(final JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(2);

        if (command.isChangeInBigDecimalParameterNamed(CreditStandingApiConstants.mraParamName, this.totalCreditLine)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(CreditStandingApiConstants.mraParamName,
                    command.extractLocale());
            actualChanges.put(CreditStandingApiConstants.mraParamName, newValue);
            this.totalCreditLine = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamed(CreditStandingApiConstants.rciMaxParamName, this.rciMax)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(CreditStandingApiConstants.rciMaxParamName,
                    command.extractLocale());
            actualChanges.put(CreditStandingApiConstants.rciMaxParamName, newValue);
            this.rciMax = newValue;
        }

        return actualChanges;
    }

    public BigDecimal getTotalCreditLine() {
        return totalCreditLine;
    }

    public void setTotalCreditLine(BigDecimal totalCreditLine) {
        this.totalCreditLine = totalCreditLine;
    }

    public BigDecimal getRciMax() {
        return rciMax;
    }

    public void setRciMax(BigDecimal rciMax) {
        this.rciMax = rciMax;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
