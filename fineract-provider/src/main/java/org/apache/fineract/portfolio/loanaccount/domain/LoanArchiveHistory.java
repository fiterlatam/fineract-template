package org.apache.fineract.portfolio.loanaccount.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "m_archive_loan_history")
@Cacheable(false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class LoanArchiveHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "title", unique = true)
    private String title;
    @Column(name = "identificacion")
    private Integer identificacion;
    @Column(name = "primer_nombre")
    private String primerNombre;
    @Column(name = "segundo_nombre")
    private String segundoNombre;
    @Column(name = "primer_apellido")
    private String primerApellido;
    @Column(name = "segundo_apellido")
    private String segundoApellido;
    @Column(name = "estado_cliente")
    private String estadoCliente;
    @Column(name = "numero_obligacion")
    private String numeroObligacion;
    @Column(name = "nit_empresa")
    private String nitEmpresa;
    @Column(name = "telefono_sac_1")
    private String telefonoSac;
    @Column(name = "celular_sac_1")
    private String celularSac;
    @Column(name = "telefono_sac_2")
    private String telefonoSac2;
    @Column(name = "celular_sac_2")
    private String celularSac2;
    @Column(name = "email_sac")
    private String emailSac;
    @Column(name = "direccion_sac")
    private String direccionSac;
    @Column(name = "barrio_sac")
    private String barrioSac;
    @Column(name = "ciudad_sac")
    private Integer ciudadSac;
    @Column(name = "departamento")
    private String departamento;
    @Column(name = "tipo_credito")
    private String tipoCredito;
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
    @Column(name = "dias_mora")
    private Long diasMora;
    @Column(name = "valor_cuota")
    private BigDecimal valorCuota;
    @Column(name = "capital")
    private BigDecimal capital;
    @Column(name = "aval")
    private BigDecimal aval;
    @Column(name = "intereses")
    private BigDecimal intereses;
    @Column(name = "intereses_de_mora")
    private BigDecimal interesesDeMora;
    @Column(name = "iva_interes_de_mora")
    private BigDecimal ivaInteresDeMora;
    @Column(name = "seguro")
    private BigDecimal seguro;
    @Column(name = "seguros_voluntarios")
    private BigDecimal segurosVoluntarios;
    @Column(name = "punto_de_venta")
    private String puntoDeVenta;
    @Column(name = "tipo_documento")
    private String tipoDocumento;
    @Column(name = "empresa")
    private String empresa;
    @Column(name = "marca")
    private String marca;
    @Column(name = "razon_social")
    private String razonSocial;
    @Column(name = "ciudad_punto_credito")
    private String ciudadPuntoCredito;
    @Column(name = "estado_cuota")
    private String estadoCuota;
    @Column(name = "parentesco_familiar_1")
    private String parentescoFamiliar;
    @Column(name = "nombre_familiar_1")
    private String nombreFamiliar;
    @Column(name = "parentesco_familiar_2")
    private String parentescoFamiliar2;
    @Column(name = "fecha_financiacion")
    private LocalDate fechaFinanciacion;
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;
    @Column(name = "genero")
    private String genero;
    @Column(name = "empresa_labora")
    private String empresaLabora;
    @Column(name = "ingresos")
    private BigDecimal ingresos;
    @Column(name = "periodicidad")
    private String periodicidad;
    @Column(name = "antiguedad_cliente")
    private LocalDate antiguedadCliente;
    @Column(name = "empresa_reporta")
    private String empresaReporta;
    @Column(name = "abono")
    private BigDecimal abono;
    @Column(name = "condonaciones")
    private BigDecimal condonaciones;
    @Column(name = "actividad_laboral")
    private String actividadLaboral;
    @Column(name = "cre_estado")
    private String creEstado;
    @Column(name = "cre_saldo")
    private BigDecimal creSaldo;
    @Column(name = "cuo_saldo")
    private BigDecimal cuoSaldo;
    @Column(name = "cuo_estado")
    private String cuoEstado;
    @Column(name = "estado_civil")
    private String estadoCivil;
    @Column(name = "numero_de_reprogramaciones")
    private Integer numeroDeReprogramaciones;
}
