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
package org.apache.fineract.portfolio.loanaccount.loanschedule.data;

import java.math.BigDecimal;
import java.util.Objects;

@lombok.Builder
@lombok.Getter
public class OverdueLoanScheduleData {

    private final Long loanId;
    private final Long chargeId;
    private final Long installmentId;
    private final String locale;
    private final BigDecimal amount;
    private final String dateFormat;
    private final String dueDate;
    private final BigDecimal principalOverdue;
    private final BigDecimal interestOverdue;
    private final Integer periodNumber;

    @Override
    public String toString() {
        return "{" + "chargeId:" + this.chargeId + ", locale:'" + this.locale + '\'' + ", amount:" + this.amount + ", dateFormat:'"
                + this.dateFormat + '\'' + ", dueDate:'" + this.dueDate + '\'' + ", principal:'" + this.principalOverdue + '\''
                + ", interest:'" + this.interestOverdue + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OverdueLoanScheduleData that = (OverdueLoanScheduleData) o;
        return Objects.equals(loanId, that.loanId) && Objects.equals(chargeId, that.chargeId)
                && Objects.equals(installmentId, that.installmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId, chargeId, installmentId);
    }
}
