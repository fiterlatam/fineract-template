package org.apache.fineract.portfolio.loanaccount.invoice.domain;

import static org.apache.fineract.infrastructure.core.domain.AuditableFieldsConstants.CREATED_DATE_DB_FIELD;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
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
public class FacturaElectronicaMensual extends AbstractPersistableCustom implements Cloneable {

    @Column(name = CREATED_DATE_DB_FIELD)
    private OffsetDateTime createdDate;

    @Column(name = "num_resolucion", length = 50)
    @Size(max = 50)
    private String num_resolucion;

    @Column(name = "prefijo", length = 6)
    @Size(max = 6)
    private String prefijo;

    @Column(name = "fec_desde")
    private LocalDate fec_desde;

    @Column(name = "fec_hasta")
    private LocalDate fec_hasta;

    @Column(name = "consecutivo_inicial")
    private Long consecutivo_inicial;

    @Column(name = "consecutivo_final")
    private Long consecutivo_final;

    @Column(name = "clave_tecnica", length = 100)
    @Size(max = 100)
    private String clave_tecnica;

    @Column(name = "nota")
    private String nota;

    @Column(name = "numero_doc", length = 35)
    @Size(max = 35)
    private String numero_doc;

    @Column(name = "tip_doc", length = 6)
    @Size(max = 6)
    private String tip_doc;

    @Column(name = "fecha_factura")
    private LocalDate fecha_factura;

    @Column(name = "tipo_factura", length = 6)
    @Size(max = 6)
    private String tipo_factura;

    @Column(name = "moneda", length = 3)
    @Size(max = 3)
    private String moneda;

    @Column(name = "forma_pago", length = 1)
    @Size(max = 1)
    private String forma_pago;

    @Column(name = "medio_pago", length = 5)
    @Size(max = 5)
    private String medio_pago;

    @Column(name = "fecha_vence")
    private LocalDate fecha_vence;

    @Column(name = "fecha_inicial")
    private LocalDate fecha_inicial;

    @Column(name = "fecha_final")
    private LocalDate fecha_final;

    @Column(name = "est_fact", length = 1)
    @Size(max = 1)
    private String est_fact;

    @Column(name = "num_facafect", length = 35)
    @Size(max = 35)
    private String num_facafect;

    @Column(name = "fec_facafect")
    private LocalDate fec_facafect;

    @Column(name = "total_unidades", length = 3)
    @Size(max = 3)
    private String total_unidades;

    @Column(name = "logo", length = 5)
    @Size(max = 5)
    private String logo;

    @Column(name = "nit_emisor", length = 35)
    @Size(max = 35)
    private String nit_emisor;

    @Column(name = "nom_emisor", length = 100)
    @Size(max = 100)
    private String nom_emisor;

    @Column(name = "inf_tributaria")
    private String inf_tributaria;

    @Column(name = "cod_pais_tienda", length = 5)
    @Size(max = 5)
    private String cod_pais_tienda;

    @Column(name = "nom_pais_tienda", length = 40)
    @Size(max = 40)
    private String nom_pais_tienda;

    @Column(name = "dep_tienda", length = 5)
    @Size(max = 5)
    private String dep_tienda;

    @Column(name = "nom_dep_tienda", length = 40)
    @Size(max = 40)
    private String nom_dep_tienda;

    @Column(name = "cod_mun_tienda", length = 10)
    @Size(max = 10)
    private String cod_mun_tienda;

    @Column(name = "ciudad_tienda", length = 40)
    @Size(max = 40)
    private String ciudad_tienda;

    @Column(name = "nombre_tienda", length = 100)
    @Size(max = 100)
    private String nombre_tienda;

    @Column(name = "direccion_tienda", length = 50)
    @Size(max = 50)
    private String direccion_tienda;

    @Column(name = "tel_tienda", length = 35)
    @Size(max = 35)
    private String tel_tienda;

    @Column(name = "email_tienda", length = 100)
    @Size(max = 100)
    private String email_tienda;

    @Column(name = "id_cliente", length = 35)
    @Size(max = 35)
    private String id_cliente;

    @Column(name = "tipo_docid", length = 5)
    @Size(max = 5)
    private String tipo_docid;

    @Column(name = "tipo_pers")
    private Long tipo_pers;

    @Column(name = "nombre_cliente", length = 100)
    @Size(max = 100)
    private String nombre_cliente;

    @Column(name = "apellido_cliente", length = 100)
    @Size(max = 100)
    private String apellido_cliente;

    @Column(name = "direccion", length = 200)
    @Size(max = 200)
    private String direccion;

    @Column(name = "ciudad", length = 100)
    @Size(max = 100)
    private String ciudad;

    @Column(name = "codigopostal", length = 10)
    @Size(max = 10)
    private String codigopostal;

    @Column(name = "telefono", length = 35)
    @Size(max = 35)
    private String telefono;

    @Column(name = "email", length = 100)
    @Size(max = 100)
    private String email;

    @Column(name = "posicion")
    private Long posicion;

    @Column(name = "cantidad")
    private BigDecimal cantidad;

    @Column(name = "costo_total")
    private BigDecimal costo_total;

    @Column(name = "precio_unitario")
    private BigDecimal precio_unitario;

    @Column(name = "sku", length = 150)
    @Size(max = 150)
    private String sku;

    @Column(name = "nom_articulo", length = 150)
    @Size(max = 150)
    private String nom_articulo;

    @Column(name = "referencia", length = 150)
    @Size(max = 150)
    private String referencia;

    @Column(name = "id_mandante", length = 150)
    @Size(max = 150)
    private String id_mandante;

    @Column(name = "descripcion_mandante", length = 150)
    @Size(max = 150)
    private String descripcion_mandante;

    @Column(name = "codigo_descuento", length = 3)
    @Size(max = 3)
    private String codigo_descuento;

    @Column(name = "porcentajedescuento")
    private BigDecimal porcentajedescuento;

    @Column(name = "descuento")
    private BigDecimal descuento;

    @Column(name = "porcentaje_impuesto_item")
    private BigDecimal porcentaje_impuesto_item;

    @Column(name = "impuesto_item")
    private BigDecimal impuesto_item;

    @Column(name = "iva_codigo", length = 2)
    @Size(max = 2)
    private String iva_codigo;

    @Column(name = "iva_name", length = 3)
    @Size(max = 3)
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

    @Column(name = "tas_cambmon", length = 35)
    @Size(max = 35)
    private String tas_cambmon;

    @Column(name = "cod_moncamb", length = 3)
    @Size(max = 3)
    private String cod_moncamb;

    @Column(name = "tot_basimpo")
    private BigDecimal tot_basimpo;

    @Column(name = "tot_facmon")
    private BigDecimal tot_facmon;

    @Column(name = "tip_factexport")
    private BigDecimal tip_factexport;

    @Column(name = "tipo_prod", length = 100)
    @Size(max = 100)
    private String tipo_prod;

    @Column(name = "loan_transaction_id")
    private Long loan_transaction_id;

    public BigDecimal getImpuesto_item() {
        return this.impuesto_item == null ? BigDecimal.ZERO : this.impuesto_item;
    }

    public BigDecimal getTotal() {
        return this.total == null ? BigDecimal.ZERO : this.total;
    }

    @Override
    public FacturaElectronicaMensual clone() {
        try {
            return (FacturaElectronicaMensual) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
