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
import lombok.Builder;
import lombok.Data;
import org.apache.fineract.organisation.prequalification.domain.LoanAdditionProperties;
import org.springframework.beans.BeanUtils;

@Data
@Builder
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

    public LoanAdditionProperties toEntity() {
        final LoanAdditionProperties loanAdditionProperties = new LoanAdditionProperties();
        BeanUtils.copyProperties(this, loanAdditionProperties);
        return loanAdditionProperties;
    }
}
