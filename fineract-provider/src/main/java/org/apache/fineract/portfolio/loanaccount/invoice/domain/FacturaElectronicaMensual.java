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
public class FacturaElectronicaMensual extends AbstractPersistableCustom implements Cloneable {

    @Column(name = CREATED_DATE_DB_FIELD)
    private OffsetDateTime createdDate;

    @Column(name = "num_resolucion")
    private String numResolucion;

    @Column(name = "prefijo")
    private String prefijo;

    @Column(name = "fec_desde")
    private LocalDate fecDesde;

    @Column(name = "fec_hasta")
    private LocalDate fecHasta;

    @Column(name = "consecutivo_inicial")
    private Long consecutivoInicial;

    @Column(name = "consecutivo_final")
    private Long consecutivoFinal;

    @Column(name = "clave_tecnica")
    private String claveTecnica;

    @Column(name = "nota")
    private String nota;

    @Column(name = "numero_doc")
    private String numeroDoc;

    @Column(name = "tip_doc")
    private String tipDoc;

    @Column(name = "fecha_factura")
    private LocalDate fechaFactura;

    @Column(name = "tipo_factura")
    private String tipoFactura;

    @Column(name = "moneda")
    private String moneda;

    @Column(name = "forma_pago")
    private String formaPago;

    @Column(name = "medio_pago")
    private String medioPago;

    @Column(name = "fecha_vence")
    private LocalDate fechaVence;

    @Column(name = "fecha_inicial")
    private LocalDate fechaInicial;

    @Column(name = "fecha_final")
    private LocalDate fechaFinal;

    @Column(name = "est_fact")
    private String estFact;

    @Column(name = "num_facafect")
    private String numFacafect;

    @Column(name = "fec_facafect")
    private LocalDate fecFacafect;

    @Column(name = "total_unidades")
    private String totalUnidades;

    @Column(name = "logo")
    private String logo;

    @Column(name = "nit_emisor")
    private String nitEmisor;

    @Column(name = "nom_emisor")
    private String nomEmisor;

    @Column(name = "inf_tributaria")
    private String infTributaria;

    @Column(name = "cod_pais_tienda")
    private String codPaisTienda;

    @Column(name = "nom_pais_tienda")
    private String nomPaisTienda;

    @Column(name = "dep_tienda")
    private String depTienda;

    @Column(name = "nom_dep_tienda")
    private String nomDepTienda;

    @Column(name = "cod_mun_tienda")
    private String codMunTienda;

    @Column(name = "ciudad_tienda")
    private String ciudadTienda;

    @Column(name = "nombre_tienda")
    private String nombreTienda;

    @Column(name = "direccion_tienda")
    private String direccionTienda;

    @Column(name = "tel_tienda")
    private String telTienda;

    @Column(name = "email_tienda")
    private String emailTienda;

    @Column(name = "id_cliente")
    private String idCliente;

    @Column(name = "tipo_docid")
    private String tipoDocid;

    @Column(name = "tipo_pers")
    private Long tipoPers;

    @Column(name = "nombre_cliente")
    private String nombreCliente;

    @Column(name = "apellido_cliente")
    private String apellidoCliente;

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
    private BigDecimal costoTotal;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Column(name = "sku")
    private String sku;

    @Column(name = "nom_articulo")
    private String nomArticulo;

    @Column(name = "referencia")
    private String referencia;

    @Column(name = "id_mandante")
    private String idMandante;

    @Column(name = "descripcion_mandante")
    private String descripcionMandante;

    @Column(name = "codigo_descuento")
    private String codigoDescuento;

    @Column(name = "porcentajedescuento")
    private BigDecimal porcentajedescuento;

    @Column(name = "descuento")
    private BigDecimal descuento;

    @Column(name = "porcentaje_impuesto_item")
    private BigDecimal porcentajeImpuestoItem;

    @Column(name = "impuesto_item")
    private BigDecimal impuestoItem;

    @Column(name = "iva_codigo")
    private String ivaCodigo;

    @Column(name = "iva_name")
    private String ivaName;

    @Column(name = "base")
    private BigDecimal base;

    @Column(name = "porcentaje_impuesto")
    private BigDecimal porcentajeImpuesto;

    @Column(name = "impuesto")
    private BigDecimal impuesto;

    @Column(name = "por_dto")
    private BigDecimal porDto;

    @Column(name = "val_dto")
    private BigDecimal valDto;

    @Column(name = "total")
    private BigDecimal total;

    @Column(name = "nota2")
    private String nota2;

    @Column(name = "tas_cambmon")
    private String tasCambmon;

    @Column(name = "cod_moncamb")
    private String codMoncamb;

    @Column(name = "tot_basimpo")
    private BigDecimal totBasimpo;

    @Column(name = "tot_facmon")
    private BigDecimal totFacmon;

    @Column(name = "tip_factexport")
    private BigDecimal tipFactexport;

    @Column(name = "tipo_prod")
    private String tipoProd;

    @Column(name = "loan_transaction_id")
    private Long loanTransactionId;

    public BigDecimal getImpuestoItem() {
        return this.impuestoItem == null ? BigDecimal.ZERO : this.impuestoItem;
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
