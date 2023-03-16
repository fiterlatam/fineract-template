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

package org.apache.fineract.portfolio.creditstanding.data;

import java.io.Serializable;
import java.math.BigDecimal;

public class CreditStandingData implements Serializable {

    private Long id;
    private Long clientId;
    private BigDecimal mra;
    private BigDecimal mraAvailable;
    private BigDecimal rciMax;
    private BigDecimal monthlyCommitment;
    private BigDecimal totalDebt;
    private BigDecimal currentDebt;
    private BigDecimal expiredDebt;
    private Integer delayInDays;

    public static CreditStandingData instance(Long id, Long clientId, BigDecimal mra, BigDecimal rciMax) {
        return new CreditStandingData(id, clientId, mra, null, rciMax, null, null, null, null, null);
    }

    public CreditStandingData(Long id, Long clientId, BigDecimal mra, BigDecimal mraAvailable, BigDecimal rciMax,
            BigDecimal monthlyCommitment, BigDecimal totalDebt, BigDecimal currentDebt, BigDecimal expiredDebt, Integer delayInDays) {
        this.id = id;
        this.clientId = clientId;
        this.mra = mra;
        this.mraAvailable = mraAvailable;
        this.rciMax = rciMax;
        this.monthlyCommitment = monthlyCommitment;
        this.totalDebt = totalDebt;
        this.currentDebt = currentDebt;
        this.expiredDebt = expiredDebt;
        this.delayInDays = delayInDays;
    }

    public BigDecimal getMraAvailable() {
        return mraAvailable;
    }

    public void setMraAvailable(BigDecimal mraAvailable) {
        this.mraAvailable = mraAvailable;
    }

    public BigDecimal getMonthlyCommitment() {
        return monthlyCommitment;
    }

    public void setMonthlyCommitment(BigDecimal monthlyCommitment) {
        this.monthlyCommitment = monthlyCommitment;
    }

    public BigDecimal getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(BigDecimal totalDebt) {
        this.totalDebt = totalDebt;
    }

    public BigDecimal getCurrentDebt() {
        return currentDebt;
    }

    public void setCurrentDebt(BigDecimal currentDebt) {
        this.currentDebt = currentDebt;
    }

    public BigDecimal getExpiredDebt() {
        return expiredDebt;
    }

    public void setExpiredDebt(BigDecimal expiredDebt) {
        this.expiredDebt = expiredDebt;
    }

    public Integer getDelayInDays() {
        return delayInDays;
    }

    public void setDelayInDays(Integer delayInDays) {
        this.delayInDays = delayInDays;
    }
}
