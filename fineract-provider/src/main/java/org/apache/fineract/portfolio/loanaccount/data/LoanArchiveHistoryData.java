package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class LoanArchiveHistoryData {

    private String title;
    private Integer identificacion;
    private String primerNombre;
    private String segundoNombre;
    private String segundoApellido;
    private String estadoCliente;
    private String numeroObligacion;
    private String nitEmpresa;
    private String telefonoSac;
    private String celularSac;
    private String telefonoSac2;
    private String celularSac2;
    private String emailSac;
    private String direccionSac;
    private String barrioSac;
    private Integer ciudadSac;
    private String tipoCredito;
    private LocalDate fechaVencimiento;
    private Integer dias_mora;
    private Integer valorCuota;
    private BigDecimal capital;
    private BigDecimal intereses;
    private BigDecimal interesesDeMora;
    private BigDecimal ivaInteresDeMora;
    private BigDecimal segurosVoluntarios;
    private String puntoDeVenta;
    private String tipoDocumento;
    private String razonSocial;
    private String ciudadPuntoCredito;
    private String parentescoFamiliar;
    private String nombreFamiliar;
    private String parentescoFamiliar2;
    private String fechaFinanciacion;
    private String fechaNacimiento;
    private String genero;
    private String empresaLabora;
    private BigDecimal ingresos;
    private String periodicidad;
    private String antiguedadCliente;
    private String empresaReporta;
    private BigDecimal abono;
    private BigDecimal condonaciones;
    private String actividadLaboral;
    private String creEstado;
    private BigDecimal creSaldo;
    private BigDecimal cuoSaldo;
    private String cuoEstado;
    private String estadoCivil;
    private Integer numeroDeReprogramaciones;
    private String departamento;

}
