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
package org.apache.fineract.portfolio.cupo.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;

public final class CupoData {

    private Long id;
    private BigDecimal amount;
    private BigDecimal amountSubmitted;
    private BigDecimal amountApproved;
    private BigDecimal amountAvailable;
    private BigDecimal amountInHold;
    private ClientData clientData;
    private GroupGeneralData groupData;
    private LocalDate expirationDate;
    private LocalDate approvalDate;
    private EnumOptionData status;
    private CurrencyData currency;

    // template
    private Collection<CurrencyData> currencyOptions;

    public static CupoData instance(Long id, BigDecimal amount, BigDecimal amountSubmitted, BigDecimal amountApproved,
            ClientData clientData, GroupGeneralData groupData, LocalDate expirationDate, LocalDate approvalDate, EnumOptionData status,
            BigDecimal amountInHold, BigDecimal amountAvailable, CurrencyData currency) {
        return new CupoData(id, amount, amountSubmitted, amountApproved, clientData, groupData, expirationDate, approvalDate, status,
                currency, amountInHold, amountAvailable, null);
    }

    private CupoData(Long id, BigDecimal amount, BigDecimal amountSubmitted, BigDecimal amountApproved, ClientData clientData,
            GroupGeneralData groupData, LocalDate expirationDate, LocalDate approvalDate, EnumOptionData status, CurrencyData currency,
            BigDecimal amountInHold, BigDecimal amountAvailable, Collection<CurrencyData> currencyOptions) {
        this.id = id;
        this.amount = amount;
        this.amountSubmitted = amountSubmitted;
        this.amountApproved = amountApproved;
        this.amountInHold = amountInHold;
        this.amountAvailable = amountAvailable;
        this.clientData = clientData;
        this.groupData = groupData;
        this.expirationDate = expirationDate;
        this.approvalDate = approvalDate;
        this.status = status;
        this.currency = currency;
        this.currencyOptions = currencyOptions;
    }

    public static CupoData template(ClientData clientData, GroupGeneralData groupData, Collection<CurrencyData> currencyOptions) {
        return new CupoData(null, null, null, null, clientData, groupData, null, null, null, null, null, null, currencyOptions);
    }
}
