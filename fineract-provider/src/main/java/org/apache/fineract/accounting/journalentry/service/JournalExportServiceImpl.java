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
package org.apache.fineract.accounting.journalentry.service;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.accounting.journalentry.data.ColombianJournalEntryData;
import org.apache.fineract.accounting.journalentry.data.JournalExportRequest;
import org.apache.fineract.infrastructure.core.serialization.JsonParserHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * Service implementation for exporting journal entries in Colombian format
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalExportServiceImpl implements JournalExportService {

    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String TAB_DELIMITER = "\t";
    private static final String LINE_SEPARATOR = "\r\n";

    @Override
    public void exportJournalEntries(JournalExportRequest request, OutputStream outputStream, String filename) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

            // Write header row
            writeHeader(writer);

            // Retrieve data from the view
            List<ColombianJournalEntryData> entries = retrieveExportData(request);

            // Write data rows
            for (ColombianJournalEntryData entry : entries) {
                writeDataRow(writer, entry);
            }

            log.info("Successfully exported {} journal entries to file: {}", entries.size(), filename);

        } catch (Exception e) {
            log.error("Error exporting journal entries: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export journal entries", e);
        }
    }

    @Override
    public String generateFilename(JournalExportRequest request) {
        String dateFormat = StringUtils.isBlank(request.getDateFormat()) ? "yyyy-MM-dd" : request.getDateFormat();
        String localeStr = StringUtils.isBlank(request.getLocale()) ? "en" : request.getLocale();
        Locale locale = JsonParserHelper.localeFromString(localeStr);

        LocalDate fromDate = DateUtils.parseLocalDate(request.getDateFrom(), dateFormat, locale);
        LocalDate toDate = DateUtils.parseLocalDate(request.getDateTo(), dateFormat, locale);

        String fromDateStr = fromDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String toDateStr = toDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("journal_entries_%s_%s.txt", fromDateStr, toDateStr);
    }

    private List<ColombianJournalEntryData> retrieveExportData(JournalExportRequest request) {
        String dateFormat = StringUtils.isBlank(request.getDateFormat()) ? "yyyy-MM-dd" : request.getDateFormat();
        String localeStr = StringUtils.isBlank(request.getLocale()) ? "en" : request.getLocale();
        Locale locale = JsonParserHelper.localeFromString(localeStr);

        LocalDate fromDate = DateUtils.parseLocalDate(request.getDateFrom(), dateFormat, locale);
        LocalDate toDate = DateUtils.parseLocalDate(request.getDateTo(), dateFormat, locale);

        String sql = """
                SELECT * FROM v_colombian_journal_export
                WHERE office_id = ?
                AND MOV_FECHADCMTO BETWEEN ? AND ?
                ORDER BY MOV_FECHADCMTO, office_id
                """;

        return jdbcTemplate.query(sql, new Object[] { request.getOfficeId(), fromDate, toDate }, new ColombianJournalEntryRowMapper());
    }

    private void writeHeader(PrintWriter writer) {
        String[] headers = { "MOV_MOVIMIENTO", "MES_MESCONTABLE", "CTA_CUENTA", "EMP_EMPRESA", "EMP_EMPFL", "MOV_VALOR", "TPC_TIPOCOMPROB",
                "CeCo", "TER_TERCERO", "TER_DIGVER", "TER_GRUPO", "MON_MONEDA", "TRM_FECHA", "MOV_VLROTRAMON", "MOV_VLRTRM",
                "CPT_CPTOTRIBUTA", "MOV_PORCENTAJE", "MOV_VLRBASE", "MOV_DCMTO", "MOV_AFECTADCMTO", "MOV_FECHADCMTO", "MOV_FINI",
                "MOV_FFIN", "NAT", "CPC_CPTOCOMP", "TPD_TIPODOC", "SCO_SUBDIVCOM", "MENCONCEPTO", "MOV_FECHA", "MOV_ESTADO", "MOV_CTAPADRE",
                "TER_GRANCONT", "TER_REGIVA", "MOV_BASERETIVA", "MOV_CAUSARETIVA", "TER_AUTORETENEDOR", "MOV_CAUSARETEFTE",
                "MOV_BASERETEFTE", "MOV_VLRDIFBASE", "MOV_VLRDIFMON", "MOV_MOVPADRE", "MOV_FECHAMOV", "MOV_USUARIO", "MOV_SECUENCIA",
                "OBR_OBRA", "OBR_SUBOBRA", "var_intnulo", "tip_docafecta", "var_intnulo2" };

        writer.write(String.join(TAB_DELIMITER, headers) + LINE_SEPARATOR);
    }

    private void writeDataRow(PrintWriter writer, ColombianJournalEntryData entry) {
        String[] values = { String.valueOf(entry.getMovMovimiento()), entry.getMesMescontable(), entry.getCtaCuenta(),
                entry.getEmpEmpresa(), entry.getEmpEmpfl(), formatAmount(entry.getMovValor()), entry.getTpcTipocomprob(), entry.getCeCo(),
                entry.getTerTercero(), entry.getTerDigver(), entry.getTerGrupo(), entry.getMonMoneda(), entry.getTrmFecha(),
                entry.getMovVlrotramon(), entry.getMovVlrtrm(), entry.getCptCptotributa(), entry.getMovPorcentaje(), entry.getMovVlrbase(),
                entry.getMovDcmto(), entry.getMovAfectadcmto(), formatDate(entry.getMovFechadcmto()), formatDate(entry.getMovFini()),
                formatDate(entry.getMovFfin()), String.valueOf(entry.getNat()), entry.getCpcCptocomp(), entry.getTpdTipodoc(),
                entry.getScoSubdivcom(), entry.getMenconcepto(), formatDate(entry.getMovFecha()), entry.getMovEstado(),
                entry.getMovCtapadre(), entry.getTerGrancont(), entry.getTerRegiva(), entry.getMovBaseretiva(), entry.getMovCausaretiva(),
                entry.getTerAutoretenedor(), entry.getMovCausaretefte(), entry.getMovBaserefte(), entry.getMovVlrdifbase(),
                entry.getMovVlrdifmon(), entry.getMovMovpadre(), entry.getMovFechamov(), entry.getMovUsuario(), entry.getMovSecuencia(),
                entry.getObrObra(), entry.getObrSubobra(), String.valueOf(entry.getVarIntnulo()), entry.getTipDocafecta(),
                String.valueOf(entry.getVarIntnulo2()) };

        writer.write(String.join(TAB_DELIMITER, values) + LINE_SEPARATOR);
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%.2f", amount);
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER);
    }

    /**
     * Row mapper for Colombian journal entry data
     */
    private static class ColombianJournalEntryRowMapper implements RowMapper<ColombianJournalEntryData> {

        @Override
        public ColombianJournalEntryData mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            ColombianJournalEntryData data = new ColombianJournalEntryData();

            // Map all 49 columns from the view
            data.setMovMovimiento(rs.getInt("MOV_MOVIMIENTO"));
            data.setMesMescontable(rs.getString("MES_MESCONTABLE"));
            data.setCtaCuenta(rs.getString("CTA_CUENTA"));
            data.setEmpEmpresa(rs.getString("EMP_EMPRESA"));
            data.setEmpEmpfl(rs.getString("EMP_EMPFL"));
            data.setMovValor(rs.getBigDecimal("MOV_VALOR"));
            data.setTpcTipocomprob(rs.getString("TPC_TIPOCOMPROB"));
            data.setCeCo(rs.getString("CeCo"));
            data.setTerTercero(rs.getString("TER_TERCERO"));
            data.setTerDigver(rs.getString("TER_DIGVER"));
            data.setTerGrupo(rs.getString("TER_GRUPO"));
            data.setMonMoneda(rs.getString("MON_MONEDA"));
            data.setTrmFecha(rs.getString("TRM_FECHA"));
            data.setMovVlrotramon(rs.getString("MOV_VLROTRAMON"));
            data.setMovVlrtrm(rs.getString("MOV_VLRTRM"));
            data.setCptCptotributa(rs.getString("CPT_CPTOTRIBUTA"));
            data.setMovPorcentaje(rs.getString("MOV_PORCENTAJE"));
            data.setMovVlrbase(rs.getString("MOV_VLRBASE"));
            data.setMovDcmto(rs.getString("MOV_DCMTO"));
            data.setMovAfectadcmto(rs.getString("MOV_AFECTADCMTO"));
            data.setMovFechadcmto(rs.getDate("MOV_FECHADCMTO").toLocalDate());
            data.setMovFini(rs.getDate("MOV_FINI").toLocalDate());
            data.setMovFfin(rs.getDate("MOV_FFIN").toLocalDate());
            data.setNat(rs.getInt("NAT"));
            data.setCpcCptocomp(rs.getString("CPC_CPTOCOMP"));
            data.setTpdTipodoc(rs.getString("TPD_TIPODOC"));
            data.setScoSubdivcom(rs.getString("SCO_SUBDIVCOM"));
            data.setMenconcepto(rs.getString("MENCONCEPTO"));
            data.setMovFecha(rs.getDate("MOV_FECHA").toLocalDate());
            data.setMovEstado(rs.getString("MOV_ESTADO"));
            data.setMovCtapadre(rs.getString("MOV_CTAPADRE"));
            data.setTerGrancont(rs.getString("TER_GRANCONT"));
            data.setTerRegiva(rs.getString("TER_REGIVA"));
            data.setMovBaseretiva(rs.getString("MOV_BASERETIVA"));
            data.setMovCausaretiva(rs.getString("MOV_CAUSARETIVA"));
            data.setTerAutoretenedor(rs.getString("TER_AUTORETENEDOR"));
            data.setMovCausaretefte(rs.getString("MOV_CAUSARETEFTE"));
            data.setMovBaserefte(rs.getString("MOV_BASERETEFTE"));
            data.setMovVlrdifbase(rs.getString("MOV_VLRDIFBASE"));
            data.setMovVlrdifmon(rs.getString("MOV_VLRDIFMON"));
            data.setMovMovpadre(rs.getString("MOV_MOVPADRE"));
            data.setMovFechamov(rs.getString("MOV_FECHAMOV"));
            data.setMovUsuario(rs.getString("MOV_USUARIO"));
            data.setMovSecuencia(rs.getString("MOV_SECUENCIA"));
            data.setObrObra(rs.getString("OBR_OBRA"));
            data.setObrSubobra(rs.getString("OBR_SUBOBRA"));
            data.setVarIntnulo(rs.getInt("var_intnulo"));
            data.setTipDocafecta(rs.getString("tip_docafecta"));
            data.setVarIntnulo2(rs.getInt("var_intnulo2"));

            // Additional context fields
            data.setOfficeId(rs.getLong("office_id"));
            data.setEntityTypeEnum(rs.getInt("entity_type_enum"));
            data.setEntityId(rs.getLong("entity_id"));
            data.setTransactionId(rs.getString("transaction_id"));
            data.setClassificationEnum(rs.getInt("classification_enum"));

            return data;
        }
    }
}
