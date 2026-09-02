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
package org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.templates;

import com.google.common.base.Splitter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.dataqueries.domain.PromissoryNoteTemplate;
import org.apache.fineract.infrastructure.dataqueries.domain.PromissoryNoteTemplateRepository;
import org.apache.fineract.organisation.agency.data.AgencyData;
import org.apache.fineract.organisation.agency.service.AgencyReadPlatformService;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromissoryNoteTemplateFour {

    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanRepository loanRepository;
    private final PromissoryNoteTemplateRepository promissoryNoteTemplateRepository;
    private final AgencyReadPlatformService agencyReadPlatformService;

    public String generatePdf(String json) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        return generate(object);
    }

    private String generate(JsonObject object) {

        final Long loanId = object.get("loanId").getAsLong();
        final Long agencyId = object.get("agencyId").getAsLong();
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es"));
        final LocalDate date = DateUtils.getBusinessLocalDate();

        MonetaryCurrency currency = null;

        LoanAccountData loanAccountData = this.loanReadPlatformService.retrieveOne(loanId);
        Loan loan = loanRepository.findById(loanId).get();
        final PromissoryNoteTemplate template = promissoryNoteTemplateRepository.findByPromissoryNumber(4L);
        currency = loan.getCurrency();

        AtomicReference<Integer> numerPayments = new AtomicReference<>(0);

        Optional<LoanRepaymentScheduleInstallment> opt = loan.getRepaymentScheduleInstallments().stream()
                .sorted(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber))
                .filter(item -> item.getInstallmentNumber() == 1).findFirst();

        BigDecimal firstPaymentAmount = opt.isPresent() ? opt.get().getTotalPrincipalAndInterest(currency).getAmount() : BigDecimal.ZERO;

        Optional<LoanRepaymentScheduleInstallment> optLast = loan.getRepaymentScheduleInstallments().stream()
                .max(Comparator.comparing(LoanRepaymentScheduleInstallment::getInstallmentNumber));

        loan.getRepaymentScheduleInstallments().forEach(period -> {
            if (firstPaymentAmount.compareTo(period.getTotalPrincipalAndInterest(period.getLoan().getCurrency()).getAmount()) == 0) {
                numerPayments.getAndSet(numerPayments.get() + 1);
            }
        });

        // FIRST PARAGRAPH
        String clientName = loanAccountData.getClientName();
        String clientDpiText = getNumber(Long.valueOf(loan.getClient().getDpiNumber()), false, false, true);
        String clientDpiNumber = loan.getClient().getDpiNumber();
        String clientAddress = loanRepository.retrieveAddressByLoanId(loanId);
        String creditAmountText = MoneyHelper.getMoneyString(loan.getApprovedPrincipal()).toUpperCase();
        String creditPurpose = loanRepository.retrieveLoanPurposeCodeByLoanId(loanId);
        String creditDetail = loanRepository.retrieveLoanDetailPurposeByLoanId(loanId);

        // SECOND PARAGRAPH
        String termText = getNumber(loan.getTermFrequency(), true, false, false);
        String disbursementDate = DateUtils.getDateInLetters(loan.getDisbursementDate());
        String secondTermText = termText;
        String numberEqualsQuotas = getNumber(numerPayments.get(), true, false, false);
        String quotaAmount = MoneyHelper.getMoneyString(firstPaymentAmount).toUpperCase();
        String numberLastQuota = getNumber(optLast.map(LoanRepaymentScheduleInstallment::getInstallmentNumber).orElse(0), true, false,
                false);
        String lastQuotaAmount = optLast.isPresent()
                ? MoneyHelper.getMoneyString(optLast.get().getTotalPrincipalAndInterest(currency).getAmount()).toUpperCase()
                : "";
        String paymentDay = optLast.isPresent() ? getNumber(optLast.get().getDueDate().getDayOfMonth(), true, false, false) : "";

        // THIRD PARAGRAPH
        String interestRateText = getNumber(
                loan.getLoanProductRelatedDetail().getAnnualNominalInterestRate().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_EVEN),
                true, true, false);

        // LAST PARAGRAPH
        AgencyData agencyData = this.agencyReadPlatformService.findById(agencyId);

        String municipio = agencyData != null && agencyData.getCity() != null ? agencyData.getCity().getName() : "__________";

        String department = agencyData != null && agencyData.getState() != null ? agencyData.getState().getName() : "__________";
        // GUARANTOR DATA
        Object[] dataGuarantor = this.loanRepository.retrieveGuarantorDataByLoanId(loanId);
        Object[] data = null;
        if (dataGuarantor.length == 0) {
            List<ApiParameterError> list = new ArrayList<>();
            ApiParameterError apiParameterError = ApiParameterError.parameterError("err.msg.does.not.complete",
                    "The loan does not contains guarantor data", "loanId");
            list.add(apiParameterError);
            throw new PlatformApiDataValidationException("err.msg.does.not.complete", "The loan does not contains guarantor data", list);
        } else {
            data = (Object[]) dataGuarantor[0];
        }
        String guarantorName = data[0] != null ? data[0].toString() : "";
        String guarantorDPI = data[1] != null ? data[1].toString() : "";
        String guarantorDPIText = getNumber(Long.parseLong(guarantorDPI), false, false, true);
        String guarantorAddress = data[2] != null ? data[2].toString() : "";
        Document document = new Document();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {

            PdfWriter writer = PdfWriter.getInstance(document, baos);

            URL url = getClass().getResource("/img/fb_logo_flat.png");
            Image logo = Image.getInstance(url);
            logo.scaleToFit(150, 50);

            logo.scaleToFit(150, 50);

            writer.setPageEvent(new PromissoryNoteTemplateFour.HeaderFooterEvent(logo));

            document.setMargins(50, 50, 100, 50);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

            // Título
            Paragraph title = new Paragraph(template.getTitle(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Cuerpo completo del pagaré (texto legal completo con variables)
            String bodyText = String.format(template.getBlockOne(), clientName, clientDpiText, clientDpiNumber, clientAddress,
                    creditAmountText, creditPurpose, creditDetail, termText, disbursementDate, secondTermText,
                    numberEqualsQuotas + " de " + quotaAmount, numberLastQuota, lastQuotaAmount, paymentDay, interestRateText, municipio,
                    department, DateUtils.numberToLetters(date.getDayOfMonth()).toLowerCase(),
                    date.getMonth().getDisplayName(TextStyle.FULL, new Locale("es")),
                    DateUtils.numberToLetters(date.getYear() - 2000).toLowerCase());

            Paragraph body = new Paragraph(bodyText, normalFont);
            body.setAlignment(Element.ALIGN_JUSTIFIED);
            body.setSpacingAfter(20f);
            document.add(body);

            // Firmas
            document.add(createSignatureSection(clientName, null, "Promitente deudora o libradora", null));

            String avalText = String.format(template.getBlockTwo(), guarantorName, guarantorDPIText, guarantorDPI, guarantorAddress,
                    municipio, department, DateUtils.numberToLetters(date.getDayOfMonth()).toLowerCase(),
                    date.getMonth().getDisplayName(TextStyle.FULL, new Locale("es")),
                    DateUtils.numberToLetters(date.getYear() - 2000).toLowerCase());

            Paragraph bodyAval = new Paragraph(avalText, normalFont);
            bodyAval.setAlignment(Element.ALIGN_JUSTIFIED);
            bodyAval.setSpacingAfter(20f);
            document.add(bodyAval);

            document.add(createSignatureSection(guarantorName, null, "Aval", null));

            document.close();

            byte[] pdfBytes = baos.toByteArray();
            return "{\"pdfBase64\":\"" + Base64.getEncoder().encodeToString(pdfBytes) + "\"}";

        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return null;
    }

    private PdfPTable createSignatureSection(String clientName, String witnessName, String subtitleOne, String subtitleTwo) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(40f);

        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font font = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
        if (clientName != null) {
            PdfPCell cell1 = new PdfPCell(
                    new Paragraph("F. _________________________________\n\n" + clientName.toUpperCase() + "\n\n" + subtitleOne, font));
            cell1.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell1);

        } else {

            table.addCell(emptyCell);
        }
        if (witnessName != null) {
            PdfPCell cell2 = new PdfPCell(
                    new Paragraph("F. _________________________________\n\n" + witnessName.toUpperCase() + "\n\n" + subtitleTwo, font));
            cell2.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell2);
        } else {
            table.addCell(emptyCell);
        }

        return table;
    }

    // Header y Footer opcional
    private static class HeaderFooterEvent extends PdfPageEventHelper {

        private final Image logo;

        HeaderFooterEvent(Image logo) {
            this.logo = logo;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte cb = writer.getDirectContent();
                float x = document.right() - logo.getScaledWidth();
                float y = document.top() + 20;
                logo.setAbsolutePosition(x, y);
                cb.addImage(logo, false);

                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                        new Phrase("THE FRIENDSHIP BRIDGE", FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY)), document.right(),
                        document.bottom() - 20, 0);
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        }
    }

    private String getNumber(Number number, boolean parentheses, boolean percentage, boolean dpi) {

        RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(new Locale("es"), RuleBasedNumberFormat.SPELLOUT);
        BigDecimal bd = new BigDecimal(number.toString()).setScale(2, RoundingMode.HALF_UP);
        String value = rbnf.format(bd).toUpperCase();
        String numericValue = bd.stripTrailingZeros().scale() <= 0 ? bd.toBigInteger().toString() : bd.toPlainString();

        if (dpi && number.toString().matches("\\d{13}")) {
            value = "";
            String valueFormatted = number.toString().replaceFirst("(\\d{4})(\\d{5})(\\d{4})", "$1,$2,$3");
            Iterable<String> parts = Splitter.on(",").split(valueFormatted);
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                int leadingZeros = part.length() - part.replaceFirst("^0+", "").length();
                if (leadingZeros > 0) {
                    sb.append("CERO ".repeat(leadingZeros));
                }
                long parsed = Long.parseLong(part);
                if (parsed > 0) {
                    sb.append(rbnf.format(parsed));
                }
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
            value = sb.toString().toUpperCase();
        }

        if (parentheses) {
            return value + " (" + numericValue + (percentage ? "%" : "") + ")";
        }

        return value;
    }

}
