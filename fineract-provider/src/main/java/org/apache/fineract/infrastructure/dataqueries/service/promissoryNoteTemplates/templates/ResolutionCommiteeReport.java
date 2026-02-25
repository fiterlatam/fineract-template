/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.templates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResolutionCommiteeReport {

    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanRepository loanRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final Color PURPLE = new Color(230, 0, 230);
    private static final Color LIGHT_GRAY = new Color(240, 240, 240);
    private static final Color VALUE_YELLOW = new Color(255, 247, 204);
    private static final Color VALUE_GRAY = new Color(232, 232, 232);
    private static final Color VALUE_WHITE = new Color(255, 255, 255);

    public byte[] generatePdf(String json) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        Long loanId = object.get("R_loanId").getAsLong();
        List<Map<String, Object>> data = retrieveData(loanId, object.get("R_prequalificationId").getAsLong());
        return generate(data, loanId);
    }

    private byte[] generate(List<Map<String, Object>> data, Long loanId) {

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Map<String, String> columNames = getTemplateLabels();

        MonetaryCurrency currency = this.loanRepository.findById(loanId).get().getCurrency();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            // =========================
            // HEADER (3 columnas)
            // =========================

            PdfPTable header = new PdfPTable(3);
            header.setWidthPercentage(100);
            header.setWidths(new float[] { 3, 4, 3 });

            // LOGO
            URL url = getClass().getResource("/img/fb_logo_flat.png");
            Image logo = Image.getInstance(url);
            logo.scaleToFit(120, 60);

            PdfPCell logoCell = new PdfPCell(logo);
            logoCell.setBorder(Rectangle.NO_BORDER);
            header.addCell(logoCell);

            // CENTRO (facilitador + código)
            PdfPTable centerTable = new PdfPTable(4);
            centerTable.setWidthPercentage(100);

            addCell(centerTable, columNames.get("facilitatorName"), labelFont, VALUE_WHITE, Rectangle.NO_BORDER, 2);
            addCell(centerTable, data.get(0).get("facilitatorName"), valueFont, VALUE_WHITE, Rectangle.NO_BORDER, 2);

            addCell(centerTable, columNames.get("clientCode"), labelFont, VALUE_WHITE, Rectangle.NO_BORDER, 2);
            addCell(centerTable, data.get(0).get("clientCode"), valueFont, VALUE_YELLOW, Rectangle.NO_BORDER, 2);
            addCell(centerTable, "", labelFont, VALUE_WHITE, Rectangle.NO_BORDER, 2);
            addCell(centerTable, "", valueFont, VALUE_WHITE, Rectangle.NO_BORDER, 2);

            PdfPCell centerCell = new PdfPCell(centerTable);
            centerCell.setBorder(Rectangle.NO_BORDER);
            header.addCell(centerCell);

            // AGENCIA
            PdfPTable rightTable = new PdfPTable(3);
            rightTable.setWidthPercentage(100);

            addCell(rightTable, columNames.get("agencyName"), labelFont, VALUE_WHITE, Rectangle.NO_BORDER, 1);
            addCell(rightTable, data.get(0).get("agencyName"), valueFont, VALUE_GRAY, Rectangle.NO_BORDER, 2);
            addCell(rightTable, "", labelFont, VALUE_WHITE, Rectangle.NO_BORDER, 3);

            PdfPCell rightCell = new PdfPCell(rightTable);
            rightCell.setBorder(Rectangle.NO_BORDER);
            header.addCell(rightCell);

            document.add(header);
            document.add(Chunk.NEWLINE);

            // =========================
            // TITLE BAR
            // =========================

            PdfPCell titleCell = new PdfPCell(new Phrase(columNames.get("title"), titleFont));
            titleCell.setBackgroundColor(PURPLE);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setPadding(8);
            titleCell.setBorder(Rectangle.NO_BORDER);

            PdfPTable titleTable = new PdfPTable(1);
            titleTable.setWidthPercentage(100);
            titleTable.addCell(titleCell);

            document.add(titleTable);
            document.add(Chunk.NEWLINE);

            // =========================
            // TABLA PRINCIPAL (4 columnas)
            // =========================

            PdfPTable mainTable = new PdfPTable(4);
            mainTable.setWidthPercentage(100);
            mainTable.setWidths(new float[] { 3, 2, 3, 2 });

            addStyledCell(mainTable, columNames.get("clientName"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("clientName"), valueFont, VALUE_YELLOW, 3);

            addStyledCell(mainTable, columNames.get("loanAmount"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, Money.of(currency, (BigDecimal) data.get(0).get("loanAmount")).getAmount(), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("loanTerm"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("loanTerm"), valueFont, LIGHT_GRAY, 1);

            addStyledCell(mainTable, columNames.get("installmentAmount"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, Money.of(currency, (BigDecimal) data.get(0).get("installmentAmount")).getAmount(), valueFont,
                    LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("loanPurpose"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("loanPurpose"), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("frequencyTerm"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("frequencyTerm"), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("interestMethod"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("interestMethod"), valueFont, LIGHT_GRAY, 1);

            addStyledCell(mainTable, columNames.get("equivalentAnnualRate"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("equivalentAnnualRate"), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("annualInterest"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("annualInterest"), valueFont, LIGHT_GRAY, 1);

            addStyledCell(mainTable, columNames.get("expenseAdmin"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("registeredMortgage"), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("frequencyTermInt"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("frequencyTerm"), valueFont, LIGHT_GRAY, 1);

            addStyledCell(mainTable, columNames.get("frequencyTermCap"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("frequencyTerm"), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("collateralType"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("collateralType"), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("collateral"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("collateral"), valueFont, VALUE_YELLOW, 1);
            addStyledCell(mainTable, columNames.get("collateralCoverage"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("collateralCoverage"), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("collateralValue"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("collateralValue"), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("registeredMortgage"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("registeredMortgage"), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("program"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("program"), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("sector"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("collateralType"), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("formalizationDocument"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, "", valueFont, LIGHT_GRAY, 3);

            addStyledCell(mainTable, columNames.get("approvalDate"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("approvalDate"), valueFont, VALUE_YELLOW, 1);
            addStyledCell(mainTable, columNames.get("emitionDate"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("approvalDate"), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("equivalentMonthlyRate"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("equivalentMonthlyRate"), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("nominalMonthlyRate"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("nominalMonthlyRate"), valueFont, LIGHT_GRAY, 1);

            document.add(mainTable);

            document.add(Chunk.NEWLINE);

            PdfPCell titleCellTwo = new PdfPCell(new Phrase(columNames.get("observationTitle"), titleFont));
            titleCellTwo.setBackgroundColor(PURPLE);
            titleCellTwo.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCellTwo.setPadding(8);
            titleCellTwo.setBorder(Rectangle.NO_BORDER);

            PdfPTable titleTableTwo = new PdfPTable(1);
            titleTableTwo.setWidthPercentage(100);
            titleTableTwo.addCell(titleCellTwo);

            document.add(titleTableTwo);
            document.add(Chunk.NEWLINE);

            PdfPCell titleCellThree = new PdfPCell(new Phrase(columNames.get("analysisInformation"), valueFont));
            titleCellThree.setBackgroundColor(VALUE_WHITE);
            titleCellThree.setHorizontalAlignment(Element.ALIGN_LEFT);
            titleCellThree.setPadding(8);
            titleCellThree.setBorder(Rectangle.NO_BORDER);

            PdfPTable titleTableThree = new PdfPTable(1);
            titleTableThree.setWidthPercentage(100);
            titleTableThree.addCell(titleCellThree);

            document.add(titleTableThree);
            document.add(Chunk.NEWLINE);

            for (Map<String, Object> item : data) {

                PdfPTable logsTable = new PdfPTable(4);
                logsTable.setWidthPercentage(100);
                logsTable.setWidths(new float[] { 2, 2, 2, 2 });

                addStyledCell(logsTable, "", labelFont, VALUE_WHITE, 4);
                addStyledCell(logsTable, "", labelFont, VALUE_WHITE, 4);

                addStyledCell(logsTable, columNames.get("committeeLevel"), labelFont, LIGHT_GRAY, 1);
                addStyledCell(logsTable, item.get("commiteeLevel"), valueFont, LIGHT_GRAY, 3);
                addStyledCell(logsTable, columNames.get("updatedBy"), labelFont, LIGHT_GRAY, 1);
                addStyledCell(logsTable, item.get("updatedBy"), valueFont, LIGHT_GRAY, 3);
                addStyledCell(logsTable, columNames.get("logDate"), labelFont, LIGHT_GRAY, 1);
                addStyledCell(logsTable, item.get("logDate"), valueFont, LIGHT_GRAY, 3);

                document.add(logsTable);
            }

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addCell(PdfPTable table, Object text, Font font, Color bg, int border, int spacing) {

        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text.toString() : "", font));

        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setBorder(border);
        cell.setColspan(spacing);
        table.addCell(cell);
    }

    private void addStyledCell(PdfPTable table, Object text, Font font, Color bg, int spacing) {

        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text.toString() : "", font));

        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setColspan(spacing);
        table.addCell(cell);
    }

    private List<Map<String, Object>> retrieveData(Long loanId, Long prequalificationId) {
        String sql = """
                SELECT mc.display_name as clientName,
                ml.principal_amount as loanAmount,
                agency.name as agencyName,
                mc.dpi as clientCode,
                (CASE
                WHEN psl.to_status =1006 then 'COMMITTEE A'
                WHEN psl.to_status =1005 then 'COMMITTEE B'
                WHEN psl.to_status =1004 then 'COMMITTEE C'
                WHEN psl.to_status =1003 then 'COMMITTEE D'
                ELSE 'INVÁLIDO' END) as commiteeLevel,
                COALESCE(((mpl.required_guarantee_percent/100)* pgm.approved_amount),'N/A') as collateral,
                COALESCE((pgm.approved_amount/((mpl.required_guarantee_percent/100)* pgm.approved_amount)),'N/A') as collateralCoverage,
                'N/A' as registeredMortgage,
                'PUENTE al Éxito' as program,
                (select date_created from m_prequalification_status_log where prequalification_id=? and to_status=900 ) as approvalDate,
                lrs.equivalentAnnualRate,lrs.equivalentMonthlyRate,lrs.installmentAmount,
                (CASE
                WHEN ml.repayment_period_frequency_enum =0 then concat(ml.repay_every,' ', (CASE WHEN ml.repay_every>1 then 'DÍAS' ELSE 'DÍA' END))
                WHEN ml.repayment_period_frequency_enum =1 then concat(ml.repay_every,' ',(CASE WHEN ml.repay_every>1 then 'SEMANAS' ELSE 'SEMANA' END))
                WHEN ml.repayment_period_frequency_enum =2 then concat(ml.repay_every,' ', (CASE WHEN ml.repay_every>1 then 'MESES' ELSE 'MES' END))
                WHEN ml.repayment_period_frequency_enum =3 then concat(ml.repay_every,' ',(CASE WHEN ml.repay_every>1 then 'AÑOS' ELSE 'AÑO' END))
                WHEN ml.repayment_period_frequency_enum =4 then concat(ml.repay_every,' ','TÉRMINO COMPLETO')
                ELSE 'INVÁLIDO' END) as frequencyTerm,
                (CASE
                WHEN ml.term_period_frequency_enum =0 then concat(ml.term_frequency,' ', (CASE WHEN ml.term_frequency>1 then 'DÍAS' ELSE 'DÍA' END))
                WHEN ml.term_period_frequency_enum =1 then concat(ml.term_frequency,' ',(CASE WHEN ml.term_frequency>1 then 'SEMANAS' ELSE 'SEMANA' END))
                WHEN ml.term_period_frequency_enum =2 then concat(ml.term_frequency,' ', (CASE WHEN ml.term_frequency>1 then 'MESES' ELSE 'MES' END))
                WHEN ml.term_period_frequency_enum =3 then concat(ml.term_frequency,' ',(CASE WHEN ml.term_frequency>1 then 'AÑOS' ELSE 'AÑO' END))
                WHEN ml.term_period_frequency_enum =4 then concat(ml.term_frequency,' ','TÉRMINO COMPLETO')
                ELSE 'INVÁLIDO' END) as loanTerm,
                (case when ml.interest_method_enum = 0 then 'Saldo decreciente' else 'Plano' END) as interestMethod,
                (ml.annual_nominal_interest_rate/12) as nominalMonthlyRate,
                ml.annual_nominal_interest_rate as annualInterest,
                COALESCE(mccv.code_value,'N/A' ) as collateralType,
                COALESCE(mlc.value,'N/A' )  as collateralValue,
                'N/A' as registeredMortgage,
                COALESCE(lad.destino_prestamo,'N/A' )as loanPurpose,
                psl.date_created as logDate,
                concat(au.username,' - ',au.firstname,' ', au.lastname, ' ') as updatedBy,
                concat(fc.username,' - ',fc.firstname,' ', fc.lastname, ' ') as facilitatorName,
                ml.id as loanId
                FROM m_prequalification_status_log psl
                JOIN m_prequalification_group pg on pg.id = psl.prequalification_id
                JOIN m_prequalification_group_members pgm ON pgm.group_id=pg.id
                JOIN m_client mc on mc.dpi=pgm.dpi
                left join m_loan ml on ml.prequalification_id = pg.id and ml.client_id = mc.id
                left join m_product_loan mpl on mpl.id = ml.product_id
                left join m_client_loan_additional_properties lad on lad.loan_id=ml.id
                left join m_loan_collateral mlc on mlc.loan_id = ml.id
                left join m_code_value mccv on mccv.id = mlc.type_cv_id
                left join m_group_client mgc on mgc.client_id = mc.id
                left join m_group mg on mg.id = mgc.group_id
                left join m_group center on center.id = mg.parent_id
                left join m_portfolio portfolio on portfolio.id = center.portfolio_id
                left join m_supervision supv on supv.id = portfolio.supervision_id
                left join m_agency agency on agency.id = supv.agency_id
                left join m_appuser au on au.id = psl.assigned_to
                left join m_appuser fc on fc.id = pg.facilitator
                left join (select lrs.loan_id , SUM(
                  COALESCE(lrs.principal_amount, 0) +
                  COALESCE(lrs.interest_amount, 0) +
                  COALESCE(lrs.fee_charges_amount, 0) +
                  COALESCE(lrs.penalty_charges_amount, 0)
                ) AS installmentAmount,
                (((((SUM(
                  COALESCE(lrs.principal_amount, 0) +
                  COALESCE(lrs.interest_amount, 0) +
                  COALESCE(lrs.fee_charges_amount, 0) +
                  COALESCE(lrs.penalty_charges_amount, 0)
                ) * ml.term_frequency)-ml.principal_amount )/ml.principal_amount)/ml.term_frequency)*12) as equivalentAnnualRate,
                (((((SUM(
                  COALESCE(lrs.principal_amount, 0) +
                  COALESCE(lrs.interest_amount, 0) +
                  COALESCE(lrs.fee_charges_amount, 0) +
                  COALESCE(lrs.penalty_charges_amount, 0)
                ) * ml.term_frequency)-ml.principal_amount )/ml.principal_amount)/ml.term_frequency)) as equivalentMonthlyRate
                from m_loan_repayment_schedule lrs inner join m_loan ml on ml.id=lrs.loan_id where lrs.loan_id = ? AND lrs.installment=1) as lrs on lrs.loan_id = ml.id
                where pg.id = ? and psl.to_status in (1003,1004,1005,1006) order by psl.id desc
                """;
        return this.jdbcTemplate.queryForList(sql, prequalificationId, loanId, prequalificationId);
    }

    private Map<String, String> getTemplateLabels() {
        String sql = "SELECT text FROM m_template WHERE id = 1";
        String json = jdbcTemplate.queryForObject(sql, String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing template JSON", e);
        }
    }

}
