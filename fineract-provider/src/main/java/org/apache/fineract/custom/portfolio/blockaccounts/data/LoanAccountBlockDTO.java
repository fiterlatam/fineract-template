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
package org.apache.fineract.custom.portfolio.blockaccounts.data;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanAccountBlockDTO {

    private Long id;
    private Long loanId;
    private Long blockingReasonId;
    private String blockingReasonName;
    private LocalDate applicationDate;
    private Boolean accelerate;
    private Boolean freezeCurrentInterest;
    private Boolean freezeInterestArrears;
    private Boolean freezeLifeInsurance;
    private Boolean freezeMypime;
    private Boolean active;
    private String formattedLastModifiedDate;
    private String createdByName;
    private String actionName;
}
