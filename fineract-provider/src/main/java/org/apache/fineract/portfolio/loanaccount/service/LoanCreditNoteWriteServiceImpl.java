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
        BigDecimal currentInterest = command.bigDecimalValueOfParameterDefaultToZeroIfNull("currentInterest");
        BigDecimal arrearInterest = command.bigDecimalValueOfParameterDefaultToZeroIfNull("arrearInterest");
        BigDecimal honorarios = command.bigDecimalValueOfParameterDefaultToZeroIfNull("honorarios");
        BigDecimal aval = command.bigDecimalValueOfParameterDefaultToZeroIfNull("aval");
        BigDecimal insurance = command.bigDecimalValueOfParameterDefaultToZeroIfNull("insurance");
        BigDecimal capital = command.bigDecimalValueOfParameterDefaultToZeroIfNull("capital");

        Long documentId = command.longValueOfParameterNamed("documentId");
        // if document id is not null, then fetch the document and attach it to the credit note
        Document document = null;
        LoanCreditNote creditNote = new LoanCreditNote();
        if (documentId != null) {
            document = this.documentRepository.getReferenceById(documentId);
            creditNote.setDocument(document);

        }
        BigDecimal totalAmount = (currentInterest != null ? currentInterest : BigDecimal.ZERO)
                .add(arrearInterest != null ? arrearInterest : BigDecimal.ZERO).add(honorarios != null ? honorarios : BigDecimal.ZERO)
                .add(aval != null ? aval : BigDecimal.ZERO).add(insurance != null ? insurance : BigDecimal.ZERO)
                .add(capital != null ? capital : BigDecimal.ZERO);

        creditNote.setLoan(loan);
        creditNote.setCreditNoteDate(creditNoteDate);
        creditNote.setArrearInterest(arrearInterest);
        creditNote.setCurrentInterest(currentInterest);
        creditNote.setHonorarios(honorarios);
        creditNote.setAval(aval);
        creditNote.setInsurance(insurance);
        creditNote.setCapital(capital);
        creditNote.setTotalAmount(totalAmount);

        // save the credit note
        return this.loanCreditNoteRepository.save(creditNote);

    }
}
