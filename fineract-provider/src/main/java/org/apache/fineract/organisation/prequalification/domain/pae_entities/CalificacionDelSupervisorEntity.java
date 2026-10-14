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

import java.math.BigDecimal;
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
import org.apache.fineract.organisation.prequalification.data.paeadditional.CalificacionDelSupervisor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "m_pae_calificacion_del_supervisor")
public class CalificacionDelSupervisorEntity extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "loan_additional_pae_id")
    private LoanAdditionalDataPAEEntity loanAdditionalDataPAE;

    @Column(name = "punteo", precision = 19, scale = 6)
    private BigDecimal punteo;

    @Column(name = "calificacion", length = 100)
    private String calificacion;

    @Column(name = "ubicacion", length = 255)
    private String ubicacion;

    @Column(name = "supervisor", length = 255)
    private String supervisor;

    @Column(name = "comentarios", columnDefinition = "TEXT")
    private String comentarios;

    public CalificacionDelSupervisor toDTO() {
        final CalificacionDelSupervisor dto = new CalificacionDelSupervisor();
        dto.setPunteo(this.punteo);
        dto.setCalificacion(this.calificacion);
        dto.setUbicacion(this.ubicacion);
        dto.setSupervisor(this.supervisor);
        dto.setComentarios(this.comentarios);
        return dto;
    }

    public static CalificacionDelSupervisorEntity fromDTO(final CalificacionDelSupervisor dto) {
        final CalificacionDelSupervisorEntity entity = new CalificacionDelSupervisorEntity();
        entity.setPunteo(dto.getPunteo());
        entity.setCalificacion(dto.getCalificacion());
        entity.setUbicacion(dto.getUbicacion());
        entity.setSupervisor(dto.getSupervisor());
        entity.setComentarios(dto.getComentarios());
        return entity;
    }
}
