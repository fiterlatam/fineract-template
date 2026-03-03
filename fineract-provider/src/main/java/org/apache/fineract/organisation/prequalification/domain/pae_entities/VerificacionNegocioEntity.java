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
package org.apache.fineract.organisation.prequalification.domain.pae_entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.prequalification.data.paeadditional.VerificacionNegocio;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "m_pae_verificacion_negocio")
public class VerificacionNegocioEntity extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "loan_additional_pae_id", nullable = false)
    private LoanAdditionalDataPAEEntity loanAdditionalDataPAE;

    @Column(name = "negocio_propio_y_manejado_por_cliente", length = 50)
    private String negocioPropioYManejadoPorCliente;

    @Column(name = "antiguedad_mayor_a_3_anios", length = 50)
    private String antiguedadMayorA3Anios;

    @Column(name = "fotocopia_tarjeta_de_salud", length = 50)
    private String fotocopiaTarjetaDeSalud;

    @Column(name = "boleta_o_tarjeta_derecho_de_piso", length = 50)
    private String boletaOTarjetaDerechoDePiso;

    @Column(name = "fotocopia_facturas_compra_venta", length = 50)
    private String fotocopiaFacturasCompraVenta;

    @Column(name = "copia_rtu", length = 50)
    private String copiaRTU;

    @Column(name = "fotografias_coinciden_con_expediente", length = 50)
    private String fotografiasCoincidenConExpediente;

    @Column(name = "valor_ventas_compras_coinciden_con_expediente", length = 50)
    private String valorVentasComprasCoincidenConExpediente;

    @Column(name = "negocio_ordenado_y_limpio", length = 50)
    private String negocioOrdenadoYLimpio;

    @Column(name = "negocio_concurrido", length = 50)
    private String negocioConcurrido;

    @Column(name = "negocio_elegible_segun_politica", length = 50)
    private String negocioElegibleSegunPolitica;

    @Column(name = "pago_de_prestamos_coinciden_con_expediente", length = 50)
    private String pagoDePrestamosCoincidenConExpediente;

    @Column(name = "ubicacion_negocio", length = 255)
    private String ubicacionNegocio;

    @Column(name = "nombre_negocio", length = 255)
    private String nombreNegocio;

    @Column(name = "descripcion_negocio", length = 500)
    private String descripcionNegocio;

    public VerificacionNegocio toDTO() {
        final VerificacionNegocio dto = new VerificacionNegocio();
        dto.setNegocioPropioYManejadoPorCliente(this.negocioPropioYManejadoPorCliente);
        dto.setAntiguedadMayorA3Anios(this.antiguedadMayorA3Anios);
        dto.setFotocopiaTarjetaDeSalud(this.fotocopiaTarjetaDeSalud);
        dto.setBoletaOTarjetaDerechoDePiso(this.boletaOTarjetaDerechoDePiso);
        dto.setFotocopiaFacturasCompraVenta(this.fotocopiaFacturasCompraVenta);
        dto.setCopiaRTU(this.copiaRTU);
        dto.setFotografiasCoincidenConExpediente(this.fotografiasCoincidenConExpediente);
        dto.setValorVentasComprasCoincidenConExpediente(this.valorVentasComprasCoincidenConExpediente);
        dto.setNegocioOrdenadoYLimpio(this.negocioOrdenadoYLimpio);
        dto.setNegocioConcurrido(this.negocioConcurrido);
        dto.setNegocioElegibleSegunPolitica(this.negocioElegibleSegunPolitica);
        dto.setPagoDePrestamosCoincidenConExpediente(this.pagoDePrestamosCoincidenConExpediente);
        dto.setUbicacionNegocio(this.ubicacionNegocio);
        dto.setNombreNegocio(this.nombreNegocio);
        dto.setDescripcionNegocio(this.descripcionNegocio);
        return dto;
    }

    public static VerificacionNegocioEntity fromDTO(final VerificacionNegocio dto) {
        final VerificacionNegocioEntity entity = new VerificacionNegocioEntity();
        entity.setNegocioPropioYManejadoPorCliente(dto.getNegocioPropioYManejadoPorCliente());
        entity.setAntiguedadMayorA3Anios(dto.getAntiguedadMayorA3Anios());
        entity.setFotocopiaTarjetaDeSalud(dto.getFotocopiaTarjetaDeSalud());
        entity.setBoletaOTarjetaDerechoDePiso(dto.getBoletaOTarjetaDerechoDePiso());
        entity.setFotocopiaFacturasCompraVenta(dto.getFotocopiaFacturasCompraVenta());
        entity.setCopiaRTU(dto.getCopiaRTU());
        entity.setFotografiasCoincidenConExpediente(dto.getFotografiasCoincidenConExpediente());
        entity.setValorVentasComprasCoincidenConExpediente(dto.getValorVentasComprasCoincidenConExpediente());
        entity.setNegocioOrdenadoYLimpio(dto.getNegocioOrdenadoYLimpio());
        entity.setNegocioConcurrido(dto.getNegocioConcurrido());
        entity.setNegocioElegibleSegunPolitica(dto.getNegocioElegibleSegunPolitica());
        entity.setPagoDePrestamosCoincidenConExpediente(dto.getPagoDePrestamosCoincidenConExpediente());
        entity.setUbicacionNegocio(dto.getUbicacionNegocio());
        entity.setNombreNegocio(dto.getNombreNegocio());
        entity.setDescripcionNegocio(dto.getDescripcionNegocio());
        return entity;
    }
}

