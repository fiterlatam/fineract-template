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
            EnumOptionData enumOptionData = this.makeBureauCheckApiCall(member.getDpi());
            if (enumOptionData == null) {
                throw new DpiBuroChequeException(member.getDpi());
            }
            member.updateBuroCheckStatus(enumOptionData.getId().intValue());
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

    private EnumOptionData makeBureauCheckApiCall(final String dpi) {
        EnumOptionData enumOptionData = null;
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
                final String classificationLetter = this.fromApiJsonHelper.extractStringNamed("Clasificacion", jsonElement);
                enumOptionData = BuroCheckClassification.status(BuroCheckClassification.fromLetter(classificationLetter).getId());
            }
        }

        return enumOptionData;
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
        if (!loanProduct.getName().equalsIgnoreCase(loanAdditionalData.getProducto())) {
            throw new PlatformDataIntegrityException("error.msg.selected.loan.product.not.same.with.the.case.id",
                    "Loan additional properties Not Found for Case ID " + caseId);
        }
        return loanAdditionalData;
    }

    public LoanAdditionalData mapFromJson(final JsonElement jsonElement) {
        final LoanAdditionalData.LoanAdditionalDataBuilder loanAdditionalDataBuilder = LoanAdditionalData.builder();
        final String dateFormat = "yyyy-MM-dd";
        final String dateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
        final String localeAsString = "en";
        final Locale locale = JsonParserHelper.localeFromString(localeAsString);

        final String caseId = this.fromApiJsonHelper.extractStringNamed("case_id", jsonElement);
        loanAdditionalDataBuilder.caseId(caseId);

        final Integer ciclosCancelados = this.fromApiJsonHelper.extractIntegerNamed("Ciclos_Cancelados", jsonElement, locale);
        loanAdditionalDataBuilder.ciclosCancelados(ciclosCancelados);

        final Long branchCode = this.fromApiJsonHelper.extractLongNamed("branch_code", jsonElement);
        loanAdditionalDataBuilder.branchCode(branchCode);

        final String cargoTesorera = this.fromApiJsonHelper.extractStringNamed("cargoTesorera", jsonElement);
        loanAdditionalDataBuilder.cargoTesorera(cargoTesorera);

        final String cargo = this.fromApiJsonHelper.extractStringNamed("cargo", jsonElement);
        loanAdditionalDataBuilder.cargo(cargo);

        final String estadoSolicitud = this.fromApiJsonHelper.extractStringNamed("estado_solicitud", jsonElement);
        loanAdditionalDataBuilder.estadoSolicitud(estadoSolicitud);

        final LocalDate fechaInicio = this.fromApiJsonHelper.extractLocalDateNamed("fecha_inicio", jsonElement, dateFormat, locale);
        loanAdditionalDataBuilder.fechaInicio(fechaInicio);

        final String producto = this.fromApiJsonHelper.extractStringNamed("producto", jsonElement);
        loanAdditionalDataBuilder.producto(producto);

        final LocalDate fechaSolicitud = this.fromApiJsonHelper.extractLocalDateNamed("Fecha_Solicitud", jsonElement, dateFormat, locale);
        loanAdditionalDataBuilder.fechaSolicitud(fechaSolicitud);

        final String codigoCliente = this.fromApiJsonHelper.extractStringNamed("codigo_cliente", jsonElement);
        loanAdditionalDataBuilder.codigoCliente(codigoCliente);

        final String actividadNegocio = this.fromApiJsonHelper.extractStringNamed("actividad_negocio", jsonElement);
        loanAdditionalDataBuilder.actividadNegocio(actividadNegocio);

        final BigDecimal activoCorriente = this.fromApiJsonHelper.extractBigDecimalNamed("activo_corriente", jsonElement, locale);
        loanAdditionalDataBuilder.activoCorriente(activoCorriente);

        final BigDecimal activoNocorriente = this.fromApiJsonHelper.extractBigDecimalNamed("activo_no_corriente", jsonElement, locale);
        loanAdditionalDataBuilder.activoNocorriente(activoNocorriente);

        final BigDecimal alimentacion = this.fromApiJsonHelper.extractBigDecimalNamed("alimentacion", jsonElement, locale);
        loanAdditionalDataBuilder.alimentacion(alimentacion);

        final BigDecimal alquilerCliente = this.fromApiJsonHelper.extractBigDecimalNamed("alquiler_cliente", jsonElement, locale);
        loanAdditionalDataBuilder.alquilerCliente(alquilerCliente);

        final BigDecimal alquilerGasto = this.fromApiJsonHelper.extractBigDecimalNamed("alquiler_gasto", jsonElement, locale);
        loanAdditionalDataBuilder.alquilerGasto(alquilerGasto);

        final BigDecimal alquilerLocal = this.fromApiJsonHelper.extractBigDecimalNamed("alquiler_local", jsonElement, locale);
        loanAdditionalDataBuilder.alquilerLocal(alquilerLocal);

        final String antiguedadNegocio = this.fromApiJsonHelper.extractStringNamed("antiguedad_negocio", jsonElement);
        loanAdditionalDataBuilder.antiguedadNegocio(antiguedadNegocio);

        final String apoyoFamilia = this.fromApiJsonHelper.extractStringNamed("apoyo_familia", jsonElement);
        loanAdditionalDataBuilder.apoyoFamilia(apoyoFamilia);

        final Integer aprobacionesBc = this.fromApiJsonHelper.extractIntegerNamed("aprobaciones_bc", jsonElement, locale);
        loanAdditionalDataBuilder.aprobacionesBc(aprobacionesBc);

        final String area = this.fromApiJsonHelper.extractStringNamed("area", jsonElement);
        loanAdditionalDataBuilder.area(area);

        final Integer bienesInmuebles = this.fromApiJsonHelper.extractIntegerNamed("bienes_inmuebles", jsonElement, locale);
        loanAdditionalDataBuilder.bienesInmuebles(bienesInmuebles);

        final Integer bienesInmueblesFamiliares = this.fromApiJsonHelper.extractIntegerNamed("bienes_inmuebles_familiares", jsonElement,
                locale);
        loanAdditionalDataBuilder.bienesInmueblesFamiliares(bienesInmueblesFamiliares);

        final String cDpi = this.fromApiJsonHelper.extractStringNamed("c_dpi", jsonElement);
        loanAdditionalDataBuilder.cDpi(cDpi);

        final Integer cEdad = this.fromApiJsonHelper.extractIntegerNamed("c_edad", jsonElement, locale);
        loanAdditionalDataBuilder.cEdad(cEdad);

        final LocalDate cFechaNacimiento = this.fromApiJsonHelper.extractLocalDateNamed("c_fecha_nacimiento", jsonElement, dateFormat,
                locale);
        loanAdditionalDataBuilder.cFechaNacimiento(cFechaNacimiento);

        final String cOtroNombre = this.fromApiJsonHelper.extractStringNamed("c_otro_nombre", jsonElement);
        loanAdditionalDataBuilder.cOtroNombre(cOtroNombre);

        final String cPrimerApellido = this.fromApiJsonHelper.extractStringNamed("c_primer_apellido", jsonElement);
        loanAdditionalDataBuilder.cPrimerApellido(cPrimerApellido);

        final String cPrimerNombre = this.fromApiJsonHelper.extractStringNamed("c_primer_nombre", jsonElement);
        loanAdditionalDataBuilder.cPrimerNombre(cPrimerNombre);

        final String cProfesion = this.fromApiJsonHelper.extractStringNamed("c_profesion", jsonElement);
        loanAdditionalDataBuilder.cProfesion(cProfesion);

        final String cSegundoApellido = this.fromApiJsonHelper.extractStringNamed("c_segundo_apellido", jsonElement);
        loanAdditionalDataBuilder.cSegundoApellido(cSegundoApellido);

        final String cSegundoNombre = this.fromApiJsonHelper.extractStringNamed("c_segundo_nombre", jsonElement);
        loanAdditionalDataBuilder.cSegundoNombre(cSegundoNombre);

        final String cTelefono = this.fromApiJsonHelper.extractStringNamed("c_telefono", jsonElement);
        loanAdditionalDataBuilder.cTelefono(cTelefono);

        final BigDecimal capacidadPago = this.fromApiJsonHelper.extractBigDecimalNamed("capacidad_pago", jsonElement, locale);
        loanAdditionalDataBuilder.capacidadPago(capacidadPago);

        final BigDecimal comunalVigente = this.fromApiJsonHelper.extractBigDecimalNamed("comunal_vigente", jsonElement, locale);
        loanAdditionalDataBuilder.comunalVigente(comunalVigente);

        final BigDecimal costoUnitario = this.fromApiJsonHelper.extractBigDecimalNamed("costo_unitario", jsonElement, locale);
        loanAdditionalDataBuilder.costoUnitario(costoUnitario);

        final BigDecimal costoVenta = this.fromApiJsonHelper.extractBigDecimalNamed("costo_venta", jsonElement, locale);
        loanAdditionalDataBuilder.costoVenta(costoVenta);

        final BigDecimal cuantoPagar = this.fromApiJsonHelper.extractBigDecimalNamed("cuanto_pagar", jsonElement, locale);
        loanAdditionalDataBuilder.cuantoPagar(cuantoPagar);

        final BigDecimal cuentasPorPagar = this.fromApiJsonHelper.extractBigDecimalNamed("cuentas_por_pagar", jsonElement, locale);
        loanAdditionalDataBuilder.cuentasPorPagar(cuentasPorPagar);

        final Integer cuota = this.fromApiJsonHelper.extractIntegerNamed("cuota", jsonElement, locale);
        loanAdditionalDataBuilder.cuota(cuota);

        final Integer cuotaOtros = this.fromApiJsonHelper.extractIntegerNamed("cuota_otros", jsonElement, locale);
        loanAdditionalDataBuilder.cuotaOtros(cuotaOtros);

        final Integer cuotaPuente = this.fromApiJsonHelper.extractIntegerNamed("cuota_puente", jsonElement, locale);
        loanAdditionalDataBuilder.cuotaPuente(cuotaPuente);

        final Integer cuotasPendientesBc = this.fromApiJsonHelper.extractIntegerNamed("cuotas_pendientes_bc", jsonElement, locale);
        loanAdditionalDataBuilder.cuotasPendientesBc(cuotasPendientesBc);

        final Integer dependientes = this.fromApiJsonHelper.extractIntegerNamed("dependientes", jsonElement, locale);
        loanAdditionalDataBuilder.dependientes(dependientes);

        final String destinoPrestamo = this.fromApiJsonHelper.extractStringNamed("destino_prestamo", jsonElement);
        loanAdditionalDataBuilder.destinoPrestamo(destinoPrestamo);

        final Integer educacion = this.fromApiJsonHelper.extractIntegerNamed("educacion", jsonElement, locale);
        loanAdditionalDataBuilder.educacion(educacion);

        final BigDecimal efectivo = this.fromApiJsonHelper.extractBigDecimalNamed("efectivo", jsonElement, locale);
        loanAdditionalDataBuilder.efectivo(efectivo);

        final BigDecimal endeudamientoActual = this.fromApiJsonHelper.extractBigDecimalNamed("endeudamiento_actual", jsonElement, locale);
        loanAdditionalDataBuilder.endeudamientoActual(endeudamientoActual);

        final BigDecimal endeudamientoFuturo = this.fromApiJsonHelper.extractBigDecimalNamed("endeudamiento_futuro", jsonElement, locale);
        loanAdditionalDataBuilder.endeudamientoFuturo(endeudamientoFuturo);

        final Integer enf = this.fromApiJsonHelper.extractIntegerNamed("enf", jsonElement, locale);
        loanAdditionalDataBuilder.enf(enf);

        final String escribe = this.fromApiJsonHelper.extractStringNamed("escribe", jsonElement);
        loanAdditionalDataBuilder.escribe(escribe);

        final String evolucionNegocio = this.fromApiJsonHelper.extractStringNamed("evolucion_negocio", jsonElement);
        loanAdditionalDataBuilder.evolucionNegocio(evolucionNegocio);

        final String fPep = this.fromApiJsonHelper.extractStringNamed("f_pep", jsonElement);
        loanAdditionalDataBuilder.fPep(fPep);

        final Integer familiares = this.fromApiJsonHelper.extractIntegerNamed("familiares", jsonElement, locale);
        loanAdditionalDataBuilder.familiares(familiares);

        final LocalDate fechaPrimeraReunion = this.fromApiJsonHelper.extractLocalDateNamed("fecha_primera_reunion", jsonElement, dateFormat,
                locale);
        loanAdditionalDataBuilder.fechaPrimeraReunion(fechaPrimeraReunion);

        final Integer flujoDisponible = this.fromApiJsonHelper.extractIntegerNamed("flujo_disponible", jsonElement, locale);
        loanAdditionalDataBuilder.flujoDisponible(flujoDisponible);

        final String garantiaPrestamo = this.fromApiJsonHelper.extractStringNamed("garantia_prestamo", jsonElement);
        loanAdditionalDataBuilder.garantiaPrestamo(garantiaPrestamo);

        final BigDecimal gastosFamiliares = this.fromApiJsonHelper.extractBigDecimalNamed("gastos_familiares", jsonElement, locale);
        loanAdditionalDataBuilder.gastosFamiliares(gastosFamiliares);

        final BigDecimal gastosNegocio = this.fromApiJsonHelper.extractBigDecimalNamed("gastos_negocio", jsonElement, locale);
        loanAdditionalDataBuilder.gastosNegocio(gastosNegocio);

        final Integer herramientas = this.fromApiJsonHelper.extractIntegerNamed("herramientas", jsonElement, locale);
        loanAdditionalDataBuilder.herramientas(herramientas);

        final Integer hijos = this.fromApiJsonHelper.extractIntegerNamed("hijos", jsonElement, locale);
        loanAdditionalDataBuilder.hijos(hijos);

        final Integer mortgages = this.fromApiJsonHelper.extractIntegerNamed("mortgages", jsonElement, locale);
        loanAdditionalDataBuilder.mortgages(mortgages);

        final Integer impuestos = this.fromApiJsonHelper.extractIntegerNamed("impuestos", jsonElement, locale);
        loanAdditionalDataBuilder.impuestos(impuestos);

        final String ingresadoPor = this.fromApiJsonHelper.extractStringNamed("ingresado_por", jsonElement);
        loanAdditionalDataBuilder.ingresadoPor(ingresadoPor);

        final BigDecimal ingresoFamiliar = this.fromApiJsonHelper.extractBigDecimalNamed("ingreso_familiar", jsonElement, locale);
        loanAdditionalDataBuilder.ingresoFamiliar(ingresoFamiliar);

        final Integer integrantesAdicional = this.fromApiJsonHelper.extractIntegerNamed("integrantes_adicional", jsonElement, locale);
        loanAdditionalDataBuilder.integrantesAdicional(integrantesAdicional);

        final BigDecimal inventarios = this.fromApiJsonHelper.extractBigDecimalNamed("inventarios", jsonElement, locale);
        loanAdditionalDataBuilder.inventarios(inventarios);

        final BigDecimal inversionTotal = this.fromApiJsonHelper.extractBigDecimalNamed("inversion_total", jsonElement, locale);
        loanAdditionalDataBuilder.inversionTotal(inversionTotal);

        final String invertir = this.fromApiJsonHelper.extractStringNamed("invertir", jsonElement);
        loanAdditionalDataBuilder.invertir(invertir);

        final String lee = this.fromApiJsonHelper.extractStringNamed("lee", jsonElement);
        loanAdditionalDataBuilder.lee(lee);

        final BigDecimal menajeDelHogar = this.fromApiJsonHelper.extractBigDecimalNamed("menaje_del_hogar", jsonElement, locale);
        loanAdditionalDataBuilder.menajeDelHogar(menajeDelHogar);

        final BigDecimal mobiliarioYequipo = this.fromApiJsonHelper.extractBigDecimalNamed("mobiliario_y_equipo", jsonElement, locale);
        loanAdditionalDataBuilder.mobiliarioYequipo(mobiliarioYequipo);

        final BigDecimal montoSolicitado = this.fromApiJsonHelper.extractBigDecimalNamed("monto_solicitado", jsonElement, locale);
        loanAdditionalDataBuilder.montoSolicitado(montoSolicitado);

        final String motivoSolicitud = this.fromApiJsonHelper.extractStringNamed("motivo_solicitud", jsonElement);
        loanAdditionalDataBuilder.motivoSolicitud(motivoSolicitud);

        final String nit = this.fromApiJsonHelper.extractStringNamed("nit", jsonElement);
        loanAdditionalDataBuilder.nit(nit);

        final String nombrePropio = this.fromApiJsonHelper.extractStringNamed("nombre_propio", jsonElement);
        loanAdditionalDataBuilder.nombrePropio(nombrePropio);

        final BigDecimal pasivoCorriente = this.fromApiJsonHelper.extractBigDecimalNamed("pasivo_corriente", jsonElement, locale);
        loanAdditionalDataBuilder.pasivoCorriente(pasivoCorriente);

        final BigDecimal pasivoNoCorriente = this.fromApiJsonHelper.extractBigDecimalNamed("pasivo_no_Corriente", jsonElement, locale);
        loanAdditionalDataBuilder.pasivoNoCorriente(pasivoNoCorriente);

        final BigDecimal pensiones = this.fromApiJsonHelper.extractBigDecimalNamed("pensiones", jsonElement, locale);
        loanAdditionalDataBuilder.pensiones(pensiones);

        final String pep = this.fromApiJsonHelper.extractStringNamed("pep", jsonElement);
        loanAdditionalDataBuilder.pep(pep);

        final Integer plazo = this.fromApiJsonHelper.extractIntegerNamed("plazo", jsonElement, locale);
        loanAdditionalDataBuilder.plazo(plazo);

        final Integer plazoVigente = this.fromApiJsonHelper.extractIntegerNamed("plazo_vigente", jsonElement, locale);
        loanAdditionalDataBuilder.plazoVigente(plazoVigente);

        final String poseeCuenta = this.fromApiJsonHelper.extractStringNamed("posee_cuenta", jsonElement);
        loanAdditionalDataBuilder.poseeCuenta(poseeCuenta);

        final Long prestamoPuente = this.fromApiJsonHelper.extractLongNamed("prestamo_puente", jsonElement);
        loanAdditionalDataBuilder.prestamoPuente(prestamoPuente);

        final BigDecimal propuestaFacilitador = this.fromApiJsonHelper.extractBigDecimalNamed("propuesta_facilitador", jsonElement, locale);
        loanAdditionalDataBuilder.propuestaFacilitador(propuestaFacilitador);

        final String puntoReunion = this.fromApiJsonHelper.extractStringNamed("punto_reunion", jsonElement);
        loanAdditionalDataBuilder.puntoReunion(puntoReunion);

        final BigDecimal relacionGastos = this.fromApiJsonHelper.extractBigDecimalNamed("relacion_gastos", jsonElement, locale);
        loanAdditionalDataBuilder.relacionGastos(relacionGastos);

        final BigDecimal rentabilidadNeta = this.fromApiJsonHelper.extractBigDecimalNamed("rentabilidad_neta", jsonElement, locale);
        loanAdditionalDataBuilder.rentabilidadNeta(rentabilidadNeta);

        final BigDecimal rotacionInventario = this.fromApiJsonHelper.extractBigDecimalNamed("rotacion_inventario", jsonElement, locale);
        loanAdditionalDataBuilder.rotacionInventario(rotacionInventario);

        final BigDecimal salarioCliente = this.fromApiJsonHelper.extractBigDecimalNamed("salario_cliente", jsonElement, locale);
        loanAdditionalDataBuilder.salarioCliente(salarioCliente);

        final BigDecimal salarios = this.fromApiJsonHelper.extractBigDecimalNamed("salarios", jsonElement, locale);
        loanAdditionalDataBuilder.salarios(salarios);

        final String salud = this.fromApiJsonHelper.extractStringNamed("salud", jsonElement);
        loanAdditionalDataBuilder.salud(salud);

        final String servicios = this.fromApiJsonHelper.extractStringNamed("servicios", jsonElement);
        loanAdditionalDataBuilder.servicios(servicios);

        final BigDecimal serviciosBasicos = this.fromApiJsonHelper.extractBigDecimalNamed("servicios_basicos", jsonElement, locale);
        loanAdditionalDataBuilder.serviciosBasicos(serviciosBasicos);

        final BigDecimal serviciosGasto = this.fromApiJsonHelper.extractBigDecimalNamed("servicios_gasto", jsonElement, locale);
        loanAdditionalDataBuilder.serviciosGasto(serviciosGasto);

        final BigDecimal serviciosMedicos = this.fromApiJsonHelper.extractBigDecimalNamed("servicios_medicos", jsonElement, locale);
        loanAdditionalDataBuilder.serviciosMedicos(serviciosMedicos);

        final Integer tarjetas = this.fromApiJsonHelper.extractIntegerNamed("tarjetas", jsonElement, locale);
        loanAdditionalDataBuilder.tarjetas(tarjetas);

        final String tipoVivienda = this.fromApiJsonHelper.extractStringNamed("tipo_vivienda", jsonElement);
        loanAdditionalDataBuilder.tipoVivienda(tipoVivienda);

        final BigDecimal totalActivo = this.fromApiJsonHelper.extractBigDecimalNamed("total_activo", jsonElement, locale);
        loanAdditionalDataBuilder.totalActivo(totalActivo);

        final BigDecimal totalIngresos = this.fromApiJsonHelper.extractBigDecimalNamed("total_ingresos", jsonElement, locale);
        loanAdditionalDataBuilder.totalIngresos(totalIngresos);

        final BigDecimal totalIngresosFamiliares = this.fromApiJsonHelper.extractBigDecimalNamed("total_ingresos_familiares", jsonElement,
                locale);
        loanAdditionalDataBuilder.totalIngresosFamiliares(totalIngresosFamiliares);

        final BigDecimal totalPasivo = this.fromApiJsonHelper.extractBigDecimalNamed("total_pasivo", jsonElement, locale);
        loanAdditionalDataBuilder.totalPasivo(totalPasivo);

        final BigDecimal transporteGasto = this.fromApiJsonHelper.extractBigDecimalNamed("transporte_gasto", jsonElement, locale);
        loanAdditionalDataBuilder.transporteGasto(transporteGasto);

        final BigDecimal transporteNegocio = this.fromApiJsonHelper.extractBigDecimalNamed("transporte_negocio", jsonElement, locale);
        loanAdditionalDataBuilder.transporteNegocio(transporteNegocio);

        final String ubicacionCliente = this.fromApiJsonHelper.extractStringNamed("ubicacion_cliente", jsonElement);
        loanAdditionalDataBuilder.ubicacionCliente(ubicacionCliente);

        final String ubicacionNegocio = this.fromApiJsonHelper.extractStringNamed("ubicacion_negocio", jsonElement);
        loanAdditionalDataBuilder.ubicacionNegocio(ubicacionNegocio);

        final BigDecimal utilidadBruta = this.fromApiJsonHelper.extractBigDecimalNamed("utilidad_bruta", jsonElement, locale);
        loanAdditionalDataBuilder.utilidadBruta(utilidadBruta);

        final BigDecimal utilidadNeta = this.fromApiJsonHelper.extractBigDecimalNamed("utilidad_neta", jsonElement, locale);
        loanAdditionalDataBuilder.utilidadNeta(utilidadNeta);

        final Integer validFiador = this.fromApiJsonHelper.extractIntegerNamed("valid_fiador", jsonElement, locale);
        loanAdditionalDataBuilder.validFiador(validFiador);

        final BigDecimal valorGarantia = this.fromApiJsonHelper.extractBigDecimalNamed("valor_garantia", jsonElement, locale);
        loanAdditionalDataBuilder.valorGarantia(valorGarantia);

        final Integer vehiculos = this.fromApiJsonHelper.extractIntegerNamed("vehiculos", jsonElement, locale);
        loanAdditionalDataBuilder.vehiculos(vehiculos);

        final BigDecimal vestimenta = this.fromApiJsonHelper.extractBigDecimalNamed("vestimenta", jsonElement, locale);
        loanAdditionalDataBuilder.vestimenta(vestimenta);

        final String visitoNegocio = this.fromApiJsonHelper.extractStringNamed("visito_negocio", jsonElement);
        loanAdditionalDataBuilder.visitoNegocio(visitoNegocio);

        final String externalId = this.fromApiJsonHelper.extractStringNamed("external_id", jsonElement);
        loanAdditionalDataBuilder.externalId(externalId);

        final String ownerId = this.fromApiJsonHelper.extractStringNamed("owner_id", jsonElement);
        loanAdditionalDataBuilder.ownerId(ownerId);

        final String caseName = this.fromApiJsonHelper.extractStringNamed("case_name", jsonElement);
        loanAdditionalDataBuilder.caseName(caseName);

        final LocalDateTime dateOpened = this.fromApiJsonHelper.extractLocalDateTimeNamed("date_opened", jsonElement, dateTimeFormat,
                locale);
        loanAdditionalDataBuilder.dateOpened(dateOpened);

        final LocalDate fechaFin = this.fromApiJsonHelper.extractLocalDateNamed("fecha_fin", jsonElement, dateFormat, locale);
        loanAdditionalDataBuilder.fechaFin(fechaFin);

        final BigDecimal ventas = this.fromApiJsonHelper.extractBigDecimalNamed("ventas", jsonElement, locale);
        loanAdditionalDataBuilder.ventas(ventas);

        final BigDecimal cuentasPorCobrar = this.fromApiJsonHelper.extractBigDecimalNamed("cuentas_por_cobrar", jsonElement, locale);
        loanAdditionalDataBuilder.cuentasPorCobrar(cuentasPorCobrar);

        final BigDecimal hipotecas = this.fromApiJsonHelper.extractBigDecimalNamed("hipotecas", jsonElement, locale);
        loanAdditionalDataBuilder.hipotecas(hipotecas);

        final String excepcion = this.fromApiJsonHelper.extractStringNamed("excepcion", jsonElement);
        loanAdditionalDataBuilder.excepcion(excepcion);

        final Integer tipoExcepcion = this.fromApiJsonHelper.extractIntegerNamed("tipo_excepcion", jsonElement, locale);
        loanAdditionalDataBuilder.tipoExcepcion(tipoExcepcion);

        final String descripcionExcepcion = this.fromApiJsonHelper.extractStringNamed("descripcion_excepcion", jsonElement);
        loanAdditionalDataBuilder.descripcionExcepcion(descripcionExcepcion);

        final BigDecimal montoAutorizado = this.fromApiJsonHelper.extractBigDecimalNamed("monto_autorizado", jsonElement, locale);
        loanAdditionalDataBuilder.montoAutorizado(montoAutorizado);

        final String observaciones = this.fromApiJsonHelper.extractStringNamed("observaciones", jsonElement);
        loanAdditionalDataBuilder.observaciones(observaciones);

        final BigDecimal montoOtrosIngresos = this.fromApiJsonHelper.extractBigDecimalNamed("monto_otros_ingresos", jsonElement, locale);
        loanAdditionalDataBuilder.montoOtrosIngresos(montoOtrosIngresos);

        final BigDecimal capitalDdeTrabajo = this.fromApiJsonHelper.extractBigDecimalNamed("capital_de_trabajo", jsonElement, locale);
        loanAdditionalDataBuilder.capitalDdeTrabajo(capitalDdeTrabajo);

        final String origenOtrosIngresos = this.fromApiJsonHelper.extractStringNamed("origen_otros_ingresos", jsonElement);
        loanAdditionalDataBuilder.origenOtrosIngresos(origenOtrosIngresos);

        final String otrosIngresos = this.fromApiJsonHelper.extractStringNamed("otros_ingresos", jsonElement);
        loanAdditionalDataBuilder.otrosIngresos(otrosIngresos);

        final BigDecimal relacionOtrosIngresos = this.fromApiJsonHelper.extractBigDecimalNamed("Relacion_otros_ingresos", jsonElement,
                locale);
        loanAdditionalDataBuilder.relacionOtrosIngresos(relacionOtrosIngresos);

        return loanAdditionalDataBuilder.build();
    }
}
