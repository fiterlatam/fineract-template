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
package org.apache.fineract.accounting.journalentry.domain;

public final class BitacoraMasterConstants {

    private BitacoraMasterConstants() {}

    public static final String ACCOUNT_TYPE_PR = "PR";
    public static final String ACCOUNT_TYPE_AH = "AH";
    public static final String ACCOUNT_TYPE_PF = "PF";
    public static final String ACCOUNT_TYPE_DV = "DV";

    // Loans
    public static final String TRX_TYPE_DESEMBOLSO = "DESEMBOLSO";
    public static final String TRX_TYPE_DESEMBOLSO_MULTITRANCHES = "DESEMBOLSOEG";
    public static final String TRX_TYPE_DESEMBOLSO_CUPO = "DESEMBOLSOLC";
    public static final String TRX_TYPE_REGISTRO_GARANTIA = "REGISTROGARANTIA";
    public static final String TRX_TYPE_GARANTIA = "GARANTIA";
    public static final String TRX_TYPE_CAMBIO_CATEGORIA = "CAMBIOCATEGORIA";
    public static final String TRX_TYPE_DEVENGO_COMISION = "DEVENGOCOMISION";
    public static final String TRX_TYPE_LIBERACION_GARANTIA = "LIBERACIONGARANTIA";
    public static final String TRX_TYPE_PAGO = "PAGO";
    public static final String TRX_TYPE_PAGO_CUPO = "PAGOLC";
    public static final String TRX_TYPE_CALCULO_MORA = "CALCULOMORA";
    public static final String TRX_TYPE_CALCULO_PENALIDAD = "CALCULOPENALIDAD";
    public static final String TRX_TYPE_CALCULO_INTERESES = "CALCULOINTERESES";
    public static final String TRX_TYPE_APROBACION = "APROBACION";
    public static final String TRX_TYPE_APROBACION_MULTITRANCHES = "APROBACIONEG";
    public static final String TRX_TYPE_APROBACION_CUPO = "APROBACIONLC";
    public static final String TRX_TYPE_FORMALIZACION = "FORMALIZACION";
    public static final String TRX_TYPE_FORMALIZACION_MULTITRANCHES = "FORMALIZACIONEG";
    public static final String TRX_TYPE_FORMALIZACION_CUPO = "FORMALIZACIONLC";
    public static final String TRX_TYPE_CAMBIOAGRUPACION = "CAMBIOAGRUPACION";
    public static final String TRX_TYPE_CAMBIO_STATUS_AVENCIDO = "CAMBIOSTATUSAVENCIDO";
    public static final String TRX_TYPE_CAMBIO_STATUS_JURIDICO = "CAMBIOSTATUSAJURIDICO";
    public static final String TRX_TYPE_CAMBIO_STATUS_AVIGENTE = "CAMBIOSTATUSAVIGENTE";

    // Savings
    public static final String TRX_TYPE_SOBREGIROCR = "SOBREGIROCR";
    public static final String TRX_TYPE_SOBREGIRODB = "SOBREGIRODB";
    public static final String TRX_TYPE_NC = "NC";
    public static final String TRX_TYPE_DP = "DP";
    public static final String TRX_TYPE_ND = "ND";
    public static final String TRX_TYPE_NDCARGOS = "NDCARGOS";
    public static final String TRX_TYPE_CALCINT = "CALCINT";
    public static final String TRX_TYPE_CAPINT = "CAPINT";
    public static final String TRX_TYPE_ISRINT = "ISRINT";
    public static final String TRX_TYPE_BLC = "BLC";
    public static final String TRX_TYPE_DBLC = "DBLC";

    // Buy/Sell USD
    public static final String TRX_TYPE_VENTA = "VENTA";
    public static final String TRX_TYPE_COMPRA = "COMPRA";

    // Other
    public static final String TRX_TYPE_PREFIX_REVERSE = "REVERSION";

}
