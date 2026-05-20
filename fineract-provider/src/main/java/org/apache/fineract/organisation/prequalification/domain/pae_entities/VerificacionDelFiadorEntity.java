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
import org.apache.fineract.organisation.prequalification.data.paeadditional.VerificacionDelFiador;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "m_pae_verificacion_del_fiador")
public class VerificacionDelFiadorEntity extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "loan_additional_pae_id")
    private LoanAdditionalDataPAEEntity loanAdditionalDataPAE;

    @Column(name = "conoce_a_clienta", length = 50)
    private String conoceAClienta;

    @Column(name = "si_es_familiar_muestra_independencia_economica", length = 50)
    private String siEsFamiliarMuestraIndependenciaEconomica;

    @Column(name = "sabe_que_es_fiador_y_conoce_el_monto", length = 50)
    private String sabeQueEsFiadorYConoceElMonto;

    @Column(name = "rango_edad_20_60", length = 50)
    private String rangoEdad20_60;

    @Column(name = "direccion_coincide_con_expediente", length = 50)
    private String direccionCoincideConExpediente;

    @Column(name = "esta_solvente_en_pda", length = 50)
    private String estaSolventeEnPDA;

    @Column(name = "anio_de_laborar_o_3_anios_en_negocio", length = 50)
    private String anioDeLaborarO3AniosEnNegocio;

    @Column(name = "es_fiador_de_otra_persona", length = 50)
    private String esFiadorDeOtraPersona;

    @Column(name = "cuenta_con_constancia_de_ingresos", length = 50)
    private String cuentaConConstanciaDeIngresos;

    @Column(name = "cuenta_con_constancia_de_propiedad_del_negocio", length = 50)
    private String cuentaconConstanciaDePropiedadDelNegocio;

    @Column(name = "negocio_elegible_segun_politica", length = 50)
    private String negocioElegibleSegunPolitica;

    public VerificacionDelFiador toDTO() {
        final VerificacionDelFiador dto = new VerificacionDelFiador();
        dto.setConoceAClienta(this.conoceAClienta);
        dto.setSiEsFamiliarMuestraIndependenciaEconomica(this.siEsFamiliarMuestraIndependenciaEconomica);
        dto.setSabeQueEsFiadorYConoceElMonto(this.sabeQueEsFiadorYConoceElMonto);
        dto.setRangoEdad20_60(this.rangoEdad20_60);
        dto.setDireccionCoincideConExpediente(this.direccionCoincideConExpediente);
        dto.setEstaSolventeEnPDA(this.estaSolventeEnPDA);
        dto.setAnioDeLaborarO3AniosEnNegocio(this.anioDeLaborarO3AniosEnNegocio);
        dto.setEsFiadorDeOtraPersona(this.esFiadorDeOtraPersona);
        dto.setCuentaConConstanciaDeIngresos(this.cuentaConConstanciaDeIngresos);
        dto.setCuentaconConstanciaDePropiedadDelNegocio(this.cuentaconConstanciaDePropiedadDelNegocio);
        dto.setNegocioElegibleSegunPolitica(this.negocioElegibleSegunPolitica);
        return dto;
    }

    public static VerificacionDelFiadorEntity fromDTO(final VerificacionDelFiador dto) {
        final VerificacionDelFiadorEntity entity = new VerificacionDelFiadorEntity();
        entity.setConoceAClienta(dto.getConoceAClienta());
        entity.setSiEsFamiliarMuestraIndependenciaEconomica(dto.getSiEsFamiliarMuestraIndependenciaEconomica());
        entity.setSabeQueEsFiadorYConoceElMonto(dto.getSabeQueEsFiadorYConoceElMonto());
        entity.setRangoEdad20_60(dto.getRangoEdad20_60());
        entity.setDireccionCoincideConExpediente(dto.getDireccionCoincideConExpediente());
        entity.setEstaSolventeEnPDA(dto.getEstaSolventeEnPDA());
        entity.setAnioDeLaborarO3AniosEnNegocio(dto.getAnioDeLaborarO3AniosEnNegocio());
        entity.setEsFiadorDeOtraPersona(dto.getEsFiadorDeOtraPersona());
        entity.setCuentaConConstanciaDeIngresos(dto.getCuentaConConstanciaDeIngresos());
        entity.setCuentaconConstanciaDePropiedadDelNegocio(dto.getCuentaconConstanciaDePropiedadDelNegocio());
        entity.setNegocioElegibleSegunPolitica(dto.getNegocioElegibleSegunPolitica());
        return entity;
    }
}
