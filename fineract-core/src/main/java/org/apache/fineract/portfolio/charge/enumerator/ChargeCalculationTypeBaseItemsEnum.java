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

package org.apache.fineract.portfolio.charge.enumerator;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ChargeCalculationTypeBaseItemsEnum {

    FLAT(0, "Flat", "FLAT", "flat"), DISBURSED_AMOUNT(1, "Disbursed Amount", "DISB", "disbursedamount"), PRINCIPAL_INSTALLMENT(2,
            "Installment Principal", "IPRIN",
            "installmentprincipal"), INTEREST_INSTALLMENT(3, "Installment Interest", "IINT", "installmentinterest"), OUTSTANDING_PRINCIPAL(
                    4, "Outstanding principal", "OPRIN", "outstandingprincipal"), OUTSTANDING_INTEREST(5, "Outstanding Interest", "OINT",
                            "outstandinginterest"), SEGURO_OBRIGATORIO(6, "Seguro Obrigatorio", "SEGO", "seguroobrigatorio"), AVAL(7,
                                    "Aval", "AVAL", "aval"), HOORARIOS(8, "Hoorarios", "HONO", "honorarios"), PERCENT_OF_ANOTHER_CHARGE(9,
                                            "% Of another charge", "ACHG", "percentofanothercharge"),
                                                        SEGURO_VOLUNTARIO(10, "Seguro voluntario/asistencia", "VOLUNTARIO", "segurovoluntario")   ;

    private int index;
    private String description;
    private String acronym;
    private String code;

    public Boolean isFlat() {
        return this.index == FLAT.index;
    }

    public Boolean isDisbursedAmount() {
        return this.index == DISBURSED_AMOUNT.index;
    }

    public Boolean isPrincipalInstallment() {
        return this.index == PRINCIPAL_INSTALLMENT.index;
    }

    public Boolean isInterestInstallment() {
        return this.index == INTEREST_INSTALLMENT.index;
    }

    public Boolean isOutstandingPrincipal() {
        return this.index == OUTSTANDING_PRINCIPAL.index;
    }

    public Boolean isOutstandingInterest() {
        return this.index == OUTSTANDING_INTEREST.index;
    }

    public Boolean isSeguroObrigatorio() {
        return this.index == SEGURO_OBRIGATORIO.index;
    }

    public Boolean isAval() {
        return this.index == AVAL.index;
    }

    public Boolean isHonorarios() {
        return this.index == HOORARIOS.index;
    }

    public Boolean isPercentOfAnotherCharge() {
        return this.index == PERCENT_OF_ANOTHER_CHARGE.index;
    }
    public Boolean isSeguroVoluntario() {
        return this.index == SEGURO_VOLUNTARIO.index;
    }
}
