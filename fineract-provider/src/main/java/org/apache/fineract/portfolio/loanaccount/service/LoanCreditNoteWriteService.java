package org.apache.fineract.portfolio.loanaccount.service;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;

public interface LoanCreditNoteWriteService {

    CommandProcessingResult addLoanCreditNote(Long loanId, JsonCommand command);

    void processInvoiceOffsetByCreditNote(Long creditNoteId);
}
