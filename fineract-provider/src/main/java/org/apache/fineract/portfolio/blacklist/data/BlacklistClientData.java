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
package org.apache.fineract.portfolio.blacklist.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;

/**
 * Immutable data object represent client identity data.
 */
public class BlacklistClientData {

    private final Long id;
    private final Long clientId;
    private final Long productId;
    private final String productCode;
    private final String clientName;
    private final Integer year;
    private final CodeValueData typification;
    private final String dpi;
    private final String nit;
    private final String description;
    private final String agencyId;
    private final BigDecimal balance;
    private final BigDecimal disbursementAmount;
    private final String status;
    private final String addedBy;
    private final LocalDateTime createdAt;
    @SuppressWarnings("unused")
    private final Collection<CodeValueData> typificationOptions;
    private final Collection<LoanProductData> loanProducts;

    public BlacklistClientData(final Long id, final Long clientId, final String clientName, final Long productId,
            final CodeValueData typification, final String productCode, final String dpi, final String nit, final String agencyName,
            final Integer year, final BigDecimal balance, final BigDecimal disbursementAmount, final String addedBy,
            final LocalDateTime createdAt, final String description, final String status,
            final Collection<CodeValueData> typificationOptions, final Collection<LoanProductData> loanProducts) {
        this.id = id;

        this.clientId = clientId;
        this.clientName = clientName;
        this.productId = productId;
        this.productCode = productCode;
        this.year = year;
        this.typification = typification;
        this.dpi = dpi;
        this.nit = nit;
        this.description = description;
        this.agencyId = agencyName;
        this.balance = balance;
        this.disbursementAmount = disbursementAmount;
        this.status = status;
        this.addedBy = addedBy;
        this.createdAt = createdAt;
        this.typificationOptions = typificationOptions;
        this.loanProducts = loanProducts;
    }

    public static BlacklistClientData template(Collection<CodeValueData> codeValues, final Collection<LoanProductData> loanProducts) {
        return new BlacklistClientData(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                codeValues, loanProducts);
    }

    public static BlacklistClientData instance(Long id, String displayName, EnumOptionData status, CodeValueData typification,
            Long productId, String dpiNumber, String nitNumber, String agencyName, String productCode, String productName,
            BigDecimal balance, BigDecimal disbursementAmount, String addedBy, String year, String description) {
        return new BlacklistClientData(id, null, displayName, productId, typification, productCode, dpiNumber, nitNumber, agencyName,
                Integer.valueOf(year), balance, disbursementAmount, addedBy, null, description, status.getValue(), null, null);
    }
}
