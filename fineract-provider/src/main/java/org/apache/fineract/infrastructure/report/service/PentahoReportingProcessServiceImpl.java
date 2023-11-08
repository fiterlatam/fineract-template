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
package org.apache.fineract.infrastructure.report.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.ApiParameterHelper;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.report.annotation.ReportService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.CompoundDataFactory;
import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.DefaultReportEnvironment;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.DriverConnectionProvider;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.SQLReportDataFactory;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.csv.CSVReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.ExcelReportUtil;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@ReportService(type = "Pentaho")
public class PentahoReportingProcessServiceImpl implements ReportingProcessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PentahoReportingProcessServiceImpl.class);
    private static final String MIFOS_BASE_DIR = System.getProperty("user.home") + File.separator + ".mifosx";

    private final PlatformSecurityContext context;
    private final FineractProperties fineractProperties;
    @Value("${fineract.configuration.pentahoFolderName}")
    private String pentahoFolderName;

    @Autowired
    public PentahoReportingProcessServiceImpl(final PlatformSecurityContext context, final FineractProperties fineractProperties) {
        ClassicEngineBoot.getInstance().start();
        this.context = context;
        this.fineractProperties = fineractProperties;
    }

    @Override
    public Response processRequest(final String reportName, final MultivaluedMap<String, String> queryParams) {
        final var outputTypeParam = queryParams.getFirst("output-type");
        final var reportParams = getReportParams(queryParams);
        final var locale = ApiParameterHelper.extractLocale(queryParams);
        var outputType = "HTML";
        if (StringUtils.isNotBlank(outputTypeParam)) {
            outputType = outputTypeParam;
        }
        if ((!outputType.equalsIgnoreCase("HTML") && !outputType.equalsIgnoreCase("PDF") && !outputType.equalsIgnoreCase("XLS")
                && !outputType.equalsIgnoreCase("XLSX") && !outputType.equalsIgnoreCase("CSV"))) {
            throw new PlatformDataIntegrityException("error.msg.invalid.outputType", "No matching Output Type: " + outputType);
        }
        final var reportPath = MIFOS_BASE_DIR + File.separator + pentahoFolderName + File.separator + reportName + ".prpt";
        var outPutInfo = "Report path: " + reportPath;
        LOGGER.info("Report path: {}", outPutInfo);

        final var manager = new ResourceManager();
        manager.registerDefaults();
        Resource res;
        try {
            res = manager.createDirectly(reportPath, MasterReport.class);
            final var masterReport = (MasterReport) res.getResource();
            final ReportParameterValues parameterValues = masterReport.getParameterValues();
            addParametersToReport(masterReport, reportParams);
            String tenantUrl = (String) parameterValues.get("tenantUrl");
            String username = (String) parameterValues.get("username");
            String password = (String) parameterValues.get("password");
            DataFactory dataFactory = masterReport.getDataFactory();
            CompoundDataFactory compoundDataFactory = (CompoundDataFactory) dataFactory;
            SQLReportDataFactory sqlReportDataFactory = (SQLReportDataFactory) compoundDataFactory.get(0);
            DriverConnectionProvider connectionProvider = (DriverConnectionProvider) sqlReportDataFactory.getConnectionProvider();
            final var tenant = ThreadLocalContextUtil.getTenant();
            final var tenantConnection = tenant.getConnection();
            connectionProvider.setUrl(tenantUrl);
            connectionProvider.setDriver("org.mariadb.jdbc.Driver");
            connectionProvider.setProperty("password", password);
            connectionProvider.setProperty("user", username);
            connectionProvider.setProperty("::pentaho-reporting::hostname", tenantConnection.getSchemaServer());
            connectionProvider.setProperty("::pentaho-reporting::database-type", "MariaDb");
            connectionProvider.setProperty("::pentaho-reporting::name", "fineract");
            connectionProvider.setProperty("::pentaho-reporting::database-name", tenantConnection.getReadOnlySchemaName());
            connectionProvider.setProperty("defaultFetchSize", "500");
            connectionProvider.setProperty("::pentaho-reporting-other-attribute::STREAM_RESULTS", "Y");
            connectionProvider.setProperty("::pentaho-reporting-other-attribute::PORT_NUMBER", tenantConnection.getSchemaServerPort());
            connectionProvider.setProperty("::pentaho-reporting::port", tenantConnection.getSchemaServerPort());
            connectionProvider.setProperty("socketFactory", null);
            connectionProvider.setProperty("cloudSqlInstance", null);
            final var reportEnvironment = (DefaultReportEnvironment) masterReport.getReportEnvironment();
            if (locale != null) {
                reportEnvironment.setLocale(locale);
            }
            TimeZone timeZone = TimeZone.getDefault();
            if (Arrays.asList(TimeZone.getAvailableIDs()).contains(tenant.getTimezoneId())) {
                timeZone = TimeZone.getTimeZone(tenant.getTimezoneId());
            }
            reportEnvironment.setTimeZone(timeZone);
            final var baos = new ByteArrayOutputStream();
            if ("PDF".equalsIgnoreCase(outputType)) {
                PdfReportUtil.createPDF(masterReport, baos);
                return Response.ok().entity(baos.toByteArray()).type("application/pdf").build();

            } else if ("XLS".equalsIgnoreCase(outputType)) {
                ExcelReportUtil.createXLS(masterReport, baos);
                return Response.ok().entity(baos.toByteArray()).type("application/vnd.ms-excel")
                        .header("Content-Disposition", "attachment;filename=" + reportName.replaceAll(" ", "") + ".xls").build();

            } else if ("XLSX".equalsIgnoreCase(outputType)) {
                ExcelReportUtil.createXLSX(masterReport, baos);
                return Response.ok().entity(baos.toByteArray()).type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .header("Content-Disposition", "attachment;filename=" + reportName.replaceAll(" ", "") + ".xlsx").build();

            } else if ("CSV".equalsIgnoreCase(outputType)) {
                CSVReportUtil.createCSV(masterReport, baos, "UTF-8");
                return Response.ok().entity(baos.toByteArray()).type("text/csv")
                        .header("Content-Disposition", "attachment;filename=" + reportName.replaceAll(" ", "") + ".csv").build();

            } else if ("HTML".equalsIgnoreCase(outputType)) {
                HtmlReportUtil.createStreamHTML(masterReport, baos);
                return Response.ok().entity(baos.toByteArray()).type("text/html").build();

            } else {
                throw new PlatformDataIntegrityException("error.msg.invalid.outputType", "No matching Output Type: " + outputType);

            }
        } catch (final ResourceException | ReportProcessingException | IOException e) {
            LOGGER.error("Pentaho failed", e);
            throw new PlatformDataIntegrityException("error.msg.reporting.error", "Pentaho failed: " + e.getMessage());
        }
    }

    private void addParametersToReport(final MasterReport report, final Map<String, String> queryParams) {
        final var currentUser = this.context.authenticatedUser();
        try {
            final var rptParamValues = report.getParameterValues();
            final var paramsDefinition = report.getParameterDefinition();
            for (final ParameterDefinitionEntry paramDefEntry : paramsDefinition.getParameterDefinitions()) {
                final String paramName = paramDefEntry.getName();
                final String pValue = queryParams.get(paramName);
                if (!(paramName.equals("tenantUrl")
                        || (paramName.equals("userhierarchy") || paramName.equals("username")
                                || (paramName.equals("password") || paramName.equals("userid")))
                        || (StringUtils.isBlank(pValue) && (paramName.equals("startDate") || paramName.equals("endDate"))))) {
                    LOGGER.info("paramName:" + paramName);
                    if (StringUtils.isBlank(pValue)) {
                        throw new PlatformDataIntegrityException("error.msg.reporting.error",
                                "Pentaho Parameter: " + paramName + " - not Provided");
                    }

                    final Class<?> clazz = paramDefEntry.getValueType();
                    LOGGER.info("addParametersToReport(" + paramName + " : " + pValue + " : " + clazz.getCanonicalName() + ")");
                    if (clazz.getCanonicalName().equalsIgnoreCase("java.lang.Integer")) {
                        rptParamValues.put(paramName, Integer.parseInt(pValue));
                    } else if (clazz.getCanonicalName().equalsIgnoreCase("java.lang.Long")) {
                        rptParamValues.put(paramName, Long.parseLong(pValue));
                    } else if (clazz.getCanonicalName().equalsIgnoreCase("java.sql.Date")) {
                        rptParamValues.put(paramName, Date.valueOf(pValue));
                    } else {
                        rptParamValues.put(paramName, pValue);
                    }
                }
            }

            final var tenant = ThreadLocalContextUtil.getTenant();
            final var tenantConnection = tenant.getConnection();
            final String schemaParameters = tenantConnection.getSchemaConnectionParameters();
            var tenantUrl = "jdbc:mariadb://" + tenantConnection.getSchemaServer() + ":" + tenantConnection.getSchemaServerPort() + "/"
                    + tenantConnection.getSchemaName() + "?useSSL=false&" + schemaParameters;
            final var userHierarchy = currentUser.getOffice().getHierarchy();
            var outPutInfo4 = "db URL:" + tenantUrl + "      userhierarchy:" + userHierarchy;
            LOGGER.info(outPutInfo4);
            rptParamValues.put("userhierarchy", userHierarchy);
            final var userid = currentUser.getId();
            var outPutInfo5 = "db URL:" + tenantUrl + "      userid:" + userid;
            LOGGER.info(outPutInfo5);
            rptParamValues.put("userid", userid);
            rptParamValues.put("tenantUrl", tenantUrl);
            rptParamValues.put("username", tenantConnection.getSchemaUsername());
            rptParamValues.put("password", tenantConnection.getSchemaPassword());
        } catch (final Exception e) {
            LOGGER.error("error.msg.reporting.error:", e);
            throw new PlatformDataIntegrityException("error.msg.reporting.error", e.getMessage());
        }
    }

    @Override
    public Map<String, String> getReportParams(final MultivaluedMap<String, String> queryParams) {
        final Map<String, String> reportParams = new HashMap<>();
        final var keys = queryParams.keySet();
        String pKey;
        String pValue;
        for (final String k : keys) {
            if (k.startsWith("R_")) {
                pKey = k.substring(2);
                pValue = queryParams.get(k).get(0);
                reportParams.put(pKey, pValue);
            }
        }
        return reportParams;
    }
}
