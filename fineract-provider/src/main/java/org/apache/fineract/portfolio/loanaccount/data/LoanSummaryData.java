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
import org.apache.fineract.organisation.monetary.data.CurrencyData;

/**
 * Immutable data object representing loan summary information.
 */
@SuppressWarnings("unused")
public class LoanSummaryData {

    private final CurrencyData currency;
    private final BigDecimal principalDisbursed;
    private final BigDecimal principalPaid;
    private final BigDecimal principalWrittenOff;
    private final BigDecimal principalOutstanding;
    private final BigDecimal principalOverdue;
    private final BigDecimal interestCharged;
    private final BigDecimal interestPaid;
    private final BigDecimal interestWaived;
    private final BigDecimal interestWrittenOff;
    private final BigDecimal interestOutstanding;
    private final BigDecimal interestOverdue;
    private final BigDecimal feeChargesCharged;
    private final BigDecimal feeChargesDueAtDisbursementCharged;
    private final BigDecimal feeChargesPaid;
    private final BigDecimal feeChargesWaived;
    private final BigDecimal feeChargesWrittenOff;
    private final BigDecimal feeChargesOutstanding;
    private final BigDecimal feeChargesOverdue;
    private final BigDecimal penaltyChargesCharged;
    private final BigDecimal penaltyChargesPaid;
    private final BigDecimal penaltyChargesWaived;
    private final BigDecimal penaltyChargesWrittenOff;
    private final BigDecimal penaltyChargesOutstanding;
    private final BigDecimal penaltyChargesOverdue;
    private final BigDecimal totalExpectedRepayment;
    private final BigDecimal totalRepayment;
    private final BigDecimal totalExpectedCostOfLoan;
    private final BigDecimal totalCostOfLoan;
    private final BigDecimal totalWaived;
    private final BigDecimal totalWrittenOff;
    private final BigDecimal totalOutstanding;
    private final BigDecimal totalOverdue;
    private final BigDecimal totalRecovered;
    private final LocalDate overdueSinceDate;
    private final Long writeoffReasonId;
    private final String writeoffReason;
    private final BigDecimal totalVatOnInterestCharged;
    private final BigDecimal totalVatOnInterestPaid;
    private final BigDecimal totalVatOnInterestWrittenOff;
    private final BigDecimal totalVatOnInterestWaived;
    private final BigDecimal totalVatOnInterestOutstanding;
    private final BigDecimal totalVatOnInterestOverdue;

    private final BigDecimal totalVatOnChargeExpected;
    private final BigDecimal totalVatOnChargePaid;
    private final BigDecimal totalVatOnChargeWrittenOff;
    private final BigDecimal totalVatOnChargeWaived;
    private final BigDecimal totalVatOnChargeOutstanding;
    private final BigDecimal totalVatOnChargeOverdue;

    private final BigDecimal totalVatOnPenaltyChargeExpected;
    private final BigDecimal totalVatOnPenaltyChargePaid;
    private final BigDecimal totalVatOnPenaltyChargeWrittenOff;
    private final BigDecimal totalVatOnPenaltyChargeWaived;
    private final BigDecimal totalVatOnPenaltyChargeOutstanding;
    private final BigDecimal totalVatOnPenaltyChargeOverdue;
    private final BigDecimal originationFees;

    private final BigDecimal interestVatOverdue;

    public LoanSummaryData(final CurrencyData currency, final BigDecimal principalDisbursed, final BigDecimal principalPaid,
            final BigDecimal principalWrittenOff, final BigDecimal principalOutstanding, final BigDecimal principalOverdue,
            final BigDecimal interestCharged, final BigDecimal interestPaid, final BigDecimal interestWaived,
            final BigDecimal interestWrittenOff, final BigDecimal interestOutstanding, final BigDecimal interestOverdue,
            final BigDecimal feeChargesCharged, final BigDecimal feeChargesDueAtDisbursementCharged, final BigDecimal feeChargesPaid,
            final BigDecimal feeChargesWaived, final BigDecimal feeChargesWrittenOff, final BigDecimal feeChargesOutstanding,
            final BigDecimal feeChargesOverdue, final BigDecimal penaltyChargesCharged, final BigDecimal penaltyChargesPaid,
            final BigDecimal penaltyChargesWaived, final BigDecimal penaltyChargesWrittenOff, final BigDecimal penaltyChargesOutstanding,
            final BigDecimal penaltyChargesOverdue, final BigDecimal totalExpectedRepayment, final BigDecimal totalRepayment,
            final BigDecimal totalExpectedCostOfLoan, final BigDecimal totalCostOfLoan, final BigDecimal totalWaived,
            final BigDecimal totalWrittenOff, final BigDecimal totalOutstanding, final BigDecimal totalOverdue,
            final LocalDate overdueSinceDate, final Long writeoffReasonId, final String writeoffReason, final BigDecimal totalRecovered,
            final BigDecimal totalVatOnInterestCharged, final BigDecimal totalVatOnInterestPaid,
            final BigDecimal totalVatOnInterestWrittenOff, final BigDecimal totalVatOnInterestWaived,
            final BigDecimal totalVatOnInterestOutstanding, final BigDecimal totalVatOnInterestOverdue,
            final BigDecimal totalVatOnChargeExpected, final BigDecimal totalVatOnChargePaid, final BigDecimal totalVatOnChargeWrittenOff,
            final BigDecimal totalVatOnChargeWaived, final BigDecimal totalVatOnChargeOutstanding, final BigDecimal totalVatOnChargeOverdue,
            final BigDecimal originationFees, final BigDecimal interestVatOverdue, final BigDecimal totalVatOnPenaltyChargeExpected,
            final BigDecimal totalVatOnPenaltyChargePaid, final BigDecimal totalVatOnPenaltyChargeWrittenOff,
            final BigDecimal totalVatOnPenaltyChargeWaived, final BigDecimal totalVatOnPenaltyChargeOutstanding,
            final BigDecimal totalVatOnPenaltyChargeOverdue) {
        this.currency = currency;
        this.principalDisbursed = principalDisbursed;
        this.principalPaid = principalPaid;
        this.principalWrittenOff = principalWrittenOff;
        this.principalOutstanding = principalOutstanding;
        this.principalOverdue = principalOverdue;
        this.interestCharged = interestCharged;
        this.interestPaid = interestPaid;
        this.interestWaived = interestWaived;
        this.interestWrittenOff = interestWrittenOff;
        this.interestOutstanding = interestOutstanding;
        this.interestOverdue = interestOverdue;
        this.feeChargesCharged = feeChargesCharged;
        this.feeChargesDueAtDisbursementCharged = feeChargesDueAtDisbursementCharged;
        this.feeChargesPaid = feeChargesPaid;
        this.feeChargesWaived = feeChargesWaived;
        this.feeChargesWrittenOff = feeChargesWrittenOff;
        this.feeChargesOutstanding = feeChargesOutstanding;
        this.feeChargesOverdue = feeChargesOverdue;
        this.penaltyChargesCharged = penaltyChargesCharged;
        this.penaltyChargesPaid = penaltyChargesPaid;
        this.penaltyChargesWaived = penaltyChargesWaived;
        this.penaltyChargesWrittenOff = penaltyChargesWrittenOff;
        this.penaltyChargesOutstanding = penaltyChargesOutstanding;
        this.penaltyChargesOverdue = penaltyChargesOverdue;
        this.totalExpectedRepayment = totalExpectedRepayment;
        this.totalRepayment = totalRepayment;
        this.totalExpectedCostOfLoan = totalExpectedCostOfLoan;
        this.totalCostOfLoan = totalCostOfLoan;
        this.totalWaived = totalWaived;
        this.totalWrittenOff = totalWrittenOff;
        this.totalOutstanding = totalOutstanding;
        this.totalOverdue = totalOverdue;
        this.overdueSinceDate = overdueSinceDate;
        this.writeoffReasonId = writeoffReasonId;
        this.writeoffReason = writeoffReason;
        this.totalRecovered = totalRecovered;
        this.totalVatOnInterestCharged = totalVatOnInterestCharged;
        this.totalVatOnInterestPaid = totalVatOnInterestPaid;
        this.totalVatOnInterestWrittenOff = totalVatOnInterestWrittenOff;
        this.totalVatOnInterestWaived = totalVatOnInterestWaived;
        this.totalVatOnInterestOutstanding = totalVatOnInterestOutstanding;
        this.totalVatOnInterestOverdue = totalVatOnInterestOverdue;
        this.totalVatOnChargeExpected = totalVatOnChargeExpected;
        this.totalVatOnChargePaid = totalVatOnChargePaid;
        this.totalVatOnChargeWrittenOff = totalVatOnChargeWrittenOff;
        this.totalVatOnChargeWaived = totalVatOnChargeWaived;
        this.totalVatOnChargeOutstanding = totalVatOnChargeOutstanding;
        this.totalVatOnChargeOverdue = totalVatOnChargeOverdue;
        this.originationFees = originationFees;
        this.interestVatOverdue = interestVatOverdue;

        this.totalVatOnPenaltyChargeExpected = totalVatOnPenaltyChargeExpected;
        this.totalVatOnPenaltyChargePaid = totalVatOnPenaltyChargePaid;
        this.totalVatOnPenaltyChargeWrittenOff = totalVatOnPenaltyChargeWrittenOff;
        this.totalVatOnPenaltyChargeWaived = totalVatOnPenaltyChargeWaived;
        this.totalVatOnPenaltyChargeOutstanding = totalVatOnPenaltyChargeOutstanding;
        this.totalVatOnPenaltyChargeOverdue = totalVatOnPenaltyChargeOverdue;
    }

    public BigDecimal getTotalOutstanding() {
        return this.totalOutstanding;
    }

    public BigDecimal getTotalPaidFeeCharges() {
        return feeChargesPaid;
    }

    public BigDecimal getOriginationFees() {
        return originationFees;
    }

}
