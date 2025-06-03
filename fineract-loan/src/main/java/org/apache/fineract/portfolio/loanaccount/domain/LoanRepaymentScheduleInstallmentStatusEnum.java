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
package org.apache.fineract.portfolio.loanaccount.domain;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoanRepaymentScheduleInstallmentStatusEnum {

    NOT_APPLICABLE("NA", "installment.status.na", "Not Applicable"), //
    PENDING("PENDING", "installment.status.pending", "Pending Installment"), //
    PAID("PAID", "installment.status.paid", "Paid Installment"), //
    PARTIALLY_PAID("PARTIALLY PAID", "installment.status.paid.partially", "Partially Paid Instalment"), //
    LATE("LATE", "installment.status.late", "Installment in MORA"), //
    UNDEFINED("UNDEFINED", "na", "Undefined"), //
    ;

    private String status;
    private String code;
    private String description;

    public static LoanRepaymentScheduleInstallmentStatusEnum findByStatus(String status) {
        return Arrays.asList(LoanRepaymentScheduleInstallmentStatusEnum.values()).stream()
                .filter(obj -> obj.getStatus().equalsIgnoreCase(status)).findFirst().orElse(UNDEFINED);
    }
}
