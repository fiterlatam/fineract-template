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
package org.apache.fineract.portfolio.savings.negativeholds.service;

import java.util.List;
import org.apache.fineract.portfolio.savings.negativeholds.data.GuaranteeAccountSnapshot;
import org.apache.fineract.portfolio.savings.negativeholds.data.NegativeHoldsValidationData;

public interface NegativeHoldsReadPlatformService {

    /** Endpoint 1: valida las cuentas de Garantías de uno o varios clientes (por número de cliente) o una cuenta. */
    List<NegativeHoldsValidationData> validate(List<String> clientAccountNos, Long savingsId, boolean includeMovements);

    /** Reporte: todas las cuentas de Garantías activas con alguna retención de saldo corrido negativo. */
    List<NegativeHoldsValidationData> report(boolean includeMovements);

    /** Ids de cuentas de Garantías activas del cliente. */
    List<Long> findGuaranteeAccountIdsByClientAccountNo(String clientAccountNo);

    /** Carga la cuenta y sus movimientos; con lockAccount=true bloquea la fila de m_savings_account (FOR UPDATE). */
    GuaranteeAccountSnapshot loadSnapshot(Long savingsId, boolean lockAccount);
}
