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
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
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

    private static final Color PURPLE = new Color(92, 6, 140);
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
            titleFont.setColor(LIGHT_GRAY);
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
            addStyledCell(mainTable, "Q. " + formatAmount(data.get(0).get("loanAmount")), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("loanTerm"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("loanTerm"), valueFont, LIGHT_GRAY, 1);

            addStyledCell(mainTable, columNames.get("installmentAmount"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, "Q. " + formatAmount(data.get(0).get("installmentAmount")), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("loanPurpose"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("loanPurpose"), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("frequencyTerm"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("frequencyTerm"), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("interestMethod"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("interestMethod"), valueFont, LIGHT_GRAY, 1);

            addStyledCell(mainTable, columNames.get("equivalentAnnualRate"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, formatAmountPercentage(data.get(0).get("equivalentAnnualRate")), valueFont, LIGHT_GRAY, 1);

            addStyledCell(mainTable, columNames.get("annualInterest"), labelFont, VALUE_WHITE, 1);
            Object annualInterest = data.get(0).get("annualInterest");
            // format as percentage with 2 decimals

            addStyledCell(mainTable, formatAmountPercentage(annualInterest), valueFont, LIGHT_GRAY, 1);

            addStyledCell(mainTable, columNames.get("expenseAdmin"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("expenseAdmin"), valueFont, LIGHT_GRAY, 1);

            addStyledCell(mainTable, columNames.get("monthlyInterest"), labelFont, VALUE_WHITE, 1);
            Object monthlyInterest = data.get(0).get("monthlyInterest");
            // format as percentage with 2 decimals

            addStyledCell(mainTable, formatAmountPercentage(monthlyInterest), valueFont, LIGHT_GRAY, 1);

            addStyledCell(mainTable, columNames.get("frequencyTermInt"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("frequencyTerm"), valueFont, LIGHT_GRAY, 1);

            addStyledCell(mainTable, columNames.get("frequencyTermCap"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("frequencyTerm"), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("collateralType"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("collateralType"), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("collateral"), labelFont, VALUE_WHITE, 1);
            Object collateralObj = data.get(0).get("collateral");
            addStyledCell(mainTable, collateralObj, valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("collateralCoverage"), labelFont, VALUE_WHITE, 1);
            Object collateralCoverage = data.get(0).get("collateralCoverage");
            addStyledCell(mainTable, formatAmountPercentage(collateralCoverage), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("collateralValue"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, "Q. " + formatAmount(data.get(0).get("collateralValue")), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("registeredMortgage"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("registeredMortgage"), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("program"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("program"), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, columNames.get("sector"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("sector"), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("formalizationDocument"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("formalizationDocument"), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("approvalDate"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("approvalDate"), valueFont, VALUE_YELLOW, 1);
            addStyledCell(mainTable, columNames.get("emitionDate"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, data.get(0).get("approvalDate"), valueFont, VALUE_YELLOW, 1);

            addStyledCell(mainTable, columNames.get("equivalentMonthlyRate"), labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, formatAmountPercentage(data.get(0).get("equivalentMonthlyRate")), valueFont, LIGHT_GRAY, 1);
            addStyledCell(mainTable, "", labelFont, VALUE_WHITE, 1);
            addStyledCell(mainTable, "", valueFont, LIGHT_GRAY, 1);

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

            String label = columNames.get("analysisInformation");
            String value = data.get(0).get("analysisInformation") != null ? data.get(0).get("analysisInformation").toString() : "N/A";

            Paragraph paragraph = new Paragraph();
            paragraph.add(new Chunk(label + "\n", labelFont));
            paragraph.add(new Chunk(value, valueFont));

            PdfPCell titleCellThree = new PdfPCell(paragraph);
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

    private Object formatAmountPercentage(Object annualInterest) {
        String percentageString = "0.00%";
        if (NumberUtils.isCreatable(String.valueOf(annualInterest))) {
            BigDecimal bd = new BigDecimal(String.valueOf(annualInterest));
            // Format the number to 2 decimals with thousands separator before adding the percentage symbol
            BigDecimal rounded = bd.setScale(2, RoundingMode.HALF_UP);
            String formatted = String.format("%,.2f", rounded);
            percentageString = formatted + " %";
        }
        return percentageString;
    }

    private Object formatAmount(Object annualInterest) {
        String percentageString = "0";
        if (NumberUtils.isCreatable(String.valueOf(annualInterest))) {
            BigDecimal bd = new BigDecimal(String.valueOf(annualInterest));
            // Format the number to 2 decimals with thousands separator before adding the percentage symbol
            BigDecimal rounded = bd.setScale(2, RoundingMode.HALF_UP);
            String formatted = String.format("%,.2f", rounded);
            percentageString = formatted;
        }
        return percentageString;
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
                SELECT DISTINCT psl.to_status, mc.display_name as clientName,
                                ml.principal_amount as loanAmount,
                                agency.name as agencyName,
                                mc.id as clientCode,
                                (lrs.equivalentAnnualRate * 100) as equivalentAnnualRate,
                                'No Aplica' as expenseAdmin,
                                (CASE
                                WHEN psl.to_status =1006 then 'COMMITTEE A'
                                WHEN psl.to_status =1005 then 'COMMITTEE B'
                                WHEN psl.to_status =1004 then 'COMMITTEE C'
                                WHEN psl.to_status =1003 then 'COMMITTEE D'
                                WHEN psl.to_status =911 then 'COMMITTEE D CON EXCEPCIONES'
                                WHEN psl.to_status =912 then 'COMMITTEE C CON EXCEPCIONES'
                                WHEN psl.to_status =913 then 'COMMITTEE B CON EXCEPCIONES'
                                WHEN psl.to_status =914 then 'COMMITTEE A CON EXCEPCIONES'
                                ELSE 'INVÁLIDO' END) as commiteeLevel,
                                COALESCE(pgr.collateral,'No Aplica') as collateral,
                                COALESCE(hptrcv.code_description,'No Aplica') as registeredMortgage,
                                COALESCE((ml.principal_amount/(pgr.collateralValue))* 100,'N/A') as collateralCoverage,
                                CASE
                					WHEN mpl.owner_type_enum = 4 THEN 'PAE'
                					ELSE 'PUENTE al Éxito'
                				END AS program,
                                cv_sol.code_value as sector,
                                cv_sol_fd.code_value as formalizationDocument,
                                (select date_created from m_prequalification_status_log where prequalification_id=? and to_status=900 order by date_created desc limit 1 ) as approvalDate,
                                ml.annual_nominal_interest_rate,(lrs.equivalentAnnualRate / 12)*100 AS equivalentMonthlyRate,lrs.installmentAmount,
                                (CASE
                                WHEN ml.repayment_period_frequency_enum =0 then 'A diario'
                                WHEN ml.repayment_period_frequency_enum =1 then 'Semanalmente'
                                WHEN ml.repayment_period_frequency_enum =2 then 'Mensual'
                                WHEN ml.repayment_period_frequency_enum =3 then 'Anualmente'
                                WHEN ml.repayment_period_frequency_enum =4 then 'TÉRMINO COMPLETO'
                                ELSE 'INVÁLIDO' END) as frequencyTerm,
                                (CASE
                                WHEN ml.term_period_frequency_enum =0 then concat(ml.term_frequency,' ', (CASE WHEN ml.term_frequency>1 then 'DÍAS' ELSE 'DÍA' END))
                                WHEN ml.term_period_frequency_enum =1 then concat(ml.term_frequency,' ',(CASE WHEN ml.term_frequency>1 then 'SEMANAS' ELSE 'SEMANA' END))
                                WHEN ml.term_period_frequency_enum =2 then concat(ml.term_frequency,' ', (CASE WHEN ml.term_frequency>1 then 'MESES' ELSE 'Mensual' END))
                                WHEN ml.term_period_frequency_enum =3 then concat(ml.term_frequency,' ',(CASE WHEN ml.term_frequency>1 then 'AÑOS' ELSE 'AÑO' END))
                                WHEN ml.term_period_frequency_enum =4 then concat(ml.term_frequency,' ','TÉRMINO COMPLETO')
                                ELSE 'INVÁLIDO' END) as loanTerm,
                                (case when ml.interest_method_enum = 0 then 'Sobre saldos.' else 'Plano' END) as interestMethod,
                                (ml.annual_nominal_interest_rate/12) as nominalMonthlyRate,
                                ml.annual_nominal_interest_rate as annualInterest,
                                (ml.annual_nominal_interest_rate/12) as monthlyInterest,
                                COALESCE(
                				(
                					SELECT GROUP_CONCAT(mcv.code_value ORDER BY mcv.code_value SEPARATOR ', ')
                					FROM p_garantia pg
                					JOIN m_code_value mcv
                						ON mcv.id = pg.guaranteeType_cd_tipo_garantia
                					WHERE pg.loan_id = ml.id
                				),
                                CONCAT(mccv.code_value,' ',pgrtype.collateralType)) as collateralType,
                                COALESCE((
                									SELECT SUM(pg.valor_garantia)
                									FROM p_garantia pg
                									WHERE pg.loan_id = ml.id
                								),
                                COALESCE(pgr.collateralValue,'N/A' ))  as collateralValue,
                                COALESCE(
                				(
                					SELECT GROUP_CONCAT(mcv.code_value ORDER BY mcv.code_value SEPARATOR ', ')
                					FROM p_destino pd
                					JOIN m_code_value mcv
                						ON mcv.id = pd.loanPurposeOptionsPAE_cd_destino
                					WHERE pd.loan_id = ml.id
                				),
                				COALESCE(lad.destino_prestamo,'N/A' )
                				) AS loanPurpose,
                                psl.date_created as logDate,
                                concat(au.username,' - ',au.firstname,' ', au.lastname, ' ') as updatedBy,
                                concat(fc.username,' - ',fc.firstname,' ', fc.lastname, ' ') as facilitatorName,
                        COALESCE(
                          (
                              SELECT GROUP_CONCAT(
                                  CONCAT(
                                      DATE_FORMAT(psl2.date_created,'%d/%m/%Y %H:%i'),
                                      ' - ',
                                      psl2.comments,
                                      ' (Excepción: ',
                                      CASE WHEN psl2.is_exception = 1 THEN 'SI' ELSE 'NO' END,
                                      ')'
                                  )
                                  ORDER BY psl2.date_created ASC
                                  SEPARATOR ', '
                              )
                              FROM m_prequalification_status_log psl2
                              WHERE psl2.prequalification_id = pg.id
                                AND psl2.comments IS NOT NULL
                                AND psl2.is_exception IS NOT NULL
                          ),
                          'N/A'
                          ) AS analysisInformation,
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
                                left join m_appuser fc on fc.id = portfolio.responsible_user_id
                                left join p_solicitante ps on ps.loan_id = ml.id
                                left join m_code_value cv_sol on cv_sol.id = ps.classificationOptions_cd_actividad_economica_principal
                                left join m_code_value cv_sol_fd on cv_sol_fd.id = ps.formalizationDocument_cd_documento_formalizacion
                                left join p_garantia hptr on hptr.loan_id
                                left join m_code_value hptrcv on hptrcv.id = hptr.registeredMortgage_cd_hipoteca_registrada
                                left join m_code_value grtp on grtp.id = hptr.guaranteeType_cd_tipo_garantia and grtp.code_description='Hipoteca'
                                LEFT JOIN (
                                    SELECT
                                        loan_id, GROUP_CONCAT( CONCAT( detalle_garantia, ' GARANTIA', rn ) SEPARATOR ',' ) AS collateral,
                                        SUM(valor_garantia) AS collateralValue
                                        FROM
                                        	( SELECT loan_id, detalle_garantia, valor_garantia, ROW_NUMBER() OVER ( PARTITION BY loan_id ORDER BY detalle_garantia ) AS rn FROM p_garantia ) t
                                        GROUP BY loan_id
                                    ) pgr ON pgr.loan_id = ml.id
                                LEFT JOIN (
                                    SELECT
                                    	loan_id,
                                    	GROUP_CONCAT( CONCAT( guarantee_type) SEPARATOR ',' ) AS collateralType
                                    FROM
                                        ( SELECT pf.loan_id, mcv.code_value as guarantee_type FROM p_fiador pf LEFT JOIN m_code_value mcv on pf.guarantorType_cd_tipo_fiador_tercero = mcv.id ) t\s
                                    GROUP BY loan_id
                                    	) pgrtype ON pgrtype.loan_id = ml.id
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
                                    ) * ml.term_frequency)-ml.principal_amount )/ml.principal_amount)/ml.term_frequency) *100) as equivalentMonthlyRate
                                    from m_loan_repayment_schedule lrs inner join m_loan ml on ml.id=lrs.loan_id where lrs.loan_id = ? AND lrs.installment=1) as lrs on lrs.loan_id = ml.id
                                where pg.id = ? and psl.to_status in (1003,1004,1005,1006,911,912,913,914) order by psl.id desc
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
