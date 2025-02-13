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
package org.apache.fineract.portfolio.loanaccount.jobs.archiveloanhistory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSales;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSalesRepository;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.portfolio.client.domain.ClientStatus;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.delinquency.service.DelinquencyReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.CollectionData;
import org.apache.fineract.portfolio.loanaccount.data.LoanArchiveHistoryData;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanArchiveHistory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanArchiveHistoryRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInstallmentCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTermVariations;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.service.LoanUtilService;
import org.apache.fineract.portfolio.loanproduct.domain.LoanCustomizationDetail;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Setter
public class LoanHistoryArchiver {

    private List<LoanArchiveHistoryData> loansForArchival;
    private final LoanArchiveHistoryRepository loanArchiveHistoryRepository;
    private final LoanRepositoryWrapper loanRepository;
    private final DelinquencyReadPlatformService delinquencyReadPlatformService;
    private final ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository;
    private final CodeValueRepository codeValueRepository;
    private final LoanUtilService loanUtilService;

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = Exception.class)
    public void archiveLoans() throws JobExecutionException {
        List<Throwable> errors = new ArrayList<>();
        if (!loansForArchival.isEmpty()) {
            log.info("Running Archivo de cartera for loans batch with maximum loanId {}",
                    Long.valueOf(loansForArchival.get(loansForArchival.size() - 1).getNumeroObligacion()));
            List<String> archiveLoanId = new ArrayList<>();
            for (LoanArchiveHistoryData dataLoan : loansForArchival) {
                Loan loan = loanRepository.findOneWithNotFoundDetection(Long.valueOf(dataLoan.getNumeroObligacion()));
                if (loan != null) {
                    final CollectionData collectionData = this.delinquencyReadPlatformService.calculateLoanCollectionData(loan.getId());
                    final Long daysInArrears = collectionData.getPastDueDays();
                    List<LoanRepaymentScheduleInstallment> currentInstallments = loan.getRepaymentScheduleInstallments();
                    List<LoanTransaction> listTransactionLoan = loan.getLoanTransactions();
                    Integer repaymentCount = 0;
                    for (LoanTransaction detailTransaction : listTransactionLoan) {
                        if (detailTransaction.getTypeOf().isRepayment()) {
                            repaymentCount = repaymentCount + 1;
                        }
                    }

                    for (LoanRepaymentScheduleInstallment currentInstallment : currentInstallments) {
                        Optional<LoanArchiveHistory> existingLoanArchive = loanArchiveHistoryRepository
                                .findByNumeroObligacion(dataLoan.getNumeroObligacion() + "+" + currentInstallment.getInstallmentNumber());
                        Collection<LoanCharge> mandatoryInsuranceCharges = loan.getLoanCharges().stream()
                                .filter(LoanCharge::isMandatoryInsurance).toList();
                        Collection<LoanCharge> voluntaryInsuranceCharges = loan.getLoanCharges().stream()
                                .filter(LoanCharge::isVoluntaryInsurance).toList();
                        Collection<LoanCharge> avalCharges = loan.getLoanCharges().stream().filter(LoanCharge::isAvalCharge).toList();
                        Collection<LoanCharge> ivaCharges = loan.getLoanCharges().stream()
                                .filter(LoanCharge::isCustomPercentageBasedOfAnotherCharge).toList();
                        BigDecimal mandatoryInsuranceAmount = mandatoryInsuranceCharges.stream()
                                .flatMap(lic -> lic.installmentCharges().stream())
                                .filter(lc -> Objects.equals(currentInstallment.getInstallmentNumber(),
                                        lc.getInstallment().getInstallmentNumber()))
                                .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal voluntaryInsuranceAmount = voluntaryInsuranceCharges.stream()
                                .flatMap(lic -> lic.installmentCharges().stream())
                                .filter(lc -> Objects.equals(currentInstallment.getInstallmentNumber(),
                                        lc.getInstallment().getInstallmentNumber()))
                                .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal avalAmount = avalCharges.stream().flatMap(lic -> lic.installmentCharges().stream()).filter(
                                lc -> Objects.equals(currentInstallment.getInstallmentNumber(), lc.getInstallment().getInstallmentNumber()))
                                .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

                        // Calculate term Charge
                        BigDecimal mandatoryInsuranceTermChargeAmount = ivaCharges.stream()
                                .filter(lc -> mandatoryInsuranceCharges.stream()
                                        .anyMatch(mic -> mic.getCharge().getId().equals(lc.getCharge().getParentChargeId())))
                                .flatMap(lic -> lic.installmentCharges().stream())
                                .filter(lc -> Objects.equals(currentInstallment.getInstallmentNumber(),
                                        lc.getInstallment().getInstallmentNumber()))
                                .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal voluntaryInsuranceTermChargeAmount = ivaCharges.stream()
                                .filter(lc -> voluntaryInsuranceCharges.stream()
                                        .anyMatch(mic -> mic.getCharge().getId().equals(lc.getCharge().getParentChargeId())))
                                .flatMap(lic -> lic.installmentCharges().stream())
                                .filter(lc -> Objects.equals(currentInstallment.getInstallmentNumber(),
                                        lc.getInstallment().getInstallmentNumber()))
                                .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal avalTermChargeAmount = ivaCharges.stream()
                                .filter(lc -> avalCharges.stream()
                                        .anyMatch(mic -> mic.getCharge().getId().equals(lc.getCharge().getParentChargeId())))
                                .flatMap(lic -> lic.installmentCharges().stream())
                                .filter(lc -> Objects.equals(currentInstallment.getInstallmentNumber(),
                                        lc.getInstallment().getInstallmentNumber()))
                                .map(LoanInstallmentCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

                        mandatoryInsuranceAmount = mandatoryInsuranceAmount.add(mandatoryInsuranceTermChargeAmount);
                        voluntaryInsuranceAmount = voluntaryInsuranceAmount.add(voluntaryInsuranceTermChargeAmount);
                        avalAmount = avalAmount.add(avalTermChargeAmount);
                        Integer numberReschedule = 0;

                        for (LoanTermVariations termVariations : loan.getLoanTermVariations()) {
                            if (termVariations.getTermType().isRediferir() || termVariations.getTermType().isInterestRateVariation()
                                    || termVariations.getTermType().isExtendRepaymentPeriod()) {
                                numberReschedule = numberReschedule + 1;
                            }
                        }
                        LoanTransaction transaction = loan.getLastPaymentTransaction();
                        String brand = "";
                        String ally = "";
                        String cityPoinfsales = "";
                        String categoryPointOfSales = "";
                        String pointOfSale = "";
                        String estadoCivil = "";
                        String cityClient = " ";
                        String departamentoCity = " ";
                        String actividadLaboral = "";
                        String parentescoFamiliar = "";

                        if (transaction != null) {
                            if (transaction.getPaymentDetail() != null) {
                                Optional<ClientAllyPointOfSales> clientAllPointOfsales = clientAllyPointOfSalesRepository
                                        .findByCode(transaction.getPaymentDetail().getPointOfSalesCode());
                                if (clientAllPointOfsales.isPresent()) {
                                    ClientAllyPointOfSales clientAllyPointOfSales = clientAllPointOfsales.get();
                                    ally = clientAllyPointOfSales.getClientAlly().getCompanyName();
                                    pointOfSale = clientAllyPointOfSales.getName();
                                    Optional<CodeValue> getbrand = codeValueRepository
                                            .findById(clientAllyPointOfSales.getBrandCodeValueId());
                                    if (getbrand.isPresent()) {
                                        CodeValue brands = getbrand.get();
                                        brand = brands.getLabel();
                                    }
                                    Optional<CodeValue> getCity = codeValueRepository.findById(clientAllyPointOfSales.getCityCodeValueId());
                                    if (getCity.isPresent()) {
                                        CodeValue citys = getCity.get();
                                        cityPoinfsales = citys.getLabel();
                                    }
                                    Optional<CodeValue> getCategory = codeValueRepository
                                            .findById(clientAllyPointOfSales.getCategoryCodeValueId());
                                    if (getCategory.isPresent()) {
                                        CodeValue categoryCode = getCategory.get();
                                        categoryPointOfSales = categoryCode.getLabel();
                                    }
                                }
                            }
                        }

                        if (dataLoan.getEstadoCivil() != null) {
                            Optional<CodeValue> getEstadoCivil = codeValueRepository.findById(Long.parseLong(dataLoan.getEstadoCivil()));
                            if (getEstadoCivil.isPresent()) {
                                CodeValue estadoCivilCode = getEstadoCivil.get();
                                estadoCivil = estadoCivilCode.getLabel();
                            }
                        }
                        if (dataLoan.getCiudadSac() != null) {
                            Optional<CodeValue> getCiudadSac = codeValueRepository.findById(Long.valueOf(dataLoan.getCiudadSac()));
                            if (getCiudadSac.isPresent()) {
                                CodeValue ciudadSac = getCiudadSac.get();
                                cityClient = ciudadSac.getLabel();
                            }
                        }

                        if (dataLoan.getDepartamento() != null) {
                            Optional<CodeValue> getDepartamento = codeValueRepository.findById(Long.valueOf(dataLoan.getDepartamento()));
                            if (getDepartamento.isPresent()) {
                                CodeValue departamento = getDepartamento.get();
                                departamentoCity = departamento.getLabel();
                            }
                        }

                        if (dataLoan.getParentescoFamiliar() != null) {
                            Optional<CodeValue> getParentesco = codeValueRepository
                                    .findById(Long.valueOf(dataLoan.getParentescoFamiliar()));
                            if (getParentesco.isPresent()) {
                                CodeValue parentescoCode = getParentesco.get();
                                parentescoFamiliar = parentescoCode.getLabel();
                            }
                        }

                        if (dataLoan.getActividadLaboral() != null) {
                            Optional<CodeValue> getActividadLaboral = codeValueRepository
                                    .findById(Long.valueOf(dataLoan.getActividadLaboral()));
                            if (getActividadLaboral.isPresent()) {
                                CodeValue actividadLaboralCode = getActividadLaboral.get();
                                actividadLaboral = actividadLaboralCode.getLabel();
                            }
                        }

                        if (actividadLaboral.equals("PENSIONADO")) {
                            if (dataLoan.getCiudadPuntoCredito() != null) {
                                Optional<CodeValue> getDepartamento = codeValueRepository
                                        .findById(Long.valueOf(dataLoan.getCiudadPuntoCredito()));
                                if (getDepartamento.isPresent()) {
                                    CodeValue departamento = getDepartamento.get();
                                    String departamentoScore = departamento.getScore();
                                    Optional<CodeValue> personalDepartemento = codeValueRepository
                                            .findCiudadAndDepartamentoData(departamentoScore);
                                    if (personalDepartemento.isPresent()) {
                                        departamentoCity = personalDepartemento.get().getLabel();
                                    }
                                }
                            }
                        }
                        final ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, null);
                        final LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment = loan
                                .fetchLoanForeclosureDetail(LocalDate.now(), scheduleGeneratorDTO);
                        BigDecimal creSaldo = loanRepaymentScheduleInstallment.getTotalOutstanding(loan.getCurrency()).getAmount();

                        String creEstad = ClientStatus.fromInt(loan.getClient().getStatus()).name();
                        if (loan.getLoanCustomizationDetail() != null) {
                            LoanCustomizationDetail loanCustomizationDetail = loan.getLoanCustomizationDetail();
                            if (loanCustomizationDetail.getBlockStatus() != null) {
                                creEstad = "BLOCKING";
                            }
                        }

                        String estadoCuota = loan.getStatus().name();
                        String cuoEstado = estadoCuota;

                        String estadoCliente = dataLoan.getEstadoCliente();
                        if (estadoCliente == null || estadoCliente.equals("CASTIGO")) {
                            estadoCliente = ClientStatus.fromInt(loan.getClient().getStatus()).name();
                        }

                        BigDecimal writtenOffPrincipal = currentInstallment.getPrincipalWrittenOff(loan.getCurrency()).getAmount();
                        BigDecimal writtenOffInterest = currentInstallment.getInterestWrittenOff(loan.getCurrency()).getAmount();
                        BigDecimal writtenOffFees = currentInstallment.getFeeChargesWrittenOff(loan.getCurrency()).getAmount();
                        BigDecimal writtenOffPenalties = currentInstallment.getPenaltyChargesWrittenOff(loan.getCurrency()).getAmount();
                        BigDecimal totalWrittenOff = writtenOffPrincipal.add(writtenOffInterest).add(writtenOffFees)
                                .add(writtenOffPenalties);

                        if (existingLoanArchive.isPresent()) {

                            LoanArchiveHistory existingEntry = existingLoanArchive.get();
                            existingEntry.setIdentificacion(dataLoan.getNitEmpresa());
                            existingEntry.setPrimerNombre(dataLoan.getPrimerNombre());
                            existingEntry.setSegundoNombre(dataLoan.getSegundoNombre());
                            existingEntry.setPrimerApellido(dataLoan.getPrimerApellido());
                            existingEntry.setSegundoApellido(dataLoan.getSegundoApellido());
                            existingEntry.setEstadoCliente(estadoCliente);
                            existingEntry
                                    .setNumeroObligacion(dataLoan.getNumeroObligacion() + "+" + currentInstallment.getInstallmentNumber());
                            existingEntry.setNitEmpresa("800139398");
                            existingEntry.setTelefonoSac(dataLoan.getTelefonoSac());
                            existingEntry.setCelularSac(dataLoan.getCelularSac());
                            existingEntry.setCelularReferencia(dataLoan.getCelularReferencia());
                            existingEntry.setEmailSac(dataLoan.getEmailSac());
                            existingEntry.setDireccionSac(dataLoan.getDireccionSac());
                            existingEntry.setBarrioSac("OTRO");
                            existingEntry.setCiudadSac(cityClient);
                            existingEntry.setTipoCredito(categoryPointOfSales);
                            existingEntry.setDepartamento(departamentoCity);
                            existingEntry.setRazonSocial(dataLoan.getRazonSocial());
                            existingEntry.setNombreFamiliar(dataLoan.getNombreFamiliar());
                            existingEntry.setGenero(dataLoan.getGenero());
                            existingEntry.setEmpresaLabora(dataLoan.getEmpresaLabora());
                            existingEntry.setIngresos(dataLoan.getIngresos());
                            if (dataLoan.getAntiguedadCliente() != null) {
                                existingEntry.setAntiguedadCliente(LocalDate.parse(dataLoan.getAntiguedadCliente()));
                            }
                            existingEntry.setDiasMora(daysInArrears);
                            existingEntry.setFechaVencimiento(currentInstallment.getDueDate());
                            existingEntry.setValorCuota(currentInstallment.getTotalOutstanding(loan.getCurrency()).getAmount()); // get
                            // outsanding
                            existingEntry.setCapital(currentInstallment.getPrincipal(loan.getCurrency()).getAmount());
                            existingEntry.setAval(avalAmount);
                            existingEntry.setIntereses(currentInstallment.getInterestCharged(loan.getCurrency()).getAmount());
                            existingEntry
                                    .setInteresesDeMora(currentInstallment.getPenaltyChargesOutstanding(loan.getCurrency()).getAmount());
                            existingEntry.setSeguro(mandatoryInsuranceAmount);
                            existingEntry.setSegurosVoluntarios(voluntaryInsuranceAmount);
                            existingEntry.setPeriodicidad(PeriodFrequencyType.fromInt(loan.getTermPeriodFrequencyType()).name());
                            existingEntry.setEmpresaReporta("INTERCREDITO");
                            existingEntry.setAbono(currentInstallment.getTotalPaid(loan.getCurrency()).getAmount());
                            existingEntry.setCondonaciones(totalWrittenOff);
                            existingEntry.setActividadLaboral(actividadLaboral);
                            existingEntry.setNumeroDeReprogramaciones(numberReschedule);
                            existingEntry.setCreSaldo(creSaldo);
                            existingEntry.setCuoSaldo(currentInstallment.getTotalOutstanding(loan.getCurrency()).getAmount());
                            existingEntry.setMontoInicial(loan.getApprovedPrincipal());
                            existingEntry.setCuoEstado(cuoEstado);
                            if (dataLoan.getFechaNacimiento() != null) {
                                existingEntry.setFechaNacimiento(LocalDate.parse(dataLoan.getFechaNacimiento()));
                            }

                            existingEntry.setEmpresa(ally);
                            existingEntry.setMarca(brand);
                            existingEntry.setCiudadPuntoCredito(cityPoinfsales);
                            existingEntry.setEstadoCuota(estadoCuota);
                            existingEntry.setIvaInteresDeMora(BigDecimal.ZERO);
                            existingEntry.setFechaFinanciacion(loan.getDisbursementDate());
                            existingEntry.setPuntoDeVenta(pointOfSale);
                            existingEntry.setTipoDocumento(dataLoan.getTipoDocumento());
                            existingEntry.setParentescoFamiliar(parentescoFamiliar);
                            existingEntry.setEstadoCivil(estadoCivil);
                            existingEntry.setNitEmpresaAliada(dataLoan.getNitEmpresaAliada());
                            existingEntry.setCreEstado(creEstad);
                            loanArchiveHistoryRepository.save(existingEntry);
                        } else {
                            LoanArchiveHistory loanArchiveHistory = new LoanArchiveHistory();
                            loanArchiveHistory.setTitle("Archive Loan " + loan.getId());
                            loanArchiveHistory.setIdentificacion(dataLoan.getNitEmpresa());
                            loanArchiveHistory.setPrimerNombre(dataLoan.getPrimerNombre());
                            loanArchiveHistory.setSegundoNombre(dataLoan.getSegundoNombre());
                            loanArchiveHistory.setPrimerApellido(dataLoan.getPrimerApellido());
                            loanArchiveHistory.setSegundoApellido(dataLoan.getSegundoApellido());
                            loanArchiveHistory.setEstadoCliente(estadoCliente);
                            loanArchiveHistory
                                    .setNumeroObligacion(dataLoan.getNumeroObligacion() + "+" + currentInstallment.getInstallmentNumber());
                            loanArchiveHistory.setNitEmpresa("800139398");
                            loanArchiveHistory.setTelefonoSac(dataLoan.getTelefonoSac());
                            loanArchiveHistory.setCelularSac(dataLoan.getCelularSac());
                            loanArchiveHistory.setCelularReferencia(dataLoan.getCelularReferencia());
                            loanArchiveHistory.setEmailSac(dataLoan.getEmailSac());
                            loanArchiveHistory.setDireccionSac(dataLoan.getDireccionSac());
                            loanArchiveHistory.setBarrioSac("OTRO");
                            loanArchiveHistory.setCiudadSac(cityClient);
                            loanArchiveHistory.setTipoCredito(categoryPointOfSales);
                            loanArchiveHistory.setEmpresaLabora(dataLoan.getEmpresaLabora());
                            loanArchiveHistory.setDepartamento(departamentoCity);
                            loanArchiveHistory.setMontoInicial(loan.getApprovedPrincipal());
                            loanArchiveHistory.setRazonSocial(dataLoan.getRazonSocial());
                            loanArchiveHistory.setNombreFamiliar(dataLoan.getNombreFamiliar());
                            loanArchiveHistory.setGenero(dataLoan.getGenero());
                            loanArchiveHistory.setIngresos(dataLoan.getIngresos());
                            if (dataLoan.getAntiguedadCliente() != null) {
                                loanArchiveHistory.setAntiguedadCliente(LocalDate.parse(dataLoan.getAntiguedadCliente()));
                            }
                            loanArchiveHistory.setDiasMora(daysInArrears);
                            loanArchiveHistory.setFechaVencimiento(currentInstallment.getDueDate());
                            loanArchiveHistory.setValorCuota(currentInstallment.getTotalOutstanding(loan.getCurrency()).getAmount());
                            loanArchiveHistory.setCapital(loan.getLoanSummary().getTotalPrincipalOutstanding());
                            loanArchiveHistory.setAval(avalAmount);
                            loanArchiveHistory.setIntereses(currentInstallment.getInterestCharged(loan.getCurrency()).getAmount());
                            loanArchiveHistory
                                    .setInteresesDeMora(currentInstallment.getPenaltyChargesOutstanding(loan.getCurrency()).getAmount());
                            loanArchiveHistory.setSeguro(mandatoryInsuranceAmount);
                            loanArchiveHistory.setSegurosVoluntarios(voluntaryInsuranceAmount);
                            loanArchiveHistory.setPeriodicidad(PeriodFrequencyType.fromInt(loan.getTermPeriodFrequencyType()).name());
                            loanArchiveHistory.setEmpresaReporta("INTERCREDITO");
                            loanArchiveHistory.setAbono(currentInstallment.getTotalPaid(loan.getCurrency()).getAmount());
                            loanArchiveHistory.setCondonaciones(totalWrittenOff);
                            loanArchiveHistory.setActividadLaboral(actividadLaboral);
                            loanArchiveHistory.setNumeroDeReprogramaciones(numberReschedule);
                            loanArchiveHistory.setCreSaldo(creSaldo);
                            loanArchiveHistory.setCuoSaldo(currentInstallment.getTotalOutstanding(loan.getCurrency()).getAmount());
                            loanArchiveHistory.setCuoEstado(cuoEstado);
                            if (dataLoan.getFechaNacimiento() != null) {
                                loanArchiveHistory.setFechaNacimiento(LocalDate.parse(dataLoan.getFechaNacimiento()));
                            }
                            loanArchiveHistory.setEmpresa(ally);
                            loanArchiveHistory.setMarca(brand);
                            loanArchiveHistory.setCiudadPuntoCredito(cityPoinfsales);
                            loanArchiveHistory.setEstadoCuota(estadoCuota);
                            loanArchiveHistory.setIvaInteresDeMora(BigDecimal.ZERO);
                            loanArchiveHistory.setFechaFinanciacion(loan.getDisbursementDate());
                            loanArchiveHistory.setPuntoDeVenta(pointOfSale);
                            loanArchiveHistory.setTipoDocumento(dataLoan.getTipoDocumento());
                            loanArchiveHistory.setEstadoCivil(estadoCivil);
                            loanArchiveHistory.setNitEmpresaAliada(dataLoan.getNitEmpresaAliada());
                            loanArchiveHistory.setCreEstado(creEstad);
                            loanArchiveHistoryRepository.save(loanArchiveHistory);
                        }
                        if (!currentInstallment.isObligationsMet()) {
                            archiveLoanId.add(dataLoan.getNumeroObligacion() + "+" + currentInstallment.getInstallmentNumber());
                        }

                    }
                }
            }
            if (archiveLoanId.size() > 0) {
                List<LoanArchiveHistory> oldLoanArchiveHistories = loanArchiveHistoryRepository.findByNumeroObligacionNotIn(archiveLoanId);
                loanArchiveHistoryRepository.deleteAll(oldLoanArchiveHistories);
            }
            if (!errors.isEmpty()) {
                log.error("Error occurred while running Archivo de cartera for loans batch with maximum loanId {}",
                        loansForArchival.get(loansForArchival.size() - 1).getNumeroObligacion());
                throw new JobExecutionException(errors);
            }
            log.info("Completed Archivo de cartera for loans batch with maximum loanId {}",
                    loansForArchival.get(loansForArchival.size() - 1).getNumeroObligacion());
        }
    }
}
