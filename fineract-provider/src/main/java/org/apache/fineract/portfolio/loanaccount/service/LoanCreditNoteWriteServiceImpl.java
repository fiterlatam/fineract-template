package org.apache.fineract.portfolio.loanaccount.service;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.fineract.custom.infrastructure.core.service.CustomDateUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.documentmanagement.domain.Document;
import org.apache.fineract.infrastructure.documentmanagement.domain.DocumentRepository;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanCreditNoteBusinessEvent;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.client.data.ClientAdditionalFieldsData;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.data.SpecialWriteOffPayload;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCreditNote;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCreditNoteRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInstallmentCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInvoiceOffsetByCreditNote;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanCreditNoteAmountCannotBeZeroException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanCreditNoteDateCannotBeFutureException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanCreditNoteNotFoundException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanTransactionNotFoundException;
import org.apache.fineract.portfolio.loanaccount.invoice.data.LoanDocumentData;
import org.apache.fineract.portfolio.loanaccount.invoice.data.LoanElectronicInvoiceData;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicMensualRepository;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicaMensual;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.LoanDocumentConcept;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterization;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterizationRepository;
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
    private final BusinessEventNotifierService businessEventNotifierService;
    private final LoanTransactionRepository loanTransactionRepository;
    private final ClientReadPlatformService clientReadPlatformService;
    private final FacturaElectronicMensualRepository facturaElectronicMensualRepository;
    private final LoanProductParameterizationRepository productParameterizationRepository;

    @Override
    public CommandProcessingResult addLoanCreditNote(Long loanId, JsonCommand command) {
        // first assemble the associated loan
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        final LoanProduct loanProduct = loan.loanProduct();
        if (Boolean.FALSE.equals(loanProduct.getCustomAllowCreditNote())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.credit.note.not.allowed",
                    "Credit note is not allowed for this loan product");
        }
        // assemble the credit note
        final LoanCreditNote creditNote = this.assembleLoanCreditNote(loan, command);
        final LoanTransaction loanTransaction = this.loanTransactionRepository.findById(creditNote.getTransactionId())
                .orElseThrow(() -> new LoanTransactionNotFoundException(creditNote.getTransactionId()));

        this.businessEventNotifierService.notifyPostBusinessEvent(new LoanCreditNoteBusinessEvent(loanTransaction));
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
        CommandProcessingResult result = this.loanWritePlatformService.specialWriteOff(loan.getId(), jsonCommand, creditNote);
        creditNote.setTransactionId(result.getResourceId());
    }

    private String generateSpecialWriteOffPayload(Loan loan, LoanCreditNote creditNote) {
        // we need to check for charges
        List<Map<String, Object>> charges = getListOfChargesForCreditNote(loan, creditNote);
        if (creditNote.getTotalAmount().compareTo(BigDecimal.ZERO) < 1) {
            throw new LoanCreditNoteAmountCannotBeZeroException();
        }
        SpecialWriteOffPayload specialWriteOffPayload = SpecialWriteOffPayload.builder().loanId(loan.getId())
                .principalPortion(creditNote.getCapital()).interestPortion(creditNote.getCurrentInterest())
                .totalWriteOffAmount(creditNote.getTotalAmount()).dateFormat(CustomDateUtils.SPANISH_DATE_FORMAT).locale("es")
                .isCreditNote(true).build();

        if (!charges.isEmpty()) {
            specialWriteOffPayload.setCharges(charges);
        }

        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(CustomDateUtils.SPANISH_DATE_FORMAT));
        return gsonBuilder.create().toJson(specialWriteOffPayload);

    }

    @Deprecated
    private List<Map<String, Object>> generateChargesForSpecialWriteOff(Loan loan, LoanCreditNote creditNote) {
        List<Map<String, Object>> charges = new ArrayList<>();
        boolean writeOffCharge = creditNote.includesCharges();
        LoanTransactionData loanTransaction = this.loanReadPlatformService.retrieveLoanSpecialWriteOffTemplate(loan.getId());
        List<LoanChargeData> currentOutstandingLoanCharges = loanTransaction.getCurrentOutstandingLoanCharges();
        List<Long> currentOutstandingLoanChargeIds = currentOutstandingLoanCharges.stream().map(LoanChargeData::getChargeId).toList();
        Collection<LoanCharge> loanCharges = loan.getCharges().stream().filter(LoanCharge::isNotPaid)
                .filter(l -> currentOutstandingLoanChargeIds.contains(l.getCharge().getId())).toList();
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
                    final LoanInstallmentCharge loanInstallmentCharge = insuranceCharge.getInstallmentLoanCharge(1);
                    final LoanCharge vatCharge = loanCharges.stream().filter(
                            loanCharge -> Objects.equals(insuranceCharge.getCharge().getId(), loanCharge.getCharge().getParentChargeId()))
                            .findFirst().orElse(null);
                    if (vatCharge != null && loanInstallmentCharge != null && vatCharge.getInstallmentLoanCharge(1) != null) {
                        final LoanInstallmentCharge vatInstallmentCharge = vatCharge.getInstallmentLoanCharge(1);
                        final BigDecimal divider = loanInstallmentCharge.getAmount().add(vatInstallmentCharge.getAmount());
                        final BigDecimal vatChargePortion = vatInstallmentCharge.getAmount().multiply(insurance).divide(divider,
                                RoundingMode.HALF_UP);
                        final BigDecimal chargePortion = insurance.subtract(vatChargePortion);
                        charges.add(Map.of("chargeId", Objects.requireNonNull(insuranceCharge.getCharge().getId()), "writeOffAmount",
                                chargePortion));
                        charges.add(Map.of("chargeId", Objects.requireNonNull(vatCharge.getCharge().getId()), "writeOffAmount",
                                vatChargePortion));
                    } else {
                        charges.add(Map.of("chargeId", Objects.requireNonNull(insuranceCharge.getCharge().getId()), "writeOffAmount",
                                creditNote.getMandatoryInsurance()));
                    }
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
                    final LoanInstallmentCharge loanInstallmentCharge = insuranceCharge.getInstallmentLoanCharge(1);
                    final LoanCharge vatCharge = loanCharges.stream().filter(
                            loanCharge -> Objects.equals(insuranceCharge.getCharge().getId(), loanCharge.getCharge().getParentChargeId()))
                            .findFirst().orElse(null);
                    if (vatCharge != null && loanInstallmentCharge != null && vatCharge.getInstallmentLoanCharge(1) != null) {
                        BigDecimal vatChargePortion = currentOutstandingLoanCharges.stream()
                                .filter(loanCharge -> loanCharge.getChargeId().compareTo(vatCharge.getCharge().getId()) == 0)
                                .map(lc -> lc.getAmountOutstanding()).reduce(BigDecimal.ZERO, BigDecimal::add);

                        // If charge is already calculated in DB, use it instead of recalculate
                        if (BigDecimal.ZERO.compareTo(vatChargePortion) == 0) {
                            final LoanInstallmentCharge vatInstallmentCharge = vatCharge.getInstallmentLoanCharge(1);
                            final BigDecimal divider = loanInstallmentCharge.getAmount().add(vatInstallmentCharge.getAmount());
                            vatChargePortion = vatInstallmentCharge.getAmount().multiply(mandatoryInsurance).divide(divider,
                                    RoundingMode.HALF_UP);
                        }

                        final BigDecimal chargePortion = mandatoryInsurance.subtract(vatChargePortion);
                        charges.add(Map.of("chargeId", Objects.requireNonNull(insuranceCharge.getCharge().getId()), "writeOffAmount",
                                chargePortion));
                        charges.add(Map.of("chargeId", Objects.requireNonNull(vatCharge.getCharge().getId()), "writeOffAmount",
                                vatChargePortion));
                    } else {
                        charges.add(Map.of("chargeId", Objects.requireNonNull(insuranceCharge.getCharge().getId()), "writeOffAmount",
                                creditNote.getMandatoryInsurance()));
                    }
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
                    final LoanInstallmentCharge loanInstallmentCharge = avalCharge.getInstallmentLoanCharge(1);
                    final LoanCharge vatCharge = loanCharges.stream().filter(
                            loanCharge -> Objects.equals(avalCharge.getCharge().getId(), loanCharge.getCharge().getParentChargeId()))
                            .findFirst().orElse(null);
                    if (vatCharge != null && loanInstallmentCharge != null && vatCharge.getInstallmentLoanCharge(1) != null) {
                        final LoanInstallmentCharge vatInstallmentCharge = vatCharge.getInstallmentLoanCharge(1);
                        final BigDecimal divider = loanInstallmentCharge.getAmount().add(vatInstallmentCharge.getAmount());
                        final BigDecimal vatChargePortion = vatInstallmentCharge.getAmount().multiply(mandatoryInsurance).divide(divider,
                                RoundingMode.HALF_UP);
                        final BigDecimal chargePortion = aval.subtract(vatChargePortion);
                        charges.add(Map.of("chargeId", Objects.requireNonNull(avalCharge.getCharge().getId()), "writeOffAmount",
                                chargePortion));
                        charges.add(Map.of("chargeId", Objects.requireNonNull(vatCharge.getCharge().getId()), "writeOffAmount",
                                vatChargePortion));
                    } else {
                        charges.add(Map.of("chargeId", Objects.requireNonNull(avalCharge.getCharge().getId()), "writeOffAmount",
                                creditNote.getMandatoryInsurance()));
                    }
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

    private List<Map<String, Object>> getListOfChargesForCreditNote(Loan loan, LoanCreditNote creditNote) {
        List<Map<String, Object>> charges = new ArrayList<>();
        if (creditNote.includesCharges()) {
            Money aval = Money.of(loan.getCurrency(), creditNote.getAval());
            Money honorarios = Money.of(loan.getCurrency(), creditNote.getHonorarios());
            Money mandatoryInsurance = Money.of(loan.getCurrency(), creditNote.getMandatoryInsurance());
            Money insurance = Money.of(loan.getCurrency(), creditNote.getInsurance());
            Money arrearInterest = Money.of(loan.getCurrency(), creditNote.getArrearInterest());

            Collection<LoanCharge> loanCharges = loan.getCharges().stream().filter(LoanCharge::isNotFullyPaid).toList();

            if (aval.isGreaterThanZero()) {
                LoanCharge avalCharge = loanCharges.stream().filter(loanCharge -> loanCharge.getCharge().isAvalCharge()).findFirst()
                        .orElse(null);
                if (avalCharge != null) {
                    for (LoanCharge vatCharge : loanCharges) {
                        if (Objects.equals(avalCharge.getCharge().getId(), vatCharge.getCharge().getParentChargeId())) {
                            Money vatAmount = aval.percentageOfVat(vatCharge.getPercentage(), RoundingMode.HALF_UP);
                            charges.add(Map.of("chargeId", Objects.requireNonNull(vatCharge.getCharge().getId()), "writeOffAmount",
                                    vatAmount.getAmount()));
                            aval = aval.minus(vatAmount);
                            break;
                        }
                    }
                    charges.add(
                            Map.of("chargeId", Objects.requireNonNull(avalCharge.getCharge().getId()), "writeOffAmount", aval.getAmount()));
                } else {
                    throw new GeneralPlatformDomainRuleException("charge.not.found.for.credit.note", "Loan does not have aval charge",
                            "aval");
                }
            }

            if (honorarios.isGreaterThanZero()) {
                LoanCharge honorariosCharge = loanCharges.stream().filter(loanCharge -> loanCharge.getCharge().isFlatHono()).findFirst()
                        .orElse(null);
                if (honorariosCharge != null) {
                    for (LoanCharge vatCharge : loanCharges) {
                        if (Objects.equals(honorariosCharge.getCharge().getId(), vatCharge.getCharge().getParentChargeId())) {
                            Money vatAmount = honorarios.percentageOfVat(vatCharge.getPercentage(), RoundingMode.HALF_UP);
                            charges.add(Map.of("chargeId", Objects.requireNonNull(vatCharge.getCharge().getId()), "writeOffAmount",
                                    vatAmount.getAmount()));
                            honorarios = honorarios.minus(vatAmount);
                            break;
                        }
                    }
                    charges.add(Map.of("chargeId", Objects.requireNonNull(honorariosCharge.getCharge().getId()), "writeOffAmount",
                            honorarios.getAmount()));
                } else {
                    throw new GeneralPlatformDomainRuleException("charge.not.found.for.credit.note", "Loan does not have honorarios charge",
                            "honorarios");
                }
            }

            if (insurance.isGreaterThanZero()) {
                LoanCharge insuranceCharge = loanCharges.stream().filter(loanCharge -> loanCharge.getCharge().isVoluntaryInsurance())
                        .findFirst().orElse(null);
                if (insuranceCharge != null) {
                    for (LoanCharge vatCharge : loanCharges) {
                        if (Objects.equals(insuranceCharge.getCharge().getId(), vatCharge.getCharge().getParentChargeId())) {
                            Money vatAmount = insurance.percentageOfVat(vatCharge.getPercentage(), RoundingMode.HALF_UP);
                            charges.add(Map.of("chargeId", Objects.requireNonNull(vatCharge.getCharge().getId()), "writeOffAmount",
                                    vatAmount.getAmount()));
                            insurance = insurance.minus(vatAmount);
                            break;
                        }
                    }
                    charges.add(Map.of("chargeId", Objects.requireNonNull(insuranceCharge.getCharge().getId()), "writeOffAmount",
                            insurance.getAmount()));
                } else {
                    throw new GeneralPlatformDomainRuleException("charge.not.found.for.credit.note",
                            "Loan does not have voluntary assurance charge", "seguro voluntario");
                }
            }

            if (mandatoryInsurance.isGreaterThanZero()) {
                LoanCharge mandatoryInsuranceCharge = loanCharges.stream()
                        .filter(loanCharge -> loanCharge.getCharge().isMandatoryInsurance()).findFirst().orElse(null);
                if (mandatoryInsuranceCharge != null) {
                    for (LoanCharge vatCharge : loanCharges) {
                        if (Objects.equals(mandatoryInsuranceCharge.getCharge().getId(), vatCharge.getCharge().getParentChargeId())) {
                            Money vatAmount = mandatoryInsurance.percentageOfVat(vatCharge.getPercentage(), RoundingMode.HALF_UP);
                            charges.add(Map.of("chargeId", Objects.requireNonNull(vatCharge.getCharge().getId()), "writeOffAmount",
                                    vatAmount.getAmount()));
                            mandatoryInsurance = mandatoryInsurance.minus(vatAmount);
                            break;
                        }
                    }
                    charges.add(Map.of("chargeId", Objects.requireNonNull(mandatoryInsuranceCharge.getCharge().getId()), "writeOffAmount",
                            mandatoryInsurance.getAmount()));
                } else {
                    throw new GeneralPlatformDomainRuleException("charge.not.found.for.credit.note",
                            "Loan does not have mandatory insurance charge", "seguro obligatorio");
                }
            }

            if (arrearInterest.isGreaterThanZero()) {
                LoanCharge arrearsCharge = loanCharges.stream().filter(loanCharge -> loanCharge.getCharge().isPenalty()).findFirst()
                        .orElse(null);
                if (arrearsCharge != null) {
                    for (LoanCharge vatCharge : loanCharges) {
                        if (Objects.equals(arrearsCharge.getCharge().getId(), vatCharge.getCharge().getParentChargeId())) {
                            Money vatAmount = arrearInterest.percentageOfVat(vatCharge.getPercentage(), RoundingMode.HALF_UP);
                            charges.add(Map.of("chargeId", Objects.requireNonNull(vatCharge.getCharge().getId()), "writeOffAmount",
                                    vatAmount.getAmount()));
                            arrearInterest = arrearInterest.minus(vatAmount);
                            break;
                        }
                    }
                    charges.add(Map.of("chargeId", Objects.requireNonNull(arrearsCharge.getCharge().getId()), "writeOffAmount",
                            arrearInterest.getAmount()));
                } else {
                    throw new GeneralPlatformDomainRuleException("charge.not.found.for.credit.note", "Loan does not have arrears charge",
                            "penalización");
                }
            }
        }
        return charges;
    }

    private BigDecimal percentageOf(BigDecimal amount, BigDecimal percentage, final RoundingMode roundingMode, MonetaryCurrency currency) {
        final BigDecimal newAmount = amount.multiply(percentage).divide(BigDecimal.valueOf(100), roundingMode);
        return Money.of(currency, newAmount).getAmount();
    }

    @SuppressWarnings("unused")
    private BigDecimal calculateVatAmount(Collection<LoanCharge> loanCharges, LoanCharge parentCharge, BigDecimal amount,
            MonetaryCurrency currency, List<Map<String, Object>> charges) {
        BigDecimal vatAmount = BigDecimal.ZERO;
        BigDecimal parentChargeAmount = BigDecimal.ZERO;
        for (LoanInstallmentCharge installmentCharge : parentCharge.installmentCharges()) {
            if (installmentCharge.isPaid()) {
                continue;
            } else {
                parentChargeAmount = installmentCharge.getAmountOutstanding();
                break;
            }
        }
        for (LoanCharge vatCharge : loanCharges) {
            if (Objects.equals(parentCharge.getCharge().getId(), vatCharge.getCharge().getParentChargeId())) {
                BigDecimal outstandingVatAmount = BigDecimal.ZERO;
                for (LoanInstallmentCharge installmentCharge : vatCharge.installmentCharges()) {
                    if (installmentCharge.isPaid()) {
                        continue;
                    } else {
                        outstandingVatAmount = installmentCharge.getAmountOutstanding();
                        break;
                    }
                }
                if (outstandingVatAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal calculatedAmount = percentageOf(parentChargeAmount, vatCharge.amountOrPercentage(), RoundingMode.HALF_UP,
                            currency);
                    if (calculatedAmount.compareTo(outstandingVatAmount) < 0) {
                        outstandingVatAmount = calculatedAmount;
                    }
                }
                vatAmount = outstandingVatAmount;
                if (vatAmount.compareTo(BigDecimal.ZERO) > 0) {
                    charges.add(Map.of("chargeId", Objects.requireNonNull(vatCharge.getCharge().getId()), "writeOffAmount", vatAmount));
                }
                break;
            }
        }

        return vatAmount;
    }

    private Set<FacturaElectronicaMensual> processElectronicCreditNoteForConcept(final CreditNoteConceptAmount creditNoteConceptAmount,
            final LoanDocumentConcept loanDocumentConcept, final String clientIdNumber, final String loanProductType,
            final LoanCreditNote loanCreditNote, final AtomicLong itemCounter, final String documentNumber,
            final LoanProductParameterization loanProductParameterization) {
        final List<LoanElectronicInvoiceData> interestInvoicesToBeOffset = loanReadPlatformService
                .retrieveAvailableElectronicInvoicesToBeOffset(clientIdNumber, loanProductType, loanDocumentConcept.getSku());
        final Set<FacturaElectronicaMensual> newCreditNoteDocuments = new HashSet<>();
        if (!interestInvoicesToBeOffset.isEmpty()) {
            for (final LoanElectronicInvoiceData interestInvoiceToBeOffData : interestInvoicesToBeOffset) {
                final Long invoiceId = interestInvoiceToBeOffData.getId();
                final FacturaElectronicaMensual facturaElectronicaMensualToBeOffset = this.facturaElectronicMensualRepository
                        .findById(invoiceId)
                        .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.loan.credit.note.invoice.not.found",
                                "Invoice not found {} " + invoiceId));
                final BigDecimal precioUnitario = interestInvoiceToBeOffData.getPrecioUnitario();
                final BigDecimal offsetAmountAccountedFor = interestInvoiceToBeOffData.getOffsetAmountAccountedFor();
                final BigDecimal invoiceAmountToBeOffset = precioUnitario.subtract(offsetAmountAccountedFor);
                final String offsetInvoiceNumber = facturaElectronicaMensualToBeOffset.getNumero_doc();
                final LocalDate offsetInvoiceDate = facturaElectronicaMensualToBeOffset.getFecha_factura();
                final BigDecimal remainingConceptAmountToBeUsed = creditNoteConceptAmount.getRemainingAmount();
                if (remainingConceptAmountToBeUsed.compareTo(BigDecimal.ZERO) > 0
                        && invoiceAmountToBeOffset.compareTo(BigDecimal.ZERO) > 0) {
                    final Long itemPosition = itemCounter.incrementAndGet();
                    final FacturaElectronicaMensual creditNoteDocument = facturaElectronicaMensualToBeOffset.clone();
                    creditNoteDocument.copyValuesFromProductParameterization(loanProductParameterization);
                    creditNoteDocument.setPosicion(itemPosition);
                    creditNoteDocument.setCreatedDate(DateUtils.getAuditOffsetDateTime());
                    creditNoteDocument.setNumero_doc(documentNumber);
                    creditNoteDocument.setId(null);
                    creditNoteDocument.setTip_doc(LoanDocumentData.LoanDocumentType.CREDIT_NOTE.getCode());
                    creditNoteDocument.setTipo_factura("1");
                    creditNoteDocument.setNum_facafect(offsetInvoiceNumber);
                    creditNoteDocument.setFec_facafect(offsetInvoiceDate);

                    if (remainingConceptAmountToBeUsed.compareTo(invoiceAmountToBeOffset) >= 0) {
                        facturaElectronicaMensualToBeOffset.setFullyOffsetByCN(true);
                        final LoanInvoiceOffsetByCreditNote loanInvoiceOffsetByCreditNote = new LoanInvoiceOffsetByCreditNote();
                        loanInvoiceOffsetByCreditNote.setLoanCreditNote(loanCreditNote);
                        loanInvoiceOffsetByCreditNote.setFacturaElectronicaMensual(facturaElectronicaMensualToBeOffset);
                        loanInvoiceOffsetByCreditNote.adjustPortionByConcept(loanDocumentConcept, invoiceAmountToBeOffset);
                        loanInvoiceOffsetByCreditNote.setActive(true);
                        loanCreditNote.getLoanInvoiceOffsetByCreditNoteSet().add(loanInvoiceOffsetByCreditNote);

                        // Populate the credit note document
                        final BigDecimal baseValue = creditNoteConceptAmount.determineBaseValue(invoiceAmountToBeOffset).setScale(2,
                                RoundingMode.HALF_UP);
                        final BigDecimal vatValue = creditNoteConceptAmount.determineVatValue(invoiceAmountToBeOffset).setScale(2,
                                RoundingMode.HALF_UP);
                        creditNoteDocument.setCosto_total(baseValue);
                        creditNoteDocument.setTotal(baseValue);
                        creditNoteDocument.setBase(baseValue);
                        creditNoteDocument.setPrecio_unitario(baseValue);
                        creditNoteDocument.setImpuesto_item(vatValue);
                        creditNoteDocument.setFullyOffsetByCN(false);
                        newCreditNoteDocuments.add(creditNoteDocument);
                        this.facturaElectronicMensualRepository.save(facturaElectronicaMensualToBeOffset);
                        creditNoteConceptAmount.incrementAccountedForAmount(invoiceAmountToBeOffset);
                    } else {
                        facturaElectronicaMensualToBeOffset.setFullyOffsetByCN(false);
                        final LoanInvoiceOffsetByCreditNote loanInvoiceOffsetByCreditNote = new LoanInvoiceOffsetByCreditNote();
                        loanInvoiceOffsetByCreditNote.setLoanCreditNote(loanCreditNote);
                        loanInvoiceOffsetByCreditNote.setFacturaElectronicaMensual(facturaElectronicaMensualToBeOffset);
                        loanInvoiceOffsetByCreditNote.setInterestPortion(remainingConceptAmountToBeUsed);
                        loanInvoiceOffsetByCreditNote.adjustPortionByConcept(loanDocumentConcept, remainingConceptAmountToBeUsed);
                        loanInvoiceOffsetByCreditNote.setActive(true);
                        loanCreditNote.getLoanInvoiceOffsetByCreditNoteSet().add(loanInvoiceOffsetByCreditNote);

                        // Populate the credit note document
                        final BigDecimal baseValue = creditNoteConceptAmount.determineBaseValue(remainingConceptAmountToBeUsed).setScale(2,
                                RoundingMode.HALF_UP);
                        final BigDecimal vatValue = creditNoteConceptAmount.determineVatValue(remainingConceptAmountToBeUsed).setScale(2,
                                RoundingMode.HALF_UP);
                        creditNoteDocument.setCosto_total(baseValue);
                        creditNoteDocument.setTotal(baseValue);
                        creditNoteDocument.setBase(baseValue);
                        creditNoteDocument.setImpuesto_item(vatValue);
                        creditNoteDocument.setCosto_total(baseValue);
                        creditNoteDocument.setPrecio_unitario(baseValue);
                        creditNoteDocument.setFullyOffsetByCN(false);
                        newCreditNoteDocuments.add(creditNoteDocument);
                        this.facturaElectronicMensualRepository.save(facturaElectronicaMensualToBeOffset);
                        creditNoteConceptAmount.incrementAccountedForAmount(remainingConceptAmountToBeUsed);
                        break;
                    }
                }
            }
        }
        return newCreditNoteDocuments;
    }

    @Override
    public void processInvoiceOffsetByCreditNote(final Long creditNoteId) {
        final LoanCreditNote loanCreditNote = this.loanCreditNoteRepository.findById(creditNoteId)
                .orElseThrow(() -> new LoanCreditNoteNotFoundException(creditNoteId));
        if (!loanCreditNote.isFullyUsedByInvoice()) {
            final Long clientId = loanCreditNote.getLoan().getClientId();
            final InvoiceGenerationResult invoiceGenerationResult = this.generateInvoiceNumber(loanCreditNote);
            final AtomicLong itemCounter = invoiceGenerationResult.getItemCounter();
            final String documentNumber = invoiceGenerationResult.getDocumentNumber();
            final LoanProductParameterization loanProductParameterization = invoiceGenerationResult.getLoanProductParameterization();
            final ClientAdditionalFieldsData clientAdditionalInformation = this.clientReadPlatformService
                    .retrieveClientAdditionalData(clientId);
            final String clientIdNumber = ObjectUtils.defaultIfNull(clientAdditionalInformation.getNit(),
                    clientAdditionalInformation.getCedula());
            final String loanProductType = loanCreditNote.getLoan().loanProduct().getProductType().getLabel();
            final Set<LoanInvoiceOffsetByCreditNote> loanInvoiceOffsetByCreditNoteSet = loanCreditNote
                    .getLoanInvoiceOffsetByCreditNoteSet();

            final BigDecimal interestPortionAccountedFor = loanInvoiceOffsetByCreditNoteSet.stream()
                    .filter(LoanInvoiceOffsetByCreditNote::isActive).map(LoanInvoiceOffsetByCreditNote::getInterestPortion)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            final CreditNoteConceptAmount interestCreditNoteConceptAmount = new CreditNoteConceptAmount(loanCreditNote.getCurrentInterest(),
                    BigDecimal.ZERO, interestPortionAccountedFor);
            final Set<FacturaElectronicaMensual> interestElectronicCns = processElectronicCreditNoteForConcept(
                    interestCreditNoteConceptAmount, LoanDocumentConcept.INT_CORRIENTE, clientIdNumber, loanProductType, loanCreditNote,
                    itemCounter, documentNumber, loanProductParameterization);

            final BigDecimal mandatoryPortionAccountedFor = loanInvoiceOffsetByCreditNoteSet.stream()
                    .filter(LoanInvoiceOffsetByCreditNote::isActive).map(LoanInvoiceOffsetByCreditNote::getMandatoryInsurancePortion)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            final CreditNoteConceptAmount mandatoryInsuranceCreditNoteConceptAmount = new CreditNoteConceptAmount(
                    loanCreditNote.getMandatoryInsurance(), loanCreditNote.getMandatoryInsuranceVat(), mandatoryPortionAccountedFor);
            final Set<FacturaElectronicaMensual> mandatoryInsuranceElectronicCns = processElectronicCreditNoteForConcept(
                    mandatoryInsuranceCreditNoteConceptAmount, LoanDocumentConcept.SEGURO_OBLIGATORIO, clientIdNumber, loanProductType,
                    loanCreditNote, itemCounter, documentNumber, loanProductParameterization);

            final BigDecimal voluntaryPortionAccountedFor = loanInvoiceOffsetByCreditNoteSet.stream()
                    .filter(LoanInvoiceOffsetByCreditNote::isActive).map(LoanInvoiceOffsetByCreditNote::getVoluntaryInsurancePortion)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            final CreditNoteConceptAmount voluntaryInsuranceCreditNoteConceptAmount = new CreditNoteConceptAmount(
                    loanCreditNote.getInsurance(), loanCreditNote.getVoluntaryInsuranceVat(), voluntaryPortionAccountedFor);
            final Set<FacturaElectronicaMensual> voluntaryInsuranceElectronicCns = processElectronicCreditNoteForConcept(
                    voluntaryInsuranceCreditNoteConceptAmount, LoanDocumentConcept.SEGUROS_VOLUNTARIOS, clientIdNumber, loanProductType,
                    loanCreditNote, itemCounter, documentNumber, loanProductParameterization);

            final BigDecimal honorariosPortionAccountedFor = loanInvoiceOffsetByCreditNoteSet.stream()
                    .filter(LoanInvoiceOffsetByCreditNote::isActive).map(LoanInvoiceOffsetByCreditNote::getHonorariosPortion)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            final CreditNoteConceptAmount honorariosCreditNoteConceptAmount = new CreditNoteConceptAmount(loanCreditNote.getHonorarios(),
                    loanCreditNote.getHonorariosVat(), honorariosPortionAccountedFor);
            final Set<FacturaElectronicaMensual> honorariosElectronicCns = processElectronicCreditNoteForConcept(
                    honorariosCreditNoteConceptAmount, LoanDocumentConcept.HONORARIOS, clientIdNumber, loanProductType, loanCreditNote,
                    itemCounter, documentNumber, loanProductParameterization);

            final BigDecimal penaltyPortionAccountedFor = loanInvoiceOffsetByCreditNoteSet.stream()
                    .filter(LoanInvoiceOffsetByCreditNote::isActive).map(LoanInvoiceOffsetByCreditNote::getPenaltyPortion)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            final CreditNoteConceptAmount penaltyCreditNoteConceptAmount = new CreditNoteConceptAmount(loanCreditNote.getArrearInterest(),
                    loanCreditNote.getPenaltyVat(), penaltyPortionAccountedFor);
            final Set<FacturaElectronicaMensual> penaltyElectronicCns = processElectronicCreditNoteForConcept(
                    penaltyCreditNoteConceptAmount, LoanDocumentConcept.INT_DE_MORA, clientIdNumber, loanProductType, loanCreditNote,
                    itemCounter, documentNumber, loanProductParameterization);

            if (interestCreditNoteConceptAmount.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0
                    && mandatoryInsuranceCreditNoteConceptAmount.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0
                    && voluntaryInsuranceCreditNoteConceptAmount.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0
                    && honorariosCreditNoteConceptAmount.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0
                    && penaltyCreditNoteConceptAmount.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
                loanCreditNote.setFullyUsedByInvoice(true);
            }

            final Set<FacturaElectronicaMensual> newCreditNoteDocuments = new HashSet<>();
            newCreditNoteDocuments.addAll(interestElectronicCns);
            newCreditNoteDocuments.addAll(mandatoryInsuranceElectronicCns);
            newCreditNoteDocuments.addAll(voluntaryInsuranceElectronicCns);
            newCreditNoteDocuments.addAll(honorariosElectronicCns);
            newCreditNoteDocuments.addAll(penaltyElectronicCns);

            if (!newCreditNoteDocuments.isEmpty()) {
                final int itemsCount = newCreditNoteDocuments.size();
                final BigDecimal totalImpuestoItem = newCreditNoteDocuments.stream().map(FacturaElectronicaMensual::getImpuesto_item)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                final BigDecimal porcentajeImpuestoItem = newCreditNoteDocuments.stream()
                        .filter(f -> Objects.nonNull(f.getPorcentaje_impuesto_item())).findFirst().orElse(new FacturaElectronicaMensual())
                        .getPorcentaje_impuesto_item();
                for (final FacturaElectronicaMensual facturaElectronicaMensualItem : newCreditNoteDocuments) {
                    facturaElectronicaMensualItem.setImpuesto(totalImpuestoItem);
                    facturaElectronicaMensualItem.setPorcentaje_impuesto(porcentajeImpuestoItem);
                    facturaElectronicaMensualItem.setTotal_unidades(String.valueOf(itemsCount));
                }
                this.facturaElectronicMensualRepository.saveAll(newCreditNoteDocuments);
                this.loanCreditNoteRepository.saveAndFlush(loanCreditNote);
            }
        }
    }

    private synchronized InvoiceGenerationResult generateInvoiceNumber(LoanCreditNote loanCreditNote) {
        final String productTypeName = loanCreditNote.getLoan().loanProduct().getProductType() != null
                ? loanCreditNote.getLoan().loanProduct().getProductType().getLabel()
                : "";
        final List<LoanProductParameterization> productParameterizations = this.productParameterizationRepository
                .findByProductType(productTypeName);
        if (productParameterizations.isEmpty()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.credit.note.product.parameterization.not.found",
                    "Product parameterization not found for product type: " + productTypeName);
        }
        final LoanProductParameterization loanProductParameterization = productParameterizations.get(0);
        final Long rangeStartNumber = loanProductParameterization.getRangeStartNumber();
        final Long creditNoteCounter = loanProductParameterization.getCreditNoteCounter();
        final Long rangeEndNumber = loanProductParameterization.getRangeEndNumber();
        final Long currentCounter = ObjectUtils.defaultIfNull(creditNoteCounter, 0L) + 1L;
        final String documentNumber = String.valueOf(rangeStartNumber + currentCounter);
        loanProductParameterization.setCreditNoteCounter(currentCounter);
        final AtomicLong itemCounter = new AtomicLong(0);
        loanProductParameterization.setCreditNoteCounter(currentCounter);
        if (currentCounter > rangeEndNumber) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.invoice.counter.exceeds.range.end.number",
                    String.format("Invoice counter exceeds the range end number: %s and product type: %s", rangeEndNumber,
                            loanProductParameterization.getProductType()));
        }
        this.productParameterizationRepository.saveAndFlush(loanProductParameterization);
        return new InvoiceGenerationResult(itemCounter, documentNumber, loanProductParameterization);
    }

    @lombok.Getter
    @lombok.RequiredArgsConstructor
    private static class InvoiceGenerationResult {

        private final AtomicLong itemCounter;
        private final String documentNumber;
        private final LoanProductParameterization loanProductParameterization;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class CreditNoteConceptAmount {

        private BigDecimal base;
        private BigDecimal vat;
        private BigDecimal accountedFor;

        public BigDecimal getBase() {
            return this.base != null ? this.base : BigDecimal.ZERO;
        }

        public BigDecimal getVat() {
            return this.vat != null ? this.vat : BigDecimal.ZERO;
        }

        public BigDecimal getAccountedFor() {
            return this.accountedFor != null ? this.accountedFor : BigDecimal.ZERO;
        }

        public void incrementAccountedForAmount(BigDecimal amount) {
            this.accountedFor = this.getAccountedFor().add(amount);
        }

        public BigDecimal getRemainingAmount() {
            return this.getBase().add(this.getVat()).subtract(this.getAccountedFor());
        }

        public BigDecimal determineBaseValue(final BigDecimal conceptAmountToBeUsed) {
            BigDecimal baseValue = BigDecimal.ZERO;
            if (this.getRemainingAmount().compareTo(conceptAmountToBeUsed) >= 0) {
                BigDecimal baseRatio = this.getBase().divide(this.getBase().add(this.getVat()), RoundingMode.HALF_UP);
                baseValue = baseRatio.multiply(conceptAmountToBeUsed);
            }
            return baseValue;
        }

        public BigDecimal determineVatValue(final BigDecimal conceptAmountToBeUsed) {
            BigDecimal vatValue = BigDecimal.ZERO;
            if (this.getRemainingAmount().compareTo(conceptAmountToBeUsed) >= 0) {
                BigDecimal vatRatio = this.getVat().divide(this.getBase().add(this.getVat()), RoundingMode.HALF_UP);
                vatValue = vatRatio.multiply(conceptAmountToBeUsed);
            }
            return vatValue;
        }
    }
}
