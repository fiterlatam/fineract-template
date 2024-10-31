package org.apache.fineract.portfolio.loanaccount.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.documentmanagement.domain.Document;
import org.apache.fineract.infrastructure.documentmanagement.domain.DocumentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCreditNote;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCreditNoteRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanCreditNoteDateCannotBeFutureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanCreditNoteWriteServiceImpl implements LoanCreditNoteWriteService {

    private final LoanCreditNoteRepository loanCreditNoteRepository;
    private final LoanAssembler loanAssembler;
    private final DocumentRepository documentRepository;

    @Override
    public CommandProcessingResult addLoanCreditNote(Long loanId, JsonCommand command) {
        // first assemble the associated loan
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        // assemble the credit note
        final LoanCreditNote creditNote = this.assembleLoanCreditNote(loan, command);

        return CommandProcessingResult.commandOnlyResult(creditNote.getId());
    }

    // create a method to validate and assemble the credit note
    private LoanCreditNote assembleLoanCreditNote(final Loan loan, final JsonCommand command) {
        // extract fields and validate them
        final LocalDate currentDate = DateUtils.getLocalDateOfTenant();
        LocalDate creditNoteDate = command.localDateValueOfParameterNamed("creditNoteDate");
        // credit note date cannot be a future date
        if (creditNoteDate.isAfter(currentDate)) {
            throw new LoanCreditNoteDateCannotBeFutureException();
        }
        BigDecimal currentInterest = command.bigDecimalValueOfParameterNamed("currentInterest");
        BigDecimal arrearInterest = command.bigDecimalValueOfParameterNamed("arrearInterest");
        BigDecimal honoarios = command.bigDecimalValueOfParameterNamed("honoarios");
        BigDecimal aval = command.bigDecimalValueOfParameterNamed("aval");
        BigDecimal insurance = command.bigDecimalValueOfParameterNamed("insurance");
        BigDecimal capital = command.bigDecimalValueOfParameterNamed("capital");
        BigDecimal totalAmount = command.bigDecimalValueOfParameterNamed("totalAmount");
        Long documentId = command.longValueOfParameterNamed("documentId");
        // if document id is not null, then fetch the document and attach it to the credit note
        Document document = null;
        LoanCreditNote creditNote = new LoanCreditNote();
        if (documentId != null) {
            document = this.documentRepository.getReferenceById(documentId);
            creditNote.setDocument(document);

        }

        creditNote.setLoan(loan);
        creditNote.setCreditNoteDate(creditNoteDate);
        creditNote.setArrearInterest(arrearInterest);
        creditNote.setCurrentInterest(currentInterest);
        creditNote.setHonoarios(honoarios);
        creditNote.setAval(aval);
        creditNote.setInsurance(insurance);
        creditNote.setCapital(capital);
        creditNote.setTotalAmount(totalAmount);
        // creditNote.setAttachment(creditNoteData.getAttachment());

        // save the credit note
        return this.loanCreditNoteRepository.save(creditNote);

    }
}
