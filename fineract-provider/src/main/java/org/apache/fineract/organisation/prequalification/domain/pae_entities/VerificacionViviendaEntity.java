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

import java.time.LocalDate;
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
import org.apache.fineract.organisation.prequalification.data.paeadditional.VerificacionVivienda;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "m_pae_verificacion_vivienda")
public class VerificacionViviendaEntity extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "loan_additional_pae_id")
    private LoanAdditionalDataPAEEntity loanAdditionalDataPAE;

    @Column(name = "fecha_supervision")
    private LocalDate fechaSupervision;

    @Column(name = "vivienda_propia", length = 50)
    private String viviendaPropia;

    @Column(name = "es_guatemalteca", length = 50)
    private String esGuatemalteca;

    @Column(name = "rango_edad_20_60", length = 50)
    private String rangoEdad20_60;

    @Column(name = "recibo_servicios_con_direccion_exacta", length = 50)
    private String reciboServiciosConDireccionExacta;

    @Column(name = "recibo_servicios_propio", length = 50)
    private String reciboServiciosPropio;

    @Column(name = "cuenta_con_servicios_basicos", length = 50)
    private String cuentaConServiciosBasicos;

    @Column(name = "direccion_coincide_con_expediente", length = 50)
    private String direccionCoincideConExpediente;

    @Column(name = "ubicacion_vivienda", length = 255)
    private String ubicacionVivienda;

    public VerificacionVivienda toDTO() {
        final VerificacionVivienda dto = new VerificacionVivienda();
        dto.setFechaSupervision(this.fechaSupervision);
        dto.setViviendaPropia(this.viviendaPropia);
        dto.setEsGuatemalteca(this.esGuatemalteca);
        dto.setRangoEdad20_60(this.rangoEdad20_60);
        dto.setReciboServiciosConDireccionExacta(this.reciboServiciosConDireccionExacta);
        dto.setReciboServiciosPropio(this.reciboServiciosPropio);
        dto.setCuentaConServiciosBasicos(this.cuentaConServiciosBasicos);
        dto.setDireccionCoincideConExpediente(this.direccionCoincideConExpediente);
        dto.setUbicacionVivienda(this.ubicacionVivienda);
        return dto;
    }

    public static VerificacionViviendaEntity fromDTO(final VerificacionVivienda dto) {
        final VerificacionViviendaEntity entity = new VerificacionViviendaEntity();
        entity.setFechaSupervision(dto.getFechaSupervision());
        entity.setViviendaPropia(dto.getViviendaPropia());
        entity.setEsGuatemalteca(dto.getEsGuatemalteca());
        entity.setRangoEdad20_60(dto.getRangoEdad20_60());
        entity.setReciboServiciosConDireccionExacta(dto.getReciboServiciosConDireccionExacta());
        entity.setReciboServiciosPropio(dto.getReciboServiciosPropio());
        entity.setCuentaConServiciosBasicos(dto.getCuentaConServiciosBasicos());
        entity.setDireccionCoincideConExpediente(dto.getDireccionCoincideConExpediente());
        entity.setUbicacionVivienda(dto.getUbicacionVivienda());
        return entity;
    }

    public void setLoanAdditionalDataPAE(final LoanAdditionalDataPAEEntity loanAdditionalDataPAE) {
        this.loanAdditionalDataPAE = loanAdditionalDataPAE;
    }
}
