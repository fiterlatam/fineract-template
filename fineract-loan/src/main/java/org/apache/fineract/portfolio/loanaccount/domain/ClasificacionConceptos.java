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

package org.apache.fineract.portfolio.loanaccount.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Getter
@Setter
@Entity
@Table(name = "c_clasificacion_conceptos")
public class ClasificacionConceptos extends AbstractPersistableCustom {

    @Column(length = 50)
    private String concepto;

    @Column
    private boolean mandato;

    @Column
    private boolean excluido;

    @Column
    private boolean exento;

    @Column
    private boolean gravado;

    @Column
    private String norma;

    @Column(precision = 18, scale = 4)
    private BigDecimal tarifa;

    public void update(JsonCommand command) {
        this.concepto = command.stringValueOfParameterNamed("concepto");
        this.mandato = command.booleanPrimitiveValueOfParameterNamed("mandato");
        this.excluido = command.booleanPrimitiveValueOfParameterNamed("excluido");
        this.exento = command.booleanPrimitiveValueOfParameterNamed("exento");
        this.gravado = command.booleanPrimitiveValueOfParameterNamed("gravado");
        this.norma = command.stringValueOfParameterNamed("norma");
        this.tarifa = command.bigDecimalValueOfParameterNamed("tarifa");
    }

    public static ClasificacionConceptos create(JsonCommand command) {
        ClasificacionConceptos clasificacionConceptos = new ClasificacionConceptos();
        clasificacionConceptos.update(command);
        return clasificacionConceptos;
    }
}
