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
package org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.impl;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRelationTypeEnum.CHARGEBACK;
import static org.apache.fineract.portfolio.loanproduct.domain.AllocationType.*;
import static org.apache.fineract.portfolio.loanproduct.domain.DueType.IN_ADVANCE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.loanaccount.domain.*;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.AbstractLoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.MoneyHolder;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleProcessingType;
import org.apache.fineract.portfolio.loanproduct.domain.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class AdvancedPaymentScheduleTransactionProcessor extends AbstractLoanRepaymentScheduleTransactionProcessor {

    public static final String ADVANCED_PAYMENT_ALLOCATION_STRATEGY = "advanced-payment-allocation-strategy";
    public static final String INSURANCE_PARAM = "insurance";
    public static final String GUARANTOR_PARAM = "guarantor";
    public static final String MANDATORY_INSURANCE_PARAM = "MandatoryInsurance";
    public static final String HONORARIOS_PARAM = "Honorarios";

    @Override
    public String getCode() {
        return ADVANCED_PAYMENT_ALLOCATION_STRATEGY;
    }

    @Override
    public String getName() {
        return "Advanced payment allocation strategy";
    }

    @Override
    protected Money handleTransactionThatIsALateRepaymentOfInstallment(LoanRepaymentScheduleInstallment currentInstallment,
            List<LoanRepaymentScheduleInstallment> installments, LoanTransaction loanTransaction, Money transactionAmountUnprocessed,
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings, Set<LoanCharge> charges) {
        Money overpayment = Money.of(transactionAmountUnprocessed.getCurrency(), BigDecimal.ZERO);
        MoneyHolder overpaymentHolder = new MoneyHolder(overpayment);
        return processLatestTransaction(loanTransaction,
                new TransactionCtx(transactionAmountUnprocessed.getCurrency(), installments, charges, overpaymentHolder));
    }

    @Override
    protected Money handleTransactionThatIsPaymentInAdvanceOfInstallment(LoanRepaymentScheduleInstallment currentInstallment,
            List<LoanRepaymentScheduleInstallment> installments, LoanTransaction loanTransaction, Money paymentInAdvance,
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings, Set<LoanCharge> charges) {
        loanTransaction.updateComponents(paymentInAdvance, Money.zero(paymentInAdvance.getCurrency()),
                Money.zero(paymentInAdvance.getCurrency()), Money.zero(paymentInAdvance.getCurrency()));
        return paymentInAdvance;
    }

    @Override
    public Money handleTransactionThatIsOnTimePaymentOfInstallment(LoanRepaymentScheduleInstallment currentInstallment,
            LoanTransaction loanTransaction, Money transactionAmountUnprocessed,
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings, Set<LoanCharge> charges) {

        return new CreocoreLoanRepaymentScheduleTransactionProcessor().handleTransactionThatIsOnTimePaymentOfInstallment(currentInstallment,
                loanTransaction, transactionAmountUnprocessed, transactionMappings, charges);
    }

    @Override
    protected Money handleRefundTransactionPaymentOfInstallment(LoanRepaymentScheduleInstallment currentInstallment,
            LoanTransaction loanTransaction, Money transactionAmountUnprocessed,
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings) {
        throw new NotImplementedException();
    }

    @Override
    public Money handleRepaymentSchedule(List<LoanTransaction> transactionsPostDisbursement, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, Set<LoanCharge> loanCharges) {
        Money unProcessed = Money.zero(currency);
        for (final LoanTransaction loanTransaction : transactionsPostDisbursement) {
            if ((loanTransaction.isRepaymentLikeType() || loanTransaction.isInterestWaiver() || loanTransaction.isRecoveryRepayment())
                    && !loanTransaction.isSpecialWriteOff()) {
                loanTransaction.resetDerivedComponents();
            }

            if (loanTransaction.isInterestWaiver()) {
                processTransaction(loanTransaction, currency, installments, loanCharges, null);
            } else {
                unProcessed = processTransaction(loanTransaction, currency, installments, loanCharges, null);
            }
        }
        return unProcessed;
    }

    @Override
    protected boolean isTransactionInAdvanceOfInstallment(final int installmentIndex,
            final List<LoanRepaymentScheduleInstallment> installments, final LocalDate transactionDate) {
        final LoanRepaymentScheduleInstallment currentInstallment = installments.get(installmentIndex);
        return DateUtils.isBefore(transactionDate, currentInstallment.getFromDate());
    }

    @Override
    public ChangedTransactionDetail reprocessLoanTransactions(LocalDate disbursementDate, List<LoanTransaction> loanTransactions,
            MonetaryCurrency currency, List<LoanRepaymentScheduleInstallment> installments, Set<LoanCharge> charges) {
        if (charges != null) {
            for (final LoanCharge loanCharge : charges) {
                if (!loanCharge.isDueAtDisbursement()) {
                    loanCharge.resetPaidAmount(currency);
                    loanCharge.resetWrittenOffAmount(currency);
                }
            }
        }

        addChargeOnlyRepaymentInstallmentIfRequired(charges, installments);

        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
            currentInstallment.resetDerivedComponents();
            currentInstallment.updateDerivedFields(currency, disbursementDate);
            List<LoanInstallmentCharge> installmentCharges = currentInstallment.getInstallmentChargesSorted();
            BigDecimal amount = BigDecimal.ZERO;
            for (LoanInstallmentCharge installmentCharge : installmentCharges) {
                if (installmentCharge.getLoanCharge().isFeeCharge() && !installmentCharge.getLoanCharge().isDueAtDisbursement()
                        && installmentCharge.getAmount() != null) {
                    amount = amount.add(installmentCharge.getAmount());
                }
            }
            if (charges != null && !charges.isEmpty()) {
                for (final LoanCharge loanCharge : charges) {
                    if (loanCharge.isFlatSpecificDueDateChargeForInstallment(currentInstallment)) {
                        amount = amount.add(loanCharge.amount());
                    }
                }
            }
            currentInstallment.setFeeChargesCharged(amount);
        }

        List<ChargeOrTransaction> chargeOrTransactions = createSortedChargesAndTransactionsList(loanTransactions, charges);

        final ChangedTransactionDetail changedTransactionDetail = new ChangedTransactionDetail();
        MoneyHolder overpaymentHolder = new MoneyHolder(Money.zero(currency));
        for (final ChargeOrTransaction chargeOrTransaction : chargeOrTransactions) {
            chargeOrTransaction.getLoanTransaction().ifPresent(loanTransaction -> processSingleTransaction(loanTransaction, currency,
                    installments, charges, changedTransactionDetail, overpaymentHolder));
        }
        List<LoanTransaction> txs = chargeOrTransactions.stream().map(ChargeOrTransaction::getLoanTransaction).filter(Optional::isPresent)
                .map(Optional::get).toList();
        reprocessInstallments(disbursementDate, txs, installments, currency);
        return changedTransactionDetail;
    }

    @Override
    public Money processLatestTransaction(LoanTransaction loanTransaction, TransactionCtx ctx) {
        Money unprocessed = Money.zero(ctx.getCurrency());
        switch (loanTransaction.getTypeOf()) {
            case DISBURSEMENT -> handleDisbursement();
            case WRITEOFF, CREDIT_NOTE ->
                handleWriteOff(loanTransaction, ctx.getCurrency(), ctx.getInstallments(), ctx.getCharges(), ctx.getOverpaymentHolder());
            case REFUND_FOR_ACTIVE_LOAN -> handleRefund(loanTransaction, ctx.getCurrency(), ctx.getInstallments(), ctx.getCharges());
            case CHARGEBACK -> handleChargeback(loanTransaction, ctx);
            case CREDIT_BALANCE_REFUND ->
                handleCreditBalanceRefund(loanTransaction, ctx.getCurrency(), ctx.getInstallments(), ctx.getOverpaymentHolder());
            case REPAYMENT, MERCHANT_ISSUED_REFUND, PAYOUT_REFUND, GOODWILL_CREDIT, CHARGE_REFUND, CHARGE_ADJUSTMENT, DOWN_PAYMENT,
                    WAIVE_INTEREST, RECOVERY_REPAYMENT ->
                unprocessed = handleRepayment(loanTransaction, ctx.getCurrency(), ctx.getInstallments(), ctx.getCharges(),
                        ctx.getOverpaymentHolder());
            case CHARGE_OFF -> handleChargeOff(loanTransaction, ctx.getCurrency(), ctx.getInstallments());
            case CHARGE_PAYMENT -> handleChargePayment(loanTransaction, ctx.getCurrency(), ctx.getInstallments(), ctx.getCharges(),
                    ctx.getOverpaymentHolder());
            case WAIVE_CHARGES -> log.debug("WAIVE_CHARGES transaction will not be processed.");
            case ACCRUAL -> log.debug("ACCRUAL transaction will not be processed.");
            default -> log.warn("Unhandled transaction processing for transaction type: {}", loanTransaction.getTypeOf());
        }
        return unprocessed;
    }

    @Override
    protected void handleChargeback(LoanTransaction loanTransaction, TransactionCtx ctx) {
        processCreditTransaction(loanTransaction, ctx);
    }

    private boolean hasNoCustomCreditAllocationRule(LoanTransaction loanTransaction) {
        return (loanTransaction.getLoan().getCreditAllocationRules() == null || loanTransaction.getLoan().getCreditAllocationRules()
                .stream().noneMatch(e -> e.getTransactionType().getLoanTransactionType().equals(loanTransaction.getTypeOf())));
    }

    protected LoanTransaction findOriginalTransaction(LoanTransaction loanTransaction, TransactionCtx ctx) {
        if (loanTransaction.getId() != null) { // this the normal case without reverse-replay
            Optional<LoanTransaction> originalTransaction = loanTransaction.getLoan().getLoanTransactions().stream()
                    .filter(tr -> tr.getLoanTransactionRelations().stream()
                            .anyMatch(this.hasMatchingToLoanTransaction(loanTransaction.getId(), CHARGEBACK)))
                    .findFirst();
            if (originalTransaction.isEmpty()) {
                throw new GeneralPlatformDomainRuleException("error.msg.chargeback.transaction.must.have.original.transaction",
                        "Chargeback transaction must have an original transaction");
            }
            return originalTransaction.get();
        } else { // when there is no id, then it might be that the original transaction is changed, so we need to look
            // it up from the Ctx.
            Long originalChargebackTransactionId = ctx.getChangedTransactionDetail().getCurrentTransactionToOldId().get(loanTransaction);
            Collection<LoanTransaction> updatedTransactions = ctx.getChangedTransactionDetail().getNewTransactionMappings().values();
            Optional<LoanTransaction> updatedTransaction = updatedTransactions.stream().filter(tr -> tr.getLoanTransactionRelations()
                    .stream().anyMatch(this.hasMatchingToLoanTransaction(originalChargebackTransactionId, CHARGEBACK))).findFirst();

            if (updatedTransaction.isPresent()) {
                return updatedTransaction.get();
            } else { // if it is not there, then it simply means that this has not changed during reverse replay
                Optional<LoanTransaction> originalTransaction = loanTransaction.getLoan().getLoanTransactions().stream()
                        .filter(tr -> tr.getLoanTransactionRelations().stream()
                                .anyMatch(this.hasMatchingToLoanTransaction(originalChargebackTransactionId, CHARGEBACK)))
                        .findFirst();
                if (originalTransaction.isEmpty()) {
                    throw new GeneralPlatformDomainRuleException("error.msg.chargeback.transaction.must.have.original.transaction",
                            "Chargeback transaction must have an original transaction");
                }
                return originalTransaction.get();
            }
        }
    }

    @SuppressWarnings({ "squid:S3776" })
    protected void processCreditTransaction(LoanTransaction loanTransaction, TransactionCtx ctx) {
        if (hasNoCustomCreditAllocationRule(loanTransaction)) {
            super.processCreditTransaction(loanTransaction, ctx.getOverpaymentHolder(), ctx.getCurrency(), ctx.getInstallments());
        } else {
            log.debug("Processing credit transaction with custom credit allocation rules");

            loanTransaction.resetDerivedComponents();
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings = new ArrayList<>();
            final Comparator<LoanRepaymentScheduleInstallment> byDate = Comparator.comparing(LoanRepaymentScheduleInstallment::getDueDate);
            ctx.getInstallments().sort(byDate);
            final Money zeroMoney = Money.zero(ctx.getCurrency());
            Money transactionAmount = loanTransaction.getAmount(ctx.getCurrency());
            Money amountToDistribute = MathUtil
                    .negativeToZero(loanTransaction.getAmount(ctx.getCurrency()).minus(ctx.getOverpaymentHolder().getMoneyObject()));
            Money repaidAmount = MathUtil.negativeToZero(transactionAmount.minus(amountToDistribute));
            loanTransaction.setOverPayments(repaidAmount);
            ctx.getOverpaymentHolder().setMoneyObject(ctx.getOverpaymentHolder().getMoneyObject().minus(repaidAmount));

            if (amountToDistribute.isGreaterThanZero() && loanTransaction.isChargeback()) {
                LoanTransaction originalTransaction = findOriginalTransaction(loanTransaction, ctx);
                Map<AllocationType, BigDecimal> originalAllocation = getOriginalAllocation(originalTransaction);
                LoanCreditAllocationRule chargeBackAllocationRule = getChargebackAllocationRules(loanTransaction);
                Map<AllocationType, Money> chargebackAllocation = calculateChargebackAllocationMap(originalAllocation,
                        amountToDistribute.getAmount(), chargeBackAllocationRule.getAllocationTypes(), ctx.getCurrency());

                loanTransaction.updateComponents(chargebackAllocation.get(PRINCIPAL), chargebackAllocation.get(INTEREST),
                        chargebackAllocation.get(FEE), chargebackAllocation.get(PENALTY));

                final LocalDate transactionDate = loanTransaction.getTransactionDate();
                boolean loanTransactionMapped = false;
                LocalDate pastDueDate = null;
                final boolean isWriteOffTransaction = loanTransaction.isWriteOff();
                for (final LoanRepaymentScheduleInstallment currentInstallment : ctx.getInstallments()) {
                    pastDueDate = currentInstallment.getDueDate();
                    if (!currentInstallment.isAdditional() && DateUtils.isAfter(currentInstallment.getDueDate(), transactionDate)) {

                        currentInstallment.addToCredits(transactionAmount.getAmount());
                        currentInstallment.addToPrincipal(transactionDate, chargebackAllocation.get(PRINCIPAL));
                        Money originalInterest = currentInstallment.getInterestCharged(ctx.getCurrency());
                        currentInstallment.updateInterestCharged(
                                originalInterest.plus(chargebackAllocation.get(INTEREST)).getAmountDefaultedToNullIfZero());

                        if (repaidAmount.isGreaterThanZero()) {
                            currentInstallment.payPrincipalComponent(loanTransaction.getTransactionDate(), repaidAmount,
                                    isWriteOffTransaction);
                            transactionMappings.add(LoanTransactionToRepaymentScheduleMapping.createFrom(loanTransaction,
                                    currentInstallment, repaidAmount, zeroMoney, zeroMoney, zeroMoney));
                        }
                        loanTransactionMapped = true;
                        break;

                        // If already exists an additional installment just update the due date and
                        // principal from the Loan chargeback / CBR transaction
                    } else if (currentInstallment.isAdditional()) {
                        if (DateUtils.isAfter(transactionDate, currentInstallment.getDueDate())) {
                            currentInstallment.updateDueDate(transactionDate);
                        }
                        currentInstallment.addToCredits(transactionAmount.getAmount());
                        currentInstallment.addToPrincipal(transactionDate, chargebackAllocation.get(PRINCIPAL));
                        Money originalInterest = currentInstallment.getInterestCharged(ctx.getCurrency());
                        currentInstallment.updateInterestCharged(
                                originalInterest.plus(chargebackAllocation.get(INTEREST)).getAmountDefaultedToNullIfZero());
                        if (repaidAmount.isGreaterThanZero()) {
                            currentInstallment.payPrincipalComponent(loanTransaction.getTransactionDate(), repaidAmount,
                                    isWriteOffTransaction);
                            transactionMappings.add(LoanTransactionToRepaymentScheduleMapping.createFrom(loanTransaction,
                                    currentInstallment, repaidAmount, zeroMoney, zeroMoney, zeroMoney));
                        }
                        loanTransactionMapped = true;
                        break;
                    }
                }

                // New installment will be added (N+1 scenario)
                if (!loanTransactionMapped) {
                    if (loanTransaction.getTransactionDate().equals(pastDueDate)) {
                        LoanRepaymentScheduleInstallment currentInstallment = ctx.getInstallments().get(ctx.getInstallments().size() - 1);
                        currentInstallment.addToCredits(transactionAmount.getAmount());
                        currentInstallment.addToPrincipal(transactionDate, chargebackAllocation.get(PRINCIPAL));
                        Money originalInterest = currentInstallment.getInterestCharged(ctx.getCurrency());
                        currentInstallment.updateInterestCharged(
                                originalInterest.plus(chargebackAllocation.get(INTEREST)).getAmountDefaultedToNullIfZero());
                        if (repaidAmount.isGreaterThanZero()) {
                            currentInstallment.payPrincipalComponent(loanTransaction.getTransactionDate(), repaidAmount,
                                    isWriteOffTransaction);
                            transactionMappings.add(LoanTransactionToRepaymentScheduleMapping.createFrom(loanTransaction,
                                    currentInstallment, repaidAmount, zeroMoney, zeroMoney, zeroMoney));
                        }
                    } else {
                        Loan loan = loanTransaction.getLoan();
                        LoanRepaymentScheduleInstallment installment = new LoanRepaymentScheduleInstallment(loan,
                                (ctx.getInstallments().size() + 1), pastDueDate, transactionDate, zeroMoney.getAmount(),
                                zeroMoney.getAmount(), zeroMoney.getAmount(), zeroMoney.getAmount(), false, null);
                        installment.markAsAdditional();
                        installment.addToCredits(transactionAmount.getAmount());
                        installment.addToPrincipal(transactionDate, chargebackAllocation.get(PRINCIPAL));
                        Money originalInterest = installment.getInterestCharged(ctx.getCurrency());
                        installment.updateInterestCharged(
                                originalInterest.plus(chargebackAllocation.get(INTEREST)).getAmountDefaultedToNullIfZero());
                        loan.addLoanRepaymentScheduleInstallment(installment);
                        if (repaidAmount.isGreaterThanZero()) {
                            installment.payPrincipalComponent(loanTransaction.getTransactionDate(), repaidAmount, isWriteOffTransaction);
                            transactionMappings.add(LoanTransactionToRepaymentScheduleMapping.createFrom(loanTransaction, installment,
                                    repaidAmount, zeroMoney, zeroMoney, zeroMoney));
                        }
                    }
                }

                loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(transactionMappings);
            }

        }
    }

    @NotNull
    private LoanCreditAllocationRule getChargebackAllocationRules(LoanTransaction loanTransaction) {
        return loanTransaction.getLoan().getCreditAllocationRules().stream()
                .filter(tr -> tr.getTransactionType().equals(CreditAllocationTransactionType.CHARGEBACK)).findFirst().orElseThrow();
    }

    @NotNull
    private Map<AllocationType, BigDecimal> getOriginalAllocation(LoanTransaction originalLoanTransaction) {
        Map<AllocationType, BigDecimal> originalAllocation = new HashMap<>();
        originalAllocation.put(PRINCIPAL, originalLoanTransaction.getPrincipalPortion());
        originalAllocation.put(INTEREST, originalLoanTransaction.getInterestPortion());
        originalAllocation.put(PENALTY, originalLoanTransaction.getPenaltyChargesPortion());
        originalAllocation.put(FEE, originalLoanTransaction.getFeeChargesPortion());
        return originalAllocation;
    }

    protected Map<AllocationType, Money> calculateChargebackAllocationMap(Map<AllocationType, BigDecimal> originalAllocation,
            BigDecimal amountToDistribute, List<AllocationType> allocationTypes, MonetaryCurrency currency) {
        BigDecimal remainingAmount = amountToDistribute;
        Map<AllocationType, Money> result = new HashMap<>();
        Arrays.stream(AllocationType.values()).forEach(allocationType -> result.put(allocationType, Money.of(currency, BigDecimal.ZERO)));
        for (AllocationType allocationType : allocationTypes) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal originalAmount = originalAllocation.get(allocationType);
                if (originalAmount != null && remainingAmount.compareTo(originalAmount) > 0
                        && originalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    result.put(allocationType, Money.of(currency, originalAmount));
                    remainingAmount = remainingAmount.subtract(originalAmount);
                } else if (originalAmount != null && remainingAmount.compareTo(originalAmount) <= 0
                        && originalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    result.put(allocationType, Money.of(currency, remainingAmount));
                    remainingAmount = BigDecimal.ZERO;
                }
            }
        }
        return result;
    }

    private Predicate<LoanTransactionRelation> hasMatchingToLoanTransaction(Long id, LoanTransactionRelationTypeEnum typeEnum) {
        return relation -> relation.getRelationType().equals(typeEnum) && Objects.equals(relation.getToTransaction().getId(), id);
    }

    @Override
    protected void handleRefund(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, Set<LoanCharge> charges) {
        Money zero = Money.zero(currency);
        List<LoanTransactionToRepaymentScheduleMapping> transactionMappings = new ArrayList<>();
        Money transactionAmountUnprocessed = loanTransaction.getAmount(currency);

        List<LoanPaymentAllocationRule> paymentAllocationRules = loanTransaction.getLoan().getPaymentAllocationRules();
        LoanPaymentAllocationRule defaultPaymentAllocationRule = paymentAllocationRules.stream()
                .filter(e -> PaymentAllocationTransactionType.DEFAULT.equals(e.getTransactionType())).findFirst().orElseThrow();
        LoanPaymentAllocationRule paymentAllocationRule = paymentAllocationRules.stream()
                .filter(e -> loanTransaction.getTypeOf().equals(e.getTransactionType().getLoanTransactionType())).findFirst()
                .orElse(defaultPaymentAllocationRule);
        Balances balances = new Balances(zero, zero, zero, zero);
        List<PaymentAllocationType> paymentAllocationTypes;
        FutureInstallmentAllocationRule futureInstallmentAllocationRule;
        if (PaymentAllocationTransactionType.DEFAULT.equals(paymentAllocationRule.getTransactionType())) {
            // if the allocation rule is not defined then the reverse order of the default allocation rule will be used
            paymentAllocationTypes = new ArrayList<>(paymentAllocationRule.getAllocationTypes());
            Collections.reverse(paymentAllocationTypes);
            futureInstallmentAllocationRule = FutureInstallmentAllocationRule.LAST_INSTALLMENT;
        } else {
            paymentAllocationTypes = paymentAllocationRule.getAllocationTypes();
            futureInstallmentAllocationRule = paymentAllocationRule.getFutureInstallmentAllocationRule();
        }
        if (LoanScheduleProcessingType.HORIZONTAL
                .equals(loanTransaction.getLoan().getLoanProductRelatedDetail().getLoanScheduleProcessingType())) {
            LinkedHashMap<DueType, List<PaymentAllocationType>> paymentAllocationsMap = paymentAllocationTypes.stream().collect(
                    Collectors.groupingBy(PaymentAllocationType::getDueType, LinkedHashMap::new, mapping(Function.identity(), toList())));

            for (Map.Entry<DueType, List<PaymentAllocationType>> paymentAllocationsEntry : paymentAllocationsMap.entrySet()) {
                transactionAmountUnprocessed = refundTransactionHorizontally(loanTransaction, currency, installments,
                        transactionAmountUnprocessed, paymentAllocationsEntry.getValue(), futureInstallmentAllocationRule,
                        transactionMappings, charges, balances);
                if (!transactionAmountUnprocessed.isGreaterThanZero()) {
                    break;
                }
            }
        } else if (LoanScheduleProcessingType.VERTICAL
                .equals(loanTransaction.getLoan().getLoanProductRelatedDetail().getLoanScheduleProcessingType())) {
            for (PaymentAllocationType paymentAllocationType : paymentAllocationTypes) {
                transactionAmountUnprocessed = refundTransactionVertically(loanTransaction, currency, installments, zero,
                        transactionMappings, transactionAmountUnprocessed, futureInstallmentAllocationRule, charges, balances,
                        paymentAllocationType);
                if (!transactionAmountUnprocessed.isGreaterThanZero()) {
                    break;
                }
            }
        }

        loanTransaction.updateComponents(balances.getAggregatedPrincipalPortion(), balances.getAggregatedInterestPortion(),
                balances.getAggregatedFeeChargesPortion(), balances.getAggregatedPenaltyChargesPortion());
        loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(transactionMappings);
    }

    private void processSingleTransaction(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, Set<LoanCharge> charges, ChangedTransactionDetail changedTransactionDetail,
            MoneyHolder overpaymentHolder) {
        TransactionCtx ctx = new TransactionCtx(currency, installments, charges, overpaymentHolder, changedTransactionDetail);
        if (loanTransaction.getId() == null) {
            processLatestTransaction(loanTransaction, ctx);
            if (loanTransaction.isInterestWaiver()) {
                loanTransaction.adjustInterestComponent(currency);
            }
        } else {
            /*
             * For existing transactions, check if the re-payment breakup (principal, interest, fees, penalties) has
             * changed.<br>
             */
            final LoanTransaction newLoanTransaction = LoanTransaction.copyTransactionProperties(loanTransaction);
            ctx.getChangedTransactionDetail().getCurrentTransactionToOldId().put(newLoanTransaction, loanTransaction.getId());

            // Reset derived component of new loan transaction and
            // re-process transaction
            processLatestTransaction(newLoanTransaction, ctx);
            if (loanTransaction.isInterestWaiver()) {
                newLoanTransaction.adjustInterestComponent(currency);
            }
            /*
             * Check if the transaction amounts have changed. If so, reverse the original transaction and update
             * changedTransactionDetail accordingly
             */
            if (LoanTransaction.transactionAmountsMatch(currency, loanTransaction, newLoanTransaction)) {
                loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(
                        newLoanTransaction.getLoanTransactionToRepaymentScheduleMappings());
            } else {
                createNewTransaction(loanTransaction, newLoanTransaction, changedTransactionDetail);
            }
        }
    }

    @NotNull
    private List<ChargeOrTransaction> createSortedChargesAndTransactionsList(List<LoanTransaction> loanTransactions,
            Set<LoanCharge> charges) {
        List<ChargeOrTransaction> chargeOrTransactions = new ArrayList<>();
        if (charges != null) {
            chargeOrTransactions.addAll(charges.stream().map(ChargeOrTransaction::new).toList());
        }
        if (loanTransactions != null) {
            chargeOrTransactions.addAll(loanTransactions.stream().map(ChargeOrTransaction::new).toList());
        }
        return chargeOrTransactions;
    }

    private void handleDisbursement() {
        log.info("Disbursement transaction will not be processed.");
    }

    private Money handleRepayment(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, Set<LoanCharge> charges, MoneyHolder overpaymentHolder) {
        if (loanTransaction.isRepaymentLikeType() || loanTransaction.isInterestWaiver() || loanTransaction.isRecoveryRepayment()) {
            loanTransaction.resetDerivedComponents();
        }
        Money transactionAmountUnprocessed = loanTransaction.getAmount(currency);
        return processTransaction(loanTransaction, currency, installments, transactionAmountUnprocessed, charges, overpaymentHolder);
    }

    @Override
    protected void handleWriteOff(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges, final MoneyHolder overpaymentHolder) {
        if ((loanTransaction.isRepaymentLikeType() || loanTransaction.isInterestWaiver() || loanTransaction.isRecoveryRepayment())
                && !loanTransaction.isSpecialWriteOff()) {
            loanTransaction.resetDerivedComponents();
        }

        Money transactionAmountUnprocessed = loanTransaction.getAmount(currency);
        processTransaction(loanTransaction, currency, installments, transactionAmountUnprocessed, charges, overpaymentHolder);
    }

    private LoanTransactionToRepaymentScheduleMapping getTransactionMapping(
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings, LoanTransaction loanTransaction,
            LoanRepaymentScheduleInstallment currentInstallment, MonetaryCurrency currency) {
        Money zero = Money.zero(currency);
        LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = transactionMappings.stream()
                .filter(e -> loanTransaction.equals(e.getLoanTransaction()))
                .filter(e -> currentInstallment.equals(e.getLoanRepaymentScheduleInstallment())).findFirst().orElse(null);
        if (loanTransactionToRepaymentScheduleMapping == null) {
            loanTransactionToRepaymentScheduleMapping = LoanTransactionToRepaymentScheduleMapping.createFrom(loanTransaction,
                    currentInstallment, zero, zero, zero, zero);
            transactionMappings.add(loanTransactionToRepaymentScheduleMapping);
        }
        return loanTransactionToRepaymentScheduleMapping;
    }

    private Money processPaymentAllocation(PaymentAllocationType paymentAllocationType, LoanRepaymentScheduleInstallment currentInstallment,
            LoanTransaction loanTransaction, Money transactionAmountUnprocessed,
            LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping, Set<LoanCharge> chargesOfInstallment,
            Balances balances, LoanRepaymentScheduleInstallment.PaymentAction action) {
        log.debug("processPaymentAllocation - Transaction ID: {}, Installment: {}, Allocation Type: {}, Amount: {}",
                loanTransaction.getId() != null ? loanTransaction.getId() : "NEW", currentInstallment.getInstallmentNumber(),
                paymentAllocationType.getAllocationType(), transactionAmountUnprocessed.getAmount());

        LocalDate transactionDate = loanTransaction.getTransactionDate();
        Money zero = transactionAmountUnprocessed.zero();
        final boolean isWriteOffTransaction = loanTransaction.isWriteOff();
        Money portion;
        if (loanTransaction.claimType() != null
                && loanTransaction.claimType().equals(AdvancedPaymentScheduleTransactionProcessor.INSURANCE_PARAM)
                && paymentAllocationType.getAllocationType().equals(AllocationType.MANDATORY_INSURANCE)) {
            log.info("processPaymentAllocation - Returning zero for MANDATORY_INSURANCE with INSURANCE claim type");
            portion = transactionAmountUnprocessed.zero();
        } else if (loanTransaction.claimType() != null
                && loanTransaction.claimType().equals(AdvancedPaymentScheduleTransactionProcessor.GUARANTOR_PARAM)
                && paymentAllocationType.getAllocationType().equals(AVAL)) {
            log.info("processPaymentAllocation - Returning zero for AVAL with GUARANTOR claim type");
            portion = transactionAmountUnprocessed.zero();
        } else if (loanTransaction.claimType() != null && paymentAllocationType.getAllocationType().equals(FEES)) {
            log.info("processPaymentAllocation - Returning zero for FEES with claim type");
            portion = transactionAmountUnprocessed.zero();
        } else {

            LoanRepaymentScheduleInstallment.PaymentFunction paymentFunction = currentInstallment
                    .getPaymentFunction(paymentAllocationType.getAllocationType(), action);
            portion = paymentFunction.accept(transactionDate, transactionAmountUnprocessed, isWriteOffTransaction, loanTransaction);
            log.debug("Payment function result - Transaction ID: {}, Installment: {}, Allocation Type: {}, Portion: {}",
                    loanTransaction.getId() != null ? loanTransaction.getId() : "NEW", currentInstallment.getInstallmentNumber(),
                    paymentAllocationType.getAllocationType(), portion.getAmount());
        }

        ChargesPaidByFunction chargesPaidByFunction = getChargesPaymentFunction(action);

        switch (paymentAllocationType.getAllocationType()) {
            case PENALTY -> {
                balances.setAggregatedPenaltyChargesPortion(balances.getAggregatedPenaltyChargesPortion().add(portion));
                addToTransactionMapping(loanTransactionToRepaymentScheduleMapping, zero, zero, zero, portion);
                Set<LoanCharge> penalties = chargesOfInstallment.stream().filter(LoanCharge::isPenaltyCharge).collect(Collectors.toSet());
                chargesPaidByFunction.accept(loanTransaction, portion, penalties, currentInstallment.getInstallmentNumber());
            }
            case FEE -> {
                balances.setAggregatedFeeChargesPortion(balances.getAggregatedFeeChargesPortion().add(portion));
                addToTransactionMapping(loanTransactionToRepaymentScheduleMapping, zero, zero, portion, zero);
                Set<LoanCharge> fees = chargesOfInstallment.stream().filter(LoanCharge::isFeeCharge).collect(Collectors.toSet());
                chargesPaidByFunction.accept(loanTransaction, portion, fees, currentInstallment.getInstallmentNumber());
            }
            case FEES -> {
                balances.setAggregatedFeeChargesPortion(balances.getAggregatedFeeChargesPortion().add(portion));
                addToTransactionMapping(loanTransactionToRepaymentScheduleMapping, zero, zero, portion, zero);
                Set<LoanCharge> fees = chargesOfInstallment.stream().filter(LoanCharge::isFlatHono).collect(Collectors.toSet());
                chargesPaidByFunction.accept(loanTransaction, portion, fees, currentInstallment.getInstallmentNumber());
            }
            case AVAL -> {
                balances.setAggregatedFeeChargesPortion(balances.getAggregatedFeeChargesPortion().add(portion));
                addToTransactionMapping(loanTransactionToRepaymentScheduleMapping, zero, zero, portion, zero);
                Set<LoanCharge> fees = chargesOfInstallment.stream().filter(LoanCharge::isAvalCharge).collect(Collectors.toSet());
                chargesPaidByFunction.accept(loanTransaction, portion, fees, currentInstallment.getInstallmentNumber());
            }
            case MANDATORY_INSURANCE -> {
                log.info("processPaymentAllocation - Processing MANDATORY_INSURANCE case, portion: {}", portion.getAmount());
                balances.setAggregatedFeeChargesPortion(balances.getAggregatedFeeChargesPortion().add(portion));
                addToTransactionMapping(loanTransactionToRepaymentScheduleMapping, zero, zero, portion, zero);
                Set<LoanCharge> fees = chargesOfInstallment.stream().filter(LoanCharge::isMandatoryInsurance).collect(Collectors.toSet());
                log.info("processPaymentAllocation - MANDATORY_INSURANCE charges count: {}", fees.size());
                log.info("processPaymentAllocation - About to call chargesPaidByFunction.accept for MANDATORY_INSURANCE");
                chargesPaidByFunction.accept(loanTransaction, portion, fees, currentInstallment.getInstallmentNumber());
                log.info("processPaymentAllocation - chargesPaidByFunction.accept completed for MANDATORY_INSURANCE");
            }
            case VOLUNTARY_INSURANCE -> {
                balances.setAggregatedFeeChargesPortion(balances.getAggregatedFeeChargesPortion().add(portion));
                addToTransactionMapping(loanTransactionToRepaymentScheduleMapping, zero, zero, portion, zero);
                Set<LoanCharge> fees = chargesOfInstallment.stream().filter(LoanCharge::isVoluntaryInsurance).collect(Collectors.toSet());
                chargesPaidByFunction.accept(loanTransaction, portion, fees, currentInstallment.getInstallmentNumber());
            }
            case INTEREST -> {
                balances.setAggregatedInterestPortion(balances.getAggregatedInterestPortion().add(portion));
                addToTransactionMapping(loanTransactionToRepaymentScheduleMapping, zero, portion, zero, zero);
            }
            case PRINCIPAL -> {
                balances.setAggregatedPrincipalPortion(balances.getAggregatedPrincipalPortion().add(portion));
                addToTransactionMapping(loanTransactionToRepaymentScheduleMapping, portion, zero, zero, zero);
            }
        }
        log.info("processPaymentAllocation completed - Transaction ID: {}, Installment: {}, Allocation Type: {}, Final Portion: {}",
                loanTransaction.getId() != null ? loanTransaction.getId() : "NEW", currentInstallment.getInstallmentNumber(),
                paymentAllocationType.getAllocationType(), portion.getAmount());
        return portion;
    }

    private void addToTransactionMapping(LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping,
            Money principalPortion, Money interestPortion, Money feePortion, Money penaltyPortion) {
        BigDecimal aggregatedPenalty = ObjectUtils
                .defaultIfNull(loanTransactionToRepaymentScheduleMapping.getPenaltyChargesPortion(), BigDecimal.ZERO)
                .add(penaltyPortion.getAmount());
        BigDecimal aggregatedFee = ObjectUtils
                .defaultIfNull(loanTransactionToRepaymentScheduleMapping.getFeeChargesPortion(), BigDecimal.ZERO)
                .add(feePortion.getAmount());
        BigDecimal aggregatedInterest = ObjectUtils
                .defaultIfNull(loanTransactionToRepaymentScheduleMapping.getInterestPortion(), BigDecimal.ZERO)
                .add(interestPortion.getAmount());
        BigDecimal aggregatedPrincipal = ObjectUtils
                .defaultIfNull(loanTransactionToRepaymentScheduleMapping.getPrincipalPortion(), BigDecimal.ZERO)
                .add(principalPortion.getAmount());
        loanTransactionToRepaymentScheduleMapping.setComponents(aggregatedPrincipal, aggregatedInterest, aggregatedFee, aggregatedPenalty);
    }

    private void handleOverpayment(Money overpaymentPortion, LoanTransaction loanTransaction, MoneyHolder overpaymentHolder) {
        if (overpaymentPortion.isGreaterThanZero()) {
            onLoanOverpayment(loanTransaction, overpaymentPortion);
            overpaymentHolder.setMoneyObject(overpaymentPortion);
            loanTransaction.setOverPayments(overpaymentPortion);
        } else {
            overpaymentHolder.setMoneyObject(overpaymentPortion.zero());
        }
    }

    private void handleChargeOff(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments) {
        loanTransaction.resetDerivedComponents();
        // determine how much is outstanding total and breakdown for principal, interest and charges
        Money principalPortion = Money.zero(currency);
        Money interestPortion = Money.zero(currency);
        Money feeChargesPortion = Money.zero(currency);
        Money penaltychargesPortion = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
            if (currentInstallment.isNotFullyPaidOff()) {
                principalPortion = principalPortion.plus(currentInstallment.getPrincipalOutstanding(currency));
                interestPortion = interestPortion.plus(currentInstallment.getInterestOutstanding(currency));
                feeChargesPortion = feeChargesPortion.plus(currentInstallment.getFeeChargesOutstanding(currency));
                penaltychargesPortion = penaltychargesPortion.plus(currentInstallment.getPenaltyChargesOutstanding(currency));
            }
        }

        loanTransaction.updateComponentsAndTotal(principalPortion, interestPortion, feeChargesPortion, penaltychargesPortion);
    }

    @SuppressWarnings({ "squid:S3655" })
    private void handleChargePayment(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, Set<LoanCharge> charges, MoneyHolder overpaymentHolder) {
        Money zero = Money.zero(currency);
        Money feeChargesPortion = zero;
        Money penaltyChargesPortion = zero;
        List<LoanTransactionToRepaymentScheduleMapping> transactionMappings = new ArrayList<>();
        LoanChargePaidBy loanChargePaidBy = loanTransaction.getLoanChargesPaid().stream().findFirst().get();
        LoanCharge loanCharge = loanChargePaidBy.getLoanCharge();
        Money amountToBePaid = Money.of(currency, loanTransaction.getAmount());
        if (loanCharge.getAmountOutstanding(currency).isLessThan(amountToBePaid)) {
            amountToBePaid = loanCharge.getAmountOutstanding(currency);
        }

        LocalDate startDate = loanTransaction.getLoan().getDisbursementDate();

        final boolean isWriteOffTransaction = loanTransaction.isWriteOff();

        Money unprocessed = loanTransaction.getAmount(currency);
        int firstNormalInstallmentNumber = LoanRepaymentScheduleProcessingWrapper.fetchFirstNormalInstallmentNumber(installments);
        for (final LoanRepaymentScheduleInstallment installment : installments) {
            boolean isDue = installment.getInstallmentNumber().equals(firstNormalInstallmentNumber)
                    ? loanCharge.isDueForCollectionFromIncludingAndUpToAndIncluding(startDate, installment.getDueDate())
                    : loanCharge.isDueForCollectionFromAndUpToAndIncluding(startDate, installment.getDueDate());
            if (isDue) {
                Integer installmentNumber = installment.getInstallmentNumber();
                Money paidAmount = loanCharge.updatePaidAmountBy(amountToBePaid, installmentNumber, zero, isWriteOffTransaction);

                LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                        transactionMappings, loanTransaction, installment, currency);

                if (loanTransaction.isPenaltyPayment()) {
                    penaltyChargesPortion = installment.payPenaltyChargesComponent(loanTransaction.getTransactionDate(), paidAmount,
                            isWriteOffTransaction);
                    loanTransaction.setLoanChargesPaid(Collections
                            .singleton(new LoanChargePaidBy(loanTransaction, loanCharge, paidAmount.getAmount(), installmentNumber)));
                    addToTransactionMapping(loanTransactionToRepaymentScheduleMapping, zero, zero, zero, penaltyChargesPortion);
                } else {
                    feeChargesPortion = installment.payFeeChargesComponent(loanTransaction.getTransactionDate(), paidAmount,
                            isWriteOffTransaction);
                    loanTransaction.setLoanChargesPaid(Collections
                            .singleton(new LoanChargePaidBy(loanTransaction, loanCharge, paidAmount.getAmount(), installmentNumber)));
                    addToTransactionMapping(loanTransactionToRepaymentScheduleMapping, zero, zero, feeChargesPortion, zero);
                }

                loanTransaction.updateComponents(zero, zero, feeChargesPortion, penaltyChargesPortion);
                unprocessed = loanTransaction.getAmount(currency).minus(paidAmount);
                loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(transactionMappings);
            }
        }

        if (unprocessed.isGreaterThanZero()) {
            processTransaction(loanTransaction, currency, installments, unprocessed, charges, overpaymentHolder);
        }
    }

    @SuppressWarnings({ "squid:S3776", "squid:S1119" })
    private Money refundTransactionHorizontally(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, Money transactionAmountUnprocessed,
            List<PaymentAllocationType> paymentAllocationTypes, FutureInstallmentAllocationRule futureInstallmentAllocationRule,
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings, Set<LoanCharge> charges, Balances balances) {
        Money zero = Money.zero(currency);
        Money refundedPortion;
        outerLoop: do {
            LoanRepaymentScheduleInstallment latestPastDueInstallment = getLatestPastDueInstallmentForRefund(loanTransaction, currency,
                    installments, zero);
            LoanRepaymentScheduleInstallment dueInstallment = getDueInstallmentForRefund(loanTransaction, currency, installments, zero);

            List<LoanRepaymentScheduleInstallment> inAdvanceInstallments = getFutureInstallmentsForRefund(loanTransaction, currency,
                    installments, futureInstallmentAllocationRule, zero);

            int firstNormalInstallmentNumber = LoanRepaymentScheduleProcessingWrapper.fetchFirstNormalInstallmentNumber(installments);
            for (PaymentAllocationType paymentAllocationType : paymentAllocationTypes) {
                switch (paymentAllocationType.getDueType()) {
                    case PAST_DUE -> {
                        if (latestPastDueInstallment != null) {
                            Set<LoanCharge> oldestPastDueInstallmentCharges = getLoanChargesOfInstallment(charges, latestPastDueInstallment,
                                    firstNormalInstallmentNumber);
                            LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                    transactionMappings, loanTransaction, latestPastDueInstallment, currency);
                            refundedPortion = processPaymentAllocation(paymentAllocationType, latestPastDueInstallment, loanTransaction,
                                    transactionAmountUnprocessed, loanTransactionToRepaymentScheduleMapping,
                                    oldestPastDueInstallmentCharges, balances, LoanRepaymentScheduleInstallment.PaymentAction.UNPAY);
                            transactionAmountUnprocessed = transactionAmountUnprocessed.minus(refundedPortion);
                        } else {
                            break outerLoop;
                        }
                    }
                    case DUE -> {
                        if (dueInstallment != null) {
                            Set<LoanCharge> dueInstallmentCharges = getLoanChargesOfInstallment(charges, dueInstallment,
                                    firstNormalInstallmentNumber);
                            LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                    transactionMappings, loanTransaction, dueInstallment, currency);
                            refundedPortion = processPaymentAllocation(paymentAllocationType, dueInstallment, loanTransaction,
                                    transactionAmountUnprocessed, loanTransactionToRepaymentScheduleMapping, dueInstallmentCharges,
                                    balances, LoanRepaymentScheduleInstallment.PaymentAction.UNPAY);
                            transactionAmountUnprocessed = transactionAmountUnprocessed.minus(refundedPortion);
                        } else {
                            break outerLoop;
                        }
                    }
                    case IN_ADVANCE -> {
                        int numberOfInstallments = inAdvanceInstallments.size();
                        if (numberOfInstallments > 0) {
                            Money evenPortion = transactionAmountUnprocessed.dividedBy(numberOfInstallments, MoneyHelper.getRoundingMode());
                            Money balanceAdjustment = transactionAmountUnprocessed.minus(evenPortion.multipliedBy(numberOfInstallments));
                            for (LoanRepaymentScheduleInstallment inAdvanceInstallment : inAdvanceInstallments) {
                                Set<LoanCharge> inAdvanceInstallmentCharges = getLoanChargesOfInstallment(charges, inAdvanceInstallment,
                                        firstNormalInstallmentNumber);
                                if (inAdvanceInstallment.equals(inAdvanceInstallments.get(numberOfInstallments - 1))) {
                                    evenPortion = evenPortion.add(balanceAdjustment);
                                }
                                LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                        transactionMappings, loanTransaction, inAdvanceInstallment, currency);
                                refundedPortion = processPaymentAllocation(paymentAllocationType, inAdvanceInstallment, loanTransaction,
                                        evenPortion, loanTransactionToRepaymentScheduleMapping, inAdvanceInstallmentCharges, balances,
                                        LoanRepaymentScheduleInstallment.PaymentAction.UNPAY);
                                transactionAmountUnprocessed = transactionAmountUnprocessed.minus(refundedPortion);
                            }
                        } else {
                            break outerLoop;
                        }
                    }
                }
            }
        } while (installments.stream().anyMatch(installment -> installment.getTotalPaid(currency).isGreaterThan(zero))
                && transactionAmountUnprocessed.isGreaterThanZero());
        return transactionAmountUnprocessed;
    }

    @SuppressWarnings({ "squid:S3776" })
    private Money refundTransactionVertically(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, Money zero,
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings, Money transactionAmountUnprocessed,
            FutureInstallmentAllocationRule futureInstallmentAllocationRule, Set<LoanCharge> charges, Balances balances,
            PaymentAllocationType paymentAllocationType) {
        LoanRepaymentScheduleInstallment currentInstallment = null;
        Money refundedPortion = zero;
        int firstNormalInstallmentNumber = LoanRepaymentScheduleProcessingWrapper.fetchFirstNormalInstallmentNumber(installments);
        do {
            switch (paymentAllocationType.getDueType()) {
                case PAST_DUE -> {
                    currentInstallment = getLatestPastDueInstallmentForRefund(loanTransaction, currency, installments, zero);
                    if (currentInstallment != null) {
                        Set<LoanCharge> oldestPastDueInstallmentCharges = getLoanChargesOfInstallment(charges, currentInstallment,
                                firstNormalInstallmentNumber);
                        LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                transactionMappings, loanTransaction, currentInstallment, currency);
                        refundedPortion = processPaymentAllocation(paymentAllocationType, currentInstallment, loanTransaction,
                                transactionAmountUnprocessed, loanTransactionToRepaymentScheduleMapping, oldestPastDueInstallmentCharges,
                                balances, LoanRepaymentScheduleInstallment.PaymentAction.UNPAY);
                        transactionAmountUnprocessed = transactionAmountUnprocessed.minus(refundedPortion);
                    }
                }
                case DUE -> {
                    currentInstallment = getDueInstallmentForRefund(loanTransaction, currency, installments, zero);
                    if (currentInstallment != null) {
                        Set<LoanCharge> dueInstallmentCharges = getLoanChargesOfInstallment(charges, currentInstallment,
                                firstNormalInstallmentNumber);
                        LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                transactionMappings, loanTransaction, currentInstallment, currency);
                        refundedPortion = processPaymentAllocation(paymentAllocationType, currentInstallment, loanTransaction,
                                transactionAmountUnprocessed, loanTransactionToRepaymentScheduleMapping, dueInstallmentCharges, balances,
                                LoanRepaymentScheduleInstallment.PaymentAction.UNPAY);
                        transactionAmountUnprocessed = transactionAmountUnprocessed.minus(refundedPortion);
                    }
                }
                case IN_ADVANCE -> {
                    List<LoanRepaymentScheduleInstallment> currentInstallments = getFutureInstallmentsForRefund(loanTransaction, currency,
                            installments, futureInstallmentAllocationRule, zero);
                    int numberOfInstallments = currentInstallments.size();
                    refundedPortion = zero;
                    if (numberOfInstallments > 0) {
                        Money evenPortion = transactionAmountUnprocessed.dividedBy(numberOfInstallments, MoneyHelper.getRoundingMode());
                        Money balanceAdjustment = transactionAmountUnprocessed.minus(evenPortion.multipliedBy(numberOfInstallments));
                        for (LoanRepaymentScheduleInstallment internalCurrentInstallment : currentInstallments) {
                            currentInstallment = internalCurrentInstallment;
                            Set<LoanCharge> inAdvanceInstallmentCharges = getLoanChargesOfInstallment(charges, currentInstallment,
                                    firstNormalInstallmentNumber);
                            if (internalCurrentInstallment.equals(currentInstallments.get(numberOfInstallments - 1))) {
                                evenPortion = evenPortion.add(balanceAdjustment);
                            }
                            LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                    transactionMappings, loanTransaction, currentInstallment, currency);
                            Money internalUnpaidPortion = processPaymentAllocation(paymentAllocationType, currentInstallment,
                                    loanTransaction, evenPortion, loanTransactionToRepaymentScheduleMapping, inAdvanceInstallmentCharges,
                                    balances, LoanRepaymentScheduleInstallment.PaymentAction.UNPAY);
                            if (internalUnpaidPortion.isGreaterThanZero()) {
                                refundedPortion = internalUnpaidPortion;
                            }
                            transactionAmountUnprocessed = transactionAmountUnprocessed.minus(internalUnpaidPortion);
                        }
                    } else {
                        currentInstallment = null;
                    }
                }
            }
        } while (currentInstallment != null && transactionAmountUnprocessed.isGreaterThanZero() && refundedPortion.isGreaterThanZero());
        return transactionAmountUnprocessed;
    }

    @Nullable
    private static LoanRepaymentScheduleInstallment getDueInstallmentForRefund(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, Money zero) {
        return installments.stream().filter(installment -> installment.getTotalPaid(currency).isGreaterThan(zero))
                .filter(installment -> loanTransaction.isOn(installment.getDueDate()))
                .max(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).orElse(null);
    }

    @Nullable
    private static LoanRepaymentScheduleInstallment getLatestPastDueInstallmentForRefund(LoanTransaction loanTransaction,
            MonetaryCurrency currency, List<LoanRepaymentScheduleInstallment> installments, Money zero) {
        return installments.stream().filter(installment -> installment.getTotalPaid(currency).isGreaterThan(zero))
                .filter(e -> loanTransaction.isAfter(e.getDueDate()))
                .max(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).orElse(null);
    }

    @NotNull
    private static List<LoanRepaymentScheduleInstallment> getFutureInstallmentsForRefund(LoanTransaction loanTransaction,
            MonetaryCurrency currency, List<LoanRepaymentScheduleInstallment> installments,
            FutureInstallmentAllocationRule futureInstallmentAllocationRule, Money zero) {
        List<LoanRepaymentScheduleInstallment> inAdvanceInstallments = new ArrayList<>();
        if (FutureInstallmentAllocationRule.REAMORTIZATION.equals(futureInstallmentAllocationRule)) {
            inAdvanceInstallments = installments.stream().filter(installment -> installment.getTotalPaid(currency).isGreaterThan(zero))
                    .filter(e -> loanTransaction.isBefore(e.getDueDate())).toList();
        } else if (FutureInstallmentAllocationRule.NEXT_INSTALLMENT.equals(futureInstallmentAllocationRule)) {
            inAdvanceInstallments = installments.stream().filter(installment -> installment.getTotalPaid(currency).isGreaterThan(zero))
                    .filter(e -> loanTransaction.isBefore(e.getDueDate()))
                    .min(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).stream().toList();
        } else if (FutureInstallmentAllocationRule.LAST_INSTALLMENT.equals(futureInstallmentAllocationRule)) {
            inAdvanceInstallments = installments.stream().filter(installment -> installment.getTotalPaid(currency).isGreaterThan(zero))
                    .filter(e -> loanTransaction.isBefore(e.getDueDate()))
                    .max(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).stream().toList();
        }
        return inAdvanceInstallments;
    }

    private Money processTransaction(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, Money transactionAmountUnprocessed, Set<LoanCharge> charges,
            MoneyHolder overpaymentHolder) {
        if (!loanTransaction.isSpecialWriteOff()) {
            Money zero = Money.zero(currency);
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings = new ArrayList<>();

            List<LoanPaymentAllocationRule> paymentAllocationRules = loanTransaction.getLoan().getPaymentAllocationRules();
            LoanPaymentAllocationRule defaultPaymentAllocationRule = paymentAllocationRules.stream()
                    .filter(e -> PaymentAllocationTransactionType.DEFAULT.equals(e.getTransactionType())).findFirst().orElseThrow();
            LoanPaymentAllocationRule paymentAllocationRule = paymentAllocationRules.stream()
                    .filter(e -> loanTransaction.getTypeOf().equals(e.getTransactionType().getLoanTransactionType())).findFirst()
                    .orElse(defaultPaymentAllocationRule);
            Balances balances = new Balances(zero, zero, zero, zero);
            transactionAmountUnprocessed = processPeriodsHorizontally(loanTransaction, currency, installments, transactionAmountUnprocessed,
                    paymentAllocationRule, transactionMappings, charges, balances);
            loanTransaction.updateComponents(balances.getAggregatedPrincipalPortion(), balances.getAggregatedInterestPortion(),
                    balances.getAggregatedFeeChargesPortion(), balances.getAggregatedPenaltyChargesPortion());
            loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(transactionMappings);
        } else {
            transactionAmountUnprocessed = processSpecialWriteOff(loanTransaction, currency, installments);
            final Set<LoanCharge> loanFees = extractFeeCharges(charges);
            final Set<LoanCharge> loanPenalties = extractPenaltyCharges(charges);
            if (loanTransaction.isNotWaiver() && !loanTransaction.isAccrual()) {
                Money feeCharges = loanTransaction.getFeeChargesPortion(currency);
                Money penaltyCharges = loanTransaction.getPenaltyChargesPortion(currency);
                if (feeCharges.isGreaterThanZero()) {
                    updateChargesPaidAmountBy(loanTransaction, feeCharges, loanFees, null);
                }
                if (penaltyCharges.isGreaterThanZero()) {
                    updateChargesPaidAmountBy(loanTransaction, penaltyCharges, loanPenalties, null);
                }
            }
        }

        handleOverpayment(transactionAmountUnprocessed, loanTransaction, overpaymentHolder);
        return transactionAmountUnprocessed;
    }

    private Money processPeriodsHorizontally(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, Money transactionAmountUnprocessed,
            LoanPaymentAllocationRule paymentAllocationRule, List<LoanTransactionToRepaymentScheduleMapping> transactionMappings,
            Set<LoanCharge> charges, Balances balances) {
        log.info("Starting processPeriodsHorizontally - Transaction ID: {}, Amount: {}, Installments count: {}",
                loanTransaction.getId() != null ? loanTransaction.getId() : "NEW", transactionAmountUnprocessed.getAmount(),
                installments.size());

        LinkedHashMap<DueType, List<PaymentAllocationType>> paymentAllocationsMap = paymentAllocationRule.getAllocationTypes().stream()
                .collect(Collectors.groupingBy(PaymentAllocationType::getDueType, LinkedHashMap::new,
                        mapping(Function.identity(), toList())));

        log.debug("Payment allocations map: {}", paymentAllocationsMap.keySet());

        for (Map.Entry<DueType, List<PaymentAllocationType>> paymentAllocationsEntry : paymentAllocationsMap.entrySet()) {
            log.debug("Processing due type: {} with {} allocation types", paymentAllocationsEntry.getKey(),
                    paymentAllocationsEntry.getValue().size());

            Money amountBeforeProcessing = transactionAmountUnprocessed;
            transactionAmountUnprocessed = processAllocationsHorizontally(loanTransaction, currency, installments,
                    transactionAmountUnprocessed, paymentAllocationsEntry.getValue(),
                    paymentAllocationRule.getFutureInstallmentAllocationRule(), transactionMappings, charges, balances);

            log.debug("After processing due type {}: Amount before: {}, Amount after: {}, Difference: {}", paymentAllocationsEntry.getKey(),
                    amountBeforeProcessing.getAmount(), transactionAmountUnprocessed.getAmount(),
                    amountBeforeProcessing.getAmount().subtract(transactionAmountUnprocessed.getAmount()));

            if (transactionAmountUnprocessed.isZero()) {
                log.info("No more funds to process, breaking out of due type loop");
                // no more funds to process
                break;
            }
        }

        log.info("Completed processPeriodsHorizontally - Final unprocessed amount: {}", transactionAmountUnprocessed.getAmount());
        return transactionAmountUnprocessed;
    }

    @SuppressWarnings({ "squid:S3776" })
    private Money processAllocationsHorizontally(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, Money transactionAmountUnprocessed,
            List<PaymentAllocationType> paymentAllocationTypes, FutureInstallmentAllocationRule futureInstallmentAllocationRule,
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings, Set<LoanCharge> charges, Balances balances) {

        log.info("Starting processAllocationsHorizontally - Transaction ID: {}, Initial amount: {}, Installments count: {}",
                loanTransaction.getId() != null ? loanTransaction.getId() : "NEW", transactionAmountUnprocessed.getAmount(),
                installments.size());

        Money paidPortion;
        boolean exit = false;
        int loopIteration = 0;
        final int MAX_ITERATIONS = 100; // Safety limit to prevent infinite loops

        do {
            loopIteration++;
            log.info("Loop iteration {} - Transaction ID: {}, Unprocessed amount: {}, Exit flag: {}", loopIteration,
                    loanTransaction.getId() != null ? loanTransaction.getId() : "NEW", transactionAmountUnprocessed.getAmount(), exit);

            if (loopIteration > MAX_ITERATIONS) {
                log.error(
                        "INFINITE LOOP DETECTED! Reached maximum iterations ({}) for transaction ID: {}. "
                                + "Unprocessed amount: {}, Exit flag: {}",
                        MAX_ITERATIONS, loanTransaction.getId() != null ? loanTransaction.getId() : "NEW",
                        transactionAmountUnprocessed.getAmount(), exit);
                log.error("Installments state:");
                for (int i = 0; i < installments.size(); i++) {
                    LoanRepaymentScheduleInstallment inst = installments.get(i);
                    log.error("  Installment {}: Number={}, FullyPaid={}, Outstanding={}, DueDate={}", i, inst.getInstallmentNumber(),
                            !inst.isNotFullyPaidOff(), inst.getTotalOutstanding(currency).getAmount(), inst.getDueDate());
                }
                throw new RuntimeException("Infinite loop detected in processAllocationsHorizontally for transaction ID: "
                        + (loanTransaction.getId() != null ? loanTransaction.getId() : "NEW"));
            }

            if (transactionAmountUnprocessed.isZero()) {
                log.debug("Transaction amount is zero, setting exit flag");
                exit = true;
                continue;
            }

            // Log installment states for debugging
            long unpaidInstallmentsCount = installments.stream().filter(LoanRepaymentScheduleInstallment::isNotFullyPaidOff).count();
            log.info("Unpaid installments count: {}", unpaidInstallmentsCount);

            LoanRepaymentScheduleInstallment oldestPastDueInstallment = installments.stream()
                    .filter(LoanRepaymentScheduleInstallment::isNotFullyPaidOff).filter(e -> loanTransaction.isAfter(e.getDueDate()))
                    .min(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).orElse(null);

            log.info("Oldest past due installment: {}",
                    oldestPastDueInstallment != null ? oldestPastDueInstallment.getInstallmentNumber() : "null");

            boolean found = false;
            log.info("Claim type check - Transaction claim type: {}", loanTransaction.claimType());
            if (loanTransaction.claimType() != null) {
                log.info("Processing claim type: {}", loanTransaction.claimType());
                Money installmentOutStandingFee = Money.zero(currency);
                if (oldestPastDueInstallment != null) {
                    if (loanTransaction.claimType().equals(AdvancedPaymentScheduleTransactionProcessor.INSURANCE_PARAM)) {
                        installmentOutStandingFee = oldestPastDueInstallment.getFeeChargesOutstandingByType(currency,
                                AdvancedPaymentScheduleTransactionProcessor.MANDATORY_INSURANCE_PARAM);
                    } else if (loanTransaction.claimType().equals(AdvancedPaymentScheduleTransactionProcessor.GUARANTOR_PARAM)) {
                        installmentOutStandingFee = oldestPastDueInstallment.getFeeChargesOutstandingByType(currency, "Aval");
                    }
                    installmentOutStandingFee = installmentOutStandingFee.plus(oldestPastDueInstallment
                            .getFeeChargesOutstandingByType(currency, AdvancedPaymentScheduleTransactionProcessor.HONORARIOS_PARAM));
                }
                if (oldestPastDueInstallment != null
                        && oldestPastDueInstallment.getTotalOutstanding(currency).isGreaterThan(installmentOutStandingFee)) {
                    found = true;
                    log.info("Found valid past due installment for claim type processing");
                }
                while (!found && oldestPastDueInstallment != null) {
                    log.info("Searching for valid past due installment, current: {}", oldestPastDueInstallment.getInstallmentNumber());
                    Money outStandingFee;
                    if (loanTransaction.claimType().equals(AdvancedPaymentScheduleTransactionProcessor.INSURANCE_PARAM)) {
                        outStandingFee = oldestPastDueInstallment.getFeeChargesOutstandingByType(currency,
                                AdvancedPaymentScheduleTransactionProcessor.MANDATORY_INSURANCE_PARAM);
                    } else {
                        outStandingFee = oldestPastDueInstallment.getFeeChargesOutstandingByType(currency, "Aval");
                    }
                    outStandingFee = outStandingFee.plus(oldestPastDueInstallment.getFeeChargesOutstandingByType(currency,
                            AdvancedPaymentScheduleTransactionProcessor.HONORARIOS_PARAM));

                    if (oldestPastDueInstallment.getTotalOutstanding(currency).isEqualTo(outStandingFee)) {
                        Integer installment = oldestPastDueInstallment.getInstallmentNumber();
                        log.info("Installment {} has only fee charges, moving to next", installment);
                        oldestPastDueInstallment = installments.stream().filter(LoanRepaymentScheduleInstallment::isNotFullyPaidOff)
                                .filter(e -> loanTransaction.isAfter(e.getDueDate()) && e.getInstallmentNumber() > installment)
                                .min(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).orElse(null);
                        log.info("Next past due installment: {}",
                                oldestPastDueInstallment != null ? oldestPastDueInstallment.getInstallmentNumber() : "null");
                    } else {
                        found = true;
                        log.info("Found valid past due installment: {}", oldestPastDueInstallment.getInstallmentNumber());
                    }
                }
            }
            LoanRepaymentScheduleInstallment dueInstallment = installments.stream()
                    .filter(LoanRepaymentScheduleInstallment::isNotFullyPaidOff)
                    .filter(e -> loanTransaction.isOnOrBetween(e.getFromDate(), e.getDueDate()) || loanTransaction.isOn(e.getDueDate()))
                    .min(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).orElse(null);

            log.debug("Due installment: {}", dueInstallment != null ? dueInstallment.getInstallmentNumber() : "null");

            found = false;
            if (loanTransaction.claimType() != null) {
                Money installmentOutStandingFee = Money.zero(currency);
                if (dueInstallment != null) {
                    if (loanTransaction.claimType().equals(AdvancedPaymentScheduleTransactionProcessor.INSURANCE_PARAM)) {
                        installmentOutStandingFee = dueInstallment.getFeeChargesOutstandingByType(currency,
                                AdvancedPaymentScheduleTransactionProcessor.MANDATORY_INSURANCE_PARAM);
                    } else if (loanTransaction.claimType().equals(AdvancedPaymentScheduleTransactionProcessor.GUARANTOR_PARAM)) {
                        installmentOutStandingFee = dueInstallment.getFeeChargesOutstandingByType(currency, "Aval");
                    }
                    installmentOutStandingFee = installmentOutStandingFee.plus(dueInstallment.getFeeChargesOutstandingByType(currency,
                            AdvancedPaymentScheduleTransactionProcessor.HONORARIOS_PARAM));
                }
                if (dueInstallment != null && dueInstallment.getTotalOutstanding(currency).isGreaterThan(installmentOutStandingFee)) {
                    found = true;
                    log.debug("Found valid due installment for claim type processing");
                }
                while (!found && dueInstallment != null) {
                    log.debug("Searching for valid due installment, current: {}", dueInstallment.getInstallmentNumber());
                    Money outStandingFee;
                    if (loanTransaction.claimType().equals(AdvancedPaymentScheduleTransactionProcessor.INSURANCE_PARAM)) {
                        outStandingFee = dueInstallment.getFeeChargesOutstandingByType(currency,
                                AdvancedPaymentScheduleTransactionProcessor.MANDATORY_INSURANCE_PARAM);
                    } else {
                        outStandingFee = dueInstallment.getFeeChargesOutstandingByType(currency, "Aval");
                    }
                    outStandingFee = outStandingFee.plus(dueInstallment.getFeeChargesOutstandingByType(currency,
                            AdvancedPaymentScheduleTransactionProcessor.HONORARIOS_PARAM));
                    if (dueInstallment.getTotalOutstanding(currency).isEqualTo(outStandingFee)) {
                        Integer installment = dueInstallment.getInstallmentNumber();
                        log.debug("Installment {} has only fee charges, moving to next", installment);
                        dueInstallment = installments.stream().filter(LoanRepaymentScheduleInstallment::isNotFullyPaidOff)
                                .filter(e -> loanTransaction.isOnOrBetween(e.getFromDate(), e.getDueDate())
                                        && e.getInstallmentNumber() > installment)
                                .min(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).orElse(null);
                        log.debug("Next due installment: {}", dueInstallment != null ? dueInstallment.getInstallmentNumber() : "null");
                    } else {
                        found = true;
                        log.debug("Found valid due installment: {}", dueInstallment.getInstallmentNumber());
                    }
                }
            }
            log.info("Claim type processing completed - Found valid installment: {}", found);
            // For having similar logic we are populating installment list even when the future installment
            // allocation rule is NEXT_INSTALLMENT or LAST_INSTALLMENT hence the list has only one element.
            // As per SU+ requirements, advance payment goes to outstanding balance so first immediate advance
            // installment
            // will always be seleted
            List<LoanRepaymentScheduleInstallment> inAdvanceInstallments = new ArrayList<>();
            if (FutureInstallmentAllocationRule.REAMORTIZATION.equals(futureInstallmentAllocationRule)) {
                inAdvanceInstallments = installments.stream().filter(LoanRepaymentScheduleInstallment::isNotFullyPaidOff)
                        .filter(e -> loanTransaction.isBefore(e.getFromDate()))
                        .max(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).stream().toList();
            } else if (FutureInstallmentAllocationRule.NEXT_INSTALLMENT.equals(futureInstallmentAllocationRule)) {
                inAdvanceInstallments = installments.stream().filter(LoanRepaymentScheduleInstallment::isNotFullyPaidOff)
                        .filter(e -> !loanTransaction.isAfter(e.getFromDate()))
                        .min(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).stream().toList();
            } else if (FutureInstallmentAllocationRule.LAST_INSTALLMENT.equals(futureInstallmentAllocationRule)) {
                inAdvanceInstallments = installments.stream().filter(LoanRepaymentScheduleInstallment::isNotFullyPaidOff)
                        .filter(e -> loanTransaction.isBefore(e.getFromDate()))
                        .max(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).stream().toList();
            }

            log.info("In advance installments count: {}", inAdvanceInstallments.size());

            int firstNormalInstallmentNumber = LoanRepaymentScheduleProcessingWrapper.fetchFirstNormalInstallmentNumber(installments);
            boolean stopProcessingAdvanceInstallment = false;
            for (PaymentAllocationType paymentAllocationType : paymentAllocationTypes) {
                log.info("Processing allocation type: {} for due type: {}", paymentAllocationType.getAllocationType(),
                        paymentAllocationType.getDueType());

                switch (paymentAllocationType.getDueType()) {
                    case PAST_DUE -> {
                        log.info("Entering PAST_DUE case - Oldest past due installment: {}",
                                oldestPastDueInstallment != null ? oldestPastDueInstallment.getInstallmentNumber() : "null");
                        if (oldestPastDueInstallment != null) {
                            log.info("Processing PAST_DUE allocation for installment: {}", oldestPastDueInstallment.getInstallmentNumber());
                            Set<LoanCharge> oldestPastDueInstallmentCharges = getLoanChargesOfInstallment(charges, oldestPastDueInstallment,
                                    firstNormalInstallmentNumber);
                            LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                    transactionMappings, loanTransaction, oldestPastDueInstallment, currency);
                            paidPortion = processPaymentAllocation(paymentAllocationType, oldestPastDueInstallment, loanTransaction,
                                    transactionAmountUnprocessed, loanTransactionToRepaymentScheduleMapping,
                                    oldestPastDueInstallmentCharges, balances, LoanRepaymentScheduleInstallment.PaymentAction.PAY);
                            log.info("PAST_DUE paid portion: {}", paidPortion.getAmount());
                            transactionAmountUnprocessed = transactionAmountUnprocessed.minus(paidPortion);
                            log.info("PAST_DUE remaining amount: {}", transactionAmountUnprocessed.getAmount());

                            // Log the next iteration details
                            log.info("PAST_DUE processing completed - Will continue to next allocation type");

                            // Additional safeguard: if no progress is made, force exit
                            if (paidPortion.isZero()) {
                                log.warn("PAST_DUE allocation returned zero amount - forcing exit to prevent infinite loop");
                                exit = true;
                            } else {
                                log.info("PAST_DUE allocation made progress - continuing to next allocation type");
                            }
                        } else {
                            log.info("No past due installment found, setting exit flag");
                            exit = true;
                        }
                    }
                    case DUE -> {
                        if (dueInstallment != null) {
                            log.debug("Processing DUE allocation for installment: {}", dueInstallment.getInstallmentNumber());
                            Set<LoanCharge> dueInstallmentCharges = getLoanChargesOfInstallment(charges, dueInstallment,
                                    firstNormalInstallmentNumber);
                            LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                    transactionMappings, loanTransaction, dueInstallment, currency);
                            paidPortion = processPaymentAllocation(paymentAllocationType, dueInstallment, loanTransaction,
                                    transactionAmountUnprocessed, loanTransactionToRepaymentScheduleMapping, dueInstallmentCharges,
                                    balances, LoanRepaymentScheduleInstallment.PaymentAction.PAY);
                            log.debug("DUE paid portion: {}", paidPortion.getAmount());
                            transactionAmountUnprocessed = transactionAmountUnprocessed.minus(paidPortion);
                            log.debug("DUE remaining amount: {}", transactionAmountUnprocessed.getAmount());

                            // Additional safeguard: if no progress is made, force exit
                            if (paidPortion.isZero()) {
                                log.warn("DUE allocation returned zero amount - forcing exit to prevent infinite loop");
                                exit = true;
                            } else {
                                exit = true;
                            }
                        } else {
                            log.debug("No due installment found, setting exit flag");
                            exit = true;
                        }
                    }
                    case IN_ADVANCE -> {
                        if (loanTransaction.doNotProcessAdvanceInstallments() || stopProcessingAdvanceInstallment) {
                            // This condition will only be true if loan processing type is VERTICAL.
                            // For vertical payments, Past Due and Due installments MUST be processed Horizontally
                            log.debug(
                                    "Skipping IN_ADVANCE processing - doNotProcessAdvanceInstallments: {}, stopProcessingAdvanceInstallment: {}",
                                    loanTransaction.doNotProcessAdvanceInstallments(), stopProcessingAdvanceInstallment);
                            exit = true;
                        } else {
                            int numberOfInstallments = inAdvanceInstallments.size();
                            log.debug("Processing IN_ADVANCE allocation for {} installments", numberOfInstallments);
                            if (numberOfInstallments > 0) {
                                Money zero = transactionAmountUnprocessed.zero();
                                for (LoanRepaymentScheduleInstallment inAdvanceInstallment : inAdvanceInstallments) {
                                    if (transactionAmountUnprocessed.isGreaterThanZero()) {
                                        log.debug("Processing advance installment: {}", inAdvanceInstallment.getInstallmentNumber());
                                        String productName = inAdvanceInstallment.getLoan().getLoanProduct().getName();
                                        // if (inAdvanceInstallment.isMigratedInstallment() ||
                                        // (inAdvanceInstallment.getLoan().isMigratedLoan()
                                        // && (productName.contains(LoanProductType.CREDITO_ROTATIVO.getCode())
                                        // || productName.contains(LoanProductType.NANO_CREDITO.getCode())))) {
                                        if (inAdvanceInstallment.isMigratedInstallment()) {
                                            log.debug("Processing migrated installment as due/past due");
                                            // Process migrated installments as due or past due installments
                                            Set<LoanCharge> inAdvanceInstallmentCharges = getLoanChargesOfInstallment(charges,
                                                    inAdvanceInstallment, firstNormalInstallmentNumber);
                                            LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                                    transactionMappings, loanTransaction, inAdvanceInstallment, currency);
                                            paidPortion = processPaymentAllocation(paymentAllocationType, inAdvanceInstallment,
                                                    loanTransaction, transactionAmountUnprocessed,
                                                    loanTransactionToRepaymentScheduleMapping, inAdvanceInstallmentCharges, balances,
                                                    LoanRepaymentScheduleInstallment.PaymentAction.PAY);
                                            log.debug("Migrated installment paid portion: {}", paidPortion.getAmount());
                                            transactionAmountUnprocessed = transactionAmountUnprocessed.minus(paidPortion);
                                            log.debug("Migrated installment remaining amount: {}",
                                                    transactionAmountUnprocessed.getAmount());

                                            // Additional safeguard: if no progress is made, force exit
                                            if (paidPortion.isZero()) {
                                                log.warn(
                                                        "Migrated installment allocation returned zero amount - forcing exit to prevent infinite loop");
                                                exit = true;
                                            }
                                        } else {
                                            if (inAdvanceInstallment.isLastInstallment(installments)
                                                    && inAdvanceInstallment.isOverpaidInAdvance(currency) && transactionAmountUnprocessed
                                                            .isGreaterThanOrEqualTo(inAdvanceInstallment.getPrincipal(currency))) {
                                                log.debug("Processing advance overpayment for last installment");
                                                // This MUST be true only in case of advance overpayment after repayment
                                                // schedule is regenerated
                                                // Process principal and move the remaining amount to overpaid

                                                Money paidPrincipalComponent = inAdvanceInstallment.payPrincipalComponent(
                                                        loanTransaction.getTransactionDate(), transactionAmountUnprocessed, false,
                                                        loanTransaction);

                                                inAdvanceInstallment.setAdvancePrincipalAmount(inAdvanceInstallment
                                                        .getAdvancePrincipalAmount().add(transactionAmountUnprocessed.getAmount()));

                                                balances.setAggregatedPrincipalPortion(
                                                        balances.getAggregatedPrincipalPortion().add(transactionAmountUnprocessed));
                                                LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                                        transactionMappings, loanTransaction, inAdvanceInstallment, currency);
                                                addToTransactionMapping(loanTransactionToRepaymentScheduleMapping,
                                                        transactionAmountUnprocessed, zero, zero, zero);
                                                transactionAmountUnprocessed = transactionAmountUnprocessed.minus(paidPrincipalComponent);
                                                stopProcessingAdvanceInstallment = true;
                                                log.debug("Advance overpayment processed, remaining amount: {}",
                                                        transactionAmountUnprocessed.getAmount());

                                            } else {
                                                log.debug("Processing regular advance payment");
                                                balances.setAggregatedPrincipalPortion(
                                                        balances.getAggregatedPrincipalPortion().add(transactionAmountUnprocessed));
                                                inAdvanceInstallment.checkIfRepaymentPeriodObligationsAreMet(
                                                        loanTransaction.getTransactionDate(), currency);

                                                inAdvanceInstallment.trackAdvanceAndLateTotalsForRepaymentPeriod(
                                                        loanTransaction.getTransactionDate(), currency, transactionAmountUnprocessed);
                                                inAdvanceInstallment.setAdvancePrincipalAmount(inAdvanceInstallment
                                                        .getAdvancePrincipalAmount().add(transactionAmountUnprocessed.getAmount()));

                                                // Handle on date repayments - it was not filling in advance column
                                                if (inAdvanceInstallment.getFromDate().isEqual(loanTransaction.getTransactionDate())) {
                                                    inAdvanceInstallment
                                                            .setTotalPaidInAdvance(inAdvanceInstallment.getTotalPaidInAdvance(currency)
                                                                    .getAmount().add(transactionAmountUnprocessed.getAmount()));
                                                }

                                                inAdvanceInstallment.setRecalculateEMI(loanTransaction.recalculateEMI());
                                                LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                                        transactionMappings, loanTransaction, inAdvanceInstallment, currency);
                                                addToTransactionMapping(loanTransactionToRepaymentScheduleMapping,
                                                        transactionAmountUnprocessed, zero, zero, zero);

                                                transactionAmountUnprocessed = Money.zero(currency);
                                                log.debug("Regular advance payment processed, remaining amount: {}",
                                                        transactionAmountUnprocessed.getAmount());
                                            }
                                        }
                                    }
                                }
                                exit = true;
                                log.debug("IN_ADVANCE processing completed, setting exit flag");
                            } else {
                                log.debug("No advance installments found, setting exit flag");
                                exit = true;
                            }
                        }
                    }
                }
            }
        }
        // We are allocating till there is no pending installment or there is no more unprocessed transaction amount
        // or there is no more outstanding balance of the allocation type
        while (!exit && installments.stream().anyMatch(LoanRepaymentScheduleInstallment::isNotFullyPaidOff)
                && transactionAmountUnprocessed.isGreaterThanZero());

        log.info("Completed processAllocationsHorizontally - Final unprocessed amount: {}, Exit flag: {}, Loop iterations: {}",
                transactionAmountUnprocessed.getAmount(), exit, loopIteration);
        return transactionAmountUnprocessed;
    }

    private Money processPaymentAllocationComponent(LoanTransaction loanTransaction, MonetaryCurrency currency,
            Money transactionAmountUnprocessed, List<LoanTransactionToRepaymentScheduleMapping> transactionMappings,
            Set<LoanCharge> charges, Balances balances, PaymentAllocationType paymentAllocationType,
            LoanRepaymentScheduleInstallment inScopeInstallment, int firstNormalInstallmentNumber) {
        Money paidPortion;
        Set<LoanCharge> dueInstallmentCharges = getLoanChargesOfInstallment(charges, inScopeInstallment, firstNormalInstallmentNumber);
        LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(transactionMappings,
                loanTransaction, inScopeInstallment, currency);
        paidPortion = processPaymentAllocation(paymentAllocationType, inScopeInstallment, loanTransaction, transactionAmountUnprocessed,
                loanTransactionToRepaymentScheduleMapping, dueInstallmentCharges, balances,
                LoanRepaymentScheduleInstallment.PaymentAction.PAY);
        transactionAmountUnprocessed = transactionAmountUnprocessed.minus(paidPortion);
        return transactionAmountUnprocessed;
    }

    @NotNull
    private static Set<LoanCharge> getLoanChargesOfInstallment(Set<LoanCharge> charges, LoanRepaymentScheduleInstallment currentInstallment,
            int firstNormalInstallmentNumber) {
        List<LoanCharge> flatChargesForInstallment = charges.stream()
                .filter(loanCharge -> loanCharge.isFlatSpecificDueDateChargeForInstallment(currentInstallment)).toList();
        currentInstallment.setFlatSpecificDueDateCharges(flatChargesForInstallment);
        return charges.stream().filter(loanCharge -> loanCharge.isDueForCollectionForInstallment(currentInstallment)
                || currentInstallment.getInstallmentCharges().stream().filter(li -> li.getLoanCharge().isLifeInsurance()).count() > 0)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings({ "squid:S3776" })
    private Money processPeriodsVertically(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, Money transactionAmountUnprocessed,
            LoanPaymentAllocationRule paymentAllocationRule, List<LoanTransactionToRepaymentScheduleMapping> transactionMappings,
            Set<LoanCharge> charges, Balances balances) {
        int firstNormalInstallmentNumber = LoanRepaymentScheduleProcessingWrapper.fetchFirstNormalInstallmentNumber(installments);
        for (PaymentAllocationType paymentAllocationType : paymentAllocationRule.getAllocationTypes()) {
            if (transactionAmountUnprocessed.isZero()) {
                break;
            }
            if (!paymentAllocationType.getDueType().equals(IN_ADVANCE)) {
                // Only process ADVANCE PAYMENTS VERTICALLY
                continue;
            }
            FutureInstallmentAllocationRule futureInstallmentAllocationRule = paymentAllocationRule.getFutureInstallmentAllocationRule();
            LoanRepaymentScheduleInstallment currentInstallment = null;
            Money paidPortion = Money.zero(currency);
            do {
                Predicate<LoanRepaymentScheduleInstallment> predicate = getFilterPredicate(paymentAllocationType, currency,
                        loanTransaction);
                switch (paymentAllocationType.getDueType()) {
                    case PAST_DUE -> {
                        currentInstallment = installments.stream().filter(predicate).filter(e -> loanTransaction.isAfter(e.getDueDate()))
                                .min(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).orElse(null);
                        if (currentInstallment != null) {
                            Set<LoanCharge> oldestPastDueInstallmentCharges = getLoanChargesOfInstallment(charges, currentInstallment,
                                    firstNormalInstallmentNumber);
                            LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                    transactionMappings, loanTransaction, currentInstallment, currency);
                            paidPortion = processPaymentAllocation(paymentAllocationType, currentInstallment, loanTransaction,
                                    transactionAmountUnprocessed, loanTransactionToRepaymentScheduleMapping,
                                    oldestPastDueInstallmentCharges, balances, LoanRepaymentScheduleInstallment.PaymentAction.PAY);
                            transactionAmountUnprocessed = transactionAmountUnprocessed.minus(paidPortion);
                        }
                    }
                    case DUE -> {
                        currentInstallment = installments.stream().filter(predicate).filter(e -> loanTransaction.isOn(e.getDueDate()))
                                .min(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).orElse(null);
                        if (currentInstallment != null) {
                            Set<LoanCharge> dueInstallmentCharges = getLoanChargesOfInstallment(charges, currentInstallment,
                                    firstNormalInstallmentNumber);
                            LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                    transactionMappings, loanTransaction, currentInstallment, currency);
                            paidPortion = processPaymentAllocation(paymentAllocationType, currentInstallment, loanTransaction,
                                    transactionAmountUnprocessed, loanTransactionToRepaymentScheduleMapping, dueInstallmentCharges,
                                    balances, LoanRepaymentScheduleInstallment.PaymentAction.PAY);
                            transactionAmountUnprocessed = transactionAmountUnprocessed.minus(paidPortion);
                        }
                    }
                    case IN_ADVANCE -> {
                        // For having similar logic we are populating installment list even when the future installment
                        // allocation rule is NEXT_INSTALLMENT or LAST_INSTALLMENT hence the list has only one element.
                        List<LoanRepaymentScheduleInstallment> currentInstallments = new ArrayList<>();
                        if (FutureInstallmentAllocationRule.REAMORTIZATION.equals(futureInstallmentAllocationRule)) {
                            currentInstallments = installments.stream().filter(predicate)
                                    .filter(e -> loanTransaction.isBefore(e.getFromDate())).toList();
                        } else if (FutureInstallmentAllocationRule.NEXT_INSTALLMENT.equals(futureInstallmentAllocationRule)) {
                            currentInstallments = installments.stream().filter(predicate)
                                    .filter(e -> loanTransaction.isBefore(e.getFromDate()))
                                    .min(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).stream().toList();
                        } else if (FutureInstallmentAllocationRule.LAST_INSTALLMENT.equals(futureInstallmentAllocationRule)) {
                            currentInstallments = installments.stream().filter(predicate)
                                    .filter(e -> loanTransaction.isBefore(e.getFromDate()))
                                    .max(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber)).stream().toList();
                        }
                        int numberOfInstallments = currentInstallments.size();
                        paidPortion = Money.zero(currency);
                        if (numberOfInstallments > 0) {
                            // This will be the same amount as transactionAmountUnprocessed in case of the future
                            // installment allocation is NEXT_INSTALLMENT or LAST_INSTALLMENT
                            Money evenPortion = transactionAmountUnprocessed.dividedBy(numberOfInstallments, MoneyHelper.getRoundingMode());
                            // Adjustment might be needed due to the divide operation and the rounding mode
                            Money balanceAdjustment = transactionAmountUnprocessed.minus(evenPortion.multipliedBy(numberOfInstallments));
                            for (LoanRepaymentScheduleInstallment internalCurrentInstallment : currentInstallments) {
                                currentInstallment = internalCurrentInstallment;
                                Set<LoanCharge> inAdvanceInstallmentCharges = getLoanChargesOfInstallment(charges, currentInstallment,
                                        firstNormalInstallmentNumber);
                                // Adjust the portion for the last installment
                                if (internalCurrentInstallment.equals(currentInstallments.get(numberOfInstallments - 1))) {
                                    evenPortion = evenPortion.add(balanceAdjustment);
                                }
                                LoanTransactionToRepaymentScheduleMapping loanTransactionToRepaymentScheduleMapping = getTransactionMapping(
                                        transactionMappings, loanTransaction, currentInstallment, currency);
                                Money internalPaidPortion = processPaymentAllocation(paymentAllocationType, currentInstallment,
                                        loanTransaction, evenPortion, loanTransactionToRepaymentScheduleMapping,
                                        inAdvanceInstallmentCharges, balances, LoanRepaymentScheduleInstallment.PaymentAction.PAY);
                                // Some extra logic to allocate as much as possible across the installments if the
                                // outstanding balances are different
                                if (internalPaidPortion.isGreaterThanZero()) {
                                    paidPortion = internalPaidPortion;
                                }
                                transactionAmountUnprocessed = transactionAmountUnprocessed.minus(internalPaidPortion);
                            }
                        } else {
                            currentInstallment = null;
                        }
                    }
                }
            }
            // We are allocating till there is no pending installment or there is no more unprocessed transaction amount
            // or there is no more outstanding balance of the allocation type
            while (currentInstallment != null && transactionAmountUnprocessed.isGreaterThanZero() && paidPortion.isGreaterThanZero());
        }
        return transactionAmountUnprocessed;
    }

    private Predicate<LoanRepaymentScheduleInstallment> getFilterPredicate(PaymentAllocationType paymentAllocationType,
            MonetaryCurrency currency, LoanTransaction loanTransaction) {
        return switch (paymentAllocationType.getAllocationType()) {
            case PENALTY -> p -> p.getPenaltyChargesOutstanding(currency).isGreaterThanZero();
            case FEE -> p -> p.getFeeChargesOutstanding(currency).isGreaterThanZero();
            case FEES -> p -> p.getFeeChargesOutstandingByType(currency, AdvancedPaymentScheduleTransactionProcessor.HONORARIOS_PARAM)
                    .isGreaterThanZero();
            case AVAL -> p -> !loanTransaction.isAvalClaim() && p.getFeeChargesOutstandingByType(currency, "Aval").isGreaterThanZero();
            case MANDATORY_INSURANCE -> p -> !loanTransaction.isInsuranceClaim()
                    && p.getFeeChargesOutstandingByType(currency, AdvancedPaymentScheduleTransactionProcessor.MANDATORY_INSURANCE_PARAM)
                            .isGreaterThanZero();
            case VOLUNTARY_INSURANCE -> p -> p.getFeeChargesOutstandingByType(currency, "VoluntaryInsurance").isGreaterThanZero();
            case INTEREST -> p -> p.getInterestOutstanding(currency).isGreaterThanZero();
            case PRINCIPAL -> p -> p.getPrincipalOutstanding(currency).isGreaterThanZero();
        };
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static final class Balances {

        private Money aggregatedPrincipalPortion;
        private Money aggregatedFeeChargesPortion;
        private Money aggregatedInterestPortion;
        private Money aggregatedPenaltyChargesPortion;
    }
}
