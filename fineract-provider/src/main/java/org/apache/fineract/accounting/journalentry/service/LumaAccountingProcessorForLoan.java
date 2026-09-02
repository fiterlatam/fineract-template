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
package org.apache.fineract.accounting.journalentry.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.fineract.accounting.journalentry.data.LumaBitacoraTransactionTypeEnum;
import org.apache.fineract.accounting.journalentry.domain.BitaCoraDetails;
import org.apache.fineract.accounting.journalentry.domain.BitaCoraMaster;
import org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.apache.fineract.portfolio.cupo.domain.Cupo;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargePaidBy;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LumaAccountingProcessorForLoan extends LumaAccountingProcessor {

    // LH-1039: por el momento, enviar el número 999 para los registros de la cartera
    private static final Integer LOAN_CAUSAL = 999;

    private CurrencyReadPlatformService currencyReadPlatformService;
    // private final BusinessEventNotifierService businessEventNotifierService;
    // private ExternalGuaranteeLoanRepository externalGuaranteeLoanRepository;

    @Autowired
    public LumaAccountingProcessorForLoan(CurrencyReadPlatformService currencyReadPlatformService) {
        this.currencyReadPlatformService = currencyReadPlatformService;
        // this.businessEventNotifierService = businessEventNotifierService;
        // this.externalGuaranteeLoanRepository = externalGuaranteeLoanRepository;
    }

    /*
     * @PostConstruct public void addListeners() { this.businessEventNotifierService.addBusinessEventPostListeners(
     * BusinessEventNotificationConstants.BusinessEvents.LOAN_ADJUST_TRANSACTION, new AdjustFundsOnBusinessEvent());
     * this.businessEventNotifierService.addBusinessEventPostListeners(
     * BusinessEventNotificationConstants.BusinessEvents.LOAN_MAKE_REPAYMENT, new OnLoanRepaymentTransactionEvent());
     * this.businessEventNotifierService.addBusinessEventPostListeners(BusinessEventNotificationConstants.BusinessEvents
     * .LOAN_FORECLOSURE, new OnLoanRepaymentTransactionEvent());
     * this.businessEventNotifierService.addBusinessEventPostListeners(BusinessEventNotificationConstants.BusinessEvents
     * .LOAN_DISBURSAL, new OnLoanDisbursementTransactionEvent());
     * this.businessEventNotifierService.addBusinessEventPostListeners(
     * BusinessEventNotificationConstants.BusinessEvents.LOAN_UNDO_DISBURSAL, new OnLoanUndoDisburseEvent());
     * this.businessEventNotifierService.addBusinessEventPostListeners(
     * BusinessEventNotificationConstants.BusinessEvents.LOAN_UNDO_FORMALIZE, new OnLoanUndoFormalizeEvent());
     * this.businessEventNotifierService.addBusinessEventPostListeners(
     * BusinessEventNotificationConstants.BusinessEvents.LOAN_UNDO_APPROVAL, new OnLoanUndoApprovalEvent()); }
     *
     * private class OnLoanUndoApprovalEvent implements BusinessEventListener {
     *
     * @Override public void businessEventToBeExecuted(
     *
     * @SuppressWarnings("unused") Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity)
     * {}
     *
     * @Override public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BusinessEntity, Object>
     * businessEventEntity) { Object entity =
     * businessEventEntity.get(BusinessEventNotificationConstants.BusinessEntity.LOAN); if (entity instanceof Loan) {
     * Loan loan = (Loan) entity; reverseLastLogForLoanAndTrxType(loan.getId(),
     * BitacoraMasterConstants.TRX_TYPE_APROBACION); } } }
     *
     * private class OnLoanUndoFormalizeEvent implements BusinessEventListener {
     *
     * @Override public void businessEventToBeExecuted(
     *
     * @SuppressWarnings("unused") Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity)
     * {}
     *
     * @Override public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BusinessEntity, Object>
     * businessEventEntity) { Object entity =
     * businessEventEntity.get(BusinessEventNotificationConstants.BusinessEntity.LOAN); if (entity instanceof Loan) {
     * Loan loan = (Loan) entity; reverseLastLogForLoanAndTrxType(loan.getId(),
     * BitacoraMasterConstants.TRX_TYPE_FORMALIZACION); } } }
     *
     * private class OnLoanUndoDisburseEvent implements BusinessEventListener {
     *
     * @Override public void businessEventToBeExecuted(
     *
     * @SuppressWarnings("unused") Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity)
     * {}
     *
     * @Override public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BusinessEntity, Object>
     * businessEventEntity) { Object entity =
     * businessEventEntity.get(BusinessEventNotificationConstants.BusinessEntity.LOAN); if (entity instanceof Loan) {
     * Loan loan = (Loan) entity; for (LoanTransaction loanTransaction : loan.getReversedTransacionsAfterUndoDisburse())
     * { reverseLogForTransactionId(loanTransaction.getId(), BitacoraMasterConstants.ACCOUNT_TYPE_PR); } } } }
     *
     * private class OnLoanDisbursementTransactionEvent implements BusinessEventListener {
     *
     * @Override public void businessEventToBeExecuted(
     *
     * @SuppressWarnings("unused") Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity)
     * {}
     *
     * @Override public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BusinessEntity, Object>
     * businessEventEntity) { Object entity =
     * businessEventEntity.get(BusinessEventNotificationConstants.BusinessEntity.LOAN); if (entity instanceof Loan) {
     * Loan loan = (Loan) entity; LoanTransaction loanDisbursementTransaction = null; LoanTransaction
     * repaymentAtDisbursementTransaction = null; for (LoanTransaction loanTransaction : loan.getLoanTransactions()) {
     * if (loanTransaction.isNotReversed()) { if (loanTransaction.isDisbursement()) { BitaCoraMaster existingLog =
     * findMasterLogForTransactionId(loanTransaction.getId(), BitacoraMasterConstants.ACCOUNT_TYPE_PR); if (existingLog
     * == null) { loanDisbursementTransaction = loanTransaction; } } else if
     * (loanTransaction.isRepaymentAtDisbursement()) { repaymentAtDisbursementTransaction = loanTransaction; } } } if
     * (loanDisbursementTransaction != null) { BitaCoraMaster bitaCoraMaster =
     * createJournalEntryForLoanDisbursement(loanDisbursementTransaction, loan, repaymentAtDisbursementTransaction);
     * saveMasterLogAndDetails(bitaCoraMaster); } } } }
     *
     * private class OnLoanRepaymentTransactionEvent implements BusinessEventListener {
     *
     * @Override public void businessEventToBeExecuted(
     *
     * @SuppressWarnings("unused") Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity)
     * {}
     *
     * @Override public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BusinessEntity, Object>
     * businessEventEntity) { Object entity =
     * businessEventEntity.get(BusinessEventNotificationConstants.BusinessEntity.LOAN_TRANSACTION); if (entity
     * instanceof LoanTransaction) { LoanTransaction loanTransaction = (LoanTransaction) entity; BitaCoraMaster
     * bitaCoraMaster = createJournalEntryForLoanRepayment(loanTransaction); saveMasterLogAndDetails(bitaCoraMaster); }
     * } }
     *
     * private class AdjustFundsOnBusinessEvent implements BusinessEventListener {
     *
     * @Override public void businessEventToBeExecuted(
     *
     * @SuppressWarnings("unused") Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity)
     * {}
     *
     * @Override public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BusinessEntity, Object>
     * businessEventEntity) { Object entity =
     * businessEventEntity.get(BusinessEventNotificationConstants.BusinessEntity.LOAN_ADJUSTED_TRANSACTION); if (entity
     * instanceof LoanTransaction) { LoanTransaction loanTransaction = (LoanTransaction) entity;
     * reverseLogForTransactionId(loanTransaction.getId(), BitacoraMasterConstants.ACCOUNT_TYPE_PR); } } }
     *
     * public BitaCoraMaster createJournalEntriesForDisbursementChargesAccrual(final
     * LoanDisbursementChargeAccrualDetails details) {
     *
     * Loan loan = details.getLoancharge().getLoan(); CurrencyData currencyData =
     * this.currencyReadPlatformService.retrieveCurrency(loan.getCurrencyCode());
     *
     * String status = getStatusStringFromLoanNpa(loan.isNpa()); Date transactionDate = Date
     * .from(details.getInstallment().getDueDate().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant()); final
     * BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate); String currencyString =
     * currencyData.getIntCode().toString(); BigDecimal amount = details.getAmount(); String groupString =
     * getGroupStringFromLoan(loan); String typeOfCredit = getTypeOfLoanString(loan); String loanCategory =
     * loan.getCategory(); Long transactionId = null;
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate, BitacoraMasterConstants.TRX_TYPE_DEVENGO_COMISION,
     * transactionId, loan.getId(), BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount, status,
     * groupString, typeOfCredit, loanCategory); master.setOffice(loan.getOffice());
     * master.setExchangeRate(exchangeRate); master.setClientId(loan.getLumaClientId());
     *
     * String amountType = details.getLoancharge().getCharge().getAmountTypeCode().getDescription(); Integer headingCode
     * = details.getLoancharge().getId().intValue();
     *
     * BigDecimal differentialAmount = amount.multiply(exchangeRate); BitaCoraDetails masterDetails = new
     * BitaCoraDetails("DEVENGO", amount, LOAN_CAUSAL, null, headingCode, amountType, null, null, null, master,
     * differentialAmount); master.addBitaCoraDetails(masterDetails);
     *
     * if (master.isGreaterThanZero()) return master; return null; }
     *
     * public BitaCoraMaster createJournalEntriesForGuaranteeHold(final Guarantee guarantee, BigDecimal amount, Loan
     * loan) {
     *
     * String status = getStatusStringFromLoanNpa(loan.isNpa()); Date transactionDate =
     * Date.from(guarantee.getCreatedDate().get()); CurrencyData currencyData =
     * this.currencyReadPlatformService.retrieveCurrency(guarantee.getCurrencyCode()); final BigDecimal exchangeRate =
     * getExchangeRate(currencyData.code(), transactionDate); String currencyString =
     * currencyData.getIntCode().toString(); String groupString = getGroupStringFromLoan(loan); String typeOfCredit =
     * getTypeOfLoanString(loan); String loanCategory = loan.getCategory(); Long transactionId = null;
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate, BitacoraMasterConstants.TRX_TYPE_REGISTRO_GARANTIA,
     * transactionId, guarantee.getId(), BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString,
     * guarantee.getTotalAmount(), status, groupString, typeOfCredit, loanCategory); master.setOffice(loan.getOffice());
     * master.setExchangeRate(exchangeRate); master.setClientId(loan.getLumaClientId());
     *
     * BigDecimal differentialAmount = amount.multiply(exchangeRate); BitaCoraDetails masterDetails = new
     * BitaCoraDetails("MontoGarantia", guarantee.getTotalAmount(), LOAN_CAUSAL, null,
     * Integer.valueOf(guarantee.getType().label()), guarantee.getId().toString(), null, null, null, master,
     * differentialAmount); master.addBitaCoraDetails(masterDetails);
     *
     * if (master.isGreaterThanZero()) return master; return null; }
     *
     * public BitaCoraMaster createJournalEntriesForAccrualInterest(final CurrencyData currencyData, BigDecimal
     * totalAccruedInterest, BigDecimal totalAccruedExceedingInterest, final LocalDate accruedTill, Long loanId, Long
     * officeId, boolean isNpa, Long clientId, Integer loanGroup, String loanType, String loanCategory, Long
     * transactionId) { String status = getStatusStringFromLoanNpa(isNpa); Date transactionDate =
     * Date.from(accruedTill.atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant()); final BigDecimal
     * exchangeRate = getExchangeRate(currencyData.code(), transactionDate); String currencyString =
     * currencyData.getIntCode().toString(); String groupString = String.valueOf(loanGroup);
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate, BitacoraMasterConstants.TRX_TYPE_CALCULO_INTERESES,
     * transactionId, loanId, BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, totalAccruedInterest, status,
     * groupString, loanType, loanCategory); master.setClientId(clientId); master.setOfficeId(officeId);
     * master.setExchangeRate(exchangeRate);
     *
     * BigDecimal differentialAmount = totalAccruedInterest.multiply(exchangeRate); String amountType = "INTERESES";
     * String headingValue = "Descripción \"Cálculo de intereses " +
     * accruedTill.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag("es_GT"))) + "\"";
     * BitaCoraDetails masterDetailsIntereses = new BitaCoraDetails(headingValue, totalAccruedInterest, LOAN_CAUSAL,
     * null, null, amountType, null, null, null, master, differentialAmount);
     * master.addBitaCoraDetails(masterDetailsIntereses);
     *
     * if (totalAccruedExceedingInterest.compareTo(BigDecimal.ZERO) > 0) { BigDecimal
     * differentialAmounttotalAccruedExceedingInterest = totalAccruedExceedingInterest.multiply(exchangeRate);
     * BitaCoraDetails masterDetailsInteresesAnticipado = new BitaCoraDetails("Descarga intereses anticipados",
     * totalAccruedExceedingInterest, LOAN_CAUSAL, currencyString, null, "REBAJAINTERESANTICIPADO", null, null, null,
     * master, differentialAmounttotalAccruedExceedingInterest);
     * master.addBitaCoraDetails(masterDetailsInteresesAnticipado); }
     *
     * if (master.isGreaterThanZero()) return master; return null; }
     *
     * public BitaCoraMaster createJournalEntriesForGuaranteeRelease(final Guarantee guarantee, GuaranteeTransaction
     * guaranteeTransaction) { CurrencyData currencyData =
     * this.currencyReadPlatformService.retrieveCurrency(guarantee.getCurrencyCode()); String currencyString =
     * currencyData.getIntCode().toString(); String status = "VIG"; Date transactionDate = DateUtils.getDateOfTenant();
     * final BigDecimal exchangeRate = getExchangeRate(guarantee.getCurrencyCode(), transactionDate); BigDecimal amount
     * = guaranteeTransaction.getAmount(); String groupString = "1"; String typeOfCredit = "1"; String loanCategory =
     * "A"; Long transactionId = guaranteeTransaction.getId();
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate, BitacoraMasterConstants.TRX_TYPE_LIBERACION_GARANTIA,
     * transactionId, guarantee.getId(), BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount, status,
     * groupString, typeOfCredit, loanCategory); master.setExchangeRate(exchangeRate);
     *
     * BigDecimal differentialAmount = amount.multiply(exchangeRate); BitaCoraDetails masterDetails = new
     * BitaCoraDetails("MontoGarantia", amount, LOAN_CAUSAL, null, Integer.valueOf(guarantee.getType().label()),
     * guarantee.getId().toString(), null, null, null, master, differentialAmount);
     * master.addBitaCoraDetails(masterDetails);
     *
     * if (master.isGreaterThanZero()) return master; return null; }
     *
     * public BitaCoraMaster createJournalEntryForLoanRepayment(final LoanTransaction loanTransaction) { Loan loan =
     * loanTransaction.getLoan(); CurrencyData currencyData =
     * this.currencyReadPlatformService.retrieveCurrency(loan.getCurrencyCode()); String status =
     * getStatusStringFromLoanNpa(loan.isNpa()); String currencyString = currencyData.getIntCode().toString(); String
     * groupString = getGroupStringFromLoan(loan); String typeOfCredit = getTypeOfLoanString(loan); String loanCategory
     * = loan.getCategory();
     *
     * BigDecimal loanPenaltyChargesPaid = BigDecimal.ZERO; BigDecimal loanDelinquencyChargesPaid = BigDecimal.ZERO;
     * BigDecimal loanAditionalInterestChargesPaid = BigDecimal.ZERO; List<LoanChargePaidBy> loanFeeChargesPaid = new
     * ArrayList<>(); for (final LoanChargePaidBy chargePaidBy : loanTransaction.getLoanChargesPaid()) { if
     * (chargePaidBy.getLoanCharge().isPenaltyCharge()) { if
     * (chargePaidBy.getLoanCharge().getCharge().getName().startsWith(LumaChargesConstants.CHARGE_PREFIX_MORA)) {
     * loanDelinquencyChargesPaid = loanDelinquencyChargesPaid.add(chargePaidBy.getAmount()); } else if
     * (chargePaidBy.getLoanCharge().getCharge().getName()
     * .startsWith(LumaChargesConstants.CHARGE_PREFIX_INTERESADICIONAL)) { loanAditionalInterestChargesPaid =
     * loanAditionalInterestChargesPaid.add(chargePaidBy.getAmount()); } else if
     * (chargePaidBy.getLoanCharge().getCharge().getName().startsWith(LumaChargesConstants.CHARGE_PREFIX_PENALIDAD)) {
     * loanPenaltyChargesPaid = loanPenaltyChargesPaid.add(chargePaidBy.getAmount()); } } else {
     * loanFeeChargesPaid.add(chargePaidBy); } }
     *
     * Date transactionDate = loanTransaction.getDateOf(); String transactionType =
     * BitacoraMasterConstants.TRX_TYPE_PAGO; if (loan.getAccountAssociations() != null &&
     * loan.getAccountAssociations().linkedCupo() != null) { transactionType =
     * BitacoraMasterConstants.TRX_TYPE_PAGO_CUPO; } final BigDecimal exchangeRate =
     * getExchangeRate(currencyData.code(), transactionDate); Long transactionId = loanTransaction.getId(); BigDecimal
     * amount = loanTransaction.getAmount(loan.getCurrency()).getAmount(); String reference =
     * loanTransaction.getPaymentDetail() == null ? null : loanTransaction.getPaymentDetail().getReceiptNumber();
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate, transactionType, transactionId, loan.getId(),
     * BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount, status, groupString, typeOfCredit,
     * loanCategory); master.setOffice(loan.getOffice()); master.setExchangeRate(exchangeRate);
     * master.setClientId(loan.getLumaClientId()); master.setReference(reference);
     *
     * BigDecimal principalDetailsDifferentialAmount =
     * loanTransaction.getPrincipalPortion(loan.getCurrency()).multipliedBy(exchangeRate) .getAmount(); BitaCoraDetails
     * principalDetails = new BitaCoraDetails("Valor de capital pagado al prestamo ".concat(loan.getId().toString()),
     * loanTransaction.getPrincipalPortion(), LOAN_CAUSAL, null, loan.getLoanPurpose() == null ? null :
     * loan.getLoanPurpose().position(), "CAPITAL", 0, 1, "Public", master, principalDetailsDifferentialAmount); if
     * (loanTransaction.getPrincipalPortion(loan.getCurrency()).isGreaterThanZero()) {
     * master.addBitaCoraDetails(principalDetails); }
     *
     * String INTERESES_str = "INTERESES"; String MORA_str = "MORA"; String PENALIDAD_str = "PENALIDAD"; if
     * (loan.isNpa()) { INTERESES_str = "INTERESVENCIDO"; MORA_str = "MORAVENCIDA"; PENALIDAD_str = "PENALIDADVENCIDA";
     * }
     *
     * if (Objects.nonNull(loanTransaction.getExceedingInterestAmount()) &&
     * loanTransaction.getExceedingInterestAmount().compareTo(BigDecimal.ZERO) > 0) { BigDecimal
     * interestAnticipadoDetailsDifferentialAmount =
     * loanTransaction.getExceedingInterestAmount().multiply(exchangeRate); BitaCoraDetails interestDetails = new
     * BitaCoraDetails("Valor int Anticipado pagado ".concat(loan.getId().toString()),
     * loanTransaction.getExceedingInterestAmount(), LOAN_CAUSAL, null, null, "INTERESANTICIPADO", 0, 1, "Public",
     * master, interestAnticipadoDetailsDifferentialAmount); master.addBitaCoraDetails(interestDetails); }
     *
     * if (loanTransaction.getInterestPortion(loan.getCurrency()).isGreaterThanZero()) { BigDecimal interestAmountPaid =
     * loanTransaction.getInterestPortion(loan.getCurrency()).getAmount(); if
     * (loanAditionalInterestChargesPaid.compareTo(BigDecimal.ZERO) > 0) { interestAmountPaid =
     * interestAmountPaid.add(loanAditionalInterestChargesPaid); } if
     * (Objects.nonNull(loanTransaction.getExceedingInterestAmount()) &&
     * loanTransaction.getExceedingInterestAmount().compareTo(BigDecimal.ZERO) > 0) { interestAmountPaid =
     * interestAmountPaid.subtract(loanTransaction.getExceedingInterestAmount()); } if
     * (interestAmountPaid.compareTo(BigDecimal.ZERO) > 0) { BigDecimal interestDetailsDifferentialAmount =
     * interestAmountPaid.multiply(exchangeRate); BitaCoraDetails interestDetails = new
     * BitaCoraDetails("Valor de intereses pagado ptmo ".concat(loan.getId().toString()), interestAmountPaid,
     * LOAN_CAUSAL, "", loan.getLoanPurpose() == null ? null : loan.getLoanPurpose().position(), INTERESES_str, 0, 1,
     * "Public", master, interestDetailsDifferentialAmount); master.addBitaCoraDetails(interestDetails); } }
     *
     * // Penalies if (loanPenaltyChargesPaid.compareTo(BigDecimal.ZERO) > 0) { BigDecimal penalidadDifferential =
     * loanPenaltyChargesPaid.multiply(exchangeRate); BitaCoraDetails penaltiesDetails = new
     * BitaCoraDetails("Valor penalidad pagado ptmo ".concat(loan.getId().toString()), loanPenaltyChargesPaid,
     * LOAN_CAUSAL, "", null, PENALIDAD_str, 0, 1, "Public", master, penalidadDifferential);
     * master.addBitaCoraDetails(penaltiesDetails); }
     *
     * // Delinquency if (loanDelinquencyChargesPaid.compareTo(BigDecimal.ZERO) > 0) { BigDecimal moraDifferential =
     * loanDelinquencyChargesPaid.multiply(exchangeRate); BitaCoraDetails delinquencyDetails = new
     * BitaCoraDetails("Valor mora pagado ptmo ".concat(loan.getId().toString()), loanDelinquencyChargesPaid,
     * LOAN_CAUSAL, null, null, MORA_str, 0, 1, "Public", master, moraDifferential);
     * master.addBitaCoraDetails(delinquencyDetails); }
     *
     * // Fees
     *
     * for (LoanChargePaidBy chargePaidBy : loanFeeChargesPaid) { BigDecimal amountCharge = chargePaidBy.getAmount();
     * BigDecimal amountChargeDifferential = amountCharge.multiply(exchangeRate); Charge charge =
     * chargePaidBy.getLoanCharge().getCharge(); String heading =
     * "Valor del cargo ".concat(charge.getId().toString()).concat(" al ptmo ").concat(loan.getId().toString());
     * BitaCoraDetails feesDetails = new BitaCoraDetails(heading, amountCharge, LOAN_CAUSAL, "",
     * charge.getId().intValue(), charge.getAmountTypeCode().label(), 0, 1, "Public", master, amountChargeDifferential);
     *
     * master.addBitaCoraDetails(feesDetails); }
     *
     * // LS-83 [Log Contable] - Diferencial / PNC-4835 // BigDecimal paymentDifferentialAmount = //
     * amount.multiply(getExchangeRate(loan.getCurrency().getCode())); BigDecimal paymentDifferentialAmount =
     * BigDecimal.ZERO; for (BitaCoraDetails d : master.details()) { d.setDifferential(d.getDifferential().setScale(2,
     * RoundingMode.DOWN)); paymentDifferentialAmount = paymentDifferentialAmount.add(d.getDifferential()); }
     * BitaCoraDetails repaymentDetails = new BitaCoraDetails("Pago al prestamo ".concat(loan.getId().toString()),
     * loanTransaction.getAmount(loan.getCurrency()).getAmount(), LOAN_CAUSAL, null, loan.getLoanPurpose() == null ?
     * null : loan.getLoanPurpose().position(), BitacoraMasterConstants.TRX_TYPE_PAGO, 0, 1, "Public", master,
     * paymentDifferentialAmount.setScale(2, RoundingMode.DOWN)); master.addBitaCoraDetails(repaymentDetails);
     *
     * return master; }
     */

    public BitaCoraMaster createJournalEntryForLoanDisbursement(final LoanTransaction loanTransaction, Loan loan,
            final LoanTransaction repaymentAtDisbursementTransaction) {
        List<LoanCharge> disbursementCharges = new ArrayList<>();
        BigDecimal disbursementFees = BigDecimal.ZERO;
        for (LoanCharge loanCharge : loan.getLoanCharges()) {
            if (loanCharge.isDisbursementCharge() && loanCharge.isActive()) {
                if (repaymentAtDisbursementTransaction != null
                        && repaymentAtDisbursementTransaction.getFeeChargesPortion(loan.getCurrency()).isGreaterThanZero()
                        && repaymentAtDisbursementTransaction.getTransactionDate().isEqual(loanTransaction.getTransactionDate())) {
                    for (LoanChargePaidBy paidBy : repaymentAtDisbursementTransaction.getLoanChargesPaid()) {
                        if (paidBy.getLoanCharge().getId().equals(loanCharge.getId())) {
                            disbursementCharges.add(loanCharge);
                            disbursementFees = disbursementFees.add(loanCharge.getAmount(loan.getCurrency()).getAmount());
                        }
                    }
                }
            }
        }

        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(loan.getCurrencyCode());
        String status = getStatusStringFromLoanNpa(loan.isNpa());
        String currencyString = currencyData.getIntCode().toString();
        String groupString = getGroupStringFromLoan(loan);
        String typeOfCredit = getTypeOfLoanString(loan);

        Date transactionDate = Date.from(loanTransaction.getDateOf().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());

        String transactionType = loan.getDisbursementDetails().size() == 0 ? BitacoraMasterConstants.TRX_TYPE_DESEMBOLSO
                : BitacoraMasterConstants.TRX_TYPE_DESEMBOLSO_MULTITRANCHES;
        if (loan.getAccountAssociations() != null && loan.getAccountAssociations().linkedCupo() != null) {
            transactionType = BitacoraMasterConstants.TRX_TYPE_DESEMBOLSO_CUPO;
        }
        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        Long transactionId = loanTransaction.getId();
        BigDecimal amount = loanTransaction.getAmount(loan.getCurrency()).getAmount().add(disbursementFees);
        String reference = loanTransaction.getPaymentDetail() == null ? null : loanTransaction.getPaymentDetail().getReceiptNumber();

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, transactionType, transactionId, loan.getId(),
                BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount, status, groupString, typeOfCredit);
        master.setOffice(loan.getOffice());
        master.setExchangeRate(exchangeRate);
        master.setClientId(loan.getLumaClientId());
        master.setReference(reference);

        BigDecimal principalDetailsDifferentialAmount = loanTransaction.getAmount(loan.getCurrency()).multipliedBy(exchangeRate)
                .getAmount();
        BitaCoraDetails principalDetails = new BitaCoraDetails(
                "Desembolso " + (loan.getDisbursementDetails().size() == 0 ? " " : "EG ") + "del prestamo: " + loan.getId(),
                loanTransaction.getAmount(loan.getCurrency()).getAmount(), LOAN_CAUSAL, null, 0, "CAPITAL", 1, 1, "Public", master,
                principalDetailsDifferentialAmount);
        master.addBitaCoraDetails(principalDetails);

        for (LoanCharge loanCharge : disbursementCharges) {
            BigDecimal feesDifferentialAmount = Money.of(loan.getCurrency(), loanCharge.getAmount(loan.getCurrency()).getAmount())
                    .multipliedBy(exchangeRate).getAmount();
            BitaCoraDetails disbursementFee = new BitaCoraDetails(loanCharge.getCharge().getName(),
                    loanCharge.getAmount(loan.getCurrency()).getAmount(), LOAN_CAUSAL, "", loanCharge.getCharge().getId().intValue(),
                    "DESEMBOLSOCTA", 1, 1, "Public", master, feesDifferentialAmount);
            master.addBitaCoraDetails(disbursementFee);
        }

        BigDecimal depositNetAmount = loanTransaction.getAmount(loan.getCurrency()).minus(disbursementFees).getAmount();

        // LS-83[Log Contable] - Diferencial / PNC-4835
        // BigDecimal feesDifferentialAmount = Money.of(loan.getCurrency(),
        // depositNetAmount).multipliedBy(exchangeRate).getAmount();
        BigDecimal feesDifferentialAmount = BigDecimal.ZERO;
        for (BitaCoraDetails d : master.details()) {
            d.setDifferential(d.getDifferential().setScale(2, RoundingMode.DOWN));
            if ("CAPITAL".equals(d.getAmountType())) {
                feesDifferentialAmount = feesDifferentialAmount.add(d.getDifferential());
            } else {
                feesDifferentialAmount = feesDifferentialAmount.subtract(d.getDifferential());
            }
        }

        BitaCoraDetails disbursementFee = new BitaCoraDetails("Deposito a cuenta " + loan.getId(), depositNetAmount, LOAN_CAUSAL, null, 0,
                "DESEMBOLSOCTA", 1, 1, "Public", master, feesDifferentialAmount.setScale(2, RoundingMode.DOWN));
        master.addBitaCoraDetails(disbursementFee);

        return master;
    }

    /*
     * public BitaCoraMaster createJournalEntriesForCategoryChange(Loan loan, LumaBalanceHData lumaBalanceHData, String
     * oldCategory) {
     *
     * String status = getStatusStringFromLoanNpa(lumaBalanceHData.isStatusVencido()); Date transactionDate = Date
     * .from(DateUtils.getLocalDateOfTenant().minusDays(1).atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant()
     * ); CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(loan.getCurrencyCode()); final
     * BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate); String currencyString =
     * currencyData.getIntCode().toString(); String groupString = lumaBalanceHData.getLoanPurpose().toString(); String
     * typeOfCredit = lumaBalanceHData.getLoanType().toString(); String loanCategory = oldCategory; Long transactionId =
     * null; final BigDecimal amount = lumaBalanceHData.getSaldoCapital();
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate, BitacoraMasterConstants.TRX_TYPE_CAMBIO_CATEGORIA,
     * transactionId, loan.getId(), BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount, status,
     * groupString, typeOfCredit, loanCategory); master.setOffice(loan.getOffice());
     * master.setExchangeRate(exchangeRate); master.setClientId(loan.getLumaClientId());
     *
     * List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();
     *
     * // CAPITAL final String heading_capital = "Traslado de capital por cambio de categoria"; final BigDecimal
     * amount_capital = lumaBalanceHData.getSaldoCapital(); final Integer causal_capital = LOAN_CAUSAL; final String
     * newValue_capital = loan.getCategory(); final Integer headingCode_capital = null; final String amountType_capital
     * = "CAPITAL"; final Integer destinationAccount_capital = null; final Integer destinationBank_capital = null; final
     * String destinationClientType_capital = null; final BitaCoraMaster master_capital = master; final BigDecimal
     * differential_capital = amount_capital.multiply(exchangeRate); BitaCoraDetails capitalDetails = new
     * BitaCoraDetails(heading_capital, amount_capital, causal_capital, newValue_capital, headingCode_capital,
     * amountType_capital, destinationAccount_capital, destinationBank_capital, destinationClientType_capital,
     * master_capital, differential_capital); details.add(capitalDetails);
     *
     * // INTERESES final String heading_intereses = "Traslado de intereses por cambio de categoria"; final BigDecimal
     * amount_intereses = lumaBalanceHData.getInteresVencido(); final Integer causal_intereses = LOAN_CAUSAL; final
     * String newValue_intereses = loan.getCategory(); final Integer headingCode_intereses = null; final String
     * amountType_intereses = loan.isNpa() ? "INTERESVENCIDO" : "INTERESES"; final Integer destinationAccount_intereses
     * = null; final Integer destinationBank_intereses = null; final String destinationClientType_intereses = null;
     * final BitaCoraMaster master_intereses = master; final BigDecimal differential_intereses =
     * amount_intereses.multiply(exchangeRate); BitaCoraDetails interestDetails = new BitaCoraDetails(heading_intereses,
     * amount_intereses, causal_intereses, newValue_intereses, headingCode_intereses, amountType_intereses,
     * destinationAccount_intereses, destinationBank_intereses, destinationClientType_intereses, master_intereses,
     * differential_intereses); if (interestDetails.isGreaterThanZero()) { details.add(interestDetails); }
     *
     * // MORA final String heading_mora = "Traslado de mora por cambio de categoria"; final BigDecimal amount_mora =
     * lumaBalanceHData.getMoraVencida(); final Integer causal_mora = LOAN_CAUSAL; final String newValue_mora =
     * loan.getCategory(); final Integer headingCode_mora = null; final String amountType_mora = loan.isNpa() ?
     * "MORAVENCIDA" : "MORA"; final Integer destinationAccount_mora = null; final Integer destinationBank_mora = null;
     * final String destinationClientType_mora = null; final BitaCoraMaster master_mora = master; final BigDecimal
     * differential_mora = amount_mora.multiply(exchangeRate); BitaCoraDetails delinquencyDetails = new
     * BitaCoraDetails(heading_mora, amount_mora, causal_mora, newValue_mora, headingCode_mora, amountType_mora,
     * destinationAccount_mora, destinationBank_mora, destinationClientType_mora, master_mora, differential_mora); if
     * (delinquencyDetails.isGreaterThanZero()) { details.add(delinquencyDetails); }
     *
     * // PENALIDAD final String heading_penalidad = "Traslado de penalidad por cambio de categoria"; final BigDecimal
     * amount_penalidad = lumaBalanceHData.getPenalidadVencida(); final Integer causal_penalidad = LOAN_CAUSAL; final
     * String newValue_penalidad = loan.getCategory(); final Integer headingCode_penalidad = null; final String
     * amountType_penalidad = loan.isNpa() ? "PENALIDADVENCIDA" : "PENALIDAD"; final Integer
     * destinationAccount_penalidad = null; final Integer destinationBank_penalidad = null; final String
     * destinationClientType_penalidad = null; final BitaCoraMaster master_penalidad = master; final BigDecimal
     * differential_penalidad = amount_penalidad.multiply(exchangeRate); BitaCoraDetails penaltiesDetail = new
     * BitaCoraDetails(heading_penalidad, amount_penalidad, causal_penalidad, newValue_penalidad, headingCode_penalidad,
     * amountType_penalidad, destinationAccount_penalidad, destinationBank_penalidad, destinationClientType_penalidad,
     * master_penalidad, differential_penalidad); if (penaltiesDetail.isGreaterThanZero()) {
     * details.add(penaltiesDetail); }
     *
     * master.setBitaCoraDetails(details); return master; }
     *
     * public BitaCoraMaster createJournalEntriesForMoraSobreIntereses(Loan loan, BigDecimal amount_mora) { String
     * status = getStatusStringFromLoanNpa(loan.isNpa()); // LS-89: no deben registrarse con le fecha del día sino con
     * la fecha que se esta cerrando. final Date transactionDate =
     * Date.from(DateUtils.getDateOfTenant().toInstant().minus(Duration.ofDays(1))); CurrencyData currencyData =
     * this.currencyReadPlatformService.retrieveCurrency(loan.getCurrencyCode()); final BigDecimal exchangeRate =
     * getExchangeRate(currencyData.code(), transactionDate); String currencyString =
     * currencyData.getIntCode().toString(); String groupString = getGroupStringFromLoan(loan); String typeOfCredit =
     * getTypeOfLoanString(loan); String loanCategory = loan.getCategory(); Long transactionId = null; final BigDecimal
     * amount = loan.getSummary().getTotalPrincipalOutstanding();
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate, BitacoraMasterConstants.TRX_TYPE_CALCULO_MORA,
     * transactionId, loan.getId(), BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount, status,
     * groupString, typeOfCredit, loanCategory); master.setOffice(loan.getOffice());
     * master.setExchangeRate(exchangeRate); master.setClientId(loan.getLumaClientId());
     *
     * List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();
     *
     * // MORA final String heading_mora = "Cálculo diario de mora"; final Integer causal_mora = LOAN_CAUSAL; final
     * String newValue_mora = null; final Integer headingCode_mora = null; final String amountType_mora = loan.isNpa() ?
     * "MORAVENCIDA" : "MORA"; final Integer destinationAccount_mora = null; final Integer destinationBank_mora = null;
     * final String destinationClientType_mora = null; final BitaCoraMaster master_mora = master; final BigDecimal
     * differential_mora = amount_mora.multiply(exchangeRate); BitaCoraDetails details_mora = new
     * BitaCoraDetails(heading_mora, amount_mora, causal_mora, newValue_mora, headingCode_mora, amountType_mora,
     * destinationAccount_mora, destinationBank_mora, destinationClientType_mora, master_mora, differential_mora);
     * details.add(details_mora);
     *
     * master.setBitaCoraDetails(details); return master; }
     *
     * public BitaCoraMaster createJournalEntriesForPenalizacionNoPago(Loan loan, BigDecimal amount_penalidad) { String
     * status = getStatusStringFromLoanNpa(loan.isNpa()); // LS-89: no deben registrarse con le fecha del día sino con
     * la fecha que se esta cerrando. final Date transactionDate =
     * Date.from(DateUtils.getDateOfTenant().toInstant().minus(Duration.ofDays(1))); CurrencyData currencyData =
     * this.currencyReadPlatformService.retrieveCurrency(loan.getCurrencyCode()); final BigDecimal exchangeRate =
     * getExchangeRate(currencyData.code(), transactionDate); String currencyString =
     * currencyData.getIntCode().toString(); String groupString = getGroupStringFromLoan(loan); String typeOfCredit =
     * getTypeOfLoanString(loan); String loanCategory = loan.getCategory(); Long transactionId = null; final BigDecimal
     * amount = loan.getSummary().getTotalPrincipalOutstanding();
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate, BitacoraMasterConstants.TRX_TYPE_CALCULO_PENALIDAD,
     * transactionId, loan.getId(), BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount, status,
     * groupString, typeOfCredit, loanCategory); master.setOffice(loan.getOffice());
     * master.setExchangeRate(exchangeRate); master.setClientId(loan.getLumaClientId());
     *
     * List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();
     *
     * // PENALIDAD final String heading_penalidad = "Cálculo diario de penalidad "; final Integer causal_penalidad =
     * LOAN_CAUSAL; final String newValue_penalidad = null; final Integer headingCode_penalidad = null; final String
     * amountType_penalidad = loan.isNpa() ? "PENALIDADVENCIDA" : "PENALIDAD"; final Integer
     * destinationAccount_penalidad = null; final Integer destinationBank_penalidad = null; final String
     * destinationClientType_penalidad = null; final BitaCoraMaster master_penalidad = master; final BigDecimal
     * differential_penalidad = amount_penalidad.multiply(exchangeRate); BitaCoraDetails details_penalidad = new
     * BitaCoraDetails(heading_penalidad + loan.getId(), amount_penalidad, causal_penalidad, newValue_penalidad,
     * headingCode_penalidad, amountType_penalidad, destinationAccount_penalidad, destinationBank_penalidad,
     * destinationClientType_penalidad, master_penalidad, differential_penalidad); details.add(details_penalidad);
     *
     * master.setBitaCoraDetails(details); return master; }
     *
     * public BitaCoraMaster createJournalEntriesForOverdueFees(Loan loan, BigDecimal amount_penalidad) { String status
     * = getStatusStringFromLoanNpa(loan.isNpa()); // LS-89: no deben registrarse con le fecha del día sino con la fecha
     * que se esta cerrando. final Date transactionDate =
     * Date.from(DateUtils.getDateOfTenant().toInstant().minus(Duration.ofDays(1))); CurrencyData currencyData =
     * this.currencyReadPlatformService.retrieveCurrency(loan.getCurrencyCode()); final BigDecimal exchangeRate =
     * getExchangeRate(currencyData.code(), transactionDate); String currencyString =
     * currencyData.getIntCode().toString(); String groupString = getGroupStringFromLoan(loan); String typeOfCredit =
     * getTypeOfLoanString(loan); String loanCategory = loan.getCategory(); Long transactionId = null; final BigDecimal
     * amount = loan.getSummary().getTotalPrincipalOutstanding();
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate, BitacoraMasterConstants.TRX_TYPE_CALCULO_INTERESES,
     * transactionId, loan.getId(), BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount, status,
     * groupString, typeOfCredit, loanCategory); master.setOffice(loan.getOffice());
     * master.setExchangeRate(exchangeRate); master.setClientId(loan.getLumaClientId());
     *
     * List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();
     *
     * // PENALIDAD final String heading_penalidad = "Cálculo diario de Interés Adicional "; final Integer
     * causal_penalidad = LOAN_CAUSAL; final String newValue_penalidad = null; final Integer headingCode_penalidad =
     * null; final String amountType_penalidad = loan.isNpa() ? "INTERESADICIONALVENCIDO" : "INTERESADICIONAL"; final
     * Integer destinationAccount_penalidad = null; final Integer destinationBank_penalidad = null; final String
     * destinationClientType_penalidad = null; final BitaCoraMaster master_penalidad = master; final BigDecimal
     * differential_penalidad = amount_penalidad.multiply(exchangeRate); BitaCoraDetails details_penalidad = new
     * BitaCoraDetails(heading_penalidad + loan.getId(), amount_penalidad, causal_penalidad, newValue_penalidad,
     * headingCode_penalidad, amountType_penalidad, destinationAccount_penalidad, destinationBank_penalidad,
     * destinationClientType_penalidad, master_penalidad, differential_penalidad); details.add(details_penalidad);
     *
     * master.setBitaCoraDetails(details); return master; }
     */

    public BitaCoraMaster createJournalEntry(LumaBitacoraTransactionTypeEnum lumaBitacoraTransactionTypeEnum, Loan loan,
            LocalDate transactionLocalDate, BigDecimal amount) {
        String status = getStatusStringFromLoanNpa(loan.isNpa());
        final Date transactionDate = Date.from(transactionLocalDate.atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
        String transactionType = lumaBitacoraTransactionTypeEnum.getCode();

        // transactionType for "MULTITRANCHES"
        if (transactionType.equals(BitacoraMasterConstants.TRX_TYPE_APROBACION)) {
            transactionType = loan.getDisbursementDetails().size() == 0 ? BitacoraMasterConstants.TRX_TYPE_APROBACION
                    : BitacoraMasterConstants.TRX_TYPE_APROBACION_MULTITRANCHES;
        }

        // transactionType for "CUPO"
        if (loan.getAccountAssociations() != null && loan.getAccountAssociations().linkedCupo() != null) {
            switch (transactionType) {
                case BitacoraMasterConstants.TRX_TYPE_APROBACION:
                    transactionType = BitacoraMasterConstants.TRX_TYPE_APROBACION_CUPO;
                break;
                case BitacoraMasterConstants.TRX_TYPE_FORMALIZACION:
                    transactionType = BitacoraMasterConstants.TRX_TYPE_FORMALIZACION_CUPO;
                break;
                default:
                break;
            }
        }
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(loan.getCurrencyCode());
        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        String currencyString = currencyData.getIntCode().toString();
        String groupString = getGroupStringFromLoan(loan);
        String typeOfCredit = getTypeOfLoanString(loan);
        Long transactionId = null;

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, transactionType, transactionId, loan.getId(),
                lumaBitacoraTransactionTypeEnum.getAccountType(), currencyString, amount, status, groupString, typeOfCredit);
        master.setOffice(loan.getOffice());
        master.setExchangeRate(exchangeRate);
        master.setClientId(loan.getLumaClientId());
        master.setObservations(lumaBitacoraTransactionTypeEnum.getDescription() + loan.getAccountNumber());

        BigDecimal amountDifferential = amount.multiply(exchangeRate);
        BitaCoraDetails masterDetails = new BitaCoraDetails(lumaBitacoraTransactionTypeEnum.getHeading() + loan.getId(), amount,
                LOAN_CAUSAL, null, null, lumaBitacoraTransactionTypeEnum.getAmountType(), null, null, null, master, amountDifferential);
        master.addBitaCoraDetails(masterDetails);

        if (master.isGreaterThanZero()) {
            return master;
        }
        return null;
    }

    /*
     * public List<BitaCoraMaster> createJournalEntriesForLoanGuarantees(Loan loan) { List<BitaCoraMaster> retMasterList
     * = new ArrayList<>();
     *
     * List<ExternalGuaranteeLoan> guaranteeList = this.externalGuaranteeLoanRepository.getAllByLoanId(loan.getId()); if
     * (guaranteeList == null || guaranteeList.isEmpty()) return null;
     *
     * for (ExternalGuaranteeLoan guaranteeLoan : guaranteeList) {
     *
     * // Create master record for guarantee String status = getStatusStringFromLoanNpa(loan.isNpa()); final Date
     * transactionDate = Date
     * .from(loan.getDisbursementDate().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant()); CurrencyData
     * currencyData = this.currencyReadPlatformService.retrieveCurrency(loan.getCurrencyCode()); final BigDecimal
     * exchangeRate = getExchangeRate(currencyData.code(), transactionDate); String currencyString =
     * currencyData.getIntCode().toString(); String groupString = getGroupStringFromLoan(loan); String typeOfCredit =
     * getTypeOfLoanString(loan); String loanCategory = loan.getCategory(); Long transactionId = null; final BigDecimal
     * amount = guaranteeLoan.getGuarantee().getTotalAmount();
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate, BitacoraMasterConstants.TRX_TYPE_GARANTIA,
     * transactionId, loan.getId(), BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount, status,
     * groupString, typeOfCredit, loanCategory); master.setOffice(loan.getOffice());
     * master.setExchangeRate(exchangeRate); master.setClientId(loan.getLumaClientId());
     *
     * List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();
     *
     * // Create the detail for the Guarantee final String heading = "Garantia " + loan.getId(); final Integer causal =
     * LOAN_CAUSAL; final String newValue = String.valueOf(guaranteeLoan.getGuarantee().getId()); final Integer
     * headingCode = null; final String amountType = "MONTOGARANTIA"; final Integer destinationAccount = null; final
     * Integer destinationBank = null; final String destinationClientType = null; final BigDecimal amountGuaranteeType =
     * guaranteeLoan.getGuarantee().getTotalAmount(); final BigDecimal differential =
     * amountGuaranteeType.multiply(exchangeRate);
     *
     * BitaCoraDetails collateralDetails = new BitaCoraDetails(heading, amountGuaranteeType, causal, newValue,
     * headingCode, amountType, destinationAccount, destinationBank, destinationClientType, master, differential);
     * details.add(collateralDetails);
     *
     * master.setBitaCoraDetails(details);
     *
     * retMasterList.add(master); }
     *
     * return retMasterList; }
     *
     * public BitaCoraMaster createJournalEntriesForNPAchange(Boolean isForResetting, Long loanId, LumaBalanceHData
     * lumaBalanceHData) { if (isForResetting) {
     *
     * final String status = "VEN"; Date transactionDate = Date
     * .from(DateUtils.getLocalDateOfTenant().minusDays(1).atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant()
     * ); CurrencyData currencyData =
     * this.currencyReadPlatformService.retrieveCurrency(lumaBalanceHData.getCurrencyCode()); final BigDecimal
     * exchangeRate = getExchangeRate(currencyData.code(), transactionDate); String currencyString =
     * currencyData.getIntCode().toString(); String groupString = lumaBalanceHData.getLoanPurpose().toString(); String
     * typeOfCredit = lumaBalanceHData.getLoanType().toString(); String loanCategory = lumaBalanceHData.getCategory();
     * final BigDecimal amount = lumaBalanceHData.getSaldoCapital(); Long transactionId = null;
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate,
     * BitacoraMasterConstants.TRX_TYPE_CAMBIO_STATUS_AVIGENTE, transactionId, loanId,
     * BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount, status, groupString, typeOfCredit,
     * loanCategory); master.setOfficeId(lumaBalanceHData.getOfficeId()); master.setExchangeRate(exchangeRate);
     * master.setClientId(lumaBalanceHData.getClientId());
     *
     * List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();
     *
     * final String heading_capital = "Traslado de capital por cambio de Estado a VIG"; final BigDecimal amount_capital
     * = lumaBalanceHData.getSaldoCapital(); final Integer causal_capital = LOAN_CAUSAL; final String newValue_capital =
     * "VIG"; final Integer headingCode_capital = null; final String amountType_capital = "CAPITAL"; final Integer
     * destinationAccount_capital = null; final Integer destinationBank_capital = null; final String
     * destinationClientType_capital = null; final BitaCoraMaster master_capital = master; final BigDecimal
     * differential_capital = amount_capital.multiply(exchangeRate); BitaCoraDetails capitalDetails = new
     * BitaCoraDetails(heading_capital, amount_capital, causal_capital, newValue_capital, headingCode_capital,
     * amountType_capital, destinationAccount_capital, destinationBank_capital, destinationClientType_capital,
     * master_capital, differential_capital); details.add(capitalDetails);
     *
     * final String heading_intereses = "Traslado de intereses por cambio de Estado a VIG"; final BigDecimal
     * amount_intereses = lumaBalanceHData.getInteresVencido(); final Integer causal_intereses = LOAN_CAUSAL; final
     * String newValue_intereses = "VIG"; final Integer headingCode_intereses = null; final String amountType_intereses
     * = "INTERESES"; final Integer destinationAccount_intereses = null; final Integer destinationBank_intereses = null;
     * final String destinationClientType_intereses = null; final BitaCoraMaster master_intereses = master; final
     * BigDecimal differential_intereses = amount_intereses.multiply(exchangeRate); BitaCoraDetails interestDetails =
     * new BitaCoraDetails(heading_intereses, amount_intereses, causal_intereses, newValue_intereses,
     * headingCode_intereses, amountType_intereses, destinationAccount_intereses, destinationBank_intereses,
     * destinationClientType_intereses, master_intereses, differential_intereses); if
     * (interestDetails.isGreaterThanZero()) { details.add(interestDetails); }
     *
     * final String heading_mora = "Traslado de mora por cambio de Estado a VIG"; final BigDecimal amount_mora =
     * lumaBalanceHData.getMoraVencida(); final Integer causal_mora = LOAN_CAUSAL; final String newValue_mora = "VIG";
     * final Integer headingCode_mora = null; final String amountType_mora = "MORA"; final Integer
     * destinationAccount_mora = null; final Integer destinationBank_mora = null; final String
     * destinationClientType_mora = null; final BitaCoraMaster master_mora = master; final BigDecimal differential_mora
     * = amount_mora.multiply(exchangeRate); BitaCoraDetails delinquencyDetails = new BitaCoraDetails(heading_mora,
     * amount_mora, causal_mora, newValue_mora, headingCode_mora, amountType_mora, destinationAccount_mora,
     * destinationBank_mora, destinationClientType_mora, master_mora, differential_mora); if
     * (delinquencyDetails.isGreaterThanZero()) { details.add(delinquencyDetails); }
     *
     * final String heading_penalidad = "Traslado de penalidad por cambio de Estado a VIG"; final BigDecimal
     * amount_penalidad = lumaBalanceHData.getPenalidadVencida(); final Integer causal_penalidad = LOAN_CAUSAL; final
     * String newValue_penalidad = "VIG"; final Integer headingCode_penalidad = null; final String amountType_penalidad
     * = "PENALIDAD"; final Integer destinationAccount_penalidad = null; final Integer destinationBank_penalidad = null;
     * final String destinationClientType_penalidad = null; final BitaCoraMaster master_penalidad = master; final
     * BigDecimal differential_penalidad = amount_penalidad.multiply(exchangeRate); BitaCoraDetails penaltiesDetail =
     * new BitaCoraDetails(heading_penalidad, amount_penalidad, causal_penalidad, newValue_penalidad,
     * headingCode_penalidad, amountType_penalidad, destinationAccount_penalidad, destinationBank_penalidad,
     * destinationClientType_penalidad, master_penalidad, differential_penalidad); if
     * (penaltiesDetail.isGreaterThanZero()) { details.add(penaltiesDetail); }
     *
     * master.setBitaCoraDetails(details); return master; } else { final String status = "VIG"; Date transactionDate =
     * Date
     * .from(DateUtils.getLocalDateOfTenant().minusDays(1).atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant()
     * ); CurrencyData currencyData =
     * this.currencyReadPlatformService.retrieveCurrency(lumaBalanceHData.getCurrencyCode()); final BigDecimal
     * exchangeRate = getExchangeRate(currencyData.code(), transactionDate); String currencyString =
     * currencyData.getIntCode().toString(); String groupString = lumaBalanceHData.getLoanPurpose().toString(); String
     * typeOfCredit = lumaBalanceHData.getLoanType().toString(); String loanCategory = lumaBalanceHData.getCategory();
     * final BigDecimal amount = lumaBalanceHData.getSaldoCapital(); Long transactionId = null;
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate,
     * BitacoraMasterConstants.TRX_TYPE_CAMBIO_STATUS_AVENCIDO, transactionId, loanId,
     * BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount, status, groupString, typeOfCredit,
     * loanCategory); master.setOfficeId(lumaBalanceHData.getOfficeId()); master.setExchangeRate(exchangeRate);
     * master.setClientId(lumaBalanceHData.getClientId());
     *
     * List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();
     *
     * final String heading_capital = "Traslado de capital por cambio de Estado a VEN"; final BigDecimal amount_capital
     * = lumaBalanceHData.getSaldoCapital(); final Integer causal_capital = LOAN_CAUSAL; final String newValue_capital =
     * "VEN"; final Integer headingCode_capital = null; final String amountType_capital = "CAPITAL"; final Integer
     * destinationAccount_capital = null; final Integer destinationBank_capital = null; final String
     * destinationClientType_capital = null; final BitaCoraMaster master_capital = master; final BigDecimal
     * differential_capital = amount_capital.multiply(exchangeRate); BitaCoraDetails principalOverDue = new
     * BitaCoraDetails(heading_capital, amount_capital, causal_capital, newValue_capital, headingCode_capital,
     * amountType_capital, destinationAccount_capital, destinationBank_capital, destinationClientType_capital,
     * master_capital, differential_capital); details.add(principalOverDue);
     *
     * final String heading_intereses = "Traslado de intereses por cambio de Estado a VEN"; final BigDecimal
     * amount_intereses = lumaBalanceHData.getInteresVencido(); final Integer causal_intereses = LOAN_CAUSAL; final
     * String newValue_intereses = "VEN"; final Integer headingCode_intereses = null; final String amountType_intereses
     * = "INTERESES"; final Integer destinationAccount_intereses = null; final Integer destinationBank_intereses = null;
     * final String destinationClientType_intereses = null; final BitaCoraMaster master_intereses = master; final
     * BigDecimal differential_intereses = amount_intereses.multiply(exchangeRate); BitaCoraDetails interestOverDue =
     * new BitaCoraDetails(heading_intereses, amount_intereses, causal_intereses, newValue_intereses,
     * headingCode_intereses, amountType_intereses, destinationAccount_intereses, destinationBank_intereses,
     * destinationClientType_intereses, master_intereses, differential_intereses); if
     * (interestOverDue.isGreaterThanZero()) { details.add(interestOverDue); }
     *
     * final String heading_mora = "Traslado de mora por cambio de Estado a VEN"; final BigDecimal amount_mora =
     * lumaBalanceHData.getMoraVencida(); final Integer causal_mora = LOAN_CAUSAL; final String newValue_mora = "VEN";
     * final Integer headingCode_mora = null; final String amountType_mora = "MORA"; final Integer
     * destinationAccount_mora = null; final Integer destinationBank_mora = null; final String
     * destinationClientType_mora = null; final BitaCoraMaster master_mora = master; final BigDecimal differential_mora
     * = amount_mora.multiply(exchangeRate); BitaCoraDetails moraCharges = new BitaCoraDetails(heading_mora,
     * amount_mora, causal_mora, newValue_mora, headingCode_mora, amountType_mora, destinationAccount_mora,
     * destinationBank_mora, destinationClientType_mora, master_mora, differential_mora); if
     * (moraCharges.isGreaterThanZero()) { details.add(moraCharges); }
     *
     * final String heading_penalidad = "Traslado de penalidad por cambio de Estado a VEN"; final BigDecimal
     * amount_penalidad = lumaBalanceHData.getPenalidadVencida(); final Integer causal_penalidad = LOAN_CAUSAL; final
     * String newValue_penalidad = "VEN"; final Integer headingCode_penalidad = null; final String amountType_penalidad
     * = "PENALIDAD"; final Integer destinationAccount_penalidad = null; final Integer destinationBank_penalidad = null;
     * final String destinationClientType_penalidad = null; final BitaCoraMaster master_penalidad = master; final
     * BigDecimal differential_penalidad = amount_penalidad.multiply(exchangeRate); BitaCoraDetails penaltiesDetail =
     * new BitaCoraDetails(heading_penalidad, amount_penalidad, causal_penalidad, newValue_penalidad,
     * headingCode_penalidad, amountType_penalidad, destinationAccount_penalidad, destinationBank_penalidad,
     * destinationClientType_penalidad, master_penalidad, differential_penalidad); if
     * (penaltiesDetail.isGreaterThanZero()) { details.add(penaltiesDetail); }
     *
     * master.setBitaCoraDetails(details); return master; } }
     *
     * public BitaCoraMaster createJournalEntriesForCambioAgrupacion(LumaBalanceHData lumaBalanceHData, String
     * oldGroupString, String newGroupString) { final String status = lumaBalanceHData.getStringEstado(); Date
     * transactionDate = Date
     * .from(DateUtils.getLocalDateOfTenant().minusDays(1).atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant()
     * ); CurrencyData currencyData =
     * this.currencyReadPlatformService.retrieveCurrency(lumaBalanceHData.getCurrencyCode()); final BigDecimal
     * exchangeRate = getExchangeRate(currencyData.code(), transactionDate); String currencyString =
     * currencyData.getIntCode().toString(); String groupString = oldGroupString; String typeOfCredit =
     * lumaBalanceHData.getLoanType().toString(); String loanCategory = lumaBalanceHData.getCategory(); final BigDecimal
     * amount = lumaBalanceHData.getSaldoCapital(); Long transactionId = null;
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate, BitacoraMasterConstants.TRX_TYPE_CAMBIOAGRUPACION,
     * transactionId, lumaBalanceHData.getLoanId(), BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount,
     * status, groupString, typeOfCredit, loanCategory); master.setOfficeId(lumaBalanceHData.getOfficeId());
     * master.setExchangeRate(exchangeRate); master.setClientId(lumaBalanceHData.getClientId());
     *
     * List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();
     *
     * final String heading_capital = "Traslado de capital por cambio de agrupación"; final BigDecimal amount_capital =
     * lumaBalanceHData.getSaldoCapital(); final String newValue_capital = newGroupString; // new value
     * "empresarial mayor" final Integer headingCode_capital = null; final String amountType_capital = "CAPITAL"; final
     * Integer destinationAccount_capital = null; final Integer destinationBank_capital = null; final String
     * destinationClientType_capital = null; final BitaCoraMaster master_capital = master; final BigDecimal
     * differential_capital = amount_capital.multiply(exchangeRate); BitaCoraDetails capitalDetails = new
     * BitaCoraDetails(heading_capital, amount_capital, 999, newValue_capital, headingCode_capital, amountType_capital,
     * destinationAccount_capital, destinationBank_capital, destinationClientType_capital, master_capital,
     * differential_capital); details.add(capitalDetails);
     *
     * final String heading_intereses = "Traslado de intereses por cambio de agrupación"; final BigDecimal
     * amount_intereses = lumaBalanceHData.getInteresVencido(); final String newValue_intereses = newGroupString; // new
     * value "empresarial mayor" final Integer headingCode_intereses = null; final String amountType_intereses =
     * lumaBalanceHData.isStatusVencido() ? "INTERESVENCIDO" : "INTERESES"; final Integer destinationAccount_intereses =
     * null; final Integer destinationBank_intereses = null; final String destinationClientType_intereses = null; final
     * BitaCoraMaster master_intereses = master; final BigDecimal differential_intereses =
     * amount_intereses.multiply(exchangeRate); BitaCoraDetails interestDetails = new BitaCoraDetails(heading_intereses,
     * amount_intereses, 999, newValue_intereses, headingCode_intereses, amountType_intereses,
     * destinationAccount_intereses, destinationBank_intereses, destinationClientType_intereses, master_intereses,
     * differential_intereses); if (interestDetails.isGreaterThanZero()) { details.add(interestDetails); }
     *
     * final String heading_mora = "Traslado de mora por cambio de agrupación"; final BigDecimal amount_mora =
     * lumaBalanceHData.getMoraVencida(); final String newValue_mora = newGroupString; // new value "empresarial mayor"
     * final Integer headingCode_mora = null; final String amountType_mora = lumaBalanceHData.isStatusVencido() ?
     * "MORAVENCIDA" : "MORA"; final Integer destinationAccount_mora = null; final Integer destinationBank_mora = null;
     * final String destinationClientType_mora = null; final BitaCoraMaster master_mora = master; final BigDecimal
     * differential_mora = amount_mora.multiply(exchangeRate); BitaCoraDetails delinquencyDetails = new
     * BitaCoraDetails(heading_mora, amount_mora, 999, newValue_mora, headingCode_mora, amountType_mora,
     * destinationAccount_mora, destinationBank_mora, destinationClientType_mora, master_mora, differential_mora); if
     * (delinquencyDetails.isGreaterThanZero()) { details.add(delinquencyDetails); }
     *
     * final String heading_penalidad = "Traslado de penalidad por cambio de agrupación"; final BigDecimal
     * amount_penalidad = lumaBalanceHData.getPenalidadVencida(); final String newValue_penalidad = newGroupString; //
     * new value "empresarial mayor" final Integer headingCode_penalidad = null; final String amountType_penalidad =
     * lumaBalanceHData.isStatusVencido() ? "PENALIDADVENCIDA" : "PENALIDAD"; final Integer destinationAccount_penalidad
     * = null; final Integer destinationBank_penalidad = null; final String destinationClientType_penalidad = null;
     * final BitaCoraMaster master_penalidad = master; final BigDecimal differential_penalidad =
     * amount_penalidad.multiply(exchangeRate); BitaCoraDetails penaltiesDetail = new BitaCoraDetails(heading_penalidad,
     * amount_penalidad, 999, newValue_penalidad, headingCode_penalidad, amountType_penalidad,
     * destinationAccount_penalidad, destinationBank_penalidad, destinationClientType_penalidad, master_penalidad,
     * differential_penalidad); if (delinquencyDetails.isGreaterThanZero()) { details.add(penaltiesDetail); }
     * master.setBitaCoraDetails(details); return master; }
     *
     * public void reverseLogForTransactionId(Long loanTransactionId) {
     * this.reverseLogForTransactionId(loanTransactionId, BitacoraMasterConstants.ACCOUNT_TYPE_PR); }
     *
     * public void reverseLastLogForLoanAndTrxType(Long loanId, String transactionType) { Optional<BitaCoraMaster>
     * formalizeLog = this.findLastMasterLogForAccountIdAndTypeandTrxType(loanId,
     * BitacoraMasterConstants.ACCOUNT_TYPE_PR, transactionType); if (formalizeLog.isPresent()) {
     * this.reverseMasterLogAndDetails(formalizeLog.get()); } }
     *
     * public BitaCoraMaster createJudcialCollectionLog(Long loanId, LumaBalanceHData lumaBalanceHData) { final String
     * status = "JUD"; Date transactionDate =
     * Date.from(DateUtils.getLocalDateOfTenant().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
     * CurrencyData currencyData =
     * this.currencyReadPlatformService.retrieveCurrency(lumaBalanceHData.getCurrencyCode()); final BigDecimal
     * exchangeRate = getExchangeRate(currencyData.code(), transactionDate); String currencyString =
     * currencyData.getIntCode().toString(); String groupString = lumaBalanceHData.getLoanPurpose().toString(); String
     * typeOfCredit = lumaBalanceHData.getLoanType().toString(); String loanCategory = lumaBalanceHData.getCategory();
     * final BigDecimal amount = lumaBalanceHData.getSaldoCapital(); Long transactionId = null;
     *
     * BitaCoraMaster master = new BitaCoraMaster(transactionDate,
     * BitacoraMasterConstants.TRX_TYPE_CAMBIO_STATUS_JURIDICO, transactionId, loanId,
     * BitacoraMasterConstants.ACCOUNT_TYPE_PR, currencyString, amount, status, groupString, typeOfCredit,
     * loanCategory); master.setOfficeId(lumaBalanceHData.getOfficeId()); master.setExchangeRate(exchangeRate);
     * master.setClientId(lumaBalanceHData.getClientId());
     *
     * List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();
     *
     * final String heading_capital = "Traslado de capital por cambio de Estado a JUD"; final BigDecimal amount_capital
     * = lumaBalanceHData.getSaldoCapital(); final Integer causal_capital = LOAN_CAUSAL; final String newValue_capital =
     * "JUD"; final Integer headingCode_capital = null; final String amountType_capital = "CAPITAL"; final Integer
     * destinationAccount_capital = null; final Integer destinationBank_capital = null; final String
     * destinationClientType_capital = null; final BitaCoraMaster master_capital = master; final BigDecimal
     * differential_capital = amount_capital.multiply(exchangeRate); BitaCoraDetails principalOverDue = new
     * BitaCoraDetails(heading_capital, amount_capital, causal_capital, newValue_capital, headingCode_capital,
     * amountType_capital, destinationAccount_capital, destinationBank_capital, destinationClientType_capital,
     * master_capital, differential_capital); details.add(principalOverDue);
     *
     * final String heading_intereses = "Traslado de intereses por cambio de Estado a JUD"; final BigDecimal
     * amount_intereses = lumaBalanceHData.getInteresVencido(); final Integer causal_intereses = LOAN_CAUSAL; final
     * String newValue_intereses = "JUD"; final Integer headingCode_intereses = null; final String amountType_intereses
     * = "INTERESES"; final Integer destinationAccount_intereses = null; final Integer destinationBank_intereses = null;
     * final String destinationClientType_intereses = null; final BitaCoraMaster master_intereses = master; final
     * BigDecimal differential_intereses = amount_intereses.multiply(exchangeRate); BitaCoraDetails interestOverDue =
     * new BitaCoraDetails(heading_intereses, amount_intereses, causal_intereses, newValue_intereses,
     * headingCode_intereses, amountType_intereses, destinationAccount_intereses, destinationBank_intereses,
     * destinationClientType_intereses, master_intereses, differential_intereses); if
     * (interestOverDue.isGreaterThanZero()) { details.add(interestOverDue); }
     *
     * final String heading_mora = "Traslado de MORA por cambio de Estado a JUD"; final BigDecimal amount_mora =
     * lumaBalanceHData.getMoraVencida(); final Integer causal_mora = LOAN_CAUSAL; final String newValue_mora = "JUD";
     * final Integer headingCode_mora = null; final String amountType_mora = "MORA"; final Integer
     * destinationAccount_mora = null; final Integer destinationBank_mora = null; final String
     * destinationClientType_mora = null; final BitaCoraMaster master_mora = master; final BigDecimal differential_mora
     * = amount_mora.multiply(exchangeRate); BitaCoraDetails moraCharges = new BitaCoraDetails(heading_mora,
     * amount_mora, causal_mora, newValue_mora, headingCode_mora, amountType_mora, destinationAccount_mora,
     * destinationBank_mora, destinationClientType_mora, master_mora, differential_mora); if
     * (moraCharges.isGreaterThanZero()) { details.add(moraCharges); }
     *
     * final String heading_penalidad = "Traslado de PENALIDAD por cambio de Estado a JUD"; final BigDecimal
     * amount_penalidad = lumaBalanceHData.getPenalidadVencida(); final Integer causal_penalidad = LOAN_CAUSAL; final
     * String newValue_penalidad = "JUD"; final Integer headingCode_penalidad = null; final String amountType_penalidad
     * = "PENALIDAD"; final Integer destinationAccount_penalidad = null; final Integer destinationBank_penalidad = null;
     * final String destinationClientType_penalidad = null; final BitaCoraMaster master_penalidad = master; final
     * BigDecimal differential_penalidad = amount_penalidad.multiply(exchangeRate); BitaCoraDetails penaltiesDetail =
     * new BitaCoraDetails(heading_penalidad, amount_penalidad, causal_penalidad, newValue_penalidad,
     * headingCode_penalidad, amountType_penalidad, destinationAccount_penalidad, destinationBank_penalidad,
     * destinationClientType_penalidad, master_penalidad, differential_penalidad); if
     * (penaltiesDetail.isGreaterThanZero()) { details.add(penaltiesDetail); } master.setBitaCoraDetails(details);
     * return master;
     *
     * }
     *
     */
    public BitaCoraMaster createJournalEntryForCupoApprove(Cupo cupo) {
        Date transactionDate = DateUtils.getDateOfTenant();
        String transactionType = "CREACIONCUPO";
        Long transactionId = null;
        Long accountId = cupo.getId();
        String accountType = BitacoraMasterConstants.ACCOUNT_TYPE_PR;
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(cupo.getCurrencyCode());
        String currencyString = currencyData.getIntCode().toString();
        BigDecimal amount = cupo.getAmountApproved();
        String status = "active";
        final BigDecimal exchangeRate = getExchangeRate(cupo.getCurrencyCode(), transactionDate);
        String groupString = "1";
        String typeOfCredit = "1";

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, transactionType, transactionId, accountId, accountType, currencyString,
                amount, status, groupString, typeOfCredit);

        master.setExchangeRate(exchangeRate);
        master.setClientId(cupo.getClientId() != null ? cupo.getClientId() : cupo.getGroupId());

        final String heading = "Creación de Cupo";
        final Integer causal = LOAN_CAUSAL;
        final String newValue = null;
        final Integer headingCode = null;
        final String amountType = "CAPITAL";
        final Integer destinationAccount = null;
        final Integer destinationBank = null;
        final String destinationClientType = null;
        final BigDecimal differential = amount.multiply(exchangeRate);
        BitaCoraDetails details = new BitaCoraDetails(heading, amount, causal, newValue, headingCode, amountType, destinationAccount,
                destinationBank, destinationClientType, master, differential);

        master.addBitaCoraDetails(details);

        return master;
    }

    public BitaCoraMaster createJournalEntryForCupoExtension(Cupo cupo, BigDecimal amount) {
        Date transactionDate = DateUtils.getDateOfTenant();
        String transactionType = "AMPLIACIONCUPO";
        Long transactionId = null;
        Long accountId = cupo.getId();
        String accountType = BitacoraMasterConstants.ACCOUNT_TYPE_PR;
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(cupo.getCurrencyCode());
        String currencyString = currencyData.getIntCode().toString();
        String status = "active";
        final BigDecimal exchangeRate = getExchangeRate(cupo.getCurrencyCode(), transactionDate);
        String groupString = "1";
        String typeOfCredit = "1";

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, transactionType, transactionId, accountId, accountType, currencyString,
                amount, status, groupString, typeOfCredit);

        master.setExchangeRate(exchangeRate);
        master.setClientId(cupo.getClientId() != null ? cupo.getClientId() : cupo.getGroupId());

        final String heading = "Ampliación de Cupo";
        final Integer causal = LOAN_CAUSAL;
        final String newValue = null;
        final Integer headingCode = null;
        final String amountType = "CAPITAL";
        final Integer destinationAccount = null;
        final Integer destinationBank = null;
        final String destinationClientType = null;
        final BigDecimal differential = amount.multiply(exchangeRate);
        BitaCoraDetails details = new BitaCoraDetails(heading, amount, causal, newValue, headingCode, amountType, destinationAccount,
                destinationBank, destinationClientType, master, differential);

        master.addBitaCoraDetails(details);

        return master;
    }

    public BitaCoraMaster createJournalEntryForCupoReduction(Cupo cupo, BigDecimal amount) {
        Date transactionDate = DateUtils.getDateOfTenant();
        String transactionType = "REDUCCIONCUPO";
        Long transactionId = null;
        Long accountId = cupo.getId();
        String accountType = BitacoraMasterConstants.ACCOUNT_TYPE_PR;
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(cupo.getCurrencyCode());
        String currencyString = currencyData.getIntCode().toString();
        String status = "active";
        final BigDecimal exchangeRate = getExchangeRate(cupo.getCurrencyCode(), transactionDate);
        String groupString = "1";
        String typeOfCredit = "1";

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, transactionType, transactionId, accountId, accountType, currencyString,
                amount, status, groupString, typeOfCredit);

        master.setExchangeRate(exchangeRate);
        master.setClientId(cupo.getClientId() != null ? cupo.getClientId() : cupo.getGroupId());

        final String heading = "Reducción de Cupo";
        final Integer causal = LOAN_CAUSAL;
        final String newValue = null;
        final Integer headingCode = null;
        final String amountType = "CAPITAL";
        final Integer destinationAccount = null;
        final Integer destinationBank = null;
        final String destinationClientType = null;
        final BigDecimal differential = amount.multiply(exchangeRate);
        BitaCoraDetails details = new BitaCoraDetails(heading, amount, causal, newValue, headingCode, amountType, destinationAccount,
                destinationBank, destinationClientType, master, differential);

        master.addBitaCoraDetails(details);

        return master;
    }

    public BitaCoraMaster createJournalEntryForCupoCancel(Cupo cupo, BigDecimal amount) {
        Date transactionDate = DateUtils.getDateOfTenant();
        String transactionType = "CANCELACIONCUPO";
        Long transactionId = null;
        Long accountId = cupo.getId();
        String accountType = BitacoraMasterConstants.ACCOUNT_TYPE_PR;
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(cupo.getCurrencyCode());
        String currencyString = currencyData.getIntCode().toString();
        String status = "canceled";
        final BigDecimal exchangeRate = getExchangeRate(cupo.getCurrencyCode(), transactionDate);
        String groupString = "1";
        String typeOfCredit = "1";

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, transactionType, transactionId, accountId, accountType, currencyString,
                amount, status, groupString, typeOfCredit);

        master.setExchangeRate(exchangeRate);
        master.setClientId(cupo.getClientId() != null ? cupo.getClientId() : cupo.getGroupId());

        final String heading = "Cancelación de Cupo";
        final Integer causal = LOAN_CAUSAL;
        final String newValue = null;
        final Integer headingCode = null;
        final String amountType = "CAPITAL";
        final Integer destinationAccount = null;
        final Integer destinationBank = null;
        final String destinationClientType = null;
        final BigDecimal differential = amount.multiply(exchangeRate);
        BitaCoraDetails details = new BitaCoraDetails(heading, amount, causal, newValue, headingCode, amountType, destinationAccount,
                destinationBank, destinationClientType, master, differential);

        master.addBitaCoraDetails(details);

        return master;
    }

}
