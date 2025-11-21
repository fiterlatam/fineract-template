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
package org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Base64;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromissoryNoteTemplateSix {

    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanRepository loanRepository;

    public String generatePdf(String json) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        return generate(object);
    }

    private String generate(JsonObject object) {

        final Long loanId = object.get("loanId").getAsLong();
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es"));
        final LocalDate date = DateUtils.getBusinessLocalDate();

        MonetaryCurrency currency = null;

        LoanAccountData loanAccountData = this.loanReadPlatformService.retrieveOne(loanId);
        Loan loan = loanRepository.findById(loanId).get();
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
        String clientDpiText = getNumber(Long.valueOf(loan.getClient().getDpiNumber()), false, true);
        String clientDpiNumber = loan.getClient().getDpiNumber();
        String clientAddress = loan.getClient().getClientContactInformation().getReferenceHousingData();
        String creditAmountText = getNumber(loan.getApprovedPrincipal(), true, true);
        String creditPurpose = loan.getLoanPurpose() != null ? loan.getLoanPurpose().getDescription() : "";

        // SECOND PARAGRAPH
        String termText = getNumber(loan.getTermFrequency(), true, true);
        String disbursementDate = loan.getDisbursementDate().format(fmt).toUpperCase();
        String secondTermText = termText;
        String numberEqualsQuotas = String.valueOf(numerPayments.get());
        String quotaAmount = getNumber(firstPaymentAmount, true, true);
        String numberLastQuota = optLast.map(loanSchedulePeriodData -> loanSchedulePeriodData.getInstallmentNumber().toString()).orElse("");
        String lastQuotaAmount = optLast.isPresent()
                ? getNumber(optLast.get().getTotalPrincipalAndInterest(currency).getAmount(), true, true)
                : "";
        String paymentDay = optLast.isPresent() ? getNumber(optLast.get().getDueDate().getDayOfMonth(), true, true) : "";

        // THIRD PARAGRAPH
        String interestRateText = getNumber(loan.getLoanProductRelatedDetail().getAnnualNominalInterestRate(), true, true);

        // LAST PARAGRAPH
        String witnessName = object.get("witnessName").getAsString();
        String witnessDpiText = getNumber(object.get("witnessDPI").getAsNumber(), false, true);
        String witnessDpiNumber = "(" + object.get("witnessDPI").getAsString() + ")";
        String department = loan.getPrequalificationGroup() != null && loan.getPrequalificationGroup().getAgency() != null
                && loan.getPrequalificationGroup().getAgency().getCountry() != null
                        ? loan.getPrequalificationGroup().getAgency().getCountry().getDescription()
                        : "";

        // GUARANTOR DATA
        String guarantorName = object.get("guarantorName").getAsString();
        String guarantorDPI = "(" + object.get("guarantorDPI").getAsString() + ")";
        String guarantorDPIText = getNumber(object.get("guarantorDPI").getAsNumber(), false, true);
        String guarantorAddress = object.get("guarantorAddress").getAsString();
        Document document = new Document();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {

            PdfWriter writer = PdfWriter.getInstance(document, baos);

            URL url = getClass().getResource("/img/fb_logo_flat.png");
            Image logo = Image.getInstance(url);
            logo.scaleToFit(150, 50);

            logo.scaleToFit(150, 50);

            writer.setPageEvent(new PromissoryNoteTemplateSix.HeaderFooterEvent(logo));

            document.setMargins(50, 50, 100, 50);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);

            // Título
            Paragraph title = new Paragraph("PAGARÉ LIBRE DE PROTESTO", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Cuerpo completo del pagaré (texto legal completo con variables)
            String bodyText = String.format(
                    """
                            Yo: %s, me identifico con Documento Personal de Identificación (DPI) %s, (%s), extendido por el Registro Nacional de las Personas de la República de Guatemala en adelante me denominaré simple e indistintamente "la parte promitente deudora y/o libradora), y señalo como lugar para recibir comunicaciones y/o notificaciones que para efecto de este título será domicilio especial el siguiente: %s.

                            Manifiesto que, por el presente PAGARÉ libre de protesto, prometo pagar incondicionalmente a la orden o endoso de "THE FRIENDSHIP BRIDGE" en adelante llamado "THE FRIENDSHIP BRIDGE" y/o "Beneficiaria o tenedora" la suma total de: %s; por lo cual me declaro lisa y llana deudora de "THE FRIENDSHIP BRIDGE" y declaro que utilizaré este financiamiento para %s que se detalla así: %S. El pago de la referida suma lo haré bajo los siguientes términos:

                            A) DEL PLAZO Y FORMA DE PAGO:
                            Me obligo a pagar la referida suma de este título en el plazo de %s a contar del %s, cantidad que pagaré sin necesidad de previo cobro o requerimiento, mediante el pago de cuotas mensuales y sucesivas las cuales son %s, las primeras %s y la número %s de %s, mismas que se harán efectivas el %s hábil bancario de cada mes calendario o el inmediato posterior si ese día fuere inhábil bancario. Todo pago lo haré en las oficinas de THE FRIENDSHIP BRIDGE conocidas por mi persona.

                            B) INTERESES:
                            Sobre la suma o capital total que he prometido incondicionalmente pagar, reconozco que se incluye un cargo total de intereses calculado bajo una %s sobre saldos mensual. Esta tasa de interés efectiva en la presente operación corresponderá exactamente con la tasa de interés nominal anteriormente indicada y aceptada siempre y cuando los abonos mencionados se realicen en la forma y tiempo aquí establecido. Las cuotas de interés están incluidas en las cuotas o amortizaciones anteriormente mencionadas, dado que el pago es mediante la modalidad de cuota nivelada. Los intereses compensan los servicios que me presta la institución ya que este préstamo incluye servicios adicionales como los de capacitación y otros.

                            C) ACEPTACIÓN Y OBLIGACIÓN DE LA PARTE DEUDORA:
                            a. Acepto que la parte tenedora de este título, podrá dar por vencido el plazo de este título en forma anticipada y exigir ejecutivamente el pago total del saldo adeudado tanto de capital, intereses, intereses moratorios gastos y costas judiciales en los siguientes casos:
                            a.1) Si no cumplo cualquiera de las obligaciones aquí contraídas
                            a.2) Si se dictare mandamiento de embargo en mi contra y/o avalista si lo hubiese (s);
                            a.3) Si dejare de pagar puntualmente una sola de las cuotas convenidas; y
                            a.4) Si THE FRIENDSHIP BRIDGE comprobare que utilicé el financiamiento para fines distintos a los antes mencionados.

                            b. Renuncio al fuero de mi respectivo domicilio; me someto y sujeto a la jurisdicción y tribunales que elija y pueda utilizar a su elección la parte tenedora de este título, y para el caso de ejecución, me acojo al procedimiento establecido en el Código Procesal Civil y Mercantil y Código de comercio.

                            c. Acepto como buenas y exactas las cuentas que la parte tenedora de este título formule acerca de este título y como líquido, exigible y de plazo vencido la cantidad que se exija.

                            d. Acepto que se tengan como válidas y bien hechas las comunicaciones y/o notificaciones que se realicen y/o dirijan al lugar indicado como domicilio especial, a no ser que comunique y/o notifique por escrito a THE FRIENDSHIP BRIDGE, de cualquier cambio en la misma y que obre en su poder.

                            e. Acepto que todo el gasto por cobranza es por mi cuenta, y en concepto de "Gastos Administrativos por desembolso" no pagaré cantidad alguna, dada la exoneración de gastos de desembolso del 2.5%% sobre el monto otorgado, realizado por el beneficiario. En caso de atraso en el pago de una o más cuotas sucesivas reconozco que THE FRIENDSHIP BRIDGE cobrará como "interés moratorio" o cuota moratoria, una suma calculada así: el monto de capital vencido multiplicado por una tasa mensual de seis por ciento (6%%).

                            f. CANCELACIÓN ANTICIPADA: Acepto que podré cancelar de manera anticipada el monto total de la deuda únicamente si tengo pagado al menos el cincuenta por ciento (50%%) de las cuotas del crédito vigente, de lo contrario se me penalizará con el tres por ciento (3%%) sobre el capital adeudado. h. Acepto que para el caso de ejecución, THE FRIENDSHIP BRIDGE no está obligado a prestar fianza o garantía alguna, exoneración que se hará extensiva a los depositarios e interventores nombrados, no quedando THE FRIENDSHIP BRIDGE responsable por las actuaciones de estos y que para el caso de remate sirva de base el valor de los bienes embargados o el monto total de la demanda incluyendo intereses y costas a elección de THE FRIENDSHIP BRIDGE, garantizando la presente obligación con todos mis bienes presentes y futuros; i. Acepto que este título es cedible o negociable, mediante simple endoso, sin necesidad previa o posterior aviso o notificación;

                            h. Renuncio expresamente a los derechos que pudieren conferirme las leyes vigentes o que en el futuro entraren en vigor y que pudieran permitirme cumplir las obligaciones contraídas en este documento en forma distinta a la pactada. Como deudor declaro que estoy plenamente enterado de todas y cada uno de los términos de este pagaré, lo acepto, ratifico y firmo por no saber firmar dejo impresa la huella de mi pulgar derecho firmando como testigo al señor %s, quien es persona capaz e idónea y se identifica con Documento Personal de Identificación (DPI) %s, %s, extendido por el Registro Nacional de las Personas de la República de Guatemala.

                            Lugar y fecha de emisión: Municipio y Departamento de %s, %s de %s, del año dos mil %s.
                            """,
                    clientName, clientDpiText, clientDpiNumber, clientAddress, creditAmountText, creditPurpose, "detalle", termText,
                    disbursementDate, secondTermText, numberEqualsQuotas + " " + quotaAmount, numberLastQuota, lastQuotaAmount, paymentDay,
                    interestRateText, witnessName, witnessDpiText, witnessDpiNumber, department, date.getDayOfMonth(),
                    date.getMonth().getDisplayName(TextStyle.FULL, new Locale("es")), date.getYear() - 2000);
            Paragraph body = new Paragraph(bodyText, normalFont);
            body.setAlignment(Element.ALIGN_JUSTIFIED);
            body.setSpacingAfter(20f);
            document.add(body);

            // Firmas
            document.add(createSignatureSection(clientName, null, "Promitente deudora o libradora", null));

            String avalText = String.format(
                    """


                            AVALISTA

                            YO: %s, me identifico con Documento Personal de Identificación (DPI) %s, (%s), extendido por el Registro Nacional de las Personas de la República de Guatemala en adelante me denominaré simple e indistintamente "la parte promitente deudora y/o libradora), y señalo como lugar para recibir comunicaciones y/o notificaciones que para efecto de este título será domicilio especial el siguiente: %s obligándome a comunicar de inmediato a THE FRIENDSHIP BRIDGE cualquier cambio; la prueba de dicha comunicación corre a mi cargo, aceptando para el caso de no dar este aviso como válida cualquier notificación que se me haga llegar en la dirección antes señalada. Y por no saber firmar dejo impresa la huella de mi pulgar derecho firmando como testigo al señor %s, quien es persona capaz e idónea y se identifica con Documento Personal de identificación (DPI) %s, %s, extendido por el Registro Nacional de Personas de la República de Guatemala.

                            Lugar y fecha de emisión: Municipio y Departamento de %s, %s de %s, del año dos mil %s.
                            """,
                    guarantorName, guarantorDPIText, guarantorDPI, guarantorAddress, witnessName, witnessDpiText, witnessDpiNumber,
                    department, date.getDayOfMonth(), date.getMonth().getDisplayName(TextStyle.FULL, new Locale("es")),
                    date.getYear() - 2000);
            Paragraph bodyAval = new Paragraph(avalText, normalFont);
            bodyAval.setAlignment(Element.ALIGN_JUSTIFIED);
            bodyAval.setSpacingAfter(20f);
            document.add(bodyAval);

            document.add(createSignatureSection(guarantorName, witnessName, "Aval", "Testigo"));

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
        emptyCell.setBorder(Rectangle.NO_BORDER);
        emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font font = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
        if (clientName != null) {
            PdfPCell cell1 = new PdfPCell(
                    new Paragraph("F. _________________________________\n\n" + clientName.toUpperCase() + "\n\n" + subtitleOne, font));
            cell1.setBorder(Rectangle.NO_BORDER);
            cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell1);

        } else {

            table.addCell(emptyCell);
        }
        if (witnessName != null) {
            PdfPCell cell2 = new PdfPCell(
                    new Paragraph("F. _________________________________\n\n" + witnessName.toUpperCase() + "\n\n" + subtitleTwo, font));
            cell2.setBorder(Rectangle.NO_BORDER);
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

        public HeaderFooterEvent(Image logo) {
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
                        new Phrase("THE FRIENDSHIP BRIDGE", FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY)),
                        document.right(), document.bottom() - 20, 0);
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        }
    }

    private String getNumber(Number number, boolean parentheses, Boolean uppercase) {

        RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(new Locale("es"), RuleBasedNumberFormat.SPELLOUT);
        BigDecimal bd = new BigDecimal(number.toString()).setScale(2, RoundingMode.HALF_UP);
        String value = uppercase ? rbnf.format(bd).toUpperCase() : rbnf.format(bd);
        String numericValue = bd.stripTrailingZeros().scale() <= 0 ? bd.toBigInteger().toString() : bd.toPlainString();

        if (parentheses) {
            return "( " + value + " " + numericValue + " )";
        }

        return value;
    }

}
