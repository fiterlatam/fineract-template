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
package org.apache.fineract.portfolio.savings.negativeholds.data;

import java.math.BigDecimal;
import lombok.Getter;

/**
 * Movimiento activo que queda entre una retención y su liberación reversadas. Al corregir se guarda el saldo corrido
 * que Fineract recalcularía y sobregiro 0; si no, el siguiente recálculo vería un sobregiro distinto al guardado y
 * volvería a lanzar el error de contabilidad.
 */
@Getter
public final class NegativeHoldBalanceAdjustmentData {

    private final Long id;
    private final BigDecimal saldoAntes;
    private final BigDecimal saldoDespues;
    private final BigDecimal sobregiroAntes;

    public NegativeHoldBalanceAdjustmentData(final Long id, final BigDecimal saldoAntes, final BigDecimal saldoDespues,
            final BigDecimal sobregiroAntes) {
        this.id = id;
        this.saldoAntes = saldoAntes;
        this.saldoDespues = saldoDespues;
        this.sobregiroAntes = sobregiroAntes;
    }
}
