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
package org.apache.fineract.organisation.prequalification.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.prequalification.data.LoanAdditionalData;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.springframework.beans.BeanUtils;

@Getter
@Entity
@Table(name = "m_client_loan_additional_properties")
public class LoanAdditionProperties extends AbstractPersistableCustom {

    @OneToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @OneToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "Ciclos_Cancelados")
    private Integer ciclosCancelados;

    @Column(name = "branch_code")
    private Long branchCode;

    @Column(name = "cargoTesorera")
    private String cargoTesorera;

    @Column(name = "cargo")
    private String cargo;

    @Column(name = "estado_solicitud")
    private String estadoSolicitud;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "producto")
    private String producto;

    @Column(name = "Fecha_Solicitud")
    private LocalDate fechaSolicitud;

    @Column(name = "codigo_cliente")
    private String codigoCliente;

    @Column(name = "actividad_negocio")
    private String actividadNegocio;

    @Column(name = "activo_corriente")
    private BigDecimal activoCorriente;

    @Column(name = "activo_no_corriente")
    private BigDecimal activoNocorriente;

    @Column(name = "alimentacion")
    private BigDecimal alimentacion;

    @Column(name = "alquiler_cliente")
    private BigDecimal alquilerCliente;

    @Column(name = "alquiler_gasto")
    private BigDecimal alquilerGasto;

    @Column(name = "alquiler_local")
    private BigDecimal alquilerLocal;

    @Column(name = "antiguedad_negocio")
    private String antiguedadNegocio;

    @Column(name = "apoyo_familia")
    private String apoyoFamilia;

    @Column(name = "aprobaciones_bc")
    private Integer aprobacionesBc;

    @Column(name = "area")
    private String area;

    @Column(name = "bienes_inmuebles")
    private Integer bienesInmuebles;

    @Column(name = "bienes_inmuebles_familiares")
    private Integer bienesInmueblesFamiliares;

    @Column(name = "c_dpi")
    private String cDpi;

    @Column(name = "c_edad")
    private Integer cEdad;

    @Column(name = "c_fecha_nacimiento")
    private LocalDate cFechaNacimiento;

    @Column(name = "c_otro_nombre")
    private String cOtroNombre;

    @Column(name = "c_primer_apellido")
    private String cPrimerApellido;

    @Column(name = "c_primer_nombre")
    private String cPrimerNombre;

    @Column(name = "c_profesion")
    private String cProfesion;

    @Column(name = "c_segundo_apellido")
    private String cSegundoApellido;

    @Column(name = "c_segundo_nombre")
    private String cSegundoNombre;

    @Column(name = "c_telefono")
    private String cTelefono;

    @Column(name = "capacidad_pago")
    private BigDecimal capacidadPago;

    @Column(name = "comunal_vigente")
    private BigDecimal comunalVigente;

    @Column(name = "costo_unitario")
    private BigDecimal costoUnitario;

    @Column(name = "costo_venta")
    private BigDecimal costoVenta;

    @Column(name = "cuanto_pagar")
    private BigDecimal cuantoPagar;

    @Column(name = "cuentas_por_pagar")
    private BigDecimal cuentasPorPagar;

    @Column(name = "cuota")
    private Integer cuota;

    @Column(name = "cuota_otros")
    private Integer cuotaOtros;

    @Column(name = "cuota_puente")
    private Integer cuotaPuente;

    @Column(name = "cuotas_pendientes_bc")
    private Integer cuotasPendientesBc;

    @Column(name = "dependientes")
    private Integer dependientes;

    @Column(name = "destino_prestamo")
    private String destinoPrestamo;

    @Column(name = "educacion")
    private Integer educacion;

    @Column(name = "efectivo")
    private BigDecimal efectivo;

    @Column(name = "endeudamiento_actual")
    private BigDecimal endeudamientoActual;

    @Column(name = "endeudamiento_futuro")
    private BigDecimal endeudamientoFuturo;

    @Column(name = "enf")
    private Integer enf;

    @Column(name = "escribe")
    private String escribe;

    @Column(name = "evolucion_negocio")
    private String evolucionNegocio;

    @Column(name = "f_pep")
    private String fPep;

    @Column(name = "familiares")
    private Integer familiares;

    @Column(name = "fecha_primera_reunion")
    private LocalDate fechaPrimeraReunion;

    @Column(name = "flujo_disponible")
    private Integer flujoDisponible;

    @Column(name = "garantia_prestamo")
    private String garantiaPrestamo;

    @Column(name = "gastos_familiares")
    private BigDecimal gastosFamiliares;

    @Column(name = "gastos_negocio")
    private BigDecimal gastosNegocio;

    @Column(name = "herramientas")
    private Integer herramientas;

    @Column(name = "hijos")
    private Integer hijos;

    @Column(name = "mortgages")
    private Integer mortgages;

    @Column(name = "impuestos")
    private Integer impuestos;

    @Column(name = "ingresado_por")
    private String ingresadoPor;

    @Column(name = "ingreso_familiar")
    private BigDecimal ingresoFamiliar;

    @Column(name = "integrantes_adicional")
    private Integer integrantesAdicional;

    @Column(name = "inventarios")
    private BigDecimal inventarios;

    @Column(name = "inversion_total")
    private BigDecimal inversionTotal;

    @Column(name = "invertir")
    private String invertir;

    @Column(name = "lee")
    private String lee;

    @Column(name = "menaje_del_hogar")
    private BigDecimal menajeDelHogar;

    @Column(name = "mobiliario_y_equipo")
    private BigDecimal mobiliarioYequipo;

    @Column(name = "monto_solicitado")
    private BigDecimal montoSolicitado;

    @Column(name = "motivo_solicitud")
    private String motivoSolicitud;

    @Column(name = "nit")
    private String nit;

    @Column(name = "nombre_propio")
    private String nombrePropio;

    @Column(name = "pasivo_corriente")
    private BigDecimal pasivoCorriente;

    @Column(name = "pasivo_no_Corriente")
    private BigDecimal pasivoNoCorriente;

    @Column(name = "pensiones")
    private BigDecimal pensiones;

    @Column(name = "pep")
    private String pep;

    @Column(name = "plazo")
    private Integer plazo;

    @Column(name = "plazo_vigente")
    private Integer plazoVigente;

    @Column(name = "posee_cuenta")
    private String poseeCuenta;

    @Column(name = "prestamo_puente")
    private Long prestamoPuente;

    @Column(name = "propuesta_facilitador")
    private BigDecimal propuestaFacilitador;

    @Column(name = "punto_reunion")
    private String puntoReunion;

    @Column(name = "relacion_gastos")
    private BigDecimal relacionGastos;

    @Column(name = "rentabilidad_neta")
    private BigDecimal rentabilidadNeta;

    @Column(name = "rotacion_inventario")
    private BigDecimal rotacionInventario;

    @Column(name = "salario_cliente")
    private BigDecimal salarioCliente;

    @Column(name = "salarios")
    private BigDecimal salarios;

    @Column(name = "salud")
    private String salud;

    @Column(name = "servicios")
    private String servicios;

    @Column(name = "servicios_basicos")
    private BigDecimal serviciosBasicos;

    @Column(name = "servicios_gasto")
    private BigDecimal serviciosGasto;

    @Column(name = "servicios_medicos")
    private BigDecimal serviciosMedicos;

    @Column(name = "tarjetas")
    private Integer tarjetas;

    @Column(name = "tipo_vivienda")
    private String tipoVivienda;

    @Column(name = "total_activo")
    private BigDecimal totalActivo;

    @Column(name = "total_ingresos")
    private BigDecimal totalIngresos;

    @Column(name = "total_ingresos_familiares")
    private BigDecimal totalIngresosFamiliares;

    @Column(name = "total_pasivo")
    private BigDecimal totalPasivo;

    @Column(name = "transporte_gasto")
    private BigDecimal transporteGasto;

    @Column(name = "transporte_negocio")
    private BigDecimal transporteNegocio;

    @Column(name = "ubicacion_cliente")
    private String ubicacionCliente;

    @Column(name = "ubicacion_negocio")
    private String ubicacionNegocio;

    @Column(name = "utilidad_bruta")
    private BigDecimal utilidadBruta;

    @Column(name = "utilidad_neta")
    private BigDecimal utilidadNeta;

    @Column(name = "valid_fiador")
    private Integer validFiador;

    @Column(name = "valor_garantia")
    private BigDecimal valorGarantia;

    @Column(name = "vehiculos")
    private Integer vehiculos;

    @Column(name = "vestimenta")
    private BigDecimal vestimenta;

    @Column(name = "visito_negocio")
    private String visitoNegocio;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "owner_id")
    private String ownerId;

    @Column(name = "case_name")
    private String caseName;

    @Column(name = "case_type")
    private String caseType;

    @Column(name = "date_opened")
    private LocalDateTime dateOpened;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "ventas")
    private BigDecimal ventas;

    public LoanAdditionProperties() {}

    public static LoanAdditionProperties fromAdditionalData(final LoanAdditionalData loanAdditionalData) {
        final LoanAdditionProperties loanAdditionProperties = new LoanAdditionProperties();
        BeanUtils.copyProperties(loanAdditionalData, loanAdditionProperties);
        return loanAdditionProperties;
    }

    public LoanAdditionalData toData() {
        final LoanAdditionalData loanAdditionalData = LoanAdditionalData.builder().build();
        BeanUtils.copyProperties(this, loanAdditionalData);
        return loanAdditionalData;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public void setCiclosCancelados(Integer ciclosCancelados) {
        this.ciclosCancelados = ciclosCancelados;
    }

    public void setBranchCode(Long branchCode) {
        this.branchCode = branchCode;
    }

    public void setCargoTesorera(String cargoTesorera) {
        this.cargoTesorera = cargoTesorera;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public void setEstadoSolicitud(String estadoSolicitud) {
        this.estadoSolicitud = estadoSolicitud;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public void setFechaSolicitud(LocalDate fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public void setCodigoCliente(String codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public void setActividadNegocio(String actividadNegocio) {
        this.actividadNegocio = actividadNegocio;
    }

    public void setActivoCorriente(BigDecimal activoCorriente) {
        this.activoCorriente = activoCorriente;
    }

    public void setActivoNocorriente(BigDecimal activoNocorriente) {
        this.activoNocorriente = activoNocorriente;
    }

    public void setAlimentacion(BigDecimal alimentacion) {
        this.alimentacion = alimentacion;
    }

    public void setAlquilerCliente(BigDecimal alquilerCliente) {
        this.alquilerCliente = alquilerCliente;
    }

    public void setAlquilerGasto(BigDecimal alquilerGasto) {
        this.alquilerGasto = alquilerGasto;
    }

    public void setAlquilerLocal(BigDecimal alquilerLocal) {
        this.alquilerLocal = alquilerLocal;
    }

    public void setAntiguedadNegocio(String antiguedadNegocio) {
        this.antiguedadNegocio = antiguedadNegocio;
    }

    public void setApoyoFamilia(String apoyoFamilia) {
        this.apoyoFamilia = apoyoFamilia;
    }

    public void setAprobacionesBc(Integer aprobacionesBc) {
        this.aprobacionesBc = aprobacionesBc;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public void setBienesInmuebles(Integer bienesInmuebles) {
        this.bienesInmuebles = bienesInmuebles;
    }

    public void setBienesInmueblesFamiliares(Integer bienesInmueblesFamiliares) {
        this.bienesInmueblesFamiliares = bienesInmueblesFamiliares;
    }

    public void setcDpi(String cDpi) {
        this.cDpi = cDpi;
    }

    public void setcEdad(Integer cEdad) {
        this.cEdad = cEdad;
    }

    public void setcFechaNacimiento(LocalDate cFechaNacimiento) {
        this.cFechaNacimiento = cFechaNacimiento;
    }

    public void setcOtroNombre(String cOtroNombre) {
        this.cOtroNombre = cOtroNombre;
    }

    public void setcPrimerApellido(String cPrimerApellido) {
        this.cPrimerApellido = cPrimerApellido;
    }

    public void setcPrimerNombre(String cPrimerNombre) {
        this.cPrimerNombre = cPrimerNombre;
    }

    public void setcProfesion(String cProfesion) {
        this.cProfesion = cProfesion;
    }

    public void setcSegundoApellido(String cSegundoApellido) {
        this.cSegundoApellido = cSegundoApellido;
    }

    public void setcSegundoNombre(String cSegundoNombre) {
        this.cSegundoNombre = cSegundoNombre;
    }

    public void setcTelefono(String cTelefono) {
        this.cTelefono = cTelefono;
    }

    public void setCapacidadPago(BigDecimal capacidadPago) {
        this.capacidadPago = capacidadPago;
    }

    public void setComunalVigente(BigDecimal comunalVigente) {
        this.comunalVigente = comunalVigente;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public void setCostoVenta(BigDecimal costoVenta) {
        this.costoVenta = costoVenta;
    }

    public void setCuantoPagar(BigDecimal cuantoPagar) {
        this.cuantoPagar = cuantoPagar;
    }

    public void setCuentasPorPagar(BigDecimal cuentasPorPagar) {
        this.cuentasPorPagar = cuentasPorPagar;
    }

    public void setCuota(Integer cuota) {
        this.cuota = cuota;
    }

    public void setCuotaOtros(Integer cuotaOtros) {
        this.cuotaOtros = cuotaOtros;
    }

    public void setCuotaPuente(Integer cuotaPuente) {
        this.cuotaPuente = cuotaPuente;
    }

    public void setCuotasPendientesBc(Integer cuotasPendientesBc) {
        this.cuotasPendientesBc = cuotasPendientesBc;
    }

    public void setDependientes(Integer dependientes) {
        this.dependientes = dependientes;
    }

    public void setDestinoPrestamo(String destinoPrestamo) {
        this.destinoPrestamo = destinoPrestamo;
    }

    public void setEducacion(Integer educacion) {
        this.educacion = educacion;
    }

    public void setEfectivo(BigDecimal efectivo) {
        this.efectivo = efectivo;
    }

    public void setEndeudamientoActual(BigDecimal endeudamientoActual) {
        this.endeudamientoActual = endeudamientoActual;
    }

    public void setEndeudamientoFuturo(BigDecimal endeudamientoFuturo) {
        this.endeudamientoFuturo = endeudamientoFuturo;
    }

    public void setEnf(Integer enf) {
        this.enf = enf;
    }

    public void setEscribe(String escribe) {
        this.escribe = escribe;
    }

    public void setEvolucionNegocio(String evolucionNegocio) {
        this.evolucionNegocio = evolucionNegocio;
    }

    public void setfPep(String fPep) {
        this.fPep = fPep;
    }

    public void setFamiliares(Integer familiares) {
        this.familiares = familiares;
    }

    public void setFechaPrimeraReunion(LocalDate fechaPrimeraReunion) {
        this.fechaPrimeraReunion = fechaPrimeraReunion;
    }

    public void setFlujoDisponible(Integer flujoDisponible) {
        this.flujoDisponible = flujoDisponible;
    }

    public void setGarantiaPrestamo(String garantiaPrestamo) {
        this.garantiaPrestamo = garantiaPrestamo;
    }

    public void setGastosFamiliares(BigDecimal gastosFamiliares) {
        this.gastosFamiliares = gastosFamiliares;
    }

    public void setGastosNegocio(BigDecimal gastosNegocio) {
        this.gastosNegocio = gastosNegocio;
    }

    public void setHerramientas(Integer herramientas) {
        this.herramientas = herramientas;
    }

    public void setHijos(Integer hijos) {
        this.hijos = hijos;
    }

    public void setMortgages(Integer mortgages) {
        this.mortgages = mortgages;
    }

    public void setImpuestos(Integer impuestos) {
        this.impuestos = impuestos;
    }

    public void setIngresadoPor(String ingresadoPor) {
        this.ingresadoPor = ingresadoPor;
    }

    public void setIngresoFamiliar(BigDecimal ingresoFamiliar) {
        this.ingresoFamiliar = ingresoFamiliar;
    }

    public void setIntegrantesAdicional(Integer integrantesAdicional) {
        this.integrantesAdicional = integrantesAdicional;
    }

    public void setInventarios(BigDecimal inventarios) {
        this.inventarios = inventarios;
    }

    public void setInversionTotal(BigDecimal inversionTotal) {
        this.inversionTotal = inversionTotal;
    }

    public void setInvertir(String invertir) {
        this.invertir = invertir;
    }

    public void setLee(String lee) {
        this.lee = lee;
    }

    public void setMenajeDelHogar(BigDecimal menajeDelHogar) {
        this.menajeDelHogar = menajeDelHogar;
    }

    public void setMobiliarioYequipo(BigDecimal mobiliarioYequipo) {
        this.mobiliarioYequipo = mobiliarioYequipo;
    }

    public void setMontoSolicitado(BigDecimal montoSolicitado) {
        this.montoSolicitado = montoSolicitado;
    }

    public void setMotivoSolicitud(String motivoSolicitud) {
        this.motivoSolicitud = motivoSolicitud;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public void setNombrePropio(String nombrePropio) {
        this.nombrePropio = nombrePropio;
    }

    public void setPasivoCorriente(BigDecimal pasivoCorriente) {
        this.pasivoCorriente = pasivoCorriente;
    }

    public void setPasivoNoCorriente(BigDecimal pasivoNoCorriente) {
        this.pasivoNoCorriente = pasivoNoCorriente;
    }

    public void setPensiones(BigDecimal pensiones) {
        this.pensiones = pensiones;
    }

    public void setPep(String pep) {
        this.pep = pep;
    }

    public void setPlazo(Integer plazo) {
        this.plazo = plazo;
    }

    public void setPlazoVigente(Integer plazoVigente) {
        this.plazoVigente = plazoVigente;
    }

    public void setPoseeCuenta(String poseeCuenta) {
        this.poseeCuenta = poseeCuenta;
    }

    public void setPrestamoPuente(Long prestamoPuente) {
        this.prestamoPuente = prestamoPuente;
    }

    public void setPropuestaFacilitador(BigDecimal propuestaFacilitador) {
        this.propuestaFacilitador = propuestaFacilitador;
    }

    public void setPuntoReunion(String puntoReunion) {
        this.puntoReunion = puntoReunion;
    }

    public void setRelacionGastos(BigDecimal relacionGastos) {
        this.relacionGastos = relacionGastos;
    }

    public void setRentabilidadNeta(BigDecimal rentabilidadNeta) {
        this.rentabilidadNeta = rentabilidadNeta;
    }

    public void setRotacionInventario(BigDecimal rotacionInventario) {
        this.rotacionInventario = rotacionInventario;
    }

    public void setSalarioCliente(BigDecimal salarioCliente) {
        this.salarioCliente = salarioCliente;
    }

    public void setSalarios(BigDecimal salarios) {
        this.salarios = salarios;
    }

    public void setSalud(String salud) {
        this.salud = salud;
    }

    public void setServicios(String servicios) {
        this.servicios = servicios;
    }

    public void setServiciosBasicos(BigDecimal serviciosBasicos) {
        this.serviciosBasicos = serviciosBasicos;
    }

    public void setServiciosGasto(BigDecimal serviciosGasto) {
        this.serviciosGasto = serviciosGasto;
    }

    public void setServiciosMedicos(BigDecimal serviciosMedicos) {
        this.serviciosMedicos = serviciosMedicos;
    }

    public void setTarjetas(Integer tarjetas) {
        this.tarjetas = tarjetas;
    }

    public void setTipoVivienda(String tipoVivienda) {
        this.tipoVivienda = tipoVivienda;
    }

    public void setTotalActivo(BigDecimal totalActivo) {
        this.totalActivo = totalActivo;
    }

    public void setTotalIngresos(BigDecimal totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public void setTotalIngresosFamiliares(BigDecimal totalIngresosFamiliares) {
        this.totalIngresosFamiliares = totalIngresosFamiliares;
    }

    public void setTotalPasivo(BigDecimal totalPasivo) {
        this.totalPasivo = totalPasivo;
    }

    public void setTransporteGasto(BigDecimal transporteGasto) {
        this.transporteGasto = transporteGasto;
    }

    public void setTransporteNegocio(BigDecimal transporteNegocio) {
        this.transporteNegocio = transporteNegocio;
    }

    public void setUbicacionCliente(String ubicacionCliente) {
        this.ubicacionCliente = ubicacionCliente;
    }

    public void setUbicacionNegocio(String ubicacionNegocio) {
        this.ubicacionNegocio = ubicacionNegocio;
    }

    public void setUtilidadBruta(BigDecimal utilidadBruta) {
        this.utilidadBruta = utilidadBruta;
    }

    public void setUtilidadNeta(BigDecimal utilidadNeta) {
        this.utilidadNeta = utilidadNeta;
    }

    public void setValidFiador(Integer validFiador) {
        this.validFiador = validFiador;
    }

    public void setValorGarantia(BigDecimal valorGarantia) {
        this.valorGarantia = valorGarantia;
    }

    public void setVehiculos(Integer vehiculos) {
        this.vehiculos = vehiculos;
    }

    public void setVestimenta(BigDecimal vestimenta) {
        this.vestimenta = vestimenta;
    }

    public void setVisitoNegocio(String visitoNegocio) {
        this.visitoNegocio = visitoNegocio;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public void setDateOpened(LocalDateTime dateOpened) {
        this.dateOpened = dateOpened;
    }
}
