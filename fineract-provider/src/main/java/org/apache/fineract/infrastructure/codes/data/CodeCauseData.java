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
package org.apache.fineract.infrastructure.codes.data;

import java.io.Serializable;
import java.util.Collection;
import org.apache.fineract.organisation.monetary.data.CurrencyData;

public final class CodeCauseData implements Serializable {

    private final Integer id;
    private final String description;
    private final Boolean allowOverdraft;
    private final Boolean isCashOperation;
    private final String operationType;
    private final Boolean sendAcrm;
    private final Integer codigoAcrm;
    private final String shortDescription;
    private final Boolean isDocument;
    private final Integer currencyCode;
    private final CurrencyData currency;
    private final Collection<CurrencyData> currencyOptions;
    private final Boolean isShownInCashierModule;

    public static CodeCauseData template(final Collection<CurrencyData> currencyOptions) {
        return new CodeCauseData(null, null, null, null, null, null, null, null, null, null, null, currencyOptions, null);
    }

    public static CodeCauseData codigoAcrmOnly(final Integer codigoAcrm) {
        return new CodeCauseData(null, null, null, null, null, null, codigoAcrm, null, null, null, null, null, null);
    }

    public static CodeCauseData instance(final Integer id, final String description, final Boolean allowOverdraft,
            final Boolean isCashOperation, final String operationType, final Boolean sendAcrm, final Integer codigoAcrm,
            final String shortDescription, final Boolean isDocument, final Integer currencyCode, final CurrencyData currency,
            final Boolean isShownInCashierModule) {
        return new CodeCauseData(id, description, allowOverdraft, isCashOperation, operationType, sendAcrm, codigoAcrm, shortDescription,
                isDocument, currencyCode, currency, null, isShownInCashierModule);
    }

    private CodeCauseData(final Integer id, final String description, final Boolean allowOverdraft, final Boolean isCashOperation,
            final String operationType, final Boolean sendAcrm, final Integer codigoAcrm, final String shortDescription,
            final Boolean isDocument, final Integer currencyCode, final CurrencyData currency, Collection<CurrencyData> currencyOptions,
            final Boolean isShownInCashierModule) {
        this.id = id;
        this.description = description;
        this.allowOverdraft = allowOverdraft;
        this.isCashOperation = isCashOperation;
        this.operationType = operationType;
        this.sendAcrm = sendAcrm;
        this.codigoAcrm = codigoAcrm;
        this.shortDescription = shortDescription;
        this.isDocument = isDocument;
        this.currencyCode = currencyCode;
        this.currency = currency;
        this.currencyOptions = currencyOptions;
        this.isShownInCashierModule = isShownInCashierModule;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getDescription() {
        return description;
    }

    public boolean getAllowOverdraft() {
        if (this.allowOverdraft != null) {
            return allowOverdraft.booleanValue();
        }
        return false;
    }

    public Integer getCodigoAcrm() {
        return codigoAcrm;
    }

}
