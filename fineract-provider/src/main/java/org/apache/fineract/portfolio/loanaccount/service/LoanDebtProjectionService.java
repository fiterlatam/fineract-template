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
package org.apache.fineract.portfolio.loanaccount.service;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.portfolio.loanaccount.data.LoanDebtProjectionData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanDebtProjectionService {

    private final LoanRepositoryWrapper loanRepositoryWrapper;

    public LoanDebtProjectionData calculateDebtProjection(Long loanId, String projectionDate, String dateFormat) {
        // Calculate the debt projection for a loan
        // just mockup first to test
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
        LocalDate projectedFutureDate = DateUtils.parseLocalDate(projectionDate, dateFormat);
        final MonetaryCurrency currency = loan.getCurrency();

        // fetch all repayment schedules that have not be fully paid and will be overdue on the projection date
        List<LoanRepaymentScheduleInstallment> overdueInstallments = loan.getRepaymentScheduleInstallments().stream()
                .filter(installment -> installment.isOverdueOn(projectedFutureDate))
                .filter(installment -> installment.isNotFullyPaidOff())
                .toList();


        // early return if no overdue installments
        if (overdueInstallments.isEmpty()) {
            return new LoanDebtProjectionData(0, new LoanDebtProjectionData.OverdueBalanceDetails(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), new LoanDebtProjectionData.TotalBalanceDetails(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }


        validateLoanForProjection(loan);
        // Calculate Overdue Balance Details
        LoanDebtProjectionData.OverdueBalanceDetails overdueDetails = calculateOverdueBalanceDetails(loan, projectedFutureDate);

        // Calculate Total Balance Details
        LoanDebtProjectionData.TotalBalanceDetails totalDetails = calculateTotalBalanceDetails(loan, projectedFutureDate, overdueDetails);

        // Calculate Projected Overdue Days
        Integer projectedOverdueDays = calculateProjectedOverdueDays(loan, projectedFutureDate);

        return new LoanDebtProjectionData(projectedOverdueDays, overdueDetails, totalDetails);
    }




    private void validateLoanForProjection(Loan loan) {
       // validate that loan is active and not closed
        if (loan.isClosed()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.closed", "Loan is closed and cannot be projected", loan.getId());
        }
    }


    private BigDecimal calculateOverdueInstallmentBalance(
            List<LoanRepaymentScheduleInstallment> overdueInstallments
    ) {

        MonetaryCurrency currency = overdueInstallments.get(0).getLoan().getCurrency();
        return overdueInstallments.stream()
                .map(installment -> {
                    // Sum principal, interest, and other mandatory components
                    return installment.getPrincipal(currency)
                            .add(installment.getInterestCharged(currency))
                            .add(installment.getFeeChargesCharged(currency))
                            .add(installment.getPenaltyChargesCharged(currency));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);



    }

    private LoanDebtProjectionData.TotalBalanceDetails calculateTotalBalanceDetails(
            Loan loan,
            LocalDate projectionDate,
            LoanDebtProjectionData.OverdueBalanceDetails overdueDetails
    ) {
        // Calculate Future Balance
        BigDecimal futureBalance = calculateFutureBalance(loan, projectionDate);

        // Calculate Total Balances
        BigDecimal totalOverdueBalance = calculateTotalOverdueBalance(overdueDetails);
        BigDecimal totalBalance = totalOverdueBalance.add(futureBalance);

        return new LoanDebtProjectionData.TotalBalanceDetails(
                totalOverdueBalance,
                futureBalance,
                totalBalance
        );
    }



    private Integer calculateProjectedOverdueDays(Loan loan, LocalDate projectionDate) {
        // Implementation depends on Fineract's LoanRepaymentScheduleInstallment
        return loan.getLoanRepaymentScheduleInstallments().stream()
                .filter(installment ->
                        installment.getDueDate().isBefore(projectionDate) ||
                        installment.getDueDate().equals(projectionDate)
                )
                .map(installment ->
                        installment.getDueDate().until(projectionDate).getDays()
                )
                .reduce(Integer::sum)
                .orElse(0);
    }


}
