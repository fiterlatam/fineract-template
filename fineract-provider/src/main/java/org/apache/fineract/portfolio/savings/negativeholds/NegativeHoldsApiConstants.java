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
package org.apache.fineract.portfolio.savings.negativeholds;

public final class NegativeHoldsApiConstants {

    private NegativeHoldsApiConstants() {}

    /** Recurso de permisos: READ_NEGATIVEHOLDS / FIX_NEGATIVEHOLDS. */
    public static final String RESOURCE_NAME = "NEGATIVEHOLDS";
    public static final String ENTITY_NAME = "NEGATIVEHOLDS";
    public static final String ACTION_FIX = "FIX";
    public static final String COMMAND_FIX = "fix";

    public static final String CLIENT_ACCOUNT_NO_PARAM = "clientAccountNo";
    public static final String SAVINGS_ID_PARAM = "savingsId";
    public static final String EXPECTED_PAIRS_PARAM = "expectedPairs";
    public static final String HOLD_ID_PARAM = "holdId";
    public static final String RELEASE_ID_PARAM = "releaseId";

    /** Patrón SQL del producto de Garantías (constante GURANTEE_PRODUCT_NAME = "Garantías"). */
    public static final String GUARANTEE_PRODUCT_NAME_LIKE = "Garant%";

    // Motivos (por qué una cuenta no es corregible con el método de reversar pares)
    public static final String MOTIVO_SIN_CUENTA = "SIN_CUENTA_GARANTIAS";
    public static final String MOTIVO_MULTIPLES_CUENTAS = "MULTIPLES_CUENTAS_GARANTIAS";
    public static final String MOTIVO_NADA_QUE_CORREGIR = "NADA_QUE_CORREGIR";
    public static final String MOTIVO_RETENCION_SIN_LIBERAR = "RETENCION_ACTIVA_SIN_LIBERAR";
    public static final String MOTIVO_PAR_INVALIDO = "PAR_INVALIDO";
    public static final String MOTIVO_PAR_CON_ASIENTOS = "PAR_CON_ASIENTOS_CONTABLES";
    public static final String MOTIVO_NEGATIVO_OTRO_ORIGEN = "NEGATIVO_DE_OTRO_ORIGEN";
    public static final String MOTIVO_SIMULACION_NEGATIVA = "SIMULACION_NEGATIVA";

    // Advertencias (no bloquean)
    public static final String ADV_SALDOS_NO_COINCIDEN = "SALDOS_GUARDADOS_NO_COINCIDEN_CON_RECALCULO";
    public static final String ADV_SOBREGIRO_REGISTRADO = "SOBREGIRO_REGISTRADO_EN_OTROS_MOVIMIENTOS";
}
