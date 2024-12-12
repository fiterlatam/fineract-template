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
package org.apache.fineract.portfolio.loanaccount.invoice.data;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.portfolio.loanaccount.domain.ClasificacionConceptos;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ClasificacionConceptosData {

    private Long id;
    private String concepto;
    private boolean mandato;
    private boolean excluido;
    private boolean exento;
    private boolean gravado;
    private String norma;
    private BigDecimal tarifa;

    public ClasificacionConceptosData(ClasificacionConceptos clasificacionConceptos) {
        this.id = clasificacionConceptos.getId();
        this.concepto = clasificacionConceptos.getConcepto();
        this.mandato = clasificacionConceptos.isMandato();
        this.excluido = clasificacionConceptos.isExcluido();
        this.exento = clasificacionConceptos.isExento();
        this.gravado = clasificacionConceptos.isGravado();
        this.norma = clasificacionConceptos.getNorma();
        this.tarifa = clasificacionConceptos.getTarifa();
    }
}
