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
package org.apache.fineract.portfolio.loanaccount.event;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CustomHookEventProcessorEnum {

    CREDITO_ROTATIVO_FIRST_USE("org.apache.fineract.portfolio.loanaccount.event.LoanCreditoRotativoFirstUseEventProcessor",
            "First use in revolving loan"),

    LOAN_OFFICER_UPDATE("org.apache.fineract.portfolio.loanaccount.event.LoanDisbursementUpdateLoanOfficerCodeEventProcessor",
            "Actualizar Codigo de Promotor"),

    DISBURSEMENT("org.apache.fineract.portfolio.loanaccount.event.LoanDisbursementEventProcessor", "Disbursement Event"),

    REPAYMENT_EVENT("org.apache.fineract.portfolio.loanaccount.event.LoanRepaymentEventProcessor", "Repayment Event"),

    RENEWAL_EVENT("org.apache.fineract.portfolio.loanaccount.event.LoanCloseAsRescheduledEventProcessor", "Renewal Event"),

    RENEWAL_EVENT_FORECLOSURE("org.apache.fineract.portfolio.loanaccount.event.LoanCloseAsRescheduledForeclosureEventProcessor",
            "Renewal Event"),

    LOAN_DISBURSEMENT_APPROVED_AMOUNT_AVAILABLE(
            "org.apache.fineract.portfolio.loanaccount.event.LoanDisbursementApprovedAmountAvailableEventProcessor",
            "New disbursement revolving with balance 0"),

    DISBURSEMENT_GUARANTEE("org.apache.fineract.portfolio.loanaccount.event.LoanDisbursementGuaranteeEventProcessor",
            "Disbursement Guarantee"),

    DISBURSEMENT_CREDITO_ROTATIVO("org.apache.fineract.portfolio.loanaccount.event.LoanDisbursementCreditoRotativoEventProcessor",
            "Aprobación De Crédito Rotativo - Operaciones"),

    DISBURSEMENT_CREDITO_ROTATIVO_UPDATE(
            "org.apache.fineract.portfolio.loanaccount.event.LoanDisbursementCreditoRotativoUpdateEventProcessor",
            "Aprobación De Crédito Rotativo - Operaciones"),

    LOAN_DISBURSEMENT_REMAINING_AMOUNT("org.apache.fineract.portfolio.loanaccount.event.LoanDisbursementRemainingAmountEventProcessor",
            "Revolving amount available"),

    ANULLMENT_EVENT("org.apache.fineract.portfolio.loanaccount.event.LoanRejectionGuaranteeEventProcessor", "Annulment Event"),

    CONTACTABILITY("org.apache.fineract.portfolio.loanaccount.event.LoanApprovalContactabilityEventProcessor", "Contactability"), //

    CONTACTABILITY_UPDATE("org.apache.fineract.portfolio.loanaccount.event.LoanApprovalContactabilityUpdateEventProcessor",
            "Contactability"),

    LOAN_DISBURSEMENT_REPORT_EVENT("org.apache.fineract.portfolio.loanaccount.event.LoanDisbursementReportEventProcessor",
            "Disbursement report event"),

    LOAN_DISBURSEMENT_REVERSAL_EVENT("org.apache.fineract.portfolio.loanaccount.event.LoanDisbursementReversalEventProcessor",
            "LOAN_DISBURSEMENT_REVERSAL"),

    LOAN_DISBURSEMENT_REVERSAL_UNDO_EVENT("org.apache.fineract.portfolio.loanaccount.event.LoanDisbursementReversalUndoEventProcessor",
            "LOAN_DISBURSEMENT_REVERSAL"),

    LOAN_DISBURSEMENT_REPOST_FILE_EVENT("org.apache.fineract.portfolio.loanaccount.event.LoanDisbursementReportFileEventProcessor",
            "Disbursement Report File"),

    NOTFOUND("", ""),;

    private String clazz;
    private String hookName;

    public static CustomHookEventProcessorEnum fromClazz(String clazz) {
        return Arrays.stream(values()).filter(e -> e.getClazz().equals(clazz)).findFirst().orElse(NOTFOUND);
    }

    public static CustomHookEventProcessorEnum fromHookName(String hookName) {
        return Arrays.stream(values()).filter(e -> e.getHookName().equals(hookName)).findFirst().orElse(NOTFOUND);
    }
}
