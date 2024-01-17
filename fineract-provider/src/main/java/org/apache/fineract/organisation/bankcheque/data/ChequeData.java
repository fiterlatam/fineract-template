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
package org.apache.fineract.organisation.bankcheque.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

@Builder
@Data
public class ChequeData {

    private Long id;
    private Long chequeNo;
    private Long batchId;
    private Long batchNo;
    private String bankAccNo;
    private Long bankAccId;
    private Long agencyId;
    private String agencyName;
    private String bankName;
    private EnumOptionData status;
    private String description;
    private LocalDate createdDate;
    private String createdByUsername;
    private LocalDate voidedDate;
    private String voidedByUsername;
    private LocalDate voidAuthorizedDate;
    private String voidAuthorizedByUsername;
    private LocalDate printedDate;
    private String printedByUsername;
    private String lastModifiedByUsername;
    private LocalDate usedOnDate;
    private BigDecimal guaranteeAmount;
    private BigDecimal loanAmount;
    private String clientName;
    private String clientNo;
    private String groupName;
    private String groupNo;
    private String loanAccNo;
    private Long loanAccId;
    private String caseId;
    private Long guaranteeId;
    private BigDecimal chequeAmount;
    private Boolean reassingedCheque;
    private String depositNumber;
}
