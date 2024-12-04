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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.portfolio.loanaccount.data.LoanDebtProjectionData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanDebtProjectionService {

    private final LoanRepositoryWrapper loanRepositoryWrapper;

    public LoanDebtProjectionData calculateDebtProjection(Long loanId, String projectionDate, String dateFormat) {
        // Find the loan and validate
        Loan loan = validateLoanForProjection(loanId);
        LocalDate projectedFutureDate = DateUtils.parseLocalDate(projectionDate, dateFormat);

        // Get overdue and future installments
        List<LoanRepaymentScheduleInstallment> overdueInstallments = getOverdueInstallments(loan, projectedFutureDate);
        List<LoanRepaymentScheduleInstallment> futureInstallments = getFutureInstallments(loan, projectedFutureDate);

        // Calculate projected overdue days
        Long projectedOverdueDays = calculateProjectedOverdueDays(overdueInstallments);

        // Calculate discriminated past due balance
        LoanDebtProjectionData.OverdueBalanceDetails overdueBalanceDetails = calculateDiscriminatedPastDueBalance(overdueInstallments,
                loan.getCurrency());

        // Calculate total balance details
        LoanDebtProjectionData.TotalBalanceDetails totalBalanceDetails = calculateTotalBalanceDetails(loan, projectedFutureDate,
                overdueBalanceDetails, futureInstallments);

        return new LoanDebtProjectionData(projectedOverdueDays, overdueBalanceDetails, totalBalanceDetails);
    }

    private Loan validateLoanForProjection(Long loanId) {
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

        // Validate loan is active
        if (loan.isClosed()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.is.closed", "Loan is closed and cannot be projected",
                    loan.getId());
        }

        return loan;
    }

    private List<LoanRepaymentScheduleInstallment> getOverdueInstallments(Loan loan, LocalDate projectedFutureDate) {
        return loan.getRepaymentScheduleInstallments().stream()
                .filter(installment -> installment.getDueDate().isBefore(projectedFutureDate)
                        || installment.getDueDate().equals(projectedFutureDate))
                .filter(LoanRepaymentScheduleInstallment::isNotFullyPaidOff).toList();
    }

    private List<LoanRepaymentScheduleInstallment> getFutureInstallments(Loan loan, LocalDate projectedFutureDate) {
        LocalDate currentDate = DateUtils.getLocalDateOfTenant();
        return loan.getRepaymentScheduleInstallments().stream().filter(
                installment -> installment.getDueDate().isAfter(currentDate) && installment.getDueDate().isBefore(projectedFutureDate))
                .toList();
    }

    private LoanDebtProjectionData.OverdueBalanceDetails calculateDiscriminatedPastDueBalance(
            List<LoanRepaymentScheduleInstallment> overdueInstallments, MonetaryCurrency currency) {
        if (overdueInstallments.isEmpty()) {
            return new LoanDebtProjectionData.OverdueBalanceDetails(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Calculate Past Due Installment Balance
        BigDecimal pastDueInstallmentBalance = overdueInstallments.stream()
                .map(installment -> installment.getPrincipal(currency).add(installment.getInterestCharged(currency))
                        .add(installment.getFeeChargesCharged(currency)).add(installment.getPenaltyChargesCharged(currency)).getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        // Calculate Delinquency Interest
        BigDecimal delinquencyInterest = overdueInstallments.stream()
                .map(installment -> installment.getPenaltyChargesCharged(currency).getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Calculate Fee
        BigDecimal fee = overdueInstallments.stream().map(installment -> installment.getFeeChargesCharged(currency).getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        return new LoanDebtProjectionData.OverdueBalanceDetails(pastDueInstallmentBalance, delinquencyInterest, fee);
    }

    private LoanDebtProjectionData.TotalBalanceDetails calculateTotalBalanceDetails(Loan loan, LocalDate projectedFutureDate,
            LoanDebtProjectionData.OverdueBalanceDetails overdueDetails, List<LoanRepaymentScheduleInstallment> futureInstallments) {
        // Calculate Future Balance
        BigDecimal futureBalance = calculateFutureBalance(loan, projectedFutureDate, futureInstallments);

        // Calculate Total Overdue Balance
        BigDecimal totalOverdueBalance = calculateTotalOverdueBalance(overdueDetails);

        // Calculate Total Balance
        BigDecimal totalBalance = totalOverdueBalance.add(futureBalance).setScale(2, RoundingMode.HALF_UP);

        return new LoanDebtProjectionData.TotalBalanceDetails(totalOverdueBalance, futureBalance, totalBalance);
    }

    private BigDecimal calculateFutureBalance(Loan loan, LocalDate projectedFutureDate,
            List<LoanRepaymentScheduleInstallment> futureInstallments) {
        // Calculate principal balance from last due date
        BigDecimal principalBalance = calculatePrincipalBalance(loan);

        // Calculate current interest for additional days
        BigDecimal currentInterest = futureInstallments.stream()
                .map(installment -> calculateCurrentInterest(loan, installment.getDueDate(), projectedFutureDate))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        return principalBalance.add(currentInterest).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePrincipalBalance(Loan loan) {
        // Implement logic to calculate remaining principal balance
        return loan.getLoanSummary().getTotalOutstanding();
    }

    private BigDecimal calculateCurrentInterest(Loan loan, LocalDate lastRepaymentDate, LocalDate projectedFutureDate) {
        // Implement logic to calculate interest for additional days
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateTotalOverdueBalance(LoanDebtProjectionData.OverdueBalanceDetails overdueDetails) {
        return overdueDetails.getTotal();
    }

    private Long calculateProjectedOverdueDays(List<LoanRepaymentScheduleInstallment> overdueInstallments) {
        if (overdueInstallments.isEmpty()) {
            return 0L;
        }
        LocalDate currentDate = DateUtils.getLocalDateOfTenant();
        return overdueInstallments.stream().map(LoanRepaymentScheduleInstallment::getDueDate)
                .map(dueDate -> DateUtils.getDifferenceInDays(currentDate, dueDate)).max(Long::compareTo).orElse(0L);
    }
}
