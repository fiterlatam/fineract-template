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
package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanCreditNoteData {

    private Long id;
    private Long loanId;
    private LocalDate creditNoteDate;
    private BigDecimal arrearInterest;
    private BigDecimal currentInterest;
    private BigDecimal honorarios;
    private BigDecimal aval;
    private BigDecimal insurance;
    private BigDecimal mandatoryInsurance;
    private BigDecimal capital;
    private BigDecimal totalAmount;
    private Long documentId;
    private String documentName;
    private Long transactionId;

}
