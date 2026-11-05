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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Resultado de validar una cuenta de Garantías (respuesta del endpoint 1). */
@Getter
@Setter
public final class NegativeHoldsValidationData {

    private Long savingsId;
    private String accountNo;
    private Long clientId;
    private String clientAccountNo;
    private String clientName;
    private BigDecimal saldo;
    private BigDecimal retenido;
    private boolean corregible;
    private List<String> motivos = new ArrayList<>();
    private List<String> advertencias = new ArrayList<>();
    private List<NegativeHoldPairData> pares = new ArrayList<>();
    private BigDecimal saldoMinActual;
    private BigDecimal saldoMinSimulado;
    private List<NegativeHoldMovementData> movimientos;

    /** Resultado para un número de cliente sin cuenta de Garantías activa. */
    public static NegativeHoldsValidationData withoutAccount(final String clientAccountNo, final String motivo) {
        final NegativeHoldsValidationData data = new NegativeHoldsValidationData();
        data.setClientAccountNo(clientAccountNo);
        data.setCorregible(false);
        data.getMotivos().add(motivo);
        return data;
    }
}
