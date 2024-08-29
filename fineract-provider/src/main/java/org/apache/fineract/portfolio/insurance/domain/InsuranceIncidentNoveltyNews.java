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
package org.apache.fineract.portfolio.insurance.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.insurance.data.InsuranceIncidentData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "m_insurance_novelty_news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "instance")
public class InsuranceIncidentNoveltyNews extends AbstractPersistableCustom {

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_id", referencedColumnName = "id", nullable = false)
    private Loan loan;

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_charge_id", referencedColumnName = "id", nullable = false)
    private LoanCharge loanCharge;

    @Column(name = "default_from_installment")
    private Integer defaultInstallmentNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "novelty_id", referencedColumnName = "id", nullable = false)
    private InsuranceIncident insuranceIncident;

    @Column(name = "novelty_date")
    private LocalDate noveltyDate;

    @Column(name = "default_amount")
    private BigDecimal defaultAmount;

}
