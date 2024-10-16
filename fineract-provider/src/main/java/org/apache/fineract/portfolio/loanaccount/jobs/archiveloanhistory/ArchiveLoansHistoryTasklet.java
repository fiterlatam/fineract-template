package org.apache.fineract.portfolio.loanaccount.jobs.archiveloanhistory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSales;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSalesRepository;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.portfolio.accountdetails.service.AccountEnumerations;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.delinquency.service.DelinquencyReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.CollectionData;
import org.apache.fineract.portfolio.loanaccount.data.LoanArchiveHistoryData;
import org.apache.fineract.portfolio.loanaccount.domain.*;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.service.LoanArchiveHistoryReadWritePlatformService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class ArchiveLoansHistoryTasklet implements Tasklet {

    private LoanArchiveHistoryReadWritePlatformService loanArchiveHistoryService;
    private LoanArchiveHistoryRepository loanArchiveHistoryRepository;
    private LoanRepositoryWrapper loanRepository;
    private DelinquencyReadPlatformService delinquencyReadPlatformService;
    private ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository;
    private CodeValueRepository codeValueRepository;

    public ArchiveLoansHistoryTasklet(LoanArchiveHistoryReadWritePlatformService loanArchiveHistoryService,
            LoanArchiveHistoryRepository loanArchiveHistoryRepository, LoanRepositoryWrapper loanRepository,
            DelinquencyReadPlatformService delinquencyReadPlatformService,
            ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository, CodeValueRepository codeValueRepository) {
        this.loanArchiveHistoryService = loanArchiveHistoryService;
        this.loanArchiveHistoryRepository = loanArchiveHistoryRepository;
        this.loanRepository = loanRepository;
        this.delinquencyReadPlatformService = delinquencyReadPlatformService;
        this.clientAllyPointOfSalesRepository = clientAllyPointOfSalesRepository;
        this.codeValueRepository = codeValueRepository;

    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<Throwable> errors = new ArrayList<>();
        LocalDate archiveDate = DateUtils.getLocalDateOfTenant();
        log.info("Running Archivo de cartera {}", archiveDate);
        try {
            List<String> archiveLoanId = new ArrayList<String>();
            List<LoanArchiveHistoryData> listLoan = loanArchiveHistoryService.getLoanArchiveCollectionData();
            for (LoanArchiveHistoryData dataLoan : listLoan) {
                Loan loan = loanRepository.findOneWithNotFoundDetection(Long.valueOf(dataLoan.getNumeroObligacion()));
                if (loan != null) {
                    final CollectionData collectionData = this.delinquencyReadPlatformService.calculateLoanCollectionData(loan.getId());
                    final Long daysInArrears = collectionData.getPastDueDays();
                    LoanRepaymentScheduleInstallment currentInstallment = loan.getLastLoanRepaymentScheduleInstallment();
                    Optional<LoanArchiveHistory> existingLoanArchive = loanArchiveHistoryRepository
                            .findByTitle("Archive Loan " + loan.getId());

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
                                || termVariations.getTermType().isInterestRateFromInstallment()
                                || termVariations.getTermType().isExtendRepaymentPeriod()) {
                            numberReschedule = numberReschedule + 1;
                        }
                    }
                    LoanTransaction transaction = loan.getLatestTransaction();
                    Optional<ClientAllyPointOfSales> clientAllPointOfsales = clientAllyPointOfSalesRepository
                            .findByCode(transaction.getPaymentDetail().getPointOfSalesCode());
                    String brand = "";
                    String ally = "";
                    String cityPoinfsales = "";

                    if (clientAllPointOfsales.isPresent()) {
                        ClientAllyPointOfSales clientAllyPointOfSales = clientAllPointOfsales.get();
                        ally = clientAllyPointOfSales.getClientAlly().getCompanyName();
                        Optional<CodeValue> getbrand = codeValueRepository.findById(clientAllyPointOfSales.getBrandCodeValueId());
                        if (getbrand.isPresent()) {
                            CodeValue brands = getbrand.get();
                            brand = brands.getLabel();
                        }
                        Optional<CodeValue> getCity = codeValueRepository.findById(clientAllyPointOfSales.getCityCodeValueId());
                        if (getCity.isPresent()) {
                            CodeValue citys = getCity.get();
                            cityPoinfsales = citys.getLabel();
                        }
                    }

                    if (existingLoanArchive.isPresent()) {
                        LoanArchiveHistory existingEntry = existingLoanArchive.get();

                        existingEntry.setIdentificacion(dataLoan.getIdentificacion());
                        existingEntry.setPrimerNombre(dataLoan.getPrimerNombre());
                        existingEntry.setSegundoNombre(dataLoan.getSegundoNombre());
                        existingEntry.setSegundoApellido(dataLoan.getSegundoApellido());
                        existingEntry.setEstadoCliente(dataLoan.getEstadoCliente());
                        existingEntry.setNumeroObligacion(dataLoan.getNumeroObligacion() + "+" + currentInstallment.getInstallmentNumber());
                        existingEntry.setNitEmpresa(dataLoan.getNitEmpresa());
                        existingEntry.setTelefonoSac(dataLoan.getTelefonoSac());
                        existingEntry.setCelularSac(dataLoan.getCelularSac());
                        existingEntry.setEmailSac(dataLoan.getEmailSac());
                        existingEntry.setDireccionSac(dataLoan.getDireccionSac());
                        existingEntry.setBarrioSac(dataLoan.getBarrioSac());
                        existingEntry.setCiudadSac(dataLoan.getCiudadSac());
                        existingEntry.setCiudadSac(dataLoan.getCiudadSac());
                        existingEntry.setDepartamento(dataLoan.getDepartamento());
                        existingEntry.setRazonSocial(dataLoan.getRazonSocial());
                        existingEntry.setNombreFamiliar(dataLoan.getNombreFamiliar());
                        existingEntry.setGenero(dataLoan.getGenero());
                        existingEntry.setEmpresaLabora(dataLoan.getEmpresaLabora());
                        existingEntry.setIngresos(dataLoan.getIngresos());
                        existingEntry.setAntiguedadCliente(LocalDate.parse(dataLoan.getAntiguedadCliente()));
                        existingEntry.setDiasMora(daysInArrears);
                        existingEntry.setFechaVencimiento(currentInstallment.getDueDate());
                        existingEntry.setTipoCredito(AccountEnumerations.loanType(loan.getLoanType()).toString());
                        existingEntry.setValorCuota(loan.getLoanSummary().getTotalOutstanding());
                        existingEntry.setCapital(loan.getLoanSummary().getTotalPrincipalOutstanding());
                        existingEntry.setIntereses(loan.getLoanSummary().getTotalInterestOutstanding());
                        existingEntry.setInteresesDeMora(currentInstallment.getPenaltyChargesOutstanding(loan.getCurrency()).getAmount());
                        existingEntry.setSeguro(BigDecimal.ZERO);
                        existingEntry.setSegurosVoluntarios(BigDecimal.ZERO);
                        existingEntry.setPeriodicidad(PeriodFrequencyType.fromInt(loan.getTermPeriodFrequencyType()).name());
                        existingEntry.setEmpresaReporta("INTERCREDITO");
                        existingEntry.setAbono(BigDecimal.ZERO);
                        existingEntry.setActividadLaboral(dataLoan.getActividadLaboral());
                        existingEntry.setNumeroDeReprogramaciones(numberReschedule);
                        existingEntry.setCuoEstado(dataLoan.getCuoEstado());
                        existingEntry.setCreSaldo(dataLoan.getCreSaldo());
                        existingEntry.setCreSaldo(dataLoan.getCreSaldo());
                        existingEntry.setFechaNacimiento(LocalDate.parse(dataLoan.getFechaNacimiento()));
                        existingEntry.setEmpresa(ally);
                        existingEntry.setMarca(brand);
                        existingEntry.setCiudadPuntoCredito(cityPoinfsales);
                        existingEntry.setEstadoCuota("");
                        existingEntry.setIvaInteresDeMora(BigDecimal.ZERO);
                        existingEntry.setFechaFinanciacion(loan.getDisbursementDate());
                        existingEntry.setCiudadPuntoCredito("");
                    } else {
                        LoanArchiveHistory loanArchiveHistory = new LoanArchiveHistory();
                        loanArchiveHistory.setTitle("Archive Loan " + loan.getId());
                        loanArchiveHistory.setIdentificacion(dataLoan.getIdentificacion());
                        loanArchiveHistory.setPrimerNombre(dataLoan.getPrimerNombre());
                        loanArchiveHistory.setSegundoNombre(dataLoan.getSegundoNombre());
                        loanArchiveHistory.setSegundoApellido(dataLoan.getSegundoApellido());
                        loanArchiveHistory.setEstadoCliente(dataLoan.getEstadoCliente());
                        loanArchiveHistory
                                .setNumeroObligacion(dataLoan.getNumeroObligacion() + "+" + currentInstallment.getInstallmentNumber());
                        loanArchiveHistory.setNitEmpresa(dataLoan.getNitEmpresa());
                        loanArchiveHistory.setTelefonoSac(dataLoan.getTelefonoSac());
                        loanArchiveHistory.setCelularSac(dataLoan.getCelularSac());
                        loanArchiveHistory.setEmailSac(dataLoan.getEmailSac());
                        loanArchiveHistory.setDireccionSac(dataLoan.getDireccionSac());
                        loanArchiveHistory.setBarrioSac(dataLoan.getBarrioSac());
                        loanArchiveHistory.setCiudadSac(dataLoan.getCiudadSac());
                        loanArchiveHistory.setCiudadSac(dataLoan.getCiudadSac());
                        loanArchiveHistory.setDepartamento(dataLoan.getDepartamento());
                        loanArchiveHistory.setRazonSocial(dataLoan.getRazonSocial());
                        loanArchiveHistory.setNombreFamiliar(dataLoan.getNombreFamiliar());
                        loanArchiveHistory.setGenero(dataLoan.getGenero());
                        loanArchiveHistory.setEmpresaLabora(dataLoan.getEmpresaLabora());
                        loanArchiveHistory.setIngresos(dataLoan.getIngresos());
                        loanArchiveHistory.setAntiguedadCliente(LocalDate.parse(dataLoan.getAntiguedadCliente()));
                        loanArchiveHistory.setDiasMora(daysInArrears);
                        loanArchiveHistory.setFechaVencimiento(currentInstallment.getDueDate());
                        loanArchiveHistory.setTipoCredito(AccountEnumerations.loanType(loan.getLoanType()).toString());
                        loanArchiveHistory.setValorCuota(loan.getLoanSummary().getTotalOutstanding());
                        loanArchiveHistory.setCapital(loan.getLoanSummary().getTotalPrincipalOutstanding());
                        loanArchiveHistory.setIntereses(loan.getLoanSummary().getTotalInterestOutstanding());
                        loanArchiveHistory
                                .setInteresesDeMora(currentInstallment.getPenaltyChargesOutstanding(loan.getCurrency()).getAmount());
                        loanArchiveHistory.setSeguro(mandatoryInsuranceAmount);
                        loanArchiveHistory.setSegurosVoluntarios(voluntaryInsuranceAmount);
                        loanArchiveHistory.setPeriodicidad(PeriodFrequencyType.fromInt(loan.getTermPeriodFrequencyType()).name());
                        loanArchiveHistory.setEmpresaReporta("INTERCREDITO");
                        loanArchiveHistory.setAbono(BigDecimal.ZERO);
                        loanArchiveHistory.setActividadLaboral(dataLoan.getActividadLaboral());
                        loanArchiveHistory.setNumeroDeReprogramaciones(numberReschedule);
                        loanArchiveHistory.setCreSaldo(dataLoan.getCuoSaldo());
                        loanArchiveHistory.setCuoSaldo(dataLoan.getCuoSaldo());
                        loanArchiveHistory.setCuoEstado(dataLoan.getCuoEstado());
                        loanArchiveHistory.setFechaNacimiento(LocalDate.parse(dataLoan.getFechaNacimiento()));
                        loanArchiveHistory.setEmpresa(ally);
                        loanArchiveHistory.setMarca(brand);
                        loanArchiveHistory.setCiudadPuntoCredito(cityPoinfsales);
                        loanArchiveHistory.setEstadoCuota("");
                        loanArchiveHistory.setIvaInteresDeMora(BigDecimal.ZERO);
                        loanArchiveHistory.setFechaFinanciacion(loan.getDisbursementDate());
                        loanArchiveHistory.setCiudadPuntoCredito("");

                        loanArchiveHistoryRepository.save(loanArchiveHistory);
                    }
                    archiveLoanId.add(dataLoan.getNumeroObligacion() + "+" + currentInstallment.getInstallmentNumber());
                }
            }
            if (archiveLoanId.size() > 0) {
                List<LoanArchiveHistory> oldLoanArchiveHistories = loanArchiveHistoryRepository.findByNumeroObligacionNotIn(archiveLoanId);
                loanArchiveHistoryRepository.deleteAll(oldLoanArchiveHistories);
            }

        } catch (Exception e) {
            log.error("Failed to run Archivo de cartera  {}", archiveDate, e);
            errors.add(e);
        }
        if (!errors.isEmpty()) {
            throw new JobExecutionException(errors);
        }
        return RepeatStatus.FINISHED;
    }

}
