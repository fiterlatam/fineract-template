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
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanaccount.data.DisbursementData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.RepaymentScheduleRelatedLoanData;
import org.apache.fineract.portfolio.loanaccount.invoice.data.LoanInvoiceData;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicMensualRepository;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicaMensual;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleCalculationPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
public class FacturaElectronicaMensualTasklet implements Tasklet {

    private final FacturaElectronicMensualRepository facturaElectronicMensualRepository;
    private final JdbcTemplate jdbcTemplate;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanScheduleCalculationPlatformService calculationPlatformService;

    @Autowired
    public FacturaElectronicaMensualTasklet(final FacturaElectronicMensualRepository facturaElectronicMensualRepository,
            final JdbcTemplate jdbcTemplate, final LoanReadPlatformService loanReadPlatformService,
            final LoanScheduleCalculationPlatformService calculationPlatformService) {
        this.facturaElectronicMensualRepository = facturaElectronicMensualRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.loanReadPlatformService = loanReadPlatformService;
        this.calculationPlatformService = calculationPlatformService;
    }

    @Override
    public RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext) throws Exception {
        log.info("FacturaElectronicaMensualTasklet execute method called");
        final LocalDate businessLocalDate = DateUtils.getBusinessLocalDate();
        final YearMonth yearMonth = YearMonth.from(businessLocalDate);
        final LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();
        final LocalDate firstDayOfMonth = businessLocalDate.withDayOfMonth(1);
        final LocalDate secondLastDayOfMonth = lastDayOfMonth.minusDays(1);
        if (!businessLocalDate.equals(secondLastDayOfMonth)) {
            final String loanInvoiceQuery = """
                                SELECT
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
                                	pp.billing_prefix AS "billingPrefix",
                                	pp.billing_resolution_number AS "billingResolutionNumber",
                                	pp.range_start_number AS "rangeStartNumber",
                                	pp.range_end_number AS "rangeEndNumber",
                                	pp.last_invoice_number AS "lastInvoiceNumber",
                                	pp.last_credit_note_number AS "lastCreditNoteNumber",
                                	pp.last_debit_note_number AS "lastDebitNoteNumber",
                                	mpl.name AS "loanProductName",
                                	cce."NIT" AS "companyNIT",
                                	dept.code_score AS "companyDeptCode",
                                	dept.code_value AS "companyDeptName",
                                	mun.code_score AS "companyMunCode",
                                	mun.code_value AS "companyMunName",
                                	companycity.code_score AS "companyCityCode",
                                	companycity.code_value AS "companyCityName",
                                	cce."Direccion" AS "companyAddress",
                                	mc.email_address AS "clientEmailAddress",
                                	cce."Telefono" AS "companyTelephone",
                                	ccp."Cedula" AS "clientCedula",
                                	ccp."Direccion" AS "clientAddress",
                                	clientcity.code_score AS "clientCityCode",
                                	clientcity.code_value AS "clientCityName",
                                	ccp."Telefono" AS "clientTelephone"
                                FROM m_loan ml
                                INNER JOIN m_client mc ON mc.id = ml.client_id
                                INNER JOIN m_product_loan mpl ON mpl.id = ml.product_id
                                INNER JOIN m_code_value prodtype ON prodtype.id = mpl.product_type
                                INNER JOIN (
                                    SELECT DISTINCT ON (mptp.product_type) *
                                    FROM m_product_type_parameters mptp
                                    WHERE mptp.expiration_date >= CURRENT_DATE
                                 ) pp ON pp.product_type = prodtype.code_value
                                LEFT JOIN m_loan_arrears_aging mlaa ON mlaa.loan_id = ml.id
                                LEFT JOIN campos_cliente_empresas cce ON cce.client_id = mc.id
                                LEFT JOIN m_code_value dept ON dept.id = cce."Departamento_cd_Departamento"
                                LEFT JOIN m_code_value mun ON mun.id = cce."Departamento_cd_Departamento"
                                LEFT JOIN m_code_value companycity ON companycity.id = cce."Ciudad_cd_Ciudad"
                                LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                                LEFT JOIN m_code_value clientcity ON clientcity.id = ccp."Ciudad_cd_Ciudad"
                                WHERE ml.loan_status_id = 300
                                    AND COALESCE(CURRENT_DATE - mlaa.overdue_since_date_derived::DATE, 0) < 90
                                ORDER BY mc.id, ml.id
                    """;
            final List<LoanInvoiceData> loanInvoiceDataList = jdbcTemplate.query(loanInvoiceQuery, (rs, rowNum) -> LoanInvoiceData.builder()
                    .loanId(rs.getLong("loanId")).clientId(rs.getLong("clientId")).clientLegalForm(rs.getInt("clientLegalForm"))
                    .clientDisplayName(rs.getString("clientDisplayName")).clientLastName(rs.getString("clientLastName"))
                    .clientEmailAddress(rs.getString("clientEmailAddress"))
                    .overdueSinceDate(JdbcSupport.getLocalDate(rs, "overdueSinceDate")).daysInArrears(rs.getInt("daysInArrears"))
                    .productTypeId(rs.getLong("productTypeId")).productTypeName(rs.getString("productTypeName"))
                    .clientIdNumber(rs.getString("clientIdNumber")).billingPrefix(rs.getString("billingPrefix"))
                    .billingResolutionNumber(rs.getString("billingResolutionNumber"))
                    .rangeStartNumber(JdbcSupport.getLong(rs, "rangeStartNumber")).rangeEndNumber(JdbcSupport.getLong(rs, "rangeEndNumber"))
                    .lastInvoiceNumber(JdbcSupport.getLong(rs, "lastInvoiceNumber"))
                    .lastCreditNoteNumber(JdbcSupport.getLong(rs, "lastCreditNoteNumber"))
                    .lastDebitNoteNumber(JdbcSupport.getLong(rs, "lastDebitNoteNumber")).loanProductName(rs.getString("loanProductName"))
                    .companyNIT(rs.getString("companyNIT")).companyDeptCode(rs.getString("companyDeptCode"))
                    .companyDeptName(rs.getString("companyDeptName")).companyMunCode(rs.getString("companyMunCode"))
                    .companyMunName(rs.getString("companyMunName")).companyCityCode(rs.getString("companyCityCode"))
                    .companyCityName(rs.getString("companyCityName")).companyAddress(rs.getString("companyAddress"))
                    .companyTelephone(rs.getString("companyTelephone")).clientCedula(rs.getString("clientCedula"))
                    .clientAddress(rs.getString("clientAddress")).clientCityCode(rs.getString("clientCityCode"))
                    .clientCityName(rs.getString("clientCityName")).clientTelephone(rs.getString("clientTelephone")).build());
            for (final LoanInvoiceData loanInvoiceData : loanInvoiceDataList) {
                final Long loanId = loanInvoiceData.getLoanId();
                LoanAccountData loanBasicDetails = this.loanReadPlatformService.retrieveOne(loanId);
                Collection<DisbursementData> disbursementData = this.loanReadPlatformService.retrieveLoanDisbursementDetails(loanId);
                final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedData = loanBasicDetails.getTimeline()
                        .repaymentScheduleRelatedData(loanBasicDetails.getCurrency(), loanBasicDetails.getPrincipal(),
                                loanBasicDetails.getApprovedPrincipal(), loanBasicDetails.getInArrearsTolerance(),
                                loanBasicDetails.getFeeChargesAtDisbursementCharged());
                final LoanScheduleData repaymentScheduleData = this.loanReadPlatformService.retrieveRepaymentSchedule(loanId,
                        repaymentScheduleRelatedData, disbursementData, loanBasicDetails.isInterestRecalculationEnabled(),
                        LoanScheduleType.fromEnumOptionData(loanBasicDetails.getLoanScheduleType()));
                this.calculationPlatformService.getFeeChargesDetail(repaymentScheduleData, loanId);
                final Collection<LoanSchedulePeriodData> loanSchedulePeriodDataList = repaymentScheduleData.getPeriods();
                BigDecimal outstandingPrincipal = BigDecimal.ZERO;
                BigDecimal currentInterest = BigDecimal.ZERO;
                BigDecimal overdueInterest = BigDecimal.ZERO;
                BigDecimal outstandingPenalty = BigDecimal.ZERO;
                BigDecimal outstandingMandatoryInsurance = BigDecimal.ZERO;
                BigDecimal outstandingVoluntaryInsurance = BigDecimal.ZERO;
                BigDecimal outstandingAval = BigDecimal.ZERO;
                BigDecimal outstandingHonorarios = BigDecimal.ZERO;
                for (final LoanSchedulePeriodData loanSchedulePeriodData : loanSchedulePeriodDataList) {
                    if (loanSchedulePeriodData.getPeriod() != null && loanSchedulePeriodData.getPeriod() > 0) {
                        if (!DateUtils.isBefore(businessLocalDate, loanSchedulePeriodData.getDueDate())) {
                            outstandingPrincipal = outstandingPrincipal.add(loanSchedulePeriodData.getPrincipalOutstanding());
                            overdueInterest = overdueInterest.add(loanSchedulePeriodData.getInterestOutstanding());
                            outstandingPenalty = outstandingPenalty.add(loanSchedulePeriodData.getPenaltyChargesOutstanding());
                            outstandingMandatoryInsurance = outstandingMandatoryInsurance
                                    .add(loanSchedulePeriodData.getMandatoryInsuranceOutstanding());
                            outstandingVoluntaryInsurance = outstandingVoluntaryInsurance
                                    .add(loanSchedulePeriodData.getVoluntaryInsuranceOutstanding());
                            outstandingAval = outstandingAval.add(loanSchedulePeriodData.getAvalOutstanding());
                            outstandingHonorarios = outstandingHonorarios.add(loanSchedulePeriodData.getHonorariosOutstanding());
                        } else if (DateUtils.isAfter(businessLocalDate, loanSchedulePeriodData.getFromDate())) {
                            final int totalPeriodDays = Math.toIntExact(
                                    ChronoUnit.DAYS.between(loanSchedulePeriodData.getFromDate(), loanSchedulePeriodData.getDueDate()));
                            final int tillDays = Math
                                    .toIntExact(ChronoUnit.DAYS.between(loanSchedulePeriodData.getFromDate(), businessLocalDate));
                            final BigDecimal interestForCurrentPeriod = calculateInterestForDays(totalPeriodDays,
                                    loanSchedulePeriodData.getInterestDue(), tillDays);
                            final BigDecimal interestAccountedForCurrentPeriod = loanSchedulePeriodData.getInterestWaived()
                                    .add(loanSchedulePeriodData.getInterestPaid()).add(loanSchedulePeriodData.getInterestWrittenOff());
                            if (interestForCurrentPeriod.compareTo(interestAccountedForCurrentPeriod) > 0) {
                                currentInterest = currentInterest.add(interestForCurrentPeriod.subtract(interestAccountedForCurrentPeriod));
                            } else {
                                currentInterest = currentInterest.add(BigDecimal.ZERO);
                            }
                            outstandingPrincipal = outstandingPrincipal.add(loanSchedulePeriodData.getPrincipalOutstanding());
                            outstandingPenalty = outstandingPenalty.add(loanSchedulePeriodData.getPenaltyChargesOutstanding());
                            outstandingMandatoryInsurance = outstandingMandatoryInsurance
                                    .add(loanSchedulePeriodData.getMandatoryInsuranceOutstanding());
                            outstandingVoluntaryInsurance = outstandingVoluntaryInsurance
                                    .add(loanSchedulePeriodData.getVoluntaryInsuranceOutstanding());
                            outstandingAval = outstandingAval.add(loanSchedulePeriodData.getAvalOutstanding());
                        }
                    }
                }
                loanInvoiceData.setOutstandingPrincipal(outstandingPrincipal);
                loanInvoiceData.setCurrentInterest(currentInterest);
                loanInvoiceData.setOverdueInterest(overdueInterest);
                loanInvoiceData.setOutstandingPenalty(outstandingPenalty);
                loanInvoiceData.setOutstandingMandatoryInsurance(outstandingMandatoryInsurance);
                loanInvoiceData.setOutstandingVoluntaryInsurance(outstandingVoluntaryInsurance);
                loanInvoiceData.setOutstandingAval(outstandingAval);
                loanInvoiceData.setOutstandingHonorarios(outstandingHonorarios);
                final BigDecimal outstandingAmount = outstandingPrincipal.add(currentInterest).add(overdueInterest).add(outstandingPenalty)
                        .add(outstandingMandatoryInsurance).add(outstandingVoluntaryInsurance).add(outstandingAval)
                        .add(outstandingHonorarios);
                loanInvoiceData.setTotalOutstanding(outstandingAmount);
            }
            final List<LoanInvoiceData> groupedLoanInvoices = loanInvoiceDataList.stream()
                    .filter(i -> i.getTotalOutstanding().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.groupingBy(li1 -> Arrays.asList(li1.getClientId(), li1.getProductTypeName()),
                            Collectors.collectingAndThen(Collectors.toList(), list -> {
                                final BigDecimal outstandingPrincipal = list.stream().map(LoanInvoiceData::getOutstandingPrincipal)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                final BigDecimal currentInterest = list.stream().map(LoanInvoiceData::getCurrentInterest)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                final BigDecimal overdueInterest = list.stream().map(LoanInvoiceData::getOverdueInterest)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                final BigDecimal outstandingPenalty = list.stream().map(LoanInvoiceData::getOutstandingPenalty)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                final BigDecimal outstandingMandatoryInsurance = list.stream()
                                        .map(LoanInvoiceData::getOutstandingMandatoryInsurance).reduce(BigDecimal.ZERO, BigDecimal::add);
                                final BigDecimal outstandingVoluntaryInsurance = list.stream()
                                        .map(LoanInvoiceData::getOutstandingVoluntaryInsurance).reduce(BigDecimal.ZERO, BigDecimal::add);
                                final BigDecimal outstandingAval = list.stream().map(LoanInvoiceData::getOutstandingAval)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                final BigDecimal outstandingHonorarios = list.stream().map(LoanInvoiceData::getOutstandingHonorarios)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                final BigDecimal totalOutstanding = list.stream().map(LoanInvoiceData::getTotalOutstanding)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                final Integer loansCount = list.size();
                                final LoanInvoiceData loanInvoiceData = list.get(0);
                                final Long invoiceNumber = loanInvoiceData.getLastInvoiceNumber() + 1;
                                return LoanInvoiceData.builder().clientIdNumber(loanInvoiceData.getClientIdNumber())
                                        .clientDisplayName(loanInvoiceData.getClientDisplayName())
                                        .clientLastName(loanInvoiceData.getClientLastName())
                                        .clientLegalForm(loanInvoiceData.getClientLegalForm()).clientId(loanInvoiceData.getClientId())
                                        .clientEmailAddress(loanInvoiceData.getClientEmailAddress()).loanId(loanInvoiceData.getLoanId())
                                        .productTypeId(loanInvoiceData.getProductTypeId())
                                        .productTypeName(loanInvoiceData.getProductTypeName())
                                        .overdueSinceDate(loanInvoiceData.getOverdueSinceDate())
                                        .daysInArrears(loanInvoiceData.getDaysInArrears()).outstandingPrincipal(outstandingPrincipal)
                                        .currentInterest(currentInterest).overdueInterest(overdueInterest)
                                        .outstandingPenalty(outstandingPenalty).outstandingMandatoryInsurance(outstandingMandatoryInsurance)
                                        .outstandingVoluntaryInsurance(outstandingVoluntaryInsurance).outstandingAval(outstandingAval)
                                        .outstandingHonorarios(outstandingHonorarios).totalOutstanding(totalOutstanding)
                                        .loansCount(loansCount).firstDayOfMonth(firstDayOfMonth).lastDayOfMonth(lastDayOfMonth)
                                        .documentNumber(invoiceNumber).loanProductName(loanInvoiceData.getLoanProductName())
                                        .companyNIT(loanInvoiceData.getCompanyNIT()).companyDeptCode(loanInvoiceData.getCompanyDeptCode())
                                        .companyDeptName(loanInvoiceData.getCompanyDeptName())
                                        .companyMunCode(loanInvoiceData.getCompanyMunCode())
                                        .companyMunName(loanInvoiceData.getCompanyMunName())
                                        .companyCityCode(loanInvoiceData.getCompanyCityCode())
                                        .companyCityName(loanInvoiceData.getCompanyCityName())
                                        .companyAddress(loanInvoiceData.getCompanyAddress())
                                        .companyTelephone(loanInvoiceData.getCompanyTelephone())
                                        .clientCedula(loanInvoiceData.getClientCedula()).clientAddress(loanInvoiceData.getClientAddress())
                                        .clientCityCode(loanInvoiceData.getClientCityCode())
                                        .clientCityName(loanInvoiceData.getClientCityName())
                                        .clientTelephone(loanInvoiceData.getClientTelephone()).build();
                            })))
                    .values().stream().toList();
            final List<FacturaElectronicaMensual> groupLoanInvoiceEntities = groupedLoanInvoices.stream().map(LoanInvoiceData::toEntity)
                    .toList();
            this.facturaElectronicMensualRepository.saveAllAndFlush(groupLoanInvoiceEntities);
        }
        return RepeatStatus.FINISHED;
    }

    private BigDecimal calculateInterestForDays(int daysInPeriod, BigDecimal interest, int days) {
        if (interest.doubleValue() == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(interest.doubleValue() / daysInPeriod * days);
    }
}
