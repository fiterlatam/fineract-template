package org.apache.fineract.portfolio.loanaccount.invoice.domain;

import static org.apache.fineract.infrastructure.core.domain.AuditableFieldsConstants.CREATED_DATE_DB_FIELD;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "c_facturacion_electronica")
@Getter
@Setter
public class FacturaElectronicaMensual extends AbstractPersistableCustom {

    @Column(name = CREATED_DATE_DB_FIELD)
    private OffsetDateTime createdDate;

    @Column(name = "num_resolucion")
    private String num_resolucion;

    @Column(name = "prefijo")
    private String prefijo;

    @Column(name = "fec_desde")
    private LocalDate fec_desde;

    @Column(name = "fec_hasta")
    private LocalDate fec_hasta;

    @Column(name = "consecutivo_inicial")
    private Long consecutivo_inicial;

    @Column(name = "consecutivo_final")
    private Long consecutivo_final;

    @Column(name = "clave_tecnica")
    private String clave_tecnica;

    @Column(name = "nota")
    private String nota;

    @Column(name = "numero_doc")
    private String numero_doc;

    @Column(name = "tip_doc")
    private String tip_doc;

    @Column(name = "fecha_factura")
    private LocalDate fecha_factura;

    @Column(name = "tipo_factura")
    private String tipo_factura;

    @Column(name = "moneda")
    private String moneda;

    @Column(name = "forma_pago")
    private String forma_pago;

    @Column(name = "medio_pago")
    private String medio_pago;

    @Column(name = "fecha_vence")
    private LocalDate fecha_vence;

    @Column(name = "fecha_inicial")
    private LocalDate fecha_inicial;

    @Column(name = "fecha_final")
    private LocalDate fecha_final;

    @Column(name = "est_fact")
    private String est_fact;

    @Column(name = "num_facafect")
    private String num_facafect;

    @Column(name = "fec_facafect")
    private LocalDate fec_facafect;

    @Column(name = "total_unidades")
    private String total_unidades;

    @Column(name = "logo")
    private String logo;

    @Column(name = "nit_emisor")
    private String nit_emisor;

    @Column(name = "nom_emisor")
    private String nom_emisor;

    @Column(name = "inf_tributaria")
    private String inf_tributaria;

    @Column(name = "cod_pais_tienda")
    private String cod_pais_tienda;

    @Column(name = "nom_pais_tienda")
    private String nom_pais_tienda;

    @Column(name = "dep_tienda")
    private String dep_tienda;

    @Column(name = "nom_dep_tienda")
    private String nom_dep_tienda;

    @Column(name = "cod_mun_tienda")
    private String cod_mun_tienda;

    @Column(name = "ciudad_tienda")
    private String ciudad_tienda;

    @Column(name = "nombre_tienda")
    private String nombre_tienda;

    @Column(name = "direccion_tienda")
    private String direccion_tienda;

    @Column(name = "tel_tienda")
    private String tel_tienda;

    @Column(name = "email_tienda")
    private String email_tienda;

    @Column(name = "id_cliente")
    private String id_cliente;

    @Column(name = "tipo_docid")
    private String tipo_docid;

    @Column(name = "tipo_pers")
    private Long tipo_pers;

    @Column(name = "nombre_cliente")
    private String nombre_cliente;

    @Column(name = "apellido_cliente")
    private String apellido_cliente;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "ciudad")
    private String ciudad;

    @Column(name = "codigopostal")
    private String codigopostal;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "email")
    private String email;

    @Column(name = "posicion")
    private Long posicion;

    @Column(name = "cantidad")
    private BigDecimal cantidad;

    @Column(name = "costo_total")
    private BigDecimal costo_total;

    @Column(name = "precio_unitario")
    private BigDecimal precio_unitario;

    @Column(name = "sku")
    private String sku;

    @Column(name = "nom_articulo")
    private String nom_articulo;

    @Column(name = "referencia")
    private String referencia;

    @Column(name = "id_mandante")
    private String id_mandante;

    @Column(name = "descripcion_mandante")
    private String descripcion_mandante;

    @Column(name = "codigo_descuento")
    private String codigo_descuento;

    @Column(name = "porcentajedescuento")
    private BigDecimal porcentajedescuento;

    @Column(name = "descuento")
    private BigDecimal descuento;

    @Column(name = "porcentaje_impuesto_item")
    private BigDecimal porcentaje_impuesto_item;

    @Column(name = "impuesto_item")
    private BigDecimal impuesto_item;

    @Column(name = "iva_codigo")
    private String iva_codigo;

    @Column(name = "iva_name")
    private String iva_name;

    @Column(name = "base")
    private BigDecimal base;

    @Column(name = "porcentaje_impuesto")
    private BigDecimal porcentaje_impuesto;

    @Column(name = "impuesto")
    private BigDecimal impuesto;

    @Column(name = "por_dto")
    private BigDecimal por_dto;

    @Column(name = "val_dto")
    private BigDecimal val_dto;

    @Column(name = "total")
    private BigDecimal total;

    @Column(name = "nota2")
    private String nota2;

    @Column(name = "tas_cambmon")
    private String tas_cambmon;

    @Column(name = "cod_moncamb")
    private String cod_moncamb;

    @Column(name = "tot_basimpo")
    private BigDecimal tot_basimpo;

    @Column(name = "tot_facmon")
    private BigDecimal tot_facmon;

    @Column(name = "tip_factexport")
    private BigDecimal tip_factexport;

}
