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
package org.apache.fineract.organisation.prequalification.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.codec.binary.Base64;
import org.apache.fineract.infrastructure.configuration.data.ExternalServicesPropertiesData;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesConstants;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.JsonParserHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.prequalification.data.BuroData;
import org.apache.fineract.organisation.prequalification.data.LoanAdditionalData;
import org.apache.fineract.organisation.prequalification.domain.BuroCheckClassification;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationMemberRepository;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationStatusLogRepository;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroup;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupMember;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatus;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatusLog;
import org.apache.fineract.organisation.prequalification.exception.DpiBuroChequeException;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class BureauValidationWritePlatformServiceImpl implements BureauValidationWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(BureauValidationWritePlatformServiceImpl.class);
    private final PlatformSecurityContext context;
    private final PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper;
    private final PreQualificationMemberRepository preQualificationMemberRepository;
    private final PreQualificationStatusLogRepository preQualificationStatusLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ExternalServicesPropertiesReadPlatformService externalServicePropertiesReadPlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final LoanProductRepository loanProductRepository;

    @Autowired
    public BureauValidationWritePlatformServiceImpl(PlatformSecurityContext context,
            final PreQualificationMemberRepository preQualificationMemberRepository,
            PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper,
            final PreQualificationStatusLogRepository preQualificationStatusLogRepository,
            ExternalServicesPropertiesReadPlatformService externalServicePropertiesReadPlatformService, FromJsonHelper fromApiJsonHelper,
            final LoanProductRepository loanProductRepository) {
        this.context = context;
        this.prequalificationGroupRepositoryWrapper = prequalificationGroupRepositoryWrapper;
        this.preQualificationMemberRepository = preQualificationMemberRepository;
        this.preQualificationStatusLogRepository = preQualificationStatusLogRepository;
        this.externalServicePropertiesReadPlatformService = externalServicePropertiesReadPlatformService;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.loanProductRepository = loanProductRepository;
    }

    @Override
    public CommandProcessingResult validatePrequalificationWithBureau(Long prequalificationId, JsonCommand command) {
        PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(prequalificationId);

        Integer fromStatus = prequalificationGroup.getStatus();
        AppUser addedBy = this.context.getAuthenticatedUserIfPresent();

        List<PrequalificationGroupMember> members = this.preQualificationMemberRepository
                .findAllByPrequalificationGroup(prequalificationGroup);
        for (PrequalificationGroupMember member : members) {
            final BuroData buroData = this.makeBureauCheckApiCall(member.getDpi());
            if (buroData == null) {
                throw new DpiBuroChequeException(member.getDpi());
            }
            if (buroData.getClassification() != null) {
                EnumOptionData enumOptionData = buroData.getClassification();
                member.updateBuroCheckStatus(enumOptionData.getId().intValue());
            }
            member.setNombre(buroData.getNombre());
            member.setTipo(buroData.getTipo());
            member.setNumero(buroData.getNumero());
            member.setEstado(buroData.getEstado());
            member.setFecha(buroData.getFecha());
            member.setCuentas(buroData.getCuentas());
            member.setResumen(buroData.getResumen());
            this.preQualificationMemberRepository.save(member);
        }
        prequalificationGroup.updateStatus(PrequalificationStatus.BURO_CHECKED);
        this.prequalificationGroupRepositoryWrapper.save(prequalificationGroup);

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(addedBy, fromStatus, prequalificationGroup.getStatus(),
                null, prequalificationGroup);

        this.preQualificationStatusLogRepository.saveAndFlush(statusLog);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdAsString(prequalificationGroup.getId().toString()) //
                .withEntityId(prequalificationGroup.getId()) //
                .build();
    }

    private BuroData makeBureauCheckApiCall(final String dpi) {
        BuroData.BuroDataBuilder buroDataBuilder = BuroData.builder();
        final Collection<ExternalServicesPropertiesData> externalServicesPropertiesDatas = this.externalServicePropertiesReadPlatformService
                .retrieveOne(ExternalServicesConstants.DPI_BURO_CHECK_SERVICE_NAME);
        String dpiBuroCheckApiUsername = null;
        String dpiBuroCheckApiPassword = null;
        String dpiBuroCheckApiHost = null;
        for (final ExternalServicesPropertiesData externalServicesPropertiesData : externalServicesPropertiesDatas) {
            if ("dpiBuroCheckApiUsername".equalsIgnoreCase(externalServicesPropertiesData.getName())) {
                dpiBuroCheckApiUsername = externalServicesPropertiesData.getValue();
            } else if ("dpiBuroCheckApiPassword".equalsIgnoreCase(externalServicesPropertiesData.getName())) {
                dpiBuroCheckApiPassword = externalServicesPropertiesData.getValue();
            } else if ("dpiBuroCheckApiHost".equalsIgnoreCase(externalServicesPropertiesData.getName())) {
                dpiBuroCheckApiHost = externalServicesPropertiesData.getValue();
            }
        }
        final String credentials = dpiBuroCheckApiUsername + ":" + dpiBuroCheckApiPassword;
        final String basicAuth = new String(Base64.encodeBase64(credentials.getBytes(Charset.defaultCharset())), Charset.defaultCharset());
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(List.of(MediaType.ALL));
        httpHeaders.add("Authorization", "Basic " + basicAuth);
        final String url = dpiBuroCheckApiHost + "?DPI=" + dpi;
        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(httpHeaders), String.class);
        } catch (ResourceAccessException ex) {
            LOG.debug("DPI Buro Check Provider {} not available", url, ex);
        }

        if (responseEntity == null || !responseEntity.getStatusCode().equals(HttpStatus.OK)) {
            throw new PlatformDataIntegrityException("error.msg.mobile.service.provider.not.available", "DPI Buro Check Provider.");
        }
        if (responseEntity.hasBody()) {
            final JsonElement jsonElement = this.fromApiJsonHelper.parse(responseEntity.getBody());
            if (jsonElement.isJsonObject()) {
                final String dateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
                final String localeAsString = "en";
                final Locale locale = JsonParserHelper.localeFromString(localeAsString);
                final String classificationLetter = this.fromApiJsonHelper.extractStringNamed("Clasificacion", jsonElement);
                final String cuentas = this.fromApiJsonHelper.extractStringNamed("Cuentas", jsonElement);
                final String resumen = this.fromApiJsonHelper.extractStringNamed("Resumen", jsonElement);
                final JsonArray identificacions = this.fromApiJsonHelper.extractJsonArrayNamed("Identificacion", jsonElement);
                if (identificacions != null && !identificacions.isEmpty()) {
                    final JsonElement identificacionJson = identificacions.get(0);
                    final String tipo = this.fromApiJsonHelper.extractStringNamed("Tipo", identificacionJson);
                    final String numero = this.fromApiJsonHelper.extractStringNamed("Numero", identificacionJson);
                    final String estado = this.fromApiJsonHelper.extractStringNamed("Estado", identificacionJson);
                    buroDataBuilder.tipo(tipo).numero(numero).estado(estado);
                }
                final LocalDateTime fetcha = this.fromApiJsonHelper.extractLocalDateTimeNamed("Fecha", jsonElement, dateTimeFormat, locale);
                EnumOptionData enumOptionData = BuroCheckClassification
                        .status(BuroCheckClassification.fromLetter(classificationLetter).getId());
                buroDataBuilder.classification(enumOptionData).fecha(fetcha).cuentas(cuentas).resumen(resumen).build();
            }
        }

        return buroDataBuilder.build();
    }

    @Override
    public LoanAdditionalData retrieveAdditionProperties(final Long productId, final Long clientId, final String caseId) {
        final Collection<ExternalServicesPropertiesData> externalServicesPropertiesDatas = this.externalServicePropertiesReadPlatformService
                .retrieveOne(ExternalServicesConstants.LOAN_ADDITIONAL_PROPERTIES_SERVICE_NAME);
        final LoanProduct loanProduct = this.loanProductRepository.findById(productId)
                .orElseThrow(() -> new LoanProductNotFoundException(productId));
        String loanAdditionalsApiUsername = null;
        String loanAdditionalsApiPassword = null;
        String loanAdditionalsApiHost = null;
        for (final ExternalServicesPropertiesData externalServicesPropertiesData : externalServicesPropertiesDatas) {
            if ("loanAdditionalsApiUsername".equalsIgnoreCase(externalServicesPropertiesData.getName())) {
                loanAdditionalsApiUsername = externalServicesPropertiesData.getValue();
            } else if ("loanAdditionalsApiPassword".equalsIgnoreCase(externalServicesPropertiesData.getName())) {
                loanAdditionalsApiPassword = externalServicesPropertiesData.getValue();
            } else if ("loanAdditionalsApiHost".equalsIgnoreCase(externalServicesPropertiesData.getName())) {
                loanAdditionalsApiHost = externalServicesPropertiesData.getValue();
            }
        }
        final String credentials = loanAdditionalsApiUsername + ":" + loanAdditionalsApiPassword;
        final String basicAuth = new String(Base64.encodeBase64(credentials.getBytes(Charset.defaultCharset())), Charset.defaultCharset());
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(List.of(MediaType.ALL));
        httpHeaders.add("Authorization", "Basic " + basicAuth);
        final String url = loanAdditionalsApiHost + "?caseid=" + caseId;
        ResponseEntity<String> responseEntity = null;
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            responseEntity = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(httpHeaders), String.class);
        } catch (RestClientException ex) {
            LOG.debug("Loan Additional API Provider {} not available", url, ex);
        }
        if (responseEntity == null || !responseEntity.getStatusCode().equals(HttpStatus.OK)) {
            throw new PlatformDataIntegrityException("error.msg.external.service.provider.not.available",
                    "Loan additional properties API Provider is not available");
        }
        LoanAdditionalData loanAdditionalData = null;
        if (responseEntity.hasBody()) {
            final JsonElement jsonElement = this.fromApiJsonHelper.parse(responseEntity.getBody());
            if (jsonElement.isJsonObject()) {
                final String error = this.fromApiJsonHelper.extractStringNamed("Error", jsonElement);
                if (error != null) {
                    throw new PlatformDataIntegrityException("error.msg.external.service.provider.error.occurred",
                            error + " Case ID == " + caseId);
                }
                loanAdditionalData = this.mapFromJson(jsonElement);
            }
        }
        if (loanAdditionalData == null) {
            throw new PlatformDataIntegrityException("error.msg.loan.additional.not.found",
                    "Loan additional properties Not Found for Case ID " + caseId);
        }
        if (!(loanProduct.getName().equalsIgnoreCase(loanAdditionalData.getProducto())
                || loanProduct.getName().equalsIgnoreCase(loanAdditionalData.getPrograma()))) {
            throw new PlatformDataIntegrityException("error.msg.selected.loan.product.not.same.with.the.case.id",
                    "Loan additional properties Not Found for Case ID " + caseId);
        }
        return loanAdditionalData;
    }

    public LoanAdditionalData mapFromJson(final JsonElement jsonElement) {
        final LoanAdditionalData loanAdditionalData = new LoanAdditionalData();
        final String dateFormat = "yyyy-MM-dd";
        final String dateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
        final String localeAsString = "en";
        final Locale locale = JsonParserHelper.localeFromString(localeAsString);

        final String caseId = this.fromApiJsonHelper.extractStringNamed("case_id", jsonElement);
        loanAdditionalData.setCaseId(caseId);

        final Integer ciclosCancelados = this.fromApiJsonHelper.extractIntegerNamed("Ciclos_Cancelados", jsonElement, locale);
        loanAdditionalData.setCiclosCancelados(ciclosCancelados);

        final Long branchCode = this.fromApiJsonHelper.extractLongNamed("branch_code", jsonElement);
        loanAdditionalData.setBranchCode(branchCode);

        final String cargoTesorera = this.fromApiJsonHelper.extractStringNamed("cargoTesorera", jsonElement);
        loanAdditionalData.setCargoTesorera(cargoTesorera);

        final String cargo = this.fromApiJsonHelper.extractStringNamed("cargo", jsonElement);
        loanAdditionalData.setCargo(cargo);

        final String estadoSolicitud = this.fromApiJsonHelper.extractStringNamed("estado_solicitud", jsonElement);
        loanAdditionalData.setEstadoSolicitud(estadoSolicitud);

        final LocalDate fechaInicio = this.fromApiJsonHelper.extractLocalDateNamed("fecha_inicio", jsonElement, dateFormat, locale);
        loanAdditionalData.setFechaInicio(fechaInicio);

        final String producto = this.fromApiJsonHelper.extractStringNamed("producto", jsonElement);
        loanAdditionalData.setProducto(producto);

        final LocalDate fechaSolicitud = this.fromApiJsonHelper.extractLocalDateNamed("Fecha_Solicitud", jsonElement, dateFormat, locale);
        loanAdditionalData.setFechaSolicitud(fechaSolicitud);

        final String codigoCliente = this.fromApiJsonHelper.extractStringNamed("codigo_cliente", jsonElement);
        loanAdditionalData.setCodigoCliente(codigoCliente);

        final String actividadNegocio = this.fromApiJsonHelper.extractStringNamed("actividad_negocio", jsonElement);
        loanAdditionalData.setActividadNegocio(actividadNegocio);

        final BigDecimal activoCorriente = this.fromApiJsonHelper.extractBigDecimalNamed("activo_corriente", jsonElement, locale);
        loanAdditionalData.setActivoCorriente(activoCorriente);

        final BigDecimal activoNocorriente = this.fromApiJsonHelper.extractBigDecimalNamed("activo_no_corriente", jsonElement, locale);
        loanAdditionalData.setActivoNocorriente(activoNocorriente);

        final BigDecimal alimentacion = this.fromApiJsonHelper.extractBigDecimalNamed("alimentacion", jsonElement, locale);
        loanAdditionalData.setAlimentacion(alimentacion);

        final BigDecimal alquilerCliente = this.fromApiJsonHelper.extractBigDecimalNamed("alquiler_cliente", jsonElement, locale);
        loanAdditionalData.setAlquilerCliente(alquilerCliente);

        final BigDecimal alquilerGasto = this.fromApiJsonHelper.extractBigDecimalNamed("alquiler_gasto", jsonElement, locale);
        loanAdditionalData.setAlquilerGasto(alquilerGasto);

        final BigDecimal alquilerLocal = this.fromApiJsonHelper.extractBigDecimalNamed("alquiler_local", jsonElement, locale);
        loanAdditionalData.setAlquilerLocal(alquilerLocal);

        final String antiguedadNegocio = this.fromApiJsonHelper.extractStringNamed("antiguedad_negocio", jsonElement);
        loanAdditionalData.setAntiguedadNegocio(antiguedadNegocio);

        final String apoyoFamilia = this.fromApiJsonHelper.extractStringNamed("apoyo_familia", jsonElement);
        loanAdditionalData.setApoyoFamilia(apoyoFamilia);

        final Integer aprobacionesBc = this.fromApiJsonHelper.extractIntegerNamed("aprobaciones_bc", jsonElement, locale);
        loanAdditionalData.setAprobacionesBc(aprobacionesBc);

        final String area = this.fromApiJsonHelper.extractStringNamed("area", jsonElement);
        loanAdditionalData.setArea(area);

        final Integer bienesInmuebles = this.fromApiJsonHelper.extractIntegerNamed("bienes_inmuebles", jsonElement, locale);
        loanAdditionalData.setBienesInmuebles(bienesInmuebles);

        final Integer bienesInmueblesFamiliares = this.fromApiJsonHelper.extractIntegerNamed("bienes_inmuebles_familiares", jsonElement,
                locale);
        loanAdditionalData.setBienesInmueblesFamiliares(bienesInmueblesFamiliares);

        final String cDpi = this.fromApiJsonHelper.extractStringNamed("c_dpi", jsonElement);
        loanAdditionalData.setCDpi(cDpi);

        final Integer cEdad = this.fromApiJsonHelper.extractIntegerNamed("c_edad", jsonElement, locale);
        loanAdditionalData.setCEdad(cEdad);

        final LocalDate cFechaNacimiento = this.fromApiJsonHelper.extractLocalDateNamed("c_fecha_nacimiento", jsonElement, dateFormat,
                locale);
        loanAdditionalData.setCFechaNacimiento(cFechaNacimiento);

        final String cOtroNombre = this.fromApiJsonHelper.extractStringNamed("c_otro_nombre", jsonElement);
        loanAdditionalData.setCOtroNombre(cOtroNombre);

        final String cPrimerApellido = this.fromApiJsonHelper.extractStringNamed("c_primer_apellido", jsonElement);
        loanAdditionalData.setCPrimerApellido(cPrimerApellido);

        final String cPrimerNombre = this.fromApiJsonHelper.extractStringNamed("c_primer_nombre", jsonElement);
        loanAdditionalData.setCPrimerNombre(cPrimerNombre);

        final String cProfesion = this.fromApiJsonHelper.extractStringNamed("c_profesion", jsonElement);
        loanAdditionalData.setCProfesion(cProfesion);

        final String cSegundoApellido = this.fromApiJsonHelper.extractStringNamed("c_segundo_apellido", jsonElement);
        loanAdditionalData.setCSegundoApellido(cSegundoApellido);

        final String cSegundoNombre = this.fromApiJsonHelper.extractStringNamed("c_segundo_nombre", jsonElement);
        loanAdditionalData.setCSegundoNombre(cSegundoNombre);

        final String cTelefono = this.fromApiJsonHelper.extractStringNamed("c_telefono", jsonElement);
        loanAdditionalData.setCTelefono(cTelefono);

        final BigDecimal capacidadPago = this.fromApiJsonHelper.extractBigDecimalNamed("capacidad_pago", jsonElement, locale);
        loanAdditionalData.setCapacidadPago(capacidadPago);

        final BigDecimal comunalVigente = this.fromApiJsonHelper.extractBigDecimalNamed("comunal_vigente", jsonElement, locale);
        loanAdditionalData.setComunalVigente(comunalVigente);

        final BigDecimal costoUnitario = this.fromApiJsonHelper.extractBigDecimalNamed("costo_unitario", jsonElement, locale);
        loanAdditionalData.setCostoUnitario(costoUnitario);

        final BigDecimal costoVenta = this.fromApiJsonHelper.extractBigDecimalNamed("costo_venta", jsonElement, locale);
        loanAdditionalData.setCostoVenta(costoVenta);

        final BigDecimal cuantoPagar = this.fromApiJsonHelper.extractBigDecimalNamed("cuanto_pagar", jsonElement, locale);
        loanAdditionalData.setCuantoPagar(cuantoPagar);

        final BigDecimal cuentasPorPagar = this.fromApiJsonHelper.extractBigDecimalNamed("cuentas_por_pagar", jsonElement, locale);
        loanAdditionalData.setCuentasPorPagar(cuentasPorPagar);

        final Integer cuota = this.fromApiJsonHelper.extractIntegerNamed("cuota", jsonElement, locale);
        loanAdditionalData.setCuota(cuota);

        final Integer cuotaOtros = this.fromApiJsonHelper.extractIntegerNamed("cuota_otros", jsonElement, locale);
        loanAdditionalData.setCuotaOtros(cuotaOtros);

        final Integer cuotaPuente = this.fromApiJsonHelper.extractIntegerNamed("cuota_puente", jsonElement, locale);
        loanAdditionalData.setCuotaPuente(cuotaPuente);

        final Integer cuotasPendientesBc = this.fromApiJsonHelper.extractIntegerNamed("cuotas_pendientes_bc", jsonElement, locale);
        loanAdditionalData.setCuotasPendientesBc(cuotasPendientesBc);

        final Integer dependientes = this.fromApiJsonHelper.extractIntegerNamed("dependientes", jsonElement, locale);
        loanAdditionalData.setDependientes(dependientes);

        final String destinoPrestamo = this.fromApiJsonHelper.extractStringNamed("destino_prestamo", jsonElement);
        loanAdditionalData.setDestinoPrestamo(destinoPrestamo);

        final Integer educacion = this.fromApiJsonHelper.extractIntegerNamed("educacion", jsonElement, locale);
        loanAdditionalData.setEducacion(educacion);

        final BigDecimal efectivo = this.fromApiJsonHelper.extractBigDecimalNamed("efectivo", jsonElement, locale);
        loanAdditionalData.setEfectivo(efectivo);

        final BigDecimal endeudamientoActual = this.fromApiJsonHelper.extractBigDecimalNamed("endeudamiento_actual", jsonElement, locale);
        loanAdditionalData.setEndeudamientoActual(endeudamientoActual);

        final BigDecimal endeudamientoFuturo = this.fromApiJsonHelper.extractBigDecimalNamed("endeudamiento_futuro", jsonElement, locale);
        loanAdditionalData.setEndeudamientoFuturo(endeudamientoFuturo);

        final Integer enf = this.fromApiJsonHelper.extractIntegerNamed("enf", jsonElement, locale);
        loanAdditionalData.setEnf(enf);

        final String escribe = this.fromApiJsonHelper.extractStringNamed("escribe", jsonElement);
        loanAdditionalData.setEscribe(escribe);

        final String evolucionNegocio = this.fromApiJsonHelper.extractStringNamed("evolucion_negocio", jsonElement);
        loanAdditionalData.setEvolucionNegocio(evolucionNegocio);

        final String fPep = this.fromApiJsonHelper.extractStringNamed("f_pep", jsonElement);
        loanAdditionalData.setFPep(fPep);

        final Integer familiares = this.fromApiJsonHelper.extractIntegerNamed("familiares", jsonElement, locale);
        loanAdditionalData.setFamiliares(familiares);

        final LocalDate fechaPrimeraReunion = this.fromApiJsonHelper.extractLocalDateNamed("fecha_primera_reunion", jsonElement, dateFormat,
                locale);
        loanAdditionalData.setFechaPrimeraReunion(fechaPrimeraReunion);

        final Integer flujoDisponible = this.fromApiJsonHelper.extractIntegerNamed("flujo_disponible", jsonElement, locale);
        loanAdditionalData.setFlujoDisponible(flujoDisponible);

        final String garantiaPrestamo = this.fromApiJsonHelper.extractStringNamed("garantia_prestamo", jsonElement);
        loanAdditionalData.setGarantiaPrestamo(garantiaPrestamo);

        final BigDecimal gastosFamiliares = this.fromApiJsonHelper.extractBigDecimalNamed("gastos_familiares", jsonElement, locale);
        loanAdditionalData.setGastosFamiliares(gastosFamiliares);

        final BigDecimal gastosNegocio = this.fromApiJsonHelper.extractBigDecimalNamed("gastos_negocio", jsonElement, locale);
        loanAdditionalData.setGastosNegocio(gastosNegocio);

        final Integer herramientas = this.fromApiJsonHelper.extractIntegerNamed("herramientas", jsonElement, locale);
        loanAdditionalData.setHerramientas(herramientas);

        final Integer hijos = this.fromApiJsonHelper.extractIntegerNamed("hijos", jsonElement, locale);
        loanAdditionalData.setHijos(hijos);

        final Integer mortgages = this.fromApiJsonHelper.extractIntegerNamed("mortgages", jsonElement, locale);
        loanAdditionalData.setMortgages(mortgages);

        final Integer impuestos = this.fromApiJsonHelper.extractIntegerNamed("impuestos", jsonElement, locale);
        loanAdditionalData.setImpuestos(impuestos);

        final String ingresadoPor = this.fromApiJsonHelper.extractStringNamed("ingresado_por", jsonElement);
        loanAdditionalData.setIngresadoPor(ingresadoPor);

        final BigDecimal ingresoFamiliar = this.fromApiJsonHelper.extractBigDecimalNamed("ingreso_familiar", jsonElement, locale);
        loanAdditionalData.setIngresoFamiliar(ingresoFamiliar);

        final Integer integrantesAdicional = this.fromApiJsonHelper.extractIntegerNamed("integrantes_adicional", jsonElement, locale);
        loanAdditionalData.setIntegrantesAdicional(integrantesAdicional);

        final BigDecimal inventarios = this.fromApiJsonHelper.extractBigDecimalNamed("inventarios", jsonElement, locale);
        loanAdditionalData.setInventarios(inventarios);

        final BigDecimal inversionTotal = this.fromApiJsonHelper.extractBigDecimalNamed("inversion_total", jsonElement, locale);
        loanAdditionalData.setInversionTotal(inversionTotal);

        final String invertir = this.fromApiJsonHelper.extractStringNamed("invertir", jsonElement);
        loanAdditionalData.setInvertir(invertir);

        final String lee = this.fromApiJsonHelper.extractStringNamed("lee", jsonElement);
        loanAdditionalData.setLee(lee);

        final BigDecimal menajeDelHogar = this.fromApiJsonHelper.extractBigDecimalNamed("menaje_del_hogar", jsonElement, locale);
        loanAdditionalData.setMenajeDelHogar(menajeDelHogar);

        final BigDecimal mobiliarioYequipo = this.fromApiJsonHelper.extractBigDecimalNamed("mobiliario_y_equipo", jsonElement, locale);
        loanAdditionalData.setMobiliarioYequipo(mobiliarioYequipo);

        final BigDecimal montoSolicitado = this.fromApiJsonHelper.extractBigDecimalNamed("monto_solicitado", jsonElement, locale);
        loanAdditionalData.setMontoSolicitado(montoSolicitado);

        final String motivoSolicitud = this.fromApiJsonHelper.extractStringNamed("motivo_solicitud", jsonElement);
        loanAdditionalData.setMotivoSolicitud(motivoSolicitud);

        final String nit = this.fromApiJsonHelper.extractStringNamed("nit", jsonElement);
        loanAdditionalData.setNit(nit);

        final String nombrePropio = this.fromApiJsonHelper.extractStringNamed("nombre_propio", jsonElement);
        loanAdditionalData.setNombrePropio(nombrePropio);

        final BigDecimal pasivoCorriente = this.fromApiJsonHelper.extractBigDecimalNamed("pasivo_corriente", jsonElement, locale);
        loanAdditionalData.setPasivoCorriente(pasivoCorriente);

        final BigDecimal pasivoNoCorriente = this.fromApiJsonHelper.extractBigDecimalNamed("pasivo_no_Corriente", jsonElement, locale);
        loanAdditionalData.setPasivoNoCorriente(pasivoNoCorriente);

        final BigDecimal pensiones = this.fromApiJsonHelper.extractBigDecimalNamed("pensiones", jsonElement, locale);
        loanAdditionalData.setPensiones(pensiones);

        final String pep = this.fromApiJsonHelper.extractStringNamed("pep", jsonElement);
        loanAdditionalData.setPep(pep);

        final Integer plazo = this.fromApiJsonHelper.extractIntegerNamed("plazo", jsonElement, locale);
        loanAdditionalData.setPlazo(plazo);

        final Integer plazoVigente = this.fromApiJsonHelper.extractIntegerNamed("plazo_vigente", jsonElement, locale);
        loanAdditionalData.setPlazoVigente(plazoVigente);

        final String poseeCuenta = this.fromApiJsonHelper.extractStringNamed("posee_cuenta", jsonElement);
        loanAdditionalData.setPoseeCuenta(poseeCuenta);

        final Long prestamoPuente = this.fromApiJsonHelper.extractLongNamed("prestamo_puente", jsonElement);
        loanAdditionalData.setPrestamoPuente(prestamoPuente);

        final BigDecimal propuestaFacilitador = this.fromApiJsonHelper.extractBigDecimalNamed("propuesta_facilitador", jsonElement, locale);
        loanAdditionalData.setPropuestaFacilitador(propuestaFacilitador);

        final String puntoReunion = this.fromApiJsonHelper.extractStringNamed("punto_reunion", jsonElement);
        loanAdditionalData.setPuntoReunion(puntoReunion);

        final BigDecimal relacionGastos = this.fromApiJsonHelper.extractBigDecimalNamed("relacion_gastos", jsonElement, locale);
        loanAdditionalData.setRelacionGastos(relacionGastos);

        final BigDecimal rentabilidadNeta = this.fromApiJsonHelper.extractBigDecimalNamed("rentabilidad_neta", jsonElement, locale);
        loanAdditionalData.setRentabilidadNeta(rentabilidadNeta);

        final BigDecimal rotacionInventario = this.fromApiJsonHelper.extractBigDecimalNamed("rotacion_inventario", jsonElement, locale);
        loanAdditionalData.setRotacionInventario(rotacionInventario);

        final BigDecimal salarioCliente = this.fromApiJsonHelper.extractBigDecimalNamed("salario_cliente", jsonElement, locale);
        loanAdditionalData.setSalarioCliente(salarioCliente);

        final BigDecimal salarios = this.fromApiJsonHelper.extractBigDecimalNamed("salarios", jsonElement, locale);
        loanAdditionalData.setSalarios(salarios);

        final String salud = this.fromApiJsonHelper.extractStringNamed("salud", jsonElement);
        loanAdditionalData.setSalud(salud);

        final String servicios = this.fromApiJsonHelper.extractStringNamed("servicios", jsonElement);
        loanAdditionalData.setServicios(servicios);

        final BigDecimal serviciosBasicos = this.fromApiJsonHelper.extractBigDecimalNamed("servicios_basicos", jsonElement, locale);
        loanAdditionalData.setServiciosBasicos(serviciosBasicos);

        final BigDecimal serviciosGasto = this.fromApiJsonHelper.extractBigDecimalNamed("servicios_gasto", jsonElement, locale);
        loanAdditionalData.setServiciosGasto(serviciosGasto);

        final BigDecimal serviciosMedicos = this.fromApiJsonHelper.extractBigDecimalNamed("servicios_medicos", jsonElement, locale);
        loanAdditionalData.setServiciosMedicos(serviciosMedicos);

        final Integer tarjetas = this.fromApiJsonHelper.extractIntegerNamed("tarjetas", jsonElement, locale);
        loanAdditionalData.setTarjetas(tarjetas);

        final String tipoVivienda = this.fromApiJsonHelper.extractStringNamed("tipo_vivienda", jsonElement);
        loanAdditionalData.setTipoVivienda(tipoVivienda);

        final BigDecimal totalActivo = this.fromApiJsonHelper.extractBigDecimalNamed("total_activo", jsonElement, locale);
        loanAdditionalData.setTotalActivo(totalActivo);

        final BigDecimal totalIngresos = this.fromApiJsonHelper.extractBigDecimalNamed("total_ingresos", jsonElement, locale);
        loanAdditionalData.setTotalIngresos(totalIngresos);

        final BigDecimal totalIngresosFamiliares = this.fromApiJsonHelper.extractBigDecimalNamed("total_ingresos_familiares", jsonElement,
                locale);
        loanAdditionalData.setTotalIngresosFamiliares(totalIngresosFamiliares);

        final BigDecimal totalPasivo = this.fromApiJsonHelper.extractBigDecimalNamed("total_pasivo", jsonElement, locale);
        loanAdditionalData.setTotalPasivo(totalPasivo);

        final BigDecimal transporteGasto = this.fromApiJsonHelper.extractBigDecimalNamed("transporte_gasto", jsonElement, locale);
        loanAdditionalData.setTransporteGasto(transporteGasto);

        final BigDecimal transporteNegocio = this.fromApiJsonHelper.extractBigDecimalNamed("transporte_negocio", jsonElement, locale);
        loanAdditionalData.setTransporteNegocio(transporteNegocio);

        final String ubicacionCliente = this.fromApiJsonHelper.extractStringNamed("ubicacion_cliente", jsonElement);
        loanAdditionalData.setUbicacionCliente(ubicacionCliente);

        final String ubicacionNegocio = this.fromApiJsonHelper.extractStringNamed("ubicacion_negocio", jsonElement);
        loanAdditionalData.setUbicacionNegocio(ubicacionNegocio);

        final BigDecimal utilidadBruta = this.fromApiJsonHelper.extractBigDecimalNamed("utilidad_bruta", jsonElement, locale);
        loanAdditionalData.setUtilidadBruta(utilidadBruta);

        final BigDecimal utilidadNeta = this.fromApiJsonHelper.extractBigDecimalNamed("utilidad_neta", jsonElement, locale);
        loanAdditionalData.setUtilidadNeta(utilidadNeta);

        final Integer validFiador = this.fromApiJsonHelper.extractIntegerNamed("valid_fiador", jsonElement, locale);
        loanAdditionalData.setValidFiador(validFiador);

        final BigDecimal valorGarantia = this.fromApiJsonHelper.extractBigDecimalNamed("valor_garantia", jsonElement, locale);
        loanAdditionalData.setValorGarantia(valorGarantia);

        final Integer vehiculos = this.fromApiJsonHelper.extractIntegerNamed("vehiculos", jsonElement, locale);
        loanAdditionalData.setVehiculos(vehiculos);

        final BigDecimal vestimenta = this.fromApiJsonHelper.extractBigDecimalNamed("vestimenta", jsonElement, locale);
        loanAdditionalData.setVestimenta(vestimenta);

        final String visitoNegocio = this.fromApiJsonHelper.extractStringNamed("visito_negocio", jsonElement);
        loanAdditionalData.setVisitoNegocio(visitoNegocio);

        final String externalId = this.fromApiJsonHelper.extractStringNamed("external_id", jsonElement);
        loanAdditionalData.setExternalId(externalId);

        final String ownerId = this.fromApiJsonHelper.extractStringNamed("owner_id", jsonElement);
        loanAdditionalData.setOwnerId(ownerId);

        final String caseName = this.fromApiJsonHelper.extractStringNamed("case_name", jsonElement);
        loanAdditionalData.setCaseName(caseName);

        final LocalDateTime dateOpened = this.fromApiJsonHelper.extractLocalDateTimeNamed("date_opened", jsonElement, dateTimeFormat,
                locale);
        loanAdditionalData.setDateOpened(dateOpened);

        final LocalDate fechaFin = this.fromApiJsonHelper.extractLocalDateNamed("fecha_fin", jsonElement, dateFormat, locale);
        loanAdditionalData.setFechaFin(fechaFin);

        final BigDecimal ventas = this.fromApiJsonHelper.extractBigDecimalNamed("ventas", jsonElement, locale);
        loanAdditionalData.setVentas(ventas);

        final BigDecimal cuentasPorCobrar = this.fromApiJsonHelper.extractBigDecimalNamed("cuentas_por_cobrar", jsonElement, locale);
        loanAdditionalData.setCuentasPorCobrar(cuentasPorCobrar);

        final BigDecimal hipotecas = this.fromApiJsonHelper.extractBigDecimalNamed("hipotecas", jsonElement, locale);
        loanAdditionalData.setHipotecas(hipotecas);

        final String excepcion = this.fromApiJsonHelper.extractStringNamed("excepcion", jsonElement);
        loanAdditionalData.setExcepcion(excepcion);

        final Integer tipoExcepcion = this.fromApiJsonHelper.extractIntegerNamed("tipo_excepcion", jsonElement, locale);
        loanAdditionalData.setTipoExcepcion(tipoExcepcion);

        final String descripcionExcepcion = this.fromApiJsonHelper.extractStringNamed("descripcion_excepcion", jsonElement);
        loanAdditionalData.setDescripcionExcepcion(descripcionExcepcion);

        final BigDecimal montoAutorizado = this.fromApiJsonHelper.extractBigDecimalNamed("monto_autorizado", jsonElement, locale);
        loanAdditionalData.setMontoAutorizado(montoAutorizado);

        final String observaciones = this.fromApiJsonHelper.extractStringNamed("observaciones", jsonElement);
        loanAdditionalData.setObservaciones(observaciones);

        final BigDecimal montoOtrosIngresos = this.fromApiJsonHelper.extractBigDecimalNamed("monto_otros_ingresos", jsonElement, locale);
        loanAdditionalData.setMontoOtrosIngresos(montoOtrosIngresos);

        final BigDecimal capitalDdeTrabajo = this.fromApiJsonHelper.extractBigDecimalNamed("capital_de_trabajo", jsonElement, locale);
        loanAdditionalData.setCapitalDdeTrabajo(capitalDdeTrabajo);

        final String origenOtrosIngresos = this.fromApiJsonHelper.extractStringNamed("origen_otros_ingresos", jsonElement);
        loanAdditionalData.setOrigenOtrosIngresos(origenOtrosIngresos);

        final String otrosIngresos = this.fromApiJsonHelper.extractStringNamed("otros_ingresos", jsonElement);
        loanAdditionalData.setOtrosIngresos(otrosIngresos);

        final BigDecimal relacionOtrosIngresos = this.fromApiJsonHelper.extractBigDecimalNamed("Relacion_otros_ingresos", jsonElement,
                locale);
        loanAdditionalData.setRelacionOtrosIngresos(relacionOtrosIngresos);

        final String programa = this.fromApiJsonHelper.extractStringNamed("Programa", jsonElement);
        loanAdditionalData.setPrograma(programa);

        final String aldeaVivienda = this.fromApiJsonHelper.extractStringNamed("aldea_vivienda", jsonElement);
        loanAdditionalData.setAldeaVivienda(aldeaVivienda);

        final Integer aniosComunidad = this.fromApiJsonHelper.extractIntegerNamed("anios_comunidad", jsonElement, locale);
        loanAdditionalData.setAniosComunidad(aniosComunidad);

        final Integer aniosDeActividadNegocio = this.fromApiJsonHelper.extractIntegerNamed("anios_de_actividad_negocio", jsonElement,
                locale);
        loanAdditionalData.setAniosDeActividadNegocio(aniosDeActividadNegocio);

        final String apellidoCasadaSolicitante = this.fromApiJsonHelper.extractStringNamed("apellido_casada_solicitante", jsonElement);
        loanAdditionalData.setApellidoCasadaSolicitante(apellidoCasadaSolicitante);

        final String cActividadEconomica = this.fromApiJsonHelper.extractStringNamed("c_actividad_economica", jsonElement);
        loanAdditionalData.setCActividadEconomica(cActividadEconomica);

        final String cApellidoDeCasada = this.fromApiJsonHelper.extractStringNamed("c_apellido_de_casada", jsonElement);
        loanAdditionalData.setCApellidoDeCasada(cApellidoDeCasada);

        final String cDepartamento = this.fromApiJsonHelper.extractStringNamed("c_departamento", jsonElement);
        loanAdditionalData.setCDepartamento(cDepartamento);

        final String cDepartamentoDpi = this.fromApiJsonHelper.extractStringNamed("c_departamento_dpi", jsonElement);
        loanAdditionalData.setCDepartamentoDpi(cDepartamentoDpi);

        final String cDescripcionNegocio = this.fromApiJsonHelper.extractStringNamed("c_descripcion_negocio", jsonElement);
        loanAdditionalData.setCDescripcionNegocio(cDescripcionNegocio);

        final String descripcionNegocio = this.fromApiJsonHelper.extractStringNamed("descripcionNegocio", jsonElement);
        loanAdditionalData.setDescripcionNegocio(descripcionNegocio);

        final String cLugarNacimiento = this.fromApiJsonHelper.extractStringNamed("c_lugar_nacimiento", jsonElement);
        loanAdditionalData.setCLugarNacimiento(cLugarNacimiento);

        final String cMunicipio = this.fromApiJsonHelper.extractStringNamed("c_municipio", jsonElement);
        loanAdditionalData.setCMunicipio(cMunicipio);

        final String cMunicipioDpi = this.fromApiJsonHelper.extractStringNamed("c_municipio_dpi", jsonElement);
        loanAdditionalData.setCMunicipioDpi(cMunicipioDpi);

        final String cNit = this.fromApiJsonHelper.extractStringNamed("c_nit", jsonElement);
        loanAdditionalData.setCNit(cNit);

        final String cSectorEconomico = this.fromApiJsonHelper.extractStringNamed("c_sector_economico", jsonElement);
        loanAdditionalData.setCSectorEconomico(cSectorEconomico);

        final String calleNegocio = this.fromApiJsonHelper.extractStringNamed("calle_negocio", jsonElement);
        loanAdditionalData.setCalleNegocio(calleNegocio);

        final String calleVivienda = this.fromApiJsonHelper.extractStringNamed("calle_vivienda", jsonElement);
        loanAdditionalData.setCalleVivienda(calleVivienda);

        final String casaNegocio = this.fromApiJsonHelper.extractStringNamed("casa_negocio", jsonElement);
        loanAdditionalData.setCasaNegocio(casaNegocio);

        final String celularSolicitante = this.fromApiJsonHelper.extractStringNamed("celular_solicitante", jsonElement);
        loanAdditionalData.setCelularSolicitante(celularSolicitante);

        final String coloniaNegocio = this.fromApiJsonHelper.extractStringNamed("colonia_negocio", jsonElement);
        loanAdditionalData.setColoniaNegocio(coloniaNegocio);

        final String coloniaVivienda = this.fromApiJsonHelper.extractStringNamed("colonia_vivienda", jsonElement);
        loanAdditionalData.setColoniaVivienda(coloniaVivienda);

        final String correoElectronico = this.fromApiJsonHelper.extractStringNamed("correo_electronico", jsonElement);
        loanAdditionalData.setCorreoElectronico(correoElectronico);

        final Integer cuentas_uso_familia = this.fromApiJsonHelper.extractIntegerNamed("cuentas_uso_familia", jsonElement, locale);
        loanAdditionalData.setCuentas_uso_familia(cuentas_uso_familia);

        final Integer cuentas_uso_negocio = this.fromApiJsonHelper.extractIntegerNamed("cuentas_uso_negocio", jsonElement, locale);
        loanAdditionalData.setCuentas_uso_negocio(cuentas_uso_negocio);

        final String datos_moviles = this.fromApiJsonHelper.extractStringNamed("datos_moviles", jsonElement);
        loanAdditionalData.setDatos_moviles(datos_moviles);

        final String departamento_dpi_solicitante = this.fromApiJsonHelper.extractStringNamed("departamento_dpi_solicitante", jsonElement);
        loanAdditionalData.setDepartamento_dpi_solicitante(departamento_dpi_solicitante);

        final String departamento_solicitante = this.fromApiJsonHelper.extractStringNamed("departamento_solicitante", jsonElement);
        loanAdditionalData.setDepartamento_solicitante(departamento_solicitante);

        final String departamento_vivienda = this.fromApiJsonHelper.extractStringNamed("departamento_vivienda", jsonElement);
        loanAdditionalData.setDepartamento_vivienda(departamento_vivienda);

        final String descripcion_giro_negocio = this.fromApiJsonHelper.extractStringNamed("descripcion_giro_negocio", jsonElement);
        loanAdditionalData.setDescripcion_giro_negocio(descripcion_giro_negocio);

        final BigDecimal detalle_compras = this.fromApiJsonHelper.extractBigDecimalNamed("detalle_compras", jsonElement, locale);
        loanAdditionalData.setDetalle_compras(detalle_compras);

        final String detalle_de_inversion = this.fromApiJsonHelper.extractStringNamed("detalle_de_inversion", jsonElement);
        loanAdditionalData.setDetalle_de_inversion(detalle_de_inversion);

        final BigDecimal detalle_otros_ingresos = this.fromApiJsonHelper.extractBigDecimalNamed("detalle_otros_ingresos", jsonElement,
                locale);
        loanAdditionalData.setDetalle_otros_ingresos(detalle_otros_ingresos);

        final String detalle_prendaria = this.fromApiJsonHelper.extractStringNamed("detalle_prendaria", jsonElement);
        loanAdditionalData.setDetalle_prendaria(detalle_prendaria);

        final BigDecimal detalle_recuperacion_cuentas = this.fromApiJsonHelper.extractBigDecimalNamed("detalle_recuperacion_cuentas",
                jsonElement, locale);
        loanAdditionalData.setDetalle_recuperacion_cuentas(detalle_recuperacion_cuentas);

        final BigDecimal detalle_ventas = this.fromApiJsonHelper.extractBigDecimalNamed("detalle_ventas", jsonElement, locale);
        loanAdditionalData.setDetalle_ventas(detalle_ventas);

        final Integer edad_solicitante = this.fromApiJsonHelper.extractIntegerNamed("edad_solicitante", jsonElement, locale);
        loanAdditionalData.setEdad_solicitante(edad_solicitante);

        final BigDecimal efectivo_uso_familia = this.fromApiJsonHelper.extractBigDecimalNamed("efectivo_uso_familia", jsonElement, locale);
        loanAdditionalData.setEfectivo_uso_familia(efectivo_uso_familia);

        final BigDecimal efectivo_uso_negocio = this.fromApiJsonHelper.extractBigDecimalNamed("efectivo_uso_negocio", jsonElement, locale);
        loanAdditionalData.setEfectivo_uso_negocio(efectivo_uso_negocio);

        final String entorno_del_negocio = this.fromApiJsonHelper.extractStringNamed("entorno_del_negocio", jsonElement);
        loanAdditionalData.setEntorno_del_negocio(entorno_del_negocio);

        final String escolaridad_solicitante = this.fromApiJsonHelper.extractStringNamed("escolaridad_solicitante", jsonElement);
        loanAdditionalData.setEscolaridad_solicitante(escolaridad_solicitante);

        final String estado_civil_solicitante = this.fromApiJsonHelper.extractStringNamed("estado_civil_solicitante", jsonElement);
        loanAdditionalData.setEstado_civil_solicitante(estado_civil_solicitante);

        final String etnia_maya = this.fromApiJsonHelper.extractStringNamed("etnia_maya", jsonElement);
        loanAdditionalData.setEtnia_maya(etnia_maya);

        final String etnia_no_maya = this.fromApiJsonHelper.extractStringNamed("etnia_no_maya", jsonElement);
        loanAdditionalData.setEtnia_no_maya(etnia_no_maya);

        final String explique_el_tema = this.fromApiJsonHelper.extractStringNamed("explique_el_tema", jsonElement);
        loanAdditionalData.setExplique_el_tema(explique_el_tema);

        final String facilitador = this.fromApiJsonHelper.extractStringNamed("facilitador", jsonElement);
        loanAdditionalData.setFacilitador(facilitador);

        final LocalDate fecha_estacionalidad = this.fromApiJsonHelper.extractLocalDateNamed("fecha_estacionalidad", jsonElement, dateFormat,
                locale);
        loanAdditionalData.setFecha_estacionalidad(fecha_estacionalidad);

        final LocalDate fecha_inico_operaciones = this.fromApiJsonHelper.extractLocalDateNamed("fecha_inico_operaciones", jsonElement,
                dateFormat, locale);
        loanAdditionalData.setFecha_inico_operaciones(fecha_inico_operaciones);

        final LocalDate fecha_integraciones = this.fromApiJsonHelper.extractLocalDateNamed("fecha_integraciones", jsonElement, dateFormat,
                locale);
        loanAdditionalData.setFecha_integraciones(fecha_integraciones);

        final LocalDate fecha_inventario = this.fromApiJsonHelper.extractLocalDateNamed("fecha_inventario", jsonElement, dateFormat,
                locale);
        loanAdditionalData.setFecha_inventario(fecha_inventario);

        final LocalDate fecha_nacimiento_solicitante = this.fromApiJsonHelper.extractLocalDateNamed("fecha_nacimiento_solicitante",
                jsonElement, dateFormat, locale);
        loanAdditionalData.setFecha_nacimiento_solicitante(fecha_nacimiento_solicitante);

        final LocalDate fecha_visita = this.fromApiJsonHelper.extractLocalDateNamed("fecha_visita", jsonElement, dateFormat, locale);
        loanAdditionalData.setFecha_visita(fecha_visita);

        final String frecuencia_compras = this.fromApiJsonHelper.extractStringNamed("frecuencia_compras", jsonElement);
        loanAdditionalData.setFrecuencia_compras(frecuencia_compras);

        final String frecuencia_ventas = this.fromApiJsonHelper.extractStringNamed("frecuencia_ventas", jsonElement);
        loanAdditionalData.setFrecuencia_ventas(frecuencia_ventas);

        final String genero = this.fromApiJsonHelper.extractStringNamed("genero", jsonElement);
        loanAdditionalData.setGenero(genero);

        final String grupo_etnico = this.fromApiJsonHelper.extractStringNamed("grupo_etnico", jsonElement);
        loanAdditionalData.setGrupo_etnico(grupo_etnico);

        final String habla_espaniol = this.fromApiJsonHelper.extractStringNamed("habla_espaniol", jsonElement);
        loanAdditionalData.setHabla_espaniol(habla_espaniol);

        final String institucion = this.fromApiJsonHelper.extractStringNamed("institucion", jsonElement);
        loanAdditionalData.setInstitucion(institucion);

        final String inversion_actual = this.fromApiJsonHelper.extractStringNamed("inversion_actual", jsonElement);
        loanAdditionalData.setInversion_actual(inversion_actual);

        final String local_negocio = this.fromApiJsonHelper.extractStringNamed("local_negocio", jsonElement);
        loanAdditionalData.setLocal_negocio(local_negocio);

        final String lote_negocio = this.fromApiJsonHelper.extractStringNamed("lote_negocio", jsonElement);
        loanAdditionalData.setLote_negocio(lote_negocio);

        final String lote_vivienda = this.fromApiJsonHelper.extractStringNamed("lote_vivienda", jsonElement);
        loanAdditionalData.setLote_vivienda(lote_vivienda);

        final String manzana_negocio = this.fromApiJsonHelper.extractStringNamed("manzana_negocio", jsonElement);
        loanAdditionalData.setManzana_negocio(manzana_negocio);

        final String manzana_vivienda = this.fromApiJsonHelper.extractStringNamed("manzana_vivienda", jsonElement);
        loanAdditionalData.setManzana_vivienda(manzana_vivienda);

        final String municipio_dpi_solicitante = this.fromApiJsonHelper.extractStringNamed("manzana_vivienda", jsonElement);
        loanAdditionalData.setMunicipio_dpi_solicitante(municipio_dpi_solicitante);

        final String municipio_negocio = this.fromApiJsonHelper.extractStringNamed("municipio_negocio", jsonElement);
        loanAdditionalData.setMunicipio_negocio(municipio_negocio);

        final String municipio_solicitante = this.fromApiJsonHelper.extractStringNamed("municipio_solicitante", jsonElement);
        loanAdditionalData.setMunicipio_solicitante(municipio_solicitante);

        final String municipio_vivienda = this.fromApiJsonHelper.extractStringNamed("municipio_vivienda", jsonElement);
        loanAdditionalData.setMunicipio_vivienda(municipio_vivienda);

        final String nacimiento_solicitante = this.fromApiJsonHelper.extractStringNamed("nacimiento_solicitante", jsonElement);
        loanAdditionalData.setNacimiento_solicitante(nacimiento_solicitante);

        final String nit_negocio = this.fromApiJsonHelper.extractStringNamed("nit_negocio", jsonElement);
        loanAdditionalData.setNit_negocio(nit_negocio);

        final String no_casa_vivienda = this.fromApiJsonHelper.extractStringNamed("no_casa_vivienda", jsonElement);
        loanAdditionalData.setNo_casa_vivienda(no_casa_vivienda);

        final String nombre_negocio = this.fromApiJsonHelper.extractStringNamed("nombre_negocio", jsonElement);
        loanAdditionalData.setNombre_negocio(nombre_negocio);

        final Integer num_contador_vivienda = this.fromApiJsonHelper.extractIntegerNamed("num_contador_vivienda", jsonElement, locale);
        loanAdditionalData.setNum_contador_vivienda(num_contador_vivienda);

        final Integer numero_fiadores = this.fromApiJsonHelper.extractIntegerNamed("numero_fiadores", jsonElement, locale);
        loanAdditionalData.setNumero_fiadores(numero_fiadores);

        final String observaciones_visita = this.fromApiJsonHelper.extractStringNamed("observaciones_visita", jsonElement);
        loanAdditionalData.setObservaciones_visita(observaciones_visita);

        final BigDecimal otros_activos_familia = this.fromApiJsonHelper.extractBigDecimalNamed("otros_activos_familia", jsonElement,
                locale);
        loanAdditionalData.setOtros_activos_familia(otros_activos_familia);

        final BigDecimal otros_activos_negocio = this.fromApiJsonHelper.extractBigDecimalNamed("otros_activos_negocio", jsonElement,
                locale);
        loanAdditionalData.setOtros_activos_negocio(otros_activos_negocio);

        final String otros_ingresos_de_la_solicitante = this.fromApiJsonHelper.extractStringNamed("otros_ingresos_de_la_solicitante",
                jsonElement);
        loanAdditionalData.setOtros_ingresos_de_la_solicitante(otros_ingresos_de_la_solicitante);

        final String patente_sociedad = this.fromApiJsonHelper.extractStringNamed("patente_sociedad", jsonElement);
        loanAdditionalData.setPatente_sociedad(patente_sociedad);

        final String primer_apellido_solicitante = this.fromApiJsonHelper.extractStringNamed("primer_apellido_solicitante", jsonElement);
        loanAdditionalData.setPrimer_apellido_solicitante(primer_apellido_solicitante);

        final String primer_nombre_solicitante = this.fromApiJsonHelper.extractStringNamed("primer_nombre_solicitante", jsonElement);
        loanAdditionalData.setPrimer_nombre_solicitante(primer_nombre_solicitante);

        final String profesion_solicitante = this.fromApiJsonHelper.extractStringNamed("profesion_solicitante", jsonElement);
        loanAdditionalData.setProfesion_solicitante(profesion_solicitante);

        final String punto_de_referencia = this.fromApiJsonHelper.extractStringNamed("punto_de_referencia", jsonElement);
        loanAdditionalData.setPunto_de_referencia(punto_de_referencia);

        final String razon_social = this.fromApiJsonHelper.extractStringNamed("razon_social", jsonElement);
        loanAdditionalData.setRazon_social(razon_social);

        final String referencias_vecinos = this.fromApiJsonHelper.extractStringNamed("referencias_vecinos", jsonElement);
        loanAdditionalData.setReferencias_vecinos(referencias_vecinos);

        final String sector_economico_negocio = this.fromApiJsonHelper.extractStringNamed("sector_economico_negocio", jsonElement);
        loanAdditionalData.setSector_economico_negocio(sector_economico_negocio);

        final String sector_vivienda = this.fromApiJsonHelper.extractStringNamed("sector_vivienda", jsonElement);
        loanAdditionalData.setSector_vivienda(sector_vivienda);

        final String segundo_apellido_solicitante = this.fromApiJsonHelper.extractStringNamed("segundo_apellido_solicitante", jsonElement);
        loanAdditionalData.setSegundo_apellido_solicitante(segundo_apellido_solicitante);

        final String segundo_nombre_solicitante = this.fromApiJsonHelper.extractStringNamed("segundo_nombre_solicitante", jsonElement);
        loanAdditionalData.setSegundo_nombre_solicitante(segundo_nombre_solicitante);

        final BigDecimal tasa = this.fromApiJsonHelper.extractBigDecimalNamed("tasa", jsonElement, locale);
        loanAdditionalData.setTasa(tasa);

        final String telefono_negocio = this.fromApiJsonHelper.extractStringNamed("telefono_negocio", jsonElement);
        loanAdditionalData.setTelefono_negocio(telefono_negocio);

        final String tiene_correo = this.fromApiJsonHelper.extractStringNamed("tiene_correo", jsonElement);
        loanAdditionalData.setTiene_correo(tiene_correo);

        final String tipo_credito = this.fromApiJsonHelper.extractStringNamed("tipo_credito", jsonElement);
        loanAdditionalData.setTipo_credito(tipo_credito);

        final String telefono_fijo = this.fromApiJsonHelper.extractStringNamed("telefono_fijo", jsonElement);
        loanAdditionalData.setTelefono_fijo(telefono_fijo);

        final String tipo_direccion_negocio = this.fromApiJsonHelper.extractStringNamed("tipo_direccion_negocio", jsonElement);
        loanAdditionalData.setTipo_direccion_negocio(tipo_direccion_negocio);

        final BigDecimal total_costo_ventas = this.fromApiJsonHelper.extractBigDecimalNamed("total_costo_ventas", jsonElement, locale);
        loanAdditionalData.setTotal_costo_ventas(total_costo_ventas);

        final BigDecimal total_cuentas_por_cobrar = this.fromApiJsonHelper.extractBigDecimalNamed("total_cuentas_por_cobrar", jsonElement,
                locale);
        loanAdditionalData.setTotal_cuentas_por_cobrar(total_cuentas_por_cobrar);

        final BigDecimal total_cuota_mensual = this.fromApiJsonHelper.extractBigDecimalNamed("total_cuota_mensual", jsonElement, locale);
        loanAdditionalData.setTotal_cuota_mensual(total_cuota_mensual);

        final BigDecimal total_deuda = this.fromApiJsonHelper.extractBigDecimalNamed("total_deuda", jsonElement, locale);
        loanAdditionalData.setTotal_deuda(total_deuda);

        final BigDecimal total_efectivo = this.fromApiJsonHelper.extractBigDecimalNamed("total_efectivo", jsonElement, locale);
        loanAdditionalData.setTotal_efectivo(total_efectivo);

        final BigDecimal total_gastos_negocio = this.fromApiJsonHelper.extractBigDecimalNamed("total_gastos_negocio", jsonElement, locale);
        loanAdditionalData.setTotal_gastos_negocio(total_gastos_negocio);

        final BigDecimal total_gastos_vivienda = this.fromApiJsonHelper.extractBigDecimalNamed("total_gastos_vivienda", jsonElement,
                locale);
        loanAdditionalData.setTotal_gastos_vivienda(total_gastos_vivienda);

        final BigDecimal total_inmueble_familia = this.fromApiJsonHelper.extractBigDecimalNamed("total_inmueble_familia", jsonElement,
                locale);
        loanAdditionalData.setTotal_inmueble_familia(total_inmueble_familia);

        final BigDecimal total_inmueble_negocio = this.fromApiJsonHelper.extractBigDecimalNamed("total_inmueble_negocio", jsonElement,
                locale);
        loanAdditionalData.setTotal_inmueble_negocio(total_inmueble_negocio);

        final BigDecimal total_inmuebles = this.fromApiJsonHelper.extractBigDecimalNamed("total_inmuebles", jsonElement, locale);
        loanAdditionalData.setTotal_inmuebles(total_inmuebles);

        final BigDecimal total_inventario = this.fromApiJsonHelper.extractBigDecimalNamed("total_inventario", jsonElement, locale);
        loanAdditionalData.setTotal_inventario(total_inventario);

        final BigDecimal total_maquinaria = this.fromApiJsonHelper.extractBigDecimalNamed("total_maquinaria", jsonElement, locale);
        loanAdditionalData.setTotal_maquinaria(total_maquinaria);

        final BigDecimal total_menaje_de_hogar = this.fromApiJsonHelper.extractBigDecimalNamed("total_menaje_de_hogar", jsonElement,
                locale);
        loanAdditionalData.setTotal_menaje_de_hogar(total_menaje_de_hogar);

        final BigDecimal total_mobiliario_equipo = this.fromApiJsonHelper.extractBigDecimalNamed("total_mobiliario_equipo", jsonElement,
                locale);
        loanAdditionalData.setTotal_mobiliario_equipo(total_mobiliario_equipo);

        final BigDecimal total_otros_activos = this.fromApiJsonHelper.extractBigDecimalNamed("total_otros_activos", jsonElement, locale);
        loanAdditionalData.setTotal_otros_activos(total_otros_activos);

        final BigDecimal total_precio_ventas = this.fromApiJsonHelper.extractBigDecimalNamed("total_precio_ventas", jsonElement, locale);
        loanAdditionalData.setTotal_precio_ventas(total_precio_ventas);

        final BigDecimal total_recibido = this.fromApiJsonHelper.extractBigDecimalNamed("total_recibido", jsonElement, locale);
        loanAdditionalData.setTotal_recibido(total_recibido);

        final Integer total_vehiculo_familia = this.fromApiJsonHelper.extractIntegerNamed("total_vehiculo_familia", jsonElement, locale);
        loanAdditionalData.setTotal_vehiculo_familia(total_vehiculo_familia);

        final Integer total_vehiculo_negocio = this.fromApiJsonHelper.extractIntegerNamed("total_vehiculo_negocio", jsonElement, locale);
        loanAdditionalData.setTotal_vehiculo_negocio(total_vehiculo_negocio);

        final Integer total_vehiculos = this.fromApiJsonHelper.extractIntegerNamed("total_vehiculos", jsonElement, locale);
        loanAdditionalData.setTotal_vehiculos(total_vehiculos);

        final String ubicacion_cliente = this.fromApiJsonHelper.extractStringNamed("ubicacion_cliente", jsonElement);
        loanAdditionalData.setUbicacion_cliente(ubicacion_cliente);

        final String ubicacion_negocio = this.fromApiJsonHelper.extractStringNamed("ubicacion_negocio", jsonElement);
        loanAdditionalData.setUbicacion_negocio(ubicacion_negocio);

        final String usa_facebook = this.fromApiJsonHelper.extractStringNamed("usa_facebook", jsonElement);
        loanAdditionalData.setUsa_facebook(usa_facebook);

        final String verificacion_negocio = this.fromApiJsonHelper.extractStringNamed("verificacion_negocio", jsonElement);
        loanAdditionalData.setVerificacion_negocio(verificacion_negocio);

        final String verificacion_vivienda = this.fromApiJsonHelper.extractStringNamed("verificacion_vivienda", jsonElement);
        loanAdditionalData.setVerificacion_vivienda(verificacion_vivienda);

        final String whatsapp = this.fromApiJsonHelper.extractStringNamed("whatsapp", jsonElement);
        loanAdditionalData.setWhatsapp(whatsapp);

        final Integer zona_negocio = this.fromApiJsonHelper.extractIntegerNamed("zona_negocio", jsonElement, locale);
        loanAdditionalData.setZona_negocio(zona_negocio);

        final Integer zona_vivienda = this.fromApiJsonHelper.extractIntegerNamed("zona_vivienda", jsonElement, locale);
        loanAdditionalData.setZona_vivienda(zona_vivienda);

        final String detalle_fiadores = this.fromApiJsonHelper.extractStringNamed("detalle_fiadores", jsonElement);
        loanAdditionalData.setDetalle_fiadores(detalle_fiadores);

        final String dpi_solicitante = this.fromApiJsonHelper.extractStringNamed("dpi_solicitante", jsonElement);
        loanAdditionalData.setDpi_solicitante(dpi_solicitante);

        final String recuperacion_cuentas = this.fromApiJsonHelper.extractStringNamed("recuperacion_cuentas", jsonElement);
        loanAdditionalData.setRecuperacion_cuentas(recuperacion_cuentas);

        final String tercer_nombre_solicitante = this.fromApiJsonHelper.extractStringNamed("tercer_nombre_solicitante", jsonElement);
        loanAdditionalData.setTercer_nombre_solicitante(tercer_nombre_solicitante);

        return loanAdditionalData;
    }
}
