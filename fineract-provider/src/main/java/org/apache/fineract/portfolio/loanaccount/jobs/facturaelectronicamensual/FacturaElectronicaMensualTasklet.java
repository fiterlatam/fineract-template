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
package org.apache.fineract.portfolio.loanaccount.jobs.facturaelectronicamensual;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanaccount.invoice.data.LoanDocumentData;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicMensualRepository;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicaMensual;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformService;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterization;
import org.apache.fineract.portfolio.loanproductparameterization.domain.LoanProductParameterizationRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@Slf4j
public class FacturaElectronicaMensualTasklet implements Tasklet {

    private final FacturaElectronicMensualRepository facturaElectronicMensualRepository;
    private final JdbcTemplate jdbcTemplate;
    private final LoanProductParameterizationRepository productParameterizationRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final LoanWritePlatformService loanWritePlatformService;

    @Autowired
    public FacturaElectronicaMensualTasklet(final FacturaElectronicMensualRepository facturaElectronicMensualRepository,
            final JdbcTemplate jdbcTemplate, final LoanProductParameterizationRepository productParameterizationRepository,
            final ConfigurationDomainService configurationDomainService, LoanWritePlatformService loanWritePlatformService) {
        this.facturaElectronicMensualRepository = facturaElectronicMensualRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.productParameterizationRepository = productParameterizationRepository;
        this.configurationDomainService = configurationDomainService;
        this.loanWritePlatformService = loanWritePlatformService;
    }

    @Override
    public RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext) throws Exception {
        log.info("FacturaElectronicaMensualTasklet execute method called");
        final LocalDate businessLocalDate = DateUtils.getBusinessLocalDate();
        final YearMonth yearMonth = YearMonth.from(businessLocalDate);
        final LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();
        final LocalDate firstDayOfMonth = businessLocalDate.withDayOfMonth(1);
        final LocalDate secondLastDayOfMonth = lastDayOfMonth.minusDays(1);
        final boolean enableMonthlyInvoiceGenerationOnJobTrigger = this.configurationDomainService
                .enableMonthlyInvoiceGenerationOnJobTrigger();
        if (businessLocalDate.equals(secondLastDayOfMonth) || enableMonthlyInvoiceGenerationOnJobTrigger) {
            final List<LoanProductParameterization> loanProductParameterizations = this.productParameterizationRepository.findAll();
            final LoanInvoiceMapper loanInvoiceMapper = new LoanInvoiceMapper();
            final String invoiceQuery = "SELECT " + loanInvoiceMapper.invoiceSchema();
            final String creditNoteQuery = "SELECT " + loanInvoiceMapper.creditNoteSchema();
            final List<LoanDocumentData> loanInvoiceDataList = this.jdbcTemplate.query(invoiceQuery, loanInvoiceMapper, firstDayOfMonth,
                    secondLastDayOfMonth);
            final List<LoanDocumentData> groupedLoanInvoices = groupByClientIdAndProductType(loanInvoiceDataList);
            final List<FacturaElectronicaMensual> facturaElectronicaMensuals = new ArrayList<>();
            for (final LoanDocumentData groupedLoanInvoice : groupedLoanInvoices) {
                groupedLoanInvoice.setDocumentType(LoanDocumentData.LoanDocumentType.INVOICE);
                this.loanWritePlatformService.processAndSaveLoanDocument(groupedLoanInvoice);
            }
            this.facturaElectronicMensualRepository.saveAllAndFlush(facturaElectronicaMensuals);
            this.productParameterizationRepository.saveAllAndFlush(loanProductParameterizations);
            final List<LoanDocumentData> loanCreditNoteDataList = this.jdbcTemplate.query(creditNoteQuery, loanInvoiceMapper,
                    firstDayOfMonth, secondLastDayOfMonth);
            final List<LoanDocumentData> groupedLoanCreditNotes = groupByClientIdAndProductType(loanCreditNoteDataList);
            for (final LoanDocumentData groupedLoanCreditNote : groupedLoanCreditNotes) {
                groupedLoanCreditNote.setDocumentType(LoanDocumentData.LoanDocumentType.CREDIT_NOTE);
                this.loanWritePlatformService.processAndSaveLoanDocument(groupedLoanCreditNote);
            }
        }
        return RepeatStatus.FINISHED;
    }

    private List<LoanDocumentData> groupByClientIdAndProductType(final List<LoanDocumentData> loanDocumentDataList) {
        final LocalDate businessLocalDate = DateUtils.getBusinessLocalDate();
        final YearMonth yearMonth = YearMonth.from(businessLocalDate);
        final LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();
        final LocalDate firstDayOfMonth = businessLocalDate.withDayOfMonth(1);
        final LocalDate secondLastDayOfMonth = lastDayOfMonth.minusDays(1);
        return loanDocumentDataList.stream()
                .collect(Collectors.groupingBy(li1 -> Arrays.asList(li1.getClientId(), li1.getProductTypeName()),
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            final BigDecimal interestPaid = list.stream().map(LoanDocumentData::getInterestPaid).reduce(BigDecimal.ZERO,
                                    BigDecimal::add);
                            final BigDecimal penaltyChargesPaid = list.stream().map(LoanDocumentData::getPenaltyChargesPaid)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            final BigDecimal mandatoryInsurancePaid = list.stream().map(LoanDocumentData::getMandatoryInsurancePaid)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            final BigDecimal voluntaryInsurancePaid = list.stream().map(LoanDocumentData::getVoluntaryInsurancePaid)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            final BigDecimal honorariosPaid = list.stream().map(LoanDocumentData::getHonorariosPaid).reduce(BigDecimal.ZERO,
                                    BigDecimal::add);
                            final BigDecimal totalPaid = list.stream().map(LoanDocumentData::getTotalPaid).reduce(BigDecimal.ZERO,
                                    BigDecimal::add);
                            final Integer loansCount = list.size();
                            final LoanDocumentData loanDocumentData = list.get(0);
                            return LoanDocumentData.builder().clientIdNumber(loanDocumentData.getClientIdNumber())
                                    .productTypeParamId(loanDocumentData.getProductTypeParamId())
                                    .billingPrefix(loanDocumentData.getBillingPrefix())
                                    .billingResolutionNumber(loanDocumentData.getBillingResolutionNumber())
                                    .rangeStartNumber(loanDocumentData.getRangeStartNumber())
                                    .rangeEndNumber(loanDocumentData.getRangeEndNumber())
                                    .lastInvoiceNumber(loanDocumentData.getLastInvoiceNumber())
                                    .lastCreditNoteNumber(loanDocumentData.getLastCreditNoteNumber())
                                    .lastDebitNoteNumber(loanDocumentData.getLastDebitNoteNumber())
                                    .technicalKey(loanDocumentData.getTechnicalKey()).nota(loanDocumentData.getNota())
                                    .nota(loanDocumentData.getNota()).clientDisplayName(loanDocumentData.getClientDisplayName())
                                    .clientLastName(loanDocumentData.getClientLastName())
                                    .clientLegalForm(loanDocumentData.getClientLegalForm()).clientId(loanDocumentData.getClientId())
                                    .clientEmailAddress(loanDocumentData.getClientEmailAddress()).loanId(loanDocumentData.getLoanId())
                                    .productTypeId(loanDocumentData.getProductTypeId())
                                    .productTypeName(loanDocumentData.getProductTypeName())
                                    .overdueSinceDate(loanDocumentData.getOverdueSinceDate())
                                    .daysInArrears(loanDocumentData.getDaysInArrears()).interestPaid(interestPaid)
                                    .penaltyChargesPaid(penaltyChargesPaid).mandatoryInsurancePaid(mandatoryInsurancePaid)
                                    .voluntaryInsurancePaid(voluntaryInsurancePaid).honorariosPaid(honorariosPaid).totalPaid(totalPaid)
                                    .loansCount(loansCount).firstDayOfMonth(firstDayOfMonth).secondLastDayOfMonth(secondLastDayOfMonth)
                                    .lastDayOfMonth(lastDayOfMonth).loanProductName(loanDocumentData.getLoanProductName())
                                    .companyNIT(loanDocumentData.getCompanyNIT()).companyDocType(loanDocumentData.getCompanyDocType())
                                    .companyDeptCode(loanDocumentData.getCompanyDeptCode())
                                    .companyDeptName(loanDocumentData.getCompanyDeptName())
                                    .companyCityCode(loanDocumentData.getCompanyCityCode())
                                    .companyCityName(loanDocumentData.getCompanyCityName())
                                    .companyAddress(loanDocumentData.getCompanyAddress())
                                    .companyTelephone(loanDocumentData.getCompanyTelephone())
                                    .clientCedula(loanDocumentData.getClientCedula()).clientAddress(loanDocumentData.getClientAddress())
                                    .clientCityCode(loanDocumentData.getClientCityCode())
                                    .clientCityName(loanDocumentData.getClientCityName())
                                    .clientTelephone(loanDocumentData.getClientTelephone()).build();
                        })))
                .values().stream().toList();

    }

    public static class LoanInvoiceMapper implements RowMapper<LoanDocumentData> {

        public String invoiceSchema() {
            return """
                        	mc.id AS "clientId",
                        	mc.legal_form_enum AS "clientLegalForm",
                        	ml.id AS "loanId",
                        	mc.display_name AS "clientDisplayName",
                        	mc.lastname AS "clientLastName",
                        	mlaa.overdue_since_date_derived AS "overdueSinceDate",
                        	COALESCE(CURRENT_DATE - mlaa.overdue_since_date_derived::DATE,0) AS "daysInArrears",
                        	prodtype.id AS "productTypeId",
                        	prodtype.code_value AS "productTypeName",
                        	COALESCE(cce."NIT", ccp."Cedula") AS "clientIdNumber",
                        	pp.id AS "productTypeParamId",
                        	pp.billing_prefix AS "billingPrefix",
                        	pp.billing_resolution_number AS "billingResolutionNumber",
                        	pp.range_start_number AS "rangeStartNumber",
                        	pp.range_end_number AS "rangeEndNumber",
                        	pp.last_invoice_number AS "lastInvoiceNumber",
                        	pp.last_credit_note_number AS "lastCreditNoteNumber",
                        	pp.last_debit_note_number AS "lastDebitNoteNumber",
                        	pp.clave_tecnica AS "technicalKey",
                        	pp.nota AS nota,
                        	mpl.name AS "loanProductName",
                        	cce."NIT" AS "companyNIT",
                        	dept.code_score AS "companyDeptCode",
                        	dept.code_value AS "companyDeptName",
                        	companycity.code_score AS "companyCityCode",
                        	companycity.code_value AS "companyCityName",
                        	cce."Direccion" AS "companyAddress",
                        	mc.email_address AS "clientEmailAddress",
                        	cce."Telefono" AS "companyTelephone",
                        	ccp."Cedula" AS "clientCedula",
                        	ccp."Direccion" AS "clientAddress",
                        	clientcity.code_score AS "clientCityCode",
                        	clientcity.code_value AS "clientCityName",
                        	ccp."Telefono" AS "clientTelephone",
                        	companydoctype.code_value AS "companyDocType",
                        	mlt."interest" AS "interestPaid",
                        	mlt."mandatoryInsurance" AS "mandatoryInsurancePaid",
                        	mlt."voluntaryInsurance" AS "voluntaryInsurancePaid",
                        	mlt."honorarios" AS "honorariosPaid",
                        	mlt."penaltyCharges" AS "penaltyChargesPaid",
                        	mlt."totalPaid" AS "totalPaid",
                        	mandatory_insurance_code."codeValue" AS "mandatoryInsuranceCode",
                         	voluntary_insurance_code."codeValue" AS "voluntaryInsuranceCode",
                         	mandatory_insurance_code."codeName" AS "mandatoryInsuranceName",
                          	voluntary_insurance_code."codeName" AS "voluntaryInsuranceName"
                        FROM m_loan ml
                        INNER JOIN m_client mc ON mc.id = ml.client_id
                        INNER JOIN m_product_loan mpl ON mpl.id = ml.product_id
                        INNER JOIN m_code_value prodtype ON prodtype.id = mpl.product_type
                        INNER JOIN (
                        		SELECT
                        			mlt.loan_id AS "loanId",
                        			SUM(COALESCE(mlt.interest_portion_derived, 0)) AS "interest",
                        			SUM(COALESCE(mandatory_insurance.amount, 0) + COALESCE(vat_mandatory_insurance.amount, 0)) AS "mandatoryInsurance",
                        			SUM(COALESCE(voluntary_insurance.amount, 0) + COALESCE(vat_voluntary_insurance.amount, 0)) AS "voluntaryInsurance",
                        			SUM(COALESCE(hono.amount, 0) + COALESCE(vat_hono.amount, 0)) AS "honorarios",
                        			SUM(COALESCE(penalty.amount, 0) + COALESCE(vat_penalty.amount, 0)) AS "penaltyCharges",
                        			SUM(COALESCE(mlt.interest_portion_derived, 0)
                        				+ COALESCE(mandatory_insurance.amount, 0) + COALESCE(vat_mandatory_insurance.amount, 0)
                        				+ COALESCE(voluntary_insurance.amount, 0) + COALESCE(vat_voluntary_insurance.amount, 0)
                        				+ COALESCE(hono.amount, 0) + COALESCE(vat_hono.amount, 0)
                        				+ COALESCE(penalty.amount, 0) + COALESCE(vat_penalty.amount, 0)) AS "totalPaid"
                        		FROM m_loan_transaction mlt
                        		LEFT JOIN (
                        			SELECT mlcpd.loan_transaction_id,
                        					SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.charge_calculation_enum IN (468, 575, 231)
                        			GROUP BY mlcpd.loan_transaction_id
                        		) mandatory_insurance ON mandatory_insurance.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342 AND parent.charge_calculation_enum IN (468, 575, 231)
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_mandatory_insurance ON vat_mandatory_insurance.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.charge_calculation_enum = 1034
                        			GROUP BY mlcpd.loan_transaction_id
                        		) voluntary_insurance ON voluntary_insurance.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342 AND parent.charge_calculation_enum = 1034
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_voluntary_insurance ON vat_voluntary_insurance.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.charge_calculation_enum = 41
                        			GROUP BY mlcpd.loan_transaction_id
                        		) aval ON aval.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342 AND parent.charge_calculation_enum = 41
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_aval ON vat_aval.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id ,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.charge_calculation_enum = 1009
                        			GROUP BY mlcpd.loan_transaction_id
                        		) hono ON hono.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342 AND parent.charge_calculation_enum = 1009
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_hono ON vat_hono.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id ,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.is_penalty = TRUE
                        			GROUP BY mlcpd.loan_transaction_id
                        		) penalty ON penalty.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342
                        				AND mc.is_penalty = TRUE
                        				AND parent.charge_calculation_enum = 1009
                        				AND parent.is_penalty = TRUE
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_penalty ON vat_penalty.loan_transaction_id = mlt.id
                        		WHERE mlt.is_reversed = FALSE AND mlt.occurred_on_suspended_account = FALSE
                        		AND mlt.transaction_type_enum = 2
                        		AND (mlt.transaction_date BETWEEN ? AND ?)
                        		GROUP BY mlt.loan_id
                        ) mlt ON mlt."loanId" = ml.id
                        INNER JOIN (
                            SELECT DISTINCT ON (mptp.product_type) *
                            FROM m_product_type_parameters mptp
                            WHERE mptp.expiration_date >= CURRENT_DATE
                         ) pp ON pp.product_type = prodtype.code_value
                        LEFT JOIN m_loan_arrears_aging mlaa ON mlaa.loan_id = ml.id
                        LEFT JOIN campos_cliente_empresas cce ON cce.client_id = mc.id
                        LEFT JOIN m_code_value companydoctype ON companydoctype.id = cce."Tipo ID_cd_Tipo ID"
                        LEFT JOIN m_code_value dept ON dept.id = cce."Departamento_cd_Departamento"
                        LEFT JOIN m_code_value companycity ON companycity.id = cce."Ciudad_cd_Ciudad"
                        LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                        LEFT JOIN m_code_value clientcity ON clientcity.id = ccp."Ciudad_cd_Ciudad"
                        LEFT JOIN (
                          SELECT mlc.loan_id,
                          MAX(mc.insurance_code) AS "codeValue",
                          MAX(mc.insurance_name) AS "codeName"
                          FROM m_loan_charge mlc
                          INNER JOIN m_charge mc ON mc.id = mlc.charge_id
                          WHERE mlc.charge_calculation_enum IN (468, 575, 231)
                          GROUP BY mlc.loan_id
                        ) mandatory_insurance_code ON mandatory_insurance_code.loan_id = ml.id
                        LEFT JOIN (
                          SELECT mlc.loan_id,
                          MAX(mc.insurance_code) AS "codeValue",
                          MAX(mc.insurance_name) AS "codeName"
                          FROM m_loan_charge mlc
                          INNER JOIN m_charge mc ON mc.id = mlc.charge_id
                          WHERE mlc.charge_calculation_enum = 1034
                          GROUP BY mlc.loan_id
                        ) voluntary_insurance_code ON voluntary_insurance_code.loan_id = ml.id
                        WHERE ml.loan_status_id = 300
                            AND COALESCE(CURRENT_DATE - mlaa.overdue_since_date_derived::DATE, 0) < 90
                            AND mlt."totalPaid" > 0
                        ORDER BY mc.id, ml.id
                    """;
        }

        public String creditNoteSchema() {
            return """
                        	mc.id AS "clientId",
                        	mc.legal_form_enum AS "clientLegalForm",
                        	ml.id AS "loanId",
                        	mc.display_name AS "clientDisplayName",
                        	mc.lastname AS "clientLastName",
                        	mlaa.overdue_since_date_derived AS "overdueSinceDate",
                        	COALESCE(CURRENT_DATE - mlaa.overdue_since_date_derived::DATE,0) AS "daysInArrears",
                        	prodtype.id AS "productTypeId",
                        	prodtype.code_value AS "productTypeName",
                        	COALESCE(cce."NIT", ccp."Cedula") AS "clientIdNumber",
                        	pp.id AS "productTypeParamId",
                        	pp.billing_prefix AS "billingPrefix",
                        	pp.billing_resolution_number AS "billingResolutionNumber",
                        	pp.range_start_number AS "rangeStartNumber",
                        	pp.range_end_number AS "rangeEndNumber",
                        	pp.last_invoice_number AS "lastInvoiceNumber",
                        	pp.last_credit_note_number AS "lastCreditNoteNumber",
                        	pp.last_debit_note_number AS "lastDebitNoteNumber",
                        	pp.clave_tecnica AS "technicalKey",
                        	pp.nota AS nota,
                        	mpl.name AS "loanProductName",
                        	cce."NIT" AS "companyNIT",
                        	dept.code_score AS "companyDeptCode",
                        	dept.code_value AS "companyDeptName",
                        	companycity.code_score AS "companyCityCode",
                        	companycity.code_value AS "companyCityName",
                        	cce."Direccion" AS "companyAddress",
                        	mc.email_address AS "clientEmailAddress",
                        	cce."Telefono" AS "companyTelephone",
                        	ccp."Cedula" AS "clientCedula",
                        	ccp."Direccion" AS "clientAddress",
                        	clientcity.code_score AS "clientCityCode",
                        	clientcity.code_value AS "clientCityName",
                        	ccp."Telefono" AS "clientTelephone",
                        	companydoctype.code_value AS "companyDocType",
                        	mlt."interest" AS "interestPaid",
                        	mlt."mandatoryInsurance" AS "mandatoryInsurancePaid",
                        	mlt."voluntaryInsurance" AS "voluntaryInsurancePaid",
                        	mlt."honorarios" AS "honorariosPaid",
                        	mlt."penaltyCharges" AS "penaltyChargesPaid",
                        	mlt."totalPaid" AS "totalPaid",
                        	mandatory_insurance_code."codeValue" AS "mandatoryInsuranceCode",
                         	voluntary_insurance_code."codeValue" AS "voluntaryInsuranceCode",
                         	mandatory_insurance_code."codeName" AS "mandatoryInsuranceName",
                          	voluntary_insurance_code."codeName" AS "voluntaryInsuranceName"
                        FROM m_loan ml
                        INNER JOIN m_client mc ON mc.id = ml.client_id
                        INNER JOIN m_product_loan mpl ON mpl.id = ml.product_id
                        INNER JOIN m_code_value prodtype ON prodtype.id = mpl.product_type
                        INNER JOIN (
                        		SELECT
                        			mlt.loan_id AS "loanId",
                        			SUM(COALESCE(mlt.interest_portion_derived, 0)) AS "interest",
                        			SUM(COALESCE(mandatory_insurance.amount, 0) + COALESCE(vat_mandatory_insurance.amount, 0)) AS "mandatoryInsurance",
                        			SUM(COALESCE(voluntary_insurance.amount, 0) + COALESCE(vat_voluntary_insurance.amount, 0)) AS "voluntaryInsurance",
                        			SUM(COALESCE(hono.amount, 0) + COALESCE(vat_hono.amount, 0)) AS "honorarios",
                        			SUM(COALESCE(penalty.amount, 0) + COALESCE(vat_penalty.amount, 0)) AS "penaltyCharges",
                        			SUM(COALESCE(mlt.interest_portion_derived, 0)
                        				+ COALESCE(mandatory_insurance.amount, 0) + COALESCE(vat_mandatory_insurance.amount, 0)
                        				+ COALESCE(voluntary_insurance.amount, 0) + COALESCE(vat_voluntary_insurance.amount, 0)
                        				+ COALESCE(hono.amount, 0) + COALESCE(vat_hono.amount, 0)
                        				+ COALESCE(penalty.amount, 0) + COALESCE(vat_penalty.amount, 0)) AS "totalPaid"
                        		FROM m_loan_transaction mlt
                        		LEFT JOIN (
                        			SELECT mlcpd.loan_transaction_id,
                        					SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.charge_calculation_enum IN (468, 575, 231)
                        			GROUP BY mlcpd.loan_transaction_id
                        		) mandatory_insurance ON mandatory_insurance.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342 AND parent.charge_calculation_enum IN (468, 575, 231)
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_mandatory_insurance ON vat_mandatory_insurance.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.charge_calculation_enum = 1034
                        			GROUP BY mlcpd.loan_transaction_id
                        		) voluntary_insurance ON voluntary_insurance.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342 AND parent.charge_calculation_enum = 1034
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_voluntary_insurance ON vat_voluntary_insurance.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.charge_calculation_enum = 41
                        			GROUP BY mlcpd.loan_transaction_id
                        		) aval ON aval.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342 AND parent.charge_calculation_enum = 41
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_aval ON vat_aval.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id ,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.charge_calculation_enum = 1009
                        			GROUP BY mlcpd.loan_transaction_id
                        		) hono ON hono.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342 AND parent.charge_calculation_enum = 1009
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_hono ON vat_hono.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id ,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.is_penalty = TRUE
                        			GROUP BY mlcpd.loan_transaction_id
                        		) penalty ON penalty.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342
                        				AND mc.is_penalty = TRUE
                        				AND parent.charge_calculation_enum = 1009
                        				AND parent.is_penalty = TRUE
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_penalty ON vat_penalty.loan_transaction_id = mlt.id
                        		INNER JOIN m_loan_credit_note mlcn ON mlcn.transaction_id = mlt.id
                        		WHERE mlt.is_reversed = FALSE AND mlt.occurred_on_suspended_account = FALSE
                        		AND (mlt.transaction_type_enum = 6 AND mlt.is_special_writeoff = TRUE)
                        		AND (mlt.transaction_date BETWEEN ? AND ?)
                        		GROUP BY mlt.loan_id
                        ) mlt ON mlt."loanId" = ml.id
                        INNER JOIN (
                            SELECT DISTINCT ON (mptp.product_type) *
                            FROM m_product_type_parameters mptp
                            WHERE mptp.expiration_date >= CURRENT_DATE
                         ) pp ON pp.product_type = prodtype.code_value
                        LEFT JOIN m_loan_arrears_aging mlaa ON mlaa.loan_id = ml.id
                        LEFT JOIN campos_cliente_empresas cce ON cce.client_id = mc.id
                        LEFT JOIN m_code_value companydoctype ON companydoctype.id = cce."Tipo ID_cd_Tipo ID"
                        LEFT JOIN m_code_value dept ON dept.id = cce."Departamento_cd_Departamento"
                        LEFT JOIN m_code_value companycity ON companycity.id = cce."Ciudad_cd_Ciudad"
                        LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                        LEFT JOIN m_code_value clientcity ON clientcity.id = ccp."Ciudad_cd_Ciudad"
                        LEFT JOIN (
                          SELECT mlc.loan_id,
                          MAX(mc.insurance_code) AS "codeValue",
                          MAX(mc.insurance_name) AS "codeName"
                          FROM m_loan_charge mlc
                          INNER JOIN m_charge mc ON mc.id = mlc.charge_id
                          WHERE mlc.charge_calculation_enum IN (468, 575, 231)
                          GROUP BY mlc.loan_id
                        ) mandatory_insurance_code ON mandatory_insurance_code.loan_id = ml.id
                        LEFT JOIN (
                          SELECT mlc.loan_id,
                          MAX(mc.insurance_code) AS "codeValue",
                          MAX(mc.insurance_name) AS "codeName"
                          FROM m_loan_charge mlc
                          INNER JOIN m_charge mc ON mc.id = mlc.charge_id
                          WHERE mlc.charge_calculation_enum = 1034
                          GROUP BY mlc.loan_id
                        ) voluntary_insurance_code ON voluntary_insurance_code.loan_id = ml.id
                        WHERE ml.loan_status_id = 300
                            AND COALESCE(CURRENT_DATE - mlaa.overdue_since_date_derived::DATE, 0) < 90
                            AND mlt."totalPaid" > 0
                        ORDER BY mc.id, ml.id
                    """;
        }

        public String transactionSchema() {
            return """
                        	mc.id AS "clientId",
                        	mc.legal_form_enum AS "clientLegalForm",
                        	ml.id AS "loanId",
                        	mc.display_name AS "clientDisplayName",
                        	mc.lastname AS "clientLastName",
                        	mlaa.overdue_since_date_derived AS "overdueSinceDate",
                        	COALESCE(CURRENT_DATE - mlaa.overdue_since_date_derived::DATE,0) AS "daysInArrears",
                        	prodtype.id AS "productTypeId",
                        	prodtype.code_value AS "productTypeName",
                        	COALESCE(cce."NIT", ccp."Cedula") AS "clientIdNumber",
                        	pp.id AS "productTypeParamId",
                        	pp.billing_prefix AS "billingPrefix",
                        	pp.billing_resolution_number AS "billingResolutionNumber",
                        	pp.range_start_number AS "rangeStartNumber",
                        	pp.range_end_number AS "rangeEndNumber",
                        	pp.last_invoice_number AS "lastInvoiceNumber",
                        	pp.last_credit_note_number AS "lastCreditNoteNumber",
                        	pp.last_debit_note_number AS "lastDebitNoteNumber",
                        	pp.clave_tecnica AS "technicalKey",
                        	pp.nota AS nota,
                        	mpl.name AS "loanProductName",
                        	cce."NIT" AS "companyNIT",
                        	dept.code_score AS "companyDeptCode",
                        	dept.code_value AS "companyDeptName",
                        	companycity.code_score AS "companyCityCode",
                        	companycity.code_value AS "companyCityName",
                        	cce."Direccion" AS "companyAddress",
                        	mc.email_address AS "clientEmailAddress",
                        	cce."Telefono" AS "companyTelephone",
                        	ccp."Cedula" AS "clientCedula",
                        	ccp."Direccion" AS "clientAddress",
                        	clientcity.code_score AS "clientCityCode",
                        	clientcity.code_value AS "clientCityName",
                        	ccp."Telefono" AS "clientTelephone",
                        	companydoctype.code_value AS "companyDocType",
                        	mlt."interest" AS "interestPaid",
                        	mlt."mandatoryInsurance" AS "mandatoryInsurancePaid",
                        	mlt."voluntaryInsurance" AS "voluntaryInsurancePaid",
                        	mlt."honorarios" AS "honorariosPaid",
                        	mlt."penaltyCharges" AS "penaltyChargesPaid",
                        	mlt."totalPaid" AS "totalPaid",
                        	mandatory_insurance_code."codeValue" AS "mandatoryInsuranceCode",
                         	voluntary_insurance_code."codeValue" AS "voluntaryInsuranceCode",
                         	mandatory_insurance_code."codeName" AS "mandatoryInsuranceName",
                          	voluntary_insurance_code."codeName" AS "voluntaryInsuranceName"
                        FROM m_loan ml
                        INNER JOIN m_client mc ON mc.id = ml.client_id
                        INNER JOIN m_product_loan mpl ON mpl.id = ml.product_id
                        INNER JOIN m_code_value prodtype ON prodtype.id = mpl.product_type
                        INNER JOIN (
                        		SELECT
                        			mlt.loan_id AS "loanId",
                        			mlt.id AS "transactionId",
                        			SUM(COALESCE(mlt.interest_portion_derived, 0)) AS "interest",
                        			SUM(COALESCE(mandatory_insurance.amount, 0) + COALESCE(vat_mandatory_insurance.amount, 0)) AS "mandatoryInsurance",
                        			SUM(COALESCE(voluntary_insurance.amount, 0) + COALESCE(vat_voluntary_insurance.amount, 0)) AS "voluntaryInsurance",
                        			SUM(COALESCE(hono.amount, 0) + COALESCE(vat_hono.amount, 0)) AS "honorarios",
                        			SUM(COALESCE(penalty.amount, 0) + COALESCE(vat_penalty.amount, 0)) AS "penaltyCharges",
                        			SUM(COALESCE(mlt.interest_portion_derived, 0)
                        				+ COALESCE(mandatory_insurance.amount, 0) + COALESCE(vat_mandatory_insurance.amount, 0)
                        				+ COALESCE(voluntary_insurance.amount, 0) + COALESCE(vat_voluntary_insurance.amount, 0)
                        				+ COALESCE(hono.amount, 0) + COALESCE(vat_hono.amount, 0)
                        				+ COALESCE(penalty.amount, 0) + COALESCE(vat_penalty.amount, 0)) AS "totalPaid"
                        		FROM m_loan_transaction mlt
                        		LEFT JOIN (
                        			SELECT mlcpd.loan_transaction_id,
                        					SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.charge_calculation_enum IN (468, 575, 231)
                        			GROUP BY mlcpd.loan_transaction_id
                        		) mandatory_insurance ON mandatory_insurance.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342 AND parent.charge_calculation_enum IN (468, 575, 231)
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_mandatory_insurance ON vat_mandatory_insurance.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.charge_calculation_enum = 1034
                        			GROUP BY mlcpd.loan_transaction_id
                        		) voluntary_insurance ON voluntary_insurance.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342 AND parent.charge_calculation_enum = 1034
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_voluntary_insurance ON vat_voluntary_insurance.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.charge_calculation_enum = 41
                        			GROUP BY mlcpd.loan_transaction_id
                        		) aval ON aval.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342 AND parent.charge_calculation_enum = 41
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_aval ON vat_aval.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id ,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.charge_calculation_enum = 1009
                        			GROUP BY mlcpd.loan_transaction_id
                        		) hono ON hono.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342 AND parent.charge_calculation_enum = 1009
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_hono ON vat_hono.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id ,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			WHERE mlc.is_penalty = TRUE
                        			GROUP BY mlcpd.loan_transaction_id
                        		) penalty ON penalty.loan_transaction_id = mlt.id
                        		LEFT JOIN (
                        			SELECT
                        				mlcpd.loan_transaction_id,
                        				SUM(mlcpd.amount) amount
                        			FROM m_loan_charge_paid_by mlcpd
                        			JOIN m_loan_charge mlc ON mlc.id = mlcpd.loan_charge_id
                        			JOIN m_charge mc ON mc.id = mlc.charge_id
                        			JOIN m_charge parent ON parent.id = mc.parent_charge_id
                        			WHERE mc.charge_calculation_enum = 342
                        				AND mc.is_penalty = TRUE
                        				AND parent.charge_calculation_enum = 1009
                        				AND parent.is_penalty = TRUE
                        			GROUP BY mlcpd.loan_transaction_id
                        		) vat_penalty ON vat_penalty.loan_transaction_id = mlt.id
                        		WHERE mlt.is_reversed = FALSE
                        		GROUP BY mlt.loan_id, mlt.id
                        ) mlt ON mlt."loanId" = ml.id
                        INNER JOIN (
                            SELECT DISTINCT ON (mptp.product_type) *
                            FROM m_product_type_parameters mptp
                            WHERE mptp.expiration_date >= CURRENT_DATE
                         ) pp ON pp.product_type = prodtype.code_value
                        LEFT JOIN m_loan_arrears_aging mlaa ON mlaa.loan_id = ml.id
                        LEFT JOIN campos_cliente_empresas cce ON cce.client_id = mc.id
                        LEFT JOIN m_code_value companydoctype ON companydoctype.id = cce."Tipo ID_cd_Tipo ID"
                        LEFT JOIN m_code_value dept ON dept.id = cce."Departamento_cd_Departamento"
                        LEFT JOIN m_code_value companycity ON companycity.id = cce."Ciudad_cd_Ciudad"
                        LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                        LEFT JOIN m_code_value clientcity ON clientcity.id = ccp."Ciudad_cd_Ciudad"
                        LEFT JOIN (
                          SELECT mlc.loan_id,
                          MAX(mc.insurance_code) AS "codeValue",
                           MAX(mc.insurance_name) AS "codeName"
                          FROM m_loan_charge mlc
                          INNER JOIN m_charge mc ON mc.id = mlc.charge_id
                          WHERE mlc.charge_calculation_enum IN (468, 575, 231)
                          GROUP BY mlc.loan_id
                        ) mandatory_insurance_code ON mandatory_insurance_code.loan_id = ml.id
                        LEFT JOIN (
                          SELECT mlc.loan_id,
                          MAX(mc.insurance_code) AS "codeValue",
                          MAX(mc.insurance_name) AS "codeName"
                          FROM m_loan_charge mlc
                          INNER JOIN m_charge mc ON mc.id = mlc.charge_id
                          WHERE mlc.charge_calculation_enum = 1034
                          GROUP BY mlc.loan_id
                        ) voluntary_insurance_code ON voluntary_insurance_code.loan_id = ml.id
                    """;
        }

        @Override
        public LoanDocumentData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            return LoanDocumentData.builder().loanId(rs.getLong("loanId")).clientId(rs.getLong("clientId"))
                    .clientLegalForm(rs.getInt("clientLegalForm")).clientDisplayName(rs.getString("clientDisplayName"))
                    .clientLastName(rs.getString("clientLastName")).clientEmailAddress(rs.getString("clientEmailAddress"))
                    .overdueSinceDate(JdbcSupport.getLocalDate(rs, "overdueSinceDate")).daysInArrears(rs.getInt("daysInArrears"))
                    .productTypeId(rs.getLong("productTypeId")).productTypeName(rs.getString("productTypeName"))
                    .clientIdNumber(rs.getString("clientIdNumber")).productTypeParamId(rs.getLong("productTypeParamId"))
                    .billingPrefix(rs.getString("billingPrefix")).billingResolutionNumber(rs.getString("billingResolutionNumber"))
                    .rangeStartNumber(JdbcSupport.getLong(rs, "rangeStartNumber")).rangeEndNumber(JdbcSupport.getLong(rs, "rangeEndNumber"))
                    .lastInvoiceNumber(JdbcSupport.getLong(rs, "lastInvoiceNumber"))
                    .lastCreditNoteNumber(JdbcSupport.getLong(rs, "lastCreditNoteNumber"))
                    .lastDebitNoteNumber(JdbcSupport.getLong(rs, "lastDebitNoteNumber")).technicalKey(rs.getString("technicalKey"))
                    .nota(rs.getString("nota")).loanProductName(rs.getString("loanProductName")).companyNIT(rs.getString("companyNIT"))
                    .companyDocType(rs.getString("companyDocType")).companyDeptCode(rs.getString("companyDeptCode"))
                    .companyDeptName(rs.getString("companyDeptName")).companyCityCode(rs.getString("companyCityCode"))
                    .companyCityName(rs.getString("companyCityName")).companyAddress(rs.getString("companyAddress"))
                    .companyTelephone(rs.getString("companyTelephone")).clientCedula(rs.getString("clientCedula"))
                    .clientAddress(rs.getString("clientAddress")).clientCityCode(rs.getString("clientCityCode"))
                    .clientCityName(rs.getString("clientCityName")).clientTelephone(rs.getString("clientTelephone"))
                    .interestPaid(JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestPaid"))
                    .penaltyChargesPaid(JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesPaid"))
                    .mandatoryInsurancePaid(JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "mandatoryInsurancePaid"))
                    .voluntaryInsurancePaid(JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "voluntaryInsurancePaid"))
                    .honorariosPaid(JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "honorariosPaid"))
                    .totalPaid(JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalPaid"))
                    .mandatoryInsuranceCode(rs.getString("mandatoryInsuranceCode"))
                    .voluntaryInsuranceCode(rs.getString("voluntaryInsuranceCode"))
                    .voluntaryInsuranceName(rs.getString("voluntaryInsuranceName"))
                    .mandatoryInsuranceName(rs.getString("mandatoryInsuranceName")).build();
        }
    }

}
