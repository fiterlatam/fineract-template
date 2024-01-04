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

    @Column(name = "cuentas_por_cobrar")
    private BigDecimal cuentasPorCobrar;

    @Column(name = "hipotecas")
    private BigDecimal hipotecas;

    @Column(name = "excepcion")
    private String excepcion;

    @Column(name = "tipo_excepcion")
    private Integer tipoExcepcion;

    @Column(name = "descripcion_excepcion")
    private String descripcionExcepcion;

    @Column(name = "monto_autorizado")
    private BigDecimal montoAutorizado;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "capital_de_trabajo")
    private BigDecimal capitalDdeTrabajo;

    @Column(name = "monto_otros_ingresos")
    private BigDecimal montoOtrosIngresos;

    @Column(name = "origen_otros_ingresos")
    private String origenOtrosIngresos;

    @Column(name = "otros_ingresos")
    private String otrosIngresos;

    @Column(name = "Relacion_otros_ingresos")
    private BigDecimal relacionOtrosIngresos;

    @Column(name = "Programa")
    private String programa;

    @Column(name = "aldea_vivienda")
    private String aldeaVivienda;

    @Column(name = "anios_comunidad")
    private Integer aniosComunidad;

    @Column(name = "anios_de_actividad_negocio")
    private Integer aniosDeActividadNegocio;

    @Column(name = "apellido_casada_solicitante")
    private String apellidoCasadaSolicitante;

    @Column(name = "c_actividad_economica")
    private String cActividadEconomica;

    @Column(name = "c_apellido_de_casada")
    private String cApellidoDeCasada;

    @Column(name = "c_departamento")
    private String cDepartamento;

    @Column(name = "c_departamento_dpi")
    private String cDepartamentoDpi;

    @Column(name = "c_descripcion_negocio")
    private String cDescripcionNegocio;

    @Column(name = "descripcion_negocio")
    private String descripcionNegocio;

    @Column(name = "c_lugar_nacimiento")
    private String cLugarNacimiento;

    @Column(name = "c_municipio")
    private String cMunicipio;

    @Column(name = "c_municipio_dpi")
    private String cMunicipioDpi;

    @Column(name = "c_nit")
    private String cNit;

    @Column(name = "c_sector_economico")
    private String cSectorEconomico;

    @Column(name = "calle_negocio")
    private String calleNegocio;

    @Column(name = "calle_vivienda")
    private String calleVivienda;

    @Column(name = "casa_negocio")
    private String casaNegocio;

    @Column(name = "celular_solicitante")
    private String celularSolicitante;

    @Column(name = "colonia_negocio")
    private String coloniaNegocio;

    @Column(name = "colonia_vivienda")
    private String coloniaVivienda;

    @Column(name = "correo_electronico")
    private String correoElectronico;

    @Column(name = "cuentas_uso_familia")
    private Integer cuentas_uso_familia;

    @Column(name = "cuentas_uso_negocio")
    private Integer cuentas_uso_negocio;

    @Column(name = "datos_moviles")
    private String datos_moviles;

    @Column(name = "departamento_dpi_solicitante")
    private String departamento_dpi_solicitante;

    @Column(name = "departamento_negocio")
    private String departamento_negocio;

    @Column(name = "departamento_solicitante")
    private String departamento_solicitante;

    @Column(name = "departamento_vivienda")
    private String departamento_vivienda;

    @Column(name = "descripcion_giro_negocio")
    private String descripcion_giro_negocio;

    @Column(name = "detalle_compras")
    private BigDecimal detalle_compras;

    @Column(name = "detalle_de_inversion")
    private String detalle_de_inversion;

    @Column(name = "detalle_otros_ingresos")
    private BigDecimal detalle_otros_ingresos;

    @Column(name = "detalle_prendaria")
    private String detalle_prendaria;

    @Column(name = "detalle_recuperacion_cuentas")
    private BigDecimal detalle_recuperacion_cuentas;

    @Column(name = "detalle_ventas")
    private BigDecimal detalle_ventas;

    @Column(name = "edad_solicitante")
    private Integer edad_solicitante;

    @Column(name = "efectivo_uso_familia")
    private BigDecimal efectivo_uso_familia;

    @Column(name = "efectivo_uso_negocio")
    private BigDecimal efectivo_uso_negocio;

    @Column(name = "entorno_del_negocio")
    private String entorno_del_negocio;

    @Column(name = "escolaridad_solicitante")
    private String escolaridad_solicitante;

    @Column(name = "estado_civil_solicitante")
    private String estado_civil_solicitante;

    @Column(name = "etnia_maya")
    private String etnia_maya;

    @Column(name = "etnia_no_maya")
    private String etnia_no_maya;

    @Column(name = "explique_el_tema")
    private String explique_el_tema;

    @Column(name = "facilitador")
    private String facilitador;

    @Column(name = "fecha_estacionalidad")
    private LocalDate fecha_estacionalidad;

    @Column(name = "fecha_inico_operaciones")
    private LocalDate fecha_inico_operaciones;

    @Column(name = "fecha_integraciones")
    private LocalDate fecha_integraciones;

    @Column(name = "fecha_inventario")
    private LocalDate fecha_inventario;

    @Column(name = "fecha_nacimiento_solicitante")
    private LocalDate fecha_nacimiento_solicitante;

    @Column(name = "fecha_visita")
    private LocalDate fecha_visita;

    @Column(name = "frecuencia_compras")
    private String frecuencia_compras;

    @Column(name = "frecuencia_ventas")
    private String frecuencia_ventas;

    @Column(name = "genero")
    private String genero;

    @Column(name = "grupo_etnico")
    private String grupo_etnico;

    @Column(name = "habla_espaniol")
    private String habla_espaniol;

    @Column(name = "institucion")
    private String institucion;

    @Column(name = "inversion_actual")
    private String inversion_actual;

    @Column(name = "local_negocio")
    private String local_negocio;

    @Column(name = "lote_negocio")
    private String lote_negocio;

    @Column(name = "lote_vivienda")
    private String lote_vivienda;

    @Column(name = "manzana_negocio")
    private String manzana_negocio;

    @Column(name = "manzana_vivienda")
    private String manzana_vivienda;

    @Column(name = "municipio_dpi_solicitante")
    private String municipio_dpi_solicitante;

    @Column(name = "municipio_negocio")
    private String municipio_negocio;

    @Column(name = "municipio_solicitante")
    private String municipio_solicitante;

    @Column(name = "municipio_vivienda")
    private String municipio_vivienda;

    @Column(name = "nacimiento_solicitante")
    private String nacimiento_solicitante;

    @Column(name = "nit_negocio")
    private String nit_negocio;

    @Column(name = "no_casa_vivienda")
    private String no_casa_vivienda;

    @Column(name = "nombre_negocio")
    private String nombre_negocio;

    @Column(name = "num_contador_vivienda")
    private Integer num_contador_vivienda;

    @Column(name = "numero_fiadores")
    private Integer numero_fiadores;

    @Column(name = "observaciones_visita")
    private String observaciones_visita;

    @Column(name = "otros_activos_familia")
    private BigDecimal otros_activos_familia;

    @Column(name = "otros_activos_negocio")
    private BigDecimal otros_activos_negocio;

    @Column(name = "otros_ingresos_de_la_solicitante")
    private String otros_ingresos_de_la_solicitante;

    @Column(name = "patente_sociedad")
    private String patente_sociedad;

    @Column(name = "primer_apellido_solicitante")
    private String primer_apellido_solicitante;

    @Column(name = "primer_nombre_solicitante")
    private String primer_nombre_solicitante;

    @Column(name = "profesion_solicitante")
    private String profesion_solicitante;

    @Column(name = "punto_de_referencia")
    private String punto_de_referencia;

    @Column(name = "razon_social")
    private String razon_social;

    @Column(name = "referencias_vecinos")
    private String referencias_vecinos;

    @Column(name = "sector_economico_negocio")
    private String sector_economico_negocio;

    @Column(name = "sector_negocio")
    private String sector_negocio;

    @Column(name = "sector_vivienda")
    private String sector_vivienda;

    @Column(name = "segundo_apellido_solicitante")
    private String segundo_apellido_solicitante;

    @Column(name = "segundo_nombre_solicitante")
    private String segundo_nombre_solicitante;

    @Column(name = "tasa")
    private BigDecimal tasa;

    @Column(name = "telefono_fijo")
    private String telefono_fijo;

    @Column(name = "telefono_negocio")
    private String telefono_negocio;

    @Column(name = "tiene_correo")
    private String tiene_correo;

    @Column(name = "tipo_credito")
    private String tipo_credito;

    @Column(name = "tipo_direccion_negocio")
    private String tipo_direccion_negocio;

    @Column(name = "total_costo_ventas")
    private BigDecimal total_costo_ventas;

    @Column(name = "total_cuentas_por_cobrar")
    private BigDecimal total_cuentas_por_cobrar;

    @Column(name = "total_cuota_mensual")
    private BigDecimal total_cuota_mensual;

    @Column(name = "total_deuda")
    private BigDecimal total_deuda;

    @Column(name = "total_efectivo")
    private BigDecimal total_efectivo;

    @Column(name = "total_gastos_negocio")
    private BigDecimal total_gastos_negocio;

    @Column(name = "total_gastos_vivienda")
    private BigDecimal total_gastos_vivienda;

    @Column(name = "total_inmueble_familia")
    private BigDecimal total_inmueble_familia;

    @Column(name = "total_inmueble_negocio")
    private BigDecimal total_inmueble_negocio;

    @Column(name = "total_inmuebles")
    private BigDecimal total_inmuebles;

    @Column(name = "total_inventario")
    private BigDecimal total_inventario;

    @Column(name = "total_maquinaria")
    private BigDecimal total_maquinaria;

    @Column(name = "total_menaje_de_hogar")
    private BigDecimal total_menaje_de_hogar;

    @Column(name = "total_mobiliario_equipo")
    private BigDecimal total_mobiliario_equipo;

    @Column(name = "total_otros_activos")
    private BigDecimal total_otros_activos;

    @Column(name = "total_precio_ventas")
    private BigDecimal total_precio_ventas;

    @Column(name = "total_recibido")
    private BigDecimal total_recibido;

    @Column(name = "total_vehiculo_familia")
    private Integer total_vehiculo_familia;

    @Column(name = "total_vehiculo_negocio")
    private Integer total_vehiculo_negocio;

    @Column(name = "total_vehiculos")
    private Integer total_vehiculos;

    @Column(name = "usa_facebook")
    private String usa_facebook;

    @Column(name = "verificacion_negocio")
    private String verificacion_negocio;

    @Column(name = "verificacion_vivienda")
    private String verificacion_vivienda;

    @Column(name = "whatsapp")
    private String whatsapp;

    @Column(name = "zona_negocio")
    private Integer zona_negocio;

    @Column(name = "zona_vivienda")
    private Integer zona_vivienda;

    @Column(name = "detalle_fiadores")
    private String detalle_fiadores;

    @Column(name = "dpi_solicitante")
    private String dpi_solicitante;

    @Column(name = "recuperacion_cuentas")
    private String recuperacion_cuentas;

    @Column(name = "tercer_nombre_solicitante")
    private String tercer_nombre_solicitante;

    public LoanAdditionProperties() {}

    public LoanAdditionalData toData() {
        final LoanAdditionalData loanAdditionalData = new LoanAdditionalData();
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

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public void setVentas(BigDecimal ventas) {
        this.ventas = ventas;
    }

    public void setCuentasPorCobrar(BigDecimal cuentasPorCobrar) {
        this.cuentasPorCobrar = cuentasPorCobrar;
    }

    public void setHipotecas(BigDecimal hipotecas) {
        this.hipotecas = hipotecas;
    }

    public void setExcepcion(String excepcion) {
        this.excepcion = excepcion;
    }

    public void setTipoExcepcion(Integer tipoExcepcion) {
        this.tipoExcepcion = tipoExcepcion;
    }

    public void setDescripcionExcepcion(String descripcionExcepcion) {
        this.descripcionExcepcion = descripcionExcepcion;
    }

    public void setMontoAutorizado(BigDecimal montoAutorizado) {
        this.montoAutorizado = montoAutorizado;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setCapitalDdeTrabajo(BigDecimal capitalDdeTrabajo) {
        this.capitalDdeTrabajo = capitalDdeTrabajo;
    }

    public void setMontoOtrosIngresos(BigDecimal montoOtrosIngresos) {
        this.montoOtrosIngresos = montoOtrosIngresos;
    }

    public void setOrigenOtrosIngresos(String origenOtrosIngresos) {
        this.origenOtrosIngresos = origenOtrosIngresos;
    }

    public void setOtrosIngresos(String otrosIngresos) {
        this.otrosIngresos = otrosIngresos;
    }

    public void setRelacionOtrosIngresos(BigDecimal relacionOtrosIngresos) {
        this.relacionOtrosIngresos = relacionOtrosIngresos;
    }

    public void setPrograma(String programa) {
        this.programa = programa;
    }

    public void setAldeaVivienda(String aldeaVivienda) {
        this.aldeaVivienda = aldeaVivienda;
    }

    public void setAniosComunidad(Integer aniosComunidad) {
        this.aniosComunidad = aniosComunidad;
    }

    public void setAniosDeActividadNegocio(Integer aniosDeActividadNegocio) {
        this.aniosDeActividadNegocio = aniosDeActividadNegocio;
    }

    public void setApellidoCasadaSolicitante(String apellidoCasadaSolicitante) {
        this.apellidoCasadaSolicitante = apellidoCasadaSolicitante;
    }

    public void setcActividadEconomica(String cActividadEconomica) {
        this.cActividadEconomica = cActividadEconomica;
    }

    public void setcApellidoDeCasada(String cApellidoDeCasada) {
        this.cApellidoDeCasada = cApellidoDeCasada;
    }

    public void setcDepartamento(String cDepartamento) {
        this.cDepartamento = cDepartamento;
    }

    public void setcDepartamentoDpi(String cDepartamentoDpi) {
        this.cDepartamentoDpi = cDepartamentoDpi;
    }

    public void setcDescripcionNegocio(String cDescripcionNegocio) {
        this.cDescripcionNegocio = cDescripcionNegocio;
    }

    public void setcLugarNacimiento(String cLugarNacimiento) {
        this.cLugarNacimiento = cLugarNacimiento;
    }

    public void setcMunicipio(String cMunicipio) {
        this.cMunicipio = cMunicipio;
    }

    public void setcMunicipioDpi(String cMunicipioDpi) {
        this.cMunicipioDpi = cMunicipioDpi;
    }

    public void setcNit(String cNit) {
        this.cNit = cNit;
    }

    public void setcSectorEconomico(String cSectorEconomico) {
        this.cSectorEconomico = cSectorEconomico;
    }

    public void setCalleNegocio(String calleNegocio) {
        this.calleNegocio = calleNegocio;
    }

    public void setCalleVivienda(String calleVivienda) {
        this.calleVivienda = calleVivienda;
    }

    public void setCasaNegocio(String casaNegocio) {
        this.casaNegocio = casaNegocio;
    }

    public void setCelularSolicitante(String celularSolicitante) {
        this.celularSolicitante = celularSolicitante;
    }

    public void setColoniaNegocio(String coloniaNegocio) {
        this.coloniaNegocio = coloniaNegocio;
    }

    public void setColoniaVivienda(String coloniaVivienda) {
        this.coloniaVivienda = coloniaVivienda;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public void setCuentas_uso_familia(Integer cuentas_uso_familia) {
        this.cuentas_uso_familia = cuentas_uso_familia;
    }

    public void setCuentas_uso_negocio(Integer cuentas_uso_negocio) {
        this.cuentas_uso_negocio = cuentas_uso_negocio;
    }

    public void setDatos_moviles(String datos_moviles) {
        this.datos_moviles = datos_moviles;
    }

    public void setDepartamento_dpi_solicitante(String departamento_dpi_solicitante) {
        this.departamento_dpi_solicitante = departamento_dpi_solicitante;
    }

    public void setDepartamento_negocio(String departamento_negocio) {
        this.departamento_negocio = departamento_negocio;
    }

    public void setDepartamento_solicitante(String departamento_solicitante) {
        this.departamento_solicitante = departamento_solicitante;
    }

    public void setDepartamento_vivienda(String departamento_vivienda) {
        this.departamento_vivienda = departamento_vivienda;
    }

    public void setDescripcion_giro_negocio(String descripcion_giro_negocio) {
        this.descripcion_giro_negocio = descripcion_giro_negocio;
    }

    public void setDetalle_compras(BigDecimal detalle_compras) {
        this.detalle_compras = detalle_compras;
    }

    public void setDetalle_de_inversion(String detalle_de_inversion) {
        this.detalle_de_inversion = detalle_de_inversion;
    }

    public void setDetalle_otros_ingresos(BigDecimal detalle_otros_ingresos) {
        this.detalle_otros_ingresos = detalle_otros_ingresos;
    }

    public void setDetalle_prendaria(String detalle_prendaria) {
        this.detalle_prendaria = detalle_prendaria;
    }

    public void setDetalle_recuperacion_cuentas(BigDecimal detalle_recuperacion_cuentas) {
        this.detalle_recuperacion_cuentas = detalle_recuperacion_cuentas;
    }

    public void setDetalle_ventas(BigDecimal detalle_ventas) {
        this.detalle_ventas = detalle_ventas;
    }

    public void setEdad_solicitante(Integer edad_solicitante) {
        this.edad_solicitante = edad_solicitante;
    }

    public void setEfectivo_uso_familia(BigDecimal efectivo_uso_familia) {
        this.efectivo_uso_familia = efectivo_uso_familia;
    }

    public void setEfectivo_uso_negocio(BigDecimal efectivo_uso_negocio) {
        this.efectivo_uso_negocio = efectivo_uso_negocio;
    }

    public void setEntorno_del_negocio(String entorno_del_negocio) {
        this.entorno_del_negocio = entorno_del_negocio;
    }

    public void setEscolaridad_solicitante(String escolaridad_solicitante) {
        this.escolaridad_solicitante = escolaridad_solicitante;
    }

    public void setEstado_civil_solicitante(String estado_civil_solicitante) {
        this.estado_civil_solicitante = estado_civil_solicitante;
    }

    public void setEtnia_maya(String etnia_maya) {
        this.etnia_maya = etnia_maya;
    }

    public void setEtnia_no_maya(String etnia_no_maya) {
        this.etnia_no_maya = etnia_no_maya;
    }

    public void setExplique_el_tema(String explique_el_tema) {
        this.explique_el_tema = explique_el_tema;
    }

    public void setFacilitador(String facilitador) {
        this.facilitador = facilitador;
    }

    public void setFecha_estacionalidad(LocalDate fecha_estacionalidad) {
        this.fecha_estacionalidad = fecha_estacionalidad;
    }

    public void setFecha_inico_operaciones(LocalDate fecha_inico_operaciones) {
        this.fecha_inico_operaciones = fecha_inico_operaciones;
    }

    public void setFecha_integraciones(LocalDate fecha_integraciones) {
        this.fecha_integraciones = fecha_integraciones;
    }

    public void setFecha_inventario(LocalDate fecha_inventario) {
        this.fecha_inventario = fecha_inventario;
    }

    public void setFecha_nacimiento_solicitante(LocalDate fecha_nacimiento_solicitante) {
        this.fecha_nacimiento_solicitante = fecha_nacimiento_solicitante;
    }

    public void setFecha_visita(LocalDate fecha_visita) {
        this.fecha_visita = fecha_visita;
    }

    public void setFrecuencia_compras(String frecuencia_compras) {
        this.frecuencia_compras = frecuencia_compras;
    }

    public void setFrecuencia_ventas(String frecuencia_ventas) {
        this.frecuencia_ventas = frecuencia_ventas;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public void setGrupo_etnico(String grupo_etnico) {
        this.grupo_etnico = grupo_etnico;
    }

    public void setHabla_espaniol(String habla_espaniol) {
        this.habla_espaniol = habla_espaniol;
    }

    public void setInstitucion(String institucion) {
        this.institucion = institucion;
    }

    public void setInversion_actual(String inversion_actual) {
        this.inversion_actual = inversion_actual;
    }

    public void setLocal_negocio(String local_negocio) {
        this.local_negocio = local_negocio;
    }

    public void setLote_negocio(String lote_negocio) {
        this.lote_negocio = lote_negocio;
    }

    public void setLote_vivienda(String lote_vivienda) {
        this.lote_vivienda = lote_vivienda;
    }

    public void setManzana_negocio(String manzana_negocio) {
        this.manzana_negocio = manzana_negocio;
    }

    public void setManzana_vivienda(String manzana_vivienda) {
        this.manzana_vivienda = manzana_vivienda;
    }

    public void setMunicipio_dpi_solicitante(String municipio_dpi_solicitante) {
        this.municipio_dpi_solicitante = municipio_dpi_solicitante;
    }

    public void setMunicipio_negocio(String municipio_negocio) {
        this.municipio_negocio = municipio_negocio;
    }

    public void setMunicipio_solicitante(String municipio_solicitante) {
        this.municipio_solicitante = municipio_solicitante;
    }

    public void setMunicipio_vivienda(String municipio_vivienda) {
        this.municipio_vivienda = municipio_vivienda;
    }

    public void setNacimiento_solicitante(String nacimiento_solicitante) {
        this.nacimiento_solicitante = nacimiento_solicitante;
    }

    public void setNit_negocio(String nit_negocio) {
        this.nit_negocio = nit_negocio;
    }

    public void setNo_casa_vivienda(String no_casa_vivienda) {
        this.no_casa_vivienda = no_casa_vivienda;
    }

    public void setNombre_negocio(String nombre_negocio) {
        this.nombre_negocio = nombre_negocio;
    }

    public void setNum_contador_vivienda(Integer num_contador_vivienda) {
        this.num_contador_vivienda = num_contador_vivienda;
    }

    public void setNumero_fiadores(Integer numero_fiadores) {
        this.numero_fiadores = numero_fiadores;
    }

    public void setObservaciones_visita(String observaciones_visita) {
        this.observaciones_visita = observaciones_visita;
    }

    public void setOtros_activos_familia(BigDecimal otros_activos_familia) {
        this.otros_activos_familia = otros_activos_familia;
    }

    public void setOtros_activos_negocio(BigDecimal otros_activos_negocio) {
        this.otros_activos_negocio = otros_activos_negocio;
    }

    public void setOtros_ingresos_de_la_solicitante(String otros_ingresos_de_la_solicitante) {
        this.otros_ingresos_de_la_solicitante = otros_ingresos_de_la_solicitante;
    }

    public void setPatente_sociedad(String patente_sociedad) {
        this.patente_sociedad = patente_sociedad;
    }

    public void setPrimer_apellido_solicitante(String primer_apellido_solicitante) {
        this.primer_apellido_solicitante = primer_apellido_solicitante;
    }

    public void setPrimer_nombre_solicitante(String primer_nombre_solicitante) {
        this.primer_nombre_solicitante = primer_nombre_solicitante;
    }

    public void setProfesion_solicitante(String profesion_solicitante) {
        this.profesion_solicitante = profesion_solicitante;
    }

    public void setPunto_de_referencia(String punto_de_referencia) {
        this.punto_de_referencia = punto_de_referencia;
    }

    public void setRazon_social(String razon_social) {
        this.razon_social = razon_social;
    }

    public void setReferencias_vecinos(String referencias_vecinos) {
        this.referencias_vecinos = referencias_vecinos;
    }

    public void setSector_economico_negocio(String sector_economico_negocio) {
        this.sector_economico_negocio = sector_economico_negocio;
    }

    public void setSector_negocio(String sector_negocio) {
        this.sector_negocio = sector_negocio;
    }

    public void setSector_vivienda(String sector_vivienda) {
        this.sector_vivienda = sector_vivienda;
    }

    public void setSegundo_apellido_solicitante(String segundo_apellido_solicitante) {
        this.segundo_apellido_solicitante = segundo_apellido_solicitante;
    }

    public void setSegundo_nombre_solicitante(String segundo_nombre_solicitante) {
        this.segundo_nombre_solicitante = segundo_nombre_solicitante;
    }

    public void setTasa(BigDecimal tasa) {
        this.tasa = tasa;
    }

    public void setTelefono_fijo(String telefono_fijo) {
        this.telefono_fijo = telefono_fijo;
    }

    public void setTelefono_negocio(String telefono_negocio) {
        this.telefono_negocio = telefono_negocio;
    }

    public void setTiene_correo(String tiene_correo) {
        this.tiene_correo = tiene_correo;
    }

    public void setTipo_credito(String tipo_credito) {
        this.tipo_credito = tipo_credito;
    }

    public void setTipo_direccion_negocio(String tipo_direccion_negocio) {
        this.tipo_direccion_negocio = tipo_direccion_negocio;
    }

    public void setTotal_costo_ventas(BigDecimal total_costo_ventas) {
        this.total_costo_ventas = total_costo_ventas;
    }

    public void setTotal_cuentas_por_cobrar(BigDecimal total_cuentas_por_cobrar) {
        this.total_cuentas_por_cobrar = total_cuentas_por_cobrar;
    }

    public void setTotal_cuota_mensual(BigDecimal total_cuota_mensual) {
        this.total_cuota_mensual = total_cuota_mensual;
    }

    public void setTotal_deuda(BigDecimal total_deuda) {
        this.total_deuda = total_deuda;
    }

    public void setTotal_efectivo(BigDecimal total_efectivo) {
        this.total_efectivo = total_efectivo;
    }

    public void setTotal_gastos_negocio(BigDecimal total_gastos_negocio) {
        this.total_gastos_negocio = total_gastos_negocio;
    }

    public void setTotal_gastos_vivienda(BigDecimal total_gastos_vivienda) {
        this.total_gastos_vivienda = total_gastos_vivienda;
    }

    public void setTotal_inmueble_familia(BigDecimal total_inmueble_familia) {
        this.total_inmueble_familia = total_inmueble_familia;
    }

    public void setTotal_inmueble_negocio(BigDecimal total_inmueble_negocio) {
        this.total_inmueble_negocio = total_inmueble_negocio;
    }

    public void setTotal_inmuebles(BigDecimal total_inmuebles) {
        this.total_inmuebles = total_inmuebles;
    }

    public void setTotal_inventario(BigDecimal total_inventario) {
        this.total_inventario = total_inventario;
    }

    public void setTotal_maquinaria(BigDecimal total_maquinaria) {
        this.total_maquinaria = total_maquinaria;
    }

    public void setTotal_menaje_de_hogar(BigDecimal total_menaje_de_hogar) {
        this.total_menaje_de_hogar = total_menaje_de_hogar;
    }

    public void setTotal_mobiliario_equipo(BigDecimal total_mobiliario_equipo) {
        this.total_mobiliario_equipo = total_mobiliario_equipo;
    }

    public void setTotal_otros_activos(BigDecimal total_otros_activos) {
        this.total_otros_activos = total_otros_activos;
    }

    public void setTotal_precio_ventas(BigDecimal total_precio_ventas) {
        this.total_precio_ventas = total_precio_ventas;
    }

    public void setTotal_recibido(BigDecimal total_recibido) {
        this.total_recibido = total_recibido;
    }

    public void setTotal_vehiculo_familia(Integer total_vehiculo_familia) {
        this.total_vehiculo_familia = total_vehiculo_familia;
    }

    public void setTotal_vehiculo_negocio(Integer total_vehiculo_negocio) {
        this.total_vehiculo_negocio = total_vehiculo_negocio;
    }

    public void setTotal_vehiculos(Integer total_vehiculos) {
        this.total_vehiculos = total_vehiculos;
    }

    public void setUsa_facebook(String usa_facebook) {
        this.usa_facebook = usa_facebook;
    }

    public void setVerificacion_negocio(String verificacion_negocio) {
        this.verificacion_negocio = verificacion_negocio;
    }

    public void setVerificacion_vivienda(String verificacion_vivienda) {
        this.verificacion_vivienda = verificacion_vivienda;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public void setZona_negocio(Integer zona_negocio) {
        this.zona_negocio = zona_negocio;
    }

    public void setZona_vivienda(Integer zona_vivienda) {
        this.zona_vivienda = zona_vivienda;
    }

    public void setDescripcionNegocio(String descripcionNegocio) {
        this.descripcionNegocio = descripcionNegocio;
    }

    public void setDetalle_fiadores(String detalle_fiadores) {
        this.detalle_fiadores = detalle_fiadores;
    }

    public void setDpi_solicitante(String dpi_solicitante) {
        this.dpi_solicitante = dpi_solicitante;
    }

    public void setRecuperacion_cuentasnal(String recuperacion_cuentas) {
        this.recuperacion_cuentas = recuperacion_cuentas;
    }

    public void setRecuperacion_cuentas(String recuperacion_cuentas) {
        this.recuperacion_cuentas = recuperacion_cuentas;
    }

    public void setTercer_nombre_solicitante(String tercer_nombre_solicitante) {
        this.tercer_nombre_solicitante = tercer_nombre_solicitante;
    }
}
