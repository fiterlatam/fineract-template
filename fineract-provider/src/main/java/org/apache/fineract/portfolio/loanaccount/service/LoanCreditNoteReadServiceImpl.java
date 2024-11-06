package org.apache.fineract.portfolio.loanaccount.service;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.portfolio.loanaccount.data.LoanCreditNoteData;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCreditNote;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCreditNoteRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanCreditNoteNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanCreditNoteReadServiceImpl implements LoanCreditNoteReadService {

    private final LoanCreditNoteRepository loanCreditNoteRepository;

    @Override
    public Collection<LoanCreditNoteData> retrieveAllCreditNotesForLoan(Long loanId) {

        List<LoanCreditNote> creditNotes = loanCreditNoteRepository.findByLoan_Id(loanId);

        return creditNotes.stream().map(LoanCreditNote::toData).toList();
    }

    @Override
    public LoanCreditNoteData retrieveCreditNoteForLoan(Long loanId, Long creditNoteId) {
        LoanCreditNote creditNote = loanCreditNoteRepository.findByLoan_IdAndId(loanId, creditNoteId);
        if (creditNote != null) {
            return creditNote.toData();
        }
        throw new LoanCreditNoteNotFoundException(creditNoteId, loanId);
    }
}
