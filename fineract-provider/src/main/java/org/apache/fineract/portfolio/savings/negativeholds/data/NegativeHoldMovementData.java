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
import java.time.LocalDate;
import lombok.Getter;

/** Movimiento de la cuenta para la respuesta de validación. */
@Getter
public final class NegativeHoldMovementData {

    private final Long id;
    private final Integer tipo;
    private final BigDecimal monto;
    private final LocalDate fecha;
    private final BigDecimal saldoCorrido;
    /** Saldo corrido recalculado sin los pares (null si el movimiento no cuenta: reversado o parte de un par). */
    private final BigDecimal saldoSimulado;
    private final boolean reversado;
    private final boolean enPar;
    private final Long releaseId;
    private final long asientos;

    public NegativeHoldMovementData(final Long id, final Integer tipo, final BigDecimal monto, final LocalDate fecha,
            final BigDecimal saldoCorrido, final BigDecimal saldoSimulado, final boolean reversado, final boolean enPar,
            final Long releaseId, final long asientos) {
        this.id = id;
        this.tipo = tipo;
        this.monto = monto;
        this.fecha = fecha;
        this.saldoCorrido = saldoCorrido;
        this.saldoSimulado = saldoSimulado;
        this.reversado = reversado;
        this.enPar = enPar;
        this.releaseId = releaseId;
        this.asientos = asientos;
    }
}
