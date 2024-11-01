package org.apache.fineract.portfolio.loanaccount.service;

import java.util.Collection;
import org.apache.fineract.portfolio.loanaccount.data.LoanCreditNoteData;

public interface LoanCreditNoteReadService {

    Collection<LoanCreditNoteData> retrieveAllCreditNotesForLoan(Long loanId);

    LoanCreditNoteData retrieveCreditNoteForLoan(Long loanId, Long creditNoteId);
}
