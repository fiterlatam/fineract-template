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
import org.apache.fineract.organisation.prequalification.data.paeadditional.EntrevistaCliente;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "m_pae_entrevista_cliente")
public class EntrevistaClienteEntity extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "loan_additional_pae_id", nullable = false)
    private LoanAdditionalDataPAEEntity loanAdditionalDataPAE;

    @Column(name = "entienden_lo_que_dice_el_facilitador", length = 50)
    private String entiendenLoQueDiceElFacilitador;

    @Column(name = "tiene_claro_tasa_de_interes", length = 50)
    private String tieneClaroTasaDeInteres;

    @Column(name = "monto_y_plazo_coinciden_con_expediente", length = 50)
    private String montoYPlazoCoincidenConExpediente;

    @Column(name = "realizo_algun_pago_al_facilitador", length = 50)
    private String realizoAlgunPagoAlFacilitador;

    @Column(name = "destino_del_prestamo_coincide_con_plan", length = 50)
    private String destinoDelPrestamoCoincideConPlan;

    @Column(name = "tiene_claro_que_debe_de_participar_en_capacitaciones", length = 50)
    private String tieneClaroQueDebeDeParticiparEnCapacitaciones;

    @Column(name = "facilitador_atendio_bien_y_resolvio_sus_dudas", length = 50)
    private String facilitadorAtendioBienYResolvioSusDudas;

    @Column(name = "cliente_apto_para_continuar_con_el_proceso", length = 50)
    private String clienteAptoParaContinuarConElProceso;

    @Column(name = "tipo_de_garantia", length = 100)
    private String tipoDeGarantia;

    public EntrevistaCliente toDTO() {
        final EntrevistaCliente dto = new EntrevistaCliente();
        dto.setEntiendenLoQueDiceElFacilitador(this.entiendenLoQueDiceElFacilitador);
        dto.setTieneClaroTasaDeInteres(this.tieneClaroTasaDeInteres);
        dto.setMontoYPlazoCoincidenConExpediente(this.montoYPlazoCoincidenConExpediente);
        dto.setRealizoAlgunPagoAlFacilitador(this.realizoAlgunPagoAlFacilitador);
        dto.setDestinoDelPrestamoCoincideConPlan(this.destinoDelPrestamoCoincideConPlan);
        dto.setTieneClaroQueDebeDeParticiparEnCapacitaciones(this.tieneClaroQueDebeDeParticiparEnCapacitaciones);
        dto.setFacilitadorAtendioBienYResolvioSusDudas(this.facilitadorAtendioBienYResolvioSusDudas);
        dto.setClienteAptoParaContinuarConElProceso(this.clienteAptoParaContinuarConElProceso);
        dto.setTipoDeGarantia(this.tipoDeGarantia);
        return dto;
    }

    public static EntrevistaClienteEntity fromDTO(final EntrevistaCliente dto) {
        final EntrevistaClienteEntity entity = new EntrevistaClienteEntity();
        entity.setEntiendenLoQueDiceElFacilitador(dto.getEntiendenLoQueDiceElFacilitador());
        entity.setTieneClaroTasaDeInteres(dto.getTieneClaroTasaDeInteres());
        entity.setMontoYPlazoCoincidenConExpediente(dto.getMontoYPlazoCoincidenConExpediente());
        entity.setRealizoAlgunPagoAlFacilitador(dto.getRealizoAlgunPagoAlFacilitador());
        entity.setDestinoDelPrestamoCoincideConPlan(dto.getDestinoDelPrestamoCoincideConPlan());
        entity.setTieneClaroQueDebeDeParticiparEnCapacitaciones(dto.getTieneClaroQueDebeDeParticiparEnCapacitaciones());
        entity.setFacilitadorAtendioBienYResolvioSusDudas(dto.getFacilitadorAtendioBienYResolvioSusDudas());
        entity.setClienteAptoParaContinuarConElProceso(dto.getClienteAptoParaContinuarConElProceso());
        entity.setTipoDeGarantia(dto.getTipoDeGarantia());
        return entity;
    }
}

