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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.prequalification.data.LoanAdditionalDataPAE;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "m_pae_loan_additional_data")
public class LoanAdditionalDataPAEEntity extends AbstractPersistableCustom {

    @OneToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "case_id", length = 100)
    private String caseId;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "verificacion_vivienda_id")
    private VerificacionViviendaEntity verificacionVivienda;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "verificacion_negocio_id")
    private VerificacionNegocioEntity verificacionNegocio;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "entrevista_cliente_id")
    private EntrevistaClienteEntity entrevistaCliente;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "verificacion_del_fiador_id")
    private VerificacionDelFiadorEntity verificacionDelFiador;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "calificacion_del_supervisor_id")
    private CalificacionDelSupervisorEntity calificacionDelSupervisor;

    public LoanAdditionalDataPAE toDTO() {
        final LoanAdditionalDataPAE dto = new LoanAdditionalDataPAE();
        dto.setCaseId(this.caseId);

        if (this.verificacionVivienda != null) {
            dto.setVerificacionVivienda(this.verificacionVivienda.toDTO());
        }
        if (this.verificacionNegocio != null) {
            dto.setVerificacionNegocio(this.verificacionNegocio.toDTO());
        }
        if (this.entrevistaCliente != null) {
            dto.setEntrevistaCliente(this.entrevistaCliente.toDTO());
        }
        if (this.verificacionDelFiador != null) {
            dto.setVerificacionDelFiador(this.verificacionDelFiador.toDTO());
        }
        if (this.calificacionDelSupervisor != null) {
            dto.setCalificacionDelSupervisor(this.calificacionDelSupervisor.toDTO());
        }

        return dto;
    }

    public static LoanAdditionalDataPAEEntity fromDTO(final LoanAdditionalDataPAE dto) {
        final LoanAdditionalDataPAEEntity entity = new LoanAdditionalDataPAEEntity();
        entity.setCaseId(dto.getCaseId());

        if (dto.getVerificacionVivienda() != null) {
            entity.setVerificacionVivienda(VerificacionViviendaEntity.fromDTO(dto.getVerificacionVivienda()));
        }
        if (dto.getVerificacionNegocio() != null) {
            entity.setVerificacionNegocio(VerificacionNegocioEntity.fromDTO(dto.getVerificacionNegocio()));
        }
        if (dto.getEntrevistaCliente() != null) {
            entity.setEntrevistaCliente(EntrevistaClienteEntity.fromDTO(dto.getEntrevistaCliente()));
        }
        if (dto.getVerificacionDelFiador() != null) {
            entity.setVerificacionDelFiador(VerificacionDelFiadorEntity.fromDTO(dto.getVerificacionDelFiador()));
        }
        if (dto.getCalificacionDelSupervisor() != null) {
            entity.setCalificacionDelSupervisor(CalificacionDelSupervisorEntity.fromDTO(dto.getCalificacionDelSupervisor()));
        }

        return entity;
    }
}

