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

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.springframework.stereotype.Component;

/**
 * A wrapper for dealing with side-effect free functionality related to a loans transactions and repayment schedule.
 */
@Component
public final class LoanSummaryWrapper {

    public Money calculateTotalPrincipalRepaid(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getPrincipalCompleted(currency));
        }
        return total;
    }

    public Money calculateTotalPrincipalWrittenOff(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getPrincipalWrittenOff(currency));
        }
        return total;
    }

    public Money calculateTotalPrincipalOverdueOn(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency, final LocalDate overdueAsOf) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            if (installment.isOverdueOn(overdueAsOf)) {
                total = total.plus(installment.getPrincipalOutstanding(currency));
            }
        }
        return total;
    }

    public Money calculateTotalInterestCharged(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getInterestCharged(currency));
        }
        return total;
    }

    public Money calculateTotalInterestRepaid(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getInterestPaid(currency));
        }
        return total;
    }

    public Money calculateTotalInterestWaived(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getInterestWaived(currency));
        }
        return total;
    }

    public Money calculateTotalInterestWrittenOff(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getInterestWrittenOff(currency));
        }
        return total;
    }

    public Money calculateTotalInterestOverdueOn(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency, final LocalDate overdueAsOf) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            if (installment.isOverdueOn(overdueAsOf)) {
                total = total.plus(installment.getInterestOutstanding(currency));
            }
        }
        return total;
    }

    public Money calculateTotalFeeChargesCharged(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getFeeChargesCharged(currency));
        }
        return total;
    }

    public Money calculateTotalFeeChargesRepaid(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getFeeChargesPaid(currency));
        }
        return total;
    }

    public Money calculateTotalFeeChargesWaived(Set<LoanCharge> charges, final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanCharge charge : charges) {
            if (charge.isActive() && !charge.isPenaltyCharge()) {
                total = total.plus(charge.getAmountWaived(currency));
            }
        }
        return total;
    }

    public Money calculateTotalFeeChargesWrittenOff(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getFeeChargesWrittenOff(currency));
        }
        return total;
    }

    public Money calculateTotalFeeChargesOverdueOn(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency, final LocalDate overdueAsOf) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            if (installment.isOverdueOn(overdueAsOf)) {
                total = total.plus(installment.getFeeChargesOutstanding(currency));
            }
        }
        return total;
    }

    public Money calculateTotalPenaltyChargesCharged(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getPenaltyChargesCharged(currency));
        }
        return total;
    }

    public Money calculateTotalPenaltyChargesRepaid(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getPenaltyChargesPaid(currency));
        }
        return total;
    }

    public Money calculateTotalPenaltyChargesWaived(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getPenaltyChargesWaived(currency));
        }
        return total;
    }

    public Money calculateTotalPenaltyChargesWrittenOff(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getPenaltyChargesWrittenOff(currency));
        }
        return total;
    }

    public Money calculateTotalPenaltyChargesOverdueOn(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency, final LocalDate overdueAsOf) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            if (installment.isOverdueOn(overdueAsOf)) {
                total = total.plus(installment.getPenaltyChargesOutstanding(currency));
            }
        }
        return total;
    }

    public Money calculateTotalOverdueOn(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency, final LocalDate overdueAsOf) {

        final Money principalOverdue = calculateTotalPrincipalOverdueOn(repaymentScheduleInstallments, currency, overdueAsOf);
        final Money interestOverdue = calculateTotalInterestOverdueOn(repaymentScheduleInstallments, currency, overdueAsOf);
        final Money feeChargesOverdue = calculateTotalFeeChargesOverdueOn(repaymentScheduleInstallments, currency, overdueAsOf);
        final Money penaltyChargesOverdue = calculateTotalPenaltyChargesOverdueOn(repaymentScheduleInstallments, currency, overdueAsOf);

        return principalOverdue.plus(interestOverdue).plus(feeChargesOverdue).plus(penaltyChargesOverdue);
    }

    public LocalDate determineOverdueSinceDateFrom(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency, final LocalDate from) {

        LocalDate overdueSince = null;
        final Money totalOverdue = calculateTotalOverdueOn(repaymentScheduleInstallments, currency, from);
        if (totalOverdue.isGreaterThanZero()) {
            for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
                if (installment.isOverdueOn(from)) {
                    if (overdueSince == null || overdueSince.isAfter(installment.getDueDate())) {
                        overdueSince = installment.getDueDate();
                    }
                }
            }
        }

        return overdueSince;
    }

    public Money calculateTotalChargesRepaidAtDisbursement(Set<LoanCharge> charges, MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        if (charges == null) {
            return total;
        }
        for (final LoanCharge loanCharge : charges) {
            if (!loanCharge.isPenaltyCharge() && loanCharge.getAmountPaid(currency).isGreaterThanZero()) {
                total = total.plus(loanCharge.getAmountPaid(currency));
            }
        }
        return total;

    }

    public Money calculateTotalVatOnInterest(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getVatOnInterestCharged(currency));
        }
        return total;
    }

    public Money calculateTotalVatOnInterestPaid(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getVatOnInterestPaid(currency));
        }
        return total;
    }

    public Money calculateTotalVatOnInterestWaived(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getVatOnInterestWaived(currency));
        }
        return total;
    }

    public Money calculateTotalVatOnInterestWrittenOff(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getVatOnInterestWrittenOff(currency));
        }
        return total;
    }

    public Money calculateTotalVatOnCharges(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getVatOnChargeExpected(currency));
        }
        return total;
    }

    public Money calculateTotalVatOnChargesPaid(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getVatOnChargePaid(currency));
        }
        return total;
    }

    public Money calculateTotalVatOnChargesWaived(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getVatOnChargeWaived(currency));
        }
        return total;
    }

    public Money calculateTotalVatOnChargesWrittenOff(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getVatOnChargeWrittenOff(currency));
        }
        return total;
    }

    public Money calculateTotalVatOnInterestOverdue(List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        LocalDate businessDate = DateUtils.getBusinessLocalDate();

        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            if (installment.getDueDate().isBefore(businessDate) && installment.getVatOnInterestOutstanding(currency).isGreaterThanZero()) {
                total = total.plus(installment.getVatOnInterestOutstanding(currency));
            }
        }
        return total;
    }

    public Money calculateTotalVatOnChargeOverdue(List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        LocalDate businessDate = DateUtils.getBusinessLocalDate();

        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            if (installment.getDueDate().isBefore(businessDate)) {
                if (installment.getVatOnChargeExpected(currency).isGreaterThanZero()) {
                    total = total.plus(installment.getVatOnChargeExpected(currency));
                }
            }
        }
        return total;
    }

    public Money calculateTotalPenaltyChargesOverdue(List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        LocalDate businessDate = DateUtils.getBusinessLocalDate();

        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            if (installment.getDueDate().isBefore(businessDate)) {
                if (installment.getPenaltyChargesCharged(currency).isGreaterThanZero()) {
                    total = total.plus(installment.getPenaltyChargesCharged(currency));
                }
            }
        }
        return total;
    }

    public Money calculateTotalVatOnPenaltyCharges(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getVatOnPenaltyChargeExpected(currency));
        }
        return total;
    }

    public Money calculateTotalVatOnPenaltyChargesPaid(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getVatOnPenaltyChargePaid(currency));
        }
        return total;
    }

    public Money calculateTotalVatOnPenaltyChargesWaived(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getVatOnPenaltyChargeWaived(currency));
        }
        return total;
    }

    public Money calculateTotalVatOnPenaltyChargesWrittenOff(final List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            final MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            total = total.plus(installment.getVatOnPenaltyChargeWrittenOff(currency));
        }
        return total;
    }

    public Money calculateTotalVatOnPenaltyChargeOverdue(List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments,
            MonetaryCurrency currency) {
        Money total = Money.zero(currency);
        LocalDate businessDate = DateUtils.getBusinessLocalDate();

        for (final LoanRepaymentScheduleInstallment installment : repaymentScheduleInstallments) {
            if (installment.getDueDate().isBefore(businessDate)) {
                if (installment.getVatOnPenaltyChargeExpected(currency).isGreaterThanZero()) {
                    total = total.plus(installment.getVatOnPenaltyChargeExpected(currency));
                }
            }
        }
        return total;
    }
}
