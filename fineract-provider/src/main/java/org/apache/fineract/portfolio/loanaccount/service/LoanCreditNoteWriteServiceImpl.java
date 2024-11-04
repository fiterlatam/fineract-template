package org.apache.fineract.portfolio.loanaccount.service;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.infrastructure.core.service.CustomDateUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.documentmanagement.domain.Document;
import org.apache.fineract.infrastructure.documentmanagement.domain.DocumentRepository;
import org.apache.fineract.portfolio.loanaccount.data.SpecialWriteOffPayload;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCreditNote;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCreditNoteRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanCreditNoteAmountCannotBeZeroException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanCreditNoteDateCannotBeFutureException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanCreditNoteWriteServiceImpl implements LoanCreditNoteWriteService {

    private final LoanCreditNoteRepository loanCreditNoteRepository;
    private final LoanAssembler loanAssembler;
    private final DocumentRepository documentRepository;
    private final LoanWritePlatformService loanWritePlatformService;
    private final LoanReadPlatformService loanReadPlatformService;
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
        BigDecimal mandatoryInsurance = command.bigDecimalValueOfParameterDefaultToZeroIfNull("mandatoryInsurance");
        BigDecimal capital = command.bigDecimalValueOfParameterDefaultToZeroIfNull("capital");

        Long documentId = command.longValueOfParameterNamed("documentId");
        // if document id is not null, then fetch the document and attach it to the credit note
        Document document = null;
        LoanCreditNote creditNote = new LoanCreditNote();
        if (documentId != null) {
            document = this.documentRepository.getReferenceById(documentId);
            creditNote.setDocument(document);

        }

        // set the credit note fields
        creditNote.setLoan(loan);
        creditNote.setCreditNoteDate(creditNoteDate);
        creditNote.setArrearInterest(arrearInterest);
        creditNote.setCurrentInterest(currentInterest);
        creditNote.setHonorarios(honorarios);
        creditNote.setAval(aval);
        creditNote.setInsurance(insurance);
        creditNote.setMandatoryInsurance(mandatoryInsurance);
        creditNote.setCapital(capital);
        creditNote.calculateTotalAmount();

        // validate that total amount is not less than or equal to zero
        if (creditNote.getTotalAmount().compareTo(BigDecimal.ZERO) < 1) {
            throw new LoanCreditNoteAmountCannotBeZeroException();
        }

        // first apply the credit note to the loan
        this.applyCreditNoteToLoan(loan, creditNote);

        // save the credit note
        return this.loanCreditNoteRepository.save(creditNote);

    }

    private void applyCreditNoteToLoan(final Loan loan, LoanCreditNote creditNote) {
        // apply the credit note to the loan

        String payload = generateSpecialWriteOffPayload(loan, creditNote);
        log.info("Payload for special write off {}", payload);
        JsonElement jsonElement = fromApiJsonHelper.parse(payload);
        // setup json command
        JsonCommand jsonCommand = new JsonCommand(null, payload, jsonElement, fromApiJsonHelper, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        CommandProcessingResult result = this.loanWritePlatformService.specialWriteOff(loan.getId(), jsonCommand);

        creditNote.setTransactionId(result.getResourceId());

    }

    private String generateSpecialWriteOffPayload(Loan loan, LoanCreditNote creditNote) {
        // we need to chack for charges

        List<Map<String, Object>> charges = generateChargesForSpecialWriteOff(loan, creditNote);
        if (creditNote.getTotalAmount().compareTo(BigDecimal.ZERO) < 1) {
            throw new LoanCreditNoteAmountCannotBeZeroException();
        }

        SpecialWriteOffPayload specialWriteOffPayload = SpecialWriteOffPayload.builder().loanId(loan.getId())
                .principalPortion(creditNote.getCapital()).interestPortion(creditNote.getCurrentInterest())
                .totalWriteOffAmount(creditNote.getTotalAmount()).dateFormat(CustomDateUtils.SPANISH_DATE_FORMAT).locale("es").build();

        if (!charges.isEmpty()) {
            specialWriteOffPayload.setCharges(charges);
        }

        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(CustomDateUtils.SPANISH_DATE_FORMAT));
        return gsonBuilder.create().toJson(specialWriteOffPayload);

    }

    private static List<Map<String, Object>> generateChargesForSpecialWriteOff(Loan loan, LoanCreditNote creditNote) {
        List<Map<String, Object>> charges = new ArrayList<>();
        boolean writeOffCharge = creditNote.includesCharges();
        Collection<LoanCharge> loanCharges = loan.getCharges().stream().filter(LoanCharge::isNotPaid).toList();
        log.info("Loan charges {}", loanCharges.size());

        if (writeOffCharge) {

            // check for arrears
            BigDecimal arrearInterest = creditNote.getArrearInterest();
            if (arrearInterest != null && arrearInterest.compareTo(BigDecimal.ZERO) > 0) {
                LoanCharge arrearCharge = loanCharges.stream().filter(LoanCharge::isPenaltyCharge).findFirst().orElse(null);

                if (arrearCharge != null) {
                    charges.add(Map.of("chargeId", Objects.requireNonNull(arrearCharge.getCharge().getId()), "writeOffAmount",
                            creditNote.getArrearInterest()));
                } else {
                    creditNote.setArrearInterest(BigDecimal.ZERO);
                }

            }
            // check for voluntary insurance
            BigDecimal insurance = creditNote.getInsurance();
            if (insurance != null && insurance.compareTo(BigDecimal.ZERO) > 0) {
                LoanCharge insuranceCharge = loanCharges.stream().filter(loanCharge -> loanCharge.getCharge().isVoluntaryInsurance())
                        .findFirst().orElse(null);
                if (insuranceCharge != null) {
                    charges.add(Map.of("chargeId", Objects.requireNonNull(insuranceCharge.getCharge().getId()), "writeOffAmount",
                            creditNote.getInsurance()));
                } else {
                    creditNote.setInsurance(BigDecimal.ZERO);
                }
            }

            // check for mandatory insurance
            BigDecimal mandatoryInsurance = creditNote.getMandatoryInsurance();
            if (mandatoryInsurance != null && mandatoryInsurance.compareTo(BigDecimal.ZERO) > 0) {

                LoanCharge insuranceCharge = loanCharges.stream().filter(loanCharge -> loanCharge.getCharge().isMandatoryInsurance())
                        .findFirst().orElse(null);
                log.info(" is charge present {}", insuranceCharge);
                // check if insurance is present
                if (insuranceCharge != null) {
                    charges.add(Map.of("chargeId", Objects.requireNonNull(insuranceCharge.getCharge().getId()), "writeOffAmount",
                            creditNote.getMandatoryInsurance()));
                } else {
                    creditNote.setMandatoryInsurance(BigDecimal.ZERO);
                }
            }
            // check for honorarios
            BigDecimal honorarios = creditNote.getHonorarios();
            if (honorarios != null && honorarios.compareTo(BigDecimal.ZERO) > 0) {
                LoanCharge honorariosCharge = loanCharges.stream().filter(loanCharge -> loanCharge.getCharge().isFlatHono()).findFirst()
                        .orElse(null);
                if (honorariosCharge != null) {
                    charges.add(Map.of("chargeId", Objects.requireNonNull(honorariosCharge.getCharge().getId()), "writeOffAmount",
                            creditNote.getHonorarios()));
                } else {
                    creditNote.setHonorarios(BigDecimal.ZERO);
                }
            }
            // check for aval
            BigDecimal aval = creditNote.getAval();
            if (aval != null && aval.compareTo(BigDecimal.ZERO) > 0) {
                LoanCharge avalCharge = loanCharges.stream().filter(loanCharge -> loanCharge.getCharge().isAvalCharge()).findFirst()
                        .orElse(null);
                if (avalCharge != null) {
                    charges.add(Map.of("chargeId", Objects.requireNonNull(avalCharge.getCharge().getId()), "writeOffAmount",
                            creditNote.getAval()));
                } else {
                    creditNote.setAval(BigDecimal.ZERO);
                }
            }

        } else {
            creditNote.resetCharges();
        }
        creditNote.calculateTotalAmount();
        return charges;
    }
}
