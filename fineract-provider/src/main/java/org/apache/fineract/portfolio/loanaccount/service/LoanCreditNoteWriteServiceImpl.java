package org.apache.fineract.portfolio.loanaccount.service;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.custom.infrastructure.core.service.CustomDateUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.documentmanagement.domain.Document;
import org.apache.fineract.infrastructure.documentmanagement.domain.DocumentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCreditNote;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCreditNoteRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanCreditNoteAmountCannotBeZeroException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanCreditNoteDateCannotBeFutureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanCreditNoteWriteServiceImpl implements LoanCreditNoteWriteService {

    private final LoanCreditNoteRepository loanCreditNoteRepository;
    private final LoanAssembler loanAssembler;
    private final DocumentRepository documentRepository;
    private final LoanWritePlatformService loanWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;

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
        // validate that total amount is not zero
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new LoanCreditNoteAmountCannotBeZeroException();
        }

        // set the credit note fields
        creditNote.setLoan(loan);
        creditNote.setCreditNoteDate(creditNoteDate);
        creditNote.setArrearInterest(arrearInterest);
        creditNote.setCurrentInterest(currentInterest);
        creditNote.setHonorarios(honorarios);
        creditNote.setAval(aval);
        creditNote.setInsurance(insurance);
        creditNote.setCapital(capital);
        creditNote.setTotalAmount(totalAmount);

        // first apply the credit note to the loan
        this.applyCreditNoteToLoan(loan, creditNote);

        // save the credit note
        return this.loanCreditNoteRepository.save(creditNote);

    }

    private void applyCreditNoteToLoan(final Loan loan, LoanCreditNote creditNote) {
        // apply the credit note to the loan

        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(CustomDateUtils.SPANISH_DATE_FORMAT));
        Object payloadData = generateSpecialWriteOffPayload(loan, creditNote);
        String payload = gsonBuilder.create().toJson(payloadData);
        JsonElement jsonElement = fromApiJsonHelper.parse(payload);
        // setup json command
        JsonCommand jsonCommand = new JsonCommand(null, payload, jsonElement, fromApiJsonHelper, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        CommandProcessingResult result = this.loanWritePlatformService.specialWriteOff(loan.getId(), jsonCommand);

        creditNote.setTransactionId(result.getResourceId());

    }

    private String generateSpecialWriteOffPayload(Loan loan, LoanCreditNote creditNote) {
        // generate the payload for the special write off
        // this is a simplified version of the payload
        // you can add more fields as needed
        // here we are just setting the transaction date and the amount
        // you can add more fields as needed
        // here we are just setting the transaction date and the amount

        // sample paload
        /*
         *
         * { "loanId": 453, "charges": [ { "chargeId": 5, "writeOffAmount": 2594 }, { "chargeId": 46, "writeOffAmount":
         * 8800 }, { "chargeId": 35, "writeOffAmount": 32 } ], "principalPortion": 1000, "interestPortion": 12,
         * "totalWriteOffAmount": 12438, "dateFormat": "dd MMMM yyyy", "locale": "en" }
         */
        // TODO , I'll be changing this in the next commit
        return "{\n" + "  \"loanId\": " + loan.getId() + ",\n" + "  \"charges\": [\n" + "    {\n" + "      \"chargeId\": 5,\n"
                + "      \"writeOffAmount\": " + creditNote.getArrearInterest() + "\n" + "    },\n" + "    {\n"
                + "      \"chargeId\": 46,\n" + "      \"writeOffAmount\": " + creditNote.getHonorarios() + "\n" + "    },\n" + "    {\n"
                + "      \"chargeId\": 35,\n" + "      \"writeOffAmount\": " + creditNote.getAval() + "\n" + "    }\n" + "  ],\n"
                + "  \"principalPortion\": " + creditNote.getCapital() + ",\n" + "  \"interestPortion\": " + creditNote.getCurrentInterest()
                + ",\n" + "  \"totalWriteOffAmount\": " + creditNote.getTotalAmount() + ",\n" + "  \"dateFormat\": \"dd MMMM yyyy\",\n"
                + "  \"locale\": \"en\"\n" + "}";
    }
}
