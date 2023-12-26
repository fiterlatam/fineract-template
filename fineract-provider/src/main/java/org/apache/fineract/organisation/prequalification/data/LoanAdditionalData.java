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
package org.apache.fineract.organisation.prequalification.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.organisation.prequalification.domain.LoanAdditionProperties;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
@NoArgsConstructor
public class LoanAdditionalData {

    private Long id;

    private Long loanId;

    private Long clientId;

    private String caseId;

    private Integer ciclosCancelados;

    private Long branchCode;

    private String cargoTesorera;

    private String cargo;

    private String estadoSolicitud;

    private LocalDate fechaInicio;

    private String producto;

    private LocalDate fechaSolicitud;

    private String codigoCliente;

    private String actividadNegocio;

    private BigDecimal activoCorriente;

    private BigDecimal activoNocorriente;

    private BigDecimal alimentacion;

    private BigDecimal alquilerCliente;

    private BigDecimal alquilerGasto;

    private BigDecimal alquilerLocal;

    private String antiguedadNegocio;

    private String apoyoFamilia;

    private Integer aprobacionesBc;

    private String area;

    private Integer bienesInmuebles;

    private Integer bienesInmueblesFamiliares;

    private String cDpi;

    private Integer cEdad;

    private LocalDate cFechaNacimiento;

    private String cOtroNombre;

    private String cPrimerApellido;

    private String cPrimerNombre;

    private String cProfesion;

    private String cSegundoApellido;

    private String cSegundoNombre;

    private String cTelefono;

    private BigDecimal capacidadPago;

    private BigDecimal comunalVigente;

    private BigDecimal costoUnitario;

    private BigDecimal costoVenta;

    private BigDecimal cuantoPagar;

    private BigDecimal cuentasPorPagar;

    private Integer cuota;

    private Integer cuotaOtros;

    private Integer cuotaPuente;

    private Integer cuotasPendientesBc;

    private Integer dependientes;

    private String destinoPrestamo;

    private Integer educacion;

    private BigDecimal efectivo;

    private BigDecimal endeudamientoActual;

    private BigDecimal endeudamientoFuturo;

    private Integer enf;

    private String escribe;

    private String evolucionNegocio;

    private String fPep;

    private Integer familiares;

    private LocalDate fechaPrimeraReunion;

    private Integer flujoDisponible;

    private String garantiaPrestamo;

    private BigDecimal gastosFamiliares;

    private BigDecimal gastosNegocio;

    private Integer herramientas;

    private Integer hijos;

    private Integer mortgages;

    private Integer impuestos;

    private String ingresadoPor;

    private BigDecimal ingresoFamiliar;

    private Integer integrantesAdicional;

    private BigDecimal inventarios;

    private BigDecimal inversionTotal;

    private String invertir;

    private String lee;

    private BigDecimal menajeDelHogar;

    private BigDecimal mobiliarioYequipo;

    private BigDecimal montoSolicitado;

    private String motivoSolicitud;

    private String nit;

    private String nombrePropio;

    private BigDecimal pasivoCorriente;

    private BigDecimal pasivoNoCorriente;

    private BigDecimal pensiones;

    private String pep;

    private Integer plazo;

    private Integer plazoVigente;

    private String poseeCuenta;

    private Long prestamoPuente;

    private BigDecimal propuestaFacilitador;

    private String puntoReunion;

    private BigDecimal relacionGastos;

    private BigDecimal rentabilidadNeta;

    private BigDecimal rotacionInventario;

    private BigDecimal salarioCliente;

    private BigDecimal salarios;

    private String salud;

    private String servicios;

    private BigDecimal serviciosBasicos;

    private BigDecimal serviciosGasto;

    private BigDecimal serviciosMedicos;

    private Integer tarjetas;

    private String tipoVivienda;

    private BigDecimal totalActivo;

    private BigDecimal totalIngresos;

    private BigDecimal totalIngresosFamiliares;

    private BigDecimal totalPasivo;

    private BigDecimal transporteGasto;

    private BigDecimal transporteNegocio;

    private String ubicacionCliente;

    private String ubicacionNegocio;

    private BigDecimal utilidadBruta;

    private BigDecimal utilidadNeta;

    private Integer validFiador;

    private BigDecimal valorGarantia;

    private Integer vehiculos;

    private BigDecimal vestimenta;

    private String visitoNegocio;

    private String externalId;

    private String ownerId;

    private String caseName;

    private String caseType;

    private LocalDateTime dateOpened;

    private LocalDate fechaFin;

    private BigDecimal ventas;

    private BigDecimal cuentasPorCobrar;

    private BigDecimal hipotecas;

    private String excepcion;

    private Integer tipoExcepcion;

    private String descripcionExcepcion;

    private BigDecimal montoAutorizado;

    private String observaciones;

    private BigDecimal capitalDdeTrabajo;

    private BigDecimal montoOtrosIngresos;

    private String origenOtrosIngresos;

    private String otrosIngresos;

    private BigDecimal relacionOtrosIngresos;

    private String programa;

    private String aldeaVivienda;

    private Integer aniosComunidad;

    private Integer aniosDeActividadNegocio;

    private String apellidoCasadaSolicitante;

    private String cActividadEconomica;

    private String cApellidoDeCasada;

    private String cDepartamento;

    private String cDepartamentoDpi;

    private String cDescripcionNegocio;

    private String descripcionNegocio;

    private String cLugarNacimiento;

    private String cMunicipio;

    private String cMunicipioDpi;

    private String cNit;

    private String cSectorEconomico;

    private String calleNegocio;

    private String calleVivienda;

    private String casaNegocio;

    private String celularSolicitante;

    private String coloniaNegocio;

    private String coloniaVivienda;

    private String correoElectronico;

    private Integer cuentas_uso_familia;

    private Integer cuentas_uso_negocio;

    private String datos_moviles;

    private String departamento_dpi_solicitante;

    private String departamento_negocio;

    private String departamento_solicitante;

    private String departamento_vivienda;

    private String descripcion_giro_negocio;

    private BigDecimal detalle_compras;

    private String detalle_de_inversion;

    private BigDecimal detalle_otros_ingresos;

    private String detalle_prendaria;

    private BigDecimal detalle_recuperacion_cuentas;

    private BigDecimal detalle_ventas;

    private Integer edad_solicitante;

    private BigDecimal efectivo_uso_familia;

    private BigDecimal efectivo_uso_negocio;

    private String entorno_del_negocio;

    private String escolaridad_solicitante;

    private String estado_civil_solicitante;

    private String etnia_maya;

    private String etnia_no_maya;

    private String explique_el_tema;

    private String facilitador;

    private LocalDate fecha_estacionalidad;

    private LocalDate fecha_inico_operaciones;

    private LocalDate fecha_integraciones;

    private LocalDate fecha_inventario;

    private LocalDate fecha_nacimiento_solicitante;

    private LocalDate fecha_visita;

    private String frecuencia_compras;

    private String frecuencia_ventas;

    private String genero;

    private String grupo_etnico;

    private String habla_espaniol;

    private String institucion;

    private String inversion_actual;

    private String local_negocio;

    private String lote_negocio;

    private String lote_vivienda;

    private String manzana_negocio;

    private String manzana_vivienda;

    private String municipio_dpi_solicitante;

    private String municipio_negocio;

    private String municipio_solicitante;

    private String municipio_vivienda;

    private String nacimiento_solicitante;

    private String nit_negocio;

    private String no_casa_vivienda;

    private String nombre_negocio;

    private Integer num_contador_vivienda;

    private Integer numero_fiadores;

    private String observaciones_visita;

    private BigDecimal otros_activos_familia;

    private BigDecimal otros_activos_negocio;

    private String otros_ingresos_de_la_solicitante;

    private String patente_sociedad;

    private String primer_apellido_solicitante;

    private String primer_nombre_solicitante;

    private String profesion_solicitante;

    private String punto_de_referencia;

    private String razon_social;

    private String referencias_vecinos;

    private String sector_economico_negocio;

    private String sector_negocio;

    private String sector_vivienda;

    private String segundo_apellido_solicitante;

    private String segundo_nombre_solicitante;

    private BigDecimal tasa;

    private String telefono_fijo;

    private String telefono_negocio;

    private String tiene_correo;

    private String tipo_credito;

    private String tipo_direccion_negocio;

    private BigDecimal total_costo_ventas;

    private BigDecimal total_cuentas_por_cobrar;

    private BigDecimal total_cuota_mensual;

    private BigDecimal total_deuda;

    private BigDecimal total_efectivo;

    private BigDecimal total_gastos_negocio;

    private BigDecimal total_gastos_vivienda;

    private BigDecimal total_inmueble_familia;

    private BigDecimal total_inmueble_negocio;

    private BigDecimal total_inmuebles;

    private BigDecimal total_inventario;

    private BigDecimal total_maquinaria;

    private BigDecimal total_menaje_de_hogar;

    private BigDecimal total_mobiliario_equipo;

    private BigDecimal total_otros_activos;

    private BigDecimal total_precio_ventas;

    private BigDecimal total_recibido;

    private Integer total_vehiculo_familia;

    private Integer total_vehiculo_negocio;

    private Integer total_vehiculos;

    private String ubicacion_cliente;

    private String ubicacion_negocio;

    private String usa_facebook;

    private String verificacion_negocio;

    private String verificacion_vivienda;

    private String whatsapp;

    private Integer zona_negocio;

    private Integer zona_vivienda;

    private String detalle_fiadores;

    private String dpi_solicitante;

    private LocalDate fecha_solicitud;

    private String recuperacion_cuentas;

    private String tercer_nombre_solicitante;

    public LoanAdditionProperties toEntity() {
        final LoanAdditionProperties loanAdditionProperties = new LoanAdditionProperties();
        BeanUtils.copyProperties(this, loanAdditionProperties);
        return loanAdditionProperties;
    }
}
