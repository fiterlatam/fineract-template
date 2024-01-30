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
package org.apache.fineract.portfolio.group.data;

import java.math.BigDecimal;

/**
 * Immutable data object representing loan summary for a given group
 */
public class GroupLoanSummaryData {

    private Long groupId;
    private String loanShortProductName;
    private BigDecimal totalRepayment;
    private BigDecimal totalOverdue;
    private BigDecimal totalPaymentExpected;
    private Integer clientCounter;

    public GroupLoanSummaryData() {}

    public GroupLoanSummaryData(Long groupId, String loanShortProductName, BigDecimal totalRepayment, BigDecimal totalPaymentExpected,
            BigDecimal totalOverdue, Integer clientCounter) {
        this.groupId = groupId;
        this.loanShortProductName = loanShortProductName;
        this.totalRepayment = totalRepayment;
        this.totalOverdue = totalOverdue;
        this.clientCounter = clientCounter;
        this.totalPaymentExpected = totalPaymentExpected;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public void setLoanShortProductName(String loanShortProductName) {
        this.loanShortProductName = loanShortProductName;
    }

    public void setTotalRepayment(BigDecimal totalRepayment) {
        this.totalRepayment = totalRepayment;
    }

    public void setTotalOverdue(BigDecimal totalOverdue) {
        this.totalOverdue = totalOverdue;
    }

    public void setClientCounter(Integer clientCounter) {
        this.clientCounter = clientCounter;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getLoanShortProductName() {
        return loanShortProductName;
    }

    public BigDecimal getTotalRepayment() {
        return totalRepayment;
    }

    public BigDecimal getTotalOverdue() {
        return totalOverdue;
    }

    public Integer getClientCounter() {
        return clientCounter;
    }

    public BigDecimal getTotalPaymentExpected() {
        return totalPaymentExpected;
    }

    public void setTotalPaymentExpected(BigDecimal totalPaymentExpected) {
        this.totalPaymentExpected = totalPaymentExpected;
    }
}
