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
package org.apache.fineract.organisation.prequalification.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.organisation.prequalification.data.paeadditional.CalificacionDelSupervisor;
import org.apache.fineract.organisation.prequalification.data.paeadditional.EntrevistaCliente;
import org.apache.fineract.organisation.prequalification.data.paeadditional.VerificacionDelFiador;
import org.apache.fineract.organisation.prequalification.data.paeadditional.VerificacionNegocio;
import org.apache.fineract.organisation.prequalification.data.paeadditional.VerificacionVivienda;
import org.apache.fineract.organisation.prequalification.domain.pae_entities.CalificacionDelSupervisorEntity;
import org.apache.fineract.organisation.prequalification.domain.pae_entities.EntrevistaClienteEntity;
import org.apache.fineract.organisation.prequalification.domain.pae_entities.LoanAdditionalDataPAEEntity;
import org.apache.fineract.organisation.prequalification.domain.pae_entities.VerificacionDelFiadorEntity;
import org.apache.fineract.organisation.prequalification.domain.pae_entities.VerificacionNegocioEntity;
import org.apache.fineract.organisation.prequalification.domain.pae_entities.VerificacionViviendaEntity;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
@NoArgsConstructor
public class LoanAdditionalDataPAE extends LoanAdditionalData {

    private VerificacionNegocio verificacionNegocio;
    private EntrevistaCliente entrevistaCliente;
    private CalificacionDelSupervisor calificacionDelSupervisor;
    private VerificacionDelFiador verificacionDelFiador;
    private VerificacionVivienda verificacionVivienda;

    public LoanAdditionalDataPAEEntity toPaeEntity(LoanAdditionalDataPAEEntity loanAdditionEntity) {
        CalificacionDelSupervisorEntity calificacionDelSupervisorEntity = loanAdditionEntity.getCalificacionDelSupervisor();
        EntrevistaClienteEntity entrevistaClienteEntity = loanAdditionEntity.getEntrevistaCliente();
        VerificacionNegocioEntity verificacionNegocioEntity = loanAdditionEntity.getVerificacionNegocio();
        VerificacionViviendaEntity verificacionViviendaEntity = loanAdditionEntity.getVerificacionVivienda();
        VerificacionDelFiadorEntity verificacionDelFiadorEntity = loanAdditionEntity.getVerificacionDelFiador();
        if (calificacionDelSupervisorEntity == null) {
            calificacionDelSupervisorEntity = new CalificacionDelSupervisorEntity();
        }
        BeanUtils.copyProperties(this.calificacionDelSupervisor, calificacionDelSupervisorEntity);
        calificacionDelSupervisorEntity.setLoanAdditionalDataPAE(loanAdditionEntity);
        loanAdditionEntity.setCalificacionDelSupervisor(calificacionDelSupervisorEntity);
        if (entrevistaClienteEntity == null) {
            entrevistaClienteEntity = new EntrevistaClienteEntity();
        }
        BeanUtils.copyProperties(this.entrevistaCliente, entrevistaClienteEntity);
        entrevistaClienteEntity.setLoanAdditionalDataPAE(loanAdditionEntity);
        loanAdditionEntity.setEntrevistaCliente(entrevistaClienteEntity);
        if (verificacionNegocioEntity == null) {
            verificacionNegocioEntity = new VerificacionNegocioEntity();
        }
        BeanUtils.copyProperties(this.verificacionNegocio, verificacionNegocioEntity);
        verificacionNegocioEntity.setLoanAdditionalDataPAE(loanAdditionEntity);
        loanAdditionEntity.setVerificacionNegocio(verificacionNegocioEntity);
        if (verificacionViviendaEntity == null) {
            verificacionViviendaEntity = new VerificacionViviendaEntity();
        }
        BeanUtils.copyProperties(this.verificacionVivienda, verificacionViviendaEntity);
        verificacionViviendaEntity.setLoanAdditionalDataPAE(loanAdditionEntity);
        loanAdditionEntity.setVerificacionVivienda(verificacionViviendaEntity);
        if (verificacionDelFiadorEntity == null) {
            verificacionDelFiadorEntity = new VerificacionDelFiadorEntity();
        }
        BeanUtils.copyProperties(this.verificacionDelFiador, verificacionDelFiadorEntity);
        verificacionDelFiadorEntity.setLoanAdditionalDataPAE(loanAdditionEntity);
        loanAdditionEntity.setVerificacionDelFiador(verificacionDelFiadorEntity);

        return loanAdditionEntity;
    }
}
