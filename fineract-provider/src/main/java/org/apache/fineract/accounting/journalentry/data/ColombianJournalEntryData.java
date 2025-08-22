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
package org.apache.fineract.accounting.journalentry.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data DTO for Colombian journal entry export format Maps to the v_colombian_journal_export database view
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColombianJournalEntryData {

    // Fixed value fields
    private Integer movMovimiento = 0;
    private String mesMescontable;
    private String ctaCuenta;
    private String empEmpresa = "IT";
    private String empEmpfl = "99";
    private BigDecimal movValor;

    // Transaction type and cost center
    private String tpcTipocomprob;
    private String ceCo;

    // Client information
    private String terTercero;
    private String terDigver = "";
    private String terGrupo = "";

    // Currency and exchange rate
    private String monMoneda = "1";
    private String trmFecha = "";
    private String movVlrotramon = "";
    private String movVlrtrm = "";

    // VAT handling
    private String cptCptotributa = "";
    private String movPorcentaje = "";
    private String movVlrbase = "";

    // Document reference
    private String movDcmto;
    private String movAfectadcmto = "";

    // Dates
    private LocalDate movFechadcmto;
    private LocalDate movFini;
    private LocalDate movFfin;

    // Nature (Debit/Credit)
    private Integer nat;

    // Additional fields
    private String cpcCptocomp = "";
    private String tpdTipodoc = "";
    private String scoSubdivcom = "";

    // Description
    private String menconcepto;
    private LocalDate movFecha;

    // Status and user fields
    private String movEstado = "S";
    private String movCtapadre = "";
    private String terGrancont = "";
    private String terRegiva = "";
    private String movBaseretiva = "";
    private String movCausaretiva = "";
    private String terAutoretenedor = "";
    private String movCausaretefte = "";
    private String movBaserefte = "";
    private String movVlrdifbase = "";
    private String movVlrdifmon = "";
    private String movMovpadre = "";
    private String movFechamov = "";
    private String movUsuario = "O";
    private String movSecuencia = "S";
    private String obrObra = "";
    private String obrSubobra = "";
    private Integer varIntnulo = 0;
    private String tipDocafecta = "";
    private Integer varIntnulo2 = 0;

    // Additional context fields for filtering
    private Long officeId;
    private Integer entityTypeEnum;
    private Long entityId;
    private String transactionId;
    private Integer classificationEnum;
}
